# Refatoração de UX e UI

Implementação da versão 1.6.0 (código 10), em 30/09/2026. A identidade visual e os logos seguem o projeto base OpenMonetis.

## Resultado

- Home com status operacional em laranja suave (claro: `#F7E5CF`; escuro: `#40311F`), progresso do WorkManager, contadores, checklist de permissão/apps e acesso ao histórico. Apps monitorados mostram seus ícones mesmo antes da concessão de permissão, em grade com colunas de largura uniforme, linhas centralizadas e altura consistente para os nomes.
- Card operacional mostra “Conexão configurada”; a última verificação aparece somente quando há uma data salva, separada do horário do último envio.
- Filtros em até duas colunas, usando uma coluna em janelas estreitas ou com fonte ampliada, sem vazamento horizontal. Barra de status clara em laranja com símbolos escuros.
- Ícone do Companion com fundo branco puro, marca laranja e símbolo 20% menor; exports e imagem de loja atualizados.
- Histórico com filtros nomeados e contagens, incluindo erros e descartados, filtragem SQL antes do limite e páginas de 50 registros.
- Modelos, cards, status, feedback e formatação BRL/data compartilhados entre as duas telas. Estado coletado com o ciclo de vida e seleção de filtro preservada.
- Configurações com apps primeiro, seleção múltipla pesquisável, conexão configurada/horário de verificação, alertas, dados e diagnóstico.
- Setup rolável com adaptação ao teclado, ações IME, etapas visíveis, campos bloqueados durante verificação e recuperação da câmera pelos ajustes. A URL é preservada após recriação; o token não é persistido no estado de processo.
- Erro de token abre diretamente a edição da conexão; falhas temporárias permitem reenvio. Cada filtro tem mensagem vazia contextual.
- Exclusão individual confirmada, com efeito local explicado e desfazer. Descarte também oferece desfazer. A exclusão obtém um snapshot em transação para restaurar o estado efetivo, mesmo se o envio terminou após abrir a confirmação.
- Limpeza geral informa contagens e impede exclusão enquanto há envios. Exportação JSON inclui `syncedAt` e oferece compartilhamento com acesso ao arquivo.
- Controles de remoção com 48 dp e descrições específicas para apps e gatilhos; filtros usam o estado selecionado nativo do Compose.

## Preparação local da 1.6.0

Em 30/09/2026, a versão foi atualizada para `1.6.0`, com `versionCode = 10`. O changelog reúne as mudanças dessa versão, e o workflow continua disparando apenas por tags `v*` ou manualmente. Ele agora executa testes unitários de release e lint antes de gerar o APK assinado.

Na revisão final, `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`, `lintDebug`, `testReleaseUnitTest` e `lintRelease` passaram. São 13 testes unitários em cada variante, sem falhas, e zero erros de lint. Permanecem 93 avisos: principalmente versões de dependências, recursos sem uso e sugestões de estilo. O aviso de contexto na classe base dos ViewModels foi revisado: Home e Histórico recebem `@ApplicationContext` via Hilt.

O build release também passou com minificação R8 e redução de recursos. A validação local produziu um APK release sem assinatura, usando um script Gradle temporário fora do repositório; a configuração de assinatura do projeto foi preservada. O APK publicado será assinado pelo workflow com os secrets já configurados.

Nenhum emulador foi iniciado e nenhum teste instrumentado foi executado no aparelho pessoal durante essa preparação. O APK de teste 1.6.0 foi atualizado no aparelho com os dados locais preservados. A Home e o Histórico foram capturados nos temas claro e escuro, sem emulador e sem executar testes instrumentados no aparelho pessoal.

## Dados e sincronização

A migração Room 1→2 acrescenta `synced_at` sem apagar ou recriar tabelas. Registros legados permanecem com data de envio desconhecida, excluídos de “Enviados hoje”. Novos envios registram a hora efetiva e são retidos por 30 dias a partir dela. Processados e descartados sem data de envio continuam usando a captura para retenção.

Pedidos de sincronização são serializados com `APPEND_OR_REPLACE`; capturas durante um envio não cancelam o worker ativo. O worker percorre lotes de 50 com cursor estável e corte temporal, resolve resultados omitidos como falhas, separa erros de autenticação de falhas temporárias e propaga cancelamento. A aquisição de cada registro é condicional e atômica, impedindo envio de registros removidos ou descartados antes da aquisição.

