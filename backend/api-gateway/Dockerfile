FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN addgroup --system appuser && adduser --system --ingroup appuser appuser

ENV JAVA_OPTS="-XX:+UseZGC -Xms128m -Xmx256m"
COPY --from=build --chown=appuser:appuser /app/target/api-gateway-*.jar app.jar

USER appuser

EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
