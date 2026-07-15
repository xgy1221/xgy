package com.xgy.cloud.service;

import com.xgy.cloud.common.BizException;
import com.xgy.cloud.domain.PhoneWhitelist;
import com.xgy.cloud.repository.PhoneWhitelistRepository;
import com.xgy.cloud.security.SecurityUtils;
import com.xgy.cloud.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WhitelistService {

    private final PhoneWhitelistRepository phoneWhitelistRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UserPrincipal principal) {
        if (SecurityUtils.isParent()) {
            throw new BizException(403, "家长无权查看白名单");
        }
        Long orgId = SecurityUtils.requireOrgId();
        return phoneWhitelistRepository.findByOrgIdOrderByIdDesc(orgId).stream()
                .map(this::toView).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> add(UserPrincipal principal, WhitelistAddRequest req) {
        if (SecurityUtils.isParent()) {
            throw new BizException(403, "家长无权操作");
        }
        Long orgId = SecurityUtils.requireOrgId();
        if (!StringUtils.hasText(req.getPhone())) {
            throw new BizException("手机号不能为空");
        }
        phoneWhitelistRepository.findByOrgIdAndPhone(orgId, req.getPhone()).ifPresent(w -> {
            throw new BizException("该手机号已在白名单");
        });
        PhoneWhitelist w = new PhoneWhitelist();
        w.setOrgId(orgId);
        w.setPhone(req.getPhone());
        w.setParentName(req.getParentName());
        w.setNote(req.getNote());
        phoneWhitelistRepository.save(w);
        return toView(w);
    }

    private Map<String, Object> toView(PhoneWhitelist w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", w.getId());
        m.put("orgId", w.getOrgId());
        m.put("phone", w.getPhone());
        m.put("parentName", w.getParentName());
        m.put("note", w.getNote());
        return m;
    }

    @Data
    public static class WhitelistAddRequest {
        private String phone;
        private String parentName;
        private String note;
    }
}
