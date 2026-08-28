# Guia de execução — xiaozhi-brasil-esp32-server-java (Windows)

Passo a passo para deixar o projeto rodando localmente no Windows, via IntelliJ ou linha de comando. Revisado e complementado com base no código do repositório (`docker-compose-db.yml`, `application*.yml`, `bin/_common.sh`, `providerConfig.ts`) e nas notas de memória do projeto.

## Arquitetura (contexto rápido)

O projeto é **multi-módulo, processo duplo**, compartilhando o mesmo MySQL + Redis:

| Serviço | Módulo Maven | Porta | Função |
|---|---|---|---|
| Painel administrativo | `xiaozhi-server` | 8091 | API REST, gestão de usuários/dispositivos/personas, OTA |
| Diálogo | `xiaozhi-dialogue` | 8092 | WebSocket/MQTT, pipeline de voz em tempo real com IA |
| Frontend | `web/` | 8084 | UI de administração (Vue 3 + Ant Design) |

Módulos de suporte (sem serviço próprio, apenas bibliotecas): `xiaozhi-common`, `xiaozhi-service`, `xiaozhi-ai`.

---

## 1. Pré-requisitos

- **JDK 21** — `java -version`
- **Maven** — `mvn -v`
- **Node.js** — versão exigida pelo `web/package.json`: `^20.19.0` ou `>=22.12.0` (Node 22 LTS é o caminho mais simples) — `node -v`
- **Git Bash** — os scripts do projeto (`bin/*.sh`, `scripts/*.sh`) são shell scripts; no Windows, rode-os pelo Git Bash (ou WSL), não pelo CMD/PowerShell
- **Docker Desktop** — mais simples do que instalar MySQL e Redis nativamente no Windows

> O `docs/WINDOWS_DEVELOPMENT.md` do repositório só menciona instalar MySQL nativo, mas o `application.yml` também exige Redis (`spring.data.redis`, `spring.cache.type: redis`, além do Redisson para lock distribuído). O caminho mais rápido no Windows é subir **só a infraestrutura** em container e rodar os serviços Java localmente (via IntelliJ ou Maven), como no passo 2 abaixo.

## 2. Subir a infraestrutura (MySQL + Redis)

```bash
docker compose -f docker-compose-db.yml up -d
```

Esse compose já sobe **os dois serviços** (não precisa de um segundo comando para o Redis):

| Serviço | Porta | Credenciais |
|---|---|---|
| MySQL 8.0 | 3306 | banco `xiaozhi`, usuário `xiaozhi` / senha `123456` (root: `abc123456`) |
| Redis 7 | 6379 | sem autenticação (`database: 0`) |

O usuário/banco/charset (`utf8mb4_unicode_ci`) são criados automaticamente pelas variáveis de ambiente do container — **não é necessário** rodar o `CREATE USER`/`GRANT` manual que aparece em `docs/WINDOWS_DEVELOPMENT.md`; isso só seria necessário se você instalasse o MySQL nativamente. As credenciais já vêm pré-configuradas em `application-dev.yml` de `xiaozhi-server` e `xiaozhi-dialogue`, então não há nada para editar aqui.

Não precisa importar SQL na mão — o Flyway cria o schema completo no primeiro start do `xiaozhi-server` (só ele roda as migrations; o `xiaozhi-dialogue` tem `flyway.enabled: false` para não duplicar).

> **Se aparecer erro de migration falha logo no primeiro start** ("Schema `xiaozhi` contains a failed migration to version 1!"): isso vinha de uma versão antiga de `V1__init.sql` que incluía `CREATE USER`/`GRANT`/`FLUSH PRIVILEGES`, comandos que falham porque o Flyway se conecta com o usuário `xiaozhi` (sem privilégio global, só `ALL PRIVILEGES ON xiaozhi.*`). Se o seu checkout ainda tiver esse problema, limpe a entrada falha antes de reiniciar:
> ```sql
> USE xiaozhi;
> DELETE FROM flyway_schema_history WHERE success = 0;
> ```

## 3. Baixar modelos e bibliotecas nativas (obrigatório na primeira vez)

Via **Git Bash**, na raiz do projeto:

