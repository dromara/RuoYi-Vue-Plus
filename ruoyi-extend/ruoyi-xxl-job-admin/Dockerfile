FROM ghcr.io/graalvm/jdk:ol8-java17-22.3.0

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/xxljob/logs

WORKDIR /ruoyi/xxljob

ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 9100

ADD ./target/ruoyi-xxl-job-admin.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
