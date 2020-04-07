package com.rental.workflow.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author gdyang
 */
@Data
@ConfigurationProperties(prefix = "spring.activiti.base")
public class ActivitiExtendProperties {
    /**
     * 流程图字体配置
     */
    private String activityFontName = "宋体";

    /**
     * 流程图字体配置
     */
    private String labelFontName = "宋体";
}
