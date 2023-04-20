package com.tool.sqlpad.controller;

import com.tool.sqlpad.constant.CookieProperty;
import com.tool.sqlpad.enums.Role;
import com.tool.sqlpad.model.SID;
import com.tool.sqlpad.model.UserInfo;
import com.tool.sqlpad.security.HttpDomain;
import com.tool.sqlpad.security.UserCookieCodec;
import com.tool.sqlpad.security.UserCookieCodecAgent;
import com.tool.sqlpad.service.MapperService;
import com.tool.sqlpad.service.SqlService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.text.MessageFormat;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static com.tool.sqlpad.enums.Role.ADMIN;
import static com.tool.sqlpad.enums.Role.SUB_PARTNER;

@RestController
@RequestMapping("merito")
@RequiredArgsConstructor
public class LoginController {
    private final SqlService sqlService;
    private final MapperService mapperService;
    private static final String COOKIE_PATH = """
            document.cookie = "%s=%s; domain=%s; path=/";
            """;
    private static final String LIMIT = " LIMIT 1 ";
    private static final String UNION_ALL = "  UNION ALL ";
    private static final long TTL = TimeUnit.MINUTES.toMillis(10);
    private static final UserCookieCodecAgent agentCookieCodec = new UserCookieCodecAgent(TTL);
    private static final UserCookieCodec playerCookieCodec = new UserCookieCodec(TTL);

    @SneakyThrows
    @GetMapping("get-cookie")
    public String getCookie(String loginId,
                            @RequestParam(defaultValue = "") String type,
                            @RequestParam(required = false, defaultValue = "localhost") String host) {
        var domain = new HttpDomain(host);
        var user = getUserInfo(loginId, type);
        var result = new StringBuilder();
        result.append(COOKIE_PATH.formatted(CookieProperty.SINGLE_LOGIN, getOrCreateSid(user), domain.getDomain()));
        if (Role.PARTNER == user.getRole() || SUB_PARTNER == user.getRole()) {
            result.append(COOKIE_PATH.formatted(CookieProperty.SESSION, Base64.getUrlEncoder().encodeToString(agentCookieCodec.encode(Role.valueOf(user.getRole().name()), user.getUserId())), domain.getDomain()));
        } else {
            result.append(COOKIE_PATH.formatted(CookieProperty.SESSION, Base64.getUrlEncoder().encodeToString(playerCookieCodec.encode(user.getRole(), user.getUserId())), domain.getDomain()));
            var encodeUserId = DigestUtils.md5Hex(String.valueOf(user.getUserId())).toUpperCase();
            result.append(COOKIE_PATH.formatted("code", encodeUserId, domain.getDomain()));
        }
        return result.toString();
    }

    @SneakyThrows
    @GetMapping("get-token")
    public Object getToken(String loginId, @RequestParam(defaultValue = "") String type) {
        var user = getUserInfo(loginId, type);
        var slid = getOrCreateSid(user);
        var meSessionId = Base64.getUrlEncoder().encodeToString(playerCookieCodec.encode(user.getRole(), user.getUserId()));
        var message = MessageFormat.format("{0}:{1}", slid, meSessionId);
        var token = Base64.getEncoder().encodeToString(message.getBytes());
        return "localStorage.setItem('currentToken', 'Bearer %s')".formatted(token);
    }

    private String getOrCreateSid(UserInfo user) {
        var sid = "";
        if (user.getRole().equals(ADMIN)) {
            sid = sqlService.query("SELECT slid FROM admin WHERE user_id = %s".formatted(user.getUserId()));
        } else if (user.getRole().equals(SUB_PARTNER)) {
            sid = sqlService.query("SELECT slid FROM sub_user WHERE user_id = %s".formatted(user.getUserId()));
        } else {
            sid = sqlService.query("SELECT slid FROM user_info_view WHERE user_id = %s".formatted(user.getUserId()));
        }
        return mapperService.readValueAsArray(sid, SID.class).get(0).getSlid();
    }

