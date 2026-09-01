# docker build -t worker-counter:v1 .
FROM alpine:3.19

# Install any dependencies you need (e.g., curl, python, etc.)
RUN apk add --no-cache bash

# Copy your script or executable into the image
COPY worker.sh /usr/local/bin/worker.sh
RUN chmod +x /usr/local/bin/worker.sh

# Run the script when the container starts
ENTRYPOINT ["/usr/local/bin/worker.sh"]