import io.vertx.core.*;
import io.vertx.core.json.JsonObject;


public final class Clan extends AbstractVerticle {
    private static final String ACTIVE_CLANS = "activeClans";
    private static final String INACTIVE_CLANS = "inactiveClans";

    public static void main(String[] args) {
        Vertx.clusteredVertx(new VertxOptions(), vertx -> vertx.result().deployVerticle(new Clan()));
    }

    @Override
    public void start() {
        vertx.sharedData().getCounter("clanNumber", counter -> {
            if (counter.succeeded()) {
                counter.result().incrementAndGet(id -> clanCreation(id.result()));
            }
        });
    }

    private void clanCreation(long id) {
        final String clanName = "clan" + id;
        JsonObject clan = new JsonObject().put("name", clanName);
        vertx.sharedData().getAsyncMap(INACTIVE_CLANS, map -> map.result().put(clanName, clan, completion ->
                System.out.println(clanName + " created!")));

        vertx.eventBus().<JsonObject>consumer(clanName + ".administrator.join", event -> {
            final JsonObject admin = event.body();
            final String name = admin.getString("name");
            long moderatorsLimit = admin.getLong("moderatorsLimit");
            System.out.println("moderators limit is " + moderatorsLimit);
            long usersLimit = admin.getLong("usersLimit");
            System.out.println("users limit is " + usersLimit);

            vertx.sharedData().getAsyncMap(clanName + ".members", map -> map.result().put(name, admin, completion ->
                    System.out.println("Administrator " + name + " joins the " + clanName)));

            vertx.sharedData().getAsyncMap(INACTIVE_CLANS, map -> map.result().remove(clanName, completion ->
                    System.out.println(clanName + " removed from inactiveClans")));

            clan.put("usersLimit", usersLimit);

            vertx.sharedData().getAsyncMap(ACTIVE_CLANS, map -> map.result().put(clanName, clan, completion ->
                    System.out.println(clanName + " added to activeClans")));

            moderatorsJoin(clanName, moderatorsLimit);
        });
    }

    private void moderatorsJoin(String clanName, long moderatorsLimit) {
        vertx.eventBus().<JsonObject>consumer(clanName + ".moderator.join", event -> {
            vertx.sharedData().getCounter(clanName + ".moderators.count", counter -> counter.result().get(count -> {
                if (count.result() < moderatorsLimit) {
                    JsonObject moderator = event.body();
                    String name = moderator.getString("name");
                    vertx.sharedData().getAsyncMap(clanName + ".members", map -> map.result().put(name, moderator,
                            completion -> System.out.println(name + " joined the " + clanName)));
                    vertx.sharedData().getCounter(clanName + ".moderators.count", inc -> inc.result().incrementAndGet(AsyncResult::succeeded));
                } else {
                    System.out.println("Moderators limit reached!");
                }
            }));
        });
    }
}