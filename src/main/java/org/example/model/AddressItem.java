package org.example.model;

// 添加地址项类
public class AddressItem {
    private final long id;
    private final String address;

    public AddressItem(long id, String address) {
        this.id = id;
        this.address = address;
    }

    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return address;  // 这将显示在下拉框中
    }
}