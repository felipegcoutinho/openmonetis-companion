# Análise de UX e UI — OpenMonetis Companion

Data: 29/09/2026.

Atualização: a identidade visual foi posteriormente alinhada ao projeto base. Os contrastes da seção 1 descrevem o estado anterior; veja [a implementação da marca](BRANDING.md) para a paleta atual. As recomendações de fluxo e comportamento foram implementadas nesta revisão; a validação e eventuais limites estão registrados na tabela e no relatório da implementação.

## Escopo e conclusão

Revisão estática das telas Compose, navegação, ViewModels e consultas usadas pela interface, considerando as mudanças locais ainda não commitadas. A revisão original gerou as recomendações abaixo; a tabela de status registra o que foi implementado desde então. A revisão original foi estática. A implementação posterior acrescenta testes instrumentados e validação no emulador; a auditoria manual com TalkBack e um banco real permanece necessária antes de uma publicação. Os contrastes foram calculados a partir das cores sRGB declaradas no tema, sem considerar alterações de renderização ou overlays.

O app tem uma base funcional adequada: configuração em duas etapas, opção de QR Code e token manual, detalhes com texto original, alertas de permissão contextualizados e estados de envio acompanhados de texto e ícone. A maior oportunidade é tornar evidente se a captura está funcionando, o que precisa de atenção e o que aconteceu após uma ação.

## Prioridades

| Prioridade | Frente | Status atual | Trabalho restante |
| --- | --- | --- | --- |
| Alta | Identidade e contraste | Implementado | Auditoria manual final de acessibilidade |
| Alta | Filtros e acessibilidade | Implementado | TalkBack em aparelho real |
| Alta | Sincronização | Implementado | Homologação com servidor real |
| Alta | Histórico e contadores | Implementado | Migração Room 1→2 preserva registros e não inventa datas de envio antigas |
| Alta | Setup adaptável | Implementado | Validar leitura de QR com câmera física |
| Média | Primeiro uso operacional | Implementado | Checklist na Home |
| Média | Exclusão e descarte | Implementado | Desfazer individual; limpeza geral permanece irreversível com confirmação |
| Média | Organização da Home | Implementado | Sincronização, ajustes e histórico acessíveis |
| Média | Configurações | Implementado | Apps primeiro, seleção múltipla e conexão com horário de verificação |
| Média | Mensagens e erros | Implementado | Vazios por filtro, edição direta para token inválido e exportação compartilhável |
| Média | Documentação e assets | Atualizado | Consulte [resultado e validação](UI_IMPLEMENTATION.md) |

## Refatoração implementada

A identidade já foi implementada. A camada de apresentação e o estado que ela exibe foram refatorados. Algumas correções exigem também consultas Room e integração com WorkManager, porque os problemas percebidos na UI vêm dos dados e do andamento da sincronização.

### Etapa 1 — Estado confiável

- Observar capturas, contagens e apps monitorados, usando coleta de estado ligada ao ciclo de vida.
- Acompanhar o WorkManager para representar espera por rede, envio e resultado.
- Aplicar filtros na consulta antes do limite; acrescentar paginação.
- Corrigir o indicador diário. Usar um timestamp real de envio exige uma migração Room que preserve os dados já instalados; também é possível renomear o indicador para refletir a consulta atual.
- Diferenciar configuração salva, servidor verificado e captura habilitada.

### Etapa 2 — Telas e componentes

- Home: status operacional, pendências que precisam de ação e sincronização explícita.
- Histórico: acesso claro, filtros com nomes e contagens, estados vazios e opções de recuperação.
- Setup: etapas visíveis, URL/token, adaptação ao teclado e checklist de captura/apps.
- Configurações: captura/apps, conexão, alertas, dados e sobre; logs em diagnóstico.
- Componentes compartilhados: card de captura, badge de status, mensagens vazias, eventos de feedback e formatação BRL/data.
- Consolidar a duplicação de modelos, filtros e cards entre Home e Histórico. A Home agora oferece acesso ao histórico e preserva o filtro selecionado.
- Adotar feedback persistente e desfazer; ajustar TalkBack, descrições específicas e espaçamento dos controles.

### Etapa 3 — Validação e documentação do resultado

- Validar os fluxos reais em aparelho/emulador com tema escuro, tela compacta, paisagem, teclado, fontes ampliadas e permissões negadas.
- Exercitar pendências antigas, retorno dos ajustes, erros de token e indisponibilidade do servidor.
- Registrar screenshots reais de Home, configuração e ajustes e atualizar as instruções para a UI final.

Critério de conclusão: o usuário consegue configurar captura e apps, distinguir o que foi enviado do que está pendente, entender uma falha e recuperá-la; as listas refletem o estado real e os controles continuam acessíveis em janelas pequenas e com texto ampliado.

## Evidências da revisão original

