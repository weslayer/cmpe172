package com.cmpe172.rental.repository;

import com.cmpe172.rental.dto.FeaturedEquipmentDto;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SummaryRepository {

    private static final RowMapper<FeaturedEquipmentDto> FEATURED = (rs, i) -> new FeaturedEquipmentDto(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getString("asset_tag"),
            rs.getBigDecimal("daily_rate"),
            rs.getString("business_name"));

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

    public List<FeaturedEquipmentDto> findFeatured(int limit) {
        return jdbc.query("""
                SELECT sv.id, sv.name, sv.category, sv.asset_tag, sv.daily_rate, p.business_name
                FROM services sv
                JOIN providers p ON p.id = sv.provider_id
                WHERE sv.status = 'ACTIVE'
                ORDER BY sv.id
                LIMIT :limit
                """, Map.of("limit", limit), FEATURED);
    }

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Map.of(), Long.class);
        return n == null ? 0 : n;
    }
}
