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

def OBSERVABILITY_REPO = "https://github.com/wso2-cellery/mesh-observability.git"
def SECURITY_REPO = "https://github.com/wso2-cellery/mesh-security.git"
def CONTROLLER_REPO = "https://github.com/wso2-cellery/mesh-controller.git"
def DISTRIBUTION_REPO = "https://github.com/wso2-cellery/distribution.git"
def SDK_REPO = "https://github.com/wso2-cellery/sdk.git"
def SAMPLES_REPO = "https://github.com/wso2-cellery/samples.git"
def BRANCH = "master"

def jenkinsHome = JENKINS_HOME
def jenkinsHost = "https://wso2.org/jenkins"
def celleryProjectUrl = "$jenkinsHost/job/cellery"

def sdkJobName = "sdk"
def sdkJobUrl = "$celleryProjectUrl/job/$sdkJobName"

def sdkReleaseVersion = SDKReleaseVersion

def DOCKER_REPO = "wso2cellery"

def apiResponse
def filepath
def patternToFind 
def textToReplace

stage ("Update versions in SDK for release ") {
  node ('Cellery-MAC'){ 
    ws ("/Users/wso2/workspace/cellery/sdk") {
        deleteDir()
        println("Cloning SDK repo")
          checkout([$class: 'GitSCM', branches: [[name: BRANCH]],
                          userRemoteConfigs: [[url: SDK_REPO]]])
        sh script: "git checkout -b release-${sdkReleaseVersion}"

        filepath = "README.md"
          patternToFind = /(latest)/
          textToReplace = sdkReleaseVersion
          replaceTagVersions(filepath, patternToFind, textToReplace)
          
          patternToFind = /(latest)/
          textToReplace = sdkReleaseVersion
          replaceTagVersions(filepath, patternToFind, textToReplace)
          
          patternToFind = /(master)/
          textToReplace = "v" + sdkReleaseVersion
          replaceTagVersions(filepath, patternToFind, textToReplace)
          
          filepath = "docs/README.md"
          patternToFind = /(latest)/
          textToReplace = sdkReleaseVersion
          replaceTagVersions(filepath, patternToFind, textToReplace)
          
          patternToFind = /(<version>)/
          textToReplace = sdkReleaseVersion
          replaceTagVersions(filepath, patternToFind, textToReplace)
          

          filesStr = sh (returnStdout: true, script: "find . -name 'pom.xml'").toString()
          filesStr.split('\n').each  {
              filepath = it
                  
              patternToFind = /(<version>${sdkReleaseVersion}-SNAPSHOT<\/version>)/
              textToReplace = "<version>" + sdkReleaseVersion + "</version>"
              replaceTagVersions(filepath, patternToFind, textToReplace)
          }
          filesStr = sh (returnStdout: true, script: "find . -name 'Ballerina.toml'").toString()
          filesStr.split('\n').each  {
              filepath = it
                  
              patternToFind = /(${sdkReleaseVersion}-SNAPSHOT)/
              textToReplace = sdkReleaseVersion
              replaceTagVersions(filepath, patternToFind, textToReplace)
          }
      
      withCredentials([usernamePassword(credentialsId: '4ff4a55b-1313-45da-8cbf-b2e100b1accd', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
                sh """
                set +x
                git config --global user.email "jenkins-bot@wso2.com"
                git config --global user.name "WSO2 Builder"
                set -x
                git add .
                git commit -m \"Update versions for release v${sdkReleaseVersion}\"
                git pull --rebase origin ${BRANCH}
                git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/wso2-cellery/sdk
                """
                //  git push -f https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/wso2-cellery/distribution
      } // end withCredentials
    } // end ws
  } // end node
}

stage ("Release SDK") {
   print ("SDK " + sdkReleaseVersion)
    performRelease( "../" + sdkJobName, sdkReleaseVersion, "release-" + sdkReleaseVersion)
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