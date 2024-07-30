package com.nr.lambda.app;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.newrelic.opentracing.LambdaTracer;
import com.newrelic.opentracing.aws.LambdaTracing;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseHandler<EV, RS> implements RequestHandler<EV, RS> {
    static {
        GlobalTracer.registerIfAbsent(LambdaTracer.INSTANCE);
    }

    public RS handleRequest(EV event, Context context) {
        return LambdaTracing.instrument(event, context, this::instrumentedHandler);
    }

    public RS instrumentedHandler(EV event, Context context) {
        try {
            Span span = GlobalTracer.get().activeSpan();
            span.setOperationName(context.getFunctionName());
            context.getLogger().log("Span ID : " + span.context().toSpanId());
            context.getLogger().log("Trace ID : " + span.context().toTraceId());
            context.getLogger().log("Function " + context.getFunctionName() + " called");
            printEventData(event, context);
            return doHandleRequest(event, context);
        } catch (Exception e) {
            context.getLogger().log("Exception thrown: " + e.getMessage());
            return catchError(500);
        }
    }

    public abstract RS doHandleRequest(EV event, Context context);

    public abstract RS catchError(Integer statusCode);

    public abstract void printEventData(EV event, Context context);

    public static void printMapEvent(Map<String, Object> event, Context context) {
        Object headers = event.get("headers");
        if (headers instanceof Map) {
            Map<?,?> headersMap = (Map<?,?>)headers;
            for (Object key: headersMap.keySet()) {
                Object value = headersMap.get(key);
                context.getLogger().log("Header with key [" + key + "] & value: " + value);
            }
        }
        for (Map.Entry<String, Object> entry: event.entrySet()) {
            context.getLogger().log("Request data [" + entry.getKey() + "] & value: " + entry.getValue());
        }
    }

    public static Map<String, Object> createMapResponse(Integer statusCode, String contentType, String body) {
        Map<String, Object> res = new HashMap<>();
        res.put("statusCode", statusCode);
        res.put("body", body);
        Map<String, Object> resHeaders = new HashMap<>();
        resHeaders.put("Content-Type", contentType);
        res.put("headers", resHeaders);
        return res;
    }

}
