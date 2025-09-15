# CodeNavigator API 文档

## 📋 目录

1. [API概述](#api概述)
2. [认证授权](#认证授权)
3. [用户管理API](#用户管理api)
4. [学习路径API](#学习路径api)
5. [AI对话API](#ai对话api)
6. [代码分析API](#代码分析api)
7. [进度跟踪API](#进度跟踪api)
8. [文件管理API](#文件管理api)
9. [错误处理](#错误处理)
10. [SDK和示例](#sdk和示例)

---

## 🌐 API概述

### 基本信息
- **基础URL**: `https://api.codenavigator.com/v1`
- **协议**: HTTPS
- **数据格式**: JSON
- **字符编码**: UTF-8
- **API版本**: v1.0.0

### 请求格式
```http
GET /api/v1/users/profile HTTP/1.1
Host: api.codenavigator.com
Authorization: Bearer <access_token>
Content-Type: application/json
Accept: application/json
```

### 响应格式
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    // 具体数据内容
  },
  "timestamp": "2024-01-15T10:30:00Z",
  "requestId": "req_123456789"
}
```

### 状态码说明
| 状态码 | 说明 | 描述 |
|--------|------|------|
| 200 | OK | 请求成功 |
| 201 | Created | 资源创建成功 |
| 400 | Bad Request | 请求参数错误 |
| 401 | Unauthorized | 未授权访问 |
| 403 | Forbidden | 权限不足 |
| 404 | Not Found | 资源不存在 |
| 429 | Too Many Requests | 请求频率超限 |
| 500 | Internal Server Error | 服务器内部错误 |

---

## 🔐 认证授权

### JWT Token认证
CodeNavigator API使用JWT Token进行身份认证。

#### 获取Token
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "your_password"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600,
    "tokenType": "Bearer",
    "user": {
      "id": 123,
      "username": "user@example.com",
      "role": "USER"
    }
  }
}
```

#### 刷新Token
```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### 使用Token
在所有需要认证的请求中添加Authorization头：
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### API Key认证（用于服务间调用）
```http
X-API-Key: your_api_key_here
```

---

## 👥 用户管理API

### 用户注册
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "newuser@example.com",
  "password": "secure_password",
  "confirmPassword": "secure_password",
  "firstName": "张",
  "lastName": "三",
  "phoneNumber": "+86 138 0013 8000"
}
```

**响应示例**:
```json
{
  "code": 201,
  "message": "User registered successfully",
  "data": {
    "userId": 456,
    "username": "newuser@example.com",
    "status": "PENDING_VERIFICATION"
  }
}
```

### 获取用户信息
```http
GET /api/v1/users/profile
Authorization: Bearer <access_token>
```

**响应示例**:
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "id": 123,
    "username": "user@example.com",
    "firstName": "张",
    "lastName": "三",
    "email": "user@example.com",
    "phoneNumber": "+86 138 0013 8000",
    "role": "USER",
    "status": "ACTIVE",
    "profile": {
      "avatar": "https://cdn.example.com/avatars/123.jpg",
      "bio": "热爱编程的学习者",
      "learningGoals": ["Java", "Spring Boot", "微服务"],
      "experience": "BEGINNER"
    },
    "statistics": {
      "totalLearningTime": 3600,
      "completedPaths": 2,
      "currentLevel": 5,
      "achievements": 8
    },
    "createdAt": "2024-01-01T00:00:00Z",
    "lastLoginAt": "2024-01-15T10:30:00Z"
  }
}
```

### 更新用户信息
```http
PUT /api/v1/users/profile
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "firstName": "李",
  "lastName": "四",
  "phoneNumber": "+86 139 0013 9000",
  "profile": {
    "bio": "全栈开发工程师",
    "learningGoals": ["React", "Node.js", "Docker"]
  }
}
```

### 修改密码
```http
PUT /api/v1/users/password
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "currentPassword": "old_password",
  "newPassword": "new_secure_password",
  "confirmPassword": "new_secure_password"
}
```

---

## 🛤️ 学习路径API

### 获取学习路径列表
```http
GET /api/v1/learning-paths?category=backend&difficulty=beginner&page=1&size=10
Authorization: Bearer <access_token>
```

**查询参数**:
- `category`: 分类（backend, frontend, mobile, ai等）
- `difficulty`: 难度（beginner, intermediate, advanced）
- `tags`: 标签（多个用逗号分隔）
- `page`: 页码（默认1）
- `size`: 每页大小（默认10，最大100）

**响应示例**:
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Java基础入门",
        "description": "从零开始学习Java编程语言",
        "category": "backend",
        "difficulty": "beginner",
        "estimatedHours": 40,
        "tags": ["Java", "OOP", "基础"],
        "instructor": {
          "id": 101,
          "name": "张老师",
          "avatar": "https://cdn.example.com/avatars/101.jpg"
        },
        "statistics": {
          "enrolledCount": 1250,
          "completedCount": 890,
          "averageRating": 4.8,
          "reviewCount": 156
        },
        "thumbnail": "https://cdn.example.com/thumbnails/java-basics.jpg",
        "createdAt": "2024-01-01T00:00:00Z",
        "updatedAt": "2024-01-15T10:30:00Z"
      }
    ],
    "page": {
      "number": 1,
      "size": 10,
      "totalElements": 25,
      "totalPages": 3
    }
  }
}
```

### 获取学习路径详情
```http
GET /api/v1/learning-paths/1
Authorization: Bearer <access_token>
```

**响应示例**:
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "title": "Java基础入门",
    "description": "从零开始学习Java编程语言，掌握面向对象编程思想",
    "category": "backend",
    "difficulty": "beginner",
    "estimatedHours": 40,
    "prerequisites": [],
    "learningObjectives": [
      "掌握Java基本语法",
      "理解面向对象编程",
      "能够编写简单的Java程序"
    ],
    "modules": [
      {
        "id": 101,
        "title": "Java环境搭建",
        "description": "安装JDK和IDE，配置开发环境",
        "order": 1,
        "estimatedMinutes": 120,
        "type": "video",
        "resources": [
          {
            "type": "video",
            "title": "JDK安装教程",
            "url": "https://cdn.example.com/videos/jdk-install.mp4",
            "duration": 900
          },
          {
            "type": "document",
            "title": "环境配置指南",
            "url": "https://cdn.example.com/docs/java-setup.pdf"
          }
        ]
      }
    ],
    "instructor": {
      "id": 101,
      "name": "张老师",
      "title": "高级Java工程师",
      "bio": "10年Java开发经验，曾任职于阿里巴巴",
      "avatar": "https://cdn.example.com/avatars/101.jpg"
    },
    "enrollment": {
      "isEnrolled": true,
      "enrolledAt": "2024-01-10T09:00:00Z",
      "progress": {
        "completedModules": 3,
        "totalModules": 8,
        "completionPercentage": 37.5,
        "lastAccessedAt": "2024-01-15T10:30:00Z"
      }
    }
  }
}
```

### 报名学习路径
```http
POST /api/v1/learning-paths/1/enroll
Authorization: Bearer <access_token>
```

### 更新学习进度
```http
PUT /api/v1/learning-paths/1/modules/101/progress
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "status": "COMPLETED",
  "timeSpent": 1800,
  "notes": "已完成JDK安装和配置"
}
```

---

## 🤖 AI对话API

### 开始新对话
```http
POST /api/v1/conversations
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "title": "Java学习咨询",
  "context": {
    "learningPath": 1,
    "currentModule": 101,
    "userLevel": "beginner"
  }
}
```

**响应示例**:
```json
{
  "code": 201,
  "message": "Conversation created successfully",
  "data": {
    "conversationId": "conv_abc123",
    "title": "Java学习咨询",
    "status": "ACTIVE",
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

### 发送消息
```http
POST /api/v1/conversations/conv_abc123/messages
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "content": "我在学习Java时遇到了一个问题，能帮我看看这段代码吗？",
  "type": "text",
  "metadata": {
    "code": "public class Hello {\n    public static void main(String[] args) {\n        System.out.println(\"Hello World\");\n    }\n}",
    "language": "java"
  }
}
```

**响应示例**:
```json
{
  "code": 201,
  "message": "Message sent successfully",
  "data": {
    "messageId": "msg_def456",
    "content": "我在学习Java时遇到了一个问题，能帮我看看这段代码吗？",
    "type": "text",
    "sender": "USER",
    "timestamp": "2024-01-15T10:30:00Z",
    "aiResponse": {
      "messageId": "msg_ghi789",
      "content": "这段代码看起来是一个标准的Hello World程序。代码结构很正确！让我为你详细解释一下...",
      "type": "text",
      "sender": "ASSISTANT",
      "timestamp": "2024-01-15T10:30:05Z",
      "confidence": 0.95,
      "suggestedActions": [
        {
          "type": "runCode",
          "label": "运行代码",
          "url": "/api/v1/code/execute"
        },
        {
          "type": "learnMore",
          "label": "了解更多Java语法",
          "url": "/learning-paths/1/modules/102"
        }
      ]
    }
  }
}
```

### 获取对话历史
```http
GET /api/v1/conversations/conv_abc123/messages?page=1&size=20
Authorization: Bearer <access_token>
```

### 获取用户对话列表
```http
GET /api/v1/conversations?status=active&page=1&size=10
Authorization: Bearer <access_token>
```

---

## 🔍 代码分析API

### 提交代码分析
```http
POST /api/v1/code-analysis
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "code": "public class Calculator {\n    public int add(int a, int b) {\n        return a + b;\n    }\n}",
  "language": "java",
  "analysisType": "quality",
  "options": {
    "includePerformance": true,
    "includeSecurity": true,
    "includeStyle": true
  }
}
```

**响应示例**:
```json
{
  "code": 201,
  "message": "Code analysis started",
  "data": {
    "analysisId": "analysis_xyz789",
    "status": "PROCESSING",
    "estimatedTime": 30,
    "submittedAt": "2024-01-15T10:30:00Z"
  }
}
```

### 获取分析结果
```http
GET /api/v1/code-analysis/analysis_xyz789
Authorization: Bearer <access_token>
```

**响应示例**:
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "analysisId": "analysis_xyz789",
    "status": "COMPLETED",
    "submittedAt": "2024-01-15T10:30:00Z",
    "completedAt": "2024-01-15T10:30:25Z",
    "result": {
      "overallScore": 85,
      "metrics": {
        "maintainability": 90,
        "readability": 88,
        "complexity": 75,
        "performance": 82,
        "security": 95
      },
      "issues": [
        {
          "id": "issue_001",
          "severity": "WARNING",
          "category": "STYLE",
          "message": "建议为类添加JavaDoc注释",
          "line": 1,
          "column": 1,
          "suggestions": [
            "添加类级别的JavaDoc文档",
            "说明类的用途和使用方法"
          ]
        }
      ],
      "suggestions": [
        {
          "type": "DOCUMENTATION",
          "priority": "MEDIUM",
          "title": "改进代码文档",
          "description": "为类和方法添加详细的JavaDoc注释",
          "examples": [
            "/**\n * 计算器类，提供基本的数学运算功能\n */\npublic class Calculator {"
          ]
        }
      ],
      "statistics": {
        "linesOfCode": 5,
        "complexity": 1,
        "testCoverage": 0,
        "duplicateLines": 0
      }
    }
  }
}
```

### 批量代码分析
```http
POST /api/v1/code-analysis/batch
Authorization: Bearer <access_token>
Content-Type: multipart/form-data

