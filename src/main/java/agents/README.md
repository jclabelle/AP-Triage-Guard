# Overview
Wrapper classes are never instantiated. They serve as "factories" for the real workflow agents.

Their goal is to give ConfigAgentUtils a class name whose fromConfig method it can call, and have that method build the real workflow agent (SequentialAgent /
ParallelAgent / LoopAgent) plus sub‑agents.

# ADK Requirements
- ConfigAgentUtils.fromConfig(path):
    - Loads YAML into a BaseAgentConfig (or LlmAgentConfig).
    - Resolves agentClass via ComponentRegistry.resolveAgentClass.
    - Calls agentClass.getDeclaredMethod("fromConfig", configClass, String.class) and invokes it.
    - Casts the result to BaseAgent.

# Wrapper class solution
Our wrapper classes:
- Are visible as the agentClass string in YAML.
- Extend BaseAgent (so resolveAgentClass will accept it).
- Implement a static fromConfig(BaseAgentConfig, String) returning a BaseAgent.
- Are abstract to reinforce the fact they are never to be instantiated.

# Workflow callbacks from YAML
- Workflow agents can optionally configure before/after agent callbacks in YAML:
    - beforeAgentCallbacks:
        - name: "callbackName"
    - afterAgentCallbacks:
        - name: "callbackName"
- These fields are not part of ADK’s BaseAgentConfig. They are read by config.WorkflowCallbackConfigHelper, which:
    - Re-reads the YAML file for the workflow agent.
    - Resolves each callback name via ComponentRegistry.resolveBeforeAgentCallback / resolveAfterAgentCallback.
    - Returns lists of Callbacks.BeforeAgentCallback and Callbacks.AfterAgentCallback.
- Each workflow wrapper (Sequential / Parallel / Loop) uses WorkflowCallbackConfigHelper and passes the resolved callbacks into the corresponding builder methods.

# LoopAgent maxIterations from YAML
- ADK 0.3.0 does not expose loop parameters (maxIterations) via BaseAgentConfig.
- LoopWorkflowAgent adds optional support for a maxIterations field in the loop workflow YAML:
    - maxIterations: 3
- LoopWorkflowAgent.fromConfig:
    - Re-reads the loop YAML file.
    - If maxIterations is present and a positive integer (number or numeric string), calls LoopAgent.builder().maxIterations(value).
    - If maxIterations is absent or empty, the builder falls back to ADK’s default behavior.
    - If maxIterations is invalid (non-numeric or <= 0), configuration fails fast with a clear error message.

# Agent class registrations
- ConfigAgentUtils.fromConfig calls ComponentRegistry.resolveAgentClass(agentClassString).
- resolveAgentClass only knows about classes registered in ComponentRegistry (no fall back to Class.forName).
- The built‑in ADK agents are registered in initializePreWiredEntries, but wrapper classes are not.
- To work around this, wrapper classes are registered once at startup in Main through config.RegistrationService.registerWorkflowWrappers().
- Any new workflow class must be added to the registration calls inside config.RegistrationService.registerWorkflowWrappers().

# Function tool registrations
- LlmAgents resolve tools through the ComponentRegistry.
- For this to work, tools must be registered as FunctionTool instances inside ComponentRegistry under the name used in the YAML.
- The registration happens once at startup inb Main through config.RegistrationService.registerFunctionTools().
- Any new tool must be added to the registration calls inside config.RegistrationService.registerFunctionTools().
