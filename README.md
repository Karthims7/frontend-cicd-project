# CollabCut: Independent Film Post-Production Tracker

A cloud-native, microservices-driven post-production management suite for independent filmmakers and boutique post houses, running locally on macOS (Apple Silicon M1 Pro) with **Podman** and **Minikube**, and automated via **GitHub Actions**.

---

## 1. Domain & Pipeline Overview

The post-production film pipeline (editing, dialogue subtitles, sound design, color grading) is notoriously complex. CollabCut solves this by decoupling the process into dedicated responsibilities:

1. **Footage Ingest:** Editors upload raw takes, tag Scene / Take / Camera Roll metadata, and map clip proxies.
2. **Script Burn-In Pipeline:** An automated pipeline overlays screenplay dialogue subtitles directly onto the work proxy files.
3. **Review & Feedback Service:** Directors, producers, colorists, and sound designers review picture cuts and drop frame-accurate, time-coded feedback notes (`00:01:24:12`) directly linked to the scene.

```
                      [ Director / Editor Browser ]
                                     │
           (Mac: http://localhost:3000 | Phone: http://<Mac-IP>:3000)
                                     │
                                     ▼
         ┌─────────────────────────────────────────────────────────┐
         │ Kubernetes Cluster (Minikube / containerd)              │
         │                                                         │
         │   [ Frontend Service (NodePort: 30080) ]                │
         │                  │                                      │
         │                  ▼                                      │
         │   [ CollabCut Studio UI (Vue 3 + Nginx Reverse Proxy) ] │
         │       ├── Cinematic Viewport & Scrubber                 │
         │       ├── Ingest Bin & Takes Manager                    │
         │       └── Proxy: /api/* ──────────────────────────┐     │
         │                                                   │     │
         │                                                   ▼     │
         │            [ CollabCut Core Service (Port: 8080) ]      │
         │               (Spring Boot 4 / Java 21)                 │
         │               ├── Footage Ingest API                    │
         │               ├── Script Burn-In Orchestrator           │
         │               └── Timecoded Review Notes API            │
         │                         │                               │
         │                         ▼                               │
         │              [ postgres (ClusterIP: 5432) ]             │
         │                   (PostgreSQL 16)                       │
         │                   ├── clips table                       │
         │                   └── timecoded_notes table             │
         │                         │                               │
         │                         ▼                               │
         │              [ PVC: postgres-pvc (1Gi) ]                │
         └─────────────────────────────────────────────────────────┘
```

---

## 2. Directory Structure

```text
├── .github/workflows/
│   └── ci.yml               # GitHub Actions CI pipeline (Java tests, Vue build, Kubeconform)
├── database/
│   ├── init.sql             # CollabCut schema (clips, timecoded_notes) and seed data
│   └── README.md            # Database management guide
├── frontend/
│   ├── src/                 # CollabCut Vue 3 Studio application
│   ├── Containerfile        # Multi-stage production container build (Node.js + Nginx)
│   ├── Dockerfile           # Standard Dockerfile compatibility mirror
│   ├── nginx.conf           # SPA routing & /api/ reverse proxy configuration
│   └── package.json         # Vite & Vue dependencies
├── k8s/
│   ├── coredns-backup.yaml  # Reference backup of CoreDNS ConfigMap
│   ├── frontend.yaml        # Deployment & NodePort Service for Vue frontend
│   ├── postgres.yaml        # Secret, PVC, Deployment & Service for PostgreSQL
│   └── user-service.yaml    # Deployment & Service for CollabCut backend service
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
* **CI/CD:** GitHub Actions with Kubeconform schema validation

---

## 4. Building & Deploying the Application

### A. Apply Database Schema
```bash
kubectl exec -i $(kubectl get pod -l app=postgres -o jsonpath='{.items[0].metadata.name}') -- psql -U appuser -d appdb < database/init.sql
```

### B. Build and Deploy Backend
```bash
cd user-service
./mvnw clean package -DskipTests
podman build -t localhost/user-service:local -f Containerfile .
podman save localhost/user-service:local -o /tmp/user-service.tar
minikube image load /tmp/user-service.tar -p devops-lab
rm -f /tmp/user-service.tar
cd ..
kubectl rollout restart deployment/user-service
```

### C. Build and Deploy Frontend
```bash
cd frontend
npm run build
podman build -t localhost/frontend:local -f Containerfile .
podman save localhost/frontend:local -o /tmp/frontend.tar
minikube image load /tmp/frontend.tar -p devops-lab
rm -f /tmp/frontend.tar
cd ..
kubectl rollout restart deployment/frontend
```

---

## 5. Accessing the CollabCut Studio

### From your Mac
Run port forwarding on port `3000`:
```bash
kubectl port-forward svc/frontend 3000:80
```
Open **[http://localhost:3000](http://localhost:3000)** in your browser.

### From your Mobile Phone (Same Local Wi-Fi)
1. Find your Mac's Wi-Fi IP address:
   ```bash
   ipconfig getifaddr en0
   ```
2. Start the port forward binding to all interfaces:
   ```bash
   kubectl port-forward --address 0.0.0.0 svc/frontend 3000:80
   ```
3. Open `http://<YOUR-MAC-IP>:3000` on your mobile browser.
