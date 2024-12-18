package zombie.entity;

import java.util.HashMap;
import zombie.entity.util.enums.IOEnum;

public enum GameEntityType implements IOEnum {
   IsoObject((byte)1, 1),
   InventoryItem((byte)2, 2),
   VehiclePart((byte)3, 4),
   IsoMovingObject((byte)4, 8),
   Template((byte)5, 16),
   MetaEntity((byte)5, 32);

   private static final HashMap<Byte, GameEntityType> map = new HashMap();
   private final byte id;
   private final int bits;

   private GameEntityType(byte var3, int var4) {
      this.id = var3;
      this.bits = var4;
   }

   public byte getId() {
      return this.id;
   }

   public static GameEntityType FromID(byte var0) {
      return (GameEntityType)map.get(var0);
   }

   public byte getByteId() {
      return this.id;
   }

   public int getBits() {
      return this.bits;
   }

   static {
      GameEntityType[] var0 = values();
      int var1 = var0.length;

      for(int var2 = 0; var2 < var1; ++var2) {
         GameEntityType var3 = var0[var2];
         map.put(var3.id, var3);
      }

   }
}
