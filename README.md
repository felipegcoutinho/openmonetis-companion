<p align="center">
  <img src="./logo.png" alt="OpenMonetis Companion Logo" height="80" />
</p>

<p align="center">
  App Android para captura automática de notificações bancárias e integração com o OpenMonetis.
</p>

> **Requer o OpenMonetis instalado.** Este app é um complemento que captura notificações e envia para sua instância do [OpenMonetis](https://github.com/felipegcoutinho/openmonetis).

[![Android](https://img.shields.io/badge/Android-12+-3DDC84?style=flat-square&logo=android)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=flat-square&logo=jetpack-compose)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-CC_BY--NC--SA_4.0-orange?style=flat-square&logo=creative-commons)](LICENSE)

---

## Índice

- [Sobre o Projeto](#sobre-o-projeto)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Instalação](#instalação)
- [Configuração](#configuração)
- [Arquitetura](#arquitetura)
- [Desenvolvimento](#desenvolvimento)
- [Contribuindo](#contribuindo)

---

## Sobre o Projeto

**OpenMonetis Companion** é o app Android oficial do ecossistema OpenMonetis. Ele captura automaticamente notificações de transações dos seus apps de banco e fintech, extrai as informações relevantes (valor, descrição) e envia para a **Caixa de Entrada** do OpenMonetis como pré-lançamentos.

### Como funciona

1. O app escuta notificações dos apps de banco configurados
2. Quando detecta uma transação (Pix recebido, compra no cartão, etc.), extrai os dados
3. Envia automaticamente para sua instância do OpenMonetis via API
4. As transações aparecem na "Caixa de Entrada" para você revisar e aprovar

### Por que usar

- **Economia de tempo:** Não precisa digitar cada transação manualmente
- **Precisão:** Valores e descrições são capturados diretamente da notificação
- **Controle:** Você ainda revisa e aprova antes de virar um lançamento oficial
- **Privacidade:** Seus dados ficam no SEU servidor, não em nuvens de terceiros

---

## Features

- Escuta notificações em tempo real e filtra apenas apps de banco configurados
- Extrai valor e descrição automaticamente, detectando tipo de transação (Pix, cartão, transferência)
- Envio automático para o OpenMonetis com retry em caso de falha
- Sincronização em segundo plano via WorkManager
- Autenticação via token de API com EncryptedSharedPreferences
- Histórico de notificações capturadas com filtros por status
- Setup guiado de conexão com servidor
- Gatilhos de captura personalizáveis
- Tema claro/escuro (segue sistema)

---

## Tech Stack

| Componente | Tecnologia |
|------------|------------|
| **Linguagem** | Kotlin |
| **Min SDK** | Android 12 (API 31) |
| **UI** | Jetpack Compose + Material 3 |
| **Arquitetura** | MVVM + Clean Architecture |
| **DI** | Hilt |
| **Database** | Room |
| **Network** | Retrofit + OkHttp |
| **Async** | Coroutines + Flow |
| **Background** | WorkManager |
| **Segurança** | EncryptedSharedPreferences |

---

## Instalação

Baixe a última versão do APK na página de [Releases](https://github.com/felipegcoutinho/openmonetis-companion/releases).

### Requisitos

- Android 12 ou superior
- Instância do OpenMonetis configurada e acessível
- Token de API gerado no OpenMonetis

### Instalação Manual

1. Baixe o arquivo `openmonetis-companion-vX.X.X.apk`
2. No Android, habilite "Instalar apps de fontes desconhecidas" para seu navegador/gerenciador de arquivos
3. Abra o APK e instale
4. Siga o assistente de configuração

---

## Configuração

### 1. Gerar Token no OpenMonetis

1. Acesse sua instância do OpenMonetis
2. Vá em **Ajustes → OpenMonetis Companion**
3. Clique em **Gerar Token**
4. Copie o token gerado (ele só é mostrado uma vez!)

### 2. Configurar o App

1. Abra o OpenMonetis Companion
2. Insira a URL do seu servidor (ex: `https://openmonetis.com`)
3. Cole o token de API
4. Clique em **Conectar**

### 3. Permissões

O app solicitará permissão de **Acesso a Notificações**:

1. Toque em **Conceder Permissão**
2. Encontre "OpenMonetis Companion" na lista
3. Ative a permissão

### 4. Selecionar Apps

Por padrão, os principais apps de banco já vêm configurados. Você pode ajustar em **Configurações → Apps Monitorados**.

---

## Arquitetura

### Estrutura do Projeto

```
app/src/main/java/br/com/openmonetis/companion/
├── OpenMonetisApp.kt              # Application class (Hilt)
├── di/                           # Módulos de Injeção de Dependência
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
│   └── NetworkModule.kt
├── data/
│   ├── local/                    # Room Database
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   └── entities/
│   ├── remote/                   # Retrofit API
│   │   ├── api/
│   │   └── dto/
│   └── repository/               # Repositórios
├── domain/
│   ├── model/                    # Modelos de domínio
│   └── repository/               # Interfaces
├── service/
│   ├── NotificationListenerService.kt  # Captura de notificações
│   └── SyncWorker.kt                   # Sincronização em background
├── ui/
│   ├── theme/                    # Material 3 Theme
│   ├── navigation/               # Navigation Compose
│   └── screens/
│       ├── setup/                # Tela de configuração inicial
│       ├── home/                 # Tela principal
│       ├── settings/             # Configurações
│       ├── history/              # Histórico
│       └── logs/                 # Logs de sincronização
└── util/                         # Utilitários
```

### Comunicação com OpenMonetis

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/health` | Verifica conectividade |
| POST | `/api/inbox` | Envia notificação única |
| POST | `/api/inbox/batch` | Envia múltiplas notificações |

---

## Desenvolvimento

### Pré-requisitos

- Android Studio
- JDK 17
- Android SDK 35

### Setup

1. Clone o repositório
   ```bash
   git clone https://github.com/felipegcoutinho/openmonetis-companion.git
   cd openmonetis-companion
   ```

2. Abra no Android Studio e sincronize o Gradle

3. Execute no emulador ou dispositivo: **Run → Run 'app'**

### Build Release

```bash
./gradlew assembleRelease
```

O APK será gerado em `app/build/outputs/apk/release/`.

---

## Contribuindo

Contribuições são bem-vindas!

1. **Fork** o projeto
2. **Clone** seu fork
   ```bash
   git clone https://github.com/seu-usuario/openmonetis-companion.git
   ```
3. **Crie uma branch** para sua feature
   ```bash
   git checkout -b feature/minha-feature
   ```
4. **Commit** suas mudanças
5. **Push** e abra um **Pull Request**

### Adicionando Suporte a Novo Banco

1. Identificar o `packageName` do app
2. Criar regras de parsing em `NotificationParser`
3. Adicionar à lista de apps suportados
4. Testar com notificações reais

---

## Licença

Este projeto está licenciado sob a **Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International** (CC BY-NC-SA 4.0).

---

## Links

- **OpenMonetis (Web App):** [github.com/felipegcoutinho/openmonetis](https://github.com/felipegcoutinho/openmonetis)
- **Releases:** [github.com/felipegcoutinho/openmonetis-companion/releases](https://github.com/felipegcoutinho/openmonetis-companion/releases)
- **Issues:** [github.com/felipegcoutinho/openmonetis-companion/issues](https://github.com/felipegcoutinho/openmonetis-companion/issues)

---

<div align="center">

**Parte do ecossistema [OpenMonetis](https://github.com/felipegcoutinho/openmonetis)**

</div>
