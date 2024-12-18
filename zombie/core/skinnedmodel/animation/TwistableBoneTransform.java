package zombie.core.skinnedmodel.animation;

import zombie.util.Pool;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;

public class TwistableBoneTransform extends BoneTransform {
   public float BlendWeight = 0.0F;
   public float Twist = 0.0F;
   private static final Pool<TwistableBoneTransform> s_pool = new Pool(TwistableBoneTransform::new);

   protected TwistableBoneTransform() {
   }

   public void reset() {
      super.reset();
      this.BlendWeight = 0.0F;
      this.Twist = 0.0F;
   }

   public void set(BoneTransform var1) {
      super.set(var1);
      TwistableBoneTransform var2 = (TwistableBoneTransform)Type.tryCastTo(var1, TwistableBoneTransform.class);
      if (var2 != null) {
         this.BlendWeight = var2.BlendWeight;
         this.Twist = var2.Twist;
      }

   }

   public static TwistableBoneTransform alloc() {
      return (TwistableBoneTransform)s_pool.alloc();
   }

   public static TwistableBoneTransform[] allocArray(int var0) {
      TwistableBoneTransform[] var1 = new TwistableBoneTransform[var0];
      PZArrayUtil.arrayPopulate(var1, TwistableBoneTransform::alloc);
      return var1;
   }
}
