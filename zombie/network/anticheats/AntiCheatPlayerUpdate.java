package zombie.network.anticheats;

import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;

public class AntiCheatPlayerUpdate extends AbstractAntiCheat {
   public AntiCheatPlayerUpdate() {
   }

   public boolean update(UdpConnection var1) {
      super.update(var1);
      PacketValidator var2 = var1.validator;
      IsoPlayer[] var3 = var1.players;
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         IsoPlayer var6 = var3[var5];
         if (var6 != null && var6.isDead()) {
            var1.validator.playerUpdateTimeoutReset();
            return true;
         }
      }

      if (var2.playerUpdateTimeoutCheck()) {
         return false;
      } else {
         return true;
      }
   }

   public interface IAntiCheatUpdate {
      boolean playerUpdateTimeoutCheck();

      void playerUpdateTimeoutReset();
   }
}
