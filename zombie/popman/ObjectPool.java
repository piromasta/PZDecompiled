package zombie.popman;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import zombie.debug.DebugOptions;

public class ObjectPool<T> {
   private final Allocator<T> allocator;
   private final ArrayList<T> pool;

   public ObjectPool() {
      this((Allocator)null);
   }

   public ObjectPool(Allocator<T> var1) {
      this.pool = new ArrayList<T>() {
         public boolean contains(Object var1) {
            for(int var2 = 0; var2 < ObjectPool.this.pool.size(); ++var2) {
               if (ObjectPool.this.pool.get(var2) == var1) {
                  return true;
               }
            }

            return false;
         }
      };
      this.allocator = var1;
   }

   public synchronized T alloc() {
      return this.pool.isEmpty() ? this.makeObject() : this.pool.remove(this.pool.size() - 1);
   }

   public synchronized void release(T var1) {
      assert var1 != null;

      assert !this.pool.contains(var1);

      if (var1 != null) {
         if (!DebugOptions.instance.Checks.ObjectPoolContains.getValue() || !this.pool.contains(var1)) {
            this.pool.add(var1);
         }
      }
   }

   public synchronized void release(List<T> var1) {
      for(int var2 = 0; var2 < var1.size(); ++var2) {
         if (var1.get(var2) != null) {
            this.release(var1.get(var2));
         }
      }

   }

   public synchronized void release(Iterable<T> var1) {
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         Object var3 = var2.next();
         if (var3 != null) {
            this.release(var3);
         }
      }

   }

   public synchronized void release(T[] var1) {
      if (var1 != null) {
         for(int var2 = 0; var2 < var1.length; ++var2) {
            if (var1[var2] != null) {
               this.release(var1[var2]);
            }
         }

      }
   }

   public synchronized void releaseAll(List<T> var1) {
      for(int var2 = 0; var2 < var1.size(); ++var2) {
         if (var1.get(var2) != null) {
            this.release(var1.get(var2));
         }
      }

   }

   public synchronized void clear() {
      this.pool.clear();
   }

   protected T makeObject() {
      if (this.allocator != null) {
         return this.allocator.allocate();
      } else {
         throw new UnsupportedOperationException("Allocator is null. The ObjectPool is intended to be used with an allocator, or with the function makeObject overridden in a subclass.");
      }
   }

   public synchronized void forEach(Consumer<T> var1) {
      for(int var2 = 0; var2 < this.pool.size(); ++var2) {
         var1.accept(this.pool.get(var2));
      }

   }

   public interface Allocator<T> {
      T allocate();
   }
}
