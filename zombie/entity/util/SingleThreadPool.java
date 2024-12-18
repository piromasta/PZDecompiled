package zombie.entity.util;

public abstract class SingleThreadPool<T> {
   public final int max;
   public int peak;
   private final Array<T> freeObjects;

   public SingleThreadPool() {
      this(16, 2147483647);
   }

   public SingleThreadPool(int var1) {
      this(var1, 2147483647);
   }

   public SingleThreadPool(int var1, int var2) {
      this.freeObjects = new Array(false, var1);
      this.max = var2;
   }

   protected abstract T newObject();

   public T obtain() {
      return this.freeObjects.size == 0 ? this.newObject() : this.freeObjects.pop();
   }

   public void free(T var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("object cannot be null.");
      } else {
         if (this.freeObjects.size < this.max) {
            this.freeObjects.add(var1);
            this.peak = Math.max(this.peak, this.freeObjects.size);
            this.reset(var1);
         } else {
            this.discard(var1);
         }

      }
   }

   public void fill(int var1) {
      for(int var2 = 0; var2 < var1; ++var2) {
         if (this.freeObjects.size < this.max) {
            this.freeObjects.add(this.newObject());
         }
      }

      this.peak = Math.max(this.peak, this.freeObjects.size);
   }

   protected void reset(T var1) {
      if (var1 instanceof Poolable) {
         ((Poolable)var1).reset();
      }

   }

   protected void discard(T var1) {
      this.reset(var1);
   }

   public void freeAll(Array<T> var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("objects cannot be null.");
      } else {
         Array var2 = this.freeObjects;
         int var3 = this.max;
         int var4 = 0;

         for(int var5 = var1.size; var4 < var5; ++var4) {
            Object var6 = var1.get(var4);
            if (var6 != null) {
               if (var2.size < var3) {
                  var2.add(var6);
                  this.reset(var6);
               } else {
                  this.discard(var6);
               }
            }
         }

         this.peak = Math.max(this.peak, var2.size);
      }
   }

   public void clear() {
      Array var1 = this.freeObjects;
      int var2 = 0;

      for(int var3 = var1.size; var2 < var3; ++var2) {
         this.discard(var1.get(var2));
      }

      var1.clear();
   }

   public int getFree() {
      return this.freeObjects.size;
   }

   public interface Poolable {
      void reset();
   }
}
