<template>
  <div class="studio-app">
    <!-- Clean Minimalist Header (Linear / Apple TV+ aesthetic) -->
    <header class="app-header">
      <div class="brand-group">
        <div class="brand-logo">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none">
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
        <button class="btn-ghost" :disabled="loading" @click="fetchAll" title="Refresh takes from database">
          <svg :class="{ 'spin': loading }" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
            <path d="M21 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/>
          </svg>
          <span>Refresh</span>
        </button>
        <button class="btn-gold" @click="openUploadModal">
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
              <div class="take-top-right">
                <span class="take-status-pill" :class="'pill-' + asset.status.toLowerCase()">
                  {{ formatStatus(asset.status) }}
                </span>
                <button 
                  class="btn-card-quick-delete" 
                  @click.stop="deleteAsset(asset.id, asset.title)" 
                  title="Delete take"
                >
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                  </svg>
                </button>
              </div>
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
                  class="btn-tile-reload" 
                  @click="triggerTileReload(asset.id)"
                  title="Reload or attach video footage file for this take"
                >
                  <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                    <polyline points="17 8 12 3 7 8"></polyline>
                    <line x1="12" y1="3" x2="12" y2="15"></line>
                  </svg>
                  <span>Reload</span>
                </button>
                <button 
                  class="btn-tile-delete" 
                  @click.stop="deleteAsset(asset.id, asset.title)" 
                  title="Delete take"
                >
                  <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                  </svg>
                  <span>Delete</span>
                </button>
              </div>
            </div>
          </div>

          <div v-if="filteredAssets.length === 0" class="empty-bin-state">
            <p>No takes match your search filter.</p>
          </div>
        </div>
      </aside>

      <!-- Center & Right: Real Cinema Player & Notes -->
      <section class="main-stage">
        <div v-if="selectedAsset" class="stage-content">
          
          <!-- Real Cinema Player Canvas -->
          <div class="cinema-player-card">
            <!-- Player Top Status Header -->
            <div class="player-header-bar">
              <div class="take-ident">
                <span class="take-accent-dot" :class="{ 'live': isPlaying }"></span>
                <span class="take-ident-title">{{ selectedAsset.title }}</span>
                <span class="take-ident-sub">{{ selectedAsset.sceneId }} • Take {{ selectedAsset.takeNumber }}</span>
              </div>
              <div class="header-video-source">
                <span class="source-tag" :title="currentVideoSource">{{ currentVideoSourceLabel }}</span>
                <button class="btn-attach-video" @click="triggerQuickLocalUpload" title="Load video from Laptop or External Drive">
                  <span>📂 Switch Video</span>
                </button>
                <div class="smpte-pill">{{ formattedCurrentTimecode }}</div>
              </div>
            </div>

            <!-- Real HTML5 Video Player Viewport -->
            <div 
              class="viewport-box"
              :class="{ 'drag-over': isDraggingOver }"
              @dragover.prevent="isDraggingOver = true"
              @dragleave.prevent="isDraggingOver = false"
              @drop.prevent="handleDropVideo"
              @click="togglePlay"
            >
              <!-- The Real HTML5 Video Player Element -->
              <video 
                ref="videoPlayerRef"
                class="html5-video-canvas"
                :src="currentVideoSource"
                playsinline
                preload="metadata"
                @timeupdate="onVideoTimeUpdate"
                @loadedmetadata="onVideoLoadedMetadata"
                @play="isPlaying = true"
                @pause="isPlaying = false"
                @ended="isPlaying = false"
              ></video>

              <!-- Missing Local Footage / Reload Prompt Overlay -->
              <div 
                v-if="isLocalSourceMissing" 
                class="local-missing-overlay"
                @click.stop
              >
                <div class="local-missing-card">
                  <div class="missing-badge">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                      <polyline points="17 8 12 3 7 8"></polyline>
                      <line x1="12" y1="3" x2="12" y2="15"></line>
                    </svg>
                    <span>LOCAL FOOTAGE REFERENCE</span>
                  </div>
                  <h3 class="missing-filename">{{ localMissingFileName }}</h3>
                  <p class="missing-desc">
                    This take is linked to a video file on your laptop or external drive. 
                    Click below or drop the video file into this viewport to restore instant playback.
                  </p>
                  <div class="missing-actions">
                    <button class="btn-missing-reload" @click="triggerQuickLocalUpload">
                      <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                        <polyline points="17 8 12 3 7 8"></polyline>
                        <line x1="12" y1="3" x2="12" y2="15"></line>
                      </svg>
                      <span>Reload Video File</span>
                    </button>
                    <button class="btn-missing-switch-preset" @click="openUploadModal">
                      <span>+ Switch / Upload New Cut</span>
                    </button>
                  </div>
                </div>
              </div>

              <!-- Drag-and-drop indicator overlay -->
              <div v-if="isDraggingOver" class="drag-drop-overlay">
                <div class="drag-drop-content">
                  <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#f4c524" stroke-width="2">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                    <polyline points="17 8 12 3 7 8"></polyline>
                    <line x1="12" y1="3" x2="12" y2="15"></line>
                  </svg>
                  <p>Drop Video File to Play Directly from Your Laptop / External Drive</p>
                </div>
              </div>

              <!-- Center Big Play Button Overlay -->
              <transition name="play-fade">
                <div v-if="!isPlaying && !isDraggingOver" class="play-overlay-btn">
                  <svg viewBox="0 0 24 24" width="28" height="28" fill="#f4c524">
                    <polygon points="8,5 19,12 8,19" />
                  </svg>
                </div>
              </transition>

              <!-- Burned Screenplay Subtitle Floating Bar -->
              <div 
                v-if="selectedAsset.scriptDialogue" 
                class="screenplay-subtitle-capsule" 
                :class="{ 'is-burned': selectedAsset.processedS3Url }"
                @click.stop
              >
                <span class="subtitle-badge">
                  {{ selectedAsset.processedS3Url ? '⚡ SUBTITLE BURNED-IN' : 'SCREENPLAY CUE' }}
                </span>
                <p class="subtitle-line">
                  "{{ selectedAsset.scriptDialogue }}"
                </p>
              </div>
            </div>

            <!-- Hidden File Input for Quick Local Switch -->
            <input 
              ref="quickFileInputRef"
              type="file"
              accept="video/*"
              style="display: none;"
              @change="handleQuickFileSelect"
            />

            <!-- Hidden File Input for reloading specific tile from disk -->
            <input 
              ref="tileReloadInputRef"
              type="file"
              accept="video/*,.mp4,.mov,.webm,.mkv"
              style="display: none;"
              @change="handleTileReloadSelected"
            />

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
                  step="0.05"
                  :value="currentSeconds"
                  @input="onScrubberSeek" 
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
                title="Click to seek video directly to this frame"
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
                <p>No feedback on this take yet. Play the video, pause at any frame, and post a note above.</p>
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
          <p>Choose any film take from the left bin to watch the real video stream, verify subtitles, and drop notes.</p>
        </div>
      </section>
    </main>

    <!-- Modal: Ingest New Film Take (With Local File & Cloud Source Tabs!) -->
    <div v-if="showUploadModal" class="modal-backdrop" @click.self="showUploadModal = false">
      <div class="modal-card">
        <div class="modal-card-top">
          <h3>Ingest New Film Take</h3>
          <button class="btn-modal-close" @click="showUploadModal = false">✕</button>
        </div>

        <!-- Source Selector Tabs -->
        <div class="modal-source-tabs">
          <button 
            type="button" 
            class="source-tab" 
            :class="{ 'active': uploadSourceType === 'LOCAL' }"
            @click="uploadSourceType = 'LOCAL'"
          >
            💻 Laptop / External Drive
          </button>
          <button 
            type="button" 
            class="source-tab" 
            :class="{ 'active': uploadSourceType === 'CLOUD' }"
            @click="uploadSourceType = 'CLOUD'"
          >
            ☁️ Cloud / Google Drive / S3
          </button>
          <button 
            type="button" 
            class="source-tab" 
            :class="{ 'active': uploadSourceType === 'SAMPLE' }"
            @click="uploadSourceType = 'SAMPLE'"
          >
            🎞️ Sample Film Clips
          </button>
        </div>

        <form @submit.prevent="submitNewClip">
          <!-- Option 1: Local Laptop / Hard Disk Video File -->
          <div v-if="uploadSourceType === 'LOCAL'" class="upload-dropzone-box">
            <input 
              ref="modalFileInputRef"
              type="file" 
              accept="video/*" 
              id="local-video-file-input"
              class="hidden-file-input"
              @change="onModalFileSelected"
            />
            <label for="local-video-file-input" class="dropzone-label">
              <svg viewBox="0 0 24 24" width="36" height="36" fill="none" stroke="#f4c524" stroke-width="2">
                <rect x="2" y="2" width="20" height="20" rx="4"></rect>
                <path d="m10 9 5 3-5 3z"></path>
              </svg>
              <div v-if="!selectedLocalFileName" class="dropzone-text">
                <span class="highlight-text">Click to browse or Drag & Drop</span>
                <span class="sub-text">Any video format (.mp4, .mov, .webm, .mkv) • Any file size from Laptop or SSD</span>
              </div>
              <div v-else class="selected-file-info">
                <span class="file-name">✅ Loaded: {{ selectedLocalFileName }}</span>
                <span class="file-size">{{ selectedLocalFileSize }}</span>
              </div>
            </label>
          </div>

          <!-- Option 2: Cloud / Google Drive / S3 URL -->
          <div v-else-if="uploadSourceType === 'CLOUD'" class="form-group">
            <label>Video Stream URL (Google Drive, AWS S3, CloudFront, or Direct MP4)</label>
            <input 
              v-model="newClip.s3BucketUrl" 
              type="text" 
              placeholder="e.g. https://drive.google.com/file/d/... or https://bucket.s3.amazonaws.com/take.mp4" 
              required
            />
            <p class="field-hint">Google Drive sharing links and direct video URLs stream directly into the cinema player.</p>
          </div>

          <!-- Option 3: Instant Preset Film Clips -->
          <div v-else class="sample-presets-grid">
            <button 
              type="button" 
              v-for="sample in samplePresets" 
              :key="sample.url"
              class="sample-preset-btn"
              :class="{ 'active': newClip.s3BucketUrl === sample.url }"
              @click="applySamplePreset(sample)"
            >
              <span class="sample-title">{{ sample.title }}</span>
              <span class="sample-desc">{{ sample.desc }}</span>
            </button>
          </div>

          <!-- Metadata Fields -->
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
            <input v-model="newClip.title" type="text" placeholder="e.g. Rooftop Confrontation Master" required />
          </div>

          <div class="form-group">
            <label>Script Screenplay Dialogue (for Subtitles)</label>
            <textarea 
              v-model="newClip.scriptDialogue" 
              rows="2" 
              placeholder='e.g. DETECTIVE: "Where were you the night of the incident?"'
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
              <input v-model.number="newClip.durationSeconds" type="number" min="1" required />
            </div>
          </div>

          <div class="modal-actions">
            <button type="button" class="btn-cancel" @click="showUploadModal = false">Cancel</button>
            <button type="submit" class="btn-confirm" :disabled="submitting">
              {{ submitting ? 'Ingesting...' : 'Ingest & Attach Video' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';

const assets = ref([]);
const selectedAsset = ref(null);
const loading = ref(false);
const submitting = ref(false);
const searchQuery = ref('');
const activeFilter = ref('ALL');
const showUploadModal = ref(false);

// Video player ref and playback state
const videoPlayerRef = ref(null);
const quickFileInputRef = ref(null);
const modalFileInputRef = ref(null);
const tileReloadInputRef = ref(null);
const targetReloadAssetId = ref(null);
const pendingLocalFile = ref(null);

const isPlaying = ref(false);
const currentSeconds = ref(0);
const isDraggingOver = ref(false);

// Local memory video blob sources indexed by asset ID
const localVideoSources = ref({});
const localPendingBlobUrl = ref('');
const selectedLocalFileName = ref('');
const selectedLocalFileSize = ref('');

// ==========================================
// IndexedDB Persistent Local Video Storage
// Stores raw File/Blob instances across browser reloads
// ==========================================
const DB_NAME = 'collabcut_media_db';
const DB_VERSION = 1;
const STORE_NAME = 'videos';

function openVideoDatabase() {
  return new Promise((resolve) => {
    if (typeof window === 'undefined' || !window.indexedDB) {
      resolve(null);
      return;
    }
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = (event) => {
      const db = event.target.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'assetId' });
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = (e) => {
      console.warn('IndexedDB open error:', e);
      resolve(null);
    };
  });
}

async function saveVideoToIndexedDB(assetId, file) {
  try {
    const db = await openVideoDatabase();
    if (!db) return;
    return new Promise((resolve) => {
      const tx = db.transaction(STORE_NAME, 'readwrite');
      const store = tx.objectStore(STORE_NAME);
      store.put({
        assetId: String(assetId),
        file: file,
        name: file.name,
        size: file.size,
        type: file.type,
        updatedAt: Date.now()
      });
      tx.oncomplete = () => resolve(true);
      tx.onerror = (err) => {
        console.warn('IndexedDB put error:', err);
        resolve(false);
      };
    });
  } catch (err) {
    console.warn('saveVideoToIndexedDB exception:', err);
  }
}

async function removeVideoFromIndexedDB(assetId) {
  try {
    const db = await openVideoDatabase();
    if (!db) return;
    return new Promise((resolve) => {
      const tx = db.transaction(STORE_NAME, 'readwrite');
      const store = tx.objectStore(STORE_NAME);
      store.delete(String(assetId));
      tx.oncomplete = () => resolve(true);
      tx.onerror = () => resolve(false);
    });
  } catch (err) {
    console.warn('removeVideoFromIndexedDB exception:', err);
  }
}

async function loadAllStoredVideos() {
  try {
    const db = await openVideoDatabase();
    if (!db) return;
    return new Promise((resolve) => {
      const tx = db.transaction(STORE_NAME, 'readonly');
      const store = tx.objectStore(STORE_NAME);
      const req = store.getAll();
      req.onsuccess = () => {
        const records = req.result || [];
        records.forEach(rec => {
          if (rec.file && rec.assetId) {
            try {
              const blobUrl = URL.createObjectURL(rec.file);
              localVideoSources.value[rec.assetId] = blobUrl;
              localVideoSources.value[Number(rec.assetId)] = blobUrl;
            } catch (err) {
              console.warn('Error creating object URL from stored file:', err);
            }
          }
        });
        resolve();
      };
      req.onerror = () => resolve();
    });
  } catch (err) {
    console.warn('loadAllStoredVideos exception:', err);
  }
}

// Ingest source tab
const uploadSourceType = ref('LOCAL'); // 'LOCAL' | 'CLOUD' | 'SAMPLE'

// Sample film presets
const samplePresets = [
  {
    title: 'Tears of Steel (Sci-Fi Action)',
    desc: '4K open-movie dialogue take with robotic VFX',
    url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4',
    sceneId: 'Scene 09',
    takeNumber: 1,
    scriptDialogue: 'CELIA: "What happened to you? Why did you join the machine coalition?"',
    duration: 180
  },
  {
    title: 'Sintel (Dragon Hunt Confrontation)',
    desc: 'Cinematic animated master with orchestral score',
    url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4',
    sceneId: 'Scene 21B',
    takeNumber: 3,
    scriptDialogue: 'SINTEL: "I looked across the snowy peak... and I knew he was waiting."',
    duration: 120
  },
  {
    title: 'Big Buck Bunny (Forest Master)',
    desc: 'Daylight 60fps wide angle landscape cut',
    url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
    sceneId: 'Scene 02',
    takeNumber: 4,
    scriptDialogue: 'NARRATOR: "The morning broke silently across the meadow."',
    duration: 150
  }
];

// Fallback high-speed CDN videos for seeded assets
const defaultCdnVideos = [
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4',
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4',
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4'
];

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
  durationSeconds: 120,
  s3BucketUrl: ''
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
  const s = Math.floor(sec % 60).toString().padStart(2, '0');
  const frames = Math.floor((sec % 1) * 24).toString().padStart(2, '0');
  return `${hrs}:${mins}:${s}:${frames}`;
});

