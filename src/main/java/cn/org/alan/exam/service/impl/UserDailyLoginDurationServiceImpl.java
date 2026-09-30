package cn.org.alan.exam.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cn.org.alan.exam.mapper.UserDailyLoginDurationMapper;
import cn.org.alan.exam.model.entity.UserDailyLoginDuration;
import cn.org.alan.exam.service.IUserDailyLoginDurationService;

/**
 * @Author Alan
 * @Version
 * @Date 2024/5/28 10:46 PM
 */
@Service
public class UserDailyLoginDurationServiceImpl extends ServiceImpl<UserDailyLoginDurationMapper, UserDailyLoginDuration> implements IUserDailyLoginDurationService {
}
