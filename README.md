## Dapr Pulsar OAuth2 Client Secret File Path Testing

This project demonstrates the Dapr Pulsar component's ability to read OAuth2 client secrets from files (`oauth2ClientSecretPath` feature).

**Status:** ✅ Feature is production ready (tested on Minikube)

### Quick Start

See [k8s/README.md](k8s/README.md) for complete testing instructions.

The k8s folder is organized into:
- **infrastructure/** - Helm values for Hydra and Pulsar
- **components/** - Dapr component definitions
- **credentials/** - OAuth2 credential files (ConfigMaps)
- **test-scenarios/** - Test deployments and scenario guides

### Three Test Scenarios

1. **Option A: Plain Text Secret** ✅ PASSING
   - Deploy: `k8s/credentials/oauth-credentials-plain-text.yaml` + component + deployment
   - Result: Messages published continuously

2. **Option B: JSON Secret (Valid)** ✅ PASSING
   - Deploy: `k8s/credentials/oauth-credentials-json.yaml` + component + deployment
   - Result: Messages published continuously

3. **Option C: JSON Secret (Invalid)** ✅ FAILING AS EXPECTED
   - Deploy: `k8s/credentials/oauth-credentials-json-invalid.yaml` + component + deployment
   - Result: Auth error (expected)


