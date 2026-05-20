# Gym Management System

基于 Spring Boot 3 的健身房管理系统，提供会员管理、教练管理、课程预约、设备管理、签到打卡、支付管理等功能。

## 技术栈

- **后端框架**: Spring Boot 3.2.5
- **安全框架**: Spring Security + JWT
- **持久层**: MyBatis-Plus 3.5.5
- **数据库**: SQL Server
- **API 文档**: SpringDoc OpenAPI (Swagger UI)
- **工具库**: Hutool
- **Java 版本**: 17

## 功能模块

| 模块 | 说明 |
|------|------|
| 用户认证 | 登录、JWT 鉴权、密码修改 |
| 会员管理 | 会员信息的增删改查 |
| 教练管理 | 教练信息及排班管理 |
| 课程管理 | 课程发布与预约 |
| 签到管理 | 会员签到打卡记录 |
| 设备管理 | 健身设备信息维护 |
| 支付管理 | 支付记录管理 |
| 会员卡管理 | 会员卡开通与续费 |
| 通知公告 | 系统通知推送 |
| 仪表盘 | 数据统计概览 |
| 系统日志 | 操作日志记录 |

## 项目结构

```
src/main/java/com/gym/
├── config/          # 配置类 (Security、MyBatis-Plus、全局异常处理)
├── controller/      # REST 控制器
├── dto/             # 数据传输对象
├── entity/          # 实体类
├── mapper/          # MyBatis-Plus Mapper 接口
├── security/        # JWT 工具与过滤器
├── service/         # 业务逻辑层
├── util/            # 通用工具类
└── GymApplication.java  # 启动类
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- SQL Server 数据库

### 配置数据库

1. 创建数据库，执行初始化脚本：

```bash
sqlcmd -S localhost -d master -i src/main/resources/sql/init.sql
```

2. 修改数据库连接配置 `src/main/resources/application-dev.yml`。

### 运行

```bash
# 使用 Maven 运行
./mvnw spring-boot:run

# 或打包后运行
./mvnw clean package
java -jar target/gym-management-1.0.0.jar
```

启动后访问：
- 应用首页: http://localhost:8080
- API 文档: http://localhost:8080/swagger-ui.html

## 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 教练 | coach01 / coach02 / coach03 | coach123 |
| 会员 | member01 ~ member05 | member123 |

> 需在 `application-dev.yml` 中设置 `app.demo.reset-passwords: true` 以启用默认密码初始化。
