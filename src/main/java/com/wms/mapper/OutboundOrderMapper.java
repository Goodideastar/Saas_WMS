package com.wms.mapper;

import com.wms.entity.OutboundOrder;
import com.wms.entity.OutboundOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OutboundOrderMapper {
    OutboundOrder selectById(Long id);
    int insert(OutboundOrder order);
    int update(OutboundOrder order);
    int softDeleteById(Long id);

    List<OutboundOrder> selectPage(@Param("offset") int offset, @Param("limit") int limit,
                                   @Param("orderNo") String orderNo,
                                   @Param("status") String status,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);
    int selectCount(@Param("orderNo") String orderNo,
                    @Param("status") String status,
                    @Param("startTime") LocalDateTime startTime,
                    @Param("endTime") LocalDateTime endTime);

    List<OutboundOrderItem> selectItemsByOrderId(Long orderId);
    int insertItem(OutboundOrderItem item);

    Long selectSeq();
    int countByStatus(String status);
}
