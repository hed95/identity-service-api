FROM digitalpatterns/jre:latest

WORKDIR /app

ADD ./build/libs/identity-service-api.jar /app/

USER java

ENTRYPOINT /opt/java/openjdk/bin/java -jar /app/identity-service-api.jar

EXPOSE 8080

USER 1000
