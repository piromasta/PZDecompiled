package zombie.ai.states.animals;

import java.util.HashMap;
import zombie.GameTime;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.util.StringUtils;
import zombie.util.Type;

public final class AnimalClimbOverFenceState extends State {
   private static final AnimalClimbOverFenceState _instance = new AnimalClimbOverFenceState();
   static final Integer PARAM_START_X = 0;
   static final Integer PARAM_START_Y = 1;
   static final Integer PARAM_Z = 2;
   static final Integer PARAM_END_X = 3;
   static final Integer PARAM_END_Y = 4;
   static final Integer PARAM_DIR = 5;
   static final Integer PARAM_ZOMBIE_ON_FLOOR = 6;
   static final Integer PARAM_PREV_STATE = 7;
   static final Integer PARAM_SCRATCH = 8;
   static final Integer PARAM_COUNTER = 9;
   static final Integer PARAM_SOLID_FLOOR = 10;
   static final Integer PARAM_SHEET_ROPE = 11;
   static final Integer PARAM_RUN = 12;
   static final Integer PARAM_SPRINT = 13;
   static final Integer PARAM_COLLIDABLE = 14;
   static final int FENCE_TYPE_WOOD = 0;
   static final int FENCE_TYPE_METAL = 1;
   static final int FENCE_TYPE_SANDBAG = 2;
   static final int FENCE_TYPE_GRAVELBAG = 3;
   static final int FENCE_TYPE_BARBWIRE = 4;
   static final int FENCE_TYPE_ROADBLOCK = 5;
   static final int FENCE_TYPE_METAL_BARS = 6;
   static final int TRIP_WOOD = 0;
   static final int TRIP_METAL = 1;
   static final int TRIP_SANDBAG = 2;
   static final int TRIP_GRAVELBAG = 3;
   static final int TRIP_BARBWIRE = 4;
   public static final int TRIP_TREE = 5;
   public static final int TRIP_ZOMBIE = 6;
   public static final int COLLIDE_WITH_WALL = 7;
   public static final int TRIP_METAL_BARS = 8;
   public static final int TRIP_WINDOW = 9;

   public AnimalClimbOverFenceState() {
   }

