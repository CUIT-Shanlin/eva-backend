# 会话交接摘要（用于新对话继续）

> 仓库：`/home/lystran/programming/java/web/eva-backend`  
> 当前分支：`ddd`  
> 目标方向：把当前“按技术分层”的单体，逐步重构为“按业务限界上下文分模块”的 **DDD 模块化单体**，并预留低成本拆微服务路径（先拆服务、暂时共享库，小步快跑）。

---

## 1. 关键业务结论（必须记住）

用户已明确确认的业务语义：

1) `cour_inf.week`：表示学期从 `semester.start_date` 起算的第 **n 周**（单周周次）。  
2) `cour_inf.day`：`day=1` 表示 **周一**，`day=n` 表示周 n（周一是一周开始）。  
3) 同一课次允许多人评教（这是必须如此的业务前提）。  
4) 模板锁定规则：**只要有人评教过就锁定**，锁定后不允许切换课程模板。  
5) “高分次数”的阈值：希望 **可配置**（后续可加入评教配置，如 `highScoreThreshold`）。  
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
- 应用层：`eva-app/.../ICourseDetailServiceImpl.updateCourses()` -> `courseUpdateGateway.updateCourses(...)`
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
  - `updateCourse()`：当 templateId 发生变化时，先 `courseTemplateLockService.assertCanChangeTemplate(courseId, semId)`，锁定则抛 `UpdateException`
  - `updateCourses()`：批量切换前先整体校验，若存在锁定课程则整体失败（避免部分成功部分失败）

注意：中间曾临时实现了一个基础设施校验器（现在已不再被主流程使用）：
- `eva-infra/.../CourseTemplateLockChecker.java`（已提交但目前属于“可删的遗留”候选）
- 仍有对应 Mockito 单测：`start/src/test/.../CourseTemplateLockCheckerTest.java`

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
  - 只跑指定测试（多模块时需要忽略未命中模块）：  
    - `mvn -pl start -am test -q -Dtest=CourseTemplateLockCheckerTest -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.repo.local=.m2/repository`

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

- `git status` 显示当前仍有未提交改动：`M .gitignore`（需要在新会话确认 diff 并决定是否提交）。

---

## 7. 下一步建议任务（供新会话继续）

推荐按“继续模块化 + 小步 commit”的方式推进：

1) **清理遗留/重复实现**（可作为一个小 commit）  
   - `CourseTemplateLockChecker` 目前已被 bc-template 取代，可考虑删除该类与对应测试（或迁移测试去验证 `CourseTemplateLockQueryPortImpl`）。  

2) **把“课程模板切换”从 infra gateway 进一步上移到业务用例（bc-course）**  
   - 新增 `bc-course` 模块骨架（domain/application/ports），把“批量切换模板”做成 `ChangeCourseTemplateCommand` 用例。  
   - `eva-infra` 只实现 repository/port；Controller/Service 只调 bc-course application。  

3) **落实“高分阈值可配置”**  
   - 在评教配置中增加 `highScoreThreshold`，并统一统计口径（导出/看板/AI 报告均使用同一策略）。  

4) **继续事件化联动**  
   - 提交评教已事件化消息清理；后续可以把模板快照/锁定、统计刷新、AI 报告失效/重算也改为订阅 `EvaluationSubmittedEvent`。  

