package zombie.characters.animals;

import java.util.HashMap;
import zombie.SandboxOptions;
import zombie.util.StringUtils;

public final class AnimalSoundState {
   private final IsoAnimal animal;
   private String desiredSoundName = null;
   private int desiredSoundPriority = 0;
   private String playingSoundName = null;
   private long eventInstance = 0L;
   private int priority = 0;
   private final HashMap<String, Long> lastPlayedTimeMS = new HashMap();
   private final HashMap<String, Long> intervalExpireTime = new HashMap();

   public AnimalSoundState(IsoAnimal var1) {
      this.animal = var1;
   }

   public IsoAnimal getAnimal() {
      return this.animal;
   }

   public long getEventInstance() {
      return this.eventInstance;
   }

   public long getLastPlayedTimeMS(String var1) {
      return (Long)this.lastPlayedTimeMS.getOrDefault(var1, 0L);
   }

   public int getPriority() {
      return this.priority;
   }

   public void setDesiredSoundPriority(int var1) {
      this.desiredSoundPriority = var1;
   }

   public int getDesiredSoundPriority() {
      return this.desiredSoundPriority;
   }

   public boolean shouldPlay() {
      if (this.desiredSoundName == null) {
         return false;
      } else if (this.isPlayingDesiredSound()) {
         return true;
      } else if (this.getIntervalExpireTime(this.desiredSoundName) == 0L) {
         return true;
      } else {
         return System.currentTimeMillis() >= this.getIntervalExpireTime(this.desiredSoundName);
      }
   }

   public void setDesiredSoundName(String var1) {
      this.desiredSoundName = StringUtils.discardNullOrWhitespace(var1);
   }

   public String getDesiredSoundName() {
      return this.desiredSoundName;
   }

   public void setIntervalExpireTime(String var1, long var2) {
      this.intervalExpireTime.put(var1, var2);
   }

   public long getIntervalExpireTime(String var1) {
      return (Long)this.intervalExpireTime.getOrDefault(var1, 0L);
   }

   public long start(String var1, int var2) {
      this.stop();
      if (StringUtils.isNullOrEmpty(var1)) {
         return 0L;
      } else {
         this.playingSoundName = var1;
         this.eventInstance = this.animal.getEmitter().playVocals(var1);
         if (SandboxOptions.instance.AnimalSoundAttractZombies.getValue() && this.animal.adef != null && this.animal.adef.idleSoundRadius > 0.0F && this.animal.adef.idleSoundVolume > 0.0F) {
            this.animal.addWorldSoundUnlessInvisible((int)this.animal.adef.idleSoundRadius, (int)this.animal.adef.idleSoundVolume, false);
         }

         this.lastPlayedTimeMS.put(var1, System.currentTimeMillis());
         this.priority = var2;
         return this.eventInstance;
      }
   }

   public void stop() {
      if (!this.isPlaying()) {
         this.playingSoundName = null;
         this.eventInstance = 0L;
         this.priority = 0;
      } else {
         this.animal.getEmitter().stopOrTriggerSoundLocal(this.eventInstance);
         this.eventInstance = 0L;
         this.playingSoundName = null;
         this.priority = 0;
      }
   }

   public boolean isPlaying() {
      return this.eventInstance != 0L && this.animal.getEmitter().isPlaying(this.eventInstance);
   }

   public boolean isPlayingDesiredSound() {
      if (this.desiredSoundName == null) {
         return false;
      } else {
         return !this.isPlaying() ? false : StringUtils.equals(this.playingSoundName, this.desiredSoundName);
      }
   }

   public boolean isPlaying(String var1) {
      return this.isPlaying() && var1 != null && StringUtils.equalsIgnoreCase(var1, this.playingSoundName);
   }
}
