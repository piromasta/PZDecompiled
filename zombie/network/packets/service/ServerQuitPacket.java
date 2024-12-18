package zombie.network.packets.service;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.ConnectionManager;
import zombie.network.GameClient;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 0,
   priority = 0,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 2
)
public class ServerQuitPacket implements INetworkPacket {
   public ServerQuitPacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public void processClient(UdpConnection var1) {
      GameWindow.kickReason = "Server shut down safely. Players and map data saved.";
      GameWindow.bServerDisconnected = true;
      ConnectionManager.log("receive-packet", "server-quit", GameClient.connection);
   }
}
