package com.tool.sqlpad.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tool.sqlpad.enums.Role;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserInfo {
    @JsonProperty("user_id")
    private long userId;
    @JsonProperty("login_id")
    private String loginId;
    @JsonProperty("user_code")
    private String userCode;
    @JsonProperty("level_name")
    private String level;
    @JsonProperty("status")
    private String status;
    @JsonProperty("role")
    private Role role;
    @JsonProperty("currency_code")
    private String currency;
    @JsonProperty("balance")
    private BigDecimal balance;
    @JsonProperty("user_type")
    private String userType;
    @JsonProperty("brand")
    private String brand;
    @JsonProperty("brand_type")
    private String brandType;
    @JsonProperty("domain_name")
    private String domainName;
    @JsonProperty("pin")
    private String pin;
    @JsonProperty("first_name")
    private String firstName;
    @JsonProperty("last_name")
    private String lastName;
}
