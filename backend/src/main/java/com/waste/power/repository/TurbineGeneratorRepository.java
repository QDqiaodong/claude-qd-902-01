package com.waste.power.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.waste.power.domain.TurbineGenerator;

/**
 * 汽轮发电机组数据访问。
 */
public interface TurbineGeneratorRepository extends JpaRepository<TurbineGenerator, Long> {

    Optional<TurbineGenerator> findByUnitNo(String unitNo);
}
