package com.openagv.ioc;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import com.duangframework.db.DbClientFatory;
import com.duangframework.db.annotation.DbClient;
import com.duangframework.db.core.IDbClient;
import com.duangframework.db.mongodb.MongoDao;
import com.google.inject.Inject;
import com.openagv.core.AppContext;
import com.openagv.tools.ToolsKit;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 依赖注入
 * Created by laotang on 2019/11/3.
 */
public class IocHelper {

    public static void ioc() throws Exception {
        Iterator<Object> iterator = AppContext.getInjectClassObjectSet().iterator();
        while (iterator.hasNext()) {
            Object serviceObj = iterator.next();
            Class<?> clazz = serviceObj.getClass();
            ioc(serviceObj, clazz);
        }
    }

    public static void ioc(Object serviceObj, Class clazz) throws Exception {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            boolean isInjectAnn = field.isAnnotationPresent(Inject.class) || field.isAnnotationPresent(javax.inject.Inject.class);
            if (isInjectAnn && MongoDao.class.equals(field.getType())) {
                ParameterizedType paramType = (ParameterizedType) field.getGenericType();
                Type[] types = paramType.getActualTypeArguments();
                if (ToolsKit.isNotEmpty(types)) {
                    // <>里的泛型类
                    Class<?> paramTypeClass = ClassUtil.loadClass(types[0].getTypeName());
                    Object daoObj = getDbInjectDao(field, paramTypeClass);
                    if (null != daoObj) {
                        field.setAccessible(true);
                        field.set(serviceObj, daoObj);
                        DbClientFatory.duang().setDao(serviceObj.getClass(), daoObj);
                    }
                }
            }
        }
    }

    private static final Map<String, Object> DB_DAO_MAP = new HashMap<>();
    private static Object getDbInjectDao(Field field, Class<?> paramTypeClass) {
        String key = paramTypeClass.getName();
        Object dbDaoObj = DB_DAO_MAP.get(key);
        if(ToolsKit.isEmpty(dbDaoObj)) {
            DbClient dbClient = field.getAnnotation(DbClient.class);
            String dbClientId = ToolsKit.isNotEmpty(dbClient) ? dbClient.id() : "";
//            List<?> proxyList = null;
//            dbDaoObj =  MongoUtils.getMongoDao(dbClientId, paramTypeClass, proxyList);
            dbDaoObj = ReflectUtil.newInstance(MongoDao.class, dbClientId, paramTypeClass);
            DB_DAO_MAP.put(key, dbDaoObj);
        }
        return DB_DAO_MAP.get(key);
    }

}
