package com.baidu.beidou.report.web.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;

import com.baidu.beidou.cprogroup.bo.CproGroup;
import com.baidu.beidou.cprogroup.bo.CproKeyword;
import com.baidu.beidou.cprogroup.constant.CproGroupConstant;
import com.baidu.beidou.cprogroup.facade.GroupPackFacade;
import com.baidu.beidou.cprogroup.service.CproGroupMgr;
import com.baidu.beidou.cprogroup.service.CproKeywordMgr;
import com.baidu.beidou.cprogroup.util.KTKeywordUtil;
import com.baidu.beidou.cprogroup.util.TargettypeUtil;
import com.baidu.beidou.cprogroup.vo.PackKeywordVo;
import com.baidu.beidou.cproplan.bo.CproPlan;
import com.baidu.beidou.cproplan.service.CproPlanMgr;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.olap.constant.Constants;
import com.baidu.beidou.olap.service.KeywordStatService;
import com.baidu.unbiz.olap.util.ReportUtils;
import com.baidu.beidou.olap.vo.GroupKtViewItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.report.service.ReportCacheService;
import com.baidu.beidou.report.util.ReportUtil;
import com.baidu.beidou.report.vo.KeywordInfoView;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.comparator.GroupKtComparator;
import com.baidu.beidou.report.vo.group.GroupKtReportSumData;
import com.baidu.beidou.report.vo.group.GroupKtReportVo;
import com.baidu.beidou.report.vo.group.GroupKtViewItemSum;
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
import com.baidu.beidou.util.vo.KtkeywordRpcQueryParam;
import com.baidu.beidou.util.vo.PackKeywordRpcQueryParam;

/**
 * <p>ClassName:ListCproGroupKtAction
 * <p>Function: 关键词报表
 *
 * @author   <a href="mailto:liuhao05@baidu.com">刘浩</a>
 * @created  2012-5-22
 */
public class ListCproGroupKtAction extends BeidouReportActionSupport {

	private static final long serialVersionUID = 5552040860782126944L;
	
	@Resource(name="keywordStatServiceImpl")
	private KeywordStatService ktStatMgr;

	private CproPlanMgr cproPlanMgr = null;
	private CproGroupMgr cproGroupMgr = null;
	private CproKeywordMgr cproKeywordMgr = null;
	private GroupPackFacade groupPackFacade = null;
	
	private ReportCacheService cacheService;

	/** 下载报表的列数 */
	public static final int DOWN_REPORT_QT_COL_NUM = 23;
	/** 结果集（全集）总行数 */
	int count;
	
	private List<Integer> planIds = new ArrayList<Integer>();
	private List<Integer> groupIds = new ArrayList<Integer>();
	private List<Integer> wordIds = new ArrayList<Integer>();
	private Integer planId;
	private Integer groupId;
	private int orient ;// 排序方向
	
	private boolean hasKeyword;//当前层级（未过滤时）是否含有关键词
	
//	private boolean isMixTodayAndBeforeFlag;	// 当前查询是否混合今天和过去日期
//	private boolean isOnlyTodayFlag;	// 当前查询是否只包含今天
	
	private int TOKEN_EXPIRE = 60;
	
	private String token;
	
	private String beidouBasePath;
	
	// ---------------------action方法---------------------

