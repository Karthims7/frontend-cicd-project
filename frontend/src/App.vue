<template>
  <div class="studio-workstation">
    <!-- Top Broadcast Console Header -->
    <header class="console-header">
      <div class="console-brand-cluster">
        <div class="hardware-logo">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none">
            <rect x="2" y="2" width="20" height="20" rx="3" fill="#000000" stroke="#f4c524" stroke-width="2"/>
            <polygon points="9,6 18,12 9,18" fill="#f4c524"/>
          </svg>
        </div>
        <div class="brand-text-block">
          <div class="brand-line">
            <span class="brand-name">COLLABCUT</span>
            <span class="brand-model">NLE PRO // v2.4</span>
            <span class="system-status-pill">
              <span class="status-pulse"></span>
              <span>ENGINE READY</span>
            </span>
          </div>
          <div class="telemetry-bar">
            <span>PROJECT: <em>CHRONICLES OF DAWN</em></span>
            <span class="sep">/</span>
            <span>COLOR: ACEScc 1.3</span>
            <span class="sep">/</span>
            <span>TIMEBASE: 24.000 FPS</span>
            <span class="sep">/</span>
            <span>CLUSTER: MINIKUBE K8S</span>
          </div>
        </div>
      </div>

      <div class="console-actions-cluster">
        <div class="telemetry-stat">
          <span class="stat-num">{{ assets.length }}</span>
          <span class="stat-label">INGESTED TAKES</span>
        </div>
        <button class="tactile-btn tactile-sync" :disabled="loading" @click="fetchAll" title="Sync Reel Metadata">
          <svg class="sync-icon" :class="{ 'spin': loading }" viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
            <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/>
          </svg>
          <span>SYNC REEL</span>
        </button>
        <button class="tactile-btn tactile-primary" @click="showUploadModal = true">
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          <span>INGEST TAKE</span>
        </button>
      </div>
    </header>

    <!-- System Toast Message -->
    <transition name="fade">
      <div v-if="toast.show" class="hardware-toast" :class="toast.type">
        <span class="toast-indicator">▶</span>
        <span>{{ toast.message }}</span>
      </div>
    </transition>

    <!-- Main Dual-Deck Workspace -->
    <main class="console-body">
      <!-- Left: Master Ingest Media Pool -->
      <aside class="media-pool-deck">
        <div class="deck-header">
          <div class="deck-title-row">
            <div class="deck-title">MEDIA POOL // FOOTAGE BINS</div>
            <div class="filter-pills">
              <button 
                class="filter-pill" 
                :class="{ 'active': activeFilter === 'ALL' }" 
                @click="activeFilter = 'ALL'"
              >
                ALL ({{ assets.length }})
              </button>
              <button 
                class="filter-pill" 
                :class="{ 'active': activeFilter === 'UPLOADED' }" 
                @click="activeFilter = 'UPLOADED'"
              >
                RAW ({{ countStatus('UPLOADED') }})
              </button>
              <button 
                class="filter-pill" 
                :class="{ 'active': activeFilter === 'READY_FOR_REVIEW' }" 
                @click="activeFilter = 'READY_FOR_REVIEW'"
              >
                READY ({{ countStatus('READY_FOR_REVIEW') }})
              </button>
              <button 
                class="filter-pill" 
                :class="{ 'active': activeFilter === 'APPROVED' }" 
                @click="activeFilter = 'APPROVED'"
              >
                LOCK ({{ countStatus('APPROVED') }})
              </button>
            </div>
          </div>

          <div class="bin-search-bar">
            <span class="search-prefix">CMD:</span>
            <input 
              v-model="searchQuery" 
              type="text" 
              placeholder="Search Scene, Take, Roll or Dialogue line..." 
            />
            <button v-if="searchQuery" class="clear-btn" @click="searchQuery = ''">✕</button>
          </div>
        </div>

        <!-- Clips Feed -->
        <div class="bins-scroll-area">
          <div 
            v-for="asset in filteredAssets" 
            :key="asset.id"
            class="slate-card"
            :class="{ 'slate-selected': selectedAsset && selectedAsset.id === asset.id }"
            @click="selectAsset(asset)"
          >
            <!-- Slate Top Telemetry -->
            <div class="slate-meta-bar">
              <span class="slate-scene-take">{{ asset.sceneId }} // TK {{ asset.takeNumber }}</span>
              <span class="slate-status-badge" :class="'status-' + asset.status.toLowerCase()">
                {{ formatStatus(asset.status) }}
              </span>
            </div>

            <!-- Anamorphic Scope Preview Frame -->
            <div class="anamorphic-scope-box">
              <div class="crop-mark top-left"></div>
              <div class="crop-mark top-right"></div>
              <div class="crop-mark bottom-left"></div>
              <div class="crop-mark bottom-right"></div>
              
              <div class="scope-inner">
                <div class="scope-title">{{ asset.title }}</div>
                <div class="scope-specs">
                  <span>{{ asset.cameraRoll }}</span>
                  <span class="bullet">•</span>
                  <span>{{ asset.fps }} FPS</span>
                  <span class="bullet">•</span>
                  <span>PRORES 422</span>
                </div>
              </div>
            </div>

            <!-- Dialogue Teleprompter Snippet -->
            <div v-if="asset.scriptDialogue" class="slate-dialogue-block">
              <div class="dialogue-cue">SCREENPLAY // CUE:</div>
              <div class="dialogue-quote">"{{ asset.scriptDialogue }}"</div>
            </div>

            <!-- Slate Hardware Actions -->
            <div class="slate-footer">
              <span class="slate-runtime">TC {{ formatSeconds(asset.durationSeconds) }} // {{ (asset.comments || []).length }} NOTES</span>
              
              <div class="slate-btns" @click.stop>
                <button 
                  v-if="asset.status === 'UPLOADED'" 
                  class="action-btn action-burn" 
                  @click="triggerBurnIn(asset)"
                  title="Simulate FFmpeg dialogue burn-in worker"
                >
                  <span>⚡ BURN SUBTITLES</span>
                </button>
                <button 
                  v-else-if="asset.status === 'READY_FOR_REVIEW'" 
                  class="action-btn action-lock" 
                  @click="updateStatus(asset, 'APPROVED')"
                  title="Commit picture lock"
                >
                  <span>🔒 LOCK CUT</span>
                </button>
                <button 
                  class="action-btn-del" 
                  @click="deleteAsset(asset.id, asset.title)" 
                  title="Delete Take"
                >
                  ✕
                </button>
              </div>
            </div>
          </div>

          <div v-if="filteredAssets.length === 0" class="empty-bin-notice">
            <span class="empty-flag">// NO MATCHING TAKES FOUND //</span>
          </div>
        </div>
      </aside>

      <!-- Right: Master Cinema Grading & Review Deck -->
      <section class="master-cinema-deck">
        <div v-if="selectedAsset" class="viewport-wrapper">
          
          <!-- Cinema Stage Monitor Screen -->
          <div class="broadcast-viewport">
            <!-- Top Broadcast Overlay Strip -->
            <div class="viewport-hud-top">
              <div class="hud-left">
                <span class="rec-led" :class="{ 'live': isPlaying }"></span>
                <span class="hud-tag">{{ isPlaying ? 'PLAYING 24.00' : 'STBY // PAUSED' }}</span>
                <span class="hud-lens">A-CAM • 35mm T1.5</span>
              </div>
              <div class="hud-smpte-display">
                <span class="smpte-digits">{{ formattedCurrentTimecode }}</span>
              </div>
              <div class="hud-right">
                <span class="hud-format">4096x2160 DCI</span>
                <span class="hud-scene">{{ selectedAsset.sceneId }} // TK {{ selectedAsset.takeNumber }}</span>
              </div>
            </div>

            <!-- 2.39:1 / 16:9 Cinema Letterbox Stage -->
            <div class="cinema-display-canvas">
              <!-- Viewfinder Optical Guides -->
              <div class="frame-guide action-safe"></div>
              <div class="frame-guide title-safe"></div>
              <div class="center-crosshair">
                <div class="ch-h"></div>
                <div class="ch-v"></div>
              </div>

              <!-- Scene Ident Text -->
              <div class="cinema-scene-ident">
                <div class="cinema-main-title">{{ selectedAsset.title }}</div>
                <div class="cinema-tech-meta">
                  <span>{{ selectedAsset.cameraRoll }}</span>
                  <span class="sep">/</span>
                  <span>COLOR: ARRI REC709</span>
                  <span class="sep">/</span>
                  <span>AUDIO: 48kHz 24-BIT</span>
                </div>
              </div>

              <!-- Dialogue Subtitle Overlay (Authentic Broadcast Burn-In) -->
              <div class="burned-subtitle-deck" :class="{ 'has-burned': selectedAsset.processedS3Url }">
                <div class="burned-indicator">
                  <span class="indicator-mark">●</span>
                  <span>{{ selectedAsset.processedS3Url ? 'FFMPEG BURN-IN TRACK' : 'SCREENPLAY WORK PROXY' }}</span>
                </div>
                <div class="subtitle-text">
                  "{{ selectedAsset.scriptDialogue || 'No dialogue assigned for this take.' }}"
                </div>
              </div>
            </div>

            <!-- Stereo Audio VU Peak Meters (Pulsing during playback) -->
            <div class="audio-vu-deck">
              <div class="vu-channel">
                <span class="vu-label">L</span>
                <div class="vu-meter-bar">
                  <div class="vu-segment" v-for="n in 20" :key="'l-'+n" :class="getVuClass(n)"></div>
                </div>
              </div>
              <div class="vu-channel">
                <span class="vu-label">R</span>
                <div class="vu-meter-bar">
                  <div class="vu-segment" v-for="n in 20" :key="'r-'+n" :class="getVuClass(n)"></div>
                </div>
              </div>
              <div class="vu-db-scale">
                <span>-36</span>
                <span>-24</span>
                <span>-18</span>
                <span>-12</span>
                <span>-6</span>
                <span>0dB</span>
                <span class="peak-label">PEAK</span>
              </div>
            </div>

            <!-- Precision Transport Deck -->
            <div class="transport-control-strip">
              <!-- Transport Hardware Keys -->
              <div class="transport-keys">
                <button class="t-btn" @click="stepFrame(-1)" title="Step -1 Frame [◄]">
                  <span>|◄</span>
                </button>
                <button class="t-btn" @click="jumpTime(-5)" title="Jump -5s [J]">
                  <span>◄◄</span>
                </button>
                <button 
                  class="t-btn t-play" 
                  :class="{ 't-playing': isPlaying }" 
                  @click="togglePlay"
                  :title="isPlaying ? 'Pause [SPACE]' : 'Play [SPACE]'"
                >
                  <svg v-if="!isPlaying" viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                    <path d="M8 5v14l11-7z"/>
                  </svg>
                  <svg v-else viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                    <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>
                  </svg>
                  <span>{{ isPlaying ? 'PAUSE' : 'PLAY' }}</span>
                </button>
                <button class="t-btn" @click="jumpTime(5)" title="Jump +5s [L]">
                  <span>►►</span>
                </button>
                <button class="t-btn" @click="stepFrame(1)" title="Step +1 Frame [►]">
                  <span>►|</span>
                </button>
              </div>

              <!-- Timecode Timeline Ruler & Marker Scrubber -->
              <div class="timeline-scrubber-container">
                <div class="timeline-tick-ruler">
                  <span>00:00</span>
                  <span>00:30</span>
                  <span>01:00</span>
                  <span>01:30</span>
                  <span>02:00</span>
                  <span>02:30</span>
                </div>

                <div class="scrubber-track-wrapper">
                  <!-- Visual Marker Pins on Timeline -->
                  <div 
                    v-for="note in (selectedAsset.comments || [])" 
                    :key="'pin-'+note.id" 
                    class="timeline-marker-pin"
                    :style="{ left: calcMarkerLeft(note.timestampSeconds) }"
                    :title="note.timecode + ' - ' + note.commentText"
                    @click.stop="jumpToTimecode(note.timestampSeconds)"
                  >
                    <span class="marker-diamond">◆</span>
                  </div>

                  <input 
                    type="range" 
                    min="0" 
                    :max="selectedAsset.durationSeconds || 150" 
                    v-model.number="currentSeconds" 
                    class="hardware-scrubber" 
                  />
                </div>
              </div>

              <div class="transport-readout">
                <span class="cur-tc">{{ formattedCurrentTimecode }}</span>
                <span class="sep">/</span>
                <span class="tot-tc">{{ formatSeconds(selectedAsset.durationSeconds) }}:00</span>
              </div>
            </div>
          </div>

          <!-- Edit Decision List (EDL) Director Marker Thread -->
          <div class="edl-marker-deck">
            <div class="edl-header-row">
              <div class="edl-title-block">
                <div class="edl-title">EDIT DECISION LIST // DIRECTORS LOG</div>
                <div class="edl-sub">Frame-accurate feedback stored directly in PostgreSQL database</div>
              </div>
              <div class="edl-count-badge">{{ (selectedAsset.comments || []).length }} MARKERS</div>
            </div>

            <!-- Instant Ingest Bar (No Dialog Popups) -->
            <form class="edl-entry-bar" @submit.prevent="submitInlineNote">
              <div class="entry-tc-lock">
                <span class="tc-lock-label">SMPTE:</span>
                <span class="tc-lock-val">{{ formattedCurrentTimecode }}</span>
              </div>
              <div class="dept-selector-cluster">
                <select v-model="quickDept" class="hardware-select">
                  <option value="EDITORIAL">EDITORIAL</option>
                  <option value="COLOR_GRADE">COLOR GRADE</option>
                  <option value="SOUND_DESIGN">SOUND DESIGN</option>
                  <option value="VFX">VFX</option>
                </select>
              </div>
              <input 
                v-model="quickNoteText" 
                type="text" 
                placeholder="Log frame note (e.g. Trim head 4 frames; boost color saturation)..." 
                class="hardware-input"
                required
              />
              <button type="submit" class="tactile-btn tactile-post" :disabled="submitting || !quickNoteText.trim()">
                <span>{{ submitting ? 'LOGGING...' : '+ LOG MARKER' }}</span>
              </button>
            </form>

            <!-- EDL Marker Table -->
            <div class="edl-table-wrapper">
              <table class="edl-table">
                <thead>
                  <tr>
                    <th style="width: 130px;">SMPTE TC</th>
                    <th style="width: 120px;">DEPARTMENT</th>
                    <th style="width: 120px;">LOGGED BY</th>
                    <th>EDITORIAL DECISION / NOTE</th>
                    <th style="width: 60px; text-align: right;">ACTION</th>
                  </tr>
                </thead>
                <tbody>
                  <tr 
                    v-for="note in (selectedAsset.comments || [])" 
                    :key="note.id" 
                    class="edl-row"
                    @click="jumpToTimecode(note.timestampSeconds)"
                    title="Click row to seek player to this frame"
                  >
                    <td>
                      <span class="edl-tc-tag">
                        <span class="tc-play-hint">►</span>
                        <span>{{ note.timecode }}</span>
                      </span>
                    </td>
                    <td>
                      <span class="edl-dept-tag" :class="'dept-' + (note.department || 'editorial').toLowerCase()">
                        {{ note.department }}
                      </span>
                    </td>
                    <td><span class="edl-author">{{ note.userId || 'Director' }}</span></td>
                    <td><span class="edl-note-text">{{ note.commentText }}</span></td>
                    <td style="text-align: right;">
                      <button 
                        class="btn-row-del" 
                        @click.stop="deleteComment(note.id)" 
                        title="Delete marker"
                      >
                        ✕
                      </button>
                    </td>
                  </tr>

                  <tr v-if="!selectedAsset.comments || selectedAsset.comments.length === 0">
                    <td colspan="5" class="empty-edl-cell">
                      <span>// NO TIMECODED MARKERS LOGGED FOR THIS TAKE //</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

        </div>

        <!-- No Take Selected Placeholder -->
        <div v-else class="empty-deck-state">
          <div class="radar-scope-icon">
            <svg viewBox="0 0 64 64" width="64" height="64" fill="none">
              <circle cx="32" cy="32" r="28" stroke="#f4c524" stroke-width="1.5" stroke-dasharray="4 4"/>
              <circle cx="32" cy="32" r="16" stroke="rgba(244, 197, 36, 0.4)" stroke-width="1.5"/>
              <line x1="32" y1="4" x2="32" y2="60" stroke="rgba(244, 197, 36, 0.3)"/>
              <line x1="4" y1="32" x2="60" y2="32" stroke="rgba(244, 197, 36, 0.3)"/>
              <polygon points="26,20 44,32 26,44" fill="#f4c524"/>
            </svg>
          </div>
          <h3>STANDBY // SELECT TAKE FROM MEDIA POOL</h3>
          <p>Choose any film slate from the left bin to inspect proxy stream, monitor dialogue burn-in, or plot EDL markers.</p>
        </div>
      </section>
    </main>

    <!-- Modal: Ingest Take Hardware Sheet -->
    <div v-if="showUploadModal" class="modal-backdrop" @click.self="showUploadModal = false">
      <div class="hardware-modal-box">
        <div class="modal-box-header">
          <div class="modal-box-title">
            <span class="title-glyph">▶</span>
            <span>INGEST RAW FOOTAGE TAKE // CAMERA REEL</span>
          </div>
          <button class="btn-box-close" @click="showUploadModal = false">✕</button>
        </div>

        <form @submit.prevent="submitNewClip">
          <div class="form-grid-row">
            <div class="form-cell flex-2">
              <label>SCENE IDENTIFIER</label>
              <input v-model="newClip.sceneId" type="text" placeholder="e.g. Scene 12B" required />
            </div>
            <div class="form-cell flex-1">
              <label>TAKE #</label>
              <input v-model.number="newClip.takeNumber" type="number" min="1" required />
            </div>
          </div>

          <div class="form-cell">
            <label>TAKE TITLE / SLATE DESCRIPTION</label>
            <input v-model="newClip.title" type="text" placeholder="e.g. Rooftop Confrontation Wide Master" required />
          </div>

          <div class="form-cell">
            <label>SCREENPLAY DIALOGUE (FOR FFmpeg BURN-IN WORKER)</label>
            <textarea 
              v-model="newClip.scriptDialogue" 
              rows="3" 
              placeholder='e.g. DETECTIVE: "Where were you the night of the incident?"'
              required
            ></textarea>
          </div>

          <div class="form-grid-row">
            <div class="form-cell flex-1">
              <label>CAMERA ROLL IDENT</label>
              <input v-model="newClip.cameraRoll" type="text" placeholder="Roll A-04" />
            </div>
            <div class="form-cell flex-1">
              <label>DURATION (SECONDS)</label>
              <input v-model.number="newClip.durationSeconds" type="number" min="5" required />
            </div>
          </div>

          <div class="modal-box-actions">
            <button type="button" class="tactile-btn tactile-cancel" @click="showUploadModal = false">ABORT</button>
            <button type="submit" class="tactile-btn tactile-commit" :disabled="submitting">
              {{ submitting ? 'INGESTING...' : 'COMMIT TAKE TO POSTGRESQL' }}
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

