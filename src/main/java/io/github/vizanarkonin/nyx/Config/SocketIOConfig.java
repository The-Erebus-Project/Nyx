package io.github.vizanarkonin.nyx.Config;

import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.Transport;

import io.github.vizanarkonin.nyx.Handlers.WebSocketSessionController;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Component
public class SocketIOConfig {
    private final Logger log = LogManager.getLogger(this.getClass().getName());
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    // Key is a sessionId token, value is another map - key is socket ID, value is Socket-IO client instance
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String,SocketIOClient>> clients = new ConcurrentHashMap<>();

    @Value("${socket.host}")
    private String socketHost;
    @Value("${socket.port}")
    private int socketPort;

    @Autowired
    private WebSocketSessionController webSocketSessionController;

    private SocketIOServer server;

    @Bean
    public SocketIOServer socketIOServer() {
        Configuration config = new Configuration();

        config.setHostname(socketHost);
        config.setPort(socketPort);
        config.setTransports(Transport.WEBSOCKET);

        // We reduce the ping interval to 10 seconds for clearer communication flow (less annoying to debug)
        config.setPingInterval(10_000);
        config.setPingTimeout(20_000);

        // Authorization listener. We expect to receive a JSESSIONID token as a query parameter.
        // TODO: We might need to change it if we move to a different auth model, but for now this will do
        config.setAuthorizationListener(data -> {
            String sessionId = data.getSingleUrlParam("sessionId");
            if (sessionId == null || sessionId.isEmpty()) {
                return new AuthorizationResult(false);
            }

            if (webSocketSessionController.isSessionAuthenticated(sessionId)) {
                return new AuthorizationResult(true);
            }
            
            return new AuthorizationResult(false);
        });

        server = new SocketIOServer(config);
        server.start();

        server.addConnectListener(client -> {
            String sessionId = client.getHandshakeData().getSingleUrlParam("sessionId");
            String socketId = client.getSessionId().toString();
            log.debug("Client connected: {}", socketId);

            if (!clients.containsKey(sessionId))
                clients.put(sessionId, new ConcurrentHashMap<>());
            log.debug("Storing client {} for sessionId {}", socketId, sessionId);
            clients.get(sessionId).put(socketId, client);
        });

        server.addDisconnectListener(client -> {
            String sessionId = client.getHandshakeData().getSingleUrlParam("sessionId");
            String socketId = client.getSessionId().toString();
            log.debug("Client disconnected: {}", socketId);
            
            log.debug("Removing the client {} from the clients storage for sessionId {}", socketId, sessionId);
            clients.get(sessionId).remove(socketId);
        });

        // To make sure we don't get any 'dangling' socket connections after JSESSIONID expiration - we run a periodic task
        // that validates all currently registered sessionIs's, and if some of them are expired - disconnects them all
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Iterator<Entry<String, ConcurrentHashMap<String, SocketIOClient>>> entries = clients.entrySet().iterator();
                log.debug("Session state validation tick fired. Amount of registered JSESSIONID tokens: {}", clients.size());
                while (entries.hasNext()) {
                    Map.Entry<String, ConcurrentHashMap<String,SocketIOClient>> entry = entries.next();
                    String sessionId = entry.getKey();
                    if (!webSocketSessionController.isSessionAuthenticated(sessionId)) {
                        log.debug("Detected expired session with ID {}. Disconnecting the sockets connected using this token", sessionId);
                        Iterator<Entry<String, SocketIOClient>> sockets = entry.getValue().entrySet().iterator();
                        while(sockets.hasNext()) {
                            Map.Entry<String, SocketIOClient> socketEntry = sockets.next();
                            socketEntry.getValue().sendEvent("disconnect", "Session Expired");
                        }

                        entries.remove();
                    } else {
                        log.debug("Session {} is alive. Skipping", sessionId);
                    }
                }
            } catch (Exception e) {
                log.error(e);
                log.error(ExceptionUtils.getStackTrace(e));
            }
            
        }, 1, 1, TimeUnit.MINUTES);

        return server;
    }

    @PreDestroy
    public void stopSocketServer() {
        this.server.stop();
    }
}