	//删除（当前查询条件下的）所有关键词
	public String deleteAllKt() {
		// 参数初始化
		try {
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return ERROR;
		}
		
		// 判断是否超过关键词阈值
		this.checkIfKeywordCountExceed();
		
		boolean hasFilter = hasFilter();
		if (isNormalReport()) {
			KtkeywordRpcQueryParam param = new KtkeywordRpcQueryParam();
			if (hasFilter) {// 有过滤
				List<GroupKtViewItem> infoData;// 目标结果集
				Map<Integer, List<Long>> toDelGroupWordIds = new HashMap<Integer, List<Long>>();
				// 说明：此处为了方便，直接用利用之前的方法，将过滤后的ids用于删除，
				// 其实还可以优化的，如：任何情况下不用去查atom，不用查planName,groupName等
				// 获取VO列表
				infoData = getViewItems(false);
				for (GroupKtViewItem item : infoData) {
					if (!item.isHasDel()) {
						Integer groupId = item.getGroupId();
						long wordId = item.getWordId();
						
						List<Long> wordIds = toDelGroupWordIds.get(groupId);
						if (CollectionUtils.isEmpty(wordIds)) {
							wordIds = new ArrayList<Long>();
							toDelGroupWordIds.put(groupId, wordIds);
						}
						wordIds.add(wordId);
					}
				}
				if (toDelGroupWordIds.isEmpty()) {
					return ERROR;
				}
				param.setGroupWordIds(toDelGroupWordIds);
				param.setUserId(qp.getUserId());

			} else {
				// 无过滤，按groupid,planid,userid进行删除
				param.setUserId(qp.getUserId());
				param.setPlanId(qp.getPlanId());
				param.setGroupId(qp.getGroupId());
			}
			token = TokenUtil.generateToken();
			BeidouCacheInstance.getInstance().memcacheRandomSet(token, param, TOKEN_EXPIRE);
	
			return "deleteNormalKeywords";
		} else {
			PackKeywordRpcQueryParam param = new PackKeywordRpcQueryParam();
			if (hasFilter) {//有过滤
				List<GroupKtViewItem> infoData;	// 目标结果集
				Map<Long, List<Long>> toDelPackWordIds = new HashMap<Long, List<Long>>();	// 待删除词，key为gpId，value为wordIds
				
				// 说明：此处为了方便，直接用利用之前的方法，将过滤后的ids用于删除，
				// 其实还可以优化的，如：任何情况下不用去查atom，不用查planName,groupName等
				// 获取VO列表
				infoData = getViewItems(false);
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
				if (toDelPackWordIds.isEmpty()) {
					return ERROR;
				}
				param.setPackWordIds(toDelPackWordIds);
				param.setUserId(qp.getUserId());
							
			} else {
				// 无过滤，按groupid,planid,userid进行删除
				param.setUserId(qp.getUserId());
				param.setPlanId(qp.getPlanId());
				param.setGroupId(qp.getGroupId());
			}
			token = TokenUtil.generateToken();
			BeidouCacheInstance.getInstance().memcacheRandomSet(token, param, TOKEN_EXPIRE);
	
			return "deletePackKeywords";
		}
	}
	
	private void checkIfKeywordCountExceed() {
		Integer userId = qp.getUserId();
		Integer planId = qp.getPlanId();
		Integer groupId = qp.getGroupId();
		boolean isNormalReport = isNormalReport();
		Long count = 0L;
		
		if (isNormalReport) {
			count = cproKeywordMgr.countKeywordByLevelId(groupId, planId, userId);
			
		} else {
			count = Long.valueOf(groupPackFacade.countKeywordByLevel(userId, planId, groupId));
		}
		
		if (count > ReportWebConstants.MAX_KEYWORD_COUNT_IN_KT_REPORT) {
			throw new BusinessException(this.getText("error.report.kt.exceed"));
		}
	}
	
	@Override
	protected void initStateMapping() {
		
		//做一些初始化操作
		if(qp.getPlanId() != null) {
			planIds = new ArrayList<Integer>();
			planIds.add(qp.getPlanId());
		}
		if(qp.getGroupId() != null) {
			groupIds = new ArrayList<Integer>();
			groupIds.add(qp.getGroupId());
		}
		wordIds = new ArrayList<Integer>();

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
		
		if (qp.getKtListType() == null 
				|| qp.getKtListType() < ReportConstants.KtReportType.KT_REPORT_NORMAL 
				|| qp.getKtListType() > ReportConstants.KtReportType.KT_REPORT_PACK) {
			qp.setKtListType(ReportConstants.KtReportType.KT_REPORT_NORMAL);
		}
		
		// 在initParameter之后执行条件判断
//		isMixTodayAndBeforeFlag = this.isMixTodayAndBefore(from, to);
//		isOnlyTodayFlag = this.isOnlyToday(from, to);
	}

