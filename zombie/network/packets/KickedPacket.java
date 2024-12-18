package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;

@PacketSetting(
   ordering = 0,
   priority = 0,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 6
)
public class KickedPacket implements INetworkPacket {
   @JSONField
   public String description;
   @JSONField
   public String reason;

   public KickedPacket() {
   }

   public void setData(Object... var1) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void processClient(UdpConnection var1) {
   }

   public void processClientLoading(UdpConnection var1) {
   }
}
