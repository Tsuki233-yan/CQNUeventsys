package com.campus.eventsys.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 活动评价统计VO
 * 用于评价统计接口的响应数据结构
 */
@Data
@Schema(description = "活动评价统计信息")
public class ReviewStatisticsVO {

    @Schema(description = "活动ID", example = "1")
    private Long activityId;

    @Schema(description = "平均评分，保留1位小数", example = "4.5")
    private Double avgRating;

    @Schema(description = "总评价数", example = "20")
    private Integer totalCount;

    @Schema(description = "各星级分布，key为星级(5/4/3/2/1)，value为该星级的评价数")
    private Map<String, Integer> ratingDistribution;
}
