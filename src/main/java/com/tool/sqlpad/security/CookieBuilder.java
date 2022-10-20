package com.tool.sqlpad.security;

import javax.servlet.http.Cookie;

public class CookieBuilder {
    private final Cookie cookie;

    public CookieBuilder(String name, String value) {
        cookie = new Cookie(name, value);
        cookie.setPath("/");
    }

    public CookieBuilder domain(HttpDomain domain) {
        cookie.setDomain(domain.getDomain());
        return this;
    }

    public CookieBuilder httpOnly() {
        cookie.setHttpOnly(true);
        return this;
    }

    public CookieBuilder maxAge(int maxAge) {
        cookie.setMaxAge(maxAge);
        return this;
    }

    public Cookie get() {
        return cookie;
    }

    public CookieBuilder secure() {
        cookie.setSecure(true);
        return this;
    }
}
