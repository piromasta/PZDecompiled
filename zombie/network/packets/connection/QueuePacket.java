package zombie.network.packets.connection;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 0,
   priority = 0,
   reliability = 3,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 5
)
public class QueuePacket implements INetworkPacket {
   public QueuePacket() {
   }

   public void setConnectionImmediate() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public static enum MessageType {
      None,
      ConnectionImmediate,
      PlaceInQueue;

      private MessageType() {
      }
   }
}
