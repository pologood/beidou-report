package com.baidu.beidou.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.baidu.beidou.util.StringUtils;

/**
 * ParameterReader can be used to prepare data for TestCase
 * The input could be an m-row-n-column String (with the \n as row separater)
 * The output will be a List object which contains String array representing each row
 * @author qiaojian
 *
 */
public class ParameterReader {

	private static Log log = LogFactory.getLog(ParameterReader.class);
	
	/** 使EXPECT_COLUMN_NUM失效的值，即不验证EXPECT_COLUMN_NUM */
	public static final int INVALID_EXPECT_COLUMN_NUM = -1;
	
	/**
	 * 根据给定的Class对象，得到对应的URL
	 * 该函数使用的是开源的代码
	 * @param cls
	 * @return
	 */
	protected static URL getClassLocationURL(final Class cls){
		if (cls == null){
			log.error("[getTestDataPathByClass]：the input class is null");
			return null;
		}
		URL   result   =   null;   
        final   String   clsAsResource   =   cls.getName().replace('.','/');   
        final   ProtectionDomain   pd   =   cls.getProtectionDomain();   
        
        if   (pd   !=   null)   {   
                final   CodeSource   cs   =   pd.getCodeSource();    
                if   (cs   !=   null)   {   
                        result   =   cs.getLocation();   
                }   
                if   (result   !=   null)   {   
                    if   ("file".equals(result.getProtocol()))   {   
                            try   {   
                                if   (result.toExternalForm().endsWith(".jar")   
                                        ||   result.toExternalForm().endsWith(".zip"))   {   
                                        result   =   new   URL("jar:".concat(   
                                                        result.toExternalForm()).concat("!/")   
                                                                          .concat(clsAsResource));   
                                }   else   if   (new   File(result.getFile()).isDirectory())   {   
                                        result   =   new   URL(result,   clsAsResource);   
                                }   
                            }   catch   (MalformedURLException   ignore)   {    
                    			log.error("[getTestDataPathByClass]：Catch MalformedURLException when process the result = " + result);
                            }   
                    }   
                }   
        }   
        //补救一下
        if   (result   ==   null)   {   
                final   ClassLoader   clsLoader   =   cls.getClassLoader();   
                result=clsLoader!=null?clsLoader.getResource(clsAsResource):ClassLoader.getSystemResource(clsAsResource);   
        }   
        return result;
	}
	
	
	
	/**   
     *   获取一个测试类对应的data数据文件所在的绝对路径。      
     *   只要是在本程序中可以被加载的类，都可以定位到它的对应的data文件的绝对路径   
     *   @param   cls  一个对象的Class属性   
     *   @return   这个类的class文件位置的绝对路径。   如果没有这个类的定义，则返回null。   
     */   
   public   static   String   getTestDataPathByClass(Class   cls) {   
           String   path   =   null;   
           if   (cls   ==   null)   { 
        	   log.error("[getTestDataPathByClass]：the input class is null");
               return null;
           }   
           URL   url   =   getClassLocationURL(cls);   
           if (url != null)   {   
                   path = url.getPath();   
                   //对jar中的情况进行处理
                   if   ("jar".equalsIgnoreCase(url.getProtocol()))   {   
                           try   {   
                                   path   =   new   URL(path).getPath();   
                           }   catch   (MalformedURLException   e)   {  
                        	   log.error("[getTestDataPathByClass]：Catch MalformedURLException, path is "+path);
                        	   return null;
                           }   
                           int   location   =   path.indexOf("!/");   
                           if   (location   !=   -1)   {   
                                   path =path.substring(0,location);   
                           }   
                   }
                   File   file= new File(path);   
                   try {
					path   =   file.getCanonicalPath();
				} catch (IOException e) {
					log.error("[getTestDataPathByClass]：Catch IOException, path is "+path);
					return null;
				}   
           }   
           if (path != null) {
        	   path = (path + ".data").replace('\\', '/');
           }
           return   path;   
   }   


	
//	/**
//	 * 以指定的编码读取指定文件中的内容，返回结果是一个字符串。
//	 * @param filename
//	 * @param encoding
//	 * @return
//	 */
//	protected static String file2String(String filename, String encoding) {
//		String ret="";		
//		try { 
//			if (encoding == null || encoding.trim().equals("")) {
//				log.warn("[file2String]:The encoding is null or empty, using default encoding GBK now!");
//				encoding="GBK";
//			}
//			File f = new File(filename);
//			InputStreamReader read = new InputStreamReader(new FileInputStream(f),encoding);
//			BufferedReader xBufferedReader = new BufferedReader(read);
//			String line = null;
//			while((line = xBufferedReader.readLine()) != null) {
//				ret=ret+line+"\n";
//			}
//			xBufferedReader.close();
//			read.close();
//			return ret;	
//		} catch (Exception e) {
//			log.error("[file2String]:Encounter Exception when read the file ["+filename+"] with encoding=["+encoding+"]! Use default empty return value",e); 
//		}  
//		return ret;
//	}
	
