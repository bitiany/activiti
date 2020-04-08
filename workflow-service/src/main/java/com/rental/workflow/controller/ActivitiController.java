package com.rental.workflow.controller;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rental.workflow.config.properties.ActivitiExtendProperties;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.image.ProcessDiagramGenerator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.*;


/**
 * @author gdyang
 */
@RestController
@RequestMapping("/activiti")
public class ActivitiController {

    @Autowired
    private ActivitiExtendProperties properties;
    @Autowired
    public RepositoryService repositoryService;
    @Autowired
    public TaskService taskService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;
    /**
     * 创建模型
     */
    @RequestMapping("/create")
    public void create(HttpServletRequest request, HttpServletResponse response) {
        try {
            ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();

            RepositoryService repositoryService = processEngine.getRepositoryService();

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode editorNode = objectMapper.createObjectNode();
            editorNode.put("id", "canvas");
            editorNode.put("resourceId", "canvas");
            ObjectNode stencilSetNode = objectMapper.createObjectNode();
            stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
            editorNode.put("stencilset", stencilSetNode);
            Model modelData = repositoryService.newModel();

            ObjectNode modelObjectNode = objectMapper.createObjectNode();
            modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, "hello1111");
            modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
            String description = "hello1111";
            modelObjectNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, description);
            modelData.setMetaInfo(modelObjectNode.toString());
            modelData.setName("hello1111");
            modelData.setKey("12313123");

            //保存模型
            repositoryService.saveModel(modelData);
            repositoryService.addModelEditorSource(modelData.getId(), editorNode.toString().getBytes("utf-8"));
            response.sendRedirect(request.getContextPath() + "/modeler.html?modelId=" + modelData.getId());
        } catch (Exception e) {
            System.out.println("创建模型失败：");
        }
    }


    @RequestMapping(value = "/deploy/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "部署发布模型")
    public String deploy(@PathVariable String id) {
        // 获取模型
        Model modelData = repositoryService.getModel(id);
        byte[] bytes = repositoryService.getModelEditorSource(modelData.getId());
        if (bytes == null) {
        }

        try {
            JsonNode modelNode = new ObjectMapper().readTree(bytes);

            BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
            if(model.getProcesses().size()==0){
            }
            byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(model);

            // 部署发布模型流程
            String processName = modelData.getName() + ".bpmn20.xml";
            Deployment deployment = repositoryService.createDeployment()
                    .name(modelData.getName())
                    .addString(processName, new String(bpmnBytes, "UTF-8"))
                    .deploy();

            // 设置流程分类 保存扩展流程至数据库
            List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).list();
            if(CollectionUtils.isNotEmpty(list)){
                return list.stream().findFirst().get().getId();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "success";
    }

    @RequestMapping("start/{pid}")
    @ResponseBody
    public String apply(@PathVariable("pid") String id, @RequestParam(name = "assigner", required = true) String assigner, HttpServletRequest request){
        // 启动流程用户
        identityService.setAuthenticatedUserId(assigner);
        // 启动流程 需传入业务表id变量
        Map paramMap = new HashMap();
        Map map = request.getParameterMap();
        Iterator<Map.Entry<String, String[]>> it = map.entrySet().iterator();
        Map.Entry<String, String[]> entry;
        while(it.hasNext()){
            entry = it.next();
            paramMap.put(entry.getKey(), entry.getValue()[0]);
        }
        ProcessInstance pi = runtimeService.startProcessInstanceById(id, "businessKey", paramMap);
        // 设置流程实例名称
        runtimeService.setProcessInstanceName(pi.getId(), pi.getName());
        Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        return task.getId() + ":" + task.getProcessInstanceId() + ":" + task.getAssignee();
    }


    @RequestMapping(value = "/pass", method = RequestMethod.GET)
    @ApiOperation(value = "任务节点审批通过")
    public String pass(@ApiParam("任务id") @RequestParam String id,
                               @ApiParam("流程实例id") @RequestParam String procInstId,
                                @RequestParam(name = "assigner", required = true) String assignee,
                               @ApiParam("优先级") @RequestParam(required = false) Integer priority,
                               @ApiParam("意见评论") @RequestParam(required = false) String comment,
                               @ApiParam("是否发送站内消息") @RequestParam(defaultValue = "false") Boolean sendMessage,
                               @ApiParam("是否发送短信通知") @RequestParam(defaultValue = "false") Boolean sendSms,
                               @ApiParam("是否发送邮件通知") @RequestParam(defaultValue = "false") Boolean sendEmail){

        if(StringUtils.isBlank(comment)){
            comment = "";
        }
        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(procInstId).singleResult();
        Task task = taskService.createTaskQuery().taskId(id).singleResult();
        if(null != task) {
            if (!assignee.equals(task.getAssignee())) {
                return "无权限";
            }
            if(StringUtils.isNotBlank(task.getOwner())&&!("RESOLVED").equals(task.getDelegationState().toString())){
                // 未解决的委托任务 先resolve
                String oldAssignee = task.getAssignee();
                taskService.resolveTask(id);
                taskService.setAssignee(id, oldAssignee);
            }
        }
        taskService.addComment(id, procInstId, comment);
        taskService.complete(id);
        task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        if(null == task){
            return "finish";
        }
        return task.getId() + " : " + task.getProcessInstanceId()+ " : " + task.getAssignee();
        // 记录实际审批人员
//        iHistoryIdentityService.insert(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()),
//                ActivitiConstant.EXECUTOR_TYPE, securityUtil.getCurrUser().getId(), id, procInstId);
//        return ResultUtil.success("操作成功");
    }

    @RequestMapping(value = "/getHighlightImg/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "获取高亮实时流程图")
    public void getHighlightImg(@ApiParam("流程实例id") @PathVariable String id,
                                HttpServletResponse response){

        InputStream inputStream = null;
        ProcessInstance pi = null;
        String picName = "";
        // 查询历史
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery().processInstanceId(id).singleResult();
        if (hpi.getEndTime() != null) {
            // 已经结束流程获取原图
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().processDefinitionId(hpi.getProcessDefinitionId()).singleResult();
            picName = pd.getDiagramResourceName();
            inputStream = repositoryService.getResourceAsStream(pd.getDeploymentId(), pd.getDiagramResourceName());
        } else {
            pi = runtimeService.createProcessInstanceQuery().processInstanceId(id).singleResult();
            BpmnModel bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());

            List<String> highLightedActivities = new ArrayList<String>();
            // 高亮任务节点
            List<Task> tasks = taskService.createTaskQuery().processInstanceId(id).list();
            for (Task task : tasks) {
                highLightedActivities.add(task.getTaskDefinitionKey());
            }

            List<String> highLightedFlows = new ArrayList<String>();
            ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
            inputStream = diagramGenerator.generateDiagram(bpmnModel, "png", highLightedActivities, highLightedFlows,
                    properties.getActivityFontName(), properties.getLabelFontName(), properties.getLabelFontName(),null, 1.0);
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().processDefinitionId(pi.getProcessDefinitionId()).singleResult();
            picName = pd.getDiagramResourceName();
        }
        try {
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(picName, "UTF-8"));
            byte[] b = new byte[1024];
            int len = -1;
            while ((len = inputStream.read(b, 0, 1024)) != -1) {
                response.getOutputStream().write(b, 0, len);
            }
            response.flushBuffer();
        } catch (IOException e) {
          e.printStackTrace();
        }
    }
}