```bash
./scripts/download_models.sh          # tudo: VAD + libs nativas (sherpa-onnx JNI + Vosk) + STT + TTS
# ou, se for usar STT/TTS de provedores em nuvem (não local):
./scripts/download_base.sh            # só VAD + libs nativas — obrigatório sempre, mesmo usando STT/TTS de terceiros
```

Isso popula `lib/` (DLLs/dylibs nativas) e `models/` (`silero_vad.onnx`, `vosk-model/`, `tts/`) **na raiz do projeto**. Verifique o status a qualquer momento com:

```bash
./scripts/download_models.sh status
```

> **Modelos TTS locais (Sherpa-ONNX) que você adicionar manualmente depois** precisam ficar em `models/tts` já **extraídos como subpasta** (ex.: `models/tts/vits-piper-pt_BR-faber-medium/`) — o backend só reconhece diretórios, nunca o `.tar.bz2` compactado direto. Depois de extrair, o scan é feito ao vivo: basta recarregar a tela de Personagem, não precisa reiniciar o backend. Além disso, as vozes locais só aparecem no dropdown de vozes do Personagem se já existir, em **Configurações → Configuração de síntese de voz**, um registro cadastrado com provider **"Sherpa-ONNX (local)"** — sem esse cadastro, o dropdown fica vazio mesmo com os modelos extraídos corretamente.

## 4. Rodar o backend

### Opção A — Scripts (Git Bash / WSL)

```bash
bin/all.sh start       # compila (mvn clean install) e inicia server + dialogue
bin/all.sh status      # verifica status/porta de cada serviço
bin/all.sh stop        # para os dois
bin/all.sh restart
```

### Opção B — Maven na mão (CMD / PowerShell / Git Bash)

```bash
mvn clean install -DskipTests

java -Djava.library.path=lib -jar xiaozhi-server\target\xiaozhi-server-*.jar
java -Djava.library.path=lib -jar xiaozhi-dialogue\target\xiaozhi-dialogue-*-exec.jar
```

Rode cada `java -jar` em um terminal separado, com o diretório de trabalho na **raiz do projeto** (é de lá que os caminhos relativos `lib`, `models/vosk-model`, `models/tts`, `models/silero_vad.onnx` são resolvidos).

### Opção C — IntelliJ IDEA (dois Run Configurations Spring Boot)

1. Abra a raiz do projeto como projeto Maven (deixe o IntelliJ importar o reactor com os 5 módulos) e rode **Maven → Reload** / `mvn clean install -DskipTests` uma vez, para gerar os artefatos de `xiaozhi-common`, `xiaozhi-service` e `xiaozhi-ai` dos quais `xiaozhi-server` e `xiaozhi-dialogue` dependem.
2. Crie um **Run/Debug Configuration → Spring Boot** para cada main class:
   - `com.xiaozhi.XiaozhiApplication` (módulo `xiaozhi-server`)
   - `com.xiaozhi.DialogueApplication` (módulo `xiaozhi-dialogue`)
3. Em **VM options** de cada um, adicione:
   ```
   -Djava.library.path=lib
   ```
4. **Atenção ao Working directory:** por padrão, o IntelliJ define o *working directory* de um Run Configuration Spring Boot como a raiz do **módulo** (ex.: `...\xiaozhi-server`), não a raiz do projeto. Como `lib/` e `models/` só existem na raiz (`D:\_PROJETOS\Backend\xiaozhi-brasil-esp32-server-java`), com o working directory errado o backend sobe mas falha ao carregar a lib nativa/os modelos (`-Djava.library.path=lib` aponta para uma pasta que não existe dentro do módulo). Edite o campo **Working directory** dos dois Run Configurations para a pasta raiz do projeto.
5. Rode os dois configurations (server e dialogue). Ative o profile `dev` se ele não vier marcado por padrão (`ACTIVE_PROFILES=dev`).

> Nota: se você reaproveitar um `.idea/workspace.xml` já existente no projeto, confira se **ambos** os Run Configurations têm o VM option `-Djava.library.path=lib` — é comum o de `DialogueApplication` ficar sem essa opção nas configurações salvas, e é justamente o `xiaozhi-dialogue` que roda o pipeline de voz em tempo real (STT/TTS via JNI), então ele também precisa da lib nativa.

## 5. Frontend (opcional, para administrar via UI)

