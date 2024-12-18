package zombie.entity.components.resources;

public enum ResourceIO {
   Input((byte)1),
   Output((byte)2),
   Any((byte)3);

   private final byte id;

   private ResourceIO(byte var3) {
      this.id = var3;
   }

   public byte getId() {
      return this.id;
   }

   public static ResourceIO fromId(byte var0) {
      ResourceIO[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         ResourceIO var4 = var1[var3];
         if (var4.id == var0) {
            return var4;
         }
      }

      return null;
   }
}
