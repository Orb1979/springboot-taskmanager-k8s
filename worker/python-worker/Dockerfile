# docker build -t worker-counter:v2 .
FROM python:3.12-slim
WORKDIR /app
RUN apt-get update && \
    apt-get install -y --no-install-recommends iputils-ping && \
    rm -rf /var/lib/apt/lists/*
RUN pip install --no-cache-dir kafka-python
COPY worker.py .

# -u run Python unbuffered
# without it stdout defaults to block buffering instead of line buffering.
# This means print() output gets held in memory and only flushed when the buffer fills up or the process exits normally
CMD ["python3", "-u", "worker.py"]