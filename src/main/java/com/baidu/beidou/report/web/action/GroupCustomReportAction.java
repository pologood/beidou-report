package com.baidu.beidou.report.web.action;

import java.io.ByteArrayInputStream;
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
import com.baidu.beidou.period.code.ReportTypeCode;
import com.baidu.beidou.period.report.ReportResponse;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.StringUtils;
import com.baidu.unbiz.biz.result.Result;
import com.baidu.unbiz.common.CollectionUtil;
import com.baidu.unbiz.common.io.ByteArray;
import com.baidu.unbiz.olap.obj.BaseItem;

public class GroupCustomReportAction extends BeidouReportActionSupport {
	private static final long serialVersionUID = 1116584041553970033L;
	private static Log log = LogFactory.getLog(GroupCustomReportAction.class);
	
	private static final String downloadTitle = "推广组效果报告";

	// @Resource(name = "groupStatServiceImpl")
	// private GroupStatService groupStatService;
	//
	// @Resource(name = "siteStatServiceImpl")
	// private SiteStatService siteStatService;

	@Resource
	private PeriodReportAPI pdApi;

	// private GroupCustomReportFacade groupCustomReportFacade = null;
	//
	// public GroupCustomReportFacade getGroupCustomReportFacade() {
	// return groupCustomReportFacade;
	// }
	//
	// public void setGroupCustomReportFacade(
	// GroupCustomReportFacade groupCustomReportFacade) {
	// this.groupCustomReportFacade = groupCustomReportFacade;
	// }

	@Override
	protected void initStateMapping() {
		if (qp != null) {
			qp.setStateMapping(ReportWebConstants.PLAN_FRONT_BACKEND_STATE_MAPPING
					.get(qp.getViewState()));
		}
	}

	/**
	 * 获取推广计划效果列表 当前在出异常时直接返回status=1，系统提示“系统内部异常”，后续需要给出具体提示
	 **/
	public String ajaxList() {
		try {
			// 初始化一下参数
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED_SERVER);
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

		Report report = toReport(qp, from, to);
		setReportType(report);

		ReportResponse<?> response = pdApi.getResponse(report).getResult();
		// 是否按统计字段排序 order
		List<? extends BaseItem> list = response.getDataList();

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

	// private ReportSumData sumSiteStatData(
	// List<? extends SiteViewItem> viewItemList) {
	// ReportSumData sumItem = new ReportSumData();
	// if (CollectionUtils.isNotEmpty(viewItemList)) {
	// for (SiteViewItem item : viewItemList) {
	// sumItem.setSrchs(sumItem.getSrchs() + item.getSrchs());
	// sumItem.setClks(sumItem.getClks() + item.getClks());
	// sumItem.setCost(sumItem.getCost() + item.getCost());
	// sumItem.setSrchuv(sumItem.getSrchuv() + item.getSrchuv());
	// sumItem.setClkuv(sumItem.getClkuv() + item.getClkuv());
	// }
	// }
	// // 汇总“ctr,acp,cpm”
	// sumItem.generateExtentionFields();
	// sumItem.setSummaryText("总计");
	// return sumItem;
	// }

	/**
	 * 定制报告--下载推广组效果报告
	 */
	public String downloadReport() throws IOException {
		try {
			// 初始化一下参数
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED_SERVER);
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
		try {
			fileName = user.getUsername() + "-"
					+ this.getText("customreport.name.group");
			fileName = getReportTitle(fileName, report.getTypeCode());
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

	// FIXME
	private void setReportType(Report report) {
		if (report.getBySite() == 0) {
			report.setTypeCode(ReportTypeCode.GROUP_REPORT);
		} else if (report.getBySite() == 1) {
			report.setTypeCode(ReportTypeCode.GROUP_SITE_REPORT);
		} else if (report.getBySite() == 2) {
			report.setTypeCode(ReportTypeCode.GROUP_SUBSITE_REPORT);
		}
	}

	// FIXME
	private String getReportTitle(String name, ReportTypeCode type) {
		switch (type) {
		case GROUP_REPORT:
			return name;
		case GROUP_SITE_REPORT:
			return name + "-主域-";
		case GROUP_SUBSITE_REPORT:
			return name + "-二级-";
		default:
			return name;
		}
	}
}
