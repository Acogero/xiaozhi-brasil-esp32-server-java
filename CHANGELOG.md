# Changelog

## [5.0.0] - 2025-04-10

### 💥 Mudanças Importantes
- **refactor!: projeto dividido em arquitetura multi-módulo** 
  - Refatorado de projeto monolítico para multi-módulo Maven: `xiaozhi-common`, `xiaozhi-service`, `xiaozhi-ai`, `xiaozhi-dialogue`, `xiaozhi-server`
  - Módulos desacoplados por interfaces estreitas; módulo de IA desacoplado da camada de Service (narrow ports)
  - Componentes Web migrados do módulo common para o server, com responsabilidades mais claras

### Novos Recursos

#### Arquitetura & Infraestrutura
- feat: introdução do Flyway para versionamento do banco de dados
- feat: introdução do padrão DDD (Domain-Driven Design) (aggregate root, eventos de domínio, Pipeline)
- feat: novos scripts Shell genéricos para gerenciamento de serviços
- feat: infraestrutura de testes unitários (Vitest) e E2E (Playwright) no frontend
- feat: nova anotação de log de auditoria (AuditLog), reforçando o rastreamento de operações

#### Permissões & Segurança
- feat: refatoração do sistema de gerenciamento de permissões
- feat: nova página de gerenciamento de permissões (frontend)
- feat(auth): exposição das permissões de papel e exibição no gerenciamento de usuários

#### IA & Diálogo
- feat: aprimoramento da memória de longo prazo, com suporte a recuperação em grafo (Graph Retrieval)
- feat: refatoração da base de conhecimento, integrando memória de longo prazo e reconhecimento de voz
- feat: refatoração do Pipeline da base de conhecimento
- feat: implementação da síntese de voz em streaming via Edge TTS
- feat: nova função para ler quadros de áudio no formato Ogg Opus a partir do stream de entrada

#### Frontend
- feat: novo suporte de fallback para o player de áudio

### Refatoração & Otimização

#### Padronização do Tratamento de Exceções
- refactor: padronização completa do tratamento de exceções na camada Controller (em 4 etapas)
- refactor: padronização da semântica de falha nas operações CRUD (create/update/delete)
- refactor: padronização do tratamento de exceções em módulos como clonagem de voz, reconhecimento de voz e validação de usuário
- refactor: reforço do tratamento de erros no protocolo OTA, upload/TTS e endpoints especiais
- refactor: eliminação da semântica de falha por null e zero; a criação interna de BO não retorna mais null

#### Modelo de Domínio & Nomenclatura
- refactor: SysSummary substituído por SummaryBO
- refactor: ConversationTurn renomeado para DialogueTurn
- refactor: functionNames renomeado para mcpList
- refactor: padronização da nomenclatura de auth role permission
- refactor: campo de papel do usuário padronizado como authRole
- refactor: refatoração do ApiResponse para padronizar o formato de resposta
- refactor: refatoração do gerenciamento de token e dos métodos de envio de mensagem

#### API & Interfaces
- refactor: refatoração dos endpoints da API para o estilo RESTful
- refactor: parâmetros de paginação padronizados de start/limit para pageNo/pageSize
- refactor: padronização do contrato de erros do MCP Controller
- refactor: separação dos módulos mcpServer e mcpEndpoint
- refactor: substituição da conversão manual de login id pela API tipada do sa-token

#### Desacoplamento de Arquitetura
- refactor: desacoplamento moderado da arquitetura de Session
- refactor: os módulos Dialogue e Server passam a escanear apenas os pacotes necessários
- refactor: remoção do vazamento do objeto req nos fluxos de dialogue e de runtime interno
- refactor: o ID do usuário passa a ser transmitido explicitamente nas fronteiras de service e task
- refactor: consolidação da configuração de caminhos em runtime, reduzindo caminhos fixos nos serviços STT/TTS
- refactor: remoção de caminhos fixos no código
- refactor: padronização do local de cache
- refactor: padronização dos eventos de domínio
- refactor: otimização da lógica de diálogo e remoção de métodos redundantes de virtual thread

#### Limpeza
- refactor: remoção das classes não utilizadas ExitKeywordDetector e IntentDetector
- refactor: remoção das classes não utilizadas HttpSessionProvider, ResponseUtils, SessionProvider e DramaJson
- refactor: remoção da funcionalidade de limiar de voz personalizado
- refactor: remoção de vários documentos de arquitetura desatualizados
- delete: remoção da transmissão redundante de userId na base de conhecimento
- delete: remoção de métodos não utilizados

