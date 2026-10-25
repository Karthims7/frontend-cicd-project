<template>
  <div class="collabcut-app">
    <!-- Studio Header -->
    <header class="studio-header">
      <div class="studio-brand">
        <div class="clapper-icon">🎬</div>
        <div>
          <div class="brand-title">CollabCut Studio</div>
          <div class="project-tag">
            Event-Driven Film Post-Production Tracker • <em>"Chronicles of Dawn"</em>
          </div>
        </div>
      </div>
      <div class="header-controls">
        <button class="btn-event-feed" @click="showEventFeed = !showEventFeed">
          <span>📡 Broker Stream ({{ events.length }})</span>
        </button>
        <button class="btn-ingest" @click="showIngestModal = true">
          <span>+ Ingest Footage</span>
        </button>
        <button class="btn-icon" :disabled="loading" @click="fetchAll" title="Reload pipeline data">
          <span :class="{ 'spin': loading }">🔄</span>
        </button>
      </div>
    </header>

    <!-- Microservices Architecture Diagram Banner -->
    <section class="arch-banner">
      <div class="arch-label">Decoupled Architecture Flow:</div>
      <div class="arch-flow">
        <div class="arch-node">
          <span class="node-title">1. Ingest API</span>
          <span class="node-sub">POST /api/assets</span>
        </div>
        <div class="arch-arrow">
          <span class="event-tag">footage.uploaded</span>
          <span>➔</span>
        </div>
        <div class="arch-node">
          <span class="node-title">2. Burn-In Worker</span>
          <span class="node-sub">FFmpeg Dialogue Overlay</span>
        </div>
        <div class="arch-arrow">
          <span class="event-tag">footage.processed</span>
          <span>➔</span>
        </div>
        <div class="arch-node">
          <span class="node-title">3. Review API</span>
          <span class="node-sub">PostgreSQL Timecodes</span>
        </div>
      </div>
    </section>

    <!-- KPI Metrics Bar -->
    <section class="kpi-grid">
      <div class="kpi-card" :class="{ 'active': filterStatus === 'ALL' }" @click="filterStatus = 'ALL'">
        <div class="kpi-icon">🎞️</div>
        <div>
          <div class="kpi-num">{{ assets.length }}</div>
          <div class="kpi-label">Total Assets</div>
        </div>
      </div>

      <div class="kpi-card" :class="{ 'active': filterStatus === 'UPLOADED' }" @click="filterStatus = 'UPLOADED'">
        <div class="kpi-icon ingest-color">📥</div>
        <div>
          <div class="kpi-num">{{ countStatus('UPLOADED') }}</div>
          <div class="kpi-label">Uploaded</div>
        </div>
      </div>

      <div class="kpi-card" :class="{ 'active': filterStatus === 'READY_FOR_REVIEW' }" @click="filterStatus = 'READY_FOR_REVIEW'">
        <div class="kpi-icon review-color">👁️</div>
        <div>
          <div class="kpi-num">{{ countStatus('READY_FOR_REVIEW') }}</div>
          <div class="kpi-label">Ready for Review</div>
        </div>
      </div>

      <div class="kpi-card" :class="{ 'active': filterStatus === 'APPROVED' }" @click="filterStatus = 'APPROVED'">
        <div class="kpi-icon locked-color">🔒</div>
        <div>
          <div class="kpi-num">{{ countStatus('APPROVED') }}</div>
          <div class="kpi-label">Picture Lock</div>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon notes-color">💬</div>
        <div>
          <div class="kpi-num">{{ totalCommentsCount }}</div>
          <div class="kpi-label">Timecoded Notes</div>
        </div>
      </div>
    </section>

    <!-- Toast Notification -->
    <div v-if="toast.show" class="studio-toast" :class="toast.type">
      {{ toast.message }}
    </div>

    <!-- Live Event Broker Modal / Drawer -->
    <div v-if="showEventFeed" class="event-drawer">
      <div class="drawer-header">
        <div class="drawer-title">
          <span>📡 Asynchronous Message Broker Feed (RabbitMQ Log)</span>
        </div>
        <button class="btn-close-sm" @click="showEventFeed = false">✕</button>
      </div>
      <div class="event-list">
        <div v-for="evt in events" :key="evt.id" class="event-card">
          <div class="event-card-top">
            <span class="event-badge">{{ evt.eventName }}</span>
            <span class="event-time">{{ formatDate(evt.createdAt) }}</span>
          </div>
          <pre><code>{{ formatPayload(evt.payload) }}</code></pre>
        </div>
        <div v-if="events.length === 0" class="empty-events">No events emitted yet.</div>
      </div>
    </div>

    <!-- Main Studio Workspace (Split View) -->
    <div class="studio-workspace">
      <!-- Left: Asset Ingest Bin -->
      <section class="bin-panel">
        <div class="panel-topbar">
          <div class="bin-title">
            <span>Footage Ingest Bin</span>
            <span class="count-pill">{{ filteredAssets.length }}</span>
          </div>
          <div class="bin-search">
            <input 
              v-model="searchQuery" 
              type="text" 
              placeholder="Search scene, take, roll, title..." 
            />
          </div>
        </div>

        <div class="asset-feed">
          <div 
            v-for="asset in filteredAssets" 
            :key="asset.id"
            class="asset-card"
            :class="{ 'selected': selectedAsset && selectedAsset.id === asset.id }"
            @click="selectAsset(asset)"
          >
            <div class="asset-card-header">
              <span class="scene-badge">{{ asset.sceneId }} • Take {{ asset.takeNumber }}</span>
              <span class="status-pill" :class="'status-' + asset.status.toLowerCase()">
                {{ formatStatus(asset.status) }}
              </span>
            </div>

            <div class="asset-title">{{ asset.title }}</div>

            <div class="s3-path">
              <span>🗄️ {{ asset.s3BucketUrl }}</span>
            </div>

            <div v-if="asset.processedS3Url" class="s3-path burned-path">
              <span>⚡ {{ asset.processedS3Url }}</span>
            </div>

            <div class="asset-metadata">
              <span>📼 {{ asset.cameraRoll }}</span>
              <span>⏱️ {{ formatSeconds(asset.durationSeconds) }}</span>
              <span>🎞️ {{ asset.fps }} fps</span>
              <span>💬 {{ (asset.comments || []).length }} notes</span>
            </div>

            <div v-if="asset.scriptDialogue" class="dialogue-snippet">
              "{{ asset.scriptDialogue }}"
            </div>

            <div class="asset-card-actions">
              <button 
                v-if="asset.status === 'UPLOADED'" 
                class="btn-action btn-burnin" 
                @click.stop="triggerBurnIn(asset)"
                title="Simulate FFmpeg subtitle burn-in worker"
              >
                ⚡ Run Script Burn-In
              </button>

              <button 
                v-else-if="asset.status === 'READY_FOR_REVIEW'" 
                class="btn-action btn-approve" 
                @click.stop="updateStatus(asset, 'APPROVED')"
                title="Picture Lock"
              >
                🔒 Picture Lock
              </button>

              <button 
                class="btn-action-del" 
                @click.stop="deleteAsset(asset.id, asset.title)"
                title="Delete Asset"
              >
                🗑️
              </button>
            </div>
          </div>

          <div v-if="filteredAssets.length === 0" class="empty-bin">
            No footage assets match your search filter.
          </div>
        </div>
      </section>

      <!-- Right: Director Review Monitor & Timecoded Notes -->
      <section class="review-panel">
        <div v-if="selectedAsset" class="monitor-wrapper">
          <!-- Interactive Cinema Viewport -->
          <div class="cinema-viewport">
            <div class="viewport-overlay-top">
              <div class="rec-indicator">
                <span class="rec-dot" :class="{ 'playing': isPlaying }"></span>
                <span>{{ isPlaying ? 'PLAY' : 'PAUSE' }}</span>
              </div>
              <span class="tc-display">{{ formattedCurrentTimecode }}</span>
              <span class="lens-tag">{{ selectedAsset.sceneId }} | TK {{ selectedAsset.takeNumber }}</span>
            </div>

            <div class="viewport-screen">
              <div class="cinema-title">{{ selectedAsset.title }}</div>
              <div class="cinema-roll">{{ selectedAsset.cameraRoll }} • {{ selectedAsset.fps }} FPS • 1080p Proxy</div>

              <!-- Script Subtitle Dialogue Burn-In -->
              <div class="burned-subtitle">
                <span class="burn-tag">
                  {{ selectedAsset.processedS3Url ? '⚡ SCRIPT BURNED IN' : 'RAW WORK PROXY' }}
                </span>
                <p>{{ selectedAsset.scriptDialogue || 'No screenplay dialogue assigned.' }}</p>
              </div>
            </div>

            <!-- Video Scrubber & Play Controls -->
            <div class="viewport-controls">
              <button class="btn-play" @click="togglePlay">
                {{ isPlaying ? '⏸️ Pause' : '▶️ Play Cut' }}
              </button>
              <input 
                type="range" 
                min="0" 
                :max="selectedAsset.durationSeconds || 150" 
                v-model.number="currentSeconds" 
                class="scrubber-slider" 
              />
              <span class="duration-tag">
                {{ formatSeconds(currentSeconds) }} / {{ formatSeconds(selectedAsset.durationSeconds) }}
              </span>
            </div>
          </div>

          <!-- Timecoded Feedback Collaboration Panel -->
          <div class="notes-panel">
            <div class="notes-header">
              <div>
                <h3 class="notes-title">Timecoded Collaboration Thread</h3>
                <p class="notes-sub">Director, Editor, Sound Supervisor & Colorist Notes</p>
              </div>
              <button class="btn-add-note" @click="openAddNoteModal">
                + Drop Note @ {{ formattedCurrentTimecode }}
              </button>
            </div>

            <!-- Notes List -->
            <div class="notes-stream">
              <div 
                v-for="note in (selectedAsset.comments || [])" 
                :key="note.id" 
                class="note-item"
                @click="jumpToTimecode(note.timestampSeconds)"
              >
                <div class="note-timecode" title="Click to jump player to this timecode">
                  {{ note.timecode }}
                </div>
                <div class="note-body">
                  <div class="note-meta">
                    <span class="dept-badge" :class="'dept-' + (note.department || 'editorial').toLowerCase()">
                      {{ note.department }}
                    </span>
                    <span class="author-tag">{{ note.userId }}</span>
                  </div>
                  <div class="note-text">{{ note.commentText }}</div>
                </div>
                <button 
                  class="btn-note-del" 
                  @click.stop="deleteComment(note.id)" 
                  title="Delete note"
                >
                  ✕
                </button>
              </div>

              <div v-if="!selectedAsset.comments || selectedAsset.comments.length === 0" class="empty-notes">
                No time-coded feedback on this clip yet. Click <strong>+ Drop Note</strong> to leave a note at the current playhead position.
              </div>
            </div>
          </div>
        </div>

        <div v-else class="no-selection-state">
          <div class="film-reel-icon">🎬</div>
          <h3>Select a Footage Asset from the Bin</h3>
          <p>Choose any clip on the left to review its proxy, inspect dialogue subtitles, and drop time-coded feedback.</p>
        </div>
      </section>
    </div>

    <!-- Modal: Ingest New Footage Asset (Service 1) -->
    <div v-if="showIngestModal" class="modal-overlay" @click.self="showIngestModal = false">
      <div class="modal-box">
        <div class="modal-header">
          <h3>Ingest Raw Footage Asset (Service 1)</h3>
          <button class="modal-close" @click="showIngestModal = false">✕</button>
        </div>
        <form @submit.prevent="submitIngestAsset">
          <div class="form-row">
            <div class="form-group flex-2">
              <label>Scene / Take Title</label>
              <input v-model="newAsset.title" type="text" required placeholder="e.g. Alley Shootout Head-On" />
            </div>
            <div class="form-group flex-1">
              <label>Scene ID</label>
              <input v-model="newAsset.sceneId" type="text" required placeholder="Scene 14B" />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group flex-1">
              <label>Take #</label>
              <input v-model.number="newAsset.takeNumber" type="number" required min="1" />
            </div>
            <div class="form-group flex-1">
              <label>Camera Roll</label>
              <input v-model="newAsset.cameraRoll" type="text" required placeholder="Roll A-04" />
            </div>
            <div class="form-group flex-1">
              <label>Duration (sec)</label>
              <input v-model.number="newAsset.durationSeconds" type="number" required min="10" />
            </div>
          </div>

          <div class="form-group">
            <label>Object Storage Proxy Path (MinIO / S3)</label>
            <input v-model="newAsset.s3BucketUrl" type="text" placeholder="s3://collabcut-raw/scene14b_tk3.mp4" />
          </div>

          <div class="form-group">
            <label>Script Screenplay Dialogue (for Burn-In)</label>
            <textarea 
              v-model="newAsset.scriptDialogue" 
              rows="3" 
              placeholder='e.g. MARCUS: "If we leave now, there is no turning back."'
            ></textarea>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="showIngestModal = false">Cancel</button>
            <button type="submit" class="btn-primary" :disabled="submitting">
              {{ submitting ? 'Ingesting...' : 'Ingest & Emit Event' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Modal: Drop Timecoded Note (Service 3) -->
    <div v-if="showAddNoteModal" class="modal-overlay" @click.self="showAddNoteModal = false">
      <div class="modal-box">
        <div class="modal-header">
          <h3>Drop Timecoded Feedback Note (Service 3)</h3>
          <button class="modal-close" @click="showAddNoteModal = false">✕</button>
        </div>
        <form @submit.prevent="submitAddNote">
          <div class="form-row">
            <div class="form-group flex-1">
              <label>Timecode</label>
              <input v-model="newNote.timecode" type="text" required placeholder="00:01:24:12" />
            </div>
            <div class="form-group flex-1">
              <label>Department</label>
              <select v-model="newNote.department">
                <option value="EDITORIAL">EDITORIAL</option>
                <option value="COLOR_GRADE">COLOR GRADE</option>
                <option value="SOUND_DESIGN">SOUND DESIGN</option>
                <option value="VFX">VFX</option>
              </select>
            </div>
          </div>

          <div class="form-group">
            <label>Author / Role</label>
            <input v-model="newNote.userId" type="text" placeholder="Director Nolan" />
          </div>

          <div class="form-group">
            <label>Feedback Message</label>
            <textarea 
              v-model="newNote.commentText" 
              rows="3" 
              required 
              placeholder="e.g. Trim 6 frames off head of take. The pause feels too prolonged."
            ></textarea>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="showAddNoteModal = false">Cancel</button>
            <button type="submit" class="btn-primary" :disabled="submitting">
              {{ submitting ? 'Saving...' : 'Save Note to PostgreSQL' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Footer -->
    <footer class="studio-footer">
      <span>CollabCut: Independent Film Post-Production Tracker • Kubernetes + Minikube • Podman Driver</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';

const assets = ref([]);
const events = ref([]);
const selectedAsset = ref(null);
const loading = ref(false);
const submitting = ref(false);
const searchQuery = ref('');
const filterStatus = ref('ALL');
const showIngestModal = ref(false);
const showAddNoteModal = ref(false);
const showEventFeed = ref(false);

const isPlaying = ref(false);
const currentSeconds = ref(74);
let playInterval = null;

const newAsset = ref({
  title: '',
  sceneId: '',
  takeNumber: 1,
  cameraRoll: 'Roll A-01',
  durationSeconds: 150,
  s3BucketUrl: '',
  scriptDialogue: ''
});

const newNote = ref({
  timecode: '00:01:14:08',
  timestampSeconds: 74,
  userId: 'Director Nolan',
  department: 'EDITORIAL',
  commentText: ''
});

const toast = ref({
  show: false,
  message: '',
  type: 'success'
});

function showToast(msg, type = 'success') {
  toast.value = { show: true, message: msg, type };
  setTimeout(() => { toast.value.show = false; }, 4000);
}

// Convert seconds to Timecode HH:MM:SS:FF
const formattedCurrentTimecode = computed(() => {
  const sec = currentSeconds.value || 0;
  const hrs = Math.floor(sec / 3600).toString().padStart(2, '0');
  const mins = Math.floor((sec % 3600) / 60).toString().padStart(2, '0');
  const s = (sec % 60).toString().padStart(2, '0');
  const frames = '12';
  return `${hrs}:${mins}:${s}:${frames}`;
});

function togglePlay() {
  isPlaying.value = !isPlaying.value;
  if (isPlaying.value) {
    playInterval = setInterval(() => {
      const max = (selectedAsset.value && selectedAsset.value.durationSeconds) || 150;
      if (currentSeconds.value >= max) {
        currentSeconds.value = 0;
      } else {
        currentSeconds.value++;
      }
    }, 1000);
  } else {
    clearInterval(playInterval);
  }
}

function jumpToTimecode(sec) {
  currentSeconds.value = sec;
}

function openAddNoteModal() {
  newNote.value.timecode = formattedCurrentTimecode.value;
  newNote.value.timestampSeconds = currentSeconds.value;
  showAddNoteModal.value = true;
}

async function fetchAssets() {
  try {
    const res = await fetch('/api/assets');
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    assets.value = data;

    if (selectedAsset.value) {
      selectedAsset.value = assets.value.find(a => a.id === selectedAsset.value.id) || assets.value[0] || null;
    } else if (assets.value.length > 0) {
      selectedAsset.value = assets.value[0];
    }
  } catch (err) {
    console.error('Fetch assets failed:', err);
  }
}

async function fetchEvents() {
  try {
    const res = await fetch('/api/events');
    if (res.ok) {
      events.value = await res.json();
    }
  } catch (err) {
    console.error('Fetch events failed:', err);
  }
}

async function fetchAll() {
  loading.value = true;
  await Promise.all([fetchAssets(), fetchEvents()]);
  loading.value = false;
}

function selectAsset(asset) {
  selectedAsset.value = asset;
  currentSeconds.value = 0;
}

async function triggerBurnIn(asset) {
  try {
    const res = await fetch(`/api/assets/${asset.id}/burn-in`, { method: 'POST' });
    if (!res.ok) throw new Error(`Burn-in failed: ${res.status}`);
    showToast(`Worker burned dialogue into proxy! Emitted 'footage.processed'. Ready for Review!`, 'success');
    await fetchAll();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function updateStatus(asset, newStatus) {
  try {
    const res = await fetch(`/api/assets/${asset.id}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: newStatus })
    });
    if (!res.ok) throw new Error(`Update failed: ${res.status}`);
    showToast(`${asset.sceneId} locked! Status updated to ${newStatus}.`, 'success');
    await fetchAll();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function submitIngestAsset() {
  submitting.value = true;
  try {
    const res = await fetch('/api/assets', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newAsset.value)
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.error || `Error ${res.status}`);
    }

    const created = await res.json();
    showToast(`Asset '${created.sceneId}' Ingested! Emitted 'footage.uploaded' event.`, 'success');
    newAsset.value = { title: '', sceneId: '', takeNumber: 1, cameraRoll: 'Roll A-01', durationSeconds: 150, s3BucketUrl: '', scriptDialogue: '' };
    showIngestModal.value = false;
    await fetchAll();
    selectedAsset.value = created;
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    submitting.value = false;
  }
}

async function submitAddNote() {
  if (!selectedAsset.value) return;
  submitting.value = true;
  try {
    const payload = {
      asset_id: selectedAsset.value.id,
      timestamp_seconds: newNote.value.timestampSeconds,
      timecode: newNote.value.timecode,
      user_id: newNote.value.userId,
      department: newNote.value.department,
      comment_text: newNote.value.commentText
    };

    const res = await fetch('/api/comments', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.error || `Error ${res.status}`);
    }

    showToast(`Timecoded note saved to PostgreSQL! Emitted 'comment.created'.`, 'success');
    newNote.value.commentText = '';
    showAddNoteModal.value = false;
    await fetchAll();
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    submitting.value = false;
  }
}

async function deleteAsset(id, title) {
  if (!confirm(`Are you sure you want to delete asset '${title}' from PostgreSQL?`)) return;

  try {
    const res = await fetch(`/api/assets/${id}`, { method: 'DELETE' });
    if (!res.ok) throw new Error(`Delete failed: ${res.status}`);
    showToast(`Asset deleted from PostgreSQL.`, 'success');
    await fetchAll();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function deleteComment(id) {
  try {
    const res = await fetch(`/api/comments/${id}`, { method: 'DELETE' });
    if (!res.ok) throw new Error(`Delete failed`);
    showToast(`Note deleted.`, 'success');
    await fetchAll();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

const filteredAssets = computed(() => {
  return assets.value.filter(a => {
    const q = searchQuery.value.toLowerCase();
    const matchesSearch = 
      a.title.toLowerCase().includes(q) ||
      a.sceneId.toLowerCase().includes(q) ||
      a.cameraRoll.toLowerCase().includes(q);
    const matchesFilter = filterStatus.value === 'ALL' || a.status === filterStatus.value;
    return matchesSearch && matchesFilter;
  });
});

const totalCommentsCount = computed(() => {
  return assets.value.reduce((acc, a) => acc + (a.comments ? a.comments.length : 0), 0);
});

function countStatus(st) {
  return assets.value.filter(a => a.status === st).length;
}

function formatStatus(st) {
  switch (st) {
    case 'UPLOADED': return 'Uploaded';
    case 'PROCESSING_BURN_IN': return 'Processing';
    case 'READY_FOR_REVIEW': return 'Ready for Review';
    case 'APPROVED': return 'Picture Lock';
    default: return st;
  }
}

function formatSeconds(sec) {
  if (!sec && sec !== 0) return '00:00';
  const m = Math.floor(sec / 60).toString().padStart(2, '0');
  const s = (sec % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleTimeString();
}

function formatPayload(raw) {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}

onMounted(() => {
  fetchAll();
});

onUnmounted(() => {
  if (playInterval) clearInterval(playInterval);
});
</script>

<style scoped>
.collabcut-app {
  max-width: 1440px;
  margin: 0 auto;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  color: #f3f4f6;
  font-family: 'Plus Jakarta Sans', sans-serif;
}

/* Header */
.studio-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  padding-bottom: 1rem;
}

.studio-brand {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.clapper-icon {
  font-size: 2.2rem;
  background: rgba(245, 158, 11, 0.15);
  border: 1px solid rgba(245, 158, 11, 0.3);
  border-radius: 10px;
  padding: 0.4rem 0.6rem;
}

.brand-title {
  font-size: 1.6rem;
  font-weight: 800;
  letter-spacing: -0.03em;
  background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.project-tag {
  font-size: 0.85rem;
  color: #9ca3af;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.btn-event-feed {
  background: rgba(6, 182, 212, 0.15);
  border: 1px solid rgba(6, 182, 212, 0.3);
  color: #22d3ee;
  padding: 0.5rem 0.85rem;
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-ingest {
  background: linear-gradient(135deg, #d97706 0%, #b45309 100%);
  color: white;
  border: none;
  padding: 0.55rem 1.1rem;
  border-radius: 6px;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-icon {
  background: #111827;
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #f3f4f6;
  padding: 0.5rem 0.75rem;
  border-radius: 6px;
  cursor: pointer;
}

.spin {
  animation: spin 1s linear infinite;
  display: inline-block;
}
@keyframes spin { 100% { transform: rotate(360deg); } }

/* Architecture Banner */
.arch-banner {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  padding: 0.75rem 1.25rem;
  display: flex;
  align-items: center;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.arch-label {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  color: #9ca3af;
  letter-spacing: 0.05em;
}

.arch-flow {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.arch-node {
  background: #111827;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  padding: 0.4rem 0.75rem;
  display: flex;
  flex-direction: column;
}

.node-title { font-size: 0.8rem; font-weight: 700; color: #fbbf24; }
.node-sub { font-size: 0.65rem; color: #9ca3af; font-family: 'JetBrains Mono', monospace; }

.arch-arrow {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  color: #6b7280;
  font-size: 0.9rem;
}

.event-tag {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
  border: 1px dashed rgba(245, 158, 11, 0.4);
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
  font-size: 0.65rem;
  font-family: 'JetBrains Mono', monospace;
}

/* KPI Grid */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 1rem;
}

.kpi-card {
  background: rgba(17, 24, 39, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  padding: 1rem;
  display: flex;
  align-items: center;
  gap: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.kpi-card:hover, .kpi-card.active {
  border-color: #f59e0b;
  background: rgba(245, 158, 11, 0.08);
}

.kpi-icon {
  font-size: 1.6rem;
  padding: 0.5rem;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
}
.ingest-color { background: rgba(59, 130, 246, 0.15); }
.review-color { background: rgba(168, 85, 247, 0.15); }
.locked-color { background: rgba(16, 185, 129, 0.15); }
.notes-color { background: rgba(6, 182, 212, 0.15); }

.kpi-num {
  font-size: 1.4rem;
  font-weight: 800;
  font-family: 'JetBrains Mono', monospace;
}
.kpi-label { font-size: 0.75rem; color: #9ca3af; text-transform: uppercase; font-weight: 600; }

/* Toast */
.studio-toast {
  padding: 0.75rem 1rem;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
}
.studio-toast.success { background: rgba(16, 185, 129, 0.15); border: 1px solid #10b981; color: #34d399; }
.studio-toast.error { background: rgba(239, 68, 68, 0.15); border: 1px solid #ef4444; color: #f87171; }

/* Event Broker Drawer */
.event-drawer {
  background: #030712;
  border: 1px solid #06b6d4;
  border-radius: 10px;
  padding: 1rem;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.7);
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.drawer-title { font-size: 0.95rem; font-weight: 700; color: #22d3ee; }
.btn-close-sm { background: transparent; border: none; color: #9ca3af; cursor: pointer; }

.event-list {
  display: flex;
  gap: 0.75rem;
  overflow-x: auto;
  padding-bottom: 0.5rem;
}

.event-card {
  min-width: 280px;
  background: #111827;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  padding: 0.65rem;
}

.event-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.4rem;
}

.event-badge {
  background: rgba(6, 182, 212, 0.2);
  color: #22d3ee;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
  font-size: 0.7rem;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 700;
}

.event-time { font-size: 0.65rem; color: #6b7280; font-family: 'JetBrains Mono', monospace; }

.event-card pre {
  font-size: 0.7rem;
  color: #94a3b8;
  line-height: 1.3;
}

/* Split Workspace */
.studio-workspace {
  display: grid;
  grid-template-columns: 460px 1fr;
  gap: 1.5rem;
  min-height: 680px;
}

@media (max-width: 1024px) {
  .studio-workspace { grid-template-columns: 1fr; }
}

/* Bin Panel */
.bin-panel {
  background: rgba(17, 24, 39, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-topbar {
  padding: 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.bin-title { display: flex; justify-content: space-between; font-weight: 700; }
.count-pill { background: rgba(255, 255, 255, 0.1); padding: 0.15rem 0.5rem; border-radius: 9999px; font-size: 0.75rem; }

.bin-search input {
  width: 100%;
  background: #0b0f19;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  padding: 0.45rem 0.75rem;
  color: white;
  font-size: 0.85rem;
  outline: none;
}

.asset-feed {
  padding: 1rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  max-height: 720px;
}

.asset-card {
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;
  padding: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.asset-card:hover {
  border-color: rgba(245, 158, 11, 0.5);
  background: rgba(30, 41, 59, 0.9);
}

.asset-card.selected {
  border-color: #f59e0b;
  background: rgba(245, 158, 11, 0.07);
  box-shadow: 0 0 15px rgba(245, 158, 11, 0.15);
}

.asset-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.35rem;
}

.scene-badge { font-family: 'JetBrains Mono', monospace; font-weight: 700; font-size: 0.8rem; color: #fbbf24; }

.status-pill {
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}
.status-uploaded { background: rgba(59, 130, 246, 0.2); color: #60a5fa; }
.status-ready_for_review { background: rgba(168, 85, 247, 0.2); color: #c084fc; }
.status-approved { background: rgba(16, 185, 129, 0.2); color: #34d399; }

.asset-title { font-weight: 600; font-size: 0.95rem; margin-bottom: 0.35rem; }

.s3-path {
  font-size: 0.7rem;
  color: #64748b;
  font-family: 'JetBrains Mono', monospace;
  margin-bottom: 0.2rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.burned-path { color: #f59e0b; font-weight: 600; }

.asset-metadata {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  font-size: 0.75rem;
  color: #9ca3af;
  margin: 0.4rem 0;
  font-family: 'JetBrains Mono', monospace;
}

.dialogue-snippet {
  background: rgba(0, 0, 0, 0.4);
  border-left: 2px solid #f59e0b;
  padding: 0.4rem 0.6rem;
  font-size: 0.75rem;
  color: #d1d5db;
  font-style: italic;
  margin-bottom: 0.6rem;
}

.asset-card-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.4rem;
}

.btn-action {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.35rem 0.75rem;
  border-radius: 4px;
  border: none;
  cursor: pointer;
}
.btn-burnin { background: #d97706; color: white; }
.btn-approve { background: #059669; color: white; }
.btn-action-del { background: transparent; border: none; cursor: pointer; opacity: 0.6; }
.btn-action-del:hover { opacity: 1; }

/* Right Review Monitor */
.review-panel {
  background: rgba(17, 24, 39, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.cinema-viewport {
  background: #000;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding: 1.25rem;
  position: relative;
  min-height: 290px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background-image: radial-gradient(circle at center, rgba(30, 41, 59, 0.5) 0%, #000000 80%);
}

.viewport-overlay-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.85rem;
}

.rec-indicator {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.75rem;
  font-weight: 700;
}

.rec-dot {
  width: 10px;
  height: 10px;
  background: #64748b;
  border-radius: 50%;
}
.rec-dot.playing {
  background: #ef4444;
  box-shadow: 0 0 10px #ef4444;
}

.tc-display { font-weight: 700; color: #fbbf24; font-size: 1rem; }

.viewport-screen { text-align: center; padding: 1.5rem 0; }
.cinema-title { font-size: 1.4rem; font-weight: 800; }
.cinema-roll { font-size: 0.8rem; color: #64748b; font-family: 'JetBrains Mono', monospace; }

.burned-subtitle {
  margin: 1.25rem auto 0 auto;
  max-width: 620px;
  background: rgba(0, 0, 0, 0.85);
  border: 1px dashed rgba(245, 158, 11, 0.4);
  border-radius: 6px;
  padding: 0.75rem 1rem;
}

.burn-tag {
  display: inline-block;
  font-size: 0.65rem;
  font-family: 'JetBrains Mono', monospace;
  color: #fbbf24;
  font-weight: 700;
  margin-bottom: 0.3rem;
}

.burned-subtitle p { font-size: 0.95rem; color: #fef08a; font-style: italic; }

.viewport-controls {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.btn-play {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
  padding: 0.4rem 0.8rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.scrubber-slider {
  flex: 1;
  accent-color: #f59e0b;
  cursor: pointer;
}

.duration-tag {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
  color: #94a3b8;
  white-space: nowrap;
}

/* Notes Section */
.notes-panel {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.notes-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.notes-title { font-size: 1.1rem; font-weight: 700; }
.notes-sub { font-size: 0.8rem; color: #9ca3af; }

.btn-add-note {
  background: rgba(245, 158, 11, 0.15);
  border: 1px solid rgba(245, 158, 11, 0.4);
  color: #fbbf24;
  padding: 0.45rem 0.85rem;
  border-radius: 6px;
  font-weight: 700;
  font-size: 0.8rem;
  cursor: pointer;
}

.notes-stream {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  overflow-y: auto;
  max-height: 340px;
}

.note-item {
  display: flex;
  gap: 1rem;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 0.85rem;
  cursor: pointer;
  position: relative;
}

.note-item:hover {
  background: rgba(30, 41, 59, 0.8);
  border-color: rgba(245, 158, 11, 0.3);
}

.note-timecode {
  font-family: 'JetBrains Mono', monospace;
  font-weight: 700;
  color: #fbbf24;
  font-size: 0.85rem;
  white-space: nowrap;
}

.note-body { flex: 1; }

.note-meta {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-bottom: 0.35rem;
}

.dept-badge {
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
  font-family: 'JetBrains Mono', monospace;
}
.dept-editorial { background: rgba(59, 130, 246, 0.2); color: #60a5fa; }
.dept-color_grade { background: rgba(245, 158, 11, 0.2); color: #fbbf24; }
.dept-sound_design { background: rgba(16, 185, 129, 0.2); color: #34d399; }
.dept-vfx { background: rgba(168, 85, 247, 0.2); color: #c084fc; }

.author-tag { font-size: 0.75rem; color: #9ca3af; font-weight: 600; }
.note-text { font-size: 0.85rem; color: #e5e7eb; line-height: 1.4; }

.btn-note-del {
  background: transparent;
  border: none;
  color: #64748b;
  cursor: pointer;
}
.btn-note-del:hover { color: #ef4444; }

.empty-notes { text-align: center; color: #64748b; padding: 2.5rem; font-size: 0.85rem; }
.no-selection-state { margin: auto; text-align: center; padding: 4rem 2rem; color: #64748b; }
.film-reel-icon { font-size: 3rem; margin-bottom: 1rem; }

/* Modals */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal-box {
  background: #111827;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  width: 100%;
  max-width: 520px;
  padding: 1.5rem;
}

.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.25rem; }
.modal-close { background: transparent; border: none; color: #9ca3af; font-size: 1.2rem; cursor: pointer; }

.form-row { display: flex; gap: 0.75rem; }
.flex-1 { flex: 1; }
.flex-2 { flex: 2; }

.form-group { margin-bottom: 1rem; }
.form-group label { display: block; font-size: 0.8rem; color: #9ca3af; margin-bottom: 0.35rem; font-weight: 600; }
.form-group input, .form-group select, .form-group textarea {
  width: 100%;
  background: #030712;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  padding: 0.55rem 0.75rem;
  color: white;
  font-family: inherit;
  font-size: 0.85rem;
  outline: none;
}
.form-group input:focus, .form-group select:focus, .form-group textarea:focus { border-color: #f59e0b; }

.modal-footer { display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem; }
.btn-cancel { background: transparent; border: 1px solid rgba(255, 255, 255, 0.1); color: #9ca3af; padding: 0.5rem 1rem; border-radius: 6px; cursor: pointer; }
.btn-primary { background: linear-gradient(135deg, #d97706 0%, #b45309 100%); color: white; border: none; padding: 0.55rem 1.25rem; border-radius: 6px; font-weight: 700; font-size: 0.85rem; cursor: pointer; }

.studio-footer { text-align: center; font-size: 0.8rem; color: #64748b; padding-top: 1rem; border-top: 1px solid rgba(255, 255, 255, 0.08); }
</style>
