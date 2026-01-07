# DDD 重构计划书（面向“可低成本拆微服务”的模块化单体）

> 适用范围：本仓库 `eva-backend`（Spring Boot + Maven 多模块）。  
> 构建基线：**Java 17**（避免高版本 JDK 下的编译器/注解处理器兼容性问题）。  
> 离线验证示例：`mvn -o -pl eva-infra -am test -q -Dmaven.repo.local=.m2/repository`（网络受限场景优先）。  
> 目标：把当前“按技术分层”的结构，重构为“按业务域分模块”的 **DDD 模块化单体**，并在架构与边界上为未来拆分微服务预留低成本路径。

---

## 0. 背景与总体目标

### 0.1 当前事实（来自现有文档与代码）

- 业务核心：教师之间的听课/选课、评教任务、评教记录、统计导出、消息提醒、AI 评估报告。
- 数据核心表（`data/doc/*`）：
  - IAM：`sys_user`、`sys_role`、`sys_menu`、`sys_user_role`、`sys_role_menu`、LDAP 同步相关
  - 课表/课程：`semester`、`subject`、`course`、`cour_inf`、`course_type`、`course_type_course`
  - 评教：`eva_task`、`form_record`、`form_template`、`cour_one_eva_template`
  - 消息：`msg_tip`
  - 审计：`sys_log`、`sys_log_module`
- 关键业务规则（你已确认/文档明确）：
  - `week` 表示学期内单周周次（从开学日期 `start_date` 起算第 n 周）。
  - 同一课次允许多人评教（必须如此）。
  - 课程模板：只要有人评教过就锁定（不能再切换）。
  - “高分次数”阈值应可配置。
  - AI 报告维度：教师 + 学期（汇总全部课程，报告中可按课程分节）。
- 代码现状：已有 `eva-adapter / eva-app / eva-domain / eva-infra` 等技术分层模块；但“领域层”被 DTO/CO/Cmd 污染，跨域耦合明显；且 `eva-client` 承载了大量 BO/CO/DTO（边界协议对象），导致“业务归属不清 + 复用边界失控”。

### 0.2 重构目标（可验收）

1. **按业务域拆模块**（Bounded Context 为单位），形成可独立演进的模块化单体。
2. 落地 **六边形架构（Ports & Adapters）**：领域模型不依赖 Web/DB/LDAP/LLM。
3. 落地 **CQRS**：写模型（聚合）与读模型（投影/查询）解耦，复杂统计/看板可独立优化。
4. 通过 **领域事件** 串联跨域联动（消息、日志、统计、AI 报告），为未来“事件驱动微服务”铺路。
5. 以“最小切片”渐进重构：不推倒重来，不大改接口；每阶段可交付、可回滚。

---

## 1. 领域划分（Subdomain）

> 原则：围绕业务能力而非数据库表；以“变化原因一致”为标准聚合到同一子域。

| 子域 | 类型 | 核心能力 | 主要数据/接口线索 |
|---|---|---|---|
| 评教（Evaluation） | 核心域 | 评教任务/记录、达标统计、导出 | `eva_task`、`form_record`、导出接口、看板接口 |
| 课表与课程（Course Scheduling） | 支撑域 | 学期、课程、课次、导入课表、改课联动 | `semester/course/cour_inf`、课程操作说明、导入课表接口 |
| 表单模板（Form Template） | 支撑域 | 模板管理、课程模板锁定、快照统计 | `form_template`、`cour_one_eva_template`、模板切换说明 |
| 消息通知（Messaging） | 通用域 | 消息入库、WS 转发、逾期提醒 | `msg_tip`、消息业务说明 |
| 身份与权限（IAM） | 通用域 | 登录、用户/角色/菜单、LDAP 同步 | `sys_*`、LDAP 文档、鉴权注解 |
| 审计日志（Audit） | 通用域 | 操作日志与模块字典 | `sys_log/sys_log_module` |
| AI 报告（AI Report） | 支撑域 | 教师学期报告生成与导出 | AI 报告样例、LangChain4j 相关实现 |

---

## 2. 限界上下文（Bounded Context）划分与上下文地图（Context Map）

### 2.1 Bounded Context 列表（建议）

1. **BC-IAM**：认证、授权、LDAP 同步与本地用户信息。
2. **BC-Course**：学期/课程/课次/课程类型/课表导入与改课业务规则。
3. **BC-Template**：评教模板、课程模板快照、锁定策略。
4. **BC-Evaluation**（核心）：评教任务、评教记录、统计、导出。
5. **BC-Messaging**：消息生命周期（保存、推送、已读、展示/删除）、逾期提醒生成规则。
6. **BC-Audit**：操作日志记录与查询。
7. **BC-AIReport**：教师学期 AI 报告（聚合统计 + LLM 文案生成 + Word 导出）。

### 2.2 上下游关系（建议）

- BC-IAM（上游）→ 全部上下文：提供 `UserId/Role/Permission`。
- BC-Course（上游）→ BC-Evaluation：提供课次时间、冲突校验所需课表数据。
- BC-Template（上游）→ BC-Evaluation：提供任务关联的模板快照/模板解析；提供“锁定”判定依据。
- BC-Evaluation（发布事件）→ BC-Messaging/BC-Audit/BC-AIReport：任务分配/取消/提交评教触发联动。

### 2.3 集成策略（从单体到微服务的可迁移方案）

在模块化单体阶段：
- 同进程调用：**只允许应用层接口调用**（Application Service），不允许跨 BC 直接访问对方 DAL/Mapper。
- 跨 BC 数据：通过 **ACL（Anti-Corruption Layer）** 做模型转换。
- 跨 BC 联动：优先使用 **领域事件（本地事件总线）**，避免强耦合调用链。

在未来微服务阶段：
- 同样的“应用层接口”可自然替换为 HTTP/gRPC 客户端或消息订阅者实现（保持端口不变）。
- 事件从 Spring 本地事件切换到 MQ（Kafka/RabbitMQ）+ Outbox。

---

## 3. 目标架构：按业务模块的六边形架构（可拆分微服务）

### 3.1 目标 Maven 模块结构（建议新结构）

> 核心原则（需求变更，2025-12-24）：在 **root 聚合**里，**每个 BC 只占用一个顶层聚合模块**（例如 `bc-iam/`）。  
> 该 BC 顶层模块自身通常是 `pom` packaging，用来聚合其内部子模块；BC 内部再按职责拆为 `domain/application/infrastructure`（必要时再细分 adapter-in/out，但过渡期可先并入 `infrastructure`）。  
> 这样做的目的：root `pom.xml` 的 `<modules>` 保持“一个 BC 一条”，避免历史平铺过渡模块（如 `bc-*-infra`）继续膨胀，后续“折叠归位/拆微服务”成本更可控。

建议新增/调整为：

- `start`：组合根（Spring Boot 启动、配置装配、跨模块组装）
- `bc-iam/`（顶层：聚合模块）
  - `bc-iam/domain`（artifactId 示例：`bc-iam-domain`）：领域模型 + 端口（Ports）
  - `bc-iam/application`（artifactId 示例：`bc-iam-application`）：用例编排（Commands/Queries）+ 边界协议对象（`contract/dto`）
  - `bc-iam/infrastructure`（artifactId 示例：`bc-iam-infrastructure`）：出站适配器（DB/Mapper/LDAP/缓存/日志等），实现 Ports
  - （可选）`bc-iam/adapter`：如确需把入站 Controller 从 `eva-adapter` 折叠归位，可再拆 `adapter-in` 子模块；过渡期可先不做
- `bc-course/`、`bc-template/`、`bc-evaluation/`、`bc-messaging/`、`bc-audit/`、`bc-ai-report/`（结构同上）
- `shared-kernel`（严格受控）：仅放“稳定且无业务语义”的通用类型（如 `PageRequest/PageResult`、`DomainEvent` 基类、`Clock`、`Result`）。

> 说明：你当前的 `eva-adapter/eva-app/eva-domain/eva-infra` 属于技术切片，下一步会逐步把代码搬运并收敛到以上 BC 结构。

### 3.2 六边形架构的层级职能（每个 BC 内一致）

**Domain（领域层）**
- 聚合/实体/值对象/领域服务/领域事件（只表达业务规则）
- 定义端口（Ports）：Repository、外部服务接口、时间/随机等可替换依赖
- 不依赖 Spring、不依赖 MyBatis、不依赖 Web DTO

**Application（应用层）**
- 用例编排：事务边界、权限/幂等等应用级规则
- CQRS：命令处理器（Command Handler）与查询服务（Query Service）
- 发布领域事件（建议 after-commit 发布）
- 不做复杂 SQL（查询可委托 QueryRepo，但要保持“读写分离”原则）

**Adapters-In（入站适配器）**
- REST Controller / WebSocket endpoint
- DTO 解析、参数校验、鉴权注解、返回模型组装
- 将 DTO 转为 Command/Query 对象调用 Application

**Adapters-Out / Infrastructure（出站适配器）**
- DB/Mapper/DO、LDAP、LLM、文件系统、消息推送、第三方接口
- 实现 Domain 端口（Repository、ExternalService）

---

## 4. 领域模型设计（实体/聚合/值对象）——按 BC 给出落地方案

> 注意：这里给的是“领域模型（业务语义）”，不等同于“数据库表一一对应的贫血实体”。DB 表仅作为参考映射。

### 4.1 BC-Course（课表与课程）

**核心概念**
- 学期（Semester）：开学日期是“周次计算”的根。
- 科目（Subject）：课程名称与性质（理论/实验/其他）。
- 课程（Course）：某学期某老师教授某科目。
- 课次（CourseSession/ClassMeeting）：课程在某周某天某节次、地点上课。

**聚合建议**

1) `Semester` 聚合
- 实体：`Semester`
- 值对象：`SemesterId`、`AcademicYear`（如 2024-2025）、`StartDate`
- 领域服务：`AcademicCalendarService`
  - `LocalDate resolveDate(Semester, WeekNo, DayOfWeek)`（统一时间语义）

2) `Course` 聚合
- 实体：`Course`
- 值对象：`CourseId`、`TeacherId`、`SubjectId`、`CourseNature`
- 子实体/集合：`CourseSession`（可作为 `Course` 的内部集合，或单独聚合作为取舍）
- 关键不变量：
  - 同一学期同一老师同一科目是否允许多条？（需结合现状确认；若允许则引入 `CourseCode/Section` 之类区分）

3) `CourseType`（课程类型）聚合
- 实体：`CourseType`
- 关联：`CourseTypeAssignment`（类型与课程的绑定）

**关键业务规则落地（来自“课程操作说明”）**
- “改课程时段”触发：
  1) 给所有人发通知消息
  2) 撤回该课程所有未完成评教任务，并通知任务发起者
  3) 删除并重建该课程所有课次
- “删课”触发：
  1) 全体通知
  2) 删除/撤回相关评教任务并通知
  3) 删除评教记录、删除课次、逻辑删除课程

> 落地方式：这些是跨 BC 行为（消息/评教/日志），建议由 **BC-Course 的应用层用例**发出领域事件，让订阅者去执行联动。

---

### 4.2 BC-Template（评教表单模板与快照）

**聚合建议**

1) `FormTemplate` 聚合（对应 `form_template`）
- 实体：`FormTemplate`
- 值对象：`TemplateId`、`TemplateName`、`TemplateProps`（指标集合，含去重规则）、`TemplateScope`（理论/实验/非默认）
- 关键不变量：
  - 同一模板内指标名不可重复（重复时按规则规范化：如自动加序号，或拒绝并提示）

2) `CourseTemplateSnapshot` 聚合（对应 `cour_one_eva_template`）
- 实体：`CourseTemplateSnapshot`
- 值对象：`SemesterId`、`CourseId`、`SnapshotTemplate`（冻结后的模板 JSON）、`CourseStatistics`（结构化统计）
- 关键不变量（你确认）：**只要有人评教过就锁定**
  - 落地判定：当某课程本学期首次产生 `EvaluationRecord` 时，创建/更新快照并标记“锁定”。
  - “锁定”状态可以显式落库（推荐），或由“快照是否存在”推导（你现在的表结构更偏后者）。

---

### 4.3 BC-Evaluation（评教：核心域）

**聚合建议**

1) `EvaluationTask` 聚合（对应 `eva_task`）
- 实体：`EvaluationTask`
- 关键字段：
  - `TaskId`
  - `EvaluatorId`（评教老师）
  - `CourseSessionId`（被评课次 `cour_inf`）
  - `Status`（待执行/已执行/已撤回）
- 值对象：
  - `TaskStatus`
  - `TimeSlot`（来自课次，用于冲突校验；通过端口查询）
- 关键不变量：
  - 同一 `EvaluatorId + CourseSessionId` 不应重复生成多条“进行中的任务”（应用层需幂等）
  - 任务“已执行”后不可取消（除非管理员有特殊能力，需定义业务规则）

2) `EvaluationRecord` 聚合（对应 `form_record`）
- 实体：`EvaluationRecord`
- 关联：`TaskId`
- 值对象：
  - `CriterionScore`（指标名 + 分数 1~100）
  - `TextComment`（详细评价）
  - `Topic`（课程主题）
- 关键不变量：
  - 一个任务最多产生一条有效记录（否则要定义“多次提交/覆盖”规则）
  - 分数范围、指标集合应与该任务关联模板一致

3) 领域服务 / 策略
- `TaskAssignmentPolicy`
  - 冲突校验：与本人教学课/其它任务时间冲突（由 BC-Course 提供课表查询端口）
  - 配额校验：被评老师被评次数上限（来自可配置 `EvaConfig`）
- `ScorePolicy`
  - 高分阈值 `highScoreThreshold`（可配置，默认 90）
  - 统计口径（课程平均分、教师学期汇总等）

---

### 4.4 BC-Messaging（消息）

**聚合建议**

1) `Message` 聚合（对应 `msg_tip`）
- 实体：`Message`
- 值对象：
  - `MessageType`（待办/通知/提醒/警告）
  - `MessageMode`（普通/评教）
  - `Recipient`（单人/广播）
  - `ReadStatus`（已读/未读、是否已展示）
- 关键规则（来自消息业务说明）：
  - 自动逾期消息：`senderId` 为 `-1/null`，同一 `taskId` 最多一条；在“连接 WS”和“拉取消息”时机创建或更新。

> 建议：把“逾期消息生成”作为 `OverdueReminderService`（应用层定时/触发器），并通过端口获取任务上课日期（由 BC-Course 计算）与任务状态（BC-Evaluation）。

---

### 4.5 BC-IAM（身份与权限）

**聚合建议**

1) `User` 聚合（对应 `sys_user`）
- 实体：`User`
- 值对象：`UserId`、`Username`、`Phone`、`Email`、`Department`、`ProfTitle`

2) `Role` 聚合、`Permission/Menu` 聚合（对应 `sys_role/sys_menu`）
- 关联：`UserRoleAssignment`、`RoleMenuAssignment`

3) `LdapSync` 作为领域服务（或应用服务）
- 外部系统通过端口 `LdapClientPort` 访问，避免 IAM 领域被 LDAP 细节污染。

---

### 4.6 BC-Audit（审计日志）

**聚合建议**

1) `AuditLog`（对应 `sys_log`）
- 值对象：`ModuleId`、`OperationType`（查增改删/其他）、`OperatorId`、`IpAddress`

> 审计通常属于通用域，不建议在各 BC 里直接写 DB；统一由事件订阅者记录。

---

### 4.7 BC-AIReport（AI 报告）

**聚合建议**

1) `TeacherSemesterReport` 聚合（建议新增领域模型，落库可选）
- 实体：`TeacherSemesterReport`
- 值对象：
  - `TeacherId`、`SemesterId`
  - `TotalEvaluations`、`HighScoreCount`（阈值来自配置）
  - `OverallSummary`（AI 总体评价）
  - `CourseReports[]`：每门课程的统计与分析（优点/缺点/建议）
- 端口：
  - `ReportFactsQueryPort`：从评教域读取结构化事实（统计结果、评价文本摘要等）
  - `LlmGeneratePort`：LLM 文案生成
  - `WordExportPort`：Word 导出（POI）

> 原则：LLM 只负责“语言生成”，事实计算必须在本地完成，避免“AI 幻觉”污染统计。

---

## 5. 领域事件（Domain Events）设计与落地

