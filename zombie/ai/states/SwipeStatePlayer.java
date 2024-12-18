package zombie.ai.states;

import java.util.HashMap;
import java.util.Iterator;
import se.krka.kahlua.vm.KahluaTable;
import zombie.CombatManager;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaHookManager;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoLivingCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableReference;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventBroadcaster;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.fields.AttackVars;
import zombie.network.fields.HitInfo;
import zombie.ui.UIManager;
import zombie.util.StringUtils;
import zombie.util.Type;

public final class SwipeStatePlayer extends State {
   private static final float ShoveChargeDeltaMultiplier = 2.0F;
   private static final float MaxStartChargeDelta = 90.0F;
   private static final float ChargeDeltaModifier = 25.0F;
   private static final float ShoveRecoilDelay = 10.0F;
   private static final float WeaponEmptyRecoilDelay = 10.0F;
   private static final int BaseAimingDelay = 10;
   private static final float DefaultChargeDelta = 2.0F;
   private static final float BreakMultiplierBase = 1.0F;
   private static final float BreakMultiplerChargeModifier = 1.5F;
   private static final float DefaultMaintenanceXP = 1.0F;
   private static final int ConditionLowerChance = 10;
   private static final int FootDamageBaseRange = 10;
   private static final int NoShoesFootDamageBaseRange = 3;
   private static final SwipeStatePlayer _instance = new SwipeStatePlayer();
   private static final Integer PARAM_LOWER_CONDITION = 0;
   private static final Integer PARAM_ATTACKED = 1;
   private static AnimEventBroadcaster s_dbgGlobalEventBroadcaster = null;

   public static SwipeStatePlayer instance() {
      return _instance;
   }

   public SwipeStatePlayer() {
      this.addAnimEventListener("ActiveAnimFinishing", this::OnAnimEvent_ActiveAnimFinishing);
      this.addAnimEventListener("NonLoopedAnimFadeOut", this::OnAnimEvent_ActiveAnimFinishing);
      this.addAnimEventListener("AttackAnim", this::OnAnimEvent_AttackAnim);
      this.addAnimEventListener("BlockTurn", this::OnAnimEvent_BlockTurn);
      this.addAnimEventListener("ShoveAnim", this::OnAnimEvent_ShoveAnim);
      this.addAnimEventListener("StompAnim", this::OnAnimEvent_StompAnim);
      this.addAnimEventListener("GrappleGrabAnim", this::OnAnimEvent_GrappleGrabAnim);
      this.addAnimEventListener("AttackCollisionCheck", this::OnAnimEvent_AttackCollisionCheck);
      this.addAnimEventListener("GrappleGrabCollisionCheck", this::OnAnimEvent_GrappleGrabCollisionCheck);
      this.addAnimEventListener("BlockMovement", this::OnAnimEvent_BlockMovement);
      this.addAnimEventListener("WeaponEmptyCheck", this::OnAnimEvent_WeaponEmptyCheck);
      this.addAnimEventListener("ShotDone", this::OnAnimEvent_ShotDone);
      this.addAnimEventListener(this::OnAnimEvent_SetVariable);
      this.addAnimEventListener("SetMeleeDelay", this::OnAnimEvent_SetMeleeDelay);
      this.addAnimEventListener("playRackSound", SwipeStatePlayer::OnAnimEvent_PlayRackSound);
      this.addAnimEventListener("playClickSound", SwipeStatePlayer::OnAnimEvent_PlayClickSound);
      this.addAnimEventListener("PlaySwingSound", this::OnAnimEvent_PlaySwingSound);
      this.addAnimEventListener("PlayerVoiceSound", this::OnAnimEvent_PlayerVoiceSound);
      this.addAnimEventListener("SitGroundStarted", this::OnAnimEvent_SitGroundStarted);
   }

   public static void dbgOnGlobalAnimEvent(IsoGameCharacter var0, AnimEvent var1) {
      if (Core.bDebug) {
         if (!(var0.getCurrentState() instanceof SwipeStatePlayer)) {
            if (s_dbgGlobalEventBroadcaster == null) {
               s_dbgGlobalEventBroadcaster = new AnimEventBroadcaster();
               s_dbgGlobalEventBroadcaster.addListener("playRackSound", SwipeStatePlayer::OnAnimEvent_PlayRackSound);
               s_dbgGlobalEventBroadcaster.addListener("playClickSound", SwipeStatePlayer::OnAnimEvent_PlayClickSound);
               s_dbgGlobalEventBroadcaster.addListener("PlaySwingSound", SwipeStatePlayer::OnAnimEvent_PlaySwingSoundAlways);
               s_dbgGlobalEventBroadcaster.addListener("PlayerVoiceSound", SwipeStatePlayer::OnAnimEvent_PlayerVoiceSoundAlways);
            }

            DebugLog.Animation.trace("Received anim event: %s", var1);
            s_dbgGlobalEventBroadcaster.animEvent(var0, var1);
         }
      }
   }

