package org.example.model;

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
        return address;
    }
}