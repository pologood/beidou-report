package com.baidu.beidou.report.constant;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

import com.baidu.beidou.cprogroup.constant.CproGroupConstant;
import com.baidu.beidou.cproplan.constant.CproPlanConstant;
import com.baidu.beidou.cprounit.constant.CproUnitConstant;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.dao.vo.FrontBackendGroupStateMapping;
import com.baidu.beidou.report.dao.vo.FrontBackendPlanStateMapping;
import com.baidu.beidou.report.dao.vo.FrontBackendUnitStateMapping;
import com.baidu.beidou.report.vo.QueryParameter;
import com.baidu.beidou.report.vo.StatInfo;
import com.baidu.beidou.user.constant.UserConstant;

public class ReportWebConstants {

	//-----------------------公用属性---------------------------
	/** 在星中，FLASH图表需要查询的时间段最少为7天数据，因此from和to之间的差值至少为6 */
	public static final int MINI_FLASH_QUERY_DATE_DISTANCE = 6;

	// exception messages
	public static final String ERR_DATE = "日期解析错误";
	public static final String ERR_DORIS = "查询数据量过大，建议您缩小查询范围。";
	/** 文件下载时主文件名最大长度 */
	public static final int FILE_MAIN_NAME_MAXLENGTH = 80;
	
	/** 需要前端来排序的阈值大小设置 */
	public static int FRONT_SORT_THRESHOLD = 10000;
	/** Sql查询时的 id in {ids} 的ids个数限制 */
	public static int IDS_FILTER_SIZE_THRESHOLD = FRONT_SORT_THRESHOLD;
	
	/** 按照IDS过滤的时其长度不建议超过此值 */
	public static int IDS_SIZE_THRESHOLD = FRONT_SORT_THRESHOLD;
	
	/** 默认编码为utf-8 */
	public static final String DEFAULT_FILE_ENCODING = "GBK" ;
	/** 报告查询时的最小时间限制 */
	public static final String REPORT_MIN_DATE_STRING = "20081113";
	
	/** 查询日期范围不可超过1年，366天 */
	public static final int QUERY_MAX_DAYS = 366;
	
	/**
	 * 标识Doris的QT和CT的keyword数据是否合并在一起了
	 * true表示已经合并在一起了，false表示未完全合并在一起；
	 * since beidou3.0，后续合并成功之后可将该开关和相关无用逻辑删除掉
	 */
	public static boolean KEYWORD_STATDATA_MERGED = true;
	/**
	 * 关键词合并后的开始时间，参考该时间来分流查询
	 * 
	 * since beidou3.0，后续合并成功之后可将该开关和相关无用逻辑删除掉
	 */
	public static Date KEYWORD_STATDATA_MERGED_STARTDATE = null;


	
	//-----------------------plan级别属性---------------------------

	/** 推广计划的投放日程列名 */
	private static String[] PLAN_ALL_SCHEME_NAMES = new String[] {
			"sundayscheme", "mondayscheme", "tuesdayscheme", "wednesdayscheme",
			"thursdayscheme", "fridayscheme", "saturdayscheme" };
	
	/** 前端排序参数名----状态 */
	public static final String FRONT_BACKEND_ORDERNAME_VIEWSTATE = "viewState";
	/** 前端排序参数名----推广计划名 */
	public static final String FRONT_BACKEND_ORDERNAME_PLANNAME = "planname";
	/** 前端排序参数名----推广组名 */
	public static final String FRONT_BACKEND_ORDERNAME_GROUPNAME = "groupname";
	/** 前端排序参数名----推广计划无线出价比例 */
	public static final String FRONT_BACKEND_ORDERNAME_BIDRATIO = "bidRatio";
	/** 前端排序参数名----推广计划无线出价比例 */
	public static final String FRONT_BACKEND_ORDERNAME_DEVICE = "deviceId";
	/** 前后端参数名映射转换：plan列表 */
	public static final Map<String, String> PLAN_FRONT_BACKEND_PARAMNAME_MAPPING = new HashMap<String, String>();
	/** 前后端参数名映射转换：group列表 */
	public static final Map<String, String> GROUP_FRONT_BACKEND_PARAMNAME_MAPPING = new HashMap<String, String>();
	/** 前后端参数名映射转换：unit列表 */
	public static final Map<String, String> UNIT_FRONT_BACKEND_PARAMNAME_MAPPING = new HashMap<String, String>();
	/** 前后端参数名映射转换：site列表 */
	public static final Map<String, String> SITE_FRONT_BACKEND_PARAMNAME_MAPPING = new HashMap<String, String>();
	/** 前后端状态映射转换：key为列表中的状态标识，value为对应的viewState */
	public static final Map<Integer, FrontBackendPlanStateMapping> PLAN_FRONT_BACKEND_STATE_MAPPING 
															= new HashMap<Integer, FrontBackendPlanStateMapping>();
	
