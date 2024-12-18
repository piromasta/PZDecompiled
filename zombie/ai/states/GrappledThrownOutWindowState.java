package zombie.ai.states;

import fmod.fmod.FMODManager;
import java.util.HashMap;
import zombie.GameTime;
import zombie.SoundManager;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
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
import zombie.util.Type;

public class GrappledThrownOutWindowState extends State {
   private static final GrappledThrownOutWindowState _instance = new GrappledThrownOutWindowState();
   static final Integer PARAM_START_X = 0;
   static final Integer PARAM_START_Y = 1;
   static final Integer PARAM_Z = 2;
   static final Integer PARAM_OPPOSITE_X = 3;
   static final Integer PARAM_OPPOSITE_Y = 4;
   static final Integer PARAM_DIR = 5;
   static final Integer PARAM_ZOMBIE_ON_FLOOR = 6;
   static final Integer PARAM_PREV_STATE = 7;
   static final Integer PARAM_SCRATCH = 8;
   static final Integer PARAM_COUNTER = 9;
   static final Integer PARAM_SOLID_FLOOR = 10;
   static final Integer PARAM_SHEET_ROPE = 11;
   static final Integer PARAM_END_X = 12;
   static final Integer PARAM_END_Y = 13;

   public GrappledThrownOutWindowState() {
   }

   public static GrappledThrownOutWindowState instance() {
      return _instance;
   }

   public void enter(IsoGameCharacter var1) {
      IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      var1.setIgnoreMovement(true);
      var1.setHideWeaponModel(true);
      HashMap var3 = var1.getStateMachineParams(this);
      boolean var4 = var3.get(PARAM_COUNTER) == Boolean.TRUE;
      var1.setVariable("ClimbWindowStarted", false);
      var1.setVariable("ClimbWindowEnd", false);
      var1.setVariable("ClimbWindowFinished", false);
      var1.clearVariable("ClimbWindowGetUpBack");
      var1.clearVariable("ClimbWindowGetUpFront");
      var1.setVariable("ClimbWindowOutcome", var4 ? "obstacle" : "success");
      var1.clearVariable("ClimbWindowFlopped");
      IsoZombie var5 = (IsoZombie)Type.tryCastTo(var1, IsoZombie.class);
      if (!var4 && var5 != null && var5.shouldDoFenceLunge()) {
         this.setLungeXVars(var5);
         var1.setVariable("ClimbWindowOutcome", "lunge");
      }

      if (var3.get(PARAM_SOLID_FLOOR) == Boolean.FALSE) {
         var1.setVariable("ClimbWindowOutcome", "fall");
      }

      if (!(var1 instanceof IsoZombie) && var3.get(PARAM_SHEET_ROPE) == Boolean.TRUE) {
         var1.setVariable("ClimbWindowOutcome", "rope");
      }

      if (var2 != null && var2.isLocalPlayer()) {
         var2.dirtyRecalcGridStackTime = 20.0F;
         var2.triggerMusicIntensityEvent("ClimbThroughWindow");
      }

   }

