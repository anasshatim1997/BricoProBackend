package com.bricopro.common.sanitizer;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Component
@Order(1)
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(new XssRequestWrapper((HttpServletRequest) request), response);
    }

    @Schema(description = "Xss Request Wrapper")
    public static class XssRequestWrapper extends HttpServletRequestWrapper {

        private final HtmlSanitizer sanitizer = new HtmlSanitizer();

        public XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            return sanitizer.sanitize(super.getParameter(name));
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            return Arrays.stream(values).map(sanitizer::sanitize).toArray(String[]::new);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> original = super.getParameterMap();
            Map<String, String[]> sanitized = new LinkedHashMap<>();
            original.forEach((k, v) -> sanitized.put(k,
                    Arrays.stream(v).map(sanitizer::sanitize).toArray(String[]::new)));
            return Collections.unmodifiableMap(sanitized);
        }

        @Override
        public String getHeader(String name) {
            return sanitizer.sanitize(super.getHeader(name));
        }
    }
}
