# 云睿智学｜在线教育平台

**开发与维护：江添琦**

面向 Java 后端实习面试的职业技能课程交易与学习平台。项目采用模块化单体架构，围绕“限量优惠购课、支付事件可靠发布、幂等发放课程资格、视频学习和高频进度保存”形成完整业务闭环，没有为了堆技术照搬庞大的微服务脚手架。项目同时包含可实际操作的学生端和管理端网页。

## 技术栈

- Java 17、Spring Boot 3、Spring Security、JWT、Spring JDBC、Flyway
- MySQL 8、Redis 7、RabbitMQ 3
- Docker Compose、SpringDoc/Swagger、JUnit 5、Mockito
- Vue 3、Vite、Element Plus（学生端网页）

## 核心链路

```text
注册/登录 -> JWT鉴权 -> 浏览职业课程 -> 并发领券 -> 最优优惠组合
          -> 创建订单 -> 金额校验 -> 支付幂等
          -> 本地消息表 -> RabbitMQ确认投递 -> 幂等发放学习资格
          -> 短期凭证播放视频 -> Redis覆盖最新学习进度 -> 停止播放后延迟落MySQL
          -> 测验/积分/结课证明
```

主要亮点：

1. 订单更新与 Outbox 本地消息同事务，RabbitMQ Publisher Confirm 后才标记成功，发布失败指数退避重试；消费者使用唯一索引保证重复事件不会重复发课。
2. 支付回调通过订单行锁、交易号与金额校验实现幂等和防篡改；消费者使用唯一索引实现重复消息幂等。
3. 播放器约每15秒把进度覆盖到 Redis，并创建20秒延迟任务；任务到期后只有缓存值未变化才落库，写库后采用 compare-and-delete 避免误删新的进度。
4. 优惠券库存使用 MySQL 条件更新防超发，Redis 锁减少热点竞争；下单自动枚举可叠加组合并锁券。
5. 课程视频通过学习资格校验和 5 分钟播放凭证控制访问，支持 HTTP Range 分段读取和断点续播。
6. 积分事件持久化后幂等投影到 Redis ZSet，支持实时名次和历史赛季归档；雪花算法生成订单号，Flyway 管理数据库迁移。

消息边界：当前发布端实现本地消息、发布确认和失败重试，消费端实现业务幂等；尚未配置消费失败重试和死信队列，因此不宣传为完整生产级可靠消息闭环。

## 启动与验收

```powershell
cd F:\shixi\yunrui-edu-interview
powershell -ExecutionPolicy Bypass -File .\scripts\start-infra.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\start-app.ps1
```

保持后端窗口运行，另开一个 PowerShell 启动前端：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-frontend.ps1
```

- Swagger：<http://localhost:18080/swagger-ui.html>
- 学生端网页：<http://localhost:18082/index.html>
- 健康检查：<http://localhost:18080/actuator/health>
- RabbitMQ：<http://localhost:15673>（guest / guest）

当前网页可体验注册登录、修改密码、个人资料、课程搜索分类与分页、收藏、购物车、优惠券、下单、模拟支付、异步发课、真实 MP4 播放、断点续播、自动上报章节进度、课程完成度、结课证明、学习笔记、章节测验、课程问答、积分排行、课程评价、站内通知和退款。视频使用 5 分钟有效的短期播放凭证并支持 HTTP 分段读取，未购买课程无法播放。退款采用正式审核流程：学生提交申请，管理员通过或拒绝；通过后订单变为已退款并收回课程权限，处理结果通过站内通知送达。管理员还可以查询用户、启用或停用账号、调整角色，查看课程、收入、待办和课程销量概览，创建课程草稿，维护章节，上传或替换 MP4，创建或停用测验题，回复学生问题，审核退款以及发布、下架课程；停用账号会立即使已有登录令牌失效，删除章节时会同步清理该章节上传的视频文件。演示管理员账号为 `demo_admin`，密码为 `Demo@123456`，其他新注册账号默认是普通学生。

另开终端执行完整验收：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

前端和后端都启动后，可执行覆盖两名用户、正常流程和异常流程的完整测试：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\frontend-full-test.ps1
```

停止应用使用 `Ctrl+C`。停止基础设施执行 `scripts\stop-infra.ps1`。
数据库结构以 `src/main/resources/db/migration` 中的 Flyway 脚本为准。核心业务实现位于 `src/main/java/com/yunrui/edu`；运行及验证脚本位于 `scripts/`。

学生端网页基于公开的[天机学堂用户端](https://github.com/Serendipity-jpg/tj-portal)进行适配，保留原有视觉素材和页面结构，将核心业务接口改接到当前单体后端。该前端仅用于本地学习和项目演示。
