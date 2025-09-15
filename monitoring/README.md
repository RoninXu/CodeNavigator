# CodeNavigator 监控和日志系统

## 概述

本目录包含了 CodeNavigator 生产环境的完整监控和日志解决方案，基于 Prometheus + Grafana + ELK Stack 构建。

## 架构组件

### 1. 监控组件
- **Prometheus**: 指标收集和存储
- **Grafana**: 可视化仪表板
- **AlertManager**: 告警管理和通知
- **各种Exporter**: 系统和应用指标导出器

### 2. 日志组件
- **Elasticsearch**: 日志存储和搜索
- **Kibana**: 日志分析和可视化
- **Filebeat**: 日志收集和传输

### 3. 系统监控
- **Node Exporter**: 系统级指标
- **cAdvisor**: 容器监控
- **MySQL Exporter**: 数据库监控
- **Redis Exporter**: 缓存监控
- **Nginx Exporter**: Web服务器监控

## 文件说明

### 配置文件
- `prometheus.yml`: Prometheus主配置文件
- `alertmanager.yml`: 告警管理器配置
- `filebeat.yml`: 日志收集配置
- `logback-spring.xml`: 应用日志配置

### 仪表板
- `grafana/dashboards/codenavigator-dashboard.json`: 主监控仪表板
- `grafana/datasources/prometheus.yml`: Grafana数据源配置

### 告警规则
- `alerts/application-alerts.yml`: 应用监控告警规则

### 部署文件
- `docker-monitoring.yml`: 独立监控服务部署文件

## 部署方式

### 1. 完整部署（推荐）
使用主目录的 `docker-compose.prod.yml` 部署完整的应用和监控栈：

```bash
# 设置环境变量
export DB_PASSWORD="your_db_password"
export REDIS_PASSWORD="your_redis_password"
export GRAFANA_PASSWORD="your_grafana_password"

# 启动完整服务
docker-compose -f docker-compose.prod.yml up -d
```

### 2. 独立监控部署
仅部署监控组件，连接到现有的应用：

```bash
cd monitoring
docker-compose -f docker-monitoring.yml up -d
```

## 访问地址

### 监控服务
- **Grafana**: http://localhost:3000
  - 用户名: admin
  - 密码: 通过环境变量设置
- **Prometheus**: http://localhost:9090
- **AlertManager**: http://localhost:9093

### 日志服务
- **Kibana**: http://localhost:5601
- **Elasticsearch**: http://localhost:9200

### 应用监控端点
- **应用健康检查**: http://localhost:8080/actuator/health
- **应用指标**: http://localhost:8080/actuator/prometheus
- **Nginx状态**: http://localhost:80/nginx_status

## 监控指标

### 应用指标
- HTTP请求量和响应时间
- 错误率和状态码分布
- JVM内存和GC性能
- 线程池使用情况
- 数据库连接池状态
- Redis操作统计

### 系统指标
- CPU和内存使用率
- 磁盘空间和I/O
- 网络流量
- 容器资源使用

### 业务指标
- 用户注册和登录统计
- AI服务调用量
- 文件上传统计
- 错误和异常统计

## 告警规则

### 关键告警 (Critical)
- 应用下线
- 数据库连接池耗尽
- 磁盘空间不足（<10%）

### 警告告警 (Warning)
- HTTP错误率过高（>5%）
- 响应时间过慢（>2s）
- 内存使用率过高（>80%）
- CPU使用率过高（>80%）

### 业务告警
- 用户注册失败率过高
- AI服务超时
- 文件上传失败

## 日志配置

### 日志级别
- **生产环境**: INFO级别，ERROR单独记录
- **开发环境**: DEBUG级别，控制台输出

### 日志格式
- JSON格式，便于Elasticsearch解析
- 包含链路追踪ID (traceId, spanId)
- 包含用户ID和业务上下文

### 日志轮转
- 按大小轮转：100MB
- 按时间轮转：每天
- 保留30天历史日志
- 总大小限制：3GB

## 环境变量配置

### 告警配置
```bash
# SMTP配置
SMTP_HOST=smtp.gmail.com:587
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_app_password

# 告警接收邮箱
CRITICAL_ALERT_EMAIL=admin@codenavigator.com
WARNING_ALERT_EMAIL=dev@codenavigator.com
BUSINESS_ALERT_EMAIL=business@codenavigator.com

# Slack告警（可选）
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/...
```

### Grafana配置
```bash
GRAFANA_USER=admin
GRAFANA_PASSWORD=secure_password
```

## 性能调优

### Prometheus
- 数据保留期：30天
- 采集间隔：15秒（应用10秒）
- 存储压缩：启用

### Elasticsearch
- 堆内存：512MB-1GB
- 索引刷新间隔：5秒
- 分片数：1（单节点）

### 日志优化
- 异步日志写入
- 批量处理
- 压缩存储

## 故障排查

### 常见问题
1. **Prometheus无法连接应用**
   - 检查网络连接
   - 验证端点可访问性
   - 确认防火墙配置

2. **Grafana显示无数据**
   - 检查Prometheus数据源配置
   - 验证查询语句
   - 确认时间范围

3. **告警不工作**
   - 检查AlertManager配置
   - 验证SMTP设置
   - 查看AlertManager日志

4. **日志收集失败**
   - 检查Filebeat配置
   - 验证Elasticsearch连接
   - 确认日志文件权限

### 调试命令
```bash
# 检查服务状态
docker-compose ps

# 查看服务日志
docker-compose logs prometheus
docker-compose logs grafana
docker-compose logs alertmanager

# 测试Prometheus查询
curl http://localhost:9090/api/v1/query?query=up

# 测试Elasticsearch
curl http://localhost:9200/_cluster/health
```

## 扩展配置

### 添加新的监控目标
1. 在 `prometheus.yml` 中添加新的 job
2. 配置相应的告警规则
3. 在Grafana中创建仪表板

### 自定义告警
1. 修改 `alerts/application-alerts.yml`
2. 重新加载Prometheus配置
3. 验证告警规则

### 集成外部系统
- Slack通知集成
- 钉钉告警集成
- 邮件告警模板自定义

## 安全注意事项

1. **访问控制**
   - 限制监控端点访问
   - 配置认证和授权
   - 使用HTTPS传输

2. **数据保护**
   - 敏感信息脱敏
   - 日志访问权限控制
   - 定期备份监控数据

3. **网络安全**
   - 内网访问限制
   - 防火墙配置
   - VPN访问控制