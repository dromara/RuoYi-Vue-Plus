FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/server
RUN mkdir -p /ruoyi/server/logs
RUN mkdir -p /ruoyi/server/temp

WORKDIR /ruoyi/server

ENV SERVER_PORT=8080

EXPOSE ${SERVER_PORT}

ADD ./target/ruoyi-admin.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dserver.port=${SERVER_PORT}","-jar", "app.jar"]
