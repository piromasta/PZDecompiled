package zombie.audio;

import fmod.fmod.FMOD_STUDIO_EVENT_DESCRIPTION;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;

public final class GameSoundClip {
   public static short INIT_FLAG_DISTANCE_MIN = 1;
   public static short INIT_FLAG_DISTANCE_MAX = 2;
   public final GameSound gameSound;
   public String event;
   public FMOD_STUDIO_EVENT_DESCRIPTION eventDescription;
   public FMOD_STUDIO_EVENT_DESCRIPTION eventDescriptionMP;
   public String file;
   public float volume = 1.0F;
   public float pitch = 1.0F;
   public float distanceMin = 10.0F;
   public float distanceMax = 10.0F;
   public float reverbMaxRange = 10.0F;
   public float reverbFactor = 0.0F;
   public int priority = 5;
   public short initFlags = 0;
   public short reloadEpoch;

   public GameSoundClip(GameSound var1) {
      this.gameSound = var1;
      this.reloadEpoch = var1.reloadEpoch;
   }

   public String getEvent() {
      return this.event;
   }

   public String getFile() {
      return this.file;
   }

   public float getVolume() {
      return this.volume;
   }

   public float getPitch() {
      return this.pitch;
   }

   public boolean hasMinDistance() {
      return (this.initFlags & INIT_FLAG_DISTANCE_MIN) != 0;
   }

   public boolean hasMaxDistance() {
      return (this.initFlags & INIT_FLAG_DISTANCE_MAX) != 0;
   }

   public float getMinDistance() {
      return this.distanceMin;
   }

   public float getMaxDistance() {
      return this.distanceMax;
   }

   public float getEffectiveVolume() {
      return this.volume * this.gameSound.getUserVolume();
   }

   public float getEffectiveVolumeInMenu() {
      return this.volume * this.gameSound.getUserVolume();
   }

   public GameSoundClip checkReloaded() {
      if (this.reloadEpoch == this.gameSound.reloadEpoch) {
         return this;
      } else {
         GameSoundClip var1 = null;

         for(int var2 = 0; var2 < this.gameSound.clips.size(); ++var2) {
            GameSoundClip var3 = (GameSoundClip)this.gameSound.clips.get(var2);
            if (var3 == this) {
               return this;
            }

            if (var3.event != null && var3.event.equals(this.event)) {
               var1 = var3;
            }

            if (var3.file != null && var3.file.equals(this.file)) {
               var1 = var3;
            }
         }

         if (var1 == null) {
            this.reloadEpoch = this.gameSound.reloadEpoch;
            return this;
         } else {
            return var1;
         }
      }
   }

   public boolean hasSustainPoints() {
      return this.eventDescription != null && this.eventDescription.bHasSustainPoints;
   }

   public boolean hasParameter(FMOD_STUDIO_PARAMETER_DESCRIPTION var1) {
      return this.eventDescription != null && this.eventDescription.hasParameter(var1);
   }
}
