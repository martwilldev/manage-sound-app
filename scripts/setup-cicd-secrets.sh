#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# setup-cicd-secrets.sh — Configura o .env local e os Segredos do CI/CD
# ==============================================================================
# Uso:
#   ./scripts/setup-cicd-secrets.sh /caminho/para/sua-chave-google-play.json
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_DIR"

JSON_ARG="${1:-}"
TARGET_JSON="$REPO_DIR/app/keystore/play-service-account.json"

echo "=========================================================="
echo "🔐 CONFIGURADOR DE SEGREDOS CI/CD — MODOS DE SOM"
echo "=========================================================="

if [[ -n "$JSON_ARG" ]]; then
  if [[ ! -f "$JSON_ARG" ]]; then
    echo "❌ Erro: Arquivo '$JSON_ARG' não encontrado." >&2
    exit 1
  fi
  mkdir -p "$REPO_DIR/app/keystore"
  cp "$JSON_ARG" "$TARGET_JSON"
  echo "✅ Chave JSON copiada para: app/keystore/play-service-account.json"
fi

if [[ -f "$TARGET_JSON" ]]; then
  CLIENT_EMAIL=$(python3 -c "import json; print(json.load(open('$TARGET_JSON')).get('client_email', 'DESCONHECIDO'))")
  echo "• Service Account detectada: $CLIENT_EMAIL"
else
  echo "⚠️  Nenhuma chave JSON encontrada em app/keystore/play-service-account.json ainda."
  echo "   Quando baixar do Google Cloud, rode:"
  echo "   ./scripts/setup-cicd-secrets.sh ~/Downloads/nome-do-arquivo.json"
fi

# Garante que o .env (físico ou FIFO virtual do 1Password) existe
if [[ ! -e "$REPO_DIR/.env" && ! -p "$REPO_DIR/.env" ]]; then
  cp "$REPO_DIR/.env.example" "$REPO_DIR/.env"
fi

KEYSTORE_PATH="$REPO_DIR/app/keystore/manage-sound-release.jks"
if [[ -f "$KEYSTORE_PATH" ]]; then
  KEYSTORE_B64=$(base64 < "$KEYSTORE_PATH" | tr -d '\n')
  echo "✅ Keystore Release encontrada (app/keystore/manage-sound-release.jks)"
else
  echo "❌ Keystore não encontrada em $KEYSTORE_PATH" >&2
  exit 1
fi

# Carrega variáveis do .env local
set -a
# shellcheck disable=SC1091
source "$REPO_DIR/.env"
set +a

# Instala automaticamente o Git Pre-Commit Security Guard contra vazamentos
if [[ -d "$REPO_DIR/.git/hooks" ]] && [[ -f "$REPO_DIR/scripts/git-pre-commit-guard.sh" ]]; then
  cp "$REPO_DIR/scripts/git-pre-commit-guard.sh" "$REPO_DIR/.git/hooks/pre-commit"
  chmod +x "$REPO_DIR/.git/hooks/pre-commit"
  echo "🛡️  Git Pre-Commit Security Guard ativo (bloqueia .env, .jks, .json e senhas)."
fi

# Se houver repositório remoto no GitHub e gh instalado, sincroniza GitHub Encrypted Secrets
if command -v gh >/dev/null 2>&1 && git remote -v | grep -qi "github.com"; then
  echo "----------------------------------------------------------"
  echo "🐙 Repositório GitHub detectado! Sincronizando GitHub Actions Encrypted Secrets..."
  printf "%s" "$KEYSTORE_B64" | gh secret set KEYSTORE_BASE64
  printf "%s" "${RELEASE_STORE_PASSWORD}" | gh secret set RELEASE_STORE_PASSWORD
  printf "%s" "${RELEASE_KEY_ALIAS}" | gh secret set RELEASE_KEY_ALIAS
  printf "%s" "${RELEASE_KEY_PASSWORD}" | gh secret set RELEASE_KEY_PASSWORD
  if [[ -f "$TARGET_JSON" ]]; then
    gh secret set PLAY_STORE_SERVICE_ACCOUNT_JSON < "$TARGET_JSON"
  fi
  echo "✅ Segredos criptografados sincronizados no GitHub Actions!"
# Se houver repositório remoto no GitLab e glab instalado, sincroniza variáveis automaticamente
elif command -v glab >/dev/null 2>&1 && git remote -v | grep -qi "gitlab.com"; then
  echo "----------------------------------------------------------"
  echo "☁️  Repositório GitLab detectado! Sincronizando variáveis CI/CD..."
  glab variable set KEYSTORE_BASE64 "$KEYSTORE_B64" --masked || glab variable update KEYSTORE_BASE64 "$KEYSTORE_B64"
  glab variable set RELEASE_STORE_PASSWORD "${RELEASE_STORE_PASSWORD}" --masked || glab variable update RELEASE_STORE_PASSWORD "${RELEASE_STORE_PASSWORD}"
  glab variable set RELEASE_KEY_ALIAS "${RELEASE_KEY_ALIAS}" || glab variable update RELEASE_KEY_ALIAS "${RELEASE_KEY_ALIAS}"
  glab variable set RELEASE_KEY_PASSWORD "${RELEASE_KEY_PASSWORD}" --masked || glab variable update RELEASE_KEY_PASSWORD "${RELEASE_KEY_PASSWORD}"
  if [[ -f "$TARGET_JSON" ]]; then
    glab variable set PLAY_STORE_SERVICE_ACCOUNT_JSON "$(cat "$TARGET_JSON")" --type file || \
      glab variable update PLAY_STORE_SERVICE_ACCOUNT_JSON "$(cat "$TARGET_JSON")"
  fi
  echo "✅ Variáveis sincronizadas no GitLab CI/CD!"
else
  echo "----------------------------------------------------------"
  echo "📋 Variáveis prontas para GitLab CI / GitHub Actions:"
  echo "   • RELEASE_KEY_ALIAS      = managesoundmode"
  echo "   • RELEASE_STORE_PASSWORD = (já configurado no seu .env)"
  echo "   • RELEASE_KEY_PASSWORD   = (já configurado no seu .env)"
  echo "   • KEYSTORE_BASE64        = (${#KEYSTORE_B64} caracteres gerados automaticamente)"
fi

echo "=========================================================="
echo "🎉 Ambiente .env pronto! Para publicar no Teste Fechado:"
echo "   ./scripts/ci-pipeline.sh --bump patch --deploy alpha"
echo "=========================================================="
