package com.baidu.beidou.report.web.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;

import com.baidu.beidou.cprogroup.bo.AppExclude;
import com.baidu.beidou.cprogroup.bo.CproGroup;
import com.baidu.beidou.cprogroup.bo.GroupTradePrice;
import com.baidu.beidou.cprogroup.constant.UnionSiteCache;
import com.baidu.beidou.cprogroup.service.AppExcludeMgr;
import com.baidu.beidou.cprogroup.service.CproGroupMgr;
import com.baidu.beidou.cprogroup.service.GroupSiteConfigMgr;
import com.baidu.beidou.cprogroup.util.KTKeywordUtil;
import com.baidu.beidou.cprogroup.vo.AppInfo;
import com.baidu.beidou.cprogroup.vo.TradeInfo;
import com.baidu.beidou.cproplan.bo.CproPlan;
import com.baidu.beidou.cproplan.service.CproPlanMgr;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.olap.constant.Constants;
import com.baidu.beidou.olap.service.AppStatService;
import com.baidu.beidou.olap.vo.AppViewItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.dao.vo.GroupQueryParameter;
import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.report.service.ReportCproGroupMgr;
import com.baidu.beidou.report.util.ReportUtil;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.app.AppViewItemSum;
import com.baidu.beidou.report.vo.group.AppReportSumData;
import com.baidu.beidou.report.vo.group.AppReportVo;
import com.baidu.beidou.report.vo.group.ExtGroupViewItem;
import com.baidu.beidou.stat.driver.bo.Constant;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.LogUtils;
import com.baidu.beidou.util.StringUtils;
import com.baidu.beidou.util.TokenUtil;
import com.baidu.beidou.util.memcache.BeidouCacheInstance;
import com.baidu.unbiz.olap.util.ReportUtils;


public class ListGroupAppAction extends BeidouReportActionSupport{

	private static final long serialVersionUID = 1L;
	private static final int TOKEN_EXPIRE = 60;
	private String operation = null;
	private static final String OPERATION_STOP_CHARGE = "stop";
	private CproPlanMgr cproPlanMgr = null;
	private CproGroupMgr cproGroupMgr = null;
	private List<Integer> planIds = new ArrayList<Integer>();
	private List<Integer> groupIds = new ArrayList<Integer>();
	private ReportCproGroupMgr reportCproGroupMgr;
	private GroupSiteConfigMgr siteConfigMgr;	
	private AppExcludeMgr appExcludeMgr;
	
	@Resource(name="appStatServiceImpl")
	private AppStatService appStatMgr;


	/**
	 * 排序方向（正排为1，倒排为-1）
	 */
	private int orient;
	private Map<Integer, Map<Integer, Integer>> tradePriceMap = null;
	
	Map<Integer, Set<Long>> appExcludeMap = null;
	
	private List<Integer> firstTradeIds = new ArrayList<Integer>();

	private List<Integer> secondTradeIds = new ArrayList<Integer>();
	

	@Override
	protected void initStateMapping() {
		qp.setUserId(userId);
		//单独处理前端传递过来planid groupid unitid为0的情况
		if (qp.getPlanId() == null || qp.getPlanId() == 0) {
			qp.setPlanId(null);
		}
		if (qp.getGroupId() == null || qp.getGroupId() == 0) {
			qp.setGroupId(null);
		}
		if (qp.getUnitId() == null || qp.getUnitId() == 0) {
			qp.setUnitId(null);
		}
		if(qp.getPlanId() != null) {
			planIds = new ArrayList<Integer>();
			planIds.add(qp.getPlanId());
		}
		if(qp.getGroupId() != null) {
			groupIds = new ArrayList<Integer>();
			groupIds.add(qp.getGroupId());
		}
		if(qp.getPage() == null || qp.getPage() < 0) {
			qp.setPage(0);
		}

		if(qp.getPageSize() == null || qp.getPageSize() < 1) {
			qp.setPageSize(ReportConstants.PAGE_SIZE );
		}
		
		if(null != qp.getFTradeId()){
			firstTradeIds.add(qp.getFTradeId());
		}
		
		if(null != qp.getSTradeId()){
			secondTradeIds.add(qp.getSTradeId());
		}
		
	}	
	
