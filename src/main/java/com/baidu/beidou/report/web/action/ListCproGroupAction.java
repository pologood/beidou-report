package com.baidu.beidou.report.web.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.util.CollectionUtils;

import com.baidu.beidou.aot.control.AotReportItem;
import com.baidu.beidou.cprogroup.service.CproGroupMgr;
import com.baidu.beidou.cproplan.bo.CproPlan;
import com.baidu.beidou.cproplan.service.CproPlanMgr;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.olap.constant.Constants;
import com.baidu.unbiz.olap.constant.OlapConstants;
import com.baidu.unbiz.olap.constant.SortOrder;
import com.baidu.beidou.olap.service.GroupStatService;
import com.baidu.beidou.olap.service.ReportByDayStatService;
import com.baidu.unbiz.olap.util.ReportUtils;
import com.baidu.beidou.olap.vo.GroupViewItem;
import com.baidu.beidou.olap.vo.ReportDayViewItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.report.service.ReportCacheService;
import com.baidu.beidou.report.service.ReportCproGroupMgr;
import com.baidu.beidou.report.util.ReportUtil;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.group.GroupReportSumData;
import com.baidu.beidou.report.vo.group.GroupReportVo;
import com.baidu.beidou.report.vo.group.GroupSumData;
import com.baidu.beidou.stat.driver.bo.Constant;
import com.baidu.beidou.tool.util.TransReportHelper;
import com.baidu.beidou.tool.vo.TempSitesAndTrans;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouConstant;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.LogUtils;
import com.baidu.beidou.util.StringUtils;

public class ListCproGroupAction extends BeidouReportActionSupport {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1741336002877563352L;

	private CproPlanMgr cproPlanMgr = null;
	
	private CproGroupMgr cproGroupMgr;

	private ReportCproGroupMgr reportCproGroupMgr = null;
	
	private ReportCacheService cacheService;
	
	@Resource(name="groupStatServiceImpl")
	GroupStatService groupStatMgr;

	@Resource(name="reportByDayStatServiceImpl")
	private ReportByDayStatService byDayStatService;
	
	/** 结果集（全集）总行数 */
	int count;

	public String execute() {

		return SUCCESS;
	}

