package zombie.core.random;

import org.uncommons.maths.random.CellularAutomatonRNG;
import org.uncommons.maths.random.SeedException;

public class RandStandard extends RandAbstract {
   public static final RandStandard INSTANCE = new RandStandard();

   protected RandStandard() {
   }

   public void init() {
      try {
         this.rand = new CellularAutomatonRNG(new PZSeedGenerator());
      } catch (SeedException var2) {
         var2.printStackTrace();
      }

   }
}
