package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.characters.IsoPlayer;

public final class ParameterMusicThreat extends FMODGlobalParameter {
   private int m_playerIndex = -1;
   private ThreatLevel m_threatLevel;

   public ParameterMusicThreat() {
      super("MusicThreat");
      this.m_threatLevel = ParameterMusicThreat.ThreatLevel.Low;
   }

   public float calculateCurrentValue() {
      IsoPlayer var1 = this.choosePlayer();
      if (var1 == null) {
         this.m_threatLevel = ParameterMusicThreat.ThreatLevel.Low;
      } else {
         float var2 = var1.getMusicThreatStatuses().getIntensity();
         this.m_threatLevel = var2 < 34.0F ? ParameterMusicThreat.ThreatLevel.Low : (var2 < 67.0F ? ParameterMusicThreat.ThreatLevel.Medium : ParameterMusicThreat.ThreatLevel.High);
      }

      return (float)this.m_threatLevel.label;
   }

   public void setState(IsoPlayer var1, ThreatLevel var2) {
      if (var1 == this.choosePlayer()) {
         this.m_threatLevel = var2;
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
               this.m_threatLevel = ParameterMusicThreat.ThreatLevel.Low;
               return var2;
            }
         }

         return null;
      }
   }

   public static enum ThreatLevel {
      Low(0),
      Medium(1),
      High(2);

      final int label;

      private ThreatLevel(int var3) {
         this.label = var3;
      }
   }
}
