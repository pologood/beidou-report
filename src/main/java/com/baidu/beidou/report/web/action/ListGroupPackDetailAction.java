package com.baidu.beidou.report.web.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;

import com.baidu.beidou.cprogroup.bo.CproGroup;
import com.baidu.beidou.cprogroup.bo.GroupPack;
import com.baidu.beidou.cprogroup.constant.CproGroupConstant;
import com.baidu.beidou.cprogroup.constant.InterestConstant;
import com.baidu.beidou.cprogroup.facade.CproITFacade;
import com.baidu.beidou.cprogroup.facade.GroupPackFacade;
import com.baidu.beidou.cprogroup.service.CproGroupMgr;
import com.baidu.beidou.cprogroup.service.CproKeywordMgr;
import com.baidu.beidou.cprogroup.service.GroupPackMgr;
import com.baidu.beidou.cprogroup.util.KTKeywordUtil;
import com.baidu.beidou.cprogroup.util.TargettypeUtil;
import com.baidu.beidou.cprogroup.vo.GroupPackInterestKey;
import com.baidu.beidou.cprogroup.vo.InterestVo4Report;
import com.baidu.beidou.cprogroup.vo.PackITVo;
import com.baidu.beidou.cprogroup.vo.PackKeywordVo;
import com.baidu.beidou.cproplan.bo.CproPlan;
import com.baidu.beidou.cproplan.service.CproPlanMgr;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.olap.constant.Constants;
import com.baidu.beidou.olap.service.ItStatService;
import com.baidu.beidou.olap.service.KeywordStatService;
import com.baidu.unbiz.olap.util.ReportUtils;
import com.baidu.beidou.olap.vo.GroupKtViewItem;
import com.baidu.beidou.olap.vo.InterestViewItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.report.util.ReportUtil;
import com.baidu.beidou.report.vo.KeywordInfoView;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.comparator.GroupItComparator;
import com.baidu.beidou.report.vo.comparator.GroupKtComparator;
import com.baidu.beidou.report.vo.group.GroupKtReportSumData;
import com.baidu.beidou.report.vo.group.GroupKtViewItemSum;
import com.baidu.beidou.report.vo.group.GroupPackItReportVo;
import com.baidu.beidou.report.vo.group.GroupPackKtReportVo;
import com.baidu.beidou.report.vo.group.InterestAssistant;
import com.baidu.beidou.report.vo.group.InterestReportSumData;
import com.baidu.beidou.report.vo.group.InterestViewItemSum;
import com.baidu.beidou.report.vo.group.KeywordItemKey;
import com.baidu.beidou.stat.driver.bo.Constant;
import com.baidu.beidou.tool.util.TransReportHelper;
import com.baidu.beidou.tool.vo.TempSitesAndTrans;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.LogUtils;
import com.baidu.beidou.util.StringUtils;
import com.baidu.beidou.util.TokenUtil;
import com.baidu.beidou.util.atomdriver.AtomUtils;
import com.baidu.beidou.util.memcache.BeidouCacheInstance;
import com.baidu.beidou.util.vo.ItRpcQueryParam;
import com.baidu.beidou.util.vo.PackKeywordRpcQueryParam;

public class ListGroupPackDetailAction extends BeidouReportActionSupport {

	private static final long serialVersionUID = -8576562400171678203L;
	
	@Resource(name="keywordStatServiceImpl")
	private KeywordStatService ktStatMgr;
	
	@Resource(name="itStatServiceImpl")
	ItStatService itStatMgr;
	
	private CproPlanMgr cproPlanMgr = null;
	private CproGroupMgr cproGroupMgr = null;
	private CproKeywordMgr cproKeywordMgr = null;
	private GroupPackFacade groupPackFacade = null;
	private GroupPackMgr groupPackMgr = null;
	private CproITFacade cproITFacade = null;
	
	
	/** 受众组合详情页关键词子tab下载报表的列数 */
	public static final int DOWN_REPORT_PACK_KT_COL_NUM = 19;
	
	/** 受众组合详情页兴趣子tab下载报表的列数 */
	public static final int DOWN_REPORT_PACK_IT_COL_NUM = 17;
	
	/**
	 * Memcached的Token过期时间
	 */
	private int TOKEN_EXPIRE = 60;
	/**
	 * Memcached使用的Token
	 */
	private String token="";
	/**
	 * 排序方向（正排为1，倒排为-1）
	 */
	private int orient;
	/**
	 * 二级兴趣点排序方向（当按照兴趣列排序时候，二级兴趣点需要按照其ID正排或倒排；当按照非兴趣列排序时候，二级兴趣点只需按照ID正排）
	 */
	private int childOrient;
	/**
	 * 总记录数、行数（一级兴趣点+兴趣组合）
	 */
	private int count;
	private List<Integer> planIds = new ArrayList<Integer>();
	private List<Integer> groupIds = new ArrayList<Integer>();
	private List<Integer> wordIds = new ArrayList<Integer>();
	private List<Long> gpIds = new ArrayList<Long>();
	private Integer planId;
	private Integer groupId;
	private String beidouBasePath;
	
	private boolean hasKeyword;		//当前层级（未过滤时）是否含有关键词
	
	private String packName;
	
	/**
	 * 当前层级下是否有自选兴趣
	 */
	private boolean hasInterest = false;
	
//	private boolean isMixTodayAndBeforeFlag;	// 当前查询是否混合今天和过去日期
//	private boolean isOnlyTodayFlag;	// 当前查询是否只包含今天
	
	@Override
	protected void initStateMapping() {
		if (qp.getPlanId() != null) {
			planIds = new ArrayList<Integer>();
			planIds.add(qp.getPlanId());
		}
		if (qp.getGroupId() != null) {
			groupIds = new ArrayList<Integer>();
			groupIds.add(qp.getGroupId());
		}
		if (qp.getGpId() != null) {
			gpIds = new ArrayList<Long>();
			gpIds.add(qp.getGpId());
		}
		wordIds = new ArrayList<Integer>();

		orient = ReportConstants.SortOrder.ASC;
		childOrient = ReportConstants.SortOrder.ASC;
		if (QueryParameterConstant.SortOrder.SORTORDER_DESC.equalsIgnoreCase(qp
				.getOrder())) {
			orient = ReportConstants.SortOrder.DES;
			if (qp.getOrderBy().equals("interestName")) {
				childOrient = ReportConstants.SortOrder.DES;
			}
		}
		if (qp.getPage() == null || qp.getPage() < 0) {
			qp.setPage(0);
		}

		if (qp.getPageSize() == null || qp.getPageSize() < 1) {
			qp.setPageSize(ReportConstants.PAGE_SIZE);
		}
		
		// 受众组合详情页下，关键词均为受众词
		qp.setKtListType(ReportConstants.KtReportType.KT_REPORT_PACK);
		
		// 在initParameter之后执行条件判断
//		isMixTodayAndBeforeFlag = this.isMixTodayAndBefore(from, to);
//		isOnlyTodayFlag = this.isOnlyToday(from, to);
	}
	
	/**
	 * 判断此次请求是否有过滤条件
	 */
	protected boolean hasFilter() {
		return qp.isBoolValue() || !StringUtils.isEmpty(qp.getKeyword()) || qp.hasStatField4Filter();
	}
	
	/**
	 * deleteAllPackKt: 删除（当前查询条件下的）所有关键词
	 * @version beidou 3 plus
	 * @author genglei01
	 * @date 2012-9-11
	 */
	public String deleteAllPackKt() {
		// 参数初始化
		try {
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return ERROR;
		}
		
		boolean hasFilter = hasFilter();
		PackKeywordRpcQueryParam param = new PackKeywordRpcQueryParam();
		if (hasFilter) {//有过滤
			List<GroupKtViewItem> infoData;	// 目标结果集
			Map<Long, List<Long>> toDelPackWordIds = new HashMap<Long, List<Long>>();	// 待删除词，key为gpId，value为wordIds
			
			// 说明：此处为了方便，直接用利用之前的方法，将过滤后的ids用于删除，
			// 其实还可以优化的，如：任何情况下不用去查atom，不用查planName,groupName等
			// 获取VO列表
			infoData = generatePackKtList(false);
			for (GroupKtViewItem item : infoData) {
				if (!item.isHasDel()) {
					long gpId = item.getGpId();
					long wordId = item.getWordId();
					
					List<Long> wordIds = toDelPackWordIds.get(gpId);
					if (CollectionUtils.isEmpty(wordIds)) {
						wordIds = new ArrayList<Long>();
						toDelPackWordIds.put(gpId, wordIds);
					}
					wordIds.add(wordId);
				}
			}
			if (CollectionUtils.isEmpty(toDelPackWordIds.keySet())) {
				return ERROR;
			}
			param.setPackWordIds(toDelPackWordIds);
			param.setUserId(qp.getUserId());
						
		} else {
			// 无过滤，按groupid,planid,userid进行删除
			param.setUserId(qp.getUserId());
			param.setPlanId(qp.getPlanId());
			param.setGroupId(qp.getGroupId());
			param.setGpId(qp.getGpId());
		}
		token = TokenUtil.generateToken();
		BeidouCacheInstance.getInstance().memcacheRandomSet(token, param, TOKEN_EXPIRE);

		return SUCCESS;
	}
	
