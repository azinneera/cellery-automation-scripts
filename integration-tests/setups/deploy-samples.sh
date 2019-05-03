#!/bin/bash

#  Copyright (c) 2019 WSO2 Inc. (http:www.wso2.org) All Rights Reserved.
#
#  WSO2 Inc. licenses this file to you under the Apache License,
#  Version 2.0 (the "License"); you may not use this file except
#  in compliance with the License.
#  You may obtain a copy of the License at
#
#  http:www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.

version="0.2.0"

log_info() {
    echo "${log_prefix}[INFO]" $1
}  

deployHelloWorldSample() {
	cd $samples_root/hello-world
	cellery build hello-world.bal $docker_hub_org/hello-world-cell:$version
	cellery run $docker_hub_org/hello-world-cell:$version -n hello-world-cell -y
	cellery run $docker_hub_org/hello-world-cell:$version -e VHOST_NAME=my-hello-world.com -e HELLO_NAME=WSO2 -n my-hello-world -y
	cellery list instances
}

assert () {              #  If condition false, exit from script with error message.
                         
  	E_PARAM_ERR=98
  	E_ASSERT_FAILED=99
 
  	if [ -z "$2" ]          
  	then
    	return $E_PARAM_ERR   # Not enough parameters passed.
  	fi
 
  	lineno=$2
 
  	if [ ! $1 ] 
  	then
    	echo "Assertion failed:  \"$1\""
    	echo "File \"$0\", line $lineno"
    exit $E_ASSERT_FAILED # Discontinue execution of script
  fi 
} 

log_info "Cloning Samples Repo..."
git clone https://github.com/wso2-cellery/samples.git

deployHelloWorldSample

echo "192.168.56.10  hello-world.com  my-hello-world.com" | sudo tee -a /etc/hosts 

condition=$(curl -s --head  --request GET http://my-hello-world.com | grep "200 OK" > /dev/null)==true
assert "$condition" $LINENO

exit 0