// Check if selected asset has a local file reference that needs re-attaching
const isLocalSourceMissing = computed(() => {
  if (!selectedAsset.value) return false;
  const s3 = selectedAsset.value.s3BucketUrl || '';
  if (s3.startsWith('local://')) {
    const id = selectedAsset.value.id;
    return !localVideoSources.value[id] && !localVideoSources.value[String(id)] && !localVideoSources.value[Number(id)];
  }
  return false;
});

const localMissingFileName = computed(() => {
  if (!selectedAsset.value) return '';
  const s3 = selectedAsset.value.s3BucketUrl || '';
  if (s3.startsWith('local://')) {
    return s3.replace('local://', '');
  }
  return selectedAsset.value.title;
});

// Active Video Stream Source Resolution
const currentVideoSource = computed(() => {
  if (!selectedAsset.value) return '';
  const id = selectedAsset.value.id;
  
  // 1. Check if user attached a local video file (Blob URL from memory or IndexedDB)
  if (localVideoSources.value[id]) {
    return localVideoSources.value[id];
  }
  if (localVideoSources.value[String(id)]) {
    return localVideoSources.value[String(id)];
  }
  if (localVideoSources.value[Number(id)]) {
    return localVideoSources.value[Number(id)];
  }

  // 2. Check if asset's s3BucketUrl is a direct HTTP/HTTPS/Blob URL
  const rawUrl = selectedAsset.value.s3BucketUrl || '';
  if (rawUrl.startsWith('http://') || rawUrl.startsWith('https://') || rawUrl.startsWith('blob:')) {
    // If Google Drive link, convert to streamable URL
    if (rawUrl.includes('drive.google.com')) {
      const match = rawUrl.match(/\/d\/([a-zA-Z0-9_-]+)/);
      if (match && match[1]) {
        return `https://drive.google.com/uc?export=download&id=${match[1]}`;
      }
    }
    return rawUrl;
  }

  // 3. Fallback to reliable sample film video only for cloud/demo assets, NOT for local:// references
  if (!rawUrl.startsWith('local://')) {
    const idx = (typeof id === 'number' ? id : 1) % defaultCdnVideos.length;
    return defaultCdnVideos[idx];
  }

  return '';
});

