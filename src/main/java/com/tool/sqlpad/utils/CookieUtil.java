package com.tool.sqlpad.utils;

import com.tool.sqlpad.security.CookieBuilder;
import com.tool.sqlpad.security.HttpDomain;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CookieUtil {
    private static final Logger LOGGER = Logger.getLogger(CookieUtil.class.getName());
    public static final String MEMBER_CAPTCHA_COOKIES = "_capt";

    public static String getCookie(String key, HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Request cookies: {0}", getCookiesStr(cookies));
        }
        if (cookies == null) {
            LOGGER.log(Level.FINE, "CookieUtil cookies is null for key {0}", String.valueOf(key));
            return "";
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(key)) {
                return cookie.getValue();
            }
        }
        LOGGER.log(Level.FINE, "CookieUtil cookies is empty for key {0}", String.valueOf(key));
        return "";
    }

    public static String getCookiesStr(Cookie[] cookies) {
        StringBuilder sb = new StringBuilder();
        if (cookies != null) {
            String separator = "";
            for (Cookie ck : cookies) {
                sb.append(separator).append(ck.getName()).append("=").append(ck.getValue());
                separator = ",";
            }
        }
        return sb.toString();
    }

    public static void addCookie(String key, String value, HttpServletRequest request, HttpServletResponse response) {
        response.addCookie(new CookieBuilder(key, value).domain(new HttpDomain(request)).httpOnly().get());
    }
    public static void addCookie(String key, String value, HttpServletRequest request, HttpServletResponse response, boolean isHttpOnly) {
        CookieBuilder builder = new CookieBuilder(key, value).domain(new HttpDomain(request));
        if (isHttpOnly) {
            builder.httpOnly();
        }
        response.addCookie(builder.get());
    }
}
