package com.baidu.beidou.report.web.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.util.CollectionUtils;

import com.baidu.beidou.aot.control.AotReportItem;
import com.baidu.beidou.cprogroup.bo.CproGroup;
import com.baidu.beidou.cprogroup.service.CproGroupMgr;
import com.baidu.beidou.cprogroup.service.UnionSiteMgr;
import com.baidu.beidou.cprogroup.vo.SimplePopUpSiteInfo;
import com.baidu.beidou.cproplan.bo.CproPlan;
import com.baidu.beidou.cproplan.service.CproPlanMgr;
import com.baidu.beidou.cprounit.exception.RefuseReasonFormatException;
import com.baidu.beidou.cprounit.service.CproUnitMgr;
import com.baidu.beidou.cprounit.service.RefuseReasonUtils;
import com.baidu.beidou.cprounit.service.SeniorUnitMgrFilter;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.olap.constant.Constants;
import com.baidu.beidou.olap.service.ReportByDayStatService;
import com.baidu.beidou.olap.service.SiteStatService;
import com.baidu.beidou.olap.service.UnitStatService;
import com.baidu.beidou.olap.vo.ReportDayViewItem;
import com.baidu.beidou.olap.vo.ReportMainSiteViewItem;
import com.baidu.beidou.olap.vo.ReportSiteViewItem;
import com.baidu.beidou.olap.vo.ReportSubSiteViewItem;
import com.baidu.beidou.olap.vo.UnitMainSiteItem;
import com.baidu.beidou.olap.vo.UnitViewItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.report.service.ReportCacheService;
import com.baidu.beidou.report.service.ReportCproUnitMgr;
import com.baidu.beidou.report.util.ReportUtil;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.ReportDayVo;
import com.baidu.beidou.report.vo.ReportSiteVo;
import com.baidu.beidou.report.vo.ReportSumData;
import com.baidu.beidou.report.vo.StatInfo;
import com.baidu.beidou.report.vo.unit.UnitReportSumData;
import com.baidu.beidou.report.vo.unit.UnitReportVo;
import com.baidu.beidou.report.vo.unit.UnitSumData;
import com.baidu.beidou.report.vo.unit.UnitUrlSumData;
import com.baidu.beidou.stat.driver.bo.Constant;
import com.baidu.beidou.stat.service.ClickUrlService;
import com.baidu.beidou.stat.vo.ClickStatVo;
import com.baidu.beidou.tool.util.TransReportHelper;
import com.baidu.beidou.tool.vo.TempSitesAndTrans;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouConstant;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.LogUtils;
import com.baidu.beidou.util.StringUtils;
import com.baidu.beidou.util.vo.AbsJsonObject;
import com.baidu.unbiz.olap.constant.OlapConstants;
import com.baidu.unbiz.olap.constant.SortOrder;
import com.baidu.unbiz.olap.util.ReportUtils;

public class ListCproUnitAction extends BeidouReportActionSupport {

	private static final long serialVersionUID = 4775242764102742893L;
	
	@Resource(name="unitStatServiceImpl")
	UnitStatService unitStatMgr;
	
	@Resource(name="reportByDayStatServiceImpl")
	private ReportByDayStatService byDayStatService;
	
	@Resource(name="siteStatServiceImpl")
	private SiteStatService siteStatMgr;

	private CproPlanMgr cproPlanMgr;

	private CproGroupMgr cproGroupMgr;

	private UnionSiteMgr unionSiteMgr;

	private ReportCproUnitMgr reportCproUnitMgr = null;

	private ClickUrlService clickUrlService;

	private CproUnitMgr cproUnitMgr;

	private ReportCacheService cacheService;

	private String wm123SiteDetailPrefix;

	private String wm123SiteDetailSuffix;

	
	/** 下载报表的列数 */
	public static final int DOWN_REPORT_UNIT_SITE_COL_NUM = 7;
	public static final int DOWN_REPORT_UNIT_BYSITE_COL_NUM = 7;
	/** 结果集（全集）总行数 */
	int count;

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

	/**
	 * ajaxFlashXml :生成XML文档，格式如下： <?xml version='1.0' encoding='utf-8'?> <data
	 * overview='最近七天' showIndex='0' tag1='/展现次数/' tag2='/点击次数/' tag3='/点击率/%'
	 * tag4='/总费用/' selected='1'> <record date='2010-06-25' data1='0.00'
	 * data2='0.00' data3='0.00%' data4='132323'/> <record date='2010-06-26'
	 * data1='0.00' data2='0.00' data3='0.03%' data4='132322'/> </data>
	 */
	public String ajaxFlashXml() {

		try {
			super.initParameterForAtLestServenDay();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_SUCCESS);
			jsonObject.addData("state",
					ReportWebConstants.FLASH_DATA_STATE.UNNORMAL);
			jsonObject.addData("xml", "");
			return SUCCESS;
		}
		
//        if (super.isMixTodayAndBefore(this.getFrom(), this.getTo())
//                        || super.isOnlyToday(this.getFrom(), this.getTo())) {
//            jsonObject.setStatus(BeidouConstant.JSON_OPERATE_FAILED);
//            jsonObject.setGlobalMsg(getText("mixed.report.not.allowed"));
//            return SUCCESS;
//        }
		
        List<ReportDayViewItem> statList; //天粒度的汇总数据,key为时间，value为统计值
		
		//首先看是否需要查询AOT获取IDS来按IDS来滤。
		AotReportItem aotItems = getAotItems();//null表示不按AOT，否则表示要按AOT
		
		if (qp.hasStatField4Filter() ) {//如果需要按统计字段过滤则要查询出[from,to]+groupId维度的数据
	
			statList = getStatItemsFilterByStatField(aotItems);//时间粒度：天
			
		} else {
			// 否则只用查出user维度的，但需要传入过滤后的ids
			statList = getStatItemsFilterByNonStatField(aotItems);
		}

		String xml = generateXml(statList);

		jsonObject.addData("state", ReportWebConstants.FLASH_DATA_STATE.NORMAL);
		jsonObject.addData("data", xml);

		return SUCCESS;
	}

	/** 创意列表使用 */
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

		List<UnitViewItem> infoData;// 目标结果集（可能是全集或者分页集）

		// 1、首先看是否需要查询AOT获取IDS来按IDS来滤。
		AotReportItem aotItems = getAotItems();// null表示不按AOT，否则表示要按AOT
		genQueryItemIds(aotItems);// 如果需要按ids查询的话则生成qp.ids

		// 2、查询总条数，该数字可能不是最终值，因为有可能需要按统计值过滤，这样的话该值会被再次修改
		count = reportCproUnitMgr.countCproUnitViewItem(qp);

		// 3、获取VO列表
		infoData = getViewItems(aotItems, false);

		// 4、合并其他信息：拒绝理由
		if (!org.apache.commons.collections.CollectionUtils.isEmpty(infoData)) {
			Map<Long, UnitViewItem> mapping = new HashMap<Long, UnitViewItem>(
					infoData.size());
			for (UnitViewItem item : infoData) {
				mapping.put(item.getUnitId(), item);
			}

			Map<Long, String> refuseReasonMap = reportCproUnitMgr
					.findCproUnitAuditInfo(mapping.keySet(), qp.getUserId());
			if (!org.apache.commons.collections.MapUtils
					.isEmpty(refuseReasonMap)) {
				for (Map.Entry<Long, String> entry : refuseReasonMap.entrySet()) {
					try {
						mapping.get(entry.getKey()).setRefuseReason(
								RefuseReasonUtils.generateRefuseReason(
										entry.getValue(), true));
					} catch (RefuseReasonFormatException e) {
						log.error(e);
					}
				}
			}
		}

		// 5、生成汇总的统计数据
		UnitSumData sumData = calculateSumData(infoData);

