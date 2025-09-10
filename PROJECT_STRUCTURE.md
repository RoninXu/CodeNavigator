# CodeNavigator 项目结构规划

## 📁 Maven多模块项目结构

```
CodeNavigator/
├── pom.xml                              # 父pom文件
├── README.md                            # 项目说明文档
├── DEVELOPMENT_PLAN.md                  # 开发计划
├── docker-compose.yml                   # 开发环境容器编排
├── .github/                             # GitHub Actions配置
│   └── workflows/
│       ├── ci.yml                       # 持续集成
│       └── cd.yml                       # 持续部署
│
├── codenavigator-common/                # 公共模块
│   ├── pom.xml
│   └── src/main/java/com/codenavigator/common/
│       ├── constants/                   # 常量定义
│       ├── enums/                       # 枚举类
│       ├── exceptions/                  # 自定义异常
│       ├── utils/                       # 工具类
│       └── dto/                         # 数据传输对象
│
├── codenavigator-core/                  # 核心业务模块
│   ├── pom.xml
│   └── src/main/java/com/codenavigator/core/
│       ├── entity/                      # 实体类
│       │   ├── User.java
│       │   ├── LearningPath.java
│       │   ├── LearningModule.java
│       │   ├── UserProgress.java
│       │   └── LearningNote.java
│       ├── repository/                  # 数据访问层
│       │   ├── UserRepository.java
│       │   ├── LearningPathRepository.java
│       │   └── UserProgressRepository.java
│       ├── service/                     # 业务逻辑层
│       │   ├── ConversationService.java
│       │   ├── LearningPathService.java
│       │   ├── CodeAnalysisService.java
│       │   └── NoteService.java
│       └── config/                      # 配置类
│           ├── DatabaseConfig.java
│           └── RedisConfig.java
│
├── codenavigator-ai/                    # AI服务模块
│   ├── pom.xml
│   └── src/main/java/com/codenavigator/ai/
│       ├── engine/                      # AI引擎
│       │   ├── ConversationEngine.java
│       │   ├── CodeAnalyzer.java
│       │   └── PathGenerator.java
│       ├── model/                       # AI模型相关
│       │   ├── ConversationContext.java
│       │   ├── AnalysisResult.java
│       │   └── LearningPlan.java
│       ├── prompt/                      # Prompt模板
│       │   ├── CodeAnalysisPrompt.java
│       │   └── PathGenerationPrompt.java
│       └── config/                      # AI配置
│           └── LangChainConfig.java
│
├── codenavigator-web/                   # Web控制器模块
│   ├── pom.xml
│   └── src/main/java/com/codenavigator/web/
│       ├── controller/                  # 控制器
│       │   ├── ConversationController.java
│       │   ├── LearningController.java
│       │   └── UserController.java
│       ├── dto/                         # Web层DTO
│       │   ├── request/
│       │   └── response/
│       ├── interceptor/                 # 拦截器
│       │   └── AuthenticationInterceptor.java
│       └── config/                      # Web配置
│           ├── WebMvcConfig.java
│           └── CorsConfig.java
│
├── codenavigator-app/                   # 应用启动模块
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/codenavigator/
│       │   │   └── CodeNavigatorApplication.java
│       │   └── resources/
│       │       ├── application.yml      # 应用配置
│       │       ├── application-dev.yml  # 开发环境配置
│       │       ├── application-prod.yml # 生产环境配置
│       │       ├── static/              # 静态资源
│       │       │   ├── css/
│       │       │   ├── js/
│       │       │   └── images/
│       │       └── templates/           # Thymeleaf模板
│       │           ├── layout/
│       │           │   └── base.html
│       │           ├── conversation/
│       │           │   └── chat.html
│       │           └── learning/
│       │               ├── path.html
│       │               └── progress.html
│       └── test/
│           └── java/com/codenavigator/
│               ├── integration/         # 集成测试
│               └── unit/                # 单元测试
│
└── docs/                                # 文档目录
    ├── api/                             # API文档
    ├── deployment/                      # 部署文档
    ├── development/                     # 开发文档
    └── user-guide/                      # 用户指南
```

