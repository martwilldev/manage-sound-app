# AGENTS.md — manage-sound-app

> Este repositório faz parte do ecossistema **Manage Sound Mode (Modos de Som)**.  
> Siga sempre as diretrizes de arquitetura, qualidade e segurança abaixo ao evoluir o aplicativo.

---

## 1. Visão Geral do Repositório

* **Nome**: `manage-sound-app` (`com.managesoundmode.app`)
* **Descrição**: Aplicativo Android nativo 100% *on-device* para gerenciamento, perfis e automação contextual de modos de som, horários e locais/Wi-Fi.
* **Repositório Público (Portfólio & Produção)**: `https://github.com/martwilldev/manage-sound-app`
* **Stack Principal**:
  * **Android Gradle Plugin (AGP)**: `9.0.1` (JDK 17 toolchain, `compileSdk = 36`, `targetSdk = 36`, `minSdk = 29`)
  * **Kotlin**: `2.3.20` (`compose`, `serialization`)
  * **Jetpack Compose BOM**: `2026.03.01` (Material 3 Expressive — *Zenith Design System*)
  * **Jetpack Navigation 3**: `1.0.1` (`navigation3-runtime`, `navigation3-ui`, `lifecycle-viewmodel-navigation3:2.10.0`)
  * **Jetpack Glance**: `1.1.1` (`glance-appwidget`, `glance-material3`)
  * **Jetpack DataStore**: `1.1.7` (`datastore-preferences`)
  * **Jetpack WorkManager**: `2.10.1` (`work-runtime-ktx`)

---

## 2. Comandos Essenciais

```bash
# 1. Rodar suíte de testes unitários + Android Lint Release (Quality Gate)
./gradlew testDebugUnitTest lintRelease --no-daemon

# 2. Compilar APK de Debug
./gradlew assembleDebug --no-daemon

# 3. Executar esteira CI/CD local em modo validação (Dry-Run)
./scripts/ci-pipeline.sh
```

---

## 3. Estrutura Interna (Clean Architecture Pragmática)

```text
manage-sound-app/
├── .github/
│   ├── CODEOWNERS                  # Exige revisão de @martwilldev em qualquer PR
│   └── workflows/android-ci-cd.yml # Esteira CI/CD blindada (Quality Gate + Google Play API v3)
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro          # Regras R8 para Serialization, Nav3, Glance e WorkManager
│   └── src/
│       ├── main/kotlin/com/managesoundmode/app/
│       │   ├── domain/
│       │   │   ├── engine/         # RuleEvaluator (Máquina de estados determinística)
│       │   │   └── model/          # SoundMode, SoundProfile, AutomationModels, AccessControlModels
│       │   ├── data/
│       │   │   ├── system/         # SoundSystemController (AudioManager + NotificationManager)
│       │   │   ├── local/          # SoundPreferencesDataSource (DataStore + Backup Sanitizado)
│       │   │   └── repository/     # SoundModeRepository (Single Source of Truth)
│       │   ├── system/             # InternalIntentSecurity, BackgroundGeofenceManager,
│       │   │                       # ExactRuleAlarmScheduler, SmartNotificationManager,
│       │   │                       # SystemZenRuleManager, AppShortcutsManager, LocationProximityHelper
│       │   ├── receiver/           # SystemAutomationReceiver (Autenticado via InternalIntentSecurity)
│       │   ├── tile/               # SoundModeTileService (8 Quick Settings Tiles sincronizados)
│       │   ├── widget/             # SoundModeGlanceWidget (Widgets 2x1, 4x1 e 4x2)
│       │   ├── work/               # RestoreSoundModeWorker (WorkManager auto-restore)
│       │   ├── ui/
│       │   │   ├── common/         # AppLabels (i18n PT-BR/EN) & AppTestTags
│       │   │   ├── components/     # SystemComponents, FirstRunOnboardingDialog, GeofenceMapPickerCard
│       │   │   ├── theme/          # Zenith Theme (Noite Boreal Dark & Alvorada Nórdica Light)
│       │   │   └── main/           # MainScreen, MainScreenViewModel e conteúdos das 5 abas
│       │   ├── MainActivity.kt
│       │   ├── Navigation.kt
│       │   └── NavigationKeys.kt
│       ├── test/                   # Testes unitários (RuleEvaluatorTest, MainScreenViewModelTest)
│       └── androidTest/            # Testes instrumentados de UI e E2E (MainScreenTest, ManageSoundModeE2ETest)
├── docs/PRIVACY_POLICY.md          # Política de Privacidade Bilíngue (100% On-Device)
├── scripts/                        # Scripts de automação CI/CD e guarda Pre-Commit
├── LICENSE                         # Portfolio & Source-Available License
├── SECURITY.md                     # Política de Segurança e Arquitetura de Proteção
└── README.md
```

---

## 4. Regras de Segurança e Qualidade (Obrigatórias)

1. **Zero Vazamento de Segredos**: Nunca comite `.env`, `.jks`, `keystore.properties`, `*service-account*.json`, `.apk` ou `.aab`. Mantenha o hook [`scripts/git-pre-commit-guard.sh`](./scripts/git-pre-commit-guard.sh) ativo.
2. **Proteção contra Intent Spoofing (`InternalIntentSecurity`)**: Toda nova `PendingIntent` destinada ao `SystemAutomationReceiver` ou à `MainActivity` deve ser assinada com `InternalIntentSecurity.signIntent(context, intent)`.
3. **Respeito ao RBAC (`AccessControlConfig`)**: Ações iniciadas pelo usuário (inclusive via Widgets, Notificações ou Shortcuts) devem respeitar as permissões do papel ativo (`AppPermission.CHANGE_SOUND_AND_TIMER`).
4. **Verificação Obrigatória**: Sempre execute `./gradlew testDebugUnitTest lintRelease` antes de qualquer commit.
