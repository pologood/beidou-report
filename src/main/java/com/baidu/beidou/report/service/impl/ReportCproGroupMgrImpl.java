package com.baidu.beidou.report.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.cprogroup.bo.CproGroup;
import com.baidu.beidou.cprogroup.constant.CproGroupConstant;
import com.baidu.beidou.cprogroup.constant.UnionSiteCache;
import com.baidu.beidou.cprogroup.dao.CproGroupDao;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.olap.constant.Constants;
import com.baidu.beidou.olap.service.ReportByDayStatService;
import com.baidu.beidou.olap.vo.GroupViewItem;
import com.baidu.beidou.olap.vo.ReportDayViewItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.dao.ReportCproGroupDao;
import com.baidu.beidou.report.dao.vo.FrontBackendGroupStateMapping;
import com.baidu.beidou.report.dao.vo.GroupQueryParameter;
import com.baidu.beidou.report.facade.TransReportFacade;
import com.baidu.beidou.report.service.ReportCproGroupMgr;
import com.baidu.beidou.report.util.ReportUtil;
import com.baidu.beidou.report.vo.QueryParameter;
import com.baidu.beidou.report.vo.StatInfo;
import com.baidu.beidou.report.vo.comparator.ViewStateOrderComparator;
import com.baidu.beidou.report.vo.group.ExtGroupViewItem;
import com.baidu.beidou.stat.driver.bo.Constant;
import com.baidu.beidou.stat.facade.ReportFacade;
import com.baidu.beidou.stat.facade.UvReportFacade;
import com.baidu.beidou.stat.service.HolmesDataService;
import com.baidu.beidou.stat.service.TransDataService;
import com.baidu.beidou.stat.service.UvDataService;
import com.baidu.beidou.tool.util.TransReportHelper;
import com.baidu.beidou.tool.vo.TempSitesAndTrans;
import com.baidu.unbiz.olap.constant.OlapConstants;
import com.baidu.unbiz.olap.util.ReportUtils;

/**
 * <p>ClassName:ReportCproGroupMgrImpl
 * <p>Function: 推广组报表服务
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-5
 */
public class ReportCproGroupMgrImpl implements ReportCproGroupMgr{

	private static Log logger = LogFactory.getLog(ReportCproGroupMgrImpl.class);
	
	@Resource(name="reportByDayStatServiceImpl")
	private ReportByDayStatService byDayStatService;
	
	private ReportCproGroupDao reportCproGroupDao;
	private CproGroupDao cproGroupDao;
	
	/**
	 * added by zhuxiaoling since cpweb-550
	 */
	/**UV数据的查询接口*/
	private UvDataService uvDataService;
	
	/**Holmes数据的查询接口*/
	private HolmesDataService holmesDataService;
	
	/**UV数据的Facade接口*/
	private UvReportFacade uvReportFacade;
	
	/**Report的Facade接口*/
	private ReportFacade reportFacade;
	
	/**
	 * 转化数据的Facade接口
	 */
	private TransReportFacade transReportFacade;
	
	/**
	 * 转化数据的查询接口
	 */
	private TransDataService transDataService;

	public List<GroupViewItem> findCproGroupViewItem(QueryParameter qp, boolean shouldPagable) {
		
		if(qp == null || qp.getUserId() == null) {
			logger.error("queryparameter or userId must not be null when execute the query");
			return new ArrayList<GroupViewItem>();
		}
		GroupQueryParameter queryParam = makeGroupQueryParameter(qp, shouldPagable);
		List<GroupViewItem> result = reportCproGroupDao.findCproGroupReportInfo(queryParam);
		
		//如果分页了且按状态排序则需要在内在中按状态排序； 但当前逻辑一般走不到这个逻辑，因为大于1W的时候是不能按状态排序，而小于1W则由前端排序。
		if (shouldPagable && !CollectionUtils.isEmpty(result) && 
				ReportWebConstants.FRONT_BACKEND_ORDERNAME_VIEWSTATE.equals(qp.getOrderBy())) {
			
			//如果按状态排序则在内存中排。
			Collections.sort(result, new ViewStateOrderComparator(qp.getOrder()));
		}
		return result;
	}

	public int countCproGroupViewItem(QueryParameter qp) {
		if(qp == null || qp.getUserId() == null) {
			logger.error("queryparameter or userId must not be null when execute the query");
			return 0;
		}
		GroupQueryParameter queryParam = makeGroupQueryParameter(qp, false);
		return reportCproGroupDao.countCproGroupReportInfo(queryParam);
	}
	
