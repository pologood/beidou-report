package com.baidu.beidou.report.web.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;

import com.baidu.beidou.cprogroup.util.KTKeywordUtil;
import com.baidu.beidou.cproplan.bo.CproPlan;
import com.baidu.beidou.cproplan.constant.CproPlanConstant;
import com.baidu.beidou.cproplan.service.CproPlanMgr;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.olap.constant.Constants;
import com.baidu.beidou.olap.service.DevicePlanStatService;
import com.baidu.unbiz.olap.util.ReportUtils;
import com.baidu.beidou.olap.vo.DeviceViewItem;
import com.baidu.beidou.olap.vo.PlanViewItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.report.service.ReportCproPlanMgr;
import com.baidu.beidou.report.util.ReportUtil;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.device.DeviceViewItemSum;
import com.baidu.beidou.report.vo.plan.DeviceReportSumData;
import com.baidu.beidou.report.vo.plan.DeviceReportVo;
import com.baidu.beidou.stat.driver.bo.Constant;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.LogUtils;
import com.baidu.beidou.util.StringUtils;


public class ListPlanDeviceAction extends BeidouReportActionSupport{

	private static final long serialVersionUID = 1L;
	
	private CproPlanMgr cproPlanMgr = null;
	private List<Integer> planIds = new ArrayList<Integer>();
	private List<Integer> groupIds = new ArrayList<Integer>();
	private ReportCproPlanMgr reportCproPlanMgr;	
	
	@Resource(name="devicePlanStatServiceImpl")
	private DevicePlanStatService deviceService;
	
	/**
	 * 排序方向（正排为1，倒排为-1）
	 */
	private int orient;
	
	@Override
	protected void initStateMapping() {
		qp.setUserId(userId);
		//单独处理前端传递过来planid groupid unitid为0的情况
		if (qp.getPlanId() == null || qp.getPlanId() == 0) {
			qp.setPlanId(null);
		}
		if (qp.getGroupId() == null || qp.getGroupId() == 0) {
			qp.setGroupId(null);
		}
		if (qp.getUnitId() == null || qp.getUnitId() == 0) {
			qp.setUnitId(null);
		}
		if(qp.getPlanId() != null) {
			planIds = new ArrayList<Integer>();
			planIds.add(qp.getPlanId());
		}
		if(qp.getGroupId() != null) {
			groupIds = new ArrayList<Integer>();
			groupIds.add(qp.getGroupId());
		}
		if(qp.getPage() == null || qp.getPage() < 0) {
			qp.setPage(0);
		}

		if(qp.getPageSize() == null || qp.getPageSize() < 1) {
			qp.setPageSize(ReportConstants.PAGE_SIZE );
		}
		
	}	
	
	protected void initParameter() {
		
		super.initParameter();
		
		orient = ReportConstants.SortOrder.ASC;
		if (QueryParameterConstant.SortOrder.SORTORDER_DESC
				.equalsIgnoreCase(qp.getOrder())) {
			orient = ReportConstants.SortOrder.DES;		
		}
	}
	
