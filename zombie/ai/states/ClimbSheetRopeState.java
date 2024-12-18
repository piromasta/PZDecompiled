package zombie.ai.states;

import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.Moodles.MoodleType;
import zombie.characters.skills.PerkFactory;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;

public final class ClimbSheetRopeState extends State {
   public static final float CLIMB_SPEED = 0.16F;
   public static final float CLIMB_SLOWDOWN = 0.5F;
   private static final ClimbSheetRopeState _instance = new ClimbSheetRopeState();

   public ClimbSheetRopeState() {
   }

   public static ClimbSheetRopeState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      var1.setIgnoreMovement(true);
      var1.setbClimbing(true);
      var1.setVariable("ClimbRope", true);
   }

   public void execute(IsoGameCharacter var1) {
      var1.getStateMachineParams(this);
      float var3 = 0.0F;
      float var4 = 0.0F;
      if (var1.getCurrentSquare().getProperties().Is(IsoFlagType.climbSheetN) || var1.getCurrentSquare().getProperties().Is(IsoFlagType.climbSheetTopN)) {
         var1.setDir(IsoDirections.N);
         var3 = 0.54F;
         var4 = 0.39F;
      }

      if (var1.getCurrentSquare().getProperties().Is(IsoFlagType.climbSheetS) || var1.getCurrentSquare().getProperties().Is(IsoFlagType.climbSheetTopS)) {
         var1.setDir(IsoDirections.S);
         var3 = 0.118F;
         var4 = 0.5756F;
      }

      if (var1.getCurrentSquare().getProperties().Is(IsoFlagType.climbSheetW) || var1.getCurrentSquare().getProperties().Is(IsoFlagType.climbSheetTopW)) {
         var1.setDir(IsoDirections.W);
         var3 = 0.4F;
         var4 = 0.7F;
      }

      if (var1.getCurrentSquare().getProperties().Is(IsoFlagType.climbSheetE) || var1.getCurrentSquare().getProperties().Is(IsoFlagType.climbSheetTopE)) {
         var1.setDir(IsoDirections.E);
         var3 = 0.5417F;
         var4 = 0.3144F;
      }

      float var5 = var1.getX() - (float)PZMath.fastfloor(var1.getX());
      float var6 = var1.getY() - (float)PZMath.fastfloor(var1.getY());
      float var7;
      if (var5 != var3) {
         var7 = (var3 - var5) / 4.0F;
         var5 += var7;
         var1.setX((float)PZMath.fastfloor(var1.getX()) + var5);
      }

      if (var6 != var4) {
         var7 = (var4 - var6) / 4.0F;
         var6 += var7;
         var1.setY((float)PZMath.fastfloor(var1.getY()) + var6);
      }

      var1.setNextX(var1.getX());
      var1.setNextY(var1.getY());
      var7 = var1.getClimbRopeSpeed(false);
      var1.getSpriteDef().AnimFrameIncrease = var7;
      float var8 = var1.getZ() + var7 / 10.0F * GameTime.instance.getMultiplier();
      int var9 = var1.getCurrentSquare().getChunk().getMaxLevel();
      var8 = Math.min(var8, (float)var9);

      int var10;
      for(var10 = PZMath.fastfloor(var1.getZ()); (float)var10 <= var8; ++var10) {
         IsoCell var11 = IsoWorld.instance.getCell();
         IsoGridSquare var12 = var11.getGridSquare((double)var1.getX(), (double)var1.getY(), (double)var10);
         if (IsoWindow.isTopOfSheetRopeHere(var12)) {
            var1.setZ((float)var10);
            var1.setCurrent(var12);
            var1.setCollidable(true);
            IsoGridSquare var13 = var12.nav[var1.dir.index()];
            if (var13 != null) {
               if (!var13.TreatAsSolidFloor()) {
                  var1.climbDownSheetRope();
                  return;
               }

               IsoWindow var14 = var12.getWindowTo(var13);
               if (var14 != null) {
                  if (!var14.open) {
                     var14.ToggleWindow(var1);
                  }

                  if (!var14.canClimbThrough(var1)) {
                     var1.climbDownSheetRope();
                     return;
                  }

                  var1.climbThroughWindow(var14, 4);
                  return;
               }

               IsoThumpable var15 = var12.getWindowThumpableTo(var13);
               if (var15 != null) {
                  if (!var15.canClimbThrough(var1)) {
                     var1.climbDownSheetRope();
                     return;
                  }

                  var1.climbThroughWindow(var15, 4);
                  return;
               }

               var15 = var12.getHoppableThumpableTo(var13);
               if (var15 != null) {
                  if (!IsoWindow.canClimbThroughHelper(var1, var12, var13, var1.dir == IsoDirections.N || var1.dir == IsoDirections.S)) {
                     var1.climbDownSheetRope();
                     return;
                  }

                  var1.climbOverFence(var1.dir);
                  return;
               }

               IsoWindowFrame var16 = var12.getWindowFrameTo(var13);
               if (var16 != null) {
                  if (!var16.canClimbThrough(var1)) {
                     var1.climbDownSheetRope();
                     return;
                  }

                  var1.climbThroughWindowFrame(var16);
                  return;
               }

               IsoObject var17 = var12.getWallHoppableTo(var13);
               if (var17 != null) {
                  if (!IsoWindow.canClimbThroughHelper(var1, var12, var13, var1.dir == IsoDirections.N || var1.dir == IsoDirections.S)) {
                     var1.climbDownSheetRope();
                     return;
                  }

                  var1.climbOverFence(var1.dir);
                  return;
               }
            }

            return;
         }
      }

      var1.setZ(var8);
      if (var1.getZ() >= (float)var9) {
         var1.setCollidable(true);
         var1.clearVariable("ClimbRope");
      }

      var10 = (int)(var1.getClimbingFailChanceFloat() + 1.0F);
      boolean var18 = var1.getClimbRopeTime() > (float)(var10 * 10);
      var1.setClimbRopeTime(var1.getClimbRopeTime() + GameTime.instance.getMultiplier());
      var10 *= 100;
      var10 = (int)((float)var10 / GameTime.instance.getMultiplier());
      if (!IsoWindow.isSheetRopeHere(var1.getCurrentSquare())) {
         var1.setCollidable(true);
         var1.setbClimbing(false);
         var1.setbFalling(true);
         var1.clearVariable("ClimbRope");
      } else if (var18 && !SandboxOptions.instance.EasyClimbing.getValue() && Rand.NextBool(var10)) {
         var1.fallFromRope();
      }

      float var19 = (float)(var1.getPerkLevel(PerkFactory.Perks.Nimble) + Math.max(var1.getPerkLevel(PerkFactory.Perks.Strength), var1.getPerkLevel(PerkFactory.Perks.Fitness)) * 2) / 3.0F;
      var1.addBothArmMuscleStrain((float)(0.02 * (double)GameTime.instance.getMultiplier() * (double)(var1.getMoodles().getMoodleLevel(MoodleType.HeavyLoad) + 1)) * ((15.0F - var19) / 10.0F) * (GameTime.instance.getMultiplier() / 0.8F));
      if (var1 instanceof IsoPlayer && ((IsoPlayer)var1).isLocalPlayer()) {
         ((IsoPlayer)var1).dirtyRecalcGridStackTime = 2.0F;
      }

   }

   public void exit(IsoGameCharacter var1) {
      var1.setIgnoreMovement(false);
      var1.setbClimbing(false);
      var1.clearVariable("ClimbRope");
   }
}
