package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.NetTimedAction;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;

@PacketSetting(
   ordering = 1,
   priority = 1,
   reliability = 3,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3
)
public class NetTimedActionPacket extends NetTimedAction implements INetworkPacket {
   public NetTimedActionPacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }
}
