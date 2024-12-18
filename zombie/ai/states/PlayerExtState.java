package zombie.ai.states;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.util.Type;

public final class PlayerExtState extends State {
   private static final PlayerExtState _instance = new PlayerExtState();

   public PlayerExtState() {
   }

   public static PlayerExtState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      var1.setVariable("ExtPlaying", true);
   }

   public void execute(IsoGameCharacter var1) {
   }

   public void exit(IsoGameCharacter var1) {
      var1.clearVariable("ExtPlaying");
      var1.clearVariable("PlayerVoiceSound");
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      var1.getStateMachineParams(this);
      IsoPlayer var4 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      if ("ExtFinishing".equalsIgnoreCase(var2.m_EventName)) {
         var1.setVariable("ExtPlaying", false);
         var1.clearVariable("PlayerVoiceSound");
      }

      if (var2.m_EventName.equalsIgnoreCase("PlayerVoiceSound")) {
         if (var1.getVariableBoolean("PlayerVoiceSound")) {
            return;
         }

         if (var4 == null) {
            return;
         }

         var1.setVariable("PlayerVoiceSound", true);
         var4.stopPlayerVoiceSound(var2.m_ParameterValue);
         var4.playerVoiceSound(var2.m_ParameterValue);
      }

   }
}