	/**
	 * 关键词Tab取消分日图表，因而该功能并没有在cpweb-492项目中进行升级
	 * 如后续又恢复分日图表，那么需要对该功能升级
	 */
	/**
	 * ajaxFlashXml :生成XML文档，格式如下： <?xml version='1.0' encoding='utf-8'?> <data
	 * overview='最近七天' showIndex='0' tag1='/展现次数/' tag2='/点击次数/' tag3='/点击率/%'
	 * tag4='/总费用/' selected='1'> <record date='2010-06-25' data1='0.00'
	 * data2='0.00' data3='0.00%' data4='132323'/> <record date='2010-06-26'
	 * data1='0.00' data2='0.00' data3='0.03%' data4='132322'/> </data>
	 */
	/*
	public String ajaxFlashXml() {

		try {
			super.initParameterForAtLestServenDay();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_SUCCESS);
			jsonObject.addData("state",
					ReportWebConstants.FLASH_DATA_STATE.UNNORMAL);
			jsonObject.addData("xml", "");
			return SUCCESS;
		}

		Map<String, Map<String, Object>> statData; // 天粒度的汇总数据,key为时间，value为统计值

		if (hasFilter()) {// 如果需要按统计字段过滤则要查询出[from,to]+keyword维度的数据

			statData = getStatItemsWithFilter();// 时间粒度：天

		} else {
			// 否则只用查出user维度的，但需要传入过滤后的ids
			statData = getStatItemsWithoutFilter();
		}

		String xml = generateXml(statData);
		jsonObject.addData("state", ReportWebConstants.FLASH_DATA_STATE.NORMAL);
		jsonObject.addData("data", xml);

		return SUCCESS;
	}
	*/
	
	/**
	 * 判断此次请求是否有过滤条件
	 */
	protected boolean hasFilter() {
		return qp.isBoolValue() || !StringUtils.isEmpty(qp.getKeyword()) || qp.hasStatField4Filter();
	}

