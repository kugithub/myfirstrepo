#!/bin/bash

HOME=`echo $HOME`
REPO=`pwd`
VER="$1"
DEPLOYMENT="$2"
BRANCH_NM=`git branch | grep '*' | awk '{print $2}'`

mkdir -p $REPO/build/
cp $REPO/service/target/wwrc-configuration-server-service-$VER-jar-with-dependencies.jar $REPO/build/

### Either vBump Docker.prod, Docker.dev and Docker.dev.debug
if   [ "$DEPLOYMENT" == '--prod' ];then
	echo
	echo "=============== PRODUCT MODE ================="
    sed "s/@VERSION@/${VER}/g" $REPO/dockers/Dockerfile.prod > $REPO/Dockerfile
    sed "s/TAG_VERSION/${VER}/g; s/BRANCH_NM/${BRANCH_NM}/g" $REPO/dockers/rio.tmpl > $REPO/rio.yaml
elif [ "$DEPLOYMENT" == '--dev' ];then
	echo
	echo "=============== DEV MODE ================="
    sed "s/@VERSION@/${VER}/g" $REPO/dockers/Dockerfile.dev > $REPO/dockers/Dockerfile
elif [ "$DEPLOYMENT" == '--debug' ];then
	echo
	echo "=============== DEBUG MODE ================="
    sed "s/@VERSION@/${VER}/g" $REPO/dockers/Dockerfile.dev.debug > $REPO/dockers/Dockerfile
else
	echo
	echo "=============== DEV MODE ================="
    sed "s/@VERSION@/${VER}/g" $REPO/dockers/Dockerfile.dev > $REPO/dockers/Dockerfile
fi

### Run docker build if not in the prod mode
if [ "$DEPLOYMENT" == '--prod' ];then
    echo "Ready to check in. Below is your Dockerfile"
    echo
    cat $REPO/Dockerfile
else
	(docker build . -t config-server:$VER -f dockers/Dockerfile)
fi

