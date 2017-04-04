package com.baidu.beidou.report.web.action;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.CollectionUtils;

import com.baidu.beidou.account.constant.AccountConfig;
import com.baidu.beidou.account.service.MfcService;
import com.baidu.beidou.cproplan.constant.CproPlanConstant;
import com.baidu.beidou.cproplan.service.CproPlanMgr;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.olap.constant.Constants;
import com.baidu.unbiz.olap.constant.OlapConstants;
import com.baidu.unbiz.olap.constant.SortOrder;
import com.baidu.beidou.olap.service.AccountStatService;
import com.baidu.beidou.olap.service.PlanStatService;
import com.baidu.beidou.olap.service.ReportByDayStatService;
import com.baidu.unbiz.olap.util.ReportUtils;
import com.baidu.beidou.olap.vo.PlanViewItem;
import com.baidu.beidou.olap.vo.ReportDayViewItem;
import com.baidu.beidou.olap.vo.UserAccountViewItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.report.service.ReportCacheService;
import com.baidu.beidou.report.service.ReportCproPlanMgr;
import com.baidu.beidou.report.util.ReportUtil;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.plan.PlanOffTimeVo;
import com.baidu.beidou.report.vo.plan.PlanReportSumData;
import com.baidu.beidou.report.vo.plan.PlanReportVo;
import com.baidu.beidou.report.vo.plan.PlanSumData;
import com.baidu.beidou.report.vo.plan.UserAccountReportVo;
import com.baidu.beidou.stat.driver.bo.Constant;
import com.baidu.beidou.stat.driver.bo.Constant.DorisDataType;
import com.baidu.beidou.stat.util.DateUtil;
import com.baidu.beidou.tool.util.TransReportHelper;
import com.baidu.beidou.tool.vo.TempSitesAndTrans;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.user.constant.UserConstant;
import com.baidu.beidou.user.service.UserInfoMgr;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.DateUtils;
import com.baidu.beidou.util.LogUtils;
import com.baidu.beidou.util.StringUtils;

public class ListCproPlanAction extends BeidouReportActionSupport  {
	
	private static final long serialVersionUID = 1116584041553970033L;
	private static Log log = LogFactory.getLog(ListCproPlanAction.class);

	private ReportCproPlanMgr reportCproPlanMgr = null;	
	
	private CproPlanMgr cproPlanMgr;
	
	@Resource(name="accountStatServiceImpl")
	private AccountStatService accountStatMgr;
	
	@Resource(name="planStatServiceImpl")
	PlanStatService planStatMgr;
	
	@Resource(name="reportByDayStatServiceImpl")
	private ReportByDayStatService byDayStatService;
	
	/** 下线时间显示格式 */
	public static final String OFFTIME_VIEW_FORMAT = "HH:mm";
	
	//－－－－－－－－－－－－－以下内容为下载使用-----------------
	/** 待下载的文件内容 */
	private String content;
	/**
	 * 返回给用户下载的excel流，
	 */
	private InputStream inputStream = new ByteArrayInputStream("ERROR".getBytes());
	/**
	 * 返回给用户下载的excel文件大小
	 */
	private long fileSize;
	private String fileName = "test.xls";
	
	private ReportCacheService cacheService;
	
	private MfcService mfcService = null;
	
	private UserInfoMgr userInfoMgr;
	
	//---------------------action方法---------------------
	
	public String execute() {
		
		return SUCCESS;
	}
	
	@Override
	protected void initStateMapping() {
		if (qp!= null) {
			qp.setStateMapping(ReportWebConstants.PLAN_FRONT_BACKEND_STATE_MAPPING.get(qp.getViewState()));
		}
	}
	

	/**
	 * ajaxFlashXml :生成XML文档，格式如下：
     *	<?xml version='1.0' encoding='utf-8'?>
     *	<data overview='最近七天'  showIndex='0' tag1='/展现次数/' tag2='/点击次数/' tag3='/点击率/%' tag4='/总费用/' selected='1'>
     *	<record date='2010-06-25' data1='0.00' data2='0.00' data3='0.00%' data4='132323'/>
     *	<record date='2010-06-26' data1='0.00' data2='0.00' data3='0.03%' data4='132322'/>
     *	</data>
     *
     * 当前生成的xml放在json内部，如果出异常则还是return SUCCESS，异常标识在data里面的state=1
	 */
	public String ajaxFlashXml() {
		
		try{
			super.initParameterForAtLestServenDay();
		} catch (Exception e){
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_SUCCESS);
			jsonObject.addData("state", ReportWebConstants.FLASH_DATA_STATE.UNNORMAL);
			jsonObject.addData("xml", "");
			return SUCCESS;
		}
		
		/** 
		 * 如果统计时间包含今天，则返回空
		 */
//		if(super.isMixTodayAndBefore(from, to) || super.isOnlyToday(from, to)){
//			log.warn("推广计划不提供今日的统计数据");
//			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_SUCCESS);
//			jsonObject.addData("state", ReportWebConstants.FLASH_DATA_STATE.UNNORMAL);
//			jsonObject.addData("xml", "");
//			return SUCCESS;
//		}
		
		List<ReportDayViewItem> statList; //天粒度的汇总数据,key为时间，value为统计值
		