	/**
	 * modPackKtPatternTypeAll: （当前查询条件下的）修改所有关键词的匹配模式
	 * @version beidou 3 plus
	 * @author genglei01
	 * @date 2012-9-11
	 */
	public String modPackKtPatternTypeAll() {
		// 参数初始化
		try {
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return ERROR;
		}
		
		boolean hasFilter = hasFilter();
		PackKeywordRpcQueryParam param = new PackKeywordRpcQueryParam();
		if (hasFilter) {//有过滤
			List<GroupKtViewItem> infoData;	// 目标结果集
			Map<Long, List<Long>> toUpdatePackWordIds = new HashMap<Long, List<Long>>();	// 待删除词，key为gpId，value为wordIds
			
			// 说明：此处为了方便，直接用利用之前的方法，将过滤后的ids用于删除，
			// 其实还可以优化的，如：任何情况下不用去查atom，不用查planName,groupName等
			// 获取VO列表
			infoData = generatePackKtList(false);
			for (GroupKtViewItem item : infoData) {
				if (!item.isHasDel()) {
					long gpId = item.getGpId();
					long wordId = item.getWordId();
					
					List<Long> wordIds = toUpdatePackWordIds.get(gpId);
					if (CollectionUtils.isEmpty(wordIds)) {
						wordIds = new ArrayList<Long>();
						toUpdatePackWordIds.put(gpId, wordIds);
					}
					wordIds.add(wordId);
				}
			}
			if (CollectionUtils.isEmpty(toUpdatePackWordIds.keySet())) {
				return ERROR;
			}
			param.setPackWordIds(toUpdatePackWordIds);
			param.setUserId(qp.getUserId());
						
		} else {
			// 无过滤，按groupid,planid,userid进行删除
			param.setUserId(qp.getUserId());
			param.setPlanId(qp.getPlanId());
			param.setGroupId(qp.getGroupId());
			param.setGpId(qp.getGpId());
		}
		token = TokenUtil.generateToken();
		BeidouCacheInstance.getInstance().memcacheRandomSet(token, param, TOKEN_EXPIRE);

		return SUCCESS;
	}
	
