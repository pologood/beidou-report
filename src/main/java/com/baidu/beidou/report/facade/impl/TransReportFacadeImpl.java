package com.baidu.beidou.report.facade.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.facade.TransReportFacade;
import com.baidu.beidou.olap.vo.ReportDayViewItem;
import com.baidu.beidou.olap.vo.AbstractViewItem;
import com.baidu.beidou.olap.vo.SiteViewItem;
import com.baidu.beidou.stat.service.TransDataService;
import com.baidu.beidou.tool.driver.vo.HmSite;
import com.baidu.beidou.tool.driver.vo.HmTrans;
import com.baidu.beidou.tool.service.HolmesMgr;
import com.baidu.beidou.tool.util.TransReportHelper;
import com.baidu.beidou.tool.vo.TempSitesAndTrans;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.DateUtils;
import com.baidu.beidou.util.memcache.BeidouCacheInstance;

public class TransReportFacadeImpl implements TransReportFacade {
	
	private static final Log LOG = LogFactory.getLog(TransReportFacadeImpl.class);
	
	private HolmesMgr holmesMgr;
	
	/**
	 * 添加转化数据的接口对象
	 */
	private TransDataService transDataService;

	private int transDataReadyTime = 14;
	
	private int signFlagMemcacheExpireTimeInMinute = 10;
	
