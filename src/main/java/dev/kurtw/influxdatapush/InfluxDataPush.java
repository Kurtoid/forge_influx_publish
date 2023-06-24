package dev.kurtw.influxdatapush;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.kurtw.influxdatapush.InfluxPushThread.Performance;
import dev.kurtw.influxdatapush.InfluxPushThread.PlayerCount;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("influxdatapush")
public class InfluxDataPush
{
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // keep a reference to the server
    private MinecraftServer server = null;

    public InfluxDataPush()
    {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(Type.SERVER, InfluxPushConfig.SPEC, "influxdatapush.toml");
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
        LOGGER.info("Setup called for InfluxDataPush");
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("Server starting for InfluxDataPush");
        server = event.getServer();
        InfluxPushThread.configure(InfluxPushConfig.serverAddress.get(),
                InfluxPushConfig.token.get(), InfluxPushConfig.org.get(), InfluxPushConfig.bucket.get());
        InfluxPushThread.setLocation(InfluxPushConfig.location.get());
        InfluxPushThread.getInstance().start();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event)
    {
        // Do something when the server stops
        LOGGER.info("Server stopping for InfluxDataPush");
        InfluxPushThread.getInstance().running = false;
    }

    int lastPublished = 0;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event)
    {
        // Do something when the server ticks
        // LOGGER.info("Server tick for InfluxDataPush");

        if (event.phase != Phase.END) {
            return;
        }

        if (server == null) {
            return;
        }

        if (server.getTickCount() - lastPublished < InfluxPushConfig.update_ticks.get()) {
            return;
        }

        // server.tickTimes stores the last 100 tick times in milliseconds
        Performance p = new Performance();
        p.ticks = server.getTickCount();
        double tickTimes = 0;
        for (int i = 0; i < server.tickTimes.length; i++) {
            tickTimes += server.tickTimes[i];
        }
        tickTimes /= server.tickTimes.length;
        // convert to milliseconds
        tickTimes /= 1000000;
        p.mspt = tickTimes;
        p.tps = Math.min(1000 / tickTimes, 20);
        int loadedChunks = 0;
        for (ServerLevel level : server.getAllLevels()) {
            loadedChunks += level.getChunkSource().getLoadedChunksCount();
        }
        p.loadedChunks = loadedChunks;

        InfluxPushThread.pushMessage(p);

        PlayerCount pc = new PlayerCount();
        pc.value = server.getPlayerCount();
        InfluxPushThread.pushMessage(pc);

        lastPublished = server.getTickCount();
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        // do @SubscribeEvent for mods here
        @SubscribeEvent
        public static void onConfigReloaded(ModConfigEvent.Reloading event) {
            LOGGER.info("Config reloaded for InfluxDataPush");
            if (event.getConfig().getType() == Type.SERVER) {
                InfluxPushThread.configure(InfluxPushConfig.serverAddress.get(),
                        InfluxPushConfig.token.get(), InfluxPushConfig.org.get(), InfluxPushConfig.bucket.get());
                InfluxPushThread.setLocation(InfluxPushConfig.location.get());
            }
        }

        @SubscribeEvent 
        public static void onConfigChanged(ModConfigEvent event) {
            LOGGER.info("Config changed for InfluxDataPush");
        }
    }
    
}
