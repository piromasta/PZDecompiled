package zombie.util;

public abstract class PooledObject implements IPooledObject {
   private boolean m_isFree = true;
   private Pool.PoolReference m_pool;

   public PooledObject() {
   }

   public final Pool.PoolReference getPoolReference() {
      return this.m_pool;
   }

   public final synchronized void setPool(Pool.PoolReference var1) {
      this.m_pool = var1;
   }

   public final synchronized void release() {
      if (this.m_pool != null) {
         synchronized(this.m_pool.m_pool) {
            this.m_pool.release(this);
         }
      } else {
         this.onReleased();
      }

   }

   public final synchronized boolean isFree() {
      return this.m_isFree;
   }

   public final synchronized void setFree(boolean var1) {
      this.m_isFree = var1;
   }
}
