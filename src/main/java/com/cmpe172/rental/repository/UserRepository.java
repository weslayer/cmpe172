package com.cmpe172.rental.repository;

import com.cmpe172.rental.model.AuthenticatedUser;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private static final RowMapper<AuthenticatedUser> MAPPER = (rs, i) -> new AuthenticatedUser(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("full_name"),
            rs.getString("role"),
            rs.getObject("provider_id", Long.class));

    private final NamedParameterJdbcTemplate jdbc;

    public UserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AuthenticatedUser> findByEmail(String email) {
        return jdbc.query("""
                SELECT u.id, u.email, u.password_hash, u.full_name, u.role, p.id AS provider_id
                FROM users u
                LEFT JOIN providers p ON p.user_id = u.id
                WHERE u.email = :email
                """, Map.of("email", email), MAPPER).stream().findFirst();
    }
}