	/**
	 * 关键词列表Action方法
	 */
	public String ajaxList() {
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
		// 判断是否超过关键词阈值
		this.checkIfKeywordCountExceed();

		List<GroupKtViewItem> infoData;// 目标结果集（可能是全集或者分页集）

		// 2、获取VO列表
		infoData = getViewItems(false);
		
		// 3、查询总条数，该数字可能不是最终值，因为有可能需要按统计值过滤，这样的话该值会被再次修改
		count = infoData.size();
		
		// 4、排序
		Collections.sort(infoData, new GroupKtComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		GroupKtViewItemSum sumData = calculateSumData(infoData);

		// 6、生成缓存条件：总条件小于1W。
		boolean cache = shouldCache(count);
		//不缓存的话就只拿一页数据
		if(!cache) {
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
	 * 下载关键词报告Action方法
	 */
	public String download() throws IOException {

		// 1、初始化一下参数
		super.initParameter();
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}
		
		// 判断是否超过关键词阈值
		this.checkIfKeywordCountExceed();

		// 下载的CSV共有四部分
		// 1、账户基本信息，2、列头，3、列表，4、汇总信息

		List<GroupKtViewItem> infoData = null;// 目标plan集
		
		// 2、获取统计数据(不分页)
		infoData = getViewItems(false);

		// 3、查询总条数，该数字可能不是最终值，因为有可能需要按统计值过滤，这样的话该值会被再次修改
		count = infoData.size();
		
		// 4、排序
		Collections.sort(infoData, new GroupKtComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		GroupKtViewItemSum sumData = calculateSumData(infoData);
		
		// 6、处理转化数据和UV数据
//		if (isOnlyTodayFlag) {
//			this.clearNotRealtimeStat(infoData, sumData);
//		} else {
			this.reportFacade.postHandleTransAndUvData(userId, from, to, infoData, sumData);
//		}

		GroupKtReportVo vo = new GroupKtReportVo(qp.getLevel());// 报表下载使用的VO
		vo.setNormalReport(isNormalReport());
		vo.setAccountInfo(generateAccountInfo(user.getUsername()));
		vo.setDetails(infoData);
		vo.setHeaders(generateReportHeader());
		vo.setSummary(generateReportSummary(sumData));

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(vo, output);

		// 设置下载需要使用到的一些属性
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
		fileName += this.getText("download.kt.filename.prefix");
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
	 * generateAccountInfo: 生成报表用的账户信息的VO
	 * 
	 * @return
	 */
	protected ReportAccountInfo generateAccountInfo(String userName) {
		ReportAccountInfo accountInfo = new ReportAccountInfo();
		accountInfo.setReport(this.getText("download.account.report.kt"));
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
	protected String[] generateReportHeader() {

		String[] headers = new String[DOWN_REPORT_QT_COL_NUM];
		for (int col = 0; col < headers.length; col++) {
			headers[col] = this.getText("download.kt.head.col" + (col + 1));
		}
		return headers;
	}
	
	/**
	 * calculateSumData:根据返回结果休计算汇总信息
	 * 
	 * @param infoData
	 * @return 返回结果集的汇总信息
	 */
	public GroupKtViewItemSum calculateSumData(List<GroupKtViewItem> infoData) {
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
	 * generateReportSummary: 生成报表汇总信息
	 * 
	 * @param sumData 汇总数据VO
	 * @return 用于表示报表的汇总信息VO
	 */
	protected GroupKtReportSumData generateReportSummary(GroupKtViewItemSum sumData) {

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
		
		sum.setSummaryText(this.getText("download.summary.kt",
				new String[] { String.valueOf(sumData.getKeywordCount()) }));// 添加“合计”

		return sum;
	}

	// ---------------------内部方法---------------------

	/**
	 * <p>
	 * getViewItems: 生成前端VO，主要经历以下几步
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
	private List<GroupKtViewItem> getViewItems(boolean forFlashXml) {
		// 校验userId信息
		Integer userId = qp.getUserId();
		if (userId == null || userId <= 0) {
			return new ArrayList<GroupKtViewItem>();
		}
			
		// 1、查询基本统计数据和UV数据，但不排序
		boolean isNormalReport = isNormalReport();

		long start = System.currentTimeMillis();
		List<GroupKtViewItem> allData = this.getStatAndTransData(isNormalReport); //获取kt的doris数据，有缓存
		long now = System.currentTimeMillis();
		log.debug("query kt doris data cost time: " + (now - start));
	
		// 2、获取当前层级的所有关键词列表
		List<KeywordInfoView> keywords = new ArrayList<KeywordInfoView>();
		if (isNormalReport) {
			User user = userMgr.findUserBySFid(userId);//先找到user		
			List<CproKeyword> normalKeywords = cproKeywordMgr.findByLevelId(qp.getGroupId(), qp.getPlanId(), user);
			
			for (CproKeyword keyword : normalKeywords) {
				keywords.add(new KeywordInfoView(keyword));
			}
		} else {
			List<PackKeywordVo> packKeywords = groupPackFacade.findKeywordByLevel(userId, qp.getPlanId(), qp.getGroupId());
			
			Map<KeywordItemKey, KeywordInfoView> map = new HashMap<KeywordItemKey, KeywordInfoView>();

			// 在关键词Tab下受众词表中，一个高级组合会有多个相同词（存在于不同基础关键词包中）
			// 所以，需要保留一个词，保留的逻辑是，同一高级组合下，如果多个基础次包的相同词：
			// 2. 存在支持QT，保留支持高级匹配的，显示“高级”；如果没有支持高级匹配的，则显示“标准”。
			for (PackKeywordVo keyword : packKeywords) {
				KeywordItemKey newKey = new KeywordItemKey(keyword.getGroupId(), 
						keyword.getGpId(), keyword.getWordId());
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
		}
		hasKeyword = CollectionUtils.isNotEmpty(keywords);
			
		if(forFlashXml && CollectionUtils.isEmpty(allData)) {
			// 如果是统计flash使用的，且没有统计数据，则直接返回空
			return new ArrayList<GroupKtViewItem>();
		}
		
		// 生成Map有利于数据合并
		Map<KeywordItemKey, KeywordInfoView> mapView = new HashMap<KeywordItemKey, KeywordInfoView>(keywords.size());//用户所选的关键词
		Set<Integer> planIds = new HashSet<Integer>();
		Set<Integer> groupIds = new HashSet<Integer>();
		for (KeywordInfoView keyword : keywords) {
			KeywordItemKey key = new KeywordItemKey(keyword.getGroupId(), 
					keyword.getGpId(), keyword.getWordId());
			
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
		
		// 3、组装数据并且过滤，先以mysql表为基准获得已删除关键词，若没被删除，则构造VO对象
		//   通过ATOM反差已经删除的关键词，并构造VO对象
		//   添加没有doris统计数据的关键词，并构造VO对象
		
		// 存放待查询受众组合是否为已优化的gpIds
		List<Long> packOptimizedGpIdList = new ArrayList<Long>();
		
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
			Map<KeywordItemKey, GroupKtViewItem> deletedKeywordMap = new HashMap<KeywordItemKey, GroupKtViewItem>();
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
					deletedKeywordMap.put(key, stat);
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
					
					if (!isNormalReport) {
						// 设置受众信息
						view.setGpId(gpId);
						view.setPackName(keywordInfo.getPackName());
						view.setPackType(keywordInfo.getPackType());
						
						packOptimizedGpIdList.add(gpId);
					}
					
					if (!isFiltered(view)) {
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
				Map<Integer, Integer> packTypeMapping = new HashMap<Integer, Integer>();
				if (!isNormalReport) {
					Map<Integer, Map<String, Object>> packInfoMap = groupPackFacade.getPackNameByRefPackId(userId, refPackIdList);
					for (Integer id : packInfoMap.keySet()) {
						Map<String, Object> packInfo = packInfoMap.get(id);
						if (packInfo != null) {
							packNameMapping.put(id, (String)packInfo.get("name"));
							packTypeMapping.put(id, (Integer)packInfo.get("type"));
						}
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
					view.setHasDel(true);	// 标识为已经删除
					
					if (!isNormalReport) {
						KeywordItemKey key = new KeywordItemKey(statGroupId, gpId, statWordId);
						keywordInfo = mapView.get(key);
						
						// 设置受众信息
						view.setGpId(gpId);
						view.setPackName(packNameMapping.get(refPackId));
						view.setPackType(packTypeMapping.get(refPackId));
					}
					
					if(!isFiltered(view)) {
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
				
				if (!isNormalReport) {
					
					// 设置受众信息
					view.setGpId(keywordInfoView.getGpId());
					view.setPackName(keywordInfoView.getPackName());
					view.setPackType(keywordInfoView.getPackType());
					
					packOptimizedGpIdList.add(keywordInfoView.getGpId());
				}
				
				if (!isFiltered(view)) {
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
				
				if (!isNormalReport) {
					// 设置受众信息
					view.setGpId(keywordInfoView.getGpId());
					view.setPackName(keywordInfoView.getPackName());
					view.setPackType(keywordInfoView.getPackType());
					
					packOptimizedGpIdList.add(keywordInfoView.getGpId());
				}
				
				if (!isFiltered(view)) {
					// 增加关键词质量度一列
					appendKtWordQuality(blackList, view);
					infoData.add(view);
				}
			}
		}
		
		this.fillPackOptimized(infoData, packOptimizedGpIdList);
		
		return infoData;
	}
		
	private List<GroupKtViewItem> getStatAndTransData(boolean isNormalReport) {
		List<Map<String, Object>> allData;
		
		List<GroupKtViewItem> olapList = ktStatMgr.queryGroupKeywordData(qp.getUserId(), planIds, groupIds, null, null, 
				wordIds, from, to, null, 0, ReportConstants.TU_NONE, qp.getKtListType());
		
		List<Map<String, Object>> uvData = uvDataService.queryKeywordData(qp.getUserId(), planIds, 
				groupIds, null, null, null, null, from, to, null, 0, ReportConstants.TU_NONE, 
				Constant.REPORT_TYPE_DEFAULT, 0, 0, qp.getKtListType());
		
		// 2、合并转化、Holmes等其他数据
		// 排序参数
		List<String> idKeys = new ArrayList<String>();
		if (isNormalReport) {
			idKeys.add(ReportConstants.GROUP);
			idKeys.add(ReportConstants.WORD);
		} else {
			idKeys.add(ReportConstants.GPID);
			idKeys.add(ReportConstants.WORD);
		}
		
		// 如果需要获取转化数据，则获取转化数据并合并
		boolean needToFetchTransData = this.transReportFacade.needToFetchTransData(userId, from, to, false);
		if (needToFetchTransData) {
			
			TempSitesAndTrans tempSiteAndTrans = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userId);
			
			// 获取转化数据
			List<Map<String, Object>> transData = transDataService.queryKeywordData(userId, 
					tempSiteAndTrans.getTransSiteIds(), tempSiteAndTrans.getTransTargetIds(),
					planIds, groupIds, null, null, null, null, from, to, null, 0,
					ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0, qp.getKtListType());
			// 获取holmes数据
			List<Map<String, Object>> holmesData = holmesDataService.queryKeywordData(userId, null, null, 
					planIds, groupIds, null, null, null, null, from, to, null, 0,
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
				new String[]{Constants.COLUMN.GROUPID, Constants.COLUMN.GPID, Constants.COLUMN.WORDID}));
		
		resultList = ReportUtils.mergeItemList(dorisList, olapList, mergeKeys, 
				Constants.statMergeVals, GroupKtViewItem.class, true);
		
		ReportUtil.generateExtentionFields(resultList);
		
		return resultList;
	}
	
	private boolean isNormalReport() {
		return (qp.getKtListType() == ReportConstants.KtReportType.KT_REPORT_NORMAL) ? true : false;
	}
	
	/**
	 * fillPackOptimized: 向结果集中添加是否已优化的标记
	 * @version beidou-api 3 plus
	 * @author genglei01
	 * @date 2012-9-24
	 */
	private void fillPackOptimized(List<GroupKtViewItem> infoData, List<Long> packOptimizedGpIdList) {
		Map<Long, Boolean> packOptimizedMap = groupPackFacade.checkPackOptimizedByGpIds(packOptimizedGpIdList);
		
		for (GroupKtViewItem item : infoData) {
			Boolean flag = packOptimizedMap.get(item.getGpId());
			
			if (flag != null && flag) {
				item.setHasModified(true);
			}
		}
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
	 * isFiltered:判断是否将当前记录过滤
	 *
	 * @param item 待判断VO对象
	 * @return  true表示要过滤，false表示保留    
	 */
	protected boolean isFiltered(GroupKtViewItem item) {
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
	 * getStatItemsWithFilter：当有过滤条件的情况下根据过滤后的IDs获取天粒度的统计数据
	 * 
	 * @return
	 * @since
	 */
	/*
	private Map<String, Map<String, Object>> getStatItemsWithFilter() {
		List<Integer> filterIds = null;// 需要按IDS来滤过的。
		List<Map<String, Object>> statData = null;
		List<GroupKtViewItem> infoData = getViewItems(true);

		filterIds = new ArrayList<Integer>();
		for (GroupKtViewItem stat : infoData) {
			filterIds.add(stat.getKeywordId().intValue());
		}
		if (CollectionUtils.isEmpty(filterIds)) {
			return new HashMap<String, Map<String, Object>>();
		} else {
			statData = keywordStatService.queryAUserData(userId, planIds,
					groupIds,  null, null, filterIds, flashFrom, flashTo, null, 0,
					ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT);
			return generateStatMap(statData);
		}
	}
	*/

	/**
	 * getStatItemsFilterByNonStatField: 根据ids查询user维度的统计信息，前提是：无过滤字段
	 * @return 返回天粒度的统计数据
	 */
	/*
	private Map<String, Map<String, Object>> getStatItemsWithoutFilter() {
		List<Map<String, Object>> stats = keywordStatService.queryAUserData(
				userId, planIds, groupIds,  null, null, null, flashFrom, flashTo, null, 0,
				ReportConstants.TU_DAY, Constant.REPORT_TYPE_DEFAULT);
		return generateStatMap(stats);
	}
	*/

	/********************** [g/s]etters ***************************** */

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

	public CproKeywordMgr getCproKeywordMgr() {
		return cproKeywordMgr;
	}

	public void setCproKeywordMgr(CproKeywordMgr cproKeywordMgr) {
		this.cproKeywordMgr = cproKeywordMgr;
	}

	public List<Integer> getWordIds() {
		return wordIds;
	}

	public void setWwordIds(List<Integer> wordIds) {
		this.wordIds = wordIds;
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

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getBeidouBasePath() {
		return beidouBasePath;
	}

	public void setBeidouBasePath(String beidouBasePath) {
		this.beidouBasePath = beidouBasePath;
	}

	public GroupPackFacade getGroupPackFacade() {
		return groupPackFacade;
	}

	public void setGroupPackFacade(GroupPackFacade groupPackFacade) {
		this.groupPackFacade = groupPackFacade;
	}
	
	public ReportCacheService getCacheService() {
		return cacheService;
	}


	public void setCacheService(ReportCacheService cacheService) {
		this.cacheService = cacheService;
	}
}
