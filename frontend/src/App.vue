<template>
  <div class="studio-app">
    <!-- Clean Minimalist Header (Linear / Apple TV+ aesthetic) -->
    <header class="app-header">
      <div class="brand-group">
        <div class="brand-logo">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none">
            <rect x="2" y="2" width="20" height="20" rx="6" fill="#f4c524" />
            <polygon points="10,7 17,12 10,17" fill="#000000" />
          </svg>
        </div>
        <div class="brand-titles">
          <span class="brand-name">CollabCut</span>
          <span class="brand-badge">STUDIO</span>
        </div>
      </div>

      <div class="header-actions">
        <span class="take-counter">{{ assets.length }} Takes</span>
        <button class="btn-ghost" :disabled="loading" @click="fetchAll" title="Refresh takes">
          <svg :class="{ 'spin': loading }" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
            <path d="M21 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/>
          </svg>
          <span>Refresh</span>
        </button>
        <button class="btn-gold" @click="showUploadModal = true">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          <span>New Take</span>
        </button>
      </div>
    </header>

    <!-- Toast Notification -->
    <transition name="toast">
      <div v-if="toast.show" class="app-toast">
        <span class="toast-dot"></span>
        <span>{{ toast.message }}</span>
      </div>
    </transition>

    <!-- Main Studio Content -->
    <main class="app-layout">
      <!-- Left: Take Bin Sidebar -->
      <aside class="sidebar-bin">
        <div class="sidebar-header">
          <div class="sidebar-search">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="7"/><path d="m21 21-4.35-4.35"/>
            </svg>
            <input 
              v-model="searchQuery" 
              type="text" 
              placeholder="Search takes, scenes, dialogue..." 
            />
            <button v-if="searchQuery" class="clear-search-btn" @click="searchQuery = ''">✕</button>
          </div>

          <!-- Quick Filters -->
          <div class="filter-row">
            <button 
              class="filter-tag" 
              :class="{ 'active': activeFilter === 'ALL' }" 
              @click="activeFilter = 'ALL'"
            >
              All ({{ assets.length }})
            </button>
            <button 
              class="filter-tag" 
              :class="{ 'active': activeFilter === 'UPLOADED' }" 
              @click="activeFilter = 'UPLOADED'"
            >
              Needs Subtitles
            </button>
            <button 
              class="filter-tag" 
              :class="{ 'active': activeFilter === 'READY_FOR_REVIEW' }" 
              @click="activeFilter = 'READY_FOR_REVIEW'"
            >
              Ready
            </button>
            <button 
              class="filter-tag" 
              :class="{ 'active': activeFilter === 'APPROVED' }" 
              @click="activeFilter = 'APPROVED'"
            >
              Locked
            </button>
          </div>
        </div>

        <!-- Take Cards List -->
        <div class="takes-list">
          <div 
            v-for="asset in filteredAssets" 
            :key="asset.id"
            class="take-card"
            :class="{ 'selected': selectedAsset && selectedAsset.id === asset.id }"
            @click="selectAsset(asset)"
          >
            <div class="take-card-top">
              <span class="take-scene">{{ asset.sceneId }} • Take {{ asset.takeNumber }}</span>
              <span class="take-status-pill" :class="'pill-' + asset.status.toLowerCase()">
                {{ formatStatus(asset.status) }}
              </span>
            </div>

            <h4 class="take-title">{{ asset.title }}</h4>

            <p v-if="asset.scriptDialogue" class="take-dialogue">
              "{{ asset.scriptDialogue }}"
            </p>

            <div class="take-card-bottom">
              <span class="take-runtime">{{ formatSeconds(asset.durationSeconds) }} • {{ (asset.comments || []).length }} notes</span>
              
              <div class="take-actions" @click.stop>
                <button 
                  v-if="asset.status === 'UPLOADED'" 
                  class="btn-burn-action" 
                  @click="triggerBurnIn(asset)"
                  title="Burn screenplay subtitles into video"
                >
                  ⚡ Burn Subtitles
                </button>
                <button 
                  v-else-if="asset.status === 'READY_FOR_REVIEW'" 
                  class="btn-lock-action" 
                  @click="updateStatus(asset, 'APPROVED')"
                  title="Picture Lock this cut"
                >
                  🔒 Lock
                </button>
                <button 
                  class="btn-delete-action" 
                  @click="deleteAsset(asset.id, asset.title)" 
                  title="Delete take"
                >
                  ✕
                </button>
              </div>
            </div>
          </div>

          <div v-if="filteredAssets.length === 0" class="empty-bin-state">
            <p>No takes match your search filter.</p>
          </div>
        </div>
      </aside>

      <!-- Center & Right: Hero Cinema Player & Notes -->
      <section class="main-stage">
        <div v-if="selectedAsset" class="stage-content">
          
          <!-- Cinema Player Canvas -->
          <div class="cinema-player-card">
            <!-- Player Top Status Header -->
            <div class="player-header-bar">
              <div class="take-ident">
                <span class="take-accent-dot" :class="{ 'live': isPlaying }"></span>
                <span class="take-ident-title">{{ selectedAsset.title }}</span>
                <span class="take-ident-sub">{{ selectedAsset.sceneId }} • Take {{ selectedAsset.takeNumber }}</span>
              </div>
              <div class="smpte-pill">{{ formattedCurrentTimecode }}</div>
            </div>

            <!-- Cinematic 16:9 Viewport -->
            <div class="viewport-box" @click="togglePlay">
              <!-- Center Big Play Button Overlay -->
              <transition name="play-fade">
                <div v-if="!isPlaying" class="play-overlay-btn">
                  <svg viewBox="0 0 24 24" width="28" height="28" fill="#f4c524">
                    <polygon points="8,5 19,12 8,19" />
                  </svg>
                </div>
              </transition>

              <!-- Burned Screenplay Subtitle Floating Bar -->
              <div class="screenplay-subtitle-capsule" :class="{ 'is-burned': selectedAsset.processedS3Url }">
                <span class="subtitle-badge">
                  {{ selectedAsset.processedS3Url ? '⚡ SUBTITLE BURNED' : 'SCREENPLAY CUE' }}
                </span>
                <p class="subtitle-line">
                  "{{ selectedAsset.scriptDialogue || 'No screenplay dialogue assigned.' }}"
                </p>
              </div>
            </div>

            <!-- Player Transport & Scrubber Bar -->
            <div class="player-transport-deck">
              <button 
                class="btn-play-pause" 
                :class="{ 'playing': isPlaying }" 
                @click.stop="togglePlay"
              >
                <svg v-if="!isPlaying" viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                  <polygon points="8,5 19,12 8,19" />
                </svg>
                <svg v-else viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                  <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>
                </svg>
                <span>{{ isPlaying ? 'Pause' : 'Play' }}</span>
              </button>

              <!-- Timeline Scrubber with Markers -->
              <div class="scrubber-container">
                <div class="scrubber-pins">
                  <div 
                    v-for="note in (selectedAsset.comments || [])" 
                    :key="'pin-'+note.id" 
                    class="scrubber-pin"
                    :style="{ left: calcMarkerLeft(note.timestampSeconds) }"
                    :title="note.timecode + ' • ' + note.commentText"
                    @click.stop="jumpToTimecode(note.timestampSeconds)"
                  ></div>
                </div>
                <input 
                  type="range" 
                  min="0" 
                  :max="selectedAsset.durationSeconds || 150" 
                  v-model.number="currentSeconds" 
                  class="timeline-slider" 
                />
              </div>

              <span class="timeline-readout">
                {{ formatSeconds(currentSeconds) }} / {{ formatSeconds(selectedAsset.durationSeconds) }}
              </span>
            </div>
          </div>

          <!-- Instant Feedback Section (No Popups!) -->
          <div class="feedback-deck">
            <div class="feedback-top-row">
              <h3>Feedback & Director Notes</h3>
              <span class="notes-count">{{ (selectedAsset.comments || []).length }} Notes</span>
            </div>

            <!-- Inline Modern Add Note Capsule -->
            <form class="note-input-capsule" @submit.prevent="submitInlineNote">
              <span class="time-capsule-badge">{{ formattedCurrentTimecode }}</span>
              
              <select v-model="quickDept" class="dept-dropdown">
                <option value="EDITORIAL">Editorial</option>
                <option value="COLOR_GRADE">Color</option>
                <option value="SOUND_DESIGN">Sound</option>
                <option value="VFX">VFX</option>
              </select>

              <input 
                v-model="quickNoteText" 
                type="text" 
                placeholder="Leave feedback at current timecode (press Enter)..." 
                class="note-field"
                required
              />

              <button type="submit" class="btn-post-note" :disabled="submitting || !quickNoteText.trim()">
                <span>{{ submitting ? 'Posting...' : 'Post' }}</span>
              </button>
            </form>

            <!-- Notes List (Clean Cards) -->
            <div class="notes-cards-stream">
              <div 
                v-for="note in (selectedAsset.comments || [])" 
                :key="note.id" 
                class="note-bubble-card"
                @click="jumpToTimecode(note.timestampSeconds)"
                title="Click to jump video to this note"
              >
                <div class="note-tc-pill">{{ note.timecode }}</div>
                
                <div class="note-main">
                  <div class="note-meta-line">
                    <span class="dept-tag">{{ note.department }}</span>
                    <span class="author-tag">{{ note.userId || 'Director' }}</span>
                  </div>
                  <p class="note-text-body">{{ note.commentText }}</p>
                </div>

                <button 
                  class="btn-delete-note" 
                  @click.stop="deleteComment(note.id)" 
                  title="Delete note"
                >
                  ✕
                </button>
              </div>

              <div v-if="!selectedAsset.comments || selectedAsset.comments.length === 0" class="empty-notes-prompt">
                <p>No feedback on this take yet. Scrub the video and post a note above.</p>
              </div>
            </div>
          </div>

        </div>

        <!-- No Selection Placeholder -->
        <div v-else class="empty-stage-state">
          <div class="empty-stage-circle">
            <svg viewBox="0 0 24 24" width="32" height="32" fill="#f4c524">
              <polygon points="8,5 19,12 8,19" />
            </svg>
          </div>
          <h3>Select a Take to Review</h3>
          <p>Choose any film take from the left bin to watch the proxy, verify subtitles, and drop notes.</p>
        </div>
      </section>
    </main>

    <!-- Modal: Add New Take -->
    <div v-if="showUploadModal" class="modal-backdrop" @click.self="showUploadModal = false">
      <div class="modal-card">
        <div class="modal-card-top">
          <h3>Ingest New Film Take</h3>
          <button class="btn-modal-close" @click="showUploadModal = false">✕</button>
        </div>

        <form @submit.prevent="submitNewClip">
          <div class="form-row">
            <div class="form-group flex-2">
              <label>Scene</label>
              <input v-model="newClip.sceneId" type="text" placeholder="e.g. Scene 14" required />
            </div>
            <div class="form-group flex-1">
              <label>Take #</label>
              <input v-model.number="newClip.takeNumber" type="number" min="1" required />
            </div>
          </div>

          <div class="form-group">
            <label>Title / Description</label>
            <input v-model="newClip.title" type="text" placeholder="e.g. Rooftop Chase Master" required />
          </div>

          <div class="form-group">
            <label>Script Screenplay Dialogue</label>
            <textarea 
              v-model="newClip.scriptDialogue" 
              rows="3" 
              placeholder='e.g. DETECTIVE: "Where were you at midnight?"'
              required
            ></textarea>
          </div>

          <div class="form-row">
            <div class="form-group flex-1">
              <label>Camera Roll</label>
              <input v-model="newClip.cameraRoll" type="text" placeholder="Roll A-01" />
            </div>
            <div class="form-group flex-1">
              <label>Duration (Seconds)</label>
              <input v-model.number="newClip.durationSeconds" type="number" min="5" required />
            </div>
          </div>

          <div class="modal-actions">
            <button type="button" class="btn-cancel" @click="showUploadModal = false">Cancel</button>
            <button type="submit" class="btn-confirm" :disabled="submitting">
              {{ submitting ? 'Adding Take...' : 'Add Take' }}
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
const activeFilter = ref('ALL');
const showUploadModal = ref(false);

