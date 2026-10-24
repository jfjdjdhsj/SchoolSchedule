# -*- coding: utf-8 -*-
"""
校园作息表 · 设备统计后端
纯 Python 标准库实现（http.server + sqlite3），零第三方依赖。
数据文件：/sjb/sjb.db（SQLite）
配置文件：/sjb/config.json（api_key / admin_password 管理页密码 / page_token URL 直连锁）
端口：18080
"""

import json
import os
import re
import secrets
import sqlite3
import sys
import threading
import time
from datetime import datetime, timedelta, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(BASE_DIR, "sjb.db")
CONFIG_PATH = os.path.join(BASE_DIR, "config.json")
PORT = 18081

CST = timezone(timedelta(hours=8))  # 北京时间

# ---------------- 配置 ----------------

# 安全提示：不要在代码中硬编码任何密码 / 密钥。
# 管理密码请写入 config.json 的 admin_password 字段（首次启动若缺少该字段将无法进入管理页）。
# 生成强随机密码：python3 -c "import secrets; print(secrets.token_urlsafe(12))"
ADMIN_PASSWORD_DEFAULT = ""  # 留空：必须通过 config.json 配置，见同目录 README.md 部署说明
COOKIE_NAME = "sjb_auth"           # 管理页登录态 Cookie

def load_config():
    cfg = None
    if os.path.exists(CONFIG_PATH):
        with open(CONFIG_PATH, "r", encoding="utf-8") as f:
            cfg = json.load(f)
    changed = False
    if cfg is None:
        cfg = {"api_key": secrets.token_hex(24), "page_token": secrets.token_hex(16)}
        changed = True
    if "admin_password" not in cfg:
        cfg["admin_password"] = ADMIN_PASSWORD_DEFAULT
        changed = True
    if changed:
        with open(CONFIG_PATH, "w", encoding="utf-8") as f:
            json.dump(cfg, f, ensure_ascii=False, indent=2)
    return cfg

CONFIG = load_config()

# ---------------- 数据库 ----------------

_db_lock = threading.Lock()

def get_db():
    conn = sqlite3.connect(DB_PATH, timeout=10)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    return conn

def init_db():
    with _db_lock:
        conn = get_db()
        conn.executescript("""
        CREATE TABLE IF NOT EXISTS devices (
            device_id TEXT PRIMARY KEY,
            model TEXT DEFAULT '',
            brand TEXT DEFAULT '',
            manufacturer TEXT DEFAULT '',
            os_version TEXT DEFAULT '',
            sdk_int INTEGER DEFAULT 0,
            app_version TEXT DEFAULT '',
            app_version_code INTEGER DEFAULT 0,
            screen_resolution TEXT DEFAULT '',
            locale TEXT DEFAULT '',
            timezone TEXT DEFAULT '',
            first_seen TEXT NOT NULL,
            last_seen TEXT NOT NULL,
            report_count INTEGER DEFAULT 0
        );
        CREATE TABLE IF NOT EXISTS records (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT NOT NULL,
            reported_at TEXT NOT NULL,
            server_at TEXT NOT NULL,
            app_version TEXT DEFAULT '',
            timetable TEXT DEFAULT '',
            round TEXT DEFAULT '',
            network_type TEXT DEFAULT '',
            battery_level INTEGER DEFAULT -1,
            ip TEXT DEFAULT '',
            event TEXT DEFAULT ''
        );
        CREATE INDEX IF NOT EXISTS idx_records_device ON records(device_id);
        CREATE INDEX IF NOT EXISTS idx_records_time ON records(server_at);
        CREATE TABLE IF NOT EXISTS feedbacks (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT DEFAULT '',
            app_version TEXT DEFAULT '',
            category TEXT DEFAULT '',
            content TEXT NOT NULL,
            contact TEXT DEFAULT '',
            created_at TEXT NOT NULL
        );
        CREATE INDEX IF NOT EXISTS idx_feedbacks_time ON feedbacks(created_at);
        """)
        conn.commit()
        conn.close()