### Correções
- fix: correção de diversos bugs e otimização da implantação em cluster
- fix: correção de problema de implantação em cluster do endpoint e da dependência de migração do mcpServer
- fix: correção de bug no envio de frases de áudio do CosyVoice
- fix: correção de problema em que o dispositivo não era atualizado a tempo após a atualização do papel/persona
- fix: correção da construção duplicada de mensagens de diálogo via MQTT e do envio múltiplo de "start" pela palavra de ativação
- fix: correção de erro no som de teste e de erro no acerto do cache de áudio
- fix: correção de erro de inicialização de classe
- fix: correção de chamada de método incorreta

### Docker & Implantação
- refactor: padronização da rede Docker, adição de limites de recursos e ajuste do tamanho de upload para 100MB
- update: Dockerfile-server adaptado para o build multi-módulo
- update: contexto de build do Dockerfile-node ajustado para o diretório web
- update: regras de exclusão do .dockerignore atualizadas
- update: remoção do volume maven_repo não utilizado do docker-compose.yml

### Testes
- test: linha de base de testes do Controller concluída
---

## [3.0.0] - 2025-11-01

### 💥 Mudanças Importantes
- **feat: arquitetura de frontend totalmente atualizada para Vue 3** 🎉
  - Migração completa para Vue 3.5.22 + Composition API
  - Uso do Vite 7 como ferramenta de build, melhorando a experiência de desenvolvimento e a velocidade de build
  - Adoção do TypeScript 5.9 para reforçar a segurança de tipos
  - Gerenciamento de estado atualizado para o Pinia 3
  - Roteamento atualizado para o Vue Router 4
  - Código refatorado com o padrão Composables, aumentando a reutilização

- **feat: arquitetura de backend totalmente atualizada e refatorada** 🚀
  - Introdução do mecanismo de autenticação JWT, reforçando a segurança
  - Novo encapsulamento padronizado de resultado (ResultMessage/ResultStatus)
  - Nova arquitetura orientada a eventos (ChatSessionOpenEvent, ChatAbortEvent, etc.)
  - Novo sistema completo de gerenciamento de permissões (RBAC)
  - Camada Controller totalmente refatorada, com estrutura de código mais clara

### Novos Recursos

#### Frontend
- feat: runtime Node.js atualizado para a v22
- feat: introdução de um toolchain de desenvolvimento moderno
  - Uso do oxlint e do ESLint 9 para lint do código
  - Integração do Vue DevTools 8 para depuração
  - Adoção do Prettier 3.6 para padronizar o estilo de código
- feat: biblioteca de componentes de UI atualizada para o Ant Design Vue 4.2.6
- feat: nova biblioteca de utilitários @vueuse/core, oferecendo uma rica API de composição
- feat: novo componente de carregamento global e error boundary
- feat: novo componente de chat flutuante, aprimorando a experiência de interação

#### Funcionalidades Principais do Backend
- feat: novo sistema de autenticação JWT (JwtUtil)
  - Suporte à geração e renovação de Token
  - Suporte a Token de login via WeChat
  - Suporte a claims personalizadas
- feat: novo serviço de login via WeChat (WxLoginService)
- feat: novo sistema de gerenciamento de permissões
  - Mapeamento de permissões por papel (SysAuthRole, SysPermission, SysRolePermission)
  - Controle de permissões RBAC completo
- feat: novo utilitário de captcha (CaptchaUtils)
- feat: novo utilitário de e-mail (EmailUtils)
- feat: novo serviço de SMS (SmsUtils)
- feat: novo utilitário de hash de arquivos (FileHashUtil)
- feat: novo utilitário de aprimoramento de áudio (AudioEnhancer)

#### AI & LLM
- feat: novo serviço de LLM da OpenAI (OpenAiLlmService)
  - Suporte a resposta em streaming
  - Suporte ao modo de raciocínio profundo (deep thinking)
  - Suporte a Function Calling
  - Novo mecanismo de callback de Token
- feat: novo suporte ao MCP (Model Context Protocol)
  - Gerenciamento de MCP Session
  - Integração do serviço de dispositivo MCP
