e reading and JSON parsing, making it reusable for other components.
## Quick Start

### Prerequisites
- Minikube with registry addon enabled
- Helm 3.x
- kubectl

### 1. Create Namespaces
```bash
kubectl create namespace pulsar-test
kubectl create namespace pulsar
```

### 2. Install Dapr
```bash
dapr init -k
```

### 3. Install Hydra OAuth2 Server
```bash
helm repo add ory https://k8s.ory.sh/helm/charts
helm repo update
helm install hydra-oauth2 ory/hydra -f k8s/hydra-helm-values.yaml -n pulsar-test --create-namespace
kubectl apply -f k8s/hydra-oauth2-clients.yaml

# Wait for Hydra to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=hydra -n pulsar-test --timeout=300s
```

### 4. Install Pulsar
```bash
helm repo add apache https://pulsar.apache.org/charts
helm repo update
helm install pulsar apache/pulsar -f k8s/pulsar-helm-values-minimal.yaml -n pulsar --create-namespace

# Wait for Pulsar to be ready
kubectl wait --for=condition=ready pod -l app=pulsar,component=broker -n pulsar --timeout=600s
```

### 5. Deploy Test Application

**Option A: Plain Text Secret**
```bash
kubectl apply -f k8s/oauth-credentials-plain-text.yaml
kubectl apply -f k8s/pulsar-component-fixed-plain.yaml
kubectl apply -f k8s/publisher-deploy-fixed.yaml

# Verify
kubectl logs -n pulsar-test -l app=publisher-fixed -c publisher --tail=10
# Should see: ✓ Message X sent
```

**Option B: JSON Secret**
```bash
kubectl apply -f k8s/oauth-credentials-json.yaml
kubectl apply -f k8s/pulsar-component-fixed-json.yaml
kubectl apply -f k8s/publisher-deploy-fixed-json.yaml

# Verify
kubectl logs -n pulsar-test -l app=publisher-fixed-json -c publisher --tail=10
# Should see: ✓ Message X sent
```

**Option C: Invalid Secret (Should Fail)**
```bash
kubectl apply -f k8s/oauth-credentials-json-invalid.yaml
kubectl apply -f k8s/pulsar-component-fixed-json.yaml
kubectl apply -f k8s/publisher-deploy-fixed-json-invalid.yaml

# Verify failure
kubectl logs -n pulsar-test -l app=publisher-fixed-json-invalid -c daprd --tail=20
# Should see: "invalid_client" "Client authentication failed"
```

## File Formats

### Plain Text (`oauth-credentials-plain-text.yaml`)
The file contains just the secret value:
```yaml
data:
  pulsar-secret.txt: "pulsar-secret"
```

### JSON (`oauth-credentials-json.yaml`)
The file contains a JSON object with a `client_secret` field:
```yaml
data:
  evmg-oauth2-credentials.json: |
    {
      "type": "client_credentials",
      "client_id": "pulsar-client",
      "client_secret": "pulsar-secret",
      "issuer_url": "http://mock-oauth2.pulsar-test.svc.cluster.local:8080/default"
    }
```

**Note**: Only the `client_secret` field is extracted from the JSON. Other fields are ignored.

## Verification

### Check Component Initialization
```bash
kubectl logs -n pulsar-test -l app=publisher-fixed -c daprd | grep "Fetched initial"
# Should see: "Fetched initial oauth2 client_credentials token"
```

### Check Pulsar Authentication
```bash
kubectl logs -n pulsar pulsar-broker-0 | grep "connected with role"
# Should see: "connected with role=pulsar-client using authMethod=token"
```

### Check Message Publishing
```bash
kubectl logs -n pulsar-test -l app=publisher-fixed -c publisher
# Should see: ✓ Message 1 sent, ✓ Message 2 sent, etc.
```