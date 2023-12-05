package zombie.util.lambda;

import zombie.util.Pool;
import zombie.util.PooledObject;

public final class ReturnValueContainer<T> extends PooledObject {
   public T ReturnVal;
   private static final Pool<ReturnValueContainer<Object>> s_pool = new Pool(ReturnValueContainer::new);

   public ReturnValueContainer() {
   }

   public void onReleased() {
      this.ReturnVal = null;
   }

   public static <E> ReturnValueContainer<E> alloc() {
      return (ReturnValueContainer)s_pool.alloc();
   }
}
