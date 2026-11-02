# AGENTS.md — manage-sound-app

> Este repositório faz parte do ecossistema **Manage Sound Mode**.  
> Além das regras locais abaixo, siga sempre as diretrizes globais da raiz em [`../../AGENTS.md`](../../AGENTS.md) e o estado global em [`../../STATE.md`](../../STATE.md).

---

## 1. Visão Geral do Repositório

* **Nome**: `manage-sound-app`
* **Descrição**: Aplicativo Android nativo (Kotlin + Jetpack Compose) para gerenciamento, perfis e automação de modos de som.
* **Localização no Workspace**: `repos/manage-sound-app/`
* **Stack Principal**:
  * **Android Gradle Plugin (AGP)**: `9.0.1` (Java 17 toolchain)
  * **Kotlin**: `2.3.20` (`compose`, `serialization`)
  * **Jetpack Compose BOM**: `2026.03.01` (Material 3, Edge-to-Edge, Material Icons Extended)
  * **Jetpack Navigation 3**: `1.0.1` (`navigation3-runtime`, `navigation3-ui`, `lifecycle-viewmodel-navigation3:2.10.0`)
  * **Jetpack Lifecycle**: `2.10.0` (`collectAsStateWithLifecycle`, `LifecycleResumeEffect`, `ViewModel`)
  * **Jetpack DataStore**: `1.1.7` (`datastore-preferences`)
  * **Jetpack WorkManager**: `2.10.1` (`work-runtime-ktx`)
  * **Kotlin Coroutines & Serialization**: `coroutines:1.10.2`, `kotlinx-serialization-json:1.9.0`

---

## 2. Comandos Essenciais

```bash
# Rodar testes unitários (JUnit 4 + Coroutines Test)
./gradlew testDebugUnitTest

# Compilar APK de Debug
./gradlew assembleDebug

# Compilar e instalar no emulador/dispositivo conectado via Android CLI
android run
```

---

## 3. Estrutura Interna (Clean Architecture Pragmática)

```text
manage-sound-app/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/com/managesoundmode/app/
│       │   ├── domain/model/       # SoundMode, VolumeStreams, SoundProfile, TimedModeSession
│       │   ├── data/
│       │   │   ├── system/         # AndroidSoundSystemController (AudioManager + NotificationManager)
│       │   │   ├── local/          # DataStoreSoundPreferencesDataSource (Jetpack DataStore)
│       │   │   └── repository/     # SoundModeRepository & DefaultSoundModeRepository (SSOT)
│       │   ├── work/               # RestoreSoundModeWorker (WorkManager auto-restore)
│       │   ├── di/                 # AppContainer & DefaultAppContainer
│       │   ├── ui/
│       │   │   ├── theme/          # Tokens Dark-First & cores semânticas (DESIGN_SYSTEM.md)
│       │   │   └── main/           # MainScreen & MainScreenViewModel (UDF + StateFlow)
│       │   ├── MainActivity.kt
│       │   ├── Navigation.kt       # Jetpack Navigation 3 (NavDisplay + entryProvider)
│       │   └── NavigationKeys.kt
│       ├── test/                   # Testes unitários rápidos com Fakes
│       └── androidTest/            # Testes instrumentados de UI Compose
├── gradle/libs.versions.toml       # Version Catalog centralizado
├── AGENTS.md
└── README.md
```

---

## 4. Regras Específicas deste Repositório

1. **Unidirectional Data Flow (UDF)**: `ViewModel` nunca deve receber `Context` ou `Activity`. Use `collectAsStateWithLifecycle()` nas telas Compose.
2. **Permissão DND (`NotificationManager`)**: Antes de aplicar `SoundMode.SILENT`, sempre verifique `hasDndPermission()`. Se ausente, exiba o banner educativo que direciona para `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`.
3. **Validação**: Nunca finalize uma tarefa sem rodar `./gradlew testDebugUnitTest assembleDebug`.
