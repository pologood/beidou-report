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

import com.baidu.beidou.atleft.rpc.ir.IrFacade;
import com.baidu.beidou.cprogroup.bo.AttachInfo;
import com.baidu.beidou.cprogroup.bo.CproGroup;
import com.baidu.beidou.cprogroup.constant.AttachInfoConstant;
import com.baidu.beidou.cprogroup.service.AttachInfoMgr;
import com.baidu.beidou.cprogroup.service.CproGroupMgr;
import com.baidu.beidou.cprogroup.service.GroupAttachInfoMgr;
import com.baidu.beidou.cprogroup.util.KTKeywordUtil;
import com.baidu.beidou.cprogroup.vo.AttachInfoVo;
import com.baidu.beidou.cproplan.bo.CproPlan;
import com.baidu.beidou.cproplan.service.CproPlanMgr;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.olap.service.AttachStatService;
import com.baidu.beidou.olap.vo.AttachViewItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.dao.vo.GroupQueryParameter;
import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.report.service.ReportCproGroupMgr;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.app.AppViewItemSum;
import com.baidu.beidou.report.vo.attach.AttachReportSumData;
import com.baidu.beidou.report.vo.attach.AttachReportVo;
import com.baidu.beidou.report.vo.attach.AttachViewItemSum;
import com.baidu.beidou.report.vo.group.AttachItemKey;
import com.baidu.beidou.report.vo.group.ExtGroupViewItem;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.LogUtils;
import com.baidu.beidou.util.StringUtils;
import com.baidu.beidou.util.TokenUtil;
import com.baidu.beidou.util.atomdriver.AtomUtils;
import com.baidu.beidou.util.memcache.BeidouCacheInstance;


public class ListGroupAttachAction extends BeidouReportActionSupport{

	private static final long serialVersionUID = 1L;
	private static final int TOKEN_EXPIRE = 60;
	
	private AttachInfoMgr attachInfoMgr;
	private CproPlanMgr cproPlanMgr = null;
	private CproGroupMgr cproGroupMgr = null;
	private List<Integer> planIds = new ArrayList<Integer>();
	private List<Integer> groupIds = new ArrayList<Integer>();
	private ReportCproGroupMgr reportCproGroupMgr;
	private IrFacade irFacade;
	private GroupAttachInfoMgr groupAttachInfoMgr;
	
	private boolean hasAttach;//当前层级（未过滤时）是否含有Attach
	
	private String beidouBasePath;
	private String token;

	
	@Resource(name="attachStatServiceImpl")
	private AttachStatService attachService;

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
	 * 生成用于Attach报告前端显示的VO
	 * @return 
	 */
	public String ajaxAttachList(){		

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
		List<AttachViewItem> list = generateAttachList();
		
		// 3、查询总条数，该数字可能不是最终值，因为有可能需要按统计值过滤，这样的话该值会被再次修改
		int count = list.size();
		
		// 4、排序
		Collections.sort(list, new CproAttachComparator(qp.getOrderBy(), orient));
		
		// 5、生成汇总的统计数据
		AttachViewItemSum sumData = calculateSumData(list);

		// 6、计算总页码
		int totalPage = super.getTotalPage(count);
		
		// 7、获取分页
		list = pagerList(list);

		/**
		 * 如果统计时间是今天，将统计数据列中处点击、消费外的其他列置为-1
		 */
		
		jsonObject.addData("cache",1);
		jsonObject.addData("list", list);
		jsonObject.addData("hasAttach", hasAttach);
		jsonObject.addData("totalPage", totalPage);
		jsonObject.addData("attachCount", count);
		jsonObject.addData("sum", sumData);
		
		//8、处理当日和昨日不入库的uv和转化数据
		this.reportFacade.postHandleTransAndUvData(userId, from, to, list, sumData);

		return SUCCESS;		
	}
	
