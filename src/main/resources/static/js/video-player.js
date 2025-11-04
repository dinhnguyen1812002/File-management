// Custom Video Player Component for Alpine.js
function videoPlayer() {
    return {
        // Video state
        isPlaying: false,
        isMuted: false,
        volume: 100,
        currentTime: 0,
        duration: 0,
        progressPercent: 0,
        bufferedPercent: 0,
        videoLoading: true,
        videoError: false,
        videoErrorMessage: '',
        videoSize: '',

        // UI state
        showVideoControls: false,
        showPlayButton: false,
        controlsTimeout: null,

        // Video element reference
        videoElement: null,

        // Initialize video player
        init() {
            this.$nextTick(() => {
                this.videoElement = this.$refs.videoElement;
                if (this.videoElement) {
                    this.setupVideoEvents();
                }
            });
        },

        // Setup video event listeners
        setupVideoEvents() {
            if (!this.videoElement) return;

            // Keyboard controls
            document.addEventListener('keydown', (e) => {
                if (!this.showVideoControls) return;
                
                switch(e.code) {
                    case 'Space':
                        e.preventDefault();
                        this.togglePlayPause();
                        break;
                    case 'ArrowLeft':
                        e.preventDefault();
                        this.seek(-10);
                        break;
                    case 'ArrowRight':
                        e.preventDefault();
                        this.seek(10);
                        break;
                    case 'ArrowUp':
                        e.preventDefault();
                        this.changeVolume(10);
                        break;
                    case 'ArrowDown':
                        e.preventDefault();
                        this.changeVolume(-10);
                        break;
                    case 'KeyM':
                        e.preventDefault();
                        this.toggleMute();
                        break;
                    case 'KeyF':
                        e.preventDefault();
                        this.toggleFullscreen();
                        break;
                }
            });
        },

        // Video event handlers
        onVideoLoaded() {
            this.duration = this.videoElement.duration;
            this.videoSize = this.getVideoSize();
            console.log('Video metadata loaded');
        },

        onVideoReady() {
            this.videoLoading = false;
            this.videoError = false;
            console.log('Video ready to play');
        },

        onVideoError(event) {
            this.videoLoading = false;
            this.videoError = true;
            
            const error = this.videoElement.error;
            if (error) {
                switch(error.code) {
                    case error.MEDIA_ERR_ABORTED:
                        this.videoErrorMessage = 'Video loading was aborted';
                        break;
                    case error.MEDIA_ERR_NETWORK:
                        this.videoErrorMessage = 'Network error occurred';
                        break;
                    case error.MEDIA_ERR_DECODE:
                        this.videoErrorMessage = 'Video decoding error';
                        break;
                    case error.MEDIA_ERR_SRC_NOT_SUPPORTED:
                        this.videoErrorMessage = 'Video format not supported';
                        break;
                    default:
                        this.videoErrorMessage = 'Unknown video error';
                }
            } else {
                this.videoErrorMessage = 'Failed to load video';
            }
            
            console.error('Video error:', this.videoErrorMessage);
        },

        onTimeUpdate() {
            if (this.videoElement) {
                this.currentTime = this.videoElement.currentTime;
                this.progressPercent = (this.currentTime / this.duration) * 100;
            }
        },

        onVideoEnded() {
            this.isPlaying = false;
            this.showPlayButton = true;
            setTimeout(() => {
                this.showPlayButton = false;
            }, 2000);
        },

        onVideoWaiting() {
            this.videoLoading = true;
        },

        onVideoCanPlay() {
            this.videoLoading = false;
        },

        onVideoProgress() {
            if (this.videoElement && this.videoElement.buffered.length > 0) {
                const bufferedEnd = this.videoElement.buffered.end(this.videoElement.buffered.length - 1);
                this.bufferedPercent = (bufferedEnd / this.duration) * 100;
            }
        },

        // Control methods
        togglePlayPause() {
            if (!this.videoElement || this.videoError) return;

            if (this.videoElement.paused) {
                this.play();
            } else {
                this.pause();
            }
        },

        play() {
            if (!this.videoElement) return;
            
            const playPromise = this.videoElement.play();
            if (playPromise !== undefined) {
                playPromise
                    .then(() => {
                        this.isPlaying = true;
                        this.showPlayButton = true;
                        setTimeout(() => {
                            this.showPlayButton = false;
                        }, 1000);
                    })
                    .catch(error => {
                        console.error('Error playing video:', error);
                        this.videoError = true;
                        this.videoErrorMessage = 'Failed to play video';
                    });
            }
        },

        pause() {
            if (!this.videoElement) return;
            
            this.videoElement.pause();
            this.isPlaying = false;
            this.showPlayButton = true;
            setTimeout(() => {
                this.showPlayButton = false;
            }, 1000);
        },

        toggleMute() {
            if (!this.videoElement) return;
            
            this.videoElement.muted = !this.videoElement.muted;
            this.isMuted = this.videoElement.muted;
        },

        setVolume(value) {
            if (!this.videoElement) return;
            
            this.volume = value;
            this.videoElement.volume = value / 100;
            this.isMuted = value == 0;
        },

        changeVolume(delta) {
            const newVolume = Math.max(0, Math.min(100, this.volume + delta));
            this.setVolume(newVolume);
        },

        seekTo(event) {
            if (!this.videoElement || !this.duration) return;
            
            const rect = event.currentTarget.getBoundingClientRect();
            const percent = (event.clientX - rect.left) / rect.width;
            const newTime = percent * this.duration;
            
            this.videoElement.currentTime = newTime;
        },

        seek(seconds) {
            if (!this.videoElement) return;
            
            const newTime = Math.max(0, Math.min(this.duration, this.currentTime + seconds));
            this.videoElement.currentTime = newTime;
        },

        toggleFullscreen() {
            const container = this.videoElement.parentElement;
            
            if (!document.fullscreenElement) {
                if (container.requestFullscreen) {
                    container.requestFullscreen();
                } else if (container.webkitRequestFullscreen) {
                    container.webkitRequestFullscreen();
                } else if (container.msRequestFullscreen) {
                    container.msRequestFullscreen();
                }
            } else {
                if (document.exitFullscreen) {
                    document.exitFullscreen();
                } else if (document.webkitExitFullscreen) {
                    document.webkitExitFullscreen();
                } else if (document.msExitFullscreen) {
                    document.msExitFullscreen();
                }
            }
        },

        // UI control methods
        showControls() {
            this.showVideoControls = true;
            this.clearControlsTimeout();
            
            this.controlsTimeout = setTimeout(() => {
                if (this.isPlaying) {
                    this.showVideoControls = false;
                }
            }, 3000);
        },

        hideControls() {
            this.clearControlsTimeout();
            
            this.controlsTimeout = setTimeout(() => {
                if (this.isPlaying) {
                    this.showVideoControls = false;
                }
            }, 1000);
        },

        clearControlsTimeout() {
            if (this.controlsTimeout) {
                clearTimeout(this.controlsTimeout);
                this.controlsTimeout = null;
            }
        },

        // Utility methods
        formatTime(seconds) {
            if (!seconds || isNaN(seconds)) return '0:00';
            
            const minutes = Math.floor(seconds / 60);
            const remainingSeconds = Math.floor(seconds % 60);
            return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
        },

        getVideoSize() {
            if (!this.videoElement) return '';
            
            const width = this.videoElement.videoWidth;
            const height = this.videoElement.videoHeight;
            
            if (width && height) {
                return `${width}x${height}`;
            }
            return '';
        },

        retryVideo() {
            this.videoError = false;
            this.videoLoading = true;
            this.videoElement.load();
        },

        // Cleanup method
        destroy() {
            this.clearControlsTimeout();
            if (this.videoElement) {
                this.videoElement.pause();
                this.videoElement.src = '';
                this.videoElement.load();
            }
        }
    };
}
