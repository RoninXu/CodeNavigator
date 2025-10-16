# CodeNavigator 快速部署指南

**版本**: V1.0
**更新日期**: 2025-10-16
**部署时间**: 约10-15分钟

---

## 🎯 部署概述

本指南将帮助您在15分钟内快速部署CodeNavigator系统，适用于开发和测试环境。

### 部署方式对比

| 部署方式 | 难度 | 时间 | 适用场景 |
|---------|------|------|---------|
| Docker Compose (推荐) | ⭐ | 10分钟 | 开发、测试、演示 |
| 源码部署 | ⭐⭐ | 20分钟 | 开发调试 |
| Kubernetes | ⭐⭐⭐⭐ | 60分钟 | 生产环境 |

---

## 🚀 方式一: Docker Compose 一键部署 (推荐)

### 前置要求

**必需软件**:
- Docker 20.10+
- Docker Compose 2.0+

**系统要求**:
- 操作系统: Linux / macOS / Windows 10+
- CPU: 2核心+
- 内存: 4GB+
- 磁盘: 20GB+

### 快速检查

```bash
# 检查Docker版本
docker --version
docker-compose --version

# 检查系统资源
free -h     # Linux
vm_stat     # macOS
```

### 第一步: 获取代码

```bash
# 克隆项目
git clone https://github.com/RoninXu/CodeNavigator.git
cd CodeNavigator

# 或下载ZIP并解压
wget https://github.com/RoninXu/CodeNavigator/archive/refs/heads/main.zip
unzip main.zip
cd CodeNavigator-main
```

### 第二步: 配置环境变量

```bash
# 创建环境变量文件
cat > .env << 'EOF'
# OpenAI API密钥 (必需)
OPENAI_API_KEY=your_openai_api_key_here

# DeepSeek API密钥 (可选)
DEEPSEEK_API_KEY=your_deepseek_api_key_here

# 数据库配置 (可选，使用默认值即可)
MYSQL_ROOT_PASSWORD=codenavigator_root_2024
MYSQL_PASSWORD=codenavigator123

# Redis配置 (可选)
REDIS_PASSWORD=
EOF
```

**重要**: 请替换 `your_openai_api_key_here` 为您的实际OpenAI API密钥。

### 第三步: 启动服务

```bash
# 一键启动所有服务
docker-compose up -d

# 查看启动日志
docker-compose logs -f
```

**等待时间**: 首次启动需要3-5分钟（下载镜像、构建应用、初始化数据库）

### 第四步: 验证部署

```bash
# 检查服务状态
docker-compose ps

# 期望输出:
# NAME                      STATUS              PORTS
# codenavigator-mysql       Up (healthy)        0.0.0.0:3306->3306/tcp
# codenavigator-redis       Up (healthy)        0.0.0.0:6379->6379/tcp
# codenavigator-app         Up                  0.0.0.0:8080->8080/tcp
```

```bash
# 测试应用健康状态
curl http://localhost:8080/actuator/health

# 期望输出: {"status":"UP"}
```

### 第五步: 访问应用

**应用地址**:
- 主页: http://localhost:8080
- API文档: http://localhost:8080/swagger-ui.html
- 健康检查: http://localhost:8080/actuator/health

### 常用命令

```bash
# 查看日志
docker-compose logs app        # 应用日志
docker-compose logs mysql      # 数据库日志
docker-compose logs -f         # 实时查看所有日志

# 重启服务
docker-compose restart app     # 重启应用
docker-compose restart         # 重启所有服务

# 停止服务
docker-compose stop            # 停止服务（保留数据）
docker-compose down            # 停止并删除容器（保留数据）
docker-compose down -v         # 停止并删除所有数据（慎用）

# 更新代码
git pull
docker-compose down
docker-compose up -d --build

# 查看资源使用
docker stats
```

---

## 💻 方式二: 源码部署

### 前置要求

**必需软件**:
- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+
- Node.js 18+ (可选，前端开发)

### 第一步: 安装依赖服务

#### 安装MySQL

**Ubuntu/Debian**:
```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
sudo mysql_secure_installation
```

**macOS**:
```bash
brew install mysql
brew services start mysql
```

**Windows**:
下载并安装: https://dev.mysql.com/downloads/mysql/

#### 安装Redis

**Ubuntu/Debian**:
```bash
sudo apt install redis-server
sudo systemctl start redis
```

**macOS**:
```bash
brew install redis
brew services start redis
```

