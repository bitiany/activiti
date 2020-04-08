package com.rental.workflow.controller;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;

/**
 * @author gdyang
 * @Description
 * @date 2020/4/3 上午10:43
 */
public class ProcessEventListener implements ExecutionListener {


    @Override
    public void notify(DelegateExecution delegateExecution) throws Exception {
        System.out.println("监听器:  " +  delegateExecution.getEventName());
    }

}
