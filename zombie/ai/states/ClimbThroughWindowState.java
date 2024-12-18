package zombie.ai.states;

import fmod.fmod.FMODManager;
import java.util.HashMap;
import zombie.GameTime;
import zombie.SoundManager;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.MoveDeltaModifiers;
import zombie.characters.Moodles.MoodleType;
import zombie.characters.skills.PerkFactory;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.util.Pool;
import zombie.util.Type;

public final class ClimbThroughWindowState extends State {
   private static final ClimbThroughWindowState _instance = new ClimbThroughWindowState();
   static final Integer PARAM_PARAMS = 0;
   static final Integer PARAM_PREV_STATE = 1;
   static final Integer PARAM_ZOMBIE_ON_FLOOR = 2;

   public ClimbThroughWindowState() {
   }

   public static ClimbThroughWindowState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      var1.setIgnoreMovement(true);
      var1.setHideWeaponModel(true);
      HashMap var3 = var1.getStateMachineParams(this);
      ClimbThroughWindowPositioningParams var4 = (ClimbThroughWindowPositioningParams)var3.get(PARAM_PARAMS);
      boolean var5 = var4.isCounter;
      var1.setVariable("ClimbWindowStarted", false);
      var1.setVariable("ClimbWindowEnd", false);
      var1.setVariable("ClimbWindowFinished", false);
      var1.clearVariable("ClimbWindowGetUpBack");
      var1.clearVariable("ClimbWindowGetUpFront");
      var1.setVariable("ClimbWindowOutcome", var5 ? "obstacle" : "success");
      var1.clearVariable("ClimbWindowFlopped");
      IsoZombie var6 = (IsoZombie)Type.tryCastTo(var1, IsoZombie.class);
      if (!var5 && var6 != null && var6.shouldDoFenceLunge()) {
         this.setLungeXVars(var6);
         var1.setVariable("ClimbWindowOutcome", "lunge");
      }

      if (!var4.isFloor) {
         var1.setVariable("ClimbWindowOutcome", "fall");
      }

      if (!(var1 instanceof IsoZombie) && var4.isSheetRope) {
         var1.setVariable("ClimbWindowOutcome", "rope");
      }

