package com.tool.sqlpad.security;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class CookieJar {
    private CookieJar() {}
    public static void add(HttpServletResponse response, String name, String value) {
        add(response, name, value, true, null);
    }
    
    public static void add(HttpServletResponse response, String name, String value, boolean httpOnly, String domain) {
        Cookie cookie = new Cookie(name, value);

        cookie.setHttpOnly(httpOnly);
        cookie.setPath("/");
        if (domain != null) {
            cookie.setDomain(domain);
        }

        response.addCookie(cookie);
    }
    
    public static void clear(HttpServletResponse response, String name, String domain) {
        clear(response, name, true, domain);
    }

    public static void clear(HttpServletResponse response, String name, boolean httpOnly, String domain) {
        Cookie cookie = new Cookie(name, null);

        cookie.setHttpOnly(httpOnly);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        if (domain != null) {
            cookie.setDomain(domain);
        }

        response.addCookie(cookie);
    }

}
