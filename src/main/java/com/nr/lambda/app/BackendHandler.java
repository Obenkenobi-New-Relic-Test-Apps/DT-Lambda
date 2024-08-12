package com.nr.lambda.app;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.newrelic.opentracing.LambdaSpanContext;
import io.opentracing.SpanContext;
import io.opentracing.util.GlobalTracer;

import java.util.HashMap;
import java.util.Map;

public class BackendHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        try {
            SpanContext spanContext = GlobalTracer.get().activeSpan().context();
            String parentId = "N/A";
            if (spanContext instanceof LambdaSpanContext) {
                parentId = ((LambdaSpanContext)spanContext).getParentId();
            }
            String body = "{ " +
                    "\"myTraceId\": \""+ spanContext.toTraceId() + "\", " +
                    "\"key\": \"value\" " +
                    "\"spanId\": \"" + spanContext.toSpanId() + "\", " +
                    "\"parentId\": \"" + parentId + "\" " +
                    "}";
            return createMapResponse(200, "application/json", body);
        } catch (Exception e) {
            context.getLogger().log(e.getMessage());
            return createMapResponse(500, "application/html", "500 request");
        }
    }

    private static Map<String, Object> createMapResponse(Integer statusCode, String contentType, String body) {
        Map<String, Object> res = new HashMap<>();
        res.put("statusCode", statusCode);
        res.put("body", body);
        Map<String, Object> resHeaders = new HashMap<>();
        resHeaders.put("Content-Type", contentType);
        res.put("headers", resHeaders);
        return res;
    }
}
