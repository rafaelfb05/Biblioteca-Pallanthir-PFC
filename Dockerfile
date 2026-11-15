FROM eclipse-temurin:24-jdk

WORKDIR /app

COPY backend .

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["java", "-Xmx256m", "-Xms128m", "-Xss512k", "-XX:MaxMetaspaceSize=160m", "-XX:ReservedCodeCacheSize=64m", "-XX:MaxDirectMemorySize=32m", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-jar", "target/biblioteca-0.0.1-SNAPSHOT.jar"]