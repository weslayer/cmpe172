package com.cmpe172.rental.repository;

import com.cmpe172.rental.dto.EquipmentDto;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

// Equipment items live in the services table.
@Repository
public class EquipmentRepository {

    private static final RowMapper<EquipmentDto> MAPPER = (rs, i) -> new EquipmentDto(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getString("asset_tag"),
            rs.getBigDecimal("daily_rate"),
            rs.getLong("provider_id"),
            rs.getString("business_name"));

    private final NamedParameterJdbcTemplate jdbc;

    public EquipmentRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<EquipmentDto> findActive(Long providerId, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource("limit", limit);
        String byProvider = "";
        if (providerId != null) {
            byProvider = " AND sv.provider_id = :providerId";
            params.addValue("providerId", providerId);
        }
        return jdbc.query("""
                SELECT sv.id, sv.name, sv.category, sv.asset_tag, sv.daily_rate, sv.provider_id, p.business_name
                FROM services sv
                JOIN providers p ON p.id = sv.provider_id
                WHERE sv.status = 'ACTIVE'
                """ + byProvider + " ORDER BY sv.id LIMIT :limit", params, MAPPER);
    }

    public Optional<Long> findProviderId(long serviceId) {
        return jdbc.queryForList("SELECT provider_id FROM services WHERE id = :id",
                Map.of("id", serviceId), Long.class).stream().findFirst();
    }
}
