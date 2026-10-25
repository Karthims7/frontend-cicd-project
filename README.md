# Production-Inspired Kubernetes Microservices Lab

A practical, production-inspired Kubernetes microservices lab running locally on macOS (Apple Silicon M1 Pro) using **Podman** and **Minikube**, featuring a **Vue.js** frontend, a **Java Spring Boot** backend microservice, and a **PostgreSQL** persistent database, automated with **GitHub Actions**.

---

## 1. Architecture Topology

```
                       [ Client / Browser ]
                                │
        (Mac: http://localhost:3000 | Phone: http://<Mac-IP>:3000)
                                │
                                ▼
         ┌──────────────────────────────────────────────┐
         │ Kubernetes Cluster (Minikube / containerd)   │
         │                                              │
         │   [ Frontend Service (NodePort: 30080) ]     │
         │                  │                           │
         │                  ▼                           │
         │   [ Frontend Pod (Vue 3 + Nginx Reverse Proxy)
         │       ├── Static SPA Assets                  │
         │       └── Proxy: /api/* ───────────────┐     │
         │                                        │     │
         │                                        ▼     │
         │            [ user-service (ClusterIP: 8080) ]│
         │               (Spring Boot 4 / Java 21)      │
         │                         │                    │
         │                         ▼                    │
         │              [ postgres (ClusterIP: 5432) ]  │
         │                   (PostgreSQL 16)            │
         │                         │                    │
         │                         ▼                    │
         │              [ PVC: postgres-pvc (1Gi) ]     │
         └──────────────────────────────────────────────┘
```

---

## 2. Directory Structure

```text
├── .github/workflows/
│   └── ci.yml               # GitHub Actions CI pipeline (Backend, Frontend, K8s dry-run)
├── database/
│   ├── init.sql             # Database schema and seed data
│   └── README.md            # Database management guide
├── frontend/
│   ├── src/                 # Vue 3 application source code
│   ├── Containerfile        # Multi-stage production container build (Node.js + Nginx)
│   ├── Dockerfile           # Standard Dockerfile compatibility mirror
│   ├── nginx.conf           # SPA routing & /api/ reverse proxy configuration
│   └── package.json         # Vite & Vue dependencies
├── k8s/
│   ├── coredns-backup.yaml  # Reference backup of CoreDNS ConfigMap
│   ├── frontend.yaml        # Deployment & NodePort Service for Vue frontend
│   ├── postgres.yaml        # Secret, PVC, Deployment & Service for PostgreSQL
│   └── user-service.yaml    # Deployment & Service for Spring Boot user-service
├── user-service/
│   ├── src/                 # Spring Boot source code and tests
│   ├── Containerfile        # JRE runtime image definition
│   ├── Dockerfile           # Multi-stage Maven build definition
│   └── pom.xml              # Maven dependencies (Web, Actuator, JPA, PostgreSQL)
└── README.md                # Project documentation
```

---

## 3. Technology Stack & Prerequisites

* **Host Machine:** MacBook Pro M1 Pro (Apple Silicon, macOS)
* **Container Engine:** Podman (`applehv` machine provider)
* **Local Kubernetes:** Minikube (`podman` driver, `containerd` container runtime)
* **Frontend:** Vue 3, Vite, Nginx reverse proxy
* **Backend:** Java 21, Spring Boot 4.1.2, Spring Data JPA, HikariCP
* **Database:** PostgreSQL 16 Alpine with Kubernetes PersistentVolumeClaim
* **CI/CD:** GitHub Actions

---

## 4. Building & Loading Images (Podman-Native Workflow)

Because Minikube uses `containerd`, container images built locally via Podman must be transferred into Minikube's local image cache.

### Backend (`user-service`)
```bash
# 1. Build JAR using Maven
cd user-service
./mvnw clean package -DskipTests

# 2. Build image with Podman
podman build -t localhost/user-service:local -f Containerfile .
cd ..

# 3. Transfer image into Minikube
podman save localhost/user-service:local -o /tmp/user-service.tar
minikube image load /tmp/user-service.tar -p devops-lab
rm -f /tmp/user-service.tar
```

### Frontend (`frontend`)
```bash
# 1. Build multi-stage image with Podman
podman build -t localhost/frontend:local -f frontend/Containerfile frontend/

# 2. Transfer image into Minikube
podman save localhost/frontend:local -o /tmp/frontend.tar
minikube image load /tmp/frontend.tar -p devops-lab
rm -f /tmp/frontend.tar
```

---

## 5. Deploying to Kubernetes

Apply manifests in dependency order:

```bash
# 1. Database
kubectl apply -f k8s/postgres.yaml

# 2. Backend Service
kubectl apply -f k8s/user-service.yaml

# 3. Frontend UI
kubectl apply -f k8s/frontend.yaml

# 4. Initialize Database Schema & Seed Data
kubectl exec -i $(kubectl get pod -l app=postgres -o jsonpath='{.items[0].metadata.name}') -- psql -U appuser -d appdb < database/init.sql
```

Verify that all workloads are healthy and `Running`:
```bash
kubectl get pods,svc,pvc
```

---

## 6. Accessing the Application

### Access from your Mac
Run port forwarding on port `3000`:
```bash
kubectl port-forward svc/frontend 3000:80
```
Open **[http://localhost:3000](http://localhost:3000)** in your browser.

### Access from your Mobile Phone (Same Local Wi-Fi)
1. Find your Mac's Wi-Fi IP address:
   ```bash
   ipconfig getifaddr en0
   ```
   *(Example: `192.168.29.163`)*

2. Start the port forward binding to all interfaces:
   ```bash
   kubectl port-forward --address 0.0.0.0 svc/frontend 3000:80
   ```

3. Open **`http://<YOUR-MAC-IP>:3000`** on your phone's browser (e.g. `http://192.168.29.163:3000`).

---

## 7. CI/CD Architecture (GitHub Actions)

The workflow in `.github/workflows/ci.yml` triggers on every push and pull request to `main`:
1. **`backend-ci`**: Sets up JDK 21, executes `./mvnw test`, packages the Spring Boot JAR, and validates container image build.
2. **`frontend-ci`**: Sets up Node.js 20, runs `npm install`, compiles the Vue production bundle (`npm run build`), and builds the Nginx container image.
3. **`k8s-validation`**: Uses `kubectl --dry-run=client` to ensure all Kubernetes manifests are syntactically valid and conform to Kubernetes schemas.

> [!NOTE]
> **CI vs. Deployment Separation:**
> Cloud runners on GitHub Actions cannot directly reach your local Minikube cluster running on your Mac without a secure tunnel or a self-hosted runner. The CI pipeline validates builds and images on GitHub, while local deployment runs reproducibly from your repository manifests.
