package com.bricopro.user.mapper;

import com.bricopro.user.dto.UserDtos.*;
import com.bricopro.user.entity.*;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserSummary toSummary(User user);

    @Mapping(target = "user", source = "user")
    @Mapping(target = "services", source = "services")
    WorkerProfileResponse toWorkerResponse(WorkerProfile profile);

    WorkerServiceDto toServiceDto(WorkerService service);

    ClientProfileResponse toClientResponse(ClientProfile profile);

    AvailabilityResponse toAvailabilityResponse(WorkerAvailability availability);

    List<AvailabilityResponse> toAvailabilityList(List<WorkerAvailability> list);
}
