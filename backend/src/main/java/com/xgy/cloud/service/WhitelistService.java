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
        SecurityUtils.requireStaff();
        Long orgId = SecurityUtils.requireOrgId();
        return phoneWhitelistRepository.findByOrgIdOrderByIdDesc(orgId).stream()
                .map(this::toView).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> add(UserPrincipal principal, WhitelistAddRequest req) {
        SecurityUtils.requireAcademicOrAdmin();
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

    @Transactional
    public void remove(UserPrincipal principal, Long id) {
        SecurityUtils.requireAcademicOrAdmin();
        Long orgId = SecurityUtils.requireOrgId();
        PhoneWhitelist w = phoneWhitelistRepository.findById(id)
                .orElseThrow(() -> new BizException("白名单不存在"));
        if (!orgId.equals(w.getOrgId())) {
            throw new BizException(403, "无权删除");
        }
        phoneWhitelistRepository.delete(w);
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
