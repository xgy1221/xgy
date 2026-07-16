package com.xgy.cloud.repository;

import com.xgy.cloud.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByOrgIdOrderByIdDesc(Long orgId);

    List<Order> findByOrgIdAndPartnerUserIdOrderByIdDesc(Long orgId, Long partnerUserId);

    List<Order> findByOrgIdAndCampusOrderByIdDesc(Long orgId, String campus);

    @Query("select o from Order o where o.orgId = :orgId and (:campus is null or o.campus = :campus)")
    List<Order> findForFinance(@Param("orgId") Long orgId, @Param("campus") String campus);
}
