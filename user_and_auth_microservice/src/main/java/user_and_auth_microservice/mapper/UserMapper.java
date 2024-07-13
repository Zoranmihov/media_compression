package user_and_auth_microservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import user_and_auth_microservice.dtos.RegisterDTO;
import user_and_auth_microservice.dtos.UserDTO;
import user_and_auth_microservice.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toUser(User user);

    @Mapping(target = "password", ignore = true)
    User registerToUser(RegisterDTO registerDTO);
}
