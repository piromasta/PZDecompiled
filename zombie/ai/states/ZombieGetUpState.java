package zombie.ai.states;

import java.util.HashMap;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.network.GameClient;
import zombie.util.StringUtils;

public final class ZombieGetUpState extends State {
   private static final ZombieGetUpState _instance = new ZombieGetUpState();
   static final Integer PARAM_PREV_STATE = 2;

   public ZombieGetUpState() {
   }

   public static ZombieGetUpState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      IsoZombie var2 = (IsoZombie)var1;
      HashMap var3 = var2.getStateMachineParams(this);
      State var4 = var2.getStateMachine().getPrevious();
      if (var4 == ZombieGetUpFromCrawlState.instance()) {
         var4 = (State)var2.getStateMachineParams(ZombieGetUpFromCrawlState.instance()).get(1);
      }

      var3.put(PARAM_PREV_STATE, var4);
      var2.parameterZombieState.setState(ParameterZombieState.State.GettingUp);
      var2.setOnFloor(true);
      if (GameClient.bClient) {
         var2.setKnockedDown(false);
      }

   }

   public void exit(IsoGameCharacter var1) {
      IsoZombie var2 = (IsoZombie)var1;
      HashMap var3 = var2.getStateMachineParams(this);
      var2.setCollidable(true);
      var2.clearVariable("SprinterTripped");
      var2.clearVariable("ShouldStandUp");
      if (StringUtils.isNullOrEmpty(var2.getHitReaction())) {
         var2.setSitAgainstWall(false);
      }

      var2.setKnockedDown(false);
      var2.AllowRepathDelay = 0.0F;
      if (var3.get(PARAM_PREV_STATE) == PathFindState.instance()) {
         if (var2.getPathFindBehavior2().getTargetChar() == null) {
            var2.setVariable("bPathfind", true);
            var2.setVariable("bMoving", false);
         } else if (var2.isTargetLocationKnown()) {
            var2.pathToCharacter(var2.getPathFindBehavior2().getTargetChar());
         } else if (var2.LastTargetSeenX != -1) {
            var2.pathToLocation(var2.LastTargetSeenX, var2.LastTargetSeenY, var2.LastTargetSeenZ);
         }
      } else if (var3.get(PARAM_PREV_STATE) == WalkTowardState.instance()) {
         var2.setVariable("bPathFind", false);
         var2.setVariable("bMoving", true);
      }

   }
}
