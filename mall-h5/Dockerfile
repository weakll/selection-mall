FROM node:22-bookworm-slim AS build
WORKDIR /app
COPY mall-h5/package.json mall-h5/pnpm-lock.yaml ./
RUN npm install --global pnpm@10.15.1 && pnpm install --frozen-lockfile --ignore-scripts && pnpm rebuild esbuild vue-demi
COPY mall-h5/ ./
RUN pnpm build

FROM nginx:1.27-alpine
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