   private static void WeaponLowerConditionEvent(HandWeapon var0, IsoGameCharacter var1) {
      if (var0.getCondition() <= 0) {
         IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
         if (var2 != null && (!var2.getAttackVars().targetsStanding.isEmpty() || !var2.getAttackVars().targetsProne.isEmpty())) {
            var2.triggerMusicIntensityEvent("WeaponBreaksDuringCombat");
         }
      }

   }

   private void doAttack(IsoPlayer var1, float var2, boolean var3, String var4, AttackVars var5) {
      var1.setForceShove(var3);
      var1.setClickSound(var4);
      if (var3) {
         var2 *= 2.0F;
      }

      if (var2 > 90.0F) {
         var2 = 90.0F;
      }

      var2 /= 25.0F;
      var1.useChargeDelta = var2;
      Object var6 = var1.getPrimaryHandItem();
      if (var6 == null || !(var6 instanceof HandWeapon) || var3 || var5.bDoShove || var5.bDoGrapple) {
         var6 = var1.bareHands;
      }

      if (var6 instanceof HandWeapon) {
         var1.setUseHandWeapon((HandWeapon)var6);
         if (var1.PlayerIndex == 0 && var1.JoypadBind == -1 && UIManager.getPicked() != null && (!GameClient.bClient || var1.isLocalPlayer())) {
            if (UIManager.getPicked().tile instanceof IsoMovingObject) {
               var1.setAttackTargetSquare(((IsoMovingObject)UIManager.getPicked().tile).getCurrentSquare());
            } else {
               var1.setAttackTargetSquare(UIManager.getPicked().square);
            }
         }

         if (var3) {
            var1.setRecoilDelay(10.0F);
            var1.setAimingDelay((float)(((HandWeapon)var6).getAimingTime() + 10));
         } else {
            var1.setRecoilDelay((float)var5.recoilDelay);
            var1.setAimingDelay(var1.getAimingDelay() + (float)((HandWeapon)var6).getRecoilDelay(var1) * CombatManager.PostShotAimingDelayRecoilModifier + (float)((HandWeapon)var6).getAimingTime() * CombatManager.PostShotAimingDelayAimingModifier);
         }
      }

   }

