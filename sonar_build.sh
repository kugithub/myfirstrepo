#!/bin/bash
export ArtifactoryRepo="com/apple/wwrc/foundation"
sonaraccesstoken=`cat $BUILD_SECRETS_PATH/ACCESSTOKEN`
sonarURL="https://rn-post-lapp1712.rno.apple.com:4005"
PROJECT="wwrc-authorization"
VER="$2"

sed -i "s/@SONARQUBE_ACCESS_TOKEN@/$sonaraccesstoken/g" /tmp/settings.xml
echo "########################################################################"
sed -i "s/@ENC_RIO_CHANGEME@/$var/g" /tmp/settings.xml
source /tmp/.bash_profile

mkdir -p .dist/${ArtifactoryRepo}/wwrc-authorization/$2
mkdir -p .dist/${ArtifactoryRepo}/wwrc-authorization-service/$2
mkdir -p .dist/${ArtifactoryRepo}/wwrc-authorization-testclient/$2


echo "######################## Running Maven dependency check ################################################"
mvn -s /tmp/settings.xml dependency-check:check
echo "######################## End Maven dependency check ################################################"

echo "######################## Running Mavensonar check ################################################"
mvn -s /tmp/settings.xml clean verify sonar:sonar -Dmaven.test.skip=false -Dsonar.branch=${RIO_BRANCH_NAME}-RIO
echo "######################## End Maven sonar check ################################################"

echo "######################## Running maven install ################################################"
mvn -s /tmp/settings.xml clean install -U -Dmaven.test.skip=true
echo "######################## end maven install ################################################"

#sleep 30
#qualityStatus=`curl -k -u $sonaraccesstoken: $sonarURL/api/qualitygates/project_status\?projectKey\=com.apple.wwrc:$1 |sed -e 's/[{}]/''/g' | awk -v k="text" '{n=split($0,a,","); for (i=1; i<=n; i++) print a[i]}' | grep projectStatus | awk -F ":" '{print $NF}'`
#if [ "$qualityStatus" = "\"OK\"" ]; then
#  echo "Build passes Qaulity check successfully"
#else
#  echo "Build fails quality check"
#  exit 1;
#fi;

cp -rf /home/builder/.m2/repository/${ArtifactoryRepo}/wwrc-authorization/$2  .dist/${ArtifactoryRepo}/wwrc-authorization
cp -rf /home/builder/.m2/repository/${ArtifactoryRepo}/wwrc-authorization-service/$2  .dist/${ArtifactoryRepo}/wwrc-authorization-service
cp -rf /home/builder/.m2/repository/${ArtifactoryRepo}/wwrc-authorization-testclient/$2 .dist/${ArtifactoryRepo}/wwrc-authorization-testclient
