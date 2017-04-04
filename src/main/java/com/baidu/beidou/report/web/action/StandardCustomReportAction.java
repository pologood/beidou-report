package com.baidu.beidou.report.web.action;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.annotation.Resource;

import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.period.api.PeriodReportAPI;
import com.baidu.beidou.period.bo.Report;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.StandardCustomReportVo;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouConstant;
import com.baidu.beidou.util.LogUtils;
import com.baidu.beidou.util.StringUtils;
import com.baidu.unbiz.biz.result.Result;
import com.baidu.unbiz.common.io.ByteArray;

public class StandardCustomReportAction extends BeidouReportActionSupport {

	private static final long serialVersionUID = 4775242764102742876L;

	// private StandardCustomReportFacade standardCustomFacade;

	private long start = 0; // 用于记录action的开始时间
	private long last = 0; // 用于记录action的处理的上次记录时间
	private long now = 0; // 用于记录action的处理的当前时间
	private static final String LOG_PREFIX_DOWNLOAD_CUSTOM_STANDARD = "下载定制报告-标准化每日报告-";

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

	/** 定制报告-标准化每日报告-列表下载使用 */
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

		start = System.currentTimeMillis();
		last = start;
		log.debug(LOG_PREFIX_DOWNLOAD_CUSTOM_STANDARD + "开始于:" + start);
		String logInfo;

		// 数据准备
		Report report = toReport(qp, from, to);
		report.setReportType(ReportConstants.ReportType.STANDARD_REPORT);
		// 报表数据
		Result<ByteArray> result = pdApi.createAndDownload(report);
		if (!result.isSuccess()) {
			return ERROR;
		}
		// 下载所需额外属性
		ByteArray byteArray = result.getResult();
		byte[] bytes = byteArray.toByteArray();
		inputStream = new ByteArrayInputStream(bytes);
		;
		fileSize = bytes.length;

		fileName = this.generateDownloadFileName(user.getUsername());

		logInfo = LOG_PREFIX_DOWNLOAD_CUSTOM_STANDARD + "输出标准化每日报告:";
		recordExeTime(logInfo);

		return SUCCESS;
	}

	void fillStandardReportHeader(StandardCustomReportVo vo) {

		vo.setUserLevelHeaders(this.getText(
				"customreport.download.standard.account.head").split(","));
		vo.setPlanLevelHeaders(this.getText(
				"customreport.download.standard.plan.head").split(","));
		vo.setGroupLevelHeaders(this.getText(
				"customreport.download.standard.unit.head").split(","));

	}

	void fillStandardReportName(StandardCustomReportVo vo) {

		vo.setUserLevelReportName(this
				.getText("customreport.download.standard.account.name"));
		vo.setPlanLevelReportName(this
				.getText("customreport.download.standard.plan.name"));
		vo.setGroupLevelReportName(this
				.getText("customreport.download.standard.group.name"));

	}

	/**
	 * generateAccountInfo: 生成报表用的账户信息
	 *
	 * @return
	 */
	private String generateDownloadFileName(String userName) {

		// 生成文件名：[网盟推广每日报告]-[username]_[下载时间].csv
		String fileName = this
				.getText("customreport.download.standard.filename.prefix");
		fileName += "-" + userName;
		try {
			// 主文件名（时间之前部分）不能超过80个字符，中文算两个字符
			fileName = StringUtils.subGBKString(fileName, 0,
					ReportWebConstants.FILE_MAIN_NAME_MAXLENGTH);
		} catch (UnsupportedEncodingException e1) {
			LogUtils.error(log, e1);
		}
		fileName += "_" + sd.format(to) + ".csv";

		try {
			// 中文文件名需要用ISO8859-1编码
			fileName = new String(fileName.getBytes(fileEncoding), "ISO8859-1");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage());
		}
		return fileName;
	}

	private void recordExeTime(String logInfo) {
		logInfo += (now - last);
		now = System.currentTimeMillis();
		log.debug(logInfo);
		last = now;
	}

	/**
	 * generateAccountInfo: 生成报表用的账户信息
	 *
	 * @return
	 */
	protected ReportAccountInfo generateAccountInfo(String userName) {

		ReportAccountInfo accountInfo = new ReportAccountInfo();
		accountInfo.setDateRange(sd2.format(to));

		return accountInfo;
	}

	// public StandardCustomReportFacade getStandardCustomFacade() {
	// return standardCustomFacade;
	// }
	//
	// public void setStandardCustomFacade(
	// StandardCustomReportFacade standardCustomFacade) {
	// this.standardCustomFacade = standardCustomFacade;
	// }

	/********************* [g/s]etters ******************************/

}
