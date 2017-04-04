package com.baidu.beidou.report.service;

import com.baidu.beidou.aot.control.AotReportItem;

public interface BeidouAotService {

	AotReportItem queryReportItem(int userId, int aotItemId, int planId, int groupId, int userType);
}
