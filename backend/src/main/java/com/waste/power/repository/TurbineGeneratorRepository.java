package com.waste.power.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.waste.power.domain.TurbineGenerator;

import jakarta.persistence.LockModeType;

/**
 * 汽轮发电机组数据访问。
 */
public interface TurbineGeneratorRepository extends JpaRepository<TurbineGenerator, Long> {

    Optional<TurbineGenerator> findByUnitNo(String unitNo);

    /** 日结串行化：同机组并发日结时行锁排队，避免两人同时算出同一基数 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TurbineGenerator> findWithLockingByUnitNo(String unitNo);
}
