package zombie.entity.components.attributes;

import java.util.concurrent.ConcurrentLinkedDeque;

public class AttributeFactory {
   private static final boolean POOL_ENABLED = true;
   private static final ConcurrentLinkedDeque<AttributeInstance.Bool> pool_bool = new ConcurrentLinkedDeque();
   private static final ConcurrentLinkedDeque<AttributeInstance.String> pool_string = new ConcurrentLinkedDeque();
   private static final ConcurrentLinkedDeque<AttributeInstance.Float> pool_float = new ConcurrentLinkedDeque();
   private static final ConcurrentLinkedDeque<AttributeInstance.Double> pool_double = new ConcurrentLinkedDeque();
   private static final ConcurrentLinkedDeque<AttributeInstance.Byte> pool_byte = new ConcurrentLinkedDeque();
   private static final ConcurrentLinkedDeque<AttributeInstance.Short> pool_short = new ConcurrentLinkedDeque();
   private static final ConcurrentLinkedDeque<AttributeInstance.Int> pool_int = new ConcurrentLinkedDeque();
   private static final ConcurrentLinkedDeque<AttributeInstance.Long> pool_long = new ConcurrentLinkedDeque();
   private static final ConcurrentLinkedDeque<AttributeInstance.Enum<?>> pool_enum = new ConcurrentLinkedDeque();
   private static final ConcurrentLinkedDeque<AttributeInstance.EnumSet<?>> pool_enumSet = new ConcurrentLinkedDeque();
   private static final ConcurrentLinkedDeque<AttributeInstance.EnumStringSet<?>> pool_enumStringSet = new ConcurrentLinkedDeque();

   public AttributeFactory() {
   }

   public static void Reset() {
   }

   public static <T extends AttributeInstance> T CreateTyped(AttributeType var0) {
      AttributeInstance var1 = AllocAttribute(var0);
      var1.setType(var0);
      return var1;
   }

   public static AttributeInstance Create(AttributeType var0) {
      AttributeInstance var1 = AllocAttribute(var0);
      var1.setType(var0);
      return var1;
   }

   private static AttributeInstance AllocAttribute(AttributeType var0) {
      switch (var0.getValueType()) {
         case String:
            return AllocAttributeString();
         case Boolean:
            return AllocAttributeBool();
         case Float:
            return AllocAttributeFloat();
         case Double:
            return AllocAttributeDouble();
         case Byte:
            return AllocAttributeByte();
         case Short:
            return AllocAttributeShort();
         case Int:
            return AllocAttributeInt();
         case Long:
            return AllocAttributeLong();
         case Enum:
            return AllocAttributeEnum();
         case EnumSet:
            return AllocAttributeEnumSet();
         case EnumStringSet:
            return AllocAttributeEnumStringSet();
         default:
            String var10002 = var0.toString();
            throw new RuntimeException("Could not allocate Attribute. [" + var10002 + ", valueType = " + var0.getValueType() + "]");
      }
   }

   protected static AttributeInstance.Enum AllocAttributeEnum() {
      AttributeInstance.Enum var0 = (AttributeInstance.Enum)pool_enum.poll();
      if (var0 == null) {
         var0 = new AttributeInstance.Enum();
      }

      return var0;
   }

   protected static void Release(AttributeInstance.Enum var0) {
      var0.reset();
      pool_enum.offer(var0);
   }

   protected static AttributeInstance.EnumSet AllocAttributeEnumSet() {
      AttributeInstance.EnumSet var0 = (AttributeInstance.EnumSet)pool_enumSet.poll();
      if (var0 == null) {
         var0 = new AttributeInstance.EnumSet();
      }

      return var0;
   }

   protected static void Release(AttributeInstance.EnumSet var0) {
      var0.reset();
      pool_enumSet.offer(var0);
   }

   protected static AttributeInstance.EnumStringSet AllocAttributeEnumStringSet() {
      AttributeInstance.EnumStringSet var0 = (AttributeInstance.EnumStringSet)pool_enumStringSet.poll();
      if (var0 == null) {
         var0 = new AttributeInstance.EnumStringSet();
      }

      return var0;
   }

   protected static void Release(AttributeInstance.EnumStringSet var0) {
      var0.reset();
      pool_enumStringSet.offer(var0);
   }

   protected static AttributeInstance.String AllocAttributeString() {
      AttributeInstance.String var0 = (AttributeInstance.String)pool_string.poll();
      if (var0 == null) {
         var0 = new AttributeInstance.String();
      }

      return var0;
   }

   protected static void Release(AttributeInstance.String var0) {
      var0.reset();
      pool_string.offer(var0);
   }

   protected static AttributeInstance.Bool AllocAttributeBool() {
      AttributeInstance.Bool var0 = (AttributeInstance.Bool)pool_bool.poll();
      if (var0 == null) {
         var0 = new AttributeInstance.Bool();
      }

      return var0;
   }

   protected static void Release(AttributeInstance.Bool var0) {
      var0.reset();
      pool_bool.offer(var0);
   }

   protected static AttributeInstance.Float AllocAttributeFloat() {
      AttributeInstance.Float var0 = (AttributeInstance.Float)pool_float.poll();
      if (var0 == null) {
         var0 = new AttributeInstance.Float();
      }

      return var0;
   }

   protected static void Release(AttributeInstance.Float var0) {
      var0.reset();
      pool_float.offer(var0);
   }

   protected static AttributeInstance.Double AllocAttributeDouble() {
      AttributeInstance.Double var0 = (AttributeInstance.Double)pool_double.poll();
      if (var0 == null) {
         var0 = new AttributeInstance.Double();
      }

      return var0;
   }

   protected static void Release(AttributeInstance.Double var0) {
      var0.reset();
      pool_double.offer(var0);
   }

   protected static AttributeInstance.Byte AllocAttributeByte() {
      AttributeInstance.Byte var0 = (AttributeInstance.Byte)pool_byte.poll();
      if (var0 == null) {
         var0 = new AttributeInstance.Byte();
      }

      return var0;
   }

   protected static void Release(AttributeInstance.Byte var0) {
      var0.reset();
      pool_byte.offer(var0);
   }

   protected static AttributeInstance.Short AllocAttributeShort() {
      AttributeInstance.Short var0 = (AttributeInstance.Short)pool_short.poll();
      if (var0 == null) {
         var0 = new AttributeInstance.Short();
      }

      return var0;
   }

   protected static void Release(AttributeInstance.Short var0) {
      var0.reset();
      pool_short.offer(var0);
   }

   protected static AttributeInstance.Int AllocAttributeInt() {
      AttributeInstance.Int var0 = (AttributeInstance.Int)pool_int.poll();
      if (var0 == null) {
         var0 = new AttributeInstance.Int();
      }

      return var0;
   }

   protected static void Release(AttributeInstance.Int var0) {
      var0.reset();
      pool_int.offer(var0);
   }

   protected static AttributeInstance.Long AllocAttributeLong() {
      AttributeInstance.Long var0 = (AttributeInstance.Long)pool_long.poll();
      if (var0 == null) {
         var0 = new AttributeInstance.Long();
      }

      return var0;
   }

   protected static void Release(AttributeInstance.Long var0) {
      var0.reset();
      pool_long.offer(var0);
   }
}