const currentVideoSourceLabel = computed(() => {
  if (!selectedAsset.value) return '';
  const id = selectedAsset.value.id;
  if (localVideoSources.value[id] || localVideoSources.value[String(id)] || localVideoSources.value[Number(id)]) {
    return '💻 Local Video';
  }
  const raw = selectedAsset.value.s3BucketUrl || '';
  if (raw.startsWith('local://')) return '📂 Local Video (Needs Reload)';
  if (raw.includes('drive.google.com')) return '☁️ Google Drive';
  if (raw.startsWith('http')) return '🌐 Cloud Stream';
  return '🎞️ Studio Reel';
});

// Real Video Control: Play / Pause
function togglePlay() {
  if (!videoPlayerRef.value) return;
  if (videoPlayerRef.value.paused) {
    videoPlayerRef.value.play().then(() => {
      isPlaying.value = true;
    }).catch(err => {
      console.warn('Playback error:', err);
    });
  } else {
    videoPlayerRef.value.pause();
    isPlaying.value = false;
  }
}

// Video time update event from browser HTML5 engine
function onVideoTimeUpdate() {
  if (!videoPlayerRef.value) return;
  currentSeconds.value = videoPlayerRef.value.currentTime;
}

// Video metadata loaded
function onVideoLoadedMetadata() {
  if (!videoPlayerRef.value || !selectedAsset.value) return;
  const dur = Math.round(videoPlayerRef.value.duration);
  if (dur && dur > 0 && dur !== Infinity) {
    selectedAsset.value.durationSeconds = dur;
  }
}

