FROM registry.littlestrange.site/library/eclipse-temurin:17-jre

LABEL authors="LittleStrange"
MAINTAINER  "LittleStrange"

ENV PARAMS=""

ENV JVMOPTIONS=""

RUN mkdir -p /data/avatar
RUN cp -f /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

COPY ./start/target/eva-server.jar /app.jar

#ENTRYPOINT ["java","$JVMOPTIONS","-jar","/app.jar","$PARAMS"]
ENTRYPOINT ["java","-Duser.timezone=GMT+08","-Dspring.profiles.active=test","-jar","/app.jar"]