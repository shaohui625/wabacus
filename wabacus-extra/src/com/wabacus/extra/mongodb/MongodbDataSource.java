package com.wabacus.extra.mongodb;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.dom4j.Element;
import org.jongo.Jongo;
import org.jongo.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.wabacus.config.database.datasource.AbsDataSource;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.extra.WabacusBeanFactory;
import com.wabacus.system.IConnection;
import com.wabacus.util.DesEncryptTools;

public class MongodbDataSource extends AbsDataSource {
	/**
	 * Logger for this class
	 */
	private static final Logger LOG = LoggerFactory.getLogger(MongodbDataSource.class);

	private String serverAddress;

	private String dbName;

	private Mongo mongo;

	private boolean lazy = true;

	private List<ServerAddress> saList = new ArrayList();

	@Override
	public void loadConfig(Element eleDataSource) {
		super.loadConfig(eleDataSource);

		List lstEleProperties = eleDataSource.elements("property");

		if (lstEleProperties == null || lstEleProperties.size() == 0) {
			throw new WabacusConfigLoadingException("没有为数据源：" + this.getName() + "配置alias、configfile等参数");
		}

		for (int i = 0; i < lstEleProperties.size(); i++) {
			final Element eleChild = (Element) lstEleProperties.get(i);
			String name = eleChild.attributeValue("name");
			name = name == null ? "" : name.trim();

			String value = this.getOverridePropertyValue(name, eleChild.getText());

			value = value == null ? "" : value.trim();
			if (value.equals("")) {
				continue;
			}
			if (name.equals("dbName")) {
				dbName = value;
			} else if (name.equals("serverAddress")) {
				serverAddress = value;
			} else if (name.equals("username")) {
				username = value;
			} else if (name.equals("password")) {
				password = value;

			} else if (name.equals("lazy")) {
				lazy = Boolean.parseBoolean(value);
			}

		}
		LOG.info("serverAddress:{} dbName:{},username:{}",new Object[]{serverAddress,dbName,password});
		if (password != null && password.startsWith("{3DES}")) {
			password = password.substring("{3DES}".length());
			if (DesEncryptTools.KEY_OBJ == null) {
				throw new WabacusConfigLoadingException("没有取到密钥文件，无法完成数据库密码解密操作");
			}
			password = DesEncryptTools.decrypt(password);
		}

		final String[] aList = serverAddress.split("[;,]");

		for (int i = 0; i < aList.length; i++) {
			String str = aList[i];
			if (StringUtils.isBlank(str)) {
				continue;
			}
			final String[] hostAndPort = str.trim().split("[:]");
			ServerAddress sa;
			try {
				sa = hostAndPort.length > 1 ? new ServerAddress(hostAndPort[0], NumberUtils.toInt(hostAndPort[1]))
						: new ServerAddress(hostAndPort[0]);
				saList.add(sa);
			} catch (UnknownHostException e) {
				throw new IllegalArgumentException(e.getMessage(), e);
			}
		}

		if (!lazy && saList.size() > 0) {
			mongo = new Mongo(saList);
		}
	}

	private String password;

	private String username;

	public Mongo getMongo() {
		if (null == mongo) {
			mongo = new Mongo(saList);
		}
		return mongo;
	}

	@Override
	public IConnection getIConnection() {
		Jongo jongo = getJongo();
		return new MongodbConnection(jongo);
	}

	private Jongo jongo;

	private Jongo getJongo() {
		if (null == jongo) {
			final Mapper mapper  = JsonMapperFactory.getJongoMapper();
			final DB db = getMongoDB();
			jongo = new Jongo(db,mapper);
		}
		return jongo;
	}

	protected DB getMongoDB() {
		DB db = WabacusBeanFactory.getInstance().getBean(dbName);
		if (null != db) {
			return db;
		}
		db = getMongo().getDB(dbName);
		if (StringUtils.isNotEmpty(password) && !db.authenticate(username, password.toCharArray())) {
			throw new MongoException("unable to authenticate");
		}
		return db;
	}

	@Override
	public Connection getConnection() {
		throw new IllegalAccessError("NotImpl");
	}

}
