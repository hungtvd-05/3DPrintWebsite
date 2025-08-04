package com.web.controller;

import com.web.model.*;
import com.web.service.*;
import com.web.util.CommonUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/user")
public class UserController {

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
    private CartService cartService;

    @Autowired
    private AddressService addressService;

    @ModelAttribute
    public void getUserDetails(Principal p, Model m) {

        if (p != null) {
            String email = p.getName();
            UserAccount user = userService.getUserAccountByEmail(email);
            m.addAttribute("countCart", cartService.countCartByUserId(user.getUserId()));
            m.addAttribute("user", user);
        }
        WebInfo webInfo = webInfoService.getWebInfo();
        m.addAttribute("webInfo", (webInfo != null) ? webInfo : new WebInfo());
        m.addAttribute("policiesInfo", blogService.getALlPoliciesIsEnabled().stream().limit(3));
//        m.addAttribute("supportUrls", supportUrlService.getSupportUrl());
    }

    @GetMapping("/")
    public String home() {
        return "user/home";
    }

    @GetMapping("/my-products")
    public String myProducts(Model m, Principal p,
                             @RequestParam(name = "page", defaultValue = "1") Integer pageNumber,
                             @RequestParam(value = "sorted", defaultValue = "") String sorted,
                             @RequestParam(value = "search", defaultValue = "") String search,
                             @RequestParam(name = "pageSize", defaultValue = "36") Integer pageSize) {

        UserAccount userAccount = userService.getCurrentUserAccount();

        Page<Product> page = productService.getAllProductsPaginationOfCurrentUser(pageNumber - 1, pageSize, sorted, search, userAccount);
        m.addAttribute("products", page.getContent());
        m.addAttribute("sorted", sorted);
        m.addAttribute("search", search);
        m.addAttribute("productDTOs", productService.getAllProductsOfCurrentUserForSearch(userAccount));
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("page", page.getNumber());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());
        return "user/my_products";
    }

    @GetMapping("/add-product")
    public String loadAddProduct(Model m) {
        List<TagDTO> tags = productService.getAllTags();
        m.addAttribute("tags", tags);
        return "user/add_product_user";
    }

    @GetMapping("/edit-product:{id:[0-9]+}")
    public String loadEditProduct(@PathVariable("id") long id, Model m) {
        Product product = productService.getProductById(id);

        if (ObjectUtils.isEmpty(product)) {
            m.addAttribute("msg", "Không tìm thấy sản phẩm");
            return "message";
        } else if (product.getCreatedBy().getRole().equals("ROLE_ADMIN")) {
            m.addAttribute("msg", "Sản phẩm không thuộc quyền sở hữu của bạn");
            return "message";
        }

        List<TagDTO> tags = productService.getAllTags();
        m.addAttribute("tags", tags);
        m.addAttribute("product", product);
        return "user/edit_product";
    }

    @GetMapping("/profile")
    public String profile(Model m, Principal p) {
        return "user/profile";
    }

    @PostMapping("/update-profile-1")
    public String updateProfile(@RequestParam Long id,
                                @RequestParam(value = "img", required = false) MultipartFile img,
                                @RequestParam String fullName,
                                @RequestParam String phoneNumber,
                                @RequestParam(value = "linkToOrder", defaultValue = "") String linkToOrder,
                                Principal p,
                                HttpSession session) throws IOException {


        UserAccount user = userService.getUserAccountByEmail(p.getName());

        if (user == null || !Objects.equals(user.getUserId(), id)) {
            return "redirect:/user/profile";
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
        user.setLinkToOrder(linkToOrder.trim());

        if (!ObjectUtils.isEmpty(userService.updateUserAccount(user))) {
            session.setAttribute("succMsg", "Cập nhật thông tin thành công!");
        } else {
            session.setAttribute("errorMsg", "Chưa thể cập nhật thông tin!");
        }

        return "redirect:/user/profile";
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
            return "redirect:/user/profile";
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

        return "redirect:/user/profile";
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

        return "redirect:/user/profile";
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
        m.addAttribute("view", "/user/favorites");
        return "products";
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

    @GetMapping("/cart")
    public String viewCart(Model m, Principal p) {

        UserAccount user = userService.getUserAccountByEmail(p.getName());

        List<CartItemDTO> cartItems = cartService.getCartWithProducts(user.getUserId());

        m.addAttribute("cartItems", cartItems);

        return "user/cart";
    }

    @PostMapping("/update-quantity")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateQuantity(@RequestBody UpdateQuantityRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = cartService.updateQuantity(request.getCartId(), request.getQuantity());

            if (success) {
                response.put("success", true);
                response.put("message", "Quantity updated successfully");
                response.put("cartId", request.getCartId());
                response.put("newQuantity", request.getQuantity());
            } else {
                response.put("success", false);
                response.put("message", "Cart item not found");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating quantity: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/delete-cart-item")
    public ResponseEntity<Map<String, Object>> deleteCartItem(@RequestBody DeleteCartRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = cartService.deleteCartItem(request.getCartId());

            if (success) {
                response.put("success", true);
                response.put("message", "Cart item deleted successfully");
            } else {
                response.put("success", false);
                response.put("message", "Cart item not found");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting cart item");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/addresses")
    public String viewAddress(Model m, Principal p) {
        UserAccount user = userService.getUserAccountByEmail(p.getName());
        List<Address> addresses = addressService.findByUserId(user.getUserId());
        m.addAttribute("addresses", addresses);
        return "user/addresses";
    }

    @GetMapping("/add-address")
    private String addAddress(Model m, Principal p) {
        return "user/add-address";
    }

    @PostMapping("/add-address")
    public String addAddress(@ModelAttribute Address address, Principal p, HttpSession session) {

        if (!ObjectUtils.isEmpty(addressService.addAddress(address))) {
            session.setAttribute("succMsg", "Thêm địa chỉ thành công!");
        } else {
            session.setAttribute("errorMsg", "Không thể thêm địa chỉ!");
        }

        return "redirect:/user/addresses";
    }

    @PostMapping("/update-default-address")
    public ResponseEntity<Map<String, Object>> updateDefaultAddress(@RequestBody UpdateDefaultAddressRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = addressService.updateDefaultAddress(request.getAddressId());

            if (success) {
                response.put("success", true);
                response.put("message", "Default address updated successfully");
            } else {
                response.put("success", false);
                response.put("message", "Address not found");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating default address");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/edit-address:{id:[0-9]+}")
    public String editAddressPage(@PathVariable Long id, Model model) {
        Address address = addressService.findById(id);
        if (address != null) {
            model.addAttribute("address", address);
            return "user/edit-address";
        }
        return "redirect:/user/addresses";
    }

    @PostMapping("/update-address")
    public String updateAddress(@ModelAttribute Address address, Principal p, HttpSession session) {

        if (!ObjectUtils.isEmpty(addressService.addAddress(address))) {
            session.setAttribute("succMsg", "Cập nhật địa chỉ thành công!");
        } else {
            session.setAttribute("errorMsg", "Không thể cập nhật địa chỉ!");
        }

        return "redirect:/user/addresses";
    }

    @GetMapping("/delete-address:{id:[0-9]+}")
    public String deleteAddress(@PathVariable Long id, Principal p, HttpSession session) {
        UserAccount user = userService.getUserAccountByEmail(p.getName());
        Address address = addressService.findById(id);
        if (!ObjectUtils.isEmpty(address) && address.getUserId().equals(user.getUserId())) {
            addressService.deleteAddress(address);
            session.setAttribute("succMsg", "Xóa địa chỉ thành công!");
        } else {
            session.setAttribute("errorMsg", "Không thể xóa địa chỉ!");
        }
        return "redirect:/user/addresses";
    }

    @GetMapping("/checkout")
    public String checkout(Model m, Principal p) {
        UserAccount user = userService.getUserAccountByEmail(p.getName());

        List<CartItemDTO> cartItems = cartService.getCartWithProducts(user.getUserId());
        Double totalPrice = cartService.calculateTotalPrice(cartItems);
        if (cartItems.isEmpty()) {
            m.addAttribute("msg", "Giỏ hàng của bạn đang trống!");
            return "message";
        }
        m.addAttribute("address", addressService.getDefaultAddress(user.getUserId()));
        m.addAttribute("cartItems", cartItems);
        m.addAttribute("totalPrice", totalPrice);
        m.addAttribute("user", user);

        return "user/checkout";
    }

    @GetMapping("/order-product")
    public String orderProduct(Model m, Principal p) {
        UserAccount user = userService.getUserAccountByEmail(p.getName());




        return "user/order_product";
    }
}
