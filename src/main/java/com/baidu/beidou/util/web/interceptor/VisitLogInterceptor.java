package com.baidu.beidou.util.web.interceptor;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;

import com.baidu.beidou.user.bo.Visitor;
import com.baidu.beidou.user.constant.UserConstant;
import com.baidu.beidou.user.constant.UserWebConstant;
import com.baidu.beidou.util.CookieUtils;
import com.baidu.beidou.util.HttpUtils;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

/**
 * 2010-04-23 记录访问北斗的所有动态请求
 * 
 * @author yangyun
 * @version 1.2.8
 */
public class VisitLogInterceptor extends AbstractInterceptor {

    private static final long serialVersionUID = -6862081304411513562L;

    private static final Log LOG = LogFactory.getLog("visitaccess");

    private static final String UC_ST_KEY = "__cas__st__3";

    private static final String SIGNIN_UC_KEY = "SIGNIN_UC";

    @Override
    public String intercept(ActionInvocation arg0) throws Exception {

        HttpServletRequest request = ServletActionContext.getRequest();
        HttpSession session = request.getSession();
        ActionContext context = ActionContext.getContext();

        String ip = HttpUtils.getHttpForwardIp(request);
        Visitor loginer = (Visitor) session.getAttribute(UserWebConstant.USER_KEY);
        String browser = HttpUtils.getHttpBrowser(request);
        String url = HttpUtils.getFullURL(request);

        String result = arg0.invoke();

        @SuppressWarnings("unchecked")
        List<Integer> userids = (List<Integer>) context.get(UserConstant.USER_PRIVILEGE_KEY);

        String ucst = CookieUtils.getCookieValue(request, UC_ST_KEY);
        String suc = CookieUtils.getCookieValue(request, SIGNIN_UC_KEY);

        LOG.info(getVisitorInfo(ip, loginer, userids, browser, url, ucst, suc));

        return result;
    }

    /**
     * 构造日志信息
     * 
     * @author yangyun
     * @version 1.2.18
     * @param time
     *            访问时间
     * @param useTime
     *            服务器处理时间
     * @param ip
     *            访问者IP
     * @param loginer
     *            访问者
     * @param userids
     *            被操作者ID列表
     * @param browser
     *            访问者浏览器信息
     * @param url
     *            访问地址,包括参数信息
     * @return
     */
    private String getVisitorInfo(String ip, Visitor loginer, List<Integer> userids,
                    String browser, String url, String ucst, String suc) {
        StringBuilder sb = new StringBuilder();
        sb.append("IP=").append(null != ip ? ip : "unknown").append("\t");
        if (loginer == null) {
            sb.append("visitor=[]\t");
        } else {
            sb.append("visitor=[").append(loginer.getUserid()).append(",")
                            .append(loginer.getRoles()).append("]\t");
        }
        sb.append("userids=").append(getUsers(userids)).append("\t");
        sb.append("url=[").append(url).append("]\t");
        sb.append("browser=[").append(browser).append("]\t");
        sb.append("ucst=[").append(ucst).append("]\t");
        sb.append("suc=[").append(suc).append("]");
        return sb.toString();
    }

    /**
     * ID列表去重
     * 
     * @author yangyun
     * @version 1.2.18
     * @param userids
     * @return
     */
    private Set<Integer> getUsers(List<Integer> userids) {
        Set<Integer> set = new HashSet<Integer>();

        if (userids != null && userids.size() > 0) {
            for (Integer id : userids) {
                set.add(id);
            }
        }

        return set;
    }

}
