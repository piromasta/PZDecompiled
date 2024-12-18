package zombie.network.packets.service;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 0,
   priority = 0,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 4
)
public class AccessDeniedPacket implements INetworkPacket {
   public AccessDeniedPacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }
}
