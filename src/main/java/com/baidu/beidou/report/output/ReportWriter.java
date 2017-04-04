package com.baidu.beidou.report.output;

import java.io.IOException;
import java.io.OutputStream;

import com.baidu.beidou.report.vo.AbstractReportVo;

/**
 * 写个接口，万一以后PM不想要CSV
 * @author lingbing
 *
 */
public interface ReportWriter {

	/**
	 * 根据报表数据生成报告文件，并返回token（可以是文件名）
	 * @param report
	 * @return
	 */
	public String write(AbstractReportVo report) throws IOException;
	
	/**
	 * 根据报表数据生成报告的流，并返回流的size
	 * @param report
	 * @param outputStream 输入流数据，必须由使用者new好，且由使用者close
	 * @return
	 */
	public void write(AbstractReportVo report, OutputStream outputStream) throws IOException;
	
	
}
