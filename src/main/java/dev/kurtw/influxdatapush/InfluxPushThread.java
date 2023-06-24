package dev.kurtw.influxdatapush;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.Builder;
import java.security.KeyStore.LoadStoreParameter;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

class InfluxPushThread extends Thread {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean configured = false;
    private static boolean needs_reconfigure = false;

    static InfluxPushThread instance = null;

    BlockingQueue<Object> messageQueue = new java.util.concurrent.LinkedBlockingQueue<Object>();

    boolean running = true;
    static String _location = "singleplayer-dev";

    private static String serverAddress;
    private static String token;
    private static String org;
    private static String bucket;

    private InfluxPushThread() {

    }

    static public void setLocation(String l) {
        _location = l;
    }

    public static void configure(String serverAddress, String token, String org, String bucket) {
        InfluxPushThread.serverAddress = serverAddress;
        InfluxPushThread.token = token;
        InfluxPushThread.org = org;
        InfluxPushThread.bucket = bucket;

        LOGGER.info("Configuring InfluxPushThread with serverAddress: " + serverAddress + " token: " + token + " org: "
                + org + " bucket: " + bucket);

        if (instance == null) {
            instance = new InfluxPushThread();
        } else {
            // notify that we are reconfiguring
            needs_reconfigure = true;
        }

        configured = true;
    }

    public Long getQueueSize() {
        if (!configured) {
            return 0L;
        }
        return (long) messageQueue.size();
    }

    public static InfluxPushThread getInstance() {
        if (instance == null) {
            instance = new InfluxPushThread();
        }
        return instance;
    }

    public static void pushMessage(Object message) {
        if (!configured) {
            LOGGER.error("InfluxPushThread not configured");
            return;
        }
        try {
            instance.messageQueue.put(message);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while waiting for message queue", e);
        }
    }

    public void run() {
        if (!configured) {
            LOGGER.error("InfluxPushThread not configured");
            return;
        }
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder requestBuilder = null;
        while (running) {
            try {
                if (needs_reconfigure || requestBuilder == null) {
                    LOGGER.info("Reconfiguring InfluxPushThread with serverAddress: " + serverAddress + " token: "
                            + token + " org: " + org + " bucket: " + bucket);
                    requestBuilder = makeBuilder();
                    needs_reconfigure = false;
                }
                Object message = messageQueue.take();
                String messageString = message.toString();
                HttpRequest request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(messageString))
                        .build();
                HttpResponse<String> result = client.send(request, HttpResponse.BodyHandlers.ofString());

                int statusCode = result.statusCode();
                if (statusCode != 204) {
                    LOGGER.error("Influx push failed with status code: " + statusCode);
                    LOGGER.error("Influx push failed with message: " + result.body());
                }

            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while waiting for message", e);
            } catch (IOException e) {
                LOGGER.error("Error sending message", e);
            } catch (Exception e) {
                LOGGER.error("Unknown error", e);
            } finally {
            }
        }
    }

    private Builder makeBuilder() throws URISyntaxException {
        return HttpRequest.newBuilder()
                .uri(new URI(
                        serverAddress + "/api/v2/write?org=" + org + "&bucket=" + bucket + "&precision=ms"))
                .header("Authorization", "Token " + token)
                .header("Content-Type", "text/plain; charset=utf-8")
                .header("Keep-Alive", "timeout=60, max=1000");
    }

    public static class PlayerCount {

        String location;

        Integer value;

        Instant time;

        public PlayerCount() {
            this.time = Instant.now();
            this.location = _location;
        }

        public String toString() {
            // influx line protocol
            return String.format("player_count,location=%s value=%d %d", location, value, time.toEpochMilli());
        }
    }

    public static class Performance {
        String location;

        Double tps;

        Double mspt;

        Integer ticks;

        Integer loadedChunks;

        Long influxQueueSize;

        Instant time;

        public Performance() {
            this.location = _location;
            this.time = Instant.now();
            influxQueueSize = (long) instance.getQueueSize();
        }

        public String toString() {
            // influx line protocol
            return String.format(
                    "performance,location=%s tps=%f,ticks=%d,influxQueueSize=%d,loadedChunks=%d,mspt=%f %d",
                    location, tps, ticks, influxQueueSize, loadedChunks, mspt, time.toEpochMilli());
        }
    }

}
