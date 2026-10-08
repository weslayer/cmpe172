package com.cmpe172.rental.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record CreateSlotRequest(
        @NotNull Long serviceId,
        @NotNull @Future OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt) {

    @AssertTrue(message = "endsAt must be after startsAt")
    public boolean isEndAfterStart() {
        return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
    }
}
