package zombie.ai.states;

import fmod.fmod.FMODManager;
import zombie.ai.State;
import zombie.audio.parameters.ParameterCharacterMovementSpeed;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.iso.IsoDirections;
import zombie.util.Type;

public final class CollideWithWallState extends State {
   private static final CollideWithWallState _instance = new CollideWithWallState();

   public CollideWithWallState() {
   }

   public static CollideWithWallState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      var1.setIgnoreMovement(true);
      if (var1 instanceof IsoPlayer) {
         ((IsoPlayer)var1).setIsAiming(false);
      }

      if (var1.isCollidedN()) {
         var1.setDir(IsoDirections.N);
      }

      if (var1.isCollidedS()) {
         var1.setDir(IsoDirections.S);
      }

      if (var1.isCollidedE()) {
         var1.setDir(IsoDirections.E);
      }

      if (var1.isCollidedW()) {
         var1.setDir(IsoDirections.W);
      }

      var1.setCollideType("wall");
   }

   public void execute(IsoGameCharacter var1) {
      var1.setLastCollideTime(70.0F);
   }

   public void exit(IsoGameCharacter var1) {
      var1.clearVariable("PlayerVoiceSound");
      var1.setCollideType((String)null);
      var1.setIgnoreMovement(false);
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      IsoPlayer var3 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      if ("PlayCollideSound".equalsIgnoreCase(var2.m_EventName)) {
         long var4 = var1.playSound(var2.m_ParameterValue);
         ParameterCharacterMovementSpeed var6 = ((IsoPlayer)var1).getParameterCharacterMovementSpeed();
         var1.getEmitter().setParameterValue(var4, var6.getParameterDescription(), (float)ParameterCharacterMovementSpeed.MovementType.Sprint.label);
         var1.getEmitter().setParameterValue(var4, FMODManager.instance.getParameterDescription("TripObstacleType"), 7.0F);
      }

      if (var2.m_EventName.equalsIgnoreCase("PlayerVoiceSound")) {
         if (var1.getVariableBoolean("PlayerVoiceSound")) {
            return;
         }

         if (var3 == null) {
            return;
         }

         var1.setVariable("PlayerVoiceSound", true);
         var3.playerVoiceSound(var2.m_ParameterValue);
      }

   }
}
