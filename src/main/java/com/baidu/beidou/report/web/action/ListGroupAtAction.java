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

import com.baidu.beidou.atleft.bo.CproGroupAtleft;
import com.baidu.beidou.atleft.rpc.ir.IrFacade;
import com.baidu.beidou.atleft.rpc.ir.params.Id2WordRespResult.IdName;
import com.baidu.beidou.atleft.service.AtLeftGroupService;
import com.baidu.beidou.cprogroup.bo.CproGroup;
import com.baidu.beidou.cprogroup.service.CproGroupMgr;
import com.baidu.beidou.cprogroup.util.KTKeywordUtil;
import com.baidu.beidou.cproplan.bo.CproPlan;
import com.baidu.beidou.cproplan.service.CproPlanMgr;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.olap.constant.Constants;
import com.baidu.beidou.olap.service.AtStatService;
import com.baidu.beidou.olap.vo.AtViewItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.dao.vo.GroupQueryParameter;
import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.report.service.ReportCproGroupMgr;
import com.baidu.beidou.report.util.ReportUtil;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.app.AppViewItemSum;
import com.baidu.beidou.report.vo.at.AtReportSumData;
import com.baidu.beidou.report.vo.at.AtReportVo;
import com.baidu.beidou.report.vo.at.AtViewItemSum;
import com.baidu.beidou.report.vo.group.AtItemKey;
import com.baidu.beidou.report.vo.group.ExtGroupViewItem;
import com.baidu.beidou.stat.driver.bo.Constant;
import com.baidu.beidou.tool.util.TransReportHelper;
import com.baidu.beidou.tool.vo.TempSitesAndTrans;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.LogUtils;
import com.baidu.beidou.util.StringUtils;
import com.baidu.beidou.util.TokenUtil;
import com.baidu.beidou.util.memcache.BeidouCacheInstance;
import com.baidu.unbiz.olap.util.ReportUtils;


public class ListGroupAtAction extends BeidouReportActionSupport{

	private static final long serialVersionUID = 1L;
	private static final int TOKEN_EXPIRE = 60;

	private CproPlanMgr cproPlanMgr = null;
	private CproGroupMgr cproGroupMgr = null;
	private List<Integer> planIds = new ArrayList<Integer>();
	private List<Integer> groupIds = new ArrayList<Integer>();
	private ReportCproGroupMgr reportCproGroupMgr;
	private AtLeftGroupService atLeftGroupService;
	private IrFacade irFacade;
	
	@Resource(name="atStatServiceImpl")
	private AtStatService atService;
	
	private boolean hasAt;//当前层级（未过滤时）是否含有At
	
