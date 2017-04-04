package com.baidu.beidou.report.web.action;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;

import com.baidu.beidou.cprogroup.bo.TrashObject;
import com.baidu.beidou.cprogroup.constant.UnionSiteCache;
import com.baidu.beidou.cprogroup.service.InterestMgr;
import com.baidu.beidou.cprogroup.vo.InterestCacheObject;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.exception.ParameterInValidException;
import com.baidu.beidou.report.vo.audience.AudienceAssistantVo;
import com.baidu.beidou.report.vo.audience.AudienceSumDataVo;
import com.baidu.beidou.report.vo.audience.BaseAudienceVo;
import com.baidu.beidou.report.vo.comparator.CproAudienceAssistantVoComparator;
import com.baidu.beidou.report.vo.comparator.CproAudienceBaseVoComparator;
import com.baidu.beidou.stat.facade.AudienceAnalyFacade;
import com.baidu.beidou.stat.vo.AudienceVo;
import com.baidu.beidou.util.BeidouCoreConstant;

/**
 * report模块受众分析Action类
 * @author liuhao05
 *
 */
public class ListCproAudiencection extends BeidouReportActionSupport{
	private static final long serialVersionUID = -8090744106335305018L;	
	
	/**
	 * 受众分析业务Facade
	 */
	private AudienceAnalyFacade audienceAnalyFacade = null;
	
	/**
	 * 原始兴趣点的服务
	 */
	private InterestMgr interestMgr;
	
	/**
	 * 排序方向（正排为1，倒排为-1）
	 */
	private int orient;

	private List<Integer> planIds = new ArrayList<Integer>();
	private List<Integer> groupIds = new ArrayList<Integer>();
	private String beidouBasePath;
	
	/**
	 * 存放构造的地域的父子关系Mapping
	 */
	private Map<Integer, AudienceAssistantVo> regionMapping = new HashMap<Integer, AudienceAssistantVo>();	
	
	/**
	 * 存放构造的兴趣的父子关系Mapping
	 */
	private Map<Integer, AudienceAssistantVo> interestMapping = new HashMap<Integer, AudienceAssistantVo>();	
	
