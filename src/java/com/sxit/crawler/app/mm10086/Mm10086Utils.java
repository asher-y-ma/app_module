package com.sxit.crawler.app.mm10086;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.sxit.crawler.core.fetch.FetchEntityBuilder;
import com.sxit.crawler.core.fetch.FetchEntry;
import com.sxit.crawler.core.fetch.FetchHTTP;

public class Mm10086Utils {
	
	/**
	 * 根据url获取网页，并分析出分页总数，返回0表示未分析出结果
	 * @param url
	 * @return
	 */
	public static int getTotal(String url){
		int total=0;
		FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(url);
		try {
			FetchHTTP fetchHTTP = new FetchHTTP();
			fetchEntry=fetchHTTP.process(fetchEntry);
			if (fetchEntry.getResult() != null) {
				String text=fetchEntry.getResult().getPageContent().toString();
				Document doc=Jsoup.parse(text);
				Element element=doc.select("div.list-page>a.last").first();
				String hrefUrl=element.attr("href").toString();
				String result=StringUtils.substringAfterLast(hrefUrl, "p=");
				total=Integer.parseInt(result);
			}
		} catch (Exception e) {
			return 0;
		}
		
		return total;
	}
}
