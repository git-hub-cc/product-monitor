package org.example.model;

/**
 * 地址项实体类
 * 用于管理地址信息，主要用于下拉框显示
 */
public class AddressItem {
    // 地址ID
    private final long id;
    // 地址详细信息
    private final String address;

    /**
     * 带参构造函数
     * @param id 地址ID
     * @param address 地址详细信息
     */
    public AddressItem(long id, String address) {
        this.id = id;
        this.address = address;
    }

    /**
     * 获取地址ID
     * @return 地址ID
     */
    public long getId() {
        return id;
    }

    /**
     * 重写toString方法，用于下拉框显示
     * @return 地址详细信息
     */
    @Override
    public String toString() {
        return address;
    }
}