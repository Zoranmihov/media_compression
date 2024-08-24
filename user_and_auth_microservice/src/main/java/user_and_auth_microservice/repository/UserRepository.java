package user_and_auth_microservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import user_and_auth_microservice.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    @Query("SELECT u FROM User u WHERE u.email = :email OR u.username = :username")
    Optional<User> findByEmailOrUsernameForRegistration(@Param("email") String email, @Param("username") String username);
    @Query("SELECT u FROM User u WHERE u.email = :login OR u.username = :login")
    Optional<User> findByEmailOrUsername(@Param("login") String login);
}
