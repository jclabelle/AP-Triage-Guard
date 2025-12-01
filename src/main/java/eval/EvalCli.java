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

import java.nio.file.Path;
import java.util.List;

/**
 * Simple CLI entrypoint for running agent evaluations.
 *
 * Usage:
 *   java -cp target/minimal-agent-1.0-SNAPSHOT.jar \
 *        eval.EvalCli \
 *        <evalset.json> \
 *        <eval_config.json> \
 *        <agent_config.yaml> \
 *        [numRuns]
 */
public final class EvalCli {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: EvalCli <evalset.json> <eval_config.json> <agent_config.yaml> [numRuns]");
            System.exit(1);
        }

        Path evalSetPath = Path.of(args[0]);
        Path evalConfigPath = Path.of(args[1]);
        String agentConfigPath = args[2];
        int numRuns = args.length >= 4 ? Integer.parseInt(args[3]) : 1;

        // Ensure YAML-configured workflow wrappers and function tools are registered
        RegistrationService registrationService = new RegistrationService();
        registrationService.registerWorkflowWrappers();
        registrationService.registerFunctionTools();

        EvalSet evalSet = MAPPER.readValue(evalSetPath.toFile(), EvalSet.class);
        EvalConfig evalConfig = MAPPER.readValue(evalConfigPath.toFile(), EvalConfig.class);

        BaseAgent rootAgent = ConfigAgentUtils.fromConfig(agentConfigPath);
        AgentRunner runner = new JavaAdkAgentRunner(rootAgent);

        List<EvaluationCriterion> criteria = List.of(
                new ToolTrajectoryAvgScoreCriterion(),
                new ResponseMatchScoreCriterion()
        );

        AgentEvaluator evaluator = new AgentEvaluator(runner, criteria);

        EvalRunResult result = evaluator.evaluate(evalSet, evalConfig, numRuns);

        boolean passed = result.passed();

        System.out.println("Eval set: " + result.evalSetId());
        for (EvalCaseResult caseResult : result.caseResults()) {
            System.out.println("Case " + caseResult.evalId() + ": " + (caseResult.passed() ? "PASSED" : "FAILED"));
            for (MetricScore ms : caseResult.metricScores()) {
                Double threshold = ms.threshold();
                System.out.printf(
                        "  - %s: %.4f (threshold=%s, passed=%s)%n",
                        ms.metricName(),
                        ms.score(),
                        threshold == null ? "n/a" : threshold,
                        ms.passed()
                );
            }
        }

        System.out.println("Overall: " + (passed ? "PASSED" : "FAILED"));
        if (!passed) {
            System.exit(2);
        }
    }
}
