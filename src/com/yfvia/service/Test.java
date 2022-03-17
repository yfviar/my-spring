package com.yfvia.service;

import com.yfvia.spring.ViaApplicationContext;

public class Test {
    public static void main(String[] args) {
        ViaApplicationContext applicationContext = new ViaApplicationContext(AppConfig.class);
        UserService userService = (UserService) applicationContext.getBean("userService");
    }
}
