package zombie.entity.util.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public final class Method {
   private final java.lang.reflect.Method method;

   Method(java.lang.reflect.Method var1) {
      this.method = var1;
   }

   public String getName() {
      return this.method.getName();
   }

   public Class getReturnType() {
      return this.method.getReturnType();
   }

   public Class[] getParameterTypes() {
      return this.method.getParameterTypes();
   }

   public Class getDeclaringClass() {
      return this.method.getDeclaringClass();
   }

   public boolean isAccessible() {
      return this.method.isAccessible();
   }

   public void setAccessible(boolean var1) {
      this.method.setAccessible(var1);
   }

   public boolean isAbstract() {
      return Modifier.isAbstract(this.method.getModifiers());
   }

   public boolean isDefaultAccess() {
      return !this.isPrivate() && !this.isProtected() && !this.isPublic();
   }

   public boolean isFinal() {
      return Modifier.isFinal(this.method.getModifiers());
   }

   public boolean isPrivate() {
      return Modifier.isPrivate(this.method.getModifiers());
   }

   public boolean isProtected() {
      return Modifier.isProtected(this.method.getModifiers());
   }

   public boolean isPublic() {
      return Modifier.isPublic(this.method.getModifiers());
   }

   public boolean isNative() {
      return Modifier.isNative(this.method.getModifiers());
   }

   public boolean isStatic() {
      return Modifier.isStatic(this.method.getModifiers());
   }

   public boolean isVarArgs() {
      return this.method.isVarArgs();
   }

   public Object invoke(Object var1, Object... var2) throws ReflectionException {
      try {
         return this.method.invoke(var1, var2);
      } catch (IllegalArgumentException var4) {
         throw new ReflectionException("Illegal argument(s) supplied to method: " + this.getName(), var4);
      } catch (IllegalAccessException var5) {
         throw new ReflectionException("Illegal access to method: " + this.getName(), var5);
      } catch (InvocationTargetException var6) {
         throw new ReflectionException("Exception occurred in method: " + this.getName(), var6);
      }
   }

   public boolean isAnnotationPresent(Class<? extends java.lang.annotation.Annotation> var1) {
      return this.method.isAnnotationPresent(var1);
   }

   public Annotation[] getDeclaredAnnotations() {
      java.lang.annotation.Annotation[] var1 = this.method.getDeclaredAnnotations();
      Annotation[] var2 = new Annotation[var1.length];

      for(int var3 = 0; var3 < var1.length; ++var3) {
         var2[var3] = new Annotation(var1[var3]);
      }

      return var2;
   }

   public Annotation getDeclaredAnnotation(Class<? extends java.lang.annotation.Annotation> var1) {
      java.lang.annotation.Annotation[] var2 = this.method.getDeclaredAnnotations();
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
}
