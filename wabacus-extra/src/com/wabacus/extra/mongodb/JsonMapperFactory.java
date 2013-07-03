package com.wabacus.extra.mongodb;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.MapperFeature.AUTO_DETECT_GETTERS;
import static com.fasterxml.jackson.databind.MapperFeature.AUTO_DETECT_SETTERS;

import java.io.IOException;
import java.util.Date;

import org.bson.types.ObjectId;
import org.jongo.Mapper;
import org.jongo.marshall.jackson.JacksonMapper;
import org.jongo.marshall.jackson.bson4jackson.MongoBsonFactory;
import org.jongo.marshall.jackson.configuration.PropertyModifier;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.mongodb.util.JSON;
import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.extra.mongodb.bson.BsonDeserializers;
import com.wabacus.extra.mongodb.bson.CustomBsonModule;

/**
 * Factory
 *
 * @version $Id: JsonMapperFactory.java 3638 2013-05-12 15:10:22Z qxo $
 * @author qxo(qxodream@gmail.com) 
 * @since  2013-1-22
 */
public final class JsonMapperFactory {

	private static ObjectMapper jacksonMapper = null;

	public static ObjectMapper getJsonMapper() {
		if (jacksonMapper == null) {
			jacksonMapper = createPreConfiguredMapper(null); //外部可配置
		}
		return jacksonMapper;
	}
	
	public static Mapper createMapper() {
		return new JacksonMapper.Builder(createPreConfiguredMapper(MongoBsonFactory.createFactory()))
		//   .addDeserializer(Date.class, new DateDeserializer())
		.registerModule(createBsonModule())
	
		.setVisibilityChecker(VisibilityChecker.Std.defaultInstance().withFieldVisibility(ANY))
		.addModifier(new PropertyModifier())
				.build();
	}

    public static CustomBsonModule createBsonModule() {
        CustomBsonModule model = new CustomBsonModule();
        return model;
    }

	private static Mapper jongoMapper;

	public static Mapper getJongoMapper() {

	    
	    if (jongoMapper == null) {
			jongoMapper = createMapper();
		}
		return jongoMapper;
	}

	public static ObjectMapper createPreConfiguredMapper(JsonFactory fy) {
		// final BsonFactory fy = MongoBsonFactory.createFactory();
		final ObjectMapper mapper = null == fy ? new ObjectMapper() : new ObjectMapper(fy);
		//mapper.registerModule(new BsonModule());
		mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(AUTO_DETECT_GETTERS, false);
		mapper.configure(AUTO_DETECT_SETTERS, true);
		mapper.setSerializationInclusion(NON_NULL);
	//	mapper.setVisibilityChecker(VisibilityChecker.Std.defaultInstance().withFieldVisibility(ANY));
		mapper.registerModule(new JodaModule());
		
		mapper.enable(MapperFeature.AUTO_DETECT_SETTERS);
	
		final SimpleModule module = new SimpleModule("jongo", new Version(1, 0, 0, null, null, null));
		// addBSONTypeSerializers(module)
		module.addSerializer(ObjectId.class, new BsonObjectIdSerializer());
		module.addDeserializer(String.class, new BsonDeserializers.CustomStringDeserializer());
		
	//	  module.addDeserializer(AbsReportDataPojo.class, new BsonDeserializers.AbsReportDataPojoDeserializer());
		    
		  
		mapper.registerModule(module);
		return mapper;
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

	// private static void addBSONTypeSerializers(SimpleModule module)
	// {
	// NativeSerializer serializer=new NativeSerializer();
	// NativeDeserializer deserializer=new NativeDeserializer();
	// for(Class primitive:BSONPrimitives.getPrimitives())
	// {
	// module.addSerializer(primitive,serializer);
	// // module.addDeserializer(primitive,deserializer);
	// }
	// // module.addDeserializer(Date.class,new BackwardDateDeserializer(deserializer));
	// }
	//
	public static class NativeDeserializer extends JsonDeserializer<Object> {

		@Override
		public Object deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			String asString = jp.readValueAsTree().toString();
			return JSON.parse(asString);
		}
	}

	// public static class ObjectIdDeserializer extends JsonDeserializer<ObjectId>
	// {
	//
	// @Override
	// public ObjectId deserialize(JsonParser jp,DeserializationContext ctxt) throws
	// IOException,JsonProcessingException
	// {
	// String asString=jp.readValueAsTree().toString();
	// return JSON.parse(asString);
	// }
	// }

	public static class NativeSerializer extends JsonSerializer<Object> {

		public void serialize(Object obj, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException,
				JsonProcessingException {
			jsonGenerator.writeRawValue(JSON.serialize(obj));
		}
	}

	static class BackwardDateDeserializer extends JsonDeserializer<Date> {

		private final NativeDeserializer deserializer;

		public BackwardDateDeserializer(NativeDeserializer deserializer) {
			this.deserializer = deserializer;
		}

		@Override
		public Date deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			Object deserializedDate = parse(jp, ctxt);
			if (deserializedDate instanceof Long) {
				return new Date((Long) deserializedDate);
			}
			return (Date) deserializedDate;
		}

		private Object parse(JsonParser jp, DeserializationContext ctxt) throws IOException {
			return deserializer.deserialize(jp, ctxt);
		}
	}

}
