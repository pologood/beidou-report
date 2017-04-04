package com.baidu.beidou.report.web.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;

import com.baidu.beidou.cprogroup.bo.CproGroup;
import com.baidu.beidou.cprogroup.constant.UnionSiteCache;
import com.baidu.beidou.cprogroup.service.CproGroupMgr;
import com.baidu.beidou.cproplan.bo.CproPlan;
import com.baidu.beidou.cproplan.service.CproPlanMgr;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.olap.constant.Constants;
import com.baidu.beidou.olap.service.RegionStatService;
import com.baidu.unbiz.olap.util.ReportUtils;
import com.baidu.beidou.olap.vo.RegionViewItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.dao.vo.GroupQueryParameter;
import com.baidu.beidou.report.exception.ParameterInValidException;
import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.report.service.ReportCproGroupMgr;
import com.baidu.beidou.report.util.ReportUtil;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.group.ExtGroupViewItem;
import com.baidu.beidou.report.vo.region.RegionAssistant;
import com.baidu.beidou.report.vo.region.RegionReportSumData;
import com.baidu.beidou.report.vo.region.RegionReportVo;
import com.baidu.beidou.report.vo.region.RegionViewItemSum;
import com.baidu.beidou.stat.driver.bo.Constant;
import com.baidu.beidou.stat.driver.bo.Constant.DorisDataType;
import com.baidu.beidou.tool.util.TransReportHelper;
import com.baidu.beidou.tool.vo.TempSitesAndTrans;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.LogUtils;
import com.baidu.beidou.util.StringUtils;

/**
 * 地域report模块Action类
 * @author liuhao05
 *
 */
public class ListCproRegionAction extends BeidouReportActionSupport{
	private static final long serialVersionUID = 1L;
	private CproPlanMgr cproPlanMgr = null;
	private CproGroupMgr cproGroupMgr = null;	
	private ReportCproGroupMgr reportCproGroupMgr = null;
	
	@Resource(name="regionStatServiceImpl")
	RegionStatService regStatMgr;

	/**
	 * 排序方向（正排为1，倒排为-1）
	 */
	private int orient;
	
	/**
	 * 总记录数、行数
	 */
	private int count;
	
	private List<Integer> planIds = new ArrayList<Integer>();
	private List<Integer> groupIds = new ArrayList<Integer>();
	private Map<Integer, Map<Integer,RegionAssistant>> regionGroupMapping = new HashMap<Integer, Map<Integer,RegionAssistant>>();
	private Integer planid;
	private Integer groupid;
	private String beidouBasePath;
	
	/**
	 * 地域比较器
	 */
	class CproRegionComparator implements Comparator<RegionAssistant>  {