// Scrubber seek
function onScrubberSeek(e) {
  const target = parseFloat(e.target.value);
  currentSeconds.value = target;
  if (videoPlayerRef.value) {
    videoPlayerRef.value.currentTime = target;
  }
}

// Seek video to exact second
function jumpToTimecode(sec) {
  currentSeconds.value = sec;
  if (videoPlayerRef.value) {
    videoPlayerRef.value.currentTime = sec;
    if (videoPlayerRef.value.paused) {
      videoPlayerRef.value.play().catch(() => {});
    }
  }
  showToast(`Seeked video to ${formattedCurrentTimecode.value}`);
}

function calcMarkerLeft(timestampSec) {
  const max = (selectedAsset.value && selectedAsset.value.durationSeconds) || 150;
  const pct = Math.min(100, Math.max(0, (timestampSec / max) * 100));
  return `${pct}%`;
}

// Direct Drag-and-Drop video onto Cinema Viewport
async function handleDropVideo(e) {
  isDraggingOver.value = false;
  if (!e.dataTransfer || !e.dataTransfer.files || !e.dataTransfer.files.length) return;
  const file = e.dataTransfer.files[0];
  if (!file.type.startsWith('video/') && !file.name.match(/\.(mp4|mov|webm|mkv|avi)$/i)) {
    showToast('Please drop a valid video file (.mp4, .mov, .webm, .mkv)', 'error');
    return;
  }

  if (!selectedAsset.value) {
    showToast('Select a take first or click + New Take to ingest', 'error');
    return;
  }

  await attachLocalFileToAsset(selectedAsset.value.id, file);
}

