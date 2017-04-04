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

import org.apache.commons.collections.CollectionUtils;

import com.baidu.beidou.cprogroup.bo.CproGroup;
import com.baidu.beidou.cprogroup.service.CproGroupMgr;
import com.baidu.beidou.cprogroup.util.KTKeywordUtil;
import com.baidu.beidou.cproplan.bo.CproPlan;
import com.baidu.beidou.cproplan.service.CproPlanMgr;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.group.ProductReportSumData;
import com.baidu.beidou.report.vo.group.ProductReportVo;
import com.baidu.beidou.report.vo.product.ProductViewItem;
import com.baidu.beidou.report.vo.product.ProductViewItemSum;
import com.baidu.beidou.stat.driver.bo.Constant;
import com.baidu.beidou.stat.service.ProductStatService;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.LogUtils;
import com.baidu.beidou.util.StringUtils;
import com.baidu.beidou.util.sidriver.bo.SiProductBiz.ProductNameResponse.ProductNameItem;
import com.baidu.beidou.util.sidriver.service.SmartIdeaService;

/**
 * 智能创意产品报告
 * 
 * @author Zhang Xu
 */
public class ListGroupProductAction extends BeidouReportActionSupport {

	private static final long serialVersionUID = 1L;

	private ProductStatService productStatService;

	private SmartIdeaService smartIdeaService;

	private CproPlanMgr cproPlanMgr = null;
	private CproGroupMgr cproGroupMgr = null;
	private List<Integer> planIds = new ArrayList<Integer>();
	private List<Integer> groupIds = new ArrayList<Integer>();
	
	private String beidouBasePath;

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
		if (qp.getPlanId() != null) {
			planIds = new ArrayList<Integer>();
			planIds.add(qp.getPlanId());
		}
		if (qp.getGroupId() != null) {
			groupIds = new ArrayList<Integer>();
			groupIds.add(qp.getGroupId());
		}
		if (qp.getPage() == null || qp.getPage() < 0) {
			qp.setPage(0);
		}