	/**
	 * ajaxPackKtList: 受众组合详情页关键词子Tab
	 * @version beidou 3 plus
	 * @author genglei01
	 * @date 2012-9-11
	 */
	public String ajaxPackKtList() {
		// new ReportWebConstants().setFRONT_SORT_THRESHOLD(40);

		// 1、参数初始化
		try {
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return SUCCESS;
		}

		List<GroupKtViewItem> infoData;// 目标结果集（可能是全集或者分页集）

		// 2、获取VO列表
		infoData = generatePackKtList(false);
		
		// 3、查询总条数，该数字可能不是最终值，因为有可能需要按统计值过滤，这样的话该值会被再次修改
		count = infoData.size();
		
		// 4、排序
		Collections.sort(infoData, new GroupKtComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		GroupKtViewItemSum sumData = calculatePackKtSumData(infoData);

		// 6、生成缓存条件：总条件小于1W。
		boolean cache = shouldCache(count);
		// 不缓存的话就只拿一页数据
		if (!cache) {
			infoData = ReportWebConstants.subListinPage(infoData, qp.getPage(), qp.getPageSize());
		}

		// 7、计算总页码
		int totalPage = super.getTotalPage(count);

		jsonObject.addData("list", infoData);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("hasKeyword", hasKeyword);
		jsonObject.addData("cache", cache ? ReportConstants.Boolean.TRUE
				: ReportConstants.Boolean.FALSE);
		jsonObject.addData("sum", sumData);
		
//		if (isOnlyTodayFlag) {
//			this.clearNotRealtimeStat(infoData, sumData);
//		} else {
			this.reportFacade.postHandleTransAndUvData(userId, from, to, infoData, sumData);
//		}

		return SUCCESS;
	}
	
	/**
	 * <p>
	 * generateKtListForPack: 生成前端VO，主要经历以下几步
	 * </p>
		0、参数处理，主要是设置时间参数和userId到queryparameter对象中
		1、通过keywordStatService查询指定时间段内用户所有的Qt词统计信息；
		2、获取该层级下所有的关键词；
		3、通过keywordid把#1和#2内容进行Merge：如果bool（只查询已添加）=true则用#1做基内容，#2中多余数据丢弃；如bool=false，
		则#1和#2 merge之后得到A，#2中未merge部分通过wordid反查Atom，结果集与#2再进行merge得到B，最后再将A、B两部分结果集进行Merge；
		4、按照统计字段、关键词进行过滤和排序；
		5、生成汇总的统计数据；
		6、如果需要分页的话获取分页数据；
		7、生成分页汇总数据
		8、返回jsonObject

	 * 
	 * @param forFlashXml 是否是给flash汇总使用
	 * @return 满足前端查询条件的对象
	 */
	private List<GroupKtViewItem> generatePackKtList(boolean forFlashXml) {
		// 校验userId信息
		Integer userId = qp.getUserId();
		if (userId == null || userId <= 0) {
			return new ArrayList<GroupKtViewItem>();
		}
			
		// 1、查询基本统计数据和UV数据，但不排序
		List<GroupKtViewItem> allData = this.getPackKtList();
		
		
		// 2、获取该受众组合的所有关键词列表
		List<KeywordInfoView> keywords = new ArrayList<KeywordInfoView>();
		List<PackKeywordVo> packKeywords = groupPackFacade.findKeywordByGpId(qp.getGpId());
		
		Map<KeywordItemKey, KeywordInfoView> map = new HashMap<KeywordItemKey, KeywordInfoView>();
		// 在关键词Tab下受众词表中，一个高级组合会有多个相同词（存在于不同基础关键词包中）
		// 所以，需要保留一个词，保留的逻辑是，同一高级组合下，如果多个基础次包的相同词：
		// 1. 均不支持QT，则匹配模式列显示“-”；
		// 2. 存在支持QT，保留支持高级匹配的，显示“高级”；如果没有支持高级匹配的，则显示“标准”。
		for (PackKeywordVo keyword : packKeywords) {
			KeywordItemKey newKey = new KeywordItemKey(keyword.getGroupId(), keyword.getGpId(), keyword.getWordId());
			KeywordInfoView newValue = new KeywordInfoView(keyword);
			
			KeywordInfoView preValue = map.get(newKey);
			if (preValue == null) {
				map.put(newKey, newValue);
			} else {
				if (!TargettypeUtil.hasQT(newValue.getTargetType())) {
					continue;
				}
				if (!TargettypeUtil.hasQT(preValue.getTargetType())
						&& TargettypeUtil.hasQT(newValue.getTargetType())) {
					map.put(newKey, newValue);
					continue;
				}
			}
		}
		
		keywords.addAll(map.values());
		hasKeyword = CollectionUtils.isNotEmpty(keywords);
			
		if(forFlashXml && CollectionUtils.isEmpty(allData)) {
			// 如果是统计flash使用的，且没有统计数据，则直接返回空
			return new ArrayList<GroupKtViewItem>();
		}
		
		// 3、生成Map有利于数据合并
		Map<KeywordItemKey, KeywordInfoView> mapView = new HashMap<KeywordItemKey, KeywordInfoView>(keywords.size());//用户所选的关键词
		Set<Integer> planIds = new HashSet<Integer>();
		Set<Integer> groupIds = new HashSet<Integer>();
		for (KeywordInfoView keyword : keywords) {
			KeywordItemKey key = new KeywordItemKey(keyword.getGroupId(), keyword.getGpId(), keyword.getWordId());
			mapView.put(key, keyword);
			planIds.add(keyword.getPlanId());
			groupIds.add(keyword.getGroupId());
		}
		for (GroupKtViewItem stat : allData ) {
			planIds.add(stat.getPlanId());
			groupIds.add(stat.getGroupId());
		}

		// 根据groupIds和planIds查找对应的推广组和推广计划列表
		Map<Integer, String> planIdNameMapping = cproPlanMgr.findPlanNameByPlanIds(planIds);
		Map<Integer, String> groupIdNameMapping = cproGroupMgr.findGroupNameByGroupIds(groupIds);
		
		// 获取当前层级推广组列表（包括删除的），放在Map中，供后续使用
		List<CproGroup> groupList = cproGroupMgr.findCproGroupByGroupIds(new ArrayList<Integer>(groupIds));
		Map<Integer, CproGroup> groupMap = new HashMap<Integer, CproGroup>();
		if (!CollectionUtils.isEmpty(groupList)) {
			for (CproGroup group : groupList) {
				groupMap.put(group.getGroupId(), group);
			}
		}
		
		// 4、组装数据并且过滤，先以mysql表为基准获得已删除关键词，若没被删除，则构造VO对象
		//   通过ATOM反差已经删除的关键词，并构造VO对象
		//   添加没有doris统计数据的关键词，并构造VO对象
		
		// 根据userId获取其1，2星级的词
		Map<Integer, Integer> blackList = cproKeywordMgr.getKTBlackListByUserId(qp.getUserId());
		List<GroupKtViewItem> infoData = new ArrayList<GroupKtViewItem>();
		if (!CollectionUtils.isEmpty(allData)) {
			// 如果有统计数据则进行后续的数据合并操作；
			Long statWordId;
			Integer statPlanId;
			Integer statGroupId;
			Long gpId;
			Integer refPackId;
			KeywordInfoView keywordInfo;
			
			// 存放已删除但有统计信息的关键词统计信息，key为wordId，value为统计信息
			Map<Long, GroupKtViewItem> deletedKeywordMap = new HashMap<Long, GroupKtViewItem>();
			Set<Integer> deletedWordIds = new HashSet<Integer>();
			Set<Integer> deletedRefPackIds = new HashSet<Integer>();
			
			// 遍历统计数据，如果相应关键词数据库中不存在则放入删除Map，待后续处理
			for (GroupKtViewItem stat : allData) {
				try {
					statWordId = stat.getWordId();
					statGroupId = stat.getGroupId();
					gpId = stat.getGpId();
					refPackId = stat.getRefPackId();
					statPlanId = stat.getPlanId();
				} catch (Exception e) {
					log.error("error to digest doris data[" + stat + "]",e);
					continue;
				}
				
				KeywordItemKey key = new KeywordItemKey(statGroupId, gpId, statWordId);
				keywordInfo = mapView.remove(key);
				if (keywordInfo == null) {
					// 如果关键词已被刪除，则记录之，以备后续查atom；
					deletedKeywordMap.put(statWordId, stat);
					deletedWordIds.add(statWordId.intValue());
					if (refPackId != 0) {
						deletedRefPackIds.add(refPackId);
					}
				} else {
					// 没有删除则生成待显示的VO对象
					GroupKtViewItem view = new GroupKtViewItem();
					view.fillStatRecord(stat);
					view.setPlanId(statPlanId);
					view.setPlanName(planIdNameMapping.get(statPlanId));
					view.setGroupId(statGroupId);
					view.setGroupName(groupIdNameMapping.get(statGroupId));
					view.setWordId(new Long(statWordId));
					view.setKeyword(keywordInfo.getKeyword());
					
					// 设置受众信息
					view.setGpId(gpId);
										
					if (!isPackKtFiltered(view)) {
						// 增加关键词质量度一列
						appendKtWordQuality(blackList, view);
						infoData.add(view);
					}
				}
			}
			
			// 处理已删除词的统计数据
			if(!qp.isBoolValue() && !deletedKeywordMap.isEmpty()) {

				// 需要查询Atom
				Map<Integer, String> atomIdKeywordMapping = AtomUtils.getWordById(deletedWordIds);
				// 查询受众组合名称
				List<Integer> refPackIdList = new ArrayList<Integer>(deletedRefPackIds);
				
				Map<Integer, String> packNameMapping = new HashMap<Integer, String>();
				Map<Integer, Map<String, Object>> packInfoMap = groupPackFacade.getPackNameByRefPackId(userId, refPackIdList);
				for (Integer id : packInfoMap.keySet()) {
					Map<String, Object> packInfo = packInfoMap.get(id);
					if (packInfo != null) {
						packNameMapping.put(id, (String)packInfo.get("name"));
					}
				}
				
				for (GroupKtViewItem stat : deletedKeywordMap.values()) {
					try {
						statWordId = stat.getWordId();
						statGroupId = stat.getGroupId();
						gpId = stat.getGpId();
						refPackId = stat.getRefPackId();
						statPlanId = stat.getPlanId();
					} catch (Exception e) {
						log.error("error to digest doris data[" + stat + "]",e);
						continue;
					}
					GroupKtViewItem view = new GroupKtViewItem();
					view.fillStatRecord(stat);
					view.setPlanId(statPlanId);
					view.setPlanName(planIdNameMapping.get(statPlanId));
					view.setGroupId(statGroupId);
					view.setGroupName(groupIdNameMapping.get(statGroupId));
					view.setWordId(new Long(statWordId));
					view.setKeyword(atomIdKeywordMapping.get(statWordId.intValue()));
					view.setHasDel(true);	// 标识为已经删除,已经删除的关键词对应的“匹配模式”显示为“-”
					
					// 设置受众信息
					view.setGpId(gpId);
					
					if(!isPackKtFiltered(view)) {
						// 增加关键词质量度一列
						appendKtWordQuality(blackList, view);
						infoData.add(view);
					}
				}
			}
			
			// 添加剩下的没有统计数据的QT词
			for (KeywordInfoView keywordInfoView : mapView.values()) {
				GroupKtViewItem view = new GroupKtViewItem();
				view.setPlanId(keywordInfoView.getPlanId());
				view.setPlanName(planIdNameMapping.get(keywordInfoView.getPlanId()));
				view.setGroupId(keywordInfoView.getGroupId());
				view.setGroupName(groupIdNameMapping.get(keywordInfoView.getGroupId()));
				view.setWordId(keywordInfoView.getWordId());
				view.setKeyword(keywordInfoView.getKeyword());
				
				// 设置受众信息
				view.setGpId(keywordInfoView.getGpId());
				
				if (!isPackKtFiltered(view)) {
					// 增加关键词质量度一列
					appendKtWordQuality(blackList, view);
					infoData.add(view);
				}
			}
		} else {
			for (KeywordInfoView keywordInfoView : keywords) {
				GroupKtViewItem view = new GroupKtViewItem();
				view.setPlanId(keywordInfoView.getPlanId());
				view.setPlanName(planIdNameMapping.get(keywordInfoView.getPlanId()));
				view.setGroupId(keywordInfoView.getGroupId());
				view.setGroupName(groupIdNameMapping.get(keywordInfoView.getGroupId()));
				view.setWordId(keywordInfoView.getWordId());
				view.setKeyword(keywordInfoView.getKeyword());
				
				// 设置受众信息
				view.setGpId(keywordInfoView.getGpId());
				
				if (!isPackKtFiltered(view)) {
					// 增加关键词质量度一列
					appendKtWordQuality(blackList, view);
					infoData.add(view);
				}
			}
		}
		
		return infoData;
	}
	
	
	private List<GroupKtViewItem> getPackKtList() {
		List<Map<String, Object>> allData;
		List<GroupKtViewItem> olapList = ktStatMgr.queryGroupKeywordData(qp.getUserId(), planIds, groupIds,
				gpIds, null, wordIds, from, to, null, 0, ReportConstants.TU_NONE, qp.getKtListType());
		
		List<Map<String, Object>> uvData = uvDataService.queryKeywordData(qp.getUserId(), planIds, 
				groupIds, gpIds, null, null, null, from, to, null, 0, ReportConstants.TU_NONE, 
				Constant.REPORT_TYPE_DEFAULT, 0, 0, qp.getKtListType());
		
		// 2、合并转化、Holmes等其他数据
		// 排序参数
		List<String> idKeys = new ArrayList<String>();
		idKeys.add(ReportConstants.GPID);
		idKeys.add(ReportConstants.WORD);
		
		// 如果需要获取转化数据，则获取转化数据并合并
		boolean needToFetchTransData = this.transReportFacade.needToFetchTransData(userId, from, to, false);
		if (needToFetchTransData) {
			
			TempSitesAndTrans tempSiteAndTrans = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userId);
			
			// 获取转化数据
			List<Map<String, Object>> transData = transDataService.queryKeywordData(userId, 
					tempSiteAndTrans.getTransSiteIds(), tempSiteAndTrans.getTransTargetIds(),
					planIds, groupIds, gpIds, null, null, null, from, to, null, 0,
					ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0, qp.getKtListType());
			// 获取holmes数据
			List<Map<String, Object>> holmesData = holmesDataService.queryKeywordData(userId, null, null, 
					planIds, groupIds, gpIds, null, null, null, from, to, null, 0,
					ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0, qp.getKtListType());

			// Merge基本统计/UV数据与转化数据/holmes数据
			allData = this.reportFacade.mergeTransHolmesAndUvDataByMulitKey(Constant.DorisDataType.UV, 
					transData, holmesData, uvData, idKeys);
		} else {
			// Merge基本统计/UV数据
			allData = uvData;
		}
		
		List<GroupKtViewItem> dorisList = new ArrayList<GroupKtViewItem>();
		List<GroupKtViewItem> resultList = new ArrayList<GroupKtViewItem>();
		//7、填充数据
		if (! CollectionUtils.isEmpty(allData)) {
			for (Map<String, Object> row : allData) {
				GroupKtViewItem item = new GroupKtViewItem();
				if (row != null) {
					item.setPlanId(Integer.valueOf(row.get(ReportConstants.PLAN).toString()));
					item.setGroupId(Integer.valueOf(row.get(ReportConstants.GROUP).toString()));
					item.setWordId(Long.valueOf(row.get(ReportConstants.WORD).toString()));
					item.setGpId(Long.valueOf(row.get(ReportConstants.GPID).toString()));
					item.setRefPackId(Integer.valueOf(row.get(ReportConstants.REFPACKID).toString()));
					item.fillStatRecord(row);
					dorisList.add(item);
				}
			}
		}
		
		Set<String> mergeKeys = new HashSet<String>(Arrays.asList(
				new String[]{Constants.COLUMN.GPID, Constants.COLUMN.WORDID}));
		
		resultList = ReportUtils.mergeItemList(dorisList, olapList, mergeKeys, 
				Constants.statMergeVals, GroupKtViewItem.class, true);
		
		ReportUtil.generateExtentionFields(resultList);
		
		return resultList;
	}
	
