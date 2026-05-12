package running_Dead;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import running_Dead.networking.ProtocolClient;
import tage.networking.NetworkDiscovery;

/**
 * Client-side networking coordinator.
 * It resolves auto-discovery, creates ProtocolClient, sends transform/item state, and closes cleanly on exit.
 * Connected to: Owned by MyGame; creates ProtocolClient and is called by MyGameInitializer/MyGameUpdater.
 */
public class MyGameNetworkingSystem {
    private final MyGame game;

    public MyGameNetworkingSystem(MyGame game) {
        this.game = game;
    }

    public void setupNetworkingIfNeeded() {
        if (game.state.isMultiplayer) setupNetworking();
    }

    public void installShutdownHandlers() {
        java.lang.Runtime.getRuntime().addShutdownHook(new Thread(this::sendByeAndClose));
        try {
            javax.swing.JFrame frame = (javax.swing.JFrame) javax.swing.SwingUtilities.getWindowAncestor(MyGame.getEngine().getRenderSystem().getGLCanvas());
            if (frame != null) {
                frame.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override public void windowClosing(java.awt.event.WindowEvent e) { sendByeAndClose(); }
                });
            }
        } catch (Exception e) {
            System.out.println("unable to attach window close handler");
        }
    }

    public void processNetworking(float elapsTime) {
        if (game.state.protClient != null) game.state.protClient.processPackets();
    }

    public void sendPlayerTransform() {
        if (game.state.protClient != null && game.state.isClientConnected) {
            // Movement packets also carry held item and flashlight direction so other clients see the same equipment.
            org.joml.Vector3f flashlightDirection = game.itemSystem.getFlashlightBeamDirectionForNetwork();
            game.state.protClient.sendMoveMessage(game.assets.avatar.getWorldLocation(), game.state.playerYaw,
                    game.state.equippedItem, game.state.flashlightOn, flashlightDirection);
        }
    }

    public void setIsConnected(boolean value) { game.state.isClientConnected = value; }

    private void setupNetworking() {
        game.state.isClientConnected = false;
        try {
            InetAddress remoteAddress = resolveServerAddress();
            printMultiplayerStartupHints(remoteAddress);
            game.state.protClient = new ProtocolClient(remoteAddress, game.state.serverPort, game.state.serverProtocol, game);
        } catch (SocketTimeoutException e) {
            System.out.println("auto-discovery failed --> no NetworkingServer replied on UDP port " + NetworkDiscovery.DEFAULT_DISCOVERY_PORT);
            System.out.println("auto-discovery failed --> make sure the server is running and that your network allows local device discovery");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (game.state.protClient == null) System.out.println("missing protocol host");
        else {
            System.out.println("sending join message to protocol host");
            game.state.protClient.sendJoinMessage();
        }
    }

    private InetAddress resolveServerAddress() throws IOException {
        if (!NetworkDiscovery.usesAutoDiscovery(game.state.serverAddress)) return InetAddress.getByName(game.state.serverAddress);
        System.out.println("auto-discovery --> searching for a NetworkingServer on UDP port " + NetworkDiscovery.DEFAULT_DISCOVERY_PORT);
        NetworkDiscovery.DiscoveredServer discoveredServer = NetworkDiscovery.discoverServer(NetworkDiscovery.DEFAULT_DISCOVERY_PORT, NetworkDiscovery.DEFAULT_DISCOVERY_TIMEOUT_MS);
        game.state.serverAddress = discoveredServer.getAddress().getHostAddress();
        game.state.serverPort = discoveredServer.getGamePort();
        game.state.serverProtocol = discoveredServer.getProtocolType();
        System.out.println("auto-discovery --> found server at " + game.state.serverAddress + ":" + game.state.serverPort + " using " + game.state.serverProtocol);
        return discoveredServer.getAddress();
    }

    private void printMultiplayerStartupHints(InetAddress remoteAddress) {
        System.out.println("multiplayer target --> " + game.state.serverAddress + " (" + remoteAddress.getHostAddress() + "):" + game.state.serverPort + " using " + game.state.serverProtocol);
        System.out.println("multiplayer note --> launch two separate MyGame clients for two players");
        if (remoteAddress.isAnyLocalAddress() || remoteAddress.isLoopbackAddress()) {
            System.out.println("multiplayer note --> " + game.state.serverAddress + " only reaches this same computer");
            System.out.println("multiplayer note --> remote machines must use the server computer's LAN IPv4 address instead of localhost");
        }
    }

    private void sendByeAndClose() {
        if (game.state.byeMessageSent) return;
        game.state.byeMessageSent = true;
        game.soundSystem.stopAll();
        if (game.state.protClient != null && game.state.isClientConnected) {
            System.out.println("sending bye message to protocol host");
            game.state.protClient.sendByeMessage();
        }
    }
}
