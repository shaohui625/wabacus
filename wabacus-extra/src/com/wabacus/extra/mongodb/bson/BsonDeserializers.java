package com.wabacus.extra.mongodb.bson;

import static org.jongo.MongoCollection.MONGO_QUERY_OID;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.bson.types.MaxKey;
import org.bson.types.MinKey;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

import de.undercouch.bson4jackson.types.ObjectId;

public class BsonDeserializers extends SimpleDeserializers {

    public BsonDeserializers() {
        addDeserializer(Date.class, new DateDeserializer());

      //  addDeserializer(String.class, new ObjectIdDeserializer());
        NativeDeserializer nativeDeserializer = new NativeDeserializer();
        addDeserializer(MinKey.class, new MinKeyDeserializer());
        addDeserializer(MaxKey.class, new MaxKeyDeserializer());
        addDeserializer(DBObject.class, nativeDeserializer);
      addDeserializer(String.class, new CustomStringDeserializer());


    }

    public static class CustomStringDeserializer extends JsonDeserializer<String> {
        // implements ContextualDeserializer

//        public CustomStringDeserializer() {
//            super(String.class);
//        }

        // public JsonDeserializer<String> createContextual(DeserializationContext ctxt, BeanProperty
        // property)
        // throws JsonMappingException {
        // // TODO Auto-generated method stub
        // return null;
        // }

        private StringDeserializer defaultStringDeserializer = new StringDeserializer();

        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            
//            TreeNode treeNode = jp.readValueAsTree();
//            JsonNode oid = ((JsonNode) treeNode).get(MONGO_QUERY_OID);
//            if (oid != null){
//                return oid.asText();
//            }else if(false){
//                return treeNode.toString();
//            }
            
            Object deserialized = jp.getEmbeddedObject();
            if(deserialized instanceof String){
                return (String)deserialized;
            }else  if (deserialized instanceof org.bson.types.ObjectId) {
                return ((org.bson.types.ObjectId)deserialized).toString();
            }else  if (deserialized instanceof de.undercouch.bson4jackson.types.ObjectId) {
                return (convertToNativeObjectId((de.undercouch.bson4jackson.types.ObjectId)deserialized)).toString();
            }
      
//       
//            if (deserialized instanceof de.undercouch.bson4jackson.types.ObjectId) {
//
//            }
            return defaultStringDeserializer.deserialize(jp, ctxt);
        }
        
        private org.bson.types.ObjectId convertToNativeObjectId(ObjectId id) {
            return new org.bson.types.ObjectId(id.getTime(), id.getMachine(), id.getInc());
        }

        // @Override
        // public Class<?> deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer
        // typeDeserializer)
        // throws IOException, JsonProcessingException
        // {
        // System.out.println("ClassDeserializer.deserializeWithType called");
        // return null;
        // }
    }

    private static class DateDeserializer extends JsonDeserializer<Date> {

        @Override
        public Date deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            Object deserialized = jp.getEmbeddedObject();
            if (deserialized instanceof Long) {
                return getDateFromBackwardFormat((Long) deserialized);
            } else if (deserialized instanceof String) {
                try {
                    return DateUtils.parseDate((String) deserialized, new String[] { "yyyy-MM-dd",
                            "yyyMMdd" });
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace(); // FIXME
                }
            }
            return (Date) deserialized;
        }

        private Date getDateFromBackwardFormat(Long deserialized) {
            return new Date(deserialized);
        }
    }

    private static class MinKeyDeserializer extends JsonDeserializer<MinKey> {
        @Override
        public MinKey deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            return new MinKey();
        }
    }

    private static class MaxKeyDeserializer extends JsonDeserializer<MaxKey> {
        @Override
        public MaxKey deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            return new MaxKey();
        }
    }

    private static class NativeDeserializer<T> extends JsonDeserializer<T> {
        @Override
        public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            String asString = jp.readValueAsTree().toString();
            return (T) JSON.parse(asString);
        }
    }

}
