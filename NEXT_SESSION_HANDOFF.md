# 会话交接摘要（用于新对话继续）

> 仓库：`/home/lystran/programming/java/web/eva-backend`  
> 当前分支：`ddd`  
> 目标方向：把当前“按技术分层”的单体，逐步重构为“按业务限界上下文分模块”的 **DDD 模块化单体**，并预留低成本拆微服务路径（先拆服务、暂时共享库，小步快跑）。

---

## 0. 本轮会话增量总结（2025-12-18）

本轮会话聚焦“DDD 渐进式重构（不做功能优化）”，继续执行方案 B/C，并补齐课程模块的收敛闭环：

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

本轮新增提交（按时间顺序）：
- `a122ff58 feat(bc-course): 引入课程BC用例与基础设施端口实现`
- `285db180 feat(bc-messaging): 课程操作副作用事件化并收敛消息发送`
- `4a22fdaf fix(gitignore): 仅忽略仓库根 data 目录`（避免误伤 `eva-client/.../dto/data` 包路径）
- `60fe256d feat(eva-config): 支持高分阈值可配置`
- `a330d8e7 test(bc-template): 迁移模板锁定校验并清理旧实现`

验证（建议使用 Java17；网络受限时再加 `-o` 离线）：
- 切换 JDK（本机已安装）：`sdk use java 17.0.17-zulu`  
  或：`export JAVA_HOME=\"$HOME/.sdkman/candidates/java/17.0.17-zulu\"`
- `mvn -pl bc-course -am test -q -Dmaven.repo.local=.m2/repository`
- `mvn -pl bc-messaging -am test -q -Dmaven.repo.local=.m2/repository`
- `mvn -pl eva-infra -am -DskipTests test -q -Dmaven.repo.local=.m2/repository`

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

验证命令（离线优先，避免网络受限）：
- `mvn -o -pl bc-course -am test -q -Dmaven.repo.local=.m2/repository`
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

当前 `git log --oneline -n 20` 关键提交如下（最新在上）：

- `4d9e624b chore: gitignore add data`（忽略 data 等内容，避免提交大文件）  
- `539ffd44 chore: 扩展忽略规则覆盖 Windows 风格路径目录`  
- `e4042b56 feat(template): 引入模板锁定服务并接入课程模板切换`  
- `b33e7d2c chore: 忽略 Linux 下误生成的 Windows 路径目录`  
- `a33bf203 test(course): 添加课程模板锁定校验单测`  
- `b8806de4 update: security dependency version`（用户更新依赖版本）  
- `d925b4ae fix(evaluation): 修复提交评教缓存失效参数类型`  
- `46bcf379 fix(course): 已评教课程模板锁定禁止切换`（早期锁定实现，后续被 bc-template 接管）  
- `beb6a3b7 chore: 添加 bc-template 模块骨架`  
- `01ef8339 chore: 忽略 Serena 元数据目录`  
- `2587de84 test(evaluation): 添加提交评教用例单元测试`  
- `9c1d5daa update: 修改依赖源`（用户更新依赖源）  
- `ab28d80a feat(evaluation): 接入提交评教用例并事件化清理消息`  
- `887ed43b feat(evaluation): 引入提交评教用例与持久化端口`  
- `bf5c05e3 chore: 添加 bc-evaluation 模块骨架`

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
   - 切 JDK 到 17（见上文），再跑一次 `mvn -pl bc-course -am test` / `mvn -pl bc-messaging -am test` 快速验收。
   - 用 Serena 重新索引（用户要求“开始任务先更新一次索引”）。

1) **继续收敛“教师自助课表”链路（IUserCourseServiceImpl）**
   - 目标：把 `deleteSelfCourse()`、`updateSelfCourse()` 里直接 `msgResult/msgService` 的跨域副作用，统一事件化并收敛到 `bc-messaging`（保持行为不变）。
   - 推荐落地顺序：
     - 先引入 `AfterCommitEventPublisher`（已具备）；
     - 让两方法返回的 `Map<String, Map<Integer,Integer>>` 复用 `CourseOperationSideEffectsEvent`；
     - 只改“消息发送/撤回评教消息”触发点，不动主流程逻辑。

2) **继续压扁旧 `CourseUpdateGatewayImpl.updateCourse()`（当前仍是大泥球核心之一）**
   - 目标：为“修改课程信息”新增 `bc-course` 用例（例如 `UpdateCourseInfoUseCase`），并把现有 DB/缓存/日志迁移到 `eva-infra` 的端口实现。
   - 验收标准：旧 gateway 变为委托壳；用例有纯单测；行为不变。

3) **事件载荷逐步语义化（中长期）**
   - 当前为了行为不变，事件仍携带 `Map<String, Map<Integer,Integer>>` 作为过渡载荷；
   - 后续可逐步替换为更明确的字段（广播文案、撤回任务列表等），并为 MQ + Outbox 做准备（先不做优化，等收敛完成后再演进）。