	/**
	 * appendKtWordQuality：填充关键词质量度
	 * 
	 * @param blackList 黑名单
	 * @param view 待填充的VO对象
	 */
	private void appendKtWordQuality(Map<Integer, Integer> blackList, GroupKtViewItem view){
		if(view != null && blackList != null){
			Integer q = blackList.get(view.getWordId().intValue());//注意值类型（Long，Integer）
			if((q != null)&&( q == CproGroupConstant.KT_WORD_QUALITY_DEGREE_1)){//2星和3星合并。此处升级,update by liuhao05
				view.setQualityDg(q);
			}else if(StringUtils.hitKTBlackRules(view.getKeyword())){
				view.setQualityDg(CproGroupConstant.KT_WORD_QUALITY_DEGREE_1);
			}
		}
	}
	
	/**
	 * isPackKtFiltered: 判断是否将当前记录过滤
	 *
	 * @param item 待判断VO对象
	 * @return  true表示要过滤，false表示保留    
	 */
	protected boolean isPackKtFiltered(GroupKtViewItem item) {
		//1、按查询关键词过滤
		String query = qp.getKeyword();
		if(!StringUtils.isEmpty(query)) {
			// add by kanghongwei since cpweb429(qtIM)
			query = KTKeywordUtil.validateKeyword(query);
			if (StringUtils.isEmpty(item.getKeyword())) {
				return true;
			} else if( !item.getKeyword().contains(query)) {
				return true;
			}
		}
		//2、按统计字段过滤
		return ReportWebConstants.filter(qp, item);
	}
	
	/**
	 * downloadPackKtList: 受众组合详情页关键词报表下载
	 * @version beidou 3 plus
	 * @author genglei01
	 * @date 2012-9-11
	 */
	public String downloadPackKtList() throws IOException {

		// 1、初始化一下参数
		super.initParameter();
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}
		
		this.initPackName();
		
		// 下载的CSV共有四部分
		// 1、账户基本信息，2、列头，3、列表，4、汇总信息

		List<GroupKtViewItem> infoData = null;// 目标plan集
		
		// 2、获取统计数据(不分页)
		infoData = generatePackKtList(false);

		// 3、查询总条数，该数字可能不是最终值，因为有可能需要按统计值过滤，这样的话该值会被再次修改
		count = infoData.size();
		
		// 4、排序
		Collections.sort(infoData, new GroupKtComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		GroupKtViewItemSum sumData = calculatePackKtSumData(infoData);
		
		// 6、处理转化数据和UV数据
//		if (isOnlyTodayFlag) {
//			this.clearNotRealtimeStat(infoData, sumData);
//		} else {
			this.reportFacade.postHandleTransAndUvData(userId, from, to, infoData, sumData);
//		}

		GroupPackKtReportVo vo = new GroupPackKtReportVo(qp.getLevel());// 报表下载使用的VO
		vo.setAccountInfo(generatePackKtAccountInfo(user.getUsername()));
		vo.setDetails(infoData);
		vo.setHeaders(generatePackKtReportHeader());
		vo.setSummary(generatePackKtReportSummary(sumData));

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(vo, output);

		// 设置下载需要使用到的一些属性
		byte[] bytes = output.toByteArray();
		inputStream = new ByteArrayInputStream(bytes);
		fileSize = bytes.length;
		
		fileName = packName + "-";
		fileName += this.getText("download.pack.kt.filename.prefix");
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
	
	/**
	 * generatePackKtAccountInfo: 生成报表用的账户信息的VO
	 * 
	 * @return
	 */
	protected ReportAccountInfo generatePackKtAccountInfo(String userName) {
		ReportAccountInfo accountInfo = new ReportAccountInfo();
		accountInfo.setReport(this.getText("download.account.report.pack.kt"));
		accountInfo.setReportText(this.getText("download.account.report"));

		accountInfo.setAccount(userName);
		accountInfo.setAccountText(this.getText("download.account.account"));

		accountInfo.setDateRange(sd1.format(from) + " - " + sd1.format(to));
		accountInfo.setDateRangeText(this.getText("download.account.daterange"));

		String level = this.getText("download.account.level.allplan");
		Integer groupId = qp.getGroupId();
		if (groupId != null) {
			CproGroup group = cproGroupMgr.findCproGroupById(groupId);
			if (group != null) {
				CproPlan plan = cproPlanMgr.findCproPlanById(group.getPlanId());
				level += "/" + this.getText("download.account.level.plan") + plan.getPlanName();
				level += "/" + this.getText("download.account.level.group") + group.getGroupName();
				level += "/" + this.getText("download.account.level.pack") + packName;
			}
		}
			
		accountInfo.setLevel(level);
		accountInfo.setLevelText(this.getText("download.account.level"));

		return accountInfo;
	}

	/**
	 * generatePackKtReportHeader: 生成报表用的列表头
	 * 
	 * @return
	 * @since
	 */
	protected String[] generatePackKtReportHeader() {

		String[] headers = new String[DOWN_REPORT_PACK_KT_COL_NUM];
		for (int col = 0; col < headers.length; col++) {
			headers[col] = this.getText("download.pack.kt.head.col" + (col + 1));
		}
		return headers;
	}
	
	/**
	 * calculatePackKtSumData:根据返回结果休计算汇总信息
	 * 
	 * @param infoData
	 * @return 返回结果集的汇总信息
	 */
	public GroupKtViewItemSum calculatePackKtSumData(List<GroupKtViewItem> infoData) {
		GroupKtViewItemSum sumData = new GroupKtViewItemSum();

		for (GroupKtViewItem item : infoData) {
			sumData.setClks(sumData.getClks() + item.getClks());
			sumData.setCost(sumData.getCost() + item.getCost());
			sumData.setSrchs(sumData.getSrchs() + item.getSrchs());
			
			sumData.setSrchuv(sumData.getSrchuv() + item.getSrchuv());
			sumData.setClkuv(sumData.getClkuv() + item.getClkuv());
		}
		// 生成扩展数据
		sumData.generateExtentionFields();

		sumData.setKeywordCount(infoData.size());
		return sumData;
	}
	
	/**
	 * generatePackKtReportSummary: 生成报表汇总信息
	 * 
	 * @param sumData 汇总数据VO
	 * @return 用于表示报表的汇总信息VO
	 */
	protected GroupKtReportSumData generatePackKtReportSummary(GroupKtViewItemSum sumData) {

		GroupKtReportSumData sum = new GroupKtReportSumData();
		// 设置基础统计数据的汇总
		sum.setClks(sumData.getClks());
		sum.setSrchs(sumData.getSrchs());
		sum.setCost(sumData.getCost());
		sum.setAcp(sumData.getAcp().doubleValue());
		sum.setCtr(sumData.getCtr().doubleValue());
		sum.setCpm(sumData.getCpm().doubleValue());
		
		// 设置UV统计数据的汇总
		sum.setSrchuv(sumData.getSrchuv());
		sum.setClkuv(sumData.getClkuv());
		sum.setSrsur(sumData.getSrsur().doubleValue());
		sum.setCusur(sumData.getCusur().doubleValue());
		sum.setCocur(sumData.getCocur().doubleValue());
		
		sum.setSummaryText(this.getText("download.summary.pack.kt",
				new String[] { String.valueOf(sumData.getKeywordCount()) }));// 添加“合计”

		return sum;
	}
	
	public void initPackName() {
		// 设置受众组合名称
		GroupPack groupPack = groupPackMgr.getGroupPackById(qp.getGpId());
		if (groupPack != null) {
			Integer packId = groupPack.getPackId();
			List<Integer> packIds = new ArrayList<Integer>();
			packIds.add(packId);
			Map<Integer, String> packNameMap = groupPackMgr.getPackNameByPackIds(userId, packIds);
			packName = packNameMap.get(packId);
		}
	}
	
	/**
	 * 批量删除当前列表全部自选的IT定向关系
	 */
	public String deleteAllPackIt() {
		
		try {
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);
			return ERROR;
		}
		
		ItRpcQueryParam param = new ItRpcQueryParam();
		Map<Long,List<Integer>> packInterestIds = new HashMap<Long, List<Integer>>();
		if (hasFilter()) {// 有过滤的话，构造查询对象放入memcache里
			List<InterestAssistant> infoData;
			infoData = generateChosenPackItList(false);
			for (InterestAssistant item : infoData) {
				Long gpId = item.getSelfInterest().getGpId();
				
				ArrayList<Integer> interestIds = (ArrayList<Integer>) packInterestIds.get(gpId);
				if (interestIds == null) {
					interestIds = new ArrayList<Integer>();
					interestIds.add(item.getSelfInterestId());
					packInterestIds.put(gpId, interestIds);
				} else {
					interestIds.add(item.getSelfInterestId());
					packInterestIds.put(gpId, interestIds);
				}
			}
			param.setPackInterestIds(packInterestIds);
		} else {// 无过滤的话，不作处理，只转发
			token = "";
			// 无过滤，按groupid,planid,userid进行删除
			Long gpId = qp.getGpId();
			if (gpId == null) {
				gpId = 0L;
			}
			param.setUserId(qp.getUserId());
			param.setGpId(gpId);
		}

		token = TokenUtil.generateToken();
		BeidouCacheInstance.getInstance().memcacheRandomSet(token, param, TOKEN_EXPIRE);
		return SUCCESS;
	}
	