- feat: aprimoramento do serviço de diálogo (DialogueService)
  - Otimização do gerenciamento de sessão
  - Melhoria do fluxo de processamento de mensagens
  - Suporte a arquitetura orientada a eventos
- feat: grande refatoração do serviço de VAD
  - Otimização da detecção de atividade de voz
  - Melhoria do modelo Silero VAD
  - Nova configuração de parâmetros avançados

#### Atualização de Dependências
- update: SDK da Alibaba Cloud totalmente atualizado
  - nls-sdk-transcriber: 2.2.1 → 2.2.18
  - nls-sdk-tts: 2.2.17 → 2.2.18
  - dashscope-sdk-java: 2.20.2 → 2.20.6
  - Novo SDK de SMS da Alibaba Cloud 2.0.24
- update: dependências do Spring Boot atualizadas
  - Novo spring-boot-starter-data-redis (aprimoramento de cache)
  - Integração do spring-ai-starter-mcp-client
- update: commons-io: 2.11.0 → 2.18.0
- update: okhttp: 5.0.0-alpha.14 → 4.9.3 (melhoria de estabilidade)
- update: novo okio 3.13.0

### Otimizações e Melhorias

#### Otimizações no Frontend
- perf: desempenho do servidor de desenvolvimento do Vite bastante aprimorado
- perf: otimização do tamanho do build de produção e melhoria da velocidade de carregamento
- perf: otimização dos guards de rota e da verificação de permissões
- update: imagem Docker atualizada para node:22-alpine
- update: dependências totalmente atualizadas para as versões estáveis mais recentes
- update: otimização da configuração do ambiente de desenvolvimento e do mecanismo de hot reload
- dx: melhor inferência e sugestão de tipos do TypeScript
- dx: Hot Module Replacement (HMR) mais rápido

#### Otimizações no Backend
- refactor: aprimoramento do tratamento global de exceções (GlobalExceptionHandler)
  - Nova exceção de recurso não encontrado (ResourceNotFoundException)
  - Nova exceção de não autorizado (UnauthorizedException)
  - Padronização do formato de resposta de exceções
- refactor: refatoração do interceptor de autenticação (AuthenticationInterceptor)
  - Suporte a autenticação JWT
  - Otimização da lógica de verificação de permissões
- refactor: refatoração do gerenciamento de sessão (SessionManager)
  - Melhoria do gerenciamento do ciclo de vida da sessão
  - Otimização do processamento concorrente
- refactor: refatoração do processador de mensagens (MessageHandler)
  - Otimização do fluxo de mensagens
  - Melhoria do tratamento de erros
- refactor: otimização do handler de WebSocket (WebSocketHandler)
  - Reforço do gerenciamento de conexões
  - Melhoria do tratamento de exceções
- refactor: otimização do sistema de memória de diálogo
  - Refatoração do DatabaseChatMemory
  - Melhoria do MessageWindowConversation
  - Otimização da interface Conversation
- refactor: otimização da chamada de ferramentas do LLM
  - Melhoria do ToolsGlobalRegistry
  - Refatoração do XiaoZhiToolCallingManager
  - Novo NewChatFunction
- refactor: otimização do serviço de STT
  - Otimização do código de todos os provedores de STT
  - Melhoria do tratamento de erros e dos logs
- refactor: otimização das classes de entidade
  - Melhorias em SysConfig, SysDevice, SysMessage, SysUser
- refactor: otimização dos Mapper XML
  - Refatoração de todos os arquivos Mapper
  - Otimização do SQL
- refactor: refatoração completa da camada Service
  - Nova configuração de transação (TransactionConfig)
  - Otimização da lógica de negócio
  - Melhoria da camada de acesso a dados

### Atualizações no Docker
- update: otimização da configuração do docker-compose.yml
  - Melhoria das dependências entre serviços
  - Otimização do health check
  - Reforço da configuração de rede
- update: Dockerfile-node atualizado para o Node 22

---