	/**
	 * 生成用于attach报告前端显示的VO
	 * @return 
	 */
	public String downloadAttachList() throws IOException{		

		//1、初始化一下参数
		initParameter();
		
		//2、查询账户信息
		User user = userMgr.findUserBySFid(userId);
		if (user == null) {
			throw new BusinessException("用户信息不存在");
		}
		
		//3、构造报告VO，下载的CSV共有四部分：1、账户基本信息，2、列头，3、列表，4、汇总信息	
		AttachReportVo vo = new AttachReportVo(qp.getLevel());//报表下载使用的VO
		
		List<AttachViewItem> infoData = null;//目标plan集
		
		//3.1、获取统计数据
		infoData = generateAttachList();//无统计时间粒度
		
		//3.2、排序
		Collections.sort(infoData, new CproAttachComparator(qp.getOrderBy(), orient));

		//3.3、生成汇总的统计数据
		AttachViewItemSum sumData = calculateSumData(infoData);
		
		//3.4处理当日和昨日不入库的uv和转化数据 add by wangchongjie since 2012.11.03
		this.reportFacade.postHandleTransAndUvData(userId, from, to, infoData, sumData);
		
		//3.5、判断是否需要转化数据
		boolean showTransData = false;
		boolean transDataValid = false;
		
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
		fileName += this.getText("download.attach.filename.prefix." + qp.getAttachType());
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
	 * ATTACH比较器
	 */
	class CproAttachComparator implements Comparator<AttachViewItem>  {

		int order;
		String col;
		public CproAttachComparator(String col, int order) {
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
		
		public int compare(AttachViewItem o1, AttachViewItem o2) {
			if(o1 == null) {
				return -1 * order;
			}
			if(o2 == null) {
				return 1 * order;
			}
			if(ReportWebConstants.ATTACH_COLUMN_ATTACHNAME.equals(col)) {
				return compareByString(o1.getAttachName(), o2.getAttachName());
			}
			if(ReportWebConstants.ATTACH_COLUMN_ATTACHSTATE.equals(col)) {
				return order * (int)(o1.getAttachState() - o2.getAttachState());
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
					return o1.getAttachId().intValue() - o2.getAttachId().intValue();
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
			
			return order;
		}			
	}
	
	private List<AttachViewItem> getStatAndTransData() {
		
		List<Integer> attachTypes = new ArrayList<Integer>();
		attachTypes.add(qp.getAttachType());

		//1、获取统计数据
		List<AttachViewItem> olapList  = attachService.queryGroupAttachData(userId, planIds, groupIds, null, attachTypes, 
				from, to, null, 0, ReportConstants.TU_NONE);
		
		return olapList;
	}
	
	private List<AttachViewItem> generateAttachList(){
		if(null == qp.getAttachType()){
			throw new RuntimeException("AttachType is Null");
		}
		//1、获取推广组信息
		GroupQueryParameter queryParam = initGroupQueryParameter();
		List<ExtGroupViewItem> groupItems = reportCproGroupMgr.findExtCproGroupReportInfo(queryParam);
		Map<Integer, ExtGroupViewItem> extGroupViewMapping = new HashMap<Integer, ExtGroupViewItem>(groupItems.size());
		for(ExtGroupViewItem tmp : groupItems){
			extGroupViewMapping.put(tmp.getGroupId(), tmp);
		}
		
		List<AttachViewItem> list = new ArrayList<AttachViewItem>();
        List<AttachViewItem> mergedData = this.getStatAndTransData();

        // 查询已有词
        List<AttachInfo> attachList =
                attachInfoMgr.getAttachInfoByLevel(userId, qp.getPlanId(), qp.getGroupId(), qp.getAttachType());
        if (qp.getAttachType() == AttachInfoConstant.ATTACH_INFO_SUB_URL) {
            for (AttachInfo attach : attachList) {
                AttachInfoVo info = groupAttachInfoMgr.getSubUrlInfoFromUbmc(attach.getGroupId());
                String attachSubUrlTitle = info.getAttachSubUrlTitle();
                String attachSubUrlLink = info.getAttachSubUrlLink();
                if (attachSubUrlTitle.length() > 0 && attachSubUrlLink.length() > 0) {
                    String[] subUrlTitleArray = attachSubUrlTitle.split(",");
                    String attachContent = "子链:";
                    for (int i = 0; i < subUrlTitleArray.length; i++) {
                        attachContent += subUrlTitleArray[i] + ",";
                    }
                    attachContent = attachContent.substring(0, attachContent.length() - 1);
                    attach.setAttachContent(attachContent);
                }
            }
        }
		List<AttachViewItem> existAttach = new ArrayList<AttachViewItem>();
		if(CollectionUtils.isNotEmpty(attachList)){
			for(AttachInfo attach : attachList){
				AttachViewItem dbItem = new AttachViewItem(attach);
				//获取推广组信息
				ExtGroupViewItem groupViewItem = extGroupViewMapping.get(dbItem.getGroupId());
				if (groupViewItem != null) {
					dbItem.setGroupName(groupViewItem.getGroupName());
					dbItem.setPlanId(groupViewItem.getPlanId());
					dbItem.setPlanName(groupViewItem.getPlanName());
					dbItem.setViewState(groupViewItem.getViewState());
					dbItem.setViewStateOrder(groupViewItem.getViewStateOrder());
				}
				existAttach.add(dbItem);
			}
		}

		Map<AttachItemKey, AttachViewItem> mapView = new HashMap<AttachItemKey, AttachViewItem>(existAttach.size());//用户所选的At
		for (AttachViewItem at : existAttach) {
			AttachItemKey key = new AttachItemKey(at.getGroupId(), at.getAttachId(), at.getAttachType());
			mapView.put(key, at);
		}
				
		//9、构造显示VO列表
		if (!CollectionUtils.isEmpty(mergedData)) {//如果有统计数据
			hasAttach = true;
			List<Integer> deletedWordIds = new ArrayList<Integer>();
			List<AttachViewItem> deletedAttachItems = new ArrayList<AttachViewItem>();
			// 9.3合并数据并过滤
			for (AttachViewItem record : mergedData) {

				AttachViewItem attachItem = new AttachViewItem();
				attachItem.fillStatRecord(record);
				
				Integer groupId = record.getGroupId();
				attachItem.setGroupId(groupId);
				//获取推广组信息
				ExtGroupViewItem groupViewItem = extGroupViewMapping.get(groupId);
				if (groupViewItem != null) {
					attachItem.setGroupName(groupViewItem.getGroupName());
					attachItem.setPlanId(groupViewItem.getPlanId());
					attachItem.setPlanName(groupViewItem.getPlanName());
					attachItem.setViewState(groupViewItem.getViewState());
					attachItem.setViewStateOrder(groupViewItem.getViewStateOrder());
				}
				
				attachItem.setAttachId(record.getAttachId());
				attachItem.setAttachType(record.getAttachType());
				
				String attachText = "未知";
				AttachViewItem dbItem = mapView.get(new AttachItemKey(attachItem.getGroupId(), attachItem.getAttachId(), attachItem.getAttachType()));
				if(dbItem != null){
					attachText = dbItem.getAttachName();
					attachItem.setHasDel(false);
					attachItem.setAttachState(dbItem.getAttachState());
				}else{
					deletedWordIds.add(attachItem.getAttachId().intValue());
					attachItem.setHasDel(true);
					attachItem.setAttachState(AttachInfoConstant.STATUS_DELETE); //2表示删除
					deletedAttachItems.add(attachItem);
				}
				attachItem.setAttachName(attachText);
				
				//按照过滤条件过滤
				if (!this.isFiltered(attachItem)) {
					list.add(attachItem);
				}
				mapView.remove(new AttachItemKey(attachItem.getGroupId(), attachItem.getAttachId(), attachItem.getAttachType()));
			}
			//补全删除词的字面
			if(CollectionUtils.isNotEmpty(deletedWordIds) && CollectionUtils.isNotEmpty(deletedAttachItems)){
				if(qp.getAttachType() == AttachInfoConstant.ATTACH_INFO_PHONE || qp.getAttachType() == AttachInfoConstant.ATTACH_INFO_MESSAGE){
					Set<Integer> deletedWordIdSets = new HashSet<Integer>();
					deletedWordIdSets.addAll(deletedWordIds);
					// 需要查询Atom
					Map<Integer, String> atomIdKeywordMapping = AtomUtils.getWordById(deletedWordIdSets);
					for(AttachViewItem item : deletedAttachItems){
						String attachText = atomIdKeywordMapping.get(item.getAttachId().intValue());
						if(attachText == null){
							attachText = "未知";
						}
						item.setAttachName(attachText);
					}
				}
				if(qp.getAttachType() == AttachInfoConstant.ATTACH_INFO_CONSULT){
					for(AttachViewItem item : deletedAttachItems){
						item.setAttachName(AttachInfoConstant.ATTACH_INFO_CONSULT_NAME);
					}
				}
			}
		}
		
		//添加其它已购词
		if(CollectionUtils.isNotEmpty(mapView.keySet())){
			hasAttach = true;
			for(AttachViewItem item : mapView.values()){
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
	public AttachViewItemSum calculateSumData(List<AttachViewItem> infoData) {
		AttachViewItemSum sumData = new AttachViewItemSum();
		for (AttachViewItem item : infoData) {
			sumData.setClks(sumData.getClks() + item.getClks());
			sumData.setCost(sumData.getCost() + item.getCost());
			sumData.setSrchs(sumData.getSrchs() + item.getSrchs());		
			sumData.setSrchuv(sumData.getSrchuv() + item.getSrchuv());
			sumData.setClkuv(sumData.getClkuv() + item.getClkuv());
		}
		sumData.generateExtentionFields();
		if(null != infoData){
			sumData.setAttachCount(infoData.size());
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
	protected boolean isFiltered(AttachViewItem item) {
		//1、按查询关键词过滤
		String query = qp.getKeyword();
		if(!StringUtils.isEmpty(query)) {
			query = KTKeywordUtil.validateKeyword(query);
			if (StringUtils.isEmpty(item.getAttachName())) {
				return true;
			} else if( !item.getAttachName().contains(query)) {
				return true;
			}
		}
		if(null != qp.getAttachState() && ! qp.getAttachState().equals(item.getAttachState())){
			return true;
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
		accountInfo.setReport(this.getText("download.account.report.attach"));
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
		int maxColumn = 11;
		prefix = "download.attach.head.col";
		for ( int col = 0; col < maxColumn; col++ ) {
			
			if(col == 1){
				//第二列是plan如果在plan或者group层级不需要看
				if(qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_PLAN)
						|| qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_GROUP)){
					continue;
				}
			}else if(col == 2){
				//第三列是group如果在group层级不需要看
				if(qp.getLevel().equals(QueryParameterConstant.LEVEL.LEVEL_GROUP)){
					continue;
				}
			}else if(col == 0){
				header.add(this.getText(prefix + (col + 1) + "." + qp.getAttachType()));
				continue;
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
	protected AttachReportSumData generateReportSummary(AttachViewItemSum sumData) {

		AttachReportSumData sum = new AttachReportSumData(); 
		
		sum.setClks(sumData.getClks());
		sum.setSrchs(sumData.getSrchs());
		sum.setCost(sumData.getCost());
		sum.setAcp(sumData.getAcp().doubleValue());
		sum.setCtr(sumData.getCtr().doubleValue());
		sum.setCpm(sumData.getCpm().doubleValue());
		sum.setSrchuv(sumData.getSrchuv());
		sum.setClkuv(sumData.getClkuv());
		sum.setSummaryText(this.getText("download.summary.attach"+"."+qp.getAttachType(),
				new String[] { String.valueOf(sumData.getAttachCount()) }));// 添加“合计”
		
		return sum;
	}
	
	public String deleteAllAttach(){

		// 1、参数初始化
		try {
			this.initParameter();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			jsonObject.setStatus(BeidouCoreConstant.JSON_OPERATE_FAILED);
			jsonObject.addMsg(ReportWebConstants.ERR_DATE);

			return ERROR;
		}
		List<AttachViewItem> list = this.generateAttachList();
		if(CollectionUtils.isEmpty(list)){
			this.token = "";
			buildEmptyResult();
		}else{
			generateAttachToken(list);
		}	
		return "deleteAllAttach";
	}
	
	private void generateAttachToken(List<AttachViewItem> list){
		this.token = "";
		//groupid -> atLeftId list
		HashMap<Integer, List<Long>> result = new HashMap<Integer, List<Long>>();
		for(AttachViewItem item : list){
			List<Long> tmp = result.get(item.getGroupId());
			if(tmp == null){
				tmp = new ArrayList<Long>();
				result.put(item.getGroupId(), tmp);
			}
			tmp.add(item.getAttachId());				
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
		jsonObject.addData("hasAttach", false);
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
	
	public IrFacade getIrFacade() {
		return irFacade;
	}

	public void setIrFacade(IrFacade irFacade) {
		this.irFacade = irFacade;
	}
	
	public boolean isHasAt() {
		return hasAttach;
	}

	public void setHasAt(boolean hasAt) {
		this.hasAttach = hasAt;
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
	
	public AttachInfoMgr getAttachInfoMgr() {
		return attachInfoMgr;
	}

	public void setAttachInfoMgr(AttachInfoMgr attachInfoMgr) {
		this.attachInfoMgr = attachInfoMgr;
	}

    /**
     * @return the groupAttachInfoMgr
     */
    public GroupAttachInfoMgr getGroupAttachInfoMgr() {
        return groupAttachInfoMgr;
    }

    /**
     * @param groupAttachInfoMgr the groupAttachInfoMgr to set
     */
    public void setGroupAttachInfoMgr(GroupAttachInfoMgr groupAttachInfoMgr) {
        this.groupAttachInfoMgr = groupAttachInfoMgr;
    }

}