### 5.1 事件命名与负载原则

- 命名：`<BC>.<Aggregate>.<VerbPastTense>`，如 `Evaluation.TaskAssigned`
- 负载：只携带“业务标识与必要快照”，避免塞入大对象/DTO
- 发布时机：建议 **事务提交后（after-commit）** 发布，确保订阅者读到一致数据

### 5.2 建议事件清单

**BC-Evaluation 发布**
- `Evaluation.TaskAssigned`：新任务创建（触发待办消息）
- `Evaluation.TaskCancelled`：任务撤回（触发消息撤销/通知）
- `Evaluation.Submitted`：提交评教（触发：任务完成、消息清理、模板锁定、统计刷新、AI 报告失效/重算）
- `Evaluation.RecordDeleted`：删除评教记录（触发统计刷新）

**BC-Course 发布**
- `Course.CourseScheduleChanged`：课程时段变更（触发撤回相关未完成任务 + 全体通知）
- `Course.CourseDeleted`：课程删除（触发任务/记录/课次清理与通知）

**BC-Template 发布**
- `Template.TemplateUpdated/Deleted/Created`
- `Template.CourseTemplateLocked`：首次评教导致锁定（可由 Evaluation 提交时触发，但状态归属模板域）

**BC-Messaging 发布（可选）**
- `Messaging.MessageCreated/Read/Deleted`

### 5.3 事件订阅者（跨域联动落地）

- `MessagingSubscriber`：处理任务分配/撤回/逾期提醒/课程变更通知
- `AuditSubscriber`：记录关键操作日志
- `TemplateSubscriber`：评教提交触发快照生成与锁定
- `AiReportSubscriber`：评教提交后标记“教师学期报告需重算”（同步或异步）

---

## 6. CQRS 设计（写模型/读模型分离）

### 6.1 写模型（Commands + 聚合）

写侧目标：保证业务不变量、可读性与可测试性。

建议用例（示例）：
- `AssignEvaluationTaskCommand`（发起评教任务）
- `CancelEvaluationTaskCommand`
- `SubmitEvaluationCommand`
- `ImportScheduleCommand`（导入课表）
- `ChangeCourseScheduleCommand`（改时段）
- `UpdateEvaConfigCommand`（含高分阈值）

写侧实现落点：
- Application：`*CommandHandler`（事务、权限、幂等、调用聚合）
- Domain：聚合方法 `task.cancel()`、`task.complete()` 等，抛出业务异常
- Out：Repository 保存聚合

### 6.2 读模型（Queries + Projections）

读侧目标：为列表/看板/统计提供高性能、低耦合查询。

读模型策略分三档（按成本递进）：

1) **轻量 CQRS（推荐起步）**：读写分离代码结构，但仍直接查现有表（复杂 SQL 在 QueryRepo）
- 优点：改动小，快速见效
- 缺点：统计查询可能慢

2) **投影表/物化视图（中期）**：针对看板/统计建立 `*_projection` 表，通过事件增量更新
- 优点：统计快、逻辑集中
- 缺点：需要数据迁移与一致性设计

3) **独立读库/搜索（后期）**：拆微服务后引入 Elastic/ClickHouse 等（视需求）

建议优先落地的投影（面向现有导出/看板）：
- `teacher_semester_evaluation_summary`：教师-学期维度（评教次数、被评次数、高分次数…）
- `course_semester_score_summary`：课程-学期维度（最低/均值/最高/课程平均分）

---

## 7. “按业务分模块”的重构步骤（从单体到可拆微服务）

> 你明确指出现有分层模式错误：因此计划以“业务 BC 模块化”为主线推进，而不是继续加深技术分层。

### Phase 0：建立可回归的安全网（1~3 天）

交付物：
- 关键用例回归清单 + 表征测试（characterization tests）
- 敏感配置治理：把密钥/密码移出版本库（环境变量/外部配置），并完成密钥轮换

验收：
- 不改变外部 API 行为下，关键链路稳定可复现

### Phase 1：模块骨架落地（3~5 天）

交付物：
- 新建 `bc-*` 模块骨架（domain/application/adapters-in/out）
- `eva-bootstrap` 作为组合根（替代现在“start + 组件扫描”过度耦合）
- 建立跨 BC 调用规范：只能依赖对方 `*-application` 暴露的接口（或 `*-api`）

验收：
- 启动成功，原接口可用；新增模块不改变行为

### Phase 2：核心域先行——评教任务/提交评教（5~10 天）

交付物：
- 把“评教任务/记录”的核心规则收敛到 `bc-evaluation-domain`
- 用领域事件替代跨层硬编码：提交评教后触发消息清理、模板锁定、统计刷新
- 增加幂等与数据约束（应用层 + DB 唯一索引二选一或都做）

验收：
- 评教闭环（发起/撤回/提交/查询）一致，且逻辑更清晰可测

### Phase 3：课程变更联动（5~10 天）

交付物：
- 把“改时段/删课”按文档规则固化（并变成事件驱动联动）
- 收敛“课程时间/教室冲突”校验实现：以可复用组件/端口统一入口，避免重复 SQL 与文案漂移（行为保持不变）
- 旧 `*GatewayImpl` 逐步退化为“委托壳”，写侧业务流程仅保留在 BC-Course 的用例/端口适配器中
- QueryWrapper 片段收敛：优先提炼“时间段重叠/教室占用”等底层查询片段，减少重复与口径漂移风险（行为保持不变）
  - 实施建议：优先迁移写侧端口适配器（如改课/自助改课/分配评教），以最小改动方式替换重复 wrapper 片段，避免口径漂移
- 统一 week/day→日期时间算法在 BC-Course 内实现并提供端口

验收：
- 改课/删课对任务/消息的联动符合 `data/doc/04_关于课程业务操作的统一说明.md`

### Phase 4：模板锁定与快照（3~7 天）

交付物：
- 明确锁定判定：首次评教 -> 创建/更新 `CourseTemplateSnapshot`
- 严格禁止“已评教课程切换模板”（应用层拦截 + 前端提示）

验收：
- 锁定行为与管理端手册一致；历史数据兼容

### Phase 5：CQRS 强化（按性能需求推进）

交付物：
- 先做“轻量 CQRS”结构化（QueryRepo），再按看板与导出需要建设投影表
- 把“高分阈值”等统计口径集中到配置与策略

验收：
- 导出 Excel 与看板查询性能可控，口径一致

### Phase 6：AI 报告（教师+学期）产品化（3~10 天）

交付物：
- `TeacherSemesterReport` 生成：结构化事实（统计）→ LLM 文案 → Word 导出
- 支持缓存/异步（可选）：避免生成时间影响接口 SLA

验收：
- 报告结构与样例一致；事实一致、文案可追溯、可重复生成

---

## 8. 微服务演进策略（保证“低成本可拆”）

### 8.1 可拆分的前置条件（在单体阶段就要做到）

1) **BC 内聚**：一个 BC 的代码、配置、数据访问都在自己的模块里，不跨模块偷拿 Mapper。
2) **边界协议稳定**：跨 BC 只通过应用层接口或事件；DTO/CO 不向领域层渗透。
3) **事件语义清晰**：事件名与负载稳定，未来可搬到 MQ。
4) **数据所有权明确**：表归属某个 BC；其它 BC 通过 API/事件获取，不直接 join（单体阶段可暂时 join，但要“隔离在 QueryRepo”里，避免写侧依赖）。

### 8.2 拆分优先级（建议）

第一批最容易拆：
- `bc-messaging`（独立 WS/消息服务）
- `bc-ai-report`（异步生成，天然后台任务）

第二批：
- `bc-evaluation`（核心域，拆分收益大但要稳）
- `bc-course`（与 evaluation 关系紧，拆分需明确 API 与一致性策略）

IAM 可独立，但要考虑单点登录与权限同步成本。

---

## 9. 质量与技术债治理（针对“设计缺陷/代码缺陷很多”的现实）

### 9.1 技术债分类与处理策略

- **架构债（优先）**：跨 BC 直接访问 DAL、领域层依赖 DTO、静态工具散落、职责混乱  
  → 通过“模块边界 + 端口 + 事件”逐步收敛
- **一致性债（高风险）**：并发下重复任务、模板锁定不严、统计口径不一致  
  → 幂等 + 唯一索引 + 统一策略类
- **安全债（最高优先级）**：明文密钥、弱配置管理  
  → 立刻迁移到环境变量/密钥管理，并轮换
- **可测试性债（长期收益）**：缺少用例级测试、难以回归  
  → 表征测试 + 端口替身（Fake/InMemory）

### 9.2 验收“不可退化”指标（建议）

- 每个 Phase 至少新增/固化一组用例级自动化回归
- 关键业务规则（week 计算、模板锁定、多人评教）必须有测试覆盖
- 关键接口响应结构不变（除非明确版本升级）
- 回归测试避免依赖外部服务/本地文件，保证可重复执行

---

## 10. 本计划的下一步落地建议（已确认基础前提）

### 10.1 已确认的基础前提（历史决策，后续不再反复确认）

- 微服务演进策略：**先拆服务，暂时共享库**（过渡期）。
- 时间语义：`cour_inf.day` 中 `day=1` 表示 **周一**（周一是一周开始）。
- 新对话启动方式：优先复制 `NEXT_SESSION_HANDOFF.md` 的 **0.11 推荐版**（不固化 commitId），并按其中顺序阅读与执行。

### 10.2 下一步优先顺序（保持“写侧优先 + 行为不变”）

> 滚动口径（更新至 2026-01-05）：当前主线为 **bc-course 的 S0（旧 gateway 压扁为委托壳）**；✅ 已压扁 `CourseUpdateGatewayImpl.updateCourse`（Serena：调用点为 `UpdateCoursePortImpl.updateCourse`；旧 gateway 不再构造 Command；保持行为不变；落地：`c31df92c`）、`CourseUpdateGatewayImpl.updateCourses`（旧 gateway 不再构造 Command；保持行为不变；落地：`84dffcc2`）、`CourseUpdateGatewayImpl.importCourseFile`（Serena：调用点为 `ImportCourseFilePortImpl.importCourseFile`；旧 gateway 不再构造 Command；保持行为不变；落地：`5e93a08a`）、`CourseUpdateGatewayImpl.updateSingleCourse`（Serena：调用点为 `UpdateSingleCoursePortImpl.updateSingleCourse`；旧 gateway 不再构造 Command；保持行为不变；落地：`9eea1a54`）、`CourseUpdateGatewayImpl.updateSelfCourse`（Serena：调用点为 `UpdateSelfCoursePortImpl.updateSelfCourse`；旧 gateway 不再构造 Command；保持行为不变；落地：`c0f30c1f`）、`CourseUpdateGatewayImpl.addNotExistCoursesDetails`（Serena：调用点为 `AddNotExistCoursesDetailsPortImpl.addNotExistCoursesDetails`；旧 gateway 不再构造 Command；保持行为不变；落地：`62d48ee6`）与 `CourseUpdateGatewayImpl.addExistCoursesDetails`（Serena：调用点为 `AddExistCoursesDetailsPortImpl.addExistCoursesDetails`；旧 gateway 不再构造 Command；保持行为不变；落地：`de34a308`）。下一步建议：进入 **S0.2（收敛 `eva-domain` 对 `bc-course` 的编译期依赖面）**，保持行为不变。
> 新会话续接方式：优先复制 `NEXT_SESSION_HANDOFF.md` 的 0.11 推荐版提示词，并按 0.10 的“下一步拆分与里程碑/提交点”顺序执行，避免遗漏约束与回归命令。

- 补充进展（2026-01-05，S0.2 起步，保持行为不变）：已将学期 CO `SemesterCO` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`77126c4a`）。
- 补充进展（2026-01-05，S0.2 持续推进，保持行为不变）：已将通用学期入参 `Term` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`23bff82f`）。
- 补充进展（2026-01-06，S0.2 持续推进，保持行为不变）：已将课程查询 Query（`CourseQuery/CourseConditionalQuery/MobileCourseQuery`）从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`e479ce0e`）。
- 补充进展（2026-01-06，S0.2 持续推进，保持行为不变）：已将导入课表 BO `CourseExcelBO` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`1f47a032`）。
- 补充进展（2026-01-06，S0.2 持续推进，保持行为不变）：已将课程写侧命令对象子簇（`AlignTeacherCmd/UpdateCourseTypeCmd/UpdateCoursesCmd/UpdateCoursesToTypeCmd/UpdateSingleCourseCmd`）从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`0978b3cb`）。
- 补充进展（2026-01-06，S0.2 持续推进，保持行为不变）：已将课程 CO 子簇（`SubjectCO/SelfTeachCourseCO/SelfTeachCourseTimeCO/SelfTeachCourseTimeInfoCO`）从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`87d8c692`）。
- 补充进展（2026-01-06，S0.2 持续推进，保持行为不变）：已将课程写侧剩余命令对象（`UpdateCourseCmd/AddCoursesAndCourInfoCmd/UpdateCourseInfoAndTimeCmd`）从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`0d18e4ad`），从而完成 `edu.cuit.client.dto.cmd.course/*` 的迁移闭包。
- 补充进展（2026-01-06，S0.2 持续推进，保持行为不变）：已将推荐课程 CO `RecommendCourseCO` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`24595a53`）。
- 补充进展（2026-01-06，S0.2 持续推进，保持行为不变）：已将评教模板 CO `EvaTemplateCO` 从 `bc-evaluation-contract` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`34579fe0`），用于解除课程 CO（`CourseModelCO`）的类型依赖阻塞，支撑后续迁移 `CourseDetailCO`。
- 补充进展（2026-01-06，S0.2 持续推进，保持行为不变）：已将课程详情相关 CO（`TeacherInfoCO/CourseModelCO/CourseDetailCO`）从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`4dbcb2de`），进一步消除 eva-domain 对 bc-course 的类型依赖。
- 补充进展（2026-01-06，S0.2 收尾，保持行为不变）：Serena 证伪 `eva-domain` 已不再需要 `bc-course` 提供的 `edu.cuit.client.*` 类型后，移除 `eva-domain/pom.xml` 对 `bc-course` 的 Maven 依赖，改为显式依赖 `shared-kernel`（最小回归通过；落地：`01b36508`）。
- 补充进展（2026-01-06，S0.2 依赖收敛补齐，保持行为不变）：将课程用户侧接口 `IUserCourseService`（以及其出参 `SimpleSubjectResultCO`）迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`e2a697f1`），从而移除 `bc-ai-report-infra` 对 `bc-course` 的显式编译期依赖（避免依赖回潮）。
- 补充进展（2026-01-06，S0.2 延伸，保持行为不变）：已将评教域 CO `CourseScoreCO/EvaTeacherInfoCO` 从 `bc-evaluation/contract` 迁移到 `shared-kernel`（保持 `package` 不变），并移除 `bc-course/application` 对 `bc-evaluation-contract` 的编译期依赖（最小回归通过；落地：`bc30e9de`）。
- 补充进展（2026-01-06，S0.2 延伸，保持行为不变）：已将单节课详情 CO `SingleCourseDetailCO` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`95b01a07`）。
- 补充进展（2026-01-06，S0.2 延伸，保持行为不变）：已将单课次衍生详情 CO `ModifySingleCourseDetailCO` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`1e9be81d`）。
- 补充进展（2026-01-06，S0.2 延伸，保持行为不变）：已将课程 API 接口 `ICourseService/ICourseTypeService` 从 `bc-course/application` 迁移到 `shared-kernel`（保持 `package` 不变；最小回归通过；落地：`4dbeb55f`）。
- 补充进展（2026-01-06，S0.2 延伸，保持行为不变）：已将课程详情接口 `ICourseDetailService` 从 `bc-course/application` 迁移到 `shared-kernel`，并将其签名依赖的 `SimpleCourseResultCO` 一并下沉（均保持 `package` 不变；最小回归通过；落地：`f9ccc6e9`）。

