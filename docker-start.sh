#!/bin/bash
echo "HOST_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')" > .env
docker compose up -d