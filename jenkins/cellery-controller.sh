#!/bin/bash

# Inject environment variables to the build process
# GO111MODULE=on
# GOPATH=/home/ubuntu/go

set -x
mkdir -p $GOPATH/bin
mkdir -p $GOPATH/pkg

make build

# Push docker images

docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD

if [[ -v ReleaseVersion ]] ; then
  DOCKER_REPO=wso2cellery DOCKER_IMAGE_TAG=$ReleaseVersion make docker-push artifacts;
else

  DOCKER_REPO=wso2cellery DOCKER_IMAGE_TAG=latest make docker-push artifacts
  DOCKER_REPO=wso2cellery make docker-push artifacts;
fi


# Create release
set -x
set -e

if [[ -v ReleaseVersion ]] ; then

	VERSION=v$ReleaseVersion
    repo=wso2/cellery-controller
        
    response=$(curl --retry 5 -k -s -o /dev/null -w '%{http_code}' -u $GIT_BOT_CREDENTIALS  \
             -d '{"tag_name": "'$VERSION'", "name":"Cellery Mesh Controller Release '$VERSION'","body":"Cellery Controller version '$VERSION' released! ", "prerelease": '$isPreRelease'}'  \
             "https://api.github.com/repos/$repo/releases")
             
	if [ $response != 201 ]; then
    	echo "Failed to create release on github"
    	exit 1
    fi
    
fi


# files to archive - build/controller
