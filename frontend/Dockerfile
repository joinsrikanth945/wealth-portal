# Angular 22 single-page app: build with Node, serve the static files with Nginx.
FROM node:24-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration production

FROM nginx:1.29-alpine
# Where /api is forwarded; Azure overrides it with http://wealth-api.
ENV API_UPSTREAM=http://backend:8080
# The Nginx image turns templates into /etc/nginx/conf.d/*.conf at start-up.
RUN rm -f /etc/nginx/conf.d/default.conf
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
# Writes app-config.json from KEYCLOAK_URL at start-up (scripts here run before Nginx).
COPY docker/40-app-config.sh /docker-entrypoint.d/40-app-config.sh
RUN sed -i 's/\r$//' /docker-entrypoint.d/40-app-config.sh && chmod +x /docker-entrypoint.d/40-app-config.sh
COPY --from=build /src/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
