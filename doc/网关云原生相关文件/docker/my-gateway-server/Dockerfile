FROM openjdk:17-jdk-slim
WORKDIR /app
VOLUME /app/server-data
ADD gateway-core-1.0-SNAPSHOT-jar-with-dependencies.jar api-gateway-server.jar
ENTRYPOINT ["java", "-jar", "/app/api-gateway-server.jar"]
EXPOSE 8888 
