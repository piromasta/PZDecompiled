package zombie.entity.components.resources;

import java.util.HashMap;
import zombie.entity.util.enums.IOEnum;

public enum ResourceFlag implements IOEnum {
   AutoDecay((byte)3, 4);

   private static final HashMap<Byte, ResourceFlag> cache = new HashMap();
   final byte id;
   final int bits;

   private ResourceFlag(byte var3, int var4) {
      this.id = var3;
      this.bits = var4;
   }

   public byte getByteId() {
      return this.id;
   }

   public static ResourceFlag fromByteId(byte var0) {
      return (ResourceFlag)cache.get(var0);
   }

   public int getBits() {
      return this.bits;
   }

   static {
      ResourceFlag[] var0 = values();
      int var1 = var0.length;

      for(int var2 = 0; var2 < var1; ++var2) {
         ResourceFlag var3 = var0[var2];
         cache.put(var3.id, var3);
      }

   }
}
