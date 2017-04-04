/**
 * DateScaleHelper.java
 * Sep 18, 2008
 */
package com.baidu.beidou.util;

import java.util.Calendar;
import java.util.Date;

/**
 * @author zengyunfeng
 * @version 1.0.0
 */
public class DateScaleHelper {

	private static String format = "yyyyMMdd";
	/**
	 * 根据日期的快速入口，获得起始日期.
	 * 如果返回值为null,则表示不是快速时间段查询。
	 */
	public static String startDate(String dateScale){
		String dateStart = null;
		Calendar date = Calendar.getInstance();
		
		if (BeidouCoreConstant.PAGE_DATE_RANGE_TODAY.equals(dateScale)){
			dateStart = DateUtils.formatDate(date.getTime(), format);
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_YESTERDAY.equals(dateScale)) {
			date.add(Calendar.DATE, -1);
			dateStart = DateUtils.formatDate(date.getTime(), format);
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_LAST7DAYS
				.equals(dateScale)) {
			date.add(Calendar.DATE, -1);
			date.add(Calendar.DATE, -6);
			dateStart = DateUtils.formatDate(date.getTime(), format);
			
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_LASTWEEK
				.equals(dateScale)) {
			int week = date.get(Calendar.DAY_OF_WEEK);
			if(week==Calendar.SUNDAY){
				//今日为周日，则上周的起始日期从上周一开始
				date.add(Calendar.DATE, -7-6);
			}else{
				date.add(Calendar.DATE, -7);
				date.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
			}
			dateStart = DateUtils.formatDate(date.getTime(), format);
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_THISMONTH
				.equals(dateScale)) {
			if(date.get(Calendar.DAY_OF_MONTH)==1){
				//1号
				dateStart = DateUtils.formatDate(date.getTime(), format); 
			}else{
				date.set(Calendar.DAY_OF_MONTH, 1);
				dateStart = DateUtils.formatDate(date.getTime(), format);
				
			}
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_LASTMONTH
				.equals(dateScale)) {
			date.set(Calendar.DAY_OF_MONTH, 1);
			date.add(Calendar.DATE, -1);
			date.set(Calendar.DAY_OF_MONTH, 1);
			dateStart = DateUtils.formatDate(date.getTime(), format);
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_ALLDATE.equals(dateScale)) {
			dateStart = BeidouCoreConstant.PAGE_DATE_ALLDATE_START;
		} 
		return dateStart;
	}
	
	/**
	 * added by yanjie at 20090304
	 */
	public static Date startDate2(String dateScale){
		Date dateStart = null;
		
		//yyyymmdd00
		Calendar date = DateUtils.getCurDateCeil();
		
		if (BeidouCoreConstant.PAGE_DATE_RANGE_TODAY.equals(dateScale)) {
			dateStart = date.getTime();
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_YESTERDAY.equals(dateScale)) {
			date.add(Calendar.DATE, -1);
			dateStart = date.getTime();
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_LAST7DAYS
				.equals(dateScale)) {
			date.add(Calendar.DATE, -1);
			date.add(Calendar.DATE, -6);
			dateStart = date.getTime();
			
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_LASTWEEK
				.equals(dateScale)) {
			int week = date.get(Calendar.DAY_OF_WEEK);
			if(week==Calendar.SUNDAY){
				//今日为周日，则上周的起始日期从上周一开始
				date.add(Calendar.DATE, -7-6);
			}else{
				date.add(Calendar.DATE, -7);
				date.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
			}
			dateStart = date.getTime();
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_THISMONTH
				.equals(dateScale)) {
			if(date.get(Calendar.DAY_OF_MONTH)==1){
				//1号
				dateStart = date.getTime(); 
			}else{
				date.set(Calendar.DAY_OF_MONTH, 1);
				dateStart = date.getTime();
				
			}
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_LASTMONTH
				.equals(dateScale)) {
			date.set(Calendar.DAY_OF_MONTH, 1);
			date.add(Calendar.DATE, -1);
			date.set(Calendar.DAY_OF_MONTH, 1);
			dateStart = date.getTime();
		}else if(BeidouCoreConstant.PAGE_DATE_RANGE_LAST90DAYS
				.equals(dateScale)){
			date.add(Calendar.DATE, -90);
			dateStart = date.getTime();
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_ALLDATE.equals(dateScale)) {
			dateStart = BeidouCoreConstant.getAlldateStart();
		} 
		return dateStart;
	}
	
