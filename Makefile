# Makefile for Dapr Pulsar Demo

# Build Java applications
build:
	cd publisher && mvn clean package
	cd subscriber && mvn clean package

apps-up:
	# Clean up existing containers and images
	docker compose rm -f
	
	# Build Java applications
	cd publisher && mvn clean package
	cd subscriber && mvn clean package
	
	# Start everything with fresh builds
	docker compose pull
	docker compose up -d --build --force-recreate

apps-down:
	docker compose down

# Clean up
clean:
	docker compose down -v
	cd publisher && mvn clean
	cd subscriber && mvn clean

# dapr-run Publisher
dapr-run-publisher:
	cd publisher && mvn clean package
	dapr run --app-id publisher \
         --components-path ../components \
         -- java -jar target/dapr-pulsar-publisher-1.0-SNAPSHOT.jar

# dapr-run Subscriber
dapr-run-subscriber:
	cd subscriber && mvn clean package
	dapr run --app-id subscriber \
         --app-port 8082 \
         --resources-path ../components \
         -- java -jar target/dapr-pulsar-subscriber-1.0-SNAPSHOT.jar

# Help
help:
	@echo "Available commands:"
	@echo "  make build              - Build Java applications"
	@echo "  make apps-up           - Start everything including Java apps"
	@echo "  make apps-down         - Stop everything"
	@echo "  make clean             - Clean up everything"

.PHONY: build apps-up apps-down clean