# DevHub 后端

Java 21、Spring Boot、MyBatis、MySQL。启动前配置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`，默认端口为 8080。

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
