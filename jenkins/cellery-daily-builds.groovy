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
def SECURITY_REPO = "https://github.com/wso2/cellery-security.git"
def CONTROLLER_REPO = "https://github.com/wso2/cellery-controller.git"
def DISTRIBUTION_REPO = "https://github.com/wso2/cellery-distribution.git"
def SDK_REPO = "https://github.com/wso2/cellery.git"

def jenkinsHome = JENKINS_HOME
def jenkinsHost = "https://wso2.org/jenkins"
def celleryProjectUrl = "$jenkinsHost/job/cellery"

def observabilityJobName = "cellery-observability"
def observabilityWorkspace = "$jenkinsHome/workspace/$observabilityJobName"
def observabilityJobUrl = "$celleryProjectUrl/job/$observabilityJobName"

def securityJobName = "cellery-security"
def securityWorkspace = "$jenkinsHome/workspace/$securityJobName"
def securityJobUrl = "$celleryProjectUrl/job/$securityJobName"

def controllerJobName = "cellery-controller"
def controllerWorkspace = "$jenkinsHome/workspace/$controllerJobName"
def controllerJobUrl = "$celleryProjectUrl/job/$controllerJobName"

def distributionJobName = "cellery-distribution"
def distributionJobUrl = "$celleryProjectUrl/job/$distributionJobName"

def sdkJobName = "sdk"
def sdkJobUrl = "$celleryProjectUrl/job/$sdkJobName"

def observabilityLatestCommitSHA = ""
def securityLatestCommitSHA = ""
def controllerLatestCommitSHA = ""
def distributionLatestCommitSHA = ""
def sdkLatestCommitSHA = ""

def DOCKER_REPO = "wso2cellery"

def apiResponse
def filepath
def patternToFind 
def textToReplace

def balVersion = "0.990.3"
def balInstallerFileName = "ballerina-linux-installer-x64-${balVersion}.deb"
def balInstallerPath = "/build/jenkins-home/software/$balInstallerFileName"
  
stage ('Build mesh-observability, mesh-security and mesh-controller repositories') {
      // Build Observability, Security and Controller in parallel
      parallel (
          'mesh-observability': {
            build job: observabilityJobName
          },
          'mesh-security': {  
            build job: securityJobName            
          },
        /* Moved to GitActions */
          'mesh-controller': { 
            build job: controllerJobName
          }
      ) // end parallel
} //end stage

stage ("Build distribution") {
  build job: distributionJobName
}

stage ("Build sdk") {
  build job: sdkJobName
}

stage ("Push installers to website") {
    node ("Cellery-Ubuntu"){
    dir("nightly-sync") {
        deleteDir()
        copyArtifacts(projectName: sdkJobName)
        def liveUser = "<username>"
        def liveHost = "<host_ip>"
        sdkLatestCommitSHA = getlastSuccessfulCommit(sdkJobUrl)
        def deployOutputPath="cellery-release-artifacts/nightlies"
        def medadataFile = "metadata.json"
        def metadataTpl = "/build/resources/" + medadataFile
        
        sh """
            version=0.6.0
            
            currentDate=`date +%Y-%m-%d`
            mkdir -p \$currentDate/\$version
            
        	linuxInstSize=`ls -lh installers/ubuntu-x64/target/cellery-ubuntu-x64-${sdkLatestCommitSHA}.deb | cut -d " " -f5 | sed 's/M/mb/g'`
        	macInstSize=`ls -lh installers/macOS-x64/target/pkg-signed/cellery-macos-installer-x64-${sdkLatestCommitSHA}.pkg | cut -d " " -f5 | sed 's/M/mb/g'`
        	
        	mv installers/ubuntu-x64/target/cellery-ubuntu-x64-${sdkLatestCommitSHA}.deb \$currentDate/\$version/cellery-ubuntu-x64-\$version-\$currentDate.deb
            mv installers/macOS-x64/target/pkg-signed/cellery-macos-installer-x64-${sdkLatestCommitSHA}.pkg \$currentDate/\$version/cellery-macos-installer-x64-\$version-\$currentDate.pkg
        	
        	cp ${metadataTpl} .
        	
            sed -i "s/<version>/\$version/g" ${medadataFile}
            sed -i "s/<currentDate>/\$currentDate/g" ${medadataFile}
            sed -i "s/<linuxInstSize>/\$linuxInstSize/g" ${medadataFile}
            sed -i "s/<macInstSize>/\$macInstSize/g" ${medadataFile}

            mv ${medadataFile} \$currentDate/\$version
            
            scp -P 1984 -r \$currentDate ${liveUser}@${liveHost}:${deployOutputPath}
            ssh -p 1984 ${liveUser}@$liveHost "cd ${deployOutputPath} && ./nightly-deploy.sh \$currentDate \$version"
        """
    } // end dir
  } // end node
}
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
