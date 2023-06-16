FROM findepi/graalvm:java17-native

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/powerjob/logs

WORKDIR /ruoyi/powerjob

EXPOSE 7700

ADD ./target/ruoyi-powerjob-server.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