## 🏗️ 核心数据模型设计

### 1. 用户模型 (User)
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String username;
    
    @Column(unique = true) 
    private String email;
    
    private String avatar;
    
    @Enumerated(EnumType.STRING)
    private UserLevel level;           // BEGINNER, INTERMEDIATE, ADVANCED
    
    @OneToMany(mappedBy = "user")
    private List<UserProgress> progressList;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### 2. 学习路径模型 (LearningPath)
```java
@Entity
@Table(name = "learning_paths")
public class LearningPath {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;               // e.g., "Kafka深入学习"
    private String framework;          // e.g., "Kafka"
    private String description;
    
    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficulty;
    
    private Integer estimatedHours;
    
    @OneToMany(mappedBy = "learningPath", cascade = CascadeType.ALL)
    @OrderBy("sequence")
    private List<LearningModule> modules;
    
    @Column(columnDefinition = "json")
    private String prerequisites;      // JSON格式的先修技能
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 3. 学习模块模型 (LearningModule)
```java
@Entity
@Table(name = "learning_modules")
public class LearningModule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;              // e.g., "实现基础EventLoop"
    
    @Column(columnDefinition = "text")
    private String description;
    
    @Column(columnDefinition = "text")
    private String requirements;       // 具体任务要求
    
    @Column(columnDefinition = "text")
    private String hints;              // 提示信息
    
    private Integer sequence;          // 模块顺序
    private Integer estimatedMinutes;
    
    @ManyToOne
    @JoinColumn(name = "learning_path_id")
    private LearningPath learningPath;
    
    @Enumerated(EnumType.STRING)
    private ModuleType type;           // CODE_IMPLEMENTATION, THEORY_STUDY, CODE_REVIEW
    
    @Column(columnDefinition = "json")
    private String successCriteria;   // JSON格式的成功标准
}
```

### 4. 用户进度模型 (UserProgress)
```java
@Entity
@Table(name = "user_progress")
public class UserProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "learning_path_id")
    private LearningPath learningPath;
    
    @ManyToOne
    @JoinColumn(name = "current_module_id")
    private LearningModule currentModule;
    
    @Enumerated(EnumType.STRING)
    private ProgressStatus status;     // NOT_STARTED, IN_PROGRESS, COMPLETED, PAUSED
    
    private Integer completedModules;
    private Integer totalModules;
    private Double completionPercentage;
    
    @Column(columnDefinition = "json")
    private String moduleProgress;     // JSON格式的详细进度
    
    private LocalDateTime startedAt;
    private LocalDateTime lastActiveAt;
    private LocalDateTime completedAt;
}
```

### 5. 学习笔记模型 (LearningNote)
```java
@Entity
@Table(name = "learning_notes") 
public class LearningNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "module_id")
    private LearningModule module;
    
    private String title;
    
    @Column(columnDefinition = "longtext")
    private String userCode;           // 用户提交的代码
    
    @Column(columnDefinition = "longtext")
    private String aiFeedback;         // AI反馈内容
    
    @Column(columnDefinition = "longtext")
    private String sourceComparison;   // 源码对照内容
    
    @Column(columnDefinition = "longtext")
    private String summary;            // 学习总结
    
    @Enumerated(EnumType.STRING)
    private NoteStatus status;         // DRAFT, PUBLISHED, ARCHIVED
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

## 📦 关键配置文件

### 1. 父POM配置 (pom.xml)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.codenavigator</groupId>
    <artifactId>codenavigator-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    
    <modules>
        <module>codenavigator-common</module>
        <module>codenavigator-core</module>
        <module>codenavigator-ai</module>
        <module>codenavigator-web</module>
        <module>codenavigator-app</module>
    </modules>
    
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <spring.boot.version>3.2.0</spring.boot.version>
        <langchain4j.version>0.29.1</langchain4j.version>
    </properties>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring.boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### 2. 应用配置 (application.yml)
