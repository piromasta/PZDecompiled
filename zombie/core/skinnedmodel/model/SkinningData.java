package zombie.core.skinnedmodel.model;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;
import zombie.core.skinnedmodel.animation.AnimationClip;

public final class SkinningData {
   public final Buffers buffers;
   public HashMap<String, AnimationClip> AnimationClips;
   public List<Matrix4f> BindPose;
   public List<Matrix4f> InverseBindPose;
   public List<Matrix4f> BoneOffset = new ArrayList();
   public List<Integer> SkeletonHierarchy;
   public HashMap<String, Integer> BoneIndices;
   private volatile boolean m_boneHierachyValid = false;
   private SkinningBoneHierarchy m_boneHieararchy = null;
   private final Object m_boneHiearchyLock = new String("SkinningBoneHiearchy Lock. Prevent chickens from partially T-posing. Because AnimationPlayer.checkBoneMap was reading while buildBoneHiearchy was writing. :D");
   private SkinningBoneHierarchy m_skeletonBoneHieararchy = null;
   private String m_skeletonBoneName = "Dummy01";

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
      this.buffers = null;
   }

   private void validateBoneHierarchy() {
      if (!this.m_boneHierachyValid) {
         synchronized(this.m_boneHiearchyLock) {
            if (!this.m_boneHierachyValid) {
               this.m_boneHieararchy = new SkinningBoneHierarchy();
               this.m_boneHieararchy.buildBoneHiearchy(this);
               this.m_boneHierachyValid = true;
            }

         }
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

   public SkinningBoneHierarchy getSkeletonBoneHiearchy() {
      if (this.m_skeletonBoneHieararchy == null) {
         this.m_skeletonBoneHieararchy = this.getBoneHieararchy().getSubHierarchy(this.m_skeletonBoneName);
      }

      return this.m_skeletonBoneHieararchy;
   }

   public static final class Buffers {
      public FloatBuffer boneMatrices;
      public FloatBuffer boneWeights;
      public ShortBuffer boneIDs;

      public Buffers(List<Matrix4f> var1, float[] var2, List<Integer> var3) {
         this.boneMatrices = BufferUtils.createFloatBuffer(var1.size() * 16);

         int var4;
         for(var4 = 0; var4 < var1.size(); ++var4) {
            Matrix4f var5 = (Matrix4f)var1.get(var4);
            var5.store(this.boneMatrices);
         }

         this.boneWeights = BufferUtils.createFloatBuffer(var2.length);
         this.boneWeights.put(var2);
         this.boneIDs = BufferUtils.createShortBuffer(var3.size());

         for(var4 = 0; var4 < var3.size(); ++var4) {
            this.boneIDs.put(((Integer)var3.get(var4)).shortValue());
         }

      }
   }
}
