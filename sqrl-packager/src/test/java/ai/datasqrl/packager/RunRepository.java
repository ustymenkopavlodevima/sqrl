package ai.datasqrl.packager;

import ai.datasqrl.flink.FlinkQueryUseCaseTest;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class RunRepository extends FlinkQueryUseCaseTest {

    @Test
    @Disabled
    public void testQuery(Vertx vertx, VertxTestContext testContext) {
        fullScriptTest(DataSQRL.INSTANCE.getScript(), DataSQRL.INSTANCE.getGraphQL(), vertx, testContext);
    }

}