//		if (super.isOnlyToday(getFrom(), getTo())) {
//			super.clearNotRealtimeStat(infoData, sumData);
//		}

		// 6、生成缓存条件：总条件小于1W且当前取的是第一页数据。
		boolean cache = shouldCache(count);

		// 7、计算总页码
		int totalPage = super.getTotalPage(count);

		// 8、计算总的数据
		boolean hasElements = hasGroupcountWithoutConditions();

		jsonObject.addData("list", infoData);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("cache", cache ? ReportConstants.Boolean.TRUE
				: ReportConstants.Boolean.FALSE);
		jsonObject.addData("sum", sumData);
		jsonObject.addData("hasElements", hasElements);

		// 9、处理当日和昨日不入库的uv和转化数据
		this.reportFacade.postHandleTransAndUvData(userId, from, to, infoData,
				sumData);

		return SUCCESS;
	}

	private boolean hasGroupcountWithoutConditions() {
		if (qp == null || qp.getGroupId() == null) {
			return true;
		}
		List<Integer> groupIdList = new ArrayList<Integer>();
		groupIdList.add(qp.getGroupId());
		SeniorUnitMgrFilter filter = new SeniorUnitMgrFilter();
		filter.setGroupId(groupIdList);
		filter.setUserid(qp.getUserId());
		long unitCountWithoutDeleted = cproUnitMgr.countUnitByFilter(
				qp.getUserId(), filter);
		if (unitCountWithoutDeleted > 0) {
			return true;
		}
		return false;
	}

	public String ajaxListByDay() {
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

		List<ReportDayViewItem> itemList = getReportDayViewItems();
		ReportSumData sum = getReportSumData(itemList.size());

//		if (super.isOnlyToday(getFrom(), getTo())) {
//			super.clearNotRealtimeStat(itemList, sum);
//		}

		int totalPage = getTotalPage(itemList.size());
		itemList = pagerList(itemList);

		jsonObject.addData("list", itemList);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("cache", 1);
		jsonObject.addData("sum", sum);

		// 处理当日和昨日不入库的uv和转化数据
		// this.reportFacade.postHandleTransAndUvData(userId, from, to,
		// itemList, sum);

		return SUCCESS;
	}

	public String ajaxListBySite() {
		jsonObject = new AbsJsonObject();
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

		// 网站基本信息尚未获取
		@SuppressWarnings("unchecked")
		List<ReportSiteViewItem> itemList = (List<ReportSiteViewItem>) getReportSiteViewItems();
		merge(itemList);

		ReportSumData sum = getReportSumData(itemList.size());

//		if (super.isOnlyToday(getFrom(), getTo())) {
//			super.clearNotRealtimeStat(itemList, sum);
//		}

		int totalPage = getTotalPage(itemList.size());
		itemList = pagerList(itemList);

		jsonObject.addData("list", itemList);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("cache", 1);
		jsonObject.addData("sum", sum);

		return SUCCESS;
	}

	
	public String ajaxListBySubSite() {
		jsonObject = new AbsJsonObject();

		try {
			// 初始化一下参数
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return SUCCESS;
		}

		@SuppressWarnings("unchecked")
		List<ReportSiteViewItem> itemList = (List<ReportSiteViewItem>) getReportSubSiteViewItems();

		ReportSumData sum = getReportSubSiteSumData(itemList);

		int totalPage = getTotalPage(itemList.size());
		itemList = pagerList(itemList);

		jsonObject.addData("list", itemList);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("cache", 1);
		jsonObject.addData("sum", sum);

		return SUCCESS;
	}

	// 该方法提供下载功能
	public String download() throws IOException {

		// 初始化一下参数
		super.initParameter();
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}

		// 下载的CSV共有四部分
		// 1、账户基本信息，2、列头，3、列表，4、汇总信息

		List<UnitViewItem> infoData = null;// 目标plan集
		AotReportItem aotItems = getAotItems();// null表示不按AOT，否则表示要按AOT

		genQueryItemIds(aotItems);// 如果需要按ids查询的话则生成qp.ids

		count = reportCproUnitMgr.countCproUnitViewItem(qp);
		// 1、获取统计数据(不分页)
		infoData = getViewItems(aotItems, true);// 无统计时间粒度

		// 2、生成汇总的统计数据
		UnitSumData sumData = calculateSumData(infoData);

