package com.baidu.beidou.report.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.baidu.beidou.olap.vo.ReportDayViewItem;
import com.baidu.beidou.report.vo.StatInfo;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.QueryParameterConstant;

public class ReportUtil {
	
	public static List<ReportDayViewItem> transformStatDataByDay(List<Map<String, Object>> statData){
		List<ReportDayViewItem> result = new ArrayList<ReportDayViewItem>();
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
		for(Map<String, Object> stat : statData){
			ReportDayViewItem item = new ReportDayViewItem();
			item.fillStatRecord(stat);
			item.generateExtentionFields();
			Date date = (Date) stat.get(ReportConstants.FROMDATE);
			item.setShowDate(sd.format(date));
			
			result.add(item);
		}
		return result;
	}
	
	public static <T extends StatInfo> void generateExtentionFields(List<T> itemList){
		if(CollectionUtils.isEmpty(itemList)){
			return;
		}
		for(T item : itemList){
			item.generateExtentionFields();
		}
	}

	/**
	 * 根据类别和类型生成atType
	 * 
	 * @param category
	 * @param wordType
	 * @return
	 * 2014-4-12 下午4:42:59 created by wangchongjie
	 */
	public static Integer getAtType(Integer category, Integer wordType){
		return category * 100 + wordType;
	}
	
	/**
	 * <p>transferFromLongToInteger:将List<Long>转换成List<Integer>
	 *
	 * @param target
	 * @return      
	 * @since 
	*/
	public static List<Integer> transferFromLongToInteger(List<Long> target) {
		
		
		if( CollectionUtils.isEmpty(target) ) {
			return new ArrayList<Integer>();
		} else {
			List<Integer> result = new ArrayList<Integer>(target.size());
			for (int i = 0; i < target.size(); i++) {
				result.add(target.get(i).intValue());
			}
			return result;
		}
	}
	public static List<Long> transferFromIntegerToLong(List<Integer> target) {
		
		
		if( CollectionUtils.isEmpty(target) ) {
			return new ArrayList<Long>();
		} else {
			List<Long> result = new ArrayList<Long>(target.size());
			for (int i = 0; i < target.size(); i++) {
				result.add((long)target.get(i));
			}
			return result;
		}
	}
	
	public static Integer[] transferFromStringToInteger(String str) {
		Integer[] result = {};
		if(!StringUtils.isEmpty(str)) {
			String[] arr = str.split(QueryParameterConstant.PARAM_DELIMETER);
			result = new Integer[arr.length];
			for(int i=0; i<arr.length; i++) {
				result[i] = Integer.decode(arr[i]);
			}
		}
		
		return result;
	}
}
