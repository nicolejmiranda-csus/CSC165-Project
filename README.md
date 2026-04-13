# CSC165 Project — Running the Server and Client
## Compile

From the project root:
```bat

compile.bat

```

Or manually:
```bat

javac -Xlint:unchecked a3/*.java

```

---
## Running the Server

  The server must be started **before** any clients connect.
```bat

java a3.NetworkingServer <port> <protocol>

```

**Examples:**
```bat

java a3.NetworkingServer 6969 UDP

java a3.NetworkingServer 6969 TCP

```

---
## Running the Client

```bat

java --add-exports java.base/java.lang=ALL-UNNAMED ^

     --add-exports java.desktop/sun.awt=ALL-UNNAMED ^

     --add-exports java.desktop/sun.java2d=ALL-UNNAMED ^

     -Dsun.java2d.d3d=false -Dsun.java2d.uiScale=1 ^

     a3.MyGame <serverAddress> <port> <protocol> <avatarName>

```


**Examples:**
```bat

rem Play offline (no server needed)

java ... a3.MyGame null 6969 UDP playerModel1

  

rem Connect to a local server with the default avatar

java ... a3.MyGame localhost 6969 UDP playerModel1

  

rem Connect to a remote server using the second avatar model

java ... a3.MyGame 192.168.1.10 6969 UDP playerModel2

  

rem Connect using TCP

java ... a3.MyGame localhost 6969 TCP playerModel1

```

  
Or use the provided batch files

(offline, no args):
```bat

run.bat 

```

local multiplayer, playerModel1
```bat

runLocalPlayer1.bat

```

local multiplayer, playerModel2
```bat

runLocalPlayer2.bat

```

---
## Quick Start (Local Multiplayer)

1. Open a terminal and start the server:
   ```bat

   java a3.NetworkingServer 6969 UDP

   ```

2. Open one or more additional terminals and start a client in each, using `localhost` as the address:

   ```bat

runLocalPlayer1.bat | runLocalPlayer2.bat

   ```

3. Each client will appear as a ghost avatar in all other connected clients' games.