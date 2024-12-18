package zombie.ai.states;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.network.GameClient;
import zombie.util.Type;

public final class FitnessState extends State {
   private static final FitnessState _instance = new FitnessState();

   public FitnessState() {
   }

   public static FitnessState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      var1.setIgnoreMovement(true);
      var1.setVariable("FitnessFinished", false);
      var1.clearVariable("ExerciseStarted");
      var1.clearVariable("ExerciseEnded");
   }

   public void execute(IsoGameCharacter var1) {
   }

   public void exit(IsoGameCharacter var1) {
      if (GameClient.bClient && var1 instanceof IsoPlayer && ((IsoPlayer)var1).isLocalPlayer()) {
         GameClient.sendEvent((IsoPlayer)var1, "EventUpdateFitness");
      }

      var1.setIgnoreMovement(false);
      var1.clearVariable("FitnessFinished");
      var1.clearVariable("ExerciseStarted");
      var1.clearVariable("ExerciseHand");
      var1.clearVariable("FitnessStruggle");
      var1.setVariable("ExerciseEnded", true);
      var1.clearVariable("PlayerVoiceSound");
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      IsoPlayer var3 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      if (var2.m_EventName.equalsIgnoreCase("PlayerVoiceSound") && var3 != null) {
         if (var3.getVariableBoolean("PlayerVoiceSound")) {
         }

         var3.setVariable("PlayerVoiceSound", true);
         var3.stopPlayerVoiceSound(var2.m_ParameterValue);
         var3.playerVoiceSound(var2.m_ParameterValue);
      }

   }
}
