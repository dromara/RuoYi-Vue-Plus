# 贝尔实验室 Spring 官方推荐镜像 JDK下载地址 https://bell-sw.com/pages/downloads/
FROM bellsoft/liberica-openjdk-rocky:21.0.11-cds
# FROM bellsoft/liberica-openjdk-rocky:25.0.3-cds
# FROM findepi/graalvm:java21-native

LABEL maintainer="Lion Li"

RUN mkdir -p /ruoyi/snailai/logs

WORKDIR /ruoyi/snailai

ENV LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""

EXPOSE 8900
EXPOSE 18888

ADD ./target/ruoyi-snailai-server.jar ./app.jar

SHELL ["/bin/bash", "-c"]

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom \
           -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} \
           -jar app.jar
