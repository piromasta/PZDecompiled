package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.characters.IsoPlayer;

public final class ParameterMusicIntensity extends FMODGlobalParameter {
   private int m_playerIndex = -1;
   private Intensity m_intensity;

   public ParameterMusicIntensity() {
      super("MusicIntensity");
      this.m_intensity = ParameterMusicIntensity.Intensity.Low;
   }

   public float calculateCurrentValue() {
      IsoPlayer var1 = this.choosePlayer();
      if (var1 == null) {
         this.m_intensity = ParameterMusicIntensity.Intensity.Low;
      } else {
         float var2 = var1.getMusicIntensityEvents().getIntensity();
         this.m_intensity = var2 < 34.0F ? ParameterMusicIntensity.Intensity.Low : (var2 < 67.0F ? ParameterMusicIntensity.Intensity.Medium : ParameterMusicIntensity.Intensity.High);
      }

      return (float)this.m_intensity.label;
   }

   public void setState(IsoPlayer var1, Intensity var2) {
      if (var1 == this.choosePlayer()) {
         this.m_intensity = var2;
      }

   }

   private IsoPlayer choosePlayer() {
      if (this.m_playerIndex != -1) {
         IsoPlayer var1 = IsoPlayer.players[this.m_playerIndex];
         if (var1 == null || var1.isDead()) {
            this.m_playerIndex = -1;
         }
      }

      if (this.m_playerIndex != -1) {
         return IsoPlayer.players[this.m_playerIndex];
      } else {
         for(int var3 = 0; var3 < IsoPlayer.numPlayers; ++var3) {
            IsoPlayer var2 = IsoPlayer.players[var3];
            if (var2 != null && !var2.isDead()) {
               this.m_playerIndex = var3;
               this.m_intensity = ParameterMusicIntensity.Intensity.Low;
               return var2;
            }
         }

         return null;
      }
   }

   public static enum Intensity {
      Low(0),
      Medium(1),
      High(2);

      final int label;

      private Intensity(int var3) {
         this.label = var3;
      }
   }
}
