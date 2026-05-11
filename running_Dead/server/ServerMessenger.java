package running_Dead.server;

import java.util.UUID;

interface ServerMessenger {
    void sendTo(UUID targetId, String message);
}
