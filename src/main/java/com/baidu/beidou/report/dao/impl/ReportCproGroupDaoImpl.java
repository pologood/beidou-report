package com.baidu.beidou.report.dao.impl;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;

import com.baidu.beidou.cprogroup.constant.CproGroupConstant;
import com.baidu.beidou.cprogroup.vo.CproGroupState;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.dao.ReportCproGroupDao;
import com.baidu.beidou.report.dao.vo.GroupQueryParameter;
import com.baidu.beidou.report.vo.group.ExtGroupViewItem;
import com.baidu.beidou.olap.vo.GroupViewItem;
import com.baidu.beidou.util.BeidouConstant;
import com.baidu.beidou.util.CollectionsUtil;
import com.baidu.beidou.util.CproStateUtil;
import com.baidu.beidou.util.FilterUtils;
import com.baidu.beidou.util.StringUtils;
import com.baidu.beidou.util.dao.GenericRowMapping;


/**
 * <p>ClassName:ReportCproGroupDaoImpl
 * <p>Function: 查询report相关的计划信息
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-9
 * @version  $Id: Exp $
 */
public class ReportCproGroupDaoImpl extends BaseReportDaoImpl implements ReportCproGroupDao {

	private GenericRowMapping<GroupViewItem> mapping = new GenericRowMapping<GroupViewItem>(){

		public GroupViewItem mapRow(ResultSet rs, int rowNum) throws SQLException {
			GroupViewItem item = new GroupViewItem();
			item.setPlanId(rs.getInt(1));
			item.setPlanName(rs.getString(2));
			item.setPlanState(rs.getInt(3));
			item.setBudgetOver(rs.getInt(4));
			
			item.setGroupId(rs.getInt(5));
			item.setGroupName(rs.getString(6));
			item.setState(rs.getInt(7));
			item.setGroupType(rs.getInt(8));
			item.setPrice(rs.getInt(9)/100.0);//单位：元
			
			item.setIsSmart(rs.getInt(10));//单位：元
			
			//生成viewState信息
			CproGroupState groupState = new CproGroupState();
			groupState.setPlanState(item.getPlanState());
			groupState.setBudgetOver(item.getBudgetOver());
			groupState.setGroupState(item.getState());//此处一定要注意，getState表示的当前层级对象(如计划、组)的状态
			
			item.setViewState(CproStateUtil.getCproGroupViewState(groupState));
			item.setViewStateOrder(CproStateUtil.getGroupViewStateOrder(item.getViewState()));
			return item;
		}};
		
		public static List<Integer> unConvertSiteList(final String siteListStr){
			
			List<Integer> siteIdList = new ArrayList<Integer>();
			
			if(StringUtils.isEmpty(siteListStr)){
				return siteIdList;
			}
			
			String[] siteList = siteListStr.split("\\" + CproGroupConstant.FIELD_SEPERATOR);		
			
			if(siteList != null){
				for(String siteIdStr : siteList){
					siteIdList.add(Integer.valueOf(siteIdStr));			
				}
			}
			return siteIdList;
		}
		
		private GenericRowMapping<ExtGroupViewItem> extMapping = new GenericRowMapping<ExtGroupViewItem>(){

			public ExtGroupViewItem mapRow(ResultSet rs, int rowNum) throws SQLException {
				ExtGroupViewItem item = new ExtGroupViewItem();
				item.setPlanId(rs.getInt(1));
				item.setPlanName(rs.getString(2));
				item.setPlanState(rs.getInt(3));
				item.setBudgetOver(rs.getInt(4));
				
				item.setGroupId(rs.getInt(5));
				item.setGroupName(rs.getString(6));
				item.setState(rs.getInt(7));
				item.setGroupType(rs.getInt(8));
				item.setPrice(rs.getInt(9));//单位：分，注意，不同于列表中使用的GroupViewItem
				
				//生成viewState信息
				CproGroupState groupState = new CproGroupState();
				groupState.setPlanState(item.getPlanState());
				groupState.setBudgetOver(item.getBudgetOver());
				groupState.setGroupState(item.getState());//此处一定要注意，getState表示的当前层级对象(如计划、组)的状态
				
				item.setViewState(CproStateUtil.getCproGroupViewState(groupState));
				item.setViewStateOrder(CproStateUtil.getGroupViewStateOrder(item.getViewState()));
				
				//生成site相关信息
				item.setIsAllSite(rs.getInt(10));
				if(CproGroupConstant.GROUP_ALLSITE != item.getIsAllSite()){
					String siteList = rs.getString(11);
					String tradeList = rs.getString(12);
					List<Integer> site = unConvertSiteList(siteList);
					Set<Integer> siteSet = new HashSet<Integer>(site.size());
					siteSet.addAll(site);
					item.setSiteList(siteSet);
					List<Integer> trade = unConvertSiteList(tradeList);
					Set<Integer> tradeSet = new HashSet<Integer>(site.size());
					tradeSet.addAll(trade);
					item.setSiteTradeList(tradeSet);
				}
				return item;
			}};
	