**下一步建议（S0.1，保持行为不变；每步只改 1 个小包/小类簇）：**
1) 用 Serena 盘点 `eva-domain` 的 `import edu.cuit.client.*` 清单，并证伪“每个类型当前由哪个模块提供”（避免凭直觉改依赖）。
2) 聚焦一个高收益收敛口：**收敛 `eva-domain` 对 `bc-course`（应用层 jar）的编译期依赖面**。做法：把仍落在 `bc-course/application` 下、但本质是“边界协议对象”（`edu.cuit.client.*` 的 BO/CO/Query/Cmd）的小簇，逐步迁移到 `shared-kernel`（优先保持 `package` 不变以降风险）。
3) 当 Serena 证伪 `eva-domain` 不再引用 “仅由 `bc-course` 提供的 `edu.cuit.client.*` 类型” 后，再独立提交移除 `eva-domain/pom.xml` 对 `bc-course` 的依赖（保持行为不变；确保装配/编译闭合）。
4) 每步闭环：Serena → 最小回归 → commit → 同步三文档（保持行为不变）。

1) ✅ **评教任务发布写侧收敛**：把 `EvaUpdateGatewayImpl.postEvaTask` 收敛到 `bc-evaluation` 用例 + 端口，跨域副作用（消息/日志/缓存）按“事务提交后事件”固化（行为不变；落地提交：`8e434fe1/ca69b131/e9043f96`）。
2) ✅ **评教删除写侧收敛**：把 `EvaDeleteGatewayImpl.deleteEvaRecord/deleteEvaTemplate` 收敛到 `bc-evaluation`（行为不变；落地提交：`ea928055/07b65663/05900142`）。
3) ✅ **课程读侧渐进收敛**：为 `CourseQueryGatewayImpl` 引入 `QueryPort/QueryRepo`（先结构化，再考虑 CQRS 投影表；落地提交：`ba8f2003`）。
4) ✅ **评教读侧渐进收敛**：为 `EvaQueryGatewayImpl` 抽取 `EvaQueryRepo`，gateway 退化为委托壳（保持统计口径与异常文案不变；落地提交：`02f4167d`）。
5) **评教读侧进一步解耦**：按用例维度拆分 QueryService（任务/记录/统计/模板），将 query 端口逐步迁到 `bc-evaluation` 应用层，`eva-infra` 仅保留实现（行为不变）。  
   - 阶段性策略微调（2025-12-29）：允许“微调”（仅结构性重构；不改业务语义；缓存/日志/异常文案/副作用顺序完全不变）；短期优先把“评教读侧解耦（A → B）”主线推进到可验收里程碑，S0 折叠不再作为近期新增试点目标（避免分散注意力），仅做已在进行中的收尾与文档维护。
   - 补充进展（2025-12-29）：导出基类 `EvaStatisticsExporter` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（保持包名不变），并在 `bc-evaluation-infra` 补齐 `bc-course/bc-iam-contract` 依赖以闭合编译类型（保持行为不变；最小回归通过；落地：`e8ca391c`）。
   - 补充进展（2025-12-29）：导出装饰器 `FillAverageScoreExporterDecorator` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（保持包名不变；保持行为不变；最小回归通过；落地：`4e150984`）。
   - 补充进展（2025-12-29）：导出装饰器 `FillEvaRecordExporterDecorator` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（保持包名不变；保持行为不变；最小回归通过；落地：`b3afcb11`）。
   - 补充进展（2025-12-29）：导出装饰器 `FillUserStatisticsExporterDecorator` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（保持包名不变；保持行为不变；最小回归通过；落地：`e83600f6`）。
   - 补充进展（2025-12-29）：统计导出工厂 `EvaStatisticsExcelFactory` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（导出异常文案/日志输出完全一致；保持行为不变；最小回归通过；落地：`5b2c2223`）。
   - 补充进展（2025-12-29）：统计导出端口装配已切换：`BcEvaluationConfiguration.evaStatisticsExportPort()` 已改为委托 `bc-evaluation-infra` 的 `EvaStatisticsExportPortImpl`（内部仍调用 `EvaStatisticsExcelFactory.createExcelData`；保持行为不变；最小回归通过；落地：`565552fa`）。
   - 补充进展（2025-12-29）：将 POI 工具类 `ExcelUtils` 从 `eva-app` 迁移到 `eva-infra-shared`（保持包名不变）并补齐 `poi/poi-ooxml` 依赖，为后续导出实现从 `eva-app` 归位到基础设施模块扫清“循环依赖”风险（保持行为不变；最小回归通过；落地：`04009c85`）。
   - 补充进展（2025-12-29）：在 `EvaStatisticsQueryUseCase` 内部收敛 `type` 分支分发为 `dispatchByType(...)`，减少重复分支判断，避免后续继续归位方法簇时出现分支口径漂移（保持行为不变；最小回归通过；落地：`38ce9ece`）。
   - 补充进展（2025-12-29）：统计导出 `exportEvaStatistics` 调用归位到 `EvaStatisticsQueryUseCase`：引入导出端口 `EvaStatisticsExportPort`（Bean 仍委托既有 `EvaStatisticsExcelFactory.createExcelData`），旧入口 `EvaStatisticsServiceImpl.exportEvaStatistics` 退化为纯委托壳（保持 `@CheckSemId` 触发点与异常文案/日志顺序不变；最小回归通过；落地：`0d15de60`）。
   - 下一步建议（统计导出基础设施归位，保持行为不变；每次只迁 1 个类 + 最小回归）：✅ 已完成 `BcEvaluationConfiguration.evaStatisticsExportPort()` 委托切换（`565552fa`）；✅ 已完成 `eva-app/pom.xml` 移除 `poi/poi-ooxml` Maven 直依赖（课表解析已迁移到 `bc-course-infra`，保持行为不变；落地：`383dbf33`）。
   - 补充进展（2025-12-29）：课表 Excel/POI 解析已进一步“端口化”：新增 `CourseExcelResolvePort`（`bc-course/application`）并由 `bc-course-infra` 提供适配器实现，`eva-app` 调用侧改为依赖端口，移除对解析实现的直耦合（保持行为不变；落地：`5a7cd0a0`）。
   - 下一步建议（主线切换，保持行为不变）：`bc-messaging` 已阶段性闭环（依赖收敛后半段证伪 + 运行时装配由 `start` 承接），本主线不继续；如需推进仅做 S0 结构折叠/依赖证伪（见 `NEXT_SESSION_HANDOFF.md` 0.9，保持行为不变）。
   - 进展：已拆分统计/导出、任务、记录、模板查询端口（`EvaStatisticsQueryPort` / `EvaTaskQueryPort` / `EvaRecordQueryPort` / `EvaTemplateQueryPort`），应用层开始迁移，行为保持不变；旧 `EvaQueryGatewayImpl` 已移除；并已引入 `bc-evaluation-infra` 承接评教读侧查询实现（QueryPortImpl + QueryRepo/Repository，保持包名/行为不变；落地：`be6dc05c`）。
   - 补充进展（2025-12-27）：为后续“按用例进一步收窄依赖类型”做准备，已细分统计 QueryPort：新增 `EvaStatisticsOverviewQueryPort/EvaStatisticsTrendQueryPort/EvaStatisticsUnqualifiedUserQueryPort`，并让 `EvaStatisticsQueryPort` `extends` 以上子端口（仅接口拆分，不改实现/不改装配；保持行为不变；落地：`a1d6ccab`）。
   - 补充进展（2025-12-28）：工程噪音收敛（dev 环境 MyBatis 日志）：将 `application-dev.yml` 中 MyBatis-Plus 的 `log-impl` 从 `StdOutImpl` 切换为 `Slf4jImpl`，避免 SQL 调试日志直出 stdout（仅 dev profile，生产不变；最小回归通过；落地：`cb3a4620`）。
   - 补充进展（2025-12-28）：工程噪音收敛（dev/test 非法入参打印）：将 `application-dev.yml/application-test.yml` 中 `common.print-illegal-arguments` 从 `true` 调整为 `false`，减少控制台噪音（仅 dev/test profile；不改业务逻辑；最小回归通过；落地：`21ba35dd`）。
   - 补充进展（2025-12-28）：已在 `EvaStatisticsServiceImpl` 落地“依赖类型收窄”：由聚合接口 `EvaStatisticsQueryPort` 改为按用例簇注入三个子端口（不改业务逻辑/异常文案；保持行为不变；最小回归通过；落地：`c19d8801`）。
	   - 补充进展（2025-12-28）：已在统计导出侧（`EvaStatisticsExporter`）落地“依赖类型收窄”：静态初始化中将统计端口由 `EvaStatisticsQueryPort` 收窄为 `EvaStatisticsOverviewQueryPort`（保持 `SpringUtil.getBean(...)` 次数与顺序不变；保持行为不变；最小回归通过；落地：`9b3c4e6a`）。
	   - 补充进展（2025-12-28）：统计导出链路子端口补齐：新增 `EvaStatisticsCountAbEvaQueryPort`，并让 `EvaStatisticsOverviewQueryPort` `extends` 该子端口（仅接口细分，不改实现/不改装配；保持行为不变；最小回归通过；落地：`24b13138`）。
	   - 补充进展（2025-12-28）：统计导出基类依赖类型收窄：将导出基类 `EvaStatisticsExporter` 静态初始化中获取统计端口的依赖类型从 `EvaStatisticsOverviewQueryPort` 收窄为 `EvaStatisticsCountAbEvaQueryPort`（保持 `SpringUtil.getBean(...)` 次数与顺序不变；保持行为不变；最小回归通过；落地：`7337d378`）。
	   - 补充进展（2025-12-28）：已开始将统计读侧用例归位到 `bc-evaluation`：新增 `EvaStatisticsQueryUseCase`（当前为委托壳，不改变分支/异常文案/阈值计算），并在 `BcEvaluationConfiguration` 完成装配；`EvaStatisticsServiceImpl` 退化为委托该用例（保持行为不变；最小回归通过；落地：`db09d87b`）。
	   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分起步：新增得分子端口 `EvaRecordScoreQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`4e47ffe3`）。
	   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分：让聚合端口 `EvaRecordQueryPort` `extends EvaRecordScoreQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`0c7e7d13`）。
	   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分：新增分页子端口 `EvaRecordPagingQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`e4f0efe9`）。
	   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分：让聚合端口 `EvaRecordQueryPort` `extends EvaRecordPagingQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`76976c0b`）。
	   - 补充进展（2025-12-28）：记录读侧依赖类型收窄：`EvaRecordServiceImpl` 由注入 `EvaRecordQueryPort` 收窄为 `EvaRecordPagingQueryPort/EvaRecordScoreQueryPort`（不改业务逻辑/异常文案；最小回归通过；落地：`39a4bafe`）。
	   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分：新增用户日志子端口 `EvaRecordUserLogQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`fcac9324`）。
	   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分：让聚合端口 `EvaRecordQueryPort` `extends EvaRecordUserLogQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`1e025e48`）。
	   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分：新增按课程查询子端口 `EvaRecordCourseQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`e9034541`）。
	   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分：让聚合端口 `EvaRecordQueryPort` `extends EvaRecordCourseQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`b8efeaf5`）。
	   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分：新增数量统计子端口 `EvaRecordCountQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`db876379`）。
		   - 补充进展（2025-12-28）：记录读侧 QueryPort 细分：让聚合端口 `EvaRecordQueryPort` `extends EvaRecordCountQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`0d562206`）。
		   - 补充进展（2025-12-28）：记录导出链路子端口补齐：新增 `EvaRecordExportQueryPort`（组合 `EvaRecordCourseQueryPort/EvaRecordScoreQueryPort`），并让聚合端口 `EvaRecordQueryPort` `extends` 该子端口（仅新增接口+继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`5df35c36`）。
		   - 补充进展（2025-12-28）：记录导出链路依赖类型收窄：将导出基类 `EvaStatisticsExporter` 静态初始化中获取记录端口的依赖类型从 `EvaRecordQueryPort` 收窄为 `EvaRecordExportQueryPort`（保持 `SpringUtil.getBean(...)` 次数与顺序不变；保持行为不变；最小回归通过；落地：`682bf081`）。
		   - 补充进展（2025-12-28）：记录读侧依赖类型收窄：`UserServiceImpl` 由注入 `EvaRecordQueryPort` 收窄为 `EvaRecordCountQueryPort`（不改业务逻辑/异常文案；补充单测；最小回归通过；落地：`8b24d2f8`）。
		   - 补充进展（2025-12-28）：记录读侧依赖类型收窄：`MsgServiceImpl` 由注入 `EvaRecordQueryPort` 收窄为 `EvaRecordCountQueryPort`（不改业务逻辑/异常文案；保持行为不变；最小回归通过；落地：`147d486b`）。
		   - 补充进展（2025-12-28）：记录读侧依赖类型收窄：`UserEvaServiceImpl` 由注入 `EvaRecordQueryPort` 收窄为 `EvaRecordUserLogQueryPort/EvaRecordScoreQueryPort`（不改业务逻辑/异常文案；保持行为不变；最小回归通过；落地：`80886841`）。
			   - 补充进展（2025-12-28）：任务读侧 QueryPort 细分起步：新增单任务信息子端口 `EvaTaskInfoQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`26b79c3a`）。
			   - 补充进展（2025-12-28）：任务读侧聚合端口继承子端口：让 `EvaTaskQueryPort` `extends EvaTaskInfoQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`56834293`）。
			   - 补充进展（2025-12-28）：任务读侧依赖类型收窄：`MsgServiceImpl` 由注入 `EvaTaskQueryPort` 收窄为 `EvaTaskInfoQueryPort`（不改业务逻辑/异常文案；保持行为不变；最小回归通过；落地：`7aa49e7f`）。
			   - 补充进展（2025-12-28）：任务读侧 QueryPort 细分起步：新增分页子端口 `EvaTaskPagingQueryPort`（仅新增接口，不改实现/不改装配；保持行为不变；最小回归通过；落地：`f0a172d1`）。
			   - 补充进展（2025-12-28）：任务读侧聚合端口继承子端口：让 `EvaTaskQueryPort` `extends EvaTaskPagingQueryPort`（仅接口继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`2fd9d24e`）。
				   - 补充进展（2025-12-28）：任务读侧 QueryPort 细分：新增本人任务/数量统计子端口 `EvaTaskSelfQueryPort/EvaTaskCountQueryPort`，并让 `EvaTaskQueryPort` `extends` 这些子端口（仅新增接口+继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`9d5064fc`）。
				   - 补充进展（2025-12-28）：任务读侧依赖类型收窄：`EvaTaskServiceImpl` 由注入 `EvaTaskQueryPort` 收窄为 `EvaTaskPagingQueryPort/EvaTaskSelfQueryPort/EvaTaskInfoQueryPort`（不改业务逻辑/异常文案；保持行为不变；最小回归通过；落地：`4b22f059`）。
				   - 补充进展（2026-01-01）：任务读侧用例归位深化（分页）：新增 `EvaTaskQueryUseCase` 并将旧入口 `EvaTaskServiceImpl.pageEvaUnfinishedTask` 退化为纯委托壳，把“分页查询 + 实体→CO 组装 + 分页结果组装”归位到 UseCase（`@CheckSemId` 触发点仍保留在旧入口；保持行为不变；最小回归通过；落地：`d67f0ace`）。
				   - 补充进展（2026-01-02）：任务读侧用例归位深化（单任务详情）：将旧入口 `EvaTaskServiceImpl.oneEvaTaskInfo` 退化为纯委托壳，并把 “单任务查询 + 懒加载顺序对齐的实体→CO 组装” 归位到 `EvaTaskQueryUseCase`（异常文案不变；保持行为不变；最小回归通过；落地：`94736365`）。
				   - 补充进展（2026-01-02）：任务读侧用例归位深化（本人任务列表）：将旧入口 `EvaTaskServiceImpl.evaSelfTaskInfo` 的“任务列表查询 + 懒加载顺序对齐的实体→CO 组装”归位到 `EvaTaskQueryUseCase`；旧入口仍保留 `@CheckSemId` 与当前用户解析（`StpUtil` + `userQueryGateway`）并委托 UseCase（异常文案/副作用顺序不变；保持行为不变；最小回归通过；落地：`1ac196c6`）。
					   - 补充进展（2025-12-28）：模板读侧 QueryPort 细分起步：新增分页/全量/按任务取模板子端口 `EvaTemplatePagingQueryPort/EvaTemplateAllQueryPort/EvaTemplateTaskTemplateQueryPort`，并让 `EvaTemplateQueryPort` `extends` 这些子端口（仅新增接口+继承，不改实现/不改装配；保持行为不变；最小回归通过；落地：`a14d3c53`）。
					   - 补充进展（2025-12-28）：模板读侧依赖类型收窄：`EvaTemplateServiceImpl` 由注入 `EvaTemplateQueryPort` 收窄为 `EvaTemplatePagingQueryPort/EvaTemplateAllQueryPort/EvaTemplateTaskTemplateQueryPort`（不改业务逻辑/异常文案；保持行为不变；最小回归通过；落地：`b86db7e4`）。
					   - 补充进展（2026-01-01）：模板读侧用例归位深化（分页）：新增 `EvaTemplateQueryUseCase` 并将旧入口 `EvaTemplateServiceImpl.pageEvaTemplate` 退化为纯委托壳，把“实体→CO 组装 + 分页结果组装”归位到 UseCase（`@CheckSemId` 触发点仍保留在旧入口；时间格式/分页字段赋值顺序不变；保持行为不变；最小回归通过；落地：`afcb4ff7`）。
					   - 补充进展（2026-01-01）：模板读侧用例归位深化（按任务取模板）：将旧入口 `EvaTemplateServiceImpl.evaTemplateByTaskId` 退化为纯委托壳，并把 “按任务取模板 + 空结果兜底 JSON” 归位到 `EvaTemplateQueryUseCase`（`@CheckSemId` 触发点仍保留在旧入口；保持行为不变；最小回归通过；落地：`f98a9eed`）。
					   - 补充进展（2026-01-02）：模板读侧用例归位深化（全量模板列表）：将旧入口 `EvaTemplateServiceImpl.evaAllTemplate` 退化为纯委托壳，并把 “全量模板查询 + 结果组装” 归位到 `EvaTemplateQueryUseCase`（保持行为不变；最小回归通过；落地：`cd8e6ecb`）。
				   - 补充进展（2025-12-28）：模板读侧引用面盘点结论/证伪：使用 Serena 盘点 `EvaTemplateQueryPort` 在全仓库的引用面，除端口定义外仅剩实现侧 `EvaTemplateQueryPortImpl`；应用层未发现其它对聚合端口的注入点/调用点，因此模板主题的“端口细分 + 服务层依赖类型收窄”阶段可视为已闭合（保持行为不变；以 `NEXT_SESSION_HANDOFF.md` 的 0.9 证据记录为准）。
				   - 补充进展（2025-12-28）：统计读侧用例归位深化：将 `EvaStatisticsServiceImpl.pageUnqualifiedUser` 的 `type` 分支选择归位到 `EvaStatisticsQueryUseCase.pageUnqualifiedUser`（`@CheckSemId` 触发点仍保留在旧入口；异常文案不变；保持行为不变；最小回归通过；落地：`22dccc70`）。
			   - 下一步建议（方向 A → B，保持行为不变）：先将记录/任务/模板按同套路做“子 QueryPort 接口 + `extends`”与 `eva-app` 注入类型收窄；并将 `EvaStatisticsQueryUseCase` 逐步从委托壳演进为“统计读侧用例编排落点”（每次只迁 1 个方法簇，且 `@CheckSemId` 触发点仍保留在旧入口）。
				   - 下一步建议（阶段性，保持行为不变）：评教读侧 D1 已覆盖 `eva-app/src/main/java/edu/cuit/app/service/impl/eva` 下 5 个入口（统计/记录/任务/模板/用户评教记录），后续建议将同套路复制到 `bc-course` 的读侧入口（优先 `ICourseServiceImpl` 的“纯查询”方法簇）。✅ 已起步：新增 `CourseQueryUseCase` 并将 `courseNum/courseTimeDetail/getDate` 退化为委托壳（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地：`4b06187f`）。✅ 进一步起步：新增 `CourseDetailQueryUseCase` 并将 `getCourseDetail` 退化为委托壳（异常文案保持不变；最小回归通过；落地：`d045c79e`）。✅ 进一步起步：新增 `TimeCourseQueryUseCase` 并将 `getTimeCourse` 退化为委托壳（`StpUtil.getLoginId()` 仍保留在旧入口，保持行为不变；最小回归通过；落地：`4454ecae`）。✅ 写侧起步：将 `allocateTeacher` 退化为委托壳并引入 `AllocateTeacherUseCase`（`AfterCommitEventPublisher` 发布顺序不变；最小回归通过；落地：`6e20721b`）。✅ 写侧起步：将 `deleteCourses` 退化为委托壳并引入 `DeleteCoursesEntryUseCase`（`AfterCommitEventPublisher` 发布顺序不变；最小回归通过；落地：`d53b287a`）。✅ 写侧继续：已完成 `ICourseServiceImpl.updateSingleCourse` 入口用例归位（保持两次 `StpUtil.getLoginId()` 调用位置/顺序与 AfterCommit 发布顺序完全不变；最小回归通过；落地以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。✅ 写侧继续：已完成 `ICourseServiceImpl.addNotExistCoursesDetails` 入口用例归位（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地：`5a73fb75`）。✅ 写侧继续：已完成 `ICourseServiceImpl.addExistCoursesDetails` 入口用例归位（保持行为不变；最小回归通过；落地：`a5a9c777`）。✅ 写侧继续：已完成 `IUserCourseServiceImpl.deleteSelfCourse` 入口用例归位（保持两次 `StpUtil.getLoginId()` 调用位置/顺序与 AfterCommit 发布顺序完全不变；最小回归通过；落地：`76845038`）。✅ 写侧继续：已完成 `IUserCourseServiceImpl.updateSelfCourse` 入口用例归位（保持两次 `StpUtil.getLoginId()` 调用位置/顺序与 AfterCommit 发布顺序完全不变，且 `CourseOperationMessageMode.TASK_LINKED` 参数不变；最小回归通过；落地：`2d1327d3`）。✅ 写侧继续：已完成 `IUserCourseServiceImpl.importCourse` 的 `importCourseFile(...)` 调用点端口化（保持解析/异常/AfterCommit 顺序不变；最小回归通过；落地：`054b511d`）。✅ 写侧继续：已完成 `ICourseDetailServiceImpl.updateCourse` 的 `updateCourse(...)` 调用点端口化（保持 `@CheckSemId`、异常转换与 AfterCommit 顺序不变；最小回归通过；落地：`bcf17d7f`）。✅ 写侧继续：已完成 `ICourseDetailServiceImpl.updateCourses` 入口用例归位（保持 `@CheckSemId/@Transactional` 触发点与异常转换不变；最小回归通过；落地：`849ed92e`）。✅ 写侧继续：已完成 `ICourseDetailServiceImpl.delete` 入口用例归位（保持 `@CheckSemId` 触发点与 `deleteCourse(...) → userId 查询 → publishAfterCommit(...)` 顺序不变；最小回归通过；落地：`e38463c2`）。✅ 写侧继续：已完成 `ICourseDetailServiceImpl.addCourse` 入口用例归位（保持 `@CheckSemId` 触发点与 `courseUpdateGateway.addCourse(semId)` 调用链不变；最小回归通过；落地：`5c989ace`）。✅ S0 收尾（依赖收窄）：已清理 `ICourseServiceImpl` 中残留的未使用注入依赖（`CourseQueryGateway/CourseUpdateGateway` 仅声明无调用点；保持行为不变；最小回归通过；落地：`9577cd85`）。✅ S0 收尾（依赖收窄）：已清理 `IUserCourseServiceImpl` 中残留的未使用注入依赖（`CourseDeleteGateway/MsgServiceImpl/MsgResult` 仅声明无调用点；保持行为不变；最小回归通过；落地：`402affc2`）。✅ S0 收尾（依赖收窄）：将 `IUserCourseServiceImpl.isImported` 的依赖从 `CourseUpdateGateway` 收敛为直接依赖 `IsCourseImportedUseCase`（原 gateway 本身已委托该用例；本次仅收窄注入依赖，不改业务语义；保持行为不变；最小回归通过；落地：`25aad45a`）。✅ S0（旧 gateway 压扁为委托壳）：压扁 `CourseUpdateGatewayImpl.updateCourseType`：新增 `UpdateCourseTypeEntryUseCase` 并让旧 gateway 仅保留事务边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地：`785974a6`）。✅ S0（旧 gateway 压扁为委托壳）：压扁 `CourseUpdateGatewayImpl.addCourseType`：新增 `AddCourseTypeEntryUseCase` 并让旧 gateway 仅保留事务边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地：`34e9a0a8`）。✅ S0（旧 gateway 压扁为委托壳）：压扁 `CourseUpdateGatewayImpl.updateCoursesType`：新增 `UpdateCoursesTypeEntryUseCase` 并让旧 gateway 仅保留事务边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地：`709dc5b6`）。✅ S0（旧 gateway 压扁为委托壳）：进一步压扁 `CourseDeleteGatewayImpl.deleteCourseType`：新增 `DeleteCourseTypeEntryUseCase` 并让旧 gateway 仅保留事务边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地：`cf747b9c`）。✅ S0（旧 gateway 压扁为委托壳）：进一步压扁 `CourseDeleteGatewayImpl.deleteCourse`：新增 `DeleteCourseGatewayEntryUseCase` 并让旧 gateway 仅保留事务边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地：`dfd977fe`）。✅ S0（旧 gateway 压扁为委托壳）：进一步压扁 `CourseDeleteGatewayImpl.deleteCourses`：新增 `DeleteCoursesGatewayEntryUseCase` 并让旧 gateway 仅保留事务边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地：`6428e685`）。✅ S0（旧 gateway 压扁为委托壳）：进一步压扁 `CourseDeleteGatewayImpl.deleteSelfCourse`：新增 `DeleteSelfCourseGatewayEntryUseCase` 并让旧 gateway 仅保留事务边界与委托调用（不在基础设施层构造命令/编排流程；保持行为不变；最小回归通过；落地：`c0268b14`）。✅ S0（旧 gateway 压扁为委托壳）：压扁 `CourseUpdateGatewayImpl.assignTeacher`：新增 `AssignTeacherGatewayEntryUseCase` 并让旧 gateway 不再构造命令（保持事务边界/异常文案/副作用顺序完全不变；最小回归通过；落地：`0b85c612`）。✅ S0（旧 gateway 压扁为委托壳）：压扁 `CourseUpdateGatewayImpl.updateCourse`：新增 `UpdateCourseGatewayEntryUseCase` 并让旧 gateway 不再构造 Command（保持事务边界/异常文案/副作用顺序完全不变；最小回归通过；落地：`c31df92c`）。下一步建议：继续压扁 `CourseUpdateGatewayImpl.updateSingleCourse`（Serena：调用点为 `UpdateSingleCoursePortImpl.updateSingleCourse`；每次只改 1 个方法，保持行为不变）。
			     - 记录主题建议顺序：✅ `MsgServiceImpl`（已收窄依赖：`EvaRecordCountQueryPort`）→ ✅ `UserEvaServiceImpl`（已收窄依赖：`EvaRecordUserLogQueryPort/EvaRecordScoreQueryPort`）→ 视测试可控性再处理导出/AI 报告链路对记录端口的依赖收窄（保持行为不变；相关类可能涉及 `StpUtil` 静态登录态，单测需提前规划“可重复”的登录态注入策略）。
			     - 任务主题建议顺序（保持行为不变）：✅ `MsgServiceImpl` 与 ✅ `EvaTaskServiceImpl` 的任务端口依赖类型已完成收窄；（模板主题）✅ 子 QueryPort 与 ✅ `EvaTemplateServiceImpl` 依赖类型收窄已完成。下一步建议：如需继续推进模板读侧解耦，可盘点是否存在其它引用 `EvaTemplateQueryPort` 的应用层类并逐一收窄（每次只改 1 个类 + 最小回归/可运行单测）。
	6) ✅ **IAM 写侧继续收敛**：`UserUpdateGatewayImpl.deleteUser` 已收敛到 `bc-iam`（含 LDAP 删除、角色解绑、缓存失效/日志等副作用，保持行为不变；落地提交：`5f08151c/e23c810a/cccd75a3/2846c689`）。
