package com.marketplace.out.repository;

import com.marketplace.model.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUserName(String userName);
    User save(User user);
    User update(User user);
    boolean existsById(long id);
}