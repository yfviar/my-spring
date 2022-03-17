package com.yfvia.spring;

public class ViaApplicationContext {
    private Class configClass;

    public ViaApplicationContext(Class configClass) {
        this.configClass = configClass;


    }

    public Object getBean(String beanName) {
        return null;
    }
}