7) ✅ **系统管理读侧渐进收敛**：`UserQueryGatewayImpl` 的用户查询能力已收敛到 `bc-iam`（保持行为不变；落地提交：`3e6f2cb2/8c245098/92a9beb3`、`9f664229/38384628/de662d1c/8a74faf5`、`56bbafcf/7e5f0a74/bc5fb3c6/6a1332b0`）。
8) ✅ **系统管理写侧继续收敛**：`RoleUpdateGatewayImpl.assignPerms/deleteMultipleRole` 与菜单变更触发的缓存失效（`MenuUpdateGatewayImpl.handleUserMenuCache`）已收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
   - 落地提交链：`f8838951/91d13db4/7fce88b8/d6e3bed1/4d650166/46e666f9`
9) ✅ **系统管理写侧继续收敛（菜单/角色写侧主链路）**：
   - 菜单写侧主链路（`MenuUpdateGatewayImpl.updateMenuInfo/deleteMenu/deleteMultipleMenu/createMenu`）已收敛到 `bc-iam`（保持行为不变；落地提交：`f022c415`）。
   - 角色写侧剩余入口（`RoleUpdateGatewayImpl.updateRoleInfo/updateRoleStatus/deleteRole/createRole`）已收敛到 `bc-iam`（保持行为不变；落地提交：`64fadb20`）。
10) **AI 报告 / 审计日志模块化（建议）**：启动 `bc-ai-report` / `bc-audit` 最小骨架，并优先挑选 1 条写链路按同套路收敛（保持行为不变）。
   - 进展：已完成骨架与组合根 wiring（提交点 A：`a30a1ff9`）；已完成审计日志写链路收敛（提交点 B：`b0b72263`）；已完成 AI 报告导出链路收敛（提交点 B2：`c68b3174`）；已完成导出旧入口进一步退化为纯委托壳（提交点 B3：`7f4b3358`）。
   - 补充进展（2025-12-27）：已将 AI 报告导出实现（`AiReportDocExportPortImpl` + `AiReportExporter`）从 `eva-app` 归位到 `bc-ai-report`（保持 `package` 不变；保持行为不变；落地提交：`d1262c32`）。
   - 补充进展（2025-12-27）：已将 AI 报告 analysis 端口适配器 `AiReportAnalysisPortImpl` 从 `eva-app` 归位到 `bc-ai-report`（保持 `package` 不变；保持行为不变；落地提交：`6f34e894`）。
   - 补充进展（2025-12-27）：已将 AI 报告 username → userId 查询端口适配器 `AiReportUserIdQueryPortImpl` 从 `eva-app` 归位到 `bc-ai-report`（保持 `package` 不变；保持行为不变；落地提交：`e2a608e2`）。
   - 补充进展（2025-12-27）：已将 AI 相关基础设施 `edu.cuit.infra.ai.*`（模型 Bean 配置、提示词常量、消息工具等）从 `eva-infra` 归位到 `bc-ai-report`，并移除 `bc-ai-report` → `eva-infra` 编译期依赖（保持 `package` 不变；保持行为不变；落地提交：`e2f2a7ff`）。
   - 补充进展（2025-12-27）：已将 `bc-ai-report` 组合根 `BcAiReportConfiguration` 从 `eva-app` 归位到 `bc-ai-report`（保持 `package` 不变；Bean 定义与 `@Lazy` 环断策略不变；保持行为不变；落地提交：`58c2f055`）。
   - 补充进展（2025-12-27）：已将 `@CheckSemId` 注解 `edu.cuit.app.aop.CheckSemId` 从 `eva-app` 下沉到 `shared-kernel`（保持 `package` 不变；切面匹配表达式不变；保持行为不变；落地提交：`1c595052`）。
   - 补充进展（2025-12-27）：已将 AI 报告旧入口 `AiCourseAnalysisService` 从 `eva-app` 归位到 `bc-ai-report`（保持 `package` 不变；保持 `@Service/@CheckSemId` 触发点不变；保持行为不变；落地提交：`ca321a20`）。
   - 补充进展（2025-12-27）：已用 Serena 对全仓库“剩余落库/记录写链路”做证据化盘点并证伪（证据清单见 `NEXT_SESSION_HANDOFF.md` 的 0.9）；因此条目 25 的后续重点切换为 **S0 折叠 `bc-ai-report`**（仅搬运/依赖收敛，保持行为不变）。
