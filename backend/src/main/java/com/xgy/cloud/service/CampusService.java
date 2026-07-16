package com.xgy.cloud.service;

import com.xgy.cloud.common.BizException;
import com.xgy.cloud.domain.Campus;
import com.xgy.cloud.repository.CampusRepository;
import com.xgy.cloud.security.SecurityUtils;
import com.xgy.cloud.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampusService {

    private final CampusRepository campusRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UserPrincipal principal) {
        Long orgId = principal.getOrgId();
        if (orgId == null) {
            if (SecurityUtils.isParent()) {
                throw new BizException("请先切换学员以确定机构");
            }
            orgId = SecurityUtils.requireOrgId();
        }
        return campusRepository.findByOrgId(orgId).stream().map(this::toView).collect(Collectors.toList());
    }

    private Map<String, Object> toView(Campus c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("orgId", c.getOrgId());
        m.put("name", c.getName());
        m.put("address", c.getAddress());
        m.put("status", c.getStatus());
        return m;
    }
}
