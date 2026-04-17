@echo off
setlocal

set SPRING_PROFILES_ACTIVE=prod
set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=synapsecore
set DB_USER=postgres
set DB_PASSWORD=postgres
set SPRING_DATA_REDIS_URL=redis://localhost:6379
set CORS_ALLOWED_ORIGINS=http://localhost,http://localhost:5173
set SESSION_COOKIE_SECURE=false
set SESSION_COOKIE_SAME_SITE=Lax
set ALLOW_HEADER_FALLBACK=false
set SPRING_JPA_HIBERNATE_DDL_AUTO=update
set SYNAPSECORE_BUILD_VERSION=0.0.1-localhost
set SYNAPSECORE_BUILD_COMMIT=local-host-run
set SYNAPSECORE_BUILD_TIME=2026-04-08T15:30:00Z

call mvnw.cmd spring-boot:run
