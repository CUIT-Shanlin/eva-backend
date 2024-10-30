FROM registry.littlestrange.site/library/eclipse-temurin:17-jre

LABEL authors="LittleStrange"
MAINTAINER  "LittleStrange"

ENV PARAMS=""

ENV JVMOPTIONS="-Duser.timezone=GMT+08 -Dspring.profiles.active=test -jar"

RUN mkdir -p /data/avatar
RUN echo Asia/Shanghai > /etc/timezone

COPY ./start/target/eva-server.jar /app.jar

#ENTRYPOINT ["java","$JVMOPTIONS","-jar","/app.jar","$PARAMS"]
CMD java ${JVMOPTIONS} /app.jar ${PARAMS}