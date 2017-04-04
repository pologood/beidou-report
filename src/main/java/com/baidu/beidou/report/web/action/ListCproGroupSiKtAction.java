package com.baidu.beidou.report.web.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

import com.baidu.beidou.cprogroup.bo.CproGroup;
import com.baidu.beidou.cprogroup.bo.SmartIdeaKeyword;
import com.baidu.beidou.cprogroup.constant.CproGroupConstant;
import com.baidu.beidou.cprogroup.service.CproGroupMgr;
import com.baidu.beidou.cprogroup.service.SmartIdeaKeywordMgr;
import com.baidu.beidou.cprogroup.util.KTKeywordUtil;
import com.baidu.beidou.cproplan.bo.CproPlan;
import com.baidu.beidou.cproplan.service.CproPlanMgr;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.exception.ParameterInValidException;
import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.report.vo.KeywordInfoView;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.comparator.GroupKtComparator;
import com.baidu.beidou.report.vo.group.GroupKtReportSumData;
import com.baidu.beidou.report.vo.group.GroupKtReportVo;
import com.baidu.beidou.olap.vo.GroupKtViewItem;
import com.baidu.beidou.report.vo.group.GroupKtViewItemSum;
import com.baidu.beidou.report.vo.group.KeywordItemKey;
import com.baidu.beidou.stat.constant.StatConstant;
import com.baidu.beidou.stat.driver.bo.Constant;
import com.baidu.beidou.stat.service.SmartIdeaKeywordStatService;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.LogUtils;
import com.baidu.beidou.util.StringUtils;
import com.baidu.beidou.util.TokenUtil;
import com.baidu.beidou.util.atomdriver.AtomUtils;
import com.baidu.beidou.util.memcache.BeidouCacheInstance;
import com.baidu.beidou.util.vo.KtkeywordRpcQueryParam;

/**
 * 智能创意推广组优选词/有展现词报告
 * 
 * @author Zhang Xu
 */
public class ListCproGroupSiKtAction extends BeidouReportActionSupport {

	private static final long serialVersionUID = 2689815547291404408L;

	private SmartIdeaKeywordStatService smartIdeaKeywordStatService;

	private CproPlanMgr cproPlanMgr = null;
	private CproGroupMgr cproGroupMgr = null;
	private SmartIdeaKeywordMgr smartIdeaKeywordMgr = null;

	/** 下载报表的列数 */
	public static final int DOWN_REPORT_QT_COL_NUM = 23;
	/** 结果集（全集）总行数 */
	int count;

	private List<Integer> planIds = new ArrayList<Integer>();
	private List<Integer> groupIds = new ArrayList<Integer>();
	private List<Integer> wordIds = new ArrayList<Integer>();
	private Integer planId;
	private Integer groupId;
	private Integer patternId;
	private int orient;// 排序方向

	private boolean hasKeyword;//当前层级（未过滤时）是否含有优选词

//	private boolean isMixTodayAndBeforeFlag; // 当前查询是否混合今天和过去日期

	private String token;

	private String beidouBasePath;
	
	private final static int TOKEN_EXPIRE = 60;
	
	private final static Long DEFAULT_GPID = 0L;

	// ---------------------action方法---------------------

