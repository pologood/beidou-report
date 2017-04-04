/**
 * 2009-1-7 下午02:23:13
 */
package com.baidu.beidou.util.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * @author zengyunfeng
 * @version 1.1.0
 */
public class AbsListJsonObject<E> extends AbsJsonObject {
	
	public static final String LIST_DATA = "listData";
	
	public static final String SUM_DATA = "sumData";
	
	public void putSumData(final List<E> list) {
		if (list == null) {
			return;
		}
		super.addData(LIST_DATA, list);
	}
	
	/**
	 * 设置list对象，list内的对象需要与前端一致
	 * 
	 * @author zengyunfeng
	 * @version 1.1.0
	 * @param list
	 */
	public void putListData(final List<E> list) {
		if (list == null) {
			return;
		}
		super.addData(LIST_DATA, list);
	}

	/**
	 * list中增加list对象
	 * 
	 * @author zengyunfeng
	 * @version 1.1.0
	 * @param row
	 */
	public void addListData(final E row) {
		if (row == null) {
			return;
		}
		List<E> list = (List<E>)super.getData().get(LIST_DATA);
		if(list == null){
			list = new ArrayList<E>();
		}
		list.add(row);
		super.addData(LIST_DATA, list);
	}
}
