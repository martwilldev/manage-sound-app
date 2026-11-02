# Manage Sound Mode — Android App (`manage-sound-app`)

Aplicativo Android nativo construído com **Kotlin 2.3**, **AGP 9**, **Jetpack Compose (BOM 2026.03)**, **Material 3**, **Navigation 3**, **DataStore** e **WorkManager**.

## Funcionalidades Implementadas (MVP v0.1)

* **Alternância Tátil Instantânea**: Troca em 1 toque entre `Normal`, `Vibrar` e `Silencioso` com feedback hático e visual (`#10B981` Emerald, `#F59E0B` Amber, `#6366F1` Indigo).
* **Tratamento Inteligente de Permissão DND**: Detecção reativa de `NotificationManager.isNotificationPolicyAccessGranted` com revalidação automática no `onResume` (`LifecycleResumeEffect`) e atalho direto para as configurações do sistema.
* **Controle Independente de Streams de Volume**: Sliders dedicados para Toque (`STREAM_RING`), Notificações (`STREAM_NOTIFICATION`), Mídia (`STREAM_MUSIC`) e Alarme (`STREAM_ALARM`).
* **Perfis de Som (Presets + Customizados)**: Perfis prontos (*Casa*, *Trabalho*, *Reunião*, *Sono*) e criação de novos perfis persistidos em `Preferences DataStore`.
* **Modo Temporizado (Auto-Restore)**: Ativação temporária (15m, 30m, 45m, 60m) com agendamento via `WorkManager` (`RestoreSoundModeWorker`) para restaurar o modo anterior automaticamente.

## Como Executar

```bash
# Executar testes unitários
./gradlew testDebugUnitTest

# Gerar APK de debug
./gradlew assembleDebug
```
