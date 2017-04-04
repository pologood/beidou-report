package com.baidu.beidou.report.dao.impl;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;

import com.baidu.beidou.cprounit.vo.CproUnitState;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.dao.ReportCproUnitDao;
import com.baidu.beidou.report.dao.vo.UnitQueryParameter;
import com.baidu.beidou.olap.vo.UnitViewItem;
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
 * <p>ClassName:ReportCproGroupDaoImpl
 * <p>Function: 查询report相关的计划信息
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-3-9
 * @version  $Id: Exp $
 */
public class ReportCproUnitDaoImpl extends BaseReportDaoImpl implements ReportCproUnitDao {

	private PartitionStrategy strategy = null;

	private GenericRowMapping<UnitViewItem> mapping = new GenericRowMapping<UnitViewItem>(){

		public UnitViewItem mapRow(ResultSet rs, int rowNum) throws SQLException {
			UnitViewItem item = new UnitViewItem();
			item.setPlanId(rs.getInt(1));
			item.setPlanName(rs.getString(2));
			item.setPlanState(rs.getInt(3));
			item.setBudgetOver(rs.getInt(4));
			
			item.setGroupId(rs.getInt(5));
			item.setGroupName(rs.getString(6));
			item.setGroupState(rs.getInt(7));
			item.setGroupType(rs.getInt(8));
			
			item.setUnitId(rs.getLong(9));
			item.setState(rs.getInt(10));
			item.setWuliaoType(rs.getInt(11));
			item.setTitle(rs.getString(12));
			item.setDescription1(rs.getString(13));
			item.setDescription2(rs.getString(14));
			item.setShowUrl(rs.getString(15));
			item.setTargetUrl(rs.getString(16));
			item.setWidth(rs.getInt(17));
			item.setHeight(rs.getInt(18));
			item.setSrc(rs.getString(19));
//			item.setRefuseReason(rs.getString(20));
			item.setPromotionType(rs.getInt(20));
			item.setMcId(rs.getLong(21));
			item.setVersionId(rs.getInt(22));
			item.setIsSmart(rs.getInt(23));
			item.setTemplateId(rs.getInt(24));
			
			//生成viewState信息
			CproUnitState unitState = new CproUnitState();
			unitState.setPlanState(item.getPlanState());
			unitState.setBudgetOver(item.getBudgetOver());
			unitState.setGroupState(item.getGroupState());//此处一定要注意，getState表示的当前层级对象(如计划、组)的状态
			unitState.setUnitState(item.getState());
			
			item.setViewState(CproStateUtil.getCproUnitViewState(unitState));
			item.setViewStateOrder(CproStateUtil.getUnitViewStateOrder(item.getViewState()));
			return item;
		}};
	