		int order;
		String col;
		public CproRegionComparator(String col, int order) {
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
		
		public int compare(RegionAssistant o1, RegionAssistant o2) {
			if(o1 == null) {
				return -1 * order;
			}
			if(o2 == null) {
				return 1 * order;
			}
			if(col.equals(ReportWebConstants.REGION_COLUMN_REGIONNAME)) {
				if(o1.getSelfRegion().getRegionId() == 0){//“其他”排在最后
					return order * 1;
				}else{
					return compareByString(o1.getSelfRegion().getRegionName(), o2.getSelfRegion().getRegionName());
				}
			}
			if(ReportWebConstants.FRONT_BACKEND_ORDERNAME_PLANNAME.equalsIgnoreCase(col)) {
				return compareByString(o1.getSelfRegion().getPlanName(), o2.getSelfRegion().getPlanName());
			}
			if(ReportWebConstants.FRONT_BACKEND_ORDERNAME_GROUPNAME.equalsIgnoreCase(col)) {
				return compareByString(o1.getSelfRegion().getGroupName(), o2.getSelfRegion().getGroupName());
			}
			if(ReportWebConstants.FRONT_BACKEND_ORDERNAME_VIEWSTATE.equalsIgnoreCase(col)) {
				return -1 * order * (int)(o1.getSelfRegion().getViewState() - o2.getSelfRegion().getViewState());
			}
			if(ReportConstants.SRCHS.equals(col)) {
				if((o1.getSelfRegion().getSrchs() - o2.getSelfRegion().getSrchs())==0){
					return o1.getSelfRegionId() - o2.getSelfRegionId();
				}
				else{
					return order * (int)(o1.getSelfRegion().getSrchs() - o2.getSelfRegion().getSrchs());
				}
			}
			
			if(ReportConstants.CLKS.equals(col)) {
				return order * (int)(o1.getSelfRegion().getClks() - o2.getSelfRegion().getClks());
			}
			
			if(ReportConstants.COST.equals(col)) {
				if(o1.getSelfRegion().getCost() > o2.getSelfRegion().getCost()){
					return order * 1;
				}else if(o1.getSelfRegion().getCost() < o2.getSelfRegion().getCost()){
					return order * -1;
				}else{
					return 0;
				}
				
			}
			
			if(ReportConstants.CTR.equals(col)) {
				BigDecimal b1 = o1.getSelfRegion().getCtr();
				BigDecimal b2 = o2.getSelfRegion().getCtr();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}

			if(ReportConstants.ACP.equals(col)) {
				BigDecimal b1 = o1.getSelfRegion().getAcp();
				BigDecimal b2 = o2.getSelfRegion().getAcp();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}

			if(ReportConstants.CPM.equals(col)) {
				BigDecimal b1 = o1.getSelfRegion().getCpm();
				BigDecimal b2 = o2.getSelfRegion().getCpm();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			/**
			 * 增加受众数据和转化数据added by liuhao since cpweb-492
			 */
			if(ReportConstants.SRCHUV.equals(col)) {
				return order * (int)(o1.getSelfRegion().getSrchuv() - o2.getSelfRegion().getSrchuv());
			}
			
			if(ReportConstants.CLKUV.equals(col)) {
				return order * (int)(o1.getSelfRegion().getClkuv() - o2.getSelfRegion().getClkuv());
			}
			
			if(ReportConstants.SRSUR.equals(col)) {
				BigDecimal b1 = o1.getSelfRegion().getSrsur();
				BigDecimal b2 = o2.getSelfRegion().getSrsur();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if(ReportConstants.CUSUR.equals(col)) {
				BigDecimal b1 = o1.getSelfRegion().getCusur();
				BigDecimal b2 = o2.getSelfRegion().getCusur();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if(ReportConstants.COCUR.equals(col)) {
				BigDecimal b1 = o1.getSelfRegion().getCocur();
				BigDecimal b2 = o2.getSelfRegion().getCocur();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if(ReportConstants.RES_TIME_STR.equals(col)) {
				return order * (int)(o1.getSelfRegion().getResTimeStr().compareTo(o2.getSelfRegion().getResTimeStr()));
			}
			
			if(ReportConstants.ARRIVAL_RATE.equals(col)) {
				BigDecimal b1 = o1.getSelfRegion().getArrivalRate();
				BigDecimal b2 = o2.getSelfRegion().getArrivalRate();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if(ReportConstants.HOP_RATE.equals(col)) {
				BigDecimal b1 = o1.getSelfRegion().getHopRate();
				BigDecimal b2 = o2.getSelfRegion().getHopRate();
				if(b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if(ReportConstants.DIRECT_TRANS_CNT.equals(col)) {
				return order * (int)(o1.getSelfRegion().getDirectTrans() - o2.getSelfRegion().getDirectTrans());
			}
			
			if(ReportConstants.INDIRECT_TRANS_CNT.equals(col)) {
				return order * (int)(o1.getSelfRegion().getIndirectTrans() - o2.getSelfRegion().getIndirectTrans());
			}
			
			return order;
		}			
	}
	
	
	/**
	 * <p>initParameter: 初始化参数，一般在列表页用
	 * @throws Exception 
	 *      
	*/
	protected void initParameter() {
		
		//把用户ID和userID封装进QueryParameter中
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
		
		//处理时间参数，封装进QueryParameter中
		try {
			tuneDate();
		} catch (Exception e) {
			throw new ParameterInValidException(ReportWebConstants.ERR_DATE);
		}
		
		initStateMapping();
	}
	
	/**
	 * 初始化参数操作
	 */
	@Override
	protected void initStateMapping() {
		
		if(qp.getPlanId() != null) {
			planIds = new ArrayList<Integer>();
			planIds.add(qp.getPlanId());
		}
		if(qp.getGroupId() != null) {
			groupIds = new ArrayList<Integer>();
			groupIds.add(qp.getGroupId());
		}

		orient = ReportConstants.SortOrder.ASC;
		if (QueryParameterConstant.SortOrder.SORTORDER_DESC
				.equalsIgnoreCase(qp.getOrder())) {
			orient = ReportConstants.SortOrder.DES;		
		}
		if(qp.getPage() == null || qp.getPage() < 0) {
			qp.setPage(0);
		}

		if(qp.getPageSize() == null || qp.getPageSize() < 1) {
			qp.setPageSize(ReportConstants.PAGE_SIZE );
		}
	}
	
	/**
	 * generateAccountInfo: 生成报表用的账户信息的VO
	 * 
	 * @return
	 */
	protected ReportAccountInfo generateAccountInfo(String userName) {
		ReportAccountInfo accountInfo = new ReportAccountInfo();
		accountInfo.setReport(this.getText("download.account.report.region"));
		accountInfo.setReportText(this.getText("download.account.report"));

		accountInfo.setAccount(userName);
		accountInfo.setAccountText(this.getText("download.account.account"));

		accountInfo.setDateRange(sd1.format(from) + " - " + sd1.format(to));
		accountInfo
				.setDateRangeText(this.getText("download.account.daterange"));

		String level = this.getText("download.account.level.allplan");
		if (QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(qp.getLevel())) {
			
			Integer planId = qp.getPlanId();
			if(planId != null ) {
				CproPlan plan = cproPlanMgr.findCproPlanById(planId);
				if (plan != null) {
					level += "/" + this.getText("download.account.level.plan") + plan.getPlanName();
				}
			}
		} else if (QueryParameterConstant.LEVEL.LEVEL_GROUP.equals(qp.getLevel())) {
			Integer groupId = qp.getGroupId();
			if(groupId != null ) {
				CproGroup group = cproGroupMgr.findCproGroupById(groupId);
				if (group != null) {
					CproPlan plan = cproPlanMgr.findCproPlanById(group.getPlanId());
					level += "/" + this.getText("download.account.level.plan") + plan.getPlanName();
					level += "/" + this.getText("download.account.level.group") + group.getGroupName();
				}
			}
			
		}  else {
			//account级别不用设置具体信息
		}
		accountInfo.setLevel(level);
		accountInfo.setLevelText(this.getText("download.account.level"));

		return accountInfo;
	}
	
	/**
	 * generateReportHeader:生成报表用的列表头
	 * 
	 * @return
	 * @since
	 */
	protected String[] generateReportHeader(boolean showTransData) {
		List<String> header = new ArrayList<String>();
		String prefix = null;
		int maxColumn = 16;
		prefix = "download.region.head.col";
		for ( int col = 0; col < maxColumn; col++ ) {
			if(col == 2){
				//第一列是plan如果在plan或者group层级不需要看
				if(qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_PLAN)
						|| qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_GROUP)){
					continue;
				}
			}else if(col == 3){
				//第二列是group如果在group层级不需要看
				if(qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_GROUP)){
					continue;
				}
			}
			header.add(this.getText(prefix + (col + 1)));
		}
		
		if (showTransData) {
			header.add(this.getText("download.head.arrivalrate.holmes"));
			header.add(this.getText("download.head.hoprate.holmes"));
			header.add(this.getText("download.head.restime.holmes"));
			header.add(this.getText("download.head.direct.trans"));
			header.add(this.getText("download.head.indirect.trans"));
		}
		
		String[] array = new String[header.size()];
		return header.toArray(array);
	}
	
	/**
	 * generateReportSummary: 生成报表汇总信息
	 * 
	 * @param sumData 汇总数据VO
	 * @return 用于表示报表的汇总信息VO
	 */
	protected RegionReportSumData generateReportSummary(RegionViewItemSum sumData) {

		RegionReportSumData sum = new RegionReportSumData();
		sum.setClks(sumData.getClks());
		sum.setSrchs(sumData.getSrchs());
		sum.setCost(sumData.getCost());
		sum.setAcp(sumData.getAcp().doubleValue());
		sum.setCtr(sumData.getCtr().doubleValue());
		sum.setCpm(sumData.getCpm().doubleValue());
		sum.setSrchuv(sumData.getSrchuv());
		sum.setClkuv(sumData.getClkuv());	
		sum.setSummaryText(this.getText("download.summary.region",
				new String[] { String.valueOf(sumData.getRegionCount()) }
		));// 添加“合计”
		return sum;
	}
	
	/**
	 * 生成地域报告的汇总数据
	 * @param infoData
	 * @return
	 */
	private RegionViewItemSum generateRegionViewSum(List<RegionAssistant> infoData){
		
		//生成汇总的统计数据
		RegionViewItemSum sumData = new RegionViewItemSum();
		for (RegionAssistant item : infoData) {
			sumData.setClks(sumData.getClks() + item.getSelfRegion().getClks());
			sumData.setCost(sumData.getCost() + item.getSelfRegion().getCost());
			sumData.setSrchs(sumData.getSrchs() + item.getSelfRegion().getSrchs());
			sumData.setSrchuv(sumData.getSrchuv() + item.getSelfRegion().getSrchuv());
			sumData.setClkuv(sumData.getClkuv() + item.getSelfRegion().getClkuv());
		}
		
		// 生成扩展数据
		sumData.generateExtentionFields();
		
		//设置地域数字
		sumData.setRegionCount(infoData.size());
		return sumData;
	}
	
	/**
	 * 判断此次请求是否有过滤条件(包括是否搜索、是否按照统计字段和受众字段过滤)
	 * @return
	 */
	protected boolean hasFilter() {
		return qp.isBoolValue() || !StringUtils.isEmpty(qp.getKeyword()) || qp.hasStatField4Filter();
	}
	
	/**
	 * 获取前端显示VO的List方法
	 * @param infoData
	 * @return
	 */
	private List<RegionViewItem> pagerRegionViewItemList (List<RegionAssistant> infoData) {
		int page = 0;
		int pageSize = ReportConstants.PAGE_SIZE;
		if(qp.getPage() != null && qp.getPage() > 0) {
			page = qp.getPage();
		}
		if(qp.getPageSize() != null && qp.getPageSize() > 0) {
			pageSize = qp.getPageSize();
		}
		
		infoData = ReportWebConstants.subListinPage(infoData, page, pageSize);
		List<RegionViewItem> result = new ArrayList<RegionViewItem>();
		for(RegionAssistant ia : infoData){
			result.addAll(ia.getRegionViewItem());
		}
		return result;
	}
	
	/**
	 * 对港澳台的地域进行二级地域的过滤处理
	 * @param regionData
	 */
	private void filterSpecialRegion(List<RegionAssistant> regionData){
		for(RegionAssistant vo : regionData){
			//如果是港澳台，则不需要二级地域数据，去掉“其他”
			if(vo.getSelfRegion().getRegionId() == ReportWebConstants.REGION_ID_MACAU
					|| 
			   vo.getSelfRegion().getRegionId() == ReportWebConstants.REGION_ID_HONGKONG
				    || 
			   vo.getSelfRegion().getRegionId() == ReportWebConstants.REGION_ID_TAIWAN){
			   vo.setChildRegions(null);
			   vo.getSelfRegion().setChildCount(0);
			}
		}
	}
	
	/**
	 * isFiltered:判断是否将当前记录过滤
	 *
	 * @param item 待判断VO对象
	 * @return  true表示要过滤，false表示保留    
	 */
	protected boolean isFiltered(RegionAssistant item) {
		//1、按查询关键词过滤
		String query = qp.getKeyword();
		if(!StringUtils.isEmpty(query)) {
			if (StringUtils.isEmpty(item.getSelfRegion().getRegionName())) {
				return true;
			} else if( !item.getSelfRegion().getRegionName().contains(query)) {
				return true;
			}
		}
		//2、按统计和受众字段过滤
		return ReportWebConstants.filter(qp,item.getSelfRegion());
	}
	
	/****************************************action方法***********************************/
		
	/**
	 * 处理有展现兴趣列表的action方法
	 * @return
	 */
	public String ajaxRegionList(){		

		// 1、参数初始化
		try {
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return SUCCESS;
		}

		/** 
		 * 如果统计时间混合今天和历史时间，则返回空
		 */
//		if(super.isMixTodayAndBefore(from, to)){
//			log.warn("系统无法提供今日和历史时间混合的报告数据");
//			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED_SERVER);
//			jsonObject.addMsg(ReportWebConstants.ERR_DATE);
//			
//			return SUCCESS;
//		}
		
		// 2、获取RegionAssistantVO列表		
		List<RegionAssistant> infoData = generateRegionItemList();
		
		// 3、获取总记录数
		count = infoData.size();
		
		// 4、排序
		Collections.sort(infoData, new CproRegionComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		RegionViewItemSum sumData = generateRegionViewSum(infoData);

		// 6、生成显示的RegionViewItem列表
		this.filterSpecialRegion(infoData);//对港澳台地域的二级地域的“其他”进行删除处理
		List<RegionViewItem> list = this.pagerRegionViewItemList(infoData);
		
		// 7、计算总页码
		int totalPage = super.getTotalPage(count);
		
		/**
		 * 如果统计时间是今天，将统计数据列中处点击、消费外的其他列置为-1
		 */
//		if(super.isOnlyToday(from, to)){
//			super.clearNotRealtimeStat(list, sumData);
//		}
		
		// 8、构造JSON数据
		jsonObject.addData("list", list);
		jsonObject.addData("cache",0);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("cache", ReportConstants.Boolean.FALSE);
		jsonObject.addData("sum", sumData);

		// 9、处理当日和昨日不入库的uv和转化数据
		this.reportFacade.postHandleTransAndUvData(userId, from, to, list, sumData);
		
		return SUCCESS;
		
	}
	
	
	/**
	 * 下载关键词报告Action方法
	 */
	public String downloadRegionList() throws IOException {

		// 1、参数初始化
		super.initParameter();
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}
		
		/** 
		 * 如果统计时间混合今天和历史时间，则返回空
		 */
//		if(super.isMixTodayAndBefore(from, to)){
//			throw new BusinessException("系统无法提供今日和历史时间混合的报告数据");
//		}
		
		List<RegionAssistant> infoData = new ArrayList<RegionAssistant>();// 目标结果集（可能是全集或者分页集）

		// 2、获取VO列表
		infoData = generateRegionItemList();
		
		// 3、获取总记录数
		count = infoData.size();
		
		// 4、排序
		Collections.sort(infoData, new CproRegionComparator(ReportConstants.SRCHS, orient));
		
		// 5、生成汇总的统计数据
		RegionViewItemSum sumData = generateRegionViewSum(infoData);

		// 6、判断是否需要转化数据
		boolean showTransData = this.transReportFacade.isTransToolSigned(userId, false);
		boolean transDataValid = this.transReportFacade.needToFetchTransData(userId, from, to, false);
		
		/**
		 * 如果统计时间是今天，将统计数据列中点击、消费外的其他列置为-1
		 */
//		if(super.isOnlyToday(from, to)){
//			super.clearNotRealtimeStat(sumData);
//			for(RegionAssistant region : infoData){
//				super.clearNotRealtimeStat(region.getSelfRegion());
//				super.clearNotRealtimeStat(region.getChildRegions(), null);
//			}
//		}
		
		// 7、处理当日和昨日不入库的uv和转化数据 add by wangchongjie since 2012.11.03
		for (RegionAssistant items : infoData ){
			this.reportFacade.postHandleTransAndUvData(userId, from, to, items.getRegionViewItem(), sumData);
		}	
		
		// 8、构造报告VO
		RegionReportVo vo = new RegionReportVo(qp.getLevel());//报表下载使用的VO
		vo.setShowTransData(showTransData);
		vo.setTransDataValid(transDataValid);
		vo.setAccountInfo(generateAccountInfo(user.getUsername()));
		vo.setDetails(infoData);
		vo.setHeaders(generateReportHeader(showTransData));
		vo.setSumData(generateReportSummary(sumData));
		
		// 9、构造输出流
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(vo, output);

		// 10、设置下载需要使用到的一些属性
		byte[] bytes = output.toByteArray();
		inputStream = new ByteArrayInputStream(bytes);
		fileSize = bytes.length;

		if ( qp.getGroupId() != null ) {
			CproGroup group = cproGroupMgr.findCproGroupById(qp.getGroupId());
			if (group != null) {
				fileName = group.getGroupName() + "-";
			}
		} else if ( qp.getPlanId() != null ) {
			CproPlan plan = cproPlanMgr.findCproPlanById(qp.getPlanId());
			if (plan != null) {
				fileName = plan.getPlanName() + "-";
			}
		} else {
			fileName = user.getUsername() + "-";
		}
		fileName += this.getText("download.region.filename.prefix");
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
	
	
	/***********************************action业务方法***********************************************/
		
	/**
	 * 生成用于地域前端显示的VO
	 * @param forFlash 是否用于flash显示
	 * @return 
	 */
	private List<RegionAssistant> generateRegionItemList(){		
		
		//1、要返回的地域列表
		List<RegionAssistant> regionAssistantList = new ArrayList<RegionAssistant>();
		
		//2、构造的查询数据结构
		Map<Integer,List<String>> groupRegionMap = new HashMap<Integer,List<String>>();
		
		//3、获取的统计数据和受众数据
		List<Map<String, Object>> mergedData = new ArrayList<Map<String, Object>>();
		
		//4、获取推广组信息
		GroupQueryParameter queryParam = initGroupQueryParameter();
		List<ExtGroupViewItem> groupItems = reportCproGroupMgr.findExtCproGroupReportInfo(queryParam);
		Map<Integer, ExtGroupViewItem> extGroupViewMapping = new HashMap<Integer, ExtGroupViewItem>(groupItems.size());
		for(ExtGroupViewItem tmp : groupItems){
			extGroupViewMapping.put(tmp.getGroupId(), tmp);
		}
		
		//5、从doris查询当前层级下所有统计信息
		List<RegionViewItem> olapList = regStatMgr.queryGroupRegData(userId, planIds, groupIds, null, null,
					from, to, null, 0, ReportConstants.TU_NONE);
		
		//6、从doris查询当前层级下的所有UV信息
		List<Map<String, Object>> uvData = uvDataService.queryRegData(userId, planIds, groupIds, null, null,
				from, to, null, 0, 
				ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0, ReportConstants.DorisLevelType.ALL);
		
		//7、判断是否需要获取转化数据以及holmes数据
		boolean needToFetchTransData = this.transReportFacade.needToFetchTransData(userId,from,to,false);
		
		if (needToFetchTransData) {
			
			TempSitesAndTrans tempSiteAndTrans = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userId);
			
			//8.1 获取转化数据
			List<Map<String, Object>> transData = transDataService.queryRegData(
								userId, tempSiteAndTrans.getTransSiteIds(), tempSiteAndTrans.getTransTargetIds(), 
								planIds, groupIds, null, null, from, to, null, 0, 
								ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0, ReportConstants.DorisLevelType.ALL);
			
			//8.2 获取holmes数据
			List<Map<String, Object>> holmesData = holmesDataService.queryRegData(
					userId, null, null, planIds, groupIds, null, null, 
					from, to, null, 0, 
					ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);

			
			//8.3 Merge统计数据、UV数据、Holmes数据、转化数据，不需要排序，复合key为groupid+genderid
			List<String> mulitKey = new ArrayList<String>();
			mulitKey.add(ReportConstants.GROUP);
			mulitKey.add(ReportConstants.PROVID);
			mulitKey.add(ReportConstants.CITYID);
			mergedData = this.reportFacade.mergeTransHolmesAndUvDataByMulitKey(DorisDataType.STAT, transData, holmesData, uvData, mulitKey);		
		
		} else {
			
			//8.1 Merge统计数据、UV数据，复合key为groupid+genderid
			mergedData = uvData;
		}
		
		List<RegionViewItem> dorisList = new ArrayList<RegionViewItem>();
		List<RegionViewItem> resultList = new ArrayList<RegionViewItem>();
		//7、填充数据
		if (! CollectionUtils.isEmpty(mergedData)) {
			for (Map<String, Object> row : mergedData) {
				RegionViewItem item = new RegionViewItem();
				if (row != null) {
					item.setPlanId(Integer.valueOf(row.get(ReportConstants.PLAN).toString()));
					item.setGroupId(Integer.valueOf(row.get(ReportConstants.GROUP).toString()));
					item.setFirstRegionId((Integer)row.get(ReportConstants.PROVID));
					item.setSecondRegionId((Integer)row.get(ReportConstants.CITYID));
					item.fillStatRecord(row);
					dorisList.add(item);
				}
			}
		}
		
		Set<String> mergeKeys = new HashSet<String>(Arrays.asList(
				new String[]{Constants.COLUMN.GROUPID, Constants.COLUMN.PROVINCEID, Constants.COLUMN.CITYID}));
		
		resultList = ReportUtils.mergeItemList(dorisList, olapList, mergeKeys, 
				Constants.statMergeVals, RegionViewItem.class, true);
		
		ReportUtil.generateExtentionFields(resultList);
        
		
					
		//9、将地域放入map中，供过滤使用,key为复合key，即推广组id+地域id
		Map<String,RegionViewItem> regionDataMap = new HashMap<String,RegionViewItem>();
		
		if(!CollectionUtils.isEmpty(resultList)){ 
			for(RegionViewItem data: resultList){
				Integer groupid = data.getGroupId();
				Integer provid = data.getFirstRegionId();
				Integer cityid = data.getSecondRegionId();
				
				//构造RegCache查询参数
				List<String> regionIds = null;
				if(groupid != null ){
					regionIds = groupRegionMap.get(groupid);
					if(regionIds == null ){
						regionIds = new ArrayList<String>();
					}
					String key = provid + "_" + cityid;
					regionIds.add(key);
				}
				groupRegionMap.put(groupid, regionIds);
	
				//存储数据,用于以后填充数据
				if((provid != null) && (cityid != null) && (groupid != null)){
					String key = groupid + "_" + provid + "_" + cityid;
					regionDataMap.put(key, data);
				}
			}
			
			//10、通过AdCache接口，获得具有父子关系的有展现的地域树
			this.generateRegionVo(groupRegionMap);
			
			//11、遍历CproItFacade返回的兴趣树列表，构造report显示VOList并过滤
			if(!regionGroupMapping.isEmpty()){
				RegionViewItem data;
				
				//遍历每个推广组
				for (Integer groupid: regionGroupMapping.keySet()){
					Map<Integer,RegionAssistant> regionVoMap= regionGroupMapping.get(groupid);
					for(Integer firstRegionId: regionVoMap.keySet()){
						
						//构造父地域
						RegionAssistant regionAssistant = new RegionAssistant(firstRegionId);
						RegionAssistant firstRegion = regionVoMap.get(firstRegionId);
						RegionViewItem viewItem = new RegionViewItem();		
						viewItem.setRegionId(firstRegionId);
						String key = "";
						if(firstRegionId == 0){
							key = groupid + "_" + firstRegionId + "_" + 0;
							data = regionDataMap.get(key);
						}else{
							key = groupid + "_" + firstRegionId + "_" + ReportConstants.sumFlagInDoris;
							data = regionDataMap.get(key);
						}
						viewItem.fillStatRecord(data);
						viewItem.setRegionName(firstRegion.getSelfRegion().getRegionName());
						viewItem.setRegionId(firstRegion.getSelfRegion().getRegionId());
						viewItem.setType(ReportWebConstants.REGION_TYPE_FIRST);
						
						ExtGroupViewItem groupViewItem = extGroupViewMapping.get(groupid);
						viewItem.setGroupName(groupViewItem.getGroupName());
						viewItem.setViewState(groupViewItem.getViewState());
						viewItem.setPlanName(groupViewItem.getPlanName());
						viewItem.setGroupId(groupViewItem.getGroupId());
						viewItem.setPlanId(groupViewItem.getPlanId());
						
						regionAssistant.setSelfRegion(viewItem);	
						regionAssistant.setChildOrient(orient);
						
						//构造子地域
						for(RegionViewItem childRegion: firstRegion.getChildRegions()){
							RegionViewItem childViewItem = new RegionViewItem();
							key = groupid + "_" + firstRegionId + "_" + childRegion.getRegionId();
							data = regionDataMap.get(key);
							childViewItem.fillStatRecord(data);
							childViewItem.setRegionId(childRegion.getRegionId());
							if(childRegion.getRegionId() == 0){
								childRegion.setRegionId(Integer.MAX_VALUE);
							}
							childViewItem.setRegionName(childRegion.getRegionName());
							childViewItem.setType(ReportWebConstants.REGION_TYPE_SECOND);
							
							groupViewItem = extGroupViewMapping.get(groupid);
							childViewItem.setGroupName(groupViewItem.getGroupName());
							childViewItem.setViewState(groupViewItem.getViewState());
							childViewItem.setPlanName(groupViewItem.getPlanName());
							childViewItem.setGroupId(groupViewItem.getGroupId());
							childViewItem.setPlanId(groupViewItem.getPlanId());		
							
							childViewItem.setFirstRegionId(firstRegionId);
							regionAssistant.addSecondRegion(childViewItem);
						}
						
						//计算扩展列
						regionAssistant.ensureStatFields();
						
						//按照统计字段和UV字段过滤
						if(!isFiltered(regionAssistant))
							regionAssistantList.add(regionAssistant);
					}
				}
			}
		}		
		return regionAssistantList;
	}
	
	
	/**
	 * 通过RegCache相关接口构造地域的父子关系树
	 * @param groupRegionMap
	 */
	private void generateRegionVo(Map<Integer,List<String>> groupRegionMap){
		//1、遍历所有推广组
		for(Integer groupId: groupRegionMap.keySet()){
			
			//2、判断当前推广组地域列表是否存在，若存在取出，若不存在则新建
			Map<Integer,RegionAssistant> groupRegions =  regionGroupMapping.get(groupId);
			if( groupRegions == null){
				groupRegions = new HashMap<Integer,RegionAssistant>();
			}
			regionGroupMapping.put(groupId, groupRegions);
			
			//3、遍历推广组地域id列表
			List<String> regionList = groupRegionMap.get(groupId);
			for(String regionKey:regionList){
				String[] regionIds = regionKey.split("_");
				Integer firstRegionId = Integer.parseInt(regionIds[0]);
				if( firstRegionId == 0){//如果一级地域是其他或者未识别，则没有二级地域，因此不做二级地域的处理
					RegionAssistant assistant = groupRegions.get(firstRegionId);
					if(assistant == null){
						assistant = new RegionAssistant(firstRegionId);
						RegionViewItem firstRegionItem = new RegionViewItem();
						firstRegionItem.setRegionId(firstRegionId);
						firstRegionItem.setRegionName(ReportWebConstants.REGION_OTHER);//设置地域名为“其他”
						assistant.setSelfRegion(firstRegionItem);
						groupRegions.put(firstRegionId, assistant);
					}
				}else{
					Integer secondRegionId = Integer.parseInt(regionIds[1]);
					
					//处理一级地域
					RegionAssistant assistant = groupRegions.get(firstRegionId);
					if(assistant == null){
						assistant = new RegionAssistant(firstRegionId);
						RegionViewItem firstRegionItem = new RegionViewItem();
						firstRegionItem.setRegionId(firstRegionId);
						String regionName = UnionSiteCache.regCache.getRegNameList().get(firstRegionId);
						firstRegionItem.setRegionName(regionName);
						assistant.setSelfRegion(firstRegionItem);
						groupRegions.put(firstRegionId, assistant);
					}
					
					//处理二级地域
					if(secondRegionId != ReportConstants.sumFlagInDoris){
						RegionViewItem secondRegionItem = new RegionViewItem();
						secondRegionItem.setRegionId(secondRegionId);	
						if(secondRegionId != 0){
							String regionName = UnionSiteCache.regCache.getRegNameList().get(secondRegionId);
							secondRegionItem.setRegionName(regionName);
						}else{
							secondRegionItem.setRegionName(ReportWebConstants.REGION_NOTKNOWN);//设置地域名为“未知”
						}
						assistant.addSecondRegion(secondRegionItem);
					}
				}
			}
		}
	}
		
	/**
	 * 返回推广组信息查询参数封装
	 * @return
	 */
	private GroupQueryParameter initGroupQueryParameter(){
		GroupQueryParameter queryParam = new GroupQueryParameter();	
		queryParam.setUserId(userId);
		if(QueryParameterConstant.LEVEL.LEVEL_ACCOUNT.equals(qp.getLevel())){
			//Do nothing
		}else if(QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(qp.getLevel())){
			queryParam.setPlanId(super.qp.getPlanId());			
		}else if(QueryParameterConstant.LEVEL.LEVEL_GROUP.equals(qp.getLevel())){
			List<Long> ids = new ArrayList<Long>(1);
			ids.add(super.qp.getGroupId().longValue());
			queryParam.setIds(ids);
		}
		return queryParam;
	}
	
	/***************************************** getters and setters *****************************************/

	public CproPlanMgr getCproPlanMgr() {
		return cproPlanMgr;
	}

	public void setCproPlanMgr(CproPlanMgr cproPlanMgr) {
		this.cproPlanMgr = cproPlanMgr;
	}

	public CproGroupMgr getCproGroupMgr() {
		return cproGroupMgr;
	}

	public void setCproGroupMgr(CproGroupMgr cproGroupMgr) {
		this.cproGroupMgr = cproGroupMgr;
	}

	public ReportCproGroupMgr getReportCproGroupMgr() {
		return reportCproGroupMgr;
	}

	public void setReportCproGroupMgr(ReportCproGroupMgr reportCproGroupMgr) {
		this.reportCproGroupMgr = reportCproGroupMgr;
	}

	public ReportCproGroupMgr getReportGroupMgr() {
		return reportCproGroupMgr;
	}

	public void setReportGroupMgr(ReportCproGroupMgr reportGroupMgr) {
		this.reportCproGroupMgr = reportGroupMgr;
	}

	public List<Integer> getPlanIds() {
		return planIds;
	}

	public void setPlanIds(List<Integer> planIds) {
		this.planIds = planIds;
	}

	public List<Integer> getGroupIds() {
		return groupIds;
	}

	public void setGroupIds(List<Integer> groupIds) {
		this.groupIds = groupIds;
	}

	public int getOrient() {
		return orient;
	}

	public void setOrient(int orient) {
		this.orient = orient;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public Integer getPlanid() {
		return planid;
	}

	public void setPlanid(Integer planid) {
		this.planid = planid;
	}

	public Integer getGroupid() {
		return groupid;
	}

	public void setGroupid(Integer groupid) {
		this.groupid = groupid;
	}

	public String getBeidouBasePath() {
		return beidouBasePath;
	}

	public void setBeidouBasePath(String beidouBasePath) {
		this.beidouBasePath = beidouBasePath;
	}
}

