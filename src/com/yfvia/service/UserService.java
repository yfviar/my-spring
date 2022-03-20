package com.yfvia.service;

import com.yfvia.spring.*;

@Component("userService")
@Scope("prototype")
public class UserService implements BeanNameAware, InitializingBean {

    @Autowire
    private OrderService orderService;

    private String beanName;

    public void test() {
        System.out.println(orderService);
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getBeanName() {
        return beanName;
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("------" + beanName + ":初始化------");
    }
}
