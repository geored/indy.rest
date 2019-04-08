FROM indyperf/jdk.mvn.npm:1.0

RUN apt-get -y update && apt-get -y upgrade

ADD . /opt/indy

RUN cd /opt/indy && \
    mvn clean -DskipTests=true install && \
    cd /opt/indy/deployments/launcher/target && \
    tar -xvf indy-launcher-1.7.4-SNAPSHOT-complete.tar.gz

WORKDIR /opt/indy/deployments/launcher/target/indy

RUN chgrp -R 0 /opt/indy && \
    chmod -R g=u /opt/indy && \
    cd /opt/indy/deployments/launcher/target/indy


ENV JAVA_MAJOR_VERSION 8
ENV JAVA_APP_DIR /opt/indy/deployments/launcher/target/indy
ENV JAVA_LIB_DIR /opt/indy/deployments/launcher/target/indy/lib
ENV JAVA_OPTIONS "-Dindy.home=. -Dorg.jboss.logging.provider=sl4j -Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2"
ENV JAVA_CLASSPATH ${JAVA_LIB_DIR}/indy-embedder-1.7.4-SNAPSHOT.jar
ENV JAVA_CLASSPATH $JAVA_CLASSPATH:$JAVA_LIB_DIR/thirdparty/*
ENV JAVA_MAIN_CLASS org.commonjava.indy.boot.jaxrs.JaxRsBooter

EXPOSE 8080
CMD java -cp $JAVA_CLASSPATH $JAVA_OPTIONS $JAVA_MAIN_CLASS
