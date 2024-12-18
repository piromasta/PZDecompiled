package zombie.network.packets.service;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatRecipe;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 0,
   priority = 0,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 7,
   anticheats = {AntiCheat.Recipe}
)
public class RecipePacket implements INetworkPacket, AntiCheatRecipe.IAntiCheat {
   public RecipePacket() {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public long getClientChecksum() {
      return 0L;
   }

   public long getServerChecksum() {
      return 0L;
   }
}
