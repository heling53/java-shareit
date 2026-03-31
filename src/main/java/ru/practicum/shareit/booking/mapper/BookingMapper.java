package ru.practicum.shareit.booking.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingInputDto;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.booking.model.Booking;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookingMapper {

    BookingDto toBookingDto(Booking booking);

    Booking toBooking(BookingInputDto dto);

    List<BookingDto> toBookingDtoList(List<Booking> bookings);

    @Mapping(target = "bookerId", source = "booker.id")
    BookingShortDto toBookingShortDto(Booking booking);
}