11) ✅ **BC 自包含三层结构试点（`bc-iam`，阶段 1：适配器归属）**：已引入平铺过渡模块 `bc-iam-infra` 并接入组合根，且已将 `bciam/adapter/*` 端口适配器从 `eva-infra` 迁移到 `bc-iam-infra`（保持行为不变；落地提交：`42a6f66f/070068ec/03ceb685/02b3e8aa/6b9d2ce7/5aecc747/1c3d4b8c`）。
   - 说明：阶段 2（IAM DAL 抽离 + shared 拆分 + 去依赖）已完成，`bc-iam-infra` 已不再依赖 `eva-infra`（落地提交：`2ad911ea`；保持行为不变）。

### 10.4 术语澄清与最终目标结构（减少“gateway”混淆）

> 背景：项目早期参考 COLA 命名把大量类叫做 `*GatewayImpl`，但其中一部分事实上同时扮演了“应用入口 + DB 访问 + 副作用”，导致“gateway=DB gateway”的直觉与 DDD/六边形的职责划分冲突。
>
> 渐进式重构期间，本计划将严格按职责描述，而不被历史命名绑架：

- **UseCase（应用层用例）**：BC 的写侧入口（`bc-*/application/usecase`），不直接依赖 DB，只依赖 Port。
- **Port（应用层端口）**：UseCase 的出站依赖抽象（`bc-*/application/port`）。
- **Port Adapter（基础设施适配器）**：实现 Port、搬运旧 DB/副作用流程。过渡期可能放在 `eva-infra/.../bc*/adapter` 或历史过渡模块 `bc-*-infra`；**需求变更后**不再新增 `bc-*-infra` 平铺模块，新增适配器优先归位到目标 BC 的 `infrastructure` 子模块（见下方“最终目标结构”）。
- **旧 gateway（委托壳 / 兼容入口）**：历史 `eva-infra/.../*GatewayImpl`，对外接口不变（尤其缓存注解触发点），内部逐步退化为委托到 UseCase。

最终目标（你期望的“完美契合 DDD”）建议按 BC 自包含推进（优先 `bc-iam` 试点）：
- 每个 BC 至少在 **package** 上自包含三层：`<bc>.domain` / `<bc>.application` / `<bc>.infrastructure`。
- **需求变更（2025-12-24）**：每个 BC 在仓库中只占用 **一个顶层目录/聚合模块**（例如 `bc-iam/`），其内部按职责拆为 `domain/application/infrastructure` **子模块**（例如 `bc-iam/domain`、`bc-iam/application`、`bc-iam/infrastructure`），并由 `start`/组合根统一装配。历史上已存在的 `bc-iam-infra`、`bc-evaluation-infra` 等平铺模块作为过渡形态保留，后续按“折叠归位”里程碑迁入对应 BC 的内部子模块。
- **需求补充（2025-12-24）**：逐步拆解 `eva-client`：将 `edu.cuit.client.*` 下的 BO/CO/DTO 等对象按业务归属迁入对应 BC（优先放在 BC 的 `application` 子模块下的 `contract/dto` 包，避免领域层污染；**允许改包名**以完成归位）；确实跨 BC 复用的对象再沉淀到 shared-kernel；最终让 `eva-client` 退出主干依赖。
- `eva-*` 技术切片逐步退场：最终仅保留 shared-kernel、统一启动/装配与跨 BC 的极少量集成胶水（严格受控）。

### 10.5 `eva-*` 技术切片退场/整合到 BC 的路线（长期，保持行为不变）

> 回答“什么时候可以把 `eva-*` 模块整合进各业务 `bc-*`”的统一口径：**不是某个固定日期，而是一组可验证的前置条件**。避免在入口/装配尚未收敛时“硬搬模块”导致装配顺序、切面触发点或副作用顺序漂移。

**阶段定义（从“可以开始”到“可以移除模块”）：**

1) **可以开始做整合（进入 S1）的前置条件**
   - ✅ 业务入口已按“用例 + 端口 + 端口适配器 + 委托壳”模式持续推进，且每次只迁 1 个入口方法簇并闭环（Serena → 最小回归 → 提交 → 三文档同步）。
   - ✅ 核心 BC 的结构折叠（S0）已完成或接近完成（`bc-iam/bc-evaluation/bc-course/bc-template/bc-ai-report/bc-audit` 已完成阶段性折叠；`bc-messaging` 尚未折叠时不建议启动“全量整合 eva-*”）。
   - ✅ 组合根装配责任清晰：运行时装配以 `start` 为主，`eva-app` 仅作为过渡装配层（可逐步抽薄，但不要在同一提交里同时“迁入口 + 迁装配”）。

2) **可以移除 `eva-app` / `eva-adapter` 的判定标准（建议的 DoD）**
   - `eva-adapter`：Controller 仅承载 HTTP 协议适配与参数校验；业务编排已全部进入对应 `bc-*/application`（或由 `bc-*` 的入口类承接），并且 Controller 不再直接依赖 `eva-infra` 的实现细节。
   - `eva-app`：不再包含任何“业务入口实现”（大量 `*ServiceImpl` 只剩委托壳或已归位），`@CheckSemId` 触发点与 `StpUtil.getLoginId()` 调用次数/顺序在迁移后仍可逐项对照证伪；装配要么迁入 `start`，要么迁入对应 BC 的 configuration（保持 Bean 名称与初始化顺序不变）。

3) **可以移除 `eva-infra` 的判定标准（建议的 DoD）**
   - `eva-infra` 中旧 `*GatewayImpl` 已全部退化为委托壳或迁入对应 `bc-*/infrastructure`（或其过渡落点），不再承担“业务编排”。
   - `eva-infra-dal` / `eva-infra-shared` 作为跨 BC 的共享技术模块：可保留（推荐），或后续再按里程碑评估是否更名/进一步收敛到 shared-kernel；**不建议**把这类跨域共享硬塞进某个单一 BC。

**节奏建议（避免一次性大迁移）：**

- 先把“入口方法簇”按 BC 迁完（A→B），让 `eva-app` 业务实现逐步清空；再做“模块移除/目录折叠”类变更。
- 每次只做一个维度：要么迁入口（业务代码），要么迁装配/模块结构（wiring/Maven），避免同时改动导致回滚困难。
- 盘点清单落点：`docs/DDD_REFACTOR_BACKLOG.md` 的 4.3（未收敛清单）用于持续维护“仍在 eva-* 的入口/旧网关候选”。

### 10.3 未完成清单（滚动，供下一会话排期）

#### bc-course（课程）S0.2 延伸：协议承载面继续收敛（保持行为不变）

> 背景：S0.2 的“主目标”（`eva-domain` 去 `bc-course` 编译期依赖）已闭环；但 `bc-course/application` 曾长期承载一批 `edu.cuit.client.*` 协议接口/对象，且部分签名依赖评教域 DTO（如 `CourseScoreCO/EvaTeacherInfoCO`）。本阶段已按路线 A 将该簇逐步下沉到 `shared-kernel`（均保持 `package` 不变；保持行为不变），并移除 `bc-course/application` 对 `bc-evaluation-contract` 的编译期依赖，避免“课程协议签名反向依赖评教 contract”。下一步主线切换为：在协议承载面已进入 `shared-kernel` 的前提下，逐个模块收敛对 `bc-course` 的编译期依赖（每次只改 1 个 `pom.xml`，便于回滚）。

- 下一步（建议每步只改 1 个小包/小类簇，保持行为不变）：
  1) ✅ Serena 证据化盘点：`edu.cuit.client.api.course` 下残留接口（`ICourseDetailService/ICourseService/ICourseTypeService`）以及其签名依赖的“跨 BC”类型落点与引用面（已完成；证据与结论以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
  2) ✅ 路线 A（推荐，最小改动）：已将确属“跨 BC 协议对象”的 `edu.cuit.client.dto.clientobject.eva` 小簇（`CourseScoreCO/EvaTeacherInfoCO`）迁到 `shared-kernel`（保持 `package` 不变；落地：`bc30e9de`）。
  3) ✅ 路线 A（继续推进，保持行为不变）：已将 `SingleCourseDetailCO/ModifySingleCourseDetailCO/SimpleCourseResultCO` 与课程 API 接口（`ICourseDetailService/ICourseService/ICourseTypeService`）逐步下沉到 `shared-kernel`（均保持 `package` 不变；细节以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
  4) ✅ 延伸主线（依赖方收敛，保持行为不变）：已完成两处“依赖方收敛”闭环：`bc-evaluation-infra` 去 `bc-course` 编译期依赖（落地：`a0bcf74f`）与 `eva-infra-shared` 去 `bc-course` 编译期依赖（落地：`6ab3837a`）。补齐：为闭合 `bc-evaluation-infra` 对 `ISemesterService` 的编译期引用，已将 `edu.cuit.client.api.ISemesterService` 从 `bc-course/application` 下沉到 `shared-kernel`（保持 `package` 不变；落地：`c22802ff`）。同时已启动“课程域基础设施归位”：将 `edu.cuit.infra.bccourse.adapter/*PortImpl` 从 `eva-infra` 迁移到 `bc-course-infra`（保持 `package` 不变；落地：`c4179654`），并将 `ClassroomCacheConstants` 归位到 `eva-infra-shared` 以便后续迁移 `*RepositoryImpl`（落地：`c22802ff`）。下一步：继续对 `eva-adapter`/`eva-app` 做同样的“证伪 → 依赖替换”（每步只改 1 个 `pom.xml`，保持行为不变；注意 `eva-adapter` 当前仅经由 `eva-app` 间接获得 `bc-course`，需先评估是否存在“可替换的显式依赖点”再动手）。
     - 补充阻塞（保持行为不变）：`eva-infra` 当前仍大量引用 `edu.cuit.bc.course.*`（课程域用例/端口/异常 + 端口适配器/旧 gateway 委托壳），因此暂不满足“仅使用 `edu.cuit.client.*`”的前提；需先按里程碑将课程域基础设施逐步归位到 `bc-course-infra`（或 `bc-course/infrastructure`），再评估 `eva-infra` 去 `bc-course` 依赖（证据与记录以 `NEXT_SESSION_HANDOFF.md` 0.9 为准）。
  5) 路线 B（后置，结构更清晰但成本更高）：若后续发现 `shared-kernel` 承载课程域接口/CO 规模继续膨胀，可新增 `bc-course-contract`（或更中立的 contract 模块）承载这些接口/CO，避免继续膨胀 `shared-kernel`；再逐步把依赖方从 `shared-kernel` 切到 contract（保持 `package`/行为不变）。

#### bc-messaging（消息域）后置规划（仅规划，不落地；保持行为不变）

> 背景：截至 2026-01-02，`bc-messaging` 已完成组合根/监听器/端口适配器归位与依赖收敛关键环节（见 `NEXT_SESSION_HANDOFF.md` 0.9），本段保留为“后置结构折叠/依赖证伪”清单（保持行为不变）。
> 目标：若后续需要继续推进，优先做 S0 结构折叠与依赖面证伪；当前会话不落地“新增折叠试点”类提交，避免分散主线（保持行为不变）。

- 现状散落点（Serena 证据化盘点，2025-12-29）：
  - 组合根：✅ 已归位到 `bc-messaging/src/main/java/edu/cuit/app/config/BcMessagingConfiguration.java`（保持 `package` 不变；落地：`4e3e2cf2`）。
  - 应用侧监听器：✅ `bc-messaging/src/main/java/edu/cuit/app/bcmessaging/CourseOperationSideEffectsListener.java`（落地：`22ee30e7`）；✅ `bc-messaging/src/main/java/edu/cuit/app/bcmessaging/CourseTeacherTaskMessagesListener.java`（落地：`0987f96f`）
  - 应用侧端口适配器：✅ `bc-messaging/src/main/java/edu/cuit/app/bcmessaging/adapter/CourseBroadcastPortAdapter.java`（落地：`84ee070a`）；✅ `bc-messaging/src/main/java/edu/cuit/app/bcmessaging/adapter/TeacherTaskMessagePortAdapter.java`（落地：`9ea14cff`）；✅ `bc-messaging/src/main/java/edu/cuit/app/bcmessaging/adapter/EvaMessageCleanupPortAdapter.java`（落地：`73ab3f3c`；依赖类型收窄为 `IMsgService` 以避免 Maven 循环依赖，行为不变）
  - 基础设施端口适配器：✅ `bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageDeletionPortImpl.java`、✅ `bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageReadPortImpl.java`、✅ `bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageDisplayPortImpl.java`、✅ `bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageInsertionPortImpl.java`、✅ `bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageQueryPortImpl.java`（均已归位）。
  - 对应应用层端口（用于建立“端口→适配器”映射）：`bc-messaging/src/main/java/edu/cuit/bc/messaging/application/port/*Port.java`
  - 迁移前置：为避免应用侧端口适配器对 `eva-app` 的编译期耦合导致 Maven 循环依赖，已先将消息发送组装类 `MsgResult` 从 `eva-app` 归位到 `bc-messaging-contract`（保持包名不变；落地：`31878b61`）。
  - 迁移前置（DAL/Convertor 归位，保持行为不变）：消息表数据对象 `MsgTipDO` 与 Mapper `MsgTipMapper`（含 `MsgTipMapper.xml`）已从 `eva-infra` 归位到 `eva-infra-dal`（保持 `package/namespace` 不变）；消息转换器 `MsgConvertor` 已从 `eva-infra` 归位到 `eva-infra-shared`（保持 `package` 不变）。本阶段基础设施端口适配器已全部归位完成，后续可转入“依赖收敛/结构折叠”等更高收益里程碑（仍保持行为不变）。
  - 依赖收敛准备（事件枚举下沉到 contract，保持行为不变）：将 `CourseOperationMessageMode` 从 `bc-messaging` 迁移到 `bc-messaging-contract`（保持 `package edu.cuit.bc.messaging.application.event` 不变；落地：`b2247e7f`）。
  - 依赖收敛准备（事件载荷下沉到 contract，保持行为不变）：将 `CourseOperationSideEffectsEvent` 从 `bc-messaging` 迁移到 `bc-messaging-contract`（保持 `package edu.cuit.bc.messaging.application.event` 不变；落地：`ea2c0d9b`）。
  - 依赖收敛准备（事件载荷下沉到 contract，保持行为不变）：将 `CourseTeacherTaskMessagesEvent` 从 `bc-messaging` 迁移到 `bc-messaging-contract`（保持 `package edu.cuit.bc.messaging.application.event` 不变；落地：`12f43323`）。
  - 依赖收敛（应用侧编译期依赖面收窄，保持行为不变）：`eva-app` 对消息域的 Maven 依赖从 `bc-messaging` 收敛为仅依赖 `bc-messaging-contract`（仅用于事件载荷类型；落地：`d3aeb3ab`）。
- 下一步建议（依赖收敛后半段，保持行为不变）：✅ 已完成：在 `start/pom.xml` 补齐 `bc-messaging` 的 `runtime` 依赖（保持行为不变；落地：`f23254ec`）；✅ 已完成：移除 `eva-infra/pom.xml` 中 `bc-messaging` 的 `runtime` 依赖，把“运行时装配责任”上推到组合根 `start`（保持行为不变；落地：`507f95b2`）。✅ 已完成后置建议：使用 Serena 证伪 `eva-infra` 无 `bcmessaging` / `edu.cuit.bc.messaging` / `bc-messaging` 的编译期引用，且运行时装配由 `start` 承接（证据见 `NEXT_SESSION_HANDOFF.md` 0.9；保持行为不变）。

