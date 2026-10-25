<template>
  <div class="collabcut-app">
    <!-- Header -->
    <header class="studio-header">
      <div class="studio-brand">
        <div class="clapper-icon">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="#f4c524">
            <path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm-2 2 1.5 3H17l-1.5-3H18zm-4.5 0 1.5 3H12.5L11 6h2.5zM9 6l1.5 3H8L6.5 6H9zm-5 5h16v7H4v-7z"/>
          </svg>
        </div>
        <div>
          <h1 class="brand-title">CollabCut</h1>
          <p class="brand-tagline">Film Review & Script Subtitle Suite</p>
        </div>
      </div>
      <div class="header-actions">
        <span class="clip-count-badge">{{ assets.length }} Clips</span>
        <button class="btn-refresh" :disabled="loading" @click="fetchAll" title="Reload footage list">
          <svg class="btn-icon" :class="{ 'spin': loading }" viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/>
          </svg>
          <span>Reload</span>
        </button>
        <button class="btn-primary" @click="showUploadModal = true">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          <span>Add New Clip</span>
        </button>
      </div>
    </header>

    <!-- Toast Notification -->
    <transition name="fade">
      <div v-if="toast.show" class="studio-toast" :class="toast.type">
        {{ toast.message }}
      </div>
    </transition>

    <!-- Main Workspace -->
    <main class="studio-grid">
      <!-- Left: Clips List -->
      <aside class="clips-panel">
        <div class="panel-header">
          <div class="panel-title-row">
            <h2>Footage Clips</h2>
            <span class="count-pill">{{ filteredAssets.length }}</span>
          </div>
          <div class="search-box">
            <svg class="search-icon" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="#f4c524" stroke-width="2.5" stroke-linecap="round">
              <circle cx="11" cy="11" r="7"/><path d="m21 21-4.35-4.35"/>
            </svg>
            <input 
              v-model="searchQuery" 
              type="text" 
              placeholder="Filter by scene, take, or title..." 
            />
            <button v-if="searchQuery" class="clear-search" @click="searchQuery = ''">✕</button>
          </div>
        </div>

        <div class="clips-list">
          <div 
            v-for="asset in filteredAssets" 
            :key="asset.id"
            class="clip-card"
            :class="{ 'selected': selectedAsset && selectedAsset.id === asset.id }"
            @click="selectAsset(asset)"
          >
            <div class="clip-card-header">
              <span class="scene-tag">{{ asset.sceneId }} • Take {{ asset.takeNumber }}</span>
              <span class="status-badge" :class="'badge-' + asset.status.toLowerCase()">
                {{ formatStatus(asset.status) }}
              </span>
            </div>

            <div class="clip-title">{{ asset.title }}</div>

            <div v-if="asset.scriptDialogue" class="clip-dialogue-preview">
              "{{ asset.scriptDialogue }}"
            </div>

            <div class="clip-card-footer">
              <span class="clip-meta">{{ formatSeconds(asset.durationSeconds) }} • {{ (asset.comments || []).length }} notes</span>
              
              <div class="clip-actions" @click.stop>
                <button 
                  v-if="asset.status === 'UPLOADED'" 
                  class="btn-burn-action" 
                  @click="triggerBurnIn(asset)"
                  title="Burn screenplay subtitles into video proxy"
                >
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="currentColor">
                    <path d="m13 2-8 12h6l-2 8 10-13h-6l2-7z"/>
                  </svg>
                  <span>Burn Subtitles</span>
                </button>
                <button 
                  v-else-if="asset.status === 'READY_FOR_REVIEW'" 
                  class="btn-lock-action" 
                  @click="updateStatus(asset, 'APPROVED')"
                  title="Mark as Picture Locked"
                >
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/>
                  </svg>
                  <span>Lock Cut</span>
                </button>
                <button 
                  class="btn-delete-clip" 
                  @click="deleteAsset(asset.id, asset.title)" 
                  title="Delete Clip"
                >
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M3 6h18m-2 0v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6m3 0V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <div v-if="filteredAssets.length === 0" class="empty-clips">
            <p>No clips found matching "{{ searchQuery }}"</p>
          </div>
        </div>
      </aside>

      <!-- Right: Cinema Player & Review Notes -->
      <section class="review-panel">
        <div v-if="selectedAsset" class="review-container">
          
          <!-- Cinema Player Viewport -->
          <div class="cinema-player">
            <!-- Screen Top Overlay -->
            <div class="player-top-bar">
              <div class="playback-badge">
                <span class="live-dot" :class="{ 'playing': isPlaying }"></span>
                <span>{{ isPlaying ? 'PLAYING' : 'PAUSED' }}</span>
              </div>
              <div class="timecode-display">{{ formattedCurrentTimecode }}</div>
              <div class="scene-indicator">{{ selectedAsset.sceneId }} | TK {{ selectedAsset.takeNumber }}</div>
            </div>

            <!-- Cinematic Stage -->
            <div class="player-screen">
              <div class="cinema-heading">
                <h3>{{ selectedAsset.title }}</h3>
                <p>{{ selectedAsset.cameraRoll }} • 24 FPS • 1080p</p>
              </div>

              <!-- Screenplay Dialogue Subtitle Overlay -->
              <div class="dialogue-overlay" :class="{ 'burned': selectedAsset.processedS3Url }">
                <span class="sub-label">
                  {{ selectedAsset.processedS3Url ? 'BURNED-IN SUBTITLE' : 'ORIGINAL SCREENPLAY' }}
                </span>
                <p class="sub-text">"{{ selectedAsset.scriptDialogue || 'No screenplay dialogue assigned.' }}"</p>
              </div>
            </div>

            <!-- Player Controls -->
            <div class="player-controls">
              <!-- Tactile Hardware Play/Pause Button -->
              <button 
                class="btn-play-toggle" 
                :class="{ 'is-playing': isPlaying }" 
                @click="togglePlay"
                :title="isPlaying ? 'Pause Video' : 'Play Video'"
              >
                <!-- Play Icon SVG -->
                <svg v-if="!isPlaying" class="transport-icon" viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                  <path d="M8 5v14l11-7z"/>
                </svg>
                <!-- Pause Icon SVG -->
                <svg v-else class="transport-icon" viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                  <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>
                </svg>
                <span>{{ isPlaying ? 'Pause' : 'Play' }}</span>
              </button>

              <input 
                type="range" 
                min="0" 
                :max="selectedAsset.durationSeconds || 120" 
                v-model.number="currentSeconds" 
                class="cinema-scrubber" 
              />
              <span class="time-readout">
                {{ formatSeconds(currentSeconds) }} / {{ formatSeconds(selectedAsset.durationSeconds) }}
              </span>
            </div>
          </div>

          <!-- Instant Feedback Note Section (No Popups!) -->
          <div class="feedback-section">
            <div class="feedback-header">
              <h3>Review Notes ({{ (selectedAsset.comments || []).length }})</h3>
              <p class="hint">Type feedback below — it automatically attaches to the current playhead <strong>{{ formattedCurrentTimecode }}</strong>.</p>
            </div>

            <!-- Inline Add Note Form -->
            <form class="inline-note-form" @submit.prevent="submitInlineNote">
              <div class="note-time-badge">
                <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5">
                  <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
                </svg>
                <span>{{ formattedCurrentTimecode }}</span>
              </div>
              <select v-model="quickDept" class="dept-select">
                <option value="EDITORIAL">Director / Editorial</option>
                <option value="COLOR_GRADE">Color Grade</option>
                <option value="SOUND_DESIGN">Sound Design</option>
                <option value="VFX">VFX</option>
              </select>
              <input 
                v-model="quickNoteText" 
                type="text" 
                placeholder="Type note at current timecode and press Enter..." 
                class="note-input"
                required
              />
              <button type="submit" class="btn-post-note" :disabled="submitting || !quickNoteText.trim()">
                <span>{{ submitting ? 'Adding...' : 'Post Note' }}</span>
              </button>
            </form>

            <!-- Notes Timeline -->
            <div class="notes-timeline">
              <div 
                v-for="note in (selectedAsset.comments || [])" 
                :key="note.id" 
                class="timeline-item"
                @click="jumpToTimecode(note.timestampSeconds)"
                title="Click to jump video to this note"
              >
                <div class="timeline-tc">{{ note.timecode }}</div>
                <div class="timeline-content">
                  <div class="timeline-meta">
                    <span class="timeline-dept">{{ note.department }}</span>
                    <span class="timeline-author">{{ note.userId || 'Director' }}</span>
                  </div>
                  <div class="timeline-text">{{ note.commentText }}</div>
                </div>
                <button 
                  class="btn-del-note" 
                  @click.stop="deleteComment(note.id)" 
                  title="Delete note"
                >
                  ✕
                </button>
              </div>

              <div v-if="!selectedAsset.comments || selectedAsset.comments.length === 0" class="no-notes">
                <span>No notes for this clip yet. Play the video and post a note above.</span>
              </div>
            </div>
          </div>

        </div>

        <!-- No Selection Placeholder -->
        <div v-else class="empty-selection">
          <div class="clapper-empty-icon">
            <svg viewBox="0 0 24 24" width="56" height="56" fill="#f4c524">
              <path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm-2 2 1.5 3H17l-1.5-3H18zm-4.5 0 1.5 3H12.5L11 6h2.5zM9 6l1.5 3H8L6.5 6H9zm-5 5h16v7H4v-7z"/>
            </svg>
          </div>
          <h3>Select a Clip to Review</h3>
          <p>Choose any film take from the left to watch, inspect subtitles, or add feedback.</p>
        </div>
      </section>
    </main>

    <!-- Modal: Add New Clip -->
    <div v-if="showUploadModal" class="modal-backdrop" @click.self="showUploadModal = false">
      <div class="modal-card">
        <div class="modal-card-header">
          <h3>Add New Footage Take</h3>
          <button class="btn-close-modal" @click="showUploadModal = false">✕</button>
        </div>

        <form @submit.prevent="submitNewClip">
          <div class="form-row">
            <div class="form-field flex-2">
              <label>Scene & Take</label>
              <input v-model="newClip.sceneId" type="text" placeholder="e.g. Scene 12B" required />
            </div>
            <div class="form-field flex-1">
              <label>Take #</label>
              <input v-model.number="newClip.takeNumber" type="number" min="1" required />
            </div>
          </div>

          <div class="form-field">
            <label>Clip Title / Description</label>
            <input v-model="newClip.title" type="text" placeholder="e.g. Rooftop Confrontation Master" required />
          </div>

          <div class="form-field">
            <label>Script Screenplay Dialogue (for Subtitles)</label>
            <textarea 
              v-model="newClip.scriptDialogue" 
              rows="3" 
              placeholder='e.g. DETECTIVE: "Where were you at midnight?"'
              required
            ></textarea>
          </div>

          <div class="form-row">
            <div class="form-field flex-1">
              <label>Camera Roll</label>
              <input v-model="newClip.cameraRoll" type="text" placeholder="Roll A-01" />
            </div>
            <div class="form-field flex-1">
              <label>Duration (seconds)</label>
              <input v-model.number="newClip.durationSeconds" type="number" min="5" required />
            </div>
          </div>

          <div class="modal-card-actions">
            <button type="button" class="btn-secondary" @click="showUploadModal = false">Cancel</button>
            <button type="submit" class="btn-primary" :disabled="submitting">
              {{ submitting ? 'Saving...' : 'Add Clip' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';

const assets = ref([]);
const selectedAsset = ref(null);
const loading = ref(false);
const submitting = ref(false);
const searchQuery = ref('');
const showUploadModal = ref(false);

// Video scrubber state
const isPlaying = ref(false);
const currentSeconds = ref(42);
let playInterval = null;

// Inline comment inputs
const quickNoteText = ref('');
const quickDept = ref('EDITORIAL');

// New clip input
const newClip = ref({
  sceneId: '',
  takeNumber: 1,
  title: '',
  scriptDialogue: '',
  cameraRoll: 'Roll A-01',
  durationSeconds: 120,
  s3BucketUrl: 's3://collabcut-raw/take.mp4'
});

// Toast notification
const toast = ref({ show: false, message: '', type: 'success' });
function showToast(msg, type = 'success') {
  toast.value = { show: true, message: msg, type };
  setTimeout(() => { toast.value.show = false; }, 3500);
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
      const max = (selectedAsset.value && selectedAsset.value.durationSeconds) || 120;
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
  showToast(`Jumped playhead to ${formattedCurrentTimecode.value}`);
}

async function fetchAll() {
  loading.value = true;
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
    console.error('Fetch assets error:', err);
  } finally {
    loading.value = false;
  }
}

function selectAsset(asset) {
  selectedAsset.value = asset;
  currentSeconds.value = 0;
}

async function triggerBurnIn(asset) {
  try {
    const res = await fetch(`/api/assets/${asset.id}/burn-in`, { method: 'POST' });
    if (!res.ok) throw new Error(`Burn-in failed`);
    showToast(`Subtitles burned onto ${asset.sceneId}! Ready to review.`, 'success');
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
    if (!res.ok) throw new Error(`Update status failed`);
    showToast(`${asset.sceneId} locked for final cut!`, 'success');
    await fetchAll();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// Inline note submission
async function submitInlineNote() {
  if (!selectedAsset.value || !quickNoteText.value.trim()) return;
  submitting.value = true;
  try {
    const payload = {
      asset_id: selectedAsset.value.id,
      timestamp_seconds: currentSeconds.value,
      timecode: formattedCurrentTimecode.value,
      user_id: 'Director',
      department: quickDept.value,
      comment_text: quickNoteText.value.trim()
    };

    const res = await fetch('/api/comments', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) throw new Error(`Could not post note`);
    showToast(`Note posted at ${payload.timecode}!`, 'success');
    quickNoteText.value = '';
    await fetchAll();
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    submitting.value = false;
  }
}

async function deleteComment(id) {
  try {
    const res = await fetch(`/api/comments/${id}`, { method: 'DELETE' });
    if (!res.ok) throw new Error(`Delete note failed`);
    showToast(`Note deleted.`, 'success');
    await fetchAll();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function submitNewClip() {
  submitting.value = true;
  try {
    const res = await fetch('/api/assets', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ...newClip.value,
        s3BucketUrl: `s3://collabcut-raw/${newClip.value.sceneId.replace(/\s+/g, '_').toLowerCase()}_tk${newClip.value.takeNumber}.mp4`
      })
    });

    if (!res.ok) throw new Error(`Failed to create clip`);
    const created = await res.json();
    showToast(`Added clip: ${created.sceneId}`, 'success');
    showUploadModal.value = false;
    newClip.value = { sceneId: '', takeNumber: 1, title: '', scriptDialogue: '', cameraRoll: 'Roll A-01', durationSeconds: 120, s3BucketUrl: '' };
    await fetchAll();
    selectedAsset.value = created;
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    submitting.value = false;
  }
}

async function deleteAsset(id, title) {
  if (!confirm(`Delete '${title}'?`)) return;
  try {
    const res = await fetch(`/api/assets/${id}`, { method: 'DELETE' });
    if (!res.ok) throw new Error(`Delete clip failed`);
    showToast(`Deleted ${title}`, 'success');
    await fetchAll();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

const filteredAssets = computed(() => {
  const q = searchQuery.value.trim().toLowerCase();
  if (!q) return assets.value;
  return assets.value.filter(a => 
    a.title.toLowerCase().includes(q) ||
    a.sceneId.toLowerCase().includes(q) ||
    (a.scriptDialogue && a.scriptDialogue.toLowerCase().includes(q))
  );
});

function formatStatus(st) {
  switch (st) {
    case 'UPLOADED': return 'Burn-in Needed';
    case 'PROCESSING_BURN_IN': return 'Burning Subtitles...';
    case 'READY_FOR_REVIEW': return 'Ready for Review';
    case 'APPROVED': return 'Picture Locked';
    default: return st;
  }
}

function formatSeconds(sec) {
  if (!sec && sec !== 0) return '00:00';
  const m = Math.floor(sec / 60).toString().padStart(2, '0');
  const s = (sec % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
}

onMounted(() => {
  fetchAll();
});

onUnmounted(() => {
  if (playInterval) clearInterval(playInterval);
});
</script>

<style scoped>
/* Strict Two-Color Brand: #f4c524 and Black */
.collabcut-app {
  max-width: 1400px;
  margin: 0 auto;
  padding: 1.5rem;
  background-color: #000000;
  color: #ffffff;
  font-family: 'Plus Jakarta Sans', sans-serif;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Header */
.studio-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(244, 197, 36, 0.25);
  padding-bottom: 1.25rem;
}

.studio-brand {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.clapper-icon {
  background: #000000;
  border: 1px solid #f4c524;
  border-radius: 8px;
  padding: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 15px rgba(244, 197, 36, 0.25);
}

.brand-title {
  font-size: 1.8rem;
  font-weight: 800;
  color: #f4c524;
  letter-spacing: -0.03em;
  line-height: 1.1;
}

.brand-tagline {
  font-size: 0.85rem;
  color: #a3a3a3;
  margin-top: 0.15rem;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.clip-count-badge {
  background: #000000;
  border: 1px solid rgba(244, 197, 36, 0.3);
  color: #f4c524;
  padding: 0.45rem 0.85rem;
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
}

/* Tactile Brand Button: Reload Footage */
.btn-refresh {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: #000000;
  border: 1.5px solid #f4c524;
  color: #f4c524;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.85rem;
  font-weight: 800;
  letter-spacing: 0.02em;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: inset 0 1px 0 rgba(244, 197, 36, 0.2), 0 2px 5px rgba(0, 0, 0, 0.8);
}
.btn-refresh:hover:not(:disabled) {
  background: #f4c524;
  color: #000000;
  box-shadow: 0 0 15px rgba(244, 197, 36, 0.4);
}
.btn-refresh:active:not(:disabled) {
  transform: translateY(1px);
}
.btn-refresh:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-icon {
  display: inline-block;
  transition: transform 0.2s ease;
}

.spin {
  animation: spin 0.8s linear infinite;
}
@keyframes spin { 100% { transform: rotate(360deg); } }

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: #f4c524;
  color: #000000;
  border: 1.5px solid #f4c524;
  padding: 0.55rem 1.25rem;
  border-radius: 6px;
  font-weight: 800;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.8), 0 0 15px rgba(244, 197, 36, 0.25);
}
.btn-primary:hover {
  background: #000000;
  color: #f4c524;
}
.btn-primary:active {
  transform: translateY(1px);
}

/* Toast */
.studio-toast {
  position: fixed;
  top: 1.5rem;
  right: 1.5rem;
  background: #000000;
  border: 1.5px solid #f4c524;
  color: #f4c524;
  padding: 0.85rem 1.25rem;
  border-radius: 8px;
  font-weight: 800;
  font-size: 0.9rem;
  z-index: 200;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.9), 0 0 15px rgba(244, 197, 36, 0.35);
}

/* Studio Grid Layout */
.studio-grid {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 1.5rem;
  flex: 1;
}

@media (max-width: 1024px) {
  .studio-grid {
    grid-template-columns: 1fr;
  }
}

/* Left: Clips Panel */
.clips-panel {
  background: #0a0a0a;
  border: 1px solid rgba(244, 197, 36, 0.2);
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: calc(100vh - 160px);
  min-height: 650px;
}

.panel-header {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid rgba(244, 197, 36, 0.15);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.panel-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-title-row h2 {
  font-size: 1.1rem;
  font-weight: 800;
  color: #ffffff;
}

.count-pill {
  background: #000000;
  color: #f4c524;
  border: 1px solid rgba(244, 197, 36, 0.3);
  padding: 0.15rem 0.6rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 0.75rem;
}

.search-box input {
  width: 100%;
  background: #000000;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 6px;
  padding: 0.5rem 2rem 0.5rem 2.2rem;
  color: #ffffff;
  font-size: 0.85rem;
  outline: none;
  transition: all 0.2s;
}
.search-box input:focus {
  border-color: #f4c524;
  box-shadow: 0 0 10px rgba(244, 197, 36, 0.25);
}

.clear-search {
  position: absolute;
  right: 0.75rem;
  background: transparent;
  border: none;
  color: #a3a3a3;
  cursor: pointer;
}

.clips-list {
  padding: 1rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  flex: 1;
}

.clip-card {
  background: #000000;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 1rem;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.clip-card:hover {
  border-color: rgba(244, 197, 36, 0.4);
  background: #0d0d0d;
}

.clip-card.selected {
  border-color: #f4c524;
  background: #121212;
  box-shadow: 0 0 18px rgba(244, 197, 36, 0.2);
}

.clip-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.scene-tag {
  font-family: 'JetBrains Mono', monospace;
  font-weight: 800;
  font-size: 0.85rem;
  color: #f4c524;
}

.status-badge {
  font-size: 0.65rem;
  font-weight: 800;
  text-transform: uppercase;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}
.badge-uploaded {
  background: #000000;
  color: #f4c524;
  border: 1px solid rgba(244, 197, 36, 0.5);
}
.badge-ready_for_review {
  background: #f4c524;
  color: #000000;
}
.badge-approved {
  background: #000000;
  color: #ffffff;
  border: 1px solid #ffffff;
}

.clip-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: #ffffff;
}

.clip-dialogue-preview {
  font-size: 0.75rem;
  color: #a3a3a3;
  font-style: italic;
  border-left: 2px solid #f4c524;
  padding-left: 0.5rem;
  line-height: 1.3;
}

.clip-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.25rem;
}

.clip-meta {
  font-size: 0.75rem;
  color: #737373;
  font-family: 'JetBrains Mono', monospace;
}

.clip-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-burn-action {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: #f4c524;
  color: #000000;
  border: 1px solid #f4c524;
  font-weight: 800;
  font-size: 0.75rem;
  padding: 0.3rem 0.7rem;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-burn-action:hover {
  background: #000000;
  color: #f4c524;
}

.btn-lock-action {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: #000000;
  color: #f4c524;
  border: 1px solid #f4c524;
  font-weight: 800;
  font-size: 0.75rem;
  padding: 0.3rem 0.7rem;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-lock-action:hover {
  background: #f4c524;
  color: #000000;
}

.btn-delete-clip {
  background: transparent;
  border: none;
  color: #737373;
  cursor: pointer;
  padding: 0.25rem;
  display: flex;
  align-items: center;
  transition: color 0.2s;
}
.btn-delete-clip:hover {
  color: #ffffff;
}

.empty-clips {
  text-align: center;
  padding: 3rem 1rem;
  color: #737373;
  font-size: 0.9rem;
}

/* Right: Cinema Review Panel */
.review-panel {
  background: #0a0a0a;
  border: 1px solid rgba(244, 197, 36, 0.2);
  border-radius: 10px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 160px);
  min-height: 650px;
}

.review-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
}

/* Cinema Player */
.cinema-player {
  background: #000000;
  border-bottom: 1px solid rgba(244, 197, 36, 0.25);
  display: flex;
  flex-direction: column;
}

.player-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.85rem 1.25rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.playback-badge {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.75rem;
  font-weight: 800;
  font-family: 'JetBrains Mono', monospace;
}

.live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #525252;
}
.live-dot.playing {
  background: #f4c524;
  box-shadow: 0 0 10px #f4c524;
}

.timecode-display {
  font-family: 'JetBrains Mono', monospace;
  font-size: 1.25rem;
  font-weight: 800;
  color: #f4c524;
  text-shadow: 0 0 10px rgba(244, 197, 36, 0.35);
}

.scene-indicator {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8rem;
  color: #a3a3a3;
}

.player-screen {
  position: relative;
  padding: 3rem 2rem 2.5rem 2rem;
  text-align: center;
  min-height: 250px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background-image: radial-gradient(circle at center, #141414 0%, #000000 80%);
}

.cinema-heading h3 {
  font-size: 1.5rem;
  font-weight: 800;
  color: #ffffff;
}

.cinema-heading p {
  font-size: 0.8rem;
  color: #737373;
  margin-top: 0.25rem;
  font-family: 'JetBrains Mono', monospace;
}

.dialogue-overlay {
  margin: 1.5rem auto 0 auto;
  max-width: 680px;
  background: rgba(0, 0, 0, 0.9);
  border: 1px solid #f4c524;
  border-radius: 8px;
  padding: 0.85rem 1.25rem;
  box-shadow: 0 0 25px rgba(244, 197, 36, 0.2);
}

.sub-label {
  display: block;
  font-size: 0.65rem;
  font-weight: 800;
  font-family: 'JetBrains Mono', monospace;
  color: #f4c524;
  margin-bottom: 0.35rem;
  letter-spacing: 0.05em;
}

.sub-text {
  font-size: 1.05rem;
  font-weight: 600;
  color: #f4c524;
  line-height: 1.4;
}

.player-controls {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  padding: 0.85rem 1.25rem;
  background: #050505;
}

/* Tactile Hardware Play/Pause Button */
.btn-play-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  background: #f4c524;
  color: #000000;
  border: 1.5px solid #f4c524;
  font-weight: 800;
  font-size: 0.85rem;
  padding: 0.5rem 1.25rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.3), 0 2px 6px rgba(0, 0, 0, 0.8);
}
.btn-play-toggle:hover {
  background: #000000;
  color: #f4c524;
  box-shadow: 0 0 15px rgba(244, 197, 36, 0.35);
}
.btn-play-toggle:active {
  transform: translateY(1px);
}
.btn-play-toggle.is-playing {
  background: #000000;
  color: #f4c524;
  border-color: #f4c524;
  box-shadow: 0 0 12px rgba(244, 197, 36, 0.3);
}
.btn-play-toggle.is-playing:hover {
  background: #f4c524;
  color: #000000;
}

.transport-icon {
  display: inline-block;
  flex-shrink: 0;
}

.cinema-scrubber {
  flex: 1;
  accent-color: #f4c524;
  cursor: pointer;
  height: 6px;
}

.time-readout {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.85rem;
  font-weight: 700;
  color: #a3a3a3;
  white-space: nowrap;
}

/* Feedback Section */
.feedback-section {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  flex: 1;
}

.feedback-header h3 {
  font-size: 1.1rem;
  font-weight: 800;
  color: #ffffff;
}

.feedback-header .hint {
  font-size: 0.8rem;
  color: #a3a3a3;
  margin-top: 0.15rem;
}

.inline-note-form {
  display: flex;
  gap: 0.65rem;
  background: #000000;
  border: 1px solid rgba(244, 197, 36, 0.3);
  border-radius: 8px;
  padding: 0.65rem;
  align-items: center;
  flex-wrap: wrap;
}

.note-time-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: #111111;
  border: 1px solid #f4c524;
  color: #f4c524;
  padding: 0.45rem 0.75rem;
  border-radius: 6px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8rem;
  font-weight: 700;
  white-space: nowrap;
}

