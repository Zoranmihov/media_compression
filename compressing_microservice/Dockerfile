FROM tiangolo/uvicorn-gunicorn-fastapi:python3.7

# Set the working directory
WORKDIR /app

# Copy the requirements.txt file to the working directory
COPY requirements.txt .

# Install the dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy the application code to the working directory
COPY ./app /app

# Expose the port that the app will run on
EXPOSE 8082

# Run the application
CMD ["uvicorn", "main:app", "--reload", "--host", "0.0.0.0", "--port", "8082"]
