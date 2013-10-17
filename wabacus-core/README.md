wabacus-core
===========
框架扩展性修改点
1. 增加AbstractJdbcDatabaseType,所有关系数据类库(jdbc)的AbsDatabaseType都继承它,以便扩展原来AbsDatabaseType支持非关系数据库
2. 增加AbstractJdbcDataSource,所有关系数据类库(jdbc)的数据源(AbsDataSource)都继承它,以便扩展原来AbsDataSource支持非jdbc数据源
3.

可扩展性修改点的正则表达式
//[$]ByQXO.*//ByQXO[$]  ==> //$ByQXO   ... //ByQXO$

其中位置文件头的表示为新增文件的格式如下:"//$ByQXO NEW       //ByQXO$"



系统


mvn clean source:jar deploy  -DskipTests=true -Dgpg.skip=true -DaltDeploymentRepository=branchitech-maven::default::http://service.branchitech.com/artifactory/libs-releases-local
