package zombie.entity.components.resources;

public enum ResourceType {
   Item((byte)1),
   Fluid((byte)2),
   Energy((byte)3),
   Any((byte)0);

   private final byte id;

   private ResourceType(byte var3) {
      this.id = var3;
   }

   public byte getId() {
      return this.id;
   }

   public static ResourceType fromId(byte var0) {
      ResourceType[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         ResourceType var4 = var1[var3];
         if (var4.id == var0) {
            return var4;
         }
      }

      return null;
   }
}
