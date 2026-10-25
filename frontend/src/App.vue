<template>
  <div class="lab-container">
    <!-- Header -->
    <header class="header">
      <div class="brand">
        <div class="logo-badge">☸️</div>
        <div>
          <h1>Kubernetes Microservices Lab</h1>
          <p class="subtitle">Podman • Minikube • Vue • Spring Boot • PostgreSQL</p>
        </div>
      </div>
      <div class="cluster-badge">
        <span class="status-dot"></span>
        <span>devops-lab (Minikube / containerd)</span>
      </div>
    </header>

    <!-- Architecture Flow -->
    <section class="card flow-card">
      <h2 class="section-title">Traffic & Architecture Topology</h2>
      <div class="flow-diagram">
        <div class="node">
          <div class="node-icon">💻</div>
          <div class="node-title">Browser</div>
          <div class="node-meta">Client / Host</div>
        </div>
        <div class="flow-arrow">➔</div>
        <div class="node active">
          <div class="node-icon">⚡</div>
          <div class="node-title">Frontend Pod</div>
          <div class="node-meta">Vue 3 + Nginx :80</div>
        </div>
        <div class="flow-arrow">➔</div>
        <div class="node" :class="{ active: apiState.status === 'success' }">
          <div class="node-icon">☕</div>
          <div class="node-title">user-service</div>
          <div class="node-meta">Spring Boot :8080</div>
        </div>
        <div class="flow-arrow">➔</div>
        <div class="node" :class="{ active: apiState.status === 'success' }">
          <div class="node-icon">🐘</div>
          <div class="node-title">postgres</div>
          <div class="node-meta">PostgreSQL 16 :5432</div>
        </div>
      </div>
    </section>

    <!-- Status Grid -->
    <div class="grid-layout">
      <!-- Status Column -->
      <section class="card">
        <h2 class="section-title">Component Status</h2>
        <div class="status-list">
          <div class="status-item">
            <div class="item-info">
              <span class="item-name">Frontend UI</span>
              <span class="item-sub">Vue 3 SPA via Nginx Reverse Proxy</span>
            </div>
            <span class="pill pill-success">Running (1/1)</span>
          </div>

          <div class="status-item">
            <div class="item-info">
              <span class="item-name">user-service</span>
              <span class="item-sub">Spring Boot 4.1.2 (Java 21)</span>
            </div>
            <span 
              class="pill"
              :class="apiState.status === 'success' ? 'pill-success' : (apiState.status === 'loading' ? 'pill-loading' : 'pill-neutral')"
            >
              {{ apiState.status === 'success' ? 'Responsive' : (apiState.status === 'loading' ? 'Checking...' : 'Ready') }}
            </span>
          </div>

          <div class="status-item">
            <div class="item-info">
              <span class="item-name">PostgreSQL Database</span>
              <span class="item-sub">Persistent storage bound (1Gi PVC)</span>
            </div>
            <span class="pill pill-success">Connected</span>
          </div>

          <div class="status-item">
            <div class="item-info">
              <span class="item-name">Cluster Pod Network</span>
              <span class="item-sub">IPTables Forwarding & DNS Resolution</span>
            </div>
            <span class="pill pill-success">Healthy</span>
          </div>
        </div>
      </section>

      <!-- Interactive API Card -->
      <section class="card">
        <div class="card-header">
          <h2 class="section-title">Backend API Inspector</h2>
          <button 
            class="btn-primary" 
            :disabled="apiState.status === 'loading'" 
            @click="fetchHelloApi"
          >
            <span v-if="apiState.status === 'loading'" class="spinner"></span>
            <span>{{ apiState.status === 'loading' ? 'Requesting...' : 'Fetch /api/hello' }}</span>
          </button>
        </div>

        <div class="endpoint-bar">
          <span class="method">GET</span>
          <span class="url">{{ apiEndpoint }}</span>
          <span v-if="apiState.latency !== null" class="latency">{{ apiState.latency }} ms</span>
        </div>

        <!-- Success Result -->
        <div v-if="apiState.status === 'success'" class="result-box success">
          <div class="result-header">
            <span class="badge badge-success">HTTP 200 OK</span>
            <span class="timestamp">{{ apiState.timestamp }}</span>
          </div>
          <pre><code>{{ JSON.stringify(apiState.data, null, 2) }}</code></pre>
        </div>

        <!-- Loading Result -->
        <div v-else-if="apiState.status === 'loading'" class="result-box loading">
          <div class="skeleton-line"></div>
          <div class="skeleton-line short"></div>
        </div>

        <!-- Error Result -->
        <div v-else-if="apiState.status === 'error'" class="result-box error">
          <div class="result-header">
            <span class="badge badge-error">Request Failed</span>
            <span class="timestamp">{{ apiState.timestamp }}</span>
          </div>
          <p class="error-msg">{{ apiState.errorMessage }}</p>
          <div class="troubleshoot-tips">
            <strong>Checklist:</strong>
            <ul>
              <li>Ensure <code>user-service</code> pod is Running in Kubernetes.</li>
              <li>Verify that Nginx reverse proxy routes <code>/api/</code> to <code>http://user-service:8080/api/</code>.</li>
              <li>Check cluster pod-to-pod forwarding.</li>
            </ul>
          </div>
        </div>

        <!-- Idle Result -->
        <div v-else class="result-box idle">
          <p>Click <strong>Fetch /api/hello</strong> to test end-to-end communication from Vue to the Spring Boot microservice.</p>
        </div>
      </section>
    </div>

    <!-- Footer -->
    <footer class="footer">
      <span>Kubernetes Microservices Lab • Running on Apple Silicon (M1 Pro) with Podman & Minikube</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';

