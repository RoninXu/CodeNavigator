# CodeNavigator 管理员指南

## 📋 目录

1. [系统概述](#系统概述)
2. [部署指南](#部署指南)
3. [配置管理](#配置管理)
4. [监控运维](#监控运维)
5. [用户管理](#用户管理)
6. [数据管理](#数据管理)
7. [安全管理](#安全管理)
8. [性能调优](#性能调优)
9. [故障处理](#故障处理)
10. [系统维护](#系统维护)

---

## 🏗️ 系统概述

### 架构组件
CodeNavigator 采用微服务架构，主要组件包括：

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│    Nginx        │    │   Spring Boot   │    │     MySQL       │
│  (反向代理)      │────│   (核心应用)     │────│   (数据存储)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │              ┌─────────────────┐              │
         │              │     Redis       │              │
         └──────────────│   (缓存系统)     │──────────────┘
                        └─────────────────┘
                                 │
                   ┌─────────────────┐    ┌─────────────────┐
                   │   Prometheus    │    │   Elasticsearch │
                   │   (监控系统)     │    │   (日志系统)     │
                   └─────────────────┘    └─────────────────┘
```

### 技术栈
- **后端**: Spring Boot 3.2, Java 17
- **数据库**: MySQL 8.0, Redis 7
- **前端**: Thymeleaf, Bootstrap 5, JavaScript
- **容器化**: Docker, Docker Compose
- **监控**: Prometheus, Grafana, ELK Stack
- **代理**: Nginx
- **AI服务**: LangChain4j, OpenAI API

### 系统要求
- **CPU**: 最低 4核，推荐 8核+
- **内存**: 最低 8GB，推荐 16GB+
- **存储**: 最低 100GB SSD，推荐 500GB+
- **网络**: 稳定的互联网连接
- **操作系统**: Linux (Ubuntu 20.04+/CentOS 8+)

---

## 🚀 部署指南

### 生产环境部署

#### 1. 环境准备
```bash
# 安装 Docker 和 Docker Compose
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo pip3 install docker-compose

# 创建部署目录
sudo mkdir -p /opt/codenavigator
cd /opt/codenavigator

# 克隆项目
git clone https://github.com/your-org/codenavigator.git .
```

#### 2. 环境变量配置
```bash
# 创建环境变量文件
cp .env.example .env

# 编辑环境变量
vim .env
```

必需的环境变量：
```env
# 数据库配置
DB_PASSWORD=your_secure_password
MYSQL_ROOT_PASSWORD=your_root_password

# Redis配置
REDIS_PASSWORD=your_redis_password

# AI服务配置
OPENAI_API_KEY=your_openai_api_key

# 监控配置
GRAFANA_PASSWORD=your_grafana_password

# 告警配置
SMTP_HOST=smtp.gmail.com:587
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_app_password
CRITICAL_ALERT_EMAIL=admin@yourdomain.com
WARNING_ALERT_EMAIL=dev@yourdomain.com

# 域名配置
DOMAIN_NAME=yourdomain.com
```

#### 3. SSL证书配置
```bash
# 创建SSL目录
sudo mkdir -p ssl

# 使用Let's Encrypt获取免费证书
sudo apt install certbot
sudo certbot certonly --standalone -d yourdomain.com

# 复制证书到ssl目录
sudo cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem ssl/cert.pem
sudo cp /etc/letsencrypt/live/yourdomain.com/privkey.pem ssl/key.pem
sudo chmod 644 ssl/*.pem
```

#### 4. 启动服务
```bash
# 启动生产环境
docker-compose -f docker-compose.prod.yml up -d

# 查看服务状态
docker-compose -f docker-compose.prod.yml ps

# 查看日志
docker-compose -f docker-compose.prod.yml logs -f
```

### 开发环境部署

#### 1. 快速启动
```bash
# 克隆项目
git clone https://github.com/your-org/codenavigator.git
cd codenavigator

# 启动开发环境
mvn spring-boot:run -pl codenavigator-app
```

#### 2. Docker开发环境
```bash
# 启动开发服务
docker-compose up -d mysql redis

# 运行应用
mvn spring-boot:run -pl codenavigator-app
```

---

## ⚙️ 配置管理

### 应用配置

#### 数据库配置
```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      idle-timeout: 600000
      max-lifetime: 1800000
```

#### Redis配置
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      password: ${REDIS_PASSWORD}
      lettuce:
        pool:
          max-active: 50
          max-idle: 20
          min-idle: 5
```

#### AI服务配置
```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY}
      model-name: gpt-4
      temperature: 0.5
      timeout: 30s
```

### Nginx配置
主要配置文件位于 `nginx/conf.d/codenavigator.conf`：

```nginx
# SSL配置
ssl_certificate /etc/nginx/ssl/cert.pem;
ssl_certificate_key /etc/nginx/ssl/key.pem;

# 安全头
add_header Strict-Transport-Security "max-age=63072000" always;
add_header X-Frame-Options DENY;
add_header X-Content-Type-Options nosniff;

# 负载均衡
upstream codenavigator_backend {
    server app:8080 max_fails=3 fail_timeout=30s;
    keepalive 32;
}
```

### 监控配置

#### Prometheus配置
```yaml
# monitoring/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'codenavigator-app'
    static_configs:
      - targets: ['app:8080']
    metrics_path: /actuator/prometheus
```

#### 告警规则
```yaml
# monitoring/alerts/application-alerts.yml
groups:
  - name: codenavigator-application
    rules:
      - alert: ApplicationDown
        expr: up{job="codenavigator-app"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "CodeNavigator应用不可用"
```

---

## 📊 监控运维

### 监控访问地址
- **Grafana**: http://your-domain:3000
- **Prometheus**: http://your-domain:9090
- **AlertManager**: http://your-domain:9093
- **Kibana**: http://your-domain:5601

### 关键监控指标

#### 应用指标
```promql
# HTTP请求速率
rate(http_server_requests_total[5m])

# 响应时间95百分位
histogram_quantile(0.95, rate(http_server_requests_duration_seconds_bucket[5m]))

# 错误率
rate(http_server_requests_total{status=~"5.."}[5m]) / rate(http_server_requests_total[5m])

# JVM内存使用率
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
```

#### 系统指标
```promql
# CPU使用率
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# 内存使用率
(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100

# 磁盘使用率
(1 - (node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"})) * 100
```

### 日志管理

#### 日志级别配置
```yaml
logging:
  level:
    root: INFO
    com.codenavigator: INFO
    org.springframework: WARN
```

#### 日志轮转配置
```yaml
logging:
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30
      total-size-cap: 3GB
```

### 备份策略

#### 数据库备份
```bash
#!/bin/bash
# 每日备份脚本
BACKUP_DIR="/opt/backups/mysql"
DATE=$(date +%Y%m%d_%H%M%S)

docker exec mysql mysqldump -u root -p$MYSQL_ROOT_PASSWORD \
  --single-transaction --routines --triggers code_navigator \
  > $BACKUP_DIR/codenavigator_$DATE.sql

# 保留30天备份
find $BACKUP_DIR -name "*.sql" -mtime +30 -delete
```

#### 配置备份
```bash
#!/bin/bash
# 配置文件备份
tar -czf /opt/backups/config_$(date +%Y%m%d).tar.gz \
  /opt/codenavigator/nginx/ \
  /opt/codenavigator/monitoring/ \
  /opt/codenavigator/.env
```

---

## 👥 用户管理

### 用户角色
- **超级管理员**: 系统全权限
- **管理员**: 用户和内容管理权限
- **讲师**: 课程和学习路径管理权限
- **学员**: 基础学习功能权限

### 用户操作

#### 创建管理员用户
```sql
-- 在数据库中直接创建管理员
INSERT INTO users (username, email, password_hash, role, status, created_at) 
VALUES ('admin', 'admin@yourdomain.com', 'hashed_password', 'ADMIN', 'ACTIVE', NOW());
```

#### 批量用户导入
```bash
# 使用CSV文件批量导入用户
curl -X POST http://your-domain/admin/users/import \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -F "file=@users.csv"
```

#### 用户状态管理
```bash
# 激活用户
curl -X PUT http://your-domain/admin/users/{userId}/activate \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# 禁用用户
curl -X PUT http://your-domain/admin/users/{userId}/deactivate \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 权限管理
```yaml
# 权限配置示例
permissions:
  admin:
    - user.manage
    - content.manage
    - system.monitor
  instructor:
    - course.create
    - course.edit
    - student.view
  student:
    - course.view
    - progress.track
```

---

## 🗄️ 数据管理

### 数据库管理

#### 连接数据库
```bash
# 进入MySQL容器
docker exec -it codenavigator-mysql mysql -u root -p

# 查看数据库状态
SHOW DATABASES;
USE code_navigator;
SHOW TABLES;
```

#### 性能优化
```sql
-- 查看慢查询
SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'long_query_time';

-- 分析表
ANALYZE TABLE users;
ANALYZE TABLE learning_paths;
ANALYZE TABLE user_progress;

-- 优化表
OPTIMIZE TABLE users;
OPTIMIZE TABLE conversations;
```

#### 索引管理
```sql
-- 查看表索引
SHOW INDEX FROM users;

-- 创建复合索引
CREATE INDEX idx_user_progress_composite ON user_progress(user_id, learning_path_id, status);

-- 监控索引使用情况
SELECT 
  TABLE_NAME,
  INDEX_NAME,
  SEQ_IN_INDEX,
  COLUMN_NAME,
  CARDINALITY
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'code_navigator';
```

### Redis管理

#### 连接Redis
```bash
# 进入Redis容器
docker exec -it codenavigator-redis redis-cli -a $REDIS_PASSWORD

# 查看Redis信息
INFO memory
INFO stats
```

#### 缓存管理
```bash
# 查看所有键
KEYS *

# 查看特定模式的键
KEYS codenavigator:user:*

# 清除特定缓存
DEL codenavigator:user:123

# 清除所有缓存（慎用）
FLUSHALL
```

### 数据迁移

#### 数据导出
```bash
# 导出用户数据
docker exec mysql mysqldump -u root -p$MYSQL_ROOT_PASSWORD \
  code_navigator users > users_export.sql

# 导出学习进度
docker exec mysql mysqldump -u root -p$MYSQL_ROOT_PASSWORD \
  code_navigator user_progress > progress_export.sql
```

#### 数据导入
```bash
# 导入数据
docker exec -i mysql mysql -u root -p$MYSQL_ROOT_PASSWORD \
  code_navigator < users_import.sql
```

---

## 🔒 安全管理

### 安全配置

#### SSL/TLS配置
```nginx
# 现代SSL配置
ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
ssl_prefer_server_ciphers off;

# HSTS
add_header Strict-Transport-Security "max-age=63072000" always;
```

#### 防火墙配置
```bash
# Ubuntu/Debian
sudo ufw enable
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw deny 3306/tcp  # 数据库端口仅内网访问
sudo ufw deny 6379/tcp  # Redis端口仅内网访问

# CentOS/RHEL
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

#### 访问控制
```nginx
# 限制管理接口访问
location /admin {
    allow 192.168.1.0/24;  # 仅允许内网访问
    deny all;
    proxy_pass http://codenavigator_backend;
}

# API限流
limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=login:10m rate=1r/s;
```

### 安全监控

#### 登录监控
```sql
-- 监控异常登录
SELECT 
  ip_address,
  COUNT(*) as failed_attempts,
  MAX(attempted_at) as last_attempt
FROM login_attempts 
WHERE success = false 
  AND attempted_at > DATE_SUB(NOW(), INTERVAL 1 HOUR)
GROUP BY ip_address
HAVING failed_attempts > 5;
```

#### 安全日志
```bash
# 监控Nginx访问日志中的异常请求
tail -f /var/log/nginx/access.log | grep -E "(40[0-9]|50[0-9])"

# 监控认证失败
grep "authentication failed" /var/log/codenavigator/codenavigator.log
```

### 数据保护

#### 敏感数据加密
```java
// 数据库敏感字段加密
@Entity
public class User {
    @Column(name = "email")
    @Convert(converter = EncryptionConverter.class)
    private String email;
    
    @Column(name = "phone")
    @Convert(converter = EncryptionConverter.class)
    private String phone;
}
```

#### 备份加密
```bash
# 加密备份
mysqldump -u root -p$MYSQL_ROOT_PASSWORD code_navigator | \
  gpg --cipher-algo AES256 --compress-algo 1 --symmetric \
  --output backup_$(date +%Y%m%d).sql.gpg
```

---

## ⚡ 性能调优

### JVM调优

#### 内存配置
```bash
# 生产环境JVM参数
JAVA_OPTS="-Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:+UseStringDeduplication \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/codenavigator/"
```

#### GC调优
```bash
# G1GC参数调优
-XX:G1HeapRegionSize=16m
-XX:G1NewSizePercent=30
-XX:G1MaxNewSizePercent=40
-XX:G1MixedGCLiveThresholdPercent=85
-XX:G1MixedGCCountTarget=8
```

### 数据库调优

#### MySQL配置优化
```ini
# my.cnf
[mysqld]
# 内存配置
innodb_buffer_pool_size = 4G
innodb_log_file_size = 256M
innodb_log_buffer_size = 64M

# 连接配置
max_connections = 1000
max_connect_errors = 100000

# 查询缓存
query_cache_type = 1
query_cache_size = 256M

# InnoDB配置
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT
```

#### 查询优化
```sql
-- 分析慢查询
SELECT 
  query_time,
  lock_time,
  rows_sent,
  rows_examined,
  sql_text
FROM mysql.slow_log 
ORDER BY query_time DESC 
LIMIT 10;

-- 优化建议
EXPLAIN SELECT * FROM users WHERE email = 'user@example.com';
```

### 缓存优化

#### Redis配置
```redis
# redis.conf
maxmemory 2gb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000

# 网络优化
tcp-keepalive 300
timeout 0
```

#### 缓存策略
```java
// 多级缓存策略
@Cacheable(value = "users", key = "#userId", unless = "#result == null")
public User getUserById(Long userId) {
    return userRepository.findById(userId);
}

@CacheEvict(value = "users", key = "#user.id")
public User updateUser(User user) {
    return userRepository.save(user);
}
```

### 网络优化

#### Nginx优化
```nginx
# 工作进程数
worker_processes auto;
worker_connections 4096;

# 性能优化
sendfile on;
tcp_nopush on;
tcp_nodelay on;
keepalive_timeout 65;

# Gzip压缩
gzip on;
gzip_vary on;
gzip_min_length 1000;
gzip_comp_level 6;
```

---

## 🚨 故障处理

### 常见故障

#### 应用无法启动
**症状**: 应用启动失败
**排查步骤**:
```bash
# 1. 查看应用日志
docker logs codenavigator-app

# 2. 检查配置文件
cat .env

# 3. 验证数据库连接
docker exec mysql mysql -u root -p$MYSQL_ROOT_PASSWORD -e "SELECT 1"

# 4. 检查端口占用
netstat -tlnp | grep 8080
```

#### 数据库连接失败
**症状**: 应用无法连接数据库
**排查步骤**:
```bash
# 1. 检查MySQL容器状态
docker ps | grep mysql

# 2. 查看MySQL日志
docker logs codenavigator-mysql

# 3. 验证网络连接
docker exec app ping mysql

# 4. 检查用户权限
docker exec mysql mysql -u root -p$MYSQL_ROOT_PASSWORD \
  -e "SELECT User, Host FROM mysql.user WHERE User='codenavigator'"
```

#### 内存不足
**症状**: 系统响应缓慢，内存使用率高
**解决方案**:
```bash
# 1. 查看内存使用情况
free -h
docker stats

# 2. 调整JVM堆内存
vim docker-compose.prod.yml
# 修改 JAVA_OPTS: "-Xms1g -Xmx3g"

# 3. 重启应用
docker-compose restart app

# 4. 监控内存使用
watch -n 1 'free -h'
```

### 紧急处理流程

#### 服务中断处理
1. **立即响应** (5分钟内)
   - 确认服务状态
   - 启动紧急处理程序
   - 通知相关人员

2. **问题定位** (15分钟内)
   - 查看监控报警
   - 分析日志文件
   - 确定故障范围

3. **紧急恢复** (30分钟内)
   - 实施回滚计划
   - 重启关键服务
   - 验证服务恢复

4. **根因分析** (24小时内)
   - 详细分析故障原因
   - 制定改进措施
   - 更新应急预案

#### 数据恢复
```bash
# 1. 停止应用写入
docker-compose stop app

# 2. 从备份恢复数据
mysql -u root -p$MYSQL_ROOT_PASSWORD code_navigator < backup_latest.sql

# 3. 验证数据完整性
mysql -u root -p$MYSQL_ROOT_PASSWORD -e "
  SELECT COUNT(*) FROM code_navigator.users;
  SELECT COUNT(*) FROM code_navigator.conversations;
"

# 4. 重启应用
docker-compose start app
```

---

## 🔧 系统维护

### 定期维护任务

#### 每日维护
```bash
#!/bin/bash
# daily_maintenance.sh

# 1. 备份数据库
/opt/scripts/backup_database.sh

# 2. 清理临时文件
find /tmp -name "*.tmp" -mtime +1 -delete

# 3. 检查磁盘空间
df -h | awk '$5 > 80 {print "Warning: " $1 " is " $5 " full"}'

# 4. 检查服务状态
docker-compose ps | grep -v "Up" && echo "Some services are down!"
```

#### 每周维护
```bash
#!/bin/bash
# weekly_maintenance.sh

# 1. 优化数据库
docker exec mysql mysql -u root -p$MYSQL_ROOT_PASSWORD -e "
  OPTIMIZE TABLE code_navigator.users;
  OPTIMIZE TABLE code_navigator.conversations;
  OPTIMIZE TABLE code_navigator.user_progress;
"

# 2. 清理过期日志
find /var/log -name "*.log" -mtime +30 -delete

# 3. 更新系统包
apt update && apt upgrade -y

# 4. 重启服务（低峰期）
docker-compose restart
```

#### 每月维护
```bash
#!/bin/bash
# monthly_maintenance.sh

# 1. 分析数据库性能
docker exec mysql mysqltuner

# 2. 检查SSL证书过期
openssl x509 -in ssl/cert.pem -noout -dates

# 3. 安全审计
/opt/scripts/security_audit.sh

# 4. 容量规划评估
/opt/scripts/capacity_planning.sh
```

### 版本更新

#### 更新流程
1. **准备阶段**
   ```bash
   # 备份当前版本
   docker-compose down
   tar -czf backup_$(date +%Y%m%d).tar.gz /opt/codenavigator
   
   # 下载新版本
   git fetch origin
   git checkout v1.1.0
   ```

2. **测试阶段**
   ```bash
   # 在测试环境验证
   docker-compose -f docker-compose.test.yml up -d
   
   # 运行测试套件
   mvn test
   ```

3. **部署阶段**
   ```bash
   # 停止服务
   docker-compose -f docker-compose.prod.yml down
   
   # 更新镜像
   docker-compose -f docker-compose.prod.yml build
   
   # 启动新版本
   docker-compose -f docker-compose.prod.yml up -d
   ```

4. **验证阶段**
   ```bash
   # 健康检查
   curl -f http://localhost:8080/actuator/health
   
   # 功能验证
   /opt/scripts/smoke_test.sh
   ```

### 监控和告警

#### 系统监控脚本
```bash
#!/bin/bash
# system_monitor.sh

# CPU使用率检查
CPU_USAGE=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | awk -F% '{print $1}')
if (( $(echo "$CPU_USAGE > 80" | bc -l) )); then
    echo "High CPU usage: $CPU_USAGE%"
    # 发送告警
fi

# 内存使用率检查
MEM_USAGE=$(free | grep Mem | awk '{printf("%.2f"), $3/$2 * 100.0}')
if (( $(echo "$MEM_USAGE > 80" | bc -l) )); then
    echo "High memory usage: $MEM_USAGE%"
    # 发送告警
fi

# 磁盘使用率检查
DISK_USAGE=$(df / | tail -1 | awk '{print $5}' | sed 's/%//')
if [ $DISK_USAGE -gt 80 ]; then
    echo "High disk usage: $DISK_USAGE%"
    # 发送告警
fi
```

#### 应用健康检查
```bash
#!/bin/bash
# health_check.sh

# 检查应用响应
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)
if [ $RESPONSE -ne 200 ]; then
    echo "Application health check failed: HTTP $RESPONSE"
    # 尝试重启服务
    docker-compose restart app
fi

# 检查数据库连接
DB_STATUS=$(docker exec mysql mysql -u root -p$MYSQL_ROOT_PASSWORD -e "SELECT 1" 2>/dev/null)
if [ $? -ne 0 ]; then
    echo "Database connection failed"
    # 发送告警
fi
```

---

## 📞 支持联系

### 技术支持
- **邮箱**: admin-support@codenavigator.com
- **电话**: +86-400-XXX-XXXX
- **在线文档**: https://docs.codenavigator.com

### 紧急联系
- **24/7热线**: +86-138-XXXX-XXXX
- **紧急邮箱**: emergency@codenavigator.com
- **值班群**: [微信群二维码]

### 社区支持
- **官方论坛**: https://forum.codenavigator.com
- **GitHub Issues**: https://github.com/codenavigator/issues
- **QQ群**: 123456789

---

**📋 需要支持？** 随时联系我们的技术团队！

**🛠️ 愉快运维！** 让我们一起构建更稳定的CodeNavigator服务！