# ---------------- 工具 ----------------

def cst_now():
    return datetime.now(CST)

def cst_str(dt):
    return dt.strftime("%Y-%m-%d %H:%M:%S")

def clean(s, maxlen=128):
    if s is None:
        return ""
    return str(s).strip()[:maxlen]

def to_int(v, default=-1):
    try:
        return int(v)
    except (TypeError, ValueError):
        return default

def client_ip(handler):
    fwd = handler.headers.get("X-Forwarded-For")
    if fwd:
        return fwd.split(",")[0].strip()
    return handler.client_address[0] if handler.client_address else ""

# ---------------- 业务 ----------------

def handle_report(data, ip):
    device_id = clean(data.get("device_id"), 64)
    if not device_id:
        raise ValueError("device_id 缺失")

    now = cst_now()
    now_str = cst_str(now)
    reported_at = clean(data.get("reported_at"), 32) or now_str
    event = clean(data.get("event"), 16) or "report"

    fields = {
        "model": clean(data.get("model")),
        "brand": clean(data.get("brand")),
        "manufacturer": clean(data.get("manufacturer")),
        "os_version": clean(data.get("os_version")),
        "sdk_int": to_int(data.get("sdk_int"), 0),
        "app_version": clean(data.get("app_version")),
        "app_version_code": to_int(data.get("app_version_code"), 0),
        "screen_resolution": clean(data.get("screen_resolution")),
        "locale": clean(data.get("locale")),
        "timezone": clean(data.get("timezone")),
    }

    conn = get_db()
    try:
        with _db_lock:
            row = conn.execute(
                "SELECT device_id FROM devices WHERE device_id=?", (device_id,)
            ).fetchone()
            if row is None:
                conn.execute(
                    """INSERT INTO devices
                    (device_id, model, brand, manufacturer, os_version, sdk_int,
                     app_version, app_version_code, screen_resolution, locale, timezone,
                     first_seen, last_seen, report_count)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,1)""",
                    (device_id, fields["model"], fields["brand"], fields["manufacturer"],
                     fields["os_version"], fields["sdk_int"], fields["app_version"],
                     fields["app_version_code"], fields["screen_resolution"],
                     fields["locale"], fields["timezone"], now_str, now_str),
                )
            else:
                sets = ", ".join(f"{k}=?" for k in fields)
                vals = list(fields.values()) + [now_str, device_id]
                conn.execute(
                    f"UPDATE devices SET {sets}, last_seen=?, report_count=report_count+1 WHERE device_id=?",
                    vals,
                )
            conn.execute(
                """INSERT INTO records
                (device_id, reported_at, server_at, app_version, timetable, round,
                 network_type, battery_level, ip, event)
                VALUES (?,?,?,?,?,?,?,?,?,?)""",
                (device_id, reported_at, now_str, fields["app_version"],
                 clean(data.get("timetable")), clean(data.get("round")),
                 clean(data.get("network_type")), to_int(data.get("battery_level"), -1),
                 ip, event),
            )
            conn.commit()
    finally:
        conn.close()
    return {"ok": True, "server_time": now_str}

def handle_feedback(data):
    """保存用户反馈（仅文字）：content 必填，category/contact 可选"""
    content = clean(data.get("content"), 1000)
    if not content:
        return False
    conn = get_db()
    try:
        conn.execute(
            "INSERT INTO feedbacks (device_id, app_version, category, content, contact, created_at) "
            "VALUES (?, ?, ?, ?, ?, ?)",
            (
                clean(data.get("device_id"), 64),
                clean(data.get("app_version"), 32),
                clean(data.get("category"), 16),
                content,
                clean(data.get("contact"), 100),
                cst_str(cst_now()),
            ),
        )
        conn.commit()
    finally:
        conn.close()
    return True


