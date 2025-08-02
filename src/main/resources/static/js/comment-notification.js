// comment-notification.js
class CommentNotificationService {
    constructor(productId, userId) {
        this.productId = productId;
        this.userId = userId;
        this.stompClient = null;
        this.connect();
    }
    
    connect() {
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        
        this.stompClient.connect({}, (frame) => {
            console.log('Connected: ' + frame);
            
            // Lắng nghe comment mới cho sản phẩm này
            this.stompClient.subscribe('/topic/product/' + this.productId, (message) => {
                const notification = JSON.parse(message.body);
                this.handleNewComment(notification);
            });
            
            // Lắng nghe thông báo cá nhân
            this.stompClient.subscribe('/user/queue/notifications', (message) => {
                const notification = JSON.parse(message.body);
                this.handlePersonalNotification(notification);
            });
        });
    }
    
    handleNewComment(notification) {
        // Kiểm tra xem comment có phải của user hiện tại không
        if (notification.user.userId === this.userId) {
            return; // Không hiển thị comment của chính mình
        }
        
        // Cập nhật UI với comment mới
        this.addCommentToUI(notification.comment);
        
        // Hiển thị thông báo
        this.showNotification(notification.user.fullName + ' đã thêm comment mới');
    }
    
    handlePersonalNotification(notification) {
        // Hiển thị thông báo đặc biệt cho chủ sở hữu sản phẩm
        this.showNotification('Có comment mới trên sản phẩm của bạn: ' + notification.user.fullName);
        
        // Cập nhật badge thông báo
        this.updateNotificationBadge();
    }
    
    addCommentToUI(comment) {
        // Thêm comment mới vào đầu danh sách
        const commentElement = this.createCommentElement(comment);
        const commentsContainer = document.getElementById('comments-container');
        
        if (commentsContainer) {
            // Thêm animation
            commentElement.style.opacity = '0';
            commentElement.style.transform = 'translateY(-20px)';
            
            commentsContainer.insertBefore(commentElement, commentsContainer.firstChild);
            
            // Animate in
            setTimeout(() => {
                commentElement.style.transition = 'all 0.3s ease';
                commentElement.style.opacity = '1';
                commentElement.style.transform = 'translateY(0)';
            }, 100);
        }
    }
    
    showNotification(message) {
        // Toast notification
        this.showToast(message);
        
        // Browser notification nếu được phép
        if (Notification.permission === 'granted') {
            new Notification('PrintWebsite - Thông báo mới', {
                body: message,
                icon: '/images/logo-small.png'
            });
        }
    }
    
    showToast(message) {
        // Tạo toast element
        const toast = document.createElement('div');
        toast.className = 'notification-toast';
        toast.innerHTML = `
            <div class="toast-content">
                <i class="fas fa-comment"></i>
                <span>${message}</span>
            </div>
        `;
        
        document.body.appendChild(toast);
        
        // Show animation
        setTimeout(() => toast.classList.add('show'), 100);
        
        // Hide after 3 seconds
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => document.body.removeChild(toast), 300);
        }, 3000);
    }
    
    createCommentElement(comment) {
        const div = document.createElement('div');
        div.className = 'comment-item new-comment';
        div.setAttribute('data-comment-id', comment.id);
        
        div.innerHTML = `
            <div class="comment-header">
                <div class="user-info">
                    <strong>${comment.userAccount.fullName}</strong>
                    <span class="comment-time">${this.formatTime(comment.createdAt)}</span>
                </div>
            </div>
            <div class="comment-content">${this.escapeHtml(comment.content)}</div>
            <div class="comment-actions">
                <button class="btn-reply" onclick="showReplyForm(${comment.id})">
                    <i class="fas fa-reply"></i> Trả lời
                </button>
            </div>
        `;
        
        return div;
    }
    
    formatTime(dateTime) {
        const date = new Date(dateTime);
        const now = new Date();
        const diff = now - date;
        
        if (diff < 60000) { // < 1 minute
            return 'Vừa xong';
        } else if (diff < 3600000) { // < 1 hour
            return Math.floor(diff / 60000) + ' phút trước';
        } else if (diff < 86400000) { // < 1 day
            return Math.floor(diff / 3600000) + ' giờ trước';
        } else {
            return date.toLocaleDateString('vi-VN');
        }
    }
    
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    updateNotificationBadge() {
        const badge = document.getElementById('notification-badge');
        if (badge) {
            const count = parseInt(badge.textContent || '0') + 1;
            badge.textContent = count;
            badge.style.display = 'inline';
        }
    }
    
    disconnect() {
        if (this.stompClient !== null) {
            this.stompClient.disconnect();
        }
    }
}

// Export để sử dụng ở file khác
window.CommentNotificationService = CommentNotificationService;