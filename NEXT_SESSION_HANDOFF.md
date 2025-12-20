# 会话交接摘要（用于新对话继续）

> 仓库：`/home/lystran/programming/java/web/eva-backend`  
> 当前分支：`ddd`  
> 目标方向：把当前“按技术分层”的单体，逐步重构为“按业务限界上下文分模块”的 **DDD 模块化单体**，并预留低成本拆微服务路径（先拆服务、暂时共享库，小步快跑）。

---

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
          - `bc-messaging/src/main/java/edu/cuit/bc/messaging/application/event/CourseOperationMessageMode.java`
          - `bc-messaging/src/main/java/edu/cuit/bc/messaging/application/event/CourseOperationSideEffectsEvent.java`
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
   - IAM 域：`UserUpdateGatewayImpl.deleteUser` 仍在旧 gateway（DB 删除 + LDAP 删除 + 角色解绑 + 缓存失效 + 日志；需保持行为不变）。
   - 系统管理读侧：`UserQueryGatewayImpl.fileUserEntity` 等仍在旧 gateway（可按 QueryPort 渐进收敛，行为不变）。
   - AI 报告 / 审计日志：尚未模块化到 `bc-ai-report` / `bc-audit`。

17) **下一会话推荐重构任务：IAM 域 `UserUpdateGatewayImpl.deleteUser`（保持行为不变）**
   - 背景：`deleteUser` 涉及 DB 删除、LDAP 删除、角色解绑、缓存失效与日志记录，是 IAM 写侧典型“多副作用写流程”，当前仍在旧 gateway。
   - 目标：按“用例 + 端口 + `eva-infra` 端口适配器 + 旧 gateway 委托壳”的套路收敛到 `bc-iam`（行为不变；`updateStatus` 已完成收敛，提交链：`e3fcdbf0/8e82e01f/eb54e13e`）。
   - 建议拆分提交（每步一条 commit，且每步跑最小回归）：
     1) `bc-iam`：新增 `DeleteUserUseCase` + `UserDeletionPort`（命名可对齐现有范式）+ 纯单测（只测“委托端口一次”）。
     2) `eva-infra`：新增端口适配器 `UserDeletionPortImpl`，把旧 `UserUpdateGatewayImpl.deleteUser` 的流程原样搬运（注意顺序与文案）。
     3) `eva-app`：在 `BcIamConfiguration` 装配 `DeleteUserUseCase` Bean。
     4) `eva-infra`：旧 `UserUpdateGatewayImpl.deleteUser` 退化为委托壳（只委托用例）。
     5) 文档闭环：更新 `NEXT_SESSION_HANDOFF.md` / `DDD_REFACTOR_PLAN.md` / `docs/DDD_REFACTOR_BACKLOG.md`，记录提交链与行为约束。
   - 关键约束（必须保持）：
     - 异常类型/异常文案不变（尤其是 `checkAdmin` 的 `"初始管理员账户不允许此操作"`、以及 `checkIdExistence` 的 `"用户id不存在"`）。
     - 顺序与时机不变：DB 删除 → LDAP 删除 → 角色解绑 → 缓存失效 → 日志记录。
     - 缓存 key/area 不变：沿用旧 `handleUserUpdateCache` 失效清单（含 `COURSE_LIST_BY_SEM` 等）。
   - 关键落地点（便于快速定位）：
     - 旧实现：`eva-infra/src/main/java/edu/cuit/infra/gateway/impl/user/UserUpdateGatewayImpl.java`
     - 端口适配器目录：`eva-infra/src/main/java/edu/cuit/infra/bciam/adapter/`
     - 组合根：`eva-app/src/main/java/edu/cuit/app/config/BcIamConfiguration.java`
