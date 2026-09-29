#!/usr/bin/env python3
import json
import os
import time
import traceback
from datetime import datetime, timezone
from kafka import KafkaProducer

KAFKA_TOPIC = "task-status-events"
producer = None
task_ref_id = None

def format_error(error):
    worker_frames = traceback.extract_tb(error.__traceback__)
    stack = " ".join(
        f'File "{frame.filename}", line {frame.lineno}, in {frame.name}: '
        f"{frame.line.strip() if frame.line else ''}"
        for frame in worker_frames
        if frame.filename == __file__
    )
    return f"{stack} {type(error).__name__}: {error}"

def send_status(status, error_message=None):
    event = {"taskReferenceId": task_ref_id, "status": status, "errorMessage": error_message}
    producer.send(KAFKA_TOPIC, value=event)
    producer.flush()
    print(f"Sent event: {event}")

def main():
    global producer, task_ref_id

    # setup kafka first, so we can send error if global try/catch hook fails
    task_ref_id = os.environ["TASK_REFERENCE_ID"]
    kafka_bootstrap_servers = os.environ.get("KAFKA_BOOTSTRAP_SERVERS")
    producer = KafkaProducer(
        bootstrap_servers=kafka_bootstrap_servers,
        value_serializer=lambda value: json.dumps(value).encode("utf-8"),
    )

    # parse data
    payload = json.loads(os.environ.get("TASK_PAYLOAD"))
    duration = int(payload.get("durationSeconds", 60))

    print(f"Worker started for task {task_ref_id}. Counting for {duration} seconds...")
    send_status("RUNNING")
    for count in range(1, duration + 1):
        now = datetime.now(timezone.utc).strftime("%H:%M:%S")
        print(f"[{now}] Current Count: {count}")
        time.sleep(1)

    print(f"{duration} seconds are up! Final count was: {duration}")
    send_status("COMPLETED")


if __name__ == "__main__":
    try:
        print("Worker started")
        main()
    except Exception as error:
        error_message = format_error(error)
        print(f"Worker failed: {error_message}")

        # A failure before producer or TASK_ID initialization cannot be reported to Kafka.
        if producer is not None and task_ref_id is not None:
            try:
                send_status("FAILED", "worker failed: " + error_message)
            except Exception as kafka_error:
                print(f"Could not send FAILED status to Kafka: {kafka_error}")

        raise