	/** 
	 * 前后端状态映射转换：key为列表中的状态标识，value为对应的真实state
	 * 说明：由于group的前端显示状态得直接定位到对应的groupstate，因此不用再映射成CproGroupState,此处不同于推广计划
	 *  */
	public static final Map<Integer, FrontBackendGroupStateMapping> GROUP_FRONT_BACKEND_STATE_MAPPING 
															= new HashMap<Integer, FrontBackendGroupStateMapping>();
	/** Unit的前后端状态映射转换关系
	 * Key为前端下拉列表中的枚举值，Value为对应的实际DB中存储的状态（可能是包含或者不包含两种），
	 *  */
	public static final Map<Integer, FrontBackendUnitStateMapping> UNIT_FRONT_BACKEND_STATE_MAPPING 
															= new HashMap<Integer, FrontBackendUnitStateMapping>();
	
	
	public static interface FLASH_DATA_STATE {
		public static final int NORMAL = 0;
		public static final int UNNORMAL = 1;
	}
	public static interface DOWNLOAD_ACCOUNT_NAME {
		public static final String ROW1 = "报告：";
		public static final String ROW2 = "帐户：";
		public static final String ROW3 = "日期范围：";
		public static final String ROW4 = "所属层级：";
	}
	
	public static interface TRANS_DATA_DIMENSION {//转化数据统计维度，共四个
		int PLAN		= 0;	//推广计划
		int GROUP		= 1;	//推广组
		int UNIT		= 2;	//创意
		int GROUP_SITE	= 3;	//推广组分网站
	}
	
	/**
	 * 兴趣报告相关常量定义
	 * 1：一级兴趣点
	 * 2：二级兴趣点
	 * 3：兴趣组合
	 */
	public static final int INTEREST_TYPE_FIRST = 1;
	public static final int INTEREST_TYPE_SECOND = 2;
	public static final int INTEREST_TYPE_CUSTOM = 3;
	public static final String INTEREST_OTHER = "其他";
	
	/**
	 * 地域报表相关常量定义
	 * 1:一级地域
	 * 2:二级地域
	 */
	public static final int REGION_TYPE_FIRST = 1;
	public static final int REGION_TYPE_SECOND = 2;
	public static final String REGION_OTHER = "其他";
	public static final String REGION_NOTKNOWN = "未知";
	public static final String REGION_COLUMN_REGIONNAME = "regionName";
	public static final int REGION_ID_MACAU = 5;
	public static final int REGION_ID_HONGKONG = 6;
	public static final int REGION_ID_TAIWAN = 7;
	
	/**
	 * 关键词质量度常量定义
	 */
	public static final int KT_PATTERNTYPE_NO = -1;
	
	/**
	 * 人群属性性别常量
	 * 7:男
	 * 8:女
	 * 0:未知性别
	 * 9:未传入
	 */
	public static final int DT_GENDER_NOTKNOWN = 0;
	public static final int DT_GENDER_MALE = 7;
	public static final int DT_GENDER_FEMALE = 8;
	public static final int DT_GENDER_NOTINPUT = 9;
	public static final String DT_GENDER_MALE_STRING = "男";
	public static final String DT_GENDER_FEMALE_STRING = "女";
	public static final String DT_GENDER_NOTKNOWN_STRING = "未知";
	
	public static final String APP_COLUMN_APPNAME = "appName";
	public static final String PRODUCT_COLUMN_PRODUCTNAME = "productName";
	public static final String APP_COLUMN_PRICE = "price";
	
	public static final String AT_COLUMN_ATNAME = "atName";
	public static final String AT_COLUMN_ATTYPE = "atType";
	
	public static final String ATTACH_COLUMN_ATTACHNAME = "attachName";
	public static final String ATTACH_COLUMN_ATTACHSTATE = "attachState";
	
