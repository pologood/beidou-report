package com.baidu.beidou.report.web.action;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.olap.vo.UnitViewItem;
import com.baidu.beidou.period.api.PeriodReportAPI;
import com.baidu.beidou.period.bo.Report;
import com.baidu.beidou.period.code.ReportTypeCode;
import com.baidu.beidou.period.report.ReportResponse;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.unit.UnitReportSumData;
import com.baidu.beidou.report.vo.unit.UnitSumData;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouConstant;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.LogUtils;
import com.baidu.beidou.util.StringUtils;
import com.baidu.unbiz.biz.result.Result;
import com.baidu.unbiz.common.CollectionUtil;
import com.baidu.unbiz.common.io.ByteArray;
import com.baidu.unbiz.olap.obj.BaseItem;

public class UnitCustomReportAction extends BeidouReportActionSupport {

	// private long start = 0; // 用于记录action的开始时间
	// private long last = 0; // 用于记录action的处理的上次记录时间
	// private long now = 0; // 用于记录action的处理的当前时间
	// private static final String LOG_PREFIX_CUSTOM_UNIT = "处理定制报告创意列表-";
	// private static final String LOG_PREFIX_DOWNLOAD_CUSTOM_UNIT =
	// "下载定制报告创意列表-";

	private static final long serialVersionUID = 4775242764102742678L;
	
	private static final String downloadTitle= "创意效果报告";

	// private ReportCproUnitMgr reportCproUnitMgr;
	//
	// private UnitCustomReportFacade unitCustomFacade;
	//
	@Resource
	private PeriodReportAPI pdApi;

	public String execute() {

		return SUCCESS;
	}

	@Override
	protected void initStateMapping() {
		if (qp != null) {
			qp.setStateMapping(ReportWebConstants.UNIT_FRONT_BACKEND_STATE_MAPPING
					.get(qp.getViewState()));
		}
	}

	/** 定制报告-创意效果-列表使用 */
	public String ajaxList() {
		try {
			// 初始化一下参数
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return SUCCESS;
		}

//		if (super.isMixTodayAndBefore(this.getFrom(), this.getTo())) {
//			jsonObject.setStatus(BeidouConstant.JSON_OPERATE_FAILED);
//			jsonObject.setGlobalMsg(getText("mixed.report.not.allowed"));
//			return SUCCESS;
//		}

		if (null == qp || null == qp.getIsBySite() || null == qp.getTimeUnit()) {
			jsonObject.setStatus(BeidouConstant.JSON_OPERATE_FAILED);
			jsonObject.setGlobalMsg(getText("custom.report.parameter.missing"));
			return SUCCESS;
		}

		Report report = toReport(qp, from, to);
		setReportType(report);

		ReportResponse<?> response = pdApi.getResponse(report).getResult();
		// 是否按统计字段排序 order
		List<? extends BaseItem> list = response.getDataList();

		if (CollectionUtil.isEmpty(list)) {
			buildEmptyResult();
			return SUCCESS;
		}

		// step6:生成缓存条件：总创意数小于1W。
		list.remove(list.size() - 1);
		int count = list.size();
		// boolean cache = shouldCache(count);

		// step7:计算总页码
		int totalPage = super.getTotalPage(count);
		// 是否需要前端排序
		int isNeedFrontSort = ReportConstants.Boolean.TRUE;
		if (list.size() > ReportWebConstants.FRONT_SORT_THRESHOLD) { // 总数>1W
			isNeedFrontSort = ReportConstants.Boolean.FALSE;
			// 分页
			list = ReportWebConstants.subListinPage(list, getQpPage(),
					getQpPageSize());
		}
		jsonObject.addData("list", list);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("sum", response.getSumData());
		jsonObject.addData("cache", isNeedFrontSort);

		return SUCCESS;
	}

	private void buildEmptyResult() {
		jsonObject.addData("list", Collections.emptyList());
		jsonObject.addData("totalPage", 0);
		jsonObject.addData("cache", ReportConstants.Boolean.TRUE);
	}

	/** 定制报告-创意效果-列表下载使用 */
	public String download() throws IOException {

		// 初始化一下参数
		super.initParameter();
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}

//		if (super.isMixTodayAndBefore(this.getFrom(), this.getTo())) {
//			jsonObject.setStatus(BeidouConstant.JSON_OPERATE_FAILED);
//			jsonObject.setGlobalMsg(getText("mixed.report.not.allowed"));
//			return SUCCESS;
//		}

		// 数据准备
		Report report = toReport(qp, from, to);
		setReportType(report);
		report.setReportName(downloadTitle);

		// 报表数据
		Result<ByteArray> result = pdApi.createAndDownload(report);
		if (!result.isSuccess()) {
			return ERROR;
		}
		// 下载所需额外属性
		ByteArray byteArray = result.getResult();
		byte[] bytes = byteArray.toByteArray();
		inputStream = new ByteArrayInputStream(bytes);
		fileSize = bytes.length;

		fileName = this.generateDownloadFileName(report);
		// logInfo = LOG_PREFIX_DOWNLOAD_CUSTOM_UNIT + "输出结果文件:";
		// recordExeTime(logInfo);

