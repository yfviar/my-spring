package com.yfvia.spring;

/**
 * 实现该接口的类，可以根据不同的beanName，bean对象
 * 在初始化前后，做不同的事情
 */
public interface BeanPostProcessor {
    public Object postProcessBeforeInitialization(String beanName, Object bean);

    public Object postProcessAfterInitialization(String beanName, Object bean);
}
