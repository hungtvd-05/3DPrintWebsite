package com.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.controller.AdminController;
import com.web.model.*;
import com.web.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api")
//@RequiredArgsConstructor
public class ApiController {

    String path = System.getProperty("user.dir") + File.separator
            + "src" + File.separator + "main" + File.separator + "resources"
            + File.separator + "static" + File.separator;

    String imgPath = System.getProperty("user.dir") + File.separator
            + "src" + File.separator + "main" + File.separator + "resources"
            + File.separator + "static" + File.separator + "img";

    @Autowired
    private WebInfoService webInfoService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CartService cartService;

    @PostMapping("/addContact")
    public ResponseEntity<String> addContact(
            @RequestParam("name") String name,
            @RequestParam("url") String url) {

        webInfoService.addContactInfo(name, url);

        return ResponseEntity.ok("Contact added successfully");
    }

    @GetMapping("/deleteContactUrl")
    public ResponseEntity<String> deleteContactUrl(@RequestParam("key") String key) {

        webInfoService.deleteContactInfo(key);

        return ResponseEntity.ok("Contact added successfully");
    }

    @GetMapping("/delete-product")
    public ResponseEntity<String> deleteProduct(@RequestParam long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok("Product deleted successfully");
    }

    @GetMapping("/favorite")
    public ResponseEntity<String> favoriteProduct(@RequestParam Long productId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        UserAccount currentUser = userService.getUserAccountByEmail(email);

        if (currentUser == null) {
            return ResponseEntity.ok("User not found");
        }

        if (currentUser.getFavoriteProducts().contains(productId)) {
            currentUser.getFavoriteProducts().remove(productId);
            userService.updateUserAccount(currentUser);
            return ResponseEntity.ok("removed");
        } else {
            currentUser.getFavoriteProducts().add(productId);
            userService.updateUserAccount(currentUser);
        }

        return ResponseEntity.ok("added");
    }

    @PostMapping("/add-comment")
    public ResponseEntity<String> addComment(@RequestParam("content") String content,
                                             @RequestParam("productId") Long productId,
                                             @RequestParam(value = "parentCommentId", required = false) Long parentCommentId) {

        UserAccount currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(403).body("User not found");
        }

        Product product = productService.getProductById(productId);

        Map<String, Object> notification = new HashMap<>();
        notification.put("contentId", product.getId());
        notification.put("content", content);
        notification.put("parentCommentId", parentCommentId);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", currentUser.getUserId());
        userInfo.put("fullName", currentUser.getFullName());
        userInfo.put("profileImage", currentUser.getProfileImage());
        notification.put("user", userInfo);

        Comment savedComment;

        UserAccount productOwner = product.getCreatedBy();

        if (parentCommentId != null) {
            Comment parentComment = commentService.findById(parentCommentId);
            savedComment = commentService.addReply(parentComment, content, currentUser);

            notification.put("id", savedComment.getId());
            notification.put("createdAt", savedComment.getCreatedAt().toString());

            String notificationKey = String.format("%s_%d_%d_%d", "new_reply", productOwner.getUserId(), currentUser.getUserId(), product.getId());
            notification.put("type", "new_comment");
            notification.put("notificationKey", notificationKey);

            messagingTemplate.convertAndSend("/topic/product/" + product.getId(), notification);

            if (!productOwner.getUserId().equals(currentUser.getUserId()) && !productOwner.getUserId().equals(parentComment.getUserAccount().getUserId())) {
                notificationService.saveAndSendNotification(
                        productOwner,
                        "new_comment",
                        content,
                        product,
                        currentUser,
                        savedComment.getCreatedAt(),
                        notificationKey
                );

                messagingTemplate.convertAndSendToUser(
                        String.valueOf(productOwner.getUserId()),
                        "/queue/notifications",
                        notification
                );
            }

            if (!parentComment.getUserAccount().getUserId().equals(currentUser.getUserId())) {

                notificationKey = String.format("%s_%d_%d_%d", "new_reply", parentComment.getUserAccount().getUserId(), currentUser.getUserId(), product.getId());

                notificationService.saveAndSendNotification(
                        parentComment.getUserAccount(),
                        "new_reply",
                        content,
                        product,
                        currentUser,
                        savedComment.getCreatedAt(),
                        notificationKey
                );

                notification.put("type", "new_reply");
                notification.put("notificationKey", notificationKey);

                messagingTemplate.convertAndSendToUser(
                        String.valueOf(parentComment.getUserAccount().getUserId()),
                        "/queue/notifications",
                        notification
                );
            }

        } else {
            savedComment = commentService.addComment(content, product, currentUser);
            String notificationKey = String.format("%s_%d_%d_%d", "new_comment", productOwner.getUserId(), currentUser.getUserId(), product.getId());

            notification.put("id", savedComment.getId());
            notification.put("createdAt", savedComment.getCreatedAt().toString());
            notification.put("type", "new_comment");
            notification.put("notificationKey", notificationKey);

            messagingTemplate.convertAndSend("/topic/product/" + product.getId(), notification);

            if (!productOwner.getUserId().equals(currentUser.getUserId())) {

                notificationService.saveAndSendNotification(
                        productOwner,
                        "new_comment",
                        content,
                        product,
                        currentUser,
                        savedComment.getCreatedAt(),
                        notificationKey
                );

                messagingTemplate.convertAndSendToUser(
                        String.valueOf(productOwner.getUserId()),
                        "/queue/notifications",
                        notification
                );

            }


        }