// Video playback
const isPlaying = ref(false);
const currentSeconds = ref(42);
let playInterval = null;

// Inline comment
const quickNoteText = ref('');
const quickDept = ref('EDITORIAL');

// New clip input
const newClip = ref({
  sceneId: '',
  takeNumber: 1,
  title: '',
  scriptDialogue: '',
  cameraRoll: 'Roll A-01',
  durationSeconds: 150,
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
  showToast(`Jumped to ${formattedCurrentTimecode.value}`);
}

function calcMarkerLeft(timestampSec) {
  const max = (selectedAsset.value && selectedAsset.value.durationSeconds) || 150;
  const pct = Math.min(100, Math.max(0, (timestampSec / max) * 100));
  return `${pct}%`;
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
    showToast(`Subtitles burned onto ${asset.sceneId}! Ready for review.`, 'success');
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
    showToast(`${asset.sceneId} locked!`, 'success');
    await fetchAll();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

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

    if (!res.ok) throw new Error(`Failed to create take`);
    const created = await res.json();
    showToast(`Added ${created.sceneId}`, 'success');
    showUploadModal.value = false;
    newClip.value = { sceneId: '', takeNumber: 1, title: '', scriptDialogue: '', cameraRoll: 'Roll A-01', durationSeconds: 150, s3BucketUrl: '' };
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
    if (!res.ok) throw new Error(`Delete take failed`);
    showToast(`Deleted ${title}`, 'success');
    await fetchAll();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

const filteredAssets = computed(() => {
  return assets.value.filter(a => {
    const q = searchQuery.value.trim().toLowerCase();
    const matchesSearch = !q || 
      a.title.toLowerCase().includes(q) ||
      a.sceneId.toLowerCase().includes(q) ||
      (a.scriptDialogue && a.scriptDialogue.toLowerCase().includes(q));
    const matchesFilter = activeFilter.value === 'ALL' || a.status === activeFilter.value;
    return matchesSearch && matchesFilter;
  });
});

function formatStatus(st) {
  switch (st) {
    case 'UPLOADED': return 'Needs Subtitles';
    case 'PROCESSING_BURN_IN': return 'Processing';
    case 'READY_FOR_REVIEW': return 'Ready';
    case 'APPROVED': return 'Locked';
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
/* Ultra-Sleek Modern Studio (Linear / RunwayML / Apple TV+ aesthetic) in #f4c524 and Black */

.studio-app {
  max-width: 1440px;
  margin: 0 auto;
  min-height: 100vh;
  padding: 1.5rem 2rem 2.5rem 2rem;
  background-color: #000000;
  color: #ffffff;
  font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

/* 1. Header */
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-group {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-titles {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.brand-name {
  font-size: 1.25rem;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: -0.02em;
}

.brand-badge {
  background: rgba(244, 197, 36, 0.12);
  color: #f4c524;
  border: 1px solid rgba(244, 197, 36, 0.3);
  font-size: 0.65rem;
  font-weight: 800;
  padding: 0.15rem 0.5rem;
  border-radius: 9999px;
  letter-spacing: 0.06em;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.take-counter {
  font-size: 0.85rem;
  color: #888890;
  font-weight: 500;
  margin-right: 0.25rem;
}

.btn-ghost {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: transparent;
  color: #888890;
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 0.5rem 0.9rem;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-ghost:hover:not(:disabled) {
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.25);
  background: rgba(255, 255, 255, 0.04);
}
.btn-ghost:disabled {
  opacity: 0.4;
}

.btn-gold {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: #f4c524;
  color: #000000;
  border: none;
  padding: 0.5rem 1.15rem;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  box-shadow: 0 4px 14px rgba(244, 197, 36, 0.25);
}
.btn-gold:hover {
  background: #dfb31b;
  transform: translateY(-1px);
}

.spin {
  animation: spin 0.8s linear infinite;
}
@keyframes spin { 100% { transform: rotate(360deg); } }

/* Toast */
.app-toast {
  position: fixed;
  top: 1.5rem;
  right: 2rem;
  background: #09090b;
  border: 1px solid rgba(244, 197, 36, 0.4);
  color: #ffffff;
  padding: 0.75rem 1.25rem;
  border-radius: 10px;
  font-size: 0.85rem;
  font-weight: 600;
  z-index: 500;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.8), 0 0 15px rgba(244, 197, 36, 0.2);
}
.toast-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #f4c524;
}

/* 2. Main Studio Layout */
.app-layout {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 2rem;
  align-items: start;
}

@media (max-width: 1024px) {
  .app-layout {
    grid-template-columns: 1fr;
  }
}

/* Left: Sidebar Bin */
.sidebar-bin {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.sidebar-header {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.sidebar-search {
  position: relative;
  display: flex;
  align-items: center;
  background: #0c0c0e;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 0.55rem 0.85rem;
  gap: 0.5rem;
  color: #71717a;
  transition: all 0.15s ease;
}
.sidebar-search:focus-within {
  border-color: #f4c524;
  box-shadow: 0 0 0 1px #f4c524;
  color: #ffffff;
}
.sidebar-search input {
  flex: 1;
  background: transparent;
  border: none;
  color: #ffffff;
  font-size: 0.85rem;
  outline: none;
}
.clear-search-btn {
  background: transparent;
  border: none;
  color: #71717a;
  cursor: pointer;
}

.filter-row {
  display: flex;
  gap: 0.4rem;
  overflow-x: auto;
  padding-bottom: 2px;
}

.filter-tag {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #71717a;
  padding: 0.3rem 0.65rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}
.filter-tag:hover {
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.2);
}
.filter-tag.active {
  background: rgba(244, 197, 36, 0.12);
  color: #f4c524;
  border-color: rgba(244, 197, 36, 0.4);
}

.takes-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
  padding-right: 4px;
}

/* Take Card */
.take-card {
  background: #09090b;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 10px;
  padding: 1rem;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  transition: all 0.15s ease;
}
.take-card:hover {
  background: #111113;
  border-color: rgba(255, 255, 255, 0.15);
}
.take-card.selected {
  background: #121214;
  border-color: #f4c524;
  box-shadow: 0 0 20px rgba(244, 197, 36, 0.15);
}

.take-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.take-scene {
  font-size: 0.8rem;
  font-weight: 700;
  color: #f4c524;
  font-family: 'JetBrains Mono', monospace;
}

.take-status-pill {
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
}
.pill-uploaded {
  background: rgba(255, 255, 255, 0.06);
  color: #a1a1aa;
}
.pill-ready_for_review {
  background: rgba(244, 197, 36, 0.15);
  color: #f4c524;
}
.pill-approved {
  background: #ffffff;
  color: #000000;
  font-weight: 800;
}

.take-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: #ffffff;
  line-height: 1.3;
}

.take-dialogue {
  font-size: 0.8rem;
  color: #a1a1aa;
  font-style: italic;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.take-card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.25rem;
}

.take-runtime {
  font-size: 0.75rem;
  color: #71717a;
  font-family: 'JetBrains Mono', monospace;
}

.take-actions {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.btn-burn-action {
  background: #f4c524;
  color: #000000;
  border: none;
  font-size: 0.75rem;
  font-weight: 800;
  padding: 0.3rem 0.75rem;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-burn-action:hover {
  background: #dfb31b;
}

.btn-lock-action {
  background: transparent;
  color: #f4c524;
  border: 1px solid #f4c524;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.25rem 0.65rem;
  border-radius: 5px;
  cursor: pointer;
}
.btn-lock-action:hover {
  background: #f4c524;
  color: #000000;
}

.btn-delete-action {
  background: transparent;
  border: none;
  color: #52525b;
  cursor: pointer;
  padding: 0.2rem;
  font-size: 0.8rem;
}
.btn-delete-action:hover {
  color: #ffffff;
}

.empty-bin-state {
  text-align: center;
  padding: 3rem 1rem;
  color: #71717a;
  font-size: 0.85rem;
}

/* 3. Center & Right: Hero Cinema Stage */
.main-stage {
  display: flex;
  flex-direction: column;
}

.stage-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Cinema Player Card */
.cinema-player-card {
  background: #09090b;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 40px -15px rgba(0, 0, 0, 0.9);
}

.player-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  background: #0c0c0e;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.take-ident {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.take-accent-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #52525b;
}
.take-accent-dot.live {
  background: #f4c524;
  box-shadow: 0 0 10px #f4c524;
}

.take-ident-title {
  font-size: 1rem;
  font-weight: 800;
  color: #ffffff;
}

.take-ident-sub {
  font-size: 0.8rem;
  color: #71717a;
  font-family: 'JetBrains Mono', monospace;
}

.smpte-pill {
  background: #000000;
  border: 1px solid rgba(244, 197, 36, 0.35);
  color: #f4c524;
  font-family: 'JetBrains Mono', monospace;
  font-size: 1rem;
  font-weight: 800;
  padding: 0.35rem 0.85rem;
  border-radius: 6px;
  letter-spacing: 0.05em;
  text-shadow: 0 0 10px rgba(244, 197, 36, 0.3);
}

/* Viewport Box (16:9 Cinema Aspect) */
.viewport-box {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  max-height: 440px;
  background: #000000;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: center;
  padding: 2rem;
  cursor: pointer;
  background-image: radial-gradient(circle at center, #111114 0%, #000000 85%);
  overflow: hidden;
}

/* Center Play Button Overlay */
.play-overlay-btn {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.75);
  border: 1.5px solid #f4c524;
  display: flex;
  align-items: center;
  justify-content: center;
  padding-left: 3px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.8), 0 0 20px rgba(244, 197, 36, 0.3);
  transition: all 0.2s ease;
}
.viewport-box:hover .play-overlay-btn {
  transform: translate(-50%, -50%) scale(1.08);
  background: #000000;
  box-shadow: 0 10px 35px rgba(0, 0, 0, 0.9), 0 0 25px rgba(244, 197, 36, 0.45);
}

/* Floating Subtitle Capsule */
.screenplay-subtitle-capsule {
  max-width: 640px;
  width: 100%;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 10px;
  padding: 0.85rem 1.25rem;
  text-align: center;
  z-index: 2;
  transition: all 0.2s ease;
}
.screenplay-subtitle-capsule.is-burned {
  border-color: #f4c524;
  box-shadow: 0 0 25px rgba(244, 197, 36, 0.2);
}

.subtitle-badge {
  display: block;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.65rem;
  font-weight: 800;
  color: #f4c524;
  letter-spacing: 0.08em;
  margin-bottom: 0.3rem;
}

.subtitle-line {
  font-size: 1.05rem;
  font-weight: 600;
  color: #f4c524;
  line-height: 1.4;
}

/* Player Transport & Scrubber Deck */
.player-transport-deck {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  padding: 1rem 1.5rem;
  background: #0c0c0e;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.btn-play-pause {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: #f4c524;
  color: #000000;
  border: none;
  font-size: 0.85rem;
  font-weight: 800;
  padding: 0.45rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}
.btn-play-pause:hover {
  background: #dfb31b;
}
.btn-play-pause.playing {
  background: #000000;
  color: #f4c524;
  border: 1px solid #f4c524;
}

.scrubber-container {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}

.scrubber-pins {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.scrubber-pin {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 6px;
  height: 14px;
  background: #f4c524;
  border-radius: 2px;
  pointer-events: auto;
  cursor: pointer;
  z-index: 5;
  box-shadow: 0 0 6px #f4c524;
  transition: transform 0.15s ease;
}
.scrubber-pin:hover {
  transform: translate(-50%, -50%) scale(1.4);
}

.timeline-slider {
  width: 100%;
  accent-color: #f4c524;
  cursor: pointer;
  height: 5px;
}

.timeline-readout {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.85rem;
  color: #888890;
  font-weight: 600;
  white-space: nowrap;
}

/* 4. Instant Feedback Deck */
.feedback-deck {
  background: #09090b;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.feedback-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.feedback-top-row h3 {
  font-size: 1.1rem;
  font-weight: 800;
  color: #ffffff;
}
.notes-count {
  font-size: 0.8rem;
  font-weight: 700;
  color: #f4c524;
  background: rgba(244, 197, 36, 0.12);
  border: 1px solid rgba(244, 197, 36, 0.3);
  padding: 0.2rem 0.65rem;
  border-radius: 9999px;
}

/* Note Input Capsule */
.note-input-capsule {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  background: #000000;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 10px;
  padding: 0.45rem 0.65rem;
  transition: border-color 0.15s ease;
}
.note-input-capsule:focus-within {
  border-color: #f4c524;
  box-shadow: 0 0 0 1px #f4c524;
}

.time-capsule-badge {
  background: #111113;
  color: #f4c524;
  border: 1px solid rgba(244, 197, 36, 0.3);
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8rem;
  font-weight: 700;
  padding: 0.35rem 0.65rem;
  border-radius: 6px;
  white-space: nowrap;
}

.dept-dropdown {
  background: #111113;
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #ffffff;
  padding: 0.4rem 0.65rem;
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 600;
  outline: none;
}

.note-field {
  flex: 1;
  background: transparent;
  border: none;
  color: #ffffff;
  font-size: 0.9rem;
  padding: 0.4rem;
  outline: none;
}

.btn-post-note {
  background: #f4c524;
  color: #000000;
  border: none;
  font-size: 0.85rem;
  font-weight: 800;
  padding: 0.45rem 1.15rem;
  border-radius: 6px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}
.btn-post-note:hover:not(:disabled) {
  background: #dfb31b;
}
.btn-post-note:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* Notes Cards Stream */
.notes-cards-stream {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  max-height: 280px;
  overflow-y: auto;
}

.note-bubble-card {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  background: #000000;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;
  padding: 0.85rem 1rem;
  cursor: pointer;
  transition: all 0.15s ease;
}
.note-bubble-card:hover {
  background: #0c0c0e;
  border-color: rgba(244, 197, 36, 0.4);
}

.note-tc-pill {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.85rem;
  font-weight: 800;
  color: #f4c524;
  background: #111113;
  padding: 0.25rem 0.55rem;
  border-radius: 4px;
  white-space: nowrap;
}

.note-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.note-meta-line {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.dept-tag {
  font-size: 0.65rem;
  font-weight: 800;
  color: #f4c524;
  text-transform: uppercase;
}
.author-tag {
  font-size: 0.75rem;
  color: #71717a;
}

.note-text-body {
  font-size: 0.9rem;
  color: #e4e4e7;
  line-height: 1.4;
}

.btn-delete-note {
  background: transparent;
  border: none;
  color: #52525b;
  cursor: pointer;
  font-size: 0.85rem;
  padding: 0.2rem;
}
.btn-delete-note:hover {
  color: #ffffff;
}

.empty-notes-prompt {
  text-align: center;
  padding: 2.5rem 1rem;
  color: #71717a;
  font-size: 0.85rem;
}

/* Empty Stage State */
.empty-stage-state {
  margin: auto;
  text-align: center;
  padding: 6rem 2rem;
  color: #71717a;
}
.empty-stage-circle {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(244, 197, 36, 0.1);
  border: 1px solid rgba(244, 197, 36, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.5rem auto;
  padding-left: 4px;
}
.empty-stage-state h3 {
  font-size: 1.2rem;
  font-weight: 800;
  color: #ffffff;
  margin-bottom: 0.4rem;
}
.empty-stage-state p {
  font-size: 0.9rem;
  max-width: 440px;
  margin: 0 auto;
}

/* Modal: Clean Modern Card */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 500;
  padding: 1.5rem;
}

.modal-card {
  background: #09090b;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 14px;
  width: 100%;
  max-width: 500px;
  padding: 1.75rem;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.95), 0 0 25px rgba(244, 197, 36, 0.15);
}

.modal-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}
.modal-card-top h3 {
  font-size: 1.2rem;
  font-weight: 800;
  color: #ffffff;
}

.btn-modal-close {
  background: transparent;
  border: none;
  color: #71717a;
  font-size: 1.2rem;
  cursor: pointer;
}
.btn-modal-close:hover {
  color: #ffffff;
}

.form-row {
  display: flex;
  gap: 0.85rem;
}
.flex-1 { flex: 1; }
.flex-2 { flex: 2; }

.form-group {
  margin-bottom: 1rem;
}
.form-group label {
  display: block;
  font-size: 0.8rem;
  font-weight: 600;
  color: #a1a1aa;
  margin-bottom: 0.35rem;
}
.form-group input, .form-group textarea {
  width: 100%;
  background: #000000;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  padding: 0.6rem 0.85rem;
  color: #ffffff;
  font-family: inherit;
  font-size: 0.85rem;
  outline: none;
  transition: all 0.15s ease;
}
.form-group input:focus, .form-group textarea:focus {
  border-color: #f4c524;
  box-shadow: 0 0 0 1px #f4c524;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.75rem;
}

.btn-cancel {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #a1a1aa;
  padding: 0.55rem 1.1rem;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-cancel:hover {
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.25);
}

.btn-confirm {
  background: #f4c524;
  color: #000000;
  border: none;
  padding: 0.55rem 1.35rem;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-confirm:hover:not(:disabled) {
  background: #dfb31b;
}
</style>
