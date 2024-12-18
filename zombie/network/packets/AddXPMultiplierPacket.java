package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;

@PacketSetting(
   ordering = 0,
   priority = 2,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 2
)
public class AddXPMultiplierPacket implements INetworkPacket {
   public AddXPMultiplierPacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }
}