```bash
cd web
npm install
npm run dev
```

Acesse **http://localhost:8084** — login padrão: `admin` / `123456`.

## 6. Endereços de acesso — resumo

| Serviço | Endereço |
|---|---|
| Frontend | http://localhost:8084 |
| API do painel administrativo | http://localhost:8091 |
| Swagger/OpenAPI | http://localhost:8091/swagger-ui.html |
| WebSocket de diálogo | ws://localhost:8092/ws/xiaozhi/v1/ |

## 7. Problemas comuns

| Problema | Solução |
|---|---|
| Conflito de porta | Altere `server.port` em `xiaozhi-server/src/main/resources/application.yml` (server) ou `xiaozhi-dialogue/src/main/resources/application.yml` (dialogue) |
| Falha ao conectar no MySQL/Redis | Confirme que os containers estão de pé: `docker compose -f docker-compose-db.yml ps` |
| Falha no build Maven | Rode `mvn clean install` e confirme acesso ao Maven Central |
| `UnsatisfiedLinkError` / lib nativa não encontrada ao rodar pelo IntelliJ | Veja o passo 4, opção C, item 4 — quase sempre é o *working directory* do Run Configuration apontando para o módulo em vez da raiz do projeto |
| Vozes Sherpa-ONNX não aparecem no Personagem | Confirme que a pasta em `models/tts` está extraída (não `.tar.bz2`) **e** que existe um registro com provider "Sherpa-ONNX (local)" em Configurações → Configuração de síntese de voz |
| Erro "无此权限" / permissão insuficiente em Configuração de papéis → Ferramentas MCP | Reinicie o `xiaozhi-server` — a migration Flyway que semeia essas permissões roda automaticamente na inicialização |

---

## 8. Cadastrando um modelo LLM (Gemini)

Depois que o backend e o frontend estiverem no ar, para cadastrar o Gemini como modelo:

**Modelo Categoria:** `Gemini`

**Tipo de modelo:** deixe em **"Modelo de conversa"** se este Gemini vai ser o LLM principal de um agente (uso mais comum). Só use **"Modelo multimodal"** se quiser que ele sirva como modelo padrão de visão (leitura de imagem) do sistema — internamente o catálogo do Gemini está marcado como multimodal, então nesse tipo o campo de nome do modelo ganha autocompletar; em "conversa" você digita o nome manualmente, sem problema.

**Modelo Nome — atenção aqui:** esse campo não é um apelido; ele é enviado literalmente como o parâmetro `model` na chamada à API do Google. Coloque um ID de modelo válido, por exemplo:

```
gemini-3.6-flash
```

Confirmado funcionando em 28/08/2026 — como a Google atualiza os IDs de modelo periodicamente, se este parar de responder, confira a lista atual em [ai.google.dev/gemini-api/docs/models](https://ai.google.dev/gemini-api/docs/models).

**Modelo Descrição:** campo livre — se quiser um apelido tipo "Gemini-talk", pode usar.

**Definir como padrão:** ligue só se quiser que esse seja o modelo usado automaticamente quando nenhum outro for escolhido explicitamente para um agente.

**Modo de raciocínio:** deixe desligado. Esse toggle ativa `reasoningEffort` (pensado para o1/o3/GLM-Z1), e não há garantia de que a camada de compatibilidade OpenAI do Gemini trate esse parâmetro corretamente — pode gerar erro.

**API Key:** cole a chave gerada em [aistudio.google.com/apikey](https://aistudio.google.com/apikey).

**API URL — o ponto mais importante a corrigir:** o placeholder do campo mostra só `https://generativelanguage.googleapis.com`, mas a tela sempre completa a URL final com o sufixo fixo `/chat/completions` (exibido em cinza à direita do campo — isso é comportamento confirmado no código do frontend, `providerConfig.ts`). Se você deixar só o domínio, a chamada vai para `.../chat/completions`, que não existe na API do Google — vai dar erro.

O endpoint compatível com OpenAI do Gemini é:

```
https://generativelanguage.googleapis.com/v1beta/openai
```

Preencha **exatamente assim** no campo API URL (sem barra final), para que o resultado montado pelo sistema fique:

```
https://generativelanguage.googleapis.com/v1beta/openai/chat/completions
```

que é o endpoint real do Google.
