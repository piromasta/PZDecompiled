package zombie.ai.states.animals;

import org.joml.Vector3f;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.behavior.BehaviorAction;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.pathfind.Path;
import zombie.pathfind.PolygonalMap2;

public final class AnimalWalkState extends State {
   private static final AnimalWalkState _instance = new AnimalWalkState();
   private final Vector2 temp = new Vector2();
   private final Vector3f worldPos = new Vector3f();

   public AnimalWalkState() {
   }

   public static AnimalWalkState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      var1.setVariable("bMoving", true);
   }

   public void execute(IsoGameCharacter var1) {
      IsoAnimal var2 = (IsoAnimal)var1;
      this.temp.x = var2.getPathFindBehavior2().getTargetX();
      this.temp.y = var2.getPathFindBehavior2().getTargetY();
      Vector2 var10000 = this.temp;
      var10000.x -= var2.getX();
      var10000 = this.temp;
      var10000.y -= var2.getY();
      float var3 = this.temp.getLength();
      boolean var4 = var1.isCollidedThisFrame();
      if (var4) {
         var2.pathToLocation(var1.getPathTargetX(), var1.getPathTargetY(), var1.getPathTargetZ());
         if (!var2.getVariableBoolean("bPathfind")) {
            var2.setVariable("bPathfind", true);
            var2.setVariable("bMoving", true);
         }

      } else {
         float var5;
         float var6;
         float var7;
         if (!GameServer.bServer) {
            var5 = Math.min(var3 / 2.0F, 4.0F);
            var6 = (float)((var1.getID() + var2.animalID) % 20) / 10.0F - 1.0F;
            var7 = (float)((var2.getID() + var2.animalID) % 20) / 10.0F - 1.0F;
            var10000 = this.temp;
            var10000.x += var2.getX();
            var10000 = this.temp;
            var10000.y += var2.getY();
            var10000 = this.temp;
            var10000.x += var6 * var5;
            var10000 = this.temp;
            var10000.y += var7 * var5;
            var10000 = this.temp;
            var10000.x -= var2.getX();
            var10000 = this.temp;
            var10000.y -= var2.getY();
         }

         this.temp.normalize();
         var2.setDir(IsoDirections.fromAngle(this.temp));
         var2.setForwardDirection(this.temp);
         var5 = Math.abs(var2.getX() - var2.getPathFindBehavior2().getTargetX());
         var6 = Math.abs(var2.getY() - var2.getPathFindBehavior2().getTargetY());
         if ((double)var5 < 0.5 && (double)var6 < 0.5) {
            var1.setVariable("bMoving", false);
         }

         if (var1.getPathFindBehavior2().walkingOnTheSpot.check(var1) && var2.spottedChr == null) {
            var1.setVariable("bMoving", false);
            var1.setMoving(false);
            var1.getPathFindBehavior2().reset();
            var1.setPath2((Path)null);
            var2.getBehavior().walkedOnSpot();
         }

         if (var2.getBehavior().isDoingBehavior && var2.getBehavior().behaviorAction == BehaviorAction.FIGHTANIMAL && var2.getBehavior().behaviorObject instanceof IsoMovingObject) {
            var7 = var2.DistToProper((IsoMovingObject)var2.getBehavior().behaviorObject);
            if (var7 <= (float)var2.adef.attackDist) {
               this.exit(var1);
            }
         }

         var2.getBehavior().wanderIdle();
      }
   }

   public void exit(IsoGameCharacter var1) {
      var1.setVariable("bMoving", false);
      ((IsoAnimal)var1).getBehavior().doBehaviorAction();
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      IsoAnimal var3 = (IsoAnimal)var1;
      if ("PlayBreedSound".equalsIgnoreCase(var2.m_EventName)) {
         var3.onPlayBreedSoundEvent(var2.m_ParameterValue);
      }

      super.animEvent(var1, var2);
   }

   public boolean isMoving(IsoGameCharacter var1) {
      return true;
   }

   private boolean isPathClear(IsoGameCharacter var1, float var2, float var3, float var4) {
      int var5 = (int)var2 / 8;
      int var6 = (int)var3 / 8;
      IsoChunk var7 = GameServer.bServer ? ServerMap.instance.getChunk(var5, var6) : IsoWorld.instance.CurrentCell.getChunkForGridSquare((int)var2, (int)var3, (int)var4);
      if (var7 != null) {
         int var8 = 1;
         var8 |= 2;
         return !PolygonalMap2.instance.lineClearCollide(var1.getX(), var1.getY(), var2, var3, (int)var4, var1.getPathFindBehavior2().getTargetChar(), var8);
      } else {
         return false;
      }
   }
}
