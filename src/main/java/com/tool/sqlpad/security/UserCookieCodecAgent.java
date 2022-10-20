package com.tool.sqlpad.security;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class UserCookieCodecAgent extends CookieCodec {
    private static final long TTL = TimeUnit.MINUTES.toMillis(10);
    private static final byte[] SALT = loadSalt();

    public UserCookieCodecAgent(long ttl) {
        super(SALT, ttl);
    }

    public UserCookieCodecAgent() {
        super(SALT, TTL);
    }

    public static byte[] loadSalt() {
        return Base64.getDecoder().decode("+oamh+FhmElPmrqGnMbhP0G0jIHNSQNeMNq5nnp/hDE=");
    }
}
