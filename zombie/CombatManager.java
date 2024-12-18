package zombie;

import fmod.fmod.FMODManager;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.util.vector.Matrix4f;
import zombie.Lua.LuaEventManager;
import zombie.ai.states.AttackState;
import zombie.ai.states.FakeDeadZombieState;
import zombie.ai.states.FishingState;
import zombie.ai.states.PlayerHitReactionPVPState;
import zombie.ai.states.PlayerHitReactionState;
import zombie.ai.states.SwipeStatePlayer;
import zombie.ai.states.ZombieEatBodyState;
import zombie.ai.states.ZombieGetUpState;
import zombie.ai.states.ZombieOnGroundState;
import zombie.audio.parameters.ParameterMeleeHitSurface;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.Faction;
import zombie.characters.HitReactionNetworkAI;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoLivingCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Stats;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.Moodles.MoodleType;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.combat.MeleeTargetComparator;
import zombie.combat.RangeTargetComparator;
import zombie.combat.TargetComparator;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.physics.BallisticsController;
import zombie.core.physics.BallisticsTarget;
import zombie.core.physics.RagdollBodyPart;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.textures.ColorInfo;
import zombie.debug.BaseDebugWindow;
import zombie.debug.DebugContext;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.debug.debugWindows.TargetHitInfoPanel;
import zombie.input.GameKeyboard;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponType;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoGridSquareCollisionData;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.enums.MaterialType;
import zombie.iso.objects.IsoBulletTracerEffects;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoReticle;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.network.fields.AttackVars;
import zombie.network.fields.HitInfo;
import zombie.pathfind.PolygonalMap2;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.VehicleScript;
import zombie.ui.MoodlesUI;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public final class CombatManager {
   private static CombatManager combatManager;
   private static final ArrayList<HitInfo> HitList2 = new ArrayList();
   public final ObjectPool<HitInfo> hitInfoPool = new ObjectPool(HitInfo::new);
   private static final float BallisticsTargetsHighlightAlpha = 0.65F;
   private static final float MeleeTargetsHighlightAlpha = 1.0F;
   private static final float DrivebyDotOptimalAngle = -0.7F;
   private static final float DrivebyDotMaximumAngle = -0.1F;
   private static final int DrivebyDotToHitMaximumPenalty = 40;
   public static final float OptimalRangeToHitMaximumBonus = 15.0F;
   private static final float OptimalRangeDropOffToHitPenalty = 4.0F;
   private static final float OptimalRangeDropOffToHitPenaltyIncrement = 0.3F;
   public static final float LowLightThreshold = 0.75F;
   public static final int LowLightToHitMaximumPenalty = 50;
   private static final int PointBlankToHitMaximumBonus = 40;
   public static final float PointBlankDistance = 3.0F;
   private static final float PointBlankDropOffToHitPenalty = 0.7F;
   public static float PostShotAimingDelayRecoilModifier = 0.25F;
   public static float PostShotAimingDelayAimingModifier = 0.05F;
   private static final float MinimumToHitChance = 5.0F;
   private static final float MaximumStartToHitChance = 95.0F;
   private static final float MaximumToHitChance = 100.0F;
   private static final float MovingToHitPenalty = 5.0F;
   private static final float RunningToHitPenalty = 15.0F;
   private static final float SprintingToHitPenalty = 25.0F;
   private static final float MarksmanTraitToHitBonus = 20.0F;
   private static final float ArmPainToHitModifier = 0.1F;
   private static final float PanicToHitBasePenalty = 8.0F;
   private static final float PanicToHitDistanceModifier = 0.5F;
   private static final float StressToHitBasePenalty = 8.0F;
   private static final float StressToHitDistanceModifier = 0.5F;
   private static final float TiredToHitBasePenalty = 5.0F;
   private static final float EnduranceToHitBasePenalty = 5.0F;
   private static final float DrunkToHitBasePenalty = 8.0F;
   private static final float DrunkToHitDistanceModifier = 0.5F;
   private static final float WindIntensityToHitPenalty = 6.0F;
   private static final float WindIntensityToHitAimingModifier = 0.2F;
   private static final float WindIntensityToHitMinimumMarksmanModifier = 0.6F;
   private static final float WindIntensityToHitMaximumMarksmanModifier = 1.0F;
   private static final float RainIntensityToHitDistanceModifier = 0.5F;
   private static final float FogIntensityDistanceModifier = 10.0F;
   private static final float PointBlankMaximumDistanceModifier = 1.0F;
   private static final float SightlessToHitBaseDistance = 15.0F;
   private static final float SightlessToHitProneModifier = 2.0F;
   private static final float SightlessAimDelayToHitDistanceModifier = 0.1F;
   private static final float VehicleDamageScaleFactor = 50.0F;
   public static final int StrengthLevelOffset = 15;
   public static float FirearmRecoilMuscleStrainModifier = 0.05F;
   public static final float StrengthLevelMuscleStrainModifier = 10.0F;
   public static final float TwoHandedWeaponMuscleStrainModifier = 0.5F;
   private static final float RecoilDelay = 10.0F;
   private static final float PainThreshold = 10.0F;
   private static final float MinPainFactor = 1.0F;
   private static final float MaxPainFactor = 30.0F;
   private static final float SuperAttackMultiplier = 5.0F;
   private static final int AimingLevelPanicThreshold = 6;
   private static final int PanicLevelAimingThreshold = 2;
   private static final float PanicLevelAimingDamageSplitModifier = 0.2F;
   private static final int StressLevelDamageReductionThreshold = 1;
   private static final int PanicLevelDamageReductionThreshold = 1;
   private static final float PanicLevelDamageSplitModifier = 0.1F;
   private static final float StressLevelDamageSplitModifier = 0.1F;
   private static final float MinBaseDamageSplitModifier = 0.1F;
   private static final float MinDamageSplit = 0.7F;
   private static final float MaxDamageSplit = 1.0F;
   private static final float StrengthPerkStompModifier = 0.2F;
   private static final float NoShoesDamageSplitModifier = 0.5F;
   private static final float EnduranceLevel1DamageSplitModifier = 0.5F;
   private static final float EnduranceLevel2DamageSplitModifier = 0.2F;
   private static final float EnduranceLevel3DamageSplitModifier = 0.1F;
   private static final float EnduranceLevel4DamageSplitModifier = 0.05F;
   private static final float TiredLevel1DamageSplitModifier = 0.5F;
   private static final float TiredLevel2DamageSplitModifier = 0.2F;
   private static final float TiredLevel3DamageSplitModifier = 0.1F;
   private static final float TiredLevel4DamageSplitModifier = 0.05F;
   private static final float HeadHitDamageSplitModifier = 3.0F;
   private static final float LegHitDamageSplitModifier = 0.05F;
   private static final float BaseBodyPartClothingDefenseModifier = 0.5F;
   private static final int ZombieMaxDefense = 100;
   private static final int AxeVsTreeBonusModifier = 2;
   private static final float UseChargeDelta = 3.0F;
   private static final int AdditionalCritChanceFromBehind = 30;
   private static final int AdditionalCritChanceDefault = 5;
   private static final float CriticalHitSpeedMultiplier = 1.1F;
   private static final float BreakMultiplierBase = 1.0F;
   private static final float BreakMultiplierChargeModifier = 1.5F;
   private static final float MinAngleFloorModifier = 1.5F;
   public static float BallisticsControllerDistanceThreshold = 2.0F;
   private static final Integer PARAM_LOWER_CONDITION = 0;
   private static final Integer PARAM_ATTACKED = 1;
   public static final int ISOCURSOR = 0;
   public static final int ISORETICLE = 1;
   public static int targetReticleMode = 0;
   private boolean bHitOnlyTree;
   private IsoTree treeHit;
   private IsoObject objHit;
   private final ArrayList<Float> dotList = new ArrayList();
   private static final ArrayList<IsoMovingObject> movingStatic = new ArrayList();
   private static final Vector3 tempVector3_1 = new Vector3();
   private static final Vector3 tempVector3_2 = new Vector3();
   private static final Vector2 tempVector2_1 = new Vector2();
   private static final Vector2 tempVector2_2 = new Vector2();
   private final Vector4f tempVector4f = new Vector4f();
   private static final Vector3 tempVectorBonePos = new Vector3();
   private static final float DefaultMaintenanceXP = 1.0F;
   private static final int ConditionLowerChance = 10;
   private static final Vector3f ballisticsDirectionVector = new Vector3f();
   private static final Vector3f ballisticsStartPosition = new Vector3f();
   private static final Vector3f ballisticsEndPosition = new Vector3f();
   private static final String BreakLightBulbSound = "SmashWindow";
   private static final Color OccludedTargetDebugColor;
   private static final Color TargetableDebugColor;
   private static final float TargetDebugAlpha = 1.0F;
   private static final float VehicleTargetDebugRadius = 1.5F;
   private static final float CharacterTargetDebugRadius = 0.1F;
   private final MeleeTargetComparator meleeTargetComparator = new MeleeTargetComparator();
   private final RangeTargetComparator rangeTargetComparator = new RangeTargetComparator();
   private final WindowVisitor windowVisitor = new WindowVisitor();

   public static CombatManager getInstance() {
      if (combatManager == null) {
         combatManager = new CombatManager();
      }

      return combatManager;
   }

   public CombatManager() {
   }

   public float calculateDamageToVehicle(IsoGameCharacter var1, float var2, float var3, int var4) {
      if (var2 == 0.0F) {
         return (float)var4;
      } else {
         return !(var3 <= 0.0F) && var4 != 0 ? var3 * 50.0F * ((float)var4 / 10.0F) / var2 : 0.0F;
      }
   }

   private void setParameterCharacterHitResult(IsoGameCharacter var1, IsoZombie var2, long var3) {
      if (var3 != 0L) {
         byte var5 = 0;
         if (var2 != null) {
            if (var2.isDead()) {
               var5 = 2;
            } else if (var2.isKnockedDown()) {
               var5 = 1;
            }
         }

         var1.getEmitter().setParameterValue(var3, FMODManager.instance.getParameterDescription("CharacterHitResult"), (float)var5);
      }
   }

   public HandWeapon getWeapon(IsoGameCharacter var1) {
      return var1.getAttackingWeapon();
   }

   private boolean checkObjectHit(IsoGameCharacter var1, HandWeapon var2, IsoGridSquare var3, boolean var4, boolean var5) {
      if (var3 == null) {
         return false;
      } else {
         for(int var6 = var3.getSpecialObjects().size() - 1; var6 >= 0; --var6) {
            IsoObject var7 = (IsoObject)var3.getSpecialObjects().get(var6);
            IsoDoor var8 = (IsoDoor)Type.tryCastTo(var7, IsoDoor.class);
            IsoThumpable var9 = (IsoThumpable)Type.tryCastTo(var7, IsoThumpable.class);
            IsoWindow var10 = (IsoWindow)Type.tryCastTo(var7, IsoWindow.class);
            IsoCompost var11 = (IsoCompost)Type.tryCastTo(var7, IsoCompost.class);
            Thumpable var12;
            if (var8 != null && (var4 && var8.north || var5 && !var8.north)) {
               var12 = var8.getThumpableFor(var1);
               if (var12 != null) {
                  var12.WeaponHit(var1, var2);
                  this.objHit = var8;
                  return true;
               }
            }

            if (var9 != null) {
               if (!var9.isDoor() && !var9.isWindow() && var9.isBlockAllTheSquare()) {
                  var12 = var9.getThumpableFor(var1);
                  if (var12 != null) {
                     var12.WeaponHit(var1, var2);
                     this.objHit = var9;
                     return true;
                  }
               } else if (var4 && var9.north || var5 && !var9.north) {
                  var12 = var9.getThumpableFor(var1);
                  if (var12 != null) {
                     var12.WeaponHit(var1, var2);
                     this.objHit = var9;
                     return true;
                  }
               }
            }

            if (var10 != null && (var4 && var10.north || var5 && !var10.north)) {
               var12 = var10.getThumpableFor(var1);
               if (var12 != null) {
                  var12.WeaponHit(var1, var2);
                  this.objHit = var10;
                  return true;
               }
            }

            if (var11 != null) {
               var12 = var11.getThumpableFor(var1);
               if (var12 != null) {
                  var12.WeaponHit(var1, var2);
                  this.objHit = var11;
                  return true;
               }
            }
         }

         return false;
      }
   }

   private boolean CheckObjectHit(IsoGameCharacter var1, HandWeapon var2) {
      if (var1.isAimAtFloor()) {
         this.bHitOnlyTree = false;
         return false;
      } else {
         boolean var3 = false;
         int var4 = 0;
         int var5 = 0;
         IsoDirections var6 = IsoDirections.fromAngle(var1.getForwardDirection());
         int var7 = 0;
         int var8 = 0;
         if (var6 == IsoDirections.NE || var6 == IsoDirections.N || var6 == IsoDirections.NW) {
            --var8;
         }

         if (var6 == IsoDirections.SE || var6 == IsoDirections.S || var6 == IsoDirections.SW) {
            ++var8;
         }

         if (var6 == IsoDirections.NW || var6 == IsoDirections.W || var6 == IsoDirections.SW) {
            --var7;
         }

         if (var6 == IsoDirections.NE || var6 == IsoDirections.E || var6 == IsoDirections.SE) {
            ++var7;
         }

         IsoCell var9 = IsoWorld.instance.CurrentCell;
         IsoGridSquare var10 = var1.getCurrentSquare();
         IsoGridSquare var11 = var9.getGridSquare(var10.getX() + var7, var10.getY() + var8, var10.getZ());
         if (var11 != null) {
            if (this.checkObjectHit(var1, var2, var11, false, false)) {
               var3 = true;
               ++var4;
            }

            if (!var11.isBlockedTo(var10)) {
               for(int var12 = 0; var12 < var11.getObjects().size(); ++var12) {
                  IsoObject var13 = (IsoObject)var11.getObjects().get(var12);
                  if (var13 instanceof IsoTree) {
                     this.treeHit = (IsoTree)var13;
                     var3 = true;
                     ++var4;
                     ++var5;
                     if (var13.getObjectIndex() == -1) {
                        --var12;
                     }
                  }
               }
            }
         }

         if ((var6 == IsoDirections.NE || var6 == IsoDirections.N || var6 == IsoDirections.NW) && this.checkObjectHit(var1, var2, var10, true, false)) {
            var3 = true;
            ++var4;
         }

         IsoGridSquare var14;
         if (var6 == IsoDirections.SE || var6 == IsoDirections.S || var6 == IsoDirections.SW) {
            var14 = var9.getGridSquare(var10.getX(), var10.getY() + 1, var10.getZ());
            if (this.checkObjectHit(var1, var2, var14, true, false)) {
               var3 = true;
               ++var4;
            }
         }

         if (var6 == IsoDirections.SE || var6 == IsoDirections.E || var6 == IsoDirections.NE) {
            var14 = var9.getGridSquare(var10.getX() + 1, var10.getY(), var10.getZ());
            if (this.checkObjectHit(var1, var2, var14, false, true)) {
               var3 = true;
               ++var4;
            }
         }

         if ((var6 == IsoDirections.NW || var6 == IsoDirections.W || var6 == IsoDirections.SW) && this.checkObjectHit(var1, var2, var10, false, true)) {
            var3 = true;
            ++var4;
         }

         this.bHitOnlyTree = var3 && var4 == var5;
         return var3;
      }
   }

   public void splash(IsoMovingObject var1, HandWeapon var2, IsoGameCharacter var3) {
      IsoGameCharacter var4 = (IsoGameCharacter)var1;
      if (var2 != null && SandboxOptions.instance.BloodLevel.getValue() > 1) {
         int var5 = var2.getSplatNumber();
         if (var5 < 1) {
            var5 = 1;
         }

         if (Core.bLastStand) {
            var5 *= 3;
         }

         switch (SandboxOptions.instance.BloodLevel.getValue()) {
            case 2:
               var5 /= 2;
            case 3:
            default:
               break;
            case 4:
               var5 *= 2;
               break;
            case 5:
               var5 *= 5;
         }

         for(int var6 = 0; var6 < var5; ++var6) {
            var4.splatBlood(3, 0.3F);
         }
      }

      byte var12 = 3;
      byte var11 = 7;
      switch (SandboxOptions.instance.BloodLevel.getValue()) {
         case 1:
            var11 = 0;
            break;
         case 2:
            var11 = 4;
            var12 = 5;
         case 3:
         default:
            break;
         case 4:
            var11 = 10;
            var12 = 2;
            break;
         case 5:
            var11 = 15;
            var12 = 0;
      }

      if (SandboxOptions.instance.BloodLevel.getValue() > 1) {
         var4.splatBloodFloorBig();
      }

      float var7 = 0.5F;
      if (var4 instanceof IsoZombie && (((IsoZombie)var4).bCrawling || var4.getCurrentState() == ZombieOnGroundState.instance())) {
         var7 = 0.2F;
      }

      float var8 = Rand.Next(1.5F, 5.0F);
      float var9 = Rand.Next(1.5F, 5.0F);
      if (var3 instanceof IsoPlayer && ((IsoPlayer)var3).isDoShove()) {
         var8 = Rand.Next(0.0F, 0.5F);
         var9 = Rand.Next(0.0F, 0.5F);
      }

      if (var11 > 0) {
         var4.playBloodSplatterSound();
      }

      for(int var10 = 0; var10 < var11; ++var10) {
         if (Rand.Next(var12) == 0) {
            new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, var4.getCell(), var4.getX(), var4.getY(), var4.getZ() + var7, var4.getHitDir().x * var8, var4.getHitDir().y * var9);
         }
      }

   }

   private int DoSwingCollisionBoneCheck(IsoGameCharacter var1, HandWeapon var2, IsoGameCharacter var3, int var4, float var5) {
      movingStatic.clear();
      float var8 = var2.WeaponLength;
      var8 += 0.5F;
      if (var1.isAimAtFloor() && ((IsoLivingCharacter)var1).isDoShove()) {
         var8 = 0.3F;
      }

      Model.BoneToWorldCoords(var3, var4, tempVectorBonePos);

      for(int var9 = 1; var9 <= 10; ++var9) {
         float var10 = (float)var9 / 10.0F;
         tempVector3_1.x = var1.getX();
         tempVector3_1.y = var1.getY();
         tempVector3_1.z = var1.getZ();
         Vector3 var10000 = tempVector3_1;
         var10000.x += var1.getForwardDirection().x * var8 * var10;
         var10000 = tempVector3_1;
         var10000.y += var1.getForwardDirection().y * var8 * var10;
         tempVector3_1.x = tempVectorBonePos.x - tempVector3_1.x;
         tempVector3_1.y = tempVectorBonePos.y - tempVector3_1.y;
         tempVector3_1.z = 0.0F;
         boolean var11 = tempVector3_1.getLength() < var5;
         if (var11) {
            return var4;
         }
      }

      return -1;
   }

   public void processMaintanenceCheck(IsoGameCharacter var1, HandWeapon var2, IsoObject var3) {
      if (!GameClient.bClient) {
         if (var3 instanceof IsoTree) {
            LuaEventManager.triggerEvent("OnWeaponHitTree", var1, var2);
         }

         if (Rand.NextBool(2) && !var2.isRanged() && WeaponType.getWeaponType(var1) != WeaponType.barehand && (!var1.isAimAtFloor() || !((IsoLivingCharacter)var1).isDoShove())) {
            if (var2.isTwoHandWeapon() && (var1.getPrimaryHandItem() != var2 || var1.getSecondaryHandItem() != var2) && Rand.NextBool(3)) {
               return;
            }

            if (!var2.hasTag("NoMaintenanceXp")) {
               float var4 = 1.0F;
               if (var2.getConditionLowerChance() > 10) {
                  var4 = var4 * 10.0F / (float)var2.getConditionLowerChance();
               }

               if (GameServer.bServer) {
                  GameServer.addXp((IsoPlayer)var1, PerkFactory.Perks.Maintenance, (float)((int)var4));
               } else {
                  var1.getXp().AddXP(PerkFactory.Perks.Maintenance, var4);
               }
            }
         }
      }

   }

   public void attackCollisionCheck(IsoGameCharacter var1, HandWeapon var2, SwipeStatePlayer var3) {
      HashMap var4 = var1.getStateMachineParams(var3);
      IsoLivingCharacter var5 = (IsoLivingCharacter)var1;
      IsoPlayer var6 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      if (var1.isPerformingShoveAnimation()) {
         var5.setDoShove(true);
      }

      if (var1.isPerformingGrappleGrabAnimation()) {
         var5.setDoGrapple(true);
      }

      if (GameServer.bServer) {
         DebugLog.Network.debugln("Player swing connects.");
      }

      LuaEventManager.triggerEvent("OnWeaponSwingHitPoint", var1, var2);
      if (var2.getPhysicsObject() != null) {
         var1.Throw(var2);
      }

      if (var2.isUseSelf()) {
         var2.Use();
      }

      if (var2.isOtherHandUse() && var1.getSecondaryHandItem() != null) {
         var1.getSecondaryHandItem().Use();
      }

      boolean var7 = false;
      if (var5.isDoShove() && !var1.isAimAtFloor()) {
         var7 = true;
      }

      boolean var8 = false;
      boolean var9 = false;
      var1.getAttackVars().setWeapon(var2);
      var1.getAttackVars().targetOnGround.setMovingObject(var5.targetOnGround);
      var1.getAttackVars().bAimAtFloor = var1.isAimAtFloor();
      var1.getAttackVars().bDoShove = var5.isDoShove() || var1.isPerformingShoveAnimation();
      var1.getAttackVars().bDoGrapple = var5.isDoGrapple() || var5.isPerformingGrappleGrabAnimation();
      this.calculateHitInfoList(var1);
      this.treeHit = null;
      int var10 = var1.getHitInfoList().size();
      boolean var11 = this.CheckObjectHit(var1, var2);
      boolean var12 = false;
      boolean var13 = false;
      float var16;
      Stats var10000;
      if (var2.isUseEndurance()) {
         float var14 = var2.getWeight();
         if (var14 < 1.0F) {
            var14 = 1.0F;
         }

         float var15 = 0.0F;
         if (var2.isTwoHandWeapon() && (var1.getPrimaryHandItem() != var2 || var1.getSecondaryHandItem() != var2)) {
            var15 = var14 / 1.5F / 10.0F;
         }

         if (!var1.isForceShove()) {
            var16 = (var14 * 0.18F * var2.getFatigueMod(var1) * var1.getFatigueMod() * var2.getEnduranceMod() * 0.3F + var15) * 0.04F;
            float var17 = var1.Traits.getTraitEnduranceLossModifier();
            var10000 = var1.getStats();
            var10000.endurance -= var16 * var17;
         }
      }

      var1.addCombatMuscleStrain(var2, var10);
      var1.setLastHitCount(var10);
      if (var2.isAimedFirearm() || !var2.isMultipleHitConditionAffected()) {
         var8 = true;
      }

      int var53 = 1;
      this.dotList.clear();
      if (var10 == 0 && var1.getClickSound() != null && !var5.isDoShove()) {
         if (var6 == null || var6.isLocalPlayer()) {
            var1.getEmitter().playSound(var1.getClickSound());
         }

         var1.setRecoilDelay(10.0F);
      }

      boolean var54 = false;

      int var57;
      for(int var55 = 0; var55 < var10; ++var55) {
         var57 = 0;
         boolean var18 = false;
         HitInfo var19 = (HitInfo)var1.getHitInfoList().get(var55);
         IsoMovingObject var20 = var19.getObject();
         BaseVehicle var21 = (BaseVehicle)Type.tryCastTo(var20, BaseVehicle.class);
         IsoZombie var22 = (IsoZombie)Type.tryCastTo(var20, IsoZombie.class);
         if (var20 instanceof IsoGameCharacter) {
            var12 = true;
         }

         if (var20 != null) {
            var13 = true;
         }

         if (var19.getObject() == null && var19.window.getObject() != null) {
            var19.window.getObject().WeaponHit(var1, var2);
         } else {
            this.smashWindowBetween(var1, var20, var2);
            if (!this.isWindowBetween(var1, var20)) {
               int var23 = var19.chance;
               boolean var24 = Rand.Next(100) <= var23;
               if (var24) {
                  Vector2 var25 = tempVector2_1.set(var1.getX(), var1.getY());
                  Vector2 var26 = tempVector2_2.set(var20.getX(), var20.getY());
                  var26.x -= var25.x;
                  var26.y -= var25.y;
                  Vector2 var27 = var1.getLookVector(tempVector2_1);
                  var27.tangent();
                  var26.normalize();
                  boolean var28 = true;
                  float var29 = var27.dot(var26);

                  float var31;
                  for(int var30 = 0; var30 < this.dotList.size(); ++var30) {
                     var31 = (Float)this.dotList.get(var30);
                     if (Math.abs(var29 - var31) < 1.0E-4F) {
                        var28 = false;
                     }
                  }

                  float var59 = var2.getMinDamage();
                  var31 = var2.getMaxDamage();
                  long var32 = 0L;
                  if (!var28) {
                     var59 /= 5.0F;
                     var31 /= 5.0F;
                  }

                  if (var1.isAimAtFloor() && !var2.isRanged() && var1.isNPC()) {
                     this.splash(var20, var2, var1);
                     var57 = (byte)Rand.Next(2);
                  } else if (var1.isAimAtFloor() && !var2.isRanged()) {
                     if (var6 == null || var6.isLocalPlayer()) {
                        if (!StringUtils.isNullOrEmpty(var2.getHitFloorSound())) {
                           var1.getEmitter().stopSoundByName(var2.getSwingSound());
                           if (var6 != null) {
                              var6.setMeleeHitSurface(ParameterMeleeHitSurface.Material.Body);
                           }

                           var32 = var1.playSound(var2.getHitFloorSound());
                        } else {
                           var1.getEmitter().stopSoundByName(var2.getSwingSound());
                           if (var6 != null) {
                              var6.setMeleeHitSurface(ParameterMeleeHitSurface.Material.Body);
                           }

                           var32 = var1.playSound(var2.getZombieHitSound());
                        }
                     }

                     int var34 = this.DoSwingCollisionBoneCheck(var1, this.getWeapon(var1), (IsoGameCharacter)var20, ((IsoGameCharacter)var20).getAnimationPlayer().getSkinningBoneIndex("Bip01_Head", -1), 0.28F);
                     if (var34 == -1) {
                        var34 = this.DoSwingCollisionBoneCheck(var1, this.getWeapon(var1), (IsoGameCharacter)var20, ((IsoGameCharacter)var20).getAnimationPlayer().getSkinningBoneIndex("Bip01_Spine", -1), 0.28F);
                        if (var34 == -1) {
                           var34 = this.DoSwingCollisionBoneCheck(var1, this.getWeapon(var1), (IsoGameCharacter)var20, ((IsoGameCharacter)var20).getAnimationPlayer().getSkinningBoneIndex("Bip01_L_Calf", -1), 0.13F);
                           if (var34 == -1) {
                              var34 = this.DoSwingCollisionBoneCheck(var1, this.getWeapon(var1), (IsoGameCharacter)var20, ((IsoGameCharacter)var20).getAnimationPlayer().getSkinningBoneIndex("Bip01_R_Calf", -1), 0.13F);
                           }

                           if (var34 == -1) {
                              var34 = this.DoSwingCollisionBoneCheck(var1, this.getWeapon(var1), (IsoGameCharacter)var20, ((IsoGameCharacter)var20).getAnimationPlayer().getSkinningBoneIndex("Bip01_L_Foot", -1), 0.23F);
                           }

                           if (var34 == -1) {
                              var34 = this.DoSwingCollisionBoneCheck(var1, this.getWeapon(var1), (IsoGameCharacter)var20, ((IsoGameCharacter)var20).getAnimationPlayer().getSkinningBoneIndex("Bip01_R_Foot", -1), 0.23F);
                           }

                           if (var34 == -1) {
                              continue;
                           }

                           var18 = true;
                        }
                     } else {
                        this.splash(var20, var2, var1);
                        this.splash(var20, var2, var1);
                        var57 = (byte)(Rand.Next(0, 3) + 1);
                     }
                  }

                  if (!var1.getAttackVars().bAimAtFloor && (!var1.getAttackVars().bCloseKill || !var1.isCriticalHit()) && !var5.isDoShove() && var20 instanceof IsoGameCharacter && (var6 == null || var6.isLocalPlayer())) {
                     if (var6 != null) {
                        var6.setMeleeHitSurface(ParameterMeleeHitSurface.Material.Body);
                     }

                     if (var2.isRanged()) {
                        var32 = ((IsoGameCharacter)var20).playSound(var2.getZombieHitSound());
                     } else {
                        var1.getEmitter().stopSoundByName(var2.getSwingSound());
                        var32 = var1.playSound(var2.getZombieHitSound());
                     }
                  }

                  float var37;
                  if (var2.isRanged() && var22 != null) {
                     Vector2 var62 = tempVector2_1.set(var1.getX(), var1.getY());
                     Vector2 var35 = tempVector2_2.set(var20.getX(), var20.getY());
                     var35.x -= var62.x;
                     var35.y -= var62.y;
                     Vector2 var36 = var22.getForwardDirection();
                     var35.normalize();
                     var36.normalize();
                     var37 = var35.dot(var36);
                     var22.setHitFromBehind(var37 > 0.5F);
                  }

                  if (this.dotList.isEmpty()) {
                     this.dotList.add(var29);
                  }

                  if (var22 != null && var22.isCurrentState(ZombieOnGroundState.instance())) {
                     var22.setReanimateTimer(var22.getReanimateTimer() + (float)Rand.Next(10));
                  }

                  if (var22 != null && var22.isCurrentState(ZombieGetUpState.instance())) {
                     var22.setReanimateTimer((float)(Rand.Next(60) + 30));
                  }

                  boolean var63 = false;
                  if (!var2.isTwoHandWeapon() || var1.isItemInBothHands(var2)) {
                     var63 = true;
                  }

                  float var60 = var59;
                  float var61 = var31 - var59;
                  if (var61 != 0.0F) {
                     var60 = var59 + (float)Rand.Next((int)(var61 * 1000.0F)) / 1000.0F;
                  }

                  if (!var2.isRanged()) {
                     var60 *= var2.getDamageMod(var1) * var1.getHittingMod();
                  }

                  if (!var63 && !var2.isRanged() && var31 > var59) {
                     var60 -= var59;
                  }

                  if (!var2.isRanged()) {
                     int var38;
                     if (var1.isAimAtFloor() && var5.isDoShove()) {
                        var37 = 0.0F;

                        for(var38 = BodyPartType.ToIndex(BodyPartType.UpperLeg_L); var38 <= BodyPartType.ToIndex(BodyPartType.Foot_R); ++var38) {
                           var37 += ((BodyPart)var1.getBodyDamage().getBodyParts().get(var38)).getPain();
                        }

                        if (var37 > 10.0F) {
                           var60 /= PZMath.clamp(var37 / 10.0F, 1.0F, 30.0F);
                           MoodlesUI.getInstance().wiggle(MoodleType.Pain);
                           MoodlesUI.getInstance().wiggle(MoodleType.Injured);
                        }
                     } else {
                        var37 = 0.0F;

                        for(var38 = BodyPartType.ToIndex(BodyPartType.Hand_L); var38 <= BodyPartType.ToIndex(BodyPartType.UpperArm_R); ++var38) {
                           var37 += ((BodyPart)var1.getBodyDamage().getBodyParts().get(var38)).getPain();
                        }

                        if (var37 > 10.0F) {
                           var60 /= PZMath.clamp(var37 / 10.0F, 1.0F, 30.0F);
                           MoodlesUI.getInstance().wiggle(MoodleType.Pain);
                           MoodlesUI.getInstance().wiggle(MoodleType.Injured);
                        }
                     }
                  }

                  if (!var2.isRanged()) {
                     var60 *= var1.Traits.getTraitDamageDealtReductionModifier();
                  }

                  var37 = var60 / ((float)var53 * 0.5F);
                  if (var1.isAttackWasSuperAttack()) {
                     var37 *= 5.0F;
                  }

                  ++var53;
                  if (var11) {
                     var13 = true;
                  }

                  if (var13) {
                     var8 = true;
                  }

                  Vector2 var64 = tempVector2_1.set(var1.getX(), var1.getY());
                  Vector2 var39 = tempVector2_2.set(var20.getX(), var20.getY());
                  var39.x -= var64.x;
                  var39.y -= var64.y;
                  float var40 = var39.getLength();
                  float var41 = 1.0F;
                  if (var2.isRangeFalloff()) {
                     var41 = 1.0F;
                  } else if (var2.isRanged()) {
                     var41 = 0.5F;
                  } else {
                     var41 = var40 / var2.getMaxRange(var1);
                  }

                  var41 *= 2.0F;
                  if (var41 < 0.3F) {
                     var41 = 1.0F;
                  }

                  if (!var2.isRanged() && var1.getMoodles().getMoodleLevel(MoodleType.Panic) > 1) {
                     var37 -= (float)var1.getMoodles().getMoodleLevel(MoodleType.Panic) * 0.1F;
                     MoodlesUI.getInstance().wiggle(MoodleType.Panic);
                  }

                  if (!var2.isRanged() && var1.getMoodles().getMoodleLevel(MoodleType.Stress) > 1) {
                     var37 -= (float)var1.getMoodles().getMoodleLevel(MoodleType.Stress) * 0.1F;
                     MoodlesUI.getInstance().wiggle(MoodleType.Stress);
                  }

                  if (var37 < 0.0F) {
                     var37 = 0.1F;
                  }

                  if (var1.isAimAtFloor() && var5.isDoShove()) {
                     var37 = Rand.Next(0.7F, 1.0F) + (float)var1.getPerkLevel(PerkFactory.Perks.Strength) * 0.2F;
                     Clothing var42 = (Clothing)var1.getWornItem("Shoes");
                     if (var42 == null) {
                        var37 *= 0.5F;
                     } else {
                        var37 *= var42.getStompPower();
                     }
                  }

                  if (!var2.isRanged()) {
                     switch (var1.getMoodles().getMoodleLevel(MoodleType.Endurance)) {
                        case 0:
                        default:
                           break;
                        case 1:
                           var37 *= 0.5F;
                           MoodlesUI.getInstance().wiggle(MoodleType.Endurance);
                           break;
                        case 2:
                           var37 *= 0.2F;
                           MoodlesUI.getInstance().wiggle(MoodleType.Endurance);
                           break;
                        case 3:
                           var37 *= 0.1F;
                           MoodlesUI.getInstance().wiggle(MoodleType.Endurance);
                           break;
                        case 4:
                           var37 *= 0.05F;
                           MoodlesUI.getInstance().wiggle(MoodleType.Endurance);
                     }

                     switch (var1.getMoodles().getMoodleLevel(MoodleType.Tired)) {
                        case 0:
                        default:
                           break;
                        case 1:
                           var37 *= 0.5F;
                           MoodlesUI.getInstance().wiggle(MoodleType.Tired);
                           break;
                        case 2:
                           var37 *= 0.2F;
                           MoodlesUI.getInstance().wiggle(MoodleType.Tired);
                           break;
                        case 3:
                           var37 *= 0.1F;
                           MoodlesUI.getInstance().wiggle(MoodleType.Tired);
                           break;
                        case 4:
                           var37 *= 0.05F;
                           MoodlesUI.getInstance().wiggle(MoodleType.Tired);
                     }
                  }

                  var1.knockbackAttackMod = 1.0F;
                  if ("KnifeDeath".equals(var1.getVariableString("ZombieHitReaction"))) {
                     var41 *= 1000.0F;
                     var1.knockbackAttackMod = 0.0F;
                     var1.addWorldSoundUnlessInvisible(4, 4, false);
                     var1.getAttackVars().bCloseKill = true;
                     var20.setCloseKilled(true);
                  } else {
                     var1.getAttackVars().bCloseKill = false;
                     var20.setCloseKilled(false);
                     var1.addWorldSoundUnlessInvisible(8, 8, false);
                     if (Rand.Next(3) == 0 || var1.isAimAtFloor() && var5.isDoShove()) {
                        var1.addWorldSoundUnlessInvisible(10, 10, false);
                     } else if (Rand.Next(7) == 0) {
                        var1.addWorldSoundUnlessInvisible(16, 16, false);
                     }
                  }

                  var20.setHitFromAngle(var19.dot);
                  if (var22 != null) {
                     var22.setHitFromBehind(var1.isBehind(var22));
                     var22.setHitAngle(var1.getForwardDirection());
                     var22.setPlayerAttackPosition(var22.testDotSide(var1));
                     var22.setHitHeadWhileOnFloor(var57);
                     var22.setHitLegsWhileOnFloor(var18);
                     if (var57 > 0) {
                        var22.addBlood(BloodBodyPartType.Head, true, true, true);
                        var22.addBlood(BloodBodyPartType.Torso_Upper, true, false, false);
                        var22.addBlood(BloodBodyPartType.UpperArm_L, true, false, false);
                        var22.addBlood(BloodBodyPartType.UpperArm_R, true, false, false);
                        var37 *= 3.0F;
                     }

                     if (var18) {
                        var37 = 0.05F;
                     }

                     boolean var65 = false;
                     int var66;
                     if (var57 > 0) {
                        var66 = Rand.Next(BodyPartType.ToIndex(BodyPartType.Head), BodyPartType.ToIndex(BodyPartType.Neck) + 1);
                     } else if (var18) {
                        var66 = Rand.Next(BodyPartType.ToIndex(BodyPartType.Groin), BodyPartType.ToIndex(BodyPartType.Foot_R) + 1);
                     } else {
                        var66 = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.Neck) + 1);
                     }

                     float var43 = var22.getBodyPartClothingDefense(var66, false, var2.isRanged()) * 0.5F;
                     var43 += var22.getBodyPartClothingDefense(var66, true, var2.isRanged());
                     var43 *= (float)SandboxOptions.instance.Lore.ZombiesArmorFactor.getValue();
                     int var44 = SandboxOptions.instance.Lore.ZombiesMaxDefense.getValue();
                     if (var44 > 100) {
                        var44 = 100;
                     }

                     if (var43 > (float)var44) {
                        var43 = (float)var44;
                     }

                     float var45 = var37 * Math.abs(1.0F - var43 / 100.0F);
                     BodyPartType var46 = BodyPartType.FromIndex(var66);
                     boolean var47 = ((IsoLivingCharacter)var1).isDoShove();
                     if (var47 || WeaponType.getWeaponType(var2) == WeaponType.knife) {
                        boolean var48 = false;
                        boolean var49 = false;
                        if (!var1.isAimAtFloor()) {
                           var49 = var1.isBehind(var22);
                        } else {
                           var49 = var22.isFallOnFront();
                        }

                        if (var49) {
                           var48 = var22.bodyPartIsSpikedBehind(var66);
                        } else {
                           var48 = var22.bodyPartIsSpiked(var66);
                        }

                        boolean var50 = var48 && var1.isAimAtFloor() && var47;
                        boolean var51 = var48 && !var50 && (var1.getPrimaryHandItem() == null || var1.getPrimaryHandItem() instanceof HandWeapon);
                        boolean var52 = var48 && !var50 && (var1.getSecondaryHandItem() == null || var1.getSecondaryHandItem() instanceof HandWeapon) && var47;
                        if (var50) {
                           var22.addBlood(BloodBodyPartType.FromIndex(var66), true, false, false);
                           var1.spikePart(BodyPartType.Foot_R);
                        }

                        if (var51) {
                           var22.addBlood(BloodBodyPartType.FromIndex(var66), true, false, false);
                           var1.spikePart(BodyPartType.Hand_R);
                        }

                        if (var52) {
                           var22.addBlood(BloodBodyPartType.FromIndex(var66), true, false, false);
                           var1.spikePart(BodyPartType.Hand_L);
                        }
                     }

                     if (Core.bDebug) {
                        DebugLogStream var73 = DebugLog.Combat;
                        String var10001 = BodyPartType.getDisplayName(var46);
                        var73.debugln("Zombie got hit in " + var10001 + " with a " + var2.getFullType() + " for " + var45 + " out of " + var37 + " after totalDef of " + var43 + "% was applied");
                     }

                     var37 = var45;
                     if (!GameClient.bClient && !GameServer.bServer || GameClient.bClient && IsoPlayer.isLocalPlayer(var1)) {
                        var9 = var22.helmetFall(var57 > 0);
                     }

                     if ("KnifeDeath".equals(var1.getVariableString("ZombieHitReaction")) && !"Tutorial".equals(Core.GameMode)) {
                        byte var70 = 8;
                        if (var22.isCurrentState(AttackState.instance())) {
                           var70 = 3;
                        }

                        int var71 = var1.getPerkLevel(PerkFactory.Perks.SmallBlade) + 1;
                        if (Rand.NextBool(var70 + var71 * 2)) {
                           InventoryItem var72 = var1.getPrimaryHandItem();
                           var1.getInventory().Remove(var72);
                           var1.removeFromHands(var72);
                           var22.setAttachedItem("JawStab", var72);
                           var22.setJawStabAttach(true);
                        }

                        var22.setKnifeDeath(true);
                     }
                  }

                  float var67 = 0.0F;
                  boolean var68 = var1.isCriticalHit();
                  if (var21 == null && var20.getSquare() != null && var1.getSquare() != null) {
                     var20.setCloseKilled(var1.getAttackVars().bCloseKill);
                     if (var6.isLocalPlayer() || var1.isNPC()) {
                        var67 = var20.Hit(var2, var1, var37, var7, var41);
                        this.setParameterCharacterHitResult(var1, var22, var32);
                     }

                     if (!GameClient.bClient && !GameServer.bServer) {
                        LuaEventManager.triggerEvent("OnWeaponHitXp", var1, var2, var20, var37, 1);
                     }

                     if ((!var5.isDoShove() || var1.isAimAtFloor()) && var1.DistToSquared(var20) < 2.0F && Math.abs(var1.getZ() - var20.getZ()) < 0.5F) {
                        var1.addBlood((BloodBodyPartType)null, false, false, false);
                     }

                     if (var20 instanceof IsoGameCharacter) {
                        if (((IsoGameCharacter)var20).isDead()) {
                           var10000 = var1.getStats();
                           var10000.stress -= 0.02F;
                        } else if (!(var20 instanceof IsoPlayer) && (!var5.isDoShove() || var1.isAimAtFloor())) {
                           this.splash(var20, var2, var1);
                        }
                     }
                  } else if (var21 != null) {
                     boolean var69 = var21.processHit(var1, var2, var37);
                     if (!var69) {
                        ((HitInfo)var1.getHitInfoList().get(var55)).object.setMovingObject((IsoMovingObject)null);
                     } else {
                        var67 = var37;
                     }
                  }

                  if (GameClient.bClient && var1.isLocal()) {
                     if (var20 instanceof IsoGameCharacter) {
                        HitReactionNetworkAI.CalcHitReactionWeapon(var1, (IsoGameCharacter)var20, var2);
                     }

                     var54 = GameClient.sendPlayerHit(var1, var20, var2, var67, var7, var41, var68, var9, var57 > 0);
                  }
               }
            }
         }
      }

      if (!var8 && this.bHitOnlyTree) {
         boolean var56 = var2.getScriptItem().Categories.contains("Axe");
         var57 = var56 ? 2 : 1;
         if (Rand.Next(var2.getConditionLowerChance() * var57 + var1.getMaintenanceMod()) == 0) {
            var8 = true;
         } else if (!GameClient.bClient) {
            this.processMaintanenceCheck(var1, var2, this.treeHit);
         } else if (var1.isLocal() && !var54) {
            var54 = GameClient.sendPlayerHit(var1, this.treeHit, var2, 0.0F, false, 1.0F, false, false, false);
         }
      }

      if (this.treeHit != null) {
         this.treeHit.WeaponHit(var1, var2);
      }

      if (var2 == ((IsoLivingCharacter)var1).bareHands) {
         var8 = false;
      }

      if (var8) {
         var16 = 1.0F;
         if (var6 != null && "charge".equals(var6.getAttackType())) {
            var16 /= 1.5F;
         }

         if (!var2.damageCheck(0, var16)) {
            if (!GameClient.bClient) {
               this.processMaintanenceCheck(var1, var2, this.objHit);
            } else if (var1.isLocal() && !var54) {
               var54 = GameClient.sendPlayerHit(var1, this.objHit, var2, 0.0F, false, 1.0F, false, false, false);
            }
         }
      }

      if (GameClient.bClient && var1.isLocal() && !var54) {
         GameClient.sendPlayerHit(var1, (IsoObject)null, var2, 0.0F, var7, 1.0F, var1.isCriticalHit(), false, false);
      }

      var4.put(PARAM_LOWER_CONDITION, var8 && var2.getCondition() > 0 && !var1.isRangedWeaponEmpty() ? Boolean.TRUE : Boolean.FALSE);
      var4.put(PARAM_ATTACKED, Boolean.TRUE);
      if (var2.isAimedFirearm()) {
         EffectsManager.getInstance().startMuzzleFlash(var1);
         BallisticsController var58 = var1.getBallisticsController();
         var58.update();
         this.fireWeapon(var2, var1);
      }

   }

   public void releaseBallisticsTargets(IsoGameCharacter var1) {
      ArrayList var2 = var1.getHitInfoList();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         HitInfo var4 = (HitInfo)var2.get(var3);
         IsoMovingObject var6 = var4.getObject();
         if (var6 instanceof IsoZombie var5) {
            if (var5.useBallistics()) {
               BallisticsTarget var7 = var5.getBallisticsTarget();
               if (var7 != null) {
                  var5.releaseBallisticsTarget();
               }
            }
         }
      }

   }

   public void applyDamage(IsoGameCharacter var1, float var2) {
      if (!var1.isInvulnerable()) {
         var1.applyDamage(var2);
      }
   }

   public void applyDamage(BodyPart var1, float var2) {
      IsoGameCharacter var3 = var1.getParentChar();
      if (var3 != null && !var3.isInvulnerable()) {
         var1.ReduceHealth(var2);
      }
   }

   public void calculateAttackVars(IsoLivingCharacter var1) {
      this.calculateAttackVars(var1, var1.getAttackVars());
   }

   private void calculateAttackVars(IsoLivingCharacter var1, AttackVars var2) {
      if (!var2.isProcessed) {
         HandWeapon var3 = (HandWeapon)Type.tryCastTo(var1.getPrimaryHandItem(), HandWeapon.class);
         if (var3 != null && var3.getOtherHandRequire() != null) {
            InventoryItem var4 = var1.getSecondaryHandItem();
            if (var4 != null) {
               if (!var4.getType().equals(var3.getOtherHandRequire()) && !var4.hasTag(var3.getOtherHandRequire()) || var4.getCurrentUses() == 0) {
                  var3 = null;
               }
            } else {
               var3 = null;
            }
         }

         if (!GameClient.bClient || var1.isLocal()) {
            boolean var11 = var1.isPerformingHostileAnimation();
            var2.setWeapon(var3 == null ? var1.bareHands : var3);
            var2.targetOnGround.setMovingObject((IsoMovingObject)null);
            var2.bAimAtFloor = false;
            var2.bCloseKill = false;
            var2.bDoShove = var1.isDoShove();
            var2.bDoGrapple = var1.isDoGrapple();
            if (!var11) {
               var1.setVariable("ShoveAimX", 0.5F);
               var1.setVariable("ShoveAimY", 1.0F);
               if (var2.bDoShove && var1.getVariableBoolean("isMoving")) {
                  var1.setVariable("ShoveAim", true);
               } else {
                  var1.setVariable("ShoveAim", false);
               }
            }

            var2.useChargeDelta = var1.useChargeDelta;
            var2.recoilDelay = 0;
            if (var2.bDoGrapple) {
               var2.bAimAtFloor = false;
               var2.setWeapon(var1.bareHands);
            } else if ((var2.getWeapon(var1) == var1.bareHands || var2.bDoShove || var1.isForceShove()) && ((IsoPlayer)var1).getAttackType() != "charge") {
               var2.bDoShove = true;
               var2.bAimAtFloor = false;
               var2.setWeapon(var1.bareHands);
            }

            this.calcValidTargets(var1, var2.getWeapon(var1), true, var2.targetsProne, var2.targetsStanding);
            HitInfo var5 = var2.targetsStanding.isEmpty() ? null : (HitInfo)var2.targetsStanding.get(0);
            HitInfo var6 = var2.targetsProne.isEmpty() ? null : (HitInfo)var2.targetsProne.get(0);
            if (this.isProneTargetBetter(var1, var5, var6)) {
               var5 = null;
            }

            if (!var11) {
               var1.setAimAtFloor(false);
            }

            float var7 = 3.4028235E38F;
            if (var5 != null) {
               if (!var11) {
                  var1.setAimAtFloor(false);
               }

               var2.bAimAtFloor = false;
               var2.targetOnGround.setMovingObject((IsoMovingObject)null);
               var7 = var5.distSq;
            } else if (var6 != null && (Core.getInstance().isOptionAutoProneAtk() || var1.isDoShove())) {
               if (!var11) {
                  var1.setAimAtFloor(true);
               }

               var2.bAimAtFloor = true;
               var2.targetOnGround.setMovingObject(var6.getObject());
            }

            if (!(var7 >= var2.getWeapon(var1).getMinRange() * var2.getWeapon(var1).getMinRange()) && (var5 == null || !this.isWindowBetween(var1, var5.getObject()))) {
               if (var1.getStats().NumChasingZombies <= 1 && WeaponType.getWeaponType((IsoGameCharacter)var1) == WeaponType.knife) {
                  var2.bCloseKill = true;
                  return;
               }

               var2.bDoShove = true;
               IsoPlayer var8 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
               if (var8 != null && (!var8.isAuthorizedHandToHand() || "charge".equals(var8.getAttackType()))) {
                  var2.bDoShove = false;
               }

               var2.bAimAtFloor = false;
               if (var1.bareHands.getSwingAnim() != null) {
                  var2.useChargeDelta = 3.0F;
               }
            }

            int var12 = GameKeyboard.whichKeyDown("ManualFloorAtk");
            int var9 = GameKeyboard.whichKeyDown("Sprint");
            boolean var10 = var1.getVariableBoolean("StartedAttackWhileSprinting");
            if (GameKeyboard.isKeyDown("ManualFloorAtk") && (var12 != var9 || !var10)) {
               var2.bAimAtFloor = true;
               var2.bDoShove = GameKeyboard.isKeyDown("Melee") || var2.getWeapon(var1) == var1.bareHands;
               var1.setDoShove(var2.bDoShove);
            }

            if (var2.getWeapon(var1).isRanged() && !"Auto".equalsIgnoreCase(var1.getFireMode())) {
               var2.recoilDelay = var2.getWeapon(var1).getRecoilDelay(var1);
            }

         }
      }
   }

   private void calculateValidBallisticsTargets(IsoLivingCharacter var1, HandWeapon var2, boolean var3, ArrayList<HitInfo> var4, ArrayList<HitInfo> var5) {
   }

   public void calcValidTargets(IsoLivingCharacter var1, HandWeapon var2, boolean var3, ArrayList<HitInfo> var4, ArrayList<HitInfo> var5) {
      boolean var6 = false;
      BallisticsController var7 = var1.getBallisticsController();
      if (var7 != null) {
         var6 = var2.isAimedFirearm();
      }

      Object var8 = this.meleeTargetComparator;
      this.hitInfoPool.release((List)var4);
      this.hitInfoPool.release((List)var5);
      var4.clear();
      var5.clear();
      float var9 = Core.getInstance().getIgnoreProneZombieRange();
      float var10 = var2.getMaxRange() * var2.getRangeMod(var1);
      float var11 = Math.max(var9, var10 + (var3 ? 1.0F : 0.0F));
      float var12 = var2.getMinAngle();
      float var13 = var2.getMaxAngle();
      ArrayList var14 = IsoWorld.instance.CurrentCell.getObjectList();

      for(int var15 = 0; var15 < var14.size(); ++var15) {
         IsoMovingObject var16 = (IsoMovingObject)var14.get(var15);
         HitInfo var17 = this.calcValidTarget(var1, var2, var16, var11);
         if (var17 != null) {
            if (isStanding(var16)) {
               var5.add(var17);
            } else {
               var4.add(var17);
            }
         }
      }

      if (!var6 && !var4.isEmpty() && this.shouldIgnoreProneZombies(var1, var5, var9)) {
         this.hitInfoPool.release((List)var4);
         var4.clear();
      }

      if (var2.isRanged()) {
         var8 = this.rangeTargetComparator;
      }

      if (var6) {
         this.removeUnhittableBallisticsTargets(var1, var2, var11, BallisticsControllerDistanceThreshold, var3, var5);
      } else {
         this.removeUnhittableTargets(var1, var2, var12, var13, var3, var5);
      }

      if (var2.isRanged() && var7 != null) {
         this.removeUnhittableBallisticsTargets(var1, var2, var11, BallisticsControllerDistanceThreshold, var3, var4);
      } else {
         var12 /= 1.5F;
         this.removeUnhittableTargets(var1, var2, var12, var13, var3, var4);
      }

      ((TargetComparator)var8).setBallisticsController(var7);
      var5.sort((Comparator)var8);
      var4.sort((Comparator)var8);
   }

   private boolean shouldIgnoreProneZombies(IsoGameCharacter var1, ArrayList<HitInfo> var2, float var3) {
      if (var3 <= 0.0F) {
         return false;
      } else {
         boolean var4 = var1.isInvisible() || var1 instanceof IsoPlayer && ((IsoPlayer)var1).isGhostMode();

         for(int var5 = 0; var5 < var2.size(); ++var5) {
            HitInfo var6 = (HitInfo)var2.get(var5);
            IsoZombie var7 = (IsoZombie)Type.tryCastTo(var6.getObject(), IsoZombie.class);
            if ((var7 == null || var7.target != null || var4) && !(var6.distSq > var3 * var3)) {
               boolean var8 = PolygonalMap2.instance.lineClearCollide(var1.getX(), var1.getY(), var6.getObject().getX(), var6.getObject().getY(), PZMath.fastfloor(var1.getZ()), var1, false, true);
               if (!var8) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private boolean isPointWithinDistance(Vector3f var1, Vector3f var2, Vector3 var3, float var4) {
      float var5 = var2.x - var1.x;
      float var6 = var2.y - var1.y;
      float var7 = var2.z - var1.z;
      float var8 = var3.x - var1.x;
      float var9 = var3.y - var1.y;
      float var10 = var3.z - var1.z;
      float var11 = var9 * var7 - var10 * var6;
      float var12 = var10 * var5 - var8 * var7;
      float var13 = var8 * var6 - var9 * var5;
      float var14 = var11 * var11 + var12 * var12 + var13 * var13;
      float var15 = var5 * var5 + var6 * var6 + var7 * var7;
      float var16 = var5 * var8 + var6 * var9 + var7 * var10;
      return var14 <= var4 * var4 * var15 && var16 > 0.0F;
   }

   private void removeUnhittableBallisticsTargets(IsoGameCharacter var1, HandWeapon var2, float var3, float var4, boolean var5, ArrayList<HitInfo> var6) {
      this.calculateBallistics(var1, var3, var5);

      for(int var7 = var6.size() - 1; var7 >= 0; --var7) {
         HitInfo var8 = (HitInfo)var6.get(var7);
         tempVector3_1.set(var8.x, var8.y, var8.z);
         if (!this.isPointWithinDistance(ballisticsStartPosition, ballisticsEndPosition, tempVector3_1, var4)) {
            if (Core.bDebug && DebugOptions.instance.Character.Debug.Render.AimCone.getValue()) {
               LineDrawer.DrawIsoCircle(var8.x, var8.y, var8.z, 0.1F, OccludedTargetDebugColor.r, OccludedTargetDebugColor.g, OccludedTargetDebugColor.b, 1.0F);
            }

            this.hitInfoPool.release((Object)var8);
            var6.remove(var7);
         } else if (Core.bDebug && DebugOptions.instance.Character.Debug.Render.AimCone.getValue()) {
            LineDrawer.DrawIsoCircle(var8.x, var8.y, var8.z, 0.1F, TargetableDebugColor.r, TargetableDebugColor.g, TargetableDebugColor.b, 1.0F);
         }
      }

   }

   private boolean isUnhittableTarget(IsoGameCharacter var1, HandWeapon var2, float var3, float var4, HitInfo var5, boolean var6) {
      if (!(var5.dot < var3) && !(var5.dot > var4)) {
         Vector3 var7 = tempVectorBonePos.set(var5.x, var5.y, var5.z);
         return !var1.IsAttackRange(var2, var5.getObject(), var7, var6);
      } else {
         return true;
      }
   }

   private void calculateBallistics(IsoGameCharacter var1, float var2, boolean var3) {
      float var4 = var1.getLookAngleRadians();
      BallisticsController var5 = var1.getBallisticsController();
      ballisticsDirectionVector.x = (float)Math.cos((double)var4);
      ballisticsDirectionVector.y = (float)Math.sin((double)var4);
      ballisticsDirectionVector.z = 0.0F;
      ballisticsDirectionVector.normalize(1.0F);
      if (var3) {
         ++var2;
      }

      Vector3 var6 = var5.getMuzzlePosition();
      ballisticsStartPosition.set(var6.x, var6.y, var6.z);
      ballisticsEndPosition.set(ballisticsStartPosition.x + ballisticsDirectionVector.x * var2, ballisticsStartPosition.y + ballisticsDirectionVector.y * var2, ballisticsStartPosition.z + ballisticsDirectionVector.z * var2);
   }

   private boolean isHittableBallisticsTarget(IsoGameCharacter var1, float var2, float var3, boolean var4, Vector3 var5) {
      this.calculateBallistics(var1, var2, var4);
      return this.isPointWithinDistance(ballisticsStartPosition, ballisticsEndPosition, var5, var3);
   }

   private void removeUnhittableTargets(IsoGameCharacter var1, HandWeapon var2, float var3, float var4, boolean var5, ArrayList<HitInfo> var6) {
      for(int var7 = var6.size() - 1; var7 >= 0; --var7) {
         HitInfo var8 = (HitInfo)var6.get(var7);
         if (this.isUnhittableTarget(var1, var2, var3, var4, var8, var5)) {
            this.hitInfoPool.release((Object)var8);
            var6.remove(var7);
         }
      }

   }

   private boolean getNearestTargetPosAndDot(IsoGameCharacter var1, HandWeapon var2, IsoMovingObject var3, boolean var4, Vector4f var5) {
      this.getNearestTargetPosAndDot(var1, var3, var5);
      float var6 = var5.w;
      float var7 = var2.getMinAngle();
      float var8 = var2.getMaxAngle();
      IsoGameCharacter var9 = (IsoGameCharacter)Type.tryCastTo(var3, IsoGameCharacter.class);
      if (var9 != null && !isStanding(var3)) {
         var7 /= 1.5F;
      }

      if (!(var6 < var7) && !(var6 > var8)) {
         Vector3 var10 = tempVectorBonePos.set(var5.x, var5.y, var5.z);
         return var1.IsAttackRange(var2, var3, var10, var4);
      } else {
         return false;
      }
   }

   private void getNearestTargetPosAndDot(IsoGameCharacter var1, IsoMovingObject var2, Vector3 var3, Vector2 var4, Vector4f var5) {
      float var6 = var1.getDotWithForwardDirection(var3);
      var6 = PZMath.clamp(var6, -1.0F, 1.0F);
      var5.w = Math.max(var6, var5.w);
      float var7 = IsoUtils.DistanceToSquared(var1.getX(), var1.getY(), (float)(PZMath.fastfloor(var1.getZ()) * 3), var3.x, var3.y, (float)(PZMath.fastfloor(var2.getZ()) * 3));
      if (var7 < var4.x) {
         var4.x = var7;
         var5.set(var3.x, var3.y, var3.z, var5.w);
      }

   }

   private void getNearestTargetPosAndDot(IsoGameCharacter var1, IsoMovingObject var2, String var3, Vector2 var4, Vector4f var5) {
      Vector3 var6 = getBoneWorldPos(var2, var3, tempVectorBonePos);
      this.getNearestTargetPosAndDot(var1, var2, var6, var4, var5);
   }

   private void getNearestTargetPosAndDot(IsoGameCharacter var1, IsoMovingObject var2, Vector4f var3) {
      Vector2 var4 = tempVector2_1.set(3.4028235E38F, 0.0F / 0.0F);
      var3.w = -1.0F / 0.0F;
      IsoGameCharacter var5 = (IsoGameCharacter)Type.tryCastTo(var2, IsoGameCharacter.class);
      if (var5 == null) {
         this.getNearestTargetPosAndDot(var1, var2, (String)null, var4, var3);
      } else {
         Vector3 var6 = tempVector3_1;
         int var7 = getBoneIndex(var2, "Bip01_Head");
         if (var7 != -1 && getBoneIndex(var2, "Bip01_HeadNub") != -1) {
            getBoneWorldPos(var2, "Bip01_Head", tempVector3_1);
            getBoneWorldPos(var2, "Bip01_HeadNub", tempVector3_2);
            tempVector3_1.addToThis(tempVector3_2);
            tempVector3_1.div(2.0F);
         } else if (var7 != -1) {
            getPointAlongBoneXAxis(var2, "Bip01_Head", 0.075F, var6);
            Model.VectorToWorldCoords((IsoGameCharacter)var2, var6);
         }

         Vector3 var8;
         if (isStanding(var2)) {
            this.getNearestTargetPosAndDot(var1, var2, var6, var4, var3);
            this.getNearestTargetPosAndDot(var1, var2, "Bip01_Pelvis", var4, var3);
            var8 = tempVectorBonePos.set(var2.getX(), var2.getY(), var2.getZ());
            this.getNearestTargetPosAndDot(var1, var2, var8, var4, var3);
         } else {
            this.getNearestTargetPosAndDot(var1, var2, var6, var4, var3);
            this.getNearestTargetPosAndDot(var1, var2, "Bip01_Pelvis", var4, var3);
            var7 = getBoneIndex(var2, "Bip01_DressFrontNub");
            if (var7 == -1) {
               var7 = getBoneIndex(var2, "Bip01_DressFront02");
               if (var7 != -1) {
                  var8 = tempVector3_2;
                  getPointAlongBoneXAxis(var2, "Bip01_DressFront02", 0.2F, var8);
                  Model.VectorToWorldCoords((IsoGameCharacter)var2, var8);
                  this.getNearestTargetPosAndDot(var1, var2, var8, var4, var3);
               }
            } else {
               this.getNearestTargetPosAndDot(var1, var2, "Bip01_DressFrontNub", var4, var3);
            }
         }

      }
   }

   private HitInfo calcValidTarget(IsoLivingCharacter var1, HandWeapon var2, IsoMovingObject var3, float var4) {
      if (var3 == var1) {
         return null;
      } else {
         IsoGameCharacter var5 = (IsoGameCharacter)Type.tryCastTo(var3, IsoGameCharacter.class);
         if (var5 == null) {
            return null;
         } else if (var5.isGodMod()) {
            return null;
         } else if (!var5.isAnimal() && !checkPVP(var1, var3)) {
            return null;
         } else if (var2 != null && !var2.isRanged() && var1.DistToSquared(var5) > 9.0F) {
            return null;
         } else {
            float var6 = Math.abs(var5.getZ() - var1.getZ());
            if (!var2.isRanged() && var6 >= 0.5F) {
               return null;
            } else if (var6 > 3.3F) {
               return null;
            } else if (!var5.isShootable()) {
               return null;
            } else if (var5.isCurrentState(FakeDeadZombieState.instance())) {
               return null;
            } else if (var5.isDead()) {
               return null;
            } else if (var5.getHitReaction() != null && var5.getHitReaction().contains("Death")) {
               return null;
            } else {
               Vector4f var7 = this.tempVector4f;
               this.getNearestTargetPosAndDot(var1, var5, var7);
               float var8 = var7.w;
               float var9 = IsoUtils.DistanceToSquared(var1.getX(), var1.getY(), (float)(PZMath.fastfloor(var1.getZ()) * 3), var7.x, var7.y, (float)(PZMath.fastfloor(var5.getZ()) * 3));
               if (var8 < 0.0F) {
                  return null;
               } else if (var9 > var4 * var4) {
                  return null;
               } else {
                  LosUtil.TestResults var10 = LosUtil.lineClear(var1.getCell(), PZMath.fastfloor(var1.getX()), PZMath.fastfloor(var1.getY()), PZMath.fastfloor(var1.getZ()), PZMath.fastfloor(var5.getX()), PZMath.fastfloor(var5.getY()), PZMath.fastfloor(var5.getZ()), false);
                  return var10 != LosUtil.TestResults.Blocked && var10 != LosUtil.TestResults.ClearThroughClosedDoor ? ((HitInfo)this.hitInfoPool.alloc()).init(var5, var8, var9, var7.x, var7.y, var7.z) : null;
               }
            }
         }
      }
   }

   public static boolean isProne(IsoMovingObject var0) {
      IsoZombie var1 = (IsoZombie)Type.tryCastTo(var0, IsoZombie.class);
      if (var1 == null) {
         return var0.isOnFloor();
      } else if (var1.isOnFloor()) {
         return true;
      } else if (var1.isKnockedDown()) {
         return true;
      } else if (var1.isCurrentState(ZombieEatBodyState.instance())) {
         return true;
      } else if (var1.isDead()) {
         return true;
      } else if (var1.isSitAgainstWall()) {
         return true;
      } else {
         return var1.isCrawling();
      }
   }

   private static boolean isStanding(IsoMovingObject var0) {
      return !isProne(var0);
   }

   public boolean isProneTargetBetter(IsoGameCharacter var1, HitInfo var2, HitInfo var3) {
      if (var2 != null && var2.getObject() != null) {
         if (var3 != null && var3.getObject() != null) {
            if (var2.distSq <= var3.distSq) {
               return false;
            } else {
               boolean var4 = PolygonalMap2.instance.lineClearCollide(var1.getX(), var1.getY(), var2.getObject().getX(), var2.getObject().getY(), PZMath.fastfloor(var1.getZ()), (IsoMovingObject)null, false, true);
               if (!var4) {
                  return false;
               } else {
                  boolean var5 = PolygonalMap2.instance.lineClearCollide(var1.getX(), var1.getY(), var3.getObject().getX(), var3.getObject().getY(), PZMath.fastfloor(var1.getZ()), (IsoMovingObject)null, false, true);
                  return !var5;
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean checkPVP(IsoGameCharacter var0, IsoMovingObject var1) {
      if (var1 instanceof IsoAnimal) {
         return true;
      } else {
         IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(var0, IsoPlayer.class);
         IsoPlayer var3 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
         if (GameClient.bClient && var3 != null && var0 != null) {
            if (var3.isGodMod() || !ServerOptions.instance.PVP.getValue() || ServerOptions.instance.SafetySystem.getValue() && var0.getSafety().isEnabled() && ((IsoGameCharacter)var1).getSafety().isEnabled()) {
               return false;
            }

            if (NonPvpZone.getNonPvpZone(PZMath.fastfloor(var1.getX()), PZMath.fastfloor(var1.getY())) != null) {
               return false;
            }

            if (var2 != null && NonPvpZone.getNonPvpZone(PZMath.fastfloor(var0.getX()), PZMath.fastfloor(var0.getY())) != null) {
               return false;
            }

            if (var2 != null && !var2.factionPvp && !var3.factionPvp) {
               Faction var4 = Faction.getPlayerFaction(var2);
               Faction var5 = Faction.getPlayerFaction(var3);
               if (var5 != null && var4 == var5) {
                  return false;
               }
            }
         }

         return GameClient.bClient || var3 == null || IsoPlayer.getCoopPVP();
      }
   }

   private void CalcHitListShove(IsoGameCharacter var1, boolean var2, AttackVars var3, ArrayList<HitInfo> var4) {
      HandWeapon var5 = var3.getWeapon((IsoLivingCharacter)var1);
      ArrayList var6 = IsoWorld.instance.CurrentCell.getObjectList();

      for(int var7 = 0; var7 < var6.size(); ++var7) {
         IsoMovingObject var8 = (IsoMovingObject)var6.get(var7);
         if (var8 != var1 && !(var8 instanceof BaseVehicle)) {
            IsoGameCharacter var9 = (IsoGameCharacter)Type.tryCastTo(var8, IsoGameCharacter.class);
            if (var9 != null && !var9.isGodMod() && !var9.isDead() && !(var1.DistToSquared(var8) > 9.0F)) {
               IsoZombie var10 = (IsoZombie)Type.tryCastTo(var8, IsoZombie.class);
               if ((var10 == null || !var10.isCurrentState(FakeDeadZombieState.instance())) && (var10 == null || !var10.isReanimatedForGrappleOnly()) && checkPVP(var1, var8)) {
                  boolean var11 = var8 == var3.targetOnGround.getMovingObject() || var8.isShootable() && isStanding(var8) && !var3.bAimAtFloor || var8.isShootable() && isProne(var8) && var3.bAimAtFloor;
                  if (var11) {
                     Vector4f var12 = this.tempVector4f;
                     if (this.getNearestTargetPosAndDot(var1, var5, var8, var2, var12)) {
                        float var13 = var12.w;
                        float var14 = IsoUtils.DistanceToSquared(var1.getX(), var1.getY(), (float)PZMath.fastfloor(var1.getZ()) * 3.0F, var12.x, var12.y, (float)(PZMath.fastfloor(var12.z) * 3));
                        LosUtil.TestResults var15 = LosUtil.lineClear(var1.getCell(), PZMath.fastfloor(var1.getX()), PZMath.fastfloor(var1.getY()), PZMath.fastfloor(var1.getZ()), PZMath.fastfloor(var8.getX()), PZMath.fastfloor(var8.getY()), PZMath.fastfloor(var8.getZ()), false);
                        if (var15 != LosUtil.TestResults.Blocked && var15 != LosUtil.TestResults.ClearThroughClosedDoor && (var8.getCurrentSquare() == null || var1.getCurrentSquare() == null || var8.getCurrentSquare() == var1.getCurrentSquare() || !var8.getCurrentSquare().isWindowBlockedTo(var1.getCurrentSquare())) && var8.getSquare().getTransparentWallTo(var1.getSquare()) == null) {
                           HitInfo var16 = ((HitInfo)this.hitInfoPool.alloc()).init(var8, var13, var14, var12.x, var12.y, var12.z);
                           if (var3.targetOnGround.getMovingObject() == var8) {
                              var4.clear();
                              var4.add(var16);
                              break;
                           }

                           var4.add(var16);
                        }
                     }
                  }
               }
            }
         }
      }

   }

   private void getNearbyGrappleTargets(IsoMovingObject var1, Predicate<IsoMovingObject> var2, Collection<IsoMovingObject> var3) {
      var3.clear();
      int var4 = PZMath.fastfloor(var1.getX());
      int var5 = PZMath.fastfloor(var1.getY());
      int var6 = PZMath.fastfloor(var1.getZ());

      for(int var7 = -2; var7 <= 2; ++var7) {
         for(int var8 = -2; var8 <= 2; ++var8) {
            IsoGridSquare var9 = IsoWorld.instance.CurrentCell.getGridSquare(var4 + var8, var5 + var7, var6);
            if (var9 != null) {
               ArrayList var10 = var9.getMovingObjects();

               for(int var11 = 0; var11 < var10.size(); ++var11) {
                  IsoMovingObject var12 = (IsoMovingObject)var10.get(var11);
                  if (var2.test(var12)) {
                     var3.add(var12);
                  }
               }

               ArrayList var14 = var9.getStaticMovingObjects();

               for(int var15 = 0; var15 < var14.size(); ++var15) {
                  IsoMovingObject var13 = (IsoMovingObject)var14.get(var15);
                  if (var2.test(var13)) {
                     var3.add(var13);
                  }
               }
            }
         }
      }

   }

   private void calcHitListGrapple(IsoGameCharacter var1, boolean var2, AttackVars var3, ArrayList<HitInfo> var4) {
      ArrayList var5 = CombatManager.CalcHitListGrappleReusables.foundObjects;
      var5.clear();
      this.getNearbyGrappleTargets(var1, (var0) -> {
         if (var0 instanceof BaseVehicle) {
            return false;
         } else {
            IsoGameCharacter var1 = (IsoGameCharacter)Type.tryCastTo(var0, IsoGameCharacter.class);
            return var1 == null || !var1.isGodMod();
         }
      }, var5);
      HandWeapon var6 = var3.getWeapon((IsoLivingCharacter)var1);
      Iterator var7 = var5.iterator();

      while(var7.hasNext()) {
         IsoMovingObject var8 = (IsoMovingObject)var7.next();
         if (var8 != var1) {
            Vector4f var9 = CombatManager.CalcHitListGrappleReusables.posAndDot;
            if (this.getNearestTargetPosAndDot(var1, var6, var8, var2, var9)) {
               LosUtil.TestResults var10 = LosUtil.lineClear(var1.getCell(), PZMath.fastfloor(var1.getX()), PZMath.fastfloor(var1.getY()), PZMath.fastfloor(var1.getZ()), PZMath.fastfloor(var8.getX()), PZMath.fastfloor(var8.getY()), PZMath.fastfloor(var8.getZ()), false);
               if (var10 != LosUtil.TestResults.Blocked && var10 != LosUtil.TestResults.ClearThroughClosedDoor && (var8.getCurrentSquare() == null || var1.getCurrentSquare() == null || var8.getCurrentSquare() == var1.getCurrentSquare() || !var8.getCurrentSquare().isWindowBlockedTo(var1.getCurrentSquare())) && var8.getSquare().getTransparentWallTo(var1.getSquare()) == null) {
                  float var11 = var9.w;
                  float var12 = IsoUtils.DistanceToSquared(var1.getX(), var1.getY(), (float)PZMath.fastfloor(var1.getZ()) * 3.0F, var9.x, var9.y, (float)PZMath.fastfloor(var9.z) * 3.0F);
                  HitInfo var13 = ((HitInfo)this.hitInfoPool.alloc()).init(var8, var11, var12, var9.x, var9.y, var9.z);
                  if (var3.targetOnGround.getMovingObject() == var8) {
                     var4.clear();
                     var4.add(var13);
                     break;
                  }

                  var4.add(var13);
               }
            }
         }
      }

      var5.clear();
   }

   private void CalculateHitListWeapon(IsoGameCharacter var1, boolean var2, AttackVars var3, ArrayList<HitInfo> var4) {
      HandWeapon var5 = var3.getWeapon((IsoLivingCharacter)var1);
      ArrayList var6 = IsoWorld.instance.CurrentCell.getObjectList();

      for(int var7 = 0; var7 < var6.size(); ++var7) {
         IsoMovingObject var8 = (IsoMovingObject)var6.get(var7);
         if (var8 != var1) {
            IsoGameCharacter var9 = (IsoGameCharacter)Type.tryCastTo(var8, IsoGameCharacter.class);
            if ((var9 == null || !var9.isGodMod()) && (var9 == null || !var9.isDead())) {
               IsoZombie var10 = (IsoZombie)Type.tryCastTo(var8, IsoZombie.class);
               if ((var10 == null || !var10.isCurrentState(FakeDeadZombieState.instance())) && (var10 == null || !var10.isReanimatedForGrappleOnly()) && (var8 instanceof IsoAnimal || checkPVP(var1, var8))) {
                  boolean var11 = var8 == var3.targetOnGround.getMovingObject() || var8.isShootable() && isStanding(var8) && !var3.bAimAtFloor || var8.isShootable() && isProne(var8) && var3.bAimAtFloor;
                  if (var11) {
                     Vector4f var12 = this.tempVector4f;
                     if (var8 instanceof BaseVehicle) {
                        if (var5.isRanged()) {
                           tempVector3_1.set(var8.getX(), var8.getY(), var8.getZ());
                           if (!this.isHittableBallisticsTarget(var1, var5.getMaxRange(), BallisticsControllerDistanceThreshold, var2, tempVector3_1)) {
                              if (Core.bDebug && DebugOptions.instance.Character.Debug.Render.AimCone.getValue()) {
                                 LineDrawer.DrawIsoCircle(tempVector3_1.x, tempVector3_1.y, tempVector3_1.z, 1.5F, OccludedTargetDebugColor.r, OccludedTargetDebugColor.g, OccludedTargetDebugColor.b, 1.0F);
                              }
                              continue;
                           }

                           var12.set(var8.getX(), var8.getY(), var8.getZ());
                           if (Core.bDebug && DebugOptions.instance.Character.Debug.Render.AimCone.getValue()) {
                              LineDrawer.DrawIsoCircle(tempVector3_1.x, tempVector3_1.y, tempVector3_1.z, 1.5F, TargetableDebugColor.r, TargetableDebugColor.g, TargetableDebugColor.b, 1.0F);
                           }
                        } else {
                           float var13 = var1.getDotWithForwardDirection(var8.getX(), var8.getY());
                           if (var13 < 0.8F) {
                              continue;
                           }

                           var12.set(var8.getX(), var8.getY(), var8.getZ(), var13);
                        }
                     } else {
                        if (var9 == null || var5 != null && !var5.isRanged() && var1.DistToSquared(var8) > 9.0F) {
                           continue;
                        }

                        if (var5.isRanged()) {
                           tempVector3_1.set(var9.getX(), var9.getY(), var9.getZ());
                           if (!this.isHittableBallisticsTarget(var1, var5.getMaxRange(), BallisticsControllerDistanceThreshold, var2, tempVector3_1)) {
                              continue;
                           }

                           var12.set(var9.getX(), var9.getY(), var9.getZ());
                        } else if (!this.getNearestTargetPosAndDot(var1, var5, var8, var2, var12)) {
                           continue;
                        }
                     }

                     LosUtil.TestResults var18 = LosUtil.lineClear(var1.getCell(), PZMath.fastfloor(var1.getX()), PZMath.fastfloor(var1.getY()), PZMath.fastfloor(var1.getZ()), PZMath.fastfloor(var8.getX()), PZMath.fastfloor(var8.getY()), PZMath.fastfloor(var8.getZ()), false);
                     if (var18 != LosUtil.TestResults.Blocked && var18 != LosUtil.TestResults.ClearThroughClosedDoor) {
                        float var14 = var12.w;
                        float var15 = IsoUtils.DistanceToSquared(var1.getX(), var1.getY(), (float)PZMath.fastfloor(var1.getZ()) * 3.0F, var12.x, var12.y, (float)PZMath.fastfloor(var8.getZ()) * 3.0F);
                        if (!var5.isAimedFirearm() && var8.getSquare().getTransparentWallTo(var1.getSquare()) != null && var1 instanceof IsoPlayer) {
                           if (WeaponType.getWeaponType(var1) == WeaponType.spear && !var5.hasTag("NoFenceStab")) {
                              ((IsoPlayer)var1).setAttackType("spearStab");
                           } else if (WeaponType.getWeaponType(var1) != WeaponType.knife) {
                              continue;
                           }
                        }

                        IsoWindow var16 = this.getWindowBetween(var1, var8);
                        if (var16 == null || !var16.isBarricaded()) {
                           HitInfo var17 = ((HitInfo)this.hitInfoPool.alloc()).init(var8, var14, var15, var12.x, var12.y, var12.z);
                           var17.window.setObject(var16);
                           var4.add(var17);
                        }
                     }
                  }
               }
            }
         }
      }

      if (!var4.isEmpty() && var5.isRanged()) {
         this.processBallisticsTargets(var1, var5, var4);
      } else {
         this.CalcHitListWindow(var1, var5, var4);
      }
   }

   private void processBallisticsTargets(IsoGameCharacter var1, HandWeapon var2, ArrayList<HitInfo> var3) {
      if (!var3.isEmpty()) {
         for(int var4 = 0; var4 < var3.size(); ++var4) {
            HitInfo var5 = (HitInfo)var3.get(var4);
            IsoMovingObject var7 = var5.getObject();
            if (var7 instanceof IsoZombie) {
               IsoZombie var6 = (IsoZombie)var7;
               if (var6.useBallistics()) {
                  BallisticsTarget var18 = var6.ensureExitsBallisticsTarget(var6);
                  if (var18 != null) {
                     this.highlightTarget(var6, Color.yellow, 0.65F);
                     var18.add();
                  }
               }
            }
         }
      }

      BallisticsController var13 = var1.getBallisticsController();
      if (var13 != null) {
         int var16;
         if (var2.isRanged() && var1.useBallistics()) {
            targetReticleMode = 1;
            float var14 = var2.getMaxRange(var1);
            var13.setRange(var14);
            if (var2.isRangeFalloff()) {
               var16 = var2.getProjectileCount();
               float var19 = var2.getProjectileSpread();
               float var8 = var2.getProjectileWeightCenter();
               var1.getBallisticsController().getSpreadData(var14, var19, var8, var16);
            } else {
               var1.getBallisticsController().getCameraTargets(var14 + 0.5F, true);
               var1.getBallisticsController().getTargets(var14);
            }
         }

         int var15;
         if (!var3.isEmpty()) {
            for(var15 = 0; var15 < var3.size(); ++var15) {
               HitInfo var17 = (HitInfo)var3.get(var15);
               IsoMovingObject var21 = var17.getObject();
               if (var21 instanceof IsoZombie) {
                  IsoZombie var20 = (IsoZombie)var21;
                  if (var20.useBallistics()) {
                     BallisticsTarget var23 = var20.getBallisticsTarget();
                     if (var23 != null) {
                        if (!var13.isValidTarget(var20.getID()) && !var13.isValidCachedTarget(var20.getID())) {
                           this.highlightTarget(var20, Color.white, 0.65F);
                           var3.remove(var15);
                           --var15;
                        } else {
                           if (var13.isCameraTarget(var20.getID()) || var13.isCachedCameraTarget(var20.getID())) {
                              this.highlightTarget(var20, Color.red, 0.65F);
                           }

                           if (var13.isSpreadTarget(var20.getID()) || var13.isCachedSpreadTarget(var20.getID())) {
                              this.highlightTarget(var20, Color.magenta, 0.65F);
                           }
                        }
                     }
                  }
               }
            }
         }

         if (var2.isRangeFalloff()) {
            if (!var3.isEmpty()) {
               var15 = var3.size();

               for(var16 = 0; var16 < var15; ++var16) {
                  HitInfo var22 = (HitInfo)var3.get(var16);
                  IsoMovingObject var9 = var22.getObject();
                  if (var9 instanceof IsoZombie) {
                     IsoZombie var24 = (IsoZombie)var9;
                     if (var24.useBallistics()) {
                        BallisticsTarget var25 = var24.getBallisticsTarget();
                        if (var25 != null) {
                           int var10 = var13.spreadCount(var24.getID());
                           if (var10 > 1) {
                              for(int var11 = 0; var11 < var10 - 1; ++var11) {
                                 HitInfo var12 = ((HitInfo)this.hitInfoPool.alloc()).init(var22);
                                 var3.add(var12);
                              }
                           }
                        }
                     }
                  }
               }
            }

         }
      }
   }

   private void CalcHitListWindow(IsoGameCharacter var1, HandWeapon var2, ArrayList<HitInfo> var3) {
      Vector2 var4 = var1.getLookVector(tempVector2_1);
      var4.setLength(var2.getMaxRange(var1) * var2.getRangeMod(var1));
      HitInfo var5 = null;
      ArrayList var6 = IsoWorld.instance.CurrentCell.getWindowList();

      for(int var7 = 0; var7 < var6.size(); ++var7) {
         IsoWindow var8 = (IsoWindow)var6.get(var7);
         if (PZMath.fastfloor(var8.getZ()) == PZMath.fastfloor(var1.getZ()) && this.windowVisitor.isHittable(var8)) {
            float var9 = var8.getX();
            float var10 = var8.getY();
            float var11 = var9 + (var8.getNorth() ? 1.0F : 0.0F);
            float var12 = var10 + (var8.getNorth() ? 0.0F : 1.0F);
            if (Line2D.linesIntersect((double)var1.getX(), (double)var1.getY(), (double)(var1.getX() + var4.x), (double)(var1.getY() + var4.y), (double)var9, (double)var10, (double)var11, (double)var12)) {
               IsoGridSquare var13 = var8.getAddSheetSquare(var1);
               if (var13 != null && !LosUtil.lineClearCollide(PZMath.fastfloor(var1.getX()), PZMath.fastfloor(var1.getY()), PZMath.fastfloor(var1.getZ()), var13.x, var13.y, var13.z, false)) {
                  float var14 = IsoUtils.DistanceToSquared(var1.getX(), var1.getY(), var9 + (var11 - var9) / 2.0F, var10 + (var12 - var10) / 2.0F);
                  if (var5 == null || !(var5.distSq < var14)) {
                     float var15 = 1.0F;
                     if (var5 == null) {
                        var5 = (HitInfo)this.hitInfoPool.alloc();
                     }

                     var5.init(var8, var15, var14);
                  }
               }
            }
         }
      }

      if (var5 != null) {
         var3.add(var5);
      }

   }

   public void calculateHitInfoList(IsoGameCharacter var1) {
      ArrayList var2 = var1.getHitInfoList();
      AttackVars var3 = var1.getAttackVars();
      this.calculateHitInfoList(var1, false, var3, var2);
   }

   private void calculateHitInfoList(IsoGameCharacter var1, boolean var2, AttackVars var3, ArrayList<HitInfo> var4) {
      if (!GameClient.bClient || var1.isLocal()) {
         if (var4.isEmpty()) {
            HandWeapon var5 = var3.getWeapon((IsoLivingCharacter)var1);
            int var6 = var5.getMaxHitCount();
            if (var3.bDoShove) {
               var6 = WeaponType.getWeaponType(var1) != WeaponType.barehand ? 3 : 1;
            }

            if (!var5.isRanged() && !SandboxOptions.instance.MultiHitZombies.getValue()) {
               var6 = 1;
            }

            if (var5 == ((IsoPlayer)var1).bareHands && !(var1.getPrimaryHandItem() instanceof HandWeapon)) {
               var6 = 1;
            }

            if (var5 == ((IsoPlayer)var1).bareHands && var3.targetOnGround.getMovingObject() != null) {
               var6 = 1;
            }

            if (var6 > 0) {
               if (var3.bDoShove) {
                  this.CalcHitListShove(var1, var2, var3, var4);
               } else if (var3.bDoGrapple) {
                  this.calcHitListGrapple(var1, var2, var3, var4);
               } else {
                  this.CalculateHitListWeapon(var1, var2, var3, var4);
               }

               if (var4.size() != 1 || ((HitInfo)var4.get(0)).getObject() != null) {
                  float var7 = var1.getZ();
                  this.filterTargetsByZ(var7, var4);
                  Object var8 = this.meleeTargetComparator;
                  if (var5.isRanged()) {
                     var8 = this.rangeTargetComparator;
                  }

                  ((TargetComparator)var8).setBallisticsController(var1.getBallisticsController());
                  var4.sort((Comparator)var8);
                  if (var5.isPiercingBullets()) {
                     HitList2.clear();
                     double var9 = 0.0;

                     for(int var11 = 0; var11 < var4.size(); ++var11) {
                        HitInfo var12 = (HitInfo)var4.get(var11);
                        IsoMovingObject var13 = var12.getObject();
                        if (var13 != null) {
                           double var14 = (double)(var1.getX() - var13.getX());
                           double var16 = (double)(-(var1.getY() - var13.getY()));
                           double var18 = Math.atan2(var16, var14);
                           if (var18 < 0.0) {
                              var18 = Math.abs(var18);
                           } else {
                              var18 = 6.283185307179586 - var18;
                           }

                           if (var11 == 0) {
                              var9 = Math.toDegrees(var18);
                              HitList2.add(var12);
                           } else {
                              double var20 = Math.toDegrees(var18);
                              if (Math.abs(var9 - var20) < 1.0) {
                                 HitList2.add(var12);
                                 break;
                              }
                           }
                        }
                     }

                     var4.removeAll(HitList2);
                     this.hitInfoPool.release((List)var4);
                     var4.clear();
                     var4.addAll(HitList2);
                  } else {
                     while(var4.size() > var6) {
                        this.hitInfoPool.release((Object)((HitInfo)var4.remove(var4.size() - 1)));
                     }
                  }

                  for(int var22 = 0; var22 < var4.size(); ++var22) {
                     HitInfo var10 = (HitInfo)var4.get(var22);
                     HitChanceData var23 = this.calculateHitChanceData(var1, var5, var10);
                     var10.chance = (int)var23.hitChance;
                     if (DebugOptions.instance.Character.Debug.AlwaysHitTarget.getValue()) {
                        var10.chance = 100;
                     }
                  }

               }
            }
         }
      }
   }

   private void filterTargetsByZ(float var1, ArrayList<HitInfo> var2) {
      float var3 = 3.4028235E38F;
      HitInfo var4 = null;

      int var5;
      HitInfo var6;
      IsoMovingObject var7;
      float var8;
      float var9;
      for(var5 = 0; var5 < var2.size(); ++var5) {
         var6 = (HitInfo)var2.get(var5);
         var7 = var6.getObject();
         var8 = var7 == null ? var6.z : var7.getZ();
         var9 = Math.abs(var8 - var1);
         if (var9 < var3) {
            var3 = var9;
            var4 = var6;
         }
      }

      if (var4 != null) {
         for(var5 = var2.size() - 1; var5 >= 0; --var5) {
            var6 = (HitInfo)var2.get(var5);
            if (var6 != var4) {
               var7 = var6.getObject();
               var8 = var7 == null ? var6.z : var7.getZ();
               var9 = Math.abs(var8 - var4.z);
               if (var9 > 0.5F) {
                  this.hitInfoPool.release((Object)var6);
                  var2.remove(var5);
               }
            }
         }

      }
   }

   private static int getBoneIndex(IsoMovingObject var0, String var1) {
      IsoGameCharacter var2 = (IsoGameCharacter)Type.tryCastTo(var0, IsoGameCharacter.class);
      if (var2 != null && var1 != null) {
         AnimationPlayer var3 = var2.getAnimationPlayer();
         return var3 != null && var3.isReady() ? var3.getSkinningBoneIndex(var1, -1) : -1;
      } else {
         return -1;
      }
   }

   public static Vector3 getBoneWorldPos(IsoMovingObject var0, String var1, Vector3 var2) {
      int var3 = getBoneIndex(var0, var1);
      if (var3 == -1) {
         return var2.set(var0.getX(), var0.getY(), var0.getZ());
      } else {
         Model.BoneToWorldCoords((IsoGameCharacter)var0, var3, var2);
         return var2;
      }
   }

   private static Vector3 getPointAlongBoneXAxis(IsoMovingObject var0, String var1, float var2, Vector3 var3) {
      int var4 = getBoneIndex(var0, var1);
      if (var4 == -1) {
         return var3.set(var0.getX(), var0.getY(), var0.getZ());
      } else {
         AnimationPlayer var5 = ((IsoGameCharacter)var0).getAnimationPlayer();
         Matrix4f var6 = var5.getModelTransformAt(var4);
         float var7 = var6.m03;
         float var8 = var6.m13;
         float var9 = var6.m23;
         float var10 = var6.m00;
         float var11 = var6.m10;
         float var12 = var6.m20;
         return var3.set(var7 + var10 * var2, var8 + var11 * var2, var9 + var12 * var2);
      }
   }

   public static float getDistanceModifierSightless(float var0, boolean var1) {
      if (var0 > 3.0F) {
         return (3.0F - var0) * (15.0F + (3.0F - var0) * -0.7F);
      } else {
         return var0 < 3.0F ? (3.0F - var0) / 3.0F * 40.0F * (var1 ? 2.0F : 1.0F) : 0.0F;
      }
   }

   public static float getAimDelayPenaltySightless(float var0, float var1) {
      if (var1 < 3.0F) {
         var0 *= var1 / 3.0F;
      } else if (var1 > 3.0F) {
         var0 *= 1.0F + (var1 - 3.0F) * 0.1F;
      }

      return var0;
   }

   public static float getDistanceModifier(float var0, float var1, float var2, boolean var3) {
      if (var0 < var1) {
         return var0 > 3.0F ? (var0 - var1) * (4.0F + (var0 - var1) * -0.3F) : 0.0F;
      } else if (var0 > var2) {
         return -((var0 - var2) * (4.0F + (var0 - var2) * 0.3F));
      } else {
         float var4 = (var2 - var1) * 0.5F;
         return (float)(15.0 * Math.exp(-Math.pow((double)(var0 - (var1 + var4)), 2.0) / Math.pow((double)(2.0F * ((var2 - var1) / 7.0F)), 2.0)));
      }
   }

   public static float getMovePenalty(IsoGameCharacter var0, float var1) {
      float var2 = var0.getBeenMovingFor() * (1.0F - (float)(var0.getPerkLevel(PerkFactory.Perks.Aiming) + var0.getPerkLevel(PerkFactory.Perks.Nimble)) / 40.0F);
      if (var1 < 10.0F) {
         var2 *= var1 / 10.0F;
      } else {
         var2 *= 1.0F + (var1 - 10.0F) * 0.07F;
      }

      return var2;
   }

   public static float getAimDelayPenalty(float var0, float var1, float var2, float var3) {
      if (var2 > -1.0F && var1 >= var2 && var1 <= var3) {
         float var4 = (var3 - var2) * 0.5F;
         var0 *= 1.0F - (1.0F - Math.abs(var1 - (var2 + var4)) / var4) * 0.25F;
      } else if (var1 > var3) {
         var0 *= 1.0F + (var1 - var3) * 0.1F;
      }

      if (var1 < 3.0F) {
         var0 *= var1 / 3.0F;
      }

      return var0;
   }

   public static float getMoodlesPenalty(IsoGameCharacter var0, float var1) {
      return (float)var0.getMoodles().getMoodleLevel(MoodleType.Panic) * (8.0F + var1 * 0.5F) + (float)var0.getMoodles().getMoodleLevel(MoodleType.Stress) * (8.0F + var1 * 0.5F) + (float)var0.getMoodles().getMoodleLevel(MoodleType.Tired) * 5.0F + (float)var0.getMoodles().getMoodleLevel(MoodleType.Endurance) * 5.0F + (float)var0.getMoodles().getMoodleLevel(MoodleType.Drunk) * (8.0F + var1 * 0.5F);
   }

   public static float getWeatherPenalty(IsoGameCharacter var0, HandWeapon var1, IsoGridSquare var2, float var3) {
      float var4 = 0.0F;
      boolean var5 = var1.getActiveSight() != null && var1.getActiveSight().hasTag("thermal");
      if (var2.isOutside()) {
         var4 += ClimateManager.getInstance().getWindIntensity() * (6.0F - (float)var0.getPerkLevel(PerkFactory.Perks.Aiming) * 0.2F) * var3 * (var0.getCharacterTraits().Marksman.isSet() ? 0.6F : 1.0F);
         var4 += ClimateManager.getInstance().getRainIntensity() * var3 * 0.5F;
         var4 *= var0.Traits.getTraitWeatherPenaltyModifier();
         if (!var5) {
            var4 += ClimateManager.getInstance().getFogIntensity() * 10.0F * var3;
         }

         var4 *= PZMath.min(var3 / 3.0F, 1.0F);
      }

      if (var5) {
         return var4;
      } else {
         float var6 = var2.getLightLevel(var0 instanceof IsoPlayer ? ((IsoPlayer)var0).getPlayerNum() : -1);
         if (var6 < 0.75F) {
            float var7 = PZMath.max(0.0F, 50.0F * (1.0F - var6 / 0.75F));
            var4 += var7 - var1.getLowLightBonus();
         }

         return var4;
      }
   }

   private HitChanceData calculateHitChanceData(IsoGameCharacter var1, HandWeapon var2, HitInfo var3) {
      HitChanceData var4 = new HitChanceData();
      IsoMovingObject var5 = null;
      if (var3 != null) {
         var5 = var3.getObject();
      }

      float var6 = (float)var2.getHitChance();
      if (var6 > 95.0F) {
         var6 = 95.0F;
      }

      var6 += var2.getAimingPerkHitChanceModifier() * (float)var1.getPerkLevel(PerkFactory.Perks.Aiming);
      int var14;
      float var15;
      if (var1.getVehicle() != null && var5 != null) {
         BaseVehicle var7 = var1.getVehicle();
         Vector3f var8 = var7.getForwardVector((Vector3f)((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).alloc());
         Vector2 var9 = (Vector2)((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).alloc();
         var9.x = var8.x;
         var9.y = var8.z;
         var9.normalize();
         Vector2 var10 = (Vector2)((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).alloc();
         var10.x = var5.getX();
         var10.y = var5.getY();
         var10.x -= var1.getX();
         var10.y -= var1.getY();
         var10.normalize();
         boolean var11 = var10.dot(var9) < 0.0F;
         int var12 = var7.getSeat(var1);
         VehicleScript.Area var13 = var7.getScript().getAreaById(var7.getPassengerArea(var12));
         var14 = var13.x > 0.0F ? 90 : -90;
         var9.rotate((float)Math.toRadians((double)var14));
         var9.normalize();
         var15 = var10.dot(var9);
         ((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).release(var9);
         ((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).release(var10);
         ((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).release(var8);
         if (var15 > -0.6F && !var2.isRanged()) {
            return var4;
         }

         if (var15 > -0.1F) {
            return var4;
         }

         if (!var2.isRanged()) {
            var4.hitChance = 100.0F;
            return var4;
         }

         if (var1.isDriving() && var2.isTwoHandWeapon()) {
            return var4;
         }

         float var16;
         if (var15 > -0.7F) {
            var16 = -0.1F;
            if (var7.isDriver(var1)) {
               var16 -= 0.15F;
            }

            VehiclePart var17 = var7.getPartForSeatContainer(var12);
            if (var17 != null) {
               var16 -= var17.getItemContainer().getCapacityWeight() * (var2.isTwoHandWeapon() ? 0.05F : 0.025F);
            }

            var16 -= var1.getInventory().getCapacityWeight() * 0.01F;
            if (var11) {
               var16 -= var2.isTwoHandWeapon() ? 0.15F : 0.1F;
            }

            float var18 = PZMath.clamp(40.0F * (1.0F - (var15 - 0.1F) / -0.8F), 0.0F, 40.0F) * (var2.isTwoHandWeapon() ? 1.5F : 1.0F) * (var11 ? 1.5F : 1.0F) * (var15 <= var16 ? 1.0F : 3.0F);
            var6 -= var18;
            var4.aimPenalty += var18;
         }

         var16 = Math.abs(var7.getCurrentSpeedKmHour()) * (var7.isDriver(var1) ? 3.0F : 2.0F);
         var6 -= var16;
         var4.aimPenalty += var16;
      }

      if (!var2.isRanged()) {
         var4.hitChance = 100.0F;
         return var4;
      } else {
         float var19 = var2.getMaxSightRange(var1);
         float var20 = var2.getMinSightRange(var1);
         float var21 = var19;
         if (var3 != null) {
            var21 = PZMath.sqrt(var3.distSq);
         }

         boolean var22 = false;
         if (var5 != null) {
            var22 = isProne(var5);
         }

         float var23 = PZMath.max(getDistanceModifierSightless(var21, var22) - getAimDelayPenaltySightless(PZMath.max(0.0F, var1.getAimingDelay()), var21), getDistanceModifier(var21, var20, var19, var22) - getAimDelayPenalty(PZMath.max(0.0F, var1.getAimingDelay()), var21, var20, var19));
         var6 += var23;
         var4.aimPenalty += getAimDelayPenalty(var1.getAimingDelay(), var21, var20, var19) * 20.0F;
         float var24 = getMovePenalty(var1, var21);
         var6 -= var24;
         var4.aimPenalty += var24;
         if (var3 != null) {
            IsoMovingObject var27 = var3.getObject();
            if (var27 instanceof IsoPlayer) {
               IsoPlayer var25 = (IsoPlayer)var27;
               if (var25.getVehicle() != null) {
                  float var28 = Math.abs(var25.getVehicle().getCurrentSpeedKmHour()) * 2.0F;
                  var6 -= var28;
                  var4.aimPenalty += var28;
               } else if (var25.isSprinting()) {
                  var6 -= 25.0F;
                  var4.aimPenalty += 25.0F;
               } else if (var25.isRunning()) {
                  var6 -= 15.0F;
                  var4.aimPenalty += 15.0F;
               } else if (var25.isPlayerMoving()) {
                  var6 -= 5.0F;
                  var4.aimPenalty += 5.0F;
               }
            }
         }

         if (var1.Traits.Marksman.isSet()) {
            var6 += 20.0F;
         }

         float var26 = 0.0F;

         for(var14 = BodyPartType.ToIndex(BodyPartType.Hand_L); var14 <= BodyPartType.ToIndex(BodyPartType.UpperArm_R); ++var14) {
            var26 += ((BodyPart)var1.getBodyDamage().getBodyParts().get(var14)).getPain();
         }

         if (var26 > 0.0F) {
            var6 -= var26 * 0.1F;
            var4.aimPenalty += var26 * 0.1F;
         }

         IsoGridSquare var29 = var1.getSquare();
         if (var5 != null) {
            var29 = var5.getSquare();
         }

         var15 = getWeatherPenalty(var1, var2, var29, var21);
         var6 -= var15;
         var4.aimPenalty += var15;
         var15 = getMoodlesPenalty(var1, var21);
         var6 -= var15;
         var4.aimPenalty += var15;
         var15 = 100.0F - 100.0F / var1.getWornItemsVisionModifier();
         var6 -= var15;
         var4.aimPenalty += var15;
         var4.hitChance = PZMath.clamp((float)((int)var6), 5.0F, 100.0F);
         return var4;
      }
   }

   private LosUtil.TestResults los(int var1, int var2, int var3, int var4, int var5, LOSVisitor var6) {
      IsoCell var7 = IsoWorld.instance.CurrentCell;
      int var10 = var4 - var2;
      int var11 = var3 - var1;
      int var12 = var5 - var5;
      float var13 = 0.5F;
      float var14 = 0.5F;
      IsoGridSquare var15 = var7.getGridSquare(var1, var2, var5);
      float var16;
      float var17;
      IsoGridSquare var18;
      if (Math.abs(var11) > Math.abs(var10)) {
         var16 = (float)var10 / (float)var11;
         var17 = (float)var12 / (float)var11;
         var13 += (float)var2;
         var14 += (float)var5;
         var11 = var11 < 0 ? -1 : 1;
         var16 *= (float)var11;

         for(var17 *= (float)var11; var1 != var3; var15 = var18) {
            var1 += var11;
            var13 += var16;
            var14 += var17;
            var18 = var7.getGridSquare(var1, PZMath.fastfloor(var13), PZMath.fastfloor(var14));
            if (var6.visit(var18, var15)) {
               return var6.getResult();
            }
         }
      } else {
         var16 = (float)var11 / (float)var10;
         var17 = (float)var12 / (float)var10;
         var13 += (float)var1;
         var14 += (float)var5;
         var10 = var10 < 0 ? -1 : 1;
         var16 *= (float)var10;

         for(var17 *= (float)var10; var2 != var4; var15 = var18) {
            var2 += var10;
            var13 += var16;
            var14 += var17;
            var18 = var7.getGridSquare(PZMath.fastfloor(var13), var2, PZMath.fastfloor(var14));
            if (var6.visit(var18, var15)) {
               return var6.getResult();
            }
         }
      }

      return LosUtil.TestResults.Clear;
   }

   private IsoWindow getWindowBetween(int var1, int var2, int var3, int var4, int var5) {
      this.windowVisitor.init();
      this.los(var1, var2, var3, var4, var5, this.windowVisitor);
      return this.windowVisitor.window;
   }

   private IsoWindow getWindowBetween(IsoMovingObject var1, IsoMovingObject var2) {
      return this.getWindowBetween(PZMath.fastfloor(var1.getX()), PZMath.fastfloor(var1.getY()), PZMath.fastfloor(var2.getX()), PZMath.fastfloor(var2.getY()), PZMath.fastfloor(var1.getZ()));
   }

   public boolean isWindowBetween(IsoMovingObject var1, IsoMovingObject var2) {
      return this.getWindowBetween(var1, var2) != null;
   }

   public void smashWindowBetween(IsoGameCharacter var1, IsoMovingObject var2, HandWeapon var3) {
      IsoWindow var4 = this.getWindowBetween(var1, var2);
      if (var4 != null) {
         var4.WeaponHit(var1, var3);
      }
   }

   public void Reset() {
      this.hitInfoPool.forEach((var0) -> {
         var0.object = null;
      });
   }

   public void processHit(HandWeapon var1, IsoGameCharacter var2, IsoZombie var3) {
      String var4 = "";
      String var5 = var2.getVariableString("ZombieHitReaction");
      if ("Shot".equals(var5)) {
         var5 = "ShotBelly";
         var2.setCriticalHit(Rand.Next(100) < ((IsoPlayer)var2).calculateCritChance(var3));
         int var6 = RagdollBodyPart.BODYPART_COUNT.ordinal();
         BallisticsController var7 = var2.getBallisticsController();
         if (var7.isCameraTarget(var3.getID())) {
            var6 = var7.getCachedTargetedBodyPart(var3.getID());
            String var8 = RagdollBodyPart.values()[var6].name();
            DebugLog.Combat.println("CombatManager::ProcessHit %d isCameraTarget and hit BodyPart %d - %s", var3.getID(), var6, var8);
         }

         if (var6 != RagdollBodyPart.BODYPART_COUNT.ordinal()) {
            this.processTargetedHit(var1, var2, var3, RagdollBodyPart.values()[var6]);
            return;
         }

         var5 = "ShotBelly";
         Vector2 var20 = var2.getForwardDirection();
         Vector2 var9 = var3.getHitAngle();
         double var10 = (double)(var20.x * var9.y - var20.y * var9.x);
         double var12 = var10 >= 0.0 ? 1.0 : -1.0;
         double var14 = (double)(var20.x * var9.x + var20.y * var9.y);
         double var16 = Math.acos(var14) * var12;
         var4 = this.calculateShotDirection(var3, var16);
         int var18;
         if ("N".equals(var4)) {
            if (var3.isHitFromBehind()) {
               var5 = "ShotBellyStep";
            } else {
               var18 = Rand.Next(2);
               switch (var18) {
                  case 0:
                     var5 = "ShotBelly";
                     break;
                  case 1:
                     var5 = "ShotBellyStep";
               }
            }
         }

         if ("S".equals(var4)) {
            var5 = "ShotBellyStep";
         }

         if ("L".equals(var4) || "R".equals(var4)) {
            if (var3.isHitFromBehind()) {
               var18 = Rand.Next(3);
               switch (var18) {
                  case 0:
                     var5 = "ShotChest";
                     break;
                  case 1:
                     var5 = "ShotLeg";
                     break;
                  case 2:
                     var5 = "ShotShoulderStep";
               }
            } else {
               var18 = Rand.Next(5);
               switch (var18) {
                  case 0:
                     var5 = "ShotChest";
                     break;
                  case 1:
                     var5 = "ShotChestStep";
                     break;
                  case 2:
                     var5 = "ShotLeg";
                     break;
                  case 3:
                     var5 = "ShotShoulder";
                     break;
                  case 4:
                     var5 = "ShotShoulderStep";
               }
            }

            var5 = var5 + var4;
         }

         if (var2.isCriticalHit()) {
            if ("S".equals(var4)) {
               var5 = "ShotHeadFwd";
            }

            if ("N".equals(var4)) {
               var5 = "ShotHeadBwd";
            }

            if (("L".equals(var4) || "R".equals(var4)) && Rand.Next(4) == 0) {
               var5 = "ShotHeadBwd";
            }
         }
      }

      this.applyBlood(var1, var3, var5, var4);
      if ("ShotHeadFwd".equals(var5) && Rand.Next(2) == 0) {
         var5 = "ShotHeadFwd02";
      }

      if (var3.getEatBodyTarget() != null) {
         if (var3.getVariableBoolean("onknees")) {
            var5 = "OnKnees";
         } else {
            var5 = "Eating";
         }
      }

      if ("Floor".equalsIgnoreCase(var5) && var3.isCurrentState(ZombieGetUpState.instance()) && var3.isFallOnFront()) {
         var5 = "GettingUpFront";
      }

      if (!var5.isEmpty()) {
         var3.setHitReaction(var5);
      } else {
         var3.setStaggerBack(true);
         var3.setHitReaction("");
         if ("LEFT".equals(var3.getPlayerAttackPosition()) || "RIGHT".equals(var3.getPlayerAttackPosition())) {
            var2.setCriticalHit(false);
         }
      }

      RagdollBodyPart var19 = this.getBodyPart(var5, var4);
      this.createCombatData(var1, var2, var3, var19);
   }

   private RagdollBodyPart getBodyPart(String var1, String var2) {
      if (var1.contains("Head")) {
         return RagdollBodyPart.BODYPART_HEAD;
      } else if (var1.contains("Chest")) {
         return RagdollBodyPart.BODYPART_SPINE;
      } else if (var1.contains("Belly")) {
         return RagdollBodyPart.BODYPART_PELVIS;
      } else {
         boolean var3;
         if (var1.contains("Leg")) {
            var3 = Rand.Next(2) == 0;
            if ("L".equals(var2)) {
               return var3 ? RagdollBodyPart.BODYPART_LEFT_LOWER_LEG : RagdollBodyPart.BODYPART_LEFT_UPPER_LEG;
            } else {
               return var3 ? RagdollBodyPart.BODYPART_RIGHT_LOWER_LEG : RagdollBodyPart.BODYPART_RIGHT_UPPER_LEG;
            }
         } else if (var1.contains("Shoulder")) {
            var3 = Rand.Next(2) == 0;
            if ("L".equals(var2)) {
               return var3 ? RagdollBodyPart.BODYPART_LEFT_LOWER_ARM : RagdollBodyPart.BODYPART_LEFT_UPPER_ARM;
            } else {
               return var3 ? RagdollBodyPart.BODYPART_RIGHT_LOWER_ARM : RagdollBodyPart.BODYPART_RIGHT_UPPER_ARM;
            }
         } else {
            return RagdollBodyPart.BODYPART_SPINE;
         }
      }
   }

   private String calculateShotDirection(IsoZombie var1, double var2) {
      String var4 = "";
      if (var2 < 0.0) {
         var2 += 6.283185307179586;
      }

      int var5;
      if (Math.toDegrees(var2) < 45.0) {
         var4 = "S";
         var5 = Rand.Next(9);
         if (var5 > 6) {
            var4 = "L";
         }

         if (var5 > 4) {
            var4 = "R";
         }
      }

      if (Math.toDegrees(var2) > 45.0 && Math.toDegrees(var2) < 90.0) {
         if (Rand.Next(4) == 0) {
            var4 = "S";
         } else {
            var4 = "R";
         }
      }

      if (Math.toDegrees(var2) > 90.0 && Math.toDegrees(var2) < 135.0) {
         var4 = "R";
      }

      if (Math.toDegrees(var2) > 135.0 && Math.toDegrees(var2) < 180.0) {
         if (Rand.Next(4) == 0) {
            var4 = "N";
         } else {
            var4 = "R";
         }
      }

      if (Math.toDegrees(var2) > 180.0 && Math.toDegrees(var2) < 225.0) {
         var4 = "N";
         var5 = Rand.Next(9);
         if (var5 > 6) {
            var4 = "L";
         }

         if (var5 > 4) {
            var4 = "R";
         }
      }

      if (Math.toDegrees(var2) > 225.0 && Math.toDegrees(var2) < 270.0) {
         if (Rand.Next(4) == 0) {
            var4 = "N";
         } else {
            var4 = "L";
         }
      }

      if (Math.toDegrees(var2) > 270.0 && Math.toDegrees(var2) < 315.0) {
         var4 = "L";
      }

      if (Math.toDegrees(var2) > 315.0) {
         if (Rand.Next(4) == 0) {
            var4 = "S";
         } else {
            var4 = "L";
         }
      }

      return var4;
   }

   private void applyBlood(HandWeapon var1, IsoZombie var2, String var3, String var4) {
      if (var3.contains("Shot")) {
         if (var3.contains("Head")) {
            var2.addBlood(BloodBodyPartType.Head, false, true, true);
         } else if (var3.contains("Chest")) {
            var2.addBlood(BloodBodyPartType.Torso_Upper, !var2.isCriticalHit(), var2.isCriticalHit(), true);
         } else if (var3.contains("Belly")) {
            var2.addBlood(BloodBodyPartType.Torso_Lower, !var2.isCriticalHit(), var2.isCriticalHit(), true);
         } else {
            boolean var5;
            if (var3.contains("Leg")) {
               var5 = Rand.Next(2) == 0;
               if ("L".equals(var4)) {
                  var2.addBlood(var5 ? BloodBodyPartType.LowerLeg_L : BloodBodyPartType.UpperLeg_L, !var2.isCriticalHit(), var2.isCriticalHit(), true);
               } else {
                  var2.addBlood(var5 ? BloodBodyPartType.LowerLeg_R : BloodBodyPartType.UpperLeg_R, !var2.isCriticalHit(), var2.isCriticalHit(), true);
               }
            } else if (var3.contains("Shoulder")) {
               var5 = Rand.Next(2) == 0;
               if ("L".equals(var4)) {
                  var2.addBlood(var5 ? BloodBodyPartType.ForeArm_L : BloodBodyPartType.UpperArm_L, !var2.isCriticalHit(), var2.isCriticalHit(), true);
               } else {
                  var2.addBlood(var5 ? BloodBodyPartType.ForeArm_R : BloodBodyPartType.UpperArm_R, !var2.isCriticalHit(), var2.isCriticalHit(), true);
               }
            }
         }
      } else if (var1.getCategories().contains("Blunt")) {
         var2.addBlood(BloodBodyPartType.FromIndex(Rand.Next(BloodBodyPartType.UpperArm_L.index(), BloodBodyPartType.Groin.index())), false, false, true);
      } else if (!var1.getCategories().contains("Unarmed")) {
         var2.addBlood(BloodBodyPartType.FromIndex(Rand.Next(BloodBodyPartType.UpperArm_L.index(), BloodBodyPartType.Groin.index())), false, true, true);
      }

   }

   public void highlightTarget(IsoGameCharacter var1, Color var2, float var3) {
      if (DebugOptions.instance.PhysicsRenderHighlightBallisticsTargets.getValue()) {
         var1.setOutlineHighlight(true);
         var1.setOutlineHighlightCol(var2.r, var2.g, var2.b, var3);
      }
   }

   private void highlightTargets(IsoPlayer var1) {
      boolean var2 = false;
      HandWeapon var3 = (HandWeapon)Type.tryCastTo(var1.getPrimaryHandItem(), HandWeapon.class);
      if (var3 != null) {
         var2 = var3.isRanged();
      }

      if (DebugOptions.instance.Character.Debug.Render.MeleeOutline.getValue() && !var2) {
         this.highlightMeleeTargets(var1, Color.cyan.r, Color.cyan.g, Color.cyan.b, 0.65F);
      }

      if (!var2) {
         this.highlightTargets(var1, var1.getHitInfoList());
      }
   }

   private void highlightTargets(IsoPlayer var1, ArrayList<HitInfo> var2) {
      if (Core.getInstance().getOptionMeleeOutline()) {
         if (var1.isLocalPlayer() && !var1.isNPC() && var1.isAiming()) {
            ColorInfo var3 = Core.getInstance().getBadHighlitedColor();
            this.highlightMeleeTargets(var1, var3.r, var3.g, var3.b, 1.0F);
         }
      }
   }

   private void highlightMeleeTargets(IsoPlayer var1, float var2, float var3, float var4, float var5) {
      this.calculateHitInfoList(var1);
      ArrayList var6 = var1.getHitInfoList();
      Iterator var7 = var6.iterator();

      while(var7.hasNext()) {
         HitInfo var8 = (HitInfo)var7.next();
         IsoMovingObject var10 = var8.getObject();
         if (var10 instanceof IsoGameCharacter var9) {
            var9.setOutlineHighlight(true);
            var9.setOutlineHighlightCol(var2, var3, var4, var5);
         }
      }

   }

   public void pressedAttack(IsoPlayer var1) {
      boolean var2 = GameClient.bClient && !var1.isLocalPlayer();
      boolean var3 = var1.isSprinting();
      var1.setSprinting(false);
      var1.setForceSprint(false);
      if (!var1.isAttackStarted() && !var1.isCurrentState(PlayerHitReactionState.instance())) {
         if (!var1.isCurrentState(FishingState.instance())) {
            if (!GameClient.bClient || !var1.isCurrentState(PlayerHitReactionPVPState.instance()) || ServerOptions.instance.PVPMeleeWhileHitReaction.getValue()) {
               if (!var1.canPerformHandToHandCombat()) {
                  var1.clearHandToHandAttack();
               } else {
                  if (!var1.isAttackStarted()) {
                     var1.setVariable("StartedAttackWhileSprinting", var3);
                  }

                  var1.setInitiateAttack(true);
                  var1.setAttackStarted(true);
                  if (!var2) {
                     var1.setCriticalHit(false);
                  }

                  var1.setAttackFromBehind(false);
                  WeaponType var4 = WeaponType.getWeaponType((IsoGameCharacter)var1);
                  if (!GameClient.bClient || var1.isLocalPlayer()) {
                     var1.setAttackType((String)PZArrayUtil.pickRandom(var4.possibleAttack));
                  }

                  if (!GameClient.bClient || var1.isLocalPlayer()) {
                     var1.setCombatSpeed(var1.calculateCombatSpeed());
                  }

                  this.calculateAttackVars(var1);
                  String var5 = var1.getVariableString("Weapon");
                  if (var5 != null && var5.equals("throwing") && !var1.getAttackVars().bDoShove) {
                     var1.setAttackAnimThrowTimer(2000L);
                     var1.setIsAiming(true);
                  }

                  if (var2) {
                     var1.getAttackVars().bDoShove = var1.isDoShove();
                     var1.getAttackVars().bAimAtFloor = var1.isAimAtFloor();
                     var1.getAttackVars().bDoGrapple = var1.isDoGrapple();
                  }

                  if (var1.getAttackVars().bDoShove && !var1.isAuthorizedHandToHand()) {
                     var1.setDoShove(false);
                     var1.setForceShove(false);
                     var1.setInitiateAttack(false);
                     var1.setAttackStarted(false);
                     var1.setAttackType((String)null);
                  } else if (var1.getAttackVars().bDoGrapple && !var1.isAuthorizedHandToHand()) {
                     var1.setDoGrapple(false);
                     var1.setInitiateAttack(false);
                     var1.setAttackStarted(false);
                     var1.setAttackType((String)null);
                  } else {
                     HandWeapon var6 = var1.getAttackVars().getWeapon(var1);
                     var1.setUseHandWeapon(var6);
                     var1.setAimAtFloor(var1.getAttackVars().bAimAtFloor);
                     var1.setDoShove(var1.getAttackVars().bDoShove);
                     var1.setDoGrapple(var1.getAttackVars().bDoGrapple);
                     var1.targetOnGround = (IsoGameCharacter)var1.getAttackVars().targetOnGround.getMovingObject();
                     if (var6 != null && var4.isRanged) {
                        var1.setRecoilDelay((float)var6.getRecoilDelay(var1));
                     }

                     int var7 = Rand.Next(0, 3);
                     if (var7 == 0) {
                        var1.setAttackVariationX(Rand.Next(-1.0F, -0.5F));
                     }

                     if (var7 == 1) {
                        var1.setAttackVariationX(0.0F);
                     }

                     if (var7 == 2) {
                        var1.setAttackVariationX(Rand.Next(0.5F, 1.0F));
                     }

                     var1.setAttackVariationY(0.0F);
                     this.calculateHitInfoList(var1);
                     IsoGameCharacter var8 = null;
                     if (!var1.getHitInfoList().isEmpty()) {
                        var8 = (IsoGameCharacter)Type.tryCastTo(((HitInfo)var1.getHitInfoList().get(0)).getObject(), IsoGameCharacter.class);
                     }

                     if (var8 == null) {
                        if (var1.isAiming() && !var1.isMeleePressed() && var6 != var1.bareHands) {
                           var1.setDoShove(false);
                           var1.setForceShove(false);
                        }

                        if (var1.isAiming() && !var1.isGrapplePressed() && var6 != var1.bareHands) {
                           var1.setDoGrapple(false);
                        }

                        var1.setLastAttackWasHandToHand(var1.isDoHandToHandAttack());
                        if (var4.canMiss && !var1.isAimAtFloor() && (!GameClient.bClient || var1.isLocalPlayer())) {
                           var1.setAttackType("miss");
                        }

                     } else {
                        if (!GameClient.bClient || var1.isLocalPlayer()) {
                           var1.setAttackFromBehind(var1.isBehind(var8));
                        }

                        float var9 = IsoUtils.DistanceTo(var8.getX(), var8.getY(), var1.getX(), var1.getY());
                        var1.setVariable("TargetDist", var9);
                        int var10 = var1.calculateCritChance(var8);
                        if (var8 instanceof IsoZombie) {
                           IsoZombie var11 = var1.getClosestZombieToOtherZombie((IsoZombie)var8);
                           if (!var1.getAttackVars().bAimAtFloor && var9 > 1.25F && var4 == WeaponType.spear && (var11 == null || IsoUtils.DistanceTo(var8.getX(), var8.getY(), var11.getX(), var11.getY()) > 1.7F)) {
                              if (!GameClient.bClient || var1.isLocalPlayer()) {
                                 var1.setAttackType("overhead");
                              }

                              if (var1.getPrimaryHandItem() == null || var1.getPrimaryHandItem().hasTag("FakeSpear")) {
                                 var10 += 30;
                              }
                           }
                        }

                        if (var1.isLocalPlayer() && !var8.isOnFloor()) {
                           var8.setHitFromBehind(var1.isAttackFromBehind());
                        }

                        if (var1.isAttackFromBehind()) {
                           if (!(var8 instanceof IsoZombie) || ((IsoZombie)var8).target != null || var1.getPrimaryHandItem() != null && !var1.getPrimaryHandItem().hasTag("FakeSpear")) {
                              var10 += 5;
                           } else {
                              var10 += 30;
                           }
                        }

                        if (var8 instanceof IsoPlayer && var4.isRanged && !var1.isDoHandToHandAttack()) {
                           var10 = (int)(var1.getAttackVars().getWeapon(var1).getStopPower() * (1.0F + (float)var1.getPerkLevel(PerkFactory.Perks.Aiming) / 15.0F));
                        }

                        if (var1.getPrimaryHandItem() != null && var1.getPrimaryHandItem().hasTag("NoCriticals")) {
                           var10 = 0;
                        }

                        if (!GameClient.bClient || var1.isLocalPlayer()) {
                           var1.setCriticalHit(Rand.Next(100) < var10);
                           if (var1.isAttackFromBehind() && var1.getAttackVars().bCloseKill && var8 instanceof IsoZombie && ((IsoZombie)var8).target == null && var1.getPrimaryHandItem() != null && !var1.getPrimaryHandItem().hasTag("FakeSpear")) {
                              var1.setCriticalHit(true);
                           }

                           if (var1.isCriticalHit() && !var1.getAttackVars().bCloseKill && !var1.isDoShove() && var4 == WeaponType.knife) {
                              var1.setCriticalHit(false);
                           }

                           var1.setAttackWasSuperAttack(false);
                           if (var1.getStats().NumChasingZombies > 1 && var1.getAttackVars().bCloseKill && !var1.isDoShove() && var4 == WeaponType.knife) {
                              var1.setCriticalHit(false);
                           }
                        }

                        if (var1.getPrimaryHandItem() != null && var1.getPrimaryHandItem().hasTag("NoCriticals")) {
                           var1.setCriticalHit(false);
                        }

                        if (var1.isCriticalHit()) {
                           var1.setCombatSpeed(var1.getCombatSpeed() * 1.1F);
                        }

                        if (Core.bDebug) {
                           DebugLog.Combat.debugln("Attacked zombie: dist: " + var9 + ", chance: (" + ((HitInfo)var1.getHitInfoList().get(0)).chance + "), crit: " + var1.isCriticalHit() + " (" + var10 + "%) from behind: " + var1.isAttackFromBehind());
                        }

                        var1.setLastAttackWasHandToHand(var1.isDoHandToHandAttack());
                     }
                  }
               }
            }
         }
      }
   }

   private void processTargetedHit(HandWeapon var1, IsoGameCharacter var2, IsoZombie var3, RagdollBodyPart var4) {
      String var5 = "ShotBelly";
      Vector2 var6 = var2.getForwardDirection();
      Vector2 var7 = var3.getHitAngle();
      double var8 = (double)(var6.x * var7.y - var6.y * var7.x);
      double var10 = var8 >= 0.0 ? 1.0 : -1.0;
      double var12 = (double)(var6.x * var7.x + var6.y * var7.y);
      double var14 = Math.acos(var12) * var10;
      String var16 = this.calculateShotDirection(var3, var14);
      int var17 = Rand.Next(2);
      switch (var4) {
         case BODYPART_PELVIS:
            var5 = "ShotBellyStep";
            if ("N".equals(var16)) {
               if (var3.isHitFromBehind()) {
                  var5 = "ShotBellyStepBehind";
               } else {
                  switch (var17) {
                     case 0:
                        var5 = "ShotBelly";
                  }
               }
            }
            break;
         case BODYPART_SPINE:
            var5 = "ShotChest";
            if ("L".equals(var16)) {
               var5 = var17 == 0 ? "ShotChestL" : "ShotChestStepL";
            } else if ("R".equals(var16)) {
               var5 = var17 == 0 ? "ShotChestR" : "ShotChestStepR";
            }
            break;
         case BODYPART_HEAD:
            var5 = var17 == 0 ? "ShotHeadFwd" : "ShotHeadFwd02";
            if (("L".equals(var16) || "R".equals(var16)) && Rand.Next(4) == 0) {
               var5 = "ShotHeadBwd";
            }
            break;
         case BODYPART_LEFT_UPPER_LEG:
         case BODYPART_LEFT_LOWER_LEG:
            var5 = "ShotLegL";
            break;
         case BODYPART_RIGHT_UPPER_LEG:
         case BODYPART_RIGHT_LOWER_LEG:
            var5 = "ShotLegR";
            break;
         case BODYPART_LEFT_UPPER_ARM:
         case BODYPART_LEFT_LOWER_ARM:
            var5 = "ShotShoulderL";
            if (var3.isHitFromBehind()) {
               var5 = "ShotShoulderStepL";
            }
            break;
         case BODYPART_RIGHT_UPPER_ARM:
         case BODYPART_RIGHT_LOWER_ARM:
            var5 = "ShotShoulderR";
            if (var3.isHitFromBehind()) {
               var5 = "ShotShoulderStepR";
            }
            break;
         default:
            throw new IllegalStateException("Unexpected value: " + var4);
      }

      this.applyBlood(var1, var3, var5, var16);
      if (var3.getEatBodyTarget() != null) {
         if (var3.getVariableBoolean("onknees")) {
            var5 = "OnKnees";
         } else {
            var5 = "Eating";
         }
      }

      if ("Floor".equalsIgnoreCase(var5) && var3.isCurrentState(ZombieGetUpState.instance()) && var3.isFallOnFront()) {
         var5 = "GettingUpFront";
      }

      if (var5 != null && !"".equals(var5)) {
         var3.setHitReaction(var5);
      } else {
         var3.setStaggerBack(true);
         var3.setHitReaction("");
         if ("LEFT".equals(var3.getPlayerAttackPosition()) || "RIGHT".equals(var3.getPlayerAttackPosition())) {
            var2.setCriticalHit(false);
         }
      }

      var3.setUsePhysicHitReaction(true);
      this.createCombatData(var1, var2, var3, var4);
      DebugLog.Combat.println("hitReaction = %s", var5);
   }

   private void createCombatData(HandWeapon var1, IsoGameCharacter var2, IsoZombie var3, RagdollBodyPart var4) {
      BallisticsTarget var5 = var3.getBallisticsTarget();
      if (var5 != null) {
         BallisticsTarget.CombatDamageData var6 = var5.getCombatDamageData();
         var6.event = var3.getHitReaction();
         var6.target = var3;
         var6.attacker = var2;
         var6.handWeapon = var1;
         var6.bodyPart = var4;
         var5.setCombatDamageDataProcessed(false);
      }

   }

   public void update(boolean var1) {
      if (IsoPlayer.players[0] != null) {
         this.updateReticle(IsoPlayer.players[0]);
         this.highlightTargets(IsoPlayer.players[0]);
      }

   }

   public void postUpdate(boolean var1) {
   }

   public void updateReticle(IsoPlayer var1) {
      if (targetReticleMode != 0) {
         if (var1.isLocalPlayer() && !var1.isNPC()) {
            if (var1.isAiming()) {
               HandWeapon var2 = (HandWeapon)Type.tryCastTo(var1.getPrimaryHandItem(), HandWeapon.class);
               if (var2 == null || var2.getSwingAnim() == null || var2.getCondition() <= 0) {
                  var2 = var1.bareHands;
               }

               if (var2.isRanged()) {
                  boolean var3 = var1.isDoShove();
                  boolean var4 = var1.isDoGrapple();
                  HandWeapon var5 = var1.getUseHandWeapon();
                  var1.setDoShove(false);
                  var1.setDoGrapple(false);
                  var1.setUseHandWeapon(var2);
                  this.calculateAttackVars(var1);
                  this.calculateHitInfoList(var1);
                  if (Core.bDebug) {
                     this.updateTargetHitInfoPanel(var1);
                  }

                  ColorInfo var6 = IsoPlayer.getInf();
                  HitChanceData var7 = this.calculateHitChanceData(var1, var2, (HitInfo)null);
                  IsoReticle.getInstance().setChance((int)var7.hitChance);
                  IsoReticle.getInstance().setAimPenalty((int)var7.aimPenalty);
                  IsoReticle.getInstance().hasTarget(!var1.getHitInfoList().isEmpty());
                  IsoReticle.getInstance().setReticleColor(Core.getInstance().getNoTargetColor());
                  float var8 = 999.9F;

                  for(int var9 = 0; var9 < var1.getHitInfoList().size(); ++var9) {
                     HitInfo var10 = (HitInfo)var1.getHitInfoList().get(var9);
                     IsoMovingObject var11 = var10.getObject();
                     if (var10.distSq < var8) {
                        if (var11 instanceof IsoZombie || var11 instanceof IsoPlayer) {
                           float var12 = (float)var10.chance < 70.0F ? (float)var10.chance / 140.0F : ((float)var10.chance - 70.0F) / 30.0F * 0.5F + 0.5F;
                           Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), var12, var6);
                           var8 = var10.distSq;
                           IsoReticle.getInstance().setChance(var10.chance);
                           IsoReticle.getInstance().setReticleColor(Core.getInstance().getTargetColor());
                        }

                        if (var10.window.getObject() != null) {
                           var10.window.getObject().setHighlightColor(0.8F, 0.1F, 0.1F, 0.5F);
                           var10.window.getObject().setHighlighted(true);
                        }
                     }
                  }

                  IsoReticle.getInstance().setAimColor(var6);
                  var1.setDoShove(var3);
                  var1.setDoGrapple(var4);
                  var1.setUseHandWeapon(var5);
               }
            }
         }
      }
   }

   private void updateTargetHitInfoPanel(IsoGameCharacter var1) {
      Iterator var2 = DebugContext.instance.getWindows().iterator();

      while(var2.hasNext()) {
         BaseDebugWindow var3 = (BaseDebugWindow)var2.next();
         if (var3 instanceof TargetHitInfoPanel var4) {
            var4.setIsoGameCharacter(var1);
            var4.hitInfoList.clear();
            Iterator var5 = var1.getHitInfoList().iterator();

            while(var5.hasNext()) {
               HitInfo var6 = (HitInfo)var5.next();
               var4.hitInfoList.add(var6);
            }

            return;
         }
      }

   }

   private void calculateDirectionVector(float var1) {
      ballisticsDirectionVector.x = (float)Math.cos((double)var1);
      ballisticsDirectionVector.y = (float)Math.sin((double)var1);
      ballisticsDirectionVector.z = 0.0F;
      ballisticsDirectionVector.normalize(1.0F);
   }

   private void fireWeapon(HandWeapon var1, IsoGameCharacter var2) {
      if (var1 != null) {
         float var3 = var1.getMaxRange(var2);
         BallisticsController var4 = var2.getBallisticsController();
         this.calculateDirectionVector(var2.getLookAngleRadians());
         Vector3 var5 = var4.getMuzzlePosition();
         float var6 = var5.x + ballisticsDirectionVector.x * var3;
         float var7 = var5.y + ballisticsDirectionVector.y * var3;
         float var8 = var5.z + ballisticsDirectionVector.z * var3;
         if (var1.isRangeFalloff()) {
            boolean var9 = this.isVehicleHit(var2.getHitInfoList());
            int var10;
            if (!var4.hasSpreadData()) {
               var4.setRange(var3);
               var4.update();
               var10 = var1.getProjectileCount();
               float var11 = var1.getProjectileSpread();
               float var12 = var1.getProjectileWeightCenter();
               var4.getSpreadData(var3, var11, var12, var10);
            }

            var10 = var4.getNumberOfSpreadData();
            float[] var25 = var4.getBallisticsSpreadData();
            int var27 = 0;

            for(int var13 = 0; var13 < var10; ++var13) {
               zombie.core.skinnedmodel.Vector3 var14 = new zombie.core.skinnedmodel.Vector3();
               zombie.core.skinnedmodel.Vector3 var15 = new zombie.core.skinnedmodel.Vector3();
               float var16 = var25[var27++];
               float var17 = var25[var27++];
               float var18 = var25[var27++] / 2.46F;
               float var19 = var25[var27++];
               var14.set(var5.x, var5.y, var5.z);
               var15.set(var17, var19, var18);
               if (var16 == 0.0F) {
                  Vector3 var21;
                  if (var9) {
                     zombie.core.skinnedmodel.Vector3 var20 = this.checkHitVehicle(var2.getHitInfoList(), var14, var15);
                     if (var20 != null) {
                        var21 = PZMath.closestVector3(var14.x(), var14.y(), var14.z(), var15.x(), var15.y(), var15.z(), var20.x(), var20.y(), var15.z());
                        IsoBulletTracerEffects.getInstance().addEffect(var2, var3, var21.x, var21.y, var21.z);
                        continue;
                     }
                  }

                  IsoGridSquareCollisionData var28 = LosUtil.getFirstBlockingIsoGridSquare(var2.getCell(), (int)var5.x, (int)var5.y, (int)var5.z, (int)var17, (int)var19, (int)var18, false);
                  if (var28.testResults != LosUtil.TestResults.Clear && var28.testResults != LosUtil.TestResults.ClearThroughClosedDoor) {
                     var21 = PZMath.closestVector3(var5.x, var5.y, var5.z, var17, var19, var18, var28.hitPosition.x, var28.hitPosition.y, var18);
                     IsoBulletTracerEffects.getInstance().addEffect(var2, var3, var21.x, var21.y, var21.z, var28.isoGridSquare);
                  } else {
                     IsoBulletTracerEffects.getInstance().addEffect(var2, var3, var17, var19, var18);
                  }
               } else {
                  IsoBulletTracerEffects.getInstance().addEffect(var2, var3, var17, var19, var18);
               }
            }
         } else {
            ArrayList var22 = var2.getHitInfoList();
            Vector3 var26;
            if (var22.isEmpty()) {
               IsoGridSquareCollisionData var23 = LosUtil.getFirstBlockingIsoGridSquare(var2.getCell(), (int)var5.x, (int)var5.y, (int)var5.z, (int)var6, (int)var7, (int)var8, false);
               if (var23.testResults != LosUtil.TestResults.Clear && var23.testResults != LosUtil.TestResults.ClearThroughClosedDoor) {
                  var26 = PZMath.closestVector3(var5.x, var5.y, var5.z, var6, var7, var8, var23.hitPosition.x, var23.hitPosition.y, var8);
                  IsoBulletTracerEffects.getInstance().addEffect(var2, var3, var26.x, var26.y, var26.z, var23.isoGridSquare);
               } else {
                  IsoBulletTracerEffects.getInstance().addEffect(var2, var3);
               }
            } else {
               assert var22.size() == 1;

               HitInfo var24 = (HitInfo)var22.get(0);
               var26 = PZMath.closestVector3(var5.x, var5.y, var5.z, var6, var7, var8, var24.x, var24.y, var24.z);
               IsoBulletTracerEffects.getInstance().addEffect(var2, var3, var26.x, var26.y, var26.z);
            }
         }

      }
   }

   private boolean isVehicleHit(ArrayList<HitInfo> var1) {
      for(int var2 = 0; var2 < var1.size(); ++var2) {
         IsoMovingObject var3 = ((HitInfo)var1.get(var2)).getObject();
         if (var3 != null) {
            BaseVehicle var4 = (BaseVehicle)Type.tryCastTo(var3, BaseVehicle.class);
            if (var4 != null) {
               return true;
            }
         }
      }

      return false;
   }

   private zombie.core.skinnedmodel.Vector3 checkHitVehicle(ArrayList<HitInfo> var1, zombie.core.skinnedmodel.Vector3 var2, zombie.core.skinnedmodel.Vector3 var3) {
      zombie.core.skinnedmodel.Vector3 var4 = null;

      for(int var5 = 0; var5 < var1.size(); ++var5) {
         IsoMovingObject var6 = ((HitInfo)var1.get(var5)).getObject();
         BaseVehicle var7 = (BaseVehicle)Type.tryCastTo(var6, BaseVehicle.class);
         if (var7 != null) {
            var4 = var7.getIntersectPoint(var2, var3);
            if (var4 != null) {
               return var4;
            }
         }
      }

      return var4;
   }

   public boolean hitIsoGridSquare(IsoGridSquare var1, Vector3f var2) {
      return this.shootPlacedItems(var1, new Vector3(var2.x(), var2.y(), var2.z()));
   }

   private boolean shootPlacedItems(IsoGridSquare var1, Vector3 var2) {
      float var3 = 999.9F;
      IsoWorldInventoryObject var4 = null;
      IsoObject[] var5 = (IsoObject[])var1.getObjects().getElements();
      int var6 = var1.getObjects().size();

      for(int var7 = 0; var7 < var6; ++var7) {
         IsoObject var8 = var5[var7];
         IsoWorldInventoryObject var9 = (IsoWorldInventoryObject)Type.tryCastTo(var8, IsoWorldInventoryObject.class);
         IsoLightSwitch var10 = (IsoLightSwitch)Type.tryCastTo(var8, IsoLightSwitch.class);
         if (var9 != null || var10 != null) {
            if (var10 != null && var10.hasLightBulb()) {
               SoundManager.instance.playImpactSound(var1, MaterialType.Glass_Solid);
               SoundManager.instance.PlayWorldSound("SmashWindow", var1, 0.2F, 20.0F, 1.0F, true);
               var10.setBulbItemRaw((String)null);
            }

            if (var9 != null && var9.name != null && (var9.name.contains("Bottle") || var9.name.contains("Can"))) {
               Vector3 var11 = new Vector3(var9.getWorldPosX(), var9.getWorldPosY(), var9.getWorldPosZ());
               float var12 = var2.distanceTo(var11);
               if (var12 < var3) {
                  var3 = var12;
                  var4 = var9;
               }
            }
         }
      }

      if (var4 != null) {
         SoundManager.instance.playImpactSound(var1, MaterialType.Glass_Light);
         var1.transmitRemoveItemFromSquare(var4);
         return true;
      } else {
         return false;
      }
   }

   static {
      OccludedTargetDebugColor = Color.white;
      TargetableDebugColor = Color.green;
   }

   private static final class WindowVisitor implements LOSVisitor {
      LosUtil.TestResults test;
      IsoWindow window;

      private WindowVisitor() {
      }

      void init() {
         this.test = LosUtil.TestResults.Clear;
         this.window = null;
      }

      public boolean visit(IsoGridSquare var1, IsoGridSquare var2) {
         if (var1 != null && var2 != null) {
            boolean var3 = true;
            boolean var4 = false;
            LosUtil.TestResults var5 = var1.testVisionAdjacent(var2.getX() - var1.getX(), var2.getY() - var1.getY(), var2.getZ() - var1.getZ(), var3, var4);
            if (var5 == LosUtil.TestResults.ClearThroughWindow) {
               IsoWindow var6 = var1.getWindowTo(var2);
               if (this.isHittable(var6) && var6.TestVision(var1, var2) == IsoObject.VisionResult.Unblocked) {
                  this.window = var6;
                  return true;
               }
            }

            if (var5 == LosUtil.TestResults.Blocked || this.test == LosUtil.TestResults.Clear || var5 == LosUtil.TestResults.ClearThroughWindow && this.test == LosUtil.TestResults.ClearThroughOpenDoor) {
               this.test = var5;
            } else if (var5 == LosUtil.TestResults.ClearThroughClosedDoor && this.test == LosUtil.TestResults.ClearThroughOpenDoor) {
               this.test = var5;
            }

            return this.test == LosUtil.TestResults.Blocked;
         } else {
            return false;
         }
      }

      public LosUtil.TestResults getResult() {
         return this.test;
      }

      boolean isHittable(IsoWindow var1) {
         if (var1 == null) {
            return false;
         } else if (var1.isBarricaded()) {
            return true;
         } else {
            return !var1.isDestroyed() && !var1.IsOpen();
         }
      }
   }

   private static final class CalcHitListGrappleReusables {
      static final ArrayList<IsoMovingObject> foundObjects = new ArrayList();
      static final Vector4f posAndDot = new Vector4f();

      private CalcHitListGrappleReusables() {
      }
   }

   private static class HitChanceData {
      public float hitChance = 0.0F;
      public float aimPenalty = 0.0F;

      public HitChanceData() {
      }
   }

   private interface LOSVisitor {
      boolean visit(IsoGridSquare var1, IsoGridSquare var2);

      LosUtil.TestResults getResult();
   }

   private static class s_performance {
      static final PerformanceProfileProbe highlightRangedTargets = new PerformanceProfileProbe("CombatManager.highlightRangedTargets");

      private s_performance() {
      }
   }
}
