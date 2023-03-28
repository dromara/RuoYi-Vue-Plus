FROM ghcr.io/graalvm/graalvm-ce:22.3.0

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/monitor/logs

WORKDIR /ruoyi/monitor

EXPOSE 9090

ADD ./target/ruoyi-monitor-admin.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
