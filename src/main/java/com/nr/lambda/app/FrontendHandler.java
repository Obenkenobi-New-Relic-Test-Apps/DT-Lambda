package com.nr.lambda.app;

import com.amazonaws.services.lambda.runtime.Context;
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

public class FrontendHandler extends BaseHandler<Map<String, Object>, Map<String, Object>> {
    private static final String BACKEND_URL_V1 = System.getenv("BACKEND_URL_V1");
    private static final String BACKEND_URL_V2 = System.getenv("BACKEND_URL_V2");
    @Override
    public Map<String, Object> doHandleRequest(Map<String, Object> event, Context context) {
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
    }

    public String callBackend(String title, String url, Context context) {
        StringBuilder htmlReturnSnippet = new StringBuilder();
        htmlReturnSnippet.append("<h2>").append(title).append("</h2>");
        try {
            // Create a GET request connection
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");

            // Get Span Context and tracer
            SpanContext spanContext = GlobalTracer.get().activeSpan().context();

            // Pass headers from context
            TextMap headers = new TextMapAdapter(new HashMap<>());
            GlobalTracer.get().inject(spanContext, Format.Builtin.HTTP_HEADERS, headers);
            headers.forEach(header -> con.setRequestProperty(header.getKey(), header.getValue()));

            // Render http response
            htmlReturnSnippet.append("<h3>Response Code</h3><p><b>").append(con.getResponseCode()).append("</b></p>");
            htmlReturnSnippet.append("<h3>Response Body</h3>");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            for (String inputLine = in.readLine(); inputLine != null; inputLine = in.readLine()) {
                htmlReturnSnippet.append("<p><b>").append(inputLine).append("</b></p><br/>");
            }

            // Generate html snippet
            String htmlSnippetString = htmlReturnSnippet.toString();
            context.getLogger().log("Returned html snippet: " + htmlSnippetString);
            return htmlSnippetString;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> catchError(Integer statusCode) {
        return createMapResponse(statusCode, "application/html", "500 request");
    }

    @Override
    public void printEventData(Map<String, Object> event, Context context) {
        printMapEvent(event, context);
    }

}
