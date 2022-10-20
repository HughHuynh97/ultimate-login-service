package com.tool.sqlpad.security;

import com.tool.sqlpad.constant.CookieProperty;
import com.tool.sqlpad.enums.Role;
import com.tool.sqlpad.enums.StepLogin;
import com.tool.sqlpad.security.partner.User;

import javax.enterprise.inject.Alternative;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static com.tool.sqlpad.security.HttpDomain.getDomain;


public abstract class CookieCodec {

    private static final Role[] ROLES = Role.values();
    private static final int SIZE = 4 /*role.orginal*/ + 8 /*id*/ + 8 /*ts*/;

    private final long ttl;
    private final byte[] salt;
    private final int size;

    CookieCodec(byte[] salt, long ttl) {
        this.salt = salt;
        this.ttl = ttl;
        this.size = SIZE + salt.length;
    }

    public byte[] encode(Role role, long id) {
        MessageDigest digest = md();
        digest.update(salt);
        byte[] buffer = new byte[size];
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.putInt(role.ordinal());
        bb.putLong(id);
        bb.putLong(ttl + System.currentTimeMillis());
        digest.update(buffer, 0, 20);
        bb.put(digest.digest());
        return buffer;
    }

    public User decode(byte[] message) {
        if (message.length != size) {
            return InvalidUser.INSTANCE;
        }

        MessageDigest digest = md();
        digest.update(salt);
        digest.update(message, 0, 20);

        byte[] hash = digest.digest();

        for (int i = 0; i < salt.length; i++) {
            if (hash[i] != message[i + 20]) {
                return InvalidUser.INSTANCE;
            }
        }

        ByteBuffer bb = ByteBuffer.wrap(message);

        int ordinal = bb.getInt();
        long id = bb.getLong();
        long timestamp = bb.getLong();

        if (timestamp < System.currentTimeMillis() || ordinal >= ROLES.length) {
            // expired
            return InvalidUser.INSTANCE;
        }

        return new ValidUser(ROLES[ordinal], id, timestamp);
    }

    public User getValidUser(Role role, long agentId) {
        return new ValidUser(role, agentId, System.currentTimeMillis() + 60000);
    }

    protected static MessageDigest md() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("not possbile, jdk requires sha-256");
        }
    }

    @Alternative
    private static class InvalidUser implements User {

        final static InvalidUser INSTANCE = new InvalidUser();

        @Override
        public long getId() {
            throw new RuntimeException();
        }

        @Override
        public Role getRole() {
            throw new RuntimeException();
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public long getExpiry() {
            return System.currentTimeMillis() - 1;
        }

        @Override
        public void keepAlive(HttpServletResponse response, HttpServletRequest request) {
            throw new RuntimeException("Invalid User");
        }

        @Override
        public boolean hasExpiried() {
            return true;
        }

        @Override
        public void logout(HttpServletResponse response, HttpServletRequest request) {
        }

        @Override
        public StepLogin getStep() {
            return StepLogin.login;
        }
    }

    @Alternative
    private static class ValidUser implements User {

        final Role role;
        final long id;
        final long expiry;

        public ValidUser(Role role, long id, long expiry) {
            this.role = role;
            this.id = id;
            this.expiry = expiry;
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public Role getRole() {
            return role;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public long getExpiry() {
            return expiry;
        }

        @Override
        public void keepAlive(HttpServletResponse response, HttpServletRequest request) {
            if (isValid() && (expiry == 0 || (expiry - System.currentTimeMillis()) <= 60000)) {
                CookieJar.add(response, CookieProperty.SESSION, Base64.getUrlEncoder().encodeToString((new UserCookieCodec()).encode(role, id)), true, getDomain(request));
            }
        }

        @Override
        public boolean hasExpiried() {
            return expiry < System.currentTimeMillis();
        }

        @Override
        public void logout(HttpServletResponse response, HttpServletRequest request) {
            CookieJar.add(response, CookieProperty.SESSION, null);
        }

        @Override
        public StepLogin getStep() {
            return StepLogin.login;
        }
    }

}
