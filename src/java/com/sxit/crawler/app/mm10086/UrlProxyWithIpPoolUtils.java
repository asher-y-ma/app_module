package com.sxit.crawler.app.mm10086;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sxit.crawler.commons.exception.ConnectTimeOutException;
import com.sxit.crawler.commons.exception.ReadTimeOutException;

/**
 * 关闭自动转向，获取真正下载url(代理)
 * @param args
 */
public class UrlProxyWithIpPoolUtils {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; rv:25.0) Gecko/20100101 Firefox/25.0";
	
	private List<Map<String,String>> ipPool;
	private int ipNum;
	
	public List<Map<String, String>> getIpPool() {
		return ipPool;
	}
	public void setIpPool(List<Map<String, String>> ipPool) {
		this.ipPool = ipPool;
	}
	private void switchIp(HttpClient httpclient){
		if(this.ipPool==null){
			log.warn("proxy ip table is null");
		}else if(this.ipPool.size()<1){
			log.warn("proxy ip table is null");
		}
		for(;this.ipNum<this.ipPool.size();){
			Map<String,String> map=this.ipPool.get(this.ipNum);
			String proxyIp=String.valueOf(map.get("ip"));
			int proxyPort=Integer.parseInt(map.get("port")) ;
			HttpHost proxy;
			proxy = new HttpHost(proxyIp,proxyPort,"http");
			httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
			this.ipNum++;
			if(this.ipNum==this.ipPool.size()){
				this.ipNum=0;
			}
			break;
		}
	}
	public String getUrl(String url){
		HttpClient httpclient=null;
		String resultUrl=null;
		try {
			httpclient= new DefaultHttpClient(new ThreadSafeClientConnManager());
			httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, DEFAULT_USER_AGENT);
			httpclient.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 20000);
			httpclient.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 60000);
			httpclient.getParams().setBooleanParameter("http.protocol.handle-redirects",false);
			
			switchIp(httpclient);
			
			Map<String,String> para=getFirstLocation(httpclient,url);
			String url1=para.get("Location");
			String cookie=para.get("cookie");
			
			if(url1!=null){
				if(url1.indexOf(".apk")!=-1){
					resultUrl=url1;
				}else{
					return url1=getSecondLocation(httpclient,url1,cookie);
				}
			}
		} catch (Exception e) {
			log.warn(e.getMessage());
		} finally {
			try{
				httpclient.getConnectionManager().shutdown();
			}catch(Exception e){
				log.error("httpclient closed error:",e);
			}
		}
		
		return resultUrl;
	}
	private Map<String,String> getFirstLocation(HttpClient httpclient,String url){
		log.info("switch ip----->"+httpclient.getParams().getParameter("http.route.default-proxy"));
		Map<String,String> para=new HashMap<String,String>();
		HttpGet httpget=null;
		try {
			httpget=new HttpGet();
			httpget.setURI(new URI(url));
			
			HttpResponse response = httpclient.execute( httpget);
			int statusCode=response.getStatusLine().getStatusCode();
			if(statusCode==302 || statusCode==301){
				//get Location
				String reUrl=response.getFirstHeader("Location").getValue();
				 //get cookie
				String set_cookie = response.getLastHeader("Set-Cookie").getValue();
				if(set_cookie.indexOf("dCookie")==-1){
					log.warn("-------cookie error!--------");
				}
				String dCookie=set_cookie.substring(0,set_cookie.indexOf(";"));
				para.put("Location", reUrl);
				para.put("cookie", dCookie);
			}
		}catch (ConnectTimeoutException ex) {
			throw new ConnectTimeOutException("HttpClient Connection TimeOut! ",ex);
		}catch(SocketTimeoutException ex){
			throw new ReadTimeOutException("HttpClient Read TimeOut!",ex);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		return para;
	}
	private String getSecondLocation(HttpClient httpclient,String url,String cookie){
		String reUrl=null;
		HttpGet httpget=null;
		try {
			httpget=new HttpGet();
			httpget.setURI(new URI(url));
			httpget.setHeader("cookie", cookie);
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
