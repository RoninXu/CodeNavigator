# 多阶段构建 - 构建阶段
FROM openjdk:17-jdk-slim AS builder

WORKDIR /app

# 复制Maven配置文件
COPY pom.xml .
COPY codenavigator-common/pom.xml ./codenavigator-common/
COPY codenavigator-core/pom.xml ./codenavigator-core/
COPY codenavigator-ai/pom.xml ./codenavigator-ai/
COPY codenavigator-web/pom.xml ./codenavigator-web/
COPY codenavigator-app/pom.xml ./codenavigator-app/

# 安装Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# 下载依赖
RUN mvn dependency:go-offline -B

# 复制源代码
COPY codenavigator-common/src ./codenavigator-common/src/
COPY codenavigator-core/src ./codenavigator-core/src/
COPY codenavigator-ai/src ./codenavigator-ai/src/
COPY codenavigator-web/src ./codenavigator-web/src/
COPY codenavigator-app/src ./codenavigator-app/src/

# 构建应用
RUN mvn clean package -DskipTests

# 运行阶段
FROM openjdk:17-jre-slim

WORKDIR /app

# 创建非root用户
RUN groupadd -r codenavigator && useradd -r -g codenavigator codenavigator

# 复制构建的jar文件
COPY --from=builder /app/codenavigator-app/target/codenavigator-app-*.jar app.jar

# 创建日志和上传目录
RUN mkdir -p /app/logs /app/uploads && \
    chown -R codenavigator:codenavigator /app

# 切换到非root用户
USER codenavigator

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]