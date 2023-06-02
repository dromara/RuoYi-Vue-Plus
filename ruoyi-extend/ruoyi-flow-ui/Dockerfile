FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER zmx

RUN mkdir -p /ruoyi/flow/ui/logs

WORKDIR /ruoyi/flow/ui

ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 9205

ADD ./target/ruoyi-flow-ui.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
