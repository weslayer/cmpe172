package com.cmpe172.rental.repository;

import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SummaryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public SummaryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long countProviders() {
        return count("SELECT count(*) FROM providers");
    }

    public long countActiveEquipment() {
        return count("SELECT count(*) FROM services WHERE status = 'ACTIVE'");
    }

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Map.of(), Long.class);
        return n == null ? 0 : n;
    }
}
