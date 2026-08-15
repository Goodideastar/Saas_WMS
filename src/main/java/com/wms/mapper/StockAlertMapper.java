package com.wms.mapper;

import com.wms.entity.StockAlert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface StockAlertMapper {
    StockAlert selectById(Long id);
    int insert(StockAlert alert);
    int update(StockAlert alert);
    int softDeleteById(Long id);

    List<StockAlert> selectPage(@Param("offset") int offset, @Param("limit") int limit,
                                @Param("status") String status,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);
    int selectCount(@Param("status") String status,
                    @Param("startTime") LocalDateTime startTime,
                    @Param("endTime") LocalDateTime endTime);

    int countByProduct(@Param("productId") Long productId);
    int countByStatus(@Param("status") String status);
    int countToday();
}
