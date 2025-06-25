package com.iexceed.appzillon.sd.WorkFlow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.iexceed.appzillon.sd.WorkFlow.entity.Field;
import com.iexceed.appzillon.sd.WorkFlow.entity.Stage;
@Repository
public interface FieldRepository extends JpaRepository<Field, Integer> {
     
     List<Field> findByStage(Stage stage);
     
     void deleteByStage(Stage stage);

      // Find fields by stage number and workflow id
    @Query("SELECT f FROM Field f WHERE f.stage.stageNumber = :stageNumber AND f.stage.workflow.id = :workflowId")
    List<Field> findByStageNumberAndWorkflowId(@Param("stageNumber") int stageNumber, @Param("workflowId") int workflowId);
}