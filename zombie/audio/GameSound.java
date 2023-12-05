package zombie.audio;

import fmod.fmod.FMODManager;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import java.util.ArrayList;
import zombie.SystemDisabler;
import zombie.core.Rand;

public final class GameSound {
   public String name;
   public String category = "General";
   public boolean loop = false;
   public boolean is3D = true;
   public final ArrayList<GameSoundClip> clips = new ArrayList();
   private float userVolume = 1.0F;
   public MasterVolume master;
   public int maxInstancesPerEmitter;
   public short reloadEpoch;

   public GameSound() {
      this.master = GameSound.MasterVolume.Primary;
      this.maxInstancesPerEmitter = -1;
   }

   public String getName() {
      return this.name;
   }

   public String getCategory() {
      return this.category;
   }

   public boolean isLooped() {
      return this.loop;
   }

   public void setUserVolume(float var1) {
      this.userVolume = Math.max(0.0F, Math.min(2.0F, var1));
   }

   public float getUserVolume() {
      return !SystemDisabler.getEnableAdvancedSoundOptions() ? 1.0F : this.userVolume;
   }

   public GameSoundClip getRandomClip() {
      return (GameSoundClip)this.clips.get(Rand.Next(this.clips.size()));
   }

   public String getMasterName() {
      return this.master.name();
   }

   public int numClipsUsingParameter(String var1) {
      FMOD_STUDIO_PARAMETER_DESCRIPTION var2 = FMODManager.instance.getParameterDescription(var1);
      if (var2 == null) {
         return 0;
      } else {
         int var3 = 0;

         for(int var4 = 0; var4 < this.clips.size(); ++var4) {
            GameSoundClip var5 = (GameSoundClip)this.clips.get(var4);
            if (var5.hasParameter(var2)) {
               ++var3;
            }
         }

         return var3;
      }
   }

   public void reset() {
      this.name = null;
      this.category = "General";
      this.loop = false;
      this.is3D = true;
      this.clips.clear();
      this.userVolume = 1.0F;
      this.master = GameSound.MasterVolume.Primary;
      this.maxInstancesPerEmitter = -1;
      ++this.reloadEpoch;
   }

   public static enum MasterVolume {
      Primary,
      Ambient,
      Music,
      VehicleEngine;

      private MasterVolume() {
      }
   }
}
