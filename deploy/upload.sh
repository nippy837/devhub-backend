#!/usr/bin/env bash
set -euo pipefail

: "${SSH_HOST:?Missing SSH_HOST}"
: "${SSH_USER:?Missing SSH_USER}"
: "${DEPLOY_RELEASE:?Missing DEPLOY_RELEASE}"
[[ "$DEPLOY_RELEASE" =~ ^backend-[0-9]+-[0-9]+$ ]]

# JAR 内未变化的依赖可以直接复用；不要再套一层 gzip 或使用随机文件名。
# 每次上传到独立发布目录，不覆盖运行中的版本。
for attempt in 1 2 3; do
  echo "增量上传，第 $attempt/3 次尝试"
  if timeout --signal=TERM --kill-after=15s 300s \
    rsync -rt --checksum --no-whole-file --partial-dir=.rsync-partial \
      --timeout=120 --info=progress2 --stats \
      --copy-dest=/home/ubuntu/projects/devhub/.backend-transfer-base \
      -e "ssh -o BatchMode=yes -o StrictHostKeyChecking=yes -o ConnectTimeout=20 -o ServerAliveInterval=30 -o ServerAliveCountMax=3" \
      deploy-package/ \
      "$SSH_USER@$SSH_HOST:/home/ubuntu/projects/devhub/releases/$DEPLOY_RELEASE/"; then
    exit 0
  fi
  if [[ "$attempt" -lt 3 ]]; then sleep 5; fi
done

echo "部署文件上传失败，保留分片供重试；线上容器尚未更新。" >&2
exit 1