files: [选择文件]
language: java
analysisType: full
```

---

## 📊 进度跟踪API

### 获取学习统计
```http
GET /api/v1/users/statistics
Authorization: Bearer <access_token>
```

**响应示例**:
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "overview": {
      "totalLearningTime": 7200,
      "streakDays": 15,
      "completedPaths": 3,
      "currentLevel": 8,
      "experiencePoints": 2450
    },
    "recent": {
      "last7Days": {
        "learningTime": 1200,
        "completedModules": 5,
        "conversationsCount": 12
      },
      "last30Days": {
        "learningTime": 4800,
        "completedModules": 18,
        "conversationsCount": 45
      }
    },
    "achievements": [
      {
        "id": "first_completion",
        "title": "初来乍到",
        "description": "完成第一个学习模块",
        "icon": "🎉",
        "unlockedAt": "2024-01-05T14:20:00Z"
      }
    ],
    "skillLevels": {
      "java": {
        "level": 3,
        "progress": 65,
        "nextLevelPoints": 350
      },
      "spring": {
        "level": 2,
        "progress": 30,
        "nextLevelPoints": 700
      }
    }
  }
}
```

### 记录学习活动
```http
POST /api/v1/users/activities
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "type": "MODULE_COMPLETED",
  "resourceId": "module_101",
  "timeSpent": 1800,
  "metadata": {
    "score": 85,
    "attempts": 2,
    "notes": "掌握了Java基本语法"
  }
}
```

