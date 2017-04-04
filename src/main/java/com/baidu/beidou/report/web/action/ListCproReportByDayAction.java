package com.baidu.beidou.report.web.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baidu.beidou.cprogroup.service.CproGroupMgr;
import com.baidu.beidou.cproplan.service.CproPlanMgr;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.report.service.ReportCproGroupMgr;
import com.baidu.beidou.report.service.ReportCproPlanMgr;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.olap.vo.ReportDayViewItem;
import com.baidu.beidou.report.vo.ReportDayVo;
import com.baidu.beidou.report.vo.ReportSumData;
import com.baidu.beidou.report.vo.plan.UserAccountReportVo;
import com.baidu.beidou.olap.vo.UserAccountViewItem;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.StringUtils;

/**
 * 为分日报告提供列表展现和列表下载的Action
 * @author zhuxiaoling since cpweb-550
 */

public class ListCproReportByDayAction extends BeidouReportActionSupport {

	private static final long serialVersionUID = 2527593402494681623L;
	
	private CproPlanMgr cproPlanMgr = null;
	private CproGroupMgr cproGroupMgr = null;
	private ReportCproPlanMgr reportCproPlanMgr = null;
	private ReportCproGroupMgr reportCproGroupMgr = null;
	
	@Override
	protected void initStateMapping() {
		if (qp!= null) {
			if (QueryParameterConstant.LEVEL.LEVEL_GROUP.equals(qp.getLevel())) {
				qp.setStateMapping(ReportWebConstants.GROUP_FRONT_BACKEND_STATE_MAPPING.get(qp.getViewState()));
			}
			else {
				qp.setStateMapping(ReportWebConstants.PLAN_FRONT_BACKEND_STATE_MAPPING.get(qp.getViewState()));
			}
		}
	}
	
	/**
	 * 生成各层级分日报告的Action方法
	 * @return
	 */
	public String ajaxListByDay() {
		try {
			//初始化参数
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);
			return SUCCESS;
		}
		
		/** 
		 * 如果统计时间混合今天和历史时间，则返回空
		 */
//		if(super.isMixTodayAndBefore(from, to)){
//			log.warn("分日报告不提供时间上跨越今天的统计数据");
//			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
//			jsonObject.setGlobalMsg(getText("mixed.report.not.allowed"));
//			return SUCCESS;
//		}
		
		//账户层级分日报告，复用网盟推广首页分日报告逻辑，增加根据统计字段筛选的逻辑
		if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(qp.getLevel())) {
			return accountReportByDay();
		}
		else if (QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(qp.getLevel())) {//推广计划层级
			return planReportByDay();
		}
		else if (QueryParameterConstant.LEVEL.LEVEL_GROUP.equals(qp.getLevel())) {//推广组层级
			return groupReportByDay();
		}	
		return SUCCESS;
	}
	
	/**
	 * 下载各层级分日报告的Action方法
	 * @return
	 */
	public String downloadByDay() {
		try {
			//初始化参数
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);
			return SUCCESS;
		}
		
		/** 
		 * 如果统计时间混合今天和历史时间，则返回空
		 */
