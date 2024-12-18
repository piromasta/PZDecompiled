package zombie.Lua;

import se.krka.kahlua.converter.JavaToLuaConverter;
import se.krka.kahlua.converter.KahluaConverterManager;
import se.krka.kahlua.converter.LuaToJavaConverter;

public final class KahluaNumberConverter {
   private KahluaNumberConverter() {
   }

   public static void install(KahluaConverterManager var0) {
      var0.addLuaConverter(new LuaToJavaConverter<Double, Long>() {
         public Long fromLuaToJava(Double var1, Class<Long> var2) {
            return var1.longValue();
         }

         public Class<Long> getJavaType() {
            return Long.class;
         }

         public Class<Double> getLuaType() {
            return Double.class;
         }
      });
      var0.addLuaConverter(new LuaToJavaConverter<Double, Integer>() {
         public Integer fromLuaToJava(Double var1, Class<Integer> var2) {
            return var1.intValue();
         }

         public Class<Integer> getJavaType() {
            return Integer.class;
         }

         public Class<Double> getLuaType() {
            return Double.class;
         }
      });
      var0.addLuaConverter(new LuaToJavaConverter<Double, Float>() {
         public Float fromLuaToJava(Double var1, Class<Float> var2) {
            return var1.floatValue();
         }

         public Class<Float> getJavaType() {
            return Float.class;
         }

         public Class<Double> getLuaType() {
            return Double.class;
         }
      });
      var0.addLuaConverter(new LuaToJavaConverter<Double, Byte>() {
         public Byte fromLuaToJava(Double var1, Class<Byte> var2) {
            return var1.byteValue();
         }

         public Class<Byte> getJavaType() {
            return Byte.class;
         }

         public Class<Double> getLuaType() {
            return Double.class;
         }
      });
      var0.addLuaConverter(new LuaToJavaConverter<Double, Character>() {
         public Character fromLuaToJava(Double var1, Class<Character> var2) {
            return (char)var1.intValue();
         }

         public Class<Character> getJavaType() {
            return Character.class;
         }

         public Class<Double> getLuaType() {
            return Double.class;
         }
      });
      var0.addLuaConverter(new LuaToJavaConverter<Double, Short>() {
         public Short fromLuaToJava(Double var1, Class<Short> var2) {
            return var1.shortValue();
         }

         public Class<Short> getJavaType() {
            return Short.class;
         }

         public Class<Double> getLuaType() {
            return Double.class;
         }
      });
      var0.addJavaConverter(new NumberToLuaConverter(Double.class));
      var0.addJavaConverter(new NumberToLuaConverter(Float.class));
      var0.addJavaConverter(new NumberToLuaConverter(Integer.class));
      var0.addJavaConverter(new NumberToLuaConverter(Long.class));
      var0.addJavaConverter(new NumberToLuaConverter(Short.class));
      var0.addJavaConverter(new NumberToLuaConverter(Byte.class));
      var0.addJavaConverter(new NumberToLuaConverter(Character.class));
      var0.addJavaConverter(new NumberToLuaConverter(Double.TYPE));
      var0.addJavaConverter(new NumberToLuaConverter(Float.TYPE));
      var0.addJavaConverter(new NumberToLuaConverter(Integer.TYPE));
      var0.addJavaConverter(new NumberToLuaConverter(Long.TYPE));
      var0.addJavaConverter(new NumberToLuaConverter(Short.TYPE));
      var0.addJavaConverter(new NumberToLuaConverter(Byte.TYPE));
      var0.addJavaConverter(new NumberToLuaConverter(Character.TYPE));
      var0.addJavaConverter(new JavaToLuaConverter<Boolean>() {
         public Object fromJavaToLua(Boolean var1) {
            return var1;
         }

         public Class<Boolean> getJavaType() {
            return Boolean.class;
         }
      });
   }

   private static final class NumberToLuaConverter<T extends Number> implements JavaToLuaConverter<T> {
      private final Class<T> clazz;

      public NumberToLuaConverter(Class<T> var1) {
         this.clazz = var1;
      }

      public Object fromJavaToLua(T var1) {
         return var1 instanceof Double ? var1 : KahluaNumberConverter.DoubleCache.valueOf(var1.doubleValue());
      }

      public Class<T> getJavaType() {
         return this.clazz;
      }
   }

   private static final class DoubleCache {
      static final int low = -128;
      static final int high = 10000;
      static final Double[] cache = new Double[10129];

      private DoubleCache() {
      }

      public static Double valueOf(double var0) {
         return var0 == (double)((int)var0) && var0 >= -128.0 && var0 <= 10000.0 ? cache[(int)(var0 + 128.0)] : var0;
      }

      static {
         int var0 = -128;

         for(int var1 = 0; var1 < cache.length; ++var1) {
            cache[var1] = (double)(var0++);
         }

      }
   }
}
