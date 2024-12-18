package zombie.core.random;

import zombie.iso.worldgen.WGParams;

public class RandLocation extends RandAbstract {
   public RandLocation(int var1, int var2) {
      this.rand = WGParams.instance.getRandom(var1, var2);
   }

   public void init() {
      throw new UnsupportedOperationException();
   }
}