   public void enter(IsoGameCharacter var1) {
      IsoPlayer var2 = (IsoPlayer)var1;
      if ("HitReaction".equals(var2.getHitReaction())) {
         var2.clearVariable("HitReaction");
      }

      UIManager.speedControls.SetCurrentGameSpeed(1);
      HashMap var3 = var2.getStateMachineParams(this);
      var3.put(PARAM_LOWER_CONDITION, Boolean.FALSE);
      var3.put(PARAM_ATTACKED, Boolean.FALSE);
      CombatManager.getInstance().calculateAttackVars(var2);
      this.doAttack(var2, 2.0F, var2.isForceShove(), var2.getClickSound(), var2.getAttackVars());
      HandWeapon var4 = var2.getUseHandWeapon();
      if (var4 != null) {
         if (!var2.bRemote) {
            var2.setRecoilVarY(0.0F);
            float var5 = (float)var4.getRecoilDelay();
            float var6 = (float)var4.getRecoilDelay(var2);
            if (var6 < var5 && var5 != 0.0F) {
               float var7 = 1.0F - var6 / var5;
               var2.setRecoilVarX(var7);
            } else {
               var2.setRecoilVarX(0.0F);
            }
         }

         if ("Auto".equals(var2.getFireMode())) {
            var2.setVariable("autoShootSpeed", 8.0F * GameTime.getAnimSpeedFix());
            var2.setVariable("autoShootVarY", 0.0F);
            var2.setVariable("autoShootVarX", 1.0F);
         } else {
            var1.setVariable("singleShootSpeed", PZMath.max(0.5F, 0.8F + (1.0F - (float)var4.getRecoilDelay(var1) / (float)var4.getRecoilDelay())) * GameTime.getAnimSpeedFix());
         }
      }

      var2.setVariable("ShotDone", false);
      var2.setPerformingShoveAnimation(false);
      var2.setPerformingGrappleGrabAnimation(false);
      if (!GameClient.bClient || var2.isLocalPlayer()) {
         var2.setVariable("AimFloorAnim", var2.getAttackVars().bAimAtFloor);
      }

      LuaEventManager.triggerEvent("OnWeaponSwing", var2, var4);
      if (LuaHookManager.TriggerHook("WeaponSwing", var2, var4)) {
         var2.getStateMachine().revertToPreviousState(this);
      }

      var2.StopAllActionQueue();
      if (var2.isLocalPlayer()) {
         IsoWorld.instance.CurrentCell.setDrag((KahluaTable)null, var2.PlayerIndex);
      }

      var4 = var2.getAttackVars().getWeapon(var2);
      var2.setAimAtFloor(var2.getAttackVars().bAimAtFloor);
      boolean var8 = var2.isDoShove();
      var2.setDoShove(var2.getAttackVars().bDoShove);
      var2.setPerformingGrappleGrabAnimation(var2.getAttackVars().bDoGrapple);
      var2.useChargeDelta = var2.getAttackVars().useChargeDelta;
      var2.targetOnGround = (IsoGameCharacter)var2.getAttackVars().targetOnGround.getMovingObject();
      if (GameClient.bClient && var2 == IsoPlayer.getInstance()) {
         GameClient.instance.sendPlayer(var2);
      }

      if (!var2.isDoShove() && !var8 && !var4.isRanged() && var2.isLocalPlayer()) {
         var2.clearVariable("PlayedSwingSound");
      } else if ((var2.isDoShove() || var8) && var2.isLocalPlayer() && !var2.isGrappling()) {
         if (var2.targetOnGround != null) {
            var2.playSound("AttackStomp");
         } else {
            var2.playSound("AttackShove");
         }
      }

   }

   public void execute(IsoGameCharacter var1) {
      var1.StopAllActionQueue();
   }

   private void OnAnimEvent_ActiveAnimFinishing(IsoGameCharacter var1) {
      HashMap var2 = var1.getStateMachineParams(this);
      IsoPlayer var3 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      boolean var4 = var2.get(PARAM_LOWER_CONDITION) == Boolean.TRUE;
      if (var4) {
         var2.put(PARAM_LOWER_CONDITION, Boolean.FALSE);
         HandWeapon var5 = CombatManager.getInstance().getWeapon(var1);
         WeaponLowerConditionEvent(var5, var1);
      }

   }

   private void OnAnimEvent_AttackAnim(IsoGameCharacter var1, boolean var2) {
      var1.setPerformingAttackAnimation(var2);
   }

   private void OnAnimEvent_BlockTurn(IsoGameCharacter var1, boolean var2) {
      var1.setIgnoreMovement(var2);
   }

   private void OnAnimEvent_ShoveAnim(IsoGameCharacter var1, boolean var2) {
      var1.setPerformingShoveAnimation(var2);
   }

   private void OnAnimEvent_StompAnim(IsoGameCharacter var1, boolean var2) {
      var1.setPerformingStompAnimation(var2);
   }

   private void OnAnimEvent_GrappleGrabAnim(IsoGameCharacter var1, boolean var2) {
      var1.setPerformingGrappleGrabAnimation(var2);
   }

   private void OnAnimEvent_AttackCollisionCheck(IsoGameCharacter var1) {
      HashMap var2 = var1.getStateMachineParams(this);
      HandWeapon var3 = CombatManager.getInstance().getWeapon(var1);
      if (var2.get(PARAM_ATTACKED) == Boolean.FALSE && IsoPlayer.isLocalPlayer(var1)) {
         CombatManager.getInstance().attackCollisionCheck(var1, var3, this);
      }

   }

   private void OnAnimEvent_GrappleGrabCollisionCheck(IsoGameCharacter var1, String var2) {
      if (IsoPlayer.isLocalPlayer(var1)) {
         HashMap var3 = var1.getStateMachineParams(this);
         HandWeapon var4 = CombatManager.getInstance().getWeapon(var1);
         if (var3.get(PARAM_ATTACKED) == Boolean.FALSE) {
            this.GrappleGrabCollisionCheck(var1, var4, var2);
         }
      }
   }

