package com.yfvia.spring;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;

public class ViaApplicationContext {
    private Class configClass;

    public ViaApplicationContext(Class configClass) {
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
                        className = className.replace("\\", ".");
//                        System.out.println(className);


                        try {
                            Class<?> clazz = classLoader.loadClass(className);
                            if (clazz.isAnnotationPresent(Component.class)) {
                                System.out.println(clazz.getName());
//                                Bean
                            }
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }


                    }
                }
            }
        }

    }

    public Object getBean(String beanName) {
        return null;
    }
}