	@Override
	protected void initStateMapping() {
		if (qp != null) {
			qp.setStateMapping(ReportWebConstants.GROUP_FRONT_BACKEND_STATE_MAPPING
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
		
        List<ReportDayViewItem> statList; // 天粒度的汇总数据,key为时间，value为统计值

		// 首先看是否需要查询AOT获取IDS来按IDS来滤。
		AotReportItem aotItems = getAotItems();// null表示不按AOT，否则表示要按AOT

		if (qp.hasStatField4Filter()) {// 如果需要按统计字段过滤则要查询出[from,to]+groupId维度的数据

			statList = getStatItemsFilterByStatField(aotItems);// 时间粒度：天

		} else {
			// 否则只用查出user维度的，但需要传入过滤后的ids
			statList = getStatItemsFilterByNonStatField(aotItems);
		}

		String xml = generateXml(statList);

		jsonObject.addData("state", ReportWebConstants.FLASH_DATA_STATE.NORMAL);
		jsonObject.addData("data", xml);

		return SUCCESS;
	}

	/** 组列表使用 */
	public String ajaxList() {
		//new ReportWebConstants().setFRONT_SORT_THRESHOLD(40);
		try {
			// 初始化一下参数
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return SUCCESS;
		}

//        if (super.isMixTodayAndBefore(this.getFrom(), this.getTo())) {
//            jsonObject.setStatus(BeidouConstant.JSON_OPERATE_FAILED);
//            jsonObject.setGlobalMsg(getText("mixed.report.not.allowed"));
//            return SUCCESS;
//        }

		List<GroupViewItem> infoData = Collections.emptyList();// 目标结果集（可能是全集或者分页集）

		// 1、首先看是否需要查询AOT获取IDS来按IDS来滤。
		AotReportItem aotItems = getAotItems();// null表示不按AOT，否则表示要按AOT
		genQueryItemIds(aotItems);//如果需要按ids查询的话则生成qp.ids
		
		// 2、查询总条数，该数字可能不是最终值，因为有可能需要按统计值过滤，这样的话该值会被再次修改
		count = reportCproGroupMgr.countCproGroupViewItem(qp);

        // 3、获取VO列表
        infoData = getViewItems(aotItems, false);

		// 4、合并建议出价（单位是分，前端转换成元）
		if (aotItems != null
				&& aotItems.isNeedPrice()
				&& !CollectionUtils.isEmpty(aotItems.getObjIdList())
				&& !CollectionUtils.isEmpty(aotItems.getGroupPriceList())
				&& aotItems.getObjIdList().size() == aotItems
						.getGroupPriceList().size()) {
			merge(infoData, aotItems);
		}

		// 5、生成汇总的统计数据
		GroupSumData sumData = calculateSumData(infoData);

		// 6、生成缓存条件：总条件小于1W且当前取的是第一页数据。
		boolean cache = shouldCache(count);

		// 7、计算总页码
		int totalPage = super.getTotalPage(count);
		
		// 8、计算总的数据
		boolean hasElements = hasGroupcountWithoutConditions();
		
//		if (super.isOnlyToday(getFrom(), getTo())) {
//		    super.clearNotRealtimeStat(infoData, sumData);
//		}
		
		jsonObject.addData("list", infoData);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("cache", cache ? ReportConstants.Boolean.TRUE
				: ReportConstants.Boolean.FALSE);
		jsonObject.addData("sum", sumData);
		jsonObject.addData("hasElements", hasElements);
		
		// 9、处理当日和昨日不入库的uv和转化数据
		this.reportFacade.postHandleTransAndUvData(userId, from, to, infoData, sumData);
		
		return SUCCESS;
	}
	
	private boolean hasGroupcountWithoutConditions() {
		if (qp != null && qp.getPlanId() != null) {
			long groupCount = cproGroupMgr.countCproGroupByPlanId(qp.getPlanId());
			if (groupCount > 0L) {
				return true;
			}
			return false;
		}
		return true;
	}

//	public String ajaxListByDay() throws Exception {
//		jsonObject = new AbsJsonObject();
//		try {
//			super.initParameter();
//		} catch (Exception e) {
//			log.warn(e.getMessage(), e);
//			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED_SERVER);
//			jsonObject.addMsg(ReportWebConstants.ERR_DATE);
//
//			return SUCCESS;
//		}
//		
//        if (super.isMixTodayAndBefore(this.getFrom(), this.getTo())) {
//            jsonObject.setStatus(BeidouConstant.JSON_OPERATE_FAILED);
//            jsonObject.setGlobalMsg(getText("mixed.report.not.allowed"));
//            return SUCCESS;
//        }
//		
//		List<ReportDayViewItem> itemList = getReportDayViewItems();
//		ReportSumData sum = getReportSumData(itemList.size());
//
//		int totalPage = getTotalPage(itemList.size());
//		itemList = pagerList(itemList);
//		
//        if (super.isOnlyToday(getFrom(), getTo())) {
//            super.clearNotRealtimeStat(itemList, sum);
//        }
//        
//		jsonObject.addData("list", itemList);
//		jsonObject.addData("totalPage", totalPage);
//		jsonObject.addData("cache", 1);
//		jsonObject.addData("sum", sum);
//		
//		//处理当日和昨日不入库的uv和转化数据
//		//this.reportFacade.postHandleTransAndUvData(userId, from, to, itemList, sum);
//		
//		return SUCCESS;
//	}

	/**
	 * 下载推广组列表
	 * @return
	 * @throws IOException
	 */
	public String download() throws IOException {

		//1、初始化一下参数
		super.initParameter();
		
		//2、获取账户信息
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}

		//3、过滤Aot信息
		List<GroupViewItem> infoData = null;// 目标plan集
		AotReportItem aotItems = getAotItems();// null表示不按AOT，否则表示要按AOT
		genQueryItemIds(aotItems);//如果需要按ids查询的话则生成qp.ids
		
		count = reportCproGroupMgr.countCproGroupViewItem(qp);
		//4、获取统计数据(不分页)
		infoData = getViewItems(aotItems, true);//无统计时间粒度

		//5、生成汇总的统计数据
		GroupSumData sumData = calculateSumData(infoData);
		
		//处理当日和昨日不入库的uv和转化数据 add by wangchongjie since 2012.11.03
		this.reportFacade.postHandleTransAndUvData(userId, from, to, infoData, sumData);

//        if (super.isOnlyToday(getFrom(), getTo())) {
//            super.clearNotRealtimeStat(infoData, sumData);
//        }
		
		//6、下载的CSV共有四部分：1、账户基本信息，2、列头，3、列表，4、汇总信息	
		GroupReportVo vo = new GroupReportVo(qp.getLevel());// 报表下载使用的VO
		boolean showTransData = this.transReportFacade.isTransToolSigned(userId, false);
		boolean transDataValid = this.transReportFacade.needToFetchTransData(userId, from, to, false);
		vo.setShowTransData(showTransData);
		vo.setTransDataValid(transDataValid);
		vo.setAccountInfo(generateAccountInfo(user.getUsername()));
		vo.setDetails(infoData);
		vo.setHeaders(generateReportHeader(showTransData));
		vo.setSummary(generateReportSummary(sumData));

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(vo, output);

		//7、设置下载需要使用到的一些属性
		byte[] bytes = output.toByteArray();
		inputStream = new ByteArrayInputStream(bytes);
		fileSize = bytes.length;

		if (qp.getPlanId() != null) {
			// 如果指定了推广计划则不显示账户名
			CproPlan plan = cproPlanMgr.findCproPlanById(qp.getPlanId());
			if (plan != null) {
				fileName = plan.getPlanName() + "-";
			}
		} else {
			fileName = user.getUsername() + "-";
		}
		fileName += this.getText("download.group.filename.prefix");
		try {
			fileName = StringUtils.subGBKString(fileName, 0,
					ReportWebConstants.FILE_MAIN_NAME_MAXLENGTH);
		} catch (UnsupportedEncodingException e1) {
			LogUtils.error(log, e1);
		}

		fileName += "-" + sd.format(from) + "-" + sd.format(to) + ".csv";

		try {
			//中文文件名需要用ISO8859-1编码
			fileName = new String(fileName.getBytes(fileEncoding), "ISO8859-1");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage());
		}
		return SUCCESS;
	}

//	public String downloadByDay() throws Exception {
//		// 初始化一下参数
//		super.initParameter();
//		
//		List<ReportDayViewItem> itemList = getReportDayViewItems();
//		ReportSumData sum = getReportSumData(itemList.size());
//		
//		//处理当日和昨日不入库的uv和转化数据 add by wangchongjie since 2012.11.03
//		//this.reportFacade.postHandleTransAndUvData(userId, from, to, itemList, sum);
//		
//        if (super.isOnlyToday(getFrom(), getTo())) {
//            super.clearNotRealtimeStat(itemList, sum);
//        }
//		
//		ReportDayVo vo = new ReportDayVo();//分日报表下载使用的VO
//		//下载的CSV共有三部分
//		//1、列头，
//		//2、列表，
//		//3、汇总信息
//
//		boolean showTransData = this.transReportFacade.isTransToolSigned(userId, false);
//		boolean transDataValid = this.transReportFacade.needToFetchTransData(userId, from, to, false);
//		vo.setShowTransData(showTransData);
//		vo.setTransDataValid(transDataValid);
//		vo.setHeaders(generateReportDayHeader(showTransData));
//		vo.setDetails(itemList);
//		vo.setSummary(sum);
//		vo.setSummaryText(this.getText("download.summary.group.day", 
//				new String[]{String.valueOf(sum.getCount())}));
//
//		ByteArrayOutputStream output = new ByteArrayOutputStream();
//		ReportCSVWriter.getInstance().write(vo, output);
//
//		// 设置下载需要使用到的一些属性
//		byte[] bytes = output.toByteArray();
//		inputStream = new ByteArrayInputStream(bytes);
//		fileSize = bytes.length;
//
//		fileName = this.getText("download.group.day.filename.prefix");
//		try {
//			fileName = StringUtils.subGBKString(fileName, 0,
//					ReportWebConstants.FILE_MAIN_NAME_MAXLENGTH);
//		} catch (UnsupportedEncodingException e1) {
//			LogUtils.error(log, e1);
//		}
//
//		fileName += "-" + sd.format(from) + "-" + sd.format(to) + ".csv";
//
//		try {
//			//中文文件名需要用ISO8859-1编码
//			fileName = new String(fileName.getBytes(fileEncoding), "ISO8859-1");
//		} catch (UnsupportedEncodingException e) {
//			log.error(e.getMessage());
//		}
//		return SUCCESS;
//	}
	
//    private List<ReportDayViewItem> getReportDayViewItems() {
//        List<Map<String, Object>> mergedData = null;
//
//        // 1、获取统计数据
//        List<Map<String, Object>> statData = Collections.emptyList();
//        if (super.isOnlyToday(this.getFrom(), this.getTo())) {
//            statData = this.realtimeStatSrv.queryGroupData(qp.getUserId(),
//                            null, Arrays.asList(qp.getGroupId()),
//                            qp.getOrderBy(), qp.getOrder(),
//                            0, Integer.MAX_VALUE);
//        } else {
//        	statData = statMgr.queryGroupData(userId, null, Arrays.asList(new Integer[] { qp.getGroupId() }),
//                            from, to, null, 0, ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT);
//        }
//
//        // 2、获取UV数据
//        List<Map<String, Object>> uvData = this.uvDataService.queryGroupData(userId, null,
//                        Arrays.asList(new Integer[] { qp.getGroupId() }), from, to, null, 0,
//                        ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);
//
//        // 3、判断是否需要转化数据
//        boolean needToFetchTransData = this.transReportFacade.needToFetchTransData(userId, from,
//                        to, false);
//
//        if (needToFetchTransData) {
//            // 4、获取转化数据
//            List<Map<String, Object>> transData = transDataService.queryGroupData(userId, null,
//                            null, null, Arrays.asList(new Integer[] { qp.getGroupId() }), from, to,
//                            null, 0, ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);
//
//            // 5、获取holmes数据
//            List<Map<String, Object>> holmesData = holmesDataService.queryGroupData(userId, null,
//                            null, null, Arrays.asList(new Integer[] { qp.getGroupId() }), from, to,
//                            null, 0, ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);
//
//            // 6、Merge统计数据、UV数据、Holmes数据、转化数据（FROMDATE为Key）
//            mergedData = this.reportFacade.mergeTransHolmesUvAndStatData(DorisDataType.STAT,
//                            transData, holmesData, uvData, statData, ReportConstants.FROMDATE);
//
//        } else {
//            // 6、Merge统计数据、UV数据（FROMDATE为Key）
//            mergedData = this.uvReportFacade.mergeUvAndStatData(true, uvData, statData,
//                            ReportConstants.FROMDATE);
//        }
//
//        if ("day".equalsIgnoreCase(qp.getOrderBy()) && "desc".equalsIgnoreCase(qp.getOrder())) {
//            Collections.reverse(mergedData);
//        }
//
//        return transformStatDataByDay(mergedData);
//    }
//	
//	private ReportSumData getReportSumData(long count) {
//		//分日报告不涉及到过滤操作，为提高性能能，因此可直接使用doris汇总结果
//		ReportSumData sum = new ReportSumData();
//		List<Map<String, Object>> statSum = null;
//		
//		//1、查询统计数据的汇总数据
//		if (super.isOnlyToday(this.getFrom(), this.getTo())) {
//			statSum = this.realtimeStatSrv.queryGroupData(qp.getUserId(),
//                            null, Arrays.asList(qp.getGroupId()),
//                            qp.getOrderBy(), qp.getOrder(),
//                            0, Integer.MAX_VALUE);
//        }
//		else{
//			statSum= statMgr.queryGroupData(userId, null,
//					Arrays.asList(new Integer[]{qp.getGroupId()}), from, to, null, 0, 
//					ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT);
//		}
//		
//		//2、查询UV数据的汇总数据
//		List<Map<String, Object>> uvSum = uvDataService.queryGroupData(userId, null,
//				Arrays.asList(new Integer[]{qp.getGroupId()}), from, to, null, 0, 
//				ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
//		
//		//3、合并统计数据和UV数据
//		List<Map<String, Object>> mergedSum = uvReportFacade.mergeUvAndStatData(true, uvSum, statSum, ReportConstants.GROUP);
//		
//		if (!CollectionUtils.isEmpty(mergedSum)){
//			Map<String, Object> sumData = mergedSum.get(0);
//			sum.fillStatRecord(sumData);
//			sum.setCount(count);
//		}
//		
//		return sum;
//	}
	
