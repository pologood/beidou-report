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
import org.apache.commons.lang.ArrayUtils;

import com.baidu.beidou.cprogroup.bo.CproGroup;
import com.baidu.beidou.cprogroup.facade.GroupPackFacade;
import com.baidu.beidou.cprogroup.service.CproGroupMgr;
import com.baidu.beidou.cprogroup.util.KTKeywordUtil;
import com.baidu.beidou.cprogroup.vo.GroupPackVo;
import com.baidu.beidou.cproplan.bo.CproPlan;
import com.baidu.beidou.cproplan.service.CproPlanMgr;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.olap.constant.Constants;
import com.baidu.beidou.olap.service.PackStatService;
import com.baidu.unbiz.olap.util.ReportUtils;
import com.baidu.beidou.olap.vo.GroupPackViewItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.report.util.ReportUtil;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.group.GroupPackReportSumData;
import com.baidu.beidou.report.vo.group.GroupPackReportVo;
import com.baidu.beidou.report.vo.group.GroupPackViewItemSum;
import com.baidu.beidou.stat.driver.bo.Constant;
import com.baidu.beidou.tool.util.TransReportHelper;
import com.baidu.beidou.tool.vo.TempSitesAndTrans;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.LogUtils;
import com.baidu.beidou.util.StringUtils;
import com.baidu.beidou.util.TokenUtil;
import com.baidu.beidou.util.memcache.BeidouCacheInstance;
import com.baidu.beidou.util.vo.GroupPackRpcQueryParam;

public class ListCproGroupPackAction extends BeidouReportActionSupport {

	private static final long serialVersionUID = 3129120854753785465L;
	
	@Resource(name="packStatServiceImpl")
	private PackStatService packStatMgr;
	
	private CproPlanMgr cproPlanMgr = null;
	private CproGroupMgr cproGroupMgr = null;	
	private GroupPackFacade groupPackFacade = null;
	
	/** 下载报表的列数 */
	public static final int DOWN_REPORT_QT_COL_NUM = 22;
	/** 结果集（全集）总行数 */
	int count;
	
	private List<Integer> planIds = new ArrayList<Integer>();
	private List<Integer> groupIds = new ArrayList<Integer>();
	private List<Integer> wordIds = new ArrayList<Integer>();
	private Integer planId;
	private Integer groupId;
	private int orient ;// 排序方向
	
	private int TOKEN_EXPIRE = 60;
	private String token;
	private String beidouBasePath;
	
	private boolean hasPack;//当前层级是否含有受众组合
	
//	private boolean isMixTodayAndBeforeFlag;	// 当前查询是否混合今天和过去日期
//	private boolean isOnlyTodayFlag;	// 当前查询是否只包含今天
	
	/**
	 * Pack比较器
	 */
	class CproPackComparator implements Comparator<GroupPackViewItem>  {