    private UserInfo getUserInfo(String loginId, String type) {
        var backoffice = """
                SELECT user_id, user_code, login_id, 'ADMIN' AS role, 'BO' AS level_name, status, '' AS currency_code,
                'BO' AS user_type, 0 AS balance, 'bo' AS brand, 'bo' AS brand_type, 'bo.beatus99.com' AS domain_name, pin, first_name, last_name
                FROM admin
                WHERE status <> 'CLOSED' AND (login_id = '%s' OR user_code = '%s')
                """;
        var agent = """
                SELECT u.user_id, u.user_code, u.login_id, 'PARTNER' AS role, u.level_name, u.status, u.currency_code, u.user_type,
                0 AS balance, b.brand_name AS brand, b.brand_type, IFNULL(c.domain_name, b.domain_name) AS domain_name, '' AS pin, '' AS first_name, '' AS last_name
                FROM user_account u
                    INNER JOIN brand b ON b.brand_id = u.brand_id
                    LEFT JOIN brand_domain_config c ON c.brand_id = b.brand_id
                WHERE u.status NOT IN ('INACTIVE','CLOSED')
                    AND u.level_name <> 'PL' AND (u.login_id = '%s' OR u.user_code = '%s')
                """;
        var subAgent = """
                SELECT s.user_id, s.user_code, s.login_id, 'SUB_PARTNER' AS role, u.level_name, s.status, u.currency_code, u.user_type, 0 AS balance,
                b.brand_name AS brand, b.brand_type, IFNULL(c.domain_name, b.domain_name) AS domain_name, s.pin, s.first_name, s.last_name
                FROM sub_user s
                    INNER JOIN user_account u ON u.user_id = s.parent_id
                    INNER JOIN brand b ON b.brand_id = u.brand_id
                    LEFT JOIN brand_domain_config c ON c.brand_id = b.brand_id
                WHERE u.status NOT IN ('INACTIVE','CLOSED')
                    AND u.level_name <> 'PL'
                    AND (s.login_id = '%s' OR s.user_code = '%s')
                """;
        var player = """
                SELECT u.user_id, u.user_code, u.login_id,
                IF(b.domain_type = 'WHITELABEL', 'WL_PLAYER', IF(u.user_type ='CASH', 'CASH_PLAYER',IF(u.user_type = 'CREDIT_CASH', 'CREDIT_CASH_PLAYER', 'PLAYER'))) AS role,
                u.level_name, u.status, u.currency_code, u.user_type, 0 AS balance,
                b.brand_name AS brand, b.brand_type, IFNULL(c.domain_name, b.domain_name) AS domain_name, '' AS pin, '' AS first_name, '' AS last_name
                FROM user_account u
                    INNER JOIN brand b ON b.brand_id = u.brand_id
                    LEFT JOIN brand_domain_config c ON c.brand_id = b.brand_id
                WHERE u.status NOT IN ('INACTIVE','CLOSED')
                    AND u.level_name = 'PL'
                    AND (u.login_id = '%s' OR u.user_code = '%s')
                """;
        var rs = "";
        switch (type) {
            case "admin" -> {
                backoffice = (backoffice + LIMIT).formatted(loginId, loginId);
                rs = sqlService.query(backoffice);
            }
            case "agent" -> {
                agent = (agent + LIMIT).formatted(loginId, loginId);
                rs = sqlService.query(agent);
            }
            case "sub" -> {
                subAgent = (subAgent + LIMIT).formatted(loginId, loginId);
                rs = sqlService.query(subAgent);
            }
            case "player" -> {
                player = (player + LIMIT).formatted(loginId, loginId);
                rs = sqlService.query(player);
            }
            default -> {
                var total = (agent + UNION_ALL + subAgent + UNION_ALL + backoffice + UNION_ALL + player + LIMIT).formatted(loginId, loginId, loginId, loginId, loginId, loginId, loginId, loginId);
                rs = sqlService.query(total);
            }
        }
        return mapperService.readValueAsArray(rs, UserInfo.class).get(0);
    }

}
