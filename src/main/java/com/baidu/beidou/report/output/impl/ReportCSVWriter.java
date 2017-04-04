package com.baidu.beidou.report.output.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import com.Ostermiller.util.ExcelCSVPrinter;
import com.baidu.beidou.report.constant.QueryParameterConstant;
import com.baidu.beidou.report.output.ReportWriter;
import com.baidu.beidou.report.vo.AbstractReportVo;
import com.baidu.beidou.report.vo.ReportAccountInfo;
import com.baidu.beidou.report.vo.StandardCustomReportVo;
import com.baidu.beidou.report.vo.plan.PlanReportSumData;
import com.baidu.beidou.report.vo.plan.PlanReportVo;
import com.baidu.beidou.olap.vo.PlanViewItem;

public class ReportCSVWriter implements ReportWriter{

	private static final String encoding = "gbk";
	private static final String fileDir = "E:\\";
	private static final String subfix = ".csv";
	
	private ReportCSVWriter(){
		super();
	}
	
	private static final ReportWriter writer = new ReportCSVWriter();
	public static ReportWriter getInstance(){
		return writer;
	}
	
	public String write(AbstractReportVo report) throws IOException{
		if(report == null ){
			return null;
		}		
		String token = String.valueOf(System.nanoTime());
		String filePath = fileDir + token + subfix;
		OutputStreamWriter out = null;
		ExcelCSVPrinter printer = null;
		try{
			File file = new File(filePath);
			if (!file.exists()) {
				file.createNewFile();
			} 
			out = new OutputStreamWriter(new FileOutputStream(filePath), encoding);
			printer = new ExcelCSVPrinter(out);			

			//输出账户信息
			for (String[] acctInfo : report.getCsvReportAccountInfo() ) {
				printer.writeln(acctInfo);
			}
			
			//输出一个空行
			printer.writeln();
			
			//输出表头
			printer.writeln(report.getCsvReportHeader());
			
			//输出详情
			for(String[]  detail : report.getCsvReportDetail()){
				printer.writeln(detail);
			}	
			
			//输出汇总
			printer.writeln(report.getCsvReportSummary());
	
		} catch(IOException io) {
			throw io;
		} finally {
			try{
				if(printer != null){
					printer.close();
				}
				if(out != null){
					out.close();
				}
			}catch(Exception ex){
				return null;
			}
		}
		return token;
		
	}

	public void write(AbstractReportVo report, OutputStream outputStream) throws IOException {
		
		if(report == null ){
			return;
		}		
		if(report instanceof StandardCustomReportVo){
			this.writeStandardReport((StandardCustomReportVo)report, outputStream);
			return;
		}
		
		ExcelCSVPrinter printer = null;
		OutputStreamWriter writer = null;
		try{			
			writer = new OutputStreamWriter(outputStream, "GBK");
			printer = new ExcelCSVPrinter(writer);
			
			//byte[] bomBytes = {(byte)0xEF, (byte)0xBB, (byte)0xBF};
			//String bom = new String(bomBytes);
			//printer.writeln(bom);
			
			if(report.getAccountInfo() != null) {
				//输出账户信息
				for (String[] acctInfo : report.getCsvReportAccountInfo() ) {
					printer.writeln(acctInfo);
				}
				
				//输出一个空行
				printer.writeln();
			}
			
			//输出表头
			printer.writeln(report.getCsvReportHeader());
			
			//输出详情
			for(String[]  detail : report.getCsvReportDetail()){
				printer.writeln(detail);
			}	
			
			//输出汇总
			printer.writeln(report.getCsvReportSummary());
			
		}catch(IOException io){
			throw io;
		}finally{
			try{
				if(printer != null){
					printer.close();
				}				
			}catch(Exception ex){
			}
		}
	}
	
	private void writeStandardReport(StandardCustomReportVo report, OutputStream outputStream) throws IOException {
		if(report == null ){
			return;
		}		
		ExcelCSVPrinter printer = null;
		OutputStreamWriter writer = null;
		try{			
			writer = new OutputStreamWriter(outputStream, "GBK");
			printer = new ExcelCSVPrinter(writer);
			
			if(report.getAccountInfo() != null) {
				//输出账户信息
				for (String[] acctInfo : report.getCsvReportAccountInfo() ) {
					printer.writeln(acctInfo);
				}
				
				//输出一个空行
				printer.writeln();
			}
			
			/******账户层级******/
			//输出账户层级表名
			printer.writeln(report.getUserLevelReportName());
			//输出账户层级表头
			printer.writeln(report.getCsvUserLevelHeader());
			//输出详情
			for(String[]  detail : report.getCsvUserLevelDetail()){
				printer.writeln(detail);
			}	
			//输出一个空行
			printer.writeln();
			
			/******推广计划层级******/
			//输出账户层级表名
			printer.writeln(report.getPlanLevelReportName());
			//输出账户层级表头
			printer.writeln(report.getCsvPlanLevelHeader());
			//输出详情
			for(String[]  detail : report.getCsvPlanLevelDetail()){
				printer.writeln(detail);
			}	
			//输出一个空行
			printer.writeln();
			
			/******推广组层级******/
			//输出账户层级表名
			printer.writeln(report.getGroupLevelReportName());
			//输出账户层级表头
			printer.writeln(report.getCsvGroupLevelHeader());
			//输出详情
			for(String[]  detail : report.getCsvGroupLevelDetail()){
				printer.writeln(detail);
			}	
			
		}catch(IOException io){
			throw io;
		}finally{
			try{
				if(printer != null){
					printer.close();
				}				
			}catch(Exception ex){
			}
		}
	}
	
	public static void main(String[] args) throws IOException{

		System.out.println("test");
	}

}
