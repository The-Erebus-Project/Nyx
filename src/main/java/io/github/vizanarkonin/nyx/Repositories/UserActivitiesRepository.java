package io.github.vizanarkonin.nyx.Repositories;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

import io.github.vizanarkonin.nyx.Models.UserActivity;

public interface UserActivitiesRepository extends CrudRepository<UserActivity, Integer> {
    List<UserActivity> findByUserId(long userId);
    List<UserActivity> findTop200ByOrderByIdDesc();
}
