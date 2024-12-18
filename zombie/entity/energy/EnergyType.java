package zombie.entity.energy;

import java.util.HashMap;
import java.util.HashSet;
import zombie.core.Core;

public enum EnergyType {
   None((byte)-1),
   Electric((byte)1),
   Mechanical((byte)2),
   Thermal((byte)3),
   Steam((byte)4),
   VoidEnergy((byte)126),
   Modded((byte)127);

   private static final HashSet<String> energyNames = new HashSet();
   private static final HashMap<Byte, EnergyType> energyIdMap = new HashMap();
   private static final HashMap<String, EnergyType> energyNameMap = new HashMap();
   private final byte id;
   private String lowerCache = null;

   private EnergyType(byte var3) {
      this.id = var3;
   }

   public byte getId() {
      return this.id;
   }

   public String toStringLower() {
      if (this.lowerCache != null) {
         return this.lowerCache;
      } else {
         this.lowerCache = this.toString().toLowerCase();
         return this.lowerCache;
      }
   }

   public static boolean containsNameLowercase(String var0) {
      return energyNames.contains(var0.toLowerCase());
   }

   public static EnergyType FromId(byte var0) {
      return (EnergyType)energyIdMap.get(var0);
   }

   public static EnergyType FromNameLower(String var0) {
      return (EnergyType)energyNameMap.get(var0.toLowerCase());
   }

   static {
      EnergyType[] var0 = values();
      int var1 = var0.length;

      for(int var2 = 0; var2 < var1; ++var2) {
         EnergyType var3 = var0[var2];
         if (Core.bDebug && energyIdMap.containsKey(var3.id)) {
            throw new IllegalStateException("ID duplicate in EnergyType");
         }

         energyNames.add(var3.toString().toLowerCase());
         energyNameMap.put(var3.toString().toLowerCase(), var3);
         energyIdMap.put(var3.id, var3);
      }

   }
}
