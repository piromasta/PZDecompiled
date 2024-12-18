package zombie.entity.util.reflect;

import java.lang.reflect.Modifier;

public final class ClassReflection {
   public ClassReflection() {
   }

   public static Class forName(String var0) throws ReflectionException {
      try {
         return Class.forName(var0);
      } catch (ClassNotFoundException var2) {
         throw new ReflectionException("Class not found: " + var0, var2);
      }
   }

   public static String getSimpleName(Class var0) {
      return var0.getSimpleName();
   }

   public static boolean isInstance(Class var0, Object var1) {
      return var0.isInstance(var1);
   }

   public static boolean isAssignableFrom(Class var0, Class var1) {
      return var0.isAssignableFrom(var1);
   }

   public static boolean isMemberClass(Class var0) {
      return var0.isMemberClass();
   }

   public static boolean isStaticClass(Class var0) {
      return Modifier.isStatic(var0.getModifiers());
   }

   public static boolean isArray(Class var0) {
      return var0.isArray();
   }

   public static boolean isPrimitive(Class var0) {
      return var0.isPrimitive();
   }

   public static boolean isEnum(Class var0) {
      return var0.isEnum();
   }

   public static boolean isAnnotation(Class var0) {
      return var0.isAnnotation();
   }

   public static boolean isInterface(Class var0) {
      return var0.isInterface();
   }

   public static boolean isAbstract(Class var0) {
      return Modifier.isAbstract(var0.getModifiers());
   }

   public static <T> T newInstance(Class<T> var0) throws ReflectionException {
      try {
         return var0.newInstance();
      } catch (InstantiationException var2) {
         throw new ReflectionException("Could not instantiate instance of class: " + var0.getName(), var2);
      } catch (IllegalAccessException var3) {
         throw new ReflectionException("Could not instantiate instance of class: " + var0.getName(), var3);
      }
   }

   public static Class getComponentType(Class var0) {
      return var0.getComponentType();
   }

   public static Constructor[] getConstructors(Class var0) {
      java.lang.reflect.Constructor[] var1 = var0.getConstructors();
      Constructor[] var2 = new Constructor[var1.length];
      int var3 = 0;

      for(int var4 = var1.length; var3 < var4; ++var3) {
         var2[var3] = new Constructor(var1[var3]);
      }

      return var2;
   }

   public static Constructor getConstructor(Class var0, Class... var1) throws ReflectionException {
      try {
         return new Constructor(var0.getConstructor(var1));
      } catch (SecurityException var3) {
         throw new ReflectionException("Security violation occurred while getting constructor for class: '" + var0.getName() + "'.", var3);
      } catch (NoSuchMethodException var4) {
         throw new ReflectionException("Constructor not found for class: " + var0.getName(), var4);
      }
   }

   public static Constructor getDeclaredConstructor(Class var0, Class... var1) throws ReflectionException {
      try {
         return new Constructor(var0.getDeclaredConstructor(var1));
      } catch (SecurityException var3) {
         throw new ReflectionException("Security violation while getting constructor for class: " + var0.getName(), var3);
      } catch (NoSuchMethodException var4) {
         throw new ReflectionException("Constructor not found for class: " + var0.getName(), var4);
      }
   }

   public static Object[] getEnumConstants(Class var0) {
      return var0.getEnumConstants();
   }

   public static Method[] getMethods(Class var0) {
      java.lang.reflect.Method[] var1 = var0.getMethods();
      Method[] var2 = new Method[var1.length];
      int var3 = 0;

      for(int var4 = var1.length; var3 < var4; ++var3) {
         var2[var3] = new Method(var1[var3]);
      }

      return var2;
   }

   public static Method getMethod(Class var0, String var1, Class... var2) throws ReflectionException {
      try {
         return new Method(var0.getMethod(var1, var2));
      } catch (SecurityException var4) {
         throw new ReflectionException("Security violation while getting method: " + var1 + ", for class: " + var0.getName(), var4);
      } catch (NoSuchMethodException var5) {
         throw new ReflectionException("Method not found: " + var1 + ", for class: " + var0.getName(), var5);
      }
   }

