package zombie.characters;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import zombie.CombatManager;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.PersistentOutfits;
import zombie.SandboxOptions;
import zombie.SharedDescriptors;
import zombie.SoundManager;
import zombie.SystemDisabler;
import zombie.VirtualZombieManager;
import zombie.WorldSoundManager;
import zombie.Lua.LuaEventManager;
import zombie.ai.State;
import zombie.ai.ZombieGroupManager;
import zombie.ai.astar.AStarPathFinder;
import zombie.ai.astar.Mover;
import zombie.ai.states.AttackNetworkState;
import zombie.ai.states.AttackState;
import zombie.ai.states.BumpedState;
import zombie.ai.states.BurntToDeath;
import zombie.ai.states.ClimbOverFenceState;
import zombie.ai.states.ClimbOverWallState;
import zombie.ai.states.ClimbThroughWindowPositioningParams;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.ai.states.CrawlingZombieTurnState;
import zombie.ai.states.FakeDeadAttackState;
import zombie.ai.states.FakeDeadZombieState;
import zombie.ai.states.GrappledThrownOutWindowState;
import zombie.ai.states.IdleState;
import zombie.ai.states.LungeNetworkState;
import zombie.ai.states.LungeState;
import zombie.ai.states.PathFindState;
import zombie.ai.states.PlayerHitReactionState;
import zombie.ai.states.StaggerBackState;
import zombie.ai.states.ThumpState;
import zombie.ai.states.WalkTowardNetworkState;
import zombie.ai.states.WalkTowardState;
import zombie.ai.states.ZombieEatBodyState;
import zombie.ai.states.ZombieFaceTargetState;
import zombie.ai.states.ZombieFallDownState;
import zombie.ai.states.ZombieFallingState;
import zombie.ai.states.ZombieGetDownState;
import zombie.ai.states.ZombieGetUpFromCrawlState;
import zombie.ai.states.ZombieGetUpState;
import zombie.ai.states.ZombieHitReactionState;
import zombie.ai.states.ZombieIdleState;
import zombie.ai.states.ZombieOnGroundState;
import zombie.ai.states.ZombieReanimateState;
import zombie.ai.states.ZombieSittingState;
import zombie.ai.states.ZombieTurnAlerted;
import zombie.audio.parameters.ParameterCharacterInside;
import zombie.audio.parameters.ParameterCharacterMovementSpeed;
import zombie.audio.parameters.ParameterCharacterOnFire;
import zombie.audio.parameters.ParameterFootstepMaterial;
import zombie.audio.parameters.ParameterFootstepMaterial2;
import zombie.audio.parameters.ParameterPlayerDistance;
import zombie.audio.parameters.ParameterShoeType;
import zombie.audio.parameters.ParameterVehicleHitLocation;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.AttachedItems.AttachedItem;
import zombie.characters.AttachedItems.AttachedItems;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.WornItems.WornItem;
import zombie.characters.WornItems.WornItems;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionGroup;
import zombie.characters.skills.PerkFactory;
import zombie.core.BoxedStaticValues;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderSettings;
import zombie.core.opengl.Shader;
import zombie.core.physics.RagdollSettingsManager;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.Vector3;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.sharedskele.SharedSkeleAnimationRepository;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.utils.BooleanGrid;
import zombie.core.utils.OnceEvery;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemSpawner;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.LosUtil;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.MPStatistics;
import zombie.network.NetworkVariables;
import zombie.network.ServerLOS;
import zombie.network.ServerMap;
import zombie.network.packets.character.ZombiePacket;
import zombie.pathfind.Path;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.popman.NetworkZombieManager;
import zombie.popman.NetworkZombieSimulator;
import zombie.popman.ZombieCountOptimiser;
import zombie.popman.ZombieStateFlags;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.AttackVehicleState;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public final class IsoZombie extends IsoGameCharacter implements IHumanVisual {
   public static final byte SPEED_SPRINTER = 1;
   public static final byte SPEED_FAST_SHAMBLER = 2;
   public static final byte SPEED_SHAMBLER = 3;
   public static final byte SPEED_RANDOM = 4;
   public static final byte HEARING_PINPOINT = 1;
   public static final byte HEARING_NORMAL = 2;
   public static final byte HEARING_POOR = 3;
   public static final byte HEARING_RANDOM = 4;
   public static final byte HEARING_NORMAL_OR_POOR = 5;
   public static final byte THUMP_FLAG_GENERIC = 1;
   public static final byte THUMP_FLAG_WINDOW_EXTRA = 2;
   public static final byte THUMP_FLAG_WINDOW = 3;
   public static final byte THUMP_FLAG_METAL = 4;
   public static final byte THUMP_FLAG_GARAGE_DOOR = 5;
   public static final byte THUMP_FLAG_WIRE_FENCE = 6;
   public static final byte THUMP_FLAG_METAL_POLE_GATE = 7;
   private boolean alwaysKnockedDown;
   private boolean onlyJawStab;
   private boolean forceEatingAnimation;
   private boolean noTeeth;
   public static final int AllowRepathDelayMax = 120;
   public static final boolean SPRINTER_FIXES = true;
   public int LastTargetSeenX;
   public int LastTargetSeenY;
   public int LastTargetSeenZ;
   public boolean Ghost;
   public float LungeTimer;
   public long LungeSoundTime;
   public IsoMovingObject target;
   public float TimeSinceSeenFlesh;
   private float targetSeenTime;
   public int FollowCount;
   public int ZombieID;
   private float BonusSpotTime;
   public boolean bStaggerBack;
   private boolean bKnifeDeath;
   private boolean bJawStabAttach;
   private boolean bBecomeCrawler;
   private boolean bFakeDead;
   private boolean bForceFakeDead;
   private boolean bWasFakeDead;
   private boolean bReanimate;
   public DeadBodyAtlas.BodyTexture atlasTex;
   private boolean bReanimatedPlayer;
   public boolean bIndoorZombie;
   public int thumpFlag;
   public boolean thumpSent;
   private float thumpCondition;
   public static final float EAT_BODY_DIST = 1.0F;
   public static final float EAT_BODY_TIME = 3600.0F;
   public static final float LUNGE_TIME = 180.0F;
   public static final float CRAWLER_DAMAGE_DOT = 0.9F;
   public static final float CRAWLER_DAMAGE_RANGE = 1.5F;
   private boolean useless;
   public int speedType;
   public ZombieGroup group;
   public boolean inactive;
   public int strength;
   public int cognition;
   public int memory;
   public int sight;
   public int hearing;
   private ArrayList<InventoryItem> itemsToSpawnAtDeath;
   private int voiceChoice;
   private float soundReactDelay;
   private final IsoGameCharacter.Location delayedSound;
   private boolean bSoundSourceRepeating;
   public Object soundSourceTarget;
   public float soundAttract;
   public float soundAttractTimeout;
   private final Vector2 hitAngle;
   public boolean alerted;
   private String walkType;
   private float footstepVolume;
   protected SharedDescriptors.Descriptor sharedDesc;
   public boolean bDressInRandomOutfit;
   public String pendingOutfitName;
   protected final HumanVisual humanVisual;
   private int crawlerType;
   private String playerAttackPosition;
   private float eatSpeed;
   private boolean sitAgainstWall;
   private static final int CHECK_FOR_CORPSE_TIMER_MAX = 10000;
   private float checkForCorpseTimer;
   public IsoDeadBody bodyToEat;
   public IsoMovingObject eatBodyTarget;
   private int hitTime;
   private int thumpTimer;
   private boolean hitLegsWhileOnFloor;
   public boolean collideWhileHit;
   private float m_characterTextureAnimTime;
   private float m_characterTextureAnimDuration;
   public int lastPlayerHit;
   public final float VISION_RADIUS_MAX;
   public final float VISION_RADIUS_MIN;
   public float VISION_RADIUS_RESULT;
   public final float VISION_FOG_PENALTY_MAX;
   public final float VISION_RAIN_PENALTY_MAX;
   public final float VISION_DARKNESS_PENALTY_MAX;
   public final int HEARING_UNSEEN_OFFSET_MIN;
   public final int HEARING_UNSEEN_OFFSET_HEAVY_RAIN;
   public final int HEARING_UNSEEN_OFFSET_MAX;
   protected final ItemVisuals itemVisuals;
   private int hitHeadWhileOnFloor;
   private boolean m_bAttackDidDamage;
   private String m_attackOutcome;
   private boolean m_reanimatedForGrappleOnly;
   public Imposter imposter;
   private BaseVehicle vehicle4testCollision;
   public String SpriteName;
   public static final int PALETTE_COUNT = 3;
   public final Vector2 vectorToTarget;
   public float AllowRepathDelay;
   public boolean KeepItReal;
   private boolean isSkeleton;
   public final ParameterCharacterInside parameterCharacterInside;
   private final ParameterCharacterMovementSpeed parameterCharacterMovementSpeed;
   public final ParameterCharacterOnFire parameterCharacterOnFire;
   private final ParameterFootstepMaterial parameterFootstepMaterial;
   private final ParameterFootstepMaterial2 parameterFootstepMaterial2;
   public final ParameterPlayerDistance parameterPlayerDistance;
   private final ParameterShoeType parameterShoeType;
   private final ParameterVehicleHitLocation parameterVehicleHitLocation;
   public final ParameterZombieState parameterZombieState;
   public boolean scratch;
   public boolean laceration;
   public final NetworkZombieAI networkAI;
   public UdpConnection authOwner;
   public IsoPlayer authOwnerPlayer;
   public ZombiePacket zombiePacket;
   public boolean zombiePacketUpdated;
   public long lastChangeOwner;
   public int bloodSplatAmount;
   public BodyPartType lastHitPart;
   private static final SharedSkeleAnimationRepository m_sharedSkeleRepo = new SharedSkeleAnimationRepository();
   public int palette;
   static final int s_saveFormatVersion = 1;
   public int AttackAnimTime;
   public static int AttackAnimTimeMax = 50;
   public IsoMovingObject spottedLast;
   Aggro[] aggroList;
   static Map<String, String> temporaryMapCloseSneakBonusDir = Map.ofEntries(Map.entry("trashcontainers_01_08", "NWSE"), Map.entry("trashcontainers_01_09", "NWSE"), Map.entry("trashcontainers_01_10", "NWSE"), Map.entry("trashcontainers_01_11", "NWSE"), Map.entry("trashcontainers_01_12", "NWSE"), Map.entry("trashcontainers_01_13", "NWSE"), Map.entry("trashcontainers_01_14", "NWSE"), Map.entry("trashcontainers_01_15", "NWSE"), Map.entry("f_bushes_1_96", "NWSE"), Map.entry("f_bushes_1_97", "NWSE"), Map.entry("f_bushes_1_98", "NWSE"), Map.entry("f_bushes_1_99", "NWSE"), Map.entry("f_bushes_1_100", "NWSE"), Map.entry("f_bushes_1_101", "NWSE"), Map.entry("f_bushes_1_102", "NWSE"), Map.entry("f_bushes_1_103", "NWSE"), Map.entry("f_bushes_1_104", "NWSE"), Map.entry("f_bushes_1_105", "NWSE"), Map.entry("f_bushes_1_106", "NWSE"), Map.entry("f_bushes_1_107", "NWSE"), Map.entry("f_bushes_1_108", "NWSE"), Map.entry("f_bushes_1_109", "NWSE"), Map.entry("f_bushes_1_110", "NWSE"), Map.entry("f_bushes_1_111", "NWSE"), Map.entry("f_bushes_2_0", "NWSE"), Map.entry("f_bushes_2_1", "NWSE"), Map.entry("f_bushes_2_2", "NWSE"), Map.entry("f_bushes_2_3", "NWSE"), Map.entry("f_bushes_2_4", "NWSE"), Map.entry("f_bushes_2_5", "NWSE"), Map.entry("f_bushes_2_6", "NWSE"), Map.entry("f_bushes_2_7", "NWSE"), Map.entry("f_bushes_2_10", "NWSE"), Map.entry("f_bushes_2_11", "NWSE"), Map.entry("f_bushes_2_12", "NWSE"), Map.entry("f_bushes_2_13", "NWSE"), Map.entry("f_bushes_2_14", "NWSE"), Map.entry("f_bushes_2_15", "NWSE"), Map.entry("f_bushes_2_16", "NWSE"), Map.entry("f_bushes_2_17", "NWSE"), Map.entry("vegetation_ornamental_01_0", "NWSE"), Map.entry("vegetation_ornamental_01_1", "NWSE"), Map.entry("vegetation_ornamental_01_2", "NWSE"), Map.entry("vegetation_ornamental_01_3", "NWSE"), Map.entry("vegetation_ornamental_01_4", "NWSE"), Map.entry("vegetation_ornamental_01_5", "NWSE"), Map.entry("vegetation_ornamental_01_6", "NWSE"), Map.entry("vegetation_ornamental_01_7", "NWSE"), Map.entry("vegetation_ornamental_01_10", "NWSE"), Map.entry("vegetation_ornamental_01_11", "NWSE"), Map.entry("vegetation_ornamental_01_12", "NWSE"), Map.entry("vegetation_ornamental_01_13", "NWSE"), Map.entry("fencing_01_96", "NWSE"), Map.entry("fencing_01_98", "NWSE"), Map.entry("fencing_01_100", "NWSE"), Map.entry("fencing_01_102", "NWSE"), Map.entry("fencing_01_32", "N"), Map.entry("fencing_01_33", "N"), Map.entry("fencing_01_34", "W"), Map.entry("fencing_01_35", "W"), Map.entry("fencing_01_36", "NW"), Map.entry("fencing_01_4", "W"), Map.entry("fencing_01_5", "N"), Map.entry("fencing_01_6", "NW"), Map.entry("carpentry_02_40", "W"), Map.entry("carpentry_02_41", "N"), Map.entry("carpentry_02_42", "NW"), Map.entry("carpentry_02_44", "W"), Map.entry("carpentry_02_45", "N"), Map.entry("carpentry_02_46", "NW"), Map.entry("carpentry_02_48", "W"), Map.entry("carpentry_02_49", "N"), Map.entry("carpentry_02_50", "NW"), Map.entry("fixtures_doors_fences_01_104", "W"), Map.entry("fixtures_doors_fences_01_105", "W"), Map.entry("fixtures_doors_fences_01_96", "W"), Map.entry("fixtures_doors_fences_01_97", "W"), Map.entry("fixtures_doors_fences_01_48", "W"), Map.entry("fixtures_doors_fences_01_49", "W"), Map.entry("fixtures_doors_fences_01_56", "W"), Map.entry("fixtures_doors_fences_01_57", "W"), Map.entry("fixtures_doors_fences_01_98", "N"), Map.entry("fixtures_doors_fences_01_99", "N"), Map.entry("fixtures_doors_fences_01_106", "N"), Map.entry("fixtures_doors_fences_01_107", "N"), Map.entry("fixtures_doors_fences_01_50", "N"), Map.entry("fixtures_doors_fences_01_51", "N"), Map.entry("fixtures_doors_fences_01_58", "N"), Map.entry("fixtures_doors_fences_01_59", "N"), Map.entry("fixtures_doors_fences_01_8", "W"), Map.entry("fixtures_doors_fences_01_9", "N"), Map.entry("fixtures_doors_fences_01_12", "W"), Map.entry("fixtures_doors_fences_01_13", "N"), Map.entry("fixtures_doors_fences_01_4", "W"), Map.entry("fixtures_doors_fences_01_5", "N"));
   static Map<String, Float> temporaryMapCloseSneakBonusValue = Map.ofEntries(Map.entry("trashcontainers_01_08", 100.0F), Map.entry("trashcontainers_01_09", 100.0F), Map.entry("trashcontainers_01_10", 100.0F), Map.entry("trashcontainers_01_11", 100.0F), Map.entry("trashcontainers_01_12", 100.0F), Map.entry("trashcontainers_01_13", 100.0F), Map.entry("trashcontainers_01_14", 100.0F), Map.entry("trashcontainers_01_15", 100.0F), Map.entry("f_bushes_1_96", 100.0F), Map.entry("f_bushes_1_97", 100.0F), Map.entry("f_bushes_1_98", 100.0F), Map.entry("f_bushes_1_99", 100.0F), Map.entry("f_bushes_1_100", 100.0F), Map.entry("f_bushes_1_101", 100.0F), Map.entry("f_bushes_1_102", 100.0F), Map.entry("f_bushes_1_103", 100.0F), Map.entry("f_bushes_1_104", 100.0F), Map.entry("f_bushes_1_105", 100.0F), Map.entry("f_bushes_1_106", 100.0F), Map.entry("f_bushes_1_107", 100.0F), Map.entry("f_bushes_1_108", 100.0F), Map.entry("f_bushes_1_109", 100.0F), Map.entry("f_bushes_1_110", 100.0F), Map.entry("f_bushes_1_111", 100.0F), Map.entry("f_bushes_2_0", 100.0F), Map.entry("f_bushes_2_1", 100.0F), Map.entry("f_bushes_2_2", 100.0F), Map.entry("f_bushes_2_3", 100.0F), Map.entry("f_bushes_2_4", 100.0F), Map.entry("f_bushes_2_5", 100.0F), Map.entry("f_bushes_2_6", 100.0F), Map.entry("f_bushes_2_7", 100.0F), Map.entry("f_bushes_2_10", 100.0F), Map.entry("f_bushes_2_11", 100.0F), Map.entry("f_bushes_2_12", 100.0F), Map.entry("f_bushes_2_13", 100.0F), Map.entry("f_bushes_2_14", 100.0F), Map.entry("f_bushes_2_15", 100.0F), Map.entry("f_bushes_2_16", 100.0F), Map.entry("f_bushes_2_17", 100.0F), Map.entry("vegetation_ornamental_01_0", 100.0F), Map.entry("vegetation_ornamental_01_1", 100.0F), Map.entry("vegetation_ornamental_01_2", 100.0F), Map.entry("vegetation_ornamental_01_3", 100.0F), Map.entry("vegetation_ornamental_01_4", 100.0F), Map.entry("vegetation_ornamental_01_5", 100.0F), Map.entry("vegetation_ornamental_01_6", 100.0F), Map.entry("vegetation_ornamental_01_7", 100.0F), Map.entry("vegetation_ornamental_01_10", 100.0F), Map.entry("vegetation_ornamental_01_11", 100.0F), Map.entry("vegetation_ornamental_01_12", 100.0F), Map.entry("vegetation_ornamental_01_13", 100.0F), Map.entry("fencing_01_96", 100.0F), Map.entry("fencing_01_98", 100.0F), Map.entry("fencing_01_100", 100.0F), Map.entry("fencing_01_102", 100.0F), Map.entry("fencing_01_32", 100.0F), Map.entry("fencing_01_33", 100.0F), Map.entry("fencing_01_34", 100.0F), Map.entry("fencing_01_35", 100.0F), Map.entry("fencing_01_36", 100.0F), Map.entry("fencing_01_4", 60.0F), Map.entry("fencing_01_5", 60.0F), Map.entry("fencing_01_6", 60.0F), Map.entry("carpentry_02_40", 100.0F), Map.entry("carpentry_02_41", 100.0F), Map.entry("carpentry_02_42", 100.0F), Map.entry("carpentry_02_44", 100.0F), Map.entry("carpentry_02_45", 100.0F), Map.entry("carpentry_02_46", 100.0F), Map.entry("carpentry_02_48", 100.0F), Map.entry("carpentry_02_49", 100.0F), Map.entry("carpentry_02_50", 100.0F), Map.entry("fixtures_doors_fences_01_104", 100.0F), Map.entry("fixtures_doors_fences_01_105", 100.0F), Map.entry("fixtures_doors_fences_01_96", 100.0F), Map.entry("fixtures_doors_fences_01_97", 100.0F), Map.entry("fixtures_doors_fences_01_48", 100.0F), Map.entry("fixtures_doors_fences_01_49", 100.0F), Map.entry("fixtures_doors_fences_01_56", 100.0F), Map.entry("fixtures_doors_fences_01_57", 100.0F), Map.entry("fixtures_doors_fences_01_98", 100.0F), Map.entry("fixtures_doors_fences_01_99", 100.0F), Map.entry("fixtures_doors_fences_01_106", 100.0F), Map.entry("fixtures_doors_fences_01_107", 100.0F), Map.entry("fixtures_doors_fences_01_50", 100.0F), Map.entry("fixtures_doors_fences_01_51", 100.0F), Map.entry("fixtures_doors_fences_01_58", 100.0F), Map.entry("fixtures_doors_fences_01_59", 100.0F), Map.entry("fixtures_doors_fences_01_8", 100.0F), Map.entry("fixtures_doors_fences_01_9", 100.0F), Map.entry("fixtures_doors_fences_01_12", 100.0F), Map.entry("fixtures_doors_fences_01_13", 100.0F), Map.entry("fixtures_doors_fences_01_4", 100.0F), Map.entry("fixtures_doors_fences_01_5", 100.0F));
   public int spotSoundDelay;
   public float movex;
   public float movey;
   private int stepFrameLast;
   private final OnceEvery networkUpdate;
   public short lastRemoteUpdate;
   public short OnlineID;
   private static final ArrayList<IsoDeadBody> tempBodies = new ArrayList();
   float timeSinceRespondToSound;
   public String walkVariantUse;
   public String walkVariant;
   public boolean bLunger;
   public boolean bRunning;
   public boolean bCrawling;
   private boolean bCanCrawlUnderVehicle;
   private boolean bCanWalk;
   public boolean bRemote;
   private static final FloodFill floodFill = new FloodFill();
   public boolean ImmortalTutorialZombie;

   public String getObjectName() {
      return "Zombie";
   }

   public short getOnlineID() {
      return this.OnlineID;
   }

   public boolean isRemoteZombie() {
      return this.authOwner == null;
   }

   public UdpConnection getOwner() {
      return this.authOwner;
   }

   public IsoPlayer getOwnerPlayer() {
      return this.authOwnerPlayer;
   }

   public void setVehicle4TestCollision(BaseVehicle var1) {
      this.vehicle4testCollision = var1;
   }

   public IsoZombie(IsoCell var1) {
      this(var1, (SurvivorDesc)null, -1);
   }

   public IsoZombie(IsoCell var1, SurvivorDesc var2, int var3) {
      super(var1, 0.0F, 0.0F, 0.0F);
      this.alwaysKnockedDown = false;
      this.onlyJawStab = false;
      this.forceEatingAnimation = false;
      this.noTeeth = false;
      this.LastTargetSeenX = -1;
      this.LastTargetSeenY = -1;
      this.LastTargetSeenZ = -1;
      this.Ghost = false;
      this.LungeTimer = 0.0F;
      this.LungeSoundTime = 0L;
      this.TimeSinceSeenFlesh = 100000.0F;
      this.targetSeenTime = 0.0F;
      this.FollowCount = 0;
      this.ZombieID = 0;
      this.BonusSpotTime = 0.0F;
      this.bStaggerBack = false;
      this.bKnifeDeath = false;
      this.bJawStabAttach = false;
      this.bBecomeCrawler = false;
      this.bFakeDead = false;
      this.bForceFakeDead = false;
      this.bWasFakeDead = false;
      this.bReanimate = false;
      this.atlasTex = null;
      this.bReanimatedPlayer = false;
      this.bIndoorZombie = false;
      this.thumpFlag = 0;
      this.thumpSent = false;
      this.thumpCondition = 1.0F;
      this.useless = false;
      this.speedType = -1;
      this.inactive = false;
      this.strength = -1;
      this.cognition = -1;
      this.memory = -1;
      this.sight = -1;
      this.hearing = -1;
      this.itemsToSpawnAtDeath = null;
      this.voiceChoice = -1;
      this.soundReactDelay = 0.0F;
      this.delayedSound = new IsoGameCharacter.Location(-1, -1, -1);
      this.bSoundSourceRepeating = false;
      this.soundSourceTarget = null;
      this.soundAttract = 0.0F;
      this.soundAttractTimeout = 0.0F;
      this.hitAngle = new Vector2();
      this.alerted = false;
      this.walkType = null;
      this.footstepVolume = 1.0F;
      this.bDressInRandomOutfit = false;
      this.humanVisual = new HumanVisual(this);
      this.crawlerType = 0;
      this.playerAttackPosition = null;
      this.eatSpeed = 1.0F;
      this.sitAgainstWall = false;
      this.checkForCorpseTimer = 10000.0F;
      this.bodyToEat = null;
      this.hitTime = 0;
      this.thumpTimer = 0;
      this.hitLegsWhileOnFloor = false;
      this.collideWhileHit = true;
      this.m_characterTextureAnimTime = 0.0F;
      this.m_characterTextureAnimDuration = 1.0F;
      this.lastPlayerHit = -1;
      this.VISION_RADIUS_MAX = 20.0F;
      this.VISION_RADIUS_MIN = 10.0F;
      this.VISION_RADIUS_RESULT = 20.0F;
      this.VISION_FOG_PENALTY_MAX = 7.0F;
      this.VISION_RAIN_PENALTY_MAX = 2.5F;
      this.VISION_DARKNESS_PENALTY_MAX = 5.0F;
      this.HEARING_UNSEEN_OFFSET_MIN = 2;
      this.HEARING_UNSEEN_OFFSET_HEAVY_RAIN = 5;
      this.HEARING_UNSEEN_OFFSET_MAX = 10;
      this.itemVisuals = new ItemVisuals();
      this.hitHeadWhileOnFloor = 0;
      this.m_bAttackDidDamage = false;
      this.m_attackOutcome = "";
      this.m_reanimatedForGrappleOnly = false;
      this.imposter = new Imposter();
      this.vehicle4testCollision = null;
      this.SpriteName = "BobZ";
      this.vectorToTarget = new Vector2();
      this.AllowRepathDelay = 0.0F;
      this.KeepItReal = false;
      this.isSkeleton = false;
      this.parameterCharacterInside = new ParameterCharacterInside(this);
      this.parameterCharacterMovementSpeed = new ParameterCharacterMovementSpeed(this);
      this.parameterCharacterOnFire = new ParameterCharacterOnFire(this);
      this.parameterFootstepMaterial = new ParameterFootstepMaterial(this);
      this.parameterFootstepMaterial2 = new ParameterFootstepMaterial2(this);
      this.parameterPlayerDistance = new ParameterPlayerDistance(this);
      this.parameterShoeType = new ParameterShoeType(this);
      this.parameterVehicleHitLocation = new ParameterVehicleHitLocation();
      this.parameterZombieState = new ParameterZombieState(this);
      this.scratch = false;
      this.laceration = false;
      this.authOwner = null;
      this.authOwnerPlayer = null;
      this.zombiePacket = new ZombiePacket();
      this.zombiePacketUpdated = false;
      this.lastChangeOwner = -1L;
      this.bloodSplatAmount = 1000;
      this.palette = 0;
      this.AttackAnimTime = 50;
      this.spottedLast = null;
      this.aggroList = new Aggro[4];
      this.spotSoundDelay = 0;
      this.stepFrameLast = -1;
      this.networkUpdate = new OnceEvery(1.0F);
      this.lastRemoteUpdate = 0;
      this.OnlineID = -1;
      this.timeSinceRespondToSound = 1000000.0F;
      this.walkVariantUse = null;
      this.walkVariant = "ZombieWalk";
      this.bCanCrawlUnderVehicle = true;
      this.bCanWalk = true;
      this.getWrappedGrappleable().setOnGrappledEndCallback(this::onZombieGrappleEnded);
      this.registerVariableCallbacks();
      this.Health = 1.8F + Rand.Next(0.0F, 0.3F);
      this.weight = 0.7F;
      this.dir = IsoDirections.fromIndex(Rand.Next(8));
      this.humanVisual.randomBlood();
      if (var2 != null) {
         this.descriptor = var2;
         this.palette = var3;
      } else {
         this.descriptor = SurvivorFactory.CreateSurvivor();
         this.palette = Rand.Next(3) + 1;
      }

      this.setFemale(this.descriptor.isFemale());
      if (!this.descriptor.getVoicePrefix().contains("Zombie")) {
         this.descriptor.setVoicePrefix(Objects.equals(this.descriptor.getVoicePrefix(), "VoiceFemale") ? "FemaleZombie" : "MaleZombie");
      }

      this.SpriteName = this.isFemale() ? "KateZ" : "BobZ";
      if (this.palette != 1) {
         this.SpriteName = this.SpriteName + this.palette;
      }

      this.InitSpritePartsZombie();
      this.sprite.def.tintr = 0.95F + (float)Rand.Next(5) / 100.0F;
      this.sprite.def.tintg = 0.95F + (float)Rand.Next(5) / 100.0F;
      this.sprite.def.tintb = 0.95F + (float)Rand.Next(5) / 100.0F;
      this.setDefaultState(ZombieIdleState.instance());
      this.setFakeDead(false);
      this.DoZombieStats();
      this.width = 0.3F;
      this.setAlphaAndTarget(0.0F);
      this.finder.maxSearchDistance = 20;
      this.hurtSound = this.descriptor.getVoicePrefix() + "Hurt";
      this.initializeStates();
      this.getActionContext().setGroup(ActionGroup.getActionGroup("zombie"));
      this.initWornItems("Human");
      this.initAttachedItems("Human");
      this.networkAI = new NetworkZombieAI(this);
      this.clearAggroList();
   }

   public String toString() {
      String var10000 = this.getClass().getSimpleName();
      return var10000 + "{  Name:" + this.getName() + ",  ID:" + this.getID() + "  StateFlags:" + ZombieStateFlags.fromZombie(this) + "}";
   }

   public void initializeStates() {
      this.clearAIStateMap();
      this.registerAIState("attack-network", AttackNetworkState.instance());
      this.registerAIState("attackvehicle-network", IdleState.instance());
      this.registerAIState("fakedead-attack-network", IdleState.instance());
      this.registerAIState("lunge-network", LungeNetworkState.instance());
      this.registerAIState("walktoward-network", WalkTowardNetworkState.instance());
      this.registerAIState("grappledThrownOutWindow", GrappledThrownOutWindowState.instance());
      if (this.bCrawling) {
         this.registerAIState("attack", AttackState.instance());
         this.registerAIState("fakedead", FakeDeadZombieState.instance());
         this.registerAIState("fakedead-attack", FakeDeadAttackState.instance());
         this.registerAIState("getup", ZombieGetUpFromCrawlState.instance());
         this.registerAIState("hitreaction", ZombieHitReactionState.instance());
         this.registerAIState("hitreaction-knockeddown", ZombieHitReactionState.instance());
         this.registerAIState("hitreaction-shothead-bwd", ZombieFallDownState.instance());
         this.registerAIState("hitreaction-shothead-fwd", ZombieFallDownState.instance());
         this.registerAIState("hitreaction-shothead-fwd02", ZombieFallDownState.instance());
         this.registerAIState("hitreaction-onfloor", ZombieHitReactionState.instance());
         this.registerAIState("hitreaction-gettingup", ZombieHitReactionState.instance());
         this.registerAIState("hitreaction-whilesitting", ZombieHitReactionState.instance());
         this.registerAIState("hitreaction-hit", ZombieHitReactionState.instance());
         this.registerAIState("idle", ZombieIdleState.instance());
         this.registerAIState("onground", ZombieOnGroundState.instance());
         this.registerAIState("onground-ragdoll", ZombieOnGroundState.instance());
         this.registerAIState("pathfind", PathFindState.instance());
         this.registerAIState("reanimate", ZombieReanimateState.instance());
         this.registerAIState("staggerback", StaggerBackState.instance());
         this.registerAIState("staggerback-knockeddown", StaggerBackState.instance());
         this.registerAIState("vehicleCollision-staggerback", StaggerBackState.instance());
         this.registerAIState("thump", ThumpState.instance());
         this.registerAIState("turn", CrawlingZombieTurnState.instance());
         this.registerAIState("walktoward", WalkTowardState.instance());
      } else {
         this.registerAIState("attack", AttackState.instance());
         this.registerAIState("attackvehicle", AttackVehicleState.instance());
         this.registerAIState("bumped", BumpedState.instance());
         this.registerAIState("climbfence", ClimbOverFenceState.instance());
         this.registerAIState("climbwindow", ClimbThroughWindowState.instance());
         this.registerAIState("eatbody", ZombieEatBodyState.instance());
         this.registerAIState("falldown", ZombieFallDownState.instance());
         this.registerAIState("vehicleCollision-falldown", ZombieFallDownState.instance());
         this.registerAIState("falldown-eatingbody", ZombieFallDownState.instance());
         this.registerAIState("falldown-knifedeath", ZombieFallDownState.instance());
         this.registerAIState("falldown-onknees", ZombieFallDownState.instance());
         this.registerAIState("falldown-whilesitting", ZombieFallDownState.instance());
         this.registerAIState("falldown-headleft", ZombieFallDownState.instance());
         this.registerAIState("falldown-headright", ZombieFallDownState.instance());
         this.registerAIState("falldown-headtop", ZombieFallDownState.instance());
         this.registerAIState("falldown-speardeath1", ZombieFallDownState.instance());
         this.registerAIState("falldown-speardeath2", ZombieFallDownState.instance());
         this.registerAIState("falldown-uppercut", ZombieFallDownState.instance());
         this.registerAIState("falling", ZombieFallingState.instance());
         this.registerAIState("face-target", ZombieFaceTargetState.instance());
         this.registerAIState("fakedead", FakeDeadZombieState.instance());
         this.registerAIState("fakedead-attack", FakeDeadAttackState.instance());
         this.registerAIState("getdown", ZombieGetDownState.instance());
         this.registerAIState("getup", ZombieGetUpState.instance());
         this.registerAIState("getup-fromOnBack", ZombieGetUpState.instance());
         this.registerAIState("getup-fromOnFront", ZombieGetUpState.instance());
         this.registerAIState("getup-fromSitting", ZombieGetUpState.instance());
         this.registerAIState("hitreaction", ZombieHitReactionState.instance());
         this.registerAIState("hitreaction-knockeddown", ZombieHitReactionState.instance());
         this.registerAIState("hitreaction-shothead-bwd", ZombieFallDownState.instance());
         this.registerAIState("hitreaction-shothead-fwd", ZombieFallDownState.instance());
         this.registerAIState("hitreaction-shothead-fwd02", ZombieFallDownState.instance());
         this.registerAIState("hitreaction-onfloor", ZombieHitReactionState.instance());
         this.registerAIState("hitreaction-gettingup", ZombieHitReactionState.instance());
         this.registerAIState("hitreaction-whilesitting", ZombieHitReactionState.instance());
         this.registerAIState("hitreaction-hit", ZombieHitReactionState.instance());
         this.registerAIState("idle", ZombieIdleState.instance());
         this.registerAIState("lunge", LungeState.instance());
         this.registerAIState("onground", ZombieOnGroundState.instance());
         this.registerAIState("onground-ragdoll", ZombieOnGroundState.instance());
         this.registerAIState("pathfind", PathFindState.instance());
         this.registerAIState("sitting", ZombieSittingState.instance());
         this.registerAIState("staggerback", StaggerBackState.instance());
         this.registerAIState("staggerback-knockeddown", StaggerBackState.instance());
         this.registerAIState("vehicleCollision-staggerback", StaggerBackState.instance());
         this.registerAIState("thump", ThumpState.instance());
         this.registerAIState("turnalerted", ZombieTurnAlerted.instance());
         this.registerAIState("walktoward", WalkTowardState.instance());
         this.registerAIState("grappled", ZombieIdleState.instance());
      }

   }

   private void registerVariableCallbacks() {
      this.setVariable("bClient", () -> {
         return GameClient.bClient && this.isRemoteZombie();
      });
      this.setVariable("bMovingNetwork", () -> {
         return (this.isLocal() || !this.isBumped()) && (IsoUtils.DistanceManhatten(this.networkAI.targetX, this.networkAI.targetY, this.getX(), this.getY()) > 0.5F || this.getZ() != (float)this.networkAI.targetZ);
      });
      this.setVariable("hitHeadType", this::getHitHeadWhileOnFloor);
      this.setVariable("realState", this::getRealState);
      this.setVariable("battack", this::getShouldAttack);
      this.setVariable("isFacingTarget", this::isFacingTarget);
      this.setVariable("targetSeenTime", this::getTargetSeenTime);
      this.setVariable("battackvehicle", () -> {
         if (this.getVariableBoolean("bPathfind")) {
            return false;
         } else if (this.isMoving()) {
            return false;
         } else if (this.target == null) {
            return false;
         } else if (Math.abs(this.target.getZ() - this.getZ()) >= 0.8F) {
            return false;
         } else if (this.target instanceof IsoPlayer && ((IsoPlayer)this.target).isGhostMode()) {
            return false;
         } else if (!(this.target instanceof IsoGameCharacter)) {
            return false;
         } else {
            BaseVehicle var1 = ((IsoGameCharacter)this.target).getVehicle();
            return var1 != null && var1.isCharacterAdjacentTo(this);
         }
      });
      this.setVariable("beatbodytarget", () -> {
         if (this.isForceEatingAnimation()) {
            return true;
         } else {
            if (!GameServer.bServer) {
               this.updateEatBodyTarget();
            }

            return this.getEatBodyTarget() != null;
         }
      });
      this.setVariable("bbecomecrawler", this::isBecomeCrawler, this::setBecomeCrawler);
      this.setVariable("bfakedead", () -> {
         return this.bFakeDead;
      });
      this.setVariable("bfalling", () -> {
         return this.getZ() > 0.0F && this.fallTime > 2.0F;
      });
      this.setVariable("bhastarget", () -> {
         if (this.target instanceof IsoGameCharacter && ((IsoGameCharacter)this.target).ReanimatedCorpse != null) {
            this.setTarget((IsoMovingObject)null);
         }

         return this.target != null;
      });
      this.setVariable("shouldSprint", () -> {
         if (this.target instanceof IsoGameCharacter && ((IsoGameCharacter)this.target).ReanimatedCorpse != null) {
            this.setTarget((IsoMovingObject)null);
         }

         return this.target != null || this.soundSourceTarget != null && !(this.soundSourceTarget instanceof IsoZombie);
      });
      this.setVariable("bknockeddown", this::isKnockedDown);
      this.setVariable("blunge", () -> {
         if (this.target == null) {
            return false;
         } else if (PZMath.fastfloor(this.getZ()) != PZMath.fastfloor(this.target.getZ())) {
            return false;
         } else {
            if (this.target instanceof IsoGameCharacter) {
               if (((IsoGameCharacter)this.target).getVehicle() != null) {
                  return false;
               }

               if (((IsoGameCharacter)this.target).ReanimatedCorpse != null) {
                  return false;
               }
            }

            if (this.target instanceof IsoPlayer && ((IsoPlayer)this.target).isGhostMode()) {
               this.setTarget((IsoMovingObject)null);
               return false;
            } else {
               IsoGridSquare var1 = this.getCurrentSquare();
               IsoGridSquare var2 = this.target.getCurrentSquare();
               if (var2 != null && var2.isSomethingTo(var1) && this.getThumpTarget() != null) {
                  return false;
               } else if (this.isCurrentState(ZombieTurnAlerted.instance()) && !this.isFacingTarget()) {
                  return false;
               } else {
                  float var3 = this.vectorToTarget.getLength();
                  return var3 > 3.5F && (!(var3 <= 4.0F) || !(this.target instanceof IsoGameCharacter) || ((IsoGameCharacter)this.target).getVehicle() == null) ? false : !PolygonalMap2.instance.lineClearCollide(this.getX(), this.getY(), this.target.getX(), this.target.getY(), PZMath.fastfloor(this.getZ()), this.target, false, true);
               }
            }
         }
      });
      this.setVariable("bpassengerexposed", () -> {
         return AttackVehicleState.instance().isPassengerExposed(this);
      });
      this.setVariable("bistargetissmallvehicle", () -> {
         return this.target != null && this.target instanceof IsoPlayer && ((IsoPlayer)this.target).getVehicle() != null ? ((IsoPlayer)this.target).getVehicle().getScript().isSmallVehicle : true;
      });
      this.setVariable("breanimate", this::isReanimate, this::setReanimate);
      this.setVariable("reanimatedForGrappleOnly", this::isReanimatedForGrappleOnly, this::setReanimatedForGrappleOnly);
      this.setVariable("bstaggerback", this::isStaggerBack);
      this.setVariable("bthump", () -> {
         if (this.getThumpTarget() instanceof IsoObject && !(this.getThumpTarget() instanceof BaseVehicle)) {
            IsoObject var1 = (IsoObject)this.getThumpTarget();
            if (var1.getSquare() == null || this.DistToSquared(var1.getX() + 0.5F, var1.getY() + 0.5F) > 9.0F) {
               this.setThumpTarget((Thumpable)null);
            }
         }

         if (this.getThumpTimer() > 0) {
            this.setThumpTarget((Thumpable)null);
         }

         return this.getThumpTarget() != null;
      });
      this.setVariable("bundervehicle", this::isUnderVehicle);
      this.setVariable("bBeingSteppedOn", this::isBeingSteppedOn);
      this.setVariable("distancetotarget", () -> {
         return this.target == null ? "" : String.valueOf(this.vectorToTarget.getLength() - this.getWidth() + this.target.getWidth());
      });
      this.setVariable("lasttargetseen", () -> {
         return this.LastTargetSeenX != -1;
      });
      this.setVariable("lungetimer", () -> {
         return this.LungeTimer;
      });
      this.setVariable("reanimatetimer", this::getReanimateTimer);
      this.setVariable("stateeventdelaytimer", this::getStateEventDelayTimer);
      this.setVariable("turndirection", () -> {
         if (this.getPath2() != null) {
            return "";
         } else {
            IsoDirections var1;
            boolean var2;
            if (this.target != null && this.vectorToTarget.getLength() != 0.0F) {
               if (GameClient.bClient && this.isRemoteZombie()) {
                  tempo.set(this.networkAI.targetX - this.getX(), this.networkAI.targetY - this.getY());
               } else {
                  tempo.set(this.vectorToTarget);
               }

               var1 = IsoDirections.fromAngle(tempo);
               if (this.dir == var1) {
                  return "";
               } else {
                  var2 = CrawlingZombieTurnState.calculateDir(this, var1);
                  return var2 ? "left" : "right";
               }
            } else if (this.isCurrentState(WalkTowardState.instance())) {
               WalkTowardState.instance().calculateTargetLocation(this, tempo);
               Vector2 var10000 = tempo;
               var10000.x -= this.getX();
               var10000 = tempo;
               var10000.y -= this.getY();
               var1 = IsoDirections.fromAngle(tempo);
               if (this.dir == var1) {
                  return "";
               } else {
                  var2 = CrawlingZombieTurnState.calculateDir(this, var1);
                  return var2 ? "left" : "right";
               }
            } else {
               if (this.isCurrentState(PathFindState.instance())) {
               }

               return "";
            }
         }
      });
      this.setVariable("alerted", () -> {
         return this.alerted;
      });
      this.setVariable("zombiewalktype", () -> {
         return this.walkType;
      });
      this.setVariable("crawlertype", () -> {
         return this.crawlerType;
      });
      this.setVariable("bGetUpFromCrawl", this::shouldGetUpFromCrawl);
      this.setVariable("playerattackposition", this::getPlayerAttackPosition);
      this.setVariable("eatspeed", () -> {
         return this.eatSpeed;
      });
      this.setVariable("issitting", this::isSitAgainstWall);
      this.setVariable("bKnifeDeath", this::isKnifeDeath, this::setKnifeDeath);
      this.setVariable("bJawStabAttach", this::isJawStabAttach, this::setJawStabAttach);
      this.setVariable("bPathFindPrediction", () -> {
         return NetworkVariables.PredictionTypes.PathFind.equals(this.networkAI.predictionType);
      });
      this.setVariable("bCrawling", this::isCrawling, this::setCrawler);
      this.setVariable("AttackDidDamage", this::getAttackDidDamage, this::setAttackDidDamage);
      this.setVariable("AttackOutcome", this::getAttackOutcome, this::setAttackOutcome);
   }

   protected void OnAnimEvent_IsAlmostUp(IsoGameCharacter var1) {
      super.OnAnimEvent_IsAlmostUp(var1);
      if (var1 == this) {
         this.setSitAgainstWall(false);
      }

   }

   private boolean getShouldAttack() {
      if (SystemDisabler.zombiesDontAttack) {
         return false;
      } else if (this.isReanimatedForGrappleOnly()) {
         return false;
      } else if (this.target != null && !this.target.isZombiesDontAttack()) {
         if (this.target instanceof IsoGameCharacter) {
            if (this.target.isOnFloor() && ((IsoGameCharacter)this.target).getCurrentState() != BumpedState.instance()) {
               this.setTarget((IsoMovingObject)null);
               return false;
            }

            BaseVehicle var1 = ((IsoGameCharacter)this.target).getVehicle();
            if (var1 != null) {
               return false;
            }

            if (((IsoGameCharacter)this.target).ReanimatedCorpse != null) {
               return false;
            }

            if (((IsoGameCharacter)this.target).getStateMachine().getCurrent() == ClimbOverWallState.instance()) {
               return false;
            }
         }

         if (this.bReanimate) {
            return false;
         } else if (Math.abs(this.target.getZ() - this.getZ()) >= 0.2F) {
            return false;
         } else if (this.target instanceof IsoPlayer && ((IsoPlayer)this.target).isGhostMode()) {
            return false;
         } else if (this.bFakeDead) {
            return !this.isUnderVehicle() && this.DistTo(this.target) < 1.3F;
         } else if (!this.bCrawling) {
            IsoGridSquare var5 = this.getCurrentSquare();
            IsoGridSquare var2 = this.target.getCurrentSquare();
            if (var5 != null && var5.isSomethingTo(var2)) {
               return false;
            } else {
               float var3 = this.bCrawling ? 1.4F : 0.72F;
               float var4 = this.vectorToTarget.getLength();
               return var4 <= var3;
            }
         } else {
            return !this.isUnderVehicle() && this.DistTo(this.target) < 1.3F;
         }
      } else {
         return false;
      }
   }

   public void actionStateChanged(ActionContext var1) {
      super.actionStateChanged(var1);
      if (this.networkAI != null && GameServer.bServer) {
         this.networkAI.extraUpdate();
      }

   }

   protected void onAnimPlayerCreated(AnimationPlayer var1) {
      super.onAnimPlayerCreated(var1);
      var1.setSharedAnimRepo(m_sharedSkeleRepo);
   }

   public String GetAnimSetName() {
      return this.bCrawling ? "zombie-crawler" : "zombie";
   }

   public void InitSpritePartsZombie() {
      SurvivorDesc var1 = this.descriptor;
      this.InitSpritePartsZombie(var1);
   }

   public void InitSpritePartsZombie(SurvivorDesc var1) {
      this.sprite.disposeAnimation();
      this.legsSprite = this.sprite;
      this.legsSprite.name = var1.torso;
      this.ZombieID = Rand.Next(10000);
      this.bUseParts = true;
   }

   public void pathToCharacter(IsoGameCharacter var1) {
      if (!(this.AllowRepathDelay > 0.0F) || !this.isCurrentState(PathFindState.instance()) && !this.isCurrentState(WalkTowardState.instance()) && !this.isCurrentState(WalkTowardNetworkState.instance())) {
         super.pathToCharacter(var1);
      }
   }

   public void pathToLocationF(float var1, float var2, float var3) {
      if (!(this.AllowRepathDelay > 0.0F) || !this.isCurrentState(PathFindState.instance()) && !this.isCurrentState(WalkTowardState.instance()) && !this.isCurrentState(WalkTowardNetworkState.instance())) {
         super.pathToLocationF(var1, var2, var3);
      }
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      super.load(var1, var2, var3);
      this.walkVariant = "ZombieWalk";
      this.SpriteName = "BobZ";
      if (this.palette != 1) {
         this.SpriteName = this.SpriteName + this.palette;
      }

      SurvivorDesc var4 = this.descriptor;
      this.setFemale(var4.isFemale());
      if (this.isFemale()) {
         if (this.palette == 1) {
            this.SpriteName = "KateZ";
         } else {
            this.SpriteName = "KateZ" + this.palette;
         }
      }

      this.hurtSound = var4.getVoicePrefix() + "Hurt";
      this.InitSpritePartsZombie(var4);
      this.sprite.def.tintr = 0.95F + (float)Rand.Next(5) / 100.0F;
      this.sprite.def.tintg = 0.95F + (float)Rand.Next(5) / 100.0F;
      this.sprite.def.tintb = 0.95F + (float)Rand.Next(5) / 100.0F;
      this.setDefaultState(ZombieIdleState.instance());
      this.DoZombieStats();
      int var5 = var1.getInt();
      this.setWidth(0.3F);
      this.TimeSinceSeenFlesh = (float)var1.getInt();
      this.setAlpha(0.0F);
      int var6 = var1.getInt();
      if (var5 == 0) {
         DebugLog.Zombie.debugln("Loading Zombie isFakeDead state:%d", var6);
         this.setFakeDead(var6 == 1);
      } else if (var5 >= 1) {
         ZombieStateFlags var7 = ZombieStateFlags.fromInt(var6);
         DebugLog.Zombie.debugln("Loading Zombie state flags:%s", var7);
         if (var7.isFakeDead()) {
            this.setFakeDead(true);
         } else if (var7.isCrawling()) {
            this.setCrawler(true);
            this.setCanWalk(var7.isCanWalk());
            this.setOnFloor(true);
            this.setFallOnFront(true);
            this.walkVariant = "ZombieWalk";
            this.DoZombieStats();
         }

         if (var7.isInitialized()) {
            this.setCanCrawlUnderVehicle(var7.isCanCrawlUnderVehicle());
         }

         this.setReanimatedForGrappleOnly(var7.isReanimatedForGrappleOnly());
      }

      ArrayList var12 = this.savedInventoryItems;
      byte var8 = var1.get();

      for(int var9 = 0; var9 < var8; ++var9) {
         String var10 = GameWindow.ReadString(var1);
         short var11 = var1.getShort();
         if (var11 >= 0 && var11 < var12.size() && this.wornItems.getBodyLocationGroup().getLocation(var10) != null) {
            this.wornItems.setItem(var10, (InventoryItem)var12.get(var11));
         }
      }

      this.setStateMachineLocked(false);
      this.setDefaultState();
      this.getCell().getZombieList().add(this);
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      DebugLog.Saving.trace("Saving: %s", this);
      super.save(var1, var2);
      var1.putInt(1);
      var1.putInt((int)this.TimeSinceSeenFlesh);
      ZombieStateFlags var3 = ZombieStateFlags.fromZombie(this);
      var1.putInt(var3.asInt());
      if (this.wornItems.size() > 127) {
         throw new RuntimeException("too many worn items");
      } else {
         var1.put((byte)this.wornItems.size());
         this.wornItems.forEach((var2x) -> {
            GameWindow.WriteString(var1, var2x.getLocation());
            var1.putShort((short)this.savedInventoryItems.indexOf(var2x.getItem()));
         });
      }
   }

   public void collideWith(IsoObject var1) {
      if (!this.Ghost && var1 != null) {
         if (var1.rerouteCollide != null) {
            var1 = this.rerouteCollide;
         }

         State var2 = this.getCurrentState();
         boolean var3 = this.isCurrentState(PathFindState.instance()) || this.isCurrentState(LungeState.instance()) || this.isCurrentState(LungeNetworkState.instance()) || this.isCurrentState(WalkTowardState.instance()) || this.isCurrentState(WalkTowardNetworkState.instance());
         IsoWindow var4 = (IsoWindow)Type.tryCastTo(var1, IsoWindow.class);
         if (var4 != null && var4.canClimbThrough(this) && var3) {
            if (!this.isFacingObject(var4, 0.8F)) {
               super.collideWith(var1);
               return;
            }

            if (var2 != PathFindState.instance() && !this.bCrawling) {
               this.climbThroughWindow(var4);
            }
         } else {
            if (var3 && !this.bCrawling && var1 instanceof IsoWindowFrame) {
               IsoWindowFrame var5 = (IsoWindowFrame)var1;
               if (var5.canClimbThrough(this)) {
                  if (!this.isFacingObject(var5, 0.8F)) {
                     super.collideWith(var1);
                     return;
                  }

                  if (var2 != PathFindState.instance()) {
                     this.climbThroughWindowFrame(var5);
                  }

                  return;
               }
            }

            if (var1 instanceof IsoThumpable && ((IsoThumpable)var1).canClimbThrough(this) && var3) {
               if (var2 != PathFindState.instance() && !this.bCrawling) {
                  this.climbThroughWindow((IsoThumpable)var1);
               }
            } else if ((!(var1 instanceof IsoDoor) || !((IsoDoor)var1).isHoppable()) && var1 != null && var1.getThumpableFor(this) != null && var3) {
               boolean var6 = (this.isCurrentState(PathFindState.instance()) || this.isCurrentState(WalkTowardState.instance()) || this.isCurrentState(WalkTowardNetworkState.instance())) && this.getPathFindBehavior2().isGoalSound();
               if (!SandboxOptions.instance.Lore.ThumpNoChasing.getValue() && this.target == null && !var6) {
                  this.setVariable("bPathfind", false);
                  this.setVariable("bMoving", false);
               } else {
                  if (var1 instanceof IsoThumpable && !SandboxOptions.instance.Lore.ThumpOnConstruction.getValue()) {
                     return;
                  }

                  Object var7 = var1;
                  if (var1 instanceof IsoWindow && var1.getThumpableFor(this) != null && var1.isDestroyed()) {
                     var7 = var1.getThumpableFor(this);
                  }

                  this.setThumpTarget((Thumpable)var7);
               }

               this.setPath2((Path)null);
            }
         }

         super.collideWith(var1);
      }
   }

   public float Hit(HandWeapon var1, IsoGameCharacter var2, float var3, boolean var4, float var5, boolean var6) {
      if ((Core.bTutorial || Core.bDebug) && this.ImmortalTutorialZombie) {
         return 0.0F;
      } else {
         BodyPartType var7 = BodyPartType.FromIndex(Rand.Next(BodyPartType.ToIndex(BodyPartType.Torso_Upper), BodyPartType.ToIndex(BodyPartType.Torso_Lower) + 1));
         if (Rand.NextBool(7)) {
            var7 = BodyPartType.Head;
         }

         if (var2.isCriticalHit() && Rand.NextBool(3)) {
            var7 = BodyPartType.Head;
         }

         LuaEventManager.triggerEvent("OnHitZombie", this, var2, var7, var1);
         this.lastHitPart = var7;
         var2.setLastHitCharacter(this);
         float var8 = super.Hit(var1, var2, var3, var4, var5, var6);
         if (GameServer.bServer && !this.isRemoteZombie()) {
            this.addAggro(var2, var8);
         }

         this.TimeSinceSeenFlesh = 0.0F;
         if (!this.isDead() && !this.isOnFloor() && !var4 && var1 != null && var1.getScriptItem().getCategories().contains("Blade") && var2 instanceof IsoPlayer && this.DistToProper(var2) <= 0.9F && (this.isCurrentState(AttackState.instance()) || this.isCurrentState(AttackNetworkState.instance()) || this.isCurrentState(LungeState.instance()) || this.isCurrentState(LungeNetworkState.instance()))) {
            this.setHitForce(0.5F);
            this.changeState(StaggerBackState.instance());
         }

         if (GameServer.bServer || GameClient.bClient && this.isDead()) {
            this.lastPlayerHit = var2.getOnlineID();
         }

         return var8;
      }
   }

   public void onMouseLeftClick() {
      if (IsoPlayer.getInstance() != null && !IsoPlayer.getInstance().isAiming()) {
         if (IsoPlayer.getInstance().IsAttackRange(this.getX(), this.getY(), this.getZ())) {
            Vector2 var1 = new Vector2(this.getX(), this.getY());
            var1.x -= IsoPlayer.getInstance().getX();
            var1.y -= IsoPlayer.getInstance().getY();
            var1.normalize();
            IsoPlayer.getInstance().DirectionFromVector(var1);
            IsoPlayer.getInstance().AttemptAttack();
         }

      }
   }

   public void onZombieGrappleEnded() {
      if (this.isReanimatedForGrappleOnly()) {
         DebugLog.Grapple.debugln("Reanimated Corpse Dropped. Dying again.");
         this.die();
      }

   }

   private void renderAtlasTexture(float var1, float var2, float var3) {
      if (this.atlasTex != null) {
         if (IsoSprite.globalOffsetX == -1.0F) {
            IsoSprite.globalOffsetX = -IsoCamera.frameState.OffX;
            IsoSprite.globalOffsetY = -IsoCamera.frameState.OffY;
         }

         float var4 = IsoUtils.XToScreen(var1, var2, var3, 0);
         float var5 = IsoUtils.YToScreen(var1, var2, var3, 0);
         this.sx = var4;
         this.sy = var5;
         var4 = this.sx + IsoSprite.globalOffsetX;
         var5 = this.sy + IsoSprite.globalOffsetY;
         ColorInfo var6 = inf.set(1.0F, 1.0F, 1.0F, 1.0F);
         if (PerformanceSettings.LightingFrameSkip < 3 && this.getCurrentSquare() != null) {
            this.getCurrentSquare().interpolateLight(var6, var1 - (float)this.getCurrentSquare().getX(), var2 - (float)this.getCurrentSquare().getY());
         }

         this.atlasTex.render(var1, var2, var3, (float)((int)var4), (float)((int)var5), var6.r, var6.g, var6.b, var6.a);
      }
   }

   public void render(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      if (this.getCurrentState() == FakeDeadZombieState.instance()) {
         if (this.bDressInRandomOutfit) {
            ModelManager.instance.dressInRandomOutfit(this);
         }

         if (this.atlasTex == null) {
            this.atlasTex = DeadBodyAtlas.instance.getBodyTexture(this);
            DeadBodyAtlas.instance.render();
         }

         if (this.atlasTex != null) {
            this.renderAtlasTexture(var1, var2, var3);
         }

      } else {
         if (this.atlasTex != null) {
            this.atlasTex = null;
         }

         if (IsoCamera.getCameraCharacter() != IsoPlayer.getInstance()) {
            this.setAlphaAndTarget(1.0F);
         }

         super.render(var1, var2, var3, var4, var5, var6, var7);
      }
   }

   public void renderlast() {
      super.renderlast();
      if (DebugOptions.instance.ZombieRenderCanCrawlUnderVehicle.getValue() && this.isCanCrawlUnderVehicle()) {
         this.renderTextureOverHead("media/ui/FavoriteStar.png");
      }

      if (DebugOptions.instance.ZombieRenderMemory.getValue()) {
         String var1;
         if (this.target == null) {
            var1 = "media/ui/Moodles/Moodle_Icon_Bored.png";
         } else if (this.BonusSpotTime == 0.0F) {
            var1 = "media/ui/Moodles/Moodle_Icon_Angry.png";
         } else {
            var1 = "media/ui/Moodles/Moodle_Icon_Zombie.png";
         }

         this.renderTextureOverHead(var1);
         int var2 = (int)IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
         int var3 = (int)IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
         int var4 = TextManager.instance.getFontFromEnum(UIFont.Small).getLineHeight();
         TextManager.instance.DrawString((double)var2, (double)(var3 += var4), "AllowRepathDelay : " + this.AllowRepathDelay);
         TextManager.instance.DrawString((double)var2, (double)(var3 += var4), "BonusSpotTime : " + this.BonusSpotTime);
         TextManager.instance.DrawString((double)var2, (double)(var3 + var4), "TimeSinceSeenFlesh : " + this.TimeSinceSeenFlesh);
      }

      if (DebugOptions.instance.ZombieRenderViewDistance.getValue()) {
         if (this.target == null) {
            this.updateVisionRadius();
            LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), this.VISION_RADIUS_RESULT, 32, 1.0F, 1.0F, 1.0F, 0.3F);
         } else if (this.BonusSpotTime == 0.0F) {
            LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), this.VISION_RADIUS_RESULT, 32, 1.0F, 1.0F, 0.0F, 0.3F);
         } else {
            LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), this.VISION_RADIUS_RESULT, 32, 1.0F, 0.0F, 0.0F, 0.3F);
         }
      }

   }

   protected boolean renderTextureInsteadOfModel(float var1, float var2) {
      boolean var3 = this.isCurrentState(WalkTowardState.instance()) || this.isCurrentState(PathFindState.instance());
      String var4 = "zombie";
      String var5 = var3 ? "walktoward" : "idle";
      byte var6 = 4;
      int var7 = (int)(this.m_characterTextureAnimTime / this.m_characterTextureAnimDuration * (float)var6);
      float var8 = (var3 ? 0.67F : 1.0F) * ((float)var7 / (float)var6);
      DeadBodyAtlas.BodyTexture var9 = DeadBodyAtlas.instance.getBodyTexture(this.isFemale(), var4, var5, this.getDir(), var7, var8);
      if (var9 != null) {
         float var10 = IsoUtils.XToScreen(var1, var2, this.getZ(), 0);
         float var11 = IsoUtils.YToScreen(var1, var2, this.getZ(), 0);
         var10 -= IsoCamera.getOffX();
         var11 -= IsoCamera.getOffY();
         int var12 = IsoCamera.frameState.playerIndex;
         if (PerformanceSettings.FBORenderChunk) {
            IndieGL.enableDepthTest();
            IndieGL.glBlendFunc(770, 771);
            IndieGL.glDepthFunc(515);
            IndieGL.StartShader(0);
            TextureDraw.nextZ = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), var1 + 0.3F, var2 + 0.3F, this.getZ() + 0.3F).depthStart * 2.0F - 1.0F;
         }

         var9.render(var1, var2, this.getZ(), var10, var11, 0.0F, 0.0F, 0.0F, this.getAlpha(var12));
      }

      if (DebugOptions.instance.Character.Debug.Render.Angle.getValue()) {
         tempo.set(this.dir.ToVector());
         this.drawDirectionLine(tempo, 1.2F, 0.0F, 1.0F, 0.0F);
      }

      return true;
   }

   private void renderTextureOverHead(String var1) {
      float var2 = this.getX();
      float var3 = this.getY();
      float var4 = IsoUtils.XToScreen(var2, var3, this.getZ(), 0);
      float var5 = IsoUtils.YToScreen(var2, var3, this.getZ(), 0);
      var4 = var4 - IsoCamera.getOffX() - this.offsetX;
      var5 = var5 - IsoCamera.getOffY() - this.offsetY;
      var5 -= (float)(128 / (2 / Core.TileScale));
      Texture var6 = Texture.getSharedTexture(var1);
      float var7 = Core.getInstance().getZoom(IsoCamera.frameState.playerIndex);
      var7 = Math.max(var7, 1.0F);
      int var8 = (int)((float)var6.getWidth() * var7);
      int var9 = (int)((float)var6.getHeight() * var7);
      var6.render((float)((int)var4 - var8 / 2), (float)((int)var5 - var9), (float)var8, (float)var9);
   }

   protected void updateAlpha(int var1, float var2, float var3) {
      if (this.isFakeDead()) {
         this.setAlphaAndTarget(1.0F);
      } else {
         super.updateAlpha(var1, var2, var3);
      }
   }

   public boolean allowsTwist() {
      return false;
   }

   public void RespondToSound() {
      if (!this.Ghost) {
         if (!this.isUseless()) {
            if (!GameServer.bServer) {
               if (!GameClient.bClient || !this.isRemoteZombie()) {
                  float var1;
                  if ((this.getCurrentState() == PathFindState.instance() || this.getCurrentState() == WalkTowardState.instance()) && this.getPathFindBehavior2().isGoalSound() && PZMath.fastfloor(this.getZ()) == this.getPathTargetZ() && this.bSoundSourceRepeating) {
                     var1 = this.DistToSquared((float)this.getPathTargetX(), (float)this.getPathTargetY());
                     if (var1 < 16.0F && LosUtil.lineClear(this.getCell(), PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), this.getPathTargetX(), this.getPathTargetY(), PZMath.fastfloor(this.getZ()), false) != LosUtil.TestResults.Blocked && !this.isNearSirenVehicle()) {
                        this.setVariable("bPathfind", false);
                        this.setVariable("bMoving", false);
                        this.setPath2((Path)null);
                     }
                  }

                  if (this.soundReactDelay > 0.0F) {
                     this.soundReactDelay -= GameTime.getInstance().getThirtyFPSMultiplier();
                     if (this.soundReactDelay < 0.0F) {
                        this.soundReactDelay = 0.0F;
                     }

                     if (this.soundReactDelay > 0.0F) {
                        return;
                     }
                  }

                  var1 = 0.0F;
                  Object var2 = null;
                  WorldSoundManager.WorldSound var3 = WorldSoundManager.instance.getSoundZomb(this);
                  float var4 = WorldSoundManager.instance.getSoundAttract(var3, this);
                  if (var4 <= 0.0F) {
                     var3 = null;
                  }

                  if (var3 != null) {
                     var1 = var4;
                     var2 = var3.source;
                     this.soundAttract = var4;
                     this.soundAttractTimeout = 60.0F;
                  } else if (this.soundAttractTimeout > 0.0F) {
                     this.soundAttractTimeout -= GameTime.getInstance().getThirtyFPSMultiplier();
                     if (this.soundAttractTimeout < 0.0F) {
                        this.soundAttractTimeout = 0.0F;
                     }
                  }

                  WorldSoundManager.ResultBiggestSound var5 = WorldSoundManager.instance.getBiggestSoundZomb(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), true, this);
                  if (var5.sound != null && (this.soundAttractTimeout == 0.0F || this.soundAttract * 2.0F < var5.attract)) {
                     var3 = var5.sound;
                     var1 = var5.attract;
                     var2 = var3.source;
                  }

                  if (var3 != null && var3.bRepeating && var3.z == PZMath.fastfloor(this.getZ())) {
                     float var6 = this.DistToSquared((float)var3.x, (float)var3.y);
                     if (var6 < 25.0F && LosUtil.lineClear(this.getCell(), PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), var3.x, var3.y, PZMath.fastfloor(this.getZ()), false) != LosUtil.TestResults.Blocked) {
                        var3 = null;
                     }
                  }

                  if (var3 != null) {
                     this.soundAttract = var1;
                     this.soundSourceTarget = var2;
                     this.soundReactDelay = (float)Rand.Next(0, 16);
                     this.delayedSound.x = var3.x;
                     this.delayedSound.y = var3.y;
                     this.delayedSound.z = var3.z;
                     this.bSoundSourceRepeating = var3.bRepeating;
                  }

                  if (this.delayedSound.x != -1 && this.soundReactDelay == 0.0F) {
                     int var13 = this.delayedSound.x;
                     int var7 = this.delayedSound.y;
                     int var8 = this.delayedSound.z;
                     this.delayedSound.x = -1;
                     float var9 = IsoUtils.DistanceManhatten(this.getX(), this.getY(), (float)var13, (float)var7) / 2.5F;
                     var13 += Rand.Next((int)(-var9), (int)var9);
                     var7 += Rand.Next((int)(-var9), (int)var9);
                     if ((this.getCurrentState() == PathFindState.instance() || this.getCurrentState() == WalkTowardState.instance()) && (this.getPathFindBehavior2().isGoalLocation() || this.getPathFindBehavior2().isGoalSound())) {
                        if (!IsoUtils.isSimilarDirection(this, (float)var13, (float)var7, this.getPathFindBehavior2().getTargetX(), this.getPathFindBehavior2().getTargetY(), 0.5F)) {
                           this.setTurnAlertedValues(var13, var7);
                           this.pathToSound(var13, var7, var8);
                           this.setLastHeardSound(this.getPathTargetX(), this.getPathTargetY(), this.getPathTargetZ());
                           this.AllowRepathDelay = 120.0F;
                           this.timeSinceRespondToSound = 0.0F;
                        }

                        return;
                     }

                     if (this.timeSinceRespondToSound < 60.0F) {
                        return;
                     }

                     if (!IsoUtils.isSimilarDirection(this, (float)var13, (float)var7, this.getX() + this.getForwardDirection().x, this.getY() + this.getForwardDirection().y, 0.5F)) {
                        this.setTurnAlertedValues(var13, var7);
                     }

                     if (LosUtil.lineClear(this.getCell(), (int)this.getX(), (int)this.getY(), (int)this.getZ(), var13, var7, var8, false) != LosUtil.TestResults.Blocked) {
                        this.pathToSound(var13, var7, var8);
                        this.setLastHeardSound(this.getPathTargetX(), this.getPathTargetY(), this.getPathTargetZ());
                     } else {
                        int var10 = 2;
                        if (ClimateManager.getInstance().getRainIntensity() > 0.5F) {
                           var10 = 5;
                        }

                        if (SandboxOptions.instance.Lore.Hearing.getValue() == 1) {
                           var10 -= 2;
                        }

                        if (SandboxOptions.instance.Lore.Hearing.getValue() == 3) {
                           var10 += 2;
                        }

                        var10 = Math.max(Math.min(10, var10), 2);
                        int var11 = -var10 + (int)(Math.random() * (double)(var10 * 2 + 1));
                        int var12 = -var10 + (int)(Math.random() * (double)(var10 * 2 + 1));
                        this.pathToSound(var13 + var11, var7 + var12, var8);
                        this.setLastHeardSound(this.getPathTargetX() + var11, this.getPathTargetY() + var12, this.getPathTargetZ());
                     }

                     this.AllowRepathDelay = 120.0F;
                     this.timeSinceRespondToSound = 0.0F;
                  }

               }
            }
         }
      }
   }

   public void setTurnAlertedValues(int var1, int var2) {
      Vector2 var3 = new Vector2(this.getX() - ((float)var1 + 0.5F), this.getY() - ((float)var2 + 0.5F));
      float var4 = var3.getDirectionNeg();
      if (var4 < 0.0F) {
         var4 = Math.abs(var4);
      } else {
         var4 = (float)(6.283185307179586 - (double)var4);
      }

      double var5 = Math.toDegrees((double)var4);
      Vector2 var7 = new Vector2(IsoDirections.reverse(this.getDir()).ToVector().x, IsoDirections.reverse(this.getDir()).ToVector().y);
      var7.normalize();
      float var8 = var7.getDirectionNeg();
      if (var8 < 0.0F) {
         var8 = Math.abs(var8);
      } else {
         var8 = 6.2831855F - var8;
      }

      double var9 = Math.toDegrees((double)var8);
      if ((int)var9 == 360) {
         var9 = 0.0;
      }

      if ((int)var5 == 360) {
         var5 = 0.0;
      }

      String var11 = "0";
      boolean var12 = false;
      int var13;
      if (var5 > var9) {
         var13 = (int)(var5 - var9);
         if (var13 > 350 || var13 <= 35) {
            var11 = "45R";
         }

         if (var13 > 35 && var13 <= 80) {
            var11 = "90R";
         }

         if (var13 > 80 && var13 <= 125) {
            var11 = "135R";
         }

         if (var13 > 125 && var13 <= 170) {
            var11 = "180R";
         }

         if (var13 > 170 && var13 < 215) {
            var11 = "180L";
         }

         if (var13 >= 215 && var13 < 260) {
            var11 = "135L";
         }

         if (var13 >= 260 && var13 < 305) {
            var11 = "90L";
         }

         if (var13 >= 305 && var13 < 350) {
            var11 = "45L";
         }
      } else {
         var13 = (int)(var9 - var5);
         if (var13 > 10 && var13 <= 55) {
            var11 = "45L";
         }

         if (var13 > 55 && var13 <= 100) {
            var11 = "90L";
         }

         if (var13 > 100 && var13 <= 145) {
            var11 = "135L";
         }

         if (var13 > 145 && var13 <= 190) {
            var11 = "180L";
         }

         if (var13 > 190 && var13 < 235) {
            var11 = "180R";
         }

         if (var13 >= 235 && var13 < 280) {
            var11 = "135R";
         }

         if (var13 >= 280 && var13 < 325) {
            var11 = "90R";
         }

         if (var13 >= 325 || var13 < 10) {
            var11 = "45R";
         }
      }

      this.setVariable("turnalertedvalue", var11);
      ZombieTurnAlerted.instance().setParams(this, var3.set((float)var1 + 0.5F - this.getX(), (float)var2 + 0.5F - this.getY()).getDirection());
      this.alerted = true;
      this.networkAI.extraUpdate();
   }

   public boolean getAttackDidDamage() {
      return this.m_bAttackDidDamage;
   }

   public void setAttackDidDamage(boolean var1) {
      this.m_bAttackDidDamage = var1;
   }

   public String getAttackOutcome() {
      return this.m_attackOutcome;
   }

   public void setAttackOutcome(String var1) {
      this.m_attackOutcome = var1;
   }

   public void setReanimatedForGrappleOnly(boolean var1) {
      this.m_reanimatedForGrappleOnly = var1;
   }

   public boolean isReanimatedForGrappleOnly() {
      return this.m_reanimatedForGrappleOnly;
   }

   public void clearAggroList() {
      try {
         Arrays.fill(this.aggroList, (Object)null);
      } catch (Exception var2) {
      }
   }

   private void processAggroList() {
      try {
         for(int var1 = 0; var1 < this.aggroList.length; ++var1) {
            if (this.aggroList[var1] != null && this.aggroList[var1].getAggro() <= 0.0F) {
               this.aggroList[var1] = null;
               return;
            }
         }

      } catch (Exception var2) {
      }
   }

   public void addAggro(IsoMovingObject var1, float var2) {
      try {
         if (this.aggroList[0] == null) {
            this.aggroList[0] = new Aggro(var1, var2);
         } else {
            int var3;
            for(var3 = 0; var3 < this.aggroList.length; ++var3) {
               if (this.aggroList[var3] != null && this.aggroList[var3].obj == var1) {
                  this.aggroList[var3].addDamage(var2);
                  return;
               }
            }

            for(var3 = 0; var3 < this.aggroList.length; ++var3) {
               if (this.aggroList[var3] == null) {
                  this.aggroList[var3] = new Aggro(var1, var2);
                  return;
               }
            }

         }
      } catch (Exception var4) {
      }
   }

   public boolean isLeadAggro(IsoMovingObject var1) {
      try {
         if (this.aggroList[0] == null) {
            return false;
         } else {
            this.processAggroList();
            if (this.aggroList[0] == null) {
               return false;
            } else {
               IsoMovingObject var2 = this.aggroList[0].obj;
               float var3 = this.aggroList[0].getAggro();

               for(int var4 = 1; var4 < this.aggroList.length; ++var4) {
                  if (this.aggroList[var4] != null) {
                     if (var3 >= 1.0F && this.aggroList[var4].getAggro() >= 1.0F) {
                        return false;
                     }

                     if (this.aggroList[var4] != null && var3 < this.aggroList[var4].getAggro()) {
                        var2 = this.aggroList[var4].obj;
                        var3 = this.aggroList[var4].getAggro();
                     }
                  }
               }

               return var1 == var2 && var3 == 1.0F;
            }
         }
      } catch (Exception var5) {
         return false;
      }
   }

   private Float closeSneakBonusCoeff(IsoGridSquare var1, IsoDirections var2) {
      if (var1 == null) {
         return null;
      } else {
         PZArrayList var3 = var1.getObjects();
         float var4 = 0.0F;

         for(int var5 = 0; var5 < var3.size(); ++var5) {
            IsoSprite var6 = ((IsoObject)var3.get(var5)).getSprite();
            if (var6 != null && var6.getName() != null && temporaryMapCloseSneakBonusDir.containsKey(var6.getName()) && temporaryMapCloseSneakBonusValue.containsKey(var6.getName())) {
               try {
                  if (var2.equals(IsoDirections.N) && ((String)temporaryMapCloseSneakBonusDir.get(var6.getName())).contains("N")) {
                     return (Float)temporaryMapCloseSneakBonusValue.get(var6.getName()) / 100.0F;
                  }

                  if (var2.equals(IsoDirections.S) && ((String)temporaryMapCloseSneakBonusDir.get(var6.getName())).contains("S")) {
                     return (Float)temporaryMapCloseSneakBonusValue.get(var6.getName()) / 100.0F;
                  }

                  if (var2.equals(IsoDirections.W) && ((String)temporaryMapCloseSneakBonusDir.get(var6.getName())).contains("W")) {
                     return (Float)temporaryMapCloseSneakBonusValue.get(var6.getName()) / 100.0F;
                  }

                  if (var2.equals(IsoDirections.E) && ((String)temporaryMapCloseSneakBonusDir.get(var6.getName())).contains("E")) {
                     return (Float)temporaryMapCloseSneakBonusValue.get(var6.getName()) / 100.0F;
                  }
               } catch (Exception var8) {
                  DebugLog.General.println("Problem with tile property CloseSneakBonus" + var6.getName());
               }
            }
         }

         return null;
      }
   }

   private float getObstacleMod(IsoGridSquare var1, IsoDirections var2) {
      Float var3 = this.closeSneakBonusCoeff(var1, var2);
      if (var3 != null) {
         return 1.0F - var3;
      } else {
         IsoGridSquare var4 = var1.nav[var2.index()];
         IsoDirections var5 = var2.equals(IsoDirections.S) ? IsoDirections.N : (var2.equals(IsoDirections.E) ? IsoDirections.W : var2);
         Float var6 = this.closeSneakBonusCoeff(var4, var5);
         return var6 != null ? 1.0F - var6 : 1.0F;
      }
   }

   public void spottedNew(IsoMovingObject var1, boolean var2) {
      Vector2 var10000;
      if (GameClient.bClient && this.isRemoteZombie()) {
         if (this.getTarget() != null) {
            this.vectorToTarget.x = this.getTarget().getX();
            this.vectorToTarget.y = this.getTarget().getY();
            var10000 = this.vectorToTarget;
            var10000.x -= this.getX();
            var10000 = this.vectorToTarget;
            var10000.y -= this.getY();
            if (this.isCurrentState(LungeNetworkState.instance())) {
               Vector2 var24 = new Vector2();
               var24.x = this.vectorToTarget.x;
               var24.y = this.vectorToTarget.y;
               var24.normalize();
               this.DirectionFromVector(var24);
               this.getVectorFromDirection(this.getForwardDirection());
               this.setForwardDirection(var24);
            }
         }

      } else if (this.getCurrentSquare() != null) {
         if (var1.getCurrentSquare() != null) {
            if (GameClient.bClient && !GameClient.connection.isReady()) {
               this.setTarget((IsoMovingObject)null);
               this.spottedLast = null;
            } else if (this.isReanimatedForGrappleOnly()) {
               this.setTarget((IsoMovingObject)null);
               this.spottedLast = null;
            } else if (this.isUseless()) {
               this.setTarget((IsoMovingObject)null);
               this.spottedLast = null;
            } else if (this.getCurrentSquare().getProperties().Is(IsoFlagType.smoke)) {
               this.setTarget((IsoMovingObject)null);
               this.spottedLast = null;
            } else {
               IsoGameCharacter var3 = (IsoGameCharacter)Type.tryCastTo(var1, IsoGameCharacter.class);
               if (var3 != null && !var3.isDead()) {
                  IsoPlayer var4 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
                  if (var4 == null || !var4.isGhostMode()) {
                     if (!GameClient.bClient && !GameServer.bServer || var4 == null || !var4.networkAI.isDisconnected()) {
                        if (this.getCurrentSquare() == null) {
                           this.ensureOnTile();
                        }

                        if (var1.getCurrentSquare() == null) {
                           var1.ensureOnTile();
                        }

                        float var5 = 100.0F;
                        int var6 = var4 != null && !GameServer.bServer ? var4.PlayerIndex : 0;
                        float var7 = (var1.getCurrentSquare().lighting[var6].lightInfo().r + var1.getCurrentSquare().lighting[var6].lightInfo().g + var1.getCurrentSquare().lighting[var6].lightInfo().b) / 3.0F;
                        var7 = Math.max(0.0F, Math.min(1.0F, var7));
                        var5 *= var7;
                        if (var1.getCurrentSquare().getZ() != this.current.getZ()) {
                           int var9 = Math.abs(var1.getCurrentSquare().getZ() - this.current.getZ()) * 5 + 1;
                           var5 /= (float)var9;
                        }

                        Vector2 var25 = IsoGameCharacter.tempo;
                        var25.x = var1.getX();
                        var25.y = var1.getY();
                        var25.x -= this.getX();
                        var25.y -= this.getY();
                        float var10 = GameTime.getInstance().getViewDist();
                        if (!(var25.getLength() > var10)) {
                           if (GameServer.bServer) {
                              this.bIndoorZombie = false;
                           }

                           if (var25.getLength() < var10) {
                              var10 = var25.getLength();
                           }

                           var10 *= 1.1F;
                           if (var10 > GameTime.getInstance().getViewDistMax()) {
                              var10 = GameTime.getInstance().getViewDistMax();
                           }

                           var25.normalize();
                           Vector2 var11 = this.getLookVector(tempo2);
                           float var12 = var11.dot(var25);
                           this.updateVisionRadius();
                           if (this.DistTo(var1) > this.VISION_RADIUS_RESULT) {
                              var5 = 0.0F;
                           }

                           if ((double)var10 > 0.5) {
                              if (var12 < -0.4F) {
                                 var5 = 0.0F;
                              } else if (var12 < -0.2F) {
                                 var5 /= 8.0F;
                              } else if (var12 < -0.0F) {
                                 var5 /= 4.0F;
                              } else if (var12 < 0.2F) {
                                 var5 /= 2.0F;
                              } else if (var12 <= 0.4F) {
                                 var5 *= 2.0F;
                              } else if (var12 <= 0.6F) {
                                 var5 *= 8.0F;
                              } else if (var12 <= 0.8F) {
                                 var5 *= 16.0F;
                              } else {
                                 var5 *= 32.0F;
                              }
                           }

                           if (var5 > 0.0F) {
                              IsoPlayer var13 = (IsoPlayer)Type.tryCastTo(this.target, IsoPlayer.class);
                              if (var13 != null && !GameServer.bServer && var13.RemoteID == -1 && this.current.isCanSee(var13.PlayerIndex)) {
                                 var13.targetedByZombie = true;
                                 var13.lastTargeted = 0.0F;
                              }
                           }

                           float var26 = var1.getMovementLastFrame().getLength();
                           float var14 = 0.8F;
                           if (var26 == 0.5F) {
                              var14 = 1.0F;
                           } else if (var26 == 1.0F) {
                              var14 = 1.5F;
                           } else if (var26 == 1.5F) {
                              var14 = 2.0F;
                           }

                           var5 *= var14;
                           if (var10 < 5.0F && (!var3.isRunning() && !var3.isSneaking() && !var3.isAiming() || var3.isRunning())) {
                              var5 *= 3.0F;
                           }

                           float var15 = var3.getSneakSpotMod();
                           if (var4 != null && !var4.isSneaking()) {
                              var15 = 1.0F;
                           }

                           var5 *= var15;
                           if (this.spottedLast == var1 && this.TimeSinceSeenFlesh < 120.0F) {
                           }

                           float var16;
                           float var17;
                           if (this.target != var1 && this.target != null) {
                              var16 = IsoUtils.DistanceManhatten(this.getX(), this.getY(), var1.getX(), var1.getY());
                              var17 = IsoUtils.DistanceManhatten(this.getX(), this.getY(), this.target.getX(), this.target.getY());
                              if (var16 > var17) {
                                 return;
                              }
                           }

                           if (var2) {
                              var5 = 1000000.0F;
                           }

                           if (this.BonusSpotTime > 0.0F) {
                              var5 *= 5.0F;
                           }

                           if (this.sight == 1) {
                              var5 *= 2.5F;
                           }

                           if (this.sight == 3) {
                              var5 *= 0.45F;
                           }

                           if (this.inactive) {
                              var5 *= 0.25F;
                           }

                           var16 = 1.0F;
                           if (var4 != null && var4.Traits.Inconspicuous.isSet()) {
                              var16 = 0.8F;
                           }

                           if (var4 != null && var4.Traits.Conspicuous.isSet()) {
                              var16 = 1.2F;
                           }

                           var5 *= var16;
                           var17 = 1.0F;
                           int var19;
                           if (this.getCurrentSquare() != var1.getCurrentSquare() && var4 != null && var4.isSneaking()) {
                              int var18 = Math.abs(this.getCurrentSquare().getX() - var1.getCurrentSquare().getX());
                              var19 = Math.abs(this.getCurrentSquare().getY() - var1.getCurrentSquare().getY());
                              if (var18 > var19) {
                                 if (this.getCurrentSquare().getX() - var1.getCurrentSquare().getX() > 0) {
                                    var17 = this.getObstacleMod(var1.getCurrentSquare(), IsoDirections.E);
                                 } else {
                                    var17 = this.getObstacleMod(var1.getCurrentSquare(), IsoDirections.W);
                                 }
                              } else if (this.getCurrentSquare().getY() - var1.getCurrentSquare().getY() > 0) {
                                 var17 = this.getObstacleMod(var1.getCurrentSquare(), IsoDirections.S);
                              } else {
                                 var17 = this.getObstacleMod(var1.getCurrentSquare(), IsoDirections.N);
                              }
                           }

                           var5 *= var17;
                           boolean var27 = false;

                           for(var19 = 0; var19 < IsoWorld.instance.CurrentCell.getVehicles().size(); ++var19) {
                              BaseVehicle var20 = (BaseVehicle)IsoWorld.instance.CurrentCell.getVehicles().get(var19);
                              Vector3 var21 = var20.getIntersectPoint(new Vector3(var3.getX(), var3.getY(), var3.getZ() + 0.1F), new Vector3(this.getX(), this.getY(), this.getZ() + 0.1F));
                              if (var21 != null) {
                                 var27 = true;
                              }
                           }

                           var25.x = var1.getX();
                           var25.y = var1.getY();
                           var25.x -= this.getX();
                           var25.y -= this.getY();
                           float var28 = var27 && var4.getVehicle() == null ? (var25.getLength() < 1.5F ? 0.5F : 0.0F) : 1.0F;
                           var5 *= var28;
                           var5 /= this.getWornItemsVisionModifier();
                           if (this.getEatBodyTarget() != null && var5 > 0.0F) {
                              var5 = (float)((double)var5 * 0.5);
                           }

                           var5 = (float)PZMath.fastfloor(var5);
                           boolean var29 = false;
                           var5 = Math.min(var5, 400.0F);
                           var5 /= 400.0F;
                           var5 = Math.max(0.0F, var5);
                           var5 = Math.min(1.0F, var5);
                           float var30 = GameTime.instance.getMultiplier();
                           var5 = (float)(1.0 - Math.pow((double)(1.0F - var5), (double)var30));
                           var5 *= 100.0F;
                           if ((float)Rand.Next(10000) / 100.0F < var5) {
                              var29 = true;
                           }

                           if (!GameClient.bClient && !GameServer.bServer || NetworkZombieManager.canSpotted(this) || var1 == this.target) {
                              if (!var29) {
                                 if (var5 > 20.0F && var4 != null && var10 < 15.0F) {
                                    var4.bCouldBeSeenThisFrame = true;
                                 }

                                 boolean var31 = var4 != null && !var4.isbCouldBeSeenThisFrame() && !var4.isbSeenThisFrame() && var4.isSneaking() && var4.isJustMoved();
                                 float var32 = 1100.0F;
                                 if (GameServer.bDebug) {
                                    var32 = 100.0F;
                                 }

                                 if (var31 && Rand.Next((int)(var32 * GameTime.instance.getInvMultiplier())) == 0) {
                                    if (GameServer.bServer) {
                                       GameServer.addXp((IsoPlayer)var1, PerkFactory.Perks.Sneak, 1.0F);
                                    } else if (!GameClient.bClient) {
                                       var4.getXp().AddXP(PerkFactory.Perks.Sneak, 1.0F);
                                    }
                                 }

                                 if (var31 && Rand.Next((int)(var32 * GameTime.instance.getInvMultiplier())) == 0) {
                                    if (GameServer.bServer) {
                                       GameServer.addXp((IsoPlayer)var1, PerkFactory.Perks.Lightfoot, 1.0F);
                                    } else if (!GameClient.bClient) {
                                       var4.getXp().AddXP(PerkFactory.Perks.Lightfoot, 1.0F);
                                    }
                                 }

                              } else {
                                 if (var4 != null) {
                                    var4.setbSeenThisFrame(true);
                                 }

                                 if (!var2) {
                                    this.BonusSpotTime = 720.0F;
                                 }

                                 this.LastTargetSeenX = PZMath.fastfloor(var1.getX());
                                 this.LastTargetSeenY = PZMath.fastfloor(var1.getY());
                                 this.LastTargetSeenZ = PZMath.fastfloor(var1.getZ());
                                 if (this.stateMachine.getCurrent() != StaggerBackState.instance()) {
                                    if (this.target != var1) {
                                       this.targetSeenTime = 0.0F;
                                       if (GameServer.bServer && !this.isRemoteZombie()) {
                                          this.addAggro(var1, 1.0F);
                                       }
                                    }

                                    this.setTarget(var1);
                                    this.vectorToTarget.x = var1.getX();
                                    this.vectorToTarget.y = var1.getY();
                                    var10000 = this.vectorToTarget;
                                    var10000.x -= this.getX();
                                    var10000 = this.vectorToTarget;
                                    var10000.y -= this.getY();
                                    float var22 = this.vectorToTarget.getLength();
                                    if (!var2) {
                                       this.TimeSinceSeenFlesh = 0.0F;
                                       this.targetSeenTime += GameTime.getInstance().getRealworldSecondsSinceLastUpdate();
                                    }

                                    if (this.target != this.spottedLast || this.getCurrentState() != LungeState.instance() || !(this.LungeTimer > 0.0F)) {
                                       if (this.target != this.spottedLast || this.getCurrentState() != AttackVehicleState.instance()) {
                                          if (PZMath.fastfloor(this.getZ()) == PZMath.fastfloor(this.target.getZ()) && (var22 <= 3.5F || this.target instanceof IsoGameCharacter && ((IsoGameCharacter)this.target).getVehicle() != null && var22 <= 4.0F) && this.getStateEventDelayTimer() <= 0.0F && !PolygonalMap2.instance.lineClearCollide(this.getX(), this.getY(), var1.getX(), var1.getY(), PZMath.fastfloor(this.getZ()), var1)) {
                                             this.setTarget(var1);
                                             if (this.getCurrentState() == LungeState.instance()) {
                                                return;
                                             }
                                          }

                                          this.spottedLast = var1;
                                          if (!this.Ghost && !this.getCurrentSquare().getProperties().Is(IsoFlagType.smoke)) {
                                             this.setTarget(var1);
                                             if (this.AllowRepathDelay > 0.0F) {
                                                return;
                                             }

                                             if (this.target instanceof IsoGameCharacter && ((IsoGameCharacter)this.target).getVehicle() != null) {
                                                if ((this.getCurrentState() == PathFindState.instance() || this.getCurrentState() == WalkTowardState.instance()) && this.getPathFindBehavior2().getTargetChar() == this.target) {
                                                   return;
                                                }

                                                if (this.getCurrentState() == AttackVehicleState.instance()) {
                                                   return;
                                                }

                                                BaseVehicle var23 = ((IsoGameCharacter)this.target).getVehicle();
                                                if (Math.abs(var23.getCurrentSpeedKmHour()) > 0.8F && this.DistToSquared(var23) <= 16.0F) {
                                                   return;
                                                }

                                                this.pathToCharacter((IsoGameCharacter)this.target);
                                                this.AllowRepathDelay = 10.0F;
                                                return;
                                             }

                                             this.pathToCharacter(var3);
                                             if (Rand.Next(5) == 0) {
                                                this.spotSoundDelay = 200;
                                             }

                                             this.AllowRepathDelay = 480.0F;
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
            }
         }
      }
   }

   public void spottedOld(IsoMovingObject var1, boolean var2) {
      Vector2 var10000;
      if (GameClient.bClient && this.isRemoteZombie()) {
         if (this.getTarget() != null) {
            this.vectorToTarget.x = this.getTarget().getX();
            this.vectorToTarget.y = this.getTarget().getY();
            var10000 = this.vectorToTarget;
            var10000.x -= this.getX();
            var10000 = this.vectorToTarget;
            var10000.y -= this.getY();
            if (this.isCurrentState(LungeNetworkState.instance())) {
               Vector2 var25 = new Vector2();
               var25.x = this.vectorToTarget.x;
               var25.y = this.vectorToTarget.y;
               var25.normalize();
               this.DirectionFromVector(var25);
               this.getVectorFromDirection(this.getForwardDirection());
               this.setForwardDirection(var25);
            }
         }

      } else if (this.getCurrentSquare() != null) {
         if (var1.getCurrentSquare() != null) {
            if (GameClient.bClient && !GameClient.connection.isReady()) {
               this.setTarget((IsoMovingObject)null);
               this.spottedLast = null;
            } else if (this.isReanimatedForGrappleOnly()) {
               this.setTarget((IsoMovingObject)null);
               this.spottedLast = null;
            } else if (this.isUseless()) {
               this.setTarget((IsoMovingObject)null);
               this.spottedLast = null;
            } else if (this.getCurrentSquare().getProperties().Is(IsoFlagType.smoke)) {
               this.setTarget((IsoMovingObject)null);
               this.spottedLast = null;
            } else {
               IsoGameCharacter var3 = (IsoGameCharacter)Type.tryCastTo(var1, IsoGameCharacter.class);
               if (var3 != null && !var3.isDead()) {
                  IsoPlayer var4 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
                  if (var4 == null || !var4.isGhostMode()) {
                     if (!GameClient.bClient && !GameServer.bServer || var4 == null || !var4.networkAI.isDisconnected()) {
                        if (this.getCurrentSquare() == null) {
                           this.ensureOnTile();
                        }

                        if (var1.getCurrentSquare() == null) {
                           var1.ensureOnTile();
                        }

                        float var5 = 200.0F;
                        int var6 = var4 != null && !GameServer.bServer ? var4.PlayerIndex : 0;
                        float var7 = (var1.getCurrentSquare().lighting[var6].lightInfo().r + var1.getCurrentSquare().lighting[var6].lightInfo().g + var1.getCurrentSquare().lighting[var6].lightInfo().b) / 3.0F;
                        float var8 = RenderSettings.getInstance().getAmbientForPlayer(var6);
                        float var9 = (this.getCurrentSquare().lighting[var6].lightInfo().r + this.getCurrentSquare().lighting[var6].lightInfo().g + this.getCurrentSquare().lighting[var6].lightInfo().b) / 3.0F;
                        var9 = var9 * var9 * var9;
                        if (var7 > 1.0F) {
                           var7 = 1.0F;
                        }

                        if (var7 < 0.0F) {
                           var7 = 0.0F;
                        }

                        if (var9 > 1.0F) {
                           var9 = 1.0F;
                        }

                        if (var9 < 0.0F) {
                           var9 = 0.0F;
                        }

                        float var10 = 1.0F - (var7 - var9);
                        if (var7 < 0.2F) {
                           var7 = 0.2F;
                        }

                        if (var8 < 0.2F) {
                           var8 = 0.2F;
                        }

                        if (var1.getCurrentSquare().getRoom() != this.getCurrentSquare().getRoom()) {
                           var5 = 50.0F;
                           if (var1.getCurrentSquare().getRoom() != null && this.getCurrentSquare().getRoom() == null || var1.getCurrentSquare().getRoom() == null && this.getCurrentSquare().getRoom() != null) {
                              var5 = 20.0F;
                              if (!var3.isAiming() && !var3.isSneaking()) {
                                 if (var1.getMovementLastFrame().getLength() <= 0.04F && var7 < 0.4F) {
                                    var5 = 10.0F;
                                 }
                              } else if (var7 < 0.4F) {
                                 var5 = 0.0F;
                              } else {
                                 var5 = 10.0F;
                              }
                           }
                        }

                        Vector2 var11 = IsoGameCharacter.tempo;
                        var11.x = var1.getX();
                        var11.y = var1.getY();
                        var11.x -= this.getX();
                        var11.y -= this.getY();
                        if (var1.getCurrentSquare().getZ() != this.current.getZ()) {
                           int var12 = Math.abs(var1.getCurrentSquare().getZ() - this.current.getZ()) * 5;
                           ++var12;
                           var5 /= (float)var12;
                        }

                        float var26 = GameTime.getInstance().getViewDist();
                        if (!(var11.getLength() > var26)) {
                           if (GameServer.bServer) {
                              this.bIndoorZombie = false;
                           }

                           if (var11.getLength() < var26) {
                              var26 = var11.getLength();
                           }

                           var26 *= 1.1F;
                           if (var26 > GameTime.getInstance().getViewDistMax()) {
                              var26 = GameTime.getInstance().getViewDistMax();
                           }

                           var11.normalize();
                           Vector2 var13 = this.getLookVector(tempo2);
                           float var14 = var13.dot(var11);
                           this.updateVisionRadius();
                           if (this.DistTo(var1) > this.VISION_RADIUS_RESULT) {
                              var5 -= 10000.0F;
                           }

                           if ((double)var26 > 0.5) {
                              if (var14 < -0.4F) {
                                 var5 = 0.0F;
                              } else if (var14 < -0.2F) {
                                 var5 /= 8.0F;
                              } else if (var14 < -0.0F) {
                                 var5 /= 4.0F;
                              } else if (var14 < 0.2F) {
                                 var5 /= 2.0F;
                              } else if (var14 <= 0.4F) {
                                 var5 *= 2.0F;
                              } else if (var14 <= 0.6F) {
                                 var5 *= 8.0F;
                              } else if (var14 <= 0.8F) {
                                 var5 *= 16.0F;
                              } else {
                                 var5 *= 32.0F;
                              }
                           }

                           if (var5 > 0.0F) {
                              IsoPlayer var15 = (IsoPlayer)Type.tryCastTo(this.target, IsoPlayer.class);
                              if (var15 != null && !GameServer.bServer && var15.RemoteID == -1 && this.current.isCanSee(var15.PlayerIndex)) {
                                 var15.targetedByZombie = true;
                                 var15.lastTargeted = 0.0F;
                              }
                           }

                           var5 *= var10;
                           int var27 = PZMath.fastfloor(var1.getZ()) - PZMath.fastfloor(this.getZ());
                           if (var27 >= 1) {
                              var5 /= (float)(var27 * 3);
                           }

                           float var16 = PZMath.clamp(var26 / GameTime.getInstance().getViewDist(), 0.0F, 1.0F);
                           var5 *= 1.0F - var16;
                           var5 *= 1.0F - var16;
                           var5 *= 1.0F - var16;
                           float var17 = PZMath.clamp(var26 / 10.0F, 0.0F, 1.0F);
                           var5 *= 1.0F + (1.0F - var17) * 10.0F;
                           float var18 = var1.getMovementLastFrame().getLength();
                           if (var18 == 0.0F && var7 <= 0.2F) {
                              var7 = 0.0F;
                           }

                           if (var3 != null) {
                              if (var3.getTorchStrength() > 0.0F) {
                                 var5 *= 3.0F;
                              }

                              if (var18 < 0.01F) {
                                 var5 *= 0.5F;
                              } else if (var3.isSneaking()) {
                                 var5 *= 0.4F;
                              } else if (var3.isAiming()) {
                                 var5 *= 0.75F;
                              } else if (var18 < 0.06F) {
                                 var5 *= 0.8F;
                              } else if (var18 >= 0.06F) {
                                 var5 *= 2.4F;
                              }

                              if (this.eatBodyTarget != null) {
                                 var5 *= 0.6F;
                              }

                              if (var26 < 5.0F && (!var3.isRunning() && !var3.isSneaking() && !var3.isAiming() || var3.isRunning())) {
                                 var5 *= 3.0F;
                              }

                              if (this.spottedLast == var1 && this.TimeSinceSeenFlesh < 120.0F) {
                                 var5 = 1000.0F;
                              }

                              var5 *= var3.getSneakSpotMod();
                              var5 *= var8;
                              float var20;
                              if (this.target != var1 && this.target != null) {
                                 float var19 = IsoUtils.DistanceManhatten(this.getX(), this.getY(), var1.getX(), var1.getY());
                                 var20 = IsoUtils.DistanceManhatten(this.getX(), this.getY(), this.target.getX(), this.target.getY());
                                 if (var19 > var20) {
                                    return;
                                 }
                              }

                              var5 *= 0.3F;
                              if (var2) {
                                 var5 = 1000000.0F;
                              }

                              if (this.BonusSpotTime > 0.0F) {
                                 var5 = 1000000.0F;
                              }

                              var5 *= 1.2F;
                              if (this.sight == 1) {
                                 var5 *= 2.5F;
                              }

                              if (this.sight == 3) {
                                 var5 *= 0.45F;
                              }

                              if (this.inactive) {
                                 var5 *= 0.25F;
                              }

                              var5 *= 0.25F;
                              if (var4 != null && var4.Traits.Inconspicuous.isSet()) {
                                 var5 *= 0.5F;
                              }

                              if (var4 != null && var4.Traits.Conspicuous.isSet()) {
                                 var5 *= 2.0F;
                              }

                              var5 *= 1.6F;
                              if (this.getCurrentSquare() != var1.getCurrentSquare() && var4 != null && var4.isSneaking()) {
                                 IsoGridSquare var28 = null;
                                 IsoGridSquare var29 = null;
                                 int var21 = Math.abs(this.getCurrentSquare().getX() - var1.getCurrentSquare().getX());
                                 int var22 = Math.abs(this.getCurrentSquare().getY() - var1.getCurrentSquare().getY());
                                 if (var21 > var22) {
                                    if (this.getCurrentSquare().getX() - var1.getCurrentSquare().getX() > 0) {
                                       var28 = var1.getCurrentSquare().nav[IsoDirections.E.index()];
                                    } else {
                                       var28 = var1.getCurrentSquare();
                                       var29 = var1.getCurrentSquare().nav[IsoDirections.W.index()];
                                    }
                                 } else if (this.getCurrentSquare().getY() - var1.getCurrentSquare().getY() > 0) {
                                    var28 = var1.getCurrentSquare().nav[IsoDirections.S.index()];
                                 } else {
                                    var28 = var1.getCurrentSquare();
                                    var29 = var1.getCurrentSquare().nav[IsoDirections.N.index()];
                                 }

                                 if (var28 != null && var3 != null) {
                                    float var23 = var3.checkIsNearWall();
                                    if (var23 == 1.0F && var29 != null) {
                                       var23 = var29.getGridSneakModifier(true);
                                    }

                                    if (var23 > 1.0F) {
                                       float var24 = var1.DistTo(var28.x, var28.y);
                                       if (var24 > 1.0F) {
                                          var23 /= var24;
                                       }

                                       var5 /= var23;
                                    }
                                 }
                              }

                              var5 /= this.getWornItemsVisionModifier();
                              if (this.getEatBodyTarget() != null && var5 > 0.0F) {
                                 var5 = (float)((double)var5 * 0.5);
                              }

                              var5 = (float)PZMath.fastfloor(var5);
                              boolean var30 = false;
                              var5 = Math.min(var5, 400.0F);
                              var5 /= 400.0F;
                              var5 = Math.max(0.0F, var5);
                              var5 = Math.min(1.0F, var5);
                              var20 = GameTime.instance.getMultiplier();
                              var5 = (float)(1.0 - Math.pow((double)(1.0F - var5), (double)var20));
                              var5 *= 100.0F;
                              if ((float)Rand.Next(10000) / 100.0F < var5) {
                                 var30 = true;
                              }

                              if (!GameClient.bClient && !GameServer.bServer || NetworkZombieManager.canSpotted(this) || var1 == this.target) {
                                 if (!var30) {
                                    if (var5 > 20.0F && var4 != null && var26 < 15.0F) {
                                       var4.bCouldBeSeenThisFrame = true;
                                    }

                                    boolean var32 = var4 != null && !var4.isbCouldBeSeenThisFrame() && !var4.isbSeenThisFrame() && var4.isSneaking() && var4.isJustMoved();
                                    float var34 = 1100.0F;
                                    if (GameServer.bDebug) {
                                       var34 = 100.0F;
                                    }

                                    if (var32 && Rand.Next((int)(var34 * GameTime.instance.getInvMultiplier())) == 0) {
                                       if (GameServer.bServer) {
                                          GameServer.addXp((IsoPlayer)var1, PerkFactory.Perks.Sneak, 1.0F);
                                       } else if (!GameClient.bClient) {
                                          var4.getXp().AddXP(PerkFactory.Perks.Sneak, 1.0F);
                                       }
                                    }

                                    if (var32 && Rand.Next((int)(var34 * GameTime.instance.getInvMultiplier())) == 0) {
                                       if (GameServer.bServer) {
                                          GameServer.addXp((IsoPlayer)var1, PerkFactory.Perks.Lightfoot, 1.0F);
                                       } else if (!GameClient.bClient) {
                                          var4.getXp().AddXP(PerkFactory.Perks.Lightfoot, 1.0F);
                                       }
                                    }

                                 } else {
                                    if (var4 != null) {
                                       var4.setbSeenThisFrame(true);
                                    }

                                    if (!var2) {
                                       this.BonusSpotTime = 120.0F;
                                    }

                                    this.LastTargetSeenX = PZMath.fastfloor(var1.getX());
                                    this.LastTargetSeenY = PZMath.fastfloor(var1.getY());
                                    this.LastTargetSeenZ = PZMath.fastfloor(var1.getZ());
                                    if (this.stateMachine.getCurrent() != StaggerBackState.instance()) {
                                       if (this.target != var1) {
                                          this.targetSeenTime = 0.0F;
                                          if (GameServer.bServer && !this.isRemoteZombie()) {
                                             this.addAggro(var1, 1.0F);
                                          }
                                       }

                                       this.setTarget(var1);
                                       this.vectorToTarget.x = var1.getX();
                                       this.vectorToTarget.y = var1.getY();
                                       var10000 = this.vectorToTarget;
                                       var10000.x -= this.getX();
                                       var10000 = this.vectorToTarget;
                                       var10000.y -= this.getY();
                                       float var31 = this.vectorToTarget.getLength();
                                       if (!var2) {
                                          this.TimeSinceSeenFlesh = 0.0F;
                                          this.targetSeenTime += GameTime.getInstance().getRealworldSecondsSinceLastUpdate();
                                       }

                                       if (this.target != this.spottedLast || this.getCurrentState() != LungeState.instance() || !(this.LungeTimer > 0.0F)) {
                                          if (this.target != this.spottedLast || this.getCurrentState() != AttackVehicleState.instance()) {
                                             if (PZMath.fastfloor(this.getZ()) == PZMath.fastfloor(this.target.getZ()) && (var31 <= 3.5F || this.target instanceof IsoGameCharacter && ((IsoGameCharacter)this.target).getVehicle() != null && var31 <= 4.0F) && this.getStateEventDelayTimer() <= 0.0F && !PolygonalMap2.instance.lineClearCollide(this.getX(), this.getY(), var1.getX(), var1.getY(), PZMath.fastfloor(this.getZ()), var1)) {
                                                this.setTarget(var1);
                                                if (this.getCurrentState() == LungeState.instance()) {
                                                   return;
                                                }
                                             }

                                             this.spottedLast = var1;
                                             if (!this.Ghost && !this.getCurrentSquare().getProperties().Is(IsoFlagType.smoke)) {
                                                this.setTarget(var1);
                                                if (this.AllowRepathDelay > 0.0F) {
                                                   return;
                                                }

                                                if (this.target instanceof IsoGameCharacter && ((IsoGameCharacter)this.target).getVehicle() != null) {
                                                   if ((this.getCurrentState() == PathFindState.instance() || this.getCurrentState() == WalkTowardState.instance()) && this.getPathFindBehavior2().getTargetChar() == this.target) {
                                                      return;
                                                   }

                                                   if (this.getCurrentState() == AttackVehicleState.instance()) {
                                                      return;
                                                   }

                                                   BaseVehicle var33 = ((IsoGameCharacter)this.target).getVehicle();
                                                   if (Math.abs(var33.getCurrentSpeedKmHour()) > 0.8F && this.DistToSquared(var33) <= 16.0F) {
                                                      return;
                                                   }

                                                   this.pathToCharacter((IsoGameCharacter)this.target);
                                                   this.AllowRepathDelay = 10.0F;
                                                   return;
                                                }

                                                this.pathToCharacter(var3);
                                                if (Rand.Next(5) == 0) {
                                                   this.spotSoundDelay = 200;
                                                }

                                                this.AllowRepathDelay = 480.0F;
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
               }
            }
         }
      }
   }

   public void spotted(IsoMovingObject var1, boolean var2) {
      if (SandboxOptions.instance.Lore.SpottedLogic.getValue()) {
         this.spottedNew(var1, var2);
      } else {
         this.spottedOld(var1, var2);
      }

   }

   public void Move(Vector2 var1) {
      if (!GameClient.bClient || this.authOwner != null) {
         this.setNextX(this.getNextX() + var1.x * GameTime.instance.getMultiplier());
         this.setNextY(this.getNextY() + var1.y * GameTime.instance.getMultiplier());
         this.movex = var1.x;
         this.movey = var1.y;
      }
   }

   public void MoveUnmodded(Vector2 var1) {
      float var2;
      if (this.speedType == 1 && (this.isCurrentState(LungeState.instance()) || this.isCurrentState(LungeNetworkState.instance()) || this.isCurrentState(AttackState.instance()) || this.isCurrentState(AttackNetworkState.instance()) || this.isCurrentState(StaggerBackState.instance()) || this.isCurrentState(ZombieHitReactionState.instance())) && this.target instanceof IsoGameCharacter) {
         var2 = this.target.getNextX() - this.getX();
         float var3 = this.target.getNextY() - this.getY();
         float var4 = (float)Math.sqrt((double)(var2 * var2 + var3 * var3));
         var4 -= this.getWidth() + this.target.getWidth() - 0.1F;
         var4 = Math.max(0.0F, var4);
         if (var1.getLength() > var4) {
            var1.setLength(var4);
         }
      }

      if (GameClient.bClient && this.isRemoteZombie()) {
         var2 = IsoUtils.DistanceTo(this.realx, this.realy, this.networkAI.targetX, this.networkAI.targetY);
         if (var2 > 1.0F) {
            Vector2 var7 = new Vector2(this.networkAI.targetX - this.realx, this.networkAI.targetY - this.realy);
            Vector2 var8 = new Vector2(this.realx - this.getX(), this.realy - this.getY());
            float var5 = 0.5F + IsoUtils.smoothstep(0.5F, 1.5F, IsoUtils.DistanceTo(this.getX(), this.getY(), this.networkAI.targetX, this.networkAI.targetY) / var2);
            Vector2 var6 = new Vector2(var7.x + var8.x, var7.y + var8.y);
            var6.setLength(var1.getLength() * var5);
            var1.set(var6);
         }
      }

      super.MoveUnmodded(var1);
   }

   public boolean canBeDeletedUnnoticed(float var1) {
      if (!GameClient.bClient) {
         return false;
      } else {
         float var2 = 1.0F / 0.0F;
         ArrayList var3 = GameClient.instance.getPlayers();

         for(int var4 = 0; var4 < var3.size(); ++var4) {
            IsoPlayer var5 = (IsoPlayer)var3.get(var4);
            float var6 = var5.getDotWithForwardDirection(this.getX(), this.getY());
            float var7 = LightingJNI.calculateVisionCone(var5) + 0.2F;
            if (var6 > -var7) {
               return false;
            }

            float var8 = IsoUtils.DistanceToSquared(this.getX(), this.getY(), var5.getX(), var5.getY());
            if (var8 < var2) {
               var2 = var8;
            }
         }

         return var2 > var1 * var1;
      }
   }

   public void DoFootstepSound(String var1) {
      ParameterCharacterMovementSpeed.MovementType var2 = ParameterCharacterMovementSpeed.MovementType.Walk;
      float var3 = 0.5F;
      switch (var1) {
         case "sneak_walk":
            var3 = 0.25F;
            var2 = ParameterCharacterMovementSpeed.MovementType.SneakWalk;
            break;
         case "sneak_run":
            var3 = 0.25F;
            var2 = ParameterCharacterMovementSpeed.MovementType.SneakRun;
            break;
         case "strafe":
            var3 = 0.5F;
            var2 = ParameterCharacterMovementSpeed.MovementType.Strafe;
            break;
         case "walk":
            var3 = 0.5F;
            var2 = ParameterCharacterMovementSpeed.MovementType.Walk;
            break;
         case "run":
            var3 = 0.75F;
            var2 = ParameterCharacterMovementSpeed.MovementType.Run;
            break;
         case "sprint":
            var3 = 1.0F;
            var2 = ParameterCharacterMovementSpeed.MovementType.Sprint;
      }

      this.addFootstepParametersIfNeeded();
      this.parameterCharacterMovementSpeed.setMovementType(var2);
      this.DoFootstepSound(var3);
   }

   public void addFootstepParametersIfNeeded() {
      if (!GameServer.bServer && !this.getFMODParameters().parameterList.contains(this.parameterCharacterMovementSpeed)) {
         this.getFMODParameters().add(this.parameterCharacterMovementSpeed);
         this.getFMODParameters().add(this.parameterFootstepMaterial);
         this.getFMODParameters().add(this.parameterFootstepMaterial2);
         this.getFMODParameters().add(this.parameterShoeType);
      }

   }

   public void DoFootstepSound(float var1) {
      if (!GameServer.bServer) {
         if (!(var1 <= 0.0F)) {
            if (this.getCurrentSquare() != null) {
               if (GameClient.bClient && this.authOwner == null) {
                  if (this.def != null && this.sprite != null && this.sprite.CurrentAnim != null && (this.sprite.CurrentAnim.name.contains("Run") || this.sprite.CurrentAnim.name.contains("Walk"))) {
                     int var6 = (int)this.def.Frame;
                     boolean var3;
                     if (var6 >= 0 && var6 < 5) {
                        var3 = this.stepFrameLast < 0 || this.stepFrameLast > 5;
                     } else {
                        var3 = this.stepFrameLast < 5;
                     }

                     if (var3) {
                        for(int var4 = 0; var4 < IsoPlayer.numPlayers; ++var4) {
                           IsoPlayer var5 = IsoPlayer.players[var4];
                           if (var5 != null && var5.DistToSquared(this) < 225.0F) {
                              ZombieFootstepManager.instance.addCharacter(this);
                              break;
                           }
                        }
                     }

                     this.stepFrameLast = var6;
                  } else {
                     this.stepFrameLast = -1;
                  }

               } else {
                  boolean var2 = SoundManager.instance.isListenerInRange(this.getX(), this.getY(), 15.0F);
                  if (var2) {
                     this.footstepVolume = var1;
                     ZombieFootstepManager.instance.addCharacter(this);
                  }

               }
            }
         }
      }
   }

   public void preupdate() {
      if (GameServer.bServer && this.thumpSent) {
         this.thumpFlag = 0;
         this.thumpSent = false;
      }

      this.FollowCount = 0;
      if (GameClient.bClient) {
         this.networkAI.updateHitVehicle();
         if (!this.isLocal()) {
            this.networkAI.preupdate();
         } else if (this.isKnockedDown() && !this.isOnFloor()) {
            HitReactionNetworkAI var1 = this.getHitReactionNetworkAI();
            if (var1.isSetup() && !var1.isStarted()) {
               var1.start();
            }
         }
      }

      super.preupdate();
   }

   public void postupdate() {
      IsoZombie.s_performance.postUpdate.invokeAndMeasure(this, IsoZombie::postUpdateInternal);
   }

   private void postUpdateInternal() {
      if (this.target instanceof IsoPlayer) {
         ++((IsoPlayer)this.target).getStats().NumChasingZombies;
      }

      super.postupdate();
      if (this.isReanimatedForGrappleOnly() && !this.isBeingGrappled() && !this.isDead()) {
         DebugLog.Grapple.warn("Failsafe. IsoZombie grappleEnd event never occurred.");
         DebugLog.Grapple.warn("Reanimated Corpse Dropped. Dying again.");
         this.die();
      }

      if (this.isReanimatedForGrappleOnly() && this.isBeingGrappled() && this.getGrappledBy().isMoving() && this.bloodSplatAmount > 0) {
         byte var1 = 100;
         float var2 = 200.0F / (float)var1 * GameTime.instance.getInvMultiplier();
         if ((float)Rand.Next((int)var2) < var2 * 0.3F) {
            DebugLog.Zombie.trace("Dragged corpse. Bleeding on the floor 1.");
            this.splatBloodFloor();
            this.bloodSplatAmount -= 2;
         }

         if (Rand.Next((int)var2) == 0) {
            DebugLog.Zombie.trace("Dragged corpse. Bleeding on the floor 2.");
            this.splatBloodFloor();
            this.bloodSplatAmount -= 2;
         }

         this.getModData().rawset("_bloodSplatAmount", BoxedStaticValues.toDouble((double)this.bloodSplatAmount));
      }

      if (this.current == null && (!GameClient.bClient || this.authOwner != null)) {
         this.removeFromWorld();
         this.removeFromSquare();
      }

      if (!GameServer.bServer) {
         IsoPlayer var5 = this.getReanimatedPlayer();
         if (var5 != null) {
            var5.setX(this.getX());
            var5.setY(this.getY());
            var5.setZ(this.getZ());
            var5.setDir(this.getDir());
            var5.setForwardDirection(this.getForwardDirection());
            AnimationPlayer var6 = this.getAnimationPlayer();
            AnimationPlayer var3 = var5.getAnimationPlayer();
            if (var6 != null && var6.isReady() && var3 != null && var3.isReady()) {
               var3.setTargetAngle(var6.getAngle());
               var3.setAngleToTarget();
            }

            var5.setCurrent(this.getCell().getGridSquare(PZMath.fastfloor(var5.getX()), PZMath.fastfloor(var5.getY()), PZMath.fastfloor(var5.getZ())));
            var5.updateLightInfo();
            if (var5.soundListener != null) {
               var5.soundListener.setPos(var5.getX(), var5.getY(), var5.getZ());
               var5.soundListener.tick();
            }

            IsoPlayer var4 = IsoPlayer.getInstance();
            IsoPlayer.setInstance(var5);
            var5.updateLOS();
            IsoPlayer.setInstance(var4);
            if (GameClient.bClient && this.authOwner == null && this.networkUpdate.Check()) {
               GameClient.instance.sendPlayer(var5);
            }

            var5.dirtyRecalcGridStackTime = 2.0F;
         }
      }

      if (this.targetSeenTime > 0.0F && !this.isTargetVisible()) {
         this.targetSeenTime = 0.0F;
      }

   }

   public boolean isSolidForSeparate() {
      if (this.getCurrentState() != FakeDeadZombieState.instance() && this.getCurrentState() != ZombieFallDownState.instance() && this.getCurrentState() != ZombieOnGroundState.instance() && this.getCurrentState() != ZombieGetUpState.instance() && (this.getCurrentState() != ZombieHitReactionState.instance() || this.speedType == 1)) {
         return this.isSitAgainstWall() ? false : super.isSolidForSeparate();
      } else {
         return false;
      }
   }

   public boolean isPushableForSeparate() {
      if (this.getCurrentState() != ThumpState.instance() && this.getCurrentState() != AttackState.instance() && this.getCurrentState() != AttackVehicleState.instance() && this.getCurrentState() != ZombieEatBodyState.instance() && this.getCurrentState() != ZombieFaceTargetState.instance()) {
         return this.isSitAgainstWall() ? false : super.isPushableForSeparate();
      } else {
         return false;
      }
   }

   public boolean isPushedByForSeparate(IsoMovingObject var1) {
      if (var1 instanceof IsoZombie && ((IsoZombie)var1).getCurrentState() == ZombieHitReactionState.instance() && !((IsoZombie)var1).collideWhileHit) {
         return false;
      } else if (this.getCurrentState() == ZombieHitReactionState.instance() && !this.collideWhileHit) {
         return false;
      } else {
         return GameClient.bClient && var1 instanceof IsoZombie && !NetworkZombieSimulator.getInstance().isZombieSimulated(this.getOnlineID()) ? false : super.isPushedByForSeparate(var1);
      }
   }

   public void update() {
      IsoZombie.s_performance.update.invokeAndMeasure(this, IsoZombie::updateInternal);
   }

   private void updateInternal() {
      if (Core.bDebug && this.getOutfitName() != null && this.getOutfitName().contains("Debug")) {
         String var1 = this.getOutfitName();
         if (var1.contains("Immortal")) {
            this.setImmortalTutorialZombie(true);
         }

         if (var1.contains("Useless")) {
            this.setUseless(true);
         }
      }

      if (GameClient.bClient && !this.isRemoteZombie()) {
         ZombieCountOptimiser.incrementZombie(this);
         MPStatistics.clientZombieUpdated();
      } else if (GameServer.bServer) {
         MPStatistics.serverZombieUpdated();
      }

      if (SandboxOptions.instance.Lore.ActiveOnly.getValue() > 1) {
         if ((SandboxOptions.instance.Lore.ActiveOnly.getValue() != 2 || GameTime.instance.getHour() < 20 && GameTime.instance.getHour() > 8) && (SandboxOptions.instance.Lore.ActiveOnly.getValue() != 3 || GameTime.instance.getHour() <= 8 || GameTime.instance.getHour() >= 20)) {
            this.makeInactive(true);
         } else {
            this.makeInactive(false);
         }
      }

      if (this.bCrawling) {
         if (this.getActionContext().getGroup() != ActionGroup.getActionGroup("zombie-crawler")) {
            this.advancedAnimator.OnAnimDataChanged(false);
            this.initializeStates();
            this.getActionContext().setGroup(ActionGroup.getActionGroup("zombie-crawler"));
         }
      } else if (this.getActionContext().getGroup() != ActionGroup.getActionGroup("zombie")) {
         this.advancedAnimator.OnAnimDataChanged(false);
         this.initializeStates();
         this.getActionContext().setGroup(ActionGroup.getActionGroup("zombie"));
      }

      if (this.getThumpTimer() > 0) {
         --this.thumpTimer;
      }

      BaseVehicle var5 = this.getNearVehicle();
      if (var5 != null) {
         if (this.target == null && var5.isSirening()) {
            VehiclePart var2 = var5.getUseablePart(this, false);
            if (var2 != null && var2.getSquare().DistTo((IsoMovingObject)this) < 0.7F) {
               this.setThumpTarget(var5);
            }
         }

         if (var5.isAlarmed() && !var5.isPreviouslyEntered() && Rand.Next(10000) < 1) {
            var5.triggerAlarm();
         }
      }

      this.doDeferredMovement();
      this.updateEmitter();
      if (this.spotSoundDelay > 0) {
         --this.spotSoundDelay;
      }

      if (GameClient.bClient && this.authOwner == null) {
         if (this.lastRemoteUpdate > 800 && this.legsSprite.hasAnimation() && (this.legsSprite.CurrentAnim.name.equals("ZombieDeath") || this.legsSprite.CurrentAnim.name.equals("ZombieStaggerBack") || this.legsSprite.CurrentAnim.name.equals("ZombieGetUp"))) {
            DebugLog.log(DebugType.Zombie, "removing stale zombie 800 id=" + this.OnlineID);
            VirtualZombieManager.instance.removeZombieFromWorld(this);
            return;
         }

         if (GameClient.bFastForward) {
            VirtualZombieManager.instance.removeZombieFromWorld(this);
            return;
         }
      }

      if (GameClient.bClient && this.authOwner == null && this.lastRemoteUpdate < 2000 && this.lastRemoteUpdate + 1000 / PerformanceSettings.getLockFPS() > 2000) {
         DebugLog.log(DebugType.Zombie, "lastRemoteUpdate 2000+ id=" + this.OnlineID);
      }

      this.lastRemoteUpdate = (short)(this.lastRemoteUpdate + 1000 / PerformanceSettings.getLockFPS());
      if (!GameClient.bClient || this.authOwner != null || this.bRemote && this.lastRemoteUpdate <= 5000) {
         this.sprite = this.legsSprite;
         if (this.sprite != null) {
            this.updateCharacterTextureAnimTime();
            if (GameServer.bServer && this.bIndoorZombie) {
               super.update();
            } else {
               this.BonusSpotTime = PZMath.clamp(this.BonusSpotTime - GameTime.instance.getMultiplier(), 0.0F, 3.4028235E38F);
               this.TimeSinceSeenFlesh = PZMath.clamp(this.TimeSinceSeenFlesh + GameTime.instance.getMultiplier(), 0.0F, 3.4028235E38F);
               this.checkZombieEntersPlayerBuilding();
               if (this.getStateMachine().getCurrent() != ClimbThroughWindowState.instance() && this.getStateMachine().getCurrent() != ClimbOverFenceState.instance() && this.getStateMachine().getCurrent() != CrawlingZombieTurnState.instance() && this.getStateMachine().getCurrent() != ZombieHitReactionState.instance() && this.getStateMachine().getCurrent() != ZombieFallDownState.instance()) {
                  this.setCollidable(true);
                  LuaEventManager.triggerEvent("OnZombieUpdate", this);
                  if (Core.bLastStand && this.getStateMachine().getCurrent() != ThumpState.instance() && this.getStateMachine().getCurrent() != AttackState.instance() && this.TimeSinceSeenFlesh > 120.0F && Rand.Next(36000) == 0) {
                     IsoPlayer var7 = null;
                     float var8 = 1000000.0F;

                     for(int var4 = 0; var4 < IsoPlayer.numPlayers; ++var4) {
                        if (IsoPlayer.players[var4] != null && IsoPlayer.players[var4].DistTo(this) < var8 && !IsoPlayer.players[var4].isDead()) {
                           var8 = IsoPlayer.players[var4].DistTo(this);
                           var7 = IsoPlayer.players[var4];
                        }
                     }

                     if (var7 != null) {
                        this.AllowRepathDelay = -1.0F;
                        this.pathToCharacter(var7);
                     }

                  } else {
                     if (GameServer.bServer) {
                        this.vehicle4testCollision = null;
                     } else if (GameClient.bClient) {
                        if (this.vehicle4testCollision != null && this.vehicle4testCollision.updateHitByVehicle(this)) {
                           super.update();
                           this.vehicle4testCollision = null;
                           return;
                        }
                     } else {
                        if (this.Health > 0.0F && this.vehicle4testCollision != null) {
                           this.setVehicleCollision(this.testCollideWithVehicles(this.vehicle4testCollision));
                        } else {
                           this.setVehicleCollision(false);
                        }

                        if (this.isVehicleCollision()) {
                           this.vehicle4testCollision = null;
                           return;
                        }
                     }

                     this.vehicle4testCollision = null;
                     if (this.BonusSpotTime > 0.0F && this.spottedLast != null && !((IsoGameCharacter)this.spottedLast).isDead()) {
                        this.spotted(this.spottedLast, true);
                     }

                     if (GameServer.bServer && this.getStateMachine().getCurrent() == BurntToDeath.instance()) {
                        DebugLog.log(DebugType.Zombie, "Zombie is burning " + this.OnlineID);
                     }

                     super.update();
                     if (VirtualZombieManager.instance.isReused(this)) {
                        DebugLog.log(DebugType.Zombie, "Zombie added to ReusableZombies after super.update - RETURNING " + this);
                     } else if (this.getStateMachine().getCurrent() != ClimbThroughWindowState.instance() && this.getStateMachine().getCurrent() != ClimbOverFenceState.instance() && this.getStateMachine().getCurrent() != CrawlingZombieTurnState.instance()) {
                        this.ensureOnTile();
                        State var6 = this.stateMachine.getCurrent();
                        if (var6 != StaggerBackState.instance() && var6 != BurntToDeath.instance() && var6 != FakeDeadZombieState.instance() && var6 != ZombieFallDownState.instance() && var6 != ZombieOnGroundState.instance() && var6 != ZombieHitReactionState.instance() && var6 != ZombieGetUpState.instance()) {
                           if (GameServer.bServer && this.OnlineID == -1) {
                              this.OnlineID = ServerMap.instance.getUniqueZombieId();
                           } else {
                              IsoSpriteInstance var10000;
                              if (var6 == PathFindState.instance() && this.finder.progress == AStarPathFinder.PathFindProgress.notyetfound) {
                                 if (this.bCrawling) {
                                    this.PlayAnim("ZombieCrawl");
                                    this.def.AnimFrameIncrease = 0.0F;
                                 } else {
                                    this.PlayAnim("ZombieIdle");
                                    this.def.AnimFrameIncrease = 0.08F + (float)Rand.Next(1000) / 8000.0F;
                                    var10000 = this.def;
                                    var10000.AnimFrameIncrease *= 0.5F;
                                 }
                              } else if (var6 != AttackState.instance() && var6 != AttackVehicleState.instance() && (this.getNextX() != this.getX() || this.getNextY() != this.getY())) {
                                 if (this.walkVariantUse == null || var6 != LungeState.instance() && var6 != LungeNetworkState.instance()) {
                                    this.walkVariantUse = this.walkVariant;
                                 }

                                 if (this.bCrawling) {
                                    this.walkVariantUse = "ZombieCrawl";
                                 }

                                 if (var6 != ZombieIdleState.instance() && var6 != StaggerBackState.instance() && var6 != ThumpState.instance() && var6 != FakeDeadZombieState.instance()) {
                                    if (this.bRunning) {
                                       this.PlayAnim("Run");
                                       this.def.setFrameSpeedPerFrame(0.33F);
                                    } else {
                                       this.PlayAnim(this.walkVariantUse);
                                       this.def.setFrameSpeedPerFrame(0.26F);
                                       var10000 = this.def;
                                       var10000.AnimFrameIncrease *= this.speedMod;
                                    }

                                    this.setShootable(true);
                                 }
                              }
                           }

                           this.shootable = true;
                           this.solid = true;
                           this.tryThump((IsoGridSquare)null);
                           this.damageSheetRope();
                           this.AllowRepathDelay = PZMath.clamp(this.AllowRepathDelay - GameTime.instance.getMultiplier(), 0.0F, 3.4028235E38F);
                           if (this.TimeSinceSeenFlesh > (float)this.memory && this.target != null) {
                              this.setTarget((IsoMovingObject)null);
                           }

                           if (this.target instanceof IsoGameCharacter && ((IsoGameCharacter)this.target).ReanimatedCorpse != null) {
                              this.setTarget((IsoMovingObject)null);
                           }

                           if (this.target != null) {
                              this.vectorToTarget.x = this.target.getX();
                              this.vectorToTarget.y = this.target.getY();
                              Vector2 var9 = this.vectorToTarget;
                              var9.x -= this.getX();
                              var9 = this.vectorToTarget;
                              var9.y -= this.getY();
                              this.updateZombieTripping();
                           }

                           int var3;
                           if (this.getSquare() != null && this.getSquare().hasFarmingPlant()) {
                              var3 = (int)(100.0F / GameTime.getInstance().getTrueMultiplier());
                              var3 = Math.max(1, var3);
                              if (Rand.NextBool(var3)) {
                                 this.getSquare().destroyFarmingPlant();
                              }
                           }

                           if (this.getSquare() != null && this.getSquare().hasLitCampfire() && this.isMoving()) {
                              var3 = (int)(10000.0F / GameTime.getInstance().getTrueMultiplier());
                              var3 = Math.max(1, var3);
                              if (Rand.NextBool(var3)) {
                                 this.getSquare().putOutCampfire();
                              }
                           }

                           if (this.getCurrentState() != PathFindState.instance() && this.getCurrentState() != WalkTowardState.instance() && this.getCurrentState() != ClimbThroughWindowState.instance()) {
                              this.setLastHeardSound(-1, -1, -1);
                           }

                           if (this.TimeSinceSeenFlesh > 240.0F && this.timeSinceRespondToSound > 5.0F) {
                              this.RespondToSound();
                           }

                           this.timeSinceRespondToSound += GameTime.getInstance().getThirtyFPSMultiplier();
                           this.separate();
                           this.updateSearchForCorpse();
                           if (this.TimeSinceSeenFlesh > 2000.0F && this.timeSinceRespondToSound > 2000.0F) {
                              ZombieGroupManager.instance.update(this);
                           }

                        }
                     }
                  }
               } else {
                  super.update();
               }
            }
         }
      } else {
         DebugLog.log(DebugType.Zombie, "removing stale zombie 5000 id=" + this.OnlineID);
         DebugLog.log("Zombie: removing stale zombie 5000 id=" + this.OnlineID);
         VirtualZombieManager.instance.removeZombieFromWorld(this);
      }
   }

   protected void calculateStats() {
   }

   private void updateZombieTripping() {
      if (this.speedType == 1 && StringUtils.isNullOrEmpty(this.getBumpType()) && this.target != null && Rand.NextBool(Rand.AdjustForFramerate(750))) {
         this.setBumpType("trippingFromSprint");
      }

   }

   public int getVoiceChoice() {
      if (this.voiceChoice == -1) {
         this.voiceChoice = Rand.Next(3) + 1;
      }

      return this.voiceChoice;
   }

   public String getVoiceSoundName() {
      String var1 = this.getDescriptor().getVoicePrefix();
      String var2 = this.speedType == 1 ? "Sprinter" : "";
      String var3 = "Voice";
      String var10000;
      switch (this.getVoiceChoice()) {
         case 2:
            var10000 = "B";
            break;
         case 3:
            var10000 = "C";
            break;
         default:
            var10000 = "A";
      }

      String var4 = var10000;
      return var1 + var2 + var3 + var4;
   }

   public String getBiteSoundName() {
      String var1 = this.getDescriptor().getVoicePrefix();
      String var2 = this.speedType == 1 ? "Sprinter" : "";
      String var3 = "Bite";
      String var10000;
      switch (this.getVoiceChoice()) {
         case 2:
            var10000 = "B";
            break;
         case 3:
            var10000 = "C";
            break;
         default:
            var10000 = "A";
      }

      String var4 = var10000;
      return var1 + var2 + var3 + var4;
   }

   public void updateVocalProperties() {
      if (!GameServer.bServer) {
         boolean var1 = SoundManager.instance.isListenerInRange(this.getX(), this.getY(), 20.0F);
         if (this.vocalEvent != 0L && !this.getEmitter().isPlaying(this.vocalEvent)) {
            this.vocalEvent = 0L;
         }

         boolean var2 = !this.isDead() && !this.isReanimatedForGrappleOnly();
         if (var2 && !this.isFakeDead() && var1) {
            ZombieVocalsManager.instance.addCharacter(this);
         }

         if (this.vocalEvent != 0L && var2 && this.isFakeDead() && this.getEmitter().isPlaying(this.vocalEvent)) {
            this.getEmitter().stopSoundLocal(this.vocalEvent);
            this.vocalEvent = 0L;
         }

      }
   }

   public void setVehicleHitLocation(BaseVehicle var1) {
      if (!this.getFMODParameters().parameterList.contains(this.parameterVehicleHitLocation)) {
         this.getFMODParameters().add(this.parameterVehicleHitLocation);
      }

      ParameterVehicleHitLocation.HitLocation var2 = ParameterVehicleHitLocation.calculateLocation(var1, this.getX(), this.getY(), this.getZ());
      this.parameterVehicleHitLocation.setLocation(var2);
   }

   private void updateSearchForCorpse() {
      if (!this.bCrawling && this.target == null && this.eatBodyTarget == null) {
         if (this.bodyToEat != null) {
            if (this.bodyToEat.getStaticMovingObjectIndex() == -1) {
               this.bodyToEat = null;
            } else if (!this.isEatingOther(this.bodyToEat) && this.bodyToEat.getEatingZombies().size() >= 3) {
               this.bodyToEat = null;
            }
         }

         if (this.bodyToEat == null) {
            this.checkForCorpseTimer -= GameTime.getInstance().getThirtyFPSMultiplier();
            if (this.checkForCorpseTimer <= 0.0F) {
               this.checkForCorpseTimer = 10000.0F;
               tempBodies.clear();

               for(int var1 = -10; var1 < 10; ++var1) {
                  for(int var2 = -10; var2 < 10; ++var2) {
                     IsoGridSquare var3 = this.getCell().getGridSquare((double)(this.getX() + (float)var1), (double)(this.getY() + (float)var2), (double)this.getZ());
                     if (var3 != null) {
                        IsoDeadBody var4 = var3.getDeadBody();
                        if (var4 != null && !var4.isSkeleton() && !var4.isZombie() && var4.getEatingZombies().size() < 3 && !var4.isAnimal() && !var4.isAnimalSkeleton() && !PolygonalMap2.instance.lineClearCollide(this.getX(), this.getY(), var4.getX(), var4.getY(), PZMath.fastfloor(this.getZ()), (IsoMovingObject)null, false, true)) {
                           tempBodies.add(var4);
                        }
                     }
                  }
               }

               if (!tempBodies.isEmpty()) {
                  this.bodyToEat = (IsoDeadBody)PZArrayUtil.pickRandom((List)tempBodies);
                  tempBodies.clear();
               }
            }
         }

         if (this.bodyToEat != null && this.isCurrentState(ZombieIdleState.instance())) {
            if (this.DistToSquared(this.bodyToEat) > 1.0F) {
               Vector2 var5 = tempo.set(this.getX() - this.bodyToEat.getX(), this.getY() - this.bodyToEat.getY());
               var5.setLength(0.5F);
               this.pathToLocationF(this.bodyToEat.getX() + var5.x, this.bodyToEat.getY() + var5.y, this.bodyToEat.getZ());
            }

         }
      } else {
         this.checkForCorpseTimer = 10000.0F;
         this.bodyToEat = null;
      }
   }

   private void damageSheetRope() {
      if (Rand.Next(30) == 0 && this.current != null && (this.current.Is(IsoFlagType.climbSheetN) || this.current.Is(IsoFlagType.climbSheetE) || this.current.Is(IsoFlagType.climbSheetS) || this.current.Is(IsoFlagType.climbSheetW))) {
         IsoObject var1 = this.current.getSheetRope();
         if (var1 != null) {
            var1.sheetRopeHealth -= (float)Rand.Next(5, 15);
            if (var1.sheetRopeHealth < 40.0F) {
               this.current.damageSpriteSheetRopeFromBottom((IsoPlayer)null, this.current.Is(IsoFlagType.climbSheetN) || this.current.Is(IsoFlagType.climbSheetS));
               this.current.RecalcProperties();
            }

            if (var1.sheetRopeHealth <= 0.0F) {
               this.current.removeSheetRopeFromBottom((IsoPlayer)null, this.current.Is(IsoFlagType.climbSheetN) || this.current.Is(IsoFlagType.climbSheetS));
            }
         }
      }

   }

   public void getZombieWalkTowardSpeed(float var1, float var2, Vector2 var3) {
      float var4 = 1.0F;
      var4 = var2 / 24.0F;
      if (var4 < 1.0F) {
         var4 = 1.0F;
      }

      if (var4 > 1.3F) {
         var4 = 1.3F;
      }

      var3.setLength((var1 * this.getSpeedMod() + 0.006F) * var4);
      if (SandboxOptions.instance.Lore.Speed.getValue() == 1 && !this.inactive || this.speedType == 1) {
         var3.setLength(0.08F);
         this.bRunning = true;
      }

      if (var3.getLength() > var2) {
         var3.setLength(var2);
      }

   }

   public void getZombieLungeSpeed() {
      this.bRunning = SandboxOptions.instance.Lore.Speed.getValue() == 1 && !this.inactive || this.speedType == 1;
   }

   public boolean tryThump(IsoGridSquare var1) {
      if (this.Ghost) {
         return false;
      } else if (this.bCrawling) {
         return false;
      } else {
         boolean var2 = this.isCurrentState(PathFindState.instance()) || this.isCurrentState(LungeState.instance()) || this.isCurrentState(LungeNetworkState.instance()) || this.isCurrentState(WalkTowardState.instance()) || this.isCurrentState(WalkTowardNetworkState.instance());
         if (!var2) {
            return false;
         } else {
            IsoGridSquare var3;
            if (var1 != null) {
               var3 = var1;
            } else {
               var3 = this.getFeelerTile(this.getFeelersize());
            }

            if (var3 != null && this.current != null) {
               IsoObject var4 = this.current.testCollideSpecialObjects(var3);
               IsoDoor var5 = (IsoDoor)Type.tryCastTo(var4, IsoDoor.class);
               IsoThumpable var6 = (IsoThumpable)Type.tryCastTo(var4, IsoThumpable.class);
               IsoWindow var7 = (IsoWindow)Type.tryCastTo(var4, IsoWindow.class);
               IsoWindowFrame var8 = (IsoWindowFrame)Type.tryCastTo(var4, IsoWindowFrame.class);
               if (var7 != null && var7.canClimbThrough(this)) {
                  if (!this.isFacingObject(var7, 0.8F)) {
                     return false;
                  } else {
                     this.climbThroughWindow(var7);
                     return true;
                  }
               } else if (var8 != null && var8.canClimbThrough(this)) {
                  if (!this.isFacingObject(var8, 0.8F)) {
                     return false;
                  } else {
                     this.climbThroughWindowFrame(var8);
                     return true;
                  }
               } else if (var6 != null && var6.canClimbThrough(this)) {
                  this.climbThroughWindow(var6);
                  return true;
               } else if (var6 != null && var6.getThumpableFor(this) != null || var7 != null && var7.getThumpableFor(this) != null || var8 != null && var8.getThumpableFor(this) != null || var5 != null && var5.getThumpableFor(this) != null) {
                  int var9 = var3.getX() - this.current.getX();
                  int var10 = var3.getY() - this.current.getY();
                  IsoDirections var11 = IsoDirections.N;
                  if (var9 < 0 && Math.abs(var9) > Math.abs(var10)) {
                     var11 = IsoDirections.S;
                  }

                  if (var9 < 0 && Math.abs(var9) <= Math.abs(var10)) {
                     var11 = IsoDirections.SW;
                  }

                  if (var9 > 0 && Math.abs(var9) > Math.abs(var10)) {
                     var11 = IsoDirections.W;
                  }

                  if (var9 > 0 && Math.abs(var9) <= Math.abs(var10)) {
                     var11 = IsoDirections.SE;
                  }

                  if (var10 < 0 && Math.abs(var9) < Math.abs(var10)) {
                     var11 = IsoDirections.N;
                  }

                  if (var10 < 0 && Math.abs(var9) >= Math.abs(var10)) {
                     var11 = IsoDirections.NW;
                  }

                  if (var10 > 0 && Math.abs(var9) < Math.abs(var10)) {
                     var11 = IsoDirections.E;
                  }

                  if (var10 > 0 && Math.abs(var9) >= Math.abs(var10)) {
                     var11 = IsoDirections.NE;
                  }

                  if (this.getDir() == var11) {
                     boolean var12 = this.getPathFindBehavior2().isGoalSound() && (this.isCurrentState(PathFindState.instance()) || this.isCurrentState(WalkTowardState.instance()) || this.isCurrentState(WalkTowardNetworkState.instance()));
                     if (SandboxOptions.instance.Lore.ThumpNoChasing.getValue() || this.target != null || var12) {
                        if (var7 != null && var7.getThumpableFor(this) != null) {
                           var4 = (IsoObject)var7.getThumpableFor(this);
                        }

                        this.setThumpTarget(var4);
                        this.setPath2((Path)null);
                     }
                  }

                  return true;
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      }
   }

   public void Wander() {
      this.changeState(ZombieIdleState.instance());
   }

   public void DoZombieInventory() {
      this.DoZombieInventory(false);
   }

   public void DoCorpseInventory() {
      this.DoZombieInventory(true);
   }

   private void DoZombieInventory(boolean var1) {
      if (!this.isReanimatedPlayer() && !this.wasFakeDead()) {
         if (!GameServer.bServer || var1) {
            this.getInventory().removeAllItems();
            this.getInventory().setSourceGrid(this.getCurrentSquare());
            this.wornItems.setFromItemVisuals(this.itemVisuals);
            this.wornItems.addItemsToItemContainer(this.getInventory());

            int var2;
            for(var2 = 0; var2 < this.attachedItems.size(); ++var2) {
               AttachedItem var3 = this.attachedItems.get(var2);
               InventoryItem var4 = var3.getItem();
               if (!this.getInventory().contains(var4)) {
                  if (var4.hasTag("ApplyOwnerName") && this.getDescriptor() != null) {
                     var4.nameAfterDescriptor(this.getDescriptor());
                  } else if (var4.hasTag("MonogramOwnerName") && this.getDescriptor() != null) {
                     var4.monogramAfterDescriptor(this.getDescriptor());
                  }

                  var4.setContainer(this.getInventory());
                  this.getInventory().getItems().add(var4);
               }
            }

            if (this.itemsToSpawnAtDeath != null && !this.itemsToSpawnAtDeath.isEmpty()) {
               for(var2 = 0; var2 < this.itemsToSpawnAtDeath.size(); ++var2) {
                  InventoryItem var5 = (InventoryItem)this.itemsToSpawnAtDeath.get(var2);
                  if (var5.hasTag("ApplyOwnerName") && this.getDescriptor() != null) {
                     var5.nameAfterDescriptor(this.getDescriptor());
                  } else if (var5.hasTag("MonogramOwnerName") && this.getDescriptor() != null) {
                     var5.monogramAfterDescriptor(this.getDescriptor());
                  }

                  ItemSpawner.spawnItem((InventoryItem)this.itemsToSpawnAtDeath.get(var2), this.inventory);
               }

               this.itemsToSpawnAtDeath.clear();
            }

            DebugLog.Death.trace("id=%d, inventory=%d", this.getOnlineID(), this.getInventory().getItems().size());
         }
      }
   }

   public void DoZombieStats() {
      if (SandboxOptions.instance.Lore.Cognition.getValue() == 1) {
         this.cognition = 1;
      }

      if (SandboxOptions.instance.Lore.Cognition.getValue() == 4) {
         this.cognition = Rand.Next(0, 2);
      }

      if (this.strength == -1 && SandboxOptions.instance.Lore.Strength.getValue() == 1) {
         this.strength = 5;
      }

      if (this.strength == -1 && SandboxOptions.instance.Lore.Strength.getValue() == 2) {
         this.strength = 3;
      }

      if (this.strength == -1 && SandboxOptions.instance.Lore.Strength.getValue() == 3) {
         this.strength = 1;
      }

      if (this.strength == -1 && SandboxOptions.instance.Lore.Strength.getValue() == 4) {
         this.strength = Rand.Next(1, 5);
      }

      int var1 = SandboxOptions.instance.Lore.Memory.getValue();
      int var2 = -1;
      if (var1 == 5) {
         var2 = Rand.Next(4);
      } else if (var1 == 6) {
         var2 = Rand.Next(3) + 1;
      }

      if (this.memory == -1 && var1 == 1 || var2 == 0) {
         this.memory = 1250;
      }

      if (this.memory == -1 && var1 == 2 || var2 == 1) {
         this.memory = 800;
      }

      if (this.memory == -1 && var1 == 3 || var2 == 2) {
         this.memory = 500;
      }

      if (this.memory == -1 && var1 == 4 || var2 == 3) {
         this.memory = 25;
      }

      if (SandboxOptions.instance.Lore.Sight.getValue() == 4) {
         this.sight = Rand.Next(3) + 1;
      } else if (SandboxOptions.instance.Lore.Sight.getValue() == 5) {
         this.sight = Rand.Next(2) + 2;
      } else {
         this.sight = SandboxOptions.instance.Lore.Sight.getValue();
      }

      if (SandboxOptions.instance.Lore.Hearing.getValue() == 4) {
         this.hearing = Rand.Next(3) + 1;
      } else if (SandboxOptions.instance.Lore.Hearing.getValue() == 5) {
         this.hearing = Rand.Next(2) + 2;
      } else {
         this.hearing = SandboxOptions.instance.Lore.Hearing.getValue();
      }

      this.doZombieSpeed();
      this.initCanCrawlUnderVehicle();
   }

   public void setWalkType(String var1) {
      this.walkType = var1;
   }

   public void DoZombieSpeeds(float var1) {
      IsoSpriteInstance var10000;
      if (this.bCrawling) {
         this.speedMod = var1;
         var10000 = this.def;
         var10000.AnimFrameIncrease *= 0.8F;
      } else if (Rand.Next(3) == 0 && SandboxOptions.instance.Lore.Speed.getValue() != 3) {
         if (SandboxOptions.instance.Lore.Speed.getValue() != 3) {
            this.bLunger = true;
            this.speedMod = var1;
            this.walkVariant = this.walkVariant + "2";
            this.def.setFrameSpeedPerFrame(0.24F);
            var10000 = this.def;
            var10000.AnimFrameIncrease *= this.speedMod;
         }
      } else {
         this.speedMod = var1;
         this.speedMod += (float)Rand.Next(1500) / 10000.0F;
         this.walkVariant = this.walkVariant + "1";
         this.def.setFrameSpeedPerFrame(0.24F);
         var10000 = this.def;
         var10000.AnimFrameIncrease *= this.speedMod;
      }

   }

   public boolean isFakeDead() {
      if (SandboxOptions.instance.Lore.DisableFakeDead.getValue() == 3) {
         this.bFakeDead = false;
      }

      if (this != null && this.getSquare() != null && this.getSquare().HasStairs() && this.bFakeDead) {
         this.bFakeDead = false;
      }

      return this.bFakeDead;
   }

   public void setFakeDead(boolean var1) {
      if (this != null && this.getSquare() != null && this.getSquare().HasStairs()) {
         this.bFakeDead = false;
      } else {
         if (var1 && Rand.Next(2) == 0) {
            this.setCrawlerType(2);
         }

         this.bFakeDead = var1;
      }
   }

   public boolean isForceFakeDead() {
      return this.bForceFakeDead;
   }

   public void setForceFakeDead(boolean var1) {
      this.bForceFakeDead = var1;
   }

   public float Hit(BaseVehicle var1, float var2, boolean var3, Vector2 var4) {
      float var5 = 0.0F;
      this.AttackedBy = var1.getDriver();
      if (this.AttackedBy != null) {
         this.AttackedBy.setUseHandWeapon((HandWeapon)null);
      }

      this.setHitDir(var4);
      this.setHitForce(var2 * 0.15F);
      int var6 = (int)(var2 * 6.0F);
      this.setTarget(var1.getDriver());
      if (!this.isStaggerBack() && !this.isOnFloor() && this.getCurrentState() != ZombieGetUpState.instance() && this.getCurrentState() != ZombieOnGroundState.instance()) {
         boolean var7 = this.isStaggerBack();
         boolean var8 = this.isKnockedDown();
         boolean var9 = this.isBecomeCrawler();
         if (var3) {
            this.setHitFromBehind(true);
            if (Rand.Next(100) <= var6) {
               if (Rand.Next(5) == 0) {
                  var9 = true;
               }

               var7 = true;
               var8 = true;
            } else {
               var7 = true;
            }
         } else if (var2 < 3.0F) {
            if (Rand.Next(100) <= var6) {
               if (Rand.Next(8) == 0) {
                  var9 = true;
               }

               var7 = true;
               var8 = true;
            } else {
               var7 = true;
            }
         } else if (var2 < 10.0F) {
            if (Rand.Next(8) == 0) {
               var9 = true;
            }

            var7 = true;
            var8 = true;
         } else {
            var5 = this.getHealth();
            if (!GameServer.bServer && !GameClient.bClient && !this.isGodMod()) {
               CombatManager.getInstance().applyDamage((IsoGameCharacter)this, var5);
               this.Kill(var1.getDriver());
            }
         }

         if (!GameServer.bServer) {
            this.setStaggerBack(var7);
            this.setKnockedDown(var8);
            this.setBecomeCrawler(var9);
         }
      } else {
         if (this.isFakeDead()) {
            this.setFakeDead(false);
         }

         this.setHitReaction("Floor");
         var5 = var2 / 5.0F;
         if (!GameServer.bServer && !GameClient.bClient) {
            CombatManager.getInstance().applyDamage((IsoGameCharacter)this, var5);
            if (this.isDead()) {
               this.Kill(var1.getDriver());
            }
         }
      }

      if (!GameServer.bServer && !GameClient.bClient) {
         this.addBlood(var2);
      }

      return var5;
   }

   public void addBlood(float var1) {
      if (!((float)Rand.Next(10) > var1)) {
         float var2 = 0.6F;
         if (SandboxOptions.instance.BloodLevel.getValue() > 1) {
            int var3 = Rand.Next(4, 10);
            if (var3 < 1) {
               var3 = 1;
            }

            if (Core.bLastStand) {
               var3 *= 3;
            }

            switch (SandboxOptions.instance.BloodLevel.getValue()) {
               case 2:
                  var3 /= 2;
               case 3:
               default:
                  break;
               case 4:
                  var3 *= 2;
                  break;
               case 5:
                  var3 *= 5;
            }

            for(int var4 = 0; var4 < var3; ++var4) {
               this.splatBlood(2, 0.3F);
            }
         }

         if (SandboxOptions.instance.BloodLevel.getValue() > 1) {
            this.splatBloodFloorBig();
         }

         if (SandboxOptions.instance.BloodLevel.getValue() > 1) {
            this.playBloodSplatterSound();
            new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + var2, this.getHitDir().x * 1.5F, this.getHitDir().y * 1.5F);
            tempo.x = this.getHitDir().x;
            tempo.y = this.getHitDir().y;
            byte var8 = 3;
            byte var7 = 0;
            byte var5 = 1;
            switch (SandboxOptions.instance.BloodLevel.getValue()) {
               case 1:
                  var5 = 0;
                  break;
               case 2:
                  var5 = 1;
                  var8 = 5;
                  var7 = 2;
               case 3:
               default:
                  break;
               case 4:
                  var5 = 3;
                  var8 = 2;
                  break;
               case 5:
                  var5 = 10;
                  var8 = 0;
            }

            for(int var6 = 0; var6 < var5; ++var6) {
               if (Rand.Next(this.isCloseKilled() ? 8 : var8) == 0) {
                  new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + var2, this.getHitDir().x * 1.5F, this.getHitDir().y * 1.5F);
               }

               if (Rand.Next(this.isCloseKilled() ? 8 : var8) == 0) {
                  new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + var2, this.getHitDir().x * 1.8F, this.getHitDir().y * 1.8F);
               }

               if (Rand.Next(this.isCloseKilled() ? 8 : var8) == 0) {
                  new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + var2, this.getHitDir().x * 1.9F, this.getHitDir().y * 1.9F);
               }

               if (Rand.Next(this.isCloseKilled() ? 4 : var7) == 0) {
                  new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + var2, this.getHitDir().x * 3.9F, this.getHitDir().y * 3.9F);
               }

               if (Rand.Next(this.isCloseKilled() ? 4 : var7) == 0) {
                  new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + var2, this.getHitDir().x * 3.8F, this.getHitDir().y * 3.8F);
               }

               if (Rand.Next(this.isCloseKilled() ? 9 : 6) == 0) {
                  new IsoZombieGiblets(IsoZombieGiblets.GibletType.Eye, this.getCell(), this.getX(), this.getY(), this.getZ() + var2, this.getHitDir().x * 0.8F, this.getHitDir().y * 0.8F);
               }
            }
         }

      }
   }

   public void hitConsequences(HandWeapon var1, IsoGameCharacter var2, boolean var3, float var4, boolean var5) {
      if (!this.isOnlyJawStab() || this.isCloseKilled()) {
         super.hitConsequences(var1, var2, var3, var4, var5);
         if (Core.bDebug) {
            DebugLog.Combat.debugln("Zombie #" + this.OnlineID + " got hit for " + var4);
         }

         this.getActionContext().reportEvent("wasHit");
         if (!var5) {
            CombatManager.getInstance().processHit(var1, var2, this);
            RagdollSettingsManager var6 = RagdollSettingsManager.getInstance();
            this.setUsePhysicHitReaction(var6.usePhysicHitReaction(this));
         }

         if (!GameClient.bClient || this.target == null || var2 == this.target || !(IsoUtils.DistanceToSquared(this.getX(), this.getY(), this.target.getX(), this.target.getY()) < 10.0F)) {
            this.setTarget(var2);
         }

         if (!GameServer.bServer && !GameClient.bClient || GameClient.bClient && var2 instanceof IsoPlayer && ((IsoPlayer)var2).isLocalPlayer() && !this.isRemoteZombie()) {
            this.setKnockedDown(var2.isCriticalHit() || this.isOnFloor() || this.isAlwaysKnockedDown());
         }

         this.checkClimbOverFenceHit();
         this.checkClimbThroughWindowHit();
         if (this.shouldBecomeCrawler(var2)) {
            this.setBecomeCrawler(true);
         }

      }
   }

   public long playHurtSound() {
      return 0L;
   }

   private void checkClimbOverFenceHit() {
      if (!this.isOnFloor()) {
         if (this.isCurrentState(ClimbOverFenceState.instance()) && this.getVariableBoolean("ClimbFenceStarted") && !this.isVariable("ClimbFenceOutcome", "fall") && !this.getVariableBoolean("ClimbFenceFlopped")) {
            HashMap var1 = (HashMap)this.StateMachineParams.get(ClimbOverFenceState.instance());
            byte var2 = 3;
            byte var3 = 4;
            int var4 = (Integer)var1.get(Integer.valueOf(var2));
            int var5 = (Integer)var1.get(Integer.valueOf(var3));
            this.climbFenceWindowHit(var4, var5);
         }
      }
   }

   private void checkClimbThroughWindowHit() {
      if (!this.isOnFloor()) {
         if (this.isCurrentState(ClimbThroughWindowState.instance()) && this.getVariableBoolean("ClimbWindowStarted") && !this.isVariable("ClimbWindowOutcome", "fall") && !this.getVariableBoolean("ClimbWindowFlopped")) {
            ClimbThroughWindowPositioningParams var1 = ClimbThroughWindowState.instance().getPositioningParams(this);
            if (var1 != null) {
               this.climbFenceWindowHit(var1.endX, var1.endY);
            }
         }
      }
   }

   private void climbFenceWindowHit(int var1, int var2) {
      if (this.getDir() == IsoDirections.W) {
         this.setX((float)var1 + 0.9F);
         this.setLastX(this.getX());
      } else if (this.getDir() == IsoDirections.E) {
         this.setX((float)var1 + 0.1F);
         this.setLastX(this.getX());
      } else if (this.getDir() == IsoDirections.N) {
         this.setY((float)var2 + 0.9F);
         this.setLastY(this.getY());
      } else if (this.getDir() == IsoDirections.S) {
         this.setY((float)var2 + 0.1F);
         this.setLastY(this.getY());
      }

      this.setStaggerBack(false);
      this.setKnockedDown(true);
      this.setOnFloor(true);
      this.setFallOnFront(true);
      this.setHitReaction("FenceWindow");
   }

   private boolean shouldBecomeCrawler(IsoGameCharacter var1) {
      if (this.isBecomeCrawler()) {
         return true;
      } else if (this.isCrawling()) {
         return false;
      } else if (Core.bLastStand) {
         return false;
      } else if (this.isDead()) {
         return false;
      } else if (this.isCloseKilled()) {
         return false;
      } else {
         IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
         if (var2 != null && !var2.isAimAtFloor() && var2.isDoShove()) {
            return false;
         } else {
            byte var3 = 30;
            if (var2 != null && var2.isAimAtFloor() && var2.isDoShove()) {
               if (this.isHitLegsWhileOnFloor()) {
                  var3 = 7;
               } else {
                  var3 = 15;
               }
            }

            return Rand.NextBool(var3);
         }
      }
   }

   public void removeFromWorld() {
      this.getEmitter().stopOrTriggerSoundByName("BurningFlesh");
      this.clearAggroList();
      VirtualZombieManager.instance.RemoveZombie(this);
      this.setPath2((Path)null);
      if (PathfindNative.USE_NATIVE_CODE) {
         PathfindNative.instance.cancelRequest(this);
      } else {
         PolygonalMap2.instance.cancelRequest(this);
      }

      if (this.getFinder().progress != AStarPathFinder.PathFindProgress.notrunning && this.getFinder().progress != AStarPathFinder.PathFindProgress.found) {
         this.getFinder().progress = AStarPathFinder.PathFindProgress.notrunning;
      }

      if (this.group != null) {
         this.group.remove(this);
         this.group = null;
      }

      if (GameServer.bServer && this.OnlineID != -1) {
         ServerMap.instance.ZombieMap.remove(this.OnlineID);
         this.OnlineID = -1;
      }

      if (GameClient.bClient) {
         GameClient.instance.removeZombieFromCache(this);
      }

      this.getCell().getZombieList().remove(this);
      if (GameServer.bServer) {
         if (this.authOwner != null || this.authOwnerPlayer != null) {
            NetworkZombieManager.getInstance().moveZombie(this, (UdpConnection)null, (IsoPlayer)null);
         }

         this.zombiePacketUpdated = false;
      }

      this.imposter.destroy();
      super.removeFromWorld();
   }

   public void resetForReuse() {
      this.setCrawler(false);
      this.initializeStates();
      this.getActionContext().setGroup(ActionGroup.getActionGroup("zombie"));
      this.advancedAnimator.OnAnimDataChanged(false);
      this.setStateMachineLocked(false);
      this.setDefaultState();
      if (this.vocalEvent != 0L) {
         this.getEmitter().stopSoundLocal(this.vocalEvent);
         this.vocalEvent = 0L;
      }

      this.parameterZombieState.setState(ParameterZombieState.State.Idle);
      this.setSceneCulled(true);
      this.releaseAnimationPlayer();
      Arrays.fill(this.IsVisibleToPlayer, false);
      this.setCurrent((IsoGridSquare)null);
      this.setLast((IsoGridSquare)null);
      this.setOnFloor(false);
      this.setCanWalk(true);
      this.setFallOnFront(false);
      this.setHitTime(0);
      this.strength = -1;
      this.setImmortalTutorialZombie(false);
      this.setOnlyJawStab(false);
      this.setAlwaysKnockedDown(false);
      this.setForceEatingAnimation(false);
      this.setNoTeeth(false);
      this.cognition = -1;
      this.speedType = -1;
      this.bodyToEat = null;
      this.checkForCorpseTimer = 10000.0F;
      this.clearAttachedItems();
      this.target = null;
      this.setEatBodyTarget((IsoMovingObject)null, false);
      this.setSkeleton(false);
      this.setReanimatedPlayer(false);
      this.setBecomeCrawler(false);
      this.setWasFakeDead(false);
      this.setKnifeDeath(false);
      this.setJawStabAttach(false);
      this.setReanimate(false);
      this.DoZombieStats();
      this.alerted = false;
      this.BonusSpotTime = 0.0F;
      this.TimeSinceSeenFlesh = 100000.0F;
      this.soundReactDelay = 0.0F;
      this.delayedSound.x = this.delayedSound.y = this.delayedSound.z = -1;
      this.bSoundSourceRepeating = false;
      this.soundSourceTarget = null;
      this.soundAttract = 0.0F;
      this.soundAttractTimeout = 0.0F;
      if (SandboxOptions.instance.Lore.Toughness.getValue() == 1) {
         this.setHealth(3.5F + Rand.Next(0.0F, 0.3F));
      }

      if (SandboxOptions.instance.Lore.Toughness.getValue() == 2) {
         this.setHealth(1.8F + Rand.Next(0.0F, 0.3F));
      }

      if (SandboxOptions.instance.Lore.Toughness.getValue() == 3) {
         this.setHealth(0.5F + Rand.Next(0.0F, 0.3F));
      }

      if (SandboxOptions.instance.Lore.Toughness.getValue() == 4) {
         this.setHealth(Rand.Next(0.5F, 3.5F) + Rand.Next(0.0F, 0.3F));
      }

      this.setCollidable(true);
      this.setShootable(true);
      if (this.isOnFire()) {
         IsoFireManager.RemoveBurningCharacter(this);
         this.setOnFire(false);
      }

      if (this.AttachedAnimSprite != null) {
         this.AttachedAnimSprite.clear();
      }

      this.OnlineID = -1;
      this.bIndoorZombie = false;
      this.setVehicle4TestCollision((BaseVehicle)null);
      this.clearItemsToSpawnAtDeath();
      this.m_persistentOutfitId = 0;
      this.m_bPersistentOutfitInit = false;
      this.sharedDesc = null;
      this.imposter.destroy();
      if (this.hasModData()) {
         this.getModData().wipe();
      }

      this.setInvulnerable(false);
   }

   public boolean wasFakeDead() {
      return this.bWasFakeDead;
   }

   public void setWasFakeDead(boolean var1) {
      this.bWasFakeDead = var1;
   }

   public void setCrawler(boolean var1) {
      this.bCrawling = var1;
   }

   public boolean isBecomeCrawler() {
      return this.bBecomeCrawler;
   }

   public void setBecomeCrawler(boolean var1) {
      if (!this.isInvulnerable()) {
         this.bBecomeCrawler = var1;
      }

   }

   public boolean isReanimate() {
      return this.bReanimate;
   }

   public void setReanimate(boolean var1) {
      this.bReanimate = var1;
   }

   public boolean isReanimatedPlayer() {
      return this.bReanimatedPlayer;
   }

   public void setReanimatedPlayer(boolean var1) {
      this.bReanimatedPlayer = var1;
   }

   public IsoPlayer getReanimatedPlayer() {
      for(int var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
         IsoPlayer var2 = IsoPlayer.players[var1];
         if (var2 != null && var2.ReanimatedCorpse == this) {
            return var2;
         }
      }

      return null;
   }

   public void setFemaleEtc(boolean var1) {
      this.setFemale(var1);
      if (this.getDescriptor() != null) {
         this.getDescriptor().setFemale(var1);
         this.getDescriptor().setVoicePrefix(var1 ? "FemaleZombie" : "MaleZombie");
      }

      this.SpriteName = var1 ? "KateZ" : "BobZ";
      this.hurtSound = this.getDescriptor().getVoicePrefix() + "Hurt";
      if (this.vocalEvent != 0L) {
         String var2 = this.getVoiceSoundName();
         if (this.getEmitter() != null && !this.getEmitter().isPlaying(var2)) {
            this.getEmitter().stopSoundLocal(this.vocalEvent);
            this.vocalEvent = 0L;
         }
      }

   }

   public void addRandomBloodDirtHolesEtc() {
      if (!this.isSkeleton()) {
         this.addRandomVisualDamages();
         this.addRandomVisualBandages();
      }

      float var1 = (float)SandboxOptions.instance.ClothingDegradation.getValue();
      if (var1 != 1.0F) {
         var1 = (var1 - 1.0F) / 2.0F;
         var1 *= var1;
         boolean var2 = var1 >= 1.0F;
         this.addBlood((BloodBodyPartType)null, false, true, false);
         int var3 = (int)(IsoWorld.instance.getWorldAgeDays() / 30.0F * var1);
         this.addLotsOfDirt((BloodBodyPartType)null, (int)((float)(Rand.Next(5, 20) + Math.min(var3, 24)) * var1), var2);
         int var4 = Math.max(8 - var3, 0);
         int var5 = PZMath.clamp(var3, 5, 10);
         var5 = (int)((float)var5 * var1);

         int var6;
         for(var6 = 0; var6 < var5; ++var6) {
            if (OutfitRNG.NextBool(var4)) {
               this.addBlood((BloodBodyPartType)null, false, true, false);
            }

            if (OutfitRNG.NextBool(var4 / 2)) {
               this.addLotsOfDirt((BloodBodyPartType)null, (Integer)null, var2);
            }
         }

         var5 = PZMath.clamp(var3, 8, 16);
         var5 = (int)((float)var5 * var1);
         var4 /= 2;

         BloodBodyPartType var7;
         for(var6 = 0; var6 < var5; ++var6) {
            if (OutfitRNG.NextBool(var4)) {
               var7 = BloodBodyPartType.FromIndex(OutfitRNG.Next(0, BloodBodyPartType.MAX.index()));
               this.addHole(var7);
               this.addBlood(var7, true, false, false);
            }
         }

         for(var6 = 0; var6 < var5; ++var6) {
            if (OutfitRNG.NextBool(var4)) {
               var7 = BloodBodyPartType.FromIndex(OutfitRNG.Next(0, BloodBodyPartType.MAX.index()));
               this.addHole(var7);
               this.addLotsOfDirt(var7, 1, var2);
            }
         }

      }
   }

   public void useDescriptor(SharedDescriptors.Descriptor var1) {
      this.getHumanVisual().clear();
      this.itemVisuals.clear();
      this.m_persistentOutfitId = var1 == null ? 0 : var1.getPersistentOutfitID();
      this.m_bPersistentOutfitInit = true;
      this.sharedDesc = var1;
      if (var1 != null) {
         this.setFemaleEtc(var1.isFemale());
         this.getHumanVisual().copyFrom(var1.getHumanVisual());
         this.getWornItems().setFromItemVisuals(var1.itemVisuals);
         this.onWornItemsChanged();
      }
   }

   public SharedDescriptors.Descriptor getSharedDescriptor() {
      return this.sharedDesc;
   }

   public int getSharedDescriptorID() {
      return this.getPersistentOutfitID();
   }

   public int getScreenProperX(int var1) {
      return (int)(IsoUtils.XToScreen(this.getX(), this.getY(), this.getZ(), 0) - IsoCamera.cameras[var1].getOffX());
   }

   public int getScreenProperY(int var1) {
      return (int)(IsoUtils.YToScreen(this.getX(), this.getY(), this.getZ(), 0) - IsoCamera.cameras[var1].getOffY());
   }

   public BaseVisual getVisual() {
      return this.humanVisual;
   }

   public HumanVisual getHumanVisual() {
      return this.humanVisual;
   }

   public ItemVisuals getItemVisuals() {
      this.getItemVisuals(this.itemVisuals);
      return this.itemVisuals;
   }

   public void getItemVisuals(ItemVisuals var1) {
      if (this.isUsingWornItems()) {
         this.getWornItems().getItemVisuals(var1);
      } else if (var1 != this.itemVisuals) {
         var1.clear();
         var1.addAll(this.itemVisuals);
      }

   }

   public boolean isUsingWornItems() {
      return this.isOnKillDone() || this.isOnDeathDone() || this.isReanimatedPlayer() || this.wasFakeDead();
   }

   public void setAsSurvivor() {
      // $FF: Couldn't be decompiled
   }

   public void dressInRandomOutfit() {
      ZombiesZoneDefinition.dressInRandomOutfit(this);
   }

   public void dressInNamedOutfit(String var1) {
      this.wornItems.clear();
      this.getHumanVisual().clear();
      this.itemVisuals.clear();
      Outfit var2 = this.isFemale() ? OutfitManager.instance.FindFemaleOutfit(var1) : OutfitManager.instance.FindMaleOutfit(var1);
      if (var2 != null) {
         if (var2.isEmpty()) {
            var2.loadItems();
            this.pendingOutfitName = var1;
         } else {
            UnderwearDefinition.addRandomUnderwear(this);
            this.getDescriptor().setForename(SurvivorFactory.getRandomForename(this.isFemale()));
            this.getHumanVisual().dressInNamedOutfit(var1, this.itemVisuals, false);
            this.getHumanVisual().synchWithOutfit(this.getHumanVisual().getOutfit());
            this.onWornItemsChanged();
         }
      }
   }

   public void dressInPersistentOutfitID(int var1) {
      this.getHumanVisual().clear();
      this.itemVisuals.clear();
      this.m_persistentOutfitId = var1;
      this.m_bPersistentOutfitInit = true;
      if (var1 != 0) {
         this.bDressInRandomOutfit = false;
         PersistentOutfits.instance.dressInOutfit(this, var1);
         this.onWornItemsChanged();
      }
   }

   public void dressInClothingItem(String var1) {
      this.wornItems.clear();
      this.getHumanVisual().dressInClothingItem(var1, this.itemVisuals);
      this.onWornItemsChanged();
   }

   public boolean onDeath_ShouldDoSplatterAndSounds(HandWeapon var1, IsoGameCharacter var2, boolean var3) {
      return this.isReanimatedForGrappleOnly() && !this.isBeingGrappled() ? false : super.onDeath_ShouldDoSplatterAndSounds(var1, var2, var3);
   }

   public void onWornItemsChanged() {
      this.parameterShoeType.setShoeType((ParameterShoeType.ShoeType)null);
   }

   public void clothingItemChanged(String var1) {
      super.clothingItemChanged(var1);
      if (!StringUtils.isNullOrWhitespace(this.pendingOutfitName)) {
         Outfit var2 = this.isFemale() ? OutfitManager.instance.FindFemaleOutfit(this.pendingOutfitName) : OutfitManager.instance.FindMaleOutfit(this.pendingOutfitName);
         if (var2 != null && !var2.isEmpty()) {
            this.dressInNamedOutfit(this.pendingOutfitName);
            this.pendingOutfitName = null;
            this.resetModelNextFrame();
         }
      }

   }

   public boolean WanderFromWindow() {
      if (this.getCurrentSquare() == null) {
         return false;
      } else {
         FloodFill var1 = floodFill;
         var1.calculate(this, this.getCurrentSquare());
         IsoGridSquare var2 = var1.choose();
         var1.reset();
         if (var2 != null) {
            this.pathToLocation(var2.getX(), var2.getY(), var2.getZ());
            return true;
         } else {
            return false;
         }
      }
   }

   public boolean isUseless() {
      return this.useless;
   }

   public void setUseless(boolean var1) {
      this.useless = var1;
   }

   public void setImmortalTutorialZombie(boolean var1) {
      this.ImmortalTutorialZombie = var1;
   }

   public boolean isTargetInCone(float var1, float var2) {
      if (this.target == null) {
         return false;
      } else {
         tempo.set(this.target.getX() - this.getX(), this.target.getY() - this.getY());
         float var3 = tempo.getLength();
         if (var3 == 0.0F) {
            return true;
         } else if (var3 > var1) {
            return false;
         } else {
            tempo.normalize();
            this.getVectorFromDirection(tempo2);
            float var4 = tempo.dot(tempo2);
            return var4 >= var2;
         }
      }
   }

   public boolean testCollideWithVehicles(BaseVehicle var1) {
      if (this.Health <= 0.0F) {
         return false;
      } else if (this.isProne()) {
         if (var1.getDriver() == null) {
            return false;
         } else {
            int var3 = var1.isEngineRunning() ? var1.testCollisionWithProneCharacter(this, true) : 0;
            if (var3 > 0) {
               if (!this.emitter.isPlaying(this.getHurtSound())) {
                  this.playHurtSound();
               }

               this.AttackedBy = var1.getDriver();
               var1.hitCharacter(this);
               if (!GameServer.bServer && !GameClient.bClient && this.isDead()) {
                  this.Kill(var1.getDriver());
               }

               super.update();
               return true;
            } else {
               return false;
            }
         }
      } else {
         if (var1.shouldCollideWithCharacters()) {
            Vector2 var2 = (Vector2)((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).alloc();
            if (var1.testCollisionWithCharacter(this, 0.3F, var2) != null) {
               ((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).release(var2);
               var1.hitCharacter(this);
               super.update();
               return true;
            }

            ((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).release(var2);
         }

         return false;
      }
   }

   public boolean isCrawling() {
      return this.bCrawling;
   }

   public boolean isCanCrawlUnderVehicle() {
      return this.bCanCrawlUnderVehicle;
   }

   public void setCanCrawlUnderVehicle(boolean var1) {
      this.bCanCrawlUnderVehicle = var1;
   }

   public boolean isCanWalk() {
      return this.bCanWalk;
   }

   public void setCanWalk(boolean var1) {
      this.bCanWalk = var1;
   }

   public void initCanCrawlUnderVehicle() {
      byte var1 = 100;
      switch (SandboxOptions.instance.Lore.CrawlUnderVehicle.getValue()) {
         case 1:
            var1 = 0;
            break;
         case 2:
            var1 = 5;
            break;
         case 3:
            var1 = 10;
            break;
         case 4:
            var1 = 25;
            break;
         case 5:
            var1 = 50;
            break;
         case 6:
            var1 = 75;
            break;
         case 7:
            var1 = 100;
      }

      this.setCanCrawlUnderVehicle(Rand.Next(100) < var1);
   }

   public boolean shouldGetUpFromCrawl() {
      if (this.isCurrentState(ZombieGetUpFromCrawlState.instance())) {
         return true;
      } else if (this.isCurrentState(ZombieGetUpState.instance())) {
         return this.stateMachine.getPrevious() == ZombieGetUpFromCrawlState.instance();
      } else if (!this.isCrawling()) {
         return false;
      } else if (!this.isCanWalk()) {
         return false;
      } else if (this.isCurrentState(PathFindState.instance())) {
         return this.stateMachine.getPrevious() == ZombieGetDownState.instance() && ZombieGetDownState.instance().isNearStartXY(this) ? false : this.getPathFindBehavior2().shouldGetUpFromCrawl();
      } else {
         if (this.isCurrentState(WalkTowardState.instance())) {
            float var1 = this.getPathFindBehavior2().getTargetX();
            float var2 = this.getPathFindBehavior2().getTargetY();
            if (this.DistToSquared(var1, var2) > 0.010000001F && PolygonalMap2.instance.lineClearCollide(this.getX(), this.getY(), var1, var2, PZMath.fastfloor(this.getZ()), (IsoMovingObject)null)) {
               return false;
            }
         }

         return this.isCurrentState(ZombieGetDownState.instance()) ? false : PolygonalMap2.instance.canStandAt(this.getX(), this.getY(), (int)this.getZ(), (IsoMovingObject)null, false, true);
      }
   }

   public void toggleCrawling() {
      boolean var1 = this.bCanCrawlUnderVehicle;
      if (this.bCrawling) {
         this.setCrawler(false);
         this.setKnockedDown(false);
         this.setStaggerBack(false);
         this.setFallOnFront(false);
         this.setOnFloor(false);
         this.DoZombieStats();
      } else {
         this.setCrawler(true);
         this.setOnFloor(true);
         this.DoZombieStats();
         this.walkVariant = "ZombieWalk";
      }

      this.bCanCrawlUnderVehicle = var1;
   }

   public void knockDown(boolean var1) {
      this.setKnockedDown(true);
      this.setStaggerBack(true);
      this.setHitReaction("");
      this.setPlayerAttackPosition(var1 ? "BEHIND" : null);
      this.setHitForce(1.0F);
      this.reportEvent("wasHit");
   }

   public void addItemToSpawnAtDeath(InventoryItem var1) {
      if (this.itemsToSpawnAtDeath == null) {
         this.itemsToSpawnAtDeath = new ArrayList();
      }

      this.itemsToSpawnAtDeath.add(var1);
   }

   public void clearItemsToSpawnAtDeath() {
      if (this.itemsToSpawnAtDeath != null) {
         this.itemsToSpawnAtDeath.clear();
      }

   }

   public IsoMovingObject getEatBodyTarget() {
      return this.eatBodyTarget;
   }

   public float getEatSpeed() {
      return this.eatSpeed;
   }

   public void setEatBodyTarget(IsoMovingObject var1, boolean var2) {
      this.setEatBodyTarget(var1, var2, Rand.Next(0.8F, 1.2F) * GameTime.getAnimSpeedFix());
   }

   public void setEatBodyTarget(IsoMovingObject var1, boolean var2, float var3) {
      if (var1 != this.eatBodyTarget) {
         if (var2 || var1 == null || var1.getEatingZombies().size() < 3) {
            if (this.eatBodyTarget != null) {
               this.eatBodyTarget.getEatingZombies().remove(this);
            }

            this.eatBodyTarget = var1;
            if (var1 != null) {
               this.eatBodyTarget.getEatingZombies().add(this);
               this.eatSpeed = var3;
            }
         }
      }
   }

   private void updateEatBodyTarget() {
      if (this.bodyToEat != null && this.isCurrentState(ZombieIdleState.instance()) && this.DistToSquared(this.bodyToEat) <= 1.0F && PZMath.fastfloor(this.getZ()) == PZMath.fastfloor(this.bodyToEat.getZ())) {
         this.setEatBodyTarget(this.bodyToEat, false);
         this.bodyToEat = null;
      }

      if (this.eatBodyTarget != null) {
         if (this.eatBodyTarget instanceof IsoDeadBody && this.eatBodyTarget.getStaticMovingObjectIndex() == -1) {
            this.setEatBodyTarget((IsoMovingObject)null, false);
         }

         if (this.target != null && !this.target.isOnFloor() && this.target != this.eatBodyTarget) {
            this.setEatBodyTarget((IsoMovingObject)null, false);
         }

         IsoPlayer var1 = (IsoPlayer)Type.tryCastTo(this.eatBodyTarget, IsoPlayer.class);
         if (var1 != null && var1.ReanimatedCorpse != null) {
            this.setEatBodyTarget((IsoMovingObject)null, false);
         }

         if (var1 != null && var1.isAlive() && !var1.isOnFloor() && !var1.isCurrentState(PlayerHitReactionState.instance())) {
            this.setEatBodyTarget((IsoMovingObject)null, false);
         }

         if (!this.isCurrentState(ZombieEatBodyState.instance()) && this.eatBodyTarget != null && this.DistToSquared(this.eatBodyTarget) > 1.0F) {
            this.setEatBodyTarget((IsoMovingObject)null, false);
         }

         if (this.eatBodyTarget != null && this.eatBodyTarget.getSquare() != null && this.current != null && this.current.isSomethingTo(this.eatBodyTarget.getSquare())) {
            this.setEatBodyTarget((IsoMovingObject)null, false);
         }

      }
   }

   private void updateCharacterTextureAnimTime() {
      boolean var1 = this.isCurrentState(WalkTowardState.instance()) || this.isCurrentState(PathFindState.instance());
      this.m_characterTextureAnimDuration = var1 ? 0.67F : 2.0F;
      this.m_characterTextureAnimTime += GameTime.getInstance().getTimeDelta();
      if (this.m_characterTextureAnimTime > this.m_characterTextureAnimDuration) {
         this.m_characterTextureAnimTime %= this.m_characterTextureAnimDuration;
      }

   }

   public Vector2 getHitAngle() {
      return this.hitAngle;
   }

   public void setHitAngle(Vector2 var1) {
      if (var1 != null) {
         this.hitAngle.set(var1);
      }
   }

   public int getCrawlerType() {
      return this.crawlerType;
   }

   public void setCrawlerType(int var1) {
      this.crawlerType = var1;
   }

   public void addRandomVisualBandages() {
      if (!"Tutorial".equals(Core.getInstance().getGameMode())) {
         for(int var1 = 0; var1 < 5; ++var1) {
            if (OutfitRNG.Next(10) == 0) {
               BodyPartType var2 = BodyPartType.getRandom();
               String var3 = var2.getBandageModel() + "_Blood";
               this.addBodyVisualFromItemType(var3);
            }
         }

      }
   }

   public void addVisualBandage(BodyPartType var1, boolean var2) {
      String var10000 = var1.getBandageModel();
      String var3 = var10000 + (var2 ? "_Blood" : "");
      this.addBodyVisualFromItemType(var3);
   }

   public void addRandomVisualDamages() {
      for(int var1 = 0; var1 < 5; ++var1) {
         if (OutfitRNG.Next(5) == 0) {
            String var2 = (String)OutfitRNG.pickRandom(ScriptManager.instance.getZedDmgMap());
            this.addBodyVisualFromItemType("Base." + var2);
         }
      }

   }

   public String getPlayerAttackPosition() {
      return this.playerAttackPosition;
   }

   public void setPlayerAttackPosition(String var1) {
      this.playerAttackPosition = var1;
   }

   public boolean isSitAgainstWall() {
      return this.sitAgainstWall;
   }

   public void setSitAgainstWall(boolean var1) {
      this.sitAgainstWall = var1;
      this.networkAI.extraUpdate();
   }

   public boolean isSkeleton() {
      if (Core.bDebug && DebugOptions.instance.Model.ForceSkeleton.getValue()) {
         this.getHumanVisual().setSkinTextureIndex(2);
         return true;
      } else {
         return this.isSkeleton;
      }
   }

   public boolean isZombie() {
      return true;
   }

   public void setSkeleton(boolean var1) {
      this.isSkeleton = var1;
      if (var1) {
         this.getHumanVisual().setHairModel("");
         this.getHumanVisual().setBeardModel("");
         ModelManager.instance.Reset(this);
      }

   }

   public int getHitTime() {
      return this.hitTime;
   }

   public void setHitTime(int var1) {
      this.hitTime = var1;
   }

   public int getThumpTimer() {
      return this.thumpTimer;
   }

   public void setThumpTimer(int var1) {
      this.thumpTimer = var1;
   }

   public IsoMovingObject getTarget() {
      return this.target;
   }

   public void setTargetSeenTime(float var1) {
      this.targetSeenTime = var1;
   }

   public float getTargetSeenTime() {
      return this.targetSeenTime;
   }

   public boolean isTargetVisible() {
      IsoPlayer var1 = (IsoPlayer)Type.tryCastTo(this.target, IsoPlayer.class);
      if (var1 != null && this.getCurrentSquare() != null) {
         return GameServer.bServer ? ServerLOS.instance.isCouldSee(var1, this.getCurrentSquare()) : this.getCurrentSquare().isCouldSee(var1.getPlayerNum());
      } else {
         return false;
      }
   }

   public float getTurnDelta() {
      return this.m_turnDeltaNormal;
   }

   public boolean isAttacking() {
      return this.isZombieAttacking();
   }

   public boolean isZombieAttacking() {
      State var1 = this.getCurrentState();
      return var1 != null && var1.isAttacking(this);
   }

   public boolean isZombieAttacking(IsoMovingObject var1) {
      if (GameClient.bClient && this.authOwner == null) {
         return this.legsSprite != null && this.legsSprite.CurrentAnim != null && "ZombieBite".equals(this.legsSprite.CurrentAnim.name);
      } else {
         return var1 == this.target && this.isCurrentState(AttackState.instance());
      }
   }

   public int getHitHeadWhileOnFloor() {
      return this.hitHeadWhileOnFloor;
   }

   public String getRealState() {
      return this.realState.toString();
   }

   public void setHitHeadWhileOnFloor(int var1) {
      this.hitHeadWhileOnFloor = var1;
      this.networkAI.extraUpdate();
   }

   public boolean isHitLegsWhileOnFloor() {
      return this.hitLegsWhileOnFloor;
   }

   public void setHitLegsWhileOnFloor(boolean var1) {
      this.hitLegsWhileOnFloor = var1;
   }

   public void makeInactive(boolean var1) {
      if (var1 != this.inactive) {
         if (var1) {
            this.walkType = Integer.toString(Rand.Next(3) + 1);
            this.walkType = "slow" + this.walkType;
            this.bRunning = false;
            this.inactive = true;
            this.speedType = 3;
         } else {
            this.speedType = -1;
            this.inactive = false;
            this.DoZombieStats();
         }

      }
   }

   public float getFootstepVolume() {
      return this.footstepVolume;
   }

   public boolean isFacingTarget() {
      if (this.target == null) {
         return false;
      } else if (GameClient.bClient && !this.isLocal() && this.isBumped()) {
         return false;
      } else {
         tempo.set(this.target.getX() - this.getX(), this.target.getY() - this.getY()).normalize();
         if (tempo.getLength() == 0.0F) {
            return true;
         } else {
            this.getLookVector(tempo2);
            float var1 = Vector2.dot(tempo.x, tempo.y, tempo2.x, tempo2.y);
            return (double)var1 >= 0.8;
         }
      }
   }

   public boolean isTargetLocationKnown() {
      if (this.target == null) {
         return false;
      } else if (this.BonusSpotTime > 0.0F) {
         return true;
      } else {
         return this.TimeSinceSeenFlesh < 1.0F;
      }
   }

   protected int getSandboxMemoryDuration() {
      int var1 = SandboxOptions.instance.Lore.Memory.getValue();
      int var2 = 160;
      if (this.inactive) {
         var2 = 5;
      } else if (var1 == 1) {
         var2 = 250;
      } else if (var1 == 3) {
         var2 = 100;
      } else if (var1 == 4) {
         var2 = 5;
      }

      var2 *= 5;
      return var2;
   }

   public boolean shouldDoFenceLunge() {
      if (!SandboxOptions.instance.Lore.ZombiesFenceLunge.getValue()) {
         return false;
      } else if (Rand.NextBool(3)) {
         return false;
      } else {
         IsoGameCharacter var1 = (IsoGameCharacter)Type.tryCastTo(this.target, IsoGameCharacter.class);
         if (var1 != null && PZMath.fastfloor(var1.getZ()) == PZMath.fastfloor(this.getZ())) {
            if (var1.getVehicle() != null) {
               return false;
            } else {
               return (double)this.DistTo(var1) < 3.9;
            }
         } else {
            return false;
         }
      }
   }

   public boolean isProne() {
      if (!this.isOnFloor()) {
         return false;
      } else if (this.bCrawling) {
         return true;
      } else {
         return this.isCurrentState(ZombieOnGroundState.instance()) || this.isCurrentState(FakeDeadZombieState.instance());
      }
   }

   public void setTarget(IsoMovingObject var1) {
      if (this.target != var1) {
         this.target = var1;
         this.networkAI.extraUpdate();
      }
   }

   public boolean isAlwaysKnockedDown() {
      return this.alwaysKnockedDown;
   }

   public void setAlwaysKnockedDown(boolean var1) {
      this.alwaysKnockedDown = var1;
   }

   public void setDressInRandomOutfit(boolean var1) {
      this.bDressInRandomOutfit = var1;
   }

   public void setBodyToEat(IsoDeadBody var1) {
      this.bodyToEat = var1;
   }

   public boolean isForceEatingAnimation() {
      return this.forceEatingAnimation;
   }

   public void setForceEatingAnimation(boolean var1) {
      this.forceEatingAnimation = var1;
   }

   public boolean isOnlyJawStab() {
      return this.onlyJawStab;
   }

   public void setOnlyJawStab(boolean var1) {
      this.onlyJawStab = var1;
   }

   public boolean isNoTeeth() {
      return this.noTeeth;
   }

   public boolean cantBite() {
      if (this.getWornItem("Mask") != null && !this.getWornItem("Mask").hasTag("CanEat")) {
         return true;
      } else if (this.getWornItem("MaskEyes") != null && !this.getWornItem("MaskEyes").hasTag("CanEat")) {
         return true;
      } else if (this.getWornItem("MaskFull") != null && !this.getWornItem("MaskFull").hasTag("CanEat")) {
         return true;
      } else if (this.getWornItem("FullHat") != null && !this.getWornItem("FullHat").hasTag("CanEat")) {
         return true;
      } else if (this.getWornItem("FullSuitHead") != null && !this.getWornItem("FullSuitHead").hasTag("CanEat")) {
         return true;
      } else {
         ItemVisuals var1 = new ItemVisuals();
         this.getItemVisuals(var1);

         for(int var2 = 0; var2 < var1.size(); ++var2) {
            ItemVisual var3 = (ItemVisual)var1.get(var2);
            Item var4 = var3.getScriptItem();
            if (var4 != null && var4.getType() == Item.Type.Clothing) {
               if (Objects.equals(var4.getBodyLocation(), "Mask") && !var4.hasTag("CanEat")) {
                  return true;
               }

               if (Objects.equals(var4.getBodyLocation(), "MaskEyes") && !var4.hasTag("CanEat")) {
                  return true;
               }

               if (Objects.equals(var4.getBodyLocation(), "MaskFull") && !var4.hasTag("CanEat")) {
                  return true;
               }

               if (Objects.equals(var4.getBodyLocation(), "FullHat") && !var4.hasTag("CanEat")) {
                  return true;
               }

               if (Objects.equals(var4.getBodyLocation(), "FullSuitHead") && !var4.hasTag("CanEat")) {
                  return true;
               }
            }
         }

         return this.isNoTeeth();
      }
   }

   public void setNoTeeth(boolean var1) {
      this.noTeeth = var1;
   }

   public void setThumpFlag(int var1) {
      if (this.thumpFlag != var1) {
         this.thumpFlag = var1;
         this.networkAI.extraUpdate();
      }
   }

   public void setThumpCondition(float var1) {
      this.thumpCondition = PZMath.clamp_01(var1);
   }

   public void setThumpCondition(int var1, int var2) {
      if (var2 <= 0) {
         this.thumpCondition = 0.0F;
      } else {
         var1 = PZMath.clamp(var1, 0, var2);
         this.thumpCondition = (float)var1 / (float)var2;
      }
   }

   public float getThumpCondition() {
      return this.thumpCondition;
   }

   public boolean isStaggerBack() {
      return this.bStaggerBack;
   }

   public void setStaggerBack(boolean var1) {
      this.bStaggerBack = var1;
   }

   public boolean isKnifeDeath() {
      return this.bKnifeDeath;
   }

   public void setKnifeDeath(boolean var1) {
      this.bKnifeDeath = var1;
   }

   public boolean isJawStabAttach() {
      return this.bJawStabAttach;
   }

   public void setJawStabAttach(boolean var1) {
      this.bJawStabAttach = var1;
   }

   public void writeInventory(ByteBuffer var1) {
      String var2 = this.isFemale() ? "inventoryfemale" : "inventorymale";
      GameWindow.WriteString(var1, var2);
      if (this.getInventory() != null) {
         var1.put((byte)1);

         try {
            int var3 = -1;
            Iterator var4 = this.getInventory().getItems().iterator();

            while(var4.hasNext()) {
               InventoryItem var5 = (InventoryItem)var4.next();
               if (PersistentOutfits.instance.isHatFallen(this.getPersistentOutfitID()) && var5.getScriptItem() != null && var5.getScriptItem().getChanceToFall() > 0) {
                  var3 = var5.id;
               }
            }

            if (var3 != -1) {
               this.getInventory().removeItemWithID(var3);
            }

            ArrayList var12 = this.getInventory().save(var1);
            WornItems var6 = this.getWornItems();
            int var7;
            if (var6 == null) {
               byte var14 = 0;
               var1.put((byte)var14);
            } else {
               int var13 = var6.size();
               if (var13 > 127) {
                  DebugLog.Multiplayer.warn("Too many worn items");
                  var13 = 127;
               }

               var1.put((byte)var13);

               for(var7 = 0; var7 < var13; ++var7) {
                  WornItem var8 = var6.get(var7);
                  if (PersistentOutfits.instance.isHatFallen(this.getPersistentOutfitID()) && var8.getItem().getScriptItem() != null && var8.getItem().getScriptItem().getChanceToFall() > 0) {
                     GameWindow.WriteStringUTF(var1, "");
                     var1.putShort((short)-1);
                  } else {
                     GameWindow.WriteStringUTF(var1, var8.getLocation());
                     var1.putShort((short)var12.indexOf(var8.getItem()));
                  }
               }
            }

            AttachedItems var16 = this.getAttachedItems();
            if (var16 == null) {
               byte var15 = 0;
               var1.put((byte)var15);
            } else {
               var7 = var16.size();
               if (var7 > 127) {
                  DebugLog.Multiplayer.warn("Too many attached items");
                  var7 = 127;
               }

               var1.put((byte)var7);

               for(int var9 = 0; var9 < var7; ++var9) {
                  AttachedItem var10 = var16.get(var9);
                  GameWindow.WriteStringUTF(var1, var10.getLocation());
                  var1.putShort((short)var12.indexOf(var10.getItem()));
               }
            }
         } catch (IOException var11) {
            DebugLog.Multiplayer.printException(var11, "WriteInventory error for zombie " + this.getOnlineID(), LogSeverity.Error);
         }
      } else {
         var1.put((byte)0);
      }

   }

   public void Kill(IsoGameCharacter var1, boolean var2) {
      if (!this.isOnKillDone()) {
         if (GameServer.bServer) {
            MPStatistics.onZombieWasKilled(this.isOnFire());
         }

         super.Kill(var1);
         if (this.shouldDoInventory()) {
            this.DoZombieInventory();
         }

         LuaEventManager.triggerEvent("OnZombieDead", this);
         if (var1 == null) {
            this.DoDeath((HandWeapon)null, (IsoGameCharacter)null, var2);
         } else if (var1.getPrimaryHandItem() instanceof HandWeapon) {
            this.DoDeath((HandWeapon)var1.getPrimaryHandItem(), var1, var2);
         } else {
            this.DoDeath(this.getUseHandWeapon(), var1, var2);
         }

      }
   }

   public void Kill(IsoGameCharacter var1) {
      this.Kill(var1, true);
   }

   public boolean shouldDoInventory() {
      return !GameClient.bClient || this.getAttackedBy() instanceof IsoPlayer && ((IsoPlayer)this.getAttackedBy()).isLocalPlayer() || this.getAttackedBy() == IsoWorld.instance.CurrentCell.getFakeZombieForHit() && (this.wasLocal() || this.isLocal());
   }

   public void becomeCorpse() {
      if (!this.isOnDeathDone()) {
         if (this.shouldBecomeCorpse()) {
            super.becomeCorpse();
            if (GameClient.bClient && this.shouldDoInventory()) {
               GameClient.sendZombieDeath(this);
            }

            if (!GameClient.bClient) {
               IsoDeadBody var1 = new IsoDeadBody(this);
               if (this.isFakeDead()) {
                  var1.setCrawling(true);
               }

               if (GameServer.bServer) {
                  GameServer.sendBecomeCorpse(var1);
               }
            }

         }
      }
   }

   public HitReactionNetworkAI getHitReactionNetworkAI() {
      return this.networkAI.hitReaction;
   }

   public NetworkCharacterAI getNetworkCharacterAI() {
      return this.networkAI;
   }

   public boolean isLocal() {
      return super.isLocal() || !this.isRemoteZombie();
   }

   public boolean isVehicleCollisionActive(BaseVehicle var1) {
      if (!super.isVehicleCollisionActive(var1)) {
         return false;
      } else {
         return !this.isCurrentState(ZombieFallDownState.instance()) && !this.isCurrentState(ZombieFallingState.instance());
      }
   }

   public boolean isSkipResolveCollision() {
      return super.isSkipResolveCollision() || this.isCurrentState(ZombieHitReactionState.instance()) || this.isCurrentState(ZombieFallDownState.instance()) || this.isCurrentState(ZombieOnGroundState.instance()) || this.isCurrentState(StaggerBackState.instance());
   }

   public void applyDamageFromVehicle(float var1, float var2) {
      this.addBlood(var1);
      CombatManager.getInstance().applyDamage((IsoGameCharacter)this, var2);
   }

   public float Hit(BaseVehicle var1, float var2, boolean var3, float var4, float var5) {
      float var6 = this.Hit(var1, var2, var3, this.getHitDir().set(var4, var5));
      this.applyDamageFromVehicle(var2, var6);
      super.Hit(var1, var2, var3, var4, var5);
      return var6;
   }

   public Float calcHitDir(IsoGameCharacter var1, HandWeapon var2, Vector2 var3) {
      Float var4;
      if (this.getAnimationPlayer() != null) {
         var4 = this.getAnimAngleRadians();
      }

      var4 = super.calcHitDir(var1, var2, var3);
      return var4;
   }

   private void updateVisionRadius() {
      ClimateManager var1 = ClimateManager.getInstance();
      float var2 = var1.getRainIntensity() * 2.5F;
      float var3 = var1.getFogIntensity() * 7.0F;
      float var4 = 1.0F - Math.min(var1.getDayLightStrength(), var1.getAmbient());
      float var5 = var4 * 5.0F;
      if (this.target != null && this.target instanceof IsoPlayer && this.target.getCurrentSquare() != null) {
         var5 = (1.0F - this.target.getCurrentSquare().getLightLevel(((IsoPlayer)this.target).getPlayerNum())) * 5.0F;
      }

      float var6 = 20.0F - Math.max(var5, var2 + var3);
      if (this.sight == 1 || SandboxOptions.instance.Lore.Sight.getValue() == 1) {
         var6 *= 1.75F;
      }

      if (this.sight == 3 || SandboxOptions.instance.Lore.Sight.getValue() == 3) {
         var6 *= 0.35F;
      }

      var6 /= this.getWornItemsVisionModifier();
      if (this.getEatBodyTarget() != null) {
         var6 = (float)((double)var6 * 0.5);
      }

      this.VISION_RADIUS_RESULT = PZMath.clamp(var6, 10.0F, 20.0F);
   }

   public boolean shouldZombieHaveKey(boolean var1) {
      String var2 = this.getOutfitName();
      boolean var3 = true;
      if (this.isSkeleton() && this.getHumanVisual() != null) {
         return false;
      } else if (this.getOutfitName() == null) {
         return var3;
      } else {
         if (var2.contains("Inmate")) {
            var3 = false;
         } else if (var2.contains("Backpacker")) {
            var3 = false;
         } else if (var2.contains("Evacuee") && !var1) {
            var3 = false;
         } else if (var2.contains("Stripper")) {
            var3 = false;
         } else if (var2.contains("Naked")) {
            var3 = false;
         } else if (var2.equals("Bandit") && !var1) {
            var3 = false;
         } else if (var2.equals("Bathrobe")) {
            var3 = false;
         } else if (var2.equals("Bedroom")) {
            var3 = false;
         } else if (var2.equals("Hobbo")) {
            var3 = false;
         } else if (var2.equals("HockeyPyscho")) {
            var3 = false;
         } else if (var2.equals("HospitalPatient")) {
            var3 = false;
         } else if (var2.equals("Swimmer")) {
            var3 = false;
         }

         return var3;
      }
   }

   private void checkZombieEntersPlayerBuilding() {
      IsoPlayer var1 = (IsoPlayer)Type.tryCastTo(this.target, IsoPlayer.class);
      if (var1 != null && var1.isLocalPlayer()) {
         if (this.getCurrentSquare() != null && this.getCurrentSquare().isCanSee(var1.PlayerIndex)) {
            IsoBuilding var2 = this.getCurrentBuilding();
            if (var2 != null) {
               IsoBuilding var3 = var1.getCurrentBuilding();
               if (var2 == var3) {
                  Object var4 = this.getMusicIntensityEventModData("ZombieEntersPlayerBuilding");
                  if (var4 == null) {
                     this.setMusicIntensityEventModData("ZombieEntersPlayerBuilding", GameTime.getInstance().getWorldAgeHours());
                     var1.triggerMusicIntensityEvent("ZombieEntersPlayerBuilding");
                  }

               }
            }
         }
      }
   }

   public void doZombieSpeed() {
      this.doZombieSpeedInternal(this.speedType);
   }

   public void doZombieSpeed(int var1) {
      this.doZombieSpeedInternal(var1);
   }

   private void doZombieSpeedInternal(int var1) {
      var1 = this.determineZombieSpeed(var1);
      if (this.bCrawling) {
         this.doCrawlerSpeed(var1);
      } else if (SandboxOptions.instance.Lore.Speed.getValue() != 3 && var1 != 3) {
         if (Rand.Next(3) != 0) {
            this.doFakeShambler(var1);
         } else if (SandboxOptions.instance.Lore.Speed.getValue() != 2 && var1 != 2) {
            if (SandboxOptions.instance.Lore.Speed.getValue() == 1 || var1 == 1) {
               this.doSprinter();
            }
         } else {
            this.doFastShambler();
         }
      } else {
         this.doShambler();
      }

   }

   private void doCrawlerSpeed(int var1) {
      this.speedMod = 0.3F;
      this.speedMod += (float)Rand.Next(1500) / 10000.0F;
      IsoSpriteInstance var10000 = this.def;
      var10000.AnimFrameIncrease *= 0.8F;
      this.doZombieSpeedInternal2(var1);
   }

   private void doSprinter() {
      this.bLunger = true;
      this.speedMod = 0.85F;
      this.walkVariant = "ZombieWalk2";
      this.speedMod += (float)Rand.Next(1500) / 10000.0F;
      this.def.setFrameSpeedPerFrame(0.24F);
      IsoSpriteInstance var10000 = this.def;
      var10000.AnimFrameIncrease *= this.speedMod;
      this.speedType = 1;
      this.doZombieSpeedInternal2();
   }

   private void doFastShambler() {
      this.bLunger = true;
      this.speedMod = 0.85F;
      this.walkVariant = "ZombieWalk2";
      this.speedMod += (float)Rand.Next(1500) / 10000.0F;
      this.def.setFrameSpeedPerFrame(0.24F);
      IsoSpriteInstance var10000 = this.def;
      var10000.AnimFrameIncrease *= this.speedMod;
      this.speedType = 2;
      this.doZombieSpeedInternal2();
   }

   private void doFakeShambler() {
      this.speedMod = 0.55F;
      this.speedMod += (float)Rand.Next(1500) / 10000.0F;
      this.walkVariant = "ZombieWalk1";
      this.def.setFrameSpeedPerFrame(0.24F);
      IsoSpriteInstance var10000 = this.def;
      var10000.AnimFrameIncrease *= this.speedMod;
      this.doZombieSpeedInternal2();
   }

   private void doFakeShambler(int var1) {
      this.speedMod = 0.55F;
      this.speedMod += (float)Rand.Next(1500) / 10000.0F;
      this.walkVariant = "ZombieWalk1";
      this.def.setFrameSpeedPerFrame(0.24F);
      IsoSpriteInstance var10000 = this.def;
      var10000.AnimFrameIncrease *= this.speedMod;
      this.doZombieSpeedInternal2(var1);
   }

   private void doShambler() {
      this.speedMod = 0.55F;
      this.speedMod += (float)Rand.Next(1500) / 10000.0F;
      this.walkVariant = "ZombieWalk1";
      this.def.setFrameSpeedPerFrame(0.24F);
      IsoSpriteInstance var10000 = this.def;
      var10000.AnimFrameIncrease *= this.speedMod;
      this.speedType = 3;
      this.doZombieSpeedInternal2();
   }

   private void doZombieSpeedInternal2() {
      this.doZombieSpeedInternal2(this.speedType);
   }

   private void doZombieSpeedInternal2(int var1) {
      if (var1 == 3) {
         this.walkType = Integer.toString(Rand.Next(3) + 1);
         this.walkType = "slow" + this.walkType;
      } else {
         this.walkType = Integer.toString(Rand.Next(5) + 1);
      }

      if (var1 == 1) {
         this.setTurnDelta(1.0F);
         this.walkType = "sprint" + this.walkType;
      }

      this.speedType = var1;
   }

   private int determineZombieSpeed(int var1) {
      if (var1 != -1) {
         return var1;
      } else {
         var1 = SandboxOptions.instance.Lore.Speed.getValue();
         if (this.inactive) {
            var1 = 3;
         } else if (var1 == 4) {
            if (Rand.Next(100) < SandboxOptions.instance.Lore.SprinterPercentage.getValue()) {
               var1 = 1;
            } else {
               var1 = Rand.Next(2) + 2;
            }
         }

         return var1;
      }
   }

   public String getLastHitPart() {
      return this.lastHitPart == null ? null : this.lastHitPart.toString();
   }

   public boolean shouldDressInRandomOutfit() {
      return this.bDressInRandomOutfit;
   }

   public String getOutfitName() {
      if (this.shouldDressInRandomOutfit()) {
         ModelManager.instance.dressInRandomOutfit(this);
      }

      if (this.getHumanVisual() != null) {
         HumanVisual var1 = this.getHumanVisual();
         Outfit var2 = var1.getOutfit();
         return var2 == null ? null : var2.m_Name;
      } else {
         return null;
      }
   }

   private static class Aggro {
      IsoMovingObject obj;
      float damage;
      long lastDamage;

      public Aggro(IsoMovingObject var1, float var2) {
         this.obj = var1;
         this.damage = var2;
         this.lastDamage = System.currentTimeMillis();
      }

      public void addDamage(float var1) {
         this.damage += var1;
         this.lastDamage = System.currentTimeMillis();
      }

      public float getAggro() {
         float var1 = (float)(System.currentTimeMillis() - this.lastDamage);
         float var2 = Math.min(1.0F, Math.max(0.0F, (10000.0F - var1) / 5000.0F));
         var2 = Math.min(1.0F, Math.max(0.0F, var2 * this.damage * 0.5F));
         return var2;
      }
   }

   private static class s_performance {
      static final PerformanceProfileProbe update = new PerformanceProfileProbe("IsoZombie.update");
      static final PerformanceProfileProbe postUpdate = new PerformanceProfileProbe("IsoZombie.postUpdate");

      private s_performance() {
      }
   }

   private static final class FloodFill {
      private IsoGridSquare start = null;
      private final int FLOOD_SIZE = 11;
      private final BooleanGrid visited = new BooleanGrid(11, 11);
      private final Stack<IsoGridSquare> stack = new Stack();
      private IsoBuilding building = null;
      private Mover mover = null;
      private final ArrayList<IsoGridSquare> choices = new ArrayList(121);

      private FloodFill() {
      }

      void calculate(Mover var1, IsoGridSquare var2) {
         this.start = var2;
         this.mover = var1;
         if (this.start.getRoom() != null) {
            this.building = this.start.getRoom().getBuilding();
         }

         boolean var3 = false;
         boolean var4 = false;
         this.push(this.start.getX(), this.start.getY());

         while((var2 = this.pop()) != null) {
            int var6 = var2.getX();

            int var5;
            for(var5 = var2.getY(); this.shouldVisit(var6, var5, var6, var5 - 1); --var5) {
            }

            var4 = false;
            var3 = false;

            while(true) {
               this.visited.setValue(this.gridX(var6), this.gridY(var5), true);
               IsoGridSquare var7 = IsoWorld.instance.CurrentCell.getGridSquare(var6, var5, this.start.getZ());
               if (var7 != null) {
                  this.choices.add(var7);
               }

               if (!var3 && this.shouldVisit(var6, var5, var6 - 1, var5)) {
                  this.push(var6 - 1, var5);
                  var3 = true;
               } else if (var3 && !this.shouldVisit(var6, var5, var6 - 1, var5)) {
                  var3 = false;
               } else if (var3 && !this.shouldVisit(var6 - 1, var5, var6 - 1, var5 - 1)) {
                  this.push(var6 - 1, var5);
               }

               if (!var4 && this.shouldVisit(var6, var5, var6 + 1, var5)) {
                  this.push(var6 + 1, var5);
                  var4 = true;
               } else if (var4 && !this.shouldVisit(var6, var5, var6 + 1, var5)) {
                  var4 = false;
               } else if (var4 && !this.shouldVisit(var6 + 1, var5, var6 + 1, var5 - 1)) {
                  this.push(var6 + 1, var5);
               }

               ++var5;
               if (!this.shouldVisit(var6, var5 - 1, var6, var5)) {
                  break;
               }
            }
         }

      }

      boolean shouldVisit(int var1, int var2, int var3, int var4) {
         if (this.gridX(var3) < 11 && this.gridX(var3) >= 0) {
            if (this.gridY(var4) < 11 && this.gridY(var4) >= 0) {
               if (this.visited.getValue(this.gridX(var3), this.gridY(var4))) {
                  return false;
               } else {
                  IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare(var3, var4, this.start.getZ());
                  if (var5 == null) {
                     return false;
                  } else if (!var5.Has(IsoObjectType.stairsBN) && !var5.Has(IsoObjectType.stairsMN) && !var5.Has(IsoObjectType.stairsTN)) {
                     if (!var5.Has(IsoObjectType.stairsBW) && !var5.Has(IsoObjectType.stairsMW) && !var5.Has(IsoObjectType.stairsTW)) {
                        if (var5.getRoom() != null && this.building == null) {
                           return false;
                        } else if (var5.getRoom() == null && this.building != null) {
                           return false;
                        } else {
                           return !IsoWorld.instance.CurrentCell.blocked(this.mover, var3, var4, this.start.getZ(), var1, var2, this.start.getZ());
                        }
                     } else {
                        return false;
                     }
                  } else {
                     return false;
                  }
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      void push(int var1, int var2) {
         IsoGridSquare var3 = IsoWorld.instance.CurrentCell.getGridSquare(var1, var2, this.start.getZ());
         this.stack.push(var3);
      }

      IsoGridSquare pop() {
         return this.stack.isEmpty() ? null : (IsoGridSquare)this.stack.pop();
      }

      int gridX(int var1) {
         return var1 - (this.start.getX() - 5);
      }

      int gridY(int var1) {
         return var1 - (this.start.getY() - 5);
      }

      int gridX(IsoGridSquare var1) {
         return var1.getX() - (this.start.getX() - 5);
      }

      int gridY(IsoGridSquare var1) {
         return var1.getY() - (this.start.getY() - 5);
      }

      IsoGridSquare choose() {
         if (this.choices.isEmpty()) {
            return null;
         } else {
            int var1 = Rand.Next(this.choices.size());
            return (IsoGridSquare)this.choices.get(var1);
         }
      }

      void reset() {
         this.building = null;
         this.choices.clear();
         this.stack.clear();
         this.visited.clear();
      }
   }

   public static enum ZombieSound {
      Burned(10),
      DeadCloseKilled(10),
      DeadNotCloseKilled(10),
      Hurt(10),
      Idle(15),
      Lunge(40),
      MAX(-1);

      private int radius;
      private static final ZombieSound[] values = values();

      private ZombieSound(int var3) {
         this.radius = var3;
      }

      public int radius() {
         return this.radius;
      }

      public static ZombieSound fromIndex(int var0) {
         return var0 >= 0 && var0 < values.length ? values[var0] : MAX;
      }
   }
}
