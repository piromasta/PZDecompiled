package zombie.ai.states;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.util.Type;

public final class PlayerFallingState extends State {
   private static final PlayerFallingState _instance = new PlayerFallingState();

   public PlayerFallingState() {
   }

   public static PlayerFallingState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      var1.setVariable("bHardFall", false);
      var1.setVariable("bHardFall2", false);
      var1.setVariable("bLandLight", false);
      var1.setVariable("bLandLightMask", false);
      var1.setVariable("bGetUpFromKnees", false);
      var1.setVariable("bGetUpFromProne", false);
      var1.clearVariable("bLandAnimFinished");
      if (var2 != null && var2.getHeightAboveFloor() > 1.5F) {
         var2.playerVoiceSound("DeathFall");
      }

   }

   public void execute(IsoGameCharacter var1) {
      IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      if (var2 != null && !var2.getVariableBoolean("bFalling")) {
         var2.stopPlayerVoiceSound("DeathFall");
      }

   }

   public void exit(IsoGameCharacter var1) {
      var1.clearVariable("bHardFall");
      var1.clearVariable("bHardFall2");
      var1.clearVariable("bLandLight");
      var1.clearVariable("bLandLightMask");
      var1.clearVariable("bLandAnimFinished");
      if (var1 instanceof IsoPlayer var2) {
         var2.stopPlayerVoiceSound("DeathFall");
      }

   }
}
