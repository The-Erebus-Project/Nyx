package io.github.vizanarkonin.nyx.Repositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import io.github.vizanarkonin.nyx.Models.Authority;


public interface AuthoritiesRepository extends CrudRepository<Authority, Integer> {
    List<Authority> findByUsername(String username);
    void deleteAllByUsername(String username);
}
