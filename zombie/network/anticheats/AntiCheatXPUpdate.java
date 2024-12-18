package zombie.network.anticheats;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;

public class AntiCheatXPUpdate extends AbstractAntiCheat {
   private static final float MAX_XP_GROWTH_RATE = 1000.0F;

   public AntiCheatXPUpdate() {
   }

   public boolean update(UdpConnection var1) {
      super.update(var1);
      boolean var2 = true;
      IsoPlayer[] var3 = var1.players;
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         IsoPlayer var6 = var3[var5];
         if (var6 != null) {
            var2 &= this.update(var1, var6);
         }
      }

      return var2;
   }

   private boolean update(UdpConnection var1, IsoGameCharacter var2) {
      IsoGameCharacter.XP var3 = var2.getXp();
      if (var3.intervalCheck()) {
         float var4 = var3.getGrowthRate();
         float var5 = var3.getMultiplier();
         if (var4 > 1000.0F * var5) {
            return false;
         }
      }

      return true;
   }

   public interface IAntiCheatUpdate {
      boolean intervalCheck();

      float getGrowthRate();

      float getMultiplier();
   }
}
