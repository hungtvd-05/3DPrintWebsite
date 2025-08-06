package com.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.config.WebInfoConfig;
import com.web.model.*;
import com.web.service.*;
import com.web.util.CommonUtil;
import com.web.util.OrderStatus;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    String path = System.getProperty("user.dir") + File.separator
            + "src" + File.separator + "main" + File.separator + "resources"
            + File.separator + "static" + File.separator;

    @Autowired
    private UserService userService;

    @Autowired
    private WebInfoService webInfoService;

    @Autowired
    private ProductService productService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BlogService blogService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private WebInfoConfig webInfoConfig;

    @Autowired
    private OrderService orderService;

    @ModelAttribute
    public void getUserDetails(Principal p, Model m) {

        if (p != null) {
            String email = p.getName();
            UserAccount user = userService.getUserAccountByEmail(email);
            m.addAttribute("user", user);
            m.addAttribute("countCart", 0);
        }
        m.addAttribute("webInfo", webInfoConfig.getWebInfo());
//        m.addAttribute("supportUrls", supportUrlService.getSupportUrl());
    }

    @GetMapping("")
    public String index() {
        return "redirect:/admin/products";
    }

    @GetMapping("/settings")
    public String other() {
        return "admin/settings";
    }

    @PostMapping("/update-webcomponents")
    public String updateWebComponents(@ModelAttribute WebInfo webInfo, @RequestParam(value = "img", required = false) MultipartFile img, HttpSession session) throws IOException {

        if (!ObjectUtils.isEmpty(webInfoService.updateWebInfo(webInfo, img))) {
            webInfoConfig.refreshWebInfo();
            session.setAttribute("succMsg", "Đã cập nhật thông tin website của bạn!");
        } else {
            session.setAttribute("errorMsg", "Lỗi cập nhật!");
        }
        return "redirect:/admin/settings";
    }

    @GetMapping("/add-product")
    public String loadAddProduct(Model m) {
        List<TagDTO> tags = productService.getAllTags();
        m.addAttribute("tags", tags);
        return "admin/add_product";
    }

    @PostMapping(value = "/save-product", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveProduct(@RequestParam("name") String name,
                                         @RequestParam("price") Double price,
                                         @RequestParam("status") Boolean status,
                                         @RequestParam("description") String description,
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
            product.setPrice(price);
            product.setStatus(status);
            product.setDescription(description);
            product.setTags(tags);

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

            if (!ObjectUtils.isEmpty(productService.saveProduct(product))) {
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

    @GetMapping("/tags")
    public ResponseEntity<List<TagDTO>> getAllTags() {
        List<TagDTO> tags = productService.getAllTags();
        System.out.println(tags);
        return ResponseEntity.ok(tags);
    }

    @GetMapping("/products")
    public String loadAllProduct(Model m,
                                 @RequestParam(name = "page", defaultValue = "1") Integer pageNumber,
                                 @RequestParam(value = "sorted", defaultValue = "") String sorted,
                                 @RequestParam(value = "search", defaultValue = "") String search,
                                 @RequestParam(name = "pageSize", defaultValue = "36") Integer pageSize) {

        Page<Product> page = productService.getAllProductsPaginationOfAdmin(pageNumber - 1, pageSize, sorted, search);
        m.addAttribute("products", page.getContent());
        m.addAttribute("sorted", sorted);
        m.addAttribute("search", search);
        m.addAttribute("productDTOs", productService.getAllProductsOfAdminForSearch());
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("page", page.getNumber());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());
        return "admin/products";
    }

    @GetMapping("/edit-product:{id:[0-9]+}")
    public String loadEditProduct(@PathVariable long id, Model m) {
        Product product = productService.getProductById(id);
        if (ObjectUtils.isEmpty(product)) {
            m.addAttribute("msg", "Không tìm thấy sản phẩm");
            return "message";
        } else if (!product.getCreatedBy().getRole().equals("ROLE_ADMIN")) {
            m.addAttribute("msg", "Sản phẩm không thuộc quyền sở hữu của bạn");
            return "message";
        }

        List<TagDTO> tags = productService.getAllTags();
        m.addAttribute("tags", tags);
        m.addAttribute("product", productService.getProductById(id));
        return "admin/edit_product";
    }

    @PostMapping(value = "/update-product/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProduct(@PathVariable long id,
                                           @RequestParam("name") String name,
                                           @RequestParam("price") Double price,
                                           @RequestParam("status") Boolean status,
                                           @RequestParam("description") String description,
                                           @RequestParam("images") String images,
                                           @RequestParam("stlFiles") String stlFiles,
                                           @RequestParam("deleteImages") String deleteImages,
                                           @RequestParam("deleteStlFiles") String deleteStlFiles,
                                           @RequestParam("tags") String tagsJson,
                                           @RequestParam("deleteTags") String deleteTags,
                                           HttpSession session) {

        try {

            Product product = productService.getProductById(id);

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

            product.setName(name);
            product.setPrice(price);
            product.setStatus(status);
            product.setDescription(description);
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

    @GetMapping("/products-of-user")
    public String loadProductsOfUser(Model m,
                                     @RequestParam(name = "page", defaultValue = "1") Integer pageNumber,
                                     @RequestParam(value = "sorted", defaultValue = "") String sorted,
                                     @RequestParam(value = "search", defaultValue = "") String search,
                                     @RequestParam(name = "pageSize", defaultValue = "36") Integer pageSize) {

        Page<Product> page = productService.getAllProductsPaginationOfUser(pageNumber - 1, pageSize, sorted, search);
        m.addAttribute("products", page.getContent());
        m.addAttribute("sorted", sorted);
        m.addAttribute("search", search);
        m.addAttribute("productDTOs", productService.getAllProductsOfUserForSearch());
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("page", page.getNumber());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());
        return "admin/products-of-user";
    }

    @GetMapping("/view_product_confirm:{id:[0-9]+}")
    public String viewProductOfUser(@PathVariable("id") Long id, Model m) {
        m.addAttribute("product", productService.getProductById(id));
        m.addAttribute("view", "");
        return "admin/view_product_confirm";
    }

    @GetMapping("/view_product_confirm:{id:[0-9]+}/files")
    public String viewProductOfUserFiles(@PathVariable("id") Long id, Model m) {
        m.addAttribute("product", productService.getProductById(id));
        m.addAttribute("view", "files");
        return "admin/view_product_confirm";
    }

    @GetMapping("/confirm-product")
    public ResponseEntity<String> confirmProduct(@RequestParam Long productId, @RequestParam String action) {
        Product product = productService.confirmProduct(productId, action);
        sendNotificationToProductOwner(product, action);

        if (product == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid action or product not found");
        } else if (action.equals("confirm") && product.getConfirmed() == 1) {
            return ResponseEntity.ok("confirmed");
        } else if (action.equals("reject") && product.getConfirmed() == -1) {
            return ResponseEntity.ok("rejected");
        }

        return ResponseEntity.ok("no change");
    }

    @PostMapping("/update-product-cost")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateProductCost(@RequestParam Long productId,
                                                                 @RequestParam Double cost) {
        Map<String, Object> response = new HashMap<>();

        try {
            Product product = productService.getProductById(productId);

            if (product == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy sản phẩm");
                return ResponseEntity.badRequest().body(response);
            }

            if (cost < 0) {
                response.put("success", false);
                response.put("message", "Chi phí không được âm");
                return ResponseEntity.badRequest().body(response);
            }

            product.setPrice(cost);
            Product updatedProduct = productService.updateProduct(product);

            if (updatedProduct != null) {
                response.put("success", true);
                response.put("message", "Cập nhật chi phí thành công");
                response.put("newCost", cost);
            } else {
                response.put("success", false);
                response.put("message", "Lỗi khi cập nhật chi phí");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi server: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private void sendNotificationToProductOwner(Product product, String action) {
        try {
            UserAccount productOwner = product.getCreatedBy();

            String notificationKey = String.format("product_%s_%d_%d",
                    action, product.getId(), productOwner.getUserId());

            String message;
            String notificationType;

            if ("confirm".equals(action) && product.getConfirmed() == 1) {
                message = String.format("Sản phẩm '%s' của bạn đã được phê duyệt và hiển thị công khai",
                        product.getName());
                notificationType = "product_approved";
            } else if ("reject".equals(action) && product.getConfirmed() == -1) {
                message = String.format("Sản phẩm '%s' của bạn đã bị từ chối phê duyệt",
                        product.getName());
                notificationType = "product_rejected";
            } else {
                return; // Không gửi thông báo nếu không có thay đổi
            }

            // Lưu thông báo vào database (nếu có NotificationService)
             notificationService.saveAndSendNotification(
                 productOwner,
                 notificationType,
                 message,
                 product,
                 userService.getAdminAccount(), // Admin là người gửi
                 LocalDateTime.now(),
                 notificationKey
             );

            // Gửi thông báo real-time
            sendRealtimeNotificationToUser(productOwner, product, message, notificationType, notificationKey);

        } catch (Exception e) {
            System.err.println("Error sending notification to product owner: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendRealtimeNotificationToUser(UserAccount user, Product product,
                                                String message, String type, String notificationKey) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("id", System.currentTimeMillis());
            notification.put("type", type);
            notification.put("productId", product.getId());
            notification.put("content", message);
            notification.put("createdAt", LocalDateTime.now().toString());
            notification.put("notificationKey", notificationKey);

            // Thông tin admin (người gửi thông báo)
            UserAccount admin = userService.getAdminAccount();
            Map<String, Object> adminInfo = new HashMap<>();
            adminInfo.put("userId", admin.getUserId());
            adminInfo.put("fullName", admin.getFullName());
            adminInfo.put("profileImage", admin.getProfileImage());
            notification.put("user", adminInfo);

            // Gửi thông báo đến user cụ thể
            messagingTemplate.convertAndSendToUser(
                    user.getUserId().toString(),
                    "/queue/notifications",
                    notification
            );

            System.out.println("Sent notification to user: " + user.getUserId() + " for product: " + product.getId());

        } catch (Exception e) {
            System.err.println("Error sending real-time notification to user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @GetMapping("/profile")
    public String profile(Model m, Principal p) {
        UserAccount user = userService.getUserAccountByEmail(p.getName());
        m.addAttribute("user", user);
        return "admin/profile";
    }

    @PostMapping("/update-profile-1")
    public String updateProfile(@RequestParam Long id,
                                @RequestParam(value = "img", required = false) MultipartFile img,
                                @RequestParam String fullName,
                                @RequestParam String phoneNumber,
                                Principal p,
                                HttpSession session) throws IOException {


        UserAccount user = userService.getUserAccountByEmail(p.getName());

        if (user == null || !Objects.equals(user.getUserId(), id)) {
            return "redirect:/admin/profile";
        }

        if (img != null && !img.isEmpty()) {
            String originalFileName = img.getOriginalFilename();
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));

            String random_name = UUID.randomUUID().toString();

            String newFileName = String.format("img_%s_%d%s",
                    random_name,
                    System.nanoTime(),
                    extension
            );

            Files.copy(img.getInputStream(),
                    Paths.get(path + "img" + File.separator + "avatar", newFileName),
                    StandardCopyOption.REPLACE_EXISTING);

            user.setProfileImage(newFileName);
        }

        user.setFullName(fullName.trim());
        user.setPhoneNumber(phoneNumber.trim());

        if (!ObjectUtils.isEmpty(userService.updateUserAccount(user))) {
            session.setAttribute("succMsg", "Cập nhật thông tin thành công!");
        } else {
            session.setAttribute("errorMsg", "Chưa thể cập nhật thông tin!");
        }

        return "redirect:/admin/profile";
    }

    @PostMapping("/update-profile-2")
    public String updateProfileNext(@RequestParam Long id,
                                    @RequestParam(value = "aboutMe", defaultValue = "") String aboutMe,
                                    @RequestParam(value = "detailAddress", defaultValue = "") String detailAddress,
                                    @RequestParam(value = "provinceCode", defaultValue = "") String provinceCode,
                                    @RequestParam(value = "province", defaultValue = "") String province,
                                    @RequestParam(value = "wardCode", defaultValue = "") String wardCode,
                                    @RequestParam(value = "ward", defaultValue = "") String ward,
                                    @RequestParam(value = "wardFullName", defaultValue = "") String wardFullName,
                                    Principal p,
                                    HttpSession session) throws IOException {


        UserAccount user = userService.getUserAccountByEmail(p.getName());

        if (user == null || !Objects.equals(user.getUserId(), id)) {
            return "redirect:/admin/profile";
        }

        List<String> oldImages = extractImagesFromContent(user.getAboutMe());
        List<String> newImages = extractImagesFromContent(aboutMe.trim());

        for (String oldImage : oldImages) {
            if (!newImages.contains(oldImage)) {
                String imagePath = path + "img" + File.separator + "ckeditor_img" + File.separator + oldImage;
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    imageFile.delete();
                }
            }
        }

        String final_aboutMe = moveImagesFromTemp(aboutMe.trim());

        user.setAboutMe(final_aboutMe);
        user.setAddress(detailAddress.trim());
        user.setProvince(province);
        user.setProvinceCode(provinceCode);
        user.setWard(ward);
        user.setWardCode(wardCode);
        user.setWardFullName(wardFullName);

        if (!ObjectUtils.isEmpty(userService.updateUserAccount(user))) {
            session.setAttribute("succMsg", "Cập nhật thông tin thành công!");
        } else {
            session.setAttribute("errorMsg", "Chưa thể cập nhật thông tin!");
        }

        return "redirect:/admin/profile";
    }

    @PostMapping("/change-password")
    public String changPassword(@RequestParam String currentPassword, @RequestParam String newPassword, Principal p, HttpSession session) {
        User user = userService.findByEmail(p.getName());

        if (passwordEncoder.matches(currentPassword, user.getPassword())) {
            if (currentPassword.equals(newPassword)) {
                session.setAttribute("errorMsg", "Mật khẩu mới không được giống mật khẩu cũ!");
            } else {
                String newEncodePassword = passwordEncoder.encode(newPassword);
                user.setPassword(newEncodePassword);
                if (!ObjectUtils.isEmpty(userService.updateUser(user))) {
                    session.setAttribute("succMsg", "Thay đổi mật khẩu thành công!");
                } else {
                    session.setAttribute("errorMsg", "Chưa thể thay đổi mật khẩu!");
                }
            }
        } else {
            session.setAttribute("errorMsg", "Mật khẩu hiện tại không đúng!");
        }

        return "redirect:/admin/profile";
    }

    @GetMapping("/favorites")
    public String viewFavorites(Model m,
                                @RequestParam(name = "page", defaultValue = "1") Integer pageNumber,
                                @RequestParam(value = "sorted", defaultValue = "") String sorted,
                                @RequestParam(value = "search", defaultValue = "") String search,
                                @RequestParam(name = "pageSize", defaultValue = "36") Integer pageSize) {

        UserAccount user = userService.getCurrentUserAccount();

        if (ObjectUtils.isEmpty(user)) {
            m.addAttribute("msg", "Bạn chưa đăng nhập!");
            return "message";
        }

        Page<Product> page = productService.getFavoriteProductsOfCurrentUser(pageNumber - 1, pageSize, sorted, search, user);
        m.addAttribute("products", page.getContent());
        m.addAttribute("sorted", sorted);
        m.addAttribute("search", search);
        m.addAttribute("productDTOs", productService.getAllProductsFavoritesOfCurrentUserForSearch(user));
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("page", page.getNumber());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());
        m.addAttribute("pageInfo", "Sản phẩm ưa thích của bạn");
        m.addAttribute("view", "/admin/favorites");
        return "products";
    }

    @GetMapping("/blogs")
    public String loadAllBlogs(Model m,
                               @RequestParam(name = "page", defaultValue = "1") Integer pageNumber,
                               @RequestParam(value = "sorted", defaultValue = "") String sorted,
                               @RequestParam(value = "search", defaultValue = "") String search,
                               @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize) {

        Page<Blog> page = blogService.getBlogsPage(pageNumber - 1, pageSize, sorted, search, true);
        m.addAttribute("blogs", page.getContent());
        m.addAttribute("sorted", sorted);
        m.addAttribute("search", search);
        m.addAttribute("blogsSearch", blogService.getAllBlogs());
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("page", page.getNumber());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());

        return "admin/blogs";
    }

    @GetMapping("/policies")
    public String loadAllPolicies(Model m,
                               @RequestParam(name = "page", defaultValue = "1") Integer pageNumber,
                               @RequestParam(value = "sorted", defaultValue = "") String sorted,
                               @RequestParam(value = "search", defaultValue = "") String search,
                               @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize) {

        Page<Blog> page = blogService.getBlogsPage(pageNumber - 1, pageSize, sorted, search, false);
        m.addAttribute("policies", page.getContent());
        m.addAttribute("sorted", sorted);
        m.addAttribute("search", search);
        m.addAttribute("policiesSearch", blogService.getAllPolicies());
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("page", page.getNumber());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());

        return "admin/policies";
    }

    @GetMapping("/add-policy")
    public String addPolicy(Model m) {
        m.addAttribute("isBlog", false);
        return "/admin/add-blog";
    }

    @GetMapping("/add-blog")
    public String addNews(Model m) {
        m.addAttribute("isBlog", true);
        return "/admin/add-blog";
    }

    @PostMapping("/save-blog")
    public String saveBlog(@RequestParam("title") String title,
                           @RequestParam("content") String content,
                           @RequestParam("status") Boolean status,
                           @RequestParam("style") Boolean style,
                           HttpSession session) throws IOException {

        String final_content = moveImagesFromTemp(content);
        Blog blog = new Blog();
        blog.setTitle(title);
        blog.setContent(final_content);
        blog.setCreateAt(LocalDateTime.now());
        blog.setStatus(status);
        blog.setIsBlog(style);

        if (!ObjectUtils.isEmpty(blogService.saveBlog(blog))) {
            session.setAttribute("succMsg", "Đã thêm thành công!");
        } else {
            session.setAttribute("errorMsg", "Lỗi!");
        }

        if (style) {
            return "redirect:/admin/blogs";
        } else {
            return "redirect:/admin/policies";
        }
    }

    @GetMapping("/edit-blog:{id:[0-9]+}")
    public String editBlog(@PathVariable("id") Long id, Model m) {
        Blog blog = blogService.getBlogById(id);
        if (ObjectUtils.isEmpty(blog)) {
            m.addAttribute("msg", "Không tìm thấy bài viết");
            return "message";
        }
        m.addAttribute("blog", blog);
        return "admin/edit-blog";
    }

    @GetMapping("/edit-policy:{id:[0-9]+}")
    public String editPolicy(@PathVariable("id") Long id, Model m) {
        Blog blog = blogService.getBlogById(id);
        if (ObjectUtils.isEmpty(blog)) {
            m.addAttribute("msg", "Không tìm thấy chính sách");
            return "message";
        }
        m.addAttribute("blog", blog);
        return "admin/edit-blog";
    }

    @PostMapping("/update-blog")
    public String updateBlog(
            @RequestParam("id") Long id,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("status") Boolean status,
            @RequestParam("style") Boolean style,
            HttpSession session) throws IOException {

        Blog existingBlog = blogService.getBlogById(id);

        if (ObjectUtils.isEmpty(existingBlog)) {
            session.setAttribute("errorMsg", "Không tìm thấy bài viết!");
            return "redirect:/admin/blogs";
        }

        List<String> oldImages = extractImagesFromContent(existingBlog.getContent());
        List<String> newImages = extractImagesFromContent(content);

        for (String oldImage : oldImages) {
            if (!newImages.contains(oldImage)) {
                String imagePath = path + "img" + File.separator + "ckeditor_img" + File.separator + oldImage;
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    imageFile.delete();
                }
            }
        }

        String final_content = moveImagesFromTemp(content);

        existingBlog.setContent(final_content);
        existingBlog.setTitle(title);
        existingBlog.setStatus(status);
        existingBlog.setIsBlog(style);

        if (!ObjectUtils.isEmpty(blogService.saveBlog(existingBlog))) {
            session.setAttribute("succMsg", "Đã cập nhật thành công!");
        } else {
            session.setAttribute("errorMsg", "Lỗi!");
        }

        if (style) {
            return "redirect:/admin/blogs";
        } else {
            return "redirect:/admin/policies";
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

    @GetMapping("/delete-blog")
    @ResponseBody
    public Map<String, Object> deleteNews(@RequestParam Long id) {
        Map<String, Object> result = new HashMap<>();
        result.put("isDelete", blogService.deleteBlog(id));
        return result;
    }

    @GetMapping("/delete-blog:{id:[0-9]+}")
    public String deleteBlog(@PathVariable("id") Long id, HttpSession session) {
        if (blogService.deleteBlog(id)) {
            session.setAttribute("succMsg", "Đã xóa bài viết thành công!");
        } else {
            session.setAttribute("errorMsg", "Lỗi khi xóa bài viết!");
        }
        return "redirect:/admin/blogs";
    }

    @GetMapping("/delete-product:{id:[0-9]+}")
    public String deleteProduct(@PathVariable("id") Long id, HttpSession session) {
        productService.deleteProduct(id);
        session.setAttribute("succMsg", "Đã xóa sản phẩm thành công!");
        return "redirect:/admin/products";
    }

    @GetMapping("/orders")
    public String loadAllOrders(Model m,
                                @RequestParam(name = "page", defaultValue = "1") Integer pageNumber,
                                @RequestParam(value = "search", defaultValue = "") String search,
                                @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize) {

        Page<Order> page = orderService.getAllOrdersPage(pageNumber - 1, pageSize, search);

        m.addAttribute("keywords", orderService.getAllKeywords());
        m.addAttribute("orders", page.getContent());
        m.addAttribute("search", search);
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("page", page.getNumber());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());

        return "admin/orders";
    }

    @PostMapping("/update-order-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateOrderStatus(@RequestParam String orderId,
                                                                 @RequestParam Integer status,
                                                                 Principal principal) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Lấy thông tin admin hiện tại
            UserAccount admin = userService.getUserAccountByEmail(principal.getName());
            if (admin == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy thông tin admin!");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Tìm đơn hàng
            Order order = orderService.getOrderByOrderId(orderId);
            if (order == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy đơn hàng!");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // Lưu trạng thái cũ để log
            String oldStatus = order.getStatus();

            // Cập nhật trạng thái đơn hàng
            String newStatus = OrderStatus.fromId(status).getName();
            order.setStatus(newStatus);
            order.setUpdatedAt(LocalDateTime.now());

            // Lưu vào database
            Order updatedOrder = orderService.updateOrder(order);

            if (updatedOrder != null) {

                response.put("success", true);
                response.put("message", "Cập nhật trạng thái đơn hàng thành công!");
                response.put("newStatus", newStatus);
                response.put("orderId", orderId);

                return ResponseEntity.ok(response);

            } else {
                response.put("success", false);
                response.put("message", "Lỗi khi cập nhật đơn hàng!");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            System.err.println("Error updating order status: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/order/{orderId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOrderById(@PathVariable String orderId) {
        Map<String, Object> response = new HashMap<>();

        try {
            Order order = orderService.getOrderByOrderId(orderId);

            if (order != null) {
                response.put("success", true);
                response.put("order", new OrderDTO(order));
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy đơn hàng");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi tải đơn hàng: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/order-details:{orderId:[0-9a-zA-Z-]+}")
    public String viewOrderDetails(@PathVariable("orderId") String orderId, Model m) {
        Order order = orderService.getOrderByOrderId(orderId);
        if (ObjectUtils.isEmpty(order)) {
            return "redirect:/admin/orders";
        }
        m.addAttribute("order", order);
        return "admin/order-details";
    }

    @GetMapping("/users")
    public String loadAllUsers(Model m,
                               @RequestParam(name = "page", defaultValue = "1") Integer pageNumber,
                               @RequestParam(value = "search", defaultValue = "") String search,
                               @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize) {

        Page<UserAccount> page = userService.getAllUsersPage(pageNumber - 1, pageSize, search);
        m.addAttribute("keywords", userService.searchAllUsers());
        m.addAttribute("users", page.getContent());
        m.addAttribute("search", search);
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("page", page.getNumber());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());

        return "admin/users";
    }

    @PostMapping("/updateSts")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateUserStatus(@RequestParam Long userId,
                                                                 @RequestParam Boolean status) {
        Map<String, Object> response = new HashMap<>();

        try {
            UserAccount user = userService.getUserAccountById(userId);
            user.setIsEnable(status);

            UserAccount updatedUser = userService.updateUserAccount(user);

            if (updatedUser != null) {
                response.put("success", true);
                response.put("message", "Cập nhật thành công!");
                response.put("statusName", updatedUser.getIsEnable() ? "Confirm" : "No");
                return ResponseEntity.ok(response);

            } else {
                response.put("success", false);
                response.put("message", "Lỗi khi cập nhật đơn hàng!");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            System.err.println("Error updating order status: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/admin")
    public String adminDashboard(Model m, Principal p, @RequestParam(value = "search", defaultValue = "") String search) {
        UserAccount currentUser = userService.getUserAccountByEmail(p.getName());

        List<UserAccount> allAdmins = search.isEmpty() ? userService.getAllAdminAccount() : userService.searchAllAdminAccount(search);

        m.addAttribute("adminList", allAdmins.stream()
                .filter(admin -> !admin.getUserId().equals(currentUser.getUserId()))
                .collect(Collectors.toList()));

        m.addAttribute("search", search);
        m.addAttribute("keywords", userService.searchAllAdmin(currentUser));
        return "admin/admin";
    }

    @GetMapping("/add-admin")
    public String addAdmin(Model m) {
        return "admin/add-admin";
    }

    @PostMapping("/save-admin")
    public String saveUser(@RequestParam("fullName") String fullName,
                           @RequestParam("phoneNumber") String phoneNumber,
                           @RequestParam("email") String email,
                           @RequestParam("password") String password,
                           HttpServletRequest request,
                           HttpSession session) {

        if (userService.existsEmail(email)) {
            session.setAttribute("errorMsg", "Email này đã tồn tại!");
            return "redirect:/admin/add-admin";
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(password);

        UserAccount userAccount = new UserAccount();
        userAccount.setFullName(fullName);
        userAccount.setPhoneNumber(phoneNumber);
        userAccount.setEmail(email);

        if (!ObjectUtils.isEmpty(userService.addAdmin(user, userAccount))) {
            session.setAttribute("succMsg", "Đã thêm quản trị viên thành công!");
        } else {
            session.setAttribute("errorMsg", "Chưa thể thêm quản trị viên!");
        }

        return "redirect:/admin/admin";
    }

    @PostMapping("/delete-admin")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteAdmin(@RequestParam Long userId, Principal principal) {
        Map<String, Object> response = new HashMap<>();

        try {
            UserAccount currentUser = userService.getUserAccountByEmail(principal.getName());
            if (currentUser == null || currentUser.getUserId().equals(userId)) {
                response.put("success", false);
                response.put("message", "Bạn không thể xóa chính mình!");
                return ResponseEntity.badRequest().body(response);
            }

            User adminToDelete = userService.getUserById(userId);

            if (adminToDelete == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy quản trị viên này!");
                return ResponseEntity.badRequest().body(response);
            } else if (adminToDelete.getUserAccount().getRole().equals("ROLE_USER")) {
                response.put("success", false);
                response.put("message", "Không thể xóa người dùng, chỉ có thể xóa quản trị viên!");
                return ResponseEntity.badRequest().body(response);
            }

            boolean isDeleted = userService.deleteAdmin(adminToDelete);
            if (isDeleted) {
                response.put("success", true);
                response.put("message", "Đã xóa quản trị viên thành công!");
            } else {
                response.put("success", false);
                response.put("message", "Lỗi khi xóa quản trị viên!");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

    }
}