.dept-select {
  background: #111111;
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #ffffff;
  padding: 0.5rem 0.75rem;
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 600;
  outline: none;
}

.note-input {
  flex: 1;
  min-width: 200px;
  background: transparent;
  border: none;
  color: #ffffff;
  padding: 0.5rem 0.5rem;
  font-size: 0.9rem;
  outline: none;
}

.btn-post-note {
  background: #f4c524;
  color: #000000;
  border: 1.5px solid #f4c524;
  padding: 0.5rem 1.25rem;
  border-radius: 6px;
  font-weight: 800;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.7);
}
.btn-post-note:hover:not(:disabled) {
  background: #000000;
  color: #f4c524;
}
.btn-post-note:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* Notes Timeline */
.notes-timeline {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  overflow-y: auto;
  max-height: 280px;
}

.timeline-item {
  display: flex;
  align-items: flex-start;
  gap: 0.85rem;
  background: #000000;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 0.75rem 1rem;
  cursor: pointer;
  transition: all 0.2s;
}

.timeline-item:hover {
  border-color: #f4c524;
  background: #0d0d0d;
}

.timeline-tc {
  font-family: 'JetBrains Mono', monospace;
  font-weight: 800;
  color: #f4c524;
  font-size: 0.85rem;
  white-space: nowrap;
  background: #111111;
  padding: 0.25rem 0.55rem;
  border-radius: 4px;
}

