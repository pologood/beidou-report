package com.baidu.beidou.report.dao.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;

import com.baidu.beidou.olap.service.AccountStatService;
import com.baidu.beidou.olap.vo.CostItem;
import com.baidu.beidou.olap.vo.UserAccountViewItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.dao.ReportDao;
import com.baidu.beidou.report.vo.CostBean;
import com.baidu.beidou.util.DateUtils;

public class DorisDaoImpl implements ReportDao {
	
	@Resource(name="accountStatServiceImpl")
	AccountStatService userStatMgr;
	
	/** 按昨天来比较 */
	static class LastCostComparator implements Comparator<CostBean>{
		
		private int order = 1;
		LastCostComparator(int myOrder) {
			if(myOrder < 0 ) {
				order = -1;
			}
		}

		public int compare(CostBean o1, CostBean o2) {
			if(o2 == null) {
				return order;
			} else if (o1 == null) {
				return -1 * order;
			} else {
				return o1.getLastCost() > o2.getLastCost() ? order : -1 * order;
			}
		}};

		/** 按所有消费来比较 */
		static class AllCostComparator implements Comparator<CostBean>{
			
			private int order = 1;
			AllCostComparator(int myOrder) {
				if(myOrder < 0 ) {
					order = -1;
				}
			}

			public int compare(CostBean o1, CostBean o2) {
				if(o2 == null) {
					return order;
				} else if (o1 == null) {
					return -1 * order;
				} else {
					return o1.getAllCost() > o2.getAllCost() ? order : -1 * order;
				}
			}};
	
	public List<CostBean> getCostBeanByUserIdsOrderByCost(
			List<Integer> userIdList, String orderBy, String order,
			int currPage, int pageSize) {
		
		//1、以所有消费构造输出列表，基于这么一个前提，昨天有消费的用户肯定在所有消费用户群中有他。
		//此处类似左连接。
		Map<Integer, CostBean> tmpResult = new HashMap<Integer, CostBean>();
		List<CostItem> userAllCost = userStatMgr.queryUserTotalCost(userIdList);
		for (CostItem item : userAllCost) {
			Integer userId = item.getUserId();
			CostBean bean = new CostBean();
			bean.setUserId(userId);
			bean.setAllCost((long) item.getCost());
			tmpResult.put(userId, bean);
		}


		int orient = ReportConstants.SortOrder.ASC;
		if (QueryParameterConstant.SortOrder.SORTORDER_DESC.equalsIgnoreCase(order)) {
			orient = ReportConstants.SortOrder.DES;
		}
		
		//2、获取用户昨天消费
		for (Integer userId : userIdList) {
			Date from = DateUtils.getPreviousDay(new Date());
			Date to = from; 
			
			List<UserAccountViewItem> userLastCost = userStatMgr.queryAUserData(userId, from, to, ReportConstants.TU_NONE);
			
			if (!CollectionUtils.isEmpty(userLastCost)) {
				UserAccountViewItem data = userLastCost.get(0);
				CostBean bean = tmpResult.get(userId);
				if (bean != null) {
					bean.setLastCost((long) data.getCost());
				}
			}
		}
		
		//3、排序
		List<CostBean> result = new ArrayList<CostBean>();
		result.addAll(tmpResult.values());
		if(ORDERBY_ALL_COST.equals(orderBy)) {
			Collections.sort(result, new AllCostComparator(orient));
		} else if(ORDERBY_LAST_COST.equals(orderBy)) {
			Collections.sort(result, new LastCostComparator(orient));
		}
		
		//4、分页
		if (currPage >= 0 && pageSize > 0) {
			return ReportWebConstants.subListinPage(result, currPage, pageSize);
		}
		return result;
	}

	public static void main(String[] args) {
		List<CostBean> result = new ArrayList<CostBean>();
		for (int i = 3; i < 6; i++) {
			CostBean b = new CostBean();
			b.setUserId(i);
			b.setAllCost(i);
			b.setLastCost(i);
			result.add(b);
		}

		CostBean b = new CostBean();
		b.setUserId(1);
		b.setAllCost(1);
		b.setLastCost(1);
		result.add(b);
		
		Collections.sort(result, new AllCostComparator(1));
		Collections.sort(result, new AllCostComparator(0));
		Collections.sort(result, new AllCostComparator(-1));
		Collections.sort(result, new LastCostComparator(1));
		Collections.sort(result, new LastCostComparator(0));
		Collections.sort(result, new LastCostComparator(-1));
		System.out.println();
	}
}