	/**
	 * 根据日期的快速入口，获得结束日期。
	 * 如果返回值为null,则表示不是快速时间段查询。
	 */
	public static String endDate(String dateScale){
		String dateEnd = null;
		
		Calendar date = Calendar.getInstance();
		
		if (BeidouCoreConstant.PAGE_DATE_RANGE_TODAY.equals(dateScale)) {
			dateEnd = DateUtils.formatDate(date.getTime(), format);
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_YESTERDAY.equals(dateScale)) {
			date.add(Calendar.DATE, -1);
			dateEnd = DateUtils.formatDate(date.getTime(), format);
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_LAST7DAYS
				.equals(dateScale)) {
			date.add(Calendar.DATE, -1);
			dateEnd = DateUtils.formatDate(date.getTime(), format);
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_LASTWEEK
				.equals(dateScale)) {
			int week = date.get(Calendar.DAY_OF_WEEK);
			if(week==Calendar.SUNDAY){
				//今日为周日，则上周的起始日期从上周一开始
				date.add(Calendar.DATE, -7-6);
			}else{
				date.add(Calendar.DATE, -7);
				date.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
			}
			date.add(Calendar.DATE, 6);
			dateEnd = DateUtils.formatDate(date.getTime(), format);
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_THISMONTH
				.equals(dateScale)) {
			if(date.get(Calendar.DAY_OF_MONTH)==1){
				//1号
				dateEnd = DateUtils.formatDate(date.getTime(), format); 
			}else{
				date.add(Calendar.DATE, -1);
				dateEnd = DateUtils.formatDate(date.getTime(), format);
			}
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_LASTMONTH
				.equals(dateScale)) {
			
			date.set(Calendar.DAY_OF_MONTH, 1);
			date.add(Calendar.DATE, -1);
			dateEnd = DateUtils.formatDate(date.getTime(), format);		
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_ALLDATE.equals(dateScale)) {
			date.add(Calendar.DATE, -1);
			dateEnd = DateUtils.formatDate(date.getTime(), format);
		} 
		return dateEnd;
	}
	/**
	 * added by yanjie at 20090304
	 */
	public static Date endDate2(String dateScale){
		Date dateEnd = null;
		
		//yyyymmdd23
		Calendar date = DateUtils.getCurDateFloor();
		
		if (BeidouCoreConstant.PAGE_DATE_RANGE_TODAY.equals(dateScale)) {
			dateEnd = date.getTime();
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_YESTERDAY.equals(dateScale)) {
			date.add(Calendar.DATE, -1);
			dateEnd = date.getTime();
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_LAST7DAYS
				.equals(dateScale)) {
			date.add(Calendar.DATE, -1);
			dateEnd = date.getTime();
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_LASTWEEK
				.equals(dateScale)) {
			int week = date.get(Calendar.DAY_OF_WEEK);
			if(week==Calendar.SUNDAY){
				//今日为周日，则上周的起始日期从上周一开始
				date.add(Calendar.DATE, -7-6);
			}else{
				date.add(Calendar.DATE, -7);
				date.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
			}
			date.add(Calendar.DATE, 6);
			dateEnd = date.getTime();
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_THISMONTH
				.equals(dateScale)) {
			if(date.get(Calendar.DAY_OF_MONTH)==1){
				//1号
				dateEnd = date.getTime(); 
			}else{
				date.add(Calendar.DATE, -1);
				dateEnd = date.getTime();
			}
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_LASTMONTH
				.equals(dateScale)) {
			
			date.set(Calendar.DAY_OF_MONTH, 1);
			date.add(Calendar.DATE, -1);
			dateEnd = date.getTime();
		}else if(BeidouCoreConstant.PAGE_DATE_RANGE_LAST90DAYS.equals(dateScale)){
			date.add(Calendar.DATE, -1);
			dateEnd = date.getTime();
		} else if (BeidouCoreConstant.PAGE_DATE_RANGE_ALLDATE.equals(dateScale)) {
			date.add(Calendar.DATE, -1);
			dateEnd = date.getTime();
		} 
		return dateEnd;
	}
	
