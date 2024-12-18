package zombie.core.skinnedmodel.animation;

import zombie.util.Pool;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;

public class AnimatorsBoneTransform extends TwistableBoneTransform {
   private float m_timeDelta = -1.0F;
   private final TwistableBoneTransform m_previousTransform = new TwistableBoneTransform();
   private static final Pool<AnimatorsBoneTransform> s_pool = new Pool(AnimatorsBoneTransform::new);

   public AnimatorsBoneTransform() {
   }

   public void set(BoneTransform var1) {
      super.set(var1);
      AnimatorsBoneTransform var2 = (AnimatorsBoneTransform)Type.tryCastTo(var1, AnimatorsBoneTransform.class);
      if (var2 != null) {
         this.m_timeDelta = var2.m_timeDelta;
         this.m_previousTransform.set(var2.m_previousTransform);
      }

   }

   public void reset() {
      super.reset();
      this.m_timeDelta = -1.0F;
      this.m_previousTransform.reset();
   }

   public <T extends BoneTransform> T getPreviousTransform(T var1) {
      var1.set((BoneTransform)this.m_previousTransform);
      return var1;
   }

   public float getTimeDelta() {
      return this.m_timeDelta;
   }

   public void nextFrame(float var1) {
      this.m_timeDelta = var1;
      this.m_previousTransform.set(this);
   }

   public static AnimatorsBoneTransform alloc() {
      return (AnimatorsBoneTransform)s_pool.alloc();
   }

   public static TwistableBoneTransform[] allocArray(int var0) {
      AnimatorsBoneTransform[] var1 = new AnimatorsBoneTransform[var0];
      PZArrayUtil.arrayPopulate(var1, AnimatorsBoneTransform::alloc);
      return var1;
   }
}
