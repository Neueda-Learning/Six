<!-- 该文件用于说明项目整体结构与开发规范，后续需持续维护目录说明、启动方式与协作约定。 -->

# 先安装element plus依赖
npm install element-plus @element-plus/icons-vue
# Payments Processing System

## 项目目录结构

```text
Six/
├─ backend/                              # Spring Boot 后端工程
│  ├─ pom.xml                            # Maven 依赖与构建配置
│  └─ src/
│     ├─ main/
│     │  ├─ java/com/example/payments/
│     │  │  ├─ controller/               # REST 接口层
│     │  │  ├─ dto/request/              # 请求 DTO
│     │  │  ├─ dto/response/             # 响应 DTO 与统一响应模型
│     │  │  ├─ entity/                   # 数据库实体模型
│     │  │  ├─ enums/                    # 状态与错误码枚举
│     │  │  ├─ exception/                # 业务异常与全局异常处理
│     │  │  ├─ mapper/                   # MyBatis-Plus Mapper 接口
│     │  │  ├─ service/                  # 业务服务接口
│     │  │  ├─ service/impl/             # 业务服务实现
│     │  │  ├─ statemachine/             # 支付状态机规则
│     │  │  ├─ validator/                # 支付参数校验
│     │  │  └─ PaymentsApplication.java  # 后端启动入口
│     │  └─ resources/
│     │     ├─ application.yml           # 通用环境配置
│     │     ├─ application-dev.yml       # 开发环境配置
│     │     ├─ application-prod.yml      # 生产环境配置
│     │     ├─ mapper/                   # Mapper XML 映射
│     │     └─ db/                       # 数据库 DDL 与初始化脚本
│     └─ test/                           # 后端测试代码
├─ src/                                  # Vue 前端源码
│  ├─ api/                               # Axios 封装与支付接口定义
│  ├─ router/                            # 前端路由配置
│  ├─ styles/                            # 全局样式与设计变量
│  ├─ views/                             # 页面级组件（列表/创建/详情）
│  ├─ App.vue                            # 前端根组件
│  └─ main.js                            # 前端启动入口
├─ index.html                            # Vite 挂载入口 HTML
├─ vite.config.js                        # Vite 构建与代理配置
├─ package.json                          # 前端依赖与脚本配置
├─ package-lock.json                     # 前端依赖锁定文件
├─ .env.development                      # 前端开发环境变量
├─ .env.production                       # 前端生产环境变量
├─ payment-processing-design.md          # 课程设计文档
└─ .gitignore                            # Git 忽略规则
```

## 目录职责说明

- `backend/`: 后端 Spring Boot 工程，负责支付生命周期 API、状态机、幂等、审计历史与数据库访问。
- `backend/src/main/java/.../controller`: 对外暴露 REST API，负责请求接收与响应返回。
- `backend/src/main/java/.../service`: 承载核心业务流程（幂等、校验、状态推进、历史记录）。
- `backend/src/main/java/.../mapper` 与 `backend/src/main/resources/mapper`: 数据访问接口与 SQL 映射。
- `backend/src/main/resources/db`: 建表脚本与初始化数据脚本。
- `src/views`: 前端页面层，覆盖创建支付、支付列表、支付详情与历史展示。
- `src/api`: 统一前端 API 调用入口，封装支付相关接口。
- `src/router`: 页面路由管理，定义页面跳转关系。
- `src/styles`: 全局样式基础与后续设计主题变量。

## 当前状态

- 当前代码为“可开发骨架”版本，核心文件已预置 `//todo` 占位标记。
- 后续可按课程要求逐步补齐后端业务逻辑、前端页面实现与接口联调。