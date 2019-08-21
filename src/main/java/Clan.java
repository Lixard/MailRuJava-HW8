import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;


public final class Clan extends AbstractVerticle {

    private String clanName;

    public static void main(String[] args) {
        Vertx.clusteredVertx(new VertxOptions(), vertx -> vertx.result().deployVerticle(new Clan()));
    }

    @Override
    public void start() {
        vertx.sharedData().getCounter("clans", counter -> {
            if (counter.succeeded()) {
                counter.result().incrementAndGet(id -> setClanName(id.result()));
            }
        });

        final LocalMap<String, JsonObject> ClanUsers = vertx.sharedData().getLocalMap(clanName + "Users" );
        final LocalMap<String, JsonObject> clanModerators = vertx.sharedData().getLocalMap(clanName + "Moderators");
        final LocalMap<String, JsonObject> clanAdmin = vertx.sharedData().getLocalMap(clanName + "Admin");

        System.out.println(clanName + " created!");

        vertx.eventBus().<JsonObject>consumer("administrator.join", event -> {
            final JsonObject admin = event.body();
            final String name = admin.getString("name");
            if (clanAdmin.isEmpty()) {
                clanAdmin.putIfAbsent(name, admin);
                System.out.println("Administrator " + name + "joins the " + clanName);
            }
        });
    }

    private void setClanName(long id) {
        clanName = "clan#" + id;
    }
}