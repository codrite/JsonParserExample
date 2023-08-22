package com.example.jsonparser;

import java.util.List;

public class Order {

    Integer id;
    String name;

    OrderType orderType;

    List<Address> addresses;

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", orderType=" + orderType +
                ", addresses=" + addresses +
                '}';
    }
}
