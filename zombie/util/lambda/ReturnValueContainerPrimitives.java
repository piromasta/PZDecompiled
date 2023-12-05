package zombie.util.lambda;

import zombie.util.Pool;
import zombie.util.PooledObject;

public final class ReturnValueContainerPrimitives {
   public ReturnValueContainerPrimitives() {
   }

   public static final class RVInt extends PooledObject {
      public int ReturnVal;
      private static final Pool<RVInt> s_pool = new Pool(RVInt::new);

      public RVInt() {
      }

      public void onReleased() {
         this.ReturnVal = 0;
      }

      public static RVInt alloc() {
         return (RVInt)s_pool.alloc();
      }
   }

   public static final class RVFloat extends PooledObject {
      public float ReturnVal;
      private static final Pool<RVFloat> s_pool = new Pool(RVFloat::new);

      public RVFloat() {
      }

      public void onReleased() {
         this.ReturnVal = 0.0F;
      }

      public static RVFloat alloc() {
         return (RVFloat)s_pool.alloc();
      }
   }

   public static final class RVBoolean extends PooledObject {
      public boolean ReturnVal;
      private static final Pool<RVBoolean> s_pool = new Pool(RVBoolean::new);

      public RVBoolean() {
      }

      public void onReleased() {
         this.ReturnVal = false;
      }

      public static RVBoolean alloc() {
         return (RVBoolean)s_pool.alloc();
      }
   }
}
