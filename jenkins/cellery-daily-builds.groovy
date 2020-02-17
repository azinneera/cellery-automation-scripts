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
  
// stage ("Build and Push docker images") {
//   node ('Cellery-Ubuntu') {    
//     deleteDir()
//     println("Cloning mesh-observability and mesh-security repositories")

//     withDockerRegistry([ credentialsId: "CELLERY_DOCKER_CREDENTIALS", url: "https://docker.wso2.com/" ]) {
//       dir(observabilityJobName) {
//           observabilityLatestCommitSHA = getlastSuccessfulCommit(observabilityJobUrl) 
//           println("observabilityLatestCommitSHA: " + observabilityLatestCommitSHA)
          
//           checkout([$class: 'GitSCM', branches: [[name: observabilityLatestCommitSHA]],
//                       userRemoteConfigs: [[url: OBSERVABILITY_REPO]]])

//           copyArtifacts(projectName: observabilityJobName, filter: 'docker/*/target/files/**')

//           println("Building observability docker images")
//           dir('docker/portal') {
//             sh script: "docker build . -t ${DOCKER_REPO}/observability-portal:latest -t ${DOCKER_REPO}/observability-portal:${observabilityLatestCommitSHA}"
//           }
//           dir('docker/sp') {
//             sh script: "docker build . -t ${DOCKER_REPO}/sp-worker:latest -t ${DOCKER_REPO}/sp-worker:${observabilityLatestCommitSHA}"
//           }
//       } // end dir mesh-observability

//       dir(GOPATH + '/src/github.com/cellery-io/' + securityJobName) {
//         securityLatestCommitSHA = getlastSuccessfulCommit(securityJobUrl)
//         println("securityLatestCommitSHA: " + securityLatestCommitSHA)

//         checkout([$class: 'GitSCM', branches: [[name: securityLatestCommitSHA]],
//                       userRemoteConfigs: [[url: SECURITY_REPO]]])

//         copyArtifacts(projectName: securityJobName, filter: 'docker/*/target/files/**')
//         dir('docker/sts') {
//           sh script: "docker build . -t ${DOCKER_REPO}/cell-sts:latest -t ${DOCKER_REPO}/cell-sts:$securityLatestCommitSHA"
//           }
//       } // end dir mesh-security

//       dir(GOPATH + '/src/github.com/cellery-io/' + controllerJobName) {
//         controllerLatestCommitSHA = getlastSuccessfulCommit(controllerJobUrl)
//         println("controllerLatestCommitSHA: " + controllerLatestCommitSHA)

//         checkout([$class: 'GitSCM', branches: [[name: controllerLatestCommitSHA]],
//                       userRemoteConfigs: [[url: CONTROLLER_REPO]]])
//       } // end dir mesh-controller
//     } // endWithDockerRegistry

//       withDockerRegistry([ credentialsId: "DOCKERHUB_CELLERY_TEMP", url: "" ]) {
//         sh """
//         docker push ${DOCKER_REPO}/observability-portal:$observabilityLatestCommitSHA
//         docker push ${DOCKER_REPO}/observability-portal:latest
//         docker push ${DOCKER_REPO}/sp-worker:$observabilityLatestCommitSHA
//         docker push ${DOCKER_REPO}/sp-worker:latest
//         docker push ${DOCKER_REPO}/cell-sts:$securityLatestCommitSHA
//         docker push ${DOCKER_REPO}/cell-sts:latest
//         """
//         dir(GOPATH + '/src/github.com/cellery-io/' + controllerJobName) {
//           copyArtifacts(projectName: controllerJobName, filter: 'build/controller')
//           sh """
//             DOCKER_REPO=${DOCKER_REPO} DOCKER_IMAGE_TAG=latest make docker-push.controller artifacts
//             DOCKER_REPO=${DOCKER_REPO} make docker-push.controller artifacts
//           """
//         }

//         dir(GOPATH + '/src/github.com/cellery-io/' + securityJobName) {
//           sh """
//           DOCKER_REPO=${DOCKER_REPO} DOCKER_IMAGE_TAG=latest make docker-push.envoy-oidc-filter
//           DOCKER_REPO=${DOCKER_REPO} make docker-push.envoy-oidc-filter
//           """
//         }
//       }

