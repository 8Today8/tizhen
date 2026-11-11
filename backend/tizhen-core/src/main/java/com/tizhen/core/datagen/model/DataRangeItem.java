package com.tizhen.core.datagen.model;

import java.math.BigDecimal;

/**
 * 数据范围变量定义。随机模式按定义顺序输出一行变量值。
 */
public record DataRangeItem(
        String name,
        String type,
        BigDecimal min,
        BigDecimal max
) {
}
