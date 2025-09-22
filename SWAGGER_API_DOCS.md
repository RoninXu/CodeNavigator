# CodeNavigator API 文档

## Swagger UI 访问说明

本项目已集成 Swagger/OpenAPI 3.0，为所有 API 接口提供了详细的文档和交互式测试界面。

### 访问地址

启动应用后，可通过以下地址访问 API 文档：

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs
- **API 文档入口**: http://localhost:8080/api-docs （会重定向到 Swagger UI）

### API 接口概览

项目包含以下主要 API 模块：

#### 1. 对话管理 (ConversationController)
- `POST /conversation/message` - 发送对话消息
- `GET /conversation/sessions/{sessionId}` - 获取会话信息
- `POST /conversation/sessions/{sessionId}/end` - 结束会话

#### 2. 学习路径管理 (LearningPathController)
- `POST /learning-paths/{pathId}/start` - 开始学习路径
- `POST /learning-paths/generate` - 生成学习路径
- `GET /learning-paths/{pathId}/progress` - 获取学习进度

#### 3. 学习模块管理 (ModuleController)
- `POST /modules/{moduleId}/complete` - 完成学习模块
- `POST /modules/{moduleId}/submit-task` - 提交学习任务
- `GET /modules/{moduleId}/hints` - 获取学习提示

### API 接口详细说明

#### 发送对话消息
```
POST /conversation/message
Content-Type: application/json

{
  "userId": "user-123",
  "message": "如何学习Spring Boot？",
  "type": "GENERAL_QUESTION"
}
```

#### 生成学习路径
```
POST /learning-paths/generate
Content-Type: application/json

{
  "technology": "Spring Boot",
  "level": "INTERMEDIATE",
  "goals": ["web开发", "微服务"]
}
```

#### 提交学习任务
```
POST /modules/{moduleId}/submit-task?taskType=code_review
Content-Type: application/json

{
  "code": "public class Hello { public static void main(String[] args) { System.out.println(\"Hello World\"); } }",
  "language": "java"
}
```

### 配置说明

Swagger 配置位于 `application.yml`:

```yaml
springdoc:
  api-docs:
    path: /api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    operationsSorter: method
    tagsSorter: alpha
    tryItOutEnabled: true
    filter: true
```

### 启动应用

1. 确保 MySQL 和 Redis 服务正在运行
2. 配置环境变量：
   ```bash
   export DB_USERNAME=root
   export DB_PASSWORD=your_password
   export OPENAI_API_KEY=your_openai_key
   ```
3. 启动应用：
   ```bash
   mvn spring-boot:run -pl codenavigator-app
   ```
4. 访问 http://localhost:8080/swagger-ui.html

### 注意事项

- 所有 API 接口都有详细的参数说明和示例
- 可以直接在 Swagger UI 中测试 API 接口
- 页面渲染相关的方法已从 API 文档中隐藏
- 支持 JSON 格式的请求和响应
- 包含完整的错误响应码说明

### 开发建议

1. 新增 API 接口时，请添加相应的 Swagger 注解
2. 使用 `@Operation` 描述接口功能
3. 使用 `@Parameter` 描述参数
4. 使用 `@ApiResponse` 描述响应
5. 页面方法使用 `@Hidden` 注解排除