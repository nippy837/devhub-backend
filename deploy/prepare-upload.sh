#!/usr/bin/env bash
set -euo pipefail

cd /home/ubuntu/projects/devhub
release_name="${1:?Missing release name}"
[[ "$release_name" =~ ^backend-[0-9]+-[0-9]+$ ]]

# 与容器更新共用锁，取得完整的现有 JAR 作为 rsync 差量比较基准。
exec 9>.deploy.lock
flock -w 300 9
mkdir -p "releases/$release_name" .backend-transfer-base
container="$(docker compose ps -q backend)"
if [[ -n "$container" ]]; then
  docker cp "$container:/app/app.jar" .backend-transfer-base/app.jar.tmp
  mv .backend-transfer-base/app.jar.tmp .backend-transfer-base/app.jar
  echo "已从当前后端容器准备增量传输基准"
else
  echo "当前没有运行的后端容器，将使用已有基准或进行首次完整上传"
fi