	/**
	 * generateAccountInfo: 生成报表用的账户信息
	 * 
	 * @return
	 */
	protected ReportAccountInfo generateAccountInfo(String userName) {
		ReportAccountInfo accountInfo = new ReportAccountInfo();
		accountInfo.setReport(this.getText("download.account.report.group"));
		accountInfo.setReportText(this.getText("download.account.report"));

		accountInfo.setAccount(userName);
		accountInfo.setAccountText(this.getText("download.account.account"));

		accountInfo.setDateRange(sd1.format(from) + " - " + sd1.format(to));
		accountInfo
				.setDateRangeText(this.getText("download.account.daterange"));

		String level = this.getText("download.account.level.allplan");
		if (QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(qp.getLevel())) {
			Integer planId = qp.getPlanId();
			if (planId != null) {
				CproPlan plan = cproPlanMgr.findCproPlanById(planId);
				if (plan != null) {
					level += "/" + this.getText("download.account.level.plan")
							+ plan.getPlanName();
				}
			}
		}
		accountInfo.setLevel(level);
		accountInfo.setLevelText(this.getText("download.account.level"));

		return accountInfo;
	}

	/**
	 * generateReportHeader:生成报表用的列表头
	 * @param showTransData 
	 * 
	 * @return
	 * @since
	 */
	protected String[] generateReportHeader(boolean showTransData) {

		String[] headers = null;
		if (showTransData) {
			headers = this.getText("download.group.head.col.has.trans").split(",");
		} else {
			headers = this.getText("download.group.head.col").split(",");
		}
		return headers;
	}
	
//	protected String[] generateReportDayHeader(boolean showTransData) {
//		String[] headers = null;
//		if (showTransData) {
//			headers = this.getText("download.group.day.head.col.has.trans").split(",");
//		} else {
//			headers = this.getText("download.group.day.head.col").split(",");
//		}
//		return headers;
//	}

//	/**
//	 * generateReportDetail: 生成报表用的列表体
//	 * 
//	 * @since
//	 */
//	protected void generateReportDetail() {
//		// 暂时不用
//	}

