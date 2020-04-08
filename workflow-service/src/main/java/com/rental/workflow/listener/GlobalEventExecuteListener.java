package com.rental.workflow.listener;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;

/**
 * @author gdyang
 * @Description
 * @date 2020/4/8 下午2:39
 */
public class GlobalEventExecuteListener implements ExecutionListener {
    @Override
    public void notify(DelegateExecution delegateExecution) throws Exception {
        switch (delegateExecution.getEventName()){
            case "start":
                System.out.println(String.format("节点开始事件：%s\n 业务key:%s\n当前节点:%s", delegateExecution.getEventName(),
                        delegateExecution.getProcessBusinessKey(), delegateExecution.getCurrentActivityName()));
                break;
            case "end":
                System.out.println(String.format("节点结束事件：%s\n 业务key:%s\n当前节点:%s", delegateExecution.getEventName(),
                        delegateExecution.getProcessBusinessKey(), delegateExecution.getCurrentActivityName()));
            case "take":
                System.out.println(String.format("连线事件：%s\n 业务key:%s\n当前节点:%s", delegateExecution.getEventName(),
                        delegateExecution.getProcessBusinessKey(), delegateExecution.getCurrentActivityName()));
        }
    }
}