	public static final String SUBLINK_COLUMN_SUBLINKNAME = "sublinkName";
	public static final String SUBLINK_COLUMN_SUBLINKURL = "sublinkUrl";
	
	
	
	//----------------------------报表下载相关常量定义

	
	/** 自有流量Domain集合(需要显示二级域的一级域集合) */
	public static List<String> showSecondDomainSiteList = new ArrayList<String>();
	
	/** 显示投放网络的最大组阈值 */
	public static int MAX_GROUP_COUNT_IN_SITEVIEW = 100;
	public static final int EXCEED_GROUP_COUNT = 1;
	public static final int NOT_EXCEED_GROUP_COUNT = 0;
	public static final int HAVE_NO_SITE = 0;
	public static final int HAVE_SITE = 1;
	
	/** 显示关键词的最大词数阈值 */
	public static int MAX_KEYWORD_COUNT_IN_KT_REPORT = 300000;
	
	public static int MAX_KEYWORD_COUNT_IN_SI_KT_REPORT = 100000;
	
	public static final int SMARTIDEA_DEFAULT_THUMBNAIL_WIDTH = 80;
	public static final int SMARTIDEA_DEFAULT_THUMBNAIL_HEIGHT = 50;
	
	static {
		
		//前后端列名转换：用于排序
		PLAN_FRONT_BACKEND_PARAMNAME_MAPPING.put("planName", "planname");
		PLAN_FRONT_BACKEND_PARAMNAME_MAPPING.put("viewState", "planstate");
		PLAN_FRONT_BACKEND_PARAMNAME_MAPPING.put("budget", "budget");
		PLAN_FRONT_BACKEND_PARAMNAME_MAPPING.put("promotionType", "promotion_type");

		GROUP_FRONT_BACKEND_PARAMNAME_MAPPING.put("planName", "planname");
		GROUP_FRONT_BACKEND_PARAMNAME_MAPPING.put("groupName", "groupname");
		GROUP_FRONT_BACKEND_PARAMNAME_MAPPING.put("viewState", "groupstate");
		GROUP_FRONT_BACKEND_PARAMNAME_MAPPING.put("grouptype", "grouptype");
		GROUP_FRONT_BACKEND_PARAMNAME_MAPPING.put("price", "price");
		
		SITE_FRONT_BACKEND_PARAMNAME_MAPPING.put("planName", "planname");
		SITE_FRONT_BACKEND_PARAMNAME_MAPPING.put("groupName", "groupname");	
		SITE_FRONT_BACKEND_PARAMNAME_MAPPING.put("groupStateFlag", "viewstate");
		SITE_FRONT_BACKEND_PARAMNAME_MAPPING.put("price", "price");

		UNIT_FRONT_BACKEND_PARAMNAME_MAPPING.put("planName", "planname");
		UNIT_FRONT_BACKEND_PARAMNAME_MAPPING.put("groupName", "groupname");
		UNIT_FRONT_BACKEND_PARAMNAME_MAPPING.put("viewState", "state");
		
		
		//Plan前后端状态映射：用于搜索,plan映射成CproPlanState
		PLAN_FRONT_BACKEND_STATE_MAPPING.put(QueryParameterConstant.FrontViewState.PLAN.ALL, null);
		PLAN_FRONT_BACKEND_STATE_MAPPING.put(QueryParameterConstant.FrontViewState.PLAN.OFFLINE,
				new FrontBackendPlanStateMapping(null, true, true));
		PLAN_FRONT_BACKEND_STATE_MAPPING.put(QueryParameterConstant.FrontViewState.PLAN.ALL_UNDELETED, 
				new FrontBackendPlanStateMapping(new int[]{CproPlanConstant.PLAN_STATE_DELETE}, false, false));
		PLAN_FRONT_BACKEND_STATE_MAPPING.put(QueryParameterConstant.FrontViewState.PLAN.NORMAL, 
				new FrontBackendPlanStateMapping(new int[]{CproPlanConstant.PLAN_STATE_NORMAL}, false, true));

		
		//Group前后端状态映射：plan映射成CproGroup.state，与plan映射方式不同。
		GROUP_FRONT_BACKEND_STATE_MAPPING.put(QueryParameterConstant.FrontViewState.GROUP.ALL, null);
		
		GROUP_FRONT_BACKEND_STATE_MAPPING.put(QueryParameterConstant.FrontViewState.GROUP.NORMAL,
				new FrontBackendGroupStateMapping(new int[]{CproPlanConstant.PLAN_STATE_NORMAL}, false, true,
						new int[]{CproGroupConstant.GROUP_STATE_NORMAL}, true));
		
		GROUP_FRONT_BACKEND_STATE_MAPPING.put(QueryParameterConstant.FrontViewState.GROUP.PAUSE,
				new FrontBackendGroupStateMapping(null, false, true,
						new int[]{CproGroupConstant.GROUP_STATE_PAUSE}, true));
		
		// 推广组所属推广计划状态为“已删除”，则不显示对应的推广组 modified by kanghongwei since 星二期第二批
		GROUP_FRONT_BACKEND_STATE_MAPPING.put(
				QueryParameterConstant.FrontViewState.GROUP.ALL_UNDELETED,
				new FrontBackendGroupStateMapping(new int[]{CproPlanConstant.PLAN_STATE_DELETE}, false, false,
						new int[] { CproGroupConstant.GROUP_STATE_DELETE }, false));
		
		//Unit映射
		UNIT_FRONT_BACKEND_STATE_MAPPING.put(QueryParameterConstant.FrontViewState.UNIT.ALL,
				new FrontBackendUnitStateMapping(null, false, true, null, true,
						new int[]{CproUnitConstant.UNIT_STATE_DELETE}, false));//去掉"删除"的
		
		UNIT_FRONT_BACKEND_STATE_MAPPING.put(QueryParameterConstant.FrontViewState.UNIT.NORMAL,
				new FrontBackendUnitStateMapping(new int[]{CproPlanConstant.PLAN_STATE_NORMAL}, false, true, 
						new int[]{CproGroupConstant.GROUP_STATE_NORMAL}, true,
						new int[]{CproUnitConstant.UNIT_STATE_NORMAL}, true));
		
		UNIT_FRONT_BACKEND_STATE_MAPPING.put(QueryParameterConstant.FrontViewState.UNIT.AUDITING,
				new FrontBackendUnitStateMapping(null, false, true, null, true,
						new int[]{CproUnitConstant.UNIT_STATE_AUDITING}, true));
		
		UNIT_FRONT_BACKEND_STATE_MAPPING.put(QueryParameterConstant.FrontViewState.UNIT.REFUSED,
				new FrontBackendUnitStateMapping(null, false, true, null, true,
						new int[]{CproUnitConstant.UNIT_STATE_REFUSE}, true));
		
		UNIT_FRONT_BACKEND_STATE_MAPPING.put(QueryParameterConstant.FrontViewState.UNIT.PAUSE,
				new FrontBackendUnitStateMapping(null, false, true, null, true,
						new int[]{CproUnitConstant.UNIT_STATE_PAUSE}, true));
		
		UNIT_FRONT_BACKEND_STATE_MAPPING.put(
				QueryParameterConstant.FrontViewState.UNIT.ALL_UNDELETED,
				new FrontBackendUnitStateMapping(new int[] { CproPlanConstant.PLAN_STATE_DELETE },
						false, false, new int[] { CproGroupConstant.GROUP_STATE_DELETE }, false,
						new int[] { CproUnitConstant.UNIT_STATE_DELETE }, false));
	}
	