	protected void initParameter() {
		
		super.initParameter();
		
		orient = ReportConstants.SortOrder.ASC;
		if (QueryParameterConstant.SortOrder.SORTORDER_DESC
				.equalsIgnoreCase(qp.getOrder())) {
			orient = ReportConstants.SortOrder.DES;		
		}
	}
	
	/**
	 * 生成用于App报告前端显示的VO
	 * @return 
	 */
	public String ajaxAppList(){		

		// 1、参数初始化
		try {
			this.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return SUCCESS;
		}
		
		// 2、生成显示的GroupDtViewItem列表
		List<AppViewItem> list = generateAppList();
		
		// 3、查询总条数，该数字可能不是最终值，因为有可能需要按统计值过滤，这样的话该值会被再次修改
		int count = list.size();
		
		// 4、排序
		Collections.sort(list, new CproAppComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		AppViewItemSum sumData = calculateSumData(list);

		// 6、计算总页码
		int totalPage = super.getTotalPage(count);
		
		// 7、获取分页
		list = pagerList(list);

		/**
		 * 如果统计时间是今天，将统计数据列中处点击、消费外的其他列置为-1
		 */
		
		jsonObject.addData("cache",1);
		jsonObject.addData("list", list);
		jsonObject.addData("totalPage", totalPage);
		
		jsonObject.addData("sum", sumData);
		
		//8、处理当日和昨日不入库的uv和转化数据
		this.reportFacade.postHandleTransAndUvData(userId, from, to, list, sumData);

		return SUCCESS;		
	}
	
	/**
	 * 生成用于兴趣报告前端显示的VO
	 * @return 
	 */
	public String downloadAppList() throws IOException{		

		//1、初始化一下参数
		initParameter();
		
		//2、查询账户信息
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}
		
		//3、构造报告VO，下载的CSV共有四部分：1、账户基本信息，2、列头，3、列表，4、汇总信息	
		AppReportVo vo = new AppReportVo(qp.getLevel());//报表下载使用的VO
		
		List<AppViewItem> infoData = null;//目标plan集
		
		//3.1、获取统计数据
		infoData = generateAppList();//无统计时间粒度
		
		//3.2、排序
		Collections.sort(infoData, new CproAppComparator(qp.getOrderBy(), orient));

		//3.3、生成汇总的统计数据
		AppViewItemSum sumData = calculateSumData(infoData);
		
		//3.4处理当日和昨日不入库的uv和转化数据 add by wangchongjie since 2012.11.03
		this.reportFacade.postHandleTransAndUvData(userId, from, to, infoData, sumData);
		
		//3.5、填充数据
		vo.setAccountInfo(generateAccountInfo(user.getUsername()));
		vo.setDetails(infoData);
		vo.setHeaders(generateReportHeader());
		vo.setSummary(generateReportSummary(sumData));
		
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(vo, output);
		
		//4、设置下载需要使用到的一些属性
		byte[] bytes = output.toByteArray();
		inputStream = new ByteArrayInputStream(bytes);
		fileSize = bytes.length;

		if ( qp.getGroupId() != null ) {
			CproGroup group = cproGroupMgr.findCproGroupById(qp.getGroupId());
			if (group != null) {
				fileName = group.getGroupName() + "-";
			}
		} else if ( qp.getPlanId() != null ) {
			CproPlan plan = cproPlanMgr.findCproPlanById(qp.getPlanId());
			if (plan != null) {
				fileName = plan.getPlanName() + "-";
			}
		} else {
			fileName = user.getUsername() + "-";
		}
		fileName += this.getText("download.app.filename.prefix");
		try {
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
	
	/**************************业务方法*********************************/
	/**
	 * 移动应用比较器
	 */
	class CproAppComparator implements Comparator<AppViewItem>  {

		int order;
		String col;
		public CproAppComparator(String col, int order) {
			this.col = col;
			this.order = order;
		}
		
		private int compareByString(String v1, String v2) {
			Collator collator=Collator.getInstance(java.util.Locale.CHINA);
			if(StringUtils.isEmpty(v1)) {
				return -1 * order;
			}
			if(StringUtils.isEmpty(v2)) {
				return order;
			}		
			return order * collator.compare(v1,v2);
		}
		
		public int compare(AppViewItem o1, AppViewItem o2) {
			if(o1 == null) {
				return -1 * order;
			}
			if(o2 == null) {
				return 1 * order;
			}
			if(col.equals(ReportWebConstants.APP_COLUMN_APPNAME)) {
				return compareByString(o1.getAppName(), o2.getAppName());
			}
			if(col.equals(ReportWebConstants.APP_COLUMN_PRICE)) {
				return -1 * order * (int)(o1.getPrice() - o2.getPrice());
			}
			if(ReportWebConstants.FRONT_BACKEND_ORDERNAME_PLANNAME.equalsIgnoreCase(col)) {
				return compareByString(o1.getPlanName(), o2.getPlanName());
			}
			if(ReportWebConstants.FRONT_BACKEND_ORDERNAME_GROUPNAME.equalsIgnoreCase(col)) {
				return compareByString(o1.getGroupName(), o2.getGroupName());
			}
			if(ReportWebConstants.FRONT_BACKEND_ORDERNAME_VIEWSTATE.equalsIgnoreCase(col)) {
				return -1 * order * (int)(o1.getViewState() - o2.getViewState());
			}
			if(ReportConstants.SRCHS.equals(col)) {
				if((o1.getSrchs() - o2.getSrchs())==0){
					return o1.getAppId().intValue() - o2.getAppId().intValue();
				}
				else{
					return order * (int)(o1.getSrchs() - o2.getSrchs());
				}
			}
			
			if(ReportConstants.CLKS.equals(col)) {
				return order * (int)(o1.getClks() - o2.getClks());
			}
			
			if(ReportConstants.COST.equals(col)) {
				if(o1.getCost() > o2.getCost()){
					return order * 1;
				}else if(o1.getCost() < o2.getCost()){
					return order * -1;
				}else{
					return 0;
				}
				
			}
			
			if(ReportConstants.CTR.equals(col)) {
				BigDecimal b1 = o1.getCtr();
				BigDecimal b2 = o2.getCtr();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}

			if(ReportConstants.ACP.equals(col)) {
				BigDecimal b1 = o1.getAcp();
				BigDecimal b2 = o2.getAcp();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}

			if(ReportConstants.CPM.equals(col)) {
				BigDecimal b1 = o1.getCpm();
				BigDecimal b2 = o2.getCpm();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if(ReportConstants.SRCHUV.equals(col)) {
				return order * (int)(o1.getSrchuv() - o2.getSrchuv());
			}
			
			if(ReportConstants.CLKUV.equals(col)) {
				return order * (int)(o1.getClkuv() - o2.getClkuv());
			}
			
			if(ReportConstants.SRSUR.equals(col)) {
				BigDecimal b1 = o1.getSrsur();
				BigDecimal b2 = o2.getSrsur();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if(ReportConstants.CUSUR.equals(col)) {
				BigDecimal b1 = o1.getCusur();
				BigDecimal b2 = o2.getCusur();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if(ReportConstants.COCUR.equals(col)) {
				BigDecimal b1 = o1.getCocur();
				BigDecimal b2 = o2.getCocur();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if(ReportConstants.RES_TIME_STR.equals(col)) {
				return order * (int)(o1.getResTimeStr().compareTo(o2.getResTimeStr()));
			}
			
			if(ReportConstants.ARRIVAL_RATE.equals(col)) {
				BigDecimal b1 = o1.getArrivalRate();
				BigDecimal b2 = o2.getArrivalRate();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if(ReportConstants.HOP_RATE.equals(col)) {
				BigDecimal b1 = o1.getHopRate();
				BigDecimal b2 = o2.getHopRate();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if(ReportConstants.DIRECT_TRANS_CNT.equals(col)) {
				return order * (int)(o1.getDirectTrans() - o2.getDirectTrans());
			}
			
			if(ReportConstants.INDIRECT_TRANS_CNT.equals(col)) {
				return order * (int)(o1.getIndirectTrans() - o2.getIndirectTrans());
			}
			
			return order;
		}			
	}
	
	private List<AppViewItem> generateAppList(){
		//1、获取推广组信息
		GroupQueryParameter queryParam = initGroupQueryParameter();
		List<ExtGroupViewItem> groupItems = reportCproGroupMgr.findExtCproGroupReportInfo(queryParam);
		Map<Integer, ExtGroupViewItem> extGroupViewMapping = new HashMap<Integer, ExtGroupViewItem>(groupItems.size());
		for(ExtGroupViewItem tmp : groupItems){
			extGroupViewMapping.put(tmp.getGroupId(), tmp);
		}
		
		//2、获取统计数据
		List<AppViewItem> olapList = appStatMgr.queryGroupAppData(userId, planIds, groupIds, firstTradeIds, 
				secondTradeIds, null, from, to, null, 0, ReportConstants.TU_NONE);
		
		//3、获取UV数据
		List<Map<String, Object>> uvData = uvDataService.queryGroupAppData(userId, planIds, groupIds,  
				this.firstTradeIds, this.secondTradeIds, null,
				from, to, null, 0, ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
		
		List<AppViewItem> dorisList = new ArrayList<AppViewItem>();
		List<AppViewItem> resultList = new ArrayList<AppViewItem>();
		//7、填充数据
		if (! CollectionUtils.isEmpty(uvData)) {
			for (Map<String, Object> row : uvData) {
				AppViewItem item = new AppViewItem();
				if (row != null) {
					item.setPlanId(Integer.valueOf(row.get(ReportConstants.PLAN).toString()));
					item.setGroupId(Integer.valueOf(row.get(ReportConstants.GROUP).toString()));
					item.setAppId(Long.valueOf(row.get(ReportConstants.APPID).toString()));
					item.fillStatRecord(row);
					dorisList.add(item);
				}
			}
		}
		
		Set<String> mergeKeys = new HashSet<String>(Arrays.asList(
				new String[]{Constants.COLUMN.GROUPID, Constants.COLUMN.APPID}));
		
		resultList = ReportUtils.mergeItemList(dorisList, olapList, mergeKeys, 
				Constants.statMergeVals, AppViewItem.class, true);
		
		ReportUtil.generateExtentionFields(resultList);
				
		this.generateAddedInfo();
		
		//9、构造显示VO列表
		if (!CollectionUtils.isEmpty(resultList)) {//如果有统计数据
			List<AppViewItem> toDel = new ArrayList<AppViewItem>();

			// 9.3合并数据并过滤
			for (AppViewItem record : resultList) {

				AppViewItem appItem = record;
				
				Integer groupId = appItem.getGroupId();
				
				//获取推广组信息
				ExtGroupViewItem groupViewItem = extGroupViewMapping.get(groupId);
				if (groupViewItem != null) {
					appItem.setGroupId(groupId);
					appItem.setGroupName(groupViewItem.getGroupName());
					appItem.setPlanId(groupViewItem.getPlanId());
					appItem.setPlanName(groupViewItem.getPlanName());
					appItem.setViewState(groupViewItem.getViewState());
					appItem.setViewStateOrder(groupViewItem.getViewStateOrder());
				}
				
				//获取App及行业、出价信息
				Long appId = appItem.getAppId();
				AppInfo app = UnionSiteCache.appCache.getAppInfoById(appId);
				if(null != app){
					appItem.setAppName(app.getName());
					TradeInfo firstTrade = UnionSiteCache.tradeInfoCache.getTradeInfoById(app.getFirstTrade());//从Cache中获取行业信息
					TradeInfo secondTrade = UnionSiteCache.tradeInfoCache.getTradeInfoById(app.getSecondTrade());
					this.appendTradeInfo(groupId, groupViewItem, firstTrade, secondTrade, appItem);
				}else{
					appItem.setAppName("未知");
				}
				
				//获取App排除信息
				if(CollectionUtils.isEmpty(this.appExcludeMap.get(groupId))){
					appItem.setFiltered(false);
				}else{
					appItem.setFiltered(this.appExcludeMap.get(groupId).contains(appId));
				}

				// 名称为“未知”的app不允许在列表“操作栏”进行“排除操作”
				if (null == app) {
					appItem.setFiltered(true);
				}

				//按照过滤条件过滤
				if (this.isFiltered(appItem)) {
					toDel.add(appItem);
				}
			}
			if(CollectionUtils.isNotEmpty(toDel)){
				resultList.removeAll(toDel);
			}
		}
		
		return resultList;
	}
	
	
	private void generateAddedInfo(){
		//从数据库中获取全部的分网站和分行业出价信息，用于组装最终的出价
		List<GroupTradePrice> tradePrice = siteConfigMgr.findAllTradePrice(userId, qp.getPlanId(), qp.getGroupId());
		//为了提高性能，这里还必须将其组成成Map使用		
		tradePriceMap = new HashMap<Integer, Map<Integer, Integer>>();		
		for(GroupTradePrice gt : tradePrice){
			Integer groupId = gt.getGroupid();
			Integer tradeId = gt.getTradeid();
			Map<Integer, Integer> tmp = tradePriceMap.get(groupId);
			if(tmp == null){
				tmp = new HashMap<Integer, Integer>();
				tradePriceMap.put(groupId, tmp);
			}
			tmp.put(tradeId, gt.getPrice());
		}
		
		//groupid-排除的appid，获取移动应用排除信息
		this.appExcludeMap = new HashMap<Integer, Set<Long>>();
		List<AppExclude>  appExcludeList = appExcludeMgr.findAppExclude(userId);
		for(AppExclude appExclude : appExcludeList){
			Integer groupId = appExclude.getGroupId();
			Set<Long> tmp = appExcludeMap.get(groupId);
			if(tmp == null){
				tmp = new HashSet<Long>();
				appExcludeMap.put(groupId, tmp);
			}
			tmp.add(appExclude.getAppSid());
		}
	}
	
	private void appendTradeInfo(Integer groupId, ExtGroupViewItem viewItem, 
			TradeInfo firstTrade, TradeInfo secondTrade, AppViewItem vo){
		
		//追加出价信息，这个比较特别
		vo.setPrice((double)viewItem.getPrice());
		if(tradePriceMap != null && tradePriceMap.get(groupId) != null){
			Map<Integer, Integer> priceMap = tradePriceMap.get(groupId);
			if(priceMap != null){
				if(secondTrade != null){
					//这是个二级行业
					Integer secondPrice = priceMap.get(secondTrade.getTradeid());
					if(secondPrice != null){
						//二级上有出价
						vo.setPrice((double)secondPrice);
					}
				}
				Integer firstPrice = priceMap.get(firstTrade.getTradeid());
				if(firstPrice != null){
					if(vo.getPrice() <= 0.0){
						//一级行业上有出价
						vo.setPrice((double)firstPrice);
					}
				}
			}
		}

		vo.setPrice(vo.getPrice()/100);
	
		if(firstTrade != null){
			vo.setFirstTrade(firstTrade.getTradename());
			vo.setFirstTradeId(firstTrade.getTradeid());
		}
		if(secondTrade != null){
			vo.setSecondTrade(secondTrade.getTradename());
			vo.setSecondTradeId(secondTrade.getTradeid());
		}
	}
	
	/**
	 * calculateSumData:根据列表返回汇总列
	 * 
	 * @param infoData
	 * @return 返回结果集的汇总信息
	 */
	public AppViewItemSum calculateSumData(List<AppViewItem> infoData) {
		AppViewItemSum sumData = new AppViewItemSum();
		for (AppViewItem item : infoData) {
			sumData.setClks(sumData.getClks() + item.getClks());
			sumData.setCost(sumData.getCost() + item.getCost());
			sumData.setSrchs(sumData.getSrchs() + item.getSrchs());		
			sumData.setSrchuv(sumData.getSrchuv() + item.getSrchuv());
			sumData.setClkuv(sumData.getClkuv() + item.getClkuv());
		}
		sumData.generateExtentionFields();
		if(null != infoData){
			sumData.setAppCount(infoData.size());
		}
		
		return sumData;
	}
	
	/**
	 * 返回推广组信息查询参数封装
	 * @return
	 */
	private GroupQueryParameter initGroupQueryParameter(){
		GroupQueryParameter queryParam = new GroupQueryParameter();	
		queryParam.setUserId(userId);
		if(QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(qp.getLevel())){
			//Do nothing
		}else if(QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(qp.getLevel())){
			queryParam.setPlanId(super.qp.getPlanId());			
		}else if(QueryParameterConstant.LEVEL.LEVEL_GROUP.equals(qp.getLevel())){
			List<Long> ids = new ArrayList<Long>(1);
			ids.add(super.qp.getGroupId().longValue());
			queryParam.setIds(ids);
		}
		return queryParam;
	}
	
	/**
	 * isFiltered:判断是否将当前记录过滤
	 *
	 * @param item 待判断VO对象
	 * @return  true表示要过滤，false表示保留    
	 */
	protected boolean isFiltered(AppViewItem item) {
		//1、按查询关键词过滤
		String query = qp.getKeyword();
		if(!StringUtils.isEmpty(query)) {
			// add by kanghongwei since cpweb429(qtIM)
			query = KTKeywordUtil.validateKeyword(query);
			if (StringUtils.isEmpty(item.getAppName())) {
				return true;
			} else if( !item.getAppName().contains(query) && !String.valueOf(item.getAppId()).contains(query)) {
				return true;
			}
		}
		//2、按统计字段过滤
		return ReportWebConstants.filter(qp, item);
	}
	
	/**
	 * generateAccountInfo: 生成报表用的账户信息
	 *
	 * @return      
	*/
	protected ReportAccountInfo generateAccountInfo(String userName) {
		ReportAccountInfo accountInfo = new ReportAccountInfo();
		accountInfo.setReport(this.getText("download.account.report.app"));
		accountInfo.setReportText(this.getText("download.account.report"));

		accountInfo.setAccount(userName);
		accountInfo.setAccountText(this.getText("download.account.account"));

		accountInfo.setDateRange(sd1.format(from) + " - " + sd1.format(to));
		accountInfo
				.setDateRangeText(this.getText("download.account.daterange"));

		String level = this.getText("download.account.level.allplan");
		if (QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(qp.getLevel())) {
			
			Integer planId = qp.getPlanId();
			if(planId != null ) {
				CproPlan plan = cproPlanMgr.findCproPlanById(planId);
				if (plan != null) {
					level += "/" + this.getText("download.account.level.plan") + plan.getPlanName();
				}
			}
		} else if (QueryParameterConstant.LEVEL.LEVEL_GROUP.equals(qp.getLevel())) {
			Integer groupId = qp.getGroupId();
			if(groupId != null ) {
				CproGroup group = cproGroupMgr.findCproGroupById(groupId);
				if (group != null) {
					CproPlan plan = cproPlanMgr.findCproPlanById(group.getPlanId());
					level += "/" + this.getText("download.account.level.plan") + plan.getPlanName();
					level += "/" + this.getText("download.account.level.group") + group.getGroupName();
				}
			}
			
		}  else {
			//account级别不用设置具体信息
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
	protected String[] generateReportHeader() {
		
		List<String> header = new ArrayList<String>();
		String prefix = null;
		int maxColumn = 19;
		prefix = "download.app.head.col";
		for ( int col = 0; col < maxColumn; col++ ) {
			if(col == 2){
				//第二列是plan如果在plan或者group层级不需要看
				if(qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_PLAN)
						|| qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_GROUP)){
					continue;
				}
			}else if(col == 3){
				//第三列是group如果在group层级不需要看
				if(qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_GROUP)){
					continue;
				}
			}
			header.add(this.getText(prefix + (col + 1)));
		}
		
		String[] array = new String[header.size()];
		return header.toArray(array);
	}
	

	/**
	 * generateReportSummary: 生成推广计划列表汇总信息
	 *
	 * @param sumData 汇总数据
	 * @return 用于表示报表的汇总信息      
	*/
	protected AppReportSumData generateReportSummary(AppViewItemSum sumData) {

		AppReportSumData sum = new AppReportSumData(); 
		
		sum.setClks(sumData.getClks());
		sum.setSrchs(sumData.getSrchs());
		sum.setCost(sumData.getCost());
		sum.setAcp(sumData.getAcp().doubleValue());
		sum.setCtr(sumData.getCtr().doubleValue());
		sum.setCpm(sumData.getCpm().doubleValue());
		sum.setSrchuv(sumData.getSrchuv());
		sum.setClkuv(sumData.getClkuv());	
		return sum;
	}
	
	public String ajaxAppToken(){

		// 1、参数初始化
		try {
			this.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return SUCCESS;
		}
		List<AppViewItem> list = this.generateAppList();
		if(CollectionUtils.isEmpty(list)){
			buildEmptyResult();
		}else{
			generateAppToken(list);
		}	
		return SUCCESS;
	}
	
	private void generateAppToken(List<AppViewItem> list){
		if(OPERATION_STOP_CHARGE.equals(operation)){
			HashMap<Integer, List<Long>> result = new HashMap<Integer, List<Long>>();
			for(AppViewItem item : list){
				List<Long> tmp = result.get(item.getGroupId());
				if(tmp == null){
					tmp = new ArrayList<Long>();
					result.put(item.getGroupId(), tmp);
				}
				tmp.add(item.getAppId());				
			}	
			String token = TokenUtil.generateToken();
			BeidouCacheInstance.getInstance().memcacheRandomSet(token, result, TOKEN_EXPIRE);
			buildTokenJson(token);
		}else{
			throw new BusinessException("invalid operation");
		}
	}
	
	private void buildTokenJson(String token){
		jsonObject.addData("token", token);
	}
	
	private void buildEmptyResult(){
		jsonObject.addData("list", Collections.emptyList());
		jsonObject.addData("totalPage", 0);
		jsonObject.addData("cache", ReportConstants.Boolean.TRUE);
		jsonObject.addData("sum", new AppViewItemSum(0));
	}
	
	/*********************getter and setter**************************/
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

	public CproGroupMgr getCproGroupMgr() {
		return cproGroupMgr;
	}

	public void setCproGroupMgr(CproGroupMgr cproGroupMgr) {
		this.cproGroupMgr = cproGroupMgr;
	}
	
	public GroupSiteConfigMgr getSiteConfigMgr() {
		return siteConfigMgr;
	}

	public void setSiteConfigMgr(GroupSiteConfigMgr siteConfigMgr) {
		this.siteConfigMgr = siteConfigMgr;
	}
	
	public List<Integer> getFirstTradeIds() {
		return firstTradeIds;
	}

	public void setFirstTradeIds(List<Integer> firstTradeIds) {
		this.firstTradeIds = firstTradeIds;
	}

	public List<Integer> getSecondTradeIds() {
		return secondTradeIds;
	}

	public void setSecondTradeIds(List<Integer> secondTradeIds) {
		this.secondTradeIds = secondTradeIds;
	}
	

	public AppExcludeMgr getAppExcludeMgr() {
		return appExcludeMgr;
	}

	public void setAppExcludeMgr(AppExcludeMgr appExcludeMgr) {
		this.appExcludeMgr = appExcludeMgr;
	}
	
	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

}

