import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public final class Administrator extends AbstractVerticle {
    private static final String ACTIVE_CLANS = "activeClans";
    private static final String INACTIVE_CLANS = "inactiveClans";


    private final int moderatorsLimit = ThreadLocalRandom.current().nextInt(1, 3);
    private final int usersLimit = ThreadLocalRandom.current().nextInt(1, 20);

    private String clanName;
    private String name;

    public static void main(String[] args) {
        Vertx.clusteredVertx(new VertxOptions(), vertx -> vertx.result().deployVerticle(new Administrator()));
    }

    @Override
    public void start() {
        vertx.sharedData().getCounter("administrators", counter -> {
            if (counter.succeeded()) {
                counter.result().incrementAndGet(id -> getRandomClan(id.result()));
            }
        });
    }

    private void getRandomClan(long id) {
        vertx.sharedData().getAsyncMap("inactiveClans", map -> {
            map.result().entries(completion -> {
                if (completion.result().size() > 0) {
                    clanName = (String) new ArrayList<>(completion.result().keySet()).get(ThreadLocalRandom.current()
                            .nextInt(completion.result().size()));
                    enterClan(id);
                } else {
                    System.out.println("There is no inactive clans!");
                }
            });
        });
    }

    private void enterClan(long id) {
        name = "administrator #" + id;
        final JsonObject message = new JsonObject().put("name", name)
                .put("moderatorsLimit", moderatorsLimit).put("usersLimit", usersLimit);
        System.out.println(name + "  joins to the " + clanName);
        vertx.eventBus().send(clanName + ".administrator.join", message);

        vertx.eventBus().consumer(name, event -> {
            System.out.println("Got a message!");
        });
    }

//    private void checkLimit() {
//        vertx.eventBus().consumer(clanName + ".check", event -> {
//
//        });
//    }

    @Override
    public void stop() {
        System.out.println("Admin is offline!");
        vertx.sharedData().getAsyncMap(ACTIVE_CLANS, map -> map.result().get(clanName, res -> {
            JsonObject clan = (JsonObject) res.result();
            vertx.sharedData().getAsyncMap(ACTIVE_CLANS, asyncMap -> asyncMap.result().remove(clanName, completion ->
                    System.out.println(clanName + " removed from activeClans")));
            vertx.sharedData().getAsyncMap(INACTIVE_CLANS, asyncMap -> asyncMap.result().put(clanName, clan, completion ->
                    System.out.println(clanName + " added to inactiveClans")));
        }));
        vertx.sharedData().getAsyncMap(clanName + ".members", map -> map.result().remove(name, completion ->
                System.out.println("Admin removed from clan members")));
    }
}
