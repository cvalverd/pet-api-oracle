#!/usr/bin/env bash
set -Eeuo pipefail

image="${1:?image URI is required}"
region="${2:?AWS region is required}"
secret_arn="${3:?Oracle credentials secret ARN is required}"
wallet_bucket="${4:?Oracle wallet bucket is required}"
wallet_key="${5:?Oracle wallet object key is required}"

if [[ ! "$image" =~ ^[0-9]{12}\.dkr\.ecr\.[a-z0-9-]+\.amazonaws\.com/pet-api-oracle:[0-9a-f]{40}$ ]]; then
  echo "Invalid API ECR image URI." >&2
  exit 2
fi
if [[ ! "$region" =~ ^[a-z0-9-]+$ \
  || ! "$secret_arn" =~ ^arn:aws:secretsmanager:${region}:[0-9]{12}:secret:[A-Za-z0-9/_+=.@-]+$ \
  || ! "$wallet_bucket" =~ ^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$ \
  || ! "$wallet_key" =~ ^[A-Za-z0-9._/-]{1,1024}$ ]]; then
  echo "Invalid Oracle resource identifier." >&2
  exit 2
fi

registry="${image%%/*}"
docker_config="$(mktemp -d /tmp/pet-ecr-docker.XXXXXX)"
env_file="$(mktemp /tmp/pet-api-env.XXXXXX)"
export DOCKER_CONFIG="$docker_config"
cleanup() {
  if [[ -n "$docker_config" ]]; then
    rm -rf -- "$docker_config"
    docker_config=""
  fi
  if [[ -n "$env_file" ]]; then
    rm -f -- "$env_file"
    env_file=""
  fi
}
trap cleanup EXIT

aws ecr get-login-password --region "$region" \
  | docker login --username AWS --password-stdin "$registry" >/dev/null
docker pull "$image"
docker logout "$registry" >/dev/null 2>&1 || true
rm -f -- "$docker_config/config.json"

aws secretsmanager get-secret-value \
  --secret-id "$secret_arn" \
  --query SecretString \
  --output text \
  --region "$region" \
  | python3 -c '
import json
import sys

secret = json.load(sys.stdin)
with open(sys.argv[1], "w", encoding="utf-8") as output:
    for key in ("DB_URL", "DB_USERNAME", "DB_PASSWORD"):
        value = secret.get(key)
        if not isinstance(value, str) or "\n" in value or "\r" in value:
            raise SystemExit(f"Secret is missing a valid {key} value.")
        output.write(f"{key}={value}\n")
' "$env_file"
printf 'AWS_REGION=%s\nORACLE_WALLET_S3_BUCKET=%s\nORACLE_WALLET_S3_KEY=%s\nTNS_ADMIN=/app/wallet\n' \
  "$region" "$wallet_bucket" "$wallet_key" >> "$env_file"
chmod 600 "$env_file"

backup=""
new_container_attempted=false
rollback() {
  status=$?
  trap - EXIT
  if (( status != 0 )); then
    if [[ "$new_container_attempted" == true ]] && docker inspect pet-api >/dev/null 2>&1; then
      docker rm -f pet-api >/dev/null
    fi
    if [[ -n "$backup" ]] && docker inspect "$backup" >/dev/null 2>&1; then
      docker rename "$backup" pet-api
      docker start pet-api
      echo "API deployment failed; previous container restored." >&2
    fi
  fi
  cleanup
  exit "$status"
}
trap rollback EXIT

if docker inspect pet-api >/dev/null 2>&1; then
  backup="pet-api-rollback-$(date +%s%N)"
  docker rename pet-api "$backup"
  docker stop "$backup" >/dev/null || true
fi

new_container_attempted=true
docker run -d \
  --name pet-api \
  --restart unless-stopped \
  --publish 127.0.0.1:8080:8080 \
  --env-file "$env_file" \
  "$image" >/dev/null
rm -f -- "$env_file"
env_file=""

healthy=false
for attempt in $(seq 1 60); do
  if curl --fail --silent --show-error --max-time 10 http://127.0.0.1:8080/pets \
    | python3 -c 'import json,sys; pets=json.load(sys.stdin); assert isinstance(pets,list) and len(pets) >= 4'; then
    healthy=true
    break
  fi
  sleep 5
done
if [[ "$healthy" != true ]]; then
  echo "API health check failed or Oracle returned fewer than four demo records." >&2
  exit 1
fi

if [[ -n "$backup" ]]; then
  docker rm "$backup" >/dev/null
fi
backup=""
cleanup
trap - EXIT
echo "API deployed successfully: $image"
