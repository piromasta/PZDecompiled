package zombie.core.opengl;

public abstract class IOpenGLState<T extends Value> {
   protected final T currentValue = this.defaultValue();
   private boolean dirty = true;

   public IOpenGLState() {
   }

   public void set(T var1) {
      if (this.dirty || !var1.equals(this.currentValue)) {
         this.setCurrentValue(var1);
         this.Set(var1);
      }

   }

   void setCurrentValue(T var1) {
      this.dirty = false;
      this.currentValue.set(var1);
   }

   public void setDirty() {
      this.dirty = true;
   }

   public void restore() {
      this.dirty = false;
      this.Set(this.getCurrentValue());
   }

   T getCurrentValue() {
      return this.currentValue;
   }

   abstract T defaultValue();

   abstract void Set(T var1);

   public interface Value {
      Value set(Value var1);
   }
}
