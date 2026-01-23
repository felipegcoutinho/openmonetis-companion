# OpenSheets Companion

App Android para captura de notificações financeiras e integração com o OpenSheets.

## Objetivo

Capturar notificações de apps de bancos e fintechs (Nubank, Itaú, Inter, etc.) e transformá-las automaticamente em pré-lançamentos na "Caixa de Entrada" do OpenSheets para processamento manual pelo usuário.

## Status

**Em desenvolvimento** - Fase 1 (MVP)

## Arquitetura

Documentação completa da arquitetura disponível em:
- [`opensheets-app/docs/ARQUITETURA_APP_ANDROID_CAPTURA_NOTIFICACOES.md`](../opensheets-app/docs/ARQUITETURA_APP_ANDROID_CAPTURA_NOTIFICACOES.md)

## Tech Stack

| Componente | Tecnologia |
|------------|------------|
| Linguagem | Kotlin |
| Arquitetura | MVVM + Clean Architecture |
| DI | Hilt |
| UI | Jetpack Compose + Material 3 |
| DB Local | Room |
| Network | Retrofit + OkHttp |
| Async | Coroutines + Flow |
| Background | WorkManager |
| Segurança | EncryptedSharedPreferences |

## Estrutura do Projeto

```
app/src/main/java/br/com/opensheets/companion/
├── OpenSheetsApp.kt                 # Application class
├── di/                              # Injeção de Dependência (Hilt)
├── data/
│   ├── local/                       # Room database
│   ├── remote/                      # Retrofit API
│   └── repository/
├── domain/
│   ├── model/
│   ├── usecase/
│   └── parser/                      # Parsing de notificações
├── service/
│   ├── NotificationListenerService.kt
│   └── SyncWorker.kt
├── ui/
│   ├── theme/
│   ├── navigation/
│   └── screens/
└── util/
```

## Requisitos

- Android 12 (API 31) ou superior
- Permissão de Notification Listener
- Conexão com instância do OpenSheets

## Desenvolvimento

```bash
# Abrir no Android Studio
# File > Open > selecionar este diretório
```

## Projetos Relacionados

- [OpenSheets App](https://github.com/opensheets/opensheets-app) - Backend e frontend web

---

*Desenvolvido como parte do ecossistema OpenSheets*
