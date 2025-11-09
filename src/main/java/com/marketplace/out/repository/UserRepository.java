package com.marketplace.out.repository;

import com.marketplace.model.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUserName(String userName);
    void save(User user);
    boolean existsById(long id);
}