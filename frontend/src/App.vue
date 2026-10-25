<template>
  <div class="collabcut-app">
    <!-- Studio Header -->
    <header class="studio-header">
      <div class="studio-brand">
        <div class="clapper-icon">🎬</div>
        <div>
          <div class="brand-title">CollabCut Studio</div>
          <div class="project-tag">Project: <em>"Chronicles of Dawn"</em> • Feature Film Post-Pipeline</div>
        </div>
      </div>
      <div class="header-controls">
        <div class="k8s-badge">
          <span class="live-pulse"></span>
          <span>devops-lab / Minikube</span>
        </div>
        <button class="btn-ingest" @click="showIngestModal = true">
          <span>+ Ingest Footage</span>
        </button>
        <button class="btn-icon" :disabled="loading" @click="fetchAll" title="Reload pipeline data">
          <span :class="{ 'spin': loading }">🔄</span>
        </button>
      </div>
    </header>

    <!-- Pipeline KPI Bar -->
    <section class="kpi-grid">
      <div class="kpi-card" :class="{ 'active': filterStatus === 'ALL' }" @click="filterStatus = 'ALL'">
        <div class="kpi-icon">🎞️</div>
        <div>
          <div class="kpi-num">{{ clips.length }}</div>
          <div class="kpi-label">Total Clips</div>
        </div>
      </div>

      <div class="kpi-card" :class="{ 'active': filterStatus === 'INGESTED' }" @click="filterStatus = 'INGESTED'">
        <div class="kpi-icon ingest-color">📥</div>
        <div>
          <div class="kpi-num">{{ countStatus('INGESTED') }}</div>
          <div class="kpi-label">Ingested</div>
        </div>
      </div>

      <div class="kpi-card" :class="{ 'active': filterStatus === 'SCRIPT_BURN_IN' }" @click="filterStatus = 'SCRIPT_BURN_IN'">
        <div class="kpi-icon burnin-color">⚡</div>
        <div>
          <div class="kpi-num">{{ countStatus('SCRIPT_BURN_IN') }}</div>
          <div class="kpi-label">Script Burn-In</div>
        </div>
      </div>

      <div class="kpi-card" :class="{ 'active': filterStatus === 'IN_REVIEW' }" @click="filterStatus = 'IN_REVIEW'">
        <div class="kpi-icon review-color">👁️</div>
        <div>
          <div class="kpi-num">{{ countStatus('IN_REVIEW') }}</div>
          <div class="kpi-label">In Review</div>
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
          <div class="kpi-num">{{ totalNotesCount }}</div>
          <div class="kpi-label">Director Notes</div>
        </div>
      </div>
    </section>

    <!-- Toast Banner -->
    <div v-if="toast.show" class="studio-toast" :class="toast.type">
      {{ toast.message }}
    </div>

    <!-- Main Studio Workspace (Split View) -->
    <div class="studio-workspace">
      <!-- Left: Clip Ingest Bin & List -->
      <section class="bin-panel">
        <div class="panel-topbar">
          <div class="bin-title">
            <span>Footage Ingest Bin</span>
            <span class="count-pill">{{ filteredClips.length }}</span>
          </div>
          <div class="bin-search">
            <input 
              v-model="searchQuery" 
              type="text" 
              placeholder="Search scene, take, roll..." 
            />
          </div>
        </div>

        <div class="clip-feed">
          <div 
            v-for="clip in filteredClips" 
            :key="clip.id"
            class="clip-card"
            :class="{ 'selected': selectedClip && selectedClip.id === clip.id }"
            @click="selectClip(clip)"
          >
            <div class="clip-card-header">
              <span class="scene-badge">{{ clip.scene }} • Take {{ clip.take }}</span>
              <span class="status-pill" :class="'status-' + clip.status.toLowerCase()">
                {{ formatStatus(clip.status) }}
              </span>
            </div>

            <div class="clip-title">{{ clip.title }}</div>

            <div class="clip-metadata">
              <span>📼 {{ clip.cameraRoll }}</span>
              <span>⏱️ {{ clip.duration }}</span>
              <span>🎞️ {{ clip.fps }} fps</span>
              <span>💬 {{ (clip.notes || []).length }} notes</span>
            </div>

            <div v-if="clip.scriptDialogue" class="dialogue-snippet">
              "{{ clip.scriptDialogue }}"
            </div>

            <div class="clip-card-actions">
              <button 
                v-if="clip.status === 'INGESTED'" 
                class="btn-action btn-burnin" 
                @click.stop="triggerBurnIn(clip)"
                title="Simulate FFmpeg burning script dialogue into proxy file"
              >
                ⚡ Burn-In Script
              </button>

              <button 
                v-else-if="clip.status === 'IN_REVIEW'" 
                class="btn-action btn-approve" 
                @click.stop="updateStatus(clip, 'APPROVED')"
                title="Director Lock"
              >
                🔒 Picture Lock
              </button>

              <button 
                class="btn-action-del" 
                @click.stop="deleteClip(clip.id, clip.title)"
                title="Delete Clip"
              >
                🗑️
              </button>
            </div>
          </div>

          <div v-if="filteredClips.length === 0" class="empty-bin">
            No footage clips match your filter.
          </div>
        </div>
      </section>

      <!-- Right: Director Review Monitor & Timecoded Notes -->
      <section class="review-panel">
        <div v-if="selectedClip" class="monitor-wrapper">
          <!-- Mock Cinema Viewport -->
          <div class="cinema-viewport">
            <div class="viewport-overlay-top">
              <span class="rec-dot"></span>
              <span class="tc-display">TC: {{ mockTimecode }}</span>
              <span class="lens-tag">{{ selectedClip.scene }} | TK {{ selectedClip.take }}</span>
            </div>

            <div class="viewport-screen">
              <div class="cinema-title">{{ selectedClip.title }}</div>
              <div class="cinema-roll">{{ selectedClip.cameraRoll }} • 24.00 FPS • PROXY 1080p</div>

              <!-- Subtitle Dialogue Burn-In Display -->
              <div class="burned-subtitle">
                <span class="burn-tag">SCRIPT BURN-IN</span>
                <p>{{ selectedClip.scriptDialogue || 'No dialogue assigned to this take.' }}</p>
              </div>
            </div>

            <div class="viewport-timeline">
              <div class="timeline-bar">
                <div class="timeline-progress" style="width: 42%;"></div>
                <div class="timeline-scrubber" style="left: 42%;"></div>
              </div>
            </div>
          </div>

          <!-- Timecoded Feedback Panel -->
          <div class="notes-panel">
            <div class="notes-header">
              <div>
                <h3 class="notes-title">Timecoded Director & Crew Feedback</h3>
                <p class="notes-sub">Frame-accurate feedback linked to this scene cut</p>
              </div>
              <button class="btn-add-note" @click="showAddNoteModal = true">
                + Drop Timecode Note
              </button>
            </div>

            <!-- Notes List -->
            <div class="notes-stream">
              <div 
                v-for="note in (selectedClip.notes || [])" 
                :key="note.id" 
                class="note-item"
              >
                <div class="note-timecode">{{ note.timecode }}</div>
                <div class="note-body">
                  <div class="note-meta">
                    <span class="dept-badge" :class="'dept-' + note.department.toLowerCase()">
                      {{ note.department }}
                    </span>
                    <span class="author-tag">{{ note.author }}</span>
                  </div>
                  <div class="note-text">{{ note.comment }}</div>
                </div>
              </div>

              <div v-if="!selectedClip.notes || selectedClip.notes.length === 0" class="empty-notes">
                No review notes on this clip yet. Click <strong>+ Drop Timecode Note</strong> to leave feedback.
              </div>
            </div>
          </div>
        </div>

        <div v-else class="no-selection-state">
          <div class="film-reel-icon">🎬</div>
          <h3>Select a Footage Clip from the Bin</h3>
          <p>Choose any clip on the left to review its proxy, inspect dialogue subtitles, and manage time-coded feedback.</p>
        </div>
      </section>
    </div>

    <!-- Modal: Ingest New Footage Clip -->
    <div v-if="showIngestModal" class="modal-overlay" @click.self="showIngestModal = false">
      <div class="modal-box">
        <div class="modal-header">
          <h3>Ingest Footage Take</h3>
          <button class="modal-close" @click="showIngestModal = false">✕</button>
        </div>
        <form @submit.prevent="submitIngestClip">
          <div class="form-row">
            <div class="form-group flex-2">
              <label>Clip / Scene Title</label>
              <input v-model="newClip.title" type="text" required placeholder="e.g. Warehouse Ambush Wide" />
            </div>
            <div class="form-group flex-1">
              <label>Scene #</label>
              <input v-model="newClip.scene" type="text" required placeholder="Scene 18A" />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group flex-1">
              <label>Take #</label>
              <input v-model.number="newClip.take" type="number" required min="1" />
            </div>
            <div class="form-group flex-1">
              <label>Camera Roll</label>
              <input v-model="newClip.cameraRoll" type="text" required placeholder="Roll A-05" />
            </div>
            <div class="form-group flex-1">
              <label>Duration</label>
              <input v-model="newClip.duration" type="text" placeholder="02:15" />
            </div>
          </div>

          <div class="form-group">
            <label>Script Dialogue (for Automatic Burn-In)</label>
            <textarea 
              v-model="newClip.scriptDialogue" 
              rows="3" 
              placeholder='e.g. DETECTIVE: "Where were you on the night of the 14th?"'
            ></textarea>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="showIngestModal = false">Cancel</button>
            <button type="submit" class="btn-primary" :disabled="submitting">
              {{ submitting ? 'Ingesting...' : 'Ingest Clip' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Modal: Add Timecoded Note -->
    <div v-if="showAddNoteModal" class="modal-overlay" @click.self="showAddNoteModal = false">
      <div class="modal-box">
        <div class="modal-header">
          <h3>Drop Timecoded Note</h3>
          <button class="modal-close" @click="showAddNoteModal = false">✕</button>
        </div>
        <form @submit.prevent="submitAddNote">
          <div class="form-row">
            <div class="form-group flex-1">
              <label>Timecode (HH:MM:SS:FF)</label>
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
            <input v-model="newNote.author" type="text" placeholder="e.g. Director Nolan" />
          </div>

          <div class="form-group">
            <label>Director / Review Note</label>
            <textarea 
              v-model="newNote.comment" 
              rows="3" 
              required 
              placeholder="e.g. Trim 4 frames from head of take before Elena turns around."
            ></textarea>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" @click="showAddNoteModal = false">Cancel</button>
            <button type="submit" class="btn-primary" :disabled="submitting">
              {{ submitting ? 'Posting...' : 'Save Timecoded Note' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Footer -->
    <footer class="studio-footer">
      <span>CollabCut: Independent Film Post-Production Tracker • Podman + Minikube • PostgreSQL 16</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';

const clips = ref([]);
const selectedClip = ref(null);
const loading = ref(false);
const submitting = ref(false);
const searchQuery = ref('');
const filterStatus = ref('ALL');
const showIngestModal = ref(false);
const showAddNoteModal = ref(false);
const mockTimecode = ref('00:01:24:18');

const newClip = ref({
  title: '',
  scene: '',
  take: 1,
  cameraRoll: 'Roll A-01',
  duration: '02:30',
  scriptDialogue: ''
});

const newNote = ref({
  timecode: '00:01:24:12',
  author: 'Director',
  department: 'EDITORIAL',
  comment: ''
});

const toast = ref({
  show: false,
  message: '',
  type: 'success'
});

function showToast(msg, type = 'success') {
  toast.value = { show: true, message: msg, type };
  setTimeout(() => {
    toast.value.show = false;
  }, 4000);
}

async function fetchClips() {
  loading.value = true;
  try {
    const res = await fetch('/api/clips');
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    clips.value = data;

    // Preserve selection
    if (selectedClip.value) {
      selectedClip.value = clips.value.find(c => c.id === selectedClip.value.id) || clips.value[0] || null;
    } else if (clips.value.length > 0) {
      selectedClip.value = clips.value[0];
    }
  } catch (err) {
    showToast(`Failed to load footage: ${err.message}`, 'error');
  } finally {
    loading.value = false;
  }
}

async function fetchAll() {
  await fetchClips();
}

function selectClip(clip) {
  selectedClip.value = clip;
}

async function triggerBurnIn(clip) {
  try {
    const res = await fetch(`/api/clips/${clip.id}/burn-in`, { method: 'POST' });
    if (!res.ok) throw new Error(`Burn-in failed: ${res.status}`);
    showToast(`Script dialogue burned into proxy for ${clip.scene}. Sent to Review!`, 'success');
    await fetchClips();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function updateStatus(clip, newStatus) {
  try {
    const res = await fetch(`/api/clips/${clip.id}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: newStatus })
    });
    if (!res.ok) throw new Error(`Update failed: ${res.status}`);
    showToast(`${clip.scene} locked! Status updated to ${newStatus}.`, 'success');
    await fetchClips();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function submitIngestClip() {
  submitting.value = true;
  try {
    const res = await fetch('/api/clips', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newClip.value)
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.error || `Error ${res.status}`);
    }

    const created = await res.json();
    showToast(`Take ${created.scene} Ingested successfully into PostgreSQL!`, 'success');
    newClip.value = { title: '', scene: '', take: 1, cameraRoll: 'Roll A-01', duration: '02:30', scriptDialogue: '' };
    showIngestModal.value = false;
    await fetchClips();
    selectedClip.value = created;
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    submitting.value = false;
  }
}

async function submitAddNote() {
  if (!selectedClip.value) return;
  submitting.value = true;
  try {
    const res = await fetch(`/api/clips/${selectedClip.value.id}/notes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newNote.value)
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.error || `Error ${res.status}`);
    }

    showToast(`Timecoded note at ${newNote.value.timecode} saved to PostgreSQL!`, 'success');
    newNote.value = { timecode: '00:01:24:12', author: 'Director', department: 'EDITORIAL', comment: '' };
    showAddNoteModal.value = false;
    await fetchClips();
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    submitting.value = false;
  }
}

async function deleteClip(id, title) {
  if (!confirm(`Are you sure you want to delete clip '${title}' and its feedback notes from database?`)) return;

  try {
    const res = await fetch(`/api/clips/${id}`, { method: 'DELETE' });
    if (!res.ok) throw new Error(`Delete failed: ${res.status}`);
    showToast(`Clip deleted from PostgreSQL.`, 'success');
    await fetchClips();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

const filteredClips = computed(() => {
  return clips.value.filter(c => {
    const query = searchQuery.value.toLowerCase();
    const matchesSearch = 
      c.title.toLowerCase().includes(query) ||
      c.scene.toLowerCase().includes(query) ||
      c.cameraRoll.toLowerCase().includes(query);
    const matchesFilter = filterStatus.value === 'ALL' || c.status === filterStatus.value;
    return matchesSearch && matchesFilter;
  });
});

const totalNotesCount = computed(() => {
  return clips.value.reduce((acc, c) => acc + (c.notes ? c.notes.length : 0), 0);
});

function countStatus(st) {
  return clips.value.filter(c => c.status === st).length;
}

function formatStatus(st) {
  switch (st) {
    case 'INGESTED': return 'Ingested';
    case 'SCRIPT_BURN_IN': return 'Script Burn-In';
    case 'IN_REVIEW': return 'In Review';
    case 'APPROVED': return 'Picture Lock';
    default: return st;
  }
}

onMounted(() => {
  fetchAll();
  // Simulate timecode ticking
  setInterval(() => {
    const frames = Math.floor(Math.random() * 24).toString().padStart(2, '0');
    mockTimecode.value = `00:01:24:${frames}`;
  }, 1000);
});
</script>

<style scoped>
.collabcut-app {
  max-width: 1400px;
  margin: 0 auto;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
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
  padding-bottom: 1.25rem;
}

.studio-brand {
  display: flex;
  align-items: center;
  gap: 1rem;
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

.project-tag em {
  color: #f3f4f6;
  font-style: normal;
  font-weight: 600;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.k8s-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: #111827;
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 0.45rem 0.85rem;
  border-radius: 9999px;
  font-size: 0.8rem;
  font-family: 'JetBrains Mono', monospace;
  color: #9ca3af;
}

.live-pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 8px #10b981;
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
  transition: all 0.2s;
}

.btn-ingest:hover {
  filter: brightness(1.15);
  transform: translateY(-1px);
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

/* KPI Grid */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
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
  transform: translateY(-2px);
}

.kpi-icon {
  font-size: 1.6rem;
  padding: 0.5rem;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
}

.ingest-color { background: rgba(59, 130, 246, 0.15); }
.burnin-color { background: rgba(245, 158, 11, 0.15); }
.review-color { background: rgba(168, 85, 247, 0.15); }
.locked-color { background: rgba(16, 185, 129, 0.15); }
.notes-color { background: rgba(6, 182, 212, 0.15); }

.kpi-num {
  font-size: 1.4rem;
  font-weight: 800;
  font-family: 'JetBrains Mono', monospace;
}

.kpi-label {
  font-size: 0.75rem;
  color: #9ca3af;
  text-transform: uppercase;
  font-weight: 600;
}

/* Toast */
.studio-toast {
  padding: 0.75rem 1rem;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
}
.studio-toast.success { background: rgba(16, 185, 129, 0.15); border: 1px solid #10b981; color: #34d399; }
.studio-toast.error { background: rgba(239, 68, 68, 0.15); border: 1px solid #ef4444; color: #f87171; }

/* Workspace Split Layout */
.studio-workspace {
  display: grid;
  grid-template-columns: 440px 1fr;
  gap: 1.5rem;
  min-height: 650px;
}

@media (max-width: 1024px) {
  .studio-workspace { grid-template-columns: 1fr; }
}

/* Left Bin Panel */
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

.bin-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 700;
  font-size: 0.95rem;
}

.count-pill {
  background: rgba(255, 255, 255, 0.1);
  padding: 0.15rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-family: 'JetBrains Mono', monospace;
}

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

.clip-feed {
  padding: 1rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  max-height: 700px;
}

.clip-card {
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;
  padding: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.clip-card:hover {
  border-color: rgba(245, 158, 11, 0.5);
  background: rgba(30, 41, 59, 0.9);
}

.clip-card.selected {
  border-color: #f59e0b;
  background: rgba(245, 158, 11, 0.07);
  box-shadow: 0 0 15px rgba(245, 158, 11, 0.15);
}

.clip-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.4rem;
}

.scene-badge {
  font-family: 'JetBrains Mono', monospace;
  font-weight: 700;
  font-size: 0.8rem;
  color: #fbbf24;
}

.status-pill {
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}
.status-ingested { background: rgba(59, 130, 246, 0.2); color: #60a5fa; }
.status-script_burn_in { background: rgba(245, 158, 11, 0.2); color: #fbbf24; }
.status-in_review { background: rgba(168, 85, 247, 0.2); color: #c084fc; }
.status-approved { background: rgba(16, 185, 129, 0.2); color: #34d399; }

.clip-title {
  font-weight: 600;
  font-size: 0.95rem;
  margin-bottom: 0.4rem;
}

.clip-metadata {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  font-size: 0.75rem;
  color: #9ca3af;
  margin-bottom: 0.5rem;
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

.clip-card-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.4rem;
}

.btn-action {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.3rem 0.65rem;
  border-radius: 4px;
  border: none;
  cursor: pointer;
}
.btn-burnin { background: #d97706; color: white; }
.btn-approve { background: #059669; color: white; }
.btn-action-del { background: transparent; border: none; cursor: pointer; opacity: 0.6; }
.btn-action-del:hover { opacity: 1; }

.empty-bin {
  text-align: center;
  color: #6b7280;
  padding: 2rem;
  font-size: 0.85rem;
}

/* Right Review Panel */
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
  min-height: 280px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background-image: radial-gradient(circle at center, rgba(30, 41, 59, 0.4) 0%, #000000 80%);
}

.viewport-overlay-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.85rem;
  color: #e5e7eb;
}

.rec-dot {
  width: 10px;
  height: 10px;
  background: #ef4444;
  border-radius: 50%;
  box-shadow: 0 0 10px #ef4444;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.tc-display {
  font-weight: 700;
  color: #fbbf24;
}

.viewport-screen {
  text-align: center;
  padding: 1.5rem 0;
}

.cinema-title {
  font-size: 1.4rem;
  font-weight: 800;
  margin-bottom: 0.25rem;
}

.cinema-roll {
  font-size: 0.8rem;
  color: #6b7280;
  font-family: 'JetBrains Mono', monospace;
}

.burned-subtitle {
  margin: 1.25rem auto 0 auto;
  max-width: 600px;
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

.burned-subtitle p {
  font-size: 0.95rem;
  color: #fef08a;
  font-style: italic;
}

.timeline-bar {
  height: 6px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 3px;
  position: relative;
  cursor: pointer;
}

.timeline-progress {
  height: 100%;
  background: #f59e0b;
  border-radius: 3px;
}

.timeline-scrubber {
  position: absolute;
  top: -4px;
  width: 14px;
  height: 14px;
  background: white;
  border-radius: 50%;
  transform: translateX(-50%);
  box-shadow: 0 0 8px rgba(0,0,0,0.8);
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

.btn-add-note:hover {
  background: rgba(245, 158, 11, 0.25);
}

.notes-stream {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  overflow-y: auto;
  max-height: 320px;
}

.note-item {
  display: flex;
  gap: 1rem;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 0.85rem;
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

.author-tag {
  font-size: 0.75rem;
  color: #9ca3af;
  font-weight: 600;
}

.note-text {
  font-size: 0.85rem;
  color: #e5e7eb;
  line-height: 1.4;
}

.empty-notes {
  text-align: center;
  color: #6b7280;
  padding: 2.5rem;
  font-size: 0.85rem;
}

.no-selection-state {
  margin: auto;
  text-align: center;
  padding: 4rem 2rem;
  color: #6b7280;
}

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
  max-width: 500px;
  padding: 1.5rem;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.7);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
}

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
.form-group input:focus, .form-group select:focus, .form-group textarea:focus {
  border-color: #f59e0b;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.5rem;
}

.btn-cancel {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #9ca3af;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  cursor: pointer;
}

.btn-primary {
  background: linear-gradient(135deg, #d97706 0%, #b45309 100%);
  color: white;
  border: none;
  padding: 0.55rem 1.25rem;
  border-radius: 6px;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
}

.studio-footer {
  text-align: center;
  font-size: 0.8rem;
  color: #6b7280;
  padding-top: 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}
</style>
