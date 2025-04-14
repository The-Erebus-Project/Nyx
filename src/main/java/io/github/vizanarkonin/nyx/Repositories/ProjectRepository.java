package io.github.vizanarkonin.nyx.Repositories;

import org.springframework.data.repository.CrudRepository;

import io.github.vizanarkonin.nyx.Models.Project;

public interface ProjectRepository extends CrudRepository<Project, Integer> {
    Project findById(int id);
    Project findByName(String name);
}
