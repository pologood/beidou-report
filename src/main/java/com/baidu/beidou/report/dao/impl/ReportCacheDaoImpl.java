package com.baidu.beidou.report.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.dao.ReportCacheDao;
import com.baidu.beidou.report.dao.ReportDao;
import com.baidu.beidou.report.vo.CostBean;
import com.baidu.beidou.report.vo.ExtendCostBean;
import com.baidu.beidou.stat.util.DateUtil;
import com.baidu.beidou.util.DateUtils;
import com.baidu.beidou.util.StringUtils;
import com.baidu.beidou.util.dao.BaseDaoSupport;
import com.baidu.beidou.util.dao.GenericRowMapping;

public class ReportCacheDaoImpl extends BaseDaoSupport implements ReportCacheDao {
	
	protected Log logger = LogFactory.getLog(getClass());
	/** 存放在beidoureport.sysnvtab中的缓存最后更新时间key */
	protected final static String LAST_MODIFY_CACHE_DATE = "CACHE_LAST_UPD_TIME"; 
	
	private static final String TABLE_NAME_USER_YEST = "beidoureport.stat_user_yest";
	
	//如下三张表废弃
//	private static final String TABLE_NAME_PLAN_YEST = "beidoureport.stat_plan_yest";
//	private static final String TABLE_NAME_GROUP_YEST = "beidoureport.stat_group_yest";
//	private static final String TABLE_NAME_UNIT_YEST = "beidoureport.stat_unit_yest";
	
