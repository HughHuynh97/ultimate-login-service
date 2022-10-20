package com.tool.sqlpad.security.partner;


import com.tool.sqlpad.enums.Role;
import com.tool.sqlpad.enums.StepLogin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface User {

    long getId();

    Role getRole();

    boolean isValid();

    long getExpiry();

    void keepAlive(HttpServletResponse response, HttpServletRequest request);

    boolean hasExpiried();

    void logout(HttpServletResponse response, HttpServletRequest request);

    StepLogin getStep();
}
