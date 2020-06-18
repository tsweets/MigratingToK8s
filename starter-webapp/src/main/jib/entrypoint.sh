#!/bin/sh

echo "Starting Profiles"
exec java ${JAVA_OPTS} -noverify -XX:+AlwaysPreTouch -Djava.security.egd=file:/dev/./urandom -cp /app/resources/:/app/classes/:/app/libs/* "liveproject.m2k8s.Application"  "$@"
