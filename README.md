
```
# !!important!! in devlopment for kafka to to talk to kubernetes, we need to kubernetes host ip
# docker-compose and spring need to read this, so start docker compose like this
# we can use the value in docker-compose and it will create an .env file with the value we can read in spring
./docker.starth.sh

#  Start rancher and make sure you are docker client is connecting to the rancher context
rancher-destkop
docker context ls
docker context use rancher-desktop

If you 2 docker deamons running, make sure you remove stop any dev containers from default
They will both run at localhost:5432 and your app might connect to the wrong one
docker context use default
docker-compose down -v
docker context use rancher-desktop
docker-compose down -v
docker-comnpose up -d

# create worker image
cd worker/python-worker
docker build -t worker-counter:v1 .
docker images | grep worker-counter

# start app through gradle
./gradlew bootRun --args='--spring.profiles.active=dev'

# rest api documentation:
http://localhost:8080/swagger-ui/index.html

# kafka -ui
http://localhost:8081/

# test the worker in kubectl
kubectl get po
kubectl get jops
kubectl delete jobs <name>
kubectl delete jobs --all
```

````
# communication between docker-compose (kafka service) and kubernetes
The kubernetes cluster runs in differnet network then docker-compose Bridge network

They can both connect to the host, eg:
docker exec -ti zookeeper ping host.docker.internal
kubectl run debug-shell --image=worker-counter:v2 --image-pull-policy=Never --restart=Never -ti -- ping host.docker.internal
But not to each other, to fix this we need to do 

# 1) On the host machine, find the gateway IP the pod network uses, check for INTERNAL-IP
kubectl get nodes -o wide # e.g 192.168.5.15

# 2) in docker-compose kafka service, add
- added extra listerner and additional port
PODNET://192.168.5.15:9093
ports:
   - "9093:9093"

#3 ExecutionService:
needs to pass the follwoing env var to kubernetes pod
KAFKA_BOOTSTRAP_SERVERS=192.168.5.15:9093

# so now they can communicate like:
kubernetes worker pod 
   ↓ 
host / Lima VM (192.168.5.15)
   ↓ Docker's published port mapping (9093:9093) forwards in to
kafka container (Docker bridge network, e.g. 172.x.x.x internally)

Linux host (Rancher Desktop)                                        
                                                                     
                    ┌─────────────────────┐                          
                    │  Spring Boot app    │                         
                    │  (runs on host)     │                         
                    └──────────┬──────────┘                         
                 kubeconfig    │    localhost:port                   
                 ┌─────────────┘    └─────────────┐                  
                 ▼                                ▼                 
  ┌─────────────────────────────┐    ┌─────────────────────────────┐ 
  │ Kubernetes cluster (k3s)    │    │ Docker (compose)            │ 
  │ Pod network                 │    │ Bridge network              │ 
  │                             │    │                             │ 
  │  ┌─────────────────────┐    │    │  ┌─────────────────────┐    │ 
  │  │ Worker pods         │    │    │  │ Kafka + Zookeeper   │    │ 
  │  │ Python script,      │───────▶ │  │                     │    │ 
  │  │ task env vars       │  host   │  └─────────────────────┘    │ 
  │  │                     │  gateway│  ┌─────────────────────┐    │ 
  │  │                     │  ip:9093│  │ Kafka UI            │    │ 
  │  └─────────────────────┘    │    │  └─────────────────────┘    │ 
  │                             │    │  ┌─────────────────────┐    │ 
  │                             │    │  │ Postgres            │    │ 
  │                             │    │  └─────────────────────┘    │ 
  └─────────────────────────────┘    └─────────────────────────────┘ 

Key connections:
Spring Boot app → Kubernetes cluster: via kubeconfig (creates Jobs)
Spring Boot app → Docker containers: via localhost:published-port (Kafka, Postgres, Kafka UI)
Worker pods → Kafka: via host gateway IP on a dedicated PODNET listener port (9093)
````

````
Production Setup with Managed Kafka (e.g. AWS MSK / Confluent Cloud)

Production Setup with Managed Kafka (e.g. AWS MSK / Confluent Cloud)

  ┌─────────────────────────────────────────────┐
  │            Kubernetes Cluster (VPC)         │
  │                                             │
  │   ┌──────────────────────────┐              │
  │   │  Spring Boot app         │              │
  │   │  (Deployment)            │              │
  │   │  ServiceAccount + RBAC   │              │
  │   └───────┬──────────────────┘              │
  │           │                                 │
  │           │ in-cluster K8s API              │
  │           ▼                                 │
  │   ┌─────────────────┐                       │
  │   │  Worker Job Pods │                      │
  │   │  (Python script) │                      │
  │   │  env:            │                      │
  │   │  KAFKA_BOOTSTRAP │                      │
  │   └────────┬─────────┘                      │
  │            │                                │
  └────────────┼────────────────────────────────┘
               │
               │ TLS + SASL/IAM
               │ bootstrap-servers
               │ (used by BOTH Spring Boot app
               │  and Worker pods)
               ▼
  ┌─────────────────────────────────────────────┐
  │          Managed Kafka Service              │
  │          (AWS MSK / Confluent Cloud)        │
  │                                             │
  │          broker-1  broker-2  broker-3       │
  │          (fully managed, same VPC           │
  │           or connected via PrivateLink)     │
  └─────────────────────────────────────────────┘

Key connections:
- Spring Boot app -> K8s API: in-cluster ServiceAccount (RBAC)
- Spring Boot app -> Kafka: bootstrap-servers endpoint, TLS + SASL/IAM auth
- Worker pods -> Kafka: same bootstrap-servers endpoint, same auth
- Kafka cluster: fully managed, no listener config needed on your side

```
