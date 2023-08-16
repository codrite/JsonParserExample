package com.example.jsonparser;

import java.util.List;

public class Person {

    String name;
    Integer age;

    List<Order> orders;

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", orders=" + orders +
                '}';
    }
}