A confirmação de conexão usa um cliente explícito para o servidor informado, sem substituir credenciais antes da verificação. “Configurado” descreve as credenciais armazenadas, sem prometer disponibilidade contínua do servidor.

## Validação

```bash
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

A suíte instrumentada usa um emulador dedicado e exemplos fictícios. Ela substitui dados locais; não deve ser executada sobre dados pessoais de um aparelho em uso.

Cobertura: migração preservando registros antigos; pendência anterior a 150 enviados; limites incrementais; contagem pelo horário do envio; retenção de captura antiga recém-enviada; limpeza bloqueada durante envio; proteção contra exclusão/descarte em envio; restauração sem sobrescrever registros; drenagem de 120 itens; respostas incompletas; HTTP 401 e 503; setup com IME/recriação; histórico, descarte/desfazer, edição do token e seleção de apps.

Evidência da etapa anterior, antes dos últimos ajustes da Home: build debug e de testes, 13 testes unitários e lint passaram, sem erros de lint. Os 10 testes instrumentados passaram no Android 16/API 36 do emulador. Os fluxos iniciais de UI também passaram no cenário compacto com tema escuro e fonte 1,3×; o fluxo de setup passou em paisagem. Após os refinamentos visuais, os três testes de UI passaram nos temas claro e escuro compacto. A cobertura adicional verifica os ícones dos apps na Home, os limites horizontais de todos os filtros e os símbolos da barra de status conforme o tema.

| Cenário | Janela | Fonte | Cobertura |
| --- | --- | --- | --- |
| Claro | 1080×1920 px, 420 dpi | 1,0× | Suíte completa |
| Escuro compacto | 720×1280 px, 320 dpi | 1,3× | Setup, ícones na Home, filtros, histórico, desfazer, edição da conexão e seleção de apps |
| Paisagem | 1280×720 px, 320 dpi | 1,0× | Setup, recriação, IME e falha de servidor |

### Capturas atuais da 1.6.0

[Home clara](screenshots/1.6.0/light/home.png) · [Histórico claro](screenshots/1.6.0/light/history.png) · [Home escura](screenshots/1.6.0/dark/home.png) · [Histórico escuro](screenshots/1.6.0/dark/history.png)

Capturas feitas em aparelho físico Galaxy S23, em 30/09/2026, na versão 1.6.0 (código 10), em 1080×2340 px. Elas mostram o card suave, a grade centralizada, o texto “Conexão configurada”, os filtros dentro da largura disponível e a barra de status laranja no tema claro. O Histórico foi mantido no filtro sem pendências para não expor o conteúdo das notificações pessoais.

As quatro imagens passaram por inspeção visual e verificação do texto da interface e de OCR. Não aparecem URL de servidor ou token. A preferência original de tema automático do aparelho foi restaurada após a captura. Apenas os PNGs aprovados foram adicionados à documentação; os dumps de interface usados na conferência permaneceram fora do repositório.

### Evidências anteriores da refatoração

[Configurações](screenshots/light/settings.png) · [Seleção múltipla](screenshots/light/app-selector.png) · [Setup](screenshots/light/setup.png) · [Histórico compacto com fonte ampliada](screenshots/dark-compact/history.png) · [Setup em paisagem](screenshots/landscape/setup.png)

As capturas anteriores em `screenshots/light`, `screenshots/dark-compact` e `screenshots/landscape` são da UI executada no emulador, com dados fictícios. Elas não representam notificações bancárias reais e registram a etapa anterior aos últimos ajustes da Home.

## Homologação manual pendente

- Auditoria manual com TalkBack em aparelho físico.
- Leitura de QR com câmera real e fluxo de permissão permanentemente negada.
- Integração com servidor de homologação e captura de notificações de bancos reais.
- Revisão do comportamento em fabricantes com restrições de execução em segundo plano.

A versão 1.6.0 usa o código 10. As capturas atuais e a publicação foram autorizadas pelo usuário. O processo de release usa o commit `Release 1.6.0` e a tag anotada `v1.6.0`; o workflow do GitHub executa a validação e gera o APK assinado. Os cenários manuais acima permanecem sem homologação nesta etapa.
