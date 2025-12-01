package eval;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.ConfigAgentUtils;
import config.RegistrationService;
import eval.config.EvalConfig;
import eval.metrics.EvaluationCriterion;
import eval.metrics.ResponseMatchScoreCriterion;
import eval.metrics.ToolTrajectoryAvgScoreCriterion;
import eval.model.EvalSet;
import eval.model.results.EvalCaseResult;
import eval.model.results.EvalRunResult;
import eval.model.results.MetricScore;
import eval.runner.AgentRunner;
import eval.runner.JavaAdkAgentRunner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AgentEvaluationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @BeforeAll
    public static void registerWrappersAndTools() {
        RegistrationService registrationService = new RegistrationService();
        registrationService.registerWorkflowWrappers();
        registrationService.registerFunctionTools();
    }

    @Test
    public void helloWorldAgent_regression() throws Exception {
        // Load evalset and config from test resources
        EvalSet evalSet = MAPPER.readValue(
                Path.of("src/test/resources/eval/hello_world_agent/hello_world_agent_evalset.json").toFile(),
                EvalSet.class);

        EvalConfig evalConfig = MAPPER.readValue(
                Path.of("src/test/resources/eval/hello_world_agent/test_config.json").toFile(),
                EvalConfig.class);

        // Create agent runner (Java ADK integration) using the invoice pipeline config
        BaseAgent rootAgent = ConfigAgentUtils.fromConfig("src/main/resources/agents/invoice/ap-invoice-pipeline.yaml");
        AgentRunner runner = new JavaAdkAgentRunner(rootAgent);

        // Register supported metrics
        List<EvaluationCriterion> criteria = List.of(
                new ToolTrajectoryAvgScoreCriterion(),
                new ResponseMatchScoreCriterion()
                // Additional metrics (e.g. LLM-judge) can be added here later
        );

        AgentEvaluator evaluator = new AgentEvaluator(runner, criteria);

        EvalRunResult result = evaluator.evaluate(evalSet, evalConfig, /* numRuns = */ 1);

        // Fail test if any case violates thresholds
        assertTrue(result.passed(), () -> formatFailure(result));
    }

    private String formatFailure(EvalRunResult result) {
        StringBuilder sb = new StringBuilder("Evaluation failed:\n");
        for (EvalCaseResult cr : result.caseResults()) {
            if (!cr.passed()) {
                sb.append("Case ").append(cr.evalId()).append(" failed:\n");
                for (MetricScore ms : cr.metricScores()) {
                    if (!ms.passed()) {
                        sb.append("  - ")
                                .append(ms.metricName())
                                .append(": ")
                                .append(ms.score())
                                .append(" < threshold ")
                                .append(ms.threshold())
                                .append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }
}
