import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public final class User extends AbstractVerticle {
    private String clanName;
    private String name;

    public static void main(String[] args) {
        Vertx.clusteredVertx(new VertxOptions(), vertx -> vertx.result().deployVerticle(new User()));
    }

    @Override
    public void start() {
        vertx.sharedData().getCounter("users", counter -> {
            if (counter.succeeded()) {
                counter.result().incrementAndGet(id -> getRandomClan(id.result()));
            }
        });
    }

    private void getRandomClan(long id) {
        vertx.sharedData().getAsyncMap("activeClans", map -> {
            map.result().entries(completion -> {
                if (completion.result().size() > 0) {
                    clanName =(String) new ArrayList<>(completion.result().keySet()).get(ThreadLocalRandom.current()
                            .nextInt(completion.result().size()));
                    enterClan(id);
                } else {
                    System.out.println("There is no active clans!");
                }
            });
        });
    }

    private void enterClan(long id) {
        name = "user #" + id;
        final JsonObject message = new JsonObject().put("name", name);
        System.out.println(name + " wants to join the clan");
        vertx.eventBus().send(clanName + ".user.join", message);
        vertx.eventBus().consumer(name, event -> {
            System.out.println("Got a message!");
        });
        sendMessages();
    }

    private void sendMessages() {
        vertx.setPeriodic(Duration.ofSeconds(20).toMillis(), event -> {
            vertx.sharedData().getAsyncMap(clanName + ".members", map -> {
                map.result().entries(completion -> {
                    if (completion.result().size() > 0) {
                        String receiverName = (String) new ArrayList<>(completion.result().keySet()).get(ThreadLocalRandom.current()
                                .nextInt(completion.result().size()));
                        vertx.eventBus().send(receiverName, "Hello from " + name);
                    }
                });
            });
        });
    }

    @Override
    public void stop() {
        vertx.sharedData().getAsyncMap(clanName + ".members", map -> map.result().remove(name, completion ->
                System.out.println(name + " removed from clan members")));
        vertx.sharedData().getCounter(clanName + ".users.count", counter -> counter.result().decrementAndGet(
                count -> System.out.println(name + " disconnected")));
    }
}
