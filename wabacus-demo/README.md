WabacusDemo
===========
通过启动时指属性或环境变量来指定全局配置文件位置
-DwabacusPropertyOverrideLoader=file://D:/tools/globalDir/WabacusGlobalConf.properties 

用此全局配置文件来覆盖reportconfig\wabacus.cfg.xml的相应配置

数据源的配置格式如下,其中ds_mysql为datasource的name
ds_mysql.url=jdbc:mysql://localhost/wabacusdemodb?useUnicode=true&characterEncoding=GBK
ds_mysql.user=root
ds_mysql.password=xxxxxxx

wabacus.system.show-sql=true