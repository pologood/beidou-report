package com.baidu.beidou.report.web.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;

import com.baidu.beidou.cprogroup.bo.CproGroup;
import com.baidu.beidou.cprogroup.rpc.SublinkFacade;
import com.baidu.beidou.cprogroup.rpc.params.SublinkResponse.SublinkInfo;
import com.baidu.beidou.cprogroup.service.CproGroupMgr;
import com.baidu.beidou.cproplan.bo.CproPlan;
import com.baidu.beidou.cproplan.service.CproPlanMgr;
import com.baidu.beidou.exception.BusinessException;
import com.baidu.beidou.olap.service.SublinkStatService;
import com.baidu.beidou.olap.vo.SublinkViewItem;
import com.baidu.beidou.report.ReportConstants;
import com.baidu.beidou.report.constant.ReportWebConstants;
import com.baidu.beidou.report.dao.vo.GroupQueryParameter;
import com.baidu.beidou.report.output.ReportWriter;
import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.report.service.ReportCproGroupMgr;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.group.ExtGroupViewItem;
import com.baidu.beidou.report.vo.sublink.SublinkViewItemSum;
import com.baidu.beidou.report.vo.sublink.SublinkComparator;
import com.baidu.beidou.report.vo.sublink.SublinkReportSumData;
import com.baidu.beidou.report.vo.sublink.SublinkReportVo;
import com.baidu.beidou.user.bo.User;
import com.baidu.beidou.util.BeidouCoreConstant;
import com.baidu.beidou.util.LogUtils;
import com.baidu.beidou.util.StringUtils;

/**
 * 分子链详细报表action
 * 
 * @author zhangxichuan
 * 
 */
public class ListGroupSublinkAction extends BeidouReportActionSupport {

    private static final long serialVersionUID = -4005023130140675145L;

    private List<Integer> planIds = new ArrayList<Integer>();
    private List<Integer> groupIds = new ArrayList<Integer>();
    private ReportCproGroupMgr reportCproGroupMgr;

    private CproPlanMgr cproPlanMgr = null;
    private CproGroupMgr cproGroupMgr = null;

    @Resource(name = "sublinkStatServiceImpl")
    private SublinkStatService sublinkService;

    @Resource(name = "sublinkFacade")
    private SublinkFacade sublinkFacade;
    private String beidouBasePath;
    private String token;

    /**
     * 排序方向（正排为1，倒排为-1）
     */
    private int orient;

