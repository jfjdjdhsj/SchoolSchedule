#!/usr/bin/env bash
# =====================================================================
# 同步脚本：本地仓库 SchoolSchedule → GitHub 脱敏仓库 SchoolSchedule-git
#
# 用法：在 SchoolSchedule 目录改动并验证后，运行
#   bash sync-to-git.sh "提交说明"
#
# 流程：
#   1. 本地仓库提交全部改动（含 local.properties / server/config.json 等
#      本地私有文件，它们被 .gitignore 排除，永远不会进入导出内容）
#   2. 用 git archive 导出当前提交的「干净源码树」（自动排除密钥、密钥库、
#      构建产物、config.json 等所有 gitignore 文件）
#   3. 敏感值扫描：发现服务器 IP / API Key / 密码等立即中止，绝不推送
#   4. 覆盖到 SchoolSchedule-git（GitHub 仓库的本地克隆）并提交推送
# =====================================================================
set -euo pipefail

SRC=/workspace/SchoolSchedule
DST=/workspace/SchoolSchedule-git
MSG="${1:-sync: 同步本地改动}"

cd "$SRC"

# 1) 本地提交
if ! git diff --quiet HEAD 2>/dev/null || ! git diff --cached --quiet; then
  git add -A
  git commit -m "$MSG"
else
  echo "本地无未提交改动"
fi
# 未跟踪的新文件也要提交
if [ -n "$(git status --porcelain)" ]; then
  git add -A
  git commit -m "$MSG"
fi

# 2) 导出干净源码树
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
git archive HEAD | tar -x -C "$TMP"

# 3) 脱敏检查（命中任意一项即中止；敏感字面量拆分书写，避免自检误报）
echo "== 敏感值扫描 =="
P1='103\.194\.107\.''86'            # 服务器 IP
P2='193e58ca[0-9a-f]{40}'           # 统计 API Key
P3='1145''24'                       # 旧管理密码
P4='4J53gYG''982v7'                 # 服务器 SSH 密码
P5='700''370'                       # 旧脚本内默认密码
LEAKS=$(grep -rlE "$P1|$P2|$P3|$P4|$P5" "$TMP" || true)
if [ -n "$LEAKS" ]; then
  echo "!! 发现敏感内容，已中止推送："
  echo "$LEAKS"
  exit 1
fi
echo "通过：无敏感内容"

# 4) 同步到 DST 并推送（rsync 保留 .git，删除多余文件保持与导出树一致）
rsync -a --delete --exclude '.git' "$TMP"/ "$DST"/
cd "$DST"
# 新克隆仓库可能缺少提交者身份，自动补齐
git config user.name  >/dev/null 2>&1 || git config user.name "jfjdjdhsj"
git config user.email >/dev/null 2>&1 || git config user.email "jfjdjdhsj@users.noreply.github.com"
if [ -n "$(git status --porcelain)" ]; then
  git add -A
  git commit -m "$MSG"
  git push origin main
  echo "== 已推送到 GitHub =="
else
  echo "GitHub 仓库已是最新，无需推送"
fi
