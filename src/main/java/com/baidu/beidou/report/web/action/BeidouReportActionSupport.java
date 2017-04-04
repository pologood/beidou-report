package com.baidu.beidou.report.web.action;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.CollectionUtils;

import com.baidu.beidou.aot.control.AotReportItem;
import com.baidu.beidou.olap.vo.AbstractViewItem;
import com.baidu.beidou.olap.vo.ReportDayViewItem;
import com.baidu.beidou.period.bo.Report;
import com.baidu.beidou.period.dto.VirtualReport;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.exception.ParameterInValidException;
import com.baidu.beidou.report.facade.TransReportFacade;
import com.baidu.beidou.report.service.BeidouAotService;
import com.baidu.beidou.report.util.ReportUtil;
import com.baidu.beidou.report.vo.QueryParameter;
import com.baidu.beidou.report.vo.ReportStatusItem;
import com.baidu.beidou.report.vo.StatInfo;
import com.baidu.beidou.stat.driver.bo.Constant.DorisDataType;
import com.baidu.beidou.stat.facade.ReportFacade;
import com.baidu.beidou.stat.facade.UvReportFacade;
import com.baidu.beidou.stat.service.HolmesDataService;
import com.baidu.beidou.stat.service.TransDataService;
import com.baidu.beidou.stat.service.UvDataService;
import com.baidu.beidou.stat.util.DateUtil;
import com.baidu.beidou.user.bo.Visitor;
import com.baidu.beidou.user.constant.UserWebConstant;
import com.baidu.beidou.user.service.UserInfoMgr;
import com.baidu.beidou.user.service.UserMgr;
import com.baidu.beidou.user.web.aware.VisitorAware;
import com.baidu.beidou.util.BeidouConstant;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.DateScaleHelper;
import com.baidu.beidou.util.DateUtils;
import com.baidu.beidou.util.vo.AbsJsonObject;
import com.opensymphony.xwork2.ActionSupport;

/**
 * 报告列表的Action基类
 * 
 * @author yanjie
 */