const apiEndpoint = ref('/api/hello');

const apiState = ref({
  status: 'idle', // 'idle' | 'loading' | 'success' | 'error'
  data: null,
  errorMessage: '',
  latency: null,
  timestamp: null
});

async function fetchHelloApi() {
  apiState.value.status = 'loading';
  apiState.value.errorMessage = '';
  const startTime = performance.now();

  try {
    const response = await fetch(apiEndpoint.value, {
      headers: {
        'Accept': 'application/json'
      }
    });

    const endTime = performance.now();
    apiState.value.latency = Math.round(endTime - startTime);
    apiState.value.timestamp = new Date().toLocaleTimeString();

    if (!response.ok) {
      throw new Error(`HTTP Error ${response.status}: ${response.statusText}`);
    }

    const json = await response.json();
    apiState.value.data = json;
    apiState.value.status = 'success';
  } catch (err) {
    const endTime = performance.now();
    apiState.value.latency = Math.round(endTime - startTime);
    apiState.value.timestamp = new Date().toLocaleTimeString();
    apiState.value.status = 'error';
    apiState.value.errorMessage = err.message || 'Failed to connect to backend API';
  }
}

onMounted(() => {
  fetchHelloApi();
});
</script>

<style scoped>
.lab-container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 2.5rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.brand {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.logo-badge {
  font-size: 2.5rem;
  background: rgba(59, 130, 246, 0.15);
  border: 1px solid rgba(59, 130, 246, 0.3);
  border-radius: var(--radius-md);
  padding: 0.5rem 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

h1 {
  font-size: 1.6rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.subtitle {
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.cluster-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  padding: 0.5rem 1rem;
  border-radius: 9999px;
  font-size: 0.85rem;
  font-family: 'JetBrains Mono', monospace;
  color: var(--text-secondary);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--accent-green);
  box-shadow: 0 0 10px var(--accent-green);
}

.card {
  background: var(--bg-card);
  backdrop-filter: blur(16px);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 1.5rem;
  box-shadow: var(--shadow-lg);
}

.section-title {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 1.25rem;
  color: var(--text-primary);
}

.flow-card {
  background: linear-gradient(135deg, rgba(17, 24, 39, 0.8) 0%, rgba(30, 41, 59, 0.5) 100%);
}

.flow-diagram {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
  padding: 0.5rem 0;
}

.node {
  flex: 1;
  min-width: 140px;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 1rem;
  text-align: center;
  transition: all 0.3s ease;
}

.node.active {
  border-color: var(--accent-blue);
  background: rgba(59, 130, 246, 0.08);
  box-shadow: 0 0 15px rgba(59, 130, 246, 0.2);
}

.node-icon {
  font-size: 1.8rem;
  margin-bottom: 0.4rem;
}

.node-title {
  font-weight: 600;
  font-size: 0.95rem;
}

.node-meta {
  font-size: 0.75rem;
  color: var(--text-muted);
  font-family: 'JetBrains Mono', monospace;
  margin-top: 0.2rem;
}

.flow-arrow {
  color: var(--text-muted);
  font-size: 1.2rem;
  font-weight: bold;
}

.grid-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

@media (max-width: 768px) {
  .grid-layout {
    grid-template-columns: 1fr;
  }
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background: rgba(15, 23, 42, 0.4);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
}

.item-info {
  display: flex;
  flex-direction: column;
}

.item-name {
  font-weight: 600;
  font-size: 0.9rem;
}

.item-sub {
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.pill {
  padding: 0.25rem 0.65rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
}

.pill-success {
  background: rgba(16, 185, 129, 0.15);
  color: var(--accent-green);
  border: 1px solid rgba(16, 185, 129, 0.3);
}

.pill-loading {
  background: rgba(59, 130, 246, 0.15);
  color: var(--accent-blue);
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.pill-neutral {
  background: rgba(156, 163, 175, 0.15);
  color: var(--text-secondary);
  border: 1px solid rgba(156, 163, 175, 0.3);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  color: white;
  padding: 0.55rem 1.1rem;
  border-radius: var(--radius-sm);
  font-weight: 600;
  font-size: 0.85rem;
}

.btn-primary:hover:not(:disabled) {
  filter: brightness(1.1);
  transform: translateY(-1px);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.endpoint-bar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 0.5rem 0.75rem;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8rem;
  margin-bottom: 1rem;
}

.method {
  background: #1e3a8a;
  color: #93c5fd;
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
  font-weight: 700;
  font-size: 0.7rem;
}

.url {
  color: var(--text-primary);
  flex: 1;
}

.latency {
  color: var(--accent-green);
  font-weight: 600;
}

.result-box {
  background: rgba(10, 15, 26, 0.95);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 1rem;
  min-height: 140px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--border-color);
}

.badge {
  font-size: 0.7rem;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.badge-success {
  background: rgba(16, 185, 129, 0.2);
  color: var(--accent-green);
}

.badge-error {
  background: rgba(239, 68, 68, 0.2);
  color: var(--accent-red);
}

.timestamp {
  font-size: 0.75rem;
  color: var(--text-muted);
  font-family: 'JetBrains Mono', monospace;
}

pre {
  color: #a7f3d0;
  font-size: 0.85rem;
  line-height: 1.5;
  overflow-x: auto;
}

.error-msg {
  color: #fca5a5;
  font-weight: 600;
  font-size: 0.9rem;
  margin-bottom: 0.75rem;
}

.troubleshoot-tips {
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: var(--radius-sm);
  padding: 0.75rem;
  font-size: 0.8rem;
  color: var(--text-secondary);
}

.troubleshoot-tips ul {
  margin-left: 1.25rem;
  margin-top: 0.4rem;
}

.idle {
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--text-secondary);
  font-size: 0.85rem;
}

.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.footer {
  text-align: center;
  font-size: 0.8rem;
  color: var(--text-muted);
  padding: 1rem 0;
  border-top: 1px solid var(--border-color);
}
</style>
