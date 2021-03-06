package com.robot.mvc.helpers;

import com.robot.mvc.core.exceptions.RobotException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * 依赖注入
 * Created by laotang on 2019/11/3.
 */
public class BeanHelper {

    private static BeanHelper BEAN_HELPER = null;
    private final static Lock lock = new ReentrantLock();
    /**
     * ioc所需要的bean
     */
    private static final Map<String, Object> iocBeanMap = new HashMap<>();

    public static Map<String, Object> getIocBeanMap() {
        return iocBeanMap;
    }

    public static BeanHelper duang() {
        try {
            lock.lock();
            if (null == BEAN_HELPER) {
                BEAN_HELPER = new BeanHelper();
            }
        } finally {
            lock.unlock();
        }
        return BEAN_HELPER;
    }

    private BeanHelper() {

    }

    public void setBean(Object targetObj) {
        String key = getBeanClassName(targetObj.getClass());
        iocBeanMap.put(key, targetObj);
    }

    /**
     * 根据Class取出对应的Ioc Bean
     */
    public <T> T getBean(Class<?> clazz) {
        String key = getBeanClassName(clazz);
        return getBean(key);
    }

    /**
     * 根据key取出对应的Ioc Bean
     *
     * @param key 类的全名，包含包路径，如： Class.getName()
     */
    public <T> T getBean(String key) {
        if (!iocBeanMap.containsKey(key)) {
            throw new RobotException("无法根据类名[" + key + "]获取实例 , 请检查！");
        }
        return (T) iocBeanMap.get(key);
    }

    private String getBeanClassName(Class<?> clazz) {
        String className = clazz.getName();
        int index = className.indexOf("$$");
        if (index > -1 && className.contains("CGLIB$$")) {
            className = className.substring(0, index);
        }
        return className;
    }
}
