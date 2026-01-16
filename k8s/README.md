# Dapr Pulsar OAuth2 Authentication

File-based OAuth2 secret management for Dapr Pulsar PubSub. Read credentials from mounted files instead of embedding in configuration.

**Status:** ✅ Production Ready (tested on Minikube)

---

## Prerequisites

- Minikube with Dapr sidecar injection enabled
- Helm 3
- `kubectl` configured for minikube

---

## Infrastructure Setup

### 1. Install Hydra OAuth2 Server

```bash
# Add Hydra Helm repo
helm repo add ory https://k8s.ory.sh
helm repo update

# Install Hydra in pulsar-test namespace
kubectl create namespace pulsar-test
helm install hydra-oauth2 ory/hydra \
  -n pulsar-test \
  -f k8s/infrastructure/hydra-helm-values.yaml

# Configure OAuth2 clients
kubectl apply -f k8s/infrastructure/hydra-oauth2-clients.yaml
```

### 2. Install Apache Pulsar

```bash
# Add Pulsar Helm repo
helm repo add apache https://pulsar.apache.org/charts
helm repo update

# Install Pulsar cluster
helm install pulsar apache/pulsar \
  -n pulsar \
  --create-namespace \
  -f k8s/infrastructure/pulsar-helm-values-minimal.yaml
```

---

## Build Publisher Application

```bash
# Build Docker image
cd publisher
mvn clean package
docker build -t publisher:1.0 .

# Load into minikube
minikube image load publisher:1.0
```

---

## Test Scenarios

All test scenarios are self-contained YAML files in `k8s/test-scenarios/`. Each includes credentials, Dapr component definition, and publisher deployment.

### Scenario A: Plain Text Secret

```bash
kubectl apply -f k8s/test-scenarios/scenario-a-plain-text.yaml
```

**Expected Result:** Publisher continuously sends messages

```bash
kubectl logs -n pulsar-test -l app=publisher-fixed -c publisher --tail=3
```

```
[20:36:21.570] ✓ Message 516 sent (0.0s)
[20:36:23.578] ✓ Message 517 sent (0.0s)
```

---

### Scenario B: JSON Secret (Valid)

```bash
kubectl apply -f k8s/test-scenarios/scenario-b-json-valid.yaml
```

**Expected Result:** Publisher continuously sends messages

```bash
kubectl logs -n pulsar-test -l app=publisher-fixed-json -c publisher --tail=3
```

```
[20:36:23.082] ✓ Message 508 sent (0.0s)
[20:36:25.090] ✓ Message 509 sent (0.0s)
```

---

### Scenario C: JSON Secret (Invalid) - Expected Failure

```bash
kubectl apply -f k8s/test-scenarios/scenario-c-json-invalid.yaml
```

**Expected Result:** Pod CrashLoopBackOff with authentication error

```bash
kubectl logs -n pulsar-test -l app=publisher-fixed-json-invalid -c daprd --tail=20 | grep -i error
```

```
level=fatal msg="Failed to initialize component pulsar-pubsub: 
oauth2: \"invalid_client\" \"Client authentication failed...\""
```

---

## Feature Details

### oauth2ClientSecretPath

Reads OAuth2 client secret from file. Supports both plain text and JSON formats.

**Component Definition:**
```yaml
spec:
  type: pubsub.pulsar
  version: v1
  metadata:
  - name: oauth2ClientID
    value: "pulsar-client"
  - name: oauth2ClientSecretPath
    value: "/snp/resources/pulsar-secret.txt"
  - name: oauth2TokenURL
    value: "http://hydra-oauth2-public.pulsar-test.svc.cluster.local:4444/oauth2/token"
```

**Supported File Formats:**

Plain text (secret value only):
```
pulsar-secret
```

JSON (with `client_secret` field):
```json
{
  "client_secret": "pulsar-secret"
}
```

**Deployment Volume Mount:**
```yaml
annotations:
  dapr.io/volume-mounts: "pulsar-secret:/snp/resources"
```

---

### oauth2CredentialsFile

Loads complete credentials from JSON file with client_id, client_secret, and issuer_url.

**Component Definition:**
```yaml
metadata:
- name: oauth2CredentialsFile
  value: "/snp/resources/credentials.json"
```

**JSON Structure:**
```json
{
  "client_id": "pulsar-client",
  "client_secret": "pulsar-secret",
  "issuer_url": "http://hydra-oauth2:4444/oauth2/token"
}
```

**Behavior:**
- Loads all three credentials from file
- Metadata fields override file values  
- Mutually exclusive with `oauth2ClientSecretPath`

---

## File Organization

```
k8s/
├── infrastructure/                    Helm configurations
│   ├── hydra-helm-values.yaml         Hydra OAuth2 server setup
│   ├── hydra-oauth2-clients.yaml      OAuth2 client definitions
│   └── pulsar-helm-values-minimal.yaml Pulsar cluster setup
└── test-scenarios/                    Test cases
    ├── scenario-a-plain-text.yaml     (ConfigMap + Component + Deployment)
    ├── scenario-b-json-valid.yaml     (ConfigMap + Component + Deployment)
    ├── scenario-c-json-invalid.yaml   (ConfigMap + Component + Deployment)
    └── README.md                       Quick reference
```

Each scenario file contains all necessary resources - no external dependencies.

---

## Troubleshooting

**Pods stuck in ErrImageNeverPull**
```bash
minikube image load publisher:1.0
```

**Authentication failures - check sidecar logs:**
```bash
kubectl logs -n pulsar-test <pod-name> -c daprd --tail=50 | grep -i "oauth2\|token"
```

**Pulsar broker connectivity - verify readiness:**
```bash
kubectl wait --for=condition=ready pod -l app=pulsar,component=broker -n pulsar --timeout=300s
```

---

## References

- [Dapr Pubsub Components](https://docs.dapr.io/reference/components-reference/supported-pubsub/)
- [Pulsar Authentication](https://pulsar.apache.org/docs/en/security-authentication-oauth2/)
- [Hydra OAuth2 Server](https://www.ory.sh/hydra/)

**Scenario:** Read OAuth2 client secret from a plain text file.
