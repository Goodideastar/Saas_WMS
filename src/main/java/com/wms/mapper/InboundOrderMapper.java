package com.wms.mapper;

import com.wms.entity.InboundOrder;
import com.wms.entity.InboundOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InboundOrderMapper {
    InboundOrder selectById(Long id);
    int insert(InboundOrder order);
    int update(InboundOrder order);
    int softDeleteById(Long id);

    List<InboundOrder> selectPage(@Param("offset") int offset, @Param("limit") int limit,
                                  @Param("orderNo") String orderNo,
                                  @Param("status") String status,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);
    int selectCount(@Param("orderNo") String orderNo,
                    @Param("status") String status,
                    @Param("startTime") LocalDateTime startTime,
                    @Param("endTime") LocalDateTime endTime);

    List<InboundOrderItem> selectItemsByOrderId(Long orderId);
    int insertItem(InboundOrderItem item);

    Long selectSeq();
    int countByStatus(String status);
}