		if (qp.hasStatField4Filter() ) {//如果需要按统计字段过滤则要查询出day+planId维度的数据

			/** 
			 * 说明一下此处的逻辑：
			 * 1、按查询时间(from,to)段查询出分Plan的汇总统计值；
			 * 2、过滤#1的结果得到可以被用于汇总的planIds；
			 * 3、根据#2中的IDS获取分日的统计数据
			 */
			statList = getStatItemsFilterByStatField();//时间粒度：天
			
		} else {
			//否则只用查出user维度的，但需要传入过滤后的ids
			statList = getStatItemsFilterByNonStatField();
		}
		
		String xml = generateXml(statList);
		
		jsonObject.addData("state", ReportWebConstants.FLASH_DATA_STATE.NORMAL);
		jsonObject.addData("data", xml);

		return SUCCESS;
	}

	/**
     * 生成账户级别的用户分日图表
	 */
	public String ajaxFlashXmlForUser() {
		
		//1、初始化参数
		try{
			super.initParameterForAtLestServenDay();
		} catch (Exception e){
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_SUCCESS);
			jsonObject.addData("state", ReportWebConstants.FLASH_DATA_STATE.UNNORMAL);
			jsonObject.addData("xml", "");
			return SUCCESS;
		}
		
		/** 
		 * 如果统计时间包含今天，则返回空
		 */
//		if(super.isMixTodayAndBefore(from, to) || super.isOnlyToday(from, to)){
//			log.warn("推广首页不提供今日的统计数据");
//			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_SUCCESS);
//			jsonObject.addData("state", ReportWebConstants.FLASH_DATA_STATE.UNNORMAL);
//			jsonObject.addData("xml", "");
//			return SUCCESS;
//		}
		
		//2、获取分日图表用户数据
		List<UserAccountViewItem> statData = getStatItemsForUser();
		
		//3、生成xml字符串
		String xml = generateXml(statData);
		
		//4、构造json数据
		jsonObject.addData("state", ReportWebConstants.FLASH_DATA_STATE.NORMAL);
		jsonObject.addData("data", xml);

		return SUCCESS;
	}
	
	/** 
	 * 计划列表使用 
	 * 
	 * 当前在出异常时直接返回status=1，系统提示“系统内部异常”，后续需要给出具体提示
	 **/
	public String ajaxList() {
		try{
			//初始化一下参数
			super.initParameter();
		} catch (Exception e){
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED_SERVER);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);
			
			return SUCCESS;
		}

		/** 
		 * 如果统计时间混合今天和历史时间，则返回空
		 */
//		if(super.isMixTodayAndBefore(from, to)){
//			log.warn("系统无法提供今日和历史时间混合的报告数据");
//			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED_SERVER);
//			jsonObject.addMsg(ReportWebConstants.ERR_DATE);
//			
//			return SUCCESS;
//		}
		
		List<PlanViewItem> infoData = null;//目标plan集
		
		//1、如果需要查询AOT则查询之以获取需要过滤的ids
		//2、查询实体对象
		//3、获取统计数据并merge第二步的数据
		//4、按照统计字段来过滤
		infoData = getViewItems();//无统计时间粒度
			
		//5、生成汇总的统计数据
		PlanSumData sumData = calculateSumData(infoData);
		
		//6、获取分页数据：如果需要分页的话
		infoData = pagerList(infoData);//暂时先注释掉，当前plan层级不进行分页处理
		
		//7、获取额外信息：下线时间，“正在投放”
		
		//7.1 下线时间查询
		List<Integer> planIdsForOffTime = getBudgetOverPlanIds(infoData);
		if (!CollectionUtils.isEmpty(planIdsForOffTime)) {
			// 为每一个已经下线的计划计算“可重新上线的最低预算” modified by kanghongwei since cpweb429(qtIM)
			calculatePlanBudget4Online(infoData);

			List<PlanOffTimeVo> offtime = reportCproPlanMgr.findCproPlanOfftime(planIdsForOffTime);
			mergeOfftimeInfo(infoData, offtime, userId);
		}
		//7.2 正在投放查询
		// 通过财务中心API获取用户的余额  modify by hanxu03 since 一站式 2.0.83
		User user = userMgr.findUserBySFid(userId);
		int userBalanceStat = UserConstant.BALANCESTAT_OFFLINE;
		double[][] result = mfcService.getUserProductBalance(
				Arrays.asList(new Integer[]{userId}), 
                Arrays.asList(new Integer[]{AccountConfig.MFC_BEIDOU_PRODUCTID}), 
                AccountConfig.MFC_OPUID_DEFAULT);
		
		if (result == null || result.length < 1 || result[0] == null || result[0].length < 1 || result[0][0] <= 0) {
		}
		else{
			userBalanceStat = UserConstant.BALANCESTAT_ONLINE;
		}
		
		if (user != null 
				&& UserConstant.BALANCESTAT_ONLINE == userBalanceStat
				&& UserConstant.USER_STATE_NORMAL == user.getUstate()
				&& (UserConstant.SHIFEN_STATE_NORMAL == user.getUshifenstatid() || UserConstant.SHIFEN_STATE_ZERO == user.getUshifenstatid())) {

			List<Integer> planIdsForInSchedual = getInSchedualPlanIds(infoData);
			planIdsForInSchedual = reportCproPlanMgr.findInSchedualPlanIds(user.getUserid(), planIdsForInSchedual);
			mergeInSchedualInfo(infoData, planIdsForInSchedual);
			
			//add by dongying since cpweb611-1
			List<Integer> planIdsForPauseInSchedual = getInSchedualPlanIds(infoData);
			planIdsForPauseInSchedual = reportCproPlanMgr.findPauseInSchedualPlanIds(user.getUserid(), planIdsForPauseInSchedual);
			mergePauseInSchedualInfo(infoData, planIdsForPauseInSchedual);
		}
		
		//8、生成分页汇总数据
		int totalPage = super.getTotalPage(infoData.size());
		
		/**
		 * 如果统计时间是今天，将统计数据列中点击、消费外的其他列置为-1
		 */
