# Identidade visual do Companion

O Companion reutiliza a identidade do projeto OpenMonetis localizado em `/Users/felipecoutinho/Documents/github/openmonetis`, consultado no commit `4395f89`. O projeto base foi apenas lido.

## Fontes de referência

- `apps/web/src/styles/app.css`: tokens OKLCH, misturas de cor, temas claro/escuro e raios de borda.
- `apps/web/src/styles/fonts.css` e `apps/web/src/assets/fonts/gt-america-*.woff2`: tipografia.
- `apps/web/src/components/openmonetis-logo.tsx`: proporções e uso do símbolo/wordmark por tema.
- `apps/web/public/images/openmonetis-mark.svg` e `openmonetis-wordmark.svg`: geometria oficial da marca.
- `apps/web/public/favicon.svg`: ícone com símbolo preto sobre fundo laranja.

Os SVGs originais estão preservados em `branding/`. Os arquivos Android usam a mesma geometria em VectorDrawable, incluindo as transformações de cada letra do wordmark. Não há imagens geradas por IA nem substituição por fontes para desenhar a marca.

## Paleta implementada

Valores convertidos de OKLCH para sRGB com arredondamento de 8 bits. `color-mix(in srgb, ...)` foi calculado no espaço sRGB, conforme declarado no CSS. A conversão confirma que o laranja dos tokens e dos SVGs é o mesmo `#FC941D`.

| Token web | Claro | Escuro | Uso Android |
| --- | --- | --- | --- |
| `--brand` / `--primary` | `#FC941D` | `#FC941D` | Botões preenchidos e marca |
| `--palette-charcoal` | `#3F2508` | `#3F2508` | Texto sobre laranja |
| `--brand-strong` | `#945712` | `#FC941D` | Textos de destaque e ações sem preenchimento |
| `--background` | `#F6F4F1` | `#111110` | Fundo do app e splash |
| `--foreground` | `#3F2508` | `#F7F4F0` | Texto principal e wordmark |
| `--card` | `#F6F4F1` | `#1A1A19` | Cards |
| `--popover` | `#F6F4F1` | `#212120` | Superfícies elevadas |
| `--secondary` | `#EFE9DF` | `#2A2A29` | Superfícies secundárias |
| `--muted` | `#EFE9DF` | `#262524` | Superfícies neutras |
| `--muted-foreground` | `#7A6753` | `#ADABA8` | Texto secundário |
| `--accent` | `#F7E5CF` | `#40311F` | Resumos e seleção |
| `--success` | `#287A50` | `#6ACB93` | Estados de sucesso |
| `--warning` | `#C37317` | `#F5B56B` | Avisos |
| `--destructive` | `#D72204` | `#FF786F` | Erros e ações destrutivas |
| `--info` / `--ring` | `#3950AD` | `#91A7FF` | Foco de formulário e carregamento |
| `--input` | `#BFB6AB` | `#4A4A48` | Borda de controles |
| `--border` composto sobre o fundo | `#CCCAC8` | `#282826` | Borda de cards |

No Material 3, `primary` corresponde ao laranja, `secondary` ao destaque legível `brand-strong`, `tertiary` a `info`, e `primaryContainer` a `accent`. Isso preserva os botões preenchidos laranja sem aplicar laranja claro aos textos de links. As superfícies tonais padrão do Material foram substituídas pela paleta do projeto; `surfaceTint` é transparente para evitar tonalidades externas à marca.

Containers de erro e informação são adaptações nativas: erro usa 12% da cor semântica sobre o fundo e informação usa 14%. No container de erro claro, o texto usa o foreground da marca para manter a legibilidade. Os indicadores de status usam as cores do tema ativo, inclusive em dialogs.

## Aplicação

- `ui/theme/Theme.kt`: paleta, cores de status, GT America nos pesos 400/500/700 e formas.
- `ui/components/OpenMonetisLogo.kt`: símbolo laranja e wordmark no foreground do tema, mantendo a proporção original. A Home mantém símbolo e wordmark com escala adequada a janelas compactas.
- `OpenMonetisButtons.kt` e `OpenMonetisDefaults.kt`: ações com contraste e foco de formulário informacional.
- Home e histórico: valores destacados, card operacional preenchido com `primaryContainer` (laranja suave no claro e marrom discreto no escuro), com texto adaptado ao tema e filtros em até duas colunas que respeitam a largura e o tamanho de fonte disponíveis. A Home exibe os ícones dos apps monitorados.
- No tema claro, a barra de status acompanha o laranja primário e usa símbolos escuros, inclusive no modo edge-to-edge do Android 15+.
- Setup e Sobre: marca oficial.
- `values/` e `values-night/`: temas nativos, barras do sistema, cores de splash.
- `mipmap-anydpi-v26/ic_launcher.xml`: adaptive icon do Companion com fundo branco puro (`#FFFFFF`), símbolo laranja (`#FC941D`), 20% menor que a versão anterior, e máscara monocromática. Essa variação distingue o app nativo do PWA, que conserva fundo laranja e símbolo preto. A geometria fica dentro da área segura de 66 dp do ícone de 108 dp. Como o app exige Android 12+, não precisa das versões raster por densidade.
- `drawable/ic_notification_small.xml`: símbolo vetorial branco para a máscara das notificações.
- `logo.png`: rasterização do símbolo oficial para o README.
- `branding/openmonetis-lockup-light.svg` e `openmonetis-lockup-dark.svg`: símbolo e wordmark oficiais combinados para o cabeçalho do README, que seleciona o asset conforme o tema do leitor. `logo.png` permanece disponível como símbolo isolado.
- `android/res/`: exports raster auxiliares dos ícones adaptativos atuais, nas dimensões originais de cada densidade. `android/play_store_512.png` apresenta a variação branca/laranja do Companion em 512 px. Esses arquivos não fazem parte do source set compilado.

GT America foi descomprimida de WOFF2 para TTF usando `woff2_decompress`, sem alterar os desenhos das fontes. Android usa os arquivos locais; não depende de download de fontes. A fonte monoespaçada Aeonik Fono permanece como referência do site, pois o app não tem conteúdo monoespaçado que justifique sua inclusão.

## Contraste e validação

Contrastes calculados a partir das cores declaradas nos contextos de uso:

| Combinação | Contraste aproximado |
| --- | --- |
| Destaque `#945712` / fundo claro `#F6F4F1` | 5,27:1 |
| Sucesso `#287A50` / card claro `#F6F4F1` | 4,79:1 |
| Texto secundário `#7A6753` / fundo claro `#F6F4F1` | 4,91:1 |
| Texto `#3F2508` / botão `#FC941D` | 6,35:1 |
| Sucesso `#6ACB93` / superfície escura `#262524` | 7,71:1 |
| Marca `#FC941D` / fundo escuro `#111110` | 8,45:1 |

A comparação de imagens renderizadas dos VectorDrawables com os SVGs originais resultou em diferença zero para símbolo e wordmark. O símbolo do adaptive icon ocupa um raio máximo de 25,70 dp, dentro da área segura de 33 dp.

A prévia em `branding-preview.png` apresenta logos, paletas e máscaras de ícone, sem representar uma captura do app em execução. Validação concluída: `:app:assembleDebug` e `:app:lintDebug` passaram, com zero erros de lint; avisos não bloqueantes permanecem no relatório. A comparação dos vetores com os SVGs originais também passou. As capturas e a validação em emulador, incluindo fonte ampliada, estão documentadas em [UI_IMPLEMENTATION.md](UI_IMPLEMENTATION.md). TalkBack em aparelho físico permanece como etapa de homologação.
