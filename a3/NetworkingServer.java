package a3;

import java.io.IOException;
import tage.networking.IGameConnection.ProtocolType;

public class NetworkingServer 
{
	private GameServerUDP thisUDPServer;
	private GameServerTCP thisTCPServer;

	public NetworkingServer(int serverPort, String protocol) 
	{	try 
		{	if(protocol.toUpperCase().compareTo("TCP") == 0)
			{	thisTCPServer = new GameServerTCP(serverPort);
			}
			else
			{	thisUDPServer = new GameServerUDP(serverPort);
			}
			System.out.println(protocol.toUpperCase() + " server started on port " + serverPort);
			System.out.println("Waiting for clients to connect...");
		} 
		catch (IOException e) 
		{	e.printStackTrace();
		}
	}

	public static void main(String[] args) 
	{	if(args.length > 1)
		{	NetworkingServer app = new NetworkingServer(Integer.parseInt(args[0]), args[1]);
		}
	}

}
