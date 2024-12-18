package zombie.util;

import gnu.trove.set.hash.THashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import zombie.util.list.PZArrayUtil;

public final class Pool<PO extends IPooledObject> {
   private final Supplier<PO> m_allocator;
   private final ThreadLocal<PoolStacks> m_stacks = ThreadLocal.withInitial(PoolStacks::new);

   public ThreadLocal<PoolStacks> getPoolStacks() {
      return this.m_stacks;
   }

   public Pool(Supplier<PO> var1) {
      this.m_allocator = var1;
   }

   public PO alloc() {
      Supplier var1 = this.m_allocator;
      PoolStacks var2 = (PoolStacks)this.m_stacks.get();
      synchronized(var2.lock) {
         return this.allocInternal(var2, var1);
      }
   }

   public void release(IPooledObject var1) {
      PoolReference var2 = var1.getPoolReference();
      Pool var3 = var2.getPool();
      PoolStacks var4 = var2.getPoolStacks();
      synchronized(var4.lock) {
         this.releaseItemInternal(var1, var4, var3);
      }
   }

   private PO allocInternal(PoolStacks var1, Supplier<PO> var2) {
      THashSet var3 = var1.inUse;
      List var4 = var1.released;
      IPooledObject var5;
      if (!var4.isEmpty()) {
         var5 = (IPooledObject)var4.remove(var4.size() - 1);
      } else {
         var5 = (IPooledObject)var2.get();
         if (var5 == null) {
            throw new NullPointerException("Allocator returned a nullPtr. This is not allowed.");
         }

         var5.setPool(new PoolReference(this, var1));
      }

      var5.setFree(false);
      var3.add(var5);
      return var5;
   }

   private void releaseItemInternal(IPooledObject var1, PoolStacks var2, Pool<IPooledObject> var3) {
      THashSet var4 = var2.inUse;
      List var5 = var2.released;
      if (var3 != this) {
         throw new UnsupportedOperationException("Cannot release item. Not owned by this pool.");
      } else if (var1.isFree()) {
         throw new UnsupportedOperationException("Cannot release item. Already released.");
      } else if (!var4.remove(var1)) {
         throw new UnsupportedOperationException("Attempting to release PooledObject not in Pool, possibly releasing on different thread than alloc. " + var1);
      } else {
         var1.setFree(true);
         var5.add(var1);
         var1.onReleased();
      }
   }

   public static <E> E tryRelease(E var0) {
      IPooledObject var1 = (IPooledObject)Type.tryCastTo(var0, IPooledObject.class);
      if (var1 != null && !var1.isFree()) {
         var1.release();
      }

      return null;
   }

   public static <E extends IPooledObject> E tryRelease(E var0) {
      if (var0 != null && !var0.isFree()) {
         var0.release();
      }

      return null;
   }

   public static <E extends IPooledObject> E[] tryRelease(E[] var0) {
      PZArrayUtil.forEach((Object[])var0, Pool::tryRelease);
      return null;
   }

   public static final class PoolStacks {
      final THashSet<IPooledObject> inUse = new THashSet();
      final List<IPooledObject> released = new ArrayList();
      final Object lock = new String("PoolStacks Thread Lock");

      PoolStacks() {
         this.inUse.setAutoCompactionFactor(0.0F);
      }

      public THashSet<IPooledObject> getInUse() {
         return this.inUse;
      }

      public List<IPooledObject> getReleased() {
         return this.released;
      }
   }

   public static final class PoolReference {
      final Pool<IPooledObject> m_pool;
      final PoolStacks m_poolStacks;

      private PoolReference(Pool<IPooledObject> var1, PoolStacks var2) {
         this.m_pool = var1;
         this.m_poolStacks = var2;
      }

      public Pool<IPooledObject> getPool() {
         return this.m_pool;
      }

      private PoolStacks getPoolStacks() {
         return this.m_poolStacks;
      }

      public void release(IPooledObject var1) {
         this.m_pool.release(var1);
      }
   }
}
