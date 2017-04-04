package com.baidu.beidou.report.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;

import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.dao.vo.FrontBackendStateMapping;
import com.baidu.beidou.report.util.ReportUtil;
import com.baidu.unbiz.common.CollectionUtil;

/**
 * <p>ClassName:QueryParameter
 * <p>Function: 封装前端的查询参数
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-2-26
 * @since    星计划(beidou 2.0)
 * @version  $Id: Exp $
 */
public class QueryParameter implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5735592808207817466L;
	

	//**************************定制报告-查询参数封装-begin
	/** 定制报告-用户自定义查询的推广计划ids */
	protected List<Integer> planIds;
	
	/** 定制报告-用户自定义查询的推广组ids */
	protected List<Integer> groupIds;
	
	/**供下载使用planIds字符串*/
	protected String planIdsStr;
	
	/**供下载使用groupIds字符串*/
	protected String groupIdsStr;
	
	/** 
	 * 定制报告是否分展现查询
	 * 0：不提供分网站数据；1：分主域；2：分二级域*/
	protected Integer isBySite;
	
	/** 定制报告生成的频率 */
	protected Integer timeUnit;
	//**************************定制报告-查询参数封装-end
	
	//**************************以下为公用字段
	/** 查询发起的时所处层级 */
	protected String level = QueryParameterConstant.LEVEL.LEVEL_ACCOUNT;
	/** 所处TAB */
	protected int tab = QueryParameterConstant.TAB.TAB_PLAN;
	/** 开始时间 */
	protected String dateStart;
	/** 结束时间 */
	protected String dateEnd;
	/** 
	 * 日期描述，与dateStart,dateEnd互斥，如“昨天”
	 * @see BeidouCoreConstant#PAGE_DATE_RANGE*
	 */
	protected String dateScale;
	/** 
	 * <<ul>搜索关键词：截去首尾空串后，按照模糊查询，不同级别匹配内容不同：
	 *   <li>Plan：匹配推广计划名称</li>
	 *   <li>Group：匹配推广组名称</li>
	 *   <li>Unit：匹配标题、描述、显示链接、点击链接</li>
	 *   <li>投放网络：<ul>
	 *   	  <li>有展现网站列表“和”自选网站“：匹配siteUrl</li>
	 *   	  <li>自选行业：匹配行业名称</li>
	 *   	</ul>
	 *   </li>
	 *   <li>Qt：搜索關鍵詞</li>
	 * </ul>
	 */
	protected String keyword;
	
	/**
	 * 计划属性：0-所有功能；1-仅无线
	 */
	protected Integer promotionType;
	
	public Integer getPromotionType() {
		return promotionType;
	}

	public void setPromotionType(Integer promotionType) {
		this.promotionType = promotionType;
	}

	/** 显示状态枚举，对应不同层级则表示的意义不同，
	 * 其实这里和用于显示的viewState不是同一个意思，叫searchState更合适，此处表示如计划状态列表中的“所有未删除”等，
	 * 而一般VO中的viewState则表示用于前端显示的显示状态，如plan的viewState可以表示“计划已下线” */
	protected Integer viewState;
	/** 排序列 */
	protected String orderBy;
	/** 排序方式 */
	protected String order;
	/** 当前页码，当前如何page=0或者为空则表示查询所有的 */
	protected Integer page;
	/** 分页大小 */
	protected Integer pageSize;
	/** 展现 */
	protected Long srchs;
	/** 展现比较操作符 */
	protected Integer srchsOp;
	/** 点击 */
	protected Long clks;
	/** 点击比较操作符 */
	protected Integer clksOp;
	/** 点击率 */
	protected Double ctr;
	/** 点击率比较操作符 */
	protected Integer ctrOp;
	
	
	/**
	 * added by liuhao since beidou3.x
	 */
	/** 展现受众*/
	protected Long srchuv;
	/** 展现受众比较操作符 */
	protected Integer srchuvOp;
	/** 点击 受众*/
	protected Long clkuv;
	/** 点击受众比较操作符 */
	protected Integer clkuvOp;
	/** 受众点击率 */
	protected Double cusur;
	/** 受众点击率比较操作符 */
	protected Integer cusurOp;
	
	
	/** 查询AOT时使用的ID，<=0表示不需要查询AOT */
	protected Integer aotItemId ;
	
	/** 用户Id */
	protected Integer userId;
	
	/** 计划Id */
	protected Integer planId;
	
	/** 组Id */
	protected Integer groupId;
	
	/** 受众组合ID */
	protected Long gpId;
	
	/** 筛选项中的展现类型 */
	protected Integer[] displayType;
	
	/**
	 * 展示类型字符串(例如：0,1,2)
	 */
	protected String displayTypeStrs;
	
	/**
	 * 推广组受众组合方式字符串（例如1,2,3,4），与group_pack表中pack_type保持一致
	 */
	protected String packTypeStrs;
	/** 筛选中的受众组合方式 */
	protected Integer[] packType;
	
	/**
	 * 推广组定向方式字符串（例如0,1,2,3）
	 */
	protected String targetTypeStrs;
	/** 筛选中的定向方式 */
	protected Integer[] targetType;
	
	/** 创意Id */
	protected Long unitId;
	/** 物料类型 */
	protected Integer[] wuliaoType;
	
	/**
	 * 物料类型字符串(例如：0,1,2)
	 */
	protected String materTypeStrs;
	
	//***************以下为“投放网络”使用
	/** 一级行业Id */
	protected Integer fTradeId;
	/** 二级行业Id */
	protected Integer sTradeId;
	/** 是否需要根据行业筛选 */
	protected boolean needTradeFilter;
	
	/** 否需要汇总，用在Flash统计处 */
	protected Integer needSum;
	
	/** 传递的ids */
	protected List<Long> ids;
	
	/** 前端传递的日历快捷名称，给FLASH使用，如：”昨天“ */
	protected String dateText;
	
	/** 网站域名 */
	protected String site;
	
	protected FrontBackendStateMapping stateMapping ;
	
	/** 多功能bool属性，用于前端参数传递，当前给QT报告使用,@cpweb-258 */
	protected boolean boolValue;
	
	/** 关键词列表：1为常规设置词表，2为受众组合词表，给QT报告使用，@cpweb-492 */
	protected Integer ktListType;
	
	/** 创意尺寸过滤条件 格式为width*height字符串 用于创意列表页  add by dongying since 体验优化三期 **/
	protected String unitScale;
	
	/** 附加创意类型 详见：AttachInfoConstant **/
	protected Integer attachType;
	
	/**
	 * 附加创意状态
	 */
	protected Integer attachState;
	

	/**
	 * <p>hasNoneStatField4Filter:是否有按非统计字段进行过滤
	 * <p>非统计字段如viewState，统计字段如srch
	 * @return true表示过滤字段中含有非统计字段，false反之      
	*/
	public boolean hasNoneStatField4Filter () {
		return (stateMapping != null && stateMapping.needFilterByState())
					|| !org.apache.commons.lang.StringUtils.isEmpty(keyword)
					|| !ArrayUtils.isEmpty(displayType)
					|| wuliaoType != null
					|| !ArrayUtils.isEmpty(targetType)
					|| fTradeId != null 
					|| sTradeId != null
					|| !CollectionUtils.isEmpty(ids) ;
	}
	
	public boolean hasStatField4Filter() {
		
		return (srchs != null && srchsOp != null) 
				|| (clks != null && clksOp != null) 
				|| (ctr != null && ctrOp != null) 
				|| (srchuv != null && srchuvOp != null) 
				|| (clkuv != null && clkuvOp != null) 
				|| (cusur != null && cusurOp != null) ;
		
	}
	
	/**
	 * <p>noPageerInfo: 无分页信息：page<=0或者pageSize<=0
	 * 
	 * @return true表示查询条件中分页信息不全，false表示分页信息全，可能需要分页获取数据。      
	*/
	public boolean noPagerInfo () {
		return page == null || page <= 0 || pageSize == null || pageSize <= 0; 
	}
	
	
	//------------------------------setters & getters
	public int getTab() {
		return tab;
	}
	public void setTab(int tab) {
		this.tab = tab;
	}
	public String getDateStart() {
		return dateStart;
	}
	public void setDateStart(String dateStart) {
		this.dateStart = dateStart;
	}
	public String getDateEnd() {
		return dateEnd;
	}
	public void setDateEnd(String dateEnd) {
		this.dateEnd = dateEnd;
	}
	public String getDateScale() {
		return dateScale;
	}
	public void setDateScale(String dateScale) {
		this.dateScale = dateScale;
	}
	public Integer getViewState() {
		return viewState;
	}
	public void setViewState(Integer viewState) {
		this.viewState = viewState;
	}
	public Integer getPage() {
		return page;
	}
	public Integer getAotItemId() {
		return aotItemId;
	}
	public void setAotItemId(Integer aotItemId) {
		this.aotItemId = aotItemId;
	}
	public Integer getAttachState() {
		return attachState;
	}

	public void setAttachState(Integer attachState) {
		this.attachState = attachState;
	}
	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		this.userId = userId;
	}
	public Integer getPlanId() {
		return planId;
	}
	public void setPlanId(Integer planId) {
		this.planId = planId;
	}
	public Integer getGroupId() {
		return groupId;
	}
	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}
	public Integer[] getDisplayType() {
		return displayType;
	}
	public void setDisplayType(Integer[] displayType) {
		this.displayType = displayType;
	}
	public String getDisplayTypeStrs() {
		return displayTypeStrs;
	}

	public void setDisplayTypeStrs(String displayTypeStrs) {
		this.displayTypeStrs = displayTypeStrs;
		this.displayType = ReportUtil.transferFromStringToInteger(displayTypeStrs);
	}
	public Integer getAttachType() {
		return attachType;
	}

	public void setAttachType(Integer attachType) {
		this.attachType = attachType;
	}

	public boolean isBoolValue() {
		return boolValue;
	}

	public void setBoolValue(boolean boolValue) {
		this.boolValue = boolValue;
	}

	public Long getUnitId() {
		return unitId;
	}
	public void setUnitId(Long unitId) {
		this.unitId = unitId;
	}
	
	public Integer[] getWuliaoType() {
		return wuliaoType;
	}

	public void setWuliaoType(Integer[] wuliaoType) {
		this.wuliaoType = wuliaoType;
	}

	public String getMaterTypeStrs() {
		return materTypeStrs;
	}

	public void setMaterTypeStrs(String materTypeStrs) {
		this.materTypeStrs = materTypeStrs;
		this.wuliaoType = ReportUtil.transferFromStringToInteger(materTypeStrs);
	}

	public Integer getFTradeId() {
		return fTradeId;
	}
	public void setFTradeId(Integer tradeId) {
		fTradeId = tradeId;
	}
	public Integer getSTradeId() {
		return sTradeId;
	}
	public void setSTradeId(Integer tradeId) {
		sTradeId = tradeId;
	}

	public Integer getNeedSum() {
		return needSum;
	}

	public void setNeedSum(Integer needSum) {
		this.needSum = needSum;
	}

	public List<Long> getIds() {
		return ids;
	}

	public void setIds(List<Long> ids) {
		this.ids = ids;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public void setPage(Integer page) {
		this.page = page;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		if (keyword != null) {
			this.keyword = keyword.trim();
		}
	}

	public String getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}

	public Double getCtr() {
		return ctr;
	}
	/**
	 * setCtr:设置CTR，由于前端传递过来的值乘了100，因此需要处理一下(/100)
	 *
	 * @param ctr 前端传递过来的页面值      
	 * @since 
	*/
	public void setCtr(Double ctr) {
		if (ctr != null ) {
			if (ctr > 0)
				this.ctr = ctr / 100.0;//前端传递过来的值是乘了100的
			else 
				this.ctr = 0d;
		}
	}
	public Integer getCtrOp() {
		return ctrOp;
	}
	public void setCtrOp(Integer ctrOp) {
		this.ctrOp = ctrOp;
	}

	public Long getSrchs() {
		return srchs;
	}

	public void setSrchs(Long srchs) {
		this.srchs = srchs;
	}

	public Integer getSrchsOp() {
		return srchsOp;
	}

	public void setSrchsOp(Integer srchsOp) {
		this.srchsOp = srchsOp;
	}

	public Long getClks() {
		return clks;
	}

	public void setClks(Long clks) {
		this.clks = clks;
	}

	public Integer getClksOp() {
		return clksOp;
	}

	public void setClksOp(Integer clksOp) {
		this.clksOp = clksOp;
	}

	public String getDateText() {
		return dateText;
	}

	public FrontBackendStateMapping getStateMapping() {
		return stateMapping;
	}

	public void setStateMapping(FrontBackendStateMapping stateMapping) {
		this.stateMapping = stateMapping;
	}

	public void setDateText(String dateText) {
		this.dateText = dateText;
	}

	public String getSite() {
		return site;
	}

	public void setSite(String site) {
		this.site = site;
	}


	public void setNeedTradeFilter(boolean needTradeFilter) {
		this.needTradeFilter = needTradeFilter;
	}

	public boolean isNeedTradeFilter() {
		return needTradeFilter;
	}

	public String getTargetTypeStrs() {
		return targetTypeStrs;
	}

	public void setTargetTypeStrs(String targetTypeStrs) {
		this.targetTypeStrs = targetTypeStrs;
		this.targetType = ReportUtil.transferFromStringToInteger(targetTypeStrs);
	}

	public Integer[] getTargetType() {
		return targetType;
	}

	public void setTargetType(Integer[] targetType) {
		this.targetType = targetType;
	}


	/************added by liuhao since beidou3.x********************/
	
	public Long getSrchuv() {
		return srchuv;
	}

	public void setSrchuv(Long srchuv) {
		this.srchuv = srchuv;
	}

	public Integer getSrchuvOp() {
		return srchuvOp;
	}

	public void setSrchuvOp(Integer srchuvOp) {
		this.srchuvOp = srchuvOp;
	}

	public Long getClkuv() {
		return clkuv;
	}

	public void setClkuv(Long clkuv) {
		this.clkuv = clkuv;
	}

	public Integer getClkuvOp() {
		return clkuvOp;
	}

	public void setClkuvOp(Integer clkuvOp) {
		this.clkuvOp = clkuvOp;
	}

	public Double getCusur() {
		return cusur;
	}

	public void setCusur(Double cusur) {
		if (cusur != null ) {
			if (cusur > 0)
				this.cusur = cusur / 100.0;//前端传递过来的值是乘了100的
			else 
				this.cusur = 0d;
		}
	}

	public Integer getCusurOp() {
		return cusurOp;
	}

	public void setCusurOp(Integer cusurOp) {
		this.cusurOp = cusurOp;
	}

	public Integer getKtListType() {
		return ktListType;
	}

	public void setKtListType(Integer ktListType) {
		this.ktListType = ktListType;
	}

	public String getPackTypeStrs() {
		return packTypeStrs;
	}

	public void setPackTypeStrs(String packTypeStrs) {
		this.packTypeStrs = packTypeStrs;
		this.packType = ReportUtil.transferFromStringToInteger(packTypeStrs);
	}

	public Integer[] getPackType() {
		return packType;
	}

	public void setPackType(Integer[] packType) {
		this.packType = packType;
	}

	public Long getGpId() {
		return gpId;
	}

	public void setGpId(Long gpId) {
		this.gpId = gpId;
	}

	public List<Integer> getPlanIds() {
		return planIds;
	}

	public List<Integer> getGroupIds() {
		return groupIds;
	}

	public String getUnitScale() {
		return unitScale;
	}

	public void setUnitScale(String unitScale) {
		this.unitScale = unitScale;
	}

	public Integer getIsBySite() {
		return isBySite;
	}

	public Integer getTimeUnit() {
		return timeUnit;
	}

	public void setPlanIds(List<Integer> planIds) {
		this.planIds = planIds;
		if(CollectionUtil.isNotEmpty(planIds)) {
			StringBuilder builder = new StringBuilder();
			for(Integer planId : planIds) {
				builder.append(planId).append(",");
			}
			builder.setLength(builder.length()-1);
			this.planIdsStr = builder.toString();
		}
	}

	public void setGroupIds(List<Integer> groupIds) {
		this.groupIds = groupIds;
		if(CollectionUtil.isNotEmpty(groupIds)) {
			StringBuilder builder = new StringBuilder();
			for(Integer groupId : groupIds) {
				builder.append(groupId).append(",");
			}
			builder.setLength(builder.length()-1);
			this.groupIdsStr = builder.toString();
		}
	}

	public void setIsBySite(Integer isBySite) {
		this.isBySite = isBySite;
	}

	public void setTimeUnit(Integer timeUnit) {
		this.timeUnit = timeUnit;
	}

	public String getPlanIdsStr() {
		return planIdsStr;
	}

	public void setPlanIdsStr(String planIdsStr) {
		this.planIdsStr = planIdsStr;
	
		//同步填充planIds
		this.planIds = new ArrayList<Integer>();
		if(null != planIdsStr){
			String[] idStrs= planIdsStr.split(",");
			for(String idStr : idStrs){
				planIds.add(Integer.parseInt(idStr));
			}
		}
	}

	public String getGroupIdsStr() {
		return groupIdsStr;
	}

	public void setGroupIdsStr(String groupIdsStr) {
		this.groupIdsStr = groupIdsStr;
		
		//同步填充groupIds
		this.groupIds = new ArrayList<Integer>();
		if(null != groupIdsStr){
			String[] idStrs= groupIdsStr.split(",");
			for(String idStr : idStrs){
				groupIds.add(Integer.parseInt(idStr));
			}
		}
	}

}
