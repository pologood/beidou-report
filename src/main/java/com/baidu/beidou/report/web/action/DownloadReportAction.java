package com.baidu.beidou.report.web.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import com.baidu.beidou.report.output.impl.ReportCSVWriter;
import com.baidu.beidou.util.StringUtils;
import com.opensymphony.xwork2.ActionSupport;

public class DownloadReportAction extends ActionSupport{

	private static final long serialVersionUID = 6536679615477851560L;
	
	private String fileName = StringUtils.fileNameEncode("优化报告.xls");
	
	private long fileSize;
	
	private InputStream inputStream = new ByteArrayInputStream("ERROR".getBytes());;
	
	public String execute(){
		ByteArrayOutputStream output = null; 
		try{			
			output = new ByteArrayOutputStream();
			ReportCSVWriter.getInstance().write(null, output);
			inputStream = new ByteArrayInputStream(output.toByteArray());
		}catch(Exception ex){
			return SUCCESS;
		}finally{
			try{				
				if(output != null){
					output.close();
				}
				if(inputStream != null){
					inputStream.close();
				}
			}catch(Exception ex){
			}
		}
		return SUCCESS;
	}
	
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}
}
