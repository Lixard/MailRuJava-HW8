import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public final class Moderator extends AbstractVerticle {

    private static final String ACTIVE_CLANS = "activeClans";
    private static final String USERS_LIMIT = "usersLimit";

    private String clanName;
    private String name;

    public static void main(String[] args) {
        Vertx.clusteredVertx(new VertxOptions(), vertx -> vertx.result().deployVerticle(new Moderator()));
    }

    @Override
    public void start() {
        vertx.sharedData().getCounter("moderators", counter -> {
            if (counter.succeeded()) {
                counter.result().incrementAndGet(id -> getRandomClan(id.result()));
            }
        });
    }

    private void getRandomClan(long id) {
        vertx.sharedData().getAsyncMap(ACTIVE_CLANS, map -> {
            map.result().entries(completion -> {
                if (completion.result().size() > 0) {
                    clanName = (String) new ArrayList<>(completion.result().keySet()).get(ThreadLocalRandom.current()
                            .nextInt(completion.result().size()));
                    enterClan(id);
                    JsonObject clan = (JsonObject) completion.result().get(clanName);
                    usersJoin(clan.getLong(USERS_LIMIT));
                } else {
                    System.out.println("There is no active clans!");
                }
            });
        });
    }

    private void enterClan(long id) {
        name = "moderator #" + id;
        final JsonObject message = new JsonObject().put("name", name);

        System.out.println(name + " wants to join the " + clanName);
        vertx.eventBus().send(clanName + ".moderator.join", message);

        vertx.eventBus().consumer(name, event -> {
            System.out.println("Got a message!");
        });
    }

    private void usersJoin(long usersLimit) {
        vertx.eventBus().<JsonObject>consumer(clanName + ".user.join", event -> {
            vertx.sharedData().getCounter(clanName + ".users.count", counter -> counter.result().get(count -> {
                if (count.result() < usersLimit) {
                    JsonObject user = event.body();
                    String userName = user.getString("name");
                    vertx.sharedData().getAsyncMap(clanName + ".members", map -> map.result().put(userName, user, completion ->
                            System.out.println(userName + " added to " + clanName)));
                    vertx.sharedData().getCounter(clanName + ".users.count", inc -> inc.result().incrementAndGet(AsyncResult::succeeded));
                } else {
                    System.out.println("Users limit reached!");
                }
            }));
        });
    }

    @Override
    public void stop() {
        vertx.sharedData().getAsyncMap(clanName + ".members", map -> map.result().remove(name, completion ->
                System.out.println(name + " removed from clan members")));
        vertx.sharedData().getCounter(clanName + ".moderators.count", counter -> counter.result().decrementAndGet(
                count -> System.out.println(name + " disconnected")));
    }
}
