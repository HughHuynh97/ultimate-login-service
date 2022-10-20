package com.tool.sqlpad.security;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;

public class HttpDomain {
    public static final String TLD = "com,org,net,int,edu,gov,mil,";
    private final String domain;
    private final String serverName;
    private boolean ip;

    public HttpDomain(HttpServletRequest req) {
        this(req.getServerName());
    }

    public HttpDomain(String serverName) {
        this.serverName = serverName.toLowerCase();
        String[] parts = serverName.split("\\.");
        if (parts.length >= 2) {
            if (!InetAddressValidator.getInstance().isValid(serverName)) {
                if (!TLD.contains(parts[1] + ",")) {
                    parts[0] = "";
                    domain = StringUtils.join(parts, ".");
                } else {
                    domain = "." + serverName;
                }
                return;
            } else {
                ip = true;
            }
        }
        domain = serverName;
    }

    public boolean isIP() {
        return ip;
    }

    public String getOriginalServerName() {
        return serverName;
    }

    public String getDomain() {
        return domain;
    }

    public boolean isValid() {
        return !serverName.equals(domain);
    }

    public static String getDomain(HttpServletRequest request) {
        HttpDomain httpDomain = new HttpDomain(request);
        if(httpDomain.isValid()){
            return httpDomain.getDomain();
        }
        return null;
    }
}
