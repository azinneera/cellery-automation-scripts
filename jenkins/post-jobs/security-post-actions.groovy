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

def SECURITY_REPO = "https://github.com/wso2/cellery-security"
def BRANCH = ""

def jenkinsHome = JENKINS_HOME
def jenkinsHost = "https://wso2.org/jenkins"
def celleryProjectUrl = "$jenkinsHost/job/cellery"

def securityJobName = "cellery-security"
def securityWorkspace = "$jenkinsHome/workspace/$securityJobName"
def securityJobUrl = "$celleryProjectUrl/job/$securityJobName"

def DOCKER_REPO = "wso2cellery"
  
stage ("Build and Push docker images") {
  node ('Cellery-Ubuntu') {    
    deleteDir()

    if (ReleaseVersion != ""){
        VERSION = ReleaseVersion
        BRANCH = "v" + VERSION
    } else {
        VERSION = getlastSuccessfulCommit(securityJobUrl) 
        BRANCH = VERSION
    }

    println("Cloning cellery-security repository")

      println("version: " + VERSION)

      dir(securityJobName) {
        deleteDir()
        checkout([$class: 'GitSCM', branches: [[name: BRANCH]],
                      userRemoteConfigs: [[url: SECURITY_REPO]]])
        copyArtifacts(projectName: "../" + securityJobName, filter: 'docker/*/target/files/**')

        withDockerRegistry([ credentialsId: "DOCKERHUB_CELLERY_TEMP", url: "" ]) {
        sh """
          unset GOROOT
          unset GOPATH
          DOCKER_REPO=${DOCKER_REPO} DOCKER_IMAGE_TAG=latest make docker-push
          DOCKER_REPO=${DOCKER_REPO} DOCKER_IMAGE_TAG=${VERSION} make docker-push
          """
        
      } // endWithDockerRegistry 
    } // end dir cellery-security

  } // end node
} // end stage

/*
 * Replace string in file.

 * @param file file to replace text in
 * @param replaceText text to replace
 */
def replaceTagVersions(String filepath, String pattern, String replaceText) {
    def text = readFile filepath
    def isExists = fileExists filepath
    def fileText = text.replaceAll(pattern, replaceText)
    writeFile file: filepath, text: fileText
}

def getlastSuccessfulCommit(String jobUrl){
  String url = jobUrl + "/lastSuccessfulBuild/api/xml?xpath=//lastBuiltRevision/SHA1"
  def response = sh script: "curl --retry 5 $url | sed \"s@.*<SHA1>\\(.*\\)</SHA1>.*@\\1@\"", returnStdout: true
  return response.trim()
}
