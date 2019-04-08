FROM georgy/maven:3.5.4_8u191-jdk as builder

ADD . /opt/indy

RUN cd /opt/indy && \
    mvn clean -DskipTests=true package && \
    cd /opt/indy/deployments/launcher/target && \
    tar -xvf indy-launcher-1.7.4-SNAPSHOT-complete.tar.gz

FROM georgy/openjdk:8u191-jdk

WORKDIR /opt/indy
COPY --from=builder /opt/indy /opt/indy

RUN chgrp -R 0 /opt/indy && \
    chmod -R g=u /opt/indy && \
    cd /opt/indy

ENV JAVA_MAJOR_VERSION 8
ENV JAVA_APP_DIR /opt/indy
ENV JAVA_LIB_DIR /opt/indy/lib
ENV JAVA_OPTIONS "-Dindy.home=. -Dorg.jboss.logging.provider=sl4j -Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2"
ENV JAVA_CLASSPATH ${JAVA_LIB_DIR}/indy-embedder-1.7.4-SNAPSHOT.jar
ENV JAVA_CLASSPATH $JAVA_CLASSPATH:$JAVA_LIB_DIR/thirdparty/*
ENV JAVA_MAIN_CLASS org.commonjava.indy.boot.jaxrs.JaxRsBooter
EXPOSE 8080

CMD java -cp $JAVA_CLASSPATH $JAVA_OPTIONS $JAVA_MAIN_CLASS