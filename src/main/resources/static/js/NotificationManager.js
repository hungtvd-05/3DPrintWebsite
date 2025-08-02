class NotificationManager {
    constructor() {
        this.notifications = [];
        this.unreadCount = 0;
        this.userId = null;
        this.stompClient = null;
        this.currentProductId = null;
        this.processedNotifications = new Map();
        this.maxNotifications = 30;
        this.reconnectAttempts = 0;
        this.updateTimer = null;
        this.metrics = {
            notificationsReceived: 0,
            duplicatesFiltered: 0,
            errorsOccurred: 0
        };
        this.notificationKeys = new Set();
        // Map để theo dõi notification theo key
        this.notificationsByKey = new Map();

        this.initializeElements();
        this.extractProductId();
        this.initializeWebSocket();
        this.bindEvents();
        this.loadNotificationsFromStorage();
        this.loadNotificationsFromServer();

        setInterval(() => this.logMetrics(), 300000);

        console.log('NotificationManager initialized with key-based deduplication');
    }

    initializeElements() {
        try {
            this.notificationBadge = document.getElementById('notificationBadge');
            this.notificationList = document.getElementById('notificationList');
            this.noNotifications = document.getElementById('noNotifications');
            this.markAllReadBtn = document.getElementById('markAllRead');

            const userIdElement = document.querySelector('[data-user-id]');
            if (userIdElement) {
                this.userId = parseInt(userIdElement.getAttribute('data-user-id'));
                console.log('User ID found:', this.userId);
            } else {
                console.log('User ID not found');
            }
        } catch (error) {
            this.handleError(error, 'initializeElements');
        }
    }

    extractProductId() {
        try {
            const urlPath = window.location.pathname;
            const productMatch = urlPath.match(/\/product:(\d+)/);
            if (productMatch) {
                this.currentProductId = parseInt(productMatch[1]);
                console.log('Current product ID:', this.currentProductId);
            }
        } catch (error) {
            this.handleError(error, 'extractProductId');
        }
    }

    initializeWebSocket() {
        try {
            if (this.stompClient && this.stompClient.connected) {
                console.log('WebSocket already connected');
                return;
            }

            console.log('Initializing WebSocket...');

            if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
                console.error('SockJS or Stomp not loaded');
                return;
            }

            const socket = new SockJS('/ws');
            this.stompClient = Stomp.over(socket);

            if (window.location.hostname !== 'localhost') {
                this.stompClient.debug = null;
            } else {
                this.stompClient.debug = (str) => console.log('STOMP: ' + str);
            }

            this.stompClient.heartbeat.outgoing = 20000;
            this.stompClient.heartbeat.incoming = 20000;

            this.stompClient.connect({},
                (frame) => this.onConnected(frame),
                (error) => this.onError(error)
            );
        } catch (error) {
            this.handleError(error, 'initializeWebSocket');
        }
    }

    onConnected(frame) {
        console.log('Connected to WebSocket: ' + frame);
        this.reconnectAttempts = 0;
        this.subscribeToNotifications();
    }

    onError(error) {
        console.error('WebSocket connection error:', error);
        this.metrics.errorsOccurred++;
        this.reconnectAttempts++;

        const delay = Math.min(5000 * this.reconnectAttempts, 30000);

        setTimeout(() => {
            if (this.reconnectAttempts < 5) {
                console.log(`Retrying WebSocket connection... (attempt ${this.reconnectAttempts})`);
                this.initializeWebSocket();
            } else {
                console.error('Max reconnection attempts reached');
            }
        }, delay);
    }

    subscribeToNotifications() {
        if (!this.stompClient) {
            console.error('STOMP client not available');
            return;
        }

        try {
            console.log('Subscribing to notifications...');

            if (this.userId) {
                this.stompClient.subscribe(`/user/${this.userId}/queue/notifications`, (message) => {
                    const notification = JSON.parse(message.body);
                    this.handleNotification(notification, true);
                });
                console.log(`Subscribed to /user/${this.userId}/queue/notifications`);
            }

            if (this.currentProductId) {
                this.stompClient.subscribe(`/topic/product/${this.currentProductId}`, (message) => {
                    const notification = JSON.parse(message.body);
                    if (notification.type === 'new_comment' || notification.type === 'new_reply') {
                        this.handleCommentNotification(notification);
                    }
                });
                console.log(`Subscribed to /topic/product/${this.currentProductId}`);
            }

            this.stompClient.subscribe('/topic/notifications', (message) => {
                const notification = JSON.parse(message.body);
                this.handleNotification(notification, true);
            });
            console.log('Subscribed to /topic/notifications');

        } catch (error) {
            this.handleError(error, 'subscribeToNotifications');
        }
    }

    bindEvents() {
        try {
            if (this.markAllReadBtn) {
                this.markAllReadBtn.addEventListener('click', (e) => {
                    e.preventDefault();
                    this.markAllAsRead();
                });
            }
        } catch (error) {
            this.handleError(error, 'bindEvents');
        }
    }

    handleNotification(notification, addNotification = false) {
        try {
            console.log('Handling notification:', notification);
            this.metrics.notificationsReceived++;

            if (notification.user && notification.user.userId === this.userId) {
                console.log('Ignoring own notification');
                return;
            }

            // Sử dụng notificationKey từ server hoặc tạo key backup
            const notificationKey = notification.notificationKey || this.createNotificationKey(notification);

            if (addNotification) {
                const newNotification = {
                    id: notification.id || Date.now(),
                    type: notification.type,
                    title: this.getNotificationTitle(notification),
                    message: this.getNotificationMessage(notification),
                    avatar: notification.user?.profileImage || 'default.png',
                    productId: notification.productId,
                    timestamp: new Date(),
                    read: false,
                    notificationKey: notificationKey,
                    serverId: notification.id
                };

                this.addOrUpdateNotification(newNotification);
            }

            this.showToastNotification(notification);
        } catch (error) {
            this.handleError(error, 'handleNotification');
        }
    }

    createNotificationKey(notification) {
        if (notification.notificationKey) {
            return notification.notificationKey;
        }

        if (notification.id) {
            return `notification_${notification.id}`;
        }

        const userId = notification.user?.userId || 'unknown';
        const productId = notification.productId || 'unknown';
        const type = notification.type || 'unknown';
        const content = (notification.content || '').substring(0, 50);

        return `${type}_${userId}_${productId}_${content}`;
    }

    addOrUpdateNotification(newNotification) {
        try {
            const notificationKey = newNotification.notificationKey;

            if (this.notificationsByKey.has(notificationKey)) {
                // Tìm và xóa notification cũ
                const oldNotificationIndex = this.notifications.findIndex(n => n.notificationKey === notificationKey);

                if (oldNotificationIndex !== -1) {
                    const oldNotification = this.notifications[oldNotificationIndex];

                    // Nếu notification cũ chưa đọc, giữ nguyên unreadCount
                    if (!oldNotification.read && newNotification.read) {
                        this.unreadCount = Math.max(0, this.unreadCount - 1);
                    } else if (oldNotification.read && !newNotification.read) {
                        this.unreadCount++;
                    }

                    // Xóa notification cũ
                    this.notifications.splice(oldNotificationIndex, 1);
                    console.log('Removed old notification with key:', notificationKey);
                }
            } else {
                // Notification mới, tăng unreadCount nếu chưa đọc
                if (!newNotification.read) {
                    this.unreadCount++;
                }
            }

            // Thêm notification mới vào đầu danh sách
            this.notifications.unshift(newNotification);
            this.notificationsByKey.set(notificationKey, newNotification);

            console.log('Added/Updated notification with key:', notificationKey);

            // Trigger UI update
            if (this.updateTimer) {
                clearTimeout(this.updateTimer);
            }

            this.updateTimer = setTimeout(() => {
                this.batchUpdateUI();
            }, 100);

        } catch (error) {
            this.handleError(error, 'addOrUpdateNotification');
        }
    }

    handleCommentNotification(notification) {
        try {
            console.log('Handling comment notification:', notification);

            const shouldAddToNotificationList = !(notification.user && notification.user.userId === this.userId);

            if (shouldAddToNotificationList) {
                const newNotification = {
                    id: notification.id || Date.now(),
                    type: notification.type,
                    title: this.getNotificationTitle(notification),
                    message: this.getNotificationMessage(notification),
                    avatar: notification.user?.profileImage || 'default.png',
                    productId: notification.productId,
                    timestamp: new Date(),
                    read: false,
                    notificationKey: notification.notificationKey || this.createNotificationKey(notification),
                    serverId: notification.id
                };

                this.addOrUpdateNotification(newNotification);
                this.showToastNotification(notification);
            }

            const isOnCommentPage = this.isOnCommentPage(notification.productId);
            if (isOnCommentPage) {
                this.addCommentToList(notification);
            }
        } catch (error) {
            this.handleError(error, 'handleCommentNotification');
        }
    }

    addCommentToList(notification) {
        try {
            if (notification.parentCommentId && notification.parentCommentId !== 0) {
                this.addReplyToComment(notification);
            } else {
                this.addNewComment(notification);
            }
        } catch (error) {
            this.handleError(error, 'addCommentToList');
        }
    }

    addReplyToComment(notification) {
        let repliesContainer = document.getElementById(`replies-container-${notification.parentCommentId}`);
        let repliesDiv = document.getElementById(`replies-${notification.parentCommentId}`);

        if (!repliesContainer) {
            this.createEmptyRepliesContainer(notification.parentCommentId);
            repliesContainer = document.getElementById(`replies-container-${notification.parentCommentId}`);
            repliesDiv = document.getElementById(`replies-${notification.parentCommentId}`);
        } else {
            this.showRepliesContainer(repliesContainer, notification.parentCommentId);
        }

        if (repliesDiv) {
            const replyHTML = this.createReplyHTML(notification);
            repliesDiv.insertAdjacentHTML('beforeend', replyHTML);
            this.highlightNewElement(repliesDiv.lastElementChild);
            this.updateReplyCount(notification.parentCommentId);
        }
    }

    createEmptyRepliesContainer(parentCommentId) {
        const parentComment = document.querySelector(`[data-comment-id="${parentCommentId}"]`)?.closest('.comment-item');

        if (parentComment) {
            // Kiểm tra user có đăng nhập không
            const replyInputSection = this.userId ? `
            <div class="mt-2">
                <div class="reply-container">
                    <input type="text" class="comment-input reply-input"
                           id="reply-input-bottom-${parentCommentId}"
                           placeholder="Viết phản hồi...">
                    <button class="send-button reply-send-button"
                            id="reply-send-bottom-${parentCommentId}"
                            data-comment-id="${parentCommentId}"
                            disabled>
                        <i class="ti ti-send"></i>
                    </button>
                </div>
            </div>
        ` : '';

            const repliesHTML = `
            <div class="replies-container ms-4 mt-2" id="replies-container-${parentCommentId}" style="display: block;">
                <div class="replies" id="replies-${parentCommentId}">
                    <!-- Replies sẽ được thêm vào đây -->
                </div>
                ${replyInputSection}
            </div>
        `;

            const commentContent = parentComment.querySelector('.comment-content');
            commentContent.insertAdjacentHTML('beforeend', repliesHTML);
            this.createToggleButton(parentComment, parentCommentId);
        }
    }

    addNewComment(notification) {
        const commentsList = document.querySelector('.comment-list');
        if (!commentsList) {
            console.log('Comment list not found');
            return;
        }

        const commentHTML = this.createCommentHTML(notification);
        commentsList.insertAdjacentHTML('afterbegin', commentHTML);
        this.highlightNewElement(commentsList.firstElementChild);
    }

    createRepliesContainer(notification) {
        const parentComment = document.querySelector(`[data-comment-id="${notification.parentCommentId}"]`)?.closest('.comment-item');

        if (parentComment) {
            const repliesHTML = `
                <div class="replies-container ms-4 mt-2" id="replies-container-${notification.parentCommentId}" style="display: block;">
                    <div class="replies" id="replies-${notification.parentCommentId}">
                        ${this.createReplyHTML(notification)}
                    </div>
                    <div class="mt-2">
                        <div class="reply-container">
                            <input type="text" class="comment-input reply-input"
                                   id="reply-input-bottom-${notification.parentCommentId}"
                                   placeholder="Viết phản hồi...">
                            <button class="send-button reply-send-button"
                                    id="reply-send-bottom-${notification.parentCommentId}"
                                    data-comment-id="${notification.parentCommentId}"
                                    disabled>
                                <i class="ti ti-send"></i>
                            </button>
                        </div>
                    </div>
                </div>
            `;

            const commentContent = parentComment.querySelector('.comment-content');
            commentContent.insertAdjacentHTML('beforeend', repliesHTML);
            this.createToggleButton(parentComment, notification.parentCommentId);
        }
    }

    showRepliesContainer(repliesContainer, parentCommentId) {
        if (repliesContainer.style.display === 'none') {
            repliesContainer.style.display = 'block';
            this.updateToggleButton(parentCommentId, true);
        }
    }

    createToggleButton(parentComment, commentId) {
        const commentActions = parentComment.querySelector('.comment-actions');
        if (commentActions && !commentActions.querySelector('.toggle-replies-button')) {
            commentActions.innerHTML = `
                <button class="btn btn-sm text-muted toggle-replies-button fs-3"
                        data-comment-id="${commentId}"
                        data-reply-count="0">
                    <i class="ti ti-chevron-up"></i>
                    <span>Ẩn phản hồi</span>
                </button>
            `;
        }
    }

    updateToggleButton(commentId, isVisible) {
        const toggleButton = document.querySelector(`[data-comment-id="${commentId}"] .toggle-replies-button`);
        if (toggleButton) {
            const icon = toggleButton.querySelector('i');
            const text = toggleButton.querySelector('span');
            if (isVisible) {
                if (icon) icon.className = 'ti ti-chevron-up';
                if (text) text.textContent = 'Ẩn phản hồi';
            } else {
                if (icon) icon.className = 'ti ti-chevron-down';
                const count = toggleButton.getAttribute('data-reply-count') || '0';
                if (text) text.textContent = `Xem ${count} phản hồi`;
            }
        }
    }

    updateReplyCount(parentCommentId) {
        const parentComment = document.querySelector(`[data-comment-id="${parentCommentId}"]`)?.closest('.comment-item');
        if (parentComment) {
            const toggleButton = parentComment.querySelector('.toggle-replies-button');
            if (toggleButton) {
                const currentCount = parseInt(toggleButton.getAttribute('data-reply-count') || '0');
                const newCount = currentCount + 1;
                toggleButton.setAttribute('data-reply-count', newCount);
            }
        }
    }

    highlightNewElement(element) {
        if (element) {
            element.style.backgroundColor = '#e3f2fd';
            element.style.transition = 'background-color 0.3s';

            setTimeout(() => {
                element.style.backgroundColor = '';
            }, 3000);

            element.scrollIntoView({
                behavior: 'smooth',
                block: 'nearest'
            });
        }
    }

    createCommentHTML(notification) {
        const now = new Date(notification.createdAt);
        const timeStr = this.formatTime(now);

        return `
            <div class="comment-item" style="animation: fadeIn 0.5s ease-in;">
                <img src="/img/avatar/${notification.user?.profileImage || 'default.png'}"
                     alt="Avatar" class="comment-avatar"
                     onerror="this.src='/img/avatar/default.png'">
                <div class="comment-content">
                    <div class="comment-user">${notification.user?.fullName || 'Người dùng'}</div>
                    <div class="comment-text">${notification.content || ''}</div>
                    <div class="comment-date">${timeStr}</div>

                    <div class="comment-actions mt-2">
                        <button class="btn btn-sm btn-link text-muted reply-button fs-3"
                                data-comment-id="${notification.id}"
                                data-comment-user="${notification.user?.fullName || 'Người dùng'}">
                            <i class="ti ti-message-circle"></i> Trả lời
                        </button>
                    </div>

                    <div class="reply-input-container" id="reply-container-${notification.id}" style="display: none;">
                        <div class="reply-container">
                            <input type="text" class="comment-input reply-input"
                                   id="reply-input-${notification.id}"
                                   placeholder="Viết phản hồi...">
                            <button class="send-button reply-send-button"
                                    id="reply-send-${notification.id}"
                                    data-comment-id="${notification.id}"
                                    disabled>
                                <i class="ti ti-send"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    createReplyHTML(notification) {
        const now = new Date(notification.createdAt);
        const timeStr = this.formatTime(now);

        return `
            <div class="comment-item reply-item" style="animation: fadeIn 0.5s ease-in;">
                <img src="/img/avatar/${notification.user?.profileImage || 'default.png'}"
                     alt="Avatar" class="comment-avatar"
                     onerror="this.src='/img/avatar/default.png'">
                <div class="comment-content">
                    <div class="comment-user">${notification.user?.fullName || 'Người dùng'}</div>
                    <div class="comment-text">${notification.content || ''}</div>
                    <div class="comment-date">${timeStr}</div>
                </div>
            </div>
        `;
    }

    isOnCommentPage(productId) {
        const urlPath = window.location.pathname + window.location.search;
        const isProductPage = urlPath.includes(`/product:${productId}`);
        const isCommentView = urlPath.includes('/comments');
        return isProductPage && isCommentView;
    }

    getNotificationTitle(notification) {
        const titles = {
            'new_comment': 'Bình luận mới',
            'new_reply': 'Phản hồi mới',
            'new_order': 'Đơn hàng mới',
            'order_status': 'Cập nhật đơn hàng',
            'new_product_approval': 'Sản phẩm cần xác nhận',
            'product_approved': 'Sản phẩm được phê duyệt',
            'product_rejected': 'Sản phẩm bị từ chối'
        };
        return titles[notification.type] || 'Thông báo';
    }

    getNotificationMessage(notification) {
        const messages = {
            'new_comment': `${notification.user?.fullName || 'Người dùng'} đã bình luận về sản phẩm
            <br>"${this.truncateText(notification.content, 50)}"
            <br>Product ID: ${notification.productId || 'Không xác định'}`,
            'new_reply': `${notification.user?.fullName || 'Người dùng'} đã trả lời bình luận của bạn
            <br>"${this.truncateText(notification.content, 50)}"
            <br>Product ID: ${notification.productId || 'Không xác định'}`,
            'new_order': `Đơn hàng mới từ ${notification.user?.fullName || 'Người dùng'}`,
            'order_status': 'Trạng thái đơn hàng đã được cập nhật',
            'new_product_approval': `${notification.user?.fullName || 'Người dùng'} cần xác nhận sản phẩm mới
            <br>"${this.truncateText(notification.content, 50)}"
            <br>Product ID: ${notification.productId || 'Không xác định'}`,
            'product_approved': `${notification.content}`,
            'product_rejected': `${notification.content}`
        };
        return messages[notification.type] || notification.message || 'Bạn có thông báo mới';
    }

    truncateText(text, maxLength = 50) {
        if (!text) return '';
        return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
    }

    // Phương thức cũ không còn sử dụng, thay thế bằng addOrUpdateNotification
    addNotification(notification) {
        console.warn('addNotification is deprecated, use addOrUpdateNotification instead');
        this.addOrUpdateNotification(notification);
    }

    batchUpdateUI() {
        try {
            if (this.notifications.length > this.maxNotifications) {
                // Xóa các notification cũ nhất và cập nhật Map
                const removedNotifications = this.notifications.splice(this.maxNotifications);
                removedNotifications.forEach(notification => {
                    if (notification.notificationKey) {
                        this.notificationsByKey.delete(notification.notificationKey);
                    }
                });
            }

            this.updateBadge();
            this.renderNotifications();
            this.saveNotificationsToStorage();
        } catch (error) {
            this.handleError(error, 'batchUpdateUI');
        }
    }

    updateBadge() {
        if (!this.notificationBadge) return;

        try {
            if (this.unreadCount > 0) {
                this.notificationBadge.textContent = this.unreadCount > 99 ? '99+' : this.unreadCount;
                this.notificationBadge.style.display = 'inline-block';
                this.notificationBadge.classList.add('notification-badge');
            } else {
                this.notificationBadge.style.display = 'none';
                this.notificationBadge.classList.remove('notification-badge');
            }
        } catch (error) {
            this.handleError(error, 'updateBadge');
        }
    }

    renderNotifications() {
        if (!this.notificationList || !this.noNotifications) return;

        try {
            if (this.notifications.length === 0) {
                this.noNotifications.style.display = 'block';
                this.notificationList.innerHTML = '';
                return;
            }

            this.noNotifications.style.display = 'none';
            this.notificationList.innerHTML = this.notifications.map(notification =>
                this.createNotificationHTML(notification)
            ).join('');

            this.bindNotificationEvents();
        } catch (error) {
            this.handleError(error, 'renderNotifications');
        }
    }

    bindNotificationEvents() {
        this.notificationList.querySelectorAll('.notification-item').forEach((item, index) => {
            item.addEventListener('click', () => {
                const notificationKey = item.getAttribute('data-notification-key');
                this.handleNotificationClick(index, notificationKey);
            });
        });
    }

    createNotificationHTML(notification) {
        const timeStr = this.formatTime(notification.timestamp);
        const unreadClass = notification.read ? '' : 'unread';

        return `
            <li class="notification-item ${unreadClass}" data-notification-key="${notification.notificationKey || ''}">
                <div class="notification-content">
                    <img src="/img/avatar/${notification.avatar}"
                         alt="Avatar" class="notification-avatar"
                         onerror="this.src='/img/avatar/default.png'">
                    <div class="flex-grow-1">
                        <div class="notification-text">
                            <strong>${notification.title}</strong><br>
                            <small class="text-muted">${notification.message}</small>
                        </div>
                        <div class="notification-time">${timeStr}</div>
                    </div>
                    ${!notification.read ? '<div class="text-primary"><i class="fa-solid fa-circle" style="font-size: 8px;"></i></div>' : ''}
                </div>
            </li>
        `;
    }

    handleNotificationClick(index, notificationKey) {
        try {
            const notification = this.notifications[index];
            this.markAsRead(index, notificationKey);

            if (notification.productId) {

                if (notification.type === 'new_comment' || notification.type === 'new_reply') {
                    window.location.href = `/product:${notification.productId}/comments`;
                } else if (notification.type === 'new_product_approval') {
                    window.location.href = `/admin/product-of-user/product:${notification.productId}`;
                } else if (notification.type === 'product_approved' || notification.type === 'product_rejected') {
                    window.location.href = `/user/edit-product:${notification.productId}`;
                }

            }
        } catch (error) {
            this.handleError(error, 'handleNotificationClick');
        }
    }

    showToastNotification(notification) {
        try {
            const toast = document.createElement('div');
            toast.className = 'toast-notification';
            toast.innerHTML = `
                <div class="toast show" role="alert" style="background: white; border: 1px solid #dee2e6; position: fixed; top: 20px; right: 20px; z-index: 9999; min-width: 300px;">
                    <div class="toast-header">
                        <img src="/img/avatar/${notification.user?.profileImage || 'default.png'}"
                             class="toast-avatar me-2" alt="Avatar" style="width: 32px; height: 32px; border-radius: 50%;"
                             onerror="this.src='/img/avatar/default.png'">
                        <strong class="me-auto">${this.getNotificationTitle(notification)}</strong>
                        <small class="text-muted">Vừa xong</small>
                        <button type="button" class="btn-close" aria-label="Close"></button>
                    </div>
                    <div class="toast-body">
                        ${this.getNotificationMessage(notification)}
                    </div>
                </div>
            `;

            document.body.appendChild(toast);

            setTimeout(() => {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 5000);

            const closeBtn = toast.querySelector('.btn-close');
            if (closeBtn) {
                closeBtn.addEventListener('click', () => {
                    if (toast.parentNode) {
                        toast.parentNode.removeChild(toast);
                    }
                });
            }
        } catch (error) {
            this.handleError(error, 'showToastNotification');
        }
    }

    formatTime(timestamp) {
        try {
            const time = new Date(timestamp);
            const hours = String(time.getHours()).padStart(2, '0');
            const minutes = String(time.getMinutes()).padStart(2, '0');
            const day = String(time.getDate()).padStart(2, '0');
            const month = String(time.getMonth() + 1).padStart(2, '0');
            const year = time.getFullYear();

            return `${hours}:${minutes} - ${day}/${month}/${year}`;
        } catch (error) {
            this.handleError(error, 'formatTime');
            return 'Invalid time';
        }
    }

    saveNotificationsToStorage() {
        try {
            const data = {
                notifications: this.notifications,
                unreadCount: this.unreadCount,
                timestamp: Date.now()
            };
            localStorage.setItem('notifications', JSON.stringify(data));
        } catch (error) {
            this.handleError(error, 'saveNotificationsToStorage');
        }
    }

    loadNotificationsFromStorage() {
        try {
            const stored = localStorage.getItem('notifications');
            if (stored) {
                const data = JSON.parse(stored);

                const maxAge = 24 * 60 * 60 * 1000;
                if (data.timestamp && (Date.now() - data.timestamp) > maxAge) {
                    localStorage.removeItem('notifications');
                    return;
                }

                this.notifications = data.notifications || [];
                this.unreadCount = data.unreadCount || 0;

                // Rebuild notificationsByKey Map
                this.notificationsByKey.clear();
                this.notifications.forEach(notification => {
                    notification.timestamp = new Date(notification.timestamp);
                    if (notification.notificationKey) {
                        this.notificationsByKey.set(notification.notificationKey, notification);
                    }
                });

                this.batchUpdateUI();
                console.log('Loaded notifications from storage:', this.notifications.length);
            }
        } catch (error) {
            this.handleError(error, 'loadNotificationsFromStorage');
            localStorage.removeItem('notifications');
        }
    }

    async loadNotificationsFromServer() {
        if (!this.userId) return;

        try {
            const response = await fetch('/api/notifications', {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                const data = await response.json();

                // Clear existing notifications and rebuild with server data
                this.notifications = [];
                this.notificationsByKey.clear();

                this.notifications = data.notifications.map(n => ({
                    type: n.type,
                    message: this.getNotificationMessage(n),
                    productId: n.productId,
                    timestamp: new Date(n.createdAt),
                    avatar: n.senderAvatar || 'default.png',
                    title: this.getNotificationTitle(n),
                    read: n.isRead,
                    serverId: n.id,
                    notificationKey: n.notificationKey || `notification_${n.id}`
                }));

                // Rebuild notificationsByKey Map
                this.notifications.forEach(notification => {
                    if (notification.notificationKey) {
                        this.notificationsByKey.set(notification.notificationKey, notification);
                    }
                });

                this.unreadCount = data.unreadCount;
                this.batchUpdateUI();
                console.log('Loaded notifications from server:', this.notifications.length);
            }
        } catch (error) {
            this.handleError(error, 'loadNotificationsFromServer');
        }
    }

    async markAsRead(notificationIndex, notificationKey) {
        try {
            const notification = this.notifications[notificationIndex];

            if (notification.serverId && !notification.read) {
                await fetch(`/api/notifications/mark-read/${notificationKey}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });
            }
        } catch (error) {
            this.handleError(error, 'markAsRead');
        }
    }

    async markAllAsRead() {
        try {
            const response = await fetch('/api/notifications/mark-all-read', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                this.notifications.forEach(notification => {
                    notification.read = true;
                    // Update in notificationsByKey Map
                    if (notification.notificationKey) {
                        this.notificationsByKey.set(notification.notificationKey, notification);
                    }
                });
                this.unreadCount = 0;
                this.batchUpdateUI();
            }
        } catch (error) {
            this.handleError(error, 'markAllAsRead');
        }
    }

    handleError(error, context) {
        console.error(`NotificationManager Error in ${context}:`, error);
        this.metrics.errorsOccurred++;

        if (window.errorLogger) {
            window.errorLogger.log('notification', error, context);
        }
    }

    logMetrics() {
        console.log('NotificationManager Metrics:', this.metrics);
        console.log('Notifications by key count:', this.notificationsByKey.size);
        console.log('Notifications array length:', this.notifications.length);
    }

    destroy() {
        try {
            if (this.stompClient && this.stompClient.connected) {
                this.stompClient.disconnect();
            }

            if (this.updateTimer) {
                clearTimeout(this.updateTimer);
            }

            this.processedNotifications.clear();
            this.notificationsByKey.clear();
            console.log('NotificationManager destroyed');
        } catch (error) {
            this.handleError(error, 'destroy');
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM loaded, initializing key-based NotificationManager...');
    if (typeof window.notificationManager === 'undefined') {
        window.notificationManager = new NotificationManager();
    }
});

window.addEventListener('beforeunload', () => {
    if (window.notificationManager) {
        window.notificationManager.destroy();
    }
});