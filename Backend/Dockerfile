FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -g 1001 -S thesystem && \
    adduser -u 1001 -S thesystem -G thesystem

WORKDIR /app

COPY build/libs/*.jar app.jar

RUN chown -R thesystem:thesystem /app

USER thesystem

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