### 获取学习日历
```http
GET /api/v1/users/calendar?year=2024&month=1
Authorization: Bearer <access_token>
```

**响应示例**:
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "year": 2024,
    "month": 1,
    "days": [
      {
        "date": "2024-01-15",
        "learningTime": 3600,
        "activitiesCount": 5,
        "completedModules": 2,
        "streak": true,
        "highlights": [
          "完成Java基础环境搭建",
          "与AI助手讨论了面向对象概念"
        ]
      }
    ],
    "summary": {
      "totalLearningDays": 20,
      "totalLearningTime": 72000,
      "averageDailyTime": 3600,
      "longestStreak": 12
    }
  }
}
```

---

## 📁 文件管理API

### 上传文件
```http
POST /api/v1/files/upload
Authorization: Bearer <access_token>
Content-Type: multipart/form-data

file: [选择文件]
category: code
tags: java,homework
description: 第三章练习代码
```

**响应示例**:
```json
{
  "code": 201,
  "message": "File uploaded successfully",
  "data": {
    "fileId": "file_abc123",
    "filename": "Calculator.java",
    "originalName": "Calculator.java",
    "size": 1024,
    "mimeType": "text/x-java-source",
    "category": "code",
    "tags": ["java", "homework"],
    "description": "第三章练习代码",
    "url": "https://cdn.example.com/files/file_abc123",
    "downloadUrl": "https://api.example.com/v1/files/file_abc123/download",
    "uploadedAt": "2024-01-15T10:30:00Z",
    "expiresAt": "2024-01-22T10:30:00Z"
  }
}
```

### 获取文件列表
```http
GET /api/v1/files?category=code&tags=java&page=1&size=10
Authorization: Bearer <access_token>
```

### 下载文件
```http
GET /api/v1/files/file_abc123/download
Authorization: Bearer <access_token>
```

### 删除文件
```http
DELETE /api/v1/files/file_abc123
Authorization: Bearer <access_token>
```

---

## ❌ 错误处理

### 错误响应格式
```json
{
  "code": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "code": "INVALID_EMAIL",
      "message": "请输入有效的邮箱地址"
    },
    {
      "field": "password",
      "code": "PASSWORD_TOO_SHORT",
      "message": "密码长度至少8位"
    }
  ],
  "timestamp": "2024-01-15T10:30:00Z",
  "requestId": "req_123456789"
}
```

### 常见错误码
| 错误码 | HTTP状态 | 说明 |
|--------|----------|------|
| INVALID_TOKEN | 401 | Token无效或已过期 |
| INSUFFICIENT_PERMISSIONS | 403 | 权限不足 |
| RESOURCE_NOT_FOUND | 404 | 资源不存在 |
| VALIDATION_FAILED | 400 | 请求参数验证失败 |
| DUPLICATE_RESOURCE | 409 | 资源已存在 |
| RATE_LIMIT_EXCEEDED | 429 | 请求频率超限 |
| INTERNAL_ERROR | 500 | 服务器内部错误 |

### 错误处理最佳实践
```javascript
// JavaScript示例
async function callAPI(url, options) {
  try {
    const response = await fetch(url, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
        ...options.headers
      },
      ...options
    });
    
    const data = await response.json();
    
    if (!response.ok) {
      throw new APIError(data.code, data.message, data.errors);
    }
    
    return data.data;
  } catch (error) {
    if (error.code === 'INVALID_TOKEN') {
      // 尝试刷新token
      await refreshToken();
      return callAPI(url, options); // 重试
    }
    throw error;
  }
}
```

---

## 🛠️ SDK和示例

### JavaScript SDK
```bash
npm install @codenavigator/javascript-sdk
```

```javascript
import { CodeNavigatorClient } from '@codenavigator/javascript-sdk';

