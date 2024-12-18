package zombie.ai.states;

import java.util.HashMap;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.Moodles.MoodleType;
import zombie.characters.skills.PerkFactory;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoWindow;

public final class ClimbDownSheetRopeState extends State {
   public static final float CLIMB_DOWN_SPEED = 0.16F;
   public static final float CLIMB_DOWN_SLOWDOWN = 0.5F;
   private static final ClimbDownSheetRopeState _instance = new ClimbDownSheetRopeState();

   public ClimbDownSheetRopeState() {
   }

   public static ClimbDownSheetRopeState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      DebugLog.log("Entering climb down state");
      var1.setIgnoreMovement(true);
      var1.setHideWeaponModel(true);
      var1.setbClimbing(true);
      var1.setVariable("ClimbRope", true);
   }

   public void execute(IsoGameCharacter var1) {
      HashMap var2 = var1.getStateMachineParams(this);
      float var3 = 0.0F;
      float var4 = 0.0F;
      if (var1.getCurrentSquare().getProperties().Is(IsoFlagType.climbSheetTopN) || var1.getCurrentSquare().getProperties().Is(IsoFlagType.climbSheetN)) {
         var1.setDir(IsoDirections.N);
         var3 = 0.54F;
         var4 = 0.39F;
      }

      if (var1.getCurrentSquare().getProperties().Is(IsoFlagType.climbSheetTopS) || var1.getCurrentSquare().getProperties().Is(IsoFlagType.climbSheetS)) {
         var1.setDir(IsoDirections.S);
         var3 = 0.118F;
         var4 = 0.5756F;
      }

      if (var1.getCurrentSquare().getProperties().Is(IsoFlagType.climbSheetTopW) || var1.getCurrentSquare().getProperties().Is(IsoFlagType.climbSheetW)) {
         var1.setDir(IsoDirections.W);
         var3 = 0.4F;
         var4 = 0.7F;
      }

      if (var1.getCurrentSquare().getProperties().Is(IsoFlagType.climbSheetTopE) || var1.getCurrentSquare().getProperties().Is(IsoFlagType.climbSheetE)) {
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
      var7 = var1.getClimbRopeSpeed(true);
      var1.getSpriteDef().AnimFrameIncrease = var7;
      float var8 = var1.getZ() - var7 / 10.0F * GameTime.instance.getMultiplier();
      int var9 = var1.getCurrentSquare().getChunk().getMinLevel();
      var8 = Math.max(var8, (float)var9);

      int var10;
      for(var10 = PZMath.fastfloor(var1.getZ()); var10 >= PZMath.fastfloor(var8); --var10) {
         IsoCell var11 = IsoWorld.instance.getCell();
         IsoGridSquare var12 = var11.getGridSquare((double)var1.getX(), (double)var1.getY(), (double)var10);
         if ((var12.Is(IsoFlagType.solidtrans) || var12.TreatAsSolidFloor() || var10 == var9) && var8 <= (float)var10) {
            var1.setZ((float)var10);
            var2.clear();
            var1.clearVariable("ClimbRope");
            var1.setCollidable(true);
            var1.setbClimbing(false);
            return;
         }
      }

      var1.setZ(var8);
      var10 = (int)(var1.getClimbingFailChanceFloat() + 1.0F);
      boolean var13 = var1.getClimbRopeTime() > (float)(var10 * 10);
      var1.setClimbRopeTime(var1.getClimbRopeTime() + GameTime.instance.getMultiplier() / 3.0F);
      var10 *= 300;
      var10 = (int)((float)var10 / GameTime.instance.getMultiplier());
      if (!IsoWindow.isSheetRopeHere(var1.getCurrentSquare())) {
         var1.setCollidable(true);
         var1.setbClimbing(false);
         var1.setbFalling(true);
         var1.clearVariable("ClimbRope");
      } else if (var13 && !SandboxOptions.instance.EasyClimbing.getValue() && Rand.NextBool(var10)) {
         var1.fallFromRope();
      }

      float var14 = (float)(var1.getPerkLevel(PerkFactory.Perks.Nimble) + Math.max(var1.getPerkLevel(PerkFactory.Perks.Strength), var1.getPerkLevel(PerkFactory.Perks.Fitness)) * 2) / 3.0F;
      var1.addBothArmMuscleStrain((float)(0.007 * (double)GameTime.instance.getMultiplier() * (double)(var1.getMoodles().getMoodleLevel(MoodleType.HeavyLoad) + 1)) * ((15.0F - var14) / 10.0F) * (GameTime.instance.getMultiplier() / 0.8F));
      if (var1 instanceof IsoPlayer && ((IsoPlayer)var1).isLocalPlayer()) {
         ((IsoPlayer)var1).dirtyRecalcGridStackTime = 2.0F;
      }

   }

   public void exit(IsoGameCharacter var1) {
      var1.setIgnoreMovement(false);
      var1.setHideWeaponModel(false);
      var1.clearVariable("ClimbRope");
      var1.setbClimbing(false);
   }
}