	//----------------------以下为公用方法--------------------------------
	
	/**
	 * <p>
	 * filter: 是否需要对item进行过滤
	 * 
	 * @param qp
	 *            查询参数
	 * @param item
	 *            待过滤对象
	 * @return true表示需要过滤，反之表示不过滤
	 */
	public static boolean filter(QueryParameter qp, StatInfo item) {
		if (qp == null) {
			return false;
		}
		if (qp.getSrchs() != null && qp.getSrchsOp() != null) {
			if (QueryParameterConstant.OP.EQ == qp.getSrchsOp()) {
				if (item.getSrchs() != qp.getSrchs()) {
					return true;
				}
			} else if (QueryParameterConstant.OP.GT_EQ == qp.getSrchsOp()) {
				if (item.getSrchs() < qp.getSrchs()) {
					return true;
				}

			} else if (QueryParameterConstant.OP.LT_EQ == qp.getSrchsOp()) {
				if (item.getSrchs() > qp.getSrchs()) {
					return true;
				}

			}
		}
		if (qp.getClks() != null && qp.getClksOp() != null) {
			if (QueryParameterConstant.OP.EQ == qp.getClksOp()) {
				if (item.getClks() != qp.getClks()) {
					return true;
				}
			} else if (QueryParameterConstant.OP.GT_EQ == qp.getClksOp()) {
				if (item.getClks() < qp.getClks()) {
					return true;
				}

			} else if (QueryParameterConstant.OP.LT_EQ == qp.getClksOp()) {
				if (item.getClks() > qp.getClks()) {
					return true;
				}

			}
		}
		if (qp.getCtr() != null && qp.getCtrOp() != null) {
			double value = item.getCtr().doubleValue() < 0 ? 0 : item.getCtr().doubleValue();
			if (QueryParameterConstant.OP.EQ == qp.getCtrOp()) {
				if (value != qp.getCtr()) {
					return true;
				}
			} else if (QueryParameterConstant.OP.GT_EQ == qp.getCtrOp()) {
				if (value < qp.getCtr()) {
					return true;
				}

			} else if (QueryParameterConstant.OP.LT_EQ == qp.getCtrOp()) {
				if (value > qp.getCtr()) {
					return true;
				}

			}
		}

		/**
		 * added by liuhao05 since cpweb-492
		 * 增加展现受众，点击受众、受众点击率三种过滤逻辑的处理
		 */
		if (qp.getSrchuv() != null && qp.getSrchuvOp() != null) {
			if (QueryParameterConstant.OP.EQ == qp.getSrchuvOp()) {
				if (item.getSrchuv() != qp.getSrchuv()) {
					return true;
				}
			} else if (QueryParameterConstant.OP.GT_EQ == qp.getSrchuvOp()) {
				if (item.getSrchuv() < qp.getSrchuv()) {
					return true;
				}

			} else if (QueryParameterConstant.OP.LT_EQ == qp.getSrchuvOp()) {
				if (item.getSrchuv() > qp.getSrchuv()) {
					return true;
				}

			}
		}
		if (qp.getClkuv() != null && qp.getClkuvOp() != null) {
			if (QueryParameterConstant.OP.EQ == qp.getClkuvOp()) {
				if (item.getClkuv() != qp.getClkuv()) {
					return true;
				}
			} else if (QueryParameterConstant.OP.GT_EQ == qp.getClkuvOp()) {
				if (item.getClkuv() < qp.getClkuv()) {
					return true;
				}

			} else if (QueryParameterConstant.OP.LT_EQ == qp.getClkuvOp()) {
				if (item.getClkuv() > qp.getClkuv()) {
					return true;
				}

			}
		}
		if (qp.getCusur() != null && qp.getCusurOp() != null) {
			double value = item.getCusur().doubleValue() < 0 ? 0 : item.getCusur().doubleValue();
			if (QueryParameterConstant.OP.EQ == qp.getCusurOp()) {
				if (value != qp.getCusur()) {
					return true;
				}
			} else if (QueryParameterConstant.OP.GT_EQ == qp.getCusurOp()) {
				if (value < qp.getCusur()) {
					return true;
				}

			} else if (QueryParameterConstant.OP.LT_EQ == qp.getCusurOp()) {
				if (value > qp.getCusur()) {
					return true;
				}

			}
		}
		return false;
	}
	

