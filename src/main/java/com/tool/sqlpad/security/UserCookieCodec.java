package com.tool.sqlpad.security;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class UserCookieCodec extends CookieCodec {

    private static final long TTL = TimeUnit.MINUTES.toMillis(10);
    private static final byte[] SALT = loadSalt();

    public UserCookieCodec(long ttl) {
        super(SALT, ttl);
    }

    public UserCookieCodec() {
        super(SALT, TTL);
    }

    public static byte[] loadSalt() {
        return Base64.getDecoder().decode("vzzo3ky8vbRUKNfemvvIJeeLWDTVznPXIC1d0FQ4bPs=");
    }
}