	public static boolean isDateEqual(Date d1, Date d2){
		return (d1.getYear()==d1.getYear() && d1.getMonth()==d2.getMonth() && d1.getDay()==d2.getDay());
	}
	
	/**
	 * 昨天
	 * @param from
	 * @param to
	 * @return
	 * 下午6:39:20 created by wangchongjie
	 */
	public static boolean isYesterday(Date from, Date to) {
		Calendar targetTo = Calendar.getInstance();
		Calendar targetFrom = Calendar.getInstance();
		Calendar now = Calendar.getInstance();
		
	    targetTo.add(Calendar.DAY_OF_YEAR, -1);  
	    targetFrom.add(Calendar.DAY_OF_YEAR, -1);
		
		if(isDateEqual(targetFrom.getTime(),from) && isDateEqual(targetTo.getTime(),to)){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 过去7天
	 * @param from
	 * @param to
	 * @return
	 * 下午6:39:29 created by wangchongjie
	 */
	public static boolean isLastSevenDays(Date from, Date to) {
		Calendar targetTo = Calendar.getInstance();
		Calendar targetFrom = Calendar.getInstance();
		Calendar now = Calendar.getInstance();
		
	    targetTo.add(Calendar.DAY_OF_YEAR, -1);
	    targetFrom.add(Calendar.DAY_OF_YEAR, -7);
	    
	    Date d = targetFrom.getTime();
		
		if(isDateEqual(targetFrom.getTime(),from) && isDateEqual(targetTo.getTime(),to)){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 上周
	 * @param from
	 * @param to
	 * @return
	 * 下午6:39:35 created by wangchongjie
	 */
	public static boolean isLastWeek(Date from, Date to) {
		Calendar targetTo = Calendar.getInstance();
		Calendar targetFrom = Calendar.getInstance();
		Calendar now = Calendar.getInstance();
		
	    int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
	    dayOfWeek--;
	    if(0 == dayOfWeek){
	    	dayOfWeek += 7;
	    }
	    targetTo.add(Calendar.DAY_OF_YEAR, -dayOfWeek);
	    targetFrom.add(Calendar.DAY_OF_YEAR, -dayOfWeek-6);
		
		if(isDateEqual(targetFrom.getTime(),from) && isDateEqual(targetTo.getTime(),to)){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 本月
	 * @param from
	 * @param to
	 * @return
	 * 下午6:39:42 created by wangchongjie
	 */
	public static boolean isThisMonth(Date from, Date to) {
		Calendar targetTo = Calendar.getInstance();
		Calendar targetFrom = Calendar.getInstance();
		Calendar now = Calendar.getInstance();
		
	    int dayOfMonth = now.get(Calendar.DAY_OF_MONTH);
	    targetTo.add(Calendar.DAY_OF_YEAR, -1);
	    targetFrom.add(Calendar.DAY_OF_YEAR, 1-dayOfMonth);
		
		if(isDateEqual(targetFrom.getTime(),from) && isDateEqual(targetTo.getTime(),to)){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 上月
	 * @param from
	 * @param to
	 * @return
	 * 下午6:39:47 created by wangchongjie
	 */
	public static boolean isLastMonth(Date from, Date to) {
		Calendar targetTo = Calendar.getInstance();
		Calendar targetFrom = Calendar.getInstance();
		Calendar now = Calendar.getInstance();
		
	    int dayOfMonth = now.get(Calendar.DAY_OF_MONTH);
	    targetTo.add(Calendar.DAY_OF_YEAR, -dayOfMonth);
	    targetFrom.add(Calendar.DAY_OF_YEAR, -dayOfMonth);
	    targetFrom.set(Calendar.DAY_OF_MONTH, 1);
		
		if(isDateEqual(targetFrom.getTime(),from) && isDateEqual(targetTo.getTime(),to)){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 上季度
	 * @param from
	 * @param to
	 * @return
	 * 下午6:39:54 created by wangchongjie
	 */
	public static boolean isLastSeason(Date from, Date to) {
		Calendar targetTo = Calendar.getInstance();
		Calendar targetFrom = Calendar.getInstance();
		Calendar now = Calendar.getInstance();
		
	    int month = now.get(Calendar.MONTH);
	    int reviseMonth = month % 3;
	    targetTo.add(Calendar.MONTH, -reviseMonth);
		int dayOfMonth = targetTo.get(Calendar.DAY_OF_MONTH);
		targetTo.add(Calendar.DAY_OF_MONTH, -dayOfMonth);
		targetFrom.add(Calendar.MONTH, -reviseMonth-3);
		targetFrom.set(Calendar.DAY_OF_MONTH, 1);
	    
		if(isDateEqual(targetFrom.getTime(),from) && isDateEqual(targetTo.getTime(),to)){
			return true;
		}else{
			return false;
		}
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(DateUtils.formatDate(DateScaleHelper.startDate2(BeidouCoreConstant.PAGE_DATE_RANGE_TODAY), "yyyyMMddHH"));
		System.out.println(DateUtils.formatDate(DateScaleHelper.endDate2(BeidouCoreConstant.PAGE_DATE_RANGE_TODAY), "yyyyMMddHH"));
		System.out.println(DateUtils.formatDate(DateScaleHelper.startDate2(BeidouCoreConstant.PAGE_DATE_RANGE_YESTERDAY), "yyyyMMddHH"));
		System.out.println(DateUtils.formatDate(DateScaleHelper.endDate2(BeidouCoreConstant.PAGE_DATE_RANGE_YESTERDAY), "yyyyMMddHH"));
		System.out.println(DateUtils.formatDate(DateScaleHelper.startDate2(BeidouCoreConstant.PAGE_DATE_RANGE_LAST7DAYS), "yyyyMMddHH"));
		System.out.println(DateUtils.formatDate(DateScaleHelper.endDate2(BeidouCoreConstant.PAGE_DATE_RANGE_LAST7DAYS), "yyyyMMddHH"));
		System.out.println(DateUtils.formatDate(DateScaleHelper.startDate2(BeidouCoreConstant.PAGE_DATE_RANGE_LASTMONTH), "yyyyMMddHH"));
		System.out.println(DateUtils.formatDate(DateScaleHelper.endDate2(BeidouCoreConstant.PAGE_DATE_RANGE_LASTMONTH), "yyyyMMddHH"));
		System.out.println(DateUtils.formatDate(DateScaleHelper.startDate2(BeidouCoreConstant.PAGE_DATE_RANGE_LASTWEEK), "yyyyMMddHH"));
		System.out.println(DateUtils.formatDate(DateScaleHelper.endDate2(BeidouCoreConstant.PAGE_DATE_RANGE_LASTWEEK), "yyyyMMddHH"));
		System.out.println(DateUtils.formatDate(DateScaleHelper.startDate2(BeidouCoreConstant.PAGE_DATE_RANGE_THISMONTH), "yyyyMMddHH"));
		System.out.println(DateUtils.formatDate(DateScaleHelper.endDate2(BeidouCoreConstant.PAGE_DATE_RANGE_THISMONTH), "yyyyMMddHH"));
		
	}

}