As seções abaixo preservam os problemas e recomendações encontrados antes da refatoração. Referências de linha descrevem a versão anterior e não a estrutura atual.

## 1. Cores, contraste e hierarquia visual

Evidência: `app/src/main/java/br/com/openmonetis/companion/ui/theme/Theme.kt`, `HomeScreen.kt` (filtros, nome do app, valor e badge) e `SettingsScreen.kt` (cabeçalhos).

| Texto / fundo declarado | Contraste aproximado |
| --- | --- |
| Laranja `#FA8A2E` / superfície clara `#FDFBFA` | 2,33:1 |
| Laranja `#FA8A2E` / card claro `#F0EEEC` | 2,08:1 |
| Verde `#0E9D6E` / card claro `#F0EEEC` | 2,99:1 |
| Verde `#0E9D6E` / superfície escura `#343231` | 3,68:1 |
| Texto escuro `#0F0D0C` / laranja `#FA8A2E` | 8,07:1 |

A recomendação para texto pequeno é pelo menos 4,5:1. Portanto, manter a marca laranja é viável, mas seu uso como texto pequeno sobre fundos claros precisa mudar. O texto escuro sobre botões laranja já apresenta bom contraste nas cores declaradas. [Referência Android sobre contraste](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility).

Recomendação: usar `onSurface` para nomes e títulos, dar mais peso tipográfico ao valor monetário e reservar o laranja para ações e destaques. Criar cores semânticas de sucesso, aviso e erro adaptadas a cada tema. Hoje `errorContainer` se aproxima de outros containers neutros, diminuindo a diferenciação visual de problemas. Evitar colorir simultaneamente título, nome do app e valor como se todos tivessem a mesma prioridade.

## 2. Filtros e acessibilidade

Evidência: `HomeScreen.kt:279`. O `FilterChip` renderiza somente um ícone com `contentDescription = null`. O título informa o filtro selecionado, mas não identifica individualmente os controles.

Recomendação: rótulos visíveis “Enviados” e “Pendentes”, estado selecionado acessível e, se útil, contagem por filtro. Acrescentar “Com erro” ou um atalho para falhas quando houver ocorrências. Padronizar “Falha no envio” no card e no detalhe, que hoje usa “Pendente com erro”. Durante `SYNCING`, exibir “Enviando” também na lista; o badge atual cai no texto genérico “Pendente”.

A exclusão no card usa tamanho visual de 32 dp; a remoção de gatilhos, 18 dp. Compose pode ampliar automaticamente a região tocável, mas isso não garante bom espaçamento entre ações. Reservar pelo menos 48 dp por alvo e verificar sobreposição com o card e com chips vizinhos. O botão de remoção deve anunciar o termo específico, por exemplo “Remover gatilho compra”. [Referência de acessibilidade do Compose](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).

## 3. Sincronização e confiança no estado apresentado

Evidência: `HomeViewModel.kt:255`. `refreshData()` inicia outras corrotinas, agenda o worker e redefine `isRefreshing` para `false` sem aguardar a leitura nem a conclusão do envio. A lista usa consultas pontuais; não observa continuamente alterações do banco. Ao retomar a Home, o efeito atual atualiza apenas a permissão.

Recomendação: separar “atualizar lista” de “sincronizar agora” ou usar um único comando com estado explícito. Observar notificações e indicadores com Flow e acompanhar o WorkManager. Mostrar “Aguardando conexão”, “Enviando”, “Concluído” e erros com próximo passo. Ao voltar dos ajustes, refletir imediatamente apps adicionados ou desativados.

Evidência adicional: `SettingsViewModel.kt:93` considera “Conectado” quando há URL e token armazenados. Isso não comprova disponibilidade atual do servidor. Usar “Configurado” para esse estado, apresentando separadamente resultado e horário da última verificação. Não executar verificação de rede contínua apenas para sustentar o rótulo.

## 4. Histórico e contadores

Evidência: `HomeViewModel.kt:146` busca as últimas 100 notificações antes de aplicar o filtro. Uma pendência mais antiga pode não aparecer, mesmo sendo contada no resumo.

Recomendação: filtrar no banco antes de limitar, paginar e manter contador e lista coerentes. Oferecer acesso a descartados se o produto permitir recuperar ou auditar esses registros. Há uma rota de histórico em `AppNavigation.kt`, mas a Home não fornece navegação para ela; definir se a Home concentra o histórico ou se deve haver uma tela dedicada, evitando experiências duplicadas.

O indicador “Enviados hoje” usa `countSyncedSince`, cuja consulta filtra `created_at`. Assim, uma notificação capturada ontem e enviada hoje não entra nesse número. Para prometer envios do dia, usar a data real de envio; alternativamente, ajustar o rótulo ao que a consulta mede.

## 5. Setup e primeiro uso

