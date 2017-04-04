package com.baidu.beidou.report.web.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;

import com.baidu.beidou.cprounit.constant.CproUnitConstant;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.report.service.ReportCproGroupMgr;
import com.baidu.beidou.report.service.ReportCproPlanMgr;
import com.baidu.beidou.report.service.ReportCproUnitMgr;
import com.baidu.beidou.report.vo.QueryParameter;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.StatInfo;
import com.baidu.beidou.report.vo.TransReportVo;
import com.baidu.beidou.olap.service.GroupStatService;
import com.baidu.beidou.olap.service.PlanStatService;
import com.baidu.beidou.olap.service.SiteStatService;
import com.baidu.beidou.olap.service.UnitStatService;
import com.baidu.beidou.olap.vo.GroupViewItem;
import com.baidu.beidou.olap.vo.MainSiteViewItem;
import com.baidu.beidou.olap.vo.PlanViewItem;
import com.baidu.beidou.olap.vo.SubSiteViewItem;
import com.baidu.beidou.olap.vo.UnitViewItem;
import com.baidu.beidou.stat.driver.bo.Constant;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.LogUtils;
import com.baidu.beidou.util.StringUtils;

public class ListTransDataAction extends BeidouReportActionSupport{

	private static final long serialVersionUID = 1L;
	
	//input parameters
	private Long siteId;	//广告主网站ID
	private Long transId;	//转化ID
	private int transSearchRange = ReportWebConstants.TRANS_DATA_DIMENSION.PLAN;//统计维度，共分四种：推广计划、推广组、创意、推广组分网站
	
	//internal used attributes
	private List<Long> transSiteIds;	//用于向后端发起0或多个广告主网站过滤，本aciton中将siteId置于其中。
	private List<Long> transTargetIds;	//用于向后端发起0或多个转化过滤，本aciton中将siteId置于其中。
	
	private ReportCproPlanMgr reportCproPlanMgr;
	private ReportCproGroupMgr reportCproGroupMgr;
	private ReportCproUnitMgr reportCproUnitMgr;
	
	@Resource(name="siteStatServiceImpl")
	SiteStatService siteStatMgr;
	
	@Resource(name="planStatServiceImpl")
	PlanStatService planStatMgr;
	
	@Resource(name="groupStatServiceImpl")
	GroupStatService groupStatMgr;
	
	@Resource(name="unitStatServiceImpl")
	UnitStatService unitStatMgr;
	
	@Override
	protected void initStateMapping() {//目前无需按照状态过滤，因此不必要实现BeidouReportActionSupport的这一方法。
		//do nothing
	}
	
	protected void initParameter() {
		super.initParameter();
		transSiteIds = new ArrayList<Long>();
		transTargetIds = new ArrayList<Long>();
		if (siteId != null && siteId > 0) {
			transSiteIds.add(siteId);
		}
		if (transId != null && transId > 0) {
			transTargetIds.add(transId);
		}
		if (transSearchRange != ReportWebConstants.TRANS_DATA_DIMENSION.PLAN
				&&
				transSearchRange != ReportWebConstants.TRANS_DATA_DIMENSION.GROUP
				&&
				transSearchRange != ReportWebConstants.TRANS_DATA_DIMENSION.UNIT
				&&
				transSearchRange != ReportWebConstants.TRANS_DATA_DIMENSION.GROUP_SITE
		) {
			throw new RuntimeException("统计范围有误：transSearchRange=" + transSearchRange);
		}
	}
	public String download() throws IOException{

		// 初始化参数
		this.initParameter();
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}

		// 下载的CSV共有四部分
		// 1、账户基本信息，2、列头，3、列表，4、汇总行

		ReportAccountInfo accountInfo = generateAccountInfo(user.getUsername());
		String[] headers = generateReportHeader();
		List resultList = getResultListForDownload();
		StatInfo sumData = getSumDataForDownload(resultList);

		TransReportVo vo = new TransReportVo();// 报表下载使用的VO
		vo.setAccountInfo(accountInfo);
		vo.setHeaders(headers);
		vo.setDetails(resultList);
		vo.setSummary(sumData);
		vo.setDimensionType(transSearchRange);

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(vo, output);

