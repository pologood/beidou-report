package com.baidu.beidou.report.web.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.cprogroup.bo.CproGroup;
import com.baidu.beidou.cprogroup.bo.GroupSiteFilter;
import com.baidu.beidou.cprogroup.bo.GroupSitePrice;
import com.baidu.beidou.cprogroup.bo.GroupTradePrice;
import com.baidu.beidou.cprogroup.constant.CproGroupConstant;
import com.baidu.beidou.cprogroup.constant.UnionSiteCache;
import com.baidu.beidou.cprogroup.service.CproGroupMgr;
import com.baidu.beidou.cprogroup.service.GroupSiteConfigMgr;
import com.baidu.beidou.cprogroup.vo.BDSiteInfo;
import com.baidu.beidou.cprogroup.vo.BDSiteLiteInfo;
import com.baidu.beidou.cprogroup.vo.TcSiteInfo;
import com.baidu.beidou.cprogroup.vo.TradeInfo;
import com.baidu.beidou.cproplan.bo.CproPlan;
import com.baidu.beidou.cproplan.service.CproPlanMgr;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.olap.constant.Constants;
import com.baidu.beidou.olap.service.SiteStatService;
import com.baidu.beidou.olap.service.TradeStatService;
import com.baidu.beidou.olap.vo.MainSiteViewItem;
import com.baidu.beidou.olap.vo.SiteStatInfo;
import com.baidu.beidou.olap.vo.SiteViewItem;
import com.baidu.beidou.olap.vo.SubSiteViewItem;
import com.baidu.beidou.olap.vo.TradeViewItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.ReportConstants.DorisLevelType;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.dao.vo.GroupQueryParameter;
import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.report.service.ReportCacheService;
import com.baidu.beidou.report.service.ReportCproGroupMgr;
import com.baidu.beidou.report.util.ReportUtil;
import com.baidu.beidou.report.vo.AbstractReportVo;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.StatInfo;
import com.baidu.beidou.report.vo.SumData;
import com.baidu.beidou.report.vo.comparator.SiteComparator;
import com.baidu.beidou.report.vo.comparator.TradeComparator;
import com.baidu.beidou.report.vo.group.ExtGroupViewItem;
import com.baidu.beidou.report.vo.site.SiteFlashCache;
import com.baidu.beidou.report.vo.site.SiteReportVo;
import com.baidu.beidou.report.vo.site.SiteSumData;
import com.baidu.beidou.report.vo.site.TradeAssistant;
import com.baidu.beidou.report.vo.site.TradeReportVo;
import com.baidu.beidou.report.vo.site.TradeSumData;
import com.baidu.beidou.stat.driver.bo.Constant;
import com.baidu.beidou.stat.driver.bo.Constant.DorisDataType;
import com.baidu.beidou.tool.util.TransReportHelper;
import com.baidu.beidou.tool.vo.TempSitesAndTrans;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouConstant;
import com.baidu.beidou.util.TokenUtil;
import com.baidu.beidou.util.memcache.BeidouCacheInstance;
import com.baidu.unbiz.olap.util.ReportUtils;

/** 
 * 1，自选网站列表
	a，对于在cprogroupinfo.sitelist中，且在站点全库中的有效网站，就会在该列表中显示
	b，对于在该列表中显示站点，一定可以单独出价，且一定可以停止投放
	c，对于在该列表中且被用户手动过滤的网站，url后面加上(已过滤)

	2，有展现网站列表
	a，对于曾经有展现的网站，就会在该列表中显示
	b，对于满足1.a或者未过滤的网站，允许其停止投放，否则操作列显示-
	c，对于在站点全库中的有效网站，只要满足(推广组全网投放|此网站在cprogroupinfo.sitelist中|此网站所属行业在cprogroupinfo.tradelist中)，就可以单独出价，否则出价列置灰
 */

public class ListGroupSiteAction extends BeidouReportActionSupport{
	private static final long serialVersionUID = 237915748428520413L;
	private static final Log LOG = LogFactory.getLog(ListGroupSiteAction.class);

	@Resource(name="siteStatServiceImpl")
	SiteStatService siteStatMgr;
	
	@Resource(name="tradeStatServiceImpl")
	TradeStatService tradeStatMgr;
	
	private static final String LOG_PREFIX_CHOSENSITE = "处理自选网站列表-";
	private static final String LOG_PREFIX_SHOWNSITE = "处理有展现网站列表-";
	private static final String LOG_PREFIX_CHOSENTRADE = "处理自选行业列表-";
	private long start = 0; //用于记录action的开始时间
	private long last = 0; //用于记录action的处理的上次记录时间
	private long now = 0; //用于记录action的处理的当前时间
	
	private static final int DOWNLOAD_SITE_HEADER_COL = 18;
	private static final int DOWNLOAD_TRADE_HEADER_COL = 17;
	
	private static final String OPERATION_MOD_PRICE = "price";
	private static final String OPERATION_STOP_CHARGE = "stop";
	
	private static final int TOKEN_EXPIRE = 60;
	
	private InputStream inputStream = null;
	private String fileName = "site.csv";
	private long fileSize = 0;
	private Integer tradeId = null;
	
	private String operation = null;
	
	//仅自己使用
	private List<Integer> planIds = null;
	private List<Integer> groupIds = null;
	private List<Integer> firstTradeIds = new ArrayList<Integer>();
	private List<Integer> secondTradeIds = new ArrayList<Integer>();
	//一定不需要排序
	private static final String orderBy = null;
	private int orient = 0;
	protected String queryKeyword = null;
	private boolean needStatFilter = false;
	
	//在推广组层级，且全网投放
	private boolean groupAllSite = false;
	
	private SiteFlashCache siteFlashCache = new SiteFlashCache();
	
	//是否需要重新向doris发起请求，目前仅针对baidu.com
	private boolean needQuerySecondDomain = false;
	
	private Map<Integer, Set<String>> siteFilterMap = null;
	private Map<Integer, Map<Integer, Integer>> sitePriceMap = null;
	private Map<Integer, Map<Integer, Integer>> tradePriceMap = null;
	
	private Map<Integer, ExtGroupViewItem> viewMapping = null;	
	
	//以下三个数据不大，作为了成员变量，避免传递
	private SiteSumData siteSumData = null;
	private TradeSumData tradeSumData = null;
	private int totalPage = 0;
	
	private GroupSiteConfigMgr siteConfigMgr;	
	private ReportCproGroupMgr reportCproGroupMgr;
	private CproPlanMgr cproPlanMgr;
	private CproGroupMgr cproGroupMgr;
	
	private ReportCacheService cacheService;

	@Override
	protected void initParameter() {
		super.initParameter();
		queryKeyword = StringUtils.isEmpty(qp.getKeyword())? 
				null : qp.getKeyword().toLowerCase();
		needStatFilter = qp.hasStatField4Filter();
	}
	
	@Override
	protected void initStateMapping() {
		//投放网络列表不需要状态筛选
	}

	
	private void checkIfGroupCountExceed(){
		int count = 0;
		if(QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(qp.getLevel())){
			count = reportCproGroupMgr.countCproGroupByUserId(userId);
		}else if(QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(qp.getLevel())){
			count = reportCproGroupMgr.countCproGroupByPlanId(qp.getPlanId());
		}
		if(count > ReportWebConstants.MAX_GROUP_COUNT_IN_SITEVIEW){			
			throw new BusinessException(this.getText("error.report.site.exceed"));
		}
	}
	
	private boolean checkIfNoSiteChosen(){
		//判断当前层级的推广组是否都是自选且没设投放网络
		if(QueryParameterConstant.LEVEL.LEVEL_GROUP.equals(qp.getLevel())){
			int ret = reportCproGroupMgr.getGroupSiteStatus(qp.getGroupId());
			if(ret == ReportCproGroupMgr.GROUP_NOSITE){
				return true;
			}else if(ret == ReportCproGroupMgr.GROUP_ALLSITE){
				this.groupAllSite = true;
			}
		}
		return false;
	}
	
	private void initLevelQueryParam(){
		if(ReportConstants.isStatField(qp.getOrderBy())){
			orient = ReportConstants.SortOrder.ASC;
			if (QueryParameterConstant.SortOrder.SORTORDER_DESC.equalsIgnoreCase(qp.getOrder())){
				orient = ReportConstants.SortOrder.DES;
			}
		}		
		if(QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(qp.getLevel())){
			return;
		}
		if(QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(qp.getLevel())){
			this.planIds = new ArrayList<Integer>(1);
			this.planIds.add(qp.getPlanId());
			return;
		}
		if(QueryParameterConstant.LEVEL.LEVEL_GROUP.equals(qp.getLevel())){
			this.groupIds = new ArrayList<Integer>(1);
			this.groupIds.add(qp.getGroupId());
		}
	}

	private void initLevelQueryParamWithTrade(){
		initLevelQueryParam();
        if(qp.getFTradeId() != null) {
            firstTradeIds.add(qp.getFTradeId());
        }
        if(tradeId != null) {
        	firstTradeIds.add(tradeId);
        }

        if(qp.getSTradeId() != null) {
            secondTradeIds.add(qp.getSTradeId());
        }
	}
	
	private boolean initTradeQueryParam(){
		if(tradeId == null || tradeId == 0){
			throw new BusinessException(this.getText("error.report.trade.null"));
		}
		TradeInfo tradeInfo = UnionSiteCache.tradeInfoCache.getTradeInfoById(tradeId);
		if(tradeInfo == null){
			throw new BusinessException(this.getText("error.report.trade.notexist"));
		}
		//行业分网站列表有一个特殊的地方，就是虽然前端的Level可以是任何层级，后端处理的时候，必须当成group层级处理
		if(super.qp == null || super.qp.getTab() != QueryParameterConstant.TAB.TAB_SITE_CHOSENTRADE_SITE){
			return false;
		}
		super.qp.setLevel(QueryParameterConstant.LEVEL.LEVEL_GROUP);
		//此处的特殊处理需要注意
		if(!initAllReport()){
			return false; 
		}
		//在MRD上的需求，这里只显示有展现网站的列表因此这块与shownSite那块逻辑非常相似
		//因此其实就是为其设上Trade就行了
		if(tradeInfo.getParentid() == 0){
			//一级行业
			qp.setFTradeId(tradeId);
			qp.setSTradeId(null);
		}else{
			//二级行业
			qp.setFTradeId(tradeInfo.getParentid());
			qp.setSTradeId(tradeId);
		}
		qp.setNeedTradeFilter(true);
		return true;
	}
	
	private boolean initAllReport(){
		//初始化一下参数
		initParameter();
		this.siteSumData = new SiteSumData();
		this.tradeSumData = new TradeSumData();
		this.checkIfGroupCountExceed();
		return true;
	}
	
	private void generateGroupInfo(Set<Integer> groupIdSet){
		GroupQueryParameter queryParam = new GroupQueryParameter();
		queryParam.setUserId(userId);
		List<Long> ids = new ArrayList<Long>(groupIdSet.size());
		//由于查询统计用long，这里进行一下transfer
		for(Integer id : groupIdSet){
			ids.add(id.longValue());
		}
		queryParam.setIds(ids);
		List<ExtGroupViewItem> viewItems = reportCproGroupMgr.findExtCproGroupReportInfo(queryParam);
		viewMapping = new HashMap<Integer, ExtGroupViewItem>(viewItems.size());
		for(ExtGroupViewItem tmp : viewItems){
			viewMapping.put(tmp.getGroupId(), tmp);
		}
	}
	
