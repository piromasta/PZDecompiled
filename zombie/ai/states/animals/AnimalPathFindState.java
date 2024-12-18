package zombie.ai.states.animals;

import java.util.HashMap;
import zombie.ai.State;
import zombie.ai.astar.AStarPathFinder;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.gameStates.IngameState;
import zombie.iso.IsoChunk;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.pathfind.Path;
import zombie.pathfind.PathFindBehavior2;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.nativeCode.PathfindNative;

public final class AnimalPathFindState extends State {
   private static final Integer PARAM_TICK_COUNT = 0;
   private static final AnimalPathFindState _instance = new AnimalPathFindState();

   public AnimalPathFindState() {
   }

   public static AnimalPathFindState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      HashMap var2 = var1.getStateMachineParams(this);
      var1.setVariable("bPathfind", true);
      var1.setVariable("bMoving", false);
      var2.put(PARAM_TICK_COUNT, IngameState.instance.numberTicks);
   }

   public void execute(IsoGameCharacter var1) {
      var1.getStateMachineParams(this);
      IsoAnimal var3 = (IsoAnimal)var1;
      PathFindBehavior2.BehaviorResult var4 = var1.getPathFindBehavior2().update();
      int var5;
      int var6;
      if (var4 == PathFindBehavior2.BehaviorResult.Failed) {
         var5 = var1.getPathTargetX();
         var6 = var1.getPathTargetY();
         int var8 = var1.getPathTargetZ();
         ((IsoAnimal)var1).pathFailed();
         var3.setShouldFollowWall(true);
         var3.getPathFindBehavior2().pathToLocation(var5, var6, var8);
         var1.setVariable("bMoving", true);
      } else if (var4 == PathFindBehavior2.BehaviorResult.Succeeded) {
         var5 = (int)var1.getPathFindBehavior2().getTargetX();
         var6 = (int)var1.getPathFindBehavior2().getTargetY();
         IsoChunk var7 = GameServer.bServer ? ServerMap.instance.getChunk(var5 / 8, var6 / 8) : IsoWorld.instance.CurrentCell.getChunkForGridSquare(var5, var6, 0);
         if (var7 == null) {
            var1.setVariable("bPathfind", false);
            var1.setVariable("bMoving", true);
         } else {
            var1.setVariable("bPathfind", false);
            var1.setVariable("bMoving", false);
            var1.setPath2((Path)null);
         }
      }
   }

   public void exit(IsoGameCharacter var1) {
      var1.setVariable("bPathfind", false);
      var1.setVariable("bMoving", false);
      if (PathfindNative.USE_NATIVE_CODE) {
         PathfindNative.instance.cancelRequest(var1);
      } else {
         PolygonalMap2.instance.cancelRequest(var1);
      }

      var1.getFinder().progress = AStarPathFinder.PathFindProgress.notrunning;
      var1.setPath2((Path)null);
      ((IsoAnimal)var1).getBehavior().doBehaviorAction();
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      IsoAnimal var3 = (IsoAnimal)var1;
      if ("PlayBreedSound".equalsIgnoreCase(var2.m_EventName)) {
         var3.onPlayBreedSoundEvent(var2.m_ParameterValue);
      }

   }

   public boolean isMoving(IsoGameCharacter var1) {
      return var1.isMoving();
   }
}
