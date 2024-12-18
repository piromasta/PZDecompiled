package zombie.characters.animals;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.CombatManager;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.WorldSoundManager;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.ai.states.animals.AnimalAlertedState;
import zombie.ai.states.animals.AnimalAttackState;
import zombie.ai.states.animals.AnimalClimbOverFenceState;
import zombie.ai.states.animals.AnimalEatState;
import zombie.ai.states.animals.AnimalFalldownState;
import zombie.ai.states.animals.AnimalFollowWallState;
import zombie.ai.states.animals.AnimalHitReactionState;
import zombie.ai.states.animals.AnimalIdleState;
import zombie.ai.states.animals.AnimalOnGroundState;
import zombie.ai.states.animals.AnimalPathFindState;
import zombie.ai.states.animals.AnimalWalkState;
import zombie.ai.states.animals.AnimalZoneState;
import zombie.characters.AnimalFootstepManager;
import zombie.characters.AnimalVocalsManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Position3D;
import zombie.characters.SurvivorDesc;
import zombie.characters.action.ActionGroup;
import zombie.characters.animals.behavior.BaseAnimalBehavior;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.characters.animals.datas.AnimalData;
import zombie.characters.animals.datas.AnimalGrowStage;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.advancedanimation.LiveAnimNode;
import zombie.core.skinnedmodel.animation.AnimationMultiTrack;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.visual.AnimalVisual;
import zombie.core.skinnedmodel.visual.IAnimalVisual;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugLog;
import zombie.debug.LineDrawer;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoButcherHook;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoPhysicsObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.areas.DesignationZone;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.fboRenderChunk.FBORenderShadows;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoFeedingTrough;
import zombie.iso.objects.IsoHutch;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoTelevision;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.RainManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.pathfind.Path;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.popman.animal.AnimalOwnershipManager;
import zombie.popman.animal.AnimalSynchronizationManager;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.ui.TextManager;
import zombie.util.PZCalendar;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public class IsoAnimal extends IsoPlayer implements IAnimalVisual {
   public static Boolean DISPLAY_EXTRA_VALUES = false;
   private static final long serialVersionUID = 1L;
   private static final Vector3f tempVector3f = new Vector3f();
   private static final Vector3 tempVector3 = new Vector3();
   public static final Vector2 tempVector2 = new Vector2();
   public int animalID;
   public int itemID;
   public IsoMovingObject spottedChr;
   private String type;
   private BaseAnimalBehavior behavior;
   private AnimalData data;
   public IsoGameCharacter attackedBy;
   public long attackedTimer;
   private boolean invincible;
   public int attachBackToMother;
   private int attachBackToTreeX;
   private int attachBackToTreeY;
   public long timeSinceLastUpdate;
   private String customName;
   public boolean smallEnclosure;
   public AnimalDefinitions adef;
   public IsoAnimal mother;
   public int motherID;
   private int milkRemoved;
   public IsoFeedingTrough eatFromTrough;
   public IsoWorldInventoryObject eatFromGround;
   public IsoFeedingTrough drinkFromTrough;
   public IsoHutch hutch;
   public HashMap<String, AnimalGene> fullGenome;
   public IsoGameCharacter atkTarget;
   public IsoObject thumpTarget;
   public IsoGameCharacter fightingOpponent;
   public Object soundSourceTarget;
   private float timeSinceRespondToSound;
   private float timeSinceFleeFromSound;
   public float stressLevel;
   private final AnimalVisual animalVisual;
   private AnimalZone m_animalZone;
   private boolean m_bMoveForwardOnZone;
   public int eggTimerInHutch;
   public int nestBox;
   public HashMap<Short, Float> playerAcceptanceList;
   public IsoPlayer heldBy;
   public IsoPlayer luredBy;
   private float luredStartTimer;
   public boolean walkToCharLuring;
   public ArrayList<String> geneticDisorder;
   private float petTimer;
   public DesignationZoneAnimal dZone;
   public DesignationZoneAnimal previousDZone;
   public ArrayList<DesignationZoneAnimal> connectedDZone;
   private float zoneCheckTimer;
   public InventoryItem movingToFood;
   public float movingToFoodTimer;
   private final HashMap<String, AnimalSoundState> animalSoundState;
   public ArrayList<IsoFeedingTrough> ignoredTrough;
   public float attachBackToMotherTimer;
   public double virtualID;
   public String migrationGroup;
   public boolean wild;
   public boolean alerted;
   public IsoMovingObject alertedChr;
   public boolean fromMeta;
   private float thumpDelay;
   private boolean shouldBeSkeleton;
   private ArrayList<IsoAnimal> babies;
   private float zoneAcceptance;
   public boolean followingWall;
   public boolean shouldFollowWall;
   private boolean onHook;
   private IsoButcherHook hook;
   public int attachBackToHookX;
   public int attachBackToHookY;
   public int attachBackToHookZ;
   private String nextFootstepSound;
   private String forceNextIdleSound;

   public IsoAnimal(IsoCell var1) {
      this(var1, 0, 0, 0, (String)null, (String)"");
      this.registerVariableCallbacks();
      this.setDefaultState(AnimalIdleState.instance());
      this.setCollidable(true);
      this.initAttachedItems("Animal");
   }

   public IsoAnimal(IsoCell var1, int var2, int var3, int var4, String var5, String var6) {
      super(var1, new SurvivorDesc(), var2, var3, var4, true);
      this.animalID = 0;
      this.itemID = 0;
      this.spottedChr = null;
      this.behavior = null;
      this.data = null;
      this.attackedBy = null;
      this.attackedTimer = -1L;
      this.invincible = false;
      this.attachBackToMother = 0;
      this.attachBackToTreeX = 0;
      this.attachBackToTreeY = 0;
      this.timeSinceLastUpdate = -1L;
      this.customName = null;
      this.smallEnclosure = false;
      this.motherID = 0;
      this.milkRemoved = 0;
      this.eatFromTrough = null;
      this.eatFromGround = null;
      this.drinkFromTrough = null;
      this.fullGenome = new HashMap();
      this.atkTarget = null;
      this.thumpTarget = null;
      this.fightingOpponent = null;
      this.soundSourceTarget = null;
      this.timeSinceRespondToSound = 1000000.0F;
      this.timeSinceFleeFromSound = 0.0F;
      this.stressLevel = 0.0F;
      this.animalVisual = new AnimalVisual(this);
      this.m_animalZone = null;
      this.m_bMoveForwardOnZone = false;
      this.eggTimerInHutch = 0;
      this.nestBox = -1;
      this.playerAcceptanceList = new HashMap();
      this.heldBy = null;
      this.luredBy = null;
      this.luredStartTimer = -1.0F;
      this.walkToCharLuring = false;
      this.geneticDisorder = new ArrayList();
      this.petTimer = 0.0F;
      this.dZone = null;
      this.previousDZone = null;
      this.connectedDZone = new ArrayList();
      this.zoneCheckTimer = 0.0F;
      this.movingToFood = null;
      this.movingToFoodTimer = 0.0F;
      this.animalSoundState = new HashMap();
      this.ignoredTrough = new ArrayList();
      this.attachBackToMotherTimer = 0.0F;
      this.virtualID = 0.0;
      this.wild = false;
      this.alerted = false;
      this.alertedChr = null;
      this.fromMeta = false;
      this.thumpDelay = 20000.0F;
      this.shouldBeSkeleton = false;
      this.babies = null;
      this.zoneAcceptance = 0.0F;
      this.followingWall = false;
      this.shouldFollowWall = false;
      this.onHook = false;
      this.hook = null;
      this.attachBackToHookX = 0;
      this.attachBackToHookY = 0;
      this.attachBackToHookZ = 0;
      this.nextFootstepSound = null;
      this.forceNextIdleSound = null;
      this.registerVariableCallbacks();
      this.setDefaultState(AnimalIdleState.instance());
      this.setCollidable(true);
      this.setIsAnimal(true);
      this.type = var5;
      AnimalBreed var7 = null;
      this.adef = AnimalDefinitions.getDef(this.getAnimalType());
      if (this.adef == null) {
         DebugLog.Animal.debugln(var5 + " is not a valid Animal Type.");
      } else {
         if (!StringUtils.isNullOrEmpty(var6)) {
            var7 = this.adef.getBreedByName(var6);
         } else {
            var7 = this.adef.getRandomBreed();
         }

         this.init(var7);
         this.setDir(IsoDirections.getRandom());
         this.initAttachedItems("Animal");
      }
   }

   public IsoAnimal(IsoCell var1, int var2, int var3, int var4, String var5, String var6, boolean var7) {
      super(var1, new SurvivorDesc(), var2, var3, var4, true);
      this.animalID = 0;
      this.itemID = 0;
      this.spottedChr = null;
      this.behavior = null;
      this.data = null;
      this.attackedBy = null;
      this.attackedTimer = -1L;
      this.invincible = false;
      this.attachBackToMother = 0;
      this.attachBackToTreeX = 0;
      this.attachBackToTreeY = 0;
      this.timeSinceLastUpdate = -1L;
      this.customName = null;
      this.smallEnclosure = false;
      this.motherID = 0;
      this.milkRemoved = 0;
      this.eatFromTrough = null;
      this.eatFromGround = null;
      this.drinkFromTrough = null;
      this.fullGenome = new HashMap();
      this.atkTarget = null;
      this.thumpTarget = null;
      this.fightingOpponent = null;
      this.soundSourceTarget = null;
      this.timeSinceRespondToSound = 1000000.0F;
      this.timeSinceFleeFromSound = 0.0F;
      this.stressLevel = 0.0F;
      this.animalVisual = new AnimalVisual(this);
      this.m_animalZone = null;
      this.m_bMoveForwardOnZone = false;
      this.eggTimerInHutch = 0;
      this.nestBox = -1;
      this.playerAcceptanceList = new HashMap();
      this.heldBy = null;
      this.luredBy = null;
      this.luredStartTimer = -1.0F;
      this.walkToCharLuring = false;
      this.geneticDisorder = new ArrayList();
      this.petTimer = 0.0F;
      this.dZone = null;
      this.previousDZone = null;
      this.connectedDZone = new ArrayList();
      this.zoneCheckTimer = 0.0F;
      this.movingToFood = null;
      this.movingToFoodTimer = 0.0F;
      this.animalSoundState = new HashMap();
      this.ignoredTrough = new ArrayList();
      this.attachBackToMotherTimer = 0.0F;
      this.virtualID = 0.0;
      this.wild = false;
      this.alerted = false;
      this.alertedChr = null;
      this.fromMeta = false;
      this.thumpDelay = 20000.0F;
      this.shouldBeSkeleton = false;
      this.babies = null;
      this.zoneAcceptance = 0.0F;
      this.followingWall = false;
      this.shouldFollowWall = false;
      this.onHook = false;
      this.hook = null;
      this.attachBackToHookX = 0;
      this.attachBackToHookY = 0;
      this.attachBackToHookZ = 0;
      this.nextFootstepSound = null;
      this.forceNextIdleSound = null;
      this.shouldBeSkeleton = var7;
      this.registerVariableCallbacks();
      this.setDefaultState(AnimalIdleState.instance());
      this.setCollidable(true);
      this.setIsAnimal(true);
      this.type = var5;
      AnimalBreed var8 = null;
      if (!StringUtils.isNullOrEmpty(var6)) {
         this.adef = AnimalDefinitions.getDef(this.getAnimalType());
         if (this.adef == null) {
            DebugLog.Animal.debugln(var5 + " is not a valid Animal Type.");
            return;
         }

         var8 = this.adef.getBreedByName(var6);
      }

      this.init(var8);
      this.setDir(IsoDirections.getRandom());
      this.initAttachedItems("Animal");
   }

   public IsoAnimal(IsoCell var1, int var2, int var3, int var4, String var5, AnimalBreed var6) {
      super(var1, new SurvivorDesc(), var2, var3, var4, true);
      this.animalID = 0;
      this.itemID = 0;
      this.spottedChr = null;
      this.behavior = null;
      this.data = null;
      this.attackedBy = null;
      this.attackedTimer = -1L;
      this.invincible = false;
      this.attachBackToMother = 0;
      this.attachBackToTreeX = 0;
      this.attachBackToTreeY = 0;
      this.timeSinceLastUpdate = -1L;
      this.customName = null;
      this.smallEnclosure = false;
      this.motherID = 0;
      this.milkRemoved = 0;
      this.eatFromTrough = null;
      this.eatFromGround = null;
      this.drinkFromTrough = null;
      this.fullGenome = new HashMap();
      this.atkTarget = null;
      this.thumpTarget = null;
      this.fightingOpponent = null;
      this.soundSourceTarget = null;
      this.timeSinceRespondToSound = 1000000.0F;
      this.timeSinceFleeFromSound = 0.0F;
      this.stressLevel = 0.0F;
      this.animalVisual = new AnimalVisual(this);
      this.m_animalZone = null;
      this.m_bMoveForwardOnZone = false;
      this.eggTimerInHutch = 0;
      this.nestBox = -1;
      this.playerAcceptanceList = new HashMap();
      this.heldBy = null;
      this.luredBy = null;
      this.luredStartTimer = -1.0F;
      this.walkToCharLuring = false;
      this.geneticDisorder = new ArrayList();
      this.petTimer = 0.0F;
      this.dZone = null;
      this.previousDZone = null;
      this.connectedDZone = new ArrayList();
      this.zoneCheckTimer = 0.0F;
      this.movingToFood = null;
      this.movingToFoodTimer = 0.0F;
      this.animalSoundState = new HashMap();
      this.ignoredTrough = new ArrayList();
      this.attachBackToMotherTimer = 0.0F;
      this.virtualID = 0.0;
      this.wild = false;
      this.alerted = false;
      this.alertedChr = null;
      this.fromMeta = false;
      this.thumpDelay = 20000.0F;
      this.shouldBeSkeleton = false;
      this.babies = null;
      this.zoneAcceptance = 0.0F;
      this.followingWall = false;
      this.shouldFollowWall = false;
      this.onHook = false;
      this.hook = null;
      this.attachBackToHookX = 0;
      this.attachBackToHookY = 0;
      this.attachBackToHookZ = 0;
      this.nextFootstepSound = null;
      this.forceNextIdleSound = null;
      this.registerVariableCallbacks();
      this.setDefaultState(AnimalIdleState.instance());
      this.setCollidable(true);
      this.setIsAnimal(true);
      this.type = var5;
      this.init(var6);
      this.setDir(IsoDirections.getRandom());
      this.initAttachedItems("Animal");
   }

   public IsoAnimal(IsoCell var1, int var2, int var3, int var4, String var5, AnimalBreed var6, boolean var7) {
      super(var1, new SurvivorDesc(), var2, var3, var4, true);
      this.animalID = 0;
      this.itemID = 0;
      this.spottedChr = null;
      this.behavior = null;
      this.data = null;
      this.attackedBy = null;
      this.attackedTimer = -1L;
      this.invincible = false;
      this.attachBackToMother = 0;
      this.attachBackToTreeX = 0;
      this.attachBackToTreeY = 0;
      this.timeSinceLastUpdate = -1L;
      this.customName = null;
      this.smallEnclosure = false;
      this.motherID = 0;
      this.milkRemoved = 0;
      this.eatFromTrough = null;
      this.eatFromGround = null;
      this.drinkFromTrough = null;
      this.fullGenome = new HashMap();
      this.atkTarget = null;
      this.thumpTarget = null;
      this.fightingOpponent = null;
      this.soundSourceTarget = null;
      this.timeSinceRespondToSound = 1000000.0F;
      this.timeSinceFleeFromSound = 0.0F;
      this.stressLevel = 0.0F;
      this.animalVisual = new AnimalVisual(this);
      this.m_animalZone = null;
      this.m_bMoveForwardOnZone = false;
      this.eggTimerInHutch = 0;
      this.nestBox = -1;
      this.playerAcceptanceList = new HashMap();
      this.heldBy = null;
      this.luredBy = null;
      this.luredStartTimer = -1.0F;
      this.walkToCharLuring = false;
      this.geneticDisorder = new ArrayList();
      this.petTimer = 0.0F;
      this.dZone = null;
      this.previousDZone = null;
      this.connectedDZone = new ArrayList();
      this.zoneCheckTimer = 0.0F;
      this.movingToFood = null;
      this.movingToFoodTimer = 0.0F;
      this.animalSoundState = new HashMap();
      this.ignoredTrough = new ArrayList();
      this.attachBackToMotherTimer = 0.0F;
      this.virtualID = 0.0;
      this.wild = false;
      this.alerted = false;
      this.alertedChr = null;
      this.fromMeta = false;
      this.thumpDelay = 20000.0F;
      this.shouldBeSkeleton = false;
      this.babies = null;
      this.zoneAcceptance = 0.0F;
      this.followingWall = false;
      this.shouldFollowWall = false;
      this.onHook = false;
      this.hook = null;
      this.attachBackToHookX = 0;
      this.attachBackToHookY = 0;
      this.attachBackToHookZ = 0;
      this.nextFootstepSound = null;
      this.forceNextIdleSound = null;
      this.shouldBeSkeleton = var7;
      this.registerVariableCallbacks();
      this.setDefaultState(AnimalIdleState.instance());
      this.setCollidable(true);
      this.setIsAnimal(true);
      this.type = var5;
      this.init(var6);
      this.setDir(IsoDirections.getRandom());
      this.initAttachedItems("Animal");
   }

   public String getObjectName() {
      return "Animal";
   }

   private void registerVariableCallbacks() {
      this.setVariable("bdead", this::isDead);
      this.setVariable("isAnimalEating", this::isAnimalEating);
      this.setVariable("isAnimalAttacking", this::isAnimalAttacking);
      this.setVariable("hasAnimalZone", this::hasAnimalZone);
      this.setVariable("isAlerted", this::isAlerted);
      this.setVariable("shouldFollowWall", this::shouldFollowWall);
   }

   public AnimalVisual getAnimalVisual() {
      return this.animalVisual;
   }

   public void addToWorld() {
      super.addToWorld();
      if (this.isOnHook()) {
         this.setVariable("onhook", true);
      }

   }

   public String GetAnimSetName() {
      return this.adef == null ? "cow" : this.adef.animset;
   }

   public void playSoundDebug() {
      this.addWorldSoundUnlessInvisible(40, 30, false);
   }

   public void update() {
      if (this.isOnHook()) {
         this.reattachBackToHook();
         this.ensureCorrectSkin();
      } else {
         if (this.getVariableBoolean("bPathfind") && this.getStateMachine().getCurrent() != AnimalPathFindState.instance()) {
            this.getStateMachine().changeState(AnimalPathFindState.instance(), (Iterable)null);
         }

         this.updateEmitter();
         if (this.getSquare() != null) {
            this.doDeferredMovement();
         }

         if (!this.isDead()) {
            this.updateInternal();
            if (GameTime.getInstance().getMultiplier() > 10.0F) {
               this.setTurnDelta(1.0F);
            } else {
               this.setTurnDelta(this.adef.turnDelta);
            }

         }
      }
   }

   private void updateZoneAcceptance() {
      if (!this.connectedDZone.isEmpty() && this.zoneAcceptance < 100.0F) {
         this.zoneAcceptance += GameTime.getInstance().getMultiplier() / 1000.0F;
         if (this.zoneAcceptance > 100.0F) {
            this.zoneAcceptance = 100.0F;
         }
      }

   }

   public void test() {
      AnimationPlayer var1 = this.getAnimationPlayer();
      AnimationMultiTrack var2 = var1.getMultiTrack();
      if (var2 != null && !var2.getTracks().isEmpty()) {
         ((AnimationTrack)var2.getTracks().get(0)).setCurrentTimeValue((float)Rand.Next(100));
      }
   }

   private void updateInternal() {
      if (!this.fromMeta) {
         if (this.behavior != null && this.data != null) {
            if ((!GameServer.bServer || this.getVehicle() != null || this.getHutch() != null) && (!GameClient.bClient || this.isLocalPlayer())) {
               this.behavior.update();
               this.data.update();
               this.width = this.adef.collisionSize * this.getAnimalSize();
               this.separate();
               this.reattachToTree();
               this.checkZone();
               this.reattachBackToMom();
               if (this.timeSinceRespondToSound > 5.0F) {
                  this.respondToSound();
               }

               if (this.petTimer > 0.0F) {
                  this.petTimer -= GameTime.getInstance().getMultiplier();
                  if (this.petTimer < 0.0F) {
                     this.petTimer = 0.0F;
                  }
               }

               this.timeSinceRespondToSound += GameTime.getInstance().getThirtyFPSMultiplier();
               this.updateStress();
               this.updateLured();
               this.updateEmitter();
               this.tryThump((IsoGridSquare)null);
               this.updateLOS();
               if (this.vehicle4testCollision != null) {
                  this.setVehicleCollision(this.testCollideWithVehicles(this.vehicle4testCollision));
                  this.vehicle4testCollision = null;
               }

               super.update();
            } else {
               this.width = this.adef.collisionSize * this.getAnimalSize();
               this.separate();
               super.update();
            }
         }
      }
   }

   public boolean testCollideWithVehicles(BaseVehicle var1) {
      if (this.Health <= 0.0F) {
         return false;
      } else {
         if (var1.shouldCollideWithCharacters()) {
            Vector2 var2 = (Vector2)((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).alloc();
            if (var1.testCollisionWithCharacter(this, 0.3F, var2) != null) {
               ((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).release(var2);
               var1.hitCharacter(this);
               super.update();
               Vector3f var3 = new Vector3f();
               var3.set(0.0F, 17.0F, 0.0F);
               var1.ApplyImpulse(this, 1.0F);
               return true;
            }

            ((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).release(var2);
         }

         return false;
      }
   }

   public void applyDamageFromVehicle(float var1, float var2) {
      this.addBlood(var1);
      CombatManager.getInstance().applyDamage((IsoGameCharacter)this, var2);
   }

   public float Hit(BaseVehicle var1, float var2, boolean var3, float var4, float var5) {
      this.Hit(var1, var2, var3, var4, var5);
      return 1.0F;
   }

   public float Hit(BaseVehicle var1, float var2, boolean var3, Vector2 var4) {
      float var5 = 0.0F;
      this.setHitDir(var4);
      this.setHitForce(var2 * 0.15F);
      int var6 = (int)(var2 * 6.0F);
      if (!this.isOnFloor() && this.getCurrentState() != AnimalOnGroundState.instance()) {
         var5 = this.getHealth();
         if (!GameServer.bServer && !GameClient.bClient) {
            this.setHealth(0.0F);
         }
      } else {
         this.setHitReaction("Floor");
         if (!GameServer.bServer && !GameClient.bClient) {
            this.setHealth(0.0F);
         }
      }

      if (!GameServer.bServer && !GameClient.bClient) {
         this.addBlood(var2);
      }

      return var5;
   }

   protected void onAnimPlayerCreated(AnimationPlayer var1) {
      super.onAnimPlayerCreated(var1);
      var1.setTwistBones("Bip01_Head");
      var1.setCounterRotationBone("Bip01");
   }

   public float getPetTimer() {
      return this.petTimer;
   }

   private void reattachBackToMom() {
      int var1;
      if (this.attachBackToMother > 0 && this.getVehicle() != null) {
         for(var1 = 0; var1 < this.getVehicle().getAnimals().size(); ++var1) {
            IsoAnimal var2 = (IsoAnimal)this.getVehicle().getAnimals().get(var1);
            if (var2.getAnimalID() == this.attachBackToMother) {
               this.setMother(var2);
               break;
            }
         }
      }

      if (this.isExistInTheWorld()) {
         if (this.attachBackToMother == 0 && (this.mother == null || !this.mother.isExistInTheWorld()) && this.motherID > 0) {
            this.attachBackToMother = this.motherID;
         }

         if (this.attachBackToMother > 0 && this.getVehicle() == null) {
            if (this.attachBackToMotherTimer < 50.0F) {
               this.attachBackToMotherTimer += GameTime.getInstance().getMultiplier();
               return;
            }

            this.attachBackToMotherTimer = 0.0F;

            for(var1 = 0; var1 < this.connectedDZone.size(); ++var1) {
               DesignationZoneAnimal var4 = (DesignationZoneAnimal)this.connectedDZone.get(var1);

               for(int var3 = 0; var3 < var4.animals.size(); ++var3) {
                  if (((IsoAnimal)var4.animals.get(var3)).animalID == this.attachBackToMother) {
                     this.setMother((IsoAnimal)var4.animals.get(var3));
                     this.attachBackToMother = 0;
                     this.motherID = this.mother.animalID;
                     return;
                  }
               }
            }
         }

      }
   }

   private void checkZone() {
      if (this.zoneCheckTimer > 0.0F) {
         this.zoneCheckTimer -= GameTime.getInstance().getMultiplier();
      } else {
         this.zoneCheckTimer = 2000.0F;
         this.dZone = DesignationZoneAnimal.getZone((int)this.getX(), (int)this.getY(), (int)this.getZ());
         if (this.dZone != null && !this.dZone.animals.contains(this)) {
            this.dZone.animals.add(this);
         }

         if (this.previousDZone != this.dZone) {
            if (this.previousDZone != null) {
               this.previousDZone.animals.remove(this);
               this.previousDZone = null;
            } else {
               this.previousDZone = this.dZone;
            }
         }

         this.connectedDZone = DesignationZoneAnimal.getAllDZones((ArrayList)null, this.dZone, (DesignationZoneAnimal)null);
         if (!this.connectedDZone.isEmpty()) {
            this.setWild(false);

            for(int var1 = 0; var1 < this.connectedDZone.size(); ++var1) {
               ((DesignationZoneAnimal)this.connectedDZone.get(var1)).check();
            }
         }

         this.updateZoneAcceptance();
      }
   }

   public IsoGridSquare getRandomSquareInZone() {
      if (this.connectedDZone.isEmpty()) {
         return null;
      } else {
         DesignationZone var1 = (DesignationZone)this.connectedDZone.get(Rand.Next(0, this.connectedDZone.size()));
         return var1.getRandomSquare();
      }
   }

   public void stopAllMovementNow() {
      this.setMoving(false);
      this.getPathFindBehavior2().reset();
      this.setPath2((Path)null);
      this.getStateMachine().changeState(this.getDefaultState(), (Iterable)null);
      this.getData().resetEatingCheck();
   }

   public void cancelLuring() {
      this.stopAllMovementNow();
      if (this.luredBy != null) {
         this.luredBy.luredAnimals.remove(this);
      }

      this.luredBy = null;
      this.walkToCharLuring = false;
      this.luredStartTimer = -1.0F;
   }

   private void updateLured() {
      if (this.luredBy != null) {
         if (!this.luredBy.isLuringAnimals) {
            this.cancelLuring();
         } else if (this.walkToCharLuring) {
            if (!this.isAnimalMoving()) {
               if (!this.luredBy.getLuredAnimals().contains(this)) {
                  this.luredBy.getLuredAnimals().add(this);
               }

               this.setVariable("animalRunning", false);
               this.pathToCharacter(this.luredBy);
            }

         } else {
            if (this.luredStartTimer > -1.0F) {
               --this.luredStartTimer;
               if (this.luredStartTimer == 0.0F) {
                  this.walkToCharLuring = true;
               }
            }

         }
      }
   }

   public void updateStress() {
      if (!this.isWild()) {
         boolean var1 = true;
         if (this.heldBy != null) {
            this.changeStress(-(GameTime.getInstance().getMultiplier() / 3000.0F));
            this.addAcceptance(this.heldBy, GameTime.getInstance().getMultiplier() / 10000.0F);
         }

         if (this.getVehicle() != null && this.getVehicle().getCurrentSpeedKmHour() > 5.0F) {
            this.changeStress(GameTime.getInstance().getMultiplier() / 40000.0F);
            var1 = false;
         }

         if ((double)this.getStats().hunger > 0.8 || (double)this.getStats().thirst > 0.8) {
            this.changeStress(GameTime.getInstance().getMultiplier() / 50000.0F);
            var1 = false;
         }

         if (this.shouldAnimalStressAboveGround()) {
            this.changeStress(GameTime.getInstance().getMultiplier() / 10000.0F);
            var1 = false;
         }

         if (this.adef.stressUnderRain && RainManager.isRaining() && this.getSquare() != null && !this.getSquare().haveRoof) {
            this.changeStress(GameTime.getInstance().getMultiplier() / 30000.0F);
            var1 = false;
         }

         if (var1) {
            this.changeStress(-(GameTime.getInstance().getMultiplier() / 5500.0F));
         }

      }
   }

   private void reattachToTree() {
      if (this.attachBackToTreeX > 0 && this.attachBackToTreeY > 0 && this.getSquare() != null) {
         IsoGridSquare var1 = this.getSquare().getCell().getGridSquare(this.attachBackToTreeX, this.attachBackToTreeY, this.getSquare().getZ());
         if (var1 == null) {
            return;
         }

         if (var1.getTree() != null) {
            this.getData().setAttachedTree(var1.getTree());
            this.attachBackToTreeX = 0;
            this.attachBackToTreeY = 0;
            return;
         }

         for(int var2 = 0; var2 < var1.getSpecialObjects().size(); ++var2) {
            IsoObject var3 = (IsoObject)var1.getSpecialObjects().get(var2);
            if (var3.getProperties().Is("CanAttachAnimal")) {
               this.getData().setAttachedTree(var3);
               this.attachBackToTreeX = 0;
               this.attachBackToTreeY = 0;
               return;
            }
         }
      }

   }

   public void respondToSound() {
      if (this.soundSourceTarget != null) {
         this.timeSinceFleeFromSound -= GameTime.getInstance().getMultiplier();
         if (!(this.timeSinceFleeFromSound <= 0.0F) && this.isAnimalMoving()) {
            return;
         }

         this.setMoving(false);
         this.soundSourceTarget = null;
      }

      WorldSoundManager.WorldSound var1 = WorldSoundManager.instance.getSoundAnimal(this);
      float var2 = WorldSoundManager.instance.getSoundAttractAnimal(var1, this);
      if (var1 != null && !(var2 <= 0.0F) && !(var1.source instanceof IsoRadio) && !(var1.source instanceof IsoTelevision)) {
         float var3 = IsoUtils.DistanceTo(this.getX(), this.getY(), this.getZ() * 3.0F, (float)var1.x, (float)var1.y, (float)(var1.z * 3));
         float var4 = 1.0F;
         if (this.isWild()) {
            var4 = 3.0F;
         }

         if (this.isWild() || !(var1.source instanceof BaseVehicle)) {
            if (!(var3 > (float)var1.radius * var4)) {
               if (var1.source instanceof IsoPlayer && ((IsoPlayer)var1.source).callOut && this.getPlayerAcceptance((IsoPlayer)var1.source) > 40.0F) {
                  this.pathToLocation((int)((IsoPlayer)var1.source).getX(), (int)((IsoPlayer)var1.source).getY(), (int)((IsoPlayer)var1.source).getZ());
               } else {
                  this.soundSourceTarget = var1;
                  this.timeSinceFleeFromSound = 1000.0F * ((float)var1.radius / 0.5F);
                  tempVector2.x = this.getX();
                  tempVector2.y = this.getY();
                  Vector2 var10000 = tempVector2;
                  var10000.x -= (float)var1.x;
                  var10000 = tempVector2;
                  var10000.y -= (float)var1.y;
                  byte var5 = 10;
                  if (this.isWild()) {
                     var5 = 30;
                     this.setIsAlerted(false);
                     this.getBehavior().lastAlerted = 1000.0F;
                  }

                  tempVector2.setLength(Math.max((float)var5, (float)var5 - tempVector2.getLength()));
                  int var6 = tempVector2.floorX();
                  int var7 = tempVector2.floorY();
                  this.changeStress((float)var1.radius / 10.0F);
                  this.setVariable("animalRunning", true);
                  this.pathToLocation((int)this.getX() + var6, (int)this.getY() + var7, (int)this.getZ());
               }
            }
         }
      }
   }

   public float calcDamage() {
      float var1 = this.adef.baseDmg;
      AnimalAllele var2 = this.getUsedGene("strength");
      if (var2 != null) {
         var1 *= var2.currentValue;
      }

      return var1;
   }

   public void HitByAnimal(IsoAnimal var1, boolean var2) {
      float var3 = var1.adef.baseDmg;
      AnimalAllele var4 = var1.getUsedGene("strength");
      if (var4 != null) {
         var3 *= var4.currentValue;
      }

      this.setHitReaction("default");
      if (!var2) {
         this.setHealth(this.getHealth() - var3 * this.getData().getHealthLoss(0.04F));
         if (this.getSquare() != null) {
            IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare(this.getSquare().x, this.getSquare().y, this.getSquare().getZ());
            if (var5 != null) {
               for(int var6 = 0; var6 < 10; ++var6) {
                  var5.getChunk().addBloodSplat((float)var5.x + Rand.Next(-0.8F, 0.8F), (float)var5.y + Rand.Next(-0.8F, 0.8F), (float)var5.z, Rand.Next(8));
               }
            }
         }
      }

      if (this.isDead()) {
         if (this.fightingOpponent instanceof IsoAnimal) {
            ((IsoAnimal)this.fightingOpponent).fightingOpponent = null;
         }

         this.fightingOpponent = null;
      }

   }

   public void initializeStates() {
      this.clearAIStateMap();
      this.registerAIState("idle", AnimalIdleState.instance());
      this.registerAIState("eating", AnimalEatState.instance());
      this.registerAIState("attack", AnimalAttackState.instance());
      this.registerAIState("walk", AnimalWalkState.instance());
      this.registerAIState("pathfind", AnimalPathFindState.instance());
      this.registerAIState("followwall", AnimalFollowWallState.instance());
      this.registerAIState("hitreaction", AnimalHitReactionState.instance());
      this.registerAIState("falldown", AnimalFalldownState.instance());
      this.registerAIState("onground", AnimalOnGroundState.instance());
      this.registerAIState("zone", AnimalZoneState.instance());
      this.registerAIState("alerted", AnimalAlertedState.instance());
      this.registerAIState("climbfence", AnimalClimbOverFenceState.instance());
   }

   public void spotted(IsoMovingObject var1, boolean var2, float var3) {
      this.behavior.spotted(var1, var2, var3);
   }

   public void drawRope(IsoGameCharacter var1) {
      if (!this.isExistInTheWorld() && this.getData() != null) {
         this.getData().getAttachedPlayer().removeAttachedAnimal(this);
      }

      int var2 = this.getAnimationPlayer().getSkinningBoneIndex(this.adef.ropeBone, -1);
      Model.BoneToWorldCoords((IsoGameCharacter)this, var2, tempVector3);
      float var3 = 0.5F;
      float var4 = 0.37F;
      float var5 = 0.3F;
      float var6 = var1.DistToProper(this);
      if (var6 > 10.0F) {
         var6 -= 10.0F;
         var3 += var6 / 10.0F;
         var4 -= var6 / 10.0F;
         var5 -= var6 / 10.0F;
      }

      float var7 = IsoUtils.XToScreenExact(tempVector3.x, tempVector3.y, tempVector3.z, 0);
      var7 -= this.getAnimalSize();
      float var8 = IsoUtils.YToScreenExact(tempVector3.x, tempVector3.y, tempVector3.z, 0);
      var8 -= this.getAnimalSize();
      var2 = var1.getAnimationPlayer().getSkinningBoneIndex("Bip01_R_Finger0", -1);
      Model.BoneToWorldCoords(var1, var2, tempVector3);
      float var9 = IsoUtils.XToScreenExact(tempVector3.x, tempVector3.y, tempVector3.z, 0);
      float var10 = IsoUtils.YToScreenExact(tempVector3.x, tempVector3.y, tempVector3.z, 0);
      LineDrawer.drawLine(var7, var8, var9, var10, var3, var4, var5, 1.0F, 2);
   }

   private void drawRope(IsoGridSquare var1) {
      float var2 = 0.5F;
      float var3 = 0.37F;
      float var4 = 0.3F;
      int var5 = this.getAnimationPlayer().getSkinningBoneIndex("Bip01_Neck", -1);
      Model.BoneToWorldCoords((IsoGameCharacter)this, var5, tempVector3);
      float var6 = var1.DistToProper(this.getCurrentSquare());
      if (var6 > 10.0F) {
         var6 -= 10.0F;
         var2 += var6 / 10.0F;
         var3 -= var6 / 10.0F;
         var4 -= var6 / 10.0F;
      }

      float var7 = IsoUtils.XToScreenExact(tempVector3.x, tempVector3.y, tempVector3.z, 0);
      var7 -= this.getAnimalSize();
      float var8 = IsoUtils.YToScreenExact(tempVector3.x, tempVector3.y, tempVector3.z, 0);
      var8 -= this.getAnimalSize();
      float var9 = IsoUtils.XToScreenExact((float)var1.x, (float)var1.y, 0.1F, 0);
      float var10 = IsoUtils.YToScreenExact((float)var1.x, (float)var1.y, 0.1F, 0);
      LineDrawer.drawLine(var7, var8, var9, var10, var2, var3, var4, 1.0F, 2);
   }

   public void render(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      super.render(var1, var2, var3, var4, var5, var6, var7);
   }

   public void renderlast() {
      if (this.data != null) {
         if (this.data.getAttachedTree() != null && this.data.getAttachedTree().getSquare() != null) {
            this.drawRope(this.data.getAttachedTree().getSquare());
         }

         this.doDebugString();
         super.renderlast();
      }
   }

   private void doDebugString() {
      if (DISPLAY_EXTRA_VALUES && !this.isOnHook()) {
         if (this.legsSprite != null && this.legsSprite.modelSlot != null && this.legsSprite.modelSlot.model != null) {
            ModelInstance var1 = this.legsSprite.modelSlot.model;
            ModelScript var2 = var1.m_modelScript;

            for(int var3 = 0; var3 < var2.getAttachmentCount(); ++var3) {
               ModelAttachment var4 = var2.getAttachment(var3);
               Position3D var5 = this.getAttachmentWorldPos(var4.getId());
               if (var5 != null) {
                  LineDrawer.DrawIsoCircle(var5.x, var5.y, var5.z, 0.03F, 1.0F, 1.0F, 1.0F, 1.0F);
               }
            }
         }

         int var6 = (int)IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
         int var7 = (int)IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
         StringBuilder var8 = new StringBuilder();
         var8.append("Name: ").append(this.getFullName()).append("\n");
         var8.append("Stress: ").append(Math.round(this.stressLevel)).append("\n");
         var8.append("Health: ").append(this.getHealth() * 100.0F).append("\n");
         Iterator var9 = this.playerAcceptanceList.keySet().iterator();

         while(var9.hasNext()) {
            short var10 = (Short)var9.next();
            if (var10 == IsoPlayer.getInstance().getOnlineID()) {
               var8.append("Acceptance: ").append(Math.round((Float)this.playerAcceptanceList.get(var10))).append("\n");
            }
         }

         if (this.getData().getMilkQuantity() > 0.0F) {
            var8.append("Milk: ").append(this.getData().getMilkQuantity()).append("\n");
         }

         IndieGL.enableBlend();
         IndieGL.glBlendFunc(770, 771);
         IndieGL.StartShader(0);
         IndieGL.disableDepthTest();
         TextManager.instance.DrawString((double)var6, (double)var7, var8.toString());
      }
   }

   public void drawDirectionLine(Vector2 var1, float var2, float var3, float var4, float var5) {
      float var6 = this.getX() + var1.x * var2;
      float var7 = this.getY() + var1.y * var2;
      float var8 = IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
      float var9 = IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
      float var10 = IsoUtils.XToScreenExact(var6, var7, this.getZ(), 0);
      float var11 = IsoUtils.YToScreenExact(var6, var7, this.getZ(), 0);
      LineDrawer.drawLine(var8, var9, var10, var11, var3, var4, var5, 0.5F, 2);
   }

   public void renderShadow(float var1, float var2, float var3) {
      if (!this.isOnHook() || this.getHook() != null) {
         if (this.getHook() != null) {
            int var13 = IsoCamera.frameState.playerIndex;
            Vector3f var14 = tempVector3f;
            Vector2 var15 = this.getAnimForwardDirection(tempo2);
            var14.set(var15.x + 0.3F, var15.y + 0.3F, 0.0F);
            float var16 = this.alpha[var13];
            ColorInfo var17 = this.getHook().getSquare().lighting[var13].lightInfo();
            if (PerformanceSettings.FBORenderChunk) {
               FBORenderShadows.getInstance().addShadow((float)this.getHook().getSquare().getX() + 0.35F, (float)this.getHook().getSquare().getY() + 0.9F, (float)this.getHook().getSquare().getZ(), var14, 0.5F, 0.7F, 0.6F, var17.r, var17.g, var17.b, var16, true);
            } else {
               IsoDeadBody.renderShadow((float)this.getHook().getSquare().getX(), (float)this.getHook().getSquare().getY(), (float)this.getHook().getSquare().getZ(), var14, 0.7F, 0.8F, 0.8F, var17, var16, true);
            }
         } else {
            IsoGridSquare var4 = this.getCurrentSquare();
            if (var4 != null) {
               int var5 = IsoCamera.frameState.playerIndex;
               Vector3f var6 = tempVector3f;
               Vector2 var7 = this.getAnimForwardDirection(tempo2);
               var6.set(var7.x, var7.y, 0.0F);
               float var8 = this.adef.shadoww * this.getData().getSize();
               float var9 = this.adef.shadowfm * this.getData().getSize();
               float var10 = this.adef.shadowbm * this.getData().getSize();
               float var11 = this.alpha[var5];
               ColorInfo var12 = var4.lighting[var5].lightInfo();
               if (PerformanceSettings.FBORenderChunk) {
                  FBORenderShadows.getInstance().addShadow(var1, var2, var3, var6, var8, var9, var10, var12.r, var12.g, var12.b, var11, true);
               } else {
                  IsoDeadBody.renderShadow(var1, var2, var3, var6, var8, var9, var10, var12, var11, true);
               }
            }
         }
      }
   }

   public BaseAnimalBehavior getBehavior() {
      return this.behavior;
   }

   public void checkAlphaAndTargetAlpha(IsoPlayer var1) {
      this.setAlphaAndTarget(var1.PlayerIndex, 1.0F);
   }

   public boolean shouldDoInventory() {
      return !GameClient.bClient || this.getAttackedBy() instanceof IsoPlayer && ((IsoPlayer)this.getAttackedBy()).isLocalPlayer() || this.getAttackedBy() == IsoWorld.instance.CurrentCell.getFakeZombieForHit() && (this.wasLocal() || this.isLocal());
   }

   public boolean shouldBecomeZombieAfterDeath() {
      return false;
   }

   public void becomeCorpse() {
      if (this.getMother() != null) {
         this.getMother().removeBaby(this);
      }

      if (!this.isOnDeathDone()) {
         if (this.shouldBecomeCorpse()) {
            this.Kill(this.getAttackedBy());
            this.setOnDeathDone(true);
            if (GameClient.bClient && this.shouldDoInventory()) {
               GameClient.sendAnimalDeath(this);
            }

            if (!GameClient.bClient) {
               IsoDeadBody var1 = new IsoDeadBody(this);
               if (GameServer.bServer) {
                  GameServer.sendBecomeCorpse(var1);
               }
            }

         }
      }
   }

   public void OnDeath() {
      LuaEventManager.triggerEvent("OnCharacterDeath", this);
   }

   public void hitConsequences(HandWeapon var1, IsoGameCharacter var2, boolean var3, float var4, boolean var5) {
      if (!var3) {
         this.setHealth(this.getHealth() - var4 * this.getData().getHealthLoss(0.025F));
      }

      if (this.isWild() && !var3) {
         this.setHealth(0.0F);
      }

      if (!var3) {
         this.splatBloodFloorBig();
      }

      this.setHitReaction("hitreact");
      this.setAttackedBy(var2);
      this.attackedTimer = GameTime.getInstance().getCalender().getTimeInMillis();
      if (var2 instanceof IsoPlayer && this.getHealth() <= 0.0F) {
         this.killed((IsoPlayer)var2);
      }

      this.setDebugStress(this.getStress() + (float)Rand.Next(20, 40));
      if ((var2 instanceof IsoPlayer || var2 instanceof IsoZombie) && this.adef.attackBack) {
         this.atkTarget = var2;
         this.getBehavior().goAttack(var2);
      }

   }

   public void setHealth(float var1) {
      if (!this.isInvincible() || !(var1 < this.Health)) {
         this.Health = var1;
      }
   }

   public void killed(IsoPlayer var1) {
      if (this.Health <= 0.0F && this.dZone != null) {
         for(int var2 = 0; var2 < this.dZone.getAnimalsConnected().size(); ++var2) {
            IsoAnimal var3 = (IsoAnimal)this.dZone.getAnimalsConnected().get(var2);
            var3.changeStress(Rand.Next(10.0F, 30.0F));
            if (var1 != null && var3.DistToProper(var1) < 10.0F) {
               var3.getBehavior().forceFleeFromChr(var1);
            }
         }
      }

   }

   public void removeFromWorld() {
      if (this.getData() != null && this.getData().getAttachedPlayer() != null) {
         this.getData().getAttachedPlayer().removeAttachedAnimal(this);
      }

      super.removeFromWorld();
      this.getCell().getRemoveList().add(this);
      this.getCell().getObjectList().remove(this);
      if (this.hutch != null) {
         this.hutch.animalOutside.remove(this);
      }

      if (this.dZone != null) {
         this.dZone.removeAnimal(this);
      }

      this.removedFromWorldMS = System.currentTimeMillis();
      AnimalPopulationManager.getInstance().addToRecentlyRemoved(this);
   }

   public AnimalData getData() {
      return this.data;
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      this.save(var1, var2, true);
   }

   public void save(ByteBuffer var1, boolean var2, boolean var3) throws IOException {
      if (var3) {
         var1.put((byte)(this.Serialize() ? 1 : 0));
         var1.put(IsoObject.factoryGetClassID(this.getObjectName()));
      }

      if (this.m_animalZone != null) {
         var1.putLong(this.m_animalZone.id.getMostSignificantBits());
         var1.putLong(this.m_animalZone.id.getLeastSignificantBits());
      } else {
         var1.putLong(0L);
         var1.putLong(0L);
      }

      var1.putFloat(this.getX());
      var1.putFloat(this.getY());
      var1.putFloat(this.getZ());
      var1.putInt(this.dir.index());
      this.getStats().save(var1);
      GameWindow.WriteString(var1, this.type);
      GameWindow.WriteString(var1, this.data.getBreed().name);
      GameWindow.WriteString(var1, this.customName);
      this.getModData().save(var1);
      var1.putInt(this.itemID);
      var1.put((byte)(this.getDescriptor().isFemale() ? 1 : 0));
      var1.putInt(this.animalID);
      ArrayList var4 = new ArrayList(this.fullGenome.keySet());
      var1.putInt(this.fullGenome.size());

      for(int var5 = 0; var5 < var4.size(); ++var5) {
         String var6 = (String)var4.get(var5);
         ((AnimalGene)this.fullGenome.get(var6)).save(var1, var2);
      }

      if (this.getData().getAttachedTree() != null) {
         var1.put((byte)1);
         var1.putInt(PZMath.fastfloor(this.getData().getAttachedTree().getX()));
         var1.putInt(PZMath.fastfloor(this.getData().getAttachedTree().getY()));
      } else {
         var1.put((byte)0);
      }

      var1.putInt(this.getData().getAge());
      var1.putDouble(this.getHoursSurvived());
      var1.putLong(GameTime.getInstance().getCalender().getTimeInMillis());
      var1.putFloat(this.getData().getSize());
      var1.putInt(this.attachBackToMother);
      if (this.mother != null) {
         var1.put((byte)1);
         var1.putInt(this.mother.animalID);
      } else {
         var1.put((byte)0);
      }

      var1.put((byte)(this.getData().pregnant ? 1 : 0));
      if (this.getData().pregnant) {
         var1.putInt(this.getData().pregnantTime);
      }

      var1.put((byte)(this.getData().canHaveMilk() ? 1 : 0));
      var1.putFloat(this.getData().getMilkQuantity());
      var1.putFloat(this.getData().maxMilkActual);
      var1.putInt(this.milkRemoved);
      var1.put((byte)this.getData().getPreferredHutchPosition());
      if (this.getBreed().woolType != null && this.getData().getMaxWool() > 0.0F) {
         var1.putFloat(this.getData().woolQty);
      }

      var1.putInt(this.getData().fertilizedTime);
      var1.put((byte)(this.getData().fertilized ? 1 : 0));
      if (this.adef.eggsPerDay > 0) {
         var1.putInt(this.getData().eggsToday);
      }

      var1.putFloat(this.stressLevel);
      var1.putInt(this.playerAcceptanceList.size());
      Iterator var7 = this.playerAcceptanceList.keySet().iterator();

      while(var7.hasNext()) {
         Short var8 = (Short)var7.next();
         var1.putShort(var8);
         var1.putFloat((Float)this.playerAcceptanceList.get(var8));
      }

      var1.putFloat(this.getData().weight);
      var1.putLong(this.getData().lastPregnancyTime);
      var1.putLong(this.getData().lastMilkTimer);
      var1.putInt(this.getData().lastImpregnateTime);
      var1.putFloat(this.getHealth());
      var1.putDouble(this.virtualID);
      GameWindow.WriteString(var1, this.migrationGroup);
      var1.putInt(this.getData().clutchSize);
      if (this.isOnHook() && this.hook != null && this.hook.getSquare() != null) {
         var1.put((byte)1);
         var1.putInt(this.hook.getSquare().getX());
         var1.putInt(this.hook.getSquare().getY());
         var1.putInt(this.hook.getSquare().getZ());
         PrintStream var10000 = System.out;
         String var10001 = this.getTypeAndBreed();
         var10000.println("save reattach " + var10001 + " pos: " + this.hook.getSquare().getX() + "," + this.hook.getSquare().getY());
      } else {
         var1.put((byte)0);
      }

   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      this.setIsAnimal(true);
      UUID var4 = new UUID(var1.getLong(), var1.getLong());
      this.m_animalZone = var4.getMostSignificantBits() == 0L && var4.getLeastSignificantBits() == 0L ? null : (AnimalZone)IsoWorld.instance.MetaGrid.animalZoneHandler.getZone(var4);
      this.setX(this.setLastX(this.setNextX(this.setScriptNextX(var1.getFloat() + (float)(IsoWorld.saveoffsetx * 300)))));
      this.setY(this.setLastY(this.setNextY(this.setScriptNextY(var1.getFloat() + (float)(IsoWorld.saveoffsety * 300)))));
      this.setZ(this.setLastZ(var1.getFloat()));
      this.dir = IsoDirections.fromIndex(var1.getInt());
      this.getStats().load(var1, var2);
      this.type = GameWindow.ReadString(var1);
      this.adef = AnimalDefinitions.getDef(this.type);
      AnimalBreed var5 = this.adef.getBreedByName(GameWindow.ReadString(var1));
      String var6 = GameWindow.ReadString(var1);
      if (!StringUtils.isNullOrEmpty(var6)) {
         this.customName = var6;
      }

      if (this.table == null) {
         this.table = LuaManager.platform.newTable();
      }

      this.table.load(var1, var2);
      this.itemID = var1.getInt();
      this.init(var5);
      this.getDescriptor().setFemale(var1.get() == 1);
      this.animalID = var1.getInt();
      int var7 = var1.getInt();

      int var8;
      for(var8 = 0; var8 < var7; ++var8) {
         AnimalGene var9 = new AnimalGene();
         var9.load(var1, var2, var3);
         this.fullGenome.put(var9.name, var9);
      }

      AnimalGene.checkGeneticDisorder(this);
      var8 = 0;
      int var12 = 0;
      if (var1.get() == 1) {
         var8 = var1.getInt();
         var12 = var1.getInt();
      }

      this.attachBackToTreeX = var8;
      this.attachBackToTreeY = var12;
      this.getData().setAge(var1.getInt());
      this.setHoursSurvived(var1.getDouble());
      this.timeSinceLastUpdate = var1.getLong();
      this.data.setSize(var1.getFloat());
      this.attachBackToMother = var1.getInt();
      if (var1.get() == 1) {
         this.attachBackToMother = var1.getInt();
      }

      this.getData().pregnant = var1.get() == 1;
      if (this.getData().pregnant) {
         this.getData().pregnantTime = var1.getInt();
      }

      this.getData().canHaveMilk = var1.get() == 1;
      this.getData().setMilkQuantity(var1.getFloat());
      this.getData().maxMilkActual = var1.getFloat();
      this.milkRemoved = var1.getInt();
      this.getData().setPreferredHutchPosition(var1.get());
      if (this.getBreed().woolType != null && this.getData().getMaxWool() > 0.0F) {
         this.getData().woolQty = var1.getFloat();
      }

      this.getData().fertilizedTime = var1.getInt();
      this.getData().fertilized = var1.get() == 1;
      if (this.adef.eggsPerDay > 0) {
         this.getData().eggsToday = var1.getInt();
      }

      this.stressLevel = var1.getFloat();
      int var10 = var1.getInt();

      for(int var11 = 0; var11 < var10; ++var11) {
         this.playerAcceptanceList.put(var1.getShort(), var1.getFloat());
      }

      this.getData().weight = var1.getFloat();
      this.getData().lastPregnancyTime = var1.getLong();
      this.getData().lastMilkTimer = var1.getLong();
      this.getData().lastImpregnateTime = var1.getInt();
      this.setHealth(var1.getFloat());
      this.virtualID = var1.getDouble();
      this.migrationGroup = GameWindow.ReadString(var1);
      this.getData().clutchSize = var1.getInt();
      this.setOnHook(var1.get() == 1);
      if (this.isOnHook()) {
         this.attachBackToHookX = var1.getInt();
         this.attachBackToHookY = var1.getInt();
         this.attachBackToHookZ = var1.getInt();
         PrintStream var10000 = System.out;
         String var10001 = this.getTypeAndBreed();
         var10000.println("load reattach " + var10001 + " pos: " + this.attachBackToHookX + "," + this.attachBackToHookY);
      }

   }

   public void init(AnimalBreed var1) {
      this.adef = AnimalDefinitions.getDef(this.getAnimalType());
      if (this.adef != null) {
         this.advancedAnimator.SetAnimSet(AnimationSet.GetAnimationSet(this.GetAnimSetName(), false));
         this.initializeStates();
         this.initType(var1);
         this.animalID = Rand.Next(10000);
         this.InitSpriteParts(this.descriptor);
         this.setTurnDelta(this.adef.turnDelta);
         this.initAge();
         AnimalGene.initGenome(this);
         AnimalGene.checkGeneticDisorder(this);
         this.getData().init();
         this.initTexture();
         this.setStateEventDelayTimer(this.getBehavior().pickRandomWanderInterval());
         this.initStress();
         this.wild = this.adef.wild;
         if (GameServer.bServer) {
            AnimalInstanceManager.getInstance().add(this, AnimalInstanceManager.getInstance().allocateID());
         }

         this.getNetworkCharacterAI().getAnimalPacket().reset(this);
      }
   }

   private void initStress() {
      AnimalAllele var1 = this.getUsedGene("stress");
      float var2 = Rand.Next(0.01F, 0.15F);
      if (var1 == null) {
         this.stressLevel = var2;
      }

      float var3 = 1.0F;
      if (var1 != null) {
         var3 = 1.0F - var1.currentValue + 1.0F;
      }

      if (!this.isBaby()) {
         var2 = (float)((double)var2 + GameTime.getInstance().getWorldAgeDaysSinceBegin() * 0.004999999888241291);
      }

      this.stressLevel = var3 * var2 * 100.0F;
      this.stressLevel = Math.min(70.0F, this.stressLevel);
   }

   private void initTexture() {
      if (this.shouldBeSkeleton()) {
         this.getAnimalVisual().setSkinTextureName(this.adef.textureSkeleton);
      } else if (!StringUtils.isNullOrEmpty(this.adef.textureSkinned) && ((KahluaTableImpl)this.getModData()).rawgetBool("skinned")) {
         this.getAnimalVisual().setSkinTextureName(this.adef.textureSkinned);
      } else {
         if (!StringUtils.isNullOrEmpty(this.getData().currentStage.nextStage) && !StringUtils.isNullOrEmpty(this.getBreed().textureBaby)) {
            this.getAnimalVisual().setSkinTextureName(this.getBreed().textureBaby);
         } else if (this.getDescriptor().isFemale()) {
            this.getAnimalVisual().setSkinTextureName((String)this.getBreed().texture.get(Rand.Next(0, this.getBreed().texture.size())));
         } else {
            this.getAnimalVisual().setSkinTextureName(this.getBreed().textureMale);
         }

      }
   }

   private void initAge() {
      this.getData().setAge(1);
      if (this.adef.minAge > 0) {
         this.getData().setAge(this.adef.minAge);
      }

      this.setHoursSurvived((double)(this.getData().getAge() * 24));
   }

   public boolean canGoThere(IsoGridSquare var1) {
      if (var1.isBlockedTo(this.getCurrentSquare())) {
         return false;
      } else if (var1.isWindowTo(this.getCurrentSquare())) {
         return false;
      } else if (this.getData().getAttachedPlayer() != null && var1.DistTo(this.getData().getAttachedPlayer().getCurrentSquare()) > 6.0F) {
         return false;
      } else {
         return this.getData().getAttachedTree() == null || !(var1.DistTo(this.getData().getAttachedTree().getSquare()) > 6.0F);
      }
   }

   public String getAnimalType() {
      return this.type;
   }

   public float getAnimalSize() {
      return this.getData().getSize();
   }

   public void setAgeDebug(int var1) {
      this.getData().setAge(Math.max(var1, this.adef.minAge));
      this.setHoursSurvived((double)(this.getData().getAge() * 24));
      this.getData().init();
   }

   public boolean haveEnoughMilkToFeedFrom() {
      return (double)this.getData().getMilkQuantity() >= 0.02;
   }

   public IsoAnimal addBaby() {
      DebugLog.Animal.debugln("Adding baby from mother: " + this.getFullName());
      if (this.adef.babyType == null) {
         return null;
      } else {
         AnimalDefinitions var1 = AnimalDefinitions.getDef(this.adef.babyType);
         IsoAnimal var2 = new IsoAnimal(this.getCell(), (int)this.getX(), (int)this.getY(), (int)this.getZ(), this.adef.babyType, var1.getBreedByName(this.getBreed().getName()));
         var2.fullGenome = AnimalGene.initGenesFromParents(this.fullGenome, this.getData().maleGenome);
         AnimalGene.checkGeneticDisorder(var2);
         var2.getData().initSize();
         var2.addToWorld();
         DebugLog.Animal.debugln("Baby added: " + var2.getFullName());
         if (var2.geneticDisorder.contains("dieatbirth")) {
            DebugLog.Animal.debugln("Baby died at birth");
            var2.setHealth(0.0F);
         }

         this.getData().updateLastPregnancyTime();
         this.getData().maleGenome = null;
         this.getData().setFertilized(false);
         if (this.adef.udder) {
            this.getData().setCanHaveMilk(true);
            this.getData().updateLastTimeMilked();
         }

         var2.setMother(this);
         var2.motherID = this.animalID;
         var2.setIsInvincible(this.isInvincible());
         return var2;
      }
   }

   private void initType(AnimalBreed var1) {
      this.behavior = new BaseAnimalBehavior(this);
      this.data = new AnimalData(this, var1);
      this.getActionContext().setGroup(ActionGroup.getActionGroup(this.adef.animset));
   }

   public void unloaded() {
      this.timeSinceLastUpdate = GameTime.getInstance().getCalender().getTimeInMillis();
      if (this.getData().getAttachedTree() != null) {
         this.attachBackToTreeX = (int)this.getData().getAttachedTree().getX();
         this.attachBackToTreeY = (int)this.getData().getAttachedTree().getY();
      }

      this.getData().setAttachedTree((IsoObject)null);
      this.getAnimalSoundState("voice").setDesiredSoundName((String)null);
      this.getAnimalSoundState("voice").setDesiredSoundPriority(0);
      this.getAnimalSoundState("voice").stop();
   }

   public void updateLastTimeSinceUpdate() {
      this.timeSinceLastUpdate = GameTime.getInstance().getCalender().getTimeInMillis();
   }

   public void debugAgeAway(int var1) {
      long var2 = (long)var1 * 3600000L;
      long var4 = GameTime.getInstance().getCalender().getTimeInMillis();
      var4 -= var2;
      long var6 = GameTime.getInstance().getCalender().getTimeInMillis() - var4;
      int var8 = this.getData().getAge();
      int var9 = (int)(var6 / 3600000L);
      int var10 = var9 / 24;
      var10 = (int)((float)var10 * this.getData().getAgeGrowModifier());
      int var10000 = var8 + var10;
      this.setHoursSurvived(this.getHoursSurvived() + (double)var9);
      this.getData().lastHourCheck = GameTime.getInstance().getHour();
      Calendar var12 = Calendar.getInstance();
      var12.setTimeInMillis(var4);
      int var13 = var12.get(11);

      for(int var14 = 0; var14 < var9; ++var14) {
         this.getData().hourGrow(true);
         var4 += 3600000L;
         var12.setTimeInMillis(var4);
         var13 = var12.get(11);
         if (this.checkKilledByMetaPredator(var13)) {
            return;
         }
      }

   }

   public void updateStatsAway(int var1) {
      this.fromMeta = false;
      if (!this.isWild()) {
         this.zoneCheckTimer = 0.0F;
         this.checkZone();
         int var2 = this.getData().getAge();
         int var3 = var1 / 24;
         var3 = (int)((float)var3 * this.getData().getAgeGrowModifier());
         int var4 = var2 + var3;
         this.setHoursSurvived((double)(var4 * 24));
         this.getData().lastHourCheck = GameTime.getInstance().getHour();
         this.getData().setAge(var4);
         PZCalendar var5 = PZCalendar.getInstance();
         var5.setTimeInMillis(this.timeSinceLastUpdate);
         int var6 = var5.get(11);

         for(int var7 = 0; var7 < var1; ++var7) {
            this.getData().hourGrow(true);
            this.timeSinceLastUpdate += 3600000L;
            var5.setTimeInMillis(this.timeSinceLastUpdate);
            var6 = var5.get(11);
            this.getData().tryInseminateInMeta(var5);
            if (var6 == 0) {
               this.getData().growUp(true);
            }

            this.getData().checkEggs(var5, true);
            if (this.checkKilledByMetaPredator(var6)) {
               return;
            }
         }

      }
   }

   public boolean checkKilledByMetaPredator(int var1) {
      if (!SandboxOptions.instance.AnimalMetaPredator.getValue()) {
         return false;
      } else if (this.adef.enterHutchTime == 0 && this.adef.exitHutchTime == 0) {
         return false;
      } else {
         if (this.adef.isInsideHutchTime(var1)) {
            short var2 = 90;
            if (this.isBaby()) {
               var2 = 45;
            }

            if (!this.isFemale()) {
               var2 = 150;
            }

            if (Rand.NextBool(var2)) {
               int var4;
               if (this.hutch != null && !this.hutch.isDoorClosed()) {
                  this.hutch.killAnimal(this);
                  this.hutch.setHutchDirt(this.hutch.getHutchDirt() + (float)Rand.Next(10, 20));
                  IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare((double)(this.hutch.getSquare().x + this.hutch.getEnterSpotX()), (double)(this.hutch.getSquare().y + this.hutch.getEnterSpotY()), (double)this.hutch.getZ());
                  if (var5 != null) {
                     for(var4 = 0; var4 < 20; ++var4) {
                        var5.getChunk().addBloodSplat((float)var5.x + Rand.Next(-0.8F, 0.8F), (float)var5.y + Rand.Next(-0.8F, 0.8F), this.hutch.getZ(), Rand.Next(8));
                     }
                  }
               } else {
                  byte var3 = 20;
                  if (this.getCurrentSquare() != null) {
                     for(var4 = 0; var4 < var3; ++var4) {
                        this.getCurrentSquare().getChunk().addBloodSplat((float)this.getCurrentSquare().x + Rand.Next(-0.8F, 0.8F), (float)this.getCurrentSquare().y + Rand.Next(-0.8F, 0.8F), (float)this.getCurrentSquare().z, Rand.Next(8));
                     }
                  }

                  this.hitConsequences((HandWeapon)null, (IsoGameCharacter)null, false, 666.0F, false);
               }

               return true;
            }
         }

         return false;
      }
   }

   public boolean isBaby() {
      return this.getData().currentStage.nextStage != null;
   }

   public boolean shearAnimal(IsoGameCharacter var1, InventoryItem var2) {
      if (!(this.getData().woolQty < 1.0F) && !var2.isBroken()) {
         if (var2 instanceof DrainableComboItem && ((DrainableComboItem)var2).getCurrentUsesFloat() <= 0.0F) {
            return false;
         } else {
            this.getData().setWoolQuantity(this.getData().woolQty - 1.0F);
            InventoryItem var3 = InventoryItemFactory.CreateItem(this.getBreed().woolType);
            this.getCurrentSquare().AddWorldInventoryItem(var3, Rand.Next(0.0F, 0.8F), Rand.Next(0.0F, 0.8F), 0.0F);
            if (Rand.NextBool((int)var2.getConditionLowerNormal())) {
               var2.setCondition(var2.getCondition() - 1);
            }

            if (var2 instanceof DrainableComboItem) {
               var2.Use();
            }

            if (GameServer.bServer) {
               GameServer.addXp((IsoPlayer)var1, PerkFactory.Perks.Husbandry, (float)Rand.Next(2, 5));
            } else if (!GameClient.bClient) {
               var1.getXp().AddXP(PerkFactory.Perks.Husbandry, (float)Rand.Next(2, 5));
            }

            if (var1.getPerkLevel(PerkFactory.Perks.Husbandry) <= 5 && Rand.NextBool(var1.getPerkLevel(PerkFactory.Perks.Husbandry) + 3)) {
               this.changeStress((float)(Rand.Next(6 - var1.getPerkLevel(PerkFactory.Perks.Husbandry) * 2, 6 - var1.getPerkLevel(PerkFactory.Perks.Husbandry) * 6) / 8));
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public String getMilkType() {
      return this.getBreed().milkType;
   }

   public InventoryItem addDebugBucketOfMilk(IsoGameCharacter var1) {
      InventoryItem var2 = InventoryItemFactory.CreateItem("Base.BucketEmpty");
      var2.getFluidContainer().addFluid(this.getBreed().getMilkType(), var2.getFluidContainer().getCapacity());
      return var2;
   }

   public InventoryItem milkAnimal(IsoGameCharacter var1, InventoryItem var2) {
      if (this.data.milkQty < 0.1F) {
         return null;
      } else if (var2.getFluidContainer().isFull()) {
         return null;
      } else {
         this.getData().updateLastTimeMilked();
         this.getData().canHaveMilk = true;
         float var3 = var2.getFluidContainer().getAmount();
         var2.getFluidContainer().addFluid(this.getBreed().getMilkType(), 0.1F);
         var3 = var2.getFluidContainer().getAmount() - var3;
         this.getData().setMilkQuantity(this.data.milkQty - var3);
         ++this.milkRemoved;
         if (GameServer.bServer) {
            GameServer.addXp((IsoPlayer)var1, PerkFactory.Perks.Husbandry, (float)Rand.Next(2, 5));
         } else if (!GameClient.bClient) {
            var1.getXp().AddXP(PerkFactory.Perks.Husbandry, (float)Rand.Next(2, 5));
         }

         if (var1.getPerkLevel(PerkFactory.Perks.Husbandry) <= 5 && Rand.NextBool(var1.getPerkLevel(PerkFactory.Perks.Husbandry) + 3)) {
            this.changeStress((float)(Rand.Next(6 - var1.getPerkLevel(PerkFactory.Perks.Husbandry) * 2, 6 - var1.getPerkLevel(PerkFactory.Perks.Husbandry) * 6) / 8));
         }

         if (this.milkRemoved >= 50) {
            this.milkRemoved = 0;
            this.getData().maxMilkActual = Math.min(this.getData().maxMilkActual + this.getData().getMaxMilk() * 0.001F, this.getData().getMaxMilk() * 1.3F);
         }

         return var2;
      }
   }

   public void setMaxSizeDebug() {
      this.setAgeDebug(this.getData().getAge() + this.getData().currentStage.getAgeToGrow(this) - 3);
   }

   public boolean addEgg(boolean var1) {
      if (this.geneticDisorder.contains("noeggs")) {
         return false;
      } else if (this.hutch != null) {
         return this.hutch.addAnimalInNestBox(this);
      } else {
         Food var2;
         if (this.getVehicle() != null) {
            var2 = this.createEgg();
            BaseVehicle var7 = this.getVehicle();
            VehiclePart var8 = var7.getPartById("TrailerAnimalEggs");
            if (var8 != null && var8.getItemContainer() != null) {
               if (var8.getItemContainer().getContentsWeight() >= (float)var8.getItemContainer().getCapacity()) {
                  VehiclePart var5 = var7.getPartById("TrailerAnimalEggs");
                  if (var5 != null && var5.getItemContainer() != null && !(var5.getItemContainer().getContentsWeight() >= (float)var5.getItemContainer().getCapacity())) {
                     var5.getItemContainer().addItem(var2);
                  } else {
                     InventoryItem var6 = var7.getSquare().AddWorldInventoryItem((InventoryItem)var2, Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
                     IsoWorld.instance.getCell().addToProcessItems(var6);
                     IsoWorld.instance.getCell().addToProcessItems((InventoryItem)var2);
                  }
               } else {
                  var8.getItemContainer().addItem(var2);
               }

               return true;
            } else {
               return false;
            }
         } else {
            var2 = this.createEgg();
            IsoGridSquare var3 = null;
            if (this.getContainer() != null) {
               this.getContainer().addItem(var2);
            } else if (var1 && !this.connectedDZone.isEmpty()) {
               var3 = this.getRandomSquareInZone();
            } else if (var3 == null) {
               var3 = this.getSquare();
            }

            if (var3 != null) {
               InventoryItem var4 = this.getSquare().AddWorldInventoryItem((InventoryItem)var2, Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
               IsoWorld.instance.getCell().addToProcessItems(var4);
               IsoWorld.instance.getCell().addToProcessItems((InventoryItem)var2);
            }

            return true;
         }
      }
   }

   public Food createEgg() {
      Food var1 = (Food)InventoryItemFactory.CreateItem(this.adef.eggType);
      if (var1 == null) {
         DebugLog.Animal.debugln("Error while creating egg: " + this.adef.eggType + " isn't a valid item.");
         return null;
      } else {
         float var2 = this.getEggGeneMod();
         float var3 = 1.0F + var2 / 2.0F;
         float var4 = 0.07F;
         var4 *= var3;
         var4 -= var4 * 2.0F;
         if (this.getData().fertilized) {
            var1.setFertilized(true);
            var1.motherID = this.animalID;
            var1.setTimeToHatch(this.adef.timeToHatch);
            var1.setAnimalHatch(this.adef.babyType);
            var1.setBaseHunger(var4);
            var1.setHungChange(var4);
            var1.setAnimalHatchBreed(this.getBreed().getName());
            var1.eggGenome = AnimalGene.initGenesFromParents(this.fullGenome, this.getData().maleGenome);
         }

         if (this.getData().clutchSize > 0) {
            --this.getData().clutchSize;
         }

         return var1;
      }
   }

   public void randomizeAge() {
      this.setAgeDebug(Rand.Next(this.adef.minAgeForBaby, this.adef.minAgeForBaby + this.getData().currentStage.getAgeToGrow(this) - 5));
   }

   public boolean isAnimalMoving() {
      return this.getCurrentState() == AnimalWalkState.instance() || this.getCurrentState() == AnimalPathFindState.instance() || this.getCurrentState() == AnimalFollowWallState.instance();
   }

   public boolean isGeriatric() {
      if (this.adef.maxAgeGeriatric <= 0) {
         return false;
      } else {
         float var1 = this.getData().getMaxAgeGeriatric() - (float)this.adef.minAge;
         return (double)this.getData().getDaysSurvived() > (double)var1 * 0.8;
      }
   }

   public String getAgeText(boolean var1, int var2) {
      String var3 = "";
      String var4 = "";
      int var10000;
      if (this.isBaby()) {
         ArrayList var5 = this.getData().getGrowStage();
         AnimalGrowStage var6 = null;
         if (var5 != null && !var5.isEmpty()) {
            for(int var7 = 0; var7 < var5.size(); ++var7) {
               AnimalGrowStage var8 = (AnimalGrowStage)var5.get(var7);
               if (var8.stage.equals(this.getAnimalType()) && !StringUtils.isNullOrEmpty(var8.nextStage)) {
                  var6 = var8;
                  break;
               }
            }
         }

         if (var6 != null) {
            if (this.getData().getDaysSurvived() < var6.getAgeToGrow(this) / 2) {
               var3 = Translator.getText("IGUI_Animal_Baby");
               var10000 = var6.getAgeToGrow(this);
               var4 = "/ " + var10000 / 2;
            } else {
               var3 = Translator.getText("IGUI_Animal_Juvenile");
               var10000 = var6.getAgeToGrow(this);
               var4 = "/ " + var10000;
            }

            if (var2 < 4 && !Core.getInstance().animalCheat) {
               var3 = Translator.getText("IGUI_Animal_Juvenile");
            }
         }
      } else {
         float var9 = this.getData().getMaxAgeGeriatric() - (float)this.adef.minAge;
         if (this.getData().getDaysSurvived() < this.getMinAgeForBaby()) {
            var3 = Translator.getText("IGUI_Animal_JuvenileWeaned");
            var4 = "/ " + this.getMinAgeForBaby();
         } else {
            var3 = Translator.getText("IGUI_Animal_Adolescent");
            var4 = "/ " + this.adef.minAge * 2;
         }

         if (this.getData().getDaysSurvived() > this.adef.minAge * 2) {
            if (this.isGeriatric()) {
               var3 = Translator.getText("IGUI_Animal_Geriatric");
            } else {
               var3 = Translator.getText("IGUI_Animal_Adult");
            }

            var4 = "/ " + Float.valueOf(var9 * 0.8F).intValue();
         }

         if (var2 < 4 && !Core.getInstance().animalCheat) {
            var3 = Translator.getText("IGUI_Animal_Adult");
         }
      }

      if (var1) {
         var10000 = this.getData().getDaysSurvived();
         var4 = " ( " + var10000 + var4 + ")";
      } else {
         var4 = "";
      }

      return var3 + var4;
   }

   public String getHealthText(boolean var1, int var2) {
      String var3 = "";
      String var4 = "";
      if (var1) {
         float var10000 = this.getHealth();
         var4 = " (" + PZMath.roundFloat(var10000, 2) + ")";
         if (this.isGeriatric()) {
            var4 = var4 + " losing health due to age.";
         }
      }

      if ((double)this.getHealth() > 0.8) {
         var3 = Translator.getText("IGUI_Animal_Healthy");
      } else if ((double)this.getHealth() > 0.55) {
         var3 = Translator.getText("IGUI_Animal_OffColor");
      } else if ((double)this.getHealth() > 0.3) {
         var3 = Translator.getText("IGUI_Animal_Sickly");
      } else {
         var3 = Translator.getText("IGUI_Animal_Dying");
      }

      if (var2 < 5) {
         var3 = Translator.getText("IGUI_Animal_Healthy");
         if ((double)this.getHealth() < 0.7) {
            var3 = Translator.getText("IGUI_Animal_Sickly");
         }
      }

      return var3 + var4;
   }

   public String getAppearanceText(boolean var1) {
      String var3 = "";
      DecimalFormat var4 = new DecimalFormat("###.##");
      if (var1) {
         String var10000 = var4.format((double)this.getHunger());
         var3 = " (" + var10000;
      }

      String var2;
      if ((double)this.getHunger() < 0.3) {
         var2 = Translator.getText("IGUI_Animal_WellFed");
      } else if ((double)this.getHunger() < 0.6) {
         var2 = Translator.getText("IGUI_Animal_Underfed");
      } else {
         var2 = Translator.getText("IGUI_Animal_Starving");
      }

      if (var1) {
         var3 = var3 + "," + var4.format((double)this.getThirst()) + ")";
      }

      String var5;
      if ((double)this.getThirst() < 0.3) {
         var5 = Translator.getText("IGUI_Animal_FullyWatered");
      } else if ((double)this.getThirst() < 0.6) {
         var5 = Translator.getText("IGUI_Animal_Thirsty");
      } else {
         var5 = Translator.getText("IGUI_Animal_DyingThirst");
      }

      return var2 + ", " + var5 + var3;
   }

   public void copyFrom(IsoAnimal var1) {
      this.setHoursSurvived(var1.getHoursSurvived());
      this.getStats().hunger = var1.getStats().hunger;
      this.getStats().thirst = var1.getStats().thirst;
      this.customName = var1.customName;
      this.stressLevel = var1.stressLevel;
      this.playerAcceptanceList = var1.playerAcceptanceList;
      this.setHealth(var1.getHealth());
      this.animalID = var1.animalID;
      this.fullGenome = var1.fullGenome;
      this.geneticDisorder = var1.geneticDisorder;
      this.attachBackToMother = var1.attachBackToMother;
      this.data = var1.getData();
      this.setFemale(var1.isFemale());
      this.data.parent = this;
      this.wild = var1.wild;
   }

   public void fertilize(IsoAnimal var1, boolean var2) {
      if (!var2) {
         float var3 = var1.getUsedGene("fertility").currentValue;
         if (var1.geneticDisorder.contains("poorfertility")) {
            var3 = 1.0F;
         }

         if (var1.geneticDisorder.contains("fertile")) {
            var3 = 100.0F;
         }

         if (var1.geneticDisorder.contains("sterile")) {
            var3 = 0.0F;
         }

         var3 *= 1.0F - var1.getData().getGeriatricPercentage();
         float var4 = this.getUsedGene("fertility").currentValue;
         if (this.geneticDisorder.contains("poorfertility")) {
            var4 = 1.0F;
         }

         if (this.geneticDisorder.contains("fertile")) {
            var4 = 100.0F;
         }

         if (this.geneticDisorder.contains("sterile")) {
            var4 = 0.0F;
         }

         var4 *= 1.0F - this.getData().getGeriatricPercentage();
         float var5 = var3 * var4 * 100.0F;
         if (var1 != null) {
            var1.getData().lastImpregnateTime = 24;
            var1.getData().animalToInseminate = new ArrayList();
         }

         if ((float)Rand.Next(100) > var5) {
            return;
         }
      }

      if (var1 == null) {
         this.getData().maleGenome = this.fullGenome;
      } else {
         this.getData().maleGenome = var1.fullGenome;
      }

      if (this.adef.eggsPerDay > 0) {
         this.getData().fertilized = true;
         this.getData().fertilizedTime = 1;
      } else {
         this.getData().pregnantTime = 0;
         this.getData().pregnant = true;
      }

   }

   public boolean isAnimalEating() {
      return "eat".equals(this.getVariableString("idleAction"));
   }

   public boolean isAnimalAttacking() {
      return this.atkTarget != null || this.adef.canThump && this.thumpTarget != null;
   }

   public boolean isAnimalSitting() {
      return "sit".equals(this.getVariableString("idleAction"));
   }

   public boolean isFemale() {
      return this.getDescriptor().isFemale();
   }

   public void setFemale(boolean var1) {
      this.getDescriptor().setFemale(var1);
   }

   public IsoGameCharacter getAttackedBy() {
      return this.attackedBy;
   }

   public void setAttackedBy(IsoGameCharacter var1) {
      this.attackedBy = var1;
   }

   public boolean isInvincible() {
      return this.invincible;
   }

   public void setIsInvincible(boolean var1) {
      this.invincible = var1;
   }

   public String getCustomName() {
      return this.customName;
   }

   public void setCustomName(String var1) {
      this.customName = var1;
   }

   public float getHunger() {
      return this.getStats().hunger;
   }

   public float getThirst() {
      return this.getStats().thirst;
   }

   public String getBabyType() {
      return this.adef.babyType;
   }

   public boolean hasUdder() {
      return this.adef.udder;
   }

   public AnimalBreed getBreed() {
      return this.getData().getBreed();
   }

   public boolean canBeMilked() {
      return this.adef.canBeMilked;
   }

   public boolean canBeSheared() {
      if (this.getBreed().woolType == null) {
         return false;
      } else {
         return this.getData().getMaxWool() > 0.0F;
      }
   }

   public int getEggsPerDay() {
      return this.adef.eggsPerDay;
   }

   public IsoHutch getHutch() {
      return this.hutch;
   }

   public void setData(AnimalData var1) {
      this.data = var1;
   }

   public boolean hasGeneticDisorder(String var1) {
      return this.geneticDisorder.contains(var1);
   }

   public String getFullName() {
      String var1;
      if (!StringUtils.isNullOrEmpty(this.customName)) {
         var1 = "";
         if (this.isWild()) {
            var1 = var1 + " (" + Translator.getText("IGUI_Animal_Wild") + ")";
         }

         return this.customName + var1;
      } else {
         var1 = Translator.getText("IGUI_AnimalType_" + this.getAnimalType());
         if (this.getData().getBreed() != null) {
            String var10000 = Translator.getText("IGUI_Breed_" + this.getData().getBreed().getName());
            var1 = var10000 + " " + var1;
         }

         if (this.isWild()) {
            var1 = var1 + " (" + Translator.getText("IGUI_Animal_Wild") + ")";
         }

         return var1;
      }
   }

   public HashMap<String, AnimalGene> getFullGenome() {
      return this.fullGenome;
   }

   public ArrayList<AnimalGene> getFullGenomeList() {
      ArrayList var1 = new ArrayList();
      Iterator var2 = this.fullGenome.keySet().iterator();

      while(var2.hasNext()) {
         String var3 = (String)var2.next();
         AnimalGene var4 = (AnimalGene)this.fullGenome.get(var3);
         var1.add(var4);
      }

      return var1;
   }

   public AnimalAllele getUsedGene(String var1) {
      var1 = var1.toLowerCase();
      AnimalGene var2 = (AnimalGene)this.fullGenome.get(var1);
      if (var2 == null) {
         DebugLog.Animal.debugln(var1 + " wasn't found in the full genome of the animal (" + this.getFullName() + ")");
         return null;
      } else {
         return var2.allele1.used ? var2.allele1 : var2.allele2;
      }
   }

   public int getAge() {
      return this.getData().getAge();
   }

   public boolean canDoAction() {
      return !this.isAnimalAttacking() && !this.isAnimalSitting() && !this.getBehavior().blockMovement && !this.isAnimalMoving();
   }

   public float getMeatRatio() {
      AnimalAllele var1 = this.getUsedGene("meatRatio");
      return var1 != null ? var1.currentValue : 1.0F;
   }

   public String getMate() {
      return this.adef.mate;
   }

   public AnimalZone getAnimalZone() {
      return this.m_animalZone;
   }

   public void setAnimalZone(AnimalZone var1) {
      this.m_animalZone = var1;
   }

   public boolean hasAnimalZone() {
      return this.getAnimalZone() != null;
   }

   public boolean isMoveForwardOnZone() {
      return this.m_bMoveForwardOnZone;
   }

   public void setMoveForwardOnZone(boolean var1) {
      this.m_bMoveForwardOnZone = var1;
   }

   public boolean isExistInTheWorld() {
      return this.square != null && this.square.getMovingObjects().contains(this);
   }

   public void changeStress(float var1) {
      if (!this.isInvincible() || !(var1 < 0.0F)) {
         AnimalAllele var2 = this.getUsedGene("stress");
         if (var2 != null) {
            if (var1 > 0.0F) {
               var1 *= 1.0F + var2.currentValue;
            } else {
               var1 *= var2.currentValue;
            }
         }

         if (this.geneticDisorder.contains("highstress")) {
            if (var1 < 0.0F) {
               var1 *= 10.0F;
            } else {
               var1 /= 10.0F;
            }
         }

         this.stressLevel += var1;
         this.stressLevel = Math.min(100.0F, Math.max(0.0F, this.stressLevel));
      }
   }

   public float getEggGeneMod() {
      AnimalAllele var1 = this.getUsedGene("eggSize");
      if (var1 != null) {
         float var2 = var1.currentValue;
         if (this.geneticDisorder.contains("smalleggs")) {
            var2 = 0.1F;
         }

         return var2;
      } else {
         return 1.0F;
      }
   }

   public void setDebugStress(float var1) {
      this.stressLevel = var1;
   }

   public void setDebugAcceptance(IsoPlayer var1, float var2) {
      this.playerAcceptanceList.put(var1.getOnlineID(), var2);
   }

   public ArrayList<InventoryItem> getAllPossibleFoodFromInv(IsoGameCharacter var1) {
      ArrayList var2 = new ArrayList();
      ArrayList var3 = this.getEatTypePossibleFromHand();
      ArrayList var4 = var1.getInventory().getAllFoodsForAnimals();

      for(int var5 = 0; var5 < var4.size(); ++var5) {
         InventoryItem var6 = (InventoryItem)var4.get(var5);
         String var7 = var6.getFullType();
         String var8 = null;
         if (var6 instanceof Food && ((Food)var6).getMilkType() != null) {
            var7 = ((Food)var6).getMilkType();
         }

         if (var6 instanceof Food) {
            var8 = ((Food)var6).getFoodType();
         }

         if (var6.isAnimalFeed()) {
            var7 = var6.getAnimalFeedType();
         }

         if (var3.contains(var7) || var3.contains(var8)) {
            var2.add(var6);
         }
      }

      return var2;
   }

   public ArrayList<String> getEatTypePossibleFromHand() {
      ArrayList var1 = new ArrayList();
      if (this.adef.feedByHandType != null) {
         var1.addAll(this.adef.feedByHandType);
      }

      if (this.adef.eatTypeTrough != null) {
         var1.addAll(this.adef.eatTypeTrough);
      }

      if (this.isBaby() && !StringUtils.isNullOrEmpty(this.getBreed().milkType)) {
         var1.add(this.getBreed().milkType);
      }

      return var1;
   }

   public void addAcceptance(IsoPlayer var1, float var2) {
      if (this.playerAcceptanceList.get(var1.getOnlineID()) == null) {
         this.playerAcceptanceList.put(var1.getOnlineID(), Rand.Next(0.0F, 20.0F));
      }

      float var3 = (Float)this.playerAcceptanceList.get(var1.getOnlineID());
      var3 += var2;
      var3 *= (float)(1 + var1.getPerkLevel(PerkFactory.Perks.Husbandry) / 10);
      if (var3 > 100.0F) {
         var3 = 100.0F;
      }

      this.playerAcceptanceList.put(var1.getOnlineID(), var3);
   }

   public void feedFromHand(IsoPlayer var1, InventoryItem var2) {
      float var3 = 0.0F;
      float var4 = 0.0F;
      this.getData().eatItem(var2, false);
      if (GameServer.bServer) {
         GameServer.addXp(var1, PerkFactory.Perks.Husbandry, (float)Rand.Next(4, 10));
      } else if (!GameClient.bClient) {
         var1.getXp().AddXP(PerkFactory.Perks.Husbandry, (float)Rand.Next(4, 10));
      }

      this.changeStress((float)(Rand.Next(-4, -1) - var1.getPerkLevel(PerkFactory.Perks.Husbandry) / 5));
      this.setDebugAcceptance(var1, Math.min(100.0F, this.getAcceptanceLevel(var1) + (float)Rand.Next(1, 4)));
   }

   public boolean petTimerDone() {
      return this.petTimer <= 0.0F;
   }

   public void petAnimal(IsoPlayer var1) {
      if (this.petTimer == 0.0F) {
         this.changeStress((float)(Rand.Next(-7, -3) - var1.getPerkLevel(PerkFactory.Perks.Husbandry)));
         this.setDebugAcceptance(var1, Math.min(100.0F, this.getAcceptanceLevel(var1) + (float)Rand.Next(2, 7)));
         this.petTimer = (float)(Rand.Next(15000, 25000) - var1.getPerkLevel(PerkFactory.Perks.Husbandry) * 200);
         if (GameServer.bServer) {
            GameServer.addXp(var1, PerkFactory.Perks.Husbandry, (float)Rand.Next(5, 10));
         } else if (!GameClient.bClient) {
            var1.getXp().AddXP(PerkFactory.Perks.Husbandry, (float)Rand.Next(5, 10));
         }
      }

      var1.petAnimal();
   }

   public float getStress() {
      return this.stressLevel;
   }

   public String getStressTxt(boolean var1, int var2) {
      String var3 = Translator.getText("IGUI_Animal_Calm");
      String var4 = "";
      if (var1) {
         var4 = " (" + PZMath.roundFloat(this.stressLevel, 2) + ")";
      }

      if (this.stressLevel > 40.0F) {
         var3 = Translator.getText("IGUI_Animal_Unnerved");
      }

      if (this.stressLevel > 60.0F) {
         var3 = Translator.getText("IGUI_Animal_Stressed");
      }

      if (this.stressLevel > 80.0F) {
         var3 = Translator.getText("IGUI_Animal_Agitated");
      }

      if (var2 < 4) {
         var3 = Translator.getText("IGUI_Animal_Calm");
         if (this.stressLevel > 40.0F) {
            var3 = Translator.getText("IGUI_Animal_Stressed");
         }
      }

      return var3 + var4;
   }

   public void fleeTo(IsoGridSquare var1) {
      if (this.getData().getAttachedPlayer() != null) {
         this.getData().getAttachedPlayer().getAttachedAnimals().remove(this);
         this.getData().setAttachedPlayer((IsoPlayer)null);
      }

      this.getData().setAttachedTree((IsoObject)null);
      this.setVariable("animalRunning", true);
      this.pathToLocation(var1.x, var1.y, var1.z);
   }

   public float getAcceptanceLevel(IsoPlayer var1) {
      if (this.playerAcceptanceList.get(var1.getOnlineID()) == null) {
         this.playerAcceptanceList.put(var1.getOnlineID(), Rand.Next(0.0F, 20.0F));
      }

      return (Float)this.playerAcceptanceList.get(var1.getOnlineID());
   }

   public boolean canBeFeedByHand() {
      return this.adef.canBeFeedByHand;
   }

   public void tryLure(IsoPlayer var1, InventoryItem var2) {
      if (this.luredBy == null) {
         if (this.CanSee(var1)) {
            if (this.getPossibleLuringItems(var1).contains(var2)) {
               float var3 = 100.0F - (this.getAcceptanceLevel(var1) + 20.0F);
               var3 *= 1.0F + (float)var1.getPerkLevel(PerkFactory.Perks.Husbandry) / 10.0F;
               if ((float)Rand.Next(100) > var3) {
                  if (this.getStress() > 40.0F && Rand.NextBool(5)) {
                     return;
                  }

                  if (this.getStress() > 60.0F && Rand.NextBool(3)) {
                     return;
                  }

                  if (this.getStress() > 80.0F) {
                     return;
                  }

                  DebugLog.DetailedInfo.trace("Animal id=%d lured by player \"%s\"", this.getOnlineID(), var1.getUsername());
                  var1.luredAnimals.add(this);
                  this.luredBy = var1;
                  this.luredStartTimer = (float)Rand.Next(100, 200);
                  if (GameServer.bServer) {
                     GameServer.addXp(var1, PerkFactory.Perks.Husbandry, (float)Rand.Next(5, 10));
                  } else if (!GameClient.bClient) {
                     var1.getXp().AddXP(PerkFactory.Perks.Husbandry, (float)Rand.Next(5, 10));
                  }
               }

            }
         }
      }
   }

   public ArrayList<InventoryItem> getPossibleLuringItems(IsoGameCharacter var1) {
      return this.getAllPossibleFoodFromInv(var1);
   }

   public void eatFromLured(IsoPlayer var1, InventoryItem var2) {
      this.cancelLuring();
      boolean var3 = false;
      if (var2 != null) {
         this.getData().eatItem(var2, false);
         this.setVariable("idleAction", "eat");
         this.faceThisObject(var1);
         this.addAcceptance(var1, 5.0F);
         this.getStats().hunger = Math.max(0.0F, Math.min(1.0F, this.getStats().hunger));
      }
   }

   public Position3D getAttachmentWorldPos(String var1) {
      if (this.legsSprite != null && this.legsSprite.modelSlot != null && this.legsSprite.modelSlot.model != null) {
         ModelAttachment var2 = this.legsSprite.modelSlot.model.getAttachmentById(var1);
         if (var2 == null) {
            return null;
         } else {
            Matrix4f var3 = ModelInstanceRenderData.makeAttachmentTransform(var2, BaseVehicle.allocMatrix4f());
            Position3D var4 = new Position3D();
            ModelInstanceRenderData.applyBoneTransform(this.getModelInstance(), var2.getBone(), var3);
            Matrix4f var5 = BaseVehicle.allocMatrix4f();
            var5.translation(this.getX(), this.getZ(), this.getY());
            var5.rotateY(-this.getAnimationPlayer().getRenderedAngle() + 0.0F);
            var5.scale(-1.5F * this.getAnimalSize(), 1.5F * this.getAnimalSize(), 1.5F * this.getAnimalSize());
            var5.mul(var3, var3);
            BaseVehicle.releaseMatrix4f(var5);
            var3.getTranslation(tempVector3f);
            BaseVehicle.releaseMatrix4f(var3);
            tempVector3f.set(tempVector3f.x, tempVector3f.z, tempVector3f.y * 0.4F);
            var4.x = tempVector3f.x;
            var4.y = tempVector3f.y;
            var4.z = tempVector3f.z;
            return var4;
         }
      } else {
         return null;
      }
   }

   public void carCrash(float var1, boolean var2) {
      if (!(this.getHealth() < 0.0F) && !(var1 < 2.0F)) {
         float var3 = 15.0F;
         if (!var2) {
            var3 = 30.0F;
         }

         this.setHealth(this.getHealth() - var1 * this.getData().getHealthLoss(var3));
      }
   }

   public void setDir(IsoDirections var1) {
      this.dir = var1;
      this.getVectorFromDirection(this.m_forwardDirection);
   }

   public String getMilkAnimPreset() {
      return this.adef.milkAnimPreset;
   }

   public void pathToLocation(int var1, int var2, int var3) {
      byte var4 = 15;
      if (this.data.getAttachedPlayer() == null || this.data.getAttachedPlayer().getCurrentSquare() == null || !(this.data.getAttachedPlayer().getCurrentSquare().DistToProper(var1, var2) > (float)var4)) {
         if (this.data.getAttachedTree() == null || this.data.getAttachedTree().getSquare() == null || !(this.data.getAttachedTree().getSquare().DistToProper(var1, var2) > (float)var4)) {
            this.getPathFindBehavior2().pathToLocation(var1, var2, var3);
            this.pathToAux((float)var1, (float)var2, (float)var3);
         }
      }
   }

   public void pathToTrough(IsoFeedingTrough var1) {
      if (var1 != null) {
         IsoGridSquare var2 = IsoWorld.instance.CurrentCell.getGridSquare((double)var1.getX(), (double)(var1.getY() - 1.0F), (double)var1.getZ());
         IsoGridSquare var3 = IsoWorld.instance.CurrentCell.getGridSquare((double)var1.getX(), (double)(var1.getY() + 1.0F), (double)var1.getZ());
         IsoGridSquare var4 = IsoWorld.instance.CurrentCell.getGridSquare((double)(var1.getX() - 1.0F), (double)var1.getY(), (double)var1.getZ());
         IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare((double)(var1.getX() + 1.0F), (double)var1.getY(), (double)var1.getZ());
         float var6 = 0.0F;
         float var7 = 0.0F;
         if (var2 != null && var3 != null && var4 != null && var5 != null) {
            IsoGridSquare var8 = null;
            if (!var1.north) {
               var4 = var2;
               var5 = var3;
               var2 = IsoWorld.instance.CurrentCell.getGridSquare((double)(var1.getX() + 1.0F), (double)var1.getY(), (double)var1.getZ());
               var3 = IsoWorld.instance.CurrentCell.getGridSquare((double)(var1.getX() - 1.0F), (double)var1.getY(), (double)var1.getZ());
            }

            boolean var9 = var2.isFree(false) && !var2.isWallTo(var1.square) && !var2.isWindowTo(var1.square);
            boolean var10 = var3.isFree(false) && !var3.isWallTo(var1.square) && !var3.isWindowTo(var1.square);
            boolean var11 = var4.isFree(false) && !var4.isWallTo(var1.square) && !var4.isWindowTo(var1.square);
            boolean var12 = var5.isFree(false) && !var5.isWallTo(var1.square) && !var5.isWindowTo(var1.square);
            if (var9 && (var2.DistToProper((IsoMovingObject)this) < var3.DistToProper((IsoMovingObject)this) || !var10)) {
               var8 = var2;
               if (!var1.north) {
                  var6 = this.adef.distToEat - 1.0F;
               } else {
                  var7 = this.adef.distToEat - 1.0F;
               }
            }

            if (var10 && (var3.DistToProper((IsoMovingObject)this) < var2.DistToProper((IsoMovingObject)this) || !var9)) {
               var8 = var3;
               if (!var1.north) {
                  var6 = 1.0F - this.adef.distToEat;
               } else {
                  var7 = 1.0F - this.adef.distToEat;
               }
            }

            if (var8 == null) {
               if (var11 && (var4.DistToProper((IsoMovingObject)this) < var5.DistToProper((IsoMovingObject)this) || !var12)) {
                  var8 = var4;
               }

               if (var12 && (var5.DistToProper((IsoMovingObject)this) < var4.DistToProper((IsoMovingObject)this) || !var11)) {
                  var8 = var5;
               }
            }

            if (var8 != null) {
               if (!var8.isFree(false)) {
                  if (this.ignoredTrough.contains(var1)) {
                     this.ignoredTrough.add(var1);
                  }

                  return;
               }

               if (this.adef.distToEat < 1.0F) {
                  this.pathToLocation((int)var1.getX(), (int)var1.getY(), (int)var1.getZ());
               } else {
                  this.pathToLocation(var8.getX(), var8.getY(), var8.getZ());
               }
            }

         }
      }
   }

   public boolean shouldBreakObstaclesDuringPathfinding() {
      if (!this.adef.canThump) {
         return false;
      } else {
         return this.getHunger() > 0.8F || this.getThirst() > 0.8F;
      }
   }

   public float getFeelersize() {
      return 0.8F;
   }

   public boolean animalShouldThump() {
      if (!this.adef.canThump) {
         return false;
      } else if (this.attackedBy != null) {
         return true;
      } else {
         if (!(this.getStats().thirst >= 0.9F) && !(this.getStats().hunger >= 0.9F)) {
            this.thumpDelay = 20000.0F;
         } else {
            if (this.thumpDelay == 0.0F) {
               return true;
            }

            this.thumpDelay -= GameTime.getInstance().getMultiplier();
            if (this.thumpDelay < 0.0F) {
               this.thumpDelay = 0.0F;
            }
         }

         return this.getStress() >= 70.0F;
      }
   }

   public boolean tryThump(IsoGridSquare var1) {
      if (!this.isAnimalAttacking() && this.animalShouldThump()) {
         if (!this.isAnimalMoving()) {
            return false;
         } else {
            IsoGridSquare var2;
            if (var1 != null) {
               var2 = var1;
            } else {
               var2 = this.getFeelerTile(this.getFeelersize());
            }

            if (var2 != null && this.current != null) {
               IsoObject var3 = this.current.testCollideSpecialObjects(var2);
               if (var3 == null) {
                  var3 = this.current.getHoppableTo(var2);
               }

               IsoDoor var4 = (IsoDoor)Type.tryCastTo(var3, IsoDoor.class);
               IsoThumpable var5 = (IsoThumpable)Type.tryCastTo(var3, IsoThumpable.class);
               if (var5 == null && var4 == null) {
                  return false;
               } else {
                  this.thumpTarget = (IsoObject)(var5 != null ? var5 : var4);
                  this.setPath2((Path)null);
                  return true;
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public float getAnimalTrailerSize() {
      return this.adef.trailerBaseSize * this.getAnimalSize();
   }

   public boolean canBePet() {
      return this.adef.canBePet;
   }

   public static void toggleExtraValues() {
      DISPLAY_EXTRA_VALUES = !DISPLAY_EXTRA_VALUES;
   }

   public void setExtraValues(Boolean var1) {
      DISPLAY_EXTRA_VALUES = var1;
   }

   public static boolean isExtraValues() {
      return DISPLAY_EXTRA_VALUES;
   }

   public void debugRandomIdleAnim() {
      if (this.isAnimalSitting() && this.adef.sittingTypeNbr > 0) {
         this.setVariable("sittingAnim", "sit" + Rand.Next(1, this.adef.sittingTypeNbr + 1));
      } else {
         this.setVariable("idleAction", "idle" + Rand.Next(1, this.adef.idleTypeNbr + 1));
      }

   }

   public void debugRandomHappyAnim() {
      this.setVariable("idleAction", "happy" + Rand.Next(1, this.adef.happyAnim + 1));
   }

   public DesignationZoneAnimal getDZone() {
      return this.dZone;
   }

   public boolean haveMatingSeason() {
      return this.adef.matingPeriodStart != 0 && this.adef.matingPeriodEnd != 0;
   }

   public boolean isInMatingSeason() {
      if (SandboxOptions.instance.AnimalMatingSeason.getValue() && this.haveMatingSeason()) {
         int var1 = GameTime.getInstance().getMonth() + 1;
         int var2 = this.adef.matingPeriodStart;
         int var3 = this.adef.matingPeriodEnd;
         if (var2 < var3) {
            return var1 >= var2 && var1 < var3;
         } else {
            return var1 < var3 || var1 >= var2;
         }
      } else {
         return true;
      }
   }

   public int getMinAgeForBaby() {
      AnimalAllele var1 = this.getUsedGene("ageToGrow");
      float var2 = 1.0F;
      if (var1 != null) {
         var2 = var1.currentValue;
      }

      int var3 = this.adef.minAgeForBaby;
      float var4 = 0.25F - var2 / 4.0F + 1.0F;
      return (int)((float)var3 * var4);
   }

   public boolean isHeld() {
      return this.heldBy != null;
   }

   public void pathFailed() {
      if (this.eatFromTrough != null) {
         this.ignoredTrough.add(this.eatFromTrough);
      }

      if (this.drinkFromTrough != null) {
         this.ignoredTrough.add(this.drinkFromTrough);
      }

      if (this.getBehavior().behaviorObject instanceof IsoFeedingTrough) {
         this.ignoredTrough.add((IsoFeedingTrough)this.getBehavior().behaviorObject);
      }

      this.stopAllMovementNow();
   }

   public AnimalSoundState getAnimalSoundState(String var1) {
      Objects.requireNonNull(var1);
      AnimalSoundState var2 = (AnimalSoundState)this.animalSoundState.get(var1);
      if (var2 == null) {
         var2 = new AnimalSoundState(this);
         this.animalSoundState.put(var1, var2);
      }

      return var2;
   }

   public void playDeadSound() {
      this.playBreedSound("death");
   }

   public void updateVocalProperties() {
      if (!GameServer.bServer) {
         AnimalSoundState var1 = this.getAnimalSoundState("voice");
         if (var1.getEventInstance() != 0L && !this.getEmitter().isPlaying(var1.getEventInstance())) {
            var1.stop();
         }

         boolean var2 = SoundManager.instance.isListenerInRange(this.getX(), this.getY(), 20.0F);
         if (this.isAlive() && var2) {
            this.chooseIdleSound();
            AnimalVocalsManager.instance.addCharacter(this);
         }

         if (this.isDead() && var2) {
            AnimalVocalsManager.instance.addCharacter(this);
         }

      }
   }

   public void playNextFootstepSound() {
      if (!StringUtils.isNullOrWhitespace(this.nextFootstepSound)) {
         this.playBreedSound(this.nextFootstepSound);
         this.nextFootstepSound = null;
      }
   }

   public void onPlayBreedSoundEvent(String var1) {
      if (!"run".equalsIgnoreCase(var1) && !"walk".equalsIgnoreCase(var1) && !"walkFront".equalsIgnoreCase(var1) && !"walkBack".equalsIgnoreCase(var1)) {
         this.playBreedSound(var1);
      } else {
         this.nextFootstepSound = var1;
         AnimalFootstepManager.instance.addCharacter(this);
      }
   }

   public long playBreedSound(String var1) {
      AnimalBreed var2 = this.getBreed();
      if (var2 == null) {
         return 0L;
      } else {
         AnimalBreed.Sound var3 = var2.getSound(var1);
         if (var3 == null) {
            return 0L;
         } else if (var3.slot != null) {
            AnimalSoundState var4 = this.getAnimalSoundState(var3.slot);
            if (var4.isPlaying() && var4.getPriority() > var3.priority) {
               return 0L;
            } else {
               var4.setDesiredSoundName(var3.soundName);
               var4.setDesiredSoundPriority(var3.priority);
               if ("death".equalsIgnoreCase(var1)) {
                  var4.setIntervalExpireTime(var3.soundName, System.currentTimeMillis() + 10000L);
               }

               return var4.start(var3.soundName, var3.priority);
            }
         } else {
            return this.playSoundLocal(var3.soundName);
         }
      }
   }

   private void chooseIdleSound() {
      AnimalSoundState var1 = this.getAnimalSoundState("voice");
      if (this.isDead()) {
         var1.setDesiredSoundName((String)null);
         var1.setDesiredSoundPriority(0);
         this.forceNextIdleSound = null;
      } else if (this.data != null && this.data.getBreed() != null) {
         String var2 = "idle";
         AnimalBreed.Sound var3 = this.data.getBreed().getSound("stressed");
         if (var3 != null && var1.isPlaying(var3.soundName)) {
            var2 = "stressed";
         } else if (this.getStress() > 50.0F && (var3 != null && var1.isPlaying(var3.soundName) || (float)Rand.Next(100) < this.getStress())) {
            var2 = "stressed";
         }

         if ("idle".equalsIgnoreCase(var2) && this.data.getBreed().isSoundDefined("idle_walk") && this.isAnimalMoving()) {
            var2 = "idle_walk";
         }

         boolean var4 = this.forceNextIdleSound != null;
         if (var4) {
            var2 = this.forceNextIdleSound;
         }

         AnimalBreed.Sound var5 = this.data.getBreed().getSound(var2);
         if (!var1.isPlaying() || var5 == null || var5.priority >= var1.getPriority()) {
            if (var5 == null) {
               var1.setDesiredSoundName((String)null);
               var1.setDesiredSoundPriority(0);
            } else {
               var1.setDesiredSoundName(var5.soundName);
               var1.setDesiredSoundPriority(var5.priority);
               if (var1.isPlayingDesiredSound() && var5.isIntervalValid()) {
                  long var6 = var1.getIntervalExpireTime(var5.soundName);
                  if (var6 < System.currentTimeMillis()) {
                     var1.setIntervalExpireTime(var5.soundName, System.currentTimeMillis() + (long)Rand.Next(var5.intervalMin, var5.intervalMax) * 1000L);
                  }
               }

               if (var4 && var1.isPlaying(var5.soundName)) {
                  this.forceNextIdleSound = null;
               }

            }
         }
      } else {
         var1.setDesiredSoundName((String)null);
         var1.setDesiredSoundPriority(0);
         this.forceNextIdleSound = null;
      }
   }

   public void playStressedSound() {
      String var1 = "stressed";
      AnimalSoundState var2 = this.getAnimalSoundState("voice");
      if (this.isDead()) {
         var2.setDesiredSoundName((String)null);
         var2.setDesiredSoundPriority(0);
      } else if (this.data != null && this.data.getBreed() != null) {
         AnimalBreed.Sound var3 = this.data.getBreed().getSound(var1);
         if (!var2.isPlaying() || var3 == null || var3.priority >= var2.getPriority()) {
            if (var3 == null) {
               var2.setDesiredSoundName((String)null);
               var2.setDesiredSoundPriority(0);
            } else {
               this.forceNextIdleSound = var1;
               var2.setIntervalExpireTime(var3.soundName, 0L);
               var2.setDesiredSoundName(var3.soundName);
               var2.setDesiredSoundPriority(var3.priority);
            }
         }
      } else {
         var2.setDesiredSoundName((String)null);
         var2.setDesiredSoundPriority(0);
      }
   }

   public void updateLoopingSounds() {
      LogSeverity var1 = DebugLog.Sound.getLogSeverity();
      DebugLog.Sound.setLogSeverity(LogSeverity.General);

      try {
         this.updateRunLoopingSound();
         this.updateWalkLoopingSound();
      } finally {
         DebugLog.Sound.setLogSeverity(var1);
      }

   }

   public void updateRunLoopingSound() {
      AnimalBreed.Sound var1 = this.getBreed().getSound("runloop");
      if (var1 != null && var1.slot != null) {
         boolean var2 = false;
         AnimLayer var3 = this.getAdvancedAnimator().getRootLayer();
         if (var3 != null) {
            List var4 = var3.getLiveAnimNodes();
            Iterator var5 = var4.iterator();

            while(var5.hasNext()) {
               LiveAnimNode var6 = (LiveAnimNode)var5.next();
               if ("runPathfind".equalsIgnoreCase(var6.getName())) {
                  var2 = true;
                  break;
               }

               if ("run".equalsIgnoreCase(var6.getName())) {
                  var2 = true;
                  break;
               }
            }
         }

         boolean var7 = SoundManager.instance.isListenerInRange(this.getX(), this.getY(), 10.0F);
         if (!var7) {
            var2 = false;
         }

         AnimalSoundState var8 = this.getAnimalSoundState(var1.slot);
         if (var8.isPlaying()) {
            if (!var2) {
               var8.stop();
            }
         } else if (var2) {
            var8.setDesiredSoundName(var1.soundName);
            var8.setDesiredSoundPriority(var1.priority);
            var8.start(var1.soundName, var1.priority);
         }

      }
   }

   public void updateWalkLoopingSound() {
      AnimalBreed.Sound var1 = this.getBreed().getSound("walkloop");
      if (var1 != null && var1.slot != null) {
         boolean var2 = false;
         AnimLayer var3 = this.getAdvancedAnimator().getRootLayer();
         if (var3 != null) {
            List var4 = var3.getLiveAnimNodes();
            Iterator var5 = var4.iterator();

            while(var5.hasNext()) {
               LiveAnimNode var6 = (LiveAnimNode)var5.next();
               if ("defaultPathfind".equalsIgnoreCase(var6.getName())) {
                  var2 = true;
                  break;
               }

               if ("defaultWalk".equalsIgnoreCase(var6.getName())) {
                  var2 = true;
                  break;
               }
            }
         }

         boolean var7 = SoundManager.instance.isListenerInRange(this.getX(), this.getY(), 10.0F);
         if (!var7) {
            var2 = false;
         }

         AnimalSoundState var8 = this.getAnimalSoundState(var1.slot);
         if (var8.isPlaying()) {
            if (!var2) {
               var8.stop();
            }
         } else if (var2) {
            var8.setDesiredSoundName(var1.soundName);
            var8.setDesiredSoundPriority(var1.priority);
            var8.start(var1.soundName, var1.priority);
         }

      }
   }

   public IsoAnimal getMother() {
      return this.mother;
   }

   public void setMother(IsoAnimal var1) {
      this.mother = var1;
      if (var1.getBabies() == null) {
         var1.babies = new ArrayList();
      }

      for(int var2 = 0; var2 < var1.getBabies().size(); ++var2) {
         if (((IsoAnimal)var1.getBabies().get(var2)).getAnimalID() == this.getAnimalID() || var1.getBabies().get(var2) == this) {
            var1.removeBaby((IsoAnimal)var1.getBabies().get(var2));
            break;
         }
      }

      var1.getBabies().add(this);
   }

   public boolean canBePicked(IsoGameCharacter var1) {
      if (!var1.isUnlimitedCarry() && !var1.isGodMod()) {
         return this.adef.canBePicked && this.getData().getWeight() < (float)(40 + var1.getPerkLevel(PerkFactory.Perks.Strength) * 7);
      } else {
         return true;
      }
   }

   public int getAnimalID() {
      return this.animalID;
   }

   public void setItemID(int var1) {
      this.itemID = var1;
   }

   public int getItemID() {
      return this.itemID;
   }

   public String getNextStageAnimalType() {
      if (this.getData().currentStage == null) {
         return "";
      } else {
         return this.isFemale() ? this.getData().currentStage.nextStage : this.getData().currentStage.nextStageMale;
      }
   }

   public void debugForceEgg() {
      this.getData().eggsToday = 0;
      this.getData().eggTime = (long)(Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue() - 10);
      this.getData().checkEggs(GameTime.instance.getCalender(), false);
   }

   public boolean isWild() {
      return this.wild;
   }

   public void setWild(boolean var1) {
      if (var1 || this.adef.canBeDomesticated) {
         if (!var1) {
            this.migrationGroup = null;
            this.virtualID = 0.0;
         } else {
            this.setDebugStress(Rand.Next(50.0F, 80.0F));
         }

         this.wild = var1;
      }
   }

   public void alertOtherAnimals(IsoMovingObject var1, boolean var2) {
      for(int var3 = (int)this.getX() - 5; var3 < (int)this.getX() + 5; ++var3) {
         for(int var4 = (int)this.getY() - 5; var4 < (int)this.getY() + 5; ++var4) {
            IsoGridSquare var5 = this.getSquare().getCell().getGridSquare((double)var3, (double)var4, (double)this.getZ());
            if (var5 != null && !var5.getAnimals().isEmpty()) {
               for(int var6 = 0; var6 < var5.getAnimals().size(); ++var6) {
                  IsoAnimal var7 = (IsoAnimal)var5.getAnimals().get(var6);
                  if (var7 != this) {
                     if (var7.adef.canBeAlerted && var7.getBehavior().lastAlerted <= 0.0F && var2) {
                        var7.setIsAlerted(true);
                        var7.alertedChr = this.alertedChr;
                     } else {
                        var7.spottedChr = this.spottedChr;
                     }
                  }
               }
            }
         }
      }

   }

   public void debugForceSit() {
      if (!this.isAnimalSitting()) {
         this.behavior.sitInTime = Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue() - 10;
         this.behavior.sitOutTime = this.behavior.sitInTime + '負';
      } else {
         this.behavior.sitOutTime = Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue() - 10;
         this.behavior.sitInTime = 0;
      }

      this.behavior.checkSit();
   }

   public boolean isAlerted() {
      return this.alerted;
   }

   public void setIsAlerted(boolean var1) {
      this.alerted = var1;
   }

   public boolean shouldFollowWall() {
      return this.shouldFollowWall;
   }

   public void setShouldFollowWall(boolean var1) {
      this.shouldFollowWall = var1;
   }

   public boolean readyToBeMilked() {
      return this.data.getMilkQuantity() > 1.0F && this.canBeMilked();
   }

   public boolean readyToBeSheared() {
      return this.data.getWoolQuantity() > 1.0F && this.canBeSheared();
   }

   public boolean haveHappyAnim() {
      return this.adef.happyAnim > 0;
   }

   public boolean canHaveEggs() {
      return this.adef.eggsPerDay > 0;
   }

   public boolean needHutch() {
      return this.adef.hutches != null && !this.adef.hutches.isEmpty();
   }

   public boolean canPoop() {
      return this.adef.dung != null;
   }

   public int getMinClutchSize() {
      float var1 = 1.0F;
      AnimalAllele var2 = this.getUsedGene("eggClutch");
      if (var2 != null) {
         var1 = var2.currentValue;
      }

      return Float.valueOf((float)this.adef.minClutchSize * var1).intValue();
   }

   public int getMaxClutchSize() {
      float var1 = 1.0F;
      AnimalAllele var2 = this.getUsedGene("eggClutch");
      if (var2 != null) {
         var1 = var2.currentValue;
      }

      return Float.valueOf((float)this.adef.maxClutchSize * var1).intValue();
   }

   public int getCurrentClutchSize() {
      return this.getData().clutchSize;
   }

   public boolean attackOtherMales() {
      return !this.isBaby() && !this.getDescriptor().isFemale() && !this.adef.dontAttackOtherMale;
   }

   public boolean shouldAnimalStressAboveGround() {
      return this.adef.stressAboveGround && this.getZ() > 0.0F;
   }

   public boolean canClimbStairs() {
      return this.adef.canClimbStairs;
   }

   public void forceWanderNow() {
      this.setStateEventDelayTimer(0.0F);
   }

   public boolean canClimbFences() {
      return this.adef.canClimbFences;
   }

   public void climbOverFence(IsoDirections var1) {
      if (this.current != null && !this.getVariableBoolean("ClimbFence")) {
         IsoGridSquare var2 = this.current.nav[var1.index()];
         if (IsoWindow.canClimbThroughHelper(this, this.current, var2, var1 == IsoDirections.N || var1 == IsoDirections.S)) {
            AnimalClimbOverFenceState.instance().setParams(this, var1);
            this.setVariable("ClimbFence", true);
            if (GameClient.bClient && this.isLocalPlayer()) {
               INetworkPacket.send(PacketTypes.PacketType.AnimalEvent, this, this.getX(), this.getY(), this.getZ());
            }

         }
      }
   }

   public boolean needMom() {
      return this.adef.needMom;
   }

   public int getFertilizedTimeMax() {
      return this.adef.fertilizedTimeMax;
   }

   public boolean isLocalPlayer() {
      return this.getNetworkCharacterAI() != null && !this.getNetworkCharacterAI().isRemote();
   }

   public float getThirstBoost() {
      return this.adef.thirstBoost;
   }

   public float getHungerBoost() {
      return this.adef.hungerBoost;
   }

   public void removeBaby(IsoAnimal var1) {
      if (this.babies != null) {
         this.babies.remove(var1);
      }

   }

   public void remove() {
      if (this.getMother() != null) {
         this.getMother().removeBaby(this);
      }

      IsoPlayer var1 = this.getData().getAttachedPlayer();
      if (var1 != null) {
         var1.getAttachedAnimals().remove(this);
      }

      this.getData().setAttachedPlayer((IsoPlayer)null);
      this.delete();
   }

   public void delete() {
      DebugLog.Animal.debugln("Animal delete id=%d", this.getOnlineID());
      this.removeFromWorld();
      this.removeFromSquare();
      AnimalInstanceManager.getInstance().remove(this);
      if (GameServer.bServer) {
         AnimalSynchronizationManager.getInstance().delete(this.getOnlineID());
      }

   }

   public InventoryItem canEatFromTrough(IsoFeedingTrough var1) {
      if (this.adef.eatTypeTrough != null && var1.getContainer() != null) {
         for(int var2 = 0; var2 < var1.getContainer().getItems().size(); ++var2) {
            InventoryItem var3 = (InventoryItem)var1.getContainer().getItems().get(var2);
            if (!(var3 instanceof Food) || !((Food)var3).isRotten()) {
               if (this.adef.eatTypeTrough.contains("All") || this.adef.eatTypeTrough.contains(var3.getFullType()) || this.adef.eatTypeTrough.contains(var3.getAnimalFeedType())) {
                  return var3;
               }

               if (var3 instanceof Food && this.adef.eatTypeTrough.contains(((Food)var3).getFoodType())) {
                  return var3;
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public float getThumpDelay() {
      return this.thumpDelay;
   }

   public float getBloodQuantity() {
      float var1 = (this.getData().getWeight() - this.adef.minWeight) / (this.adef.maxWeight - this.getData().getWeight());
      return (this.adef.minBlood + (this.adef.maxBlood - this.adef.minBlood) * var1) / 100.0F;
   }

   public int getFeatherNumber() {
      return !this.isBaby() && !StringUtils.isNullOrEmpty(this.getBreed().featherItem) && this.getBreed().maxFeather > 0 ? (int)((float)this.getBreed().maxFeather * this.getData().getSize()) : 0;
   }

   public String getFeatherItem() {
      return this.getBreed().featherItem;
   }

   public UdpConnection getOwner() {
      return AnimalOwnershipManager.getInstance().getOwner(this);
   }

   public IsoPlayer getOwnerPlayer() {
      return AnimalOwnershipManager.getInstance().getOwnership(this);
   }

   public boolean isHappy() {
      String var1 = this.getVariableString("idleAction");
      return !StringUtils.isNullOrEmpty(var1) && var1.startsWith("happy");
   }

   public boolean shouldBeSkeleton() {
      return this.shouldBeSkeleton;
   }

   public void setShouldBeSkeleton(boolean var1) {
      this.shouldBeSkeleton = var1;
   }

   public ArrayList<String> getGeneticDisorder() {
      return this.geneticDisorder;
   }

   public ArrayList<IsoAnimal> getBabies() {
      return this.babies;
   }

   public boolean useRagdoll() {
      return false;
   }

   public float getZoneAcceptance() {
      return this.zoneAcceptance;
   }

   public float getPlayerAcceptance(IsoPlayer var1) {
      if (this.playerAcceptanceList.isEmpty()) {
         return 0.0F;
      } else {
         if (this.playerAcceptanceList.get(var1.getOnlineID()) == null) {
            this.playerAcceptanceList.put(var1.getOnlineID(), Rand.Next(0.0F, 20.0F));
         }

         return (float)Math.round((Float)this.playerAcceptanceList.get(var1.getOnlineID()));
      }
   }

   public static void addAnimalPart(AnimalPart var0, IsoPlayer var1, IsoDeadBody var2) {
      float var3 = (float)var0.minNb * var2.getAnimalSize();
      float var4 = (float)var0.maxNb * var2.getAnimalSize();
      int var5 = var0.nb;
      float var6 = (float)(Double)var2.getModData().rawget("meatRatio");
      if (var6 <= 0.0F) {
         var6 = 1.0F;
      }

      InventoryItem var7 = InventoryItemFactory.CreateItem(var0.item);
      if (var7 instanceof Food) {
         var3 *= var6;
         var4 *= var6;
      }

      if (var5 == -1) {
         var5 = Rand.Next((int)var3, (int)var4);
      }

      if (var5 < 1) {
         var5 = 1;
      }

      for(int var8 = 0; var8 < var5; ++var8) {
         var7 = InventoryItemFactory.CreateItem(var0.item);
         if (var7 instanceof Food) {
            modifyMeat((Food)var7, var2.getAnimalSize(), var6);
         }

         var1.getInventory().AddItem(var7);
      }

   }

   public static void modifyMeat(Food var0, float var1, float var2) {
      var0.setHungChange(var0.getBaseHunger() * var1 * var2 * Rand.Next(0.9F, 1.1F));
      var0.setBaseHunger(var0.getHungerChange());
      var0.setCalories(var0.getCalories() * var1 * var2 * Rand.Next(0.9F, 1.1F));
      var0.setLipids(var0.getLipids() * var1 * var2 * Rand.Next(0.9F, 1.1F));
      var0.setProteins(var0.getProteins() * var1 * var2 * Rand.Next(0.9F, 1.1F));
   }

   public boolean shouldStartFollowWall() {
      return this.getData().getAttachedPlayer() != null || this.getStress() > 20.0F && this.getVariableBoolean("animalRunning") || this.isWild();
   }

   public float getCorpseSize() {
      return this.adef.corpseSize;
   }

   public float getCorpseLength() {
      return this.adef.corpseLength;
   }

   public void setOnHook(boolean var1) {
      this.onHook = var1;
   }

   public boolean isOnHook() {
      return this.onHook;
   }

   public AnimalDefinitions getAdef() {
      return this.adef;
   }

   public IsoButcherHook getHook() {
      return this.hook;
   }

   public void setHook(IsoButcherHook var1) {
      this.hook = var1;
   }

   private void reattachBackToHook() {
      if (this.attachBackToHookX != 0 || this.attachBackToHookY != 0) {
         IsoGridSquare var1 = this.getSquare().getCell().getGridSquare(this.attachBackToHookX, this.attachBackToHookY, this.attachBackToHookZ);
         if (var1 != null && var1.getObjects() != null) {
            for(int var2 = 0; var2 < var1.getObjects().size(); ++var2) {
               IsoButcherHook var3 = (IsoButcherHook)Type.tryCastTo((IsoObject)var1.getObjects().get(var2), IsoButcherHook.class);
               if (var3 != null) {
                  var3.reattachAnimal(this);
                  this.attachBackToHookX = 0;
                  this.attachBackToHookY = 0;
                  this.attachBackToHookZ = 0;
                  return;
               }
            }

         }
      }
   }

   private void ensureCorrectSkin() {
      if (!StringUtils.isNullOrEmpty(AnimalDefinitions.getDef(this.getAnimalType()).textureSkinned) && ((KahluaTableImpl)this.getModData()).rawgetBool("skinned") && !this.getAnimalVisual().getSkinTexture().equalsIgnoreCase(AnimalDefinitions.getDef(this.getAnimalType()).textureSkinned)) {
         this.getAnimalVisual().setSkinTextureName(AnimalDefinitions.getDef(this.getAnimalType()).textureSkinned);
         this.resetModel();
         this.resetModelNextFrame();
      }

   }

   public String getTypeAndBreed() {
      String var10000 = this.getAnimalType();
      return var10000 + this.getBreed().getName();
   }

   public static IsoAnimal createAnimalFromCorpse(IsoDeadBody var0) {
      IsoAnimal var1 = new IsoAnimal(var0.getSquare().getCell(), var0.getSquare().getX(), var0.getSquare().getY(), var0.getSquare().getZ(), ((KahluaTableImpl)var0.getModData()).rawgetStr("AnimalType"), ((KahluaTableImpl)var0.getModData()).rawgetStr("AnimalBreed"));
      var1.setCustomName(var0.getCustomName());
      var1.setModData(var0.getModData());
      var1.getAnimalVisual().setSkinTextureName(var0.getAnimalVisual().getSkinTexture());
      return var1;
   }

   public void updateLOS() {
      float var1 = this.getX();
      float var2 = this.getY();
      float var3 = this.getZ();
      int var4 = 0;
      boolean var5 = false;
      int var6 = this.getCell().getObjectList().size();

      for(int var7 = 0; var7 < var6; ++var7) {
         IsoMovingObject var8 = (IsoMovingObject)this.getCell().getObjectList().get(var7);
         if (!(var8 instanceof IsoPhysicsObject) && !(var8 instanceof BaseVehicle)) {
            if (var8 instanceof IsoZombie) {
               IsoZombie var9 = (IsoZombie)var8;
               if (var9.isReanimatedForGrappleOnly()) {
                  continue;
               }
            }

            if (var8 == this) {
               this.spottedList.add(var8);
            } else {
               float var17 = var8.getX();
               float var10 = var8.getY();
               float var11 = var8.getZ();
               float var12 = IsoUtils.DistanceTo(var17, var10, var1, var2);
               if (var12 < 20.0F) {
                  ++var4;
               }

               IsoGridSquare var13 = var8.getCurrentSquare();
               if (var13 != null) {
                  IsoGameCharacter var14 = (IsoGameCharacter)Type.tryCastTo(var8, IsoGameCharacter.class);
                  IsoPlayer var15 = (IsoPlayer)Type.tryCastTo(var14, IsoPlayer.class);
                  IsoZombie var16 = (IsoZombie)Type.tryCastTo(var14, IsoZombie.class);
                  if (var16 != null) {
                     this.getBehavior().spotted(var16, false, var12);
                  }

                  if (!(var14 instanceof IsoAnimal) && var14 instanceof IsoPlayer && !((IsoPlayer)var14).isInvisible() && !((IsoPlayer)var14).isGhostMode()) {
                     this.getBehavior().spotted(var14, false, var12);
                  }
               }
            }
         }
      }

   }

   public boolean canBePutInHutch(IsoHutch var1) {
      return this.adef != null && this.adef.hutches != null ? this.adef.hutches.contains(var1.type) : false;
   }

   public boolean shouldCreateZone() {
      return this.getDZone() == null && !this.isWild() && this.getData().getAttachedPlayer() == null && this.getData().getAttachedTree() == null && !this.getVariableBoolean("AnimalRunning");
   }
}
