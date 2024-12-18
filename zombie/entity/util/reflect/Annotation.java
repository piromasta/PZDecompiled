package zombie.entity.util.reflect;

public final class Annotation {
   private java.lang.annotation.Annotation annotation;

   Annotation(java.lang.annotation.Annotation var1) {
      this.annotation = var1;
   }

   public <T extends java.lang.annotation.Annotation> T getAnnotation(Class<T> var1) {
      return this.annotation.annotationType().equals(var1) ? this.annotation : null;
   }

   public Class<? extends java.lang.annotation.Annotation> getAnnotationType() {
      return this.annotation.annotationType();
   }
}
