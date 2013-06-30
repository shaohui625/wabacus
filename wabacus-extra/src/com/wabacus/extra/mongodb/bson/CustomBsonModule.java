package com.wabacus.extra.mongodb.bson;

import java.io.IOException;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import de.undercouch.bson4jackson.serializers.BsonSerializers;

public class CustomBsonModule extends de.undercouch.bson4jackson.BsonModule {

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        BsonSerializers bsonSerializers = new BsonSerializers();
        bsonSerializers.addSerializer(ObjectId.class, new BsonObjectIdSerializer());
        context.addSerializers(bsonSerializers);
        BsonDeserializers d = new BsonDeserializers();
        
        context.addDeserializers(d);
    }
    
    
   public static class BsonObjectIdSerializer extends JsonSerializer<ObjectId>
    {
      public void serialize(ObjectId obj, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
        throws IOException, JsonProcessingException
      {
         // System.out.println("obj:"+obj);
          jsonGenerator.writeStartObject();
          jsonGenerator.writeFieldName("$oid");
          jsonGenerator.writeString(obj.toString());
          
          jsonGenerator.writeEndObject();
      //  ((MongoBsonGenerator)jsonGenerator).writeNativeObjectId(obj);
      }
    }
}