	protected GroupQueryParameter makeGroupQueryParameter(QueryParameter qp, boolean shouldPagable) {
		
		GroupQueryParameter queryParam = new GroupQueryParameter();
		
		queryParam.setUserId(qp.getUserId());//userId不可能为空
		queryParam.setPlanId(qp.getPlanId());//planId可能为空

		queryParam.setName(qp.getKeyword());
		
		Integer[] groupTypes = qp.getDisplayType();
		if(!ArrayUtils.isEmpty(qp.getTargetType())){
			queryParam.setTargetType(qp.getTargetType());
		}
		
		if (!ArrayUtils.isEmpty(groupTypes)) {
			boolean hasFix = false;//是否有固定
			boolean hasFlow = false;//是否有悬浮
			boolean hasFilm = false;//是否有贴片
			
			for (Integer groupType : groupTypes) {
				if(CproGroupConstant.GROUP_TYPE_FIXED == groupType) {
					hasFix = true;
					continue;
				}
				if(CproGroupConstant.GROUP_TYPE_FLOW == groupType) {
					hasFlow = true;
					continue;
				}
				if(CproGroupConstant.GROUP_TYPE_FILM == groupType) {
					hasFilm = true;
					continue;
				}
			}
			if (hasFix == hasFlow && hasFix == hasFilm) {
				//如果三个都有或者都没有则不用按groupType过滤
			} else {
				queryParam.setGroupType(qp.getDisplayType());
			}
		}

		FrontBackendGroupStateMapping stateMapping = (FrontBackendGroupStateMapping)qp.getStateMapping();
		if (stateMapping != null  && stateMapping.needFilterByState()) {
			//group的状态
			queryParam.setState(stateMapping.getGroupState());
			queryParam.setStateInclude(stateMapping.isIncludeGroupState());
			
			//plan的状态
			queryParam.setIncludePlanState(stateMapping.isIncludePlanState());//采用in | not in
			queryParam.setBudgetOver(stateMapping.isHasBudgetOver());//添加已下线状态
			queryParam.setPlanState(stateMapping.getPlanStateWithoutBudgetOver());
		}
		
		if (!CollectionUtils.isEmpty(qp.getIds())) {
			queryParam.setIds(qp.getIds());
		}
		
		
		//不需要设置分页信息：分页信息不全或者按“状态”进行排序，或者明确说明（shouldPagable==false）不分页
//		  if (!qp.noPagerInfo()
//				&& !ReportWebConstants.FRONT_BACKEND_ORDERNAME_VIEWSTATE.equals(qp.getOrderBy())
//				&& !shouldPagable) {
		
		//如果需要分页则设置分页信息
		// 通常来说，如果进行了分页的话就不按state和统计字段进行排序（记录条件大于1W的情况）
		if(shouldPagable) {
			int page = (qp.getPage() == null || qp.getPage() < 0) ? 0 : qp.getPage();
			int pageSize = (qp.getPageSize() == null || qp.getPageSize() < 0) ? ReportConstants.PAGE_SIZE : qp.getPageSize();
			queryParam.setPage(page);
			queryParam.setPageSize(pageSize);
		}
		
		//非按统计字段和状态排序则通过sql的order by来排序（因为DB中的state值与需求的排序规则不匹配）
		if(shouldPagable
				&& !ReportConstants.isStatField(qp.getOrderBy()) 
				&& !ReportWebConstants.FRONT_BACKEND_ORDERNAME_VIEWSTATE.equals(qp.getOrderBy())
				&& !StringUtils.isEmpty(qp.getOrderBy())) {
			
			//如果按照状态来排序的话由于在DB中不方便，因此由内存来排序
			queryParam.setSortColumn(ReportWebConstants.GROUP_FRONT_BACKEND_PARAMNAME_MAPPING.get(qp.getOrderBy().toLowerCase()));
			queryParam.setSortOrder(qp.getOrder());
		}
		
		return queryParam;
	}

	public ReportCproGroupDao getReportCproGroupDao() {
		return reportCproGroupDao;
	}

	public void setReportCproGroupDao(ReportCproGroupDao reportCproGroupDao) {
		this.reportCproGroupDao = reportCproGroupDao;
	}

