package zombie.network.packets.safehouse;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatSafeHouseSurvivor;
import zombie.network.fields.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 1,
   anticheats = {AntiCheat.SafeHouseSurviving}
)
public class SafehouseClaimPacket extends PlayerID implements INetworkPacket, AntiCheatSafeHouseSurvivor.IAntiCheat {
   public SafehouseClaimPacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public IsoPlayer getSurvivor() {
      return null;
   }
}
