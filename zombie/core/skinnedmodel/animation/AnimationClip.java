package zombie.core.skinnedmodel.animation;

import java.util.ArrayList;
import java.util.List;
import zombie.util.list.PZArrayUtil;

public final class AnimationClip {
   public final String Name;
   public final boolean IsRagdoll;
   public final boolean KeepLastFrame;
   private float m_duration;
   public StaticAnimation staticClip;
   private final KeyframeByBoneIndexElement[] m_KeyFramesByBoneIndex;
   private final List<Keyframe> m_rootMotionKeyframes;
   private final Keyframe[] KeyframeArray;

   public AnimationClip(float var1, List<Keyframe> var2, String var3, boolean var4) {
      this(var1, var2, var3, var4, false);
   }

   public AnimationClip(float var1, List<Keyframe> var2, String var3, boolean var4, boolean var5) {
      this.m_rootMotionKeyframes = new ArrayList();
      this.Name = var3;
      this.IsRagdoll = var5;
      this.m_duration = var1;
      this.KeepLastFrame = var4;
      this.KeyframeArray = (Keyframe[])var2.toArray(new Keyframe[0]);
      this.m_KeyFramesByBoneIndex = new KeyframeByBoneIndexElement[60];
      this.recalculateKeyframesByBoneIndex();
   }

   public Keyframe getKeyframe(int var1) {
      return this.KeyframeArray[var1];
   }

   public Keyframe[] getBoneFramesAt(int var1) {
      return this.m_KeyFramesByBoneIndex[var1].m_keyframes;
   }

   public int getRootMotionFrameCount() {
      return this.m_rootMotionKeyframes.size();
   }

   public Keyframe getRootMotionFrameAt(int var1) {
      return (Keyframe)this.m_rootMotionKeyframes.get(var1);
   }

   public Keyframe[] getKeyframes() {
      return this.KeyframeArray;
   }

   public float getDuration() {
      return this.m_duration;
   }

   private KeyframeByBoneIndexElement getKeyframesForBone(int var1) {
      return this.m_KeyFramesByBoneIndex[var1];
   }

   public Keyframe[] getKeyframesForBone(int var1, Keyframe[] var2) {
      KeyframeByBoneIndexElement var3 = this.getKeyframesForBone(var1);
      int var4 = var3.m_keyframes.length;
      if (PZArrayUtil.lengthOf(var2) < var4) {
         var2 = (Keyframe[])PZArrayUtil.newInstance(Keyframe.class, var2, var4, false, Keyframe::new);
      }

      PZArrayUtil.arrayCopy((Object[])var2, (Object[])var3.m_keyframes);
      return var2;
   }

   public float getTranslationLength(BoneAxis var1) {
      float var3 = this.KeyframeArray[this.KeyframeArray.length - 1].Position.x - this.KeyframeArray[0].Position.x;
      float var2;
      if (var1 == BoneAxis.Y) {
         var2 = -this.KeyframeArray[this.KeyframeArray.length - 1].Position.z + this.KeyframeArray[0].Position.z;
      } else {
         var2 = this.KeyframeArray[this.KeyframeArray.length - 1].Position.y - this.KeyframeArray[0].Position.y;
      }

      return (float)Math.sqrt((double)(var3 * var3 + var2 * var2));
   }

   public void recalculateKeyframesByBoneIndex() {
      ArrayList var1 = new ArrayList();
      int var2 = this.KeyframeArray.length > 1 ? this.KeyframeArray.length - (this.KeepLastFrame ? 0 : 1) : 1;

      for(int var3 = 0; var3 < 60; ++var3) {
         var1.clear();

         for(int var4 = 0; var4 < var2; ++var4) {
            Keyframe var5 = this.KeyframeArray[var4];
            if (var5.Bone == var3) {
               var1.add(var5);
            }
         }

         this.m_KeyFramesByBoneIndex[var3] = new KeyframeByBoneIndexElement(var1);
      }

   }

   private static class KeyframeByBoneIndexElement {
      final Keyframe[] m_keyframes;

      KeyframeByBoneIndexElement(List<Keyframe> var1) {
         this.m_keyframes = (Keyframe[])var1.toArray(new Keyframe[0]);
      }
   }
}
