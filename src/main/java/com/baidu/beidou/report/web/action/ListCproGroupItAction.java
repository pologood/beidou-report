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
import com.baidu.beidou.cprogroup.constant.InterestConstant;
import com.baidu.beidou.cprogroup.facade.CproITFacade;
import com.baidu.beidou.cprogroup.facade.GroupPackFacade;
import com.baidu.beidou.cprogroup.service.CproGroupMgr;
import com.baidu.beidou.cprogroup.service.InterestMgr;
import com.baidu.beidou.cprogroup.util.TargettypeUtil;
import com.baidu.beidou.cprogroup.vo.GroupPackInterestKey;
import com.baidu.beidou.cprogroup.vo.InterestVo4Report;
import com.baidu.beidou.cprogroup.vo.PackITVo;
import com.baidu.beidou.cproplan.bo.CproPlan;
import com.baidu.beidou.cproplan.service.CproPlanMgr;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.olap.constant.Constants;
import com.baidu.beidou.olap.service.ItStatService;
import com.baidu.unbiz.olap.util.ReportUtils;
import com.baidu.beidou.olap.vo.InterestViewItem;
import com.baidu.beidou.pack.constant.PackTypeConstant;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.exception.ParameterInValidException;
import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.report.service.ReportCproGroupMgr;
import com.baidu.beidou.report.util.ReportUtil;
import com.baidu.beidou.report.vo.QueryParameter;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.comparator.GroupItComparator;
import com.baidu.beidou.report.vo.group.InterestAssistant;
import com.baidu.beidou.report.vo.group.InterestReportSumData;
import com.baidu.beidou.report.vo.group.InterestReportVo;
import com.baidu.beidou.report.vo.group.InterestViewItemSum;
import com.baidu.beidou.stat.driver.bo.Constant;
import com.baidu.beidou.tool.util.TransReportHelper;
import com.baidu.beidou.tool.vo.TempSitesAndTrans;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.LogUtils;
import com.baidu.beidou.util.StringUtils;
import com.baidu.beidou.util.TokenUtil;
import com.baidu.beidou.util.memcache.BeidouCacheInstance;
import com.baidu.beidou.util.vo.ItRpcQueryParam;

/**
 * 兴趣定向report模块Action类
 * @author liuhao05
 *
 */
public class ListCproGroupItAction extends BeidouReportActionSupport{
	private static final long serialVersionUID = 1L; 	
	private CproITFacade cproITFacade;	
	private CproPlanMgr cproPlanMgr = null;
	private CproGroupMgr cproGroupMgr = null;	
	private ReportCproGroupMgr reportGroupMgr = null;
	private GroupPackFacade groupPackFacade = null;
	private InterestMgr interestMgr = null;
	
	@Resource(name="itStatServiceImpl")
	ItStatService itStatMgr;
	
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
	private Integer planid;
	private Integer groupid;
	private String beidouBasePath;
	/**
	 * 当前层级下是否有自选兴趣
	 */
	private boolean hasInterest = false;
	
//	private boolean isMixTodayAndBeforeFlag;	// 当前查询是否混合今天和过去日期
//	private boolean isOnlyTodayFlag;	// 当前查询是否只包含今天
	
	/**
	 * <p>initParameter: 初始化参数，一般在列表页用
	 * @throws Exception 
	 *      
	*/
	protected void initParameter() {
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
		childOrient = ReportConstants.SortOrder.ASC;
		if (QueryParameterConstant.SortOrder.SORTORDER_DESC
				.equalsIgnoreCase(qp.getOrder())) {
			orient = ReportConstants.SortOrder.DES;		
			if(qp.getOrderBy().equals("interestName")){
				childOrient = ReportConstants.SortOrder.DES;
			}
		}
		if(qp.getPage() == null || qp.getPage() < 0) {
			qp.setPage(0);
		}

		if(qp.getPageSize() == null || qp.getPageSize() < 1) {
			qp.setPageSize(ReportConstants.PAGE_SIZE );
		}
		
		// 在initParameter之后执行条件判断
//		isMixTodayAndBeforeFlag = this.isMixTodayAndBefore(from, to);
//		isOnlyTodayFlag = this.isOnlyToday(from, to);
	}
	
