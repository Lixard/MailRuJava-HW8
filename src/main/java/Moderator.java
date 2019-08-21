import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

public final class Moderator extends AbstractVerticle {

    public static void main(String[] args) {
        Vertx.clusteredVertx(new VertxOptions(), vertx -> vertx.result().deployVerticle(new Moderator()));
    }

    @Override
    public void start() {
        vertx.sharedData().getCounter("moderators", counter -> {
            if (counter.succeeded()) {
                counter.result().incrementAndGet(id -> enterClan(id.result()));
            }
        });
    }

    private void enterClan(long id) {
        final String name = "moderator #" + id;
        final JsonObject message = new JsonObject().put("name", name);
        System.out.println(name + " wants to join the clan");
        vertx.eventBus().send("moderators.join", message);
    }
}
