import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.ThreadLocalRandom;

public final class Administrator extends AbstractVerticle {

    private final int moderatorsCount = ThreadLocalRandom.current().nextInt(1, 3);
    private final int usersCount = ThreadLocalRandom.current().nextInt(1, 20);

    public static void main(String[] args) {
        Vertx.clusteredVertx(new VertxOptions(), vertx -> vertx.result().deployVerticle(new Administrator()));
    }

    @Override
    public void start() {
        vertx.sharedData().getCounter("administrators", counter -> {
            if (counter.succeeded()) {
                counter.result().incrementAndGet(id -> enterClan(id.result()));
            }
        });
    }

    private void enterClan(long id) {
        final String name = "administrator #" + id;
        final JsonObject message = new JsonObject().put("name", name);
        System.out.println(name + " wants to join the clan");
        vertx.eventBus().send("administrator.join", message);
    }
}
