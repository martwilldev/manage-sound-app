# Política de Privacidade / Privacy Policy — Modos de Som (Manage Sound Mode)

* **Pacote Android:** `com.managesoundmode.app`
* **Última atualização:** 06 de novembro de 2026
* **URL Pública Oficial:** https://matheusmaia.gitlab.io/manage-sound-mode-privacy/

---

## 🇧🇷 Português (PT-BR)

### Resumo Transparente (100% On-Device)
O **Modos de Som (Manage Sound Mode)** opera **100% localmente no seu dispositivo**. Não possuímos servidores de coleta, não exigimos conta ou login, não incluímos SDKs de anúncios ou telemetria e jamais compartilhamos seus dados pessoais ou de localização com terceiros.

### 1. Dados Armazenados Localmente
O aplicativo salva exclusivamente no armazenamento interno privado do Android (`Jetpack DataStore`):
* **Perfis de Som e Volumes:** Nomes e níveis dos 4 canais (`Toque`, `Notificações`, `Mídia` e `Alarme`).
* **Rotinas de Horário:** Horários de início/término e dias da semana.
* **Cercas Geográficas (Locais):** Nome do local, latitude/longitude, raio em metros e SSID Wi-Fi opcional.
* **Histórico de Transições:** Registro local das últimas transições de modo de som para diagnóstico do próprio usuário.

### 2. Permissões Solicitadas e Finalidade
* `ACCESS_NOTIFICATION_POLICY` (**Não Perturbe / DND**): Necessária para alternar entre os modos Som Ativo, Vibrar e Silencioso.
* `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` (**Localização / Geofencing**): Opcional, solicitada apenas após o aceite explícito do Aviso de Destaque (*Prominent Disclosure*) na aba *Locais*. Usada exclusivamente no dispositivo para ativar/restaurar perfis de som ao entrar ou sair de locais configurados pelo usuário.
* `SCHEDULE_EXACT_ALARM` & `RECEIVE_BOOT_COMPLETED` (**Alarmes Exatos e Boot**): Garantem a execução pontual das rotinas agendadas e timers regressivos.
* `POST_NOTIFICATIONS` (**Notificações**): Exibe o timer ativo, o gadget rápido opcional e sugestões ao silenciar pelos botões físicos.

### 3. Exclusão Total de Dados
O usuário pode apagar todos os dados a qualquer momento em **Ajustes > Backup & Restaurar Padrões > Restaurar Perfis e Regras de Fábrica**, limpar os logs em **Ajustes > Histórico de Transições**, ou desinstalar o aplicativo.

---

## 🇺🇸 English (EN)

### Transparent Summary (100% On-Device)
**Manage Sound Mode** operates **100% locally on your device**. We do not operate collection servers, require no account or sign-in, embed no advertising or analytics SDKs, and never share your personal or location data with third parties.

### 1. Locally Stored Data
The app stores the following strictly inside Android's private internal storage (`Jetpack DataStore`):
* **Sound Profiles & Volumes:** Profile names and levels for all 4 streams (`Ring`, `Notifications`, `Media`, and `Alarm`).
* **Schedule Routines:** Start/end times and days of the week.
* **Geofences (Locations):** Location name, latitude/longitude, radius in meters, and optional Wi-Fi SSID.
* **Transition History:** Local log of recent sound mode transitions for user diagnostics.

### 2. Permissions & Purpose
* `ACCESS_NOTIFICATION_POLICY` (**Do Not Disturb / DND**): Required to switch between Active Sound, Vibrate, and Silent modes.
* `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` (**Location / Geofencing**): Optional, requested only after accepting the in-app *Prominent Disclosure* in the *Locations* tab. Evaluated strictly on-device to activate/restore sound profiles when entering or leaving user-configured locations.
* `SCHEDULE_EXACT_ALARM` & `RECEIVE_BOOT_COMPLETED` (**Exact Alarms & Boot**): Ensure punctual execution of scheduled routines and countdown timers.
* `POST_NOTIFICATIONS` (**Notifications**): Displays active countdown timers, the optional quick bar gadget, and smart timer prompts when silencing via hardware volume buttons.

### 3. Complete Data Deletion
Users can erase all custom data at any time under **Settings > Backup & Factory Reset > Restore Factory Profiles & Rules**, clear logs under **Settings > Transition History**, or uninstall the application.
