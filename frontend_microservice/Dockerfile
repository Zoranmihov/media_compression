FROM node:20.16.0-slim

WORKDIR /app

# COPY ./app/package*.json ./

# RUN npm install

COPY ./app /app

EXPOSE 3000

CMD ["sh", "-c", "npm install && npm run dev"]

# For production

# Build the application
# RUN npm run build

# Start the Next.js server in production mode
# CMD ["npm", "start"]