//   } // end node
// } // end stage

stage ("Build distribution") {
  build job: distributionJobName
}

stage ("Build sdk") {
  build job: sdkJobName
}

// stage ("Build and Push docker and Cellery Runtime images") {
//   parallel (
//     'docker-build': {
//       node ('Cellery-Ubuntu') {
//           dir('distribution') {
//             deleteDir()
//             distributionLatestCommitSHA = getlastSuccessfulCommit(distributionJobUrl)
//             println("distributionLatestCommitSHA: " + distributionLatestCommitSHA)
//             println("Cloning distribution repository")
//             checkout([$class: 'GitSCM', branches: [[name: distributionLatestCommitSHA]],
//                             userRemoteConfigs: [[url: DISTRIBUTION_REPO]]])

//             withDockerRegistry([ credentialsId: "CELLERY_DOCKER_CREDENTIALS", url: "https://docker.wso2.com/" ]) {
//               copyArtifacts(projectName: distributionJobName, filter: 'docker/**/target/files/**')
//               dir('docker/global-apim') {
//                 sh script: "docker build . -t ${DOCKER_REPO}/wso2am:latest -t ${DOCKER_REPO}/wso2am:$distributionLatestCommitSHA"
//               }
//               dir('docker/microgateway/init-container') {
//                 sh script: "docker build . -t ${DOCKER_REPO}/cell-gateway-init:latest -t ${DOCKER_REPO}/cell-gateway-init:$distributionLatestCommitSHA"
//               }
//               dir('docker/microgateway/microgateway-container') {
//                 sh script: "docker build . -t ${DOCKER_REPO}/cell-gateway:latest -t ${DOCKER_REPO}/cell-gateway:$distributionLatestCommitSHA"
//               }
//               dir('docker/lightweight-idp') {
//                 sh "docker build . -t ${DOCKER_REPO}/wso2is-lightweight:latest -t ${DOCKER_REPO}/wso2is-lightweight:$distributionLatestCommitSHA"
//               }
//             }
//           } // end dir distribution

//           dir(GOPATH + '/src/github.com/cellery-io/' + sdkJobName) {
//             deleteDir()
//               sdkLatestCommitSHA = getlastSuccessfulCommit(sdkJobUrl)
//               println("sdkLatestCommitSHA: " + sdkLatestCommitSHA)

//               checkout([$class: 'GitSCM', branches: [[name: sdkLatestCommitSHA]],
//                             userRemoteConfigs: [[url: SDK_REPO]]])

//               copyArtifacts(
//                   projectName: sdkJobName, 
//                   filter: 'installers/ubuntu-x64/target/cellery-ubuntu-x64-*.deb')
//               dir('installers/docker') {

//                 filepath = "install.sh"
//                 patternToFind = /(cellery-ubuntu-x64-\S+)/
//                 textToReplace = "cellery-ubuntu-x64-" + sdkLatestCommitSHA.trim() + ".deb"
//                 replaceTagVersions(filepath, patternToFind, textToReplace)

//                 if (!fileExists(balInstallerPath)) {
//                     sh "curl --retry 5 https://product-dist.ballerina.io/downloads/$balVersion/$balInstallerFileName --output $balInstallerPath --create-dirs"
//                 }

//                 println("Building installer docker image")
//                 sh """
//                 mkdir -p files
//                 mv ../ubuntu-x64/target/cellery-ubuntu-x64-*.deb files/
//                 cp $balInstallerPath files/
//                 docker build . -t ${DOCKER_REPO}/cellery-cli:latest -t ${DOCKER_REPO}/cellery-cli:$sdkLatestCommitSHA 
//                 """
//               } //end dir
//             } // end dir

