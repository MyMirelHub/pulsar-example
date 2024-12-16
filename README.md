# Dapr Pulsar Pub/Sub Demo

This project demonstrates a Dapr pub/sub implementation using Apache Pulsar as the message broker. It consists of a publisher and subscriber application written in Java Spring Boot, deployed to Kubernetes\

## Project Structure

```
dapr-pulsar-demo/
├── publisher/
│   ├── src/main/java/com/example/Publisher.java
│   ├── pom.xml
│   └── Dockerfile
├── subscriber/
│   ├── src/main/java/com/example/Subscriber.java
│   ├── pom.xml
│   └── Dockerfile
├── components/
│   └── pulsar.yaml
├── docker-compose.yaml
└── Makefile
```

## Prerequisites

- A k8s cluster
- [kubectl](https://kubernetes.io/docs/tasks/tools/#kubectl)
- [Helm](https://helm.sh/docs/intro/install/)
- [Docker](https://docs.docker.com/engine/install/)
- [Maven](https://maven.apache.org/install.html)
- [Dapr CLI](https://docs.dapr.io/getting-started/install-dapr-cli/)

## Setup Steps for Kubernetes Deployment

### 1. Install Dapr

```bash
# Initialize Dapr in your cluster
dapr init -k
```

### 2. Install Apache Pulsar in your cluster

```bash
# Add Pulsar Helm repository
helm repo add apache https://pulsar.apache.org/charts
helm repo update

# Create namespace for Pulsar
kubectl create namespace pulsar

# Install Pulsar
helm install pulsar apache/pulsar \
  --timeout 10m \
  --namespace pulsar \
  --set components.functions=false \
  --set monitoring.prometheus=false \
  --set monitoring.grafana=false \
  --set monitoring.node_exporter=false
```

### 3. Create Application Namespace

```bash
kubectl create namespace pulsar-test
```

### 4. Build Applications

#### Publisher

```bash
cd publisher
mvn clean package

cd ../subscriber
mvn clean package
```

### 4. Create Docker Images

#### Publisher Dockerfile

```dockerfile
FROM --platform=linux/amd64 openjdk:11-jre-slim
WORKDIR /app
COPY target/dapr-pulsar-publisher-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Build Images

```bash
# Point to Minikube's Docker daemon
eval $(minikube docker-env)

# Build publisher
cd publisher
mvn clean package
docker build --platform linux/amd64 -t publisher:1.0 .

# Build subscriber
cd ../subscriber
mvn clean package
docker buildx build --platform linux/amd64 -t <your-registry>/subscriber:1.0 --push .
```

### 5. Deploy to Kubernetes

#### Dapr Component Configuration

```yaml
# k8s/pulsar-component.yaml
apiVersion: dapr.io/v1alpha1
kind: Component
metadata:
  name: pulsar-pubsub
  namespace: pulsar-test
spec:
  type: pubsub.pulsar
  version: v1
  metadata:
  - name: host
    value: "pulsar://pulsar-proxy.pulsar.svc.cluster.local:6650"
  - name: tenant
    value: "public"
  - name: namespace
    value: "default"
```

#### Deploy Applications

```bash
# Apply Dapr component
kubectl apply -f k8s/pulsar-component.yaml

# Deploy publisher and subscriber
kubectl apply -f k8s/publisher-deploy.yaml
kubectl apply -f k8s/subscriber-deploy.yaml
```

### 7. Verify Deployment

```bash
# Check pod status
kubectl get pods -n pulsar-test

# Check publisher logs
kubectl logs -n pulsar-test -l app=publisher -c publisher

# Check subscriber logs
kubectl logs -n pulsar-test -l app=subscriber -c subscriber

# Check Dapr sidecar logs if needed
kubectl logs -n pulsar-test -l app=publisher -c daprd
kubectl logs -n pulsar-test -l app=subscriber -c daprd
```

## Troubleshooting

1. If pods fail to start, check the events:

```bash
kubectl describe pod -n pulsar-test -l app=publisher
kubectl describe pod -n pulsar-test -l app=subscriber
```

2. Verify Pulsar component is loaded:

```bash
kubectl get components -n pulsar-test
```

3. For architecture-related Docker issues, ensure platform is specified in build:

```bash
docker build --platform linux/amd64 -t <image-name>:1.0 .
```

## Port Configuration

- Subscriber runs on port 8082
- Pulsar broker runs on port 6650
- Make sure these ports are available in your cluster

## Running Options

### 1. Run using docker-compose

#### Setup Pulsar component host

Edit `./components/pulsar-component.yaml` to use docker-compose host option:

```yaml
- name: host
  value: "pulsar://pulsar:6650"
```

#### Build the Java Applications

```bash
# Build publisher
cd publisher
mvn clean package

# Build subscriber
cd ../subscriber
mvn clean package
cd ..
```

### Start the Applications

First, clean up any existing containers to prevent caching issues:

```bash
# Remove existing containers
docker rm -f $(docker ps -aq)

# Remove existing images
docker rmi $(docker images -q 'pulsar-example-publisher' -a)
docker rmi $(docker images -q 'pulsar-example-subscriber' -a)
```

Then start everything:

```bash
docker compose up -d --build
```

### Stop Everything

```bash
docker compose down
```

> ⚠️ **Important Note**: When starting the applications, the subscriber's Dapr sidecar might fail to subscribe initially because the topic doesn't exist yet when Pulsar is starting up. If you don't see messages being received, restart the subscriber containers:
> ```bash
> docker compose restart subscriber subscriber-dapr
> ```


### 2. Run using Make

Edit `./components/pulsar-component.yaml` to use docker-compose host option:

```yaml
- name: host
  value: "pulsar://pulsar:6650"
```

Then run:

```bash
# Clean up existing containers and build everything fresh
make apps-up

# Stop everything
make apps-down
```

> ⚠️ **Important Note**: When starting everything with `apps-up`, the subscriber's Dapr sidecar might fail to subscribe initially because the topic doesn't exist yet when Pulsar is starting up. If you don't see messages being received, restart the subscriber container.
> This gives Pulsar enough time to be fully operational and the topic to be created by the publisher.

### 3. Run Infrastructure in Docker and Apps Locally (using `dapr run`)

Run Pulsar as a Docker container:

```bash
docker run -d -it \
  --name pulsar \
  -p 6650:6650 \
  -p 8080:8080 \
  apachepulsar/pulsar:latest \
  bin/pulsar standalone
```

Edit `./components/pulsar-component.yaml` to use docker-compose host option:

```yaml
- name: host
  value: "localhost:6650"
```

```bash
# Start publisher
make dapr-run-publisher

# In one terminal, run the subscriber
make dapr-run-subscriber
```

### 4. Clean Up Everything

```bash
make clean
```

### Available Make Commands

```bash
make build                # Build Java applications
make apps-up              # Start everything including Java apps
make apps-down            # Stop everything
make dapr-run-publisher   # Run Publisher using dapr run
make dapr-run-subscriber  # Run Subscriber using dapr run
make clean                # Clean up everything
```

## Important Notes

- The infrastructure setup includes:
  - Apache Pulsar
  - Dapr sidecars
  - Placement service
- Port configurations:
  - Publisher app: 8890
  - Subscriber app: 8082
  - Pulsar broker: 6650
  - Pulsar web: 8080
  - Publisher Dapr sidecar: 50001 (gRPC), 3500 (HTTP)
  - Subscriber Dapr sidecar: 50002 (gRPC), 3501 (HTTP)
