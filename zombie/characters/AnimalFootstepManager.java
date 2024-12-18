package zombie.characters;

import zombie.characters.animals.IsoAnimal;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;

public final class AnimalFootstepManager extends BaseAnimalSoundManager {
   public static final AnimalFootstepManager instance = new AnimalFootstepManager();

   public AnimalFootstepManager() {
      super(20, 500);
   }

   public void playSound(IsoAnimal var1) {
      LogSeverity var2 = DebugLog.Sound.getLogSeverity();
      DebugLog.Sound.setLogSeverity(LogSeverity.General);

      try {
         var1.playNextFootstepSound();
      } finally {
         DebugLog.Sound.setLogSeverity(var2);
      }

   }

   public void postUpdate() {
   }
}
