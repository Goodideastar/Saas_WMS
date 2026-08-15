package com.wms.mapper;

import com.wms.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductMapper {
    Product selectById(Long id);
    int insert(Product product);
    int update(Product product);
    int softDeleteById(Long id);

    @Select("SELECT * FROM product WHERE id = #{id} AND is_deleted = 0 FOR UPDATE")
    Product selectForUpdate(Long id);

    List<Product> selectPage(@Param("offset") int offset, @Param("limit") int limit,
                             @Param("category") String category,
                             @Param("keyword") String keyword,
                             @Param("minStock") Integer minStock,
                             @Param("maxStock") Integer maxStock,
                             @Param("status") Integer status);
    int selectCount(@Param("category") String category,
                    @Param("keyword") String keyword,
                    @Param("minStock") Integer minStock,
                    @Param("maxStock") Integer maxStock,
                    @Param("status") Integer status);

    List<Product> selectList(@Param("productCode") String productCode);

    int updateStatusById(@Param("id") Long id, @Param("status") Integer status);
    int adjustStockById(@Param("id") Long id, @Param("quantity") int quantity);
    int updateVersionById(@Param("id") Long id, @Param("version") int version,
                          @Param("currentStock") int currentStock);

    int countLowStock();
}
