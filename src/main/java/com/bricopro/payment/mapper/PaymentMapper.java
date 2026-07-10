package com.bricopro.payment.mapper;

import com.bricopro.payment.dto.PaymentDtos.PaymentResponse;
import com.bricopro.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    @Mapping(target = "taskId", source = "task.id")
    PaymentResponse toResponse(Payment payment);
}
