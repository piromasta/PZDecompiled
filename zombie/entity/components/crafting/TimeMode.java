package zombie.entity.components.crafting;

import java.util.HashMap;
import zombie.entity.util.enums.IOEnum;

/** @deprecated */
@Deprecated
public enum TimeMode implements IOEnum {
   Seconds((byte)1, 1),
   GameMinutes((byte)2, 2);

   private static final HashMap<Byte, TimeMode> cache = new HashMap();
   byte id;
   int bits;

   private TimeMode(byte var3, int var4) {
      this.id = var3;
      this.bits = var4;
   }

   public byte getByteId() {
      return this.id;
   }

   public static TimeMode fromByteId(byte var0) {
      return (TimeMode)cache.get(var0);
   }

   public int getBits() {
      return this.bits;
   }

   static {
      TimeMode[] var0 = values();
      int var1 = var0.length;

      for(int var2 = 0; var2 < var1; ++var2) {
         TimeMode var3 = var0[var2];
         cache.put(var3.id, var3);
      }

   }
}
