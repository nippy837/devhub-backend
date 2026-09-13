# DevHub 后端

Java 21、Spring Boot、MyBatis、MySQL。启动前配置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`，默认端口为 8080。

## 登录与贪吃蛇排行

上线前由数据库管理员手动执行 [sql/20260913_auth_snake.sql](sql/20260913_auth_snake.sql)。
脚本新增 `app_users`、`snake_games` 两张表，应用不自动建表，也不修改现有账号管理的数据和访问方式。
未执行 DDL 时，新的注册、登录和排行接口不可用；原有账号接口保持原样。

| 请求 | 用途 |
| --- | --- |
| `POST /api/auth/register` | `username`、`password`，注册并登录 |
| `POST /api/auth/login` | 用户名密码登录 |
| `GET /api/auth/me` | 当前用户 `{id, username}`，游客为 null |
| `POST /api/auth/logout` | 使当前会话失效 |
| `POST /api/snake/games` | 登录后提交 `difficulty`（easy/normal/hard），返回 `{id, seed}` |
| `POST /api/snake/games/{id}/finish` | 登录后提交 `{moves: "RRDD..."}`，返回服务端复算的 `{score, outcome}` |
| `GET /api/snake/leaderboard` | 公开的前 50 名 `entries`，以及当前用户的 `myBest` |

以上写接口必须携带 `Content-Type: application/json` 和 `X-DevHub-Request: 1`。
使用同源 HttpOnly、SameSite=Strict 会话 Cookie，不开放跨域访问。
用户名为 3–24 位英文字母、数字或下划线，不区分大小写；密码 8–128 位，使用独立随机盐与 PBKDF2-HMAC-SHA256（600000 轮）存储。
注册和登录按来源地址限制为每分钟 30 次。会话闲置 7 天过期，后端重启后需重新登录，数据库成绩继续保留。
HTTPS 部署应设置 `SESSION_COOKIE_SECURE=true`；本地 HTTP 开发默认 false。

棋盘为 20×20，初始蛇长 3，最多同时存在 8 颗果实，每颗 20 分，最高 7940 分。
左右、上下边界互通，撞到自己失败，填满棋盘获胜；可主动结束并记分。
服务端保存对局种子，按相同随机算法复算最多 50000 步操作和实际耗时，不接收客户端提供的分数。
每个方向字符表示一个移动周期：U/D/L/R；每周期耗时为轻松 200ms、标准 140ms、挑战 90ms。
同一对局只计分一次，重复提交返回既有成绩；仅对局所属用户可提交。每用户每分钟最多开始 20 局。
复算用于防止直接篡改分数，不防止自动化代玩。排行榜按每位用户的最高正分排序，同分按用户 ID 升序排列，三个难度共用排行榜。
未登录时可本地试玩，游客成绩不补录至账号。此版本与旧版本计分不同，游客最高分使用新的本地存储键。

## 账号接口

| 操作 | 请求 | 成功响应中的 data |
| --- | --- | --- |
| 分页查询 | `GET /api/accounts?page=1&pageSize=10&keyword=demo&environment=test` | 分页对象 |
| 新增 | `POST /api/accounts` | 新账号 ID（HTTP 201） |
| 编辑 | `PUT /api/accounts?id=10` | null（HTTP 200） |
| 删除 | `DELETE /api/accounts?id=10` | null（HTTP 200） |

响应统一为 `{"code":0,"message":"success","data":...}`。参数错误返回 HTTP 400；编辑或删除不存在的账号返回 HTTP 404。

新增、编辑接受 `systemName`、`environment`、`username`、`password`、`loginUrl`、`remark` 六个字段。
前三项必填，环境为 `dev`、`test` 或 `prod`。PUT 是完整更新，可选字段为空表示清空；密码保留首尾空格。

分页默认第 1 页、每页 10 条，只接受 5、10、50 条。关键词匹配系统名、用户名、备注，环境为精确筛选。
先筛选再分页，按照 ID 降序排列。页码超过最后一页时返回最后有效页；空列表返回第 1 页。

分页对象包含：

- `records`：当前页 AccountVO 数组。
- `total`：符合筛选条件的总条数。
- `page`、`pageSize`：实际页码、每页条数。
- `stats`：全部账号的 `totalAccounts`、`systemCount`、`testCount`。
- `environments`：全部账号使用的环境列表。

**发布时需同步更新前端**：查询的 data 已从数组改为分页对象。

## 代码阅读顺序

本项目的数据对象使用普通 Java `class`，不使用 `record`；getter/setter 和构造方法可以使用 Lombok 生成。

1. `AccountController`：用 `@RequestParam("id")` 绑定查询参数，用 `@RequestBody` 绑定请求体，通过 `@Valid` 和 `@Positive` 触发校验。
2. `AccountCreateDTO` / `AccountUpdateDTO`：继承 `AccountWriteDTO` 的共同字段校验；`AccountQueryDTO` 校验分页参数。
3. `AccountServiceImpl`：处理业务规则、空格整理和页码修正。删除影响行数为 0 时抛出业务异常。
4. `AccountMapper`：只负责 SQL 和参数绑定；COUNT 和分页查询复用同一筛选条件。
5. `GlobalExceptionHandler`：把业务异常转换成前端可以识别的 HTTP 状态与 Result。

编辑使用事务和 `SELECT ... FOR UPDATE` 锁定目标行，因此不会把“提交相同内容”误判为不存在，也避免检查存在后记录被并发删除。
分页的多条读取放在同一个可重复读事务中，使本次响应的总数、记录与统计保持一致；不同请求之间会看到最新提交的数据。

## 操作日志

账号增删改查请求使用 SLF4J 输出运行日志，并将操作类型、目标 ID、请求结果和耗时写入 `account_operation_logs`。
启动新版后端前，需在账号所在数据库创建日志表；应用不会自动建表。
成功请求、参数错误和账号不存在等失败请求都会记录，密码和完整请求体不会进入日志。
日志以独立事务保存；入库失败时输出 SLF4J 错误日志，不改变已经完成的账号操作结果。
可通过响应头 `X-Request-Id` 关联数据库记录与运行日志；表中的 `created_time` 使用 UTC。

## 构建

使用 Java 21 运行：

```sh
mvn clean package
```

已移除后端测试类、测试专用配置、测试建表 SQL 和测试依赖。
现有 MySQL 表沿用 `created_time`、`updated_time` 列，无需新增业务字段。
