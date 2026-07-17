package com.luckyby.personaldocs.config;
import com.luckyby.personaldocs.user.*; import org.springframework.beans.factory.annotation.Value; import org.springframework.boot.CommandLineRunner; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration; import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration public class BootstrapData {
  @Bean CommandLineRunner createAdmin(UserRepository users, PasswordEncoder encoder, @Value("${app.bootstrap-admin.password}") String password) { return args -> { if (users.findByUsername("admin").isEmpty()) { AppUser u=new AppUser();u.setUsername("admin");u.setPassword(encoder.encode(password));u.setRole("ROLE_ADMIN");users.save(u); } }; }
}
