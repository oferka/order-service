# ── Build stage ───────────────────────────────────────────────────────────────
# No official maven:3.9-eclipse-temurin-26 image exists yet; install Maven onto the JDK image.
FROM eclipse-temurin:26-jdk AS build
ARG MAVEN_VERSION=3.9.9
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates && \
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
      | tar -xzC /opt && \
    ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn && \
    rm -rf /var/lib/apt/lists/*
WORKDIR /app

# Cache dependency layer separately from source
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B \
    && mv target/order-service-*.jar target/app.jar

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:26-jre
WORKDIR /app

COPY --from=build /app/target/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "app.jar"]
