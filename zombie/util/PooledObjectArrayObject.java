package zombie.util;

public final class PooledObjectArrayObject<T extends IPooledObject> extends PooledArrayObject<T> {
   public PooledObjectArrayObject() {
   }

   public void onReleased() {
      int var1 = 0;

      for(int var2 = this.length(); var1 < var2; ++var1) {
         ((IPooledObject)this.get(var1)).release();
      }

   }
}
