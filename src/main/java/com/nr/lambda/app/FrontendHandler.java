package com.nr.lambda.app;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.propagation.TextMapAdapter;
import io.opentracing.util.GlobalTracer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class FrontendHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final String BACKEND_URL_V1 = System.getenv("BACKEND_URL_V1");
    private static final String BACKEND_URL_V2 = System.getenv("BACKEND_URL_V2");
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        try {
            Span span = GlobalTracer.get().activeSpan();
            SpanContext spanContext = span.context();
            String html = "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                    "<title>Front End Handler</title>" +
                    "<style>body {background: white; color: black} </style>" +
                    "</head>" +
                    "<body>" +
                    "<h1>Front End Handler</h1>" +
                    callBackend("V1 Backend Call Result", BACKEND_URL_V1, context) +
                    callBackend("V2 Backend Call Result", BACKEND_URL_V2, context) +
                    "<h2>Trace Information</h2>" +
                    "<p><b>Trace Id: " + spanContext.toTraceId() + "</b></p>" +
                    "<p><b>Span Id: " + spanContext.toSpanId() + "</b></p>" +
                    "</body>" +
                    "</html>";
            return createMapResponse(200, "text/html", html);
        } catch (Exception e) {
            context.getLogger().log(e.getMessage());
            return createMapResponse(500, "application/html", "500 request");
        }
    }

    public String callBackend(String title, String url, Context context) {
        StringBuilder html = new StringBuilder();
        html.append("<h2>").append(title).append("</h2>");
        try {
            // Create a GET request connection
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");

            // Get Span Context and tracer
            SpanContext spanContext = GlobalTracer.get().activeSpan().context();

            // Pass distributed tracing headers from context into the http client
            TextMap dtHeaders = new TextMapAdapter(new HashMap<>());
            GlobalTracer.get().inject(spanContext, Format.Builtin.HTTP_HEADERS, dtHeaders);
            for (Map.Entry<String, String> dtHeader: dtHeaders) {
                String headerName = dtHeader.getKey();
                String headerValue = dtHeader.getValue();
                con.setRequestProperty(headerName, headerValue);
            }

            // Render http response
            html.append("<h3>Response Code</h3><p><b>").append(con.getResponseCode()).append("</b></p>");
            html.append("<h3>Response Body</h3>");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            for (String inputLine = in.readLine(); inputLine != null; inputLine = in.readLine()) {
                html.append("<p><b>").append(inputLine).append("</b></p>");
            }
            html.append("</br>");

            // Generate html snippet
            String htmlSnippetStr = html.toString();
            context.getLogger().log("Returned html snippet: " + htmlSnippetStr);
            return htmlSnippetStr;
        } catch (IOException e) {
            throw new RuntimeException(e);
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