   private void OnAnimEvent_BlockMovement(IsoGameCharacter var1, AnimEvent var2) {
      if (SandboxOptions.instance.AttackBlockMovements.getValue()) {
         var1.setVariable("SlowingMovement", Boolean.parseBoolean(var2.m_ParameterValue));
      }
   }

   private void OnAnimEvent_WeaponEmptyCheck(IsoGameCharacter var1) {
      if (var1.getClickSound() != null) {
         if (!IsoPlayer.isLocalPlayer(var1)) {
            return;
         }

         var1.playSound(var1.getClickSound());
         var1.setRecoilDelay(10.0F);
      }

   }

   private void OnAnimEvent_ShotDone(IsoGameCharacter var1) {
      HandWeapon var2 = CombatManager.getInstance().getWeapon(var1);
      if (var2 != null && var2.isRackAfterShoot()) {
         var1.setVariable("ShotDone", true);
      }

   }

   private void OnAnimEvent_SetVariable(IsoGameCharacter var1, AnimationVariableReference var2, String var3) {
      if ("ShotDone".equalsIgnoreCase(var2.getName())) {
         HandWeapon var4 = CombatManager.getInstance().getWeapon(var1);
         var1.setVariable("ShotDone", var1.getVariableBoolean("ShotDone") && var4 != null && var4.isRackAfterShoot());
      }

   }

   private static void OnAnimEvent_PlayRackSound(IsoGameCharacter var0) {
      HandWeapon var1 = CombatManager.getInstance().getWeapon(var0);
      if (IsoPlayer.isLocalPlayer(var0)) {
         var0.playSound(var1.getRackSound());
      }
   }

   private static void OnAnimEvent_PlayClickSound(IsoGameCharacter var0) {
      HandWeapon var1 = CombatManager.getInstance().getWeapon(var0);
      if (IsoPlayer.isLocalPlayer(var0)) {
         var0.playSound(var1.getClickSound());
         checkRangedWeaponFailedToShoot(var0);
      }
   }

   private void OnAnimEvent_PlaySwingSound(IsoGameCharacter var1, String var2) {
      if (IsoPlayer.isLocalPlayer(var1)) {
         if (!var1.getVariableBoolean("PlayedSwingSound")) {
            var1.setVariable("PlayedSwingSound", true);
            OnAnimEvent_PlaySwingSoundAlways(var1, var2);
         }
      }
   }

   private static void OnAnimEvent_PlaySwingSoundAlways(IsoGameCharacter var0, String var1) {
      if (IsoPlayer.isLocalPlayer(var0)) {
         HandWeapon var2 = CombatManager.getInstance().getWeapon(var0);
         if (var2 != null) {
            if (!StringUtils.isNullOrWhitespace(var1)) {
               String var3 = var2.getSoundByID(var1);
               if (var3 != null) {
                  var0.playSound(var3);
                  return;
               }
            }

            var0.playSound(var2.getSwingSound());
         }
      }
   }

   private void OnAnimEvent_PlayerVoiceSound(IsoGameCharacter var1, String var2) {
      if (!var1.getVariableBoolean("PlayerVoiceSound")) {
         var1.setVariable("PlayerVoiceSound", true);
         OnAnimEvent_PlayerVoiceSoundAlways(var1, var2);
      }
   }

   private static void OnAnimEvent_PlayerVoiceSoundAlways(IsoGameCharacter var0, String var1) {
      IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(var0, IsoPlayer.class);
      if (var2 != null) {
         var2.stopPlayerVoiceSound(var1);
         var2.playerVoiceSound(var1);
      }
   }

   private void OnAnimEvent_SetMeleeDelay(IsoGameCharacter var1, float var2) {
      var1.setMeleeDelay(var2);
   }

   private void OnAnimEvent_SitGroundStarted(IsoGameCharacter var1) {
      var1.setVariable("SitGroundAnim", "Idle");
   }

