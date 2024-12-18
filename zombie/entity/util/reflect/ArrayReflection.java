package zombie.entity.util.reflect;

import java.lang.reflect.Array;

public final class ArrayReflection {
   public ArrayReflection() {
   }

   public static Object newInstance(Class var0, int var1) {
      return Array.newInstance(var0, var1);
   }

   public static int getLength(Object var0) {
      return Array.getLength(var0);
   }

   public static Object get(Object var0, int var1) {
      return Array.get(var0, var1);
   }

   public static void set(Object var0, int var1, Object var2) {
      Array.set(var0, var1, var2);
   }
}
