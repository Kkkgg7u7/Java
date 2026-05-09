package com.account.service.impl;

import com.account.dao.RecordMapper;
import com.account.entity.Record;
import com.account.service.RecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class RecordServiceImpl implements RecordService {

    @Autowired
    private RecordMapper recordMapper;

    @Override
    public Record getById(Long id) {
        if (id == null) {
            return null;
        }
        return recordMapper.selectById(id);
    }

    @Override
    public List<Record> getByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return recordMapper.selectByUserId(userId);
    }

    @Override
    public List<Record> getByCondition(Long userId, Integer type, String category,
                                        String accountType, LocalDate startDate, LocalDate endDate) {
        if (userId == null) {
            return null;
        }
        return recordMapper.selectByCondition(userId, type, category, accountType, startDate, endDate);
    }

    @Override
    public boolean add(Record record) {
        if (record == null || record.getUserId() == null) {
            return false;
        }
        if (record.getRecordDate() == null) {
            record.setRecordDate(LocalDate.now());
        }
        record.setCreateTime(LocalDateTime.now());
        return recordMapper.insert(record) > 0;
    }

    @Override
    public boolean update(Record record) {
        if (record == null || record.getId() == null) {
            return false;
        }
        return recordMapper.update(record) > 0;
    }

    @Override
    public boolean delete(Long id) {
        if (id == null) {
            return false;
        }
        return recordMapper.deleteById(id) > 0;
    }

    @Override
    public Map<String, Object> getMonthlyStatistics(Long userId, Integer year, Integer month) {
        if (userId == null) {
            return null;
        }
        return recordMapper.selectMonthlyStatistics(userId, year, month);
    }

    @Override
    public List<Map<String, Object>> getCategoryStatistics(Long userId, Integer type, Integer year, Integer month) {
        if (userId == null) {
            return null;
        }
        return recordMapper.selectCategoryStatistics(userId, type, year, month);
    }

    @Override
    public List<Map<String, Object>> getMonthlyTrend(Long userId, Integer year) {
        if (userId == null) {
            return null;
        }
        return recordMapper.selectMonthlyTrend(userId, year);
    }

}
