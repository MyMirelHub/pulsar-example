# Dapr Pulsar OAuth2 Client Secret from File Fix

This repository demonstrates the fix for reading OAuth2 client secrets from files in Dapr's Pulsar component.

## Problem

The Pulsar component supports `oauth2ClientSecretPath` to read client secrets from files, but the implementation was missing. This fix adds support for reading secrets from:
- **Plain text files**: Contains just the secret value
- **JSON files**: Contains `{"client_secret": "value"}` structure

## Quick Start

### Prerequisites
- Minikube with registry addon enabled
- Helm 3.x
- kubectl

### 1. Install Dapr (Custom Build)
```bash
cd ~/dapr
export DAPR_REGISTRY=localhost:60019
export DAPR_TAG=1.0.920
export TARGET_OS=linux
export TARGET_ARCH=arm64
export BUILD_TAG=${DAPR_TAG}-${TARGET_OS}-${TARGET_ARCH}

make modtidy-all
make build-linux
make docker-build
minikube image load localhost:60019/daprd:${BUILD_TAG}
make docker-deploy-k8s PULL_POLICY=Never
```

### 2. Install Hydra OAuth2 Server
```bash
helm repo add ory https://k8s.ory.sh/helm/charts
helm repo update
helm install hydra-oauth2 ory/hydra -f k8s/hydra-helm-values.yaml -n pulsar-test
kubectl apply -f k8s/hydra-oauth2-clients.yaml
```

### 3. Install Pulsar
```bash
helm repo add apache https://pulsar.apache.org/charts
helm install pulsar apache/pulsar -f k8s/pulsar-helm-values-minimal.yaml -n pulsar
```

### 4. Deploy Test Application

**Option A: Plain Text Secret**
```bash
kubectl apply -f k8s/oauth-credentials-plain-text.yaml
kubectl apply -f k8s/pulsar-component-fixed-plain.yaml
kubectl apply -f k8s/publisher-deploy-fixed.yaml
```

**Option B: JSON Secret**
```bash
kubectl apply -f k8s/oauth-credentials-json.yaml
kubectl apply -f k8s/pulsar-component-fixed-json.yaml
kubectl apply -f k8s/publisher-deploy-fixed-json.yaml
```

### 5. Verify
```bash
kubectl logs -n pulsar-test -l app=publisher-fixed -c publisher
# Should see: ✓ Message X sent
```

## File Formats

### Plain Text (`oauth-credentials-plain-text.yaml`)
```yaml
data:
  pulsar-secret.txt: "pulsar-secret"
```

### JSON (`oauth-credentials-json.yaml`)
```yaml
data:
  evmg-oauth2-credentials.json: |
    {
      "client_secret": "pulsar-secret"
    }
```

## Testing Invalid Secret

To test that invalid secrets are rejected:
```bash
kubectl apply -f k8s/oauth-credentials-json-invalid.yaml
# Update component to use invalid secret file
# Should fail with authentication error
```
