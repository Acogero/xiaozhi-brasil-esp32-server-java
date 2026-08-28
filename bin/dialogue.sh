#!/usr/bin/env bash
# =============================================================================
# Script de gerenciamento do xiaozhi-dialogue
# Uso: bin/dialogue.sh <start|stop|restart|status>
# =============================================================================
source "$(cd "$(dirname "$0")" && pwd)/_common.sh"

NAME="xiaozhi-dialogue"
MODULE="xiaozhi-dialogue"
PORT=8092

case "${1:-}" in
  start)
    build "$MODULE"
    start_service "$NAME" "$MODULE" "$PORT"
    ;;
  stop)
    stop_service "$NAME"
    ;;
  restart)
    stop_service "$NAME"
    sleep 1
    build "$MODULE"
    start_service "$NAME" "$MODULE" "$PORT"
    ;;
  status)
    status_service "$NAME" "$PORT"
    ;;
  *)
    usage "bin/dialogue.sh"
    exit 1
    ;;
esac
