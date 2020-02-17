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

def jenkinsHost = "https://wso2.org/jenkins"
def celleryProjectUrl = "$jenkinsHost/job/cellery"

def sdkJobName = "sdk"
def sdkReleaseVersion = SDKReleaseVersion

def filepath

stage ("Push installers to website") {
  node ("Cellery-Ubuntu"){
        dir("nightly-sync") {
           // deleteDir()
            //copyArtifacts(projectName: "../" + sdkJobName)
            def liveUser = "<username>"
            def liveHost = "<host_ip>"
            def deployOutputPath="/var/www/cellery"
            def scriptLocation="/home/" + liveUser
	        def metadataFile = "metadata.json"
            def metadataTpl = "/build/resources/metadata-release.json"

            sh """
              version=${sdkReleaseVersion}
                
              currentDate=`date +%Y-%m-%d`
              mkdir -p \$version
                
              linuxInstSize=`ls -lh installers/ubuntu-x64/target/cellery-ubuntu-x64-\$version.deb | cut -d " " -f5 | sed 's/M/mb/g'`
              macInstSize=`ls -lh installers/macOS-x64/target/pkg-signed/cellery-macos-installer-x64-\$version.pkg | cut -d " " -f5 | sed 's/M/mb/g'`
              
              mv installers/ubuntu-x64/target/cellery-ubuntu-x64-\$version.deb \$version/
              mv installers/macOS-x64/target/pkg-signed/cellery-macos-installer-x64-\$version.pkg \$version/
              

	        cp ${metadataTpl} ${metadataFile}

            sed -i "s/<version>/\$version/g" ${metadataFile}
            sed -i "s/<currentDate>/\$currentDate/g" ${metadataFile}
            sed -i "s/<linuxInstSize>/\$linuxInstSize/g" ${metadataFile}
            sed -i "s/<macInstSize>/\$macInstSize/g" ${metadataFile}

            mv ${metadataFile} \$version
                
              scp -P 1984 -r \$version/* ${liveUser}@${liveHost}:${deployOutputPath}
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
