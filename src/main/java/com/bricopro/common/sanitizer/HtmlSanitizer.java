package com.bricopro.common.sanitizer;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class HtmlSanitizer {

    private static final Pattern SCRIPT_TAG     = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HTML_TAGS      = Pattern.compile("<[^>]+>");
    private static final Pattern JS_EVENTS      = Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern JAVASCRIPT_URI = Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_INJECTION  = Pattern.compile("(--|;|/\\*|\\*/|xp_|UNION|SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE)", Pattern.CASE_INSENSITIVE);

    public String sanitize(String input) {
        if (input == null) return null;
        String clean = input;
        clean = SCRIPT_TAG.matcher(clean).replaceAll("");
        clean = JS_EVENTS.matcher(clean).replaceAll("");
        clean = JAVASCRIPT_URI.matcher(clean).replaceAll("");
        clean = HTML_TAGS.matcher(clean).replaceAll("");
        clean = clean.replace("&", "&amp;")
                     .replace("\"", "&quot;")
                     .replace("'", "&#x27;")
                     .replace("/", "&#x2F;");
        return clean.trim();
    }

    public String sanitizeLoose(String input) {
        if (input == null) return null;
        return SCRIPT_TAG.matcher(input).replaceAll("")
               .replace("<script", "")
               .replace("</script", "")
               .trim();
    }

    public boolean containsSqlInjection(String input) {
        if (input == null) return false;
        return SQL_INJECTION.matcher(input).find();
    }
}