   public void exit(IsoGameCharacter var1) {
      HashMap var2 = var1.getStateMachineParams(this);
      var1.setSprinting(false);
      ((IsoPlayer)var1).setForceSprint(false);
      var1.setIgnoreMovement(false);
      var1.setPerformingShoveAnimation(false);
      var1.setPerformingStompAnimation(false);
      var1.setPerformingGrappleGrabAnimation(false);
      var1.setPerformingAttackAnimation(false);
      var1.setVariable("AimFloorAnim", false);
      ((IsoPlayer)var1).setBlockMovement(false);
      if (var1.isAimAtFloor() && ((IsoLivingCharacter)var1).isDoShove()) {
         Clothing var3 = (Clothing)var1.getWornItem("Shoes");
         int var4 = 10;
         if (var3 == null) {
            var4 = 3;
         } else {
            var4 += var3.getConditionLowerChance() / 2;
            if (Rand.Next(var3.getConditionLowerChance()) == 0) {
               var3.setCondition(var3.getCondition() - 1);
            }
         }

         if (Rand.Next(var4) == 0) {
            if (var3 == null) {
               var1.getBodyDamage().getBodyPart(BodyPartType.Foot_R).AddDamage((float)Rand.Next(5, 10));
               var1.getBodyDamage().getBodyPart(BodyPartType.Foot_R).setAdditionalPain(var1.getBodyDamage().getBodyPart(BodyPartType.Foot_R).getAdditionalPain() + (float)Rand.Next(5, 10));
            } else {
               var1.getBodyDamage().getBodyPart(BodyPartType.Foot_R).AddDamage((float)Rand.Next(1, 5));
               var1.getBodyDamage().getBodyPart(BodyPartType.Foot_R).setAdditionalPain(var1.getBodyDamage().getBodyPart(BodyPartType.Foot_R).getAdditionalPain() + (float)Rand.Next(1, 5));
            }
         }
      }

      HandWeapon var5 = CombatManager.getInstance().getWeapon(var1);
      var1.clearVariable("ZombieHitReaction");
      ((IsoPlayer)var1).setAttackStarted(false);
      ((IsoPlayer)var1).setAttackType((String)null);
      ((IsoLivingCharacter)var1).setDoShove(false);
      var1.setDoGrapple(false);
      var1.clearVariable("RackWeapon");
      var1.clearVariable("bShoveAiming");
      var1.clearVariable("PlayedSwingSound");
      var1.clearVariable("PlayerVoiceSound");
      boolean var6 = var2.get(PARAM_ATTACKED) == Boolean.TRUE;
      if (var5 != null && (var5.getCondition() <= 0 || var6 && var5.isUseSelf())) {
         var1.removeFromHands(var5);
         if (DebugOptions.instance.Multiplayer.Debug.AutoEquip.getValue() && var5.getPhysicsObject() != null) {
            var1.setPrimaryHandItem(var1.getInventory().getItemFromType(var5.getType()));
         }

         var1.getInventory().setDrawDirty(true);
      }

      if (var1.isRangedWeaponEmpty()) {
         var1.setRecoilDelay(10.0F);
      }

      var1.setRangedWeaponEmpty(false);
      var1.setForceShove(false);
      var1.setClickSound((String)null);
      if (var6) {
         LuaEventManager.triggerEvent("OnPlayerAttackFinished", var1, var5);
      }

   }

