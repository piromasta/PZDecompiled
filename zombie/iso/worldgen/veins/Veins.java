package zombie.iso.worldgen.veins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import zombie.debug.DebugLog;
import zombie.iso.worldgen.WGParams;

public class Veins {
   private Map<String, List<OreVein>> cache = new HashMap();
   private Map<String, OreVeinConfig> config;

   public Veins(Map<String, OreVeinConfig> var1) {
      this.config = var1;
   }

   public List<OreVein> get(int var1, int var2) {
      if (this.cache.containsKey("" + var1 + "_" + var2)) {
         return (List)this.cache.get("" + var1 + "_" + var2);
      } else {
         Random var3 = WGParams.instance.getRandom(var1, var2);
         ArrayList var4 = new ArrayList();
         Iterator var5 = this.config.values().iterator();

         while(var5.hasNext()) {
            OreVeinConfig var6 = (OreVeinConfig)var5.next();
            if (!(var3.nextFloat() > var6.getProbability())) {
               var4.add(new OreVein(var1, var2, var6, var3));
            }
         }

         this.cache.put("" + var1 + "_" + var2, var4);
         if (!var4.isEmpty()) {
            DebugLog.log(var4.toString());
         }

         return var4;
      }
   }
}
