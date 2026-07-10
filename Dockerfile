FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn

RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

COPY src ./src
RUN ./mvnw package -DskipTests -q


FROM eclipse-temurin:17-jre-alpine

WORKDIR /app


RUN addgroup -S bricopro && adduser -S bricopro -G bricopro


COPY --from=builder /app/target/*.jar app.jar

RUN mkdir -p /app/uploads && chown -R bricopro:bricopro /app

USER bricopro

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]