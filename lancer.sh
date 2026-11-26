#!/bin/sh
# Lance l application (serveur + pages) : http://localhost:8080
cd "$(dirname "$0")/backend" && ./mvnw spring-boot:run "$@"
