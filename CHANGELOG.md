# Changelog

Todas as mudanças notáveis deste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/),
e este projeto adere ao [Versionamento Semântico](https://semver.org/lang/pt-BR/).

## [Não lançado]

## [1.6.1] - 2026-09-30

### Corrigido

- Envio declara `timestampFormatVersion: 2` para que a API interprete o horário UTC sem aplicar a correção legada de três horas.
- Rejeições por horário inválido e conflito de identificador mostram mensagens específicas; conteúdo arbitrário retornado pelo servidor continua fora dos logs e da interface.

## [1.6.0] - 2026-09-30

### Adicionado

- Leitor de QR Code no setup para importar tokens gerados pelo OpenMonetis.
- Testes instrumentados de migração, histórico, concorrência de ações, lotes e fluxos de UI.
- Validação automatizada da política de URLs e do protocolo versionado de pareamento.

### Segurança

- Servidores de produção exigem HTTPS; HTTP fica restrito a origens locais em builds de debug.
- Verificação de servidor e token ocorre antes de substituir credenciais armazenadas.
- Tokens deixam de aparecer na edição do servidor e passam a ser mascarados nos campos de entrada.
- Logs HTTP não registram headers ou corpos e ficam desativados em builds de produção.
- Logs locais deixam de armazenar conteúdo integral de notificações e mensagens de exceção.
- A listagem de aplicativos deixa de solicitar acesso a todos os pacotes instalados.

### Alterado

- Home reorganizada com status de captura, andamento real do WorkManager, contadores e checklist de primeiro uso.
- Apps monitorados exibem ícones e nomes em grade centralizada e adaptativa na Home; o card operacional usa um tom suave de laranja em ambos os temas.
- Card operacional mostra “Conexão configurada” e exibe a última verificação somente quando há uma data registrada.
- Filtros de Home e histórico se adaptam em uma ou duas colunas conforme largura e tamanho de fonte, sem ultrapassar a tela.
- Barra de status no tema claro usa laranja primário e símbolos escuros.
- Ícone do Companion diferenciado do PWA: fundo branco puro, marca laranja e símbolo 20% menor.
- Histórico acessível pela Home, com filtros nomeados, contagens e carregamento incremental após filtragem no banco.
- Configurações priorizam apps monitorados, permitem seleção múltipla e incluem gatilhos e diagnóstico.
- Setup com etapas explícitas, rolagem, adaptação ao teclado, ações IME e recuperação da permissão de câmera.
- Cards, filtros, detalhes e formatação de valores/datas compartilhados entre Home e histórico.
- Exclusão individual e descarte oferecem desfazer; limpeza geral informa total e pendências e protege envios em andamento.
- Exportação JSON oferece compartilhamento e inclui o horário real de envio.
- Room migra de 1 para 2 preservando dados; “Enviados hoje” passa a usar a data efetiva do envio.
- Sincronização serializada drena lotes, trata respostas incompletas e propaga cancelamento; novos pedidos não interrompem envios ativos.

- Identidade visual alinhada ao OpenMonetis: temas claro e escuro, cores semânticas e tipografia GT America.
- Logos, splash e ícones do Companion usam os vetores oficiais do projeto principal, com suporte a ícones temáticos do Android.
- Textos e ações sobre fundos claros usam a cor de marca com maior contraste; cores de sucesso e aviso acompanham o tema.
- README revisado com logo para temas claro e escuro, instruções de pareamento e permissões atuais, estrutura real do código e links para os reviews.
- Exports auxiliares de ícones Android e imagem de loja alinhados à marca oficial.
- Notificações sincronizadas, processadas ou descartadas passam a ser removidas após 30 dias.

### Removido

- Contrato e armazenamento de refresh token que não fazem parte da API do OpenMonetis.
- Campo `userId` legado da resposta de verificação do dispositivo.

## [1.5.2] - 2026-05-30

### Adicionado

- Links para os repositórios do Companion, do OpenMonetis e para o perfil do autor na seção Sobre
- Testes de regressão para extração de estabelecimentos em notificações do Cartão Mercado Pago

### Alterado

- Cards de notificações destacam a descrição normalizada e mantêm o texto original nos detalhes
- Card de permissão de captura aparece na tela inicial somente quando a permissão está desabilitada
- Resumo de apps monitorados na tela inicial abre os ajustes e destaca quando nenhum app está configurado
- Permissão para exibir alertas do Companion é solicitada somente ao ativar um alerta
- Seção Sobre consolidada para reduzir o espaço ocupado na tela de ajustes

### Corrigido

- Extração do estabelecimento em notificações achatadas do Cartão Mercado Pago, ignorando o texto informativo da próxima fatura
- Card de permissão da tela inicial agora abre corretamente as configurações nativas de captura de notificações

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
