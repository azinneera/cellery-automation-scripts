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

def securityJobName = "mesh-security"
def securityWorkspace = "$jenkinsHome/workspace/$securityJobName"
def securityJobUrl = "$celleryProjectUrl/job/$securityJobName"

def controllerJobName = "mesh-controller"
def controllerWorkspace = "$jenkinsHome/workspace/$controllerJobName"
def controllerJobUrl = "$celleryProjectUrl/job/$controllerJobName"

def distributionJobName = "cellery-distribution"
def distributionJobUrl = "$celleryProjectUrl/job/$distributionJobName"

def sdkJobName = "sdk"
def sdkJobUrl = "$celleryProjectUrl/job/$sdkJobName"

def samplesJobName = "samples"
def samplesJobUrl = "$celleryProjectUrl/job/$samplesJobName"

def observabilityReleaseVersion = ObservabilityReleaseVersion
def observabilityDevelopmentVersion = ObservabilityDevelopmentVersion

def securityReleaseVersion = SecurityReleaseVersion
def securityDevelopmentVersion = SecurityDevelopmentVersion

def controllerReleaseVersion = ControllerReleaseVersion

def distributionReleaseVersion = DistributionReleaseVersion
def distributionDevelopmentVersion = DistributionDevelopmentVersion

def DOCKER_REPO = "wso2cellery"

def apiResponse
def filepath
def patternToFind 
def textToReplace