// Video scrubber state
const isPlaying = ref(false);
const currentSeconds = ref(42);
let playInterval = null;

// Audio VU meter simulation
const vuPulse = ref(12);
let vuInterval = null;

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

    vuInterval = setInterval(() => {
      vuPulse.value = Math.floor(Math.random() * 12) + 6;
    }, 100);
  } else {
    clearInterval(playInterval);
    clearInterval(vuInterval);
    vuPulse.value = 0;
  }
}

function stepFrame(step) {
  const max = (selectedAsset.value && selectedAsset.value.durationSeconds) || 150;
  let next = currentSeconds.value + step;
  if (next < 0) next = 0;
  if (next > max) next = max;
  currentSeconds.value = next;
}

function jumpTime(sec) {
  const max = (selectedAsset.value && selectedAsset.value.durationSeconds) || 150;
  let next = currentSeconds.value + sec;
  if (next < 0) next = 0;
  if (next > max) next = max;
  currentSeconds.value = next;
}

function jumpToTimecode(sec) {
  currentSeconds.value = sec;
  showToast(`SEEK: Jumped playhead to ${formattedCurrentTimecode.value}`);
}

function calcMarkerLeft(timestampSec) {
  const max = (selectedAsset.value && selectedAsset.value.durationSeconds) || 150;
  const pct = Math.min(100, Math.max(0, (timestampSec / max) * 100));
  return `${pct}%`;
}

