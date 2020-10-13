package se.martin.eventsource.trace;

import brave.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@WebFilter("/*")
public class TraceFilter implements Filter {

    private static final String TRACE_ID_HEADER = "x-b3-traceid";

    @Autowired
    Tracer tracer;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        var httpResponse = (HttpServletResponse) response;
        var traceId = tracer.currentSpan().context().traceIdString();
        log.debug("Setting " + TRACE_ID_HEADER + " to " + traceId);
        httpResponse.setHeader(TRACE_ID_HEADER, traceId);

        chain.doFilter(request, response);

    }
}
