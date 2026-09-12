# 使用 Java 21 运行环境
# 服务器已有缓存时，可以复用镜像层
FROM eclipse-temurin:21-jre

WORKDIR /app

# GitHub 会提前打包，并将 JAR 统一命名为 app.jar
COPY app.jar /app/app.jar

# 后端在容器内监听 8080
EXPOSE 8080

# 启动 Spring Boot
ENTRYPOINT ["java", "-jar", "/app/app.jar"]