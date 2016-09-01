package com.sxit.crawler.app.app91;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sxit.crawler.commons.BeanHelper;
import com.sxit.crawler.commons.jdbc.DatatableOperator;
import com.sxit.crawler.core.fetch.FetchEntityBuilder;
import com.sxit.crawler.core.fetch.FetchEntry;
import com.sxit.crawler.core.fetch.FetchHTTP;
import com.sxit.crawler.core.fetch.SimpleUserAgentProvider;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.module.CrawlModule;

public class A91AppCrawlerModeul extends CrawlModule{
	
	private static Logger log = LoggerFactory.getLogger(A91AppCrawlerModeul.class);

	private final static String DEFAULT_JOB_NAME = A91AppCrawlerModeul.class.getSimpleName();

	public static final String USER_AGENT_STRING = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.72 Safari/537.36";
	
	public static final UserAgentProvider USER_AGENT_PROVIDER = new SimpleUserAgentProvider(DEFAULT_JOB_NAME, USER_AGENT_STRING);
	
	private final static Gson GSON = new GsonBuilder().enableComplexMapKeySerialization().create();
	
	private DatatableOperator datatableOperator;
	private App91Config config;
	private A91AppUrlUtils a91AppUrlUtils;
	
	public A91AppCrawlerModeul(){
		JdbcTemplate jdbcTemplate = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
		this.datatableOperator = new DatatableOperator(initDatatableConfig("TBAS_MM.xml"), jdbcTemplate);
		this.config=(App91Config) BeanHelper.getBean("app91config");
		a91AppUrlUtils=new A91AppUrlUtils();
	}
	
	@Override
	public void execute() {
		extr();
	}
	@SuppressWarnings("serial")
	private void extr(){
		String seedUrl=getUrlList();
		int x=config.getStartClassId();
		int xEnd=config.getEndClassId();
		for(;x<=xEnd;x++){
			for(int y=1;y<=100;y++){
				String url=seedUrl;
				url=url.replace("[first]", String.valueOf(x));
				url=url.replace("[second]", String.valueOf(y));
				FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(url);
				try {
					FetchHTTP fetchHTTP = new FetchHTTP();
					fetchEntry=fetchHTTP.process(fetchEntry);
				} catch (Exception e) {
					continue;
				}
				if (fetchEntry.getResult() != null) {
					try {
						StringBuffer sb=fetchEntry.getResult().getPageContent();
						String json=sb.toString();
						App91Model date = GSON.fromJson(json, new TypeToken<App91Model>(){}.getType());
						
						boolean ifContinue=analyse(date,x);
						if(!ifContinue){
							break;
						}
						log.info(new StringBuffer().append("分类id-->").append(x).append(",分页id-->").append(y).toString());
					} catch (Exception e) {
						log.info("------------->没有此分页:分类id为="+x+"  分页id为="+y);
					}
				}
			}
		}
	}
	private String getUrlList(){
		return "http://bbx2.sj.91.com/soft/phone/list.aspx?act=282&tagid=[first]&title=%e7%b3%bb%e7%bb%9f%e5%ae%89%e5%85%a8&pact=213&mt=4&sv=3.7.1&pid=2&osv=4.2.2&cpu=armeabi-v7a,armeabi&st=5&sessionid=zEK5GLga2EXJ3YKnSn0e2OZXj%2fsr131mR0IHS7wFqzfzLEd80kyVuXhA0dZLQkjql13%2fL0r7kkY%3d&imei=358094050694935&imsi=460029197613725&nt=10&dm=L39h&chl=jZg3GleBqu9XdELIGKzVcuOA4hey28b6&pi=[second]";
	}
	private boolean analyse(App91Model date,int classId){
		String code=date.getCode();
		String errorDesc=String.valueOf(date.getErrorDesc());
		if("0".equals(code) && "sys_success".equals(errorDesc)){
			//继续执行
			try {
				List<Map<String,String>> items=date.getResult().getItems();
				
				List<Map<String, Object>> recordList = new ArrayList<Map<String,Object>>();
				for(int x=0;x<items.size();x++){
					try {
						Map<String,String> item=items.get(x);
						String mmid=null;
						String mmtitle=item.get("name");
						String version=item.get("versionName");
						String classname=date.getResult().getTag().get("name");
						String parentclassname="\u0039\u0031\u52A9\u624B";
						String url=item.get("detailUrl");
						String headurl="http://hiapk.91rb.com";
						String bidname="";
						String webmmid=null;
						String domain="91rb.com";
						String domainname="\u0039\u0031\u52A9\u624B";
						String createdate=null;
						String downloadurl=item.get("downloadUrl");
						String beginstr="";
						String endstr="";
						Long classid=new Long(classId);
						Long featuretype=2L;
						
						if(StringUtils.isBlank(classname)){
							classname="\u5176\u4ED6";
						}
						downloadurl=a91AppUrlUtils.getUrl(downloadurl);
						
						if(StringUtils.isNotBlank(downloadurl)){
							webmmid=StringUtils.substringAfterLast(downloadurl, "/");
							if(StringUtils.isBlank(webmmid)){
								continue;
							}
							if(webmmid.indexOf("apk")==-1){
								continue;
							}
						}else{
							continue;
						}
						
						Map<String, Object> row = new HashMap<String, Object>();
						row.put("MMID", mmid);
						row.put("MMTITLE", mmtitle);
						row.put("VERSION", version);
						row.put("CLASSNAME", classname);
						row.put("PARENTCLASSNAME", parentclassname);
						row.put("URL", url);
						row.put("HEADURL", headurl);
						row.put("BIDNAME", bidname);
						row.put("WEBMMID", webmmid);
						row.put("DOMAIN", domain);
						row.put("DOMAINNAME", domainname);
						row.put("CREATEDATE", createdate);
						row.put("DOWNLOADURL", downloadurl);
						row.put("BEGINSTR", beginstr);
						row.put("ENDSTR", endstr);
						row.put("CLASSID", classid);
						row.put("FEATURETYPE", featuretype);
						recordList.add(row);
					} catch (Exception e) {
						log.warn(e.getMessage());
					}
				}
				if(recordList.size()>0){
					datatableOperator.saveData(recordList, true);
				}
			} catch (Exception e) {
				log.error(e.getLocalizedMessage());
				return true;
			}
		}else{
			//出错误，或链接失效
			return false;
		}
		return true;
	}
}
class App91Model{
	private String Code;
	private String ErrorDesc;
	private App91ModelResult Result;
	public String getCode() {
		return Code;
	}
	public void setCode(String code) {
		Code = code;
	}
	public String getErrorDesc() {
		return ErrorDesc;
	}
	public void setErrorDesc(String errorDesc) {
		ErrorDesc = errorDesc;
	}
	public App91ModelResult getResult() {
		return Result;
	}
	public void setResult(App91ModelResult result) {
		Result = result;
	}
}
class App91ModelResult{
	private String atLastPage;
	private Map<String,String> tag;
	private Object cates;
	private List<Map<String,String>> items;
	public String getAtLastPage() {
		return atLastPage;
	}
	public void setAtLastPage(String atLastPage) {
		this.atLastPage = atLastPage;
	}
	public Map<String, String> getTag() {
		return tag;
	}
	public void setTag(Map<String, String> tag) {
		this.tag = tag;
	}
	
	public Object getCates() {
		return cates;
	}
	public void setCates(Object cates) {
		this.cates = cates;
	}
	public List<Map<String, String>> getItems() {
		return items;
	}
	public void setItems(List<Map<String, String>> items) {
		this.items = items;
	}
}