	//删除（当前查询条件下的）所有优选词
	public String deleteAll() {
		// 参数初始化
		try {
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return ERROR;
		}

		// 判断是否超过优选词阈值
		this.checkIfKeywordCountExceed();

		boolean hasFilter = hasFilter();

		KtkeywordRpcQueryParam param = new KtkeywordRpcQueryParam();
		if (hasFilter) {// 有过滤
			List<GroupKtViewItem> infoData;// 目标结果集
			Map<Integer, List<Long>> toDelGroupWordIds = new HashMap<Integer, List<Long>>();
			// 说明：此处为了方便，直接用利用之前的方法，将过滤后的ids用于删除，
			// 其实还可以优化的，如：任何情况下不用去查atom，不用查planName,groupName等
			// 获取VO列表
			infoData = getViewItems();
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

		return "deleteSiKeywords";
	}

	/**
	 * 优选词列表Action方法
	 */
	public String ajaxList() {
		//1. 参数初始化
		try {
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return SUCCESS;
		}

		//2. 判断是否超过优选词阈值
		checkIfKeywordCountExceed();

		List<GroupKtViewItem> infoData;// 目标结果集（可能是全集或者分页集）

		//3. 获取VO列表
		infoData = getViewItems();

		//4. 查询总条数，该数字可能不是最终值，因为有可能需要按统计值过滤，这样的话该值会被再次修改
		count = infoData.size();

		//5. 排序
		Collections.sort(infoData, new GroupKtComparator(qp.getOrderBy(), orient));

		//6. 生成汇总的统计数据
		GroupKtViewItemSum sumData = calculateSumData(infoData);

		//7. 生成缓存条件：总条件小于1W
		boolean cache = shouldCache(count);
		// 不缓存则只拿一页数据
		if (!cache) {
			infoData = ReportWebConstants.subListinPage(infoData, qp.getPage(), qp.getPageSize());
		}

		//8. 计算总页码
		int totalPage = super.getTotalPage(count);

		jsonObject.addData("list", infoData);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("hasKeyword", hasKeyword);
		jsonObject.addData("cache", cache ? ReportConstants.Boolean.TRUE : ReportConstants.Boolean.FALSE);
		jsonObject.addData("sum", sumData);

		return SUCCESS;
	}
	
	/**
	 * 有展现词列表Action方法
	 */
	public String ajaxListShown() {
		//1. 参数初始化
		try {
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return SUCCESS;
		}

		//2. 判断是否超过优选词阈值
		checkIfKeywordCountExceed();

		List<GroupKtViewItem> infoData;// 目标结果集（可能是全集或者分页集）

		//3. 获取VO列表
		infoData = getShownViewItems();

		//4. 查询总条数，该数字可能不是最终值，因为有可能需要按统计值过滤，这样的话该值会被再次修改
		count = infoData.size();

		//5. 排序
		Collections.sort(infoData, new GroupKtComparator(qp.getOrderBy(), orient));

		//6. 生成汇总的统计数据
		GroupKtViewItemSum sumData = calculateSumData(infoData);

		//7. 生成缓存条件：总条件小于1W
		boolean cache = shouldCache(count);
		// 不缓存则只拿一页数据
		if (!cache) {
			infoData = ReportWebConstants.subListinPage(infoData, qp.getPage(), qp.getPageSize());
		}

		//8. 计算总页码
		int totalPage = super.getTotalPage(count);

		jsonObject.addData("list", infoData);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("hasKeyword", hasKeyword);
		jsonObject.addData("cache", cache ? ReportConstants.Boolean.TRUE : ReportConstants.Boolean.FALSE);
		jsonObject.addData("sum", sumData);

		return SUCCESS;
	}

	/**
	 * 下载优选词报告
	 */
	public String download() throws IOException {

		//1. 初始化一下参数
		super.initParameter();
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}

		//2. 判断是否超过优选词阈值
		checkIfKeywordCountExceed();

		// 下载的CSV共有四部分
		// 1、账户基本信息，2、列头，3、列表，4、汇总信息
		List<GroupKtViewItem> infoData = null;// 目标plan集

		//3. 获取统计数据(不分页)
		infoData = getViewItems();

		//4. 查询总条数，该数字可能不是最终值，因为有可能需要按统计值过滤，这样的话该值会被再次修改
		count = infoData.size();

		//5. 排序
		Collections.sort(infoData, new GroupKtComparator(qp.getOrderBy(), orient));

		//6. 生成汇总的统计数据
		GroupKtViewItemSum sumData = calculateSumData(infoData);

		GroupKtReportVo vo = new GroupKtReportVo(qp.getLevel());// 报表下载使用的VO
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

		if (qp.getGroupId() != null) {
			CproGroup group = cproGroupMgr.findCproGroupById(qp.getGroupId());
			if (group != null) {
				fileName = group.getGroupName() + "-";
			}
		} else if (qp.getPlanId() != null) {
			CproPlan plan = cproPlanMgr.findCproPlanById(qp.getPlanId());
			if (plan != null) {
				fileName = plan.getPlanName() + "-";
			}
		} else {
			fileName = user.getUsername() + "-";
		}
		fileName += "关键词-优选模板词";
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
	 * 下载有展现词报告
	 */
	public String downloadShown() throws IOException {

		//1. 初始化一下参数
		super.initParameter();
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}

		//2. 判断是否超过优选词阈值
		checkIfKeywordCountExceed();

		// 下载的CSV共有四部分
		// 1、账户基本信息，2、列头，3、列表，4、汇总信息
		List<GroupKtViewItem> infoData = null;// 目标plan集

		//3. 获取统计数据(不分页)
		infoData = getShownViewItems();

		//4. 查询总条数，该数字可能不是最终值，因为有可能需要按统计值过滤，这样的话该值会被再次修改
		count = infoData.size();

		//5. 排序
		Collections.sort(infoData, new GroupKtComparator(qp.getOrderBy(), orient));

		//6. 生成汇总的统计数据
		GroupKtViewItemSum sumData = calculateSumData(infoData);

		GroupKtReportVo vo = new GroupKtReportVo(qp.getLevel());// 报表下载使用的VO
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

		if (qp.getGroupId() != null) {
			CproGroup group = cproGroupMgr.findCproGroupById(qp.getGroupId());
			if (group != null) {
				fileName = group.getGroupName() + "-";
			}
		} else if (qp.getPlanId() != null) {
			CproPlan plan = cproPlanMgr.findCproPlanById(qp.getPlanId());
			if (plan != null) {
				fileName = plan.getPlanName() + "-";
			}
		} else {
			fileName = user.getUsername() + "-";
		}
		fileName += "关键词-优选模板词-分关键词";
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
	 * generateAccountInfo: 生成报表用的账户信息的VO
	 */
	protected ReportAccountInfo generateAccountInfo(String userName) {
		ReportAccountInfo accountInfo = new ReportAccountInfo();
		accountInfo.setReport(this.getText("download.account.report.kt"));
		accountInfo.setReportText(this.getText("download.account.report"));

		accountInfo.setAccount(userName);
		accountInfo.setAccountText(this.getText("download.account.account"));

		accountInfo.setDateRange(sd1.format(from) + " - " + sd1.format(to));
		accountInfo.setDateRangeText(this.getText("download.account.daterange"));

		String level = this.getText("download.account.level.allplan");
		if (QueryParameterConstant.LEVEL.LEVEL_PLAN.equals(qp.getLevel())) {

			Integer planId = qp.getPlanId();
			if (planId != null) {
				CproPlan plan = cproPlanMgr.findCproPlanById(planId);
				if (plan != null) {
					level += "/" + this.getText("download.account.level.plan") + plan.getPlanName();
				}
			}
		} else if (QueryParameterConstant.LEVEL.LEVEL_GROUP.equals(qp.getLevel())) {
			Integer groupId = qp.getGroupId();
			if (groupId != null) {
				CproGroup group = cproGroupMgr.findCproGroupById(groupId);
				if (group != null) {
					CproPlan plan = cproPlanMgr.findCproPlanById(group.getPlanId());
					level += "/" + this.getText("download.account.level.plan") + plan.getPlanName();
					level += "/" + this.getText("download.account.level.group") + group.getGroupName();
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

		sum.setSummaryText(this.getText("download.summary.kt", new String[] { String.valueOf(sumData.getKeywordCount()) }));// 添加“合计”

		return sum;
	}
	
	@Override
	protected void initStateMapping() {
		//做一些初始化操作
		if (qp.getPlanId() != null) {
			planIds = new ArrayList<Integer>();
			planIds.add(qp.getPlanId());
		}
		if (qp.getGroupId() != null) {
			groupIds = new ArrayList<Integer>();
			groupIds.add(qp.getGroupId());
		}
		wordIds = new ArrayList<Integer>();

		orient = ReportConstants.SortOrder.ASC;
		if (QueryParameterConstant.SortOrder.SORTORDER_DESC.equalsIgnoreCase(qp.getOrder())) {
			orient = ReportConstants.SortOrder.DES;
		}

		if (qp.getPage() == null || qp.getPage() < 0) {
			qp.setPage(0);
		}

		if (qp.getPageSize() == null || qp.getPageSize() < 1) {
			qp.setPageSize(ReportConstants.PAGE_SIZE);
		}

		if (qp.getKtListType() == null) {
			qp.setKtListType(ReportConstants.KtReportType.KT_REPORT_SMARTIDEA_KEYWORD);
		}
		
		if (qp.getKtListType() == ReportConstants.KtReportType.KT_REPORT_SMARTIDEA_KEYWORD_DETAIL) {
			if (patternId == null || patternId == 0) {
				throw new ParameterInValidException("分关键词报告必须传递优选模板词patternId");
			}
		}
		
		// 在initParameter之后执行条件判断
//		isMixTodayAndBeforeFlag = this.isMixTodayAndBefore(from, to);
	}

	/**
	 * 判断此次请求是否有过滤条件
	 */
	protected boolean hasFilter() {
		return qp.isBoolValue() || !StringUtils.isEmpty(qp.getKeyword()) || qp.hasStatField4Filter();
	}

	// ---------------------内部方法---------------------

	/**
	 * getViewItems: 生成前端VO，主要经历以下几步
	 * </p>
	 * 0、参数处理，主要是设置时间参数和userId到query parameter对象中
	 * 1、通过SmartIdeaKeywordStatService查询指定时间段内用户所有的词统计信息；
	 * 2、获取该层级下所有的优选词；
	 * 3、通过keywordid把#1和#2内容进行Merge：如果bool（只查询已添加）=true则用#1做基内容，#2中多余数据丢弃；如bool=false，
	 * 则#1和#2 merge之后得到A，#2中未merge部分通过wordid反查Atom，结果集与#2再进行merge得到B，最后再将A、B两部分结果集进行Merge；
	 * 4、按照统计字段、优选词进行过滤和排序；
	 * 5、生成汇总的统计数据；
	 * 6、如果需要分页的话获取分页数据；
	 * 7、生成分页汇总数据
	 * 8、返回jsonObject
	 * 
	 * @return 满足前端查询条件的对象
	 */
	private List<GroupKtViewItem> getViewItems() {
		// 校验userId信息
		Integer userId = qp.getUserId();
		if (userId == null || userId <= 0) {
			return new ArrayList<GroupKtViewItem>();
		}

		// 查询基本统计数据和UV数据，但不排序
		long start = System.currentTimeMillis();
		List<Map<String, Object>> allData = this.getStatData(StatConstant.SMARTIDEA_IS_PATTERN_TRUE);
		long now = System.currentTimeMillis();
		log.debug("query si kt doris data cost time: " + (now - start));

		// 获取当前层级的所有优选词列表
		List<KeywordInfoView> keywords = new ArrayList<KeywordInfoView>();
		List<SmartIdeaKeyword> siKeywords = smartIdeaKeywordMgr.findKeywordsByLevelId(qp.getGroupId(), qp.getPlanId(), qp.getUserId());
		for (SmartIdeaKeyword keyword : siKeywords) {
			keywords.add(new KeywordInfoView(keyword));
		}
		
		hasKeyword = CollectionUtils.isNotEmpty(keywords);

		// 生成Map有利于数据合并
		Map<KeywordItemKey, KeywordInfoView> mapView = new HashMap<KeywordItemKey, KeywordInfoView>(keywords.size());//用户所选的优选词
		Set<Integer> planIds = new HashSet<Integer>();
		Set<Integer> groupIds = new HashSet<Integer>();
		for (KeywordInfoView keyword : keywords) {
			KeywordItemKey key = new KeywordItemKey(keyword.getGroupId(), DEFAULT_GPID, keyword.getWordId());

			mapView.put(key, keyword);
			planIds.add(keyword.getPlanId());
			groupIds.add(keyword.getGroupId());
		}
		for (Map<String, Object> stat : allData) {
			planIds.add(Integer.valueOf(stat.get(ReportConstants.PLAN).toString()));
			groupIds.add(Integer.valueOf(stat.get(ReportConstants.GROUP).toString()));
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

		// 组装数据并且过滤，先以mysql表为基准获得已删除优选词，若没被删除，则构造VO对象
		//   通过ATOM反差已经删除的优选词，并构造VO对象
		//   添加没有doris统计数据的优选词，并构造VO对象
		List<GroupKtViewItem> infoData = new ArrayList<GroupKtViewItem>();
		if (!CollectionUtils.isEmpty(allData)) {
			// 如果有统计数据则进行后续的数据合并操作；
			Long statWordId;
			Integer statPlanId;
			Integer statGroupId;
			KeywordInfoView keywordInfo;

			// 存放已删除但有统计信息的优选词统计信息，key为wordId，value为统计信息
			Map<KeywordItemKey, Map<String, Object>> deletedKeywordMap = new HashMap<KeywordItemKey, Map<String, Object>>();
			Set<Integer> deletedWordIds = new HashSet<Integer>();

			// 遍历统计数据，如果相应优选词数据库中不存在则放入删除Map，待后续处理
			for (Map<String, Object> stat : allData) {
				try {
					statWordId = Long.valueOf(stat.get(ReportConstants.WORD).toString());
					statGroupId = Integer.valueOf(stat.get(ReportConstants.GROUP).toString());
					statPlanId = Integer.valueOf(stat.get(ReportConstants.PLAN).toString());
				} catch (Exception e) {
					log.error("error to digest doris data[" + stat + "]", e);
					continue;
				}

				KeywordItemKey key = new KeywordItemKey(statGroupId, DEFAULT_GPID, statWordId);
				keywordInfo = mapView.remove(key);
				if (keywordInfo == null) {
					// 如果优选词已被刪除，则记录之，以备后续查atom；
					deletedKeywordMap.put(key, stat);
					deletedWordIds.add(statWordId.intValue());
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
					view.setQualityDg(CproGroupConstant.KT_WORD_QUALITY_DEGREE_3);
					if (!isFiltered(view)) {
						infoData.add(view);
					}
				}
			}

			// 处理已删除词的统计数据
			if (!qp.isBoolValue() && !deletedKeywordMap.isEmpty()) {

				// 需要查询Atom
				Map<Integer, String> atomIdKeywordMapping = AtomUtils.getWordById(deletedWordIds);

				for (Map<String, Object> stat : deletedKeywordMap.values()) {
					try {
						statWordId = Long.valueOf(stat.get(ReportConstants.WORD).toString());
						statGroupId = Integer.valueOf(stat.get(ReportConstants.GROUP).toString());
						statPlanId = Integer.valueOf(stat.get(ReportConstants.PLAN).toString());
					} catch (Exception e) {
						log.error("error to digest doris data[" + stat + "]", e);
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
					view.setHasDel(true); // 标识为已经删除
					view.setQualityDg(CproGroupConstant.KT_WORD_QUALITY_DEGREE_3);
					if (!isFiltered(view)) {
						infoData.add(view);
					}
				}
			}

			// 添加剩下的没有统计数据的词
			for (KeywordInfoView keywordInfoView : mapView.values()) {
				GroupKtViewItem view = new GroupKtViewItem();
				view.setPlanId(keywordInfoView.getPlanId());
				view.setPlanName(planIdNameMapping.get(keywordInfoView.getPlanId()));
				view.setGroupId(keywordInfoView.getGroupId());
				view.setGroupName(groupIdNameMapping.get(keywordInfoView.getGroupId()));
				view.setWordId(keywordInfoView.getWordId());
				view.setKeyword(keywordInfoView.getKeyword());
				view.setQualityDg(CproGroupConstant.KT_WORD_QUALITY_DEGREE_3);
				if (!isFiltered(view)) {
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
				view.setQualityDg(CproGroupConstant.KT_WORD_QUALITY_DEGREE_3);
				if (!isFiltered(view)) {
					infoData.add(view);
				}
			}
		}

		return infoData;
	}
	
	private List<GroupKtViewItem> getShownViewItems() {
		// 校验userId信息
		Integer userId = qp.getUserId();
		if (userId == null || userId <= 0) {
			return new ArrayList<GroupKtViewItem>();
		}

		// 查询基本统计数据和UV数据，但不排序
		long start = System.currentTimeMillis();
		List<Map<String, Object>> allData = this.getStatData(StatConstant.SMARTIDEA_IS_PATTERN_FALSE);
		long now = System.currentTimeMillis();
		log.debug("query si kt doris data cost time: " + (now - start));

		Set<Integer> planIds = new HashSet<Integer>();
		Set<Integer> groupIds = new HashSet<Integer>();
		for (Map<String, Object> stat : allData) {
			planIds.add(Integer.valueOf(stat.get(ReportConstants.PLAN).toString()));
			groupIds.add(Integer.valueOf(stat.get(ReportConstants.GROUP).toString()));
		}

		// 根据groupIds和planIds查找对应的推广组和推广计划列表
		Map<Integer, String> planIdNameMapping = cproPlanMgr.findPlanNameByPlanIds(planIds);
		Map<Integer, String> groupIdNameMapping = cproGroupMgr.findGroupNameByGroupIds(groupIds);
		
		List<SmartIdeaKeyword> delShownKeywords = smartIdeaKeywordMgr.findDelShownKeywordsByLevelId(qp.getGroupId(), qp.getPlanId(), qp.getUserId());
		Map<KeywordItemKey, SmartIdeaKeyword> delShownKeywordMap = new HashMap<KeywordItemKey, SmartIdeaKeyword>();
		if (!CollectionUtils.isEmpty(delShownKeywords)) {
			for (SmartIdeaKeyword keyword : delShownKeywords) {
				KeywordItemKey key = new KeywordItemKey(keyword.getGroupId(), DEFAULT_GPID, keyword.getWordId());
				delShownKeywordMap.put(key, keyword);
			}
		}

		List<GroupKtViewItem> infoData = new ArrayList<GroupKtViewItem>();
		if (!CollectionUtils.isEmpty(allData)) {
			Long statWordId;
			Integer statPlanId;
			Integer statGroupId;

			Map<KeywordItemKey, Map<String, Object>> keywordMap = new HashMap<KeywordItemKey, Map<String, Object>>();
			Set<Integer> wordIdSet = new HashSet<Integer>();

			for (Map<String, Object> stat : allData) {
				try {
					statWordId = Long.valueOf(stat.get(ReportConstants.WORD).toString());
					statGroupId = Integer.valueOf(stat.get(ReportConstants.GROUP).toString());
					statPlanId = Integer.valueOf(stat.get(ReportConstants.PLAN).toString());
				} catch (Exception e) {
					log.error("error to digest doris data[" + stat + "]", e);
					continue;
				}

				KeywordItemKey key = new KeywordItemKey(statGroupId, DEFAULT_GPID, statWordId);
				keywordMap.put(key, stat);
				wordIdSet.add(statWordId.intValue());
			}

			if (!keywordMap.isEmpty()) {
				// 需要查询Atom
				Map<Integer, String> atomIdKeywordMapping = AtomUtils.getWordById(wordIdSet);
				for (Map<String, Object> stat : keywordMap.values()) {
					try {
						statWordId = Long.valueOf(stat.get(ReportConstants.WORD).toString());
						statGroupId = Integer.valueOf(stat.get(ReportConstants.GROUP).toString());
						statPlanId = Integer.valueOf(stat.get(ReportConstants.PLAN).toString());
					} catch (Exception e) {
						log.error("error to digest doris data[" + stat + "]", e);
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
					KeywordItemKey key = new KeywordItemKey(statGroupId, DEFAULT_GPID, statWordId);
					if (delShownKeywordMap.containsKey(key)) {
						view.setHasDel(true);
					} else {
						view.setHasDel(false);
					}
					view.setQualityDg(CproGroupConstant.KT_WORD_QUALITY_DEGREE_3);
					if (!isFiltered(view)) {
						infoData.add(view);
					}
				}
			}
		} 

		return infoData;
	}
	
	private void checkIfKeywordCountExceed() {
		Integer userId = qp.getUserId();
		Integer planId = qp.getPlanId();
		Integer groupId = qp.getGroupId();
		boolean isSiKeywordNormalReport = isSiKeywordNormalReport();
		if (isSiKeywordNormalReport) {
			Integer count = smartIdeaKeywordMgr.countKeywordsByLevelId(groupId, planId, userId);
			if (count > ReportWebConstants.MAX_KEYWORD_COUNT_IN_SI_KT_REPORT) {
				throw new BusinessException(this.getText("error.report.kt.exceed"));
			}
		}
	}

	/**
	 * 查询doris获取基础统计数据
	 * @param type 类型，优选词或者有展现词
	 * @return 报告数据
	 */
	private List<Map<String, Object>> getStatData(int type) {
		List<Map<String, Object>> allData;
		if (type == StatConstant.SMARTIDEA_IS_PATTERN_TRUE) {
			allData =  smartIdeaKeywordStatService.queryGroupSiKeywordData(qp.getUserId(), planIds, groupIds, null, null, from, to, null, 0, ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, type);
		} else {
			List<Integer> patternIds = new ArrayList<Integer>(1);
			patternIds.add(patternId);
			allData =  smartIdeaKeywordStatService.queryGroupSiKeywordData(qp.getUserId(), planIds, groupIds, null, patternIds, from, to, null, 0, ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, type);
		}
		return allData;
	}

	/**
	 * 判断当前报告是否为优选词或者有展现词报告
	 */
	private boolean isSiKeywordNormalReport() {
		return (qp.getKtListType() == ReportConstants.KtReportType.KT_REPORT_SMARTIDEA_KEYWORD) ? true : false;
	}

	/**
	 * isFiltered:判断是否将当前记录过滤
	 *
	 * @param item 待判断VO对象
	 * @return  true表示要过滤，false表示保留    
	 */
	protected boolean isFiltered(GroupKtViewItem item) {
		//1、按查询优选词过滤
		String query = qp.getKeyword();
		if (!StringUtils.isEmpty(query)) {
			query = KTKeywordUtil.validateKeyword(query);
			if (StringUtils.isEmpty(item.getKeyword())) {
				return true;
			} else if (!item.getKeyword().contains(query)) {
				return true;
			}
		}
		//2、按统计字段过滤
		return ReportWebConstants.filter(qp, item);
	}

	public CproPlanMgr getCproPlanMgr() {
		return cproPlanMgr;
	}

	public void setCproPlanMgr(CproPlanMgr cproPlanMgr) {
		this.cproPlanMgr = cproPlanMgr;
	}

	public SmartIdeaKeywordStatService getSmartIdeaKeywordStatService() {
		return smartIdeaKeywordStatService;
	}

	public void setSmartIdeaKeywordStatService(SmartIdeaKeywordStatService smartIdeaKeywordStatService) {
		this.smartIdeaKeywordStatService = smartIdeaKeywordStatService;
	}

	public CproGroupMgr getCproGroupMgr() {
		return cproGroupMgr;
	}

	public void setCproGroupMgr(CproGroupMgr cproGroupMgr) {
		this.cproGroupMgr = cproGroupMgr;
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

	public SmartIdeaKeywordMgr getSmartIdeaKeywordMgr() {
		return smartIdeaKeywordMgr;
	}

	public void setSmartIdeaKeywordMgr(SmartIdeaKeywordMgr smartIdeaKeywordMgr) {
		this.smartIdeaKeywordMgr = smartIdeaKeywordMgr;
	}

	public Integer getPatternId() {
		return patternId;
	}

	public void setPatternId(Integer patternId) {
		this.patternId = patternId;
	}

}
