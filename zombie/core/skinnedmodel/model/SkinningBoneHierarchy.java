package zombie.core.skinnedmodel.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;
import zombie.util.Lambda;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class SkinningBoneHierarchy {
   private boolean m_boneHieararchyValid = false;
   private SkinningBone[] m_allBones = null;
   private SkinningBone[] m_rootBones = null;

   public SkinningBoneHierarchy() {
   }

   public boolean isValid() {
      return this.m_boneHieararchyValid;
   }

   public void buildBoneHiearchy(SkinningData var1) {
      this.m_rootBones = new SkinningBone[0];
      this.m_allBones = new SkinningBone[var1.numBones()];
      PZArrayUtil.arrayPopulate(this.m_allBones, SkinningBone::new);
      Iterator var2 = var1.BoneIndices.entrySet().iterator();

      int var4;
      while(var2.hasNext()) {
         Map.Entry var3 = (Map.Entry)var2.next();
         var4 = (Integer)var3.getValue();
         String var5 = (String)var3.getKey();
         SkinningBone var6 = this.m_allBones[var4];
         var6.Index = var4;
         var6.Name = var5;
         var6.SkeletonBone = (SkeletonBone)StringUtils.tryParseEnum(SkeletonBone.class, var5, SkeletonBone.None);
         var6.Children = new SkinningBone[0];
         if (var6.SkeletonBone == SkeletonBone.None) {
         }
      }

      for(int var7 = 0; var7 < var1.numBones(); ++var7) {
         SkinningBone var8 = this.m_allBones[var7];
         var4 = var1.getParentBoneIdx(var7);
         if (var4 > -1) {
            var8.Parent = this.m_allBones[var4];
            var8.Parent.Children = (SkinningBone[])PZArrayUtil.add(var8.Parent.Children, var8);
         } else {
            this.m_rootBones = (SkinningBone[])PZArrayUtil.add(this.m_rootBones, var8);
         }
      }

      this.m_boneHieararchyValid = true;
   }

   public int numRootBones() {
      return this.m_rootBones.length;
   }

   public SkinningBone getBoneAt(int var1) {
      return this.m_allBones[var1];
   }

   public SkinningBone getBone(SkeletonBone var1) {
      return this.getBone(Lambda.predicate(var1, (var0, var1x) -> {
         return var0.SkeletonBone == var1x;
      }));
   }

   public SkinningBone getBone(String var1) {
      return this.getBone(Lambda.predicate(var1, (var0, var1x) -> {
         return StringUtils.equalsIgnoreCase(var0.Name, var1x);
      }));
   }

   public SkinningBone getBone(Predicate<SkinningBone> var1) {
      return (SkinningBone)PZArrayUtil.find((Object[])this.m_allBones, var1);
   }

   public SkinningBone getRootBoneAt(int var1) {
      return this.m_rootBones[var1];
   }

   public SkinningBoneHierarchy getSubHierarchy(String var1) {
      SkinningBone var2 = this.getBone(var1);
      return getSubHierarchy(var2);
   }

   public SkinningBoneHierarchy getSubHierarchy(int var1) {
      SkinningBone var2 = this.getBoneAt(var1);
      return getSubHierarchy(var2);
   }

   public static SkinningBoneHierarchy getSubHierarchy(SkinningBone var0) {
      if (var0 == null) {
         return null;
      } else {
         ArrayList var1 = new ArrayList();
         populateSubHierarchy(var0, var1);
         SkinningBoneHierarchy var2 = new SkinningBoneHierarchy();
         var2.m_allBones = (SkinningBone[])var1.toArray(new SkinningBone[0]);
         var2.m_rootBones = new SkinningBone[]{var0.toRoot()};
         var2.m_boneHieararchyValid = true;
         return var2;
      }
   }

   private static void populateSubHierarchy(SkinningBone var0, ArrayList<SkinningBone> var1) {
      var1.add(var0);
      SkinningBone[] var2 = var0.Children;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         SkinningBone var5 = var2[var4];
         populateSubHierarchy(var5, var1);
      }

   }

   public int numBones() {
      return PZArrayUtil.lengthOf(this.m_allBones);
   }
}
