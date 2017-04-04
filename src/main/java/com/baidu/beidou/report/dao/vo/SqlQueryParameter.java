package com.baidu.beidou.report.dao.vo;

import java.io.Serializable;
import java.util.List;

/**
 * <p>ClassName:SqlQueryParameter
 * <p>Function: SQL查询参数基类
 *
 * @author   <a href="mailto:liangshimu@baidu.com">梁时木</a>
 * @created  2011-2-26
 * @since    星计划(beidou 2.0)
 */
public class SqlQueryParameter implements Serializable {

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
	 * </ul>
	 */
	protected String name;
	/** 状态 */
	protected int[] state;
	
	/** state采用in | not in，true表示采用in */
	protected boolean stateInclude = true;
	
	/** 排序列 */
	protected String sortColumn;
	/** 排序方式 */
	protected String sortOrder;
	/** 当前页码 */
	protected Integer page;
	/** 分页大小 */
	protected Integer pageSize ;
	
	/** 用户Id */
	protected Integer userId;
	
	/** ids */
	protected List<Long> ids;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int[] getState() {
		return state;
	}

	public void setState(int[] state) {
		this.state = state;
	}

	public String getSortColumn() {
		return sortColumn;
	}

	public void setSortColumn(String sortColumn) {
		this.sortColumn = sortColumn;
	}

	public String getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(String sortOrder) {
		this.sortOrder = sortOrder;
	}

	public Integer getPage() {
		return page;
	}

	public void setPage(Integer page) {
		this.page = page;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public boolean isStateInclude() {
		return stateInclude;
	}

	public void setStateInclude(boolean stateInclude) {
		this.stateInclude = stateInclude;
	}

	public List<Long> getIds() {
		return ids;
	}

	public void setIds(List<Long> ids) {
		this.ids = ids;
	}
	
}
