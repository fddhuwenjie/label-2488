# 校园图书借阅管理系统

> Campus Library Book Borrowing Management System

---

## How to Run

### ⚡️ 极速体验 (Shell Script)

提供一键演示脚本 `demo.sh`，自动完成环境检测、Docker 启动、API 冒烟测试。

**适用平台**：macOS / Linux / Windows (WSL/Git Bash)

```bash
# 赋予执行权限并运行
chmod +x demo.sh
./demo.sh
```

脚本覆盖认证、用户、图书、借阅四个模块共 **22 个测试用例**，完整运行输出已记录于 [`demo-output.txt`](demo-output.txt)（22/22 全部通过）。

---

### Docker 启动（推荐，无需本地安装 JDK 和 MySQL）

**前提条件**：已安装 [Docker Desktop](https://www.docker.com/products/docker-desktop/) 并确保 Docker 服务正在运行。

```bash
# 1. 进入项目根目录
cd p-2488

# 2. 一键构建镜像并启动所有服务（MySQL + Spring Boot）
#    无需额外配置，内置默认开发密钥和 CORS 设置，开箱即用
docker-compose up --build -d

# 3. 查看启动日志（等待 "Started LibraryApplication" 出现）
docker-compose logs -f backend

# 4. 验证服务健康状态
curl http://localhost:8080/actuator/health
```

启动完成后访问：`http://localhost:8080`

> **说明**：首次启动时 Maven 需下载依赖（约 2–5 分钟），后续构建会使用 Docker 层缓存加速。  
> 系统启动后会自动初始化数据库表结构和测试账号数据（仅首次执行）。

**自定义配置（可选）：**

如需覆盖默认的 JWT 密钥或 CORS 来源，可创建 `.env` 文件：

```bash
cp .env.example .env
# 编辑 .env，按需修改以下变量：
#   JWT_SECRET           — JWT 签名密钥，生成示例：openssl rand -base64 32
#   CORS_ALLOWED_ORIGINS — 前端部署域名，如 https://your-frontend.com
```

> 未创建 `.env` 时，系统使用内置默认值（仅适用于本地开发/演示）：
>
> | 变量 | 默认值 | 说明 |
> |------|--------|------|
> | `JWT_SECRET` | 内置开发密钥 | 生产环境务必替换为强随机密钥 |
> | `JWT_EXPIRATION` | `28800000`（8 小时） | JWT 令牌有效期（毫秒） |
> | `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173,http://localhost:8080` | 允许的前端来源 |

**停止服务：**
```bash
docker-compose down          # 停止容器（保留 MySQL 数据）
docker-compose down -v       # 停止容器并删除数据卷（清空所有数据）
```

---

### 本地启动（需要 JDK 17+ 和 Maven 3.6+）

本地启动使用 **H2 内存数据库**，无需安装 MySQL。

```bash
# 进入后端项目目录
cd p-2488/Backend

# 方式一：Maven 直接运行
mvn spring-boot:run

# 方式二：先打包再运行
mvn clean package -DskipTests
java -jar target/campus-library-1.0.0.jar
```

本地启动后可访问：
- **API 地址**：`http://localhost:8080`
- **H2 数据库控制台**：`http://localhost:8080/h2-console`
  - JDBC URL：`jdbc:h2:mem:librarydb`
  - 用户名：`sa`，密码：（空）

---

## Services

| 服务 | 容器名 | 端口 | 描述 |
|------|--------|------|------|
| Spring Boot 后端 | `library-backend` | `8080` | RESTful API 服务（主要服务） |
| MySQL 数据库 | `library-mysql` | `3488`（宿主机）→ `3306`（容器内） | 业务数据持久化存储 |

### API 接口总览

#### 认证模块 `/api/auth`

| 方法 | 路径 | 说明 | 认证要求 |
|------|------|------|----------|
| POST | `/api/auth/login` | 用户登录，返回 JWT 令牌 | 无需认证 |
| POST | `/api/auth/register` | 注册普通用户账号 | 无需认证 |

#### 用户管理 `/api/users`

| 方法 | 路径 | 说明 | 认证要求 |
|------|------|------|----------|
| GET | `/api/users/me` | 获取当前登录用户信息 | 已登录 |
| GET | `/api/users` | 分页查询用户列表 | 管理员 |
| GET | `/api/users/{id}` | 根据 ID 查询用户 | 管理员 / 本人 |
| PUT | `/api/users/{id}` | 更新用户信息 | 管理员 / 本人 |
| DELETE | `/api/users/{id}` | 删除用户 | 管理员 |

#### 图书管理 `/api/books`

| 方法 | 路径 | 说明 | 认证要求 |
|------|------|------|----------|
| GET | `/api/books` | 分页查询图书（支持关键字/分类过滤） | 已登录 |
| GET | `/api/books/{id}` | 获取图书详情 | 已登录 |
| POST | `/api/books` | 新增图书 | 管理员 |
| PUT | `/api/books/{id}` | 更新图书信息 | 管理员 |
| DELETE | `/api/books/{id}` | 下架图书（逻辑删除） | 管理员 |

#### 借阅管理 `/api/borrows`

| 方法 | 路径 | 说明 | 认证要求 |
|------|------|------|----------|
| POST | `/api/borrows` | 借阅图书 | 已登录 |
| PUT | `/api/borrows/{id}/return` | 归还图书 | 已登录（本人） |
| GET | `/api/borrows` | 查询借阅记录（管理员查全部，用户查自己） | 已登录 |
| GET | `/api/borrows/my` | 查询当前用户的借阅记录 | 已登录 |
| GET | `/api/borrows/{id}` | 查询借阅记录详情（含延迟加载的用户/图书信息） | 已登录 |
| GET | `/api/borrows/overdue` | 查询所有逾期未还记录 | 管理员 |

---

## 测试账号

| 账号 | 密码 | 角色 | 权限说明 |
|------|------|------|----------|
| `admin` | `admin123` | `ROLE_ADMIN`（管理员） | 拥有所有权限：图书增删改、用户管理、查看所有借阅记录 |
| `user1` | `user123` | `ROLE_USER`（普通用户） | 可借还图书、查询图书列表、管理自己的信息 |
| `user2` | `user123` | `ROLE_USER`（普通用户） | 同上 |

**快速验证（curl 示例）：**

```bash
chmod +x quick-test.sh
./quick-test.sh
```

---

## 题目内容

一、考核目标  
全面检验学生对 Java EE 轻量级框架核心原理、技术栈整合应用能力，以及框架设计思维与工程实践能力；同时结合课程思政要求，引导学生理解技术自主创新的重要性，考核学生分析问题、解决问题、系统设计与文档撰写的综合素养。

二、考核选题方向（二选一）  
方向 1：基于轻量级框架开发中小型系统（工程实践类）  
聚焦 Spring Boot、Spring MVC、MyBatis、Spring Security 等核心框架的整合应用，完成具备完整业务流程、安全控制、数据访问能力的中小型系统开发，体现对框架核心特性的掌握与工程化应用能力。

三、考核具体要求  
（一）通用要求  
技术规范性：代码遵循 Java 编码规范（如驼峰命名、注释覆盖率≥30%），框架使用 / 设计符合轻量级核心特征（低耦合、模块化、轻配置），无冗余代码或严重性能问题。

（二）方向 1：基于轻量级框架开发中小型系统详细要求  
1. 系统规模与业务范围  
需覆盖至少 3 个核心业务模块（如用户管理、数据操作、权限控制）  
校园图书借阅管理系统

2. 技术栈要求（必须包含）  
核心框架：Spring Boot（≥2.7.x）+ Spring MVC + MyBatis；  
安全控制：集成 Spring Security 实现认证 / 授权（如角色区分：普通用户 / 管理员）；  
数据层：MyBatis 实现 CRUD、延迟加载、缓存优化；  
接口规范：RESTful API 设计，支持跨域访问、请求参数合法性校验；  
构建工具：Maven（需体现生命周期、插件配置）。

---

## 项目介绍

基于 **Spring Boot 3.2 + Spring MVC + MyBatis + Spring Security + JWT** 构建的校园图书借阅管理系统。  
涵盖用户管理、图书管理、借阅管理三大核心业务模块，提供完整的 RESTful API 和角色权限控制。

### 技术架构

```
┌─────────────────────────────────────────────────┐
│              表现层（Controller）                 │
│  AuthController / UserController /               │
│  BookController / BorrowController               │
│  Spring MVC RESTful + Bean Validation 参数校验    │
├─────────────────────────────────────────────────┤
│              安全层（Security）                   │
│  Spring Security + JWT 无状态认证                 │
│  JwtAuthenticationFilter / SecurityConfig        │
│  角色授权：ROLE_ADMIN / ROLE_USER                 │
├─────────────────────────────────────────────────┤
│              业务层（Service）                    │
│  UserService / BookService / BorrowService       │
│  Spring 声明式事务（@Transactional）              │
├─────────────────────────────────────────────────┤
│              数据层（Mapper）                     │
│  MyBatis：CRUD + 动态 SQL + 延迟加载 + 二级缓存   │
│  UserMapper / BookMapper / BorrowMapper          │
├─────────────────────────────────────────────────┤
│              数据库层                             │
│  MySQL 8.0（Docker）/ H2（本地开发）              │
└─────────────────────────────────────────────────┘
```

### 项目结构

```
p-2488/
├── Backend/                              # Spring Boot 后端主项目
│   ├── src/main/java/com/library/
│   │   ├── LibraryApplication.java       # 主启动类（@MapperScan）
│   │   ├── DataInitializer.java          # 启动时初始化测试数据
│   │   ├── config/
│   │   │   ├── SecurityConfig.java       # Spring Security + CORS 配置
│   │   │   └── MybatisConfig.java        # MyBatis 延迟加载/缓存配置
│   │   ├── controller/
│   │   │   ├── AuthController.java       # 登录/注册接口
│   │   │   ├── UserController.java       # 用户管理接口
│   │   │   ├── BookController.java       # 图书管理接口
│   │   │   └── BorrowController.java     # 借阅管理接口
│   │   ├── service/
│   │   │   ├── UserService.java          # 用户服务接口
│   │   │   ├── BookService.java          # 图书服务接口
│   │   │   ├── BorrowService.java        # 借阅服务接口
│   │   │   └── impl/                     # 服务实现类
│   │   ├── mapper/
│   │   │   ├── UserMapper.java           # 用户 Mapper 接口
│   │   │   ├── BookMapper.java           # 图书 Mapper 接口
│   │   │   └── BorrowMapper.java         # 借阅 Mapper 接口
│   │   ├── entity/                       # 实体类（User/Book/BorrowRecord）
│   │   ├── dto/                          # 数据传输对象（含参数校验）
│   │   ├── security/                     # JWT 认证组件
│   │   └── common/                       # 统一响应/分页/全局异常处理
│   ├── src/main/resources/
│   │   ├── mapper/                       # MyBatis XML（含延迟加载/缓存配置）
│   │   │   ├── UserMapper.xml
│   │   │   ├── BookMapper.xml
│   │   │   └── BorrowMapper.xml
│   │   ├── db/mysql-init.sql             # MySQL 建表脚本
│   │   ├── schema.sql                    # H2 建表脚本
│   │   ├── application.yml               # 默认配置（H2 本地开发）
│   │   └── application-docker.yml        # Docker 配置（MySQL）
│   ├── Dockerfile                        # 多阶段构建，支持 ARM64/x86
│   └── pom.xml                           # Maven 构建配置（含插件）
├── docker-compose.yml                    # 容器编排（MySQL + Spring Boot）
├── .gitignore
└── README.md
```

### 关键技术实现说明

**MyBatis 延迟加载**  
`BorrowMapper.xml` 中的 `BorrowResultMap` 通过 `<association fetchType="lazy">` 配置延迟加载：  
访问 `BorrowRecord.getUser()` 时才执行 `UserMapper.findById`，访问 `getBook()` 时才执行 `BookMapper.findById`。  
列表接口使用 `SimpleResultMap`（不含关联），避免 N+1 查询问题。

**MyBatis 二级缓存**  
三个 Mapper XML 均配置了 `<cache>` 标签启用 Mapper 级别二级缓存，减少重复查询的数据库压力。  
`UserMapper`（60秒刷新）、`BookMapper`（5分钟刷新）、`BorrowMapper`（30秒刷新，保证实时性）。

**Spring Security + JWT**  
无状态认证：`JwtAuthenticationFilter` 在每次请求时从 `Authorization: Bearer <token>` 中提取 JWT，  
验证签名和有效期后，从 DB 加载用户信息并注入 `SecurityContextHolder`。  
未认证请求统一返回 `HTTP 401 Unauthorized`（通过自定义 `AuthenticationEntryPoint` 实现）。

**跨域配置（CORS）**  
`SecurityConfig.corsConfigurationSource()` 从配置属性 `cors.allowed-origins` 读取前端来源白名单，  
支持 `Authorization` 请求头传递 JWT。  
- **本地开发**：`application.yml` 默认允许 `localhost:3000/5173/8080`  
- **Docker 生产**：通过 `.env` 文件设置 `CORS_ALLOWED_ORIGINS` 为实际前端域名，不允许通配符

**JWT 密钥管理**  
JWT 签名密钥通过环境变量 `JWT_SECRET` 注入，不在代码仓库中存储任何密钥明文：  
- **本地开发**：`application.yml` 提供仅用于本地的默认值  
- **Docker 环境**：`docker-compose.yml` 和 `application-docker.yml` 均内置开发默认密钥，无需配置即可启动；生产环境通过 `.env` 文件覆盖为强随机密钥

---

## 注释覆盖率说明

### 统计口径

本项目采用**按源代码行数**统计注释覆盖率，统计范围为 `Backend/src/main/java/` 下全部 `.java` 文件，计入以下三类注释行：

| 类型 | 示例 | 说明 |
|------|------|------|
| 单行注释 | `// 说明文字` | 行内说明、逻辑注释 |
| 块注释起止行 | `/* ... */` | 多行块注释的开闭行 |
| Javadoc 行 | `/** ... */`、`* @param` | 类/方法级 Javadoc |

**不计入**：纯注解行（`@Override`、`@Autowired` 等）、空行、代码行。

### 当前度量结果

| 指标 | 数值 |
|------|------|
| 统计文件数 | 32 个 `.java` 文件 |
| 源码总行数 | 2617 行 |
| 注释行数 | 1074 行 |
| **注释覆盖率** | **41.0%**（≥ 30% 阈值） |

### 本地复现方式

在项目根目录执行以下脚本即可复现上述统计：

```bash
python3 - <<'EOF'
import os

total, comments = 0, 0
src = "Backend/src/main/java"
for root, _, files in os.walk(src):
    for f in files:
        if not f.endswith(".java"):
            continue
        with open(os.path.join(root, f), encoding="utf-8") as fp:
            for line in fp:
                total += 1
                s = line.strip()
                if s.startswith("//") or s.startswith("/*") or s.startswith("*"):
                    comments += 1
print(f"总行数: {total}")
print(f"注释行数: {comments}")
print(f"注释覆盖率: {comments/total*100:.1f}%")
EOF
```

### CI 静态检查

`pom.xml` 已集成 **Checkstyle** 插件（绑定 `verify` 阶段），执行以下命令可生成 HTML 报告：

```bash
cd Backend
mvn checkstyle:checkstyle
# 报告输出至 target/site/checkstyle.html
```
