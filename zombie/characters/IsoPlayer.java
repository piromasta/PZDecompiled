package zombie.characters;

import fmod.fmod.BaseSoundListener;
import fmod.fmod.DummySoundListener;
import fmod.fmod.FMODSoundEmitter;
import fmod.fmod.SoundListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import org.joml.Vector3f;
import se.krka.kahlua.vm.KahluaTable;
import zombie.CombatManager;
import zombie.DebugFileWatcher;
import zombie.GameSounds;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.PredicatedFileWatcher;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.SystemDisabler;
import zombie.ZomboidFileSystem;
import zombie.ZomboidGlobals;
import zombie.Lua.LuaEventManager;
import zombie.ai.State;
import zombie.ai.sadisticAIDirector.SleepingEvent;
import zombie.ai.states.BumpedState;
import zombie.ai.states.ClimbDownSheetRopeState;
import zombie.ai.states.ClimbOverFenceState;
import zombie.ai.states.ClimbOverWallState;
import zombie.ai.states.ClimbSheetRopeState;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.ai.states.CloseWindowState;
import zombie.ai.states.CollideWithWallState;
import zombie.ai.states.FakeDeadZombieState;
import zombie.ai.states.FishingState;
import zombie.ai.states.FitnessState;
import zombie.ai.states.ForecastBeatenPlayerState;
import zombie.ai.states.IdleState;
import zombie.ai.states.LungeState;
import zombie.ai.states.OpenWindowState;
import zombie.ai.states.PathFindState;
import zombie.ai.states.PlayerActionsState;
import zombie.ai.states.PlayerAimState;
import zombie.ai.states.PlayerEmoteState;
import zombie.ai.states.PlayerExtState;
import zombie.ai.states.PlayerFallDownState;
import zombie.ai.states.PlayerFallingState;
import zombie.ai.states.PlayerGetUpState;
import zombie.ai.states.PlayerHitReactionPVPState;
import zombie.ai.states.PlayerHitReactionState;
import zombie.ai.states.PlayerKnockedDown;
import zombie.ai.states.PlayerOnBedState;
import zombie.ai.states.PlayerOnGroundState;
import zombie.ai.states.PlayerSitOnFurnitureState;
import zombie.ai.states.PlayerSitOnGroundState;
import zombie.ai.states.PlayerStrafeState;
import zombie.ai.states.SmashWindowState;
import zombie.ai.states.StaggerBackState;
import zombie.ai.states.SwipeStatePlayer;
import zombie.ai.states.WalkTowardState;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.DummySoundEmitter;
import zombie.audio.FMODParameterList;
import zombie.audio.GameSound;
import zombie.audio.MusicIntensityConfig;
import zombie.audio.MusicIntensityEvents;
import zombie.audio.MusicThreatStatuses;
import zombie.audio.parameters.ParameterCharacterMovementSpeed;
import zombie.audio.parameters.ParameterCharacterOnFire;
import zombie.audio.parameters.ParameterCharacterVoicePitch;
import zombie.audio.parameters.ParameterCharacterVoiceType;
import zombie.audio.parameters.ParameterDragMaterial;
import zombie.audio.parameters.ParameterEquippedBaggageContainer;
import zombie.audio.parameters.ParameterExercising;
import zombie.audio.parameters.ParameterFirearmInside;
import zombie.audio.parameters.ParameterFirearmRoomSize;
import zombie.audio.parameters.ParameterFootstepMaterial;
import zombie.audio.parameters.ParameterFootstepMaterial2;
import zombie.audio.parameters.ParameterIsStashTile;
import zombie.audio.parameters.ParameterLocalPlayer;
import zombie.audio.parameters.ParameterMeleeHitSurface;
import zombie.audio.parameters.ParameterMoodles;
import zombie.audio.parameters.ParameterPlayerHealth;
import zombie.audio.parameters.ParameterRoomTypeEx;
import zombie.audio.parameters.ParameterShoeType;
import zombie.audio.parameters.ParameterVehicleHitLocation;
import zombie.characters.AttachedItems.AttachedItems;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.BodyDamage.Fitness;
import zombie.characters.BodyDamage.Nutrition;
import zombie.characters.CharacterTimedActions.BaseAction;
import zombie.characters.Moodles.MoodleType;
import zombie.characters.Moodles.Moodles;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionGroup;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.core.BoxedStaticValues;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.physics.PhysicsDebugRenderer;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.visual.AnimalVisual;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IAnimalVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.entity.ComponentType;
import zombie.gameStates.MainScreenState;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.input.Mouse;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponType;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoPhysicsObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SliceY;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.areas.SafeHouse;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.weather.ClimateManager;
import zombie.iso.zones.Zone;
import zombie.network.BodyDamageSync;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.MPStatistics;
import zombie.network.PassengerMap;
import zombie.network.ServerLOS;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.network.ServerWorldDatabase;
import zombie.network.fields.HitInfo;
import zombie.network.packets.actions.EventPacket;
import zombie.pathfind.Path;
import zombie.pathfind.PathFindBehavior2;
import zombie.pathfind.Point;
import zombie.pathfind.PolygonalMap2;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.savefile.ClientPlayerDB;
import zombie.savefile.PlayerDB;
import zombie.scripting.objects.VehicleScript;
import zombie.ui.TutorialManager;
import zombie.ui.UIManager;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehicleWindow;
import zombie.vehicles.VehiclesDB2;
import zombie.world.WorldDictionary;

public class IsoPlayer extends IsoLivingCharacter implements IAnimalVisual, IHumanVisual {
   private static final float StrongTraitMaxWeightDelta = 1.5F;
   private static final float WeakTraitMaxWeightDelta = 0.75F;
   private static final float FeebleTraitMaxWeightDelta = 0.9F;
   private static final float StoutTraitMaxWeightDelta = 1.25F;
   public PhysicsDebugRenderer physicsDebugRenderer;
   private String attackType;
   public static String DEATH_MUSIC_NAME = "PlayerDied";
   private boolean allowSprint;
   private boolean allowRun;
   public static boolean isTestAIMode = false;
   public static final boolean NoSound = false;
   public static int assumedPlayer = 0;
   public static int numPlayers = 1;
   public static final short MAX = 4;
   public static final IsoPlayer[] players = new IsoPlayer[4];
   private static IsoPlayer instance;
   private static final Object instanceLock = "IsoPlayer.instance Lock";
   private static final Vector2 testHitPosition = new Vector2();
   private static int FollowDeadCount = 240;
   private static final Stack<String> StaticTraits = new Stack();
   private boolean ignoreAutoVault;
   public int remoteSneakLvl;
   public int remoteStrLvl;
   public int remoteFitLvl;
   public boolean canSeeAll;
   public boolean canHearAll;
   public boolean MoodleCantSprint;
   private static final Vector2 tempo = new Vector2();
   private static final Vector2 tempVector2 = new Vector2();
   private static final String forwardStr = "Forward";
   private static final String backwardStr = "Backward";
   private static final String leftStr = "Left";
   private static final String rightStr = "Right";
   private static boolean CoopPVP = false;
   private boolean ignoreContextKey;
   private boolean ignoreInputsForDirection;
   private boolean showMPInfos;
   public long lastRemoteUpdate;
   public ArrayList<IsoAnimal> luredAnimals;
   public boolean isLuringAnimals;
   private boolean invPageDirty;
   private ArrayList<IsoAnimal> attachedAnimals;
   public boolean spottedByPlayer;
   private HashMap<Integer, Integer> spottedPlayerTimer;
   private float extUpdateCount;
   private static final int s_randomIdleFidgetInterval = 5000;
   private boolean attackStarted;
   private static final PredicatedFileWatcher m_isoPlayerTriggerWatcher;
   private final PredicatedFileWatcher m_setClothingTriggerWatcher;
   private static Vector2 tempVector2_1;
   private static Vector2 tempVector2_2;
   protected BaseVisual baseVisual;
   protected final ItemVisuals itemVisuals;
   public boolean targetedByZombie;
   public float lastTargeted;
   public float TimeSinceOpenDoor;
   public float TimeSinceCloseDoor;
   public boolean bRemote;
   public int TimeSinceLastNetData;
   public Role role;
   public String tagPrefix;
   public boolean showTag;
   public boolean factionPvp;
   public short OnlineID;
   public int OnlineChunkGridWidth;
   public boolean bJoypadMovementActive;
   public boolean bJoypadIgnoreAimUntilCentered;
   public boolean bJoypadIgnoreChargingRT;
   protected boolean bJoypadBDown;
   protected boolean bJoypadSprint;
   public boolean mpTorchCone;
   public float mpTorchDist;
   public float mpTorchStrength;
   public int PlayerIndex;
   public int serverPlayerIndex;
   public float useChargeDelta;
   public int JoypadBind;
   public float ContextPanic;
   public float numNearbyBuildingsRooms;
   public boolean isCharging;
   public boolean isChargingLT;
   private boolean bLookingWhileInVehicle;
   private boolean climbOverWallSuccess;
   private boolean climbOverWallStruggle;
   private boolean JustMoved;
   public boolean L3Pressed;
   public boolean R3Pressed;
   public float maxWeightDelta;
   public float CurrentSpeed;
   public float MaxSpeed;
   public boolean bDeathFinished;
   public boolean isSpeek;
   public boolean isVoiceMute;
   public final Vector2 playerMoveDir;
   public BaseSoundListener soundListener;
   public String username;
   public boolean dirtyRecalcGridStack;
   public float dirtyRecalcGridStackTime;
   public float runningTime;
   public float timePressedContext;
   public float chargeTime;
   private float useChargeTime;
   private boolean bPressContext;
   private boolean m_letGoAfterContextIsReleased;
   public float closestZombie;
   public final Vector2 lastAngle;
   public String SaveFileName;
   public boolean bBannedAttacking;
   public int sqlID;
   protected int ClearSpottedTimer;
   protected float timeSinceLastStab;
   protected Stack<IsoMovingObject> LastSpotted;
   protected boolean bChangeCharacterDebounce;
   protected int followID;
   protected final Stack<IsoGameCharacter> FollowCamStack;
   protected boolean bSeenThisFrame;
   protected boolean bCouldBeSeenThisFrame;
   protected float AsleepTime;
   protected final Stack<IsoMovingObject> spottedList;
   protected int TicksSinceSeenZombie;
   protected boolean Waiting;
   protected IsoSurvivor DragCharacter;
   protected float heartDelay;
   protected float heartDelayMax;
   protected long heartEventInstance;
   protected long worldAmbianceInstance;
   protected String Forname;
   protected String Surname;
   protected int DialogMood;
   protected int ping;
   protected IsoMovingObject DragObject;
   private double lastSeenZombieTime;
   private BaseSoundEmitter worldAmbienceEmitter;
   private int checkSafehouse;
   private boolean attackFromBehind;
   private long aimKeyDownMS;
   private long runKeyDownMS;
   private long sprintKeyDownMS;
   private int hypothermiaCache;
   private int hyperthermiaCache;
   private float ticksSincePressedMovement;
   private boolean flickTorch;
   private float checkNearbyRooms;
   private boolean bUseVehicle;
   private boolean bUsedVehicle;
   private float useVehicleDuration;
   private static final Vector3f tempVector3f;
   private final InputState inputState;
   private boolean isWearingNightVisionGoggles;
   private float MoveSpeed;
   private int offSetXUI;
   private int offSetYUI;
   private float combatSpeed;
   private double HoursSurvived;
   private boolean noClip;
   private boolean m_isAuthorizedHandToHandAction;
   private boolean m_isAuthorizedHandToHand;
   private boolean blockMovement;
   private Nutrition nutrition;
   private Fitness fitness;
   private boolean forceOverrideAnim;
   private boolean initiateAttack;
   private final ColorInfo tagColor;
   private String displayName;
   private boolean seeNonPvpZone;
   private boolean seeDesignationZone;
   private ArrayList<Double> selectedZonesForHighlight;
   private Double selectedZoneForHighlight;
   private final HashMap<Long, Long> mechanicsItem;
   private int sleepingPillsTaken;
   private long lastPillsTaken;
   private long heavyBreathInstance;
   private String heavyBreathSoundName;
   private boolean allChatMuted;
   private boolean forceAim;
   private boolean forceRun;
   private boolean forceSprint;
   private boolean bMultiplayer;
   private String SaveFileIP;
   protected BaseVehicle vehicle4testCollision;
   private long steamID;
   private final VehicleContainerData vehicleContainerData;
   private boolean isWalking;
   private int footInjuryTimer;
   private boolean bSneakDebounce;
   private float m_turnDelta;
   protected boolean m_isPlayerMoving;
   private float m_walkSpeed;
   private float m_walkInjury;
   private float m_runSpeed;
   private float m_idleSpeed;
   private float m_deltaX;
   private float m_deltaY;
   private float m_windspeed;
   private float m_windForce;
   private float m_IPX;
   private float m_IPY;
   float drunkDelayCommandTimer;
   private float pressedRunTimer;
   private boolean pressedRun;
   private boolean m_meleePressed;
   private boolean m_grapplePressed;
   private boolean m_canLetGoOfGrappled;
   private boolean m_lastAttackWasHandToHand;
   private boolean m_isPerformingAnAction;
   private ArrayList<String> alreadyReadBook;
   public byte bleedingLevel;
   public final NetworkPlayerAI networkAI;
   private final MusicIntensityEvents m_musicIntensityEvents;
   private final MusicThreatStatuses m_musicThreatStatuses;
   private boolean m_musicIntensity_Inside;
   private boolean isFarming;
   private float m_attackVariationX;
   private float m_attackVariationY;
   public String accessLevel;
   private boolean hasObstacleOnPath;
   private final GrapplerGruntChance m_grapplerGruntChance;
   private static final ArrayList<IsoPlayer> RecentlyRemoved;
   private boolean pathfindRun;
   private static final MoveVars s_moveVars;
   private final MoveVars drunkMoveVars;
   int atkTimer;
   private static final ArrayList<HitInfo> s_targetsProne;
   private static final ArrayList<HitInfo> s_targetsStanding;
   private boolean bReloadButtonDown;
   private boolean bRackButtonDown;
   private boolean bReloadKeyDown;
   private boolean bRackKeyDown;
   private long AttackAnimThrowTimer;
   String WeaponT;
   private final ParameterCharacterMovementSpeed parameterCharacterMovementSpeed;
   private final ParameterCharacterOnFire parameterCharacterOnFire;
   private final ParameterCharacterVoiceType parameterCharacterVoiceType;
   private final ParameterCharacterVoicePitch parameterCharacterVoicePitch;
   private final ParameterDragMaterial parameterDragMaterial;
   private final ParameterEquippedBaggageContainer parameterEquippedBaggageContainer;
   private final ParameterExercising parameterExercising;
   private final ParameterFirearmInside parameterFirearmInside;
   private final ParameterFirearmRoomSize parameterFirearmRoomSize;
   private final ParameterFootstepMaterial parameterFootstepMaterial;
   private final ParameterFootstepMaterial2 parameterFootstepMaterial2;
   private final ParameterIsStashTile parameterIsStashTile;
   private final ParameterLocalPlayer parameterLocalPlayer;
   private final ParameterMeleeHitSurface parameterMeleeHitSurface;
   private final ParameterPlayerHealth parameterPlayerHealth;
   private final ParameterVehicleHitLocation parameterVehicleHitLocation;
   private final ParameterShoeType parameterShoeType;
   private ParameterMoodles parameterMoodles;

   public IsoPlayer(IsoCell var1) {
      this(var1, (SurvivorDesc)null, 0, 0, 0);
   }

   public IsoPlayer(IsoCell var1, SurvivorDesc var2, int var3, int var4, int var5, boolean var6) {
      super(var1, (float)var3, (float)var4, (float)var5);
      this.physicsDebugRenderer = null;
      this.attackType = null;
      this.allowSprint = true;
      this.allowRun = true;
      this.ignoreAutoVault = false;
      this.remoteSneakLvl = 0;
      this.remoteStrLvl = 0;
      this.remoteFitLvl = 0;
      this.canSeeAll = false;
      this.canHearAll = false;
      this.MoodleCantSprint = false;
      this.ignoreContextKey = false;
      this.ignoreInputsForDirection = false;
      this.showMPInfos = false;
      this.lastRemoteUpdate = 0L;
      this.luredAnimals = new ArrayList();
      this.isLuringAnimals = false;
      this.invPageDirty = false;
      this.attachedAnimals = new ArrayList();
      this.spottedByPlayer = false;
      this.spottedPlayerTimer = new HashMap();
      this.extUpdateCount = 0.0F;
      this.attackStarted = false;
      this.itemVisuals = new ItemVisuals();
      this.targetedByZombie = false;
      this.lastTargeted = 1.0E8F;
      this.TimeSinceLastNetData = 0;
      this.role = Roles.getDefaultForNewUser();
      this.tagPrefix = "";
      this.showTag = true;
      this.factionPvp = false;
      this.OnlineID = 1;
      this.bJoypadMovementActive = true;
      this.bJoypadIgnoreChargingRT = false;
      this.bJoypadBDown = false;
      this.bJoypadSprint = false;
      this.mpTorchCone = false;
      this.mpTorchDist = 0.0F;
      this.mpTorchStrength = 0.0F;
      this.PlayerIndex = 0;
      this.serverPlayerIndex = 1;
      this.useChargeDelta = 0.0F;
      this.JoypadBind = -1;
      this.ContextPanic = 0.0F;
      this.numNearbyBuildingsRooms = 0.0F;
      this.isCharging = false;
      this.isChargingLT = false;
      this.bLookingWhileInVehicle = false;
      this.JustMoved = false;
      this.L3Pressed = false;
      this.R3Pressed = false;
      this.maxWeightDelta = 1.0F;
      this.CurrentSpeed = 0.0F;
      this.MaxSpeed = 0.09F;
      this.bDeathFinished = false;
      this.playerMoveDir = new Vector2(0.0F, 0.0F);
      this.username = "Bob";
      this.dirtyRecalcGridStack = true;
      this.dirtyRecalcGridStackTime = 10.0F;
      this.runningTime = 0.0F;
      this.timePressedContext = 0.0F;
      this.chargeTime = 0.0F;
      this.useChargeTime = 0.0F;
      this.bPressContext = false;
      this.m_letGoAfterContextIsReleased = false;
      this.closestZombie = 1000000.0F;
      this.lastAngle = new Vector2();
      this.bBannedAttacking = false;
      this.sqlID = -1;
      this.ClearSpottedTimer = -1;
      this.timeSinceLastStab = 0.0F;
      this.LastSpotted = new Stack();
      this.bChangeCharacterDebounce = false;
      this.followID = 0;
      this.FollowCamStack = new Stack();
      this.bSeenThisFrame = false;
      this.bCouldBeSeenThisFrame = false;
      this.AsleepTime = 0.0F;
      this.spottedList = new Stack();
      this.TicksSinceSeenZombie = 9999999;
      this.Waiting = true;
      this.DragCharacter = null;
      this.heartDelay = 30.0F;
      this.heartDelayMax = 30.0F;
      this.Forname = "Bob";
      this.Surname = "Smith";
      this.DialogMood = 1;
      this.ping = 0;
      this.DragObject = null;
      this.lastSeenZombieTime = 2.0;
      this.worldAmbienceEmitter = null;
      this.checkSafehouse = 200;
      this.attackFromBehind = false;
      this.aimKeyDownMS = 0L;
      this.runKeyDownMS = 0L;
      this.sprintKeyDownMS = 0L;
      this.hypothermiaCache = -1;
      this.hyperthermiaCache = -1;
      this.ticksSincePressedMovement = 0.0F;
      this.flickTorch = false;
      this.checkNearbyRooms = 0.0F;
      this.bUseVehicle = false;
      this.inputState = new InputState();
      this.isWearingNightVisionGoggles = false;
      this.MoveSpeed = 0.06F;
      this.offSetXUI = 0;
      this.offSetYUI = 0;
      this.combatSpeed = 1.0F;
      this.HoursSurvived = 0.0;
      this.noClip = false;
      this.m_isAuthorizedHandToHandAction = true;
      this.m_isAuthorizedHandToHand = true;
      this.blockMovement = false;
      this.forceOverrideAnim = false;
      this.initiateAttack = false;
      this.tagColor = new ColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
      this.displayName = null;
      this.seeNonPvpZone = false;
      this.seeDesignationZone = false;
      this.selectedZonesForHighlight = new ArrayList();
      this.selectedZoneForHighlight = 0.0;
      this.mechanicsItem = new HashMap();
      this.sleepingPillsTaken = 0;
      this.lastPillsTaken = 0L;
      this.heavyBreathInstance = 0L;
      this.heavyBreathSoundName = null;
      this.allChatMuted = false;
      this.forceAim = false;
      this.forceRun = false;
      this.forceSprint = false;
      this.vehicle4testCollision = null;
      this.vehicleContainerData = new VehicleContainerData();
      this.isWalking = false;
      this.footInjuryTimer = 0;
      this.m_turnDelta = 0.0F;
      this.m_isPlayerMoving = false;
      this.m_walkSpeed = 0.0F;
      this.m_walkInjury = 0.0F;
      this.m_runSpeed = 0.0F;
      this.m_idleSpeed = 0.0F;
      this.m_deltaX = 0.0F;
      this.m_deltaY = 0.0F;
      this.m_windspeed = 0.0F;
      this.m_windForce = 0.0F;
      this.m_IPX = 0.0F;
      this.m_IPY = 0.0F;
      this.drunkDelayCommandTimer = 0.0F;
      this.pressedRunTimer = 0.0F;
      this.pressedRun = false;
      this.m_meleePressed = false;
      this.m_grapplePressed = false;
      this.m_canLetGoOfGrappled = true;
      this.m_lastAttackWasHandToHand = false;
      this.m_isPerformingAnAction = false;
      this.alreadyReadBook = new ArrayList();
      this.bleedingLevel = 0;
      this.m_musicIntensityEvents = new MusicIntensityEvents();
      this.m_musicThreatStatuses = new MusicThreatStatuses(this);
      this.m_musicIntensity_Inside = true;
      this.isFarming = false;
      this.m_attackVariationX = 0.0F;
      this.m_attackVariationY = 0.0F;
      this.hasObstacleOnPath = false;
      this.m_grapplerGruntChance = new GrapplerGruntChance();
      this.pathfindRun = false;
      this.drunkMoveVars = new MoveVars();
      this.atkTimer = 0;
      this.bReloadButtonDown = false;
      this.bRackButtonDown = false;
      this.bReloadKeyDown = false;
      this.bRackKeyDown = false;
      this.AttackAnimThrowTimer = System.currentTimeMillis();
      this.WeaponT = null;
      this.parameterCharacterMovementSpeed = new ParameterCharacterMovementSpeed(this);
      this.parameterCharacterOnFire = new ParameterCharacterOnFire(this);
      this.parameterCharacterVoiceType = new ParameterCharacterVoiceType(this);
      this.parameterCharacterVoicePitch = new ParameterCharacterVoicePitch(this);
      this.parameterDragMaterial = new ParameterDragMaterial(this);
      this.parameterEquippedBaggageContainer = new ParameterEquippedBaggageContainer(this);
      this.parameterExercising = new ParameterExercising(this);
      this.parameterFirearmInside = new ParameterFirearmInside(this);
      this.parameterFirearmRoomSize = new ParameterFirearmRoomSize(this);
      this.parameterFootstepMaterial = new ParameterFootstepMaterial(this);
      this.parameterFootstepMaterial2 = new ParameterFootstepMaterial2(this);
      this.parameterIsStashTile = new ParameterIsStashTile(this);
      this.parameterLocalPlayer = new ParameterLocalPlayer(this);
      this.parameterMeleeHitSurface = new ParameterMeleeHitSurface(this);
      this.parameterPlayerHealth = new ParameterPlayerHealth(this);
      this.parameterVehicleHitLocation = new ParameterVehicleHitLocation();
      this.parameterShoeType = new ParameterShoeType(this);
      this.parameterMoodles = null;
      this.setIsAnimal(var6);
      if (this.isAnimal()) {
         this.baseVisual = new AnimalVisual(this);
      } else {
         this.baseVisual = new HumanVisual(this);
         this.registerVariableCallbacks();
         this.registerAnimEventCallbacks();
         this.getWrappedGrappleable().setOnGrappledEndCallback(this::onGrappleEnded);
      }

      this.Traits.addAll(StaticTraits);
      StaticTraits.clear();
      this.dir = IsoDirections.W;
      if (!var6) {
         this.nutrition = new Nutrition(this);
         this.fitness = new Fitness(this);
         this.clothingWetness = new ClothingWetness(this);
         this.initAttachedItems("Human");
      }

      this.initWornItems("Human");
      if (var2 != null) {
         this.descriptor = var2;
      } else {
         this.descriptor = new SurvivorDesc();
      }

      this.setFemale(this.descriptor.isFemale());
      this.setVoiceType(this.descriptor.getVoiceType());
      this.setVoicePitch(this.descriptor.getVoicePitch());
      if (!var6) {
         this.Dressup(this.descriptor);
         this.getHumanVisual().copyFrom(this.descriptor.humanVisual);
         this.InitSpriteParts(this.descriptor);
      }

      LuaEventManager.triggerEvent("OnCreateLivingCharacter", this, this.descriptor);
      this.descriptor.Instance = this;
      if (!var6) {
         this.SpeakColour = new Color(Rand.Next(135) + 120, Rand.Next(135) + 120, Rand.Next(135) + 120, 255);
      }

      if (GameClient.bClient) {
         if (Core.getInstance().getMpTextColor() != null) {
            this.SpeakColour = new Color(Core.getInstance().getMpTextColor().r, Core.getInstance().getMpTextColor().g, Core.getInstance().getMpTextColor().b, 1.0F);
         } else {
            Core.getInstance().setMpTextColor(new ColorInfo(this.SpeakColour.r, this.SpeakColour.g, this.SpeakColour.b, 1.0F));

            try {
               Core.getInstance().saveOptions();
            } catch (IOException var8) {
               var8.printStackTrace();
            }
         }
      }

      if (Core.GameMode.equals("LastStand")) {
         this.Traits.add("Strong");
      }

      if (this.Traits.Strong.isSet()) {
         this.maxWeightDelta = 1.5F;
      } else if (this.Traits.Weak.isSet()) {
         this.maxWeightDelta = 0.75F;
      } else if (this.Traits.Feeble.isSet()) {
         this.maxWeightDelta = 0.9F;
      } else if (this.Traits.Stout.isSet()) {
         this.maxWeightDelta = 1.25F;
      }

      if (this.Traits.Injured.isSet()) {
         this.getBodyDamage().AddRandomDamage();
      }

      this.bMultiplayer = GameServer.bServer || GameClient.bClient;
      this.vehicle4testCollision = null;
      if (!var6 && Core.bDebug && DebugOptions.instance.Cheat.Player.StartInvisible.getValue()) {
         this.setGhostMode(true);
         this.setGodMod(true);
      }

      if (!var6) {
         this.getActionContext().setGroup(ActionGroup.getActionGroup("player"));
         this.initializeStates();
         DebugFileWatcher.instance.add(m_isoPlayerTriggerWatcher);
      }

      this.m_setClothingTriggerWatcher = new PredicatedFileWatcher(ZomboidFileSystem.instance.getMessagingDirSub("Trigger_SetClothing.xml"), TriggerXmlFile.class, this::onTrigger_setClothingToXmlTriggerFile);
      this.networkAI = new NetworkPlayerAI(this);
      this.initFMODParameters();
   }

   public IsoPlayer(IsoCell var1, SurvivorDesc var2, int var3, int var4, int var5) {
      super(var1, (float)var3, (float)var4, (float)var5);
      this.physicsDebugRenderer = null;
      this.attackType = null;
      this.allowSprint = true;
      this.allowRun = true;
      this.ignoreAutoVault = false;
      this.remoteSneakLvl = 0;
      this.remoteStrLvl = 0;
      this.remoteFitLvl = 0;
      this.canSeeAll = false;
      this.canHearAll = false;
      this.MoodleCantSprint = false;
      this.ignoreContextKey = false;
      this.ignoreInputsForDirection = false;
      this.showMPInfos = false;
      this.lastRemoteUpdate = 0L;
      this.luredAnimals = new ArrayList();
      this.isLuringAnimals = false;
      this.invPageDirty = false;
      this.attachedAnimals = new ArrayList();
      this.spottedByPlayer = false;
      this.spottedPlayerTimer = new HashMap();
      this.extUpdateCount = 0.0F;
      this.attackStarted = false;
      this.itemVisuals = new ItemVisuals();
      this.targetedByZombie = false;
      this.lastTargeted = 1.0E8F;
      this.TimeSinceLastNetData = 0;
      this.role = Roles.getDefaultForNewUser();
      this.tagPrefix = "";
      this.showTag = true;
      this.factionPvp = false;
      this.OnlineID = 1;
      this.bJoypadMovementActive = true;
      this.bJoypadIgnoreChargingRT = false;
      this.bJoypadBDown = false;
      this.bJoypadSprint = false;
      this.mpTorchCone = false;
      this.mpTorchDist = 0.0F;
      this.mpTorchStrength = 0.0F;
      this.PlayerIndex = 0;
      this.serverPlayerIndex = 1;
      this.useChargeDelta = 0.0F;
      this.JoypadBind = -1;
      this.ContextPanic = 0.0F;
      this.numNearbyBuildingsRooms = 0.0F;
      this.isCharging = false;
      this.isChargingLT = false;
      this.bLookingWhileInVehicle = false;
      this.JustMoved = false;
      this.L3Pressed = false;
      this.R3Pressed = false;
      this.maxWeightDelta = 1.0F;
      this.CurrentSpeed = 0.0F;
      this.MaxSpeed = 0.09F;
      this.bDeathFinished = false;
      this.playerMoveDir = new Vector2(0.0F, 0.0F);
      this.username = "Bob";
      this.dirtyRecalcGridStack = true;
      this.dirtyRecalcGridStackTime = 10.0F;
      this.runningTime = 0.0F;
      this.timePressedContext = 0.0F;
      this.chargeTime = 0.0F;
      this.useChargeTime = 0.0F;
      this.bPressContext = false;
      this.m_letGoAfterContextIsReleased = false;
      this.closestZombie = 1000000.0F;
      this.lastAngle = new Vector2();
      this.bBannedAttacking = false;
      this.sqlID = -1;
      this.ClearSpottedTimer = -1;
      this.timeSinceLastStab = 0.0F;
      this.LastSpotted = new Stack();
      this.bChangeCharacterDebounce = false;
      this.followID = 0;
      this.FollowCamStack = new Stack();
      this.bSeenThisFrame = false;
      this.bCouldBeSeenThisFrame = false;
      this.AsleepTime = 0.0F;
      this.spottedList = new Stack();
      this.TicksSinceSeenZombie = 9999999;
      this.Waiting = true;
      this.DragCharacter = null;
      this.heartDelay = 30.0F;
      this.heartDelayMax = 30.0F;
      this.Forname = "Bob";
      this.Surname = "Smith";
      this.DialogMood = 1;
      this.ping = 0;
      this.DragObject = null;
      this.lastSeenZombieTime = 2.0;
      this.worldAmbienceEmitter = null;
      this.checkSafehouse = 200;
      this.attackFromBehind = false;
      this.aimKeyDownMS = 0L;
      this.runKeyDownMS = 0L;
      this.sprintKeyDownMS = 0L;
      this.hypothermiaCache = -1;
      this.hyperthermiaCache = -1;
      this.ticksSincePressedMovement = 0.0F;
      this.flickTorch = false;
      this.checkNearbyRooms = 0.0F;
      this.bUseVehicle = false;
      this.inputState = new InputState();
      this.isWearingNightVisionGoggles = false;
      this.MoveSpeed = 0.06F;
      this.offSetXUI = 0;
      this.offSetYUI = 0;
      this.combatSpeed = 1.0F;
      this.HoursSurvived = 0.0;
      this.noClip = false;
      this.m_isAuthorizedHandToHandAction = true;
      this.m_isAuthorizedHandToHand = true;
      this.blockMovement = false;
      this.forceOverrideAnim = false;
      this.initiateAttack = false;
      this.tagColor = new ColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
      this.displayName = null;
      this.seeNonPvpZone = false;
      this.seeDesignationZone = false;
      this.selectedZonesForHighlight = new ArrayList();
      this.selectedZoneForHighlight = 0.0;
      this.mechanicsItem = new HashMap();
      this.sleepingPillsTaken = 0;
      this.lastPillsTaken = 0L;
      this.heavyBreathInstance = 0L;
      this.heavyBreathSoundName = null;
      this.allChatMuted = false;
      this.forceAim = false;
      this.forceRun = false;
      this.forceSprint = false;
      this.vehicle4testCollision = null;
      this.vehicleContainerData = new VehicleContainerData();
      this.isWalking = false;
      this.footInjuryTimer = 0;
      this.m_turnDelta = 0.0F;
      this.m_isPlayerMoving = false;
      this.m_walkSpeed = 0.0F;
      this.m_walkInjury = 0.0F;
      this.m_runSpeed = 0.0F;
      this.m_idleSpeed = 0.0F;
      this.m_deltaX = 0.0F;
      this.m_deltaY = 0.0F;
      this.m_windspeed = 0.0F;
      this.m_windForce = 0.0F;
      this.m_IPX = 0.0F;
      this.m_IPY = 0.0F;
      this.drunkDelayCommandTimer = 0.0F;
      this.pressedRunTimer = 0.0F;
      this.pressedRun = false;
      this.m_meleePressed = false;
      this.m_grapplePressed = false;
      this.m_canLetGoOfGrappled = true;
      this.m_lastAttackWasHandToHand = false;
      this.m_isPerformingAnAction = false;
      this.alreadyReadBook = new ArrayList();
      this.bleedingLevel = 0;
      this.m_musicIntensityEvents = new MusicIntensityEvents();
      this.m_musicThreatStatuses = new MusicThreatStatuses(this);
      this.m_musicIntensity_Inside = true;
      this.isFarming = false;
      this.m_attackVariationX = 0.0F;
      this.m_attackVariationY = 0.0F;
      this.hasObstacleOnPath = false;
      this.m_grapplerGruntChance = new GrapplerGruntChance();
      this.pathfindRun = false;
      this.drunkMoveVars = new MoveVars();
      this.atkTimer = 0;
      this.bReloadButtonDown = false;
      this.bRackButtonDown = false;
      this.bReloadKeyDown = false;
      this.bRackKeyDown = false;
      this.AttackAnimThrowTimer = System.currentTimeMillis();
      this.WeaponT = null;
      this.parameterCharacterMovementSpeed = new ParameterCharacterMovementSpeed(this);
      this.parameterCharacterOnFire = new ParameterCharacterOnFire(this);
      this.parameterCharacterVoiceType = new ParameterCharacterVoiceType(this);
      this.parameterCharacterVoicePitch = new ParameterCharacterVoicePitch(this);
      this.parameterDragMaterial = new ParameterDragMaterial(this);
      this.parameterEquippedBaggageContainer = new ParameterEquippedBaggageContainer(this);
      this.parameterExercising = new ParameterExercising(this);
      this.parameterFirearmInside = new ParameterFirearmInside(this);
      this.parameterFirearmRoomSize = new ParameterFirearmRoomSize(this);
      this.parameterFootstepMaterial = new ParameterFootstepMaterial(this);
      this.parameterFootstepMaterial2 = new ParameterFootstepMaterial2(this);
      this.parameterIsStashTile = new ParameterIsStashTile(this);
      this.parameterLocalPlayer = new ParameterLocalPlayer(this);
      this.parameterMeleeHitSurface = new ParameterMeleeHitSurface(this);
      this.parameterPlayerHealth = new ParameterPlayerHealth(this);
      this.parameterVehicleHitLocation = new ParameterVehicleHitLocation();
      this.parameterShoeType = new ParameterShoeType(this);
      this.parameterMoodles = null;
      this.baseVisual = new HumanVisual(this);
      this.registerVariableCallbacks();
      this.registerAnimEventCallbacks();
      this.getWrappedGrappleable().setOnGrappledEndCallback(this::onGrappleEnded);
      this.Traits.addAll(StaticTraits);
      StaticTraits.clear();
      this.dir = IsoDirections.W;
      this.nutrition = new Nutrition(this);
      this.fitness = new Fitness(this);
      this.initWornItems("Human");
      this.initAttachedItems("Human");
      this.clothingWetness = new ClothingWetness(this);
      if (var2 != null) {
         this.descriptor = var2;
      } else {
         this.descriptor = new SurvivorDesc();
      }

      this.setFemale(this.descriptor.isFemale());
      this.Dressup(this.descriptor);
      this.getHumanVisual().copyFrom(this.descriptor.humanVisual);
      this.InitSpriteParts(this.descriptor);
      LuaEventManager.triggerEvent("OnCreateLivingCharacter", this, this.descriptor);
      this.descriptor.Instance = this;
      this.SpeakColour = new Color(Rand.Next(135) + 120, Rand.Next(135) + 120, Rand.Next(135) + 120, 255);
      if (GameClient.bClient) {
         if (Core.getInstance().getMpTextColor() != null) {
            this.SpeakColour = new Color(Core.getInstance().getMpTextColor().r, Core.getInstance().getMpTextColor().g, Core.getInstance().getMpTextColor().b, 1.0F);
         } else {
            Core.getInstance().setMpTextColor(new ColorInfo(this.SpeakColour.r, this.SpeakColour.g, this.SpeakColour.b, 1.0F));

            try {
               Core.getInstance().saveOptions();
            } catch (IOException var7) {
               var7.printStackTrace();
            }
         }
      }

      if (Core.GameMode.equals("LastStand")) {
         this.Traits.add("Strong");
      }

      if (this.Traits.Strong.isSet()) {
         this.maxWeightDelta = 1.5F;
      } else if (this.Traits.Weak.isSet()) {
         this.maxWeightDelta = 0.75F;
      } else if (this.Traits.Feeble.isSet()) {
         this.maxWeightDelta = 0.9F;
      } else if (this.Traits.Stout.isSet()) {
         this.maxWeightDelta = 1.25F;
      }

      if (this.Traits.Injured.isSet()) {
         this.getBodyDamage().AddRandomDamage();
      }

      this.bMultiplayer = GameServer.bServer || GameClient.bClient;
      this.vehicle4testCollision = null;
      if (Core.bDebug && DebugOptions.instance.Cheat.Player.StartInvisible.getValue()) {
         this.setGhostMode(true);
         this.setGodMod(true);
      }

      this.getActionContext().setGroup(ActionGroup.getActionGroup("player"));
      this.initializeStates();
      DebugFileWatcher.instance.add(m_isoPlayerTriggerWatcher);
      this.m_setClothingTriggerWatcher = new PredicatedFileWatcher(ZomboidFileSystem.instance.getMessagingDirSub("Trigger_SetClothing.xml"), TriggerXmlFile.class, this::onTrigger_setClothingToXmlTriggerFile);
      this.networkAI = new NetworkPlayerAI(this);
      this.setVoiceType(this.descriptor.getVoiceType());
      this.setVoicePitch(this.descriptor.getVoicePitch());
      this.initFMODParameters();
      if (!this.isAnimal()) {
         this.ai.initPlayerAI();
      }

   }

   public void setOnlineID(short var1) {
      this.OnlineID = var1;
   }

   private void registerVariableCallbacks() {
      this.setVariable("CombatSpeed", () -> {
         return this.combatSpeed;
      }, (var1) -> {
         this.combatSpeed = var1;
      });
      this.setVariable("TurnDelta", () -> {
         return this.m_turnDelta;
      }, (var1) -> {
         this.m_turnDelta = var1;
      });
      this.setVariable("sneaking", this::isSneaking, this::setSneaking);
      this.setVariable("initiateAttack", () -> {
         return this.initiateAttack;
      }, this::setInitiateAttack);
      this.setVariable("isMoving", this::isPlayerMoving);
      this.setVariable("isRunning", this::isRunning, this::setRunning);
      this.setVariable("isSprinting", this::isSprinting, this::setSprinting);
      this.setVariable("run", this::isRunning, this::setRunning);
      this.setVariable("sprint", this::isSprinting, this::setSprinting);
      this.setVariable("isStrafing", this::isStrafing);
      this.setVariable("WalkSpeed", () -> {
         return this.m_walkSpeed;
      }, (var1) -> {
         this.m_walkSpeed = var1;
      });
      this.setVariable("WalkInjury", () -> {
         return this.m_walkInjury;
      }, (var1) -> {
         this.m_walkInjury = var1;
      });
      this.setVariable("RunSpeed", () -> {
         return this.m_runSpeed;
      }, (var1) -> {
         this.m_runSpeed = var1;
      });
      this.setVariable("IdleSpeed", () -> {
         return this.m_idleSpeed;
      }, (var1) -> {
         this.m_idleSpeed = var1;
      });
      this.setVariable("DeltaX", () -> {
         return this.m_deltaX;
      }, (var1) -> {
         this.m_deltaX = var1;
      });
      this.setVariable("DeltaY", () -> {
         return this.m_deltaY;
      }, (var1) -> {
         this.m_deltaY = var1;
      });
      this.setVariable("Windspeed", () -> {
         return this.m_windspeed;
      }, (var1) -> {
         this.m_windspeed = var1;
      });
      this.setVariable("WindForce", () -> {
         return this.m_windForce;
      }, (var1) -> {
         this.m_windForce = var1;
      });
      this.setVariable("IPX", () -> {
         return this.m_IPX;
      }, (var1) -> {
         this.m_IPX = var1;
      });
      this.setVariable("IPY", () -> {
         return this.m_IPY;
      }, (var1) -> {
         this.m_IPY = var1;
      });
      this.setVariable("attacktype", () -> {
         return this.attackType;
      });
      this.setVariable("bfalling", this::getAnimVariable_bFalling);
      this.setVariable("baimatfloor", this::isAimAtFloor);
      this.setVariable("attackfrombehind", () -> {
         return this.attackFromBehind;
      });
      this.setVariable("bundervehicle", this::isUnderVehicle);
      this.setVariable("reanimatetimer", this::getReanimateTimer);
      this.setVariable("isattacking", this::isAttacking);
      this.setVariable("beensprintingfor", this::getBeenSprintingFor);
      this.setVariable("bannedAttacking", () -> {
         return this.bBannedAttacking;
      });
      this.setVariable("meleePressed", () -> {
         return this.m_meleePressed;
      });
      this.setVariable("grapplePressed", () -> {
         return this.m_grapplePressed;
      });
      this.setVariable("canLetGoOfGrappled", () -> {
         return this.m_canLetGoOfGrappled;
      });
      this.setVariable("Weapon", this::getWeaponType, this::setWeaponType);
      this.setVariable("BumpFall", false);
      this.setVariable("bClient", () -> {
         return GameClient.bClient;
      });
      this.setVariable("IsPerformingAnAction", this::isPerformingAnAction, this::setPerformingAnAction);
      this.setVariable("bShoveAiming", this::isShovingWhileAiming);
      this.setVariable("bGrappleAiming", this::isGrapplingWhileAiming);
      this.setVariable("AttackVariationX", this::getAttackVariationX);
      this.setVariable("AttackVariationY", this::getAttackVariationY);
   }

   private void registerAnimEventCallbacks() {
      this.addAnimEventListener("GrapplerRandomGrunt", this::OnAnimEvent_GrapplerPlayRandomGrunt);
   }

   private void onGrappleEnded() {
      this.setDoGrappleLetGoAfterContextKeyIsReleased(false);
   }

   private void OnAnimEvent_GrapplerPlayRandomGrunt(IsoGameCharacter var1, String var2) {
      GrapplerGruntChance var10000;
      if (Rand.Next(100) < this.m_grapplerGruntChance.gruntChance) {
         var10000 = this.m_grapplerGruntChance;
         Objects.requireNonNull(this.m_grapplerGruntChance);
         var10000.gruntChance = 15;
         String[] var3 = var2.split(",");
         int var4 = Rand.Next(0, var3.length);
         String var5 = var3[var4];
         DebugLog.Zombie.trace("Dragging corpse. Grunting: %s", var5);
         this.stopPlayerVoiceSound(var5);
         this.playerVoiceSound(var5);
      } else {
         var10000 = this.m_grapplerGruntChance;
         int var10001 = var10000.gruntChance;
         Objects.requireNonNull(this.m_grapplerGruntChance);
         var10000.gruntChance = var10001 + 5;
      }

   }

   protected Vector2 getDeferredMovement(Vector2 var1, boolean var2) {
      super.getDeferredMovement(var1, var2);
      if (DebugOptions.instance.Cheat.Player.InvisibleSprint.getValue() && this.isGhostMode() && (this.IsRunning() || this.isSprinting()) && !this.isCurrentState(ClimbOverFenceState.instance()) && !this.isCurrentState(ClimbThroughWindowState.instance()) && !this.isCurrentState(PlayerGetUpState.instance())) {
         if (this.getPath2() == null && !this.pressedMovement(false) && !this.isAutoWalk()) {
            return var1.set(0.0F, 0.0F);
         }

         if (this.getCurrentBuilding() != null) {
            var1.scale(2.5F);
            return var1;
         }

         var1.scale(7.5F);
      }

      return var1;
   }

   public float getTurnDelta() {
      return !DebugOptions.instance.Cheat.Player.InvisibleSprint.getValue() || !this.isGhostMode() || !this.isRunning() && !this.isSprinting() ? super.getTurnDelta() : 10.0F;
   }

   public void setPerformingAnAction(boolean var1) {
      this.m_isPerformingAnAction = var1;
   }

   public boolean isPerformingAnAction() {
      return this.m_isPerformingAnAction;
   }

   public boolean isAttacking() {
      return !StringUtils.isNullOrWhitespace(this.getAttackType());
   }

   public boolean shouldBeTurning() {
      return this.getAnimationPlayer().getMultiTrack().getTrackCount() == 0 ? false : super.shouldBeTurning();
   }

   public static void invokeOnPlayerInstance(Runnable var0) {
      synchronized(instanceLock) {
         if (instance != null) {
            var0.run();
         }

      }
   }

   public static IsoPlayer getInstance() {
      return instance;
   }

   public static void setInstance(IsoPlayer var0) {
      synchronized(instanceLock) {
         instance = var0;
      }
   }

   public static boolean hasInstance() {
      return instance != null;
   }

   private static void onTrigger_ResetIsoPlayerModel(String var0) {
      if (instance != null) {
         DebugLog.log(DebugType.General, "DebugFileWatcher Hit. Resetting player model: " + var0);
         instance.resetModel();
      } else {
         DebugLog.log(DebugType.General, "DebugFileWatcher Hit. Player instance null : " + var0);
      }

   }

   public static Stack<String> getStaticTraits() {
      return StaticTraits;
   }

   public static int getFollowDeadCount() {
      return FollowDeadCount;
   }

   public static void setFollowDeadCount(int var0) {
      FollowDeadCount = var0;
   }

   public static ArrayList<String> getAllFileNames() {
      ArrayList var0 = new ArrayList();
      String var1 = ZomboidFileSystem.instance.getCurrentSaveDir();

      for(int var2 = 1; var2 < 100; ++var2) {
         File var3 = new File(var1 + File.separator + "map_p" + var2 + ".bin");
         if (var3.exists()) {
            var0.add("map_p" + var2 + ".bin");
         }
      }

      return var0;
   }

   public static String getUniqueFileName() {
      int var0 = 0;
      String var1 = ZomboidFileSystem.instance.getCurrentSaveDir();

      for(int var2 = 1; var2 < 100; ++var2) {
         File var3 = new File(var1 + File.separator + "map_p" + var2 + ".bin");
         if (var3.exists()) {
            var0 = var2;
         }
      }

      ++var0;
      return ZomboidFileSystem.instance.getFileNameInCurrentSave("map_p" + var0 + ".bin");
   }

   public static ArrayList<IsoPlayer> getAllSavedPlayers() {
      ArrayList var0;
      if (GameClient.bClient) {
         var0 = ClientPlayerDB.getInstance().getAllNetworkPlayers();
      } else {
         var0 = PlayerDB.getInstance().getAllLocalPlayers();
      }

      for(int var1 = var0.size() - 1; var1 >= 0; --var1) {
         if (((IsoPlayer)var0.get(var1)).isDead()) {
            var0.remove(var1);
         }
      }

      return var0;
   }

   public static boolean isServerPlayerIDValid(String var0) {
      if (GameClient.bClient) {
         String var1 = ServerOptions.instance.ServerPlayerID.getValue();
         return var1 != null && !var1.isEmpty() ? var1.equals(var0) : true;
      } else {
         return true;
      }
   }

   public static int getPlayerIndex() {
      return instance == null ? assumedPlayer : instance.PlayerIndex;
   }

   public int getIndex() {
      return this.PlayerIndex;
   }

   public static boolean allPlayersDead() {
      for(int var0 = 0; var0 < numPlayers; ++var0) {
         if (players[var0] != null && !players[var0].isDead()) {
            return false;
         }
      }

      return IsoWorld.instance == null || IsoWorld.instance.AddCoopPlayers.isEmpty();
   }

   public static ArrayList<IsoPlayer> getPlayers() {
      return new ArrayList(Arrays.asList(players));
   }

   public static boolean allPlayersAsleep() {
      int var0 = 0;
      int var1 = 0;

      for(int var2 = 0; var2 < numPlayers; ++var2) {
         if (players[var2] != null && !players[var2].isDead()) {
            ++var0;
            if (players[var2] != null && players[var2].isAsleep()) {
               ++var1;
            }
         }
      }

      return var0 > 0 && var0 == var1;
   }

   public static boolean getCoopPVP() {
      return CoopPVP;
   }

   public static void setCoopPVP(boolean var0) {
      CoopPVP = var0;
   }

   public void TestAnimalSpotPlayer(IsoAnimal var1) {
      float var2 = IsoUtils.DistanceManhatten(var1.getX(), var1.getY(), this.getX(), this.getY());
      var1.spotted(this, false, var2);
   }

   public void TestZombieSpotPlayer(IsoMovingObject var1) {
      if (GameServer.bServer && var1 instanceof IsoZombie && ((IsoZombie)var1).target != this && ((IsoZombie)var1).isLeadAggro(this)) {
         GameServer.updateZombieControl((IsoZombie)var1, (short)1, this.OnlineID);
      } else {
         var1.spotted(this, false);
         if (var1 instanceof IsoZombie) {
            float var2 = var1.DistTo(this);
            if (var2 < this.closestZombie && !var1.isOnFloor()) {
               this.closestZombie = var2;
            }
         }

      }
   }

   public float getPathSpeed() {
      float var1 = this.getMoveSpeed() * 0.9F;
      switch (this.Moodles.getMoodleLevel(MoodleType.Endurance)) {
         case 1:
            var1 *= 0.95F;
            break;
         case 2:
            var1 *= 0.9F;
            break;
         case 3:
            var1 *= 0.8F;
            break;
         case 4:
            var1 *= 0.6F;
      }

      if (this.stats.enduranceRecharging) {
         var1 *= 0.85F;
      }

      if (this.getMoodles().getMoodleLevel(MoodleType.HeavyLoad) > 0) {
         float var2 = this.getInventory().getCapacityWeight();
         float var3 = (float)this.getMaxWeight();
         float var4 = Math.min(2.0F, var2 / var3) - 1.0F;
         var1 *= 0.65F + 0.35F * (1.0F - var4);
      }

      return var1;
   }

   public boolean isGhostMode() {
      return this.isInvisible();
   }

   public void setGhostMode(boolean var1) {
      this.setInvisible(var1);
   }

   public boolean isSeeEveryone() {
      return Core.bDebug && DebugOptions.instance.Cheat.Player.SeeEveryone.getValue();
   }

   public boolean zombiesSwitchOwnershipEachUpdate() {
      return SystemDisabler.zombiesSwitchOwnershipEachUpdate;
   }

   public Vector2 getPlayerMoveDir() {
      return this.playerMoveDir;
   }

   public void setPlayerMoveDir(Vector2 var1) {
      this.playerMoveDir.set(var1);
   }

   public void MoveUnmodded(Vector2 var1) {
      if (this.getSlowFactor() > 0.0F) {
         var1.x *= 1.0F - this.getSlowFactor();
         var1.y *= 1.0F - this.getSlowFactor();
      }

      super.MoveUnmodded(var1);
   }

   public void nullifyAiming() {
      if (this.isForceAim()) {
         this.toggleForceAim();
      }

      this.isCharging = false;
      this.setIsAiming(false);
   }

   public boolean isAimKeyDown() {
      if (this.PlayerIndex != 0) {
         return false;
      } else {
         int var1 = GameKeyboard.whichKeyDown("Aim");
         if (var1 == 0) {
            return false;
         } else {
            boolean var2 = var1 == 29 || var1 == 157;
            return !var2 || !UIManager.isMouseOverInventory();
         }
      }
   }

   private void initializeStates() {
      this.clearAIStateMap();
      if (this.getVehicle() == null) {
         this.registerAIState("actions", PlayerActionsState.instance());
         this.registerAIState("aim", PlayerAimState.instance());
         this.registerAIState("climbfence", ClimbOverFenceState.instance());
         this.registerAIState("climbdownrope", ClimbDownSheetRopeState.instance());
         this.registerAIState("climbrope", ClimbSheetRopeState.instance());
         this.registerAIState("climbwall", ClimbOverWallState.instance());
         this.registerAIState("climbwindow", ClimbThroughWindowState.instance());
         this.registerAIState("emote", PlayerEmoteState.instance());
         this.registerAIState("ext", PlayerExtState.instance());
         this.registerAIState("sitext", PlayerExtState.instance());
         this.registerAIState("falldown", PlayerFallDownState.instance());
         this.registerAIState("falling", PlayerFallingState.instance());
         this.registerAIState("getup", PlayerGetUpState.instance());
         this.registerAIState("idle", IdleState.instance());
         this.registerAIState("melee", SwipeStatePlayer.instance());
         this.registerAIState("shove", SwipeStatePlayer.instance());
         this.registerAIState("grappleGrab", SwipeStatePlayer.instance());
         this.registerAIState("draggingBody", SwipeStatePlayer.instance());
         this.registerAIState("ranged", SwipeStatePlayer.instance());
         this.registerAIState("onground", PlayerOnGroundState.instance());
         this.registerAIState("knockeddown", PlayerKnockedDown.instance());
         this.registerAIState("openwindow", OpenWindowState.instance());
         this.registerAIState("closewindow", CloseWindowState.instance());
         this.registerAIState("smashwindow", SmashWindowState.instance());
         this.registerAIState("fishing", FishingState.instance());
         this.registerAIState("fitness", FitnessState.instance());
         this.registerAIState("hitreaction", PlayerHitReactionState.instance());
         this.registerAIState("hitreactionpvp", PlayerHitReactionPVPState.instance());
         this.registerAIState("hitreaction-hit", PlayerHitReactionPVPState.instance());
         this.registerAIState("collide", CollideWithWallState.instance());
         this.registerAIState("bumped", BumpedState.instance());
         this.registerAIState("bumped-bump", BumpedState.instance());
         this.registerAIState("onbed", PlayerOnBedState.instance());
         this.registerAIState("sitonfurniture", PlayerSitOnFurnitureState.instance());
         this.registerAIState("sitonground", PlayerSitOnGroundState.instance());
         this.registerAIState("strafe", PlayerStrafeState.instance());
      } else {
         this.registerAIState("aim", PlayerAimState.instance());
         this.registerAIState("idle", IdleState.instance());
         this.registerAIState("melee", SwipeStatePlayer.instance());
         this.registerAIState("shove", SwipeStatePlayer.instance());
         this.registerAIState("ranged", SwipeStatePlayer.instance());
      }

   }

   protected void onAnimPlayerCreated(AnimationPlayer var1) {
      super.onAnimPlayerCreated(var1);
      if (!this.isAnimal()) {
         var1.addBoneReparent("Bip01_L_Thigh", "Bip01");
         var1.addBoneReparent("Bip01_R_Thigh", "Bip01");
         var1.addBoneReparent("Bip01_L_Clavicle", "Bip01_Spine1");
         var1.addBoneReparent("Bip01_R_Clavicle", "Bip01_Spine1");
         var1.addBoneReparent("Bip01_Prop1", "Bip01_R_Hand");
         var1.addBoneReparent("Bip01_Prop2", "Bip01_L_Hand");
      }
   }

   public String GetAnimSetName() {
      return this.getVehicle() == null ? "player" : "player-vehicle";
   }

   public boolean IsInMeleeAttack() {
      return this.isCurrentState(SwipeStatePlayer.instance());
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      byte var4 = var1.get();
      byte var5 = var1.get();
      super.load(var1, var2, var3);
      this.setHoursSurvived(var1.getDouble());
      SurvivorDesc var6 = this.descriptor;
      this.setFemale(var6.isFemale());
      this.InitSpriteParts(var6);
      this.SpeakColour = new Color(Rand.Next(135) + 120, Rand.Next(135) + 120, Rand.Next(135) + 120, 255);
      if (GameClient.bClient) {
         if (Core.getInstance().getMpTextColor() != null) {
            this.SpeakColour = new Color(Core.getInstance().getMpTextColor().r, Core.getInstance().getMpTextColor().g, Core.getInstance().getMpTextColor().b, 1.0F);
         } else {
            Core.getInstance().setMpTextColor(new ColorInfo(this.SpeakColour.r, this.SpeakColour.g, this.SpeakColour.b, 1.0F));

            try {
               Core.getInstance().saveOptions();
            } catch (IOException var15) {
               var15.printStackTrace();
            }
         }
      }

      this.setZombieKills(var1.getInt());
      ArrayList var7 = this.savedInventoryItems;
      byte var8 = var1.get();

      int var11;
      for(int var9 = 0; var9 < var8; ++var9) {
         String var10 = GameWindow.ReadString(var1);
         var11 = var1.getShort();
         if (var11 >= 0 && var11 < var7.size() && this.wornItems.getBodyLocationGroup().getLocation(var10) != null) {
            this.wornItems.setItem(var10, (InventoryItem)var7.get(var11));
         }
      }

      short var16 = var1.getShort();
      if (var16 >= 0 && var16 < var7.size()) {
         this.leftHandItem = (InventoryItem)var7.get(var16);
      }

      var16 = var1.getShort();
      if (var16 >= 0 && var16 < var7.size()) {
         this.rightHandItem = (InventoryItem)var7.get(var16);
      }

      this.setVariable("Weapon", WeaponType.getWeaponType((IsoGameCharacter)this).type);
      this.setSurvivorKills(var1.getInt());
      this.initSpritePartsEmpty();
      this.nutrition.load(var1);
      this.setAllChatMuted(var1.get() == 1);
      this.tagPrefix = GameWindow.ReadString(var1);
      this.setTagColor(new ColorInfo(var1.getFloat(), var1.getFloat(), var1.getFloat(), 1.0F));
      this.setDisplayName(GameWindow.ReadString(var1));
      this.showTag = var1.get() == 1;
      this.factionPvp = var1.get() == 1;
      if (var2 >= 198) {
         this.setExtraInfoFlags(var1.get());
      } else {
         this.noClip = var1.get() == 1;
      }

      if (var1.get() == 1) {
         this.savedVehicleX = var1.getFloat();
         this.savedVehicleY = var1.getFloat();
         this.savedVehicleSeat = (short)var1.get();
         this.savedVehicleRunning = var1.get() == 1;
         this.setZ(0.0F);
      }

      int var17 = var1.getInt();

      for(var11 = 0; var11 < var17; ++var11) {
         this.mechanicsItem.put(var1.getLong(), var1.getLong());
      }

      this.fitness.load(var1, var2);
      short var18 = var1.getShort();

      for(int var12 = 0; var12 < var18; ++var12) {
         short var13 = var1.getShort();
         String var14 = WorldDictionary.getItemTypeFromID(var13);
         if (var14 != null) {
            this.alreadyReadBook.add(var14);
         }
      }

      this.loadKnownMediaLines(var1, var2);
      if (var2 >= 203) {
         this.setVoiceType(var1.get());
      }

   }

   public void setExtraInfoFlags(byte var1) {
      this.setGodMod((var1 & 1) != 0);
      this.setGhostMode((var1 & 2) != 0);
      this.setInvisible((var1 & 4) != 0);
      this.setNoClip((var1 & 8) != 0);
      this.setShowAdminTag((var1 & 16) != 0);
      this.setCanHearAll((var1 & 32) != 0);
   }

   public byte getExtraInfoFlags() {
      boolean var1 = this.isInvisible() || this.isGodMod() || this.isGhostMode() || this.isNoClip() || this.isTimedActionInstantCheat() || this.isUnlimitedCarry() || this.isUnlimitedEndurance() || this.isBuildCheat() || this.isFarmingCheat() || this.isFishingCheat() || this.isHealthCheat() || this.isMechanicsCheat() || this.isMovablesCheat() || this.isCanSeeAll() || this.isCanHearAll() || this.isZombiesDontAttack() || this.isShowMPInfos();
      return (byte)((this.isGodMod() ? 1 : 0) | (this.isGhostMode() ? 2 : 0) | (this.isInvisible() ? 4 : 0) | (this.isNoClip() ? 8 : 0) | (var1 ? 16 : 0) | (this.isCanHearAll() ? 32 : 0));
   }

   public String getDescription(String var1) {
      String var10000 = this.getClass().getSimpleName();
      String var2 = var10000 + " [" + var1;
      var2 = var2 + super.getDescription(var1 + "    ") + " | " + var1;
      var2 = var2 + "hoursSurvived=" + this.getHoursSurvived() + " | " + var1;
      var2 = var2 + "zombieKills=" + this.getZombieKills() + " | " + var1;
      var2 = var2 + "wornItems=";

      int var3;
      for(var3 = 0; var3 < this.getWornItems().size() - 1; ++var3) {
         var2 = var2 + this.getWornItems().get(var3).getItem() + ", ";
      }

      var2 = var2 + " | " + var1;
      var2 = var2 + "primaryHandItem=" + this.getPrimaryHandItem() + " | " + var1;
      var2 = var2 + "secondaryHandType=" + this.getSecondaryHandType() + " | " + var1;
      var2 = var2 + "survivorKills=" + this.getSurvivorKills() + " | " + var1;
      if (this.isAllChatMuted()) {
         var2 = var2 + "AllChatMuted | " + var1;
      }

      var2 = var2 + "tag=" + this.tagPrefix + ", color=(" + this.getTagColor().r + ", " + this.getTagColor().g + ", " + this.getTagColor().b + ") | " + var1;
      var2 = var2 + "displayName=" + this.displayName + " | " + var1;
      if (this.showTag) {
         var2 = var2 + "showTag | " + var1;
      }

      if (this.factionPvp) {
         var2 = var2 + "factionPvp | " + var1;
      }

      if (this.isNoClip()) {
         var2 = var2 + "noClip | " + var1;
      }

      if (this.vehicle != null) {
         var2 = var2 + "vehicle [ pos=(" + this.vehicle.getX() + ", " + this.vehicle.getY() + ") seat=" + this.vehicle.getSeat(this) + " isEngineRunning=" + this.vehicle.isEngineRunning() + " ] " + var1;
      }

      var2 = var2 + "mechanicsItem=";

      for(var3 = 0; var3 < this.mechanicsItem.size() - 1; ++var3) {
         var2 = var2 + this.mechanicsItem.get(var3) + ", ";
      }

      var2 = var2 + " | " + var1;
      var2 = var2 + "alreadyReadBook=";

      for(var3 = 0; var3 < this.alreadyReadBook.size() - 1; ++var3) {
         var2 = var2 + (String)this.alreadyReadBook.get(var3) + ", ";
      }

      var2 = var2 + " ] ";
      return var2;
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      IsoPlayer var3 = instance;
      instance = this;

      try {
         super.save(var1, var2);
      } finally {
         instance = var3;
      }

      var1.putDouble(this.getHoursSurvived());
      var1.putInt(this.getZombieKills());
      if (this.wornItems.size() > 127) {
         throw new RuntimeException("too many worn items");
      } else {
         var1.put((byte)this.wornItems.size());
         this.wornItems.forEach((var2x) -> {
            GameWindow.WriteString(var1, var2x.getLocation());
            var1.putShort((short)this.savedInventoryItems.indexOf(var2x.getItem()));
         });
         var1.putShort((short)this.savedInventoryItems.indexOf(this.getPrimaryHandItem()));
         var1.putShort((short)this.savedInventoryItems.indexOf(this.getSecondaryHandItem()));
         var1.putInt(this.getSurvivorKills());
         this.nutrition.save(var1);
         var1.put((byte)(this.isAllChatMuted() ? 1 : 0));
         GameWindow.WriteString(var1, this.tagPrefix);
         var1.putFloat(this.getTagColor().r);
         var1.putFloat(this.getTagColor().g);
         var1.putFloat(this.getTagColor().b);
         GameWindow.WriteString(var1, this.displayName);
         var1.put((byte)(this.showTag ? 1 : 0));
         var1.put((byte)(this.factionPvp ? 1 : 0));
         var1.put(this.getExtraInfoFlags());
         if (this.vehicle != null) {
            var1.put((byte)1);
            var1.putFloat(this.vehicle.getX());
            var1.putFloat(this.vehicle.getY());
            var1.put((byte)this.vehicle.getSeat(this));
            var1.put((byte)(this.vehicle.isEngineRunning() ? 1 : 0));
         } else {
            var1.put((byte)0);
         }

         var1.putInt(this.mechanicsItem.size());
         Iterator var4 = this.mechanicsItem.keySet().iterator();

         while(var4.hasNext()) {
            Long var5 = (Long)var4.next();
            var1.putLong(var5);
            var1.putLong((Long)this.mechanicsItem.get(var5));
         }

         this.fitness.save(var1);
         var1.putShort((short)this.alreadyReadBook.size());

         for(int var8 = 0; var8 < this.alreadyReadBook.size(); ++var8) {
            var1.putShort(WorldDictionary.getItemRegistryID((String)this.alreadyReadBook.get(var8)));
         }

         this.saveKnownMediaLines(var1);
         var1.put((byte)this.getVoiceType());
      }
   }

   public void save() throws IOException {
      synchronized(SliceY.SliceBufferLock) {
         ByteBuffer var2 = SliceY.SliceBuffer;
         var2.clear();
         var2.put((byte)80);
         var2.put((byte)76);
         var2.put((byte)89);
         var2.put((byte)82);
         var2.putInt(219);
         GameWindow.WriteString(var2, this.bMultiplayer ? ServerOptions.instance.ServerPlayerID.getValue() : "");
         var2.putInt(PZMath.fastfloor(this.getX() / 8.0F));
         var2.putInt(PZMath.fastfloor(this.getY() / 8.0F));
         var2.putInt(PZMath.fastfloor(this.getX()));
         var2.putInt(PZMath.fastfloor(this.getY()));
         var2.putInt(PZMath.fastfloor(this.getZ()));
         this.save(var2);
         File var3 = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("map_p.bin"));
         if (!Core.getInstance().isNoSave()) {
            FileOutputStream var4 = new FileOutputStream(var3);

            try {
               BufferedOutputStream var5 = new BufferedOutputStream(var4);

               try {
                  var5.write(var2.array(), 0, var2.position());
               } catch (Throwable var11) {
                  try {
                     var5.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }

                  throw var11;
               }

               var5.close();
            } catch (Throwable var12) {
               try {
                  var4.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }

               throw var12;
            }

            var4.close();
         }

         if (this.getVehicle() != null && !GameClient.bClient) {
            VehiclesDB2.instance.updateVehicleAndTrailer(this.getVehicle());
         }

      }
   }

   public void save(String var1) throws IOException {
      this.SaveFileName = var1;
      synchronized(SliceY.SliceBufferLock) {
         SliceY.SliceBuffer.clear();
         SliceY.SliceBuffer.putInt(219);
         GameWindow.WriteString(SliceY.SliceBuffer, this.bMultiplayer ? ServerOptions.instance.ServerPlayerID.getValue() : "");
         this.save(SliceY.SliceBuffer);
         File var3 = (new File(var1)).getAbsoluteFile();
         FileOutputStream var4 = new FileOutputStream(var3);

         try {
            BufferedOutputStream var5 = new BufferedOutputStream(var4);

            try {
               var5.write(SliceY.SliceBuffer.array(), 0, SliceY.SliceBuffer.position());
            } catch (Throwable var11) {
               try {
                  var5.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }

               throw var11;
            }

            var5.close();
         } catch (Throwable var12) {
            try {
               var4.close();
            } catch (Throwable var9) {
               var12.addSuppressed(var9);
            }

            throw var12;
         }

         var4.close();
      }
   }

   public void load(String var1) throws IOException {
      File var2 = (new File(var1)).getAbsoluteFile();
      if (var2.exists()) {
         this.SaveFileName = var1;
         FileInputStream var3 = new FileInputStream(var2);

         try {
            BufferedInputStream var4 = new BufferedInputStream(var3);

            try {
               synchronized(SliceY.SliceBufferLock) {
                  SliceY.SliceBuffer.clear();
                  int var6 = var4.read(SliceY.SliceBuffer.array());
                  SliceY.SliceBuffer.limit(var6);
                  int var7 = SliceY.SliceBuffer.getInt();
                  this.SaveFileIP = GameWindow.ReadStringUTF(SliceY.SliceBuffer);
                  this.load(SliceY.SliceBuffer, var7);
               }
            } catch (Throwable var12) {
               try {
                  var4.close();
               } catch (Throwable var10) {
                  var12.addSuppressed(var10);
               }

               throw var12;
            }

            var4.close();
         } catch (Throwable var13) {
            try {
               var3.close();
            } catch (Throwable var9) {
               var13.addSuppressed(var9);
            }

            throw var13;
         }

         var3.close();
      }
   }

   public void removeFromWorld() {
      this.getEmitter().stopOrTriggerSoundByName("BurningFlesh");
      this.getEmitter().stopSoundLocal(this.vocalEvent);
      this.vocalEvent = 0L;
      this.removedFromWorldMS = System.currentTimeMillis();
      if (!(this instanceof IsoAnimal) && !RecentlyRemoved.contains(this)) {
         RecentlyRemoved.add(this);
      }

      super.removeFromWorld();
   }

   public static void UpdateRemovedEmitters() {
      IsoCell var0 = IsoWorld.instance.CurrentCell;
      long var1 = System.currentTimeMillis();

      for(int var3 = RecentlyRemoved.size() - 1; var3 >= 0; --var3) {
         IsoPlayer var4 = (IsoPlayer)RecentlyRemoved.get(var3);
         if ((var0.getObjectList().contains(var4) || var0.getAddList().contains(var4)) && !var0.getRemoveList().contains(var4)) {
            RecentlyRemoved.remove(var3);
         } else {
            var4.getFMODParameters().update();
            var4.getEmitter().tick();
            if (var1 - var4.removedFromWorldMS > 5000L) {
               var4.getEmitter().stopAll();
               RecentlyRemoved.remove(var3);
            }
         }
      }

   }

   public static void Reset() {
      for(int var0 = 0; var0 < 4; ++var0) {
         IsoPlayer var1 = players[var0];
         if (var1 != null) {
            var1.getAdvancedAnimator().Reset();
            var1.releaseAnimationPlayer();
            var1.getSpottedList().clear();
            var1.LastSpotted.clear();
         }

         players[var0] = null;
      }

      RecentlyRemoved.clear();
   }

   public void setVehicle4TestCollision(BaseVehicle var1) {
      this.vehicle4testCollision = var1;
   }

   public boolean isSaveFileInUse() {
      for(int var1 = 0; var1 < numPlayers; ++var1) {
         IsoPlayer var2 = players[var1];
         if (var2 != null) {
            if (this.sqlID != -1 && this.sqlID == var2.sqlID) {
               return true;
            }

            if (this.SaveFileName != null && this.SaveFileName.equals(var2.SaveFileName)) {
               return true;
            }
         }
      }

      return false;
   }

   public void removeSaveFile() {
      try {
         if (PlayerDB.isAvailable()) {
            PlayerDB.getInstance().saveLocalPlayersForce();
         }

         if (this.isNPC() && this.SaveFileName != null) {
            File var1 = (new File(this.SaveFileName)).getAbsoluteFile();
            if (var1.exists()) {
               var1.delete();
            }
         }
      } catch (Exception var2) {
         ExceptionLogger.logException(var2);
      }

   }

   public boolean isSaveFileIPValid() {
      return isServerPlayerIDValid(this.SaveFileIP);
   }

   public String getObjectName() {
      return "Player";
   }

   public int getJoypadBind() {
      return this.JoypadBind;
   }

   public boolean isLBPressed() {
      return this.JoypadBind == -1 ? false : JoypadManager.instance.isLBPressed(this.JoypadBind);
   }

   public Vector2 getControllerAimDir(Vector2 var1) {
      if (GameWindow.ActivatedJoyPad != null && this.JoypadBind != -1 && this.bJoypadMovementActive) {
         float var2 = JoypadManager.instance.getAimingAxisX(this.JoypadBind);
         float var3 = JoypadManager.instance.getAimingAxisY(this.JoypadBind);
         if (this.bJoypadIgnoreAimUntilCentered) {
            if (var1.set(var2, var3).getLengthSquared() > 0.0F) {
               return var1.set(0.0F, 0.0F);
            }

            this.bJoypadIgnoreAimUntilCentered = false;
         }

         if (var1.set(var2, var3).getLength() < 0.3F) {
            var3 = 0.0F;
            var2 = 0.0F;
         }

         if (var2 == 0.0F && var3 == 0.0F) {
            return var1.set(0.0F, 0.0F);
         }

         var1.set(var2, var3);
         var1.normalize();
         var1.rotate(-0.7853982F);
      }

      return var1;
   }

   public Vector2 getMouseAimVector(Vector2 var1) {
      int var2 = Mouse.getX();
      int var3 = Mouse.getY();
      var1.x = IsoUtils.XToIso((float)var2, (float)var3 + 55.0F * this.def.getScaleY(), this.getZ()) - this.getX();
      var1.y = IsoUtils.YToIso((float)var2, (float)var3 + 55.0F * this.def.getScaleY(), this.getZ()) - this.getY();
      var1.normalize();
      return var1;
   }

   public Vector2 getAimVector(Vector2 var1) {
      return this.JoypadBind == -1 ? this.getMouseAimVector(var1) : this.getControllerAimDir(var1);
   }

   public float getGlobalMovementMod(boolean var1) {
      return !this.isGhostMode() && !this.isNoClip() ? super.getGlobalMovementMod(var1) : 1.0F;
   }

   public boolean isInTrees2(boolean var1) {
      return !this.isGhostMode() && !this.isNoClip() ? super.isInTrees2(var1) : false;
   }

   public float getMoveSpeed() {
      float var1 = 1.0F;

      for(int var2 = BodyPartType.ToIndex(BodyPartType.UpperLeg_L); var2 <= BodyPartType.ToIndex(BodyPartType.Foot_R); ++var2) {
         BodyPart var3 = this.getBodyDamage().getBodyPart(BodyPartType.FromIndex(var2));
         float var4 = 1.0F;
         if (var3.getFractureTime() > 20.0F) {
            var4 = 0.4F;
            if (var3.getFractureTime() > 50.0F) {
               var4 = 0.3F;
            }

            if (var3.getSplintFactor() > 0.0F) {
               var4 += var3.getSplintFactor() / 10.0F;
            }
         }

         if (var3.getFractureTime() < 20.0F && var3.getSplintFactor() > 0.0F) {
            var4 = 0.8F;
         }

         if (var4 > 0.7F && var3.getDeepWoundTime() > 0.0F) {
            var4 = 0.7F;
            if (var3.bandaged()) {
               var4 += 0.2F;
            }
         }

         if (var4 < var1) {
            var1 = var4;
         }
      }

      if (var1 != 1.0F) {
         return this.MoveSpeed * var1;
      } else if (this.getMoodles().getMoodleLevel(MoodleType.Panic) >= 4 && this.Traits.AdrenalineJunkie.isSet()) {
         float var5 = 1.0F;
         int var6 = this.getMoodles().getMoodleLevel(MoodleType.Panic) + 1;
         var5 += (float)var6 / 50.0F;
         return this.MoveSpeed * var5;
      } else {
         return this.MoveSpeed;
      }
   }

   public void setMoveSpeed(float var1) {
      this.MoveSpeed = var1;
   }

   public float getTorchStrength() {
      if (this.isAnimal()) {
         return 0.0F;
      } else if (this.bRemote) {
         return this.mpTorchStrength;
      } else {
         InventoryItem var1 = this.getActiveLightItem();
         return var1 != null ? var1.getLightStrength() : 0.0F;
      }
   }

   public float getInvAimingMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Aiming);
      if (var1 == 1) {
         return 0.9F;
      } else if (var1 == 2) {
         return 0.86F;
      } else if (var1 == 3) {
         return 0.82F;
      } else if (var1 == 4) {
         return 0.74F;
      } else if (var1 == 5) {
         return 0.7F;
      } else if (var1 == 6) {
         return 0.66F;
      } else if (var1 == 7) {
         return 0.62F;
      } else if (var1 == 8) {
         return 0.58F;
      } else if (var1 == 9) {
         return 0.54F;
      } else {
         return var1 == 10 ? 0.5F : 0.9F;
      }
   }

   public float getAimingMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Aiming);
      if (var1 == 1) {
         return 1.1F;
      } else if (var1 == 2) {
         return 1.14F;
      } else if (var1 == 3) {
         return 1.18F;
      } else if (var1 == 4) {
         return 1.22F;
      } else if (var1 == 5) {
         return 1.26F;
      } else if (var1 == 6) {
         return 1.3F;
      } else if (var1 == 7) {
         return 1.34F;
      } else if (var1 == 8) {
         return 1.36F;
      } else if (var1 == 9) {
         return 1.4F;
      } else {
         return var1 == 10 ? 1.5F : 1.0F;
      }
   }

   public float getReloadingMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Reloading);
      return 3.5F - (float)var1 * 0.25F;
   }

   public float getAimingRangeMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Aiming);
      if (var1 == 1) {
         return 1.2F;
      } else if (var1 == 2) {
         return 1.28F;
      } else if (var1 == 3) {
         return 1.36F;
      } else if (var1 == 4) {
         return 1.42F;
      } else if (var1 == 5) {
         return 1.5F;
      } else if (var1 == 6) {
         return 1.58F;
      } else if (var1 == 7) {
         return 1.66F;
      } else if (var1 == 8) {
         return 1.72F;
      } else if (var1 == 9) {
         return 1.8F;
      } else {
         return var1 == 10 ? 2.0F : 1.1F;
      }
   }

   public boolean isPathfindRunning() {
      return this.pathfindRun;
   }

   public void setPathfindRunning(boolean var1) {
      this.pathfindRun = var1;
   }

   public boolean isBannedAttacking() {
      return this.bBannedAttacking;
   }

   public void setBannedAttacking(boolean var1) {
      this.bBannedAttacking = var1;
   }

   public float getInvAimingRangeMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Aiming);
      if (var1 == 1) {
         return 0.8F;
      } else if (var1 == 2) {
         return 0.7F;
      } else if (var1 == 3) {
         return 0.62F;
      } else if (var1 == 4) {
         return 0.56F;
      } else if (var1 == 5) {
         return 0.45F;
      } else if (var1 == 6) {
         return 0.38F;
      } else if (var1 == 7) {
         return 0.31F;
      } else if (var1 == 8) {
         return 0.24F;
      } else if (var1 == 9) {
         return 0.17F;
      } else {
         return var1 == 10 ? 0.1F : 0.8F;
      }
   }

   private void updateCursorVisibility() {
      if (this.isAiming()) {
         if (this.PlayerIndex == 0 && this.JoypadBind == -1 && !this.isDead()) {
            if (!Core.getInstance().getOptionShowCursorWhileAiming()) {
               if (Core.getInstance().getIsoCursorVisibility() != 0) {
                  if (!UIManager.isForceCursorVisible()) {
                     int var1 = Mouse.getXA();
                     int var2 = Mouse.getYA();
                     if (var1 >= IsoCamera.getScreenLeft(0) && var1 <= IsoCamera.getScreenLeft(0) + IsoCamera.getScreenWidth(0)) {
                        if (var2 >= IsoCamera.getScreenTop(0) && var2 <= IsoCamera.getScreenTop(0) + IsoCamera.getScreenHeight(0)) {
                           Mouse.setCursorVisible(false);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void render(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      if (Core.getInstance().displayPlayerModel || this.isAnimal()) {
         if (!this.attachedAnimals.isEmpty()) {
            for(int var8 = 0; var8 < this.attachedAnimals.size(); ++var8) {
               ((IsoAnimal)this.attachedAnimals.get(var8)).drawRope((IsoGameCharacter)this);
            }
         }

         if (DebugOptions.instance.Character.Debug.Render.DisplayRoomAndZombiesZone.getValue()) {
            String var10 = "";
            if (this.getCurrentRoomDef() != null) {
               var10 = this.getCurrentRoomDef().name;
            }

            Zone var9 = ZombiesZoneDefinition.getDefinitionZoneAt(PZMath.fastfloor(var1), PZMath.fastfloor(var2), PZMath.fastfloor(var3));
            if (var9 != null) {
               var10 = var10 + " - " + var9.name + " / " + var9.type;
            }

            this.Say(var10);
         }

         if (!getInstance().checkCanSeeClient(this)) {
            this.setTargetAlpha(0.0F);
            getInstance().spottedPlayerTimer.remove(this.getRemoteID());
         } else {
            this.setTargetAlpha(1.0F);
         }

         super.render(var1, var2, var3, var4, var5, var6, var7);
      }
   }

   public void renderlast() {
      super.renderlast();
      if (DebugOptions.instance.Character.Debug.Render.FMODRoomType.getValue() && this.isLocalPlayer()) {
         ParameterRoomTypeEx.render(this);
      }

   }

   public float doBeatenVehicle(float var1) {
      if (GameClient.bClient && this.isLocalPlayer()) {
         this.changeState(ForecastBeatenPlayerState.instance());
         return 0.0F;
      } else if (!GameClient.bClient && !this.isLocalPlayer()) {
         return 0.0F;
      } else {
         float var2 = this.getDamageFromHitByACar(var1);
         LuaEventManager.triggerEvent("OnPlayerGetDamage", this, "CARHITDAMAGE", var2);
         if (this.isAlive()) {
            if (GameClient.bClient) {
               if (this.isCurrentState(PlayerSitOnGroundState.instance())) {
                  this.setKnockedDown(ServerOptions.getInstance().KnockedDownAllowed.getValue());
                  this.setReanimateTimer(20.0F);
               } else if (!this.isOnFloor() && !(var1 > 15.0F) && !this.isCurrentState(PlayerHitReactionState.instance()) && !this.isCurrentState(PlayerGetUpState.instance()) && !this.isCurrentState(PlayerOnGroundState.instance())) {
                  this.setHitReaction("HitReaction");
                  this.getActionContext().reportEvent("washit");
                  this.setVariable("hitpvp", false);
               } else {
                  this.setHitReaction("HitReaction");
                  this.getActionContext().reportEvent("washit");
                  this.setVariable("hitpvp", false);
                  this.setKnockedDown(ServerOptions.getInstance().KnockedDownAllowed.getValue());
                  this.setReanimateTimer(20.0F);
               }
            } else if (this.getCurrentState() != PlayerHitReactionState.instance() && this.getCurrentState() != PlayerFallDownState.instance() && this.getCurrentState() != PlayerOnGroundState.instance() && !this.isKnockedDown()) {
               if (var2 > 15.0F) {
                  this.setKnockedDown(ServerOptions.getInstance().KnockedDownAllowed.getValue());
                  this.setReanimateTimer(20.0F + (float)Rand.Next(60));
               }

               this.setHitReaction("HitReaction");
               this.getActionContext().reportEvent("washit");
            }
         }

         return var2;
      }
   }

   public void update() {
      IsoPlayer.s_performance.update.invokeAndMeasure(this, IsoPlayer::updateInternal1);
   }

   private void updateInternal1() {
      if (this.isAnimal()) {
         super.update();
         AnimalInstanceManager.getInstance().update((IsoAnimal)this);
      } else {
         boolean var1 = this.updateInternal2();
         if (var1) {
            if (!this.bRemote) {
               this.updateLOS();
            }

            super.update();
         }

         GameClient.instance.sendPlayer2(this);
      }
   }

   private void setBeenMovingSprinting() {
      if (this.isJustMoved()) {
         this.setBeenMovingFor(this.getBeenMovingFor() + 1.25F * GameTime.getInstance().getMultiplier());
      } else {
         this.setBeenMovingFor(this.getBeenMovingFor() - 0.625F * GameTime.getInstance().getMultiplier());
      }

      if (this.isJustMoved() && this.isSprinting()) {
         this.setBeenSprintingFor(this.getBeenSprintingFor() + 1.25F * GameTime.getInstance().getMultiplier());
      } else {
         this.setBeenSprintingFor(0.0F);
      }

   }

   private boolean updateInternal2() {
      if (Core.bDebug && GameKeyboard.isKeyPressed(28)) {
         FBORenderChunkManager.instance.clearCache();
      }

      if (isTestAIMode) {
         this.isNPC = true;
      }

      if (!GameServer.bServer && !this.isNPC) {
         if ((this.bFalling || this.fallTime > 0.0F || this.isOnFire()) && UIManager.speedControls.getCurrentGameSpeed() > 1) {
            UIManager.speedControls.SetCurrentGameSpeed(1);
         }

         if ((this.bFalling || this.fallTime > 0.0F) && this.isCurrentState(SwipeStatePlayer.instance())) {
            this.setDoGrapple(false);
            this.LetGoOfGrappled("Aborted");
         }
      }

      if (!this.attackStarted) {
         this.setInitiateAttack(false);
         this.setAttackType((String)null);
      }

      if ((this.isRunning() || this.isSprinting()) && this.getDeferredMovement(tempo).getLengthSquared() > 0.0F) {
         this.runningTime += GameTime.getInstance().getThirtyFPSMultiplier();
      } else {
         this.runningTime = 0.0F;
      }

      if (this.getLastCollideTime() > 0.0F) {
         this.setLastCollideTime(this.getLastCollideTime() - GameTime.getInstance().getThirtyFPSMultiplier());
      }

      this.updateDeathDragDown();
      this.updateGodModeKey();
      this.networkAI.update();
      this.doDeferredMovement();
      if (GameServer.bServer) {
         this.vehicle4testCollision = null;
      } else if (GameClient.bClient) {
         if (this.vehicle4testCollision != null) {
            if (!this.isLocal()) {
               this.vehicle4testCollision.updateHitByVehicle(this);
            }

            this.vehicle4testCollision = null;
         }
      } else {
         this.updateHitByVehicle();
         this.vehicle4testCollision = null;
      }

      this.updateEquippedItemSounds();
      this.updateEmitter();
      if (!this.isAnimal()) {
         this.updateMechanicsItems();
         this.updateHeavyBreathing();
         this.updateTemperatureCheck();
         this.updateAimingStance();
         if (SystemDisabler.doCharacterStats) {
            this.nutrition.update();
         }

         this.fitness.update();
         this.updateVisionEffectTargets();
      }

      this.updateSoundListener();
      SafetySystemManager.update(this);
      if (!GameClient.bClient && !GameServer.bServer && this.bDeathFinished) {
         return false;
      } else {
         if (!GameClient.bClient && this.getCurrentBuildingDef() != null && !this.isInvisible()) {
            this.getCurrentBuildingDef().setHasBeenVisited(true);
         }

         if (this.checkSafehouse > 0 && GameServer.bServer) {
            --this.checkSafehouse;
            if (this.checkSafehouse == 0) {
               this.checkSafehouse = 200;
               SafeHouse var1 = SafeHouse.isSafeHouse(this.getCurrentSquare(), (String)null, false);
               if (var1 != null) {
                  var1.updateSafehouse(this);
                  var1.checkTrespass(this);
                  this.updateDisguisedState();
               }
            }
         }

         if (this.bRemote && this.TimeSinceLastNetData > 600) {
            IsoWorld.instance.CurrentCell.getObjectList().remove(this);
            if (this.movingSq != null) {
               this.movingSq.getMovingObjects().remove(this);
            }
         }

         this.TimeSinceLastNetData += (int)GameTime.instance.getMultiplier();
         this.TimeSinceOpenDoor += GameTime.instance.getMultiplier();
         this.TimeSinceCloseDoor += GameTime.instance.getMultiplier();
         this.lastTargeted += GameTime.instance.getMultiplier();
         this.targetedByZombie = false;
         this.checkActionGroup();
         if (this.isJustMoved() && !this.isNPC && !this.hasPath()) {
            if (!GameServer.bServer && UIManager.getSpeedControls().getCurrentGameSpeed() > 1) {
               UIManager.getSpeedControls().SetCurrentGameSpeed(1);
            }
         } else if (!GameClient.bClient && this.stats.endurance < this.stats.endurancedanger && Rand.Next((int)(300.0F * GameTime.instance.getInvMultiplier())) == 0) {
            if (GameServer.bServer) {
               GameServer.addXp(this, PerkFactory.Perks.Fitness, 1.0F);
            } else {
               this.xp.AddXP(PerkFactory.Perks.Fitness, 1.0F);
            }
         }

         float var13 = 1.0F;
         float var2 = 0.0F;
         if (this.isJustMoved() && !this.isNPC) {
            if (!this.isRunning() && !this.isSprinting()) {
               var2 = 1.0F;
            } else {
               var2 = 1.5F;
            }

            if (!GameClient.bClient && (double)var2 > 0.0 && !this.isNPC) {
               LuaEventManager.triggerEvent("OnPlayerMove", this);
            }
         }

         if (this.updateRemotePlayer()) {
            if (this.updateWhileDead()) {
               return true;
            } else {
               this.updateHeartSound();
               this.updateDraggingCorpseSounds();
               this.checkIsNearWall();
               this.checkIsNearVehicle();
               this.updateExt();
               this.setBeenMovingSprinting();
               this.updateAimingDelay();
               return true;
            }
         } else {
            assert !GameServer.bServer;

            assert !this.bRemote;

            assert !GameClient.bClient || this.isLocalPlayer();

            IsoCamera.setCameraCharacter(this);
            instance = this;
            if (this.isLocalPlayer()) {
               IsoCamera.cameras[this.PlayerIndex].update();
               if (UIManager.getMoodleUI((double)this.PlayerIndex) != null) {
                  UIManager.getMoodleUI((double)this.PlayerIndex).setCharacter(this);
               }
            }

            if (this.closestZombie > 1.2F) {
               this.slowTimer = -1.0F;
               this.slowFactor = 0.0F;
            }

            this.ContextPanic -= 1.5F * GameTime.instance.getTimeDelta();
            if (this.ContextPanic < 0.0F) {
               this.ContextPanic = 0.0F;
            }

            this.lastSeenZombieTime += (double)(GameTime.instance.getGameWorldSecondsSinceLastUpdate() / 60.0F / 60.0F);
            LuaEventManager.triggerEvent("OnPlayerUpdate", this);
            if (this.pressedMovement(false)) {
               this.ContextPanic = 0.0F;
               this.ticksSincePressedMovement = 0.0F;
            } else {
               this.ticksSincePressedMovement += GameTime.getInstance().getThirtyFPSMultiplier();
            }

            this.setVariable("pressedMovement", this.pressedMovement(true));
            if (this.updateWhileDead()) {
               return true;
            } else {
               this.updateHeartSound();
               this.updateDraggingCorpseSounds();
               this.updateVocalProperties();
               this.updateEquippedBaggageContainer();
               this.updateWorldAmbiance();
               this.updateSneakKey();
               this.checkIsNearWall();
               this.checkIsNearVehicle();
               this.updateExt();
               this.updateInteractKeyPanic();
               this.updateMusicIntensityEvents();
               this.updateMusicThreatStatuses();
               if (this.isAsleep()) {
                  this.m_isPlayerMoving = false;
               }

               if (this.getVehicle() != null && this.getVehicle().isDriver(this) && this.getVehicle().hasHorn() && Core.getInstance().getKey("Shout") == Core.getInstance().getKey("VehicleHorn")) {
               }

               if (!this.getIgnoreMovement() && !this.isAsleep()) {
                  if (this.checkActionsBlockingMovement()) {
                     if (this.getVehicle() != null && this.getVehicle().getDriver() == this && this.getVehicle().getController() != null) {
                        this.getVehicle().getController().clientControls.reset();
                        this.getVehicle().updatePhysics();
                     }

                     this.clearUseKeyVariables();
                     return true;
                  } else {
                     this.enterExitVehicle();
                     this.checkActionGroup();
                     this.checkReloading();
                     this.checkWalkTo();
                     if (this.checkActionsBlockingMovement()) {
                        this.clearUseKeyVariables();
                        return true;
                     } else if (this.getVehicle() != null) {
                        this.updateWhileInVehicle();
                        return true;
                     } else {
                        this.checkVehicleContainers();
                        this.setCollidable(true);
                        this.updateCursorVisibility();
                        this.bSeenThisFrame = false;
                        this.bCouldBeSeenThisFrame = false;
                        if (IsoCamera.getCameraCharacter() == null && GameClient.bClient) {
                           IsoCamera.setCameraCharacter(instance);
                        }

                        if (this.updateUseKey()) {
                           return true;
                        } else {
                           this.updateEnableModelsKey();
                           this.updateChangeCharacterKey();
                           boolean var3 = false;
                           boolean var4 = false;
                           boolean var5 = false;
                           this.setRunning(false);
                           this.setSprinting(false);
                           this.useChargeTime = this.chargeTime;
                           if (!this.isBlockMovement() && !this.isNPC) {
                              if (!this.isCharging && !this.isChargingLT) {
                                 this.chargeTime = 0.0F;
                              } else {
                                 this.chargeTime += 1.0F * GameTime.instance.getMultiplier();
                              }

                              this.UpdateInputState(this.inputState);
                              var4 = this.inputState.bMelee;
                              var5 = this.inputState.bGrapple;
                              var3 = this.inputState.isAttacking;
                              this.setRunning(this.inputState.bRunning);
                              this.setSprinting(this.inputState.bSprinting);
                              if (this.isSprinting() && !this.isJustMoved()) {
                                 this.setSprinting(false);
                              }

                              if (this.isSprinting()) {
                                 this.setRunning(false);
                              }

                              if (this.inputState.bSprinting && !this.isSprinting()) {
                                 this.setRunning(true);
                              }

                              if (this.isPickingUpBody() || this.isPuttingDownBody()) {
                                 this.inputState.isAiming = false;
                              }

                              this.setIsAiming(this.inputState.isAiming);
                              this.isCharging = this.inputState.isCharging;
                              this.isChargingLT = this.inputState.isChargingLT;
                              this.updateMovementRates();
                              if (this.isAiming()) {
                                 this.StopAllActionQueueAiming();
                              }

                              if (var3) {
                                 this.setIsAiming(true);
                              }

                              this.Waiting = false;
                              if (this.isAiming()) {
                                 this.setMoving(false);
                                 this.setRunning(false);
                                 this.setSprinting(false);
                              }

                              if (this.isFarming()) {
                                 this.setIsAiming(false);
                              }

                              ++this.TicksSinceSeenZombie;
                           }

                           if (this.playerMoveDir.x == 0.0F && this.playerMoveDir.y == 0.0F) {
                              this.setForceRun(false);
                              this.setForceSprint(false);
                           }

                           this.movementLastFrame.x = this.playerMoveDir.x;
                           this.movementLastFrame.y = this.playerMoveDir.y;
                           if (this.stateMachine.getCurrent() != StaggerBackState.instance() && this.stateMachine.getCurrent() != FakeDeadZombieState.instance() && UIManager.speedControls != null) {
                              if (GameKeyboard.isKeyDown(88) && Translator.debug) {
                                 Translator.loadFiles();
                              }

                              this.setJustMoved(false);
                              MoveVars var6 = s_moveVars;
                              this.updateMovementFromInput(var6);
                              if (!this.JustMoved && this.hasPath() && this.getPathFindBehavior2().shouldBeMoving()) {
                                 this.setJustMoved(true);
                              }

                              float var7 = var6.strafeX;
                              float var8 = var6.strafeY;
                              this.updateAimingDelay();
                              this.setBeenMovingSprinting();
                              var13 *= var2;
                              if (var13 > 1.0F) {
                                 var13 *= this.getSprintMod();
                              }

                              if (var13 > 1.0F && this.Traits.Athletic.isSet()) {
                                 var13 *= 1.2F;
                              }

                              if (var13 > 1.0F) {
                                 if (this.Traits.Overweight.isSet()) {
                                    var13 *= 0.99F;
                                 }

                                 if (this.Traits.Obese.isSet()) {
                                    var13 *= 0.85F;
                                 }

                                 if (this.getNutrition().getWeight() > 120.0) {
                                    var13 *= 0.97F;
                                 }

                                 if (this.Traits.OutOfShape.isSet()) {
                                    var13 *= 0.99F;
                                 }

                                 if (this.Traits.Unfit.isSet()) {
                                    var13 *= 0.8F;
                                 }
                              }

                              if (!this.isAnimal()) {
                                 this.updateEndurance();
                              }

                              if (this.isAiming() && this.isJustMoved()) {
                                 var13 *= 0.7F;
                              }

                              if (this.isAiming()) {
                                 var13 *= this.getNimbleMod();
                              }

                              this.isWalking = false;
                              if (var13 > 0.0F && !this.isNPC) {
                                 this.isWalking = true;
                              }

                              if (this.isJustMoved()) {
                                 this.sprite.Animate = true;
                              }

                              if (this.isNPC) {
                                 var4 = this.ai.doUpdatePlayerControls(var4);
                              }

                              this.m_meleePressed = var4;
                              this.m_grapplePressed = var5;
                              if (!this.m_grapplePressed) {
                                 this.setDoGrapple(false);
                                 this.m_canLetGoOfGrappled = true;
                              }

                              if (!this.bPressContext && this.isDoGrappleLetGoAfterContextKeyIsReleased()) {
                                 this.setDoGrapple(false);
                                 this.setDoGrappleLetGo();
                              }

                              if (var4) {
                                 if (!this.m_lastAttackWasHandToHand) {
                                    this.setMeleeDelay(Math.min(this.getMeleeDelay(), 2.0F));
                                 }

                                 if (!this.bBannedAttacking && this.isAuthorizedHandToHand() && this.CanAttack() && this.getMeleeDelay() <= 0.0F) {
                                    this.setDoShove(true);
                                    this.setDoGrapple(false);
                                    if (!this.isCharging && !this.isChargingLT) {
                                       this.setIsAiming(false);
                                    }

                                    this.AttemptAttack(this.useChargeTime);
                                    this.useChargeTime = 0.0F;
                                    this.chargeTime = 0.0F;
                                 }
                              } else if (var5) {
                                 if (!this.m_lastAttackWasHandToHand) {
                                    this.setMeleeDelay(Math.min(this.getMeleeDelay(), 2.0F));
                                 }

                                 if (!this.bBannedAttacking && this.isAuthorizedHandToHand() && this.CanAttack() && this.getMeleeDelay() <= 0.0F) {
                                    if (!this.isGrappling()) {
                                       this.m_canLetGoOfGrappled = false;
                                       this.setDoGrapple(true);
                                       if (!this.isCharging && !this.isChargingLT) {
                                          this.setIsAiming(false);
                                       }
                                    } else if (this.m_canLetGoOfGrappled) {
                                       if (this.bPressContext) {
                                          this.setDoGrappleLetGoAfterContextKeyIsReleased(true);
                                       } else {
                                          this.setDoGrapple(false);
                                          this.setDoGrappleLetGo();
                                       }
                                    }

                                    this.useChargeTime = 0.0F;
                                    this.chargeTime = 0.0F;
                                 }
                              } else if (this.isAiming() && this.CanAttack()) {
                                 if (this.DragCharacter != null) {
                                    this.DragObject = null;
                                    this.DragCharacter.Dragging = false;
                                    this.DragCharacter = null;
                                 }

                                 if (var3 && !this.bBannedAttacking) {
                                    this.sprite.Animate = true;
                                    if (this.getRecoilDelay() <= 0.0F && this.getMeleeDelay() <= 0.0F) {
                                       this.AttemptAttack(this.useChargeTime);
                                    }

                                    this.useChargeTime = 0.0F;
                                    this.chargeTime = 0.0F;
                                 }
                              }

                              if (this.isAiming() && !this.isNPC && !this.isCurrentState(FishingState.instance()) && !this.isFarming()) {
                                 if (this.JoypadBind != -1 && !this.bJoypadMovementActive) {
                                    if (this.getForwardDirection().getLengthSquared() > 0.0F) {
                                       this.DirectionFromVector(this.getForwardDirection());
                                    }
                                 } else {
                                    Vector2 var9 = tempVector2.set(0.0F, 0.0F);
                                    if (GameWindow.ActivatedJoyPad != null && this.JoypadBind != -1) {
                                       this.getControllerAimDir(var9);
                                    } else {
                                       this.getMouseAimVector(var9);
                                    }

                                    if (var9.getLengthSquared() > 0.0F) {
                                       this.DirectionFromVector(var9);
                                       this.setForwardDirection(var9);
                                    }
                                 }

                                 var6.NewFacing = this.dir;
                              }

                              if (this.getForwardDirection().x == 0.0F && this.getForwardDirection().y == 0.0F) {
                                 this.setForwardDirection(this.dir.ToVector());
                              }

                              if (this.lastAngle.x != this.getForwardDirection().x || this.lastAngle.y != this.getForwardDirection().y) {
                                 this.lastAngle.x = this.getForwardDirection().x;
                                 this.lastAngle.y = this.getForwardDirection().y;
                                 this.dirtyRecalcGridStackTime = 2.0F;
                              }

                              this.stats.endurance = PZMath.clamp(this.stats.endurance, 0.0F, 1.0F);
                              AnimationPlayer var14 = this.getAnimationPlayer();
                              if (var14 != null && var14.isReady()) {
                                 float var10 = var14.getAngle() + var14.getTwistAngle();
                                 this.dir = IsoDirections.fromAngle(tempVector2.setLengthAndDirection(var10, 1.0F));
                              } else if (!this.bFalling && !this.isAiming() && !var3) {
                                 this.dir = var6.NewFacing;
                              }

                              if (this.isAiming() && (GameWindow.ActivatedJoyPad == null || this.JoypadBind == -1)) {
                                 this.playerMoveDir.x = var6.moveX;
                                 this.playerMoveDir.y = var6.moveY;
                              }

                              if (!this.isAiming() && this.isJustMoved()) {
                                 this.playerMoveDir.x = this.getForwardDirection().x;
                                 this.playerMoveDir.y = this.getForwardDirection().y;
                              }

                              if (this.isJustMoved()) {
                                 if (this.isSprinting()) {
                                    this.CurrentSpeed = 1.5F;
                                 } else if (this.isRunning()) {
                                    this.CurrentSpeed = 1.0F;
                                 } else {
                                    this.CurrentSpeed = 0.5F;
                                 }
                              } else {
                                 this.CurrentSpeed = 0.0F;
                              }

                              boolean var15 = this.IsInMeleeAttack();
                              if (!this.CharacterActions.isEmpty()) {
                                 BaseAction var11 = (BaseAction)this.CharacterActions.get(0);
                                 if (var11.overrideAnimation) {
                                    var15 = true;
                                 }
                              }

                              if (!var15 && !this.isForceOverrideAnim()) {
                                 if (this.getPath2() == null) {
                                    if (this.CurrentSpeed > 0.0F && (!this.bClimbing || this.lastFallSpeed > 0.0F)) {
                                       if (!this.isRunning() && !this.isSprinting()) {
                                          this.StopAllActionQueueWalking();
                                       } else {
                                          this.StopAllActionQueueRunning();
                                       }
                                    }
                                 } else {
                                    this.StopAllActionQueueWalking();
                                 }
                              }

                              if (this.slowTimer > 0.0F) {
                                 this.slowTimer -= GameTime.instance.getRealworldSecondsSinceLastUpdate();
                                 this.CurrentSpeed *= 1.0F - this.slowFactor;
                                 this.slowFactor -= GameTime.instance.getMultiplier() / 100.0F;
                                 if (this.slowFactor < 0.0F) {
                                    this.slowFactor = 0.0F;
                                 }
                              } else {
                                 this.slowFactor = 0.0F;
                              }

                              this.playerMoveDir.setLength(this.CurrentSpeed);
                              if (this.playerMoveDir.x != 0.0F || this.playerMoveDir.y != 0.0F) {
                                 this.dirtyRecalcGridStackTime = 10.0F;
                              }

                              if (this.getPath2() != null && this.current != this.last) {
                                 this.dirtyRecalcGridStackTime = 10.0F;
                              }

                              this.closestZombie = 1000000.0F;
                              this.weight = 0.3F;
                              this.separate();
                              if (!this.isAnimal()) {
                                 this.updateSleepingPillsTaken();
                                 this.updateTorchStrength();
                              }

                              if (this.isNPC) {
                                 this.ai.postUpdate();
                                 var7 = this.ai.brain.HumanControlVars.strafeX;
                                 var8 = this.ai.brain.HumanControlVars.strafeY;
                              }

                              this.m_isPlayerMoving = this.isJustMoved() || this.hasPath() && this.getPathFindBehavior2().shouldBeMoving();
                              boolean var16 = this.isInTrees();
                              float var12;
                              if (var16) {
                                 var12 = "parkranger".equals(this.getDescriptor().getProfession()) ? 1.3F : 1.0F;
                                 var12 = "lumberjack".equals(this.getDescriptor().getProfession()) ? 1.15F : var12;
                                 if (this.isRunning()) {
                                    var12 *= 1.1F;
                                 }

                                 this.setVariable("WalkSpeedTrees", var12);
                              }

                              if ((var16 || this.m_walkSpeed < 0.4F || this.m_walkInjury > 0.5F) && this.isSprinting() && !this.isGhostMode()) {
                                 if (this.runSpeedModifier < 1.0F) {
                                    this.setMoodleCantSprint(true);
                                 }

                                 this.setSprinting(false);
                                 this.setRunning(true);
                                 if (this.isInTreesNoBush()) {
                                    this.setForceSprint(false);
                                    this.setBumpType("left");
                                    this.setVariable("BumpDone", false);
                                    this.setVariable("BumpFall", true);
                                    this.setVariable("TripObstacleType", "tree");
                                    this.getActionContext().reportEvent("wasBumped");
                                 }
                              }

                              this.m_deltaX = var7;
                              this.m_deltaY = var8;
                              this.m_windspeed = ClimateManager.getInstance().getWindSpeedMovement();
                              var12 = this.getForwardDirection().getDirectionNeg();
                              this.m_windForce = ClimateManager.getInstance().getWindForceMovement(this, var12);
                              return true;
                           } else {
                              return true;
                           }
                        }
                     }
                  }
               } else {
                  this.clearUseKeyVariables();
                  return true;
               }
            }
         }
      }
   }

   private void setDoGrappleLetGoAfterContextKeyIsReleased(boolean var1) {
      this.m_letGoAfterContextIsReleased = var1;
   }

   private boolean isDoGrappleLetGoAfterContextKeyIsReleased() {
      return this.m_letGoAfterContextIsReleased;
   }

   private void updateMovementFromInput(MoveVars var1) {
      var1.moveX = 0.0F;
      var1.moveY = 0.0F;
      var1.strafeX = 0.0F;
      var1.strafeY = 0.0F;
      var1.NewFacing = this.dir;
      if (!TutorialManager.instance.StealControl) {
         if (!this.isBlockMovement()) {
            if (!this.isPickingUpBody() && !this.isPuttingDownBody()) {
               if (!this.isNPC) {
                  if (!MPDebugAI.updateMovementFromInput(this, var1)) {
                     if (!(this.fallTime > 2.0F)) {
                        if (GameWindow.ActivatedJoyPad != null && this.JoypadBind != -1) {
                           this.updateMovementFromJoypad(var1);
                        }

                        if (this.PlayerIndex == 0 && this.JoypadBind == -1) {
                           this.updateMovementFromKeyboardMouse(var1);
                        }

                        if (this.isJustMoved()) {
                           this.getForwardDirection().normalize();
                           UIManager.speedControls.SetCurrentGameSpeed(1);
                        }

                     }
                  }
               }
            }
         }
      }
   }

   private void randomizeDrunkenMovement(MoveVars var1, boolean var2) {
      int var3 = Rand.NextBool(20) ? 2 : 1;
      boolean var4 = false;
      if (this.drunkMoveVars.NewFacing == null) {
         this.drunkMoveVars.NewFacing = var1.NewFacing;
      }

      int var6 = (var1.NewFacing.index() - this.drunkMoveVars.NewFacing.index() + 4) % 8 - 4;
      var6 = var6 < -4 ? var6 + 4 : var6;
      boolean var5 = false;
      if (Math.abs(var6) >= 2) {
         if (var6 < 0) {
            var5 = true;
         }
      } else if (Rand.NextBool(2)) {
         var5 = true;
      }

      if (var5) {
         var1.NewFacing = this.drunkMoveVars.NewFacing.RotRight(var3);
      } else {
         var1.NewFacing = this.drunkMoveVars.NewFacing.RotLeft(var3);
      }

      if (var2) {
         switch (var1.NewFacing) {
            case N:
               var1.moveX = 1.0F;
               var1.moveY = -1.0F;
               break;
            case NW:
               var1.moveX = 0.0F;
               var1.moveY = -1.0F;
               break;
            case W:
               var1.moveX = -1.0F;
               var1.moveY = -1.0F;
               break;
            case SW:
               var1.moveX = -1.0F;
               var1.moveY = 0.0F;
               break;
            case S:
               var1.moveX = -1.0F;
               var1.moveY = 1.0F;
               break;
            case SE:
               var1.moveX = 0.0F;
               var1.moveY = 1.0F;
               break;
            case E:
               var1.moveX = 1.0F;
               var1.moveY = 1.0F;
               break;
            case NE:
               var1.moveX = 1.0F;
               var1.moveY = 0.0F;
         }

      } else {
         switch (var1.NewFacing) {
            case N:
               var1.moveX = 0.0F;
               var1.moveY = -1.0F;
               break;
            case NW:
               var1.moveX = -1.0F;
               var1.moveY = -1.0F;
               break;
            case W:
               var1.moveX = -1.0F;
               var1.moveY = 0.0F;
               break;
            case SW:
               var1.moveX = -1.0F;
               var1.moveY = 1.0F;
               break;
            case S:
               var1.moveX = 0.0F;
               var1.moveY = 1.0F;
               break;
            case SE:
               var1.moveX = 1.0F;
               var1.moveY = 1.0F;
               break;
            case E:
               var1.moveX = 1.0F;
               var1.moveY = 0.0F;
               break;
            case NE:
               var1.moveX = 1.0F;
               var1.moveY = -1.0F;
         }

      }
   }

   private void adjustMovementForDrunks(MoveVars var1, boolean var2) {
      if (this.getMoodles().getMoodleLevel(MoodleType.Drunk) > 1) {
         this.drunkDelayCommandTimer += GameTime.getInstance().getMultiplier();
         int var3 = Rand.AdjustForFramerate(4 * this.getMoodles().getMoodleLevel(MoodleType.Drunk));
         if ((float)var3 > this.drunkDelayCommandTimer) {
            var1.moveY = this.drunkMoveVars.moveY;
            var1.moveX = this.drunkMoveVars.moveX;
            var1.NewFacing = this.drunkMoveVars.NewFacing;
         } else {
            this.drunkDelayCommandTimer = 0.0F;
            if ((var1.moveX != 0.0F || var1.moveY != 0.0F) && (float)Rand.Next(160) < this.getStats().Drunkenness) {
               this.randomizeDrunkenMovement(var1, var2);
            }

            if ((this.isSprinting() || this.isRunning()) && (float)Rand.Next(2000) < this.getStats().Drunkenness * (float)this.getMoodles().getMoodleLevel(MoodleType.Drunk)) {
               this.setVariable("BumpDone", false);
               this.clearVariable("BumpFallType");
               this.setBumpType("trippingFromSprint");
               this.setBumpFall(true);
               this.setBumpFallType(Rand.NextBool(5) ? "pushedFront" : "pushedBehind");
               this.getActionContext().reportEvent("wasBumped");
            }
         }
      } else {
         this.drunkDelayCommandTimer = 0.0F;
      }

      this.drunkMoveVars.moveY = var1.moveY;
      this.drunkMoveVars.moveX = var1.moveX;
      this.drunkMoveVars.NewFacing = var1.NewFacing;
   }

   private void updateMovementFromJoypad(MoveVars var1) {
      this.playerMoveDir.x = 0.0F;
      this.playerMoveDir.y = 0.0F;
      this.getJoypadAimVector(tempVector2);
      float var2 = tempVector2.x;
      float var3 = tempVector2.y;
      Vector2 var4 = this.getJoypadMoveVector(tempVector2);
      if (var4.getLength() > 1.0F) {
         var4.setLength(1.0F);
      }

      if (this.isAutoWalk()) {
         if (var4.getLengthSquared() < 0.25F) {
            var4.set(this.getAutoWalkDirection());
         } else {
            this.setAutoWalkDirection(var4);
            this.getAutoWalkDirection().normalize();
         }
      }

      float var5 = var4.x;
      float var6 = var4.y;
      Vector2 var10000;
      if (Math.abs(var5) > 0.0F) {
         var10000 = this.playerMoveDir;
         var10000.x += 0.04F * var5;
         var10000 = this.playerMoveDir;
         var10000.y -= 0.04F * var5;
         this.setJustMoved(true);
      }

      if (Math.abs(var6) > 0.0F) {
         var10000 = this.playerMoveDir;
         var10000.y += 0.04F * var6;
         var10000 = this.playerMoveDir;
         var10000.x += 0.04F * var6;
         this.setJustMoved(true);
      }

      this.playerMoveDir.setLength(0.05F * (float)Math.pow((double)var4.getLength(), 9.0));
      if (var2 == 0.0F && var3 == 0.0F) {
         if ((var5 != 0.0F || var6 != 0.0F) && this.playerMoveDir.getLengthSquared() > 0.0F) {
            var4 = tempVector2.set(this.playerMoveDir);
            var4.normalize();
            var1.NewFacing = IsoDirections.fromAngle(var4);
         }
      } else {
         Vector2 var7 = tempVector2.set(var2, var3);
         var7.normalize();
         var1.NewFacing = IsoDirections.fromAngle(var7);
      }

      var1.moveX = this.playerMoveDir.x;
      var1.moveY = this.playerMoveDir.y;
      this.playerMoveDir.x = var1.moveX;
      this.playerMoveDir.y = var1.moveY;
      PathFindBehavior2 var9 = this.getPathFindBehavior2();
      if (this.playerMoveDir.x == 0.0F && this.playerMoveDir.y == 0.0F && this.getPath2() != null && var9.isStrafing() && !var9.bStopping) {
         this.playerMoveDir.set(var9.getTargetX() - this.getX(), var9.getTargetY() - this.getY());
         this.playerMoveDir.normalize();
      }

      if (this.playerMoveDir.x != 0.0F || this.playerMoveDir.y != 0.0F) {
         if (this.isStrafing()) {
            tempo.set(this.playerMoveDir.x, -this.playerMoveDir.y);
            tempo.normalize();
            float var8 = this.legsSprite.modelSlot.model.AnimPlayer.getRenderedAngle();
            if ((double)var8 > 6.283185307179586) {
               var8 -= 6.2831855F;
            }

            if (var8 < 0.0F) {
               var8 += 6.2831855F;
            }

            tempo.rotate(var8);
            var1.strafeX = tempo.x;
            var1.strafeY = tempo.y;
            this.m_IPX = this.playerMoveDir.x;
            this.m_IPY = this.playerMoveDir.y;
         } else {
            var1.moveX = this.playerMoveDir.x;
            var1.moveY = this.playerMoveDir.y;
            tempo.set(this.playerMoveDir);
            tempo.normalize();
            this.setForwardDirection(tempo);
         }
      }

   }

   private void updateMovementFromKeyboardMouse(MoveVars var1) {
      int var2 = GameKeyboard.whichKeyDown("Left");
      int var3 = GameKeyboard.whichKeyDown("Right");
      int var4 = GameKeyboard.whichKeyDown("Forward");
      int var5 = GameKeyboard.whichKeyDown("Backward");
      boolean var6 = var2 > 0;
      boolean var7 = var3 > 0;
      boolean var8 = var4 > 0;
      boolean var9 = var5 > 0;
      if (!var6 && !var7 && !var8 && !var9 || var2 != 30 && var3 != 30 && var4 != 30 && var5 != 30 || !GameKeyboard.isKeyDown(29) && !GameKeyboard.isKeyDown(157) || !UIManager.isMouseOverInventory() || !Core.getInstance().isSelectingAll()) {
         if (!this.isIgnoreInputsForDirection()) {
            if (Core.bAltMoveMethod) {
               if (var6 && !var7) {
                  var1.moveX -= 0.04F;
                  var1.NewFacing = IsoDirections.W;
               }

               if (var7 && !var6) {
                  var1.moveX += 0.04F;
                  var1.NewFacing = IsoDirections.E;
               }

               if (var8 && !var9) {
                  var1.moveY -= 0.04F;
                  if (var1.NewFacing == IsoDirections.W) {
                     var1.NewFacing = IsoDirections.NW;
                  } else if (var1.NewFacing == IsoDirections.E) {
                     var1.NewFacing = IsoDirections.NE;
                  } else {
                     var1.NewFacing = IsoDirections.N;
                  }
               }

               if (var9 && !var8) {
                  var1.moveY += 0.04F;
                  if (var1.NewFacing == IsoDirections.W) {
                     var1.NewFacing = IsoDirections.SW;
                  } else if (var1.NewFacing == IsoDirections.E) {
                     var1.NewFacing = IsoDirections.SE;
                  } else {
                     var1.NewFacing = IsoDirections.S;
                  }
               }
            } else {
               if (var6) {
                  var1.moveX = -1.0F;
               } else if (var7) {
                  var1.moveX = 1.0F;
               }

               if (var8) {
                  var1.moveY = 1.0F;
               } else if (var9) {
                  var1.moveY = -1.0F;
               }

               if (var1.moveX != 0.0F || var1.moveY != 0.0F) {
                  tempo.set(var1.moveX, var1.moveY);
                  tempo.normalize();
                  var1.NewFacing = IsoDirections.fromAngle(tempo);
               }

               if (this.isAnimatingBackwards()) {
                  var1.moveX = -var1.moveX;
                  var1.moveY = -var1.moveY;
               }
            }
         }

         PathFindBehavior2 var10 = this.getPathFindBehavior2();
         if (var1.moveX == 0.0F && var1.moveY == 0.0F && this.getPath2() != null && (var10.isStrafing() || this.isAiming()) && !var10.bStopping) {
            Vector2 var11 = tempo.set(var10.getTargetX() - this.getX(), var10.getTargetY() - this.getY());
            Vector2 var12 = tempo2.set(-1.0F, 0.0F);
            float var13 = 1.0F;
            float var14 = var11.dot(var12);
            float var15 = var14 / var13;
            var12 = tempo2.set(0.0F, -1.0F);
            var14 = var11.dot(var12);
            float var16 = var14 / var13;
            tempo.set(var16, var15);
            tempo.normalize();
            tempo.rotate(0.7853982F);
            var1.moveX = tempo.x;
            var1.moveY = tempo.y;
         }

         if (var1.moveX != 0.0F || var1.moveY != 0.0F) {
            if (this.stateMachine.getCurrent() == PathFindState.instance()) {
               this.setDefaultState();
            }

            this.setJustMoved(true);
            this.setMoveDelta(1.0F);
            if (this.isStrafing()) {
               tempo.set(var1.moveX, var1.moveY);
               tempo.normalize();
               float var17 = this.legsSprite.modelSlot.model.AnimPlayer.getRenderedAngle();
               var17 += 0.7853982F;
               if ((double)var17 > 6.283185307179586) {
                  var17 -= 6.2831855F;
               }

               if (var17 < 0.0F) {
                  var17 += 6.2831855F;
               }

               tempo.rotate(var17);
               var1.strafeX = tempo.x;
               var1.strafeY = tempo.y;
               this.m_IPX = var1.moveX;
               this.m_IPY = var1.moveY;
            } else {
               tempo.set(var1.moveX, -var1.moveY);
               tempo.normalize();
               tempo.rotate(-0.7853982F);
               this.setForwardDirection(tempo);
            }
         }

      }
   }

   private void updateAimingStance() {
      if (this.isVariable("LeftHandMask", "RaiseHand")) {
         this.clearVariable("LeftHandMask");
      }

      if (this.isAiming() && !this.isCurrentState(SwipeStatePlayer.instance())) {
         HandWeapon var1 = (HandWeapon)Type.tryCastTo(this.getPrimaryHandItem(), HandWeapon.class);
         var1 = var1 == null ? this.bareHands : var1;
         if (this.isLocal()) {
            CombatManager.targetReticleMode = var1.isAimedFirearm() ? 1 : 0;
         }

         if (var1.hasComponent(ComponentType.FluidContainer) && var1.getFluidContainer().getRainCatcher() > 0.0F && !var1.getFluidContainer().isEmpty()) {
            var1.getFluidContainer().Empty();
         }

         CombatManager.getInstance().calcValidTargets(this, var1, true, s_targetsProne, s_targetsStanding);
         HitInfo var2 = s_targetsStanding.isEmpty() ? null : (HitInfo)s_targetsStanding.get(0);
         HitInfo var3 = s_targetsProne.isEmpty() ? null : (HitInfo)s_targetsProne.get(0);
         if (CombatManager.getInstance().isProneTargetBetter(this, var2, var3)) {
            var2 = null;
         }

         boolean var4 = this.isPerformingHostileAnimation();
         if (!var4) {
            this.setAimAtFloor(false);
         }

         if (var2 != null) {
            if (!var4) {
               this.setAimAtFloor(false);
            }
         } else if (var3 != null && !var4) {
            this.setAimAtFloor(true);
         }

         if (var2 != null) {
            boolean var5 = !this.isPerformingAttackAnimation() && var1.getSwingAnim() != null && var1.CloseKillMove != null && var2.distSq < var1.getMinRange() * var1.getMinRange();
            if (var5 && (this.getSecondaryHandItem() == null || this.getSecondaryHandItem().getItemReplacementSecondHand() == null)) {
               this.setVariable("LeftHandMask", "RaiseHand");
            }
         }

         CombatManager.getInstance().hitInfoPool.release((List)s_targetsStanding);
         CombatManager.getInstance().hitInfoPool.release((List)s_targetsProne);
         s_targetsStanding.clear();
         s_targetsProne.clear();
      }
   }

   protected void calculateStats() {
      if (!this.bRemote) {
         super.calculateStats();
      }
   }

   protected void updateStats_Sleeping() {
      float var1 = 2.0F;
      if (allPlayersAsleep()) {
         var1 *= GameTime.instance.getDeltaMinutesPerDay();
      }

      Stats var10000 = this.stats;
      var10000.endurance = (float)((double)var10000.endurance + ZomboidGlobals.ImobileEnduranceReduce * SandboxOptions.instance.getEnduranceRegenMultiplier() * (double)this.getRecoveryMod() * (double)GameTime.instance.getMultiplier() * (double)var1);
      if (this.stats.endurance > 1.0F) {
         this.stats.endurance = 1.0F;
      }

      float var2;
      float var3;
      if (this.stats.fatigue > 0.0F) {
         var2 = 1.0F;
         if (this.Traits.Insomniac.isSet()) {
            var2 *= 0.5F;
         }

         if (this.Traits.NightOwl.isSet()) {
            var2 *= 1.4F;
         }

         var3 = 1.0F;
         if ("averageBedPillow".equals(this.getBedType())) {
            var3 = 1.05F;
         }

         if ("goodBed".equals(this.getBedType())) {
            var3 = 1.1F;
         } else if ("goodBedPillow".equals(this.getBedType())) {
            var3 = 1.15F;
         } else if ("badBed".equals(this.getBedType())) {
            var3 = 0.9F;
         } else if ("badBedPillow".equals(this.getBedType())) {
            var3 = 0.95F;
         } else if ("floor".equals(this.getBedType())) {
            var3 = 0.6F;
         } else if ("floorPillow".equals(this.getBedType())) {
            var3 = 0.75F;
         }

         float var4 = 1.0F / GameTime.instance.getMinutesPerDay() / 60.0F * GameTime.instance.getMultiplier() / 2.0F;
         this.timeOfSleep += var4;
         if (this.timeOfSleep > this.delayToActuallySleep) {
            float var5 = 1.0F;
            if (this.Traits.NeedsLessSleep.isSet()) {
               var5 *= 0.75F;
            } else if (this.Traits.NeedsMoreSleep.isSet()) {
               var5 *= 1.18F;
            }

            float var6 = 1.0F;
            if (this.stats.fatigue <= 0.3F) {
               var6 = 7.0F * var5;
               var10000 = this.stats;
               var10000.fatigue -= var4 / var6 * 0.3F * var2 * var3;
            } else {
               var6 = 5.0F * var5;
               var10000 = this.stats;
               var10000.fatigue -= var4 / var6 * 0.7F * var2 * var3;
            }
         }

         if (this.stats.fatigue < 0.0F) {
            this.stats.fatigue = 0.0F;
         }
      }

      if (this.Moodles.getMoodleLevel(MoodleType.FoodEaten) == 0) {
         var2 = this.getAppetiteMultiplier();
         var10000 = this.stats;
         var10000.hunger = (float)((double)var10000.hunger + ZomboidGlobals.HungerIncreaseWhileAsleep * SandboxOptions.instance.getStatsDecreaseMultiplier() * (double)var2 * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay() * this.getHungerMultiplier());
      } else {
         var10000 = this.stats;
         var10000.hunger += (float)(ZomboidGlobals.HungerIncreaseWhenWellFed * SandboxOptions.instance.getStatsDecreaseMultiplier() * ZomboidGlobals.HungerIncreaseWhileAsleep * SandboxOptions.instance.getStatsDecreaseMultiplier() * (double)GameTime.instance.getMultiplier() * this.getHungerMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay());
      }

      if (this.ForceWakeUpTime == 0.0F) {
         this.ForceWakeUpTime = 9.0F;
      }

      var2 = GameTime.getInstance().getTimeOfDay();
      var3 = GameTime.getInstance().getLastTimeOfDay();
      if (var3 > var2) {
         if (var3 < this.ForceWakeUpTime) {
            var2 += 24.0F;
         } else {
            var3 -= 24.0F;
         }
      }

      boolean var7 = var2 >= this.ForceWakeUpTime && var3 < this.ForceWakeUpTime;
      if (this.getAsleepTime() > 16.0F) {
         var7 = true;
      }

      if (GameClient.bClient || numPlayers > 1) {
         var7 = var7 || this.pressedAim() || this.pressedMovement(false);
      }

      if (this.ForceWakeUp) {
         var7 = true;
      }

      if (this.Asleep && var7) {
         this.ForceWakeUp = false;
         SoundManager.instance.setMusicWakeState(this, "WakeNormal");
         SleepingEvent.instance.wakeUp(this);
         this.ForceWakeUpTime = -1.0F;
         if (GameClient.bClient) {
            GameClient.instance.sendPlayer(this);
         }

         this.dirtyRecalcGridStackTime = 20.0F;
      }

   }

   public void updateEnduranceWhileSitting() {
      float var1 = (float)ZomboidGlobals.SittingEnduranceMultiplier;
      var1 *= 1.0F - this.stats.fatigue * 0.8F;
      var1 *= GameTime.instance.getMultiplier();
      Stats var10000 = this.stats;
      var10000.endurance = (float)((double)var10000.endurance + ZomboidGlobals.ImobileEnduranceReduce * SandboxOptions.instance.getEnduranceRegenMultiplier() * (double)this.getRecoveryMod() * (double)var1);
      this.stats.endurance = PZMath.clamp(this.stats.endurance, 0.0F, 1.0F);
   }

   private void updateEndurance() {
      if (!this.isSitOnGround() && !this.isSittingOnFurniture() && !this.isResting()) {
         float var1 = 1.0F;
         if (this.isSneaking()) {
            var1 = 1.5F;
         }

         Stats var10000;
         float var4;
         float var7;
         if (!(this.CurrentSpeed > 0.0F) || !this.isRunning() && !this.isSprinting() && !this.isDraggingCorpse()) {
            if (this.CurrentSpeed > 0.0F && this.Moodles.getMoodleLevel(MoodleType.HeavyLoad) > 2) {
               var7 = 0.7F;
               if (this.Traits.Asthmatic.isSet()) {
                  var7 = 1.0F;
               }

               float var3 = 1.4F;
               if (this.Traits.Overweight.isSet()) {
                  var3 = 2.9F;
               }

               if (this.Traits.Athletic.isSet()) {
                  var3 = 0.8F;
               }

               var3 *= 3.0F;
               var3 *= this.getPacingMod();
               var3 *= this.getHyperthermiaMod();
               var4 = 2.8F;
               switch (this.Moodles.getMoodleLevel(MoodleType.HeavyLoad)) {
                  case 2:
                     var4 = 1.5F;
                     break;
                  case 3:
                     var4 = 1.9F;
                     break;
                  case 4:
                     var4 = 2.3F;
               }

               var10000 = this.stats;
               var10000.endurance = (float)((double)var10000.endurance - ZomboidGlobals.RunningEnduranceReduce * (double)var3 * 0.5 * (double)var7 * (double)var1 * (double)GameTime.instance.getMultiplier() * (double)var4 / 2.0);
            }
         } else {
            double var2 = ZomboidGlobals.RunningEnduranceReduce;
            if (this.isSprinting()) {
               var2 = ZomboidGlobals.SprintingEnduranceReduce;
            }

            if (this.isDraggingCorpse()) {
               var2 = ZomboidGlobals.SprintingEnduranceReduce;
            }

            var4 = 1.4F;
            if (this.Traits.Overweight.isSet()) {
               var4 = 2.9F;
            }

            if (this.Traits.Athletic.isSet()) {
               var4 = 0.8F;
            }

            var4 *= 2.3F;
            var4 *= this.getPacingMod();
            var4 *= this.getHyperthermiaMod();
            float var5 = 0.7F;
            if (this.Traits.Asthmatic.isSet()) {
               var5 = 1.0F;
            }

            if (this.Moodles.getMoodleLevel(MoodleType.HeavyLoad) == 0) {
               var10000 = this.stats;
               var10000.endurance = (float)((double)var10000.endurance - var2 * (double)var4 * 0.5 * (double)var5 * (double)GameTime.instance.getMultiplier() * (double)var1);
            } else {
               float var6 = 2.8F;
               switch (this.Moodles.getMoodleLevel(MoodleType.HeavyLoad)) {
                  case 1:
                     var6 = 1.5F;
                     break;
                  case 2:
                     var6 = 1.9F;
                     break;
                  case 3:
                     var6 = 2.3F;
               }

               var10000 = this.stats;
               var10000.endurance = (float)((double)var10000.endurance - var2 * (double)var4 * 0.5 * (double)var5 * (double)GameTime.instance.getMultiplier() * (double)var6 * (double)var1);
            }
         }

         if (!this.isPlayerMoving()) {
            var7 = 1.0F;
            var7 *= 1.0F - this.stats.fatigue * 0.85F;
            var7 *= GameTime.instance.getMultiplier();
            if (this.Moodles.getMoodleLevel(MoodleType.HeavyLoad) <= 1) {
               var10000 = this.stats;
               var10000.endurance = (float)((double)var10000.endurance + ZomboidGlobals.ImobileEnduranceReduce * SandboxOptions.instance.getEnduranceRegenMultiplier() * (double)this.getRecoveryMod() * (double)var7);
            }
         }

         if (!this.isSprinting() && !this.isRunning() && this.CurrentSpeed > 0.0F) {
            var7 = 1.0F;
            var7 *= 1.0F - this.stats.fatigue;
            var7 *= GameTime.instance.getMultiplier();
            if (this.getMoodles().getMoodleLevel(MoodleType.Endurance) < 2) {
               if (this.Moodles.getMoodleLevel(MoodleType.HeavyLoad) <= 1) {
                  var10000 = this.stats;
                  var10000.endurance = (float)((double)var10000.endurance + ZomboidGlobals.ImobileEnduranceReduce / 4.0 * SandboxOptions.instance.getEnduranceRegenMultiplier() * (double)this.getRecoveryMod() * (double)var7);
               }
            } else {
               var10000 = this.stats;
               var10000.endurance = (float)((double)var10000.endurance - ZomboidGlobals.RunningEnduranceReduce / 7.0 * (double)var1);
            }
         }

      } else {
         this.updateEnduranceWhileSitting();
      }
   }

   private boolean checkActionsBlockingMovement() {
      if (this.CharacterActions.isEmpty()) {
         return false;
      } else {
         BaseAction var1 = (BaseAction)this.CharacterActions.get(0);
         return var1.blockMovementEtc;
      }
   }

   private void updateInteractKeyPanic() {
      if (this.PlayerIndex == 0) {
         if (GameKeyboard.isKeyPressed("Interact")) {
            this.ContextPanic += 0.6F;
         }

      }
   }

   private void updateSneakKey() {
      if (this.PlayerIndex != 0) {
         this.bSneakDebounce = false;
      } else {
         if (!this.isBlockMovement() && GameKeyboard.isKeyDown("Crouch")) {
            if (!this.bSneakDebounce) {
               this.setSneaking(!this.isSneaking());
               this.bSneakDebounce = true;
            }
         } else {
            this.bSneakDebounce = false;
         }

      }
   }

   private void updateChangeCharacterKey() {
      if (Core.bDebug) {
         if (this.PlayerIndex == 0 && GameKeyboard.isKeyDown(22)) {
            if (!this.bChangeCharacterDebounce) {
               this.FollowCamStack.clear();
               this.bChangeCharacterDebounce = true;

               for(int var2 = 0; var2 < this.getCell().getObjectList().size(); ++var2) {
                  IsoMovingObject var1 = (IsoMovingObject)this.getCell().getObjectList().get(var2);
                  if (var1 instanceof IsoSurvivor) {
                     this.FollowCamStack.add((IsoSurvivor)var1);
                  }
               }

               if (!this.FollowCamStack.isEmpty()) {
                  if (this.followID >= this.FollowCamStack.size()) {
                     this.followID = 0;
                  }

                  IsoCamera.SetCharacterToFollow((IsoGameCharacter)this.FollowCamStack.get(this.followID));
                  ++this.followID;
               }

            }
         } else {
            this.bChangeCharacterDebounce = false;
         }
      }
   }

   private void updateEnableModelsKey() {
      if (Core.bDebug) {
         if (this.PlayerIndex == 0 && GameKeyboard.isKeyPressed("ToggleModelsEnabled")) {
            ModelManager.instance.bDebugEnableModels = !ModelManager.instance.bDebugEnableModels;
         }

      }
   }

   private void updateDeathDragDown() {
      if (!this.isDead()) {
         if (this.isDeathDragDown()) {
            if (this.isGodMod()) {
               this.setDeathDragDown(false);
            } else if (!"EndDeath".equals(this.getHitReaction())) {
               for(int var1 = -1; var1 <= 1; ++var1) {
                  for(int var2 = -1; var2 <= 1; ++var2) {
                     IsoGridSquare var3 = this.getCell().getGridSquare(PZMath.fastfloor(this.getX()) + var2, PZMath.fastfloor(this.getY()) + var1, PZMath.fastfloor(this.getZ()));
                     if (var3 != null) {
                        for(int var4 = 0; var4 < var3.getMovingObjects().size(); ++var4) {
                           IsoMovingObject var5 = (IsoMovingObject)var3.getMovingObjects().get(var4);
                           IsoZombie var6 = (IsoZombie)Type.tryCastTo(var5, IsoZombie.class);
                           if (var6 != null && var6.isAlive() && !var6.isOnFloor()) {
                              this.setAttackedBy(var6);
                              this.setHitReaction("EndDeath");
                              this.setBlockMovement(true);
                              return;
                           }
                        }
                     }
                  }
               }

               this.setDeathDragDown(false);
               if (GameClient.bClient) {
                  DebugLog.DetailedInfo.warn("UpdateDeathDragDown: no zombies found around player \"%s\"", this.getUsername());
                  this.setHitFromBehind(false);
                  this.Kill((IsoGameCharacter)null);
               }

            }
         }
      }
   }

   private void updateGodModeKey() {
      if (Core.bDebug) {
         if (GameKeyboard.isKeyPressed("ToggleGodModeInvisible")) {
            IsoPlayer var1 = null;

            for(int var2 = 0; var2 < numPlayers; ++var2) {
               if (players[var2] != null && !players[var2].isDead()) {
                  var1 = players[var2];
                  break;
               }
            }

            if (this == var1) {
               boolean var4 = !var1.isGodMod();
               DebugLog.General.println("Toggle GodMode: %s", var4 ? "ON" : "OFF");
               var1.setInvisible(var4);
               var1.setGhostMode(var4);
               var1.setGodMod(var4);
               var1.setNoClip(var4);

               for(int var3 = 0; var3 < numPlayers; ++var3) {
                  if (players[var3] != null && players[var3] != var1) {
                     players[var3].setInvisible(var4);
                     players[var3].setGhostMode(var4);
                     players[var3].setGodMod(var4);
                     players[var3].setNoClip(var4);
                  }
               }

               if (GameClient.bClient) {
                  GameClient.sendPlayerExtraInfo(var1);
               }
            }

         }
      }
   }

   private void checkReloading() {
      HandWeapon var1 = (HandWeapon)Type.tryCastTo(this.getPrimaryHandItem(), HandWeapon.class);
      if (var1 != null && var1.isReloadable(this)) {
         boolean var2 = false;
         boolean var3 = false;
         boolean var4;
         if (this.JoypadBind != -1 && this.bJoypadMovementActive) {
            var4 = JoypadManager.instance.isRBPressed(this.JoypadBind);
            if (var4) {
               var2 = !this.bReloadButtonDown;
            }

            this.bReloadButtonDown = var4;
            var4 = JoypadManager.instance.isLBPressed(this.JoypadBind);
            if (var4) {
               var3 = !this.bRackButtonDown;
            }

            this.bRackButtonDown = var4;
         }

         if (this.PlayerIndex == 0) {
            var4 = GameKeyboard.isKeyDown("ReloadWeapon");
            if (var4) {
               var2 = !this.bReloadKeyDown;
            }

            this.bReloadKeyDown = var4;
            var4 = GameKeyboard.isKeyDown("Rack Firearm");
            if (var4) {
               var3 = !this.bRackKeyDown;
            }

            this.bRackKeyDown = var4;
         }

         if (var2) {
            this.setVariable("WeaponReloadType", var1.getWeaponReloadType());
            LuaEventManager.triggerEvent("OnPressReloadButton", this, var1);
         } else if (var3) {
            this.setVariable("WeaponReloadType", var1.getWeaponReloadType());
            LuaEventManager.triggerEvent("OnPressRackButton", this, var1);
         }

      }
   }

   public void postupdate() {
      IsoPlayer.s_performance.postUpdate.invokeAndMeasure(this, IsoPlayer::postupdateInternal);
   }

   private void postupdateInternal() {
      boolean var1 = this.hasHitReaction();
      super.postupdate();
      if (var1 && this.hasHitReaction() && !this.isCurrentState(PlayerHitReactionState.instance()) && !this.isCurrentState(PlayerHitReactionPVPState.instance())) {
         this.setHitReaction("");
      }

      if (this.isNPC) {
         GameTime var2 = GameTime.getInstance();
         float var3 = 1.0F / var2.getMinutesPerDay() / 60.0F * var2.getMultiplier() / 2.0F;
         if (Core.bLastStand) {
            var3 = 1.0F / var2.getMinutesPerDay() / 60.0F * var2.getUnmoddedMultiplier() / 2.0F;
         }

         this.setHoursSurvived(this.getHoursSurvived() + (double)var3);
      }

      if (this.getBodyDamage() != null) {
         this.getBodyDamage().setBodyPartsLastState();
      }

   }

   public boolean isSolidForSeparate() {
      return this.isGhostMode() ? false : super.isSolidForSeparate();
   }

   public boolean isPushableForSeparate() {
      if (this.isCurrentState(PlayerHitReactionState.instance())) {
         return false;
      } else {
         return this.isCurrentState(SwipeStatePlayer.instance()) ? false : super.isPushableForSeparate();
      }
   }

   public boolean isPushedByForSeparate(IsoMovingObject var1) {
      if (!this.isPlayerMoving() && var1.isZombie() && ((IsoZombie)var1).isAttacking()) {
         return false;
      } else {
         return !GameClient.bClient || this.isLocalPlayer() && this.isJustMoved() ? super.isPushedByForSeparate(var1) : false;
      }
   }

   private void updateExt() {
      if (!this.isSneaking()) {
         if (!this.isGrappling() && !this.isBeingGrappled()) {
            this.extUpdateCount += GameTime.getInstance().getMultiplier() / 0.8F;
            if (!this.getAdvancedAnimator().containsAnyIdleNodes() && !this.isSitOnGround()) {
               this.extUpdateCount = 0.0F;
            }

            if (!(this.extUpdateCount <= 5000.0F)) {
               this.extUpdateCount = 0.0F;
               if (this.stats.NumVisibleZombies == 0 && this.stats.NumChasingZombies == 0) {
                  if (Rand.NextBool(3)) {
                     if (this.getAdvancedAnimator().containsAnyIdleNodes() || this.isSitOnGround()) {
                        this.onIdlePerformFidgets();
                        this.reportEvent("EventDoExt");
                     }
                  }
               }
            }
         }
      }
   }

   private void onIdlePerformFidgets() {
      Moodles var1 = this.getMoodles();
      BodyDamage var2 = this.getBodyDamage();
      if (var1.getMoodleLevel(MoodleType.Hypothermia) > 0 && Rand.NextBool(7)) {
         this.setVariable("Ext", "Shiver");
      } else if (var1.getMoodleLevel(MoodleType.Hyperthermia) > 0 && Rand.NextBool(7)) {
         this.setVariable("Ext", "WipeBrow");
      } else {
         int var10002;
         if (var1.getMoodleLevel(MoodleType.Sick) > 0 && Rand.NextBool(7)) {
            if (Rand.NextBool(4)) {
               this.setVariable("Ext", "Cough");
            } else {
               var10002 = Rand.Next(2);
               this.setVariable("Ext", "PainStomach" + (var10002 + 1));
            }

         } else if (var1.getMoodleLevel(MoodleType.Endurance) > 2 && Rand.NextBool(10)) {
            if (Rand.NextBool(5) && !this.isSitOnGround()) {
               this.setVariable("Ext", "BentDouble");
            } else {
               this.setVariable("Ext", "WipeBrow");
            }

         } else if (var1.getMoodleLevel(MoodleType.Tired) > 2 && Rand.NextBool(10)) {
            if (Rand.NextBool(7)) {
               this.setVariable("Ext", "TiredStretch");
            } else if (Rand.NextBool(7)) {
               this.setVariable("Ext", "Sway");
            } else {
               this.setVariable("Ext", "Yawn");
            }

         } else if (var2.doBodyPartsHaveInjuries(BodyPartType.Head, BodyPartType.Neck) && Rand.NextBool(7)) {
            if (var2.areBodyPartsBleeding(BodyPartType.Head, BodyPartType.Neck) && Rand.NextBool(2)) {
               this.setVariable("Ext", "WipeHead");
            } else {
               var10002 = Rand.Next(2);
               this.setVariable("Ext", "PainHead" + (var10002 + 1));
            }

         } else if (var2.doBodyPartsHaveInjuries(BodyPartType.UpperArm_L, BodyPartType.ForeArm_L) && Rand.NextBool(7)) {
            if (var2.areBodyPartsBleeding(BodyPartType.UpperArm_L, BodyPartType.ForeArm_L) && Rand.NextBool(2)) {
               this.setVariable("Ext", "WipeArmL");
            } else {
               this.setVariable("Ext", "PainArmL");
            }

         } else if (var2.doBodyPartsHaveInjuries(BodyPartType.UpperArm_R, BodyPartType.ForeArm_R) && Rand.NextBool(7)) {
            if (var2.areBodyPartsBleeding(BodyPartType.UpperArm_R, BodyPartType.ForeArm_R) && Rand.NextBool(2)) {
               this.setVariable("Ext", "WipeArmR");
            } else {
               this.setVariable("Ext", "PainArmR");
            }

         } else if (var2.doesBodyPartHaveInjury(BodyPartType.Hand_L) && Rand.NextBool(7)) {
            this.setVariable("Ext", "PainHandL");
         } else if (var2.doesBodyPartHaveInjury(BodyPartType.Hand_R) && Rand.NextBool(7)) {
            this.setVariable("Ext", "PainHandR");
         } else if (!this.isSitOnGround() && var2.doBodyPartsHaveInjuries(BodyPartType.UpperLeg_L, BodyPartType.LowerLeg_L) && Rand.NextBool(7)) {
            if (var2.areBodyPartsBleeding(BodyPartType.UpperLeg_L, BodyPartType.LowerLeg_L) && Rand.NextBool(2)) {
               this.setVariable("Ext", "WipeLegL");
            } else {
               this.setVariable("Ext", "PainLegL");
            }

         } else if (!this.isSitOnGround() && var2.doBodyPartsHaveInjuries(BodyPartType.UpperLeg_R, BodyPartType.LowerLeg_R) && Rand.NextBool(7)) {
            if (var2.areBodyPartsBleeding(BodyPartType.UpperLeg_R, BodyPartType.LowerLeg_R) && Rand.NextBool(2)) {
               this.setVariable("Ext", "WipeLegR");
            } else {
               this.setVariable("Ext", "PainLegR");
            }

         } else if (var2.doBodyPartsHaveInjuries(BodyPartType.Torso_Upper, BodyPartType.Torso_Lower) && Rand.NextBool(7)) {
            if (var2.areBodyPartsBleeding(BodyPartType.Torso_Upper, BodyPartType.Torso_Lower) && Rand.NextBool(2)) {
               var10002 = Rand.Next(2);
               this.setVariable("Ext", "WipeTorso" + (var10002 + 1));
            } else {
               this.setVariable("Ext", "PainTorso");
            }

         } else if (WeaponType.getWeaponType((IsoGameCharacter)this) != WeaponType.barehand) {
            var10002 = Rand.Next(5);
            this.setVariable("Ext", "" + (var10002 + 1));
         } else if (Rand.NextBool(10)) {
            this.setVariable("Ext", "ChewNails");
         } else if (Rand.NextBool(10)) {
            this.setVariable("Ext", "ShiftWeight");
         } else if (Rand.NextBool(10)) {
            this.setVariable("Ext", "PullAtColar");
         } else if (Rand.NextBool(10)) {
            this.setVariable("Ext", "BridgeNose");
         } else {
            var10002 = Rand.Next(5);
            this.setVariable("Ext", "" + (var10002 + 1));
         }
      }
   }

   private boolean updateUseKey() {
      if (GameServer.bServer) {
         return false;
      } else if (!this.isLocalPlayer()) {
         return false;
      } else if (this.PlayerIndex != 0) {
         return false;
      } else {
         this.timePressedContext += GameTime.instance.getRealworldSecondsSinceLastUpdate();
         boolean var1 = GameKeyboard.isKeyDown("Interact");
         if (var1 && this.timePressedContext < 0.5F) {
            this.bPressContext = true;
         } else {
            if (this.bPressContext && (Core.CurrentTextEntryBox != null && Core.CurrentTextEntryBox.isDoingTextEntry() || !GameKeyboard.doLuaKeyPressed)) {
               this.bPressContext = false;
            }

            if (this.bPressContext && this.doContext(this.dir)) {
               this.clearUseKeyVariables();
               return true;
            }

            if (!var1) {
               this.clearUseKeyVariables();
            }
         }

         return false;
      }
   }

   private void clearUseKeyVariables() {
      this.bPressContext = false;
      this.timePressedContext = 0.0F;
   }

   private void updateHitByVehicle() {
      if (!GameServer.bServer) {
         if (this.isLocalPlayer()) {
            if (this.vehicle4testCollision != null && this.ulBeatenVehicle.Check() && SandboxOptions.instance.DamageToPlayerFromHitByACar.getValue() > 1) {
               BaseVehicle var1 = this.vehicle4testCollision;
               this.vehicle4testCollision = null;
               if (var1.isEngineRunning() && this.getVehicle() != var1) {
                  float var2 = var1.jniLinearVelocity.x;
                  float var3 = var1.jniLinearVelocity.z;
                  float var4 = (float)Math.sqrt((double)(var2 * var2 + var3 * var3));
                  Vector2 var5 = (Vector2)((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).alloc();
                  Vector2 var6 = var1.testCollisionWithCharacter(this, 0.20000002F, var5);
                  if (var6 != null && var6.x != -1.0F) {
                     var6.x = (var6.x - var1.getX()) * var4 * 1.0F + this.getX();
                     var6.y = (var6.y - var1.getY()) * var4 * 1.0F + this.getX();
                     if (this.isOnFloor()) {
                        int var7 = var1.testCollisionWithProneCharacter(this, false);
                        if (var7 > 0) {
                           this.doBeatenVehicle(Math.max(var4 * 6.0F, 5.0F));
                        }

                        this.doBeatenVehicle(0.0F);
                     } else if (this.getCurrentState() != PlayerFallDownState.instance() && var4 > 0.1F) {
                        this.doBeatenVehicle(Math.max(var4 * 2.0F, 5.0F));
                     }
                  }

                  ((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).release(var5);
               }
            }
         }
      }
   }

   private void updateSoundListener() {
      if (!GameServer.bServer) {
         if (this.isLocalPlayer()) {
            if (this.soundListener == null) {
               this.soundListener = (BaseSoundListener)(Core.SoundDisabled ? new DummySoundListener(this.PlayerIndex) : new SoundListener(this.PlayerIndex));
            }

            this.soundListener.setPos(this.getX(), this.getY(), this.getZ());
            this.checkNearbyRooms -= GameTime.getInstance().getThirtyFPSMultiplier();
            if (this.checkNearbyRooms <= 0.0F) {
               this.checkNearbyRooms = 30.0F;
               this.numNearbyBuildingsRooms = (float)IsoWorld.instance.MetaGrid.countNearbyBuildingsRooms(this);
            }

            this.soundListener.tick();
         }
      }
   }

   public void updateMovementRates() {
      this.calculateWalkSpeed();
      this.m_idleSpeed = this.calculateIdleSpeed();
      this.updateFootInjuries();
   }

   public void pressedAttack() {
      CombatManager.getInstance().pressedAttack(this);
   }

   public void setAttackVariationX(float var1) {
      this.m_attackVariationX = var1;
   }

   private float getAttackVariationX() {
      return this.m_attackVariationX;
   }

   public void setAttackVariationY(float var1) {
      this.m_attackVariationY = var1;
   }

   private float getAttackVariationY() {
      return this.m_attackVariationY;
   }

   public boolean canPerformHandToHandCombat() {
      if (this.primaryHandModel != null && !StringUtils.isNullOrEmpty(this.primaryHandModel.maskVariableValue) && this.secondaryHandModel != null && !StringUtils.isNullOrEmpty(this.secondaryHandModel.maskVariableValue)) {
         return false;
      } else {
         return this.getPrimaryHandItem() == null || this.getPrimaryHandItem().getItemReplacementPrimaryHand() == null || this.getSecondaryHandItem() == null || this.getSecondaryHandItem().getItemReplacementSecondHand() == null;
      }
   }

   public void clearHandToHandAttack() {
      super.clearHandToHandAttack();
      this.setInitiateAttack(false);
      this.attackStarted = false;
      this.setAttackType((String)null);
   }

   public void setAttackAnimThrowTimer(long var1) {
      this.AttackAnimThrowTimer = System.currentTimeMillis() + var1;
   }

   public boolean isAttackAnimThrowTimeOut() {
      return this.AttackAnimThrowTimer <= System.currentTimeMillis();
   }

   public boolean isAiming() {
      if (!this.isAttackAnimThrowTimeOut()) {
         return true;
      } else {
         return GameClient.bClient && this.isLocalPlayer() && DebugOptions.instance.Multiplayer.Debug.AttackPlayer.getValue() ? false : super.isAiming();
      }
   }

   private String getWeaponType() {
      return !this.isAttackAnimThrowTimeOut() ? "throwing" : this.WeaponT;
   }

   private void setWeaponType(String var1) {
      this.WeaponT = var1;
   }

   public int calculateCritChance(IsoGameCharacter var1) {
      if (this.isDoShove()) {
         float var8 = 35.0F;
         if (var1 instanceof IsoPlayer) {
            IsoPlayer var12 = (IsoPlayer)var1;
            var8 = 20.0F;
            if (GameClient.bClient && !var12.isLocalPlayer()) {
               double var9 = var12 instanceof IsoAnimal ? (double)((IsoAnimal)var12).getData().getWeight() : var12.getNutrition().getWeight();
               var8 -= (float)var12.remoteStrLvl * 1.5F;
               if (var9 < 80.0) {
                  var8 += (float)Math.abs((var9 - 80.0) / 2.0);
               } else {
                  var8 -= (float)((var9 - 80.0) / 2.0);
               }
            }
         }

         var8 -= (float)(this.getMoodles().getMoodleLevel(MoodleType.Endurance) * 5);
         var8 -= (float)(this.getMoodles().getMoodleLevel(MoodleType.HeavyLoad) * 5);
         var8 -= (float)this.getMoodles().getMoodleLevel(MoodleType.Panic) * 1.3F;
         var8 += (float)(this.getPerkLevel(PerkFactory.Perks.Strength) * 2);
         return (int)var8;
      } else if (this.isDoShove() && var1.getStateMachine().getCurrent() == StaggerBackState.instance() && var1 instanceof IsoZombie) {
         return 100;
      } else if (this.getPrimaryHandItem() != null && this.getPrimaryHandItem() instanceof HandWeapon) {
         HandWeapon var2 = (HandWeapon)this.getPrimaryHandItem();
         float var3 = var2.getCriticalChance();
         if (var2.isAlwaysKnockdown()) {
            return 100;
         } else {
            WeaponType var4 = WeaponType.getWeaponType((IsoGameCharacter)this);
            if (var4.isRanged) {
               var3 += (float)(var2.getAimingPerkCritModifier() * this.getPerkLevel(PerkFactory.Perks.Aiming));
               float var5 = this.DistToProper(var1);
               float var6 = var2.getMaxSightRange(this);
               float var7 = var2.getMinSightRange(this);
               var3 += PZMath.max(CombatManager.getDistanceModifierSightless(var5, false) - CombatManager.getAimDelayPenaltySightless(PZMath.max(0.0F, this.getAimingDelay()), var5), CombatManager.getDistanceModifier(var5, var7, var6, false) - CombatManager.getAimDelayPenalty(PZMath.max(0.0F, this.getAimingDelay()), var5, var7, var6));
               var3 -= CombatManager.getMoodlesPenalty(this, var5);
               var3 -= CombatManager.getWeatherPenalty(this, var2, var1.getCurrentSquare(), var5);
               var3 -= CombatManager.getMovePenalty(this, var5);
               if (this.Traits.Marksman.isSet()) {
                  var3 += 10.0F;
               }
            } else {
               if (var2.isTwoHandWeapon() && (this.getPrimaryHandItem() != var2 || this.getSecondaryHandItem() != var2)) {
                  var3 -= var3 / 3.0F;
               }

               if (this.chargeTime < 2.0F) {
                  var3 -= var3 / 5.0F;
               }

               int var10 = this.getPerkLevel(PerkFactory.Perks.Blunt);
               if (var2.getCategories().contains("Axe")) {
                  var10 = this.getPerkLevel(PerkFactory.Perks.Axe);
               }

               if (var2.getCategories().contains("LongBlade")) {
                  var10 = this.getPerkLevel(PerkFactory.Perks.LongBlade);
               }

               if (var2.getCategories().contains("Spear")) {
                  var10 = this.getPerkLevel(PerkFactory.Perks.Spear);
               }

               if (var2.getCategories().contains("SmallBlade")) {
                  var10 = this.getPerkLevel(PerkFactory.Perks.SmallBlade);
               }

               if (var2.getCategories().contains("SmallBlunt")) {
                  var10 = this.getPerkLevel(PerkFactory.Perks.SmallBlunt);
               }

               var3 += (float)var10 * 3.0F;
               if (var1 instanceof IsoPlayer) {
                  IsoPlayer var11 = (IsoPlayer)var1;
                  if (GameClient.bClient && !var11.isLocalPlayer()) {
                     var3 -= (float)var11.remoteStrLvl * 1.5F;
                     if (var11.getNutrition().getWeight() < 80.0) {
                        var3 += (float)Math.abs((var11.getNutrition().getWeight() - 80.0) / 2.0);
                     } else {
                        var3 -= (float)((var11.getNutrition().getWeight() - 80.0) / 2.0);
                     }
                  }
               }

               var3 -= (float)(this.getMoodles().getMoodleLevel(MoodleType.Endurance) * 5);
               var3 -= (float)(this.getMoodles().getMoodleLevel(MoodleType.HeavyLoad) * 5);
               var3 -= (float)this.getMoodles().getMoodleLevel(MoodleType.Panic) * 1.3F;
            }

            if (var1 instanceof IsoZombie) {
               if (SandboxOptions.instance.Lore.Toughness.getValue() == 1) {
                  var3 -= 6.0F;
               }

               if (SandboxOptions.instance.Lore.Toughness.getValue() == 3) {
                  var3 += 6.0F;
               }
            }

            return (int)PZMath.clamp(var3, 10.0F, 90.0F);
         }
      } else {
         return 0;
      }
   }

   private void checkJoypadIgnoreAimUntilCentered() {
      if (this.bJoypadIgnoreAimUntilCentered) {
         if (GameWindow.ActivatedJoyPad != null && this.JoypadBind != -1 && this.bJoypadMovementActive) {
            float var1 = JoypadManager.instance.getAimingAxisX(this.JoypadBind);
            float var2 = JoypadManager.instance.getAimingAxisY(this.JoypadBind);
            if (var1 * var1 + var2 + var2 <= 0.0F) {
               this.bJoypadIgnoreAimUntilCentered = false;
            }
         }

      }
   }

   public boolean isAimControlActive() {
      if (this.isForceAim()) {
         return true;
      } else if (this.isAimKeyDown()) {
         return true;
      } else {
         return GameWindow.ActivatedJoyPad != null && this.JoypadBind != -1 && this.bJoypadMovementActive && this.getJoypadAimVector(tempo).getLengthSquared() > 0.0F;
      }
   }

   public boolean allowsTwist() {
      if (this.isAttacking()) {
         return false;
      } else if (this.isTwisting()) {
         return true;
      } else if (this.isAiming()) {
         return true;
      } else {
         return this.isSneaking();
      }
   }

   private Vector2 getJoypadAimVector(Vector2 var1) {
      if (this.bJoypadIgnoreAimUntilCentered) {
         return var1.set(0.0F, 0.0F);
      } else {
         float var2 = JoypadManager.instance.getAimingAxisY(this.JoypadBind);
         float var3 = JoypadManager.instance.getAimingAxisX(this.JoypadBind);
         float var4 = JoypadManager.instance.getDeadZone(this.JoypadBind, 0);
         if (var3 * var3 + var2 * var2 < var4 * var4) {
            var2 = 0.0F;
            var3 = 0.0F;
         }

         return var1.set(var3, var2);
      }
   }

   private Vector2 getJoypadMoveVector(Vector2 var1) {
      float var2 = JoypadManager.instance.getMovementAxisY(this.JoypadBind);
      float var3 = JoypadManager.instance.getMovementAxisX(this.JoypadBind);
      float var4 = JoypadManager.instance.getDeadZone(this.JoypadBind, 0);
      if (var3 * var3 + var2 * var2 < var4 * var4) {
         var2 = 0.0F;
         var3 = 0.0F;
      }

      var1.set(var3, var2);
      if (this.isIgnoreInputsForDirection()) {
         var1.set(0.0F, 0.0F);
      }

      return var1;
   }

   private void updateToggleToAim() {
      if (this.PlayerIndex == 0) {
         if (!Core.getInstance().isToggleToAim()) {
            this.setForceAim(false);
         } else {
            boolean var1 = this.isAimKeyDown();
            long var2 = System.currentTimeMillis();
            if (var1) {
               if (this.aimKeyDownMS == 0L) {
                  this.aimKeyDownMS = var2;
               }
            } else {
               if (this.aimKeyDownMS != 0L && var2 - this.aimKeyDownMS < 500L) {
                  this.toggleForceAim();
               } else if (this.isForceAim()) {
                  if (this.aimKeyDownMS != 0L) {
                     this.toggleForceAim();
                  } else {
                     int var4 = GameKeyboard.whichKeyDown("Aim");
                     boolean var5 = var4 == 29 || var4 == 157;
                     if (var5 && UIManager.isMouseOverInventory()) {
                        this.toggleForceAim();
                     }
                  }
               }

               this.aimKeyDownMS = 0L;
            }

         }
      }
   }

   private void UpdateInputState(InputState var1) {
      var1.bMelee = false;
      var1.bGrapple = false;
      if (!MPDebugAI.updateInputState(this, var1)) {
         if (GameWindow.ActivatedJoyPad != null && this.JoypadBind != -1) {
            if (this.bJoypadMovementActive) {
               var1.isAttacking = this.isCharging;
               if (this.bJoypadIgnoreChargingRT) {
                  var1.isAttacking = false;
               }

               if (this.bJoypadIgnoreAimUntilCentered) {
                  float var2 = JoypadManager.instance.getAimingAxisX(this.JoypadBind);
                  float var3 = JoypadManager.instance.getAimingAxisY(this.JoypadBind);
                  if (var2 == 0.0F && var3 == 0.0F) {
                     this.bJoypadIgnoreAimUntilCentered = false;
                  }
               }
            }

            if (this.isChargingLT) {
               var1.bMelee = true;
               var1.isAttacking = false;
            }
         } else {
            var1.isAttacking = this.isCharging && GameKeyboard.isKeyDown("Attack/Click");
            if (GameKeyboard.isKeyDown("Melee") && this.isAuthorizedHandToHandAction()) {
               var1.bMelee = true;
               var1.isAttacking = false;
            }

            if ((GameKeyboard.isKeyDown("Interact") || this.getGrapplingTarget() != null && ((IsoGameCharacter)this.getGrapplingTarget()).isOnFire()) && this.isAuthorizedHandToHandAction()) {
               var1.bGrapple = true;
               var1.isAttacking = false;
            }
         }

         if (GameWindow.ActivatedJoyPad != null && this.JoypadBind != -1) {
            if (this.bJoypadMovementActive) {
               var1.isCharging = JoypadManager.instance.isRTPressed(this.JoypadBind);
               var1.isChargingLT = JoypadManager.instance.isLTPressed(this.JoypadBind);
               if (this.bJoypadIgnoreChargingRT && !var1.isCharging) {
                  this.bJoypadIgnoreChargingRT = false;
               }
            }

            var1.isAiming = false;
            var1.bRunning = false;
            var1.bSprinting = false;
            Vector2 var7 = this.getJoypadAimVector(tempVector2);
            if (var7.x == 0.0F && var7.y == 0.0F) {
               var1.isCharging = false;
               Vector2 var9 = this.getJoypadMoveVector(tempVector2);
               if (this.isAutoWalk() && var9.getLengthSquared() == 0.0F) {
                  var9.set(this.getAutoWalkDirection());
               }

               if (var9.x != 0.0F || var9.y != 0.0F) {
                  if (this.isAllowRun()) {
                     var1.bRunning = JoypadManager.instance.isRTPressed(this.JoypadBind);
                  }

                  var1.isAttacking = false;
                  var1.bMelee = false;
                  this.bJoypadIgnoreChargingRT = true;
                  var1.isCharging = false;
                  boolean var10 = JoypadManager.instance.isBPressed(this.JoypadBind);
                  if (var1.bRunning && var10 && !this.bJoypadBDown) {
                     this.bJoypadSprint = !this.bJoypadSprint;
                  }

                  this.bJoypadBDown = var10;
                  var1.bSprinting = this.bJoypadSprint;
               }
            } else {
               var1.isAiming = true;
            }

            if (!var1.bRunning) {
               this.bJoypadBDown = false;
               this.bJoypadSprint = false;
            }
         } else {
            var1.isAiming = this.isAimKeyDown() && this.getPlayerNum() == 0 && StringUtils.isNullOrEmpty(this.getVariableString("BumpFallType"));
            var1.isCharging = this.isAimKeyDown();
            if (this.isAllowRun()) {
               var1.bRunning = GameKeyboard.isKeyDown("Run");
            } else {
               var1.bRunning = false;
            }

            if (this.isAllowSprint()) {
               if (!Core.getInstance().isOptiondblTapJogToSprint()) {
                  if (GameKeyboard.isKeyDown("Sprint")) {
                     var1.bSprinting = true;
                     this.pressedRunTimer = 1.0F;
                  } else {
                     var1.bSprinting = false;
                  }
               } else {
                  if (!GameKeyboard.wasKeyDown("Run") && GameKeyboard.isKeyDown("Run") && this.pressedRunTimer < 30.0F && this.pressedRun) {
                     var1.bSprinting = true;
                  }

                  if (GameKeyboard.wasKeyDown("Run") && !GameKeyboard.isKeyDown("Run")) {
                     var1.bSprinting = false;
                     this.pressedRun = true;
                  }

                  if (!var1.bRunning) {
                     var1.bSprinting = false;
                  }

                  if (this.pressedRun) {
                     ++this.pressedRunTimer;
                  }

                  if (this.pressedRunTimer > 30.0F) {
                     this.pressedRunTimer = 0.0F;
                     this.pressedRun = false;
                  }
               }
            } else {
               var1.bSprinting = false;
            }

            this.updateToggleToAim();
            if (var1.bRunning || var1.bSprinting) {
               this.setForceAim(false);
            }

            long var4;
            boolean var6;
            boolean var8;
            if (this.PlayerIndex == 0 && Core.getInstance().isToggleToRun()) {
               var6 = GameKeyboard.isKeyDown("Run");
               var8 = GameKeyboard.wasKeyDown("Run");
               var4 = System.currentTimeMillis();
               if (var6 && !var8) {
                  this.runKeyDownMS = var4;
               } else if (!var6 && var8 && var4 - this.runKeyDownMS < 500L) {
                  this.toggleForceRun();
               }
            }

            if (this.PlayerIndex == 0 && Core.getInstance().isToggleToSprint()) {
               var6 = GameKeyboard.isKeyDown("Sprint");
               var8 = GameKeyboard.wasKeyDown("Sprint");
               var4 = System.currentTimeMillis();
               if (var6 && !var8) {
                  this.sprintKeyDownMS = var4;
               } else if (!var6 && var8 && var4 - this.sprintKeyDownMS < 500L) {
                  this.toggleForceSprint();
               }
            }

            if (this.isForceAim()) {
               var1.isAiming = true;
               var1.isCharging = true;
            }

            if (this.isForceRun()) {
               var1.bRunning = true;
            }

            if (this.isForceSprint()) {
               var1.bSprinting = true;
            }
         }

      }
   }

   public IsoZombie getClosestZombieToOtherZombie(IsoZombie var1) {
      IsoZombie var2 = null;
      ArrayList var3 = new ArrayList();
      ArrayList var4 = IsoWorld.instance.CurrentCell.getObjectList();

      for(int var5 = 0; var5 < var4.size(); ++var5) {
         IsoMovingObject var6 = (IsoMovingObject)var4.get(var5);
         if (var6 != var1 && var6 instanceof IsoZombie) {
            var3.add((IsoZombie)var6);
         }
      }

      float var9 = 0.0F;

      for(int var10 = 0; var10 < var3.size(); ++var10) {
         IsoZombie var7 = (IsoZombie)var3.get(var10);
         float var8 = IsoUtils.DistanceTo(var7.getX(), var7.getY(), var1.getX(), var1.getY());
         if (var2 == null || var8 < var9) {
            var2 = var7;
            var9 = var8;
         }
      }

      return var2;
   }

   /** @deprecated */
   @Deprecated
   public IsoGameCharacter getClosestZombieDist() {
      float var1 = 0.4F;
      boolean var2 = false;
      testHitPosition.x = this.getX() + this.getForwardDirection().x * var1;
      testHitPosition.y = this.getY() + this.getForwardDirection().y * var1;
      HandWeapon var3 = this.getWeapon();
      ArrayList var4 = new ArrayList();

      for(int var5 = PZMath.fastfloor(testHitPosition.x) - (int)var3.getMaxRange(); var5 <= PZMath.fastfloor(testHitPosition.x) + (int)var3.getMaxRange(); ++var5) {
         for(int var6 = PZMath.fastfloor(testHitPosition.y) - (int)var3.getMaxRange(); var6 <= PZMath.fastfloor(testHitPosition.y) + (int)var3.getMaxRange(); ++var6) {
            IsoGridSquare var7 = IsoWorld.instance.CurrentCell.getGridSquare((double)var5, (double)var6, (double)this.getZ());
            if (var7 != null && var7.getMovingObjects().size() > 0) {
               for(int var8 = 0; var8 < var7.getMovingObjects().size(); ++var8) {
                  IsoMovingObject var9 = (IsoMovingObject)var7.getMovingObjects().get(var8);
                  if (var9 instanceof IsoZombie) {
                     Vector2 var10 = tempVector2_1.set(this.getX(), this.getY());
                     Vector2 var11 = tempVector2_2.set(var9.getX(), var9.getY());
                     var11.x -= var10.x;
                     var11.y -= var10.y;
                     Vector2 var12 = this.getForwardDirection();
                     var11.normalize();
                     var12.normalize();
                     Float var13 = var11.dot(var12);
                     if (var13 >= var3.getMinAngle() || var9.isOnFloor()) {
                        var2 = true;
                     }

                     if (var2 && ((IsoZombie)var9).Health > 0.0F) {
                        ((IsoZombie)var9).setHitFromBehind(this.isBehind((IsoZombie)var9));
                        ((IsoZombie)var9).setHitAngle(((IsoZombie)var9).getForwardDirection());
                        ((IsoZombie)var9).setPlayerAttackPosition(((IsoZombie)var9).testDotSide(this));
                        float var14 = IsoUtils.DistanceTo(var9.getX(), var9.getY(), this.getX(), this.getY());
                        if (var14 < var3.getMaxRange()) {
                           var4.add((IsoZombie)var9);
                        }
                     }
                  }
               }
            }
         }
      }

      if (!var4.isEmpty()) {
         Collections.sort(var4, new Comparator<IsoGameCharacter>() {
            public int compare(IsoGameCharacter var1, IsoGameCharacter var2) {
               float var3 = IsoUtils.DistanceTo(var1.getX(), var1.getY(), IsoPlayer.testHitPosition.x, IsoPlayer.testHitPosition.y);
               float var4 = IsoUtils.DistanceTo(var2.getX(), var2.getY(), IsoPlayer.testHitPosition.x, IsoPlayer.testHitPosition.y);
               if (var3 > var4) {
                  return 1;
               } else {
                  return var4 > var3 ? -1 : 0;
               }
            }
         });
         return (IsoGameCharacter)var4.get(0);
      } else {
         return null;
      }
   }

   public void hitConsequences(HandWeapon var1, IsoGameCharacter var2, boolean var3, float var4, boolean var5) {
      if (var2 instanceof IsoAnimal) {
         this.getBodyDamage().DamageFromAnimal((IsoAnimal)var2);
         this.setKnockedDown(((IsoAnimal)var2).adef.knockdownAttack);
         this.setHitReaction("HitReaction");
      } else {
         String var6 = var2.getVariableString("ZombieHitReaction");
         if ("Shot".equals(var6)) {
            var2.setCriticalHit(Rand.Next(100) < ((IsoPlayer)var2).calculateCritChance(this));
         }

         if (var2 instanceof IsoPlayer && (GameServer.bServer || GameClient.bClient)) {
            if (ServerOptions.getInstance().KnockedDownAllowed.getValue()) {
               this.setKnockedDown(var2.isCriticalHit());
            }
         } else {
            this.setKnockedDown(var2.isCriticalHit());
         }

         if (var2 instanceof IsoPlayer) {
            if (!StringUtils.isNullOrEmpty(this.getHitReaction())) {
               this.getActionContext().reportEvent("washitpvpagain");
            }

            this.getActionContext().reportEvent("washitpvp");
            this.setVariable("hitpvp", true);
         } else {
            this.getActionContext().reportEvent("washit");
         }

         String var7;
         if (var3) {
            if (GameServer.bServer) {
               GameServer.addXp((IsoPlayer)var2, PerkFactory.Perks.Strength, 2.0F);
            } else {
               if (!GameClient.bClient) {
                  var2.xp.AddXP(PerkFactory.Perks.Strength, 2.0F);
               }

               this.setHitForce(Math.min(0.5F, this.getHitForce()));
               this.setHitReaction("HitReaction");
               var7 = this.testDotSide(var2);
               this.setHitFromBehind("BEHIND".equals(var7));
            }
         } else {
            if (GameClient.bClient && var2 instanceof IsoPlayer) {
               this.getBodyDamage().splatBloodFloorBig();
            } else {
               this.getBodyDamage().DamageFromWeapon(var1, -1);
            }

            if ("Bite".equals(var6)) {
               var7 = this.testDotSide(var2);
               boolean var8 = var7.equals("FRONT");
               boolean var9 = var7.equals("BEHIND");
               if (var7.equals("RIGHT")) {
                  var6 = var6 + "LEFT";
               }

               if (var7.equals("LEFT")) {
                  var6 = var6 + "RIGHT";
               }

               if (var6 != null && !"".equals(var6)) {
                  this.setHitReaction(var6);
               }
            } else if (!this.isKnockedDown()) {
               this.setHitReaction("HitReaction");
            }

         }
      }
   }

   private HandWeapon getWeapon() {
      if (this.getPrimaryHandItem() instanceof HandWeapon) {
         return (HandWeapon)this.getPrimaryHandItem();
      } else {
         return this.getSecondaryHandItem() instanceof HandWeapon ? (HandWeapon)this.getSecondaryHandItem() : (HandWeapon)InventoryItemFactory.CreateItem("BareHands");
      }
   }

   private void updateMechanicsItems() {
      if (!this.mechanicsItem.isEmpty()) {
         Iterator var1 = this.mechanicsItem.keySet().iterator();
         ArrayList var2 = new ArrayList();

         while(var1.hasNext()) {
            Long var3 = (Long)var1.next();
            Long var4 = (Long)this.mechanicsItem.get(var3);
            if (GameTime.getInstance().getCalender().getTimeInMillis() > var4 + 86400000L) {
               var2.add(var3);
            }
         }

         for(int var5 = 0; var5 < var2.size(); ++var5) {
            this.mechanicsItem.remove(var2.get(var5));
         }

      }
   }

   private void enterExitVehicle() {
      boolean var1 = this.PlayerIndex == 0 && GameKeyboard.isKeyDown("Interact");
      if (var1) {
         this.bUseVehicle = true;
         this.useVehicleDuration += GameTime.instance.getRealworldSecondsSinceLastUpdate();
      }

      if (!this.bUsedVehicle && this.bUseVehicle && (!var1 || this.useVehicleDuration > 0.5F)) {
         this.bUsedVehicle = true;
         if (this.getVehicle() != null) {
            LuaEventManager.triggerEvent("OnUseVehicle", this, this.getVehicle(), this.useVehicleDuration > 0.5F);
         } else {
            for(int var2 = 0; var2 < this.getCell().vehicles.size(); ++var2) {
               BaseVehicle var3 = (BaseVehicle)this.getCell().vehicles.get(var2);
               if (var3.getUseablePart(this) != null) {
                  LuaEventManager.triggerEvent("OnUseVehicle", this, var3, this.useVehicleDuration > 0.5F);
                  break;
               }
            }
         }
      }

      if (!var1) {
         this.bUseVehicle = false;
         this.bUsedVehicle = false;
         this.useVehicleDuration = 0.0F;
      }

   }

   public void checkActionGroup() {
      ActionGroup var1 = this.getActionContext().getGroup();
      ActionGroup var2;
      if (this.getVehicle() == null) {
         var2 = ActionGroup.getActionGroup("player");
         if (var1 != var2) {
            this.advancedAnimator.OnAnimDataChanged(false);
            this.initializeStates();
            this.getActionContext().setGroup(var2);
            this.clearVariable("bEnteringVehicle");
            this.clearVariable("EnterAnimationFinished");
            this.clearVariable("bExitingVehicle");
            this.clearVariable("ExitAnimationFinished");
            this.clearVariable("bSwitchingSeat");
            this.clearVariable("SwitchSeatAnimationFinished");
            this.setHitReaction("");
         }
      } else {
         var2 = ActionGroup.getActionGroup("player-vehicle");
         if (var1 != var2) {
            this.advancedAnimator.OnAnimDataChanged(false);
            this.initializeStates();
            this.getActionContext().setGroup(var2);
         }
      }

   }

   public BaseVehicle getUseableVehicle() {
      if (this.getVehicle() != null) {
         return null;
      } else {
         int var1 = (PZMath.fastfloor(this.getX()) - 4) / 8 - 1;
         int var2 = (PZMath.fastfloor(this.getY()) - 4) / 8 - 1;
         int var3 = (int)Math.ceil((double)((this.getX() + 4.0F) / 8.0F)) + 1;
         int var4 = (int)Math.ceil((double)((this.getY() + 4.0F) / 8.0F)) + 1;

         for(int var5 = var2; var5 < var4; ++var5) {
            for(int var6 = var1; var6 < var3; ++var6) {
               IsoChunk var7 = GameServer.bServer ? ServerMap.instance.getChunk(var6, var5) : IsoWorld.instance.CurrentCell.getChunkForGridSquare(var6 * 8, var5 * 8, 0);
               if (var7 != null) {
                  for(int var8 = 0; var8 < var7.vehicles.size(); ++var8) {
                     BaseVehicle var9 = (BaseVehicle)var7.vehicles.get(var8);
                     if (var9.getUseablePart(this) != null || var9.getBestSeat(this) != -1) {
                        return var9;
                     }
                  }
               }
            }
         }

         return null;
      }
   }

   public Boolean isNearVehicle() {
      if (this.getVehicle() != null) {
         return false;
      } else {
         int var1 = (PZMath.fastfloor(this.getX()) - 4) / 8 - 1;
         int var2 = (PZMath.fastfloor(this.getY()) - 4) / 8 - 1;
         int var3 = (int)Math.ceil((double)((this.getX() + 4.0F) / 8.0F)) + 1;
         int var4 = (int)Math.ceil((double)((this.getY() + 4.0F) / 8.0F)) + 1;

         for(int var5 = var2; var5 < var4; ++var5) {
            for(int var6 = var1; var6 < var3; ++var6) {
               IsoChunk var7 = GameServer.bServer ? ServerMap.instance.getChunk(var6, var5) : IsoWorld.instance.CurrentCell.getChunkForGridSquare(var6 * 10, var5 * 10, 0);
               if (var7 != null) {
                  for(int var8 = 0; var8 < var7.vehicles.size(); ++var8) {
                     BaseVehicle var9 = (BaseVehicle)var7.vehicles.get(var8);
                     if (var9.getScript() != null && var9.DistTo(this) < 3.5F) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   public BaseVehicle getNearVehicle() {
      if (this.getVehicle() != null) {
         return null;
      } else {
         int var1 = (PZMath.fastfloor(this.getX()) - 4) / 8 - 1;
         int var2 = (PZMath.fastfloor(this.getY()) - 4) / 8 - 1;
         int var3 = (int)Math.ceil((double)((this.getX() + 4.0F) / 8.0F)) + 1;
         int var4 = (int)Math.ceil((double)((this.getY() + 4.0F) / 8.0F)) + 1;

         for(int var5 = var2; var5 < var4; ++var5) {
            for(int var6 = var1; var6 < var3; ++var6) {
               IsoChunk var7 = GameServer.bServer ? ServerMap.instance.getChunk(var6, var5) : IsoWorld.instance.CurrentCell.getChunk(var6, var5);
               if (var7 != null) {
                  for(int var8 = 0; var8 < var7.vehicles.size(); ++var8) {
                     BaseVehicle var9 = (BaseVehicle)var7.vehicles.get(var8);
                     if (var9.getScript() != null && PZMath.fastfloor(this.getZ()) == PZMath.fastfloor(var9.getZ()) && (!this.isLocalPlayer() || var9.getTargetAlpha(this.PlayerIndex) != 0.0F) && !(this.DistToSquared((float)PZMath.fastfloor(var9.getX()), (float)PZMath.fastfloor(var9.getY())) >= 16.0F) && PolygonalMap2.instance.intersectLineWithVehicle(this.getX(), this.getY(), this.getX() + this.getForwardDirection().x * 4.0F, this.getY() + this.getForwardDirection().y * 4.0F, var9, tempVector2) && !PolygonalMap2.instance.lineClearCollide(this.getX(), this.getY(), tempVector2.x, tempVector2.y, PZMath.fastfloor(this.getZ()), var9, false, true)) {
                        return var9;
                     }
                  }
               }
            }
         }

         return null;
      }
   }

   private void updateWhileInVehicle() {
      this.bLookingWhileInVehicle = false;
      ActionGroup var1 = this.getActionContext().getGroup();
      ActionGroup var2 = ActionGroup.getActionGroup("player-vehicle");
      if (var1 != var2) {
         this.advancedAnimator.OnAnimDataChanged(false);
         this.initializeStates();
         this.getActionContext().setGroup(var2);
      }

      if (GameClient.bClient && this.getVehicle().getSeat(this) == -1) {
         DebugLog.DetailedInfo.trace("forced " + this.getUsername() + " out of vehicle seat -1");
         this.setVehicle((BaseVehicle)null);
      } else {
         this.dirtyRecalcGridStackTime = 10.0F;
         if (this.getVehicle().isDriver(this)) {
            this.getVehicle().updatePhysics();
            boolean var3 = true;
            if (this.isAiming()) {
               WeaponType var4 = WeaponType.getWeaponType((IsoGameCharacter)this);
               if (var4.equals(WeaponType.firearm)) {
                  var3 = false;
               }
            }

            if (this.getVariableBoolean("isLoading")) {
               var3 = false;
            }

            if (var3) {
               this.getVehicle().updateControls();
            }
         } else {
            BaseVehicle var10 = this.getVehicle();
            if (var10.getDriver() == null && var10.engineSpeed > (double)var10.getScript().getEngineIdleSpeed()) {
               var10.engineSpeed = Math.max(var10.engineSpeed - (double)(50.0F * (GameTime.getInstance().getMultiplier() / 0.8F)), (double)var10.getScript().getEngineIdleSpeed());
            }

            if (GameClient.connection != null) {
               PassengerMap.updatePassenger(this);
            }
         }

         this.fallTime = 0.0F;
         this.bSeenThisFrame = false;
         this.bCouldBeSeenThisFrame = false;
         this.closestZombie = 1000000.0F;
         this.updateAimingDelay();
         this.setBeenMovingFor(this.getBeenMovingFor() - 0.625F * GameTime.getInstance().getMultiplier());
         if (!this.Asleep) {
            float var12 = (float)ZomboidGlobals.SittingEnduranceMultiplier;
            var12 *= 1.0F - this.stats.fatigue * 0.8F;
            var12 *= GameTime.instance.getMultiplier();
            Stats var10000 = this.stats;
            var10000.endurance = (float)((double)var10000.endurance + ZomboidGlobals.ImobileEnduranceReduce * SandboxOptions.instance.getEnduranceRegenMultiplier() * (double)this.getRecoveryMod() * (double)var12);
            this.stats.endurance = PZMath.clamp(this.stats.endurance, 0.0F, 1.0F);
         }

         this.updateToggleToAim();
         if (this.vehicle != null) {
            Vector3f var13 = this.vehicle.getForwardVector(tempVector3f);
            boolean var11 = this.isAimControlActive();
            if (!var11 && this.isCurrentState(IdleState.instance())) {
               this.setForwardDirection(var13.x, var13.z);
               this.getForwardDirection().normalize();
            }

            if (this.lastAngle.x != this.getForwardDirection().x || this.lastAngle.y != this.getForwardDirection().y) {
               this.dirtyRecalcGridStackTime = 10.0F;
            }

            this.DirectionFromVector(this.getForwardDirection());
            AnimationPlayer var5 = this.getAnimationPlayer();
            if (var5 != null && var5.isReady()) {
               var5.setTargetAndCurrentDirection(this.getForwardDirection());
               float var6 = var5.getAngle() + var5.getTwistAngle();
               this.dir = IsoDirections.fromAngle(tempVector2.setLengthAndDirection(var6, 1.0F));
            }

            boolean var14 = false;
            int var7 = this.vehicle.getSeat(this);
            VehiclePart var8 = this.vehicle.getPassengerDoor(var7);
            if (var8 != null) {
               VehicleWindow var9 = var8.findWindow();
               if (var9 != null && !var9.isHittable()) {
                  var14 = true;
               }
            }

            if (var14) {
               this.attackWhileInVehicle();
            } else if (var11) {
               this.bLookingWhileInVehicle = true;
               this.setAngleFromAim();
            } else {
               this.checkJoypadIgnoreAimUntilCentered();
               this.setIsAiming(false);
            }
         }

         this.updateCursorVisibility();
      }
   }

   private void attackWhileInVehicle() {
      this.setIsAiming(false);
      boolean var1 = false;
      boolean var2 = false;
      if (GameWindow.ActivatedJoyPad != null && this.JoypadBind != -1) {
         if (!this.bJoypadMovementActive) {
            return;
         }

         if (this.isChargingLT && !JoypadManager.instance.isLTPressed(this.JoypadBind)) {
            var2 = true;
         } else {
            var1 = this.isCharging && !JoypadManager.instance.isRTPressed(this.JoypadBind);
         }

         float var5 = JoypadManager.instance.getAimingAxisX(this.JoypadBind);
         float var4 = JoypadManager.instance.getAimingAxisY(this.JoypadBind);
         if (this.bJoypadIgnoreAimUntilCentered) {
            if (var5 == 0.0F && var4 == 0.0F) {
               this.bJoypadIgnoreAimUntilCentered = false;
            } else {
               var4 = 0.0F;
               var5 = 0.0F;
            }
         }

         this.setIsAiming(var5 * var5 + var4 * var4 >= 0.09F);
         this.isCharging = this.isAiming() && JoypadManager.instance.isRTPressed(this.JoypadBind);
         this.isChargingLT = this.isAiming() && JoypadManager.instance.isLTPressed(this.JoypadBind);
      } else {
         boolean var3 = this.isAimKeyDown();
         this.setIsAiming(var3);
         this.isCharging = var3;
         if (this.isForceAim()) {
            this.setIsAiming(true);
            this.isCharging = true;
         }

         if (GameKeyboard.isKeyDown("Melee") && this.isAuthorizedHandToHandAction()) {
            var2 = true;
         } else {
            var1 = this.isCharging && GameKeyboard.isKeyDown("Attack/Click");
            if (var1) {
               this.setIsAiming(true);
            }
         }
      }

      if (!this.isCharging && !this.isChargingLT) {
         this.chargeTime = 0.0F;
      }

      if (this.isAiming() && !this.bBannedAttacking && this.CanAttack()) {
         this.chargeTime += GameTime.instance.getMultiplier();
         this.useChargeTime = this.chargeTime;
         this.m_meleePressed = var2;
         this.setAngleFromAim();
         if (var2) {
            this.sprite.Animate = true;
            this.setDoShove(true);
            this.AttemptAttack(this.useChargeTime);
            this.useChargeTime = 0.0F;
            this.chargeTime = 0.0F;
         } else if (var1) {
            this.sprite.Animate = true;
            if (this.getRecoilDelay() <= 0.0F) {
               this.AttemptAttack(this.useChargeTime);
            }

            this.useChargeTime = 0.0F;
            this.chargeTime = 0.0F;
         }

      }
   }

   public void setAngleFromAim() {
      Vector2 var1 = tempVector2;
      if (GameWindow.ActivatedJoyPad != null && this.JoypadBind != -1) {
         this.getControllerAimDir(var1);
      } else {
         var1.set(this.getX(), this.getY());
         int var2 = Mouse.getX();
         int var3 = Mouse.getY();
         var1.x -= IsoUtils.XToIso((float)var2, (float)var3 + 55.0F * this.def.getScaleY(), this.getZ());
         var1.y -= IsoUtils.YToIso((float)var2, (float)var3 + 55.0F * this.def.getScaleY(), this.getZ());
         var1.x = -var1.x;
         var1.y = -var1.y;
         float var4 = IsoUtils.XToIso((float)var2, (float)var3, this.getZ() + 0.45F);
         float var5 = IsoUtils.YToIso((float)var2, (float)var3, this.getZ() + 0.45F);
         this.faceLocationF(var4, var5);
      }

      if (var1.getLengthSquared() > 0.0F) {
         var1.normalize();
         this.DirectionFromVector(var1);
         this.setForwardDirection(var1);
         if (this.lastAngle.x != var1.x || this.lastAngle.y != var1.y) {
            this.lastAngle.x = var1.x;
            this.lastAngle.y = var1.y;
            this.dirtyRecalcGridStackTime = 10.0F;
         }
      }

   }

   private void updateTorchStrength() {
      if (this.getTorchStrength() > 0.0F || this.flickTorch) {
         DrainableComboItem var1 = (DrainableComboItem)Type.tryCastTo(this.getActiveLightItem(), DrainableComboItem.class);
         if (var1 == null) {
            return;
         }

         if (Rand.Next(600 - (int)(0.4F / var1.getCurrentUsesFloat() * 100.0F)) == 0) {
            this.flickTorch = true;
         }

         this.flickTorch = false;
         if (this.flickTorch) {
            if (Rand.Next(6) == 0) {
               var1.setActivated(false);
            } else {
               var1.setActivated(true);
            }

            if (Rand.Next(40) == 0) {
               this.flickTorch = false;
               var1.setActivated(true);
            }
         }
      }

   }

   public IsoCell getCell() {
      return IsoWorld.instance.CurrentCell;
   }

   public void calculateContext() {
      float var1 = this.getX();
      float var2 = this.getY();
      float var3 = this.getX();
      IsoGridSquare[] var4 = new IsoGridSquare[4];
      if (this.dir == IsoDirections.N) {
         var4[2] = this.getCell().getGridSquare((double)(var1 - 1.0F), (double)(var2 - 1.0F), (double)var3);
         var4[1] = this.getCell().getGridSquare((double)var1, (double)(var2 - 1.0F), (double)var3);
         var4[3] = this.getCell().getGridSquare((double)(var1 + 1.0F), (double)(var2 - 1.0F), (double)var3);
      } else if (this.dir == IsoDirections.NE) {
         var4[2] = this.getCell().getGridSquare((double)var1, (double)(var2 - 1.0F), (double)var3);
         var4[1] = this.getCell().getGridSquare((double)(var1 + 1.0F), (double)(var2 - 1.0F), (double)var3);
         var4[3] = this.getCell().getGridSquare((double)(var1 + 1.0F), (double)var2, (double)var3);
      } else if (this.dir == IsoDirections.E) {
         var4[2] = this.getCell().getGridSquare((double)(var1 + 1.0F), (double)(var2 - 1.0F), (double)var3);
         var4[1] = this.getCell().getGridSquare((double)(var1 + 1.0F), (double)var2, (double)var3);
         var4[3] = this.getCell().getGridSquare((double)(var1 + 1.0F), (double)(var2 + 1.0F), (double)var3);
      } else if (this.dir == IsoDirections.SE) {
         var4[2] = this.getCell().getGridSquare((double)(var1 + 1.0F), (double)var2, (double)var3);
         var4[1] = this.getCell().getGridSquare((double)(var1 + 1.0F), (double)(var2 + 1.0F), (double)var3);
         var4[3] = this.getCell().getGridSquare((double)var1, (double)(var2 + 1.0F), (double)var3);
      } else if (this.dir == IsoDirections.S) {
         var4[2] = this.getCell().getGridSquare((double)(var1 + 1.0F), (double)(var2 + 1.0F), (double)var3);
         var4[1] = this.getCell().getGridSquare((double)var1, (double)(var2 + 1.0F), (double)var3);
         var4[3] = this.getCell().getGridSquare((double)(var1 - 1.0F), (double)(var2 + 1.0F), (double)var3);
      } else if (this.dir == IsoDirections.SW) {
         var4[2] = this.getCell().getGridSquare((double)var1, (double)(var2 + 1.0F), (double)var3);
         var4[1] = this.getCell().getGridSquare((double)(var1 - 1.0F), (double)(var2 + 1.0F), (double)var3);
         var4[3] = this.getCell().getGridSquare((double)(var1 - 1.0F), (double)var2, (double)var3);
      } else if (this.dir == IsoDirections.W) {
         var4[2] = this.getCell().getGridSquare((double)(var1 - 1.0F), (double)(var2 + 1.0F), (double)var3);
         var4[1] = this.getCell().getGridSquare((double)(var1 - 1.0F), (double)var2, (double)var3);
         var4[3] = this.getCell().getGridSquare((double)(var1 - 1.0F), (double)(var2 - 1.0F), (double)var3);
      } else if (this.dir == IsoDirections.NW) {
         var4[2] = this.getCell().getGridSquare((double)(var1 - 1.0F), (double)var2, (double)var3);
         var4[1] = this.getCell().getGridSquare((double)(var1 - 1.0F), (double)(var2 - 1.0F), (double)var3);
         var4[3] = this.getCell().getGridSquare((double)var1, (double)(var2 - 1.0F), (double)var3);
      }

      var4[0] = this.current;

      for(int var5 = 0; var5 < 4; ++var5) {
         IsoGridSquare var6 = var4[var5];
         if (var6 == null) {
         }
      }

   }

   public boolean isSafeToClimbOver(IsoDirections var1) {
      IsoGridSquare var2 = null;
      switch (var1) {
         case N:
            var2 = this.getCell().getGridSquare((double)this.getX(), (double)(this.getY() - 1.0F), (double)this.getZ());
            break;
         case NW:
         case SW:
         case SE:
         default:
            return false;
         case W:
            var2 = this.getCell().getGridSquare((double)(this.getX() - 1.0F), (double)this.getY(), (double)this.getZ());
            break;
         case S:
            var2 = this.getCell().getGridSquare((double)this.getX(), (double)(this.getY() + 1.0F), (double)this.getZ());
            break;
         case E:
            var2 = this.getCell().getGridSquare((double)(this.getX() + 1.0F), (double)this.getY(), (double)this.getZ());
      }

      if (var2 == null) {
         return false;
      } else if (var2.Is(IsoFlagType.water)) {
         return false;
      } else {
         return !var2.TreatAsSolidFloor() ? var2.HasStairsBelow() : true;
      }
   }

   public boolean doContext(IsoDirections var1) {
      if (this.isIgnoreContextKey()) {
         return false;
      } else if (this.isBlockMovement()) {
         return false;
      } else if (!this.getCharacterActions().isEmpty()) {
         return false;
      } else if (!this.isSittingOnFurniture() && !this.isSitOnGround()) {
         for(int var2 = 0; var2 < this.getCell().vehicles.size(); ++var2) {
            BaseVehicle var3 = (BaseVehicle)this.getCell().vehicles.get(var2);
            if (var3.getUseablePart(this) != null) {
               return false;
            }
         }

         float var7 = this.getX() - (float)PZMath.fastfloor(this.getX());
         float var8 = this.getY() - (float)PZMath.fastfloor(this.getY());
         IsoDirections var4 = IsoDirections.Max;
         IsoDirections var5 = IsoDirections.Max;
         if (var1 == IsoDirections.NW) {
            if (var8 < var7) {
               if (this.doContextNSWE(IsoDirections.N)) {
                  return true;
               }

               if (this.doContextNSWE(IsoDirections.W)) {
                  return true;
               }

               var4 = IsoDirections.S;
               var5 = IsoDirections.E;
            } else {
               if (this.doContextNSWE(IsoDirections.W)) {
                  return true;
               }

               if (this.doContextNSWE(IsoDirections.N)) {
                  return true;
               }

               var4 = IsoDirections.E;
               var5 = IsoDirections.S;
            }
         } else if (var1 == IsoDirections.NE) {
            var7 = 1.0F - var7;
            if (var8 < var7) {
               if (this.doContextNSWE(IsoDirections.N)) {
                  return true;
               }

               if (this.doContextNSWE(IsoDirections.E)) {
                  return true;
               }

               var4 = IsoDirections.S;
               var5 = IsoDirections.W;
            } else {
               if (this.doContextNSWE(IsoDirections.E)) {
                  return true;
               }

               if (this.doContextNSWE(IsoDirections.N)) {
                  return true;
               }

               var4 = IsoDirections.W;
               var5 = IsoDirections.S;
            }
         } else if (var1 == IsoDirections.SE) {
            var7 = 1.0F - var7;
            var8 = 1.0F - var8;
            if (var8 < var7) {
               if (this.doContextNSWE(IsoDirections.S)) {
                  return true;
               }

               if (this.doContextNSWE(IsoDirections.E)) {
                  return true;
               }

               var4 = IsoDirections.N;
               var5 = IsoDirections.W;
            } else {
               if (this.doContextNSWE(IsoDirections.E)) {
                  return true;
               }

               if (this.doContextNSWE(IsoDirections.S)) {
                  return true;
               }

               var4 = IsoDirections.W;
               var5 = IsoDirections.N;
            }
         } else if (var1 == IsoDirections.SW) {
            var8 = 1.0F - var8;
            if (var8 < var7) {
               if (this.doContextNSWE(IsoDirections.S)) {
                  return true;
               }

               if (this.doContextNSWE(IsoDirections.W)) {
                  return true;
               }

               var4 = IsoDirections.N;
               var5 = IsoDirections.E;
            } else {
               if (this.doContextNSWE(IsoDirections.W)) {
                  return true;
               }

               if (this.doContextNSWE(IsoDirections.S)) {
                  return true;
               }

               var4 = IsoDirections.E;
               var5 = IsoDirections.N;
            }
         } else {
            if (this.doContextNSWE(var1)) {
               return true;
            }

            var4 = var1.Rot180();
         }

         IsoObject var6;
         if (var4 != IsoDirections.Max) {
            var6 = this.getContextDoorOrWindowOrWindowFrame(var4);
            if (var6 != null) {
               this.doContextDoorOrWindowOrWindowFrame(var4, var6);
               return true;
            }
         }

         if (var5 != IsoDirections.Max) {
            var6 = this.getContextDoorOrWindowOrWindowFrame(var5);
            if (var6 != null) {
               this.doContextDoorOrWindowOrWindowFrame(var5, var6);
               return true;
            }
         }

         LuaEventManager.triggerEvent("OnContextKey", this, BoxedStaticValues.toDouble((double)PZMath.fastfloor(this.timePressedContext * 1000.0F)));
         return false;
      } else {
         LuaEventManager.triggerEvent("OnContextKey", this, BoxedStaticValues.toDouble((double)PZMath.fastfloor(this.timePressedContext * 1000.0F)));
         return true;
      }
   }

   private boolean doContextNSWE(IsoDirections var1) {
      assert var1 == IsoDirections.N || var1 == IsoDirections.S || var1 == IsoDirections.W || var1 == IsoDirections.E;

      if (this.current == null) {
         return false;
      } else if (var1 == IsoDirections.N && this.current.Is(IsoFlagType.climbSheetN) && this.canClimbSheetRope(this.current)) {
         this.triggerContextualAction("ClimbSheetRope", this.current, false);
         return true;
      } else if (var1 == IsoDirections.S && this.current.Is(IsoFlagType.climbSheetS) && this.canClimbSheetRope(this.current)) {
         this.triggerContextualAction("ClimbSheetRope", this.current, false);
         return true;
      } else if (var1 == IsoDirections.W && this.current.Is(IsoFlagType.climbSheetW) && this.canClimbSheetRope(this.current)) {
         this.triggerContextualAction("ClimbSheetRope", this.current, false);
         return true;
      } else if (var1 == IsoDirections.E && this.current.Is(IsoFlagType.climbSheetE) && this.canClimbSheetRope(this.current)) {
         this.triggerContextualAction("ClimbSheetRope", this.current, false);
         return true;
      } else {
         IsoGridSquare var2 = this.current.nav[var1.index()];
         boolean var3 = IsoWindow.isTopOfSheetRopeHere(var2) && this.canClimbDownSheetRope(var2);
         IsoObject var4 = this.getContextDoorOrWindowOrWindowFrame(var1);
         if (var4 == null || this.isGrappling() && !(var4 instanceof IsoWindow)) {
            IsoGridSquare var7;
            if (GameKeyboard.isKeyDown(42) && this.current != null && this.ticksSincePressedMovement > 15.0F) {
               IsoObject var5 = this.current.getDoor(true);
               if (var5 instanceof IsoDoor && ((IsoDoor)var5).isFacingSheet(this)) {
                  ((IsoDoor)var5).toggleCurtain();
                  return true;
               }

               IsoObject var6 = this.current.getDoor(false);
               if (var6 instanceof IsoDoor && ((IsoDoor)var6).isFacingSheet(this)) {
                  ((IsoDoor)var6).toggleCurtain();
                  return true;
               }

               IsoObject var8;
               if (var1 == IsoDirections.E) {
                  var7 = IsoWorld.instance.CurrentCell.getGridSquare((double)(this.getX() + 1.0F), (double)this.getY(), (double)this.getZ());
                  var8 = var7 != null ? var7.getDoor(true) : null;
                  if (var8 instanceof IsoDoor && ((IsoDoor)var8).isFacingSheet(this)) {
                     ((IsoDoor)var8).toggleCurtain();
                     return true;
                  }
               }

               if (var1 == IsoDirections.S) {
                  var7 = IsoWorld.instance.CurrentCell.getGridSquare((double)this.getX(), (double)(this.getY() + 1.0F), (double)this.getZ());
                  var8 = var7 != null ? var7.getDoor(false) : null;
                  if (var8 instanceof IsoDoor && ((IsoDoor)var8).isFacingSheet(this)) {
                     ((IsoDoor)var8).toggleCurtain();
                     return true;
                  }
               }
            }

            boolean var9 = this.isSafeToClimbOver(var1);
            if (this.getZ() > 0.0F && var3) {
               var9 = true;
            }

            if (this.timePressedContext < 0.5F && !var9) {
               return false;
            } else if (this.ignoreAutoVault) {
               return false;
            } else if (var1 == IsoDirections.N && this.getCurrentSquare().Is(IsoFlagType.HoppableN)) {
               this.triggerContextualAction("ClimbOverFence", this.getCurrentSquare().getHoppable(true));
               return true;
            } else if (var1 == IsoDirections.W && this.getCurrentSquare().Is(IsoFlagType.HoppableW)) {
               this.triggerContextualAction("ClimbOverFence", this.getCurrentSquare().getHoppable(false));
               return true;
            } else {
               IsoGridSquare var10 = this.getCurrentSquare().getAdjacentSquare(IsoDirections.S);
               if (var1 == IsoDirections.S && var10 != null && var10.Is(IsoFlagType.HoppableN)) {
                  this.triggerContextualAction("ClimbOverFence", var10.getHoppable(true));
                  return true;
               } else {
                  var7 = this.getCurrentSquare().getAdjacentSquare(IsoDirections.E);
                  if (var1 == IsoDirections.E && var7 != null && var7.Is(IsoFlagType.HoppableW)) {
                     this.triggerContextualAction("ClimbOverFence", var7.getHoppable(false));
                     return true;
                  } else {
                     return this.climbOverWall(var1);
                  }
               }
            }
         } else {
            this.doContextDoorOrWindowOrWindowFrame(var1, var4);
            return true;
         }
      }
   }

   public IsoObject getContextDoorOrWindowOrWindowFrame(IsoDirections var1) {
      if (this.current != null && var1 != null) {
         IsoGridSquare var2 = this.current.nav[var1.index()];
         IsoObject var3 = null;
         switch (var1) {
            case N:
               var3 = this.current.getOpenDoor(var1);
               if (var3 != null) {
                  return var3;
               }

               var3 = this.current.getDoorOrWindowOrWindowFrame(var1, true);
               if (var3 != null) {
                  return var3;
               }

               var3 = this.current.getDoor(true);
               if (var3 != null) {
                  return var3;
               }

               if (var2 != null && !this.current.isBlockedTo(var2)) {
                  var3 = var2.getOpenDoor(IsoDirections.S);
               }
            case NW:
            case SW:
            case SE:
            default:
               break;
            case W:
               var3 = this.current.getOpenDoor(var1);
               if (var3 != null) {
                  return var3;
               }

               var3 = this.current.getDoorOrWindowOrWindowFrame(var1, true);
               if (var3 != null) {
                  return var3;
               }

               var3 = this.current.getDoor(false);
               if (var3 != null) {
                  return var3;
               }

               if (var2 != null && !this.current.isBlockedTo(var2)) {
                  var3 = var2.getOpenDoor(IsoDirections.E);
               }
               break;
            case S:
               var3 = this.current.getOpenDoor(var1);
               if (var3 != null) {
                  return var3;
               }

               if (var2 != null) {
                  boolean var4 = this.current.isBlockedTo(var2);
                  var3 = var2.getDoorOrWindowOrWindowFrame(IsoDirections.N, var4);
                  if (var3 != null) {
                     return var3;
                  }

                  var3 = var2.getDoor(true);
               }
               break;
            case E:
               var3 = this.current.getOpenDoor(var1);
               if (var3 != null) {
                  return var3;
               }

               if (var2 != null) {
                  boolean var5 = this.current.isBlockedTo(var2);
                  var3 = var2.getDoorOrWindowOrWindowFrame(IsoDirections.W, var5);
                  if (var3 != null) {
                     return var3;
                  }

                  var3 = var2.getDoor(false);
               }
         }

         return var3;
      } else {
         return null;
      }
   }

   private void doContextDoorOrWindowOrWindowFrame(IsoDirections var1, IsoObject var2) {
      IsoGridSquare var3 = this.current.nav[var1.index()];
      boolean var4 = IsoWindow.isTopOfSheetRopeHere(var3) && this.canClimbDownSheetRope(var3);
      if (var2 instanceof IsoDoor) {
         this.doContextDoor(var1, (IsoDoor)var2);
      } else if (var2 instanceof IsoThumpable && ((IsoThumpable)var2).isDoor()) {
         this.doContextThumpableDoor(var1, (IsoThumpable)var2);
      } else if (var2 instanceof IsoWindow && !var2.getSquare().getProperties().Is(IsoFlagType.makeWindowInvincible)) {
         this.doContextWindow(var1, (IsoWindow)var2, var4);
      } else if (var2 instanceof IsoThumpable && !var2.getSquare().getProperties().Is(IsoFlagType.makeWindowInvincible)) {
         this.doContextThumpableWindow(var1, (IsoThumpable)var2, var4);
      } else if (var2 instanceof IsoWindowFrame) {
         IsoWindowFrame var5 = (IsoWindowFrame)var2;
         this.doContextWindowFrame(var1, var5, var4);
      }

   }

   private void doContextWindowFrame(IsoDirections var1, IsoWindowFrame var2, boolean var3) {
      if (GameKeyboard.isKeyDown(42)) {
         IsoCurtain var4 = var2.getCurtain();
         if (var4 != null && this.current != null && !var4.getSquare().isBlockedTo(this.current)) {
            this.triggerContextualAction(var4.IsOpen() ? "CloseCurtain" : "OpenCurtain", var4);
         }
      } else if ((this.timePressedContext >= 0.5F || this.isSafeToClimbOver(var1) || var3) && var2.canClimbThrough(this)) {
         if (this.isGrappling()) {
            this.throwGrappledTargetOutWindow(var2);
         } else {
            this.triggerContextualAction("ClimbThroughWindow", var2);
         }
      }

   }

   private void doContextThumpableWindow(IsoDirections var1, IsoThumpable var2, boolean var3) {
      if (GameKeyboard.isKeyDown(42)) {
         IsoCurtain var4 = var2.HasCurtains();
         if (var4 != null && this.current != null && !var4.getSquare().isBlockedTo(this.current)) {
            this.triggerContextualAction(var4.IsOpen() ? "CloseCurtain" : "OpenCurtain", var4);
         }
      } else if (this.timePressedContext >= 0.5F) {
         if (var2.canClimbThrough(this)) {
            if (this.isGrappling()) {
               this.throwGrappledTargetOutWindow(var2);
            } else {
               this.triggerContextualAction("ClimbThroughWindow", var2);
            }
         }
      } else {
         if (!this.isSafeToClimbOver(var1) && !var2.getSquare().haveSheetRope && !var3) {
            return;
         }

         if (var2.canClimbThrough(this)) {
            if (this.isGrappling()) {
               this.throwGrappledTargetOutWindow(var2);
            } else {
               this.triggerContextualAction("ClimbThroughWindow", var2);
            }
         }
      }

   }

   private void doContextWindow(IsoDirections var1, IsoWindow var2, boolean var3) {
      if (GameKeyboard.isKeyDown(42)) {
         IsoCurtain var4 = var2.HasCurtains();
         if (var4 != null && this.current != null && !var4.getSquare().isBlockedTo(this.current)) {
            this.triggerContextualAction(var4.IsOpen() ? "CloseCurtain" : "OpenCurtain", var4);
         }
      } else if (this.timePressedContext >= 0.5F) {
         if (var2.canClimbThrough(this)) {
            if (this.isGrappling()) {
               this.throwGrappledTargetOutWindow(var2);
            } else {
               this.triggerContextualAction("ClimbThroughWindow", var2);
            }
         } else if (!var2.PermaLocked && !var2.isBarricaded() && !var2.IsOpen() && !var2.isDestroyed()) {
            this.triggerContextualAction("OpenWindow", var2);
         }
      } else if (var2.Health > 0 && !var2.isDestroyed()) {
         IsoBarricade var5 = var2.getBarricadeForCharacter(this);
         if (!var2.open && var5 == null) {
            this.triggerContextualAction("OpenWindow", var2);
         } else if (var5 == null) {
            this.triggerContextualAction("CloseWindow", var2);
         }
      } else if (var2.isGlassRemoved()) {
         if (!this.isSafeToClimbOver(var1) && !var2.getSquare().haveSheetRope && !var3) {
            return;
         }

         if (!var2.isBarricaded()) {
            if (this.isGrappling()) {
               this.throwGrappledTargetOutWindow(var2);
            } else {
               this.triggerContextualAction("ClimbThroughWindow", var2);
            }
         }
      }

   }

   private void doContextThumpableDoor(IsoDirections var1, IsoThumpable var2) {
      if (this.timePressedContext >= 0.5F) {
         if (var2.isHoppable() && !this.isIgnoreAutoVault()) {
            this.triggerContextualAction("ClimbOverFence", var2);
         } else {
            this.triggerContextualAction(var2.IsOpen() ? "CloseDoor" : "OpenDoor", var2);
         }
      } else {
         this.triggerContextualAction(var2.IsOpen() ? "CloseDoor" : "OpenDoor", var2);
      }

   }

   private void doContextDoor(IsoDirections var1, IsoDoor var2) {
      if (GameKeyboard.isKeyDown(42) && var2.HasCurtains() != null && var2.isFacingSheet(this) && this.ticksSincePressedMovement > 15.0F) {
         this.triggerContextualAction(var2.isCurtainOpen() ? "CloseCurtain" : "OpenCurtain", var2);
      } else if (this.timePressedContext >= 0.5F) {
         if (var2.isHoppable() && !this.isIgnoreAutoVault()) {
            this.triggerContextualAction("ClimbOverFence", var2);
         } else {
            this.triggerContextualAction(var2.IsOpen() ? "CloseDoor" : "OpenDoor", var2);
         }
      } else {
         this.triggerContextualAction(var2.IsOpen() ? "CloseDoor" : "OpenDoor", var2);
      }

   }

   public boolean hopFence(IsoDirections var1, boolean var2) {
      float var4 = this.getX() - (float)PZMath.fastfloor(this.getX());
      float var5 = this.getY() - (float)PZMath.fastfloor(this.getY());
      if (var1 == IsoDirections.NW) {
         if (var5 < var4) {
            return this.hopFence(IsoDirections.N, var2) ? true : this.hopFence(IsoDirections.W, var2);
         } else {
            return this.hopFence(IsoDirections.W, var2) ? true : this.hopFence(IsoDirections.N, var2);
         }
      } else if (var1 == IsoDirections.NE) {
         var4 = 1.0F - var4;
         if (var5 < var4) {
            return this.hopFence(IsoDirections.N, var2) ? true : this.hopFence(IsoDirections.E, var2);
         } else {
            return this.hopFence(IsoDirections.E, var2) ? true : this.hopFence(IsoDirections.N, var2);
         }
      } else if (var1 == IsoDirections.SE) {
         var4 = 1.0F - var4;
         var5 = 1.0F - var5;
         if (var5 < var4) {
            return this.hopFence(IsoDirections.S, var2) ? true : this.hopFence(IsoDirections.E, var2);
         } else {
            return this.hopFence(IsoDirections.E, var2) ? true : this.hopFence(IsoDirections.S, var2);
         }
      } else if (var1 == IsoDirections.SW) {
         var5 = 1.0F - var5;
         if (var5 < var4) {
            return this.hopFence(IsoDirections.S, var2) ? true : this.hopFence(IsoDirections.W, var2);
         } else {
            return this.hopFence(IsoDirections.W, var2) ? true : this.hopFence(IsoDirections.S, var2);
         }
      } else if (this.current == null) {
         return false;
      } else {
         IsoGridSquare var6 = this.current.nav[var1.index()];
         if (var6 != null && !var6.Is(IsoFlagType.water)) {
            if (var1 == IsoDirections.N && this.getCurrentSquare().Is(IsoFlagType.HoppableN)) {
               if (var2) {
                  return true;
               } else {
                  this.triggerContextualAction("ClimbOverFence", this.getCurrentSquare().getHoppable(true));
                  return true;
               }
            } else if (var1 == IsoDirections.W && this.getCurrentSquare().Is(IsoFlagType.HoppableW)) {
               if (var2) {
                  return true;
               } else {
                  this.triggerContextualAction("ClimbOverFence", this.getCurrentSquare().getHoppable(false));
                  return true;
               }
            } else {
               IsoGridSquare var7 = this.getCurrentSquare().getAdjacentSquare(IsoDirections.S);
               if (var1 == IsoDirections.S && var7 != null && var7.Is(IsoFlagType.HoppableN)) {
                  if (var2) {
                     return true;
                  } else {
                     this.triggerContextualAction("ClimbOverFence", var7.getHoppable(true));
                     return true;
                  }
               } else {
                  IsoGridSquare var8 = this.getCurrentSquare().getAdjacentSquare(IsoDirections.E);
                  if (var1 == IsoDirections.E && var8 != null && var8.Is(IsoFlagType.HoppableW)) {
                     if (var2) {
                        return true;
                     } else {
                        this.triggerContextualAction("ClimbOverFence", var8.getHoppable(false));
                        return true;
                     }
                  } else {
                     return false;
                  }
               }
            }
         } else {
            return false;
         }
      }
   }

   public boolean canClimbOverWall(IsoDirections var1) {
      if (this.isSprinting()) {
         return false;
      } else if (this.isSafeToClimbOver(var1) && this.current != null) {
         if (this.current.haveRoof) {
            return false;
         } else if (this.current.getBuilding() != null) {
            return false;
         } else {
            IsoGridSquare var2 = IsoWorld.instance.CurrentCell.getGridSquare(this.current.x, this.current.y, this.current.z + 1);
            if (var2 != null && var2.HasSlopedRoof() && !var2.HasEave()) {
               return false;
            } else {
               IsoGridSquare var3 = this.current.nav[var1.index()];
               if (var3.haveRoof) {
                  return false;
               } else if (!var3.isSolid() && !var3.isSolidTrans()) {
                  if (var3.getBuilding() != null) {
                     return false;
                  } else {
                     IsoGridSquare var4 = IsoWorld.instance.CurrentCell.getGridSquare(var3.x, var3.y, var3.z + 1);
                     if (var4 != null && var4.HasSlopedRoof() && !var4.HasEave()) {
                        return false;
                     } else {
                        switch (var1) {
                           case N:
                              if (this.current.Is(IsoFlagType.CantClimb)) {
                                 return false;
                              }

                              if (!this.current.Has(IsoObjectType.wall)) {
                                 return false;
                              }

                              if (!this.current.Is(IsoFlagType.collideN)) {
                                 return false;
                              }

                              if (this.current.Is(IsoFlagType.HoppableN)) {
                                 return false;
                              }

                              if (var2 != null && var2.Is(IsoFlagType.collideN)) {
                                 return false;
                              }
                              break;
                           case NW:
                           case SW:
                           case SE:
                           default:
                              return false;
                           case W:
                              if (this.current.Is(IsoFlagType.CantClimb)) {
                                 return false;
                              }

                              if (!this.current.Has(IsoObjectType.wall)) {
                                 return false;
                              }

                              if (!this.current.Is(IsoFlagType.collideW)) {
                                 return false;
                              }

                              if (this.current.Is(IsoFlagType.HoppableW)) {
                                 return false;
                              }

                              if (var2 != null && var2.Is(IsoFlagType.collideW)) {
                                 return false;
                              }
                              break;
                           case S:
                              if (var3.Is(IsoFlagType.CantClimb)) {
                                 return false;
                              }

                              if (!var3.Has(IsoObjectType.wall)) {
                                 return false;
                              }

                              if (!var3.Is(IsoFlagType.collideN)) {
                                 return false;
                              }

                              if (var3.Is(IsoFlagType.HoppableN)) {
                                 return false;
                              }

                              if (var4 != null && var4.Is(IsoFlagType.collideN)) {
                                 return false;
                              }
                              break;
                           case E:
                              if (var3.Is(IsoFlagType.CantClimb)) {
                                 return false;
                              }

                              if (!var3.Has(IsoObjectType.wall)) {
                                 return false;
                              }

                              if (!var3.Is(IsoFlagType.collideW)) {
                                 return false;
                              }

                              if (var3.Is(IsoFlagType.HoppableW)) {
                                 return false;
                              }

                              if (var4 != null && var4.Is(IsoFlagType.collideW)) {
                                 return false;
                              }
                        }

                        return IsoWindow.canClimbThroughHelper(this, this.current, var3, var1 == IsoDirections.N || var1 == IsoDirections.S);
                     }
                  }
               } else {
                  return false;
               }
            }
         }
      } else {
         return false;
      }
   }

   public boolean climbOverWall(IsoDirections var1) {
      if (!this.canClimbOverWall(var1)) {
         return false;
      } else {
         this.dropHeavyItems();
         ClimbOverWallState.instance().setParams(this, var1);
         this.getActionContext().reportEvent("EventClimbWall");
         return true;
      }
   }

   private void updateSleepingPillsTaken() {
      if (this.getSleepingPillsTaken() > 0 && this.lastPillsTaken > 0L && GameTime.instance.Calender.getTimeInMillis() - this.lastPillsTaken > 7200000L) {
         this.setSleepingPillsTaken(this.getSleepingPillsTaken() - 1);
      }

   }

   public boolean AttemptAttack() {
      return this.DoAttack(this.useChargeTime);
   }

   public boolean DoAttack(float var1) {
      return this.DoAttack(var1, false, (String)null);
   }

   public boolean DoAttack(float var1, boolean var2, String var3) {
      if (!this.isAuthorizedHandToHandAction()) {
         return false;
      } else {
         this.setForceShove(var2);
         this.setClickSound(var3);
         this.pressedAttack();
         return false;
      }
   }

   public int getPlayerNum() {
      return this.PlayerIndex;
   }

   public void updateLOS() {
      this.spottedList.clear();
      this.stats.NumVisibleZombies = 0;
      this.stats.LastNumChasingZombies = this.stats.NumChasingZombies;
      this.stats.NumChasingZombies = 0;
      this.stats.MusicZombiesTargeting_DistantNotMoving = 0;
      this.stats.MusicZombiesTargeting_NearbyNotMoving = 0;
      this.stats.MusicZombiesTargeting_DistantMoving = 0;
      this.stats.MusicZombiesTargeting_NearbyMoving = 0;
      this.stats.MusicZombiesVisible = 0;
      this.NumSurvivorsInVicinity = 0;
      if (this.getCurrentSquare() != null) {
         boolean var1 = GameServer.bServer;
         boolean var2 = GameClient.bClient;
         int var3 = this.PlayerIndex;
         IsoPlayer var4 = getInstance();
         float var5 = this.getX();
         float var6 = this.getY();
         float var7 = this.getZ();
         int var8 = 0;
         int var9 = 0;
         int var10 = this.getCell().getObjectList().size();

         for(int var11 = 0; var11 < var10; ++var11) {
            IsoMovingObject var12 = (IsoMovingObject)this.getCell().getObjectList().get(var11);
            if (!(var12 instanceof IsoPhysicsObject) && !(var12 instanceof BaseVehicle)) {
               if (var12 == this) {
                  this.spottedList.add(var12);
               } else {
                  float var13 = var12.getX();
                  float var14 = var12.getY();
                  float var15 = var12.getZ();
                  float var16 = IsoUtils.DistanceTo(var13, var14, var5, var6);
                  if (var16 < 20.0F) {
                     ++var8;
                  }

                  IsoGridSquare var17 = var12.getCurrentSquare();
                  if (var17 != null) {
                     if (this.isSeeEveryone()) {
                        var12.setAlphaAndTarget(var3, 1.0F);
                     } else {
                        IsoGameCharacter var18 = (IsoGameCharacter)Type.tryCastTo(var12, IsoGameCharacter.class);
                        IsoPlayer var19 = (IsoPlayer)Type.tryCastTo(var18, IsoPlayer.class);
                        IsoZombie var20 = (IsoZombie)Type.tryCastTo(var18, IsoZombie.class);
                        if (var20 != null && var20.isReanimatedForGrappleOnly()) {
                           IsoMovingObject var28 = (IsoMovingObject)Type.tryCastTo(var20.getGrappledBy(), IsoMovingObject.class);
                           var12.setAlphaAndTarget(var28 == null ? 1.0F : var28.getTargetAlpha());
                        } else if (GameClient.bClient && var4 != null && var12 != var4 && var18 != null && var18.isInvisible() && !var4.role.haveCapability(Capability.SeesInvisiblePlayers)) {
                           var18.setAlphaAndTarget(var3, 0.0F);
                        } else {
                           boolean var21;
                           if (var1) {
                              var21 = ServerLOS.instance.isCouldSee(this, var17);
                           } else {
                              var21 = var17.isCouldSee(var3);
                           }

                           boolean var22;
                           if (var2 && var19 != null) {
                              var22 = true;
                           } else if (!var1) {
                              var22 = var17.isCanSee(var3);
                           } else {
                              var22 = var21;
                           }

                           if (!this.isAsleep() && (var22 || var16 < this.getSeeNearbyCharacterDistance() && var21)) {
                              this.TestZombieSpotPlayer(var12);
                              if (var18 != null && var18.IsVisibleToPlayer[var3]) {
                                 if (var18 instanceof IsoSurvivor) {
                                    ++this.NumSurvivorsInVicinity;
                                 }

                                 if (var20 != null) {
                                    this.lastSeenZombieTime = 0.0;
                                    if (var15 >= var7 - 1.0F && var16 < 7.0F && !var20.Ghost && !var20.isFakeDead() && var17.getRoom() == this.getCurrentSquare().getRoom()) {
                                       this.TicksSinceSeenZombie = 0;
                                       ++this.stats.NumVisibleZombies;
                                    }

                                    if (var16 < 3.0F) {
                                       ++var9;
                                    }

                                    if (!var20.isSceneCulled()) {
                                       ++this.stats.MusicZombiesVisible;
                                       if (var20.target == this) {
                                          if (!var20.isCurrentState(WalkTowardState.instance()) && !var20.isCurrentState(LungeState.instance()) && !var20.isCurrentState(PathFindState.instance())) {
                                             if (this.DistToProper(var20) >= 10.0F) {
                                                ++this.stats.MusicZombiesTargeting_DistantNotMoving;
                                             } else {
                                                ++this.stats.MusicZombiesTargeting_NearbyNotMoving;
                                             }
                                          } else if (this.DistToProper(var20) >= 10.0F) {
                                             ++this.stats.MusicZombiesTargeting_DistantMoving;
                                          } else {
                                             ++this.stats.MusicZombiesTargeting_NearbyMoving;
                                          }
                                       }
                                    }
                                 }

                                 this.spottedList.add(var18);
                                 if (!(var19 instanceof IsoPlayer) && !this.bRemote) {
                                    if (var19 != null && var19 != var4) {
                                       var19.setTargetAlpha(var3, 1.0F);
                                    } else {
                                       var18.setTargetAlpha(var3, 1.0F);
                                    }
                                 }

                                 float var23 = 4.0F;
                                 if (this.stats.NumVisibleZombies > 4) {
                                    var23 = 7.0F;
                                 }

                                 if (var16 < var23 && var18 instanceof IsoZombie && PZMath.fastfloor(var15) == PZMath.fastfloor(var7) && !this.isGhostMode() && !var2) {
                                    GameTime.instance.setMultiplier(1.0F);
                                    if (!var1) {
                                       UIManager.getSpeedControls().SetCurrentGameSpeed(1);
                                    }
                                 }

                                 if (var16 < var23 && var18 instanceof IsoZombie && PZMath.fastfloor(var15) == PZMath.fastfloor(var7) && !this.LastSpotted.contains(var18)) {
                                    Stats var10000 = this.stats;
                                    var10000.NumVisibleZombies += 2;
                                 }
                              }
                           } else {
                              if (var12 != instance) {
                                 var12.setTargetAlpha(var3, 0.0F);
                              }

                              if (var21) {
                                 this.TestZombieSpotPlayer(var12);
                              }
                           }

                           if (var16 < 2.0F && var12.getTargetAlpha(var3) == 1.0F && !this.bRemote) {
                              var12.setAlpha(var3, 1.0F);
                           }
                        }
                     }
                  }
               }
            }
         }

         if (this.isAlive() && var9 > 0 && this.stats.LastVeryCloseZombies == 0 && this.stats.NumVisibleZombies > 0 && this.stats.LastNumVisibleZombies == 0 && this.timeSinceLastStab >= 600.0F) {
            this.timeSinceLastStab = 0.0F;
            long var24 = this.getEmitter().playSoundImpl("ZombieSurprisedPlayer", (IsoObject)null);
            this.getEmitter().setVolume(var24, (float)Core.getInstance().getOptionJumpScareVolume() / 10.0F);
         }

         if (this.stats.NumVisibleZombies > 0) {
            this.timeSinceLastStab = 0.0F;
         }

         if (this.timeSinceLastStab < 600.0F) {
            this.timeSinceLastStab += GameTime.getInstance().getThirtyFPSMultiplier();
         }

         float var25 = (float)var8 / 20.0F;
         if (var25 > 1.0F) {
            var25 = 1.0F;
         }

         var25 *= 0.6F;
         SoundManager.instance.BlendVolume(MainScreenState.ambient, var25);
         int var26 = 0;

         for(int var27 = 0; var27 < this.spottedList.size(); ++var27) {
            if (!this.LastSpotted.contains(this.spottedList.get(var27))) {
               this.LastSpotted.add((IsoMovingObject)this.spottedList.get(var27));
            }

            if (this.spottedList.get(var27) instanceof IsoZombie) {
               ++var26;
            }
         }

         if (this.ClearSpottedTimer <= 0 && var26 == 0) {
            this.LastSpotted.clear();
            this.ClearSpottedTimer = 1000;
         } else {
            --this.ClearSpottedTimer;
         }

         this.stats.LastNumVisibleZombies = this.stats.NumVisibleZombies;
         this.stats.LastVeryCloseZombies = var9;
      }
   }

   public float getSeeNearbyCharacterDistance() {
      return (3.5F - this.stats.getFatigue() - this.stats.Drunkenness * 0.01F) * this.getWornItemsVisionMultiplier();
   }

   private boolean checkSpottedPLayerTimer(IsoPlayer var1) {
      if (!var1.spottedByPlayer) {
         return false;
      } else {
         if (this.spottedPlayerTimer.containsKey(var1.getRemoteID())) {
            this.spottedPlayerTimer.put(var1.getRemoteID(), (Integer)this.spottedPlayerTimer.get(var1.getRemoteID()) + 1);
         } else {
            this.spottedPlayerTimer.put(var1.getRemoteID(), 1);
         }

         if ((Integer)this.spottedPlayerTimer.get(var1.getRemoteID()) > 100) {
            var1.spottedByPlayer = false;
            return false;
         } else {
            return true;
         }
      }
   }

   public boolean checkCanSeeClient(UdpConnection var1) {
      if (var1.role.haveCapability(Capability.SeesInvisiblePlayers)) {
         return true;
      } else {
         return !this.isInvisible();
      }
   }

   public boolean checkCanSeeClient(IsoPlayer var1) {
      Vector2 var2 = tempVector2_1.set(this.getX(), this.getY());
      Vector2 var3 = tempVector2_2.set(var1.getX(), var1.getY());
      var3.x -= var2.x;
      var3.y -= var2.y;
      Vector2 var4 = this.getForwardDirection();
      var3.normalize();
      var4.normalize();
      var4.normalize();
      float var5 = var3.dot(var4);
      if (GameClient.bClient && var1 != this && this.isLocalPlayer()) {
         if (!this.isAccessLevel("None") && this.canSeeAll) {
            var1.spottedByPlayer = true;
            return true;
         } else {
            float var6 = this.current == null ? 0.0F : var1.getCurrentSquare().DistTo(this.getCurrentSquare());
            if (var6 <= 2.0F) {
               var1.spottedByPlayer = true;
               return true;
            } else if (ServerOptions.getInstance().HidePlayersBehindYou.getValue() && (double)var5 < -0.5) {
               return this.checkSpottedPLayerTimer(var1);
            } else if (var1.isGhostMode() && this.isAccessLevel("None")) {
               var1.spottedByPlayer = false;
               return false;
            } else {
               IsoGridSquare.ILighting var7 = var1.getCurrentSquare().lighting[this.getPlayerNum()];
               if (!var7.bCouldSee()) {
                  return this.checkSpottedPLayerTimer(var1);
               } else if (var1.isSneaking() && ServerOptions.getInstance().SneakModeHideFromOtherPlayers.getValue() && !var1.isSprinting()) {
                  if (var6 > 30.0F) {
                     var1.spottedByPlayer = false;
                  }

                  if (var1.spottedByPlayer) {
                     return true;
                  } else {
                     float var8 = (float)(Math.pow((double)Math.max(40.0F - var6, 0.0F), 3.0) / 12000.0);
                     float var9 = 1.0F - (float)var1.remoteSneakLvl / 10.0F * 0.9F + 0.3F;
                     float var10 = 1.0F;
                     if (var5 < 0.8F) {
                        var10 = 0.3F;
                     }

                     if (var5 < 0.6F) {
                        var10 = 0.05F;
                     }

                     float var11 = (var7.lightInfo().getR() + var7.lightInfo().getG() + var7.lightInfo().getB()) / 3.0F;
                     float var12 = (1.0F - (float)this.getMoodles().getMoodleLevel(MoodleType.Tired) / 5.0F) * 0.7F + 0.3F;
                     float var13 = (1.0F - (float)this.getMoodles().getMoodleLevel(MoodleType.Drunk) / 5.0F) * 0.7F + 0.3F;
                     float var14 = 0.1F;
                     if (var1.isPlayerMoving()) {
                        var14 = 0.35F;
                     }

                     if (var1.isRunning()) {
                        var14 = 1.0F;
                     }

                     ArrayList var15 = PolygonalMap2.instance.getPointInLine(var1.getX(), var1.getY(), this.getX(), this.getY(), PZMath.fastfloor(this.getZ()));
                     IsoGridSquare var16 = null;
                     float var17 = 0.0F;
                     float var18 = 0.0F;
                     boolean var19 = false;
                     boolean var20 = false;

                     float var23;
                     for(int var21 = 0; var21 < var15.size(); ++var21) {
                        Point var22 = (Point)var15.get(var21);
                        var16 = IsoCell.getInstance().getGridSquare((double)var22.x, (double)var22.y, (double)this.getZ());
                        if (var16 != null) {
                           var23 = var16.getGridSneakModifier(false);
                           if (var23 > 1.0F) {
                              var19 = true;
                              if (var16.getProperties().Is(IsoFlagType.windowN) || var16.getProperties().Is(IsoFlagType.windowW)) {
                                 var20 = true;
                              }
                              break;
                           }

                           int var24 = 0;

                           while(var24 < var16.getObjects().size()) {
                              IsoObject var25 = (IsoObject)var16.getObjects().get(var24);
                              if (!var25.getSprite().getProperties().Is(IsoFlagType.solidtrans) && !var25.getSprite().getProperties().Is(IsoFlagType.solid)) {
                                 if (!var25.getSprite().getProperties().Is(IsoFlagType.windowN) && !var25.getSprite().getProperties().Is(IsoFlagType.windowW)) {
                                    ++var24;
                                    continue;
                                 }

                                 var19 = true;
                                 var20 = true;
                                 break;
                              }

                              var19 = true;
                              break;
                           }

                           if (var19) {
                              break;
                           }
                        }
                     }

                     float var26 = 1.0F;
                     if (var20 && var1.isSneaking()) {
                        var26 = 0.3F;
                     }

                     if (var19) {
                        var17 = var16.DistTo(var1.getCurrentSquare());
                        var18 = var16.DistTo(this.getCurrentSquare());
                     }

                     float var27 = var18 < 2.0F ? 5.0F : Math.min(var17, 5.0F);
                     var27 = Math.max(0.0F, var27 - 1.0F);
                     var27 = var27 / 5.0F * 0.9F + 0.1F;
                     var23 = Math.max(0.1F, 1.0F - ClimateManager.getInstance().getFogIntensity());
                     float var28 = var10 * var8 * var11 * var9 * var12 * var13 * var14 * var27 * var23 * var26;
                     if (var28 >= 1.0F) {
                        var1.spottedByPlayer = true;
                        return true;
                     } else {
                        var28 = (float)(1.0 - Math.pow((double)(1.0F - var28), (double)GameTime.getInstance().getMultiplier()));
                        var28 *= 0.5F;
                        boolean var29 = Rand.Next(0.0F, 1.0F) < var28;
                        var1.spottedByPlayer = var29;
                        return var29;
                     }
                  }
               } else {
                  var1.spottedByPlayer = true;
                  return true;
               }
            }
         }
      } else {
         return true;
      }
   }

   public String getTimeSurvived() {
      String var1 = "";
      int var2 = (int)this.getHoursSurvived();
      int var4 = var2 / 24;
      int var3 = var2 % 24;
      int var5 = var4 / 30;
      var4 %= 30;
      int var6 = var5 / 12;
      var5 %= 12;
      String var7 = Translator.getText("IGUI_Gametime_day");
      String var8 = Translator.getText("IGUI_Gametime_year");
      String var9 = Translator.getText("IGUI_Gametime_hour");
      String var10 = Translator.getText("IGUI_Gametime_month");
      if (var6 != 0) {
         if (var6 > 1) {
            var8 = Translator.getText("IGUI_Gametime_years");
         }

         if (var1.length() > 0) {
            var1 = var1 + ", ";
         }

         var1 = var1 + var6 + " " + var8;
      }

      if (var5 != 0) {
         if (var5 > 1) {
            var10 = Translator.getText("IGUI_Gametime_months");
         }

         if (var1.length() > 0) {
            var1 = var1 + ", ";
         }

         var1 = var1 + var5 + " " + var10;
      }

      if (var4 != 0) {
         if (var4 > 1) {
            var7 = Translator.getText("IGUI_Gametime_days");
         }

         if (var1.length() > 0) {
            var1 = var1 + ", ";
         }

         var1 = var1 + var4 + " " + var7;
      }

      if (var3 != 0) {
         if (var3 > 1) {
            var9 = Translator.getText("IGUI_Gametime_hours");
         }

         if (var1.length() > 0) {
            var1 = var1 + ", ";
         }

         var1 = var1 + var3 + " " + var9;
      }

      if (var1.isEmpty()) {
         int var11 = (int)(this.HoursSurvived * 60.0);
         var1 = "" + var11 + " " + Translator.getText("IGUI_Gametime_minutes");
      }

      return var1;
   }

   public boolean IsUsingAimWeapon() {
      if (this.leftHandItem == null) {
         return false;
      } else if (!(this.leftHandItem instanceof HandWeapon)) {
         return false;
      } else {
         return !this.isAiming() ? false : ((HandWeapon)this.leftHandItem).bIsAimedFirearm;
      }
   }

   private boolean IsUsingAimHandWeapon() {
      if (this.leftHandItem == null) {
         return false;
      } else if (!(this.leftHandItem instanceof HandWeapon)) {
         return false;
      } else {
         return !this.isAiming() ? false : ((HandWeapon)this.leftHandItem).bIsAimedHandWeapon;
      }
   }

   private boolean DoAimAnimOnAiming() {
      return this.IsUsingAimWeapon();
   }

   public int getSleepingPillsTaken() {
      return this.sleepingPillsTaken;
   }

   public void setSleepingPillsTaken(int var1) {
      if (this.isGodMod()) {
         this.resetSleepingPillsTaken();
      } else {
         this.sleepingPillsTaken = var1;
         if (this.getStats().Drunkenness > 10.0F) {
            ++this.sleepingPillsTaken;
         }

         this.lastPillsTaken = GameTime.instance.Calender.getTimeInMillis();
      }
   }

   public void resetSleepingPillsTaken() {
      this.sleepingPillsTaken = 0;
   }

   public boolean isOutside() {
      return this.getCurrentSquare() != null && this.getCurrentSquare().getRoom() == null && !this.isInARoom();
   }

   public double getLastSeenZomboidTime() {
      return this.lastSeenZombieTime;
   }

   public float getPlayerClothingTemperature() {
      float var1 = 0.0F;
      if (this.getClothingItem_Feet() != null) {
         var1 += ((Clothing)this.getClothingItem_Feet()).getTemperature();
      }

      if (this.getClothingItem_Hands() != null) {
         var1 += ((Clothing)this.getClothingItem_Hands()).getTemperature();
      }

      if (this.getClothingItem_Head() != null) {
         var1 += ((Clothing)this.getClothingItem_Head()).getTemperature();
      }

      if (this.getClothingItem_Legs() != null) {
         var1 += ((Clothing)this.getClothingItem_Legs()).getTemperature();
      }

      if (this.getClothingItem_Torso() != null) {
         var1 += ((Clothing)this.getClothingItem_Torso()).getTemperature();
      }

      return var1;
   }

   public float getPlayerClothingInsulation() {
      float var1 = 0.0F;
      if (this.getClothingItem_Feet() != null) {
         var1 += ((Clothing)this.getClothingItem_Feet()).getInsulation() * 0.1F;
      }

      if (this.getClothingItem_Hands() != null) {
         var1 += ((Clothing)this.getClothingItem_Hands()).getInsulation() * 0.0F;
      }

      if (this.getClothingItem_Head() != null) {
         var1 += ((Clothing)this.getClothingItem_Head()).getInsulation() * 0.0F;
      }

      if (this.getClothingItem_Legs() != null) {
         var1 += ((Clothing)this.getClothingItem_Legs()).getInsulation() * 0.3F;
      }

      if (this.getClothingItem_Torso() != null) {
         var1 += ((Clothing)this.getClothingItem_Torso()).getInsulation() * 0.6F;
      }

      return var1;
   }

   public InventoryItem getActiveLightItem() {
      if (this.rightHandItem != null && this.rightHandItem.isEmittingLight()) {
         return this.rightHandItem;
      } else if (this.leftHandItem != null && this.leftHandItem.isEmittingLight()) {
         return this.leftHandItem;
      } else {
         AttachedItems var1 = this.getAttachedItems();

         for(int var2 = 0; var2 < var1.size(); ++var2) {
            InventoryItem var3 = var1.getItemByIndex(var2);
            if (var3.isEmittingLight()) {
               return var3;
            }
         }

         return null;
      }
   }

   public boolean isTorchCone() {
      if (this.bRemote) {
         return this.mpTorchCone;
      } else {
         InventoryItem var1 = this.getActiveLightItem();
         return var1 != null && var1.isTorchCone();
      }
   }

   public float getTorchDot() {
      InventoryItem var1 = this.getActiveLightItem();
      return var1 != null ? var1.getTorchDot() : 0.0F;
   }

   public float getLightDistance() {
      if (this.bRemote) {
         return this.mpTorchDist;
      } else {
         InventoryItem var1 = this.getActiveLightItem();
         return var1 != null ? (float)var1.getLightDistance() : 0.0F;
      }
   }

   public boolean pressedMovement(boolean var1) {
      if (this.isNPC) {
         return false;
      } else if (GameClient.bClient && !this.isLocal()) {
         return this.networkAI.isPressedMovement();
      } else {
         boolean var2 = false;
         if (this.PlayerIndex == 0) {
            var2 = GameKeyboard.isKeyDown("Run");
         }

         if (this.JoypadBind != -1) {
            var2 |= JoypadManager.instance.isRTPressed(this.JoypadBind);
         }

         this.setVariable("pressedRunButton", var2);
         if (!var1 && (this.isBlockMovement() || this.isIgnoreInputsForDirection())) {
            if (GameClient.bClient && this.isLocal()) {
               this.networkAI.setPressedMovement(false);
            }

            return false;
         } else if (this.PlayerIndex != 0 || !GameKeyboard.isKeyDown("Left") && !GameKeyboard.isKeyDown("Right") && !GameKeyboard.isKeyDown("Forward") && !GameKeyboard.isKeyDown("Backward")) {
            if (this.JoypadBind != -1) {
               float var3 = JoypadManager.instance.getMovementAxisY(this.JoypadBind);
               float var4 = JoypadManager.instance.getMovementAxisX(this.JoypadBind);
               float var5 = JoypadManager.instance.getDeadZone(this.JoypadBind, 0);
               if (Math.abs(var3) > var5 || Math.abs(var4) > var5) {
                  if (GameClient.bClient && this.isLocal()) {
                     this.networkAI.setPressedMovement(true);
                  }

                  return true;
               }
            }

            if (GameClient.bClient && this.isLocal()) {
               this.networkAI.setPressedMovement(false);
            }

            return false;
         } else {
            if (GameClient.bClient && this.isLocal()) {
               this.networkAI.setPressedMovement(true);
            }

            return true;
         }
      }
   }

   public boolean pressedCancelAction() {
      if (this.isNPC) {
         return false;
      } else if (GameClient.bClient && !this.isLocal()) {
         return this.networkAI.isPressedCancelAction();
      } else if (this.PlayerIndex == 0 && GameKeyboard.isKeyDown("CancelAction")) {
         if (GameClient.bClient && this.isLocal()) {
            this.networkAI.setPressedCancelAction(true);
         }

         return true;
      } else if (this.JoypadBind != -1) {
         boolean var1 = JoypadManager.instance.isBButtonStartPress(this.JoypadBind);
         if (GameClient.bClient && this.isLocal()) {
            this.networkAI.setPressedCancelAction(var1);
         }

         return var1;
      } else {
         if (GameClient.bClient && this.isLocal()) {
            this.networkAI.setPressedCancelAction(false);
         }

         return false;
      }
   }

   public boolean checkWalkTo() {
      if (this.isNPC) {
         return false;
      } else if (this.PlayerIndex == 0 && GameKeyboard.isKeyDown("WalkTo")) {
         LuaEventManager.triggerEvent("OnPressWalkTo", 0, 0, 0);
         return true;
      } else {
         return false;
      }
   }

   public boolean pressedAim() {
      if (this.isNPC) {
         return false;
      } else if (this.PlayerIndex == 0 && this.isAimKeyDown()) {
         return true;
      } else if (this.JoypadBind == -1) {
         return false;
      } else {
         float var1 = JoypadManager.instance.getAimingAxisY(this.JoypadBind);
         float var2 = JoypadManager.instance.getAimingAxisX(this.JoypadBind);
         return Math.abs(var1) > 0.1F || Math.abs(var2) > 0.1F;
      }
   }

   public boolean isDoingActionThatCanBeCancelled() {
      if (this.isDead()) {
         return false;
      } else if (!this.getCharacterActions().isEmpty()) {
         return true;
      } else {
         State var1 = this.getCurrentState();
         if (var1 != null && var1.isDoingActionThatCanBeCancelled()) {
            return true;
         } else {
            for(int var2 = 0; var2 < this.stateMachine.getSubStateCount(); ++var2) {
               var1 = this.stateMachine.getSubStateAt(var2);
               if (var1 != null && var1.isDoingActionThatCanBeCancelled()) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   public long getSteamID() {
      return this.steamID;
   }

   public void setSteamID(long var1) {
      this.steamID = var1;
   }

   public boolean isTargetedByZombie() {
      return this.targetedByZombie;
   }

   public boolean isMaskClicked(int var1, int var2, boolean var3) {
      return this.sprite == null ? false : this.sprite.isMaskClicked(this.dir, var1, var2, var3);
   }

   public int getOffSetXUI() {
      return this.offSetXUI;
   }

   public void setOffSetXUI(int var1) {
      this.offSetXUI = var1;
   }

   public int getOffSetYUI() {
      return this.offSetYUI;
   }

   public void setOffSetYUI(int var1) {
      this.offSetYUI = var1;
   }

   public String getUsername() {
      return this.getUsername(false, false);
   }

   public String getUsername(Boolean var1) {
      return this.getUsername(var1, false);
   }

   public String getUsername(Boolean var1, Boolean var2) {
      String var3 = this.username;
      if (var2) {
         this.updateDisguisedState();
         IsoGameCharacter var4 = IsoCamera.getCameraCharacter();
         boolean var5 = GameClient.bClient && var4 instanceof IsoPlayer && ((IsoPlayer)var4).role.haveCapability(Capability.CanSeePlayersStats);
         if (this.isDisguised() && !var5) {
            var3 = ServerOptions.getInstance().HideDisguisedUserName.getValue() ? "" : Translator.getText("IGUI_Disguised_Player_Name");
         }
      } else if (var1 && GameClient.bClient && ServerOptions.instance.ShowFirstAndLastName.getValue()) {
         String var10000 = this.getDescriptor().getForename();
         var3 = var10000 + " " + this.getDescriptor().getSurname();
         if (ServerOptions.instance.DisplayUserName.getValue()) {
            var3 = var3 + " (" + this.username + ")";
         }
      }

      return var3;
   }

   public void setUsername(String var1) {
      this.username = var1;
   }

   public void updateUsername() {
      if (!GameClient.bClient && !GameServer.bServer) {
         String var10001 = this.getDescriptor().getForename();
         this.username = var10001 + this.getDescriptor().getSurname();
      }
   }

   public short getOnlineID() {
      return this.OnlineID;
   }

   public boolean isLocalPlayer() {
      if (GameServer.bServer) {
         return false;
      } else {
         for(int var1 = 0; var1 < numPlayers; ++var1) {
            if (players[var1] == this) {
               return true;
            }
         }

         return false;
      }
   }

   public static boolean isLocalPlayer(IsoGameCharacter var0) {
      return var0 instanceof IsoPlayer && ((IsoPlayer)var0).isLocalPlayer();
   }

   public static boolean isLocalPlayer(Object var0) {
      return var0 instanceof IsoPlayer && ((IsoPlayer)var0).isLocalPlayer();
   }

   public static void setLocalPlayer(int var0, IsoPlayer var1) {
      players[var0] = var1;
   }

   public static IsoPlayer getLocalPlayerByOnlineID(short var0) {
      for(int var1 = 0; var1 < numPlayers; ++var1) {
         IsoPlayer var2 = players[var1];
         if (var2 != null && var2.OnlineID == var0) {
            return var2;
         }
      }

      return null;
   }

   public boolean isOnlyPlayerAsleep() {
      if (!this.isAsleep()) {
         return false;
      } else {
         for(int var1 = 0; var1 < numPlayers; ++var1) {
            if (players[var1] != null && !players[var1].isDead() && players[var1] != this && players[var1].isAsleep()) {
               return false;
            }
         }

         return true;
      }
   }

   public void setHasObstacleOnPath(boolean var1) {
      this.hasObstacleOnPath = var1;
   }

   public boolean isRemoteAndHasObstacleOnPath() {
      return !this.isLocalPlayer() && this.hasObstacleOnPath;
   }

   public void OnDeath() {
      super.OnDeath();
      if (!GameServer.bServer) {
         this.StopAllActionQueue();
         if (this.isAsleep()) {
            UIManager.FadeIn((double)this.getPlayerNum(), 0.5);
            this.setAsleep(false);
         }

         this.dropHandItems();
         if (allPlayersDead()) {
            SoundManager.instance.playMusic(DEATH_MUSIC_NAME);
         }

         if (this.isLocalPlayer()) {
            LuaEventManager.triggerEvent("OnPlayerDeath", this);
         }

         if (this.isLocalPlayer() && this.getVehicle() != null) {
            this.getVehicle().exit(this);
         }

         this.removeSaveFile();
         if (this.shouldBecomeZombieAfterDeath()) {
            this.forceAwake();
         }

         if (this.getMoodles() != null) {
            this.getMoodles().Update();
         }

         this.getCell().setDrag((KahluaTable)null, this.getPlayerNum());
      }
   }

   public boolean isNoClip() {
      return this.noClip;
   }

   public void setNoClip(boolean var1) {
      if (!Role.haveCapability(this, Capability.ToggleNoclipHimself)) {
         this.noClip = false;
      } else {
         this.noClip = var1;
      }
   }

   /** @deprecated */
   public void setAuthorizeMeleeAction(boolean var1) {
      this.setAuthorizedHandToHandAction(var1);
   }

   /** @deprecated */
   public boolean isAuthorizeMeleeAction() {
      return this.isAuthorizedHandToHandAction();
   }

   /** @deprecated */
   public void setAuthorizeShoveStomp(boolean var1) {
      this.setAuthorizedHandToHand(var1);
   }

   /** @deprecated */
   public boolean isAuthorizeShoveStomp() {
      return this.isAuthorizedHandToHand();
   }

   public void setAuthorizedHandToHandAction(boolean var1) {
      this.m_isAuthorizedHandToHandAction = var1;
   }

   public boolean isAuthorizedHandToHandAction() {
      return this.m_isAuthorizedHandToHandAction;
   }

   public void setAuthorizedHandToHand(boolean var1) {
      this.m_isAuthorizedHandToHand = var1;
   }

   public boolean isAuthorizedHandToHand() {
      return this.m_isAuthorizedHandToHand;
   }

   public boolean isBlockMovement() {
      return this.blockMovement;
   }

   public void setBlockMovement(boolean var1) {
      this.blockMovement = var1;
   }

   public void startReceivingBodyDamageUpdates(IsoPlayer var1) {
      if (GameClient.bClient && var1 != null && var1 != this && this.isLocalPlayer() && !var1.isLocalPlayer()) {
         var1.resetBodyDamageRemote();
         BodyDamageSync.instance.startReceivingUpdates(var1);
      }

   }

   public void stopReceivingBodyDamageUpdates(IsoPlayer var1) {
      if (GameClient.bClient && var1 != null && var1 != this && !var1.isLocalPlayer()) {
         BodyDamageSync.instance.stopReceivingUpdates(var1);
      }

   }

   public Nutrition getNutrition() {
      return this.nutrition;
   }

   public Fitness getFitness() {
      return this.fitness;
   }

   private boolean updateRemotePlayer() {
      if (!this.bRemote) {
         return false;
      } else {
         if (GameServer.bServer) {
            ServerLOS.instance.doServerZombieLOS(this);
            ServerLOS.instance.updateLOS(this);
            if (this.isDead()) {
               return true;
            }

            this.removeFromSquare();
            this.setX(this.realx);
            this.setY(this.realy);
            this.setZ((float)this.realz);
            this.setLastX(this.realx);
            this.setLastY(this.realy);
            this.setLastZ((float)this.realz);
            this.ensureOnTile();
            if (this.slowTimer > 0.0F) {
               this.slowTimer -= GameTime.instance.getRealworldSecondsSinceLastUpdate();
               this.slowFactor -= GameTime.instance.getMultiplier() / 100.0F;
               if (this.slowFactor < 0.0F) {
                  this.slowFactor = 0.0F;
               }
            } else {
               this.slowFactor = 0.0F;
            }
         }

         if (GameClient.bClient) {
            if (this.isCurrentState(BumpedState.instance())) {
               return true;
            }

            float var1;
            float var2;
            float var3;
            if (!this.networkAI.isCollisionEnabled() && !this.networkAI.isNoCollisionTimeout()) {
               this.setCollidable(false);
               var1 = this.realx;
               var2 = this.realy;
               var3 = (float)this.realz;
            } else {
               this.setCollidable(true);
               var1 = this.networkAI.targetX;
               var2 = this.networkAI.targetY;
               var3 = (float)this.networkAI.targetZ;
            }

            this.updateMovementRates();
            PathFindBehavior2 var4 = this.getPathFindBehavior2();
            boolean var5 = false;
            if (!this.networkAI.events.isEmpty()) {
               Iterator var6 = this.networkAI.events.iterator();

               while(var6.hasNext()) {
                  EventPacket var7 = (EventPacket)var6.next();
                  if (var7.process(this, GameClient.connection)) {
                     this.m_isPlayerMoving = this.networkAI.moving = false;
                     this.setJustMoved(false);
                     if (this.networkAI.usePathFind) {
                        var4.reset();
                        this.setPath2((Path)null);
                        this.networkAI.usePathFind = false;
                     }

                     var6.remove();
                     return true;
                  }

                  if (!var7.isMovableEvent()) {
                     tempo.set(var7.x - this.getX(), var7.y - this.getY());
                     var1 = var7.x;
                     var2 = var7.y;
                     var3 = var7.z;
                     var5 = true;
                  }

                  if (var7.isTimeout()) {
                     this.m_isPlayerMoving = this.networkAI.moving = false;
                     this.setJustMoved(false);
                     if (this.networkAI.usePathFind) {
                        var4.reset();
                        this.setPath2((Path)null);
                        this.networkAI.usePathFind = false;
                     }

                     if (Core.bDebug) {
                        DebugLog.log(DebugType.Multiplayer, String.format("Event timeout (%d) : %s", this.networkAI.events.size(), var7.getDescription()));
                     }

                     var6.remove();
                     return true;
                  }
               }
            }

            if (!var5 && this.networkAI.collidePointX > -1.0F && this.networkAI.collidePointY > -1.0F && (PZMath.fastfloor(this.getX()) != PZMath.fastfloor(this.networkAI.collidePointX) || PZMath.fastfloor(this.getY()) != PZMath.fastfloor(this.networkAI.collidePointY))) {
               var1 = this.networkAI.collidePointX;
               var2 = this.networkAI.collidePointY;
               DebugLog.DetailedInfo.trace("Player " + this.username + ": collide point (" + var1 + ", " + var2 + ") has not been reached, so move to it");
            }

            this.networkAI.targetX = var1;
            this.networkAI.targetY = var2;
            if (!this.networkAI.forcePathFinder && this.isRemoteAndHasObstacleOnPath()) {
               this.networkAI.forcePathFinder = true;
            }

            if (this.networkAI.forcePathFinder && !PolygonalMap2.instance.lineClearCollide(this.getX(), this.getY(), var1, var2, PZMath.fastfloor(this.getZ()), this.vehicle, false, true) && IsoUtils.DistanceManhatten(var1, var2, this.getX(), this.getY()) < 2.0F || this.getCurrentState() == ClimbOverFenceState.instance() || this.getCurrentState() == ClimbThroughWindowState.instance() || this.getCurrentState() == ClimbOverWallState.instance()) {
               this.networkAI.forcePathFinder = false;
            }

            float var11;
            if (!this.networkAI.needToMovingUsingPathFinder && !this.networkAI.forcePathFinder) {
               if (this.networkAI.usePathFind) {
                  var4.reset();
                  this.setPath2((Path)null);
                  this.networkAI.usePathFind = false;
               }

               var4.walkingOnTheSpot.reset(this.getX(), this.getY());
               this.getDeferredMovement(tempVector2_2);
               if (this.getCurrentState() != ClimbOverWallState.instance() && this.getCurrentState() != ClimbOverFenceState.instance()) {
                  var11 = IsoUtils.DistanceTo(this.getX(), this.getY(), this.networkAI.targetX, this.networkAI.targetY) / IsoUtils.DistanceTo(this.realx, this.realy, this.networkAI.targetX, this.networkAI.targetY);
                  float var13 = 0.8F + 0.4F * IsoUtils.smoothstep(0.8F, 1.2F, var11);
                  var4.moveToPoint(var1, var2, var13);
               } else {
                  this.MoveUnmodded(tempVector2_2);
               }

               this.m_isPlayerMoving = !var5 && IsoUtils.DistanceManhatten(var1, var2, this.getX(), this.getY()) > 0.2F || PZMath.fastfloor(var1) != PZMath.fastfloor(this.getX()) || PZMath.fastfloor(var2) != PZMath.fastfloor(this.getY()) || PZMath.fastfloor(this.getZ()) != PZMath.fastfloor(var3);
               if (!this.m_isPlayerMoving) {
                  this.DirectionFromVector(this.networkAI.direction);
                  this.setForwardDirection(this.networkAI.direction);
                  this.networkAI.forcePathFinder = false;
                  if (this.networkAI.usePathFind) {
                     var4.reset();
                     this.setPath2((Path)null);
                     this.networkAI.usePathFind = false;
                  }
               }

               this.setJustMoved(this.m_isPlayerMoving);
               this.m_deltaX = 0.0F;
               this.m_deltaY = 0.0F;
            } else {
               if (!this.networkAI.usePathFind || var1 != var4.getTargetX() || var2 != var4.getTargetY()) {
                  var4.pathToLocationF(var1, var2, var3);
                  var4.walkingOnTheSpot.reset(this.getX(), this.getY());
                  this.networkAI.usePathFind = true;
               }

               PathFindBehavior2.BehaviorResult var10 = var4.update();
               if (var10 == PathFindBehavior2.BehaviorResult.Failed) {
                  this.setPathFindIndex(-1);
                  if (this.networkAI.forcePathFinder) {
                     this.networkAI.forcePathFinder = false;
                  } else if (NetworkTeleport.teleport(this, NetworkTeleport.Type.teleportation, this.realx, this.realy, this.realz, 1.0F) && GameServer.bServer) {
                     DebugLog.Multiplayer.warn(String.format("Player %d teleport from (%.2f, %.2f, %.2f) to (%.2f, %.2f, %.2f)", this.getOnlineID(), this.getX(), this.getY(), this.getZ(), this.realx, this.realy, this.realz));
                  }
               } else if (var10 == PathFindBehavior2.BehaviorResult.Succeeded) {
                  int var12 = PZMath.fastfloor(var4.getTargetX());
                  int var8 = PZMath.fastfloor(var4.getTargetY());
                  if (GameServer.bServer) {
                     ServerMap.instance.getChunk(var12 / 8, var8 / 8);
                  } else {
                     IsoWorld.instance.CurrentCell.getChunkForGridSquare(var12, var8, 0);
                  }

                  this.m_isPlayerMoving = true;
                  this.setJustMoved(true);
               }

               this.m_deltaX = 0.0F;
               this.m_deltaY = 0.0F;
            }

            if (!this.m_isPlayerMoving || this.isAiming()) {
               this.DirectionFromVector(this.networkAI.direction);
               this.setForwardDirection(this.networkAI.direction);
               tempo.set(var1 - this.getNextX(), -(var2 - this.getNextY()));
               tempo.normalize();
               var11 = this.legsSprite.modelSlot.model.AnimPlayer.getRenderedAngle();
               if ((double)var11 > 6.283185307179586) {
                  var11 = (float)((double)var11 - 6.283185307179586);
               }

               if (var11 < 0.0F) {
                  var11 = (float)((double)var11 + 6.283185307179586);
               }

               tempo.rotate(var11);
               tempo.setLength(Math.min(IsoUtils.DistanceTo(var1, var2, this.getX(), this.getY()), 1.0F));
               this.m_deltaX = tempo.x;
               this.m_deltaY = tempo.y;
            }
         }

         return true;
      }
   }

   private boolean updateWhileDead() {
      if (GameServer.bServer) {
         return false;
      } else if (!this.isLocalPlayer()) {
         return false;
      } else if (!this.isDead()) {
         return false;
      } else {
         this.setVariable("bPathfind", false);
         this.setMoving(false);
         this.m_isPlayerMoving = false;
         if (this.getVehicle() != null) {
            this.getVehicle().exit(this);
         }

         if (this.heartEventInstance != 0L) {
            this.getEmitter().stopSound(this.heartEventInstance);
            this.heartEventInstance = 0L;
         }

         return true;
      }
   }

   private void initFMODParameters() {
      FMODParameterList var1 = this.getFMODParameters();
      if (this instanceof IsoAnimal) {
         var1.add(this.parameterFootstepMaterial);
         var1.add(this.parameterFootstepMaterial2);
      } else {
         var1.add(this.parameterCharacterMovementSpeed);
         var1.add(this.parameterCharacterOnFire);
         var1.add(this.parameterCharacterVoiceType);
         var1.add(this.parameterCharacterVoicePitch);
         var1.add(this.parameterDragMaterial);
         var1.add(this.parameterEquippedBaggageContainer);
         var1.add(this.parameterExercising);
         var1.add(this.parameterFirearmInside);
         var1.add(this.parameterFirearmRoomSize);
         var1.add(this.parameterFootstepMaterial);
         var1.add(this.parameterFootstepMaterial2);
         var1.add(this.parameterIsStashTile);
         var1.add(this.parameterLocalPlayer);
         var1.add(this.parameterMeleeHitSurface);
         var1.add(this.parameterPlayerHealth);
         var1.add(this.parameterShoeType);
         var1.add(this.parameterVehicleHitLocation);
      }
   }

   public ParameterCharacterMovementSpeed getParameterCharacterMovementSpeed() {
      return this.parameterCharacterMovementSpeed;
   }

   public void setMeleeHitSurface(ParameterMeleeHitSurface.Material var1) {
      this.parameterMeleeHitSurface.setMaterial(var1);
   }

   public void setMeleeHitSurface(String var1) {
      try {
         if ("MetalPoleGateDouble".equalsIgnoreCase(var1) || "MetalPoleGateSmall".equalsIgnoreCase(var1)) {
            var1 = "MetalGate";
         }

         this.parameterMeleeHitSurface.setMaterial(ParameterMeleeHitSurface.Material.valueOf(var1));
      } catch (IllegalArgumentException var3) {
         this.parameterMeleeHitSurface.setMaterial(ParameterMeleeHitSurface.Material.Default);
      }

   }

   public void setVehicleHitLocation(BaseVehicle var1) {
      ParameterVehicleHitLocation.HitLocation var2 = ParameterVehicleHitLocation.calculateLocation(var1, this.getX(), this.getY(), this.getZ());
      this.parameterVehicleHitLocation.setLocation(var2);
   }

   private void updateHeartSound() {
      if (!GameServer.bServer) {
         if (this.isLocalPlayer()) {
            GameSound var1 = GameSounds.getSound("HeartBeat");
            boolean var2 = var1 != null && var1.getUserVolume() > 0.0F && this.stats.Panic > 0.0F;
            if (!this.Asleep && var2 && GameTime.getInstance().getTrueMultiplier() == 1.0F) {
               this.heartDelay -= GameTime.getInstance().getThirtyFPSMultiplier();
               if (this.heartEventInstance == 0L || !this.getEmitter().isPlaying(this.heartEventInstance)) {
                  this.heartEventInstance = this.getEmitter().playSoundImpl("HeartBeat", (IsoObject)null);
                  this.getEmitter().setVolume(this.heartEventInstance, 0.0F);
               }

               if (this.heartDelay <= 0.0F) {
                  this.heartDelayMax = (float)((int)((1.0F - this.stats.Panic / 100.0F * 0.7F) * 25.0F)) * 2.0F;
                  this.heartDelay = this.heartDelayMax;
                  if (this.heartEventInstance != 0L) {
                     this.getEmitter().setVolume(this.heartEventInstance, this.stats.Panic / 100.0F);
                  }
               }
            } else if (this.heartEventInstance != 0L) {
               this.getEmitter().setVolume(this.heartEventInstance, 0.0F);
            }

         }
      }
   }

   private void updateWorldAmbiance() {
      if (!GameServer.bServer) {
         if (this.isLocalPlayer()) {
            if (this.getPlayerNum() == 0) {
               byte var1 = 0;
               if (this.worldAmbienceEmitter == null) {
                  this.worldAmbienceEmitter = (BaseSoundEmitter)(Core.SoundDisabled ? new DummySoundEmitter() : new FMODSoundEmitter());
                  this.worldAmbienceEmitter.setPos(this.getX(), this.getY(), (float)var1);
               }

               if (this.worldAmbianceInstance == 0L || !this.worldAmbienceEmitter.isPlaying(this.worldAmbianceInstance)) {
                  this.worldAmbianceInstance = this.worldAmbienceEmitter.playSoundImpl("WorldAmbiance", (IsoObject)null);
                  this.worldAmbienceEmitter.setVolume(this.worldAmbianceInstance, 1.0F);
               }

               this.worldAmbienceEmitter.setPos(this.getX(), this.getY(), (float)var1);
               this.worldAmbienceEmitter.tick();
            }
         }
      }
   }

   private void updateEquippedBaggageContainer() {
      if (!GameServer.bServer) {
         if (this.isLocalPlayer()) {
            InventoryItem var1 = this.getClothingItem_Back();
            String var2;
            if (var1 != null && var1.IsInventoryContainer()) {
               var2 = var1.getSoundParameter("EquippedBaggageContainer");
               this.parameterEquippedBaggageContainer.setContainerType(var2);
            } else {
               var1 = this.getSecondaryHandItem();
               if (var1 != null && var1.IsInventoryContainer()) {
                  var2 = var1.getSoundParameter("EquippedBaggageContainer");
                  this.parameterEquippedBaggageContainer.setContainerType(var2);
               } else {
                  var1 = this.getPrimaryHandItem();
                  if (var1 != null && var1.IsInventoryContainer()) {
                     var2 = var1.getSoundParameter("EquippedBaggageContainer");
                     this.parameterEquippedBaggageContainer.setContainerType(var2);
                  } else {
                     this.parameterEquippedBaggageContainer.setContainerType(ParameterEquippedBaggageContainer.ContainerType.None);
                  }
               }
            }
         }
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

      this.parameterCharacterMovementSpeed.setMovementType(var2);
      super.DoFootstepSound(var3);
   }

   private void updateHeavyBreathing() {
   }

   public long playerVoiceSound(String var1) {
      String var2 = this.descriptor.getVoicePrefix();
      return this.getEmitter().isPlaying(var2 + var1) ? 0L : this.getEmitter().playVocals(var2 + var1);
   }

   public long stopPlayerVoiceSound(String var1) {
      String var2 = this.descriptor.getVoicePrefix();
      return (long)this.getEmitter().stopSoundByName(var2 + var1);
   }

   public void updateVocalProperties() {
      if (!GameServer.bServer) {
         if (this.isLocal()) {
            if (this.vocalEvent != 0L && !this.getEmitter().isPlaying(this.vocalEvent)) {
               this.vocalEvent = 0L;
               if (this.parameterMoodles != null) {
                  this.parameterMoodles.reset();
               }
            }

            if (this.vocalEvent == 0L && this.isAlive()) {
               this.vocalEvent = this.playSoundLocal(this.descriptor.getVoicePrefix());
               if (this.parameterMoodles != null) {
                  this.parameterMoodles.reset();
               }
            }

            if (this.vocalEvent != 0L) {
               if (this.parameterMoodles == null) {
                  this.parameterMoodles = new ParameterMoodles(this);
               }

               this.parameterMoodles.update(this.vocalEvent);
            }

         }
      }
   }

   public boolean isDraggingCorpseStateName(String var1) {
      if (!this.isDraggingCorpse()) {
         return false;
      } else {
         AnimLayer var2 = this.getAdvancedAnimator().getRootLayer();
         return var2 == null ? false : var2.isCurrentState(var1);
      }
   }

   public boolean isPickingUpBody() {
      return this.isDraggingCorpseStateName("pickUpBody-HeadEnd") || this.isDraggingCorpseStateName("pickUpBody-LegsEnd");
   }

   public boolean isPuttingDownBody() {
      return this.isDraggingCorpseStateName("layDownBody");
   }

   private void updateDraggingCorpseSounds() {
      if (!this.isAnimal()) {
         boolean var1 = this.isPickingUpBody();
         boolean var2 = this.isPuttingDownBody();
         boolean var3 = false;
         boolean var4 = false;
         if (this.isDraggingCorpse() && !var1 && !var2) {
            if (this.isPlayerMoving() && this.getBeenMovingFor() > 20.0F) {
               var3 = true;
            }

            if (this.isTurning()) {
               var4 = true;
            }
         }

         if (var3) {
            if (!this.getEmitter().isPlaying("CorpseDrag")) {
               this.getEmitter().playSoundImpl("CorpseDrag", (IsoObject)null);
            }
         } else if (this.getEmitter().isPlaying("CorpseDrag")) {
            this.getEmitter().stopOrTriggerSoundByName("CorpseDrag");
         }

         if (var4) {
            if (!this.getEmitter().isPlaying("CorpseDragTurn")) {
               this.playSoundLocal("CorpseDragTurn");
            }
         } else if (this.getEmitter().isPlaying("CorpseDragTurn")) {
            this.getEmitter().stopOrTriggerSoundByName("CorpseDragTurn");
         }

      }
   }

   public void playBloodSplatterSound() {
      if (!this.isDead()) {
         super.playBloodSplatterSound();
      }
   }

   private void checkVehicleContainers() {
      ArrayList var1 = this.vehicleContainerData.tempContainers;
      var1.clear();
      int var2 = PZMath.fastfloor(this.getX()) - 4;
      int var3 = PZMath.fastfloor(this.getY()) - 4;
      int var4 = PZMath.fastfloor(this.getX()) + 4;
      int var5 = PZMath.fastfloor(this.getY()) + 4;
      int var6 = var2 / 8;
      int var7 = var3 / 8;
      int var8 = (int)Math.ceil((double)((float)var4 / 8.0F));
      int var9 = (int)Math.ceil((double)((float)var5 / 8.0F));

      int var10;
      for(var10 = var7; var10 < var9; ++var10) {
         for(int var11 = var6; var11 < var8; ++var11) {
            IsoChunk var12 = GameServer.bServer ? ServerMap.instance.getChunk(var11, var10) : IsoWorld.instance.CurrentCell.getChunkForGridSquare(var11 * 8, var10 * 8, 0);
            if (var12 != null) {
               for(int var13 = 0; var13 < var12.vehicles.size(); ++var13) {
                  BaseVehicle var14 = (BaseVehicle)var12.vehicles.get(var13);
                  VehicleScript var15 = var14.getScript();
                  if (var15 != null) {
                     for(int var16 = 0; var16 < var15.getPartCount(); ++var16) {
                        VehicleScript.Part var17 = var15.getPart(var16);
                        if (var17.container != null && var17.area != null && var14.isInArea(var17.area, (IsoGameCharacter)this)) {
                           VehicleContainer var18 = this.vehicleContainerData.freeContainers.isEmpty() ? new VehicleContainer() : (VehicleContainer)this.vehicleContainerData.freeContainers.pop();
                           var1.add(var18.set(var14, var16));
                        }
                     }
                  }
               }
            }
         }
      }

      if (var1.size() != this.vehicleContainerData.containers.size()) {
         this.vehicleContainerData.freeContainers.addAll(this.vehicleContainerData.containers);
         this.vehicleContainerData.containers.clear();
         this.vehicleContainerData.containers.addAll(var1);
         LuaEventManager.triggerEvent("OnContainerUpdate");
      } else {
         for(var10 = 0; var10 < var1.size(); ++var10) {
            VehicleContainer var19 = (VehicleContainer)var1.get(var10);
            VehicleContainer var20 = (VehicleContainer)this.vehicleContainerData.containers.get(var10);
            if (!var19.equals(var20)) {
               this.vehicleContainerData.freeContainers.addAll(this.vehicleContainerData.containers);
               this.vehicleContainerData.containers.clear();
               this.vehicleContainerData.containers.addAll(var1);
               LuaEventManager.triggerEvent("OnContainerUpdate");
               break;
            }
         }
      }

   }

   public void setJoypadIgnoreAimUntilCentered(boolean var1) {
      this.bJoypadIgnoreAimUntilCentered = var1;
   }

   public ByteBufferWriter createPlayerStats(ByteBufferWriter var1, String var2) {
      var1.putShort(this.getOnlineID());
      var1.putUTF(var2);
      var1.putUTF(this.getDisplayName());
      var1.putUTF(this.getDescriptor().getForename());
      var1.putUTF(this.getDescriptor().getSurname());
      var1.putUTF(this.getDescriptor().getProfession());
      if (!StringUtils.isNullOrEmpty(this.getTagPrefix())) {
         var1.putByte((byte)1);
         var1.putUTF(this.getTagPrefix());
      } else {
         var1.putByte((byte)0);
      }

      var1.putBoolean(this.isAllChatMuted());
      var1.putFloat(this.getTagColor().r);
      var1.putFloat(this.getTagColor().g);
      var1.putFloat(this.getTagColor().b);
      var1.putByte((byte)(this.showTag ? 1 : 0));
      var1.putByte((byte)(this.factionPvp ? 1 : 0));
      return var1;
   }

   public String setPlayerStats(ByteBuffer var1, String var2) {
      String var3 = GameWindow.ReadString(var1);
      String var4 = GameWindow.ReadString(var1);
      String var5 = GameWindow.ReadString(var1);
      String var6 = GameWindow.ReadString(var1);
      String var7 = "";
      if (var1.get() == 1) {
         var7 = GameWindow.ReadString(var1);
      }

      boolean var8 = var1.get() == 1;
      float var9 = var1.getFloat();
      float var10 = var1.getFloat();
      float var11 = var1.getFloat();
      String var12 = "";
      this.setTagColor(new ColorInfo(var9, var10, var11, 1.0F));
      this.setTagPrefix(var7);
      this.showTag = var1.get() == 1;
      this.factionPvp = var1.get() == 1;
      if (!var4.equals(this.getDescriptor().getForename())) {
         if (GameServer.bServer) {
            var12 = var2 + " Changed " + var3 + " forname in " + var4;
         } else {
            var12 = "Changed your forname in " + var4;
         }
      }

      this.getDescriptor().setForename(var4);
      if (!var5.equals(this.getDescriptor().getSurname())) {
         if (GameServer.bServer) {
            var12 = var2 + " Changed " + var3 + " surname in " + var5;
         } else {
            var12 = "Changed your surname in " + var5;
         }
      }

      this.getDescriptor().setSurname(var5);
      if (!var6.equals(this.getDescriptor().getProfession())) {
         if (GameServer.bServer) {
            var12 = var2 + " Changed " + var3 + " profession to " + var6;
         } else {
            var12 = "Changed your profession in " + var6;
         }
      }

      this.getDescriptor().setProfession(var6);
      if (!this.getDisplayName().equals(var3)) {
         if (GameServer.bServer) {
            var12 = var2 + " Changed display name \"" + this.getDisplayName() + "\" to \"" + var3 + "\"";
            ServerWorldDatabase.instance.updateDisplayName(this.username, var3);
         } else {
            var12 = "Changed your display name to " + var3;
         }

         this.setDisplayName(var3);
      }

      if (var8 != this.isAllChatMuted()) {
         if (var8) {
            if (GameServer.bServer) {
               var12 = var2 + " Banned " + var3 + " from using /all chat";
            } else {
               var12 = "Banned you from using /all chat";
            }
         } else if (GameServer.bServer) {
            var12 = var2 + " Allowed " + var3 + " to use /all chat";
         } else {
            var12 = "Now allowed you to use /all chat";
         }
      }

      this.setAllChatMuted(var8);
      if (GameServer.bServer && !"".equals(var12)) {
         LoggerManager.getLogger("admin").write(var12);
      }

      if (GameClient.bClient) {
         LuaEventManager.triggerEvent("OnMiniScoreboardUpdate");
      }

      return var12;
   }

   public boolean isAllChatMuted() {
      return this.allChatMuted;
   }

   public void setAllChatMuted(boolean var1) {
      this.allChatMuted = var1;
   }

   /** @deprecated */
   @Deprecated
   public String getAccessLevel() {
      return this.role == null ? "none" : this.role.getName();
   }

   public Role getRole() {
      return this.role;
   }

   public boolean isAccessLevel(String var1) {
      return this.getAccessLevel().equalsIgnoreCase(var1);
   }

   public void setRole(String var1) {
      if (GameClient.bClient) {
         Role var2 = Roles.getRole(var1.trim().toLowerCase());
         String var10000 = this.username;
         GameClient.SendCommandToServer("/setaccesslevel \"" + var10000 + "\" \"" + var2.getName() + "\"");
      }

   }

   public void addMechanicsItem(String var1, VehiclePart var2, Long var3) {
      int var4 = 1;
      int var5 = 1;
      if (this.mechanicsItem.get(Long.parseLong(var1)) == null && !GameClient.bClient) {
         if (var2.getTable("uninstall") != null && var2.getTable("uninstall").rawget("skills") != null) {
            String[] var6 = ((String)var2.getTable("uninstall").rawget("skills")).split(";");
            String[] var7 = var6;
            int var8 = var6.length;

            for(int var9 = 0; var9 < var8; ++var9) {
               String var10 = var7[var9];
               if (var10.contains("Mechanics")) {
                  int var11 = Integer.parseInt(var10.split(":")[1]);
                  if (var11 >= 6) {
                     var4 = 3;
                     var5 = 7;
                  } else if (var11 >= 4) {
                     var4 = 3;
                     var5 = 5;
                  } else if (var11 >= 2) {
                     var4 = 2;
                     var5 = 4;
                  } else if (Rand.Next(3) == 0) {
                     var4 = 2;
                     var5 = 2;
                  }

                  var4 *= 2;
                  var5 *= 2;
               }
            }
         }

         if (GameServer.bServer) {
            GameServer.addXp(this, PerkFactory.Perks.Mechanics, (float)Rand.Next(var4, var5));
         } else {
            this.getXp().AddXP(PerkFactory.Perks.Mechanics, (float)Rand.Next(var4, var5));
         }
      }

      this.mechanicsItem.put(Long.parseLong(var1), var3);
   }

   private void updateTemperatureCheck() {
      int var1 = this.Moodles.getMoodleLevel(MoodleType.Hypothermia);
      if (this.hypothermiaCache == -1 || this.hypothermiaCache != var1) {
         if (var1 >= 3 && var1 > this.hypothermiaCache && this.isAsleep() && !this.ForceWakeUp) {
            this.forceAwake();
         }

         this.hypothermiaCache = var1;
      }

      int var2 = this.Moodles.getMoodleLevel(MoodleType.Hyperthermia);
      if (this.hyperthermiaCache == -1 || this.hyperthermiaCache != var2) {
         if (var2 >= 3 && var2 > this.hyperthermiaCache && this.isAsleep() && !this.ForceWakeUp) {
            this.forceAwake();
         }

         this.hyperthermiaCache = var2;
      }

   }

   public float getZombieRelevenceScore(IsoZombie var1) {
      if (var1.getCurrentSquare() == null) {
         return -10000.0F;
      } else {
         float var2 = 0.0F;
         if (var1.getCurrentSquare().getCanSee(this.PlayerIndex)) {
            var2 += 100.0F;
         } else if (var1.getCurrentSquare().isCouldSee(this.PlayerIndex)) {
            var2 += 10.0F;
         }

         if (var1.getCurrentSquare().getRoom() != null && this.current.getRoom() == null) {
            var2 -= 20.0F;
         }

         if (var1.getCurrentSquare().getRoom() == null && this.current.getRoom() != null) {
            var2 -= 20.0F;
         }

         if (var1.getCurrentSquare().getRoom() != this.current.getRoom()) {
            var2 -= 20.0F;
         }

         float var3 = var1.DistTo(this);
         var2 -= var3;
         if (var3 < 20.0F) {
            var2 += 300.0F;
         }

         if (var3 < 15.0F) {
            var2 += 300.0F;
         }

         if (var3 < 10.0F) {
            var2 += 1000.0F;
         }

         if (var1.getTargetAlpha() < 1.0F && var2 > 0.0F) {
            var2 *= var1.getTargetAlpha();
         }

         return var2;
      }
   }

   public BaseVisual getVisual() {
      return this.baseVisual;
   }

   public HumanVisual getHumanVisual() {
      return (HumanVisual)Type.tryCastTo(this.baseVisual, HumanVisual.class);
   }

   public AnimalVisual getAnimalVisual() {
      return (AnimalVisual)Type.tryCastTo(this.baseVisual, AnimalVisual.class);
   }

   public String getAnimalType() {
      return null;
   }

   public float getAnimalSize() {
      return 1.0F;
   }

   public ItemVisuals getItemVisuals() {
      return this.itemVisuals;
   }

   public void getItemVisuals(ItemVisuals var1) {
      if (this.bRemote && !GameServer.bServer) {
         var1.clear();
         var1.addAll(this.itemVisuals);
      } else {
         this.getWornItems().getItemVisuals(var1);
      }

   }

   public void dressInNamedOutfit(String var1) {
      if (this.getHumanVisual() != null) {
         this.getHumanVisual().dressInNamedOutfit(var1, this.itemVisuals);
         this.onClothingOutfitPreviewChanged();
      }
   }

   public void dressInClothingItem(String var1) {
      this.getHumanVisual().dressInClothingItem(var1, this.itemVisuals);
      this.onClothingOutfitPreviewChanged();
   }

   private void onClothingOutfitPreviewChanged() {
      if (this.isLocalPlayer()) {
         this.getInventory().clear();
         this.wornItems.setFromItemVisuals(this.itemVisuals);
         this.wornItems.addItemsToItemContainer(this.getInventory());
         this.itemVisuals.clear();
         this.resetModel();
         this.onWornItemsChanged();
      }
   }

   public void onWornItemsChanged() {
      this.parameterShoeType.setShoeType((ParameterShoeType.ShoeType)null);
   }

   public void actionStateChanged(ActionContext var1) {
      super.actionStateChanged(var1);
   }

   public Vector2 getLastAngle() {
      return this.lastAngle;
   }

   public void setLastAngle(Vector2 var1) {
      this.lastAngle.set(var1);
   }

   public int getDialogMood() {
      return this.DialogMood;
   }

   public void setDialogMood(int var1) {
      this.DialogMood = var1;
   }

   public int getPing() {
      return this.ping;
   }

   public void setPing(int var1) {
      this.ping = var1;
   }

   public IsoMovingObject getDragObject() {
      return this.DragObject;
   }

   public void setDragObject(IsoMovingObject var1) {
      this.DragObject = var1;
   }

   public float getAsleepTime() {
      return this.AsleepTime;
   }

   public void setAsleepTime(float var1) {
      this.AsleepTime = var1;
   }

   public Stack<IsoMovingObject> getSpottedList() {
      return this.spottedList;
   }

   public int getTicksSinceSeenZombie() {
      return this.TicksSinceSeenZombie;
   }

   public void setTicksSinceSeenZombie(int var1) {
      this.TicksSinceSeenZombie = var1;
   }

   public boolean isWaiting() {
      return this.Waiting;
   }

   public void setWaiting(boolean var1) {
      this.Waiting = var1;
   }

   public IsoSurvivor getDragCharacter() {
      return this.DragCharacter;
   }

   public void setDragCharacter(IsoSurvivor var1) {
      this.DragCharacter = var1;
   }

   public float getHeartDelay() {
      return this.heartDelay;
   }

   public void setHeartDelay(float var1) {
      this.heartDelay = var1;
   }

   public float getHeartDelayMax() {
      return this.heartDelayMax;
   }

   public void setHeartDelayMax(int var1) {
      this.heartDelayMax = (float)var1;
   }

   public double getHoursSurvived() {
      return this.HoursSurvived;
   }

   public void setHoursSurvived(double var1) {
      this.HoursSurvived = var1;
   }

   public float getMaxWeightDelta() {
      return this.maxWeightDelta;
   }

   public void setMaxWeightDelta(float var1) {
      this.maxWeightDelta = var1;
   }

   public String getForname() {
      return this.Forname;
   }

   public void setForname(String var1) {
      this.Forname = var1;
   }

   public String getSurname() {
      return this.Surname;
   }

   public void setSurname(String var1) {
      this.Surname = var1;
   }

   public boolean isbChangeCharacterDebounce() {
      return this.bChangeCharacterDebounce;
   }

   public void setbChangeCharacterDebounce(boolean var1) {
      this.bChangeCharacterDebounce = var1;
   }

   public int getFollowID() {
      return this.followID;
   }

   public void setFollowID(int var1) {
      this.followID = var1;
   }

   public boolean isbSeenThisFrame() {
      return this.bSeenThisFrame;
   }

   public void setbSeenThisFrame(boolean var1) {
      this.bSeenThisFrame = var1;
   }

   public boolean isbCouldBeSeenThisFrame() {
      return this.bCouldBeSeenThisFrame;
   }

   public void setbCouldBeSeenThisFrame(boolean var1) {
      this.bCouldBeSeenThisFrame = var1;
   }

   public float getTimeSinceLastStab() {
      return this.timeSinceLastStab;
   }

   public void setTimeSinceLastStab(float var1) {
      this.timeSinceLastStab = var1;
   }

   public Stack<IsoMovingObject> getLastSpotted() {
      return this.LastSpotted;
   }

   public void setLastSpotted(Stack<IsoMovingObject> var1) {
      this.LastSpotted = var1;
   }

   public int getClearSpottedTimer() {
      return this.ClearSpottedTimer;
   }

   public void setClearSpottedTimer(int var1) {
      this.ClearSpottedTimer = var1;
   }

   public boolean IsRunning() {
      return this.isRunning();
   }

   public void InitSpriteParts() {
   }

   public String getTagPrefix() {
      return this.tagPrefix;
   }

   public void setTagPrefix(String var1) {
      this.tagPrefix = var1;
   }

   public ColorInfo getTagColor() {
      return this.tagColor;
   }

   public void setTagColor(ColorInfo var1) {
      this.tagColor.set(var1);
   }

   public String getDisplayName() {
      if (this.displayName == null || this.displayName.equals("")) {
         this.displayName = this.getUsername();
      }

      return this.displayName;
   }

   public void setDisplayName(String var1) {
      this.displayName = var1;
   }

   public boolean isSeeNonPvpZone() {
      return this.seeNonPvpZone || DebugOptions.instance.Multiplayer.Debug.SeeNonPvpZones.getValue();
   }

   public boolean isSeeDesignationZone() {
      return this.seeDesignationZone;
   }

   public void setSeeDesignationZone(boolean var1) {
      this.seeDesignationZone = var1;
   }

   public void addSelectedZoneForHighlight(Double var1) {
      if (!this.selectedZonesForHighlight.contains(var1)) {
         this.selectedZonesForHighlight.add(var1);
      }

   }

   public void setSelectedZoneForHighlight(Double var1) {
      this.selectedZoneForHighlight = var1;
   }

   public Double getSelectedZoneForHighlight() {
      return this.selectedZoneForHighlight;
   }

   public ArrayList<Double> getSelectedZonesForHighlight() {
      return this.selectedZonesForHighlight;
   }

   public void resetSelectedZonesForHighlight() {
      this.selectedZonesForHighlight.clear();
   }

   public void setSeeNonPvpZone(boolean var1) {
      this.seeNonPvpZone = var1;
   }

   public boolean isShowTag() {
      return this.showTag;
   }

   public void setShowTag(boolean var1) {
      this.showTag = var1;
   }

   public boolean isFactionPvp() {
      return this.factionPvp;
   }

   public void setFactionPvp(boolean var1) {
      this.factionPvp = var1;
   }

   public boolean isForceAim() {
      return this.forceAim;
   }

   public void setForceAim(boolean var1) {
      this.forceAim = var1;
   }

   public boolean toggleForceAim() {
      this.forceAim = !this.forceAim;
      return this.forceAim;
   }

   public boolean isForceSprint() {
      return this.forceSprint;
   }

   public void setForceSprint(boolean var1) {
      this.forceSprint = var1;
   }

   public boolean toggleForceSprint() {
      this.forceSprint = !this.forceSprint;
      return this.forceSprint;
   }

   public boolean isForceRun() {
      return this.forceRun;
   }

   public void setForceRun(boolean var1) {
      this.forceRun = var1;
   }

   public boolean toggleForceRun() {
      this.forceRun = !this.forceRun;
      return this.forceRun;
   }

   public boolean isDeaf() {
      return this.Traits.Deaf.isSet();
   }

   public boolean isForceOverrideAnim() {
      return this.forceOverrideAnim;
   }

   public void setForceOverrideAnim(boolean var1) {
      this.forceOverrideAnim = var1;
   }

   public Long getMechanicsItem(String var1) {
      return (Long)this.mechanicsItem.get(Long.parseLong(var1));
   }

   public boolean isWearingNightVisionGoggles() {
      return this.isWearingNightVisionGoggles;
   }

   public void setWearingNightVisionGoggles(boolean var1) {
      this.isWearingNightVisionGoggles = var1;
   }

   public void OnAnimEvent(AnimLayer var1, AnimEvent var2) {
      super.OnAnimEvent(var1, var2);
      if (!this.CharacterActions.isEmpty()) {
         BaseAction var3 = (BaseAction)this.CharacterActions.get(0);
         var3.OnAnimEvent(var2);
      }
   }

   public void onCullStateChanged(ModelManager var1, boolean var2) {
      super.onCullStateChanged(var1, var2);
      if (!var2) {
         DebugFileWatcher.instance.add(this.m_setClothingTriggerWatcher);
      } else {
         DebugFileWatcher.instance.remove(this.m_setClothingTriggerWatcher);
      }

   }

   public boolean isTimedActionInstant() {
      return (GameClient.bClient || GameServer.bServer) && this.isAccessLevel("None") ? false : super.isTimedActionInstant();
   }

   public boolean isSkeleton() {
      return false;
   }

   public void addWorldSoundUnlessInvisible(int var1, int var2, boolean var3) {
      if (!this.isGhostMode()) {
         super.addWorldSoundUnlessInvisible(var1, var2, var3);
      }
   }

   private void updateFootInjuries() {
      InventoryItem var1 = this.getWornItems().getItem("Shoes");
      if (var1 == null || var1.getCondition() <= 0) {
         if (this.getCurrentSquare() != null) {
            if (this.getCurrentSquare().getBrokenGlass() != null) {
               BodyPartType var2 = BodyPartType.FromIndex(Rand.Next(BodyPartType.ToIndex(BodyPartType.Foot_L), BodyPartType.ToIndex(BodyPartType.Foot_R) + 1));
               BodyPart var3 = this.getBodyDamage().getBodyPart(var2);
               if (!var3.isDeepWounded() || !var3.haveGlass()) {
                  var3.generateDeepShardWound();
                  this.playerVoiceSound("PainFromGlassCut");
               }
            }

            byte var7 = 0;
            boolean var8 = false;
            if (this.getCurrentSquare().getZone() != null && (this.getCurrentSquare().getZone().getType().equals("Forest") || this.getCurrentSquare().getZone().getType().equals("DeepForest"))) {
               var8 = true;
            }

            IsoObject var4 = this.getCurrentSquare().getFloor();
            if (var4 != null && var4.getSprite() != null && var4.getSprite().getName() != null) {
               String var5 = var4.getSprite().getName();
               if (var5.contains("blends_natural_01") && var8) {
                  var7 = 2;
               } else if (!var5.contains("blends_natural_01") && this.getCurrentSquare().getBuilding() == null) {
                  var7 = 1;
               }
            }

            if (var7 != 0) {
               if (this.isWalking && !this.isRunning() && !this.isSprinting()) {
                  this.footInjuryTimer += var7;
               } else if (this.isRunning() && !this.isSprinting()) {
                  this.footInjuryTimer += var7 + 2;
               } else {
                  if (!this.isSprinting()) {
                     if (this.footInjuryTimer > 0 && Rand.Next(3) == 0) {
                        --this.footInjuryTimer;
                     }

                     return;
                  }

                  this.footInjuryTimer += var7 + 5;
               }

               if (Rand.Next(Rand.AdjustForFramerate(8500 - this.footInjuryTimer)) <= 0) {
                  this.footInjuryTimer = 0;
                  BodyPartType var9 = BodyPartType.FromIndex(Rand.Next(BodyPartType.ToIndex(BodyPartType.Foot_L), BodyPartType.ToIndex(BodyPartType.Foot_R) + 1));
                  BodyPart var6 = this.getBodyDamage().getBodyPart(var9);
                  if (var6.getScratchTime() > 30.0F) {
                     if (!var6.isCut()) {
                        var6.setCut(true);
                        var6.setCutTime(Rand.Next(1.0F, 3.0F));
                     } else {
                        var6.setCutTime(var6.getCutTime() + Rand.Next(1.0F, 3.0F));
                     }
                  } else {
                     if (!var6.scratched()) {
                        var6.setScratched(true, true);
                        var6.setScratchTime(Rand.Next(1.0F, 3.0F));
                     } else {
                        var6.setScratchTime(var6.getScratchTime() + Rand.Next(1.0F, 3.0F));
                     }

                     if (var6.getScratchTime() > 20.0F && var6.getBleedingTime() == 0.0F) {
                        var6.setBleedingTime(Rand.Next(3.0F, 10.0F));
                     }
                  }
               }

            }
         }
      }
   }

   public int getMoodleLevel(MoodleType var1) {
      return this.getMoodles().getMoodleLevel(var1);
   }

   public boolean isAttackStarted() {
      return this.attackStarted;
   }

   public void setAttackStarted(boolean var1) {
      this.attackStarted = var1;
   }

   public boolean isBehaviourMoving() {
      return this.hasPath() || super.isBehaviourMoving();
   }

   public boolean isJustMoved() {
      if (!GameServer.bServer) {
         return this.JustMoved;
      } else {
         return Math.abs(this.getX() - this.networkAI.targetX) > 0.2F || Math.abs(this.getY() - this.networkAI.targetY) > 0.2F;
      }
   }

   public void setJustMoved(boolean var1) {
      this.JustMoved = var1;
      if (GameClient.bClient && !this.networkAI.moved && this.JustMoved) {
         this.networkAI.moved = true;
         GameClient.connection.setReady(true);
      }

   }

   public boolean isPlayerMoving() {
      return this.m_isPlayerMoving;
   }

   public float getTimedActionTimeModifier() {
      return this.getBodyDamage().getThermoregulator() != null ? this.getBodyDamage().getThermoregulator().getTimedActionTimeModifier() : 1.0F;
   }

   public boolean isLookingWhileInVehicle() {
      return this.getVehicle() != null && this.bLookingWhileInVehicle;
   }

   public void setInitiateAttack(boolean var1) {
      this.initiateAttack = var1;
   }

   public boolean isIgnoreInputsForDirection() {
      return this.ignoreInputsForDirection;
   }

   public void setIgnoreInputsForDirection(boolean var1) {
      this.ignoreInputsForDirection = var1;
   }

   public boolean isIgnoreContextKey() {
      return this.ignoreContextKey;
   }

   public void setIgnoreContextKey(boolean var1) {
      this.ignoreContextKey = var1;
   }

   public boolean isIgnoreAutoVault() {
      return this.ignoreAutoVault;
   }

   public void setIgnoreAutoVault(boolean var1) {
      this.ignoreAutoVault = var1;
   }

   public boolean isAllowSprint() {
      return this.allowSprint;
   }

   public void setAllowSprint(boolean var1) {
      this.allowSprint = var1;
   }

   public boolean isAllowRun() {
      return this.allowRun;
   }

   public void setAllowRun(boolean var1) {
      this.allowRun = var1;
   }

   public String getAttackType() {
      return this.attackType;
   }

   public void setAttackType(String var1) {
      this.attackType = var1;
   }

   public void clearNetworkEvents() {
      this.networkAI.events.clear();
      this.clearVariable("PerformingAction");
      this.clearVariable("IsPerformingAnAction");
      this.overridePrimaryHandModel = null;
      this.overrideSecondaryHandModel = null;
      this.resetModelNextFrame();
   }

   public boolean isCanSeeAll() {
      return this.canSeeAll;
   }

   public void setCanSeeAll(boolean var1) {
      if (!Role.haveCapability(this, Capability.CanSeeAll)) {
         this.canSeeAll = false;
      } else {
         this.canSeeAll = var1;
      }
   }

   public boolean isNetworkTeleportEnabled() {
      return NetworkTeleport.enable;
   }

   public void setNetworkTeleportEnabled(boolean var1) {
      NetworkTeleport.enable = var1;
   }

   public boolean isCheatPlayerSeeEveryone() {
      return DebugOptions.instance.Cheat.Player.SeeEveryone.getValue();
   }

   public float getRelevantAndDistance(float var1, float var2, float var3) {
      return Math.abs(this.getX() - var1) <= var3 * 8.0F && Math.abs(this.getY() - var2) <= var3 * 8.0F ? IsoUtils.DistanceTo(this.getX(), this.getY(), var1, var2) : 1.0F / 0.0F;
   }

   public boolean isCanHearAll() {
      return this.canHearAll;
   }

   public void setCanHearAll(boolean var1) {
      if (!Role.haveCapability(this, Capability.CanHearAll)) {
         this.canHearAll = false;
      } else {
         this.canHearAll = var1;
      }
   }

   public ArrayList<String> getAlreadyReadBook() {
      return this.alreadyReadBook;
   }

   public void setMoodleCantSprint(boolean var1) {
      this.MoodleCantSprint = var1;
   }

   public void setAttackFromBehind(boolean var1) {
      this.attackFromBehind = var1;
   }

   public boolean isAttackFromBehind() {
      return this.attackFromBehind;
   }

   public float getDamageFromHitByACar(float var1) {
      float var2 = 1.0F;
      switch (SandboxOptions.instance.DamageToPlayerFromHitByACar.getValue()) {
         case 1:
            var2 = 0.0F;
            break;
         case 2:
            var2 = 0.5F;
         case 3:
         default:
            break;
         case 4:
            var2 = 2.0F;
            break;
         case 5:
            var2 = 5.0F;
      }

      float var3 = var1 * var2;
      if (var3 > 0.0F) {
         int var4 = (int)(2.0F + var3 * 0.07F);

         for(int var5 = 0; var5 < var4; ++var5) {
            int var6 = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.MAX));
            BodyPart var7 = this.getBodyDamage().getBodyPart(BodyPartType.FromIndex(var6));
            float var8 = Math.max(Rand.Next(var3 - 15.0F, var3), 5.0F);
            if (this.Traits.FastHealer.isSet()) {
               var8 *= 0.8F;
            } else if (this.Traits.SlowHealer.isSet()) {
               var8 *= 1.2F;
            }

            switch (SandboxOptions.instance.InjurySeverity.getValue()) {
               case 1:
                  var8 *= 0.5F;
                  break;
               case 3:
                  var8 *= 1.5F;
            }

            var8 *= 0.9F;
            var7.AddDamage(var8);
            if (var8 > 40.0F && Rand.Next(12) == 0) {
               var7.generateDeepWound();
            }

            if (var8 > 10.0F && Rand.Next(100) <= 10 && SandboxOptions.instance.BoneFracture.getValue()) {
               var7.generateFracture(Rand.Next(Rand.Next(10.0F, var8 + 10.0F), Rand.Next(var8 + 20.0F, var8 + 30.0F)));
            }

            if (var8 > 30.0F && Rand.Next(100) <= 80 && SandboxOptions.instance.BoneFracture.getValue() && var6 == BodyPartType.ToIndex(BodyPartType.Head)) {
               var7.generateFracture(Rand.Next(Rand.Next(10.0F, var8 + 10.0F), Rand.Next(var8 + 20.0F, var8 + 30.0F)));
            }

            if (var8 > 10.0F && Rand.Next(100) <= 60 && SandboxOptions.instance.BoneFracture.getValue() && var6 > BodyPartType.ToIndex(BodyPartType.Groin)) {
               var7.generateFracture(Rand.Next(Rand.Next(10.0F, var8 + 20.0F), Rand.Next(var8 + 30.0F, var8 + 40.0F)));
            }
         }

         this.getBodyDamage().Update();
      }

      this.addBlood(var1);
      if (GameClient.bClient && this.isLocal()) {
         this.updateMovementRates();
         GameClient.sendPlayerInjuries(this);
         GameClient.sendPlayerDamage(this);
      }

      return var3;
   }

   public float Hit(BaseVehicle var1, float var2, boolean var3, float var4, float var5) {
      float var6 = this.doBeatenVehicle(var2);
      super.Hit(var1, var2, var3, var4, var5);
      return var6;
   }

   public void Kill(IsoGameCharacter var1) {
      if (!this.isOnKillDone()) {
         if (GameServer.bServer) {
            MPStatistics.onPlayerWasKilled(this.isOnFire(), var1 instanceof IsoPlayer, var1 instanceof IsoZombie);
         }

         super.Kill(var1);
         this.getBodyDamage().setOverallBodyHealth(0.0F);
         if (DebugOptions.instance.Multiplayer.Debug.PlayerZombie.getValue()) {
            this.getBodyDamage().setInfectionLevel(100.0F);
         }

         if (var1 == null) {
            this.DoDeath((HandWeapon)null, (IsoGameCharacter)null);
         } else {
            this.DoDeath(var1.getUseHandWeapon(), var1);
         }

         if (GameClient.bClient) {
            ClientPlayerDB.getInstance().clientSendNetworkPlayerInt(this);
         }

      }
   }

   public boolean shouldDoInventory() {
      return this.isLocalPlayer();
   }

   public void becomeCorpse() {
      if (!this.isOnDeathDone()) {
         if (this.shouldBecomeCorpse() || this.isLocalPlayer()) {
            super.becomeCorpse();
            if (GameClient.bClient && this.shouldDoInventory()) {
               GameClient.sendPlayerDeath(this);
            }

            if (!GameClient.bClient) {
               IsoDeadBody var1 = new IsoDeadBody(this);
               if (this.shouldBecomeZombieAfterDeath()) {
                  var1.reanimateLater();
               }

               if (GameServer.bServer) {
                  GameServer.sendBecomeCorpse(var1);
               }
            }

         }
      }
   }

   public void preupdate() {
      if (GameClient.bClient) {
         this.networkAI.updateHitVehicle();
         if (!this.isLocal() && this.isKnockedDown() && !this.isOnFloor()) {
            HitReactionNetworkAI var1 = this.getHitReactionNetworkAI();
            if (var1.isSetup() && !var1.isStarted()) {
               var1.start();
            }
         }
      }

      super.preupdate();
   }

   public HitReactionNetworkAI getHitReactionNetworkAI() {
      return this.networkAI.hitReaction;
   }

   public NetworkCharacterAI getNetworkCharacterAI() {
      return this.networkAI;
   }

   public void setFishingStage(String var1) {
      this.setVariable("FishingStage", var1);
      if (GameClient.bClient && !this.bRemote && this.networkAI != null) {
         try {
            FishingState.FishingStage var2 = FishingState.FishingStage.valueOf(var1);
            if (!this.networkAI.fishingStage.equals(var2)) {
               this.networkAI.fishingStage = var2;
               GameClient.sendEvent(this, var1);
            }
         } catch (IllegalArgumentException var3) {
            DebugLog.Multiplayer.printException(var3, "No such FishingStage: " + var1, LogSeverity.Error);
         }
      }

   }

   public void setFitnessSpeed() {
      this.clearVariable("FitnessStruggle");
      float var1 = (float)this.getPerkLevel(PerkFactory.Perks.Fitness) / 5.0F / 1.1F - (float)this.getMoodleLevel(MoodleType.Endurance) / 20.0F;
      if (var1 > 1.5F) {
         var1 = 1.5F;
      }

      if (var1 < 0.85F) {
         var1 = 1.0F;
         this.setVariable("FitnessStruggle", true);
      }

      this.setVariable("FitnessSpeed", var1);
   }

   public boolean isLocal() {
      return super.isLocal() || this.isLocalPlayer();
   }

   public boolean isClimbOverWallSuccess() {
      return this.climbOverWallSuccess;
   }

   public void setClimbOverWallSuccess(boolean var1) {
      this.climbOverWallSuccess = var1;
   }

   public boolean isClimbOverWallStruggle() {
      return this.climbOverWallStruggle;
   }

   public void setClimbOverWallStruggle(boolean var1) {
      this.climbOverWallStruggle = var1;
   }

   public boolean isVehicleCollisionActive(BaseVehicle var1) {
      if (!super.isVehicleCollisionActive(var1)) {
         return false;
      } else if (this.isGodMod()) {
         return false;
      } else {
         IsoPlayer var2 = (IsoPlayer)GameClient.IDToPlayerMap.get(this.vehicle4testCollision.getNetPlayerId());
         if (!CombatManager.checkPVP(var2, this)) {
            return false;
         } else if (SandboxOptions.instance.DamageToPlayerFromHitByACar.getValue() < 1) {
            return false;
         } else if (this.getVehicle() == var1) {
            return false;
         } else {
            return !this.isCurrentState(PlayerFallDownState.instance()) && !this.isCurrentState(PlayerFallingState.instance()) && !this.isCurrentState(PlayerKnockedDown.instance());
         }
      }
   }

   public boolean isSkipResolveCollision() {
      if (super.isSkipResolveCollision()) {
         return true;
      } else if (this.isLocal()) {
         return false;
      } else {
         return this.isCurrentState(PlayerFallDownState.instance()) || this.isCurrentState(BumpedState.instance()) || this.isCurrentState(PlayerKnockedDown.instance()) || this.isCurrentState(PlayerHitReactionState.instance()) || this.isCurrentState(PlayerHitReactionPVPState.instance()) || this.isCurrentState(PlayerOnGroundState.instance());
      }
   }

   public boolean isShowMPInfos() {
      return this.showMPInfos;
   }

   public void setShowMPInfos(boolean var1) {
      this.showMPInfos = var1;
   }

   public MusicIntensityEvents getMusicIntensityEvents() {
      return this.m_musicIntensityEvents;
   }

   private void updateMusicIntensityEvents() {
      boolean var1 = this.getCurrentSquare() != null && this.getCurrentSquare().isInARoom();
      if (var1) {
         this.triggerMusicIntensityEvent("InsideBuilding");
      }

      this.m_musicIntensityEvents.update();
   }

   public void triggerMusicIntensityEvent(String var1) {
      MusicIntensityConfig.getInstance().triggerEvent(var1, this.getMusicIntensityEvents());
   }

   public MusicThreatStatuses getMusicThreatStatuses() {
      return this.m_musicThreatStatuses;
   }

   private void updateMusicThreatStatuses() {
      this.m_musicThreatStatuses.update();
   }

   public void addAttachedAnimal(IsoAnimal var1) {
      this.attachedAnimals.add(var1);
   }

   public ArrayList<IsoAnimal> getAttachedAnimals() {
      return this.attachedAnimals;
   }

   public void removeAttachedAnimal(IsoAnimal var1) {
      this.attachedAnimals.remove(var1);
   }

   public void removeAllAttachedAnimals() {
      for(int var1 = 0; var1 < this.attachedAnimals.size(); ++var1) {
         ((IsoAnimal)this.attachedAnimals.get(var1)).getData().attachedPlayer = null;
      }

      this.attachedAnimals.clear();
   }

   public void lureAnimal(InventoryItem var1) {
      DesignationZoneAnimal var2 = DesignationZoneAnimal.getZone((int)this.getX(), (int)this.getY(), (int)this.getZ());
      if (var2 != null) {
         for(int var3 = 0; var3 < var2.animals.size(); ++var3) {
            ((IsoAnimal)var2.animals.get(var3)).tryLure(this, var1);
         }
      }

      byte var9 = 10;

      for(int var4 = this.getSquare().x - var9; var4 < this.getSquare().x + var9; ++var4) {
         for(int var5 = this.getSquare().y - var9; var5 < this.getSquare().y + var9; ++var5) {
            IsoGridSquare var6 = this.getSquare().getCell().getGridSquare(var4, var5, this.getSquare().z);
            if (var6 != null) {
               ArrayList var7 = var6.getAnimals();

               for(int var8 = 0; var8 < var7.size(); ++var8) {
                  ((IsoAnimal)var7.get(var8)).tryLure(this, var1);
               }
            }
         }
      }

   }

   public ArrayList<IsoAnimal> getLuredAnimals() {
      return this.luredAnimals;
   }

   public void stopLuringAnimals(boolean var1) {
      if (this.getPrimaryHandItem() != null) {
         this.setIsLuringAnimals(false);

         for(int var2 = 0; var2 < this.getLuredAnimals().size(); ++var2) {
            IsoAnimal var3 = (IsoAnimal)this.getLuredAnimals().get(var2);
            if (var1) {
               var3.eatFromLured(this, this.getPrimaryHandItem());
               var3.getData().eatFood(this.getPrimaryHandItem());
            }

            var3.cancelLuring();
         }

      }
   }

   public void setIsLuringAnimals(boolean var1) {
      this.isLuringAnimals = var1;
   }

   public int getVoiceType() {
      return this.getDescriptor().getVoiceType();
   }

   public void setVoiceType(int var1) {
      this.getDescriptor().setVoiceType(PZMath.clamp(var1, 0, 3));
   }

   public void setVoicePitch(float var1) {
      this.getDescriptor().setVoicePitch(PZMath.clamp(var1, -100.0F, 100.0F));
   }

   public boolean isFarming() {
      return this.isFarming;
   }

   public void setIsFarming(boolean var1) {
      this.isFarming = var1;
   }

   public boolean tooDarkToRead() {
      if (!this.isLocalPlayer()) {
         return false;
      } else if (this.getSquare() == null) {
         return false;
      } else {
         int var1 = this.getPlayerNum();
         float var2 = this.getSquare().getLightLevel(var1);
         return (var2 / this.getWornItemsVisionModifier() + var2) / 2.0F < 0.43F;
      }
   }

   public boolean isWalking() {
      return this.isWalking;
   }

   public boolean isInvPageDirty() {
      return this.invPageDirty;
   }

   public void setInvPageDirty(boolean var1) {
      this.invPageDirty = var1;
   }

   public float getVoicePitch() {
      return this.getDescriptor().getVoicePitch();
   }

   public void setCombatSpeed(float var1) {
      this.combatSpeed = var1;
   }

   public float getCombatSpeed() {
      return this.combatSpeed;
   }

   public boolean isMeleePressed() {
      return this.m_meleePressed;
   }

   public boolean isGrapplePressed() {
      return this.m_grapplePressed;
   }

   public void setRole(Role var1) {
      if (!var1.haveCapability(Capability.ToggleWriteRoleNameAbove)) {
         this.setShowAdminTag(false);
      }

      if (!var1.haveCapability(Capability.UseZombieDontAttackCheat)) {
         this.setZombiesDontAttack(false);
      }

      if (!var1.haveCapability(Capability.ToggleGodModHimself)) {
         this.setGodMod(false);
      }

      if (!var1.haveCapability(Capability.ToggleInvisibleHimself)) {
         this.setInvisible(false);
      }

      if (!var1.haveCapability(Capability.ToggleUnlimitedEndurance)) {
         this.setUnlimitedEndurance(false);
      }

      if (!var1.haveCapability(Capability.ToggleUnlimitedCarry)) {
         this.setUnlimitedCarry(false);
      }

      if (!var1.haveCapability(Capability.UseMovablesCheat)) {
         this.setMovablesCheat(false);
      }

      if (!var1.haveCapability(Capability.UseFastMoveCheat)) {
         this.setFastMoveCheat(false);
      }

      if (!var1.haveCapability(Capability.UseBuildCheat)) {
         this.setBuildCheat(false);
      }

      if (!var1.haveCapability(Capability.UseFarmingCheat)) {
         this.setFarmingCheat(false);
      }

      if (!var1.haveCapability(Capability.UseFishingCheat)) {
         this.setFishingCheat(false);
      }

      if (!var1.haveCapability(Capability.UseHealthCheat)) {
         this.setHealthCheat(false);
      }

      if (!var1.haveCapability(Capability.UseMechanicsCheat)) {
         this.setMechanicsCheat(false);
      }

      if (!var1.haveCapability(Capability.UseTimedActionInstantCheat)) {
         this.setTimedActionInstantCheat(false);
      }

      if (!var1.haveCapability(Capability.ToggleNoclipHimself)) {
         this.setNoClip(false);
      }

      if (!var1.haveCapability(Capability.CanSeeAll)) {
         this.setCanSeeAll(false);
      }

      if (!var1.haveCapability(Capability.CanHearAll)) {
         this.setCanHearAll(false);
      }

      if (!var1.haveCapability(Capability.UseBrushToolManager)) {
         this.setCanUseBrushTool(false);
      }

      this.role = var1;
   }

   public boolean wasLastAttackHandToHand() {
      return this.m_lastAttackWasHandToHand;
   }

   public void setLastAttackWasHandToHand(boolean var1) {
      this.m_lastAttackWasHandToHand = var1;
   }

   public void petAnimal() {
      if (GameTime.getInstance().getCalender().getTimeInMillis() - this.lastAnimalPet > 3600000L) {
         this.lastAnimalPet = GameTime.getInstance().getCalender().getTimeInMillis();
         Stats var10000 = this.getStats();
         var10000.stress -= Rand.Next(0.15F, 0.35F);
         BodyDamage var1 = this.getBodyDamage();
         var1.UnhappynessLevel -= Rand.Next(15.0F, 35.0F);
         var10000 = this.getStats();
         var10000.Panic -= Rand.Next(15.0F, 35.0F);
         var10000 = this.getStats();
         var10000.Boredom -= Rand.Next(15.0F, 35.0F);
      }

   }

   public IsoAnimal getUseableAnimal() {
      if (this.getCurrentSquare() == null) {
         return null;
      } else {
         ArrayList var1 = new ArrayList();

         for(int var2 = this.getCurrentSquare().getX() - 2; var2 < this.getCurrentSquare().getX() + 2; ++var2) {
            for(int var3 = this.getCurrentSquare().getY() - 2; var3 < this.getCurrentSquare().getY() + 2; ++var3) {
               IsoGridSquare var4 = this.getCurrentSquare().getCell().getGridSquare(var2, var3, this.getCurrentSquare().getZ());
               if (var4 != null) {
                  var1.addAll(var4.getAnimals());
               }
            }
         }

         float var7 = 99.0F;
         IsoAnimal var8 = null;

         for(int var9 = 0; var9 < var1.size(); ++var9) {
            IsoAnimal var5 = (IsoAnimal)var1.get(var9);
            if (var5.getCurrentSquare() != null) {
               IsoGridSquare var6 = this.getCurrentSquare().getAdjacentSquare(this.getDir());
               if (var6.DistToProper(var5.getCurrentSquare()) < var7) {
                  var8 = var5;
                  var7 = var6.DistToProper(var5.getCurrentSquare());
               }
            }
         }

         return var8;
      }
   }

   private boolean getAnimVariable_bFalling() {
      int var1 = 0;
      IsoChunk var2 = this.getChunk();
      if (var2 != null) {
         var1 = var2.getMinLevel();
      }

      return this.getZ() > (float)var1 && this.fallTime > 2.0F;
   }

   static {
      m_isoPlayerTriggerWatcher = new PredicatedFileWatcher(ZomboidFileSystem.instance.getMessagingDirSub("Trigger_ResetIsoPlayerModel.xml"), IsoPlayer::onTrigger_ResetIsoPlayerModel);
      tempVector2_1 = new Vector2();
      tempVector2_2 = new Vector2();
      tempVector3f = new Vector3f();
      RecentlyRemoved = new ArrayList();
      s_moveVars = new MoveVars();
      s_targetsProne = new ArrayList();
      s_targetsStanding = new ArrayList();
   }

   static class InputState {
      public boolean bMelee;
      public boolean bGrapple;
      public boolean isAttacking;
      public boolean bRunning;
      public boolean bSprinting;
      boolean isAiming;
      boolean isCharging;
      boolean isChargingLT;

      InputState() {
      }
   }

   private static class VehicleContainerData {
      ArrayList<VehicleContainer> tempContainers = new ArrayList();
      ArrayList<VehicleContainer> containers = new ArrayList();
      Stack<VehicleContainer> freeContainers = new Stack();

      private VehicleContainerData() {
      }
   }

   private final class GrapplerGruntChance {
      private final int baseChance = 15;
      private final int chanceIncrement = 5;
      private int gruntChance = 15;

      private GrapplerGruntChance() {
      }
   }

   static final class MoveVars {
      float moveX;
      float moveY;
      float strafeX;
      float strafeY;
      IsoDirections NewFacing;

      MoveVars() {
      }
   }

   private static class s_performance {
      static final PerformanceProfileProbe postUpdate = new PerformanceProfileProbe("IsoPlayer.postUpdate");
      static final PerformanceProfileProbe update = new PerformanceProfileProbe("IsoPlayer.update");

      private s_performance() {
      }
   }

   private static class VehicleContainer {
      BaseVehicle vehicle;
      int containerIndex;

      private VehicleContainer() {
      }

      public VehicleContainer set(BaseVehicle var1, int var2) {
         this.vehicle = var1;
         this.containerIndex = var2;
         return this;
      }

      public boolean equals(Object var1) {
         return var1 instanceof VehicleContainer && this.vehicle == ((VehicleContainer)var1).vehicle && this.containerIndex == ((VehicleContainer)var1).containerIndex;
      }
   }
}
