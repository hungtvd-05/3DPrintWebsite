package com.web.util;

public enum OrderStatus {

    IN_PROGRESS(1, "Đơn hàng đang được xử lí!"),
    ORDER_RECEIVED(2, "Đã xác nhận đơn hàng!"),
    PRODUCT_PACKED(3, "Đơn hàng đã đóng gói!"),
    OUT_FOR_DELIVERY(4, "Đã giao cho bên vận chuyển!"),
    DELIVERED(5, "Đã vận chuyển thành công!"),
    CANCEL(6, "Đã hủy!");

    private Integer id;

    private String name;

    private OrderStatus(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static OrderStatus fromId(Integer id) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.getId().equals(id)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No OrderStatus found for id: " + id);
    }
}
