package com.baidu.beidou.report.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.dao.ReportCacheDao;
import com.baidu.beidou.report.dao.ReportDao;
import com.baidu.beidou.report.service.ReportRpcService;
import com.baidu.beidou.report.vo.CostBean;
import com.baidu.beidou.report.vo.ExtendCostBean;
import com.baidu.beidou.util.LogUtils;

public class ReportRpcServiceImpl implements ReportRpcService {

	protected ReportDao reportDao;
	
	protected ReportCacheDao reportCacheDao;
	
	private Log log = LogFactory.getLog(getClass());
	

	public Map<Integer, Long> getAllCostByUserIds(List<Integer> userIdList) {
		// TODO Auto-generated method stub
		return reportCacheDao.getAllCostByUserIds(userIdList);
	}

	public Map<Integer, CostBean> getCostBeanByUserIds(List<Integer> userIdList) {
		Map<Integer, CostBean> map = new HashMap<Integer, CostBean>();
		try {
			List<CostBean> result = reportDao.getCostBeanByUserIdsOrderByCost(userIdList, null, null, 0, 0);
			if (result != null) {
				for (CostBean bean : result) {
					map.put(bean.getUserId(), bean);
				}
			}
			return map;
		} catch (Exception e) {
			LogUtils.error(log, e);
			return map;
		}
	}

	public List<CostBean> getCostBeanByUserIdsOrderAllCost(
			List<Integer> userIdList, boolean isAsc, int currPage, int pageSize) {
		String order = isAsc ? "" : QueryParameterConstant.SortOrder.SORTORDER_DESC;
		String column = ReportDao.ORDERBY_ALL_COST;
		try {
			return reportDao.getCostBeanByUserIdsOrderByCost(userIdList, column, order, currPage, pageSize);
		} catch (Exception e) {
			LogUtils.error(log, e);
			return new ArrayList<CostBean>();
		}
	}

	public List<CostBean> getCostBeanByUserIdsOrderLastCost(
			List<Integer> userIdList, boolean isAsc, int currPage, int pageSize) {
		String order = isAsc ? "" : QueryParameterConstant.SortOrder.SORTORDER_DESC;
		String column = ReportDao.ORDERBY_LAST_COST;
		try {
			return reportDao.getCostBeanByUserIdsOrderByCost(userIdList, column, order, currPage, pageSize);
		} catch (Exception e) {
			LogUtils.error(log, e);
			return new ArrayList<CostBean>();
		}
	}

	public Map<Integer, ExtendCostBean> getExtendCostBeanByUserIds(
			List<Integer> userIdList) {
		Map<Integer, ExtendCostBean> map = new HashMap<Integer, ExtendCostBean>();
		try {
			List<ExtendCostBean> result = reportCacheDao.getExtendCostBeanByUserIds(userIdList);
			if (result != null) {
				for (ExtendCostBean bean : result) {
					map.put(bean.getUserId(), bean);
				}
			}
			return map;
		} catch (Exception e) {
			LogUtils.error(log, e);
			return map;
		}
	}

	public ReportDao getReportDao() {
		return reportDao;
	}

	public void setReportDao(ReportDao reportDao) {
		this.reportDao = reportDao;
	}

	public ReportCacheDao getReportCacheDao() {
		return reportCacheDao;
	}

	public void setReportCacheDao(ReportCacheDao reportCacheDao) {
		this.reportCacheDao = reportCacheDao;
	}

}
