# Changelog

Todas as mudanças notáveis deste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/),
e este projeto adere ao [Versionamento Semântico](https://semver.org/lang/pt-BR/).

## [1.0.4] - 2026-02-16

### Alterado

- Projeto renomeado de **OpenSheets Companion** para **OpenMonetis Companion**
- Package Android: `br.com.opensheets.companion` → `br.com.openmonetis.companion`
- Classes renomeadas: `OpenSheetsApi` → `OpenMonetisApi`, `OpenSheetsApp` → `OpenMonetisApp`, `OpenSheetsCompanionTheme` → `OpenMonetisCompanionTheme`
- URLs do repositório atualizados para `openmonetis` / `openmonetis-companion`
- Database: `opensheets_companion.db` → `openmonetis_companion.db`
- SharedPreferences: `opensheets_secure_prefs` → `openmonetis_secure_prefs`
- README reescrito com novo nome e URLs

## [1.0.3] - 2026-02-15

### Corrigido

- Regex de extração do nome do estabelecimento nas notificações

## [1.0.2] - 2026-02-15

### Adicionado

- Logo na barra de título da tela principal
- Documentação completa no README

## [1.0.1] - 2026-02-14

### Corrigido

- Melhorias gerais de estabilidade

## [1.0.0] - 2026-02-14

### Adicionado

- Captura automática de notificações bancárias (Nubank, Itaú, Bradesco, etc.)
- Sincronização automática com OpenMonetis via API
- Setup guiado com QR Code para configuração de servidor e token
- Histórico de notificações com filtros por status
- Gatilhos de captura personalizáveis
- Tema claro/escuro (segue sistema)
- Retry automático via WorkManager
- Armazenamento seguro de token via EncryptedSharedPreferences
