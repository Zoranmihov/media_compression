package spring_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import spring_backend.dtos.RegisterDTO;
import spring_backend.dtos.UserDTO;
import spring_backend.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toUser(User user);

    @Mapping(target = "password", ignore = true)
    User registerToUser(RegisterDTO registerDTO);
}
