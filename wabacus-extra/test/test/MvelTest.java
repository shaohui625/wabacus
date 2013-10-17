package test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.mvel2.MVEL;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.PropertyHandlerFactory;
import org.mvel2.integration.VariableResolverFactory;

public class MvelTest {
    
  public static interface I {
       String getName();
    }

   public static class A1 implements I{

        public String getName() {
            return "A1";
        }
        public String getName2() {
            return "A1";
        }
        
    }
    
    public I getFacade(){
        return new A1();
    }
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        System.out.println("test:"+Arrays.asList("-A,B".split("[\\-,]+")));
        
        Map vars = new HashMap();
        vars.put("facade1", new A1());
       // MVEL.eval("facade1.getName2()", new MvelTest() , vars);
        
//        PropertyHandlerFactory.registerPropertyHandler(I.class, new PropertyHandler(){
//
//            public Object getProperty(String name, Object contextObj,
//                    VariableResolverFactory variableFactory) {
//                return MVEL.getProperty(name,contextObj);
//            }
//
//            public Object setProperty(String name, Object contextObj,
//                    VariableResolverFactory variableFactory, Object value) {
//              
//                 MVEL.setProperty(contextObj, name, value);
//                 return value;
//            }
//            
//        });
        Serializable compileExpression = MVEL.compileExpression("this.getFacade().getName2()");
      System.out.println(MVEL.executeExpression(compileExpression,new MvelTest() , vars));
     //   MVEL.eval("this.getFacade().getName2()", new MvelTest() , vars);

    }
}