	/**
	 * 将输入的inputString按照行分隔符和列分隔符切分整理为List对象返回，若某一行的列数不满足col_num则不会进入该List
	 * @param inputString： m行n列的字符串，行和行之间用row_sep分隔，列和列之间用col_sep分隔
	 * @param row_sep：行和行之间的分隔符，建议采用\n
	 * @param col_sep：列和列之间的分隔符，建议采用\t
	 * @param col_num：每一行的列数的校验值，不满足该列数的行将被抛弃并报警
	 * @return 一个List对象，含有m个String型数组，其中每个数组代表输入数据的一行
	 */
	protected  static List loadParameterFromString (String inputString, String row_sep , String col_sep, int expect_col_num){
		
		if (inputString == null) {
			log.error("[loadParameterFromString]: inputString is null error, return NULL"); 
			return null ;
		}
		if (row_sep == null || row_sep.equals("")){
			log.error("[loadParameterFromString]: row_sep is null or not valid error, return NULL"); 
			return null ;
		}
		if (col_sep == null || col_sep.equals("")){
			log.error("[loadParameterFromString]: col_sep is null or not valid error, return NULL"); 
			return null ;
		}
		if (expect_col_num < 1 && INVALID_EXPECT_COLUMN_NUM != expect_col_num){
			log.error("[loadParameterFromString]: input column number is not valid ( ="+expect_col_num+"), return NULL"); 
			return null ;
		}
		if (row_sep.equals(col_sep)){
			log.error("[loadParameterFromString]: col_sep is invalid (same as row_sep), return NULL"); 
			return null ;
		}
		
		ArrayList retList = new ArrayList();
		String[] row_arr =  inputString.split(row_sep,-1);
		
		for (int index=0 ; index<row_arr.length; index++) {
			if (row_arr[index]==null || row_arr[index].startsWith("#")) continue;  //跳过注释行
			String [] col_arr = row_arr[index].split(col_sep,-1);
			if (col_arr.length != expect_col_num  && INVALID_EXPECT_COLUMN_NUM != expect_col_num) {
				log.warn("[loadParameterFromString]: skip the row ["+row_arr[index]+"], col_sep=["+col_sep+"], expect_col_num="+expect_col_num); 
				continue;
			}
			
			retList.add(col_arr);
		}
		return retList; 
	}
	
	/**
	 * 使用默认的行列分隔符读取数据
	 * 默认的行分隔符为\n
	 * 默认的列分隔符为\t
	 * @param inputString
	 * @param col_num
	 * @return
	 */
	public  static List loadParameterFromString (String inputString, int expect_col_num){
		String default_row_sep = "\n";
		String default_col_sep = "\t";
		return loadParameterFromString(inputString,default_row_sep,default_col_sep,expect_col_num);
	}
		
	/**
	 * 使用默认的行列分隔符，从默认的文件位置载入数据返回List对象
	 * @param xClass   当前测试类的class对象  
	 * @param expect_col_num	期望的列数
	 * @return	含有结果的List对象
	 */
	public static List loadParameterFromFile(Class xClass , int expect_col_num) {
		if (xClass == null) {
			log.error("[loadParameterFromFile]: the Input Class instance is null error");  
			return null ;
		} 
		String filePath =  ParameterReader.getTestDataPathByClass(xClass);
		return loadParameterFromFile(filePath,expect_col_num);
	}
	

	public static List loadParameterFromFile(String filePath , int expect_col_num) {
		String encoding = "UTF-8";
		String col_sep = "\t";

		if (expect_col_num < 1 && INVALID_EXPECT_COLUMN_NUM != expect_col_num){
			log.error("[loadParameterFromString]: input column number is not valid ( ="+expect_col_num+"), return NULL"); 
			return null ;
		}

		ArrayList retList = new ArrayList();
		try { 
			if (encoding == null || encoding.trim().equals("")) {
				log.warn("[file2String]:The encoding is null or empty, using default encoding GBK now!");
				encoding="GBK";
			}
			File f = new File(filePath);
			InputStreamReader read = new InputStreamReader(new FileInputStream(f),encoding);
			BufferedReader xBufferedReader = new BufferedReader(read);
			String line = null;
			int index=0;
			while((line = xBufferedReader.readLine()) != null) {
				index++;
				if (StringUtils.isEmpty(line) || line.startsWith("#")) continue;  //跳过注释行
				String [] col_arr = line.split(col_sep,-1);
				if (col_arr.length != expect_col_num  && INVALID_EXPECT_COLUMN_NUM != expect_col_num) {
					log.warn("[loadParameterFromString]: skip the row [" + index + "], col_sep=["
							+ col_sep + "], expect_col_num=" + expect_col_num); 
					continue;
				}
				
				retList.add(col_arr);
			}
			xBufferedReader.close();
			read.close();
			return retList;	
		} catch (Exception e) {
			log.error("[file2String]:Encounter Exception when read the file [" + filePath + "] with encoding=["
					+ encoding + "]! Use default empty return value",e); 
		}  
		
		return retList; 
	}
	
	/*
	public static void main(String args[]) { 
		//System.out.print("the output is:" + ParameterReader.getTestDataPathByClass(ParameterReader.class));
		List xList = loadParameterFromFile(ParameterReader.class,3);
		for(int i=0; i<xList.size();i++) {
			String [] xArr = (String[]) xList.get(i); 
			System.out.println(""+xArr.length);
		}
	}
	*/
}
