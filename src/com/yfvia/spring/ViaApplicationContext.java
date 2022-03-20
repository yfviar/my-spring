package com.yfvia.spring;

import java.beans.Introspector;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class ViaApplicationContext {
    //    配置类
    private Class configClass;

    //    BeanDifinition
    private ConcurrentHashMap<String, BeanDifinition> beanDifinitionMap = new ConcurrentHashMap<>();
    //    单例池
    private ConcurrentHashMap<String, Object> singletonObjects = new ConcurrentHashMap<>();
    //    beanPostProcessor池
    private ArrayList<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    /**
     * ApplicationContext构造方法
     * 根据configClass配置类启动Spring
     *
     * @param configClass
     */
    public ViaApplicationContext(Class configClass) {
//        配置类，定义Spring的一些配置
        this.configClass = configClass;

//        判断类上有没有注解
        if (configClass.isAnnotationPresent(ComponentScan.class)) {
//            拿到注解
            ComponentScan componentScanAnnotation = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
//            拿到注解值
            String path = componentScanAnnotation.value();
//            替换为路径
            path = path.replace(".", "/");
//            拿到service中的class文件路径
            ClassLoader classLoader = ViaApplicationContext.class.getClassLoader();
            URL resource = classLoader.getResource(path);
//            创建class文件路径File对象
            File file = new File(resource.getFile());
//            是文件夹
            if (file.isDirectory()) {
//                拿到所有file
                File[] files = file.listFiles();
//                遍历file
                for (File f : files) {
//                    拿到文件绝对路径
                    String filePath = f.getAbsolutePath();
//                    过滤出类文件
                    if (filePath.endsWith(".class")) {
//                        得到前缀路径,为了处理路径,得到类的全限定名
                        URL loaderResource = classLoader.getResource("");
                        String tmpPath = new File(loaderResource.getFile()).getAbsolutePath();
                        String className = filePath.substring(tmpPath.length() + 1, filePath.indexOf(".class"));
//                        类的全限定名
                        className = className.replace("\\", ".");


                        try {
//                            获取类
                            Class<?> clazz = classLoader.loadClass(className);
//                            如果有Component注解
                            if (clazz.isAnnotationPresent(Component.class)) {
//                                如果实现BeanPostProcessor接口，添加到集合中
                                if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                                    BeanPostProcessor instance = (BeanPostProcessor) clazz.newInstance();
                                    beanPostProcessorList.add(instance);
                                    System.out.println("add");

                                }


//                                BeanDifinition存储的是bean的类的信息,将来根据BeanDifinition生成bean对象
                                BeanDifinition beanDifinition = new BeanDifinition();
//                                判定是单例还是其他
                                if (clazz.isAnnotationPresent(Scope.class)) {
                                    beanDifinition.setScope(clazz.getAnnotation(Scope.class).value());
                                } else {
//                                    没有Scope注解,默认单例
                                    beanDifinition.setScope("singleton");
                                }
                                beanDifinition.setType(clazz);
//                                拿到bean的名字
                                String beanName = clazz.getAnnotation(Component.class).value();
//                                当不指定bean名称时,默认名称
                                if (beanName.equals("")) {
                                    beanName = Introspector.decapitalize(clazz.getSimpleName());
                                }


//                                将beanDifinition添加进map
                                beanDifinitionMap.put(beanName, beanDifinition);
                            }
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        } catch (InstantiationException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }


                    }
                }
            }
        }

//        直接生成单例bean
        for (String beanName : beanDifinitionMap.keySet()) {
            BeanDifinition beanDifinition = beanDifinitionMap.get(beanName);
            if (beanDifinition.getScope().equals("singleton")) {
                Object bean = createBean(beanName, beanDifinition);
//                放进单例池
                singletonObjects.put(beanName, bean);
            }
        }

    }

    /**
     * 根据beanName获取Bean
     *
     * @param beanName
     * @return
     */
    public Object getBean(String beanName) {
        BeanDifinition beanDifinition = beanDifinitionMap.get(beanName);
        if (beanDifinition == null) {
            throw new NullPointerException();
        } else {
            String scope = beanDifinition.getScope();
//            单例bean
            if (scope.equals("singleton")) {
                Object bean = singletonObjects.get(beanName);
//                为bean的依赖属性考虑，某个bean可能还未被创建
                if (bean == null) {
                    Object o = createBean(beanName, beanDifinition);
                    singletonObjects.put(beanName, o);
                    return o;
                }
                return bean;

//                多例bean
            } else {
                return createBean(beanName, beanDifinition);
            }
        }
    }

    /**
     * 创建bean示例
     *
     * @param beanName
     * @param beanDifinition
     * @return
     */
    private Object createBean(String beanName, BeanDifinition beanDifinition) {
        Class clazz = beanDifinition.getType();
        try {
//            这一段,可以理解为,先针对一个类,把他的流程走完在考虑其他类
            Object instance = clazz.getConstructor().newInstance();
//            依赖注入
            for (Field field : clazz.getDeclaredFields()) {
//                检查注解
                if (field.isAnnotationPresent(Autowire.class)) {
//                    取消语言检查
                    field.setAccessible(true);
//                    注入!
                    field.set(instance, getBean(field.getName()));
                }
            }
//            aware回调
            if (instance instanceof BeanNameAware) {
                ((BeanNameAware) instance).setBeanName(beanName);
            }

//            BeanPostProcessor
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessBeforeInitialization(beanName, instance);
            }

//            可以做一些初始化
            if (instance instanceof InitializingBean) {
                ((InitializingBean) instance).afterPropertiesSet();
            }

            //            BeanPostProcessor
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessAfterInitialization(beanName, instance);
            }


            return instance;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }


        return null;

    }
}
