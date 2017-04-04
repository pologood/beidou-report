/**
 * ReportCproPlanMgrImplTest.java  2011-12-4
 *
 * Copyright 2011 Baidu, Inc. All rights reserved.
 * Baidu PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.baidu.beidou.report.service.impl;

import java.text.ParseException;

import org.junit.Before;

import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.vo.QueryParameter;
import com.baidu.beidou.stat.driver.bo.Constant;
import com.baidu.beidou.test.BaseJMockTest;
import com.opensymphony.xwork2.TextProvider;

/**
 * @author kanghongwei
 * 
 */
public class ReportCproPlanMgrImplTest  extends BaseJMockTest{
    
    private ReportCproPlanMgrImpl reportCproPlanMgrImpl;
	private TextProvider mockTextProvider;

	// 公共单测参数
	private final int userid = 1;
	private final int timeunit = ReportConstants.TU_DAY;
	private final int type = Constant.REPORT_TYPE_DEFAULT;
	private QueryParameter queryParameter = new QueryParameter();

	@Before
	public void beforeEach() throws ParseException {
		reportCproPlanMgrImpl = new ReportCproPlanMgrImpl();

		mockTextProvider = context.mock(TextProvider.class);
	}

//	@Test
//	public void testFindUserAccountStatData() throws ParseException {
//		// mock参数/返回值的构造
//		final Date from = DateUtils.strToDate("20111127");
//		final Date to = DateUtils.strToDate("20111203");
//		final List<Map<String, Object>> mockResultList = constructMockResultList();
//		context.checking(new Expectations() {
//			{
//				oneOf(mockStatService2).queryAUserData(userid, from, to, timeunit, type);
//				will(returnValue(mockResultList));
//			}
//		});
//
//		queryParameter.setUserId(userid);
//		List<UserAccountViewItem> funkResultList = reportCproPlanMgrImpl.findUserAccountStatData(
//				queryParameter, from, to);
//		// 数据验证
//		assertThat(funkResultList.size(), is(7));
//		assertThat(funkResultList.get(0).getShowDate(), equalToIgnoringCase("2011-11-27"));
//		assertThat(funkResultList.get(0).getSrchs(), is(100L));
//		assertThat(funkResultList.get(0).getClks(), is(5L));
//		assertThat(funkResultList.get(0).getCost(), closeTo(300d / 100.0d, 0.001d));
//		assertThat(funkResultList.get(0).getCtr().doubleValue(), closeTo(0.05d, 0.001d));
//		assertThat(funkResultList.get(0).getAcp().doubleValue(), closeTo(0.6d, 0.001d));
//		assertThat(funkResultList.get(0).getCpm().doubleValue(), closeTo(30d, 0.001d));
//
//		assertThat(funkResultList.get(6).getShowDate(), equalToIgnoringCase("2011-12-03"));
//		assertThat(funkResultList.get(6).getSrchs(), is(50L));
//		assertThat(funkResultList.get(6).getClks(), is(3L));
//		assertThat(funkResultList.get(6).getCost(), closeTo(240d / 100.0d, 0.001d));
//		assertThat(funkResultList.get(6).getCtr().doubleValue(), closeTo(0.06d, 0.001d));
//		assertThat(funkResultList.get(6).getAcp().doubleValue(), closeTo(0.8d, 0.001d));
//		assertThat(funkResultList.get(6).getCpm().doubleValue(), closeTo(48d, 0.001d));
//	}
//
//	private List<Map<String, Object>> constructMockResultList() throws ParseException {
//		List<Map<String, Object>> mockResultList = new ArrayList<Map<String, Object>>();
//		mockResultList.add(constructDorisReturnMap(DateUtils.strToDate("20111127"), DateUtils
//				.strToDate("20111127"), 100, 5, 300, 0.05, 0.6, 30));
//		mockResultList.add(constructDorisReturnMap(DateUtils.strToDate("20111128"), DateUtils
//				.strToDate("20111128"), 100, 8, 500, 0.08, 0.625, 50));
//		mockResultList.add(constructDorisReturnMap(DateUtils.strToDate("20111129"), DateUtils
//				.strToDate("20111129"), 200, 6, 800, 0.03, 1.333, 40));
//		mockResultList.add(constructDorisReturnMap(DateUtils.strToDate("20111130"), DateUtils
//				.strToDate("20111130"), 300, 50, 1000, 0.167, 0.2, 33.3));
//		mockResultList.add(constructDorisReturnMap(DateUtils.strToDate("20111201"), DateUtils
//				.strToDate("20111201"), 500, 100, 5420, 0.2, 0.542, 108.4));
//		mockResultList.add(constructDorisReturnMap(DateUtils.strToDate("20111202"), DateUtils
//				.strToDate("20111202"), 1000, 350, 24500, 0.35, 0.7, 245));
//		mockResultList.add(constructDorisReturnMap(DateUtils.strToDate("20111203"), DateUtils
//				.strToDate("20111203"), 50, 3, 240, 0.06, 0.8, 48));
//		return mockResultList;
//	}
//
//	private Map<String, Object> constructDorisReturnMap(Date fromdate, Date todate, long srchs,
//			long clks, double cost, double ctr, double acp, double cpm) {
//		Map<String, Object> dorisMap = new HashMap<String, Object>();
//		dorisMap.put("fromdate", fromdate);
//		dorisMap.put("todate", todate);
//		dorisMap.put("srchs", srchs);
//		dorisMap.put("clks", clks);
//		dorisMap.put("cost", cost);// 单位为“分”
//		dorisMap.put("ctr", ctr);
//		dorisMap.put("acp", acp);
//		dorisMap.put("cpm", cpm);
//		return dorisMap;
//	}
//
//	@Test
//	public void testSumUserAccountStatData() throws ParseException {
//		// mock参数/返回值的构造
//		final Date from = DateUtils.strToDate("20111127");
//		final Date to = DateUtils.strToDate("20111203");
//		final List<Map<String, Object>> mockResultList = constructMockResultList();
//		context.checking(new Expectations() {
//			{
//				oneOf(mockStatService2).queryAUserData(userid, from, to, timeunit, type);
//				will(returnValue(mockResultList));
//			}
//		});
//		queryParameter.setUserId(userid);
//		List<UserAccountViewItem> funkResultList = reportCproPlanMgrImpl.findUserAccountStatData(
//				queryParameter, from, to);
//		UserAccountViewItem sumViewItem = reportCproPlanMgrImpl
//				.sumUserAccountStatData(funkResultList);
//		// 数据验证
//		assertThat(sumViewItem.getNeedFrontSort(), is(1));
//		assertThat(sumViewItem.getShowDate(), nullValue());
//		assertThat(sumViewItem.getSrchs(), is(2250L));
//		assertThat(sumViewItem.getClks(), is(522L));
//		assertThat(sumViewItem.getCost(), closeTo(32760d / 100.0d, 0.001d));
//		assertThat(sumViewItem.getCtr().doubleValue(), closeTo(0.232d, 0.001d));
//		assertThat(sumViewItem.getAcp().doubleValue(), closeTo(0.627d, 0.001d));
//		assertThat(sumViewItem.getCpm().doubleValue(), closeTo(145.6d, 0.001d));
//	}
//
//	@Test
//	public void testPageUserAccountData() throws ParseException {
//		// mock参数/返回值的构造
//		final Date from = DateUtils.strToDate("20111127");
//		final Date to = DateUtils.strToDate("20111203");
//		final List<Map<String, Object>> mockResultList = constructMockResultList();
//		context.checking(new Expectations() {
//			{
//				oneOf(mockStatService2).queryAUserData(userid, from, to, timeunit, type);
//				will(returnValue(mockResultList));
//			}
//		});
//		// 分页参数准备
//		int page = 1;
//		int pageSize = 3;
//		String orderBy = "showDate";
//		String order = BeidouConstant.SORTORDER_DESC;
//
//		// 数据查询参数准备
//		queryParameter.setUserId(userid);
//		queryParameter.setPage(page);
//		queryParameter.setPageSize(pageSize);
//		queryParameter.setOrderBy(orderBy);
//		queryParameter.setOrder(order);
//		// 后端分页的条件
//		ReportWebConstants.FRONT_SORT_THRESHOLD = 5;
//		List<UserAccountViewItem> funkResultList = reportCproPlanMgrImpl.findUserAccountStatData(
//				queryParameter, from, to);
//		List<UserAccountViewItem> afterPagedList = reportCproPlanMgrImpl.pageUserAccountData(
//				funkResultList, queryParameter);
//		// 数据验证
//		assertThat(afterPagedList.size(), is(pageSize));
//		assertThat(afterPagedList.get(1).getShowDate(), equalToIgnoringCase("2011-12-02"));
//		assertThat(funkResultList.get(1).getSrchs(), is(1000L));
//		assertThat(funkResultList.get(1).getClks(), is(350L));
//		assertThat(funkResultList.get(1).getCost(), closeTo(24500d / 100.0d, 0.001d));
//		assertThat(funkResultList.get(1).getCtr().doubleValue(), closeTo(0.35d, 0.001d));
//		assertThat(funkResultList.get(1).getAcp().doubleValue(), closeTo(0.7d, 0.001d));
//		assertThat(funkResultList.get(1).getCpm().doubleValue(), closeTo(245d, 0.001d));
//	}
//
//	@Test
//	public void testGetDownloadAccountReportVo() throws ParseException {
//		// mock参数/返回值的构造
//		final Date from = DateUtils.strToDate("20111127");
//		final Date to = DateUtils.strToDate("20111203");
//		final List<Map<String, Object>> mockResultList = constructMockResultList();
//		context.checking(new Expectations() {
//			{
//				oneOf(mockStatService2).queryAUserData(userid, from, to, timeunit, type);
//				will(returnValue(mockResultList));
//			}
//		});
//		queryParameter.setUserId(userid);
//		List<UserAccountViewItem> funkResultList = reportCproPlanMgrImpl.findUserAccountStatData(
//				queryParameter, from, to);
//		UserAccountViewItem sumViewItem = reportCproPlanMgrImpl
//				.sumUserAccountStatData(funkResultList);
//		// 函数调用参数
//		User user = new User();
//		user.setUsername("测试下载分日报告");
//
//		// mock掉TextProvider
//		final String download_account_report_account = "download.account.report.account";
//		final String return_download_account_report_account = "账户";
//		final String download_account_report = "download.account.report";
//		final String return_download_account_report = "报告：";
//		final String download_account_account = "download.account.account";
//		final String return_download_account_account = "账户：";
//		final String download_account_daterange = "download.account.daterange";
//		final String return_download_account_daterange = "日期范围：";
//		final String download_account_level_account = "download.account.level.account";
//		final String return_download_account_level_account = "账户";
//		final String download_account_level = "download.account.level";
//		final String return_download_account_level = "所属层级：";
//		final String download_account_head_col = "download.account.head.col";
//		final String return_download_account_head_col = "日期,展现次数,点击次数,点击率,平均点击价格,千次展现成本,总费用";
//		final String download_summary = "download.summary";
//		final String return_download_summary = "合计：";
//		context.checking(new Expectations() {
//			{
//				oneOf(mockTextProvider).getText(download_account_report_account);
//				will(returnValue(return_download_account_report_account));
//
//				oneOf(mockTextProvider).getText(download_account_report);
//				will(returnValue(return_download_account_report));
//
//				oneOf(mockTextProvider).getText(download_account_account);
//				will(returnValue(return_download_account_account));
//
//				oneOf(mockTextProvider).getText(download_account_daterange);
//				will(returnValue(return_download_account_daterange));
//
//				oneOf(mockTextProvider).getText(download_account_level_account);
//				will(returnValue(return_download_account_level_account));
//
//				oneOf(mockTextProvider).getText(download_account_level);
//				will(returnValue(return_download_account_level));
//
//				oneOf(mockTextProvider).getText(download_account_head_col);
//				will(returnValue(return_download_account_head_col));
//
//				oneOf(mockTextProvider).getText(download_summary);
//				will(returnValue(return_download_summary));
//			}
//		});
//
//		UserAccountReportVo reportVo = reportCproPlanMgrImpl.getDownloadAccountReportVo(user,
//				funkResultList, sumViewItem, mockTextProvider, getStrDate(from), getStrDate(to));
//		assertThat(reportVo.getAccountInfo(), notNullValue());
//		// 验证账户信息
//		assertThat(reportVo.getAccountInfo().getReport(),
//				equalToIgnoringCase(return_download_account_report_account));
//		assertThat(reportVo.getAccountInfo().getReportText(),
//				equalToIgnoringCase(return_download_account_report));
//		assertThat(reportVo.getAccountInfo().getAccount(), equalToIgnoringCase(user.getUsername()));
//		assertThat(reportVo.getAccountInfo().getAccountText(),
//				equalToIgnoringCase(return_download_account_account));
//		assertThat(reportVo.getAccountInfo().getDateRange(),
//				equalToIgnoringCase("2011-11-27 - 2011-12-03"));
//		assertThat(reportVo.getAccountInfo().getDateRangeText(),
//				equalToIgnoringCase(return_download_account_daterange));
//		assertThat(reportVo.getAccountInfo().getLevel(),
//				equalToIgnoringCase(return_download_account_level_account));
//		assertThat(reportVo.getAccountInfo().getLevelText(),
//				equalToIgnoringCase(return_download_account_level));
//		// 验证消息头
//		String[] checkHeaderArray = return_download_account_head_col.split(",");
//		assertThat(reportVo.getHeaders()[0], equalToIgnoringCase(checkHeaderArray[0]));
//		assertThat(reportVo.getHeaders()[1], equalToIgnoringCase(checkHeaderArray[1]));
//		assertThat(reportVo.getHeaders()[2], equalToIgnoringCase(checkHeaderArray[2]));
//		assertThat(reportVo.getHeaders()[3], equalToIgnoringCase(checkHeaderArray[3]));
//		assertThat(reportVo.getHeaders()[4], equalToIgnoringCase(checkHeaderArray[4]));
//		assertThat(reportVo.getHeaders()[5], equalToIgnoringCase(checkHeaderArray[5]));
//		assertThat(reportVo.getHeaders()[6], equalToIgnoringCase(checkHeaderArray[6]));
//		// 验证数据明细：下载需要获取全部数据，且按日期顺序排序
//		assertThat(reportVo.getDetails().size(), is(7));
//		assertThat(reportVo.getDetails().get(5).getShowDate(), equalToIgnoringCase("2011-12-02"));
//		assertThat(reportVo.getDetails().get(5).getSrchs(), is(1000L));
//		assertThat(reportVo.getDetails().get(5).getClks(), is(350L));
//		assertThat(reportVo.getDetails().get(5).getCost(), closeTo(24500d / 100.0d, 0.001d));
//		assertThat(reportVo.getDetails().get(5).getCtr().doubleValue(), closeTo(0.35d, 0.001d));
//		assertThat(reportVo.getDetails().get(5).getAcp().doubleValue(), closeTo(0.7d, 0.001d));
//		assertThat(reportVo.getDetails().get(5).getCpm().doubleValue(), closeTo(245d, 0.001d));
//		// 验证汇总数据
//		assertThat(reportVo.getSummary().getSummaryText(),
//				equalToIgnoringCase(return_download_summary));
//		assertThat(reportVo.getSummary().getSrchs(), is(2250L));
//		assertThat(reportVo.getSummary().getClks(), is(522L));
//		assertThat(reportVo.getSummary().getCost(), closeTo(32760d / 100.0d, 0.001d));
//		assertThat(reportVo.getSummary().getCtr().doubleValue(), closeTo(0.232d, 0.001d));
//		assertThat(reportVo.getSummary().getAcp().doubleValue(), closeTo(0.627d, 0.001d));
//		assertThat(reportVo.getSummary().getCpm().doubleValue(), closeTo(145.6d, 0.001d));
//	}
//
//	private String getStrDate(Date date) {
//		Format format = new SimpleDateFormat("yyyy-MM-dd");
//		return format.format(date);
//	}
}
