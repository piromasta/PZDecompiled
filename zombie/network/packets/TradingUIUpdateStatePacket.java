package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3
)
public class TradingUIUpdateStatePacket implements INetworkPacket {
   public TradingUIUpdateStatePacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }

   static enum State {
      WindowsWasClosed,
      DealWasSealed,
      DealWasUnsealed,
      DealWasFinalized;

      private State() {
      }
   }
}
