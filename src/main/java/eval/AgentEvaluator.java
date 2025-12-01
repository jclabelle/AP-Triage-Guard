package eval;

import eval.config.CriterionConfig;
import eval.config.EvalConfig;
import eval.metrics.EvaluationCriterion;
import eval.model.EvalCase;
import eval.model.EvalSet;
import eval.model.Invocation;
import eval.model.results.EvalCaseResult;
import eval.model.results.EvalRunResult;
import eval.model.results.MetricScore;
import eval.runner.AgentRunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class AgentEvaluator {

    private final AgentRunner agentRunner;
    private final Map<String, EvaluationCriterion> availableCriteria;

    private static final Logger LOG = LoggerFactory.getLogger(AgentEvaluator.class);

    public AgentEvaluator(AgentRunner agentRunner,
                          List<EvaluationCriterion> criteria) {
        this.agentRunner = agentRunner;
        this.availableCriteria = criteria.stream()
                .collect(Collectors.toMap(EvaluationCriterion::name, c -> c));
    }

    // Evaluate all cases in the EvalSet
    public EvalRunResult evaluate(EvalSet evalSet,
                                  EvalConfig evalConfig,
                                  int numRuns)
    {
        List<EvalCaseResult> caseResults = new ArrayList<>();

        for (EvalCase evalCase : evalSet.evalCases()){
            caseResults.add(
                    evaluateCaseMultipleRuns(evalCase, evalConfig, numRuns)
            );
        }

        return new EvalRunResult(evalSet.evalSetId(), caseResults);
    }

    private EvalCaseResult evaluateCaseMultipleRuns(EvalCase evalCase,
                                                    EvalConfig evalConfig,
                                                    int numRuns)
    {
        Map<String, List<Double>> metricToScores = new HashMap<>();

        for (int run = 0; run < numRuns; run++) {
            List<Invocation> actualConversation;
            long startMillis = System.currentTimeMillis();
            LOG.info("Starting eval case '{}' (run {}/{})", evalCase.evalId(), run + 1, numRuns);
            try {
                actualConversation = agentRunner.runCase(evalCase);
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startMillis;
                LOG.error(
                        "Error while running eval case '{}' on run {} after {} ms. " +
                                "Recording 0.0 for all metrics.",
                        evalCase.evalId(), run + 1, elapsed, e
                );
                // Treat agent failures as explicit eval failures by recording a score of 0
                // for each known metric in this run.
                for (String metricName : availableCriteria.keySet()) {
                    metricToScores
                            .computeIfAbsent(metricName, k -> new ArrayList<>())
                            .add(0.0);
                }
                continue;
            }

            long elapsed = System.currentTimeMillis() - startMillis;
            LOG.info(
                    "Eval case '{}' run {} completed in {} ms with {} actual invocations.",
                    evalCase.evalId(), run + 1, elapsed, actualConversation.size()
            );

            for (Map.Entry<String, CriterionConfig> entry : evalConfig.criteria().entrySet()) {
                String metricName = entry.getKey();
                CriterionConfig criterionConfig = entry.getValue();
                EvaluationCriterion criterion = availableCriteria.get(metricName);
                if (criterion == null) {
                    continue; // unknown metric; ignore until a criterion is implemented
                }

                double score = criterion.evaluate(evalCase, actualConversation, criterionConfig);

                metricToScores
                        .computeIfAbsent(metricName, k -> new ArrayList<>())
                        .add(score);
            }
        }

        // Scores: aggregate and compare to threshold
        // Assumes CriterionConfig.threshold() is filled in (i.e. use the explicit-object form in JSON).
        List<MetricScore> metricScores = new ArrayList<>();
        for (Map.Entry<String, List<Double>> entry : metricToScores.entrySet()) {
            String metricName = entry.getKey();
            List<Double> scores = entry.getValue();
            double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.01);

            CriterionConfig cfg = evalConfig.criteria().get(metricName);
            Double threshold = cfg != null ? cfg.threshold() : null;
            boolean passed = threshold == null || avg >= threshold;

            metricScores.add(new MetricScore(metricName, avg, threshold, passed));
        }

        return new EvalCaseResult(evalCase.evalId(), metricScores);
    }
}
