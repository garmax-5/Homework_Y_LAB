package com.marketplace.out.repository;

import com.marketplace.model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UserRepositoryImpl implements UserRepository {
    private final Map<String, User> users = new HashMap<>();

    @Override
    public Optional<User> findByUserName(String userName) {
        return Optional.ofNullable(users.get(userName));
    }

    @Override
    public void save(User user) {
        users.put(user.getUserName(), user);
    }

    @Override
    public boolean existsById(long id) {
        for (User user : users.values()) {
            if (user.getId() == id) return true;
        }
        return false;
    }
}