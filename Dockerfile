FROM registry.littlestrange.site/library/eclipse-temurin:17-jre

LABEL authors="LittleStrange"
MAINTAINER  "LittleStrange"

ENV PARAMS=""

ENV JVMOPTIONS=""

COPY ./start/target/eva-server.jar /app.jar

#ENTRYPOINT ["java","$JVMOPTIONS","-jar","/app.jar","$PARAMS"]
#-dspring.profiles.active=prod
CMD java ${JVMOPTIONS} -Dspring.profiles.active=prod -jar /app.jar ${PARAMS}