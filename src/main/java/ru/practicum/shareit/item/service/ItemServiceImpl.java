package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import static org.springframework.data.domain.Sort.Direction.DESC;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final ItemMapper itemMapper;
    private final CommentMapper commentMapper;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public ItemDto create(Long userId, ItemDto itemDto) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        Item item = itemMapper.toItem(itemDto);
        item.setOwner(owner);
        return itemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    @Transactional
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));

        if (!item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Вещь не принадлежит пользователю");
        }

        if (itemDto.getName() != null) {
            item.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null) {
            item.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }

        return itemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    public ItemDto getById(Long itemId, Long userId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));

        ItemDto itemDto = itemMapper.toItemDto(item);

        itemDto.setComments(commentRepository.findAllByItemId(itemId).stream()
                .map(commentMapper::toCommentDto)
                .collect(Collectors.toList()));

        if (item.getOwner().getId().equals(userId)) {
            fillBookingInfo(itemDto, itemId);
        }

        return itemDto;
    }

    @Override
    public List<ItemDto> getByUserId(Long userId) {
        List<Item> items = itemRepository.findAllByOwnerId(userId);
        List<Long> itemIds = items.stream().map(Item::getId).collect(Collectors.toList());

        Map<Long, List<Booking>> bookingsMap = bookingRepository
                .findAllByItemIdInAndStatusNot(itemIds, BookingStatus.REJECTED, Sort.by(DESC, "start"))
                .stream()
                .collect(Collectors.groupingBy(b -> b.getItem().getId()));

        Map<Long, List<CommentDto>> commentsMap = commentRepository
                .findAllByItemIdIn(itemIds)
                .stream()
                .map(commentMapper::toCommentDto)
                .collect(Collectors.groupingBy(CommentDto::getItemId));

        LocalDateTime now = LocalDateTime.now();

        return items.stream()
                .map(item -> {
                    ItemDto dto = itemMapper.toItemDto(item);
                    List<Booking> itemBookings = bookingsMap.getOrDefault(item.getId(), Collections.emptyList());

                    dto.setLastBooking(findLast(itemBookings, now));
                    dto.setNextBooking(findNext(itemBookings, now));

                    dto.setComments(commentsMap.getOrDefault(item.getId(), Collections.emptyList()));

                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return itemRepository.search(text).stream()
                .map(itemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long itemId, CommentDto commentDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));

        boolean exists = bookingRepository.existsByItemIdAndBookerIdAndStatusAndEndBefore(
                itemId, userId, BookingStatus.APPROVED, LocalDateTime.now());

        if (!exists) {
            throw new ValidationException("Вы не можете оставить комментарий (аренда не завершена или не найдена)");
        }

        Comment comment = commentMapper.toComment(commentDto);
        comment.setItem(item);
        comment.setAuthor(user);
        comment.setCreated(LocalDateTime.now());

        return commentMapper.toCommentDto(commentRepository.save(comment));
    }

    private void fillBookingInfo(ItemDto itemDto, Long itemId) {
        LocalDateTime now = LocalDateTime.now();

        bookingRepository.findFirstByItemIdAndStartBeforeAndStatusOrderByEndDesc(itemId, now, BookingStatus.APPROVED)
                .ifPresent(last -> itemDto.setLastBooking(new BookingShortDto(last.getId(), last.getBooker().getId())));

        bookingRepository.findFirstByItemIdAndStartAfterAndStatusOrderByStartAsc(itemId, now, BookingStatus.APPROVED)
                .ifPresent(next -> itemDto.setNextBooking(new BookingShortDto(next.getId(), next.getBooker().getId())));
    }

    private BookingShortDto findLast(List<Booking> bookings, LocalDateTime now) {
        return bookings.stream()
                .filter(b -> !b.getStart().isAfter(now))
                .findFirst()
                .map(bookingMapper::toBookingShortDto)
                .orElse(null);
    }

    private BookingShortDto findNext(List<Booking> bookings, LocalDateTime now) {
        return bookings.stream()
                .filter(b -> b.getStart().isAfter(now))
                .reduce((first, second) -> second)
                .map(bookingMapper::toBookingShortDto)
                .orElse(null);
    }
}