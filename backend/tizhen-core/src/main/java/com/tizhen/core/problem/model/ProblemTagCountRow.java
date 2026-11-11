package com.tizhen.core.problem.model;

/**
 * 已发布题目的专题标签聚合行。
 */
public class ProblemTagCountRow {

    private String value;
    private Long tagCount;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Long getTagCount() {
        return tagCount;
    }

    public void setTagCount(Long tagCount) {
        this.tagCount = tagCount;
    }
}