// Quick Local Video Switch button in header
function triggerQuickLocalUpload() {
  if (quickFileInputRef.value) {
    quickFileInputRef.value.value = '';
    quickFileInputRef.value.click();
  }
}

async function handleQuickFileSelect(e) {
  if (!e.target.files || !e.target.files.length) return;
  const file = e.target.files[0];
  if (selectedAsset.value) {
    await attachLocalFileToAsset(selectedAsset.value.id, file);
  }
}

// Tile-specific Reload button
function triggerTileReload(assetId) {
  targetReloadAssetId.value = assetId;
  if (tileReloadInputRef.value) {
    tileReloadInputRef.value.value = '';
    tileReloadInputRef.value.click();
  }
}

async function handleTileReloadSelected(e) {
  if (!e.target.files || !e.target.files.length) return;
  const file = e.target.files[0];
  const assetId = targetReloadAssetId.value;
  if (!assetId) return;

  await attachLocalFileToAsset(assetId, file);

  // Automatically select this asset so user immediately sees their reloaded video
  const target = assets.value.find(a => a.id === assetId || a.id === Number(assetId));
  if (target) {
    selectedAsset.value = target;
  }
}

// Attach local video file to take (saves to IndexedDB & generates blob URL)
async function attachLocalFileToAsset(assetId, file) {
  await saveVideoToIndexedDB(assetId, file);
  const blobUrl = URL.createObjectURL(file);
  localVideoSources.value[assetId] = blobUrl;
  localVideoSources.value[Number(assetId)] = blobUrl;
  localVideoSources.value[String(assetId)] = blobUrl;
  
  // Auto-detect duration
  const tempV = document.createElement('video');
  tempV.preload = 'metadata';
  tempV.src = blobUrl;
  tempV.onloadedmetadata = () => {
    if (selectedAsset.value && (selectedAsset.value.id === assetId || selectedAsset.value.id === Number(assetId))) {
      selectedAsset.value.durationSeconds = Math.round(tempV.duration) || selectedAsset.value.durationSeconds;
    }
  };

  showToast(`Loaded "${file.name}" (Persisted across reloads)`, 'success');
  nextTick(() => {
    if (videoPlayerRef.value) {
      videoPlayerRef.value.load();
      videoPlayerRef.value.play().catch(() => {});
    }
  });
}

