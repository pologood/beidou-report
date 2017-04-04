package com.baidu.beidou.report.service.impl;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.transaction.TransactionConfiguration;

import com.baidu.beidou.test.ParameterReader;
import com.baidu.beidou.util.StringUtils;
import com.baidu.beidou.util.dao.BaseDaoSupport;
import com.baidu.gson.Gson;
import com.baidu.gson.reflect.TypeToken;

@TransactionConfiguration(transactionManager="addbTransactionManager")
public abstract class StatServiceImplTestBase extends AbstractTransactionalJUnit4SpringContextTests{
	
	protected Log log = LogFactory.getLog(this.getClass());
	
	@Autowired
	@Qualifier("thirdpartDao")
	BaseDaoSupport thirdpartDao;
	
	protected Map<String, String> in;
	protected Map<String, String> out;
	
	/**
	 * 存放临时备份表的名称
	 */
	private String tmpTableName; 
	
	public StatServiceImplTestBase(String method, String in,String out) {
		Type type = new TypeToken<Map<String, String>>(){}.getType();
		this.in = (Map<String, String>)new Gson().fromJson(in, type);
		this.out = (Map<String, String>)new Gson().fromJson(out, type);
	}

	//设置datasource	
	@Autowired
	public void setDataSource(@Qualifier("thirdpartDataSource")
	DataSource dataSource) {
		super.setDataSource(dataSource);
	}
	
	@Before
	public void initData() throws Exception {
		
		//传入0表示不进行列数验证
		String initDataFileSuffix = in.get("dataFileSuffix");
		if (StringUtils.isEmpty(initDataFileSuffix)) {
			log.warn("are you sure there is no initial data?");
			return ;
		}
		String filePath = ParameterReader.getTestDataPathByClass(this.getClass());
		List<String[]> inputs = ParameterReader.loadParameterFromFile(filePath + initDataFileSuffix, ParameterReader.INVALID_EXPECT_COLUMN_NUM);
		
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ");
		sb.append(getTableName());
		sb.append(" values ");
		for (String[] fields : inputs) {
			sb.append("(");
			for (String field : fields){
				sb.append(field);
				sb.append(",");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append("),");
		}
		sb.deleteCharAt(sb.length() - 1);		
		sb.append(";");
		log.info("Init Sql:" + sb.toString());
		
		//1、备份数据；

		tmpTableName = getTableName() + "_" + System.currentTimeMillis();
		String backUpSql = "create table "  + tmpTableName + " as select * from " + getTableName();
		thirdpartDao.excuteSql(backUpSql);
		//2、清空数据
		thirdpartDao.truncateTable(getTableName());
		
		//3、执行数据导入
		thirdpartDao.excuteSql(sb.toString());
		
	}
	
	@After
	public void restoreData() throws Exception {
		//4、恢复数据(先truncate、再insert select、再drop)

		thirdpartDao.truncateTable(getTableName());
		String sql = "insert into " + getTableName() + " select * from " + tmpTableName;
		thirdpartDao.excuteSql(sql);
		sql = "drop table "  + tmpTableName;
		thirdpartDao.excuteSql(sql);
	}
	
	/**
	 * @return 填入查询的表名
	 */
	protected abstract String getTableName() ;
}
