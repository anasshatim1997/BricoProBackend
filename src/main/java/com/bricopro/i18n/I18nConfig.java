package com.bricopro.i18n;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Configuration
@Slf4j
public class I18nConfig {

    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasenames("messages/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setDefaultLocale(Locale.FRENCH);
        return source;
    }

    @Bean
    public AcceptHeaderLocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(
                Locale.FRENCH,
                new Locale("ar"),
                Locale.ENGLISH
        ));
        resolver.setDefaultLocale(Locale.FRENCH);
        return resolver;
    }

    @Component
    public static class LocaleFilter implements Filter {

        private static final ThreadLocal<Locale> CURRENT_LOCALE = new ThreadLocal<>();

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest req = (HttpServletRequest) request;
            String lang = req.getHeader("Accept-Language");

            Locale locale = Locale.FRENCH;
            if (lang != null) {
                if (lang.startsWith("ar")) locale = new Locale("ar");
                else if (lang.startsWith("en")) locale = Locale.ENGLISH;
            }

            CURRENT_LOCALE.set(locale);
            try {
                chain.doFilter(request, response);
            } finally {
                CURRENT_LOCALE.remove();
            }
        }

        public static Locale getCurrent() {
            Locale l = CURRENT_LOCALE.get();
            return l != null ? l : Locale.FRENCH;
        }
    }
}
