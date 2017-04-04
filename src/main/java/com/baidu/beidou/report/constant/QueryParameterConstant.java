package com.baidu.beidou.report.constant;


public interface QueryParameterConstant {

	/** 层级枚举 */
	public interface LEVEL {
		/** 账户层级 */
		String LEVEL_ACCOUNT = "account";
		/** 计划层级 */
		String LEVEL_PLAN = "plan";
		/** 组层级 */
		String LEVEL_GROUP = "group";
	}
	
	/** Tab枚举 */
	public interface TAB {
		/** 计划tab */
		int TAB_PLAN = 1;
		/** 组tab */
		int TAB_GROUP = 2;
		/** 创意层级 */
		int TAB_UNIT = 3;
		/** 投放网络层级：有展现网站 */
		int TAB_SITE_SHOWNSITE = 41;
		/** 投放网络层级：自选行业 */
		int TAB_SITE_CHOSENTRADE = 42;
		/** 投放网络层级：自选行业分网站 */
		int TAB_SITE_CHOSENTRADE_SITE = 43;
		/** 投放网络层级：自选网站 */
		int TAB_SITE_CHOSENSITE = 44;
	}
	
	public interface Boolean{
		int TRUE = 1;
		int FALSE = 0;
	}
	
	/** 操作符枚举 */
	public interface OP {
		/** >= */
		int GT_EQ = 1;
		/** = */
		int EQ = 2;
		/** <= */
		int LT_EQ = 3;
	}
	
	/** 排序标识符枚举 */
	public interface SortOrder {

		/** 排序的顺序--升序 */
		String SORTORDER_ASC = "ASC";
		/** 排序的顺序--降序 */
		String SORTORDER_DESC = "DESC";	
	}
	
	/** 前端查询输入的状态枚举 */
	public interface FrontViewState {
		
		/** 计划的 */
		public interface PLAN {
			/** 所有 */
			int ALL = 0;
			/** 有效 */
			int NORMAL = 1;
			/** 已下线 */
			int OFFLINE = 2;
			/** 所有未删除 */
			int ALL_UNDELETED = 3;
		}
		
		/** 组的 */
		public interface GROUP {
			/** 所有 */
			int ALL = 0;
			/** 有效 */
			int NORMAL = 1;
			/** 暂停 */
			int PAUSE = 2;
			/** 所有未删除 */
			int ALL_UNDELETED = 3;
		}
		
		/** 创意的 */
		public interface UNIT {
			/** 所有 */
			int ALL = 0;
			/** 有效 */
			int NORMAL = 1;
			/** 审核中 */
			int AUDITING = 2;
			/** 审核拒绝 */
			int REFUSED = 3;
			/** 暂停 */
			int PAUSE = 4;
			/** 所有未删除 */
			int ALL_UNDELETED = 5;
		}
	}
	
	String PARAM_DELIMETER = ",";
		
}
