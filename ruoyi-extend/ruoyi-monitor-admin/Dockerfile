FROM ghcr.io/graalvm/jdk:ol8-java17-22.3.0

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/monitor/logs

WORKDIR /ruoyi/monitor

EXPOSE 9090

ADD ./target/ruoyi-monitor-admin.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
