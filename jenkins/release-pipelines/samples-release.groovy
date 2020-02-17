#!/usr/bin/env groovy
/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import groovy.json.JsonSlurper
import groovy.transform.Field

def SAMPLES_REPO = "https://github.com/wso2-cellery/samples.git"
def BRANCH = "master"

def jenkinsHome = JENKINS_HOME
def jenkinsHost = "https://wso2.org/jenkins"
def celleryProjectUrl = "$jenkinsHost/job/cellery"

def samplesJobName = "cellery-samples"
def samplesJobUrl = "$celleryProjectUrl/job/$samplesJobName"

def samplesReleaseVersion = SamplesReleaseVersion

def DOCKER_REPO = "wso2cellery"

def apiResponse
def filepath
def patternToFind 
def textToReplace

stage ("Update artifact versions in Samples for release") {
  node ('Cellery-MAC'){
    
        println("Updating Samples artifact versions before release")
          // clean workspace
        deleteDir()
          
        dir('samples') {
          println("Cloning samples repo")
          checkout([$class: 'GitSCM', branches: [[name: BRANCH]],
                          userRemoteConfigs: [[url: SAMPLES_REPO]]])
                          
          sh script: "git checkout -b release-${samplesReleaseVersion}"

          println("Updating versions")
          def filesStr = sh (returnStdout: true, script: "find . -name '*.md'").toString()
          filesStr.split('\n').each  {
              filepath = it
              
              patternToFind = /(latest-dev)/
              textToReplace = samplesReleaseVersion
              replaceTagVersions(filepath, patternToFind, textToReplace)
              
              patternToFind = /(latest)/
              textToReplace = samplesReleaseVersion
              replaceTagVersions(filepath, patternToFind, textToReplace)
              
              patternToFind = /(master)/
              textToReplace = "v" + samplesReleaseVersion
              replaceTagVersions(filepath, patternToFind, textToReplace)
          }

            def balfilesStr = sh (returnStdout: true, script: "find . -name '*.bal'").toString()
            balfilesStr.split('\n').each  {
                filepath = it
                patternToFind = /(latest-dev)/
                textToReplace = samplesReleaseVersion
                replaceTagVersions(filepath, patternToFind, textToReplace)
            }
            def makefilesStr = sh (returnStdout: true, script: "find . -name 'Makefile'").toString()
            makefilesStr.split('\n').each  {
                filepath = it
                patternToFind = /(latest-dev)/
                textToReplace = samplesReleaseVersion
                replaceTagVersions(filepath, patternToFind, textToReplace)
            }
            def yamlfilesStr = sh (returnStdout: true, script: "find . -name '*.yaml'").toString()
            yamlfilesStr.split('\n').each  {
                filepath = it
                patternToFind = /(latest-dev)/
                textToReplace = samplesReleaseVersion
                replaceTagVersions(filepath, patternToFind, textToReplace)
            }

          withCredentials([usernamePassword(credentialsId: '4ff4a55b-1313-45da-8cbf-b2e100b1accd', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
             sh """
             set +x
             git config --global user.email "jenkins-bot@wso2.com"
             git config --global user.name "WSO2 Builder"
             set -x
             git add .
             git commit -m \"Update artifact versions for release v${samplesReleaseVersion}\"
             git pull --rebase origin ${BRANCH}
             git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/wso2-cellery/samples
             """
            //  git push -f https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/wso2-cellery/samples
          }
      } 
    
  } // end node
} // end stage

stage ("Release Samples") {
    print ("Samples " + samplesReleaseVersion)
    performRelease( "../" + samplesJobName, samplesReleaseVersion, "release-" + samplesReleaseVersion)
}

/*
 * Replace string in file.

 * @param file file to replace text in
 * @param replaceText text to replace
 */
def replaceTagVersions(String filepath, String pattern, String replaceText) {
    def text = readFile filepath
    def fileText = text.replaceAll(pattern, replaceText)
    writeFile file: filepath, text: fileText
}

def getlastSuccessfulCommit(String jobUrl){
  String url = jobUrl + "/lastSuccessfulBuild/api/xml?xpath=//lastBuiltRevision/SHA1"
  def response = sh script: "curl --retry 5 $url | grep -oPm1 \"(?<=<SHA1>)[^<]+\"", returnStdout: true
  return response.trim()
}

def performM2Release(String jobUrl, String releaseVersion, String developmentVersion, String mavenInfo) {
  def statusCode
  def releaseBuildResponse = ""
  withCredentials([
    usernamePassword(credentialsId: 'CELLERY_DOCKER_CREDENTIALS', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')
    ]) {
    statusCode = sh script: "curl -k -s -o /dev/null -w '%{http_code}' -X POST " +
    "$jobUrl/m2release/submit " +
      "-u \"cellery-docker@wso2.com:${PASSWORD}\" " +
      "-H \"Content-Type:application/x-www-form-urlencoded\" " +
      "-d \"releaseVersion=$releaseVersion&developmentVersion=$developmentVersion" +
      "&scmUsername=&scmPassword=&scmCommentPrefix=[maven-release-plugin]&scmTag=io.cellery.observability.parent-$releaseVersion" +
      "&closeNexusStage=on" +
      "&repoDescription=Jenkins+build:+-++-+Git+Tag:+-++-+Release+version:+${releaseVersion}++-+Maven+Info:+${mavenInfo}" +
      "&json={\"releaseVersion\":\"$releaseVersion\",\"developmentVersion\":\"$developmentVersion\",\"isDryRun\":false}" +
      "&Submit=Schedule+Maven+Release+Build\"", 
    returnStdout: true
 }
  echo statusCode

  if (statusCode == "302") {
    def isReleaseInProgress = true
    sleep 600

    while(isReleaseInProgress) {
      // Retrieve a json with data of last build
      releaseBuildResponse = sh script: "curl '$jobUrl/lastBuild/api/json'", returnStdout: true
      echo releaseBuildResponse
      // If result value is null, that means build is still in progress, otherwise it would contain FAILURE or SUCCESS
      isReleaseInProgress = releaseBuildResponse.contains('"result":null') || releaseBuildResponse.contains('"result":"NOT_BUILT"')
      println(isReleaseInProgress)
      if(isReleaseInProgress){
          // Sleep the while loop for 90 seconds, to verify the result again
          sleep 90
      }
    }

  }

  if (statusCode != "302" || releaseBuildResponse.contains('"result":"FAILURE"')) {
    // If result is FAILURE, then mark the stage as failed
    sh "exit 1"
    currentBuild.result = 'FAILURE'
  }
  
}

def performRelease(String jobName, String version) {
  build job: jobName, parameters: [string(name: 'ReleaseVersion', value: version)]
}

def performRelease(String jobName, String version, String branch) {
  build job: jobName, parameters: [string(name: 'Branch', value: branch),string(name: 'ReleaseVersion', value: version)]
}