**Windows**:
下载并安装: https://github.com/microsoftarchive/redis/releases

### 第二步: 初始化数据库

```bash
# 登录MySQL
mysql -u root -p

# 创建数据库和用户
CREATE DATABASE code_navigator_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'codenavigator'@'localhost' IDENTIFIED BY 'codenavigator123';
GRANT ALL PRIVILEGES ON code_navigator_dev.* TO 'codenavigator'@'localhost';
FLUSH PRIVILEGES;
EXIT;

# 导入初始化脚本
mysql -u codenavigator -pcodenavigator123 code_navigator_dev < docs/sql/database_init.sql
```

### 第三步: 配置应用

```bash
# 配置环境变量
export DB_USERNAME=codenavigator
export DB_PASSWORD=codenavigator123
export OPENAI_API_KEY=your_openai_api_key_here
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

**或者** 创建 `application-local.yml`:

```yaml
# codenavigator-app/src/main/resources/application-local.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/code_navigator_dev
    username: codenavigator
    password: codenavigator123

  data:
    redis:
      host: localhost
      port: 6379

langchain4j:
  open-ai:
    chat-model:
      api-key: your_openai_api_key_here
```

### 第四步: 构建和运行

```bash
# 克隆代码
git clone https://github.com/RoninXu/CodeNavigator.git
cd CodeNavigator

# 构建项目
mvn clean install -DskipTests

# 运行应用
mvn spring-boot:run -pl codenavigator-app -Dspring-boot.run.profiles=local

# 或使用jar包运行
java -jar codenavigator-app/target/codenavigator-app-1.0.0.jar --spring.profiles.active=local
```

### 第五步: 验证部署

```bash
# 检查应用是否启动
curl http://localhost:8080/actuator/health

# 访问应用
open http://localhost:8080
```

---

## ☁️ 方式三: 云平台快速部署

### AWS部署 (使用EC2)

#### 一键部署脚本

```bash
#!/bin/bash
# aws-deploy.sh

# 更新系统
sudo yum update -y

# 安装Docker
sudo yum install docker -y
sudo service docker start
sudo usermod -a -G docker ec2-user

# 安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 克隆项目
git clone https://github.com/RoninXu/CodeNavigator.git
cd CodeNavigator

# 配置环境变量
cat > .env << EOF
OPENAI_API_KEY=$1
EOF

# 启动服务
docker-compose up -d

echo "部署完成！访问地址: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8080"
```

**使用方法**:
```bash
chmod +x aws-deploy.sh
./aws-deploy.sh your_openai_api_key
```

### 阿里云部署 (使用ECS)

```bash
#!/bin/bash
# aliyun-deploy.sh

# 安装Docker (CentOS)
sudo yum install -y yum-utils
sudo yum-config-manager --add-repo http://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
sudo yum install -y docker-ce
sudo systemctl start docker

# 配置Docker镜像加速
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": ["https://mirror.ccs.tencentyun.com"]
}
EOF
sudo systemctl daemon-reload
sudo systemctl restart docker

# 安装Docker Compose
sudo curl -L https://get.daocloud.io/docker/compose/releases/download/v2.20.0/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 部署应用
git clone https://github.com/RoninXu/CodeNavigator.git
cd CodeNavigator

# 配置并启动
echo "OPENAI_API_KEY=$1" > .env
docker-compose up -d

echo "部署完成！"
```

---

## 🔧 配置说明

### 环境变量完整列表

```bash
# ========== 必需配置 ==========

# OpenAI API密钥
OPENAI_API_KEY=sk-xxx

# ========== 可选配置 ==========

# DeepSeek API密钥
DEEPSEEK_API_KEY=sk-xxx

# 数据库配置
DB_HOST=mysql
DB_PORT=3306
DB_NAME=code_navigator_dev
DB_USERNAME=codenavigator
DB_PASSWORD=codenavigator123
MYSQL_ROOT_PASSWORD=codenavigator_root_2024

# Redis配置
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=

# 应用配置
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev

# JVM配置
JAVA_OPTS=-Xms512m -Xmx2g

# 日志配置
LOG_LEVEL=INFO
LOG_PATH=/app/logs
```

### 端口说明

| 服务 | 默认端口 | 说明 |
|------|---------|------|
| 应用 | 8080 | Web应用主端口 |
| MySQL | 3306 | 数据库端口 |
| Redis | 6379 | 缓存端口 |

### 数据持久化

Docker Compose默认创建以下数据卷:
- `mysql_data`: MySQL数据
- `redis_data`: Redis数据
- `app_uploads`: 应用上传文件

**数据位置**:
```bash
# 查看数据卷
docker volume ls

