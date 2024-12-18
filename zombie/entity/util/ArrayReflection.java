package zombie.entity.util;

public final class ArrayReflection {
   public ArrayReflection() {
   }

   public static Object newInstance(Class var0, int var1) {
      return java.lang.reflect.Array.newInstance(var0, var1);
   }

   public static int getLength(Object var0) {
      return java.lang.reflect.Array.getLength(var0);
   }

   public static Object get(Object var0, int var1) {
      return java.lang.reflect.Array.get(var0, var1);
   }

   public static void set(Object var0, int var1, Object var2) {
      java.lang.reflect.Array.set(var0, var1, var2);
   }
}
