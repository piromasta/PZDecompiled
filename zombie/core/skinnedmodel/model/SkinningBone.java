package zombie.core.skinnedmodel.model;

import java.util.function.Consumer;
import zombie.util.list.PZArrayUtil;

public final class SkinningBone {
   public SkinningBone Parent;
   public String Name;
   public int Index;
   public SkinningBone[] Children;
   public SkeletonBone SkeletonBone;

   public SkinningBone() {
      this.SkeletonBone = zombie.core.skinnedmodel.model.SkeletonBone.None;
   }

   public void forEachDescendant(Consumer<SkinningBone> var1) {
      forEachDescendant(this, var1);
   }

   private static void forEachDescendant(SkinningBone var0, Consumer<SkinningBone> var1) {
      if (var0.Children != null && var0.Children.length != 0) {
         SkinningBone[] var2 = var0.Children;
         int var3 = var2.length;

         int var4;
         SkinningBone var5;
         for(var4 = 0; var4 < var3; ++var4) {
            var5 = var2[var4];
            var1.accept(var5);
         }

         var2 = var0.Children;
         var3 = var2.length;

         for(var4 = 0; var4 < var3; ++var4) {
            var5 = var2[var4];
            forEachDescendant(var5, var1);
         }

      }
   }

   public String toString() {
      String var10000 = this.getClass().getName();
      return var10000 + "{ Name:\"" + this.Name + "\", Index:" + this.Index + ", SkeletonBone:" + this.SkeletonBone + ",}";
   }

   public int getParentBoneIndex() {
      return this.Parent != null ? this.Parent.Index : -1;
   }

   public SkeletonBone getParentSkeletonBone() {
      return this.Parent != null ? this.Parent.SkeletonBone : zombie.core.skinnedmodel.model.SkeletonBone.None;
   }

   public SkinningBone toRoot() {
      if (this.Parent == null) {
         return this;
      } else {
         SkinningBone var1 = new SkinningBone();
         var1.Name = this.Name;
         var1.Index = this.Index;
         var1.SkeletonBone = this.SkeletonBone;
         var1.Children = (SkinningBone[])PZArrayUtil.shallowClone(this.Children);
         var1.Parent = null;
         return var1;
      }
   }

   public boolean isRoot() {
      return this.Parent == null;
   }
}
