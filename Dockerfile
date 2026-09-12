FROM eclipse-temurin:24-jdk

WORKDIR /app

COPY backend .

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["java", "-Xmx400m", "-Xms200m", "-XX:+UseSerialGC", "-jar", "target/biblioteca-0.0.1-SNAPSHOT.jar"]