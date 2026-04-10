#!/bin/sh
set -eu

template="/usr/share/nginx/html/runtime-config.template.js"
output="/usr/share/nginx/html/runtime-config.js"
nginx_template="/etc/nginx/conf.d/default.conf.template"
nginx_output="/etc/nginx/conf.d/default.conf"

if [ -f "$template" ]; then
  envsubst '${VITE_API_URL} ${VITE_WS_URL} ${VITE_APP_BUILD_VERSION} ${VITE_APP_BUILD_COMMIT} ${VITE_APP_BUILD_TIME}' < "$template" > "$output"
fi

if [ -f "$nginx_template" ]; then
  PORT="${PORT:-80}"
  envsubst '${PORT}' < "$nginx_template" > "$nginx_output"
fi