def query_feedbacks(limit=50):
    """最近用户反馈"""
    conn = get_db()
    try:
        rows = conn.execute(
            "SELECT * FROM feedbacks ORDER BY id DESC LIMIT ?", (limit,)
        ).fetchall()
        return [dict(r) for r in rows]
    finally:
        conn.close()


def query_devices():
    conn = get_db()
    try:
        rows = conn.execute(
            "SELECT * FROM devices ORDER BY last_seen DESC"
        ).fetchall()
        return [dict(r) for r in rows]
    finally:
        conn.close()

def query_records(device_id=None, limit=100, offset=0):
    conn = get_db()
    try:
        if device_id:
            rows = conn.execute(
                "SELECT * FROM records WHERE device_id=? ORDER BY id DESC LIMIT ? OFFSET ?",
                (device_id, limit, offset),
            ).fetchall()
        else:
            rows = conn.execute(
                "SELECT * FROM records ORDER BY id DESC LIMIT ? OFFSET ?",
                (limit, offset),
            ).fetchall()
        return [dict(r) for r in rows]
    finally:
        conn.close()

def query_stats():
    conn = get_db()
    try:
        total_devices = conn.execute("SELECT COUNT(*) AS c FROM devices").fetchone()["c"]
        total_records = conn.execute("SELECT COUNT(*) AS c FROM records").fetchone()["c"]
        today = cst_now().strftime("%Y-%m-%d")
        today_records = conn.execute(
            "SELECT COUNT(*) AS c FROM records WHERE server_at LIKE ?", (today + "%",)
        ).fetchone()["c"]
        today_new = conn.execute(
            "SELECT COUNT(*) AS c FROM devices WHERE first_seen LIKE ?", (today + "%",)
        ).fetchone()["c"]
        active_day = conn.execute(
            "SELECT COUNT(DISTINCT device_id) AS c FROM records WHERE server_at LIKE ?",
            (today + "%",),
        ).fetchone()["c"]
        return {
            "total_devices": total_devices,
            "total_records": total_records,
            "today_records": today_records,
            "today_new_devices": today_new,
            "today_active_devices": active_day,
            "server_time": cst_str(cst_now()),
        }
    finally:
        conn.close()

def query_version_stats():
    """各版本的设备数分布（devices 表当前版本）"""
    conn = get_db()
    try:
        rows = conn.execute(
            "SELECT app_version AS version, COUNT(*) AS c FROM devices "
            "GROUP BY app_version ORDER BY c DESC"
        ).fetchall()
        return [dict(r) for r in rows]
    finally:
        conn.close()


def query_daily_active(days=7):
    """近 N 天每日活跃设备数与上报次数（有数据的天）"""
    conn = get_db()
    try:
        rows = conn.execute(
            "SELECT substr(server_at,1,10) AS day, COUNT(DISTINCT device_id) AS d, "
            "COUNT(*) AS c FROM records GROUP BY day ORDER BY day DESC LIMIT ?",
            (days,),
        ).fetchall()
        return [dict(r) for r in reversed(rows)]
    finally:
        conn.close()


# ---------------- 管理页面 ----------------

EVENT_LABELS = {
    "startup": "启动",
    "enter": "进入前台",
    "exit": "退到后台",
    "report": "上报",
    "heartbeat": "心跳",
}

def esc(s):
    return (str(s or "").replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace('"', "&quot;"))

def render_login_page(error=""):
    err_html = f'<div class="err">{esc(error)}</div>' if error else ""
    return f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>校园作息表 · 设备统计后台</title>
<style>
* {{ box-sizing:border-box; margin:0; padding:0; }}
body {{ font-family:-apple-system,"PingFang SC","Microsoft YaHei",sans-serif;
  background:#f6f7f9; color:#1f2329; min-height:100vh;
  display:flex; align-items:center; justify-content:center; }}
.card {{ background:#fff; border:1px solid #e5e7eb; border-radius:16px;
  padding:32px 28px; width:min(340px, 88vw); }}