	private String beidouBasePath;
	private String token;


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
	 * 生成用于At报告前端显示的VO
	 * @return 
	 */
	public String ajaxAtList(){		

		// 1、参数初始化
		try {
			this.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return SUCCESS;
		}
		
		// 2、生成显示的AtViewItem列表
		List<AtViewItem> list = generateAtList();
		
		// 3、查询总条数，该数字可能不是最终值，因为有可能需要按统计值过滤，这样的话该值会被再次修改
		int count = list.size();
		
		// 4、排序
		Collections.sort(list, new CproAtComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		AtViewItemSum sumData = calculateSumData(list);

		// 6、计算总页码
		int totalPage = super.getTotalPage(count);
		
		// 7、获取分页
		list = pagerList(list);

		/**
		 * 如果统计时间是今天，将统计数据列中处点击、消费外的其他列置为-1
		 */
		
		jsonObject.addData("cache",1);
		jsonObject.addData("list", list);
		jsonObject.addData("hasAt", hasAt);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("atCount", count);
		jsonObject.addData("sum", sumData);
		
		//8、处理当日和昨日不入库的uv和转化数据
		this.reportFacade.postHandleTransAndUvData(userId, from, to, list, sumData);

		return SUCCESS;		
	}
	
	/**
	 * 生成用于at报告前端显示的VO
	 * @return 
	 */
	public String downloadAtList() throws IOException{		

		//1、初始化一下参数
		initParameter();
		
		//2、查询账户信息
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}
		
		//3、构造报告VO，下载的CSV共有四部分：1、账户基本信息，2、列头，3、列表，4、汇总信息	
		AtReportVo vo = new AtReportVo(qp.getLevel());//报表下载使用的VO
		
		List<AtViewItem> infoData = null;//目标plan集
		
		//3.1、获取统计数据
		infoData = generateAtList();//无统计时间粒度
		
		//3.2、排序
		Collections.sort(infoData, new CproAtComparator(qp.getOrderBy(), orient));

		//3.3、生成汇总的统计数据
		AtViewItemSum sumData = calculateSumData(infoData);
		
		//3.4处理当日和昨日不入库的uv和转化数据 add by wangchongjie since 2012.11.03
		this.reportFacade.postHandleTransAndUvData(userId, from, to, infoData, sumData);
		
		//3.5、判断是否需要转化数据
		boolean showTransData = this.transReportFacade.isTransToolSigned(userId, false);
		boolean transDataValid = this.transReportFacade.needToFetchTransData(userId, from, to, false);
		
		//3.6、填充数据
		vo.setShowTransData(showTransData);
		vo.setTransDataValid(transDataValid);
		vo.setAccountInfo(generateAccountInfo(user.getUsername()));
		vo.setDetails(infoData);
		vo.setHeaders(generateReportHeader(showTransData));
		vo.setSummary(generateReportSummary(sumData));
		
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(vo, output);
		
		//4、设置下载需要使用到的一些属性
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
		fileName += this.getText("download.at.filename.prefix");
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
	 * AT比较器
	 */
	class CproAtComparator implements Comparator<AtViewItem>  {

		int order;
		String col;
		public CproAtComparator(String col, int order) {
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
		
		public int compare(AtViewItem o1, AtViewItem o2) {
			if(o1 == null) {
				return -1 * order;
			}
			if(o2 == null) {
				return 1 * order;
			}
			if(ReportWebConstants.AT_COLUMN_ATNAME.equals(col)) {
				return compareByString(o1.getAtName(), o2.getAtName());
			}
			if(ReportWebConstants.FRONT_BACKEND_ORDERNAME_PLANNAME.equalsIgnoreCase(col)) {
				return compareByString(o1.getPlanName(), o2.getPlanName());
			}
			if(ReportWebConstants.FRONT_BACKEND_ORDERNAME_GROUPNAME.equalsIgnoreCase(col)) {
				return compareByString(o1.getGroupName(), o2.getGroupName());
			}
			if(ReportWebConstants.FRONT_BACKEND_ORDERNAME_VIEWSTATE.equalsIgnoreCase(col)) {
				return -1 * order * (int)(o1.getViewState() - o2.getViewState());
			}
			if(ReportConstants.SRCHS.equals(col)) {
				if((o1.getSrchs() - o2.getSrchs())==0){
					return o1.getAtId().intValue() - o2.getAtId().intValue();
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
			
			if(null == col) {
				if((o1.getAtType() - o2.getAtType())==0){
					return order * (int)(o1.getSrchs() - o2.getSrchs());
				}
				else{
					return order * (int)(o1.getAtType() - o2.getAtType());
				}
			}
			
			return order;
		}			
	}
	
	private List<AtViewItem> getStatAndTransData() {
		
		List<AtViewItem> allData;
		List<Map<String, Object>> mergedData = new ArrayList<Map<String, Object>>();
		
		//1、获取统计数据
		List<AtViewItem> olapList  = atService.queryGroupAtData(userId, planIds, groupIds, null, null, 
				from, to, null, 0, ReportConstants.TU_NONE);
		
		//2、获取UV数据
		List<Map<String, Object>> uvData = uvDataService.queryGroupAtData(userId, planIds, groupIds,  
				null, null,
				from, to, null, 0, ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
		
		//3、合并转化、Holmes等其他数据
		// 排序参数
		List<String> idKeys = new ArrayList<String>();
		idKeys.add(ReportConstants.GROUP);
		idKeys.add(ReportConstants.ATID);
		idKeys.add(ReportConstants.ATTYPE);

		
		// 如果需要获取转化数据，则获取转化数据并合并
		boolean needToFetchTransData = this.transReportFacade.needToFetchTransData(userId, from, to, false);
		if (needToFetchTransData) {
			
			TempSitesAndTrans tempSiteAndTrans = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userId);
			
			// 获取转化数据
			List<Map<String, Object>> transData = transDataService.queryGroupAtData(userId, tempSiteAndTrans.getTransSiteIds(), 
					tempSiteAndTrans.getTransTargetIds(), planIds, groupIds, null, null, from, to, null, 0,
					ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
			
			// 获取holmes数据
			List<Map<String, Object>> holmesData = holmesDataService.queryGroupAtData(userId, null, null, planIds, groupIds, null, null, 
					from, to, null, 0, ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);

			// Merge基本统计/UV数据与转化数据/holmes数据
			mergedData = this.reportFacade.mergeTransHolmesAndUvDataByMulitKey(Constant.DorisDataType.STAT, 
					transData, holmesData, uvData, idKeys);
		}else {
			//7、不需要Merge，使用UV数据
			mergedData = uvData;
			
		}
		//将除统计数据之外merger后的数据转换为object
		List<AtViewItem> dorisList = new ArrayList<AtViewItem>();
		if (! CollectionUtils.isEmpty(mergedData)) {
			for (Map<String, Object> row : mergedData) {
				AtViewItem item = new AtViewItem();
				if (row != null) {
					item.setPlanId(Integer.valueOf(row.get(ReportConstants.PLAN).toString()));
					item.setGroupId(Integer.valueOf(row.get(ReportConstants.GROUP).toString()));
					item.setAtId(Long.valueOf(row.get(ReportConstants.ATID).toString()));
					item.setAtType(Integer.valueOf(row.get(ReportConstants.ATTYPE).toString()));
					item.fillStatRecord(row);
					dorisList.add(item);
				}
			}
		}	
		
		//8、Merge统计数据、其余的Doris数据,复合key为groupid+atid+attype
		Set<String> mulitKey = new HashSet<String>();
		mulitKey.add(Constants.COLUMN.GROUPID);
		mulitKey.add(Constants.COLUMN.ATID);
		mulitKey.add(Constants.COLUMN.ATTYPE);
		
		allData = ReportUtils.mergeItemList(dorisList, olapList, mulitKey, 
				Constants.statMergeVals, AtViewItem.class, true);
		
		ReportUtil.generateExtentionFields(allData);
		
		
		return allData;
	}
	
	private List<AtViewItem> generateAtList(){
		//1、获取推广组信息
		GroupQueryParameter queryParam = initGroupQueryParameter();
		List<ExtGroupViewItem> groupItems = reportCproGroupMgr.findExtCproGroupReportInfo(queryParam);
		Map<Integer, ExtGroupViewItem> extGroupViewMapping = new HashMap<Integer, ExtGroupViewItem>(groupItems.size());
		for(ExtGroupViewItem tmp : groupItems){
			extGroupViewMapping.put(tmp.getGroupId(), tmp);
		}
		
		List<AtViewItem> list = new ArrayList<AtViewItem>();
		List<AtViewItem> mergedData = this.getStatAndTransData();
		
		//查询已有词
		List<CproGroupAtleft> atLeftList = atLeftGroupService.findByLevelId(qp.getGroupId(), qp.getPlanId(), userId);
		List<AtViewItem> existAt = new ArrayList<AtViewItem>();
		for(CproGroupAtleft atLeft : atLeftList){
			AtViewItem dbItem = new AtViewItem(atLeft);
			//获取推广组信息
			ExtGroupViewItem groupViewItem = extGroupViewMapping.get(dbItem.getGroupId());
			if (groupViewItem != null) {
				dbItem.setGroupName(groupViewItem.getGroupName());
				dbItem.setPlanId(groupViewItem.getPlanId());
				dbItem.setPlanName(groupViewItem.getPlanName());
				dbItem.setViewState(groupViewItem.getViewState());
				dbItem.setViewStateOrder(groupViewItem.getViewStateOrder());
			}
			existAt.add(dbItem);
		}
		Map<AtItemKey, AtViewItem> mapView = new HashMap<AtItemKey, AtViewItem>(existAt.size());//用户所选的At
		for (AtViewItem at : existAt) {
			AtItemKey key = new AtItemKey(at.getGroupId(), at.getAtId(), at.getAtType());
			mapView.put(key, at);
		}
				
		//9、构造显示VO列表
		if (!CollectionUtils.isEmpty(mergedData)) {//如果有统计数据
			hasAt = true;
			// 9.3合并数据并过滤
			for (AtViewItem record : mergedData) {

				AtViewItem atItem = new AtViewItem();
				atItem.fillStatRecord(record);
				
				Integer groupId = record.getGroupId();
				atItem.setGroupId(groupId);
				//获取推广组信息
				ExtGroupViewItem groupViewItem = extGroupViewMapping.get(groupId);
				if (groupViewItem != null) {
					atItem.setGroupName(groupViewItem.getGroupName());
					atItem.setPlanId(groupViewItem.getPlanId());
					atItem.setPlanName(groupViewItem.getPlanName());
					atItem.setViewState(groupViewItem.getViewState());
					atItem.setViewStateOrder(groupViewItem.getViewStateOrder());
				}
				
				atItem.setAtId(record.getAtId());
				atItem.setAtType(record.getAtType());
				
				String atText = "未知";
				AtViewItem dbItem = mapView.get(new AtItemKey(atItem.getGroupId(), atItem.getAtId(), atItem.getAtType()));
				if(dbItem != null){
					atText = dbItem.getAtName();
					atItem.setAtleftId(dbItem.getAtleftId());
					atItem.setHasDel(false);
				}else{
					List<Integer> wordIds = new ArrayList<Integer>();
					wordIds.add(atItem.getAtId().intValue());
					List<IdName> idNames = irFacade.getWordNameByIds(wordIds);
					if(CollectionUtils.isNotEmpty(idNames)){
						atText = idNames.get(0).getAtWordText();
					}
					atItem.setHasDel(true);
				}
				atItem.setAtName(atText);
				
				//按照过滤条件过滤
				if (!this.isFiltered(atItem)) {
					list.add(atItem);
				}
				mapView.remove(new AtItemKey(atItem.getGroupId(), atItem.getAtId(), atItem.getAtType()));
			}
		}
		
		//添加其它已购词
		if(CollectionUtils.isNotEmpty(mapView.keySet())){
			hasAt = true;
			for(AtViewItem item : mapView.values()){
				if(!this.isFiltered(item)){
					list.add(item);
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
	public AtViewItemSum calculateSumData(List<AtViewItem> infoData) {
		AtViewItemSum sumData = new AtViewItemSum();
		for (AtViewItem item : infoData) {
			sumData.setClks(sumData.getClks() + item.getClks());
			sumData.setCost(sumData.getCost() + item.getCost());
			sumData.setSrchs(sumData.getSrchs() + item.getSrchs());		
			sumData.setSrchuv(sumData.getSrchuv() + item.getSrchuv());
			sumData.setClkuv(sumData.getClkuv() + item.getClkuv());
		}
		sumData.generateExtentionFields();
		if(null != infoData){
			sumData.setAtCount(infoData.size());
		}
		
		return sumData;
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
	
	/**
	 * isFiltered:判断是否将当前记录过滤
	 *
	 * @param item 待判断VO对象
	 * @return  true表示要过滤，false表示保留    
	 */
	protected boolean isFiltered(AtViewItem item) {
		//1、按查询关键词过滤
		String query = qp.getKeyword();
		if(!StringUtils.isEmpty(query)) {
			query = KTKeywordUtil.validateKeyword(query);
			if (StringUtils.isEmpty(item.getAtName())) {
				return true;
			} else if( !item.getAtName().contains(query) && !String.valueOf(item.getAtId()).contains(query)) {
				return true;
			}
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
		accountInfo.setReport(this.getText("download.account.report.at"));
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
	 * @param showTransData 
	 *
	 * @return      
	 * @since 
	*/
	protected String[] generateReportHeader(boolean showTransData) {
		List<String> header = new ArrayList<String>();
		String prefix = null;
		int maxColumn = 15;
		prefix = "download.at.head.col";
		for ( int col = 0; col < maxColumn; col++ ) {
			if(col == 2){
				//第二列是plan如果在plan或者group层级不需要看
				if(qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_PLAN)
						|| qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_GROUP)){
					continue;
				}
			}else if(col == 3){
				//第三列是group如果在group层级不需要看
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
	 * generateReportSummary: 生成推广计划列表汇总信息
	 *
	 * @param sumData 汇总数据
	 * @return 用于表示报表的汇总信息      
	*/
	protected AtReportSumData generateReportSummary(AtViewItemSum sumData) {

		AtReportSumData sum = new AtReportSumData(); 
		
		sum.setClks(sumData.getClks());
		sum.setSrchs(sumData.getSrchs());
		sum.setCost(sumData.getCost());
		sum.setAcp(sumData.getAcp().doubleValue());
		sum.setCtr(sumData.getCtr().doubleValue());
		sum.setCpm(sumData.getCpm().doubleValue());
		sum.setSrchuv(sumData.getSrchuv());
		sum.setClkuv(sumData.getClkuv());	
		
		sum.setSummaryText(this.getText("download.summary.at",
				new String[] { String.valueOf(sumData.getAtCount()) }));// 添加“合计”
		
		return sum;
	}
	
	public String deleteAllAt(){

		// 1、参数初始化
		try {
			this.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return ERROR;
		}
		List<AtViewItem> list = this.generateAtList();
		if(CollectionUtils.isEmpty(list)){
			this.token = "";
			buildEmptyResult();
		}else{
			generateAtToken(list);
		}	
		return "deleteAllAt";
	}
	
	private void generateAtToken(List<AtViewItem> list){
		this.token = "";
		//groupid -> atLeftId list
		HashMap<Integer, List<Long>> result = new HashMap<Integer, List<Long>>();
		for(AtViewItem item : list){
			List<Long> tmp = result.get(item.getGroupId());
			if(tmp == null){
				tmp = new ArrayList<Long>();
				result.put(item.getGroupId(), tmp);
			}
			tmp.add(item.getAtleftId());				
		}	
		String token = TokenUtil.generateToken();
		this.token = token;
		BeidouCacheInstance.getInstance().memcacheRandomSet(token, result, TOKEN_EXPIRE);
		buildTokenJson(token);
	}
	
	private void buildTokenJson(String token){
		jsonObject.addData("token", token);
	}
	
	private void buildEmptyResult(){
		jsonObject.addData("list", Collections.emptyList());
		jsonObject.addData("totalPage", 0);
		jsonObject.addData("hasAt", false);
		jsonObject.addData("cache", ReportConstants.Boolean.TRUE);
		jsonObject.addData("sum", new AppViewItemSum(0));
	}
	
	/*********************getter and setter**************************/
	public ReportCproGroupMgr getReportCproGroupMgr() {
		return reportCproGroupMgr;
	}

	public void setReportCproGroupMgr(ReportCproGroupMgr reportCproGroupMgr) {
		this.reportCproGroupMgr = reportCproGroupMgr;
	}

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

	public AtLeftGroupService getAtLeftGroupService() {
		return atLeftGroupService;
	}

	public void setAtLeftGroupService(AtLeftGroupService atLeftGroupService) {
		this.atLeftGroupService = atLeftGroupService;
	}
	
	public IrFacade getIrFacade() {
		return irFacade;
	}

	public void setIrFacade(IrFacade irFacade) {
		this.irFacade = irFacade;
	}
	
	public boolean isHasAt() {
		return hasAt;
	}

	public void setHasAt(boolean hasAt) {
		this.hasAt = hasAt;
	}
	

	public String getBeidouBasePath() {
		return beidouBasePath;
	}

	public void setBeidouBasePath(String beidouBasePath) {
		this.beidouBasePath = beidouBasePath;
	}
	
	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}