	/**
	 * filter:按照统计字段过滤来自于doris的Map结果
	 *
	 * @param qp 查询参数
	 * @return      
	 * @since 
	*/
	public static boolean filter(QueryParameter qp, Map<String, Object> record) {
		if (qp == null) {
			return false;
		}
		Object rd;
		if (qp.getSrchs() != null && qp.getSrchsOp() != null) {

			rd = record.get(ReportConstants.SRCHS);
			long srchs;
			if (rd != null){
				srchs = Long.parseLong(rd.toString());
			} else {
				srchs = 0;
			}
			if (QueryParameterConstant.OP.EQ == qp.getSrchsOp()) {
				if (srchs != qp.getSrchs()) {
					return true;
				}
			} else if (QueryParameterConstant.OP.GT_EQ == qp.getSrchsOp()) {
				if (srchs < qp.getSrchs()) {
					return true;
				}

			} else if (QueryParameterConstant.OP.LT_EQ == qp.getSrchsOp()) {
				if (srchs > qp.getSrchs()) {
					return true;
				}

			}
		}
		if (qp.getClks() != null && qp.getClksOp() != null) {

			rd = record.get(ReportConstants.CLKS);
			long clks;
			if (rd != null){
				clks = Long.parseLong(rd.toString());
			} else {
				clks = 0;
			}
			if (QueryParameterConstant.OP.EQ == qp.getClksOp()) {
				if (clks != qp.getClks()) {
					return true;
				}
			} else if (QueryParameterConstant.OP.GT_EQ == qp.getClksOp()) {
				if (clks < qp.getClks()) {
					return true;
				}

			} else if (QueryParameterConstant.OP.LT_EQ == qp.getClksOp()) {
				if (clks > qp.getClks()) {
					return true;
				}

			}
		}
		if (qp.getCtr() != null && qp.getCtrOp() != null) {
			rd = record.get(ReportConstants.CTR);
			double ctr;
			if (rd != null){
				ctr = Double.parseDouble(rd.toString());//用小数表示的CTR
			} else {
				ctr = 0;
			}
			if (QueryParameterConstant.OP.EQ == qp.getCtrOp()) {
				if (ctr != qp.getCtr()) {
					return true;
				}
			} else if (QueryParameterConstant.OP.GT_EQ == qp.getCtrOp()) {
				if (ctr < qp.getCtr()) {
					return true;
				}

			} else if (QueryParameterConstant.OP.LT_EQ == qp.getCtrOp()) {
				if (ctr > qp.getCtr()) {
					return true;
				}

			}
		}

		
		/**
		 * adde by liuhao since cpweb-429
		 * 增加展现受众、点击受众、受众点击率的过滤逻辑
		 */	
		if (qp.getSrchuv() != null && qp.getSrchuvOp() != null) {

			rd = record.get(ReportConstants.SRCHUV);
			long srchuv;
			if (rd != null){
				srchuv = Long.parseLong(rd.toString());
			} else {
				srchuv = 0;
			}
			if (QueryParameterConstant.OP.EQ == qp.getSrchuvOp()) {
				if (srchuv != qp.getSrchuv()) {
					return true;
				}
			} else if (QueryParameterConstant.OP.GT_EQ == qp.getSrchuvOp()) {
				if (srchuv < qp.getSrchuv()) {
					return true;
				}

			} else if (QueryParameterConstant.OP.LT_EQ == qp.getSrchuvOp()) {
				if (srchuv > qp.getSrchuv()) {
					return true;
				}

			}
		}
		if (qp.getClkuv() != null && qp.getClkuv() != null) {

			rd = record.get(ReportConstants.CLKUV);
			long clkuv;
			if (rd != null){
				clkuv = Long.parseLong(rd.toString());
			} else {
				clkuv = 0;
			}
			if (QueryParameterConstant.OP.EQ == qp.getClkuvOp()) {
				if (clkuv != qp.getClkuv()) {
					return true;
				}
			} else if (QueryParameterConstant.OP.GT_EQ == qp.getClkuvOp()) {
				if (clkuv < qp.getClkuv()) {
					return true;
				}

			} else if (QueryParameterConstant.OP.LT_EQ == qp.getClkuvOp()) {
				if (clkuv > qp.getClkuv()) {
					return true;
				}

			}
		}
		if (qp.getCusur() != null && qp.getCusurOp() != null) {
			rd = record.get(ReportConstants.CUSUR);
			double cusur;
			if (rd != null){
				cusur = Double.parseDouble(rd.toString());
			} else {
				cusur = 0;
			}
			if (QueryParameterConstant.OP.EQ == qp.getCusurOp()) {
				if (cusur != qp.getCusur()) {
					return true;
				}
			} else if (QueryParameterConstant.OP.GT_EQ == qp.getCusurOp()) {
				if (cusur < qp.getCusur()) {
					return true;
				}

			} else if (QueryParameterConstant.OP.LT_EQ == qp.getCusurOp()) {
				if (cusur > qp.getCusur()) {
					return true;
				}

			}
		}
		return false;
	}

