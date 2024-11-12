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

- A k8s cluster with Dapr installed
- kubectl
- Helm
- Docker
- Maven

## Setup Steps

### 1. Install Apache Pulsar in your cluster

```bash
# Add Pulsar Helm repository
helm repo add apache https://pulsar.apache.org/charts
helm repo update

# Create namespace for Pulsar
kubectl create namespace pulsar

# Install Pulsar
helm install pulsar apache/pulsar \
  --timeout 10m
  --namespace pulsar \
  --set components.functions=false \
  --set monitoring.prometheus=false \
  --set monitoring.grafana=false \
  --set monitoring.node_exporter=false
```

### 2. Create Application Namespace

```bash
kubectl create namespace pulsar-test
```

### 3. Build Applications

#### Publisher

```bash
cd publisher
mvn clean package

cd ../subscriber
mvn clean package
```

### 4. Create Docker Images

#### Publisher Dockerfile

#### Build Images

```bash
docker buildx create --use

# Build publisher
cd publisher
mvn clean package
docker buildx build --platform linux/amd64 -t <your-registry>/publisher:1.0 --push .

# Build subscriber
cd ../subscriber
mvn clean package
docker buildx build --platform linux/amd64 -t <your-registry>/subscriber:1.0 --push .
```

> [!IMPORTANT]
> Replace `<your-registry>` with your container registry information, both on the docker build command line and on the app deployment files.

### 5. Deploy to Kubernetes

#### Deploy Applications

```bash
# Apply Dapr component
kubectl apply -f k8s/pulsar-component.yaml

# Deploy publisher and subscriber
kubectl apply -f k8s/publisher-deploy.yaml
kubectl apply -f k8s/subscriber-deploy.yaml
```

### 6. Verify Deployment

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