//		if(super.isOnlyToday(from, to)){
//			super.clearNotRealtimeStat(infoData, sumData);
//		}
		
		// 9、增加SOP入口条件2判断    added by hanxu03 in cpweb-630
		Long planCnt = cproPlanMgr.countCproPlanByUserId(userId);
		
		jsonObject.addData("canSop", (planCnt == null || planCnt <=0) ? true : false);
		jsonObject.addData("list", infoData);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("cache", sumData.getNeedFrontSort());
		jsonObject.addData("sum", sumData);
		
		// 处理当日和昨日不入库的uv和转化数据
		this.reportFacade.postHandleTransAndUvData(userId, from, to, infoData, sumData);
		
		return SUCCESS;
	}

	private void mergePauseInSchedualInfo(List<PlanViewItem> infoData,
			List<Integer> planIdsForPauseInSchedual) {
		for (Integer id : planIdsForPauseInSchedual) {
			for (PlanViewItem item : infoData) {
				if (item.getPlanId().equals(id) ) {
					item.setInSchedule(ReportConstants.Boolean.FALSE);
				}
			}
		}
	}

	private void calculatePlanBudget4Online(List<PlanViewItem> planViewItemList) {
		if (org.apache.commons.collections.CollectionUtils.isEmpty(planViewItemList)) {
			return;
		}
		for (PlanViewItem item : planViewItemList) {
			Integer onlineBudget = cproPlanMgr.calculatePlanBudget4Online(item.getBudgetOver(),
					userId, item.getPlanId());
			item.setOnlineBudget(onlineBudget);
		}
	}

	
	/**
	 * 该方法提供推广计划列表下载功能，出异常时由相应的下载intercepor处理
	 * @return
	 * @throws IOException
	 */
	public String download() throws IOException {

		//1、初始化一下参数
		initParameter();
		
		//2、查询账户信息
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}
		
		/** 
		 * 如果统计时间混合今天和历史时间，则返回空
		 */
//		if(super.isMixTodayAndBefore(from, to)){
//			throw new BusinessException("系统无法提供今日和历史时间混合的报告数据");
//		}
		
		//3、构造报告VO，下载的CSV共有四部分：1、账户基本信息，2、列头，3、列表，4、汇总信息	
		PlanReportVo vo = new PlanReportVo(qp.getLevel());//报表下载使用的VO
		
		//需要根据userid是否为白名单控制计划状态列是否显示
		vo.setUserId(userId);
		
		List<PlanViewItem> infoData = null;//目标plan集
		
		//3.1、获取统计数据
		infoData = getViewItems();//无统计时间粒度

		//3.2、生成汇总的统计数据
		PlanSumData sumData = calculateSumData(infoData);
		
		//3.3处理当日和昨日不入库的uv和转化数据 add by wangchongjie since 2012.11.03
		this.reportFacade.postHandleTransAndUvData(userId, from, to, infoData, sumData);

		//3.4、判断是否需要转化数据
		boolean showTransData = this.transReportFacade.isTransToolSigned(userId, false);
		boolean transDataValid = this.transReportFacade.needToFetchTransData(userId, from, to, false);
		
		/**
		 * 如果统计时间是今天，将统计数据列中点击、消费外的其他列置为-1
		 */
//		if(super.isOnlyToday(from, to)){
//			super.clearNotRealtimeStat(infoData, sumData);
//		}
		
		//3.5、填充数据
		vo.setShowTransData(showTransData);
		vo.setTransDataValid(transDataValid);
		vo.setAccountInfo(generateAccountInfo(user.getUsername()));
		vo.setDetails(infoData);
		vo.setHeaders(generateReportHeader(showTransData));
		vo.setSummary(generateReportSummary(sumData));
		
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(vo, output);
		
		//4、设置下载需要使用到的一些属性
		byte[] bytes = output.toByteArray();
		inputStream = new ByteArrayInputStream(bytes);
		fileSize = bytes.length;
		
		//5、生成文件名可能会用到的元素：
		//[账户名]-[推广计划名称]-[推广组名称]-[是否创意]-[是否分网站]-[时间分布].csv
		fileName = user.getUsername() + "-" + this.getText("download.plan.filename.prefix") ;
		try {
			//主文件名（时间之前部分）不能超过80个字符，中文算两个字符
			fileName = StringUtils.subGBKString(fileName, 0, ReportWebConstants.FILE_MAIN_NAME_MAXLENGTH);
		} catch (UnsupportedEncodingException e1) {
			LogUtils.error(log, e1);
		}
		fileName += "-" + sd.format(from) + "-" + sd.format(to) + ".csv" ;
		
		try {
			//中文文件名需要用ISO8859-1编码
			fileName = new String(fileName.getBytes(fileEncoding), "ISO8859-1");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage());
		}
		return SUCCESS;
	}
	

	/**
	 * generateAccountInfo: 生成报表用的账户信息
	 *
	 * @return      
	*/
	protected ReportAccountInfo generateAccountInfo(String userName) {
		ReportAccountInfo accountInfo = new ReportAccountInfo();
		accountInfo.setReport(this.getText("download.account.report.plan"));
		accountInfo.setReportText(this.getText("download.account.report"));
		
		accountInfo.setAccount(userName);
		accountInfo.setAccountText(this.getText("download.account.account"));
		
		accountInfo.setDateRange(sd1.format(from) + " - " + sd1.format(to));
		accountInfo.setDateRangeText(this.getText("download.account.daterange"));	
		
		accountInfo.setLevel(this.getText("download.account.level.allplan"));	
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
			headers = this.getText("download.bmob.plan.head.col.has.trans").split(",");
		} else {
			headers = this.getText("download.bmob.plan.head.col").split(",");
		}
		return headers;
	}
	