	/**
	 * generateAccountInfo: 生成报表用的账户信息的VO
	 * 
	 * @return
	 */
	protected ReportAccountInfo generateAccountInfo(String userName) {
		ReportAccountInfo accountInfo = new ReportAccountInfo();
		accountInfo.setReport(this.getText("download.account.report.it"));
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

		String[] headers = new String[23];
		for (int col = 0; col < headers.length; col++) {
			headers[col] = this.getText("download.it.head.col" + (col + 1));
		}
		return headers;
	}
	
	/**
	 * generateReportSummary: 生成报表汇总信息
	 * 
	 * @param sumData 汇总数据VO
	 * @return 用于表示报表的汇总信息VO
	 */
	protected InterestReportSumData generateReportSummary(InterestViewItemSum sumData) {

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
		
		sum.setSummaryText(this.getText("download.summary.it",
				new String[] { String.valueOf(sumData.getInterestCount()) }));// 添加“合计”
		return sum;
	}
	
	/**
	 * 生成兴趣报告的汇总数据
	 * @param infoData
	 * @return
	 */
	private InterestViewItemSum generateInterestViewSum(List<InterestAssistant> infoData){
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
	 * 判断此次请求是否有过滤条件
	 * @return
	 */
	protected boolean hasFilter() {
		return qp.isBoolValue() || !StringUtils.isEmpty(qp.getKeyword()) || qp.hasStatField4Filter();
	}
	
	protected boolean hasIT(int targetType){
		return TargettypeUtil.hasIT(targetType);
	}
	/**
	 * 获取前端显示VO的List方法
	 * @param infoData
	 * @return
	 */
	private List<InterestViewItem> pagerInterestViewItemList (List<InterestAssistant> infoData) {
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
	 * isFiltered:判断是否将当前记录过滤
	 *
	 * @param item 待判断VO对象
	 * @return  true表示要过滤，false表示保留    
	 */
	protected boolean isFiltered(InterestAssistant item) {
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
	
	/****************************************action方法***********************************/
	
	/**
	 * 批量删除当前列表全部自选的IT定向关系
	 */
	public String deleteAllIt() {
		
		try {
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);
			return ERROR;
		}
		
		ItRpcQueryParam param = new ItRpcQueryParam();
		
		Map<Integer,List<Integer>> groupInterestIds = new HashMap<Integer, List<Integer>>();
		Map<Long,List<Integer>> packInterestIds = new HashMap<Long, List<Integer>>();
		if (hasFilter()) {// 有过滤的话，构造查询对象放入memcache里
			List<InterestAssistant> infoData;
			infoData = generateChosenInterestList(false);
			for (InterestAssistant item : infoData) {
				Integer groupId = item.getSelfInterest().getGroupId();
				Long gpId = item.getSelfInterest().getGpId();
				
				if (gpId == null || gpId == 0) {
					ArrayList<Integer> interestIds = (ArrayList<Integer>) groupInterestIds.get(groupId);
					if (interestIds == null) {
						interestIds = new ArrayList<Integer>();
						interestIds.add(item.getSelfInterestId());
						groupInterestIds.put(groupId, interestIds);
					} else {
						interestIds.add(item.getSelfInterestId());
						groupInterestIds.put(groupId, interestIds);
					}
				} else {
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
			}
			
			param.setGroupInterestIds(groupInterestIds);
			param.setPackInterestIds(packInterestIds);
		} else {// 无过滤的话，不作处理，只转发
			token = "";
			// 无过滤，按groupid,planid,userid进行删除
			Integer groupId = qp.getGroupId();
			Integer planId = qp.getPlanId();
			if (groupId == null) {
				groupId = 0;
			}
			if (planId == null) {
				planId = 0;
			}
			param.setUserId(qp.getUserId());
			param.setPlanId(planId);
			param.setGroupId(groupId);
			
		}

		token = TokenUtil.generateToken();
		BeidouCacheInstance.getInstance().memcacheRandomSet(token, param, TOKEN_EXPIRE);
		return SUCCESS;
	}
		
	/**
	 * 处理有展现兴趣列表的action方法
	 * @return
	 */
	public String ajaxListShownInterest(){		

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
		infoData = generateShownInterestList(false);
		
		// 3、获取总记录数（兴趣组合+一级兴趣点）
		count = infoData.size();
		
		// 4、排序
		Collections.sort(infoData, new GroupItComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		InterestViewItemSum sumData = generateInterestViewSum(infoData);

		// 6、生成显示的InterestViewItem列表
		List<InterestViewItem> list = this.pagerInterestViewItemList(infoData);
		
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
	 * 处理有展现兴趣列表的分日图表的action方法
	 * @return
	 */
//	public String ajaxListShownInterestForFlash(){	
//
//		//1、初始化日期
//		try {
//			super.initParameterForAtLestServenDay();
//		} catch (Exception e) {
//			log.warn(e.getMessage(), e);
//			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_SUCCESS);
//			jsonObject.addData("state",
//					ReportWebConstants.FLASH_DATA_STATE.UNNORMAL);
//			jsonObject.addData("data", "");
//			return SUCCESS;
//		}
//
//		Map<String, Map<String, Object>> statData = getShownInterestStatForFlash();//天粒度的汇总数据,key为时间，value为统计值
//		
//		String xml = generateXml(statData);
//		jsonObject.addData("state", ReportWebConstants.FLASH_DATA_STATE.NORMAL);
//		jsonObject.addData("data", xml);
//
//		return SUCCESS;
//	}
	
	/**
	 * 下载关键词报告Action方法
	 */
	public String downloadShownInterest() throws IOException {

		// 1、参数初始化
		super.initParameter();
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}
		List<InterestAssistant> infoData;// 目标结果集（可能是全集或者分页集）

		// 2、获取VO列表
		infoData = generateShownInterestList(false);
		
		// 3、获取总记录数
		count = infoData.size();
		
		// 4、排序
		Collections.sort(infoData, new GroupItComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		InterestViewItemSum sumData = generateInterestViewSum(infoData);
		
		// 6、处理转化数据和UV数据
//		if (isOnlyTodayFlag) {
//			this.clearNotRealtimeStatForIt(infoData, sumData);
//		} else {
			this.postHandleTransAndUvData(userId, from, to, infoData, sumData);
//		}

		// 7、构造报告VO
		InterestReportVo vo = new InterestReportVo(qp.getLevel());//报表下载使用的VO
		vo.setAccountInfo(generateAccountInfo(user.getUsername()));
		vo.setDetails(infoData);
		vo.setHeaders(generateReportHeader());
		vo.setSumData(generateReportSummary(sumData));
		vo.setInterestMap(interestMgr.getInterestMap());
		
		// 8、构造输出流
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(vo, output);

		// 9、设置下载需要使用到的一些属性
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
		fileName += this.getText("download.it.filename.prefix");
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
	 * 自选兴趣列表action方法
	 * @return
	 */
	public String ajaxListChosenInterest(){		

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
		infoData = generateChosenInterestList(false);
		
		// 3、获取总记录数
		count = infoData.size();
		
		// 4、排序
		Collections.sort(infoData, new GroupItComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		InterestViewItemSum sumData = generateInterestViewSum(infoData);

		// 6、生成显示的VOList
		List<InterestViewItem> list = this.pagerInterestViewItemList(infoData);
		
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
	 * 自选兴趣分日图表action方法
	 * @return
	 */
//	public String ajaxListChosenInterestForFlash(){	
//
//		try {
//			super.initParameterForAtLestServenDay();
//		} catch (Exception e) {
//			log.warn(e.getMessage(), e);
//			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_SUCCESS);
//			jsonObject.addData("state",
//					ReportWebConstants.FLASH_DATA_STATE.UNNORMAL);
//			jsonObject.addData("xml", "");
//			return SUCCESS;
//		}
//
//		Map<String, Map<String, Object>> statData = getChosenInterestStatForFlash();// 时间粒度：天
//
//		String xml = generateXml(statData);
//		jsonObject.addData("state", ReportWebConstants.FLASH_DATA_STATE.NORMAL);
//		jsonObject.addData("data", xml);
//
//		return SUCCESS;
//	}
	
	
	/**
	 * 下载自选兴趣列表action方法
	 */
	public String downloadChosenInterest() throws IOException {

		// 1、参数初始化
		super.initParameter();
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}
		List<InterestAssistant> infoData;// 目标结果集（可能是全集或者分页集）

		// 2、获取VO列表
		infoData = generateChosenInterestList(false);
		
		// 3、获取总记录数
		count = infoData.size();
		
		// 4、排序
		Collections.sort(infoData, new GroupItComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		InterestViewItemSum sumData = generateInterestViewSum(infoData);
		
		// 6、处理转化数据和UV数据
//		if (isOnlyTodayFlag) {
//			this.clearNotRealtimeStatForIt(infoData, sumData);
//		} else {
			this.postHandleTransAndUvData(userId, from, to, infoData, sumData);
//		}

		// 7、构造报告VO
		InterestReportVo vo = new InterestReportVo(qp.getLevel());// 报表下载使用的VO
		vo.setAccountInfo(generateAccountInfo(user.getUsername()));
		vo.setDetails(infoData);
		vo.setHeaders(generateReportHeader());
		vo.setSumData(generateReportSummary(sumData));
		vo.setInterestMap(interestMgr.getInterestMap());
		
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
		fileName += this.getText("download.it.filename.prefix");
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
	 * 生成用于兴趣报告前端显示的VO
	 * @param forFlash 是否用于flash显示
	 * @return 
	 */
	private List<InterestAssistant> generateShownInterestList(boolean forFlash){
		List<InterestAssistant> interestAssistantList = new ArrayList<InterestAssistant>();
		
		// 校验userId信息
		Integer userId = qp.getUserId();
		if (userId == null || userId <= 0) {
			return interestAssistantList;
		}
		
		// 1、查询当前层级下所有有展现的IT，包括已经删除的兴趣点和兴趣组合
		List<InterestViewItem> allData = this.getStatList();
					
		// 2、将兴趣点和兴趣组合记录放入map中，供过滤使用，并获取List<grouid,List<iid>>，待传给ITFacade
		Map<GroupPackInterestKey, Map<Integer, InterestViewItem>> iidsMap = new HashMap<GroupPackInterestKey, Map<Integer, InterestViewItem>>();
		Map<GroupPackInterestKey, List<Integer>> shownInterestMap = new HashMap<GroupPackInterestKey, List<Integer>>();	
		
		if (!CollectionUtils.isEmpty(allData)) {
			List<Integer> refPackIdList = new ArrayList<Integer>();
			
			for (InterestViewItem stat : allData) {
				Integer iid = stat.getInterestId();
				Long gpId = stat.getGpId();
				Integer groupId = stat.getGroupId();
				Integer refpackId = stat.getRefpackId();
				GroupPackInterestKey groupPackKey = new GroupPackInterestKey(groupId, gpId);
				
				// 记录gpId，以及refpackId
				if (gpId != 0 && refpackId != 0) {
					refPackIdList.add(refpackId);
				}
				
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
			
			
			Map<Integer, Map<String, Object>> packInfoMap = groupPackFacade.getPackNameByRefPackId(userId, refPackIdList);
			Map<Integer, String> packNameMapping = new HashMap<Integer, String>();
			Map<Integer, Integer> packTypeMapping = new HashMap<Integer, Integer>();
			for (Integer id : packInfoMap.keySet()) {
				Map<String, Object> packInfo = packInfoMap.get(id);
				if (packInfo != null) {
					packNameMapping.put(id, (String)packInfo.get("name"));
					packTypeMapping.put(id, (Integer)packInfo.get("type"));
				}
			}
			
			// 通过CproItFacade接口，获得具有父子关系的有展现列表的兴趣树
			Map<GroupPackInterestKey, List<InterestVo4Report>> interestVoMap 
					= cproITFacade.querySrchsInterest(shownInterestMap, userId);

			// 遍历CproItFacade返回的兴趣树列表，构造report显示VOList并过滤
			if(!interestVoMap.isEmpty()){
				InterestViewItem stat;				
				for (GroupPackInterestKey groupPackId : interestVoMap.keySet()){
					List<InterestVo4Report> interestVoList = interestVoMap.get(groupPackId);
					
					for(InterestVo4Report interestVo: interestVoList){
						Long gpId = interestVo.getGpId();
						InterestAssistant interestAssistant = null;
						InterestViewItem viewItem = new InterestViewItem();				
						stat = iidsMap.get(groupPackId).get(interestVo.getId());
						viewItem.setInterestId(interestVo.getId());
						viewItem.setGroupId(interestVo.getGroupId());
						viewItem.setViewState(interestVo.getViewState());
						viewItem.setPlanId(interestVo.getPlanId());
						viewItem.setGpId(gpId);
						viewItem.setInterestName(interestVo.getName());
						if(!forFlash){
							viewItem.setGroupName(interestVo.getGroupName());
							viewItem.setPlanName(interestVo.getPlanName());
						}
						viewItem.fillStatRecord(stat);
						interestAssistant = new InterestAssistant((Integer)interestVo.getId());
						
						boolean isInterest = false;
						if (interestVo.getId() <= InterestConstant.MAX_INTEREST_ID) {
							viewItem.setType(ReportWebConstants.INTEREST_TYPE_FIRST);
							viewItem.setOrderId(interestVo.getOrderId());
							interestAssistant.setOrderId(interestVo.getOrderId());
							isInterest = true;
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
						
						Integer refpackId = 0;
						String packName = null;
						Integer packType = null;
						// 构造子兴趣节点
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
							
							// 设置受众组合名称
							// 获取refPackId，如果未曾获取到refPackId（为null），则获取一次并设置；packName也是同样的
							if (refpackId == 0 && stat != null && stat.getRefpackId() != null) {
								refpackId = stat.getRefpackId();
							}
							if (packName == null) {
								packName = packNameMapping.get(refpackId);
								packType = packTypeMapping.get(refpackId);
							}
							
							if (refpackId == 0 || packName == null) {
								childViewItem.setPackName("");
								childViewItem.setPackType(PackTypeConstant.TYPE_INTEREST);
							} else {
								childViewItem.setPackName(packName);
								childViewItem.setPackType(packType);
							}
							
							interestAssistant.addSecondInterest(childViewItem);
						}
						
						// 设置受众组合名称
						// 存在如下情况：统计数据存在二级兴趣点数据，而返回前端数据需要已经兴趣点数据，此时refPackId使用其二级兴趣点的
						if (refpackId == 0 && stat != null && stat.getRefpackId() != null) {
							refpackId = stat.getRefpackId();
						}
						if (packName == null) {
							packName = packNameMapping.get(refpackId);
							packType = packTypeMapping.get(refpackId);
						}
						if (isInterest) {
							if (refpackId == 0 || packName == null) {
								viewItem.setPackName("");
								viewItem.setPackType(PackTypeConstant.TYPE_INTEREST);
							} else {
								viewItem.setPackName(packName);
								viewItem.setPackType(packType);
							}
						} else {
							if (refpackId == 0 || packName == null) {
								viewItem.setPackName(viewItem.getInterestName());
								viewItem.setPackType(PackTypeConstant.TYPE_INTEREST_PACK);
							} else {
								viewItem.setPackName(packName);
								viewItem.setPackType(packType);
							}
						}
						
						interestAssistant.ensureStatFields();
						if(!isFiltered(interestAssistant))
							interestAssistantList.add(interestAssistant);
					}
				}
			}
		}		
		return interestAssistantList;
	}
	
	private List<InterestViewItem> getStatList() {
		List<Map<String, Object>> allData = null;
		
		List<InterestViewItem> olapList = itStatMgr.queryGroupItData(userId, planIds, groupIds, null, 
				null, null, from, to, null, 0, ReportConstants.TU_NONE);
		
		List<Map<String, Object>> uvData = uvDataService.queryITData(userId, planIds, groupIds, 
				null, null, null, null, from, to, null, 0, ReportConstants.TU_NONE, 
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
					planIds, groupIds, null, null, null, null, from, to, null, 0,
					ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
			// 获取holmes数据
			List<Map<String, Object>> holmesData = holmesDataService.queryITData(userId, null, null, 
					planIds, groupIds, null, null, null, null, from, to, null, 0,
					ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);

			// Merge基本统计/UV数据与转化数据/holmes数据
			allData = this.reportFacade.mergeTransHolmesAndUvDataByMulitKey(Constant.DorisDataType.STAT, 
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
	 * 生成自选兴趣报告前端显示的VO
	 * @param forFlash 是否用于flash显示
	 * @return 
	 */
	private List<InterestAssistant> generateChosenInterestList(boolean forFlash){
		List<InterestAssistant> interestAssistantList = new ArrayList<InterestAssistant>();
		
		// 校验userId信息
		Integer userId = qp.getUserId();
		if (userId == null || userId <= 0) {
			return interestAssistantList;
		}
		
		// 1、查询当前层级下的所有推广组ids	
		List<Integer> groupids = new ArrayList<Integer>();
		QueryParameter groupQp = new QueryParameter();
		groupQp.setUserId(qp.getUserId());
		groupQp.setPlanId(qp.getPlanId());
		groupQp.setGroupId(qp.getGroupId());
		if(qp.getGroupId() != null){
			List<Long> ids = new ArrayList<Long>();
			ids.add(Long.valueOf(qp.getGroupId()));
			groupQp.setIds(ids);
		}	
		groupids = this.reportGroupMgr.findAllCproGroupIdsByQuery(groupQp, false);
		
		// 根据各层级查询受众组合关联的兴趣
		List<PackITVo> packITVoList = groupPackFacade.findPackITByLevel(qp.getUserId(), qp.getPlanId(), qp.getGroupId());
		Map<GroupPackInterestKey, List<Integer>> packInterestMap = new HashMap<GroupPackInterestKey, List<Integer>>();
		Map<Long, String> packNameMapping = new HashMap<Long, String>();
		Map<Long, Integer> packTypeMapping = new HashMap<Long, Integer>();
		for (PackITVo packITVo : packITVoList) {
			GroupPackInterestKey key = new GroupPackInterestKey(packITVo.getGroupId(), packITVo.getGpId());
			
			List<Integer> value = packInterestMap.get(key);
			if (CollectionUtils.isEmpty(value)) {
				value = new ArrayList<Integer>();
				packInterestMap.put(key, value);
			}
			value.add(packITVo.getIid());
			
			packNameMapping.put(packITVo.getGpId(), packITVo.getPackName());
			packTypeMapping.put(packITVo.getGpId(), packITVo.getPackType());
		}
		
		// 2 、通过当前层级下的所有推广组ids查询用户在当前层级下的所有自选的兴趣点和兴趣组合列表
		Map<GroupPackInterestKey, List<InterestVo4Report>> interestVoMap = new HashMap<GroupPackInterestKey, List<InterestVo4Report>>();
		interestVoMap = cproITFacade.queryGroupInterest(groupids, packInterestMap, userId);
 		
		if(interestVoMap.size() != 0){
			hasInterest = true;
		}

		if(!interestVoMap.isEmpty()){//如果在当前层级存在自选兴趣
			// 存放待查询受众组合是否为已优化的gpIds
			List<Long> packOptimizedGpIdList = new ArrayList<Long>();
			
			// 3、查询当前层级下所有有展现的IT，包括已经删除的兴趣点和兴趣组合
			List<InterestViewItem> allData = this.getStatList();
			
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
				for (InterestVo4Report interestVo : interestVoList) {
					Long gpId = interestVo.getGpId();
					
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
					viewItem.setGpId(gpId);
					viewItem.setInterestName(interestVo.getName());
					if(!forFlash){
						viewItem.setGroupName(interestVo.getGroupName());
						viewItem.setPlanName(interestVo.getPlanName());
					}
					viewItem.setType(ReportWebConstants.INTEREST_TYPE_FIRST);				
					interestAssistant = new InterestAssistant((Integer)interestVo.getId());
					
					// 设置gpId
					if (gpId != null && gpId > 0) {
						packOptimizedGpIdList.add(gpId);
					}
					
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
					
					// 设置受众组合类型
					Integer packType = null;
					if (null != gpId && gpId != 0) {
						packType = packTypeMapping.get(gpId);
						
					}
					if (packType != null) {
						viewItem.setPackType(packType);
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
						
						// 设置受众组合名称
						if (packName == null) {
							childViewItem.setPackName("");
						} else {
							childViewItem.setPackName(packName);
						}
						if (packType != null) {
							childViewItem.setPackType(packType);
						}
						
						interestAssistant.addSecondInterest(childViewItem);
					}
					interestAssistant.ensureStatFields();
					if (!isFiltered(interestAssistant))
						interestAssistantList.add(interestAssistant);
				}
			}
			
			this.fillPackOptimized(interestAssistantList, packOptimizedGpIdList);
		}		
		return interestAssistantList;
	}
	
	/**
	 * fillPackOptimized: 向结果集中添加是否已优化的标记
	 * @version beidou-api 3 plus
	 * @author genglei01
	 * @date 2012-9-24
	 */
	private void fillPackOptimized(List<InterestAssistant> infoData, List<Long> packOptimizedGpIdList) {
		Map<Long, Boolean> packOptimizedMap = groupPackFacade.checkPackOptimizedByGpIds(packOptimizedGpIdList);
		
		for (InterestAssistant item : infoData) {
			Long gpId = item.getSelfInterest().getGpId();
			if (gpId == null || gpId == 0) {
				continue;
			}
			
			Boolean flag = packOptimizedMap.get(gpId);
			if (flag == null || !flag) {
				continue;
			}
			
			// 设置已优化标记
			item.getSelfInterest().setHasModified(true);
			for (InterestViewItem childItem : item.getInterestViewItem()) {
				childItem.setHasModified(true);
			}
		}
	}
	
	public CproITFacade getCproITFacade() {
		return cproITFacade;
	}

	public void setCproITFacade(CproITFacade cproITFacade) {
		this.cproITFacade = cproITFacade;
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

	public ReportCproGroupMgr getReportGroupMgr() {
		return reportGroupMgr;
	}

	public void setReportGroupMgr(ReportCproGroupMgr reportGroupMgr) {
		this.reportGroupMgr = reportGroupMgr;
	}

	public int getTOKEN_EXPIRE() {
		return TOKEN_EXPIRE;
	}

	public void setTOKEN_EXPIRE(int tOKENEXPIRE) {
		TOKEN_EXPIRE = tOKENEXPIRE;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
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

	public boolean isHasInterest() {
		return hasInterest;
	}

	public void setHasInterest(boolean hasInterest) {
		this.hasInterest = hasInterest;
	}

	public void setGroupPackFacade(GroupPackFacade groupPackFacade) {
		this.groupPackFacade = groupPackFacade;
	}

	public InterestMgr getInterestMgr() {
		return interestMgr;
	}

	public void setInterestMgr(InterestMgr interestMgr) {
		this.interestMgr = interestMgr;
	}
	
	/*********getters and setters **********/
}