	private static final String TABLE_NAME_USER_ALL = "beidoureport.stat_user_all";
	private static final String TABLE_NAME_SYSNVTAB = "beidoureport.sysnvtab";
	private static final String TABLE_NAME_REALTIME_STAT_USER = "beidoureport.realtime_stat_user";
	
	
	GenericRowMapping<CostBean> costBeanMapping = new GenericRowMapping<CostBean>() {

		public CostBean mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			CostBean bean = new CostBean();
			bean.setUserId(rs.getInt(1));
			bean.setAllCost(rs.getInt(2));
			bean.setLastCost(rs.getInt(3));
			return bean;
		}};
		
		/** 统计字段映射 */
	class StatMapping implements GenericRowMapping<Map<String, Object>> {
		
		/** 放在Map中的ID标识名，如ReportConstants.GROUP */
		String idKey;
		Date from;
		Date to;
		
		StatMapping(String idKey, Date from, Date to) {
			this.idKey = idKey;
			this.from = from;
			this.to = to;
		}

		public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {


			long srchs = rs.getLong(2);
			long cost = rs.getLong(3);
			long clks = rs.getLong(4);
			
			Map<String, Object> item = new HashMap<String, Object>();

			item.put(ReportConstants.SRCHS, srchs);
			item.put(ReportConstants.CLKS, clks);
			item.put(ReportConstants.COST, cost);

			
			if (0 != srchs){
				double ctr = (clks * 1.0) / srchs ;
				double cpm = (cost * 1000.0) / ( srchs * 100.0 );//cpm需要以元（系统中为分）为单位，再*1000
				if (clks > 0) {
					item.put(ReportConstants.CTR, ctr);
				} else {
					item.put(ReportConstants.CTR, null);
				}
				if( cost > 0 ) {
					item.put(ReportConstants.CPM, cpm);
				} else {
					item.put(ReportConstants.CPM, null);
				}
			} else {
				item.put(ReportConstants.CTR, null);
				item.put(ReportConstants.CPM, null);
			}
			
			if (0 != clks){
				double acp = cost / ( clks * 100.0 ) ;//acp，以元为单位（统计中存的是分）
				item.put(ReportConstants.ACP, acp);
			} else {
				item.put(ReportConstants.ACP, null);
			}
			//为时木硬编码
			if(ReportConstants.UNIT.equals(idKey)){
				item.put(idKey, rs.getLong(1));
			}else{
				item.put(idKey, rs.getInt(1));
			}
			item.put(ReportConstants.FROMDATE, from);
			item.put(ReportConstants.TODATE, to);
			
			return item;
		}
	}
	
	@Override
	public Date queryStatCacheLastModifiedDate() {
		logger.info("queryStatCacheLastModifiedDate method is excuting");
		List<Map<String, Object>> list = super.findBySql(
				"SELECT value FROM " + TABLE_NAME_SYSNVTAB + " WHERE NAME=? LIMIT 1",
				new Object[] { LAST_MODIFY_CACHE_DATE }, new int[] { Types.VARCHAR });
		if (list.isEmpty()) {
			return null;
		} else {
			String dateString = list.get(0).get("value").toString();
			try {
				return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateString);
			} catch (ParseException e) {
				logger.error("error to parse cache date string[" + dateString + "] in beidoureport.sysnvtab ");
			}
		}
		return null;
	}
	
	@Override
	public Map<Integer, Long> getAllCostByUserIds(List<Integer> userIdList) {
		// TODO Auto-generated method stub
		
		Map<Integer, Long> result = new HashMap<Integer, Long>();
		if(CollectionUtils.isEmpty(userIdList)) {
			return result;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT userid, cost FROM " + TABLE_NAME_USER_ALL + " WHERE userid in(");
		sb.append(StringUtils.makeStrCollection(userIdList));
		sb.append(")");
		
		List<Map<String, Object>> temp = findByCondition(sb.toString(), null);
		if (temp != null && temp.size()>0){
			for(Map<String,Object> map : temp){
				result.put((Integer)map.get("userid"), (Long)map.get("cost"));
			}
		}
		
		return result;
	}
	
	
	@Override
	public List<CostBean> getCostBeanByUserIdsOrderByCost(List<Integer> userIds, 
			String orderBy, String order, int page, int pageSize) {
		logger.info("getCostBeanByUserIdsOrderByCost method is excuting");
		if(CollectionUtils.isEmpty(userIds)) {
			return new ArrayList<CostBean>();
		}
		String orderByName = "";
		if (ReportDao.ORDERBY_LAST_COST.equalsIgnoreCase(orderBy)) {
			orderByName = " order by a.cost";
		} else if(ReportDao.ORDERBY_ALL_COST.equalsIgnoreCase(orderBy)) {
			orderByName = " order by b.cost";
		} 
		String orderName = "";
		if (QueryParameterConstant.SortOrder.SORTORDER_DESC.equalsIgnoreCase(order)) {
			orderName = " " + QueryParameterConstant.SortOrder.SORTORDER_DESC;
		}
		
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT a.userid, a.cost, b.cost FROM " + TABLE_NAME_USER_ALL + " a LEFT JOIN " + TABLE_NAME_USER_YEST);
		sb.append(" b ON a.userid = b.userid WHERE a.userid in(");
		sb.append(StringUtils.makeStrCollection(userIds));
		sb.append(")");
		sb.append(orderByName);
		sb.append(orderName);
		if (page >= 0 && pageSize > 0) {
			return findBySql(costBeanMapping, sb.toString(), new Object[]{}, new int[]{}, page, pageSize);
		}
		return findBySql(costBeanMapping, sb.toString(), new Object[]{}, new int[]{});
		
	}
	
	/**
	 * checkParams: 检查查询参数
	 *
	 * @return true表示查询参数有效，false表示无效     
	 * @since 
	*/
	protected boolean checkParams(Date from, Date to) {

		if (from == null || to == null) {
			return false;
		}
			// 用户的总消费数据、昨天的Plan、Group、Unit数据则取缓存的数据，
		Date yesteray = DateUtils.getPreviousDay(new Date());
		return DateUtil.isSameDay(from, yesteray)
					&& DateUtil.isSameDay(to, yesteray);
	}

	@Override
	public List<ExtendCostBean> getExtendCostBeanByUserIds(
			List<Integer> userIdList) {
		logger.info("getExtendCostBeanByUserIds method is excuting");
		List<ExtendCostBean> retlist = new ArrayList<ExtendCostBean>();
		if (CollectionUtils.isEmpty(userIdList)) {
			return retlist;
		}
		//50 条
		final Map<Integer,ExtendCostBean> retMap = new HashMap<Integer,ExtendCostBean>(userIdList.size());
		String queryPara = StringUtils.makeStrCollection(userIdList);
		
		StringBuilder yest = new StringBuilder();
		yest.append("select userid, last_day_growth, last_week_day_growth, srchs, cost from ");
		yest.append(TABLE_NAME_USER_YEST);
		yest.append(" where userid in( ");
		yest.append(queryPara);
		yest.append(" )");
		
		findBySql(new GenericRowMapping<ExtendCostBean>() {

			public ExtendCostBean mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				ExtendCostBean bean = new ExtendCostBean();
				bean.setUserId(rs.getInt(1));
				bean.setLastDayGrowth(rs.getInt(2));
				bean.setLastWeekDayGrowth(rs.getInt(3));
				bean.setYestSrchs(rs.getLong(4));
				bean.setYestCost(rs.getLong(5));
				retMap.put(bean.getUserId(), bean);
				return null;
			}
		}, yest.toString(), new Object[] {}, new int[] {});
		
		StringBuilder today = new StringBuilder();
		today.append("select userid, cost from ");
		today.append(TABLE_NAME_REALTIME_STAT_USER);
		today.append(" where userid in( ");
		today.append(queryPara);
		today.append(" )");
		findBySql(new GenericRowMapping<ExtendCostBean>() {
			public ExtendCostBean mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				Integer userid = rs.getInt(1);
				ExtendCostBean extendCostBean = retMap.get(userid);
				if(extendCostBean == null){
					ExtendCostBean bean = new ExtendCostBean();
					bean.setUserId(userid);
					bean.setTodayCost(rs.getLong(2));
					retMap.put(userid, bean);
				}else{
					extendCostBean.setTodayCost(rs.getLong(2));
				}
				return null;
			}
		}, today.toString(), new Object[] {}, new int[] {});
		
		if(retMap.size() > 0){
			for(Map.Entry<Integer,ExtendCostBean> en : retMap.entrySet()){
				retlist.add(en.getValue());
			}
		}
		return retlist;
	}

}
