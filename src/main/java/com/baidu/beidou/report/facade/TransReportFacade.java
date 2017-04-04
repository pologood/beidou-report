package com.baidu.beidou.report.facade;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.baidu.beidou.olap.vo.ReportDayViewItem;
import com.baidu.beidou.olap.vo.AbstractViewItem;
import com.baidu.beidou.olap.vo.SiteViewItem;
import com.baidu.beidou.stat.service.TransDataService;

public interface TransReportFacade extends TransDataService {

	public List<Map<String, Object>> mergeTransAndStatData(
			boolean isOrderByStatDataField,
			List<Map<String, Object>> transData,
			List<Map<String, Object>> statData, String idKey);

	public List<Map<String, Object>> mergeTransAndStatDataByDay(
			List<Map<String, Object>> transData,
			List<Map<String, Object>> statData);

	public boolean needToFetchTransData(Integer userId, Date from,
			Date to, boolean forceGet);

	public boolean isTransToolSigned(Integer userId, boolean forceGet);

	/**
	 * 对于返回给前端的数据，当需要显示转化数据列但转化数据没有准备好时，将转化数据列全置为-1，这样前端可以显示-
	 * @param <T>
	 * @param userId
	 * @param from
	 * @param to
	 * @param infoData
	 */
	public <T extends AbstractViewItem> void postHandleTransData(
			Integer userId, Date from, Date to, List<T> infoData);

	public void postHandleTransDataByDay(Integer userId, Date from,
			Date to, List<ReportDayViewItem> infoData);

	public void postHandleGroupSiteTransData(Integer userId,
			Date from, Date to, List<SiteViewItem> infoData);

	public String getTransName(Integer userId, Long siteId, Long transId);

}