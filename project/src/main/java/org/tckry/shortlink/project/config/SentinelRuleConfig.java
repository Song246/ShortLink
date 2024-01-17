package org.tckry.shortlink.project.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 初始化限流配置
 **/
@Component
public class SentinelRuleConfig implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule createOrderRule = new FlowRule();
        createOrderRule.setResource("create_short-link");
        createOrderRule.setGrade(RuleConstant.FLOW_GRADE_QPS);  // 限制规则·根据QPS　限流
        createOrderRule.setCount(1);    // 访问上限,比如每秒最多访问1 次
        rules.add(createOrderRule);
        // 通过组件把规则加载进去
        FlowRuleManager.loadRules(rules);

    }
}