        return ResponseEntity.ok("ok");
    }

    @GetMapping("/notifications")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotifications(HttpServletRequest request) {
        try {
            UserAccount user = getCurrentUser();

            List<Notification> notifications = notificationService.getUserNotifications(user);
            long unreadCount = notificationService.getUnreadCount(user);

            List<Map<String, Object>> notificationList = notifications.stream()
                    .map(n -> {
                        Map<String, Object> notif = new HashMap<>();
                        notif.put("id", n.getId());
                        notif.put("type", n.getType());
                        notif.put("content", n.getContent());
                        notif.put("contentId", n.getContentId());
                        notif.put("createdAt", n.getCreatedAt().toString());
                        notif.put("notificationKey", n.getNotificationKey());

                        Map<String, Object> userInfo = new HashMap<>();
                        userInfo.put("userId", n.getSenderId());
                        userInfo.put("fullName", n.getSenderName());
                        userInfo.put("profileImage", n.getSenderAvatar());
                        notif.put("user", userInfo);

                        notif.put("isRead", n.getIsRead());
                        return notif;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("notifications", notificationList);
            response.put("unreadCount", unreadCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/notifications/mark-read/{notificationKey}")
    @ResponseBody
    public ResponseEntity<String> markNotificationAsRead(@PathVariable String notificationKey, HttpServletRequest request) {
        try {
            notificationService.markAsRead(notificationKey);
            return ResponseEntity.ok("Marked as read");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error marking as read");
        }
    }

    @PostMapping("/notifications/mark-all-read")
    @ResponseBody
    public ResponseEntity<String> markAllNotificationsAsRead(HttpServletRequest request) {
        try {
            System.out.println("Marking all notifications as read");

            UserAccount user = getCurrentUser();

            System.out.println(user);
            notificationService.markAllAsRead(user);
            return ResponseEntity.ok("All marked as read");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error marking all as read");
        }
    }

    private UserAccount getCurrentUser() {

        // Get the current authentication from SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        // Get username from authentication
        String email = authentication.getName();

        try {
            // Find user by username (assuming you have a userService or userRepository)
            return userService.getUserAccountByEmail(email);
        } catch (Exception e) {
            // Log the error if needed
            return null;
        }
    }

    @GetMapping("/{id}/download-all-stl")
    public ResponseEntity<Resource> downloadAllStlFiles(@PathVariable Long id) {
        try {
            Product product = productService.getProductById(id);
            if (product == null) {
                return ResponseEntity.notFound().build();
            }

            if (product.getStlFiles().isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {

                int filesAdded = 0;
                for (String fileName : product.getStlFiles().keySet()) {
                    String filePath = path + "product" + File.separator + "stl" + File.separator + fileName;
                    File stlFile = new File(filePath);

                    if (stlFile.exists() && stlFile.isFile()) {
                        try {
                            ZipEntry zipEntry = new ZipEntry(fileName);
                            zos.putNextEntry(zipEntry);

                            try (FileInputStream fis = new FileInputStream(stlFile)) {
                                byte[] buffer = new byte[8192];
                                int length;
                                while ((length = fis.read(buffer)) > 0) {
                                    zos.write(buffer, 0, length);
                                }
                            }

                            zos.closeEntry();
                            filesAdded++;
                        } catch (Exception e) {
                            System.err.println("Error adding file " + fileName + " to ZIP: " + e.getMessage());
                        }
                    }
                }

                if (filesAdded == 0) {
                    return ResponseEntity.noContent().build();
                }
            }

            byte[] zipData = baos.toByteArray();

            if (zipData.length == 0) {
                return ResponseEntity.noContent().build();
            }

            ByteArrayResource resource = new ByteArrayResource(zipData);

            // Generate filename - cải thiện sanitization
            String productName = product.getName() != null ? product.getName() : "product";
            String zipFilename = "stl-files-" + productName
                    .replaceAll("[^a-zA-Z0-9\\-_]", "-")
                    .replaceAll("-+", "-")
                    .trim() + ".zip";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFilename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(zipData.length))
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .body(resource);

        } catch (Exception e) {
            // Log chi tiết lỗi
            System.err.println("Error creating ZIP file for product " + id + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/save-image")
    public ResponseEntity<?> adminsaveImage(@RequestParam("file") MultipartFile file) {
        try {
            String originalFileName = file.getOriginalFilename();
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));

            String random_name = UUID.randomUUID().toString();

            String newFileName = String.format("img_%s_%d%s",
                    random_name,
                    System.nanoTime(),
                    extension
            );

            // Lưu file với tên mới
            Files.copy(file.getInputStream(),
                    Paths.get(path + "tmp" + File.separator + "img", newFileName),
                    StandardCopyOption.REPLACE_EXISTING);

            // Trả về response với tên file
            return ResponseEntity.ok().body(new ApiController.UploadResponse(true, newFileName));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiController.UploadResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/delete-image")
    public ResponseEntity<?> deleteImage(@RequestParam("fileName") String fileName) {
        try {
            String filePath = path + "tmp" + File.separator + "img" + File.separator + fileName;
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }

            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiController.UploadResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/save-stl")
    public ResponseEntity<?> saveStl(@RequestParam("file") MultipartFile file) {
        try {
            String originalFileName = file.getOriginalFilename();
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));

            String random_name = UUID.randomUUID().toString();

            long time_upload = System.nanoTime();

            String newFileName = String.format("stl_%s_%d%s",
                    random_name,
                    time_upload,
                    extension
            );

            // Lưu file với tên mới
            Files.copy(file.getInputStream(),
                    Paths.get(path + "tmp" + File.separator + "stl", newFileName),
                    StandardCopyOption.REPLACE_EXISTING);

            // Trả về response với tên file
            return ResponseEntity.ok().body(new ApiController.UploadResponse(true, newFileName));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiController.UploadResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/save-stl-preview")
    public ResponseEntity<?> saveStlPreview(@RequestParam("file") MultipartFile file,
                                            @RequestParam("stlFileName") String stlFileName) {
        try {
            String previewFileName = stlFileName + "_preview.png";

            Files.copy(file.getInputStream(),
                    Paths.get(path + "tmp" + File.separator + "stl", previewFileName),
                    StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok().body(new ApiController.UploadResponse(true, previewFileName));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiController.UploadResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/delete-stl")
    public ResponseEntity<?> deleteStl(@RequestParam("fileName") String fileName) {
        try {
            String filePath = path + "tmp" + File.separator + "stl" + File.separator + fileName;
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }

            String imgPath = path + "tmp" + File.separator + "stl" + File.separator + fileName + "_preview.png";
            File img = new File(imgPath);
            if (img.exists()) {
                img.delete();
            }

            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiController.UploadResponse(false, e.getMessage()));
        }
    }

    static class UploadResponse {
        private boolean success;
        private String filename;

        public UploadResponse(boolean success, String filename) {
            this.success = success;
            this.filename = filename;
        }

        // Getters and setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }
    }

    @PostMapping(value = "/save-product", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveProduct(@RequestParam("name") String name,
                                         @RequestParam(value = "price", required = false) Double price,
                                         @RequestParam("status") Boolean status,
                                         @RequestParam("description") String description,
                                         @RequestParam("isAcceptAdmin") Boolean isAcceptAdmin,
                                         @RequestParam("images") String images,
                                         @RequestParam("stlFiles") String stlFiles,
                                         @RequestParam("tags") String tagsJson,
                                         HttpSession session) {

        try {
            ObjectMapper mapper = new ObjectMapper();

            List<Tag> tags = mapper.readValue(tagsJson,
                    mapper.getTypeFactory().constructCollectionType(List.class, Tag.class));

            List<String> imageList = mapper.readValue(images,
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class));

            List<String> stlFileList = mapper.readValue(stlFiles,
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class));

            Product product = new Product();
            product.setName(name);

            if (!ObjectUtils.isEmpty(price)) {
                product.setPrice(price);
            }

            product.setStatus(status);

            String final_content = moveImagesFromTemp(description);

            product.setDescription(final_content);
            product.setTags(tags);
            product.setIsAcceptAdmin(isAcceptAdmin);

            LocalDateTime createdAt = LocalDateTime.now();

            product.setCreatedAt(createdAt);

            String sourcePathImg = path + "tmp" + File.separator + "img" + File.separator;
            String targetPathImg = path + "product" + File.separator + "img" + File.separator;

            for (String img : imageList) {
                Files.move(Paths.get(sourcePathImg + img), Paths.get(targetPathImg + img), StandardCopyOption.REPLACE_EXISTING);
                product.getImageFiles().add(img);
            }

            String sourcePathStl = path + "tmp" + File.separator + "stl" + File.separator;
            String targetPathStl = path + "product" + File.separator + "stl" + File.separator;


            for (String stl : stlFileList) {

                File stlFile = new File(sourcePathStl + stl);
                if (stlFile.exists()) {
                    long fileSize = stlFile.length();

                    Files.move(Paths.get(sourcePathStl + stl), Paths.get(targetPathStl + stl), StandardCopyOption.REPLACE_EXISTING);
                    Files.move(Paths.get(sourcePathStl + stl + "_preview.png"), Paths.get(targetPathStl + stl + "_preview.png"), StandardCopyOption.REPLACE_EXISTING);

                    product.getStlFiles().put(stl, new StlFileInfo(createdAt, fileSize));
                }

            }

            Product savedProduct = productService.saveProduct(product);

            if (!ObjectUtils.isEmpty(savedProduct)) {

                if (!product.getCreatedBy().getRole().equals("ROLE_ADMIN")) {
                    sendProductNotificationToAdmins(savedProduct);
                }

                session.setAttribute("succMsg", "Sản phẩm đã được lưu.");
            } else {
                session.setAttribute("errorMsg", "Lỗi");
            }

            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("errorMsg", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi server: " + e.getMessage());
        }

    }

    @PostMapping(value = "/update-product/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProduct(@PathVariable long id,
                                           @RequestParam("name") String name,
                                           @RequestParam(value = "price", required = false) Double price,
                                           @RequestParam("status") Boolean status,
                                           @RequestParam("description") String description,
                                           @RequestParam("isAcceptAdmin") Boolean isAcceptAdmin,
                                           @RequestParam("images") String images,
                                           @RequestParam("stlFiles") String stlFiles,
                                           @RequestParam("deleteImages") String deleteImages,
                                           @RequestParam("deleteStlFiles") String deleteStlFiles,
                                           @RequestParam("tags") String tagsJson,
//                                           @RequestParam("deleteTags") String deleteTags,
                                           HttpSession session) {

        try {

            Product product = productService.getProductById(id);

            System.out.println(product);

            if (ObjectUtils.isEmpty(product)) {
                session.setAttribute("errorMsg", "Lỗi");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy sản phẩm");
            }

            ObjectMapper mapper = new ObjectMapper();

            List<Tag> tags = mapper.readValue(tagsJson,
                    mapper.getTypeFactory().constructCollectionType(List.class, Tag.class));

            List<String> deleteImageList = mapper.readValue(deleteImages,
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class));

            List<String> deleteStlFileList = mapper.readValue(deleteStlFiles,
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class));

            List<String> imageList = mapper.readValue(images,
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class));

            List<String> stlFileList = mapper.readValue(stlFiles,
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class));

            String sourcePathImg = path + "tmp" + File.separator + "img" + File.separator;
            String targetPathImg = path + "product" + File.separator + "img" + File.separator;
            String sourcePathStl = path + "tmp" + File.separator + "stl" + File.separator;
            String targetPathStl = path + "product" + File.separator + "stl" + File.separator;

            for (String img : deleteImageList) {
                product.getImageFiles().remove(img);

                File file = new File(targetPathImg + img);
                if (file.exists()) {
                    file.delete();
                }
            }

            for (String stl : deleteStlFileList) {

                product.getStlFiles().remove(stl);

                File file = new File(targetPathStl + stl);
                if (file.exists()) {
                    file.delete();
                }
                File previewFile = new File(targetPathStl + stl + "_preview.png");
                if (previewFile.exists()) {
                    previewFile.delete();
                }

            }

            for (String img : imageList) {
                Files.move(Paths.get(sourcePathImg + img), Paths.get(targetPathImg + img), StandardCopyOption.REPLACE_EXISTING);
                product.getImageFiles().add(img);
            }

            LocalDateTime createdAt = LocalDateTime.now();

            for (String stl : stlFileList) {

                File stlFile = new File(sourcePathStl + stl);
                if (stlFile.exists()) {
                    long fileSize = stlFile.length();

                    Files.move(Paths.get(sourcePathStl + stl), Paths.get(targetPathStl + stl), StandardCopyOption.REPLACE_EXISTING);
                    Files.move(Paths.get(sourcePathStl + stl + "_preview.png"), Paths.get(targetPathStl + stl + "_preview.png"), StandardCopyOption.REPLACE_EXISTING);

                    product.getStlFiles().put(stl, new StlFileInfo(createdAt, fileSize));
                }

            }

            if (price != null) {
                product.setPrice(price);
            }

            product.setName(name);
            product.setStatus(status);

            List<String> oldImages = extractImagesFromContent(product.getDescription());
            List<String> newImages = extractImagesFromContent(description);

            for (String oldImage : oldImages) {
                if (!newImages.contains(oldImage)) {
                    String imagePath = path + "img" + File.separator + "ckeditor_img" + File.separator + oldImage;
                    File imageFile = new File(imagePath);
                    if (imageFile.exists()) {
                        imageFile.delete();
                    }
                }
            }

            String final_content = moveImagesFromTemp(description);

            product.setDescription(final_content);
            product.setIsAcceptAdmin(isAcceptAdmin);

            product.setTags(tags);
            product.setCreatedAt(createdAt);

            if (!ObjectUtils.isEmpty(productService.updateProduct(product))) {
                session.setAttribute("succMsg", "Sản phẩm đã được cập nhật.");
            } else {
                session.setAttribute("errorMsg", "Lỗi");
            }

            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("errorMsg", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi server: " + e.getMessage());
        }
    }

    private void sendProductNotificationToAdmins(Product product) {
        UserAccount admin = userService.getAdminAccount();

        String notificationKey = String.format("new_product_approval_%d_%d",
                product.getCreatedBy().getUserId(),
                product.getId());

        String message = String.format("Sản phẩm '%s' cần được xác nhận", product.getName());

        notificationService.saveAndSendNotification(
                admin,
                "new_product_approval",
                message,
                product,
                product.getCreatedBy(),
                LocalDateTime.now(),
                notificationKey + "_" + admin.getUserId() // Unique key for each admin
        );

        sendRealtimeNotificationToAdmin(admin, product, message, notificationKey);

    }

    private void sendRealtimeNotificationToAdmin(UserAccount admin, Product product,
                                                 String message, String notificationKey) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("id", System.currentTimeMillis()); // Temporary ID
            notification.put("type", "new_product_approval");
            notification.put("contentId", product.getId());
            notification.put("content", message);
            notification.put("createdAt", LocalDateTime.now().toString());
            notification.put("notificationKey", notificationKey + "_" + admin.getUserId());

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", product.getCreatedBy().getUserId());
            userInfo.put("fullName", product.getCreatedBy().getFullName());
            userInfo.put("profileImage", product.getCreatedBy().getProfileImage());
            notification.put("user", userInfo);

            // Send to specific admin user
            messagingTemplate.convertAndSendToUser(
                    admin.getUserId().toString(),
                    "/queue/notifications",
                    notification
            );
        } catch (Exception e) {
            System.err.println("Error sending real-time notification to admin: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @PostMapping("/upload/file")
    public ResponseEntity<?> uploadImageForCKEditor(@RequestParam("upload") MultipartFile file) {
        try {
            String originalFileName = file.getOriginalFilename();
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));

            String random_name = UUID.randomUUID().toString();

            String newFileName = String.format("img_%s_%d%s",
                    random_name,
                    System.nanoTime(),
                    extension
            );

            // Lưu file với tên mới
            Files.copy(file.getInputStream(),
                    Paths.get(path + "tmp" + File.separator + "img", newFileName),
                    StandardCopyOption.REPLACE_EXISTING);

            // CKEditor yêu cầu format response đặc biệt
            Map<String, Object> response = new HashMap<>();
            response.put("uploaded", true);
            response.put("url", "/tmp/img/" + newFileName); // URL để truy cập ảnh

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Format lỗi cho CKEditor
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("uploaded", false);
            Map<String, String> error = new HashMap<>();
            error.put("message", "Upload failed: " + e.getMessage());
            errorResponse.put("error", error);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    private String moveImagesFromTemp(String content) throws IOException {
        Pattern pattern = Pattern.compile("/tmp/img/([^\"\\s]+)");
        Matcher matcher = pattern.matcher(content);
        StringBuffer result = new StringBuffer();

        String finalDir = path + File.separator + "img" + File.separator + "ckeditor_img" + File.separator;
        Files.createDirectories(Paths.get(finalDir));

        while (matcher.find()) {
            String fileName = matcher.group(1);
            String tempPath = path + File.separator + "tmp" + File.separator + "img" + File.separator + fileName;
            String finalPath = finalDir + fileName;

            try {
                Files.move(Paths.get(tempPath), Paths.get(finalPath), StandardCopyOption.REPLACE_EXISTING);

                matcher.appendReplacement(result, "/img/ckeditor_img/" + fileName);
            } catch (IOException e) {
                // Giữ nguyên nếu có lỗi
                matcher.appendReplacement(result, matcher.group(0));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private List<String> extractImagesFromContent(String content) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> images = new ArrayList<>();

        // Pattern để tìm ảnh trong thư mục ckeditor_img
        Pattern pattern = Pattern.compile("/img/ckeditor_img/([^\"\\s]+)");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            images.add(matcher.group(1)); // Lấy tên file
        }

        return images;
    }

    @PostMapping("/add-to-cart")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToCart(@RequestParam Long productId,
                                                         @RequestParam(defaultValue = "1") Integer quantity) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Lấy user hiện tại
            UserAccount currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng");
                response.put("requireLogin", true);
                return ResponseEntity.ok(response);
            }

            // Kiểm tra sản phẩm tồn tại
            Product product = productService.getProductById(productId);
            if (product == null) {
                response.put("success", false);
                response.put("message", "Sản phẩm không tồn tại");
                return ResponseEntity.ok(response);
            }

            // Kiểm tra không thể mua sản phẩm của chính mình
            if (product.getCreatedBy().getUserId().equals(currentUser.getUserId())) {
                response.put("success", false);
                response.put("message", "Bạn không thể mua sản phẩm của chính mình");
                return ResponseEntity.ok(response);
            }

            // Kiểm tra sản phẩm có giá (chỉ admin mới có sản phẩm có giá)
            if (product.getPrice() == null || product.getPrice() <= 0) {
                response.put("success", false);
                response.put("message", "Sản phẩm này không có giá bán");
                return ResponseEntity.ok(response);
            }

            boolean added = cartService.addProductToCart(currentUser.getUserId(), product.getId());

            if (added) {
                response.put("success", true);
                response.put("message", "Đã thêm sản phẩm vào giỏ hàng");
                response.put("cartItemCount", cartService.countCartByUserId(currentUser.getUserId()));
                response.put("productInfo", Map.of(
                        "name", product.getName(),
                        "price", product.getPrice(),
                        "quantity", quantity
                ));
            } else {
                response.put("success", false);
                response.put("message", "Không thể thêm sản phẩm vào giỏ hàng");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

//    private int getCartItemCount(UserAccount user) {
//        try {
//            if (user.getCartItems() == null) {
//                return 0;
//            }
//            return user.getCartItems().size();
//
//        } catch (Exception e) {
//            return 0;
//        }
//    }

}
