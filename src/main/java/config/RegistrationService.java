package config;

import com.google.adk.tools.FunctionTool;
import com.google.adk.utils.ComponentRegistry;
import tools.InvoiceRepoTool;
import tools.PoRepoTool;
import tools.PolicyThresholdsTool;

public class RegistrationService {

    public void registerWorkflowWrappers(){
        ComponentRegistry registry = ComponentRegistry.getInstance();

        registry.register(
                "agents.SequentialWorkflowAgent", agents.SequentialWorkflowAgent.class);
        registry.register(
                "agents.ParallelWorkflowAgent", agents.ParallelWorkflowAgent.class);
        registry.register(
                "agents.LoopWorkflowAgent", agents.LoopWorkflowAgent.class);
    }

    public void registerFunctionTools(){
        ComponentRegistry registry = ComponentRegistry.getInstance();

        registry.register(
                "tools.InvoiceRepoTool#getInvoice",
                FunctionTool.create(InvoiceRepoTool.class, "getInvoice"));

        registry.register(
                "tools.PoRepoTool#getPoForInvoice",
                FunctionTool.create(PoRepoTool.class, "getPoForInvoice"));

        registry.register(
                "tools.PolicyThresholdsTool#getThresholds",
                FunctionTool.create(PolicyThresholdsTool.class, "getThresholds"));

    }
}
