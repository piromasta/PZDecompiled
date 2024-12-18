package zombie.entity.components.fluids;

import java.util.HashMap;
import java.util.HashSet;

public enum PoisonEffect {
   None(0),
   Mild(1),
   Medium(2),
   Severe(3),
   Extreme(4),
   Deadly(5);

   private static final HashSet<String> names = new HashSet();
   private static final HashMap<Integer, PoisonEffect> levelMap = new HashMap();
   private static final HashMap<String, PoisonEffect> nameMap = new HashMap();
   private final int level;
   private String lowerCache = null;

   private PoisonEffect(int var3) {
      this.level = var3;
   }

   public int getLevel() {
      return this.level;
   }

   public int getPlayerEffect() {
      return this.level * 15;
   }

   public static PoisonEffect FromLevel(int var0) {
      PoisonEffect var1 = (PoisonEffect)levelMap.get(var0);
      return var1 != null ? var1 : None;
   }

   public String toStringLower() {
      if (this.lowerCache != null) {
         return this.lowerCache;
      } else {
         this.lowerCache = this.toString().toLowerCase();
         return this.lowerCache;
      }
   }

   public static PoisonEffect FromNameLower(String var0) {
      return (PoisonEffect)nameMap.get(var0.toLowerCase());
   }

   public static boolean containsNameLowercase(String var0) {
      return names.contains(var0.toLowerCase());
   }

   static {
      PoisonEffect[] var0 = values();
      int var1 = var0.length;

      for(int var2 = 0; var2 < var1; ++var2) {
         PoisonEffect var3 = var0[var2];
         names.add(var3.toString().toLowerCase());
         levelMap.put(var3.level, var3);
         nameMap.put(var3.toString().toLowerCase(), var3);
      }

   }
}