    @Override
    protected void initStateMapping() {
        qp.setUserId(userId);
        // 单独处理前端传递过来planid groupid为0的情况
        if (qp.getPlanId() == null || qp.getPlanId() == 0) {
            qp.setPlanId(null);
        }
        if (qp.getGroupId() == null || qp.getGroupId() == 0) {
            qp.setGroupId(null);
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

    /**
     * 生成用于子链报告前端显示的VO
     * 
     * @return SUCCESS
     */
    public String sublinkList() {
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
        List<SublinkViewItem> list = generateSublinkList();

        // 3、查询总条数
        int count = list.size();

        // 4、排序
        Collections.sort(list, new SublinkComparator(qp.getOrderBy(), orient));

        // 5、生成汇总的统计数据
        SublinkViewItemSum sumData = calculateSumData(list);

        // 6、计算总页码
        int totalPage = super.getTotalPage(count);

        // 7、获取分页
        list = pagerList(list);

        jsonObject.addData("cache", 1);
        jsonObject.addData("list", list);
        jsonObject.addData("totalPage", totalPage);
        jsonObject.addData("sublinkCount", count);
        jsonObject.addData("sum", sumData);

        return SUCCESS;
    }

    /**
     * 下载分子链报表
     * 
     * @return SUCCESS
     * @throws IOException
     *             IO异常
     */
    public String downloadSublinkList() throws IOException {

        // 1、初始化参数
        initParameter();

        // 2、查询账户信息
        User user = userMgr.findUserBySFid(userId);
        if (user == null) {
            throw new BusinessException("用户信息不存在");
        }

        // 3、构造报告VO，下载的CSV共有四部分：1、账户基本信息，2、列头，3、列表，4、汇总信息
        SublinkReportVo vo = new SublinkReportVo(); // 报表下载使用的VO

        List<SublinkViewItem> infoData = null; // 目标plan集

        // 3.1、获取统计数据
        infoData = generateSublinkList(); // 无统计时间粒度

        // 3.2、排序
        Collections.sort(infoData, new SublinkComparator(qp.getOrderBy(), orient));

        // 3.3、生成汇总的统计数据
        SublinkViewItemSum sumData = calculateSumData(infoData);

        // 3.4、填充数据
        vo.setAccountInfo(generateAccountInfo(user.getUsername()));
        vo.setDetails(infoData);
        vo.setHeaders(generateReportHeader());
        vo.setSummary(generateReportSummary(sumData));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ReportWriter writer = ReportCSVWriter.getInstance();
        writer.write(vo, output);

        // 4、设置下载需要使用到的一些属性
        byte[] bytes = output.toByteArray();
        inputStream = new ByteArrayInputStream(bytes);
        fileSize = bytes.length;
        fileName = generateFileName(user);

        return SUCCESS;
    }

    /**
     * 拼接下载文件名
     * 
     * @param user
     * @return fileName
     */
    private String generateFileName(User user) {
        String fileName = null;
        String connector = "-";
        if (qp.getGroupId() != null) {
            CproGroup group = cproGroupMgr.findCproGroupById(qp.getGroupId());
            if (group != null) {
                fileName = group.getGroupName() + connector;
            }
        } else if (qp.getPlanId() != null) {
            CproPlan plan = cproPlanMgr.findCproPlanById(qp.getPlanId());
            if (plan != null) {
                fileName = plan.getPlanName() + connector;
            }
        } else {
            fileName = user.getUsername() + connector;
        }
        fileName += this.getText("download.sublink.filename.prefix");
        try {
            fileName = StringUtils.subGBKString(fileName, 0, ReportWebConstants.FILE_MAIN_NAME_MAXLENGTH);
        } catch (UnsupportedEncodingException e1) {
            LogUtils.error(log, e1);
        }

        fileName += connector + sd.format(from) + connector + sd.format(to) + ".csv";
        try {
            // 中文文件名需要用ISO8859-1编码
            fileName = new String(fileName.getBytes(fileEncoding), "ISO8859-1");
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
        }
        return fileName;
    }

    /**
     * generateReportSummary: 生成分子链列表汇总信息
     * 
     * @param sumData
     *            汇总数据
     * @return 用于表示报表的汇总信息
     */
    private SublinkReportSumData generateReportSummary(SublinkViewItemSum sumData) {
        SublinkReportSumData sum = new SublinkReportSumData();

        sum.setSrchs(sumData.getSrchs());
        sum.setClks(sumData.getClks());
        sum.setCtr(sumData.getCtr().doubleValue());
        sum.setAcp(sumData.getAcp().doubleValue());
        sum.setCpm(sumData.getCpm().doubleValue());
        sum.setCost(sumData.getCost());
        sum.setSrchuv(sumData.getSrchuv());
        sum.setClkuv(sumData.getClkuv());
        sum.setSummaryText(this.getText("download.summary.sublink",
                new String[] { String.valueOf(sumData.getSublinkCount()) })); // 添加“合计”

        return sum;
    }

    /**
     * 生成报表头部字段名称
     * 
     * @return 报表头部字段名称数组
     */
    private String[] generateReportHeader() {
        String prefix = null;
        int maxColumn = 10;
        String[] array = new String[maxColumn];
        prefix = "download.sublink.head.col";
        for (int col = 0; col < maxColumn; col++) {
            array[col] = this.getText(prefix + (col + 1));
        }
        return array;
    }

    /**
     * 生成账户信息数据，用于分子链信息下载
     * 
     * @param userName
     * @return
     */
    private ReportAccountInfo generateAccountInfo(String userName) {
        ReportAccountInfo accountInfo = new ReportAccountInfo();

        accountInfo.setReport(this.getText("download.account.report.sublink"));
        accountInfo.setReportText(this.getText("download.account.report"));
        accountInfo.setAccount(userName);
        accountInfo.setAccountText(this.getText("download.account.account"));
        accountInfo.setDateRange(sd1.format(from) + " - " + sd1.format(to));
        accountInfo.setDateRangeText(this.getText("download.account.daterange"));

        return accountInfo;
    }

    /**
     * 由分子链列表数据生成汇总数据
     * 
     * @param infoData
     * @return 汇总展示数据
     */
    private SublinkViewItemSum calculateSumData(List<SublinkViewItem> infoData) {
        SublinkViewItemSum sumData = new SublinkViewItemSum();
        for (SublinkViewItem item : infoData) {
            sumData.setClks(sumData.getClks() + item.getClks());
            sumData.setCost(sumData.getCost() + item.getCost());
            sumData.setSrchs(sumData.getSrchs() + item.getSrchs());
            sumData.setSrchuv(sumData.getSrchuv() + item.getSrchuv());
            sumData.setClkuv(sumData.getClkuv() + item.getClkuv());
        }
        sumData.generateExtentionFields();
        if (null != infoData) {
            sumData.setSublinkCount(infoData.size());
        }

        return sumData;
    }

    /**
     * 获取olap统计数据
     * 
     * @return 包含统计数据的分子链展现list
     */
    private List<SublinkViewItem> getStatAndTransData() {

        // 获取统计数据
        List<SublinkViewItem> olapList = sublinkService.queryGroupSublinkData(userId, planIds, groupIds, null, from,
                to, null, 0, ReportConstants.TU_NONE);

        return olapList;
    }

    /**
     * 查询生成分子链列表
     * 
     * @return 分子链列表
     */
    private List<SublinkViewItem> generateSublinkList() {

        // 1、获取推广组信息
        GroupQueryParameter queryParam = initGroupQueryParameter();
        List<ExtGroupViewItem> groupItems = reportCproGroupMgr.findExtCproGroupReportInfo(queryParam);
        Map<Integer, ExtGroupViewItem> extGroupViewMapping = null;
        if (groupItems != null) {
            extGroupViewMapping = new HashMap<Integer, ExtGroupViewItem>(groupItems.size());
            for (ExtGroupViewItem tmp : groupItems) {
                extGroupViewMapping.put(tmp.getGroupId(), tmp);
            }
        } else {
            extGroupViewMapping = new HashMap<Integer, ExtGroupViewItem>(0);
        }

        // 查询统计数据
        List<SublinkViewItem> mergedData = this.getStatAndTransData();
        List<Long> sublinkIds = new ArrayList<Long>(mergedData.size());
        List<Integer> groupIds = new ArrayList<Integer>(mergedData.size());
        for (SublinkViewItem item : mergedData) {
            groupIds.add(item.getGroupId());
            sublinkIds.add(item.getSublinkId().longValue());
        }

        // 查询已有分子链
        List<SublinkInfo> sublinkInfoList = sublinkFacade.getSublinkList(userId, groupIds, sublinkIds);
        Map<Long, SublinkInfo> mapInfo = new HashMap<Long, SublinkInfo>(sublinkInfoList.size()); // 用户所选的sublink
        for (SublinkInfo sublinkInfo : sublinkInfoList) {
            mapInfo.put(sublinkInfo.getSubId(), sublinkInfo);
        }

        if (!CollectionUtils.isEmpty(mergedData)) { // 如果有统计数据
            // 9.3合并数据
            for (SublinkViewItem sublinkItem : mergedData) {

                // 将统计信息与其他信息合并

                // 合并推广组信息
                Integer groupId = sublinkItem.getGroupId();
                ExtGroupViewItem groupViewItem = extGroupViewMapping.get(groupId);
                if (groupViewItem != null) {
                    sublinkItem.setGroupName(groupViewItem.getGroupName());
                    sublinkItem.setPlanName(groupViewItem.getPlanName());
                }

                // 合并分子链信息
                String sublinkName = "未知";
                String sublinkUrl = "未知";
                SublinkInfo sublinkInfo = mapInfo.get((long) sublinkItem.getSublinkId());
                if (sublinkInfo != null) {
                    sublinkName = sublinkInfo.getSubTitle();
                    sublinkUrl = sublinkInfo.getSubUrl();
                }
                sublinkItem.setSublinkName(sublinkName);
                sublinkItem.setSublinkUrl(sublinkUrl);
            }
        }

        return mergedData;
    }

    /**
     * 返回推广组信息查询参数封装
     * 
     * @return 推广组信息查询参数
     */
    private GroupQueryParameter initGroupQueryParameter() {
        GroupQueryParameter queryParam = new GroupQueryParameter();
        queryParam.setUserId(userId);
        queryParam.setPlanId(super.qp.getPlanId());
        return queryParam;
    }

    /********************* getters and setters **************************/
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

}
