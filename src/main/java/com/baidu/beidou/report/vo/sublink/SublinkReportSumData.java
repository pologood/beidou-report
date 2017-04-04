package com.baidu.beidou.report.vo.sublink;

import com.baidu.beidou.report.vo.StatInfo;

/**
 * 子链文字汇总
 * 
 * @author zhangxichuan
 * 
 */

public class SublinkReportSumData extends StatInfo {

    private static final long serialVersionUID = 4276838674916540173L;

    /** 文字“汇总” */
    protected String summaryText;

    /**
     * getter for summaryText
     * 
     * @return 文字汇总
     */
    public String getSummaryText() {
        return summaryText;
    }

    /**
     * getter for summaryText
     * 
     * @param summaryText
     *            文字汇总
     */
    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

}
