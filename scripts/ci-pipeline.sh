#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# ci-pipeline.sh — Esteira Completa PROD (Manage Sound Mode / Modos de Som)
# ==============================================================================
# Uso:
#   ./scripts/ci-pipeline.sh                     # Roda Quality + Build + Dry-Run
#   ./scripts/ci-pipeline.sh --bump patch        # Incrementa versão + Roda Pipe
#   ./scripts/ci-pipeline.sh --deploy alpha      # Roda tudo + Publica no Teste Fechado
#   ./scripts/ci-pipeline.sh --deploy production # Roda tudo + Publica em Produção
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
WORKSPACE_STORE_DIR="$(cd "$REPO_DIR/../.." 2>/dev/null && pwd)/docs/store"

cd "$REPO_DIR"

# Carrega automaticamente o arquivo .env (físico ou FIFO virtual do 1Password) se existir
if [[ -e "$REPO_DIR/.env" || -p "$REPO_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$REPO_DIR/.env"
  set +a
fi

BUMP_MODE=""
DEPLOY_TRACK=""
SKIP_TESTS="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --bump)
      BUMP_MODE="${2:-patch}"
      shift 2
      ;;
    --deploy)
      DEPLOY_TRACK="${2:-${PLAY_STORE_TRACK:-alpha}}"
      shift 2
      ;;
    --skip-tests)
      SKIP_TESTS="true"
      shift 1
      ;;
    *)
      echo "Opção desconhecida: $1" >&2
      exit 1
      ;;
  esac
done

echo "=========================================================="
echo "🛡️  MODOS DE SOM — ESTEIRA CI/CD DE PRODUÇÃO"
echo "=========================================================="

# 0. Bump opcional de versão
if [[ -n "$BUMP_MODE" ]]; then
  echo "▶ [0/4] Incrementando versão ($BUMP_MODE)..."
  ./scripts/bump-version.sh "$BUMP_MODE"
fi

VERSION_MAJOR=$(grep -E '^VERSION_MAJOR=' version.properties | cut -d'=' -f2)
VERSION_MINOR=$(grep -E '^VERSION_MINOR=' version.properties | cut -d'=' -f2)
VERSION_PATCH=$(grep -E '^VERSION_PATCH=' version.properties | cut -d'=' -f2)
VERSION_CODE=$(grep -E '^VERSION_CODE=' version.properties | cut -d'=' -f2)
VERSION_NAME="${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}"

echo "• Versão Alvo: v${VERSION_NAME} (versionCode = ${VERSION_CODE})"

# Decodifica KEYSTORE_BASE64 se estiver rodando em servidor CI sem keystore física
CLEANUP_KEYSTORE="false"
if [[ -n "${KEYSTORE_BASE64:-}" ]] && [[ ! -f "app/keystore/manage-sound-release.jks" ]]; then
  echo "• Decodificando KEYSTORE_BASE64 temporária para assinatura..."
  mkdir -p app/keystore
  echo "$KEYSTORE_BASE64" | base64 --decode > app/keystore/manage-sound-release.jks
  CLEANUP_KEYSTORE="true"
fi

cleanup() {
  if [[ "$CLEANUP_KEYSTORE" == "true" ]]; then
    rm -f "$REPO_DIR/app/keystore/manage-sound-release.jks"
  fi
}
trap cleanup EXIT

# 1. Estágio 1 — Quality Gate (Testes Unitários + Android Lint)
if [[ "$SKIP_TESTS" == "false" ]]; then
  echo "----------------------------------------------------------"
  echo "▶ [1/4] ESTÁGIO 1: Quality Gate (Unit Tests + Android Lint)"
  echo "----------------------------------------------------------"
  ./gradlew testDebugUnitTest lintRelease --no-daemon
  echo "✅ Estágio 1 aprovado (Testes Unitários + Lint sem erros)."
else
  echo "⏭️  [1/4] Pulando testes (--skip-tests)."
fi

# 2. Estágio 2 — Build & Sign Release (AAB + APK + Símbolos Nativos + R8 Mapping)
echo "----------------------------------------------------------"
echo "▶ [2/4] ESTÁGIO 2: Empacotamento e Assinatura Release (API 36)"
echo "----------------------------------------------------------"
./gradlew bundleRelease assembleRelease --no-daemon
echo "✅ Estágio 2 aprovado (AAB e APK Release gerados e assinados)."

# 3. Estágio 3 — Arquivamento de Artefatos (dist/ e docs/store/)
echo "----------------------------------------------------------"
echo "▶ [3/4] ESTÁGIO 3: Arquivamento de Artefatos & Mapping R8"
echo "----------------------------------------------------------"
mkdir -p "$REPO_DIR/dist"
AAB_SRC="app/build/outputs/bundle/release/app-release.aab"
APK_SRC="app/build/outputs/apk/release/app-release.apk"
MAP_SRC="app/build/outputs/mapping/release/mapping.txt"

cp "$AAB_SRC" "$REPO_DIR/dist/ManageSoundMode-v${VERSION_NAME}-b${VERSION_CODE}.aab"
cp "$APK_SRC" "$REPO_DIR/dist/ManageSoundMode-v${VERSION_NAME}-b${VERSION_CODE}.apk"
if [[ -f "$MAP_SRC" ]]; then
  cp "$MAP_SRC" "$REPO_DIR/dist/mapping-v${VERSION_NAME}-b${VERSION_CODE}.txt"
fi

if [[ -d "$WORKSPACE_STORE_DIR" ]]; then
  cp "$AAB_SRC" "$WORKSPACE_STORE_DIR/ManageSoundMode-v0.1.0.aab"
  cp "$APK_SRC" "$WORKSPACE_STORE_DIR/ManageSoundMode-v0.1.0.apk"
  echo "• Artefatos sincronizados em: $WORKSPACE_STORE_DIR/"
fi
echo "• Artefatos versionados em  : $REPO_DIR/dist/"

# 4. Estágio 4 — Publicação na Google Play Store
echo "----------------------------------------------------------"
echo "▶ [4/4] ESTÁGIO 4: Google Play Store Deployment"
echo "----------------------------------------------------------"
if [[ -n "$DEPLOY_TRACK" ]]; then
  python3 ./scripts/publish-play-store.py --track "$DEPLOY_TRACK"
else
  python3 ./scripts/publish-play-store.py --track "${PLAY_STORE_TRACK:-alpha}" --dry-run
fi

echo "=========================================================="
echo "🎉 ESTEIRA CONCLUÍDA COM SUCESSO! (v${VERSION_NAME} / code ${VERSION_CODE})"
echo "=========================================================="
