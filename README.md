# Frontend CI/CD & Kubernetes Microservices Lab

A full-stack Kubernetes microservices project running locally on macOS (M1 Pro) using Podman and Minikube, featuring:
- **Frontend**: Vue.js application
- **Backend**: Java Spring Boot microservice (`user-service`)
- **Database**: PostgreSQL with persistent storage (PVC)
- **Containerization & Orchestration**: Podman, Minikube, Kubernetes manifests
- **CI/CD**: GitHub Actions pipeline

## Architecture & Directory Structure
- `k8s/`: Kubernetes manifests for PostgreSQL, User Service, and CoreDNS
- `user-service/`: Spring Boot service with JPA & Actuator health endpoints
- `frontend/`: Vue client application
- `database/`: Database schema, migrations, and seed scripts
- `.github/workflows/`: CI/CD automation pipelines
