package zombie.ai.states;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoDirections;
import zombie.util.StringUtils;

public final class PlayerOnBedState extends State {
   private static final PlayerOnBedState _instance = new PlayerOnBedState();

   public PlayerOnBedState() {
   }

   public static PlayerOnBedState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      var1.getStateMachineParams(this);
      var1.setIgnoreMovement(true);
      var1.setCollidable(false);
      var1.setOnBed(true);
      if (!(var1.getPrimaryHandItem() instanceof HandWeapon) && !(var1.getSecondaryHandItem() instanceof HandWeapon)) {
         var1.setHideWeaponModel(true);
      }

      if (var1.getStateMachine().getPrevious() == IdleState.instance()) {
         var1.clearVariable("forceGetUp");
         var1.clearVariable("OnBedAnim");
         var1.clearVariable("OnBedStarted");
      }

      IsoDirections var3 = IsoDirections.fromAngle(var1.getAnimAngleRadians());
      switch (var3) {
         case N:
            var1.setY((float)((int)var1.getY()) + 0.3F);
            break;
         case S:
            var1.setY((float)((int)var1.getY()) + 0.7F);
            break;
         case W:
            var1.setX((float)((int)var1.getX()) + 0.3F);
            break;
         case E:
            var1.setX((float)((int)var1.getX()) + 0.7F);
      }

      var1.blockTurning = true;
   }

   public void execute(IsoGameCharacter var1) {
      var1.getStateMachineParams(this);
      IsoPlayer var3 = (IsoPlayer)var1;
      if (var3.pressedMovement(false)) {
         var1.StopAllActionQueue();
         var1.setVariable("forceGetUp", true);
      }

      if (var1.getVariableBoolean("OnBedStarted")) {
      }

      var3.setInitiateAttack(false);
      var3.setAttackStarted(false);
      var3.setAttackType((String)null);
   }

   public void exit(IsoGameCharacter var1) {
      var1.setHideWeaponModel(false);
      if (StringUtils.isNullOrEmpty(var1.getVariableString("HitReaction"))) {
         var1.clearVariable("forceGetUp");
         var1.clearVariable("OnBedAnim");
         var1.clearVariable("OnBedStarted");
         var1.setIgnoreMovement(false);
      }

   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      if (var2.m_EventName.equalsIgnoreCase("OnBedStarted")) {
         var1.setVariable("OnBedStarted", true);
         var1.setVariable("OnBedAnim", "Awake");
      }

   }
}
