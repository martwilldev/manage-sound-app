#!/usr/bin/env bash
set -euo pipefail

# Git Pre-Commit Security Guard — Bloqueia vazamento de chaves, .env, binários ou senhas em repositório público

STAGED_FILES=$(git diff --cached --name-only --diff-filter=ACM)
if [[ -z "$STAGED_FILES" ]]; then
  exit 0
fi

# 1. Bloqueia extensões e nomes de arquivos proibidos
if echo "$STAGED_FILES" | grep -E -i '(^|/)\.env($|\.)|\.jks$|\.keystore$|keystore\.properties$|service-account.*\.json$|\.aab$|\.apk$|\.idsig$' | grep -v '\.example$' >/dev/null 2>&1; then
  echo "❌ [SECURITY BLOCK] Tentativa de commit de arquivo sensível ou binário detectada!" >&2
  echo "$STAGED_FILES" | grep -E -i '(^|/)\.env($|\.)|\.jks$|\.keystore$|keystore\.properties$|service-account.*\.json$|\.aab$|\.apk$|\.idsig$' | grep -v '\.example$' >&2
  exit 1
fi

# 2. Bloqueia chaves privadas, tokens ou credenciais reais no diff (ignorando o próprio script de guarda)
DIFF_CONTENT=$(git diff --cached -- . ':(exclude)scripts/git-pre-commit-guard.sh')

if echo "$DIFF_CONTENT" | grep -E '^\+.*(BEGIN PRIVATE KEY|BEGIN RSA PRIVATE KEY|"private_key_id"|"private_key"|AIza[0-9A-Za-z_-]{35}|ghp_[0-9A-Za-z]{36}|glpat-[0-9A-Za-z_-]{20})' >/dev/null 2>&1; then
  echo "❌ [SECURITY BLOCK] Chave privada ou token de API detectado no conteúdo do commit!" >&2
  exit 1
fi

# 3. Se o arquivo .env local existir, garante que as senhas reais configuradas nele jamais apareçam no diff
REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
if [[ -f "$REPO_ROOT/.env" ]]; then
  STORE_PASS=$(grep -E '^RELEASE_STORE_PASSWORD=' "$REPO_ROOT/.env" | cut -d'=' -f2- | tr -d '"' | tr -d "'" || true)
  KEY_PASS=$(grep -E '^RELEASE_KEY_PASSWORD=' "$REPO_ROOT/.env" | cut -d'=' -f2- | tr -d '"' | tr -d "'" || true)

  for SECRET_VAL in "$STORE_PASS" "$KEY_PASS"; do
    if [[ -n "$SECRET_VAL" ]] && [[ "$SECRET_VAL" != "change_me_in_production" ]]; then
      if echo "$DIFF_CONTENT" | grep -E '^\+' | grep -F "$SECRET_VAL" >/dev/null 2>&1; then
        echo "❌ [SECURITY BLOCK] Senha real do arquivo .env detectada no conteúdo do commit!" >&2
        exit 1
      fi
    fi
  done
fi

exit 0
