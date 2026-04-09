package ru.practicum.shareit.booking.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingInputDto;
import ru.practicum.shareit.booking.dto.BookingShortDto; // Явный импорт внешнего класса
import ru.practicum.shareit.booking.model.Booking;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BookingMapper {

    BookingDto toBookingDto(Booking booking);

    Booking toBooking(BookingInputDto dto);

    List<BookingDto> toBookingDtoList(List<Booking> bookings);

    @Mapping(target = "bookerId", source = "booker.id")
    BookingShortDto toBookingShortDto(Booking booking); // Возвращает внешний DTO
}