	/**
	 * subListinPage: 获取分页数据
	 *
	 * @param list 待分页集合
	 * @param page 起始页，从0开始
	 * @param pageSize 页大小
	 * @return      
	*/
	public static <T> List<T> subListinPage(List<T> list, final int page, int pageSize) {

		if (!CollectionUtils.isEmpty(list)){
			int count = list.size();
			if (pageSize < 1) {
				pageSize = ReportConstants.PAGE_SIZE;
			}
			int fromIdx = page * pageSize;
			int toIdx = fromIdx + pageSize;
			
			if (fromIdx >= count) {
				return new ArrayList<T>();
			}
			
			if (toIdx > count){
				toIdx = count;
			}
			list = list.subList(fromIdx, toIdx);
		}
		return list;
	}

	/**
	 * getTodayPlanScheme:获取推广计划今天的投放日程名
	 * 
	 * @return
	 * @since
	 */
	public final static String getTodayPlanScheme() {
		// 数据下标从开始，但开始日期从1开始
		return PLAN_ALL_SCHEME_NAMES[Calendar.getInstance().get(
				Calendar.DAY_OF_WEEK) - 1];
	}

	/**
	 * getNowPlanScheme:获取表示当前时间点可投放的二进制表示
	 * 
	 * @return
	 * @since
	 */
	public final static int getNowPlanScheme() {
		return 1 << Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
	}