	/* (non-Javadoc)
	 * @see com.baidu.beidou.report.facade.impl.TransReportFacade#mergeTransAndStatData(boolean, java.util.List, java.util.List, java.lang.String)
	 */
	public List<Map<String, Object>> mergeTransAndStatData(
			boolean isOrderByStatDataField,
			List<Map<String, Object>> transData,
			List<Map<String, Object>> statData, String idKey){
		
		
		if(isOrderByStatDataField){
			Map<Object,Map<String, Object>> transDataMap = new HashMap<Object,Map<String, Object>>();
			if(transData != null){
				for(Map<String, Object> trans : transData){
					transDataMap.put(trans.get(idKey), trans);
				}
			}
			if(statData != null){
				for (Map<String, Object> stat : statData) {
					Map<String, Object> trans = transDataMap.get(stat.get(idKey));
					if (trans!=null) {
						stat.put(ReportConstants.DIRECT_TRANS_CNT, trans.get(ReportConstants.DIRECT_TRANS_CNT));
						stat.put(ReportConstants.INDIRECT_TRANS_CNT, trans.get(ReportConstants.INDIRECT_TRANS_CNT));
						transDataMap.remove(stat.get(idKey));
					}
				}
			}
			if(statData == null){
				statData = new ArrayList<Map<String, Object>>();
			}
			for(Map<String, Object> trans : transDataMap.values()){
				statData.add(trans);
			}
			return statData;
		} else {
			Map<Object,Map<String, Object>> stateDataMap = new HashMap<Object,Map<String, Object>>();
			if(statData != null){
				for(Map<String, Object> stat : statData){
					stateDataMap.put(stat.get(idKey), stat);
				}
			}
			if(transData != null){
				for (Map<String, Object> trans : transData) {
					Map<String, Object> stat = stateDataMap.get(trans.get(idKey));
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
						stateDataMap.remove(trans.get(idKey));
					}
				}
			}

			if(transData == null){
				transData = new ArrayList<Map<String, Object>>();
			}
			for(Map<String, Object> stat : stateDataMap.values()){
				transData.add(stat);
			}
			return transData;
		}
	}

	
	/* (non-Javadoc)
	 * @see com.baidu.beidou.report.facade.impl.TransReportFacade#mergeTransAndStatDataByDay(java.util.List, java.util.List)
	 */
	public List<Map<String, Object>> mergeTransAndStatDataByDay(
			List<Map<String, Object>> transData,
			List<Map<String, Object>> statData){
		
		if (CollectionUtils.isNotEmpty(transData) || CollectionUtils.isNotEmpty(statData)) {
			return this.mergeTransAndStatData(true, transData, statData, ReportConstants.FROMDATE);
		}else {
			return statData;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.baidu.beidou.report.facade.impl.TransReportFacade#needToFetchTransData(java.lang.Integer, java.util.Date, java.util.Date, boolean)
	 */
	public boolean needToFetchTransData(Integer userId, Date from, Date to,boolean forceGet) {
		
		boolean need = false;
		boolean signed = this.isTransToolSigned(userId, forceGet);
		if (signed) {
			Date now = new Date();
			Date today = DateUtils.getDateCeil(now).getTime();
			Date yestoday = DateUtils.getDateCeil(DateUtils.getPreviousDay(now)).getTime();

			need = from.before(yestoday) ;//查询的数据超过昨天， 则始终需要查询Doris
			if (!need) {//查询的时间在昨天今天以内
				Calendar readyTime = DateUtils.getCurDateCeil();
				readyTime.add(Calendar.HOUR_OF_DAY, transDataReadyTime);
				need = now.after(readyTime.getTime()) && from.before(today);
			}
		}
		return need;
	}
	
	
	
	/* (non-Javadoc)
	 * @see com.baidu.beidou.report.facade.impl.TransReportFacade#isTransToolSigned(java.lang.Integer, boolean)
	 */
	public boolean isTransToolSigned(Integer userId, boolean forceGet ){
		String key = BeidouCoreConstant.MEMCACHE_KEY_IS_TRANS_TOOL_SIGNED + String.valueOf(userId);
		
		boolean signed = false;//用户是否签订转化工具协议？
		
		if (!forceGet) {//非强制从holmes获取，则从缓存中获取
			Object obj = BeidouCacheInstance.getInstance().memcacheGet(key);
			if (obj != null) {
				signed = (Boolean)(obj);
			} else {
				forceGet = true;
			}
		}
		
		if (forceGet) {
			try {
				signed = holmesMgr.isContractSigned(userId);
			} catch (Exception e) {
				LOG.error("调用holmes API获取用户是否签约时发生错误", e);
			}
			BeidouCacheInstance.getInstance().memcacheSet(key, signed, 60*signFlagMemcacheExpireTimeInMinute);//十分钟失效
		}
		
		return signed;
	}


	/* (non-Javadoc)
	 * @see com.baidu.beidou.report.facade.impl.TransReportFacade#postHandleTransData(java.lang.Integer, java.util.Date, java.util.Date, java.util.List)
	 */
	public <T extends AbstractViewItem> void postHandleTransData(Integer userId, Date from, Date to,
			List<T> infoData) {
		if (CollectionUtils.isNotEmpty(infoData)) {
			boolean isTransToolSigned = this.isTransToolSigned(userId, false);//用户是转化工具用户
			boolean isTransDataValid = this.needToFetchTransData(userId, from, to, false);//转化数据可用
			if (isTransToolSigned && !isTransDataValid) {
				for (AbstractViewItem item : infoData) {
					item.setIndirectTrans(-1);
					item.setDirectTrans(-1);
				}
			}
		}
		
		
	}


	/* (non-Javadoc)
	 * @see com.baidu.beidou.report.facade.impl.TransReportFacade#postHandleTransDataByDay(java.lang.Integer, java.util.Date, java.util.Date, java.util.List)
	 */
	public void postHandleTransDataByDay(Integer userId, Date from, Date to,
			List<ReportDayViewItem> infoData) {
		if (CollectionUtils.isNotEmpty(infoData)) {
			boolean isTransToolSigned = this.isTransToolSigned(userId, false);//用户是转化工具用户
			boolean isTransDataValid = this.needToFetchTransData(userId, from, to, false);//转化数据可用
			if (isTransToolSigned && !isTransDataValid) {
				for (ReportDayViewItem item : infoData) {
					item.setIndirectTrans(-1);
					item.setDirectTrans(-1);
				}
			}
		}
	}


	/* (non-Javadoc)
	 * @see com.baidu.beidou.report.facade.impl.TransReportFacade#postHandleGroupSiteTransData(java.lang.Integer, java.util.Date, java.util.Date, java.util.List)
	 */
	public void postHandleGroupSiteTransData(Integer userId, Date from,
			Date to, List<SiteViewItem> infoData) {
		if (CollectionUtils.isNotEmpty(infoData)) {
			boolean isTransToolSigned = this.isTransToolSigned(userId, false);//用户是转化工具用户
			boolean isTransDataValid = this.needToFetchTransData(userId, from, to, false);//转化数据可用
			if (isTransToolSigned && !isTransDataValid) {
				for (SiteViewItem item : infoData) {
					item.setIndirectTrans(-1);
					item.setDirectTrans(-1);
				}
			}
		}
	}

	public void setHolmesMgr(HolmesMgr holmesMgr) {
		this.holmesMgr = holmesMgr;
	}

	public void setTransDataReadyTime(int transDataReadyTime) {
		this.transDataReadyTime = transDataReadyTime;
	}

	public TransDataService getTransDataService() {
		return transDataService;
	}


	public void setTransDataService(TransDataService transDataService) {
		this.transDataService = transDataService;
	}

	public void setSignFlagMemcacheExpireTimeInMinute(
			int signFlagMemcacheExpireTimeInMinute) {
		this.signFlagMemcacheExpireTimeInMinute = signFlagMemcacheExpireTimeInMinute;
	}


	public String getTransName(Integer userId, Long siteId, Long transId) {
		if (siteId == null || transId == null || userId == null) {
			return null;
		}

		boolean isSiteIdValid = false;
		List<HmSite> siteList = this.holmesMgr.getSiteListForSelect(userId);//网站ID是与开放主域相匹配的网站
		if (CollectionUtils.isNotEmpty(siteList)) {
			for (HmSite site : siteList) {
				if (site.getSite_id().equals(siteId)) {
					isSiteIdValid = true;
				}
			}
		}
		
		if (!isSiteIdValid) {
			return null;//用户不存在这个siteId;
		} 
		
		String transName = null;
		List<HmTrans> list = this.holmesMgr.getPageTransListBySiteId(siteId);
		if (CollectionUtils.isNotEmpty(list)) {
			for (HmTrans trans : list) {
				if (trans.getTrans_id().equals(transId)) {
					transName = trans.getName();
					break;
				}
			}
		}

		return transName;
	}

	public List<Map<String, Object>> queryUserData(
			int userid,
			List<Long> transSiteIds, 
			List<Long> transTargetIds, 
			Date from,
			Date to, 
			String orderColumn, 
			int order, 
			int timeunit, 
			int type, int page, int pageSize) {
		TempSitesAndTrans tast = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userid, transSiteIds, transTargetIds);
		
		if (CollectionUtils.isEmpty(tast.getTransSiteIds())
				||
			CollectionUtils.isEmpty(tast.getTransTargetIds())
		) {
			return new ArrayList<Map<String,Object>>(0);
		}
		
		return transDataService.queryUserData(userid, tast.getTransSiteIds(), tast.getTransTargetIds(),
		    		 from, to, orderColumn, order, timeunit, type, page, pageSize);
	}

	/**
	 * 获取指定用户的推广计划在指定时间段内的统计数据，按照时间粒度、推广计划分组
	 * transSiteIds有一个或多个值时，统计指定的广告组网站
	 * transTargetIds有一个或多个值时，统计指定的转化
	 * planids有一个或多个值时，统计指定推广计划
	 * planids为null时，统计用户的所有推广计划
	 * 返回格式：[
	{FROMDATE:Date, TODATE:Date, PLANID:Integer, DIRECT_TRANS_CNT:Long,INDIRECT_TRANS_CNT:Long}, 
	 {}, ...]
	 * 首先按照指定列排序，然后按照时间升序，然后按照planid升序
	 * 
	 * 输入排序字段column可能为srchs/clks/cost/ctr/acp/cpm，或null
	 */
	public List<Map<String, Object>> queryPlanData(
			int userid, 
			List<Long> transSiteIds,
			List<Long> transTargetIds,
			List<Integer> planids,
			Date from, 
			Date to, 
			String orderColumn, 
			int order, 
			int timeunit, 
			int type, int page, int pageSize){
		
		TempSitesAndTrans tast = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userid, transSiteIds, transTargetIds);
		
		if (CollectionUtils.isEmpty(tast.getTransSiteIds())
				||
			CollectionUtils.isEmpty(tast.getTransTargetIds())
		) {
			return new ArrayList<Map<String,Object>>(0);
		}
		
		return transDataService.queryPlanData(userid, tast.getTransSiteIds(), tast.getTransTargetIds(),
		    		planids, from, to, orderColumn, order, timeunit, type, page, pageSize);
		
	}

	/**
	 * 获取指定用户的推广组在指定时间段内的统计数据，按照时间粒度、推广组分组
	 * transSiteIds有一个或多个值时，统计指定的广告组网站
	 * transTargetIds有一个或多个值时，统计指定的转化
	 * groupids有一个或多个值时，统计指定推广组
	 * planids有一个或多个值，groupids为null时，统计指定推广计划的所有推广组
	 * planids、groupids为null时，统计用户的所有推广组
	 * 返回格式：[
	{FROMDATE:Date, TODATE:Date, PLANID:Integer, GROUPID:Integer, DIRECT_TRANS_CNT:Long,INDIRECT_TRANS_CNT:Long}, 
	 {}, ...]
	 * 首先按照指定列排序，然后按照时间升序，然后按照groupid升序
	 */
	public List<Map<String, Object>> queryGroupData(
			int userid,
			List<Long> transSiteIds,
			List<Long> transTargetIds,
			List<Integer> planids,
			List<Integer> groupids, 
			Date from, 
			Date to, 
			String orderColumn,
			int order, 
			int timeunit, 
			int type, int page, int pageSize){
		
		TempSitesAndTrans tast = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userid, transSiteIds, transTargetIds);
		
		if (CollectionUtils.isEmpty(tast.getTransSiteIds())
				||
			CollectionUtils.isEmpty(tast.getTransTargetIds())
		) {
			return new ArrayList<Map<String,Object>>(0);
		}
		
		return transDataService.queryGroupData(userid, tast.getTransSiteIds(), tast.getTransTargetIds(),
				planids, groupids, from, to, orderColumn, order, timeunit, type, page, pageSize);
	}
	
	/**
	 * 获取指定用户的推广单元在指定时间段内的统计数据，按照时间粒度、推广单元分组
	 * transSiteIds有一个或多个值时，统计指定的广告组网站
	 * transTargetIds有一个或多个值时，统计指定的转化
	 * unitids有一个或多个值时，统计指定推广单元
	 * groupids有一个或多个值，unitids为null时，统计指定推广组的所有推广单元
	 * planids有一个或多个值，groupids、unitids为null时，统计指定推广计划的所有推广单元
	 * planids、groupids、unitids为null时，统计用户的所有推广单元
	 * 返回格式：[
	{FROMDATE:Date, TODATE:Date, PLANID:Integer, GROUPID:Integer, UNITID:Long, DIRECT_TRANS_CNT:Long,INDIRECT_TRANS_CNT:Long}, 
	 {}, ...]
	 * 首先按照指定列排序，然后按照时间升序，然后按照unitid升序
	 */
	public List<Map<String, Object>> queryUnitData(
			int userid, 
			List<Long> transSiteIds,
			List<Long> transTargetIds,
			List<Integer> planids,
			List<Integer> groupids, 
			List<Long> unitids, 
			Date from, 
			Date to,
			String orderColumn, 
			int order, 
			int timeunit, 
			int type, int page, int pageSize){
		
		TempSitesAndTrans tast = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userid, transSiteIds, transTargetIds);
		
		if (CollectionUtils.isEmpty(tast.getTransSiteIds())
				||
			CollectionUtils.isEmpty(tast.getTransTargetIds())
		) {
			return new ArrayList<Map<String,Object>>(0);
		}
		
		return transDataService.queryUnitData(userid, tast.getTransSiteIds(), tast.getTransTargetIds(),
					   planids, groupids, unitids, from, to, orderColumn, order,
					   timeunit, type, page, pageSize);
	}


	/**
	 * 获取指定用户（可以指定plan和组）在指定时间段内的统计数据，按照时间粒度、主域、Group分组
	 * 返回格式：[
	{FROMDATE:Date, TODATE:Date, PLANID:Integer,GROUPID:Integer,
	MAINSITE:String, DIRECT_TRANS_CNT:Long,INDIRECT_TRANS_CNT:Long}, 
	 {}, ...]
	 */
	public List<Map<String, Object>> queryGroupMainSiteData(
			int userid,
			List<Long> transSiteIds,
			List<Long> transTargetIds,
			List<Integer> planids, 
			List<Integer> groupids,
			Collection<String> mainSiteUrls, 
			Date from, 
			Date to, 
			String orderColumn,
			int order, 
			int timeunit, 
			int type, int page, int pageSize){
		
		TempSitesAndTrans tast = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userid, transSiteIds, transTargetIds);
		
		if (CollectionUtils.isEmpty(tast.getTransSiteIds())
				||
			CollectionUtils.isEmpty(tast.getTransTargetIds())
		) {
			return new ArrayList<Map<String,Object>>(0);
		}
		
		return transDataService.queryGroupMainSiteData(userid, tast.getTransSiteIds(), tast.getTransTargetIds(),
				planids, groupids, mainSiteUrls, from, to, orderColumn, order, timeunit, type, page, pageSize);
	}


	/**
	 * 获取指定用户（可以指定plan和组）在指定时间段内的统计数据，按照时间粒度、二级域、Group分组
	 * 返回格式：[
	{FROMDATE:Date, TODATE:Date, PLANID:Integer,GROUPID:Integer,MAINSITE:STRING,
	SUBSITE:String, DIRECT_TRANS_CNT:Long,INDIRECT_TRANS_CNT:Long}, 
	 {}, ...]
	 */
	public List<Map<String, Object>> queryGroupSubSiteData(
			int userid,
			List<Long> transSiteIds,
			List<Long> transTargetIds,
			List<Integer> planids, 
			List<Integer> groupids,
			Collection<String> mainSiteUrls,
			Collection<String> subSiteUrls, 
			Date from, 
			Date to, 
			String orderColumn,
			int order, 
			int timeunit, 
			int type, int page, int pageSize){
		
		TempSitesAndTrans tast = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userid, transSiteIds, transTargetIds);
		
		if (CollectionUtils.isEmpty(tast.getTransSiteIds())
				||
			CollectionUtils.isEmpty(tast.getTransTargetIds())
		) {
			return new ArrayList<Map<String,Object>>(0);
		}
		
		return transDataService.queryGroupSubSiteData(userid,tast.getTransSiteIds(),tast.getTransTargetIds(),
				planids, groupids, mainSiteUrls, subSiteUrls, from, to, orderColumn, order, timeunit, type, page, pageSize);
	}

	
	public List<Map<String, Object>> queryDTData(int userid,
			List<Long> transSiteIds, List<Long> transTargetIds,
			List<Integer> planids, List<Integer> groupids,
			List<Integer> genderids, Date from, Date to, String orderColumn,
			int order, int timeunit, int type, int page, int pageSize) {
		// TODO Auto-generated method stub
		return null;
	}


	public List<Map<String, Object>> queryITData(int userid,
			List<Long> transSiteIds, List<Long> transTargetIds,
			List<Integer> planids, List<Integer> groupids, List<Long> gpids,
			List<Integer> refpackids, List<Integer> iids, List<Integer> itids,
			Date from, Date to, String orderColumn, int order, int timeunit,
			int type, int page, int pageSize) {
		// TODO Auto-generated method stub
		return null;
	}


	public List<Map<String, Object>> queryKeywordData(int userid,
			List<Long> transSiteIds, List<Long> transTargetIds,
			List<Integer> planids, List<Integer> groupids, List<Long> gpids,
			List<Integer> refpackids, Collection<Integer> keywordids,
			Collection<Integer> wordids, Date from, Date to, String orderColumn,
			int order, int timeunit, int type, int page, int pageSize, int wordType) {
		// TODO Auto-generated method stub
		return null;
	}


	public List<Map<String, Object>> queryPackData(int userid,
			List<Long> transSiteIds, List<Long> transTargetIds,
			List<Integer> planids, List<Integer> groupids, List<Long> gpids,
			List<Integer> refpackids, Date from, Date to, String orderColumn,
			int order, int timeunit, int type, int page, int pageSize) {
		// TODO Auto-generated method stub
		return null;
	}


	public List<Map<String, Object>> queryGroupTradeData(int userid,
			List<Long> transSiteIds, List<Long> transTargetIds,
			List<Integer> planids, List<Integer> groupids,
			List<Integer> firstTradeIds, List<Integer> secondTradeIds,
			Date from, Date to, String orderColumn, int order, int timeunit,
			int type, int page, int pageSize, int levelType) {
		// TODO Auto-generated method stub
		return null;
	}


	public List<Map<String, Object>> queryRegData(int userid,
			List<Long> transSiteIds, List<Long> transTargetIds,
			List<Integer> planids, List<Integer> groupids,
			List<Integer> provids, List<Integer> cityids, Date from, Date to,
			String orderColumn, int order, int timeunit, int type, int page,
			int pageSize, int levelType) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public List<Map<String, Object>> queryGroupAtData(int userid,
			List<Long> transSiteIds, List<Long> transTargetIds,
			List<Integer> planids, List<Integer> groupids, List<Long> atIds,
			List<Integer> atTypes, Date from, Date to, String orderColumn,
			int order, int timeunit, int type, int page, int pageSize) {
		// TODO Auto-generated method stub
		return null;
	}
	
}


