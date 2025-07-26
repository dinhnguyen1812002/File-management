// Alpine.js Dashboard Data and Methods
document.addEventListener('alpine:init', () => {
    Alpine.data('dashboard', () => ({
        // Modal states
        showCreateFolderModal: false,
        showUploadModal: false,
        showShareModel: false,
        showPasswordModal: false,
        showPasswordVerificationModal: false,
        
        // Data objects
        selectedFile: null,
        selectedFolder: null,
        selectedFiles: [],
        passwordVerificationData: {
            folderId: null,
            folderName: '',
            password: '',
            error: ''
        },

        // Initialize Alpine.js component
        init() {
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

            if (!password.trim()) {
                this.passwordVerificationData.error = 'Please enter a password';
                return;
            }

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
        }
    }));
}); 