   private void GrappleGrabCollisionCheck(IsoGameCharacter var1, HandWeapon var2, String var3) {
      HashMap var4 = var1.getStateMachineParams(this);
      IsoLivingCharacter var5 = (IsoLivingCharacter)Type.tryCastTo(var1, IsoLivingCharacter.class);
      if (var5 == null) {
         DebugLog.Grapple.warn("GrappleGrabCollisionCheck. Failed. Character is not an IsoLivingCharacter.");
      } else if (!var1.isPerformingGrappleGrabAnimation()) {
         DebugLog.Grapple.warn("GrappleGrabCollisionCheck. Failed. Character isPerformingGrappleGrabAnimation returned FALSE.");
      } else {
         if (GameServer.bServer) {
            DebugLog.Network.println("GrappleGrabCollisionCheck.");
         }

         LuaEventManager.triggerEvent("GrappleGrabCollisionCheck", var1, var2);
         var1.getAttackVars().setWeapon(var2);
         var1.getAttackVars().targetOnGround.setMovingObject(var5.targetOnGround);
         var1.getAttackVars().bAimAtFloor = var1.isAimAtFloor();
         var1.getAttackVars().bDoShove = false;
         var1.getAttackVars().bDoGrapple = true;
         CombatManager.getInstance().calculateHitInfoList(var1);
         HitInfo var7;
         if (DebugLog.Grapple.isEnabled()) {
            DebugLog.Grapple.debugln("HitList: ");
            DebugLog.Grapple.debugln("{");
            Iterator var6 = var1.getHitInfoList().iterator();

            while(var6.hasNext()) {
               var7 = (HitInfo)var6.next();
               DebugLog.Grapple.debugln("\t%s", var7.getDescription());
            }

            DebugLog.Grapple.debugln("} // HitList end. ");
         }

         int var13 = var1.getHitInfoList().size();
         var1.setLastHitCount(var13);
         if (var13 == 0) {
            DebugLog.Grapple.println("GrappleGrabCollisionCheck. Missed.");
         } else {
            DebugLog.Grapple.println("GrappleGrabCollisionCheck. Hit.");
            DebugLog.Grapple.println("{");
            var7 = null;
            IsoGameCharacter var8 = null;
            IsoDeadBody var9 = null;

            for(int var10 = 0; var10 < var13; ++var10) {
               HitInfo var11 = (HitInfo)var1.getHitInfoList().get(var10);
               IsoMovingObject var12 = var11.getObject();
               var8 = (IsoGameCharacter)Type.tryCastTo(var12, IsoGameCharacter.class);
               if (var8 != null) {
                  var7 = var11;
                  break;
               }

               if (var9 == null) {
                  var9 = (IsoDeadBody)Type.tryCastTo(var12, IsoDeadBody.class);
                  if (var9 != null) {
                     var7 = var11;
                  }
               }
            }

            if (var7 == null) {
               DebugLog.Grapple.println("    No grapple-able characters found.");
               DebugLog.Grapple.println("}");
            } else {
               DebugLog.Grapple.println("    Grapple target found: %s", var7.getDescription());
               DebugLog.Grapple.println("}");
               IsoPlayer var14 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
               float var15 = var1.calculateGrappleEffectivenessFromTraits();
               if (var14.isLocalPlayer() || var1.isNPC()) {
                  boolean var16 = var3.endsWith("_CorpseOnly");
                  if (var8 != null && !var16) {
                     var8.Grappled(var5, var2, var15, var3);
                  } else if (var9 != null) {
                     var9.Grappled(var5, var2, var15, var3);
                  }
               }

               var4.put(PARAM_LOWER_CONDITION, Boolean.FALSE);
               var4.put(PARAM_ATTACKED, Boolean.TRUE);
            }
         }
      }
   }

   private void changeWeapon(HandWeapon var1, IsoGameCharacter var2) {
      if (var1 != null && var1.isUseSelf()) {
         var2.getInventory().setDrawDirty(true);
         Iterator var3 = var2.getInventory().getItems().iterator();

         while(var3.hasNext()) {
            InventoryItem var4 = (InventoryItem)var3.next();
            if (var4 != var1 && var4 instanceof HandWeapon && var4.getType() == var1.getType() && var4.getCondition() > 0) {
               if (var2.getPrimaryHandItem() == var1 && var2.getSecondaryHandItem() == var1) {
                  var2.setPrimaryHandItem(var4);
                  var2.setSecondaryHandItem(var4);
               } else if (var2.getPrimaryHandItem() == var1) {
                  var2.setPrimaryHandItem(var4);
               } else if (var2.getSecondaryHandItem() == var1) {
                  var2.setSecondaryHandItem(var4);
               }

               return;
            }
         }
      }

      if (var1 == null || var1.getCondition() <= 0 || var1.isUseSelf()) {
         HandWeapon var5 = (HandWeapon)var2.getInventory().getBestWeapon(var2.getDescriptor());
         var2.setPrimaryHandItem((InventoryItem)null);
         if (var2.getSecondaryHandItem() == var1) {
            var2.setSecondaryHandItem((InventoryItem)null);
         }

         if (var5 != null && var5 != var2.getPrimaryHandItem() && var5.getCondition() > 0) {
            var2.setPrimaryHandItem(var5);
            if (var5.isTwoHandWeapon() && var2.getSecondaryHandItem() == null) {
               var2.setSecondaryHandItem(var5);
            }
         }
      }

   }

   private static void checkRangedWeaponFailedToShoot(IsoGameCharacter var0) {
      if (!GameServer.bServer) {
         IsoPlayer var1 = (IsoPlayer)Type.tryCastTo(var0, IsoPlayer.class);
         if (var1 != null && var1.isLocalPlayer()) {
            int var2 = var1.getStats().MusicZombiesTargeting_NearbyMoving;
            var2 += var1.getStats().MusicZombiesTargeting_NearbyNotMoving;
            if (var2 > 0) {
               var1.triggerMusicIntensityEvent("RangedWeaponFailedToShoot");
            }

         }
      }
   }
}