		// 设置下载需要使用到的一些属性
		byte[] bytes = output.toByteArray();
		inputStream = new ByteArrayInputStream(bytes);
		fileSize = bytes.length;
		fileName = generateFileNameForDownload();
		return SUCCESS;
	}
	
	//根据数据list生成下载文件汇总行
	private StatInfo getSumDataForDownload(List resultList) {
		StatInfo sumData = null;
		if (transSearchRange == ReportWebConstants.TRANS_DATA_DIMENSION.GROUP_SITE) {
			sumData = calculateGroupSiteSumData(resultList);
		} else {
			sumData = calculateSumData(resultList);
		}
		return sumData;
	}

	//为下载收集数据
	private List getResultListForDownload() {

		List resultList = null;

		switch(transSearchRange){
		case ReportWebConstants.TRANS_DATA_DIMENSION.GROUP:
			resultList = getResultList(Integer.class, ReportConstants.GROUP);
			break;
		case ReportWebConstants.TRANS_DATA_DIMENSION.UNIT:
			resultList = getResultList(Long.class, ReportConstants.UNIT);
			break;
		case ReportWebConstants.TRANS_DATA_DIMENSION.GROUP_SITE:
			resultList = getGroupSiteResultList();
			break;
		default:
			resultList = getResultList(Integer.class, ReportConstants.PLAN);
		}
		return resultList;
	}

	//生成下载文件的表头
	private String[] generateReportHeader() {
		String[] headers = this.getText("download.trans.head.dimension" + transSearchRange).split(",");
		return headers;
	}

	//生成下载文件中的账户相关信息
	protected ReportAccountInfo generateAccountInfo(String userName) {
		ReportAccountInfo accountInfo = new ReportAccountInfo();
		accountInfo.setReport(this.getText("download.account.report.trans"));
		accountInfo.setReportText(this.getText("download.account.report"));
		
		accountInfo.setAccount(userName);
		accountInfo.setAccountText(this.getText("download.account.account"));
		
		accountInfo.setDateRange(sd1.format(from) + " - " + sd1.format(to));
		accountInfo.setDateRangeText(this.getText("download.account.daterange"));	
		
		String levelText = this.getText("download.account.trans.level");
		String level = this.getText("download.account.level.trans"+transSearchRange);
		accountInfo.setLevel(level);	
		accountInfo.setLevelText(levelText);
		
		return accountInfo;
	}
	
	private String generateFileNameForDownload(){

		String transName = "全部";
		String dimension = null;
		String fromTime = sd.format(from);
		String toTime = sd.format(to);

		if (transId != null && transId > 0) {
			transName = getTransName();
			//下列字符作为文件名非法：/ \ ? * : | " < >
			if (org.apache.commons.lang.StringUtils.containsAny(transName, "/\\\\?*:|\"<>")) {
				transName = "";
			} else {
				transName += "-";
			}
		} else {
			transName += "-";
		}

		switch(transSearchRange){
		case ReportWebConstants.TRANS_DATA_DIMENSION.GROUP:
			dimension = "推广组";
			break;
		case ReportWebConstants.TRANS_DATA_DIMENSION.UNIT:
			dimension = "创意";
			break;
		case ReportWebConstants.TRANS_DATA_DIMENSION.GROUP_SITE:
			dimension = "推广组分网站";
			break;
		default:
			dimension = "推广计划";
		}

		String prefix = transName + dimension;
		try {
			prefix = StringUtils.subGBKString(prefix, 0,ReportWebConstants.FILE_MAIN_NAME_MAXLENGTH);
		} catch (UnsupportedEncodingException e) {
			LogUtils.error(log, e);//should never happen
		}
		String fileName = prefix + "-" + fromTime + "-" + toTime + ".csv";
		
		try {
			fileName = new String(fileName.getBytes(fileEncoding), "ISO8859-1");
		} catch (UnsupportedEncodingException e) {
			LogUtils.error(log, e);//should never happen
		}
		
		return fileName;
	}

	//获取转化名称
	private String getTransName() {
		String transName = this.transReportFacade.getTransName(userId,siteId,transId);
		if (transName == null) {
			throw new RuntimeException("转化ID或网站ID错误");
		}
		return transName;
	}

	public String execute(){
		
		//初始化参数==============>
		try{
			this.initParameter();
		} catch (Exception e){
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED_SERVER);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);
			return SUCCESS;
		}
		//<==============初始化参数结束
		
		String acctionResult = SUCCESS;
		
		switch(transSearchRange){
		
		case ReportWebConstants.TRANS_DATA_DIMENSION.GROUP:
			acctionResult = listTransDataInternal(Integer.class,ReportConstants.GROUP);
			break;

		case ReportWebConstants.TRANS_DATA_DIMENSION.UNIT:
			acctionResult = listTransDataInternal(Long.class,ReportConstants.UNIT);
			break;

		case ReportWebConstants.TRANS_DATA_DIMENSION.GROUP_SITE:
			acctionResult = listTransDataInGroupSiteDimension();
			break;

		default:
			acctionResult = listTransDataInternal(Integer.class,ReportConstants.PLAN);
		}

		return acctionResult;
	}
	
	private static boolean containsSecondDomainSite(String siteUrl){
		return ReportWebConstants.showSecondDomainSiteList.contains(siteUrl);
	}

	private String listTransDataInGroupSiteDimension() {
		
		List resultList = getGroupSiteResultList();
		
		//获取总页数
		int totalPage = super.getTotalPage(resultList.size());
		
		//计算汇总数据
		StatInfo sumData = calculateGroupSiteSumData(resultList);
		
		//是否前端缓存
		boolean cache = true;
		
		jsonObject.addData("list", resultList);
		jsonObject.addData("totalNum", resultList.size());
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("cache", cache? ReportConstants.Boolean.TRUE : ReportConstants.Boolean.FALSE);
		jsonObject.addData("sum", sumData);
		
		return SUCCESS;
	}

	private static List mergeGroupSiteDbAndNonDbData(List<GroupViewItem> dbData,
			List<Map<String, Object>> nonDbData) {

		Map<Integer,GroupViewItem> map = new HashMap<Integer, GroupViewItem>();
		for (GroupViewItem group : dbData) {
			map.put(group.getGroupId(), group);
		}
		
		Iterator<Map<String, Object>> iterator = nonDbData.iterator();//统计+转化数据
		while (iterator.hasNext()) {
			Map<String, Object> item = iterator.next();

			GroupViewItem group = map.get(item.get(ReportConstants.GROUP));
			if (group == null) {//数据库中无此推广组
				iterator.remove();
			} else {
				item.put("groupName", group.getGroupName());
				item.put("planName", group.getPlanName());
				item.put("planId",group.getPlanId());
				item.put("groupId",group.getGroupId());
				item.put("viewState", group.getViewState());
				item.put("state", group.getState());
				item.put("planState", group.getPlanState());
				item.remove(ReportConstants.GROUP);
				item.remove(ReportConstants.PLAN);
			}
		}
		
		return nonDbData;
	}

	private List<Map<String, Object>> getGroupSiteNonDbData() {

		
		//是按照统计字段排序吗？点击/展现/消费
		boolean isOrderByStatDataField = ReportConstants.isStatField(qp.getOrderBy());
		
		//是按照转化数据排序吗？直接转化/间接转化
		boolean isOrderByTransDataField = ReportConstants.isTransDataField(qp.getOrderBy());
		
		//排序方向
		int orient = ReportConstants.SortOrder.ASC;
		if (QueryParameterConstant.SortOrder.SORTORDER_DESC.equalsIgnoreCase(qp.getOrder())){
			orient = ReportConstants.SortOrder.DES;
		}
		
		//====>获取主域的转化数据
		List<Map<String, Object>> transMainsiteData = null;
		if (isOrderByTransDataField) {
			transMainsiteData = this.transReportFacade.queryGroupMainSiteData(
					super.getUserId(), transSiteIds,transTargetIds, null,null,null,
					from, to, qp.getOrderBy(), orient,
					ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT,0,0
			);
		} else {
			transMainsiteData = this.transReportFacade.queryGroupMainSiteData(
					super.getUserId(), transSiteIds,transTargetIds, null,null,null,
					from, to, null, 0,
					ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT,0,0
			);
		}//<========获取主域转化数据
		
		boolean hasSecondDomainToHandle = false;//是否需要解析某些二级域的转化数据
		
		Set<String> mainSiteUrls = new HashSet<String>();
		Set<String> subSiteUrls = new HashSet<String>();
		Set<Integer> groupIds = new HashSet<Integer>();
		
		//将有转化数据的主域 URL记录下来，如果有自由流量，对hasSecondDomainToHandle置位
		Iterator<Map<String, Object>> iterator = transMainsiteData.iterator();
		while(iterator.hasNext()){
			Map<String, Object> item = iterator.next();//一个推广组主域转化数据
			
			Integer groupId = (Integer) (item.get(ReportConstants.GROUP));
			groupIds.add(groupId);
			
			String mainSiteUrl = (String) (item.get(ReportConstants.MAINSITE));//主域URL
			if (this.containsSecondDomainSite(mainSiteUrl)) {//如果需要解析二级域，则从主域转化数据里面remove掉
				hasSecondDomainToHandle = true;
				iterator.remove();
			} else {
				mainSiteUrls.add(mainSiteUrl);//需要获取这个主域的统计数据：点击/展现/消费
				//BDSiteInfo siteInfo = UnionSiteCache.siteInfoCache.getSiteInfoBySiteUrl(mainSiteUrl);
				//do we need siteId?, we can get it here
			}
		}
		
		List<Integer> groupIdList = new ArrayList<Integer>();
		groupIdList.addAll(groupIds);
		
		//如果需要，获取二级域的转化数据
		List<Map<String, Object>> transSecondDomainData = null;
		if (hasSecondDomainToHandle) {
			transSecondDomainData = this.transReportFacade.queryGroupSubSiteData(
					super.getUserId(), transSiteIds,transTargetIds, null,groupIdList,ReportWebConstants.showSecondDomainSiteList,null,
					from, to, null, 0,
					ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT,0,0
			);
			for (Map<String, Object> item : transSecondDomainData) {
				String subSiteUrl = (String)(item.get(ReportConstants.SITE));
				subSiteUrls.add(subSiteUrl);
			}
		} else {
			transSecondDomainData = new ArrayList<Map<String, Object>>();
		}
		
		//重新处理标记位，没有二级域转化数据，则不需要处理二级域统计数据
		hasSecondDomainToHandle = hasSecondDomainToHandle && transSecondDomainData.size()>0;
		
		//获取mainSite统计数据
		List<MainSiteViewItem> mainSiteStatList = null;
		if (!hasSecondDomainToHandle && isOrderByStatDataField) {//如果需要排序就排序，什么情况排序有意义？不需处理二级域的情况排序才有意义
			mainSiteStatList = siteStatMgr.queryGroupMainSiteData(super.getUserId(), null, groupIdList, null, null, 
					mainSiteUrls, from, to, qp.getOrderBy(), orient, ReportConstants.TU_NONE);
		} else {
			mainSiteStatList = siteStatMgr.queryGroupMainSiteData(super.getUserId(), null, groupIdList, null, null, 
					mainSiteUrls, from, to, null, 0, ReportConstants.TU_NONE);
		}
		
		//获取subSite统计数据
		List<SubSiteViewItem> subSiteStatList = null;
		if (hasSecondDomainToHandle) {//别排序了，反正需要重新排
			subSiteStatList = siteStatMgr.queryGroupSubSiteData(super.getUserId(), null, groupIdList, null, subSiteUrls, from, to, null, 0, ReportConstants.TU_NONE);

		}
		
		List<Map<String, Object>> mainSiteStatData = new ArrayList<Map<String, Object>>();
		if(CollectionUtils.isNotEmpty(mainSiteStatList)){
			for(MainSiteViewItem item : mainSiteStatList){
				Map<String, Object> stat = new HashMap<String, Object>();

				stat.put(ReportConstants.SRCHS, item.getSrchs());
				stat.put(ReportConstants.CLKS, item.getClks());
				stat.put(ReportConstants.COST, item.getCost() * 100);
				stat.put(ReportConstants.CTR, item.getCtr());
				stat.put(ReportConstants.CPM, item.getCpm());
				stat.put(ReportConstants.ACP, item.getAcp());
				stat.put(ReportConstants.PLAN, item.getPlanId());
				stat.put(ReportConstants.GROUP, item.getGroupId());
				stat.put(ReportConstants.MAINSITE, item.getSiteUrl());
				
				mainSiteStatData.add(stat);
			}
		
		}
		
		List<Map<String, Object>> subSiteStatData = new ArrayList<Map<String, Object>>();
		if(CollectionUtils.isNotEmpty(subSiteStatData)){
			Map<String, Object> stat = new HashMap<String, Object>();
			
			for(SubSiteViewItem item : subSiteStatList){
				stat.put(ReportConstants.SRCHS, item.getSrchs());
				stat.put(ReportConstants.CLKS, item.getClks());
				stat.put(ReportConstants.COST, item.getCost() * 100);
				stat.put(ReportConstants.CTR, item.getCtr());
				stat.put(ReportConstants.CPM, item.getCpm());
				stat.put(ReportConstants.ACP, item.getAcp());
				stat.put(ReportConstants.PLAN, item.getPlanId());
				stat.put(ReportConstants.GROUP, item.getGroupId());
				stat.put(ReportConstants.SITE, item.getSiteUrl());
			}

			subSiteStatData.add(stat);
		}
		
		
		//合并主域的转化数据与统计数据
		List<Map<String, Object>> mainSiteData = mergeGroupSiteData(transMainsiteData,mainSiteStatData,ReportConstants.MAINSITE,isOrderByTransDataField,orient == ReportConstants.SortOrder.ASC);
		
		//如果需要处理 二级域，则还需要再次排序
		if (hasSecondDomainToHandle) {
			//合并二级域的转化数据与统计数据
			List<Map<String, Object>> subSiteData = mergeGroupSiteData(transSecondDomainData,subSiteStatData,ReportConstants.SITE,true,orient == ReportConstants.SortOrder.ASC);
			
			//将二级域的数据合并到主域的数据中
			for (Map<String, Object> item : subSiteData) {
				Object subSite = item.get(ReportConstants.SITE);
				item.put(ReportConstants.MAINSITE, subSite);
			}
			mainSiteData.addAll(subSiteData);//合并一二级数据
			
			//对最终的数据进行排序
			String orderColumn = qp.getOrderBy();
			if (!isOrderByTransDataField && !isOrderByStatDataField) {
				isOrderByTransDataField = true;
				orderColumn = ReportConstants.DIRECT_TRANS_CNT;
				
			}
			mainSiteData = orderizeGroupSiteData(mainSiteData,orderColumn,orient);
		}
		
		return mainSiteData;
	}

	private static List<Map<String, Object>> orderizeGroupSiteData(
			List<Map<String, Object>> groupSiteData, final String orderColumn, final int orient) {
		
		Comparator<Map<String, Object>> comparator = new Comparator<Map<String,Object>>() {
			public int compare(Map<String, Object> o1, Map<String, Object> o2) {
				Comparable value1 = (Comparable) o1.get(orderColumn);
				Comparable value2 = (Comparable) o2.get(orderColumn);
				int result = (value1==null? -1 : value1.compareTo(value2));
				return orient == ReportConstants.SortOrder.DES ? (0-result):result;
			}
		};
		
		Collections.sort(groupSiteData, comparator);
		return groupSiteData;
	}

	private static List<Map<String, Object>> mergeGroupSiteData(
			List<Map<String, Object>> transData,
			List<Map<String, Object>> statData, String siteKey,
			boolean isOrderByTransDataField,
			boolean asc) {
		
		if (CollectionUtils.isNotEmpty(statData)) {//Cost单位转换成元在此做
			for (Map<String, Object> item : statData) {
				Double costInFen = (Double) item.get(ReportConstants.COST);//以分为单位
				Double costInYuan = costInFen/100d;
				item.put(ReportConstants.COST, costInYuan);
			}
		}
	
		if(!isOrderByTransDataField){
			//为转化数据建立索引，key为group+site
			Map<Object,Map<String, Object>> transDataMap = new HashMap<Object,Map<String, Object>>();
			for(Map<String, Object> trans : transData){
				String mapId = trans.get(ReportConstants.GROUP) + "_" + trans.get(siteKey);
				transDataMap.put(mapId, trans);
			}

			//将转化数据merge到统计数据中，如果相应转化数据不存在，则扔掉统计数据
			Iterator<Map<String, Object>> iterator = statData.iterator();
			while(iterator.hasNext()){
				Map<String, Object> stat = iterator.next();
				String mapId = stat.get(ReportConstants.GROUP) + "_" + stat.get(siteKey);
				Map<String, Object> trans = transDataMap.get(mapId);
				if (trans!=null) {
					stat.put(ReportConstants.DIRECT_TRANS_CNT, trans.get(ReportConstants.DIRECT_TRANS_CNT));
					stat.put(ReportConstants.INDIRECT_TRANS_CNT, trans.get(ReportConstants.INDIRECT_TRANS_CNT));
					transDataMap.remove(mapId);
				} else { //没有转化数据的统计数据无需保留！！
					iterator.remove();
				}
			
			}
			//对于没有匹配到统计数据的转化数据，需要处理，首先将统计数据置0，让后按照排序插入
			for(Map<String, Object> trans : transDataMap.values()){
				trans.put(ReportConstants.CLKS, 0L);
				trans.put(ReportConstants.SRCHS, 0L);
				trans.put(ReportConstants.COST, 0.0D);
			}
			if (asc) {
				statData.addAll(0, transDataMap.values());//插入到队列头
			} else {
				statData.addAll(transDataMap.values());//插入到队列尾部  
			}
			
			return statData;
			
		} else {//以转化数据为序
			
			//为统计数据建立索引，key为group+site
			Map<Object,Map<String, Object>> stateDataMap = new HashMap<Object,Map<String, Object>>();
			for(Map<String, Object> stat : statData){
				String mapId = stat.get(ReportConstants.GROUP) + "_" + stat.get(siteKey);
				stateDataMap.put(mapId, stat);
			}
			//将统计数据merge到转化数据
			for (Map<String, Object> trans : transData) {
				String mapId = trans.get(ReportConstants.GROUP) + "_" + trans.get(siteKey);
				Map<String, Object> stat = stateDataMap.get(mapId);
				if (stat!=null) {
					trans.put(ReportConstants.CLKS, stat.get(ReportConstants.CLKS));
					trans.put(ReportConstants.SRCHS, stat.get(ReportConstants.SRCHS));
					trans.put(ReportConstants.COST, stat.get(ReportConstants.COST));
					if (stat.get(ReportConstants.CTR)!=null) {
						trans.put(ReportConstants.CTR, stat.get(ReportConstants.CTR));	
					}
					if (stat.get(ReportConstants.CPM)!=null) {
						trans.put(ReportConstants.CPM, stat.get(ReportConstants.CPM));	
					}
					if (stat.get(ReportConstants.ACP)!=null) {
						trans.put(ReportConstants.ACP, stat.get(ReportConstants.ACP));	
					}
					stateDataMap.remove(mapId);
				} else {
					trans.put(ReportConstants.CLKS, 0L);
					trans.put(ReportConstants.SRCHS, 0L);
					trans.put(ReportConstants.COST, 0.0D);
				}
			}

			return transData;
		}
	}
	
	private List getGroupSiteResultList(){

		//获取转化数据+统计数据
		List<Map<String, Object>> nonDbData = getGroupSiteNonDbData();
		
		//从转化数据中获取ID列表，groupId可能重复，因此需要去重然后从数据库获取
		List<Integer> idList = getIdList(nonDbData,ReportConstants.GROUP,Integer.class);
		Set<Integer> idSet = new HashSet<Integer>();
		idSet.addAll(idList);
		idList.clear();
		idList.addAll(idSet);

		
		//从数据库获取实体数据
		List dbData = getDbData(idList,ReportConstants.GROUP);

		//将数据库数据、统计数据、转化数据合并
		List resultList = mergeGroupSiteDbAndNonDbData(dbData, nonDbData);
		
		return resultList;
	}
	
	//收集数据，可用户推广计划、推广组、创意三个维度
	//idType为三层级Id的数据类型，分别取：Integer、Integer、Long
	//idKey分别为三层级转化数据Map中ID对应的key，分别取：ReportConstants.PLAN、ReportConstants.GROUP、ReportConstants.UNIT
	private List getResultList(Class idType,String idKey){

		
		//是按照统计字段排序吗？点击/展现/消费
		boolean isOrderByStatDataField = ReportConstants.isStatField(qp.getOrderBy());
		
		//是按照转化数据排序吗？直接转化/间接转化
		boolean isOrderByTransDataField = ReportConstants.isTransDataField(qp.getOrderBy());
		
		//获取转化数据
		List<Map<String, Object>> transData = getTransData(isOrderByTransDataField,idKey);
		
		//从转化数据中获取ID列表
		List idList = getIdList(transData,idKey,idType);
		
		//获取统计数据：点击/展现/消费
		List<Map<String, Object>> statData = getStatData(isOrderByStatDataField,idList,idKey);
		
		//将转化数据与统计数据合并
		List<Map<String, Object>> nonDbData = this.transReportFacade.mergeTransAndStatData(isOrderByStatDataField,transData,statData,idKey);
		
		//从数据库获取实体数据
		List dbData = getDbData(idList,idKey);
		
		if (idKey.equals(ReportConstants.UNIT)) {
			removeIfUnitDeleted(dbData);
		}
		
		//将数据库数据、统计数据、转化数据合并
		List resultList = fillMergeWithSlaveOrder(dbData, nonDbData, idKey);
		
		return resultList;
	}

	//对于创意，如果被删除了，就不要显示它的转化数据
	private void removeIfUnitDeleted(List<UnitViewItem> dbData) {
		if (CollectionUtils.isNotEmpty(dbData)) {
			Iterator<UnitViewItem> iterator = dbData.iterator();
			while(iterator.hasNext()) {
				UnitViewItem unit = iterator.next();
				if (unit.getState().equals(CproUnitConstant.UNIT_STATE_DELETE)) {
					iterator.remove();
				}
			}
		}
	}

	private String listTransDataInternal(Class idType,String idKey) {
		
		List resultList = getResultList(idType, idKey);
		
		//获取总页数
		int totalPage = super.getTotalPage(resultList.size());
		
		//计算汇总数据
		StatInfo sumData = calculateSumData(resultList);
		
		//是否前端缓存
		boolean cache = true;
		
		jsonObject.addData("list", resultList);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("totalNum", resultList.size());
		jsonObject.addData("cache", cache? ReportConstants.Boolean.TRUE : ReportConstants.Boolean.FALSE);
		jsonObject.addData("sum", sumData);
		
		return SUCCESS;
	}

	//计算推广计划/推广组/创意转化数据的汇总数据：点击、展现、消费
	private static <T extends StatInfo> StatInfo calculateSumData (List<T> infoData) {
		StatInfo sumData = new StatInfo();
		for (StatInfo item : infoData) {
			sumData.setClks(sumData.getClks() + item.getClks());
			sumData.setCost(sumData.getCost() + item.getCost());
			sumData.setSrchs(sumData.getSrchs() + item.getSrchs());
		}
		
		//生成扩展数据
		sumData.generateExtentionFields();
		
		return sumData;
	}
	
	//计算推广组分网站转化数据的汇总数据：点击、展现、消费
	private static StatInfo calculateGroupSiteSumData(List resultList){

		StatInfo sumData = new StatInfo();
		for (Object item : resultList) {
			Map<String,Object> map = (Map<String,Object>)item;
			Long clks = (Long)(map.get(ReportConstants.CLKS));
			Long srchs = (Long)(map.get(ReportConstants.SRCHS));
			Double cost = (Double) map.get(ReportConstants.COST);
			if (clks != null) {
				sumData.setClks(sumData.getClks() + clks);
			}
			if (cost != null) {
				sumData.setCost(sumData.getCost() + cost);	
			}
			if (srchs != null) {
				sumData.setSrchs(sumData.getSrchs() + srchs);				
			}
		}
		sumData.generateExtentionFields();
		
		return sumData;
		
	}
	

	private List getDbData(List idList, String idKey) {
		
		//只要ID为空就返回空，数据库数据用户最后取
		if(CollectionUtils.isEmpty(idList)){
			return new ArrayList(0);
		}

		QueryParameter qp = new QueryParameter();
		qp.setUserId(this.getUserId());
		qp.setIds(new ArrayList<Long>());
		for (Object id : idList) {
			Number objId = (Number) id;
			qp.getIds().add(objId.longValue());
		}

		List data = null;
		if (ReportConstants.PLAN.equals(idKey)) {
			data = reportCproPlanMgr.findPlanViewItemWithoutPagable(qp);
		} else if (ReportConstants.GROUP.equals(idKey)) {
			data = reportCproGroupMgr.findCproGroupViewItem(qp, false);
		} else if (ReportConstants.UNIT.equals(idKey)) {
			data = reportCproUnitMgr.findCproUnitViewItem(qp, false);
		} else {
			throw new RuntimeException("不认识的idKey:"+idKey);
		}
		
		return data;
	}

	private List<Map<String, Object>> getStatData(
			boolean isOrderByStatDataField, List idList, String idKey) {
		int orient = ReportConstants.SortOrder.ASC;
		if (QueryParameterConstant.SortOrder.SORTORDER_DESC.equalsIgnoreCase(qp.getOrder())){
			orient = ReportConstants.SortOrder.DES;
		}
		List<Map<String, Object>> statData = new ArrayList<Map<String, Object>>();
		List<PlanViewItem> planList = null;
		List<GroupViewItem> groupList = null;
		List<UnitViewItem> unitList = null;
		
		if (ReportConstants.PLAN.equals(idKey)) {//推广计划层级转化数据
			if (isOrderByStatDataField) {
				planList = planStatMgr.queryPlanData(userId, idList, from, to, 
						qp.getOrderBy(), orient, ReportConstants.TU_NONE);
			} else {
				planList = planStatMgr.queryPlanData(userId, idList, from, to, 
						null, 0, ReportConstants.TU_NONE);
			}
			if(CollectionUtils.isEmpty(planList)){
				return statData;
			}
			for(PlanViewItem item : planList){
				Map<String, Object> stat = new HashMap<String, Object>();
				stat.put(ReportConstants.SRCHS, item.getSrchs());
				stat.put(ReportConstants.CLKS, item.getClks());
				stat.put(ReportConstants.COST, item.getCost() * 100);
				stat.put(ReportConstants.CTR, item.getCtr());
				stat.put(ReportConstants.CPM, item.getCpm());
				stat.put(ReportConstants.ACP, item.getAcp());
				stat.put(ReportConstants.PLAN, item.getPlanId());
				statData.add(stat);
			}
		} else if (ReportConstants.GROUP.equals(idKey)) {//推广计划层级转化数据
			if (isOrderByStatDataField) {
				groupList = groupStatMgr.queryGroupData(userId, null, idList, from, to,
						qp.getOrderBy(), orient, ReportConstants.TU_NONE);
			} else {
				groupList = groupStatMgr.queryGroupData(userId, null, idList, from, to,
						null, 0, ReportConstants.TU_NONE);
			}
			if(CollectionUtils.isEmpty(groupList)){
				return statData;
			}
			for(GroupViewItem item : groupList){
				Map<String, Object> stat = new HashMap<String, Object>();
				stat.put(ReportConstants.SRCHS, item.getSrchs());
				stat.put(ReportConstants.CLKS, item.getClks());
				stat.put(ReportConstants.COST, item.getCost() * 100);
				stat.put(ReportConstants.CTR, item.getCtr());
				stat.put(ReportConstants.CPM, item.getCpm());
				stat.put(ReportConstants.ACP, item.getAcp());
				stat.put(ReportConstants.PLAN, item.getPlanId());
				stat.put(ReportConstants.GROUP, item.getGroupId());
				statData.add(stat);
			}
		} else if (ReportConstants.UNIT.equals(idKey)) {//推广计划层级转化数据
			if (isOrderByStatDataField) {
				unitList = unitStatMgr.queryUnitData(userId, null, null, idList, from, to,
						qp.getOrderBy(), orient, ReportConstants.TU_NONE);
			} else {
				unitList = unitStatMgr.queryUnitData(userId, null, null, idList, from, to,
						null, 0, ReportConstants.TU_NONE);
			}
			if(CollectionUtils.isEmpty(unitList)){
				return statData;
			}
			for(UnitViewItem item : unitList){
				Map<String, Object> stat = new HashMap<String, Object>();
				stat.put(ReportConstants.SRCHS, item.getSrchs());
				stat.put(ReportConstants.CLKS, item.getClks());
				stat.put(ReportConstants.COST, item.getCost() * 100);
				stat.put(ReportConstants.CTR, item.getCtr());
				stat.put(ReportConstants.CPM, item.getCpm());
				stat.put(ReportConstants.ACP, item.getAcp());
				stat.put(ReportConstants.PLAN, item.getPlanId());
				stat.put(ReportConstants.GROUP, item.getGroupId());
				stat.put(ReportConstants.UNIT, item.getUnitId());
				statData.add(stat);
			}
		} else {
			throw new RuntimeException("不认识的idKey:"+idKey);
		}
		
		return statData;
	}

	private List<Map<String, Object>> getTransData(boolean isOrderByTransDataField, String idKey){
		int orient = ReportConstants.SortOrder.ASC;
		if (QueryParameterConstant.SortOrder.SORTORDER_DESC.equalsIgnoreCase(qp.getOrder())){
			orient = ReportConstants.SortOrder.DES;
		}

		List<Map<String, Object>> transData = null;
		if (ReportConstants.PLAN.equals(idKey)) {//推广计划层级转化数据
			if (isOrderByTransDataField) {
				transData = this.transReportFacade.queryPlanData(super.getUserId(), transSiteIds,transTargetIds, null, 
						from, to, qp.getOrderBy(), orient,
						ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT,0,0);
			} else {
				transData = this.transReportFacade.queryPlanData(super.getUserId(), transSiteIds,transTargetIds, null, 
						from, to, null, 0,
						ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT,0,0);
			}
		} else if (ReportConstants.GROUP.equals(idKey)) {//推广组层级转化数据
			if (isOrderByTransDataField) {
				transData = this.transReportFacade.queryGroupData(super.getUserId(), transSiteIds,transTargetIds, null, null,
						from, to, qp.getOrderBy(), orient,
						ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT,0,0);
			} else {
				transData = this.transReportFacade.queryGroupData(super.getUserId(), transSiteIds,transTargetIds, null, null,
						from, to, null, 0,
						ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT,0,0);
			}
		} else if (ReportConstants.UNIT.equals(idKey)) {//创意层级转化数据
			if (isOrderByTransDataField) {
				transData = this.transReportFacade.queryUnitData(super.getUserId(), transSiteIds,transTargetIds, null, null, null,
						from, to, qp.getOrderBy(), orient,
						ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT,0,0);
			} else {
				transData = this.transReportFacade.queryUnitData(super.getUserId(), transSiteIds,transTargetIds, null, null, null,
						from, to, null, 0,
						ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT,0,0);
			}
		} else {
			throw new RuntimeException("不认识的idKey:"+idKey);
		}
		
		
		return transData;
	}

	private static <T extends Number> List<T> getIdList(List<Map<String, Object>> transData,String idKey,Class<T> idType) {

		List<T> idList = new ArrayList<T>();
		for (Map<String, Object> item : transData) {
			idList.add(idType.cast(item.get(idKey)));
		}
		return idList;

	}

	/**
	 * @return the siteId
	 */
	public Long getSiteId() {
		return siteId;
	}

	/**
	 * @param siteId the siteId to set
	 */
	public void setSiteId(Long transSiteId) {
		this.siteId = transSiteId;
	}

	/**
	 * @return the transId
	 */
	public Long getTransId() {
		return transId;
	}

	/**
	 * @param transId the transId to set
	 */
	public void setTransId(Long transTargetId) {
		this.transId = transTargetId;
	}

	/**
	 * @return the transSearchRange
	 */
	public int getTransSearchRange() {
		return transSearchRange;
	}

	/**
	 * @param transSearchRange the transSearchRange to set
	 */
	public void setTransSearchRange(int transDataDimension) {
		this.transSearchRange = transDataDimension;
	}


	/**
	 * @param reportCproPlanMgr the reportCproPlanMgr to set
	 */
	public void setReportCproPlanMgr(ReportCproPlanMgr reportCproPlanMgr) {
		this.reportCproPlanMgr = reportCproPlanMgr;
	}

	/**
	 * @param reportCproGroupMgr the reportCproGroupMgr to set
	 */
	public void setReportCproGroupMgr(ReportCproGroupMgr reportCproGroupMgr) {
		this.reportCproGroupMgr = reportCproGroupMgr;
	}

	/**
	 * @param reportCproUnitMgr the reportCproUnitMgr to set
	 */
	public void setReportCproUnitMgr(ReportCproUnitMgr reportCproUnitMgr) {
		this.reportCproUnitMgr = reportCproUnitMgr;
	}
	
}
