# 会话交接摘要（用于新对话继续）

> 仓库：`/home/lystran/programming/java/web/eva-backend`  
> 当前分支：`ddd`  
> 目标方向：把当前“按技术分层”的单体，逐步重构为“按业务限界上下文分模块”的 **DDD 模块化单体**，并预留低成本拆微服务路径（先拆服务、暂时共享库，小步快跑）。

---

## 0.0 术语约定（用于减少“gateway”历史命名带来的混淆）

> 背景：本项目早期参考 COLA 命名把很多类叫做 `*GatewayImpl`，但其中相当一部分同时承载了“应用入口 + DB 访问 + 副作用（缓存/日志）”，逐步演化成大泥球。
>
> 为了渐进式重构期间沟通一致，本仓库后续文档使用以下约定（不强制立刻改代码命名，但会在文档里严格区分）：

- **旧 gateway（LegacyGateway / 委托壳）**：历史遗留的 `eva-infra/.../*GatewayImpl`，对外接口不变（尤其是缓存注解/切面触发点必须保持），内部逐步退化为“委托到 UseCase”的壳。
- **UseCase（应用层用例）**：放在 `bc-*/application/usecase`，只做“业务用例入口与编排”，不直接依赖 DB。
- **Port（应用层端口）**：放在 `bc-*/application/port`，表达 UseCase 的出站依赖（持久化/外部系统/缓存等）。
- **Port Adapter（基础设施端口适配器）**：实现 Port 并原样搬运旧 DB/副作用流程（保持行为不变）。过渡期可能落在 `eva-infra/.../bc*/adapter` 或历史过渡模块 `bc-*-infra`；**需求变更后**不再新增 `bc-*-infra` 平铺模块，新增适配器优先归位到目标 BC 的 `infrastructure` 子模块（见下方“最终形态”）。
- **最终形态（目标，2025-12-24 需求变更）**：每个 BC 在仓库中只占用 **一个顶层目录/聚合模块**（例如 `bc-iam/`、`bc-evaluation/`），其内部按职责拆为 `domain/application/infrastructure` **子模块**（或至少 package 结构完整）；`eva-*` 技术切片逐步退场或仅保留 shared-kernel/统一装配与极少量跨 BC 胶水。为保持可回滚，当前已存在的 `bc-*-infra` 作为过渡形态保留一段时间，后续按里程碑“折叠归位”到对应 BC 内部子模块。
- **需求补充（2025-12-24）**：逐步拆解 `eva-client`：将 `edu.cuit.client.*` 下的 BO/CO/DTO 等“边界协议对象”按业务归属迁入对应 BC（优先放在 BC 的 `application` 子模块下的 `contract/dto` 包，避免领域层污染；**允许改包名**以完成归位）；确实跨 BC 复用的对象再沉淀到 shared-kernel，最终让 `eva-client` 退出主干依赖。

## 0.9 本次会话增量总结（滚动，按时间倒序，更新至 `HEAD`）

**2026-01-01（本次会话）**
- ✅ **bc-messaging（消息域）：依赖收敛后半段（运行时装配上推准备）**：在 `start/pom.xml` 增加对 `bc-messaging` 的 `runtime` 依赖（保持行为不变；最小回归通过；落地提交：`f23254ec`）。
- ✅ **bc-messaging（消息域）：依赖收敛后半段（运行时装配责任上推）**：移除 `eva-infra/pom.xml` 对 `bc-messaging` 的 `runtime` 依赖，把“运行时装配责任”从 `eva-infra` 上推到组合根 `start`（保持行为不变；最小回归通过；落地提交：`507f95b2`）。
- ✅ **评教记录读侧（依赖收窄，小步）**：`EvaRecordServiceImpl.pageEvaRecord` 内联分页结果组装，移除对 `PaginationBizConvertor` 的注入依赖（分页字段赋值顺序/异常文案/循环副作用顺序不变；最小回归通过；落地提交：`55103de1`）。
- ✅ **评教记录读侧（D1：用例归位深化）**：新增 `EvaRecordQueryUseCase` 并将 `EvaRecordServiceImpl.pageEvaRecord` 退化为纯委托壳，把“实体→CO 组装 + 平均分填充 + 分页组装”编排逻辑归位到 UseCase（保持 `@CheckSemId` 触发点不变；异常文案/副作用顺序不变；最小回归通过；落地提交：`86772f59`）。
- ✅ **评教记录读侧（D1：顺序对齐加固）**：对齐 `EvaRecordQueryUseCase` 内部“实体→CO 组装”的求值顺序，避免提前触发 `Supplier` 缓存加载导致副作用顺序漂移（保持行为不变；最小回归通过；落地提交：`10991314`）。

**2025-12-30（本次会话）**
- ✅ **bc-messaging（消息域）：依赖收敛准备（事件枚举下沉到 contract）**：将 `CourseOperationMessageMode` 从 `bc-messaging` 迁移到 `bc-messaging-contract`（保持 `package edu.cuit.bc.messaging.application.event` 不变；仅类归位，不改任何业务语义；最小回归通过；落地提交：`b2247e7f`）。
- ✅ **bc-messaging（消息域）：依赖收敛准备（事件载荷下沉到 contract）**：将 `CourseOperationSideEffectsEvent` 从 `bc-messaging` 迁移到 `bc-messaging-contract`（保持 `package edu.cuit.bc.messaging.application.event` 不变；仅类归位，不改任何业务语义；最小回归通过；落地提交：`ea2c0d9b`）。
- ✅ **bc-messaging（消息域）：依赖收敛准备（事件载荷下沉到 contract）**：将 `CourseTeacherTaskMessagesEvent` 从 `bc-messaging` 迁移到 `bc-messaging-contract`（保持 `package edu.cuit.bc.messaging.application.event` 不变；仅类归位，不改任何业务语义；最小回归通过；落地提交：`12f43323`）。
- ✅ **bc-messaging（消息域）：依赖收敛（应用侧编译期依赖面收窄）**：`eva-app` 已将对 `bc-messaging` 的编译期依赖收敛为仅依赖 `bc-messaging-contract`（Serena 证据：`eva-app` 仅引用 `edu.cuit.bc.messaging.application.event.*`；保持行为不变；最小回归通过；落地提交：`d3aeb3ab`）。
- ✅ **bc-messaging（消息域）：基础设施端口适配器归位前置（DAL 归位）**：将消息表数据对象 `MsgTipDO` 从 `eva-infra` 归位到 `eva-infra-dal`（保持 `package edu.cuit.infra.dal.database.dataobject` 不变；仅类归位，不改任何业务语义；为后续把 `eva-infra/.../bcmessaging/adapter/*PortImpl` 逐个归位到 `bc-messaging` 并把依赖收敛到 `eva-infra-dal` 预置；最小回归通过；落地提交以 `git log -n 1 -- eva-infra-dal/src/main/java/edu/cuit/infra/dal/database/dataobject/MsgTipDO.java` 为准）。
- ✅ **bc-messaging（消息域）：基础设施端口适配器归位前置（DAL 归位）**：将消息表 Mapper `MsgTipMapper`（以及对应 `MsgTipMapper.xml`）从 `eva-infra` 归位到 `eva-infra-dal`（保持 `package edu.cuit.infra.dal.database.mapper` 不变；XML namespace/SQL 文案/字段映射不变；仅类与资源归位，不改任何业务语义；为后续逐个搬运 `Message*PortImpl` 并把依赖收敛到 `eva-infra-dal` 预置；最小回归通过；落地提交以 `git log -n 1 -- eva-infra-dal/src/main/java/edu/cuit/infra/dal/database/mapper/MsgTipMapper.java` 为准）。
- ✅ **bc-messaging（消息域）：基础设施端口适配器归位（消息删除）**：将 `MessageDeletionPortImpl` 从 `eva-infra` 归位到 `bc-messaging`（保持 `package edu.cuit.infra.bcmessaging.adapter` 不变；删除条件与调用顺序完全不变；并在 `bc-messaging` 补齐对 `eva-infra-dal` 的依赖以闭合 `MsgTipDO/MsgTipMapper` 与 MyBatis-Plus API；最小回归通过；落地提交以 `git log -n 1 -- bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageDeletionPortImpl.java` 为准）。
- ✅ **bc-messaging（消息域）：基础设施端口适配器归位（消息已读）**：将 `MessageReadPortImpl` 从 `eva-infra` 归位到 `bc-messaging`（保持 `package edu.cuit.infra.bcmessaging.adapter` 不变；校验逻辑/异常文案/更新条件与调用顺序完全不变；最小回归通过；落地提交以 `git log -n 1 -- bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageReadPortImpl.java` 为准）。
- ✅ **bc-messaging（消息域）：基础设施端口适配器归位（消息显示状态）**：将 `MessageDisplayPortImpl` 从 `eva-infra` 归位到 `bc-messaging`（保持 `package edu.cuit.infra.bcmessaging.adapter` 不变；校验逻辑/异常文案/更新条件与调用顺序完全不变；最小回归通过；落地提交以 `git log -n 1 -- bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageDisplayPortImpl.java` 为准）。
- ✅ **bc-messaging（消息域）：基础设施端口适配器归位前置（Convertor 归位）**：将 MapStruct 转换器 `MsgConvertor` 从 `eva-infra` 归位到 `eva-infra-shared`，并在 `eva-infra-shared` 补齐对 `bc-messaging-contract` 的依赖以闭合 `GenericRequestMsg` 类型引用（保持 `package edu.cuit.infra.convertor` 不变；保持行为不变；最小回归通过；落地提交以 `git log -n 1 -- eva-infra-shared/src/main/java/edu/cuit/infra/convertor/MsgConvertor.java` 为准）。
- ✅ **bc-messaging（消息域）：基础设施端口适配器归位（消息新增）**：将 `MessageInsertionPortImpl` 从 `eva-infra` 归位到 `bc-messaging`（保持 `package edu.cuit.infra.bcmessaging.adapter` 不变；插入后回填 `id` 与 `createTime` 的顺序不变；并在 `bc-messaging` 补齐对 `eva-infra-shared` 的依赖以闭合 `MsgConvertor` 类型引用；最小回归通过；落地提交以 `git log -n 1 -- bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageInsertionPortImpl.java` 为准）。
- ✅ **bc-messaging（消息域）：基础设施端口适配器归位（消息查询）**：将 `MessageQueryPortImpl` 从 `eva-infra` 归位到 `bc-messaging`（保持 `package edu.cuit.infra.bcmessaging.adapter` 不变；查询条件/排序/异常文案完全不变；最小回归通过；落地提交以 `git log -n 1 -- bc-messaging/src/main/java/edu/cuit/infra/bcmessaging/adapter/MessageQueryPortImpl.java` 为准）。

**2025-12-29（本次会话）**
- ✅ **bc-course（课程）：课表 Excel/POI 解析归位**：将 `eva-app` 内的课表解析实现（`edu.cuit.app.poi.course.*`）整体迁移到 `bc-course-infra`（保持 `package` 不变；异常文案/日志输出/副作用顺序完全不变），并补齐 `bc-course-infra` 对 `eva-infra-shared` 的依赖以复用 `ExcelUtils`（保持行为不变；最小回归通过；落地提交：`383dbf33`；删除旧文件并收尾：`5a7cd0a0`）。
- ✅ **bc-course（课程）：端口化与依赖收敛**：新增课表解析端口 `CourseExcelResolvePort`（`bc-course/application`），由 `bc-course-infra` 提供适配器 `CourseExcelResolvePortImpl`（内部仍复用 `CourseExcelResolver`，确保异常文案与日志不变）；`IUserCourseServiceImpl.importCourse` 改为依赖端口，移除对 `CourseExcelResolver` 的直接依赖；同时将 `eva-app` 对 `bc-course-infra` 的依赖收敛为 `runtime`，并在 `start` 测试侧补齐 `bc-course-infra` 测试依赖以保证 `CourseResolverTest` 编译（保持行为不变；最小回归通过；落地提交：`5a7cd0a0`）。
- ✅ **评教读侧进一步解耦（统计导出端口装配委托切换）**：将 `BcEvaluationConfiguration.evaStatisticsExportPort()` 从直接委托 `EvaStatisticsExcelFactory::createExcelData` 切换为委托 `bc-evaluation-infra` 的端口适配器 `EvaStatisticsExportPortImpl`（其内部仍调用 `EvaStatisticsExcelFactory.createExcelData`；保持行为不变；最小回归通过；落地提交：`565552fa`）。
- ✅ **评教读侧进一步解耦（导出基础设施归位：迁移 EvaStatisticsExcelFactory）**：将统计导出工厂 `EvaStatisticsExcelFactory` 从 `eva-app` 迁移到 `bc-evaluation-infra`（保持行为不变；导出异常文案/日志输出完全一致；最小回归通过；落地提交：`5b2c2223`）。
- ✅ **评教读侧进一步解耦（导出基础设施归位：迁移 FillUserStatisticsExporterDecorator）**：将导出装饰器 `FillUserStatisticsExporterDecorator` 从 `eva-app` 迁移到 `bc-evaluation-infra`（保持 `package edu.cuit.app.poi.eva` 不变；仅类归位，不改任何业务语义；最小回归通过；落地提交：`e83600f6`）。
- ✅ **评教读侧进一步解耦（导出基础设施归位：迁移 FillEvaRecordExporterDecorator）**：将导出装饰器 `FillEvaRecordExporterDecorator` 从 `eva-app` 迁移到 `bc-evaluation-infra`（保持 `package edu.cuit.app.poi.eva` 不变；仅类归位，不改任何业务语义；最小回归通过；落地提交：`b3afcb11`）。
- ✅ **评教读侧进一步解耦（导出基础设施归位：迁移 FillAverageScoreExporterDecorator）**：将导出装饰器 `FillAverageScoreExporterDecorator` 从 `eva-app` 迁移到 `bc-evaluation-infra`（保持 `package edu.cuit.app.poi.eva` 不变；仅类归位，不改任何业务语义；最小回归通过；落地提交：`4e150984`）。
- ✅ **评教读侧进一步解耦（导出基础设施归位：迁移 EvaStatisticsExporter）**：将导出基类 `EvaStatisticsExporter` 从 `eva-app` 迁移到 `bc-evaluation-infra`（保持 `package edu.cuit.app.poi.eva` 不变），并在 `bc-evaluation-infra` 补齐对 `bc-course/bc-iam-contract` 的编译依赖以闭合类型引用（仅类归位+依赖闭包，不改任何业务语义；静态初始化 `SpringUtil.getBean(...)` 次数/顺序不变；最小回归通过；落地提交：`e8ca391c`）。
- ✅ **MCP Serena 状态（符号级定位/引用分析）**：本会话使用 Serena 成功完成符号级定位与引用分析（未再复现 `TimeoutError`）。如后续出现 `TimeoutError`，允许临时降级为本地 `rg` 证据，但必须在本节记录“降级原因 + 可复现 rg 证据”，并在下一会话优先排查恢复。当前可复现证据（均在本仓库 `ddd` 分支执行）：
  - `rg -n --column "class\\s+EvaStatisticsExporter\\b" .` → `bc-evaluation/infrastructure/src/main/java/edu/cuit/app/poi/eva/EvaStatisticsExporter.java:24`
  - `rg -n --column "class\\s+FillUserStatisticsExporterDecorator\\b" .` → `bc-evaluation/infrastructure/src/main/java/edu/cuit/app/poi/eva/FillUserStatisticsExporterDecorator.java:19`
  - `rg -n --column "class\\s+EvaStatisticsExcelFactory\\b" .` → `bc-evaluation/infrastructure/src/main/java/edu/cuit/app/poi/eva/EvaStatisticsExcelFactory.java:13`
  - `rg -n --column "\\bExcelUtils\\b" .`：显示导出链路与课表解析均依赖 `ExcelUtils`（已归位到 `eva-infra-shared`，包名保持不变）。
- ✅ **评教读侧进一步解耦（导出基础设施归位准备：ExcelUtils 迁移）**：将 POI 工具类 `ExcelUtils` 从 `eva-app` 迁移到 `eva-infra-shared`（保持 `package edu.cuit.app.poi.util` 不变），并在 `eva-infra-shared` 补齐 `poi/poi-ooxml` 依赖，为后续把 `EvaStatisticsExporter` 等导出实现从 `eva-app` 归位到更合理落点扫清“循环依赖”风险（保持行为不变；最小回归通过；落地提交：`04009c85`）。
- ✅ **评教读侧用例归位深化（统计：旧入口委托 UseCase—exportEvaStatistics 导出链路）**：引入统计导出端口 `EvaStatisticsExportPort`（由 `BcEvaluationConfiguration` 提供 Bean：委托既有 `EvaStatisticsExcelFactory.createExcelData`），并将旧入口 `EvaStatisticsServiceImpl.exportEvaStatistics` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.exportEvaStatistics`（保持 `@CheckSemId` 触发点不变；导出异常文案/日志与副作用顺序完全不变；最小回归通过；落地提交：`0d15de60`）。
- ✅ **评教读侧用例归位深化（统计：UseCase 内部 type 分支分发逻辑收敛）**：在 `EvaStatisticsQueryUseCase` 抽出 `dispatchByType(...)`，统一复用 `type==0/type==1/否则抛 SysException("type是10以外的值")` 的分发逻辑，减少重复分支判断，避免后续继续归位方法簇时出现分支口径漂移（只重构不改业务语义/异常文案；最小回归通过；落地提交：`38ce9ece`）。
- ✅ **评教读侧用例归位深化（统计：旧入口委托 UseCase—pageUnqualifiedUser 分页结果组装）**：将旧入口 `EvaStatisticsServiceImpl.pageUnqualifiedUser` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.pageUnqualifiedUserAsPaginationQueryResult`，并移除对 `PaginationBizConvertor` 的依赖（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`f4f3fcde`）。
- ✅ **评教读侧用例归位深化（统计：pageUnqualifiedUser 分页结果组装归位起步）**：在 `EvaStatisticsQueryUseCase` 新增 `pageUnqualifiedUserAsPaginationQueryResult`，把“`PaginationResultEntity` → `PaginationQueryResultCO`”的分页结果组装逻辑先归位到用例层，为下一步旧入口 `EvaStatisticsServiceImpl.pageUnqualifiedUser` 退化为纯委托壳做准备（保持行为不变；最小回归通过；落地提交：`e97615e1`）。
- ✅ **bc-messaging 后置规划证据化（仅文档）**：补齐消息域“组合根/监听器/端口适配器散落点”的可回滚迁移路线与证据化路径清单（仅文档，不改代码；保持行为不变；落地提交：`4b05f515`；详见 `DDD_REFACTOR_PLAN.md` 第 10.3 节）。
- ✅ **文档同步与路线微调固化**：补齐“结构 DDD vs 语义 DDD”“何时可收敛 eva-*”的阶段性判断，并记录“允许微调 + 主线优先、暂不新增 S0 折叠试点提交”的策略说明，确保下会话续接不丢失（仅文档口径，不改业务语义）。
- ✅ **评教读侧用例归位深化（统计：evaScoreStatisticsInfo 空对象兜底重载归位起步）**：在 `EvaStatisticsQueryUseCase` 新增 `evaScoreStatisticsInfoOrEmpty`，先把 `Optional.empty` → `new EvaScoreInfoCO()` 的兜底逻辑归位到用例层（保持行为不变；为下一步让旧入口 `EvaStatisticsServiceImpl` 退化为纯委托壳做准备；最小回归通过；落地提交：`bce01df2`）。
- ✅ **评教读侧用例归位深化（统计：旧入口委托 UseCase—evaScoreStatisticsInfo）**：将 `EvaStatisticsServiceImpl.evaScoreStatisticsInfo` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.evaScoreStatisticsInfoOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`1bf3a4fe`）。
- ✅ **评教读侧用例归位深化（统计：evaTemplateSituation 空对象兜底重载归位起步）**：在 `EvaStatisticsQueryUseCase` 新增 `evaTemplateSituationOrEmpty`，先把 `Optional.empty` → `new EvaSituationCO()` 的兜底逻辑归位到用例层（保持行为不变；为下一步让旧入口 `EvaStatisticsServiceImpl` 退化为纯委托壳做准备；最小回归通过；落地提交：`89b6b1ee`）。
- ✅ **评教读侧用例归位深化（统计：旧入口委托 UseCase—evaTemplateSituation）**：将 `EvaStatisticsServiceImpl.evaTemplateSituation` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.evaTemplateSituationOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`78abf1a1`）。
- ✅ **评教读侧用例归位深化（统计：evaWeekAdd 空对象兜底重载归位起步）**：在 `EvaStatisticsQueryUseCase` 新增 `evaWeekAddOrEmpty`，先把 `Optional.empty` → `new EvaWeekAddCO()` 的兜底逻辑归位到用例层（保持行为不变；为下一步让旧入口 `EvaStatisticsServiceImpl` 退化为纯委托壳做准备；最小回归通过；落地提交：`5a8ac076`）。
- ✅ **评教读侧用例归位深化（统计：旧入口委托 UseCase—evaWeekAdd）**：将 `EvaStatisticsServiceImpl.evaWeekAdd` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.evaWeekAddOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`2a92ca0b`）。
- ✅ **评教读侧用例归位深化（统计：getEvaData 空对象兜底重载归位起步）**：在 `EvaStatisticsQueryUseCase` 新增 `getEvaDataOrEmpty`，先把 `Optional.empty` → `new PastTimeEvaDetailCO()` 的空对象兜底归位到用例层（保持行为不变；为下一步让旧入口 `EvaStatisticsServiceImpl` 退化为纯委托壳做准备；最小回归通过；落地提交：`1180a0f7`）。
- ✅ **评教读侧用例归位深化（统计：旧入口委托 UseCase—getEvaData）**：将 `EvaStatisticsServiceImpl.getEvaData` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.getEvaDataOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`b59db93d`）。
- ✅ **评教读侧用例归位深化（统计：getTargetAmountUnqualifiedUser 空对象兜底重载归位起步）**：在 `EvaStatisticsQueryUseCase` 新增 `getTargetAmountUnqualifiedUserOrEmpty`，先把 “`Optional.empty` → `new UnqualifiedUserResultCO().setTotal(0).setDataArr(List.of())`” 的空对象兜底归位到用例层（保持行为不变；为下一步让旧入口 `EvaStatisticsServiceImpl` 退化为纯委托壳做准备；最小回归通过；落地提交：`0ac65fb4`）。
- ✅ **评教读侧用例归位深化（统计：旧入口委托 UseCase—getTargetAmountUnqualifiedUser）**：将 `EvaStatisticsServiceImpl.getTargetAmountUnqualifiedUser` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUserOrEmpty`，从而把空对象兜底彻底归位到 UseCase（保持 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`b931b247`）。
- ✅ **评教读侧用例归位深化（统计：读测补齐—getTargetAmountUnqualifiedUserOrEmpty type=1）**：为 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUserOrEmpty(type=1)` 补齐空结果兜底的读侧用例级测试（保持行为不变；最小回归通过；落地提交：`8f3e7afe`）。
- ✅ **评教读侧用例归位深化（统计：读测补齐—getEvaDataOrEmpty 空结果兜底）**：为 `EvaStatisticsQueryUseCase.getEvaDataOrEmpty` 补齐 `Optional.empty` 时返回空对象的读侧用例级测试，并保留阈值读取顺序与委托顺序验证（保持行为不变；最小回归通过；落地提交：`a44a9bba`）。
- ✅ **评教读侧用例归位深化（统计：读测补齐—getTargetAmountUnqualifiedUserOrEmpty 非法 type）**：为 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUserOrEmpty(invalidType)` 补齐异常文案不变的读侧用例级测试，并确保不触发查询端口调用（保持行为不变；最小回归通过；落地提交：`0cb2caec`）。
- ✅ **docs（交接与计划同步）**：本次会话已按“每步闭环”将关键变更与下一步计划同步到三文档；如需定位最新同步提交，使用 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` / `git log -n 1 -- DDD_REFACTOR_PLAN.md` / `git log -n 1 -- docs/DDD_REFACTOR_BACKLOG.md`。

