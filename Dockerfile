FROM node:20-alpine
WORKDIR /app
COPY package.json server.js ./
ENV PORT=10000
EXPOSE 10000
CMD ["node", "server.js"]