//	protected String[] generateReportDayHeader(boolean showTransData) {
//		String[] headers = null;
//		if (showTransData) {
//			headers = this.getText("download.plan.day.head.col.has.trans").split(",");
//		} else {
//			headers = this.getText("download.plan.day.head.col").split(",");
//		}
//		return headers;
//	}
	

//	/**
//	 * generateReportDetail: 生成报表用的列表体
//	 *      
//	 * @since 
//	*/
//	protected void generateReportDetail() {
//		//暂时不用
//	}

	/**
	 * generateReportSummary: 生成推广计划列表汇总信息
	 *
	 * @param sumData 汇总数据
	 * @return 用于表示报表的汇总信息      
	*/
	protected PlanReportSumData generateReportSummary(PlanSumData sumData) {

		PlanReportSumData sum = new PlanReportSumData(); 
		
		sum.setClks(sumData.getClks());
		sum.setSrchs(sumData.getSrchs());
		sum.setCost(sumData.getCost());
		sum.setAcp(sumData.getAcp().doubleValue());
		sum.setCtr(sumData.getCtr().doubleValue());
		sum.setCpm(sumData.getCpm().doubleValue());
		sum.setSrchuv(sumData.getSrchuv());
		sum.setClkuv(sumData.getClkuv());
		sum.setSumBudget(sumData.getSumBudget());
		sum.setSummaryText(this.getText("download.summary.plan", 
							new String[]{String.valueOf(sumData.getPlanCount()),
								String.valueOf(sumData.getValidPlanCount())}));//添加“合计”
		
		return sum;
	}
	
	//---------------------内部方法---------------------
	/**
	 * <p>重写获取待查询IDS的方法，对于传入的qp.planId和qp.ids也加入到考虑之列
	 * <p>获取ids的的条件为为以下顺序三选一：1、qp.planId； 2、qp.ids； 3、super.genQueryItemIds(即从AOT获取)
	 */
	public List<Integer> genQueryItemIds() {

		List<Integer> filterIds = null;//需要按IDS来滤过的。
		if (org.apache.commons.collections.CollectionUtils.isEmpty(qp.getIds())
				&& (qp.getPlanId() == null || qp.getPlanId() <= 0 )) {
			filterIds = super.genQueryItemIds();
			qp.setIds(ReportUtil.transferFromIntegerToLong(filterIds));
		} else {
			if (qp.getPlanId() != null && qp.getPlanId() > 0 ) {
				qp.setIds(new ArrayList<Long>());
				qp.getIds().add((long)qp.getPlanId());
			}
			filterIds = ReportUtil.transferFromLongToInteger(qp.getIds());
		}
		return filterIds;
	}
	
	/**
	 * <p>getViewItems: 生成前端VO，主要经历以下四步</p>
	 * 1、如果需要查询AOT则查询之以获取需要过滤的ids
	 * 2、查询实体对象
	 * 3、获取统计数据并merge第二步的数据
	 * 4、按照统计字段来过滤
	 * 
	 * @return  满足前端查询条件的对象    
	*/
	private List<PlanViewItem> getViewItems() {
		List<PlanViewItem> infoData = null;
		List<Integer> filterIds = null;//需要按IDS来滤过的。
		
		//1、如果需要查询AOT则查询之以获取需要过滤的ids
		filterIds = genQueryItemIds();

		//2、查询实体对象(不分页)
		infoData = reportCproPlanMgr.findPlanViewItemWithoutPagable(qp);
		
		//3、获取统计数据并merge第二步的数据
		if (!CollectionUtils.isEmpty(infoData)) {
			
			//如果过滤条件中有非统计字段且ids个数小于等于10000就按ids进行过滤
			if (qp.hasNoneStatField4Filter() && infoData.size() <= ReportWebConstants.IDS_SIZE_THRESHOLD) {
				
				//如果有按非统计字段进行过滤则需要查询出ids
				filterIds = new ArrayList<Integer>();
				for (PlanViewItem item : infoData) {
					filterIds.add(item.getPlanId());
				}
			}
			
			//获取统计数据+UV数据+Holmes数据+转化数据
//			List<Map<String, Object>> statUVHolmesTransData = null;
//			if(super.isOnlyToday(from, to)){
//				// 如果是获取今天数据，则从数据库中获取
//				
//				String orderValue = QueryParameterConstant.SortOrder.SORTORDER_ASC;
//				if (QueryParameterConstant.SortOrder.SORTORDER_DESC.equalsIgnoreCase(qp.getOrder())){
//					orderValue = QueryParameterConstant.SortOrder.SORTORDER_DESC;
//				}
//				
//				//以实时统计字段排序?
//				boolean isOrderByRealtimeStatDataField = ReportConstants.isRealtimeStatField(qp.getOrderBy());
//				
//				statUVHolmesTransData = realtimeStatService.queryPlanData(userId, filterIds, 
//						isOrderByRealtimeStatDataField ? qp.getOrderBy() : null, orderValue, 0, 0);
//			}
//			else{
				// 如果是获取历史数据，则从doris中获取
				long start = System.currentTimeMillis();
//				statUVHolmesTransData = this.getStatAndTransData(filterIds);
//				statUVHolmesTransData = this.getPlanDorisData(filterIds); //有缓存的实现
				
				List<PlanViewItem> statList = this.getStatAndTransData(filterIds);
				
				long now = System.currentTimeMillis();
            	log.debug("query unit doris data cost time: " + (now - start));
//			}
			
			if (!CollectionUtils.isEmpty(statList)) {//数据不为空则需要合并
				
				if (!ReportConstants.isStatField(qp.getOrderBy())
						&&
					!ReportConstants.isTransDataField(qp.getOrderBy())
						&&
					!ReportConstants.isUVField(qp.getOrderBy())
						&&
					!ReportConstants.isHolmesField(qp.getOrderBy())){
					//如果不按照doris数据排序，则按照数据库字段为序进行合并
					fillMergeDataWithMasterOrder(infoData, statList);
					
				} else {	
					//否则按照doris字段为序合并，注意：此时statAndTransData为排好序的结果	
					infoData = fillMergeDataWithSlaveOrder(infoData, statList);
				}
			}
			

		}
		
		//4、按照统计字段来过滤
		infoData = filterByStatField(qp, infoData);//按统计字段过滤
		
		return infoData;
	}
	
	
	private List<PlanViewItem> getStatAndTransData(List<Integer> planIds){
		
		//如果按统计字段排序则需要以统计字段的相对顺序进行结果的排序。
		int orient = ReportConstants.SortOrder.ASC;
		if (QueryParameterConstant.SortOrder.SORTORDER_DESC.equalsIgnoreCase(qp.getOrder())){
			orient = ReportConstants.SortOrder.DES;
		}
		
		//以统计字段排序？
		boolean isOrderByStatDataField = ReportConstants.isStatField(qp.getOrderBy());
		
		//以转化字段排序？
		boolean isOrderByTransDataField = ReportConstants.isTransDataField(qp.getOrderBy());
		
		//以UV字段排序？
		boolean isOrderByUVDataField = ReportConstants.isUVField(qp.getOrderBy());
		
		//以Holmes字段排序？
		boolean isOrderByHolmesDataField = ReportConstants.isHolmesField(qp.getOrderBy());
		
		//获取统计数据
		List<PlanViewItem> olapList = planStatMgr.queryPlanData(userId, planIds, from, to,
				qp.getOrderBy(), orient, ReportConstants.TU_NONE);

		//获取UV数据
		List<Map<String, Object>> uvData = null;
		if (!isOrderByUVDataField) {
			uvData = uvDataService.queryPlanData(userId, planIds, 
												from, to, null, 0, 
												ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
		} else {//使用doris排序，获取排好序的UV数据
			uvData = uvDataService.queryPlanData(userId, planIds, 
												from, to, qp.getOrderBy(), orient, 
												ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
		}
		
		//如果需要获取转化数据，则获取转化数据并合并
		boolean needToFetchTransData = this.transReportFacade.needToFetchTransData(userId,from,to,false);
		
		//需要返回的数据
		List<Map<String, Object>> mergedData = null;
		
		if (needToFetchTransData){
			
			TempSitesAndTrans tempSiteAndTrans = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userId);
			
			//获取转化数据
			List<Map<String, Object>> tranData = null;
			if (!isOrderByTransDataField) {
				tranData = transDataService.queryPlanData(userId, 
												tempSiteAndTrans.getTransSiteIds(), tempSiteAndTrans.getTransTargetIds(), 
												planIds, from, to, null, 0, 
												ReportConstants.TU_NONE,Constant.REPORT_TYPE_DEFAULT, 0, 0);
			} else {
				//使用doris排序，获取排好序的转化数据
				tranData = transDataService.queryPlanData(userId, 
												tempSiteAndTrans.getTransSiteIds(), tempSiteAndTrans.getTransTargetIds(), 
												planIds, from, to, qp.getOrderBy(), orient,
												ReportConstants.TU_NONE,Constant.REPORT_TYPE_DEFAULT, 0, 0);
			}
			
			//获取Holmes数据
			List<Map<String, Object>> holmesData = null;
			if (!isOrderByHolmesDataField) {
				holmesData = holmesDataService.queryPlanData(userId, null, null, planIds, 
															from, to, null, 0, 
															ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
			} else {
				//使用doris排序，获取排好序的Holmes数据
				holmesData = holmesDataService.queryPlanData(userId, null, null, planIds, 
															from, to, qp.getOrderBy(), orient, 
															ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
			}
			
			//判断按照doris哪个字段排序
			Integer orderByField = this.getOrderByXFiled(isOrderByStatDataField, isOrderByUVDataField,
					isOrderByHolmesDataField, isOrderByTransDataField);
			Integer order = orderByField.equals(DorisDataType.STAT) ? null : orderByField;
			
			//Merge统计数据、UV数据、Holmes数据、转化数据	
			mergedData = this.reportFacade.mergeTransHolmesAndUvData(order, tranData, holmesData, uvData, ReportConstants.PLAN);		
		
		} else {
			//Merge统计数据和UV数据
			mergedData = uvData;
		}
		
		List<PlanViewItem> dorisList = new ArrayList<PlanViewItem>();
		List<PlanViewItem> resultList = new ArrayList<PlanViewItem>();
		//7、填充数据
		if (! CollectionUtils.isEmpty(mergedData)) {
			for (Map<String, Object> row : mergedData) {
				PlanViewItem planItem = new PlanViewItem();
				if (row != null) {
					planItem.setPlanId(Integer.valueOf(row.get(ReportConstants.PLAN).toString()));
					planItem.fillStatRecord(row);
					dorisList.add(planItem);
				}
			}
		}
		
		boolean keepMainListOrder = ! isOrderByStatDataField;
		resultList = ReportUtils.mergeItemListKeepOrder(dorisList, olapList,  Constants.COLUMN.PLANID, 
				Constants.statMergeVals, PlanViewItem.class, SortOrder.val(qp.getOrder()), true, keepMainListOrder);
		
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
	 * 5、按过滤出来的IDS查询统计按天的账户层级的统计数据并返回
	 * @return      
	 * @since 
	*/
	protected List<ReportDayViewItem> getStatItemsFilterByStatField() {
		List<PlanViewItem> infoData = null;//过滤后的数据
		List<Integer> filterIds = null;//需要按IDS来滤过的。
		List<Map<String, Object>> uvData = null;//获取的Uv数据
		
		//1、如果需要查询AOT则查询之以获取需要过滤的ids
		filterIds = genQueryItemIds();

		//2、查询实体对象
		infoData = reportCproPlanMgr.findPlanViewItemWithoutPagable(qp);
		
		//3、获取经过过滤后的filerids
		if (CollectionUtils.isEmpty(infoData)) {
			return new ArrayList<ReportDayViewItem>();
		}

		if (qp.hasNoneStatField4Filter()) {
			filterIds = new ArrayList<Integer>();
			for (PlanViewItem item : infoData) {
				filterIds.add(item.getPlanId());
			}
		}
		
		//4、获取统计数据
		List<PlanViewItem> olapList = planStatMgr.queryPlanData(userId, filterIds, from, to, null, 0, ReportConstants.TU_NONE);
		
		//5、获取Uv数据
		uvData = uvDataService.queryPlanData(userId, filterIds, from, to, null, 0, 
												ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
		List<PlanViewItem> dorisList = new ArrayList<PlanViewItem>();
		if (! CollectionUtils.isEmpty(uvData)) {
			for (Map<String, Object> row : uvData) {
				PlanViewItem planItem = new PlanViewItem();
				if (row != null) {
					planItem.setPlanId(Integer.valueOf(row.get(ReportConstants.PLAN).toString()));
					planItem.fillStatRecord(row);
					dorisList.add(planItem);
				}
			}
		}
		
		//6、合并统计数据和UV数据以便后续过滤使用
		List<PlanViewItem> resultList = ReportUtils.mergeItemList(dorisList, olapList, Constants.COLUMN.PLANID, 
				Constants.statMergeVals, PlanViewItem.class, true);
		
		ReportUtil.generateExtentionFields(resultList);
		
		//7、按照统计字段来过滤
		resultList = filterByStatFieldForList(qp, resultList);//按统计字段过滤
		
		//8、将经过统计字段和Uv字段过滤之后的ids重新查询id+day维度的统计值。
		filterIds = new ArrayList<Integer>();
		if (!CollectionUtils.isEmpty(resultList)) {
			for(PlanViewItem mergedItem : resultList) {
				filterIds.add(mergedItem.getPlanId());
			}
		}
		if (org.apache.commons.collections.CollectionUtils.isEmpty(filterIds)) {
			return new ArrayList<ReportDayViewItem>();
		} else {
			//9、再次获取统计数据和Uv数据并和并
			List<ReportDayViewItem> olapDateList = byDayStatService.queryByDayData(userId,
					filterIds, flashFrom, flashTo, ReportConstants.TU_DAY);
			
			uvData = uvDataService.queryPlanDataByTime(userId, filterIds, flashFrom, flashTo, 
													null, 0, ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);
			
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
	 * getStatItemsForUser:
	 * 查询用户层级分日图表统计数据
	 * @return      
	 * @since 
	*/
	@SuppressWarnings("unchecked")
	protected List<UserAccountViewItem> getStatItemsForUser() {
		
		List<PlanViewItem> infoData = null;//实体对象集合
		List<Integer> filterIds = null;//需要按IDS来滤
		List<Map<String, Object>> uvData = null;//Uv数据
		
		//1、如果需要查询AOT则查询之以获取需要过滤的ids
		filterIds = genQueryItemIds();

		//2、查询实体对象
		infoData = reportCproPlanMgr.findPlanViewItemWithoutPagable(qp);
		if (CollectionUtils.isEmpty(infoData)) {
			return Collections.EMPTY_LIST;
		}
		if (qp.hasNoneStatField4Filter()) {
			filterIds = new ArrayList<Integer>();
			for (PlanViewItem item : infoData) {
				filterIds.add(item.getPlanId());
			}
		}

		long start = System.currentTimeMillis();
		List<UserAccountViewItem> olapList = accountStatMgr.queryAUserData(userId, filterIds, flashFrom, flashTo, ReportConstants.TU_DAY);

		//2、获取Uv数据
		uvData = uvDataService.queryUserDataByTime(userId, flashFrom, flashTo,
						null, 0, ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);
		List<UserAccountViewItem> dorisList = new ArrayList<UserAccountViewItem>();
		if(uvData != null){
			for(Map<String, Object> record : uvData){
				UserAccountViewItem item = new UserAccountViewItem();
				item.fillStatRecord(record);
				dorisList.add(item);
			}
		}
		
		List<UserAccountViewItem> resultList = ReportUtils.mergeItemList(dorisList, olapList, OlapConstants.COLUMN.SHOWDATE, 
				Constants.statMergeVals, UserAccountViewItem.class, true);
		
		ReportUtil.generateExtentionFields(resultList);
		
		long now = System.currentTimeMillis();
		log.debug("query unit doris data cost time: " + (now - start));
		
		//6、返回
		return resultList;
	}
	
	/**
	 * getStatItemsFilterByNonStatField:
	 * 根据ids查询user维度的统计信息，前提是：过滤字段中不含统计字段
	 * @return      
	 * @since 
	*/
	protected List<ReportDayViewItem> getStatItemsFilterByNonStatField() {
		
		List<PlanViewItem> infoData = null;//实体对象集合
		List<Integer> filterIds = null;//需要按IDS来滤
		List<Map<String, Object>> uvData = null;//Uv数据
		
		//1、如果需要查询AOT则查询之以获取需要过滤的ids
		filterIds = genQueryItemIds();

		//2、查询实体对象
		infoData = reportCproPlanMgr.findPlanViewItemWithoutPagable(qp);
		if (CollectionUtils.isEmpty(infoData)) {
			return new ArrayList<ReportDayViewItem>();
		}
		if (qp.hasNoneStatField4Filter()) {
			filterIds = new ArrayList<Integer>();
			for (PlanViewItem item : infoData) {
				filterIds.add(item.getPlanId());
			}
		}

		//3、获取统计数据
		List<ReportDayViewItem> olapDateList = byDayStatService.queryByDayData(userId,
				filterIds, flashFrom, flashTo, ReportConstants.TU_DAY);

		//4、获取Uv数据
		uvData = uvDataService.queryPlanDataByTime(userId, filterIds, 
											flashFrom, flashTo, null, 0, 
											ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);
		List<ReportDayViewItem> dorisDateList = ReportUtil.transformStatDataByDay(uvData);
		
		//5、合并统计数据和UV数据
		List<ReportDayViewItem> resultDateList = ReportUtils.mergeItemList(dorisDateList, olapDateList, OlapConstants.COLUMN.SHOWDATE, 
				Constants.statMergeVals, ReportDayViewItem.class, true);
		
		ReportUtil.generateExtentionFields(resultDateList);
		
		//6、返回
		return resultDateList;
	}
	
	/**
	 * getBudgetOverPlanIds:获取列表中需要查询下线时间的planId
	 *
	 * @param infoData 待返回的结果集
	 * @return 需要查询下线时间差的planId     
	*/
	public List<Integer> getBudgetOverPlanIds(List<PlanViewItem> infoData) {
		List<Integer> result = new ArrayList<Integer>(); 
		if(!CollectionUtils.isEmpty(infoData)) {
			for (PlanViewItem item : infoData ) {
				if (CproPlanConstant.PLAN_BUDGETOVER.equals(item.getBudgetOver()) ) {
					result.add(item.getPlanId());
				}
			}
		}
		return result;
	}

	/**
	 * getInSchedualPlanIds:获取列表中需要查询"正在投放"的planId
	 *
	 * @param infoData 待返回的结果集
	 * @return 需要查询“正在”的planId     
	*/
	public List<Integer> getInSchedualPlanIds(List<PlanViewItem> infoData) {
		List<Integer> result = new ArrayList<Integer>(); 
		if(!CollectionUtils.isEmpty(infoData)) {
			for (PlanViewItem item : infoData ) {
				if (CproPlanConstant.PLAN_STATE_NORMAL == item.getState() 
						&& !CproPlanConstant.PLAN_BUDGETOVER.equals(item.getBudgetOver())) {
					result.add(item.getPlanId());
				}
			}
		}
		return result;
	}
	
	/**
	 * 
	 * @param infoData
	 *            待返回给前端的VO列表数据
	 * @param offtimeList
	 *            下线时间列表
	 * @param userId
	 */
	private void mergeOfftimeInfo(List<PlanViewItem> infoData, List<PlanOffTimeVo> offtimeList,
			Integer userId) {
		Date today = new Date();
		for (PlanOffTimeVo offtime : offtimeList) {
			for (PlanViewItem item : infoData) {

				// 由于下线时间有15分钟左右的延迟，有可能取出的是昨天的下线时间差，因此此处需要看取出来的下线时间是否是当天
				if (item.getPlanId().equals(offtime.getPlanId())
						&& DateUtil.isSameDay(today, offtime.getOfftime())) {
					item.setOfftime(DateUtils.formatDate(offtime.getOfftime(), OFFTIME_VIEW_FORMAT));
				}
			}
		}
	}

	/**
	 * mergeInSchedualInfo: 合并“正在投放”信息
	 *
	 * @param infoData
	 * @param planIdsForInSchedual      
	*/
	private void mergeInSchedualInfo (List<PlanViewItem> infoData, List<Integer> planIdsForInSchedual) {
		for (Integer id : planIdsForInSchedual) {
			for (PlanViewItem item : infoData) {
				if (item.getPlanId().equals(id) ) {
					item.setInSchedule(ReportConstants.Boolean.TRUE);
				}
			}
		}
	}
	
	/**
	 * calculateSumData:根据返回结果休计算汇总信息
	 *
	 * @param infoData
	 * @return  返回结果集的汇总信息    
	*/
	public PlanSumData calculateSumData (List<PlanViewItem> infoData) {
		PlanSumData sumData = new PlanSumData();
		
		//1、累加汇总值
		for (PlanViewItem item : infoData) {
			sumData.setClks(sumData.getClks() + item.getClks());
			sumData.setCost(sumData.getCost() + item.getCost());
			sumData.setSrchs(sumData.getSrchs() + item.getSrchs());
			sumData.setSrchuv(sumData.getSrchuv() + item.getSrchuv());
			sumData.setClkuv(sumData.getClkuv() + item.getClkuv());
			sumData.setPlanCount(sumData.getPlanCount() + 1);
			if(item.getState() != null 
					&& CproPlanConstant.PLAN_VIEW_STATE_NORMAL == item.getViewState()) {
				sumData.setValidPlanCount(sumData.getValidPlanCount() + 1);
				sumData.setSumBudget(sumData.getSumBudget() + item.getBudget());
			}
		}
		
		//2、生成扩展数据
		sumData.generateExtentionFields();
		
		if (infoData.size() <= ReportWebConstants.FRONT_SORT_THRESHOLD ) {
//				&& (qp.getPage() == null || qp.getPage() <= 0 )) {
			//page<=0 && 当前行数小于等于1W
			sumData.setNeedFrontSort(ReportConstants.Boolean.TRUE);
		}
		
		return sumData;
	}
	
	/**
	 * 网盟推广首页-获取账户分日报表
	 * 
	 * add by kanghongwei since 星二期第二批
	 */
	public String userDataList() {
		try {
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);
			return SUCCESS;
		}
		
		/** 
		 * 如果统计时间包含今天，则返回空
		 */
//		if(super.isMixTodayAndBefore(from, to) || super.isOnlyToday(from, to)){
//			log.warn("推广首页不提供今日的统计数据");
//			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
//			jsonObject.addMsg(ReportWebConstants.ERR_DATE);
//			return SUCCESS;
//		}
		
		List<UserAccountViewItem> userViewItemList = reportCproPlanMgr.findUserAccountStatData(qp, from, to);
		if(userViewItemList == null){
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED_SERVER);
			jsonObject.setGlobalMsg(ReportWebConstants.ERR_DORIS);
			return SUCCESS;
		}
		// 汇总
		UserAccountViewItem sumUserAccountItem = reportCproPlanMgr.sumUserAccountStatData(userViewItemList);
		// 总页数
		int totalPage = super.getTotalPage(userViewItemList.size());
		// 分页
		userViewItemList = reportCproPlanMgr.pageUserAccountData(userViewItemList, qp);
		jsonObject.addData("list", userViewItemList);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("cache", sumUserAccountItem.getNeedFrontSort());
		jsonObject.addData("sum", sumUserAccountItem);
		
		// 处理当日和昨日不入库的uv和转化数据
		//this.reportFacade.postHandleTransAndUvData(userId, from, to, userViewItemList, sumUserAccountItem);
		
		return SUCCESS;
	}

	/**
	 * 网盟推广首页-下载账户分日报表
	 * 
	 * add by kanghongwei since 星二期第二批
	 * 
	 * @return
	 * @throws IOException
	 */
	public String downloadUserDataList() throws IOException {
		try {
			initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);
			return SUCCESS;
		}
		
		/** 
		 * 如果统计时间包含今天，则返回空
		 */
//		if(super.isMixTodayAndBefore(from, to) || super.isOnlyToday(from, to)){
//			log.warn("推广首页不提供今日的统计数据");
//			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
//			jsonObject.addMsg(ReportWebConstants.ERR_DATE);
//			return SUCCESS;
//		}
		
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
		
		//处理当日和昨日不入库的uv和转化数据 add by wangchongjie since 2012.11.03
		//this.reportFacade.postHandleTransAndUvData(userId, from, to, userViewItemList, sumUserAccountItem);
		
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
			// 文件名：[账户名]-账户分日报告-[日期分部].csv
			fileName = user.getUsername() + "-" + this.getText("download.account.filename.prefix");
			fileName = StringUtils.subGBKString(fileName, 0,
					ReportWebConstants.FILE_MAIN_NAME_MAXLENGTH);
			fileName += "-" + sd.format(from) + "-" + sd.format(to) + ".csv";
			fileName = new String(fileName.getBytes(fileEncoding), "ISO8859-1");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage());
		}
		return SUCCESS;
	}

	public ReportCproPlanMgr getReportCproPlanMgr() {
		return reportCproPlanMgr;
	}

	public void setReportCproPlanMgr(ReportCproPlanMgr reportCproPlanMgr) {
		this.reportCproPlanMgr = reportCproPlanMgr;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setCproPlanMgr(CproPlanMgr cproPlanMgr) {
		this.cproPlanMgr = cproPlanMgr;
	}
	
	public ReportCacheService getCacheService() {
		return cacheService;
	}

	public void setCacheService(ReportCacheService cacheService) {
		this.cacheService = cacheService;
	}
	
	public MfcService getMfcService() {
		return mfcService;
	}

	public void setMfcService(MfcService mfcService) {
		this.mfcService = mfcService;
	}
	
	public UserInfoMgr getUserInfoMgr() {
		return userInfoMgr;
	}

	public void setUserInfoMgr(UserInfoMgr userInfoMgr) {
		this.userInfoMgr = userInfoMgr;
	}

}
