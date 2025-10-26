#!/bin/bash

SERVER_HOST="localhost"
SERVER_PORT=8000
CLIENT_NAME="${1:-client1}"
DELAY_SECONDS="${2:-0}"
EXIT_BEFORE_READING="${3:-false}"

java -cp build:lib/bouncycastle/* Main client "$SERVER_HOST" "$SERVER_PORT" "$CLIENT_NAME" "$DELAY_SECONDS" "$EXIT_BEFORE_READING"