	/**
	 * 生成用于设备报告前端显示的VO
	 * @return 
	 */
	public String ajaxDeviceList(){		

		// 1、参数初始化
		try {
			this.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return SUCCESS;
		}
		
		// 2、生成显示的GroupDtViewItem列表
		List<DeviceViewItem> list = generateDeviceList();
		
		// 3、查询总条数，该数字可能不是最终值，因为有可能需要按统计值过滤，这样的话该值会被再次修改
		int count = list.size();
		
		// 4、排序
		Collections.sort(list, new CproDeviceComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		DeviceViewItemSum sumData = calculateSumData(list);

		// 6、计算总页码
		int totalPage = super.getTotalPage(count);
		
		// 7、获取分页
		list = pagerList(list);

		/**
		 * 如果统计时间是今天，将统计数据列中处点击、消费外的其他列置为-1
		 */
		
		jsonObject.addData("cache",1);
		jsonObject.addData("list", list);
		jsonObject.addData("totalPage", totalPage);
		
		jsonObject.addData("sum", sumData);
		
		//8、处理当日和昨日不入库的uv和转化数据
		this.reportFacade.postHandleTransAndUvData(userId, from, to, list, sumData);

		return SUCCESS;		
	}
	
	/**
	 * 生成用于兴趣报告前端显示的VO
	 * @return 
	 */
	public String downloadDeviceList() throws IOException{		

		//1、初始化一下参数
		initParameter();
		
		//2、查询账户信息
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}
		
		//3、构造报告VO，下载的CSV共有四部分：1、账户基本信息，2、列头，3、列表，4、汇总信息	
		DeviceReportVo vo = new DeviceReportVo(qp.getLevel());//报表下载使用的VO
		
		//需要根据userid是否为白名单控制计划状态列是否显示
		vo.setUserId(qp.getUserId());
		
		List<DeviceViewItem> infoData = null;//目标plan集
		
		//3.1、获取统计数据
		infoData = generateDeviceList();//无统计时间粒度
		
		//3.2、排序
		Collections.sort(infoData, new CproDeviceComparator(qp.getOrderBy(), orient));

		//3.3、生成汇总的统计数据
		DeviceViewItemSum sumData = calculateSumData(infoData);
		
		//3.4处理当日和昨日不入库的uv和转化数据 add by wangchongjie since 2012.11.03
		this.reportFacade.postHandleTransAndUvData(userId, from, to, infoData, sumData);
		
		//3.5、填充数据
		vo.setAccountInfo(generateAccountInfo(user.getUsername()));
		vo.setDetails(infoData);
		vo.setHeaders(generateReportHeader());
		vo.setSummary(generateReportSummary(sumData));
		
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(vo, output);
		
		//4、设置下载需要使用到的一些属性
		byte[] bytes = output.toByteArray();
		inputStream = new ByteArrayInputStream(bytes);
		fileSize = bytes.length;

		if ( qp.getPlanId() != null ) {
			CproPlan plan = cproPlanMgr.findCproPlanById(qp.getPlanId());
			if (plan != null) {
				fileName = plan.getPlanName() + "-";
			}
		} else {
			fileName = user.getUsername() + "-";
		}
		fileName += this.getText("download.device.filename.prefix");
		try {
			fileName = StringUtils.subGBKString(fileName, 0,
					ReportWebConstants.FILE_MAIN_NAME_MAXLENGTH);
		} catch (UnsupportedEncodingException e1) {
			LogUtils.error(log, e1);
		}

		fileName += "-" + sd.format(from) + "-" + sd.format(to) + ".csv";

		try {
			// 中文文件名需要用ISO8859-1编码
			fileName = new String(fileName.getBytes(fileEncoding), "ISO8859-1");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage());
		}
		return SUCCESS;
	}
	
	/**************************业务方法*********************************/
	/**
	 * 移动应用比较器
	 */
	class CproDeviceComparator implements Comparator<DeviceViewItem>  {

		int order;
		String col;
		public CproDeviceComparator(String col, int order) {
			this.col = col;
			this.order = order;
		}
		
		private int compareByString(String v1, String v2) {
			Collator collator=Collator.getInstance(java.util.Locale.CHINA);
			if(StringUtils.isEmpty(v1)) {
				return -1 * order;
			}
			if(StringUtils.isEmpty(v2)) {
				return order;
			}		
			return order * collator.compare(v1,v2);
		}
		
		public int compare(DeviceViewItem o1, DeviceViewItem o2) {
			if(o1 == null) {
				return -1 * order;
			}
			if(o2 == null) {
				return 1 * order;
			}
			if(col.equals(ReportWebConstants.FRONT_BACKEND_ORDERNAME_DEVICE)) {
				return order * (int)(o1.getDeviceId() - o2.getDeviceId());
			}
			if(col.equals(ReportWebConstants.FRONT_BACKEND_ORDERNAME_BIDRATIO)) {
				return order * (int)(o1.getBidRatio() - o2.getBidRatio());
			}
			if(ReportWebConstants.FRONT_BACKEND_ORDERNAME_PLANNAME.equalsIgnoreCase(col)) {
				return compareByString(o1.getPlanName(), o2.getPlanName());
			}
	
			if(ReportConstants.SRCHS.equals(col)) {
				if((o1.getSrchs() - o2.getSrchs())==0){
					return o1.getDeviceId().intValue() - o2.getDeviceId().intValue();
				}
				else{
					return order * (int)(o1.getSrchs() - o2.getSrchs());
				}
			}
			
			if(ReportConstants.CLKS.equals(col)) {
				return order * (int)(o1.getClks() - o2.getClks());
			}
			
			if(ReportConstants.COST.equals(col)) {
				if(o1.getCost() > o2.getCost()){
					return order * 1;
				}else if(o1.getCost() < o2.getCost()){
					return order * -1;
				}else{
					return 0;
				}
				
			}
			
			if(ReportConstants.CTR.equals(col)) {
				BigDecimal b1 = o1.getCtr();
				BigDecimal b2 = o2.getCtr();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}

			if(ReportConstants.ACP.equals(col)) {
				BigDecimal b1 = o1.getAcp();
				BigDecimal b2 = o2.getAcp();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}

			if(ReportConstants.CPM.equals(col)) {
				BigDecimal b1 = o1.getCpm();
				BigDecimal b2 = o2.getCpm();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if(ReportConstants.SRCHUV.equals(col)) {
				return order * (int)(o1.getSrchuv() - o2.getSrchuv());
			}
			
			if(ReportConstants.CLKUV.equals(col)) {
				return order * (int)(o1.getClkuv() - o2.getClkuv());
			}
			
			if(ReportConstants.SRSUR.equals(col)) {
				BigDecimal b1 = o1.getSrsur();
				BigDecimal b2 = o2.getSrsur();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if(ReportConstants.CUSUR.equals(col)) {
				BigDecimal b1 = o1.getCusur();
				BigDecimal b2 = o2.getCusur();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if(ReportConstants.COCUR.equals(col)) {
				BigDecimal b1 = o1.getCocur();
				BigDecimal b2 = o2.getCocur();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if(ReportConstants.RES_TIME_STR.equals(col)) {
				return order * (int)(o1.getResTimeStr().compareTo(o2.getResTimeStr()));
			}
			
			if(ReportConstants.ARRIVAL_RATE.equals(col)) {
				BigDecimal b1 = o1.getArrivalRate();
				BigDecimal b2 = o2.getArrivalRate();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if(ReportConstants.HOP_RATE.equals(col)) {
				BigDecimal b1 = o1.getHopRate();
				BigDecimal b2 = o2.getHopRate();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if(ReportConstants.DIRECT_TRANS_CNT.equals(col)) {
				return order * (int)(o1.getDirectTrans() - o2.getDirectTrans());
			}
			
			if(ReportConstants.INDIRECT_TRANS_CNT.equals(col)) {
				return order * (int)(o1.getIndirectTrans() - o2.getIndirectTrans());
			}
			
			return order;
		}			
	}
	
	
	/**
	 * <p>重写获取待查询IDS的方法，对于传入的qp.planId和qp.ids也加入到考虑之列
	 * <p>获取ids的的条件为为以下顺序三选一：1、qp.planId； 2、qp.ids； 3、super.genQueryItemIds(即从AOT获取)
	 */
	public List<Integer> genQueryItemIds() {

		List<Integer> filterIds = null;//需要按IDS来滤过的。
		if (org.apache.commons.collections.CollectionUtils.isEmpty(qp.getIds())
				&& (qp.getPlanId() == null || qp.getPlanId() <= 0 )) {
			filterIds = super.genQueryItemIds();
			qp.setIds(ReportUtil.transferFromIntegerToLong(filterIds));
		} else {
			if (qp.getPlanId() != null && qp.getPlanId() > 0 ) {
				qp.setIds(new ArrayList<Long>());
				qp.getIds().add((long)qp.getPlanId());
			}
			filterIds = ReportUtil.transferFromLongToInteger(qp.getIds());
		}
		return filterIds;
	}
	
	private List<DeviceViewItem> generateDeviceList(){
		//1、获取推广计划信息
		this.genQueryItemIds();
		List<PlanViewItem> planItems  = reportCproPlanMgr.findPlanViewItemWithoutPagable(qp);
		Map<Integer, PlanViewItem> extPlanViewMapping = new HashMap<Integer, PlanViewItem>(planItems.size());
		for(PlanViewItem tmp : planItems){
			extPlanViewMapping.put(tmp.getPlanId(), tmp);
		}
		
		List<DeviceViewItem> list = new ArrayList<DeviceViewItem>();
		List<DeviceViewItem> mergedData = new ArrayList<DeviceViewItem>();
		
		//2、获取统计数据
		List<DeviceViewItem> olapList  = deviceService.queryPlanDeviceData(userId, planIds,
				null, from, to, null, 0, ReportConstants.TU_NONE);
		
		//3、获取UV数据
		List<Map<String, Object>> uvData = uvDataService.queryPlanDeviceData(userId, planIds, 
				null, from, to, null, 0, ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
		
		List<DeviceViewItem> dorisList = new ArrayList<DeviceViewItem>();
		if (! CollectionUtils.isEmpty(uvData)) {
			for (Map<String, Object> row : uvData) {
				DeviceViewItem item = new DeviceViewItem();
				if (row != null) {
					item.setPlanId(Integer.valueOf(row.get(ReportConstants.PLAN).toString()));
					item.setDeviceId(Integer.valueOf(row.get(ReportConstants.DEVICEID).toString()));
					item.fillStatRecord(row);
					dorisList.add(item);
				}
			}
		}
		

		//8、Merge统计数据、UV数据，复合key为planid+deviceid
		Set<String> mulitKey = new HashSet<String>();
		mulitKey.add(Constants.COLUMN.PLANID);
		mulitKey.add(Constants.COLUMN.DEVICEID);
		mergedData = ReportUtils.mergeItemList(dorisList, olapList, mulitKey, 
				Constants.statMergeVals, DeviceViewItem.class, true);
		
		ReportUtil.generateExtentionFields(mergedData);
		
		//9、构造显示VO列表
		if (!CollectionUtils.isEmpty(mergedData)) {//如果有统计数据

			// 9.3合并数据并过滤
			for (DeviceViewItem record : mergedData) {

				DeviceViewItem deviceItem = record;
				
				//设备类型id
				Integer deviceId = record.getDeviceId();
				deviceItem.setDeviceId(deviceId);
				
				Integer planId = record.getPlanId();
				
				//获取推广计划信息
				PlanViewItem planViewItem = extPlanViewMapping.get(planId);
				if (planViewItem != null) {
					deviceItem.setPlanId(planViewItem.getPlanId());
					deviceItem.setPlanName(planViewItem.getPlanName());
					deviceItem.setPromotionType(planViewItem.getPromotionType());
					if(CproPlanConstant.PROMOTIONTYPE_ALL == planViewItem.getPromotionType()){
						deviceItem.setBidRatio(planViewItem.getBidRatio() / 100); //db中存储的是乘以100的值
					}else{
						deviceItem.setBidRatio(-1); //推广类型为仅移动，出价比例显示为"--"
					}
					if(ReportConstants.DEVICE_PC == deviceItem.getDeviceId()){
						deviceItem.setBidRatio(-1); //电脑设备，出价比例显示为"--"
					}
				}

				//按照过滤条件过滤
				if (!this.isFiltered(deviceItem)) {
					list.add(deviceItem);
				}

			}
		}
		
		return list;
	}
	
	/**
	 * calculateSumData:根据列表返回汇总列
	 * 
	 * @param infoData
	 * @return 返回结果集的汇总信息
	 */
	public DeviceViewItemSum calculateSumData(List<DeviceViewItem> infoData) {
		DeviceViewItemSum sumData = new DeviceViewItemSum();
		for (DeviceViewItem item : infoData) {
			sumData.setClks(sumData.getClks() + item.getClks());
			sumData.setCost(sumData.getCost() + item.getCost());
			sumData.setSrchs(sumData.getSrchs() + item.getSrchs());		
			sumData.setSrchuv(sumData.getSrchuv() + item.getSrchuv());
			sumData.setClkuv(sumData.getClkuv() + item.getClkuv());
		}
		sumData.generateExtentionFields();
		if(null != infoData){
			sumData.setDeviceCount(infoData.size());
		}
		
		return sumData;
	}
	
	/**
	 * isFiltered:判断是否将当前记录过滤
	 *
	 * @param item 待判断VO对象
	 * @return  true表示要过滤，false表示保留    
	 */
	protected boolean isFiltered(DeviceViewItem item) {
		//1、按查询关键词过滤
		String query = qp.getKeyword();
		if(!StringUtils.isEmpty(query)) {
			query = KTKeywordUtil.validateKeyword(query);
		}
		//2、按统计字段过滤
		return ReportWebConstants.filter(qp, item);
	}
	
	/**
	 * generateAccountInfo: 生成报表用的账户信息
	 *
	 * @return      
	*/
	protected ReportAccountInfo generateAccountInfo(String userName) {
		ReportAccountInfo accountInfo = new ReportAccountInfo();
		accountInfo.setReport(this.getText("download.account.report.device"));
		accountInfo.setReportText(this.getText("download.account.report"));

		accountInfo.setAccount(userName);
		accountInfo.setAccountText(this.getText("download.account.account"));

		accountInfo.setDateRange(sd1.format(from) + " - " + sd1.format(to));
		accountInfo.setDateRangeText(this.getText("download.account.daterange"));

		String level = this.getText("download.account.level.allplan");
		if (QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(qp.getLevel())) {
			
			Integer planId = qp.getPlanId();
			if(planId != null ) {
				CproPlan plan = cproPlanMgr.findCproPlanById(planId);
				if (plan != null) {
					level += "/" + this.getText("download.account.level.plan") + plan.getPlanName();
				}
			}
		} else {
			//account级别不用设置具体信息
		}
		accountInfo.setLevel(level);
		accountInfo.setLevelText(this.getText("download.account.level"));

		return accountInfo;
	}

	
	/**
	 * generateReportHeader:生成报表用的列表头
	 * @param showTransData 
	 *
	 * @return      
	 * @since 
	*/
	protected String[] generateReportHeader() {
		
		List<String> header = new ArrayList<String>();
		String prefix = null;
		int maxColumn = 15;
		prefix = "download.device.head.col";
		for ( int col = 0; col < maxColumn; col++ ) {
			header.add(this.getText(prefix + (col + 1)));
		}
		
		String[] array = new String[header.size()];
		return header.toArray(array);
	}
	

	/**
	 * generateReportSummary: 生成推广计划列表汇总信息
	 *
	 * @param sumData 汇总数据
	 * @return 用于表示报表的汇总信息      
	*/
	protected DeviceReportSumData generateReportSummary(DeviceViewItemSum sumData) {

		DeviceReportSumData sum = new DeviceReportSumData(); 
		
		sum.setClks(sumData.getClks());
		sum.setSrchs(sumData.getSrchs());
		sum.setCost(sumData.getCost());
		sum.setAcp(sumData.getAcp().doubleValue());
		sum.setCtr(sumData.getCtr().doubleValue());
		sum.setCpm(sumData.getCpm().doubleValue());
		sum.setSrchuv(sumData.getSrchuv());
		sum.setClkuv(sumData.getClkuv());	
		sum.setSummaryText(this.getText("download.summary.device",
				new String[] { String.valueOf(sumData.getDeviceCount()) }
		));// 添加“合计”
		return sum;
	}

	
	/*********************getter and setter**************************/

	public CproPlanMgr getCproPlanMgr() {
		return cproPlanMgr;
	}

	public void setCproPlanMgr(CproPlanMgr cproPlanMgr) {
		this.cproPlanMgr = cproPlanMgr;
	}

	public ReportCproPlanMgr getReportCproPlanMgr() {
		return reportCproPlanMgr;
	}

	public void setReportCproPlanMgr(ReportCproPlanMgr reportCproPlanMgr) {
		this.reportCproPlanMgr = reportCproPlanMgr;
	}
}

