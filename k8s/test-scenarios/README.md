# Dapr Pulsar OAuth2 ClientSecretPath Feature Tests

Three test scenarios verify the `oauth2ClientSecretPath` feature works correctly.

## Test Scenarios

### Scenario A: Plain Text Secret File
- **File:** [scenario-a-plain-text.yaml](scenario-a-plain-text.yaml)
- **Tests:** `oauth2ClientSecretPath` with plain text secret
- **Expected:** ✅ Messages published continuously
- **Deploy:** `kubectl apply -f scenario-a-plain-text.yaml`

### Scenario B: JSON Secret (Valid)
- **File:** [scenario-b-json-valid.yaml](scenario-b-json-valid.yaml)
- **Tests:** `oauth2ClientSecretPath` with JSON file (extracts `client_secret` field)
- **Expected:** ✅ Messages published continuously
- **Deploy:** `kubectl apply -f scenario-b-json-valid.yaml`

### Scenario C: JSON Secret (Invalid)
- **File:** [scenario-c-json-invalid.yaml](scenario-c-json-invalid.yaml)
- **Tests:** Invalid credentials validation
- **Expected:** ❌ CrashLoopBackOff with `invalid_client` error (expected failure)
- **Deploy:** `kubectl apply -f scenario-c-json-invalid.yaml`

## Quick Verification

```bash
# Deploy all scenarios
for file in scenario-*.yaml; do kubectl apply -f "$file"; done

# Watch deployments
kubectl get pods -n pulsar-test -w

# Check logs (success scenarios)
kubectl logs -n pulsar-test -l app=publisher-fixed -c publisher --tail=5
kubectl logs -n pulsar-test -l app=publisher-fixed-json -c publisher --tail=5

# Check error (invalid scenario)
kubectl logs -n pulsar-test -l app=publisher-fixed-json-invalid -c daprd --tail=20
```

## File Structure

Each scenario is a complete, self-contained YAML file with:
- ConfigMap (credentials)
- Component definition (Dapr pubsub component)
- Deployment (publisher application)

```
test-scenarios/
├── scenario-a-plain-text.yaml
├── scenario-b-json-valid.yaml
├── scenario-c-json-invalid.yaml
└── README.md
```

See [../README.md](../README.md) for comprehensive feature documentation.
