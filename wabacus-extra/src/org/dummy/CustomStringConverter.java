package org.dummy;

import org.apache.commons.beanutils.converters.AbstractConverter;

import de.undercouch.bson4jackson.types.ObjectId;

public final class CustomStringConverter  extends AbstractConverter {

    @Override
    protected Object convertToType(Class type, Object value) throws Throwable {
        return value != null ? value.toString() : null;
    }

    @Override
    protected Class getDefaultType() {
         return String.class;
    }
    
    @Override
    protected String convertToString(Object value) throws Throwable {
        if(value instanceof  de.undercouch.bson4jackson.types.ObjectId){
           return  convertToNativeObjectId ((de.undercouch.bson4jackson.types.ObjectId)value).toString();
        }
        return value.toString();
    }
    
    private org.bson.types.ObjectId convertToNativeObjectId(ObjectId id) {
        return new org.bson.types.ObjectId(id.getTime(), id.getMachine(), id.getInc());
    }
    
}