- 建议拆分提交（每步：Serena 符号级引用分析 → 最小回归 → commit → 三文档同步）：
  1) **盘点与证据化（只改文档）**：用 Serena 列出 `eva-app/eva-infra` 中与消息域相关的散落点与引用面（优先：`BcMessagingConfiguration`、`CourseOperationSideEffectsListener`、`CourseTeacherTaskMessagesListener`、`eva-app/.../bcmessaging/adapter/*`、`eva-infra/.../bcmessaging/adapter/*`），形成“迁移清单 + 风险点”。
  2) ✅ **组合根归位**：已将 `eva-app/src/main/java/edu/cuit/app/config/BcMessagingConfiguration.java` 迁移到 `bc-messaging`（保持 `package edu.cuit.app.config` 不变；Bean 定义/装配顺序不变；最小回归通过；落地：`4e3e2cf2`）。
  3) **应用侧适配器/监听器归位**：将 `eva-app/src/main/java/edu/cuit/app/bcmessaging/*` 与 `eva-app/src/main/java/edu/cuit/app/bcmessaging/adapter/*` 逐个迁移到 `bc-messaging`（建议保持 `package` 不变；每次只迁 1 个类并补齐/复用可运行回归）。补充进展：已完成监听器 `CourseOperationSideEffectsListener`（`22ee30e7`）与 `CourseTeacherTaskMessagesListener`（`0987f96f`）迁移。
  4) **基础设施端口适配器归位（后置）**：将 `eva-infra/src/main/java/edu/cuit/infra/bcmessaging/adapter/*PortImpl.java` 逐步迁移到 `bc-messaging` 的基础设施落点（理想形态为 `bc-messaging/infrastructure` 子模块；在当前“主线优先、不新增试点”策略下，先仅规划与盘点依赖闭包，待评教读侧阶段性收敛后再落地）。
  5) **依赖收敛（后置）**：在迁移端口适配器后，逐步让 `bc-messaging` 的编译期依赖从 `eva-infra` 收敛为更小的 `eva-infra-dal` + `eva-infra-shared`（或完全无 `eva-infra` 依赖），并保持 `start` 装配不变。

