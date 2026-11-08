# Política de Segurança / Security Policy — Modos de Som (`manage-sound-app`)

## Versões Suportadas / Supported Versions

| Versão | Pacote | Target SDK | Suporte de Segurança |
| :--- | :--- | :--- | :--- |
| `0.3.x` | `com.managesoundmode.app` | Android 15/16 (API 36) | ✅ Ativo (Produção / Google Play Beta) |
| `< 0.3.0` | `com.managesoundmode.app` | API 35 | ❌ Descontinuado |

---

## Arquitetura de Segurança e Privacidade (100% On-Device)

O **Modos de Som (Manage Sound Mode)** foi projetado sob os princípios de **Privacy by Design** e **Zero-Trust Local Execution**:

1. **Execução 100% Local:** O aplicativo não possui servidores de backend, não realiza chamadas de telemetria/analytics e não exige login ou criação de conta.
2. **Isolamento de Dados (`Jetpack DataStore`):** Perfis de som, agendas de horário, cercas geográficas (latitude/longitude/raio) e SSIDs de Wi-Fi são armazenados estritamente no diretório privado interno do aplicativo (`/data/data/com.managesoundmode.app/`), protegido pelo sandbox de UID do Linux/Android.
3. **Controle de Acesso Interno (RBAC + PIN Lock):** A aba de configurações e regras críticas suportam bloqueio por PIN (verificado em tempo constante via `MessageDigest.isEqual` com suporte a hash `SHA-256` e omitido automaticamente em exportações de backup JSON) e perfis de permissão internos (`ADMIN`, `OPERATOR`, `VIEWER`, `CUSTOM`) aplicados inclusive em Widgets, Notificações e App Shortcuts.
4. **Proteção Criptográfica contra Intent Spoofing (`InternalIntentSecurity`):** Como o código-fonte do repositório é público, todas as `PendingIntent`s internas (`AlarmManager`, `LocationManager`, `NotificationManager` e `ShortcutManager`) são assinadas com um token aleatório de **256 bits (`SecureRandom`)** exclusivo por instalação (`internal_security_prefs.xml`, excluído de backup em nuvem). O `SystemAutomationReceiver` e a `MainActivity` rejeitam qualquer broadcast ou intent externa não autenticada.
5. **WebView Blindado com Subresource Integrity (`GeofenceMapPickerCard`):** O seletor de mapa OpenStreetMap opera com `allowFileAccess = false`, `allowContentAccess = false`, `MIXED_CONTENT_NEVER_ALLOW`, bloqueio de navegação externa (`shouldOverrideUrlLoading = true`) e verificação criptográfica **SRI SHA-256** para os scripts Leaflet `1.9.4`.
6. **Esteira CI/CD Blindada:**
   * Nenhum segredo, keystore (`.jks`) ou credencial do Google Cloud (`.json`) é versionado no Git.
   * O hook [`scripts/git-pre-commit-guard.sh`](./scripts/git-pre-commit-guard.sh) bloqueia localmente qualquer tentativa de commit de arquivos sensíveis, binários ou credenciais.
   * No GitHub Actions, a assinatura ocorre em memória efêmera com destruição imediata (`trap EXIT`) e permissões mínimas (`permissions: contents: read`).

---

## Como Reportar uma Vulnerabilidade / Reporting a Vulnerability

Se você identificar qualquer vulnerabilidade de segurança neste repositório:

1. **Não abra uma Issue pública** contendo detalhes de exploração.
2. Utilize a aba **[Security > Report a vulnerability](https://github.com/martwilldev/manage-sound-app/security/advisories/new)** (*GitHub Private Vulnerability Reporting*) para enviar o relato de forma privada diretamente ao mantenedor (`@martwilldev`).
3. Você receberá uma confirmação inicial em até **48 horas** com o plano de triagem e correção.
