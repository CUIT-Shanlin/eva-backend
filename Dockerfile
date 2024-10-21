FROM eclipse-temurin:17-jre

LABEL authors="XiaoMo"
MAINTAINER  "XiaoMo"

ENV PARAMS=""

ENV JVMOPTIONS=""

COPY start/.target/flight-service-biz-1.0.jar /app.jar

#ENTRYPOINT ["java","$JVMOPTIONS","-jar","/app.jar","$PARAMS"]
CMD java ${JVMOPTIONS} -jar /app.jar ${PARAMS}