	/**
	 * generateReportSummary: 生成汇总信息
	 * 
	 * @param sumData
	 *            汇总数据
	 * @return 用于表示报表的汇总信息
	 */
	protected GroupReportSumData generateReportSummary(GroupSumData sumData) {

		GroupReportSumData sum = new GroupReportSumData();
		sum.setClks(sumData.getClks());
		sum.setSrchs(sumData.getSrchs());
		sum.setCost(sumData.getCost());
		sum.setAcp(sumData.getAcp().doubleValue());
		sum.setCtr(sumData.getCtr().doubleValue());
		sum.setCpm(sumData.getCpm().doubleValue());
		sum.setSrchuv(sumData.getSrchuv());
		sum.setClkuv(sumData.getClkuv());
		sum.setSummaryText(this.getText("download.summary.group",
				new String[] { String.valueOf(sumData.getGroupCount()) }));// 添加“合计”

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
			// 如果没有aot信息则以groupid设置，如果没有groupid则以ids设置
			Integer queryId = qp.getGroupId() == null ? 0 : qp.getGroupId();
			if (queryId > 0) {
				qp.setIds(new ArrayList<Long>());
				qp.getIds().add((long) queryId);
			}
		}

	}

	protected void merge(List<GroupViewItem> vos, AotReportItem aotItems) {

		if (CollectionUtils.isEmpty(vos)) {
			return;
		}

		List<Long> ids = aotItems.getObjIdList();
		List<Float> suggestBids = aotItems.getGroupPriceList();
		for (GroupViewItem vo : vos) {
			for (int i = 0; i < ids.size(); i++) {
				Long id = ids.get(i);
				if (vo.getGroupId().longValue() == id.longValue()) {
					vo.setSuggestPrice(suggestBids.get(i) / 100.d);// 需要由元转变成分
					break;
				}
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
	private List<GroupViewItem> getViewItems(AotReportItem aotItems,
			boolean forceUnPagable) {

		List<GroupViewItem> infoData = new ArrayList<GroupViewItem>();

		if (aotItems == null || aotItems.getObjIdList() == null
				|| aotItems.getObjIdList().size() > 0) {

			// 如果查询条件中没有AOT，或者AOT返回的结果集>0则走DB查询

			List<Integer> filterIds = null;// 需要按IDS来滤过的。

			if (count == 0) {
				// 记录条数为0的话就不用进行后端查询了

			} else {

				boolean needPagable = false;// 分页查询标识
				if (count > ReportWebConstants.FRONT_SORT_THRESHOLD) {
					/**
					 * 在没有设置强制不分页（下载功能时使用）时，
					 * 记录条件大于1W就不用按照viewState（显示状态）和统计字段排序，也不用按统计字段过滤，后端来分页。
					 */
					needPagable = !forceUnPagable;
				} else {
					// 如果小于1W的话就取出所有的VO数据和统计数据，在前端进行分页处理
				}

				// 生成planId字段过滤列表，生成Doris排序方式值
				List<Integer> planIds = null;
				if (qp.getPlanId() != null && qp.getPlanId() > 0) {
					planIds = new ArrayList<Integer>();
					planIds.add(qp.getPlanId());
				}


                // 1、从DB获取数据
                infoData = reportCproGroupMgr.findCproGroupViewItem(qp, needPagable);

				if(org.apache.commons.collections.CollectionUtils.isEmpty(infoData)){
					return infoData;
				}
				
				// 2、获取对应从Mysql过滤后的ids
				filterIds = new ArrayList<Integer>();
				for (GroupViewItem item : infoData) {
					filterIds.add(item.getGroupId());
				}

				// 3、获取统计数据+Uv数据+Holmes数据+trans数据
				List<GroupViewItem> olapList = this.getStatAndTransData(needPagable, planIds,filterIds);
                
				// 4、合并mysql数据和doris数据
				if (!CollectionUtils.isEmpty(olapList)) {//数据不为空则需要合并									
					if (!ReportConstants.isStatField(qp.getOrderBy())
							&&
						!ReportConstants.isTransDataField(qp.getOrderBy())
							&&
						!ReportConstants.isUVField(qp.getOrderBy())
							&&
						!ReportConstants.isHolmesField(qp.getOrderBy())){
						
						//如果不以doris字段排序排序，则按照数据库字段为序进行合并
						fillMergeDataWithMasterOrder(infoData, olapList);
						
					} else {
						
						//否则按照doris字段为序合并，注意：此时statAndTransData为排好序的结果
						infoData = fillMergeDataWithSlaveOrder(infoData, olapList);
					}
				}

				// 5、按照doris字段进行过滤
				if (!needPagable) {
					// 如果不分页则才能按统计字段进行过虑，即当数据大太时就算前端传了按统计字段过滤也不去过滤。
					infoData = filterByStatField(qp, infoData);

					// 如果有被过滤则需要重新设置行数大小。
					count = infoData.size();
				}
			}
		} else {
			// 否则表示有按AOT进行过滤，但结果集为空，不需要再进行后续查询了。一般来说走不到该分支
			log.warn("query aot[" + qp.getAotItemId() + "] for filter ids, but return empty collection");
		}

		return infoData;
	}
	
	private List<GroupViewItem> getStatAndTransData(boolean needPagable,List<Integer> planIds, List<Integer> groupIds) {

		int orient = ReportConstants.SortOrder.ASC;
		if (QueryParameterConstant.SortOrder.SORTORDER_DESC.equalsIgnoreCase(qp.getOrder())) {
			orient = ReportConstants.SortOrder.DES;
		}
		
		//需要以统计字段排序？
		boolean isOrderByStatDataField = !needPagable && ReportConstants.isStatField(qp.getOrderBy());
		
		//需要以转化字段排序？
		boolean isOrderByTransDataField = !needPagable && ReportConstants.isTransDataField(qp.getOrderBy());
		
		//需要以转化字段排序？
		boolean isOrderByUvDataField = !needPagable && ReportConstants.isUVField(qp.getOrderBy());
		
		//需要以转化字段排序？
		boolean isOrderByHolmesDataField = !needPagable && ReportConstants.isHolmesField(qp.getOrderBy());
		
		//1、获取统计数据
		List<GroupViewItem> olapList = null;
		if (isOrderByStatDataField) {
			// 如果不需要分页且是按统计字段排序，则使用dorsi排序获取统计数据
			
			olapList = groupStatMgr.queryGroupData(userId, planIds, groupIds, from, to, qp.getOrderBy(), 
					orient, ReportConstants.TU_NONE);

		} else {
			/**
			 * 如果是需要分页或者排序字段不是统计字段，则用DB查询出来的VO的顺序进行数据合并
			 * 此处有个言下意：前端默认按srchs降排，如果发现数据量太大则放弃该规则
			 */

			// 获取ids对应的统计数据
			olapList = groupStatMgr.queryGroupData(userId, planIds, groupIds, from, to, null, 0, ReportConstants.TU_NONE);
		}
		
		
		//2、获取UV数据
		List<Map<String, Object>> uvData= null;
		if (isOrderByUvDataField) {

			// 如果不需要分页且是按UV字段排序，则用UV数据的排序顺序进行数据合并
			uvData = uvDataService.queryGroupData(userId, planIds, groupIds,
												  from, to, qp.getOrderBy(), orient, 
												  ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
			
			
		} else {

			/**
			 * 如果是需要分页或者排序字段不是统计字段，则用DB查询出来的VO的顺序进行数据合并
			 * 此处有个言下意：前端默认按srchs降排，如果发现数据量太大则放弃该规则
			 */

			//获取ids对应的统计数据			
			uvData = uvDataService.queryGroupData(userId, planIds, groupIds, 
												  from, to, null, 0, 
												  ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
		}
		
		//需要返回的数据
		List<Map<String, Object>> mergedData = null;
		
		//4、如果需要获取转化数据，则获取转化数据并合并
		boolean needToFetchTransData = this.transReportFacade.needToFetchTransData(userId,from,to,false);

		if (needToFetchTransData){
			
			TempSitesAndTrans tempSiteAndTrans = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userId);
			
			//5、获取转化数据
			List<Map<String, Object>> tranData = null;
			if (!isOrderByTransDataField) {
				tranData = transDataService.queryGroupData(userId, 
											tempSiteAndTrans.getTransSiteIds(), tempSiteAndTrans.getTransTargetIds(),
											planIds,groupIds, from, to, null, 0,
											ReportConstants.TU_NONE,Constant.REPORT_TYPE_DEFAULT,0,0);
			} else {
				tranData = transDataService.queryGroupData(userId, 
											tempSiteAndTrans.getTransSiteIds(), tempSiteAndTrans.getTransTargetIds(),
											planIds,groupIds, from, to, qp.getOrderBy(), 
											orient,ReportConstants.TU_NONE,Constant.REPORT_TYPE_DEFAULT,0,0);
			}
			
			//6、获取Holmes数据
			List<Map<String, Object>> holmesData = null;
			if (!isOrderByHolmesDataField) {
				holmesData = holmesDataService.queryGroupData(userId, null, null, planIds,groupIds, 
															from, to, null, 0,
															ReportConstants.TU_NONE,Constant.REPORT_TYPE_DEFAULT,0,0);
			} else {
				holmesData = holmesDataService.queryGroupData(userId, null, null, planIds,groupIds, 
															from, to, qp.getOrderBy(), 
															orient,ReportConstants.TU_NONE,Constant.REPORT_TYPE_DEFAULT,0,0);
			}
			
			//7、判断排序字段
			Integer orderByField = this.getOrderByXFiled(isOrderByStatDataField, isOrderByUvDataField, isOrderByHolmesDataField, isOrderByTransDataField);
			
			//8、Merge统计数据、UV数据、Holmes数据、转化数据
			mergedData = this.reportFacade.mergeTransHolmesAndUvData(orderByField,tranData, holmesData, uvData, ReportConstants.GROUP);			
		} else {
			
			//8、Merge统计数据和UV数据
			mergedData = uvData;

		}
		
		List<GroupViewItem> dorisList = new ArrayList<GroupViewItem>();
		List<GroupViewItem> resultList = new ArrayList<GroupViewItem>();
		//7、填充数据
		if (! CollectionUtils.isEmpty(mergedData)) {
			for (Map<String, Object> row : mergedData) {
				GroupViewItem item = new GroupViewItem();
				if (row != null) {
					item.setPlanId(Integer.valueOf(row.get(ReportConstants.PLAN).toString()));
					item.setGroupId(Integer.valueOf(row.get(ReportConstants.GROUP).toString()));
					item.fillStatRecord(row);
					dorisList.add(item);
				}
			}
		}
		
		boolean keepMainListOrder = ! isOrderByStatDataField;
		resultList = ReportUtils.mergeItemListKeepOrder(dorisList, olapList,  Constants.COLUMN.GROUPID, 
				Constants.statMergeVals, GroupViewItem.class, SortOrder.val(qp.getOrder()), true, keepMainListOrder);
		
		ReportUtil.generateExtentionFields(resultList);
		
		return resultList;
	}

	/**
	 * getStatItems: 获取天粒度的统计数据 需要经过以下五步： 1、如果需要查询AOT则查询之以获取需要过滤的ids
	 * 2、查询实体对象Ids列表 3、获取统计数据并merge第二步的数据 4、按照统计字段来过滤 5、将统计数据汇总到天并返回
	 * 
	 * @return
	 * @since
	 */
	private List<ReportDayViewItem> getStatItemsFilterByStatField(
			AotReportItem aotItems) {
		List<Integer> filterIds = null;// 需要按IDS来滤过的。
		List<Map<String, Object>> uvData = null;//UV数据
		
		//1、如果需要查询AOT则查询之以获取需要过滤的ids
		genQueryItemIds(aotItems);

		//2、查询实体对象
		if (qp.hasNoneStatField4Filter()) {
			filterIds = reportCproGroupMgr.findAllCproGroupIdsByQuery(qp, false);
			if(CollectionUtils.isEmpty(filterIds)) {
				return new ArrayList<ReportDayViewItem> ();
			}
		}

		//3、生成planId字段过滤列表，生成Doris排序方式值
		List<Integer> planIds = null;
		if (qp.getPlanId() != null && qp.getPlanId() > 0) {
			planIds = new ArrayList<Integer>();
			planIds.add(qp.getPlanId());
		}
		
		//4、获取统计数据
		List<GroupViewItem> olapList = groupStatMgr.queryGroupData(userId, planIds, filterIds,
				from, to, null, 0, ReportConstants.TU_NONE);
			
		//5、获取UV数据
		uvData = uvDataService.queryGroupData(userId, planIds, filterIds, 
												from, to, null, 0, 
												ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
		
		List<GroupViewItem> dorisList = new ArrayList<GroupViewItem>();
		if (! CollectionUtils.isEmpty(uvData)) {
			for (Map<String, Object> row : uvData) {
				GroupViewItem item = new GroupViewItem();
				if (row != null) {
					item.setPlanId(Integer.valueOf(row.get(ReportConstants.PLAN).toString()));
					item.setGroupId(Integer.valueOf(row.get(ReportConstants.GROUP).toString()));
					item.fillStatRecord(row);
					dorisList.add(item);
				}
			}
		}
		//6、合并数据
		List<GroupViewItem> resultList = ReportUtils.mergeItemList(dorisList, olapList, Constants.COLUMN.GROUPID, 
				Constants.statMergeVals, GroupViewItem.class, true);
		
		ReportUtil.generateExtentionFields(resultList);
		
		//7、按照统计字段来过滤
		resultList = filterByStatFieldForList(qp, resultList);// 按统计字段过滤

		//8、获取过滤后的groupids
		filterIds = new ArrayList<Integer>();
		for (GroupViewItem mergedItem : resultList) {
			filterIds.add(mergedItem.getGroupId());
		}
		
		//9、再次查询doris
		if (CollectionUtils.isEmpty(filterIds)) {
			return new ArrayList<ReportDayViewItem>();
		} else {
			//获取统计数
			List<ReportDayViewItem> olapDateList = byDayStatService.queryByDayData(userId, planIds,
					filterIds, flashFrom, flashTo, ReportConstants.TU_DAY);
			
			//获取UV数据
			uvData = uvDataService.queryGroupDataByTime(userId, planIds, filterIds,
					flashFrom, flashTo, null, 0, 
					ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);
			
			List<ReportDayViewItem> dorisDateList = new ArrayList<ReportDayViewItem>();
			dorisDateList = ReportUtil.transformStatDataByDay(uvData);
			
			List<ReportDayViewItem> resultDateList = ReportUtils.mergeItemList(dorisDateList, olapDateList, OlapConstants.COLUMN.SHOWDATE, 
					Constants.statMergeVals, ReportDayViewItem.class, true);
			
			ReportUtil.generateExtentionFields(resultList);
			
			//10、获取最终分日统计数据
			return resultDateList;
		}
	}

	/**
	 * getStatItemsFilterByNonStatField: 根据ids查询user维度的统计信息，前提是：过滤字段中不含统计字段
	 * 
	 * @return
	 * @since
	 */
	private List<ReportDayViewItem> getStatItemsFilterByNonStatField(
			AotReportItem aotItems) {

		List<Integer> filterIds = null;// 需要按IDS来滤过的。

		// 1、如果需要查询AOT则查询之以获取需要过滤的ids
		genQueryItemIds(aotItems);

		// 2、查询ids
		if (qp.hasNoneStatField4Filter()) {
			filterIds = reportCproGroupMgr.findAllCproGroupIdsByQuery(qp, false);
			
			if(CollectionUtils.isEmpty(filterIds)) {
				return new ArrayList<ReportDayViewItem>();
			}
		}

		//3、生成planId字段过滤列表，生成Doris排序方式值
		List<Integer> planIds = null;
		if (qp.getPlanId() != null && qp.getPlanId() > 0) {
			planIds = new ArrayList<Integer>();
			planIds.add(qp.getPlanId());
		}

		//4、根据ids获取统计数据
		List<ReportDayViewItem> olapDateList = byDayStatService.queryByDayData(userId, planIds,
				filterIds, flashFrom, flashTo, ReportConstants.TU_DAY);
		
		//5、根据ids获取UV数据
		List<Map<String, Object>> uvData = uvDataService.queryGroupDataByTime(
				userId, planIds, filterIds, 
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
	public GroupSumData calculateSumData(List<GroupViewItem> infoData) {
		GroupSumData sumData = new GroupSumData();
		
		//1、累加各个数据
		for (GroupViewItem item : infoData) {
			sumData.setClks(sumData.getClks() + item.getClks());
			sumData.setCost(sumData.getCost() + item.getCost());
			sumData.setSrchs(sumData.getSrchs() + item.getSrchs());
			sumData.setSrchuv(sumData.getSrchuv() + item.getSrchuv());
			sumData.setClkuv(sumData.getClkuv() + item.getClkuv());
		}
		
		//2、生成扩展数据
		sumData.generateExtentionFields();

		//3、填充推广组数
		sumData.setGroupCount(infoData.size());

		return sumData;
	}


	public ReportCproGroupMgr getReportCproGroupMgr() {
		return reportCproGroupMgr;
	}

	public void setReportCproGroupMgr(ReportCproGroupMgr reportCproGroupMgr) {
		this.reportCproGroupMgr = reportCproGroupMgr;
	}

	public CproPlanMgr getCproPlanMgr() {
		return cproPlanMgr;
	}

	public void setCproPlanMgr(CproPlanMgr cproPlanMgr) {
		this.cproPlanMgr = cproPlanMgr;
	}

	public void setCproGroupMgr(CproGroupMgr cproGroupMgr) {
		this.cproGroupMgr = cproGroupMgr;
	}
	
	public ReportCacheService getCacheService() {
		return cacheService;
	}

	public void setCacheService(ReportCacheService cacheService) {
		this.cacheService = cacheService;
	}
}
