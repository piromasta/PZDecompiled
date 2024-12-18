package zombie.iso.worldgen.zones;

import se.krka.kahlua.vm.KahluaTable;
import zombie.iso.zones.Zone;

public class WorldGenZone extends Zone {
   private boolean rocks = true;

   public WorldGenZone(String var1, String var2, int var3, int var4, int var5, int var6, int var7, KahluaTable var8) {
      super(var1, var2, var3, var4, var5, var6, var7);
      if (var8 != null) {
         Object var9 = var8.rawget("Rocks");
         if (var9 instanceof Boolean) {
            this.rocks = (Boolean)var9;
         }
      }

   }

   public boolean getRocks() {
      return this.rocks;
   }
}