const client = new CodeNavigatorClient({
  baseURL: 'https://api.codenavigator.com/v1',
  token: 'your_access_token'
});

// 获取用户信息
const user = await client.users.getProfile();

// 发送AI对话消息
const response = await client.conversations.sendMessage('conv_abc123', {
  content: 'Hello, AI!',
  type: 'text'
});

// 提交代码分析
const analysis = await client.codeAnalysis.submit({
  code: 'public class Hello { ... }',
  language: 'java'
});
```

### Python SDK
```bash
pip install codenavigator-python-sdk
```

```python
from codenavigator import CodeNavigatorClient

client = CodeNavigatorClient(
    base_url='https://api.codenavigator.com/v1',
    token='your_access_token'
)

# 获取学习路径
paths = client.learning_paths.list(category='backend', difficulty='beginner')

# 记录学习活动
client.users.record_activity({
    'type': 'MODULE_COMPLETED',
    'resource_id': 'module_101',
    'time_spent': 1800
})
```

### Java SDK
```xml
<dependency>
    <groupId>com.codenavigator</groupId>
    <artifactId>java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
import com.codenavigator.sdk.CodeNavigatorClient;
import com.codenavigator.sdk.model.*;

CodeNavigatorClient client = CodeNavigatorClient.builder()
    .baseUrl("https://api.codenavigator.com/v1")
    .token("your_access_token")
    .build();

