package zombie.core.skinnedmodel.animation;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.util.vector.Matrix4f;

public final class AnimationMultiTrack {
   private final ArrayList<AnimationTrack> m_tracks = new ArrayList();
   private static final ArrayList<AnimationTrack> tempTracks = new ArrayList();

   public AnimationMultiTrack() {
   }

   public AnimationTrack findTrack(String var1) {
      int var2 = 0;

      for(int var3 = this.m_tracks.size(); var2 < var3; ++var2) {
         AnimationTrack var4 = (AnimationTrack)this.m_tracks.get(var2);
         if (var4.getName().equals(var1)) {
            return var4;
         }
      }

      return null;
   }

   public void addTrack(AnimationTrack var1) {
      this.m_tracks.add(var1);
   }

   public void removeTrack(AnimationTrack var1) {
      int var2 = this.getIndexOfTrack(var1);
      if (var2 > -1) {
         this.removeTrackAt(var2);
      }

   }

   public void removeTracks(List<AnimationTrack> var1) {
      tempTracks.clear();
      tempTracks.addAll(var1);

      for(int var2 = 0; var2 < tempTracks.size(); ++var2) {
         this.removeTrack((AnimationTrack)tempTracks.get(var2));
      }

   }

   public void removeTrackAt(int var1) {
      ((AnimationTrack)this.m_tracks.remove(var1)).release();
   }

   public int getIndexOfTrack(AnimationTrack var1) {
      if (var1 == null) {
         return -1;
      } else {
         int var2 = -1;

         for(int var3 = 0; var3 < this.m_tracks.size(); ++var3) {
            AnimationTrack var4 = (AnimationTrack)this.m_tracks.get(var3);
            if (var4 == var1) {
               var2 = var3;
               break;
            }
         }

         return var2;
      }
   }

   public void Update(float var1) {
      for(int var2 = 0; var2 < this.m_tracks.size(); ++var2) {
         AnimationTrack var3 = (AnimationTrack)this.m_tracks.get(var2);
         var3.Update(var1);
         if (var3.CurrentClip == null) {
            this.removeTrackAt(var2);
            --var2;
         }
      }

   }

   public float getDuration() {
      float var1 = 0.0F;

      for(int var2 = 0; var2 < this.m_tracks.size(); ++var2) {
         AnimationTrack var3 = (AnimationTrack)this.m_tracks.get(var2);
         float var4 = var3.getDuration();
         if (var3.CurrentClip != null && var4 > var1) {
            var1 = var4;
         }
      }

      return var1;
   }

   public void reset() {
      int var1 = 0;

      for(int var2 = this.m_tracks.size(); var1 < var2; ++var1) {
         AnimationTrack var3 = (AnimationTrack)this.m_tracks.get(var1);
         var3.reset();
      }

      AnimationPlayer.releaseTracks(this.m_tracks);
      this.m_tracks.clear();
   }

   public List<AnimationTrack> getTracks() {
      return this.m_tracks;
   }

   public int getTrackCount() {
      return this.m_tracks.size();
   }

   public AnimationTrack getTrackAt(int var1) {
      return (AnimationTrack)this.m_tracks.get(var1);
   }

   public boolean containsAnyRagdollTracks() {
      List var1 = this.getTracks();

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         AnimationTrack var3 = (AnimationTrack)var1.get(var2);
         if (var3.isRagdoll()) {
            return true;
         }
      }

      return false;
   }

   public boolean anyRagdollFirstFrame() {
      List var1 = this.getTracks();

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         AnimationTrack var3 = (AnimationTrack)var1.get(var2);
         if (var3.isRagdollFirstFrame()) {
            return true;
         }
      }

      return false;
   }

   public void initRagdollTransforms(TwistableBoneTransform[] var1) {
      List var2 = this.getTracks();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         AnimationTrack var4 = (AnimationTrack)var2.get(var3);
         if (var4.isRagdollFirstFrame()) {
            var4.initRagdollTransforms(var1);
         }
      }

   }

   public void initRagdollTransforms(List<Matrix4f> var1) {
      List var2 = this.getTracks();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         AnimationTrack var4 = (AnimationTrack)var2.get(var3);
         if (var4.isRagdollFirstFrame()) {
            var4.initRagdollTransforms(var1);
         }
      }

   }

   public AnimationTrack getActiveRagdollTrack() {
      AnimationTrack var1 = null;
      List var2 = this.getTracks();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         AnimationTrack var4 = (AnimationTrack)var2.get(var3);
         if (var4.isRagdoll() && var4.IsPlaying && !(var4.BlendDelta <= 0.0F) && (var1 == null || var1.BlendDelta > var4.BlendDelta)) {
            var1 = var4;
         }
      }

      return var1;
   }

   public float getIKAimingLeftArmWeight() {
      float var1 = 0.0F;
      List var2 = this.getTracks();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         AnimationTrack var4 = (AnimationTrack)var2.get(var3);
         if (var4.isIKAimingLeftArm()) {
            var1 += var4.BlendDelta;
            if (var1 > 1.0F) {
               var1 = 1.0F;
               break;
            }
         }
      }

      return var1;
   }

   public float getIKAimingRightArmWeight() {
      float var1 = 0.0F;
      List var2 = this.getTracks();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         AnimationTrack var4 = (AnimationTrack)var2.get(var3);
         if (var4.isIKAimingRightArm()) {
            var1 += var4.BlendDelta;
            if (var1 > 1.0F) {
               var1 = 1.0F;
               break;
            }
         }
      }

      return var1;
   }
}
