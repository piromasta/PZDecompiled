package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatXP;
import zombie.network.anticheats.AntiCheatXPPlayer;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 3,
   anticheats = {AntiCheat.XPPlayer, AntiCheat.XP}
)
public class AddXpPacket implements INetworkPacket, AntiCheatXP.IAntiCheat, AntiCheatXPPlayer.IAntiCheat {
   public AddXpPacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public IsoPlayer getPlayer() {
      return null;
   }

   public float getAmount() {
      return 0.0F;
   }
}