.timeline-content {
  flex: 1;
}

.timeline-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.2rem;
}

.timeline-dept {
  font-size: 0.65rem;
  font-weight: 800;
  font-family: 'JetBrains Mono', monospace;
  color: #f4c524;
  text-transform: uppercase;
}

.timeline-author {
  font-size: 0.75rem;
  color: #737373;
}

.timeline-text {
  font-size: 0.85rem;
  color: #e5e5e5;
  line-height: 1.4;
}

.btn-del-note {
  background: transparent;
  border: none;
  color: #737373;
  cursor: pointer;
  padding: 0.2rem;
  font-size: 0.85rem;
}
.btn-del-note:hover {
  color: #ffffff;
}

.no-notes {
  text-align: center;
  padding: 2.5rem 1rem;
  color: #737373;
  font-size: 0.85rem;
}

/* Empty Selection */
.empty-selection {
  margin: auto;
  text-align: center;
  padding: 4rem 2rem;
  color: #737373;
}
.clapper-empty-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 1.25rem;
}

/* Modal */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 1.5rem;
}

.modal-card {
  background: #0a0a0a;
  border: 1.5px solid #f4c524;
  border-radius: 12px;
  width: 100%;
  max-width: 500px;
  padding: 1.5rem;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.95);
}

.modal-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
}

