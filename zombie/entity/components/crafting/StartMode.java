package zombie.entity.components.crafting;

import java.util.HashMap;
import zombie.entity.util.enums.IOEnum;

public enum StartMode implements IOEnum {
   Manual((byte)1, 1),
   Automatic((byte)2, 2),
   Passive((byte)3, 4);

   private static final HashMap<Byte, StartMode> cache = new HashMap();
   byte id;
   int bits;

   private StartMode(byte var3, int var4) {
      this.id = var3;
      this.bits = var4;
   }

   public byte getByteId() {
      return this.id;
   }

   public static StartMode fromByteId(byte var0) {
      return (StartMode)cache.get(var0);
   }

   public int getBits() {
      return this.bits;
   }

   static {
      StartMode[] var0 = values();
      int var1 = var0.length;

      for(int var2 = 0; var2 < var1; ++var2) {
         StartMode var3 = var0[var2];
         cache.put(var3.id, var3);
      }

   }
}