h1 {{ font-size:18px; text-align:center; margin-bottom:6px; }}
.sub {{ color:#6b7280; font-size:12px; text-align:center; margin-bottom:22px; }}
input {{ width:100%; height:44px; border:1px solid #e5e7eb; border-radius:10px;
  padding:0 14px; font-size:15px; outline:none; margin-bottom:14px; }}
input:focus {{ border-color:#2563eb; }}
button {{ width:100%; height:44px; border:none; border-radius:10px;
  background:#2563eb; color:#fff; font-size:15px; cursor:pointer; }}
button:active {{ background:#1d4ed8; }}
.err {{ background:#fef2f2; color:#b91c1c; font-size:13px;
  border-radius:8px; padding:8px 12px; margin-bottom:14px; text-align:center; }}
</style>
</head>
<body>
<div class="card">
<h1>校园作息表 · 设备统计后台</h1>
<div class="sub">请输入管理密码后进入</div>
<form method="post" action="/login">
{err_html}
<input type="password" name="password" placeholder="管理密码" autofocus autocomplete="current-password">
<button type="submit">进入</button>
</form>
</div>
</body>
</html>"""

def parse_cookies(handler):
    cookies = {}
    raw = handler.headers.get("Cookie")
    if raw:
        for pair in raw.split(";"):
            if "=" in pair:
                k, v = pair.strip().split("=", 1)
                cookies[k] = v
    return cookies

def render_device_page(dev, records):
    """单设备详情页：设备信息 + 该设备全部上报记录"""
    dev_rows = "".join(
        f"""<tr><td>{k}</td><td>{esc(v)}</td></tr>"""
        for k, v in [
            ("设备 ID", dev["device_id"]),
            ("品牌 / 型号", f"{dev['brand']} {dev['model']}"),
            ("厂商", dev["manufacturer"]),
            ("系统", f"Android {dev['os_version']} (SDK {dev['sdk_int']})"),
            ("应用版本", f"v{dev['app_version']} ({dev['app_version_code']})"),
            ("分辨率", dev["screen_resolution"]),
            ("首次上报", dev["first_seen"]),
            ("最近活跃", dev["last_seen"]),
            ("上报次数", dev["report_count"]),
        ]
    )
    rec_rows = "".join(
        f"""<tr>
<td class="mono">{esc(r['server_at'])}</td>
<td><span class="ev ev-{esc(r['event'])}">{esc(EVENT_LABELS.get(r['event'], r['event']))}</span></td>
<td>{esc(r['timetable'])} / {esc(r['round'])}</td>
<td>v{esc(r['app_version'])}</td>
<td>{esc(r['network_type'])}</td>
<td>{esc(r['battery_level'])}%</td>
<td class="mono">{esc(r['ip'])}</td>
</tr>"""
        for r in records
    )
    return f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>设备详情 · 校园作息表统计后台</title>
<style>
:root {{ --bg:#f6f7f9; --card:#fff; --line:#e5e7eb; --txt:#1f2329; --sub:#6b7280; --pri:#2563eb; }}
* {{ box-sizing:border-box; margin:0; padding:0; }}
body {{ font-family:-apple-system,"PingFang SC","Microsoft YaHei",sans-serif; background:var(--bg); color:var(--txt); padding:20px; }}
h1 {{ font-size:18px; margin-bottom:14px; }}
.back {{ display:inline-block; margin-bottom:14px; color:var(--pri); text-decoration:none; font-size:13px; }}
.panel {{ background:var(--card); border:1px solid var(--line); border-radius:12px; padding:16px; margin-bottom:18px; overflow-x:auto; }}
.panel h2 {{ font-size:15px; margin-bottom:10px; }}
table {{ width:100%; border-collapse:collapse; font-size:13px; white-space:nowrap; }}
th, td {{ text-align:left; padding:8px 10px; border-bottom:1px solid var(--line); }}
th {{ color:var(--sub); font-weight:600; background:#fafafa; }}
.info td:first-child {{ width:110px; color:var(--sub); }}
.mono {{ font-family:ui-monospace,SFMono-Regular,Consolas,monospace; font-size:12px; }}
.ev {{ padding:2px 8px; border-radius:99px; font-size:12px; }}
.ev-startup {{ background:#dcfce7; color:#15803d; }}
.ev-enter {{ background:#dbeafe; color:#1d4ed8; }}
.ev-exit {{ background:#fee2e2; color:#b91c1c; }}
.ev-report, .ev-heartbeat {{ background:#f3f4f6; color:#4b5563; }}
</style>
</head>
<body>
<a class="back" href="/">← 返回设备列表</a>
<h1>设备详情</h1>
<div class="panel"><h2>设备信息</h2><table class="info"><tbody>{dev_rows}</tbody></table></div>
<div class="panel">
<h2>上报记录（{len(records)}，最新在前）</h2>
<table>
<thead><tr><th>时间</th><th>事件</th><th>时间表/轮次</th><th>版本</th><th>网络</th><th>电量</th><th>IP</th></tr></thead>
<tbody>{rec_rows or '<tr><td colspan="7" style="color:#6b7280;text-align:center;padding:24px 0;">暂无记录</td></tr>'}</tbody>
</table>
</div>
</body>
</html>"""


def render_page(stats, devices, records, version_stats, daily_active, feedbacks):
    dev_rows = "".join(
        f"""<tr>
<td class="mono"><a class="dlink" href="/device?did={esc(d['device_id'])}">{esc(d['device_id'][:16])}</a></td>
<td>{esc(d['brand'])} {esc(d['model'])}</td>
<td>{esc(d['manufacturer'])}</td>
<td>Android {esc(d['os_version'])} (SDK {esc(d['sdk_int'])})</td>
<td>v{esc(d['app_version'])}</td>
<td>{esc(d['screen_resolution'])}</td>
<td class="mono">{esc(d['last_seen'])}</td>
<td>{esc(d['report_count'])}</td>
</tr>"""
        for d in devices
    )
    rec_rows = "".join(
        f"""<tr>
<td class="mono">{esc(r['server_at'])}</td>
<td class="mono">{esc(r['device_id'][:16])}</td>
<td><span class="ev ev-{esc(r['event'])}">{esc(EVENT_LABELS.get(r['event'], r['event']))}</span></td>
<td>{esc(r['timetable'])} / {esc(r['round'])}</td>
<td>v{esc(r['app_version'])}</td>
<td>{esc(r['network_type'])}</td>
<td>{esc(r['battery_level'])}%</td>
<td class="mono">{esc(r['ip'])}</td>
</tr>"""
        for r in records
    )
    max_ver = max((r["c"] for r in version_stats), default=1) or 1
    version_bars = "".join(
        f"""<div class="bar-row"><div class="bar-label">v{esc(r['version'] or '?')}</div>
<div class="bar-track"><div class="bar-fill" style="width:{max(2, round(r['c'] * 100 / max_ver))}%"></div></div>
<div class="bar-num">{r['c']} 台</div></div>"""
        for r in version_stats
    )
    max_day = max((d["d"] for d in daily_active), default=1) or 1
    daily_bars = "".join(
        f"""<div class="bar-row"><div class="bar-label">{esc(d['day'][5:])}</div>
<div class="bar-track"><div class="bar-fill" style="width:{max(2, round(d['d'] * 100 / max_day))}%"></div></div>
<div class="bar-num">{d['d']} 设备 / {d['c']} 次</div></div>"""
        for d in daily_active
    )
    FEEDBACK_LABELS = {"bug": "问题反馈", "suggestion": "功能建议", "other": "其他"}
    fb_rows = "".join(
        f"""<tr>
<td class="mono">{esc(r['created_at'])}</td>
<td class="mono">{esc(r['device_id'][:16] or '匿名')}</td>
<td>v{esc(r['app_version'])}</td>
<td>{esc(FEEDBACK_LABELS.get(r['category'], r['category'] or '其他'))}</td>
<td class="wrap">{esc(r['content'])}</td>
<td>{esc(r['contact'] or '—')}</td>
</tr>"""
        for r in feedbacks
    )
    return f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>校园作息表 · 设备统计后台</title>
<style>
:root {{ --bg:#f6f7f9; --card:#fff; --line:#e5e7eb; --txt:#1f2329; --sub:#6b7280; --pri:#2563eb; }}
* {{ box-sizing:border-box; margin:0; padding:0; }}
body {{ font-family:-apple-system,"PingFang SC","Microsoft YaHei",sans-serif; background:var(--bg); color:var(--txt); padding:20px; }}
h1 {{ font-size:20px; margin-bottom:4px; }}
.sub {{ color:var(--sub); font-size:13px; margin-bottom:18px; }}
.cards {{ display:grid; grid-template-columns:repeat(auto-fit,minmax(150px,1fr)); gap:12px; margin-bottom:18px; }}
.card {{ background:var(--card); border:1px solid var(--line); border-radius:12px; padding:14px 16px; }}
.card .num {{ font-size:26px; font-weight:700; color:var(--pri); }}
.card .lbl {{ font-size:12px; color:var(--sub); margin-top:2px; }}
.panel {{ background:var(--card); border:1px solid var(--line); border-radius:12px; padding:16px; margin-bottom:18px; overflow-x:auto; }}
.panel h2 {{ font-size:15px; margin-bottom:10px; }}
table {{ width:100%; border-collapse:collapse; font-size:13px; white-space:nowrap; }}
th, td {{ text-align:left; padding:8px 10px; border-bottom:1px solid var(--line); }}
th {{ color:var(--sub); font-weight:600; background:#fafafa; position:sticky; top:0; }}
tr:hover td {{ background:#f8fafc; }}
.mono {{ font-family:ui-monospace,SFMono-Regular,Consolas,monospace; font-size:12px; }}
.ev {{ padding:2px 8px; border-radius:99px; font-size:12px; }}
.ev-startup {{ background:#dcfce7; color:#15803d; }}
.ev-enter {{ background:#dbeafe; color:#1d4ed8; }}
.ev-exit {{ background:#fee2e2; color:#b91c1c; }}
.ev-report, .ev-heartbeat {{ background:#f3f4f6; color:#4b5563; }}
.empty {{ color:var(--sub); text-align:center; padding:24px 0; }}
.bar-row {{ display:flex; align-items:center; gap:10px; margin:7px 0; }}
.bar-label {{ width:56px; font-size:12px; color:var(--sub); flex:none; }}
.bar-track {{ flex:1; background:#eef1f5; border-radius:6px; height:14px; overflow:hidden; }}
.bar-fill {{ height:100%; background:linear-gradient(90deg,#2563eb,#60a5fa); border-radius:6px; min-width:4px; }}
.bar-num {{ width:86px; text-align:right; font-size:12px; color:var(--txt); flex:none; }}
a.dlink {{ color:var(--pri); text-decoration:none; }}
td.wrap {{ white-space:normal; min-width:220px; max-width:480px; }}
</style>
</head>
<body>
<h1>校园作息表 · 设备统计后台</h1>
<div class="sub">服务器时间：{esc(stats['server_time'])}（北京时间） · 数据每分钟随上报刷新 · <a href="/logout" style="color:#2563eb;">退出登录</a></div>
<div class="cards">
<div class="card"><div class="num">{stats['total_devices']}</div><div class="lbl">累计设备</div></div>
<div class="card"><div class="num">{stats['today_active_devices']}</div><div class="lbl">今日活跃设备</div></div>
<div class="card"><div class="num">{stats['today_new_devices']}</div><div class="lbl">今日新增设备</div></div>
<div class="card"><div class="num">{stats['total_records']}</div><div class="lbl">累计上报次数</div></div>
<div class="card"><div class="num">{stats['today_records']}</div><div class="lbl">今日上报次数</div></div>
</div>
<div class="panel">
<h2>版本分布（按设备）</h2>
{version_bars or '<div class="empty">暂无数据</div>'}
</div>
<div class="panel">
<h2>近 7 天活跃</h2>
{daily_bars or '<div class="empty">暂无数据</div>'}
</div>
<div class="panel">
<h2>用户反馈（{len(feedbacks)}）</h2>
<table>
<thead><tr><th>时间</th><th>设备</th><th>版本</th><th>类型</th><th>内容</th><th>联系方式</th></tr></thead>
<tbody>{fb_rows or '<tr><td colspan="6" class="empty">暂无反馈</td></tr>'}</tbody>
</table>
</div>
<div class="panel">
<h2>设备列表（{len(devices)}）</h2>
<table>
<thead><tr><th>设备ID</th><th>品牌/型号</th><th>厂商</th><th>系统</th><th>应用版本</th><th>分辨率</th><th>最近活跃</th><th>上报次数</th></tr></thead>
<tbody>{dev_rows or '<tr><td colspan="8" class="empty">暂无数据</td></tr>'}</tbody>
</table>
</div>
<div class="panel">
<h2>最近上报记录（{len(records)}）</h2>
<table>
<thead><tr><th>时间</th><th>设备ID</th><th>事件</th><th>时间表/轮次</th><th>版本</th><th>网络</th><th>电量</th><th>IP</th></tr></thead>
<tbody>{rec_rows or '<tr><td colspan="8" class="empty">暂无数据</td></tr>'}</tbody>
</table>
</div>
</body>
</html>"""

# ---------------- HTTP 服务 ----------------

class Handler(BaseHTTPRequestHandler):
    server_version = "SJB-Backend/1.0"
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt, *args):
        sys.stderr.write("[%s] %s\n" % (cst_str(cst_now()), fmt % args))

    # —— 基础响应工具 ——
    def _send_json(self, obj, status=200):
        body = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def _send_html(self, html, status=200):
        body = html.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _send_text(self, text, status=200):
        body = text.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _read_body(self):
        length = to_int(self.headers.get("Content-Length"), 0)
        if length <= 0 or length > 1024 * 1024:
            return {}
        raw = self.rfile.read(length)
        try:
            return json.loads(raw.decode("utf-8"))
        except (ValueError, UnicodeDecodeError):
            return {}

    def _authorized(self):
        key = self.headers.get("X-Api-Key") or ""
        return key == CONFIG["api_key"]

    # —— 路由 ——
    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, X-Api-Key")
        self.send_header("Content-Length", "0")
        self.end_headers()

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path.rstrip("/") or "/"
        qs = parse_qs(parsed.query)

        if path == "/" or path == "/admin":
            # 密码登录鉴权：Cookie 携带正确密码进入；URL token 直连作为后备通道
            cookies = parse_cookies(self)
            token = qs.get("token", [""])[0]
            authed = (
                cookies.get(COOKIE_NAME) == CONFIG.get("admin_password")
                or token == CONFIG["page_token"]
            )
            if not authed:
                self._send_html(render_login_page())
                return
            self._send_html(render_page(query_stats(), query_devices(), query_records(limit=200),
                                        query_version_stats(), query_daily_active(),
                                        query_feedbacks()))
            return

        if path == "/device":
            # 设备详情页（与管理页同鉴权）
            cookies = parse_cookies(self)
            token = qs.get("token", [""])[0]
            authed = (
                cookies.get(COOKIE_NAME) == CONFIG.get("admin_password")
                or token == CONFIG["page_token"]
            )
            if not authed:
                self._send_html(render_login_page())
                return
            did = qs.get("did", [""])[0]
            dev = next((d for d in query_devices() if d["device_id"] == did), None)
            if not dev:
                self._send_html(
                    "<h3>未找到该设备</h3><a href='/' style='color:#2563eb'>返回</a>",
                    status=404,
                )
                return
            self._send_html(render_device_page(dev, query_records(device_id=did, limit=200)))
            return

        if path == "/logout":
            self.send_response(302)
            self.send_header(
                "Set-Cookie", f"{COOKIE_NAME}=; Path=/; Max-Age=0; HttpOnly"
            )
            self.send_header("Location", "/")
            self.send_header("Content-Length", "0")
            self.end_headers()
            return

        if path == "/api/health":
            self._send_json({"ok": True, "time": cst_str(cst_now())})
            return

        if not self._authorized():
            self._send_json({"ok": False, "error": "未授权"}, status=401)
            return

        if path == "/api/stats":
            data = query_stats()
            data["version_dist"] = query_version_stats()   # 新增字段（旧调用方不受影响）
            data["daily_active"] = query_daily_active()    # 新增字段（旧调用方不受影响）
            self._send_json({"ok": True, "data": data})
        elif path == "/api/devices":
            self._send_json({"ok": True, "data": query_devices()})
        elif path == "/api/feedbacks":
            limit = max(1, min(to_int(qs.get("limit", ["50"])[0], 50), 200))
            self._send_json({"ok": True, "data": query_feedbacks(limit)})
        elif path == "/api/records":
            device_id = qs.get("device_id", [None])[0]
            limit = max(1, min(to_int(qs.get("limit", ["100"])[0], 100), 500))
            offset = max(0, to_int(qs.get("offset", ["0"])[0], 0))
            self._send_json({"ok": True, "data": query_records(device_id, limit, offset)})
        else:
            self._send_json({"ok": False, "error": "接口不存在"}, status=404)

    def do_POST(self):
        parsed = urlparse(self.path)
        path = parsed.path.rstrip("/")

        if path == "/login":
            # 管理页密码登录：表单 application/x-www-form-urlencoded
            length = to_int(self.headers.get("Content-Length"), 0)
            raw = self.rfile.read(length).decode("utf-8") if length > 0 else ""
            params = parse_qs(raw)
            password = params.get("password", [""])[0]
            if password == CONFIG.get("admin_password"):
                self.send_response(302)
                self.send_header(
                    "Set-Cookie",
                    f"{COOKIE_NAME}={password}; Path=/; HttpOnly; SameSite=Lax; Max-Age=2592000",
                )
                self.send_header("Location", "/")
                self.send_header("Content-Length", "0")
                self.end_headers()
            else:
                self._send_html(render_login_page("密码错误，请重新输入"), status=403)
            return

        if path == "/api/report":
            if not self._authorized():
                self._send_json({"ok": False, "error": "未授权"}, status=401)
                return
            try:
                data = self._read_body()
                self._send_json(handle_report(data, client_ip(self)))
            except ValueError as e:
                self._send_json({"ok": False, "error": str(e)}, status=400)
            except Exception as e:
                self._send_json({"ok": False, "error": "服务器内部错误: %s" % e}, status=500)
            return

        if path == "/api/feedback":
            # 用户文字反馈（与上报同一 X-Api-Key 鉴权）
            if not self._authorized():
                self._send_json({"ok": False, "error": "未授权"}, status=401)
                return
            try:
                data = self._read_body()
                if handle_feedback(data):
                    self._send_json({"ok": True})
                else:
                    self._send_json({"ok": False, "error": "反馈内容不能为空"}, status=400)
            except ValueError as e:
                self._send_json({"ok": False, "error": str(e)}, status=400)
            except Exception as e:
                self._send_json({"ok": False, "error": "服务器内部错误: %s" % e}, status=500)
            return

        self._send_json({"ok": False, "error": "接口不存在"}, status=404)

def main():
    init_db()
    srv = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print("校园作息表统计后端已启动，端口 %d，数据文件 %s" % (PORT, DB_PATH), flush=True)
    print("管理页 = http://<服务器IP>:%d/ （密码登录，默认密码见 config.json 的 admin_password）" % PORT, flush=True)
    print("URL 令牌直连 = http://<服务器IP>:%d/?token=%s" % (PORT, CONFIG["page_token"]), flush=True)
    srv.serve_forever()

if __name__ == "__main__":
    main()