- 下一步建议（从下一会话起；每步 1 次最小回归 + 1 次提交 + 文档同步；保持行为不变）：
		  - 结构性里程碑 S0（需求变更，2025-12-24）：将“BC=一个顶层聚合模块、内部 `domain/application/infrastructure` 为子模块”的结构落地到真实目录与 Maven 结构中，并把历史平铺过渡模块（`bc-iam-infra`、`bc-evaluation-infra` 等）折叠归位到对应 BC 内部子模块（每步可回滚；保持行为不变）。
			  - ✅ 进展（2025-12-25）：已以 `bc-iam` 为试点落地 `bc-iam-parent` + `domain/application/infrastructure` 子模块，并折叠归位 `bc-iam-infra` 到 `bc-iam/infrastructure`（artifactId/包名保持不变；最小回归通过；落地提交：`0b5c5383`）。
			  - ✅ 进展（2025-12-27）：已以 `bc-template` 为试点落地 `bc-template-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-template`；包名不变；最小回归通过；落地提交：`65091516`）。
				  - ✅ 进展（2025-12-27）：已以 `bc-course` 为试点落地 `bc-course-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-course`；包名不变；最小回归通过；落地提交：`e90ad03b`）。
				  - ✅ 进展（2025-12-27）：已以 `bc-ai-report` 为试点完成 S0 阶段 1：引入 `bc-ai-report-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-ai-report`；保持 `package` 不变；最小回归通过；落地提交：`e14f4f7a`）。
				  - ✅ 进展（2025-12-27）：已完成 S0 阶段 2：将 AI 报告端口适配器/导出实现/AI 基础设施搬运到 `bc-ai-report/infrastructure` 子模块，并补齐 `eva-app` → `bc-ai-report-infra` 依赖以保证装配（保持行为不变；最小回归通过；落地提交：`444c7aca`）。
					  - ✅ 进展（2025-12-27）：已以 `bc-audit` 为试点完成 S0 阶段 1：引入 `bc-audit-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-audit`；保持 `package` 不变；最小回归通过；落地提交：`81594308`）。
					  - ✅ 进展（2025-12-27）：已完成 `bc-audit` 的 S0 阶段 2：将 `edu.cuit.infra.bcaudit.adapter.LogInsertionPortImpl` 从 `eva-infra` 搬运到 `bc-audit/infrastructure` 子模块，并补齐 `eva-app` → `bc-audit-infra` 依赖以保证装配（保持行为不变；最小回归通过；落地提交：`d7858d7a`）。
					  - ✅ 进展（2025-12-27）：已完成 `bc-audit` 的 S0 阶段 3（可选）：抽离 `sys_log` 相关 DAL（`SysLog*DO/Mapper/XML`）到 `eva-infra-dal`、抽离 `LogConverter` 到 `eva-infra-shared`，并移除 `bc-audit-infra` → `eva-infra` 的过渡依赖（保持行为不变；最小回归通过；落地提交：`06ec6f3d`）。
						  - 下一步建议（仍保持行为不变，可选）：在确认装配稳定后，继续推进评教读侧解耦（方向 A → B）：
									  - A（继续收窄依赖）：✅ 已证伪“`eva-app` 仍注入记录聚合端口 `EvaRecordQueryPort`”（除端口定义与实现外无引用）。后续若出现新的应用层引用点（尤其是导出装饰器/扩展点），再按“先补齐子端口组合（仅新增接口 + `extends`）→ 再收窄注入类型”的同套路处理（每次只改 1 个类 + 1 个可运行单测）。
										  - ✅ 进展（2026-01-01，记录读侧依赖收窄）：`EvaRecordServiceImpl.pageEvaRecord` 内联分页结果组装，移除对 `PaginationBizConvertor` 的注入依赖（分页字段赋值顺序/异常文案/循环副作用顺序不变；保持行为不变；最小回归通过；落地提交：`55103de1`）。
										  - ✅ 进展（2026-01-01，记录读侧用例归位深化/D1）：新增 `EvaRecordQueryUseCase`，并将 `EvaRecordServiceImpl.pageEvaRecord` 退化为纯委托壳，把“实体→CO 组装 + 平均分填充 + 分页组装”的编排逻辑归位到 UseCase（保持 `@CheckSemId` 触发点不变；异常文案/副作用顺序不变；保持行为不变；最小回归通过；落地提交：`86772f59`）。
										  - ✅ 进展（2026-01-01，记录读侧用例归位深化/D1）：对齐 `EvaRecordQueryUseCase` 内部“实体→CO 组装”的求值顺序，避免提前触发 `Supplier` 缓存加载导致副作用顺序漂移（保持行为不变；最小回归通过；落地提交：`10991314`）。
										  - ✅ 进展（2026-01-02，用户评教记录读侧用例归位深化/D1）：新增 `UserEvaQueryUseCase`，并将旧入口 `UserEvaServiceImpl.getEvaLogInfo/getEvaLoggingInfo` 退化为纯委托壳（保留 `@CheckSemId` 与当前用户解析：`StpUtil` + `userQueryGateway`；异常文案/副作用顺序不变；保持行为不变；最小回归通过；落地提交：`96e65019`）。
										  - ✅ 进展（2025-12-28，统计读侧用例归位深化）：将 `EvaStatisticsServiceImpl.getTargetAmountUnqualifiedUser` 的 `type` 分支选择与阈值选择归位到 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUser`（保持 `@CheckSemId` 触发点仍在旧入口；异常文案与副作用顺序不变；保持行为不变；最小回归通过；落地提交：`5b20d44e`）。
										  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：在 `EvaStatisticsQueryUseCase` 新增 `evaScoreStatisticsInfoOrEmpty`，将 “`Optional.empty` → `new EvaScoreInfoCO()`” 的空对象兜底先归位到用例层（保持行为不变；为下一步旧入口退化为纯委托壳做准备；最小回归通过；落地提交：`bce01df2`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：将旧入口 `EvaStatisticsServiceImpl.evaScoreStatisticsInfo` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.evaScoreStatisticsInfoOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`1bf3a4fe`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：在 `EvaStatisticsQueryUseCase` 新增 `evaTemplateSituationOrEmpty`，将 “`Optional.empty` → `new EvaSituationCO()`” 的空对象兜底先归位到用例层（保持行为不变；为下一步旧入口退化为纯委托壳做准备；最小回归通过；落地提交：`89b6b1ee`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：将旧入口 `EvaStatisticsServiceImpl.evaTemplateSituation` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.evaTemplateSituationOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`78abf1a1`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：在 `EvaStatisticsQueryUseCase` 新增 `evaWeekAddOrEmpty`，将 “`Optional.empty` → `new EvaWeekAddCO()`” 的空对象兜底先归位到用例层（保持行为不变；为下一步旧入口退化为纯委托壳做准备；最小回归通过；落地提交：`5a8ac076`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：将旧入口 `EvaStatisticsServiceImpl.evaWeekAdd` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.evaWeekAddOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`2a92ca0b`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：在 `EvaStatisticsQueryUseCase` 新增 `getEvaDataOrEmpty`，将 “`Optional.empty` → `new PastTimeEvaDetailCO()`” 的空对象兜底先归位到用例层（保持行为不变；为下一步旧入口退化为纯委托壳做准备；最小回归通过；落地提交：`1180a0f7`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：为 `EvaStatisticsQueryUseCase.getEvaDataOrEmpty` 补齐 `Optional.empty` 时返回空对象的读侧用例级测试，并保留阈值读取顺序与委托顺序验证（保持行为不变；最小回归通过；落地提交：`a44a9bba`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：将旧入口 `EvaStatisticsServiceImpl.getEvaData` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.getEvaDataOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`b59db93d`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：在 `EvaStatisticsQueryUseCase` 新增 `getTargetAmountUnqualifiedUserOrEmpty`，将 “`Optional.empty` → `new UnqualifiedUserResultCO().setTotal(0).setDataArr(List.of())`” 的空对象兜底先归位到用例层（保持行为不变；为下一步旧入口退化为纯委托壳做准备；最小回归通过；落地提交：`0ac65fb4`）。
									  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：将旧入口 `EvaStatisticsServiceImpl.getTargetAmountUnqualifiedUser` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUserOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`b931b247`）。
										  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：为 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUserOrEmpty(type=1)` 补齐空结果兜底的读侧用例级测试（保持行为不变；最小回归通过；落地提交：`8f3e7afe`）。
										  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：为 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUserOrEmpty(invalidType)` 补齐异常文案不变的读侧用例级测试，并确保不触发查询端口调用（保持行为不变；最小回归通过；落地提交：`0cb2caec`）。
										  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：将旧入口 `EvaStatisticsServiceImpl.pageUnqualifiedUser` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.pageUnqualifiedUserAsPaginationQueryResult`，并移除对 `PaginationBizConvertor` 的依赖，从而把分页结果组装彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`f4f3fcde`）。
											  - ✅ 进展（2025-12-29，统计读侧用例归位深化）：在 `EvaStatisticsQueryUseCase` 新增 `pageUnqualifiedUserAsPaginationQueryResult`，把“`PaginationResultEntity` → `PaginationQueryResultCO`”的分页结果组装逻辑先归位到用例层，为下一步旧入口 `EvaStatisticsServiceImpl.pageUnqualifiedUser` 退化为纯委托壳并去除 `PaginationBizConvertor` 依赖做准备（保持行为不变；最小回归通过；落地提交：`e97615e1`）。
											  - 建议切分路径（仍保持行为不变，可回滚）：统计用例归位遵循“每次只迁 1 个方法簇”的节奏；`pageUnqualifiedUser`（已完成）→ `getTargetAmountUnqualifiedUser`（已完成）→ ✅ `getTargetAmountUnqualifiedUser` 的空对象兜底归位到 UseCase（补齐重载：`0ac65fb4`；旧入口委托：`b931b247`）→ ✅ `getEvaData` 的阈值计算/参数组装归位（已完成；落地：`8f4c07c5`）→ ✅ `getEvaData` 的空对象兜底归位到 UseCase（补齐重载：`1180a0f7`；旧入口委托：`b59db93d`）→ ✅ 补齐 unqualifiedUser 的“参数组装重载”（读取 `EvaConfigGateway.getEvaConfig()`；落地：`0a2fec4d`）→ ✅ 旧入口 `EvaStatisticsServiceImpl` 委托重载并去除对 `EvaConfigGateway` 的直接依赖（已完成；落地：`21f6ad5b`）→ ✅ `evaScoreStatisticsInfo` 的空对象兜底归位到 UseCase（补齐重载：`bce01df2`；旧入口委托：`1bf3a4fe`）→ ✅ `evaTemplateSituation` 的空对象兜底归位到 UseCase（补齐重载：`89b6b1ee`；旧入口委托：`78abf1a1`）→ ✅ `evaWeekAdd` 的空对象兜底归位到 UseCase（补齐重载：`5a8ac076`；旧入口委托：`2a92ca0b`）→ 下一步继续推进 B：将统计读侧其它方法簇的“默认值兜底/空对象组装”逐步归位到 `EvaStatisticsQueryUseCase`（每次只迁 1 个方法；旧入口仍保留 `@CheckSemId` 触发点不变）。
										  - 下一步建议（保持行为不变，每次只改 1 个类 + 1 个可运行回归）：可选低风险收尾：将 `EvaStatisticsServiceImpl.exportEvaStatistics` 也退化为委托壳，改为调用 `EvaStatisticsQueryUseCase` 新增的导出用例方法（内部仍调用 `EvaStatisticsExcelFactory.createExcelData(semId)`；保持 `@CheckSemId` 触发点不变）。
								  - ✅ 进展（2025-12-28，记录读侧依赖类型收窄—AI 报告分析）：将 AI 报告分析端口适配器 `AiReportAnalysisPortImpl` 对记录端口的依赖类型从聚合接口 `EvaRecordQueryPort` 收窄为子端口 `EvaRecordExportQueryPort`（保持行为不变；最小回归通过；落地提交：`4fe38934`）。
							  - 工具提示：若 Serena MCP 工具调用持续 `TimeoutError`，需要在交接文档中记录降级原因，并用本地 `rg` 提供可复现的引用证据（仍不改变业务语义）；同时在下一会话优先排查恢复 Serena，以回到“符号级引用分析”流程。
					  - 结构性里程碑 S0.1（需求变更，2025-12-24）：逐步拆解 `eva-client`：按 BC 归属迁移 BO/CO/DTO（允许改包名以归位到 BC 的 `application/contract/dto`）；新增对象不再进入 `eva-client`；跨 BC 通用对象沉淀到 shared-kernel（每步可回滚；保持行为不变）。
				    - ✅ 进展（2025-12-25）：已在 `bc-iam` 下新增 `bc-iam-contract` 子模块，并将 IAM 协议对象（`api/user/*` + `dto/cmd/user/*`）从 `eva-client` 迁移到 `bc-iam/contract`（包名归位到 `edu.cuit.bc.iam.application.contract...`；全仓库引用已更新；最小回归通过；落地提交：`dc3727fa`）。
		    - 下一步（建议顺序，仍按“每步=最小回归+提交+三文档同步”）：
		      1) ✅ 以 `bc-evaluation` 作为 S0 试点：折叠 `bc-evaluation-infra` → `bc-evaluation/infrastructure`，落地 `bc-evaluation-parent` + `domain/application/infrastructure`（已完成；落地提交：`4db04d1c`）。
			      2) 在 `bc-iam-contract` 继续迁移 IAM 专属协议对象：优先 `dto/clientobject/user/*` 与 IAM 查询条件。
				         - 进展：已迁移 `UserInfoCO/UserDetailCO/RoleInfoCO/SimpleRoleInfoCO/MenuCO/GenericMenuSectionCO/RouterDetailCO/RouterMeta` 到 `edu.cuit.bc.iam.application.contract.dto.clientobject.user`（保持行为不变；落地提交：`c1a51199`）。
				         - 进展：已迁移 IAM 查询条件 `MenuConditionalQuery` 到 `bc-iam-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`1eda37c9`）。
				         - 进展（2025-12-26）：已将 IAM 专属接口 `IDepartmentService` 从 `eva-client` 迁移到 `bc-iam-contract`（包名归位到 `edu.cuit.bc.iam.application.contract.api.department`；保持行为不变；最小回归通过；落地提交：`656dc36e`）。
				         - 进展：已在“可证实不再需要”的前提下移除 `bc-iam/application` → `eva-client` 直依赖（保持行为不变；最小回归通过；落地提交：`7371ab96`）。
							         - ✅ 进展（2025-12-27）：已对 `eva-domain` 仍存在的 `import edu.cuit.client.*` 做“来源证伪”（Serena 盘点清单 → 逐项确认类型定义文件实际归属模块；包名保持不变）。结论：`eva-client` 不再提供业务类型；`eva-domain` 的 `edu.cuit.client.*` 引用类型分别由 `shared-kernel` / 各 BC contract / `bc-course` / `eva-domain` 自身承载（保持行为不变）。
				         - 进展（2025-12-26）：已将课程写侧命令 `edu.cuit.client.dto.cmd.course/*` 与导入课表 BO `CourseExcelBO` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变），并为 `eva-infra-shared` 增加 `bc-course` 显式依赖以闭合编译依赖（保持行为不变；最小回归通过；落地提交：`8a591703`）。
				         - 进展（2025-12-26）：已将学期协议接口 `ISemesterService` 与学期 CO `SemesterCO` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`7b5997c1`）。
				         - 进展（2025-12-26）：已将通用学期入参 `Term` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`f401dcb9`）。
				         - 进展（2025-12-26）：已将课程查询 Query 对象 `CourseQuery/CourseConditionalQuery/MobileCourseQuery` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`84a6a536`）。
				         - 进展（2025-12-27）：已将教室接口 `IClassroomService` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`59471a96`）。
				         - 进展（2025-12-27）：已将 AI 接口与 BO（`IAiCourseAnalysisService/AiAnalysisBO/AiCourseSuggestionBO`）从 `eva-client` 迁移到 `bc-ai-report`，并移除 `bc-ai-report` → `eva-client` 依赖（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`badb9db6`）。
				         - 进展（2025-12-27）：已将 `EvaProp` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`4feabdd0`）。
				         - 进展（2025-12-26）：已移除 `eva-domain` → `eva-client` 的 Maven **直依赖**（保持行为不变；最小回归通过；落地提交：`9ff21249`）。
				         - 进展（2025-12-26）：已将消息入参 DTO `GenericRequestMsg` 从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`8fc7db99`）。
				         - 进展（2025-12-26）：已将课程数据对象 `CoursePeriod/CourseType` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`5629bd2a`）。
				         - 进展（2025-12-26）：已将课程 CO `SingleCourseCO` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`ccc82092`）。
				         - 进展（2025-12-26）：已将课程域 `clientobject/course` 残留 CO（`ModifySingleCourseDetailCO/RecommendCourseCO/SelfTeachCourseCO/SelfTeachCourseTimeCO/SelfTeachCourseTimeInfoCO/SubjectCO/TeacherInfoCO`）以及 `SimpleCourseResultCO/SimpleSubjectResultCO` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`ce1a0a90`）。
				         - 进展（2025-12-26）：已将消息域 response DTO（`GenericResponseMsg/EvaResponseMsg`）从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`ecb8cee5`）。
				         - 进展（2025-12-26）：已将消息域 `IMsgService/SendMessageCmd/MessageBO` 从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`431a5a23`）。
				         - 进展（2025-12-26）：已将 `SendWarningMsgCmd` 从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`e6aa5913`）。
				         - 进展（2025-12-26）：已将消息域 clientobject `EvaMsgCO` 从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`2f257a86`）。
						         - ✅ 进展（2025-12-27）：已在完成来源证伪的前提下选择方案 B，并从 root reactor 移除 `eva-client` 模块（保持行为不变；最小回归通过；落地提交：`ce07d75f`）；并进一步从仓库移除 `eva-client/` 目录（保持行为不变；落地提交：`de25e9fb`）。
			      3) 在 `bc-evaluation/contract` 继续迁移评教域协议对象：优先 `edu.cuit.client.dto.clientobject.eva.*` 与 `UnqualifiedUserConditionalQuery`（建议先保持 `package` 不变以降风险；保持行为不变）。
			         - 进展：已将评教统计接口 `IEvaStatisticsService` + `UnqualifiedUserInfoCO/UnqualifiedUserResultCO` 从 `eva-client` 迁移到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；落地提交：`978e3535`）。
			         - 进展：已将 `edu.cuit.client.dto.clientobject.eva.*` + `UnqualifiedUserConditionalQuery` 从 `eva-client` 迁移到 `bc-evaluation/contract`（保持 `package` 不变）；并将评教 API 接口（`IEvaConfigService/IEvaRecordService/IEvaTaskService/IEvaTemplateService/IUserEvaService`）迁移到 `bc-evaluation/contract`（保持 `package` 不变）；同时将课程域协议接口与 CO（`edu.cuit.client.api.course.*`、`CourseDetailCO/CourseModelCO/SingleCourseDetailCO`）从 `eva-client` 迁移到 `bc-course`，避免 `eva-client` 反向依赖评教 CO（保持行为不变；最小回归通过；落地提交：`6eb0125d`）。
				         - 进展：已将评教查询条件 `EvaTaskConditionalQuery/EvaLogConditionalQuery` 从 `eva-client` 迁移到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`d02d5522`）。
				         - 进展：已将评教 `dto/cmd/eva/*` 从 `eva-client` 迁移到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`2273ad61`）。
				         - 进展：已将 `EvaConfig` 从 `eva-client` 迁移到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`438d38bf`）。
				         - 进展：已将 `DateEvaNumCO/TimeEvaNumCO/MoreDateEvaNumCO/SimpleEvaPercentCO/SimplePercentCO/FormPropCO` 从 `eva-client` 迁移到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`c2d8a8b1`）。
				         - 进展：已将课程时间模型 `dto/data/course/CourseTime` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`5f21b5ce`）。
				         - 进展：已在“可证实不再需要”的前提下移除 `bc-evaluation-contract` → `eva-client` 直依赖（保持行为不变；最小回归通过；落地提交：`cf2001ef`）。
				         - 进展：已在“可证实不再需要”的前提下移除 `bc-evaluation/application` → `eva-client` 直依赖（保持行为不变；最小回归通过；落地提交：`10e8eb0b`）。
				         - 进展（2025-12-26）：Serena 盘点确认 `eva-client` 下评教专属目录（`api/eva`、`dto/cmd/eva`、`dto/clientobject/eva`）已迁空（盘点闭环；最小回归通过；落地提交：`e643bac9`）。
				         - 下一步（建议顺序，保持行为不变）：继续迁移评教域仍留在 `eva-client` 的协议对象（P1.2 继续推进；建议优先保持 `package` 不变以降风险）。若评教专属目录已迁空，则继续清理评教 BC 其它子模块（如 `bc-evaluation/domain` / `bc-evaluation/infrastructure`）对 `eva-client` 的直依赖：先用 Serena 盘点引用面，再逐步移除 Maven 依赖（每步最小回归+提交+三文档同步）。
				      4) 在 `bc-audit` 继续迁移审计日志协议对象（保持行为不变）：
				         - 进展：已将审计日志协议对象 `ILogService/OperateLogCO/LogModuleCO` 从 `eva-client` 迁移到 `bc-audit`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`e1dbf2d4`）。
				         - 进展（2025-12-26）：已用 Serena 盘点确认 `bc-audit` 对 `eva-client` 的实际依赖面仅剩 `SysLogBO`（`PagingQuery/GenericConditionalQuery/PaginationQueryResultCO` 已在 `shared-kernel`，`ILogService/OperateLogCO/LogModuleCO` 已在 `bc-audit`）。
				         - 进展（2025-12-26）：已将 `SysLogBO` 从 `eva-client` 迁移到 `eva-domain`（保持 `package edu.cuit.client.bo` 不变；保持行为不变），为移除 `bc-audit` → `eva-client` 直依赖做准备。
				         - 进展（2025-12-26）：已移除 `bc-audit` → `eva-client` 的 Maven **直依赖**（保持行为不变；最小回归通过）。
				         - 备注：P1.2-3 已闭环（本条后续仅做“进一步削减依赖路径”的可选推进）。
				         - 下一步：继续评估 `eva-domain`（以及 `eva-infra` 侧）对 `eva-client` 的残留依赖是否仅为已迁移到 `shared-kernel` 的通用类型；若“可证实不再需要”，再逐步削减对 `eva-client` 的依赖路径（保持行为不变；每步=最小回归+提交+三文档同步）。
				      5) 对 `PagingQuery/GenericConditionalQuery/SimpleResultCO/PaginationQueryResultCO` 等通用对象先用 Serena 盘点引用范围：跨 BC 则沉淀 shared-kernel，避免误迁到某个 BC。
				         - 进展：已新增 `shared-kernel` 子模块，并将 `PagingQuery/ConditionalQuery/GenericConditionalQuery/SimpleResultCO/PaginationQueryResultCO` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`a25815b2`）。
				         - 进展：`bc-iam-contract` / `bc-evaluation-contract` 已增加对 `shared-kernel` 的直依赖（暂保留 `eva-client` 以可回滚；最小回归通过；落地提交：`3a0ac086`）。
				         - 进展：已将 `ValidStatus/ValidStatusValidator` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`686de369`）。
				         - 进展：`bc-iam-contract` 已去除对 `eva-client` 的直依赖（保持行为不变；最小回归通过；落地提交：`8d673c17`）。
				         - 下一步：逐步让各 BC 的 `contract` 在“可证实不再需要”的前提下分阶段削减对 `eva-client` 的依赖范围（保持行为不变）。
  - ✅ 提交点 0（纯文档闭环）：补齐“条目 25”的定义/边界与验收口径（只改文档，不改代码；落地提交：`1adc80bd`），避免新会话对 24/25/26 的分界理解不一致
  - ✅ 提交点 A：启动 `bc-ai-report` / `bc-audit` 最小骨架并接入组合根（仅落点，不迁业务语义；落地提交：`a30a1ff9`）
  - ✅ 提交点 B：为 AI 报告或审计日志挑选 1 条写链路，按“用例 + 端口 + 适配器 + 旧 gateway 委托壳”收敛（审计日志写入：`LogGatewayImpl.insertLog`；保持行为不变；落地提交：`b0b72263`）
  - ✅ 提交点 C（统计主题，第一步）：读侧拆分 `EvaQueryRepo` 的统计接口为 `EvaStatisticsQueryRepo`，并让 `EvaStatisticsQueryPortImpl` 依赖收敛（统计口径不变；落地提交：`d5b07247`）
  - ✅ 提交点 C2（记录主题）：读侧拆分 `EvaQueryRepo` 的记录接口为 `EvaRecordQueryRepo`，并让 `EvaRecordQueryPortImpl` 依赖收敛（口径/异常文案不变；落地提交：`cae1a15c`）
  - ✅ 提交点 C3（任务主题）：读侧拆分 `EvaQueryRepo` 的任务接口为 `EvaTaskQueryRepo`，并让 `EvaTaskQueryPortImpl` 依赖收敛（口径/异常文案不变；落地提交：`82427967`）
  - ✅ 提交点 C4（模板主题）：读侧拆分 `EvaQueryRepo` 的模板接口为 `EvaTemplateQueryRepo`，并让 `EvaTemplateQueryPortImpl` 依赖收敛（口径/异常文案不变；落地提交：`889ec9b0`）
  - ✅ 提交点 B2（AI 报告写链路，导出）：`AiCourseAnalysisService.exportDocData` 收敛为“用例 + 端口 + 端口适配器 + 旧入口委托壳”（日志/异常文案不变；落地提交：`c68b3174`）
  - ✅ 提交点 B3（AI 报告写链路，导出）：`AiCourseAnalysisService.exportDocData` 进一步退化为“纯委托壳”（日志/异常文案不变；落地提交：`7f4b3358`）
  - ✅ 提交点 B4（AI 报告写链路，analysis）：`AiCourseAnalysisService.analysis` 收敛为“用例 + 端口 + 端口适配器 + 旧入口委托壳”（保持 `@CheckSemId` 切面触发点不变；日志/异常文案不变；落地提交：`a8150e7f`）
  - ✅ 提交点 B5（AI 报告写链路，用户名解析）：`ExportAiReportDocByUsernameUseCase` 将 username → userId 查询抽为 `bc-ai-report` 端口 + `eva-app` 端口适配器（异常文案与日志顺序保持不变；落地提交：`d7df8657`）
  - ✅ 提交点 C5-1（统计主题实现拆分）：新增 `EvaStatisticsQueryRepository` 承接 `EvaStatisticsQueryRepo` 实现，`EvaQueryRepository` 的统计方法退化为委托（口径/异常文案不变；落地提交：`9e0a8d28`）
  - ✅ 提交点 C5-2（记录主题实现拆分）：新增 `EvaRecordQueryRepository` 承接 `EvaRecordQueryRepo` 实现，`EvaQueryRepository` 的记录方法退化为委托（口径/异常文案不变；落地提交：`985f7802`）
  - ✅ 提交点 C5-3（任务主题实现拆分）：新增 `EvaTaskQueryRepository` 承接 `EvaTaskQueryRepo` 实现，`EvaQueryRepository` 的任务方法退化为委托（口径/异常文案不变；落地提交：`d467c65e`）
  - ✅ 提交点 C5-4（模板主题实现拆分）：新增 `EvaTemplateQueryRepository` 承接 `EvaTemplateQueryRepo` 实现，`EvaQueryRepository` 的模板方法退化为委托（口径/异常文案不变；落地提交：`a550675a`）
  - ✅ 提交点 D1（`bc-evaluation-infra` 阶段 1：评教读侧查询迁移 + DAL/shared 拆分）：引入 `bc-evaluation-infra` 并迁移评教读侧查询（QueryPortImpl + QueryRepo/Repository），同时将 course/eva DAL（DO/Mapper/XML）迁移到 `eva-infra-dal`、将 `CourseConvertor`/`EvaConvertor`/`EvaCacheConstants`/`CourseFormat` 迁移到 `eva-infra-shared`（保持包名不变；保持行为不变；落地提交：`be6dc05c`）
- ✅ 提交点 D2（`bc-evaluation-infra` 阶段 2：评教写侧端口适配器迁移）：迁移 `eva-infra/src/main/java/edu/cuit/infra/bcevaluation/repository/*` 到 `bc-evaluation-infra`；并将 `CalculateClassTime` 迁移到 `eva-infra-shared` 以保持 `bc-evaluation-infra` 不依赖 `eva-infra`（保持包名/行为不变；落地提交：`24e7f6c9`）
- ✅ 提交点 C-1（后续，可选，读侧门面加固）：清理 `EvaQueryRepository` 中已无引用的历史私有实现/冗余依赖，使其成为纯委托壳（保持口径/异常文案不变；落地提交：`73fc6c14`；三文档同步：`083b5807`）
- ✅ 提交点 C-2（后续，可选，读侧仓储瘦身）：继续盘点评教读侧四主题仓储（`bc-evaluation/infrastructure/src/main/java/edu/cuit/infra/bcevaluation/query/*QueryRepository.java`）残留的历史私有实现/冗余依赖，按“每次只删可证实无引用代码”的原则逐步清理（仍保持口径/异常文案不变；每步最小回归+提交+三文档同步；进展：已完成 C-2-1：`e2a2a717`；已完成 C-2-2：`8b76375f`；已完成 C-2-3：`4a317344`；已完成 C-2-4：`dba6e31d`；已完成 C-2-5：`5c1a03bc`；结论：四主题仓储未发现可证实无引用项，因此关闭 C-2）
- 提交点 C（后续，可选，剩余）：在 C5-4 完成后，视情况继续盘点评教读侧 query 实现残留私有工具/实体组装并内聚到对应主题仓储（仍保持口径/异常文案不变）

#### 条目 25（定义 / 边界 / 验收口径）

> 定位：条目 25 是“AI 报告 / 审计日志模块化试点”的会话推进索引，用于承接提交点 A/B（而提交点 C 属于读侧拆分，不属于条目 25 的范围）。

- **定义**：AI 报告 / 审计日志模块化试点（目标是提供 BC 落点与 wiring，并按既有套路先收敛 1 条写链路，行为保持不变）。
- **边界**：
  - **包含**：提交点 A + 提交点 B。
  - **不包含**：提交点 C；以及任何业务语义调整（缓存/日志/异常文案/副作用顺序完全不变）。
- **验收口径**：
  1) 组合根 wiring 完整，不引入 Maven 循环依赖；  
  2) 提交点 B 所选链路满足“UseCase + Port + Port Adapter + 旧 gateway 委托壳”的收敛套路；  
  3) 最小回归通过（命令以 `NEXT_SESSION_HANDOFF.md` 0.10 为准）。

- IAM 域：开始引入 `bc-iam`，已收敛 `UserUpdateGatewayImpl.assignRole/createUser/updateInfo/updateStatus/deleteUser`（落地提交：`16ff60b6/b65d311f/a707ab86`、`c3aa8739/a3232b78/a26e01b3/9e7d46dd`、`38c31541/6ce61024/db0fd6a3/cb789e21`、`e3fcdbf0/8e82e01f/eb54e13e`、`5f08151c/e23c810a/cccd75a3/2846c689`，保持行为不变）。
- 系统管理读侧：`UserQueryGatewayImpl.fileUserEntity` 与基础查询能力（`findIdByUsername/findUsernameById/getUserStatus/isUsernameExist`）已收敛到 `bc-iam`（保持行为不变；落地提交：`3e6f2cb2/8c245098/92a9beb3`、`9f664229/38384628/de662d1c/8a74faf5`）。
- 系统管理读侧：`UserQueryGatewayImpl.findAllUserId/findAllUsername/allUser/getUserRoleIds` 已收敛到 `bc-iam`（保持行为不变；落地提交：`56bbafcf/7e5f0a74/bc5fb3c6/6a1332b0`）。
- 系统管理写侧：角色写侧剩余入口已收敛到 `bc-iam`（保持行为不变；落地提交：`64fadb20`）。
- ✅ 中期里程碑（已完成）：`bc-iam-infra` 子模块骨架已接入组合根并完成 `bciam/adapter/*` 迁移，且阶段 2（IAM DAL 抽离 + shared 拆分 + 去依赖）已闭环（保持行为不变；阶段 1 落地：`42a6f66f/070068ec/03ceb685/02b3e8aa/6b9d2ce7/5aecc747/1c3d4b8c`；阶段 2 去依赖落地：`2ad911ea`）。
  - 阶段 2.1（已完成）：已用 Serena 盘点 `bc-iam-infra` 适配器对 IAM DAL 的直接依赖清单（后续迁移以此清单为准，避免漏搬/多搬）：
    - Mapper：`SysUserMapper`、`SysUserRoleMapper`、`SysRoleMapper`、`SysRoleMenuMapper`、`SysMenuMapper`
    - DO：`SysUserDO`、`SysUserRoleDO`、`SysRoleDO`、`SysRoleMenuDO`、`SysMenuDO`
    - XML：`eva-infra/src/main/resources/mapper/user/SysUserMapper.xml`、`SysUserRoleMapper.xml`、`SysRoleMapper.xml`、`SysRoleMenuMapper.xml`、`SysMenuMapper.xml`
  - 阶段 2.2（已完成）：已在 `bc-iam-infra` 创建 DAL 包路径与资源目录骨架（不迁代码，仅提供后续迁移落点；保持行为不变）：
    - Java：`bc-iam-infra/src/main/java/edu/cuit/infra/dal/database/dataobject/user/package-info.java`、`bc-iam-infra/src/main/java/edu/cuit/infra/dal/database/mapper/user/package-info.java`
    - Resources：`bc-iam-infra/src/main/resources/mapper/user/.gitkeep`
  - 阶段 2.3（已完成）：新增共享 DAL 子模块 `eva-infra-dal`，并先迁移 `SysUser*`（DO/Mapper/XML）到该模块（保持包名/namespace/SQL 不变；保持行为不变）。
    - 说明：Serena 引用分析确认 `SysUserMapper` 被 `eva-infra` 内多个模块直接使用；若直接迁入 `bc-iam-infra` 并从 `eva-infra` 删除会引入 Maven 循环依赖风险，因此先抽离为共享 DAL 模块，以最小可回滚方式推进。
    - 新模块：`eva-infra-dal/pom.xml`
    - Java：`eva-infra-dal/src/main/java/edu/cuit/infra/dal/database/dataobject/user/SysUserDO.java`、`SysUserRoleDO.java`；`eva-infra-dal/src/main/java/edu/cuit/infra/dal/database/mapper/user/SysUserMapper.java`、`SysUserRoleMapper.java`
    - XML：`eva-infra-dal/src/main/resources/mapper/user/SysUserMapper.xml`、`SysUserRoleMapper.xml`
  - 阶段 2.4（已完成）：继续迁移 `SysRole*`/`SysRoleMenu*`（DO/Mapper/XML）到共享模块 `eva-infra-dal`（保持包名/namespace/SQL 不变；保持行为不变）。
    - Java：`eva-infra-dal/src/main/java/edu/cuit/infra/dal/database/dataobject/user/SysRoleDO.java`、`SysRoleMenuDO.java`；`eva-infra-dal/src/main/java/edu/cuit/infra/dal/database/mapper/user/SysRoleMapper.java`、`SysRoleMenuMapper.java`
    - XML：`eva-infra-dal/src/main/resources/mapper/user/SysRoleMapper.xml`、`SysRoleMenuMapper.xml`
  - 阶段 2.5（已完成）：继续迁移 `SysMenu*`（DO/Mapper/XML）到共享模块 `eva-infra-dal`（保持包名/namespace/SQL 不变；保持行为不变）。
    - Java：`eva-infra-dal/src/main/java/edu/cuit/infra/dal/database/dataobject/user/SysMenuDO.java`；`eva-infra-dal/src/main/java/edu/cuit/infra/dal/database/mapper/user/SysMenuMapper.java`
    - XML：`eva-infra-dal/src/main/resources/mapper/user/SysMenuMapper.xml`
  - 阶段 2.6（已完成）：盘点 `bc-iam-infra` 仍依赖 `eva-infra` 的类型清单（为后续移除依赖做最小闭包拆分输入；保持行为不变）：
    - 转换器：`edu.cuit.infra.convertor.PaginationConverter`（已迁至 `eva-infra-shared`：`54d5fecd`）、`edu.cuit.infra.convertor.user.{MenuConvertor,RoleConverter,UserConverter}`（已迁至 `eva-infra-shared`：`6c798f1b`）、`edu.cuit.infra.convertor.user.LdapUserConvertor`（已迁至 `eva-infra-shared`：`0dc0ddc8`）
    - 缓存常量：`edu.cuit.infra.enums.cache.{UserCacheConstants,CourseCacheConstants}`
    - LDAP：`edu.cuit.infra.dal.ldap.{dataobject,repo}.*`（已迁至 `eva-infra-shared`：`aca70b8b`）、`edu.cuit.infra.util.EvaLdapUtils` + `edu.cuit.infra.{enums.LdapConstants,property.EvaLdapProperties}`（已迁至 `eva-infra-shared`：`3165180c`）
    - 工具：`edu.cuit.infra.util.QueryUtils`
  - 阶段 2.7（已完成）：新增 shared 子模块骨架 `eva-infra-shared`（不迁代码，仅作为后续从 `eva-infra` 抽离 Converter/LDAP/缓存常量/工具等的落点；保持行为不变）。
    - 新模块：`eva-infra-shared/pom.xml`
  - 阶段 2.8（已完成）：迁移 IAM 相关缓存常量到 `eva-infra-shared`（保持包名不变；保持行为不变）。
    - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/enums/cache/UserCacheConstants.java`、`CourseCacheConstants.java`
    - 依赖：`eva-infra-shared/pom.xml` 增加 `spring-context`（保留 `@Component`）；`eva-infra/pom.xml` 与 `bc-iam-infra/pom.xml` 增加对 `eva-infra-shared` 的依赖
  - 阶段 2.9（已完成）：迁移查询工具 `QueryUtils` 到 `eva-infra-shared`（保持包名不变；保持行为不变）。
    - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/util/QueryUtils.java`
    - 依赖：`eva-infra-shared/pom.xml` 增加对 `eva-client` 与 `zym-spring-boot-starter-jdbc` 的依赖（历史）；进展（2025-12-27）：已移除 `eva-infra-shared` → `eva-client` 直依赖（保持行为不变；最小回归通过；`9437bb12`）。
  - 阶段 2.10（已完成）：迁移 `EntityFactory` 到 `eva-infra-shared`（保持包名不变；保持行为不变）。
    - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/EntityFactory.java`
    - 依赖：`eva-infra-shared/pom.xml` 增加 `mapstruct-plus-spring-boot-starter`；并增加对 `eva-domain` 的依赖以保留 `hutool SpringUtil` 与 `cola SysException` 的依赖来源
  - 阶段 2.11（已完成）：迁移 `PaginationConverter` 到 `eva-infra-shared`（保持包名不变；保持行为不变；落地提交：`54d5fecd`）。
    - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/PaginationConverter.java`
    - 依赖：`eva-infra-shared/pom.xml` 增加对 `eva-infra-dal` 的依赖以保持编译闭包（行为不变）
  - 阶段 2.12（已完成）：迁移 `convertor.user` 的非 LDAP 转换器到 `eva-infra-shared`（保持包名不变；保持行为不变；落地提交：`6c798f1b`）。
    - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/user/{MenuConvertor,RoleConverter,UserConverter}.java`
  - 阶段 2.13（已完成）：迁移 LDAP DO/Repo 到 `eva-infra-shared`（保持包名不变；保持行为不变；落地提交：`aca70b8b`）。
    - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/dal/ldap/dataobject/{LdapGroupDO,LdapPersonDO}.java`、`eva-infra-shared/src/main/java/edu/cuit/infra/dal/ldap/repo/{LdapGroupRepo,LdapPersonRepo}.java`
    - 依赖：`eva-infra-shared/pom.xml` 增加 `spring-boot-starter-data-ldap`
  - 阶段 2.14（已完成）：迁移 `EvaLdapUtils` 相关类型到 `eva-infra-shared`（保持包名不变；保持行为不变；落地提交：`3165180c`）。
    - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/util/EvaLdapUtils.java`、`eva-infra-shared/src/main/java/edu/cuit/infra/enums/LdapConstants.java`、`eva-infra-shared/src/main/java/edu/cuit/infra/property/EvaLdapProperties.java`
  - 阶段 2.15（已完成）：迁移 `LdapUserConvertor` 到 `eva-infra-shared`（保持包名不变；保持行为不变；落地提交：`0dc0ddc8`）。
    - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/user/LdapUserConvertor.java`
  - 阶段 2.16（已完成）：移除 `bc-iam-infra` 对 `eva-infra` 的 Maven 依赖（补齐 cache/logging 编译依赖；保持行为不变；落地提交：`2ad911ea`）。
- AI 报告 / 审计日志：已启动 `bc-ai-report` / `bc-audit` 骨架并接入组合根；审计日志协议对象（`ILogService/OperateLogCO/LogModuleCO`）已从 `eva-client` 迁移到 `bc-audit`（保持行为不变；落地提交：`e1dbf2d4`）。
- 读侧：`EvaQueryRepo` 仍为大聚合 QueryRepo，需继续拆分。

---

## 11. 当前已落地里程碑（滚动）

> 说明：本节用于让新会话快速判断“已经落地到哪里”，避免重复劳动；详细提交清单以 `NEXT_SESSION_HANDOFF.md` 为准。

- `bc-evaluation`：提交评教写侧已收敛（提交评教后消息清理等副作用事件化，事务提交后触发）。
- `bc-evaluation`：评教任务发布写侧已收敛（用例 + 端口 + `eva-infra` 端口适配器 + 提交后消息事件；落地提交：`8e434fe1/ca69b131/e9043f96`）。
- `bc-evaluation`：评教删除写侧已收敛（用例 + `eva-infra` 端口适配器 + 应用层入口；落地提交：`ea928055/07b65663/05900142`）。
- `bc-template`：课程模板锁定校验已落地（已评教则禁止切换模板）。
- `bc-messaging`：课程操作副作用（通知/撤回评教消息）已事件化并统一在提交后处理。
- `bc-messaging`：消息删除/已读写侧已收敛（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`22cb60eb/dd7483aa`）。
- `bc-messaging`：消息查询读侧已收敛（`queryMsg/queryTargetAmountMsg`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`05a2381b`）。
- `bc-messaging`：消息插入写侧已收敛（`insertMessage`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`8445bc41`）。
- `bc-messaging`：消息展示状态写侧已收敛（`updateMsgDisplay`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`c315fa22`）。
- `eva-infra`：旧 `MsgGatewayImpl` 已全量退化为委托壳（CRUD 能力全部委托到 `bc-messaging`，行为不变）。
- `bc-iam`：用户创建/分配角色写侧已收敛（`createUser/assignRole`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`16ff60b6/b65d311f/a707ab86`、`c3aa8739/a3232b78/a26e01b3/9e7d46dd`）。
- `bc-iam`：用户信息更新写侧已收敛（`updateInfo`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`38c31541/6ce61024/db0fd6a3/cb789e21`）。
- `bc-iam`：用户状态更新写侧已收敛（`updateStatus`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`e3fcdbf0/8e82e01f/eb54e13e`；行为快照与下一步拆分详见 `NEXT_SESSION_HANDOFF.md`，补充提交：`e4b94add`）。
- `bc-iam`：用户删除写侧已收敛（`deleteUser`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`5f08151c/e23c810a/cccd75a3/2846c689`）。
- `bc-iam`：用户查询装配已收敛（`fileUserEntity`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`3e6f2cb2/8c245098/92a9beb3`）。
- `bc-iam`：用户基础查询已收敛（`findIdByUsername/findUsernameById/getUserStatus/isUsernameExist`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`9f664229/38384628/de662d1c/8a74faf5`）。
- `bc-iam`：用户目录查询已收敛（`findAllUserId/findAllUsername/allUser/getUserRoleIds`：用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`56bbafcf/7e5f0a74/bc5fb3c6/6a1332b0`）。
- `bc-course`：多条课程写链路已收敛（导入课表、改课/自助课表、删课、课程类型、课次新增等），旧 gateway 逐步退化为委托壳。
- `bc-course`：课程导入状态查询 `CourseUpdateGatewayImpl.isImported` 已收敛（QueryPort + 用例 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；落地提交：`4ed055a2/495287c8/f3e8e3cc`）。
- `course`：课程读侧已结构化（`CourseQueryGatewayImpl` 退化委托壳 + `CourseQueryRepo` 抽取；落地提交：`ba8f2003`）。
- `evaluation`：评教读侧已结构化（`EvaQueryGatewayImpl` 退化委托壳 + `EvaQueryRepo` 抽取；落地提交：`02f4167d`）。
- `evaluation`：评教读侧进一步解耦第一步（统计/导出查询端口 `EvaStatisticsQueryPort` 已从网关拆分并迁移应用层，行为保持不变）。
- `evaluation`：评教读侧进一步解耦补充（任务查询端口 `EvaTaskQueryPort` 已拆分并迁移应用层，行为保持不变）。
- `evaluation`：评教读侧进一步解耦扩展（记录/模板查询端口 `EvaRecordQueryPort` / `EvaTemplateQueryPort` 已拆分并迁移应用层，行为保持不变）。
- `evaluation`：补齐 `bc-evaluation` 对 `eva-client` / `eva-domain` 的依赖，保障 QueryPort 编译通过。
- `evaluation`：补齐 `bc-evaluation` 内部依赖版本号（`eva-client` / `eva-domain`），保证 Maven 解析一致。
- `evaluation`：修正统计查询端口 DTO 包名，确保编译通过。
- `evaluation`：修正统计查询端口实现 DTO 包名，确保基础设施模块编译通过。
- `evaluation`：旧 `EvaQueryGatewayImpl` 进一步收敛，委托到细分 QueryPort，避免直接依赖 QueryRepo。
- 验证：最小回归用例 `EvaRecordServiceImplTest` / `EvaStatisticsServiceImplTest` 已通过（离线）。
- 规则：下一个会话每个步骤结束都需跑最小回归用例并记录结果。
- `evaluation`：旧 `EvaQueryGateway`/`EvaQueryGatewayImpl` 已移除，避免继续引入旧网关依赖。
- 评教读侧用例级回归测试已补充（固化统计口径；落地提交：`a48cf044`）。
- `start`：回归测试稳定化（去除本地文件/外部服务依赖；落地提交：`daf343ef`）。
- `evaluation`：评教模板新增/修改写侧已收敛到 `bc-evaluation`（新增用例 + 端口 + `eva-infra` 端口适配器，并切换 `eva-app` 入口；落地提交：`ea03dbd3`）。
- `evaluation`：清理旧 `EvaUpdateGatewayImpl.putEvaTemplate` 遗留实现（提交评教写侧入口已在 `bc-evaluation`，避免旧代码回潮；落地提交：`12279f3f`）。
- 冲突校验底层片段已收敛：
  - 教室占用冲突：`ClassroomOccupancyChecker`
  - 时间段重叠：`CourInfTimeOverlapQuery`
