FROM node:20-alpine AS build
WORKDIR /app
ENV NODE_OPTIONS=--max-old-space-size=512
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
COPY docker-entrypoint.d/40-runtime-config.sh /docker-entrypoint.d/40-runtime-config.sh
RUN chmod +x /docker-entrypoint.d/40-runtime-config.sh
ENV VITE_API_URL=http://localhost:8080
ENV VITE_WS_URL=http://localhost:8080/ws
ENV VITE_APP_BUILD_VERSION=0.0.1-SNAPSHOT
ENV VITE_APP_BUILD_COMMIT=local-dev
ENV VITE_APP_BUILD_TIME=untracked
EXPOSE 80