   public static AnimalClimbOverFenceState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      IsoGridSquare var2 = var1.getCurrentSquare().nav[var1.dir.index()];
      if (((IsoAnimal)var1).canClimbFences() && (var1.getCurrentSquare().getHoppable(true) != null || var1.getCurrentSquare().getHoppable(false) != null || var2.getHoppable(true) != null || var2.getHoppable(false) != null)) {
         var1.setVariable("ClimbingFence", true);
      } else {
         var1.clearVariable("climbDown");
         var1.setVariable("ClimbFence", false);
      }
   }

   public void execute(IsoGameCharacter var1) {
      HashMap var2 = var1.getStateMachineParams(this);
      IsoDirections var3 = (IsoDirections)Type.tryCastTo(var2.get(PARAM_DIR), IsoDirections.class);
      int var4 = (Integer)var2.get(PARAM_END_X);
      int var5 = (Integer)var2.get(PARAM_END_Y);
      var1.setAnimated(true);
      if (var3 == IsoDirections.N) {
         var1.setDir(IsoDirections.N);
      } else if (var3 == IsoDirections.S) {
         var1.setDir(IsoDirections.S);
      } else if (var3 == IsoDirections.W) {
         var1.setDir(IsoDirections.W);
      } else if (var3 == IsoDirections.E) {
         var1.setDir(IsoDirections.E);
      }

      if (var1.getVariableBoolean("ClimbFenceStarted") && var1.isVariable("ClimbFenceOutcome", "fall")) {
         var1.setFallTime(Math.max(var1.getFallTime(), 2.1F));
      }

   }

   public void exit(IsoGameCharacter var1) {
      HashMap var2 = var1.getStateMachineParams(this);
      var1.clearVariable("ClimbFence");
      var1.ClearVariable("climbDown");
      IsoDirections var3 = (IsoDirections)Type.tryCastTo(var2.get(PARAM_DIR), IsoDirections.class);
      var3 = IsoDirections.reverse(var3);
      var1.setDir(var3);
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      HashMap var3 = var1.getStateMachineParams(this);
      IsoAnimal var4 = (IsoAnimal)Type.tryCastTo(var1, IsoAnimal.class);
      if (var2.m_EventName.equalsIgnoreCase("Climbed")) {
         var4.setVariable("climbDown", true);
         IsoDirections var5 = (IsoDirections)Type.tryCastTo(var3.get(PARAM_DIR), IsoDirections.class);
         var5 = var5.RotLeft();
         var5 = var5.RotLeft();
         var3.put(PARAM_DIR, var5);
      }

      if (var2.m_EventName.equalsIgnoreCase("ClimbDone")) {
         var4.clearVariable("climbDown");
         var4.setVariable("ClimbFence", false);
      }

   }

   public boolean isIgnoreCollide(IsoGameCharacter var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      HashMap var8 = var1.getStateMachineParams(this);
      int var9 = (Integer)var8.get(PARAM_START_X);
      int var10 = (Integer)var8.get(PARAM_START_Y);
      int var11 = (Integer)var8.get(PARAM_END_X);
      int var12 = (Integer)var8.get(PARAM_END_Y);
      int var13 = (Integer)var8.get(PARAM_Z);
      if (var13 == var4 && var13 == var7) {
         int var14 = PZMath.min(var9, var11);
         int var15 = PZMath.min(var10, var12);
         int var16 = PZMath.max(var9, var11);
         int var17 = PZMath.max(var10, var12);
         int var18 = PZMath.min(var2, var5);
         int var19 = PZMath.min(var3, var6);
         int var20 = PZMath.max(var2, var5);
         int var21 = PZMath.max(var3, var6);
         return var14 <= var18 && var15 <= var19 && var16 >= var20 && var17 >= var21;
      } else {
         return false;
      }
   }

   private void slideX(IsoGameCharacter var1, float var2) {
      float var3 = 0.05F * GameTime.getInstance().getThirtyFPSMultiplier();
      var3 = var2 > var1.getX() ? Math.min(var3, var2 - var1.getX()) : Math.max(-var3, var2 - var1.getX());
      var1.setX(var1.getX() + var3);
      var1.setNextX(var1.getX());
   }

   private void slideY(IsoGameCharacter var1, float var2) {
      float var3 = 0.05F * GameTime.getInstance().getThirtyFPSMultiplier();
      var3 = var2 > var1.getY() ? Math.min(var3, var2 - var1.getY()) : Math.max(-var3, var2 - var1.getY());
      var1.setY(var1.getY() + var3);
      var1.setNextY(var1.getY());
   }

   private IsoObject getFence(IsoGameCharacter var1) {
      HashMap var2 = var1.getStateMachineParams(this);
      int var3 = (Integer)var2.get(PARAM_START_X);
      int var4 = (Integer)var2.get(PARAM_START_Y);
      int var5 = (Integer)var2.get(PARAM_Z);
      IsoGridSquare var6 = IsoWorld.instance.CurrentCell.getGridSquare(var3, var4, var5);
      int var7 = (Integer)var2.get(PARAM_END_X);
      int var8 = (Integer)var2.get(PARAM_END_Y);
      IsoGridSquare var9 = IsoWorld.instance.CurrentCell.getGridSquare(var7, var8, var5);
      return var6 != null && var9 != null ? var6.getHoppableTo(var9) : null;
   }

   private int getFenceType(IsoObject var1) {
      if (var1.getSprite() == null) {
         return 0;
      } else {
         PropertyContainer var2 = var1.getSprite().getProperties();
         String var3 = var2.Val("FenceTypeLow");
         if (var3 != null) {
            if ("Sandbag".equals(var3) && var1.getName() != null && StringUtils.containsIgnoreCase(var1.getName(), "Gravel")) {
               var3 = "Gravelbag";
            }

            byte var10000;
            switch (var3) {
               case "Wood":
                  var10000 = 0;
                  break;
               case "Metal":
                  var10000 = 1;
                  break;
               case "Sandbag":
                  var10000 = 2;
                  break;
               case "Gravelbag":
                  var10000 = 3;
                  break;
               case "Barbwire":
                  var10000 = 4;
                  break;
               case "RoadBlock":
                  var10000 = 5;
                  break;
               case "MetalGate":
                  var10000 = 6;
                  break;
               default:
                  var10000 = 0;
            }

            return var10000;
         } else {
            return 0;
         }
      }
   }

   private int getTripType(IsoObject var1) {
      if (var1.getSprite() == null) {
         return 0;
      } else {
         PropertyContainer var2 = var1.getSprite().getProperties();
         String var3 = var2.Val("FenceTypeLow");
         if (var3 != null) {
            if ("Sandbag".equals(var3) && var1.getName() != null && StringUtils.containsIgnoreCase(var1.getName(), "Gravel")) {
               var3 = "Gravelbag";
            }

            byte var10000;
            switch (var3) {
               case "Wood":
                  var10000 = 0;
                  break;
               case "Metal":
                  var10000 = 1;
                  break;
               case "Sandbag":
                  var10000 = 2;
                  break;
               case "Gravelbag":
                  var10000 = 3;
                  break;
               case "Barbwire":
                  var10000 = 4;
                  break;
               case "MetalGate":
                  var10000 = 8;
                  break;
               default:
                  var10000 = 0;
            }

            return var10000;
         } else {
            return 0;
         }
      }
   }

   public void setParams(IsoGameCharacter var1, IsoDirections var2) {
      HashMap var3 = var1.getStateMachineParams(this);
      int var4 = var1.getSquare().getX();
      int var5 = var1.getSquare().getY();
      int var6 = var1.getSquare().getZ();
      int var9 = var4;
      int var10 = var5;
      switch (var2) {
         case N:
            var10 = var5 - 1;
            break;
         case S:
            var10 = var5 + 1;
            break;
         case W:
            var9 = var4 - 1;
            break;
         case E:
            var9 = var4 + 1;
            break;
         default:
            throw new IllegalArgumentException("invalid direction");
      }

      IsoGridSquare var11 = IsoWorld.instance.CurrentCell.getGridSquare(var9, var10, var6);
      boolean var12 = false;
      boolean var13 = var11 != null && var11.Is(IsoFlagType.solidtrans);
      boolean var14 = var11 != null && var11.TreatAsSolidFloor();
      boolean var15 = var11 != null && var1.canClimbDownSheetRope(var11);
      var3.put(PARAM_START_X, var4);
      var3.put(PARAM_START_Y, var5);
      var3.put(PARAM_Z, var6);
      var3.put(PARAM_END_X, var9);
      var3.put(PARAM_END_Y, var10);
      var3.put(PARAM_DIR, var2);
      var3.put(PARAM_ZOMBIE_ON_FLOOR, Boolean.FALSE);
      var3.put(PARAM_PREV_STATE, var1.getCurrentState());
      var3.put(PARAM_COUNTER, var13 ? Boolean.TRUE : Boolean.FALSE);
      var3.put(PARAM_SOLID_FLOOR, var14 ? Boolean.TRUE : Boolean.FALSE);
      var3.put(PARAM_SHEET_ROPE, var15 ? Boolean.TRUE : Boolean.FALSE);
      var3.put(PARAM_RUN, var1.isRunning() ? Boolean.TRUE : Boolean.FALSE);
      var3.put(PARAM_SPRINT, var1.isSprinting() ? Boolean.TRUE : Boolean.FALSE);
      var3.put(PARAM_COLLIDABLE, Boolean.FALSE);
   }
}
