#!/bin/bash


set -x
set -e

git checkout $Branch

if [[ -v ReleaseVersion ]] ; then
	VERSION=$ReleaseVersion	
else	
    VERSION=latest
fi

 VERSION=$VERSION make clean init check-style build test docker docker-push


# Perform release if this is a release build

if [[ -v ReleaseVersion ]] ; then

	VERSION=v$ReleaseVersion
    repo=wso2/cellery-hub
    branch=$Branch
        
    response=$(curl --retry 5 -k -s -o /dev/null -w '%{http_code}' -u $GIT_BOT_CREDENTIALS  \
             -d '{"tag_name": "'$VERSION'", "target_commitish": "'$branch'", "name":"Cellery Hub Release '$VERSION'","body":"Cellery Hub release version '$VERSION' released! ", "prerelease": '$isPreRelease'}'  \
             "https://api.github.com/repos/$repo/releases")
             
	if [ $response != 201 ]; then
    	echo "Failed to create release on github"
    	exit 1
    fi
    
fi

