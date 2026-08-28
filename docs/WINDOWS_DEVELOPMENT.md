# Guia de Implantação no Windows

## Requisitos do Sistema

- Windows 10+, com permissões de administrador

## 1. Instalar Dependências

| Dependência | Download | Variável de Ambiente | Verificação |
|------|------|----------|------|
| JDK 21 | [Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21) | `JAVA_HOME` → caminho de instalação; adicione `%JAVA_HOME%\bin` ao Path | `java -version` |
| MySQL 8.0 | [MySQL Installer](https://dev.mysql.com/downloads/installer/) | Adicione `C:\Program Files\MySQL\MySQL Server 8\bin` ao Path | `mysql --version` |
| Maven | [Download do Maven](https://maven.apache.org/download.cgi) | `MAVEN_HOME` → caminho de extração; adicione `%MAVEN_HOME%\bin` ao Path | `mvn -v` |
| Node.js | [Node.js LTS](https://nodejs.org/) | Configurado automaticamente pelo instalador | `node -v` |

## 2. Configuração do Banco de Dados

```sql
mysql -u root -p
CREATE DATABASE xiaozhi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'xiaozhi'@'localhost' IDENTIFIED BY '123456';
GRANT ALL PRIVILEGES ON xiaozhi.* TO 'xiaozhi'@'localhost';
FLUSH PRIVILEGES;
```

> Não é necessário importar o SQL manualmente — o projeto integra o Flyway, que cria as tabelas automaticamente na primeira inicialização.

## 3. Baixar Modelos e Bibliotecas Nativas

Se estiver usando serviços de STT/TTS de terceiros, é possível baixar somente as dependências básicas. Execute no Git Bash:

```bash
./scripts/download_models.sh            # baixa tudo (modelos + bibliotecas nativas)
./scripts/download_models.sh status     # verifica o status
```

Também é possível baixar individualmente, conforme a necessidade:

```bash
./scripts/download_base.sh              # dependências básicas (modelo VAD + bibliotecas nativas) — obrigatório
./scripts/download_stt.sh               # modelo Vosk STT (pode ser pulado se usar STT de terceiros)
./scripts/download_tts.sh               # modelo TTS (pode ser pulado se usar TTS de terceiros)
```

> Download manual: baixe `vosk-model-cn-0.22` em [Modelos Vosk](https://alphacephei.com/vosk/models), extraia e renomeie para `models\vosk-model`.

## 4. Implantação

O projeto adota uma **arquitetura de dois processos**:

| Serviço | Porta | Descrição |
|------|------|------|
| xiaozhi-server | 8091 | API do painel administrativo, gerenciamento de usuários/dispositivos |
| xiaozhi-dialogue | 8092 | Diálogo com dispositivos, IA, WebSocket |

```bash
git clone https://github.com/joey-zhou/xiaozhi-esp32-server-java
cd xiaozhi-esp32-server-java
```

### Método 1: Scripts bin (Git Bash / WSL)

```bash
bin/all.sh start       # compilar e iniciar
bin/all.sh status      # verificar status
bin/all.sh restart     # reiniciar
```

### Método 2: Inicialização Manual (CMD / PowerShell)

```bash
mvn clean install -DskipTests

# Terminal 1: iniciar o painel administrativo
java -Djava.library.path=lib -jar xiaozhi-server\target\xiaozhi-server-*.jar

# Terminal 2: iniciar o serviço de diálogo
java -Djava.library.path=lib -jar xiaozhi-dialogue\target\xiaozhi-dialogue-*-exec.jar
```

### Frontend

```bash
cd web && npm install && npm run dev
```

## 5. Acesso

| Serviço | Endereço |
|------|------|
| Frontend | http://localhost:8084 |
| API do painel administrativo | http://localhost:8091 |
| WebSocket | ws://localhost:8092/ws/xiaozhi/v1/ |

Administrador padrão: admin / 123456

## Problemas Comuns

| Problema | Solução |
|------|------|
| Conflito de portas | Altere `server.port` em `xiaozhi-server\src\main\resources\application.yml` |
| Falha na conexão com o MySQL | Confirme que o serviço MySQL está em execução (verifique no gerenciador de serviços) |
| Falha no build | Execute `mvn clean install` e confirme que a rede tem acesso ao repositório central do Maven |
