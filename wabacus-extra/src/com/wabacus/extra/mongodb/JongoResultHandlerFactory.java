package com.wabacus.extra.mongodb;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.DateConverter;
import org.apache.commons.lang.exception.NestableRuntimeException;
import org.jongo.ResultHandler;
import org.jongo.bson.BsonDocument;
import org.jongo.marshall.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DBObject;

public class JongoResultHandlerFactory {
	/**
	 * Logger for this class
	 */
	private static final Logger LOG = LoggerFactory.getLogger(JongoResultHandlerFactory.class);

	static {

		ConvertUtils.deregister(Date.class);

		final DateConverter converter = new DateConverter(null);
		converter.setUseLocaleFormat(true);
		converter.setPatterns(new String[] { "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm","yyyy-MM-dd"});
		ConvertUtils.register(converter, Date.class);

	}

	public final static <T> ResultHandler<T> newMapper(final Class<T> clazz) {
		return new ResultHandler<T>() {
			public T map(DBObject result) {
				
				//LOG.debug("result:{}", result);
				Map map = null;
				
				
				if (result instanceof BsonDocument) {
					
						//return  JsonMapperFactory.getJsonMapper().readValue(((BsonDocument) result).toByteArray(), clazz);
					    final T ret = getUnmarshaller().unmarshall(((BsonDocument) result), clazz);
						return ret;
//						try {
//						return  JsonMapperFactory.getJsonMapper().readValue(result.toString(), clazz);
//					} catch (JsonParseException e) {
//						throw new NestableRuntimeException(e);
//					} catch (JsonMappingException e) {
//						throw new NestableRuntimeException(e);
//					} catch (IOException e) {
//						throw new NestableRuntimeException(e);
//					}
				}else{
					map = result.toMap();
				}
				T ret;
				try {
					ret = clazz.newInstance();
					BeanUtils.copyProperties(ret, map);
					return ret;
				} catch (InstantiationException e) {
					throw new NestableRuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new NestableRuntimeException(e);

				} catch (InvocationTargetException e) {
					throw new NestableRuntimeException(e);
				}

				// final String json=JsonMapperFactory.getJsonMapper().writeValueAsString(map);
				// return unmarshaller.unmarshall(result.toString(), clazz);
			}

			protected Unmarshaller getUnmarshaller() {
				return JsonMapperFactory.getJongoMapper().getUnmarshaller();
			}
		};
	}
}
