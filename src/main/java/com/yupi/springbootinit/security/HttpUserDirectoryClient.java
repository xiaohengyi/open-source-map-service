package com.yupi.springbootinit.security;

import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Component
public class HttpUserDirectoryClient implements UserDirectoryClient {

    private final RestTemplate restTemplate;
    private final UserAuthProperties props;

    public HttpUserDirectoryClient(UserAuthProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(props.getConnectTimeoutMs());
        f.setReadTimeout(props.getReadTimeoutMs());
        this.restTemplate = new RestTemplate(f);
    }

    @SuppressWarnings("unchecked")
    @Override
    public UserContext verify(String userId, String ticket) {
        // 允许 ticket 为空；只要 userId 有值即可尝试核验
        if (!StringUtils.hasText(props.getVerifyUrl()) || !StringUtils.hasText(userId)) {
            return null;
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(props.getVerifyUrl())
                .queryParam("userId", userId);
        if (StringUtils.hasText(ticket)) {
            builder.queryParam("ticket", ticket);
        }

        ResponseEntity<Map> resp = restTemplate.getForEntity(builder.toUriString(), Map.class);
        Map body = resp.getBody();
        if (body == null || !"OK".equalsIgnoreCase(String.valueOf(body.get("code")))) {
            return null;
        }

        String id = String.valueOf(body.getOrDefault("userId", userId));
        String name = String.valueOf(body.getOrDefault("userName", id));
        Object rolesObj = body.get("roles");
        List<String> roles;
        if (rolesObj instanceof Collection) {
            roles = new ArrayList<>();
            for (Object o : (Collection<?>) rolesObj) {
                if (o != null) roles.add(o.toString());
            }
        } else {
            roles = Collections.emptyList();
        }

        return UserContext.builder()
                .userId(id)
                .userName(name)
                .roles(roles)
                .ticket(ticket)      // 传回，便于排查
                .verified(true)
                .source("REMOTE")
                .build();
    }
}
