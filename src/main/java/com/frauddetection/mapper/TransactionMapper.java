package com.frauddetection.mapper;

import com.frauddetection.dto.response.TransactionResponse;
import com.frauddetection.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    TransactionResponse toResponse(Transaction transaction);
}
