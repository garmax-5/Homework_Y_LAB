package com.marketplace.out.filestore;

import com.marketplace.model.User;
import com.marketplace.out.repository.UserRepository;

import java.io.*;
import java.util.*;

public class UserFileStore implements UserRepository {
    private final String filePath;
    private final Map<String, User> users = new HashMap<>(); // key = username

    public UserFileStore(String filePath) {
        this.filePath = filePath;
        load();
    }

    private void load() {
        File file = new File(filePath);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 4); // id,username,password,role
                if (parts.length == 4) {
                    long id = Long.parseLong(parts[0]);
                    String username = parts[1];
                    String password = parts[2];
                    User.Role role = User.Role.valueOf(parts[3]);
                    users.put(username, new User(id, username, password, role));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (User user : users.values()) {
                String line = user.getId() + "," + user.getUserName() + "," + user.getPassword() + "," + user.getRole();
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<User> findByUserName(String userName) {
        return Optional.ofNullable(users.get(userName));
    }

    @Override
    public void save(User user) {
        users.put(user.getUserName(), user);
        saveToFile();
    }

    @Override
    public boolean existsById(long id) {
        for (User user : users.values()) {
            if (user.getId() == id) return true;
        }
        return false;
    }

    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }
}
