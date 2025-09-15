# 构建阶段
FROM maven:3.9.4-openjdk-17-slim AS builder

# 设置工作目录
WORKDIR /app

# 复制pom文件进行依赖下载
COPY pom.xml .
COPY codenavigator-*/pom.xml ./

# 创建目录结构
RUN mkdir -p codenavigator-common codenavigator-core codenavigator-web codenavigator-ai codenavigator-app

# 复制pom文件到各模块目录
COPY codenavigator-common/pom.xml ./codenavigator-common/
COPY codenavigator-core/pom.xml ./codenavigator-core/
COPY codenavigator-web/pom.xml ./codenavigator-web/
COPY codenavigator-ai/pom.xml ./codenavigator-ai/
COPY codenavigator-app/pom.xml ./codenavigator-app/

# 下载依赖（利用Docker层缓存）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY codenavigator-common/src ./codenavigator-common/src
COPY codenavigator-core/src ./codenavigator-core/src
COPY codenavigator-web/src ./codenavigator-web/src
COPY codenavigator-ai/src ./codenavigator-ai/src
COPY codenavigator-app/src ./codenavigator-app/src

# 构建应用
RUN mvn clean package -DskipTests -B

# 运行阶段
FROM openjdk:17-jre-slim

# 设置系统参数
ENV LANG=C.UTF-8 \
    TZ=Asia/Shanghai \
    JAVA_OPTS="-server -Xms512m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication" \
    SPRING_PROFILES_ACTIVE=prod

# 创建应用用户
RUN groupadd --gid 1000 appuser && \
    useradd --uid 1000 --gid appuser --shell /bin/bash --create-home appuser

# 安装必要的系统工具
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        curl \
        tzdata \
        ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# 设置时区
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建应用目录
RUN mkdir -p /app /var/log/codenavigator /var/lib/codenavigator/uploads && \
    chown -R appuser:appuser /app /var/log/codenavigator /var/lib/codenavigator

# 切换到应用用户
USER appuser

# 设置工作目录
WORKDIR /app

# 复制构建的jar文件
COPY --from=builder --chown=appuser:appuser /app/codenavigator-app/target/codenavigator-app-*.jar /app/app.jar

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]