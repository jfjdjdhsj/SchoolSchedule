# 校园作息表 · 设备统计后台（server/）

纯 **Python 标准库**实现的轻量后端（无任何第三方依赖，Python 3.8+ 即可运行），职责：

1. 接收 APP 的匿名使用统计上报（`POST /api/report`）
2. 接收 APP 的用户文字反馈（`POST /api/feedback`）
3. 提供查询接口（`/api/stats`、`/api/devices`、`/api/records`、`/api/feedbacks`，`X-Api-Key` 鉴权）
4. 提供管理网页：设备列表（含 APP 版本）、版本分布、近 7 天活跃、最近上报记录、用户反馈

> ⚠️ **安全提醒**：真实密码与密钥只存放在服务器的 `config.json` 中，
> **任何密钥 / 密码都不得写入本仓库的任何文件**。本目录所有文件均已脱敏。

---

## 一、配置文件 config.json

与 `server.py` 同目录放置（示例见 [config.example.json](config.example.json)）：

| 字段 | 用途 | 生成方式 |
| --- | --- | --- |
| `api_key` | APP 上报 / 反馈 / 查询接口的鉴权密钥（48 位 hex） | `python3 -c "import secrets; print(secrets.token_hex(24))"` |
| `page_token` | 管理页 URL 直连后备通道的令牌（32 位 hex） | `python3 -c "import secrets; print(secrets.token_hex(16))"` |
| `admin_password` | 管理页登录密码 | `python3 -c "import secrets; print(secrets.token_urlsafe(12))"` 或自定 |

- 首次启动若 `config.json` 不存在，会自动生成 `api_key` / `page_token`（并打印到日志），
  但 **`admin_password` 不再提供默认值**，必须手动写入，否则无法进入管理页。
- **修改 `api_key` 后需要同步 APP**：APP 的密钥在打包时注入（见 APP 仓库根目录
  `local.properties` 的 `stats.apiKey`），改完服务器密钥需更新 `local.properties`
  并重新构建发布 APP，否则新上报会被 401 拒绝。
- `config.json` 权限建议 `600`，且**永不提交到 git**。

## 二、快速部署（Linux + systemd）

```bash
# 1. 上传文件到服务器（示例目录 /sjb）
scp server.py config.example.json sjb.service root@YOUR_SERVER:/sjb/
cd /sjb && mv config.example.json config.json
vi config.json            # 填入生成的 api_key / page_token / admin_password

# 2. 安装 systemd 服务并开机自启
cp sjb.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable --now sjb

# 3. 验证
systemctl status sjb
curl http://127.0.0.1:18081/api/health
```

- 数据库为同目录 SQLite 文件 `sjb.db`，自动创建；备份即拷贝该文件。
- 默认监听 `0.0.0.0:18081`（端口在 `server.py` 顶部 `PORT` 常量，可按需修改）。
- 建议用防火墙限制 18081 端口的来源 IP；管理页本身有密码，但少暴露更安全。

## 三、日常运维

| 操作 | 命令 |
| --- | --- |
| 修改管理密码 | 编辑 `config.json` 的 `admin_password` → `systemctl restart sjb` |
| 查看日志 | `journalctl -u sjb -n 50 --no-pager` |
| 备份数据 | `cp /sjb/sjb.db /sjb/sjb.db.$(date +%F).bak` |
| 更新代码 | 替换 `server.py` → `systemctl restart sjb` |

## 四、APP 侧对接一览

| APP 内配置（本地 local.properties，不入仓库） | 对应服务端 |
| --- | --- |
| `stats.baseUrl=http://YOUR_SERVER:18081` | 服务地址 |
| `stats.apiKey=<与 config.json 的 api_key 一致>` | `X-Api-Key` 请求头 |

配置在构建时通过 `BuildConfig` 注入（`app/build.gradle.kts`），因此**换服务器 / 换密钥都需要重新构建 APP**。

管理页功能：设备列表（品牌 / 系统 / **APP 版本** / 分辨率 / 最近活跃）、
版本分布图、近 7 天活跃图、最近上报记录（含版本）、用户反馈列表、单设备详情页
（设备列表中点击设备 ID 进入）。
