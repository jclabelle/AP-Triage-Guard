package eval.runner;

import eval.model.EvalCase;
import eval.model.Invocation;

import java.util.List;

public interface AgentRunner {

    List<Invocation> runCase(EvalCase evalCase) throws Exception;
}
