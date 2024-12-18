package zombie.util;

import java.util.List;

public interface IPooledObject {
   Pool.PoolReference getPoolReference();

   void setPool(Pool.PoolReference var1);

   void release();

   boolean isFree();

   void setFree(boolean var1);

   default void onReleased() {
   }

   static <E extends IPooledObject> E[] release(E[] var0) {
      int var1 = 0;

      for(int var2 = var0.length; var1 < var2; ++var1) {
         Pool.tryRelease(var0[var1]);
      }

      return null;
   }

   static <E extends IPooledObject> E[] tryReleaseAndBlank(E[] var0) {
      return var0 != null ? releaseAndBlank(var0) : null;
   }

   static <E extends IPooledObject> E[] releaseAndBlank(E[] var0) {
      int var1 = 0;

      for(int var2 = var0.length; var1 < var2; ++var1) {
         var0[var1] = Pool.tryRelease(var0[var1]);
      }

      return null;
   }

   static void release(List<? extends IPooledObject> var0) {
      int var1 = 0;

      for(int var2 = var0.size(); var1 < var2; ++var1) {
         Pool.tryRelease((IPooledObject)var0.get(var1));
      }

      var0.clear();
   }
}
