package zombie.network.packets.actions;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 2
)
public class HelicopterPacket implements INetworkPacket {
   public HelicopterPacket() {
   }

   public void set(Object var1, Object var2, Object var3) {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }
}
