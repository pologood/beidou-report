package com.baidu.beidou.report.dao.impl;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.baidu.beidou.cproplan.vo.CproPlanState;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.dao.ReportCproPlanDao;
import com.baidu.beidou.report.dao.vo.PlanQueryParameter;
import com.baidu.beidou.report.vo.plan.PlanOffTimeVo;
import com.baidu.beidou.olap.vo.PlanViewItem;
import com.baidu.beidou.util.BeidouConstant;
import com.baidu.beidou.util.CollectionsUtil;
import com.baidu.beidou.util.CproStateUtil;
import com.baidu.beidou.util.FilterUtils;
import com.baidu.beidou.util.StringUtils;
import com.baidu.beidou.util.dao.GenericRowMapping;
import com.baidu.beidou.util.partition.PartID;
import com.baidu.beidou.util.partition.impl.PartKeyBDidImpl;
import com.baidu.beidou.util.partition.strategy.PartitionStrategy;


/**
 * <p>ClassName:ReportCproPlanDaoImpl
 * <p>Function: 查询report相关的计划信息
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-3
 * @version  $Id: Exp $
 */
public class ReportCproPlanDaoImpl extends BaseReportDaoImpl implements ReportCproPlanDao {
	

	private PartitionStrategy strategy = null;
	
	private GenericRowMapping<PlanViewItem> mapping = new GenericRowMapping<PlanViewItem>(){

		public PlanViewItem mapRow(ResultSet rs, int rowNum) throws SQLException {
			PlanViewItem item = new PlanViewItem();
			item.setPlanId(rs.getInt(1));
			item.setPlanName(rs.getString(2));
			item.setState(rs.getInt(3));
			item.setBudget(rs.getInt(4));
			item.setBudgetOver(rs.getInt(5));
			item.setPromotionType(rs.getInt(6));
			item.setBidRatio(rs.getInt(7));
			
			//生成viewState信息
			CproPlanState planState = new CproPlanState();
			planState.setPlanState(item.getState());
			planState.setBudgetOver(item.getBudgetOver());
			
			item.setViewState(CproStateUtil.getCproPlanViewState(planState));
			item.setViewStateOrder(CproStateUtil.getPlanViewStateOrder(item.getViewState()));
			return item;
		}};
	
	/*最全的查询SQL
	SELECT p.planid, p.planname, p.planstate,p.budget,p.budgetover  
	FROM cproplan p 
	WHERE 
	p.userid=? and p.planstate = ? and p.planid in () and p.budgetover=1 
	and p.planname like ‘%%’;
	*/
	public List<PlanViewItem> findCproPlanReportInfo(
			PlanQueryParameter queryParam) {
		StringBuilder sb = new StringBuilder();
		List<Object> params = new ArrayList<Object>(); 
		List<Integer> paramsType = new ArrayList<Integer>(); 
		
		sb.append("SELECT p.planid, p.planname, p.planstate,p.budget,p.budgetover,p.promotion_type,p.wireless_bid_ratio FROM beidou.cproplan p WHERE p.userid=?");
		params.add(queryParam.getUserId());
		paramsType.add(Types.INTEGER);
		
		makePlanStateFilter(sb, queryParam.getState(), queryParam.isBudgetOver(), queryParam.isStateInclude());
		
		if (!CollectionUtils.isEmpty(queryParam.getIds())) {
			String planIdStr = StringUtils.makeStrCollection(queryParam.getIds());
			sb.append(" AND p.planid in (" + planIdStr +")");
		}
		
		if (null != queryParam.getPromotionType()) {
			sb.append(" AND p.promotion_type = " + queryParam.getPromotionType());
		}

		if (!org.apache.commons.lang.StringUtils.isEmpty(queryParam.getName())) {
			sb.append(" AND p.planname like '%" + FilterUtils.transferSpecialInSqlParam(queryParam.getName()) + "%'");
		}
		
		//排序处理
		String col = queryParam.getSortColumn();
		if(!StringUtils.isEmpty(col)) {
			sb.append(" ORDER BY " + (ReportWebConstants.isGBKColumn(col) ? 
					ReportWebConstants.generateGBKSortString( col ) : col));
			
			if (BeidouConstant.SORTORDER_DESC.equalsIgnoreCase(queryParam.getSortOrder())) {
				sb.append(" " + queryParam.getSortOrder());
			}
		}
		
		//如果page<1则表示第一次发起请求，需要取全部 的数据
		if (queryParam.getPage() != null && queryParam.getPage() > 0 
				&& queryParam.getPageSize() != null && queryParam.getPageSize() > 0) {
			return findBySql(mapping, sb.toString(), params.toArray(), 
					CollectionsUtil.tranformIntegerListToIntArray(paramsType), queryParam.getPage(), 
					queryParam.getPageSize());
		}
		
		return this.findBySql(mapping, sb.toString(), params.toArray(), 
				CollectionsUtil.tranformIntegerListToIntArray(paramsType));
	}

