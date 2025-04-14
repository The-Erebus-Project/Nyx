package io.github.vizanarkonin.nyx.Handlers;

import com.corundumstudio.socketio.SocketIOClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Some pages might have most (if not all) of their controls done via WebSockets.
 * To make sure WS procedures aren't unauthorized - we use this delegate type for all auth-related actions.
 */
@Component
public class WebSocketSessionController {

    @Autowired
    private SessionRegistry sessionRegistry;

    /**
     * To make sure user doesn't get session expiration - we use this component to refresh the session token 
     * when user performs some actions (typically - any non-ping activity) 
     * TODO: Right now it doesn't seem to work properly - session gets expired even if this method is called. Find out why and fix it.
     * @param client - Client instance to validate
     */
    public void refreshSession(SocketIOClient client) {
        String sessionId = client.getHandshakeData().getSingleUrlParam("sessionId"); // JSESSIONID
        if (sessionId == null)
            return;

        sessionRegistry.refreshLastRequest(sessionId);
    }

    /**
     * Validates if session with given ID is still alive. Not really tied to WS, but for now it's the only consumer of it other than HTTP server
     * @param sessionId - JSESSIONID value (session ID token)
     * @return          - true for authorized, false for unauthorized
     */
    public boolean isSessionAuthenticated(String sessionId) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
            for (SessionInformation session : sessions) {
                if (session.getSessionId().equals(sessionId) && !session.isExpired()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Validates if given client is still authenticated.
     * Expects the client to have JSESSIONID ("sessionId") token passed as URL parameter.
     * @param client    - SocketIOClient instance to validate
     * @return          - true for authorized, false for unauthorized
     */
    public boolean isClientAuthenticated(SocketIOClient client) {
        String sessionId = client.getHandshakeData().getSingleUrlParam("sessionId");

        return isSessionAuthenticated(sessionId);
    }
}