// 获取对话列表
List<Conversation> conversations = client.conversations().list();

// 开始新对话
Conversation conversation = client.conversations().create(
    CreateConversationRequest.builder()
        .title("Java学习咨询")
        .build()
);
```

### cURL示例
```bash
# 获取用户信息
curl -X GET https://api.codenavigator.com/v1/users/profile \
  -H "Authorization: Bearer your_access_token" \
  -H "Content-Type: application/json"

# 发送AI对话消息
curl -X POST https://api.codenavigator.com/v1/conversations/conv_abc123/messages \
  -H "Authorization: Bearer your_access_token" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "请帮我分析这段Java代码",
    "type": "text"
  }'

# 上传代码文件
curl -X POST https://api.codenavigator.com/v1/files/upload \
  -H "Authorization: Bearer your_access_token" \
  -F "file=@Calculator.java" \
  -F "category=code" \
  -F "tags=java,homework"
```

---

## 📊 API使用限制

### 频率限制
| 用户类型 | 每分钟请求数 | 每小时请求数 | 每日请求数 |
|----------|--------------|--------------|------------|
| 免费用户 | 60 | 1000 | 10000 |
| 付费用户 | 300 | 10000 | 100000 |
| 企业用户 | 1000 | 50000 | 无限制 |

### 数据限制
- **单次请求大小**: 最大 10MB
- **文件上传大小**: 最大 100MB
- **代码分析**: 最大 1MB 代码
- **对话历史**: 保留 30 天

### 响应时间SLA
- **基础API**: 95%请求 < 200ms
- **AI对话**: 95%请求 < 3s
- **代码分析**: 95%请求 < 30s

---

## 📞 支持和反馈

### 技术支持
- **开发者文档**: https://docs.codenavigator.com
- **API状态页**: https://status.codenavigator.com
- **技术支持**: api-support@codenavigator.com

### SDK仓库
- **JavaScript**: https://github.com/codenavigator/javascript-sdk
- **Python**: https://github.com/codenavigator/python-sdk
- **Java**: https://github.com/codenavigator/java-sdk

### 变更日志
- **v1.0.0** (2024-01-15): 初始版本发布
- **v1.0.1** (即将发布): 新增批量操作API

---

**📧 需要帮助？** 联系我们的API支持团队：api-support@codenavigator.com

**🚀 开始构建！** 使用CodeNavigator API创造更智能的学习体验！