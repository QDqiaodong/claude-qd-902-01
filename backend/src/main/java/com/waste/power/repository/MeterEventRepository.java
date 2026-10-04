package com.waste.power.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.waste.power.domain.MeterEvent;

/**
 * 表计回拨/更换事件数据访问。
 */
public interface MeterEventRepository extends JpaRepository<MeterEvent, Long> {

    Optional<MeterEvent> findByEventNo(String eventNo);

    /** 机组未校表恢复的挂起事件（暂停日结期间应至多一条） */
    List<MeterEvent> findByUnitNoAndEventStatusOrderByCreateTimeDesc(String unitNo, String eventStatus);

    List<MeterEvent> findTop50ByOrderByCreateTimeDesc();
}
