package com.xgy.cloud.service;

import com.xgy.cloud.domain.Org;
import com.xgy.cloud.domain.Student;
import com.xgy.cloud.repository.OrgRepository;
import com.xgy.cloud.repository.StudentRepository;
import com.xgy.cloud.security.SecurityUtils;
import com.xgy.cloud.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrgService {

    private final OrgRepository orgRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UserPrincipal principal) {
        if (SecurityUtils.isParent()) {
            Set<Long> orgIds = studentRepository.findByParentPhoneOrderByOrgIdAscIdAsc(principal.getPhone())
                    .stream().map(Student::getOrgId).collect(Collectors.toSet());
            return orgRepository.findAll().stream()
                    .filter(o -> orgIds.contains(o.getId()))
                    .map(this::toView)
                    .collect(Collectors.toList());
        }
        if (principal.getOrgId() != null) {
            return orgRepository.findById(principal.getOrgId()).stream()
                    .map(this::toView)
                    .collect(Collectors.toList());
        }
        return orgRepository.findAll().stream().map(this::toView).collect(Collectors.toList());
    }

    private Map<String, Object> toView(Org org) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", org.getId());
        m.put("code", org.getCode());
        m.put("name", org.getName());
        m.put("status", org.getStatus());
        return m;
    }
}
