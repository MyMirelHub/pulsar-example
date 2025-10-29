# Dapr Pulsar OAuth2 Token Renewal Bug Reproduction

This repository contains a minimal reproduction case for the OAuth2 token renewal bug in Dapr's Pulsar component.

## Bug Description

When using OAuth2 client_credentials authentication with the Pulsar component, messages publish successfully for ~60 seconds (token lifetime), then fail permanently. Dapr never renews the expired OAuth2 token.

## Prerequisites

- Kubernetes cluster (kind, minikube, or cloud provider)
- [kubectl](https://kubernetes.io/docs/tasks/tools/#kubectl)
- [Helm](https://helm.sh/docs/intro/install/)
- [Docker](https://docs.docker.com/engine/install/)
- [Maven](https://maven.apache.org/install.html)
- [Dapr CLI](https://docs.dapr.io/getting-started/install-dapr-cli/)

## Setup Steps

### 1. Install Dapr
```bash
dapr init -k
```

### 2. Install Apache Pulsar
```bash
# Add Pulsar Helm repository
helm repo add apache https://pulsar.apache.org/charts
helm repo update

# Create namespace for Pulsar
kubectl create namespace pulsar

# Install Pulsar with OAuth2 authentication
helm install pulsar apache/pulsar \
  --timeout 10m \
  --namespace pulsar \
  --values ./k8s/pulsar-helm-values-minimal.yaml
```

### 3. Create Test Namespace
```bash
kubectl create namespace pulsar-test
```

### 4. Deploy OAuth2 Mock Server
```bash
# Deploy mock OAuth2 server that issues 60-second tokens
kubectl apply -f k8s/oauth-mock.yaml

# Wait for OAuth server to be ready
kubectl wait --for=condition=ready pod -l app=mock-oauth2 -n pulsar-test --timeout=60s
```

### 5. Deploy Dapr Pulsar Component
```bash
# Deploy Pulsar component with OAuth2 configuration
kubectl apply -f k8s/pulsar-component.yaml
```

### 6 Deploy Publisher Application
```bash
# Deploy publisher
cd ..
kubectl apply -f k8s/publisher-deploy.yaml

# Wait for publisher to be ready
kubectl wait --for=condition=ready pod -l app=publisher -n pulsar-test --timeout=120s
```

## Reproduce the Bug

### Watch Publisher Logs
```bash
kubectl logs -f -n pulsar-test -l app=publisher -c publisher
```

**Observed behavior:**
```
[08:53:08] ✓ Message 1 sent (0.9s)
[08:53:10] ✓ Message 2 sent (0.0s)
...
[08:55:05] ✓ Message 59 sent (0.0s)
[08:55:07] ✓ Message 60 sent (0.0s)
[08:55:39] ✗ Message 61 FAILED (30.1s): INTERNAL: message send timeout
[08:56:11] ✗ Message 62 FAILED (30.0s): INTERNAL: message send timeout
```

### Verify the Root Cause

Check OAuth server logs to confirm Dapr never renews the token:
```bash
kubectl logs -n pulsar-test -l app=mock-oauth2
```

**You'll see:**
```
2025-10-29 08:53:06 DEBUG handle token request (Dapr initial fetch)
2025-10-29 08:53:08 DEBUG handle token request (Pulsar broker initial fetch)
2025-10-29 08:55:08 DEBUG handle token request (Pulsar broker renewal ✓)
2025-10-29 08:57:08 DEBUG handle token request (Pulsar broker renewal ✓)

# NO RENEWAL FROM DAPR AFTER 08:53:06!
```

Check Pulsar broker logs to see connection closure:
```bash
kubectl logs -n pulsar pulsar-broker-0 | grep -A2 "Refreshing authentication"
```

**You'll see:**
```
08:55:07 INFO [/10.244.0.138:45312] Refreshing authentication credentials
...
08:56:07 WARN [/10.244.0.138:45312] Closing connection after timeout on refreshing auth credentials
```

## What's Happening

1. **08:53:06** - Dapr fetches initial OAuth2 token (60-second expiry)
2. **08:53:08 - 08:55:07** - Messages publish successfully for ~2 minutes
3. **08:55:07** - Pulsar broker requests auth refresh (token expired)
4. **08:55:07 - 08:56:07** - Dapr never renews the token
5. **08:56:07** - Broker closes connection due to auth timeout
6. **08:56:08+** - All subsequent messages fail permanently

**Evidence:**
- OAuth server logs show Pulsar broker successfully renews at 08:55:08 and 08:57:08
- OAuth server logs show **ZERO renewal attempts from Dapr client**
- This proves Dapr's OAuth token renewal is completely broken