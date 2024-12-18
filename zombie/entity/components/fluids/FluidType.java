package zombie.entity.components.fluids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import zombie.core.Core;

public enum FluidType {
   Water((byte)1),
   Petrol((byte)2),
   Alcohol((byte)3),
   TaintedWater((byte)4),
   Beer((byte)5),
   Whiskey((byte)6),
   SodaPop((byte)7),
   Coffee((byte)8),
   Tea((byte)9),
   Wine((byte)10),
   Bleach((byte)11),
   Blood((byte)12),
   Honey((byte)14),
   Mead((byte)15),
   Acid((byte)16),
   SpiffoJuice((byte)17),
   SecretFlavoring((byte)18),
   CarbonatedWater((byte)19),
   CowMilk((byte)20),
   SheepMilk((byte)21),
   CleaningLiquid((byte)22),
   AnimalBlood((byte)23),
   AnimalGrease((byte)24),
   Dye((byte)64),
   HairDye((byte)65),
   Paint((byte)66),
   PoisonWeak((byte)70),
   PoisonNormal((byte)71),
   PoisonStrong((byte)72),
   PoisonPotent((byte)73),
   Modded((byte)127),
   None((byte)-1);

   private static final HashSet<String> fluidNames = new HashSet();
   private static final HashMap<Byte, FluidType> fluidIdMap = new HashMap();
   private static final HashMap<String, FluidType> fluidNameMap = new HashMap();
   private final byte id;
   private String lowerCache = null;

   private FluidType(byte var3) {
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
      return fluidNames.contains(var0.toLowerCase());
   }

   public static FluidType FromId(byte var0) {
      return (FluidType)fluidIdMap.get(var0);
   }

   public static FluidType FromNameLower(String var0) {
      return (FluidType)fluidNameMap.get(var0.toLowerCase());
   }

   public static ArrayList<String> getAllFluidName() {
      return new ArrayList(fluidNames);
   }

   static {
      FluidType[] var0 = values();
      int var1 = var0.length;

      for(int var2 = 0; var2 < var1; ++var2) {
         FluidType var3 = var0[var2];
         if (Core.bDebug && fluidIdMap.containsKey(var3.id)) {
            throw new IllegalStateException("ID duplicate in FluidType");
         }

         fluidNames.add(var3.toString().toLowerCase());
         fluidNameMap.put(var3.toString().toLowerCase(), var3);
         fluidIdMap.put(var3.id, var3);
      }

   }
}