   public static Method[] getDeclaredMethods(Class var0) {
      java.lang.reflect.Method[] var1 = var0.getDeclaredMethods();
      Method[] var2 = new Method[var1.length];
      int var3 = 0;

      for(int var4 = var1.length; var3 < var4; ++var3) {
         var2[var3] = new Method(var1[var3]);
      }

      return var2;
   }

   public static Method getDeclaredMethod(Class var0, String var1, Class... var2) throws ReflectionException {
      try {
         return new Method(var0.getDeclaredMethod(var1, var2));
      } catch (SecurityException var4) {
         throw new ReflectionException("Security violation while getting method: " + var1 + ", for class: " + var0.getName(), var4);
      } catch (NoSuchMethodException var5) {
         throw new ReflectionException("Method not found: " + var1 + ", for class: " + var0.getName(), var5);
      }
   }

   public static Field[] getFields(Class var0) {
      java.lang.reflect.Field[] var1 = var0.getFields();
      Field[] var2 = new Field[var1.length];
      int var3 = 0;

      for(int var4 = var1.length; var3 < var4; ++var3) {
         var2[var3] = new Field(var1[var3]);
      }

      return var2;
   }

   public static Field getField(Class var0, String var1) throws ReflectionException {
      try {
         return new Field(var0.getField(var1));
      } catch (SecurityException var3) {
         throw new ReflectionException("Security violation while getting field: " + var1 + ", for class: " + var0.getName(), var3);
      } catch (NoSuchFieldException var4) {
         throw new ReflectionException("Field not found: " + var1 + ", for class: " + var0.getName(), var4);
      }
   }

   public static Field[] getDeclaredFields(Class var0) {
      java.lang.reflect.Field[] var1 = var0.getDeclaredFields();
      Field[] var2 = new Field[var1.length];
      int var3 = 0;

      for(int var4 = var1.length; var3 < var4; ++var3) {
         var2[var3] = new Field(var1[var3]);
      }

      return var2;
   }

   public static Field getDeclaredField(Class var0, String var1) throws ReflectionException {
      try {
         return new Field(var0.getDeclaredField(var1));
      } catch (SecurityException var3) {
         throw new ReflectionException("Security violation while getting field: " + var1 + ", for class: " + var0.getName(), var3);
      } catch (NoSuchFieldException var4) {
         throw new ReflectionException("Field not found: " + var1 + ", for class: " + var0.getName(), var4);
      }
   }

   public static boolean isAnnotationPresent(Class var0, Class<? extends java.lang.annotation.Annotation> var1) {
      return var0.isAnnotationPresent(var1);
   }

   public static Annotation[] getAnnotations(Class var0) {
      java.lang.annotation.Annotation[] var1 = var0.getAnnotations();
      Annotation[] var2 = new Annotation[var1.length];

      for(int var3 = 0; var3 < var1.length; ++var3) {
         var2[var3] = new Annotation(var1[var3]);
      }

      return var2;
   }

   public static Annotation getAnnotation(Class var0, Class<? extends java.lang.annotation.Annotation> var1) {
      java.lang.annotation.Annotation var2 = var0.getAnnotation(var1);
      return var2 != null ? new Annotation(var2) : null;
   }

   public static Annotation[] getDeclaredAnnotations(Class var0) {
      java.lang.annotation.Annotation[] var1 = var0.getDeclaredAnnotations();
      Annotation[] var2 = new Annotation[var1.length];

      for(int var3 = 0; var3 < var1.length; ++var3) {
         var2[var3] = new Annotation(var1[var3]);
      }

      return var2;
   }

   public static Annotation getDeclaredAnnotation(Class var0, Class<? extends java.lang.annotation.Annotation> var1) {
      java.lang.annotation.Annotation[] var2 = var0.getDeclaredAnnotations();
      java.lang.annotation.Annotation[] var3 = var2;
      int var4 = var2.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         java.lang.annotation.Annotation var6 = var3[var5];
         if (var6.annotationType().equals(var1)) {
            return new Annotation(var6);
         }
      }

      return null;
   }

   public static Class[] getInterfaces(Class var0) {
      return var0.getInterfaces();
   }
}
