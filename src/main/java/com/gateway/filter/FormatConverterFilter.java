package com.gateway.filter;

import com.netflix.zuul.context.RequestContext;
import com.gateway.redis.ListenerService;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static com.netflix.zuul.context.RequestContext.getCurrentContext;

public abstract class FormatConverterFilter extends CommonFilter {

	private static Logger log = LoggerFactory.getLogger(FormatConverterFilter.class);

	@Autowired
	private ListenerService listenerService;

	@Override
	public boolean shouldFilter() {
		return false;
	}

	//@Override
	public boolean shouldFilter2() {
		boolean should = false;
		if(super.shouldFilter()) {
			RequestContext ctx = getCurrentContext();
			String requestUri = ctx.getRequest().getRequestURI().toString();

			String key = createKey(getPrefix(), requestUri);
			String redisRtn = listenerService.get(key);
			if (ctx.getRequest().getContentType() != null && redisRtn != null) {
				if (redisRtn.equals("true")) {
					should = true;
				}
			}
		}
		return should;
	}

	/**
	 * Redis key 값 prefix 리턴.
	 * @return
	 */
	protected abstract String getPrefix();

	public String getJsonToXml(String JsonStr) {
		JSONObject json = new JSONObject(JsonStr);
		String xml = XML.toString(json);
		return xml;
	}

	public String getXmlToJson(String XmlStr) {
		String jsonPrettyPrintString = "";
		try {
			JSONObject xmlJSONObj = XML.toJSONObject(XmlStr);
			jsonPrettyPrintString = xmlJSONObj.toString(4);
		} catch (JSONException je) {
			log.info("Json Parsing Error");
			log.info(je.toString());
		}

		return jsonPrettyPrintString;
	}

}
