package com.sxit.crawler.app.mm10086;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import com.sxit.crawler.commons.BeanHelper;
import com.sxit.crawler.commons.jdbc.DatatableOperator;
import com.sxit.crawler.core.fetch.FetchEntityBuilder;
import com.sxit.crawler.core.fetch.FetchEntry;
import com.sxit.crawler.core.fetch.FetchHTTP;
import com.sxit.crawler.core.fetch.SimpleUserAgentProvider;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.module.CrawlModule;
import com.sxit.crawler.utils.JsoupUtils;

public class Mm10086CrawlerModeul extends CrawlModule{
	
	private static Logger log = LoggerFactory.getLogger(Mm10086CrawlerModeul.class);
	private final static String DEFAULT_JOB_NAME = Mm10086CrawlerModeul.class.getSimpleName();
	public static final String USER_AGENT_STRING = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.72 Safari/537.36";
	public static final UserAgentProvider USER_AGENT_PROVIDER = new SimpleUserAgentProvider(DEFAULT_JOB_NAME, USER_AGENT_STRING);
	private String sql="select count(mmid) from tbas_mm where headurl='http://apk.mmarket.com' and url like ?";
	private DatatableOperator datatableOperator;
	private int total=0;
	private int downloadNum=0;
	private UrlProxyWithIpPoolUtils urlUtils;
	private Mm10086Config config;
	public Mm10086CrawlerModeul(){
		JdbcTemplate jdbcTemplate = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
		this.datatableOperator = new DatatableOperator(initDatatableConfig("TBAS_MM_10086.xml"), jdbcTemplate);
		urlUtils=(UrlProxyWithIpPoolUtils) BeanHelper.getBean("urlProxyWithIpPoolUtils");
		this.config=(Mm10086Config) BeanHelper.getBean("mm10086Config");
	}
	
	@Override
	public void execute() {
		extr();
	}
	private void extr(){
		//seeds url
		List<String> list=getUrlList();
		for(int x=0;x<=list.size();x++){
			String seedUrl=list.get(x);
			int pageNum=Mm10086Utils.getTotal(seedUrl);
			for(int y=1;y<=pageNum;y++){
				String url=seedUrl+"&p="+y;
				//http://mm.10086.cn/android/software?pay=1&p=1&screen=2
				for(int screen=1;screen<=4;screen++){
					String finalUrl=url+"&screen="+screen;
					FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(finalUrl,USER_AGENT_PROVIDER);
					try {
						FetchHTTP fetchHTTP = new FetchHTTP();
						fetchEntry=fetchHTTP.process(fetchEntry);
						
						if (fetchEntry.getResult() != null) {
							StringBuffer sb=fetchEntry.getResult().getPageContent();
							String html=sb.toString();
							analyse(html);
							if(checkDownloadNum()){
								log.warn("It is time to stop program,download NUM is"+this.total);
								return;
							}
						}
					} catch (Exception e) {
						continue;
					}
				}
			}
		}
	}
	/**
	 * get seeds url
	 * @return
	 */
	private List<String> getUrlList(){
		List<String> list=config.getList();
		return list;
	}
	private boolean checkDownloadNum(){
		if(this.total>(this.urlUtils.getIpPool().size()*90)){
			return true;
		}
		return false;
	}
	private void analyse(String html){
		Document doc=Jsoup.parse(html);
		Elements elements=doc.select("ul#container>li");
		for(Element element:elements){
			try {

				String url=element.select("div.part-1>div.info>a").attr("href");
				if(url!=null){
					url="http://mm.10086.cn"+url;
					if(url.indexOf("?")!=-1){
						url=StringUtils.substringBefore(url, "?");
					}
					//if(!exsitDownload(url)){
					//	log.info("url do not exist--->"+url);
						analyseDetailUrl(url);
						if(checkDownloadNum()){
							break;
						}
					//}else{
					//	log.info("url exist--->"+url);
					//}
				}
			} catch (Exception e) {
				log.warn(e.getLocalizedMessage());
				continue;
			}
		}
	}
	/**
	 * check data
	 * @param url
	 * @return
	 */
	private boolean exsitDownload(String url){
		String headUrl=StringUtils.substringBeforeLast(url, "?")+"%";
		return datatableOperator.existsData(sql,new Object[]{headUrl});
	}
	/**
	 * analyse detail url
	 * @param url
	 */
	private void analyseDetailUrl(String url){
		//http://mm.10086.cn/android/info/227397.html
		FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(url,USER_AGENT_PROVIDER);
		try {
			FetchHTTP fetchHTTP = new FetchHTTP();
			fetchEntry=fetchHTTP.process(fetchEntry);
			
			if (fetchEntry.getResult() != null) {
				analyseResult(fetchEntry);
			}
		} catch (Exception e) {
			log.warn(e.getMessage());
		}
	}
	//extract data
	private void analyseResult(FetchEntry fetchEntry){
		String html=fetchEntry.getResult().getPageContent().toString();
		Document doc=Jsoup.parse(html);
		Element element=doc.select("div#android-details").first();
		
		try {
			List<Map<String, Object>> recordList = new ArrayList<Map<String,Object>>();
			
			String mmid=null;
			String mmtitle=JsoupUtils.extrFirstText(element, "h2#apptitle");
			String version=JsoupUtils.extrFirstText(element, "span#appversion");
			String classname=element.select("ul>li").get(1).select("span").get(1).text();
			String parentclassname=element.select("ul>li").get(1).select("span").get(1).text();
			String url=fetchEntry.getUrl();
			String headurl="http://apk.mmarket.com";
			String bidname="";
			String webmmid=null;
			String domain="mmarket.com";
			String domainname="\u79FB\u52A8\u5E94\u7528\u5546\u57CE";
			String createdate=null;
			String downloadurl=element.select("li.mt20>a").get(1).attr("href");
			if(downloadurl!=null){
				downloadurl=urlUtils.getUrl(downloadurl);
				add();
				if(StringUtils.isNotBlank(downloadurl)){
					webmmid=StringUtils.substringAfterLast(downloadurl, "/");
					if(StringUtils.isBlank(webmmid)){
						return;
					}
					if(webmmid.indexOf("apk")==-1){
						return;
					}
				}else{
					return;
				}
			}
			String beginstr="";
			String endstr="";
			Long classid=10L;
			Long featuretype=2L;
			
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
			
			boolean save=datatableOperator.saveData(recordList, true);
			if(save){
				this.downloadNum++;
				log.info("successfull saved ");
			}else{
				log.info("save failed");
			}
		log.info("MM10086 downloadurl num:"+this.total);
		log.info("MM10086 save num:"+this.downloadNum);
		} catch (Exception e) {
			log.warn(e.getLocalizedMessage());
		}
	}
	private void add(){
		this.total++;
	}
}
