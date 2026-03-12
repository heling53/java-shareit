package ru.practicum.shareit.user.repository;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.user.model.User;

import java.util.*;

@Repository
public class InMemoryUserRepository implements UserRepository {
    private final Map<Long, User> users = new HashMap<>();
    private final Set<String> emails = new HashSet<>();
    private long idCounter = 1;

    @PostConstruct
    public void initEmailSet() {
        users.values().forEach(user -> emails.add(user.getEmail().toLowerCase()));
    }

    @Override
    public User save(User user) {
        user.setId(idCounter++);
        users.put(user.getId(), user);
        emails.add(user.getEmail().toLowerCase());
        return user;
    }

    @Override
    public User update(User user) {
        User oldUser = users.get(user.getId());
        if (oldUser != null) {
            emails.remove(oldUser.getEmail().toLowerCase());
        }

        users.put(user.getId(), user);
        emails.add(user.getEmail().toLowerCase());
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public void delete(Long id) {
        User user = users.remove(id);
        if (user != null) {
            emails.remove(user.getEmail().toLowerCase());
        }
    }

    @Override
    public boolean isEmailExists(String email, Long userId) {
        if (email == null) return false;

        String emailLower = email.toLowerCase();

        if (userId == null) {
            return emails.contains(emailLower);
        }

        User existingUser = users.get(userId);
        if (existingUser != null && existingUser.getEmail().equalsIgnoreCase(emailLower)) {
            return false;
        }

        return emails.contains(emailLower);
    }
}