package com.bricopro.config.security;

import com.bricopro.common.sanitizer.HtmlSanitizer;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Configuration
public class RequestGuardConfig {

    private static final long MAX_REQUEST_SIZE = 10 * 1024 * 1024; 

    @Bean
    public FilterRegistrationBean<RequestSizeFilter> requestSizeFilter() {
        FilterRegistrationBean<RequestSizeFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RequestSizeFilter());
        bean.addUrlPatterns("/api/*");
        bean.setOrder(3);
        return bean;
    }

    @Component
    @Order(4)
    public static class SqlInjectionFilter implements Filter {

        private final HtmlSanitizer sanitizer = new HtmlSanitizer();

        private static final String[] CHECKED_PARAMS = {
            "search", "query", "q", "name", "email", "description", "title", "comment"
        };

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest  req = (HttpServletRequest)  request;
            HttpServletResponse res = (HttpServletResponse) response;

            for (String param : CHECKED_PARAMS) {
                String value = req.getParameter(param);
                if (value != null && sanitizer.containsSqlInjection(value)) {
                    res.setStatus(HttpStatus.BAD_REQUEST.value());
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"Invalid input detected in parameter: " + param + "\"}");
                    return;
                }
            }
            chain.doFilter(request, response);
        }
    }

    public static class RequestSizeFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest  req = (HttpServletRequest)  request;
            HttpServletResponse res = (HttpServletResponse) response;

            if (req.getContentLengthLong() > MAX_REQUEST_SIZE) {
                res.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
                res.setContentType("application/json");
                res.getWriter().write("{\"error\":\"Request body too large. Maximum size is 10MB.\"}");
                return;
            }
            chain.doFilter(request, response);
        }
    }
}