   public void execute(IsoGameCharacter var1) {
      HashMap var2 = var1.getStateMachineParams(this);
      IsoPlayer var3 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      if (!this.isWindowClosing(var1)) {
         IsoDirections var4 = (IsoDirections)var2.get(PARAM_DIR);
         if (var4 != null) {
            if (var1.getVariableBoolean("ClimbWindowStarted", false)) {
               if (var1.isFallOnFront()) {
                  var1.setDir(var4.Rot180());
               } else {
                  var1.setDir(var4);
               }
            }

            String var5 = var1.getVariableString("ClimbWindowOutcome");
            float var6 = (float)(Integer)var2.get(PARAM_START_X) + 0.5F;
            float var7 = (float)(Integer)var2.get(PARAM_START_Y) + 0.5F;
            float var8;
            float var9;
            if (var1 instanceof IsoPlayer && var5.equalsIgnoreCase("obstacle")) {
               var8 = (float)(Integer)var2.get(PARAM_END_X) + 0.5F;
               var9 = (float)(Integer)var2.get(PARAM_END_Y) + 0.5F;
               if (var1.DistToSquared(var8, var9) < 0.5625F) {
                  var1.setVariable("ClimbWindowOutcome", "obstacleEnd");
               }
            }

            if (var1 instanceof IsoPlayer && !var1.getVariableBoolean("ClimbWindowEnd") && !"fallfront".equals(var5) && !"back".equals(var5) && !"fallback".equals(var5)) {
               int var12 = (Integer)var2.get(PARAM_OPPOSITE_X);
               int var13 = (Integer)var2.get(PARAM_OPPOSITE_Y);
               int var10 = (Integer)var2.get(PARAM_Z);
               IsoGridSquare var11 = IsoWorld.instance.CurrentCell.getGridSquare(var12, var13, var10);
               if (var11 != null) {
                  this.checkForFallingBack(var11, var1);
                  if (var11 != var1.getSquare() && var11.TreatAsSolidFloor()) {
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

            if (var1.getVariableBoolean("TransitioningThroughWindow") && !"back".equals(var5) && !"fallback".equals(var5) && !"lunge".equals(var5) && !"obstacle".equals(var5) && !"obstacleEnd".equals(var5)) {
               if (var1.getX() != var6 && (var4 == IsoDirections.N || var4 == IsoDirections.S)) {
                  this.slideX(var1, var6, 0.25F);
               }

               if (var1.getY() != var7 && (var4 == IsoDirections.W || var4 == IsoDirections.E)) {
                  this.slideY(var1, var7, 0.25F);
               }

               var8 = (float)(Integer)var2.get(PARAM_START_X);
               var9 = (float)(Integer)var2.get(PARAM_START_Y);
               switch (var4) {
                  case N:
                     var9 -= 0.1F;
                     break;
                  case S:
                     ++var9;
                     break;
                  case W:
                     var8 -= 0.1F;
                     break;
                  case E:
                     ++var8;
               }

               if (PZMath.fastfloor(var1.getX()) != PZMath.fastfloor(var8) && (var4 == IsoDirections.W || var4 == IsoDirections.E)) {
                  this.slideX(var1, var8, 0.1F);
               }

               if (PZMath.fastfloor(var1.getY()) != PZMath.fastfloor(var9) && (var4 == IsoDirections.N || var4 == IsoDirections.S)) {
                  this.slideY(var1, var9, 0.1F);
               }
            }

            if (var1.getVariableBoolean("ClimbWindowStarted") && var2.get(PARAM_SCRATCH) == Boolean.TRUE) {
               var2.put(PARAM_SCRATCH, Boolean.FALSE);
               var1.getBodyDamage().setScratchedWindow();
               if (var3 != null) {
                  var3.playerVoiceSound("PainFromGlassCut");
               }
            }

            if (var1.getVariableBoolean("ClimbWindowStarted") && var1.isVariable("ClimbWindowOutcome", "fall")) {
               var1.setFallTime(Math.max(var1.getFallTime(), 2.1F));
            }

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
      var1.clearVariable("grappledThrownOutWindow");
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

   }

   public void slideX(IsoGameCharacter var1, float var2, float var3) {
      float var4 = var3 * GameTime.getInstance().getThirtyFPSMultiplier();
      var4 = var2 > var1.getX() ? Math.min(var4, var2 - var1.getX()) : Math.max(-var4, var2 - var1.getX());
      var1.setX(var1.getX() + var4);
      var1.setNextX(var1.getX());
   }

   public void slideY(IsoGameCharacter var1, float var2, float var3) {
      float var4 = var3 * GameTime.getInstance().getThirtyFPSMultiplier();
      var4 = var2 > var1.getY() ? Math.min(var4, var2 - var1.getY()) : Math.max(-var4, var2 - var1.getY());
      var1.setY(var1.getY() + var4);
      var1.setNextY(var1.getY());
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
      return true;
   }

   public IsoObject getWindow(IsoGameCharacter var1) {
      if (!var1.isCurrentState(this)) {
         return null;
      } else {
         HashMap var2 = var1.getStateMachineParams(this);
         int var3 = (Integer)var2.get(PARAM_START_X);
         int var4 = (Integer)var2.get(PARAM_START_Y);
         int var5 = (Integer)var2.get(PARAM_Z);
         IsoGridSquare var6 = IsoWorld.instance.CurrentCell.getGridSquare(var3, var4, var5);
         int var7 = (Integer)var2.get(PARAM_END_X);
         int var8 = (Integer)var2.get(PARAM_END_Y);
         IsoGridSquare var9 = IsoWorld.instance.CurrentCell.getGridSquare(var7, var8, var5);
         if (var6 != null && var9 != null) {
            Object var10 = var6.getWindowTo(var9);
            if (var10 == null) {
               var10 = var6.getWindowThumpableTo(var9);
            }

            if (var10 == null) {
               var10 = var6.getHoppableTo(var9);
            }

            return (IsoObject)var10;
         } else {
            return null;
         }
      }
   }

   public boolean isWindowClosing(IsoGameCharacter var1) {
      HashMap var2 = var1.getStateMachineParams(this);
      if (var1.getVariableBoolean("ClimbWindowStarted")) {
         return false;
      } else {
         int var3 = (Integer)var2.computeIfAbsent(PARAM_START_X, (var0) -> {
            return 0;
         });
         int var4 = (Integer)var2.computeIfAbsent(PARAM_START_Y, (var0) -> {
            return 0;
         });
         int var5 = (Integer)var2.computeIfAbsent(PARAM_Z, (var0) -> {
            return 0;
         });
         IsoGridSquare var6 = IsoWorld.instance.CurrentCell.getGridSquare(var3, var4, var5);
         if (var1.getCurrentSquare() != var6) {
            return false;
         } else {
            IsoWindow var7 = (IsoWindow)Type.tryCastTo(this.getWindow(var1), IsoWindow.class);
            if (var7 == null) {
               return false;
            } else {
               IsoGameCharacter var8 = var7.getFirstCharacterClosing();
               if (var8 != null && var8.isVariable("CloseWindowOutcome", "success")) {
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
         var2.setMaxTurnDelta(10.0F);
      }

      if (var4 && var1.getVariableBoolean("isTurning")) {
         var2.setMaxTurnDelta(5.0F);
      }

   }

   private boolean isFreeSquare(IsoGridSquare var1) {
      return var1 != null && var1.TreatAsSolidFloor() && !var1.Is(IsoFlagType.solid) && !var1.Is(IsoFlagType.solidtrans);
   }

   private boolean isObstacleSquare(IsoGridSquare var1) {
      return var1 != null && var1.TreatAsSolidFloor() && !var1.Is(IsoFlagType.solid) && var1.Is(IsoFlagType.solidtrans) && !var1.Is(IsoFlagType.water);
   }

   private IsoGridSquare getFreeSquareAfterObstacles(IsoGridSquare var1, IsoDirections var2) {
      while(true) {
         IsoGridSquare var3 = var1.getAdjacentSquare(var2);
         if (var3 == null || var1.isSomethingTo(var3) || var1.getWindowFrameTo(var3) != null || var1.getWindowThumpableTo(var3) != null) {
            return null;
         }

         if (this.isFreeSquare(var3)) {
            return var3;
         }

         if (!this.isObstacleSquare(var3)) {
            return null;
         }

         var1 = var3;
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
      var3.clear();
      boolean var5 = false;
      boolean var4;
      if (var2 instanceof IsoWindow var7) {
         var4 = var7.north;
         if (var1 instanceof IsoPlayer && var7.isDestroyed() && !var7.isGlassRemoved() && Rand.Next(2) == 0) {
            var5 = true;
         }
      } else if (var2 instanceof IsoThumpable var8) {
         var4 = var8.north;
         if (var1 instanceof IsoPlayer && var8.getName().equals("Barbed Fence") && Rand.Next(101) > 75) {
            var5 = true;
         }
      } else {
         if (!(var2 instanceof IsoWindowFrame)) {
            throw new IllegalArgumentException("expected thumpable, window, or window-frame");
         }

         IsoWindowFrame var6 = (IsoWindowFrame)var2;
         var4 = var6.getNorth();
      }

      int var23 = var2.getSquare().getX();
      int var9 = var2.getSquare().getY();
      int var10 = var2.getSquare().getZ();
      int var11 = var23;
      int var12 = var9;
      int var13 = var23;
      int var14 = var9;
      IsoDirections var22;
      if (var4) {
         if ((float)var9 < var1.getY()) {
            var14 = var9 - 1;
            var22 = IsoDirections.N;
         } else {
            var12 = var9 - 1;
            var22 = IsoDirections.S;
         }
      } else if ((float)var23 < var1.getX()) {
         var13 = var23 - 1;
         var22 = IsoDirections.W;
      } else {
         var11 = var23 - 1;
         var22 = IsoDirections.E;
      }

      IsoGridSquare var15 = IsoWorld.instance.CurrentCell.getGridSquare(var13, var14, var10);
      boolean var16 = var15 != null && var15.Is(IsoFlagType.solidtrans);
      boolean var17 = var15 != null && var15.TreatAsSolidFloor();
      boolean var18 = var15 != null && var1.canClimbDownSheetRope(var15);
      int var19 = var13;
      int var20 = var14;
      IsoGridSquare var21;
      if (var16 && var1.isZombie()) {
         var21 = var15.getAdjacentSquare(var22);
         if (this.isFreeSquare(var21) && !var15.isSomethingTo(var21) && var15.getWindowFrameTo(var21) == null && var15.getWindowThumpableTo(var21) == null) {
            var19 = var21.x;
            var20 = var21.y;
         } else {
            var16 = false;
         }
      }

      if (var16 && !var1.isZombie()) {
         var21 = this.getFreeSquareAfterObstacles(var15, var22);
         if (var21 == null) {
            var16 = false;
         } else {
            var19 = var21.x;
            var20 = var21.y;
         }
      }

      var3.put(PARAM_START_X, var11);
      var3.put(PARAM_START_Y, var12);
      var3.put(PARAM_Z, var10);
      var3.put(PARAM_OPPOSITE_X, var13);
      var3.put(PARAM_OPPOSITE_Y, var14);
      var3.put(PARAM_END_X, var19);
      var3.put(PARAM_END_Y, var20);
      var3.put(PARAM_DIR, var22);
      var3.put(PARAM_ZOMBIE_ON_FLOOR, Boolean.FALSE);
      var3.put(PARAM_PREV_STATE, var1.getCurrentState());
      var3.put(PARAM_SCRATCH, var5 ? Boolean.TRUE : Boolean.FALSE);
      var3.put(PARAM_COUNTER, var16 ? Boolean.TRUE : Boolean.FALSE);
      var3.put(PARAM_SOLID_FLOOR, var17 ? Boolean.TRUE : Boolean.FALSE);
      var3.put(PARAM_SHEET_ROPE, var18 ? Boolean.TRUE : Boolean.FALSE);
   }
}