		int order;
		String col;
		public CproPackComparator(String col, int order) {
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
		
		public int compare(GroupPackViewItem o1, GroupPackViewItem o2) {
			if (o1 == null) {
				return -1 * order;
			}
			if (o2 == null) {
				return 1 * order;
			}
			
			if (ReportConstants.PACKNAME.equals(col)) {
				return compareByString(o1.getPackName(), o2.getPackName());
			}
			
			if (ReportWebConstants.FRONT_BACKEND_ORDERNAME_PLANNAME.equalsIgnoreCase(col)) {
				return compareByString(o1.getPlanName(), o2.getPlanName());
			}
			if (ReportWebConstants.FRONT_BACKEND_ORDERNAME_GROUPNAME.equalsIgnoreCase(col)) {
				return compareByString(o1.getGroupName(), o2.getGroupName());
			}
			
			if (ReportConstants.PACKTYPE.equals(col)) {
				return order * (int) (o1.getPackType() - o2.getPackType());
			}
			
			if (ReportConstants.PRICE.equals(col)) {
				Double b1 = o1.getPrice();
				Double b2 = o2.getPrice();
				if (b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}

			if (ReportConstants.SRCHS.equals(col)) {
				if ((o1.getSrchs() - o2.getSrchs()) == 0) {
					return (int) (o1.getSrchs() - o2.getSrchs());
				} else {
					return order * (int) (o1.getSrchs() - o2.getSrchs());
				}
			}

			if (ReportConstants.SRCHUV.equals(col)) {
				return order * (int) (o1.getSrchuv() - o2.getSrchuv());
			}

			if (ReportConstants.SRSUR.equals(col)) {
				BigDecimal b1 = o1.getSrsur();
				BigDecimal b2 = o2.getSrsur();
				if (b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}

			if (ReportConstants.CLKS.equals(col)) {
				return order * (int) (o1.getClks() - o2.getClks());
			}

			if (ReportConstants.CLKUV.equals(col)) {
				return order * (int) (o1.getClkuv() - o2.getClkuv());
			}

			if (ReportConstants.CTR.equals(col)) {
				BigDecimal b1 = o1.getCtr();
				BigDecimal b2 = o2.getCtr();
				if (b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if (ReportConstants.CUSUR.equals(col)) {
				BigDecimal b1 = o1.getCusur();
				BigDecimal b2 = o2.getCusur();
				if (b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}

			if (ReportConstants.ACP.equals(col)) {
				BigDecimal b1 = o1.getAcp();
				BigDecimal b2 = o2.getAcp();
				if (b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if (ReportConstants.COCUR.equals(col)) {
				BigDecimal b1 = o1.getCocur();
				BigDecimal b2 = o2.getCocur();
				if (b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}

			if (ReportConstants.CPM.equals(col)) {
				BigDecimal b1 = o1.getCpm();
				BigDecimal b2 = o2.getCpm();
				if (b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if (ReportConstants.COST.equals(col)) {
				if (o1.getCost() > o2.getCost()) {
					return order * 1;
				} else if (o1.getCost() < o2.getCost()) {
					return order * -1;
				} else {
					return 0;
				}
			}
			
			if (ReportConstants.ARRIVAL_RATE.equals(col)) {
				BigDecimal b1 = o1.getArrivalRate();
				BigDecimal b2 = o2.getArrivalRate();
				if (b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}

			if (ReportConstants.HOP_RATE.equals(col)) {
				BigDecimal b1 = o1.getHopRate();
				BigDecimal b2 = o2.getHopRate();
				if (b1 == null) {
					return -1 * order;
				}
				if (b2 == null) {
					return order;
				}
				return order * b1.compareTo(b2);
			}
			
			if (ReportConstants.RES_TIME_STR.equals(col)) {
				return order * o1.getResTimeStr().compareTo(o2.getResTimeStr());
			}
			
			if (ReportConstants.DIRECT_TRANS_CNT.equals(col)) {
				return order * (int) (o1.getDirectTrans() - o2.getDirectTrans());
			}
			
			if (ReportConstants.INDIRECT_TRANS_CNT.equals(col)) {
				return order * (int) (o1.getIndirectTrans() - o2.getIndirectTrans());
			}
			return order;
		}			
	}

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
		if (QueryParameterConstant.SortOrder.SORTORDER_DESC.equalsIgnoreCase(qp.getOrder())) {
			orient = ReportConstants.SortOrder.DES;
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
	 * 批量删除当前列表全部自选的受众组合关系
	 */
	public String deleteAllPack() {
		
		try {
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);
			return ERROR;
		}
		
		GroupPackRpcQueryParam param = new GroupPackRpcQueryParam();
		
		List<Long> packIds = new ArrayList<Long>();
		if (hasFilter()) {// 有过滤的话，构造查询对象放入memcache里
			List<GroupPackViewItem> infoData;
			infoData = getViewItems(false);
			for (GroupPackViewItem item : infoData) {
				Long gpId = item.getGpId();
				packIds.add(gpId);
			}
			param.setUserId(qp.getUserId());
			param.setGpIds(packIds);
			
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
	 * 关键词列表Action方法
	 */
	public String ajaxList() {
		// 1、参数初始化
		try {
			super.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return SUCCESS;
		}

		List<GroupPackViewItem> infoData;// 目标结果集（可能是全集或者分页集）

		// 2、获取VO列表
		infoData = getViewItems(false);
		
		// 3、查询总条数，该数字可能不是最终值，因为有可能需要按统计值过滤，这样的话该值会被再次修改
		count = infoData.size();
		
		// 4、排序
		Collections.sort(infoData, new CproPackComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		GroupPackViewItemSum sumData = calculateSumData(infoData);

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
		jsonObject.addData("cache", cache ? ReportConstants.Boolean.TRUE
				: ReportConstants.Boolean.FALSE);
		jsonObject.addData("hasPack", hasPack);
		jsonObject.addData("sum", sumData);
		
//		if (isOnlyTodayFlag) {
//			this.clearNotRealtimeStat(infoData, sumData);
//		} else {
			this.reportFacade.postHandleTransAndUvData(userId, from, to, infoData, sumData);
//		}

		return SUCCESS;
	}
	
	/**
	 * 下载受众组合报告Action方法
	 */
	public String download() throws IOException {

		// 1、初始化一下参数
		super.initParameter();
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}

		// 下载的CSV共有四部分
		// 1、账户基本信息，2、列头，3、列表，4、汇总信息

		List<GroupPackViewItem> infoData = null;// 目标plan集
		
		// 2、获取统计数据(不分页)
		infoData = getViewItems(false);

		// 3、查询总条数，该数字可能不是最终值，因为有可能需要按统计值过滤，这样的话该值会被再次修改
		count = infoData.size();
		
		// 4、排序
		Collections.sort(infoData, new CproPackComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		GroupPackViewItemSum sumData = calculateSumData(infoData);
		
		// 6、处理转化数据和UV数据
//		if (isOnlyTodayFlag) {
//			this.clearNotRealtimeStat(infoData, sumData);
//		} else {
			this.reportFacade.postHandleTransAndUvData(userId, from, to, infoData, sumData);
//		}

		GroupPackReportVo vo = new GroupPackReportVo(qp.getLevel());// 报表下载使用的VO
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
		fileName += this.getText("download.pack.filename.prefix");
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
	
	private List<GroupPackViewItem> getViewItems(boolean forFlashXml) {
		// 校验userId信息
		Integer userId = qp.getUserId();
		if (userId == null || userId <= 0) {
			return new ArrayList<GroupPackViewItem>();
		}
		
		// 1、查询基本统计数据和UV数据，但不排序
		List<GroupPackViewItem> allData = this.getPackList();
		
		// 2、获取当前层级的所有受众组合列表
		List<GroupPackVo> groupPackList = groupPackFacade.findGroupPackVoByLevel(userId, qp.getPlanId(), qp.getGroupId());
		hasPack = CollectionUtils.isNotEmpty(groupPackList);
		Map<Long, GroupPackVo> groupPackMap = GroupPackVo.list2Map(groupPackList);
		// 获取各受众组合单独出价
		Map<Long, Integer> groupPackPriceMap = GroupPackVo.list2MapForPackPrice(groupPackList);
		
		if (forFlashXml && CollectionUtils.isEmpty(allData)) {
			// 如果是统计flash使用的，且没有统计数据，则直接返回空
			return new ArrayList<GroupPackViewItem>();
		}
		
		Set<Integer> planIds = new HashSet<Integer>();
		Set<Integer> groupIds = new HashSet<Integer>();
		for (GroupPackVo pack : groupPackList) {
			planIds.add(pack.getPlanId());
			groupIds.add(pack.getGroupId());
		}
		for (GroupPackViewItem stat : allData ) {
			planIds.add(stat.getPlanId());
			groupIds.add(stat.getGroupId());
		}
		
		// 3、根据groupIds和planIds查找对应的推广组和推广计划列表
		Map<Integer, String> planIdNameMapping = cproPlanMgr.findPlanNameByPlanIds(planIds);
		Map<Integer, String> groupIdNameMapping = cproGroupMgr.findGroupNameByGroupIds(groupIds);
		
		// 获取推广组出价
		List<CproGroup> groupList = cproGroupMgr.findCproGroupByGroupIds(groupIds);
		Map<Integer, Integer> groupPriceMap = this.getGroupPrice(groupList);
		
		// 4、组装数据并且过滤，先以mysql表为基准获得已删除受众组合，若没被删除，则构造VO对象
		//   通过ATOM反差已经删除的受众组合，并构造VO对象
		//   添加没有doris统计数据的受众组合，并构造VO对象
		List<Long> packOptimizedGpIdList = new ArrayList<Long>();
		List<GroupPackViewItem> infoData = new ArrayList<GroupPackViewItem>();
		if (!CollectionUtils.isEmpty(allData)) {
			// 如果有统计数据则进行后续的数据合并操作；
			Integer statPlanId;
			Integer statGroupId;
			Long gpId;
			Integer refPackId;
			GroupPackVo groupPack;
			
			// 存放已删除但有统计信息的受众统计信息，key为gpId，value为统计信息
			Map<Long, GroupPackViewItem> deletedPackMap = new HashMap<Long, GroupPackViewItem>();
			Set<Integer> deletedRefpackIds = new HashSet<Integer>();
			
			// 遍历统计数据，如果相应受众数据库中不存在则放入删除Map，待后续处理
			for (GroupPackViewItem stat : allData) {
				try {
					statPlanId = Integer.valueOf(stat.getPlanId());
					statGroupId = Integer.valueOf(stat.getGroupId());
					gpId = Long.valueOf(stat.getGpId());
					refPackId = Integer.valueOf(stat.getRefPackId());
				} catch (Exception e) {
					log.error("error to digest doris data[" + stat + "]",e);
					continue;
				}
				
				groupPack = groupPackMap.remove(gpId);
				if (groupPack == null) {
					// 如果受众组合关系已被刪除，则记录之，以备后续查数据库
					// 删除的受众组合不做已优化的判断
					deletedPackMap.put(gpId, stat);
					deletedRefpackIds.add(refPackId);
				} else {
					// 没有删除则生成待显示的VO对象
					GroupPackViewItem view = new GroupPackViewItem();
					view.fillStatRecord(stat);
					view.setGpId(gpId);
					view.setPlanId(statPlanId);
					view.setPlanName(planIdNameMapping.get(statPlanId));
					view.setGroupId(statGroupId);
					view.setGroupName(groupIdNameMapping.get(statGroupId));
					view.setPackType(groupPack.getType());
					view.setPackName(groupPack.getName());
					
					// 记录gpId，以便获取是否为已优化
					packOptimizedGpIdList.add(gpId);
					
					// 设置出价为受众组合单独出价，如无则为推广组出价
					Integer packPrice = groupPackPriceMap.get(gpId);
					double groupPriceDouble = groupPriceMap.get(statGroupId) * 1.0 / 100;
					if (packPrice == null) {
						view.setPrice(groupPriceDouble);
						view.setSpecialPrice(false);
					} else {
						double packPriceDouble = packPrice * 1.0 / 100;
						view.setPrice(packPriceDouble);
						view.setOriPrice(groupPriceDouble);
						view.setSpecialPrice(true);
					}
					
					if (!isFiltered(view)) {
						infoData.add(view);
					}
				}
			}
			
			// 处理已删除受众组合
			if (!deletedPackMap.isEmpty()) {
				List<Integer> refPackIdList = new ArrayList<Integer>(deletedRefpackIds);
				Map<Integer, Map<String, Object>> packInfoMap = groupPackFacade.getPackNameByRefPackId(userId, refPackIdList);
				
				for (GroupPackViewItem stat : deletedPackMap.values()) {
					try {
						statPlanId = stat.getPlanId();
						statGroupId = stat.getGroupId();
						gpId = stat.getGpId();
						refPackId = stat.getRefPackId();
					} catch (Exception e) {
						log.error("error to digest doris data[" + stat + "]",e);
						continue;
					}
					
					// 没有删除则生成待显示的VO对象
					GroupPackViewItem view = new GroupPackViewItem();
					view.fillStatRecord(stat);
					view.setGpId(gpId);
					view.setPlanId(statPlanId);
					view.setPlanName(planIdNameMapping.get(statPlanId));
					view.setGroupId(statGroupId);
					view.setGroupName(groupIdNameMapping.get(statGroupId));
					Map<String, Object> packInfo = packInfoMap.get(refPackId);
					if (packInfo != null) {
						view.setPackType((Integer)packInfo.get("type"));
						view.setPackName((String)packInfo.get("name"));
					}
					view.setHasDel(true);
					
					// 设置出价为受众组合单独出价，如无则为推广组出价
					Integer packPrice = groupPackPriceMap.get(gpId);
					double groupPriceDouble = groupPriceMap.get(statGroupId) * 1.0 / 100;
					if (packPrice == null) {
						view.setPrice(groupPriceDouble);
						view.setSpecialPrice(false);
					} else {
						double packPriceDouble = packPrice * 1.0 / 100;
						view.setPrice(packPriceDouble);
						view.setOriPrice(groupPriceDouble);
						view.setSpecialPrice(true);
					}
					
					if (!isFiltered(view)) {
						infoData.add(view);
					}
				}
			}
			
			// 添加剩下的没有统计数据的受众组合
			for (GroupPackVo groupPackVo : groupPackMap.values()) {
				// 没有删除则生成待显示的VO对象
				GroupPackViewItem view = new GroupPackViewItem();
				view.setGpId(groupPackVo.getGpId());
				view.setPlanId(groupPackVo.getPlanId());
				view.setPlanName(planIdNameMapping.get(groupPackVo.getPlanId()));
				view.setGroupId(groupPackVo.getGroupId());
				view.setGroupName(groupIdNameMapping.get(groupPackVo.getGroupId()));
				view.setPackType(groupPackVo.getType());
				view.setPackName(groupPackVo.getName());
				
				// 记录gpId，以便获取是否为已优化
				packOptimizedGpIdList.add(view.getGpId());
				
				// 设置出价为受众组合单独出价，如无则为推广组出价
				Integer packPrice = groupPackPriceMap.get(groupPackVo.getGpId());
				double groupPriceDouble = groupPriceMap.get(groupPackVo.getGroupId()) * 1.0 / 100;
				if (packPrice == null) {
					view.setPrice(groupPriceDouble);
					view.setSpecialPrice(false);
				} else {
					double packPriceDouble = packPrice * 1.0 / 100;
					view.setPrice(packPriceDouble);
					view.setOriPrice(groupPriceDouble);
					view.setSpecialPrice(true);
				}
				
				if (!isFiltered(view)) {
					infoData.add(view);
				}
			}
		} else {
			// 如果没有统计数据，则做特殊处理
			for (GroupPackVo groupPackVo : groupPackList) {
				// 没有删除则生成待显示的VO对象
				GroupPackViewItem view = new GroupPackViewItem();
				view.setGpId(groupPackVo.getGpId());
				view.setPlanId(groupPackVo.getPlanId());
				view.setPlanName(planIdNameMapping.get(groupPackVo.getPlanId()));
				view.setGroupId(groupPackVo.getGroupId());
				view.setGroupName(groupIdNameMapping.get(groupPackVo.getGroupId()));
				view.setPackType(groupPackVo.getType());
				view.setPackName(groupPackVo.getName());
				
				// 记录gpId，以便获取是否为已优化
				packOptimizedGpIdList.add(view.getGpId());
				
				// 设置出价为受众组合单独出价，如无则为推广组出价
				Integer packPrice = groupPackPriceMap.get(groupPackVo.getGpId());
				double groupPriceDouble = groupPriceMap.get(groupPackVo.getGroupId()) * 1.0 / 100;
				if (packPrice == null) {
					view.setPrice(groupPriceDouble);
					view.setSpecialPrice(false);
				} else {
					double packPriceDouble = packPrice * 1.0 / 100;
					view.setPrice(packPriceDouble);
					view.setOriPrice(groupPriceDouble);
					view.setSpecialPrice(true);
				}
				
				if (!isFiltered(view)) {
					infoData.add(view);
				}
			}
		}
		
		this.fillPackOptimized(infoData, packOptimizedGpIdList);
		
		return infoData;
	}
	
	
	private List<GroupPackViewItem> getPackList() {
		List<Map<String, Object>> allData;
		
		List<GroupPackViewItem> olapList = packStatMgr.queryGroupPackData(userId, planIds, groupIds, null, null, 
				from, to, null, 0, ReportConstants.TU_NONE);
		
		List<Map<String, Object>> uvData = uvDataService.queryPackData(qp.getUserId(), planIds, 
				groupIds, null, null, from, to, null, 0, ReportConstants.TU_NONE, 
				Constant.REPORT_TYPE_DEFAULT, 0, 0);
		
		// 2、合并转化、Holmes等其他数据
		// 排序参数
		String idKey = ReportConstants.GPID;
		
		// 如果需要获取转化数据，则获取转化数据并合并
		boolean needToFetchTransData = this.transReportFacade.needToFetchTransData(userId, from, to, false);
		if (needToFetchTransData) {
			
			TempSitesAndTrans tempSiteAndTrans = TransReportHelper.getInstance().getTransSiteIdsAndTargetIds(userId);
			
			// 获取转化数据
			List<Map<String, Object>> transData = transDataService.queryPackData(userId, 
					tempSiteAndTrans.getTransSiteIds(), tempSiteAndTrans.getTransTargetIds(),
					planIds, groupIds, null, null, from, to, null, 0,
					ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);
			// 获取holmes数据
			List<Map<String, Object>> holmesData = holmesDataService.queryPackData(userId, null, null, 
					planIds, groupIds, null, null, from, to, null, 0,
					ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT, 0, 0);

			// Merge基本统计/UV数据与转化数据/holmes数据
			allData = this.reportFacade.mergeTransHolmesAndUvData(Constant.DorisDataType.UV, 
					transData, holmesData, uvData, idKey);
		} else {
			// Merge基本统计/UV数据
			allData = uvData;
		}
		
		List<GroupPackViewItem> dorisList = new ArrayList<GroupPackViewItem>();
		List<GroupPackViewItem> resultList = new ArrayList<GroupPackViewItem>();
		//7、填充数据
		if (! CollectionUtils.isEmpty(allData)) {
			for (Map<String, Object> row : allData) {
				GroupPackViewItem item = new GroupPackViewItem();
				if (row != null) {
					item.setPlanId(Integer.valueOf(row.get(ReportConstants.PLAN).toString()));
					item.setGroupId(Integer.valueOf(row.get(ReportConstants.GROUP).toString()));
					item.setGpId(Long.valueOf(row.get(ReportConstants.GPID).toString()));
					item.setRefPackId(Integer.valueOf(row.get(ReportConstants.REFPACKID).toString()));
					item.fillStatRecord(row);
					dorisList.add(item);
				}
			}
		}
		
		Set<String> mergeKeys = new HashSet<String>(Arrays.asList(new String[]{Constants.COLUMN.GPID}));
		
		resultList = ReportUtils.mergeItemList(dorisList, olapList, mergeKeys, 
				Constants.statMergeVals, GroupPackViewItem.class, true);
		
		ReportUtil.generateExtentionFields(resultList);
		
		return resultList;
	}
	
	/**
	 * fillPackOptimized: 向结果集中添加是否已优化的标记
	 * @version beidou-api 3 plus
	 * @author genglei01
	 * @date 2012-9-24
	 */
	private void fillPackOptimized(List<GroupPackViewItem> infoData, List<Long> packOptimizedGpIdList) {
		Map<Long, Boolean> packOptimizedMap = groupPackFacade.checkPackOptimizedByGpIds(packOptimizedGpIdList);
		
		for (GroupPackViewItem item : infoData) {
			Boolean flag = packOptimizedMap.get(item.getGpId());
			
			if (flag != null && flag) {
				item.setHasModified(true);
			}
		}
	}
	
	private Map<Integer, Integer> getGroupPrice(List<CproGroup> groupList) {
		Map<Integer, Integer> result = new HashMap<Integer, Integer>();
		
		for (CproGroup item : groupList) {
			Integer groupId = item.getGroupId();
			Integer price = item.getGroupInfo().getPrice();
			result.put(groupId, price);
		}
		return result;
	}
	
	/**
	 * isFiltered:判断是否将当前记录过滤
	 *
	 * @param item 待判断VO对象
	 * @return  true表示要过滤，false表示保留    
	 */
	protected boolean isFiltered(GroupPackViewItem item) {
		//1、按查询关键词过滤
		String query = qp.getKeyword();
		if(!StringUtils.isEmpty(query)) {
			query = KTKeywordUtil.validateKeywordForPack(query);
			if (StringUtils.isEmpty(item.getPackName())) {
				return true;
			} else if( !item.getPackName().contains(query)) {
				return true;
			}
		}
		
		//2、按受众组合类型过滤
		Integer[] packTypes = qp.getPackType();
		if (!ArrayUtils.isEmpty(packTypes)) {
			Set<Integer> packTypeSet = new HashSet<Integer>();
			for (Integer packType : packTypes) {
				packTypeSet.add(packType);
			}
			
			if (!packTypeSet.contains(item.getPackType())) {
				return true;
			}
		}
		
		//3、按统计字段过滤
		return ReportWebConstants.filter(qp, item);
	}
	
	/**
	 * calculateSumData:根据返回结果休计算汇总信息
	 * 
	 * @param infoData
	 * @return 返回结果集的汇总信息
	 */
	public GroupPackViewItemSum calculateSumData(List<GroupPackViewItem> infoData) {
		GroupPackViewItemSum sumData = new GroupPackViewItemSum();

		for (GroupPackViewItem item : infoData) {
			sumData.setClks(sumData.getClks() + item.getClks());
			sumData.setCost(sumData.getCost() + item.getCost());
			sumData.setSrchs(sumData.getSrchs() + item.getSrchs());
			
			sumData.setSrchuv(sumData.getSrchuv() + item.getSrchuv());
			sumData.setClkuv(sumData.getClkuv() + item.getClkuv());
		}
		// 生成扩展数据
		sumData.generateExtentionFields();

		sumData.setPackCount(infoData.size());
		return sumData;
	}
	
	/**
	 * generateAccountInfo: 生成报表用的账户信息的VO
	 * 
	 * @return
	 */
	protected ReportAccountInfo generateAccountInfo(String userName) {
		ReportAccountInfo accountInfo = new ReportAccountInfo();
		accountInfo.setReport(this.getText("download.account.report.pack"));
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
	 * 
	 * @return
	 * @since
	 */
	protected String[] generateReportHeader() {

		String[] headers = new String[21];
		for (int col = 0; col < headers.length; col++) {
			headers[col] = this.getText("download.pack.head.col" + (col + 1));
		}
		return headers;
	}
	
	/**
	 * generateReportSummary: 生成报表汇总信息
	 * 
	 * @param sumData 汇总数据VO
	 * @return 用于表示报表的汇总信息VO
	 */
	protected GroupPackReportSumData generateReportSummary(GroupPackViewItemSum sumData) {

		GroupPackReportSumData sum = new GroupPackReportSumData();
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
		
		sum.setSummaryText(this.getText("download.summary.pack",
				new String[] { String.valueOf(sumData.getPackCount()) }));// 添加“合计”
		return sum;
	}
	
	/**
	 * 判断此次请求是否有过滤条件
	 * @return
	 */
	protected boolean hasFilter() {
		return qp.isBoolValue() || !StringUtils.isEmpty(qp.getKeyword()) 
				|| qp.hasStatField4Filter() || !StringUtils.isEmpty(qp.getPackTypeStrs());
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

	public int getOrient() {
		return orient;
	}

	public void setOrient(int orient) {
		this.orient = orient;
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

	public void setCproPlanMgr(CproPlanMgr cproPlanMgr) {
		this.cproPlanMgr = cproPlanMgr;
	}

	public void setCproGroupMgr(CproGroupMgr cproGroupMgr) {
		this.cproGroupMgr = cproGroupMgr;
	}

	public void setGroupPackFacade(GroupPackFacade groupPackFacade) {
		this.groupPackFacade = groupPackFacade;
	}

}
