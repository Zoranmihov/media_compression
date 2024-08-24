FROM tiangolo/uvicorn-gunicorn-fastapi:python3.7

WORKDIR /app

COPY requirements.txt .

RUN apt-get update && apt-get install -y ffmpeg cron

RUN pip install --no-cache-dir -r requirements.txt

COPY ./app /app

# Add the crontab file to the cron.d directory
COPY ./bash_scripts/crontab /etc/cron.d/cleanup-cron

# Add the script to clean up the shared directory
COPY ./bash_scripts/cleanup_shared_data.sh /usr/local/bin/cleanup_shared_data.sh

# Give execution rights on the cleanup script
RUN chmod +x /usr/local/bin/cleanup_shared_data.sh

# Apply cron job
RUN crontab /etc/cron.d/cleanup-cron

# Create the log file to be able to run tail
RUN touch /var/log/cron.log

EXPOSE 8082

## Remove reload for production

CMD ["sh", "-c", "cron && tail -f /var/log/cron.log & uvicorn main:app --reload --host 0.0.0.0 --port 8082"]
