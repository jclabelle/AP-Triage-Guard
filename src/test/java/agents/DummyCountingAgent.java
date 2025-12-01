package agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.InvocationContext;
import com.google.adk.events.Event;
import io.reactivex.rxjava3.core.Flowable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test-only agent that increments a static counter each time it is run.
 */
public class DummyCountingAgent extends BaseAgent {

    private static final AtomicInteger invocationCount = new AtomicInteger(0);

    public DummyCountingAgent() {
        super(
                "DummyCountingAgent",
                "Test-only agent that counts invocations.",
                List.of(),
                null,
                null
        );
    }

    public static void resetCount() {
        invocationCount.set(0);
    }

    public static int getInvocationCount() {
        return invocationCount.get();
    }

    @Override
    protected Flowable<Event> runAsyncImpl(InvocationContext ctx) {
        invocationCount.incrementAndGet();

        Event event = Event.builder()
                .author("DummyCountingAgent")
                .build();

        return Flowable.just(event);
    }

    @Override
    protected Flowable<Event> runLiveImpl(InvocationContext ctx) {
        // For this test agent, live and async behave the same.
        return runAsyncImpl(ctx);
    }
}

