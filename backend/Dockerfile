FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
ENV MAVEN_OPTS="-Xmx384m -XX:MaxMetaspaceSize=192m -XX:ReservedCodeCacheSize=128m -Xss512k -XX:ActiveProcessorCount=2 -XX:+UseSerialGC"
COPY pom.xml .
COPY .mvn ./.mvn
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
