#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

ENV_FILE="deploy/.env"
COMPOSE_FILE="deploy/docker-compose.dev.yml"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "缺少 $ENV_FILE。" >&2
  exit 1
fi

DOCKER_BIN="docker"
if ! command -v docker >/dev/null 2>&1; then
  if [[ -x "/c/Program Files/Docker/Docker/resources/bin/docker.exe" ]]; then
    DOCKER_BIN="/c/Program Files/Docker/Docker/resources/bin/docker.exe"
  else
    echo "未找到 docker 命令。请新开终端，或先启动 Docker Desktop。" >&2
    exit 1
  fi
fi

if ! "$DOCKER_BIN" info >/dev/null 2>&1; then
  echo "Docker 引擎未运行。请先启动 Docker Desktop。" >&2
  exit 1
fi

"$DOCKER_BIN" compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" down
