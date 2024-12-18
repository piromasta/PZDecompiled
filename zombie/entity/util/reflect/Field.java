package zombie.entity.util.reflect;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class Field {
   private final java.lang.reflect.Field field;

   Field(java.lang.reflect.Field var1) {
      this.field = var1;
   }

   public String getName() {
      return this.field.getName();
   }

   public Class getType() {
      return this.field.getType();
   }

   public Class getDeclaringClass() {
      return this.field.getDeclaringClass();
   }

   public boolean isAccessible() {
      return this.field.isAccessible();
   }

   public void setAccessible(boolean var1) {
      this.field.setAccessible(var1);
   }

   public boolean isDefaultAccess() {
      return !this.isPrivate() && !this.isProtected() && !this.isPublic();
   }

   public boolean isFinal() {
      return Modifier.isFinal(this.field.getModifiers());
   }

   public boolean isPrivate() {
      return Modifier.isPrivate(this.field.getModifiers());
   }

   public boolean isProtected() {
      return Modifier.isProtected(this.field.getModifiers());
   }

   public boolean isPublic() {
      return Modifier.isPublic(this.field.getModifiers());
   }

   public boolean isStatic() {
      return Modifier.isStatic(this.field.getModifiers());
   }

   public boolean isTransient() {
      return Modifier.isTransient(this.field.getModifiers());
   }

   public boolean isVolatile() {
      return Modifier.isVolatile(this.field.getModifiers());
   }

   public boolean isSynthetic() {
      return this.field.isSynthetic();
   }

   public Class getElementType(int var1) {
      Type var2 = this.field.getGenericType();
      if (var2 instanceof ParameterizedType) {
         Type[] var3 = ((ParameterizedType)var2).getActualTypeArguments();
         if (var3.length - 1 >= var1) {
            Type var4 = var3[var1];
            if (var4 instanceof Class) {
               return (Class)var4;
            }

            if (var4 instanceof ParameterizedType) {
               return (Class)((ParameterizedType)var4).getRawType();
            }

            if (var4 instanceof GenericArrayType) {
               Type var5 = ((GenericArrayType)var4).getGenericComponentType();
               if (var5 instanceof Class) {
                  return ArrayReflection.newInstance((Class)var5, 0).getClass();
               }
            }
         }
      }

      return null;
   }

   public boolean isAnnotationPresent(Class<? extends java.lang.annotation.Annotation> var1) {
      return this.field.isAnnotationPresent(var1);
   }

   public Annotation[] getDeclaredAnnotations() {
      java.lang.annotation.Annotation[] var1 = this.field.getDeclaredAnnotations();
      Annotation[] var2 = new Annotation[var1.length];

      for(int var3 = 0; var3 < var1.length; ++var3) {
         var2[var3] = new Annotation(var1[var3]);
      }

      return var2;
   }

   public Annotation getDeclaredAnnotation(Class<? extends java.lang.annotation.Annotation> var1) {
      java.lang.annotation.Annotation[] var2 = this.field.getDeclaredAnnotations();
      if (var2 == null) {
         return null;
      } else {
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
   }

   public Object get(Object var1) throws ReflectionException {
      try {
         return this.field.get(var1);
      } catch (IllegalArgumentException var3) {
         throw new ReflectionException("Object is not an instance of " + this.getDeclaringClass(), var3);
      } catch (IllegalAccessException var4) {
         throw new ReflectionException("Illegal access to field: " + this.getName(), var4);
      }
   }

   public void set(Object var1, Object var2) throws ReflectionException {
      try {
         this.field.set(var1, var2);
      } catch (IllegalArgumentException var4) {
         throw new ReflectionException("Argument not valid for field: " + this.getName(), var4);
      } catch (IllegalAccessException var5) {
         throw new ReflectionException("Illegal access to field: " + this.getName(), var5);
      }
   }
}