## [2.8.17] - 2025-07-16
### Novos Recursos
- feat: novo Swagger
- update: modelos com etiquetas de identificação adicionadas
- update: remoção do botão de minimizar redundante no chat global
- update: otimização do estilo de exibição, permitindo alternar o estilo da aba do navegador
- update: entidades passam a usar métodos do Lombok
### Correções
- fix: correção de erro de endereço
- fix: correção de problema em que o código de verificação não funcionava ao adicionar dispositivo
- fix: correção de campo ausente na inicialização do script SQL init
- fix: correção das issues #119 #120
### Otimização de Estilo
- style: atualização da animação de zoom do chat global, mais próxima do efeito estilo Apple
- otimização: estilo do chat
### Remoções
- delete: remoção de logs inúteis
### Refatoração
- refactor(stt): otimização da estrutura de código da classe VoskSttService
- refactor: remoção de logs redundantes

# Changelog
## [2.8.16] - 2025-07-02
### Outras Mudanças
- refactor: refatoração do vad, removendo o agc
- refactor: refatoração da lógica de envio de áudio, enviando conforme a posição real do quadro

# Changelog
## [2.8.15] - 2025-07-01

### Correções
- fix: correção de erro na atualização de tag
- fix: correção de problema em que, ao modificar a configuração do papel/persona durante a escuta do dispositivo, a atualização do cache causava múltiplas consultas ao banco de dados
- fix: correção de campo de avatar ausente na inicialização (init)

### Outras Mudanças
- refactor: otimização do cache de token, reduzindo código redundante
- update: nível de log do SDK da Alibaba alterado para warn

## [2.8.0] - 2025-06-15

### Novos Recursos
- feat: adição de entrada do logback, fecha #37
- feat: nova exibição da quantidade de dispositivos em laranja

### Correções
- fix(stt.aliyun): do not reuse recognizer
- fix(stt.aliyun): support long speech recognition
- fix: memory leak. Should clean up dialogue info after session closed

### Outras Mudanças
- chore: update version to 2.8.0 [skip ci]
- update: retorno do papel/persona passa a incluir modelName
- docs: update changelog for v2.7.68 [skip ci]
- chore: update version to 2.7.68 [skip ci]
- docs: update changelog for v2.7.67 [skip ci]
- chore: update version to 2.7.67 [skip ci]
- docs: update changelog for v2.7.66 [skip ci]
- chore: update version to 2.7.66 [skip ci]
- refactor(stt): simplify SttServiceFactory

## [2.7.68] - 2025-06-14

### Correções
- fix(stt.aliyun): do not reuse recognizer
- fix(stt.aliyun): support long speech recognition
- fix: memory leak. Should clean up dialogue info after session closed

### Outras Mudanças
- chore: update version to 2.7.68 [skip ci]
- docs: update changelog for v2.7.67 [skip ci]
- chore: update version to 2.7.67 [skip ci]
- docs: update changelog for v2.7.66 [skip ci]
- chore: update version to 2.7.66 [skip ci]
- refactor(stt): simplify SttServiceFactory

## [2.7.67] - 2025-06-14

### Correções
- fix: memory leak. Should clean up dialogue info after session closed

### Outras Mudanças
- chore: update version to 2.7.67 [skip ci]
- docs: update changelog for v2.7.66 [skip ci]
- chore: update version to 2.7.66 [skip ci]

## [2.7.64] - 2025-06-12

### Correções
- Merge pull request #98 from vritser/main
- fix(audio): merge audio files

### Outras Mudanças
- chore: update version to 2.7.64 [skip ci]
- docs: update changelog for v2.7.63 [skip ci]
- chore: update version to 2.7.63 [skip ci]

## [2.7.60] - 2025-06-11

### Novos Recursos
- Merge pull request #96 from vritser/main
- feat(tts): support minimax t2a

### Correções
- fix: correção de parâmetro redundante na síntese de voz da Alibaba, removido
- fix(tts): tts service factory

### Outras Mudanças
- chore: update version to 2.7.60 [skip ci]
- docs: update changelog for v2.7.59 [skip ci]
- chore: update version to 2.7.59 [skip ci]
- refactor(tts): add default implements
- docs: update changelog for v2.7.58 [skip ci]
- chore: update version to 2.7.58 [skip ci]

## [2.7.59] - 2025-06-11

### Novos Recursos
- Merge pull request #96 from vritser/main
- feat(tts): support minimax t2a

### Correções
- fix(tts): tts service factory

### Outras Mudanças
- chore: update version to 2.7.59 [skip ci]
- refactor(tts): add default implements
- docs: update changelog for v2.7.58 [skip ci]
- chore: update version to 2.7.58 [skip ci]