	public List<Integer> findAllCproGroupIdsByQuery(QueryParameter qp,
			boolean shouldPagable) {
		

		if(qp == null || qp.getUserId() == null) {
			logger.error("queryparameter or userId must not be null when execute the query");
			return new ArrayList<Integer>(0);
		}
		GroupQueryParameter queryParam = makeGroupQueryParameter(qp, false);
		return reportCproGroupDao.findAllCproGroupIds(queryParam);
	}
	
	public List<ExtGroupViewItem> findExtCproGroupReportInfo(GroupQueryParameter queryParam){
		return reportCproGroupDao.findExtCproGroupReportInfo(queryParam);
	}

	public int countCproGroupByPlanId(Integer planId) {
		return (int)reportCproGroupDao.countGroupByPlan(planId);
	}

	public int countCproGroupByUserId(Integer userId) {
		return (int)reportCproGroupDao.countGroupByUser(userId);
	}

	public int getGroupSiteStatus(Integer groupId) {
		CproGroup group = cproGroupDao.findWithInfoById(groupId);
		if(group == null){
			throw new BusinessException("no group found");
		}
		if(group.getGroupInfo().getIsAllSite() == CproGroupConstant.GROUP_ALLSITE){
			return GROUP_ALLSITE;
		}
		if(StringUtils.isNotEmpty(group.getGroupInfo().getSiteTradeListStr())){
			return GROUP_NORMAL;
		}
		if(StringUtils.isEmpty(group.getGroupInfo().getSiteListStr())){
			return GROUP_NOSITE;
		}
		String[] array = group.getGroupInfo().getSiteListStr().split("\\|");
		if(array != null){
			for(String siteId : array){
				Integer site = Integer.valueOf(siteId);
				if(UnionSiteCache.siteInfoCache.getSiteInfoBySiteId(site) != null){
					return GROUP_NORMAL;
				}
			}
		}
		return GROUP_NOSITE;
	}

	public void setCproGroupDao(CproGroupDao cproGroupDao) {
		this.cproGroupDao = cproGroupDao;
	}

	public Set<Integer> findAllQTGroupIdsByLevel(Integer userId,
			Integer planId, Integer groupId) {
		GroupQueryParameter queryParam = new GroupQueryParameter();
		queryParam.setUserId(userId);//不可为null		
		if(groupId != null){
			List<Long> ids = new ArrayList<Long>(1);
			ids.add(Long.valueOf(groupId.longValue()));
			queryParam.setIds(ids);
		}else{
			queryParam.setPlanId(planId);//可能为null,groupid优先
		}
		List<Integer> groupids =  reportCproGroupDao.findAllCproGroupIds(queryParam);
		if(CollectionUtils.isEmpty(groupids)){
			return new HashSet<Integer>(0);
		}
		Set<Integer> result = new HashSet<Integer>(groupids.size());
		result.addAll(groupids);
		return result;
	}

	public Set<Integer> findAllKTGroupIdsByLevel(Integer userId, Integer planId, Integer groupId){
		GroupQueryParameter queryParam = new GroupQueryParameter();
		queryParam.setUserId(userId);//不可为null		
		if(groupId != null){
			List<Long> ids = new ArrayList<Long>(1);
			ids.add(Long.valueOf(groupId.longValue()));
			queryParam.setIds(ids);
		}else{
			queryParam.setPlanId(planId);//可能为null,groupid优先
		}
		List<Integer> groupids =  reportCproGroupDao.findAllCproGroupIds(queryParam);
		if(CollectionUtils.isEmpty(groupids)){
			return new HashSet<Integer>(0);
		}
		Set<Integer> result = new HashSet<Integer>(groupids.size());
		result.addAll(groupids);
		return result;
	}
	
