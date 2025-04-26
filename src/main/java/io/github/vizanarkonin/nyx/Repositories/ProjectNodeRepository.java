package io.github.vizanarkonin.nyx.Repositories;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import io.github.vizanarkonin.nyx.Models.ProjectNode;

@Repository
public interface ProjectNodeRepository extends CrudRepository<ProjectNode, Long> {
    ProjectNode findById(long id);
    List<ProjectNode> findAllByProjectId(long id);
    ProjectNode findByNodeId(String nodeId);
    boolean existsByNodeIdAndProjectId(String nodeId, long projectId);
    boolean existsByProjectIdAndIdIn(long projectId, List<Long> ids);
    void deleteAllByProjectId(long projectId);
    void deleteByNodeId(String nodeId);
    void deleteByIdIn(List<Long> ids);
}
