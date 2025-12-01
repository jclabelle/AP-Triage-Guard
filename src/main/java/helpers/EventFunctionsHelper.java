package helpers;

import com.google.adk.events.Event;
import com.google.genai.types.FunctionCall;
import com.google.common.collect.ImmutableList;
import com.google.genai.types.FunctionResponse;

import java.util.Map;

public class EventFunctionsHelper {

    public static void ViewCalls(Event event){
        ImmutableList<FunctionCall> calls = event.functionCalls();
        if (!calls.isEmpty()) {
            for (FunctionCall call : calls) {
                String toolName = call.name().get();
                // args is Optional<Map<String, Object>>
                Map<String, Object> arguments = call.args().get();
                System.out.println("  Tool: " + toolName + ", Args: " + arguments);
                // Application might dispatch execution based on this
            }
        }
    }

    public static void ViewResponses(Event event){
        ImmutableList<FunctionResponse> responses = event.functionResponses(); // from Event.java
        if (!responses.isEmpty()) {
            for (FunctionResponse response : responses) {
                String toolName = response.name().get();
                Map<String, Object> result= response.response().get(); // Check before getting the response
                System.out.println("  Tool Result: " + toolName + " -> " + result);
            }
        }
    }
}
