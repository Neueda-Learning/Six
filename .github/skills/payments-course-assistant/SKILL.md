---
name: payments-course-assistant
description: "Use when: 需要为本仓库的支付课程项目生成或修改 Spring Boot + Vue 代码、接口、数据库脚本、测试用例，且必须遵循既定状态机、幂等规则与错误码约定。"
---

# Payments Course Assistant Skill

## 目标
为课程项目提供可直接落地的实现辅助，确保所有输出与仓库中的支付设计文档一致。

## 触发场景
- 生成后端基础代码：实体、DTO、Mapper、Service、Controller、异常处理。
- 生成前端页面：支付创建、列表筛选、详情与历史时间线。
- 生成数据库 DDL、索引、初始化数据。
- 编写接口联调示例、Postman 样例、基础测试用例。

## 必须遵守的业务约束
- 仅使用状态：CREATED, VALIDATED, SENT, COMPLETED, FAILED。
- 仅允许以下状态转换：
1. CREATED -> VALIDATED
2. CREATED -> FAILED
3. VALIDATED -> SENT
4. VALIDATED -> FAILED
5. SENT -> COMPLETED
6. SENT -> FAILED
- 所有状态变化必须记录审计历史。
- 创建支付必须支持 idempotencyKey，并处理重复提交。

## 输出规范
- 代码输出优先最小可运行方案，不引入与作业无关的复杂组件。
- REST 接口必须包含：方法、路径、请求参数、响应体、错误码。
- 若新增字段或接口，必须提示同步更新：
1. 数据库结构
2. OpenAPI 注解
3. 前端 API 调用

## 后端实现清单（建议顺序）
1. 定义枚举：PaymentStatus, ErrorCode。
2. 定义实体：Payment, PaymentStatusHistory。
3. 定义请求响应 DTO。
4. 定义状态机校验组件。
5. 实现 PaymentService（幂等、校验、状态推进、历史记录）。
6. 实现 Controller 与全局异常处理。
7. 补充 OpenAPI 注解。

## 前端实现清单（建议顺序）
1. API 模块：payment.js（5 个核心接口）。
2. 页面：PaymentCreate, PaymentList, PaymentDetail。
3. 列表筛选与分页。
4. 详情页历史时间线与失败错误展示。
5. 统一错误提示与状态颜色映射。

## 验收检查
- 是否只实现 5 项课程功能，无额外范围膨胀。
- 是否保留完整 audit trail。
- 是否覆盖关键异常：非法流转、校验失败、支付不存在、网络模拟失败。
- 是否能通过 Happy Path：CREATED -> VALIDATED -> SENT -> COMPLETED。