FROM findepi/graalvm:java17-native

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/monitor/logs

WORKDIR /ruoyi/monitor

ENV LANG=C.UTF-8 LC_ALL=C.UTF-8

EXPOSE 9090

ADD ./target/ruoyi-monitor-admin.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