		if (qp.getPageSize() == null || qp.getPageSize() < 1) {
			qp.setPageSize(ReportConstants.PAGE_SIZE);
		}
	}

	protected void initParameter() {
		super.initParameter();
		orient = ReportConstants.SortOrder.ASC;
		if (QueryParameterConstant.SortOrder.SORTORDER_DESC.equalsIgnoreCase(qp.getOrder())) {
			orient = ReportConstants.SortOrder.DES;
		}
	}

	public String ajaxList() {

		// 1、参数初始化
		try {
			this.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return SUCCESS;
		}

		// 2、生成显示的ViewItem列表
		List<ProductViewItem> list = generateProductList();

		// 3、查询总条数，该数字可能不是最终值，因为有可能需要按统计值过滤，这样的话该值会被再次修改
		int count = list.size();

		// 4、排序
		Collections.sort(list, new ProductComparator(qp.getOrderBy(), orient));

		// 5、生成汇总的统计数据
		ProductViewItemSum sumData = calculateSumData(list);

		// 6、计算总页码
		int totalPage = super.getTotalPage(count);

		// 7、获取分页，生成缓存条件：总条件小于1W
		boolean cache = shouldCache(count);
		if (!cache) {
			list = ReportWebConstants.subListinPage(list, qp.getPage(), qp.getPageSize());
		}

		jsonObject.addData("cache", cache ? ReportConstants.Boolean.TRUE : ReportConstants.Boolean.FALSE);
		jsonObject.addData("list", list);
		jsonObject.addData("totalPage", totalPage);

		jsonObject.addData("sum", sumData);

		return SUCCESS;
	}

	public String download() throws IOException {

		//1、初始化一下参数
		initParameter();

		//2、查询账户信息
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}

		//3、构造报告VO，下载的CSV共有四部分：1、账户基本信息，2、列头，3、列表，4、汇总信息	
		ProductReportVo vo = new ProductReportVo(qp.getLevel());//报表下载使用的VO

		List<ProductViewItem> infoData = null;//目标plan集

		//3.1、获取统计数据
		infoData = generateProductList();//无统计时间粒度

		//3.2、排序
		Collections.sort(infoData, new ProductComparator(qp.getOrderBy(), orient));

		//3.3、生成汇总的统计数据
		ProductViewItemSum sumData = calculateSumData(infoData);

		//3.5、填充数据
		vo.setAccountInfo(generateAccountInfo(user.getUsername()));
		vo.setDetails(infoData);
		vo.setHeaders(generateReportHeader());
		vo.setSummary(generateReportSummary(sumData));

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ReportCSVWriter.getInstance().write(vo, output);

		//4、设置下载需要使用到的一些属性
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
		fileName += this.getText("download.product.filename.prefix");
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

	/**************************业务方法*********************************/
	class ProductComparator implements Comparator<ProductViewItem> {

		int order;
		String col;

		public ProductComparator(String col, int order) {
			this.col = col;
			this.order = order;
		}

		private int compareByString(String v1, String v2) {
			Collator collator = Collator.getInstance(java.util.Locale.CHINA);
			if (StringUtils.isEmpty(v1)) {
				return -1 * order;
			}
			if (StringUtils.isEmpty(v2)) {
				return order;
			}
			return order * collator.compare(v1, v2);
		}

		public int compare(ProductViewItem o1, ProductViewItem o2) {
			if (o1 == null) {
				return -1 * order;
			}
			if (o2 == null) {
				return 1 * order;
			}
			if (col.equals(ReportWebConstants.PRODUCT_COLUMN_PRODUCTNAME)) {
				return compareByString(o1.getProductName(), o2.getProductName());
			}
			if (ReportWebConstants.FRONT_BACKEND_ORDERNAME_PLANNAME.equalsIgnoreCase(col)) {
				return compareByString(o1.getPlanName(), o2.getPlanName());
			}
			if (ReportWebConstants.FRONT_BACKEND_ORDERNAME_GROUPNAME.equalsIgnoreCase(col)) {
				return compareByString(o1.getGroupName(), o2.getGroupName());
			}
			if (ReportConstants.SRCHS.equals(col)) {
				if ((o1.getSrchs() - o2.getSrchs()) == 0) {
					return o1.getProductId().intValue() - o2.getProductId().intValue();
				} else {
					return order * (int) (o1.getSrchs() - o2.getSrchs());
				}
			}

			if (ReportConstants.CLKS.equals(col)) {
				return order * (int) (o1.getClks() - o2.getClks());
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

			return order;
		}
	}

	private List<ProductViewItem> generateProductList() {
		// 校验userId信息
		Integer userId = qp.getUserId();
		if (userId == null || userId <= 0) {
			return new ArrayList<ProductViewItem>();
		}

		//2、获取统计数据
		long start = System.currentTimeMillis();
		List<Map<String, Object>> allData = productStatService.queryGroupProductData(userId, planIds, groupIds, null, from, to, null, 0, ReportConstants.TU_NONE, Constant.REPORT_TYPE_DEFAULT);
		long now = System.currentTimeMillis();
		log.debug("query si product doris data cost time: " + (now - start));

		Set<Integer> planIds = new HashSet<Integer>();
		Set<Integer> groupIds = new HashSet<Integer>();
		for (Map<String, Object> stat : allData) {
			planIds.add(Integer.valueOf(stat.get(ReportConstants.PLAN).toString()));
			groupIds.add(Integer.valueOf(stat.get(ReportConstants.GROUP).toString()));
		}

		// 根据groupIds和planIds查找对应的推广组和推广计划列表
		Map<Integer, String> planIdNameMapping = cproPlanMgr.findPlanNameByPlanIds(planIds);
		Map<Integer, String> groupIdNameMapping = cproGroupMgr.findGroupNameByGroupIds(groupIds);

		List<ProductViewItem> infoData = new ArrayList<ProductViewItem>();
		if (!CollectionUtils.isEmpty(allData)) {
			Long statProductId;
			Integer statPlanId;
			Integer statGroupId;

			Set<Long> productIdSet = new HashSet<Long>();

			for (Map<String, Object> stat : allData) {
				try {
					statProductId = Long.valueOf(stat.get(ReportConstants.PRODUCT_ID).toString());
					statGroupId = Integer.valueOf(stat.get(ReportConstants.GROUP).toString());
					statPlanId = Integer.valueOf(stat.get(ReportConstants.PLAN).toString());
					productIdSet.add(statProductId);
				} catch (Exception e) {
					log.error("error to digest doris data[" + stat + "]", e);
					continue;
				}
				
				Map<Long, String> prodId2NameMap = getProdId2NameMap(productIdSet, userId);

				ProductViewItem view = new ProductViewItem();
				view.fillStatRecord(stat);
				view.setPlanId(statPlanId);
				view.setPlanName(planIdNameMapping.get(statPlanId));
				view.setGroupId(statGroupId);
				view.setGroupName(groupIdNameMapping.get(statGroupId));
				view.setProductId(statProductId);
				String name = prodId2NameMap.get(statProductId);
				if (StringUtils.isEmpty(name)) {
					view.setProductName("产品ID:" + statProductId);
				} else {
					view.setProductName(name);
				}
				if (!isFiltered(view)) {
					infoData.add(view);
				}
			}
		}

		return infoData;
	}

	private Map<Long, String> getProdId2NameMap(Set<Long> productIdSet, Integer userId) {
		List<Long> productIdList = new ArrayList<Long>(productIdSet);
		try {
			List<ProductNameItem> productNameList = smartIdeaService.getProductNameInfos(productIdList, userId);
			Map<Long, String> prodId2NameMap = new HashMap<Long, String>(productIdList.size());
			for (ProductNameItem productNameItem : productNameList) {
				prodId2NameMap.put(productNameItem.getProductId(), productNameItem.getProductName().toStringUtf8());
			}
			return prodId2NameMap;
		} catch (Exception e) {
			log.error("Error occurred when query product name", e);
			return new HashMap<Long, String>(0);
		}
	}

	/**
	 * calculateSumData:根据列表返回汇总列
	 * 
	 * @param infoData
	 * @return 返回结果集的汇总信息
	 */
	public ProductViewItemSum calculateSumData(List<ProductViewItem> infoData) {
		ProductViewItemSum sumData = new ProductViewItemSum();
		for (ProductViewItem item : infoData) {
			sumData.setClks(sumData.getClks() + item.getClks());
			sumData.setCost(sumData.getCost() + item.getCost());
			sumData.setSrchs(sumData.getSrchs() + item.getSrchs());
			sumData.setSrchuv(sumData.getSrchuv() + item.getSrchuv());
			sumData.setClkuv(sumData.getClkuv() + item.getClkuv());
		}
		sumData.generateExtentionFields();
		if (null != infoData) {
			sumData.setProductCount(infoData.size());
		}

		return sumData;
	}

	/**
	 * isFiltered:判断是否将当前记录过滤
	 *
	 * @param item 待判断VO对象
	 * @return  true表示要过滤，false表示保留    
	 */
	protected boolean isFiltered(ProductViewItem item) {
		//1、按查询关键词过滤
		String query = qp.getKeyword();
		if (!StringUtils.isEmpty(query)) {
			// add by kanghongwei since cpweb429(qtIM)
			query = KTKeywordUtil.validateKeyword(query);
			if (StringUtils.isEmpty(item.getProductName())) {
				return true;
			} else if (!item.getProductName().contains(query) && !String.valueOf(item.getProductId()).contains(query)) {
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
		accountInfo.setReport(this.getText("download.account.report.product"));
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
	 * @param showTransData 
	 *
	 * @return      
	 * @since 
	*/
	protected String[] generateReportHeader() {

		List<String> header = new ArrayList<String>();
		String prefix = null;
		int maxColumn = 10;
		prefix = "download.product.head.col";
		for (int col = 0; col < maxColumn; col++) {
			if (col == 2) {
				//第二列是plan如果在plan或者group层级不需要看
				if (qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_PLAN) || qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_GROUP)) {
					continue;
				}
			} else if (col == 3) {
				//第三列是group如果在group层级不需要看
				if (qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_GROUP)) {
					continue;
				}
			}
			header.add(this.getText(prefix + (col + 1)));
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
	protected ProductReportSumData generateReportSummary(ProductViewItemSum sumData) {

		ProductReportSumData sum = new ProductReportSumData();

		sum.setClks(sumData.getClks());
		sum.setSrchs(sumData.getSrchs());
		sum.setCost(sumData.getCost());
		sum.setAcp(sumData.getAcp().doubleValue());
		sum.setCtr(sumData.getCtr().doubleValue());
		sum.setCpm(sumData.getCpm().doubleValue());
		sum.setSrchuv(sumData.getSrchuv());
		sum.setClkuv(sumData.getClkuv());
		
		sum.setSummaryText(this.getText("download.summary.product", new String[] { String.valueOf(sumData.getProductCount()) }));// 添加“合计”
		
		return sum;
	}
	
	/*********************getter and setter**************************/
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
	
	public ProductStatService getProductStatService() {
		return productStatService;
	}

	public void setProductStatService(ProductStatService productStatService) {
		this.productStatService = productStatService;
	}

	public SmartIdeaService getSmartIdeaService() {
		return smartIdeaService;
	}

	public void setSmartIdeaService(SmartIdeaService smartIdeaService) {
		this.smartIdeaService = smartIdeaService;
	}

	public String getBeidouBasePath() {
		return beidouBasePath;
	}

	public void setBeidouBasePath(String beidouBasePath) {
		this.beidouBasePath = beidouBasePath;
	}

}
