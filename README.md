# Dapr Pulsar Publishing Demo

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
  --values ./k8s/pulsar-helm-values-minimal.yaml 
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
```

### 5. Create Docker Images

#### Build Images

```bash
# Point to Minikube's Docker daemon
eval $(minikube docker-env)

# Build publisher
cd publisher
mvn clean package
docker build -t publisher:1.0 .

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

#### 6. Deploy Applications

```bash
# Apply Dapr component
kubectl apply -f k8s/pulsar-component.yaml

# Deploy publisher and subscriber
kubectl apply -f k8s/publisher-deploy.yaml
```

### 7. Verify Deployment

```bash
# Check pod status
kubectl get pods -n pulsar-test

# Check publisher logs
kubectl logs -n pulsar-test -l app=publisher -c publisher

# Check Dapr sidecar logs if needed
kubectl logs -n pulsar-test -l app=publisher -c daprd
```