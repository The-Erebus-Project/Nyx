package io.github.vizanarkonin.nyx.Repositories;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

import io.github.vizanarkonin.nyx.Models.User;

public interface UserRepository extends CrudRepository<User, Integer> {
    User findByUsername(String username);
    User findByEmail(String email);
    User findById(int id);
    List<User> findByEnabled(boolean enabled);
}
