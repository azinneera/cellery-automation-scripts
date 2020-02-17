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

def OBSERVABILITY_REPO = "https://github.com/wso2/cellery-observability.git"
def BRANCH = "master"

def jenkinsHome = JENKINS_HOME
def jenkinsHost = "https://wso2.org/jenkins"
def celleryProjectUrl = "$jenkinsHost/job/cellery"

def observabilityJobName = "cellery-observability"
def observabilityJobUrl = "$celleryProjectUrl/job/$observabilityJobName"

def DOCKER_REPO = "wso2cellery"
  
stage ("Build and Push docker images") {
  node ('Cellery-Ubuntu') {  
    ws(GOPATH + '/src/github.com/cellery-io/' + observabilityJobName) {
      deleteDir()
      
      if (ReleaseVersion != ""){
          VERSION = ReleaseVersion
          BRANCH = "v" + VERSION
      } else {
          VERSION = getlastSuccessfulCommit(observabilityJobUrl)
          BRANCH = VERSION
      }
      
      println("Cloning into cellery-observability")
      println("version: " + VERSION)
            
      checkout([$class: 'GitSCM', branches: [[name: BRANCH]], userRemoteConfigs: [[url: OBSERVABILITY_REPO]]])
      copyArtifacts(projectName: "../" + observabilityJobName, filter: 'docker/*/target/files/**')

      withDockerRegistry([ credentialsId: "DOCKERHUB_CELLERY_TEMP", url: "" ]) {
        sh """
            make clean.observability-agent check-style.observability-agent build.observability-agent
            DOCKER_REPO=${DOCKER_REPO} make docker-push 
            DOCKER_REPO=${DOCKER_REPO} DOCKER_IMAGE_TAG=${VERSION} make docker-push
        """
      } // end withDockerRegistry
    } // end ws
  } // end node
} // end stage

def getlastSuccessfulCommit(String jobUrl){
  String url = jobUrl + "/lastSuccessfulBuild/api/xml?xpath=//lastBuiltRevision/SHA1"
  def response = sh script: "curl --retry 5 $url | sed \"s@.*<SHA1>\\(.*\\)</SHA1>.*@\\1@\"", returnStdout: true
  return response.trim()
}
