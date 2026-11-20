package com.campus.eventsys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.eventsys.entity.Activity;
import com.campus.eventsys.mapper.ActivityMapper;
import com.campus.eventsys.service.ActivityService;
import org.springframework.stereotype.Service;

@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {
}
