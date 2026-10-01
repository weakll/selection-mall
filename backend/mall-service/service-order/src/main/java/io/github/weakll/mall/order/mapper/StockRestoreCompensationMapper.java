package io.github.weakll.mall.order.mapper;

import io.github.weakll.mall.order.model.StockRestoreCompensation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface StockRestoreCompensationMapper {
    void save(StockRestoreCompensation compensation);

    List<StockRestoreCompensation> findPending(@Param("limit") int limit);

    int markProcessing(@Param("id") Long id, @Param("now") Date now);

    int markSuccess(@Param("id") Long id, @Param("now") Date now);

    int markRetry(@Param("id") Long id, @Param("retryCount") int retryCount,
                  @Param("errorMessage") String errorMessage, @Param("nextRetryTime") Date nextRetryTime,
                  @Param("now") Date now);
}