# 查看数据卷详情
docker volume inspect codenavigator_mysql_data

# 备份数据卷
docker run --rm -v codenavigator_mysql_data:/data \
    -v $(pwd):/backup alpine \
    tar czf /backup/mysql_backup.tar.gz /data
```

---

## 🔍 故障排查

### 常见问题

#### 1. 端口冲突

**问题**: `Error starting userland proxy: listen tcp 0.0.0.0:8080: bind: address already in use`

**解决方案**:
```bash
# 查找占用端口的进程
lsof -i :8080          # Linux/macOS
netstat -ano | findstr :8080   # Windows

# 修改端口
# 编辑 docker-compose.yml
ports:
  - "8081:8080"  # 将主机端口改为8081
```

#### 2. 容器启动失败

**问题**: 容器状态显示 `Exited (1)`

**解决方案**:
```bash
# 查看详细日志
docker-compose logs app

# 常见原因和解决方法:
# - 数据库连接失败: 检查DB_HOST、DB_PASSWORD等环境变量
# - API密钥未配置: 设置OPENAI_API_KEY
# - 内存不足: 增加Docker内存限制
```

#### 3. 数据库连接失败

**问题**: `Communications link failure`

**解决方案**:
```bash
# 检查MySQL容器状态
docker-compose ps mysql

# 确认MySQL健康检查通过
docker-compose exec mysql mysqladmin ping -h localhost

# 测试数据库连接
docker-compose exec mysql mysql -u codenavigator -pcodenavigator123 code_navigator_dev -e "SELECT 1"

# 如果失败,重新创建数据库
docker-compose down -v
docker-compose up -d
```

#### 4. Redis连接失败

**问题**: `Unable to connect to Redis`

**解决方案**:
```bash
# 检查Redis状态
docker-compose exec redis redis-cli ping

# 应该返回: PONG

# 重启Redis
docker-compose restart redis
```

#### 5. AI服务调用失败

**问题**: `OpenAI API error`

**解决方案**:
```bash
# 检查API密钥是否正确
docker-compose exec app printenv | grep OPENAI

# 测试API连接
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"

# 更新API密钥
# 1. 修改 .env 文件
# 2. 重启应用
docker-compose restart app
```

### 健康检查

```bash
#!/bin/bash
# health_check.sh

echo "=== CodeNavigator 健康检查 ==="

# 检查Docker服务
if ! docker ps &> /dev/null; then
    echo "❌ Docker服务未运行"
    exit 1
else
    echo "✅ Docker服务正常"
fi

# 检查容器状态
CONTAINERS="codenavigator-mysql codenavigator-redis codenavigator-app"
for CONTAINER in $CONTAINERS; do
    if docker ps | grep -q $CONTAINER; then
        echo "✅ $CONTAINER 运行中"
    else
        echo "❌ $CONTAINER 未运行"
    fi
done

