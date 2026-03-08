#!/bin/bash

# Comprueba si Docker Compose está activo; si no, lo levanta
# Devuelve: 0 = éxito, 1 = error
# Uso: ./check_docker.sh [ruta/al/directorio]

COMPOSE_DIR="${1:-.}"
COMPOSE_FILE="$COMPOSE_DIR/docker-compose.yml"

if ! command -v docker &> /dev/null; then
    echo "❌ Docker no está instalado"
    exit 1
fi

if [ ! -f "$COMPOSE_FILE" ]; then
    echo "❌ No se encontró $COMPOSE_FILE"
    exit 1
fi

RUNNING=$(docker compose -f "$COMPOSE_FILE" ps --status running -q 2>/dev/null)

if [ -z "$RUNNING" ]; then
    echo "⚠️  No hay servicios activos. Levantando Docker Compose..."
    docker compose -f "$COMPOSE_FILE" up -d

    if [ $? -ne 0 ]; then
        echo "❌ Error al levantar los servicios"
        exit 1
    fi

    # Esperar unos segundos para que los servicios estén listos
    echo "⏳ Esperando 5 segundos a que los servicios estén listos..."
    sleep 5

    # Verificar que realmente arrancaron
    RUNNING=$(docker compose -f "$COMPOSE_FILE" ps --status running -q 2>/dev/null)
    if [ -z "$RUNNING" ]; then
        echo "❌ Los servicios no arrancaron correctamente"
        exit 1
    fi

    echo "✅ Servicios levantados correctamente"
else
    echo "✅ Servicios ya activos"
fi

docker compose -f "$COMPOSE_FILE" ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
exit 0
