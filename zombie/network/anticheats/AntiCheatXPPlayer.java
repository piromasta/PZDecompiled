package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatXPPlayer extends AbstractAntiCheat {
   public AntiCheatXPPlayer() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      if (var1.role.haveCapability(Capability.AddXP)) {
         return var3;
      } else {
         return !var1.havePlayer(var4.getPlayer()) ? "invalid player" : var3;
      }
   }

   public interface IAntiCheat {
      IsoPlayer getPlayer();

      float getAmount();
   }
}