	public List<ReportDayViewItem> getGroupReportDayViewItems(Integer userId, QueryParameter qp, 
			Date from, Date to) {
		if (userId == null || userId < 0 || qp == null || from == null || to == null) {
			return Collections.emptyList();
		}
		List<ReportDayViewItem> resultList = new ArrayList<ReportDayViewItem>();
		List<ReportDayViewItem> dorisList = null;
		List<Map<String, Object>> mergedData = null;
		
		List<ReportDayViewItem> olapList = byDayStatService.queryByDayData(userId, null,
				Arrays.asList(new Integer[]{qp.getGroupId()}), from, to, ReportConstants.TU_DAY);
		
		//2、获取UV数据
		List<Map<String, Object>> uvData = this.uvDataService.queryGroupData(userId, null, Arrays.asList(new Integer[] { qp.getGroupId() }), 
																from, to, null, 0, 
																ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);
		
		
		//3、判断是否需要转化数据
		boolean needToFetchTransData = this.transReportFacade.needToFetchTransData(userId,from,to,false);
		
		if (needToFetchTransData) {
			
			TempSitesAndTrans tempSiteAndTrans = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userId);
			
			//4、获取转化数据
			List<Map<String, Object>> transData = transDataService.queryGroupData(userId, 
										tempSiteAndTrans.getTransSiteIds(), tempSiteAndTrans.getTransTargetIds(), 
										null, Arrays.asList(new Integer[] { qp.getGroupId() }), from, to, null, 0, 
										ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);
			
			//5、获取holmes数据
			List<Map<String, Object>> holmesData = holmesDataService.queryGroupData(userId, null, null, null, Arrays.asList(new Integer[]{qp.getGroupId()}), 
																from, to, null,	0, 
																ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT, 0, 0);	
			
			//6、Merge统计数据、UV数据、Holmes数据、转化数据（FROMDATE为Key）
			mergedData = this.reportFacade.mergeTransHolmesAndUvData(Constant.DorisDataType.UV, transData, holmesData, uvData, ReportConstants.FROMDATE);		
			
		} else {
			//6、Merge统计数据、UV数据（FROMDATE为Key）
			mergedData = uvData;
		}
		
		dorisList = transformStatDataByDay(mergedData);
		
		resultList = ReportUtils.mergeItemList(dorisList, olapList, OlapConstants.COLUMN.SHOWDATE, 
				Constants.statMergeVals, ReportDayViewItem.class, true);
		
		ReportUtil.generateExtentionFields(resultList);
		
		//7、按照统计字段过滤  modified by zhuxiaoling since cpweb-550
		resultList = filterByStatField(qp, resultList);
		
		ReportUtil.generateExtentionFields(resultList);
		
		return resultList;
	}
	
	
	/**
	 * 按统计字段进行过滤
	 * @param qp 查询参数
	 * @param vos 待过滤列表
	*/
	private <T extends StatInfo> List<T> filterByStatField(QueryParameter qp, List<T> vos) {
		
		if(org.apache.commons.collections.CollectionUtils.isEmpty(vos) 
				|| !qp.hasStatField4Filter()) {
			return vos;
		} else {
			for ( int i = vos.size() - 1; i >=0; i--) {
				if (ReportWebConstants.filter(qp, vos.get(i))) {
					vos.remove(vos.get(i));
				}
			}
			return vos;
		}
	}
	
	private List<ReportDayViewItem> transformStatDataByDay(List<Map<String, Object>> statData){
		List<ReportDayViewItem> result = new ArrayList<ReportDayViewItem>();
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
		for(Map<String, Object> stat : statData){
			ReportDayViewItem item = new ReportDayViewItem();
			item.fillStatRecord(stat);
			Date date = (Date) stat.get(ReportConstants.FROMDATE);
			item.setShowDate(sd.format(date));
			
			result.add(item);
		}
		return result;
	}

	public UvDataService getUvDataService() {
		return uvDataService;
	}

	public void setUvDataService(UvDataService uvDataService) {
		this.uvDataService = uvDataService;
	}

	public HolmesDataService getHolmesDataService() {
		return holmesDataService;
	}

	public void setHolmesDataService(HolmesDataService holmesDataService) {
		this.holmesDataService = holmesDataService;
	}

	public UvReportFacade getUvReportFacade() {
		return uvReportFacade;
	}

	public void setUvReportFacade(UvReportFacade uvReportFacade) {
		this.uvReportFacade = uvReportFacade;
	}

	public ReportFacade getReportFacade() {
		return reportFacade;
	}

	public void setReportFacade(ReportFacade reportFacade) {
		this.reportFacade = reportFacade;
	}

	public TransReportFacade getTransReportFacade() {
		return transReportFacade;
	}

	public void setTransReportFacade(TransReportFacade transReportFacade) {
		this.transReportFacade = transReportFacade;
	}

	public TransDataService getTransDataService() {
		return transDataService;
	}

	public void setTransDataService(TransDataService transDataService) {
		this.transDataService = transDataService;
	}

	public CproGroupDao getCproGroupDao() {
		return cproGroupDao;
	}

}