package com.tool.sqlpad.controller;

import com.tool.sqlpad.constant.CookieProperty;
import com.tool.sqlpad.enums.Role;
import com.tool.sqlpad.model.UserInfo;
import com.tool.sqlpad.security.HttpDomain;
import com.tool.sqlpad.security.UserCookieCodec;
import com.tool.sqlpad.security.UserCookieCodecAgent;
import com.tool.sqlpad.service.MapperService;
import com.tool.sqlpad.service.SqlService;
import lombok.Data;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("ultimate-login")
public class LoginController {
    private final SqlService sqlService;
    private final MapperService mapperService;
    private static final long TTL = TimeUnit.MINUTES.toMillis(10);
    private static final UserCookieCodecAgent agentCookieCodec = new UserCookieCodecAgent(TTL);
    private static final UserCookieCodec playerCookieCodec = new UserCookieCodec(TTL);

    public LoginController(SqlService sqlService, MapperService mapperService) {
        this.sqlService = sqlService;
        this.mapperService = mapperService;
    }

    @GetMapping("get-cookie")
    public String getCookie(String loginId,
                            @RequestParam(defaultValue = "") String type,
                            @RequestParam(required = false, defaultValue = "localhost") String host) {
        var domain = new HttpDomain(host);
        var user = getUserInfo(loginId, type);
        var result = new StringBuilder();
        result.append("""
                document.cookie = "%s=%s; domain=%s; path=/";
                """.formatted(CookieProperty.SINGLE_LOGIN, getOrCreateSid(user), domain.getDomain()));
        if (Role.PARTNER == user.getRole() || Role.SUB_PARTNER == user.getRole()) {
            result.append("""
                    document.cookie = "%s=%s; domain=%s; path=/";
                    """.formatted(CookieProperty.SESSION, Base64.getUrlEncoder().encodeToString(agentCookieCodec.encode(Role.valueOf(user.getRole().name()), user.getUserId())), domain.getDomain()));
        } else {
            result.append("""
                    document.cookie = "%s=%s; domain=%s; path=/";
                    """.formatted(CookieProperty.SESSION, Base64.getUrlEncoder().encodeToString(playerCookieCodec.encode(user.getRole(), user.getUserId())), domain.getDomain()));
            var encodeUserId = DigestUtils.md5Hex(String.valueOf(user.getUserId())).toUpperCase();
            result.append("""
                    document.cookie = "%s=%s; domain=%s; path=/";
                    """.formatted("code", encodeUserId, domain.getDomain()));
        }
        return result.toString();
    }

    private String getOrCreateSid(UserInfo user) {
        String sid = switch (user.getRole()) {
            case ADMIN -> sqlService.query("SELECT slid FROM admin WHERE user_id = %s".formatted(user.getUserId()));
            case SUB_PARTNER ->
                    sqlService.query("SELECT slid FROM sub_user WHERE user_id = %s".formatted(user.getUserId()));
            default ->
                    sqlService.query("SELECT slid FROM user_info_view WHERE user_id = %s".formatted(user.getUserId()));
        };
        return mapperService.readValueAsArray(sid, SID.class).get(0).getSlid();
    }

    private UserInfo getUserInfo(String loginId, String type) {
        var backoffice = """
                SELECT user_id, user_code, login_id, 'ADMIN' AS role, 'BO' AS level_name, status, '' AS currency_code,
                'BO' AS user_type, 0 AS balance, 'bo' AS brand, 'bo' AS brand_type, 'bo' AS domain_name, pin, first_name, last_name
                FROM admin
                WHERE status <> 'CLOSED' AND (login_id = '%s' OR user_code = '%s')
                """;
        var agent = """
                SELECT u.user_id, u.user_code, u.login_id, 'PARTNER' AS role, u.level_name, u.status, u.currency_code, u.user_type,
                0 AS balance, b.brand_name AS brand, b.brand_type, b.domain_name, '' AS pin, '' AS first_name, '' AS last_name
                FROM user_account u
                    INNER JOIN brand b ON b.brand_id = u.brand_id
                WHERE u.status NOT IN ('INACTIVE','CLOSED') AND u.level_name <> 'PL' AND (u.login_id = '%s' OR u.user_code = '%s')
                """;
        var subAgent = """
                SELECT s.user_id, s.user_code, s.login_id, 'SUB_PARTNER' AS role, u.level_name, s.status, u.currency_code, u.user_type, 0 AS balance,
                b.brand_name AS brand, b.brand_type, b.domain_name, s.pin, s.first_name, s.last_name
                FROM sub_user s
                    INNER JOIN user_account u ON u.user_id = s.parent_id
                    INNER JOIN brand b ON b.brand_id = u.brand_id
                    WHERE u.status NOT IN ('INACTIVE','CLOSED') AND u.level_name <> 'PL' AND (s.login_id = '%s' OR s.user_code = '%s')
                """;
        var player = """
                SELECT u.user_id, u.user_code, u.login_id,
                IF(b.domain_type = 'WHITELABEL', 'WL_PLAYER' ,IF(u.user_type ='CASH', 'CASH_PLAYER', IF(u.user_type = 'CREDIT_CASH', 'CREDIT_CASH_PLAYER', 'PLAYER'))) AS role,
                u.level_name, u.status, u.currency_code, u.user_type, 0 AS balance,
                b.brand_name AS brand, b.brand_type, b.domain_name, '' AS pin, '' AS first_name, '' AS last_name
                FROM user_account u
                    INNER JOIN brand b ON b.brand_id = u.brand_id
                    WHERE u.status NOT IN ('INACTIVE','CLOSED') AND u.level_name = 'PL' AND (u.login_id = '%s' OR u.user_code = '%s')
                """;
        var rs = "";
        switch (type) {
            case "admin" -> {
                backoffice = (backoffice + " LIMIT 1").formatted(loginId, loginId);
                rs = sqlService.query(backoffice);
            }
            case "agent" -> {
                agent = (agent + " LIMIT 1").formatted(loginId, loginId);
                rs = sqlService.query(agent);
            }
            case "sub" -> {
                subAgent = (subAgent + " LIMIT 1").formatted(loginId, loginId);
                rs = sqlService.query(subAgent);
            }
            case "player" -> {
                player = (player + " LIMIT 1").formatted(loginId, loginId);
                rs = sqlService.query(player);
            }
            default -> {
                var total = (agent + " UNION ALL " + subAgent + " UNION ALL " + backoffice + " UNION ALL " + player + " LIMIT 1").formatted(loginId, loginId, loginId, loginId, loginId, loginId, loginId, loginId);
                rs = sqlService.query(total);
            }
        }
        return mapperService.readValueAsArray(rs, UserInfo.class).get(0);
    }

    @Data
    public static class SID {
        private String slid;
    }

}
