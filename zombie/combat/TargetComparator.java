package zombie.combat;

import java.util.Comparator;
import zombie.core.physics.BallisticsController;
import zombie.network.fields.HitInfo;

public class TargetComparator implements Comparator<HitInfo> {
   protected BallisticsController ballisticsController;

   public TargetComparator() {
   }

   public void setBallisticsController(BallisticsController var1) {
      this.ballisticsController = var1;
   }

   public int compare(HitInfo var1, HitInfo var2) {
      return 0;
   }
}