public abstract class BeidouReportActionSupport extends ActionSupport implements
		VisitorAware {

	private static final long serialVersionUID = 1116584041553970033L;
	protected Log log = LogFactory.getLog(getClass());
	private int SHOW_SUB_SITE_CNT = 50;
	protected static SimpleDateFormat sd = new SimpleDateFormat("yyyyMMdd");
	protected static SimpleDateFormat sd1 = new SimpleDateFormat("yyyy-MM-dd");
	protected static SimpleDateFormat sd2 = new SimpleDateFormat("yyyy/MM/dd");

	/** 文件编码，在文件下载时需要使用 */
	protected String fileEncoding = ReportWebConstants.DEFAULT_FILE_ENCODING;

	protected int page = 0;

	// 用于statService查询
	/** 前端传递过来的实际开始日期 */
	protected Date from;
	/** 前端传递过来的实际结束日期 */
	protected Date to;

	/**
	 * “分日图表”flash需要显示的开始日期， 因为Flash需要至少显示七天的数据，所以，当from距今天的日期小于7天时要进行自动扩展
	 */
	protected Date flashFrom;
	/** 类似flashFrom，表示结束日期，通过该日期和To相同。 */
	protected Date flashTo;
	protected QueryParameter qp;
	protected Visitor visitor;
	/** sfUserId */
	protected Integer userId;

	/** aot服务接口 */
	protected BeidouAotService aotService;
	protected UserMgr userMgr = null;
	protected UserInfoMgr userInfoMgr = null;

	protected TransReportFacade transReportFacade;

	protected TransDataService transDataService;

	/**
	 * added by liuhao05 since cpweb-492
	 */
	/** UV数据的查询接口 */
	protected UvDataService uvDataService;

	/** Holmes数据的查询接口 */
	protected HolmesDataService holmesDataService;

	/** UV数据的Facade接口 */
	protected UvReportFacade uvReportFacade;

	/** Report的Facade接口 */
	protected ReportFacade reportFacade;

	protected AbsJsonObject jsonObject = new AbsJsonObject();;

	// －－－－－－－－－－－－－以下内容为下载使用-----------------
	/** 异常情况下返回的具体信息，如js的<script>alert()</script> */
	protected String content;

	/**
	 * 返回给用户下载的文件流，
	 */
	protected InputStream inputStream = new ByteArrayInputStream(
			"ERROR".getBytes());
	/**
	 * 返回给用户下载的文件大小
	 */
	protected long fileSize;
	/** 待下载文件的名字 */
	protected String fileName = "test.xls";

	/** 下载出错时的回调提示方法名 */
	protected String __callbackName__ = "downloadError";

	/********************* helpers **********************************/

	public void validate() {
		if (userId == null || userId == 0) {
			String errMsg = this.getText("errors.required",
					new String[] { "userId" });
			this.addFieldError("用户信息", errMsg);
		}
		if (qp == null) {
			String errMsg = this.getText("errors.required",
					new String[] { "qp" });
			this.addFieldError("查询参数", errMsg);
			return;
		}
		if (StringUtils.isEmpty(qp.getDateStart())) {
			String errMsg = this.getText("errors.required",
					new String[] { "qp.dateStart" });
			this.addFieldError("开始时间", errMsg);
		} else {
			if (ReportWebConstants.REPORT_MIN_DATE_STRING.compareTo(qp
					.getDateStart()) > 0) {
				// 如果开始时间比20081113还小的话则置成该时间
				qp.setDateStart(ReportWebConstants.REPORT_MIN_DATE_STRING);
			}
		}
		if (StringUtils.isEmpty(qp.getDateEnd())) {
			String errMsg = this.getText("errors.required",
					new String[] { "qp.dateEnd" });
			this.addFieldError("结束时间", errMsg);
		} else {
			if (DateUtils.formatCurrrentDate().compareTo(qp.getDateEnd()) < 0) {
				// 如果当前时间小于结束时间则将结束时间设置成今天
				qp.setDateEnd(DateUtils.formatCurrrentDate());
			}
		}
	}

	/**
	 * emptyPagerInfo:清空分页信息 某些情况下（下载）需要保证不需要分页，因此提供该方法用于强制去掉分页信息
	 * 
	 * @since
	 */
	protected void emptyPagerInfo() {
		if (qp != null) {
			qp.setPage(null);
			qp.setPageSize(null);
		}
	}

	/**
	 * @param force
	 *            - 是否强制设置
	 * @param forceScale
	 *            - 缺省设置的快查类型
	 */
	protected void initDate(boolean force, String forceScale) {
		String sdate = DateScaleHelper.startDate(forceScale);
		String edate = DateScaleHelper.endDate(forceScale);

		if (force) {
			qp.setDateScale(forceScale);
			qp.setDateStart(sdate);
			qp.setDateEnd(edate);
		} else if (StringUtils.isEmpty(qp.getDateScale())
				&& (StringUtils.isEmpty(qp.getDateStart()) || StringUtils
						.isEmpty(qp.getDateEnd()))) {
			qp.setDateScale(forceScale);
			qp.setDateStart(sdate);
			qp.setDateEnd(edate);
		}
	}

	protected <T> List<T> getListPage(List<T> list, int page, int pageSize) {

		if (!CollectionUtils.isEmpty(list)) {
			int count = list.size();
			if (pageSize < 1) {
				pageSize = ReportConstants.PAGE_SIZE;
			}
			int fromIdx = page * pageSize;
			int toIdx = fromIdx + pageSize;
			if (toIdx > count) {
				if (count > fromIdx) {// bug fix by yangyun 20101109
					toIdx = count;
				} else {
					return new ArrayList<T>();
				}

			}
			list = list.subList(fromIdx, toIdx);
		}
		return list;
	}

	/**
	 * 老report中的方法，暂时保留着，看后续是否能用得上
	 * 
	 * @deprecated 如果后续能用上的话就把这个deprecated去掉
	 */
	protected void renderDataByDay(AbsJsonObject json,
			List<Map<String, Object>> statData, int page) {
		List<ReportDayViewItem> listData;
		int count = statData.size();

		if (count > 0) {
			int fromIdx = page * ReportConstants.PAGE_SIZE;
			int toIdx = fromIdx + ReportConstants.PAGE_SIZE;
			if (toIdx > count) {
				toIdx = count;
			}
			statData = statData.subList(fromIdx, toIdx);
			listData = transformStatDataByDay(statData);
		} else {
			listData = new ArrayList<ReportDayViewItem>(0);
		}

		json.addData("listData", listData);
	}

	/**
	 * 老report中的方法，暂时保留着，看后续是否能用得上
	 */
	protected void renderStatus(AbsJsonObject json, String dateStart,
			String dateEnd, int count) {
		ReportStatusItem status = new ReportStatusItem();
		status.setDateStart(dateStart);
		status.setDateEnd(dateEnd);
		int pn = count / ReportConstants.PAGE_SIZE + 1;
		if (count % ReportConstants.PAGE_SIZE == 0) {
			pn -= 1;
		}
		status.setTotalPage(pn);

		json.addData("status", status);
	}

	/**
	 * <p>
	 * initParameter: 初始化参数，一般在列表页用
	 * 
	 * @throws Exception
	 * 
	 */
	protected void initParameter() {
		qp.setUserId(userId);
		// 单独处理前端传递过来planid groupid unitid为0的情况
		if (qp.getPlanId() == null || qp.getPlanId() == 0) {
			qp.setPlanId(null);
		}
		if (qp.getGroupId() == null || qp.getGroupId() == 0) {
			qp.setGroupId(null);
		}
		if (qp.getUnitId() == null || qp.getUnitId() == 0) {
			qp.setUnitId(null);
		}
		/*
		 * qp.setDateStart("20100101"); qp.setDateEnd("20100301");
		 * qp.setSrchs(80000l); qp.setSrchsOp(1);
		 */
		try {
			tuneDate();
		} catch (Exception e) {
			throw new ParameterInValidException(ReportWebConstants.ERR_DATE);
		}

		initStateMapping();
	}

	/**
	 * initStateMapping:初始化状态映射（由前端显示状态映射成后端实际存储的状态）
	 * 
	 * @since
	 */
	protected abstract void initStateMapping();

	/**
	 * initParameterForAtLestServenDay:初始化参数，一般在Flash查询时用，
	 * 因为flash查询要求至少查询7天的数据，因此即使传递过来的时间差是一天也要扩展成七天。
	 *
	 * @throws Exception
	 * @since
	 */
	protected void initParameterForAtLestServenDay() throws Exception {
		initParameter();

		qp.setPage(null);// 设置不分页以获取全部数据

		// 如果时间小于6则需要对起始时间进行向前扩充。
		int dateDistance = DateUtil.getDayDistance(to, from);
		if (dateDistance < ReportWebConstants.MINI_FLASH_QUERY_DATE_DISTANCE) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(to);
			cal.add(Calendar.DAY_OF_MONTH, -1
					* ReportWebConstants.MINI_FLASH_QUERY_DATE_DISTANCE);
			flashFrom = cal.getTime();
		} else {
			flashFrom = from;
		}
		flashTo = to;
	}

	/**
	 * 判断报表的起止时间是否包含今天和历史时间
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
//	protected boolean isMixTodayAndBefore(Date from, Date to) {
//		String today = DateUtil.format_yyyyMMdd(Calendar.getInstance()
//				.getTime());
//		String fromDay = DateUtil.format_yyyyMMdd(from);
//		String toDay = DateUtil.format_yyyyMMdd(to);
//
//		return (today.equals(fromDay) && !today.equals(toDay))
//				|| (!today.equals(fromDay) && today.equals(toDay));
//	}

	/**
	 * 判断报表的起止时间是否只包含今天
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	protected boolean isOnlyToday(Date from, Date to) {
		String today = DateUtil.format_yyyyMMdd(Calendar.getInstance()
				.getTime());
		String fromDay = DateUtil.format_yyyyMMdd(from);
		String toDay = DateUtil.format_yyyyMMdd(to);

		return (today.equals(fromDay) && today.equals(toDay));
	}

//	protected void clearNotRealtimeStat(StatInfo info) {
//		info.setSrchs(-1);
//		info.setCtr(-1);
//		info.setCpm(-1);
//		info.setSrchuv(-1);
//		info.setClkuv(-1);
//		info.setTransuv(-1);
//		info.setSrsur(-1);
//		info.setCusur(-1);
//		info.setCocur(-1);
//		info.setDirectTrans(-1);
//		info.setIndirectTrans(-1);
//		info.setArrivalCnt(-1);
//		info.setArrivalRate(-1);
//		info.setEffectArrCnt(-1);
//		info.setHopCnt(-1);
//		info.setHopRate(-1);
//		info.setResTime(-1);
//		info.setResTimeStr("-");
//		info.setAvgResTime(-1);
//		info.setHolmesClks(-1);
//	}

	/**
	 * 实时统计数据里，需要清除掉点击/消费之外的数据列，置为-1（前端显示"-"）
	 * 
	 * @param dataList
	 */
