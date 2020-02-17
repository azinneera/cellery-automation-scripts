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

def VERSION
def SDK_REPO = "https://github.com/wso2/cellery"
def BRANCH = Branch
def BALLERINA_DEB = "ballerina-linux-installer-x64-1.0.3.deb"
def GIT_REVISION

stage ('Build intallers') {

   parallel (
     'ubuntu': {
      node ('Cellery-Ubuntu') {
         withEnv(['JAVA_HOME=/build/jenkins-home/software/jdk1.8']) {
         ws ("/usr/local/go/src/cellery.io/cellery") {
          deleteDir()
          checkout([$class: 'GitSCM', branches: [[name: BRANCH]],
                      userRemoteConfigs: [[url: SDK_REPO]]])

             if (ReleaseVersion != ""){
              VERSION = ReleaseVersion
              INST_VERSION = ReleaseVersion
              DISTRIBUTION_VERSION = "0.6.0"
              DISTRIBUTION_ARCHIVE_VERSION = "v0.6.0"
              OBSERVABILITY_BUILD="lastRelease" 
             } else {
                GIT_REVISION = sh (script: "git rev-parse --verify HEAD", returnStdout: true).trim()
                INST_VERSION =  GIT_REVISION
                VERSION="0.6.0-SNAPSHOT"
                DISTRIBUTION_VERSION = "master"
                DISTRIBUTION_ARCHIVE_VERSION = "master"
                OBSERVABILITY_BUILD="lastSuccessfulBuild" 
             }
             sh """
             VERSION=${VERSION} INSTALLER_VERSION=${INST_VERSION} OBSERVABILITY_BUILD=${OBSERVABILITY_BUILD} DISTRIBUTION_VERSION=${DISTRIBUTION_VERSION} DISTRIBUTION_ARCHIVE_VERSION=${DISTRIBUTION_ARCHIVE_VERSION} make build-ubuntu-installer
             """
             archiveArtifacts 'installers/ubuntu-x64/target/cellery-ubuntu-x64-*.deb'
             
             withDockerRegistry([ credentialsId: "DOCKERHUB_CELLERY_TEMP", url: "" ]) {
              sh script: """
              cp /build/resources/${BALLERINA_DEB} .
              VERSION=${VERSION} make docker-push
              """ 
             } // end withDockerRegistry
             
         } // end ws
         }
      } // end node
     },
     'macOS': {  
       node ('Cellery-MAC') {
         ws ('/Users/wso2/build/jenkins-home/goProjects/src/cellery.io/cellery') {
           deleteDir()
           checkout([$class: 'GitSCM', branches: [[name: BRANCH]],
                       userRemoteConfigs: [[url: SDK_REPO]]])

             if (ReleaseVersion != ""){
               VERSION = ReleaseVersion
               INST_VERSION = ReleaseVersion
               DISTRIBUTION_VERSION = "0.6.0"
               DISTRIBUTION_ARCHIVE_VERSION = "v0.6.0"
               OBSERVABILITY_BUILD="lastRelease"
             } else {
              VERSION="0.6.0-SNAPSHOT"
              GIT_REVISION = sh (script: "git rev-parse --verify HEAD", returnStdout: true).trim()
              INST_VERSION =  GIT_REVISION
              DISTRIBUTION_VERSION = "master"
              DISTRIBUTION_ARCHIVE_VERSION = "master"
              OBSERVABILITY_BUILD="lastSuccessfulBuild"
             }

             patternToFind = /(\<ADD ID HERE\>)/
             textToReplace = "WSO2, Inc. (QH8DVR4443)"
             replaceText("installers/macOS-x64/build-macos-x64.sh", patternToFind, textToReplace)
            
             sh """
              PATH=/usr/local/aws/bin:/Library/Ballerina/ballerina-0.991.0/bin:/usr/local/nodejs/bin:\${PATH}
              set +x
              security unlock -p <password> ~/Library/Keychains/login.keychain
              set -x
              go version
              VERSION=${VERSION} INSTALLER_VERSION=${INST_VERSION} OBSERVABILITY_BUILD=${OBSERVABILITY_BUILD} DISTRIBUTION_VERSION=${DISTRIBUTION_VERSION} DISTRIBUTION_ARCHIVE_VERSION=${DISTRIBUTION_ARCHIVE_VERSION} make build-mac-installer
             """
            
             archiveArtifacts 'installers/macOS-x64/target/pkg-signed/**'
         } // end ws
       } // end node
     } // end macOS
   ) // end parallel
  if (ReleaseVersion != "") {
    def repo="wso2/cellery"
    def uploadUrl
    def filename

    node ('Cellery-MAC') {
      dir ("/Users/wso2/build/jenkins-home/goProjects/src/cellery.io/cellery/installers") {
        filename = sh (returnStdout: true, script: "basename macOS-x64/target/pkg-signed/cellery-macos-installer-x64-*.pkg").trim()

        withCredentials([usernamePassword(credentialsId: '4ff4a55b-1313-45da-8cbf-b2e100b1accd', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
            
            def response = sh returnStdout: true, 
          script: "curl --retry 5 -s -u ${GIT_USERNAME}:${GIT_PASSWORD} " +
          "-d '{\"tag_name\": \"v${ReleaseVersion}\", \"target_commitish\": \"${BRANCH}\", \"name\":\"Cellery SDK Release v${ReleaseVersion}\",\"body\":\"Cellery SDK version v${ReleaseVersion} released! \",\"prerelease\": true}' " +
          "https://api.github.com/repos/${repo}/releases"

          uploadUrl = getUploadUrl(response)

        sh returnStdout: true, 
        script: "curl -s -H \"Content-Type: application/octet-stream\" -u ${GIT_USERNAME}:${GIT_PASSWORD} " +
        "--data-binary @macOS-x64/target/pkg-signed/${filename} " +
        "${uploadUrl}?name=${filename}\\&label=${filename}"
        } //end withCredentials
      } // end dir
    } // edn node
    
    node ('Cellery-Ubuntu') {
      dir ("/usr/local/go/src/cellery.io/cellery/installers") {
        filename = sh (returnStdout: true, script: "basename ubuntu-x64/target/cellery-ubuntu-x64-*.deb").trim()
        
        withCredentials([usernamePassword(credentialsId: '4ff4a55b-1313-45da-8cbf-b2e100b1accd', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {

          sh returnStdout: true, 
          script: "curl -s -H \"Content-Type: application/octet-stream\" -u ${GIT_USERNAME}:${GIT_PASSWORD} " +
          "--data-binary @ubuntu-x64/target/${filename} " +
          "${uploadUrl}?name=${filename}\\&label=${filename}"
        } //end withCredentials
      } // end dir
    } // edn node
      
  } //end if
} //end stage


/*
 * Replace string in file.

 * @param file file to replace text in
 * @param replaceText text to replace
 */
def replaceText(String filepath, String pattern, String replaceText) {
  def text = readFile filepath
  def isExists = fileExists filepath
  if (isExists){
    def fileText = text.replaceAll(pattern, replaceText)
    writeFile file: filepath, text: fileText
  } else {
    println("WARN: " + filepath + "file not found")
  }
}

/*
 * Get endpoint to upload release assets by parsing the response.

 * @param response json response to parse and get upload_url
 */
@NonCPS
def getUploadUrl(response) {
  JsonSlurper slurper = new JsonSlurper();
  def data = slurper.parseText(response)
  def upload_url = data.upload_url
  return upload_url.substring(0, upload_url.lastIndexOf("{"))
}