	/*最全的查询SQL
	SELECT p.planid, p.planname, p.planstate, p.budgetover, g.groupid, g.groupname, g.groupstate, g.grouptype, gi.price, g.is_smart
	FROM cproplan p, cprogroup g, cprogroupinfo gi 
	WHERE 
	g.userid=480787 AND g.groupid=gi.groupid AND p.planid=g.planid
	AND g.groupstate in (0,1,2) and g.groupid in (1) and g.grouptype in (1) and g.groupname like '%g%';
	*/
	public List<GroupViewItem> findCproGroupReportInfo(GroupQueryParameter queryParam) {
		StringBuilder sb = new StringBuilder();
		List<Object> params = new ArrayList<Object>(); 
		List<Integer> paramsType = new ArrayList<Integer>(); 

		sb.append("SELECT p.planid, p.planname, p.planstate, p.budgetover, g.groupid, g.groupname, g.groupstate, g.grouptype, gi.price, g.is_smart");
		
		makeFilter(sb, params, paramsType, queryParam);

		//排序处理
		String col = queryParam.getSortColumn();
		if(!StringUtils.isEmpty(col)) {
			sb.append(" ORDER BY " + (ReportWebConstants.isGBKColumn(col) ? 
					ReportWebConstants.generateGBKSortString( col ) : col));
			if (BeidouConstant.SORTORDER_DESC.equalsIgnoreCase(queryParam.getSortOrder())) {
				sb.append(" " + queryParam.getSortOrder());
			}
		}
		
		//设置分页信息，由service层传递的参数来保证是否需要分页，如count*不需要分页则之前就将page,pagesize设置成空
		if (queryParam.getPage() != null && queryParam.getPage() >= 0
				&& queryParam.getPageSize() != null && queryParam.getPageSize() > 0) {
			
			return findBySql(mapping, sb.toString(), params.toArray(), 
					CollectionsUtil.tranformIntegerListToIntArray(paramsType), queryParam.getPage(), 
					queryParam.getPageSize());
		}
		
		return this.findBySql(mapping, sb.toString(), params.toArray(), 
				CollectionsUtil.tranformIntegerListToIntArray(paramsType));
	}
	
	public List<ExtGroupViewItem> findExtCproGroupReportInfo(GroupQueryParameter queryParam) {
		StringBuilder sb = new StringBuilder();
		List<Object> params = new ArrayList<Object>(); 
		List<Integer> paramsType = new ArrayList<Integer>(); 

		sb.append("SELECT p.planid, p.planname, p.planstate, p.budgetover, g.groupid, " +
				"g.groupname, g.groupstate, g.grouptype, gi.price, gi.isallsite, gi.sitelist, gi.sitetradelist");
		
		makeFilter(sb, params, paramsType, queryParam);
		
		//设置分页信息，由service层传递的参数来保证是否需要分页，如count*不需要分页则之前就将page,pagesize设置成空
		if (queryParam.getPage() != null && queryParam.getPage() >= 0
				&& queryParam.getPageSize() != null && queryParam.getPageSize() > 0) {
			
			return findBySql(extMapping, sb.toString(), params.toArray(), 
					CollectionsUtil.tranformIntegerListToIntArray(paramsType), queryParam.getPage(), 
					queryParam.getPageSize());
		}
		
		return this.findBySql(extMapping, sb.toString(), params.toArray(), 
				CollectionsUtil.tranformIntegerListToIntArray(paramsType));
	}
	
