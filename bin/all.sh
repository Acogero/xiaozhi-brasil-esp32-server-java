#!/usr/bin/env bash
# =============================================================================
# Script de gerenciamento de todos os serviços (server + dialogue)
# Uso: bin/all.sh <start|stop|restart|status>
# =============================================================================
source "$(cd "$(dirname "$0")" && pwd)/_common.sh"

case "${1:-}" in
  start)
    build all
    start_service "xiaozhi-server"   "xiaozhi-server"   8091
    start_service "xiaozhi-dialogue" "xiaozhi-dialogue" 8092
    echo ""
    _ok "Todos os serviços foram iniciados"
    ;;
  stop)
    stop_service "xiaozhi-server"
    stop_service "xiaozhi-dialogue"
    _ok "Todos os serviços foram parados"
    ;;
  restart)
    stop_service "xiaozhi-server"
    stop_service "xiaozhi-dialogue"
    sleep 1
    build all
    start_service "xiaozhi-server"   "xiaozhi-server"   8091
    start_service "xiaozhi-dialogue" "xiaozhi-dialogue" 8092
    echo ""
    _ok "Todos os serviços foram reiniciados"
    ;;
  status)
    echo ""
    status_service "xiaozhi-server"   8091
    status_service "xiaozhi-dialogue" 8092
    echo ""
    ;;
  *)
    echo -e "Uso: ${BOLD}bin/all.sh${NC} <start|stop|restart|status>"
    exit 1
    ;;
esac
