package com.baidu.beidou.report.web.action;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.period.api.PeriodReportAPI;
import com.baidu.beidou.period.bo.Report;
import com.baidu.beidou.period.report.ReportResponse;
import com.baidu.beidou.period.stat.AntiBean;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.StringUtils;
import com.baidu.unbiz.biz.result.Result;
import com.baidu.unbiz.common.CollectionUtil;
import com.baidu.unbiz.common.io.ByteArray;

public class InvalidCostReportAction extends BeidouReportActionSupport {
	private static final long serialVersionUID = 1116584041553970033L;
	private static Log log = LogFactory.getLog(InvalidCostReportAction.class);

	private static final String downloadTitle = "无效点击报告";

	@Resource
	private PeriodReportAPI pdApi;

	@Override
	protected void initStateMapping() {
		if (qp != null) {
			qp.setStateMapping(ReportWebConstants.PLAN_FRONT_BACKEND_STATE_MAPPING
					.get(qp.getViewState()));
		}
	}

	/**
	 * 定制报告--获取账户效果列表
	 */
	public String ajaxList() {
		try {
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
//		if (super.isMixTodayAndBefore(from, to)) {
//			log.warn("系统无法提供今日和历史时间混合的报告数据");
//			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED_SERVER);
//			jsonObject.addMsg(ReportWebConstants.ERR_DATE);
//
//			return SUCCESS;
//		}

		// 数据准备
		Report report = toReport(qp, from, to);
		report.setReportType(ReportConstants.ReportType.INVALID_COST_REPORT);
		Result<ReportResponse<AntiBean>> result = pdApi.getResponse(report);
		if (!result.isSuccess()) {
			return ERROR;
		}

		ReportResponse<AntiBean> response = result.getResult();
		List<AntiBean> list = response.getDataList();
		if (CollectionUtil.isEmpty(list)) {
			buildEmptyResult();
			return SUCCESS;
		}

		list.remove(list.size() - 1);
		// 总页数
		int totalPage = super.getTotalPage(list.size());
		// 是否需要前端排序
		int isNeedFrontSort = ReportConstants.Boolean.TRUE;
		if (list.size() > ReportWebConstants.FRONT_SORT_THRESHOLD) { // 总数>1W
			isNeedFrontSort = ReportConstants.Boolean.FALSE;
			// 分页
			list = ReportWebConstants.subListinPage(list, getQpPage(),
					getQpPageSize());
		}
		jsonObject.addData("list", list);
		jsonObject.addData("sum", response.getSumData());
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("cache", isNeedFrontSort);
		return SUCCESS;
	}

	/**
	 * 定制报告--下载账户效果报告
	 */
	public String downloadReport() throws IOException {
		try {
			initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);
			return SUCCESS;
		}

		/**
		 * 如果统计时间混合今天和历史时间，则返回空
		 */
//		if (super.isMixTodayAndBefore(from, to)) {
//			throw new BusinessException("系统无法提供今日和历史时间混合的报告数据");
//		}

		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}
		Report report = toReport(qp, from, to);
		report.setReportType(ReportConstants.ReportType.INVALID_COST_REPORT);
		report.setReportName(downloadTitle);

		// 报表数据
		Result<ByteArray> result = pdApi.createAndDownload(report);
		if (!result.isSuccess()) {
			return ERROR;
		}
		// 下载所需额外属性
		ByteArray byteArray = result.getResult();
		inputStream = byteArray.toInputStream();
		fileSize = byteArray.getLength();
		try {
			// 文件名：[账户名]-账户效果报告-[日期分部].csv
			fileName = user.getUsername() + "-"
					+ this.getText("customreport.name.invalidcost");
			fileName = StringUtils.subGBKString(fileName, 0,
					ReportWebConstants.FILE_MAIN_NAME_MAXLENGTH);
			fileName += "-" + sd.format(from) + "-" + sd.format(to) + ".csv";
			fileName = new String(fileName.getBytes(fileEncoding), "ISO8859-1");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage());
		}
		return SUCCESS;
	}

	private void buildEmptyResult() {
		jsonObject.addData("list", Collections.emptyList());
		jsonObject.addData("totalPage", 0);
		jsonObject.addData("cache", ReportConstants.Boolean.TRUE);
	}

}