stage ("Update distribution artifact versions for release") {
  node ('Cellery-Ubuntu'){ 
    if(ReleaseDistribution.toBoolean()==true) {
        println("Updating distribution artifact versions before release")
        // clean workspace
        deleteDir()
          
        dir('distribution') {
          println("Cloning distribution repo")
          checkout([$class: 'GitSCM', branches: [[name: "master"]],
                          userRemoteConfigs: [[url: DISTRIBUTION_REPO]]])
    
          println("Updating docker image versions")
          def filesStr = sh (returnStdout: true, script: "find installer/k8s-artefacts/ -name '*.yaml'").toString()
          filesStr.split('\n').each  {
              filepath = it
              
              patternToFind = /(${DOCKER_REPO}\/cell-gateway-init\n)/
              textToReplace = "${DOCKER_REPO}/cell-gateway-init:" + distributionReleaseVersion +"\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)
              
              patternToFind = /(${DOCKER_REPO}\/cell-gateway\n)/
              textToReplace = "${DOCKER_REPO}/cell-gateway:" + distributionReleaseVersion + "\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)
              
              patternToFind = /(${DOCKER_REPO}\/wso2am\n)/
              textToReplace = "${DOCKER_REPO}/wso2am:" + distributionReleaseVersion + "\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)
              
              patternToFind = /(${DOCKER_REPO}\/wso2is-lightweight\n)/
              textToReplace = "${DOCKER_REPO}/wso2is-lightweight:" + distributionReleaseVersion + "\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)
              
              patternToFind = /(${DOCKER_REPO}\/cell-sts\n)/
              textToReplace = "${DOCKER_REPO}/cell-sts:" + securityReleaseVersion + "\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)
              
              patternToFind = /(${DOCKER_REPO}\/envoy-oidc-filter\n)/
              textToReplace = "${DOCKER_REPO}/envoy-oidc-filter:" + securityReleaseVersion + "\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)
              
              patternToFind = /(${DOCKER_REPO}\/mesh-controller\n)/
              textToReplace = "${DOCKER_REPO}/mesh-controller:" + controllerReleaseVersion + "\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)
              
              patternToFind = /(${DOCKER_REPO}\/observability-portal\n)/
              textToReplace = "${DOCKER_REPO}/observability-portal:" + observabilityReleaseVersion + "\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)
              
              patternToFind = /(${DOCKER_REPO}\/sp-worker\n)/
              textToReplace = "${DOCKER_REPO}/sp-worker:" + observabilityReleaseVersion + "\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)

              patternToFind = /(${DOCKER_REPO}\/jwks-server\n)/
              textToReplace = "${DOCKER_REPO}/jwks-server:" + securityReleaseVersion + "\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)

              patternToFind = /(${DOCKER_REPO}\/api-publisher\n)/
              textToReplace = "${DOCKER_REPO}/api-publisher:" + distributionReleaseVersion + "\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)

              patternToFind = /(${DOCKER_REPO}\/telemetry-agent\n)/
              textToReplace = "${DOCKER_REPO}/telemetry-agent:" + observabilityReleaseVersion + "\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)

              patternToFind = /(${DOCKER_REPO}\/tracing-agent\n)/
              textToReplace = "${DOCKER_REPO}/tracing-agent:" + observabilityReleaseVersion + "\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)

              patternToFind = /(${DOCKER_REPO}\/mesh-webhook\n)/
              textToReplace = "${DOCKER_REPO}/mesh-webhook:" + controllerReleaseVersion + "\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)

              patternToFind = /(${DOCKER_REPO}\/kube-agent\n)/
              textToReplace = "${DOCKER_REPO}/kube-agent:" + observabilityReleaseVersion + "\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)
     
          }
    
          filepath = "pom.xml"
          patternToFind = /(<cellery\.mesh\.security\.version>.+\n)/
          textToReplace = "<cellery.mesh.security.version>" + securityReleaseVersion + "</cellery.mesh.security.version>\n"
          replaceTagVersions(filepath, patternToFind, textToReplace)
    
          patternToFind = /(<cellery\.mesh\.obesrvability\.version>.+\n)/
          textToReplace = "<cellery.mesh.obesrvability.version>" + observabilityReleaseVersion + "</cellery.mesh.obesrvability.version>\n"
          replaceTagVersions(filepath, patternToFind, textToReplace)
    
          filepath = "installer/packer/gapim-runtime-images/script/cellery.sh"
          patternToFind = /(release_version=\S+\n)/
          textToReplace = "release_version=" + distributionReleaseVersion + "\n"
          replaceTagVersions(filepath, patternToFind, textToReplace)
          
          patternToFind = /(release_archive_version=\S+\n)/
          textToReplace = "release_archive_version=v" + distributionReleaseVersion + "\n"
          replaceTagVersions(filepath, patternToFind, textToReplace)
    
          filepath = "installer/packer/gapim-runtime-images/ubuntu.json"
          patternToFind = /(\"version\": \"[\.0-9]{1,8}-SNAPSHOT\")/
          textToReplace = "\"version\": \"" + distributionReleaseVersion + "\""
          replaceTagVersions(filepath, patternToFind, textToReplace)
    
          filepath = "installer/packer/minimum-runtime-images/script/cellery.sh"
          patternToFind = /(release_version=\S+\n)/
          textToReplace = "release_version=" + distributionReleaseVersion + "\n"
          replaceTagVersions(filepath, patternToFind, textToReplace)
          
          patternToFind = /(release_archive_version=\S+\n)/
          textToReplace = "release_archive_version=v" + distributionReleaseVersion + "\n"
          replaceTagVersions(filepath, patternToFind, textToReplace)
    
          filepath = "installer/packer/minimum-runtime-images/ubuntu.json"
          patternToFind = /(\"version\": \"[\.0-9]{1,8}-SNAPSHOT\")/
          textToReplace = "\"version\": \"" + distributionReleaseVersion + "\""
          replaceTagVersions(filepath, patternToFind, textToReplace)
          
    
          withCredentials([usernamePassword(credentialsId: '4ff4a55b-1313-45da-8cbf-b2e100b1accd', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
             sh """
             set +x
             git config --global user.email "jenkins-bot@wso2.com"
             git config --global user.name "WSO2 Builder"
             set -x
             git checkout master
             git add .
             git diff
             git commit -m \"Update artifact versions for release v${distributionReleaseVersion}\"
             git pull --rebase origin master
             git push -f https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/wso2-cellery/distribution
             """
          }
        }
    }
  } // end node
} // end stage

stage ("Release distribution") {
  node ('Cellery-Ubuntu') {
      if(ReleaseDistribution.toBoolean()==true) {
      print("Distribution release " + distributionReleaseVersion)
      performM2Release(distributionJobUrl, distributionReleaseVersion, distributionDevelopmentVersion, "io.cellery.distribution:io.cellery.distribution.parent")         
      }
  } // end node
}

stage ("Update distribution artifact versions for next development iteration") {
  node ('Cellery-Ubuntu'){ 
    if(ReleaseDistribution.toBoolean()==true) {
        println("Updating distribution artifact versions after release")
        // clean workspace
        deleteDir()
          
        dir('distribution') {
          println("Cloning distribution repo")
          checkout([$class: 'GitSCM', branches: [[name: "master"]],
                          userRemoteConfigs: [[url: DISTRIBUTION_REPO]]])
    
          println("Updating docker image versions")
    
          def filesStr = sh (returnStdout: true, script: "find installer/k8s-artefacts/ -name '*.yaml'").toString()
          filesStr.split('\n').each  {
              filepath = it
              
              patternToFind = /(${DOCKER_REPO}\/cell-gateway-init:\S+\n)/
              textToReplace = "${DOCKER_REPO}/cell-gateway-init\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)
              
              patternToFind = /(${DOCKER_REPO}\/cell-gateway:\S+\n)/
              textToReplace = "${DOCKER_REPO}/cell-gateway\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)
              
              patternToFind = /(${DOCKER_REPO}\/wso2am:\S+\n)/
              textToReplace = "${DOCKER_REPO}/wso2am\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)
              
              patternToFind = /(${DOCKER_REPO}\/wso2is-lightweight:\S+\n)/
              textToReplace = "${DOCKER_REPO}/wso2is-lightweight\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)
              
              patternToFind = /(${DOCKER_REPO}\/cell-sts:\S+\n)/
              textToReplace = "${DOCKER_REPO}/cell-sts\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)
              
              patternToFind = /(${DOCKER_REPO}\/envoy-oidc-filter:\S+\n)/
              textToReplace = "${DOCKER_REPO}/envoy-oidc-filter\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)
              
              patternToFind = /(${DOCKER_REPO}\/mesh-controller:\S+\n)/
              textToReplace = "${DOCKER_REPO}/mesh-controller\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)
              
              patternToFind = /(${DOCKER_REPO}\/observability-portal:\S+\n)/
              textToReplace = "${DOCKER_REPO}/observability-portal\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)
              
              patternToFind = /(${DOCKER_REPO}\/sp-worker:\S+\n)/
              textToReplace = "${DOCKER_REPO}/sp-worker\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)

              patternToFind = /(${DOCKER_REPO}\/jwks-server:\S+\n)/ 
              textToReplace = "${DOCKER_REPO}/jwks-server\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)

              patternToFind = /(${DOCKER_REPO}\/api-publisher:\S+\n)/ 
              textToReplace = "${DOCKER_REPO}/api-publisher\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)

              patternToFind = /(${DOCKER_REPO}\/telemetry-agent:\S+\n)/ 
              textToReplace = "${DOCKER_REPO}/telemetry-agent\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)

              patternToFind = /(${DOCKER_REPO}\/tracing-agent:\S+\n)/ 
              textToReplace = "${DOCKER_REPO}/tracing-agent\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)
                
              patternToFind = /(${DOCKER_REPO}\/mesh-webhook:\S+\n)/ 
              textToReplace = "${DOCKER_REPO}/mesh-webhook\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)

              patternToFind = /(${DOCKER_REPO}\/kube-agent:\S+\n)/ 
              textToReplace = "${DOCKER_REPO}/kube-agent\n"
              replaceTagVersions(filepath, patternToFind, textToReplace)
          }
    
          filepath = "pom.xml"
          patternToFind = /(<cellery\.mesh\.security\.version>.+\n)/
          textToReplace = "<cellery.mesh.security.version>" + securityDevelopmentVersion + "</cellery.mesh.security.version>\n"
          replaceTagVersions(filepath, patternToFind, textToReplace)
    
          patternToFind = /(<cellery\.mesh\.obesrvability\.version>.+\n)/
          textToReplace = "<cellery.mesh.obesrvability.version>" + observabilityDevelopmentVersion + "</cellery.mesh.obesrvability.version>\n"
          replaceTagVersions(filepath, patternToFind, textToReplace)
    
          filepath = "installer/packer/gapim-runtime-images/script/cellery.sh"
          patternToFind = /(release_version=\S+\n)/
          textToReplace = "release_version=master\n"
          replaceTagVersions(filepath, patternToFind, textToReplace)
         
          patternToFind = /(release_archive_version=\S+\n)/
          textToReplace = "release_archive_version=master\n"
          replaceTagVersions(filepath, patternToFind, textToReplace)
    
          filepath = "installer/packer/gapim-runtime-images/ubuntu.json"
          patternToFind = /(\"version\": \"[\.0-9]{1,8}\")/
          textToReplace = "\"version\": \"" + distributionDevelopmentVersion + "\""
          replaceTagVersions(filepath, patternToFind, textToReplace)
    
          filepath = "installer/packer/minimum-runtime-images/script/cellery.sh"
          patternToFind = /(release_version=\S+\n)/
          textToReplace = "release_version=master\n"
          replaceTagVersions(filepath, patternToFind, textToReplace) 
          
          patternToFind = /(release_archive_version=\S+\n)/
          textToReplace = "release_archive_version=master\n"
          replaceTagVersions(filepath, patternToFind, textToReplace)
    
          filepath = "installer/packer/minimum-runtime-images/ubuntu.json"
          patternToFind = /(\"version\": \"[\.0-9]{1,8}\")/
          textToReplace = "\"version\": \"" + distributionDevelopmentVersion + "\""
          replaceTagVersions(filepath, patternToFind, textToReplace)
    
          withCredentials([usernamePassword(credentialsId: '4ff4a55b-1313-45da-8cbf-b2e100b1accd', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
             sh """
             set +x
             git config --global user.email "jenkins-bot@wso2.com"
             git config --global user.name "WSO2 Builder"
             set -x
             git checkout master
             git add .
             git commit -m \"Update artifact versions for next development iteration\"
             git pull --rebase origin master
             git push -f https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/wso2-cellery/distribution
             """
            //  git push -f https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/wso2-cellery/distribution
          }
        }
    }
  } // end node
} // end stage


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
    //   "&closeNexusStage=on" +
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
