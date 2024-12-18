package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3
)
public class BodyDamageUpdatePacket implements INetworkPacket {
   public BodyDamageUpdatePacket() {
   }

   public void setStart(IsoPlayer var1) {
   }

   public void setStop(IsoPlayer var1) {
   }

   public void setUpdate(IsoPlayer var1, IsoPlayer var2, ByteBuffer var3) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void processClient(UdpConnection var1) {
   }

   public void processServer(PacketTypes.PacketType var1, UdpConnection var2) {
   }

   private static enum Type {
      START_UPDATING,
      STOP_UPDATING,
      UPDATE;

      private Type() {
      }
   }
}