      if (var2 != null && var2.isLocalPlayer()) {
         var2.dirtyRecalcGridStackTime = 20.0F;
         var2.triggerMusicIntensityEvent("ClimbThroughWindow");
      }

   }

   public void execute(IsoGameCharacter var1) {
      HashMap var2 = var1.getStateMachineParams(this);
      ClimbThroughWindowPositioningParams var3 = (ClimbThroughWindowPositioningParams)var2.get(PARAM_PARAMS);
      IsoPlayer var4 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      if (!this.isWindowClosing(var1)) {
         var1.setDir(var3.climbDir);
         String var5 = var1.getVariableString("ClimbWindowOutcome");
         int var7;
         int var8;
         if (var1 instanceof IsoZombie) {
            boolean var6 = var2.get(PARAM_ZOMBIE_ON_FLOOR) == Boolean.TRUE;
            if (!var1.isFallOnFront() && var6) {
               var7 = var3.oppositeX;
               var8 = var3.oppositeY;
               int var9 = var3.z;
               IsoGridSquare var10 = IsoWorld.instance.CurrentCell.getGridSquare(var7, var8, var9);
               if (var10 != null && var10.getBrokenGlass() != null) {
                  var1.addBlood(BloodBodyPartType.Head, true, true, true);
                  var1.addBlood(BloodBodyPartType.Head, true, true, true);
                  var1.addBlood(BloodBodyPartType.Head, true, true, true);
                  var1.addBlood(BloodBodyPartType.Head, true, true, true);
                  var1.addBlood(BloodBodyPartType.Head, true, true, true);
                  var1.addBlood(BloodBodyPartType.Neck, true, true, true);
                  var1.addBlood(BloodBodyPartType.Neck, true, true, true);
                  var1.addBlood(BloodBodyPartType.Neck, true, true, true);
                  var1.addBlood(BloodBodyPartType.Neck, true, true, true);
                  var1.addBlood(BloodBodyPartType.Torso_Upper, true, true, true);
                  var1.addBlood(BloodBodyPartType.Torso_Upper, true, true, true);
                  var1.addBlood(BloodBodyPartType.Torso_Upper, true, true, true);
               }
            }

            var1.setOnFloor(var6);
            var1.setKnockedDown(var6);
            var1.setFallOnFront(var6);
         }

         if (!var1.getVariableBoolean("ClimbWindowStarted")) {
            slideCharacterToWindowOpening(var1, var3);
         }

         float var11;
         float var13;
         if (var1 instanceof IsoPlayer && var5.equalsIgnoreCase("obstacle")) {
            var11 = (float)var3.endX + 0.5F;
            var13 = (float)var3.endY + 0.5F;
            if (var1.DistToSquared(var11, var13) < 0.5625F) {
               var1.setVariable("ClimbWindowOutcome", "obstacleEnd");
            }
         }

         if (var1 instanceof IsoPlayer && !var1.getVariableBoolean("ClimbWindowEnd") && !"fallfront".equals(var5) && !"back".equals(var5) && !"fallback".equals(var5)) {
            int var12 = var3.oppositeX;
            var7 = var3.oppositeY;
            var8 = var3.z;
            IsoGridSquare var14 = IsoWorld.instance.CurrentCell.getGridSquare(var12, var7, var8);
            if (var14 != null) {
               this.checkForFallingBack(var14, var1);
               if (var14 != var1.getSquare() && var14.TreatAsSolidFloor()) {
                  this.checkForFallingFront(var1.getSquare(), var1);
               }
            }

            if (var1.getMoodles().getMoodleLevel(MoodleType.Drunk) > 1 && var1.getVariableString("ClimbWindowOutcome").equals(var5) && (float)Rand.Next(2000) < var1.getStats().Drunkenness) {
               if (Rand.NextBool(2)) {
                  var1.setVariable("ClimbWindowOutcome", "fallback");
               } else {
                  var1.setVariable("ClimbWindowOutcome", "fallfront");
               }
            }
         }

         if (var1.getVariableBoolean("ClimbWindowStarted") && !"back".equals(var5) && !"fallback".equals(var5) && !"lunge".equals(var5) && !"obstacle".equals(var5) && !"obstacleEnd".equals(var5)) {
            var11 = (float)var3.startX;
            var13 = (float)var3.startY;
            switch (var3.climbDir) {
               case N:
                  var13 -= 0.1F;
                  break;
               case S:
                  ++var13;
                  break;
               case W:
                  var11 -= 0.1F;
                  break;
               case E:
                  ++var11;
            }

            if (PZMath.fastfloor(var1.getX()) != PZMath.fastfloor(var11) && (var3.climbDir == IsoDirections.W || var3.climbDir == IsoDirections.E)) {
               slideX(var1, var11);
            }

            if (PZMath.fastfloor(var1.getY()) != PZMath.fastfloor(var13) && (var3.climbDir == IsoDirections.N || var3.climbDir == IsoDirections.S)) {
               slideY(var1, var13);
            }
         }

         if (var1.getVariableBoolean("ClimbWindowStarted") && var3.scratch) {
            var3.scratch = false;
            var1.getBodyDamage().setScratchedWindow();
            if (var4 != null) {
               var4.playerVoiceSound("PainFromGlassCut");
            }
         }

         if (var1.getVariableBoolean("ClimbWindowStarted") && var1.isVariable("ClimbWindowOutcome", "fall")) {
            var1.setFallTime(Math.max(var1.getFallTime(), 2.1F));
         }

      }
   }

   public static void slideCharacterToWindowOpening(IsoGameCharacter var0, ClimbThroughWindowPositioningParams var1) {
      IsoDirections var2 = var1.climbDir;
      float var3;
      if (var2 == IsoDirections.N || var2 == IsoDirections.S) {
         var3 = (float)var1.startX + 0.5F;
         if (var0.getX() != var3) {
            slideX(var0, var3);
         }
      }

      if (var2 == IsoDirections.W || var2 == IsoDirections.E) {
         var3 = (float)var1.startY + 0.5F;
         if (var0.getY() != var3) {
            slideY(var0, var3);
         }
      }

   }

   private void checkForFallingBack(IsoGridSquare var1, IsoGameCharacter var2) {
      for(int var3 = 0; var3 < var1.getMovingObjects().size(); ++var3) {
         IsoMovingObject var4 = (IsoMovingObject)var1.getMovingObjects().get(var3);
         IsoZombie var5 = (IsoZombie)Type.tryCastTo(var4, IsoZombie.class);
         if (var5 != null && !var5.isOnFloor() && !var5.isSitAgainstWall()) {
            if (!var5.isVariable("AttackOutcome", "success") && Rand.Next(5 + var2.getPerkLevel(PerkFactory.Perks.Fitness)) != 0) {
               var5.playHurtSound();
               var2.setVariable("ClimbWindowOutcome", "back");
            } else {
               var5.playHurtSound();
               var2.setVariable("ClimbWindowOutcome", "fallback");
            }
         }
      }

   }

   private void checkForFallingFront(IsoGridSquare var1, IsoGameCharacter var2) {
      for(int var3 = 0; var3 < var1.getMovingObjects().size(); ++var3) {
         IsoMovingObject var4 = (IsoMovingObject)var1.getMovingObjects().get(var3);
         IsoZombie var5 = (IsoZombie)Type.tryCastTo(var4, IsoZombie.class);
         if (var5 != null && !var5.isOnFloor() && !var5.isSitAgainstWall() && var5.isVariable("AttackOutcome", "success")) {
            var5.playHurtSound();
            var2.setVariable("ClimbWindowOutcome", "fallfront");
         }
      }

   }

   public void exit(IsoGameCharacter var1) {
      var1.setIgnoreMovement(false);
      var1.setHideWeaponModel(false);
      HashMap var2 = var1.getStateMachineParams(this);
      if (var1.isVariable("ClimbWindowOutcome", "fall") || var1.isVariable("ClimbWindowOutcome", "fallback") || var1.isVariable("ClimbWindowOutcome", "fallfront")) {
         var1.setHitReaction("");
      }

      var1.clearVariable("ClimbWindowFinished");
      var1.clearVariable("ClimbWindowOutcome");
      var1.clearVariable("ClimbWindowStarted");
      var1.clearVariable("ClimbWindowFlopped");
      var1.clearVariable("PlayerVoiceSound");
      if (var1 instanceof IsoZombie) {
         var1.setOnFloor(false);
         var1.setKnockedDown(false);
      }

      IsoZombie var3 = (IsoZombie)Type.tryCastTo(var1, IsoZombie.class);
      if (var3 != null) {
         var3.AllowRepathDelay = 0.0F;
         if (var2.get(PARAM_PREV_STATE) == PathFindState.instance()) {
            if (var1.getPathFindBehavior2().getTargetChar() == null) {
               var1.setVariable("bPathFind", true);
               var1.setVariable("bMoving", false);
            } else if (var3.isTargetLocationKnown()) {
               var1.pathToCharacter(var1.getPathFindBehavior2().getTargetChar());
            } else if (var3.LastTargetSeenX != -1) {
               var1.pathToLocation(var3.LastTargetSeenX, var3.LastTargetSeenY, var3.LastTargetSeenZ);
            }
         } else if (var2.get(PARAM_PREV_STATE) == WalkTowardState.instance() || var2.get(PARAM_PREV_STATE) == WalkTowardNetworkState.instance()) {
            var1.setVariable("bPathFind", false);
            var1.setVariable("bMoving", true);
         }
      }

      if (var1 instanceof IsoZombie) {
         ((IsoZombie)var1).networkAI.isClimbing = false;
      }

      Pool.tryRelease(var2.get(PARAM_PARAMS));
      var2.clear();
   }

   public static void slideX(IsoGameCharacter var0, float var1) {
      float var2 = 0.05F * GameTime.getInstance().getThirtyFPSMultiplier();
      var2 = var1 > var0.getX() ? Math.min(var2, var1 - var0.getX()) : Math.max(-var2, var1 - var0.getX());
      var0.setX(var0.getX() + var2);
      var0.setNextX(var0.getX());
   }

   public static void slideY(IsoGameCharacter var0, float var1) {
      float var2 = 0.05F * GameTime.getInstance().getThirtyFPSMultiplier();
      var2 = var1 > var0.getY() ? Math.min(var2, var1 - var0.getY()) : Math.max(-var2, var1 - var0.getY());
      var0.setY(var0.getY() + var2);
      var0.setNextY(var0.getY());
   }

   public void animEvent(IsoGameCharacter var1, AnimEvent var2) {
      HashMap var3 = var1.getStateMachineParams(this);
      IsoPlayer var4 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      IsoZombie var5 = (IsoZombie)Type.tryCastTo(var1, IsoZombie.class);
      if (var2.m_EventName.equalsIgnoreCase("CheckAttack") && var5 != null && var5.target instanceof IsoGameCharacter) {
         ((IsoGameCharacter)var5.target).attackFromWindowsLunge(var5);
      }

      if (var2.m_EventName.equalsIgnoreCase("OnFloor") && var5 != null) {
         boolean var6 = Boolean.parseBoolean(var2.m_ParameterValue);
         var3.put(PARAM_ZOMBIE_ON_FLOOR, var6);
         if (var6) {
            this.setLungeXVars(var5);
            IsoThumpable var7 = (IsoThumpable)Type.tryCastTo(this.getWindow(var1), IsoThumpable.class);
            if (var7 != null && var7.getSquare() != null && var5.target != null) {
               var7.Health -= Rand.Next(10, 20);
               if (var7.Health <= 0) {
                  var7.destroy();
               }
            }

            var1.setVariable("ClimbWindowFlopped", true);
         }
      }

      if (var2.m_EventName.equalsIgnoreCase("PlayerVoiceSound")) {
         if (var1.getVariableBoolean("PlayerVoiceSound")) {
            return;
         }

         if (var4 == null) {
            return;
         }

         var1.setVariable("PlayerVoiceSound", true);
         var4.playerVoiceSound(var2.m_ParameterValue);
      }

      if (var2.m_EventName.equalsIgnoreCase("PlayWindowSound")) {
         if (!SoundManager.instance.isListenerInRange(var1.getX(), var1.getY(), 10.0F)) {
            return;
         }

         long var9 = var1.getEmitter().playSoundImpl(var2.m_ParameterValue, (IsoObject)null);
         var1.getEmitter().setParameterValue(var9, FMODManager.instance.getParameterDescription("TripObstacleType"), 9.0F);
      }

      if (var2.m_EventName.equalsIgnoreCase("SetState")) {
         if (var5 == null) {
            return;
         }

         try {
            ParameterZombieState.State var10 = ParameterZombieState.State.valueOf(var2.m_ParameterValue);
            var5.parameterZombieState.setState(var10);
         } catch (IllegalArgumentException var8) {
         }
      }

   }

   public boolean isIgnoreCollide(IsoGameCharacter var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      HashMap var8 = var1.getStateMachineParams(this);
      ClimbThroughWindowPositioningParams var9 = (ClimbThroughWindowPositioningParams)var8.get(PARAM_PARAMS);
      int var10 = var9.startX;
      int var11 = var9.startY;
      int var12 = var9.endX;
      int var13 = var9.endY;
      int var14 = var9.z;
      if (var14 == var4 && var14 == var7) {
         int var15 = PZMath.min(var10, var12);
         int var16 = PZMath.min(var11, var13);
         int var17 = PZMath.max(var10, var12);
         int var18 = PZMath.max(var11, var13);
         int var19 = PZMath.min(var2, var5);
         int var20 = PZMath.min(var3, var6);
         int var21 = PZMath.max(var2, var5);
         int var22 = PZMath.max(var3, var6);
         return var15 <= var19 && var16 <= var20 && var17 >= var21 && var18 >= var22;
      } else {
         return false;
      }
   }

   public IsoObject getWindow(IsoGameCharacter var1) {
      if (!var1.isCurrentState(this)) {
         return null;
      } else {
         HashMap var2 = var1.getStateMachineParams(this);
         ClimbThroughWindowPositioningParams var3 = (ClimbThroughWindowPositioningParams)var2.get(PARAM_PARAMS);
         int var4 = var3.startX;
         int var5 = var3.startY;
         int var6 = var3.z;
         IsoGridSquare var7 = IsoWorld.instance.CurrentCell.getGridSquare(var4, var5, var6);
         int var8 = var3.endX;
         int var9 = var3.endY;
         IsoGridSquare var10 = IsoWorld.instance.CurrentCell.getGridSquare(var8, var9, var6);
         if (var7 != null && var10 != null) {
            Object var11 = var7.getWindowTo(var10);
            if (var11 == null) {
               var11 = var7.getWindowThumpableTo(var10);
            }

            if (var11 == null) {
               var11 = var7.getHoppableTo(var10);
            }

            return (IsoObject)var11;
         } else {
            return null;
         }
      }
   }

   public boolean isWindowClosing(IsoGameCharacter var1) {
      HashMap var2 = var1.getStateMachineParams(this);
      ClimbThroughWindowPositioningParams var3 = (ClimbThroughWindowPositioningParams)var2.get(PARAM_PARAMS);
      if (var1.getVariableBoolean("ClimbWindowStarted")) {
         return false;
      } else {
         int var4 = var3.startX;
         int var5 = var3.startY;
         int var6 = var3.z;
         IsoGridSquare var7 = IsoWorld.instance.CurrentCell.getGridSquare(var4, var5, var6);
         if (var1.getCurrentSquare() != var7) {
            return false;
         } else {
            IsoWindow var8 = (IsoWindow)Type.tryCastTo(this.getWindow(var1), IsoWindow.class);
            if (var8 == null) {
               return false;
            } else {
               IsoGameCharacter var9 = var8.getFirstCharacterClosing();
               if (var9 != null && var9.isVariable("CloseWindowOutcome", "success")) {
                  if (var1.isZombie()) {
                     var1.setHitReaction("HeadLeft");
                  } else {
                     var1.setVariable("ClimbWindowFinished", true);
                  }

                  return true;
               } else {
                  return false;
               }
            }
         }
      }
   }

   public void getDeltaModifiers(IsoGameCharacter var1, MoveDeltaModifiers var2) {
      boolean var3 = var1.getPath2() != null;
      boolean var4 = var1 instanceof IsoPlayer;
      if (var3 && var4) {
         var2.setMaxTurnDelta(2.0F);
      }

      if (var4 && var1.getVariableBoolean("isTurning")) {
         var2.setMaxTurnDelta(2.0F);
      }

   }

   public static boolean isFreeSquare(IsoGridSquare var0) {
      return var0 != null && var0.TreatAsSolidFloor() && !var0.Is(IsoFlagType.solid) && !var0.Is(IsoFlagType.solidtrans);
   }

   public static boolean isObstacleSquare(IsoGridSquare var0) {
      return var0 != null && var0.TreatAsSolidFloor() && !var0.Is(IsoFlagType.solid) && var0.Is(IsoFlagType.solidtrans) && !var0.Is(IsoFlagType.water);
   }

   public static IsoGridSquare getFreeSquareAfterObstacles(IsoGridSquare var0, IsoDirections var1) {
      while(true) {
         IsoGridSquare var2 = var0.getAdjacentSquare(var1);
         if (var2 == null || var0.isSomethingTo(var2) || var0.getWindowFrameTo(var2) != null || var0.getWindowThumpableTo(var2) != null) {
            return null;
         }

         if (isFreeSquare(var2)) {
            return var2;
         }

         if (!isObstacleSquare(var2)) {
            return null;
         }

         var0 = var2;
      }
   }

   private void setLungeXVars(IsoZombie var1) {
      IsoMovingObject var2 = var1.getTarget();
      if (var2 != null) {
         var1.setVariable("FenceLungeX", 0.0F);
         var1.setVariable("FenceLungeY", 0.0F);
         float var3 = 0.0F;
         Vector2 var4 = var1.getForwardDirection();
         PZMath.SideOfLine var5 = PZMath.testSideOfLine(var1.getX(), var1.getY(), var1.getX() + var4.x, var1.getY() + var4.y, var2.getX(), var2.getY());
         float var6 = (float)Math.acos((double)var1.getDotWithForwardDirection(var2.getX(), var2.getY()));
         float var7 = PZMath.clamp(PZMath.radToDeg(var6), 0.0F, 90.0F);
         switch (var5) {
            case Left:
               var3 = -var7 / 90.0F;
               break;
            case OnLine:
               var3 = 0.0F;
               break;
            case Right:
               var3 = var7 / 90.0F;
         }

         var1.setVariable("FenceLungeX", var3);
      }
   }

   public boolean isPastInnerEdgeOfSquare(IsoGameCharacter var1, int var2, int var3, IsoDirections var4) {
      if (var4 == IsoDirections.N) {
         return var1.getY() < (float)(var3 + 1) - 0.3F;
      } else if (var4 == IsoDirections.S) {
         return var1.getY() > (float)var3 + 0.3F;
      } else if (var4 == IsoDirections.W) {
         return var1.getX() < (float)(var2 + 1) - 0.3F;
      } else if (var4 == IsoDirections.E) {
         return var1.getX() > (float)var2 + 0.3F;
      } else {
         throw new IllegalArgumentException("unhandled direction");
      }
   }

   public boolean isPastOuterEdgeOfSquare(IsoGameCharacter var1, int var2, int var3, IsoDirections var4) {
      if (var4 == IsoDirections.N) {
         return var1.getY() < (float)var3 - 0.3F;
      } else if (var4 == IsoDirections.S) {
         return var1.getY() > (float)(var3 + 1) + 0.3F;
      } else if (var4 == IsoDirections.W) {
         return var1.getX() < (float)var2 - 0.3F;
      } else if (var4 == IsoDirections.E) {
         return var1.getX() > (float)(var2 + 1) + 0.3F;
      } else {
         throw new IllegalArgumentException("unhandled direction");
      }
   }

   public void setParams(IsoGameCharacter var1, IsoObject var2) {
      HashMap var3 = var1.getStateMachineParams(this);
      ClimbThroughWindowPositioningParams var4 = (ClimbThroughWindowPositioningParams)var3.computeIfAbsent(PARAM_PARAMS, (var0) -> {
         return ClimbThroughWindowPositioningParams.alloc();
      });
      getClimbThroughWindowPositioningParams(var1, var2, var4);
      if (var4.windowObject == null) {
         throw new IllegalArgumentException("No valid climb-throuwh portal found. Expected thumpable, window, or window-frame");
      } else {
         var3.put(PARAM_ZOMBIE_ON_FLOOR, Boolean.FALSE);
         var3.put(PARAM_PREV_STATE, var4.climbingCharacter.getCurrentState());
      }
   }

   public static void getClimbThroughWindowPositioningParams(IsoGameCharacter var0, IsoObject var1, ClimbThroughWindowPositioningParams var2) {
      boolean var4 = false;
      boolean var3;
      if (var1 instanceof IsoWindow var6) {
         var2.canClimb = var6.canClimbThrough(var0);
         var3 = var6.north;
         if (var0 instanceof IsoPlayer && var6.isDestroyed() && !var6.isGlassRemoved() && Rand.Next(2) == 0) {
            var4 = true;
         }
      } else if (var1 instanceof IsoThumpable var7) {
         var2.canClimb = var7.canClimbThrough(var0);
         var3 = var7.north;
         if (var0 instanceof IsoPlayer && var7.getName().equals("Barbed Fence") && Rand.Next(101) > 75) {
            var4 = true;
         }
      } else {
         if (!(var1 instanceof IsoWindowFrame)) {
            var2.canClimb = false;
            var2.climbingCharacter = var0;
            var2.windowObject = null;
            return;
         }

         IsoWindowFrame var5 = (IsoWindowFrame)var1;
         var2.canClimb = true;
         var3 = var5.getNorth();
      }

      int var22 = var1.getSquare().getX();
      int var8 = var1.getSquare().getY();
      int var9 = var1.getSquare().getZ();
      int var10 = var22;
      int var11 = var8;
      int var12 = var22;
      int var13 = var8;
      IsoDirections var21;
      if (var3) {
         if ((float)var8 < var0.getY()) {
            var13 = var8 - 1;
            var21 = IsoDirections.N;
         } else {
            var11 = var8 - 1;
            var21 = IsoDirections.S;
         }
      } else if ((float)var22 < var0.getX()) {
         var12 = var22 - 1;
         var21 = IsoDirections.W;
      } else {
         var10 = var22 - 1;
         var21 = IsoDirections.E;
      }

      IsoGridSquare var14 = IsoWorld.instance.CurrentCell.getGridSquare(var12, var13, var9);
      boolean var15 = var14 != null && var14.Is(IsoFlagType.solidtrans);
      boolean var16 = var14 != null && var14.TreatAsSolidFloor();
      boolean var17 = var14 != null && var0.canClimbDownSheetRope(var14);
      int var18 = var12;
      int var19 = var13;
      IsoGridSquare var20;
      if (var15 && var0.isZombie()) {
         var20 = var14.getAdjacentSquare(var21);
         if (isFreeSquare(var20) && !var14.isSomethingTo(var20) && var14.getWindowFrameTo(var20) == null && var14.getWindowThumpableTo(var20) == null) {
            var18 = var20.x;
            var19 = var20.y;
         } else {
            var15 = false;
         }
      }

      if (var15 && !var0.isZombie()) {
         var20 = getFreeSquareAfterObstacles(var14, var21);
         if (var20 == null) {
            var15 = false;
         } else {
            var18 = var20.x;
            var19 = var20.y;
         }
      }

      var2.climbDir = var21;
      var2.climbingCharacter = var0;
      var2.windowObject = var1;
      var2.startX = var10;
      var2.startY = var11;
      var2.z = var9;
      var2.oppositeX = var12;
      var2.oppositeY = var13;
      var2.endX = var18;
      var2.endY = var19;
      var2.scratch = var4;
      var2.isCounter = var15;
      var2.isFloor = var16;
      var2.isSheetRope = var17;
   }

   public ClimbThroughWindowPositioningParams getPositioningParams(IsoGameCharacter var1) {
      HashMap var2 = var1.getStateMachineParams(this);
      return (ClimbThroughWindowPositioningParams)var2.get(PARAM_PARAMS);
   }
}
