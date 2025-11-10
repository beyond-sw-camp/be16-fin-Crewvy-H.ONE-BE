package com.crewvy.workforce_service.salary.repository;

import com.crewvy.workforce_service.salary.entity.IncomeTax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IncomeTaxRepository extends JpaRepository<IncomeTax, UUID>, IncomeTaxRepositoryCustom {

}
