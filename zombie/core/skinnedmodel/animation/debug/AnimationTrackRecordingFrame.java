package zombie.core.skinnedmodel.animation.debug;

import java.util.List;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public final class AnimationTrackRecordingFrame extends GenericNameWeightRecordingFrame {
   public AnimationTrackRecordingFrame(String var1) {
      super(var1);
   }

   public void reset() {
      super.reset();
   }

   public void logAnimWeights(List<AnimationTrack> var1, int[] var2, float[] var3) {
      for(int var4 = 0; var4 < var2.length; ++var4) {
         int var5 = var2[var4];
         if (var5 < 0) {
            break;
         }

         float var6 = var3[var4];
         AnimationTrack var7 = (AnimationTrack)var1.get(var5);
         String var8 = var7.getName();
         int var9 = var7.getLayerIdx();
         this.logWeight(var8, var9, var6);
      }

   }
}
