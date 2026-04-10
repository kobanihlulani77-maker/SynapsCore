@echo off
setlocal

set SPRING_PROFILES_ACTIVE=local
set SERVER_PORT=8080
set SYNAPSECORE_BUILD_VERSION=0.0.1-local-demo
set SYNAPSECORE_BUILD_COMMIT=local-demo-run
set SYNAPSECORE_BUILD_TIME=2026-04-08T15:45:00Z

call mvnw.cmd spring-boot:run