	private void generateAddedInfo(boolean includeSiteFilter,
			boolean includeSitePrice, boolean includeTradePrice){
		if(includeSitePrice){
			List<GroupSitePrice> sitePrice = siteConfigMgr.findAllSitePrice(userId, qp.getPlanId(), qp.getGroupId());
			sitePriceMap = new HashMap<Integer, Map<Integer, Integer>>();
			for(GroupSitePrice gs : sitePrice){
				Integer groupId = gs.getGroupid();
				Integer siteId = gs.getSiteid();
				Map<Integer, Integer> tmp = sitePriceMap.get(groupId);
				if(tmp == null){
					tmp = new HashMap<Integer, Integer>();
					sitePriceMap.put(groupId, tmp);
				}
				tmp.put(siteId, gs.getPrice());
			}
		}
		if(includeTradePrice){
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
		}
		if(includeSiteFilter){
			//获取全部的网站过滤，以便于组装是否能够停止投放操作
			List<GroupSiteFilter> siteFilter = siteConfigMgr.findAllSiteFilter(userId, qp.getPlanId(), qp.getGroupId());
			siteFilterMap = new HashMap<Integer, Set<String>>();
			for(GroupSiteFilter filter : siteFilter){
				Integer groupId = filter.getGroupid();
				Set<String> tmp = siteFilterMap.get(groupId);
				if(tmp == null){
					tmp = new HashSet<String>();
					siteFilterMap.put(groupId, tmp);
				}
				tmp.add(filter.getSite());
			}	
		}
	}
	