	public int countCproGroupReportInfo(GroupQueryParameter queryParam) {

		StringBuilder sb = new StringBuilder();
		List<Object> params = new ArrayList<Object>(); 
		List<Integer> paramsType = new ArrayList<Integer>(); 

		sb.append("SELECT count(1) ");
		
		makeFilter(sb, params, paramsType, queryParam);

		return this.findBySql(new GenericRowMapping<Integer>(){

			public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getInt(1);
			}}, sb.toString(), params.toArray(), 
				CollectionsUtil.tranformIntegerListToIntArray(paramsType)).get(0);
	}

	/**
	 * makeFilter: 构造查询过滤条件
	 *
	 * @param sb sql串
	 * @param params sql中的参数
	 * @param paramsType sql中的参数的类型
	 * @param queryParam 查询条件VO
	*/
	protected void makeFilter(StringBuilder sb, List<Object> params, List<Integer> paramsType, 
			GroupQueryParameter queryParam) {

		sb.append(" FROM beidou.cproplan p, beidou.cprogroup g, beidou.cprogroupinfo gi ");
		sb.append(" WHERE g.userid=? AND g.groupid=gi.groupid AND p.planid=g.planid");
		params.add(queryParam.getUserId());
		paramsType.add(Types.INTEGER);
		makePlanStateFilter(sb, queryParam.getPlanState(), queryParam.isBudgetOver(), queryParam.isIncludePlanState());
		makeGroupFilter(sb, params, paramsType, queryParam);
	}

	/**
	 * makeGroupFilter: 生成需要过滤推广组相关信息的Sql过滤语句
	 * 里面的过滤项全部是g.XXXX(Group的字段)
	 * @param sb 
	 * @param params
	 * @param paramsType
	 * @param queryParam
	 * @param needSort      
	*/
	protected void makeGroupFilter(StringBuilder sb, List<Object> params, List<Integer> paramsType, 
			GroupQueryParameter queryParam) {
		if(queryParam.getPlanId() != null && queryParam.getPlanId() > 0) {
			//设置planid
			sb.append(" AND g.planid=?");
			params.add(queryParam.getPlanId());
			paramsType.add(Types.INTEGER);
		}
		
		makeGroupStateFilter(sb, queryParam.getState(), queryParam.isStateInclude());
		
		if (!CollectionUtils.isEmpty(queryParam.getIds())) {
			String idStr = StringUtils.makeStrCollection(queryParam.getIds());
			sb.append(" AND g.groupid in (" + idStr +")");
		}
		if ( !ArrayUtils.isEmpty( queryParam.getGroupType()) ) {
			int multiBitsGroupType = 0;
			for (Integer groupType : queryParam.getGroupType()) {
				multiBitsGroupType |= groupType;
			}
			sb.append(" AND g.grouptype &" + multiBitsGroupType);
			
//			if (queryParam.getGroupType().length == 1) {
//				sb.append(" AND g.grouptype=" + queryParam.getGroupType()[0]);
//			} else {
//				sb.append(" AND g.grouptype in (");
//				sb.append(StringUtils.makeStrFromArray(queryParam.getGroupType(), ","));
//				sb.append(") ");
//			}
		}
		if(!ArrayUtils.isEmpty(queryParam.getTargetType())){
			int multiBitTargetType = 0;
			for (Integer targetType : queryParam.getTargetType()) {
				multiBitTargetType |= targetType;
			}
			if (ArrayUtils.contains(queryParam.getTargetType(), CproGroupConstant.GROUP_TARGET_TYPE_NONE)
					&&
				multiBitTargetType > 0
			) {
				sb.append(" AND (g.targettype & ");
				sb.append(multiBitTargetType);
				sb.append(" or g.targettype in (");
				sb.append(CproGroupConstant.GROUP_TARGET_TYPE_NONE).append(",");
				sb.append(CproGroupConstant.GROUP_TARGET_TYPE_AT_RIGHT).append(",");
				sb.append(CproGroupConstant.GROUP_TARGET_TYPE_IT);
				sb.append("))");
			} else if (multiBitTargetType > 0) {
				sb.append(" AND g.targettype &" + multiBitTargetType);
			} else {
				sb.append(" AND g.targettype in (");
				sb.append(CproGroupConstant.GROUP_TARGET_TYPE_NONE).append(",");
				sb.append(CproGroupConstant.GROUP_TARGET_TYPE_AT_RIGHT).append(",");
				sb.append(CproGroupConstant.GROUP_TARGET_TYPE_IT);
				sb.append(")");
			}
			
//			if(queryParam.getTargetType().length ==1){
//				sb.append(" AND g.targettype=" + queryParam.getTargetType()[0]);
//			}else{
//				sb.append(" AND g.targettype in (");
//				sb.append(StringUtils.makeStrFromArray(queryParam.getTargetType(), ","));
//				sb.append(") ");
//			}
		}
		if (!org.apache.commons.lang.StringUtils.isEmpty(queryParam.getName())) {
			sb.append(" AND g.groupname like '%" + FilterUtils.transferSpecialInSqlParam(queryParam.getName()) + "%'");
		}
	}
	
	public List<Integer> findAllCproGroupIds(GroupQueryParameter queryParam) {

		StringBuilder sb = new StringBuilder();
		List<Object> params = new ArrayList<Object>(); 
		List<Integer> paramsType = new ArrayList<Integer>(); 

		sb.append("SELECT g.groupid ");
		
		makeFilter(sb, params, paramsType, queryParam);
		
		//不需要分页
		return this.findBySql(new GenericRowMapping<Integer>(){

			public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getInt(1);
			}}, sb.toString(), params.toArray(), 
				CollectionsUtil.tranformIntegerListToIntArray(paramsType));
	}

	public long countGroupByPlan(Integer planId) {
		return super.countByCondition("select count(*) from beidou.cprogroup where planid=?", new Object[]{planId});
	}

	public long countGroupByUser(Integer userId) {
		return super.countByCondition("select count(*) from beidou.cprogroup where userid=?", new Object[]{userId});
	}
}
