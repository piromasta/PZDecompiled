package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;

@PacketSetting(
   ordering = 1,
   priority = 1,
   reliability = 3,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 2
)
public class ReplaceInventoryItemInContainerPacket implements INetworkPacket {
   public ReplaceInventoryItemInContainerPacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }
}
