# AI 对话接入

链路：Vue `#/tools/ai` → 同源 `POST /api/ai/chat` → Spring Boot → OpenAI Responses API。
此功能不复用 Codex 登录凭据或当前 Codex 会话。请使用自己的 API Key，并在后端进程环境中设置：

```dotenv
OPENAI_API_KEY=替换为你的API密钥
OPENAI_MODEL=替换为你的账号可调用的模型ID
OPENAI_BASE_URL=https://api.openai.com/v1
```

`OPENAI_MODEL` 没有默认值；Key 或模型为空时 AI 功能显示未配置，其他页面可继续使用。
`OPENAI_BASE_URL` 可省略。若使用代理网关，必须支持 `POST /responses` 的请求和响应格式，填写基础地址（通常以 `/v1` 结尾），不要填写完整 `/responses` 路径。仅支持远端 HTTPS，本机调试允许 HTTP；不跟随上游重定向。

本地启动：通过 IDE 的运行配置或 Shell 向 Spring Boot 注入上述环境变量，同时保留原有的数据库环境变量。Spring Boot 不会自动加载 `.env` 文件。

Docker Compose：在服务器 Compose 文件的 `backend.environment` 增加以下映射，然后在 Compose 同目录 `.env` 中填入实际值，并重新创建后端容器：

```yaml
OPENAI_API_KEY: ${OPENAI_API_KEY:-}
OPENAI_MODEL: ${OPENAI_MODEL:-}
OPENAI_BASE_URL: ${OPENAI_BASE_URL:-https://api.openai.com/v1}
```

项目根目录的 `compose.yaml` 已包含映射。前后端是独立 Git 仓库，现有发布工作流不会上传根目录 Compose 文件，因此上线时需要同步更新服务器上的 Compose 配置。不要提交真实密钥，也不要写入前端 `VITE_*` 环境变量。

## 接口与行为

- `GET /api/ai/status`：返回 `{configured, model}`，表示配置是否齐全，不代表已探测上游可达性。
- `POST /api/ai/chat`：需要现有账号登录，以及 `X-DevHub-Request: 1` 请求头。JSON 请求体为 `{"messages":[{"role":"user","content":"你好"}]}`，成功时 `data` 为 `{content, model, truncated}`。
- 使用现有的登录用户和 Cookie，无需新建表；项目的登录功能需已初始化。
- 消息从 `user` 开始、与 `assistant` 交替，最后一条为 `user`。最多 21 条、单条最多 12000 字符、总计最多 60000 字符；页面单次输入最多 4000 字符。
- 页面自动保留最近最多 10 轮完整对话作为上下文，超出长度时减少轮数。不会读取项目文件、账号密码或其他页面数据。
- 每用户每分钟最多 10 次请求、同时最多 1 条；单个后端实例最多同时处理 4 条。多实例部署时需在网关添加统一限流。
- 上游超时 60 秒，前端等待 70 秒，Nginx 已设为 75 秒。回答一次性展示；取消等待只中止浏览器请求，上游仍可能完成并计费。
- `max_output_tokens=2048`，达到生成上限时显示可继续追问的提示。上游错误正文不返回浏览器、不写入日志。
- 历史仅保存在页面内存，切换工具保留，刷新、新建对话或账号变化时清空。请求设置 `store=false`；这不代表服务提供方的其他数据保留政策被关闭。

参考：[OpenAI Responses API](https://developers.openai.com/api/reference/cli/resources/responses/methods/create)。
