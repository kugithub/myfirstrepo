#!/bin/bash
set -x
export ArtifactoryRepo="com/apple/wwrc/foundation"
sonaraccesstoken=`cat $BUILD_SECRETS_PATH/ACCESSTOKEN`
sonarURL="https://rn-post-lapp1712.rno.apple.com:4005"
PROJECT="wwrc-configuration-server"
VER="$2"

sed -i "s/@SONARQUBE_ACCESS_TOKEN@/$sonaraccesstoken/g" /tmp/settings.xml
echo "########################################################################"
sed -i "s/@ENC_RIO_CHANGEME@/$var/g" /tmp/settings.xml
source /tmp/.bash_profile

mkdir -p .dist/${ArtifactoryRepo}/wwrc-configuration-server/$2
mkdir -p .dist/${ArtifactoryRepo}/wwrc-configuration-server-service/$2
# mkdir -p .dist/${ArtifactoryRepo}/wwrc-configuration-server-testclient/$2

#echo "######################## Running Maven dependency check ################################################"
#mvn -s /tmp/settings.xml dependency-check:check
#echo "######################## End Maven dependency check ################################################"

echo "######################## Running Mavensonar check ################################################"
#mvn -s /tmp/settings.xml clean verify sonar:sonar -Dmaven.test.skip=false -Dsonar.branch=${RIO_BRANCH_NAME}-RIO
mvn -s /tmp/settings.xml clean verify sonar:sonar -Dmaven.test.skip=false  -Dsonar.projectKey=sonarpullanalysisdevops1 -Dsonar.projectName=sonarpullanalysisdevops1 -Dsonar.projectVersion=1.0 -Dsonar.github.repository=$GIT_URL -Dsonar.github.login=riosystem -Dsonar.github.oauth=14cc2a9bb0a6b19c8d8c94874e3875e61f72600e -Dsonar.verbose=true -Dsonar.analysis.mode=issues -Dsonar.github.pullRequest=1
echo "######################## End Maven sonar check ################################################"

#echo "######################## Running maven install  ################################################"
#mvn -s /tmp/settings.xml clean install -U -Dmaven.test.skip=true
#echo "######################## End maven install  ################################################"

#sleep 30
#qualityStatus=`curl -k -u $sonaraccesstoken: $sonarURL/api/qualitygates/project_status\?projectKey\=com.apple.wwrc:$1 |sed -e 's/[{}]/''/g' | awk -v k="text" '{n=split($0,a,","); for (i=1; i<=n; i++) print a[i]}' | grep projectStatus | awk -F ":" '{print $NF}'`
#if [ "$qualityStatus" = "\"OK\"" ]; then
#  echo "Build passes Qaulity check successfully"
#else
#  echo "Build fails quality check"
#  exit 1;
#fi;


#cp -rf /home/builder/.m2/repository/${ArtifactoryRepo}/wwrc-configuration-server/$2  .dist/${ArtifactoryRepo}/wwrc-configuration-server
#cp -rf /home/builder/.m2/repository/${ArtifactoryRepo}/wwrc-configuration-server-service/$2  .dist/${ArtifactoryRepo}/wwrc-configuration-server-service
