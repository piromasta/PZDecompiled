package zombie.entity.components.resources;

import zombie.core.Color;
import zombie.core.Colors;
import zombie.entity.util.enums.EnumBitStore;
import zombie.entity.util.enums.IOEnum;

public enum ResourceChannel implements IOEnum {
   NO_CHANNEL((byte)0, 0, Colors.Black),
   Channel_Red((byte)1, 1, Colors.Crimson),
   Channel_Yellow((byte)2, 2, Colors.Gold),
   Channel_Blue((byte)3, 4, Colors.DodgerBlue),
   Channel_Orange((byte)4, 8, Colors.Orange),
   Channel_Green((byte)5, 16, Colors.LimeGreen),
   Channel_Purple((byte)6, 32, Colors.MediumSlateBlue),
   Channel_Cyan((byte)7, 64, Colors.Cyan),
   Channel_Magenta((byte)8, 128, Colors.Magenta);

   public static final EnumBitStore<ResourceChannel> BitStoreAll = EnumBitStore.allOf(ResourceChannel.class);
   private final byte id;
   private final int bits;
   private final Color color;

   private ResourceChannel(byte var3, int var4, Color var5) {
      this.id = var3;
      this.bits = var4;
      this.color = var5;
   }

   public byte getByteId() {
      return this.id;
   }

   public int getBits() {
      return this.bits;
   }

   public Color getColor() {
      return this.color;
   }

   public static ResourceChannel fromId(byte var0) {
      ResourceChannel[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         ResourceChannel var4 = var1[var3];
         if (var4.id == var0) {
            return var4;
         }
      }

      return null;
   }
}
