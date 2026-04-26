package com.account.dao;

import com.account.entity.Record;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface RecordMapper {

    Record selectById(@Param("id") Long id);

    List<Record> selectByUserId(@Param("userId") Long userId);

    List<Record> selectByCondition(@Param("userId") Long userId,
                                   @Param("type") Integer type,
                                   @Param("category") String category,
                                   @Param("accountType") String accountType,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    int insert(Record record);

    int update(Record record);

    int deleteById(@Param("id") Long id);

    int deleteByUserId(@Param("userId") Long userId);

    Map<String, Object> selectMonthlyStatistics(@Param("userId") Long userId,
                                                 @Param("year") Integer year,
                                                 @Param("month") Integer month);

    List<Map<String, Object>> selectCategoryStatistics(@Param("userId") Long userId,
                                                        @Param("type") Integer type,
                                                        @Param("year") Integer year,
                                                        @Param("month") Integer month);

    List<Map<String, Object>> selectMonthlyTrend(@Param("userId") Long userId,
                                                  @Param("year") Integer year);

}