	public List<PlanOffTimeVo> findCproPlanOfftime(List<Integer> planIds) {

		if (CollectionUtils.isEmpty(planIds)) {
			return new ArrayList<PlanOffTimeVo>();
		}
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT planid, max(offtime)  FROM beidoucap.cproplan_offline WHERE planid in (" );
		
		String planIdStr = StringUtils.makeStrCollection(planIds);
		sb.append(planIdStr);
		sb.append(") GROUP BY planid");
		return this.findBySql(new GenericRowMapping<PlanOffTimeVo>() {

			public PlanOffTimeVo mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				PlanOffTimeVo vo = new PlanOffTimeVo();
				vo.setPlanId(rs.getInt(1));
				vo.setOfftime(rs.getTimestamp(2));
				return vo;
			}
			
		}, sb.toString(), new Object[]{}, new int[]{});
	}

	public List<Integer> findInSchedualPlanIds(int userId, List<Integer> planIds) {

		if (CollectionUtils.isEmpty(planIds)) {
			return new ArrayList<Integer>();
		}
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT distinct g.planid FROM beidou.cprogroup g, beidou.cproplan p " );
		sb.append("WHERE g.groupstate=0 AND p.planid=g.planid AND ");
		sb.append("p.planid in ( ");
		
		String planIdStr = StringUtils.makeStrCollection(planIds);
		sb.append(planIdStr);
		
		//拼出saturdayscheme&4096>0类似的格式
		sb.append(") AND p." + ReportWebConstants.getTodayPlanScheme()) ;
		sb.append("&");
		sb.append(ReportWebConstants.getNowPlanScheme() + ">0 " );
		

		PartID part = strategy.getPartitions(new PartKeyBDidImpl(userId));
		sb.append("AND EXISTS (SELECT 1 FROM " + part.getTablename() + " u WHERE g.groupid=u.gid AND u.state=0)");
		return findBySql(new GenericRowMapping<Integer>(){

			public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getInt(1);
			}},sb.toString(), new Object[]{}, new int[]{});
	}

	/**
	 * findPauseInSchedualPlanIds: 查询可以投放中但由于未到投放时间的计划ID
	 *
	 * @param userId 用户名的userId
	 * @param planIds 待查询计划IDS
	 * @return 处于暂停投放中的计划IDS列表     
	*/ 
	public List<Integer> findPauseInSchedualPlanIds(int userId,	List<Integer> planIds){
		if (CollectionUtils.isEmpty(planIds)) {
			return new ArrayList<Integer>();
		}
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT distinct g.planid FROM beidou.cprogroup g, beidou.cproplan p " );
		sb.append("WHERE g.groupstate=0 AND p.planid=g.planid AND ");
		sb.append("p.planid in ( ");
		
		String planIdStr = StringUtils.makeStrCollection(planIds);
		sb.append(planIdStr);
		
		//拼出saturdayscheme&4096>0类似的格式
		sb.append(") AND p." + ReportWebConstants.getTodayPlanScheme()) ;
		sb.append("&");
		sb.append(ReportWebConstants.getNowPlanScheme() + "=0 " );
		

		PartID part = strategy.getPartitions(new PartKeyBDidImpl(userId));
		sb.append("AND EXISTS (SELECT 1 FROM " + part.getTablename() + " u WHERE g.groupid=u.gid AND u.state=0)");
		return findBySql(new GenericRowMapping<Integer>(){

			public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getInt(1);
			}},sb.toString(), new Object[]{}, new int[]{});
	}
	
	public PartitionStrategy getStrategy() {
		return strategy;
	}

	public void setStrategy(PartitionStrategy strategy) {
		this.strategy = strategy;
	}
	
	
}
