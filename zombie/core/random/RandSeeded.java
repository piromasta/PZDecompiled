package zombie.core.random;

import java.util.Random;
import zombie.iso.worldgen.WGParams;

public class RandSeeded extends RandAbstract {
   public RandSeeded(long var1) {
      this.rand = new Random(var1);
   }

   public void init() {
      this.rand = new Random(WGParams.instance.getSeed());
   }
}
