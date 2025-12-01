package orchestrator;

import com.google.adk.agents.ConfigAgentUtils;
import config.OpenTelemetryBootstrap;
import config.RegistrationService;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static void main() throws ConfigAgentUtils.ConfigurationException {

        // loggingExporter -> True: Use the local logging exporter. False: Use the otlp exporter.
        var loggingExporter = false;
        OpenTelemetryBootstrap.init(loggingExporter);

        // Register wrappers and tools to enable YAML configuration
        var registrationService = new RegistrationService();
        registrationService.registerWorkflowWrappers();
        registrationService.registerFunctionTools();

        // Run the app
        var appOrchestrator = new AppOrchestrator();
        appOrchestrator.runApp();
        }
    }

