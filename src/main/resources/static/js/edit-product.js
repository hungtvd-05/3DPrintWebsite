import * as THREE from 'three';
import { STLLoader } from 'three/addons/loaders/STLLoader.js';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';

// Dropzone initialization
let imageDropzone, stlDropzone;
Dropzone.autoDiscover = false;
const existingFilesMaps = {
    images: new Map(),
    stl: new Map()
};

// Danh sách files cần xóa
const filesToDelete = {
    images: new Set(),
    stl: new Set()
};

let setTag = {
    existingTags: new Set(),
    deleteTags: new Set()
};

document.addEventListener('DOMContentLoaded', function() {
    // Image Dropzone
    imageDropzone = new Dropzone("#imageDropzone", {
        url: "/api/save-image",
        autoProcessQueue: true,
        maxFiles: 30,
        paramName: 'file',
        acceptedFiles: "image/*, .webp",
        addRemoveLinks: false,
        previewsContainer: false,
        dictDefaultMessage: "Kéo thả ảnh vào đây hoặc click để chọn ảnh",
        headers: {
            "Accept": "application/json" // Thêm header Accept
        },
        init: function() {
            this.on("addedfile", function(file) {
                // Không hiển thị preview ngay, chờ server trả về tên file
            });
            this.on("success", function(file, response) {
                if (response.success && response.filename) {
                    file.serverFilename = response.filename;
                    showImagePreview(file, response.filename);
                } else {
                    console.error("Invalid server response:", response);
                    alert("Lỗi: Server không trả về tên file hợp lệ");
                    this.removeFile(file);
                }
                console.log(imageDropzone);
            });
            this.on("error", function(file, errorMessage) {
                console.error("Error uploading file:", errorMessage);
                alert("Lỗi khi tải lên file: " + errorMessage);
                this.removeFile(file);
            });
        }
    });

    // STL Dropzone
    stlDropzone = new Dropzone("#stlDropzone", {
        url: "/api/save-stl",
        autoProcessQueue: true,
        maxFiles: 30,
        paramName: 'file',
        acceptedFiles: ".stl",
        addRemoveLinks: false,
        previewsContainer: false,
        dictDefaultMessage: "Kéo thả file STL vào đây hoặc click để chọn file",
        headers: {
            "Accept": "application/json" // Thêm header Accept
        },
        init: function() {
            this.on("addedfile", function(file) {

            });
            this.on("success", function(file, response) {
                if (response.success && response.filename) {
                    file.serverFilename = response.filename;
                    showSTLPreview(file, response.filename);
                } else {
                    console.error("Invalid server response:", response);
                    alert("Lỗi: Server không trả về tên file hợp lệ");
                    this.removeFile(file);
                }
                console.log(imageDropzone);
            });
            this.on("error", function(file, errorMessage) {
                console.error("Error uploading file:", errorMessage);
                alert("Lỗi khi tải lên file: " + errorMessage);
                this.removeFile(file);
            });
        }
    });

    if (existingImages) {
        existingImages.forEach(fileName => {
            existingFilesMaps.images.set(fileName, true);
            showExistingImagePreview(fileName);
        });
    }

    // Sửa lại phần khởi tạo existing STL files
    // Sửa lại phần khởi tạo existing STL files
    if (existingStlFiles) {
        Object.entries(existingStlFiles).forEach(([fileName, createdAt]) => {
            existingFilesMaps.stl.set(fileName, true);

            const div = document.createElement('div');
            div.className = 'preview-item';
            div.dataset.filename = fileName;

            const container = document.createElement('div');
            container.className = 'preview-content';

            // Tạo ảnh preview cho STL existing files
            const previewImg = document.createElement('img');
            previewImg.className = 'stl-preview-image';
            previewImg.style.cssText = `
            width: 300px;
            height: 300px;
            object-fit: cover;
            cursor: pointer;
            border-radius: 4px;
            border: 1px solid #ddd;
        `;

            // Click để mở modal preview (với flag isExisting = true)
            previewImg.onclick = () => showSTLModal(null, fileName, true);

            // Load ảnh preview từ server
            const img = new Image();
            img.onload = function() {
                previewImg.src = this.src;
            };
            img.onerror = function() {
                // Nếu không có ảnh preview, hiển thị placeholder
                previewImg.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjE2OSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjBmMGYwIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxNiIgZmlsbD0iIzk5OSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPk5vIFByZXZpZXc8L3RleHQ+PC9zdmc+';
            };
            img.src = `/product/stl/${fileName}_preview.png?t=${Date.now()}`;

            container.appendChild(previewImg);

            const deleteBtn = document.createElement('button');
            deleteBtn.innerHTML = '<i class="ti ti-trash"></i>';
            deleteBtn.className = 'btn-remove';
            deleteBtn.type = 'button';
            // SỬA: Đổi từ window.removeFile thành window.removeExistingFile
            deleteBtn.onclick = () => window.removeExistingFile(fileName, 'stl');
            container.appendChild(deleteBtn);

            div.appendChild(container);
            document.getElementById('stlPreview').appendChild(div);
        });
    }

    const input = document.getElementById('input-sugg');
    const tagList = document.getElementById('tagList');
    const suggestionList = document.querySelector('.suggestion-list');

    let selectedTags = [];
    let selectedTagNames = new Set();
    let availableTags = new Map();

    // Lấy danh sách tags từ API
    initialTags.forEach(tag => {
        availableTags.set(tag.name.toLowerCase(), { id: tag.id, name: tag.name });
    });

    if (existingTags && existingTags.length > 0) {
        existingTags.forEach(tag => {
            setTag.existingTags.add(tag.id);
            selectedTags.push({ id: tag.id, name: tag.name });
            selectedTagNames.add(tag.name.toLowerCase());
        });
        renderTags(); // Hiển thị các tags có sẵn
    }



    // Xử lý input
    input.addEventListener('input', function() {
        const value = this.value.trim().toLowerCase();
        updateSuggestions(value);
    });

    // Xử lý phím Enter và các phím khác
    input.addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            handleTagInput(this.value.trim());
        } else if (e.key === 'Escape') {
            hideSuggestions();
        }
    });

    // Ẩn suggestions khi click ra ngoài
    document.addEventListener('click', function(e) {
        if (!e.target.closest('.tag-input-wrapper')) {
            hideSuggestions();
        }
    });

    function updateSuggestions(value) {
        if (!value) {
            hideSuggestions();
            return;
        }

        const matches = [];
        availableTags.forEach((tag, key) => {
            if (key.includes(value) && !selectedTagNames.has(key)) {
                matches.push(tag);
            }
        });

        if (matches.length > 0) {
            showSuggestions(matches, value);
        } else {
            hideSuggestions();
        }
    }

    function showSuggestions(matches, inputValue) {
        suggestionList.innerHTML = '';
        matches.forEach(tag => {
            const div = document.createElement('div');
            div.className = 'suggestion-item';
            const matchIndex = tag.name.toLowerCase().indexOf(inputValue.toLowerCase());
            const beforeMatch = tag.name.slice(0, matchIndex);
            const match = tag.name.slice(matchIndex, matchIndex + inputValue.length);
            const afterMatch = tag.name.slice(matchIndex + inputValue.length);
            div.innerHTML = `${beforeMatch}<strong>${match}</strong>${afterMatch}`;

            div.addEventListener('click', () => {
                addTag(tag);
                input.value = '';
                hideSuggestions();
            });

            suggestionList.appendChild(div);
        });

        suggestionList.style.display = 'block';
    }

    function hideSuggestions() {
        suggestionList.style.display = 'none';
    }

    function handleTagInput(value) {
        if (!value) return;

        if (value.length > 50) {
            alert('Tên tag không được vượt quá 50 ký tự.');
            return;
        }

        const lowercaseValue = value.toLowerCase();
        if (selectedTagNames.has(lowercaseValue)) {
            alert('Tag này đã được chọn.');
            input.value = '';
            return;
        }

        const tag = availableTags.has(lowercaseValue)
            ? availableTags.get(lowercaseValue)
            : { id: null, name: value };

        addTag(tag);
        input.value = '';
        hideSuggestions();
    }

    function addTag(tag) {
        const lowercaseTagName = tag.name.toLowerCase();
        if (!selectedTagNames.has(lowercaseTagName)) {
            selectedTags.push(tag);
            selectedTagNames.add(lowercaseTagName);
            renderTags();
        }
    }

    function renderTags() {
        tagList.innerHTML = '';
        selectedTags.forEach((tag, index) => {
            const tagElement = document.createElement('div');
            tagElement.className = 'tag';
            tagElement.innerHTML = `
                ${tag.name}
                <span class="remove-tag" data-index="${index}">
                    <i class="ti ti-backspace"></i>
                </span>
            `;
            tagList.appendChild(tagElement);
        });

        // Xử lý xóa tag
        document.querySelectorAll('.remove-tag').forEach(button => {
            button.addEventListener('click', function() {
                const index = parseInt(this.getAttribute('data-index'));
                const removedTag = selectedTags[index];
                selectedTags.splice(index, 1);
                selectedTagNames.delete(removedTag.name.toLowerCase());
                if (setTag.existingTags.has(removedTag.id)) {
                    setTag.deleteTags.add(removedTag.id);
                }
                renderTags();
            });
        });
    }

    // Xử lý submit form
    document.getElementById('productForm').addEventListener('submit', function(e) {

        e.preventDefault();

        if ((!imageDropzone || !imageDropzone.files || imageDropzone.files.length === 0) && existingFilesMaps.images.size === 0) {
            alert('Vui lòng chọn ít nhất một ảnh sản phẩm');
            return;
        }

        if ((!stlDropzone || !stlDropzone.files || stlDropzone.files.length === 0) && existingFilesMaps.stl.size === 0) {
            alert('Vui lòng chọn ít nhất một file STL');
            return;
        }

        if (selectedTags.length === 0) {
            alert('Vui lòng chọn ít nhất một tag');
            return;
        }

        const formData = new FormData(this);
        let imagesServer = [];
        let stlFilesServer = [];

        // Thêm tệp vào FormData
        if (typeof imageDropzone !== 'undefined' && imageDropzone.files) {
            imageDropzone.files.forEach(file => imagesServer.push(file.serverFilename));
            formData.append('images', JSON.stringify(imagesServer));
        }

        if (typeof stlDropzone !== 'undefined' && stlDropzone.files) {
            stlDropzone.files.forEach(file => stlFilesServer.push(file.serverFilename));
            formData.append('stlFiles', JSON.stringify(stlFilesServer));
        }

        // formData.append('existingImages', JSON.stringify(Array.from(existingFilesMaps.images.keys())));
        // formData.append('existingStlFiles', JSON.stringify(Array.from(existingFilesMaps.stl.keys())));

        formData.append('deleteImages', JSON.stringify(Array.from(filesToDelete.images)));
        formData.append('deleteStlFiles', JSON.stringify(Array.from(filesToDelete.stl)));

        // Thêm tags vào FormData
        formData.append('tags', JSON.stringify(selectedTags));
        // formData.append('deleteTags', JSON.stringify(Array.from(setTag.deleteTags)));

        // Log nội dung FormData để kiểm tra
        for (let pair of formData.entries()) {
            console.log(`${pair[0]}: ${pair[1]}`);
        }


        // Gửi yêu cầu POST
        fetch('/api/update-product/' + productId, {
            method: 'POST',
            body: formData
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Network response was not ok: ${response.status} - ${response.statusText}`);
                }
                return response.text();
            })
            .then(data => {
                if (data === 'ok') {
                    alert('Cập nhật sản phẩm thành công');
                    const currentPath = window.location.pathname;
                    if (currentPath === '/admin/edit-product:' + productId) {
                        window.location.href = '/admin/products';
                    } else {
                        window.location.href = '/user/my-products';
                    }
                } else {
                    alert('Có lỗi xảy ra: ' + data);
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Có lỗi xảy ra: ' + error.message);
            });
    });
});

function showImagePreview(file, serverFilename) {
    const preview = document.getElementById('imagePreview');
    const reader = new FileReader();
    reader.onload = function(e) {
        const div = document.createElement('div');
        div.className = 'preview-item';
        div.dataset.filename = serverFilename;
        div.innerHTML = `
            <div class="preview-content">
                <img src="${e.target.result}" class="preview-image">
                <button type="button" class="btn-remove" onclick="window.removeFile('${serverFilename}', 'image')"><i class="ti ti-trash"></i></button>
            </div>
        `;
        preview.appendChild(div);
    };
    reader.readAsDataURL(file);
}

function showSTLPreview(file, serverFilename) {
    const preview = document.getElementById('stlPreview');
    const div = document.createElement('div');
    div.className = 'preview-item';
    div.dataset.filename = serverFilename;

    const container = document.createElement('div');
    container.className = 'preview-content';

    const previewImg = document.createElement('img');
    previewImg.className = 'stl-preview-image';
    previewImg.style.cssText = `
        width: 300px;
        height: 300px;
        object-fit: cover;
        cursor: pointer;
        border-radius: 4px;
        border: 1px solid #ddd;
    `;

    const isExistingFile = existingFilesMaps.stl.has(serverFilename);

    // KHÔNG thêm file mới vào existingFilesMaps.stl
    // Chỉ kiểm tra xem có phải existing file không

    previewImg.onclick = () => showSTLModal(file, serverFilename, isExistingFile);

    previewImg.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjE2OSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjBmMGYwIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxNiIgZmlsbD0iIzk5OSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPkxvYWRpbmcuLi48L3RleHQ+PC9zdmc+';

    container.appendChild(previewImg);

    const deleteBtn = document.createElement('button');
    deleteBtn.innerHTML = '<i class="ti ti-trash"></i>';
    deleteBtn.className = 'btn-remove';
    deleteBtn.type = 'button';
    // SỬA: Sử dụng hàm đúng cho từng loại file
    deleteBtn.onclick = () => {
        if (isExistingFile) {
            window.removeExistingFile(serverFilename, 'stl');
        } else {
            // File mới upload - xóa khỏi dropzone
            window.removeFile(serverFilename, 'stl');
        }
    };
    container.appendChild(deleteBtn);

    div.appendChild(container);
    preview.appendChild(div);

    // Tạo ảnh đại diện với chất lượng cao (chỉ cho file mới)
    if (!isExistingFile && file) {
        setupSTLViewer(null, file, serverFilename, true);
    }

    // Load ảnh preview từ server
    setTimeout(() => {
        const img = new Image();
        img.onload = function() {
            previewImg.src = this.src;
        };
        img.onerror = function() {
            console.error('Failed to load preview image');
            if (!isExistingFile) {
                setTimeout(() => {
                    previewImg.src = `/tmp/stl/${serverFilename}_preview.png?t=${Date.now()}`;
                }, 2000);
            }
        };

        const previewPath = isExistingFile
            ? `/product/stl/${serverFilename}_preview.png`
            : `/tmp/stl/${serverFilename}_preview.png`;

        img.src = `${previewPath}?t=${Date.now()}`;
    }, isExistingFile ? 0 : 3000);
}

// Thêm vào cuối file edit-product.js, trước phần xử lý removeFile

function showSTLModal(file, serverFilename, isExisting = false) {
    // Tạo modal
    const modal = document.createElement('div');
    modal.className = 'stl-modal';
    modal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0,0,0,0.8);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 1000;
        padding: 20px;
        box-sizing: border-box;
    `;

    const modalContent = document.createElement('div');
    modalContent.style.cssText = `
        background: white;
        padding: 20px;
        border-radius: 8px;
        position: relative;
        display: flex;
        flex-direction: column;
        box-sizing: border-box;
    `;

    const closeBtn = document.createElement('button');
    closeBtn.innerHTML = '×';
    closeBtn.style.cssText = `
        position: absolute;
        top: 10px;
        right: 15px;
        background: none;
        border: none;
        font-size: 28px;
        cursor: pointer;
        z-index: 1001;
        color: #666;
        font-weight: bold;
    `;
    closeBtn.onclick = () => {
        const container = modalContent.querySelector('.stl-3d-container');
        if (container && container.animationId) {
            cancelAnimationFrame(container.animationId);
        }
        document.body.removeChild(modal);
        window.removeEventListener('resize', handleResize);
    };

    const title = document.createElement('h3');
    title.textContent = `Preview STL: ${serverFilename}`;
    title.style.cssText = `
        margin: 0 0 15px 0;
        padding-right: 40px;
        color: #333;
        font-size: clamp(14px, 2.5vw, 18px);
        word-wrap: break-word;
        flex-shrink: 0;
    `;

    const stlContainer = document.createElement('div');
    stlContainer.className = 'stl-3d-container';
    stlContainer.style.cssText = `
        width: 100%;
        background: #f8f9fa;
        border-radius: 4px;
        overflow: hidden;
        position: relative;
    `;

    modalContent.appendChild(closeBtn);
    modalContent.appendChild(title);
    modalContent.appendChild(stlContainer);
    modal.appendChild(modalContent);
    document.body.appendChild(modal);

    let stlViewer = null;

    // Hàm tính toán kích thước với tỷ lệ khung hình cố định
    function calculateModalSize() {
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;

        // Tỷ lệ khung hình mong muốn (4:3)
        const aspectRatio = 4 / 3;

        // Khoảng trống cho padding và UI elements
        const padding = 40;
        const titleHeight = 60; // Ước tính chiều cao title + margin

        const availableWidth = viewportWidth - padding * 2;
        const availableHeight = viewportHeight - padding * 2 - titleHeight;

        let modalWidth, modalHeight;

        if (viewportWidth <= 768) {
            // Mobile: chiếm 95% viewport
            modalWidth = Math.min(availableWidth, viewportWidth * 0.95);
            modalHeight = modalWidth / aspectRatio;

            // Nếu chiều cao vượt quá available height, điều chỉnh lại
            if (modalHeight > availableHeight) {
                modalHeight = availableHeight;
                modalWidth = modalHeight * aspectRatio;
            }
        } else if (viewportWidth <= 1024) {
            // Tablet: tối đa 700px width
            modalWidth = Math.min(availableWidth, 700);
            modalHeight = modalWidth / aspectRatio;

            if (modalHeight > availableHeight) {
                modalHeight = availableHeight;
                modalWidth = modalHeight * aspectRatio;
            }
        } else {
            // Desktop: tối đa 900px width
            modalWidth = Math.min(availableWidth, 900);
            modalHeight = modalWidth / aspectRatio;

            if (modalHeight > availableHeight) {
                modalHeight = Math.min(availableHeight, 675); // 900 * 3/4
                modalWidth = modalHeight * aspectRatio;
            }
        }

        return {
            width: modalWidth,
            height: modalHeight,
            containerHeight: modalHeight - titleHeight
        };
    }

    // Hàm cập nhật kích thước modal
    function updateModalSize() {
        const size = calculateModalSize();

        modalContent.style.width = `${size.width}px`;
        modalContent.style.height = `${size.height}px`;
        stlContainer.style.height = `${size.containerHeight}px`;

        return {
            width: size.width - 40, // trừ padding
            height: size.containerHeight
        };
    }

    // Hàm resize viewer
    function resizeViewer() {
        if (stlViewer && stlViewer.renderer && stlViewer.camera) {
            const canvasSize = updateModalSize();
            stlViewer.renderer.setSize(canvasSize.width, canvasSize.height);
            stlViewer.camera.aspect = canvasSize.width / canvasSize.height;
            stlViewer.camera.updateProjectionMatrix();
        }
    }

    // Event listener cho resize
    const handleResize = () => {
        resizeViewer();
    };
    window.addEventListener('resize', handleResize);

    // Thiết lập kích thước ban đầu
    const initialSize = updateModalSize();

    // Hàm tính toán kích thước cho STL viewer
    function calculateSize() {
        // Đảm bảo sử dụng kích thước thực tế của container sau khi render
        const containerRect = stlContainer.getBoundingClientRect();
        if (containerRect.width > 0 && containerRect.height > 0) {
            return {
                width: containerRect.width,
                height: containerRect.height
            };
        }
        // Fallback nếu container chưa render
        return {
            width: initialSize.width,
            height: initialSize.height
        };
    }

    // Sử dụng requestAnimationFrame để đảm bảo DOM đã render xong
    requestAnimationFrame(() => {
        // Thiết lập STL viewer sau khi modal đã render hoàn toàn
        if (isExisting) {
            // Cho file có sẵn, load từ server path
            fetch(`/product/stl/${serverFilename}`)
                .then(response => response.arrayBuffer())
                .then(arrayBuffer => {
                    // Tạo File object từ ArrayBuffer
                    const blob = new Blob([arrayBuffer], { type: 'application/octet-stream' });
                    const existingFile = new File([blob], serverFilename, { type: 'application/octet-stream' });

                    stlViewer = setupSTLViewer(stlContainer, existingFile, serverFilename, false, true, calculateSize);

                    // Force resize ngay sau khi khởi tạo
                    setTimeout(() => {
                        resizeViewer();
                    }, 100);
                })
                .catch(error => {
                    console.error('Error loading existing STL file:', error);
                    stlContainer.innerHTML = '<div style="display: flex; justify-content: center; align-items: center; height: 100%; color: #999;">Không thể tải file STL</div>';
                });
        } else {
            // Cho file mới upload
            stlViewer = setupSTLViewer(stlContainer, file, serverFilename, false, true, calculateSize);

            // Force resize ngay sau khi khởi tạo để đảm bảo kích thước chính xác
            setTimeout(() => {
                resizeViewer();
            }, 100);
        }
    });

    // Đóng modal khi click bên ngoài
    modal.onclick = (e) => {
        if (e.target === modal) {
            closeBtn.click();
        }
    };

    // Đóng modal khi nhấn Escape
    const handleEscape = (e) => {
        if (e.key === 'Escape') {
            closeBtn.click();
            document.removeEventListener('keydown', handleEscape);
        }
    };
    document.addEventListener('keydown', handleEscape);
}

function setupSTLViewer(container, file, serverFilename, isPreview = false, isModal = false, sizeCalculator = null) {
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(75, 1, 0.1, 1000);

    // Xác định kích thước renderer
    let rendererWidth, rendererHeight;
    if (isPreview) {
        // Sử dụng kích thước FullHD cho preview
        rendererWidth = 600;
        rendererHeight = 600;
    } else if (isModal && sizeCalculator) {
        const size = sizeCalculator();
        rendererWidth = size.width;
        rendererHeight = size.height;
    } else if (isModal) {
        const containerRect = container.getBoundingClientRect();
        if (containerRect.width > 0 && containerRect.height > 0) {
            rendererWidth = containerRect.width;
            rendererHeight = containerRect.height;
        } else {
            rendererWidth = 800;
            rendererHeight = 600;
        }
    } else {
        rendererWidth = 300;
        rendererHeight = 300;
    }

    const renderer = new THREE.WebGLRenderer({
        antialias: true,
        preserveDrawingBuffer: true,
        powerPreference: "high-performance"
    });

    renderer.setSize(rendererWidth, rendererHeight);

    // Tăng pixel ratio cho preview để có chất lượng cao hơn
    if (isPreview) {
        renderer.setPixelRatio(2);
    } else {
        renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    }

    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;

    // Cập nhật camera aspect ratio
    camera.aspect = rendererWidth / rendererHeight;
    camera.updateProjectionMatrix();

    if (!isPreview) {
        container.appendChild(renderer.domElement);

        if (isModal) {
            renderer.domElement.style.cssText = `
                width: 100% !important;
                height: 100% !important;
                display: block;
                border-radius: 4px;
                object-fit: contain;
            `;

            container.style.cssText += `
                padding: 0;
                margin: 0;
                display: flex;
                justify-content: center;
                align-items: center;
            `;
        }
    }

    scene.background = new THREE.Color(0xf0f0f0);

    // Tăng chất lượng ánh sáng cho preview
    const ambientLight = new THREE.AmbientLight(0xffffff, isPreview ? 0.7 : 0.6);
    scene.add(ambientLight);

    const directionalLight = new THREE.DirectionalLight(0xffffff, isPreview ? 1.0 : 0.8);
    directionalLight.position.set(10, 10, 5);
    directionalLight.castShadow = true;

    // Tăng độ phân giải shadow map cho preview
    if (isPreview) {
        directionalLight.shadow.mapSize.width = 4096;
        directionalLight.shadow.mapSize.height = 4096;
    } else {
        directionalLight.shadow.mapSize.width = 2048;
        directionalLight.shadow.mapSize.height = 2048;
    }
    scene.add(directionalLight);

    const controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.dampingFactor = 0.05;

    let animationId;

    const reader = new FileReader();
    reader.onload = function(e) {
        const loader = new STLLoader();
        const geometry = loader.parse(e.target.result);

        const material = new THREE.MeshLambertMaterial({
            color: 0x1e88e5,
            transparent: true,
            opacity: 0.9
        });
        const mesh = new THREE.Mesh(geometry, material);
        mesh.castShadow = true;
        mesh.receiveShadow = true;

        geometry.computeBoundingBox();
        const bbox = geometry.boundingBox;
        geometry.center();

        const maxDim = Math.max(
            bbox.max.x - bbox.min.x,
            bbox.max.y - bbox.min.y,
            bbox.max.z - bbox.min.z
        );

        // Tạo lưới và mặt phẳng ở đáy
        const gridSize = maxDim * 2;
        const gridHelper = new THREE.GridHelper(gridSize, 20, 0x888888, 0xcccccc);
        gridHelper.position.y = bbox.min.y - 0.1;
        scene.add(gridHelper);

        const planeGeometry = new THREE.PlaneGeometry(gridSize, gridSize);
        const planeMaterial = new THREE.MeshLambertMaterial({
            color: 0xffffff,
            transparent: true,
            opacity: 0.3
        });
        const plane = new THREE.Mesh(planeGeometry, planeMaterial);
        plane.rotation.x = -Math.PI / 2;
        plane.position.y = bbox.min.y - 0.05;
        plane.receiveShadow = true;
        scene.add(plane);

        // Điều chỉnh vị trí camera - zoom gần hơn cho preview
        if (isPreview) {
            // Zoom gần hơn cho preview - giảm khoảng cách từ 1.5 xuống 1.0
            camera.position.set(maxDim * 1.0, maxDim * 0.8, maxDim * 1.0);
            // Nhìn vào trung tâm sản phẩm, hơi lệch lên trên
            const centerY = (bbox.max.y + bbox.min.y) / 2 + maxDim * 0.1;
            camera.lookAt(0, centerY, 0);
        } else {
            // Vị trí bình thường cho modal
            camera.position.set(maxDim * 1.5, maxDim * 1.2, maxDim * 1.5);
            camera.lookAt(0, 0, 0);
        }

        scene.add(mesh);

        // Render một frame để tạo ảnh
        renderer.render(scene, camera);

        if (isPreview) {
            // Tạo ảnh đại diện với chất lượng cao và upload lên server
            const canvas = renderer.domElement;
            canvas.toBlob(function(blob) {
                const formData = new FormData();
                formData.append('file', blob, `${serverFilename}_preview.png`);
                formData.append('stlFileName', serverFilename);

                fetch('/api/save-stl-preview', {
                    method: 'POST',
                    body: formData,
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                })
                    .then(response => response.json())
                    .then(data => {
                        if (data.success) {
                            console.log('Preview image saved:', data.filename);
                        } else {
                            console.error('Error saving preview:', data.filename);
                        }
                    })
                    .catch(error => {
                        console.error('Error uploading preview:', error);
                    });
            }, 'image/png', 1.0); // Chất lượng tối đa (1.0)
        } else {
            // Animation loop cho modal
            function animate() {
                animationId = requestAnimationFrame(animate);
                controls.update();
                renderer.render(scene, camera);
            }
            animate();
            container.animationId = animationId;
        }
    };
    reader.readAsArrayBuffer(file);

    return { renderer, camera };
}

window.removeFile = function(fileName, type) {
    const dropzone = type === 'image' ? imageDropzone : stlDropzone;
    // SỬA: Tìm file theo serverFilename thay vì name
    const file = dropzone.files.find(f => f.serverFilename === fileName);
    if (file) {
        dropzone.removeFile(file);
    }

    if (type === 'image') {
        const formData = new FormData();
        formData.append('fileName', fileName);
        fetch('/api/delete-image', {
            method: 'POST',
            body: formData
        });
    } else if (type === 'stl') {
        const formData = new FormData();
        formData.append('fileName', fileName);
        fetch('/api/delete-stl', {
            method: 'POST',
            body: formData
        });
    }

    const previewElement = document.querySelector(`.preview-item[data-filename="${fileName}"]`);
    if (previewElement) {
        previewElement.remove();
    }
};

function showExistingImagePreview(fileName) {
    const preview = document.getElementById('imagePreview');
    const div = document.createElement('div');
    div.className = 'preview-item';
    div.dataset.filename = fileName;
    div.innerHTML = `
        <div class="preview-content">
            <img src="/product/img/${fileName}" class="preview-image">
            <button type="button" class="btn-remove" onclick="window.removeExistingFile('${fileName}', 'image')">
                <i class="ti ti-trash"></i>
            </button>
        </div>
    `;
    preview.appendChild(div);
}

// Thêm hàm mới để hiển thị STL có sẵn
function showExistingSTLPreview(fileName) {
    const preview = document.getElementById('stlPreview');
    const div = document.createElement('div');
    div.className = 'preview-item';
    div.dataset.filename = fileName;

    const container = document.createElement('div');
    container.className = 'preview-content';
    div.appendChild(container);

    // Thêm nút xóa
    const deleteBtn = document.createElement('button');
    deleteBtn.innerHTML = '<i class="ti ti-trash"></i>';
    deleteBtn.className = 'btn-remove';
    deleteBtn.type = 'button';
    deleteBtn.onclick = () => window.removeExistingFile(fileName, 'stl');
    container.appendChild(deleteBtn);

    preview.appendChild(div);

    // Load và hiển thị file STL
    fetch(`/product/stl/${fileName}`)
        .then(response => response.arrayBuffer())
        .then(buffer => {
            setupSTLViewer(container, buffer);
        });
}

window.removeExistingFile = function(fileName, type) {
    const fileType = type === 'image' ? 'images' : 'stl';

    // Xóa khỏi map files hiện có
    existingFilesMaps[fileType].delete(fileName);

    // Thêm vào danh sách files cần xóa
    filesToDelete[fileType].add(fileName);

    // Xóa preview
    const previewElement = document.querySelector(`.preview-item[data-filename="${fileName}"]`);
    if (previewElement) {
        previewElement.remove();
    }
};


