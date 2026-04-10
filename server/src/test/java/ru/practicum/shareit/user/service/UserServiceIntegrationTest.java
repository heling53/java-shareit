package ru.practicum.shareit.user.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapperImpl;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;


import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({UserServiceImpl.class, UserMapperImpl.class})
class UserServiceIntegrationTest {

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private UserDto userDto;

    @BeforeEach
    void setUp() {
        userDto = UserDto.builder()
                .name("John Doe")
                .email("john@example.com")
                .build();
    }

    @Test
    void create_ShouldSaveUserAndReturnDto() {
        UserDto saved = userService.create(userDto);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("John Doe");
        assertThat(saved.getEmail()).isEqualTo("john@example.com");

        User userFromDb = entityManager.find(User.class, saved.getId());
        assertThat(userFromDb).isNotNull();
    }

    @Test
    void update_ShouldModifyExistingUser() {
        UserDto saved = userService.create(userDto);
        UserDto updateDto = UserDto.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .build();

        UserDto updated = userService.update(saved.getId(), updateDto);

        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getName()).isEqualTo("Jane Doe");
        assertThat(updated.getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void getById_ShouldReturnUser() {
        UserDto saved = userService.create(userDto);

        UserDto found = userService.getById(saved.getId());

        assertThat(found).isEqualTo(saved);
    }

    @Test
    void getById_WhenUserNotFound_ShouldThrowException() {
        assertThatThrownBy(() -> userService.getById(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getAll_ShouldReturnAllUsers() {
        userService.create(userDto);
        userService.create(UserDto.builder().name("Second").email("second@example.com").build());

        List<UserDto> users = userService.getAll();

        assertThat(users).hasSize(2);
    }

    @Test
    void delete_ShouldRemoveUser() {
        UserDto saved = userService.create(userDto);
        userService.delete(saved.getId());

        assertThatThrownBy(() -> userService.getById(saved.getId()))
                .isInstanceOf(NotFoundException.class);
    }
}