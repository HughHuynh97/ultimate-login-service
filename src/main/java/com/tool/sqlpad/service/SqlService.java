package com.tool.sqlpad.service;

import com.tool.sqlpad.model.Batch;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.tool.sqlpad.constant.Environment.MERITO_PROD;

@Service
@RequiredArgsConstructor
public class SqlService {

    @Value("${sqlpad.user}")
    private String userName;
    @Value("${sqlpad.password}")
    private String password;
    private final RestTemplate restTemplate;
    private final MapperService mapperService;
    public String getCookie() {
        var response = restTemplate.postForEntity("https://dbtool.beatus88.com/api/signin", Map.of(
                "email", userName,
                "password", password), String.class);
        return Objects.requireNonNull(response.getHeaders().get("Set-Cookie")).get(0);
    }

    public String query(String query) {
        var cookie = this.getCookie();
        var headers = new HttpHeaders();
        headers.set("cookie", cookie);
        var params = Map.of("batchText", query, "connectionId", MERITO_PROD);
        var entity = new HttpEntity<>(params, headers);
        var batch = restTemplate.postForEntity("https://dbtool.beatus88.com/api/batches", entity, Batch.class);
        var statement = Objects.requireNonNull(batch.getBody()).getStatements().get(0);
        var columnRes = restTemplate.exchange("https://dbtool.beatus88.com/api/batches/" + statement.getBatchId(), HttpMethod.GET, new HttpEntity<>(headers), Batch.class);
        var apiHeadCol = "https://dbtool.beatus88.com/api/statements/" + statement.getId() + "/results";
        var res = restTemplate.exchange(apiHeadCol, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        var columns = Objects.requireNonNull(columnRes.getBody()).getStatements().get(0).getColumns();
        var body = mapperService.readValue(res.getBody(), Object[][].class);
        var result = new ArrayList<Map<String, Object>>();
        for (Object[] objects : body) {
            var mapCol = new HashMap<String, Object>();
            for (int i = 0; i < objects.length; i++) {
                mapCol.put(columns.get(i).getName(), objects[i]);
            }
            result.add(mapCol);
        }
        return mapperService.writeValueAsString(result);
    }
}
