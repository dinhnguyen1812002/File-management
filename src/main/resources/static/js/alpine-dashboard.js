// Alpine.js Dashboard Data and Methods
document.addEventListener('alpine:init', () => {
    Alpine.data('dashboard', () => ({
        // Modal states
        showCreateFolderModal: false,
        showUploadModal: false,
        showShareModel: false,
        showMoveModal: false,
        showPreviewModal: false,
        showPasswordModal: false,
        showPasswordVerificationModal: false,

        // Data objects
        selectedFile: {
            id: null,
            fileName: ''
        },

        // Preview data
        previewData: {
            fileId: null,
            fileName: '',
            fileType: '',
            fileSize: '',
            previewUrl: '',
            previewType: '',
            loading: false,
            error: null
        },

        // Abort controller for canceling requests
        previewAbortController: null,
        selectedFolder: null,
        selectedFiles: [],
        passwordVerificationData: {
            folderId: null,
            folderName: '',
            password: '',
            error: ''
        },
        showPreviewModal: false,
        previewUrl: '',
        previewType: '',


        // Initialize Alpine.js component
        init() {
            // Store reference to this component in global variable
            window.dashboardInstance = this;

            // Watch for upload modal changes
            this.$watch('showUploadModal', (value) => {
                if (value) {
                    setTimeout(() => {
                        this.initializeUploadModal();
                    }, 100);
                } else {
                    this.clearAllFiles();
                }
            });
        },

        // Password verification method
        verifyPassword() {
            const { folderId, password } = this.passwordVerificationData;

            // if (!password.trim()) {
            //     this.passwordVerificationData.error = 'Please enter a password';
            //     return;
            // }

            // Create form data
            const formData = new FormData();
            formData.append('folderId', folderId);
            formData.append('password', password);

            // Send request
            fetch('/files/folders/verify-password-ajax', {
                method: 'POST',
                body: formData
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    // Password correct, redirect to folder
                    window.location.href = data.redirectUrl;
                } else {
                    // Password incorrect, show error
                    this.passwordVerificationData.error = data.error || 'Incorrect password. Please try again.';
                    this.passwordVerificationData.password = '';
                }
            })
            .catch(error => {
                console.error('Error:', error);
                this.passwordVerificationData.error = 'An error occurred. Please try again.';
            });
        },

        // Handle folder click method
        handleFolderClick(folderId, folderName, hasPassword) {
            if (hasPassword) {
                // Show password verification dialog
                this.passwordVerificationData = {
                    folderId: folderId,
                    folderName: folderName,
                    password: '',
                    error: ''
                };
                this.showPasswordVerificationModal = true;
            } else {
                // Navigate to folder normally
                window.location.href = '/files/dashboard?folderId=' + folderId;
            }
        },

        // File management methods
        addFiles(files) {
            Array.from(files).forEach(file => {
                // Check file size (100MB limit)
                if (file.size > 100 * 1024 * 1024) {
                    alert(`File "${file.name}" is too large. Maximum size is 100MB.`);
                    return;
                }

                // Check if file already exists
                if (this.selectedFiles.find(f => f.name === file.name && f.size === file.size)) {
                    return;
                }

                this.selectedFiles.push(file);
            });

            this.updateFilePreview();
            this.updateUploadButton();
        },

        removeFile(index) {
            this.selectedFiles.splice(index, 1);
            this.updateFilePreview();
            this.updateUploadButton();
        },

        clearAllFiles() {
            this.selectedFiles = [];
            this.updateFilePreview();
            this.updateUploadButton();
        },

        updateFilePreview() {
            const filePreview = document.getElementById('filePreview');
            const fileList = document.getElementById('fileList');

            if (this.selectedFiles.length === 0) {
                filePreview.classList.add('hidden');
                return;
            }

            filePreview.classList.remove('hidden');

            fileList.innerHTML = this.selectedFiles.map((file, index) => `
            <div class="file-preview-item bg-white border border-gray-200 rounded-lg p-4 flex items-center space-x-4">
                <div class="flex-shrink-0">
                    <i class="${this.getFileIcon(file.name)} text-2xl"></i>
                </div>
                <div class="flex-1 min-w-0">
                    <div class="flex items-center justify-between">
                        <h5 class="text-sm font-medium text-gray-900 truncate">${file.name}</h5>
                        <button type="button" onclick="removeFileFromAlpine(${index})" class="ml-2 text-red-500 hover:text-red-700 transition-colors">
                            <i class="fas fa-times"></i>
                        </button>
                    </div>
                    <div class="flex items-center space-x-4 mt-1">
                        <span class="text-xs text-gray-500">${this.formatFileSize(file.size)}</span>
                        <span class="text-xs text-gray-500">${file.type || 'Unknown type'}</span>
                    </div>
                </div>
            </div>
        `).join('');

            // Update summary
            const totalSize = this.selectedFiles.reduce((sum, file) => sum + file.size, 0);
            document.getElementById('totalFiles').textContent = this.selectedFiles.length;
            document.getElementById('totalSize').textContent = this.formatFileSize(totalSize);
        },

        updateUploadButton() {
            const uploadBtn = document.getElementById('uploadBtn');
            uploadBtn.disabled = this.selectedFiles.length === 0;
            uploadBtn.textContent = this.selectedFiles.length > 0 ? `Upload ${this.selectedFiles.length} File${this.selectedFiles.length > 1 ? 's' : ''}` : 'Upload Files';
        },

        uploadFiles() {
            if (this.selectedFiles.length === 0) return;

            const uploadProgress = document.getElementById('uploadProgress');
            const progressBar = document.getElementById('progressBar');
            const progressPercent = document.getElementById('progressPercent');
            const uploadBtn = document.getElementById('uploadBtn');
            const form = document.getElementById('uploadForm');

            uploadProgress.classList.remove('hidden');
            uploadBtn.disabled = true;
            uploadBtn.textContent = 'Uploading...';

            // Create FormData and append files
            const formData = new FormData(form);
            this.selectedFiles.forEach(file => {
                formData.append('file', file);
            });

            // Simulate upload progress
            let progress = 0;
            const interval = setInterval(() => {
                progress += Math.random() * 15;
                if (progress > 100) progress = 100;

                progressBar.style.width = progress + '%';
                progressPercent.textContent = Math.round(progress) + '%';

                if (progress >= 100) {
                    clearInterval(interval);
                    // Submit the form
                    form.submit();
                }
            }, 200);
        },

        // Initialize upload modal
        initializeUploadModal() {
            const dropZone = document.getElementById('dropZone');
            const fileInput = document.getElementById('fileInput');

            if (dropZone && fileInput) {
                dropZone.addEventListener('click', () => fileInput.click());

                dropZone.addEventListener('dragover', (e) => {
                    e.preventDefault();
                    dropZone.classList.add('drag-over');
                });

                dropZone.addEventListener('dragleave', (e) => {
                    e.preventDefault();
                    dropZone.classList.remove('drag-over');
                });

                dropZone.addEventListener('drop', (e) => {
                    e.preventDefault();
                    dropZone.classList.remove('drag-over');

                    const files = e.dataTransfer.files;
                    this.addFiles(files);
                });

                fileInput.addEventListener('change', (e) => {
                    this.addFiles(e.target.files);
                });
            }
        },

        // Utility methods
        getFileIcon(fileName) {
            const fileIcons = {
                'image': 'fas fa-image text-green-500',
                'video': 'fas fa-video text-red-500',
                'audio': 'fas fa-music text-purple-500',
                'pdf': 'fas fa-file-pdf text-red-600',
                'doc': 'fas fa-file-word text-blue-600',
                'xls': 'fas fa-file-excel text-green-600',
                'ppt': 'fas fa-file-powerpoint text-orange-600',
                'zip': 'fas fa-file-archive text-yellow-600',
                'txt': 'fas fa-file-alt text-gray-600',
                'default': 'fas fa-file text-gray-500'
            };

            const extension = fileName.split('.').pop().toLowerCase();

            if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp'].includes(extension)) {
                return fileIcons.image;
            } else if (['mp4', 'avi', 'mov', 'wmv', 'flv', 'webm'].includes(extension)) {
                return fileIcons.video;
            } else if (['mp3', 'wav', 'flac', 'aac', 'ogg'].includes(extension)) {
                return fileIcons.audio;
            } else if (extension === 'pdf') {
                return fileIcons.pdf;
            } else if (['doc', 'docx'].includes(extension)) {
                return fileIcons.doc;
            } else if (['xls', 'xlsx'].includes(extension)) {
                return fileIcons.xls;
            } else if (['ppt', 'pptx'].includes(extension)) {
                return fileIcons.ppt;
            } else if (['zip', 'rar', '7z', 'tar', 'gz'].includes(extension)) {
                return fileIcons.zip;
            } else if (['txt', 'md', 'log'].includes(extension)) {
                return fileIcons.txt;
            }
            return fileIcons.default;
        },

        formatFileSize(bytes) {
            if (bytes === 0) return '0 Bytes';
            const k = 1024;
            const sizes = ['Bytes', 'KB', 'MB', 'GB'];
            const i = Math.floor(Math.log(bytes) / Math.log(k));
            return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
        },

        // Handle password click for folders
        handlePasswordClick(button) {
            const folderId = button.getAttribute('data-folder-id');
            document.getElementById('passwordFolderId').value = folderId;

            // Check if folder has password
            const folderHasPassword = button.closest('[data-folder-id]').getAttribute('data-has-password') === 'true';
            const modalTitle = document.getElementById('passwordModalTitle');
            const modalBtn = document.getElementById('passwordModalBtn');
            const modalHint = document.getElementById('passwordModalHint');

            if (folderHasPassword) {
                modalTitle.textContent = 'Cập nhật mật khẩu thư mục';
                modalBtn.textContent = 'Cập nhật';
                modalHint.style.display = 'block';
            } else {
                modalTitle.textContent = 'Đặt mật khẩu cho thư mục';
                modalBtn.textContent = 'Đặt mật khẩu';
                modalHint.style.display = 'none';
            }

            // Open modal
            this.showPasswordModal = true;
        },

        // Open share modal
        openShareModal(fileId, fileName) {
            // Set form action and input value
            const form = document.getElementById('shareForm');
            const input = document.getElementById('fileIdInput');

            form.setAttribute('action', '/files/send/' + fileId);
            input.value = fileId;

            // Set selected file data
            this.selectedFile.id = fileId;
            this.selectedFile.fileName = fileName;

            // Show modal
            this.showShareModel = true;
        },

        // Handle share button click
        handleShareClick(button) {
            const fileId = button.getAttribute("data-file-id");
            const fileName = button.getAttribute("data-file-name");

            this.openShareModal(fileId, fileName);
        },

        // Handle move button click
        handleMoveClick(button) {
            const fileId = button.getAttribute("data-file-id");
            const fileName = button.getAttribute("data-file-name");

            this.openMoveModal(fileId, fileName);
        },

        // Open move modal
        openMoveModal(fileId, fileName) {
            console.log('Opening move modal for file:', fileId, fileName);

            // Set form action and input value
            const form = document.getElementById('moveForm');
            const input = document.getElementById('moveFileIdInput');

            if (form && input) {
                form.action = '/files/move/' + fileId;
                input.value = fileId;
                console.log('Form action set to:', form.action);
            } else {
                console.error('Form or input not found');
            }

            // Set selected file data
            this.selectedFile.id = fileId;
            this.selectedFile.fileName = fileName;

            // Clear any previously selected radio buttons and select root by default
            const radioButtons = form.querySelectorAll('input[name="targetFolderId"]');
            radioButtons.forEach(radio => radio.checked = false);

            // Select root option by default
            const rootRadio = form.querySelector('input[name="targetFolderId"][value=""]');
            if (rootRadio) {
                rootRadio.checked = true;
            }

            // Show modal
            this.showMoveModal = true;
        },

        // Handle move form submit
        handleMoveSubmit(event) {
            const form = event.target;
            const selectedRadio = form.querySelector('input[name="targetFolderId"]:checked');

            console.log('Form submit:', form.action);
            console.log('Selected radio:', selectedRadio ? selectedRadio.value : 'none');

            if (!selectedRadio) {
                event.preventDefault();
                alert('Please select a destination folder.');
                return false;
            }

            // Let the form submit normally
            return true;
        },

        // Preview file methods
        async openPreviewModal(fileId, fileName) {
            // Cancel any existing requests
            if (this.previewAbortController) {
                this.previewAbortController.abort();
            }

            // Create new abort controller
            this.previewAbortController = new AbortController();

            this.previewData.loading = true;
            this.previewData.error = null;
            this.previewData.fileId = fileId;
            this.previewData.fileName = fileName;
            this.showPreviewModal = true;

            try {
                // Get file metadata with abort signal
                const metadataResponse = await fetch(`/files/api/preview/${fileId}/metadata`, {
                    signal: this.previewAbortController.signal
                });
                if (!metadataResponse.ok) {
                    throw new Error('Failed to load file metadata');
                }
                const metadata = await metadataResponse.json();

                // Get preview info with abort signal
                const previewResponse = await fetch(`/files/api/preview/${fileId}/info`, {
                    signal: this.previewAbortController.signal
                });
                if (!previewResponse.ok) {
                    throw new Error('Failed to load preview info');
                }
                const previewInfo = await previewResponse.json();

                // Update preview data
                this.previewData.fileName = metadata.fileName;
                this.previewData.fileType = metadata.fileType;
                this.previewData.fileSize = metadata.fileSizeFormatted;
                this.previewData.previewUrl = previewInfo.previewUrl;
                this.previewData.previewType = previewInfo.previewType.toLowerCase();
                this.previewData.loading = false;

            } catch (error) {
                // Don't show error if request was aborted (user closed modal)
                if (error.name === 'AbortError') {
                    console.log('Preview request was cancelled');
                    return;
                }

                console.error('Error loading preview:', error);
                this.previewData.error = error.message;
                this.previewData.loading = false;
            }
        },

        downloadCurrentFile() {
            if (this.previewData.fileId) {
                window.location.href = `/files/download/${this.previewData.fileId}`;
            }
        },

        closePreviewModal() {
            // Stop any video playback before closing
            const videoElements = document.querySelectorAll('.preview-modal video');
            videoElements.forEach(video => {
                if (!video.paused) {
                    video.pause();
                }
                // Clear video source to stop any ongoing requests
                video.src = '';
                video.load();
            });

            // Destroy video player instances
            const videoPlayerElements = document.querySelectorAll('[x-data*="videoPlayer"]');
            videoPlayerElements.forEach(element => {
                if (element._x_dataStack && element._x_dataStack[0] && element._x_dataStack[0].destroy) {
                    element._x_dataStack[0].destroy();
                }
            });

            // Clear any ongoing fetch requests
            if (this.previewAbortController) {
                this.previewAbortController.abort();
                this.previewAbortController = null;
            }

            this.showPreviewModal = false;
            this.previewData = {
                fileId: null,
                fileName: '',
                fileType: '',
                fileSize: '',
                previewUrl: '',
                previewType: '',
                loading: false,
                error: null
            };
        },

        // Set folder password
        setFolderPassword(folderId, folderName) {
            // Set the folder ID in the hidden input
            document.getElementById('passwordFolderId').value = folderId;

            // Show the password modal
            this.showPasswordModal = true;
        },

        handlePreviewClick(fileId, fileName) {
            const extension = fileName.split('.').pop().toLowerCase();
            let previewType = 'document';
            if (['mp4', 'webm', 'ogg'].includes(extension)) {
                previewType = 'video';
                this.previewUrl = `/files/stream/${fileId}`;
            } else if (['pdf'].includes(extension)) {
                previewType = 'pdf';
                this.previewUrl = `/files/preview/${fileId}`;
            } else if (['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'].includes(extension)) {
                previewType = 'document';
                this.previewUrl = `/files/preview/${fileId}`;
            }
            this.previewType = previewType;
            this.showPreviewModal = true;

            // For non-PDF documents, load client-side rendering
            if (previewType === 'document') {
                this.loadDocumentPreview(fileId, extension);
            }
        },
        loadDocumentPreview(fileId, extension) {
            // Placeholder for client-side document rendering
            // Use libraries like docx.js or xlsx.js for DOCX/XLSX rendering
            console.log(`Load document preview for file ${fileId} with extension ${extension}`);
        }
    }));
}); 

