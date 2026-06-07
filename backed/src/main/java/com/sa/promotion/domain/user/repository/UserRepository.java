package com.sa.promotion.domain.user.repository;

import com.sa.promotion.domain.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓储接口 (MyBatis Mapper)
 */
@Mapper
public interface UserRepository {
    void save(User user);
    void update(User user);
    void delete(String userId);
    Optional<User> findById(String userId);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    boolean exists(String userId);
}