//		if(super.isMixTodayAndBefore(from, to)){
//			log.warn("分日报告不提供时间上跨越今天的统计数据");
//			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
//			jsonObject.setGlobalMsg(getText("mixed.report.not.allowed"));
//			return SUCCESS;
//		}
		
		try {
			//账户层级分日报告
			if (QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(qp.getLevel())) {
				return downloadAccountByDay();
			}
			else if (QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(qp.getLevel())) {//推广计划层级
				return downloadPlanByDay();
			}
			else if (QueryParameterConstant.LEVEL.LEVEL_GROUP.equals(qp.getLevel())) {//推广组层级
				return downloadGroupByDay();
			}
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			return SUCCESS;
		}
		return SUCCESS;
	}
	
	// ****************************账户层级*****************************
	
	/**
	 * 获取账户层级分日报告，复用网盟推广首页分日报告逻辑，增加根据统计字段筛选的逻辑
	 * @return
	 */
	public String accountReportByDay() {
		// 数据准备
		List<UserAccountViewItem> userViewItemList = reportCproPlanMgr.findUserAccountStatData(qp, from, to);
		// 汇总
		UserAccountViewItem sumUserAccountItem = reportCproPlanMgr.sumUserAccountStatData(userViewItemList);
		// 总页数
		int totalPage = super.getTotalPage(userViewItemList.size());
		// 分页
		userViewItemList = reportCproPlanMgr.pageUserAccountData(userViewItemList, qp);
		
		/**
		 * 如果统计时间是今天，将统计数据列中点击、消费外的其他列置为-1
		 */
//		if(super.isOnlyToday(from, to)){
//			super.clearNotRealtimeStat(userViewItemList, sumUserAccountItem);
//		}
		
		jsonObject.addData("list", userViewItemList);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("cache", sumUserAccountItem.getNeedFrontSort());
		jsonObject.addData("sum", sumUserAccountItem);
		
//		// 处理当日和昨日不入库的uv和转化数据
//		this.reportFacade.postHandleTransAndUvData(userId, from, to, userViewItemList, sumUserAccountItem);
		
		return SUCCESS;
	}
	
	/**
	 * 下载账户层级分日报告，复用网盟推广首页分日报告逻辑，增加根据统计字段筛选的逻辑
	 * @return
	 * @throws IOException
	 */
	public String downloadAccountByDay() throws IOException {
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}
		// 数据准备
		List<UserAccountViewItem> userViewItemList = reportCproPlanMgr.findUserAccountStatData(qp,
				from, to);
		if(userViewItemList == null){
			throw new BusinessException(ReportWebConstants.ERR_DORIS);
		}
		// 汇总
		UserAccountViewItem sumUserAccountItem = reportCproPlanMgr
				.sumUserAccountStatData(userViewItemList);
		// 报表数据
		UserAccountReportVo reportVo = reportCproPlanMgr.getDownloadAccountReportVo(user,
				userViewItemList, sumUserAccountItem, this, sd1.format(from), sd1.format(to));
		
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(reportVo, output);
		// 下载所需额外属性
		byte[] bytes = output.toByteArray();
		inputStream = new ByteArrayInputStream(bytes);
		fileSize = bytes.length;
		try {
			// 文件名：账户名-分日-当前层级-日期范围.csv
			fileName = user.getUsername() + "-" + this.getText("download.reportbyday.filename.prefix");
			fileName += "-" + this.getText("download.account.level.allplan");
			fileName = StringUtils.subGBKString(fileName, 0,
					ReportWebConstants.FILE_MAIN_NAME_MAXLENGTH);
			fileName += "-" + sd.format(from) + "-" + sd.format(to) + ".csv";
			fileName = new String(fileName.getBytes(fileEncoding), "ISO8859-1");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage());
		}
		return SUCCESS;
	}
	
	// ****************************推广计划层级*****************************
	
	/**
	 * 获取推广计划层级分日报告
	 * @return
	 */
	public String planReportByDay() {
		// 数据准备
		List<ReportDayViewItem> itemList = reportCproPlanMgr.getPlanReportDayViewItems(userId, qp, from, to);
		// 汇总
		ReportSumData sumReportDayItem = reportCproPlanMgr.sumReportDayStatData(itemList);
		
		int totalPage = getTotalPage(itemList.size());
		itemList = pagerList(itemList);
		
		/**
		 * 如果统计时间是今天，将统计数据列中点击、消费外的其他列置为-1
		 */
//		if(super.isOnlyToday(from, to)){
//			super.clearNotRealtimeStat(itemList, sumReportDayItem);
//		}
		
		jsonObject.addData("list", itemList);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("cache", 1);
		jsonObject.addData("sum", sumReportDayItem);
		
//		// 处理当日和昨日不入库的uv和转化数据
//		this.reportFacade.postHandleTransAndUvData(userId, from, to, itemList, sumReportDayItem);
		
		return SUCCESS;
	}
	
	public String downloadPlanByDay() throws IOException {
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}
		
		//1、获取分日计划列表
		List<ReportDayViewItem> itemList = reportCproPlanMgr.getPlanReportDayViewItems(userId, qp, from, to);
		ReportSumData sumReportDayItem  = reportCproPlanMgr.sumReportDayStatData(itemList);
		
		//2、构造分日下载VO，下载的CSV共有四部分：1、账户基本信息，2、列头，3、列表，4、汇总信息	
		ReportDayVo vo = new ReportDayVo();

		//3、判断是否需要转化数据
		boolean showTransData = this.transReportFacade.isTransToolSigned(userId, false);
		boolean transDataValid = this.transReportFacade.needToFetchTransData(userId, from, to, false);
		
		//4、填充数据
		vo.setShowTransData(showTransData);
		vo.setTransDataValid(transDataValid);
		vo.setAccountInfo(generateAccountInfo(user.getUsername()));
		vo.setHeaders(generateReportDayHeader(showTransData));
		vo.setDetails(itemList);
		vo.setSummary(sumReportDayItem);
		vo.setSummaryText(this.getText("download.summary"));
		
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(vo, output);
		
		//5、设置下载需要使用到的一些属性
		byte[] bytes = output.toByteArray();
		inputStream = new ByteArrayInputStream(bytes);
		fileSize = bytes.length;
		Set<Integer> planIds = new HashSet<Integer>();
		planIds.add(qp.getPlanId());
		Map<Integer, String> planName = cproPlanMgr.findPlanNameByPlanIds(planIds);
		
		try {
			//6、文件名：账户名-分日-当前层级-日期范围.csv
			fileName = user.getUsername() + "-" + this.getText("download.reportbyday.filename.prefix");
			fileName += "-" + planName.get(qp.getPlanId());
			fileName = StringUtils.subGBKString(fileName, 0,
					ReportWebConstants.FILE_MAIN_NAME_MAXLENGTH);
			fileName += "-" + sd.format(from) + "-" + sd.format(to) + ".csv";
			fileName = new String(fileName.getBytes(fileEncoding), "ISO8859-1");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage());
		}
		return SUCCESS;
	}
	
	// ****************************推广组层级*****************************
	
	/**
	 * 获取推广组层级分日报告
	 * @return
	 */
	public String groupReportByDay() {
		// 数据准备
		List<ReportDayViewItem> itemList = reportCproGroupMgr.getGroupReportDayViewItems(userId, qp, from, to);
		// 汇总
		ReportSumData sumReportDayItem = reportCproPlanMgr.sumReportDayStatData(itemList);
		
		int totalPage = getTotalPage(itemList.size());
		itemList = pagerList(itemList);
		
		/**
		 * 如果统计时间是今天，将统计数据列中点击、消费外的其他列置为-1
		 */
//		if(super.isOnlyToday(from, to)){
//			super.clearNotRealtimeStat(itemList, sumReportDayItem);
//		}
		
		jsonObject.addData("list", itemList);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("cache", 1);
		jsonObject.addData("sum", sumReportDayItem);
		
//		// 处理当日和昨日不入库的uv和转化数据
//		this.reportFacade.postHandleTransAndUvData(userId, from, to, itemList, sumReportDayItem);
		
		return SUCCESS;
	}
	
	public String downloadGroupByDay() throws IOException {
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}
		
		// 数据准备
		List<ReportDayViewItem> itemList = reportCproGroupMgr.getGroupReportDayViewItems(userId, qp, from, to);
		// 汇总
		ReportSumData sumReportDayItem = reportCproPlanMgr.sumReportDayStatData(itemList);
		
		ReportDayVo vo = new ReportDayVo();//分日报表下载使用的VO
		
		//构造分日下载VO，下载的CSV共有四部分：1、账户基本信息，2、列头，3、列表，4、汇总信息	
		boolean showTransData = this.transReportFacade.isTransToolSigned(userId, false);
		boolean transDataValid = this.transReportFacade.needToFetchTransData(userId, from, to, false);
		vo.setShowTransData(showTransData);
		vo.setTransDataValid(transDataValid);
		vo.setAccountInfo(generateAccountInfo(user.getUsername()));
		vo.setHeaders(generateReportDayHeader(showTransData));
		vo.setDetails(itemList);
		vo.setSummary(sumReportDayItem);
		vo.setSummaryText(this.getText("download.summary"));

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(vo, output);

		// 设置下载需要使用到的一些属性
		byte[] bytes = output.toByteArray();
		inputStream = new ByteArrayInputStream(bytes);
		fileSize = bytes.length;
		Set<Integer> groupIds = new HashSet<Integer>();
		groupIds.add(qp.getGroupId());
		Map<Integer, String> groupName = cproGroupMgr.findGroupNameByGroupIds(groupIds);

		try {
			//6、文件名：账户名-分日-当前层级-日期范围.csv
			fileName = user.getUsername() + "-" + this.getText("download.reportbyday.filename.prefix");
			fileName += "-" + groupName.get(qp.getGroupId());
			fileName = StringUtils.subGBKString(fileName, 0,
					ReportWebConstants.FILE_MAIN_NAME_MAXLENGTH);
			fileName += "-" + sd.format(from) + "-" + sd.format(to) + ".csv";
			fileName = new String(fileName.getBytes(fileEncoding), "ISO8859-1");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage());
		}
		
		return SUCCESS;
	}
	
	// ****************************内部函数*****************************
	
	protected String[] generateReportDayHeader(boolean showTransData) {
		String[] headers = null;
		if (QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(qp.getLevel())) {
			if (showTransData) {
				headers = this.getText("download.plan.day.head.col.has.trans").split(",");
			} else {
				headers = this.getText("download.plan.day.head.col").split(",");
			}
		}
		else if (QueryParameterConstant.LEVEL.LEVEL_GROUP.equals(qp.getLevel())) {
			if (showTransData) {
				headers = this.getText("download.group.day.head.col.has.trans").split(",");
			} else {
				headers = this.getText("download.group.day.head.col").split(",");
			}
		}
		
		return headers;
	}
	
	/**
	 * 生成账户信息
	 * @return      
	*/
	protected ReportAccountInfo generateAccountInfo(String userName) {
		ReportAccountInfo accountInfo = new ReportAccountInfo();
		accountInfo.setReportText(this.getText("download.account.report"));
		
		accountInfo.setAccount(userName);
		accountInfo.setAccountText(this.getText("download.account.account"));
		
		accountInfo.setDateRange(sd1.format(from) + " - " + sd1.format(to));
		accountInfo.setDateRangeText(this.getText("download.account.daterange"));	
		
		accountInfo.setLevelText(this.getText("download.account.level"));
		
		if (QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(qp.getLevel())) {
			accountInfo.setReport(this.getText("download.account.report.plan"));
			accountInfo.setLevel(this.getText("download.reportbyday.level.plan"));	
		}
		else if (QueryParameterConstant.LEVEL.LEVEL_GROUP.equals(qp.getLevel())) {
			accountInfo.setReport(this.getText("download.account.report.group"));
			accountInfo.setLevel(this.getText("download.reportbyday.level.group"));
		}
		
		return accountInfo;
	}

	public CproPlanMgr getCproPlanMgr() {
		return cproPlanMgr;
	}

	public void setCproPlanMgr(CproPlanMgr cproPlanMgr) {
		this.cproPlanMgr = cproPlanMgr;
	}

	public CproGroupMgr getCproGroupMgr() {
		return cproGroupMgr;
	}

	public void setCproGroupMgr(CproGroupMgr cproGroupMgr) {
		this.cproGroupMgr = cproGroupMgr;
	}

	public ReportCproPlanMgr getReportCproPlanMgr() {
		return reportCproPlanMgr;
	}

	public void setReportCproPlanMgr(ReportCproPlanMgr reportCproPlanMgr) {
		this.reportCproPlanMgr = reportCproPlanMgr;
	}

	public ReportCproGroupMgr getReportCproGroupMgr() {
		return reportCproGroupMgr;
	}

	public void setReportCproGroupMgr(ReportCproGroupMgr reportCproGroupMgr) {
		this.reportCproGroupMgr = reportCproGroupMgr;
	}

}
