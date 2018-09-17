FROM docker.apple.com/wwrc-docker-releases/pos-buildozer:j4.0 as builder
WORKDIR /code
ADD . /code
USER root
RUN source /tmp/.bash_profile && \
    mvn -s /tmp/settings.xml clean install -DskipTests -U && \
    openssl rand -base64 32 > artifactkey.bin && \
    openssl rsautl -encrypt -inkey /tmp/imagekey -pubin -in artifactkey.bin -out artifactkey.bin.enc && \
    openssl enc -aes-256-cbc -salt -in /code/service/target/wwrc-authorization-service-19.1.5-jar-with-dependencies.jar -out /code/service/target/wwrc-authorization-service-19.1.5-jar-with-dependencies.jar.enc -pass file:./artifactkey.bin

FROM docker.apple.com/wwrc_docker_releases/jamaica_serverjre:jserverjre8.0
ENV LANG C.UTF-8

WORKDIR /
COPY --from=builder /code/service/target/wwrc-authorization-service-19.1.5-jar-with-dependencies.jar.enc wwrc-authorization-service-19.1.5-jar-with-dependencies.jar.enc
COPY --from=builder /code/artifactkey.bin.enc artifactkey.bin.enc

#### Keystore and Truststore via ssl.properties
RUN touch sslinit.properties && \
   echo "wwrc.pos.secure=\"\$secure_flag\"" >> sslinit.properties && \
   echo "com.apple.ist.retail.pos.security.keyStoreFile=\"\$keystore_file_path\"" >> sslinit.properties && \
   echo "com.apple.ist.retail.pos.security.keyStorePassword=\"\$keystore_pass\"" >> sslinit.properties && \
   echo "com.apple.ist.retail.pos.security.trustStoreFile=\"\$truststore_file_path\"" >> sslinit.properties && \
   echo "com.apple.ist.retail.pos.security.trustStorePassword=\"\$truststore_pass\"" >> sslinit.properties  && \
   echo "com.apple.ist.retail.pos.security.keyAlias=\"\$key_alias\"" >> sslinit.properties

#### Main command
RUN touch startup.sh && echo "#!/bin/sh" >> startup.sh && \
    echo " eval echo \"\$privkey\" | base64 -d > privkey" >> startup.sh  && \
    echo "openssl rsautl -decrypt -inkey privkey -in artifactkey.bin.enc -out artifactkey.bin"  >> startup.sh && \
    echo "openssl rsautl -decrypt -inkey privkey -in eutilkey.bin.enc -out eutilkey.bin"  >> startup.sh && \
    echo "openssl enc -d -aes-256-cbc -in wwrc-authorization-service-19.1.5-jar-with-dependencies.jar.enc -out wwrc-authorization-service-19.1.5-jar-with-dependencies.jar -pass file:./artifactkey.bin" >> startup.sh && \
    echo "openssl enc -d -aes-256-cbc -in pos-encrypt-util-1.1.4.jar.enc -out pos-encrypt-util-1.1.4.jar -pass file:./eutilkey.bin" >> startup.sh && \
    echo "rm -rf privkey eutilkey.bin artifactkey.bin" >> startup.sh && \
    echo "cat sslinit.properties | while read line; do echo \$(eval echo \`echo \$line\`); done > ssl.properties" >> startup.sh && \
    echo "java -classpath pos-encrypt-util-1.1.4.jar:wwrc-authorization-service-19.1.5-jar-with-dependencies.jar \\" >> startup.sh && \
    echo "-Dwwrc.pos.ssl.config=ssl.properties -DbootStrapIp=\"\$event_central_ip\" \\" >> startup.sh && \
    echo "-Dcom.apple.wwrc.db.jdbcUrl=\"\$db_jdbcUrl\" \\" >> startup.sh && \
    echo "-Dcom.apple.wwrc.db.user=\"\$db_user\" \\" >> startup.sh && \
    echo "-Dcom.apple.wwrc.db.password=\"\$db_password\" \\" >> startup.sh && \
    echo "-Dcom.apple.wwrc.db.type=\"\$db_type\" \\" >> startup.sh && \
    echo "-Did_groups=POS \\" >> startup.sh && \
    echo "-Dlog4j.configurationFile=log4j2.xml \\" >> startup.sh && \
    echo "-Dservice.log.dir=logs \\" >> startup.sh && \
    echo "-DmonitoringDisabled=\"\$monitoring_disabled_flag\" \\" >> startup.sh && \
    echo "-DauthorizationDisabled=\"\$author_disabled_flag\" \\" >> startup.sh && \
    echo "-DRPC=\"\$rpc_flag\" \\" >> startup.sh && \
    echo "-DregistryDisabled=\"\$registry_disabled\" \\" >> startup.sh && \
    echo "com.apple.wwrc.foundation.framework.main.ServiceMain" >> startup.sh && \
    chmod 775 *.sh

CMD ./startup.sh
