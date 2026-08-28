# Guia de Implantação com Docker

## Pré-requisitos

- [Docker](https://docs.docker.com/get-docker/) + [Docker Compose](https://docs.docker.com/compose/install/)

| Porta | Serviço |
|------|------|
| 3306 | MySQL |
| 8084 | Frontend |
| 8091 | API do painel administrativo (xiaozhi-server) |
| 8092 | Serviço de diálogo (xiaozhi-dialogue) |

## Início Rápido

```bash
git clone https://github.com/joey-zhou/xiaozhi-esp32-server-java/
cd xiaozhi-esp32-server-java
docker-compose up -d
```

Inicia 5 serviços: MySQL, Redis, frontend Node, backend Server e serviço de diálogo Dialogue.

| Serviço | Endereço |
|------|------|
| Interface do frontend | http://localhost:8084 |
| API do painel administrativo | http://localhost:8091 |
| WebSocket | ws://IP_DO_HOST:8092/ws/xiaozhi/v1/ |

Administrador padrão: admin / 123456

> Ao conectar o dispositivo ESP32, use o IP real do host, não use localhost.

## Modelos e Bibliotecas Nativas

O build do Docker baixa automaticamente:
- **Bibliotecas nativas** — sherpa-onnx JNI + onnxruntime + Vosk (linux-x64)
- **Modelo VAD** — silero_vad.onnx
- **Modelo STT** — modelo Vosk em chinês
- **Modelo TTS** — vits-melo ou matcha

### Download Prévio (opcional, acelera o build)

Se a rede estiver lenta, baixe antecipadamente — o build do Docker pula automaticamente os arquivos já existentes:

```bash
./scripts/download_models.sh all       # baixa todos os modelos e bibliotecas nativas
./scripts/download_models.sh status    # verifica o status
```

Cada módulo também pode ser baixado individualmente:

```bash
./scripts/download_base.sh             # modelo VAD + bibliotecas nativas
./scripts/download_stt.sh              # modelo STT
./scripts/download_tts.sh              # modelo TTS
```

## Variáveis de Ambiente

| Variável | Valor Padrão | Descrição |
|------|--------|------|
| `VOSK_MODEL_SIZE` | `small` | Modelo Vosk; pode usar `standard` (~1.3GB, maior precisão) |
| `TTS_MODEL` | `vits-melo-tts-zh_en` | Modelo TTS; defina como `none` para pular o download |

```bash
VOSK_MODEL_SIZE=standard docker-compose up -d
```

## Dados Persistentes

| Nome do Volume | Descrição |
|------|------|
| `mysql_data` | Dados do MySQL |
| `redis_data` | Dados do Redis |

## Requisitos do Sistema

| Configuração | CPU | Memória | Armazenamento | Descrição |
|------|-----|------|------|------|
| Mínima | 2 núcleos | 2GB | 10GB | Requer API de STT/TTS de terceiros |
| Recomendada | 2 núcleos | 4GB | 20GB | Modelos locais pequenos |
| Completa | 4 núcleos | 8GB | 30GB | Modelos locais grandes |

## Comandos Úteis

```bash
docker-compose logs -f server      # verificar logs do backend
docker-compose logs -f dialogue    # verificar logs do serviço de diálogo
docker-compose ps                  # verificar status dos containers
docker-compose down                # parar
docker-compose down -v             # parar e excluir dados
docker-compose build --no-cache    # reconstruir
```

## Atualização

```bash
git pull
docker-compose build
docker-compose up -d
```

## Solução de Problemas

- **Falha ao iniciar o container**: `docker-compose logs <service_name>` para verificar os logs
- **Problema de conexão com o banco de dados**: `docker-compose ps mysql` para confirmar que o status está healthy
- **Falha na conexão WebSocket**: confirme que está usando o IP do host em vez de localhost, e que a porta 8092 está liberada no firewall