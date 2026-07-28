#!/usr/bin/env bash

# Helper script for managing TagSpotter Web (WASM) dev server

PORT=8080

case "$1" in
  status)
    echo "=== Web Server Status (Port $PORT) ==="
    PID=$(lsof -ti:$PORT 2>/dev/null)
    if [ -n "$PID" ]; then
      echo "🟢 Server is RUNNING on http://localhost:$PORT (PID $PID)"
      lsof -i:$PORT
    else
      echo "🔴 No server running on port $PORT."
    fi
    ;;

  stop)
    echo "Stopping any server on port $PORT..."
    lsof -ti:$PORT | xargs kill -9 2>/dev/null || true
    ./gradlew --stop 2>/dev/null || true
    echo "✅ Port $PORT and Gradle daemons cleared."
    ;;

  restart|start|"")
    echo "Ensuring port $PORT is clear before starting..."
    lsof -ti:$PORT | xargs kill -9 2>/dev/null || true
    echo "🚀 Starting TagSpotter Web (WASM) dev server on http://localhost:$PORT ..."
    ./gradlew :webApp:wasmJsBrowserDevelopmentRun
    ;;

  *)
    echo "Usage: ./scripts/web.sh {start|stop|status|restart}"
    exit 1
    ;;
esac