```yaml
spring:
  profiles:
    active: dev
  
  application:
    name: codenavigator
  
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/code_navigator?useUnicode=true&characterEncoding=utf-8&useSSL=false
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:password}
    
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    database-platform: org.hibernate.dialect.MySQL8Dialect
    
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
      
  thymeleaf:
    cache: false
    encoding: UTF-8
    
server:
  port: 8080
  servlet:
    context-path: /api/v1

# LangChain4j配置
langchain4j:
  open-ai:
    api-key: ${OPENAI_API_KEY}
    model: gpt-4
    temperature: 0.7
    max-tokens: 2000
    
# 应用业务配置
codenavigator:
  ai:
    conversation-timeout: 30000    # 30秒超时
    max-conversation-turns: 20
  learning:
    max-concurrent-paths: 5        # 用户最多同时学习5个路径
    session-timeout: 3600          # 1小时会话超时
  file:
    upload-path: ./uploads
    max-file-size: 10MB

logging:
  level:
    com.codenavigator: DEBUG
    org.springframework.web: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 3. Docker Compose配置 (docker-compose.yml)
```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: code_navigator
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./docs/sql:/docker-entrypoint-initdb.d
    
  redis:
    image: redis:7.0
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
      
  app:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
    environment:
      - DB_USERNAME=root
      - DB_PASSWORD=password
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    volumes:
      - ./logs:/app/logs

volumes:
  mysql_data:
  redis_data:
```

## 🚀 快速启动脚本

### 1. 项目初始化脚本 (init-project.sh)
```bash
#!/bin/bash

echo "🚀 初始化CodeNavigator项目..."

# 创建项目目录结构
echo "📁 创建目录结构..."
mkdir -p {codenavigator-common,codenavigator-core,codenavigator-ai,codenavigator-web,codenavigator-app}/{src/main/java/com/codenavigator,src/test/java}
mkdir -p codenavigator-app/src/main/resources/{static/{css,js,images},templates}
mkdir -p docs/{api,deployment,development,user-guide}

# 生成Maven模块POM文件
echo "📦 生成Maven配置..."
# (生成各模块的pom.xml文件脚本)

# 初始化Git仓库和配置
echo "🔧 配置Git仓库..."
git init
git add .
git commit -m "feat: 初始化项目结构"

echo "✅ 项目初始化完成！"
echo "📖 请阅读 DEVELOPMENT_PLAN.md 开始开发"
```

### 2. 开发环境启动脚本 (start-dev.sh)
```bash
#!/bin/bash

echo "🛠️ 启动开发环境..."

# 启动数据库和Redis
docker-compose up -d mysql redis

# 等待服务启动
echo "⏳ 等待数据库启动..."
sleep 10

# 执行数据库初始化
echo "🗄️ 初始化数据库..."
mysql -h localhost -u root -ppassword code_navigator < docs/sql/init.sql

# 启动应用
echo "🚀 启动应用..."
mvn spring-boot:run -pl codenavigator-app

echo "✅ 开发环境启动完成！"
echo "🌐 访问: http://localhost:8080"
```

这个项目结构设计具有以下优势：

1. **模块化清晰**: 按功能职责分离，便于团队协作开发
2. **可扩展性强**: 易于添加新功能模块或替换实现
3. **配置灵活**: 支持多环境配置和容器化部署
4. **测试友好**: 单元测试和集成测试分离
5. **文档完善**: 包含完整的开发和部署文档

<function_calls>
<invoke name="TodoWrite">
<parameter name="todos">[{"content": "\u5b8c\u5584README\u6587\u4ef6\u5185\u5bb9", "status": "completed", "activeForm": "\u5b8c\u5584README\u6587\u4ef6\u5185\u5bb9"}, {"content": "\u5236\u5b9a\u8be6\u7ec6\u7684\u5f00\u53d1\u8ba1\u5212", "status": "completed", "activeForm": "\u5236\u5b9a\u8be6\u7ec6\u7684\u5f00\u53d1\u8ba1\u5212"}, {"content": "\u521b\u5efa\u9879\u76ee\u7ed3\u6784\u89c4\u5212", "status": "completed", "activeForm": "\u521b\u5efa\u9879\u76ee\u7ed3\u6784\u89c4\u5212"}]