package constants;

/**
 * Identifies the agent whose output is displayed to the user as the final response.
 * Used by ChatUI to filter events and display only the final formatted reply.
 */
public class FinalResponseAgentName {

    /**
     * The name of the agent that produces the final user-facing response.
     * Must match the 'name' field in the corresponding agent YAML configuration.
     */
    public static final String VALUE = "ap_reply_formatter";

}
