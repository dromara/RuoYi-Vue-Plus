#FROM findepi/graalvm:java17-native
FROM openjdk:17.0.2-oraclelinux8

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/snailjob/logs

WORKDIR /ruoyi/snailjob

ENV LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS="-Xms512m -Xmx1024m"

EXPOSE 8800
EXPOSE 1788

ADD ./target/ruoyi-snailjob-server.jar ./app.jar

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom \
           -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} \
           -jar app.jar
