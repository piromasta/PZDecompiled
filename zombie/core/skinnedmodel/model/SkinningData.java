package zombie.core.skinnedmodel.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.lwjgl.util.vector.Matrix4f;
import zombie.core.skinnedmodel.animation.AnimationClip;

public final class SkinningData {
   public HashMap<String, AnimationClip> AnimationClips;
   public List<Matrix4f> BindPose;
   public List<Matrix4f> InverseBindPose;
   public List<Matrix4f> BoneOffset = new ArrayList();
   public List<Integer> SkeletonHierarchy;
   public HashMap<String, Integer> BoneIndices;
   private SkinningBoneHierarchy m_boneHieararchy = null;

   public SkinningData(HashMap<String, AnimationClip> var1, List<Matrix4f> var2, List<Matrix4f> var3, List<Matrix4f> var4, List<Integer> var5, HashMap<String, Integer> var6) {
      this.AnimationClips = var1;
      this.BindPose = var2;
      this.InverseBindPose = var3;
      this.SkeletonHierarchy = var5;

      for(int var7 = 0; var7 < var5.size(); ++var7) {
         Matrix4f var8 = (Matrix4f)var4.get(var7);
         this.BoneOffset.add(var8);
      }

      this.BoneIndices = var6;
   }

   private void validateBoneHierarchy() {
      if (this.m_boneHieararchy == null) {
         this.m_boneHieararchy = new SkinningBoneHierarchy();
         this.m_boneHieararchy.buildBoneHiearchy(this);
      }

   }

   public int numBones() {
      return this.SkeletonHierarchy.size();
   }

   public int numRootBones() {
      return this.getBoneHieararchy().numRootBones();
   }

   public int getParentBoneIdx(int var1) {
      return (Integer)this.SkeletonHierarchy.get(var1);
   }

   public SkinningBone getBoneAt(int var1) {
      return this.getBoneHieararchy().getBoneAt(var1);
   }

   public SkinningBone getBone(String var1) {
      Integer var2 = (Integer)this.BoneIndices.get(var1);
      return var2 == null ? null : this.getBoneAt(var2);
   }

   public SkinningBone getRootBoneAt(int var1) {
      return this.getBoneHieararchy().getRootBoneAt(var1);
   }

   public SkinningBoneHierarchy getBoneHieararchy() {
      this.validateBoneHierarchy();
      return this.m_boneHieararchy;
   }
}