Evidência: `SetupScreen.kt:170` e `SetupScreen.kt:238` usam colunas centralizadas sem rolagem; declaram `ImeAction.Done`, mas não configuram a ação correspondente.

Recomendação: conteúdo rolável, adaptação ao teclado e botão principal acessível em telas pequenas, paisagem e fontes maiores. Vincular “Concluir” do teclado à ação de verificar/conectar. Identificar etapas como “1 de 2 — Servidor” e “2 de 2 — Token”, explicar onde gerar o token e manter explícito o servidor escolhido. [Referência de insets e teclado](https://developer.android.com/develop/ui/compose/system/insets-ui).

Após autenticar, o fluxo navega diretamente para a Home. Recomendo um checklist curto para: habilitar captura, escolher apps e confirmar que a configuração está pronta. Separar a permissão para ler notificações bancárias da permissão para mostrar alertas do Companion. Pode haver conclusão guiada dentro da própria Home, sem obrigar novas telas.

No scanner, usar insets seguros para botão de fechar e instrução. Oferecer caminho para ajustes se a câmera tiver permissão negada permanentemente, mantendo a alternativa manual disponível. Confirmar “Token lido” sem exibir o segredo.

## 6. Exclusão e descarte

Evidência: `HomeViewModel.kt:176` exclui imediatamente o registro local; `discardNotification()` muda seu status, que fica fora dos dois filtros da Home. A limpeza geral já tem confirmação em `SettingsScreen.kt:156`.

Recomendação: Snackbar com “Desfazer” para exclusão individual e descarte, com suporte real à reversão. Explicar que excluir localmente não apaga um lançamento já enviado ao servidor. Na limpeza geral, informar quantas pendências serão removidas e usar “Excluir notificações” no botão, em vez de “Confirmar”. Posicionar ações destrutivas em uma área de dados claramente identificada.

## 7. Navegação, ajustes e densidade

Evidência: a Home reúne gatilhos, logs, atualizar e configurações na barra superior. A tela de ajustes apresenta alertas antes de apps monitorados, embora selecionar apps seja necessário para a tarefa principal. `AddAppDialog` fixa a altura em 400 dp e mostra nome técnico do pacote com destaque secundário constante.

Recomendação: manter acesso evidente à sincronização e aos ajustes; mover gatilhos e diagnósticos para itens textuais em ajustes ou menu. Avaliar largura da barra em telas compactas. Não há necessidade comprovada de adicionar navegação inferior a um app com uma tarefa principal.

Nos ajustes, apresentar captura/apps primeiro, depois conexão, alertas, dados e sobre. Se a lista de apps crescer, usar uma tela dedicada com busca e seleção múltipla. Adaptar a altura à janela; mostrar nome e ícone como informação principal e pacote apenas quando ajudar a distinguir apps com o mesmo nome. Considerar bottom sheet para detalhes curtos e tela completa para conteúdos longos.

## 8. Mensagens e recuperação de erros

Evidência: ambos os filtros compartilham “Nenhuma notificação capturada”. Falhas genéricas aparecem nos detalhes e alguns retornos usam Toast, que desaparece rapidamente.

Recomendação: “Nenhum envio ainda”, “Nenhuma pendência” e “Tudo enviado” conforme o contexto. Quando não houver captura, explicar os pré-requisitos e oferecer “Selecionar apps” ou “Ativar captura”. Expor erros de token com “Atualizar token”; erros temporários com “Tentar novamente”. Na exportação, manter confirmação visível com ação “Abrir” ou “Compartilhar”.

Padronizar vocabulário: notificação é o conteúdo capturado; lançamento é o registro enviado ao OpenMonetis. Evitar sugerir que o envio equivale a um processamento financeiro já confirmado pelo servidor. Usar datas relativas na lista, data completa nos detalhes e formatação monetária própria para BRL.

## Ordem sugerida e validação

1. Contraste, rótulos de filtros e estados reais de sincronização.
2. Filtro antes do limite, indicadores coerentes e atualização ao voltar dos ajustes.
3. Setup adaptável, checklist de primeiro uso e recuperação de falhas.
4. Desfazer, organização dos ajustes e refinamento dos cards.

Validar em execução: tema claro e escuro; tela compacta e paisagem; teclado aberto; fonte ampliada; TalkBack; permissão negada; nenhum app selecionado; servidor indisponível; token inválido; mais de 100 registros com pendências antigas; retorno dos ajustes; envio automático com a Home aberta. O objetivo é confirmar legibilidade, acesso às ações e correspondência entre estado mostrado e estado real.

## Alteração realizada nesta tarefa

Removida a configuração `buildTypes.create("v2")` de `app/build.gradle.kts` e o recurso exclusivo `app/src/v2/res/values/strings.xml`. Mantidos os tipos `debug` e `release` e o identificador principal do app. O build `:app:assembleDebug` passou após a remoção. As demais alterações locais existentes foram preservadas.