// Create a global variable to store the dashboard component
window.dashboardInstance = null;

// Function to be called from HTML
document.addEventListener('alpine:initialized', () => {
    // Add a global method to access the dashboard component
    window.removeFileFromAlpine = function(index) {
        if (window.dashboardInstance) {
            window.dashboardInstance.removeFile(index);
        } else {
            console.error('Dashboard instance not available');
        }
    };

    // Add global function for opening move modal
    window.openMoveModal = function(fileId, fileName) {
        if (window.dashboardInstance) {
            window.dashboardInstance.openMoveModal(fileId, fileName);
        } else {
            console.error('Dashboard instance not available');
        }
    };

    // Add event listeners for file action buttons
    document.addEventListener('click', function(e) {
        // Handle move file button
        if (e.target.closest('.move-file-btn')) {
            const button = e.target.closest('.move-file-btn');
            const fileId = button.getAttribute('data-file-id');
            const fileName = button.getAttribute('data-file-name');

            if (window.dashboardInstance) {
                window.dashboardInstance.openMoveModal(fileId, fileName);
            }
        }

        // Handle share file button
        if (e.target.closest('.share-file-btn')) {
            const button = e.target.closest('.share-file-btn');
            const fileId = button.getAttribute('data-file-id');
            const fileName = button.getAttribute('data-file-name');

            if (window.dashboardInstance) {
                window.dashboardInstance.openShareModal(fileId, fileName);
            }
        }

        // Handle folder password button
        if (e.target.closest('.folder-password-btn')) {
            const button = e.target.closest('.folder-password-btn');
            const folderId = button.getAttribute('data-folder-id');

            if (window.dashboardInstance) {
                window.dashboardInstance.handlePasswordClick(button);
            }
        }

        // Handle preview file button
        if (e.target.closest('.preview-file-btn')) {
            const button = e.target.closest('.preview-file-btn');
            const fileId = button.getAttribute('data-file-id');
            const fileName = button.getAttribute('data-file-name');

            if (window.dashboardInstance) {
                window.dashboardInstance.openPreviewModal(fileId, fileName);
            }
        }
    });

    // Cleanup when page unloads to prevent connection errors
    window.addEventListener('beforeunload', function() {
        // Stop all video elements
        const videos = document.querySelectorAll('video');
        videos.forEach(video => {
            if (!video.paused) {
                video.pause();
            }
            video.src = '';
            video.load();
        });

        // Destroy all video player instances
        const videoPlayerElements = document.querySelectorAll('[x-data*="videoPlayer"]');
        videoPlayerElements.forEach(element => {
            if (element._x_dataStack && element._x_dataStack[0] && element._x_dataStack[0].destroy) {
                element._x_dataStack[0].destroy();
            }
        });

        // Cancel any ongoing preview requests
        if (window.dashboardInstance && window.dashboardInstance.previewAbortController) {
            window.dashboardInstance.previewAbortController.abort();
        }
    });
});
