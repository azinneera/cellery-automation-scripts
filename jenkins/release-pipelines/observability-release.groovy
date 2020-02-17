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

def observabilityJobName = "mesh-observability"
def observabilityWorkspace = "$jenkinsHome/workspace/$observabilityJobName"
def observabilityJobUrl = "$celleryProjectUrl/job/$observabilityJobName"

def observabilityReleaseVersion = ObservabilityReleaseVersion
def observabilityDevelopmentVersion = ObservabilityDevelopmentVersion


def DOCKER_REPO = "wso2cellery"

def apiResponse
def filepath
def patternToFind 
def textToReplace

stage ('Release mesh-observability') {
    node ("Cellery-Ubuntu") {
      print ("Mesh Observability " + observabilityReleaseVersion)
            performM2Release(observabilityJobUrl, observabilityReleaseVersion, observabilityDevelopmentVersion, "io.cellery.observability:io.cellery.observability.parent")
            build job: '../post-jobs/observability-post-actions', parameters: [string(name: 'ReleaseVersion', value: observabilityReleaseVersion)]
    }
} //end stage




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