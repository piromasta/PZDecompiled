package zombie.entity.components.fluids;

import java.util.ArrayList;
import java.util.HashMap;

public enum FluidCategory {
   Beverage((byte)1),
   Alcoholic((byte)2),
   Hazardous((byte)3),
   Medical((byte)4),
   Industrial((byte)5),
   Colors((byte)6),
   Dyes((byte)7),
   HairDyes((byte)8),
   Paint((byte)9),
   Fuel((byte)10),
   Poisons((byte)11),
   Water((byte)12);

   private static final HashMap<Byte, FluidCategory> idMap = new HashMap();
   private static final ArrayList<FluidCategory> list = new ArrayList();
   private byte id;

   private FluidCategory(byte var3) {
      this.id = var3;
   }

   public byte getId() {
      return this.id;
   }

   public static FluidCategory FromId(byte var0) {
      return (FluidCategory)idMap.get(var0);
   }

   public static ArrayList<FluidCategory> getList() {
      return list;
   }

   static {
      FluidCategory[] var0 = values();
      int var1 = var0.length;

      for(int var2 = 0; var2 < var1; ++var2) {
         FluidCategory var3 = var0[var2];
         idMap.put(var3.id, var3);
         list.add(var3);
      }

   }
}