# 检查应用健康
HEALTH=$(curl -s http://localhost:8080/actuator/health | grep -o '"status":"UP"')
if [ -n "$HEALTH" ]; then
    echo "✅ 应用健康检查通过"
else
    echo "❌ 应用健康检查失败"
fi

# 检查端口监听
PORTS="8080 3306 6379"
for PORT in $PORTS; do
    if netstat -tuln | grep -q ":$PORT "; then
        echo "✅ 端口 $PORT 监听正常"
    else
        echo "❌ 端口 $PORT 未监听"
    fi
done

echo "=== 检查完成 ==="
```

---

## 🔐 安全配置

### 生产环境安全检查清单

- [ ] **修改默认密码**: 更改MySQL root密码和应用数据库密码
- [ ] **配置防火墙**: 限制数据库和Redis端口仅内网访问
- [ ] **启用HTTPS**: 配置SSL/TLS证书
- [ ] **API密钥保护**: 使用密钥管理服务存储敏感信息
- [ ] **定期备份**: 配置自动备份策略
- [ ] **更新依赖**: 定期更新Docker镜像和依赖库
- [ ] **日志审计**: 启用访问日志和操作日志

### 快速安全加固

```bash
# 1. 修改密码
docker-compose exec mysql mysql -u root -p -e "
ALTER USER 'root'@'localhost' IDENTIFIED BY 'NewStrongPassword123!';
FLUSH PRIVILEGES;"

# 2. 配置防火墙
sudo ufw enable
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw deny 3306/tcp
sudo ufw deny 6379/tcp

# 3. 启用SSL (使用Let's Encrypt)
sudo apt install certbot
sudo certbot certonly --standalone -d yourdomain.com
```

---

## 📦 备份和恢复

### 快速备份

```bash
#!/bin/bash
# backup.sh

BACKUP_DIR="./backups"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# 备份数据库
echo "备份数据库..."
docker-compose exec -T mysql mysqldump \
    -u root -pcodenavigator_root_2024 \
    --all-databases --routines --triggers \
    > $BACKUP_DIR/mysql_$DATE.sql

# 备份配置文件
echo "备份配置..."
tar -czf $BACKUP_DIR/config_$DATE.tar.gz .env docker-compose.yml

# 压缩备份
echo "压缩备份..."
tar -czf $BACKUP_DIR/backup_$DATE.tar.gz $BACKUP_DIR/mysql_$DATE.sql $BACKUP_DIR/config_$DATE.tar.gz

# 清理临时文件
rm $BACKUP_DIR/mysql_$DATE.sql $BACKUP_DIR/config_$DATE.tar.gz

echo "备份完成: $BACKUP_DIR/backup_$DATE.tar.gz"
```

### 快速恢复

```bash
#!/bin/bash
# restore.sh

BACKUP_FILE=$1

if [ -z "$BACKUP_FILE" ]; then
    echo "用法: ./restore.sh backup_file.tar.gz"
    exit 1
fi

# 解压备份
tar -xzf $BACKUP_FILE

# 停止服务
docker-compose stop app

# 恢复数据库
docker-compose exec -T mysql mysql -u root -pcodenavigator_root_2024 < mysql_backup.sql

# 重启服务
docker-compose start app

echo "恢复完成！"
```

---

## 🚀 性能优化

### 快速优化建议

#### 1. 增加资源限制

```yaml
# docker-compose.yml
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 4G
        reservations:
          cpus: '1'
          memory: 2G
```

#### 2. 启用缓存

```bash
# 确认Redis正常运行
docker-compose exec redis redis-cli ping

# 检查缓存命中率
docker-compose exec redis redis-cli INFO stats | grep keyspace
```

#### 3. 数据库连接池优化

```yaml
# application-prod.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
```

---

## 📞 获取帮助

### 文档资源
- **完整文档**: [README.md](README.md)
- **用户手册**: [docs/user-manual.md](docs/user-manual.md)
- **管理员指南**: [docs/admin-guide.md](docs/admin-guide.md)
- **性能优化**: [docs/performance-optimization.md](docs/performance-optimization.md)

### 在线支持
- **GitHub Issues**: https://github.com/RoninXu/CodeNavigator/issues
- **讨论区**: https://github.com/RoninXu/CodeNavigator/discussions

### 社区
- **官方论坛**: Coming Soon
- **QQ交流群**: Coming Soon

---

## ✅ 部署检查清单

### 基础部署检查

- [ ] Docker和Docker Compose已安装
- [ ] 已克隆代码仓库
- [ ] 已配置OPENAI_API_KEY
- [ ] 已启动服务: `docker-compose up -d`
- [ ] 容器状态健康: `docker-compose ps`
- [ ] 应用可访问: http://localhost:8080
- [ ] API文档可访问: http://localhost:8080/swagger-ui.html

### 功能验证检查

- [ ] 主页正常显示
- [ ] 用户可以注册登录
- [ ] AI对话功能正常
- [ ] 学习路径可以创建
- [ ] 代码分析功能正常

### 生产环境额外检查

- [ ] 已修改默认密码
- [ ] 已配置防火墙
- [ ] 已启用HTTPS
- [ ] 已配置备份策略
- [ ] 已配置监控告警
- [ ] 已进行性能测试
- [ ] 已配置日志轮转

---

## 🎉 部署完成

恭喜！您已成功部署CodeNavigator。

**下一步**:
1. 访问 http://localhost:8080 开始使用
2. 阅读[用户手册](docs/user-manual.md)了解功能
3. 查看[API文档](http://localhost:8080/swagger-ui.html)进行集成开发

**需要帮助？**
- 查看[常见问题](#故障排查)
- 提交[GitHub Issue](https://github.com/RoninXu/CodeNavigator/issues)

---

**最后更新**: 2025-10-16
**维护者**: CodeNavigator Team

祝您使用愉快！🚀
