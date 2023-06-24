package dev.kurtw.influxdatapush;

import net.minecraftforge.common.ForgeConfigSpec;

class InfluxPushConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<String> serverAddress;
    public static final ForgeConfigSpec.ConfigValue<String> token;
    public static final ForgeConfigSpec.ConfigValue<String> org;
    public static final ForgeConfigSpec.ConfigValue<String> bucket;
    public static final ForgeConfigSpec.ConfigValue<String> location;
    public static final ForgeConfigSpec.ConfigValue<Integer> update_ticks;

    static {
        BUILDER.push("influxkeys");

        serverAddress = BUILDER.comment("The address of the InfluxDB server to push data to")
                .define("serverAddress", "http://INFLUX:port");
        token = BUILDER.comment("Influx token").define("token",
                "");
        org = BUILDER.comment("Influx organization").define("org", "kurtsnetwork");
        bucket = BUILDER.comment("Influx bucket").define("bucket", "minecraft");

        location = BUILDER.comment("Location of the server").define("location", "server");

        BUILDER.comment("Don't pick something too big, or you'll experience FUN");
        update_ticks = BUILDER.comment("How often to update the data in ticks").define("update_ticks", 100);

        BUILDER.pop();
        SPEC = BUILDER.build();

    }
}