//           withDockerRegistry([ credentialsId: "DOCKERHUB_CELLERY_TEMP", url: "" ]) {
//               sh """
//               docker push ${DOCKER_REPO}/wso2am:$distributionLatestCommitSHA
//               docker push ${DOCKER_REPO}/wso2am:latest
//               docker push ${DOCKER_REPO}/cell-gateway-init:$distributionLatestCommitSHA
//               docker push ${DOCKER_REPO}/cell-gateway-init:latest
//               docker push ${DOCKER_REPO}/cell-gateway:$distributionLatestCommitSHA
//               docker push ${DOCKER_REPO}/cell-gateway:latest
//               docker push ${DOCKER_REPO}/wso2is-lightweight:$distributionLatestCommitSHA
//               docker push ${DOCKER_REPO}/wso2is-lightweight:latest
//               docker push ${DOCKER_REPO}/cellery-cli:$sdkLatestCommitSHA
//               docker push ${DOCKER_REPO}/cellery-cli:latest
//               """
//           } // end withDockerRegistry
//       } // end node

//     }, // end docker-build
//     'packer-build': {  
//       node ('Cellery-MAC') {
//         withEnv(["PATH+EXTRA=/usr/local/aws/bin:/Applications/VirtualBox.app/Contents/MacOS"]){
//           dir ('distribution') {
//             deleteDir()
//             distributionLatestCommitSHA = getlastSuccessfulCommit(distributionJobUrl)
//             println("distributionLatestCommitSHA: " + distributionLatestCommitSHA)
//             println("Cloning distribution repository")
//             checkout([$class: 'GitSCM', branches: [[name: distributionLatestCommitSHA]],
//                             userRemoteConfigs: [[url: DISTRIBUTION_REPO]]])
            
//             dir('installer/packer/gapim-runtime-images') {
//               sh """
//               mkdir -p iso
//               cp /build/jenkins-home/software/ubuntu-18.04.2-server-amd64.iso iso/ubuntu-18.04.2-server-amd64.iso
//               /usr/local/packer build -var 'version=0.2.0' ubuntu.json
//               """
//               filepath = "kubectl_conf/config-cellery-runtime-complete-0.2.0"
//               patternToFind = /(certificate-authority-data:.+\n)/
//               textToReplace = "insecure-skip-tls-verify: true\n"
//               replaceTagVersions(filepath, patternToFind, textToReplace)
              
//               patternToFind = /(server:.+\n)/
//               textToReplace = "server: https://192.168.56.10:6443\n"
//               replaceTagVersions(filepath, patternToFind, textToReplace)
              
//               patternToFind = /(kubernetes)/
//               textToReplace = "cellery"
//               replaceTagVersions(filepath, patternToFind, textToReplace)

//               sh """
//               aws s3 cp VirtualBox/cellery-runtime-complete*.tar.gz s3://cellery-runtime-installation-latest
//               aws s3 cp ${filepath} s3://cellery-runtime-installation-latest
//               """
//             } //end dir

//             dir('installer/packer/minimum-runtime-images') {
//               sh script: """
//               mkdir -p iso
//               cp /build/jenkins-home/software/ubuntu-18.04.2-server-amd64.iso iso/ubuntu-18.04.2-server-amd64.iso
//               /usr/local/packer build -var 'version=0.2.0' ubuntu.json
//               """
//               filepath = "kubectl_conf/config-cellery-runtime-basic-0.2.0"
//               patternToFind = /(certificate-authority-data:.+\n)/
//               textToReplace = "insecure-skip-tls-verify: true\n"
//               replaceTagVersions(filepath, patternToFind, textToReplace)
              
//               patternToFind = /(server:.+\n)/
//               textToReplace = "server: https://192.168.56.10:6443\n"
//               replaceTagVersions(filepath, patternToFind, textToReplace)
              
//               patternToFind = /(kubernetes)/
//               textToReplace = "cellery"
//               replaceTagVersions(filepath, patternToFind, textToReplace)

//               sh """
//               aws s3 cp VirtualBox/cellery-runtime-basic*.tar.gz s3://cellery-runtime-installation-latest
//               aws s3 cp ${filepath} s3://cellery-runtime-installation-latest
//               """
//             } //end dir
//           } //end dir
//         } //end withEnv
//       } //end node            
//     } // end-packer-build
//   ) // end parallel
// } // end stage

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