// Modal Local Video Selection
function onModalFileSelected(e) {
  if (!e.target.files || !e.target.files.length) return;
  const file = e.target.files[0];
  pendingLocalFile.value = file;
  selectedLocalFileName.value = file.name;
  
  const sizeMb = (file.size / (1024 * 1024)).toFixed(1);
  selectedLocalFileSize.value = `${sizeMb} MB`;

  // Auto populate title and scene if empty
  if (!newClip.value.title) {
    newClip.value.title = file.name.replace(/\.[^/.]+$/, '').replace(/[_-]/g, ' ');
  }
  if (!newClip.value.sceneId) {
    newClip.value.sceneId = 'Scene 01';
  }

  // Create local blob URL for preview
  localPendingBlobUrl.value = URL.createObjectURL(file);

  // Read duration
  const tempV = document.createElement('video');
  tempV.preload = 'metadata';
  tempV.src = localPendingBlobUrl.value;
  tempV.onloadedmetadata = () => {
    newClip.value.durationSeconds = Math.round(tempV.duration) || 120;
  };
}

function applySamplePreset(sample) {
  pendingLocalFile.value = null;
  selectedLocalFileName.value = '';
  selectedLocalFileSize.value = '';
  localPendingBlobUrl.value = '';
  newClip.value.title = sample.title;
  newClip.value.sceneId = sample.sceneId;
  newClip.value.takeNumber = sample.takeNumber;
  newClip.value.scriptDialogue = sample.scriptDialogue;
  newClip.value.durationSeconds = sample.duration;
  newClip.value.s3BucketUrl = sample.url;
  showToast(`Loaded preset: ${sample.title}`);
}

function openUploadModal() {
  pendingLocalFile.value = null;
  selectedLocalFileName.value = '';
  selectedLocalFileSize.value = '';
  localPendingBlobUrl.value = '';
  showUploadModal.value = true;
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
  isPlaying.value = false;
  nextTick(() => {
    if (videoPlayerRef.value) {
      videoPlayerRef.value.currentTime = 0;
      videoPlayerRef.value.load();
    }
  });
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
      timestamp_seconds: Math.floor(currentSeconds.value),
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
    // If user selected local file, generate s3-compatible file path for DB
    let s3Url = newClip.value.s3BucketUrl;
    if (uploadSourceType.value === 'LOCAL' && selectedLocalFileName.value) {
      s3Url = `local://${selectedLocalFileName.value}`;
    } else if (!s3Url) {
      s3Url = samplePresets[0].url;
    }

    const res = await fetch('/api/assets', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ...newClip.value,
        s3BucketUrl: s3Url
      })
    });

    if (!res.ok) throw new Error(`Failed to create take`);
    const created = await res.json();

    // If local file was loaded, save to IndexedDB and associate the blob URL with the newly created asset ID
    if (pendingLocalFile.value) {
      await saveVideoToIndexedDB(created.id, pendingLocalFile.value);
      const blobUrl = URL.createObjectURL(pendingLocalFile.value);
      localVideoSources.value[created.id] = blobUrl;
      localVideoSources.value[Number(created.id)] = blobUrl;
      localVideoSources.value[String(created.id)] = blobUrl;
      pendingLocalFile.value = null;
    } else if (localPendingBlobUrl.value) {
      localVideoSources.value[created.id] = localPendingBlobUrl.value;
      localVideoSources.value[Number(created.id)] = localPendingBlobUrl.value;
      localVideoSources.value[String(created.id)] = localPendingBlobUrl.value;
    }

    showToast(`Added ${created.sceneId} with video attached!`, 'success');
    showUploadModal.value = false;
    newClip.value = { sceneId: '', takeNumber: 1, title: '', scriptDialogue: '', cameraRoll: 'Roll A-01', durationSeconds: 120, s3BucketUrl: '' };
    await fetchAll();
    selectedAsset.value = created;

    nextTick(() => {
      if (videoPlayerRef.value) {
        videoPlayerRef.value.load();
        videoPlayerRef.value.play().catch(() => {});
      }
    });
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    submitting.value = false;
  }
}

