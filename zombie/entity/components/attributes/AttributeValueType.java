package zombie.entity.components.attributes;

import java.util.EnumSet;

public enum AttributeValueType {
   Boolean((byte)0),
   String((byte)1),
   Float((byte)2),
   Double((byte)3),
   Byte((byte)4),
   Short((byte)5),
   Int((byte)6),
   Long((byte)7),
   Enum((byte)8),
   EnumSet((byte)9),
   EnumStringSet((byte)10);

   private static final EnumSet<AttributeValueType> numerics = java.util.EnumSet.of(Float, Double, Byte, Short, Int, Long);
   private static final EnumSet<AttributeValueType> decimals = java.util.EnumSet.of(Float, Double);
   private final byte byteIndex;

   private AttributeValueType(byte var3) {
      this.byteIndex = var3;
   }

   public int getByteIndex() {
      return this.byteIndex;
   }

   public static boolean IsNumeric(AttributeValueType var0) {
      return numerics.contains(var0);
   }

   public static boolean IsDecimal(AttributeValueType var0) {
      return decimals.contains(var0);
   }

   public static AttributeValueType fromByteIndex(int var0) {
      return ((AttributeValueType[])AttributeValueType.class.getEnumConstants())[var0];
   }

   public static AttributeValueType valueOfIgnoreCase(String var0) {
      if (var0 == null) {
         return null;
      } else {
         AttributeValueType[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            AttributeValueType var4 = var1[var3];
            if (var4.name().equalsIgnoreCase(var0)) {
               return var4;
            }
         }

         return null;
      }
   }
}
