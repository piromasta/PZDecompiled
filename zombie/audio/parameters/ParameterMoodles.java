package zombie.audio.parameters;

import fmod.fmod.FMODManager;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import java.util.ArrayList;
import zombie.characters.BaseCharacterSoundEmitter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.Moodles.MoodleType;
import zombie.core.math.PZMath;

public final class ParameterMoodles {
   private final IsoGameCharacter m_character;
   private final ArrayList<ParameterMoodle> m_moodles = new ArrayList();

   public ParameterMoodles(IsoGameCharacter var1) {
      this.m_character = var1;
      this.addMoodle("Bleeding", MoodleType.Bleeding);
      this.addMoodle("Bored", MoodleType.Bored);
      this.addMoodle("Drunk", MoodleType.Drunk);
      this.addMoodle("Endurance", MoodleType.Endurance);
      this.addMoodle("FoodEaten", MoodleType.FoodEaten);
      this.addMoodle("HasACold", MoodleType.HasACold);
      this.addMoodle("HeavyLoad", MoodleType.HeavyLoad);
      this.addMoodle("Hungry", MoodleType.Hungry);
      this.addMoodle("Hyperthermia", MoodleType.Hyperthermia);
      this.addMoodle("Hypothermia", MoodleType.Hypothermia);
      this.addMoodle("Injured", MoodleType.Injured);
      this.addMoodle("Pain", MoodleType.Pain);
      this.addMoodle("Panic", MoodleType.Panic);
      this.addMoodle("Sick", MoodleType.Sick);
      this.addMoodle("Stress", MoodleType.Stress);
      this.addMoodle("Thirst", MoodleType.Thirst);
      this.addMoodle("Tired", MoodleType.Tired);
      this.addMoodle("Unhappy", MoodleType.Unhappy);
      this.addMoodle("Wet", MoodleType.Wet);
      this.addMoodle("Windchill", MoodleType.Windchill);
   }

   private void addMoodle(String var1, MoodleType var2) {
      ParameterMoodle var3 = new ParameterMoodle("Moodle" + var1, var2);
      this.m_moodles.add(var3);
   }

   public void update(long var1) {
      if (var1 != 0L) {
         if (this.m_character.getMoodles() != null) {
            for(int var3 = 0; var3 < this.m_moodles.size(); ++var3) {
               ParameterMoodle var4 = (ParameterMoodle)this.m_moodles.get(var3);
               float var5 = var4.calculateCurrentValue(this.m_character);
               var5 = PZMath.clamp(var5, 0.0F, 1.0F);
               var4.setCurrentValue(this.m_character.getEmitter(), var1, var5);
            }

         }
      }
   }

   public void reset() {
      for(int var1 = 0; var1 < this.m_moodles.size(); ++var1) {
         ParameterMoodle var2 = (ParameterMoodle)this.m_moodles.get(var1);
         var2.reset();
      }

   }

   private static final class ParameterMoodle {
      final String m_parameterName;
      final MoodleType m_moodleType;
      FMOD_STUDIO_PARAMETER_DESCRIPTION m_parameterDescription;
      float m_currentValue = 0.0F / 0.0F;

      ParameterMoodle(String var1, MoodleType var2) {
         this.m_parameterName = var1;
         this.m_moodleType = var2;
         this.m_parameterDescription = FMODManager.instance.getParameterDescription(var1);
      }

      float calculateCurrentValue(IsoGameCharacter var1) {
         return var1.isDead() ? 0.0F : (float)var1.getMoodles().getMoodleLevel(this.m_moodleType) / 4.0F;
      }

      void setCurrentValue(BaseCharacterSoundEmitter var1, long var2, float var4) {
         if (var4 != this.m_currentValue) {
            this.m_currentValue = var4;
            if (this.m_parameterDescription != null) {
               var1.setParameterValue(var2, this.m_parameterDescription, var4);
            }
         }
      }

      void reset() {
         this.m_currentValue = 0.0F / 0.0F;
      }
   }
}
