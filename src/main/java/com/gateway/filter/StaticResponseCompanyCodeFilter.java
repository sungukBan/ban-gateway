package com.gateway.filter;

import com.gateway.http.OpenAPIHttpStatus;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.gateway.redis.ListenerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;


@Component
public class StaticResponseCompanyCodeFilter
  extends CommonFilter
{
  private static Logger log = LoggerFactory.getLogger(StaticResponseCompanyCodeFilter.class);
  
  public static final String PREFIX_STATIC = String.format("%s%s", PREFIX_REDIS_KEY,  "static" );
  
  @Autowired
  private ListenerService listenerService;



  public Object runZuulFilter() throws ZuulException {
    RequestContext ctx = RequestContext.getCurrentContext();
    JsonObject staticJson = responseBody();
    log.info("staticJson : " + staticJson);

    String key = createKey(PREFIX_STATIC, getUriCompanyKey() );
    log.info("key : " + key);
    String uri = getUri();

    JsonParser jsonParser = new JsonParser();
    JsonObject object = (JsonObject)jsonParser.parse(this.listenerService.get(key));

    log.debug("object: " + object);

    log.debug("Static Response Redis Key: " + key);

    if (staticJson == null) {
      log.info(String.format("Static response not found - URI:%s, Key:%s",  uri, key ));

      return setResponse(ctx, OpenAPIHttpStatus.SERVICE_NOT_FOUND);
    }

    log.debug("-----------information-----------");
    Calendar calendar = Calendar.getInstance();

    Date date = calendar.getTime();
    String timeNow = (new SimpleDateFormat("HHmm")).format(date);

    int todayOfWeek = calendar.get(7);
    log.debug("toDay(week): " + todayOfWeek);
    log.debug("---------------input information-----------------");

    String[] weeks = null;

    if (object.get("week") != null) {
      String week = (String)Optional.ofNullable(object.get("week").toString().replace("\"", "")).orElse("");
      log.info("week: " + week);

      log.debug("----------------setting week value----------------");
      weeks = week.split(",");

      for (int i = 0; i < weeks.length; i++) {
        log.info(weeks[i]);
      }
    }

    String startTime = null;
    String endTime = null;
    if (object.get("time") != null) {
      log.debug("----------------setting time value----------------");
      String time = (String)Optional.ofNullable(object.get("time").toString().replace("\"", "")).orElse("");
      String[] times = time.split("~");
      startTime = (String)Optional.ofNullable(times[0]).orElse("");
      endTime = (String)Optional.ofNullable(times[1]).orElse("");
      log.debug("startTime: " + startTime + ", endTime: " + endTime);
    }

    log.info("--------------------------------");

    if (object.get("week") == null && object.get("time") != null) {
      log.debug("----------time----------");
      if (Integer.parseInt(timeNow) >= Integer.parseInt(startTime) && Integer.parseInt(timeNow) <= Integer.parseInt(endTime)) {
        log.debug(String.format("URI:%s, Key:%s, Data:%s", uri, key, staticJson.toString() ));
        return setResponseWithBody(ctx, OpenAPIHttpStatus.NO_ERROR,  staticJson);
      }
    }

    if (object.get("time") == null && object.get("week") != null) {
      log.info("----------week----------");

      for (int i = 0; i < weeks.length; i++) {
        log.debug("todayOfWeek: " + todayOfWeek);
        log.debug("weeks: " + Integer.parseInt(weeks[i]));
        if (todayOfWeek == Integer.parseInt(weeks[i])) {
          log.debug(String.format("URI:%s, Key:%s, Data:%s", uri, key, staticJson.toString() ));
          return setResponseWithBody(ctx, OpenAPIHttpStatus.NO_ERROR,  staticJson);
        }
      }
    }

    if (object.get("time") != null && object.get("week") != null) {
      log.debug("----------setting time & week restriction value----------");
      log.debug("toDay: " + todayOfWeek);
      for (int i = 0; i < weeks.length; i++) {

        if (todayOfWeek == Integer.parseInt(weeks[i]))
        {
          if (Integer.parseInt(timeNow) >= Integer.parseInt(startTime) && Integer.parseInt(timeNow) <= Integer.parseInt(endTime)) {
            log.info(String.format("URI:%s, Key:%s, Data:%s", uri, key, staticJson.toString() ));
            return setResponseWithBody(ctx, OpenAPIHttpStatus.NO_ERROR,  staticJson);
          }
        }
      }
    }

    if (object.get("time") == null && object.get("week") == null) {
      log.debug("----------default static response----------");
      log.debug(String.format("URI:%s, Key:%s, Data:%s", uri, key, staticJson.toString() ));
      return setResponseWithBody(ctx, OpenAPIHttpStatus.NO_ERROR,  staticJson);
    }

    return null;
  }

  @Override
  public String filterType() {
    return FILTER_TYPE_ROUTE;
  }


  public int filterOrder() { return FILTER_OTHER_PRE_STATIC_COMPANYCODE; }


  public JsonObject responseBody() {
    String key = createKey(PREFIX_STATIC,  getUriCompanyKey());

    JsonParser jsonParser = new JsonParser();
    JsonObject object = (JsonObject)jsonParser.parse(this.listenerService.get(key));
    String value = object.get("response").toString().replace("\"", "");

    JsonObject staticJson = null;
    if (value != null) {
      staticJson = new JsonObject();
      staticJson.addProperty("RSP_CD", Integer.toString(OpenAPIHttpStatus.NO_ERROR.value()));
      staticJson.addProperty("data", value);
    }
    return staticJson;
  }


  @Override
  public boolean shouldFilter() {
    boolean should = false;

//    RequestContext ctx = RequestContext.getCurrentContext();
//    String key = createKey(PREFIX_STATIC, getUriCompanyKey());
//
//    boolean contains = this.listenerService.containsKey(key);
//
//    log.info("static response company code redisKey : "+ key + " Y/N: " + contains);
//    if (ctx.getRequest().getContentType() != null && contains) {
//      should = true;
//    }


    return should;
  }


//  public String getStaticResponseKey() {
//    String uri = getUri();
//    String[] splits = uri.split("/");
//
//    return splits[2].substring(0, 3);
//  }


  @Override
  public String getFilterName() { return "Static Response CompanyCode Filter"; }
}
