package com.waste.power.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.waste.power.domain.GenerationDaily;

/**
 * 发电机组日结数据访问：机组 + 统计日唯一（库级唯一索引兜底重复日结）。
 */
public interface GenerationDailyRepository extends JpaRepository<GenerationDaily, Long> {

    Optional<GenerationDaily> findByUnitNoAndStatDate(String unitNo, LocalDate statDate);

    Optional<GenerationDaily> findByRequestId(String requestId);

    List<GenerationDaily> findByUnitNoOrderByStatDateDesc(String unitNo);

    List<GenerationDaily> findTop50ByOrderByStatDateDescIdDesc();

    boolean existsByUnitNoAndStatDate(String unitNo, LocalDate statDate);
}
