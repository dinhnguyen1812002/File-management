// Dashboard JavaScript functionality
let selectedFiles = [];

// File type icons mapping
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

function getFileIcon(fileName) {
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
}

function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function addFiles(files) {
    Array.from(files).forEach(file => {
        // Check file size (100MB limit)
        if (file.size > 100 * 1024 * 1024) {
            alert(`File "${file.name}" is too large. Maximum size is 100MB.`);
            return;
        }

        // Check if file already exists
        if (selectedFiles.find(f => f.name === file.name && f.size === file.size)) {
            return;
        }

        selectedFiles.push(file);
    });

    updateFilePreview();
    updateUploadButton();
}

function removeFile(index) {
    selectedFiles.splice(index, 1);
    updateFilePreview();
    updateUploadButton();
}

function clearAllFiles() {
    selectedFiles = [];
    updateFilePreview();
    updateUploadButton();
}

function updateFilePreview() {
    const filePreview = document.getElementById('filePreview');
    const fileList = document.getElementById('fileList');

    if (selectedFiles.length === 0) {
        filePreview.classList.add('hidden');
        return;
    }

    filePreview.classList.remove('hidden');

    fileList.innerHTML = selectedFiles.map((file, index) => `
    <div class="file-preview-item bg-white border border-gray-200 rounded-lg p-4 flex items-center space-x-4">
        <div class="flex-shrink-0">
            <i class="${getFileIcon(file.name)} text-2xl"></i>
        </div>
        <div class="flex-1 min-w-0">
            <div class="flex items-center justify-between">
                <h5 class="text-sm font-medium text-gray-900 truncate">${file.name}</h5>
                <button type="button" onclick="removeFile(${index})" class="ml-2 text-red-500 hover:text-red-700 transition-colors">
                    <i class="fas fa-times"></i>
                </button>
            </div>
            <div class="flex items-center space-x-4 mt-1">
                <span class="text-xs text-gray-500">${formatFileSize(file.size)}</span>
                <span class="text-xs text-gray-500">${file.type || 'Unknown type'}</span>
            </div>
        </div>
    </div>
`).join('');

    // Update summary
    const totalSize = selectedFiles.reduce((sum, file) => sum + file.size, 0);
    document.getElementById('totalFiles').textContent = selectedFiles.length;
    document.getElementById('totalSize').textContent = formatFileSize(totalSize);
}

function updateUploadButton() {
    const uploadBtn = document.getElementById('uploadBtn');
    uploadBtn.disabled = selectedFiles.length === 0;
    uploadBtn.textContent = selectedFiles.length > 0 ? `Upload ${selectedFiles.length} File${selectedFiles.length > 1 ? 's' : ''}` : 'Upload Files';
}

function uploadFiles() {
    if (selectedFiles.length === 0) return;

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
    selectedFiles.forEach(file => {
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
}

// Initialize drag and drop functionality when modal is shown
function initializeUploadModal() {
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
            addFiles(files);
        });

        fileInput.addEventListener('change', (e) => {
            addFiles(e.target.files);
        });
    }
}

// Initialize when Alpine.js is ready
document.addEventListener('alpine:init', () => {
    Alpine.data('uploadModal', () => ({
        showUploadModal: false,
        init() {
            this.$watch('showUploadModal', (value) => {
                if (value) {
                    setTimeout(initializeUploadModal, 100);
                } else {
                    clearAllFiles();
                }
            });
        }
    }));
});

function handlePasswordClick(button) {
    const folderId = button.getAttribute('data-folder-id');
    document.getElementById('passwordFolderId').value = folderId;

    // Mở modal thông qua Alpine
    document.querySelector('[x-data]').__x.$data.showPasswordModal = true;
}

function openShareModal(fileId, fileName) {
    // Gán action đúng endpoint
    const form = document.getElementById('shareForm');
    const input = document.getElementById('fileIdInput');

    form.setAttribute('action', '/files/send/' + fileId);
    input.value = fileId;

    // Hiển thị tên file
    document.querySelector('[x-text="selectedFile.fileName"]').textContent = fileName;

    // Hiển thị modal
    document.querySelector('[x-data]').__x.$data.showShareModel = true;
}

function handleShareClick(button) {
    const fileId = button.getAttribute("data-file-id");
    const fileName = button.getAttribute("data-file-name");

    openShareModal(fileId, fileName);
}

function setFolderPassword(folderId, folderName) {
    // Set the folder ID in the hidden input
    document.getElementById('passwordFolderId').value = folderId;

    // Show the password modal
    document.querySelector('[x-data]').__x.$data.showPasswordModal = true;
}

function updateDownloadButton() {
    const checkboxes = document.querySelectorAll('.file-checkbox:checked');
    const downloadBtn = document.getElementById('downloadSelectedBtn');

    if (checkboxes.length > 0) {
        downloadBtn.classList.remove('hidden');
        downloadBtn.textContent = `Download Selected (${checkboxes.length})`;
    } else {
        downloadBtn.classList.add('hidden');
    }
}

function downloadSelectedFiles() {
    const form = document.getElementById('multipleDownloadForm');
    const checkboxes = document.querySelectorAll('.file-checkbox:checked');

    if (checkboxes.length === 0) {
        alert('Please select at least one file to download');
        return;
    }

    form.submit();
}

// Global function to call Alpine.js methods
function removeFileFromAlpine(index) {
    const alpineComponent = document.querySelector('[x-data="dashboard"]').__x.$data;
    alpineComponent.removeFile(index);
} 