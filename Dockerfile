FROM openjdk:8-jre-alpine

WORKDIR /home

### Fill in the following
ENV componentName "EnablerLogicInterpolator"
ENV componentVersion 0.2.0
ENV componentPort 8200

ENV jar "$componentName-$componentVersion.jar"

COPY build/libs/$jar /home

EXPOSE $componentPort

CMD java $JAVA_HTTP_PROXY $JAVA_HTTPS_PROXY $JAVA_NON_PROXY_HOSTS -jar $jar