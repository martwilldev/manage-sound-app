#!/usr/bin/env bash
set -euo pipefail

# bump-version.sh — Incrementa automaticamente o VERSION_CODE e a versão SemVer
# Uso:
#   ./scripts/bump-version.sh [patch|minor|major|code-only]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
VERSION_FILE="$REPO_DIR/version.properties"

if [[ ! -f "$VERSION_FILE" ]]; then
  echo "Erro: $VERSION_FILE não encontrado." >&2
  exit 1
fi

BUMP_TYPE="${1:-patch}"

MAJOR=$(grep -E '^VERSION_MAJOR=' "$VERSION_FILE" | cut -d'=' -f2)
MINOR=$(grep -E '^VERSION_MINOR=' "$VERSION_FILE" | cut -d'=' -f2)
PATCH=$(grep -E '^VERSION_PATCH=' "$VERSION_FILE" | cut -d'=' -f2)
CODE=$(grep -E '^VERSION_CODE=' "$VERSION_FILE" | cut -d'=' -f2)

OLD_VERSION="${MAJOR}.${MINOR}.${PATCH} (code ${CODE})"
NEW_CODE=$((CODE + 1))

case "$BUMP_TYPE" in
  major)
    MAJOR=$((MAJOR + 1))
    MINOR=0
    PATCH=0
    ;;
  minor)
    MINOR=$((MINOR + 1))
    PATCH=0
    ;;
  patch)
    PATCH=$((PATCH + 1))
    ;;
  code-only)
    ;;
  *)
    echo "Uso: $0 [patch|minor|major|code-only]" >&2
    exit 1
    ;;
esac

cat > "$VERSION_FILE" <<EOF
# Canonical version configuration for Modos de Som (Manage Sound Mode)
# CI/CD or ./scripts/bump-version.sh updates these values automatically.
VERSION_MAJOR=${MAJOR}
VERSION_MINOR=${MINOR}
VERSION_PATCH=${PATCH}
VERSION_CODE=${NEW_CODE}
EOF

echo "✅ Versão atualizada: ${OLD_VERSION} -> ${MAJOR}.${MINOR}.${PATCH} (code ${NEW_CODE})"
