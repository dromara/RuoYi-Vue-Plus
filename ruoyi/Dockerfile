FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/server
RUN mkdir -p /ruoyi/server/logs
RUN mkdir -p /ruoyi/server/ruoyi/uploadPath

WORKDIR /ruoyi/server

EXPOSE 8080

ADD ./target/ruoyi-admin.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
