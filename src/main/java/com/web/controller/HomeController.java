package com.web.controller;

import com.web.config.WebInfoConfig;
import com.web.model.*;
import com.web.service.*;
import com.web.util.CommonUtil;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class HomeController {

    String imgPath = System.getProperty("user.dir") + File.separator
            + "src" + File.separator + "main" + File.separator + "resources"
            + File.separator + "static" + File.separator + "img";

    @Autowired
    private UserService userService;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private WebInfoService webInfoService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProductService productService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private BlogService blogService;

    @Autowired
    private CartService cartService;

    @Autowired
    private WebInfoConfig webInfoConfig;

    @Autowired
    private CheckOutService checkOutService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private OrderService orderService;

    @ModelAttribute
    public void getUserDetails(Principal p, Model m) {

        if (p != null) {
            String email = p.getName();
            UserAccount user = userService.getUserAccountByEmail(email);
            m.addAttribute("countCart", cartService.countCartByUserId(user.getUserId()));
            m.addAttribute("user", user);
        }
        m.addAttribute("webInfo", webInfoConfig.getWebInfo());
        m.addAttribute("policiesInfo", blogService.getALlPoliciesIsEnabled().stream().limit(3));
//        m.addAttribute("supportUrls", supportUrlService.getSupportUrl());
    }

    @GetMapping("/")
    public String index(Model m) {

        m.addAttribute("productsOfAdmin", productService.getProductsOfAdminForHomePage());
        m.addAttribute("productsOfUser", productService.getProductsOfUserForHomePage());
        m.addAttribute("productDTOs", productService.getAllProductsForSearch());
        return "home";
    }

    @GetMapping("/signin")
    public String login() {
        return "signin";
    }

    @GetMapping("/signup")
    public String register() {
        return "signup";
    }

    @PostMapping("/save-user")
    public String saveUser(@RequestParam("fullName") String fullName,
                           @RequestParam("phoneNumber") String phoneNumber,
                           @RequestParam("email") String email,
                           @RequestParam("password") String password,
                           HttpServletRequest request,
                           HttpSession session) throws IOException, MessagingException {

        if (userService.existsEmail(email)) {
            session.setAttribute("errorMsg", "Email này đã tồn tại!");
            return "redirect:/signup";
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(password);

        UserAccount userAccount = new UserAccount();
        userAccount.setFullName(fullName);
        userAccount.setPhoneNumber(phoneNumber);
        userAccount.setEmail(email);

//        user.setUserAccount(userAccount);

        String confirmToken = UUID.randomUUID().toString();
        user.setConfirmToken(confirmToken);

        User addUser = userService.addUser(user, userAccount);

        if (!ObjectUtils.isEmpty(addUser)) {

            String url = CommonUtil.generateUrl(request) + "/confirm-email?token=" + confirmToken;

            commonUtil.sendConfirmEmail(url, addUser.getEmail());

            session.setAttribute("succMsg", "Đã gửi xác nhận tài khoản qua mail của bạn!");

        } else {
            session.setAttribute("errorMsg", "Lỗi!");
        }

        return "redirect:/signup";
    }

    @GetMapping("/confirm-email")
    public String showConfirmedEmail(@RequestParam String token, Model m) {

        User user = userService.confirmEmail(token);

        if (user == null) {
            m.addAttribute("msg", "Đường link không hiệu dụng!");
            return "message";
        }
        m.addAttribute("token", token);
        return "user/confirmsuccess";
    }

    @GetMapping("/forgot-password")
    public String showForgotPassword() {
        return "forgot_password";
    }

    @PostMapping("/check-email")
    public String processForgotPassword(@RequestParam String email, HttpSession session, HttpServletRequest request) throws MessagingException, UnsupportedEncodingException {

        User user = userService.findByEmail(email);

        if (ObjectUtils.isEmpty(user)) {
            session.setAttribute("errorMsg", "không tìm thấy tài khoản!");
        } else {

            String resetToken = UUID.randomUUID().toString();
            userService.updateUserResetToken(user, resetToken);

            String url = CommonUtil.generateUrl(request) + "/reset-password?token=" + resetToken;

            commonUtil.sendMail(url, email);

            session.setAttribute("succMsg", "Đã gửi xác nhận thay đổi mật khẩu qua mail của bạn!");
        }

        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPassword(@RequestParam String token, Model m) {

        User user = userService.getUserByToken(token);

        if (user == null) {
            m.addAttribute("msg", "Đường link không hiệu dụng!");
            return "message";
        }
        m.addAttribute("token", token);
        return "reset_password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token, @RequestParam String password, Model m) {

        User user = userService.getUserByToken(token);

        if (user == null) {
            m.addAttribute("errorMsg", "Đường link không hiệu dụng!");
            return "message";
        } else {
            user.setPassword(passwordEncoder.encode(password));
            user.setResetToken(null);
            userService.updateUser(user);
            m.addAttribute("msg", "Đã thay đổi mật khẩu thành công!");
            return "message";
        }
    }

    @GetMapping("/product:{id:[0-9]+}")
    public String viewProduct(@PathVariable("id") Long id, Model m) {
        Product product = productService.getProductById(id);
        m.addAttribute("moreProducts", productService.getMoreProducts(product));
        m.addAttribute("product", product);
        m.addAttribute("view", "");
        return "view_product";
    }

    @GetMapping("/product:{id:[0-9]+}/files")
    public String viewProductFiles(@PathVariable("id") Long id, Model m) {
        Product product = productService.getProductById(id);
        m.addAttribute("moreProducts", productService.getMoreProducts(product));
        m.addAttribute("product", product);
        m.addAttribute("view", "files");
        return "view_product";
    }

    @GetMapping("/product:{id:[0-9]+}/comments")
    public String viewProductComments(@PathVariable("id") Long id, Model m) {
        Product product = productService.getProductById(id);
        m.addAttribute("moreProducts", productService.getMoreProducts(product));
        m.addAttribute("product", product);
        m.addAttribute("view", "comments");
        m.addAttribute("comments", commentService.getAllCommentsOfProduct(id));
        return "view_product";
    }

    @GetMapping("/products")
    public String viewProducts(Model m,
                               @RequestParam(name = "page", defaultValue = "1") Integer pageNumber,
                               @RequestParam(value = "sorted", defaultValue = "") String sorted,
                               @RequestParam(value = "search", defaultValue = "") String search,
                               @RequestParam(name = "pageSize", defaultValue = "24") Integer pageSize) {
        Page<Product> page = productService.getAllProducts(pageNumber - 1, pageSize, sorted, search);
        m.addAttribute("products", page.getContent());
        m.addAttribute("sorted", sorted);
        m.addAttribute("search", search);
        m.addAttribute("productDTOs", productService.getAllProductsForSearch());
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("page", page.getNumber());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());
        m.addAttribute("pageInfo", "Sản phẩm");
        m.addAttribute("view", "/products");
        return "products";
    }

    @GetMapping("/products:owner")
    public String viewProductsOwner(Model m,
                               @RequestParam(name = "page", defaultValue = "1") Integer pageNumber,
                               @RequestParam(value = "sorted", defaultValue = "") String sorted,
                               @RequestParam(value = "search", defaultValue = "") String search,
                               @RequestParam(name = "pageSize", defaultValue = "24") Integer pageSize) {
        Page<Product> page = productService.getAllProductsByRole(pageNumber - 1, pageSize, sorted, search, "ROLE_ADMIN");
        m.addAttribute("products", page.getContent());
        m.addAttribute("sorted", sorted);
        m.addAttribute("search", search);
        m.addAttribute("productDTOs", productService.getAllProductsForSearchByRole("ROLE_ADMIN"));
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("page", page.getNumber());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());
        m.addAttribute("pageInfo", "Sản phẩm");
        m.addAttribute("view", "/products:owner");
        return "products";
    }

    @GetMapping("/products:community")
    public String viewProductsCommunity(Model m,
                                    @RequestParam(name = "page", defaultValue = "1") Integer pageNumber,
                                    @RequestParam(value = "sorted", defaultValue = "") String sorted,
                                    @RequestParam(value = "search", defaultValue = "") String search,
                                    @RequestParam(name = "pageSize", defaultValue = "24") Integer pageSize) {
        Page<Product> page = productService.getAllProductsByRole(pageNumber - 1, pageSize, sorted, search, "ROLE_USER");
        m.addAttribute("products", page.getContent());
        m.addAttribute("sorted", sorted);
        m.addAttribute("search", search);
        m.addAttribute("productDTOs", productService.getAllProductsForSearchByRole("ROLE_USER"));
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("page", page.getNumber());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());
        m.addAttribute("pageInfo", "Sản phẩm");
        m.addAttribute("view", "/products:community");
        return "products";
    }

    @GetMapping("/blogs")
    public String viewBlogs(Model m,
                            @RequestParam(name = "page", defaultValue = "1") Integer pageNumber,
                            @RequestParam(value = "sorted", defaultValue = "") String sorted,
                            @RequestParam(value = "search", defaultValue = "") String search,
                            @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize) {
        Page<Blog> page = blogService.getBlogsPageIsEnable(pageNumber - 1, pageSize, sorted, search, true);
        m.addAttribute("blogs", page.getContent());
        m.addAttribute("sorted", sorted);
        m.addAttribute("search", search);
        m.addAttribute("blogsSearch", blogService.getALlBlogIsEnabled());
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("page", page.getNumber());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());

        return "blogs";
    }

    @GetMapping("/policies")
    public String viewPolicies(Model m,
                            @RequestParam(name = "page", defaultValue = "1") Integer pageNumber,
                            @RequestParam(value = "sorted", defaultValue = "") String sorted,
                            @RequestParam(value = "search", defaultValue = "") String search,
                            @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize) {
        Page<Blog> page = blogService.getBlogsPageIsEnable(pageNumber - 1, pageSize, sorted, search, false);
        m.addAttribute("blogs", page.getContent());
        m.addAttribute("sorted", sorted);
        m.addAttribute("search", search);
        m.addAttribute("blogsSearch", blogService.getALlPoliciesIsEnabled());
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("page", page.getNumber());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());

        return "policies";
    }

    @GetMapping("/blog:{id:[0-9]+}")
    public String viewBlog(@PathVariable("id") Long id, Model m) {
        Blog blog = blogService.getBlogById(id);
        if (blog == null || !blog.getIsBlog() || !blog.getStatus()) {
            m.addAttribute("msg", "Không tìm thấy bài viết!");
            return "message";
        }
        m.addAttribute("blog", blog);
        m.addAttribute("view", "blog");
        return "view_blog";
    }

    @GetMapping("/policy:{id:[0-9]+}")
    public String viewPolicy(@PathVariable("id") Long id, Model m) {
        Blog blog = blogService.getBlogById(id);
        if (blog == null || blog.getIsBlog() || !blog.getStatus()) {
            m.addAttribute("msg", "Không tìm thấy chính sách!");
            return "message";
        }
        m.addAttribute("blog", blog);
        m.addAttribute("view", "policy");
        return "view_blog";
    }

    @GetMapping("/tag:{tagName:[a-zA-Z0-9-]+}")
    public String viewTag(@PathVariable("tagName") String tagName, Model m,
                          @RequestParam(name = "page", defaultValue = "1") Integer pageNumber,
                          @RequestParam(value = "sorted", defaultValue = "") String sorted,
                          @RequestParam(name = "pageSize", defaultValue = "24") Integer pageSize) {
        Page<Product> page = productService.getAllProductByTag(pageNumber - 1, pageSize, sorted, tagName);
        m.addAttribute("tagName", tagName);
        m.addAttribute("products", page.getContent());
        m.addAttribute("sorted", sorted);
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("page", page.getNumber());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());

        return "products_by_tag";
    }

    @GetMapping("/profile:{id:[0-9]+}")
    public String viewProfile(@PathVariable("id") Long id,
                              @RequestParam(value = "sorted", defaultValue = "") String sorted,
                              @RequestParam(value = "search", defaultValue = "") String search,
                              @RequestParam(name = "page", defaultValue = "1") Integer pageNumber,
                              @RequestParam(name = "pageSize", defaultValue = "24") Integer pageSize,
                              Model m) {
        UserAccount userAccount = userService.getUserAccountById(id);
        if (ObjectUtils.isEmpty(userAccount)) {
            m.addAttribute("msg", "Không tìm thấy người dùng!");
            return "message";
        }
        Page<Product> page = productService.getAllProductsOfUserId(pageNumber - 1, pageSize, id, sorted, search);
        m.addAttribute("profileUser", userAccount);
        m.addAttribute("productDTOs", productService.getAllProductsOfUserIdForSearch(id));
        m.addAttribute("favoritesOfUser", page.getContent());
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("page", page.getNumber());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());
        m.addAttribute("viewOfWeb", "/profile:" + id);
        m.addAttribute("search", search);
        m.addAttribute("sorted", sorted);
        return "view_profile";
    }

    @GetMapping("/profile:{id:[0-9]+}/favorites")
    public String viewProfileFavorites(@PathVariable("id") Long id,
                              @RequestParam(value = "sorted", defaultValue = "") String sorted,
                              @RequestParam(value = "search", defaultValue = "") String search,
                              @RequestParam(name = "page", defaultValue = "1") Integer pageNumber,
                              @RequestParam(name = "pageSize", defaultValue = "24") Integer pageSize,
                              Model m) {
        UserAccount userAccount = userService.getUserAccountById(id);
        if (ObjectUtils.isEmpty(userAccount)) {
            m.addAttribute("msg", "Không tìm thấy người dùng!");
            return "message";
        }
        Page<Product> page = productService.getFavoriteProductsOfCurrentUser(pageNumber - 1, pageSize, sorted, search, userAccount);
        m.addAttribute("profileUser", userAccount);
        m.addAttribute("productDTOs", productService.getAllProductsFavoritesOfCurrentUserForSearch(userAccount));
        m.addAttribute("favoritesOfUser", page.getContent());
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("page", page.getNumber());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());
        m.addAttribute("viewOfWeb", "/profile:" + id + "/favorites");
        m.addAttribute("search", search);
        m.addAttribute("sorted", sorted);
        return "view_profile";
    }

    @GetMapping("/vnpay-payment")
    public String InfoPaying(HttpServletRequest request, HttpSession session, Principal p) throws MessagingException, UnsupportedEncodingException {

        UserAccount user = userService.getUserAccountByEmail(p.getName());

        String orderInfo = request.getParameter("vnp_OrderInfo");
        Order order = orderService.getOrderById(orderInfo);

        int paymentStatus = checkOutService.orderReturn(request, order);

        if (paymentStatus == 1) {
            session.setAttribute("succMsg", "Đặt hàng thành công! Mã đơn hàng: " + request.getParameter("vnp_OrderInfo"));
            cartService.clearCart(user.getUserId());

            try {
                String orderDetails = buildOrderDetailsString(order);
                commonUtil.sendOrderConfirmationEmailAsync(
                        user.getEmail(),
                        user.getFullName(),
                        orderDetails
                );
                WebInfo webInfo = webInfoConfig.getWebInfo(); // Hoặc lấy từ config
                if (webInfo != null && !webInfo.getEmail().isEmpty()) {
                    commonUtil.sendNewOrderNotificationToAdmin(
                            webInfo.getEmail(),
                            user.getFullName(),
                            user.getEmail(),
                            order.getOrderId(),
                            order.getTotalAmount(),
                            orderDetails
                    );
                }

                sendNewOrderNotificationToAdmin(order, user);

                System.out.println("Email gửi thành công đến: " + user.getEmail());
            } catch (Exception emailException) {
                System.err.println("Lỗi gửi email: " + emailException.getMessage());
            }

            return "redirect:/user/orders";
        } else {
            session.setAttribute("errorMsg", "Thanh toán thấy bại. Vui lòng thử lại sau.");
            return "redirect:/user/checkout";
        }
    }

    private String buildOrderDetailsString(Order order) {
        StringBuilder details = new StringBuilder();
        details.append("<h3>Mã đơn hàng: ").append(order.getOrderId()).append("</h3>");
        details.append("<p><strong>Người nhận:</strong> ").append(order.getReceiverName()).append("</p>");
        details.append("<p><strong>Điện thoại:</strong> ").append(order.getPhoneNumber()).append("</p>");
        details.append("<p><strong>Địa chỉ:</strong> ").append(order.getDetailAddress()).append("</p>");
        details.append("<h4>Chi tiết sản phẩm:</h4>");
        details.append("<table border='1' style='border-collapse: collapse; width: 100%;'>");
        details.append("<tr><th>Sản phẩm</th><th>Số lượng</th><th>Giá</th><th>Thành tiền</th></tr>");

        for (OrderItem item : order.getOrderItems()) {
            double itemTotal = item.getPrice() * item.getQuantity();
            details.append("<tr>")
                    .append("<td>").append(item.getProduct().getName()).append("</td>")
                    .append("<td>").append(item.getQuantity()).append("</td>")
                    .append("<td>").append(CommonUtil.formatPrice(item.getPrice())).append(" VNĐ</td>")
                    .append("<td>").append(CommonUtil.formatPrice(itemTotal)).append(" VNĐ</td>")
                    .append("</tr>");
        }

        details.append("</table>");
        details.append("<h4><strong>Tổng tiền: ").append(CommonUtil.formatPrice(order.getTotalAmount())).append(" VNĐ</strong></h4>");

        return details.toString();
    }

    private void sendNewOrderNotificationToAdmin(Order order, UserAccount customer) {
        try {
            // Tạo notification object
            Map<String, Object> notification = new HashMap<>();
            notification.put("id", System.currentTimeMillis());
            notification.put("type", "new_order");
            notification.put("contentId", order.getOrderId());
            notification.put("totalAmount", order.getTotalAmount());
            notification.put("createdAt", order.getCreatedAt());

            // Thông tin khách hàng
            Map<String, Object> customerInfo = new HashMap<>();
            customerInfo.put("userId", customer.getUserId());
            customerInfo.put("fullName", customer.getFullName());
            customerInfo.put("email", customer.getEmail());
            customerInfo.put("profileImage", customer.getProfileImage());
            notification.put("user", customerInfo);

            // Tạo content message
            String content = String.format("Đơn hàng mới #%s từ %s - Tổng tiền: %s VNĐ",
                    order.getOrderId(),
                    customer.getFullName(),
                    CommonUtil.formatPrice(order.getTotalAmount()));
            notification.put("content", content);

            // Tạo notification key để tránh duplicate
            String notificationKey = String.format("new_order_%s_%d",
                    order.getOrderId(),
                    System.currentTimeMillis());
            notification.put("notificationKey", notificationKey);

            List<UserAccount> admins = webInfoConfig.getAdminAccounts();

            for (UserAccount admin : admins) {
                Notification dbNotification = new Notification();
                dbNotification.setUser(admin);
                dbNotification.setType("new_order");
                dbNotification.setContent(content);
                dbNotification.setContentId(order.getOrderId());
                dbNotification.setSenderId(customer.getUserId());
                dbNotification.setSenderName(customer.getFullName());
                dbNotification.setSenderAvatar(customer.getProfileImage());
                dbNotification.setNotificationKey(notificationKey + "_" + admin.getUserId());
                dbNotification.setIsRead(false);
                dbNotification.setCreatedAt(LocalDateTime.now());

                notificationService.save(dbNotification);
            }

            // Gửi đến admin qua WebSocket
            messagingTemplate.convertAndSend("/topic/admin/notifications", notification);

            System.out.println("Đã gửi notification đơn hàng mới đến admin: " + order.getOrderId());

        } catch (Exception e) {
            System.err.println("Lỗi gửi notification đến admin: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
