package com.mongodb.util;

import java.util.Map;

import org.bson.types.ObjectId;

import com.google.common.collect.Maps;

/**
 * 
 * 
 * @version $Id$
 * @author qxo(qxodream@gmail.com)
 * @since 2013-5-11
 */
public final class JsonUtils {

    private JsonUtils() {

    }

    private static final class Holder1 {
        private static final ObjectSerializer INSTANCE;
        static {

            Map<Class<?>, ObjectSerializer> customSerializers = Maps.newHashMap();

            customSerializers.put(ObjectId.class, new ObjectSerializer() {
                public String serialize(Object obj) {
                    StringBuilder builder = new StringBuilder();
                    serialize(obj, builder);
                    return builder.toString();
                }

                public void serialize(Object obj, StringBuilder buf) {
                    JSON.string(buf, obj == null ? null : obj.toString());
                }
            });

            INSTANCE = createObjectSerializer(customSerializers);
        }
    }

    /**
     * 
     * @return 定制化对象json转换器 
     *  org.bson.types.ObjectId ==> 将会直接转换成字符串
     */
    public static ObjectSerializer getCustomObjectSerializer() {
        return Holder1.INSTANCE;
    }

    public static ObjectSerializer createObjectSerializer(Map<Class<?>, ObjectSerializer> customSerializers) {
        ClassMapBasedObjectSerializer json = (ClassMapBasedObjectSerializer) JSONSerializers.getStrict();

        for (Map.Entry<Class<?>, ObjectSerializer> entry : customSerializers.entrySet()) {
            json.addObjectSerializer(entry.getKey(), entry.getValue());
        }
        // json.addObjectSerializer(ObjectId.class, new ObjectSerializer() {
        // public String serialize(Object obj) {
        // StringBuilder builder = new StringBuilder();
        // serialize(obj, builder);
        // return builder.toString();
        // }
        //
        // public void serialize(Object obj, StringBuilder buf) {
        // buf.append(obj.toString());
        // }
        // });
        return json;
    };
}
