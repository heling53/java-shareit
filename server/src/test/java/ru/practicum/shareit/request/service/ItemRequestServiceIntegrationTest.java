package ru.practicum.shareit.request.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import ru.practicum.shareit.booking.mapper.BookingMapperImpl;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.CommentMapperImpl;
import ru.practicum.shareit.item.mapper.ItemMapperImpl;
import ru.practicum.shareit.item.service.ItemServiceImpl;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({ItemRequestServiceImpl.class, ItemServiceImpl.class,
        ItemMapperImpl.class, BookingMapperImpl.class, CommentMapperImpl.class})
class ItemRequestServiceIntegrationTest {

    @Autowired
    private ItemRequestServiceImpl requestService;

    @Autowired
    private ItemServiceImpl itemService;

    @Autowired
    private UserRepository userRepository;

    private User requester;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        requester = userRepository.save(User.builder().name("Requester").email("req@mail.com").build());
        anotherUser = userRepository.save(User.builder().name("Other").email("other@mail.com").build());
    }

    @Test
    void addRequest_ShouldSaveRequest() {
        ItemRequestDto dto = new ItemRequestDto("Нужна дрель");
        ItemRequestResponseDto saved = requestService.addRequest(requester.getId(), dto);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDescription()).isEqualTo("Нужна дрель");
        assertThat(saved.getCreated()).isNotNull();
        assertThat(saved.getItems()).isEmpty();
    }

    @Test
    void getUserRequests_ShouldReturnUserRequestsWithItems() {
        ItemRequestResponseDto request = requestService.addRequest(requester.getId(),
                new ItemRequestDto("Нужна дрель"));
        itemService.create(anotherUser.getId(), ItemDto.builder()
                .name("Дрель").description("Мощная").available(true)
                .requestId(request.getId()).build());

        List<ItemRequestResponseDto> requests = requestService.getUserRequests(requester.getId());

        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getItems()).hasSize(1);
        assertThat(requests.get(0).getItems().get(0).getName()).isEqualTo("Дрель");
    }

    @Test
    void getAllRequests_ShouldReturnRequestsFromOtherUsers() {
        requestService.addRequest(requester.getId(), new ItemRequestDto("Дрель"));
        requestService.addRequest(anotherUser.getId(), new ItemRequestDto("Молоток"));

        List<ItemRequestResponseDto> all = requestService.getAllRequests(anotherUser.getId(), 0, 10);

        assertThat(all).hasSize(1);
        assertThat(all.get(0).getDescription()).isEqualTo("Дрель");
    }

    @Test
    void getRequestById_ShouldReturnRequestWithItems() {
        ItemRequestResponseDto request = requestService.addRequest(requester.getId(),
                new ItemRequestDto("Дрель"));
        itemService.create(anotherUser.getId(), ItemDto.builder()
                .name("Дрель").description("...").available(true)
                .requestId(request.getId()).build());

        ItemRequestResponseDto found = requestService.getRequestById(anotherUser.getId(), request.getId());

        assertThat(found.getDescription()).isEqualTo("Дрель");
        assertThat(found.getItems()).hasSize(1);
    }
}