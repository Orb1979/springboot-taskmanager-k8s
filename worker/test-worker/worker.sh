#!/bin/bash

DURATION=300
START_TIME=$(date +%s)
END_TIME=$((START_TIME + DURATION))

COUNTER=0
echo "Worker started. I will count for 5 minutes..."

while [ $(date +%s) -lt $END_TIME ]; do
    COUNTER=$((COUNTER + 1))

    # Get current timestamp for logging
    CURRENT_TIME=$(date +"%H:%M:%S")

    echo "[$CURRENT_TIME] Current Count: $COUNTER"

    # Wait 1 second before next increment
    sleep 1
done

echo "5 minutes are up! Final count was: $COUNTER. Exiting..."