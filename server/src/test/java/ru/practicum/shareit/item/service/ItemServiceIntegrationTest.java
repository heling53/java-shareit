package ru.practicum.shareit.item.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import ru.practicum.shareit.booking.mapper.BookingMapperImpl;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.CommentMapperImpl;
import ru.practicum.shareit.item.mapper.ItemMapperImpl;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({ItemServiceImpl.class, ItemMapperImpl.class, BookingMapperImpl.class, CommentMapperImpl.class})
class ItemServiceIntegrationTest {

    @Autowired
    private ItemServiceImpl itemService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setName("Owner");
        owner.setEmail("owner@example.com");
        owner = userRepository.save(owner);

        anotherUser = new User();
        anotherUser.setName("Another");
        anotherUser.setEmail("another@example.com");
        anotherUser = userRepository.save(anotherUser);
    }

    @Test
    void create_ShouldSaveItemAndReturnDto() {
        ItemDto dto = ItemDto.builder()
                .name("Дрель")
                .description("Мощная")
                .available(true)
                .build();

        ItemDto saved = itemService.create(owner.getId(), dto);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Дрель");
        assertThat(saved.getAvailable()).isTrue();

        Item itemFromDb = entityManager.find(Item.class, saved.getId());
        assertThat(itemFromDb.getOwner().getId()).isEqualTo(owner.getId());
    }

    @Test
    void update_ShouldChangeItemFields() {
        ItemDto saved = itemService.create(owner.getId(),
                ItemDto.builder().name("Old").description("Old desc").available(true).build());

        ItemDto updateDto = ItemDto.builder()
                .name("New")
                .description("New desc")
                .available(false)
                .build();

        ItemDto updated = itemService.update(owner.getId(), saved.getId(), updateDto);

        assertThat(updated.getName()).isEqualTo("New");
        assertThat(updated.getDescription()).isEqualTo("New desc");
        assertThat(updated.getAvailable()).isFalse();
    }

    @Test
    void update_WhenNotOwner_ShouldThrowException() {
        ItemDto saved = itemService.create(owner.getId(),
                ItemDto.builder().name("Item").description("Desc").available(true).build());

        ItemDto updateDto = ItemDto.builder().name("Hack").build();

        assertThatThrownBy(() -> itemService.update(anotherUser.getId(), saved.getId(), updateDto))
                .isInstanceOf(ru.practicum.shareit.exception.NotFoundException.class);
    }

    @Test
    void getById_WhenOwner_ShouldReturnWithBookings() {
        ItemDto saved = itemService.create(owner.getId(),
                ItemDto.builder().name("Item").description("Desc").available(true).build());

        ItemDto result = itemService.getById(owner.getId(), saved.getId());

        assertThat(result.getLastBooking()).isNull();
        assertThat(result.getNextBooking()).isNull();
        assertThat(result.getComments()).isEmpty();
    }

    @Test
    void getByUserId_ShouldReturnAllItemsForOwner() {
        itemService.create(owner.getId(), ItemDto.builder().name("Item1").description("1").available(true).build());
        itemService.create(owner.getId(), ItemDto.builder().name("Item2").description("2").available(true).build());

        List<ItemDto> items = itemService.getByUserId(owner.getId());

        assertThat(items).hasSize(2);
    }

    @Test
    void search_ShouldReturnAvailableItemsContainingText() {
        itemService.create(owner.getId(), ItemDto.builder().name("Дрель").description("Инструмент").available(true).build());
        itemService.create(owner.getId(), ItemDto.builder().name("Молоток").description("Для гвоздей").available(true).build());
        itemService.create(owner.getId(), ItemDto.builder().name("Дрель недоступная").description("...").available(false).build());

        List<ItemDto> result = itemService.search("дрель");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Дрель");
    }

    @Test
    void search_WhenTextIsBlank_ShouldReturnEmptyList() {
        List<ItemDto> result = itemService.search("");
        assertThat(result).isEmpty();
    }
}