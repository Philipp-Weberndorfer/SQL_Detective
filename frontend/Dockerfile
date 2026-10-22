# Zweistufiger Build: bauen mit Node, ausliefern mit nginx.
# nginx liefert die fertigen Dateien aus und leitet /api an das Backend weiter.

FROM node:22-alpine AS build
WORKDIR /build

# Erst nur die Paketlisten, damit npm ci bei Codeaenderungen gecacht bleibt.
COPY package.json package-lock.json ./
RUN npm ci --no-fund --no-audit

COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html

EXPOSE 80
