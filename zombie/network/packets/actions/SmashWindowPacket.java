package zombie.network.packets.actions;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoObject;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.NetObject;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3
)
public class SmashWindowPacket implements INetworkPacket {
   @JSONField
   NetObject window = new NetObject();
   @JSONField
   Action action;

   public SmashWindowPacket() {
   }

   public void setSmashWindow(IsoObject var1) {
   }

   public void setRemoveBrokenGlass(IsoObject var1) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void processClient(UdpConnection var1) {
   }

   public void processServer(PacketTypes.PacketType var1, UdpConnection var2) {
   }

   static enum Action {
      smashWindow,
      removeBrokenGlass;

      private Action() {
      }
   }
}
