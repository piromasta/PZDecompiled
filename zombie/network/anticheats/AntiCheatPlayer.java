package zombie.network.anticheats;

import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatPlayer extends AbstractAntiCheat {
   public AntiCheatPlayer() {
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      var1.validator.playerUpdateTimeoutReset();
      IAntiCheat var4 = (IAntiCheat)var2;
      IsoPlayer[] var5 = var1.players;
      int var6 = var5.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         IsoPlayer var8 = var5[var7];
         if (var8 == var4.getPlayer()) {
            return var3;
         }
      }

      return "invalid player";
   }

   public interface IAntiCheat {
      IsoPlayer getPlayer();
   }
}