//		if (super.isOnlyToday(getFrom(), getTo())) {
//			super.clearNotRealtimeStat(infoData, sumData);
//		}

		// 处理当日和昨日不入库的uv和转化数据 add by wangchongjie since 2012.11.03
		this.reportFacade.postHandleTransAndUvData(userId, from, to, infoData,
				sumData);

		UnitReportVo vo = new UnitReportVo(qp.getLevel());// 报表下载使用的VO
		boolean showTransData = this.transReportFacade.isTransToolSigned(
				userId, false);
		boolean transDataValid = this.transReportFacade.needToFetchTransData(
				userId, from, to, false);
		vo.setShowTransData(showTransData);
		vo.setTransDataValid(transDataValid);
		vo.setAccountInfo(generateAccountInfo(user.getUsername()));
		vo.setDetails(infoData);
		vo.setHeaders(generateReportHeader(showTransData));
		vo.setSummary(generateReportSummary(sumData));

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(vo, output);

		// 设置下载需要使用到的一些属性
		byte[] bytes = output.toByteArray();
		inputStream = new ByteArrayInputStream(bytes);
		;
		fileSize = bytes.length;

		// 生成文件名：[推广计划名称]-[推广组名称]-[是否创意]-[是否分网站]-[时间分布].csv

		if (qp.getGroupId() != null) {
			CproGroup group = cproGroupMgr.findCproGroupById(qp.getGroupId());
			if (group != null) {
				fileName = group.getGroupName() + "-";
			}
		} else if (qp.getPlanId() != null) {
			CproPlan plan = cproPlanMgr.findCproPlanById(qp.getPlanId());
			if (plan != null) {
				fileName = plan.getPlanName() + "-";
			}
		} else {
			fileName = user.getUsername() + "-";
		}

		fileName += this.getText("download.unit.filename.prefix");

		try {
			// 主文件名（时间之前部分）不能超过80个字符，中文算两个字符
			fileName = StringUtils.subGBKString(fileName, 0,
					ReportWebConstants.FILE_MAIN_NAME_MAXLENGTH);
		} catch (UnsupportedEncodingException e1) {
			LogUtils.error(log, e1);
		}
		fileName += "-" + sd.format(from) + "-" + sd.format(to) + ".csv";

		try {
			// 中文文件名需要用ISO8859-1编码
			fileName = new String(fileName.getBytes(fileEncoding), "ISO8859-1");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage());
		}
		return SUCCESS;
	}

	public String downloadByDay() throws IOException {
		// 初始化一下参数
		super.initParameter();

		List<ReportDayViewItem> itemList = getReportDayViewItems();
		ReportSumData sum = getReportSumData(itemList.size());

//		if (super.isOnlyToday(getFrom(), getTo())) {
//			super.clearNotRealtimeStat(itemList, sum);
//		}

		ReportDayVo vo = new ReportDayVo();// 分日报表下载使用的VO
		// 下载的CSV共有三部分
		// 1、列头，
		// 2、列表，
		// 3、汇总信息

		boolean showTransData = this.transReportFacade.isTransToolSigned(
				userId, false);
		boolean transDataValid = this.transReportFacade.needToFetchTransData(
				userId, from, to, false);
		vo.setShowTransData(showTransData);
		vo.setTransDataValid(transDataValid);
		vo.setHeaders(generateReportDayHeader(showTransData));
		vo.setDetails(itemList);
		vo.setSummary(sum);
		vo.setSummaryText(this.getText("download.summary.unit.day",
				new String[] { String.valueOf(sum.getCount()) }));

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(vo, output);

		// 设置下载需要使用到的一些属性
		byte[] bytes = output.toByteArray();
		inputStream = new ByteArrayInputStream(bytes);
		;
		fileSize = bytes.length;

		fileName = this.getText("download.unit.day.filename.prefix");

		try {
			// 主文件名（时间之前部分）不能超过80个字符，中文算两个字符
			fileName = StringUtils.subGBKString(fileName, 0,
					ReportWebConstants.FILE_MAIN_NAME_MAXLENGTH);
		} catch (UnsupportedEncodingException e1) {
			LogUtils.error(log, e1);
		}
		fileName += "-" + sd.format(from) + "-" + sd.format(to) + ".csv";

		try {
			// 中文文件名需要用ISO8859-1编码
			fileName = new String(fileName.getBytes(fileEncoding), "ISO8859-1");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage());
		}
		return SUCCESS;
	}

	public String downloadBySite() throws IOException {
		// 初始化一下参数
		super.initParameter();

		@SuppressWarnings("unchecked")
		List<ReportSiteViewItem> itemList = (List<ReportSiteViewItem>) getReportSiteViewItems();
		ReportSumData sum = getReportSumData(itemList.size());

		// 处理当日和昨日不入库的uv和转化数据 add by wangchongjie since 2012.11.03
		this.reportFacade.postHandleTransAndUvData(userId, from, to, itemList,
				sum);

//		if (super.isOnlyToday(getFrom(), getTo())) {
//			super.clearNotRealtimeStat(itemList, sum);
//		}

		ReportSiteVo vo = new ReportSiteVo();// 分日报表下载使用的VO
		// 下载的CSV共有三部分
		// 1、列头，
		// 2、列表，
		// 3、汇总信息

		vo.setHeaders(generateReportSiteHeader());
		vo.setDetails(itemList);
		vo.setSummary(sum);
		vo.setSummaryText(this.getText("download.summary.unit.site",
				new String[] { String.valueOf(sum.getCount()) }));

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(vo, output);

		// 设置下载需要使用到的一些属性
		byte[] bytes = output.toByteArray();
		inputStream = new ByteArrayInputStream(bytes);
		;
		fileSize = bytes.length;

		fileName = this.getText("download.unit.site.filename.prefix");

		try {
			// 主文件名（时间之前部分）不能超过80个字符，中文算两个字符
			fileName = StringUtils.subGBKString(fileName, 0,
					ReportWebConstants.FILE_MAIN_NAME_MAXLENGTH);
		} catch (UnsupportedEncodingException e1) {
			LogUtils.error(log, e1);
		}
		fileName += "-" + sd.format(from) + "-" + sd.format(to) + ".csv";

		try {
			// 中文文件名需要用ISO8859-1编码
			fileName = new String(fileName.getBytes(fileEncoding), "ISO8859-1");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage());
		}
		return SUCCESS;
	}

	public String downloadBySubSite() throws IOException {
		// 初始化一下参数
		super.initParameter();

		/**
		 * 如果统计时间包含今天，则返回空
		 */
//		if (super.isMixTodayAndBefore(from, to) || super.isOnlyToday(from, to)) {
//			log.warn("分URL不提供今日的统计数据");
//			jsonObject.addData("list", Collections.emptyList());
//			jsonObject.addData("totalPage", 0);
//			jsonObject.addData("cache", 0);
//			jsonObject.addData("sum", null);
//			return SUCCESS;
//		}

		@SuppressWarnings("unchecked")
		List<ReportSiteViewItem> itemList = (List<ReportSiteViewItem>) getReportSubSiteViewItems();
		ReportSumData sum = getReportSubSiteSumData(itemList);

		// 处理当日和昨日不入库的uv和转化数据 add by wangchongjie since 2012.11.03
		this.reportFacade.postHandleTransAndUvData(userId, from, to, itemList,
				sum);

		ReportSiteVo vo = new ReportSiteVo();// 分日报表下载使用的VO
		// 下载的CSV共有三部分
		// 1、列头，
		// 2、列表，
		// 3、汇总信息

		vo.setHeaders(generateReportSiteHeader());
		vo.setDetails(itemList);
		vo.setSummary(sum);
		vo.setSummaryText(this.getText("download.summary.unit.bysite",
				new String[] { String.valueOf(sum.getCount()) }));

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(vo, output);

		// 设置下载需要使用到的一些属性
		byte[] bytes = output.toByteArray();
		inputStream = new ByteArrayInputStream(bytes);
		;
		fileSize = bytes.length;

		fileName = this.getText("download.unit.bysite.filename.prefix");

		try {
			// 主文件名（时间之前部分）不能超过80个字符，中文算两个字符
			fileName = StringUtils.subGBKString(fileName, 0,
					ReportWebConstants.FILE_MAIN_NAME_MAXLENGTH);
		} catch (UnsupportedEncodingException e1) {
			LogUtils.error(log, e1);
		}
		fileName += "-" + sd.format(from) + "-" + sd.format(to) + ".csv";

		try {
			// 中文文件名需要用ISO8859-1编码
			fileName = new String(fileName.getBytes(fileEncoding), "ISO8859-1");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage());
		}
		return SUCCESS;
	}

	private List<ReportDayViewItem> getReportDayViewItems() {
		List<Map<String, Object>> mergedData = null;

		// 1、获取统计数据
    	List<ReportDayViewItem> olapList = byDayStatService.queryByDayData(userId, null, null,
				Arrays.asList(new Long[]{qp.getUnitId()}), from, to, ReportConstants.TU_DAY);
		
		//2、获取UV数据
		List<Map<String, Object>> uvData = this.uvDataService.queryUnitData(userId, null, null,
				Arrays.asList(new Long[] { qp.getUnitId() }), from, to, 
				null, 0, ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);
			
		boolean needToFetchTransData = this.transReportFacade.needToFetchTransData(userId,from,to,false);
		
		if (needToFetchTransData) {
			
			TempSitesAndTrans tempSiteAndTrans = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userId);
			
			//4、获取转化数据
			List<Map<String, Object>> transData = transDataService.queryUnitData(userId, 
					tempSiteAndTrans.getTransSiteIds(), tempSiteAndTrans.getTransTargetIds(), null, null,
					Arrays.asList(new Long[] { qp.getUnitId() }), from, to, 
					null, 0, ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);
			
			//5、获取holmes数据
			List<Map<String, Object>> holmesData = holmesDataService.queryUnitData(userId, null, null, null, null,
					Arrays.asList(new Long[]{qp.getUnitId()}), from, to, null, 
					0, ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);	
			
			//6、Merge统计数据、UV数据、Holmes数据、转化数据（FROMDATE为Key）
			mergedData = this.reportFacade.mergeTransHolmesAndUvData(Constant.DorisDataType.UV, transData, holmesData, uvData, ReportConstants.FROMDATE);
			
		} else {
			//6、Merge统计数据、UV数据（FROMDATE为Key）
			mergedData = uvData;
		}

		List<ReportDayViewItem> dorisList = transformStatDataByDay(mergedData);
		
		List<ReportDayViewItem> resultList = ReportUtils.mergeItemList(dorisList, olapList, OlapConstants.COLUMN.SHOWDATE, 
				Constants.statMergeVals, ReportDayViewItem.class, true);
		
		ReportUtil.generateExtentionFields(resultList);
		
		if ("day".equalsIgnoreCase(qp.getOrderBy())
				&& "desc".equalsIgnoreCase(qp.getOrder())) {
			Collections.reverse(resultList);
		}

		return resultList;
	}

	/**
	 * Get the data list by site
	 * 
	 * @return
	 */
	private  List<? extends ReportSiteViewItem> getReportSiteViewItems() {
		int orient = ReportConstants.SortOrder.ASC;
		if ("desc".equalsIgnoreCase(qp.getOrder())) {
			orient = ReportConstants.SortOrder.DES;
		}
		String column = qp.getOrderBy();

        List<ReportMainSiteViewItem> olapList =siteStatMgr.queryAUnitDataByMainsite(
        		userId, qp.getUnitId(), from, to, column, orient, ReportConstants.TU_NONE);
		
		return olapList;
	}

	private List<? extends ReportSiteViewItem> getReportSubSiteViewItems() {
		int orient = ReportConstants.SortOrder.ASC;
		if ("desc".equalsIgnoreCase(qp.getOrder())) {
			orient = ReportConstants.SortOrder.DES;
		}
		
		List<ReportSubSiteViewItem> olapList =siteStatMgr.queryAUnitDataBySubsite(userId, qp.getUnitId(), qp.getSite(), 
				from, to, qp.getOrderBy(), orient, ReportConstants.TU_NONE);

		return olapList;
	}

	private ReportSumData getReportSumData(long count) {
		// 分日报告不涉及到过滤操作，为提高性能能，因此可直接使用doris汇总结果
		ReportSumData sum = new ReportSumData();
		// 1、查询统计数据的汇总数据
		
		List<UnitViewItem> olapSum = unitStatMgr.queryUnitData(userId, null, null, Arrays.asList(new Long[]{qp.getUnitId()}),
				from, to, null, 0, ReportConstants.TU_NONE);
		
		//2、查询UV数据的汇总数据
		List<Map<String, Object>> uvSum = uvDataService.queryUnitData(userId, null, null,
				Arrays.asList(new Long[]{qp.getUnitId()}), from, to, null, 0, 
				ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
		
		if (!CollectionUtils.isEmpty(uvSum)){
			Map<String, Object> sumData = uvSum.get(0);
			sum.fillStatRecord(sumData);
			sum.setCount(count);
		}

		if(! CollectionUtils.isEmpty(olapSum)){
			sum.mergeBasicField((StatInfo) olapSum.get(0));
		}
		sum.generateExtentionFields();
		
		return sum;

	}

	private ReportSumData getReportSubSiteSumData(
			List<ReportSiteViewItem> itemList) {
		ReportSumData sum = new ReportSumData();
		sum.setCount(Long.valueOf(itemList.size()));

		long srchsSum = 0, clksSum = 0, costSum = 0;
		BigDecimal ctrSum = new BigDecimal(0.0), acpSum = new BigDecimal(0.0), cpmSum = new BigDecimal(
				0.0);

		for (ReportSiteViewItem item : itemList) {
			srchsSum += item.getSrchs();
			clksSum += item.getClks();
			costSum += item.getCost();
			ctrSum = ctrSum.add(item.getCtr());
			acpSum = acpSum.add(item.getAcp());
			cpmSum = cpmSum.add(item.getCpm());
		}

		sum.setSrchs(srchsSum);
		sum.setClks(clksSum);
		sum.setCost(costSum);
		sum.setCtr(ctrSum.doubleValue());
		sum.setAcp(acpSum.doubleValue());
		sum.setCpm(cpmSum.doubleValue());

		return sum;
	}

	/**
	 * Fill the site detail
	 * 
	 * @param itemList
	 */
	private void merge(List<ReportSiteViewItem> itemList) {
		if (CollectionUtils.isEmpty(itemList)) {
			return;
		}
		List<String> siteUrls = new ArrayList<String>();
		for (ReportSiteViewItem item : itemList) {
			if (!StringUtils.isEmpty(item.getSite())) {
				siteUrls.add(item.getSite());
			}
		}
		List<SimplePopUpSiteInfo> vos = unionSiteMgr
				.findBasicSiteInfoByUrls(siteUrls);

		if (CollectionUtils.isEmpty(vos)) {
			return;
		}
		for (SimplePopUpSiteInfo vo : vos) {
			if (StringUtils.isEmpty(vo.getSiteUrl())) {
				continue;
			}
			for (ReportSiteViewItem item : itemList) {
				if (vo.getSiteUrl().equals(item.getSite())) {
					item.setSiteDesc(vo.getSiteDesc());
					item.setSiteId(vo.getSiteId());
					item.setSiteLink(vo.getSiteLink());
					item.setSiteName(vo.getSiteName());
					item.setSiteTrade(new int[] { vo.getFirstTradeId(),
							vo.getSecondTradeId() });
					item.setWm123Url(wm123SiteDetailPrefix + vo.getSiteId()
							+ wm123SiteDetailSuffix);
					continue;
				}
			}
		}
	}

	/**
	 * generateAccountInfo: 生成报表用的账户信息
	 * 
	 * @return
	 */
	protected ReportAccountInfo generateAccountInfo(String userName) {
		ReportAccountInfo accountInfo = new ReportAccountInfo();
		accountInfo.setReport(this.getText("download.account.report.unit"));
		accountInfo.setReportText(this.getText("download.account.report"));

		accountInfo.setAccount(userName);
		accountInfo.setAccountText(this.getText("download.account.account"));

		accountInfo.setDateRange(sd1.format(from) + " - " + sd1.format(to));
		accountInfo
				.setDateRangeText(this.getText("download.account.daterange"));

		String level = this.getText("download.account.level.allplan");

		// 设置成：所有推广计划/推广计划：XXXX（该推广计划名称）/推广组：XXXX（该推广组名称）
		if (QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(qp.getLevel())) {

			Integer planId = qp.getPlanId();
			if (planId != null) {
				CproPlan plan = cproPlanMgr.findCproPlanById(planId);
				if (plan != null) {
					level += "/" + this.getText("download.account.level.plan")
							+ plan.getPlanName();
				}
			}
		} else if (QueryParameterConstant.LEVEL.LEVEL_GROUP.equals(qp
				.getLevel())) {
			Integer groupId = qp.getGroupId();
			if (groupId != null) {
				CproGroup group = cproGroupMgr.findCproGroupById(groupId);
				if (group != null) {
					CproPlan plan = cproPlanMgr.findCproPlanById(group
							.getPlanId());
					level += "/" + this.getText("download.account.level.plan")
							+ plan.getPlanName();
					level += "/" + this.getText("download.account.level.group")
							+ group.getGroupName();
				}
			}

		} else {
			// account级别不用设置具体信息
		}
		accountInfo.setLevel(level);
		accountInfo.setLevelText(this.getText("download.account.level"));

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
	protected String[] generateReportHeader(boolean showTransData) {

		String[] headers = null;
		if (showTransData) {
			headers = this.getText("download.unit.head.col.has.trans").split(
					",");
		} else {
			headers = this.getText("download.unit.head.col").split(",");
		}
		return headers;
	}

	protected String[] generateReportDayHeader(boolean showTransData) {
		String[] headers = null;
		if (showTransData) {
			headers = this.getText("download.unit.day.head.col.has.trans")
					.split(",");
		} else {
			headers = this.getText("download.unit.day.head.col").split(",");
		}
		return headers;
	}

	protected String[] generateReportSiteHeader() {
		String[] headers = new String[DOWN_REPORT_UNIT_BYSITE_COL_NUM];
		for (int col = 0; col < headers.length; col++) {
			headers[col] = this.getText("download.unit.bysite.head.col"
					+ (col + 1));
		}
		return headers;
	}

	/**
	 * generateReportDetail: 生成报表用的列表体
	 * 
	 * @since
	 */
	protected void generateReportDetail() {
		// 暂时不用
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
		sum.setSrchuv(sumData.getSrchuv());
		sum.setClkuv(sumData.getClkuv());
		sum.setSummaryText(this.getText("download.summary.unit",
				new String[] { String.valueOf(sumData.getUnitCount()) }));// 添加“合计”

		return sum;
	}

	// ---------------------内部方法---------------------
	/**
	 * <p>
	 * 获取待查询IDS的方法，对于传入的qp.groupId和qp.ids也加入到考虑之列
	 * <p>
	 * 获取ids的的条件为为以下顺序三选一：1、aot；2、qp.groupId； 3、qp.ids；
	 */
	protected void genQueryItemIds(AotReportItem aotItems) {

		if (aotItems != null && aotItems.getObjIdList() != null) {
			// 如果aot不空则以aot信息设置之
			qp.setIds(aotItems.getObjIdList());
		} else {
			// 如果没有aot信息则以Unitid设置，如果没有Unitid则以ids设置
			long queryId = qp.getUnitId() == null ? 0 : qp.getUnitId();
			if (queryId > 0) {
				qp.setIds(new ArrayList<Long>());
				qp.getIds().add(queryId);
			}
		}

	}

	/**
	 * <p>
	 * getViewItems: 生成前端VO，主要经历以下四步
	 * </p>
	 * 1、如果需要查询AOT则查询之以获取需要过滤的ids 2、查询实体对象 3、获取统计数据并merge第二步的数据 4、按照统计字段来过滤
	 * 
	 * @param aotItems
	 *            获取的AOT信息
	 * @param forceUnPagable
	 *            是否强制不分页，一般在下载列表中会用到强制不分页。
	 * @return 满足前端查询条件的对象
	 */
	private List<UnitViewItem> getViewItems(AotReportItem aotItems,
			boolean forceUnPagable) {

		List<UnitViewItem> infoData = new ArrayList<UnitViewItem>();

		if (aotItems == null || aotItems.getObjIdList() == null
				|| aotItems.getObjIdList().size() > 0) {

			// 如果查询条件中没有AOT，或者AOT返回的结果集>0则走DB查询

			List<Long> filterIds = null;// 需要按IDS来滤过的。

			if (count == 0) {
				// 记录条数为0的话就不用进行后端查询了

			} else {

				boolean needPagable = false;// 分页查询标识
				if (count > ReportWebConstants.FRONT_SORT_THRESHOLD) {
					/**
					 * 在没有设置强制不分页（下载功能时使用）时，
					 * 记录条件大于1W就不用按照viewState（显示状态）和统计字段排序，也不用按统计字段过滤，后端来分页。
					 */
					needPagable = true;
				} else {
					// 如果小于1W的话就取出所有的VO数据和统计数据，在前端进行分页处理
				}

				// 生成planId字段过滤列表，生成Doris排序方式值
				List<Integer> planIds = null;
				if (qp.getPlanId() != null && qp.getPlanId() > 0) {
					planIds = new ArrayList<Integer>();
					planIds.add(qp.getPlanId());
				}
				// 生成planId字段过滤列表，生成Doris排序方式值
				List<Integer> groupIds = null;
				if (qp.getGroupId() != null && qp.getGroupId() > 0) {
					groupIds = new ArrayList<Integer>();
					groupIds.add(qp.getGroupId());
				}

				// 1、从DB获取数据
				infoData = reportCproUnitMgr.findCproUnitViewItem(qp,
						needPagable && !forceUnPagable);

				// 2、获取对应ids的统计数据
				filterIds = new ArrayList<Long>();
				for (UnitViewItem item : infoData) {
					filterIds.add(item.getUnitId());
				}
				
				//3、获取对应的统计数据+UV数据+Holmes数据+转化数据				
				List<UnitViewItem> olapList = this.getStatAndTransData(needPagable, planIds, groupIds, filterIds);
				
				//4、合并doris数据和mysql数据
				if (!CollectionUtils.isEmpty(olapList)) {//数据不为空则需要合并
					
					if (!ReportConstants.isStatField(qp.getOrderBy())
							&&
						!ReportConstants.isTransDataField(qp.getOrderBy())
							&&
						!ReportConstants.isUVField(qp.getOrderBy())
							&&
						!ReportConstants.isHolmesField(qp.getOrderBy())
					) {
						//如果不以doris字段排序，则按照数据库字段为序进行合并
						fillMergeDataWithMasterOrder(infoData, olapList);
						
					} else {
						
						//否则按照doris字段为序合并，注意：此时statAndTransData为排序号的结果
						infoData = fillMergeDataWithSlaveOrder(infoData, olapList);
						
					}
				}

				if (!needPagable) {
					// 如果不分页则才能按统计字段进行过虑，即当数据大太时就算前端传了按统计字段过滤也不去过滤。
					infoData = filterByStatField(qp, infoData);

					// 如果有被过滤则需要重新设置行数大小。
					count = infoData.size();
				}
			}
		} else {
			// 否则表示有按AOT进行过滤，但结果集为空，不需要再进行后续查询了。一般来说走不到该分支
			log.warn("query aot[" + qp.getAotItemId()
					+ "] for filter ids, but return empty collection");
		}

		return infoData;
	}

	/**
	 * 获取创意层级doris数据
	 * 
	 * @param needPagable
	 * @param planIds
	 * @param groupIds
	 * @param unitIds
	 * @return 下午4:18:59 created by wangchongjie
	 */
//	private List<Map<String, Object>> getUnitDorisData(boolean needPagable,List<Integer> planIds,
//			List<Integer> groupIds, List<Long> unitIds){
//		boolean shouldCache = cacheService.shouldCache(from, to);
//		
//		StringBuilder sb = new StringBuilder();
//		sb.append(ReportCache.CACHE_KEY.UNIT).append(userId).append(from).append(to)
//			.append(null==planIds?"":planIds.toString())
//			.append(null==groupIds?"":groupIds.toString())
//			.append(null==unitIds?"":unitIds.toString())
//			.append(null==qp.getOrder()?"":qp.getOrder())
//			.append(null==qp.getOrderBy()?"":qp.getOrderBy());
//		String cacheKey = DigestUtils.md5Hex(sb.toString());
//
//		//0、从缓存中获取数据
//        if (shouldCache) {
//        	List<UnitViewItem> cacheData = (List<UnitViewItem>) cacheService.getFromCache(cacheKey);
//        	
//            if (null != cacheData) {
//            	log.debug(ReportCache.CACHE_KEY.UNIT + " key=[" + cacheKey +"] query success! "+cacheData);
//            	
//            	List<Map<String, Object>> statData = new ArrayList<Map<String, Object>>();
//            	for(UnitViewItem item : cacheData){
//            		Map<String, Object> record = new HashMap<String, Object>();
//            		item.reverseFillStatData(record);
//            		record.put(ReportConstants.GROUP, item.getGroupId());
//            		record.put(ReportConstants.PLAN, item.getPlanId());
//            		record.put(ReportConstants.UNIT, item.getUnitId());
//            		statData.add(record);
//            	}  
//                return statData;
//            }
//        } else {
//            log.debug(ReportCache.CACHE_KEY.UNIT + " miss cache key=[" + cacheKey + "]");
//        }
//		
//		//2、从doris中获取创意层级列表数据
//		List<Map<String, Object>> mergedData = this.getStatAndTransData(needPagable, planIds, groupIds, unitIds);
//
//		//3、将数据存储至缓存
//		if (shouldCache) {
//			List<UnitViewItem> cacheData = new ArrayList<UnitViewItem>();
//			
//			for(Map<String, Object> o : mergedData){
//				UnitViewItem item = new UnitViewItem();
//				
//				item.fillStatRecord4Cache(o);
//				item.setPlanId((Integer) o.get(ReportConstants.PLAN));
//				item.setGroupId((Integer) o.get(ReportConstants.GROUP));
//				item.setUnitId((Long) o.get(ReportConstants.UNIT));
//				
//				cacheData.add(item);
//			}
////			if (! CollectionUtils.isEmpty(mergedData) ) {
//				cacheService.putIntoCache(cacheKey, (ArrayList<UnitViewItem>) cacheData);
//				log.debug(ReportCache.CACHE_KEY.UNIT + " key=[" + cacheKey + "] store success! " + cacheData);
////	        }
//	     }
//		
//		return mergedData;
//	}

	private List<UnitViewItem> getStatAndTransData(boolean needPagable,
			List<Integer> planIds, List<Integer> groupIds, List<Long> unitIds) {

		int orient = ReportConstants.SortOrder.ASC;
		if (QueryParameterConstant.SortOrder.SORTORDER_DESC.equalsIgnoreCase(qp
				.getOrder())) {
			orient = ReportConstants.SortOrder.DES;
		}

		// 需要以统计字段排序？
		boolean isOrderByStatDataField = !needPagable
				&& ReportConstants.isStatField(qp.getOrderBy());

		// 需要以转化字段排序？
		boolean isOrderByTransDataField = !needPagable
				&& ReportConstants.isTransDataField(qp.getOrderBy());

		// 需要以转化字段排序？
		boolean isOrderByUvDataField = !needPagable
				&& ReportConstants.isUVField(qp.getOrderBy());

		// 需要以转化字段排序？
		boolean isOrderByHolmesDataField = !needPagable
				&& ReportConstants.isHolmesField(qp.getOrderBy());

		// 1、获取统计数据
		List<UnitViewItem> olapList;
		if (isOrderByStatDataField) {
			//如果不需要分页且是按统计字段排序，则用统计数据的排序顺序进行数据合并
			olapList = unitStatMgr.queryUnitData(userId, planIds, groupIds, unitIds, from, to, qp.getOrderBy(), orient, ReportConstants.TU_NONE);
		} else {
			olapList = unitStatMgr.queryUnitData(userId, planIds, groupIds, unitIds, from, to, null, 0, ReportConstants.TU_NONE);
		}

		// 2、获取UV数据
		List<Map<String, Object>> uvData = null;
		if (isOrderByUvDataField) {

			// 如果不需要分页且是按UV字段排序，则用UV数据的排序顺序进行数据合并
			uvData = uvDataService
					.queryUnitData(userId, planIds, groupIds, unitIds, from,
							to, qp.getOrderBy(), orient,
							ReportConstants.TU_NONE,
							Constant.REPORT_TYPE_DEFAULT, 0, 0);
		} else {
			/**
			 * 如果是需要分页或者排序字段不是统计字段，则用DB查询出来的VO的顺序进行数据合并
			 * 此处有个言下意：前端默认按srchs降排，如果发现数据量太大则放弃该规则
			 */
			// 获取ids对应的统计数据
			uvData = uvDataService.queryUnitData(userId, planIds, groupIds,
					unitIds, from, to, null, 0, ReportConstants.TU_NONE,
					Constant.REPORT_TYPE_DEFAULT, 0, 0);
		}

		// 3、如果需要获取转化数据，则获取转化数据并合并
		boolean needToFetchTransData = this.transReportFacade
				.needToFetchTransData(userId, from, to, false);

		// 需要返回的数据
		List<Map<String, Object>> mergedData = null;

		if (needToFetchTransData) {

			TempSitesAndTrans tempSiteAndTrans = TransReportHelper
					.getInstance().getTransSiteIdsAndTargetIds(userId);

			// 4、获取转化数据
			List<Map<String, Object>> tranData = null;
			if (!isOrderByTransDataField) {
				tranData = transDataService.queryUnitData(userId,
						tempSiteAndTrans.getTransSiteIds(),
						tempSiteAndTrans.getTransTargetIds(), planIds,
						groupIds, unitIds, from, to, null, 0,
						ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT,
						0, 0);
			} else {
				tranData = transDataService.queryUnitData(userId,
						tempSiteAndTrans.getTransSiteIds(),
						tempSiteAndTrans.getTransTargetIds(), planIds,
						groupIds, unitIds, from, to, qp.getOrderBy(), orient,
						ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT,
						0, 0);
			}

			// 5、获取Holmes数据
			List<Map<String, Object>> holmesData = null;
			if (!isOrderByHolmesDataField) {
				holmesData = holmesDataService.queryUnitData(userId, null,
						null, planIds, groupIds, unitIds, from, to, null, 0,
						ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT,
						0, 0);
			} else {
				holmesData = holmesDataService.queryUnitData(userId, null,
						null, planIds, groupIds, unitIds, from, to,
						qp.getOrderBy(), orient, ReportConstants.TU_NONE,
						Constant.REPORT_TYPE_DEFAULT, 0, 0);
			}
			
			//6、判断排序字段
			Integer orderByField = this.getOrderByXFiled(isOrderByStatDataField, isOrderByUvDataField, isOrderByHolmesDataField, isOrderByTransDataField);
			
			//7、Merge统计数据、UV数据、Holmes数据、转化数据
			mergedData = this.reportFacade.mergeTransHolmesAndUvData(orderByField, tranData, holmesData, uvData, ReportConstants.UNIT);
			
		}else{
			
			//7、Merge统计数据和UV数据
			mergedData = uvData;
		}
		
		List<UnitViewItem> dorisList = new ArrayList<UnitViewItem>();
		List<UnitViewItem> resultList = new ArrayList<UnitViewItem>();
		//7、填充数据
		if (! CollectionUtils.isEmpty(mergedData)) {
			for (Map<String, Object> row : mergedData) {
				UnitViewItem item = new UnitViewItem();
				if (row != null) {
					item.setPlanId(Integer.valueOf(row.get(ReportConstants.PLAN).toString()));
					item.setGroupId(Integer.valueOf(row.get(ReportConstants.GROUP).toString()));
					item.setUnitId(Long.valueOf(row.get(ReportConstants.UNIT).toString()));
					item.fillStatRecord(row);
					dorisList.add(item);
				}
			}
		}
		
		boolean keepMainListOrder = ! isOrderByStatDataField;
		resultList = ReportUtils.mergeItemListKeepOrder(dorisList, olapList,  Constants.COLUMN.UNITID, 
				Constants.statMergeVals, UnitViewItem.class, SortOrder.val(qp.getOrder()), true, keepMainListOrder);
		
		ReportUtil.generateExtentionFields(resultList);
		
		return resultList;
		
	}

	/**
	 * getStatItems: 获取天粒度的统计数据
	 * 需要经过以下五步：
	 * 1、如果需要查询AOT则查询之以获取需要过滤的ids
	 * 2、查询实体对象Ids列表
	 * 3、获取统计数据并merge第二步的数据
	 * 4、按照统计字段来过滤
	 * 5、将统计数据汇总到天并返回
	 * @return      
	 * @since 
	*/
	private List<ReportDayViewItem> getStatItemsFilterByStatField(AotReportItem aotItems) {
		List<Long> filterIds = null;//需要按IDS来滤过的。
		List<Map<String, Object>> uvData = null;//UV数据
		
		//1、如果需要查询AOT则查询之以获取需要过滤的ids
		genQueryItemIds(aotItems);

		// 2、查询实体对象
		if (qp.hasNoneStatField4Filter()) {
			filterIds = reportCproUnitMgr.findAllCproUnitIdsByQuery(qp, false);

			if (CollectionUtils.isEmpty(filterIds)) {
				return new ArrayList<ReportDayViewItem> ();
			}
		}

		// 3、生成planId字段过滤列表，生成Doris排序方式值
		List<Integer> planIds = null;
		if (qp.getPlanId() != null && qp.getPlanId() > 0) {
			planIds = new ArrayList<Integer>();
			planIds.add(qp.getPlanId());
		}

		// 4、生成groupId字段过滤列表，生成Doris排序方式值
		List<Integer> groupIds = null;
		if (qp.getGroupId() != null && qp.getGroupId() > 0) {
			groupIds = new ArrayList<Integer>();
			groupIds.add(qp.getGroupId());
		}
			
		//5、获取统计数据
		List<UnitViewItem> olapList = unitStatMgr.queryUnitData(userId, planIds, groupIds, filterIds,
				from, to, null, 0, ReportConstants.TU_NONE);
		
		//6、获取UV数据
		uvData = uvDataService.queryUnitData(userId, planIds, groupIds, filterIds, 
											flashFrom, flashTo, null, 0, 
											ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
		
		List<UnitViewItem> dorisList = new ArrayList<UnitViewItem>();
		if (! CollectionUtils.isEmpty(uvData)) {
			for (Map<String, Object> row : uvData) {
				UnitViewItem item = new UnitViewItem();
				if (row != null) {
					item.setPlanId(Integer.valueOf(row.get(ReportConstants.PLAN).toString()));
					item.setGroupId(Integer.valueOf(row.get(ReportConstants.GROUP).toString()));
					item.setGroupId(Integer.valueOf(row.get(ReportConstants.UNIT).toString()));
					item.fillStatRecord(row);
					dorisList.add(item);
				}
			}
		}
		//7、合并统计数据和UV数据
		List<UnitViewItem> resultList = ReportUtils.mergeItemList(dorisList, olapList, Constants.COLUMN.UNITID, 
				Constants.statMergeVals, UnitViewItem.class, true);
		
		ReportUtil.generateExtentionFields(resultList);
		
		//8、按照统计字段来过滤
		resultList = filterByStatFieldForList(qp, resultList);//按统计字段过滤
	
		//9、获取过滤后的groupids
		filterIds = new ArrayList<Long>();
		for(UnitViewItem item : resultList) {
			filterIds.add(item.getUnitId());
		}

		// 10、再次查询doris
		if (CollectionUtils.isEmpty(filterIds)) {
			return new ArrayList<ReportDayViewItem>();
		} else {
			//获取统计数据
			List<ReportDayViewItem> olapDateList = byDayStatService.queryByDayData(userId, planIds, groupIds,
					filterIds, flashFrom, flashTo, ReportConstants.TU_DAY);
			
			//获取UV数据
			uvData = uvDataService.queryUnitDataByTime(userId, planIds, groupIds, filterIds, 
								flashFrom, flashTo, null, 0, 
								ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);
			
			List<ReportDayViewItem> dorisDateList = new ArrayList<ReportDayViewItem>();
			dorisDateList = ReportUtil.transformStatDataByDay(uvData);
			
			List<ReportDayViewItem> resultDateList = ReportUtils.mergeItemList(dorisDateList, olapDateList, OlapConstants.COLUMN.SHOWDATE, 
					Constants.statMergeVals, ReportDayViewItem.class, true);
			
			ReportUtil.generateExtentionFields(resultList);
			
			//11、获取最终分日统计数据
			return resultDateList;
		}
	}

	/**
	 * getStatItemsFilterByNonStatField:
	 * 根据ids查询user维度的统计信息，前提是：过滤字段中不含统计字段
	 * @return      
	 * @since 
	*/
	private List<ReportDayViewItem> getStatItemsFilterByNonStatField(AotReportItem aotItems) {
		
		List<Long> filterIds = null;//需要按IDS来滤过的。
		
		//1、如果需要查询AOT则查询之以获取需要过滤的ids
		genQueryItemIds(aotItems);

		// 2、查询ids
		if (qp.hasNoneStatField4Filter()) {
			filterIds = reportCproUnitMgr.findAllCproUnitIdsByQuery(qp, false);
			if (CollectionUtils.isEmpty(filterIds)) {
				return new ArrayList<ReportDayViewItem>();
			}
		}

		// 3、生成planId字段过滤列表，生成Doris排序方式值
		List<Integer> planIds = null;
		if (qp.getPlanId() != null && qp.getPlanId() > 0) {
			planIds = new ArrayList<Integer>();
			planIds.add(qp.getPlanId());
		}
		// 4、生成groupId字段过滤列表，生成Doris排序方式值
		List<Integer> groupIds = null;
		if (qp.getGroupId() != null && qp.getGroupId() > 0) {
			groupIds = new ArrayList<Integer>();
			groupIds.add(qp.getGroupId());
		}
		
		//5、根据ids获取统计数据
		List<ReportDayViewItem> olapDateList = byDayStatService.queryByDayData(userId, planIds, groupIds,
				filterIds, flashFrom, flashTo, ReportConstants.TU_DAY);
		
		//6、根据ids获取UV数据
		List<Map<String, Object>> uvData = uvDataService.queryUnitDataByTime(
				userId, planIds, groupIds,filterIds, 
				flashFrom, flashTo, null, 0, 
				ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);
		
		List<ReportDayViewItem> dorisDateList = ReportUtil.transformStatDataByDay(uvData);
		
		//6、合并统计数据和UV数据
		List<ReportDayViewItem> resultDateList = ReportUtils.mergeItemList(dorisDateList, olapDateList, OlapConstants.COLUMN.SHOWDATE, 
				Constants.statMergeVals, ReportDayViewItem.class, true);
		
		ReportUtil.generateExtentionFields(resultDateList);

		//7、返回合并结果
		return resultDateList;
	}

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
			sumData.setSrchuv(sumData.getSrchuv() + item.getSrchuv());
			sumData.setClkuv(sumData.getClkuv() + item.getClkuv());
		}

		// 2、生成扩展数据
		sumData.generateExtentionFields();

		// 3、设置总数
		sumData.setUnitCount(infoData.size());

		return sumData;
	}

	public String ajaxPageSum() throws Exception {// 分URL报告的汇总信息

		initParameter();
		if (qp.getUnitId() == null) {
			throw new Exception("创意ID不能为空");
		}
		if (StringUtils.isEmpty(qp.getKeyword())) {
			throw new Exception("网站域名不能为空");
		}

		/**
		 * 如果统计时间包含今天，则返回空
		 */
//		if (super.isMixTodayAndBefore(from, to) || super.isOnlyToday(from, to)) {
//			log.warn("分URL不提供今日的统计数据");
//			jsonObject.addData("list", Collections.emptyList());
//			jsonObject.addData("totalPage", 0);
//			jsonObject.addData("cache", 0);
//			jsonObject.addData("sum", null);
//			return SUCCESS;
//		}

		long unitId = qp.getUnitId();
		String site = qp.getKeyword();

		jsonObject = new AbsJsonObject();
		//获取网站域名维度的统计信息

		List<UnitMainSiteItem> olapList = siteStatMgr.queryUnitMainsiteData(userId, unitId, site, from, to);

		StatInfo statInfo = new StatInfo();
		if(!CollectionUtils.isEmpty(olapList)) {
			statInfo.mergeBasicField(olapList.get(0));
			statInfo.generateExtentionFields();
		}
		jsonObject.addData("stat", statInfo);

		return SUCCESS;
	}

	public String ajaxPage() throws Exception {// 分URL报告的列表

		initParameter();
		if (qp.getUnitId() == null) {
			new Exception("创意ID不能为空");
		}
		if (StringUtils.isEmpty(qp.getKeyword())) {
			new Exception("网站域名不能为空");
		}

		/**
		 * 如果统计时间包含今天，则返回空
		 */
//		if (super.isMixTodayAndBefore(from, to) || super.isOnlyToday(from, to)) {
//			log.warn("分URL不提供今日的统计数据");
//			jsonObject.addData("list", Collections.emptyList());
//			jsonObject.addData("totalPage", 0);
//			jsonObject.addData("cache", 0);
//			jsonObject.addData("sum", null);
//			return SUCCESS;
//		}

		long unitId = qp.getUnitId();
		String site = qp.getKeyword();
		if (qp.getPage() == null || qp.getPage() < 0) {
			qp.setPage(0);
		}

		if (qp.getPageSize() == null || qp.getPageSize() < 1) {
			qp.setPageSize(ReportConstants.PAGE_SIZE);
		}
		jsonObject = new AbsJsonObject();
		ClickStatVo vo = clickUrlService.queryClickUrlCount(userId,
				qp.getUnitId(), site, qp.getDateStart(), qp.getDateEnd());
		UnitUrlSumData sum = new UnitUrlSumData();
		sum.setCount(vo.getCount());
		sum.setClks(vo.getClks());
		sum.setCost(vo.getCost());
		if (vo.getClks() > 0) {
			sum.setAcp(vo.getCost() / vo.getClks());
		}
		jsonObject.addData("sum", sum);

		List<Map<String, Object>> data = clickUrlService.queryClickUrlInfo(
				userId, unitId, site, qp.getDateStart(), qp.getDateEnd(),
				qp.getPage(), qp.getPageSize(), qp.getOrderBy(), qp.getOrder());
		jsonObject.addData("list", data);

		// 强制不缓存
		jsonObject.addData("cache", ReportConstants.Boolean.FALSE);

		jsonObject.addData("totalPage", getTotalPage(vo.getCount()));

		return SUCCESS;
	}

	/********************* [g/s]etters ******************************/

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

	public UnionSiteMgr getUnionSiteMgr() {
		return unionSiteMgr;
	}

	public void setUnionSiteMgr(UnionSiteMgr unionSiteMgr) {
		this.unionSiteMgr = unionSiteMgr;
	}

	public ReportCproUnitMgr getReportCproUnitMgr() {
		return reportCproUnitMgr;
	}

	public void setReportCproUnitMgr(ReportCproUnitMgr reportCproUnitMgr) {
		this.reportCproUnitMgr = reportCproUnitMgr;
	}

	public String getWm123SiteDetailPrefix() {
		return wm123SiteDetailPrefix;
	}

	public void setWm123SiteDetailPrefix(String wm123SiteDetailPrefix) {
		this.wm123SiteDetailPrefix = wm123SiteDetailPrefix;
	}

	public String getWm123SiteDetailSuffix() {
		return wm123SiteDetailSuffix;
	}

	public void setWm123SiteDetailSuffix(String wm123SiteDetailSuffix) {
		this.wm123SiteDetailSuffix = wm123SiteDetailSuffix;
	}

	public ClickUrlService getClickUrlService() {
		return clickUrlService;
	}

	public void setClickUrlService(ClickUrlService clickUrlService) {
		this.clickUrlService = clickUrlService;
	}

	public void setCproUnitMgr(CproUnitMgr cproUnitMgr) {
		this.cproUnitMgr = cproUnitMgr;
	}

	public ReportCacheService getCacheService() {
		return cacheService;
	}

	public void setCacheService(ReportCacheService cacheService) {
		this.cacheService = cacheService;
	}
}
