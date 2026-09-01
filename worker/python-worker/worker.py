#!/usr/bin/env python3
import json
import os
import time
from datetime import datetime, timezone
from kafka import KafkaProducer

TASK_ID = os.environ["TASK_ID"]
TASK_PAYLOAD = os.environ.get("TASK_PAYLOAD", "{}")
TASK_REFERENCE_ID = os.environ.get("TASK_PAYLOAD", "{}")
KAFKA_BOOTSTRAP_SERVERS = os.environ.get("KAFKA_BOOTSTRAP_SERVERS", "host.docker.internal:9092")
KAFKA_TOPIC = "task-status-events"

payload = json.loads(TASK_PAYLOAD)
duration = int(payload.get("durationSeconds", 60))

producer = KafkaProducer(
    bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
    value_serializer=lambda v: json.dumps(v).encode("utf-8"),
)


def send_status(status, error_message=None):
    event = {"taskId": int(TASK_ID), "status": status, "errorMessage": error_message}
    producer.send(KAFKA_TOPIC, value=event)
    producer.flush()
    print(f"Sent event: {event}")


def main():
    print(f"Worker started for task {TASK_ID}. Counting for {duration} seconds...")
    send_status("RUNNING")

    try:
        for count in range(1, duration + 1):
            now = datetime.now(timezone.utc).strftime("%H:%M:%S")
            print(f"[{now}] Current Count: {count}")
            time.sleep(1)

        print(f"{duration} seconds are up! Final count was: {duration}")
        send_status("COMPLETED")

    except Exception as e:
        print(f"Worker failed: {e}")
        send_status("FAILED", str(e))
        raise


if __name__ == "__main__":
    main()