	/**
	 * <p>initParameter: 初始化参数，一般在列表页用
	 * @throws Exception 
	 *      
	*/
	protected void initParameter() {
		
		//把用户ID封装进QueryParameter中
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
	 * 获取Assistant前端显示VO的List方法（分页）
	 * @param infoData
	 * @return
	 */
	private List<BaseAudienceVo> pagerAssistantViewItemList (List<AudienceAssistantVo> infoData) {
		int page = 0;
		int pageSize = ReportConstants.PAGE_SIZE;
		if(qp.getPage() != null && qp.getPage() > 0) {
			page = qp.getPage();
		}
		if(qp.getPageSize() != null && qp.getPageSize() > 0) {
			pageSize = qp.getPageSize();
		}
		
		infoData = ReportWebConstants.subListinPage(infoData, page, pageSize);
		List<BaseAudienceVo> result = new ArrayList<BaseAudienceVo>();
		for(AudienceAssistantVo ia : infoData){
			result.addAll(ia.getAllViewItems());
		}
		return result;
	}
	
	/**
	 * 获取Assistant前端显示VO的List方法（获取全部VO不分页）
	 * @param infoData
	 * @return
	 */
	private List<BaseAudienceVo> allAssistantViewItemList (List<AudienceAssistantVo> infoData) {		
		List<BaseAudienceVo> result = new ArrayList<BaseAudienceVo>();
		for(AudienceAssistantVo ia : infoData){
			result.addAll(ia.getAllViewItems());
		}
		return result;
	}
	
	/****************************************action方法***********************************/
	
	/**
	 * 处理受众分布概况的action方法
	 * @return
	 */
	public String ajaxAudienceInfo(){		

		// 1、参数初始化
		try {
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return SUCCESS;
		}

		// 2、获取展现受众、点击受众、转化受众概况	
		Map<String, AudienceVo> audienceInfoMap = audienceAnalyFacade.queryAudienceAnalyData(userId, planIds, groupIds, from, to);
		AudienceVo srchuvAudienceInfo = audienceInfoMap.get("srchuv");
		AudienceVo clkuvAudienceInfo = audienceInfoMap.get("clkuv");
		AudienceVo transuvAudienceInfo = audienceInfoMap.get("transuv");
		
		// 3、计算受众点击率和受众转化率
		DecimalFormat df6 = new DecimalFormat("#.######");//6位小数
		BigDecimal cusur = BigDecimal.valueOf(-1);
		if((srchuvAudienceInfo != null) && (srchuvAudienceInfo.getSumUv() != 0)){
			cusur = BigDecimal.valueOf(clkuvAudienceInfo.getSumUv()*1.0d / srchuvAudienceInfo.getSumUv()*1.0d);
		}
	
		BigDecimal tucur = BigDecimal.valueOf(-1);
		if((clkuvAudienceInfo != null) && (clkuvAudienceInfo.getSumUv() != 0)){
			tucur = BigDecimal.valueOf(transuvAudienceInfo.getSumUv()*1.0d / clkuvAudienceInfo.getSumUv()*1.0d);
		}
	
		// 4、构造JSON数据
		jsonObject.addData("srchuv", audienceInfoMap.get("srchuv"));
		jsonObject.addData("clkuv", audienceInfoMap.get("clkuv"));
		jsonObject.addData("transuv", audienceInfoMap.get("transuv"));
		jsonObject.addData("cusur", df6.format(cusur));
		jsonObject.addData("tucur", df6.format(tucur));
		jsonObject.addData("cache", ReportConstants.Boolean.FALSE);

		return SUCCESS;
		
	}
	
	/**
	 * 处理性别受众分布
	 * @return
	 */
	public String ajaxGenderAudienceList(){		

		// 1、参数初始化
		try {
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return SUCCESS;
		}

		// 2、获取性别受众列表		
		List<BaseAudienceVo> infoData = generateGenderAudienceItemList();
		
		// 3、获取总记录数
		int count = infoData.size();
		
		// 4、排序
		Collections.sort(infoData, new CproAudienceBaseVoComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		AudienceSumDataVo sumData = generateBaseViewSum(infoData);
		
		// 6、计算总页码
		int totalPage = super.getTotalPage(count);
		
		// 7、获取分页数
		infoData = pagerList(infoData);
		
		// 8、构造JSON数据
		jsonObject.addData("list", infoData);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("cache", ReportConstants.Boolean.TRUE);
		jsonObject.addData("sum", sumData);
		
		return SUCCESS;
	}
	
	/**
	 * 处理有地域受众分布
	 * @return
	 */
	public String ajaxRegionAudienceList(){		

		// 1、参数初始化
		try {
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return SUCCESS;
		}

		// 2、获取地域受众列表		
		List<AudienceAssistantVo> infoData = generateRegionAudienceItemList();
		
		// 3、获取总记录数
		int count = infoData.size();
		
		// 4、排序
		Collections.sort(infoData, new CproAudienceAssistantVoComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		AudienceSumDataVo sumData = generateAssistantViewSum(infoData);

		// 6、生成显示的RegionViewItem列表
		this.filterSpecialRegion(infoData);//对港澳台地域的二级地域的“其他”进行删除处理
		List<BaseAudienceVo> list = this.pagerAssistantViewItemList(infoData);
		List<BaseAudienceVo> listall = this.allAssistantViewItemList(infoData);
		
		// 7、计算总页码
		int totalPage = super.getTotalPage(count);
		
		// 8、构造JSON数据
		jsonObject.addData("list", list);
		jsonObject.addData("listall", listall);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("cache", ReportConstants.Boolean.FALSE);
		jsonObject.addData("sum", sumData);

		return SUCCESS;
		
	}
		
	/**
	 * 处理兴趣分布受众
	 * @return
	 */
	public String ajaxInterestAudienceList(){		

		// 1、参数初始化
		try {
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return SUCCESS;
		}

		// 2、获取RegionAssistantUVVo列表		
		List<AudienceAssistantVo> infoData = generateInterestAudienceItemList();
		
		// 3、获取总记录数
		int count = infoData.size();
		
		// 4、排序
		Collections.sort(infoData, new CproAudienceAssistantVoComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		AudienceSumDataVo sumData = generateAssistantViewSum(infoData);

		// 6、生成显示的ViewItem列表
		List<BaseAudienceVo> list = this.pagerAssistantViewItemList(infoData);
		List<BaseAudienceVo> listall = this.allAssistantViewItemList(infoData);
		
		// 7、计算总页码
		int totalPage = super.getTotalPage(count);
		
		// 8、构造JSON数据
		jsonObject.addData("list", list);
		jsonObject.addData("listall", listall);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("cache", ReportConstants.Boolean.FALSE);
		jsonObject.addData("sum", sumData);

		return SUCCESS;
		
	}
	
	
	/********************************************* action业务方法***********************************************/
		
	/**
	 * 生成用于性别受众分布前端显示的VO
	 * @return 
	 */
	private List<BaseAudienceVo> generateGenderAudienceItemList(){
		
		//要返回的VoList
		List<BaseAudienceVo> genderVoList = new ArrayList<BaseAudienceVo>();
		
		//1、获取当前层级的受众分析性别相关数据
		List<Map<String,Object>> genderAudienceMap = audienceAnalyFacade.queryAudienceDTData(userId, planIds, groupIds, from, to);
		
		//2、遍历数据并构造volist
		if(!CollectionUtils.isEmpty(genderAudienceMap)){
			for(Map<String, Object> dataItem: genderAudienceMap){
				if(dataItem != null){
					BaseAudienceVo genderVo = new BaseAudienceVo();
					genderVo.setId(Integer.parseInt(dataItem.get(ReportConstants.GENDERID).toString()));
					if(genderVo.getId()==7){
						genderVo.setName(ReportWebConstants.DT_GENDER_MALE_STRING);
					}else if( genderVo.getId() == 8){
						genderVo.setName(ReportWebConstants.DT_GENDER_FEMALE_STRING);
					}else{
						genderVo.setName(ReportWebConstants.DT_GENDER_NOTKNOWN_STRING);
					}
					genderVo.setParentId(0);
					genderVo.fillAudienceRecord(dataItem);
					genderVoList.add(genderVo);
				}
			}
		}
		
		return genderVoList;
	}	
	
	/**
	 * 生成用于地域受众分布前端显示的VO
	 * @return 
	 */
	private List<AudienceAssistantVo> generateRegionAudienceItemList(){		

		//要返回的地域列表
		List<AudienceAssistantVo> regionAssistantList = new ArrayList<AudienceAssistantVo>();
		
		//1、获取当前层级的受众分析地域相关数据
		List<Map<String,Object>> interestAudienceMap = audienceAnalyFacade.queryAudienceRegData(userId, planIds, groupIds, from, to);
					
		//2、将地域放入map中，供填充数据使用
		Map<String,Map<String, Object>> interestDataMap = new HashMap<String,Map<String, Object>>();
		List<String> regionKeys = new ArrayList<String>();
		
		if(!CollectionUtils.isEmpty(interestAudienceMap)){ 
			for(Map<String, Object> data: interestAudienceMap){

				Integer provid = Integer.valueOf(data.get(ReportConstants.PROVID).toString());
				Integer cityid = Integer.valueOf(data.get(ReportConstants.CITYID).toString());
				
				//3、构造RegCache传输参数
				String key = provid + "_" + cityid;
				regionKeys.add(key);
	
				//4、存储数据
				if((provid != null) && (cityid != null)){
					//将doris每条记录存入map，供填充数据使用
					interestDataMap.put(key,data);
				}
			}
			
			//5、通过AdCache接口，获得具有父子关系的有展现的地域树
			this.generateRegionVo(regionKeys);
			
			//6、遍历CproItFacade返回的兴趣树列表，构造report显示VOList并过滤
			if(!regionMapping.isEmpty()){
				Map<String,Object> data;
				for (Integer firstRegionId: regionMapping.keySet()){
					
					//构造父地域
					AudienceAssistantVo regionAssistant = new AudienceAssistantVo(firstRegionId);
					AudienceAssistantVo firstRegion = regionMapping.get(firstRegionId);
					BaseAudienceVo viewItem = new BaseAudienceVo();		
					viewItem.setId(firstRegionId);
					String key = "";
					if(firstRegionId == 0){
						key = firstRegionId + "_" + 0;
						data = interestDataMap.get(key);
					}else{
						key = firstRegionId + "_" + ReportConstants.sumFlagInDoris;
						data = interestDataMap.get(key);
					}
					viewItem.fillAudienceRecord(data);
					
					viewItem.setName(firstRegion.getSelfVo().getName());
					viewItem.setId(firstRegion.getSelfVo().getId());
					viewItem.setParentId(0);
					viewItem.setOrderId(firstRegion.getSelfVo().getOrderId());
					
					regionAssistant.setSelfVo(viewItem);	
					regionAssistant.setChildOrient(orient);
						
					//构造子地域
					for(BaseAudienceVo childRegion: firstRegion.getChildVos()){
						BaseAudienceVo childViewItem = new BaseAudienceVo();
						key = firstRegionId + "_" + childRegion.getId();
						data = interestDataMap.get(key);
						childViewItem.fillAudienceRecord(data);
						childViewItem.setId(childRegion.getId());
						childViewItem.setName(childRegion.getName());
						childViewItem.setParentId(firstRegionId);
						childViewItem.setOrderId(childRegion.getOrderId());
						regionAssistant.addSecondVo(childViewItem);
						
					}
					
					regionAssistantList.add(regionAssistant);
				}
			}
		}	
		
		
		return regionAssistantList;
	}
	
	/**
	 * 生成用于兴趣受众分布前端显示的VO
	 * @return 
	 */
	private List<AudienceAssistantVo> generateInterestAudienceItemList(){		

		//要返回的地域列表
		List<AudienceAssistantVo> interestAssistantList = new ArrayList<AudienceAssistantVo>();
		
		//1、获取当前层级的受众分析地域相关数据
		List<Map<String,Object>> interestAudienceMap = audienceAnalyFacade.queryAudienceITData(userId, planIds, groupIds, from, to);
					
		//2、将地域放入map中，供填充数据使用
		Map<Integer,Map<String, Object>> itnerestDataMap = new HashMap<Integer,Map<String, Object>>();
		List<Integer> interestKeys = new ArrayList<Integer>();
		
		if(!CollectionUtils.isEmpty(interestAudienceMap)){ 
			for(Map<String, Object> data: interestAudienceMap){
				
				Integer iid = Integer.valueOf(data.get(ReportConstants.IID).toString());
				
				//3、构造InterestCache传输参数
				interestKeys.add(iid);
	
				//4、存储数据做mapping，以便后续填充数据
				if(iid != null){
					//将doris每条记录存入map，供填充数据使用
					itnerestDataMap.put(iid,data);
				}
			}
			
			//5、通过AdCache接口，获得具有父子关系的有展现的地域树
			this.generateInterestVo(interestKeys);
			
			//6、遍历CproItFacade返回的兴趣树列表，构造report显示VOList并过滤
			if(!interestMapping.isEmpty()){
				Map<String,Object> data;
				for (Integer firstInterestId: interestMapping.keySet()){
					
					//构造父兴趣
					AudienceAssistantVo interestAssistant = new AudienceAssistantVo(firstInterestId);
					AudienceAssistantVo firstInterest = interestMapping.get(firstInterestId);
					BaseAudienceVo viewItem = new BaseAudienceVo();		
					viewItem.setId(firstInterestId);
					data = itnerestDataMap.get(firstInterestId);
					viewItem.fillAudienceRecord(data);
					
					viewItem.setName(firstInterest.getSelfVo().getName());
					viewItem.setId(firstInterest.getSelfVo().getId());
					viewItem.setParentId(0);
					
					interestAssistant.setSelfVo(viewItem);	
					interestAssistant.setChildOrient(orient);
						
					//构造子兴趣
					for(BaseAudienceVo childInterest: firstInterest.getChildVos()){
						BaseAudienceVo childViewItem = new BaseAudienceVo();
						data = itnerestDataMap.get(childInterest.getId());
						childViewItem.fillAudienceRecord(data);
						childViewItem.setId(childInterest.getId());
						childViewItem.setName(childInterest.getName());
						childViewItem.setParentId(firstInterestId);
						interestAssistant.addSecondVo(childViewItem);
						
					}
					
					interestAssistantList.add(interestAssistant);
				}
			}
		}	
		
		
		return interestAssistantList;
	}
	
	/**
	 * 通过RegCache相关接口构造地域的父子关系树
	 * @param groupRegionMap
	 */
	private void generateRegionVo(List<String> regionKeys){
		
		//1、遍历regionIds的list
		for(String regionKey : regionKeys){
			String[] regionIds = regionKey.split("_");
			Integer firstRegionId = Integer.parseInt(regionIds[0]);
			Integer secondRegionId = Integer.parseInt(regionIds[1]);
			AudienceAssistantVo assistant = regionMapping.get(firstRegionId);
			
			if(firstRegionId != 0){
				
				//2、增加一级地域
				if(assistant == null){
					assistant = new AudienceAssistantVo(firstRegionId);
					BaseAudienceVo firstRegionItem = new BaseAudienceVo();
					firstRegionItem.setId(firstRegionId);
					String regionName = UnionSiteCache.regCache.getRegNameList().get(firstRegionId);
					firstRegionItem.setName(regionName);
					assistant.setSelfVo(firstRegionItem);
					regionMapping.put(firstRegionId, assistant);
				}
				
				//3、增加二级地域
				if( secondRegionId != ReportConstants.sumFlagInDoris){
					BaseAudienceVo secondRegionItem = new BaseAudienceVo();
					secondRegionItem.setId(secondRegionId);	
					if(secondRegionId != 0){
						String regionName = UnionSiteCache.regCache.getRegNameList().get(secondRegionId);
						secondRegionItem.setName(regionName);
					}else{
						secondRegionItem.setName(ReportWebConstants.REGION_NOTKNOWN);
					}
					assistant.addSecondVo(secondRegionItem);
				}
			}else{
				
				//2、增加一级地域
				if(assistant == null){
					assistant = new AudienceAssistantVo(firstRegionId);
					BaseAudienceVo firstRegionItem = new BaseAudienceVo();
					firstRegionItem.setId(firstRegionId);
					firstRegionItem.setName(ReportWebConstants.REGION_OTHER);
					assistant.setSelfVo(firstRegionItem);
					regionMapping.put(firstRegionId, assistant);
				}
			}
		}
	}
	
	/**
	 * 通过InterestCache相关接口构造兴趣的父子关系树
	 * @param groupRegionMap
	 */
	private void generateInterestVo(List<Integer> interestKeys){
		
		Map<Integer,InterestCacheObject> interestMap = interestMgr.getInterestCacheMap();
		Map<Integer, TrashObject> trashInterestMap = interestMgr.getTrashMap();
		
		List<Integer> trashInterestIds = new ArrayList<Integer>();
			
		//遍历interestKeys的list
		for(Integer iid : interestKeys){
			
			if(iid != 0){
				InterestCacheObject interestCacheObject = interestMap.get(iid);
				if(interestCacheObject == null){//如果兴趣点被删除了
					trashInterestIds.add(iid);
				}else{
					Integer firstInterestId = interestCacheObject.getParentId();
					if(firstInterestId == 0 ){//一级兴趣
						AudienceAssistantVo interestAssistantVo = interestMapping.get(iid);
						if(interestAssistantVo == null){
							interestAssistantVo = new AudienceAssistantVo(iid);
							BaseAudienceVo firstInterestItem = new BaseAudienceVo();
							firstInterestItem.setId(iid);
							firstInterestItem.setName(interestCacheObject.getName());
							firstInterestItem.setOrderId(interestCacheObject.getOrderId());
							interestAssistantVo.setSelfVo(firstInterestItem);
							interestMapping.put(iid, interestAssistantVo);
						}
					}else{//二级兴趣
						AudienceAssistantVo interestAssistantVo = interestMapping.get(firstInterestId);
						if(interestAssistantVo == null){
							interestAssistantVo = new AudienceAssistantVo(firstInterestId);
							InterestCacheObject firstInterest = interestMap.get(firstInterestId);//肯定是存在的
							BaseAudienceVo firstInterestItem = new BaseAudienceVo();
							firstInterestItem.setId(firstInterestId);
							firstInterestItem.setName(firstInterest.getName());
							firstInterestItem.setOrderId(interestCacheObject.getOrderId());
							interestAssistantVo.setSelfVo(firstInterestItem);
							interestMapping.put(firstInterestId, interestAssistantVo);
						}
						
						BaseAudienceVo secondInterestItem = new BaseAudienceVo();
						secondInterestItem.setId(iid);
						secondInterestItem.setName(interestCacheObject.getName());
						secondInterestItem.setOrderId(interestCacheObject.getOrderId());
						interestAssistantVo.addSecondVo(secondInterestItem);
					}
				}
			}else{				
				//增加"其他"兴趣
				AudienceAssistantVo interestAssistantVo = interestMapping.get(iid);
				if(interestAssistantVo == null){
					interestAssistantVo = new AudienceAssistantVo(iid);
					BaseAudienceVo firstRegionItem = new BaseAudienceVo();
					firstRegionItem.setId(iid);
					firstRegionItem.setName(ReportWebConstants.INTEREST_OTHER);
					firstRegionItem.setOrderId(Integer.MAX_VALUE);
					interestAssistantVo.setSelfVo(firstRegionItem);
					interestMapping.put(iid, interestAssistantVo);
				}
			}
		}
		
		
		
		//处理已删除兴趣
		for(Integer iid : trashInterestIds){
			TrashObject interestTrashObject = trashInterestMap.get(iid);
			if(interestTrashObject != null){
				AudienceAssistantVo interestTrashAssistantVo = interestMapping.get(iid);
				if(interestTrashAssistantVo == null){
					interestTrashAssistantVo = new AudienceAssistantVo(iid);
					BaseAudienceVo trashInterestVo = new BaseAudienceVo();
					trashInterestVo.setName(interestTrashObject.getName());
					trashInterestVo.setId(iid);
					interestTrashAssistantVo.setSelfVo(trashInterestVo);
					interestMapping.put(iid, interestTrashAssistantVo);
				}
			}
		}
		
	}
	
	/**
	 * 对港澳台的地域进行二级地域的过滤处理
	 * @param regionData
	 */
	private void filterSpecialRegion(List<AudienceAssistantVo> regionData){
		for(AudienceAssistantVo vo : regionData){
			//如果是港澳台，则不需要二级地域数据，去掉“其他”
			if(vo.getSelfVo().getId() == ReportWebConstants.REGION_ID_MACAU
					|| 
			   vo.getSelfVo().getId() == ReportWebConstants.REGION_ID_HONGKONG
				    || 
			   vo.getSelfVo().getId() == ReportWebConstants.REGION_ID_TAIWAN){
				vo.setChildVos(null);
			}
		}
	}
	
	/**
	 * 生成AssistantVO的汇总数据
	 * @param infoData
	 * @return
	 */
	private AudienceSumDataVo generateAssistantViewSum(List<AudienceAssistantVo> infoData){
		
		AudienceSumDataVo sumData = new AudienceSumDataVo();
		
		//1、生成汇总数据
		for (AudienceAssistantVo item : infoData) {
			sumData.setSrchuv(sumData.getSrchuv() + item.getSelfVo().getSrchuv());
			sumData.setClkuv(sumData.getClkuv() + item.getSelfVo().getClkuv());
			sumData.setTransuv(sumData.getTransuv() + item.getSelfVo().getTransuv());
		}
		
		//2、设置列表项数
		sumData.setCount(infoData.size());
		return sumData;
	}
	
	/**
	 * 生成BaseAudienceVo的汇总数据
	 * @param infoData
	 * @return
	 */
	private AudienceSumDataVo generateBaseViewSum(List<BaseAudienceVo> infoData){
		
		AudienceSumDataVo sumData = new AudienceSumDataVo();
		
		//1、生成汇总数据
		for (BaseAudienceVo item : infoData) {
			sumData.setSrchuv(sumData.getSrchuv() + item.getSrchuv());
			sumData.setClkuv(sumData.getClkuv() + item.getClkuv());
			sumData.setTransuv(sumData.getTransuv() + item.getTransuv());
		}
		
		//2、设置列表项数
		sumData.setCount(infoData.size());
		return sumData;
	}

	
	
	/********************************getters and setters****************************/
	
	public AudienceAnalyFacade getAudienceAnalyFacade() {
		return audienceAnalyFacade;
	}

	public void setAudienceAnalyFacade(AudienceAnalyFacade audienceAnalyFacade) {
		this.audienceAnalyFacade = audienceAnalyFacade;
	}

	public InterestMgr getInterestMgr() {
		return interestMgr;
	}

	public void setInterestMgr(InterestMgr interestMgr) {
		this.interestMgr = interestMgr;
	}

	public int getOrient() {
		return orient;
	}

	public void setOrient(int orient) {
		this.orient = orient;
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

	public String getBeidouBasePath() {
		return beidouBasePath;
	}

	public void setBeidouBasePath(String beidouBasePath) {
		this.beidouBasePath = beidouBasePath;
	}
	
}

