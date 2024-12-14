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
└── k8s/
    ├── publisher-deploy.yaml
    ├── subscriber-deploy.yaml
    └── pulsar-component.yaml
```

## Prerequisites

- A k8s cluster
- [kubectl](https://kubernetes.io/docs/tasks/tools/#kubectl)
- [Helm](https://helm.sh/docs/intro/install/)
- [Docker](https://docs.docker.com/engine/install/)
- [Maven](https://maven.apache.org/install.html)
- [Dapr CLI](https://docs.dapr.io/getting-started/install-dapr-cli/)

## Setup Steps

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

## Key Points

- Using Dapr for pub/sub abstraction
- Apache Pulsar as message broker
- Spring Boot applications for publisher and subscriber
- Deployed on Minikube with Dapr sidecars
- Platform-specific Docker builds for compatibility

## Run locally

```bash
dapr run --app-id subscriber \
         --app-port 8082 \
         --resources-path ../components \
         -- java -jar target/dapr-pulsar-subscriber-1.0-SNAPSHOT.jar
```

```bash
dapr run --app-id publisher \
         --resources-path ../components \
         -- java -jar target/dapr-pulsar-publisher-1.0-SNAPSHOT.jar
```


# Try with bash
docker exec -it pulsar-example-subscriber-dapr-1 bash

# If bash doesn't work, try
docker exec -it pulsar-example-subscriber-dapr-1 /bin/busybox sh

# If that doesn't work either, we can see what shell is available:
docker exec pulsar-example-subscriber-dapr-1 ls /bin