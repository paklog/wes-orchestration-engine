#!/bin/bash
set -e

echo "Starting WES Orchestration Engine..."
echo "Active Profile: ${SPRING_PROFILES_ACTIVE}"
echo "MongoDB URI: ${MONGODB_URI}"
echo "Kafka Bootstrap Servers: ${KAFKA_BOOTSTRAP_SERVERS}"
echo "Redis Host: ${REDIS_HOST}"

# Wait for MongoDB to be ready
echo "Waiting for MongoDB..."
while ! nc -z ${MONGODB_HOST:-mongodb} ${MONGODB_PORT:-27017}; do
  sleep 1
done
echo "MongoDB is ready!"

# Wait for Kafka to be ready
echo "Waiting for Kafka..."
while ! nc -z ${KAFKA_HOST:-kafka} ${KAFKA_PORT:-9092}; do
  sleep 1
done
echo "Kafka is ready!"

# Wait for Redis to be ready
echo "Waiting for Redis..."
while ! nc -z ${REDIS_HOST:-redis} ${REDIS_PORT:-6379}; do
  sleep 1
done
echo "Redis is ready!"

# Start the application
exec java ${JAVA_OPTS} -jar /app/app.jar "$@"