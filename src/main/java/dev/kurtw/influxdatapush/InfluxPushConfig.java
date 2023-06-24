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

    static {
        BUILDER.push("influxkeys");

        serverAddress = BUILDER.comment("The address of the InfluxDB server to push data to")
                .define("serverAddress", "http://192.168.4.4:8086");
        token = BUILDER.comment("Influx token").define("token",
                "");
        org = BUILDER.comment("Influx organization").define("org", "kurtsnetwork");
        bucket = BUILDER.comment("Influx bucket").define("bucket", "minecraft");

        location = BUILDER.comment("Location of the server").define("location", "server");

        BUILDER.pop();
        SPEC = BUILDER.build();

    }
}
