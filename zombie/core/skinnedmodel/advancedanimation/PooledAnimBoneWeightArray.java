package zombie.core.skinnedmodel.advancedanimation;

import java.util.List;
import zombie.util.Pool;
import zombie.util.PooledArrayObject;
import zombie.util.list.PZArrayUtil;

public class PooledAnimBoneWeightArray extends PooledArrayObject<AnimBoneWeight> {
   private static final PooledAnimBoneWeightArray s_empty = new PooledAnimBoneWeightArray();
   private static final Pool<PooledAnimBoneWeightArray> s_pool = new Pool(PooledAnimBoneWeightArray::new);

   public PooledAnimBoneWeightArray() {
   }

   public static PooledAnimBoneWeightArray alloc(int var0) {
      if (var0 == 0) {
         return s_empty;
      } else {
         PooledAnimBoneWeightArray var1 = (PooledAnimBoneWeightArray)s_pool.alloc();
         var1.initCapacity(var0, (var0x) -> {
            return new AnimBoneWeight[var0x];
         });
         return var1;
      }
   }

   public static PooledAnimBoneWeightArray toArray(List<AnimBoneWeight> var0) {
      if (var0 == null) {
         return null;
      } else {
         PooledAnimBoneWeightArray var1 = alloc(var0.size());
         PZArrayUtil.arrayCopy((Object[])((AnimBoneWeight[])var1.array()), (List)var0);
         return var1;
      }
   }

   public static PooledAnimBoneWeightArray toArray(PooledArrayObject<AnimBoneWeight> var0) {
      if (var0 == null) {
         return null;
      } else {
         PooledAnimBoneWeightArray var1 = alloc(var0.length());
         PZArrayUtil.arrayCopy((Object[])((AnimBoneWeight[])var1.array()), (Object[])((AnimBoneWeight[])var0.array()));
         return var1;
      }
   }
}
