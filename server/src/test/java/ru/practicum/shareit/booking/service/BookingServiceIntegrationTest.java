package ru.practicum.shareit.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import ru.practicum.shareit.booking.dto.BookingInputDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.mapper.BookingMapperImpl;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({BookingServiceImpl.class, BookingMapperImpl.class})
class BookingServiceIntegrationTest {

    @Autowired
    private BookingServiceImpl bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    private User owner;
    private User booker;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder().name("Owner").email("owner@mail.com").build());
        booker = userRepository.save(User.builder().name("Booker").email("booker@mail.com").build());
        item = itemRepository.save(Item.builder()
                .name("Дрель")
                .description("Аккумуляторная")
                .available(true)
                .owner(owner)
                .build());
    }

    @Test
    void create_ShouldCreateBookingWithWaitingStatus() {
        BookingInputDto input = BookingInputDto.builder()
                .itemId(item.getId())
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2))
                .build();

        BookingDto created = bookingService.create(booker.getId(), input);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(created.getItem().getId()).isEqualTo(item.getId());
        assertThat(created.getBooker().getId()).isEqualTo(booker.getId());
    }

    @Test
    void create_WhenItemNotAvailable_ShouldThrowException() {
        item.setAvailable(false);
        itemRepository.save(item);

        BookingInputDto input = BookingInputDto.builder()
                .itemId(item.getId())
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2))
                .build();

        assertThatThrownBy(() -> bookingService.create(booker.getId(), input))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_WhenBookerIsOwner_ShouldThrowException() {
        BookingInputDto input = BookingInputDto.builder()
                .itemId(item.getId())
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2))
                .build();

        assertThatThrownBy(() -> bookingService.create(owner.getId(), input))
                .isInstanceOf(NotFoundException.class); // или ValidationException, зависит от реализации
    }

    @Test
    void approve_ShouldSetStatusToApproved() {
        BookingInputDto input = BookingInputDto.builder()
                .itemId(item.getId())
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2))
                .build();
        BookingDto created = bookingService.create(booker.getId(), input);

        BookingDto approved = bookingService.approve(owner.getId(), created.getId(), true);

        assertThat(approved.getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void getById_ShouldReturnBookingForOwnerOrBooker() {
        BookingInputDto input = BookingInputDto.builder()
                .itemId(item.getId())
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2))
                .build();
        BookingDto created = bookingService.create(booker.getId(), input);

        BookingDto foundByOwner = bookingService.getById(owner.getId(), created.getId());
        assertThat(foundByOwner).isNotNull();

        BookingDto foundByBooker = bookingService.getById(booker.getId(), created.getId());
        assertThat(foundByBooker).isNotNull();
    }

    @Test
    void getAllByBooker_ShouldReturnBookingsForBooker() {
        BookingInputDto input = BookingInputDto.builder()
                .itemId(item.getId())
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2))
                .build();
        bookingService.create(booker.getId(), input);

        List<BookingDto> bookings = bookingService.getAllByBooker(booker.getId(), "ALL");

        assertThat(bookings).hasSize(1);
    }

    @Test
    void getAllByOwner_ShouldReturnBookingsForOwnedItems() {
        BookingInputDto input = BookingInputDto.builder()
                .itemId(item.getId())
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2))
                .build();
        bookingService.create(booker.getId(), input);

        List<BookingDto> bookings = bookingService.getAllByOwner(owner.getId(), "ALL");

        assertThat(bookings).hasSize(1);
    }
}