function getVuClass(index) {
  if (!isPlaying.value) return 'off';
  if (index <= vuPulse.value) {
    if (index >= 18) return 'peak';
    if (index >= 14) return 'warn';
    return 'on';
  }
  return 'off';
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
    showToast(`FFMPEG: Subtitles burned onto ${asset.sceneId}. Proxy ready for screening!`, 'success');
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
    showToast(`LOCK: ${asset.sceneId} committed to picture lock!`, 'success');
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
    showToast(`EDL: Marker plotted at ${payload.timecode}!`, 'success');
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
    showToast(`EDL: Marker removed.`, 'success');
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
    showToast(`INGEST: Committed take ${created.sceneId}`, 'success');
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
  if (!confirm(`Purge '${title}' from PostgreSQL reel database?`)) return;
  try {
    const res = await fetch(`/api/assets/${id}`, { method: 'DELETE' });
    if (!res.ok) throw new Error(`Delete clip failed`);
    showToast(`PURGED: Take removed from reel database.`, 'success');
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

function countStatus(st) {
  return assets.value.filter(a => a.status === st).length;
}

function formatStatus(st) {
  switch (st) {
    case 'UPLOADED': return 'RAW PROXY';
    case 'PROCESSING_BURN_IN': return 'BURNING...';
    case 'READY_FOR_REVIEW': return 'READY';
    case 'APPROVED': return 'LOCKED';
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
  if (vuInterval) clearInterval(vuInterval);
});
</script>

<style scoped>
/* High-End Industrial Studio Hardware Console Styling (#f4c524 and Black) */

.studio-workstation {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #000000;
  color: #ffffff;
  font-family: 'Plus Jakarta Sans', sans-serif;
  overflow: hidden;
  user-select: none;
}

/* 1. Hardware Console Top Bar */
.console-header {
  height: 52px;
  background: #080808;
  border-bottom: 1.5px solid #1a1a1a;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 1rem;
  flex-shrink: 0;
}

.console-brand-cluster {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.hardware-logo {
  background: #000000;
  border: 1.5px solid #f4c524;
  border-radius: 4px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 10px rgba(244, 197, 36, 0.25);
}

.brand-text-block {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.brand-line {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.brand-name {
  font-family: 'JetBrains Mono', monospace;
  font-weight: 800;
  font-size: 1rem;
  color: #f4c524;
  letter-spacing: 0.08em;
}

.brand-model {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.7rem;
  color: #666666;
  font-weight: 600;
}

.system-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: #000000;
  border: 1px solid #1a1a1a;
  padding: 0.1rem 0.45rem;
  border-radius: 3px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.65rem;
  font-weight: 700;
  color: #f4c524;
}

.status-pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f4c524;
  box-shadow: 0 0 8px #f4c524;
}

.telemetry-bar {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.65rem;
  color: #777777;
  display: flex;
  align-items: center;
  gap: 0.45rem;
}
.telemetry-bar em {
  color: #f4c524;
  font-style: normal;
  font-weight: 600;
}
.telemetry-bar .sep {
  color: #333333;
}

.console-actions-cluster {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.telemetry-stat {
  display: flex;
  align-items: baseline;
  gap: 0.35rem;
  background: #000000;
  border: 1px solid #1f1f1f;
  padding: 0.3rem 0.65rem;
  border-radius: 4px;
}
.stat-num {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.9rem;
  font-weight: 800;
  color: #f4c524;
}
.stat-label {
  font-size: 0.65rem;
  font-weight: 700;
  color: #666666;
  letter-spacing: 0.05em;
}

/* Tactile Buttons */
.tactile-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.45rem 0.95rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 800;
  font-family: 'JetBrains Mono', monospace;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition: all 0.15s ease;
}

.tactile-sync {
  background: #000000;
  border: 1.5px solid #2a2a2a;
  color: #a3a3a3;
}
.tactile-sync:hover:not(:disabled) {
  border-color: #f4c524;
  color: #f4c524;
}
.tactile-sync:disabled {
  opacity: 0.4;
}

.tactile-primary {
  background: #f4c524;
  border: 1.5px solid #f4c524;
  color: #000000;
  box-shadow: 0 0 12px rgba(244, 197, 36, 0.25);
}
.tactile-primary:hover {
  background: #000000;
  color: #f4c524;
}

.spin {
  animation: spin 0.8s linear infinite;
}
@keyframes spin { 100% { transform: rotate(360deg); } }

/* Toast */
.hardware-toast {
  position: fixed;
  top: 60px;
  right: 1.5rem;
  background: #000000;
  border: 1.5px solid #f4c524;
  color: #f4c524;
  padding: 0.65rem 1rem;
  border-radius: 4px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8rem;
  font-weight: 700;
  z-index: 300;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.9), 0 0 15px rgba(244, 197, 36, 0.35);
}
.toast-indicator {
  font-size: 0.7rem;
}

/* 2. Workspace Body Grid */
.console-body {
  display: grid;
  grid-template-columns: 380px 1fr;
  flex: 1;
  overflow: hidden;
}

@media (max-width: 1100px) {
  .console-body {
    grid-template-columns: 1fr;
  }
}

/* Left: Media Pool Deck */
.media-pool-deck {
  background: #070707;
  border-right: 1.5px solid #1a1a1a;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.deck-header {
  padding: 0.75rem;
  border-bottom: 1.5px solid #1a1a1a;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  background: #090909;
}

.deck-title-row {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.deck-title {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
  font-weight: 800;
  color: #f4c524;
  letter-spacing: 0.08em;
}

.filter-pills {
  display: flex;
  gap: 0.35rem;
}

.filter-pill {
  flex: 1;
  background: #000000;
  border: 1px solid #1f1f1f;
  color: #666666;
  padding: 0.25rem 0.2rem;
  border-radius: 3px;
  font-size: 0.65rem;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 700;
  cursor: pointer;
  text-align: center;
  transition: all 0.15s ease;
}
.filter-pill.active {
  border-color: #f4c524;
  color: #f4c524;
  background: #111111;
}

.bin-search-bar {
  display: flex;
  align-items: center;
  background: #000000;
  border: 1px solid #222222;
  border-radius: 4px;
  padding: 0.35rem 0.6rem;
  gap: 0.4rem;
}
.search-prefix {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.65rem;
  color: #f4c524;
  font-weight: 800;
}
.bin-search-bar input {
  flex: 1;
  background: transparent;
  border: none;
  color: #ffffff;
  font-size: 0.75rem;
  outline: none;
  font-family: inherit;
}
.clear-btn {
  background: transparent;
  border: none;
  color: #555555;
  cursor: pointer;
}

.bins-scroll-area {
  padding: 0.75rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  flex: 1;
}

/* Slate Card */
.slate-card {
  background: #0a0a0a;
  border: 1.5px solid #1a1a1a;
  border-radius: 6px;
  padding: 0.75rem;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  transition: all 0.15s ease;
}
.slate-card:hover {
  border-color: #333333;
  background: #0e0e0e;
}
.slate-card.slate-selected {
  border-color: #f4c524;
  background: #101010;
  box-shadow: 0 0 15px rgba(244, 197, 36, 0.15);
}

.slate-meta-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.slate-scene-take {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8rem;
  font-weight: 800;
  color: #f4c524;
}

.slate-status-badge {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.6rem;
  font-weight: 800;
  padding: 0.1rem 0.4rem;
  border-radius: 2px;
  letter-spacing: 0.05em;
}
.status-uploaded {
  background: #000000;
  border: 1px solid #333333;
  color: #888888;
}
.status-ready_for_review {
  background: #f4c524;
  color: #000000;
  border: 1px solid #f4c524;
}
.status-approved {
  background: #000000;
  border: 1px solid #ffffff;
  color: #ffffff;
}

/* Anamorphic Scope Box */
.anamorphic-scope-box {
  position: relative;
  background: #000000;
  border: 1px solid #1f1f1f;
  border-radius: 4px;
  padding: 0.65rem;
  min-height: 54px;
}

.crop-mark {
  position: absolute;
  width: 6px;
  height: 6px;
  border-color: #f4c524;
  opacity: 0.6;
}
.crop-mark.top-left { top: 3px; left: 3px; border-top: 1.5px solid; border-left: 1.5px solid; }
.crop-mark.top-right { top: 3px; right: 3px; border-top: 1.5px solid; border-right: 1.5px solid; }
.crop-mark.bottom-left { bottom: 3px; left: 3px; border-bottom: 1.5px solid; border-left: 1.5px solid; }
.crop-mark.bottom-right { bottom: 3px; right: 3px; border-bottom: 1.5px solid; border-right: 1.5px solid; }

.scope-inner {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.scope-title {
  font-size: 0.85rem;
  font-weight: 700;
  color: #ffffff;
}
.scope-specs {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.65rem;
  color: #666666;
  display: flex;
  align-items: center;
  gap: 0.35rem;
}
.bullet { color: #333333; }

.slate-dialogue-block {
  background: #000000;
  border-left: 2px solid #f4c524;
  padding: 0.35rem 0.5rem;
  font-size: 0.75rem;
}
.dialogue-cue {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.6rem;
  color: #f4c524;
  font-weight: 700;
  margin-bottom: 0.15rem;
}
.dialogue-quote {
  color: #cccccc;
  font-style: italic;
  line-height: 1.3;
}

.slate-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.2rem;
}

.slate-runtime {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.65rem;
  color: #555555;
}

.slate-btns {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.action-btn {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.65rem;
  font-weight: 800;
  padding: 0.25rem 0.55rem;
  border-radius: 3px;
  cursor: pointer;
  letter-spacing: 0.04em;
}

.action-burn {
  background: #f4c524;
  color: #000000;
  border: 1px solid #f4c524;
}
.action-burn:hover {
  background: #000000;
  color: #f4c524;
}

.action-lock {
  background: #000000;
  border: 1px solid #f4c524;
  color: #f4c524;
}
.action-lock:hover {
  background: #f4c524;
  color: #000000;
}

.action-btn-del {
  background: transparent;
  border: none;
  color: #555555;
  cursor: pointer;
  padding: 0.2rem;
  font-size: 0.75rem;
}
.action-btn-del:hover {
  color: #ffffff;
}

.empty-bin-notice {
  text-align: center;
  padding: 3rem 1rem;
  color: #444444;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
}

/* Right: Master Cinema Deck */
.master-cinema-deck {
  background: #030303;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.viewport-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
}

/* Broadcast Viewport */
.broadcast-viewport {
  background: #000000;
  border-bottom: 2px solid #1a1a1a;
  display: flex;
  flex-direction: column;
}

.viewport-hud-top {
  height: 38px;
  background: #060606;
  border-bottom: 1px solid #151515;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 1rem;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
}

.hud-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #888888;
}

.rec-led {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #333333;
}
.rec-led.live {
  background: #f4c524;
  box-shadow: 0 0 10px #f4c524;
}

.hud-tag {
  font-weight: 800;
  color: #ffffff;
}
.hud-lens {
  color: #555555;
  font-size: 0.7rem;
}

.hud-smpte-display {
  background: #000000;
  border: 1px solid #222222;
  padding: 0.15rem 0.75rem;
  border-radius: 3px;
}
.smpte-digits {
  font-size: 1.15rem;
  font-weight: 800;
  color: #f4c524;
  letter-spacing: 0.05em;
  text-shadow: 0 0 8px rgba(244, 197, 36, 0.4);
}

.hud-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.hud-format {
  color: #555555;
  font-size: 0.7rem;
}
.hud-scene {
  color: #f4c524;
  font-weight: 800;
}

/* Cinema Display Canvas */
.cinema-display-canvas {
  position: relative;
  background: #000000;
  min-height: 280px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 2rem 2.5rem 1.5rem 2.5rem;
  overflow: hidden;
  border-bottom: 1px solid #111111;
}

/* Viewfinder Optical Guides */
.frame-guide {
  position: absolute;
  pointer-events: none;
}
.frame-guide.action-safe {
  top: 5%; left: 5%; right: 5%; bottom: 5%;
  border: 1px dashed rgba(244, 197, 36, 0.12);
}
.frame-guide.title-safe {
  top: 10%; left: 10%; right: 10%; bottom: 10%;
  border: 1px dashed rgba(255, 255, 255, 0.05);
}

.center-crosshair {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 24px;
  height: 24px;
  pointer-events: none;
  opacity: 0.25;
}
.ch-h {
  position: absolute;
  top: 11px;
  left: 0;
  right: 0;
  height: 2px;
  background: #f4c524;
}
.ch-v {
  position: absolute;
  left: 11px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: #f4c524;
}

.cinema-scene-ident {
  text-align: center;
  position: relative;
  z-index: 2;
}
.cinema-main-title {
  font-size: 1.6rem;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: -0.02em;
}
.cinema-tech-meta {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
  color: #666666;
  margin-top: 0.35rem;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
}

/* Broadcast Burned Subtitle Deck */
.burned-subtitle-deck {
  position: relative;
  z-index: 2;
  margin: 1.5rem auto 0 auto;
  max-width: 680px;
  width: 100%;
  background: rgba(0, 0, 0, 0.92);
  border: 1.5px solid #222222;
  border-radius: 4px;
  padding: 0.75rem 1.25rem;
  text-align: center;
}
.burned-subtitle-deck.has-burned {
  border-color: #f4c524;
  box-shadow: 0 0 20px rgba(244, 197, 36, 0.2);
}

.burned-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.65rem;
  font-weight: 800;
  color: #f4c524;
  letter-spacing: 0.08em;
  margin-bottom: 0.35rem;
}
.indicator-mark {
  font-size: 0.5rem;
}

.subtitle-text {
  font-size: 1.1rem;
  font-weight: 700;
  color: #f4c524;
  line-height: 1.4;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.9);
}

/* Stereo Audio VU Meters */
.audio-vu-deck {
  height: 28px;
  background: #050505;
  border-bottom: 1px solid #151515;
  display: flex;
  align-items: center;
  padding: 0 1rem;
  gap: 1.25rem;
}

.vu-channel {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex: 1;
}
.vu-label {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.65rem;
  font-weight: 800;
  color: #666666;
}
.vu-meter-bar {
  display: flex;
  gap: 2px;
  flex: 1;
  height: 8px;
  background: #000000;
  padding: 1px;
}
.vu-segment {
  flex: 1;
  height: 100%;
  background: #111111;
  border-radius: 1px;
}
.vu-segment.on {
  background: #f4c524;
  opacity: 0.85;
}
.vu-segment.warn {
  background: #f4c524;
  box-shadow: 0 0 6px #f4c524;
}
.vu-segment.peak {
  background: #ffffff;
  box-shadow: 0 0 8px #ffffff;
}

.vu-db-scale {
  display: flex;
  gap: 1.25rem;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.6rem;
  color: #444444;
}
.peak-label {
  color: #f4c524;
  font-weight: 800;
}

/* Precision Transport Control Strip */
.transport-control-strip {
  background: #080808;
  padding: 0.75rem 1rem;
  display: flex;
  align-items: center;
  gap: 1.25rem;
  border-bottom: 1.5px solid #1a1a1a;
}

.transport-keys {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.t-btn {
  background: #000000;
  border: 1px solid #222222;
  color: #888888;
  padding: 0.4rem 0.65rem;
  border-radius: 3px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
  font-weight: 800;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  transition: all 0.15s ease;
}
.t-btn:hover {
  border-color: #f4c524;
  color: #f4c524;
}

.t-play {
  background: #f4c524;
  color: #000000;
  border-color: #f4c524;
  padding: 0.45rem 1rem;
  font-weight: 800;
}
.t-play:hover {
  background: #000000;
  color: #f4c524;
}
.t-play.t-playing {
  background: #000000;
  border-color: #f4c524;
  color: #f4c524;
  box-shadow: 0 0 10px rgba(244, 197, 36, 0.3);
}

.timeline-scrubber-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.timeline-tick-ruler {
  display: flex;
  justify-content: space-between;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.65rem;
  color: #444444;
  padding: 0 2px;
}

.scrubber-track-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.timeline-marker-pin {
  position: absolute;
  top: -12px;
  transform: translateX(-50%);
  cursor: pointer;
  z-index: 5;
  color: #f4c524;
  font-size: 0.75rem;
  text-shadow: 0 0 6px #f4c524;
  transition: transform 0.15s ease;
}
.timeline-marker-pin:hover {
  transform: translateX(-50%) scale(1.4);
}

.hardware-scrubber {
  width: 100%;
  accent-color: #f4c524;
  cursor: pointer;
  height: 5px;
}

.transport-readout {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8rem;
  font-weight: 800;
  white-space: nowrap;
}
.cur-tc { color: #f4c524; }
.tot-tc { color: #666666; }

/* 3. Edit Decision List (EDL) Markers Deck */
.edl-marker-deck {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  flex: 1;
}

.edl-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.edl-title {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8rem;
  font-weight: 800;
  color: #f4c524;
  letter-spacing: 0.06em;
}
.edl-sub {
  font-size: 0.75rem;
  color: #666666;
}
.edl-count-badge {
  background: #000000;
  border: 1px solid #222222;
  color: #f4c524;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: 3px;
}

/* Ingest Entry Bar */
.edl-entry-bar {
  display: flex;
  background: #090909;
  border: 1.5px solid #222222;
  border-radius: 4px;
  padding: 0.45rem;
  gap: 0.5rem;
  align-items: center;
}

.entry-tc-lock {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  background: #000000;
  border: 1px solid #f4c524;
  padding: 0.35rem 0.65rem;
  border-radius: 3px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
  white-space: nowrap;
}
.tc-lock-label { color: #666666; font-size: 0.65rem; }
.tc-lock-val { color: #f4c524; font-weight: 800; }

.hardware-select {
  background: #000000;
  border: 1px solid #2a2a2a;
  color: #ffffff;
  padding: 0.4rem 0.65rem;
  border-radius: 3px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
  font-weight: 700;
  outline: none;
}

.hardware-input {
  flex: 1;
  background: transparent;
  border: none;
  color: #ffffff;
  font-size: 0.85rem;
  padding: 0.4rem;
  outline: none;
  font-family: inherit;
}

.tactile-post {
  background: #f4c524;
  color: #000000;
  border: 1.5px solid #f4c524;
  padding: 0.4rem 1rem;
  white-space: nowrap;
}
.tactile-post:hover:not(:disabled) {
  background: #000000;
  color: #f4c524;
}
.tactile-post:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* EDL Marker Table */
.edl-table-wrapper {
  background: #000000;
  border: 1.5px solid #1a1a1a;
  border-radius: 4px;
  overflow-y: auto;
  max-height: 240px;
}

.edl-table {
  width: 100%;
  border-collapse: collapse;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
}

.edl-table th {
  background: #080808;
  color: #666666;
  text-align: left;
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid #1a1a1a;
  font-size: 0.65rem;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.edl-row {
  border-bottom: 1px solid #111111;
  cursor: pointer;
  transition: background 0.15s ease;
}
.edl-row:hover {
  background: #0c0c0c;
}
.edl-row td {
  padding: 0.55rem 0.75rem;
}

.edl-tc-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  color: #f4c524;
  font-weight: 800;
  background: #0a0a0a;
  border: 1px solid #222222;
  padding: 0.15rem 0.45rem;
  border-radius: 2px;
}
.tc-play-hint { font-size: 0.6rem; opacity: 0.6; }

.edl-dept-tag {
  display: inline-block;
  font-size: 0.65rem;
  font-weight: 800;
  padding: 0.1rem 0.4rem;
  border-radius: 2px;
  background: #000000;
  border: 1px solid #333333;
  color: #f4c524;
}

.edl-author {
  color: #888888;
}

.edl-note-text {
  color: #e5e5e5;
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 0.8rem;
}

.btn-row-del {
  background: transparent;
  border: none;
  color: #555555;
  cursor: pointer;
}
.btn-row-del:hover {
  color: #ffffff;
}

.empty-edl-cell {
  text-align: center;
  padding: 2.5rem 1rem;
  color: #444444;
  font-size: 0.75rem;
}

/* Empty Deck State */
.empty-deck-state {
  margin: auto;
  text-align: center;
  padding: 5rem 2rem;
  color: #555555;
}
.radar-scope-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 1.5rem;
}
.empty-deck-state h3 {
  font-family: 'JetBrains Mono', monospace;
  font-size: 1rem;
  color: #f4c524;
  letter-spacing: 0.08em;
  margin-bottom: 0.5rem;
}
.empty-deck-state p {
  font-size: 0.85rem;
  color: #666666;
  max-width: 480px;
  margin: 0 auto;
}

/* Modal: Hardware Sheet */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.88);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 500;
  padding: 1.5rem;
}

.hardware-modal-box {
  background: #080808;
  border: 2px solid #f4c524;
  border-radius: 6px;
  width: 100%;
  max-width: 540px;
  padding: 1.5rem;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.98), 0 0 25px rgba(244, 197, 36, 0.25);
}

.modal-box-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1.5px solid #1f1f1f;
  padding-bottom: 1rem;
  margin-bottom: 1.25rem;
}

.modal-box-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.85rem;
  font-weight: 800;
  color: #f4c524;
  letter-spacing: 0.06em;
}
.title-glyph { font-size: 0.7rem; }

.btn-box-close {
  background: transparent;
  border: none;
  color: #666666;
  font-size: 1.2rem;
  cursor: pointer;
}
.btn-box-close:hover {
  color: #f4c524;
}

.form-grid-row {
  display: flex;
  gap: 0.85rem;
}
.flex-1 { flex: 1; }
.flex-2 { flex: 2; }

.form-cell {
  margin-bottom: 1rem;
}
.form-cell label {
  display: block;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.7rem;
  font-weight: 700;
  color: #888888;
  margin-bottom: 0.4rem;
  letter-spacing: 0.04em;
}

.form-cell input, .form-cell textarea {
  width: 100%;
  background: #000000;
  border: 1.5px solid #222222;
  border-radius: 4px;
  padding: 0.55rem 0.75rem;
  color: #ffffff;
  font-family: inherit;
  font-size: 0.85rem;
  outline: none;
  transition: border-color 0.15s ease;
}
.form-cell input:focus, .form-cell textarea:focus {
  border-color: #f4c524;
}

.modal-box-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.5rem;
  border-top: 1.5px solid #1f1f1f;
  padding-top: 1rem;
}

.tactile-cancel {
  background: #000000;
  border: 1.5px solid #2a2a2a;
  color: #888888;
}
.tactile-cancel:hover {
  border-color: #666666;
  color: #ffffff;
}

.tactile-commit {
  background: #f4c524;
  border: 1.5px solid #f4c524;
  color: #000000;
}
.tactile-commit:hover:not(:disabled) {
  background: #000000;
  color: #f4c524;
}
</style>
