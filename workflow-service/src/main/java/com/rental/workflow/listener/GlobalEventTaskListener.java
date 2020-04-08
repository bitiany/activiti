package com.rental.workflow.listener;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;

/**
 * @author gdyang
 * @Description
 * @date 2020/4/8 上午10:43
 */
public class GlobalEventTaskListener implements TaskListener {


    @Override
    public void notify(DelegateTask delegateTask) {
        System.out.println(String.format("触发邮件通知：\n事件名称:%s\n审批人:%s \n" , delegateTask.getEventName(), delegateTask.getAssignee()));
    }
}
