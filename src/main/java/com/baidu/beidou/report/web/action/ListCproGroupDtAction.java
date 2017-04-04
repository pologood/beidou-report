package com.baidu.beidou.report.web.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;

import com.baidu.beidou.cprogroup.bo.CproGroup;
import com.baidu.beidou.cprogroup.service.CproGroupMgr;
import com.baidu.beidou.cprogroup.util.KTKeywordUtil;
import com.baidu.beidou.cproplan.bo.CproPlan;
import com.baidu.beidou.cproplan.service.CproPlanMgr;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.olap.constant.Constants;
import com.baidu.beidou.olap.service.GenderStatService;
import com.baidu.unbiz.olap.util.ReportUtils;
import com.baidu.beidou.olap.vo.GenderViewItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.dao.vo.GroupQueryParameter;
import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.report.service.ReportCproGroupMgr;
import com.baidu.beidou.report.util.ReportUtil;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.group.ExtGroupViewItem;
import com.baidu.beidou.report.vo.group.GroupDtReportSumData;
import com.baidu.beidou.report.vo.group.GroupDtReportVo;
import com.baidu.beidou.report.vo.group.GroupDtViewItemSum;
import com.baidu.beidou.stat.driver.bo.Constant;
import com.baidu.beidou.stat.driver.bo.Constant.DorisDataType;
import com.baidu.beidou.tool.util.TransReportHelper;
import com.baidu.beidou.tool.vo.TempSitesAndTrans;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.LogUtils;
import com.baidu.beidou.util.StringUtils;


public class ListCproGroupDtAction extends BeidouReportActionSupport{

	private static final long serialVersionUID = 1L;
	private CproPlanMgr cproPlanMgr = null;
	private CproGroupMgr cproGroupMgr = null;
	private List<Integer> planIds = new ArrayList<Integer>();
	private List<Integer> groupIds = new ArrayList<Integer>();
	private ReportCproGroupMgr reportCproGroupMgr;
	
	@Resource(name="genderStatServiceImpl")
	private GenderStatService genderService;

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
	
