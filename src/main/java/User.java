import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

public final class User extends AbstractVerticle {

    public static void main(String[] args) {
        Vertx.clusteredVertx(new VertxOptions(), vertx -> vertx.result().deployVerticle(new User()));
    }

    @Override
    public void start() {
        vertx.sharedData().getCounter("users", counter -> {
            if (counter.succeeded()) {
                counter.result().incrementAndGet(id -> enterClan(id.result()));
            }
        });
    }

    private void enterClan(long id) {
        final String name = "user #" + id;
        final JsonObject message = new JsonObject().put("name", name);
        System.out.println(name + " wants to join the clan");
        vertx.eventBus().send("users.join", message);
    }
}