.modal-card-header h3 {
  font-size: 1.2rem;
  font-weight: 800;
  color: #f4c524;
}

.btn-close-modal {
  background: transparent;
  border: none;
  color: #a3a3a3;
  font-size: 1.2rem;
  cursor: pointer;
}
.btn-close-modal:hover {
  color: #f4c524;
}

.form-row {
  display: flex;
  gap: 0.75rem;
}
.flex-1 { flex: 1; }
.flex-2 { flex: 2; }

.form-field {
  margin-bottom: 1rem;
}

.form-field label {
  display: block;
  font-size: 0.8rem;
  font-weight: 700;
  color: #a3a3a3;
  margin-bottom: 0.35rem;
}

.form-field input, .form-field textarea {
  width: 100%;
  background: #000000;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 6px;
  padding: 0.55rem 0.75rem;
  color: #ffffff;
  font-family: inherit;
  font-size: 0.85rem;
  outline: none;
  transition: all 0.2s;
}
.form-field input:focus, .form-field textarea:focus {
  border-color: #f4c524;
  box-shadow: 0 0 10px rgba(244, 197, 36, 0.3);
}

.modal-card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.5rem;
}

.btn-secondary {
  background: #000000;
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #ffffff;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-secondary:hover {
  border-color: #f4c524;
  color: #f4c524;
}
</style>
