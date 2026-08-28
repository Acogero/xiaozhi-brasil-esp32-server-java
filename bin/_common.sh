#!/usr/bin/env bash
# =============================================================================
# Biblioteca de funções comuns, referenciada por server.sh / dialogue.sh / all.sh, não executada diretamente
# =============================================================================

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOGS_DIR="$ROOT_DIR/logs"

# ---- Cores ----
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BLUE='\033[0;34m'; BOLD='\033[1m'; NC='\033[0m'

_log()  { echo -e "${GREEN}[xiaozhi]${NC} $*"; }
_info() { echo -e "${CYAN}[xiaozhi]${NC} $*"; }
_warn() { echo -e "${YELLOW}[xiaozhi]${NC} $*"; }
_err()  { echo -e "${RED}[xiaozhi]${NC} $*" >&2; }
_ok()   { echo -e "${GREEN}[xiaozhi]${NC} ${BOLD}$*${NC}"; }

# ---- Compilação ----
# build <module>  — compila apenas esse módulo e suas dependências
# build all       — compila tudo
build() {
  local target="${1:-all}"
  if [[ "$target" == "all" ]]; then
    _info "Compilando todos os módulos..."
    mvn clean install -DskipTests -q -f "$ROOT_DIR/pom.xml"
  else
    _info "Compilando $target e suas dependências..."
    mvn clean install -DskipTests -q -f "$ROOT_DIR/pom.xml" \
        -pl "$target" --also-make
  fi
  _log "Compilação concluída"
}

# ---- Localizar jar ----
# xiaozhi-dialogue usa classifier=exec, gerando *-exec.jar; os demais módulos usam o jar comum
find_jar() {
  local module="$1"
  if [[ "$module" == "xiaozhi-dialogue" ]]; then
    ls "$ROOT_DIR/$module/target/$module"-*-exec.jar 2>/dev/null | head -1
  else
    ls "$ROOT_DIR/$module/target/$module"-*.jar 2>/dev/null \
      | grep -v 'original' | grep -v '\-exec\.jar' | head -1
  fi
}

# ---- Caminho do arquivo PID ----
pid_file() {
  echo "$LOGS_DIR/$1.pid"
}

# ---- Verificar se o processo está ativo ----
is_running() {
  local pid_path
  pid_path="$(pid_file "$1")"
  [[ -f "$pid_path" ]] && kill -0 "$(cat "$pid_path")" 2>/dev/null
}

# ---- Iniciar um único serviço ----
# start_service <name> <module> <port> [label_color]
start_service() {
  local name="$1" module="$2" port="$3" color="${4:-$CYAN}"

  if is_running "$name"; then
    _warn "$name já está em execução (pid=$(cat "$(pid_file "$name")"))"
    return 0
  fi

  local jar
  jar="$(find_jar "$module")"
  if [[ -z "$jar" ]]; then
    _err "O jar de $module não existe, compile primeiro"; return 1
  fi

  _info "Iniciando $name (porta $port)..."

  nohup java \
    -Djava.library.path="$ROOT_DIR/lib" \
    -jar "$jar" \
    > /dev/null 2>&1 &

  echo $! > "$(pid_file "$name")"
  _ok "$name iniciado  pid=$!  log: logs/$name.log"
}

# ---- Parar um único serviço ----
stop_service() {
  local name="$1"
  local pid_path
  pid_path="$(pid_file "$name")"

  if ! is_running "$name"; then
    _warn "$name não está em execução"
    return 0
  fi

  local pid
  pid="$(cat "$pid_path")"
  _info "Parando $name (pid=$pid)..."
  kill "$pid"

  # Aguarda no máximo 15 segundos
  local i=0
  while kill -0 "$pid" 2>/dev/null && (( i < 15 )); do
    sleep 1; (( i++ ))
  done

  if kill -0 "$pid" 2>/dev/null; then
    _warn "Não foi possível encerrar normalmente, forçando o encerramento..."
    kill -9 "$pid" 2>/dev/null || true
  fi

  rm -f "$pid_path"
  _ok "$name parado"
}

# ---- Verificar status ----
status_service() {
  local name="$1" port="$2"
  if is_running "$name"; then
    local pid
    pid="$(cat "$(pid_file "$name")")"
    echo -e "  ${GREEN}●${NC} ${BOLD}$name${NC}  pid=$pid  port=$port  log: logs/$name.log"
  else
    echo -e "  ${RED}○${NC} ${BOLD}$name${NC}  não está em execução"
  fi
}

# ---- Reiniciar ----
restart_service() {
  local name="$1" module="$2" port="$3"
  stop_service  "$name"
  sleep 1
  start_service "$name" "$module" "$port"
}

# ---- Dica de uso ----
usage() {
  local script="$1"
  echo -e "Uso: ${BOLD}$script${NC} <start|stop|restart|status>"
  echo "  start    compila e inicia"
  echo "  stop     para"
  echo "  restart  para, recompila e inicia novamente"
  echo "  status   mostra o status de execução"
}