//	protected void clearNotRealtimeStat(List<? extends StatInfo> dataList,
//			StatInfo sumData) {
//		if (org.apache.commons.collections.CollectionUtils.isNotEmpty(dataList)) {
//			for (StatInfo info : dataList) {
//				clearNotRealtimeStat(info);
//			}
//		}
//
//		if (sumData != null) {
//			clearNotRealtimeStat(sumData);
//		}
//	}

	/**
	 * tuneDate: 初始化日期 将从前端传递过来的日期字符串解析成相应的Date对象
	 * 
	 * @throws Exception
	 *             格式化日期信息出错则抛出异常
	 * @since
	 */
	protected void tuneDate() throws Exception {

		if (StringUtils.isEmpty(qp.getDateScale())
				&& StringUtils.isEmpty(qp.getDateStart())
				&& StringUtils.isEmpty(qp.getDateEnd())) {
			// 星中添加：为了便于测试，此处默认把时间差设置成昨天。
			qp.setDateScale(BeidouCoreConstant.PAGE_DATE_RANGE_YESTERDAY);
		}
		if (!StringUtils.isEmpty(qp.getDateScale())) { // 快查
			from = DateScaleHelper.startDate2(qp.getDateScale());
			to = DateScaleHelper.endDate2(qp.getDateScale());

			qp.setDateStart(sd.format(from));
			qp.setDateEnd(sd.format(to));
		} else {
			// dateStart/dateEnd的判空与判序在validation中处理
			Date[] date = transformDate(qp.getDateStart(), qp.getDateEnd());
			from = date[0];
			to = date[1];
			this.validateDateRangeLessThanOneYear(from, to);
		}

	}

	private void validateDateRangeLessThanOneYear(Date from, Date to) {
		long betweenDays = DateUtils.getBetweenDate(from, to);

		if (betweenDays > ReportWebConstants.QUERY_MAX_DAYS) {
			throw new ParameterInValidException(ReportWebConstants.ERR_DATE);
		}
	}

	/**
	 * genQueryItemIds: 从AOT获取需要查询的ids列表
	 *
	 * @return 如果不需要查询aot即返回null
	 */
	protected List<Integer> genQueryItemIds() {

		List<Integer> filterIds = null;

		AotReportItem aotItems = getAotItems();// 需要查询AOT获取IDS的，需要按IDS来滤过的。
		if (aotItems != null) {
			filterIds = ReportUtil.transferFromLongToInteger(aotItems
					.getObjIdList());
			qp.setIds(aotItems.getObjIdList());
		}
		return filterIds;
	}

	/**
	 * getAotItems:从AOT获取信息
	 *
	 * @return
	 * @since
	 */
	protected AotReportItem getAotItems() {

		AotReportItem aotItems = null;
		if (qp.getAotItemId() != null && qp.getAotItemId() > 0) {

			String[] roles = userInfoMgr.getUserRoles(visitor.getUserid());
			int userType = ReportWebConstants.isUserCs(roles) ? UserWebConstant.USER_TYPE_MANAGER
					: UserWebConstant.USER_TYPE_CUSTOMER;
			aotItems = aotService.queryReportItem(qp.getUserId(),
					qp.getAotItemId(),
					qp.getPlanId() == null ? 0 : qp.getPlanId(),
					qp.getGroupId() == null ? 0 : qp.getGroupId(), userType);
		}
		return aotItems;
	}

	/**
	 * 转换字符串类型起止时间（不对日期范围做任何约束） 假设已经过判空与判序 added by yanjie at 20090310 modified
	 * by yanjie at 20090707
	 */
	protected Date[] transformDate(String dateStart, String dateEnd)
			throws Exception {
		Date ds, de;
		try {
			ds = DateUtils.strToDate(dateStart);
		} catch (Exception e) {
			throw e;
		}
		try {
			de = DateUtils.strToDate(dateEnd);
			de = DateUtils.getDateFloor(de).getTime();
		} catch (Exception e) {
			throw e;
		}

		return new Date[] { ds, de };
	}

	protected List<ReportDayViewItem> transformStatDataByDay(
			List<Map<String, Object>> statData) {
		List<ReportDayViewItem> result = new ArrayList<ReportDayViewItem>();
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
		for (Map<String, Object> stat : statData) {
			ReportDayViewItem item = new ReportDayViewItem();
			item.fillStatRecord(stat);
			Date date = (Date) stat.get(ReportConstants.FROMDATE);
			item.setShowDate(sd.format(date));

			result.add(item);
		}
		return result;
	}

	/**
	 * 老report中的方法，暂时保留着，看后续是否能用得上
	 * 
	 * @deprecated 如果后续能用上的话就把这个deprecated去掉
	 */
	protected List<Map<String, Object>> sortByColumn(
			List<Map<String, Object>> list, final String sortColumn,
			final String sortOrder) {
		Collections.sort(list, new Comparator<Map<String, Object>>() {
			public int compare(Map<String, Object> o1, Map<String, Object> o2) {
				// ctr/acp/cpm可能为null
				Object oo1 = o1.get(sortColumn);
				if (null == oo1) {
					oo1 = Double.valueOf(-1);
				}
				Object oo2 = o2.get(sortColumn);
				if (null == oo2) {
					oo2 = Double.valueOf(-1);
				}
				Double v1 = Double.valueOf(oo1.toString());
				Double v2 = Double.valueOf(oo2.toString());
				int sign = Double.compare(v1, v2);
				if (sortOrder.equalsIgnoreCase("desc")) {
					sign = -sign;
				}
				return sign;
			}
		});
		return list;
	}

	/** 获取分页数据 */
	public <T> List<T> pagerList(List<T> infoData) {
		if (infoData.size() > ReportWebConstants.FRONT_SORT_THRESHOLD) {
			int page = 0;
			int pageSize = ReportConstants.PAGE_SIZE;
			if (qp.getPage() != null && qp.getPage() > 1) {
				page = qp.getPage();
			}
			if (qp.getPageSize() != null && qp.getPageSize() > 0) {
				pageSize = qp.getPageSize();
			}

			infoData = ReportWebConstants.subListinPage(infoData, page,
					pageSize);
		}
		return infoData;
	}

	/**
	 * getTotalPage: 获取总页码
	 *
	 * @param size
	 *            总记录条数
	 * @return 总页数
	 */
	public int getTotalPage(int size) {

		int pageSize = ReportConstants.PAGE_SIZE;
		if (null != qp.getPageSize() && qp.getPageSize() > 0) {
			pageSize = qp.getPageSize();
		}
		int totalPage = size / pageSize;
		if (size > pageSize * totalPage) {
			totalPage += 1;
		}
		return totalPage;
	}

	/**
	 * generateStatMap: 生成统计信息MAP，key为时间，value为统计信息
	 *
	 * @param stats
	 *            天粒度的原始统计信息
	 * @return
	 */
	protected Map<String, Map<String, Object>> generateStatMap(
			List<Map<String, Object>> stats) {

		Map<String, Map<String, Object>> statData = new HashMap<String, Map<String, Object>>();
		if (stats != null) {
			for (Map<String, Object> stat : stats) {
				String from = sd1.format((Date) stat
						.get(ReportConstants.FROMDATE));
				statData.put(from, stat);
			}

		}

		return statData;
	}

	/**
	 * filterByStatFieldForMap:对列表按统计字段进行过滤。
	 *
	 * @param qp
	 *            查询参数
	 * @param vos
	 *            待过滤列表，该列表为planId + day维度
	 * @return 过滤后的vo(plan | group | unit)+day维度的统计信息
	 */
	protected List<Map<String, Object>> filterByStatFieldForMap(
			QueryParameter qp, List<Map<String, Object>> vos) {

		if (org.apache.commons.collections.CollectionUtils.isEmpty(vos)
				|| !qp.hasStatField4Filter()) {
			return vos;
		} else {

			for (int i = vos.size() - 1; i >= 0; i--) {
				if (ReportWebConstants.filter(qp, vos.get(i))) {
					vos.remove(vos.get(i));
				}
			}
			return vos;
			// List<Map<String, Object>> result = new ArrayList<Map<String,
			// Object>>();
			// for ( int i = 0; i < vos.size(); i++) {
			// Map<String, Object> item = vos.get(i);
			// if (!ReportWebConstants.filter(qp, item)) {
			// result.add(item);
			// }
			// }
			// return result;
		}
	}

	protected <T extends StatInfo> List<T> filterByStatFieldForList(
			QueryParameter qp, List<T> vos) {

		if (org.apache.commons.collections.CollectionUtils.isEmpty(vos)
				|| !qp.hasStatField4Filter()) {
			return vos;
		} else {
			for (int i = vos.size() - 1; i >= 0; i--) {
				if (ReportWebConstants.filter(qp, vos.get(i))) {
					vos.remove(vos.get(i));
				}
			}
			return vos;
		}
	}

	/**
	 * filterByStatField: 按统计字段进行过滤 列表查询时如果有按统计字段过滤的话则使用该方法
	 *
	 * @param qp
	 * @param vos
	 * @return
	 * @since
	 */
	protected <T extends StatInfo> List<T> filterByStatField(QueryParameter qp,
			List<T> vos) {

		if (org.apache.commons.collections.CollectionUtils.isEmpty(vos)
				|| !qp.hasStatField4Filter()) {
			return vos;
		} else {

			for (int i = vos.size() - 1; i >= 0; i--) {
				if (ReportWebConstants.filter(qp, vos.get(i))) {
					vos.remove(vos.get(i));
				}
			}
			return vos;
		}
	}

	/**
	 * fillMergeWithMasterOrder:以返回VO列表为主，填充统计数据，如果没有统计数据则填空。
	 *
	 * @param infoData
	 *            排序好的主VO对象
	 * @param statData
	 *            待合并的统计数据
	 */
	protected <T extends AbstractViewItem> void fillMergeWithMasterOrder(
			List<T> infoData, Map<Object, Map<String, Object>> statData) {

		for (T info : infoData) {
			// 获取ID对应的统计数据
			Map<String, Object> stat = statData.get(info.getId());
			if (stat != null) {
				info.fillStatRecord(stat);
			}
		}
	}

	protected <T extends AbstractViewItem> void fillMergeDataWithMasterOrder(
			List<T> infoData, Map<Object, T> statData) {

		for (T info : infoData) {
			// 获取ID对应的统计数据
			T stat = statData.get(info.getId());
			if (stat != null) {
				info.fillStatRecord(stat);
				info.generateExtentionFields();
			}
		}
	}

	/**
	 * fillMergeWithMasterOrder:以返回VO列表为主，填充统计数据，如果没有统计数据则填空。
	 *
	 * @param infoData
	 *            排序好的主VO对象
	 * @param statData
	 *            待合并的统计数据
	 * @param idKey
	 *            存在statData(统计数据)中的id的Key值
	 */
	protected <T extends AbstractViewItem> void fillMergeWithMasterOrder(
			List<T> infoData, List<Map<String, Object>> statData, String idKey) {

		// 以hashmap方式进行匹配可以减少一次循环
		Map<Object, Map<String, Object>> statMap = new HashMap<Object, Map<String, Object>>();
		for (Map<String, Object> stat : statData) {
			statMap.put(stat.get(idKey), stat);
		}
		fillMergeWithMasterOrder(infoData, statMap);
	}

	protected <T extends AbstractViewItem> void fillMergeDataWithMasterOrder(
			List<T> infoData, List<T> statData) {

		// 以hashmap方式进行匹配可以减少一次循环
		Map<Object, T> statMap = new HashMap<Object, T>();
		for (T stat : statData) {
			statMap.put(stat.getId(), stat);
		}
		fillMergeDataWithMasterOrder(infoData, statMap);
	}

	/**
	 * fillMergeWithSlaveOrder: 合并统计字段信息，并以统计字段排序的结果返回
	 *
	 * @param infoData
	 *            待返回的VO列表
	 * @param statData
	 *            按统计字段排序好的统计信息，可能比infodata少。
	 * @param idKey
	 *            存在statData(统计数据)中的id的Key值
	 * @return 以统计数据为排序基准的结果集
	 */
	protected <T extends AbstractViewItem> List<T> fillMergeWithSlaveOrder(
			List<T> infoData, List<Map<String, Object>> statData, String idKey) {

		Map<Object, T> itemMap = new HashMap<Object, T>();
		for (T item : infoData) {
			itemMap.put(item.getId(), item);
		}
		return fillMergeWithSlaveOrder(itemMap, statData, idKey);
	}

	protected <T extends AbstractViewItem> List<T> fillMergeDataWithSlaveOrder(
			List<T> infoData, List<T> statData) {

		Map<Object, T> itemMap = new HashMap<Object, T>();
		for (T item : infoData) {
			itemMap.put(item.getId(), item);
		}
		return fillMergeDataWithSlaveOrder(itemMap, statData);
	}

	/**
	 * fillMergeWithSlaveOrder:合并统计字段信息，并以统计字段排序的结果返回
	 * 
	 * @param infoData
	 *            待返回的VO列表
	 * @param statData
	 *            按统计字段排序好的统计信息，可能比infodata少。
	 * @param idKey
	 *            存在statData(统计数据)中的id的Key值
	 * @return 以统计数据为排序基准的结果集
	 */
	protected <T extends AbstractViewItem> List<T> fillMergeWithSlaveOrder(
			Map<Object, T> infoData, List<Map<String, Object>> statData,
			String idKey) {
		List<T> result = new ArrayList<T>();// 降序排时使用
		List<T> result1 = new ArrayList<T>();// 升序排时使用
		if (!org.apache.commons.collections.CollectionUtils.isEmpty(statData)) {
			for (Map<String, Object> stat : statData) {
				Object id = stat.get(idKey);
				T info = infoData.get(id);
				if (info != null) {
					info.fillStatRecord(stat);
					result.add(info);
					infoData.remove(id);
				}
			}
		}
		if (BeidouConstant.SORTORDER_DESC.equalsIgnoreCase(qp.getOrder())) {
			// 倒序就把没有统计数据的VO放在最后
			result.addAll(infoData.values());
			return result;
		} else {
			result1.addAll(infoData.values());
			result1.addAll(result);
			return result1;
		}
	}

	protected <T extends AbstractViewItem> List<T> fillMergeDataWithSlaveOrder(
			Map<Object, T> infoData, List<T> statData) {
		List<T> result = new ArrayList<T>();// 降序排时使用
		List<T> result1 = new ArrayList<T>();// 升序排时使用
		if (!org.apache.commons.collections.CollectionUtils.isEmpty(statData)) {
			for (T stat : statData) {
				Object id = stat.getId();
				T info = infoData.get(id);
				if (info != null) {
					info.fillStatRecord(stat);
					info.generateExtentionFields();
					result.add(info);
					infoData.remove(id);
				}
			}
		}
		if (BeidouConstant.SORTORDER_DESC.equalsIgnoreCase(qp.getOrder())) {
			// 倒序就把没有统计数据的VO放在最后
			result.addAll(infoData.values());
			return result;
		} else {
			result1.addAll(infoData.values());
			result1.addAll(result);
			return result1;
		}
	}

	protected boolean shouldCache(int resultSize) {
		return resultSize <= ReportWebConstants.FRONT_SORT_THRESHOLD;
	}

	/**
	 * 通过判断是否按照X字段排序返回排序类型
	 * 
	 * @param isOrderByStatDataField
	 * @param isOrderByUvDataField
	 * @param isOrderByHolmesDataField
	 * @param isOrderByTransDataField
	 * @return
	 */
	protected Integer getOrderByXFiled(boolean isOrderByStatDataField,
			boolean isOrderByUvDataField, boolean isOrderByHolmesDataField,
			boolean isOrderByTransDataField) {
		if (isOrderByUvDataField) {
			return DorisDataType.UV;
		} else if (isOrderByHolmesDataField) {
			return DorisDataType.HOLMES;
		} else if (isOrderByTransDataField) {
			return DorisDataType.TRANS;
		} else {
			return DorisDataType.STAT;
		}
	}

	/**
	 * generateXml: 生成xml文档 由于统计数据可能并不是查询时间差所有的都有，因此需要做以下几点： 1、汇总总的统计数据；
	 * 2、如果如果查询时间差不足七天则算从第几天开始显示实心值； 3、如果查询时间内某些天没有统计数据需要在xml文档中补0
	 * 
	 * @param statData
	 *            统计数据
	 * @return 用于前端flash显示的xml文档
	 */
	protected <T extends StatInfo> String generateXml(List<T> itemList) {

		// 1、生成汇总数据：由于之前是按天的数据，因此需要生成总的汇总数据。
		StatInfo info = new StatInfo();
		for (T item : itemList) {
			info.mergeBasicField(item);
		}
		info.generateExtentionFields();

		// 2、算从第几个点开始显示，下标从0开始
		int dateDistance = ReportWebConstants.MINI_FLASH_QUERY_DATE_DISTANCE;
		try {
			dateDistance = DateUtil.getDayDistance(sd.parse(qp.getDateEnd()),
					sd.parse(qp.getDateStart()));
		} catch (ParseException e) {
			jsonObject.addData("status", 1);
			jsonObject.addData("xml", "");
			return SUCCESS;
		}
		int begin = 0;
		if (dateDistance < ReportWebConstants.MINI_FLASH_QUERY_DATE_DISTANCE) {
			begin = ReportWebConstants.MINI_FLASH_QUERY_DATE_DISTANCE
					- dateDistance;
		}

		// 3、拼接xml文档
		StringBuilder sb = new StringBuilder();
		// 3.1、头
		sb.append("<?xml version='1.0' encoding='utf-8'?>");
		// 3.2、汇总部分
		sb.append("<data overview='");
		if (qp.getDateText() == null) {
			sb.append("");
		} else {
			sb.append(qp.getDateText());
		}
		sb.append("' showIndex='");
		sb.append(begin);
		sb.append("' cost='");
		sb.append("￥/总费用/");
		sb.append("' clks='");
		sb.append("/点击次数/");
		sb.append("' clkuv='");
		sb.append("/点击独立访客/");
		sb.append("' srchs='");
		sb.append("/展现次数/");
		sb.append("' srchuv='");
		sb.append("/展现独立访客/");
		sb.append("' ctr='");
		sb.append("/点击率/%");
		sb.append("' uvctr='");
		sb.append("/独立访客点击率/%");
		sb.append("' selected='4'>");

		Calendar cal = Calendar.getInstance();
		cal.setTime(flashFrom);

		Map<String, T> itemMap = new HashMap<String, T>();
		if (!CollectionUtils.isEmpty(itemList)) {
			for (T item : itemList) {
				itemMap.put(item.getShowDate(), item);
			}
		}

		// 3.3、实际显示部分
		while (!cal.getTime().after(flashTo)) {
			String dateString = sd1.format(cal.getTime());
			StatInfo tmp = itemMap.get(dateString);
			if (tmp == null) {
				tmp = new StatInfo();
			}
			sb.append("<record date='");
			sb.append(dateString);
			sb.append("' cost='");
			sb.append(tmp.getCost());
			sb.append("' clks='");
			sb.append(tmp.getClks());
			sb.append("' clkuv='");
			sb.append(tmp.getClkuv());
			sb.append("' srchs='");
			sb.append(tmp.getSrchs());
			sb.append("' srchuv='");
			sb.append(tmp.getSrchuv());
			sb.append("'/>");

			cal.add(Calendar.DAY_OF_MONTH, 1);
		}
		// 3.4、结尾
		sb.append("</data>");
		return sb.toString();
	}

	/**
	 * generateXml: 生成xml文档 由于统计数据可能并不是查询时间差所有的都有，因此需要做以下几点： 1、汇总总的统计数据；
	 * 2、如果如果查询时间差不足七天则算从第几天开始显示实心值； 3、如果查询时间内某些天没有统计数据需要在xml文档中补0
	 * 
	 * @param statData
	 *            统计数据
	 * @return 用于前端flash显示的xml文档
	 */
	protected String generateXml(Map<String, Map<String, Object>> statData) {

		// 1、生成汇总数据：由于之前是按天的数据，因此需要生成总的汇总数据。
		StatInfo info = new StatInfo();
		for (Map<String, Object> stat : statData.values()) {
			info.mergeBasicField(stat);
		}
		info.generateExtentionFields();

		// 2、算从第几个点开始显示，下标从0开始
		int dateDistance = ReportWebConstants.MINI_FLASH_QUERY_DATE_DISTANCE;
		try {
			dateDistance = DateUtil.getDayDistance(sd.parse(qp.getDateEnd()),
					sd.parse(qp.getDateStart()));
		} catch (ParseException e) {
			jsonObject.addData("status", 1);
			jsonObject.addData("xml", "");
			return SUCCESS;
		}
		int begin = 0;
		if (dateDistance < ReportWebConstants.MINI_FLASH_QUERY_DATE_DISTANCE) {
			begin = ReportWebConstants.MINI_FLASH_QUERY_DATE_DISTANCE
					- dateDistance;
		}

		// 3、拼接xml文档
		StringBuilder sb = new StringBuilder();
		// 3.1、头
		sb.append("<?xml version='1.0' encoding='utf-8'?>");
		// 3.2、汇总部分
		sb.append("<data overview='");
		if (qp.getDateText() == null) {
			sb.append("");
		} else {
			sb.append(qp.getDateText());
		}
		sb.append("' showIndex='");
		sb.append(begin);
		sb.append("' cost='");
		sb.append("￥/总费用/");
		sb.append("' clks='");
		sb.append("/点击次数/");
		sb.append("' clkuv='");
		sb.append("/点击独立访客/");
		sb.append("' srchs='");
		sb.append("/展现次数/");
		sb.append("' srchuv='");
		sb.append("/展现独立访客/");
		sb.append("' ctr='");
		sb.append("/点击率/%");
		sb.append("' uvctr='");
		sb.append("/独立访客点击率/%");
		sb.append("' selected='4'>");

		Calendar cal = Calendar.getInstance();
		cal.setTime(flashFrom);

		// 3.3、实际显示部分
		while (!cal.getTime().after(flashTo)) {
			String dateString = sd1.format(cal.getTime());
			StatInfo tmp = new StatInfo();
			tmp.fillStatRecordOnly(statData.get(dateString));
			sb.append("<record date='");
			sb.append(dateString);
			sb.append("' cost='");
			sb.append(tmp.getCost());
			sb.append("' clks='");
			sb.append(tmp.getClks());
			sb.append("' clkuv='");
			sb.append(tmp.getClkuv());
			sb.append("' srchs='");
			sb.append(tmp.getSrchs());
			sb.append("' srchuv='");
			sb.append(tmp.getSrchuv());
			sb.append("'/>");

			cal.add(Calendar.DAY_OF_MONTH, 1);
		}
		// 3.4、结尾
		sb.append("</data>");
		return sb.toString();
	}

	/**
	 * enrollToDay:将多个不同维度的数据上卷到day维度。
	 *
	 * @param stats
	 * @return 天维度的统计数据
	 */
	protected Map<String, Map<String, Object>> enrollToDay(
			List<Map<String, Object>> stats) {
		Map<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>();
		for (Map<String, Object> stat : stats) {
			String from = sd1.format((Date) stat.get(ReportConstants.FROMDATE));
			Map<String, Object> tmp = result.get(from);
			if (tmp == null) {
				tmp = new HashMap<String, Object>();
				result.put(from, tmp);
			}
			StatInfo info = new StatInfo();
			info.fillStatRecordOnly(stat);
			info.merge(tmp);
			tmp.put(ReportConstants.SRCHS, info.getSrchs());
			tmp.put(ReportConstants.CLKS, info.getClks());
			tmp.put(ReportConstants.COST, info.getCost() * 100);// 需要*100表示到分
			tmp.put(ReportConstants.ACP, info.getAcp());
			if (info.getCtr() != null && info.getCtr().doubleValue() > 0) {
				tmp.put(ReportConstants.CTR, info.getCtr());
			} else {
				tmp.put(ReportConstants.CTR, info.getCtr());
			}
			tmp.put(ReportConstants.CPM, info.getCpm());
		}
		return result;
	}

	/********************* [g/s]etters ******************************/

	public AbsJsonObject getJsonObject() {
		return jsonObject;
	}

	public void setJsonObject(AbsJsonObject jsonObject) {
		this.jsonObject = jsonObject;
	}

	public Date getFrom() {
		return from;
	}

	public void setFrom(Date from) {
		this.from = from;
	}

	public Date getTo() {
		return to;
	}

	public void setTo(Date to) {
		this.to = to;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getSHOW_SUB_SITE_CNT() {
		return SHOW_SUB_SITE_CNT;
	}

	public void setSHOW_SUB_SITE_CNT(int sHOWSUBSITECNT) {
		SHOW_SUB_SITE_CNT = sHOWSUBSITECNT;
	}

	public Visitor getVisitor() {
		return visitor;
	}

	public void setVisitor(Visitor visitor) {
		this.visitor = visitor;
	}

	public QueryParameter getQp() {
		return qp;
	}

	public void setQp(QueryParameter qp) {
		this.qp = qp;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public BeidouAotService getAotService() {
		return aotService;
	}

	public void setAotService(BeidouAotService aotService) {
		this.aotService = aotService;
	}

	public TransReportFacade getTransReportFacade() {
		return transReportFacade;
	}

	public void setTransReportFacade(TransReportFacade transReportFacade) {
		this.transReportFacade = transReportFacade;
	}

	public UserMgr getUserMgr() {
		return userMgr;
	}

	public void setUserMgr(UserMgr userMgr) {
		this.userMgr = userMgr;
	}

	public UserInfoMgr getUserInfoMgr() {
		return userInfoMgr;
	}

	public void setUserInfoMgr(UserInfoMgr userInfoMgr) {
		this.userInfoMgr = userInfoMgr;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String get__callbackName__() {
		return __callbackName__;
	}

	public void set__callbackName__(String name__) {
		__callbackName__ = name__;
	}

	public String getFileEncoding() {
		return fileEncoding;
	}

	public void setFileEncoding(String fileEncoding) {
		this.fileEncoding = fileEncoding;
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

	protected int getQpPage() {
		int page = 0;
		if (qp.getPage() != null && qp.getPage() > 0) {
			page = qp.getPage();
		}
		return page;
	}

	protected int getQpPageSize() {
		int pageSize = ReportConstants.PAGE_SIZE;
		if (qp.getPageSize() != null && qp.getPageSize() > 0) {
			pageSize = qp.getPageSize();
		}

		return pageSize;
	}

	// FIXME add by xuc
	protected int order(QueryParameter qp) {
		if (QueryParameterConstant.SortOrder.SORTORDER_DESC.equalsIgnoreCase(qp
				.getOrder())) {
			return ReportConstants.SortOrder.DES;
		}

		return ReportConstants.SortOrder.ASC;
	}

	protected Report toReport(QueryParameter queryParameter, Date from, Date to) {
		Report report = new VirtualReport();
		report.setStartTime(from);
		report.setEndTime(to);
		report.setUserId(queryParameter.getUserId());
		report.setPlanIds(queryParameter.getPlanIdsStr());
		report.setGroupIds(queryParameter.getGroupIdsStr());
		report.setTimeUnit(queryParameter.getTimeUnit());
		if (queryParameter.getIsBySite() != null) {
			report.setBySite(queryParameter.getIsBySite());
		}

		if (ReportConstants.isStatField(queryParameter.getOrderBy())) {
			// FIXME
			report.setSortList(queryParameter.getOrderBy() + "/"
					+ order(queryParameter));
		}

		return report;
	}

	public static void main(String[] args) {
		QueryParameter qp = new QueryParameter();
		@SuppressWarnings("serial")
		BeidouReportActionSupport bras = new BeidouReportActionSupport() {

			@Override
			protected void initStateMapping() {

			}
		};
		bras.setQp(qp);
		qp.setPageSize(20);
		System.out.println(bras.getTotalPage(0));
		System.out.println(bras.getTotalPage(10));
		System.out.println(bras.getTotalPage(20));
		System.out.println(bras.getTotalPage(21));
		System.out.println("20081113".compareTo("20081111"));
		System.out.println("20081113".compareTo("20081113"));
		System.out.println("20081113".compareTo("20081114"));
		System.out
				.println(DateUtils.formatCurrrentDate().compareTo("20110412"));
		System.out
				.println(DateUtils.formatCurrrentDate().compareTo("20110413"));
		System.out
				.println(DateUtils.formatCurrrentDate().compareTo("20110414"));

	}

	public TransDataService getTransDataService() {
		return transDataService;
	}

	public void setTransDataService(TransDataService transDataService) {
		this.transDataService = transDataService;
	}
}
