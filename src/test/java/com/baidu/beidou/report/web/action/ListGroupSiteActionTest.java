package com.baidu.beidou.report.web.action;

import java.util.Date;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.transaction.TransactionConfiguration;

import com.baidu.beidou.cprogroup.service.CproGroupConstantMgr;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.vo.QueryParameter;

@ContextConfiguration(locations = { "/applicationContext.xml" })
@TransactionConfiguration(transactionManager="xdbTransactionManager")
public class ListGroupSiteActionTest extends AbstractTransactionalJUnit4SpringContextTests {

	@Autowired
	ListGroupSiteAction listGroupSiteAction;

	@Autowired
	public void setDataSource(@Qualifier("xdbMultiDataSource") DataSource dataSource) {
		super.setDataSource(dataSource);
	}

	//		@Test
	public final void testAjaxList() {
		//	      初始化推广组配置
		CproGroupConstantMgr cproGroupConstantMgr = (CproGroupConstantMgr) applicationContext.getBean("cproGroupConstantMgr");
		cproGroupConstantMgr.loadSystemConfForReport();

		QueryParameter qp = new QueryParameter();
		qp.setDateStart("20120214");
		qp.setDateEnd("20120219");
		qp.setUserId(2259516);
		qp.setPlanId(581578);
		qp.setGroupId(1452922);
		qp.setOrderBy("srchs");
		qp.setOrder("desc");
		qp.setLevel(QueryParameterConstant.LEVEL.LEVEL_GROUP);
		//			qp.setKeyword("nvlady.com");

		//			qp.setSrchs(100000l);
		//			qp.setSrchsOp(1);

		ListGroupSiteAction action = listGroupSiteAction;
		action.setQp(qp);
		action.userId = 2259516;
		action.from = new Date(112, 1, 13);
		action.to = new Date(112, 1, 18);
		//			action.queryKeyword = "nvlady.com";
		action.ajaxListShownSite();
		System.out.println();
	}

	@Test
	public final void testAjaxFlashXml() {
		QueryParameter qp = new QueryParameter();
		qp.setDateStart("20081113");
		qp.setDateEnd("20100511");
		qp.setUserId(1131);
		qp.setSrchs(100000l);
		qp.setSrchsOp(1);

		ListGroupSiteAction action = listGroupSiteAction;
		action.setUserId(1131);
		action.setQp(qp);
		//			action.ajaxFlashXml();
	}
}
