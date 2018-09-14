#!/bin/bash

OPT_PORT="$1"

function _get_pom_version {
   VER=`mvn -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive exec:exec`
   echo $VER
}

##### MAIN #####
VER=$(_get_pom_version)
java -DoptionalCertAuth=true \
     -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5560 \
     -Dwwrc.pos.ssl.config=$HOME/ssl.properties \
     -DbootStrapIp=ws://localhost:9000/rcm \
     -Did_groups=POS \
     -DregistryDisabled=true \
     -DauthorizationDisabled=true \
     -DmonitoringDisabled=true \
     -classpath $HOME/pos-encrypt-util-1.1.4.jar:service/target/wwrc-customer-service-$VER-jar-with-dependencies.jar com.apple.wwrc.foundation.framework.main.ServiceMain