async function deleteAsset(id, title) {
  const targetId = Number(id);
  const previousAssets = [...assets.value];
  
  // 1. Optimistic removal: remove immediately from UI in 0ms!
  assets.value = assets.value.filter(a => a.id !== id && a.id !== targetId);

  // 2. If the deleted asset was currently selected, switch to first remaining take
  if (selectedAsset.value && (selectedAsset.value.id === id || selectedAsset.value.id === targetId)) {
    selectedAsset.value = assets.value.length > 0 ? assets.value[0] : null;
  }

  showToast(`Deleted take "${title}"`, 'success');

  // 3. Clean up IndexedDB and local blob URLs
  try {
    await removeVideoFromIndexedDB(id);
  } catch (e) {
    console.warn('Error removing from IndexedDB:', e);
  }
  delete localVideoSources.value[id];
  delete localVideoSources.value[targetId];
  delete localVideoSources.value[String(id)];

  // 4. Send asynchronous delete request to PostgreSQL backend
  try {
    const res = await fetch(`/api/assets/${id}`, { method: 'DELETE' });
    if (!res.ok && res.status !== 404) {
      throw new Error(`Server returned HTTP ${res.status}`);
    }
  } catch (err) {
    console.error('Delete take failed on server:', err);
    // Rollback if server failed
    assets.value = previousAssets;
    showToast(`Could not delete take: ${err.message}`, 'error');
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
  const s = Math.floor(sec % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
}

onMounted(async () => {
  await loadAllStoredVideos();
  await fetchAll();
});

onUnmounted(() => {
  if (videoPlayerRef.value) {
    videoPlayerRef.value.pause();
  }
});
</script>

<style scoped>
/* Ultra-Sleek Modern Studio in #f4c524 and Black */

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

.take-top-right {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.btn-card-quick-delete {
  background: transparent;
  border: none;
  color: #52525b;
  cursor: pointer;
  padding: 0.15rem 0.25rem;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}
.btn-card-quick-delete:hover {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.take-actions {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.btn-tile-reload {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  background: #18181b;
  border: 1px solid #27272a;
  color: #f4c524;
  padding: 0.25rem 0.55rem;
  border-radius: 5px;
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-tile-reload:hover {
  background: #f4c524;
  color: #000000;
  border-color: #f4c524;
}

.btn-tile-delete {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  background: #18181b;
  border: 1px solid #27272a;
  color: #a1a1aa;
  padding: 0.25rem 0.55rem;
  border-radius: 5px;
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-tile-delete:hover {
  background: rgba(239, 68, 68, 0.15);
  border-color: #ef4444;
  color: #ef4444;
}

/* Local Video Missing / Reload Prompt Overlay */
.local-missing-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.88);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 25;
  padding: 1.5rem;
}

.local-missing-card {
  background: #0d0d0d;
  border: 1px solid #27272a;
  border-radius: 12px;
  padding: 2rem 2.5rem;
  max-width: 520px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.9);
}

.missing-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.3rem 0.75rem;
  background: rgba(244, 197, 36, 0.12);
  border: 1px solid rgba(244, 197, 36, 0.4);
  color: #f4c524;
  border-radius: 20px;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.missing-filename {
  font-size: 1.15rem;
  font-weight: 700;
  color: #ffffff;
  word-break: break-all;
  line-height: 1.35;
  margin: 0;
}

.missing-desc {
  font-size: 0.85rem;
  color: #a1a1aa;
  line-height: 1.5;
  margin: 0;
}

.missing-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 0.5rem;
  flex-wrap: wrap;
  justify-content: center;
}

.btn-missing-reload {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: #f4c524;
  color: #000000;
  border: none;
  border-radius: 8px;
  padding: 0.65rem 1.25rem;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-missing-reload:hover {
  background: #ffdb4d;
  transform: translateY(-1px);
}

.btn-missing-switch-preset {
  background: #18181b;
  border: 1px solid #27272a;
  color: #ffffff;
  border-radius: 8px;
  padding: 0.65rem 1.25rem;
  font-weight: 600;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-missing-switch-preset:hover {
  border-color: #52525b;
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
  padding: 0.85rem 1.25rem;
  background: #0c0c0e;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-wrap: wrap;
  gap: 0.75rem;
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

.header-video-source {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.source-tag {
  font-size: 0.75rem;
  color: #a1a1aa;
  background: #000000;
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 0.25rem 0.6rem;
  border-radius: 6px;
  font-family: 'JetBrains Mono', monospace;
}

.btn-attach-video {
  background: transparent;
  border: 1px solid rgba(244, 197, 36, 0.3);
  color: #f4c524;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.25rem 0.65rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-attach-video:hover {
  background: rgba(244, 197, 36, 0.12);
  border-color: #f4c524;
}

.smpte-pill {
  background: #000000;
  border: 1px solid rgba(244, 197, 36, 0.35);
  color: #f4c524;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.95rem;
  font-weight: 800;
  padding: 0.25rem 0.75rem;
  border-radius: 6px;
  letter-spacing: 0.05em;
  text-shadow: 0 0 10px rgba(244, 197, 36, 0.3);
}

/* Real HTML5 Video Viewport Box */
.viewport-box {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  max-height: 480px;
  background: #000000;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: center;
  padding: 1.5rem;
  cursor: pointer;
  overflow: hidden;
}

.html5-video-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000000;
}

.viewport-box.drag-over {
  border: 2px dashed #f4c524;
}

.drag-drop-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.88);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  backdrop-filter: blur(4px);
}

.drag-drop-content {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  color: #f4c524;
  font-weight: 700;
  font-size: 0.95rem;
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
  z-index: 4;
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
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  padding: 0.75rem 1.25rem;
  text-align: center;
  z-index: 3;
  transition: all 0.2s ease;
}
.screenplay-subtitle-capsule.is-burned {
  border-color: #f4c524;
  box-shadow: 0 0 25px rgba(244, 197, 36, 0.25);
}

.subtitle-badge {
  display: block;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.65rem;
  font-weight: 800;
  color: #f4c524;
  letter-spacing: 0.08em;
  margin-bottom: 0.25rem;
}

.subtitle-line {
  font-size: 1.05rem;
  font-weight: 700;
  color: #f4c524;
  line-height: 1.4;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.9);
}

/* Player Transport & Scrubber Deck */
.player-transport-deck {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  padding: 0.85rem 1.5rem;
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

/* Modal: Source Selector & Modern Card */
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
  max-width: 540px;
  padding: 1.75rem;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.95), 0 0 25px rgba(244, 197, 36, 0.15);
  max-height: 90vh;
  overflow-y: auto;
}

.modal-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
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

/* Modal Source Tabs */
.modal-source-tabs {
  display: flex;
  gap: 0.5rem;
  background: #000000;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 0.35rem;
  margin-bottom: 1.25rem;
}

.source-tab {
  flex: 1;
  background: transparent;
  border: none;
  color: #71717a;
  padding: 0.45rem 0.5rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
  text-align: center;
}
.source-tab:hover {
  color: #ffffff;
}
.source-tab.active {
  background: #18181b;
  color: #f4c524;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.5);
}

/* Upload Dropzone Box */
.upload-dropzone-box {
  margin-bottom: 1.25rem;
}

.hidden-file-input {
  display: none;
}

.dropzone-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  background: #000000;
  border: 2px dashed rgba(244, 197, 36, 0.4);
  border-radius: 10px;
  padding: 1.75rem 1.25rem;
  cursor: pointer;
  text-align: center;
  transition: all 0.15s ease;
}
.dropzone-label:hover {
  border-color: #f4c524;
  background: rgba(244, 197, 36, 0.03);
}

.dropzone-text {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.highlight-text {
  font-weight: 700;
  font-size: 0.95rem;
  color: #ffffff;
}
.sub-text {
  font-size: 0.75rem;
  color: #71717a;
}

.selected-file-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.file-name {
  color: #f4c524;
  font-weight: 700;
  font-size: 0.95rem;
}
.file-size {
  color: #71717a;
  font-size: 0.8rem;
}

.field-hint {
  font-size: 0.75rem;
  color: #71717a;
  margin-top: 0.35rem;
}

/* Sample Presets Grid */
.sample-presets-grid {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  margin-bottom: 1.25rem;
}

.sample-preset-btn {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.2rem;
  background: #000000;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 0.65rem 0.85rem;
  cursor: pointer;
  text-align: left;
  transition: all 0.15s ease;
}
.sample-preset-btn:hover {
  background: #111114;
  border-color: rgba(244, 197, 36, 0.3);
}
.sample-preset-btn.active {
  border-color: #f4c524;
  background: rgba(244, 197, 36, 0.08);
}

.sample-title {
  font-size: 0.85rem;
  font-weight: 700;
  color: #ffffff;
}
.sample-desc {
  font-size: 0.75rem;
  color: #71717a;
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
  margin-top: 1.5rem;
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