**2025-12-28（本次会话）**
- ✅ **评教读侧用例归位深化（统计：旧入口去除 EvaConfigGateway 直依赖—unqualifiedUser）**：将 `EvaStatisticsServiceImpl.pageUnqualifiedUser/getTargetAmountUnqualifiedUser` 退化为纯委托壳，改为调用 `EvaStatisticsQueryUseCase` 的重载方法（由 UseCase 内部统一读取 `EvaConfigGateway.getEvaConfig()` 并委托既有实现），从而移除旧入口对 `EvaConfigGateway` 的直接依赖（保持 `@CheckSemId` 触发点不变；异常文案/副作用顺序不变；最小回归通过；落地提交：`21f6ad5b`）。
- ✅ **评教读侧用例归位深化（统计：unqualifiedUser 参数组装归位起步）**：在 `EvaStatisticsQueryUseCase` 新增重载方法 `pageUnqualifiedUser(semId, type, query)` 与 `getTargetAmountUnqualifiedUser(semId, type, num, error)`，内部统一读取 `EvaConfigGateway.getEvaConfig()` 并委托既有实现（不改业务语义/异常文案/副作用顺序；为下一步让 `EvaStatisticsServiceImpl` 去除对 `EvaConfigGateway` 的直接依赖做准备；最小回归 + `EvaStatisticsQueryUseCaseTest` 通过；落地提交：`0a2fec4d`）。
- ✅ **评教读侧用例归位深化（统计：getEvaData 阈值计算/参数组装归位）**：将 `EvaStatisticsServiceImpl.getEvaData` 中阈值读取（`EvaConfigGateway.getMinEvaNum/getMinBeEvaNum`）与参数组装归位到 `EvaStatisticsQueryUseCase.getEvaData(semId, num)`（保持阈值读取顺序不变；旧入口 `@CheckSemId` 触发点不变；保持行为不变；最小回归通过；落地提交：`8f4c07c5`）。
- ✅ **docs（交接与计划同步）**：本次会话已按“每步闭环”将关键变更与下一步计划同步到三文档；如需定位最新同步提交，使用 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` / `git log -n 1 -- DDD_REFACTOR_PLAN.md` / `git log -n 1 -- docs/DDD_REFACTOR_BACKLOG.md`。
- ✅ **评教读侧进一步解耦（统计：导出基类依赖类型收窄—CountAbEva）**：将导出基类 `EvaStatisticsExporter` 静态初始化中获取统计端口的依赖类型从 `EvaStatisticsOverviewQueryPort` 收窄为子端口 `EvaStatisticsCountAbEvaQueryPort`（保持 `SpringUtil.getBean(...)` 次数与顺序不变；不改任何业务语义；最小回归通过；落地提交：`7337d378`）。
- ✅ **评教读侧进一步解耦（统计：导出链路子端口补齐—CountAbEva）**：新增统计读侧子端口 `EvaStatisticsCountAbEvaQueryPort` 并让 `EvaStatisticsOverviewQueryPort` `extends` 该子端口（仅接口细分，不改实现/不改装配；保持行为不变；最小回归通过；落地提交：`24b13138`）。
- ✅ **工程噪音收敛（dev 环境 MyBatis 日志）**：将 `application-dev.yml` 中 MyBatis-Plus 的 `log-impl` 从 `org.apache.ibatis.logging.stdout.StdOutImpl` 切换为 `org.apache.ibatis.logging.slf4j.Slf4jImpl`，避免 SQL 调试日志直出 stdout（仅 dev profile，生产不变；最小回归通过；落地提交：`cb3a4620`）。
- ✅ **工程噪音收敛（dev/test 非法入参打印）**：将 `application-dev.yml/application-test.yml` 中 `common.print-illegal-arguments` 从 `true` 调整为 `false`，减少控制台噪音（仅 dev/test profile；不改业务逻辑；最小回归通过；落地提交：`21ba35dd`）。
- ✅ **评教读侧进一步解耦（记录：依赖类型收窄—AI 报告分析）**：将 AI 报告分析端口适配器 `AiReportAnalysisPortImpl` 对记录端口的依赖类型从聚合接口 `EvaRecordQueryPort` 收窄为子端口 `EvaRecordExportQueryPort`（仅收窄依赖类型，不改调用逻辑；保持行为不变；最小回归通过；落地提交：`4fe38934`）。
- ✅ **评教读侧用例归位深化（统计：未达标用户目标数分支选择归位）**：将 `EvaStatisticsServiceImpl.getTargetAmountUnqualifiedUser` 的 `type` 分支选择与阈值选择归位到 `EvaStatisticsQueryUseCase.getTargetAmountUnqualifiedUser`（旧入口 `@CheckSemId` 触发点不变；异常文案 `type是10以外的值` 不变；保持行为不变；最小回归通过；落地提交：`5b20d44e`）。
- ✅ **评教读侧用例归位深化（统计：未达标用户分页分支选择归位）**：将 `EvaStatisticsServiceImpl.pageUnqualifiedUser` 的 `type` 分支选择逻辑归位到 `EvaStatisticsQueryUseCase.pageUnqualifiedUser`（`@CheckSemId` 触发点仍保留在旧入口；异常文案 `type是10以外的值` 不变；保持行为不变；最小回归通过；落地提交：`22dccc70`）。
- ✅ **评教读侧进一步解耦（记录：依赖类型收窄—导出基类）**：将导出基类 `EvaStatisticsExporter` 静态初始化中获取记录端口的依赖类型从聚合接口 `EvaRecordQueryPort` 收窄为子端口 `EvaRecordExportQueryPort`（保持 `SpringUtil.getBean(...)` 次数与顺序不变；不改任何业务语义；最小回归通过；落地提交：`682bf081`）。
- ✅ **评教读侧进一步解耦（记录：导出链路子端口补齐—组合端口）**：新增记录导出链路子端口 `EvaRecordExportQueryPort`（组合 `EvaRecordCourseQueryPort/EvaRecordScoreQueryPort`），并让聚合端口 `EvaRecordQueryPort` `extends` 该子端口（仅新增接口+继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`5df35c36`）。
- ✅ **评教读侧进一步解耦（模板：引用面盘点结论/证伪）**：使用 Serena 盘点 `EvaTemplateQueryPort` 在全仓库的引用面，除端口定义外仅剩 `EvaTemplateQueryPortImpl` 实现侧引用；应用层（`eva-app`）未发现其它对聚合端口的注入点/调用点，因此模板主题的“端口细分 + 依赖类型收窄（服务层）”阶段可视为已闭合（保持行为不变；证据：Serena `find_referencing_symbols/search_for_pattern` 结果；最小回归通过；落地提交：`e67fc47d`）。
- ✅ **评教读侧进一步解耦（模板：依赖类型收窄—模板服务）**：将 `EvaTemplateServiceImpl` 对模板端口的依赖从聚合接口 `EvaTemplateQueryPort` 收窄为三个子端口 `EvaTemplatePagingQueryPort/EvaTemplateAllQueryPort/EvaTemplateTaskTemplateQueryPort`（不改业务逻辑/异常文案；仅调整依赖类型与调用点；保持行为不变；最小回归通过；落地提交：`b86db7e4`）。
- ✅ **评教读侧进一步解耦（模板：子端口接口细分—分页/全量/按任务取模板）**：新增模板读侧子端口 `EvaTemplatePagingQueryPort/EvaTemplateAllQueryPort/EvaTemplateTaskTemplateQueryPort`，并让 `EvaTemplateQueryPort` `extends` 这些子端口（仅新增接口+继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`a14d3c53`）。
- ✅ **评教读侧进一步解耦（任务：依赖类型收窄—任务服务）**：将 `EvaTaskServiceImpl` 对任务端口的依赖从聚合接口 `EvaTaskQueryPort` 收窄为三个子端口 `EvaTaskPagingQueryPort/EvaTaskSelfQueryPort/EvaTaskInfoQueryPort`（不改业务逻辑/异常文案；仅调整依赖类型与调用点；保持行为不变；最小回归通过；落地提交：`4b22f059`）。
- ✅ **评教读侧进一步解耦（任务：子端口接口细分—本人任务/数量统计）**：新增任务读侧子端口 `EvaTaskSelfQueryPort/EvaTaskCountQueryPort`，并让 `EvaTaskQueryPort` `extends` 这些子端口（仅新增接口+继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`9d5064fc`）。
- ✅ **评教读侧进一步解耦（任务：子端口接口细分起步—分页）**：新增任务读侧分页查询子端口 `EvaTaskPagingQueryPort`（仅新增接口，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`f0a172d1`）。
- ✅ **评教读侧进一步解耦（任务：聚合端口继承子端口—分页）**：让 `EvaTaskQueryPort` `extends EvaTaskPagingQueryPort`（仅接口继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`2fd9d24e`）。
- ✅ **评教读侧进一步解耦（任务：子端口接口细分起步—单任务信息）**：新增任务读侧“单任务信息/任务名称”子端口 `EvaTaskInfoQueryPort`（仅新增接口，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`26b79c3a`）。
- ✅ **评教读侧进一步解耦（任务：聚合端口继承子端口—单任务信息）**：让 `EvaTaskQueryPort` `extends EvaTaskInfoQueryPort`（仅接口继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`56834293`）。
- ✅ **评教读侧进一步解耦（任务：依赖类型收窄—消息）**：将 `MsgServiceImpl` 对任务端口的依赖从聚合接口 `EvaTaskQueryPort` 收窄为子端口 `EvaTaskInfoQueryPort`（不改业务逻辑/异常文案；仅调整依赖类型；保持行为不变；最小回归通过；落地提交：`7aa49e7f`）。
- ✅ **评教读侧进一步解耦（记录：依赖类型收窄—用户评教日志）**：将 `UserEvaServiceImpl` 对记录端口的依赖从聚合接口 `EvaRecordQueryPort` 收窄为两个子端口 `EvaRecordUserLogQueryPort/EvaRecordScoreQueryPort`（不改业务逻辑/异常文案；仅调整依赖类型；保持行为不变；最小回归通过；落地提交：`80886841`）。
- ✅ **评教读侧进一步解耦（记录：依赖类型收窄—消息）**：将 `MsgServiceImpl` 对记录端口的依赖从聚合接口 `EvaRecordQueryPort` 收窄为子端口 `EvaRecordCountQueryPort`（不改业务逻辑/异常文案；仅调整依赖类型；保持行为不变；最小回归通过；落地提交：`147d486b`）。
- ✅ **本次会话总览（方向 A：记录/任务读侧细分 + 依赖收窄）**：
  - 记录主题：完成记录读侧 QueryPort 细分为 5 个子端口（得分/分页/用户日志/按课程/数量统计），并让 `EvaRecordQueryPort` `extends` 这些子端口；同时完成依赖类型收窄：`EvaRecordServiceImpl` → `EvaRecordPagingQueryPort/EvaRecordScoreQueryPort`，`UserServiceImpl` → `EvaRecordCountQueryPort`，`MsgServiceImpl` → `EvaRecordCountQueryPort`，`UserEvaServiceImpl` → `EvaRecordUserLogQueryPort/EvaRecordScoreQueryPort`（保持行为不变）。
  - 任务主题：复制“统计套路”推进：新增子端口 `EvaTaskInfoQueryPort/EvaTaskPagingQueryPort/EvaTaskSelfQueryPort/EvaTaskCountQueryPort` 并让 `EvaTaskQueryPort` `extends` 这些子端口；同时完成依赖类型收窄：`MsgServiceImpl` → `EvaTaskInfoQueryPort`，`EvaTaskServiceImpl` → `EvaTaskPagingQueryPort/EvaTaskSelfQueryPort/EvaTaskInfoQueryPort`（保持行为不变）。
  - 模板主题：复制“统计套路”起步：新增子端口 `EvaTemplatePagingQueryPort/EvaTemplateAllQueryPort/EvaTemplateTaskTemplateQueryPort` 并让 `EvaTemplateQueryPort` `extends` 这些子端口；同时完成依赖类型收窄：`EvaTemplateServiceImpl` → `EvaTemplatePagingQueryPort/EvaTemplateAllQueryPort/EvaTemplateTaskTemplateQueryPort`（保持行为不变）。
  - 导出链路（记录）：为保持导出类静态初始化的 `SpringUtil.getBean(...)` 次数/顺序不变，先补齐组合子端口 `EvaRecordExportQueryPort` 并让 `EvaRecordQueryPort` `extends`；再将导出基类 `EvaStatisticsExporter` 的记录端口依赖类型收窄为 `EvaRecordExportQueryPort`（保持行为不变）。
  - 关键落地提交（按主题归类）：
    - 记录：`4e47ffe3/e4f0efe9/fcac9324/e9034541/db876379/39a4bafe/8b24d2f8/147d486b/80886841/5df35c36/682bf081`
    - 任务：`26b79c3a/56834293/f0a172d1/2fd9d24e/7aa49e7f/9d5064fc/4b22f059`
    - 模板：`a14d3c53/b86db7e4`
- ✅ **评教读侧进一步解耦（记录：依赖类型收窄—用户得分）**：将 `UserServiceImpl` 对记录端口的依赖从聚合接口 `EvaRecordQueryPort` 收窄为 `EvaRecordCountQueryPort`（不改业务逻辑/异常文案；仅调整依赖类型；补充单测 `UserServiceImplTest`；最小回归与单测通过；落地提交：`8b24d2f8`）。
- ✅ **评教读侧进一步解耦（记录：聚合端口继承子端口—数量统计）**：让 `EvaRecordQueryPort` `extends EvaRecordCountQueryPort`（仅接口继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`0d562206`）。
- ✅ **评教读侧进一步解耦（记录：子端口接口细分—数量统计）**：新增记录读侧数量统计子端口 `EvaRecordCountQueryPort`（仅新增接口，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`db876379`）。
- ✅ **评教读侧进一步解耦（记录：聚合端口继承子端口—按课程）**：让 `EvaRecordQueryPort` `extends EvaRecordCourseQueryPort`（仅接口继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`b8efeaf5`）。
- ✅ **评教读侧进一步解耦（记录：子端口接口细分—按课程）**：新增记录读侧按课程查询子端口 `EvaRecordCourseQueryPort`（仅新增接口，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`e9034541`）。
- ✅ **评教读侧进一步解耦（记录：聚合端口继承子端口—用户日志）**：让 `EvaRecordQueryPort` `extends EvaRecordUserLogQueryPort`（仅接口继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`1e025e48`）。
- ✅ **评教读侧进一步解耦（记录：子端口接口细分—用户日志）**：新增记录读侧用户日志子端口 `EvaRecordUserLogQueryPort`（仅新增接口，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`fcac9324`）。
- ✅ **评教读侧进一步解耦（记录：依赖类型收窄）**：将 `EvaRecordServiceImpl` 对记录端口的依赖从聚合接口 `EvaRecordQueryPort` 收窄为两个子端口 `EvaRecordPagingQueryPort/EvaRecordScoreQueryPort`（不改业务逻辑/异常文案；仅调整依赖类型；最小回归通过；落地提交：`39a4bafe`）。
- ✅ **评教读侧进一步解耦（记录：聚合端口继承子端口—分页）**：让 `EvaRecordQueryPort` `extends EvaRecordPagingQueryPort`（仅接口继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`76976c0b`）。
- ✅ **评教读侧进一步解耦（记录：子端口接口细分—分页）**：新增记录读侧分页子端口 `EvaRecordPagingQueryPort`（仅新增接口，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`e4f0efe9`）。
- ✅ **评教读侧进一步解耦（记录：聚合端口继承子端口）**：让 `EvaRecordQueryPort` `extends EvaRecordScoreQueryPort`（仅接口继承，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`0c7e7d13`）。
- ✅ **评教读侧进一步解耦（记录：子端口接口细分起步）**：新增记录读侧得分子端口 `EvaRecordScoreQueryPort`（仅新增接口，不改实现/不改装配；不改任何业务语义；最小回归通过；落地提交：`4e47ffe3`）。
- ✅ **评教读侧进一步解耦（统计：依赖类型收窄）**：将 `EvaStatisticsServiceImpl` 对统计端口的依赖从聚合接口 `EvaStatisticsQueryPort` 收窄为三个子端口 `EvaStatisticsOverviewQueryPort/EvaStatisticsTrendQueryPort/EvaStatisticsUnqualifiedUserQueryPort`（不改任何业务逻辑/异常文案；仅调整依赖类型；行为不变；最小回归通过；落地提交：`c19d8801`）。
- ✅ **评教读侧进一步解耦（统计导出：依赖类型收窄）**：将导出侧静态初始化中获取的统计端口由 `EvaStatisticsQueryPort` 收窄为 `EvaStatisticsOverviewQueryPort`（保持 `SpringUtil.getBean(...)` 的调用次数与顺序不变；不改业务逻辑；行为不变；最小回归通过；落地提交：`9b3c4e6a`）。
- ✅ **评教读侧进一步解耦（统计：UseCase 归位起步）**：在 `bc-evaluation` 应用层新增统计读侧用例 `EvaStatisticsQueryUseCase`，并在 `BcEvaluationConfiguration` 装配为 Bean；`EvaStatisticsServiceImpl` 改为委托该用例完成端口调用（不改现有分支/异常文案与阈值计算逻辑；行为不变；最小回归通过；落地提交：`db09d87b`）。

**2025-12-27（本次会话）**
- ✅ **S0（结构性里程碑：`bc-ai-report` 折叠归位，阶段 1）**：将 `bc-ai-report` 折叠为 `bc-ai-report-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-ai-report`；仅搬运/依赖收敛，不改业务语义；最小回归通过；落地提交：`e14f4f7a`）。
  - 说明：本阶段先完成 Maven/目录结构折叠与源码物理搬运（保持 `package` 不变），当前仍将端口适配器与 AI 基础设施暂留在 `application` 子模块；下一步再分离到 `infrastructure` 子模块（保持行为不变）。
- ✅ **S0（结构性里程碑：`bc-ai-report` 折叠归位，阶段 2）**：将端口适配器（`edu.cuit.app.bcaireport.adapter.*`）、导出实现（`edu.cuit.app.poi.ai.*`）与 AI 基础设施（`edu.cuit.infra.ai.*`）搬运到 `bc-ai-report/infrastructure` 子模块，并在 `eva-app` 补齐对 `bc-ai-report-infra` 的依赖以保证装配（保持行为不变；最小回归通过；落地提交：`444c7aca`）。
- ✅ **S0（结构性里程碑：`bc-audit` 折叠归位，阶段 1）**：将 `bc-audit` 折叠为 `bc-audit-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-audit`；仅搬运/依赖收敛，不改业务语义；最小回归通过；落地提交：`81594308`）。
  - 说明：本次尝试使用 Serena 做符号级定位/引用分析，但 MCP 工具调用持续超时；已退化为使用本地 `rg` 做定位与引用复核。变更仅涉及 Maven/目录结构与源码物理路径（`package` 不变），不影响业务语义。
- ✅ **S0（结构性里程碑：`bc-audit` 折叠归位，阶段 2）**：将审计日志写链路的端口适配器 `edu.cuit.infra.bcaudit.adapter.LogInsertionPortImpl` 从 `eva-infra` 搬运到 `bc-audit/infrastructure` 子模块；并补齐 `eva-app` → `bc-audit-infra` 依赖以保证 Spring 装配（保持行为不变；最小回归通过；落地提交：`d7858d7a`）。
  - 说明：阶段 2 为保持行为不变，`bc-audit-infra` 过渡性依赖 `eva-infra` 以复用既有 DAL/Converter/Gateway；本会话已推进阶段 3，将依赖收敛为 `eva-infra-dal` + `eva-infra-shared`（见下条，保持行为不变）。
  - 证据化引用面（可复现）：本阶段同样尝试用 Serena 做符号级引用分析，但 MCP 工具调用持续 `TimeoutError`；已退化为使用本地 `rg` 复核关键装配/引用点（不改变语义，仅用于“定位证据”）。建议复核关键词：`LogInsertionPortImpl/LogInsertionPort/InsertLogUseCase/BcAuditConfiguration/LogGatewayImpl`。
- ✅ **S0（结构性里程碑：`bc-audit` 折叠归位，阶段 3，可选）**：将 `sys_log` 相关 DAL（`SysLog*DO/Mapper/XML`）迁移到 `eva-infra-dal`，将 `LogConverter` 迁移到 `eva-infra-shared`，并将 `bc-audit-infra` Maven 依赖由 `eva-infra` 收敛为 `eva-infra-dal` + `eva-infra-shared`（保持包名/namespace/SQL 不变；缓存/日志/异常文案/副作用顺序不变；最小回归通过；落地提交：`06ec6f3d`）。
- ✅ **评教读侧进一步解耦（后置，接口细化起步）**：细分统计 QueryPort：新增 `EvaStatisticsOverviewQueryPort/EvaStatisticsTrendQueryPort/EvaStatisticsUnqualifiedUserQueryPort`，并让 `EvaStatisticsQueryPort` `extends` 以上子端口（仅接口拆分，不改实现/不改装配；行为不变；最小回归通过；落地提交：`a1d6ccab`）。
- ✅ **条目 25（AI 报告写侧：组合根 wiring 归位）**：将 `BcAiReportConfiguration` 从 `eva-app` 迁移到 `bc-ai-report`（保持 `package edu.cuit.app.config` 不变；Bean 定义与 `@Lazy` 环断策略不变；保持行为不变；最小回归通过；落地提交：`58c2f055`）。
  - 行为快照（变更前后必须一致；用于下一步继续收敛“剩余写链路”时对照）：
    - 入口与链路顺序：`GET /evaluate/export/report`（`EvaStatisticsController.exportEvaReport`）→ `IAiCourseAnalysisService.exportDocData`（`AiCourseAnalysisService.exportDocData`）→ `ExportAiReportDocByUsernameUseCase.exportDocData`。
    - username→userId：`userIdQueryPort.findIdByUsername(username)` 若为空：记录日志 `系统异常`（`log.error`，携带 `SysException("用户数据查找失败，请联系管理员")`）→ 抛该 `SysException`（异常类型/文案不变）。
    - analysis（保持 `@CheckSemId` 切面触发链路不变）：`ExportAiReportDocByUsernameUseCase` 仍通过 `@Lazy IAiCourseAnalysisService` 调用 `aiCourseAnalysisService.analysis(semId, userId)`，以确保 `@CheckSemId` 对 `semId` 的既有语义保持不变。
    - 导出失败：`exportAiReportDocUseCase.exportDocData(analysis)` 抛 `IOException` 时：日志 `AI报告导出失败` → 抛 `SysException("报告导出失败，请联系管理员")`（异常类型/文案与日志顺序不变；触发点不变）。
- ✅ **条目 25（AI 报告写侧：`@CheckSemId` 注解下沉 shared-kernel）**：将 `edu.cuit.app.aop.CheckSemId` 从 `eva-app` 迁移到 `shared-kernel`（保持 `package` 不变；`AspectConfig` 的切面匹配表达式仍为 `@annotation(edu.cuit.app.aop.CheckSemId)`；保持行为不变；最小回归通过；落地提交：`1c595052`）。
- ✅ **条目 25（AI 报告写侧：旧入口 `AiCourseAnalysisService` 归位）**：将 `AiCourseAnalysisService` 从 `eva-app` 迁移到 `bc-ai-report`（保持 `package edu.cuit.app.service.impl.ai` 不变；保持 `@Service/@CheckSemId` 切面触发点不变；保持行为不变；最小回归通过；落地提交：`ca321a20`）。
  - 行为快照（变更前后必须一致）：
    - `analysis(semId, teacherId)`：仍由 `@CheckSemId` 处理 `semId` 的空值/非法值语义后，再委托 `AnalyseAiReportUseCase.analysis(semId, teacherId)`（不新增日志/异常）。
    - `exportDocData(semId)`：仍使用 `((String) StpUtil.getLoginId())` 获取 username（含潜在 `ClassCastException` 语义不变）→ 调用 `ExportAiReportDocByUsernameUseCase.exportDocData(semId, username)`。
- ✅ **条目 25（AI 报告写侧：AI 基础设施归位 + 依赖收敛）**：将 `edu.cuit.infra.ai.*`（模型 Bean 配置、提示词常量、消息工具、AI 服务接口等）从 `eva-infra` 迁移到 `bc-ai-report`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`e2f2a7ff`），并移除 `bc-ai-report` → `eva-infra` 的编译期依赖，改为显式依赖 `langchain4j` + `langchain4j-community-dashscope-spring-boot-starter` + `bc-evaluation`（仅闭合编译依赖，不改变运行时行为）。
- ✅ **条目 25（AI 报告写侧：username→userId 链路实现归位）**：将用户名查询 userId 的端口适配器 `AiReportUserIdQueryPortImpl` 从 `eva-app` 迁移到 `bc-ai-report`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`e2a608e2`）。
  - 行为快照（变更前后必须一致）：
    - 原样委托 `UserQueryGateway.findIdByUsername(username)`，返回 `Optional<Integer>` 的空值语义保持不变（不新增日志/异常）。
- ✅ **条目 25（AI 报告写侧：analysis 链路实现归位）**：将 AI 报告分析端口适配器 `AiReportAnalysisPortImpl` 从 `eva-app` 迁移到 `bc-ai-report`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`6f34e894`）。
  - 行为快照（变更前后必须一致）：
    - 使用的模型与调用顺序不变：按课程逐个构建 `CourseAiServices`（`qwenMaxChatModel`）生成优点/缺点/建议；最后再用 `deepseekChatModel` 汇总总体建议。
    - 记录并发与组装逻辑不变：`records.parallelStream()` 保持；评分计算仍依赖 `evaRecordQueryPort.getScoreFromRecord(...).get()` 的既有语义。
    - 用户不存在：日志 `根据用户id获取用户失败` → 抛 `BizException("导出报告失败，请联系管理员")`（触发点：`AiReportAnalysisPortImpl.analysis` 末尾 `userQueryGateway.findById(...).orElseThrow(...)`）。
- ✅ **条目 25（AI 报告写侧：剩余“落库/记录”链路盘点结论 / 证伪）**：本次用 Serena 对全仓库做“符号级引用分析 + 关键词检索”，未发现与 AI 报告相关的“落库/记录/缓存写入”链路（当前仅存在：导出/analysis/username→userId 查询 + 调用大模型的外部副作用；且均已归位 `bc-ai-report`）。因此条目 25 的后续重点切换为：**S0 折叠 `bc-ai-report`**（仅搬运/依赖收敛，保持行为不变）。
  - 证据清单（可复现的 Serena 查询，均在本仓库 `ddd` 分支执行）：
    - `find_referencing_symbols(IAiCourseAnalysisService)`：引用点仅在 `EvaStatisticsController` 与 `bc-ai-report` 组合根/旧入口/用例（无 DB 写链路外溢）。
    - `search_for_pattern(AiReport|AI报告|AiAnalysisBO|bcaireport|CourseAiServices)`：命中点均在 `bc-ai-report`（以及导出测试），未出现持久化/写入类。
    - `search_for_pattern(Ai\\w*(Gateway|Repository|Mapper|Dao|DO|Entity))`：无命中（未发现 AI 报告相关 DAL/Repository/Mapper）。
    - `search_for_pattern(.insert|.save\\()`（限制在 `bc-ai-report/src/main/java`）：无命中（未发现写入调用点）。
- ✅ **S0（结构性里程碑：`bc-template` 折叠归位）**：将 `bc-template` 折叠为 `bc-template-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-template`；包名不变；保持行为不变；最小回归通过；落地提交：`65091516`）。
- ✅ **S0（结构性里程碑：`bc-course` 折叠归位）**：将 `bc-course` 折叠为 `bc-course-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-course`；包名不变；保持行为不变；最小回归通过；落地提交：`e90ad03b`）。
- ✅ **条目 25（AI 报告写侧：导出链路实现归位）**：将 AI 报告导出端口适配器 `AiReportDocExportPortImpl` 与 Word 生成器 `AiReportExporter` 从 `eva-app` 迁移到 `bc-ai-report`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`d1262c32`）。
  - 行为快照（变更前后必须一致）：
    - 导出失败（`IOException`）：日志 `AI报告导出失败` → 抛 `SysException("报告导出失败，请联系管理员")`（触发点：`ExportAiReportDocByUsernameUseCase.exportDocData`）。
- ✅ **docs（交接与计划同步）**：补齐“当前总体进度汇报”口径，并重排 0.10/0.11 的“下一会话按顺序执行”清单，确保下个新会话可直接按顺序推进且不丢信息（保持行为不变；最小回归通过；落地提交以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准）。
- ✅ **S0.1（依赖路径继续收敛）**：移除 `eva-infra-shared` → `eva-client` Maven 直依赖（保持行为不变；最小回归通过；落地提交：`9437bb12`）。
- ✅ **S0.1（通用/跨域对象继续沉淀）**：将 `EvaProp` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`4feabdd0`）。
- ✅ **S0.1（AI 报告协议继续拆 `eva-client`）**：将 AI 接口与 BO（`IAiCourseAnalysisService/AiAnalysisBO/AiCourseSuggestionBO`）从 `eva-client` 迁移到 `bc-ai-report`，并移除 `bc-ai-report` → `eva-client` 依赖，改为显式依赖 `commons-lang3`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`badb9db6`）。
- ✅ **S0.1（课程协议继续拆 `eva-client`，教室接口）**：将 `IClassroomService` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`59471a96`）。
- ✅ **S0.1（消息协议继续拆 `eva-client`，clientobject）**：将 `EvaMsgCO` 从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`2f257a86`）。
- ✅ **S0.1（消息协议继续拆 `eva-client`，cmd）**：将 `SendWarningMsgCmd` 从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`e6aa5913`）。
- ✅ **S0.1（消息协议继续拆 `eva-client`，cmd/interface/bo）**：将消息域 `IMsgService/SendMessageCmd/MessageBO` 从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`431a5a23`）。
- ✅ **S0.1（消息协议继续拆 `eva-client`，response DTO）**：将消息 response DTO（`GenericResponseMsg/EvaResponseMsg`）从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`ecb8cee5`）。
- ✅ **S0.1（课程协议继续拆 `eva-client`，clientobject/course 残留 CO）**：将课程域 `clientobject/course` 残留 CO（`ModifySingleCourseDetailCO/RecommendCourseCO/SelfTeachCourseCO/SelfTeachCourseTimeCO/SelfTeachCourseTimeInfoCO/SubjectCO/TeacherInfoCO`）以及 `SimpleCourseResultCO/SimpleSubjectResultCO` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`ce1a0a90`）。
- ✅ **S0.1（通用对象沉淀 shared-kernel，单课次 CO）**：将课程 CO `SingleCourseCO` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`ccc82092`）。
- ✅ **S0.1（通用对象沉淀 shared-kernel，课程时间段/类型）**：将课程数据对象 `CoursePeriod/CourseType` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`5629bd2a`）。
- ✅ **S0.1（消息协议继续拆 `eva-client`）**：将消息入参 DTO `GenericRequestMsg` 从 `eva-client` 迁移到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`8fc7db99`）。
- ✅ **S0.1（状态复盘，保持行为不变）**：全仓库 Maven 依赖面已不再引用 `eva-client`；root reactor 已移除 `eva-client` 模块；仓库中已移除 `eva-client/` 目录（需要回滚通过 Git 提交点即可）；`eva-domain` 中仍存在 `import edu.cuit.client.*`，但对应类型均已由 `shared-kernel` / 各 BC contract / `eva-domain` 自身承载（包名保持不变）。
- ✅ **S0.1（收尾盘点，来源证伪；包名保持不变）**：使用 Serena 盘点 `eva-domain` 内所有 `import edu.cuit.client.*`（共 10 处导入），逐项确认类型定义文件**不在** `eva-client`（其模块/目录已退出主干），而分别落在以下模块：
  - `eva-domain`：`SysLogBO`
  - `shared-kernel`：`PagingQuery`、`GenericConditionalQuery`、`SimpleResultCO`、`PaginationQueryResultCO`、`SingleCourseCO`、`CourseTime`、`CoursePeriod`、`CourseType`
  - `bc-course`：`SemesterCO`、`CourseDetailCO`、`RecommendCourseCO`、`SelfTeachCourseCO`、`SelfTeachCourseTimeCO`、`SelfTeachCourseTimeInfoCO`、`CourseExcelBO`、`Term`、`CourseQuery`、`CourseConditionalQuery`、`MobileCourseQuery`、课程写侧命令 `AlignTeacherCmd/UpdateCourseCmd/UpdateCoursesCmd/UpdateSingleCourseCmd/UpdateCourseTypeCmd/UpdateCoursesToTypeCmd`
  - `bc-evaluation/contract`：`CourseScoreCO`、`EvaTeacherInfoCO`、`EvaConfig`
  - `bc-iam/contract`：`MenuConditionalQuery`
  - `bc-messaging-contract`：`GenericRequestMsg`
- ✅ **S0.1（`eva-client` 退出 root reactor；保持行为不变）**：选择方案 B：从 root `pom.xml` 的 `<modules>` 移除 `eva-client`（最小回归通过；落地提交：`ce07d75f`）。
- ✅ **S0.1（更彻底清理；保持行为不变）**：从仓库移除 `eva-client/` 目录（最小回归通过；落地提交：`de25e9fb`）。
- ✅ **S0.1（课程协议继续拆 `eva-client`）**：将课程查询 Query 对象 `CourseQuery/CourseConditionalQuery/MobileCourseQuery` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`84a6a536`）。
- ✅ **S0.1（课程协议继续拆 `eva-client`）**：将通用学期入参 `Term` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`f401dcb9`）。
- ✅ **S0.1（课程协议继续拆 `eva-client`）**：将学期协议接口 `ISemesterService` 与学期 CO `SemesterCO` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`7b5997c1`）。
- ✅ **S0.1（收敛依赖：`eva-domain` 去 `eva-client` 直依赖）**：在“可证实不再需要”的前提下移除 `eva-domain` → `eva-client` 的 Maven **直依赖**（保持行为不变；最小回归通过；落地提交：`9ff21249`）。
- ✅ **S0.1（课程协议继续拆 `eva-client`）**：将课程写侧命令 `edu.cuit.client.dto.cmd.course/*` 与导入课表 BO `CourseExcelBO` 从 `eva-client` 迁移到 `bc-course`（保持 `package` 不变）；并为 `eva-infra-shared` 补齐 `bc-course` 显式依赖以闭合编译依赖（保持行为不变；最小回归通过；落地提交：`8a591703`）。
- ✅ **S0.1（IAM 继续推进，去 `eva-client` 残留）**：将 IAM 专属接口 `IDepartmentService` 从 `eva-client` 迁移到 `bc-iam-contract`（`edu.cuit.bc.iam.application.contract.api.department`），并更新 `DepartmentController/DepartmentServiceImpl` 引用（保持行为不变；最小回归通过；落地提交：`656dc36e`）。
- ✅ **P1.2（评教域继续拆 `eva-client`，盘点）**：Serena 盘点确认 `eva-client` 下评教专属目录（`api/eva`、`dto/cmd/eva`、`dto/clientobject/eva`）已迁空；评教 BC 内部引用的 `edu.cuit.client.*` 仅作为“协议包名”，其物理归属已在 `bc-evaluation-contract/shared-kernel`（保持行为不变；最小回归通过；落地提交：`e643bac9`）。
- ✅ **P1.2-3（审计日志协议继续拆 `eva-client`）**：Serena 盘点 `bc-audit` 对 `eva-client` 的实际依赖面：仅 `SysLogBO` 来自 `eva-client`（其余 `PagingQuery/GenericConditionalQuery/PaginationQueryResultCO` 已在 `shared-kernel`，`ILogService/OperateLogCO/LogModuleCO` 已在 `bc-audit`；保持行为不变；最小回归通过；落地提交：`ba21bbea`）。
- ✅ **P1.2-3（审计日志协议继续拆 `eva-client`）**：将 `SysLogBO` 从 `eva-client` 迁移到 `eva-domain`（保持 `package edu.cuit.client.bo` 不变；保持行为不变；最小回归通过；落地提交：`734a3741`）。
- ✅ **P1.2-3（审计日志协议继续拆 `eva-client`）**：在 `SysLogBO` 迁移后，移除 `bc-audit` → `eva-client` 的 Maven **直依赖**（保持行为不变；最小回归通过；落地提交：`2fcb257c`）。
- ✅ **P1.2-2（审计日志协议继续拆 `eva-client`）**：用 Serena 盘点 `ILogService/OperateLogCO/LogModuleCO` 引用面后，将其从 `eva-client` 迁移到 `bc-audit`（保持 `package` 不变；保持行为不变）；并为 `bc-audit` 补齐 `shared-kernel` 显式依赖以保证编译行为不变（落地提交：`e1dbf2d4`）。
- ✅ **S0.1-7（IAM application 去 `eva-client` 直依赖）**：Serena 盘点 `bc-iam/application` 对 `edu.cuit.client.*` 的引用面（仅通用类型，如 `PagingQuery/GenericConditionalQuery/SimpleResultCO`）；因此移除 `bc-iam/application` → `eva-client` 的直依赖（保持行为不变；落地提交：`7371ab96`）。
- ✅ **P1.2-1（评教 application 去 `eva-client` 直依赖）**：Serena 盘点评教 `bc-evaluation/application` 对 `edu.cuit.client.*` 的引用面（仅 Port 接口引用）；因此移除 `bc-evaluation/application` → `eva-client` 的直依赖（保持行为不变；落地提交：`10e8eb0b`）。
- ✅ **S0.1-4（评教 contract 收敛依赖）**：迁移 `DateEvaNumCO/TimeEvaNumCO/MoreDateEvaNumCO/SimpleEvaPercentCO/SimplePercentCO/FormPropCO` 到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；落地提交：`c2d8a8b1`）。
- ✅ **S0.1-5（评教 contract 收敛依赖）**：迁移 `dto/data/course/CourseTime` 到 `shared-kernel`（保持 `package` 不变；保持行为不变；落地提交：`5f21b5ce`）。
- ✅ **S0.1-6（评教 contract 去直依赖）**：移除 `bc-evaluation-contract` → `eva-client` 的直依赖；为保持编译行为不变，补齐 `bc-iam-contract` 的显式依赖（`eva-base-common`、`commons-lang3`，避免隐式经由其它模块传递；落地提交：`cf2001ef`）。
- ✅ 三文档同步（以 Git 为准，不在文内滚动固化 commitId）：使用 `git log -n 1 -- NEXT_SESSION_HANDOFF.md`、`git log -n 1 -- DDD_REFACTOR_PLAN.md`、`git log -n 1 -- docs/DDD_REFACTOR_BACKLOG.md` 获取最新同步提交。
- ✅ **docs（Backlog 路线口径对齐）**：修正 `docs/DDD_REFACTOR_BACKLOG.md` 第 6 节 bc-messaging 的“下一步”口径：基础设施端口适配器（`Message*PortImpl`）已归位完成，下一步转入“依赖收敛/结构折叠”（保持行为不变）。
- ✅ 最小回归通过（Java17）：命令见 0.10。

### 2025-12-25（上一会话摘要，保留）

- ✅ 提交点 0（纯文档闭环）：补齐“条目 25”的定义/边界与验收口径（只改文档，不改代码；落地提交：`1adc80bd`）。
- ✅ 提交点 A（结构落点，不迁业务）：启动 `bc-ai-report` / `bc-audit` 最小 Maven 子模块骨架并接入组合根（落地提交：`a30a1ff9`）。
- ✅ 提交点 B（写侧收敛，选审计日志链路）：审计日志写入 `LogGatewayImpl.insertLog` 收敛为“用例 + 端口 + 端口适配器 + 旧 gateway 委托壳”（落地提交：`b0b72263`）。
  - 行为快照（必须保持）：旧 `LogGatewayImpl.insertLog` 仍在同一处通过 `CompletableFuture.runAsync(..., executor)` 异步触发；异步线程内的字段补齐与 `logMapper.insert` 已搬运到 `bc-audit` 的端口适配器（无异常文案/缓存副作用变化）。
- ✅ 提交点 C（读侧继续拆，统计主题）：从 `EvaQueryRepo` 抽出 `EvaStatisticsQueryRepo`，并将 `EvaStatisticsQueryPortImpl` 依赖收敛到该接口（统计口径/异常文案不变；落地提交：`d5b07247`）。
- ✅ 提交点 C2（读侧继续拆，记录主题）：从 `EvaQueryRepo` 抽出 `EvaRecordQueryRepo`，并将 `EvaRecordQueryPortImpl` 依赖收敛到该接口（口径/异常文案不变；落地提交：`cae1a15c`）。
- ✅ 提交点 C3（读侧继续拆，任务主题）：从 `EvaQueryRepo` 抽出 `EvaTaskQueryRepo`，并将 `EvaTaskQueryPortImpl` 依赖收敛到该接口（口径/异常文案不变；落地提交：`82427967`）。
- ✅ 提交点 C4（读侧继续拆，模板主题）：从 `EvaQueryRepo` 抽出 `EvaTemplateQueryRepo`，并将 `EvaTemplateQueryPortImpl` 依赖收敛到该接口（口径/异常文案不变；落地提交：`889ec9b0`）。
- ✅ 提交点 B2（AI 报告写链路，导出）：`AiCourseAnalysisService.exportDocData` 收敛为“用例 + 端口 + 端口适配器 + 旧入口委托壳”（日志/异常文案不变；落地提交：`c68b3174`）。
- ✅ 提交点 B3（AI 报告写链路，导出）：`AiCourseAnalysisService.exportDocData` 进一步退化为“纯委托壳”（把 userId 解析/analysis 编排从旧入口迁出；保持行为不变；落地提交：`7f4b3358`）。
- ✅ 提交点 B4（AI 报告写链路，analysis）：`AiCourseAnalysisService.analysis` 收敛为“用例 + 端口 + 端口适配器 + 旧入口委托壳”（保持 `@CheckSemId` 切面触发点不变；日志/异常文案不变；落地提交：`a8150e7f`）。
- ✅ 提交点 B5（AI 报告写链路，用户名解析）：`ExportAiReportDocByUsernameUseCase` 内部依赖收敛：将 username → userId 的查询抽为 `bc-ai-report` 端口 + `eva-app` 端口适配器，保持异常文案与日志顺序不变（落地提交：`d7df8657`）。
- ✅ 需求变更（BC 模块组织方式）：BC 采用“**单顶层聚合模块 + 内部 `domain/application/infrastructure` 子模块**”组织方式，不再新增 `bc-*-infra` 平铺模块；历史平铺模块后续按里程碑折叠归位（仅改文档口径，不改代码语义；落地提交：`940b65ad`）。
- ✅ 结构性里程碑 S0（`bc-iam` 试点，阶段 1）：将 `bc-iam` 折叠为顶层聚合模块 `bc-iam-parent`，并在其内部落地 `domain/application/infrastructure` 子模块；同时将历史平铺模块 `bc-iam-infra` 折叠归位到 `bc-iam/infrastructure`（artifactId/包名保持不变；最小回归通过；落地提交：`0b5c5383`）。
- ✅ 结构性里程碑 S0.1（拆解 `eva-client`，`bc-iam` 先行）：将 `eva-client` 下 IAM 协议对象（`api/user/*` + `dto/cmd/user/*`）迁移到 `bc-iam/contract` 子模块（包名归位到 `edu.cuit.bc.iam.application.contract...`），并全仓库更新引用（保持行为不变；最小回归通过；落地提交：`dc3727fa`）。
- ✅ 结构性里程碑 S0（`bc-evaluation` 试点）：将 `bc-evaluation` 折叠为顶层聚合模块 `bc-evaluation-parent`，并在其内部落地 `domain/application/infrastructure` 子模块；同时将历史平铺模块 `bc-evaluation-infra` 折叠归位到 `bc-evaluation/infrastructure`（artifactId/包名保持不变；最小回归通过；落地提交：`4db04d1c`）。
- ✅ 结构性里程碑 S0.1（拆解 `eva-client`，`bc-iam` 继续推进）：将 `dto/clientobject/user/*` 中与 IAM 直接相关的 8 个 CO（`UserInfoCO/UserDetailCO/RoleInfoCO/SimpleRoleInfoCO/MenuCO/GenericMenuSectionCO/RouterDetailCO/RouterMeta`）从 `eva-client` 迁移到 `bc-iam-contract`（包名归位到 `edu.cuit.bc.iam.application.contract.dto.clientobject.user`），并全仓库更新引用（保持行为不变；最小回归通过；落地提交：`c1a51199`）。
- ✅ P1（评教统计协议归属，继续拆 `eva-client`）：新增 `bc-evaluation-contract` 子模块，并将 `IEvaStatisticsService` + 未达标用户协议对象 `UnqualifiedUserInfoCO/UnqualifiedUserResultCO` 从 `eva-client` 迁移到 `bc-evaluation/contract`（保持 `package edu.cuit.client.*` 不变，仅物理归属与依赖收敛；保持行为不变；最小回归通过；落地提交：`978e3535`）。
- ✅ P1.1（评教域继续拆 `eva-client`，协议对象归属）：迁移 `edu.cuit.client.dto.clientobject.eva.*` + `UnqualifiedUserConditionalQuery` 到 `bc-evaluation/contract`（保持 `package` 不变）；并将评教 API 接口（`IEvaConfigService/IEvaRecordService/IEvaTaskService/IEvaTemplateService/IUserEvaService`）从 `eva-client` 迁移到 `bc-evaluation/contract`（保持 `package` 不变）；同时将课程域协议接口与 CO（`edu.cuit.client.api.course.*`、`CourseDetailCO/CourseModelCO/SingleCourseDetailCO`）从 `eva-client` 迁移到 `bc-course`，以避免 `eva-client` 反向依赖评教 CO（保持行为不变；最小回归通过；落地提交：`6eb0125d`）。
- ✅ S0.1（通用对象沉淀 shared-kernel，避免误迁）：用 Serena 盘点 `PagingQuery/GenericConditionalQuery/SimpleResultCO/PaginationQueryResultCO` 的跨 BC 引用范围，确认跨 BC 复用后，新增 `shared-kernel` 子模块并将上述通用类型（含 `ConditionalQuery`）从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`a25815b2`）。
- ✅ S0.1（收敛依赖，第一步）：`bc-iam-contract` / `bc-evaluation-contract` 已增加对 `shared-kernel` 的直依赖（暂保留 `eva-client` 以可回滚；最小回归通过；落地提交：`3a0ac086`）。
- ✅ S0.1（IAM 继续推进）：迁移 IAM 查询条件 `MenuConditionalQuery` 到 `bc-iam-contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`1eda37c9`）。
- ✅ P1.2（评教域继续拆 `eva-client`）：迁移评教查询条件 `EvaTaskConditionalQuery/EvaLogConditionalQuery` 到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`d02d5522`）。
- ✅ S0.1（通用对象沉淀 shared-kernel，继续收敛）：迁移 `ValidStatus/ValidStatusValidator` 到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`686de369`）。
- ✅ S0.1（收敛依赖，第二步）：`bc-iam-contract` 已去除对 `eva-client` 的直依赖（通过 `shared-kernel` + 必要的 BC contract 组合满足编译；保持行为不变；最小回归通过；落地提交：`8d673c17`）。
- ✅ S0.1-3（评教 contract 去依赖前置）：迁移评教 `dto/cmd/eva/*` 到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`2273ad61`）。
- ✅ S0.1-3（评教 contract 去依赖前置）：迁移 `EvaConfig` 到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`438d38bf`）。
- ✅ S0.1-4（评教 contract 收敛依赖，继续推进）：迁移 `DateEvaNumCO/TimeEvaNumCO/MoreDateEvaNumCO/SimpleEvaPercentCO/SimplePercentCO/FormPropCO` 到 `bc-evaluation/contract`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`c2d8a8b1`）。
- ✅ S0.1-5（评教 contract 收敛依赖，课程时间模型）：迁移 `dto/data/course/CourseTime` 到 `shared-kernel`（保持 `package` 不变；保持行为不变；最小回归通过；落地提交：`5f21b5ce`）。
- ✅ S0.1-6（评教 contract 去 `eva-client` 直依赖）：移除 `bc-evaluation-contract` → `eva-client` 的直依赖，并补齐 `bc-iam-contract` 的显式依赖（避免隐式经由其它模块传递；保持行为不变；最小回归通过；落地提交：`cf2001ef`）。
- ✅ 本次会话提交链（按发生顺序，便于回溯/回滚）：`4db04d1c`（S0 代码）→ `135b9e6b`（S0 三文档同步）→ `c1a51199`（S0.1 代码）→ `e093900f`（S0.1 三文档同步）→ `978e3535`（P1 代码）→ `4e9e22f3`（P1 三文档同步）→ `6eb0125d`（P1.1 代码）→ `a25815b2`（S0.1 通用对象沉淀代码）→ `3a0ac086`（S0.1 contract 显式依赖 shared-kernel 代码）→ `4994b6d0`（S0.1 三文档同步）→ `1eda37c9`（S0.1 IAM QueryCondition 迁移代码）→ `c2d8a8b1`（S0.1-4 评教 CO 迁移代码）→ `5e0ba929`（S0.1-4 三文档同步）→ `5f21b5ce`（S0.1-5 CourseTime 迁移代码）→ `547cab40`（S0.1-5 三文档同步）→ `cf2001ef`（S0.1-6 评教 contract 去依赖；三文档同步提交以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` / `git log -n 1 -- DDD_REFACTOR_PLAN.md` / `git log -n 1 -- docs/DDD_REFACTOR_BACKLOG.md` 为准）。
- ✅ 需求补充（拆解 `eva-client`，并允许改包名）：后续重构将把 `edu.cuit.client.*` 下 BO/CO/DTO 等对象按业务归属迁入对应 BC（允许调整包名以归位到 `bc-xxx/application` 的 `contract/dto`）；跨 BC 复用对象再沉淀到 shared-kernel（落地提交：`a4c6bae8`）。
- ✅ 文档补强（结构性里程碑路线）：进一步明确“一个 BC 一个顶层聚合模块 + `domain/application/infrastructure` 子模块”落地方式，并补齐拆 `eva-client` 的推荐拆分步骤（只改文档口径；落地提交：`0c87c31a`）。
- ✅ 提交点 C5-1（读侧实现继续拆，统计主题）：新增 `EvaStatisticsQueryRepository` 承接 `EvaStatisticsQueryRepo` 实现，`EvaQueryRepository` 中对应方法退化为委托（口径/异常文案不变；落地提交：`9e0a8d28`；三文档同步：`61b0dfa4`）。
- ✅ 提交点 C5-2（读侧实现继续拆，记录主题）：新增 `EvaRecordQueryRepository` 承接 `EvaRecordQueryRepo` 实现，`EvaQueryRepository` 中对应方法退化为委托（口径/异常文案不变；落地提交：`985f7802`；三文档同步：`68895003`）。
- ✅ 提交点 C5-3（读侧实现继续拆，任务主题）：新增 `EvaTaskQueryRepository` 承接 `EvaTaskQueryRepo` 实现，`EvaQueryRepository` 中对应方法退化为委托（口径/异常文案不变；落地提交：`d467c65e`；三文档同步：`ebff7002`）。
- ✅ 提交点 C5-4（读侧实现继续拆，模板主题）：新增 `EvaTemplateQueryRepository` 承接 `EvaTemplateQueryRepo` 实现，`EvaQueryRepository` 中模板相关方法退化为委托（口径/异常文案不变；落地提交：`a550675a`）。
- ✅ 评教 BC 自包含三层结构试点（阶段 1：引入 `bc-evaluation-infra`）：迁移评教读侧查询（QueryPortImpl + QueryRepo/Repository）从 `eva-infra` 到 `bc-evaluation-infra`，并将 course/eva DAL（DO/Mapper/XML）迁移到 `eva-infra-dal`、将 `CourseConvertor`/`EvaConvertor`/`EvaCacheConstants`/`CourseFormat` 迁移到 `eva-infra-shared`（均保持包名不变；保持行为不变；落地提交：`be6dc05c`）。
- ✅ 评教 BC 自包含三层结构试点（阶段 2：写侧 Repo 迁移）：迁移评教写侧端口适配器/Repo（`eva-infra/src/main/java/edu/cuit/infra/bcevaluation/repository/*`）到 `bc-evaluation-infra`，并将 `CalculateClassTime` 迁移到 `eva-infra-shared` 以保持 `bc-evaluation-infra` 不依赖 `eva-infra`（均保持包名/行为不变；落地提交：`24e7f6c9`）。
- ✅ 提交点 C-1（读侧门面加固，可选）：清理 `EvaQueryRepository` 中已无引用的历史私有实现/冗余依赖，使其成为纯委托壳（保持行为不变；落地提交：`73fc6c14`；三文档同步：`083b5807`）。
- ✅ 提交点 C-2-1（读侧仓储瘦身，可选）：清理 `EvaRecordQueryRepository` 无用 import（保持行为不变；落地提交：`e2a2a717`）。
- ✅ 提交点 C-2-2（读侧仓储瘦身，可选）：清理 `EvaRecordQueryRepository` 冗余通配 import（保持行为不变；落地提交：`8b76375f`）。
- ✅ 提交点 C-2-3（读侧仓储瘦身，可选）：清理 `EvaQueryRepo` 冗余 import（保持行为不变；落地提交：`4a317344`）。
- ✅ 提交点 C-2-4（读侧仓储瘦身，可选）：清理 `EvaStatisticsQueryRepo` 通配 import（保持行为不变；落地提交：`dba6e31d`）。
- ✅ 提交点 C-2-5（读侧仓储瘦身，可选）：使用 Serena 盘点评教读侧四主题仓储（`bc-evaluation/infrastructure/src/main/java/edu/cuit/infra/bcevaluation/query/*QueryRepository.java`）的私有字段/私有方法/内部类引用，未发现“仅定义未被调用”的可删项；因此将 C-2 视为“已无进一步可证实无引用项”并关闭（保持行为不变；落地提交：`5c1a03bc`）。
- ✅ 文档同步（以 Git 为准，不在文内滚动固化 commitId）：使用 `git log -n 1 -- NEXT_SESSION_HANDOFF.md`、`git log -n 1 -- DDD_REFACTOR_PLAN.md`、`git log -n 1 -- docs/DDD_REFACTOR_BACKLOG.md` 获取各文档的最新同步提交。
- ✅ 以上每步最小回归均已通过（Java17）：  
  - `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repositorya`

## 0.10 下一步拆分与里程碑/提交点（下一会话开始前先读完本节）

> 目标：确保“**能继续往前走**”且不丢失重构约束（只重构、不改语义；缓存/日志/异常文案/副作用顺序完全不变）。
>
> 规则提醒：每个小提交都必须做到：Serena 符号级定位/引用分析 → **最小回归** → `git commit` → 同步三份文档（本文件 + `DDD_REFACTOR_PLAN.md` + `docs/DDD_REFACTOR_BACKLOG.md`）。
>
> 阶段性策略微调（2025-12-29）：允许“微调”（仅结构性重构；不改业务语义；缓存/日志/异常文案/副作用顺序完全不变）；在“评教统计导出基础设施归位”与“课程课表解析归位/端口化”闭环后，下一会话主线切换为 **bc-messaging（组合根/监听器/应用侧适配器归位）**，仍按“小步可回滚 + 每步闭环”推进。
>

- 本次会话最新闭环（2025-12-30，便于续接）：  
  1) ✅ 统计读侧 `pageUnqualifiedUser`：分页结果组装已归位到 `EvaStatisticsQueryUseCase`，旧入口 `EvaStatisticsServiceImpl` 已退化为纯委托壳并移除对 `PaginationBizConvertor` 的依赖（`e97615e1` / `f4f3fcde`）。  
  2) ✅ 统计读侧 `type` 分支判断去重复：在 `EvaStatisticsQueryUseCase` 内部收敛 `type` 分发为 `dispatchByType(...)`，避免后续继续归位时出现分支口径漂移（保持行为不变；`38ce9ece`）。
  3) ✅ 统计读侧导出 `exportEvaStatistics`：引入导出端口 `EvaStatisticsExportPort` 并让旧入口委托 `EvaStatisticsQueryUseCase.exportEvaStatistics`（保持 `@CheckSemId` 触发点不变；行为不变；`0d15de60`）。
  4) ✅ 导出基础设施归位准备：`ExcelUtils` 已从 `eva-app` 迁移到 `eva-infra-shared`（包名不变），并补齐 `poi/poi-ooxml` 依赖，避免后续导出类迁移出现循环依赖（保持行为不变；`04009c85`）。
  5) ✅ 导出基础设施归位（统计导出基类）：`EvaStatisticsExporter` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（包名不变），并补齐 `bc-evaluation-infra` 对 `bc-course/bc-iam-contract` 的编译依赖以闭合类型引用（保持行为不变；`e8ca391c`）。
  6) ✅ 导出基础设施归位（统计导出装饰器）：`FillAverageScoreExporterDecorator` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（包名不变；`4e150984`）。
  7) ✅ 导出基础设施归位（统计导出装饰器）：`FillEvaRecordExporterDecorator` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（包名不变；`b3afcb11`）。
  8) ✅ bc-messaging：已完成“后置规划证据化”（仅文档，不落地代码），散落点与路线见 `DDD_REFACTOR_PLAN.md` 第 10.3 节（`4b05f515`）。
  9) ✅ 导出基础设施归位（统计导出装饰器）：`FillUserStatisticsExporterDecorator` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（包名不变；`e83600f6`）。
  10) ✅ 导出基础设施归位（统计导出工厂）：`EvaStatisticsExcelFactory` 已从 `eva-app` 迁移到 `bc-evaluation-infra`（异常文案/日志输出完全一致；`5b2c2223`）。
  11) ✅ 装配切换（统计导出端口）：`BcEvaluationConfiguration.evaStatisticsExportPort()` 已切换为委托 `bc-evaluation-infra` 的 `EvaStatisticsExportPortImpl`（内部仍调用 `EvaStatisticsExcelFactory.createExcelData`；保持行为不变；`565552fa`）。
  12) ✅ bc-course（课程）：课表 Excel/POI 解析归位到 `bc-course-infra`（保持 `package` 不变；异常文案/日志输出完全不变；`383dbf33`）。
  13) ✅ bc-course（课程）：课表解析端口化与依赖收敛：`eva-app` 调用侧改为依赖 `CourseExcelResolvePort`（由 `bc-course-infra` 适配器实现）；`eva-app` 不再编译期依赖 `bc-course-infra`（改为 runtime）；`start` 测试侧补齐 `bc-course-infra` test 依赖（保持行为不变；`5a7cd0a0`）。
  14) ✅ bc-messaging（消息域）：组合根归位：`BcMessagingConfiguration` 已从 `eva-app` 迁移到 `bc-messaging`（保持 `package edu.cuit.app.config` 不变；仅补齐 `bc-messaging` 对 `spring-context` 的依赖；保持行为不变；`4e3e2cf2`）。
  15) ✅ bc-messaging（消息域）：监听器归位（课程副作用）：`CourseOperationSideEffectsListener` 已从 `eva-app` 迁移到 `bc-messaging`（保持 `package edu.cuit.app.bcmessaging` 不变；保持行为不变；`22ee30e7`）。
  16) ✅ bc-messaging（消息域）：监听器归位（课程教师任务消息）：`CourseTeacherTaskMessagesListener` 已从 `eva-app` 迁移到 `bc-messaging`（保持 `package edu.cuit.app.bcmessaging` 不变；保持行为不变；`0987f96f`）。
  17) ✅ bc-messaging（消息域）：支撑类归位（消息发送组装）：`MsgResult` 已从 `eva-app` 迁移到 `bc-messaging-contract`（保持 `package edu.cuit.app.service.operate.course` 不变；保持行为不变；用于打破后续端口适配器迁移的 Maven 循环依赖；`31878b61`）。
  18) ✅ bc-messaging（消息域）：应用侧端口适配器归位（课程广播）：`CourseBroadcastPortAdapter` 已从 `eva-app` 迁移到 `bc-messaging`（保持 `package edu.cuit.app.bcmessaging.adapter` 不变；保持行为不变；`84ee070a`）。
  19) ✅ bc-messaging（消息域）：应用侧端口适配器归位（教师任务消息）：`TeacherTaskMessagePortAdapter` 已从 `eva-app` 迁移到 `bc-messaging`（保持 `package edu.cuit.app.bcmessaging.adapter` 不变；保持行为不变；`9ea14cff`）。
  20) ✅ bc-messaging（消息域）：应用侧端口适配器归位（评教消息清理）：`EvaMessageCleanupPortAdapter` 已从 `eva-app` 迁移到 `bc-messaging`，并将依赖类型从 `MsgServiceImpl` 收窄为 `IMsgService`（保持 `deleteEvaMsg(taskId, null)` 调用不变；事务语义由原 `MsgServiceImpl` 承接；保持行为不变；`73ab3f3c`）。

- 下一步建议（仍保持行为不变；每次只改 1 个类 + 1 个可运行回归）：  
  1) ✅ **评教统计导出基础设施归位**：本阶段已闭环（装饰器/工厂归位 + 导出端口装配切换均完成；且 `eva-app` 已移除 `poi/poi-ooxml` Maven 直依赖）。后续若要继续推进评教读侧解耦，请回到“统计用例归位（空对象兜底/默认值组装）每次迁 1 个方法簇”的节奏（仍保持行为不变）。
  2) **bc-messaging（下一会话主线，按 10.3 路线）**：从“组合根归位”开始，逐步把消息域装配/监听器/应用侧适配器从 `eva-app` 归位到 `bc-messaging`（建议保持 `package` 不变以降风险；每次只迁 1 个类；保持行为不变）。建议顺序（每步闭环=Serena→最小回归→commit→三文档同步）：
     - ✅ 已完成：组合根 `BcMessagingConfiguration` 归位（`4e3e2cf2`）
     - ✅ 已完成：监听器 `CourseOperationSideEffectsListener` 归位（`22ee30e7`）
     - ✅ 已完成：监听器 `CourseTeacherTaskMessagesListener` 归位（`0987f96f`）
     - ✅ 已完成：支撑类 `MsgResult` 归位（`31878b61`，当前位于 `bc-messaging-contract`）
     - ✅ 已完成：应用侧端口适配器 `CourseBroadcastPortAdapter` 归位（`84ee070a`）
     - ✅ 已完成：应用侧端口适配器 `EvaMessageCleanupPortAdapter` 归位（`73ab3f3c`）
     - ✅ 已完成：应用侧端口适配器 `TeacherTaskMessagePortAdapter` 归位（`9ea14cff`）
     - ✅ 已完成（前置，DAL/Convertor 归位）：`MsgTipDO/MsgTipMapper(+xml)` → `eva-infra-dal`；`MsgConvertor` → `eva-infra-shared`（保持 `package/namespace` 不变；保持行为不变）。
     - ✅ 已完成（基础设施端口适配器归位）：`MessageDeletionPortImpl/MessageReadPortImpl/MessageDisplayPortImpl/MessageInsertionPortImpl/MessageQueryPortImpl` → `bc-messaging`（保持 `package` 不变；保持行为不变）。
     - ✅ 依赖收敛准备：将事件枚举 `CourseOperationMessageMode` 下沉到 `bc-messaging-contract`（保持 `package` 不变；保持行为不变；`b2247e7f`）。
     - 下一步：继续把事件载荷逐个下沉到 `bc-messaging-contract`（本阶段已完成 `CourseOperationSideEffectsEvent` 与 `CourseTeacherTaskMessagesEvent`；下一步可评估将事件载荷完全下沉后，是否能把 `eva-app` 对 `bc-messaging` 的依赖收敛为仅依赖 `bc-messaging-contract`；每步只改 1 个点；保持行为不变）。

- ✅ 提交点 0（纯文档闭环）：已完成（落地提交：`1adc80bd`）。
- ✅ 提交点 A（结构落点，不迁业务）：已完成（落地提交：`a30a1ff9`）。
- ✅ 提交点 B（写侧收敛，挑 1 条链路）：已完成（审计日志写入 `LogGatewayImpl.insertLog`；落地提交：`b0b72263`）。
- ✅ 提交点 C（读侧继续拆，统计主题）：已完成第一步（`EvaStatisticsQueryRepo` 抽取 + 端口依赖收敛；落地提交：`d5b07247`）。
- ✅ 提交点 C2（读侧继续拆，记录主题）：已完成（`EvaRecordQueryRepo` 抽取 + 端口依赖收敛；落地提交：`cae1a15c`）。
- ✅ 提交点 C3（读侧继续拆，任务主题）：已完成（`EvaTaskQueryRepo` 抽取 + 端口依赖收敛；落地提交：`82427967`）。
- ✅ 提交点 C4（读侧继续拆，模板主题）：已完成（`EvaTemplateQueryRepo` 抽取 + 端口依赖收敛；落地提交：`889ec9b0`）。
- ✅ 提交点 B2（AI 报告写链路，导出）：已完成（`AiCourseAnalysisService.exportDocData` 收敛；落地提交：`c68b3174`）。
- ✅ 提交点 B3（可选，AI 报告写链路继续瘦身委托壳）：已完成（`AiCourseAnalysisService.exportDocData` 进一步退化为纯委托壳；落地提交：`7f4b3358`）。
- ✅ 提交点 B4（AI 报告写链路，analysis）：已完成（`AiCourseAnalysisService.analysis` 收敛为“用例+端口+适配器+委托壳”；落地提交：`a8150e7f`）。
- ✅ 提交点 B5（AI 报告写链路，用户名解析）：已完成（username → userId 查询抽为 `bc-ai-report` 端口 + `eva-app` 端口适配器；落地提交：`d7df8657`）。
- ✅ 提交点 C5-1（读侧实现继续拆，统计主题）：已完成（抽出 `EvaStatisticsQueryRepository`；落地提交：`9e0a8d28`；三文档同步：`61b0dfa4`）。
- ✅ 提交点 C5-2（读侧实现继续拆，记录主题）：已完成（抽出 `EvaRecordQueryRepository`；落地提交：`985f7802`；三文档同步：`68895003`）。
- ✅ 提交点 C5-3（读侧实现继续拆，任务主题）：已完成（抽出 `EvaTaskQueryRepository`；落地提交：`d467c65e`；三文档同步：`ebff7002`）。
- ✅ 提交点 C5-4（读侧实现继续拆，模板主题）：已完成（抽出 `EvaTemplateQueryRepository`；落地提交：`a550675a`）。
- ✅ 评教 BC 自包含三层结构试点（阶段 1：引入 `bc-evaluation-infra`）：已完成（评教读侧查询迁移 + course/eva DAL 迁移 + shared 迁移；落地提交：`be6dc05c`）。
- ✅ 评教 BC 自包含三层结构试点（阶段 2：写侧 Repo 迁移）：已完成（迁移 `eva-infra/src/main/java/edu/cuit/infra/bcevaluation/repository/*` 到 `bc-evaluation-infra`；并迁移 `CalculateClassTime` 到 `eva-infra-shared`，保持 `bc-evaluation-infra` 不依赖 `eva-infra`；落地提交：`24e7f6c9`）。
- ✅ 提交点 C-1（读侧门面加固，可选）：已完成（清理 `EvaQueryRepository` 为纯委托壳；落地提交：`73fc6c14`；三文档同步：`083b5807`）。

下一会话建议（按顺序执行；历史已完成项见下方 0.12 “总体进度概览”）：
0) **bc-messaging（依赖收敛后半段，优先，保持行为不变）**：当前已完成“事件枚举/载荷下沉到 `bc-messaging-contract` + `eva-app` 依赖面收敛为 `bc-messaging-contract`”（见 0.9）。下一步建议以“组合根兜底运行时依赖”为目标继续收敛依赖边界（每步闭环）：
   - 目标：让消息域的运行时装配由组合根承接（建议 `start`），并尽量减少 `eva-app/eva-infra` 对 `bc-messaging` 的编译期耦合（仍保持行为不变）。
   - 建议拆分为 3 个可回滚提交（每步：Serena → 最小回归 → commit → 三文档同步）：
     1) ✅ 已完成：组合根补齐运行时依赖：在 `start/pom.xml` 增加对 `bc-messaging` 的 `runtime` 依赖，确保 Spring 装配不变（保持行为不变；落地提交：`f23254ec`）。
     2) ✅ 已完成：依赖上推：移除 `eva-infra/pom.xml` 对 `bc-messaging` 的 `runtime` 依赖，把“运行时装配责任”从 `eva-infra` 上推到组合根（保持行为不变；落地提交：`507f95b2`）。
     3) 注意：`bc-messaging-contract` 当前包含 `MsgResult`（`@Component`，保持行为不变），因此其对 `spring-context` 的依赖暂不建议移除；若后续要移除，必须先将 `MsgResult` 的装配责任迁回 `bc-messaging` 或其它合适模块并闭环验证。

1) **评教读侧进一步解耦（优先，方向 A → B，保持行为不变）**：当前已完成“统计 QueryPort 细分 + 依赖收窄 + UseCase 归位起步 + 首个分支归位”（见 0.9：统计 `a1d6ccab/c19d8801/9b3c4e6a/db09d87b/22dccc70`；其余记录/任务/模板/导出链路进展见 0.9 的提交清单）。下一步建议拆分为更小的可回滚提交：
   - **A（继续收窄依赖）**：按同套路继续推进读侧其它主题（优先记录/任务/模板）：先做“子端口接口 + `extends`（不改实现/不改装配）”，再逐个把 `eva-app` 中的注入类型收窄为子端口（每次只改 1 个类 + 对应测试）。
	     - 进展（统计导出链路，2025-12-28）：已补齐 `EvaStatisticsCountAbEvaQueryPort`，并将导出基类 `EvaStatisticsExporter` 静态初始化中获取统计端口的依赖类型收窄到该子端口（保持 `SpringUtil.getBean(...)` 次数与顺序不变；见 0.9：`24b13138/7337d378`，保持行为不变）。
	     - 进展（记录主题，2025-12-28）：已完成记录 QueryPort 细分（5 子端口）+ 聚合端口 `extends`，并已收窄 `EvaRecordServiceImpl` 与 `UserServiceImpl` 的依赖类型（见 0.9：`39a4bafe/8b24d2f8` 等，保持行为不变）。
		     - ✅ 补充进展（记录主题，2025-12-28）：已证伪“全仓库仍存在对记录聚合端口 `EvaRecordQueryPort` 的注入点/调用点”（除端口定义与 `EvaRecordQueryPortImpl` 实现外无其它引用；Serena 证据见会话内盘点结论）。因此记录主题的“端口细分 + 应用层依赖类型收窄”阶段可视为已闭合；后续若出现新引用点，再按同套路逐一收窄（每次只改 1 个类 + 1 个可运行单测）。
	     - 进展（任务主题，2025-12-28）：已新增任务读侧子端口 `EvaTaskInfoQueryPort/EvaTaskPagingQueryPort/EvaTaskSelfQueryPort/EvaTaskCountQueryPort`，并让 `EvaTaskQueryPort` `extends` 这些子端口；已完成依赖类型收窄：`MsgServiceImpl` → `EvaTaskInfoQueryPort`、`EvaTaskServiceImpl` → `EvaTaskPagingQueryPort/EvaTaskSelfQueryPort/EvaTaskInfoQueryPort`（见 0.9，保持行为不变）。
	     - 进展（模板主题，2025-12-28）：已新增模板读侧子端口 `EvaTemplatePagingQueryPort/EvaTemplateAllQueryPort/EvaTemplateTaskTemplateQueryPort` 并让 `EvaTemplateQueryPort` `extends`；已完成依赖类型收窄：`EvaTemplateServiceImpl` → `EvaTemplatePagingQueryPort/EvaTemplateAllQueryPort/EvaTemplateTaskTemplateQueryPort`；并已用 Serena 证伪 `eva-app` 仍存在其它对 `EvaTemplateQueryPort` 的注入点/调用点（见 0.9：`a14d3c53/b86db7e4/e67fc47d`，保持行为不变）。
		     - 下一步建议（任务/模板主题，保持行为不变）：（可选）为 `EvaTaskServiceImpl` 补齐可重复的用例级回归（重点：异常文案/日志拼接保持不变），但涉及 `StpUtil` 静态登录态时需先固化“可重复”的登录态注入/隔离策略；模板主题在“端口细分 + 服务层依赖类型收窄 + 引用面证伪”阶段已闭合，后续若出现新的应用层引用点再按同套路逐一收窄即可（每次只改 1 个类 + 1 个可运行单测）。
		   - **B（用例归位深化）**：将 `EvaStatisticsQueryUseCase` 从“委托壳”逐步演进为“统计读侧用例编排落点”：建议每次只迁 1 个方法簇。✅ 已完成：`getEvaData` 的阈值计算/参数组装归位（落地：`8f4c07c5`）；unqualifiedUser 链路的“参数组装重载”（读取 `EvaConfigGateway.getEvaConfig()`，落地：`0a2fec4d`），且旧入口 `EvaStatisticsServiceImpl` 已委托重载并去除对 `EvaConfigGateway` 的直接依赖（落地：`21f6ad5b`）。✅ 已完成：`evaScoreStatisticsInfo` 的空对象兜底归位到 UseCase（补齐 `evaScoreStatisticsInfoOrEmpty`：`bce01df2`；旧入口委托该重载：`1bf3a4fe`）。✅ 已完成：`evaTemplateSituation` 的空对象兜底归位到 UseCase（补齐 `evaTemplateSituationOrEmpty`：`89b6b1ee`；旧入口委托该重载：`78abf1a1`）。✅ 已完成：`evaWeekAdd` 的空对象兜底归位到 UseCase（补齐 `evaWeekAddOrEmpty`：`5a8ac076`；旧入口委托该重载：`2a92ca0b`）。下一步建议（仍保持行为不变）：继续挑选统计读侧下一簇“默认值兜底/空对象组装”进行归位（仍按“每次只迁 1 个方法簇”的节奏）。
1) **条目 25（优先，写侧）**：AI 报告“剩余落库/记录写链路”已完成盘点并证伪（见 0.9 的证据清单）。后续请将条目 25 的执行重点切换为：**S0 折叠 `bc-ai-report`**（仅搬运/依赖收敛，保持行为不变）。
   - 补充进展（2025-12-27）：已将导出链路实现（`AiReportDocExportPortImpl` + `AiReportExporter`）、analysis 链路实现（`AiReportAnalysisPortImpl`）与 username→userId 端口适配器（`AiReportUserIdQueryPortImpl`）从 `eva-app` 归位到 `bc-ai-report`（保持 `package` 不变；保持行为不变；提交：`d1262c32`、`6f34e894`、`e2a608e2`），并进一步将 `edu.cuit.infra.ai.*` 从 `eva-infra` 归位到 `bc-ai-report`（保持 `package` 不变；保持行为不变；提交：`e2f2a7ff`）。
   - 补充进展（2025-12-27）：已将 `BcAiReportConfiguration` 与旧入口 `AiCourseAnalysisService` 归位到 `bc-ai-report`，并将 `@CheckSemId` 注解下沉到 `shared-kernel`（均保持 `package`/切面触发点/异常与日志行为不变；提交：`58c2f055`、`ca321a20`、`1c595052`）。
   - 补充进展（2025-12-27）：S0 已完成阶段 1/2：`bc-ai-report-parent` + 内部子模块已落地；端口适配器/导出实现/AI 基础设施已归位 `bc-ai-report/infrastructure`，并补齐 `eva-app` → `bc-ai-report-infra` 依赖（保持行为不变；提交：`e14f4f7a`、`444c7aca`）。
   - 说明：由于本次盘点已证伪，本条目的“证据清单”已补齐；无需再“凭感觉继续拆”，直接推进 S0 更能带来结构性收益且可回滚。
2) **结构性里程碑 S0（次优先）**：`bc-audit` 已完成阶段 1/2/3（提交：`81594308`、`d7858d7a`、`06ec6f3d`）。下一步建议（可选，保持行为不变）：若仍发现 `bc-audit-infra` 从 `eva-infra` 引用类型（理论上应为 0），则继续按“先抽离到 `eva-infra-dal`/`eva-infra-shared` 或归位到 `bc-audit/infrastructure`”的小步策略处理；否则将本项视为闭环并转入评教读侧解耦。
   - 实现路径（已落地，供后续复用；每步：符号级盘点 → 最小回归 → commit → 三文档同步）：
     - 盘点 `bc-audit-infra` 仍从 `eva-infra` 引用的类型清单（聚焦 `edu.cuit.infra.dal.database.*`、`edu.cuit.infra.convertor.*`、`edu.cuit.infra.gateway.*`），形成“依赖闭包”证据。
     - 将 `sys_log` 相关 DO/Mapper/XML 按“保持包名/namespace/SQL 不变”的策略迁移到 `eva-infra-dal`。
     - 将 `LogInsertionPortImpl` 仍依赖的转换/工具类按最小闭包迁移到 `eva-infra-shared`（或直接归位 `bc-audit/infrastructure`，二选一；保持行为不变）。
     - 最后将 `bc-audit-infra` 的 Maven 依赖由 `eva-infra` 收敛为更小的 `eva-infra-dal` + `eva-infra-shared`（或完全不依赖 `eva-infra`），并确保 `eva-app` 装配不变。
   - 参考：`bc-template`/`bc-course`/`bc-ai-report` 已完成折叠归位（提交：`65091516`、`e90ad03b`、`e14f4f7a/444c7aca`）。
3) （可选/后置）**评教读侧进一步解耦**：在不改变统计口径/异常文案前提下，按用例维度继续细化 QueryService/QueryPort（保持行为不变）。
   - 最小切入点建议（仍保持行为不变，便于可回滚）：优先从 `bc-evaluation/application/port/EvaStatisticsQueryPort` 着手，先按“用例簇”拆出 2~3 个更细粒度的子 Port（**仅新增接口 + `extends`，不改实现/不改装配**），再按“每次只迁 1 个用例方法”的节奏逐步收窄上层依赖类型（避免一次性大改导致口径/异常漂移）。
   - 风险提示：`eva-app` 中存在通过 `SpringUtil.getBean(...)` 获取 QueryPort 的静态初始化路径（例如导出相关类）；如需调整依赖类型或移动类归属，务必保持 Bean 初始化时机与副作用顺序不变。

## 0.12 当前总体进度概览（2025-12-29，更新至 `HEAD`）

> 用于回答“现在总进度到哪了”，避免每次会话重新盘点。

- **bc-iam（系统管理/IAM）**：已完成大量写侧/读侧收敛；历史上通过平铺过渡模块 `bc-iam-infra` 完成适配器归属与去 `eva-infra` 依赖闭环（见历史提交点）。**按 2025-12-24 需求变更**：已将该过渡模块折叠归位到 `bc-iam/infrastructure` 子模块（落地提交：`0b5c5383`），并新增 `bc-iam-contract` 子模块承接 IAM 协议对象迁移（落地提交：`dc3727fa`）。
- **bc-evaluation（评教）**：写侧主链路（任务发布/删除/模板）已按“用例+端口+适配器+委托壳”收敛；历史上通过平铺过渡模块 `bc-evaluation-infra` 完成读侧迁移与写侧 Repo 迁移，并通过 `eva-infra-shared`/`eva-infra-dal` 解决跨 BC 共享（均保持包名/行为不变）。**按 2025-12-24 需求变更**：已将该过渡模块折叠归位到 `bc-evaluation/infrastructure` 子模块（落地提交：`4db04d1c`）。补充进展（2025-12-29）：统计读侧用例归位深化已完成 `evaScoreStatisticsInfo/evaTemplateSituation/evaWeekAdd` 三簇空对象兜底归位到 `EvaStatisticsQueryUseCase`，并让旧入口 `EvaStatisticsServiceImpl` 退化为委托壳（见 0.9，保持行为不变）。
- **bc-evaluation（评教，contract）**：已新增 `bc-evaluation-contract` 并迁移评教统计接口 `IEvaStatisticsService` + 未达标用户协议对象 `UnqualifiedUserInfoCO/UnqualifiedUserResultCO`（保持 `package edu.cuit.client.*` 不变，仅物理归属与依赖收敛；保持行为不变；落地提交：`978e3535`）；并继续迁移评教统计/表单相关 CO（`DateEvaNumCO/TimeEvaNumCO/MoreDateEvaNumCO/SimpleEvaPercentCO/SimplePercentCO/FormPropCO`，保持 `package` 不变；保持行为不变；`c2d8a8b1`），以及课程时间模型 `CourseTime`（沉淀到 `shared-kernel`，保持 `package` 不变；保持行为不变；`5f21b5ce`）；并已在“可证实不再需要”的前提下移除 `bc-evaluation-contract` → `eva-client` 直依赖（`cf2001ef`）。
- **bc-audit（审计日志）**：已完成 `LogGatewayImpl.insertLog` 写链路收敛（异步触发点保留在旧入口，落库与字段补齐在端口适配器）；并已将审计日志协议对象 `ILogService/OperateLogCO/LogModuleCO` 从 `eva-client` 迁移到 `bc-audit`（保持 `package` 不变；保持行为不变；`e1dbf2d4`）。
- **bc-audit（审计日志，S0 折叠归位）**：已完成阶段 1/2/3：引入 `bc-audit-parent` + 内部子模块；将 `LogInsertionPortImpl` 归位 `bc-audit/infrastructure`；并已抽离 `sys_log` 相关 DAL/Converter 以移除 `bc-audit-infra` → `eva-infra` 过渡依赖（保持行为不变；提交：`81594308`、`d7858d7a`、`06ec6f3d`）。
- **bc-ai-report（AI 报告）**：已完成模块骨架接入组合根；导出写链路、analysis 与用户名解析已逐步收敛为“用例+端口+端口适配器+旧入口委托壳”（保持行为不变）。补充进展：导出链路实现（`AiReportDocExportPortImpl` + `AiReportExporter`）与 analysis 端口适配器（`AiReportAnalysisPortImpl`）已从 `eva-app` 归位到 `bc-ai-report`（保持 `package` 不变；保持行为不变；提交：`d1262c32`、`6f34e894`）；username→userId 端口适配器（`AiReportUserIdQueryPortImpl`）亦已归位（提交：`e2a608e2`）；并进一步将 `edu.cuit.infra.ai.*` 从 `eva-infra` 归位到 `bc-ai-report` 且移除 `bc-ai-report` → `eva-infra` 编译期依赖（提交：`e2f2a7ff`）。按 2025-12-24 需求变更：后续不再新增 `bc-ai-report-infra` 平铺模块，新增适配器归位到 `bc-ai-report/` 内部 `infrastructure` 子模块（或先落在 `eva-app`，再按里程碑折叠归位）。
- **bc-ai-report（AI 报告，S0 折叠归位）**：已完成阶段 1：引入 `bc-ai-report-parent` + 内部 `domain/application/infrastructure` 子模块；应用层 artifactId 仍为 `bc-ai-report`（保持行为不变；提交：`e14f4f7a`）。
- **bc-ai-report（AI 报告，S0 折叠归位，补充）**：已完成阶段 2：端口适配器/导出实现/AI 基础设施归位 `bc-ai-report/infrastructure` 子模块，并补齐 `eva-app` → `bc-ai-report-infra` 依赖以保证装配（保持行为不变；提交：`444c7aca`）。
- **bc-ai-report（AI 报告，补充）**：旧入口 `AiCourseAnalysisService` 与组合根 `BcAiReportConfiguration` 已归位到 `bc-ai-report`；为保持 `@CheckSemId` 切面语义不变，`edu.cuit.app.aop.CheckSemId` 已下沉到 `shared-kernel`（均保持包名/异常/日志/副作用顺序不变；提交：`58c2f055`、`ca321a20`、`1c595052`）。
- **bc-template（模板）**：已完成 S0 结构折叠为 `bc-template-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-template`；包名不变；保持行为不变；落地提交：`65091516`）。
- **bc-course（课程）**：已完成 S0 折叠为 `bc-course-parent` + 内部 `domain/application/infrastructure` 子模块（应用层 artifactId 仍为 `bc-course`；包名不变；保持行为不变；提交：`e90ad03b`）。读侧 `CourseQueryGatewayImpl` 已退化委托壳并抽出 QueryRepo/Repository（保持行为不变）。
- **bc-messaging（消息）**：当前仍以平铺模块为主（`bc-messaging/src`），尚未完成“一个 BC 一个聚合模块 + 内部三层子模块”的结构折叠；但协议对象迁移已存在 `bc-messaging-contract`，后续建议优先把 `bc-messaging` 也按 S0 结构折叠（仅搬运/依赖收敛，保持行为不变）。

**阶段性判断（用于回答“什么时候能收敛 eva-* / 什么时候算 DDD”）**
- **结构上的 DDD（可验收）**：所有 `bc-*` 都具备稳定的 `domain/application/infrastructure` 结构落点（模块或至少 package 自包含），旧入口逐步退化为委托壳，出站依赖全部通过 Port 统一表达；此时可认为“结构 DDD”基本成型。
- **eva-* 技术切片可显著退场的门槛**：当 Controller/Service/UseCase 入口基本都落在各 BC（或仅保留极薄的兼容入口），且 `eva-app/eva-infra` 不再承载业务编排，仅保留 `shared-kernel` + 必要的 `eva-infra-dal/eva-infra-shared`（或等价的 BC 内 infrastructure）时，才适合开始“批量下线/合并 eva-* 模块”。
- **语义上的 DDD（长期）**：聚合边界、领域事件、一致性规则、读写分离（投影/查询模型）等逐步落地，才能称为“语义 DDD 完整”；建议在结构边界稳定后再推进（风险更低、回滚更容易）。

### 条目 25（定义 / 边界 / 验收口径）

> 背景：历史上“条目 24/25/26”作为会话推进索引使用；其中 24/26 已落地，但 25 的定义容易被“提交点 A/B/C”新拆分吸收，导致不同会话对边界理解不一致。

- **定义**：**AI 报告 / 审计日志模块化试点**。目标是为后续写侧收敛提供稳定落点（BC 模块骨架 + 组合根 wiring），并按既有套路先收敛 1 条写链路（行为保持不变）。
- **包含范围（本条目对应提交点 A + B）**：
  - **A：结构落点**：新增 `bc-ai-report` / `bc-audit` Maven 子模块骨架（只建目录/依赖/包结构/最小配置），并接入组合根（仅 wiring，不迁业务语义）。
  - **B：写侧收敛（挑 1 条链路）**：从 AI 报告或审计日志中选 1 条写链路，按“UseCase + Port + Port Adapter + 旧 gateway 委托壳”收敛，确保旧入口对外接口/注解触发点不变。
- **不包含范围**：
  - 不包含 `EvaQueryRepo` 的读侧拆分（提交点 C）。
  - 不做任何业务语义调整；**缓存/日志/异常文案/副作用顺序完全不变**。
- **验收口径（必须同时满足）**：
  1) `mvn -pl start -am test ...` 最小回归通过（命令见本节下方）；  
  2) 组合根 wiring 完整（Spring Bean 可被扫描/装配，且不引入 Maven 循环依赖）；  
  3) 提交点 B 所选链路：旧 gateway 退化为委托壳后，**缓存注解/日志/异常文案/副作用顺序完全不变**（以变更前行为快照对照）。
- 最小回归命令（每步结束必跑，Java17）：  
  - `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repositorya`

## 0.11 新对话开启提示词（直接复制粘贴）

### 推荐版（不固化 commitId，优先复制本段）

你是资深全栈架构师/技术导师，只用中文回答。

仓库：/home/lystran/programming/java/web/eva-backend  
先确认（必须）：
1) `git branch --show-current` 必须输出 `ddd`
2) `git rev-parse HEAD`；且 `git merge-base --is-ancestor 2e4c4923 HEAD` 退出码必须为 `0`
3) 当前交接基线（用于对照）：以 `git rev-parse --short HEAD` 输出为准
4) 本会话交接文档基线：以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准（不在提示词里固化 commitId）

强约束（必须严格执行）：
- 只做重构，不改业务语义；缓存/日志/异常文案/副作用顺序完全不变
- 允许微调：仅限结构性重构（收窄依赖/拆接口/搬运默认值兜底/类归位），仍以“行为不变”为最高约束
- 必须使用 Serena 做符号级定位与引用分析（若 MCP 工具出现 `TimeoutError` 等不可用情况：需在 0.9 记录“降级原因 + 可复现的 `rg` 证据”，并在下一会话优先排查恢复）
- 每个小步骤闭环顺序：Serena（符号级定位/引用分析）→ 最小回归 → git commit → 同步 NEXT_SESSION_HANDOFF.md / DDD_REFACTOR_PLAN.md / docs/DDD_REFACTOR_BACKLOG.md（保持行为不变）
- 每次结束对话前：先写清“下一步拆分与里程碑/提交点”

开始前按顺序阅读（重点章节同旧要求）：
1) NEXT_SESSION_HANDOFF.md（重点看 0.0、0.9、0.10、0.11，以及条目 24/25/26）
2) DDD_REFACTOR_PLAN.md（重点看 10.2/10.3/10.4）
3) docs/DDD_REFACTOR_BACKLOG.md（重点看 4.2、4.3、6）
4) data/ 与 data/doc/（如需核对表/字段语义）

本会话目标（按顺序执行；每步闭环=Serena→最小回归→提交→三文档同步；保持行为不变）：

0) ✅ 已闭环（避免重复劳动）：
   - 评教统计导出基础设施归位：装饰器/工厂归位 + `EvaStatisticsExportPort` 装配切换 + `eva-app` 移除 POI Maven 直依赖（见 0.9）。
   - bc-course 课表解析：POI 解析归位到 `bc-course-infra` + `CourseExcelResolvePort` 端口化 + 调用侧依赖收敛（见 0.9）。
   - bc-messaging（消息域）：组合根 `BcMessagingConfiguration` 已归位到 `bc-messaging`（保持 `package` 不变；见 0.9）。
   - bc-messaging（消息域）：监听器 `CourseOperationSideEffectsListener` 已归位到 `bc-messaging`（保持 `package` 不变；见 0.9）。
   - bc-messaging（消息域）：监听器 `CourseTeacherTaskMessagesListener` 已归位到 `bc-messaging`（保持 `package` 不变；见 0.9）。
   - bc-messaging（消息域）：支撑类 `MsgResult` 已归位到 `bc-messaging-contract`（保持 `package` 不变；见 0.9）。
   - bc-messaging（消息域）：应用侧端口适配器 `CourseBroadcastPortAdapter` 已归位到 `bc-messaging`（保持 `package` 不变；见 0.9）。
   - bc-messaging（消息域）：应用侧端口适配器 `TeacherTaskMessagePortAdapter` 已归位到 `bc-messaging`（保持 `package` 不变；见 0.9）。
   - bc-messaging（消息域）：应用侧端口适配器 `EvaMessageCleanupPortAdapter` 已归位到 `bc-messaging`（保持 `package` 不变；见 0.9）。
   - bc-messaging（消息域）：应用侧事件载荷已下沉到 `bc-messaging-contract`：`CourseOperationMessageMode/CourseOperationSideEffectsEvent/CourseTeacherTaskMessagesEvent`（均保持 `package edu.cuit.bc.messaging.application.event` 不变；见 0.9）。
   - bc-messaging（消息域）：依赖收敛阶段性闭环：`eva-app` 已将对 `bc-messaging` 的编译期依赖收敛为仅依赖 `bc-messaging-contract`（仅用于事件载荷类型；见 0.9）。

1) **bc-messaging（下一会话主线，按 10.3 路线）**：先组合根 → 再监听器/应用侧适配器 → 最后基础设施端口适配器与依赖收敛（每步只迁 1 个类；保持行为不变）。建议顺序：
   1.1) ✅ 已完成：迁 `eva-app/src/main/java/edu/cuit/app/config/BcMessagingConfiguration.java` → `bc-messaging`（保持 `package edu.cuit.app.config` 不变；Bean 定义与装配顺序不变）。
   1.2) ✅ 已完成：迁监听器 `CourseOperationSideEffectsListener` → `bc-messaging`（保持 `package` 不变）。
   1.3) ✅ 已完成：迁监听器 `CourseTeacherTaskMessagesListener` → `bc-messaging`（保持 `package` 不变）。
   1.4) ✅ 已完成：迁应用侧端口适配器 `CourseBroadcastPortAdapter` → `bc-messaging`（保持 `package` 不变；依赖 `MsgResult`（位于 `bc-messaging-contract`），因此先归位了 `MsgResult` 以避免 Maven 循环依赖）。
   1.5) ✅ 已完成：迁应用侧端口适配器 `TeacherTaskMessagePortAdapter` → `bc-messaging`（保持 `package` 不变；依赖 `MsgResult`（位于 `bc-messaging-contract`））。
   1.6) ✅ 已完成：迁应用侧端口适配器 `EvaMessageCleanupPortAdapter` → `bc-messaging`（保持 `package` 不变；将依赖类型收窄为 `IMsgService` 以避免 Maven 循环依赖；保持行为不变）。
   1.7) （后置）再推进 `eva-infra/src/main/java/edu/cuit/infra/bcmessaging/adapter/*PortImpl.java` 的归位与依赖收敛（保持行为不变；细节见 `DDD_REFACTOR_PLAN.md` 10.3）：
        - ✅ 1.7.0（前置，DAL 归位）：`MsgTipDO` 已归位到 `eva-infra-dal`（保持 `package` 不变；见 0.9）。
        - ✅ 1.7.1（前置，DAL 归位）：`MsgTipMapper`（含 `MsgTipMapper.xml`）已归位到 `eva-infra-dal`（保持 `package/namespace` 不变；见 0.9）。
        - ✅ 1.7.2（基础设施端口适配器归位）：`MessageDeletionPortImpl` 已归位到 `bc-messaging`，并在 `bc-messaging` 补齐对 `eva-infra-dal` 的依赖以闭合编译与装配（保持行为不变；见 0.9）。
        - ✅ 1.7.3（基础设施端口适配器归位）：`MessageReadPortImpl` 已归位到 `bc-messaging`（保持行为不变；见 0.9）。
        - ✅ 1.7.4（基础设施端口适配器归位）：`MessageDisplayPortImpl` 已归位到 `bc-messaging`（保持行为不变；见 0.9）。
        - ✅ 1.7.5（基础设施端口适配器归位）：`MessageInsertionPortImpl` 已归位到 `bc-messaging`（保持行为不变；见 0.9）。
        - ✅ 1.7.6（基础设施端口适配器归位）：`MessageQueryPortImpl` 已归位到 `bc-messaging`（保持行为不变；见 0.9）。
        - ✅ 1.7.7（依赖收敛准备）：事件枚举/载荷已下沉到 `bc-messaging-contract`（保持 `package` 不变；见 0.9）。
        - ✅ 1.7.8（依赖收敛）：`eva-app` 的 Maven 依赖已从 `bc-messaging` 收敛为 `bc-messaging-contract`（保持行为不变；见 0.9）。
        - 下一步建议（依赖收敛后半段，仍保持行为不变；每步只做 1 个小改动并闭环）：
          1) Serena 证伪：`eva-infra` 对 `bc-messaging` 不再存在编译期引用；且“运行时装配责任”已由组合根 `start` 承接（见 0.9/0.10）。
          2) ✅ 已完成：组合根承接运行时装配：在 `start/pom.xml` 增加对 `bc-messaging` 的 `runtime` 依赖（保持行为不变；落地提交：`f23254ec`）。
          3) ✅ 已完成：移除 `eva-infra/pom.xml` 对 `bc-messaging` 的 `runtime` 依赖，把“运行时装配责任”上推到组合根（保持行为不变；落地提交：`507f95b2`）。

2) （可选/后置）**评教读侧用例归位深化（统计）**：继续按“每次只迁 1 个方法簇”的节奏归位默认值兜底/空对象组装（保持行为不变）。

3) （可选/后置）**条目 25 / S0（AI 报告）**：消息域推进顺利后，再回到 `bc-ai-report` 的 S0 做“仅搬运/依赖收敛”（保持行为不变）。

已闭环（用于避免重复劳动）：
- ✅ S0.1：`eva-client` 已从主干依赖链彻底退出（含：来源证伪 + 退出 reactor + 目录从仓库移除；保持行为不变；落地提交以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准）。

最小回归命令（每步结束必跑，Java17）：
export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" \
&& mvn -pl start -am test \
-Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest \
-Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repositorya

（补充建议，非替代）：若本步改动涉及 `UserServiceImpl.getOneUserScore`，额外运行：  
`export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl start -am test -Dtest=edu.cuit.app.user.UserServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repositorya`

（需要定位最近会话做了什么：以 NEXT_SESSION_HANDOFF.md 的 0.9 增量总结为准；需要定位落地提交：用 `git log -n 1 -- <文档>` 或按日期范围查看 `git log --oneline`，不要把 commitId 写回提示词。）

---

### 旧版（含 commitId，不建议复制，仅历史参考）

你是资深全栈架构师/技术导师，只用中文回答。

仓库：/home/lystran/programming/java/web/eva-backend  
先确认：分支必须是 ddd；HEAD 必须 >= 2e4c4923（运行 `git rev-parse HEAD` 确认）。
当前交接基线（用于对照）：以 `git rev-parse --short HEAD` 输出为准（本文档最后同步提交以 `git log -n 1 -- NEXT_SESSION_HANDOFF.md` 为准，不在文内固化 commitId，避免后续会话滚动更新遗漏）。

强约束（必须严格执行）：
- 只做重构，不改业务语义；缓存/日志/异常文案/副作用顺序完全不变
- 必须使用 Serena 做符号级定位与引用分析
- 每个小步骤闭环顺序：Serena（符号级定位/引用分析）→ 最小回归 → git commit → 同步 NEXT_SESSION_HANDOFF.md / DDD_REFACTOR_PLAN.md / docs/DDD_REFACTOR_BACKLOG.md（保持行为不变）
- 每次结束对话前：先写清“下一步拆分与里程碑/提交点”

开始前按顺序阅读（重点章节同旧要求）：
1) NEXT_SESSION_HANDOFF.md（重点看 0.0、0.9、0.10、0.11，以及条目 24/25/26）
2) DDD_REFACTOR_PLAN.md（重点看 10.2/10.3/10.4）
3) docs/DDD_REFACTOR_BACKLOG.md（重点看 4.2、4.3、6）
4) data/ 与 data/doc/（如需核对表/字段语义）

本会话目标（优先做这个）：
- **S0.1（下一步优先：收敛 `eva-domain` → `eva-client` 依赖）**：以 `eva-domain/pom.xml` 的 `eva-client` 依赖为收敛口，优先迁移课程/学期/消息等仍留在 `eva-client` 的协议对象（先 Serena 盘点 `eva-domain` 的 `import edu.cuit.client.*` 清单），最终在“可证实不再需要”的前提下移除该依赖（保持行为不变；每步=最小回归+提交+三文档同步）。
- **S0.1（IAM 继续推进，后续）**：已推进：迁移 IAM 专属接口 `IDepartmentService` 到 `bc-iam-contract`（保持行为不变；`656dc36e`）；后续继续用 Serena 盘点 `eva-client` 残留对象在 IAM 的引用范围，若为 IAM 专属则继续迁移到 `bc-iam-contract`（或更合适的 BC）（保持行为不变）。
- ✅ **P1.2（评教域继续拆 `eva-client`，盘点）**：已闭环：Serena 盘点确认 `eva-client` 下评教专属目录已迁空（保持行为不变；`e643bac9`），后续更可能只剩“依赖面收敛/迁移其它 BC 残留对象”的推进。
- ✅ **P1.2-3（审计日志协议继续拆 `eva-client`）**：已闭环：迁移 `SysLogBO` 到 `eva-domain` 并移除 `bc-audit` → `eva-client` Maven 直依赖（保持行为不变；`734a3741/2fcb257c`）。

当前状态（已闭环）：
- 提交点 0：条目 25 定义/边界/验收口径已补齐（`1adc80bd`）
- 提交点 A：`bc-ai-report` / `bc-audit` 最小骨架已接入组合根（`a30a1ff9`）
- 提交点 B：审计日志写入 `LogGatewayImpl.insertLog` 已按“用例+端口+适配器+委托壳”收敛（`b0b72263`）
- P1.2-2：审计日志协议对象 `ILogService/OperateLogCO/LogModuleCO` 已迁移到 `bc-audit`（保持 `package` 不变；保持行为不变；`e1dbf2d4`）
- 提交点 C（统计主题）：已抽出 `EvaStatisticsQueryRepo` 并收敛端口依赖（`d5b07247`）
- 提交点 C2（记录主题）：已抽出 `EvaRecordQueryRepo` 并收敛端口依赖（`cae1a15c`）
- 提交点 C3（任务主题）：已抽出 `EvaTaskQueryRepo` 并收敛端口依赖（`82427967`）
- 提交点 C4（模板主题）：已抽出 `EvaTemplateQueryRepo` 并收敛端口依赖（`889ec9b0`）
- 提交点 B2（AI 报告导出）：`AiCourseAnalysisService.exportDocData` 已按“用例+端口+端口适配器+委托壳”收敛（`c68b3174`）
- 提交点 B3（AI 报告导出）：`AiCourseAnalysisService.exportDocData` 已进一步退化为纯委托壳（`7f4b3358`）
- 提交点 B4（AI 报告 analysis）：`AiCourseAnalysisService.analysis` 已按“用例+端口+端口适配器+委托壳”收敛（`a8150e7f`）
- 提交点 B5（AI 报告用户名解析）：`ExportAiReportDocByUsernameUseCase` 的 username → userId 查询已抽为 `bc-ai-report` 端口 + `eva-app` 端口适配器（`d7df8657`）
- 提交点 C5-1（统计主题实现拆分）：已抽出 `EvaStatisticsQueryRepository`（`9e0a8d28`）
- 提交点 C5-2（记录主题实现拆分）：已抽出 `EvaRecordQueryRepository`（`985f7802`）
- 提交点 C5-3（任务主题实现拆分）：已抽出 `EvaTaskQueryRepository`（`d467c65e`）
- 提交点 C5-4（模板主题实现拆分）：已抽出 `EvaTemplateQueryRepository`（`a550675a`）
- 提交点 D1（评教 BC 自包含三层结构试点，阶段 1）：已引入 `bc-evaluation-infra` 并迁移评教读侧查询实现（保持包名/行为不变；落地提交：`be6dc05c`）
  - 同步：course/eva DAL（DO/Mapper/XML）已迁移到 `eva-infra-dal`；`CourseConvertor`/`EvaConvertor`/`EvaCacheConstants`/`CourseFormat` 已迁移到 `eva-infra-shared`（均保持包名/namespace 不变）
  - 三文档同步提交：`4e52d74c/53832c45`
- 提交点 D2（评教 BC 自包含三层结构试点，阶段 2）：已迁移评教写侧 Repo（`eva-infra/src/main/java/edu/cuit/infra/bcevaluation/repository/*`）到 `bc-evaluation-infra`（保持包名/行为不变；落地提交：`24e7f6c9`）
  - 同步：为保持 `bc-evaluation-infra` 不依赖 `eva-infra`，已将 `CalculateClassTime` 迁移到 `eva-infra-shared`（保持包名不变）
- 提交点 C-1（读侧门面加固，可选）：已清理 `EvaQueryRepository` 为纯委托壳（移除已无引用的历史私有实现/冗余依赖；保持行为不变；落地提交：`73fc6c14`；三文档同步：`083b5807`）
- 提交点 C-2（读侧仓储瘦身，可选）：已完成 C-2-5 并关闭（盘点评教四主题 QueryRepository 未发现可证实无引用项，保持行为不变；落地：`e2a2a717/8b76375f/4a317344/dba6e31d/5c1a03bc`）
- S0.1：已新增 `shared-kernel` 子模块，并将 `PagingQuery/ConditionalQuery/GenericConditionalQuery/SimpleResultCO/PaginationQueryResultCO` 从 `eva-client` 迁移到 `shared-kernel`（保持 `package` 不变；保持行为不变；`a25815b2`）
- S0.1：`bc-iam-contract` / `bc-evaluation-contract` 已增加对 `shared-kernel` 的直依赖（暂保留 `eva-client` 以可回滚；`3a0ac086`）
- S0.1：已迁移 IAM `MenuConditionalQuery` 到 `bc-iam-contract`（保持 `package` 不变；`1eda37c9`）
- P1.2：已迁移评教 `EvaTaskConditionalQuery/EvaLogConditionalQuery` 到 `bc-evaluation/contract`（保持 `package` 不变；`d02d5522`）
- S0.1：已迁移 `ValidStatus/ValidStatusValidator` 到 `shared-kernel`（保持 `package` 不变；`686de369`）
- S0.1：`bc-iam-contract` 已去除对 `eva-client` 的直依赖（`8d673c17`）
- S0.1-7：`bc-iam/application` 已去除对 `eva-client` 的直依赖（保持行为不变；`7371ab96`）
- P1.2-1：`bc-evaluation/application` 已去除对 `eva-client` 的直依赖（保持行为不变；`10e8eb0b`）
- P1.2-2：审计日志协议对象已迁移到 `bc-audit`（保持 `package` 不变；保持行为不变；`e1dbf2d4`）
- S0.1：已迁移评教 `dto/cmd/eva/*` 到 `bc-evaluation/contract`（保持 `package` 不变；`2273ad61`）
- S0.1：已迁移 `EvaConfig` 到 `bc-evaluation/contract`（保持 `package` 不变；`438d38bf`）
- S0.1：已迁移评教统计/表单相关 CO（`DateEvaNumCO/TimeEvaNumCO/MoreDateEvaNumCO/SimpleEvaPercentCO/SimplePercentCO/FormPropCO`）到 `bc-evaluation/contract`（保持 `package` 不变；`c2d8a8b1`）
- S0.1：已将课程时间模型 `dto/data/course/CourseTime` 迁移到 `shared-kernel`（保持 `package` 不变；`5f21b5ce`）
- S0.1：已在“可证实不再需要”的前提下移除 `bc-evaluation-contract` → `eva-client` 直依赖，并补齐 `bc-iam-contract` 的显式依赖（保持行为不变；`cf2001ef`）
- P1.1：已迁移 `edu.cuit.client.dto.clientobject.eva.*` + `UnqualifiedUserConditionalQuery` 到 `bc-evaluation/contract`（保持 `package` 不变）；并迁移评教 API 接口（`IEvaConfigService/IEvaRecordService/IEvaTaskService/IEvaTemplateService/IUserEvaService`）到 `bc-evaluation/contract`（保持 `package` 不变）；并将课程域协议接口与 CO（`edu.cuit.client.api.course.*`、`CourseDetailCO/CourseModelCO/SingleCourseDetailCO`）迁移到 `bc-course`（保持行为不变；`6eb0125d`）
- 结构性里程碑 S0：`bc-iam` 已折叠为 `bc-iam-parent` + `domain/application/contract/infrastructure` 子模块，且 `bc-iam-infra` 已折叠归位到 `bc-iam/infrastructure`（`0b5c5383`）
- 结构性里程碑 S0.1：`eva-client` 下 IAM 协议对象（`api/user/*` + `dto/cmd/user/*`）已迁移到 `bc-iam/contract`（`dc3727fa`）
- 结构性里程碑 S0：`bc-evaluation` 已折叠为 `bc-evaluation-parent` + `domain/application/contract/infrastructure` 子模块，且 `bc-evaluation-infra` 已折叠归位到 `bc-evaluation/infrastructure`（`4db04d1c`）
- 结构性里程碑 S0.1：已迁移 IAM `dto/clientobject/user/*` 中与 IAM 直接相关的 8 个 CO 到 `bc-iam-contract`（包名归位；`c1a51199`）
- P1：已新增 `bc-evaluation-contract` 并迁移 `IEvaStatisticsService` + `UnqualifiedUserInfoCO/UnqualifiedUserResultCO` 到 `bc-evaluation/contract`（保持 `package` 不变；`978e3535`）

下一步提交点（建议优先级）：
1) **S0.1（下一步优先：收敛 `eva-domain` → `eva-client` 依赖）**：
   - ✅ 进展：已移除 `eva-domain` → `eva-client` Maven **直依赖**（`9ff21249`）。
   - 下一步建议（保持行为不变；每步可回滚）：继续用 Serena 盘点 `eva-domain` 中 `import edu.cuit.client.*` 的类型清单，并逐一核对“类型来源是否已在 `shared-kernel` / 各 BC contract / `eva-domain`”。若发现仍有未归位的小簇类型，则按业务归属迁移到对应 BC（例如 `bc-course`/`bc-messaging`/`bc-ai-report`）或 `shared-kernel`（每步=最小回归+提交+三文档同步）。
2) **S0.1（IAM 继续推进）**：继续迁移 IAM 专属 query/condition/CO（保持行为不变；避免新代码回流 `eva-client`；必要时先用 Serena 盘点残留引用面再决定迁移/沉淀）。
   - ✅ 已完成：移除 `bc-iam/application` → `eva-client` 的直依赖（保持行为不变；`7371ab96`）。
   - ✅ 已完成：迁移 IAM 专属接口 `IDepartmentService` 从 `eva-client` 到 `bc-iam-contract`（包名归位到 `edu.cuit.bc.iam.application.contract.api.department`；保持行为不变；`656dc36e`）。
   - 下一步：Serena 继续盘点 IAM 模块引用的 `edu.cuit.client.*` 类型范围；若为 IAM 专属则迁移到 `bc-iam-contract`（或更合适的 BC），若为跨 BC 通用则继续沉淀到 `shared-kernel`（保持行为不变）。
3) ✅ **P1.2（评教域继续拆 `eva-client`，盘点）**：已闭环：评教专属目录已迁空；后续更可能只剩“依赖面收敛”推进（保持行为不变；`e643bac9`）。
4) **条目 25（后续）**：AI 报告继续挑选剩余写链路（保存/落库/记录等）按同套路收敛（保持行为不变；参考 `docs/DDD_REFACTOR_BACKLOG.md` 第 6 节）。

当前 `eva-client` 状态（以 Git 为准）：
- `eva-client/` 目录已从仓库移除（S0.1 已闭环；如需回滚通过 Git 提交点即可）。

每步最小回归命令（每步结束都跑）：
export JAVA_HOME=\"$HOME/.sdkman/candidates/java/17.0.17-zulu\" && export PATH=\"$JAVA_HOME/bin:$PATH\" \\
&& mvn -pl start -am test \\
-Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest \\
-Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository

工具调用简报（用于继续 S0.1 的引用盘点）：
- Serena `search_for_pattern`：用于快速盘点跨 BC/跨模块引用（例如 `import\\s+edu\\.cuit\\.client\\.`），以便形成“迁移清单”（日期=2025-12-26）。
- Serena `find_symbol`/`find_referencing_symbols`：用于对目标类型（Query/CO/Cmd/Data/Validator）做符号级定位与引用分析（确保“只重构不改行为”）。
- Serena `find_referencing_symbols`：本次已用于盘点 `SemesterCO/UpdateCourseCmd/CourseExcelBO` 的跨模块引用面（日期=2025-12-26）。

## 0.7 本次会话增量总结（2025-12-21，更新至 `HEAD`）

- ✅ 系统管理写侧继续收敛：**角色写侧剩余入口**收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 收敛范围（本次条目 24）：`RoleUpdateGatewayImpl.updateRoleInfo/updateRoleStatus/deleteRole/createRole`
  - 行为快照（供回归对照，顺序必须保持）：  
    - `updateRoleInfo`：`checkRoleId` →（`cmd.getStatus() == 0` 时）`checkDefaultRole` → `roleMapper.updateById` → `handleRoleUpdateCache` → `LogUtils.logContent(tmp.getRoleName() + "角色(" + tmp.getId() + ")的信息")`
    - `updateRoleStatus`：`checkRoleId` → `checkDefaultRole` → `roleMapper.update(roleUpdate)` → `handleRoleUpdateCache` → `LogUtils.logContent(tmp.getRoleName() + " 角色(" + tmp.getId() + ")的状态")`
    - `deleteRole`：`checkRoleId` → `checkDefaultRole` → `roleMapper.deleteById` → `userRoleMapper.delete` → `roleMenuMapper.delete` → `handleRoleUpdateCache` → `LogUtils.logContent(tmp.getRoleName() + "角色(" + tmp.getId() + ")")`
    - `createRole`：`roleConverter.toRoleDO` →（若重名）`throw BizException("角色名称已存在")` → `roleMapper.insert` → `handleRoleUpdateCache(roleId)`
    - `handleRoleUpdateCache`：依次失效 `ALL_ROLE/ONE_ROLE/ROLE_MENU`，再按 `sys_user_role.role_id -> user_id` 失效 `ONE_USER_ID/ONE_USER_USERNAME`（用户名 key 允许 `null`）。
  - 关键落地点（便于快速定位）：
    - 用例：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/UpdateRoleInfoUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/UpdateRoleStatusUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/DeleteRoleUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/CreateRoleUseCase.java`
    - 端口：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/RoleInfoUpdatePort.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/RoleStatusUpdatePort.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/RoleDeletionPort.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/RoleCreationPort.java`
    - 端口适配器：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/RoleWritePortImpl.java`
    - 旧 gateway（已退化委托壳）：`eva-infra/src/main/java/edu/cuit/infra/gateway/impl/user/RoleUpdateGatewayImpl.java`
    - 组合根：`eva-app/src/main/java/edu/cuit/app/config/BcIamConfiguration.java`
  - 落地提交：`64fadb20`
- ✅ 最小回归已通过（Java17）：
  - `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

## 0.8 本次会话增量总结（2025-12-21，更新至 `HEAD`）

- ✅ 中期里程碑推进：引入 `bc-iam-infra` Maven 子模块骨架，并完成 `bciam/adapter/*` 端口适配器迁移（保持行为不变）。
  - 组合根：`eva-app` 已依赖 `bc-iam-infra`，确保后续迁移的 Spring Bean 仍在运行时 classpath 上。
  - 落地提交：`42a6f66f`
- ✅ 迁移第一步：`UserBasicQueryPortImpl` 已从 `eva-infra` 迁移到 `bc-iam-infra`（包名/类名/行为保持不变）。
  - 落地提交：`070068ec`
- ✅ 迁移第二步：`RoleWritePortImpl` 已从 `eva-infra` 迁移到 `bc-iam-infra`（包名/类名/行为保持不变）。
  - 落地提交：`03ceb685`
- ✅ 迁移第三步：`UserMenuCacheInvalidationPortImpl` 已从 `eva-infra` 迁移到 `bc-iam-infra`（包名/类名/行为保持不变）。
  - 落地提交：`02b3e8aa`
- ✅ 迁移第四步：`MenuWritePortImpl` 已从 `eva-infra` 迁移到 `bc-iam-infra`（包名/类名/行为保持不变）。
  - 落地提交：`6b9d2ce7`
- ✅ 迁移第五步：用户写侧端口适配器已分批迁移到 `bc-iam-infra`（包名/类名/行为保持不变）。
  - 范围：`UserCreationPortImpl/UserInfoUpdatePortImpl/UserStatusUpdatePortImpl/UserDeletionPortImpl`
  - 落地提交：`5aecc747`
- ✅ 迁移第六步：用户查询/分配端口适配器已迁移到 `bc-iam-infra`（包名/类名/行为保持不变）。
  - 范围：`UserRoleAssignmentPortImpl/UserDirectoryQueryPortImpl/UserEntityQueryPortImpl`
  - 落地提交：`1c3d4b8c`
  - 下一步拆分与里程碑/提交点（每步一条 commit；每步跑最小回归；每步完成更新三份文档）：
    1) Serena：确认 `eva-infra/src/main/java/edu/cuit/infra/bciam/adapter` 不再残留 IAM 端口适配器（以及是否存在旧的 Spring 配置硬引用）。
    2) 验证：补充一次“启动期校验”（可选，若不跑全量启动则至少保持最小回归通过）。
    3) 下一阶段（后置）：评估将 IAM DAL（DO/Mapper）从 `eva-infra` 拆出到 `bc-iam-infra` 或独立 `shared-infra-dal`（仍保持行为不变、先搬运后整理）。

## 0.6 本次会话增量总结（2025-12-21，更新至 `HEAD`）

- ✅ 系统管理写侧继续收敛：**菜单写侧主链路**收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 收敛范围（本次条目 23）：`MenuUpdateGatewayImpl.updateMenuInfo/deleteMenu/deleteMultipleMenu/createMenu`
  - 行为快照（供回归对照）：
    - 缓存注解不变：`@LocalCacheInvalidateContainer/@LocalCacheInvalidate` 仍保留在旧 `MenuUpdateGatewayImpl` 方法上，area/key 与触发时机不变。
    - `deleteMenu` 链路不变：递归删除顺序不变；根节点 `handleUserMenuCache(menuId)` 仍触发两次（一次来自递归末尾、一次来自 `deleteMenu` 方法末尾）。
    - 异常/日志不变：`BizException("父菜单ID: ... 不存在")`、`BizException("菜单id: ... 不存在")`；`LogUtils.logContent(...)` 文案与时机不变。
  - 关键落地点（便于快速定位）：
    - 用例：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/UpdateMenuInfoUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/DeleteMenuUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/DeleteMultipleMenuUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/CreateMenuUseCase.java`
    - 端口：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/MenuInfoUpdatePort.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/MenuDeletionPort.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/MenuBatchDeletionPort.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/MenuCreationPort.java`
    - 端口适配器：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/MenuWritePortImpl.java`
    - 旧 gateway（已退化委托壳）：`eva-infra/src/main/java/edu/cuit/infra/gateway/impl/user/MenuUpdateGatewayImpl.java`
    - 组合根：`eva-app/src/main/java/edu/cuit/app/config/BcIamConfiguration.java`
  - 落地提交：`f022c415`
- ✅ 最小回归已通过（Java17）：
  - `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

## 0.5 本次会话增量总结（2025-12-21，更新至 `HEAD`）

- ✅ 系统管理写侧继续收敛：**角色/菜单缓存与权限变更副作用**收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 收敛范围（本次条目 21）：`RoleUpdateGatewayImpl.assignPerms/deleteMultipleRole`、`MenuUpdateGatewayImpl.handleUserMenuCache`
  - 落地提交链：`f8838951/91d13db4/7fce88b8/d6e3bed1/4d650166/46e666f9`
- ✅ 最小回归已通过（Java17）：
  - `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

## 0.4 本次会话增量总结（2025-12-21，更新至 `HEAD`）

- ✅ 系统管理读侧继续收敛：`UserQueryGatewayImpl.findIdByUsername/findUsernameById/getUserStatus/isUsernameExist` 收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 落地提交链：`9f664229/38384628/de662d1c/8a74faf5`
- ✅ 系统管理读侧继续收敛：`UserQueryGatewayImpl.findAllUserId/findAllUsername/allUser/getUserRoleIds` 收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 落地提交链：`56bbafcf/7e5f0a74/bc5fb3c6/6a1332b0`
- ✅ 系统管理写侧继续收敛：角色/菜单缓存与权限变更副作用收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 落地提交链：`f8838951/91d13db4/7fce88b8/d6e3bed1/4d650166/46e666f9`
- ✅ 文档同步（交接/计划/Backlog）：
  - `15cbfefa/c312575c/c641f86b/f8838951`
- ✅ 最小回归已通过（Java17）：
  - `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

- ✅ 条目 26（`bc-iam-infra` 阶段 2：IAM DAL 抽离）已完成：已用 Serena 盘点 IAM DAL 依赖清单并完成迁移闭环，最终移除 `bc-iam-infra` → `eva-infra` 依赖（保持行为不变）。
  - **Mapper（当前仍在 `eva-infra`）**：`SysUserMapper`、`SysUserRoleMapper`、`SysRoleMapper`、`SysRoleMenuMapper`、`SysMenuMapper`
  - **DO（当前仍在 `eva-infra`）**：`SysUserDO`、`SysUserRoleDO`、`SysRoleDO`、`SysRoleMenuDO`、`SysMenuDO`
  - **XML（当前仍在 `eva-infra`）**：`eva-infra/src/main/resources/mapper/user/SysUserMapper.xml`、`SysUserRoleMapper.xml`、`SysRoleMapper.xml`、`SysRoleMenuMapper.xml`、`SysMenuMapper.xml`
  - 依赖来源（便于后续搬迁对照）：Mapper/DO 均在 `eva-infra/src/main/java/edu/cuit/infra/dal/database/(mapper|dataobject)/user/`；XML 均在 `eva-infra/src/main/resources/mapper/user/`
  - ✅ 已完成（条目 26-2）：在 `bc-iam-infra` 创建 DAL 包骨架与资源目录（不迁代码；仅作为后续迁移落点；保持行为不变）。
    - Java 包骨架：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/database/dataobject/user/package-info.java`、`bc-iam/infrastructure/src/main/java/edu/cuit/infra/dal/database/mapper/user/package-info.java`
    - 资源目录占位：`bc-iam/infrastructure/src/main/resources/mapper/user/.gitkeep`
  - ✅ 已完成（条目 26-3）：新增独立 DAL 子模块 `eva-infra-dal`，并先迁移 `SysUser*`（DO/Mapper/XML）到该模块（保持包名/namespace/SQL 不变；保持行为不变）。
    - 说明：Serena 引用分析确认 `SysUserMapper` 被 `eva-infra` 内多个模块（course/eva/log/department…）直接使用；若直接迁入 `bc-iam-infra` 并从 `eva-infra` 删除会引入 Maven 循环依赖风险，因此先抽离为共享 DAL 模块以最小可回滚方式推进。
    - 新模块：`eva-infra-dal/pom.xml`
    - Java：`eva-infra-dal/src/main/java/edu/cuit/infra/dal/database/dataobject/user/SysUserDO.java`、`SysUserRoleDO.java`；`eva-infra-dal/src/main/java/edu/cuit/infra/dal/database/mapper/user/SysUserMapper.java`、`SysUserRoleMapper.java`
    - XML：`eva-infra-dal/src/main/resources/mapper/user/SysUserMapper.xml`、`SysUserRoleMapper.xml`
  - ✅ 已完成（条目 26-4）：继续迁移 `SysRole*`/`SysRoleMenu*`（DO/Mapper/XML）到 `eva-infra-dal`（保持包名/namespace/SQL 不变；保持行为不变）。
    - Java：`eva-infra-dal/src/main/java/edu/cuit/infra/dal/database/dataobject/user/SysRoleDO.java`、`SysRoleMenuDO.java`；`eva-infra-dal/src/main/java/edu/cuit/infra/dal/database/mapper/user/SysRoleMapper.java`、`SysRoleMenuMapper.java`
    - XML：`eva-infra-dal/src/main/resources/mapper/user/SysRoleMapper.xml`、`SysRoleMenuMapper.xml`
  - ✅ 已完成（条目 26-5）：继续迁移 `SysMenu*`（DO/Mapper/XML）到 `eva-infra-dal`（保持包名/namespace/SQL 不变；保持行为不变）。
    - Java：`eva-infra-dal/src/main/java/edu/cuit/infra/dal/database/dataobject/user/SysMenuDO.java`；`eva-infra-dal/src/main/java/edu/cuit/infra/dal/database/mapper/user/SysMenuMapper.java`
    - XML：`eva-infra-dal/src/main/resources/mapper/user/SysMenuMapper.xml`
  - 下一步里程碑（每步一条 commit；每步跑最小回归；保持行为不变）：逐步把 `bc-iam-infra` 对 `eva-infra` 的依赖收敛为更小的 shared 模块集合（最终可移除）
	    - ✅ 已完成（条目 26-6-1）：盘点 `bc-iam-infra` 仍依赖 `eva-infra` 的类型清单（为后续去依赖做最小闭包拆分；保持行为不变）：
	      - 转换器：`edu.cuit.infra.convertor.PaginationConverter`（已迁至 `eva-infra-shared`，落地：`54d5fecd`）、`edu.cuit.infra.convertor.user.{MenuConvertor,RoleConverter,UserConverter}`（已迁至 `eva-infra-shared`，落地：`6c798f1b`）、`edu.cuit.infra.convertor.user.LdapUserConvertor`（已迁至 `eva-infra-shared`，落地：`0dc0ddc8`）
	      - 缓存常量：`edu.cuit.infra.enums.cache.{UserCacheConstants,CourseCacheConstants}`
	      - LDAP 相关：`edu.cuit.infra.dal.ldap.{dataobject,repo}.*`（已迁至 `eva-infra-shared`，落地：`aca70b8b`）、`edu.cuit.infra.util.EvaLdapUtils` + `edu.cuit.infra.{enums.LdapConstants,property.EvaLdapProperties}`（已迁至 `eva-infra-shared`，落地：`3165180c`）
	      - 查询工具：`edu.cuit.infra.util.QueryUtils`
    - ✅ 已完成（条目 26-6-2a）：新增 shared 子模块骨架 `eva-infra-shared`（不迁代码，仅作为后续抽离落点；保持行为不变）。
      - 新模块：`eva-infra-shared/pom.xml`
    - ✅ 已完成（条目 26-6-2b1）：迁移 IAM 相关缓存常量到 `eva-infra-shared`（保持包名不变；保持行为不变）。
      - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/enums/cache/UserCacheConstants.java`、`CourseCacheConstants.java`
      - 依赖：`eva-infra-shared/pom.xml` 增加 `spring-context`（保持 `@Component` 语义不变）；`eva-infra/pom.xml` 与 `bc-iam-infra/pom.xml` 增加对 `eva-infra-shared` 的依赖
    - ✅ 已完成（条目 26-6-2b2）：迁移查询工具 `QueryUtils` 到 `eva-infra-shared`（保持包名不变；保持行为不变）。
      - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/util/QueryUtils.java`
      - 依赖：`eva-infra-shared/pom.xml` 增加对 `eva-client` 与 `zym-spring-boot-starter-jdbc` 的依赖（保持 MyBatis-Plus QueryWrapper/Page 等 API 不变）
	    - ✅ 已完成（条目 26-6-2c1）：迁移 `EntityFactory` 到 `eva-infra-shared`（保持包名不变；保持行为不变）。
	      - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/EntityFactory.java`
	      - 依赖：`eva-infra-shared/pom.xml` 增加 `mapstruct-plus-spring-boot-starter`；并增加对 `eva-domain` 的依赖以保留 `hutool SpringUtil` 与 `cola SysException` 的依赖来源（行为不变）
	    - ✅ 已完成（条目 26-6-2c2）：迁移 `PaginationConverter` 到 `eva-infra-shared`（保持包名不变；保持行为不变）。
	      - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/PaginationConverter.java`
	      - 依赖：`eva-infra-shared/pom.xml` 增加对 `eva-infra-dal` 的依赖以保持编译闭包（行为不变）
	      - 落地提交：`54d5fecd`
	    - ✅ 已完成（条目 26-6-2c3）：迁移 `convertor.user` 的非 LDAP 转换器到 `eva-infra-shared`（保持包名不变；保持行为不变）。
	      - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/user/{MenuConvertor,RoleConverter,UserConverter}.java`
	      - 落地提交：`6c798f1b`
	    - ✅ 已完成（条目 26-6-2d1）：迁移 LDAP DO/Repo 到 `eva-infra-shared`（保持包名不变；保持行为不变）。
	      - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/dal/ldap/dataobject/{LdapGroupDO,LdapPersonDO}.java`、`eva-infra-shared/src/main/java/edu/cuit/infra/dal/ldap/repo/{LdapGroupRepo,LdapPersonRepo}.java`
	      - 依赖：`eva-infra-shared/pom.xml` 增加 `spring-boot-starter-data-ldap`
	      - 落地提交：`aca70b8b`
	    - ✅ 已完成（条目 26-6-2d2）：迁移 `EvaLdapUtils` 相关类型到 `eva-infra-shared`（保持包名不变；保持行为不变）。
	      - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/util/EvaLdapUtils.java`、`eva-infra-shared/src/main/java/edu/cuit/infra/enums/LdapConstants.java`、`eva-infra-shared/src/main/java/edu/cuit/infra/property/EvaLdapProperties.java`
	      - 落地提交：`3165180c`
	    - ✅ 已完成（条目 26-6-2d3）：迁移 `LdapUserConvertor` 到 `eva-infra-shared`（保持包名不变；保持行为不变）。
	      - Java：`eva-infra-shared/src/main/java/edu/cuit/infra/convertor/user/LdapUserConvertor.java`
	      - 落地提交：`0dc0ddc8`
	    - 条目 26-6-2 拆分提交点（均已完成；每步均跑最小回归；保持行为不变）：
	      - ✅ 条目 26-6-2d1：迁移 `edu.cuit.infra.dal.ldap.*`（Repo + DO）到 `eva-infra-shared`（保持包名不变；落地：`aca70b8b`）
	      - ✅ 条目 26-6-2d2：迁移 `edu.cuit.infra.util.EvaLdapUtils` + `edu.cuit.infra.{enums.LdapConstants,property.EvaLdapProperties}` 到 `eva-infra-shared`（保持包名不变；落地：`3165180c`）
	      - ✅ 条目 26-6-2d3：迁移 `edu.cuit.infra.convertor.user.LdapUserConvertor` 到 `eva-infra-shared`（保持包名不变；落地：`0dc0ddc8`）
	    - ✅ 已完成（条目 26-6-3）：移除 `bc-iam-infra` 对 `eva-infra` 的 Maven 依赖（保持行为不变）。
	      - 依赖：`bc-iam-infra/pom.xml` 移除 `eva-infra`，补齐 `zym-spring-boot-starter-cache` + `zym-spring-boot-starter-logging` 编译依赖（行为不变）
	      - 落地提交：`2ad911ea`

## 0.3 本次会话增量总结（2025-12-21，更新至 `HEAD`）

- ✅ IAM 域写侧继续收敛：`UserUpdateGatewayImpl.deleteUser` 收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 落地提交链：`5f08151c/e23c810a/cccd75a3/2846c689`
- ✅ 系统管理读侧渐进收敛：`UserQueryGatewayImpl.fileUserEntity` 收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 落地提交链：`3e6f2cb2/8c245098/92a9beb3`（文档同步：`0a7802e2`）
  - 行为快照（供回归对照）：
    - 异常不变：菜单查询仍可能抛出 `SysException("菜单查询出错，请联系管理员")`，且仍会 `log.error("菜单查询出错", sysException)`；
    - 缓存不变：`findById/findByUsername/page` 的 `@LocalCached` 仍保留在旧 `UserQueryGatewayImpl` 上，缓存命中/回源语义不变；
    - API 不变：对外仍通过 `UserQueryGateway` 暴露 `findById/findByUsername/page`。
- ✅ 系统管理读侧继续收敛：`UserQueryGatewayImpl.findIdByUsername/findUsernameById/getUserStatus/isUsernameExist` 收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 落地提交链：`9f664229/38384628/de662d1c/8a74faf5`
  - 行为快照（供回归对照）：
    - 缓存读取语义不变：仍优先读取 `LocalCacheManager.getCache`，area 分别为 `ONE_USER_USERNAME/ONE_USER_ID`，key 分别为 `username` 与 `String.valueOf(id)`；
    - `findIdByUsername/findUsernameById` 返回语义不变：缓存命中则 `Optional.ofNullable(cachedUser.get().getId()/getUsername())`；缓存未命中（含 `null` 或 `Optional.empty()`）则回源 DB 并保持 `Optional.ofNullable(selectOne(...)).map(...)`；
    - `getUserStatus` 返回语义不变：缓存命中则 `Optional.ofNullable(cachedUser.get().getStatus())`；缓存未命中回源 DB 并保持 `Optional.ofNullable(selectOne(...).getStatus())`（用户不存在时仍可能触发历史 NPE）；
    - `isUsernameExist` 语义不变：缓存命中直接返回 `true`；否则走 `userMapper.exists(...)`。
- ✅ 系统管理读侧继续收敛：`UserQueryGatewayImpl.findAllUserId/findAllUsername/allUser/getUserRoleIds` 收敛到 `bc-iam`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳；保持行为不变）。
  - 落地提交链：`56bbafcf/7e5f0a74/bc5fb3c6/6a1332b0`
  - 行为快照（供回归对照）：
    - 缓存不变：`@LocalCached` 仍保留在旧 `UserQueryGatewayImpl` 上（key/area 与命中语义不变）。
    - 查询字段不变：`findAllUserId` 仅 select `id`；`findAllUsername` 仅 select `username`；`allUser` 仅 select `id,name` 并沿用 `userConverter.toUserSimpleResult` 映射。
    - 角色查询不变：`getUserRoleIds` 仍为原 join 条件与返回映射（仅搬运到端口适配器）。
- ✅ 最小回归已通过（Java17）：
  - `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

## 0.2 本次会话增量总结（2025-12-20，更新至 `HEAD`）

- ✅ 评教模板新增/修改写侧收敛到 `bc-evaluation`（新增用例 + 端口 + `eva-infra` 端口适配器，并切换 `eva-app` 入口；行为不变）。
  - 落地提交：`ea03dbd3 refactor(evaluation): 评教模板新增/修改写侧收敛到bc-evaluation`
- ✅ 清理旧 `EvaUpdateGatewayImpl.putEvaTemplate` 遗留实现（避免提交评教写侧旧代码回潮；行为不变）。
  - 落地提交：`12279f3f refactor(evaluation): 清理旧EvaUpdateGatewayImpl提交评教遗留实现`
- ✅ 消息域写侧阶段性收敛：先收敛“删除/已读”（保持行为不变）。
  - 消息删除：收敛到 `bc-messaging`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 `MsgGatewayImpl` 委托壳；提交：`22cb60eb`）。
  - 消息已读：收敛到 `bc-messaging`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 `MsgGatewayImpl` 委托壳；提交：`dd7483aa`）。
- ✅ 消息域读侧进一步收敛：收敛 `queryMsg/queryTargetAmountMsg` 到 `bc-messaging`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 `MsgGatewayImpl` 委托壳；保持行为不变）。
  - 落地提交：`05a2381b`
- ✅ 消息域写侧进一步收敛：收敛 `insertMessage` 到 `bc-messaging`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 `MsgGatewayImpl` 委托壳；保持行为不变）。
  - 落地提交：`8445bc41`
- ✅ 消息域写侧进一步收敛：收敛 `updateMsgDisplay` 到 `bc-messaging`（用例 + 端口 + `eva-infra` 端口适配器 + 旧 `MsgGatewayImpl` 委托壳；保持行为不变）。
  - 落地提交：`c315fa22`
- ✅ 最小回归已通过（Java17）：
  - `export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.17-zulu" && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl start -am test -Dtest=edu.cuit.app.eva.EvaRecordServiceImplTest,edu.cuit.app.eva.EvaStatisticsServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`
- ✅ 课程域查询/校验进一步收敛：`CourseUpdateGatewayImpl.isImported` 收敛到 `bc-course`（保持行为不变）。
  - 落地：`bc-course` 新增 QueryPort + UseCase；`eva-infra` 新增端口适配器原样搬运旧查询逻辑；旧 `CourseUpdateGatewayImpl.isImported` 退化为委托壳（落地提交：`4ed055a2/495287c8/f3e8e3cc`）。
- ✅ IAM 域写侧部分收敛：`UserUpdateGatewayImpl.assignRole` 收敛到 `bc-iam`（保持行为不变）。
  - 落地：`bc-iam` 新增用例 + 端口；`eva-infra` 新增端口适配器原样搬运旧写流程；旧 `UserUpdateGatewayImpl.assignRole` 退化为委托壳（落地提交：`16ff60b6/b65d311f/a707ab86`）。
- ✅ IAM 域写侧继续收敛：`UserUpdateGatewayImpl.createUser` 收敛到 `bc-iam`（保持行为不变）。
  - 落地：`bc-iam` 新增 `CreateUserUseCase` + `UserCreationPort` 与纯单测；`eva-infra` 新增 `UserCreationPortImpl` 端口适配器原样搬运旧流程；`eva-app` 组合根装配 Bean；旧 `UserUpdateGatewayImpl.createUser` 退化为委托壳（落地提交：`c3aa8739/a3232b78/a26e01b3/9e7d46dd`）。
- ✅ IAM 域写侧继续收敛：`UserUpdateGatewayImpl.updateInfo` 收敛到 `bc-iam`（保持行为不变）。
  - 落地：`bc-iam` 新增 `UpdateUserInfoUseCase` + `UserInfoUpdatePort` 与纯单测；`eva-infra` 新增 `UserInfoUpdatePortImpl` 端口适配器原样搬运旧流程；`eva-app` 组合根装配 Bean；旧 `UserUpdateGatewayImpl.updateInfo` 退化为委托壳（落地提交：`38c31541/6ce61024/db0fd6a3/cb789e21`）。
- ✅ IAM 域写侧继续收敛：`UserUpdateGatewayImpl.updateStatus` 收敛到 `bc-iam`（保持行为不变）。
  - 落地：`bc-iam` 新增用例 + 端口；`eva-infra` 新增端口适配器原样搬运旧流程（DB 更新 → 缓存失效 → 日志）；`eva-app` 组合根装配 Bean；旧 `UserUpdateGatewayImpl.updateStatus` 退化为委托壳（落地提交：`e3fcdbf0/8e82e01f/eb54e13e`）。
  - 行为快照（供回归对照）：
    - 异常不变：仍可能抛出 `BizException("初始管理员账户不允许此操作")`、`BizException("用户id不存在")`；
    - 副作用顺序不变：DB 更新 → 缓存失效（沿用 `handleUserUpdateCache` 清单）→ 日志（`LogUtils.logContent`）；
    - 事务边界不变：仍由 `UserServiceImpl.updateStatus` 的 `@Transactional` 承载；
    - `StpUtil.logout` 时机不变：仍在 gateway 调用之后执行（且仍依赖 `userQueryGateway.findUsernameById(...).orElseThrow(() -> new BizException("用户ID不存在"))` 的异常文案）。

## 0.1 本次会话增量总结（2025-12-19，更新至 `HEAD`）

- ✅ 评教任务发布写侧收敛到 `bc-evaluation`（用例 + 端口 + 旧 gateway 委托壳），并将“待办消息发送”改为事务提交后事件触发，避免回滚误推。
- ✅ 评教删除写侧收敛到 `bc-evaluation`（删除评教记录/模板两条写链路；旧 gateway 退化委托壳；行为不变）。
- ✅ 课程读侧渐进收敛：为 `CourseQueryGatewayImpl` 引入 QueryRepo（gateway 退化委托壳，行为不变）。
- ✅ 评教读侧渐进收敛：为 `EvaQueryGatewayImpl` 引入 `EvaQueryRepo`（gateway 退化委托壳，行为不变）。
- ✅ 评教读侧用例级回归测试：固化统计口径（`EvaStatisticsServiceImpl` / `EvaRecordServiceImpl`）。
- ✅ 测试稳定化：将依赖本地文件/外部服务的 `start` 测试改为内存回归用例，移除 Spring 上下文依赖。
- ✅ 评教读侧进一步解耦（统计/导出）：新增 `EvaStatisticsQueryPort`，应用层统计与导出查询改走端口；`eva-infra` 端口实现委托 `EvaQueryRepo`，行为保持不变。
- ✅ 评教读侧进一步解耦（任务查询）：新增 `EvaTaskQueryPort`，应用层任务查询改走端口；`eva-infra` 端口实现委托 `EvaQueryRepo`，行为保持不变。
- ✅ 评教读侧进一步解耦（记录/模板查询）：新增 `EvaRecordQueryPort` / `EvaTemplateQueryPort`，应用层记录与模板查询改走端口；统计导出补齐 `getCountAbEva` 统一由 `EvaStatisticsQueryPort` 提供，行为保持不变。
- ✅ `bc-evaluation` 补齐 `eva-client`/`eva-domain` 依赖，解决新 QueryPort 编译缺失问题。
- ✅ `bc-evaluation` 内部依赖补齐版本号（`eva-client`/`eva-domain`），修复 Maven 构建缺失版本报错。
- ✅ 修正 `EvaStatisticsQueryPort` 中 DTO 包名（`PastTimeEvaDetailCO` / `ScoreRangeCourseCO`）以通过编译。
- ✅ 修正 `EvaStatisticsQueryPortImpl` 中 DTO 包名，确保 `eva-infra` 编译通过。
- ✅ 旧 `EvaQueryGatewayImpl` 进一步收敛为 QueryPort 委托（任务/记录/模板/统计），避免直接依赖 `EvaQueryRepo`。
- ✅ 最小回归已通过并完成复验（同上命令）。
- ✅ 旧 `EvaQueryGateway` / `EvaQueryGatewayImpl` 已移除（应用层已完全切换到 QueryPort）。
- 新增提交（按时间顺序）：
  - `8e434fe1 feat(bc-evaluation): 增加评教任务发布用例骨架`
  - `ca69b131 feat(eva-infra): 实现评教任务发布端口适配器`
  - `e9043f96 refactor(eva-app): 评教任务发布收敛到bc-evaluation`
  - `ea928055 feat(bc-evaluation): 增加评教删除用例骨架`
  - `07b65663 feat(eva-infra): 实现评教删除端口适配器`
  - `05900142 refactor(eva-app): 评教删除写侧收敛到bc-evaluation`
  - `cc9af9ad docs: 更新交接与计划书（评教任务发布收敛）`
  - `76a89b78 docs: 更新交接与计划书（评教删除收敛）`
  - `ba8f2003 refactor(eva-infra): 课程读侧抽取QueryRepo`
  - `02f4167d refactor(eva-infra): 评教读侧抽取QueryRepo`
  - `a48cf044 test(eva-app): 补充评教读侧用例级回归`
  - `daf343ef test(start): 稳定化回归用例并去除外部依赖`
  - `3b215441 docs: 更新交接与计划书（测试稳定化）`
  - `daf343ef test(start): 稳定化回归用例并去除外部依赖`
  - `bb391698 refactor(evaluation): 拆分统计查询端口`
  - `570fecb6 refactor(evaluation): 拆分评教任务查询端口`
  - `ec5191a3 refactor(evaluation): 拆分评教记录与模板查询端口`
  - `6029a56e chore(bc-evaluation): 补齐查询端口依赖`
  - `9efe7b12 chore(bc-evaluation): 补齐内部依赖版本`
  - `198746fc fix(bc-evaluation): 修正统计查询端口DTO包名`
  - `cf493ef2 fix(eva-infra): 修正统计查询端口实现DTO包名`
  - `3e5da427 refactor(evaluation): 旧EvaQueryGateway委托到细分端口`
  - `c0d0b31a docs: 更新交接与Backlog（旧网关收敛）`
  - `9ab6512d docs: 更新回归结果`
  - `5c74083e refactor(evaluation): 移除旧EvaQueryGateway`
  - `49b80e0f docs: 更新交接与计划书（旧网关移除）`
  - `dd5245ed docs: 更新交接提交清单`
  - `35fb9e69 docs: 更新最小回归与规则`

## 0. 本轮会话增量总结（2025-12-18，更新至 `HEAD`，以 `git log -n 1` 为准）

本轮会话聚焦“DDD 渐进式重构（不做功能优化/不改业务语义）”，继续执行方案 B/C，并持续压扁课程相关的大泥球入口：

**本会话铁律（验收标准）**
- ✅ 只做重构：收敛调用链、抽离端口/用例、让旧 gateway 退化委托壳、事件化副作用；
- ✅ 行为不变：API 不变、异常类型/异常文案不变、消息文案与副作用时机（事务提交后）不变；
- ❌ 不做任何业务逻辑优化/语义调整（即便看到明显 bug/命名不佳，也先不动）。

**新增规则（给下一个会话）**
- ✅ 每个步骤结束必须跑最小回归用例（`EvaRecordServiceImplTest` / `EvaStatisticsServiceImplTest`）。

1) **课表导入/覆盖用例收敛到 bc-course（闭环 E）**  
   - 背景：历史实现中 `IUserCourseServiceImpl.importCourse()` 既做导入又做消息通知/撤回评教消息，属于典型“大泥球联动”。
   - 做法：把导入课表的核心写操作收敛到 `bc-course` 用例，把跨域副作用统一事件化交给 `bc-messaging`。
   - 关键改动点：
     - 用例与端口：`bc-course/src/main/java/edu/cuit/bc/course/application/usecase/ImportCourseFileUseCase.java`
     - infra 端口实现：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/ImportCourseFileRepositoryImpl.java`
     - 旧 gateway 退化委托壳：`eva-infra/src/main/java/edu/cuit/infra/gateway/impl/course/CourseUpdateGatewayImpl.java`（`importCourseFile`）
     - 应用层入口事件化：`eva-app/src/main/java/edu/cuit/app/service/impl/course/IUserCourseServiceImpl.java`（`importCourse`）

2) **跨域副作用继续“事务提交后发布”统一化**  
   - 通过 `eva-app/src/main/java/edu/cuit/app/event/AfterCommitEventPublisher.java` 把事件发布固定为“提交后再执行副作用”，避免回滚不一致。
   - 导入课表复用现有 `CourseOperationSideEffectsEvent` + `bc-messaging` 用例处理（通知/撤回评教消息）。

3) **文档沉淀**  
   - 简历可用沉淀：`data/RESUME_BALL_OF_MUD_REFACTOR_SUMMARY.md`（新增了“课表导入/覆盖收敛”案例）。
   - 交接文档：本文件已补充闭环 E 与下一步行动建议。

4) **冲突校验收敛（教室占用/时间段重叠，保持行为不变）**
   - 背景：教室占用/时间段重叠的 QueryWrapper 片段在多个端口适配器里重复，容易出现 SQL 条件漂移与异常文案不一致。
   - 做法：提炼可复用的“底层校验/查询片段”，用最小改动迁移调用点；并清理已无引用的旧 gateway 遗留私有逻辑，防止回潮。
   - 落地：
     - `ClassroomOccupancyChecker`：统一“教室占用冲突”校验（异常文案保持不变）
     - `CourInfTimeOverlapQuery`：统一“时间段重叠”查询片段（`start_time <= end && end_time >= start`，保持 SQL 条件不变）

5) **教师自助课表：副作用事件化 + 主写逻辑收敛（保持行为不变）**  
   - 背景：历史实现中 `IUserCourseServiceImpl.deleteSelfCourse()/updateSelfCourse()` 同时包含：
     - 主写逻辑（删课/改课，涉及课程/课次/评教任务/缓存）；
     - 跨域副作用（消息通知、撤回评教消息）；
     - 造成应用服务“大泥球联动”与难以拆分。
   - 落地（两段式，小步提交）：
     1. **事件化跨域副作用（事务提交后发布）**：
        - `CourseOperationSideEffectsEvent` 增加 `messageMode`（`NORMAL` / `TASK_LINKED`）用于兼容历史消息模型差异：
          - `NORMAL`：历史上对应 `MsgResult.toNormalMsg`（taskId 固定 -1）；
          - `TASK_LINKED`：历史上对应 `MsgResult.toSendMsg`（taskId = map key）。
        - usecase `HandleCourseOperationSideEffectsUseCase` 根据 `messageMode` 路由到 `CourseBroadcastPort.sendNormal/sendTaskLinked`，并保持“撤回评教消息”逻辑不变。
        - 关键文件：
          - `bc-messaging-contract/src/main/java/edu/cuit/bc/messaging/application/event/CourseOperationMessageMode.java`
          - `bc-messaging-contract/src/main/java/edu/cuit/bc/messaging/application/event/CourseOperationSideEffectsEvent.java`
          - `bc-messaging/src/main/java/edu/cuit/bc/messaging/application/usecase/HandleCourseOperationSideEffectsUseCase.java`
          - `eva-app/src/main/java/edu/cuit/app/bcmessaging/adapter/CourseBroadcastPortAdapter.java`
          - `eva-app/src/main/java/edu/cuit/app/service/impl/course/IUserCourseServiceImpl.java`（删课=默认 NORMAL；改课=TASK_LINKED）
     2. **主写逻辑收敛到 bc-course 用例**（仍返回历史 `Map<String, Map<Integer,Integer>>`，以便复用事件化副作用）：
        - 自助删课：`DeleteSelfCourseUseCase` + `DeleteSelfCourseRepositoryImpl`，旧 `CourseDeleteGatewayImpl.deleteSelfCourse` 退化为委托壳；
        - 自助改课：`UpdateSelfCourseUseCase` + `UpdateSelfCourseRepositoryImpl`，旧 `CourseUpdateGatewayImpl.updateSelfCourse` 退化为委托壳；
        - 迁移内容包含：原先 gateway 内部的课程/课次/评教任务/缓存失效逻辑，全部按原样搬运（不做优化）。
        - 关键文件：
          - `bc-course/src/main/java/edu/cuit/bc/course/application/usecase/DeleteSelfCourseUseCase.java`
          - `eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/DeleteSelfCourseRepositoryImpl.java`
          - `bc-course/src/main/java/edu/cuit/bc/course/application/usecase/UpdateSelfCourseUseCase.java`
          - `eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/UpdateSelfCourseRepositoryImpl.java`
          - `eva-infra/src/main/java/edu/cuit/infra/gateway/impl/course/CourseDeleteGatewayImpl.java`
          - `eva-infra/src/main/java/edu/cuit/infra/gateway/impl/course/CourseUpdateGatewayImpl.java`

5) **课程类型修改链路收敛到 bc-course（保持行为不变）**  
   - 目标：压扁 `CourseUpdateGatewayImpl.updateCourseType()/updateCoursesType()`，让 infra 不再承载业务流程。
   - 落地：
     - bc-course：新增 `UpdateCourseTypeUseCase/UpdateCoursesTypeUseCase`（仅委托端口，不新增校验），并补齐纯单测；
     - eva-infra：新增端口实现 `UpdateCourseTypeRepositoryImpl/UpdateCoursesTypeRepositoryImpl`，迁移原 DB/缓存/日志逻辑；
     - 旧 gateway：方法退化为委托壳（行为不变）。
   - 关键文件：
     - `bc-course/src/main/java/edu/cuit/bc/course/application/usecase/UpdateCourseTypeUseCase.java`
     - `bc-course/src/main/java/edu/cuit/bc/course/application/usecase/UpdateCoursesTypeUseCase.java`
     - `eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/UpdateCourseTypeRepositoryImpl.java`
     - `eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/UpdateCoursesTypeRepositoryImpl.java`
     - `eva-infra/src/main/java/edu/cuit/infra/gateway/impl/course/CourseUpdateGatewayImpl.java`

6) **删课链路收敛到 bc-course（保持行为不变）**  
   - 目标：压扁 `CourseDeleteGatewayImpl.deleteCourse()/deleteCourses()`，把“删课 + 删除评教任务/记录 + 缓存/日志”收敛到 bc-course。
   - 落地：
     - bc-course：新增 `DeleteCourseUseCase/DeleteCoursesUseCase`（仅委托端口，不新增校验），并补齐纯单测；
     - eva-infra：新增端口实现 `DeleteCourseRepositoryImpl/DeleteCoursesRepositoryImpl`，迁移原 DB/缓存/日志逻辑（含 `isEmptiy` 条件拼装逻辑随端口实现私有化）；
     - 旧 gateway：方法退化为委托壳（行为不变）。
   - 关键文件：
     - `bc-course/src/main/java/edu/cuit/bc/course/application/usecase/DeleteCourseUseCase.java`
     - `bc-course/src/main/java/edu/cuit/bc/course/application/usecase/DeleteCoursesUseCase.java`
     - `eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/DeleteCourseRepositoryImpl.java`
      - `eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/DeleteCoursesRepositoryImpl.java`
      - `eva-infra/src/main/java/edu/cuit/infra/gateway/impl/course/CourseDeleteGatewayImpl.java`

7) **删除课程类型链路收敛到 bc-course（保持行为不变）**
   - 目标：压扁 `CourseDeleteGatewayImpl.deleteCourseType()`，让 infra 不再承载“删课程类型”的业务流程。
   - 行为不变约束（必须保持）：
     - 异常类型/异常文案不变：`UpdateException("请选择要删除的课程类型")`、`UpdateException("默认课程类型不能删除")`
     - 删除顺序不变：先删关联表 `course_type_course`，再删 `course_type`
     - 日志/缓存行为不变：`LogUtils.logContent(typeName + "课程类型")` + 失效 `COURSE_TYPE_LIST`
   - 落地：
     - bc-course：新增 `DeleteCourseTypeUseCase` + `DeleteCourseTypeRepository`（仅委托端口，不新增校验），并补齐纯单测；
     - eva-infra：新增端口实现 `DeleteCourseTypeRepositoryImpl`，把旧逻辑原样搬运（含校验/删除顺序/日志/缓存）；
     - 旧 gateway：`CourseDeleteGatewayImpl.deleteCourseType` 退化为委托壳（返回 `null`，保持签名与行为不变）。
   - 关键文件：
     - `bc-course/src/main/java/edu/cuit/bc/course/application/usecase/DeleteCourseTypeUseCase.java`
     - `eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/DeleteCourseTypeRepositoryImpl.java`
     - `eva-infra/src/main/java/edu/cuit/infra/gateway/impl/course/CourseDeleteGatewayImpl.java`

8) **新增课程类型链路收敛到 bc-course（保持行为不变）**
   - 目标：压扁 `CourseUpdateGatewayImpl.addCourseType()`，让 infra 不再承载“新增课程类型”的写流程。
   - 行为不变约束（必须保持）：
     - 异常类型/异常文案不变：`UpdateException("该课程类型已存在")`
     - 缓存行为不变：新增成功后失效 `COURSE_TYPE_LIST`
   - 落地：
     - bc-course：新增 `AddCourseTypeUseCase` + `AddCourseTypeRepository`（仅委托端口，不新增校验），并补齐纯单测；
     - eva-infra：新增端口实现 `AddCourseTypeRepositoryImpl`，把旧逻辑原样搬运（含存在性校验/插入/缓存失效）；
     - 旧 gateway：`CourseUpdateGatewayImpl.addCourseType` 退化为委托壳（返回 `null`，保持签名与行为不变）。
   - 关键文件：
     - `bc-course/src/main/java/edu/cuit/bc/course/application/usecase/AddCourseTypeUseCase.java`
     - `eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/AddCourseTypeRepositoryImpl.java`
     - `eva-infra/src/main/java/edu/cuit/infra/gateway/impl/course/CourseUpdateGatewayImpl.java`

9) **批量新建多节课（新课程）链路收敛到 bc-course（闭环 K，保持行为不变）**
   - 背景：`POST /course/batch/notExist` 入口最终落到 `CourseUpdateGatewayImpl.addNotExistCoursesDetails`，旧实现包含“科目插入/课程插入/默认模板与默认课程类型补齐/关联表写入/课次写入/缓存失效/教室冲突校验”等完整写流程，属于 infra 层承载业务。
   - 做法：新增 bc-course 用例骨架 + 端口，并在 eva-infra 端口适配器中原样搬运旧逻辑；旧 gateway 退化为委托壳。
   - 行为不变约束（必须保持）：
     - 默认模板/默认类型补齐策略不变：`templateId == null && nature in {0,1}` 时按 `is_default` 查询补齐；
     - 冲突校验与文案不变：教室冲突抛 `UpdateException("该时间段教室冲突，请修改时间")`；
     - 缓存失效不变：`SUBJECT_LIST`、`COURSE_LIST_BY_SEM`、`ALL_CLASSROOM`；
     - 课次写入时间字段不变：`createTime/updateTime` 的取值保持与旧实现一致。
   - 关键文件：
     - 用例与端口：`bc-course/src/main/java/edu/cuit/bc/course/application/usecase/AddNotExistCoursesDetailsUseCase.java`
     - infra 端口实现：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/AddNotExistCoursesDetailsRepositoryImpl.java`
     - 旧 gateway 退化委托壳：`eva-infra/src/main/java/edu/cuit/infra/gateway/impl/course/CourseUpdateGatewayImpl.java`（`addNotExistCoursesDetails`）

10) **批量新建多节课（已有课程）链路收敛到 bc-course（闭环 L，保持行为不变）**
   - 背景：`POST /course/batch/exist/{courseId}` 入口最终落到 `CourseUpdateGatewayImpl.addExistCoursesDetails`，旧实现包含“逐周新增课次 + 教室冲突校验 + 课程/科目存在性校验 + 日志 + 缓存失效”等完整写流程，属于 infra 层承载业务。
   - 做法：新增 bc-course 用例骨架 + 端口，并在 eva-infra 端口适配器中原样搬运旧逻辑；旧 gateway 退化为委托壳。
   - 行为不变约束（必须保持）：
     - 冲突校验与文案不变：教室冲突抛 `UpdateException("该时间段教室冲突，请修改时间")`；
     - 课程/科目不存在异常不变：`QueryException("不存在对应的课程")`、`QueryException("不存在对应的课程的科目")`；
     - 日志文案不变：`subjectName + "(ID:" + courseId + ")的课程的课数"`；
     - 缓存失效不变：`ALL_CLASSROOM`；
     - 课次写入时间字段不变：`createTime/updateTime` 仍为 `LocalDateTime.now()`。
   - 关键文件：
     - 用例与端口：`bc-course/src/main/java/edu/cuit/bc/course/application/usecase/AddExistCoursesDetailsUseCase.java`
     - infra 端口实现：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/AddExistCoursesDetailsRepositoryImpl.java`
     - 旧 gateway 退化委托壳：`eva-infra/src/main/java/edu/cuit/infra/gateway/impl/course/CourseUpdateGatewayImpl.java`（`addExistCoursesDetails`）

本轮新增提交（按时间顺序）：
- `a122ff58 feat(bc-course): 引入课程BC用例与基础设施端口实现`
- `285db180 feat(bc-messaging): 课程操作副作用事件化并收敛消息发送`
- `4a22fdaf fix(gitignore): 仅忽略仓库根 data 目录`（避免误伤 `eva-client/.../dto/data` 包路径）
- `60fe256d feat(eva-config): 支持高分阈值可配置`
- `a330d8e7 test(bc-template): 迁移模板锁定校验并清理旧实现`
- `35aca4eb docs: 更新会话交接与后续任务清单`
- `9a96a04a feat(bc-messaging): 自助课表操作副作用事件化`
- `1b2d8756 feat(bc-course): 收敛修改课程信息用例`
- `2b5cb49d feat(bc-course): 增加课程类型修改用例骨架`
- `255cb51f feat(eva-infra): 实现课程类型修改端口适配器`
- `b97a905b refactor(course): 课程类型修改收敛到bc-course`
- `45ca3b5c feat(bc-course): 收敛教师自助删课用例骨架`
- `e7ef502a refactor(course): 自助删课收敛到bc-course`
- `dde5ecf1 feat(bc-course): 收敛教师自助改课用例骨架`
- `87caef60 refactor(course): 自助改课收敛到bc-course`
- `7f055aa3 feat(bc-course): 收敛删课用例骨架`
- `bd85f734 feat(eva-infra): 实现删课端口适配器`
- `0a186d03 refactor(course): 删课链路收敛到bc-course`
- `cac3a97f docs: 更新会话交接（删课/自助课表收敛）`
- `cf7ef892 feat(bc-course): 增加删除课程类型用例骨架`
- `d5a8a0d3 feat(eva-infra): 实现删除课程类型端口适配器`
- `57c8bc5d refactor(course): 删除课程类型收敛到bc-course`
- `7df81f64 feat(bc-course): 增加新增课程类型用例骨架`
- `e612d92a feat(eva-infra): 实现新增课程类型端口适配器`
- `a5df07af refactor(course): 新增课程类型收敛到bc-course`
- `a73c7bed feat(bc-course): 收敛新建课程明细用例骨架`
- `685cfd8c feat(eva-infra): 实现新建课程明细端口适配器`
- `28dfdd49 refactor(course): 新建课程明细收敛到bc-course`
- `c91c4473 feat(bc-course): 收敛新增课次用例骨架`
- `b4b66261 feat(eva-infra): 实现新增课次端口适配器`
- `f447fd17 refactor(course): 新增课次收敛到bc-course`
- `080ae028 docs: 更新会话交接（新增课次收敛）`
- `8c9b7443 refactor(eva-infra): 收敛教室占用冲突校验`
- `969d606a refactor(eva-infra): 清理CourseUpdateGateway遗留私有逻辑`
- `2cfbfaee docs: 补充Java17构建基线说明`
- `e417fb8e refactor(eva-infra): 提炼课程时间段重叠查询片段`
- `2bb64aa1 refactor(eva-infra): 复用时间段重叠查询条件`
- `bcdd8564 refactor(eva-infra): 分配评教冲突校验复用时间重叠片段`
- `a53aafee docs: 记录冲突校验收敛进展与离线验证`
- `a84dc46d docs: 更新交接文档版本号`
- `94662c8a docs: 交接文档改为git log对齐`
- `0684a374 docs: 更新DDD重构Backlog进度与下一步`

验证（建议使用 Java17；网络受限时再加 `-o` 离线）：
- 切换 JDK（本机已安装）：`sdk use java 17.0.17-zulu`  
  或：`export JAVA_HOME=\"$HOME/.sdkman/candidates/java/17.0.17-zulu\" && export PATH=\"$JAVA_HOME/bin:$PATH\"`
 - `mvn -o -pl bc-course -am test -q -Dmaven.repo.local=.m2/repository`
 - `mvn -o -pl bc-messaging -am test -q -Dmaven.repo.local=.m2/repository`
 - `mvn -o -pl eva-infra -am -DskipTests test -q -Dmaven.repo.local=.m2/repository`
 - 已验证：`export JAVA_HOME=\"$HOME/.sdkman/candidates/java/17.0.17-zulu\" && export PATH=\"$JAVA_HOME/bin:$PATH\" && mvn -o -pl eva-infra -am test -q -Dmaven.repo.local=.m2/repository`
 - 注意：若直接使用系统默认 JDK（例如 GraalVM Java 25），本仓库可能在编译期报 `TypeTag :: UNKNOWN`，请优先切回 Java 17 再验证

## 1. 关键业务结论（必须记住）

用户已明确确认的业务语义：

1) `cour_inf.week`：表示学期从 `semester.start_date` 起算的第 **n 周**（单周周次）。  
2) `cour_inf.day`：`day=1` 表示 **周一**，`day=n` 表示周 n（周一是一周开始）。  
3) 同一课次允许多人评教（这是必须如此的业务前提）。  
4) 模板锁定规则：**只要有人评教过就锁定**，锁定后不允许切换课程模板。  
5) “高分次数”的阈值：已支持 **可配置**（`eva-config.json` 新增 `highScoreThreshold`，AI 报告统计已改为使用该阈值）。  
6) AI 报告维度：**教师 + 学期**（汇总该教师本学期全部课程；报告中可按课程分节）。

微服务演进约束：
- **先拆服务，暂时共享库**（过渡期），且要一小步一小步推进。

---

## 2. 架构/文档产物

- `AGENTS.md`：仓库贡献者指南（Repository Guidelines）。  
- `DDD_REFACTOR_PLAN.md`：非常详细的 DDD 重构计划书（领域划分、BC、六边形、CQRS、事件、微服务演进等）。

---

## 3. 本会话已完成的两条业务闭环（均已分阶段 commit）

### 3.1 闭环 A：提交评教（Submit Evaluation）DDD 化

现状链路（保持 API 不变）：
- Controller：`PUT /evaluate/task/form`  
  `eva-adapter/.../UpdateEvaController.putEvaTemplate()` 仍调用 `IEvaRecordService.putEvaTemplate()`。
- Service：`eva-app/.../EvaRecordServiceImpl.putEvaTemplate()` 已改为调用 bc-evaluation 用例（不再走旧 `evaUpdateGateway.putEvaTemplate`）。

新增业务模块：`bc-evaluation`
- 根模块加入：`pom.xml` 新增 `<module>bc-evaluation</module>`。
- bc-evaluation 关键代码：
  - 用例：`bc-evaluation/.../SubmitEvaluationUseCase.java`
  - 端口：`bc-evaluation/.../SubmitEvaluationRepository.java`
  - 领域事件：`bc-evaluation/.../EvaluationSubmittedEvent.java`
  - 状态/异常：`TaskStatus`、`SubmitEvaluationException`

基础设施实现（仍复用现有表/Mapper/缓存）：
- `eva-infra/.../SubmitEvaluationRepositoryImpl.java`
  - 插入 `form_record`
  - 更新 `eva_task.status=COMPLETED`
  - 若 `cour_one_eva_template` 不存在则创建快照（用于模板锁定依据）
  - 缓存失效：修复为 `invalidateCache(..., String.valueOf(taskId))`

事件化联动（事务提交后清理消息，避免回滚不一致）：
- 事件发布端口实现：`eva-app/.../SpringAfterCommitDomainEventPublisher.java`
- 监听器：`eva-app/.../EvaluationSubmittedMessageCleanupListener.java`
  - 收到 `EvaluationSubmittedEvent` 后执行 `msgService.deleteEvaMsg(taskId, null)`（删除待办+逾期提醒两类评教消息）
- 装配：`eva-app/.../BcEvaluationConfiguration.java` 提供 `SubmitEvaluationUseCase` Bean

测试：
- `bc-evaluation/src/test/.../SubmitEvaluationUseCaseTest.java`（纯单测，无 Spring）

### 3.2 闭环 B：模板锁定后禁止切换模板（Course Template Lock）

定位到的入口与实现：
- Controller：`eva-adapter/.../UpdateCourseController`  
  - `PUT /course`（单课程修改，含 templateId）  
  - `PUT /courses/template`（批量切换课程模板）
- 应用层：
  - `eva-app/.../ICourseDetailServiceImpl.updateCourse()` -> `bc-course` 用例 `ChangeSingleCourseTemplateUseCase`
  - `eva-app/.../ICourseDetailServiceImpl.updateCourses()` -> `bc-course` 用例 `ChangeCourseTemplateUseCase`
- 基础设施：`eva-infra/.../CourseUpdateGatewayImpl.updateCourse()/updateCourses()` 原先直接 update `course.template_id`，没有锁定校验。

已实现的锁定规则（最终形态：业务模块 bc-template）：
- 新增业务模块：`bc-template`
  - 根模块加入：`pom.xml` 新增 `<module>bc-template</module>`
  - 服务：`bc-template/.../CourseTemplateLockService.java`
  - 端口：`bc-template/.../CourseTemplateLockQueryPort.java`
  - 异常：`bc-template/.../TemplateLockedException.java`
  - 单测：`bc-template/.../CourseTemplateLockServiceTest.java`
- 基础设施端口实现（读现有表判断锁定）：
  - `eva-infra/.../CourseTemplateLockQueryPortImpl.java`
  - 判定策略：
    1) 优先用 `cour_one_eva_template` 是否存在（`course_id + semester_id`）作为锁定证据
    2) 若快照缺失，则回退到 `form_record` 反查（`cour_inf` -> `eva_task` -> `form_record`）保守锁定
- 单体装配：
  - `eva-app/.../BcTemplateConfiguration.java`
- 在 `CourseUpdateGatewayImpl` 接入：
  - `updateCourse()`：模板切换逻辑已上移到 `bc-course`，不再在 infra 内重复做“锁定不可切换”校验
  - `updateCourses()`：历史路径仍保留，但已委托到 `bc-course` 用例（收敛重复逻辑，便于后续删掉旧 gateway）

本会话已清理遗留/重复实现：
- 已删除 `eva-infra/.../CourseTemplateLockChecker.java`（被 `CourseTemplateLockQueryPortImpl` 取代）
- 已删除 `start/src/test/.../CourseTemplateLockCheckerTest.java`，并新增 `CourseTemplateLockQueryPortImplTest` 直接验证端口实现
- 由于当前运行环境不支持 Mockito inline mock maker（ByteBuddy 自附加），增加了 `mockito-extensions/org.mockito.plugins.MockMaker=mock-maker-subclass`
- 已新增 `bc-course`，并把模板切换（批量/单个）统一收口到用例层（避免“前端总是带 templateId”导致锁定误判）

### 3.3 闭环 C：课程改课/删课联动事件化（Course -> Messaging）

目标：把“改课/删课”产生的跨域副作用（消息通知、撤回评教消息）从课程应用服务中剥离，收敛到 `bc-messaging` 处理，便于后续拆微服务/替换 MQ。

落地：
- 新增业务模块：`bc-messaging`
  - 事件：`CourseOperationSideEffectsEvent`
  - 用例：`HandleCourseOperationSideEffectsUseCase`
- 事件发布：`eva-app/.../AfterCommitEventPublisher`（事务提交后发布，避免回滚导致联动不一致）
- 监听器：`eva-app/.../CourseOperationSideEffectsListener`（收到事件后调用 `bc-messaging` 用例）
- 改造入口（行为保持不变，仅重构调用链）：
  - `eva-app/.../ICourseServiceImpl.updateSingleCourse()`（改课）
  - `eva-app/.../ICourseServiceImpl.deleteCourses()`（删除某课程某时段等）
  - `eva-app/.../ICourseDetailServiceImpl.delete()`（删除课程）
  - `eva-app/.../ICourseDetailServiceImpl.updateCourse()`（修改课程后通知全体）
  - `eva-app/.../ICourseServiceImpl.allocateTeacher()`（分配听课/评教老师，老师任务消息也已事件化）

补充：`allocateTeacher()` 的事件使用 `CourseTeacherTaskMessagesEvent`，并由 `bc-messaging` 用例 `HandleCourseTeacherTaskMessagesUseCase` 处理（底层仍复用 `MsgResult.sendMsgtoTeacher`，行为保持不变）。

### 3.4 闭环 D：分配评教老师用例收敛（Course -> bc-course）

目标：把“分配听课/评教老师”从旧 `CourseUpdateGatewayImpl.assignTeacher()` 的大段业务逻辑中抽离，收敛到 `bc-course` 用例层，旧 gateway 仅保留委托。

落地：
- 用例：`bc-course/.../AssignEvaTeachersUseCase`
- 端口：`bc-course/.../AssignEvaTeachersRepository`
- 基础设施端口实现：`eva-infra/.../AssignEvaTeachersRepositoryImpl`（迁移原有冲突校验 + 任务创建 + 缓存失效逻辑，行为保持不变）
- 旧入口：`CourseUpdateGatewayImpl.assignTeacher()` 现在委托到用例（避免 infra 继续堆规则）

### 3.5 闭环 E：课表导入用例收敛 + 副作用事件化（Import Course Table）

目标：把“导入课表/覆盖课表”的核心写操作从旧 gateway/service 中抽离，收敛到 `bc-course`；并把导入后产生的跨域副作用（通知全体、撤回评教消息）统一事件化交给 `bc-messaging` 处理，便于后续迁移到各 BC/微服务。

落地（保持 API/行为不变，仅重构调用链）：
- `bc-course` 新增用例：
  - 命令：`bc-course/.../ImportCourseFileCommand.java`
  - 端口：`bc-course/.../ImportCourseFileRepository.java`
  - 用例：`bc-course/.../ImportCourseFileUseCase.java`
- 基础设施端口实现（迁移原逻辑，行为保持不变）：
  - `eva-infra/.../ImportCourseFileRepositoryImpl.java`
  - 迁移内容包含：学期 upsert、覆盖导入时的删除+新增、返回旧的 `Map<String, Map<Integer,Integer>>` 消息模型、教室缓存失效与日志记录
- 旧 gateway 退化为委托壳：
  - `eva-infra/.../CourseUpdateGatewayImpl.importCourseFile()` -> 委托 `ImportCourseFileUseCase`
- 导入入口侧副作用事件化：
  - `eva-app/.../IUserCourseServiceImpl.importCourse()` 不再直接循环调用 `msgResult/msgService.deleteEvaMsg`
  - 改为发布 `CourseOperationSideEffectsEvent`，由 `CourseOperationSideEffectsListener` -> `bc-messaging` 统一处理

### 3.6 闭环 F：教师自助课表链路收敛（Self Course）

目标：把“教师自助删课/改课”的主写逻辑与跨域副作用拆开：  
- 主写逻辑收敛到 `bc-course` 用例 + `eva-infra` 端口适配器（行为不变）；  
- 跨域副作用（消息通知、撤回评教消息）统一“事务提交后发布”交给 `bc-messaging`（行为不变）。

落地（保持 API/异常文案/副作用不变，仅重构调用链）：
- 自助删课：
  - bc-course 用例：`bc-course/src/main/java/edu/cuit/bc/course/application/usecase/DeleteSelfCourseUseCase.java`
  - infra 端口实现：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/DeleteSelfCourseRepositoryImpl.java`
  - 旧入口退化委托壳：`eva-infra/src/main/java/edu/cuit/infra/gateway/impl/course/CourseDeleteGatewayImpl.java`（`deleteSelfCourse`）
  - 应用层副作用事件化：`eva-app/src/main/java/edu/cuit/app/service/impl/course/IUserCourseServiceImpl.java`（`deleteSelfCourse`）
- 自助改课：
  - bc-course 用例：`bc-course/src/main/java/edu/cuit/bc/course/application/usecase/UpdateSelfCourseUseCase.java`
  - infra 端口实现：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/UpdateSelfCourseRepositoryImpl.java`
  - 旧入口退化委托壳：`eva-infra/src/main/java/edu/cuit/infra/gateway/impl/course/CourseUpdateGatewayImpl.java`（`updateSelfCourse`）
  - 应用层副作用事件化：`eva-app/src/main/java/edu/cuit/app/service/impl/course/IUserCourseServiceImpl.java`（`updateSelfCourse`，注意需要 `CourseOperationMessageMode.TASK_LINKED` 保持历史消息格式）

补充：为保证“消息格式行为不变”，`CourseOperationSideEffectsEvent` 引入了 `CourseOperationMessageMode`（`NORMAL/TASK_LINKED`）：
- `NORMAL`：历史 `MsgResult.toNormalMsg`；
- `TASK_LINKED`：历史 `MsgResult.toSendMsg`；
对应适配已落地在 `eva-app/src/main/java/edu/cuit/app/bcmessaging/adapter/CourseBroadcastPortAdapter.java`。

### 3.7 闭环 G：修改课程信息用例收敛（Update Course Info）

目标：压扁 `CourseUpdateGatewayImpl.updateCourse()`，让 infra 不再承载“修改课程信息”的业务流程（行为不变）。

落地：
- bc-course 新增用例：`bc-course/src/main/java/edu/cuit/bc/course/application/usecase/UpdateCourseInfoUseCase.java`
- infra 端口实现：`eva-infra/src/main/java/edu/cuit/infra/bccourse/adapter/UpdateCourseInfoRepositoryImpl.java`
- 旧 gateway 退化委托壳：`eva-infra/src/main/java/edu/cuit/infra/gateway/impl/course/CourseUpdateGatewayImpl.java`（`updateCourse`）

### 3.8 闭环 H：课程类型修改链路收敛（Update Course Type）

目标：压扁 `CourseUpdateGatewayImpl.updateCourseType/updateCoursesType`（行为不变）。

落地：
- bc-course 用例骨架：`UpdateCourseTypeUseCase` / `UpdateCoursesTypeUseCase`
- infra 端口实现：`UpdateCourseTypeRepositoryImpl` / `UpdateCoursesTypeRepositoryImpl`
- 旧 gateway 退化为委托壳（仍由 domain gateway 对外暴露）

### 3.9 闭环 I：删课链路收敛（Delete Course）

目标：压扁 `CourseDeleteGatewayImpl.deleteCourse/deleteCourses`（行为不变）。

落地：
- bc-course 用例骨架：`DeleteCourseUseCase` / `DeleteCoursesUseCase`
- infra 端口实现：`DeleteCourseRepositoryImpl` / `DeleteCoursesRepositoryImpl`（原 `isEmptiy` 条件拼装逻辑已随端口实现私有化）
- 旧 gateway 退化为委托壳：`eva-infra/src/main/java/edu/cuit/infra/gateway/impl/course/CourseDeleteGatewayImpl.java`

### 3.10 闭环 J：删除课程类型链路收敛（Delete Course Type）

目标：压扁 `CourseDeleteGatewayImpl.deleteCourseType`（行为不变）。

落地：
- bc-course 用例骨架：`DeleteCourseTypeUseCase`
- infra 端口实现：`DeleteCourseTypeRepositoryImpl`
- 旧 gateway 退化为委托壳：`eva-infra/src/main/java/edu/cuit/infra/gateway/impl/course/CourseDeleteGatewayImpl.java`

验证命令（离线优先，避免网络受限）：
- `mvn -o -pl bc-course -am test -q -Dmaven.repo.local=.m2/repository`
- `mvn -o -pl bc-messaging -am test -q -Dmaven.repo.local=.m2/repository`
- `mvn -o -pl eva-infra -am -DskipTests test -q -Dmaven.repo.local=.m2/repository`

---

## 4. 重要工程/环境约束（踩坑记录）

### 4.1 Java 版本必须使用 17
- Java 25（GraalVM）会触发编译异常（javac TypeTag UNKNOWN）。
- 建议命令（每次新 shell）：  
  - `source "$HOME/.sdkman/bin/sdkman-init.sh"`  
  - `sdk use java 17.0.17-zulu`

### 4.2 Maven 仓库写权限与网络
- 默认 `~/.m2` 在此环境会出现 `AccessDeniedException`（尤其是 junit-platform-launcher 目录/文件）。
- 解决策略：使用工作区本地仓库并已加入忽略：
  - 运行 Maven 时加：`-Dmaven.repo.local=.m2/repository`
  - `.gitignore` 已忽略 `.m2/`
- 网络受限：解析 GitHub Packages / 私有仓库依赖时，可能需要“开启网络（require_escalated）”。本会话多次因 DNS 失败/依赖拉取需要联网。

### 4.3 Git 提交签名（GPG）问题
- 初次提交失败原因：GPG/keyboxd 权限问题。
- 已在本仓库设置 `git config commit.gpgsign false`，提交均使用 `--no-gpg-sign`。

### 4.4 测试运行策略
- `start` 模块存在大量 `@SpringBootTest`，会尝试连接 DB/LDAP/AI 等，**全量测试容易失败**（非本会话引入）。
- 本会话验证新增测试的推荐方式：
  - 运行 bc 模块纯单测：  
    - `mvn -pl bc-evaluation test -q -Dmaven.repo.local=.m2/repository`  
    - `mvn -pl bc-template test -q -Dmaven.repo.local=.m2/repository`
    - `mvn -pl bc-course -am test -q -Dmaven.repo.local=.m2/repository`
  - 只跑指定测试（多模块时需要忽略未命中模块）：  
    - `mvn -pl start -am test -q -Dtest=CourseTemplateLockQueryPortImplTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

### 4.5 Linux 下误生成 Windows 路径目录
- 曾出现目录：`start/D:\Programming\Java\Projects\eva-backend\data\config/`（包含一个 `eva-config.json`）
- `.gitignore` 已增加忽略规则以避免污染提交（注意：该目录仍存在但会被忽略）。

---

## 5. 已完成的提交（按时间倒序，供回退/追踪）

当前 `git log --oneline -n 25` 关键提交如下（最新在上，更新至 `HEAD`，以 `git log -n 1` 为准）：

- `51dd315b docs: 补齐updateInfo收敛提交号`
- `cb789e21 refactor(iam): 收敛updateInfo到bc-iam`
- `db0fd6a3 feat(iam): 装配updateInfo组合根`
- `6ce61024 feat(eva-infra): 实现updateInfo端口适配器`
- `38c31541 feat(bc-iam): 增加updateInfo用例骨架`
- `1a3eb0b1 docs: 更新IAM收敛进度与下一步计划`
- `9e7d46dd refactor(iam): 收敛createUser到bc-iam`
- `a26e01b3 feat(iam): 装配createUser组合根`
- `a3232b78 feat(eva-infra): 实现createUser端口适配器`
- `c3aa8739 feat(bc-iam): 增加createUser用例骨架`
- `a707ab86 refactor(iam): 收敛assignRole到bc-iam`
- `b65d311f feat(iam): 装配assignRole端口适配器与组合根`
- `16ff60b6 feat(bc-iam): 初始化模块并增加assignRole用例骨架`
- `f3e8e3cc refactor(course): 收敛isImported到bc-course`
- `495287c8 feat(eva-infra): 实现课程导入状态查询端口适配器`
- `4ed055a2 feat(bc-course): 增加isImported查询用例骨架`
- `c315fa22 refactor(messaging): 收敛消息展示状态到bc-messaging`
- `8445bc41 refactor(messaging): 收敛消息插入到bc-messaging`
- `05a2381b refactor(messaging): 收敛消息查询到bc-messaging`
- `915d53ae docs: 更新计划/Backlog/交接（消息删除与已读收敛）`
- `dd7483aa refactor(messaging): 消息已读写侧收敛到bc-messaging`
- `22cb60eb refactor(messaging): 删除消息写侧收敛到bc-messaging`
- `bbad6802 docs: 更新交接与计划书（清理提交评教旧实现）`
- `12279f3f refactor(evaluation): 清理旧EvaUpdateGatewayImpl提交评教遗留实现`
- `fdac216b docs: 更新交接与计划书（评教模板写侧收敛）`

---

## 6. 当前工作区状态（需要处理/注意）

- 当前工作区除本文件外应保持干净（已按步骤完成 git 提交拆分）。
- 简历可用的“重构大泥球方法论”沉淀在：`data/RESUME_BALL_OF_MUD_REFACTOR_SUMMARY.md`
  - 注意：仓库根 `data/` 已被 `.gitignore` 忽略（设计如此，用于存放运行时/文档/样例，不进版本库）。
  - 若需要把该文档纳入版本库，请迁移到非 `data/` 目录（例如新建 `docs/`）。

---

## 7. 下一步建议任务（供新会话继续）

推荐按“继续模块化 + 小步 commit”的方式推进：

0) **新会话起手式（避免上下文浪费时间）**
   - 切 JDK 到 17（见上文），再跑一次 `mvn -pl start -am test -Dmaven.repo.local=.m2/repository` 做全量回归。
   - 用 Serena 重新索引（用户要求“开始任务先更新一次索引”）。

1) ✅ **已完成：继续收敛“教师自助课表”链路（IUserCourseServiceImpl）**
   - 已完成事件化副作用 + 主写逻辑收敛到 `bc-course`（保持行为不变）。

2) ✅ **已完成：压扁旧 `CourseUpdateGatewayImpl.updateCourse()`**
   - 已新增 `UpdateCourseInfoUseCase` + infra 端口实现，旧 gateway 退化委托壳（保持行为不变）。

3) ✅ **已完成：课程类型修改链路收敛**
   - `updateCourseType/updateCoursesType` 已收敛到 `bc-course`。

4) ✅ **已完成：删课链路收敛**
   - `deleteCourse/deleteCourses` 已收敛到 `bc-course`。

5) ✅ **已完成：压扁 `CourseDeleteGatewayImpl.deleteCourseType()`**
   - 已新增 `DeleteCourseTypeUseCase` + `DeleteCourseTypeRepositoryImpl`，旧 gateway 退化委托壳（保持行为不变）。

6) ✅ **已完成：压扁 `CourseUpdateGatewayImpl.addCourseType()`**
   - 已新增 `AddCourseTypeUseCase` + `AddCourseTypeRepositoryImpl`，旧 gateway 退化委托壳（保持行为不变）。

7) ✅ **已完成：压扁 `CourseUpdateGatewayImpl.addNotExistCoursesDetails()`**
   - 已新增 `AddNotExistCoursesDetailsUseCase` + `AddNotExistCoursesDetailsRepositoryImpl`，旧 gateway 退化委托壳（保持行为不变）。

8) ✅ **已完成：压扁 `CourseUpdateGatewayImpl.addExistCoursesDetails()`**
   - 已新增 `AddExistCoursesDetailsUseCase` + `AddExistCoursesDetailsRepositoryImpl`，旧 gateway 退化委托壳（保持行为不变）。

9) ✅ **已完成：收敛“课程时间/教室冲突校验”的重复片段（保持行为不变）**
   - 已抽取：
     - `ClassroomOccupancyChecker`（教室占用冲突校验）
     - `CourInfTimeOverlapQuery`（时间段重叠查询片段）
   - 已迁移：
     - 新增课次/新建课程明细/自助改课：替换重复 `judgeAlsoHasLocation` 与重复 QueryWrapper（异常文案保持不变）
     - 改课/自助改课/分配评教：替换时间段重叠 QueryWrapper（异常文案保持不变）
   - 已清理：`CourseUpdateGatewayImpl` 遗留私有逻辑（防止旧 gateway 回潮承载业务流程）

10) ✅ **已完成：收敛评教写侧主链路（`EvaUpdateGatewayImpl.postEvaTask`）**
   - 目标：按“用例 + 端口 + 旧 gateway 委托壳”的标准步骤，把写侧流程收敛到 `bc-evaluation`（行为不变）。
   - 用例与端口：`bc-evaluation` 新增 `PostEvaTaskUseCase` + `PostEvaTaskRepository` + `EvaluationTaskPostedEvent`，并补齐纯单测。
   - infra 端口实现：`eva-infra` 新增 `PostEvaTaskRepositoryImpl`，旧 `EvaUpdateGatewayImpl.postEvaTask` 退化为委托壳（异常文案/类型保持不变）。
   - 应用层入口：`EvaTaskServiceImpl.postEvaTask` 改为调用用例；“待办评教消息发送”改为监听 `EvaluationTaskPostedEvent` 并在事务提交后发送（避免回滚误推）。

11) **事件载荷逐步语义化（中长期）**
   - 当前为了行为不变，事件仍携带 `Map<String, Map<Integer,Integer>>` 作为过渡载荷；
   - 后续可逐步替换为更明确的字段（广播文案、撤回任务列表等），并为 MQ + Outbox 做准备（先不做优化，等收敛完成后再演进）。

12) ✅ **已完成：收敛评教删除写侧（`EvaDeleteGatewayImpl.deleteEvaRecord/deleteEvaTemplate`）**
   - 背景：删除评教记录/模板涉及跨表校验、快照清理与缓存失效，是评教域写侧的重要入口。
   - 目标：按“用例 + 端口 + 旧 gateway 委托壳”的标准步骤，把写侧流程收敛到 `bc-evaluation`（行为不变）。
   - 用例与端口：`bc-evaluation` 新增 `DeleteEvaRecordUseCase/DeleteEvaTemplateUseCase` 与端口/异常，并补齐纯单测（提交：`ea928055`）。
   - infra 端口实现：`eva-infra` 新增 `DeleteEvaRecordRepositoryImpl/DeleteEvaTemplateRepositoryImpl`，旧 `EvaDeleteGatewayImpl` 两个方法退化为委托壳（提交：`07b65663`）。
   - 应用层入口：`eva-app` 的 `EvaRecordServiceImpl/EvaTemplateServiceImpl` 改为调用用例，并将异常映射回历史 `QueryException/UpdateException`（提交：`05900142`）。

13) ✅ **已完成：课程读侧渐进收敛（`CourseQueryGatewayImpl`）**
   - 背景：课程查询（分页/课表/评教统计/移动端周期课程等）存在大量 query 与组装逻辑，长期堆在 gateway 内。
   - 目标：按“先结构化 QueryRepo，再谈 CQRS 投影表”的策略，抽离查询仓储并让 gateway 退化为委托壳（行为不变）。
   - 落地：新增 `CourseQueryRepo/CourseQueryRepository`，旧 `CourseQueryGatewayImpl` 退化为委托壳（提交：`ba8f2003`）。

14) ✅ **已完成：评教读侧渐进收敛（`EvaQueryGatewayImpl`）**
   - 背景：评教读侧统计/分页/聚合仍大量集中在 `EvaQueryGatewayImpl`，是下一阶段“结构化读模型”的高收益目标。
   - 落地：抽取 `EvaQueryRepo`/`EvaQueryRepository`，`EvaQueryGatewayImpl` 退化为委托壳（保持统计口径与异常文案不变）。

15) **下一步推荐：评教读侧进一步解耦（保持行为不变）**
   - 进展：已完成“统计/导出、任务、记录、模板”查询端口拆分（`EvaStatisticsQueryPort` / `EvaTaskQueryPort` / `EvaRecordQueryPort` / `EvaTemplateQueryPort`），应用层相关查询改走新端口。
   - 进展补充：旧 `EvaQueryGatewayImpl` 已移除，应用层已完全切换到细分 QueryPort。
   - 未完成清单（下一步）：继续按用例维度细化 QueryService（任务/记录/模板），并考虑逐步收敛/拆分 `EvaQueryRepo` 内部实现（保持统计口径不变）。
   - 规则补充：下一会话**每个步骤结束**都需跑最小回归用例并记录结果。
   - 约束：每个小步完成后都执行 `mvn -pl start -am test -Dmaven.repo.local=.m2/repository` 并据失败补强回归。

16) **当前未收敛清单（供下个会话优先处理）**
   - 系统管理写侧：菜单写侧主链路（`MenuUpdateGatewayImpl.updateMenuInfo/deleteMenu/deleteMultipleMenu/createMenu` 等）仍在旧 gateway（保持行为不变；可在后续会话按“入口用例化 → 端口搬运 → 委托壳”整体收敛到 `bc-iam`）。
   - 系统管理写侧：角色写侧其余入口（`RoleUpdateGatewayImpl.updateRoleInfo/updateRoleStatus/deleteRole/createRole` 等）仍在旧 gateway（保持行为不变；可按同套路继续收敛到 `bc-iam`）。
   - 评教读侧：`EvaQueryRepo` 仍为大聚合 QueryRepo，需继续拆分（保持统计口径不变）。
   - AI 报告 / 审计日志：已启动 `bc-ai-report` / `bc-audit` 骨架并接入组合根；审计日志写链路已收敛，且审计日志协议对象已开始从 `eva-client` 迁移到 `bc-audit`（保持行为不变；详见 0.9/0.10）。

17) ✅ **已完成：IAM 域 `UserUpdateGatewayImpl.deleteUser` 收敛到 `bc-iam`（保持行为不变）**
   - 落地提交链：`5f08151c/e23c810a/cccd75a3/2846c689`。
   - 关键约束（行为快照，必须保持）：
     - 异常类型/异常文案不变（尤其是 `checkAdmin` 的 `"初始管理员账户不允许此操作"`、以及 `checkIdExistence` 的 `"用户id不存在"`）。
     - 顺序与时机不变：DB 删除 → LDAP 删除 → 角色解绑 → 缓存失效 → 日志记录。
     - 缓存 key/area 不变：沿用旧 `handleUserUpdateCache` 失效清单（含 `COURSE_LIST_BY_SEM` 等）。
   - 关键落地点（便于快速定位）：
     - 旧 gateway（已退化委托壳）：`eva-infra/src/main/java/edu/cuit/infra/gateway/impl/user/UserUpdateGatewayImpl.java`
     - 用例与端口：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/DeleteUserUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/UserDeletionPort.java`
     - 端口适配器：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/UserDeletionPortImpl.java`
     - 组合根：`eva-app/src/main/java/edu/cuit/app/config/BcIamConfiguration.java`

18) ✅ **已完成：系统管理读侧 `UserQueryGatewayImpl.fileUserEntity` 收敛到 `bc-iam`（保持行为不变）**
   - 背景：`fileUserEntity` 负责“用户实体装配”（角色 + 菜单），是典型的读侧聚合装配逻辑，长期堆在旧 gateway 内。
   - 目标：按“用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳”的套路把装配逻辑收敛到 `bc-iam`（行为不变）。
   - 落地提交链：`3e6f2cb2/8c245098/92a9beb3`（文档同步：`0a7802e2`）。
   - 关键约束（行为快照，必须保持）：
     - 异常类型/异常文案不变：菜单查询失败仍抛 `SysException("菜单查询出错，请联系管理员")`，且保持 `log.error("菜单查询出错", sysException)`。
     - 组合方式不变：角色来源仍为 `userQueryGateway.getUserRoleIds(userId)`；菜单来源仍为 `roleQueryGateway.getRoleMenuIds(roleId)` 并逐个 `menuQueryGateway.getOne(menuId)`。
     - 缓存不变：`findById/findByUsername/page` 的 `@LocalCached` 仍保留在旧 `UserQueryGatewayImpl` 上（入口不变）。
   - 关键落地点（便于快速定位）：
     - 用例：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/FindUserByIdUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/FindUserByUsernameUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/PageUserUseCase.java`
     - 端口：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/UserEntityQueryPort.java`
     - 端口适配器：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/UserEntityQueryPortImpl.java`
     - 旧 gateway（已退化委托壳）：`eva-infra/src/main/java/edu/cuit/infra/gateway/impl/user/UserQueryGatewayImpl.java`
     - 组合根：`eva-app/src/main/java/edu/cuit/app/config/BcIamConfiguration.java`

19) ✅ **已完成：系统管理读侧 `UserQueryGatewayImpl.findIdByUsername/findUsernameById/getUserStatus/isUsernameExist` 收敛到 `bc-iam`（保持行为不变）**
   - 落地提交链：`9f664229/38384628/de662d1c/8a74faf5`。
   - 关键约束（行为快照，必须保持）：
     - 缓存读取语义不变：仍需先尝试读取 `LocalCacheManager` 中的 `ONE_USER_ID/ONE_USER_USERNAME`（key 分别为 `String.valueOf(id)` 与 `username`）。
     - 返回值语义不变：`findIdByUsername/findUsernameById/getUserStatus` 仍保持历史 `Optional/null/NPE` 表现（尤其 `getUserStatus` 在 DB 回源时仍可能触发历史 NPE）。
   - 关键落地点（便于快速定位）：
     - 用例：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/FindUserIdByUsernameUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/FindUsernameByIdUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/GetUserStatusUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/IsUsernameExistUseCase.java`
     - 端口：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/UserBasicQueryPort.java`
     - 端口适配器：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/UserBasicQueryPortImpl.java`
     - 旧 gateway（已退化委托壳）：`eva-infra/src/main/java/edu/cuit/infra/gateway/impl/user/UserQueryGatewayImpl.java`
     - 组合根：`eva-app/src/main/java/edu/cuit/app/config/BcIamConfiguration.java`

20) ✅ **已完成：系统管理读侧 `UserQueryGatewayImpl.findAllUserId/findAllUsername/allUser/getUserRoleIds` 收敛到 `bc-iam`（保持行为不变）**
   - 落地提交链：`56bbafcf/7e5f0a74/bc5fb3c6/6a1332b0`。
   - 关键约束（行为快照，必须保持）：
     - 缓存不变：`@LocalCached` 仍保留在旧 `UserQueryGatewayImpl` 上（key/area 与命中语义保持不变）。
     - 查询/映射不变：字段选择与 `userConverter.toUserSimpleResult` 映射不变；`getUserRoleIds` join 条件不变。
   - 关键落地点（便于快速定位）：
     - 用例：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/FindAllUserIdUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/FindAllUsernameUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/AllUserUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/GetUserRoleIdsUseCase.java`
     - 端口：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/UserDirectoryQueryPort.java`
     - 端口适配器：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/UserDirectoryQueryPortImpl.java`
     - 旧 gateway（已退化委托壳）：`eva-infra/src/main/java/edu/cuit/infra/gateway/impl/user/UserQueryGatewayImpl.java`
     - 组合根：`eva-app/src/main/java/edu/cuit/app/config/BcIamConfiguration.java`

21) ✅ **已完成：系统管理写侧（角色/菜单）缓存与权限变更副作用收敛到 `bc-iam`（保持行为不变）**
   - 背景：权限/菜单/角色变更会触发多层缓存失效与审计日志，历史上长期堆在旧 gateway；属于 IAM 模块化单体的关键“副作用一致性”能力。
   - 目标：优先收敛 `RoleUpdateGatewayImpl.assignPerms/deleteMultipleRole`（含缓存失效与 `LogUtils.logContent`），再评估是否把菜单写侧（`MenuUpdateGatewayImpl`）整体收敛到 `bc-iam`。
   - 行为快照（必须保持）：
     - `assignPerms` 顺序不变：`checkRoleId` → 删除原 `role_menu` → 插入新 `role_menu` → `handleRoleUpdateCache` → `LogUtils.logContent(...)`。
     - `deleteMultipleRole` 顺序不变：先逐个 `checkDefaultRole`/`checkRoleId` 并提前 `handleRoleUpdateCache`，再逐个删除 `sys_role/sys_user_role/sys_role_menu`，最后 `LogUtils.logContent(tmp + " 角色")`。
     - `handleRoleUpdateCache/handleUserMenuCache` 规则不变：失效 `ALL_ROLE/ONE_ROLE/ROLE_MENU/ALL_MENU/ONE_USER_ID/ONE_USER_USERNAME` 等，并保持 `userQueryGateway.findUsernameById(...).orElse(null)` 的空值语义。
   - 行为快照补充（已用 Serena 复核，便于搬运时逐条对照）：
     - 角色权限分配（`eva-infra/.../user/RoleUpdateGatewayImpl.assignPerms`）：
       - `checkRoleId`：`select(id, roleName).eq(id)`；不存在抛 `BizException("角色id: " + id + " 不存在")`。
       - 删除旧权限：`roleMenuMapper.delete(Wrappers.lambdaUpdate().eq(roleId))`（注意是 update wrapper）。
       - 插入新权限：逐个 `roleMenuMapper.insert(new SysRoleMenuDO().setMenuId(id).setRoleId(roleId))`（顺序与入参 list 顺序一致）。
       - 缓存失效：`handleRoleUpdateCache(roleId)` 内部第一个失效为 `invalidateCache(null, userCacheConstants.ALL_ROLE)`；随后按 `ONE_ROLE/ROLE_MENU` 与 `ONE_USER_ID/ONE_USER_USERNAME` 失效（key 都是 `String.valueOf(...)`，用户名 key 允许 `null`）。
       - 日志：最后 `LogUtils.logContent(tmp.getRoleName() + " 角色(" + tmp.getId() + ")的权限")`（必须最后执行）。
     - 角色批量删除（`eva-infra/.../user/RoleUpdateGatewayImpl.deleteMultipleRole`）：
       - 两段循环：第一段逐个 `checkDefaultRole`（默认角色抛 `BizException("默认角色不允许此操作")`）→ `checkRoleId` → 立刻 `handleRoleUpdateCache(roleId)`；第二段再逐个执行删除（`sys_role` → `sys_user_role` → `sys_role_menu`）。
       - 日志：最后 `LogUtils.logContent(tmp + " 角色")`（`tmp` 为 `List<SysRoleDO>`，来源于第一段循环 `checkRoleId` 返回值）。
     - 菜单变更触发的用户缓存失效（`eva-infra/.../user/MenuUpdateGatewayImpl.handleUserMenuCache`）：
       - 先 `invalidateCache(null, userCacheConstants.ALL_MENU)`；再按 `menuId -> roleId -> userId` 链路失效 `ONE_ROLE/ONE_USER_ID/ONE_USER_USERNAME`（用户名 key 允许 `null`）。
       - 注意 `deleteMenu` 链路中 `handleUserMenuCache(menuId)` 会在 `deleteMenuAndChildren` 内部对每个节点调用一次，且在 `deleteMenu` 方法末尾对根节点再调用一次（即根节点会触发两次失效；必须保持）。
   - 调用方入口（便于回归对照）：
     - 角色：`eva-app/src/main/java/edu/cuit/app/service/impl/user/RoleServiceImpl.java` → `roleUpdateGateway.assignPerms/deleteMultipleRole`
     - 菜单：`eva-app/src/main/java/edu/cuit/app/service/impl/user/MenuServiceImpl.java` → `menuUpdateGateway.updateMenuInfo/deleteMenu/deleteMultipleMenu`
   - 落地提交链：`f8838951/91d13db4/7fce88b8/d6e3bed1/4d650166/46e666f9`
   - 关键落地点（便于快速定位）：
     - 用例：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/AssignRolePermsUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/DeleteMultipleRoleUseCase.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/usecase/HandleUserMenuCacheUseCase.java`
     - 端口：`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/RolePermissionAssignmentPort.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/RoleBatchDeletionPort.java`、`bc-iam/application/src/main/java/edu/cuit/bc/iam/application/port/UserMenuCacheInvalidationPort.java`
     - 端口适配器：`bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/RoleWritePortImpl.java`、`bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/UserMenuCacheInvalidationPortImpl.java`
     - 旧 gateway（已退化委托壳）：`eva-infra/src/main/java/edu/cuit/infra/gateway/impl/user/RoleUpdateGatewayImpl.java`、`eva-infra/src/main/java/edu/cuit/infra/gateway/impl/user/MenuUpdateGatewayImpl.java`
     - 组合根：`eva-app/src/main/java/edu/cuit/app/config/BcIamConfiguration.java`

22) **下一会话推荐重构任务：AI 报告 / 审计日志模块化（保持行为不变）**
   - 目标：启动 `bc-ai-report` / `bc-audit` 的最小骨架，并选择 1 条高价值写链路按“用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳”收敛（异常文案与副作用顺序保持不变）。

23) ✅ **已完成：系统管理写侧继续收敛（菜单写侧主链路整体收敛到 `bc-iam`，保持行为不变）**
   - 收敛范围：`MenuUpdateGatewayImpl.updateMenuInfo/deleteMenu/deleteMultipleMenu/createMenu`
   - 强约束保持：缓存注解 area/key 与触发时机不变；`deleteMenu` 根节点缓存失效触发两次与递归删除顺序不变；异常文案与日志顺序不变。
   - 落地提交：`f022c415`

24) ✅ **已完成：系统管理写侧继续收敛（角色写侧剩余入口收敛到 `bc-iam`，保持行为不变）**
   - 收敛范围：`RoleUpdateGatewayImpl.updateRoleInfo/updateRoleStatus/deleteRole/createRole`
   - 强约束保持：异常文案不变；缓存失效 key 与时机不变；日志文案与触发时机不变；副作用顺序不变。
   - 落地提交：`64fadb20`

25) ⏳ **中期里程碑：BC “自包含三层结构”落地（以 `bc-iam` 先试点）**
   - 目标：让 `bc-iam` 在模块边界上也更贴近最终形态（最少做到 package 结构完整；更进一步可拆 Maven 子模块）。
   - 推荐拆分路径（先小步、可回滚、行为不变）：
     1) 先统一文档与命名：将“旧 gateway = 委托壳 / 入口适配器”的约定固化（本条目已完成）。
     2) ✅ 以 `bc-iam` 为试点：已引入 `bc-iam-infra` 子模块骨架，并由 `eva-app` 装配（提交：`42a6f66f`）。
    3) ✅ 已完成：将 `bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/*` 作为新落点，并已完成从 `eva-infra` 的迁移（行为不变；提交链：`070068ec/03ceb685/02b3e8aa/6b9d2ce7/5aecc747/1c3d4b8c`）。
     4) 下一阶段（仍保持行为不变）：将 IAM 的 DAL（DO/Mapper/SQL）从 `eva-infra` 逐步抽离到 `bc-iam-infra`（或更通用的 shared 模块，后置决策），最终让 `bc-iam-infra` 去掉对 `eva-infra` 的依赖。
     5) 当 `bc-iam` 完成三层自包含后，再评估把 `eva-domain` 中与 IAM 强相关的类型逐步迁移到 `bc-iam-domain`（谨慎推进，避免牵连其它 BC；共享部分沉淀到 `shared-kernel`）。

26) **下一会话推荐重构任务：`bc-iam-infra` 继续收敛（先抽离 IAM DAL，保持行为不变）**
   - 背景：目前 `bc-iam-infra` 仍通过 Maven 依赖 `eva-infra` 来复用 DAL/Starter；端口适配器已迁移，但“基础设施依赖归属”尚未完成闭环。
   - 目标：将 `edu.cuit.infra.dal.database.*user*`（DO/Mapper 及相关 XML/配置）逐步从 `eva-infra` 抽离出来，使 `bc-iam-infra` 最终可去掉对 `eva-infra` 的依赖（保持行为不变）。
     - 说明：由于 `SysUserMapper` 等被 `eva-infra` 内多个模块直接使用，本阶段优先采用“共享 DAL 子模块（`eva-infra-dal`）”承接迁移，以避免 Maven 循环依赖并保持最小可回滚。
   - 建议拆分与里程碑/提交点（每步一条 commit；每步跑最小回归；每步结束先写清下一步里程碑再收尾）：
    1) ✅ Serena：盘点 `bc-iam/infrastructure/src/main/java/edu/cuit/infra/bciam/adapter/*` 实际依赖的 Mapper/DO 清单（按“user/role/menu”分组）。
        - Mapper：`SysUserMapper`、`SysUserRoleMapper`、`SysRoleMapper`、`SysRoleMenuMapper`、`SysMenuMapper`
        - DO：`SysUserDO`、`SysUserRoleDO`、`SysRoleDO`、`SysRoleMenuDO`、`SysMenuDO`
        - XML：`eva-infra/src/main/resources/mapper/user/SysUserMapper.xml`、`SysUserRoleMapper.xml`、`SysRoleMapper.xml`、`SysRoleMenuMapper.xml`、`SysMenuMapper.xml`
     2) ✅ 新建 `bc-iam-infra` 内部 DAL 包路径（先空骨架 + 资源目录），不迁代码；确保编译通过。
     3) ✅ 迁移 `SysUser*` 相关 DO/Mapper/XML 到共享模块 `eva-infra-dal`（保持包名/namespace/SQL/异常/顺序一致）；跑最小回归。
     4) ✅ 迁移 `SysRole*`/`SysRoleMenu*` 相关 DO/Mapper/XML 到共享模块 `eva-infra-dal`；跑最小回归。
     5) ✅ 迁移 `SysMenu*` 相关 DO/Mapper/XML 到共享模块 `eva-infra-dal`；跑最小回归。
     6) ✅ 盘点 `bc-iam-infra` 仍依赖 `eva-infra` 的类型/Bean 清单（含 Converter/LDAP/缓存常量/工具等），作为后续 shared 模块拆分输入；跑最小回归。
     7) ✅ 新增 shared 子模块骨架 `eva-infra-shared`（不迁代码，仅提供后续抽离落点）；跑最小回归。
     8) 将 `bc-iam-infra` 仍依赖的 Converter/LDAP/缓存常量/工具等，从 `eva-infra` 逐步迁移到 `eva-infra-shared`（保持包名不变），并调整依赖；跑最小回归。
     9) 去掉 `bc-iam-infra` 对 `eva-infra` 的依赖（或至少只保留必须的 starter），确保仍能通过最小回归。