	/*最全的查询SQL
	SELECT p.planid, p.planname, p.planstate, p.budgetover, g.groupid, g.groupname, g.groupstate, g.grouptype, us.id, us.state, 
	um.wuliaotype, um.titile, um.description1, um.description2, um.showurl, um.targeturl, um.width, um.height, um.filesrc,ua.refuserea  
	FROM cproplan p, cprogroup g, cprounitstateX us 
	LEFT JOIN  cprounitmaterX um on us.id=um.id 
	LEFT JOIN unitauditingX ua on us.id=ua.id 
	WHERE us.uid=? AND us.pid=p.planid AND us.gid=g.groupid AND g.grouptype in {?} AND us.id in(?)
	 AND us.state in (?) AND um.wuliaotype=? AND
	 (um.title LIKE ‘%?%’ OR um.description1 LIKE ‘%?%’ OR um.description2 LIKE ‘%?%’ OR um.showurl LIKE ‘%?%’ OR um.targeturl LIKE ‘%?%’);
	*/
	public List<UnitViewItem> findCproUnitReportInfo(UnitQueryParameter queryParam) {
		StringBuilder sb = new StringBuilder();
		List<Object> params = new ArrayList<Object>(); 
		List<Integer> paramsType = new ArrayList<Integer>(); 

		sb.append("SELECT p.planid, p.planname, p.planstate, p.budgetover, g.groupid, g.groupname, g.groupstate, g.grouptype,");
		sb.append(" us.id, us.state, um.wuliaotype, um.title, um.description1, um.description2,");
		sb.append(" um.showurl, um.targeturl, um.width, um.height, um.filesrc, p.promotion_type, um.mcId, um.mcVersionId, g.is_smart, um.template_id");

		PartID part = strategy.getPartitions(new PartKeyBDidImpl(queryParam.getUserId()));
		sb.append(" FROM beidou.cproplan p, beidou.cprogroup g, beidou.cprounitstate" + part.getId() +" us ");
		sb.append(" LEFT JOIN beidou.cprounitmater" + part.getId() +" um ON us.id=um.id ");
		sb.append(" WHERE us.uid=? AND g.groupid=us.gid AND p.planid=us.pid ");
		params.add(queryParam.getUserId());
		paramsType.add(Types.INTEGER);
		
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
		
		//如果page<1则表示第一次发起请求，需要取全部 的数据
		if (queryParam.getPage() != null && queryParam.getPage() >= 0 
				&& queryParam.getPageSize() != null && queryParam.getPageSize() > 0) {
			return findBySql(mapping, sb.toString(), params.toArray(), 
					CollectionsUtil.tranformIntegerListToIntArray(paramsType), queryParam.getPage(), 
					queryParam.getPageSize());
		}
		
		return this.findBySql(mapping, sb.toString(), params.toArray(), 
				CollectionsUtil.tranformIntegerListToIntArray(paramsType));
	}
	

	public Map<Long, String> findCproUnitAuditInfo(Collection<Long> unitIds, int userId) {
		final Map<Long, String> result = new HashMap<Long, String>();
		if(CollectionUtils.isEmpty(unitIds)) {
			return result;
		}
		StringBuilder sb = new StringBuilder();

		sb.append("SELECT ua.unitid, ua.refuserea");

		PartID part = strategy.getPartitions(new PartKeyBDidImpl(userId));
		sb.append(" FROM beidou.unitauditing" + part.getId() + " ua ");
		sb.append(" WHERE ua.unitid in (");
		sb.append(StringUtils.makeStrFromCollection(unitIds, ","));
		sb.append(")");
		
		findBySql(new GenericRowMapping<Long>(){

			public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
				result.put(rs.getLong(1), rs.getString(2));
				return null;
			}},sb.toString(), new Object[]{}, new int[]{});
		
