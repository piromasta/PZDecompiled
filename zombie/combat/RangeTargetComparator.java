package zombie.combat;

import zombie.CombatManager;
import zombie.ai.states.ZombieGetUpState;
import zombie.characters.IsoZombie;
import zombie.network.fields.HitInfo;
import zombie.util.Type;

public class RangeTargetComparator extends TargetComparator {
   public RangeTargetComparator() {
   }

   public int compare(HitInfo var1, HitInfo var2) {
      float var3 = var1.distSq;
      float var4 = var2.distSq;
      IsoZombie var5 = (IsoZombie)Type.tryCastTo(var1.getObject(), IsoZombie.class);
      IsoZombie var6 = (IsoZombie)Type.tryCastTo(var2.getObject(), IsoZombie.class);
      if (var5 != null && var6 != null) {
         boolean var7 = CombatManager.isProne(var5);
         boolean var8 = CombatManager.isProne(var6);
         boolean var9 = var5.isCurrentState(ZombieGetUpState.instance());
         boolean var10 = var6.isCurrentState(ZombieGetUpState.instance());
         if (var9 && !var10 && var8) {
            return -1;
         }

         if (!var9 && var7 && var10) {
            return 1;
         }

         if (var7 && var8) {
            if (var5.isCrawling() && !var6.isCrawling()) {
               return -1;
            }

            if (!var5.isCrawling() && var6.isCrawling()) {
               return 1;
            }
         }
      }

      if (var3 > var4) {
         return 1;
      } else {
         return var4 > var3 ? -1 : 0;
      }
   }
}
