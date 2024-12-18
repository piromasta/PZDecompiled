package zombie.network.packets.service;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 0,
   requiredCapability = Capability.ConnectWithDebug,
   handlingType = 3
)
public class ServerDebugInfo implements INetworkPacket {
   public static final byte PKT_LOADED = 1;
   public static final byte PKT_REPOP = 2;

   public ServerDebugInfo() {
   }

   public void setRequestServerInfo() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }
}