		return result;
	}
	
	public int countCproUnitReportInfo(UnitQueryParameter queryParam) {

		StringBuilder sb = new StringBuilder();
		List<Object> params = new ArrayList<Object>(); 
		List<Integer> paramsType = new ArrayList<Integer>(); 

		sb.append("SELECT count(1)");

		PartID part = strategy.getPartitions(new PartKeyBDidImpl(queryParam.getUserId()));
		sb.append(" FROM  beidou.cproplan p, beidou.cprogroup g, beidou.cprounitstate" + part.getId() +" us ");
		sb.append(" LEFT JOIN beidou.cprounitmater" + part.getId() +" um ON us.id=um.id ");
		sb.append(" WHERE us.uid=? AND p.planid=us.pid AND g.groupid=us.gid ");
		params.add(queryParam.getUserId());
		paramsType.add(Types.INTEGER);
		
		makeFilter(sb, params, paramsType, queryParam);

		return this.findBySql(new GenericRowMapping<Integer>(){

			public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getInt(1);
			}}, sb.toString(), params.toArray(), 
				CollectionsUtil.tranformIntegerListToIntArray(paramsType)).get(0);
	}

	public void makeFilter(StringBuilder sb, List<Object> params, List<Integer> paramsType, UnitQueryParameter queryParam) {
//		AND g.grouptype in {?} AND us.id in(?)
//		 AND us.state in (?) AND um.wuliaotype=? AND
//		 (um.title LIKE ‘%?%’ OR um.description1 LIKE ‘%?%’ OR um.description2 LIKE ‘%?%’ OR um.showurl LIKE ‘%?%’ OR um.targeturl LIKE ‘%?%’);
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
		
		makePlanStateFilter(sb, queryParam.getPlanState(), queryParam.isBudgetOver(), queryParam.isIncludePlanState());
		makeGroupStateFilter(sb, queryParam.getGroupState(), queryParam.isIncludeGroupState());
		makeUnitStateFilter(sb, queryParam.getState(), queryParam.isStateInclude());
		
		if(queryParam.getPlanId() != null) {
			sb.append(" AND us.pid=" + queryParam.getPlanId());
		}
		
		if(queryParam.getGroupId() != null) {
			sb.append(" AND us.gid=" + queryParam.getGroupId());
		}
		
		if (!CollectionUtils.isEmpty(queryParam.getIds())) {
			String idStr = StringUtils.makeStrCollection(queryParam.getIds());
			sb.append(" AND us.id in (" + idStr +")");
		}

		if ( queryParam.getWuliaoType() != null ) {
			if (queryParam.getWuliaoType().length == 1) {
				sb.append(" AND um.wuliaotype = " + queryParam.getWuliaoType()[0]);
			} else {
				sb.append(" AND um.wuliaotype in (" + StringUtils.makeStrFromArray(queryParam.getWuliaoType(), ",") + ")");
			}
		}
		
		//增加物料尺寸过滤
		if(queryParam.getWidth()!=null && queryParam.getHeight()!=null){
			sb.append(" AND um.height=" + queryParam.getHeight() + " AND um.width=" + queryParam.getWidth());
		}
		
		//增加计划推广类型过滤
		if (null != queryParam.getPromotionType()) {
			sb.append(" AND p.promotion_type = " + queryParam.getPromotionType());
		}
		
		if (!org.apache.commons.lang.StringUtils.isEmpty(queryParam.getName())) {
			String keyword = FilterUtils.transferSpecialInSqlParam(queryParam.getName());
			sb.append(" AND ( um.title like '%" + keyword + "%'");
			sb.append(" OR um.description1 like '%" + keyword + "%'");
			sb.append(" OR um.description2 like '%" + keyword + "%'");
			sb.append(" OR um.showurl like '%" + keyword + "%'");
			sb.append(" OR um.targeturl like '%" + keyword + "%')");
		}
	}

	public List<Long> findAllCproUnitIds(UnitQueryParameter queryParam) {

		StringBuilder sb = new StringBuilder();
		List<Object> params = new ArrayList<Object>(); 
		List<Integer> paramsType = new ArrayList<Integer>(); 

		sb.append("SELECT us.id ");

		PartID part = strategy.getPartitions(new PartKeyBDidImpl(queryParam.getUserId()));
		sb.append(" FROM beidou.cproplan p, beidou.cprogroup g, beidou.cprounitstate" + part.getId() +" us ");
		sb.append(" LEFT JOIN beidou.cprounitmater" + part.getId() +" um ON us.id=um.id ");
		sb.append(" WHERE us.uid=? AND p.planid=us.pid AND g.groupid=us.gid ");
		params.add(queryParam.getUserId());
		paramsType.add(Types.INTEGER);
		
		makeFilter(sb, params, paramsType, queryParam);

		return this.findBySql(new GenericRowMapping<Long>(){

			public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getLong(1);
			}}, sb.toString(), params.toArray(), 
				CollectionsUtil.tranformIntegerListToIntArray(paramsType));
	}

	public PartitionStrategy getStrategy() {
		return strategy;
	}

	public void setStrategy(PartitionStrategy strategy) {
		this.strategy = strategy;
	}
}
