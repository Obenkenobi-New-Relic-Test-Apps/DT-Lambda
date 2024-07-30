package com.nr.lambda.app;

import com.amazonaws.services.lambda.runtime.Context;
import com.newrelic.opentracing.LambdaPayloadContext;
import com.newrelic.opentracing.LambdaSpanContext;
import io.opentracing.SpanContext;
import io.opentracing.util.GlobalTracer;

import java.util.Map;

public class BackendHandler extends BaseHandler<Map<String, Object>, Map<String, Object>> {
    @Override
    public Map<String, Object> doHandleRequest(Map<String, Object> event, Context context) {
        SpanContext spanContext = GlobalTracer.get().activeSpan().context();
        String parentId = "N/A";
        if (spanContext instanceof LambdaSpanContext) {
            parentId = ((LambdaSpanContext)spanContext).getParentId();
        }
        String body = "{\n" +
                "\"myTraceId\": \""+ spanContext.toTraceId() + "\",\n" +
                "\"key\": \"value\" \n" +
                "\"spanId\": \"" + spanContext.toSpanId() + "\",\n" +
                "\"parentId\": \"" + parentId + "\"\n" +
                "}";
        return createMapResponse(200, "application/json", body);
    }

    @Override
    public Map<String, Object> catchError(Integer statusCode) {
        return createMapResponse(statusCode, "text/plain", "500 request");
    }

    @Override
    public void printEventData(Map<String, Object> event, Context context) {
        printMapEvent(event, context);
    }
}
