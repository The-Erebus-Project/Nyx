package io.github.vizanarkonin.nyx.Repositories;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

import io.github.vizanarkonin.nyx.Models.ProjectRun;

public interface ProjectRunRepository extends CrudRepository<ProjectRun, Long> {
    ProjectRun findById(long id);
    List<ProjectRun> findAllByProjectId(long id);
    List<ProjectRun> findAllByProjectIdOrderByIdDesc(long id);
    List<ProjectRun> findByProjectIdAndStartTimeAndFinishTime(long id, long startTime, long finishTime);
    ProjectRun findTopByProjectIdOrderByIdDesc(long id);
    void deleteAllByProjectId(long projectId);
}
