package com.iexceed.appzillon.sd.WorkFlow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iexceed.appzillon.sd.WorkFlow.entity.Workflow;
@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Integer> {
    Workflow findByName(String name);

   
}