		return SUCCESS;
	}

	// private void recordExeTime(String logInfo) {
	// logInfo += (now - last);
	// now = System.currentTimeMillis();
	// log.debug(logInfo);
	// last = now;
	// }

	/**
	 * calculateSumData:根据返回结果休计算汇总信息
	 *
	 * @param infoData
	 * @return 返回结果集的汇总信息
	 */
	public UnitSumData calculateSumData(List<UnitViewItem> infoData) {
		UnitSumData sumData = new UnitSumData();

		// 1、累加汇总值
		for (UnitViewItem item : infoData) {
			sumData.setClks(sumData.getClks() + item.getClks());
			sumData.setCost(sumData.getCost() + item.getCost());
			sumData.setSrchs(sumData.getSrchs() + item.getSrchs());
		}

		// 2、生成扩展数据
		sumData.generateExtentionFields();

		// 3、设置总数
		sumData.setUnitCount(infoData.size());

		return sumData;
	}

	/**
	 * generateAccountInfo: 生成报表用的账户信息
	 *
	 * @return
	 */
	private String generateDownloadFileName(Report report) {

		User user = userMgr.findUserBySFid(userId);

		// 生成文件名：[创意效果报告]-[下载时间].csv
		String fileName = user.getUsername() + "-"
				+ this.getText("customreport.download.unit.filename.prefix");

		fileName = getReportTitle(fileName, report.getTypeCode());

		try {
			// 主文件名（时间之前部分）不能超过80个字符，中文算两个字符
			fileName = StringUtils.subGBKString(fileName, 0,
					ReportWebConstants.FILE_MAIN_NAME_MAXLENGTH);
		} catch (UnsupportedEncodingException e1) {
			LogUtils.error(log, e1);
		}
		fileName += "_" + sd.format(from) + "-" + sd.format(to) + ".csv";

		try {
			// 中文文件名需要用ISO8859-1编码
			fileName = new String(fileName.getBytes(fileEncoding), "ISO8859-1");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage());
		}
		return fileName;
	}

	/**
	 * generateAccountInfo: 生成报表用的账户信息
	 *
	 * @return
	 */
	protected ReportAccountInfo generateAccountInfo(String userName) {
		ReportAccountInfo accountInfo = new ReportAccountInfo();
		accountInfo.setReport(getText("customreport.name.unit"));
		accountInfo.setReportText(this
				.getText("customreport.accountinfo.col.report"));

		accountInfo.setAccount(userName);
		accountInfo.setAccountText(this
				.getText("customreport.accountinfo.col.account"));

		accountInfo.setDateRange(sd1.format(from) + " - " + sd1.format(to));
		accountInfo.setDateRangeText(this
				.getText("customreport.accountinfo.col.daterange"));

		List<String[]> accountInfoList = new ArrayList<String[]>();

		// 填充抬头：查询条件
		// unitCustomFacade.fillFilterContent(qp, accountInfoList);

		// accountInfoList.add(new
		// String[]{this.getText("customreport.accountinfo.col.plan"),"11111"});
		// accountInfoList.add(new
		// String[]{this.getText("customreport.accountinfo.col.group"),"22222"});
		accountInfo.setAccountInfoList(accountInfoList);

		return accountInfo;
	}

	/**
	 * generateReportHeader:生成报表用的列表头
	 * 
	 * @param showTransData
	 *
	 * @return
	 * @since
	 */
	protected String[] generateReportHeader() {

		String[] headers = null;
		if (qp.getIsBySite().equals(ReportConstants.SiteLevel.NONE)) {
			headers = this.getText("customreport.download.unit.head.col")
					.split(",");
		} else {
			headers = this
					.getText("customreport.download.unit.bysite.head.col")
					.split(",");
		}

		return headers;
	}

	/**
	 * generateReportSummary: 生成汇总信息
	 *
	 * @param sumData
	 *            汇总数据
	 * @return 用于表示报表的汇总信息
	 */
	protected UnitReportSumData generateReportSummary(UnitSumData sumData) {

		UnitReportSumData sum = new UnitReportSumData();
		sum.setClks(sumData.getClks());
		sum.setSrchs(sumData.getSrchs());
		sum.setCost(sumData.getCost());
		sum.setAcp(sumData.getAcp().doubleValue());
		sum.setCtr(sumData.getCtr().doubleValue());
		sum.setCpm(sumData.getCpm().doubleValue());
		sum.setSummaryText(this.getText("download.summary.unit",
				new String[] { String.valueOf(sumData.getUnitCount()) }));// 添加“合计”

		return sum;
	}

	/********************* [g/s]etters ******************************/

	// public ReportCproUnitMgr getReportCproUnitMgr() {
	// return reportCproUnitMgr;
	// }
	//
	// public void setReportCproUnitMgr(ReportCproUnitMgr reportCproUnitMgr) {
	// this.reportCproUnitMgr = reportCproUnitMgr;
	// }
	//
	// public UnitCustomReportFacade getUnitCustomFacade() {
	// return unitCustomFacade;
	// }
	//
	// public void setUnitCustomFacade(UnitCustomReportFacade unitCustomFacade)
	// {
	// this.unitCustomFacade = unitCustomFacade;
	// }

	// FIXME
	private void setReportType(Report report) {
		if (report.getBySite() == 0) {
			report.setTypeCode(ReportTypeCode.UNIT_REPORT);
		} else if (report.getBySite() == 1) {
			report.setTypeCode(ReportTypeCode.UNIT_SITE_REPORT);
		} else if (report.getBySite() == 2) {
			report.setTypeCode(ReportTypeCode.UNIT_SUBSITE_REPORT);
		}
	}

	// FIXME
	private String getReportTitle(String name, ReportTypeCode type) {
		switch (type) {
		case UNIT_REPORT:
			return name;
		case UNIT_SITE_REPORT:
			return name + "-主域-";
		case UNIT_SUBSITE_REPORT:
			return name + "-二级-";
		default:
			return name;
		}
	}

}
