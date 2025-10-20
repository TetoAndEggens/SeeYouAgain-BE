FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle

COPY build.gradle settings.gradle ./

RUN ./gradlew dependencies --no-daemon || true

COPY . .

RUN ./gradlew clean build -x test --no-daemon

FROM amazoncorretto:17-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=dev", "app.jar"]