	/**
	 * isUserCs: 角色是否是销管
	 *
	 * @param roles
	 * @return      
	 * @since 
	*/
	public static  boolean isUserCs(String[] roles) {
		if (roles == null) {
			return false;
		}
		for (String role : roles) {

			if (UserConstant.USER_ROLE_SYS_MANAGER.equals(role)
					|| UserConstant.USER_ROLE_CLIENT_ADMIN.equals(role)
					|| UserConstant.USER_ROLE_SALER_SUPER.equals(role)
					|| UserConstant.USER_ROLE_SALER_FIRST.equals(role)
					|| UserConstant.USER_ROLE_SALER_SECOND.equals(role)) {
				return true;

			}
		}
		return false;
	}
	
	/**
	 * isGBKColumn:是否GBK排序列
	 *
	 * @param column 待排序字段
	 * @return true表示需要按gbk排序，false表示不需要
	*/
	public static boolean isGBKColumn(String column) {
		return FRONT_BACKEND_ORDERNAME_PLANNAME.equalsIgnoreCase(column) 
		|| FRONT_BACKEND_ORDERNAME_GROUPNAME.equalsIgnoreCase(column);
	}
	
	public static String generateGBKSortString(String column) {
		if (!StringUtils.isEmpty(column)) {
			return "convert(" + column.trim() + " USING GBK)";
		} else {
			return "";
		}
	}

	
	public void setFRONT_SORT_THRESHOLD(int value) {
		FRONT_SORT_THRESHOLD = value;
		IDS_FILTER_SIZE_THRESHOLD = value;
		IDS_SIZE_THRESHOLD = value;
	}


	public static void main(String[] args) {
		System.out.println(getNowPlanScheme());
		List<Integer> source = new ArrayList<Integer>();
		List<Integer> target;
		for (int i = 1 ; i < 33; i++) {
			source.add(i);
		}

		target = subListinPage(source, 0, 10);
		target = subListinPage(source, 3, 10);
		target = subListinPage(source, 4, 10);
		System.out.println(target);
		
		System.out.println(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
		System.out.println(getTodayPlanScheme());
		System.out.println(getNowPlanScheme());
		System.out.println((2 << 2) - 1);
		Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 25);
		System.out.println(c.get(Calendar.HOUR_OF_DAY));
	}
	

	public void setShowSecondDomainSiteList(
			String showSecondDomainSiteList) {
		if (!StringUtils.isEmpty(showSecondDomainSiteList)) {
			if (ReportWebConstants.showSecondDomainSiteList == null) {
				ReportWebConstants.showSecondDomainSiteList = new ArrayList<String>();
			}
			for (String domain : showSecondDomainSiteList.split(",")) {
				ReportWebConstants.showSecondDomainSiteList.add(domain);
			}
		}
	}


	public void setKEYWORD_STATDATA_MERGED(String keyword_statdata_merged) {
		KEYWORD_STATDATA_MERGED = Boolean.valueOf(keyword_statdata_merged);
	}


	public void setKEYWORD_STATDATA_MERGED_STARTDATE(
			String keyword_statdata_merged_startdate) {
		try {
			KEYWORD_STATDATA_MERGED_STARTDATE = new SimpleDateFormat("yyyyMMdd")
							.parse(keyword_statdata_merged_startdate);
		} catch (ParseException e) {
			
			throw new RuntimeException(e);
		}
	}


	public void setMAX_GROUP_COUNT_IN_SITEVIEW(
			int max_group_count_in_siteview) {
		MAX_GROUP_COUNT_IN_SITEVIEW = max_group_count_in_siteview;
	}
}