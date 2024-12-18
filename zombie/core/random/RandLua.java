package zombie.core.random;

import org.uncommons.maths.random.CellularAutomatonRNG;
import org.uncommons.maths.random.SeedException;

public class RandLua extends RandAbstract {
   public static final RandLua INSTANCE = new RandLua();

   protected RandLua() {
   }

   public void init() {
      try {
         this.rand = new CellularAutomatonRNG(new PZSeedGenerator());
      } catch (SeedException var2) {
         var2.printStackTrace();
      }

   }
}