	private GroupQueryParameter initQueryParameter(){
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
	
	private void buildNoSiteResult(){
		this.buildEmptyResult();
		jsonObject.addData("hasSite", ReportWebConstants.HAVE_NO_SITE);
		jsonObject.addData("cache", ReportConstants.Boolean.TRUE);
	}
	
	private void buildTokenJson(String token){
		jsonObject.addData("token", token);
	}
	
	private void buildEmptyResult(){
		jsonObject.addData("list", Collections.emptyList());
		jsonObject.addData("totalPage", 0);
		jsonObject.addData("cache", ReportConstants.Boolean.TRUE);
		jsonObject.addData("hasSite", ReportWebConstants.HAVE_SITE);
		jsonObject.addData("groupAllSite", this.groupAllSite);
		if(qp.getTab() == QueryParameterConstant.TAB.TAB_SITE_CHOSENTRADE){
			jsonObject.addData("sum", new TradeSumData(0, 0, ReportConstants.Boolean.TRUE));
		}else{
			jsonObject.addData("sum", new SiteSumData(0, ReportConstants.Boolean.TRUE));
		}
	}
	
	private void buildJsonResult(List<? extends SiteStatInfo> list, int totalPage, SumData sumData){		
		jsonObject.addData("list", list);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("hasSite", ReportWebConstants.HAVE_SITE);
		jsonObject.addData("cache", sumData.getNeedFrontSort());
		jsonObject.addData("sum", sumData);
		jsonObject.addData("groupAllSite", this.groupAllSite);
		//处理当日和昨日不入库的uv和转化数据
		this.reportFacade.postHandleTransAndUvData(userId, from, to, list, sumData);	
		
	}
	
	private boolean containsSecondDomainSite(String siteUrl){
		return ReportWebConstants.showSecondDomainSiteList.contains(siteUrl);
	}
	
	private void appendGroupInfo(SiteStatInfo vo, ExtGroupViewItem viewItem){
		vo.setPlanId(viewItem.getPlanId());
		//groupId可能已经被set过一遍了，不过没关系，这里肯定相等，为了统计就这行写了
		vo.setGroupId(viewItem.getGroupId());
		vo.setGroupName(viewItem.getGroupName());
		vo.setPlanName(viewItem.getPlanName());
		vo.setGroupStateFlag(viewItem.getViewStateOrder());
		vo.setViewState(viewItem.getViewState());		
	}
	
	private List<TradeViewItem> pagerTradeList (List<TradeAssistant> infoData) {
		int page = 0;
		int pageSize = ReportConstants.PAGE_SIZE;
		if(qp.getPage() != null && qp.getPage() > 0) {
			page = qp.getPage();
		}
		if(qp.getPageSize() != null && qp.getPageSize() > 0) {
			pageSize = qp.getPageSize();
		}
		
		infoData = ReportWebConstants.subListinPage(infoData, page, pageSize);
		List<TradeViewItem> result = new ArrayList<TradeViewItem>();
		for(TradeAssistant ass : infoData){
			result.addAll(ass.getTradeViewItem());
		}
		return result;
	}
	
	/**
	 * 判断这个网站是否会被行业过滤掉
	 * @param siteInfo
	 * @return
	 */
	private boolean filterByTrade(BDSiteInfo siteInfo){
		if(qp.isNeedTradeFilter()){
			//如果前端查询行业			
			if(siteInfo == null){				
				//这个网站不在unionsite中，相当于没有一二级行业，过滤掉
				return true;
			}
			if(qp.getFTradeId() != null && qp.getFTradeId() !=0 
					&& qp.getFTradeId() != siteInfo.getFirsttradeid()){
				//一级行业不match，过滤掉
				return true;
			}
			if(qp.getSTradeId() != null && qp.getSTradeId() !=0 
					&& qp.getSTradeId() != siteInfo.getSecondtradeid()){
				//二级行业不match，过滤掉
				return true;
			}				
		}
		return false;
	}
	
	public String downloadShownSiteList(){
		SiteReportVo reportVo = new SiteReportVo(qp.getLevel());
		if(!initAllReport()){
			return ERROR; 
		}

		boolean showTransData = this.transReportFacade.isTransToolSigned(userId, false);
		boolean transDataValid = this.transReportFacade.needToFetchTransData(userId, from, to, false);
		List<SiteViewItem> list = generateShownSiteList(false,showTransData);
		
		//处理当日和昨日不入库的uv和转化数据
		this.reportFacade.postHandleTransAndUvData(userId, from, to, list, siteSumData);
		
//		if (super.isOnlyToday(getFrom(), getTo())) {
//		    super.clearNotRealtimeStat(list, siteSumData);
//		}
		
		reportVo.setDetails(list);
		reportVo.setShowTransData(showTransData);
		reportVo.setTransDataValid(transDataValid);
		reportVo.setSumData(siteSumData);
		ReportAccountInfo account = generateAccountInfo();
		reportVo.setAccountInfo(account);
		reportVo.setHeaders(this.generateHeader(showTransData));
		reportVo.setSummaryText(this.getText("download.summary.site", 
				new String[]{String.valueOf(siteSumData.getCount())}));
		boolean success = generateReport(reportVo);
		return success ? SUCCESS : ERROR;
	}
	
	public String downloadChosenSiteList(){
		SiteReportVo reportVo = new SiteReportVo(qp.getLevel());
		if(!initAllReport()){
			return ERROR; 
		}
		boolean showTransData = this.transReportFacade.isTransToolSigned(userId, false);
		boolean transDataValid = this.transReportFacade.needToFetchTransData(userId, from, to, false);
		List<SiteViewItem> list = generateChosenSiteList(false,showTransData);
		
		//处理当日和昨日不入库的uv和转化数据
		this.reportFacade.postHandleTransAndUvData(userId, from, to, list, siteSumData);
		
//		if (super.isOnlyToday(getFrom(), getTo())) {
//		    super.clearNotRealtimeStat(list, siteSumData);
//		}

		reportVo.setDetails(list);
		reportVo.setTransDataValid(transDataValid);
		reportVo.setShowTransData(showTransData);
		reportVo.setSumData(siteSumData);
		ReportAccountInfo account = generateAccountInfo();
		reportVo.setAccountInfo(account);
		reportVo.setHeaders(this.generateHeader(showTransData));
		reportVo.setSummaryText(this.getText("download.summary.site", 
				new String[]{String.valueOf(siteSumData.getCount())}));
		boolean success = generateReport(reportVo);
		return success ? SUCCESS : ERROR;
	}
	
	public String downloadChosenTradeList(){
		TradeReportVo reportVo = new TradeReportVo(qp.getLevel());
		if(!initAllReport()){
			return ERROR; 
		}
		List<TradeAssistant> list = generateTradeSiteList(false);
		
		//处理当日和昨日不入库的uv和转化数据 add by wangchongjie since 2012.11.03
		for (TradeAssistant items : list ){
			this.reportFacade.postHandleTransAndUvData(userId, from, to, items.getTradeViewItem(), tradeSumData);
		}	
		
		//3.3、判断是否需要转化数据
		boolean showTransData = this.transReportFacade.isTransToolSigned(userId, false);
		boolean transDataValid = this.transReportFacade.needToFetchTransData(userId, from, to, false);
		
		//3.4、填充数据
		reportVo.setShowTransData(showTransData);
		reportVo.setTransDataValid(transDataValid);
		reportVo.setDetails(list);
		reportVo.setAccountInfo(this.generateAccountInfo());
		reportVo.setHeaders(this.generateHeader(showTransData));
		reportVo.setSumData(tradeSumData);
		reportVo.setSummaryText(this.getText("download.summary.trade", 
				new String[]{String.valueOf(tradeSumData.getFirstTradeCount()), 
						String.valueOf(tradeSumData.getSecondTradeCount())}));
		boolean success = generateReport(reportVo);
		return success ? SUCCESS : ERROR;
	}
	
	public String downloadTradeSiteList(){
		
		initParameter();//注意，这个方法将被调用两次
						//不是因为蠢才这样做的，是因为要解决一个很无奈的BUG：
						//generateShownSiteList()方法中改变了qp.level
						//所有当level!='group'时下载数据有问题
						//解决之道是将reportVo的初始化提前，将reportVo提前的前提是要先执行initParameter()方法
						//by hejinggen@baidu.com
		
		SiteReportVo reportVo = new SiteReportVo(qp.getLevel());
		boolean showTransData = this.transReportFacade.isTransToolSigned(userId, false);
		boolean transDataValid = this.transReportFacade.needToFetchTransData(userId, from, to, false);
		reportVo.setShowTransData(showTransData);
		reportVo.setTransDataValid(transDataValid);
		ReportAccountInfo account = generateAccountInfo();
		reportVo.setAccountInfo(account);
		reportVo.setHeaders(this.generateHeader(showTransData));

		if(!initTradeQueryParam()){
			return ERROR;
		}
		List<SiteViewItem> list = generateShownSiteList(false,showTransData);
		
		//处理当日和昨日不入库的uv和转化数据
		this.reportFacade.postHandleTransAndUvData(userId, from, to, list, siteSumData);
		
		reportVo.setDetails(list);
		reportVo.setSumData(siteSumData);
		reportVo.setSummaryText(this.getText("download.summary.site",new String[]{String.valueOf(siteSumData.getCount())}));

		boolean success = generateReport(reportVo);
		return success ? SUCCESS : ERROR;
	}
	
	/**
	 * filterStatByMainSite:以mainSites列表为基准，过滤掉stats中不在mainSites的统计数据
	 * 说明：该功能主要是由于mainSites列表太大，不方便通过Doris过滤，而需要取出所有数据后在内存中自己过滤。
	 * @param 待统计的一级域名列表
	 * @param 一级域名统计值
	*/
	protected List<Map<String, Object>> filterStatByMainSite(Set<String> mainSites, List<Map<String, Object>> stats ) {
		if (CollectionUtils.isEmpty(mainSites) || CollectionUtils.isEmpty(stats)) {
			return new ArrayList<Map<String,Object>>();
		}
		List<Map<String, Object>> result = new ArrayList<Map<String,Object>>();
		
		for (Map<String, Object> stat : stats ) {
			if (mainSites.contains(stat.get(ReportConstants.MAINSITE))) {
				result.add(stat);
			}
		}
		
		return result;
	}
	
	public String ajaxListTradeSite(){
		if(!initTradeQueryParam()){
			return SUCCESS;
		}
		return this.ajaxListShownSite();
	}
	
	public String ajaxTradeSiteToken(){
		if(!initTradeQueryParam()){
			return SUCCESS;
		}
		return ajaxShownSiteToken();
	}
	
	public String ajaxListChosenTrade(){
		start = System.currentTimeMillis();
		last = start;
		LOG.debug(LOG_PREFIX_CHOSENTRADE + "开始于:" + start);
		if(!initAllReport()){
			return SUCCESS; 
		}	
		if(this.checkIfNoSiteChosen()){
			this.buildNoSiteResult();
			return SUCCESS;
		}
//        if (super.isMixTodayAndBefore(this.getFrom(), this.getTo())) {
//            jsonObject.setStatus(BeidouConstant.JSON_OPERATE_FAILED);
//            jsonObject.setGlobalMsg(getText("mixed.report.not.allowed"));
//            return SUCCESS;
//        }

		now = System.currentTimeMillis();
		LOG.debug(LOG_PREFIX_CHOSENTRADE + "初始化查询耗时:" + (now - last));
		last = now;
		List<TradeAssistant> list = generateTradeSiteList(false);
		if(CollectionUtils.isEmpty(list)){
			buildEmptyResult();
		}else{
			sortTrade(list);
			now = System.currentTimeMillis();
			LOG.debug(LOG_PREFIX_CHOSENTRADE + "排序" + list.size() + "个行业耗时:" + (now - last));
			last = now;
			//获取分页数据：如果需要分页的话
			List<TradeViewItem> result = this.pagerTradeList(list);
			now = System.currentTimeMillis();
			LOG.debug(LOG_PREFIX_CHOSENTRADE + "分页" + list.size() + "个行业耗时:" + (now - last));
			last = now;

//			if (super.isOnlyToday(getFrom(), getTo())) {
//			    super.clearNotRealtimeStat(result, tradeSumData);
//			}

			//组装数据
			buildJsonResult(result, totalPage, tradeSumData);
		}		
		LOG.debug(LOG_PREFIX_CHOSENTRADE + "总耗时:" + (System.currentTimeMillis() - start));
		return SUCCESS;
	}	
	
	public String ajaxChosenTradeToken(){
		if(!initAllReport()){
			return SUCCESS; 
		}	
		if(this.checkIfNoSiteChosen()){
			this.buildNoSiteResult();
			return SUCCESS;
		}
		List<TradeAssistant> list = generateTradeSiteList(true);
		if(CollectionUtils.isEmpty(list)){
			buildEmptyResult();
		}else{
			HashMap<Integer, List<Integer>> result = new HashMap<Integer, List<Integer>>();
			for(TradeAssistant assistant : list){
				TradeViewItem tradeView = assistant.getSelfTrade();
				List<Integer> tmp = result.get(tradeView.getGroupId());
				if(tmp == null){
					tmp = new ArrayList<Integer>();
					result.put(tradeView.getGroupId(), tmp);
				}
				tmp.add(assistant.getSelfTradeId());
			}
			String token = TokenUtil.generateToken();
			BeidouCacheInstance.getInstance().memcacheRandomSet(token, result, TOKEN_EXPIRE);
			buildTokenJson(token);
		}
		return SUCCESS;
	}

	
	public String ajaxListChosenSite(){
		start = System.currentTimeMillis();
		last = start;
		LOG.debug(LOG_PREFIX_CHOSENSITE + "开始于:" + start);
		if(!initAllReport()){
			return SUCCESS; 
		}		
		if(this.checkIfNoSiteChosen()){
			this.buildNoSiteResult();
			return SUCCESS;
		}
//        if (super.isMixTodayAndBefore(this.getFrom(), this.getTo())) {
//            jsonObject.setStatus(BeidouConstant.JSON_OPERATE_FAILED);
//            jsonObject.setGlobalMsg(getText("mixed.report.not.allowed"));
//            return SUCCESS;
//        }

		now = System.currentTimeMillis();
		LOG.debug(LOG_PREFIX_CHOSENSITE + "初始化查询及检查耗时:" + (now - last));
		last = now;
		boolean showTransData = this.transReportFacade.isTransToolSigned(userId, false);
		List<SiteViewItem> list = generateChosenSiteList(false,showTransData);
		if(CollectionUtils.isEmpty(list)){
			buildEmptyResult();
		}else{
			sortSite(list);
			now = System.currentTimeMillis();
			LOG.debug(LOG_PREFIX_CHOSENSITE + "排序" + list.size() + "个网站信息耗时:" + (now - last));
			last = now;
			//获取分页数据：如果需要分页的话
			list = pagerList(list);
			now = System.currentTimeMillis();
			LOG.debug(LOG_PREFIX_CHOSENSITE + "分页" + list.size() + "个网站信息耗时:" + (now - last));
			last = now;

//			if (super.isOnlyToday(getFrom(), getTo())) {
//			    super.clearNotRealtimeStat(list, siteSumData);
//			}

			//组装数据
			buildJsonResult(list, totalPage, siteSumData);
		}		
		LOG.debug(LOG_PREFIX_CHOSENSITE + "总耗时:" + (System.currentTimeMillis() - start));
		return SUCCESS;
	}
	
	public String ajaxChosenSiteToken(){
		if(!initAllReport()){
			return SUCCESS; 
		}		
		if(this.checkIfNoSiteChosen()){
			this.buildNoSiteResult();
			return SUCCESS;
		}
		List<SiteViewItem> list = generateChosenSiteList(true,false);
		if(CollectionUtils.isEmpty(list)){
			buildEmptyResult();
		}else{
			generateSiteToken(list);
		}	
		return SUCCESS;
	}
	
	private void generateSiteToken(List<SiteViewItem> list){
		if(OPERATION_MOD_PRICE.equals(operation)){
			//如果是处理price，必须有siteId的才行
			HashMap<Integer, List<Integer>> result = new HashMap<Integer, List<Integer>>();
			for(SiteViewItem item : list){
				if(item.getSiteId() == null || item.getSiteId() == 0
						|| item.getFiltered() || !item.getSiteValid()){
					continue;
				}
				List<Integer> tmp = result.get(item.getGroupId());
				if(tmp == null){
					tmp = new ArrayList<Integer>();
					result.put(item.getGroupId(), tmp);
				}
				tmp.add(item.getSiteId());				
			}	
			String token = TokenUtil.generateToken();
			BeidouCacheInstance.getInstance().memcacheRandomSet(token, result, TOKEN_EXPIRE);
			buildTokenJson(token);
		}else if(OPERATION_STOP_CHARGE.equals(operation)){
			HashMap<Integer, List<String>> result = new HashMap<Integer, List<String>>();
			for(SiteViewItem item : list){
				List<String> tmp = result.get(item.getGroupId());
				if(tmp == null){
					tmp = new ArrayList<String>();
					result.put(item.getGroupId(), tmp);
				}
				tmp.add(item.getSiteUrl());				
			}	
			String token = TokenUtil.generateToken();
			BeidouCacheInstance.getInstance().memcacheRandomSet(token, result, TOKEN_EXPIRE);
			buildTokenJson(token);
		}else{
			throw new BusinessException("invalid operation");
		}
	}
	
	
	public String ajaxListShownSite(){		
		start = System.currentTimeMillis();
		last = start;
		LOG.debug(LOG_PREFIX_SHOWNSITE + "开始于:" + start);
		if(!initAllReport()){
			return SUCCESS; 
		}
		if(this.checkIfNoSiteChosen()){
			this.buildNoSiteResult();
			return SUCCESS;
		}

//		if (super.isMixTodayAndBefore(this.getFrom(), this.getTo())) {
//            jsonObject.setStatus(BeidouConstant.JSON_OPERATE_FAILED);
//            jsonObject.setGlobalMsg(getText("mixed.report.not.allowed"));
//            return SUCCESS;
//        }
		
		now = System.currentTimeMillis();
        if (LOG.isDebugEnabled()) {
            LOG.debug(LOG_PREFIX_SHOWNSITE + "始初化查询耗时:" + (now - last));
        }
        last = now;
		boolean showTransData = this.transReportFacade.isTransToolSigned(userId, false);
		List<SiteViewItem> list = generateShownSiteList(false,showTransData);
		if(CollectionUtils.isEmpty(list)){
			buildEmptyResult();
		}else{
			sortSite(list);
			now = System.currentTimeMillis();
			if (LOG.isDebugEnabled()) {
	            LOG.debug(LOG_PREFIX_SHOWNSITE + "排序" + list.size() + "个网站信息耗时:" + (now - last));
			}
			last = now;
			//获取分页数据：如果需要分页的话
			list = pagerList(list);
			now = System.currentTimeMillis();
            if (LOG.isDebugEnabled()) {
			LOG.debug(LOG_PREFIX_SHOWNSITE + "分页" + list.size() + "个网站信息耗时:" + (now - last));
            }
			last = now;
			
//			if (super.isOnlyToday(getFrom(), getTo())) {
//			    super.clearNotRealtimeStat(list, siteSumData);
//			}

			//组装数据
			buildJsonResult(list, totalPage, siteSumData);
		}
		LOG.debug(LOG_PREFIX_SHOWNSITE + "总耗时:" + (System.currentTimeMillis() - start));
		return SUCCESS;
	}
	
	public String ajaxShownSiteToken(){
		if(!initAllReport()){
			return SUCCESS; 
		}
		if(this.checkIfNoSiteChosen()){
			this.buildNoSiteResult();
			return SUCCESS;
		}
		List<SiteViewItem> list = generateShownSiteList(false,false);
		if(CollectionUtils.isEmpty(list)){
			buildEmptyResult();
		}else{
			generateSiteToken(list);
		}
		return SUCCESS;
	}
	
	private void sortTrade(List<TradeAssistant> list){
		Collections.sort(list, new TradeComparator(qp));
	}
	
	private void sortSite(List<SiteViewItem> list){
		if(siteSumData.getNeedFrontSort() == ReportConstants.Boolean.FALSE){
			Collections.sort(list, new SiteComparator(qp));
		}
	}
	
	private List<TradeAssistant> generateTradeSiteList(boolean forFlash){
		List<TradeAssistant> list = null;
		//1、先根据当前层级取得全部推广组的设置，并获取推广组相关信息
		GroupQueryParameter queryParam = initQueryParameter();
		List<ExtGroupViewItem> viewItems = reportCproGroupMgr.findExtCproGroupReportInfo(queryParam);
		now = System.currentTimeMillis();
		LOG.debug(LOG_PREFIX_CHOSENTRADE + "获取" + viewItems.size() + "个推广组信息耗时:" + (now - last));
		last = now;
		
		//2、如果列表使用，则获取其他信息（分网站出价、分行业出价、网站过滤信息）
		if(!forFlash){
			generateAddedInfo(false, false, true);	
		}else{
			this.siteFlashCache = new SiteFlashCache();
		}
		
		now = System.currentTimeMillis();
		LOG.debug(LOG_PREFIX_CHOSENTRADE + "获取" + viewItems.size() + "个推广组附加信息耗时:" + (now - last));
		last = now;
		
		//行业辅助VO类的Mapping即Map<GroupID,<TradeId,TradeAssistant>>
		Map<Integer, Map<Integer, TradeAssistant>> mapping = new HashMap<Integer,Map<Integer,TradeAssistant>>();
		
		//4、遍历当前层级的推广组
		for(ExtGroupViewItem viewItem : viewItems){			
			
			if(viewItem.getIsAllSite() == CproGroupConstant.GROUP_ALLSITE 
					|| CollectionUtils.isEmpty(viewItem.getSiteTradeList())){
				//没有自选行业的不需要关心
				continue;
			}
			
			Integer groupId = viewItem.getGroupId();
			Map<Integer, TradeAssistant> map = mapping.get(groupId);
			if(map == null){
				map = new HashMap<Integer, TradeAssistant>();
				mapping.put(viewItem.getGroupId(), map);
			}
			//4.1遍历推广组的自选行业
			for(Integer tradeId : viewItem.getSiteTradeList()){
				TradeInfo tradeInfo = UnionSiteCache.tradeInfoCache.getTradeInfoById(tradeId);//从Cache中获取行业信息
				if(tradeInfo == null || tradeInfo.getViewstat() == CproGroupConstant.TRADE_OTHER){
					//不存在的trade或者“其他”都不可见
					continue;
				}		
				
				if(tradeInfo.getParentid() == 0){//如果为一级行业
					TradeAssistant tradeAssistant = new TradeAssistant(tradeInfo.getTradeid(), true, forFlash);					
					//如果选中了一级行业，就需要为其append全部二级行业
					List<Integer> subTradeList = UnionSiteCache.tradeInfoCache.getFirstSiteTradeChildren().get(tradeInfo.getTradeid());
					//过滤搜索关键字
					if(queryKeyword != null && !tradeInfo.getTradename().contains(queryKeyword)){
						continue;
					}
					//只要一级行业选中，就需要为其append全部二级行业
					if(CollectionUtils.isNotEmpty(subTradeList)){
						for(Integer subTrade : subTradeList){
							TradeInfo subTradeInfo = UnionSiteCache.tradeInfoCache.getTradeInfoById(subTrade);
							TradeViewItem vo = new TradeViewItem();
							if(!forFlash){
								appendTradeInfo(groupId, viewItem, tradeInfo, subTradeInfo, vo, 1);
							}
							tradeAssistant.addSecondTrade(vo);
						}
					}
					
					TradeViewItem vo = new TradeViewItem();
					//groupId需要设，以便于生成token
					vo.setGroupId(groupId);
					if(!forFlash){
						appendTradeInfo(groupId, viewItem, tradeInfo, null , vo, 0);
					}
					tradeAssistant.setSelfTrade(vo);
					map.put(tradeInfo.getTradeid(), tradeAssistant);					
				}else{
					//如果选中了二级行业，不需要为其append一级行业，两者互斥	
					if(queryKeyword != null && !tradeInfo.getTradename().contains(queryKeyword)){
						//不包含行业名称
						continue;
					}
					TradeAssistant tradeAssistant = new TradeAssistant(tradeInfo.getTradeid(), false, forFlash);					
					TradeViewItem vo = new TradeViewItem();
					//groupId需要设，以便于生成token
					vo.setGroupId(groupId);
					if(!forFlash){
						TradeInfo firstTradeInfo = UnionSiteCache.tradeInfoCache.getTradeInfoById(tradeInfo.getParentid());
						appendTradeInfo(groupId, viewItem, firstTradeInfo, tradeInfo, vo, 0);
					}
					tradeAssistant.setSelfTrade(vo);
					map.put(tradeInfo.getTradeid(), tradeAssistant);
				}
			}
		}
		
		now = System.currentTimeMillis();
		LOG.debug(LOG_PREFIX_CHOSENTRADE + "合并" + viewItems.size() + "个推广组的行业信息耗时:" + (now - last));
		last = now;

		//5、初始化行业查询参数
		this.initLevelQueryParamWithTrade();

		//6、填充doris数据
//        if (ReportCache.isEqualOrAfterGMAvailableDate(from) && ReportCache.isEqualOrAfterGMAvailableDate(to)) {         
    	    //如果时间在group_mainsite视图生成之后则通过group_mainsite查询
    	    addTradeStatInfoViaTrade(viewItems, forFlash, mapping);
//        } else {
        	//如果查询时间在group_mainsite视图生成之前则通过beidoustat查询
    		//加入一级域名数据、二级域名、app行业数据
//            addTradeStatInfoViaDomain(viewItems, forFlash, mapping);  		
//    	}
        
		now = System.currentTimeMillis();
		LOG.debug(LOG_PREFIX_CHOSENTRADE + "合并" + viewItems.size() + "个推广组自选行业统计数据耗时:" + (now - last));
		last = now;
		
		list = new ArrayList<TradeAssistant>();
		int firstCount = 0;
		int secondCount = 0;
		for(Entry<Integer, Map<Integer, TradeAssistant>> map : mapping.entrySet()){
			Integer gid = map.getKey();
			for(Entry<Integer, TradeAssistant> entry : map.getValue().entrySet()){		
				TradeAssistant vo = entry.getValue();
				vo.ensureStatFields();				
				if(needStatFilter){
					//如果有需要根据统计数据进行过滤的项而且这条记录被过滤掉
					vo.filter(qp);
				}		
				if(!vo.isAllFiltered()){
					if(forFlash){
						//this.mainSiteSet.addAll(vo.getIncludeSite());
						this.siteFlashCache.insertTrade(gid, vo.getSelfTradeId());
					}
					if(vo.isFirstTrade()){
						firstCount++;
					}else{
						secondCount++;
					}
					list.add(vo);
				}
			}
		}
		now = System.currentTimeMillis();
		LOG.debug(LOG_PREFIX_CHOSENTRADE + "生成高级统计字段并根据统计项过滤" + list.size() + "个行业信息耗时:" + (now - last));
		last = now;
		//至此，全部的VO数据已经组装完成
		tradeSumData = generateTradeSumData(firstCount, secondCount, list);
		now = System.currentTimeMillis();
		LOG.debug(LOG_PREFIX_CHOSENTRADE + "生成" + list.size() + "个行业汇总信息耗时:" + (now - last));
		last = now;
		//生成分页汇总数据
		totalPage = super.getTotalPage(list.size());
		return list;
	}
	
	/**
     * addTradeStatInfoViaTrade:抽取出公共方法，用于合并行业的doris数据
     * 通过带有行业信息的doris数据直接合并；
     * @param viewItems 待显示的推广组
     * @param forFlash 是否用于分日图表
     * @param mapping 结果集Map
     * @since 
    */
    private void addTradeStatInfoViaTrade(List<ExtGroupViewItem> viewItems, boolean forFlash,
    		Map<Integer, Map<Integer, TradeAssistant>> mapping) {
    	//1、获取行业统计数据

    	//注意：2012.4.25之后的行业信息才有效
        List<TradeViewItem> olapList = tradeStatMgr.queryGroupTradeData(qp.getUserId(), planIds, groupIds, firstTradeIds, 
        		secondTradeIds, from, to, null, 0, ReportConstants.TU_NONE);
        
        //2、获取行业UV数据
        List<Map<String, Object>> uvData = uvDataService.queryGroupTradeData(
        		qp.getUserId(), this.planIds, this.groupIds, firstTradeIds, secondTradeIds, 
        		from, to, null, 0, 
        		ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0, DorisLevelType.ALL);
       
        //3、合并数据
        List<Map<String, Object>> mergedData = null;
        
        //4、如果需要获取转化数据，则获取转化数据并合并
		boolean needToFetchTransData = this.transReportFacade.needToFetchTransData(userId,from,to,false);

		if (needToFetchTransData){
			
			TempSitesAndTrans tempSiteAndTrans = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userId);
			
			//5、获取转化数据
			List<Map<String, Object>> tranData = null;
			tranData = transDataService.queryGroupTradeData(userId, 
								tempSiteAndTrans.getTransSiteIds(), tempSiteAndTrans.getTransTargetIds(), 
								planIds, groupIds, firstTradeIds, secondTradeIds, 
								from, to, null, 0, ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0, DorisLevelType.ALL);
								
			//6、获取Holmes数据
			List<Map<String, Object>> holmesData = null;
			holmesData = holmesDataService.queryGroupTradeData(userId, null, null, planIds, groupIds, firstTradeIds, secondTradeIds, 
															from, to, null, 0, 
															ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
				
			//7、Merge统计数据、UV数据、Holmes数据、转化数据
			List<String> mulitKey = new ArrayList<String>();
			mulitKey.add(ReportConstants.GROUP);
			mulitKey.add(ReportConstants.FIRSTTRADEID);
			mulitKey.add(ReportConstants.SECONDTRADEID);
			mergedData = this.reportFacade.mergeTransHolmesAndUvDataByMulitKey(Constant.DorisDataType.UV,tranData, holmesData, uvData, mulitKey);			
		
		} else {
			//8、Merge统计数据和UV数据
			mergedData = uvData;

		}
		
		List<TradeViewItem> dorisList = new ArrayList<TradeViewItem>();
		List<TradeViewItem> resultList = new ArrayList<TradeViewItem>();
		//7、填充数据
		if (! CollectionUtils.isEmpty(mergedData)) {
			for (Map<String, Object> row : mergedData) {
				TradeViewItem item = new TradeViewItem();
				if (row != null) {
					item.setPlanId(Integer.valueOf(row.get(ReportConstants.PLAN).toString()));
					item.setGroupId(Integer.valueOf(row.get(ReportConstants.GROUP).toString()));
					item.setFirstTradeId((Integer)row.get(ReportConstants.FIRSTTRADEID));
					item.setSecondTradeId((Integer)row.get(ReportConstants.SECONDTRADEID));
					item.fillStatRecord(row);
					dorisList.add(item);
				}
			}
		}
		
		Set<String> mergeKeys = new HashSet<String>(Arrays.asList(
				new String[]{Constants.COLUMN.GROUPID, Constants.COLUMN.FIRSTTRADEID, Constants.COLUMN.SECONDTRADEID}));
		
		resultList = ReportUtils.mergeItemList(dorisList, olapList, mergeKeys, 
				Constants.statMergeVals, TradeViewItem.class, true);
		
		ReportUtil.generateExtentionFields(resultList);
        
        now = System.currentTimeMillis();
        LOG.debug(LOG_PREFIX_CHOSENTRADE + "[trade]查询" + viewItems.size() + "个推广组的Doris信息耗时:" + (now - last));
        last = now;
        
        //9、行业添加doris数据处理
        if(CollectionUtils.isNotEmpty(resultList)){
            for(TradeViewItem item : resultList){
                Integer firstTradeId = item.getFirstTradeId();
                Integer secondTradeId = item.getSecondTradeId();
                Integer groupId = item.getGroupId();
                Map<Integer, TradeAssistant> map = mapping.get(groupId);
                if(map == null){
                    //如果没有此group，不需要处理
                    continue;
                }
                
                //处理一级行业
                TradeAssistant firstTrade = map.get(firstTradeId);
                if (firstTrade != null) {
                    if(firstTrade.getSelfTrade() != null){
                    	if(secondTradeId == ReportConstants.sumFlagInDoris){
                        	firstTrade.getSelfTrade().fillUvAndTrans(item);
                    	} 
                    	firstTrade.getSelfTrade().mergeBasicField(item);//填充数据
                        TradeViewItem secondTradeViewItem = firstTrade.getSecondTrade(secondTradeId);
                        if(secondTradeViewItem != null){
                            secondTradeViewItem.fillStatRecord(item);
                        }
                    }
                }       
                //处理二级行业
                TradeAssistant secondTrade = map.get(secondTradeId);
                if(secondTrade != null){
                    secondTrade.getSelfTrade().fillStatRecord(item);
                }
            }
        
        }
        
    }
	
	/**
	 * 如果为了flash取数据，虽然需要过滤，但是不需要取出价、Filter等信息
	 * @param forFlash
	 * @param needTransData 是否包含转化数据
	 * @return
	 */
	private List<SiteViewItem> generateChosenSiteList(boolean forFlash, boolean needTransData){
		
		List<SiteViewItem> list = null;
		
		//1、根据当前层级取得全部推广组的信息
		GroupQueryParameter queryParam = initQueryParameter();
		List<ExtGroupViewItem> viewItems = reportCproGroupMgr.findExtCproGroupReportInfo(queryParam);
		
		now = System.currentTimeMillis();
		LOG.debug(LOG_PREFIX_CHOSENSITE + "获取" + viewItems.size() + "个推广组信息耗时:" + (now - last));
		last = now;
		
		//2、获取投放网站其他信息，包括分网站出价、分行业出价、排除网站信息
		if(!forFlash){
			generateAddedInfo(true, true, true);	
			
			now = System.currentTimeMillis();
			LOG.debug(LOG_PREFIX_CHOSENSITE + "获取" + viewItems.size() + "个推广组的附加信息耗时:" + (now - last));
			last = now;
			
		}else{
			if(forFlash){
				this.siteFlashCache = new SiteFlashCache();
			}
		}

		//格式：Map<groupid, Map<siteId, SiteViewItem>>，存放返回前端的VO的Map
		Map<Integer, Map<Integer, SiteViewItem>> mapping = new HashMap<Integer,Map<Integer, SiteViewItem>>();

		//3、遍历当前层级推广组列表
		for(ExtGroupViewItem viewItem : viewItems){			
			if(viewItem.getIsAllSite() == CproGroupConstant.GROUP_ALLSITE 
					|| CollectionUtils.isEmpty(viewItem.getSiteList())){
				//没有自选网站的不需要关心
				continue;
			}
			
			Map<Integer, SiteViewItem> map = mapping.get(viewItem.getGroupId());
			if(map == null){
				map = new HashMap<Integer, SiteViewItem>(viewItem.getSiteList().size());
				mapping.put(viewItem.getGroupId(), map);
			}
			
			//4、遍历每个推广组的每个自选的网站
			for(Integer siteId : viewItem.getSiteList()){
				SiteViewItem vo = this.generateChosenSiteInfo(siteId, viewItem, forFlash);//填充自选网站基本信息
				if(vo == null){
					continue;
				}
				map.put(siteId, vo);
			}
		}
		
		now = System.currentTimeMillis();
		LOG.debug(LOG_PREFIX_CHOSENSITE + "填充" + viewItems.size() + "个推广组信息耗时:" + (now - last));
		last = now;
		
		//如果所有推广组都没有自选网站，直接返回
		if(mapping.size() == 0){
			return list;
		}
		
		initLevelQueryParamWithTrade();
//		List<Map<String, Object>> result = null;
		
		//获取主域层级的doris数据
		List<MainSiteViewItem> olapList = this.getStatAndTransDataByMainSite();
			
		now = System.currentTimeMillis();		
//		boolean isNew = ReportCache.isEqualOrAfterGMAvailableDate(from)	&& ReportCache.isEqualOrAfterGMAvailableDate(to); 
		LOG.debug(LOG_PREFIX_CHOSENSITE  + "获取" + viewItems.size() + "个推广组的doris数据耗时:" + (now - last));
		last = now;
		
		//6、merge统计数据和UV数据
		if(CollectionUtils.isNotEmpty(olapList)){
			for(MainSiteViewItem item : olapList){
				String siteUrl = item.getSiteUrl().toLowerCase();
				BDSiteInfo siteInfo = UnionSiteCache.siteInfoCache.getSiteInfoBySiteUrl(siteUrl);

				if(!needQuerySecondDomain && containsSecondDomainSite(siteUrl)){
					//这里不需要continue，这是由于有可能主域和二级域同在unionsite中且同时被用户选中
					this.needQuerySecondDomain = true;
				}

				if(siteInfo == null){
					continue;
				}
				Integer groupId = item.getGroupId();
				Map<Integer, SiteViewItem> map = mapping.get(groupId);
				if(map == null){
					//没有这个groupId，不需要处理
					continue;
				}
				SiteViewItem vo = map.get(siteInfo.getSiteid());
				if(vo == null){
					//没有这个siteId，也不需要处理
					continue;
				}
				vo.fillStatRecord(item);
				vo.generateExtentionFields();
			}
		}
		
		now = System.currentTimeMillis();
		LOG.debug(LOG_PREFIX_CHOSENSITE + "合并" + viewItems.size() + "个推广组的统计数据信息耗时:" + (now - last));
		last = now;

		//这时候还不知道有多少个，所以不用size
		list = new ArrayList<SiteViewItem>();
		//把siteIds清空，由于部分网站可能会被统计过滤掉，这里需要再计算一遍有多少个网站

		if(this.needQuerySecondDomain){
			//向doris发起二级域的请求
			
			//获取二级域层级的doris数据
			List<SubSiteViewItem> olapSubSiteList = this.getStatAndTransDataBySubSite();
			
			if(!CollectionUtils.isEmpty(olapSubSiteList)){
				//理论上讲一级域有展现了二级域不可能没有，这里判空有点多余
				for(SubSiteViewItem item : olapSubSiteList){
					//切记这里要取出来的是二级域
					String siteUrl = item.getSiteUrl().toLowerCase();
					Integer groupId = item.getGroupId();
					BDSiteInfo siteInfo = UnionSiteCache.siteInfoCache.getSiteInfoBySiteUrl(siteUrl);
					if(siteInfo == null){
						continue;
					}
					Map<Integer, SiteViewItem> map = mapping.get(groupId);
					if(map == null){
						//没有这个groupId，不需要处理
						continue;
					}
					SiteViewItem vo = map.get(siteInfo.getSiteid());
					if(vo == null){
						//没有这个siteId，也不需要处理
						continue;
					}
					
					//在这里设就可以，这个数据前端不用，只是在给flash时处理有意义
					vo.setSecondDomain(true);
					vo.fillStatRecord(item);
					vo.generateExtentionFields();
				}
			}
		}
		now = System.currentTimeMillis();
		LOG.debug(LOG_PREFIX_CHOSENSITE + "处理二级域名耗时:" + (now - last));
		last = now;
		
		//将map转换成list，用于返回前端
		for(Map<Integer, SiteViewItem> map : mapping.values()){
			for(SiteViewItem vo : map.values()){
				if(!vo.isHasFillStatInfo()){
					//这个东西没有统计数据，因此需要补0
					vo.fillZeroStat();
				}
				if(needStatFilter && ReportWebConstants.filter(qp, vo)){
					//如果有需要根据统计数据进行过滤的项而且这条记录被过滤掉了，则continue
					continue;
				}
				list.add(vo);
				if(forFlash){
					if(vo.isSecondDomain()){
						//siteSet.add(vo.getSiteUrl());
						siteFlashCache.insertSecondDomain(vo.getGroupId(), vo.getSiteUrl());
					}else{
						//mainSiteSet.add(vo.getSiteUrl());
						siteFlashCache.insertFirstDomain(vo.getGroupId(), vo.getSiteUrl());
					}
				}
			}
		}
		now = System.currentTimeMillis();
		LOG.debug(LOG_PREFIX_CHOSENSITE + "生成" + list.size() + "个网站信息耗时:" + (now - last));
		last = now;
		if(!forFlash){
			//至此，全部的VO数据已经组装完成
			siteSumData = generateSiteSumData(list.size(), list);
			now = System.currentTimeMillis();
			LOG.debug(LOG_PREFIX_CHOSENSITE + "生成" + list.size() + "个网站信息的汇总数据耗时:" + (now - last));
			last = now;
			//生成分页汇总数据
			totalPage = super.getTotalPage(list.size());
		}
		
		return list;
	}
	
	/**
	 * 根据前端传的条件，查询主域doris数据
	 * 查询时不做排序，orderBy为null
	 * @return
	 * 上午11:12:56 created by wangchongjie
	 */
	private List<MainSiteViewItem> getStatAndTransDataByMainSite(){
		//1、获取统计数据
		List<MainSiteViewItem> olapList = siteStatMgr.queryGroupMainSiteData(userId, planIds, groupIds, 
				this.firstTradeIds, this.secondTradeIds, null, from, to, orderBy, orient, ReportConstants.TU_NONE);
		
		//2、获取Uv数据(后续增加行业过滤)
		List<Map<String, Object>> uvData = uvDataService.queryGroupMainSiteData(
				qp.getUserId(), this.planIds, this.groupIds, this.firstTradeIds, this.secondTradeIds, null, 
				from, to, orderBy, orient, ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
		
		//如果需要获取转化数据，则获取转化数据并合并
		boolean needToFetchTransData = this.transReportFacade.needToFetchTransData(userId,from,to,false);

		List<Map<String, Object>> mergedData;
		//合并统计数据和Uv数据（使用groupid+mainsiteid作为复合key）
		List<String> mulitKey = new ArrayList<String>();
		mulitKey.add(ReportConstants.GROUP);
		mulitKey.add(ReportConstants.MAINSITE);	
		
		if (needToFetchTransData){
			
			TempSitesAndTrans tempSiteAndTrans = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userId);
			
			//3、获取主域转化数据
			List<Map<String, Object>> transData = this.transDataService.queryGroupMainSiteData(
					super.getUserId(), tempSiteAndTrans.getTransSiteIds(), tempSiteAndTrans.getTransTargetIds(), 
					this.planIds,this.groupIds, null, from, to, null, 0,
					ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT,0,0
			);
	
			//4、获取主域Holmes数据
			List<Map<String, Object>> holmesData = this.holmesDataService.queryGroupMainSiteData(
								super.getUserId(), null, null, this.planIds, this.groupIds, 
								this.firstTradeIds, this.secondTradeIds, null, from, to, null, 0, 
								ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
			
			//5、Merge统计数据、UV数据、Holmes数据、转化数据
			mergedData = reportFacade.mergeTransHolmesAndUvDataByMulitKey(DorisDataType.STAT,transData, holmesData, uvData, mulitKey);
		}else{
			//5、合并统计数据和Uv数据（使用groupid+mainsiteid作为复合key）
			mergedData = uvData;
		}

		List<MainSiteViewItem> dorisList = new ArrayList<MainSiteViewItem>();
		List<MainSiteViewItem> resultList = new ArrayList<MainSiteViewItem>();
		//7、填充数据
		if (! CollectionUtils.isEmpty(mergedData)) {
			for (Map<String, Object> row : mergedData) {
				MainSiteViewItem item = new MainSiteViewItem();
				if (row != null) {
					item.setPlanId(Integer.valueOf(row.get(ReportConstants.PLAN).toString()));
					item.setGroupId(Integer.valueOf(row.get(ReportConstants.GROUP).toString()));
					item.setSiteUrl((String)row.get(ReportConstants.MAINSITE));
					item.fillStatRecord(row);
					dorisList.add(item);
				}
			}
		}
		
		Set<String> mergeKeys = new HashSet<String>(Arrays.asList(
				new String[]{Constants.COLUMN.GROUPID, Constants.COLUMN.SITEURL}));
		
		resultList = ReportUtils.mergeItemList(dorisList, olapList, mergeKeys, 
				Constants.statMergeVals, MainSiteViewItem.class, true);
		
		ReportUtil.generateExtentionFields(resultList);
		return resultList;
	}
	
	/**
	 * 对于ReportWebConstants.showSecondDomainSiteList中配置的一级域，需要查询其二级域doris数据
	 * @return
	 * 上午11:09:08 created by wangchongjie
	 */
	private List<SubSiteViewItem> getStatAndTransDataBySubSite(){
		List<Map<String, Object>> uvData = Collections.emptyList();
		//1、获取二级域的统计数据
		List<SubSiteViewItem> olapList = siteStatMgr.queryGroupSubSiteData(userId, planIds, groupIds, 
				ReportWebConstants.showSecondDomainSiteList, null, from, to, orderBy, orient, ReportConstants.TU_NONE);
		
		//2、获取二级域的Uv数据
		uvData = uvDataService.queryGroupSubSiteData(
				qp.getUserId(), this.planIds, this.groupIds, this.firstTradeIds, this.secondTradeIds, 
				ReportWebConstants.showSecondDomainSiteList, null, from, to, orderBy, orient, 
				ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
		
		//如果需要获取转化数据，则获取转化数据并合并
		boolean needToFetchTransData = this.transReportFacade.needToFetchTransData(userId,from,to,false);

		//合并统计数和UV数据
		List<String> mulitKey = new ArrayList<String>();
		mulitKey = new ArrayList<String>();
		mulitKey.add(ReportConstants.GROUP);
		mulitKey.add(ReportConstants.SITE);	
		
		List<Map<String, Object>> mergedData;

		if (needToFetchTransData){
			
			TempSitesAndTrans tempSiteAndTrans = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userId);
			
			//3、获取主域转化数据
			List<Map<String, Object>> transData = this.transDataService.queryGroupSubSiteData(
						super.getUserId(), tempSiteAndTrans.getTransSiteIds(), tempSiteAndTrans.getTransTargetIds(), 
						this.planIds,this.groupIds, ReportWebConstants.showSecondDomainSiteList, 
						null, from, to, null, 0, ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT,0,0
			);
	
			//4、获取主域Holmes数据
			List<Map<String, Object>> holmesData = this.holmesDataService.queryGroupSubSiteData(
						super.getUserId(), null, null, this.planIds, this.groupIds, 
						this.firstTradeIds, this.secondTradeIds, ReportWebConstants.showSecondDomainSiteList, 
						null, from, to, null, 0, ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
			
			//5、Merge统计数据、UV数据、Holmes数据、转化数据
			mergedData = reportFacade.mergeTransHolmesAndUvDataByMulitKey(
					DorisDataType.STAT, transData, holmesData, uvData, mulitKey);
		}else{
			//5、Merge统计数据、UV数据
			mergedData = uvData;
		}
		

		List<SubSiteViewItem> dorisList = new ArrayList<SubSiteViewItem>();
		List<SubSiteViewItem> resultList = new ArrayList<SubSiteViewItem>();
		//7、填充数据
		if (! CollectionUtils.isEmpty(mergedData)) {
			for (Map<String, Object> row : mergedData) {
				SubSiteViewItem item = new SubSiteViewItem();
				if (row != null) {
					item.setPlanId(Integer.valueOf(row.get(ReportConstants.PLAN).toString()));
					item.setGroupId(Integer.valueOf(row.get(ReportConstants.GROUP).toString()));
					item.setSiteUrl((String)row.get(ReportConstants.SITE));
					item.fillStatRecord(row);
					dorisList.add(item);
				}
			}
		}
		
		Set<String> mergeKeys = new HashSet<String>(Arrays.asList(
				new String[]{Constants.COLUMN.GROUPID, Constants.COLUMN.SITEURL}));
		
		resultList = ReportUtils.mergeItemList(dorisList, olapList, mergeKeys, 
				Constants.statMergeVals, SubSiteViewItem.class, true);
		
		ReportUtil.generateExtentionFields(resultList);
		return resultList;
	}
	
	/**
	 * @param forFlash
	 * @param needTransData 是否包含转化数据
	 * @return
	 */
	private List<SiteViewItem> generateShownSiteList(boolean forFlash,boolean needTransData){
		List<SiteViewItem> list = null;
		//首先通过查询参数拼装Doris请求
		//这个列表全部使用Doris数据作为基准
		//是否为doris加行业信息列之后的表查询
//		boolean isNew = ReportCache.isEqualOrAfterGMAvailableDate(from) && ReportCache.isEqualOrAfterGMAvailableDate(to); 
		
		//初始化行业参数
		initLevelQueryParamWithTrade();

		//1-3、获取主域层级的doris数据
		List<MainSiteViewItem> olapList = this.getStatAndTransDataByMainSite();
		now = System.currentTimeMillis();
		LOG.debug(LOG_PREFIX_SHOWNSITE + "查询doris耗时:" + (now - last));
		last = now;
		
		if(CollectionUtils.isEmpty(olapList)){		
			return list;
		}
		//现在得到的数据已经是group*mainsite的信息了，但是此时需要把其他信息merge进去才行
		Set<Integer> groupIdSet = new HashSet<Integer>();
		
		list = new ArrayList<SiteViewItem>(olapList.size());
		
		//4、取得附加信息，包括分行业出价、分网站出价、网站过滤情况信息
		if(!forFlash){
			generateAddedInfo(true, true, true);
		}else{
			siteFlashCache = new SiteFlashCache();
		}
		
		now = System.currentTimeMillis();
		LOG.debug(LOG_PREFIX_SHOWNSITE + "查询附加信息耗时:" + (now - last));
		last = now;
		/**
		 * 下面的代码将qp中的过滤字段分解到多处使用的目的，是为了提升性能
		 * 一方面避免对DB的多次查询，另一方面尽早地将不需要的数据过滤掉
		 * 但是这样就造成了代码的不统一和不美观，需要后来人注意
		 */
		
		//为返回给前端的VO填充所有可能的数据
		//包括planId, groupId, srchs, clks, cost, acp, cpm, ctr, siteUrl
		for(MainSiteViewItem item : olapList){
			String siteUrl = item.getSiteUrl().toLowerCase();
			Integer groupId = item.getGroupId();
			
			groupIdSet.add(groupId);//收集ID，后面将数据库信息补齐到统计信息列表中。
			
			//如果这个东西需要展现二级域，则将一级域信息过滤掉
			if( this.containsSecondDomainSite(siteUrl)){
				//需要展现二级域的一级域有展现，此时还需要去doris再取一次
				this.needQuerySecondDomain = true;
				continue;
			}

			SiteViewItem vo = generateShownSiteVoItemStat(item);
			if(vo != null){
				list.add(vo);
				if(forFlash) {
					siteFlashCache.insertFirstDomain(vo.getGroupId(), vo.getSiteUrl());
				}
			}
		}
		now = System.currentTimeMillis();
		LOG.debug(LOG_PREFIX_SHOWNSITE + "合并" + list.size() + "个网站信息耗时:" + (now - last));
		last = now;
		Set<String> allDomain = new HashSet<String>();
		if(!forFlash){
			//在为flash选数据的时候，不需要append这些无用信息
			generateGroupInfo(groupIdSet);
			now = System.currentTimeMillis();
			LOG.debug(LOG_PREFIX_SHOWNSITE + "获取" + groupIdSet.size() + "个推广组信息耗时:" + (now - last));
			last = now;
			for(int i = list.size()-1; i>=0; i--){
				SiteViewItem vo = list.get(i);
				allDomain.add(vo.getSiteUrl());
				Integer groupId = vo.getGroupId();
				ExtGroupViewItem viewItem = viewMapping.get(groupId);
				if(viewItem != null){
					appendGroupInfo(vo, viewItem);
					String siteUrl = vo.getSiteUrl();
					BDSiteInfo siteInfo = UnionSiteCache.siteInfoCache.getSiteInfoBySiteUrl(siteUrl);
					if(siteInfo == null){
						BDSiteLiteInfo liteInfo = UnionSiteCache.allSiteLiteCache.getSiteLiteInfoByUrl(siteUrl);
						if(liteInfo != null){
							siteInfo = new BDSiteInfo();
							siteInfo.setSiteid(liteInfo.getSiteId());
							siteInfo.setSiteurl(liteInfo.getSiteUrl());
							vo.setSiteNotEffiective(true);
							appendSiteInfo(siteInfo, vo, viewItem, true);
						}else{
							//若不是联盟网站，则通过TC网站填充信息
							vo.setSiteNotEffiective(true);
							TcSiteInfo tcSiteInfo = UnionSiteCache.tcSiteCache.getTcSiteInfoBySiteUrl(siteUrl);
							appendTcSiteInfo(tcSiteInfo, vo, viewItem);
						}
					}else{
						appendSiteInfo(siteInfo, vo, viewItem, true);
					}
				}
			}
			now = System.currentTimeMillis();
			LOG.debug(LOG_PREFIX_SHOWNSITE + "合并" + list.size() + "个网站及所属推广组信息耗时:" + (now - last));
			last = now;
		}
		
		if(this.needQuerySecondDomain){
			//向doris发起二级域的请求
			
			//获取二级域doris数据
			List<SubSiteViewItem> olapSubSiteList = this.getStatAndTransDataBySubSite();
			
			if(!CollectionUtils.isEmpty(olapSubSiteList)){
				//理论上讲一级域有展现了二级域不可能没有，这里判空有点多余
				for(SubSiteViewItem item : olapSubSiteList){
					//切记这里要取出来的是二级域
					String siteUrl = item.getSiteUrl().toLowerCase();
					Integer groupId = item.getGroupId();
					SiteViewItem vo = generateShownSiteVoItemStat(item);
					if(vo != null){
						//这种特殊的二级域也需要加入
						allDomain.add(siteUrl);
						if(forFlash) {
							//siteSet.add(siteUrl);
							siteFlashCache.insertSecondDomain(vo.getGroupId(), vo.getSiteUrl());
						}
						if(!forFlash){
							ExtGroupViewItem viewItem = viewMapping.get(groupId);
							if(viewItem != null){
								appendGroupInfo(vo, viewItem);						
								BDSiteInfo siteInfo = UnionSiteCache.siteInfoCache.getSiteInfoBySiteUrl(siteUrl);
								//TC非联盟网站只有主域，无二级域，所以此处不对TC网站进行处理
								appendSiteInfo(siteInfo, vo, viewItem, true);
							}
							list.add(vo);
						}
					}
				}
			}
		}		
		now = System.currentTimeMillis();
		LOG.debug(LOG_PREFIX_SHOWNSITE + "处理二级域名信息耗时:" + (now - last));
		last = now;
		
		if(!forFlash){
			//至此，全部的VO数据已经组装完成
			siteSumData = generateSiteSumData(allDomain.size(), list);
			now = System.currentTimeMillis();
			LOG.debug(LOG_PREFIX_SHOWNSITE + "生成汇总数据耗时:" + (now - last));
			last = now;
			//生成分页汇总数据
			totalPage = super.getTotalPage(list.size());
		}
		
		return list;
	}

	private void appendTcSiteInfo(TcSiteInfo tcSiteInfo, SiteViewItem vo, 
			ExtGroupViewItem viewItem){
		Integer groupId = vo.getGroupId();
		
		if(tcSiteInfo != null){
			//以下都是基于有这个site的前提的
			vo.setTcSite(true);
			vo.setSiteId(tcSiteInfo.getSiteId());
			vo.setSecondTrade(UnionSiteCache.tradeInfoCache.
					getSiteTradeNameList().get(tcSiteInfo.getFirstTradeId()));
			vo.setFirstTrade(UnionSiteCache.tradeInfoCache.
					getSiteTradeNameList().get(tcSiteInfo.getSecondTradeId()));	
			
			//只有有展现网站列表才需要判断是否有效
			//只有这个网站有效,且为联盟站点（在unionsitecache中）且必须当前被这个推广组选择了投放，这时才能够修改出价
			vo.setSiteValid(false);
			vo.setSiteChosen(false);
	
			if(tradePriceMap != null && tradePriceMap.get(groupId) != null){
				if(tradePriceMap.get(groupId).get(tcSiteInfo.getSecondTradeId()) != null){
					//这个推广组在这个网站所在的二级行业上进行了单独出价					
					if(viewItem.getIsAllSite() == CproGroupConstant.GROUP_ALLSITE 
							|| viewItem.getSiteTradeList().contains(tcSiteInfo.getSecondTradeId())
							|| viewItem.getSiteTradeList().contains(tcSiteInfo.getFirstTradeId())){
						double price = (double)tradePriceMap.get(groupId).get(tcSiteInfo.getSecondTradeId());
						if(vo.getSpecialPrice()){
							vo.setOriPrice(price);
						}else{
							vo.setPrice(price);
						}
					}							
				}
				
				if(tradePriceMap.get(groupId).get(tcSiteInfo.getFirstTradeId()) != null){
					//这个推广组在这个网站所在的一级行业上进行了单独出价
					if(viewItem.getIsAllSite() == CproGroupConstant.GROUP_ALLSITE 
							|| viewItem.getSiteTradeList().contains(tcSiteInfo.getFirstTradeId())
							|| viewItem.getSiteTradeList().contains(tcSiteInfo.getSecondTradeId())){
						double price = (double)tradePriceMap.get(groupId).get(tcSiteInfo.getFirstTradeId());
						if(vo.getPrice() > 0){
							if(vo.getOriPrice() <= 0){
								//已经在较低层级出过价，但是上个层级并没有原始初价，需要在这里继承
								vo.setOriPrice(price);
							}
						}else{
							vo.setPrice(price);
						}
					}
				}
			}
		}
		
		if(vo.getPrice() > 0){
			if(vo.getOriPrice() <= 0){
				//已经出过价，但是上个层级并没有原始初价，需要在这里继承
				vo.setOriPrice((double)viewItem.getPrice());
			}
		}else{
			vo.setPrice((double)viewItem.getPrice());
		}
		//将出价转换成元
		vo.setPrice(vo.getPrice() / 100);
		vo.setOriPrice(vo.getOriPrice() / 100);
	}

	private void appendSiteInfo(BDSiteInfo siteInfo, SiteViewItem vo, 
			ExtGroupViewItem viewItem, boolean judgeSiteValid){
		Integer groupId = vo.getGroupId();
		if(siteInfo != null){
			//以下都是基于有这个site的前提的
			vo.setTcSite(false);
			vo.setSiteId(siteInfo.getSiteid());
			vo.setSecondTrade(UnionSiteCache.tradeInfoCache.
					getSiteTradeNameList().get(siteInfo.getSecondtradeid()));
			vo.setFirstTrade(UnionSiteCache.tradeInfoCache.
					getSiteTradeNameList().get(siteInfo.getFirsttradeid()));	
			//只有有展现网站列表才需要判断是否有效
			if(judgeSiteValid){
				if(UnionSiteCache.siteInfoCache.getSiteInfoBySiteId(siteInfo.getSiteid()) != null){
					if(viewItem.getIsAllSite() == CproGroupConstant.GROUP_ALLSITE 
							|| viewItem.getSiteList().contains(siteInfo.getSiteid())
							|| viewItem.getSiteTradeList().contains(siteInfo.getSecondtradeid())
							|| viewItem.getSiteTradeList().contains(siteInfo.getFirsttradeid())){
						//只有这个网站有效（在unionsitecache中）且必须当前被这个推广组选择了投放，这时才能够修改出价
						vo.setSiteValid(true);
					}
				}
				if(viewItem.getSiteList() != null && viewItem.getSiteList().contains(siteInfo.getSiteid())){
					vo.setSiteChosen(true);
				}
			}
			
			//这块判断sitePriceMap是否为null，如果为null则不处理，这是为了兼容trade
			if(sitePriceMap != null && sitePriceMap.get(groupId) != null && sitePriceMap.get(groupId).get(siteInfo.getSiteid()) != null){
				//这个推广组在这个网站上进行了单独出价
				if(viewItem.getIsAllSite() == CproGroupConstant.GROUP_ALLSITE 
						|| viewItem.getSiteList().contains(siteInfo.getSiteid())
						|| viewItem.getSiteTradeList().contains(siteInfo.getSecondtradeid())
						|| viewItem.getSiteTradeList().contains(siteInfo.getFirsttradeid())){
					vo.setPrice((double)sitePriceMap.get(groupId).get(siteInfo.getSiteid()));
					vo.setSpecialPrice(true);
				}						
			}
			//楼上的site是最细粒度，不需要关心继承关系
			if(tradePriceMap != null && tradePriceMap.get(groupId) != null){
				if(tradePriceMap.get(groupId).get(siteInfo.getSecondtradeid()) != null){
					//这个推广组在这个网站所在的二级行业上进行了单独出价					
					if(viewItem.getIsAllSite() == CproGroupConstant.GROUP_ALLSITE 
							|| viewItem.getSiteTradeList().contains(siteInfo.getSecondtradeid())
							|| viewItem.getSiteTradeList().contains(siteInfo.getFirsttradeid())){
						double price = (double)tradePriceMap.get(groupId).get(siteInfo.getSecondtradeid());
						if(vo.getSpecialPrice()){
							vo.setOriPrice(price);
						}else{
							vo.setPrice(price);
						}
					}							
				}
				
				if(tradePriceMap.get(groupId).get(siteInfo.getFirsttradeid()) != null){
					//这个推广组在这个网站所在的一级行业上进行了单独出价
					if(viewItem.getIsAllSite() == CproGroupConstant.GROUP_ALLSITE 
							|| viewItem.getSiteTradeList().contains(siteInfo.getFirsttradeid())
							|| viewItem.getSiteTradeList().contains(siteInfo.getSecondtradeid())){
						double price = (double)tradePriceMap.get(groupId).get(siteInfo.getFirsttradeid());
						if(vo.getPrice() > 0){
							if(vo.getOriPrice() <= 0){
								//已经在较低层级出过价，但是上个层级并没有原始初价，需要在这里继承
								vo.setOriPrice(price);
							}
						}else{
							vo.setPrice(price);
						}
					}
				}
			}
		}
		if(vo.getPrice() > 0){
			if(vo.getOriPrice() <= 0){
				//已经出过价，但是上个层级并没有原始初价，需要在这里继承
				vo.setOriPrice((double)viewItem.getPrice());
			}
		}else{
			vo.setPrice((double)viewItem.getPrice());
		}
		//将出价转换成元
		vo.setPrice(vo.getPrice() / 100);
		vo.setOriPrice(vo.getOriPrice() / 100);
	}
	
	private void appendTradeInfo(Integer groupId, ExtGroupViewItem viewItem, 
			TradeInfo firstTrade, TradeInfo secondTrade, TradeViewItem vo,
			int level){
		this.appendGroupInfo(vo, viewItem);
		//追加出价信息，这个比较特别
		vo.setPrice((double)viewItem.getPrice());
		if(tradePriceMap != null && tradePriceMap.get(groupId) != null){
			Map<Integer, Integer> priceMap = tradePriceMap.get(groupId);
			if(priceMap != null){
				if(secondTrade != null){
					//这是个二级行业
					Integer secondPrice = priceMap.get(secondTrade.getTradeid());
					if(secondPrice != null){
						//二级上有单独出价
						vo.setPrice((double)secondPrice);
						vo.setSpecialPrice(true);
					}
				}
				Integer firstPrice = priceMap.get(firstTrade.getTradeid());
				if(firstPrice != null){
					if(secondTrade != null){
						if(vo.getSpecialPrice()){
							//一个二级行业已经单独出价，且一级也有单独出价，需要将一级出价作为其原始出价
							vo.setOriPrice(firstPrice);
						}else{
							vo.setPrice(firstPrice);
						}
					}else{
						//这是一个一级行业，它有单独出价
						vo.setPrice((double)firstPrice);
						vo.setSpecialPrice(true);
					}
				}
			}
		}
		if(vo.getSpecialPrice() && vo.getOriPrice() <= 0.0){
			vo.setOriPrice(viewItem.getPrice());
		}
		vo.setPrice(vo.getPrice()/100);
		vo.setOriPrice(vo.getOriPrice()/100);
		vo.setLevel(level);
		if(firstTrade != null){
			vo.setFirstTrade(firstTrade.getTradename());
			vo.setFirstTradeId(firstTrade.getTradeid());
		}
		if(secondTrade != null){
			//有secondTrade的时候，firstTrade一定非空
			vo.setSecondTrade(secondTrade.getTradename());
			vo.setSecondTradeId(secondTrade.getTradeid());
		}
	}
	
	
	
	private SiteViewItem generateChosenSiteInfo(Integer siteId, ExtGroupViewItem viewItem, boolean forFlash){
		BDSiteInfo info = UnionSiteCache.siteInfoCache.getSiteInfoBySiteId(siteId);
		boolean siteNotEffiective = false;
		if(info == null){
			BDSiteLiteInfo liteInfo = UnionSiteCache.allSiteLiteCache.getSiteLiteInfo(siteId);
			if(liteInfo == null){
				return null;
			}
			//重新构造BDSiteInfo对象，其中各种参数都是基元类型，不需要初始化和判空
			info = new BDSiteInfo();
			info.setSiteid(siteId);
			info.setSiteurl(liteInfo.getSiteUrl());
			//网站已失效
			siteNotEffiective = true;
		}
		String siteUrl = info.getSiteurl();			
		if(queryKeyword != null && !siteUrl.contains(queryKeyword)){
			return null;
		}
		if(filterByTrade(info)){
			return null;
		}		
		SiteViewItem vo = new SiteViewItem();
		vo.setSiteId(siteId);
		vo.setSiteNotEffiective(siteNotEffiective);
		vo.setSiteUrl(siteUrl);
		//已经在这了，肯定是true
		vo.setSiteValid(true);
		vo.setSiteChosen(true);
		if(forFlash){
			vo.setGroupId(viewItem.getGroupId());
		}else{
			this.appendGroupInfo(vo, viewItem);
			//追加其他信息
			this.appendSiteInfo(info, vo, viewItem, false);
			if(siteFilterMap.get(viewItem.getGroupId())!= null 
					&& siteFilterMap.get(viewItem.getGroupId()).contains(siteUrl)){
				vo.setFiltered(true);
			}
		}
		return vo;
	}
	
	private boolean generateReport(AbstractReportVo reportVo){
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try{
			ReportCSVWriter.getInstance().write(reportVo, outputStream);
			byte[] buffer = outputStream.toByteArray();
			this.fileSize = buffer.length;
			this.inputStream = new ByteArrayInputStream(buffer);
			return true;
		}catch(Exception ex){
			return false;
		}finally{
			try{
				if(outputStream != null){
					outputStream.close();
				}
				if(inputStream != null){
					inputStream.close();
				}
				
			}catch(Exception ex){
			}
		}
	}
	
	private String[] generateHeader(boolean showTransData){
		List<String> header = new ArrayList<String>();
		String prefix = null;
		int maxColumn = 0;
		if(qp.getTab() == QueryParameterConstant.TAB.TAB_SITE_CHOSENTRADE){
			prefix = "download.trade.head.col";
			maxColumn = DOWNLOAD_TRADE_HEADER_COL;
		}else{
			prefix = "download.site.head.col";
			maxColumn = DOWNLOAD_SITE_HEADER_COL;
		}
		if(qp.getTab() == QueryParameterConstant.TAB.TAB_SITE_CHOSENTRADE){
			for ( int col = 0; col < maxColumn; col++ ) {
				if(col == 2){
					//第一列是plan如果在plan或者group层级不需要看
					if(qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_PLAN)
							|| qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_GROUP)){
						continue;
					}
				}else if(col == 3){
					//第二列是group如果在group层级不需要看
					if(qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_GROUP)){
						continue;
					}
				}
				header.add(this.getText(prefix + (col + 1)));
			}
		}else{
			for ( int col = 0; col < maxColumn; col++ ) {
				if(col == 1){
					//第一列是plan如果在plan或者group层级不需要看
					if(qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_PLAN)
							|| qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_GROUP)){
						continue;
					}
				}else if(col == 2){
					//第二列是group如果在group层级不需要看
					if(qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_GROUP)){
						continue;
					}
				}
				header.add(this.getText(prefix + (col + 1)));
			}
		}
		
		if (showTransData) {
			header.add(this.getText("download.head.arrivalrate.holmes"));
			header.add(this.getText("download.head.hoprate.holmes"));
			header.add(this.getText("download.head.restime.holmes"));
			header.add(this.getText("download.head.direct.trans"));
			header.add(this.getText("download.head.indirect.trans"));
		}
		
		String[] array = new String[header.size()];
		return header.toArray(array);
	}
	
	private ReportAccountInfo generateAccountInfo() {
		ReportAccountInfo accountInfo = new ReportAccountInfo();
		
		
		accountInfo.setReport(this.getText("download.account.report.site"));
		accountInfo.setReportText(this.getText("download.account.report"));
		
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}
		accountInfo.setAccount(user.getUsername());
		accountInfo.setAccountText(this.getText("download.account.account"));
		
		
		String dateRange = sd1.format(from) + "-" + sd1.format(to);
		String prefix = user.getUsername();
		
		accountInfo.setDateRange(dateRange);
		accountInfo.setDateRangeText(this.getText("download.account.daterange"));	
		//[帐户名]-[推广计划名称]-[推广组名称]-[Tab名称]-[是否分网站/是否分日]-[时间分布].csv

		
		String level = this.getText("download.account.level.allplan");

		//设置成：所有推广计划/推广计划：XXXX（该推广计划名称）/推广组：XXXX（该推广组名称）
		if (QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(qp.getLevel())) {			
			Integer planId = qp.getPlanId();
			if(planId != null ) {
				CproPlan plan = cproPlanMgr.findCproPlanById(planId);
				if (plan != null) {
					level += "/" + this.getText("download.account.level.plan") + plan.getPlanName();
					accountInfo.setLevel(level);	
					prefix += "-" + plan.getPlanName();
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
					accountInfo.setLevel(level);	
					prefix += "-" +  plan.getPlanName() + "-" + group.getGroupName();
				}
			}			
		}  else {
			//account级别不用设置具体信息
		}	
		accountInfo.setLevelText(this.getText("download.account.level"));	
		try {
			fileName = prefix + "-" + getText("download.site.filename.prefix");
			if(qp.getTab() == QueryParameterConstant.TAB.TAB_SITE_CHOSENTRADE_SITE){
				fileName += "-" + getText("download.site.filename.tradesite") + "";
			}
			fileName +=  "-" + dateRange + ".csv";
			//中文文件名需要用ISO8859-1编码			
			fileName = new String(fileName.getBytes(fileEncoding), "ISO8859-1");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage());
		}
		return accountInfo;
	}
	
	private <T extends SiteViewItem> SiteViewItem generateShownSiteVoItemStat(T item){	
		if(item == null){
			return null;
		}
		String siteUrl = item.getSiteUrl();
		Integer groupId = item.getGroupId();
		if(queryKeyword != null && !siteUrl.contains(queryKeyword)){
			//注意由于是有展现网站列表，此处必须是先从doris取再过滤
			return null;
		}
		
		BDSiteInfo siteInfo = UnionSiteCache.siteInfoCache.getSiteInfoBySiteUrl(siteUrl);
		if(this.filterByTrade(siteInfo)){
			return null;
		}

		SiteViewItem vo = item;
		if(needStatFilter && ReportWebConstants.filter(qp, vo)){
			//如果有需要根据统计数据进行过滤的项而且这条记录被过滤掉了，刚continue
			return null;
		}
		//只关心groupId就行了，plan从DB去取
		//这个信息必须在这里加入，这是由于网站过滤可以过滤不在unionsite中的url
		if(siteFilterMap != null && siteFilterMap.get(groupId)!= null && siteFilterMap.get(groupId).contains(siteUrl)){
			vo.setFiltered(true);
		}
		//其余信息在后面加入
		return vo;
	}
	
	/**
	 * 生成网站汇总数据
	 * @param count
	 * @param list
	 * @return
	 */
	private SiteSumData generateSiteSumData(int count, List<? extends SiteStatInfo> list){
		SiteSumData sumData = new SiteSumData();
		//
		//1、设置网站总数
		sumData.setCount(list.size());
		
		//2、设置排序信息
		if (list.size() <= ReportWebConstants.FRONT_SORT_THRESHOLD ) {
//				&& (qp.getPage() == null || qp.getPage() <= 0 )) {
			//page<=0 && 当前行数小于等于1W
			sumData.setNeedFrontSort(ReportConstants.Boolean.TRUE);
		}		
		
		//3、累加汇总列表
		for (SiteStatInfo item : list) {
			sumData.setClks(sumData.getClks() + item.getClks());
			sumData.setCost(sumData.getCost() + item.getCost());
			sumData.setSrchs(sumData.getSrchs() + item.getSrchs());		
			sumData.setSrchuv(sumData.getSrchuv() + item.getSrchuv());
			sumData.setClkuv(sumData.getClkuv() + item.getClkuv());
		}
		
		//4、重新计算扩展列
		sumData.generateExtentionFields();
		
		//5、返回数据
		return sumData;
	}
	
	private TradeSumData generateTradeSumData(int fTradeCount, int sTradeCount, List<TradeAssistant> list){
		//生成汇总的统计数据
		TradeSumData sumData = new TradeSumData();
		sumData.setFirstTradeCount(fTradeCount);
		sumData.setSecondTradeCount(sTradeCount);
		//行业一定需要后端排序
		sumData.setNeedFrontSort(ReportConstants.Boolean.FALSE);
		for (TradeAssistant item : list) {
			StatInfo info = item.getSumData();
			sumData.setClks(sumData.getClks() + info.getClks());
			sumData.setCost(sumData.getCost() + info.getCost());
			sumData.setSrchs(sumData.getSrchs() + info.getSrchs());	
			sumData.setSrchuv(sumData.getSrchuv() + info.getSrchuv());
			sumData.setClkuv(sumData.getClkuv() + info.getClkuv());
		}
		sumData.generateExtentionFields();
		return sumData;
	}
	
	
	/**
	 * hasFilter: 是否没有过滤字段，不考虑userid ,planid ,groupid
	 *
	 * @return  是否有投放网络使用到的过滤字段 ,true表示没有过滤字段，false表示有   
	*/
	protected boolean hasNoFilter() {
		return !qp.hasStatField4Filter() 
				&& StringUtils.isEmpty(qp.getKeyword())
				&& qp.getFTradeId() == null 
				&& qp.getSTradeId() == null;
	}
	
	public GroupSiteConfigMgr getSiteConfigMgr() {
		return siteConfigMgr;
	}

	public void setSiteConfigMgr(GroupSiteConfigMgr siteConfigMgr) {
		this.siteConfigMgr = siteConfigMgr;
	}

	public ReportCproGroupMgr getReportCproGroupMgr() {
		return reportCproGroupMgr;
	}

	public void setReportCproGroupMgr(ReportCproGroupMgr reportCproGroupMgr) {
		this.reportCproGroupMgr = reportCproGroupMgr;
	}


	public Integer getTradeId() {
		return tradeId;
	}

	public void setTradeId(Integer tradeId) {
		this.tradeId = tradeId;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
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


	public String getOperation() {
		return operation;
	}


	public void setOperation(String operation) {
		this.operation = operation;
	}
	
	public ReportCacheService getCacheService() {
		return cacheService;
	}

	public void setCacheService(ReportCacheService cacheService) {
		this.cacheService = cacheService;
	}
}
