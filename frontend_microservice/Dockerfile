FROM node:20.16.0-slim

WORKDIR /app

COPY ./app /app

RUN npm install

EXPOSE 3000

CMD ["npm", "run", "dev"]

# For production

# Build the application
# RUN npm run build

# Start the Next.js server in production mode
# CMD ["npm", "start"]


