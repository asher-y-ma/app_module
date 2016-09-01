package com.sxit.crawler.app.myapp;

import java.net.SocketTimeoutException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sxit.crawler.commons.exception.ConnectTimeOutException;
import com.sxit.crawler.commons.exception.ReadTimeOutException;

public class MyappUrlUtils {

	/**
	 * 关闭自动转向，获取真正下载url
	 * @param args
	 */
	private Logger log = LoggerFactory.getLogger(getClass());
	public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; rv:25.0) Gecko/20100101 Firefox/25.0";
	private HttpClient httpclient=null;
	public MyappUrlUtils(){
		httpclient= new DefaultHttpClient(new ThreadSafeClientConnManager());
		httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, DEFAULT_USER_AGENT);
		httpclient.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 6000);
		httpclient.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 60000);
		httpclient.getParams().setBooleanParameter("http.protocol.handle-redirects", false);
	}
	public String getUrl(String url){
		String url1=getLocation(url);
		String resultUrl=null;
		if(url1!=null){
			if(url1.indexOf(".apk")!=-1){
				resultUrl=url1;
			}else{
				return url1=getUrl(url1);
			}
		}
		return resultUrl;
	}
	private String getLocation(String url){
		HttpGet httpget=null;
		String reUrl=null;
		try {
			httpget = new HttpGet(url);
			HttpResponse response = httpclient.execute(httpget);
			int statusCode=response.getStatusLine().getStatusCode();
			if(statusCode==302 || statusCode==301){
				reUrl=response.getFirstHeader("Location").getValue();
			}
		}catch (ConnectTimeoutException ex) {
			throw new ConnectTimeOutException("HttpClient Connection TimeOut! ",ex);
		}catch(SocketTimeoutException ex){
			throw new ReadTimeOutException("HttpClient Read TimeOut!",ex);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		} finally {
			try{
				httpget.abort();
			}catch(Exception e){
				log.error("httpget closed error:",e);
			}
		}
		return reUrl;
	}
}
