# AI模型动态切换功能文档

## 功能概述

CodeNavigator现已支持多个AI模型提供商的动态切换，包括OpenAI和DeepSeek，用户可以根据需要在不同的AI模型之间进行切换。

## 支持的AI提供商

| 提供商 | 代码 | 模型 | 描述 |
|--------|------|------|------|
| OpenAI | `openai` | gpt-4 | GPT系列模型 |
| DeepSeek | `deepseek` | deepseek-chat | DeepSeek对话模型 |
| Claude | `claude` | claude-3 | Anthropic Claude模型 (待实现) |
| Gemini | `gemini` | gemini-pro | Google Gemini模型 (待实现) |

## 配置说明

### 应用配置文件 (`application.yml`)

```yaml
# AI模型配置
ai:
  default-provider: openai  # 默认提供商
  providers:
    openai:
      api-key: ${OPENAI_API_KEY:your-openai-key}
      base-url: https://api.openai.com/v1
      model-name: gpt-4
      temperature: 0.7
      max-tokens: 2000
      timeout: 60s
    deepseek:
      api-key: ${DEEPSEEK_API_KEY:your-deepseek-key}
      base-url: https://api.deepseek.com/v1
      model-name: deepseek-chat
      temperature: 0.7
      max-tokens: 2000
      timeout: 60s
```

### 环境变量

```bash
# OpenAI API密钥
export OPENAI_API_KEY="sk-xxxxx"

# DeepSeek API密钥
export DEEPSEEK_API_KEY="sk-xxxxx"
```

## API接口

### 1. 获取当前AI提供商

```http
GET /ai-model/current
```

**响应示例：**
```json
{
  "provider": "openai",
  "displayName": "OpenAI",
  "description": "GPT系列模型"
}
```

### 2. 切换AI提供商

```http
POST /ai-model/switch/{provider}
```

**参数：**
- `provider`: AI提供商代码 (`openai`, `deepseek`, `claude`, `gemini`)

**响应示例：**
```json
{
  "success": true,
  "message": "已切换到DeepSeek",
  "provider": "deepseek"
}
```

### 3. 获取所有可用提供商

```http
GET /ai-model/providers
```

**响应示例：**
```json
[
  {
    "provider": "openai",
    "displayName": "OpenAI",
    "description": "GPT系列模型",
    "available": true,
    "current": true
  },
  {
    "provider": "deepseek",
    "displayName": "DeepSeek",
    "description": "DeepSeek对话模型",
    "available": true,
    "current": false
  }
]
```

### 4. 获取提供商状态

```http
GET /ai-model/providers/{provider}/status
```

**响应示例：**
```json
{
  "provider": "openai",
  "displayName": "OpenAI",
  "available": true,
  "current": false,
  "modelName": "gpt-4",
  "temperature": 0.7,
  "maxTokens": 2000,
  "baseUrl": "https://api.openai.com/v1",
  "hasApiKey": true
}
```

### 5. 测试提供商连接

```http
POST /ai-model/providers/{provider}/test
```

**响应示例：**
```json
{
  "provider": "openai",
  "displayName": "OpenAI",
  "connected": true,
  "responseTime": 1200,
  "message": "连接成功"
}
```

### 6. 发送测试消息

```http
POST /ai-model/test-message
Content-Type: application/json

{
  "message": "Hello, how are you?",
  "provider": "deepseek"  // 可选，不指定则使用当前默认提供商
}
```

**响应示例：**
```json
{
  "provider": "deepseek",
  "message": "Hello, how are you?",
  "response": "Hello! I'm doing well, thank you for asking. How can I help you today?",
  "success": true
}
```

## 对话中指定AI提供商

在发送对话消息时，可以通过`preferredProvider`字段指定特定的AI提供商：

```http
POST /conversation/message
Content-Type: application/json

{
  "userId": "user-123",
  "message": "解释一下Spring Boot的自动配置原理",
  "preferredProvider": "deepseek"  // 指定使用DeepSeek模型
}
```

## 使用示例

### 1. 检查可用提供商

```bash
curl -X GET http://localhost:8080/ai-model/providers
```

### 2. 切换到DeepSeek

```bash
curl -X POST http://localhost:8080/ai-model/switch/deepseek
```

### 3. 测试DeepSeek连接

```bash
curl -X POST http://localhost:8080/ai-model/providers/deepseek/test
```

### 4. 使用DeepSeek发送测试消息

```bash
curl -X POST http://localhost:8080/ai-model/test-message \
  -H "Content-Type: application/json" \
  -d '{
    "message": "什么是机器学习？",
    "provider": "deepseek"
  }'
```

### 5. 在对话中使用指定提供商

```bash
curl -X POST http://localhost:8080/conversation/message \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "message": "我想学习Spring Boot",
    "preferredProvider": "deepseek"
  }'
```

## 技术实现

### 核心组件

1. **AiProvider枚举** - 定义支持的AI提供商
2. **AiModelConfig** - 配置管理类，读取YAML配置
3. **AiModelService** - AI模型服务接口
4. **AiModelServiceImpl** - 服务实现，支持动态切换
5. **AiModelController** - REST API控制器
6. **ConversationEngine** - 增强支持多模型对话

### 切换机制

- 使用`AtomicReference`保证线程安全的提供商切换
- 配置热加载，支持运行时配置更新
- 智能降级，当指定提供商不可用时自动回退到默认提供商
- 统一的HTTP客户端配置，支持超时和重试机制

### 安全考虑

- API密钥通过环境变量配置，不硬编码在代码中
- 支持配置验证，确保必要的参数已正确设置
- 错误处理和日志记录，便于问题排查

## 注意事项

1. **API密钥配置**：确保为每个要使用的提供商配置正确的API密钥
2. **网络连接**：某些提供商可能需要特殊的网络配置
3. **模型差异**：不同提供商的模型可能有不同的响应格式和能力
4. **成本控制**：不同提供商的计费方式可能不同，注意监控使用成本
5. **性能差异**：不同提供商的响应时间和质量可能有差异

## 未来扩展

- 支持更多AI提供商（Claude、Gemini等）
- 添加模型性能监控和统计
- 实现基于负载的智能路由
- 支持模型A/B测试
- 添加缓存机制减少API调用成本