	/**
	 * 自选兴趣列表action方法
	 * @return
	 */
	public String ajaxChosenPackItList(){		

		// 1、参数初始化
		try {
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return SUCCESS;
		}

		List<InterestAssistant> infoData;// 目标结果集（可能是全集或者分页集）

		// 2、获取VO列表
		infoData = generateChosenPackItList(false);
		
		// 3、获取总记录数
		count = infoData.size();
		
		// 4、排序
		Collections.sort(infoData, new GroupItComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		InterestViewItemSum sumData = generatePackItViewSum(infoData);

		// 6、生成显示的VOList
		List<InterestViewItem> list = this.pagerPackItViewItemList(infoData);
		
		// 7、计算总页码
		int totalPage = super.getTotalPage(count);
		
		// 8、计算总行数，判断是否需要禁用排序和筛选
		boolean dataOver = isOver(count);
		
		// 9、构造JSON对象
		jsonObject.addData("hasInterest", hasInterest);
		jsonObject.addData("dataOver",dataOver);
		jsonObject.addData("list", list);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("cache", ReportConstants.Boolean.FALSE);
		jsonObject.addData("sum", sumData);
		
//		if (isOnlyTodayFlag) {
//			this.clearNotRealtimeStat(list, sumData);
//		} else {
			this.reportFacade.postHandleTransAndUvData(userId, from, to, list, sumData);
//		}

		return SUCCESS;
		
	}
	
	/**
	 * 下载自选兴趣列表action方法
	 */
	public String downloadChosenPackItList() throws IOException {

		// 1、参数初始化
		super.initParameter();
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}
		
		this.initPackName();
		
		List<InterestAssistant> infoData;// 目标结果集（可能是全集或者分页集）

		// 2、获取VO列表
		infoData = generateChosenPackItList(false);
		
		// 3、获取总记录数
		count = infoData.size();
		
		// 4、排序
		Collections.sort(infoData, new GroupItComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		InterestViewItemSum sumData = generatePackItViewSum(infoData);
		
		// 6、处理转化数据和UV数据
//		if (isOnlyTodayFlag) {
//			this.clearNotRealtimeStatForIt(infoData, sumData);
//		} else {
			this.postHandleTransAndUvData(userId, from, to, infoData, sumData);
//		}

		// 7、构造报告VO
		GroupPackItReportVo vo = new GroupPackItReportVo(qp.getLevel());// 报表下载使用的VO
		vo.setAccountInfo(generatePackItAccountInfo(user.getUsername()));
		vo.setDetails(infoData);
		vo.setHeaders(generatePackItReportHeader());
		vo.setSumData(generatePackItReportSummary(sumData));
		
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(vo, output);

		// 设置下载需要使用到的一些属性
		byte[] bytes = output.toByteArray();
		inputStream = new ByteArrayInputStream(bytes);
		fileSize = bytes.length;

		fileName = packName + "-";
		fileName += this.getText("download.pack.it.filename.prefix");
		try {
			fileName = StringUtils.subGBKString(fileName, 0, ReportWebConstants.FILE_MAIN_NAME_MAXLENGTH);
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
	
	/**
	 * 处理有展现兴趣列表的action方法
	 * @return
	 */
	public String ajaxShownPackItList(){		

		// 1、参数初始化
		try {
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return SUCCESS;
		}

		// 2、获取InterestAssistantVO列表		
		List<InterestAssistant> infoData;//目标结果集（可能是全集或者分页集）
		infoData = generateShownPackItList(false);
		
		// 3、获取总记录数（兴趣组合+一级兴趣点）
		count = infoData.size();
		
		// 4、排序
		Collections.sort(infoData, new GroupItComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		InterestViewItemSum sumData = generatePackItViewSum(infoData);

		// 6、生成显示的InterestViewItem列表
		List<InterestViewItem> list = this.pagerPackItViewItemList(infoData);
		
		// 7、计算总页码
		int totalPage = super.getTotalPage(count);
		
		// 8、计算总行数，判断是否需要禁用排序和筛选（true：溢出禁止，false：不溢出正常）
		boolean dataOver = isOver(count);
		
		// 9、构造JSON数据
		jsonObject.addData("dataOver",dataOver);
		jsonObject.addData("list", list);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("cache", ReportConstants.Boolean.FALSE);
		jsonObject.addData("sum", sumData);
		
//		if (isOnlyTodayFlag) {
//			this.clearNotRealtimeStat(list, sumData);
//		} else {
			this.reportFacade.postHandleTransAndUvData(userId, from, to, list, sumData);
//		}

		return SUCCESS;
		
	}
	
	/**
	 * 下载关键词报告Action方法
	 */
	public String downloadShownPackItList() throws IOException {

		// 1、参数初始化
		super.initParameter();
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}
		
		this.initPackName();
		
		List<InterestAssistant> infoData;// 目标结果集（可能是全集或者分页集）

		// 2、获取VO列表
		infoData = generateShownPackItList(false);
		
		// 3、获取总记录数
		count = infoData.size();
		
		// 4、排序
		Collections.sort(infoData, new GroupItComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		InterestViewItemSum sumData = generatePackItViewSum(infoData);
		
		// 6、处理转化数据和UV数据
//		if (isOnlyTodayFlag) {
//			this.clearNotRealtimeStatForIt(infoData, sumData);
//		} else {
			this.postHandleTransAndUvData(userId, from, to, infoData, sumData);
//		}

		// 7、构造报告VO
		GroupPackItReportVo vo = new GroupPackItReportVo(qp.getLevel());//报表下载使用的VO
		vo.setAccountInfo(generatePackItAccountInfo(user.getUsername()));
		vo.setDetails(infoData);
		vo.setHeaders(generatePackItReportHeader());
		vo.setSumData(generatePackItReportSummary(sumData));
		
		// 8、构造输出流
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(vo, output);

		// 9、设置下载需要使用到的一些属性
		byte[] bytes = output.toByteArray();
		inputStream = new ByteArrayInputStream(bytes);
		fileSize = bytes.length;

		fileName = packName + "-";
		fileName += this.getText("download.pack.it.filename.prefix");
		try {
			fileName = StringUtils.subGBKString(fileName, 0, ReportWebConstants.FILE_MAIN_NAME_MAXLENGTH);
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
	
	/**
	 * 生成自选兴趣报告前端显示的VO
	 * @param forFlash 是否用于flash显示
	 * @return 
	 */
	private List<InterestAssistant> generateChosenPackItList(boolean forFlash){
		List<InterestAssistant> interestAssistantList = new ArrayList<InterestAssistant>();
		
		// 校验userId信息
		Integer userId = qp.getUserId();
		if (userId == null || userId <= 0) {
			return interestAssistantList;
		}
		
		// 1、根据各层级查询受众组合关联的兴趣
		List<PackITVo> packITVoList = groupPackFacade.findAdvancdePackITByGpId(qp.getGpId());
		Map<GroupPackInterestKey, List<Integer>> packInterestMap = new HashMap<GroupPackInterestKey, List<Integer>>();
		Map<Long, String> packNameMapping = new HashMap<Long, String>();
		for (PackITVo packITVo : packITVoList) {
			GroupPackInterestKey key = new GroupPackInterestKey(packITVo.getGroupId(), packITVo.getGpId());
			
			List<Integer> value = packInterestMap.get(key);
			if (CollectionUtils.isEmpty(value)) {
				value = new ArrayList<Integer>();
				packInterestMap.put(key, value);
			}
			value.add(packITVo.getIid());
			
			packNameMapping.put(packITVo.getGpId(), packITVo.getPackName());
		}
		
		// 2 、通过当前层级下的所有推广组ids查询用户在当前层级下的所有自选的兴趣点和兴趣组合列表
		Map<GroupPackInterestKey, List<InterestVo4Report>> interestVoMap = new HashMap<GroupPackInterestKey, List<InterestVo4Report>>();
		// 受众组合下不查询普通的兴趣组合，因而将其清空
		List<Integer> groupIdList = new ArrayList<Integer>();
		interestVoMap = cproITFacade.queryGroupInterest(groupIdList, packInterestMap, userId);
 		
		if(interestVoMap.size() != 0){
			hasInterest = true;
		}

		if(!interestVoMap.isEmpty()){//如果在当前层级存在自选兴趣
			// 3、查询当前层级下所有有展现的IT，包括已经删除的兴趣点和兴趣组合
			List<InterestViewItem> allData = this.getPackItList();

			Map<GroupPackInterestKey, Map<Integer, InterestViewItem>> iidsMap = new HashMap<GroupPackInterestKey, Map<Integer, InterestViewItem>>();
			if (!CollectionUtils.isEmpty(allData)) {// 如果存在统计信息
				// 用Map存放统计信息，方便后来获取
				for (InterestViewItem stat : allData) {

					Integer iid = stat.getInterestId();
					Integer groupId = stat.getGroupId();
					Long gpId = stat.getGpId();
					GroupPackInterestKey groupPackId = new GroupPackInterestKey(groupId, gpId);

					Map<Integer, InterestViewItem> groupInteretsMap = iidsMap.get(groupPackId);
					if (groupInteretsMap == null) {
						groupInteretsMap = new HashMap<Integer, InterestViewItem>();
						iidsMap.put(groupPackId, groupInteretsMap);
					}
					groupInteretsMap.put(iid, stat);
				}
			}
			
			InterestViewItem stat = null;
			Map<Integer, InterestViewItem> groupMap;	
			// 4、遍历自选兴趣列表，构造兴趣树，并填充统计数据（如果有统计数据的话），
			for (GroupPackInterestKey groupPackId: interestVoMap.keySet()){
				List<InterestVo4Report> interestVoList = interestVoMap.get(groupPackId);
				for(InterestVo4Report interestVo: interestVoList){
					InterestAssistant interestAssistant = null;
					//构造父兴趣节点或兴趣组合
					InterestViewItem viewItem = new InterestViewItem();		
					groupMap = iidsMap.get(groupPackId);
					if(groupMap != null){
						stat = groupMap.get(interestVo.getId().intValue());	
						if(stat != null ){//有统计信息，填充统计信息
							viewItem.fillStatRecord(stat);
						}
					}
					viewItem.setInterestId(interestVo.getId());
					viewItem.setOrderId(interestVo.getOrderId());
					viewItem.setGroupId(interestVo.getGroupId());
					viewItem.setViewState(interestVo.getViewState());
					viewItem.setPlanId(interestVo.getPlanId());
					viewItem.setGpId(interestVo.getGpId());
					viewItem.setInterestName(interestVo.getName());
					if(!forFlash){
						viewItem.setGroupName(interestVo.getGroupName());
						viewItem.setPlanName(interestVo.getPlanName());
					}
					viewItem.setType(ReportWebConstants.INTEREST_TYPE_FIRST);				
					interestAssistant = new InterestAssistant((Integer)interestVo.getId());
					
					boolean isInterest = false;
					if(interestVo.getId() <= InterestConstant.MAX_INTEREST_ID){
						viewItem.setType(ReportWebConstants.INTEREST_TYPE_FIRST);
						viewItem.setOrderId(interestVo.getOrderId());
						interestAssistant.setOrderId(interestVo.getOrderId());
						isInterest = true;
					}else{
						viewItem.setType(ReportWebConstants.INTEREST_TYPE_CUSTOM);
						viewItem.setOrderId(-1);
						interestAssistant.setOrderId(-1);
					}
					viewItem.setHasDel(interestVo.isDeleted());
					
					// 设置出价信息
					viewItem.setPrice(interestVo.getPrice());
					viewItem.setOriPrice(interestVo.getOriPrice());
					viewItem.setSpecialPrice(interestVo.isSpecialPrice());
					
					// 设置受众组合名称
					Long gpId = interestVo.getGpId();
					String packName = packNameMapping.get(gpId);
					if (isInterest) {
						if (packName == null) {
							viewItem.setPackName("");
						} else {
							viewItem.setPackName(packName);
						}
					} else {
						if (packName == null) {
							viewItem.setPackName(viewItem.getInterestName());
						} else {
							viewItem.setPackName(packName);
						}
					}
					
					interestAssistant.setInterestType(interestVo.getInterestType());
					interestAssistant.setSelfInterest(viewItem);
					interestAssistant.setChildOrient(childOrient);
					
					//构造子兴趣节点
					for(InterestVo4Report childInterest: interestVo.getChildren()){
						InterestViewItem childViewItem = new InterestViewItem();
						groupMap = iidsMap.get(groupPackId);
						if(groupMap != null){
							stat = groupMap.get(childInterest.getId().intValue());		
							if(stat != null ){//有统计信息，填充统计信息
								childViewItem.fillStatRecord(stat);
							}
						}
						childViewItem.setInterestId(childInterest.getId());
						childViewItem.setOrderId(childInterest.getOrderId());
						childViewItem.setGroupId(childInterest.getGroupId());
						childViewItem.setViewState(childInterest.getViewState());
						childViewItem.setPlanId(childInterest.getPlanId());
						childViewItem.setGpId(childInterest.getGpId());
						childViewItem.setInterestName(childInterest.getName());
						if(!forFlash){
							childViewItem.setGroupName(childInterest.getGroupName());
							childViewItem.setPlanName(childInterest.getPlanName());
						}
						childViewItem.setType(ReportWebConstants.INTEREST_TYPE_SECOND);
						childViewItem.setFirstInterestId(viewItem.getInterestId());
						childViewItem.setHasDel(childInterest.isDeleted());
						
						// 设置出价信息
						childViewItem.setPrice(childInterest.getPrice());
						childViewItem.setOriPrice(childInterest.getOriPrice());
						childViewItem.setSpecialPrice(childInterest.isSpecialPrice());
						
						interestAssistant.addSecondInterest(childViewItem);
					}
					interestAssistant.ensureStatFields();
					if (!isPackItFiltered(interestAssistant))
						interestAssistantList.add(interestAssistant);
				}
			}			
		}		
		return interestAssistantList;
	}
	
	
	private List<InterestViewItem> getPackItList() {
		List<Map<String, Object>> allData = null;
		List<InterestViewItem> olapList = itStatMgr.queryGroupItData(userId, planIds, groupIds, gpIds, 
				null, null, from, to, null, 0, ReportConstants.TU_NONE);
		List<Map<String, Object>> uvData = uvDataService.queryITData(userId, planIds, groupIds, 
				gpIds, null, null, null, from, to, null, 0, ReportConstants.TU_NONE, 
				Constant.REPORT_TYPE_DEFAULT, 0, 0);
		
		// 合并转化、Holmes等其他数据
		// 排序参数
		List<String> idKeys = new ArrayList<String>();
		idKeys.add(ReportConstants.GROUP);
		idKeys.add(ReportConstants.GPID);
		idKeys.add(ReportConstants.IID);
		
		// 如果需要获取转化数据，则获取转化数据并合并
		boolean needToFetchTransData = this.transReportFacade.needToFetchTransData(userId, from, to, false);
		if (needToFetchTransData) {
			
			TempSitesAndTrans tempSiteAndTrans = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userId);
			
			// 获取转化数据
			List<Map<String, Object>> transData = transDataService.queryITData(userId, 
					tempSiteAndTrans.getTransSiteIds(), tempSiteAndTrans.getTransTargetIds(),
					planIds, groupIds, gpIds, null, null, null, from, to, null, 0,
					ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
			// 获取holmes数据
			List<Map<String, Object>> holmesData = holmesDataService.queryITData(userId, null, null, 
					planIds, groupIds, gpIds, null, null, null, from, to, null, 0,
					ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);

			// Merge基本统计/UV数据与转化数据/holmes数据
			allData = this.reportFacade.mergeTransHolmesAndUvDataByMulitKey(Constant.DorisDataType.UV, 
					transData, holmesData, uvData, idKeys);
		} else {
			// Merge基本统计/UV数据
			allData = uvData;
		}
		
		List<InterestViewItem> dorisList = new ArrayList<InterestViewItem>();
		List<InterestViewItem> resultList = new ArrayList<InterestViewItem>();
		//7、填充数据
		if (! CollectionUtils.isEmpty(allData)) {
			for (Map<String, Object> row : allData) {
				InterestViewItem item = new InterestViewItem();
				if (row != null) {
					item.setPlanId(Integer.valueOf(row.get(ReportConstants.PLAN).toString()));
					item.setGroupId(Integer.valueOf(row.get(ReportConstants.GROUP).toString()));
					item.setInterestId(Integer.valueOf(row.get(ReportConstants.IID).toString()));
					item.setGpId(Long.valueOf(row.get(ReportConstants.GPID).toString()));
					item.setRefpackId(Integer.valueOf(row.get(ReportConstants.REFPACKID).toString()));
					item.fillStatRecord(row);
					dorisList.add(item);
				}
			}
		}
		
		Set<String> mergeKeys = new HashSet<String>(Arrays.asList(
				new String[]{Constants.COLUMN.GROUPID, Constants.COLUMN.GPID, Constants.COLUMN.INTERESTID}));
		
		resultList = ReportUtils.mergeItemList(dorisList, olapList, mergeKeys, 
				Constants.statMergeVals, InterestViewItem.class, true);
		
		ReportUtil.generateExtentionFields(resultList);
		
		return resultList;
	}
	
	/**
	 * 生成用于兴趣报告前端显示的VO
	 * @param forFlash 是否用于flash显示
	 * @return 
	 */
	private List<InterestAssistant> generateShownPackItList(boolean forFlash){		
		List<InterestAssistant> interestAssistantList = new ArrayList<InterestAssistant>();
		
		// 校验userId信息
		Integer userId = qp.getUserId();
		if (userId == null || userId <= 0) {
			return interestAssistantList;
		}
		
		// 1、查询当前层级下所有有展现的IT，包括已经删除的兴趣点和兴趣组合
		List<InterestViewItem> allData = this.getPackItList();
					
		// 2、将兴趣点和兴趣组合记录放入map中，供过滤使用，并获取List<grouid,List<iid>>，待传给ITFacade
		Map<GroupPackInterestKey, Map<Integer, InterestViewItem>> iidsMap = new HashMap<GroupPackInterestKey, Map<Integer, InterestViewItem>>();
		Map<GroupPackInterestKey, List<Integer>> shownInterestMap = new HashMap<GroupPackInterestKey, List<Integer>>();	
		
		if (!CollectionUtils.isEmpty(allData)) {
			for (InterestViewItem stat : allData) {
				Integer iid = stat.getInterestId();
				Long gpId = stat.getGpId();
				Integer groupId = stat.getGroupId();
				GroupPackInterestKey groupPackKey = new GroupPackInterestKey(groupId, gpId);
				
				// 将doris每条记录存入map，供过滤使用
				Map<Integer, InterestViewItem> groupInteretsMap = iidsMap.get(groupPackKey);
				if (groupInteretsMap == null) {
					groupInteretsMap = new HashMap<Integer, InterestViewItem>();
					iidsMap.put(groupPackKey, groupInteretsMap);
				}
				groupInteretsMap.put(iid, stat);

				// 将doris每条记录存入List，待传给ITFacade
				List<Integer> list = (ArrayList<Integer>) shownInterestMap.get(groupPackKey);
				if (list == null) {
					list = new ArrayList<Integer>();
					shownInterestMap.put(groupPackKey, list);
				}
				list.add(iid);
			}
			
			// 通过CproItFacade接口，获得具有父子关系的有展现列表的兴趣树
			Map<GroupPackInterestKey, List<InterestVo4Report>> interestVoMap 
					= cproITFacade.querySrchsInterest(shownInterestMap, userId);

			// 3、遍历CproItFacade返回的兴趣树列表，构造report显示VOList并过滤
			if(!interestVoMap.isEmpty()){
				InterestViewItem stat;				
				for (GroupPackInterestKey groupPackId : interestVoMap.keySet()){
					List<InterestVo4Report> interestVoList = interestVoMap.get(groupPackId);
					
					for(InterestVo4Report interestVo: interestVoList){
						InterestAssistant interestAssistant = null;
						InterestViewItem viewItem = new InterestViewItem();				
						stat = iidsMap.get(groupPackId).get(interestVo.getId());
						viewItem.setInterestId(interestVo.getId());
						viewItem.setGroupId(interestVo.getGroupId());
						viewItem.setViewState(interestVo.getViewState());
						viewItem.setPlanId(interestVo.getPlanId());
						viewItem.setGpId(interestVo.getGpId());
						viewItem.setInterestName(interestVo.getName());
						if(!forFlash){
							viewItem.setGroupName(interestVo.getGroupName());
							viewItem.setPlanName(interestVo.getPlanName());
						}
						viewItem.fillStatRecord(stat);
						interestAssistant = new InterestAssistant((Integer)interestVo.getId());
						
						if (interestVo.getId() <= InterestConstant.MAX_INTEREST_ID) {
							viewItem.setType(ReportWebConstants.INTEREST_TYPE_FIRST);
							viewItem.setOrderId(interestVo.getOrderId());
							interestAssistant.setOrderId(interestVo.getOrderId());
						} else {
							viewItem.setType(ReportWebConstants.INTEREST_TYPE_CUSTOM);
							viewItem.setOrderId(-1);
							interestAssistant.setOrderId(-1);
						}
						interestAssistant.setInterestType(interestVo.getInterestType());
						viewItem.setHasDel(interestVo.isDeleted());
						
						// 设置出价信息
						viewItem.setPrice(interestVo.getPrice());
						viewItem.setOriPrice(interestVo.getOriPrice());
						viewItem.setSpecialPrice(interestVo.isSpecialPrice());
						
						interestAssistant.setSelfInterest(viewItem);
						interestAssistant.setChildOrient(childOrient);
						
						//构造子兴趣节点
						for(InterestVo4Report childInterest: interestVo.getChildren()){
							InterestViewItem childViewItem = new InterestViewItem();
							stat = iidsMap.get(groupPackId).get(childInterest.getId());
							childViewItem.setInterestId(childInterest.getId());
							childViewItem.setOrderId(childInterest.getOrderId());
							childViewItem.setGroupId(childInterest.getGroupId());
							childViewItem.setViewState(childInterest.getViewState());
							childViewItem.setPlanId(childInterest.getPlanId());
							childViewItem.setGpId(childInterest.getGpId());
							childViewItem.setInterestName(childInterest.getName());
							if(!forFlash){
								childViewItem.setGroupName(childInterest.getGroupName());
								childViewItem.setPlanName(childInterest.getPlanName());
							}
							childViewItem.fillStatRecord(stat);
							childViewItem.setFirstInterestId(viewItem.getInterestId());
							childViewItem.setType(ReportWebConstants.INTEREST_TYPE_SECOND);
							
							childViewItem.setHasDel(childInterest.isDeleted());
							
							// 设置出价信息
							childViewItem.setPrice(childInterest.getPrice());
							childViewItem.setOriPrice(childInterest.getOriPrice());
							childViewItem.setSpecialPrice(childInterest.isSpecialPrice());
							
							interestAssistant.addSecondInterest(childViewItem);
						}
						interestAssistant.ensureStatFields();
						if(!isPackItFiltered(interestAssistant))
							interestAssistantList.add(interestAssistant);
					}
				}
			}
		}		
		return interestAssistantList;
	}
	
	/**
	 * isPackItFiltered: 判断是否将当前记录过滤
	 *
	 * @param item 待判断VO对象
	 * @return  true表示要过滤，false表示保留    
	 */
	protected boolean isPackItFiltered(InterestAssistant item) {
		//1、按查询关键词过滤
		String query = qp.getKeyword();
		if(!StringUtils.isEmpty(query)) {
			if (StringUtils.isEmpty(item.getSelfInterest().getInterestName())) {
				return true;
			} else if( !item.getSelfInterest().getInterestName().contains(query)) {
				return true;
			}
		}
		//2、按统计字段过滤
		return ReportWebConstants.filter(qp,item.getSelfInterest());
	}
	
	/**
	 * isOver 判断当前行数是否溢出（若溢出，前端将禁止筛选和排序）
	 * @param resultSize
	 * @return
	 */
	protected boolean isOver(int resultSize) {
		return resultSize > ReportWebConstants.FRONT_SORT_THRESHOLD;
	}
	
	/**
	 * postHandleTransAndUvData: 处理转化和UV默认方式
	 * @version beidou-api 3 plus
	 * @author genglei01
	 * @date 2012-11-7
	 */
	private void postHandleTransAndUvData(Integer userId, Date from, Date to,
			List<InterestAssistant> infoData, InterestViewItemSum sumData) {
		List<InterestViewItem> list = new LinkedList<InterestViewItem>();
		
		if (CollectionUtils.isNotEmpty(infoData)) {
			for (InterestAssistant item : infoData) {
				list.addAll(item.getInterestViewItem());
			}
		}
		this.reportFacade.postHandleTransAndUvData(userId, from, to, list, sumData);
	}
	
	/**
	 * clearNotRealtimeStatForIt: 处理今日数据（除点击和消费等的其他字段）
	 * @version cpweb526
	 * @author genglei01
	 * @date 2012-11-16
	 */
//	private void clearNotRealtimeStatForIt(List<InterestAssistant> infoData, InterestViewItemSum sumData) {
//		List<InterestViewItem> list = new LinkedList<InterestViewItem>();
//		
//		if (CollectionUtils.isNotEmpty(infoData)) {
//			for (InterestAssistant item : infoData) {
//				list.addAll(item.getInterestViewItem());
//			}
//		}
//		this.clearNotRealtimeStat(list, sumData);
//	}
	
	/**
	 * 生成兴趣报告的汇总数据
	 * @param infoData
	 * @return
	 */
	private InterestViewItemSum generatePackItViewSum(List<InterestAssistant> infoData){
		//生成汇总的统计数据
		InterestViewItemSum sumData = new InterestViewItemSum();
		for (InterestAssistant item : infoData) {
			sumData.setClks(sumData.getClks() + item.getSelfInterest().getClks());
			sumData.setCost(sumData.getCost() + item.getSelfInterest().getCost());
			sumData.setSrchs(sumData.getSrchs() + item.getSelfInterest().getSrchs());
			
			sumData.setSrchuv(sumData.getSrchuv() + item.getSelfInterest().getSrchuv());
			sumData.setClkuv(sumData.getClkuv() + item.getSelfInterest().getClkuv());
		}
		// 生成扩展数据
		sumData.generateExtentionFields();
		sumData.setInterestCount(infoData.size());
		return sumData;
	}
	
	/**
	 * 获取前端显示VO的List方法
	 * @param infoData
	 * @return
	 */
	private List<InterestViewItem> pagerPackItViewItemList (List<InterestAssistant> infoData) {
		int page = 0;
		int pageSize = ReportConstants.PAGE_SIZE;
		if(qp.getPage() != null && qp.getPage() > 0) {
			page = qp.getPage();
		}
		if(qp.getPageSize() != null && qp.getPageSize() > 0) {
			pageSize = qp.getPageSize();
		}
		
		infoData = ReportWebConstants.subListinPage(infoData, page, pageSize);
		List<InterestViewItem> result = new ArrayList<InterestViewItem>();
		for(InterestAssistant ia : infoData){
			result.addAll(ia.getInterestViewItem());
		}
		return result;
	}
	
	/**
	 * generateAccountInfo: 生成报表用的账户信息的VO
	 * 
	 * @return
	 */
	protected ReportAccountInfo generatePackItAccountInfo(String userName) {
		ReportAccountInfo accountInfo = new ReportAccountInfo();
		accountInfo.setReport(this.getText("download.account.report.pack.it"));
		accountInfo.setReportText(this.getText("download.account.report"));

		accountInfo.setAccount(userName);
		accountInfo.setAccountText(this.getText("download.account.account"));

		accountInfo.setDateRange(sd1.format(from) + " - " + sd1.format(to));
		accountInfo
				.setDateRangeText(this.getText("download.account.daterange"));

		String level = this.getText("download.account.level.allplan");
		Integer groupId = qp.getGroupId();
		if (groupId != null) {
			CproGroup group = cproGroupMgr.findCproGroupById(groupId);
			if (group != null) {
				CproPlan plan = cproPlanMgr.findCproPlanById(group.getPlanId());
				level += "/" + this.getText("download.account.level.plan") + plan.getPlanName();
				level += "/" + this.getText("download.account.level.group") + group.getGroupName();
				level += "/" + this.getText("download.account.level.pack") + packName;
			}
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
	protected String[] generatePackItReportHeader() {

		String[] headers = new String[DOWN_REPORT_PACK_IT_COL_NUM];
		for (int col = 0; col < headers.length; col++) {
			headers[col] = this.getText("download.pack.it.head.col" + (col + 1));
		}
		return headers;
	}
	
	/**
	 * generateReportSummary: 生成报表汇总信息
	 * 
	 * @param sumData 汇总数据VO
	 * @return 用于表示报表的汇总信息VO
	 */
	protected InterestReportSumData generatePackItReportSummary(InterestViewItemSum sumData) {

		InterestReportSumData sum = new InterestReportSumData();
		// 设置基础统计数据的汇总		
		sum.setClks(sumData.getClks());
		sum.setSrchs(sumData.getSrchs());
		sum.setCost(sumData.getCost());
		sum.setAcp(sumData.getAcp().doubleValue());
		sum.setCtr(sumData.getCtr().doubleValue());
		sum.setCpm(sumData.getCpm().doubleValue());
		
		// 设置UV统计数据的汇总
		sum.setSrchuv(sumData.getSrchuv());
		sum.setClkuv(sumData.getClkuv());
		sum.setSrsur(sumData.getSrsur().doubleValue());
		sum.setCusur(sumData.getCusur().doubleValue());
		sum.setCocur(sumData.getCocur().doubleValue());
		
		sum.setSummaryText(this.getText("download.summary.pack.it",
				new String[] { String.valueOf(sumData.getInterestCount()) }));// 添加“合计”
		return sum;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public int getOrient() {
		return orient;
	}

	public void setOrient(int orient) {
		this.orient = orient;
	}

	public int getChildOrient() {
		return childOrient;
	}

	public void setChildOrient(int childOrient) {
		this.childOrient = childOrient;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
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

	public List<Integer> getWordIds() {
		return wordIds;
	}

	public void setWordIds(List<Integer> wordIds) {
		this.wordIds = wordIds;
	}

	public List<Long> getGpIds() {
		return gpIds;
	}

	public void setGpIds(List<Long> gpIds) {
		this.gpIds = gpIds;
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

	public boolean isHasKeyword() {
		return hasKeyword;
	}

	public void setHasKeyword(boolean hasKeyword) {
		this.hasKeyword = hasKeyword;
	}

	public String getPackName() {
		return packName;
	}

	public void setPackName(String packName) {
		this.packName = packName;
	}

	public boolean isHasInterest() {
		return hasInterest;
	}

	public void setHasInterest(boolean hasInterest) {
		this.hasInterest = hasInterest;
	}

	public void setCproPlanMgr(CproPlanMgr cproPlanMgr) {
		this.cproPlanMgr = cproPlanMgr;
	}

	public void setCproGroupMgr(CproGroupMgr cproGroupMgr) {
		this.cproGroupMgr = cproGroupMgr;
	}

	public void setCproKeywordMgr(CproKeywordMgr cproKeywordMgr) {
		this.cproKeywordMgr = cproKeywordMgr;
	}

	public void setGroupPackFacade(GroupPackFacade groupPackFacade) {
		this.groupPackFacade = groupPackFacade;
	}

	public void setGroupPackMgr(GroupPackMgr groupPackMgr) {
		this.groupPackMgr = groupPackMgr;
	}

	public void setCproITFacade(CproITFacade cproITFacade) {
		this.cproITFacade = cproITFacade;
	}

	public void setBeidouBasePath(String beidouBasePath) {
		this.beidouBasePath = beidouBasePath;
	}

	public String getBeidouBasePath() {
		return beidouBasePath;
	}
}