	/**
	 * 生成用于兴趣报告前端显示的VO
	 * @return 
	 */
	public String ajaxDtList(){		

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
		
		// 2、生成显示的GroupDtViewItem列表
		List<GenderViewItem> list = generateDtList();
		
		// 3、查询总条数，该数字可能不是最终值，因为有可能需要按统计值过滤，这样的话该值会被再次修改
		int count = list.size();
		
		// 5、生成汇总的统计数据
		GroupDtViewItemSum sumData = calculateSumData(list);

		// 6、计算总页码
		int totalPage = super.getTotalPage(count);
		
		// 7、获取分页
		list = pagerList(list);

		/**
		 * 如果统计时间是今天，将统计数据列中处点击、消费外的其他列置为-1
		 */
//		if(super.isOnlyToday(from, to)){
//			super.clearNotRealtimeStat(list, sumData);
//		}
		
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
	public String downloadDtList() throws IOException{		

		//1、初始化一下参数
		initParameter();
		
		//2、查询账户信息
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}
		
		//3、构造报告VO，下载的CSV共有四部分：1、账户基本信息，2、列头，3、列表，4、汇总信息	
		GroupDtReportVo vo = new GroupDtReportVo(qp.getLevel());//报表下载使用的VO
		
		List<GenderViewItem> infoData = null;//目标plan集
		
		//3.1、获取统计数据
		infoData = generateDtList();//无统计时间粒度

		//3.2、生成汇总的统计数据
		GroupDtViewItemSum sumData = calculateSumData(infoData);
		
		//3.3处理当日和昨日不入库的uv和转化数据 add by wangchongjie since 2012.11.03
		this.reportFacade.postHandleTransAndUvData(userId, from, to, infoData, sumData);

		//3.4、判断是否需要转化数据
		boolean showTransData = this.transReportFacade.isTransToolSigned(userId, false);
		boolean transDataValid = this.transReportFacade.needToFetchTransData(userId, from, to, false);
		
		/**
		 * 如果统计时间是今天，将统计数据列中处点击、消费外的其他列置为-1
		 */
//		if(super.isOnlyToday(from, to)){
//			super.clearNotRealtimeStat(infoData, sumData);
//		}
		
		//3.5、填充数据
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
		fileName += this.getText("download.dt.filename.prefix");
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
	private List<GenderViewItem> generateDtList(){
		//1、获取推广组信息
		GroupQueryParameter queryParam = initGroupQueryParameter();
		List<ExtGroupViewItem> groupItems = reportCproGroupMgr.findExtCproGroupReportInfo(queryParam);
		Map<Integer, ExtGroupViewItem> extGroupViewMapping = new HashMap<Integer, ExtGroupViewItem>(groupItems.size());
		for(ExtGroupViewItem tmp : groupItems){
			extGroupViewMapping.put(tmp.getGroupId(), tmp);
		}
		
		List<GenderViewItem> list = new ArrayList<GenderViewItem>();
		List<Map<String, Object>> mergedData = new ArrayList<Map<String, Object>>();
		List<GenderViewItem> allData = new ArrayList<GenderViewItem>();
		
		//2、获取统计数据
		List<GenderViewItem> olapList=new ArrayList<GenderViewItem>();
		olapList = genderService.queryGroupDtData(userId, planIds, groupIds, null, 
					from, to, null, 0, ReportConstants.TU_NONE);
			
		//3、获取UV数据
		List<Map<String, Object>> uvData = uvDataService.queryDTData(userId, planIds, groupIds, null, 
											from, to, null, 0, 
											ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
		
		//4、判断是否需要获取转化数据以及holmes数据
		boolean needToFetchTransData = this.transReportFacade.needToFetchTransData(userId,from,to,false);
		
		if (needToFetchTransData) {
			
			TempSitesAndTrans tempSiteAndTrans = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userId);
			
			//5、获取转化数据
			List<Map<String, Object>> transData = transDataService.queryDTData(userId, 
									tempSiteAndTrans.getTransSiteIds(), tempSiteAndTrans.getTransTargetIds(), 
									planIds, groupIds, null, from, to, null, 0, 
									ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
			
			//6、获取holmes数据
			List<Map<String, Object>> holmesData = holmesDataService.queryDTData(userId, null, null, planIds, groupIds, null,
																	from, to, null, 0, 
																	ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);	
			
			//7、MergeUV数据、Holmes数据、转化数据，不需要排序，复合key为groupid+genderid
			List<String> mulitKey = new ArrayList<String>();
			mulitKey.add(ReportConstants.GROUP);
			mulitKey.add(ReportConstants.GENDERID);	
			mergedData = this.reportFacade.mergeTransHolmesAndUvDataByMulitKey(DorisDataType.STAT, transData, holmesData, uvData, mulitKey);		
		} else {
			
			//7、不需要Merge，使用UV数据
			mergedData = uvData;
		}
		//将除统计数据之外merger后的数据转换为object
		List<GenderViewItem> dorisList = new ArrayList<GenderViewItem>();
		if (! CollectionUtils.isEmpty(mergedData)) {
			for (Map<String, Object> row : mergedData) {
				GenderViewItem item = new GenderViewItem();
				if (row != null) {
					item.setPlanId(Integer.valueOf(row.get(ReportConstants.PLAN).toString()));
					item.setGroupId(Integer.valueOf(row.get(ReportConstants.GROUP).toString()));
					item.setGenderId(Integer.valueOf(row.get(ReportConstants.GENDERID).toString()));
					if(row.get(ReportConstants.GENDERID).toString().equals("0")){
						item.setGenderId(9);
					}
					item.fillStatRecord(row);
					dorisList.add(item);
				}
			}
		}	
		
		//8、Merge统计数据、其余的Doris数据,复合key为groupid+genderid
		Set<String> mulitKey = new HashSet<String>();
		mulitKey.add(Constants.COLUMN.GROUPID);
		mulitKey.add(Constants.COLUMN.GENDERID);
		allData = ReportUtils.mergeItemList(dorisList, olapList, mulitKey, 
				Constants.statMergeVals, GenderViewItem.class, true);
		
		ReportUtil.generateExtentionFields(allData);
		
		//9、构造显示VO列表
		if(!CollectionUtils.isEmpty(allData)){//如果有统计数据
			
			// 9.1存放gender的map
			Map<String, GenderViewItem> genderMapping = new HashMap<String,GenderViewItem>();			
			for(GenderViewItem record: allData){
				Integer groupId = record.getGroupId();
				Integer genderId = record.getGenderId();
				String key = groupId + "_" + genderId;
				genderMapping.put(key, record);
			}

			// 9.2构造volist
			Map<String, GenderViewItem> dtViewItemMapping = new HashMap<String, GenderViewItem>();
			for(String key : genderMapping.keySet()){
				GenderViewItem record = genderMapping.get(key);
				if(record != null){
					Integer groupId = record.getGroupId();
					Integer genderId = record.getGenderId();
					GenderViewItem genderItem = new GenderViewItem();
					genderItem.setGenderId(genderId);
					genderItem.setGroupId(groupId);
					if(genderItem.getGenderId() == ReportWebConstants.DT_GENDER_MALE){
						genderItem.setGenderName(ReportWebConstants.DT_GENDER_MALE_STRING);
					}else if( genderItem.getGenderId() == ReportWebConstants.DT_GENDER_FEMALE){
						genderItem.setGenderName(ReportWebConstants.DT_GENDER_FEMALE_STRING);
					}else{
						genderItem.setGenderName(ReportWebConstants.DT_GENDER_NOTKNOWN_STRING);
					}
					genderItem.fillStatRecord(record);
					dtViewItemMapping.put(key, genderItem);
				}
			}
			
			// 9.3合并数据并过滤
			for(GenderViewItem genderItem : dtViewItemMapping.values()){
				Integer genderId = genderItem.getGenderId();
				Integer groupId = genderItem.getGroupId();
				
				//合并未传入（9）和未识别（0）数据项
				if(!genderItem.isDeleted()){
					if((genderItem.getGenderId() == ReportWebConstants.DT_GENDER_NOTINPUT 
							|| 
					   genderItem.getGenderId() == ReportWebConstants.DT_GENDER_NOTKNOWN)){
						Integer otherId = (genderId == ReportWebConstants.DT_GENDER_NOTKNOWN) ? ReportWebConstants.DT_GENDER_NOTINPUT : ReportWebConstants.DT_GENDER_NOTKNOWN;
						String otherKey = groupId + "_" + otherId;
						GenderViewItem otherGenderItem = dtViewItemMapping.get(otherKey);
						if(otherGenderItem != null){
							if(!otherGenderItem.isDeleted()){
								genderItem.setSrchs(genderItem.getSrchs() + otherGenderItem.getSrchs());
								genderItem.setSrchuv(genderItem.getSrchuv() + otherGenderItem.getSrchuv());
								genderItem.setClks(genderItem.getClks() + otherGenderItem.getClks());
								genderItem.setClkuv(genderItem.getClkuv() + otherGenderItem.getClkuv());
								genderItem.setCost(genderItem.getCost() + otherGenderItem.getCost());
								genderItem.setHolmesClks(genderItem.getHolmesClks() + otherGenderItem.getHolmesClks());
								genderItem.setArrivalCnt(genderItem.getArrivalCnt() + otherGenderItem.getArrivalCnt());
								genderItem.setEffectArrCnt(genderItem.getEffectArrCnt() + otherGenderItem.getEffectArrCnt());
								genderItem.setHopCnt(genderItem.getHopCnt() + otherGenderItem.getHopCnt());
								genderItem.setResTime(genderItem.getResTime() + otherGenderItem.getResTime());
								genderItem.setDirectTrans(genderItem.getDirectTrans() + otherGenderItem.getDirectTrans());
								genderItem.setIndirectTrans(genderItem.getIndirectTrans() + otherGenderItem.getIndirectTrans());
								genderItem.generateExtentionFields();
								otherGenderItem.setDeleted(true);
							}
						}
						
						//获取推广组信息
						ExtGroupViewItem groupViewItem = extGroupViewMapping.get(groupId);
						if(groupViewItem != null){
							genderItem.setGroupName(groupViewItem.getGroupName());
							genderItem.setPlanId(groupViewItem.getPlanId());
							genderItem.setPlanName(groupViewItem.getPlanName());
							genderItem.setViewState(groupViewItem.getViewState());
							genderItem.setViewStateOrder(groupViewItem.getViewStateOrder());
						}
						
						//按照过滤条件过滤
						if(!this.isFiltered(genderItem)){
							list.add(genderItem);
						}
						
					}else{
						
						//获取推广组信息
						ExtGroupViewItem groupViewItem = extGroupViewMapping.get(groupId);
						if(groupViewItem != null){
							genderItem.setGroupName(groupViewItem.getGroupName());
							genderItem.setPlanId(groupViewItem.getPlanId());
							genderItem.setPlanName(groupViewItem.getPlanName());
							genderItem.setViewState(groupViewItem.getViewState());
							genderItem.setViewStateOrder(groupViewItem.getViewStateOrder());
						}
						
						//按照过滤条件过滤
						if(!this.isFiltered(genderItem)){
							list.add(genderItem);
						}
						
					}
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
	public GroupDtViewItemSum calculateSumData(List<GenderViewItem> infoData) {
		GroupDtViewItemSum sumData = new GroupDtViewItemSum();
		for (GenderViewItem item : infoData) {
			sumData.setClks(sumData.getClks() + item.getClks());
			sumData.setCost(sumData.getCost() + item.getCost());
			sumData.setSrchs(sumData.getSrchs() + item.getSrchs());		
			sumData.setSrchuv(sumData.getSrchuv() + item.getSrchuv());
			sumData.setClkuv(sumData.getClkuv() + item.getClkuv());
		}
		sumData.generateExtentionFields();
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
	protected boolean isFiltered(GenderViewItem item) {
		//1、按查询关键词过滤
		String query = qp.getKeyword();
		if(!StringUtils.isEmpty(query)) {
			// add by kanghongwei since cpweb429(qtIM)
			query = KTKeywordUtil.validateKeyword(query);
			if (StringUtils.isEmpty(item.getGenderName())) {
				return true;
			} else if( !item.getGenderName().contains(query)) {
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
		accountInfo.setReport(this.getText("download.account.report.dt"));
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
	 * @param showTransData 
	 *
	 * @return      
	 * @since 
	*/
	protected String[] generateReportHeader(boolean showTransData) {
		List<String> header = new ArrayList<String>();
		String prefix = null;
		int maxColumn = 15;
		prefix = "download.dt.head.col";
		for ( int col = 0; col < maxColumn; col++ ) {
			if(col == 1){
				//第一列是plan如果在plan或者group层级不需要看
				if(qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_PLAN)
						|| qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_GROUP)){
					continue;
				}
			}else if(col == 2){
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
	 * generateReportSummary: 生成推广计划列表汇总信息
	 *
	 * @param sumData 汇总数据
	 * @return 用于表示报表的汇总信息      
	*/
	protected GroupDtReportSumData generateReportSummary(GroupDtViewItemSum sumData) {

		GroupDtReportSumData sum = new GroupDtReportSumData(); 
		
		sum.setClks(sumData.getClks());
		sum.setSrchs(sumData.getSrchs());
		sum.setCost(sumData.getCost());
		sum.setAcp(sumData.getAcp().doubleValue());
		sum.setCtr(sumData.getCtr().doubleValue());
		sum.setCpm(sumData.getCpm().doubleValue());
		sum.setSrchuv(sumData.getSrchuv());
		sum.setClkuv(sumData.getClkuv());	
		return sum;
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
	
}

