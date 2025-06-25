package com.iexceed.appzillon.sd.WorkFlow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iexceed.appzillon.sd.WorkFlow.entity.Stage;
import com.iexceed.appzillon.sd.WorkFlow.entity.Workflow;
@Repository
public interface StageRepository extends JpaRepository<Stage, Integer> {
    List<Stage> findByWorkflow(Workflow workflow);

    // Find stages by workflow and order them by stage number in ascending order
    List<Stage> findByWorkflowOrderByStageNumberAsc(Workflow workflow);

     // Custom method to find a Stage using workflowId and stageNumber
     Stage findByWorkflowIdAndStageNumber(int workflowId, int stageNumber);

     Stage findByStageName(String stageName);

}