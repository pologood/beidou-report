package com.baidu.beidou.report.vo.sublink;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.baidu.beidou.olap.vo.SublinkViewItem;
import com.baidu.beidou.report.vo.AbstractReportVo;
import com.baidu.beidou.report.vo.ReportAccountInfo;

/**
 * 分子链报表下载文件VO
 * 
 * @author zhangxichuan
 * 
 */
public class SublinkReportVo extends AbstractReportVo {

    private static final long serialVersionUID = -625498961918429148L;

    /** 列头：包括全部的列（含可能需要隐藏的列，如planName） */
    /** 明细 */
    private List<SublinkViewItem> details;

    /** 汇总信息 */
    protected SublinkReportSumData summary;

    @Override
    public String[] getCsvReportHeader() {
        return headers;
    }

    @Override
    public List<String[]> getCsvReportDetail() {
        List<String[]> result = new ArrayList<String[]>(details.size());

        String[] detailStringArray;
        for (SublinkViewItem item : details) {
            int index = 0;
            detailStringArray = new String[headers.length];
            detailStringArray[index] = item.getSublinkName(); // 1
            detailStringArray[++index] = item.getSublinkUrl(); // 2
            detailStringArray[++index] = item.getPlanName(); // 3
            detailStringArray[++index] = item.getGroupName(); // 4
            detailStringArray[++index] = item.getSrchs() + ""; // 5
            detailStringArray[++index] = item.getClks() + ""; // 6
            detailStringArray[++index] = stringValue(item.getCtr()); // 7
            detailStringArray[++index] = stringValue(item.getAcp()); // 8
            detailStringArray[++index] = stringValue(item.getCpm()); // 9
            detailStringArray[++index] = item.getCost() + ""; // 10

            result.add(detailStringArray);
        }

        return result;
    }

    @Override
    public String[] getCsvReportSummary() {
        String[] summaryStringArray = new String[headers.length];
        int index = 0;
        summaryStringArray[index] = summary.getSummaryText(); // 1
        summaryStringArray[++index] = ""; // 2
        summaryStringArray[++index] = ""; // 3
        summaryStringArray[++index] = ""; // 4
        summaryStringArray[++index] = summary.getSrchs() + ""; // 5
        summaryStringArray[++index] = summary.getClks() + ""; // 6
        summaryStringArray[++index] = stringValue(summary.getCtr()); // 7
        summaryStringArray[++index] = stringValue(summary.getAcp()); // 8
        summaryStringArray[++index] = stringValue(summary.getCpm()); // 9
        summaryStringArray[++index] = summary.getCost() + ""; // 10

        return summaryStringArray;
    }

    private String stringValue(BigDecimal num) {
        return num.signum() < 0 ? 0 + "" : num + "";
    }

    /**
     * getter for accountInfo
     * 
     * @return accountInfo
     */
    public ReportAccountInfo getAccountInfo() {
        return accountInfo;
    }

    /**
     * setter for accountInfo
     * 
     * @param accountInfo
     */
    public void setAccountInfo(ReportAccountInfo accountInfo) {
        this.accountInfo = accountInfo;
    }

    /**
     * getter for headers
     * 
     * @return headers
     */
    public String[] getHeaders() {
        return headers;
    }

    /**
     * setter for headers
     * 
     * @param headers
     */
    public void setHeaders(String[] headers) {
        this.headers = headers;
    }

    /**
     * getter for details
     * 
     * @return details
     */
    public List<SublinkViewItem> getDetails() {
        return details;
    }

    /**
     * setter for details
     * 
     * @param details
     */
    public void setDetails(List<SublinkViewItem> details) {
        this.details = details;
    }

    /**
     * getter for summary
     * 
     * @return summary
     */
    public SublinkReportSumData getSummary() {
        return summary;
    }

    /**
     * setter for summary
     * 
     * @param summary
     */
    public void setSummary(SublinkReportSumData summary) {
        this.summary = summary;
    }
}
