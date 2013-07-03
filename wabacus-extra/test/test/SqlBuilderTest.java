package test;

import org.jooq.SQLDialect;
import org.jooq.impl.Factory;


public class SqlBuilderTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        Factory factory = new Factory(SQLDialect.valueOf("MYSQL"));
        String sql = factory.select().from("tab1").where(Factory.condition("A like %?%", "'B' and")).toString();
     
        
        System.out.println("sql:"+sql);

    }
}
