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

def DISTRIBUTION_REPO = "https://github.com/wso2/cellery-distribution"
def BRANCH = ""
def PACKER_VERSION = "0.6.0-SNAPSHOT"

def jenkinsHome = JENKINS_HOME
def jenkinsHost = "https://wso2.org/jenkins"
def celleryProjectUrl = "$jenkinsHost/job/cellery"

def distributionJobName = "cellery-distribution"
def distributionJobUrl = "$celleryProjectUrl/job/$distributionJobName"

def DOCKER_REPO = "wso2cellery"  

stage ("Build and Push docker images") {
     node ('Cellery-Ubuntu') {  
        dir(distributionJobName) {
                deleteDir()

                if (ReleaseVersion != ""){
                    VERSION = ReleaseVersion
                    BRANCH = "v" + VERSION
                } else {
                    VERSION = getlastSuccessfulCommit(distributionJobUrl) 
                    BRANCH = VERSION
                }

                println("Version: " + VERSION)
                println("Cloning cellery-distribution repository")
                checkout([$class: 'GitSCM', branches: [[name: BRANCH]],
                                userRemoteConfigs: [[url: DISTRIBUTION_REPO]]])

                withDockerRegistry([ credentialsId: "DOCKERHUB_CELLERY_TEMP", url: ""  ]) {
                //   copyArtifacts(projectName: "../" + distributionJobName, filter: 'docker/**/target/files/**')
                  sh """
                    DOCKER_REPO=${DOCKER_REPO} DOCKER_IMAGE_TAG=${VERSION} make docker-push
                    DOCKER_REPO=${DOCKER_REPO} DOCKER_IMAGE_TAG=latest make docker-push
                  """
                }
        } // end dir cellery-distribution
      } // end node
  parallel (
    'docker-build': {
      node ('Cellery-Ubuntu') {  
        dir(distributionJobName) {
                deleteDir()

                if (ReleaseVersion != ""){
                    VERSION = ReleaseVersion
                    BRANCH = "v" + VERSION
                } else {
                    VERSION = getlastSuccessfulCommit(distributionJobUrl) 
                    BRANCH = VERSION
                }

                println("Version: " + VERSION)
                println("Cloning cellery-distribution repository")
                checkout([$class: 'GitSCM', branches: [[name: BRANCH]],
                                userRemoteConfigs: [[url: DISTRIBUTION_REPO]]])

                withDockerRegistry([ credentialsId: "DOCKERHUB_CELLERY_TEMP", url: ""  ]) {
                  copyArtifacts(projectName: "../" + distributionJobName, filter: 'docker/**/target/files/**')
                  sh """
                    DOCKER_REPO=${DOCKER_REPO} DOCKER_IMAGE_TAG=${VERSION} make docker-push
                    DOCKER_REPO=${DOCKER_REPO} DOCKER_IMAGE_TAG=latest make docker-push
                  """
                }
        } // end dir cellery-distribution
      } // end node
    }, // end docker-build
    'packer-build': {  
      node ('Cellery-MAC') {
        withEnv(["PATH+EXTRA=/usr/local/aws/bin:/Applications/VirtualBox.app/Contents/MacOS"]){
          dir (distributionJobName) {
            deleteDir()

            if (ReleaseVersion != ""){
                    VERSION = ReleaseVersion
                    BRANCH = "v" + VERSION
                    PACKER_VERSION = VERSION
            } else {
                    VERSION = getlastSuccessfulCommit(distributionJobUrl) 
                    BRANCH = VERSION
            }

            println("Version: " + VERSION)
            println("Cloning cellery-distribution repository")
            checkout([$class: 'GitSCM', branches: [[name: BRANCH]],
                            userRemoteConfigs: [[url: DISTRIBUTION_REPO]]])
            
            dir('installer/packer/gapim-runtime-images') {
              sh """
              mkdir -p iso
              cp /build/jenkins-home/software/ubuntu-18.04.2-server-amd64.iso iso/ubuntu-18.04.2-server-amd64.iso
              /usr/local/packer build -var 'version=${PACKER_VERSION}' ubuntu.json
              """
              filename = sh (returnStdout: true, script: "basename kubectl_conf/config-cellery-runtime-complete*").trim()
              
              filepath = "kubectl_conf/" + filename
              patternToFind = /(certificate-authority-data:.+\n)/
              textToReplace = "insecure-skip-tls-verify: true\n"
              replaceText(filepath, patternToFind, textToReplace)
              
              patternToFind = /(server:.+\n)/
              textToReplace = "server: https://192.168.56.10:6443\n"
              replaceText(filepath, patternToFind, textToReplace)
              
              patternToFind = /(kubernetes)/
              textToReplace = "cellery"
              replaceText(filepath, patternToFind, textToReplace)

              sh """
              aws s3 cp VirtualBox/cellery-runtime-complete*.tar.gz s3://cellery-runtime-installation
              aws s3 cp ${filepath} s3://cellery-runtime-installation
              """
            } //end dir
            // cp /build/resources/ftp-upload.sh .
            //   set +x
            //   bash ftp-upload.sh complete
            //   set -x

            dir('installer/packer/minimum-runtime-images') {
              sh script: """
              mkdir -p iso
              cp /build/jenkins-home/software/ubuntu-18.04.2-server-amd64.iso iso/ubuntu-18.04.2-server-amd64.iso
              /usr/local/packer build -var 'version=${PACKER_VERSION}' ubuntu.json
              """
              filename = sh (returnStdout: true, script: "basename kubectl_conf/config-cellery-runtime-basic*").trim()
              
              filepath = "kubectl_conf/" + filename
              patternToFind = /(certificate-authority-data:.+\n)/
              textToReplace = "insecure-skip-tls-verify: true\n"
              replaceText(filepath, patternToFind, textToReplace)
              
              patternToFind = /(server:.+\n)/
              textToReplace = "server: https://192.168.56.10:6443\n"
              replaceText(filepath, patternToFind, textToReplace)
              
              patternToFind = /(kubernetes)/
              textToReplace = "cellery"
              replaceText(filepath, patternToFind, textToReplace)

              sh """
              aws s3 cp VirtualBox/cellery-runtime-basic*.tar.gz s3://cellery-runtime-installation
              aws s3 cp ${filepath} s3://cellery-runtime-installation
              """
            //   cp /build/resources/ftp-upload.sh .
            //   set +x
            //   bash ftp-upload.sh basic
            //   set -x
            } //end dir
          } //end dir
        } //end withEnv
      } //end node            
    } // end-packer-build
  ) // end parallel
} // end stage


/*
 * Replace string in file.

 * @param file file to replace text in
 * @param replaceText text to replace
 */
def replaceText(String filepath, String pattern, String replaceText) {
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
