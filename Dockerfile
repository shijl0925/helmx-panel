# Backend Build Stage
FROM openjdk:22-ea-21-jdk-bookworm AS build-stage

RUN wget https://archive.apache.org/dist/maven/maven-3/3.8.2/binaries/apache-maven-3.8.2-bin.tar.gz && \
    tar -xzf apache-maven-3.8.2-bin.tar.gz && \
    mv apache-maven-3.8.2 /opt/maven && \
    ln -s /opt/maven/bin/mvn /usr/bin/mvn && \
    rm apache-maven-3.8.2-bin.tar.gz

RUN apt-get update && apt-get install -y unzip && rm -rf /var/lib/apt/lists/*

WORKDIR /backend-build

COPY . .

# Package backend
RUN mvn clean package -DskipTests

# Final Monolithic Application Stage
FROM openjdk:22-ea-21-jdk-bookworm AS monolithic
LABEL authors="shijl0925"

# Create application directory
WORKDIR /app

# Copy built JAR from build stage
COPY --from=build-stage /backend-build/helmx-system/target/*.jar ./app.jar

# Create config directory
RUN mkdir -p /home/conf/helmx-panel

# Expose port
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", \
  "-Duser.timezone=Asia/Shanghai", \
  "-Dfile.encoding=UTF-8", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", \
  "app.jar"]
