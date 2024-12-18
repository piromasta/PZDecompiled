package zombie.characters;

import fmod.fmod.FMODManager;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import fmod.fmod.IFMODParameterUpdater;
import gnu.trove.map.hash.THashMap;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.UUID;
import java.util.function.Consumer;
import org.joml.GeometryUtils;
import org.joml.Vector3f;
import se.krka.kahlua.vm.KahluaTable;
import zombie.AmbientStreamManager;
import zombie.CombatManager;
import zombie.DebugFileWatcher;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.PersistentOutfits;
import zombie.PredicatedFileWatcher;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.SystemDisabler;
import zombie.VirtualZombieManager;
import zombie.WorldSoundManager;
import zombie.ZomboidFileSystem;
import zombie.ZomboidGlobals;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaHookManager;
import zombie.Lua.LuaManager;
import zombie.ai.GameCharacterAIBrain;
import zombie.ai.MapKnowledge;
import zombie.ai.State;
import zombie.ai.StateMachine;
import zombie.ai.astar.AStarPathFinder;
import zombie.ai.astar.AStarPathFinderResult;
import zombie.ai.sadisticAIDirector.SleepingEventData;
import zombie.ai.states.AttackNetworkState;
import zombie.ai.states.AttackState;
import zombie.ai.states.BumpedState;
import zombie.ai.states.ClimbDownSheetRopeState;
import zombie.ai.states.ClimbOverFenceState;
import zombie.ai.states.ClimbOverWallState;
import zombie.ai.states.ClimbSheetRopeState;
import zombie.ai.states.ClimbThroughWindowPositioningParams;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.ai.states.CloseWindowState;
import zombie.ai.states.CollideWithWallState;
import zombie.ai.states.FakeDeadZombieState;
import zombie.ai.states.FishingState;
import zombie.ai.states.GrappledThrownOutWindowState;
import zombie.ai.states.IdleState;
import zombie.ai.states.LungeNetworkState;
import zombie.ai.states.LungeState;
import zombie.ai.states.OpenWindowState;
import zombie.ai.states.PathFindState;
import zombie.ai.states.PlayerGetUpState;
import zombie.ai.states.PlayerHitReactionPVPState;
import zombie.ai.states.PlayerHitReactionState;
import zombie.ai.states.PlayerOnGroundState;
import zombie.ai.states.SmashWindowState;
import zombie.ai.states.StaggerBackState;
import zombie.ai.states.SwipeStatePlayer;
import zombie.ai.states.ThumpState;
import zombie.ai.states.WalkTowardState;
import zombie.ai.states.ZombieFallDownState;
import zombie.ai.states.ZombieFallingState;
import zombie.ai.states.ZombieHitReactionState;
import zombie.ai.states.ZombieOnGroundState;
import zombie.ai.states.animals.AnimalIdleState;
import zombie.ai.states.animals.AnimalOnGroundState;
import zombie.ai.states.animals.AnimalPathFindState;
import zombie.ai.states.animals.AnimalWalkState;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.FMODParameter;
import zombie.audio.FMODParameterList;
import zombie.audio.GameSoundClip;
import zombie.audio.parameters.ParameterVehicleHitLocation;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.BloodClothingType;
import zombie.characters.AttachedItems.AttachedItem;
import zombie.characters.AttachedItems.AttachedItems;
import zombie.characters.AttachedItems.AttachedLocationGroup;
import zombie.characters.AttachedItems.AttachedLocations;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartLast;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.BodyDamage.Metabolics;
import zombie.characters.BodyDamage.Nutrition;
import zombie.characters.CharacterTimedActions.BaseAction;
import zombie.characters.CharacterTimedActions.LuaTimedActionNew;
import zombie.characters.Moodles.MoodleType;
import zombie.characters.Moodles.Moodles;
import zombie.characters.WornItems.BodyLocationGroup;
import zombie.characters.WornItems.BodyLocations;
import zombie.characters.WornItems.WornItem;
import zombie.characters.WornItems.WornItems;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionState;
import zombie.characters.action.ActionStateSnapshot;
import zombie.characters.action.IActionStateChanged;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.characters.traits.CharacterTraits;
import zombie.characters.traits.TraitCollection;
import zombie.characters.traits.TraitFactory;
import zombie.chat.ChatElement;
import zombie.chat.ChatElementOwner;
import zombie.chat.ChatManager;
import zombie.chat.ChatMessage;
import zombie.core.BoxedStaticValues;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LoggerManager;
import zombie.core.logger.ZLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.physics.BallisticsController;
import zombie.core.physics.BallisticsTarget;
import zombie.core.physics.RagdollController;
import zombie.core.physics.RagdollStateData;
import zombie.core.profiling.PerformanceProbes;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.BaseGrappleable;
import zombie.core.skinnedmodel.IGrappleable;
import zombie.core.skinnedmodel.IGrappleableWrapper;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AdvancedAnimator;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.AnimNode;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandle;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableReference;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotCallbackBool;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotCallbackFloat;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotCallbackInt;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotCallbackString;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSource;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableType;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableMap;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.core.skinnedmodel.advancedanimation.LiveAnimNode;
import zombie.core.skinnedmodel.advancedanimation.debug.AnimatorDebugMonitor;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventBroadcaster;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventCallback;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventWrappedBroadcaster;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.AnimationMultiTrack;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.AnimatorsBoneTransform;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.model.ModelInstanceTextureCreator;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.population.BeardStyle;
import zombie.core.skinnedmodel.population.BeardStyles;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.ClothingItemReference;
import zombie.core.skinnedmodel.population.HairStyle;
import zombie.core.skinnedmodel.population.HairStyles;
import zombie.core.skinnedmodel.population.IClothingItemListener;
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
import zombie.core.utils.UpdateLimit;
import zombie.core.znet.SteamGameServer;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.debug.LogSeverity;
import zombie.entity.ComponentType;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidCategory;
import zombie.entity.components.fluids.FluidConsume;
import zombie.entity.components.fluids.FluidContainer;
import zombie.gameStates.IngameState;
import zombie.input.Mouse;
import zombie.interfaces.IUpdater;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Literature;
import zombie.inventory.types.Radio;
import zombie.inventory.types.WeaponType;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoRoofFixer;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.LosUtil;
import zombie.iso.RoomDef;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.iso.fboRenderChunk.FBORenderShadows;
import zombie.iso.objects.IsoBall;
import zombie.iso.objects.IsoBulletTracerEffects;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoFallingClothing;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.IsoMolotovCocktail;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.iso.objects.RainManager;
import zombie.iso.objects.ShadowParams;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.NetworkVariables;
import zombie.network.PVPLogTool;
import zombie.network.PacketTypes;
import zombie.network.ServerGUI;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.network.anticheats.AntiCheatXPUpdate;
import zombie.network.chat.ChatServer;
import zombie.network.chat.ChatType;
import zombie.network.fields.AttackVars;
import zombie.network.fields.HitInfo;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.VariableSyncPacket;
import zombie.pathfind.Path;
import zombie.pathfind.PathFindBehavior2;
import zombie.pathfind.PolygonalMap2;
import zombie.popman.ObjectPool;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.profanity.ProfanityFilter;
import zombie.radio.ZomboidRadio;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.Recipe;
import zombie.scripting.objects.VehicleScript;
import zombie.ui.ActionProgressBar;
import zombie.ui.TextDrawObject;
import zombie.ui.TextManager;
import zombie.ui.TutorialManager;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.util.FrameDelay;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.lambda.Invokers;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleLight;
import zombie.vehicles.VehiclePart;

public abstract class IsoGameCharacter extends IsoMovingObject implements Talker, ChatElementOwner, IAnimatable, IAnimationVariableMap, IClothingItemListener, IActionStateChanged, IAnimEventCallback, IAnimEventWrappedBroadcaster, IFMODParameterUpdater, IGrappleableWrapper, ILuaVariableSource, ILuaGameCharacter {
   public IsoAIModule ai = new IsoAIModule(this);
   private boolean ignoreAimingInput = false;
   private boolean doDeathSound = true;
   private boolean canShout = true;
   public boolean doDirtBloodEtc = true;
   private static int IID = 0;
   public static final int RENDER_OFFSET_X = 1;
   public static final int RENDER_OFFSET_Y = -89;
   public static final float s_maxPossibleTwist = 70.0F;
   private static final HashMap<Integer, SurvivorDesc> SurvivorMap = new HashMap();
   private static final int[] LevelUpLevels = new int[]{25, 75, 150, 225, 300, 400, 500, 600, 700, 800, 900, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 2400, 2600, 2800, 3000, 3200, 3400, 3600, 3800, 4000, 4400, 4800, 5200, 5600, 6000};
   protected static final Vector2 tempo = new Vector2();
   protected static final ColorInfo inf = new ColorInfo();
   public long vocalEvent;
   public long removedFromWorldMS = 0L;
   private boolean bAutoWalk = false;
   private final Vector2 autoWalkDirection = new Vector2();
   private boolean bSneaking = false;
   protected static final Vector2 tempo2 = new Vector2();
   private static final Vector2 tempVector2_1 = new Vector2();
   private static final Vector2 tempVector2_2 = new Vector2();
   private static String sleepText = null;
   protected final ArrayList<InventoryItem> savedInventoryItems = new ArrayList();
   private final String instancename;
   public final ArrayList<String> amputations = new ArrayList();
   public ModelInstance hair;
   public ModelInstance beard;
   public ModelInstance primaryHandModel;
   public ModelInstance secondaryHandModel;
   private final ActionContext actionContext = new ActionContext(this);
   public final BaseCharacterSoundEmitter emitter;
   private final FMODParameterList fmodParameters = new FMODParameterList();
   private final AnimationVariableSource m_GameVariables = new AnimationVariableSource();
   private AnimationVariableSource m_PlaybackGameVariables = null;
   private boolean bRunning = false;
   private boolean bSprinting = false;
   private boolean m_avoidDamage = false;
   public boolean callOut = false;
   public IsoGameCharacter ReanimatedCorpse;
   public int ReanimatedCorpseID = -1;
   private AnimationPlayer m_animPlayer = null;
   private boolean m_animPlayerRecordingExclusive = false;
   private boolean m_deferredMovementEnabled = true;
   public final AdvancedAnimator advancedAnimator;
   public final HashMap<State, HashMap<Object, Object>> StateMachineParams = new HashMap();
   public long clientIgnoreCollision = 0L;
   private boolean isCrit = false;
   private boolean bKnockedDown = false;
   public int bumpNbr = 0;
   public boolean upKillCount = true;
   private final ArrayList<PerkInfo> PerkList = new ArrayList();
   protected final Vector2 m_forwardDirection = new Vector2();
   public boolean Asleep = false;
   public boolean isResting = false;
   public boolean blockTurning = false;
   public float speedMod = 1.0F;
   public IsoSprite legsSprite;
   private boolean bFemale = true;
   public float knockbackAttackMod = 1.0F;
   private boolean animal = false;
   public final boolean[] IsVisibleToPlayer = new boolean[4];
   public float savedVehicleX;
   public float savedVehicleY;
   public short savedVehicleSeat = -1;
   public boolean savedVehicleRunning;
   private static final float RecoilDelayDecrease = 0.625F;
   protected static final float BeenMovingForIncrease = 1.25F;
   protected static final float BeenMovingForDecrease = 0.625F;
   private IsoGameCharacter FollowingTarget = null;
   private final ArrayList<IsoMovingObject> LocalList = new ArrayList();
   private final ArrayList<IsoMovingObject> LocalNeutralList = new ArrayList();
   private final ArrayList<IsoMovingObject> LocalGroupList = new ArrayList();
   private final ArrayList<IsoMovingObject> LocalRelevantEnemyList = new ArrayList();
   private float dangerLevels = 0.0F;
   private static final Vector2 tempVector2 = new Vector2();
   private float leaveBodyTimedown = 0.0F;
   protected boolean AllowConversation = true;
   private float ReanimateTimer;
   private int ReanimAnimFrame;
   private int ReanimAnimDelay;
   private boolean Reanim = false;
   private boolean VisibleToNPCs = true;
   private int DieCount = 0;
   private float llx = 0.0F;
   private float lly = 0.0F;
   private float llz = 0.0F;
   protected int RemoteID = -1;
   protected int NumSurvivorsInVicinity = 0;
   private float LevelUpMultiplier = 2.5F;
   protected XP xp = null;
   private int LastLocalEnemies = 0;
   private final ArrayList<IsoMovingObject> VeryCloseEnemyList = new ArrayList();
   private final HashMap<String, Location> LastKnownLocation = new HashMap();
   protected IsoGameCharacter AttackedBy = null;
   protected boolean IgnoreStaggerBack = false;
   protected boolean AttackWasSuperAttack = false;
   private int TimeThumping = 0;
   private int PatienceMax = 150;
   private int PatienceMin = 20;
   private int Patience = 20;
   protected final Stack<BaseAction> CharacterActions = new Stack();
   private int ZombieKills = 0;
   private int SurvivorKills = 0;
   private int LastZombieKills = 0;
   protected boolean superAttack = false;
   protected float ForceWakeUpTime = -1.0F;
   private float fullSpeedMod = 1.0F;
   protected float runSpeedModifier = 1.0F;
   private float walkSpeedModifier = 1.0F;
   private float combatSpeedModifier = 1.0F;
   private float clothingDiscomfortModifier = 0.0F;
   private boolean bRangedWeaponEmpty = false;
   public ArrayList<InventoryContainer> bagsWorn;
   protected boolean ForceWakeUp;
   protected final BodyDamage BodyDamage;
   private BodyDamage BodyDamageRemote = null;
   private State defaultState;
   protected WornItems wornItems = null;
   protected AttachedItems attachedItems = null;
   protected ClothingWetness clothingWetness = null;
   protected SurvivorDesc descriptor;
   private final Stack<IsoBuilding> FamiliarBuildings = new Stack();
   protected final AStarPathFinderResult finder = new AStarPathFinderResult();
   private float FireKillRate = 0.0038F;
   private int FireSpreadProbability = 6;
   protected float Health = 1.0F;
   protected boolean bDead = false;
   protected boolean bKill = false;
   private boolean useRagdoll = false;
   private boolean isEditingRagdoll = false;
   private boolean isRagdoll = false;
   private boolean ragdollFall = false;
   private boolean vehicleCollision = false;
   protected boolean bPlayingDeathSound = false;
   private boolean bDeathDragDown = false;
   protected String hurtSound = "MaleZombieHurt";
   protected ItemContainer inventory = new ItemContainer();
   protected InventoryItem leftHandItem;
   protected boolean handItemShouldSendToClients = false;
   private int NextWander = 200;
   private boolean OnFire = false;
   private int pathIndex = 0;
   protected InventoryItem rightHandItem;
   protected Color SpeakColour = new Color(1.0F, 1.0F, 1.0F, 1.0F);
   protected float slowFactor = 0.0F;
   protected float slowTimer = 0.0F;
   protected boolean bUseParts = false;
   protected boolean Speaking = false;
   private float SpeakTime = 0.0F;
   private float staggerTimeMod = 1.0F;
   protected final StateMachine stateMachine;
   protected final Moodles Moodles;
   protected final Stats stats = new Stats();
   private final Stack<String> UsedItemsOn = new Stack();
   protected HandWeapon useHandWeapon = null;
   protected IsoGridSquare attackTargetSquare;
   private float BloodImpactX = 0.0F;
   private float BloodImpactY = 0.0F;
   private float BloodImpactZ = 0.0F;
   private IsoSprite bloodSplat;
   private boolean bOnBed = false;
   private final Vector2 moveForwardVec = new Vector2();
   protected boolean pathing = false;
   protected ChatElement chatElement;
   private final Stack<IsoGameCharacter> LocalEnemyList = new Stack();
   protected final Stack<IsoGameCharacter> EnemyList = new Stack();
   public final CharacterTraits Traits = new CharacterTraits();
   private int maxWeight = 8;
   private int maxWeightBase = 8;
   private float SleepingTabletEffect = 0.0F;
   private float SleepingTabletDelta = 1.0F;
   private float BetaEffect = 0.0F;
   private float BetaDelta = 0.0F;
   private float DepressEffect = 0.0F;
   private float DepressDelta = 0.0F;
   private float DepressFirstTakeTime = -1.0F;
   private float PainEffect = 0.0F;
   private float PainDelta = 0.0F;
   private boolean bDoDefer = true;
   private float haloDispTime = 128.0F;
   protected TextDrawObject userName;
   private TextDrawObject haloNote;
   private static final String namePvpSuffix = " [img=media/ui/Skull.png]";
   private static final String nameCarKeySuffix = " [img=media/ui/CarKey.png";
   private static final String voiceSuffix = "[img=media/ui/voiceon.png] ";
   private static final String voiceMuteSuffix = "[img=media/ui/voicemuted.png] ";
   protected IsoPlayer isoPlayer = null;
   private boolean hasInitTextObjects = false;
   private boolean canSeeCurrent = false;
   private boolean drawUserName = false;
   private final Location LastHeardSound = new Location(-1, -1, -1);
   protected boolean bClimbing = false;
   private boolean lastCollidedW = false;
   private boolean lastCollidedN = false;
   protected float fallTime = 0.0F;
   protected float lastFallSpeed = 0.0F;
   protected boolean bFalling = false;
   protected BaseVehicle vehicle = null;
   boolean isNPC = false;
   private long lastBump = 0L;
   private IsoGameCharacter bumpedChr = null;
   private boolean m_isCulled = true;
   private int age = 25;
   private int lastHitCount = 0;
   private Safety safety = new Safety(this);
   private float meleeDelay = 0.0F;
   private float RecoilDelay = 0.0F;
   private float BeenMovingFor = 0.0F;
   private float BeenSprintingFor = 0.0F;
   private float AimingDelay = 0.0F;
   private boolean forceShove = false;
   private String clickSound = null;
   private float reduceInfectionPower = 0.0F;
   private final List<String> knownRecipes = new ArrayList();
   private final HashSet<String> knownMediaLines = new HashSet();
   private int lastHourSleeped = 0;
   protected float timeOfSleep = 0.0F;
   protected float delayToActuallySleep = 0.0F;
   private String bedType = "averageBed";
   private IsoObject bed = null;
   private boolean isReading = false;
   private float timeSinceLastSmoke = 0.0F;
   private boolean wasOnStairs = false;
   private ChatMessage lastChatMessage;
   private String lastSpokenLine;
   private final Cheats m_cheats = new Cheats();
   private boolean showAdminTag = true;
   private long isAnimForecasted = 0L;
   private boolean fallOnFront = false;
   private boolean bKilledByFall = false;
   private boolean hitFromBehind = false;
   private String hitReaction = "";
   private String bumpType = "";
   private boolean m_isBumpDone = false;
   private boolean m_bumpFall = false;
   private boolean m_bumpStaggered = false;
   private String m_bumpFallType = "";
   private int sleepSpeechCnt = 0;
   private Radio equipedRadio;
   private InventoryItem leftHandCache;
   private InventoryItem rightHandCache;
   private InventoryItem backCache;
   private final ArrayList<ReadBook> ReadBooks = new ArrayList();
   public final LightInfo lightInfo = new LightInfo();
   private final LightInfo lightInfo2 = new LightInfo();
   private Path path2;
   private final MapKnowledge mapKnowledge = new MapKnowledge();
   protected final AttackVars attackVars = new AttackVars();
   private final ArrayList<HitInfo> hitInfoList = new ArrayList();
   private final PathFindBehavior2 pfb2 = new PathFindBehavior2(this);
   private final InventoryItem[] cacheEquiped = new InventoryItem[2];
   private boolean bAimAtFloor = false;
   protected int m_persistentOutfitId = 0;
   protected boolean m_bPersistentOutfitInit = false;
   private boolean bUpdateModelTextures = false;
   private ModelInstanceTextureCreator textureCreator = null;
   public boolean bUpdateEquippedTextures = false;
   private final ArrayList<ModelInstance> readyModelData = new ArrayList();
   private boolean m_isSitOnFurniture = false;
   private IsoObject m_sitOnFurnitureObject = null;
   private IsoDirections m_sitOnFurnitureDirection = null;
   private boolean sitOnGround = false;
   private boolean ignoreMovement = false;
   private boolean hideWeaponModel = false;
   private boolean m_isAiming = false;
   private float beardGrowTiming = -1.0F;
   private float hairGrowTiming = -1.0F;
   private float m_moveDelta = 1.0F;
   protected float m_turnDeltaNormal = 1.0F;
   protected float m_turnDeltaRunning = 0.8F;
   protected float m_turnDeltaSprinting = 0.75F;
   private float m_maxTwist = 15.0F;
   private boolean m_isMoving = false;
   private boolean m_isTurning = false;
   private boolean m_isTurningAround = false;
   private boolean m_isTurning90 = false;
   private boolean invincible = false;
   private float lungeFallTimer = 0.0F;
   private SleepingEventData m_sleepingEventData;
   private final int HAIR_GROW_TIME = 20;
   private final int BEARD_GROW_TIME = 5;
   public float realx = 0.0F;
   public float realy = 0.0F;
   public byte realz = 0;
   public NetworkVariables.ZombieState realState;
   public IsoDirections realdir;
   public String overridePrimaryHandModel;
   public String overrideSecondaryHandModel;
   public boolean forceNullOverride;
   protected final UpdateLimit ulBeatenVehicle;
   private float m_momentumScalar;
   private final HashMap<String, State> m_aiStateMap;
   private boolean m_isPerformingAttackAnim;
   private boolean m_isPerformingShoveAnim;
   private boolean m_isPerformingStompAnim;
   private NetworkTeleport teleport;
   private float wornItemsVisionModifier;
   private float wornItemsHearingModifier;
   private float corpseSicknessRate;
   private float blurFactor;
   private float blurFactorTarget;
   public boolean usernameDisguised;
   private float climbRopeTime;
   /** @deprecated */
   @Deprecated
   public ArrayList<Integer> invRadioFreq;
   private final PredicatedFileWatcher m_animStateTriggerWatcher;
   private final AnimationPlayerRecorder m_animationRecorder;
   private final String m_UID;
   private boolean m_bDebugVariablesRegistered;
   private float effectiveEdibleBuffTimer;
   private final HashMap<String, Integer> readLiterature;
   private IsoGameCharacter lastHitCharacter;
   private String m_fireMode;
   protected BallisticsController ballisticsController;
   protected BallisticsTarget ballisticsTarget;
   private final BaseGrappleable m_grappleable;
   private boolean m_isAnimatingBackwards;
   private float m_animationTimeScale;
   private boolean m_animationUpdatingThisFrame;
   private final FrameDelay m_animationInvisibleFrameDelay;
   private long muzzleFlashDuration;
   public long lastAnimalPet;
   private final AnimEventBroadcaster m_animEventBroadcaster;
   public IsoGameCharacter vbdebugHitTarget;
   private String m_hitDirEnum;
   private boolean m_isGrappleThrowOutWindow;
   private static final Vector3f tempVector3f00 = new Vector3f();
   private static final Vector3f tempVector3f01 = new Vector3f();
   private final Recoil m_recoil;
   public boolean doRender;
   public boolean usePhysicHitReaction;
   private float m_shadowFM;
   private float m_shadowBM;
   private long shadowTick;
   private static final ItemVisuals tempItemVisuals = new ItemVisuals();
   private static final ArrayList<IsoMovingObject> movingStatic = new ArrayList();
   final PerformanceProbes.Invokable.Params0.IProbe postUpdateInternal;
   final PerformanceProbes.Invokable.Params0.IProbe updateInternal;
   private static final Bandages s_bandages = new Bandages();
   private static final Vector3 tempVector = new Vector3();
   private static final Vector3 tempVectorBonePos = new Vector3();
   public final NetworkCharacter networkCharacter;
   private static boolean useBallistics = true;

   public IsoGameCharacter(IsoCell var1, float var2, float var3, float var4) {
      super(var1, false);
      this.realState = NetworkVariables.ZombieState.Idle;
      this.realdir = IsoDirections.fromIndex(0);
      this.overridePrimaryHandModel = null;
      this.overrideSecondaryHandModel = null;
      this.forceNullOverride = false;
      this.ulBeatenVehicle = new UpdateLimit(200L);
      this.m_momentumScalar = 0.0F;
      this.m_aiStateMap = new HashMap();
      this.m_isPerformingAttackAnim = false;
      this.m_isPerformingShoveAnim = false;
      this.m_isPerformingStompAnim = false;
      this.teleport = null;
      this.wornItemsVisionModifier = 1.0F;
      this.wornItemsHearingModifier = 1.0F;
      this.corpseSicknessRate = 0.0F;
      this.blurFactor = 0.0F;
      this.blurFactorTarget = 0.0F;
      this.usernameDisguised = false;
      this.climbRopeTime = 0.0F;
      this.invRadioFreq = new ArrayList();
      this.m_bDebugVariablesRegistered = false;
      this.effectiveEdibleBuffTimer = 0.0F;
      this.readLiterature = new HashMap();
      this.lastHitCharacter = null;
      this.m_fireMode = "";
      this.ballisticsController = null;
      this.ballisticsTarget = null;
      this.m_isAnimatingBackwards = false;
      this.m_animationTimeScale = 1.0F;
      this.m_animationUpdatingThisFrame = true;
      this.m_animationInvisibleFrameDelay = new FrameDelay(4);
      this.muzzleFlashDuration = -1L;
      this.lastAnimalPet = 0L;
      this.m_animEventBroadcaster = new AnimEventBroadcaster();
      this.vbdebugHitTarget = null;
      this.m_hitDirEnum = "FRONT";
      this.m_isGrappleThrowOutWindow = false;
      this.m_recoil = new Recoil();
      this.doRender = true;
      this.usePhysicHitReaction = false;
      this.m_shadowFM = 0.0F;
      this.m_shadowBM = 0.0F;
      this.shadowTick = -1L;
      this.postUpdateInternal = PerformanceProbes.create("IsoGameCharacter.postUpdate", this, (Invokers.Params1.ICallback)(IsoGameCharacter::postUpdateInternal));
      this.updateInternal = PerformanceProbes.create("IsoGameCharacter.update", this, (Invokers.Params1.ICallback)(IsoGameCharacter::updateInternal));
      this.networkCharacter = new NetworkCharacter();
      this.m_UID = String.format("%s-%s", this.getClass().getSimpleName(), UUID.randomUUID().toString());
      this.m_grappleable = new BaseGrappleable(this);
      this.getWrappedGrappleable().setOnGrappledEndCallback(this::onGrappleEnded);
      this.registerVariableCallbacks();
      this.registerAnimEventCallbacks();
      String var10001 = this.getClass().getSimpleName();
      this.instancename = var10001 + IID;
      ++IID;
      if (!(this instanceof IsoSurvivor)) {
         this.emitter = (BaseCharacterSoundEmitter)(!Core.SoundDisabled && !GameServer.bServer ? new CharacterSoundEmitter(this) : new DummyCharacterSoundEmitter(this));
      } else {
         this.emitter = null;
      }

      if (var2 != 0.0F || var3 != 0.0F || var4 != 0.0F) {
         if (this.getCell().isSafeToAdd()) {
            this.getCell().getObjectList().add(this);
         } else {
            this.getCell().getAddList().add(this);
         }
      }

      if (this.def == null) {
         this.def = IsoSpriteInstance.get(this.sprite);
      }

      if (this instanceof IsoPlayer && !(this instanceof IsoAnimal)) {
         this.BodyDamage = new BodyDamage(this);
         this.Moodles = new Moodles(this);
         this.xp = new XP(this);
      } else {
         if (this instanceof IsoAnimal) {
            this.BodyDamage = new BodyDamage(this);
         } else {
            this.BodyDamage = null;
         }

         this.Moodles = null;
         this.xp = null;
      }

      this.Patience = Rand.Next(this.PatienceMin, this.PatienceMax);
      this.setX(var2 + 0.5F);
      this.setY(var3 + 0.5F);
      this.setZ(var4);
      this.setScriptNextX(this.setLastX(this.setNextX(var2)));
      this.setScriptNextY(this.setLastY(this.setNextY(var3)));
      if (var1 != null) {
         this.current = this.getCell().getGridSquare(PZMath.fastfloor(var2), PZMath.fastfloor(var3), (int)PZMath.floor(var4));
      }

      this.offsetY = 0.0F;
      this.offsetX = 0.0F;
      this.stateMachine = new StateMachine(this);
      this.setDefaultState(IdleState.instance());
      this.inventory.parent = this;
      this.inventory.setExplored(true);
      this.chatElement = new ChatElement(this, 1, "character");
      this.m_animationRecorder = new AnimationPlayerRecorder(this);
      this.advancedAnimator = new AdvancedAnimator();
      this.advancedAnimator.init(this);
      this.advancedAnimator.animCallbackHandlers.add(this);
      this.advancedAnimator.SetAnimSet(AnimationSet.GetAnimationSet(this.GetAnimSetName(), false));
      this.advancedAnimator.setRecorder(this.m_animationRecorder);
      this.actionContext.onStateChanged.add(this);
      this.m_animStateTriggerWatcher = new PredicatedFileWatcher(ZomboidFileSystem.instance.getMessagingDirSub("Trigger_SetAnimState.xml"), AnimStateTriggerXmlFile.class, this::onTrigger_setAnimStateToTriggerFile);
   }

   private void registerVariableCallbacks() {
      this.setVariable("hitreaction", this::getHitReaction, this::setHitReaction);
      this.setVariable("collidetype", this::getCollideType, this::setCollideType);
      this.setVariable("footInjuryType", this::getFootInjuryType);
      this.setVariable("bumptype", this::getBumpType, this::setBumpType);
      this.setVariable("onbed", this::isOnBed, this::setOnBed);
      this.setVariable("sittingonfurniture", this::isSittingOnFurniture, this::setSittingOnFurniture);
      this.setVariable("sitonground", this::isSitOnGround, this::setSitOnGround);
      this.setVariable("canclimbdownrope", this::canClimbDownSheetRopeInCurrentSquare);
      this.setVariable("frombehind", this::isHitFromBehind, this::setHitFromBehind);
      this.setVariable("fallonfront", this::isFallOnFront, this::setFallOnFront);
      this.setVariable("killedbyfall", this::isKilledByFall, this::setKilledByFall);
      this.setVariable("hashitreaction", this::hasHitReaction);
      this.setVariable("intrees", this::isInTreesNoBush);
      this.setVariable("bumped", this::isBumped);
      this.setVariable("BumpDone", false, this::isBumpDone, this::setBumpDone);
      this.setVariable("BumpFall", false, this::isBumpFall, this::setBumpFall);
      this.setVariable("BumpFallType", "", this::getBumpFallType, this::setBumpFallType);
      this.setVariable("BumpStaggered", false, this::isBumpStaggered, this::setBumpStaggered);
      this.setVariable("bonfloor", this::isOnFloor, this::setOnFloor);
      this.setVariable("rangedweaponempty", this::isRangedWeaponEmpty, this::setRangedWeaponEmpty);
      this.setVariable("footInjury", this::hasFootInjury);
      this.setVariable("ChopTreeSpeed", 1.0F, this::getChopTreeSpeed);
      this.setVariable("MoveDelta", 1.0F, this::getMoveDelta, this::setMoveDelta);
      this.setVariable("TurnDelta", 1.0F, this::getTurnDelta, this::setTurnDelta);
      this.setVariable("angle", this::getDirectionAngle, this::setDirectionAngle);
      this.setVariable("animAngle", this::getAnimAngle);
      this.setVariable("twist", this::getTwist);
      this.setVariable("targetTwist", this::getTargetTwist);
      this.setVariable("maxTwist", this.m_maxTwist, this::getMaxTwist, this::setMaxTwist);
      this.setVariable("shoulderTwist", this::getShoulderTwist);
      this.setVariable("excessTwist", this::getExcessTwist);
      this.setVariable("numTwistBones", this::getNumTwistBones);
      this.setVariable("angleStepDelta", this::getAnimAngleStepDelta);
      this.setVariable("angleTwistDelta", this::getAnimAngleTwistDelta);
      this.setVariable("isTurning", false, this::isTurning, this::setTurning);
      this.setVariable("isTurning90", false, this::isTurning90, this::setTurning90);
      this.setVariable("isTurningAround", false, this::isTurningAround, this::setTurningAround);
      this.setVariable("bMoving", false, this::isMoving, this::setMoving);
      this.setVariable("beenMovingFor", this::getBeenMovingFor);
      this.setVariable("previousState", this::getPreviousActionContextStateName);
      this.setVariable("momentumScalar", this::getMomentumScalar, this::setMomentumScalar);
      this.setVariable("hasTimedActions", this::hasTimedActions);
      this.setVariable("isOverEncumbered", this::isOverEncumbered);
      if (DebugOptions.instance.Character.Debug.RegisterDebugVariables.getValue()) {
         this.registerDebugGameVariables();
      }

      this.setVariable("CriticalHit", this::isCriticalHit, this::setCriticalHit);
      this.setVariable("bKnockedDown", this::isKnockedDown, this::setKnockedDown);
      this.setVariable("bdead", this::isDead);
      this.setVariable("fallTime", this::getFallTimeAdjusted);
      this.setVariable("fallTimeAdjusted", this::getFallTimeAdjusted);
      this.setVariable("lastFallSpeed", this::getLastFallSpeed);
      this.setVariable("bHardFall", false);
      this.setVariable("bHardFall2", false);
      this.setVariable("bLandLight", false);
      this.setVariable("bLandLightMask", false);
      this.setVariable("bGetUpFromKnees", false);
      this.setVariable("bGetUpFromProne", false);
      this.setVariable("aim", this::isAiming);
      this.setVariable("AttackAnim", this::isPerformingAttackAnimation, this::setPerformingAttackAnimation);
      this.setVariable("ShoveAnim", this::isPerformingShoveAnimation, this::setPerformingShoveAnimation);
      this.setVariable("StompAnim", this::isPerformingStompAnimation, this::setPerformingStompAnimation);
      this.setVariable("PerformingHostileAnim", this::isPerformingHostileAnimation);
      this.setVariable("FireMode", this::getFireMode);
      this.setVariable("ShoutType", this::getShoutType);
      this.setVariable("ShoutItemModel", this::getShoutItemModel);
      this.setVariable("isAnimatingBackwards", this::isAnimatingBackwards, this::setAnimatingBackwards);
      BaseGrappleable.RegisterGrappleVariables(this.getGameVariablesInternal(), this);
      this.setVariable("GrappleThrowOutWindow", this::isGrappleThrowOutWindow, this::setGrappleThrowOutWindow);
      this.setVariable("canRagdoll", this::canRagdoll);
      this.setVariable("isEditingRagdoll", this::isEditingRagdoll, this::setEditingRagdoll);
      this.setVariable("isRagdoll", this::isRagdoll, this::setIsRagdoll);
      this.setVariable("isSimulationActive", this::isRagdollSimulationActive);
      this.setVariable("isUpright", this::isUpright);
      this.setVariable("isOnBack", this::isOnBack);
      this.setVariable("isRagdollFall", this::isRagdollFall, this::setRagdollFall);
      this.setVariable("isVehicleCollision", this::isVehicleCollision, this::setVehicleCollision);
      this.setVariable("usePhysicHitReaction", this::usePhysicHitReaction, this::setUsePhysicHitReaction);
      this.setVariable("useRagdollVehicleCollision", this::useRagdollVehicleCollision);
      this.setVariable("hitforce", this::getHitForce);
      this.setVariable("hitDir", this::getHitDirEnum);
      this.setVariable("hitDir.x", () -> {
         return this.getHitDir().x;
      });
      this.setVariable("hitDir.y", () -> {
         return this.getHitDir().y;
      });
      this.setVariable("recoilVarX", this::getRecoilVarX, this::setRecoilVarX);
      this.setVariable("recoilVarY", this::getRecoilVarY, this::setRecoilVarY);
   }

   private void registerAnimEventCallbacks() {
      this.addAnimEventListener(this::OnAnimEvent_SetVariable);
      this.addAnimEventListener("ClearVariable", this::OnAnimEvent_ClearVariable);
      this.addAnimEventListener("PlaySound", this::OnAnimEvent_PlaySound);
      this.addAnimEventListener("Footstep", this::OnAnimEvent_Footstep);
      this.addAnimEventListener("DamageWhileInTrees", this::OnAnimEvent_DamageWhileInTrees);
      this.addAnimEventListener("SetSharedGrappleType", this::OnAnimEvent_SetSharedGrappleType);
      this.addAnimEventListener("GrapplerLetGo", this::OnAnimEvent_GrapplerLetGo);
      this.addAnimEventListener("FallOnFront", this::OnAnimEvent_FallOnFront);
      this.addAnimEventListener("SetOnFloor", this::OnAnimEvent_SetOnFloor);
      this.addAnimEventListener("SetKnockedDown", this::OnAnimEvent_SetKnockedDown);
      this.addAnimEventListener("IsAlmostUp", this::OnAnimEvent_IsAlmostUp);
   }

   private void OnAnimEvent_GrapplerLetGo(IsoGameCharacter var1, String var2) {
      if (GameServer.bServer) {
         DebugLog.Network.println("GrapplerLetGo.");
      }

      LuaEventManager.triggerEvent("GrapplerLetGo", var1, var2);
      var1.LetGoOfGrappled(var2);
   }

   private void OnAnimEvent_FallOnFront(IsoGameCharacter var1, boolean var2) {
      var1.setFallOnFront(var2);
   }

   private void OnAnimEvent_SetOnFloor(IsoGameCharacter var1, boolean var2) {
      var1.setOnFloor(var2);
   }

   private void OnAnimEvent_SetKnockedDown(IsoGameCharacter var1, boolean var2) {
      var1.setKnockedDown(var2);
   }

   protected void OnAnimEvent_IsAlmostUp(IsoGameCharacter var1) {
      var1.setOnFloor(false);
      var1.setKnockedDown(false);
      var1.setSitOnGround(false);
   }

   public void setMuzzleFlashDuration(long var1) {
      this.muzzleFlashDuration = var1;
   }

   private void onGrappleEnded() {
      this.setGrappleThrowOutWindow(false);
   }

   public float getRecoilVarX() {
      return this.m_recoil.m_recoilVarX;
   }

   public void setRecoilVarX(float var1) {
      this.m_recoil.m_recoilVarX = var1;
   }

   public float getRecoilVarY() {
      return this.m_recoil.m_recoilVarY;
   }

   public void setRecoilVarY(float var1) {
      this.m_recoil.m_recoilVarY = var1;
   }

   public void setGrappleThrowOutWindow(boolean var1) {
      this.m_isGrappleThrowOutWindow = var1;
   }

   public boolean isGrappleThrowOutWindow() {
      return this.m_isGrappleThrowOutWindow;
   }

   public void updateRecoilVar() {
      this.setRecoilVarY(0.0F);
      this.setRecoilVarX(0.0F + (float)this.getPerkLevel(PerkFactory.Perks.Aiming) / 10.0F);
   }

   private void registerDebugGameVariables() {
      for(int var1 = 0; var1 < 2; ++var1) {
         for(int var2 = 0; var2 < 9; ++var2) {
            this.dbgRegisterAnimTrackVariable(var1, var2);
         }
      }

      this.setVariable("dbg.anm.dx", () -> {
         return this.getDeferredMovement(tempo).x / GameTime.instance.getMultiplier();
      });
      this.setVariable("dbg.anm.dy", () -> {
         return this.getDeferredMovement(tempo).y / GameTime.instance.getMultiplier();
      });
      this.setVariable("dbg.anm.da", () -> {
         return this.getDeferredAngleDelta() / GameTime.instance.getMultiplier();
      });
      this.setVariable("dbg.anm.daw", this::getDeferredRotationWeight);
      this.setVariable("dbg.forward", () -> {
         float var10000 = this.getForwardDirection().x;
         return "" + var10000 + "; " + this.getForwardDirection().y;
      });
      this.setVariable("dbg.anm.blend.fbx_x", () -> {
         return DebugOptions.instance.Animation.BlendUseFbx.getValue() ? 1.0F : 0.0F;
      });
      this.m_bDebugVariablesRegistered = true;
   }

   private void dbgRegisterAnimTrackVariable(int var1, int var2) {
      this.setVariable(String.format("dbg.anm.track%d%d", var1, var2), () -> {
         return this.dbgGetAnimTrackName(var1, var2);
      });
      this.setVariable(String.format("dbg.anm.t.track%d%d", var1, var2), () -> {
         return this.dbgGetAnimTrackTime(var1, var2);
      });
      this.setVariable(String.format("dbg.anm.w.track%d%d", var1, var2), () -> {
         return this.dbgGetAnimTrackWeight(var1, var2);
      });
   }

   public float getMomentumScalar() {
      return this.m_momentumScalar;
   }

   public void setMomentumScalar(float var1) {
      this.m_momentumScalar = var1;
   }

   public Vector2 getDeferredMovement(Vector2 var1) {
      return this.getDeferredMovement(var1, false);
   }

   protected Vector2 getDeferredMovement(Vector2 var1, boolean var2) {
      if (!this.hasAnimationPlayer()) {
         var1.set(0.0F, 0.0F);
         return var1;
      } else {
         this.m_animPlayer.getDeferredMovement(var1, var2);
         return var1;
      }
   }

   public Vector2 getDeferredMovementFromRagdoll(Vector2 var1) {
      if (!this.hasAnimationPlayer()) {
         var1.set(0.0F, 0.0F);
         return var1;
      } else {
         return this.m_animPlayer.getDeferredMovementFromRagdoll(var1);
      }
   }

   public float getDeferredAngleDelta() {
      return this.m_animPlayer == null ? 0.0F : this.m_animPlayer.getDeferredAngleDelta() * 57.295776F;
   }

   public float getDeferredRotationWeight() {
      return this.m_animPlayer == null ? 0.0F : this.m_animPlayer.getDeferredRotationWeight();
   }

   public zombie.core.math.Vector3 getTargetGrapplePos(zombie.core.math.Vector3 var1) {
      if (this.m_animPlayer == null) {
         var1.set(0.0F, 0.0F, 0.0F);
         return var1;
      } else {
         return this.m_animPlayer.getTargetGrapplePos(var1);
      }
   }

   public Vector3 getTargetGrapplePos(Vector3 var1) {
      if (this.m_animPlayer == null) {
         var1.set(0.0F, 0.0F, 0.0F);
         return var1;
      } else {
         return this.m_animPlayer.getTargetGrapplePos(var1);
      }
   }

   public void setTargetGrapplePos(float var1, float var2, float var3) {
      if (this.m_animPlayer != null) {
         this.m_animPlayer.setTargetGrapplePos(var1, var2, var3);
      }

   }

   public Vector2 getTargetGrappleRotation(Vector2 var1) {
      if (this.m_animPlayer == null) {
         var1.set(1.0F, 0.0F);
         return var1;
      } else {
         return this.m_animPlayer.getTargetGrappleRotation(var1);
      }
   }

   public boolean isStrafing() {
      return this.getPath2() != null && this.pfb2.isStrafing() ? true : this.isAiming();
   }

   public AnimationTrack dbgGetAnimTrack(int var1, int var2) {
      if (this.m_animPlayer == null) {
         return null;
      } else {
         AnimationPlayer var3 = this.m_animPlayer;
         AnimationMultiTrack var4 = var3.getMultiTrack();
         List var5 = var4.getTracks();
         AnimationTrack var6 = null;
         int var7 = 0;
         int var8 = 0;

         for(int var9 = var5.size(); var7 < var9; ++var7) {
            AnimationTrack var10 = (AnimationTrack)var5.get(var7);
            int var11 = var10.getLayerIdx();
            if (var11 == var1) {
               if (var8 == var2) {
                  var6 = var10;
                  break;
               }

               ++var8;
            }
         }

         return var6;
      }
   }

   public String dbgGetAnimTrackName(int var1, int var2) {
      AnimationTrack var3 = this.dbgGetAnimTrack(var1, var2);
      return var3 != null ? var3.getName() : "";
   }

   public float dbgGetAnimTrackTime(int var1, int var2) {
      AnimationTrack var3 = this.dbgGetAnimTrack(var1, var2);
      return var3 != null ? var3.getCurrentTrackTime() : 0.0F;
   }

   public float dbgGetAnimTrackWeight(int var1, int var2) {
      AnimationTrack var3 = this.dbgGetAnimTrack(var1, var2);
      return var3 != null ? var3.BlendDelta : 0.0F;
   }

   public float getTwist() {
      return this.m_animPlayer != null ? 57.295776F * this.m_animPlayer.getTwistAngle() : 0.0F;
   }

   public float getShoulderTwist() {
      return this.m_animPlayer != null ? 57.295776F * this.m_animPlayer.getShoulderTwistAngle() : 0.0F;
   }

   public float getMaxTwist() {
      return this.m_maxTwist;
   }

   public void setMaxTwist(float var1) {
      this.m_maxTwist = var1;
   }

   public float getExcessTwist() {
      return this.m_animPlayer != null ? 57.295776F * this.m_animPlayer.getExcessTwistAngle() : 0.0F;
   }

   public int getNumTwistBones() {
      return this.m_animPlayer != null ? this.m_animPlayer.getNumTwistBones() : 0;
   }

   public float getAbsoluteExcessTwist() {
      return Math.abs(this.getExcessTwist());
   }

   public float getAnimAngleTwistDelta() {
      return this.m_animPlayer != null ? this.m_animPlayer.angleTwistDelta : 0.0F;
   }

   public float getAnimAngleStepDelta() {
      return this.m_animPlayer != null ? this.m_animPlayer.angleStepDelta : 0.0F;
   }

   public float getTargetTwist() {
      return this.m_animPlayer != null ? 57.295776F * this.m_animPlayer.getTargetTwistAngle() : 0.0F;
   }

   public boolean isRangedWeaponEmpty() {
      return this.bRangedWeaponEmpty;
   }

   public void setRangedWeaponEmpty(boolean var1) {
      this.bRangedWeaponEmpty = var1;
   }

   public boolean hasFootInjury() {
      return !StringUtils.isNullOrWhitespace(this.getFootInjuryType());
   }

   public boolean isInTrees2(boolean var1) {
      if (this.isCurrentState(BumpedState.instance())) {
         return false;
      } else {
         IsoGridSquare var2 = this.getCurrentSquare();
         if (var2 == null) {
            return false;
         } else {
            if (var2.Has(IsoObjectType.tree)) {
               IsoTree var3 = var2.getTree();
               if (var3 == null || var1 && var3.getSize() > 2 || !var1) {
                  return true;
               }
            }

            String var4 = var2.getProperties().Val("Movement");
            if (!"HedgeLow".equalsIgnoreCase(var4) && !"HedgeHigh".equalsIgnoreCase(var4)) {
               return !var1 && var2.hasBush();
            } else {
               return true;
            }
         }
      }
   }

   public boolean isInTreesNoBush() {
      return this.isInTrees2(true);
   }

   public boolean isInTrees() {
      return this.isInTrees2(false);
   }

   public static HashMap<Integer, SurvivorDesc> getSurvivorMap() {
      return SurvivorMap;
   }

   public static int[] getLevelUpLevels() {
      return LevelUpLevels;
   }

   public static Vector2 getTempo() {
      return tempo;
   }

   public static Vector2 getTempo2() {
      return tempo2;
   }

   public static ColorInfo getInf() {
      return inf;
   }

   public boolean getIsNPC() {
      return this.isNPC;
   }

   public void setIsNPC(boolean var1) {
      this.isNPC = var1;
   }

   public BaseCharacterSoundEmitter getEmitter() {
      return this.emitter;
   }

   public void updateEmitter() {
      this.getFMODParameters().update();
      if (IsoWorld.instance.emitterUpdate || this.emitter.hasSoundsToStart()) {
         if (this.isZombie() && this.isProne()) {
            CombatManager.getBoneWorldPos(this, "Bip01_Head", tempVectorBonePos);
            this.emitter.set(tempVectorBonePos.x, tempVectorBonePos.y, this.getZ());
            this.emitter.tick();
         } else {
            this.emitter.set(this.getX(), this.getY(), this.getZ());
            this.emitter.tick();
         }
      }
   }

   protected void doDeferredMovement() {
      if (GameClient.bClient && this.getHitReactionNetworkAI() != null) {
         if (this.getHitReactionNetworkAI().isStarted()) {
            this.getHitReactionNetworkAI().move();
            return;
         }

         if (this.isDead() && this.getHitReactionNetworkAI().isDoSkipMovement()) {
            return;
         }
      }

      if (this.hasAnimationPlayer()) {
         if (this.isAnimationUpdatingThisFrame()) {
            Vector2 var1 = tempo;
            this.getDeferredMovement(var1, true);
            if (this.getPath2() != null && !this.isCurrentState(ClimbOverFenceState.instance()) && !this.isCurrentState(ClimbThroughWindowState.instance())) {
               if (this.isCurrentState(WalkTowardState.instance()) || this.isCurrentState(AnimalWalkState.instance())) {
                  DebugLog.General.warn("WalkTowardState but path2 != null");
                  this.setPath2((Path)null);
               }

            } else {
               if (GameClient.bClient) {
                  if (this instanceof IsoZombie && ((IsoZombie)this).isRemoteZombie()) {
                     if (this.getCurrentState() != ClimbOverFenceState.instance() && this.getCurrentState() != ClimbThroughWindowState.instance() && this.getCurrentState() != ClimbOverWallState.instance() && this.getCurrentState() != StaggerBackState.instance() && this.getCurrentState() != ZombieHitReactionState.instance() && this.getCurrentState() != ZombieFallDownState.instance() && this.getCurrentState() != ZombieFallingState.instance() && this.getCurrentState() != ZombieOnGroundState.instance() && this.getCurrentState() != AttackNetworkState.instance()) {
                        return;
                     }
                  } else if (this instanceof IsoAnimal && !((IsoAnimal)this).isLocalPlayer()) {
                     if (!this.isCurrentState(AnimalIdleState.instance()) && !((IsoAnimal)this).isHappy()) {
                        return;
                     }
                  } else if (this instanceof IsoPlayer && !((IsoPlayer)this).isLocalPlayer() && !this.isCurrentState(CollideWithWallState.instance()) && !this.isCurrentState(PlayerGetUpState.instance()) && !this.isCurrentState(BumpedState.instance())) {
                     return;
                  }
               }

               if (Core.bDebug && this instanceof IsoPlayer && DebugOptions.instance.Cheat.Player.FastMovement.getValue()) {
                  var1.scale(4.0F);
               }

               if (this.isGrappling() || this.isBeingGrappled()) {
                  Vector3 var2 = new Vector3();
                  this.getGrappleOffset(var2);
                  var1.x += var2.x;
                  var1.y += var2.y;
               }

               if (GameClient.bClient && this instanceof IsoZombie && this.isCurrentState(StaggerBackState.instance())) {
                  float var3 = var1.getLength();
                  var1.set(this.getHitDir());
                  var1.setLength(var3);
               }

               if (this.isDeferredMovementEnabled()) {
                  this.MoveUnmodded(var1);
               }

            }
         }
      }
   }

   protected void doDeferredMovementFromRagdoll() {
      if (this.isRagdoll) {
         if (this.hasAnimationPlayer()) {
            if (this.isAnimationUpdatingThisFrame()) {
               Vector2 var1 = tempo;
               this.getDeferredMovementFromRagdoll(var1);
               this.MoveUnmodded(var1);
               this.setX(this.getNextX());
               this.setY(this.getNextY());
            }
         }
      }
   }

   public ActionContext getActionContext() {
      return this.actionContext;
   }

   public String getPreviousActionContextStateName() {
      ActionContext var1 = this.getActionContext();
      return var1 == null ? "" : var1.getPreviousStateName();
   }

   public String getCurrentActionContextStateName() {
      ActionContext var1 = this.getActionContext();
      return var1 != null && var1.getCurrentState() != null ? var1.getCurrentStateName() : "";
   }

   public boolean hasAnimationPlayer() {
      return this.m_animPlayer != null;
   }

   public AnimationPlayer getAnimationPlayer() {
      Model var1 = ModelManager.instance.getBodyModel(this);
      boolean var2 = false;
      if (this.m_animPlayer != null && this.m_animPlayer.getModel() != var1) {
         var2 = this.m_animPlayer.getMultiTrack().getTrackCount() > 0;
         this.m_animPlayer = (AnimationPlayer)Pool.tryRelease((IPooledObject)this.m_animPlayer);
      }

      if (this.m_animPlayer == null) {
         this.m_animPlayer = AnimationPlayer.alloc(var1);
         this.onAnimPlayerCreated(this.m_animPlayer);
         if (var2) {
            this.getAdvancedAnimator().OnAnimDataChanged(false);
         }
      }

      return this.m_animPlayer;
   }

   public void releaseAnimationPlayer() {
      this.m_animPlayer = (AnimationPlayer)Pool.tryRelease((IPooledObject)this.m_animPlayer);
   }

   protected void onAnimPlayerCreated(AnimationPlayer var1) {
      var1.setIsoGameCharacter(this);
      var1.setRecorder(this.m_animationRecorder);
      var1.setTwistBones("Bip01_Pelvis", "Bip01_Spine", "Bip01_Spine1", "Bip01_Neck", "Bip01_Head");
      var1.setCounterRotationBone("Bip01");
   }

   protected void updateAnimationRecorderState() {
      if (this.m_animPlayer != null) {
         if (IsoWorld.isAnimRecorderDiscardTriggered()) {
            this.m_animPlayer.discardRecording();
         }

         boolean var1 = IsoWorld.isAnimRecorderActive();
         if (var1) {
            this.m_animPlayerRecordingExclusive = false;
         }

         boolean var2 = this.m_animPlayerRecordingExclusive;
         boolean var3 = (var2 || var1) && !this.isSceneCulled();
         if (var3) {
            this.getAnimationPlayerRecorder().logCharacterPos();
         }

         this.m_animPlayer.setRecording(var3);
      }
   }

   public boolean isAnimRecorderActive() {
      return this.hasAnimationPlayer() && this.m_animPlayer.isRecording();
   }

   public void setAnimRecorderActive(boolean var1, boolean var2) {
      if (this.hasAnimationPlayer()) {
         this.m_animPlayer.setRecording(var1);
         this.m_animPlayerRecordingExclusive = var2 && var1;
      }
   }

   public AdvancedAnimator getAdvancedAnimator() {
      return this.advancedAnimator;
   }

   public ModelInstance getModelInstance() {
      if (this.legsSprite == null) {
         return null;
      } else {
         return this.legsSprite.modelSlot == null ? null : this.legsSprite.modelSlot.model;
      }
   }

   public String getCurrentStateName() {
      return this.stateMachine.getCurrent() == null ? null : this.stateMachine.getCurrent().getName();
   }

   public String getPreviousStateName() {
      return this.stateMachine.getPrevious() == null ? null : this.stateMachine.getPrevious().getName();
   }

   public String getAnimationDebug() {
      if (this.advancedAnimator != null) {
         String var10000 = this.instancename;
         return var10000 + "\n" + this.advancedAnimator.GetDebug();
      } else {
         return this.instancename + "\n - No Animator";
      }
   }

   public String getTalkerType() {
      return this.chatElement.getTalkerType();
   }

   public void spinToZeroAllAnimNodes() {
      AdvancedAnimator var1 = this.getAdvancedAnimator();
      AnimLayer var2 = var1.getRootLayer();
      List var3 = var2.getLiveAnimNodes();

      for(int var4 = 0; var4 < var3.size(); ++var4) {
         LiveAnimNode var5 = (LiveAnimNode)var3.get(var4);
         var5.stopTransitionIn();
         var5.setWeightsToZero();
      }

   }

   public boolean isAnimForecasted() {
      return System.currentTimeMillis() < this.isAnimForecasted;
   }

   public void setAnimForecasted(int var1) {
      this.isAnimForecasted = System.currentTimeMillis() + (long)var1;
   }

   public void resetModel() {
      ModelManager.instance.Reset(this);
   }

   public void resetModelNextFrame() {
      ModelManager.instance.ResetNextFrame(this);
   }

   protected void onTrigger_setClothingToXmlTriggerFile(TriggerXmlFile var1) {
      OutfitManager.Reload();
      String var2;
      if (!StringUtils.isNullOrWhitespace(var1.outfitName)) {
         var2 = var1.outfitName;
         DebugLog.Clothing.debugln("Desired outfit name: " + var2);
         Outfit var3;
         if (var1.isMale) {
            var3 = OutfitManager.instance.FindMaleOutfit(var2);
         } else {
            var3 = OutfitManager.instance.FindFemaleOutfit(var2);
         }

         if (var3 == null) {
            DebugLog.Clothing.error("Could not find outfit: " + var2);
            return;
         }

         if (this.bFemale == var1.isMale && this instanceof IHumanVisual) {
            ((IHumanVisual)this).getHumanVisual().clear();
         }

         this.bFemale = !var1.isMale;
         if (this.descriptor != null) {
            this.descriptor.setFemale(this.bFemale);
         }

         this.dressInNamedOutfit(var3.m_Name);
         this.advancedAnimator.OnAnimDataChanged(false);
         if (this instanceof IsoPlayer) {
            LuaEventManager.triggerEvent("OnClothingUpdated", this);
         }
      } else if (!StringUtils.isNullOrWhitespace(var1.clothingItemGUID)) {
         var2 = "game";
         String var7 = var2 + "-" + var1.clothingItemGUID;
         Boolean var4 = OutfitManager.instance.getClothingItem(var7) != null;
         if (!var4) {
            Iterator var5 = ZomboidFileSystem.instance.getModIDs().iterator();

            while(var5.hasNext()) {
               String var6 = (String)var5.next();
               var7 = var6 + "-" + var1.clothingItemGUID;
               if (OutfitManager.instance.getClothingItem(var7) != null) {
                  var4 = true;
                  break;
               }
            }
         }

         if (var4) {
            this.dressInClothingItem(var7);
            if (this instanceof IsoPlayer) {
               LuaEventManager.triggerEvent("OnClothingUpdated", this);
            }
         }
      }

      ModelManager.instance.Reset(this);
   }

   protected void onTrigger_setAnimStateToTriggerFile(AnimStateTriggerXmlFile var1) {
      String var2 = this.GetAnimSetName();
      if (!StringUtils.equalsIgnoreCase(var2, var1.animSet)) {
         this.setVariable("dbgForceAnim", false);
         this.restoreAnimatorStateToActionContext();
      } else {
         DebugOptions.instance.Animation.AnimLayer.AllowAnimNodeOverride.setValue(var1.forceAnim);
         if (this.advancedAnimator.containsState(var1.stateName)) {
            this.setVariable("dbgForceAnim", var1.forceAnim);
            this.setVariable("dbgForceAnimStateName", var1.stateName);
            this.setVariable("dbgForceAnimNodeName", var1.nodeName);
            this.setVariable("dbgForceAnimScalars", var1.setScalarValues);
            this.setVariable("dbgForceScalar", var1.scalarValue);
            this.setVariable("dbgForceScalar2", var1.scalarValue2);
            this.advancedAnimator.SetState(var1.stateName);
         } else {
            DebugLog.Animation.error("State not found: " + var1.stateName);
            this.restoreAnimatorStateToActionContext();
         }

      }
   }

   private void restoreAnimatorStateToActionContext() {
      if (this.actionContext.getCurrentState() != null) {
         this.advancedAnimator.SetState(this.actionContext.getCurrentStateName(), PZArrayUtil.listConvert(this.actionContext.getChildStates(), (var0) -> {
            return var0.getName();
         }));
      }

   }

   public void clothingItemChanged(String var1) {
      if (this.wornItems != null) {
         for(int var2 = 0; var2 < this.wornItems.size(); ++var2) {
            InventoryItem var3 = this.wornItems.getItemByIndex(var2);
            ClothingItem var4 = var3.getClothingItem();
            if (var4 != null && var4.isReady() && var4.m_GUID.equals(var1)) {
               ClothingItemReference var5 = new ClothingItemReference();
               var5.itemGUID = var1;
               var5.randomize();
               var3.getVisual().synchWithOutfit(var5);
               var3.synchWithVisual();
               this.resetModelNextFrame();
            }
         }
      }

   }

   public void reloadOutfit() {
      ModelManager.instance.Reset(this);
   }

   public void setDoRender(boolean var1) {
      this.doRender = var1;
   }

   public boolean getDoRender() {
      return this.doRender;
   }

   public boolean isSceneCulled() {
      return this.m_isCulled;
   }

   public void setSceneCulled(boolean var1) {
      if (this.isSceneCulled() != var1) {
         try {
            if (var1) {
               ModelManager.instance.Remove(this);
            } else {
               ModelManager.instance.Add(this);
            }
         } catch (Exception var3) {
            System.err.println("Error in IsoGameCharacter.setSceneCulled(" + var1 + "):");
            ExceptionLogger.logException(var3);
            ModelManager.instance.Remove(this);
            this.legsSprite.modelSlot = null;
         }

      }
   }

   public void onCullStateChanged(ModelManager var1, boolean var2) {
      this.m_isCulled = var2;
      if (!var2) {
         this.restoreAnimatorStateToActionContext();
         DebugFileWatcher.instance.add(this.m_animStateTriggerWatcher);
         OutfitManager.instance.addClothingItemListener(this);
      } else {
         DebugFileWatcher.instance.remove(this.m_animStateTriggerWatcher);
         OutfitManager.instance.removeClothingItemListener(this);
      }

   }

   public void dressInRandomOutfit() {
      if (DebugLog.isEnabled(DebugType.Clothing)) {
         DebugLog.Clothing.println("IsoGameCharacter.dressInRandomOutfit>");
      }

      Outfit var1 = OutfitManager.instance.GetRandomOutfit(this.isFemale());
      if (var1 != null) {
         this.dressInNamedOutfit(var1.m_Name);
      }

   }

   public void dressInRandomNonSillyOutfit() {
      DebugLog.Clothing.println("IsoGameCharacter.dressInRandomOutfit>");
      Outfit var1 = OutfitManager.instance.GetRandomNonSillyOutfit(this.isFemale());
      if (var1 != null) {
         this.dressInNamedOutfit(var1.m_Name);
      }

   }

   public void dressInNamedOutfit(String var1) {
   }

   public void dressInPersistentOutfit(String var1) {
      if (this.isZombie()) {
         this.getDescriptor().setForename(SurvivorFactory.getRandomForename(this.isFemale()));
      }

      int var2 = PersistentOutfits.instance.pickOutfit(var1, this.isFemale());
      this.dressInPersistentOutfitID(var2);
   }

   public void dressInPersistentOutfitID(int var1) {
   }

   public String getOutfitName() {
      if (this instanceof IHumanVisual) {
         HumanVisual var1 = ((IHumanVisual)this).getHumanVisual();
         Outfit var2 = var1.getOutfit();
         return var2 == null ? null : var2.m_Name;
      } else {
         return null;
      }
   }

   public void dressInClothingItem(String var1) {
   }

   public Outfit getRandomDefaultOutfit() {
      IsoGridSquare var1 = this.getCurrentSquare();
      IsoRoom var2 = var1 == null ? null : var1.getRoom();
      String var3 = var2 == null ? null : var2.getName();
      return ZombiesZoneDefinition.getRandomDefaultOutfit(this.isFemale(), var3);
   }

   public ModelInstance getModel() {
      return this.legsSprite != null && this.legsSprite.modelSlot != null ? this.legsSprite.modelSlot.model : null;
   }

   public boolean hasActiveModel() {
      return this.legsSprite != null && this.legsSprite.hasActiveModel();
   }

   public boolean hasItems(String var1, int var2) {
      int var3 = this.inventory.getItemCount(var1);
      return var2 <= var3;
   }

   public int getLevelUpLevels(int var1) {
      return LevelUpLevels.length <= var1 ? LevelUpLevels[LevelUpLevels.length - 1] : LevelUpLevels[var1];
   }

   public int getLevelMaxForXp() {
      return LevelUpLevels.length;
   }

   public int getXpForLevel(int var1) {
      return var1 < LevelUpLevels.length ? (int)((float)LevelUpLevels[var1] * this.LevelUpMultiplier) : (int)((float)(LevelUpLevels[LevelUpLevels.length - 1] + (var1 - LevelUpLevels.length + 1) * 400) * this.LevelUpMultiplier);
   }

   public void DoDeath(HandWeapon var1, IsoGameCharacter var2) {
      this.DoDeath(var1, var2, true);
   }

   public void DoDeath(HandWeapon var1, IsoGameCharacter var2, boolean var3) {
      this.OnDeath();
      if (this.getAttackedBy() instanceof IsoPlayer && GameServer.bServer && this instanceof IsoPlayer) {
         String var4 = "";
         String var5 = "";
         if (SteamUtils.isSteamModeEnabled()) {
            var4 = " (" + ((IsoPlayer)this.getAttackedBy()).getSteamID() + ") ";
            var5 = " (" + ((IsoPlayer)this).getSteamID() + ") ";
         }

         PVPLogTool.logKill((IsoPlayer)this.getAttackedBy(), (IsoPlayer)this);
      } else {
         if (GameServer.bServer && this instanceof IsoPlayer) {
            ZLogger var10000 = LoggerManager.getLogger("user");
            String var10001 = ((IsoPlayer)this).username;
            var10000.write("user " + var10001 + " died at " + LoggerManager.getPlayerCoords((IsoPlayer)this) + " (non pvp)");
         }

         if (ServerOptions.instance.AnnounceDeath.getValue() && this instanceof IsoPlayer && GameServer.bServer) {
            ChatServer.getInstance().sendMessageToServerChat(((IsoPlayer)this).username + " is dead.");
         }
      }

      this.doDeathSplatterAndSounds(var1, var2, var3);
   }

   private void doDeathSplatterAndSounds(HandWeapon var1, IsoGameCharacter var2, boolean var3) {
      if (this.onDeath_ShouldDoSplatterAndSounds(var1, var2, var3)) {
         if (this.isDoDeathSound()) {
            this.playDeadSound();
         }

         this.setDoDeathSound(false);
         if (this.isDead()) {
            float var4 = 0.5F;
            if (this.isZombie() && (((IsoZombie)this).bCrawling || this.getCurrentState() == ZombieOnGroundState.instance())) {
               var4 = 0.2F;
            }

            if (GameServer.bServer && var3) {
               boolean var5 = this.isOnFloor() && var2 instanceof IsoPlayer && var1 != null && "BareHands".equals(var1.getType());
               GameServer.sendBloodSplatter(var1, this.getX(), this.getY(), this.getZ() + var4, this.getHitDir(), this.isCloseKilled(), var5);
            }

            int var6;
            int var9;
            if (var1 != null && SandboxOptions.instance.BloodLevel.getValue() > 1 && var3) {
               var9 = var1.getSplatNumber();
               if (var9 < 1) {
                  var9 = 1;
               }

               if (Core.bLastStand) {
                  var9 *= 3;
               }

               switch (SandboxOptions.instance.BloodLevel.getValue()) {
                  case 2:
                     var9 /= 2;
                  case 3:
                  default:
                     break;
                  case 4:
                     var9 *= 2;
                     break;
                  case 5:
                     var9 *= 5;
               }

               for(var6 = 0; var6 < var9; ++var6) {
                  this.splatBlood(3, 0.3F);
               }
            }

            if (var1 != null && SandboxOptions.instance.BloodLevel.getValue() > 1 && var3) {
               this.splatBloodFloorBig();
            }

            if (var2 != null && var2.xp != null) {
               var2.xp.AddXP(var1, 3);
            }

            if (SandboxOptions.instance.BloodLevel.getValue() > 1 && this.isOnFloor() && var2 instanceof IsoPlayer && var1 == ((IsoPlayer)var2).bareHands && var3) {
               this.playBloodSplatterSound();

               for(var9 = -1; var9 <= 1; ++var9) {
                  for(var6 = -1; var6 <= 1; ++var6) {
                     if (var9 != 0 || var6 != 0) {
                        new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + var4, (float)var9 * Rand.Next(0.25F, 0.5F), (float)var6 * Rand.Next(0.25F, 0.5F));
                     }
                  }
               }

               new IsoZombieGiblets(IsoZombieGiblets.GibletType.Eye, this.getCell(), this.getX(), this.getY(), this.getZ() + var4, this.getHitDir().x * 0.8F, this.getHitDir().y * 0.8F);
            } else if (SandboxOptions.instance.BloodLevel.getValue() > 1 && var3) {
               this.playBloodSplatterSound();
               new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + var4, this.getHitDir().x * 1.5F, this.getHitDir().y * 1.5F);
               tempo.x = this.getHitDir().x;
               tempo.y = this.getHitDir().y;
               byte var11 = 3;
               byte var10 = 0;
               byte var7 = 1;
               switch (SandboxOptions.instance.BloodLevel.getValue()) {
                  case 1:
                     var7 = 0;
                     break;
                  case 2:
                     var7 = 1;
                     var11 = 5;
                     var10 = 2;
                  case 3:
                  default:
                     break;
                  case 4:
                     var7 = 3;
                     var11 = 2;
                     break;
                  case 5:
                     var7 = 10;
                     var11 = 0;
               }

               for(int var8 = 0; var8 < var7; ++var8) {
                  if (Rand.Next(this.isCloseKilled() ? 8 : var11) == 0) {
                     new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + var4, this.getHitDir().x * 1.5F, this.getHitDir().y * 1.5F);
                  }

                  if (Rand.Next(this.isCloseKilled() ? 8 : var11) == 0) {
                     new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + var4, this.getHitDir().x * 1.5F, this.getHitDir().y * 1.5F);
                  }

                  if (Rand.Next(this.isCloseKilled() ? 8 : var11) == 0) {
                     new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + var4, this.getHitDir().x * 1.8F, this.getHitDir().y * 1.8F);
                  }

                  if (Rand.Next(this.isCloseKilled() ? 8 : var11) == 0) {
                     new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + var4, this.getHitDir().x * 1.9F, this.getHitDir().y * 1.9F);
                  }

                  if (Rand.Next(this.isCloseKilled() ? 4 : var10) == 0) {
                     new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + var4, this.getHitDir().x * 3.5F, this.getHitDir().y * 3.5F);
                  }

                  if (Rand.Next(this.isCloseKilled() ? 4 : var10) == 0) {
                     new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + var4, this.getHitDir().x * 3.8F, this.getHitDir().y * 3.8F);
                  }

                  if (Rand.Next(this.isCloseKilled() ? 4 : var10) == 0) {
                     new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + var4, this.getHitDir().x * 3.9F, this.getHitDir().y * 3.9F);
                  }

                  if (Rand.Next(this.isCloseKilled() ? 4 : var10) == 0) {
                     new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + var4, this.getHitDir().x * 1.5F, this.getHitDir().y * 1.5F);
                  }

                  if (Rand.Next(this.isCloseKilled() ? 4 : var10) == 0) {
                     new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + var4, this.getHitDir().x * 3.8F, this.getHitDir().y * 3.8F);
                  }

                  if (Rand.Next(this.isCloseKilled() ? 4 : var10) == 0) {
                     new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, this.getCell(), this.getX(), this.getY(), this.getZ() + var4, this.getHitDir().x * 3.9F, this.getHitDir().y * 3.9F);
                  }

                  if (Rand.Next(this.isCloseKilled() ? 9 : 6) == 0) {
                     new IsoZombieGiblets(IsoZombieGiblets.GibletType.Eye, this.getCell(), this.getX(), this.getY(), this.getZ() + var4, this.getHitDir().x * 0.8F, this.getHitDir().y * 0.8F);
                  }
               }
            }

         }
      }
   }

   public boolean onDeath_ShouldDoSplatterAndSounds(HandWeapon var1, IsoGameCharacter var2, boolean var3) {
      return true;
   }

   private boolean TestIfSeen(int var1) {
      IsoPlayer var2 = IsoPlayer.players[var1];
      if (var2 != null && this != var2 && !GameServer.bServer) {
         float var3 = this.DistToProper(var2);
         if (var3 > GameTime.getInstance().getViewDist()) {
            return false;
         } else {
            boolean var4 = this.current.isCanSee(var1);
            if (!var4 && this.current.isCouldSee(var1)) {
               var4 = var3 < var2.getSeeNearbyCharacterDistance();
            }

            if (!var4) {
               return false;
            } else {
               ColorInfo var5 = this.getCurrentSquare().lighting[var1].lightInfo();
               float var6 = (var5.r + var5.g + var5.b) / 3.0F;
               if (var6 > 0.6F) {
                  var6 = 1.0F;
               }

               float var7 = 1.0F - var3 / GameTime.getInstance().getViewDist();
               if (var6 == 1.0F && var7 > 0.3F) {
                  var7 = 1.0F;
               }

               float var8 = var2.getDotWithForwardDirection(this.getX(), this.getY());
               if (var8 < 0.5F) {
                  var8 = 0.5F;
               }

               var6 *= var8;
               if (var6 < 0.0F) {
                  var6 = 0.0F;
               }

               if (var3 <= 1.0F) {
                  var7 = 1.0F;
                  var6 *= 2.0F;
               }

               var6 *= var7;
               var6 *= 100.0F;
               return var6 > 0.025F;
            }
         }
      } else {
         return false;
      }
   }

   private void DoLand() {
      if (!this.isClimbing() && !this.isRagdollFall()) {
         byte var1 = 50;
         byte var2 = 80;
         byte var3 = 80;
         boolean var4 = false;
         boolean var5 = false;
         boolean var6 = this.isAlive();
         IsoPlayer var7;
         if (this instanceof IsoPlayer) {
            var7 = (IsoPlayer)this;
            if (GameServer.bServer) {
               return;
            }

            if (GameClient.bClient && ((IsoPlayer)this).bRemote) {
               return;
            }

            if (this.fallTime > 20.0F) {
               var7.stopPlayerVoiceSound("DeathFall");
            }

            if (this.fallTime >= (float)var2) {
               this.setVariable("bHardFall2", true);
               var4 = true;
               var5 = true;
            } else if (this.fallTime >= (float)var1) {
               this.setVariable("bHardFall", true);
               var4 = true;
            } else {
               this.setVariable("bLandLight", true);
               if (!(this.fallTime < 20.0F)) {
                  this.setVariable("bLandLightMask", true);
               }
            }
         }

         if (!(this.fallTime < 20.0F) && !this.isClimbing()) {
            if (this instanceof IsoZombie) {
               if (this.fallTime > (float)var1) {
                  this.hitDir.x = this.hitDir.y = 0.0F;
                  this.playHurtSound();
                  float var16 = (float)Rand.Next(150) / 1000.0F;
                  var16 *= this.fallTime / (float)var1;
                  if (((IsoZombie)this).bCrawling || !(this.fallTime >= (float)var2) && Rand.Next(100) >= 80) {
                     this.playSound("LandLight");
                  } else {
                     this.setVariable("bHardFall", true);
                     var4 = true;
                     if ((float)Rand.Next(1000) < var16 * 1000.0F) {
                        ((IsoZombie)this).setCrawler(true);
                        ((IsoZombie)this).setCanWalk(false);
                        ((IsoZombie)this).setCrawlerType(1);
                        this.playSound("FirstAidFracture");
                        this.splatBloodFloorBig();
                     }

                     this.splatBloodFloorBig();
                     this.playSound("LandHeavy");
                  }

                  var16 = (float)((double)var16 * SandboxOptions.instance.Lore.ZombiesFallDamage.getValue());
                  CombatManager.getInstance().applyDamage(this, var16);
                  this.setAttackedBy((IsoGameCharacter)null);
                  this.helmetFall(var4);
               }

            } else {
               var7 = (IsoPlayer)Type.tryCastTo(this, IsoPlayer.class);
               if (var7 != null && this.fallTime >= (float)var1) {
                  var7.getBodyDamage().setBoredomLevel(0.0F);
               }

               boolean var8 = Rand.NextBool(80 - this.getPerkLevel(PerkFactory.Perks.Nimble));
               float var9 = this.fallTime;
               float var10 = (float)((double)(Rand.Next(11) + 5) * 0.1);
               var9 *= var10;
               float var11 = this.getInventory().getCapacityWeight() / this.getInventory().getMaxWeight();
               var11 = Math.min(1.8F, var11);
               var9 *= var11;
               if (this.getCurrentSquare().getFloor() != null && this.getCurrentSquare().getFloor().getSprite().getName() != null && this.getCurrentSquare().getFloor().getSprite().getName().startsWith("blends_natural")) {
                  var9 *= 0.8F;
                  if (!var8) {
                     var8 = Rand.NextBool(65 - this.getPerkLevel(PerkFactory.Perks.Nimble));
                  }
               }

               if (!this.Traits.Obese.isSet() && !this.Traits.Emaciated.isSet()) {
                  if (this.Traits.Overweight.isSet() || this.Traits.VeryUnderweight.isSet()) {
                     var9 *= 1.2F;
                  }
               } else {
                  var9 *= 1.4F;
               }

               var9 *= Math.max(0.1F, 1.0F - (float)this.getPerkLevel(PerkFactory.Perks.Fitness) * 0.05F);
               var9 *= Math.max(0.1F, 1.0F - (float)this.getPerkLevel(PerkFactory.Perks.Nimble) * 0.05F);
               boolean var12 = false;
               if (this.fallTime >= (float)var3) {
                  if (!var8) {
                     var12 = true;
                     var9 = 1000.0F;
                  } else {
                     var9 = 0.0F;
                     var8 = false;
                     var4 = true;
                     var5 = true;
                  }
               }

               if (var4) {
                  this.fallenOnKnees(true);
                  this.dropHandItems();
               } else {
                  this.helmetFall(false);
               }

               if (!var8) {
                  this.getBodyDamage().ReduceGeneralHealth(var9);
                  if (var9 > 0.0F) {
                     this.getBodyDamage().Update();
                     this.setKilledByFall(var6 && this.isDead());
                  }

                  LuaEventManager.triggerEvent("OnPlayerGetDamage", this, "FALLDOWN", var9);
                  if (var7 != null && var9 > 5.0F && this.isAlive()) {
                     var7.playerVoiceSound(var4 ? "PainFromFallHigh" : " PainFromFallLow");
                  }

                  boolean var13 = var4 || (float)Rand.Next(100) < var9;
                  if (var13) {
                     int var14 = (int)((double)this.fallTime * 0.6);
                     if (this.getInventory().getMaxWeight() - this.getInventory().getCapacityWeight() < 2.0F) {
                        var14 = (int)((float)var14 + this.getInventory().getCapacityWeight() / this.getInventory().getMaxWeight() * 20.0F);
                     }

                     if (!this.Traits.Obese.isSet() && !this.Traits.Emaciated.isSet()) {
                        if (this.Traits.Overweight.isSet() || this.Traits.VeryUnderweight.isSet()) {
                           var14 += 10;
                        }
                     } else {
                        var14 += 20;
                     }

                     if (this.getPerkLevel(PerkFactory.Perks.Fitness) > 4) {
                        var14 = (int)((double)var14 - (double)(this.getPerkLevel(PerkFactory.Perks.Fitness) - 4) * 1.5);
                     }

                     var14 = (int)((double)var14 - (double)this.getPerkLevel(PerkFactory.Perks.Nimble) * 1.5);
                     if (!SandboxOptions.instance.BoneFracture.getValue()) {
                     }

                     BodyPartType var15;
                     if (!var5 && (!var4 || !Rand.NextBool(2))) {
                        var15 = BodyPartType.FromIndex(Rand.Next(BodyPartType.ToIndex(BodyPartType.UpperLeg_L), BodyPartType.ToIndex(BodyPartType.Foot_R) + 1));
                     } else {
                        var15 = BodyPartType.FromIndex(Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.Foot_R) + 1));
                     }

                     if (Rand.Next(100) < var14 && SandboxOptions.instance.BoneFracture.getValue()) {
                        this.getBodyDamage().getBodyPart(var15).generateFractureNew((float)Rand.Next(50, 80));
                     } else if (Rand.Next(100) < var14 + 10) {
                        this.getBodyDamage().getBodyPart(var15).generateDeepWound();
                     } else {
                        this.getBodyDamage().getBodyPart(var15).setStiffness(100.0F);
                     }

                     if (var5) {
                        var15 = BodyPartType.FromIndex(Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.Foot_R) + 1));
                        if (Rand.Next(100) < var14 && SandboxOptions.instance.BoneFracture.getValue()) {
                           this.getBodyDamage().getBodyPart(var15).generateFractureNew((float)Rand.Next(50, 80));
                        } else if (Rand.Next(100) < var14 + 10) {
                           this.getBodyDamage().getBodyPart(var15).generateDeepWound();
                        } else {
                           this.getBodyDamage().getBodyPart(var15).setStiffness(100.0F);
                        }
                     }

                     if (var12) {
                        this.splatBloodFloorBig();
                        this.splatBloodFloorBig();
                        this.splatBloodFloorBig();
                        this.splatBloodFloorBig();
                     }
                  }

               }
            }
         }
      }
   }

   public IsoGameCharacter getFollowingTarget() {
      return this.FollowingTarget;
   }

   public void setFollowingTarget(IsoGameCharacter var1) {
      this.FollowingTarget = var1;
   }

   public ArrayList<IsoMovingObject> getLocalList() {
      return this.LocalList;
   }

   public ArrayList<IsoMovingObject> getLocalNeutralList() {
      return this.LocalNeutralList;
   }

   public ArrayList<IsoMovingObject> getLocalGroupList() {
      return this.LocalGroupList;
   }

   public ArrayList<IsoMovingObject> getLocalRelevantEnemyList() {
      return this.LocalRelevantEnemyList;
   }

   public float getDangerLevels() {
      return this.dangerLevels;
   }

   public void setDangerLevels(float var1) {
      this.dangerLevels = var1;
   }

   public ArrayList<PerkInfo> getPerkList() {
      return this.PerkList;
   }

   public float getLeaveBodyTimedown() {
      return this.leaveBodyTimedown;
   }

   public void setLeaveBodyTimedown(float var1) {
      this.leaveBodyTimedown = var1;
   }

   public boolean isAllowConversation() {
      return this.AllowConversation;
   }

   public void setAllowConversation(boolean var1) {
      this.AllowConversation = var1;
   }

   public float getReanimateTimer() {
      return this.ReanimateTimer;
   }

   public void setReanimateTimer(float var1) {
      this.ReanimateTimer = var1;
   }

   public int getReanimAnimFrame() {
      return this.ReanimAnimFrame;
   }

   public void setReanimAnimFrame(int var1) {
      this.ReanimAnimFrame = var1;
   }

   public int getReanimAnimDelay() {
      return this.ReanimAnimDelay;
   }

   public void setReanimAnimDelay(int var1) {
      this.ReanimAnimDelay = var1;
   }

   public boolean isReanim() {
      return this.Reanim;
   }

   public void setReanim(boolean var1) {
      this.Reanim = var1;
   }

   public boolean isVisibleToNPCs() {
      return this.VisibleToNPCs;
   }

   public void setVisibleToNPCs(boolean var1) {
      this.VisibleToNPCs = var1;
   }

   public int getDieCount() {
      return this.DieCount;
   }

   public void setDieCount(int var1) {
      this.DieCount = var1;
   }

   public float getLlx() {
      return this.llx;
   }

   public void setLlx(float var1) {
      this.llx = var1;
   }

   public float getLly() {
      return this.lly;
   }

   public void setLly(float var1) {
      this.lly = var1;
   }

   public float getLlz() {
      return this.llz;
   }

   public void setLlz(float var1) {
      this.llz = var1;
   }

   public int getRemoteID() {
      return this.RemoteID;
   }

   public void setRemoteID(int var1) {
      this.RemoteID = var1;
   }

   public int getNumSurvivorsInVicinity() {
      return this.NumSurvivorsInVicinity;
   }

   public void setNumSurvivorsInVicinity(int var1) {
      this.NumSurvivorsInVicinity = var1;
   }

   public float getLevelUpMultiplier() {
      return this.LevelUpMultiplier;
   }

   public void setLevelUpMultiplier(float var1) {
      this.LevelUpMultiplier = var1;
   }

   public XP getXp() {
      return this.xp;
   }

   /** @deprecated */
   @Deprecated
   public void setXp(XP var1) {
      this.xp = var1;
   }

   public int getLastLocalEnemies() {
      return this.LastLocalEnemies;
   }

   public void setLastLocalEnemies(int var1) {
      this.LastLocalEnemies = var1;
   }

   public ArrayList<IsoMovingObject> getVeryCloseEnemyList() {
      return this.VeryCloseEnemyList;
   }

   public HashMap<String, Location> getLastKnownLocation() {
      return this.LastKnownLocation;
   }

   public IsoGameCharacter getAttackedBy() {
      return this.AttackedBy;
   }

   public void setAttackedBy(IsoGameCharacter var1) {
      this.AttackedBy = var1;
   }

   public boolean isIgnoreStaggerBack() {
      return this.IgnoreStaggerBack;
   }

   public void setIgnoreStaggerBack(boolean var1) {
      this.IgnoreStaggerBack = var1;
   }

   public boolean isAttackWasSuperAttack() {
      return this.AttackWasSuperAttack;
   }

   public void setAttackWasSuperAttack(boolean var1) {
      this.AttackWasSuperAttack = var1;
   }

   public int getTimeThumping() {
      return this.TimeThumping;
   }

   public void setTimeThumping(int var1) {
      this.TimeThumping = var1;
   }

   public int getPatienceMax() {
      return this.PatienceMax;
   }

   public void setPatienceMax(int var1) {
      this.PatienceMax = var1;
   }

   public int getPatienceMin() {
      return this.PatienceMin;
   }

   public void setPatienceMin(int var1) {
      this.PatienceMin = var1;
   }

   public int getPatience() {
      return this.Patience;
   }

   public void setPatience(int var1) {
      this.Patience = var1;
   }

   public Stack<BaseAction> getCharacterActions() {
      return this.CharacterActions;
   }

   public boolean hasTimedActions() {
      return !this.CharacterActions.isEmpty() || this.getVariableBoolean("IsPerformingAnAction");
   }

   public Vector2 getForwardDirection() {
      return this.m_forwardDirection;
   }

   public Vector2 getForwardDirection(Vector2 var1) {
      var1.x = this.m_forwardDirection.x;
      var1.y = this.m_forwardDirection.y;
      return var1;
   }

   public void setForwardDirection(Vector2 var1) {
      if (var1 != null) {
         this.setForwardDirection(var1.x, var1.y);
      }
   }

   public void setTargetAndCurrentDirection(Vector2 var1) {
      this.setForwardDirection(var1);
      if (this.hasAnimationPlayer()) {
         this.getAnimationPlayer().setTargetAndCurrentDirection(var1);
      }

   }

   public void setForwardDirection(float var1, float var2) {
      this.m_forwardDirection.x = var1;
      this.m_forwardDirection.y = var2;
   }

   public void zeroForwardDirectionX() {
      this.setForwardDirection(0.0F, 1.0F);
   }

   public void zeroForwardDirectionY() {
      this.setForwardDirection(1.0F, 0.0F);
   }

   public float getDirectionAngle() {
      return 57.295776F * this.getForwardDirection().getDirection();
   }

   public void setDirectionAngle(float var1) {
      float var2 = 0.017453292F * var1;
      Vector2 var3 = this.getForwardDirection();
      var3.setDirection(var2);
   }

   public float getAnimAngle() {
      return this.m_animPlayer != null && this.m_animPlayer.isReady() && !this.m_animPlayer.isBoneTransformsNeedFirstFrame() ? 57.295776F * this.m_animPlayer.getAngle() : this.getDirectionAngle();
   }

   public float getAnimAngleRadians() {
      return this.m_animPlayer != null && this.m_animPlayer.isReady() && !this.m_animPlayer.isBoneTransformsNeedFirstFrame() ? this.m_animPlayer.getAngle() : this.m_forwardDirection.getDirection();
   }

   /** @deprecated */
   @Deprecated
   public Vector2 getAnimVector(Vector2 var1) {
      return this.getAnimForwardDirection(var1);
   }

   public Vector2 getAnimForwardDirection(Vector2 var1) {
      return var1.setLengthAndDirection(this.getAnimAngleRadians(), 1.0F);
   }

   public float getLookAngleRadians() {
      return this.m_animPlayer != null && this.m_animPlayer.isReady() ? this.m_animPlayer.getAngle() + this.m_animPlayer.getTwistAngle() : this.getForwardDirection().getDirection();
   }

   public Vector2 getLookVector(Vector2 var1) {
      return var1.setLengthAndDirection(this.getLookAngleRadians(), 1.0F);
   }

   public boolean isAnimatingBackwards() {
      return this.m_isAnimatingBackwards;
   }

   public void setAnimatingBackwards(boolean var1) {
      this.m_isAnimatingBackwards = var1;
   }

   public boolean isDraggingCorpse() {
      if (!this.isGrappling()) {
         return false;
      } else {
         IGrappleable var1 = this.getGrapplingTarget();
         IsoZombie var2 = (IsoZombie)Type.tryCastTo(var1, IsoZombie.class);
         return var2 == null ? false : var2.isReanimatedForGrappleOnly();
      }
   }

   public UdpConnection getOwner() {
      return null;
   }

   public IsoPlayer getOwnerPlayer() {
      return null;
   }

   public float getDotWithForwardDirection(Vector3 var1) {
      return this.getDotWithForwardDirection(var1.x, var1.y);
   }

   public float getDotWithForwardDirection(float var1, float var2) {
      Vector2 var3 = IsoGameCharacter.L_getDotWithForwardDirection.v1.set(var1 - this.getX(), var2 - this.getY());
      var3.normalize();
      Vector2 var4 = this.getLookVector(IsoGameCharacter.L_getDotWithForwardDirection.v2);
      var4.normalize();
      return var3.dot(var4);
   }

   public boolean isAsleep() {
      return this.Asleep;
   }

   public void setAsleep(boolean var1) {
      this.Asleep = var1;
   }

   public boolean isResting() {
      return this.isResting;
   }

   public void setIsResting(boolean var1) {
      this.isResting = var1;
   }

   public int getZombieKills() {
      return this.ZombieKills;
   }

   public void setZombieKills(int var1) {
      this.ZombieKills = var1;
      if (GameServer.bServer && this instanceof IsoPlayer) {
         SteamGameServer.UpdatePlayer((IsoPlayer)this);
      }

   }

   public int getLastZombieKills() {
      return this.LastZombieKills;
   }

   public void setLastZombieKills(int var1) {
      this.LastZombieKills = var1;
   }

   public boolean isSuperAttack() {
      return this.superAttack;
   }

   public void setSuperAttack(boolean var1) {
      this.superAttack = var1;
   }

   public float getForceWakeUpTime() {
      return this.ForceWakeUpTime;
   }

   public void setForceWakeUpTime(float var1) {
      this.ForceWakeUpTime = var1;
   }

   public void forceAwake() {
      if (this.isAsleep()) {
         this.ForceWakeUp = true;
      }

   }

   public BodyDamage getBodyDamage() {
      return this.BodyDamage;
   }

   public BodyDamage getBodyDamageRemote() {
      if (this.BodyDamageRemote == null) {
         this.BodyDamageRemote = new BodyDamage(this);
      }

      return this.BodyDamageRemote;
   }

   public void resetBodyDamageRemote() {
      this.BodyDamageRemote = null;
   }

   public State getDefaultState() {
      return this.defaultState;
   }

   public void setDefaultState(State var1) {
      this.defaultState = var1;
   }

   public SurvivorDesc getDescriptor() {
      return this.descriptor;
   }

   public void setDescriptor(SurvivorDesc var1) {
      this.descriptor = var1;
   }

   public String getFullName() {
      return this.descriptor != null ? this.descriptor.forename + " " + this.descriptor.surname : "Bob Smith";
   }

   public BaseVisual getVisual() {
      throw new RuntimeException("subclasses must implement this");
   }

   public ItemVisuals getItemVisuals() {
      throw new RuntimeException("subclasses must implement this");
   }

   public void getItemVisuals(ItemVisuals var1) {
      this.getWornItems().getItemVisuals(var1);
   }

   public boolean isUsingWornItems() {
      return this.wornItems != null;
   }

   public Stack<IsoBuilding> getFamiliarBuildings() {
      return this.FamiliarBuildings;
   }

   public AStarPathFinderResult getFinder() {
      return this.finder;
   }

   public float getFireKillRate() {
      return this.FireKillRate;
   }

   public void setFireKillRate(float var1) {
      this.FireKillRate = var1;
   }

   public int getFireSpreadProbability() {
      return this.FireSpreadProbability;
   }

   public void setFireSpreadProbability(int var1) {
      this.FireSpreadProbability = var1;
   }

   public float getHealth() {
      return this.Health;
   }

   public void setHealth(float var1) {
      if (var1 != 0.0F || !this.isInvulnerable()) {
         this.Health = var1;
      }
   }

   public boolean isOnDeathDone() {
      return this.bDead;
   }

   public void setOnDeathDone(boolean var1) {
      this.bDead = var1;
   }

   public boolean isOnKillDone() {
      return this.bKill;
   }

   public void setOnKillDone(boolean var1) {
      this.bKill = var1;
   }

   public boolean isDeathDragDown() {
      return this.bDeathDragDown;
   }

   public void setDeathDragDown(boolean var1) {
      this.bDeathDragDown = var1;
   }

   public boolean isPlayingDeathSound() {
      return this.bPlayingDeathSound;
   }

   public void setPlayingDeathSound(boolean var1) {
      this.bPlayingDeathSound = var1;
   }

   public String getHurtSound() {
      return this.hurtSound;
   }

   public void setHurtSound(String var1) {
      this.hurtSound = var1;
   }

   /** @deprecated */
   @Deprecated
   public boolean isIgnoreMovementForDirection() {
      return false;
   }

   public ItemContainer getInventory() {
      return this.inventory;
   }

   public void setInventory(ItemContainer var1) {
      var1.parent = this;
      this.inventory = var1;
      this.inventory.setExplored(true);
   }

   public boolean isPrimaryEquipped(String var1) {
      if (this.leftHandItem == null) {
         return false;
      } else {
         return this.leftHandItem.getFullType().equals(var1) || this.leftHandItem.getType().equals(var1);
      }
   }

   public InventoryItem getPrimaryHandItem() {
      return this.leftHandItem;
   }

   public void setPrimaryHandItem(InventoryItem var1) {
      if (this.leftHandItem != var1) {
         if (var1 == null && this.getPrimaryHandItem() instanceof AnimalInventoryItem) {
            ((AnimalInventoryItem)this.getPrimaryHandItem()).getAnimal().heldBy = null;
         }

         if (this instanceof IsoPlayer && var1 == null && !((IsoPlayer)this).getAttachedAnimals().isEmpty() && this.getPrimaryHandItem() != null && this.getPrimaryHandItem().getType().equalsIgnoreCase("Rope")) {
            ((IsoPlayer)this).removeAllAttachedAnimals();
         }

         if (var1 == this.getSecondaryHandItem()) {
            this.setEquipParent(this.leftHandItem, var1, false);
         } else {
            this.setEquipParent(this.leftHandItem, var1);
         }

         this.leftHandItem = var1;
         this.handItemShouldSendToClients = true;
         LuaEventManager.triggerEvent("OnEquipPrimary", this, var1);
         this.resetEquippedHandsModels();
         this.setVariable("Weapon", WeaponType.getWeaponType(this).type);
         if (var1 instanceof AnimalInventoryItem && this instanceof IsoPlayer) {
            ((AnimalInventoryItem)var1).getAnimal().heldBy = (IsoPlayer)this;
         }

         if (var1 instanceof HandWeapon) {
            this.setUseHandWeapon((HandWeapon)var1);
            BallisticsController var2 = this.getBallisticsController();
            if (var2 != null) {
               var2.clearCacheTargets();
            }
         }

      }
   }

   public HandWeapon getAttackingWeapon() {
      return this.getUseHandWeapon();
   }

   protected void setEquipParent(InventoryItem var1, InventoryItem var2) {
      this.setEquipParent(var1, var2, true);
   }

   protected void setEquipParent(InventoryItem var1, InventoryItem var2, boolean var3) {
      if (var1 != null) {
         var1.setEquipParent((IsoGameCharacter)null, var3);
      }

      if (var2 != null) {
         var2.setEquipParent(this, var3);
      }

   }

   public void initWornItems(String var1) {
      BodyLocationGroup var2 = BodyLocations.getGroup(var1);
      this.wornItems = new WornItems(var2);
   }

   public WornItems getWornItems() {
      return this.wornItems;
   }

   public void setWornItems(WornItems var1) {
      this.wornItems = new WornItems(var1);
   }

   public InventoryItem getWornItem(String var1) {
      return this.wornItems.getItem(var1);
   }

   public void setWornItem(String var1, InventoryItem var2) {
      this.setWornItem(var1, var2, true);
   }

   public void setWornItem(String var1, InventoryItem var2, boolean var3) {
      InventoryItem var4 = this.wornItems.getItem(var1);
      if (var2 != var4) {
         IsoCell var5 = IsoWorld.instance.CurrentCell;
         if (var4 != null && var5 != null) {
            var5.addToProcessItemsRemove(var4);
         }

         this.wornItems.setItem(var1, var2);
         if (var2 != null && var5 != null) {
            if (var2.getContainer() != null) {
               var2.getContainer().parent = this;
            }

            var5.addToProcessItems(var2);
         }

         if (var3 && var4 != null && this instanceof IsoPlayer && !this.getInventory().hasRoomFor(this, var4)) {
            IsoGridSquare var6 = this.getCurrentSquare();
            var6 = this.getSolidFloorAt(var6.x, var6.y, var6.z);
            if (var6 != null) {
               float var7 = Rand.Next(0.1F, 0.9F);
               float var8 = Rand.Next(0.1F, 0.9F);
               float var9 = var6.getApparentZ(var7, var8) - (float)var6.getZ();
               var6.AddWorldInventoryItem(var4, var7, var8, var9);
               this.getInventory().Remove(var4);
            }
         }

         if (this.isoPlayer != null && this.isoPlayer.getHumanVisual().getHairModel().contains("Mohawk") && (Objects.equals(var1, "Hat") || Objects.equals(var1, "FullHat"))) {
            this.isoPlayer.getHumanVisual().setHairModel("MohawkFlat");
            this.resetModel();
         }

         this.resetModelNextFrame();
         if (this.clothingWetness != null) {
            this.clothingWetness.changed = true;
         }

         if (this instanceof IsoPlayer) {
            if (GameServer.bServer) {
               INetworkPacket.sendToRelative(PacketTypes.PacketType.SyncClothing, this.getX(), this.getY(), this);
            } else if (GameClient.bClient && GameClient.connection.isReady()) {
               INetworkPacket.send(PacketTypes.PacketType.SyncClothing, this);
            }
         }

         this.onWornItemsChanged();
      }
   }

   public void removeWornItem(InventoryItem var1) {
      this.removeWornItem(var1, true);
   }

   public void removeWornItem(InventoryItem var1, boolean var2) {
      String var3 = this.wornItems.getLocation(var1);
      if (var3 != null) {
         this.setWornItem(var3, (InventoryItem)null, var2);
      }
   }

   public void clearWornItems() {
      if (this.wornItems != null) {
         this.wornItems.clear();
         if (this.clothingWetness != null) {
            this.clothingWetness.changed = true;
         }

         this.onWornItemsChanged();
      }
   }

   public BodyLocationGroup getBodyLocationGroup() {
      return this.wornItems == null ? null : this.wornItems.getBodyLocationGroup();
   }

   public void onWornItemsChanged() {
   }

   public void initAttachedItems(String var1) {
      AttachedLocationGroup var2 = AttachedLocations.getGroup(var1);
      this.attachedItems = new AttachedItems(var2);
   }

   public AttachedItems getAttachedItems() {
      return this.attachedItems;
   }

   public void setAttachedItems(AttachedItems var1) {
      this.attachedItems = new AttachedItems(var1);
   }

   public InventoryItem getAttachedItem(String var1) {
      return this.attachedItems.getItem(var1);
   }

   public void setAttachedItem(String var1, InventoryItem var2) {
      InventoryItem var3 = this.attachedItems.getItem(var1);
      IsoCell var4 = IsoWorld.instance.CurrentCell;
      if (var3 != null && var4 != null) {
         var4.addToProcessItemsRemove(var3);
      }

      this.attachedItems.setItem(var1, var2);
      if (var2 != null && var4 != null) {
         InventoryContainer var5 = (InventoryContainer)Type.tryCastTo(var2, InventoryContainer.class);
         if (var5 != null && var5.getInventory() != null) {
            var5.getInventory().parent = this;
         }

         var4.addToProcessItems(var2);
      }

      this.resetEquippedHandsModels();
      IsoPlayer var6 = (IsoPlayer)Type.tryCastTo(this, IsoPlayer.class);
      if (GameClient.bClient && var6 != null && var6.isLocalPlayer() && !"bowtie".equals(var1) && !"head_hat".equals(var1)) {
         GameClient.instance.sendAttachedItem(var6, var1, var2);
      }

      if (!GameServer.bServer && var6 != null && var6.isLocalPlayer()) {
         LuaEventManager.triggerEvent("OnClothingUpdated", this);
      }

   }

   public void removeAttachedItem(InventoryItem var1) {
      String var2 = this.attachedItems.getLocation(var1);
      if (var2 != null) {
         this.setAttachedItem(var2, (InventoryItem)null);
      }
   }

   public void clearAttachedItems() {
      if (this.attachedItems != null) {
         this.attachedItems.clear();
      }
   }

   public AttachedLocationGroup getAttachedLocationGroup() {
      return this.attachedItems == null ? null : this.attachedItems.getGroup();
   }

   public ClothingWetness getClothingWetness() {
      return this.clothingWetness;
   }

   public InventoryItem getClothingItem_Head() {
      return this.getWornItem("Hat");
   }

   public void setClothingItem_Head(InventoryItem var1) {
      this.setWornItem("Hat", var1);
   }

   public InventoryItem getClothingItem_Torso() {
      return this.getWornItem("Tshirt");
   }

   public void setClothingItem_Torso(InventoryItem var1) {
      this.setWornItem("Tshirt", var1);
   }

   public InventoryItem getClothingItem_Back() {
      return this.getWornItem("Back");
   }

   public void setClothingItem_Back(InventoryItem var1) {
      this.setWornItem("Back", var1);
   }

   public InventoryItem getClothingItem_Hands() {
      return this.getWornItem("Hands");
   }

   public void setClothingItem_Hands(InventoryItem var1) {
      this.setWornItem("Hands", var1);
   }

   public InventoryItem getClothingItem_Legs() {
      return this.getWornItem("Pants");
   }

   public void setClothingItem_Legs(InventoryItem var1) {
      this.setWornItem("Pants", var1);
   }

   public InventoryItem getClothingItem_Feet() {
      return this.getWornItem("Shoes");
   }

   public void setClothingItem_Feet(InventoryItem var1) {
      this.setWornItem("Shoes", var1);
   }

   public int getNextWander() {
      return this.NextWander;
   }

   public void setNextWander(int var1) {
      this.NextWander = var1;
   }

   public boolean isOnFire() {
      return this.OnFire;
   }

   public void setOnFire(boolean var1) {
      this.OnFire = var1;
      if (GameServer.bServer) {
         if (var1) {
            IsoFireManager.addCharacterOnFire(this);
         } else {
            IsoFireManager.deleteCharacterOnFire(this);
         }
      }

   }

   public void removeFromWorld() {
      if (GameServer.bServer) {
         IsoFireManager.deleteCharacterOnFire(this);
      }

      super.removeFromWorld();
      this.releaseRagdollController();
      this.releaseBallisticsController();
      this.releaseBallisticsTarget();
      if (this.m_animationRecorder != null) {
         this.m_animationRecorder.close();
      }

   }

   public int getPathIndex() {
      return this.pathIndex;
   }

   public void setPathIndex(int var1) {
      this.pathIndex = var1;
   }

   public int getPathTargetX() {
      return PZMath.fastfloor(this.getPathFindBehavior2().getTargetX());
   }

   public int getPathTargetY() {
      return PZMath.fastfloor(this.getPathFindBehavior2().getTargetY());
   }

   public int getPathTargetZ() {
      return PZMath.fastfloor(this.getPathFindBehavior2().getTargetZ());
   }

   public InventoryItem getSecondaryHandItem() {
      return this.rightHandItem;
   }

   public void setSecondaryHandItem(InventoryItem var1) {
      if (this.rightHandItem != var1) {
         if (var1 == this.getPrimaryHandItem()) {
            this.setEquipParent(this.rightHandItem, var1, false);
         } else {
            this.setEquipParent(this.rightHandItem, var1);
         }

         this.rightHandItem = var1;
         this.handItemShouldSendToClients = true;
         LuaEventManager.triggerEvent("OnEquipSecondary", this, var1);
         this.resetEquippedHandsModels();
         this.setVariable("Weapon", WeaponType.getWeaponType(this).type);
      }
   }

   public boolean isHandItem(InventoryItem var1) {
      return this.isPrimaryHandItem(var1) || this.isSecondaryHandItem(var1);
   }

   public boolean isPrimaryHandItem(InventoryItem var1) {
      return var1 != null && this.getPrimaryHandItem() == var1;
   }

   public boolean isSecondaryHandItem(InventoryItem var1) {
      return var1 != null && this.getSecondaryHandItem() == var1;
   }

   public boolean isItemInBothHands(InventoryItem var1) {
      return this.isPrimaryHandItem(var1) && this.isSecondaryHandItem(var1);
   }

   public boolean removeFromHands(InventoryItem var1) {
      boolean var2 = true;
      if (this.isPrimaryHandItem(var1)) {
         this.setPrimaryHandItem((InventoryItem)null);
      }

      if (this.isSecondaryHandItem(var1)) {
         this.setSecondaryHandItem((InventoryItem)null);
      }

      return var2;
   }

   public Color getSpeakColour() {
      return this.SpeakColour;
   }

   public void setSpeakColour(Color var1) {
      this.SpeakColour = var1;
   }

   public void setSpeakColourInfo(ColorInfo var1) {
      this.SpeakColour = new Color(var1.r, var1.g, var1.b, 1.0F);
   }

   public float getSlowFactor() {
      return this.slowFactor;
   }

   public void setSlowFactor(float var1) {
      this.slowFactor = var1;
   }

   public float getSlowTimer() {
      return this.slowTimer;
   }

   public void setSlowTimer(float var1) {
      this.slowTimer = var1;
   }

   public boolean isbUseParts() {
      return this.bUseParts;
   }

   public void setbUseParts(boolean var1) {
      this.bUseParts = var1;
   }

   public boolean isSpeaking() {
      return this.IsSpeaking();
   }

   public void setSpeaking(boolean var1) {
      this.Speaking = var1;
   }

   public float getSpeakTime() {
      return this.SpeakTime;
   }

   public void setSpeakTime(int var1) {
      this.SpeakTime = (float)var1;
   }

   public float getSpeedMod() {
      return this.speedMod;
   }

   public void setSpeedMod(float var1) {
      this.speedMod = var1;
   }

   public float getStaggerTimeMod() {
      return this.staggerTimeMod;
   }

   public void setStaggerTimeMod(float var1) {
      this.staggerTimeMod = var1;
   }

   public StateMachine getStateMachine() {
      return this.stateMachine;
   }

   public Moodles getMoodles() {
      return this.Moodles;
   }

   public Stats getStats() {
      return this.stats;
   }

   public Stack<String> getUsedItemsOn() {
      return this.UsedItemsOn;
   }

   public HandWeapon getUseHandWeapon() {
      return this.useHandWeapon;
   }

   public void setUseHandWeapon(HandWeapon var1) {
      this.useHandWeapon = var1;
   }

   public IsoSprite getLegsSprite() {
      return this.legsSprite;
   }

   public void setLegsSprite(IsoSprite var1) {
      this.legsSprite = var1;
   }

   public IsoGridSquare getAttackTargetSquare() {
      return this.attackTargetSquare;
   }

   public void setAttackTargetSquare(IsoGridSquare var1) {
      this.attackTargetSquare = var1;
   }

   public float getBloodImpactX() {
      return this.BloodImpactX;
   }

   public void setBloodImpactX(float var1) {
      this.BloodImpactX = var1;
   }

   public float getBloodImpactY() {
      return this.BloodImpactY;
   }

   public void setBloodImpactY(float var1) {
      this.BloodImpactY = var1;
   }

   public float getBloodImpactZ() {
      return this.BloodImpactZ;
   }

   public void setBloodImpactZ(float var1) {
      this.BloodImpactZ = var1;
   }

   public IsoSprite getBloodSplat() {
      return this.bloodSplat;
   }

   public void setBloodSplat(IsoSprite var1) {
      this.bloodSplat = var1;
   }

   /** @deprecated */
   @Deprecated
   public boolean isbOnBed() {
      return this.bOnBed;
   }

   /** @deprecated */
   @Deprecated
   public void setbOnBed(boolean var1) {
      this.bOnBed = var1;
   }

   public boolean isOnBed() {
      return this.bOnBed;
   }

   public void setOnBed(boolean var1) {
      this.bOnBed = var1;
   }

   public Vector2 getMoveForwardVec() {
      return this.moveForwardVec;
   }

   public void setMoveForwardVec(Vector2 var1) {
      this.moveForwardVec.set(var1);
   }

   public boolean isPathing() {
      return this.pathing;
   }

   public void setPathing(boolean var1) {
      this.pathing = var1;
   }

   public Stack<IsoGameCharacter> getLocalEnemyList() {
      return this.LocalEnemyList;
   }

   public Stack<IsoGameCharacter> getEnemyList() {
      return this.EnemyList;
   }

   public TraitCollection getTraits() {
      return this.getCharacterTraits();
   }

   public CharacterTraits getCharacterTraits() {
      return this.Traits;
   }

   public int getMaxWeight() {
      return this.maxWeight;
   }

   public void setMaxWeight(int var1) {
      this.maxWeight = var1;
   }

   public int getMaxWeightBase() {
      return this.maxWeightBase;
   }

   public void setMaxWeightBase(int var1) {
      this.maxWeightBase = var1;
   }

   public float getSleepingTabletDelta() {
      return this.SleepingTabletDelta;
   }

   public void setSleepingTabletDelta(float var1) {
      this.SleepingTabletDelta = var1;
   }

   public float getBetaEffect() {
      return this.BetaEffect;
   }

   public void setBetaEffect(float var1) {
      this.BetaEffect = var1;
   }

   public float getDepressEffect() {
      return this.DepressEffect;
   }

   public void setDepressEffect(float var1) {
      this.DepressEffect = var1;
   }

   public float getSleepingTabletEffect() {
      return this.SleepingTabletEffect;
   }

   public void setSleepingTabletEffect(float var1) {
      this.SleepingTabletEffect = var1;
   }

   public float getBetaDelta() {
      return this.BetaDelta;
   }

   public void setBetaDelta(float var1) {
      this.BetaDelta = var1;
   }

   public float getDepressDelta() {
      return this.DepressDelta;
   }

   public void setDepressDelta(float var1) {
      this.DepressDelta = var1;
   }

   public float getPainEffect() {
      return this.PainEffect;
   }

   public void setPainEffect(float var1) {
      this.PainEffect = var1;
   }

   public float getPainDelta() {
      return this.PainDelta;
   }

   public void setPainDelta(float var1) {
      this.PainDelta = var1;
   }

   public boolean isbDoDefer() {
      return this.bDoDefer;
   }

   public void setbDoDefer(boolean var1) {
      this.bDoDefer = var1;
   }

   public Location getLastHeardSound() {
      return this.LastHeardSound;
   }

   public void setLastHeardSound(int var1, int var2, int var3) {
      this.LastHeardSound.x = var1;
      this.LastHeardSound.y = var2;
      this.LastHeardSound.z = var3;
   }

   public boolean isClimbing() {
      return this.bClimbing;
   }

   public void setbClimbing(boolean var1) {
      this.bClimbing = var1;
   }

   public boolean isLastCollidedW() {
      return this.lastCollidedW;
   }

   public void setLastCollidedW(boolean var1) {
      this.lastCollidedW = var1;
   }

   public boolean isLastCollidedN() {
      return this.lastCollidedN;
   }

   public void setLastCollidedN(boolean var1) {
      this.lastCollidedN = var1;
   }

   public float getFallTime() {
      return this.fallTime;
   }

   public float getFallTimeAdjusted() {
      return this.fallTime * 1.75F;
   }

   public void setFallTime(float var1) {
      this.fallTime = var1;
   }

   public float getLastFallSpeed() {
      return this.lastFallSpeed;
   }

   public void setLastFallSpeed(float var1) {
      this.lastFallSpeed = var1;
   }

   public boolean isbFalling() {
      return this.bFalling && !this.ragdollFall;
   }

   public void setbFalling(boolean var1) {
      this.bFalling = var1;
   }

   public IsoBuilding getCurrentBuilding() {
      if (this.current == null) {
         return null;
      } else {
         return this.current.getRoom() == null ? null : this.current.getRoom().building;
      }
   }

   public BuildingDef getCurrentBuildingDef() {
      if (this.current == null) {
         return null;
      } else if (this.current.getRoom() == null) {
         return null;
      } else {
         return this.current.getRoom().building != null ? this.current.getRoom().building.def : null;
      }
   }

   public RoomDef getCurrentRoomDef() {
      if (this.current == null) {
         return null;
      } else {
         return this.current.getRoom() != null ? this.current.getRoom().def : null;
      }
   }

   public float getTorchStrength() {
      return 0.0F;
   }

   public AnimEventBroadcaster getAnimEventBroadcaster() {
      return this.m_animEventBroadcaster;
   }

   public void OnAnimEvent(AnimLayer var1, AnimEvent var2) {
      if (var2.m_EventName != null) {
         this.animEvent(this, var2);
         if (Core.bDebug && DebugOptions.instance.Animation.AnimLayer.AllowAnimNodeOverride.getValue()) {
            dbgOnGlobalAnimEvent(this, var2);
         }

         int var3 = var1.getDepth();
         this.actionContext.reportEvent(var3, var2.m_EventName);
         this.stateMachine.stateAnimEvent(var3, var2);
      }
   }

   private static void dbgOnGlobalAnimEvent(IsoGameCharacter var0, AnimEvent var1) {
      if (Core.bDebug) {
         SwipeStatePlayer.dbgOnGlobalAnimEvent(var0, var1);
      }
   }

   private void OnAnimEvent_SetVariable(IsoGameCharacter var1, AnimationVariableReference var2, String var3) {
      DebugLog.Animation.trace("SetVariable(%s, %s)", var2, var3);
      var2.setVariable(var1, var3);
   }

   private void OnAnimEvent_ClearVariable(IsoGameCharacter var1, String var2) {
      AnimationVariableReference var3 = AnimationVariableReference.fromRawVariableName(var2);
      var3.clearVariable(var1);
   }

   private void OnAnimEvent_PlaySound(IsoGameCharacter var1, String var2) {
      var1.getEmitter().playSoundImpl(var2, this);
   }

   private void OnAnimEvent_Footstep(IsoGameCharacter var1, String var2) {
      var1.DoFootstepSound(var2);
   }

   private void OnAnimEvent_DamageWhileInTrees(IsoGameCharacter var1) {
      var1.damageWhileInTrees();
   }

   private void OnAnimEvent_SetSharedGrappleType(IsoGameCharacter var1, String var2) {
      var1.setSharedGrappleType(var2);
   }

   private void damageWhileInTrees() {
      if (!this.isZombie() && !"Tutorial".equals(Core.GameMode)) {
         int var1 = 50;
         int var2 = Rand.Next(0, BodyPartType.ToIndex(BodyPartType.MAX));
         if (this.isRunning()) {
            var1 = 30;
         }

         if (this.Traits.Outdoorsman.isSet()) {
            var1 += 50;
         }

         var1 += (int)this.getBodyPartClothingDefense(var2, false, false);
         if (Rand.NextBool(var1)) {
            this.addHole(BloodBodyPartType.FromIndex(var2));
            var1 = 6;
            if (this.Traits.ThickSkinned.isSet()) {
               var1 += 7;
            }

            if (this.Traits.ThinSkinned.isSet()) {
               var1 -= 3;
            }

            if (Rand.NextBool(var1) && (int)this.getBodyPartClothingDefense(var2, false, false) < 100) {
               BodyPart var3 = (BodyPart)this.getBodyDamage().getBodyParts().get(var2);
               if (Rand.NextBool(var1 + 10)) {
                  var3.setCut(true, true);
               } else {
                  var3.setScratched(true, true);
               }
            }
         }

      }
   }

   public float getHammerSoundMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Woodwork);
      if (var1 == 2) {
         return 0.8F;
      } else if (var1 == 3) {
         return 0.6F;
      } else if (var1 == 4) {
         return 0.4F;
      } else {
         return var1 >= 5 ? 0.4F : 1.0F;
      }
   }

   public float getWeldingSoundMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.MetalWelding);
      if (var1 == 2) {
         return 0.8F;
      } else if (var1 == 3) {
         return 0.6F;
      } else if (var1 == 4) {
         return 0.4F;
      } else {
         return var1 >= 5 ? 0.4F : 1.0F;
      }
   }

   public float getBarricadeTimeMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Woodwork);
      if (var1 == 1) {
         return 0.8F;
      } else if (var1 == 2) {
         return 0.7F;
      } else if (var1 == 3) {
         return 0.62F;
      } else if (var1 == 4) {
         return 0.56F;
      } else if (var1 == 5) {
         return 0.5F;
      } else if (var1 == 6) {
         return 0.42F;
      } else if (var1 == 7) {
         return 0.36F;
      } else if (var1 == 8) {
         return 0.3F;
      } else if (var1 == 9) {
         return 0.26F;
      } else {
         return var1 == 10 ? 0.2F : 0.7F;
      }
   }

   public float getMetalBarricadeStrengthMod() {
      switch (this.getPerkLevel(PerkFactory.Perks.MetalWelding)) {
         case 2:
            return 1.1F;
         case 3:
            return 1.14F;
         case 4:
            return 1.18F;
         case 5:
            return 1.22F;
         case 6:
            return 1.16F;
         case 7:
            return 1.3F;
         case 8:
            return 1.34F;
         case 9:
            return 1.4F;
         case 10:
            return 1.5F;
         default:
            int var1 = this.getPerkLevel(PerkFactory.Perks.Woodwork);
            if (var1 == 2) {
               return 1.1F;
            } else if (var1 == 3) {
               return 1.14F;
            } else if (var1 == 4) {
               return 1.18F;
            } else if (var1 == 5) {
               return 1.22F;
            } else if (var1 == 6) {
               return 1.26F;
            } else if (var1 == 7) {
               return 1.3F;
            } else if (var1 == 8) {
               return 1.34F;
            } else if (var1 == 9) {
               return 1.4F;
            } else {
               return var1 == 10 ? 1.5F : 1.0F;
            }
      }
   }

   public float getBarricadeStrengthMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Woodwork);
      if (var1 == 2) {
         return 1.1F;
      } else if (var1 == 3) {
         return 1.14F;
      } else if (var1 == 4) {
         return 1.18F;
      } else if (var1 == 5) {
         return 1.22F;
      } else if (var1 == 6) {
         return 1.26F;
      } else if (var1 == 7) {
         return 1.3F;
      } else if (var1 == 8) {
         return 1.34F;
      } else if (var1 == 9) {
         return 1.4F;
      } else {
         return var1 == 10 ? 1.5F : 1.0F;
      }
   }

   public float getSneakSpotMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Sneak);
      float var2 = 0.95F;
      if (var1 == 1) {
         var2 = 0.9F;
      }

      if (var1 == 2) {
         var2 = 0.8F;
      }

      if (var1 == 3) {
         var2 = 0.75F;
      }

      if (var1 == 4) {
         var2 = 0.7F;
      }

      if (var1 == 5) {
         var2 = 0.65F;
      }

      if (var1 == 6) {
         var2 = 0.6F;
      }

      if (var1 == 7) {
         var2 = 0.55F;
      }

      if (var1 == 8) {
         var2 = 0.5F;
      }

      if (var1 == 9) {
         var2 = 0.45F;
      }

      if (var1 == 10) {
         var2 = 0.4F;
      }

      var2 *= 1.2F;
      return var2;
   }

   public float getNimbleMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Nimble);
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
         return 1.38F;
      } else if (var1 == 9) {
         return 1.42F;
      } else {
         return var1 == 10 ? 1.5F : 1.0F;
      }
   }

   public float getFatigueMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Fitness);
      if (var1 == 1) {
         return 0.95F;
      } else if (var1 == 2) {
         return 0.92F;
      } else if (var1 == 3) {
         return 0.89F;
      } else if (var1 == 4) {
         return 0.87F;
      } else if (var1 == 5) {
         return 0.85F;
      } else if (var1 == 6) {
         return 0.83F;
      } else if (var1 == 7) {
         return 0.81F;
      } else if (var1 == 8) {
         return 0.79F;
      } else if (var1 == 9) {
         return 0.77F;
      } else {
         return var1 == 10 ? 0.75F : 1.0F;
      }
   }

   public float getLightfootMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Lightfoot);
      if (var1 == 1) {
         return 0.9F;
      } else if (var1 == 2) {
         return 0.79F;
      } else if (var1 == 3) {
         return 0.71F;
      } else if (var1 == 4) {
         return 0.65F;
      } else if (var1 == 5) {
         return 0.59F;
      } else if (var1 == 6) {
         return 0.52F;
      } else if (var1 == 7) {
         return 0.45F;
      } else if (var1 == 8) {
         return 0.37F;
      } else if (var1 == 9) {
         return 0.3F;
      } else {
         return var1 == 10 ? 0.2F : 0.99F;
      }
   }

   public float getPacingMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Fitness);
      if (var1 == 1) {
         return 0.8F;
      } else if (var1 == 2) {
         return 0.75F;
      } else if (var1 == 3) {
         return 0.7F;
      } else if (var1 == 4) {
         return 0.65F;
      } else if (var1 == 5) {
         return 0.6F;
      } else if (var1 == 6) {
         return 0.57F;
      } else if (var1 == 7) {
         return 0.53F;
      } else if (var1 == 8) {
         return 0.49F;
      } else if (var1 == 9) {
         return 0.46F;
      } else {
         return var1 == 10 ? 0.43F : 0.9F;
      }
   }

   public float getHyperthermiaMod() {
      float var1 = 1.0F;
      if (this.getMoodles().getMoodleLevel(MoodleType.Hyperthermia) > 1) {
         var1 = 1.0F;
         if (this.getMoodles().getMoodleLevel(MoodleType.Hyperthermia) == 4) {
            var1 = 2.0F;
         }
      }

      return var1;
   }

   public float getHittingMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Strength);
      if (var1 == 1) {
         return 0.8F;
      } else if (var1 == 2) {
         return 0.85F;
      } else if (var1 == 3) {
         return 0.9F;
      } else if (var1 == 4) {
         return 0.95F;
      } else if (var1 == 5) {
         return 1.0F;
      } else if (var1 == 6) {
         return 1.05F;
      } else if (var1 == 7) {
         return 1.1F;
      } else if (var1 == 8) {
         return 1.15F;
      } else if (var1 == 9) {
         return 1.2F;
      } else {
         return var1 == 10 ? 1.25F : 0.75F;
      }
   }

   public float getShovingMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Strength);
      if (var1 == 1) {
         return 0.8F;
      } else if (var1 == 2) {
         return 0.85F;
      } else if (var1 == 3) {
         return 0.9F;
      } else if (var1 == 4) {
         return 0.95F;
      } else if (var1 == 5) {
         return 1.0F;
      } else if (var1 == 6) {
         return 1.05F;
      } else if (var1 == 7) {
         return 1.1F;
      } else if (var1 == 8) {
         return 1.15F;
      } else if (var1 == 9) {
         return 1.2F;
      } else {
         return var1 == 10 ? 1.25F : 0.75F;
      }
   }

   public float getRecoveryMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Fitness);
      float var2 = 0.0F;
      if (var1 == 0) {
         var2 = 0.7F;
      }

      if (var1 == 1) {
         var2 = 0.8F;
      }

      if (var1 == 2) {
         var2 = 0.9F;
      }

      if (var1 == 3) {
         var2 = 1.0F;
      }

      if (var1 == 4) {
         var2 = 1.1F;
      }

      if (var1 == 5) {
         var2 = 1.2F;
      }

      if (var1 == 6) {
         var2 = 1.3F;
      }

      if (var1 == 7) {
         var2 = 1.4F;
      }

      if (var1 == 8) {
         var2 = 1.5F;
      }

      if (var1 == 9) {
         var2 = 1.55F;
      }

      if (var1 == 10) {
         var2 = 1.6F;
      }

      if (this.Traits.Obese.isSet()) {
         var2 = (float)((double)var2 * 0.4);
      }

      if (this.Traits.Overweight.isSet()) {
         var2 = (float)((double)var2 * 0.7);
      }

      if (this.Traits.VeryUnderweight.isSet()) {
         var2 = (float)((double)var2 * 0.7);
      }

      if (this.Traits.Emaciated.isSet()) {
         var2 = (float)((double)var2 * 0.3);
      }

      if (this instanceof IsoPlayer) {
         if (((IsoPlayer)this).getNutrition().getLipids() < -1500.0F) {
            var2 = (float)((double)var2 * 0.2);
         } else if (((IsoPlayer)this).getNutrition().getLipids() < -1000.0F) {
            var2 = (float)((double)var2 * 0.5);
         }

         if (((IsoPlayer)this).getNutrition().getProteins() < -1500.0F) {
            var2 = (float)((double)var2 * 0.2);
         } else if (((IsoPlayer)this).getNutrition().getProteins() < -1000.0F) {
            var2 = (float)((double)var2 * 0.5);
         }
      }

      return var2;
   }

   public float getWeightMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Strength);
      if (var1 == 1) {
         return 0.9F;
      } else if (var1 == 2) {
         return 1.07F;
      } else if (var1 == 3) {
         return 1.24F;
      } else if (var1 == 4) {
         return 1.41F;
      } else if (var1 == 5) {
         return 1.58F;
      } else if (var1 == 6) {
         return 1.75F;
      } else if (var1 == 7) {
         return 1.92F;
      } else if (var1 == 8) {
         return 2.09F;
      } else if (var1 == 9) {
         return 2.26F;
      } else {
         return var1 == 10 ? 2.5F : 0.8F;
      }
   }

   public int getHitChancesMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Aiming);
      if (var1 == 1) {
         return 1;
      } else if (var1 == 2) {
         return 1;
      } else if (var1 == 3) {
         return 2;
      } else if (var1 == 4) {
         return 2;
      } else if (var1 == 5) {
         return 3;
      } else if (var1 == 6) {
         return 3;
      } else if (var1 == 7) {
         return 4;
      } else if (var1 == 8) {
         return 4;
      } else if (var1 == 9) {
         return 5;
      } else {
         return var1 == 10 ? 5 : 1;
      }
   }

   public float getSprintMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Sprinting);
      if (var1 == 1) {
         return 1.1F;
      } else if (var1 == 2) {
         return 1.15F;
      } else if (var1 == 3) {
         return 1.2F;
      } else if (var1 == 4) {
         return 1.25F;
      } else if (var1 == 5) {
         return 1.3F;
      } else if (var1 == 6) {
         return 1.35F;
      } else if (var1 == 7) {
         return 1.4F;
      } else if (var1 == 8) {
         return 1.45F;
      } else if (var1 == 9) {
         return 1.5F;
      } else {
         return var1 == 10 ? 1.6F : 0.9F;
      }
   }

   public int getPerkLevel(PerkFactory.Perk var1) {
      PerkInfo var2 = this.getPerkInfo(var1);
      return var2 != null ? var2.level : 0;
   }

   public void setPerkLevelDebug(PerkFactory.Perk var1, int var2) {
      PerkInfo var3 = this.getPerkInfo(var1);
      if (var3 != null) {
         var3.level = var2;
      } else {
         var3 = new PerkInfo();
         var3.perk = var1;
         var3.level = var2;
         this.PerkList.add(var3);
      }

      if (GameClient.bClient && this instanceof IsoPlayer) {
         GameClient.sendPerks((IsoPlayer)this);
      }

   }

   public void LoseLevel(PerkFactory.Perk var1) {
      PerkInfo var2 = this.getPerkInfo(var1);
      if (var2 != null) {
         --var2.level;
         if (var2.level < 0) {
            var2.level = 0;
         }

         LuaEventManager.triggerEvent("LevelPerk", this, var1, var2.level, false);
         if (var1 == PerkFactory.Perks.Sneak && GameClient.bClient && this instanceof IsoPlayer) {
            GameClient.sendPerks((IsoPlayer)this);
         }

      } else {
         LuaEventManager.triggerEvent("LevelPerk", this, var1, 0, false);
      }
   }

   public void LevelPerk(PerkFactory.Perk var1, boolean var2) {
      Objects.requireNonNull(var1, "perk is null");
      if (var1 == PerkFactory.Perks.MAX) {
         throw new IllegalArgumentException("perk == Perks.MAX");
      } else {
         IsoPlayer var3 = (IsoPlayer)Type.tryCastTo(this, IsoPlayer.class);
         PerkInfo var4 = this.getPerkInfo(var1);
         if (var4 != null) {
            ++var4.level;
            if (var3 != null && !"Tutorial".equals(Core.GameMode) && this.getHoursSurvived() > 0.016666666666666666) {
               HaloTextHelper.addTextWithArrow(var3, "+1 " + var1.getName(), true, HaloTextHelper.getGoodColor());
            }

            if (var4.level > 10) {
               var4.level = 10;
            }

            LuaEventManager.triggerEventGarbage("LevelPerk", this, var1, var4.level, true);
            if (GameClient.bClient && var3 != null) {
               GameClient.sendPerks(var3);
            }

         } else {
            var4 = new PerkInfo();
            var4.perk = var1;
            var4.level = 1;
            this.PerkList.add(var4);
            if (var3 != null && !"Tutorial".equals(Core.GameMode) && this.getHoursSurvived() > 0.016666666666666666) {
               HaloTextHelper.addTextWithArrow(var3, "+1 " + var1.getName(), true, HaloTextHelper.getGoodColor());
            }

            LuaEventManager.triggerEvent("LevelPerk", this, var1, var4.level, true);
         }
      }
   }

   public void LevelPerk(PerkFactory.Perk var1) {
      this.LevelPerk(var1, true);
   }

   public void level0(PerkFactory.Perk var1) {
      PerkInfo var2 = this.getPerkInfo(var1);
      if (var2 != null) {
         var2.level = 0;
      }

   }

   public Location getLastKnownLocationOf(String var1) {
      return this.LastKnownLocation.containsKey(var1) ? (Location)this.LastKnownLocation.get(var1) : null;
   }

   public void ReadLiterature(Literature var1) {
      Stats var10000 = this.stats;
      var10000.stress += var1.getStressChange();
      this.getBodyDamage().JustReadSomething(var1);
      if (var1.getTeachedRecipes() != null) {
         for(int var2 = 0; var2 < var1.getTeachedRecipes().size(); ++var2) {
            if (!this.getKnownRecipes().contains(var1.getTeachedRecipes().get(var2))) {
               this.learnRecipe((String)var1.getTeachedRecipes().get(var2));
            }
         }
      }

      if (var1.hasTag("ConsumeOnRead")) {
         var1.Use();
      }

   }

   public void OnDeath() {
      LuaEventManager.triggerEvent("OnCharacterDeath", this);
   }

   public void splatBloodFloorBig() {
      if (this.getCurrentSquare() != null && this.getCurrentSquare().getChunk() != null) {
         this.getCurrentSquare().getChunk().addBloodSplat(this.getX(), this.getY(), this.getZ(), Rand.Next(20));
      }

   }

   public void splatBloodFloor() {
      if (this.getCurrentSquare() != null) {
         if (this.getCurrentSquare().getChunk() != null) {
            if (this.isDead() && Rand.Next(10) == 0) {
               this.getCurrentSquare().getChunk().addBloodSplat(this.getX(), this.getY(), this.getZ(), Rand.Next(20));
            }

            if (Rand.Next(14) == 0) {
               this.getCurrentSquare().getChunk().addBloodSplat(this.getX(), this.getY(), this.getZ(), Rand.Next(8));
            }

            if (Rand.Next(50) == 0) {
               this.getCurrentSquare().getChunk().addBloodSplat(this.getX(), this.getY(), this.getZ(), Rand.Next(20));
            }

         }
      }
   }

   public int getThreatLevel() {
      int var1 = this.LocalRelevantEnemyList.size();
      var1 += this.VeryCloseEnemyList.size() * 10;
      if (var1 > 20) {
         return 3;
      } else if (var1 > 10) {
         return 2;
      } else {
         return var1 > 0 ? 1 : 0;
      }
   }

   public boolean isDead() {
      return this.Health <= 0.0F || this.getBodyDamage() != null && this.getBodyDamage().getHealth() <= 0.0F;
   }

   public boolean isAlive() {
      return !this.isDead();
   }

   public boolean isEditingRagdoll() {
      return this.isEditingRagdoll;
   }

   public void setEditingRagdoll(boolean var1) {
      this.isEditingRagdoll = var1;
   }

   public boolean isRagdoll() {
      return this.isRagdoll;
   }

   public void setIsRagdoll(boolean var1) {
      this.isRagdoll = var1;
   }

   public boolean canRagdoll() {
      return this.useRagdoll;
   }

   public void setRagdollFall(boolean var1) {
      this.ragdollFall = var1;
   }

   public boolean isRagdollFall() {
      return this.ragdollFall;
   }

   public boolean isVehicleCollision() {
      return this.vehicleCollision;
   }

   public void setVehicleCollision(boolean var1) {
      this.vehicleCollision = var1;
   }

   public boolean useRagdollVehicleCollision() {
      return this.useRagdoll && this.vehicleCollision;
   }

   public boolean isUpright() {
      if (this.useRagdoll()) {
         return this.getRagdollController() == null ? false : this.getRagdollController().isUpright();
      } else {
         return false;
      }
   }

   public boolean isOnBack() {
      return this.getRagdollController() == null ? false : this.getRagdollController().isOnBack();
   }

   public boolean usePhysicHitReaction() {
      return this.usePhysicHitReaction;
   }

   public void setUsePhysicHitReaction(boolean var1) {
      this.usePhysicHitReaction = var1;
   }

   public boolean isRagdollSimulationActive() {
      return this.useRagdoll() && this.getRagdollController() != null ? this.getRagdollController().isSimulationActive() : false;
   }

   public void Seen(Stack<IsoMovingObject> var1) {
      synchronized(this.LocalList) {
         this.LocalList.clear();
         this.LocalList.addAll(var1);
      }
   }

   public boolean CanSee(IsoMovingObject var1) {
      return LosUtil.lineClear(this.getCell(), PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), PZMath.fastfloor(var1.getX()), PZMath.fastfloor(var1.getY()), PZMath.fastfloor(var1.getZ()), false) != LosUtil.TestResults.Blocked;
   }

   public IsoGridSquare getLowDangerInVicinity(int var1, int var2) {
      float var3 = -1000000.0F;
      IsoGridSquare var4 = null;

      for(int var5 = 0; var5 < var1; ++var5) {
         float var6 = 0.0F;
         int var7 = Rand.Next(-var2, var2);
         int var8 = Rand.Next(-var2, var2);
         IsoGridSquare var9 = this.getCell().getGridSquare(PZMath.fastfloor(this.getX()) + var7, PZMath.fastfloor(this.getY()) + var8, PZMath.fastfloor(this.getZ()));
         if (var9 != null && var9.isFree(true)) {
            float var10 = (float)var9.getMovingObjects().size();
            if (var9.getE() != null) {
               var10 += (float)var9.getE().getMovingObjects().size();
            }

            if (var9.getS() != null) {
               var10 += (float)var9.getS().getMovingObjects().size();
            }

            if (var9.getW() != null) {
               var10 += (float)var9.getW().getMovingObjects().size();
            }

            if (var9.getN() != null) {
               var10 += (float)var9.getN().getMovingObjects().size();
            }

            var6 -= var10 * 1000.0F;
            if (var6 > var3) {
               var3 = var6;
               var4 = var9;
            }
         }
      }

      return var4;
   }

   public void Anger(int var1) {
      float var2 = 10.0F;
      if ((float)Rand.Next(100) < var2) {
         var1 *= 2;
      }

      var1 = (int)((float)var1 * (this.stats.getStress() + 1.0F));
      var1 = (int)((float)var1 * (this.getBodyDamage().getUnhappynessLevel() / 100.0F + 1.0F));
      Stats var10000 = this.stats;
      var10000.Anger += (float)var1 / 100.0F;
   }

   public boolean hasEquipped(String var1) {
      if (var1.contains(".")) {
         var1 = var1.split("\\.")[1];
      }

      if (this.leftHandItem != null && this.leftHandItem.getType().equals(var1)) {
         return true;
      } else {
         return this.rightHandItem != null && this.rightHandItem.getType().equals(var1);
      }
   }

   public boolean hasEquippedTag(String var1) {
      if (this.leftHandItem != null && this.leftHandItem.hasTag(var1)) {
         return true;
      } else {
         return this.rightHandItem != null && this.rightHandItem.hasTag(var1);
      }
   }

   public boolean hasWornTag(String var1) {
      for(int var2 = 0; var2 < this.getWornItems().size(); ++var2) {
         InventoryItem var3 = this.getWornItems().getItemByIndex(var2);
         if (var3.hasTag(var1)) {
            return true;
         }
      }

      return false;
   }

   public void setDir(IsoDirections var1) {
      this.dir = var1;
      this.getVectorFromDirection(this.m_forwardDirection);
   }

   public void Callout(boolean var1) {
      if (this.isCanShout()) {
         this.Callout();
         if (var1) {
            this.playEmote("shout");
         }

      }
   }

   public void Callout() {
      // $FF: Couldn't be decompiled
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      super.load(var1, var2, var3);
      this.getVectorFromDirection(this.m_forwardDirection);
      if (var1.get() == 1) {
         this.descriptor = new SurvivorDesc(true);
         this.descriptor.load(var1, var2, this);
         this.bFemale = this.descriptor.isFemale();
      }

      this.getVisual().load(var1, var2);
      ArrayList var4 = this.inventory.load(var1, var2);
      this.savedInventoryItems.clear();

      for(int var5 = 0; var5 < var4.size(); ++var5) {
         this.savedInventoryItems.add((InventoryItem)var4.get(var5));
      }

      this.Asleep = var1.get() == 1;
      this.ForceWakeUpTime = var1.getFloat();
      int var6;
      if (!this.isZombie()) {
         this.stats.load(var1, var2);
         this.getBodyDamage().load(var1, var2);
         this.xp.load(var1, var2);
         ArrayList var12 = this.inventory.IncludingObsoleteItems;
         var6 = var1.getInt();
         if (var6 >= 0 && var6 < var12.size()) {
            this.leftHandItem = (InventoryItem)var12.get(var6);
         }

         var6 = var1.getInt();
         if (var6 >= 0 && var6 < var12.size()) {
            this.rightHandItem = (InventoryItem)var12.get(var6);
         }

         this.setEquipParent((InventoryItem)null, this.leftHandItem);
         if (this.rightHandItem == this.leftHandItem) {
            this.setEquipParent((InventoryItem)null, this.rightHandItem, false);
         } else {
            this.setEquipParent((InventoryItem)null, this.rightHandItem);
         }
      }

      boolean var13 = var1.get() == 1;
      if (var13) {
         this.SetOnFire();
      }

      this.DepressEffect = var1.getFloat();
      this.DepressFirstTakeTime = var1.getFloat();
      this.BetaEffect = var1.getFloat();
      this.BetaDelta = var1.getFloat();
      this.PainEffect = var1.getFloat();
      this.PainDelta = var1.getFloat();
      this.SleepingTabletEffect = var1.getFloat();
      this.SleepingTabletDelta = var1.getFloat();
      var6 = var1.getInt();

      int var7;
      for(var7 = 0; var7 < var6; ++var7) {
         ReadBook var8 = new ReadBook();
         var8.fullType = GameWindow.ReadString(var1);
         var8.alreadyReadPages = var1.getInt();
         this.ReadBooks.add(var8);
      }

      this.reduceInfectionPower = var1.getFloat();
      var7 = var1.getInt();

      int var14;
      for(var14 = 0; var14 < var7; ++var14) {
         this.knownRecipes.add(GameWindow.ReadString(var1));
      }

      this.lastHourSleeped = var1.getInt();
      this.timeSinceLastSmoke = var1.getFloat();
      this.beardGrowTiming = var1.getFloat();
      this.hairGrowTiming = var1.getFloat();
      this.setUnlimitedCarry(var1.get() == 1);
      this.setBuildCheat(var1.get() == 1);
      this.setHealthCheat(var1.get() == 1);
      this.setMechanicsCheat(var1.get() == 1);
      this.setMovablesCheat(var1.get() == 1);
      this.setFarmingCheat(var1.get() == 1);
      if (var2 >= 202) {
         this.setFishingCheat(var1.get() == 1);
      }

      if (var2 >= 217) {
         this.setCanUseBrushTool(var1.get() == 1);
         this.setFastMoveCheat(var1.get() == 1);
      }

      this.setTimedActionInstantCheat(var1.get() == 1);
      this.setUnlimitedEndurance(var1.get() == 1);
      this.setSneaking(var1.get() == 1);
      this.setDeathDragDown(var1.get() == 1);
      var14 = var1.getInt();

      for(int var9 = 0; var9 < var14; ++var9) {
         String var10 = GameWindow.ReadString(var1);
         int var11 = var1.getInt();
         this.addReadLiterature(var10, var11);
      }

      this.lastAnimalPet = var1.getLong();
   }

   public String getDescription(String var1) {
      String var10000 = this.getClass().getSimpleName();
      String var2 = var10000 + " [" + var1;
      var2 = var2 + "isDead=" + this.isDead() + " | " + var1;
      var2 = var2 + super.getDescription(var1 + "    ") + " | " + var1;
      var2 = var2 + "inventory=";

      for(int var3 = 0; var3 < this.inventory.Items.size() - 1; ++var3) {
         var2 = var2 + this.inventory.Items.get(var3) + ", ";
      }

      var2 = var2 + " ] ";
      return var2;
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      DebugLog.Saving.trace("Saving: %s", this);
      super.save(var1, var2);
      if (this.descriptor == null) {
         var1.put((byte)0);
      } else {
         var1.put((byte)1);
         this.descriptor.save(var1);
      }

      this.getVisual().save(var1);
      ArrayList var3 = this.inventory.save(var1, this);
      this.savedInventoryItems.clear();

      int var4;
      for(var4 = 0; var4 < var3.size(); ++var4) {
         this.savedInventoryItems.add((InventoryItem)var3.get(var4));
      }

      var1.put((byte)(this.Asleep ? 1 : 0));
      var1.putFloat(this.ForceWakeUpTime);
      if (!this.isZombie()) {
         this.stats.save(var1);
         this.getBodyDamage().save(var1);
         this.xp.save(var1);
         if (this.leftHandItem != null) {
            var1.putInt(this.inventory.getItems().indexOf(this.leftHandItem));
         } else {
            var1.putInt(-1);
         }

         if (this.rightHandItem != null) {
            var1.putInt(this.inventory.getItems().indexOf(this.rightHandItem));
         } else {
            var1.putInt(-1);
         }
      }

      var1.put((byte)(this.OnFire ? 1 : 0));
      var1.putFloat(this.DepressEffect);
      var1.putFloat(this.DepressFirstTakeTime);
      var1.putFloat(this.BetaEffect);
      var1.putFloat(this.BetaDelta);
      var1.putFloat(this.PainEffect);
      var1.putFloat(this.PainDelta);
      var1.putFloat(this.SleepingTabletEffect);
      var1.putFloat(this.SleepingTabletDelta);
      var1.putInt(this.ReadBooks.size());

      for(var4 = 0; var4 < this.ReadBooks.size(); ++var4) {
         ReadBook var5 = (ReadBook)this.ReadBooks.get(var4);
         GameWindow.WriteString(var1, var5.fullType);
         var1.putInt(var5.alreadyReadPages);
      }

      var1.putFloat(this.reduceInfectionPower);
      var1.putInt(this.knownRecipes.size());

      for(var4 = 0; var4 < this.knownRecipes.size(); ++var4) {
         String var6 = (String)this.knownRecipes.get(var4);
         GameWindow.WriteString(var1, var6);
      }

      var1.putInt(this.lastHourSleeped);
      var1.putFloat(this.timeSinceLastSmoke);
      var1.putFloat(this.beardGrowTiming);
      var1.putFloat(this.hairGrowTiming);
      var1.put((byte)(this.isUnlimitedCarry() ? 1 : 0));
      var1.put((byte)(this.isBuildCheat() ? 1 : 0));
      var1.put((byte)(this.isHealthCheat() ? 1 : 0));
      var1.put((byte)(this.isMechanicsCheat() ? 1 : 0));
      var1.put((byte)(this.isMovablesCheat() ? 1 : 0));
      var1.put((byte)(this.isFarmingCheat() ? 1 : 0));
      var1.put((byte)(this.isFishingCheat() ? 1 : 0));
      var1.put((byte)(this.isCanUseBrushTool() ? 1 : 0));
      var1.put((byte)(this.isFastMoveCheat() ? 1 : 0));
      var1.put((byte)(this.isTimedActionInstantCheat() ? 1 : 0));
      var1.put((byte)(this.isUnlimitedEndurance() ? 1 : 0));
      var1.put((byte)(this.isSneaking() ? 1 : 0));
      var1.put((byte)(this.isDeathDragDown() ? 1 : 0));
      var1.putInt(this.readLiterature.size());
      Iterator var8 = this.getReadLiterature().entrySet().iterator();

      while(var8.hasNext()) {
         Map.Entry var7 = (Map.Entry)var8.next();
         GameWindow.WriteString(var1, (String)var7.getKey());
         var1.putInt((Integer)var7.getValue());
      }

      var1.putLong(this.lastAnimalPet);
   }

   public ChatElement getChatElement() {
      return this.chatElement;
   }

   public void StartAction(BaseAction var1) {
      this.CharacterActions.clear();
      this.CharacterActions.push(var1);
      if (var1.valid()) {
         var1.waitToStart();
      }

   }

   public void QueueAction(BaseAction var1) {
   }

   public void StopAllActionQueue() {
      if (!this.CharacterActions.isEmpty()) {
         BaseAction var1 = (BaseAction)this.CharacterActions.get(0);
         if (var1.bStarted) {
            var1.stop();
         }

         this.CharacterActions.clear();
         if (this == IsoPlayer.players[0] || this == IsoPlayer.players[1] || this == IsoPlayer.players[2] || this == IsoPlayer.players[3]) {
            UIManager.getProgressBar((double)((IsoPlayer)this).getPlayerNum()).setValue(0.0F);
         }

      }
   }

   public void StopAllActionQueueRunning() {
      if (!this.CharacterActions.isEmpty()) {
         BaseAction var1 = (BaseAction)this.CharacterActions.get(0);
         if (var1.StopOnRun) {
            if (var1.bStarted) {
               var1.stop();
            }

            this.CharacterActions.clear();
            if (this == IsoPlayer.players[0] || this == IsoPlayer.players[1] || this == IsoPlayer.players[2] || this == IsoPlayer.players[3]) {
               UIManager.getProgressBar((double)((IsoPlayer)this).getPlayerNum()).setValue(0.0F);
            }

         }
      }
   }

   public void StopAllActionQueueAiming() {
      if (!this.CharacterActions.isEmpty()) {
         BaseAction var1 = (BaseAction)this.CharacterActions.get(0);
         if (var1.StopOnAim) {
            if (var1.bStarted) {
               var1.stop();
            }

            this.CharacterActions.clear();
            if (this == IsoPlayer.players[0] || this == IsoPlayer.players[1] || this == IsoPlayer.players[2] || this == IsoPlayer.players[3]) {
               UIManager.getProgressBar((double)((IsoPlayer)this).getPlayerNum()).setValue(0.0F);
            }

         }
      }
   }

   public void StopAllActionQueueWalking() {
      if (!this.CharacterActions.isEmpty()) {
         BaseAction var1 = (BaseAction)this.CharacterActions.get(0);
         if (var1.StopOnWalk) {
            if (var1.bStarted) {
               var1.stop();
            }

            this.CharacterActions.clear();
            if (this == IsoPlayer.players[0] || this == IsoPlayer.players[1] || this == IsoPlayer.players[2] || this == IsoPlayer.players[3]) {
               UIManager.getProgressBar((double)((IsoPlayer)this).getPlayerNum()).setValue(0.0F);
            }

         }
      }
   }

   public String GetAnimSetName() {
      return "Base";
   }

   public void SleepingTablet(float var1) {
      this.SleepingTabletEffect = 6600.0F;
      this.SleepingTabletDelta += var1;
   }

   public void BetaBlockers(float var1) {
      this.BetaEffect = 6600.0F;
      this.BetaDelta += var1;
   }

   public void BetaAntiDepress(float var1) {
      if (this.DepressEffect == 0.0F) {
         this.DepressFirstTakeTime = 10000.0F;
      }

      this.DepressEffect = 6600.0F;
      this.DepressDelta += var1;
   }

   public void PainMeds(float var1) {
      this.PainEffect = 5400.0F;
      this.PainDelta += var1;
   }

   public void initSpritePartsEmpty() {
      this.InitSpriteParts(this.descriptor);
   }

   public void InitSpriteParts(SurvivorDesc var1) {
      this.sprite.disposeAnimation();
      this.legsSprite = this.sprite;
      this.legsSprite.name = var1.torso;
      this.bUseParts = true;
   }

   public boolean HasTrait(String var1) {
      return this.Traits.contains(var1);
   }

   public void ApplyInBedOffset(boolean var1) {
      if (var1) {
         if (!this.bOnBed) {
            this.offsetX -= 20.0F;
            this.offsetY += 21.0F;
            this.bOnBed = true;
         }
      } else if (this.bOnBed) {
         this.offsetX += 20.0F;
         this.offsetY -= 21.0F;
         this.bOnBed = false;
      }

   }

   public void Dressup(SurvivorDesc var1) {
      if (!this.isZombie()) {
         if (this.wornItems != null) {
            ItemVisuals var2 = new ItemVisuals();
            var1.getItemVisuals(var2);
            this.wornItems.setFromItemVisuals(var2);
            this.wornItems.addItemsToItemContainer(this.inventory);
            var1.wornItems.clear();
            this.onWornItemsChanged();
         }
      }
   }

   public void setPathSpeed(float var1) {
   }

   public void PlayAnim(String var1) {
   }

   public void PlayAnimWithSpeed(String var1, float var2) {
   }

   public void PlayAnimUnlooped(String var1) {
   }

   public void DirectionFromVector(Vector2 var1) {
      this.dir = IsoDirections.fromAngle(var1);
   }

   public void DoFootstepSound(String var1) {
      float var2 = 1.0F;
      switch (var1) {
         case "sneak_walk":
            var2 = 0.2F;
            break;
         case "sneak_run":
            var2 = 0.5F;
            break;
         case "strafe":
            var2 = this.bSneaking ? 0.2F : 0.3F;
            break;
         case "walk":
            var2 = 0.5F;
            break;
         case "run":
            var2 = 1.3F;
            break;
         case "sprint":
            var2 = 1.8F;
      }

      this.DoFootstepSound(var2);
   }

   public void DoFootstepSound(float var1) {
      IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(this, IsoPlayer.class);
      if (GameClient.bClient && var2 != null && var2.networkAI != null) {
         var2.networkAI.footstepSoundRadius = 0;
      }

      if (var2 == null || !var2.isGhostMode() || DebugOptions.instance.Character.Debug.PlaySoundWhenInvisible.getValue()) {
         if (this.getCurrentSquare() != null) {
            if (!(var1 <= 0.0F)) {
               float var3 = var1;
               var1 *= 1.4F;
               if (this.Traits.Graceful.isSet()) {
                  var1 *= 0.6F;
               }

               if (this.Traits.Clumsy.isSet()) {
                  var1 *= 1.2F;
               }

               if (this.getWornItem("Shoes") == null) {
                  var1 *= 0.5F;
               }

               var1 *= this.getLightfootMod();
               var1 *= 2.0F - this.getNimbleMod();
               if (this.bSneaking) {
                  var1 *= this.getSneakSpotMod();
               }

               if (var1 > 0.0F) {
                  this.emitter.playFootsteps("HumanFootstepsCombined", var3);
                  if (var2 != null && var2.isGhostMode()) {
                     return;
                  }

                  int var4 = (int)Math.ceil((double)(var1 * 10.0F));
                  if (this.bSneaking) {
                     var4 = Math.max(1, var4);
                  }

                  if (this.getCurrentSquare().getRoom() != null) {
                     var4 = (int)((float)var4 * 0.5F);
                  }

                  int var5 = 2;
                  if (this.bSneaking) {
                     var5 = Math.min(12, 4 + this.getPerkLevel(PerkFactory.Perks.Lightfoot));
                  }

                  if (GameClient.bClient && var2 != null && var2.networkAI != null) {
                     var2.networkAI.footstepSoundRadius = (byte)var4;
                  }

                  if (Rand.Next(var5) == 0) {
                     WorldSoundManager.instance.addSound(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), var4, var4, false, 0.0F, 1.0F, false, false, false);
                  }
               }

            }
         }
      }
   }

   public boolean Eat(InventoryItem var1, float var2) {
      return this.Eat(var1, var2, false);
   }

   public boolean EatOnClient(InventoryItem var1, float var2) {
      Food var3 = (Food)Type.tryCastTo(var1, Food.class);
      if (var3 == null) {
         return false;
      } else {
         if (var3.getOnEat() != null) {
            Object var4 = LuaManager.getFunctionObject(var3.getOnEat());
            if (var4 != null) {
               LuaManager.caller.pcallvoid(LuaManager.thread, var4, var1, this, BoxedStaticValues.toDouble((double)var2));
            }
         }

         return true;
      }
   }

   public boolean Eat(InventoryItem var1, float var2, boolean var3) {
      Food var4 = (Food)Type.tryCastTo(var1, Food.class);
      if (var4 == null) {
         return false;
      } else {
         var2 = PZMath.clamp(var2, 0.0F, 1.0F);
         float var5 = var2;
         float var7;
         if (var4.getBaseHunger() != 0.0F && var4.getHungChange() != 0.0F) {
            float var6 = var4.getBaseHunger() * var2;
            var7 = var6 / var4.getHungChange();
            var7 = PZMath.clamp(var7, 0.0F, 1.0F);
            var2 = var7;
         }

         if (var4.getHungChange() < 0.0F && var4.getHungChange() * (1.0F - var2) > -0.01F) {
            var2 = 1.0F;
         }

         if (var4.getHungChange() == 0.0F && var4.getThirstChange() < 0.0F && var4.getThirstChange() * (1.0F - var2) > -0.01F) {
            var2 = 1.0F;
         }

         Stats var10000 = this.stats;
         var10000.thirst += var4.getThirstChange() * var2;
         if (this.stats.thirst < 0.0F) {
            this.stats.thirst = 0.0F;
         }

         var10000 = this.stats;
         var10000.hunger += var4.getHungerChange() * var2;
         var10000 = this.stats;
         var10000.endurance += var4.getEnduranceChange() * var2;
         var10000 = this.stats;
         var10000.stress += var4.getStressChange() * var2;
         var10000 = this.stats;
         var10000.fatigue += var4.getFatigueChange() * var2;
         IsoPlayer var12 = (IsoPlayer)Type.tryCastTo(this, IsoPlayer.class);
         Nutrition var13;
         if (var12 != null && !var4.isBurnt()) {
            var13 = var12.getNutrition();
            var13.setCalories(var13.getCalories() + var4.getCalories() * var2);
            var13.setCarbohydrates(var13.getCarbohydrates() + var4.getCarbohydrates() * var2);
            var13.setProteins(var13.getProteins() + var4.getProteins() * var2);
            var13.setLipids(var13.getLipids() + var4.getLipids() * var2);
         } else if (var12 != null && var4.isBurnt()) {
            var13 = var12.getNutrition();
            var13.setCalories(var13.getCalories() + var4.getCalories() * var2 / 5.0F);
            var13.setCarbohydrates(var13.getCarbohydrates() + var4.getCarbohydrates() * var2 / 5.0F);
            var13.setProteins(var13.getProteins() + var4.getProteins() * var2 / 5.0F);
            var13.setLipids(var13.getLipids() + var4.getLipids() * var2 / 5.0F);
         }

         this.getBodyDamage().setPainReduction(this.getBodyDamage().getPainReduction() + var4.getPainReduction() * var2);
         this.getBodyDamage().setColdReduction(this.getBodyDamage().getColdReduction() + (float)var4.getFluReduction() * var2);
         float var8;
         if (this.getBodyDamage().getFoodSicknessLevel() > 0.0F && (float)var4.getReduceFoodSickness() > 0.0F && this.effectiveEdibleBuffTimer <= 0.0F) {
            var7 = this.getBodyDamage().getFoodSicknessLevel();
            this.getBodyDamage().setFoodSicknessLevel(this.getBodyDamage().getFoodSicknessLevel() - (float)var4.getReduceFoodSickness() * var2);
            if (this.getBodyDamage().getFoodSicknessLevel() < 0.0F) {
               this.getBodyDamage().setFoodSicknessLevel(0.0F);
            }

            var8 = this.getBodyDamage().getPoisonLevel();
            this.getBodyDamage().setPoisonLevel(this.getBodyDamage().getPoisonLevel() - (float)var4.getReduceFoodSickness() * var2);
            if (this.getBodyDamage().getPoisonLevel() < 0.0F) {
               this.getBodyDamage().setPoisonLevel(0.0F);
            }

            if (this.Traits.IronGut.isSet()) {
               this.effectiveEdibleBuffTimer = Rand.Next(80.0F, 150.0F);
            } else if (this.Traits.WeakStomach.isSet()) {
               this.effectiveEdibleBuffTimer = Rand.Next(200.0F, 280.0F);
            } else {
               this.effectiveEdibleBuffTimer = Rand.Next(120.0F, 230.0F);
            }
         }

         this.getBodyDamage().JustAteFood(var4, var2, var3);
         if (GameServer.bServer && this instanceof IsoPlayer) {
            INetworkPacket.send((IsoPlayer)this, PacketTypes.PacketType.SyncPlayerStats, this, 20566);
            GameServer.sendSyncPlayerFields((IsoPlayer)this, (byte)8);
            INetworkPacket.send((IsoPlayer)this, PacketTypes.PacketType.EatFood, this, var4, var2);
         }

         if (var4.getOnEat() != null) {
            Object var14 = LuaManager.getFunctionObject(var4.getOnEat());
            if (var14 != null) {
               LuaManager.caller.pcallvoid(LuaManager.thread, var14, var1, this, BoxedStaticValues.toDouble((double)var2));
            }
         }

         if (var2 == 1.0F) {
            var4.setHungChange(0.0F);
            var4.UseAndSync();
         } else {
            var7 = var4.getHungChange();
            var8 = var4.getThirstChange();
            var4.multiplyFoodValues(1.0F - var2);
            if (var7 == 0.0F && var8 < 0.0F && var4.getThirstChange() > -0.01F) {
               var4.setHungChange(0.0F);
               var4.UseAndSync();
               return true;
            }

            float var9 = 0.0F;
            if (var4.isCustomWeight()) {
               String var10 = var4.getReplaceOnUseFullType();
               Item var11 = var10 == null ? null : ScriptManager.instance.getItem(var10);
               if (var11 != null) {
                  var9 = var11.getActualWeight();
               }

               var4.setWeight(var4.getWeight() - var9 - var5 * (var4.getWeight() - var9) + var9);
               var4.syncItemFields();
            }
         }

         return true;
      }
   }

   public boolean Eat(InventoryItem var1) {
      return this.Eat(var1, 1.0F);
   }

   public boolean DrinkFluid(InventoryItem var1, float var2) {
      return this.DrinkFluid(var1, var2, false);
   }

   public boolean DrinkFluid(InventoryItem var1, float var2, boolean var3) {
      if (!var1.hasComponent(ComponentType.FluidContainer)) {
         return false;
      } else {
         FluidContainer var4 = var1.getFluidContainer();
         FluidConsume var5 = var4.removeFluid(var4.getAmount() * var2, true);
         Stats var10000 = this.stats;
         var10000.thirst += var5.getThirstChange();
         var10000 = this.stats;
         var10000.hunger += var5.getHungerChange();
         var10000 = this.stats;
         var10000.endurance += var5.getEnduranceChange();
         var10000 = this.stats;
         var10000.stress += var5.getStressChange();
         var10000 = this.stats;
         var10000.fatigue += var5.getFatigueChange();
         var10000 = this.stats;
         var10000.Boredom += var5.getUnhappyChange();
         IsoPlayer var6 = (IsoPlayer)Type.tryCastTo(this, IsoPlayer.class);
         if (var6 != null) {
            Nutrition var7 = var6.getNutrition();
            var7.setCalories(var7.getCalories() + var4.getProperties().getCalories() * var2);
            var7.setCarbohydrates(var7.getCarbohydrates() + var4.getProperties().getCarbohydrates() * var2);
            var7.setProteins(var7.getProteins() + var4.getProperties().getProteins() * var2);
            var7.setLipids(var7.getLipids() + var4.getProperties().getLipids() * var2);
         }

         this.getBodyDamage().JustDrankBoozeFluid(var5.getAlcohol());
         this.getBodyDamage().setPainReduction(this.getBodyDamage().getPainReduction() + var5.getPainReduction());
         this.getBodyDamage().setColdReduction(this.getBodyDamage().getColdReduction() + var5.getFluReduction());
         this.getBodyDamage().setPoisonLevel(this.getBodyDamage().getPoisonLevel() + (float)var5.getPoisonEffect().getPlayerEffect());
         if (this.getBodyDamage().getFoodSicknessLevel() > 0.0F && var5.getFoodSicknessReduction() > 0.0F && this.effectiveEdibleBuffTimer <= 0.0F) {
            float var9 = this.getBodyDamage().getFoodSicknessLevel();
            this.getBodyDamage().setFoodSicknessLevel(this.getBodyDamage().getFoodSicknessLevel() - var5.getFoodSicknessReduction());
            if (this.getBodyDamage().getFoodSicknessLevel() < 0.0F) {
               this.getBodyDamage().setFoodSicknessLevel(0.0F);
            }

            float var8 = this.getBodyDamage().getPoisonLevel();
            this.getBodyDamage().setPoisonLevel(this.getBodyDamage().getPoisonLevel() - var5.getFoodSicknessReduction());
            if (this.getBodyDamage().getPoisonLevel() < 0.0F) {
               this.getBodyDamage().setPoisonLevel(0.0F);
            }

            if (this.Traits.IronGut.isSet()) {
               this.effectiveEdibleBuffTimer = Rand.Next(80.0F, 150.0F);
            } else if (this.Traits.WeakStomach.isSet()) {
               this.effectiveEdibleBuffTimer = Rand.Next(200.0F, 280.0F);
            } else {
               this.effectiveEdibleBuffTimer = Rand.Next(120.0F, 230.0F);
            }
         }

         if (GameServer.bServer && this instanceof IsoPlayer) {
            INetworkPacket.send((IsoPlayer)this, PacketTypes.PacketType.SyncPlayerStats, this, 26710);
         }

         return true;
      }
   }

   public boolean DrinkFluid(InventoryItem var1) {
      return this.DrinkFluid(var1, 1.0F);
   }

   public void FireCheck() {
      if (!this.OnFire) {
         if (!GameServer.bServer || !(this instanceof IsoPlayer)) {
            if (!GameClient.bClient || !this.isZombie() || !(this instanceof IsoZombie) || !((IsoZombie)this).isRemoteZombie()) {
               if (this.isZombie() && VirtualZombieManager.instance.isReused((IsoZombie)this)) {
                  DebugLog.log(DebugType.Zombie, "FireCheck running on REUSABLE ZOMBIE - IGNORED " + this);
               } else if (this.getVehicle() == null) {
                  if (this.square != null && !GameServer.bServer && (!GameClient.bClient || this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer() || this instanceof IsoZombie && !((IsoZombie)this).isRemoteZombie()) && this.square.getProperties().Is(IsoFlagType.burning)) {
                     if ((!(this instanceof IsoPlayer) || Rand.Next(Rand.AdjustForFramerate(70)) != 0) && !this.isZombie() && !(this instanceof IsoAnimal)) {
                        float var1;
                        if (!(this instanceof IsoPlayer)) {
                           var1 = this.FireKillRate * GameTime.instance.getMultiplier() / 2.0F;
                           CombatManager.getInstance().applyDamage(this, var1);
                           this.setAttackedBy((IsoGameCharacter)null);
                        } else {
                           var1 = this.FireKillRate * GameTime.instance.getThirtyFPSMultiplier() * 60.0F / 2.0F;
                           this.getBodyDamage().ReduceGeneralHealth(var1);
                           LuaEventManager.triggerEvent("OnPlayerGetDamage", this, "FIRE", var1);
                           this.getBodyDamage().OnFire(true);
                           this.forceAwake();
                        }

                        if (this.isDead()) {
                           IsoFireManager.RemoveBurningCharacter(this);
                           if (this.isZombie()) {
                              LuaEventManager.triggerEvent("OnZombieDead", this);
                              if (GameClient.bClient) {
                                 this.setAttackedBy(IsoWorld.instance.CurrentCell.getFakeZombieForHit());
                              }
                           }
                        }
                     } else {
                        this.SetOnFire();
                     }
                  }

               }
            }
         }
      }
   }

   public String getPrimaryHandType() {
      return this.leftHandItem == null ? null : this.leftHandItem.getType();
   }

   public float getGlobalMovementMod(boolean var1) {
      return this.getCurrentState() != ClimbOverFenceState.instance() && this.getCurrentState() != ClimbThroughWindowState.instance() && this.getCurrentState() != ClimbOverWallState.instance() ? super.getGlobalMovementMod(var1) : 1.0F;
   }

   public float getMovementSpeed() {
      tempo2.x = this.getX() - this.getLastX();
      tempo2.y = this.getY() - this.getLastY();
      return tempo2.getLength();
   }

   public String getSecondaryHandType() {
      return this.rightHandItem == null ? null : this.rightHandItem.getType();
   }

   public boolean HasItem(String var1) {
      if (var1 == null) {
         return true;
      } else {
         return var1.equals(this.getSecondaryHandType()) || var1.equals(this.getPrimaryHandType()) || this.inventory.contains(var1);
      }
   }

   public void changeState(State var1) {
      this.stateMachine.changeState(var1, (Iterable)null);
   }

   public State getCurrentState() {
      return this.stateMachine.getCurrent();
   }

   public boolean isCurrentState(State var1) {
      if (this.stateMachine.isSubstate(var1)) {
         return true;
      } else {
         return this.stateMachine.getCurrent() == var1;
      }
   }

   public HashMap<Object, Object> getStateMachineParams(State var1) {
      return (HashMap)this.StateMachineParams.computeIfAbsent(var1, (var0) -> {
         return new HashMap();
      });
   }

   public void setStateMachineLocked(boolean var1) {
      this.stateMachine.setLocked(var1);
   }

   public float Hit(HandWeapon var1, IsoGameCharacter var2, float var3, boolean var4, float var5) {
      return this.Hit(var1, var2, var3, var4, var5, false);
   }

   public float Hit(HandWeapon var1, IsoGameCharacter var2, float var3, boolean var4, float var5, boolean var6) {
      if (var2 != null && var1 != null) {
         if (!var4 && this.isZombie()) {
            IsoZombie var7 = (IsoZombie)this;
            var7.setHitTime(var7.getHitTime() + 1);
            if (var7.getHitTime() >= 4 && !var6) {
               var3 = (float)((double)var3 * (double)(var7.getHitTime() - 2) * 1.5);
            }
         }

         if (var2 instanceof IsoPlayer && ((IsoPlayer)var2).isDoShove() && !((IsoPlayer)var2).isAimAtFloor()) {
            var4 = true;
            var5 *= 1.5F;
         }

         LuaEventManager.triggerEvent("OnWeaponHitCharacter", var2, this, var1, var3);
         LuaEventManager.triggerEvent("OnPlayerGetDamage", this, "WEAPONHIT", var3);
         if (LuaHookManager.TriggerHook("WeaponHitCharacter", var2, this, var1, var3)) {
            return 0.0F;
         } else if (this.m_avoidDamage) {
            this.m_avoidDamage = false;
            return 0.0F;
         } else {
            if (this.noDamage) {
               var4 = true;
               this.noDamage = false;
            }

            if (this instanceof IsoSurvivor && !this.EnemyList.contains(var2)) {
               this.EnemyList.add(var2);
            }

            if (this.isZombie() && var1.getCategories().contains("SmallBlade")) {
               this.staggerTimeMod = 0.0F;
            } else {
               this.staggerTimeMod = var1.getPushBackMod() * var1.getKnockbackMod(var2) * var2.getShovingMod();
            }

            var2.addWorldSoundUnlessInvisible(5, 1, false);
            this.hitDir.x = this.getX();
            this.hitDir.y = this.getY();
            Vector2 var10000 = this.hitDir;
            var10000.x -= var2.getX();
            var10000 = this.hitDir;
            var10000.y -= var2.getY();
            this.getHitDir().normalize();
            var10000 = this.hitDir;
            var10000.x *= var1.getPushBackMod();
            var10000 = this.hitDir;
            var10000.y *= var1.getPushBackMod();
            this.hitDir.rotate(var1.HitAngleMod);
            this.setAttackedBy(var2);
            float var12 = var3;
            if (!var6) {
               var12 = this.processHitDamage(var1, var2, var3, var4, var5);
            }

            float var8 = 0.0F;
            if (var1.isTwoHandWeapon() && (var2.getPrimaryHandItem() != var1 || var2.getSecondaryHandItem() != var1)) {
               var8 = var1.getWeight() / 1.5F / 10.0F;
            }

            float var9 = (var1.getWeight() * 0.28F * var1.getFatigueMod(var2) * this.getFatigueMod() * var1.getEnduranceMod() * 0.3F + var8) * 0.04F;
            if (var2 instanceof IsoPlayer && var2.isAimAtFloor() && ((IsoPlayer)var2).isDoShove()) {
               var9 *= 2.0F;
            }

            float var10;
            if (var1.isAimedFirearm()) {
               var10 = var12 * 0.7F;
            } else {
               var10 = var12 * 0.15F;
            }

            if (this.getHealth() < var12) {
               var10 = this.getHealth();
            }

            float var11 = var10 / var1.getMaxDamage();
            if (var11 > 1.0F) {
               var11 = 1.0F;
            }

            if (this.isCloseKilled()) {
               var11 = 0.2F;
            }

            if (var1.isUseEndurance()) {
               if (var12 <= 0.0F) {
                  var11 = 1.0F;
               }

               Stats var13 = var2.getStats();
               var13.endurance -= var9 * var11;
            }

            this.hitConsequences(var1, var2, var4, var12, var6);
            return var12;
         }
      } else {
         return 0.0F;
      }
   }

   public float processHitDamage(HandWeapon var1, IsoGameCharacter var2, float var3, boolean var4, float var5) {
      float var6 = var3 * var5;
      float var7 = var6;
      if (var4) {
         var7 = var6 / 2.7F;
      }

      float var8 = var7 * var2.getShovingMod();
      if (var8 > 1.0F) {
         var8 = 1.0F;
      }

      this.setHitForce(var8);
      if (var2.Traits.Strong.isSet() && !var1.isRanged()) {
         this.setHitForce(this.getHitForce() * 1.4F);
      }

      if (var2.Traits.Weak.isSet() && !var1.isRanged()) {
         this.setHitForce(this.getHitForce() * 0.6F);
      }

      float var9 = IsoUtils.DistanceTo(var2.getX(), var2.getY(), this.getX(), this.getY());
      var9 -= var1.getMinRange();
      var9 /= var1.getMaxRange(var2);
      var9 = 1.0F - var9;
      if (var9 > 1.0F) {
         var9 = 1.0F;
      }

      float var10 = var2.stats.endurance;
      var10 *= var2.knockbackAttackMod;
      if (var10 < 0.5F) {
         var10 *= 1.3F;
         if (var10 < 0.4F) {
            var10 = 0.4F;
         }

         this.setHitForce(this.getHitForce() * var10);
      }

      if (!var1.isRangeFalloff()) {
         var9 = 1.0F;
      }

      if (!var1.isShareDamage()) {
         var3 = 1.0F;
      }

      if (var2 instanceof IsoPlayer && !var4) {
         this.setHitForce(this.getHitForce() * 2.0F);
      }

      if (var2 instanceof IsoPlayer && !((IsoPlayer)var2).isDoShove()) {
         Vector2 var11 = tempVector2_1.set(this.getX(), this.getY());
         Vector2 var12 = tempVector2_2.set(var2.getX(), var2.getY());
         var11.x -= var12.x;
         var11.y -= var12.y;
         Vector2 var13 = this.getVectorFromDirection(tempVector2_2);
         var11.normalize();
         float var14 = var11.dot(var13);
         if (var14 > -0.3F) {
            var6 *= 1.5F;
         }
      }

      if (this instanceof IsoPlayer) {
         var6 *= 0.4F;
      } else {
         var6 *= 1.5F;
      }

      int var15 = var2.getWeaponLevel();
      switch (var15) {
         case -1:
            var6 *= 0.3F;
            break;
         case 0:
            var6 *= 0.3F;
            break;
         case 1:
            var6 *= 0.4F;
            break;
         case 2:
            var6 *= 0.5F;
            break;
         case 3:
            var6 *= 0.6F;
            break;
         case 4:
            var6 *= 0.7F;
            break;
         case 5:
            var6 *= 0.8F;
            break;
         case 6:
            var6 *= 0.9F;
            break;
         case 7:
            var6 *= 1.0F;
            break;
         case 8:
            var6 *= 1.1F;
            break;
         case 9:
            var6 *= 1.2F;
            break;
         case 10:
            var6 *= 1.3F;
      }

      if (var2 instanceof IsoPlayer && var2.isAimAtFloor() && !var4 && !((IsoPlayer)var2).isDoShove()) {
         var6 *= Math.max(5.0F, var1.getCritDmgMultiplier());
      }

      if (var2.isCriticalHit() && !var4) {
         var6 *= Math.max(2.0F, var1.getCritDmgMultiplier());
      }

      if (var1.isTwoHandWeapon() && !var2.isItemInBothHands(var1)) {
         var6 *= 0.5F;
      }

      return var6;
   }

   public void hitConsequences(HandWeapon var1, IsoGameCharacter var2, boolean var3, float var4, boolean var5) {
      if (!var3) {
         if (var1.isAimedFirearm()) {
            var4 *= 0.7F;
         } else {
            var4 *= 0.15F;
         }

         CombatManager.getInstance().applyDamage(this, var4);
      }

      if (this.isDead()) {
         if (!this.isOnKillDone() && this.shouldDoInventory()) {
            this.Kill(var2);
         }

         if (this instanceof IsoZombie && ((IsoZombie)this).upKillCount) {
            var2.setZombieKills(var2.getZombieKills() + 1);
         }

      } else {
         if (var1.isSplatBloodOnNoDeath()) {
            this.splatBlood(2, 0.2F);
         }

         if (var1.isKnockBackOnNoDeath()) {
            if (GameServer.bServer) {
               if (var2.xp != null) {
                  GameServer.addXp((IsoPlayer)var2, PerkFactory.Perks.Strength, 2.0F);
               }
            } else if (!GameClient.bClient && var2.xp != null) {
               var2.xp.AddXP(PerkFactory.Perks.Strength, 2.0F);
            }
         }

      }
   }

   public boolean IsAttackRange(float var1, float var2, float var3) {
      float var4 = 1.0F;
      float var5 = 0.0F;
      if (this.leftHandItem != null) {
         InventoryItem var6 = this.leftHandItem;
         if (var6 instanceof HandWeapon) {
            var4 = ((HandWeapon)var6).getMaxRange(this);
            var5 = ((HandWeapon)var6).getMinRange();
            var4 *= ((HandWeapon)this.leftHandItem).getRangeMod(this);
         }
      }

      if (Math.abs(var3 - this.getZ()) > 0.3F) {
         return false;
      } else {
         float var7 = IsoUtils.DistanceTo(var1, var2, this.getX(), this.getY());
         return var7 < var4 && var7 > var5;
      }
   }

   public boolean IsAttackRange(HandWeapon var1, IsoMovingObject var2, Vector3 var3, boolean var4) {
      if (var1 == null) {
         return false;
      } else {
         float var5 = Math.abs(var2.getZ() - this.getZ());
         if (!var1.isRanged() && var5 >= 0.5F) {
            return false;
         } else if (var5 > 3.3F) {
            return false;
         } else {
            float var6 = var1.getMaxRange(this);
            var6 *= var1.getRangeMod(this);
            float var7 = IsoUtils.DistanceToSquared(this.getX(), this.getY(), var3.x, var3.y);
            if (var4) {
               IsoZombie var8 = (IsoZombie)Type.tryCastTo(var2, IsoZombie.class);
               if (var8 != null && var7 < 4.0F && var8.target == this && (var8.isCurrentState(LungeState.instance()) || var8.isCurrentState(LungeNetworkState.instance()))) {
                  ++var6;
               }
            }

            return var7 < var6 * var6;
         }
      }
   }

   public boolean IsSpeaking() {
      return this.chatElement.IsSpeaking();
   }

   public boolean IsSpeakingNPC() {
      return this.chatElement.IsSpeakingNPC();
   }

   public void MoveForward(float var1, float var2, float var3, float var4) {
      if (!this.isCurrentState(SwipeStatePlayer.instance())) {
         this.reqMovement.x = var2;
         this.reqMovement.y = var3;
         this.reqMovement.normalize();
         float var5 = GameTime.instance.getMultiplier();
         this.setNextX(this.getNextX() + var2 * var1 * var5);
         this.setNextY(this.getNextY() + var3 * var1 * var5);
         this.DoFootstepSound(var1);
      }
   }

   protected void pathToAux(float var1, float var2, float var3) {
      boolean var4 = true;
      if (PZMath.fastfloor(var3) == PZMath.fastfloor(this.getZ()) && IsoUtils.DistanceManhatten(var1, var2, this.getX(), this.getY()) <= 30.0F) {
         int var5 = PZMath.fastfloor(var1) / 8;
         int var6 = PZMath.fastfloor(var2) / 8;
         IsoChunk var7 = GameServer.bServer ? ServerMap.instance.getChunk(var5, var6) : IsoWorld.instance.CurrentCell.getChunkForGridSquare(PZMath.fastfloor(var1), PZMath.fastfloor(var2), PZMath.fastfloor(var3));
         if (var7 != null) {
            int var8 = 1;
            if (this instanceof IsoAnimal) {
               var8 &= -2;
            }

            var8 |= 2;
            if (!this.isZombie()) {
               var8 |= 4;
            }

            var4 = !PolygonalMap2.instance.lineClearCollide(this.getX(), this.getY(), var1, var2, PZMath.fastfloor(var3), this.getPathFindBehavior2().getTargetChar(), var8);
         }
      }

      if (var4 && this.current != null && this.current.HasStairs() && !this.current.isSameStaircase(PZMath.fastfloor(var1), PZMath.fastfloor(var2), PZMath.fastfloor(var3))) {
         var4 = false;
      }

      if (var4) {
         this.setVariable("bPathfind", false);
         this.setMoving(true);
      } else {
         this.setVariable("bPathfind", true);
         this.setMoving(false);
      }

   }

   public void pathToCharacter(IsoGameCharacter var1) {
      this.getPathFindBehavior2().pathToCharacter(var1);
      this.pathToAux(var1.getX(), var1.getY(), var1.getZ());
   }

   public void pathToLocation(int var1, int var2, int var3) {
      this.getPathFindBehavior2().pathToLocation(var1, var2, var3);
      this.pathToAux((float)var1 + 0.5F, (float)var2 + 0.5F, (float)var3);
   }

   public void pathToLocationF(float var1, float var2, float var3) {
      this.getPathFindBehavior2().pathToLocationF(var1, var2, var3);
      this.pathToAux(var1, var2, var3);
   }

   public void pathToSound(int var1, int var2, int var3) {
      this.getPathFindBehavior2().pathToSound(var1, var2, var3);
      this.pathToAux((float)var1 + 0.5F, (float)var2 + 0.5F, (float)var3);
   }

   public boolean CanAttack() {
      if (!this.isPerformingAttackAnimation() && !this.getVariableBoolean("IsRacking") && !this.getVariableBoolean("IsUnloading") && StringUtils.isNullOrEmpty(this.getVariableString("RackWeapon"))) {
         if (GameClient.bClient && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer() && (this.isCurrentState(PlayerHitReactionState.instance()) || this.isCurrentState(PlayerHitReactionPVPState.instance()))) {
            return false;
         } else if (this.isSitOnGround()) {
            return false;
         } else {
            InventoryItem var1 = this.leftHandItem;
            if (var1 instanceof HandWeapon && var1.getSwingAnim() != null) {
               this.setUseHandWeapon((HandWeapon)var1);
            }

            if (this.useHandWeapon == null) {
               return true;
            } else if (this.useHandWeapon.getCondition() <= 0) {
               this.setUseHandWeapon((HandWeapon)null);
               if (this.rightHandItem == this.leftHandItem) {
                  this.setSecondaryHandItem((InventoryItem)null);
               }

               this.setPrimaryHandItem((InventoryItem)null);
               if (this.getInventory() != null) {
                  this.getInventory().setDrawDirty(true);
               }

               return false;
            } else {
               float var2 = 12.0F;
               int var3 = this.Moodles.getMoodleLevel(MoodleType.Endurance);
               return !this.useHandWeapon.isCantAttackWithLowestEndurance() || var3 != 4;
            }
         }
      } else {
         return false;
      }
   }

   public void ReduceHealthWhenBurning() {
      if (this.OnFire) {
         if (this.isGodMod()) {
            this.StopBurning();
         } else if (!GameClient.bClient || !this.isZombie() || !(this instanceof IsoZombie) || !((IsoZombie)this).isRemoteZombie()) {
            if (!GameClient.bClient || !(this instanceof IsoPlayer) || !((IsoPlayer)this).bRemote) {
               if (this.isAlive()) {
                  float var1;
                  if (this instanceof IsoPlayer && !(this instanceof IsoAnimal)) {
                     var1 = this.FireKillRate * GameTime.instance.getThirtyFPSMultiplier() * 60.0F;
                     this.getBodyDamage().ReduceGeneralHealth(var1);
                     LuaEventManager.triggerEvent("OnPlayerGetDamage", this, "FIRE", var1);
                     this.getBodyDamage().OnFire(true);
                  } else if (this.isZombie()) {
                     var1 = this.FireKillRate / 20.0F * GameTime.instance.getMultiplier();
                     CombatManager.getInstance().applyDamage(this, var1);
                     this.setAttackedBy((IsoGameCharacter)null);
                  } else {
                     var1 = this.FireKillRate * GameTime.instance.getMultiplier();
                     if (this instanceof IsoAnimal) {
                        var1 -= this.FireKillRate / 10.0F * GameTime.instance.getMultiplier();
                     }

                     CombatManager.getInstance().applyDamage(this, var1);
                  }

                  if (this.isDead()) {
                     IsoFireManager.RemoveBurningCharacter(this);
                     if (this.isZombie()) {
                        LuaEventManager.triggerEvent("OnZombieDead", this);
                        if (GameClient.bClient) {
                           this.setAttackedBy(IsoWorld.instance.CurrentCell.getFakeZombieForHit());
                        }
                     }
                  }
               }

               if (this instanceof IsoPlayer && !(this instanceof IsoAnimal) && Rand.Next(Rand.AdjustForFramerate(((IsoPlayer)this).IsRunning() ? 150 : 400)) == 0) {
                  this.StopBurning();
               }

            }
         }
      }
   }

   /** @deprecated */
   @Deprecated
   public void DrawSneezeText() {
      if (this.getBodyDamage().IsSneezingCoughing() > 0) {
         IsoPlayer var1 = (IsoPlayer)Type.tryCastTo(this, IsoPlayer.class);
         String var2 = null;
         if (this.getBodyDamage().IsSneezingCoughing() == 1) {
            var2 = Translator.getText("IGUI_PlayerText_Sneeze");
            if (var1 != null) {
               var1.playerVoiceSound("SneezeHeavy");
            }
         }

         if (this.getBodyDamage().IsSneezingCoughing() == 2) {
            var2 = Translator.getText("IGUI_PlayerText_Cough");
            if (var1 != null) {
               var1.playerVoiceSound("Cough");
            }
         }

         if (this.getBodyDamage().IsSneezingCoughing() == 3) {
            var2 = Translator.getText("IGUI_PlayerText_SneezeMuffled");
            if (var1 != null) {
               var1.playerVoiceSound("SneezeLight");
            }
         }

         if (this.getBodyDamage().IsSneezingCoughing() == 4) {
            var2 = Translator.getText("IGUI_PlayerText_CoughMuffled");
         }

         float var3 = this.sx;
         float var4 = this.sy;
         var3 = (float)((int)var3);
         var4 = (float)((int)var4);
         var3 -= (float)((int)IsoCamera.getOffX());
         var4 -= (float)((int)IsoCamera.getOffY());
         var4 -= 48.0F;
         if (var2 != null) {
            TextManager.instance.DrawStringCentre(UIFont.Dialogue, (double)((int)var3), (double)((int)var4), var2, (double)this.SpeakColour.r, (double)this.SpeakColour.g, (double)this.SpeakColour.b, (double)this.SpeakColour.a);
         }
      }

   }

   public IsoSpriteInstance getSpriteDef() {
      if (this.def == null) {
         this.def = new IsoSpriteInstance();
      }

      return this.def;
   }

   public void render(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      if (this.doRender) {
         if (!this.isAlphaAndTargetZero()) {
            if (!this.isSeatedInVehicle() || this.getVehicle().showPassenger(this)) {
               if (!this.isSpriteInvisible()) {
                  if (!this.isAlphaZero()) {
                     if (!this.bUseParts && this.def == null) {
                        this.def = new IsoSpriteInstance(this.sprite);
                     }

                     IndieGL.glDepthMask(true);
                     IsoGridSquare var8;
                     if (!PerformanceSettings.FBORenderChunk && this.bDoDefer && var3 - (float)PZMath.fastfloor(var3) > 0.2F) {
                        var8 = this.getCell().getGridSquare(PZMath.fastfloor(var1), PZMath.fastfloor(var2), PZMath.fastfloor(var3) + 1);
                        if (var8 != null) {
                           var8.addDeferredCharacter(this);
                        }
                     }

                     var8 = this.getCurrentSquare();
                     if (PerformanceSettings.LightingFrameSkip < 3 && var8 != null) {
                        var8.interpolateLight(inf, var1 - (float)var8.getX(), var2 - (float)var8.getY());
                     } else {
                        inf.r = var4.r;
                        inf.g = var4.g;
                        inf.b = var4.b;
                        inf.a = var4.a;
                     }

                     if (Core.bDebug && DebugOptions.instance.PathfindRenderWaiting.getValue() && this.hasActiveModel()) {
                        if (this.getCurrentState() == PathFindState.instance() && this.finder.progress == AStarPathFinder.PathFindProgress.notyetfound) {
                           this.legsSprite.modelSlot.model.tintR = 1.0F;
                           this.legsSprite.modelSlot.model.tintG = 0.0F;
                           this.legsSprite.modelSlot.model.tintB = 0.0F;
                        } else {
                           this.legsSprite.modelSlot.model.tintR = 1.0F;
                           this.legsSprite.modelSlot.model.tintG = 1.0F;
                           this.legsSprite.modelSlot.model.tintB = 1.0F;
                        }
                     }

                     if (this.dir == IsoDirections.Max) {
                        this.dir = IsoDirections.N;
                     }

                     lastRenderedRendered = lastRendered;
                     lastRendered = this;
                     this.checkUpdateModelTextures();
                     float var9 = (float)Core.TileScale;
                     float var10 = this.offsetX + 1.0F * var9;
                     float var11 = this.offsetY + -89.0F * var9;
                     if (this.sprite != null) {
                        this.def.setScale(var9, var9);
                        if (!this.bUseParts) {
                           this.sprite.render(this.def, this, var1, var2, var3, this.dir, var10, var11, inf, true);
                        } else if (this.legsSprite.hasActiveModel()) {
                           this.legsSprite.renderActiveModel();
                        } else if (!this.renderTextureInsteadOfModel(var1, var2)) {
                           this.def.Flip = false;
                           inf.r = 1.0F;
                           inf.g = 1.0F;
                           inf.b = 1.0F;
                           inf.a = this.def.alpha * 0.4F;
                           this.legsSprite.renderCurrentAnim(this.def, this, var1, var2, var3, this.dir, var10, var11, inf, false, (Consumer)null);
                        }
                     }

                     int var12;
                     if (this.AttachedAnimSprite != null) {
                        for(var12 = 0; var12 < this.AttachedAnimSprite.size(); ++var12) {
                           IsoSpriteInstance var13 = (IsoSpriteInstance)this.AttachedAnimSprite.get(var12);
                           var13.update();
                           float var14 = inf.a;
                           inf.a = var13.alpha;
                           var13.SetTargetAlpha(this.getTargetAlpha());
                           var13.render(this, var1, var2, var3, this.dir, var10, var11, inf);
                           inf.a = var14;
                        }
                     }

                     for(var12 = 0; var12 < this.inventory.Items.size(); ++var12) {
                        InventoryItem var15 = (InventoryItem)this.inventory.Items.get(var12);
                        if (var15 instanceof IUpdater) {
                           ((IUpdater)var15).render();
                        }
                     }

                     if (this.useRagdoll() && this.getRagdollController() != null) {
                        this.getRagdollController().debugRender();
                     }

                     if (this.ballisticsController != null) {
                        this.ballisticsController.debugRender();
                     }

                     if (this.ballisticsTarget != null) {
                        this.ballisticsTarget.debugRender();
                     }

                  }
               }
            }
         }
      }
   }

   public void renderServerGUI() {
      if (this instanceof IsoPlayer) {
         this.setSceneCulled(false);
      }

      if (this.bUpdateModelTextures && this.hasActiveModel()) {
         this.bUpdateModelTextures = false;
         this.textureCreator = ModelInstanceTextureCreator.alloc();
         this.textureCreator.init(this);
      }

      float var1 = (float)Core.TileScale;
      float var2 = this.offsetX + 1.0F * var1;
      float var3 = this.offsetY + -89.0F * var1;
      if (this.sprite != null) {
         this.def.setScale(var1, var1);
         inf.r = 1.0F;
         inf.g = 1.0F;
         inf.b = 1.0F;
         inf.a = this.def.alpha * 0.4F;
         if (!this.isbUseParts()) {
            this.sprite.render(this.def, this, this.getX(), this.getY(), this.getZ(), this.dir, var2, var3, inf, true);
         } else {
            this.def.Flip = false;
            this.legsSprite.render(this.def, this, this.getX(), this.getY(), this.getZ(), this.dir, var2, var3, inf, true);
         }
      }

      if (Core.bDebug && this.hasActiveModel()) {
         if (this instanceof IsoZombie) {
            int var4 = (int)IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
            int var5 = (int)IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
            TextManager.instance.DrawString((double)var4, (double)var5, "ID: " + this.getOnlineID());
            TextManager.instance.DrawString((double)var4, (double)(var5 + 10), "State: " + this.getCurrentStateName());
            TextManager.instance.DrawString((double)var4, (double)(var5 + 20), "Health: " + this.getHealth());
         }

         Vector2 var6 = tempo;
         this.getDeferredMovement(var6);
         this.drawDirectionLine(var6, 1000.0F * var6.getLength() / GameTime.instance.getMultiplier() * 2.0F, 1.0F, 0.5F, 0.5F);
      }

   }

   protected float getAlphaUpdateRateMul() {
      float var1 = super.getAlphaUpdateRateMul();
      IsoGameCharacter var2 = IsoCamera.getCameraCharacter();
      if (var2.Traits.ShortSighted.isSet()) {
         var1 /= 2.0F;
      }

      if (var2.Traits.EagleEyed.isSet()) {
         var1 *= 1.5F;
      }

      return var1;
   }

   protected boolean isUpdateAlphaEnabled() {
      return !this.isTeleporting();
   }

   protected boolean isUpdateAlphaDuringRender() {
      return false;
   }

   public boolean isSeatedInVehicle() {
      return this.vehicle != null && this.vehicle.getSeat(this) != -1;
   }

   public void renderObjectPicker(float var1, float var2, float var3, ColorInfo var4) {
      if (!this.bUseParts) {
         this.sprite.renderObjectPicker(this.def, this, this.dir);
      } else {
         this.legsSprite.renderObjectPicker(this.def, this, this.dir);
      }

   }

   static Vector2 closestpointonline(double var0, double var2, double var4, double var6, double var8, double var10, Vector2 var12) {
      double var13 = var6 - var2;
      double var15 = var0 - var4;
      double var17 = (var6 - var2) * var0 + (var0 - var4) * var2;
      double var19 = -var15 * var8 + var13 * var10;
      double var21 = var13 * var13 - -var15 * var15;
      double var23;
      double var25;
      if (var21 != 0.0) {
         var23 = (var13 * var17 - var15 * var19) / var21;
         var25 = (var13 * var19 - -var15 * var17) / var21;
      } else {
         var23 = var8;
         var25 = var10;
      }

      return var12.set((float)var23, (float)var25);
   }

   public ShadowParams calculateShadowParams(ShadowParams var1) {
      if (!this.hasAnimationPlayer()) {
         return var1.set(0.45F, 1.4F, 1.125F);
      } else {
         float var10000;
         if (this instanceof IsoAnimal) {
            IsoAnimal var3 = (IsoAnimal)this;
            var10000 = var3.getAnimalSize();
         } else {
            var10000 = 1.0F;
         }

         float var2 = var10000;
         return calculateShadowParams(this.getAnimationPlayer(), var2, false, var1);
      }
   }

   public static ShadowParams calculateShadowParams(AnimationPlayer var0, float var1, boolean var2, ShadowParams var3) {
      float var4 = 0.45F;
      float var5 = 1.4F;
      float var6 = 1.125F;
      if (var0 != null && var0.isReady()) {
         float var7 = 0.0F;
         float var8 = 0.0F;
         float var9 = 0.0F;
         Vector3 var10 = IsoGameCharacter.L_renderShadow.vector3;
         Model.BoneToWorldCoords(var0, var7, var8, var9, var1, var0.getSkinningBoneIndex("Bip01_Head", -1), var10);
         float var11 = var10.x;
         float var12 = var10.y;
         Model.BoneToWorldCoords(var0, var7, var8, var9, var1, var0.getSkinningBoneIndex("Bip01_L_Foot", -1), var10);
         float var13 = var10.x;
         float var14 = var10.y;
         Model.BoneToWorldCoords(var0, var7, var8, var9, var1, var0.getSkinningBoneIndex("Bip01_R_Foot", -1), var10);
         float var15 = var10.x;
         float var16 = var10.y;
         if (var2) {
            Model.BoneToWorldCoords(var0, var7, var8, var9, var1, var0.getSkinningBoneIndex("Bip01_Pelvis", -1), var10);
            var11 -= var10.x;
            var12 -= var10.y;
            var13 -= var10.x;
            var14 -= var10.y;
            var15 -= var10.x;
            var16 -= var10.y;
         }

         Vector3f var17 = IsoGameCharacter.L_renderShadow.vector3f;
         float var18 = 0.0F;
         float var19 = 0.0F;
         Vector3f var20 = IsoGameCharacter.L_renderShadow.forward;
         Vector2 var21 = IsoGameCharacter.L_renderShadow.vector2_1.setLengthAndDirection(var0.getAngle(), 1.0F);
         var20.set(var21.x, var21.y, 0.0F);
         Vector2 var22 = closestpointonline((double)var7, (double)var8, (double)(var7 + var20.x), (double)(var8 + var20.y), (double)var11, (double)var12, IsoGameCharacter.L_renderShadow.vector2_2);
         float var23 = var22.x;
         float var24 = var22.y;
         float var25 = var22.set(var23 - var7, var24 - var8).getLength();
         if (var25 > 0.001F) {
            var17.set(var23 - var7, var24 - var8, 0.0F).normalize();
            if (var20.dot(var17) > 0.0F) {
               var18 = Math.max(var18, var25);
            } else {
               var19 = Math.max(var19, var25);
            }
         }

         var22 = closestpointonline((double)var7, (double)var8, (double)(var7 + var20.x), (double)(var8 + var20.y), (double)var13, (double)var14, IsoGameCharacter.L_renderShadow.vector2_2);
         var23 = var22.x;
         var24 = var22.y;
         var25 = var22.set(var23 - var7, var24 - var8).getLength();
         if (var25 > 0.001F) {
            var17.set(var23 - var7, var24 - var8, 0.0F).normalize();
            if (var20.dot(var17) > 0.0F) {
               var18 = Math.max(var18, var25);
            } else {
               var19 = Math.max(var19, var25);
            }
         }

         var22 = closestpointonline((double)var7, (double)var8, (double)(var7 + var20.x), (double)(var8 + var20.y), (double)var15, (double)var16, IsoGameCharacter.L_renderShadow.vector2_2);
         var23 = var22.x;
         var24 = var22.y;
         var25 = var22.set(var23 - var7, var24 - var8).getLength();
         if (var25 > 0.001F) {
            var17.set(var23 - var7, var24 - var8, 0.0F).normalize();
            if (var20.dot(var17) > 0.0F) {
               var18 = Math.max(var18, var25);
            } else {
               var19 = Math.max(var19, var25);
            }
         }

         var5 = (var18 + 0.35F) * 1.35F;
         var6 = (var19 + 0.35F) * 1.35F;
      }

      return var3.set(var4, var5, var6);
   }

   public void renderShadow(float var1, float var2, float var3) {
      if (Core.getInstance().displayPlayerModel || this.isAnimal() || this.isZombie()) {
         if (!this.isAlphaAndTargetZero()) {
            if (!this.isSeatedInVehicle()) {
               IsoGridSquare var4 = this.getCurrentSquare();
               if (var4 != null) {
                  float var5 = this.getHeightAboveFloor();
                  if (!(var5 > 0.5F)) {
                     int var6 = IsoCamera.frameState.playerIndex;
                     ShadowParams var7 = this.calculateShadowParams(IsoGameCharacter.L_renderShadow.shadowParams);
                     float var8 = var7.w;
                     float var9 = var7.fm;
                     float var10 = var7.bm;
                     float var11 = this.getAlpha(var6);
                     if (var5 > 0.0F) {
                        var11 *= 1.0F - var5 / 0.5F;
                     }

                     if (this.hasActiveModel() && this.hasAnimationPlayer() && this.getAnimationPlayer().isReady()) {
                        float var12 = 0.1F * GameTime.getInstance().getThirtyFPSMultiplier();
                        var12 = PZMath.clamp(var12, 0.0F, 1.0F);
                        if (this.shadowTick != IngameState.instance.numberTicks - 1L) {
                           this.m_shadowFM = var9;
                           this.m_shadowBM = var10;
                        }

                        this.shadowTick = IngameState.instance.numberTicks;
                        this.m_shadowFM = PZMath.lerp(this.m_shadowFM, var9, var12);
                        var9 = this.m_shadowFM;
                        this.m_shadowBM = PZMath.lerp(this.m_shadowBM, var10, var12);
                        var10 = this.m_shadowBM;
                     } else if (this.isZombie() && this.isCurrentState(FakeDeadZombieState.instance())) {
                        var11 = 1.0F;
                     } else if (this.isSceneCulled()) {
                        return;
                     }

                     Vector2 var15 = this.getAnimVector(IsoGameCharacter.L_renderShadow.vector2_1);
                     Vector3f var13 = IsoGameCharacter.L_renderShadow.forward.set(var15.x, var15.y, 0.0F);
                     if (this.getRagdollController() != null) {
                        RagdollStateData var14 = this.getRagdollController().getRagdollStateData();
                        if (var14 != null && var14.isCalculated) {
                           var13.x = var14.simulationDirection.x;
                           var13.y = var14.simulationDirection.y;
                        }
                     }

                     ColorInfo var16 = var4.lighting[var6].lightInfo();
                     if (PerformanceSettings.FBORenderChunk) {
                        FBORenderShadows.getInstance().addShadow(var1, var2, var3 - var5, var13, var8, var9, var10, var16.r, var16.g, var16.b, var11, false);
                     } else {
                        IsoDeadBody.renderShadow(var1, var2, var3 - var5, var13, var8, var9, var10, var16, var11);
                     }
                  }
               }
            }
         }
      }
   }

   public void checkUpdateModelTextures() {
      if (this.bUpdateModelTextures && this.hasActiveModel()) {
         this.bUpdateModelTextures = false;
         this.textureCreator = ModelInstanceTextureCreator.alloc();
         this.textureCreator.init(this);
      }

      if (this.bUpdateEquippedTextures && this.hasActiveModel()) {
         this.bUpdateEquippedTextures = false;
         if (this.primaryHandModel != null && this.primaryHandModel.getTextureInitializer() != null) {
            this.primaryHandModel.getTextureInitializer().setDirty();
         }

         if (this.secondaryHandModel != null && this.secondaryHandModel.getTextureInitializer() != null) {
            this.secondaryHandModel.getTextureInitializer().setDirty();
         }
      }

   }

   public boolean isMaskClicked(int var1, int var2, boolean var3) {
      if (this.sprite == null) {
         return false;
      } else {
         return !this.bUseParts ? super.isMaskClicked(var1, var2, var3) : this.legsSprite.isMaskClicked(this.dir, var1, var2, var3);
      }
   }

   public void setHaloNote(String var1) {
      this.setHaloNote(var1, this.haloDispTime);
   }

   public void setHaloNote(String var1, float var2) {
      this.setHaloNote(var1, 0, 255, 0, var2);
   }

   public void setHaloNote(String var1, int var2, int var3, int var4, float var5) {
      if (this.haloNote != null && var1 != null) {
         this.haloDispTime = var5;
         this.haloNote.setDefaultColors(var2, var3, var4);
         this.haloNote.ReadString(var1);
         this.haloNote.setInternalTickClock(this.haloDispTime);
      }

   }

   public float getHaloTimerCount() {
      return this.haloNote != null ? this.haloNote.getInternalClock() : 0.0F;
   }

   public void DoSneezeText() {
      if (this.getBodyDamage() != null) {
         if (this.getBodyDamage().IsSneezingCoughing() > 0) {
            IsoPlayer var1 = (IsoPlayer)Type.tryCastTo(this, IsoPlayer.class);
            String var2 = null;
            int var3 = 0;
            if (this.getBodyDamage().IsSneezingCoughing() == 1) {
               var2 = Translator.getText("IGUI_PlayerText_Sneeze");
               var3 = Rand.Next(2) + 1;
               this.setVariable("Ext", "Sneeze" + var3);
               if (var1 != null) {
                  var1.playerVoiceSound("SneezeHeavy");
               }
            }

            if (this.getBodyDamage().IsSneezingCoughing() == 2) {
               var2 = Translator.getText("IGUI_PlayerText_Cough");
               this.setVariable("Ext", "Cough");
               if (var1 != null) {
                  var1.playerVoiceSound("Cough");
               }
            }

            if (this.getBodyDamage().IsSneezingCoughing() == 3) {
               var2 = Translator.getText("IGUI_PlayerText_SneezeMuffled");
               var3 = Rand.Next(2) + 1;
               this.setVariable("Ext", "Sneeze" + var3);
               if (var1 != null) {
                  var1.playerVoiceSound("SneezeLight");
               }
            }

            if (this.getBodyDamage().IsSneezingCoughing() == 4) {
               var2 = Translator.getText("IGUI_PlayerText_CoughMuffled");
               this.setVariable("Ext", "Cough");
               if (var1 != null) {
                  var1.playerVoiceSound("Cough");
               }
            }

            if (var2 != null) {
               this.Say(var2);
               this.reportEvent("EventDoExt");
               if (GameClient.bClient && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
                  GameClient.sendSneezingCoughing((IsoPlayer)this, this.getBodyDamage().IsSneezingCoughing(), (byte)var3);
               }
            }
         }

      }
   }

   public String getSayLine() {
      return this.chatElement.getSayLine();
   }

   public void setSayLine(String var1) {
      this.Say(var1);
   }

   public ChatMessage getLastChatMessage() {
      return this.lastChatMessage;
   }

   public void setLastChatMessage(ChatMessage var1) {
      this.lastChatMessage = var1;
   }

   public String getLastSpokenLine() {
      return this.lastSpokenLine;
   }

   public void setLastSpokenLine(String var1) {
      this.lastSpokenLine = var1;
   }

   protected void doSleepSpeech() {
      ++this.sleepSpeechCnt;
      if ((float)this.sleepSpeechCnt > (float)(250 * PerformanceSettings.getLockFPS()) / 30.0F) {
         this.sleepSpeechCnt = 0;
         if (sleepText == null) {
            sleepText = "ZzzZZZzzzz";
            ChatElement.addNoLogText(sleepText);
         }

         this.SayWhisper(sleepText);
      }

   }

   public void SayDebug(String var1) {
      this.chatElement.SayDebug(0, var1);
   }

   public void SayDebug(int var1, String var2) {
      this.chatElement.SayDebug(var1, var2);
   }

   public int getMaxChatLines() {
      return this.chatElement.getMaxChatLines();
   }

   public void Say(String var1) {
      if (!this.isZombie()) {
         this.ProcessSay(var1, this.SpeakColour.r, this.SpeakColour.g, this.SpeakColour.b, 30.0F, 0, "default");
      }
   }

   public void Say(String var1, float var2, float var3, float var4, UIFont var5, float var6, String var7) {
      this.ProcessSay(var1, var2, var3, var4, var6, 0, var7);
   }

   public void SayWhisper(String var1) {
      this.ProcessSay(var1, this.SpeakColour.r, this.SpeakColour.g, this.SpeakColour.b, 10.0F, 0, "whisper");
   }

   public void SayShout(String var1) {
      this.ProcessSay(var1, this.SpeakColour.r, this.SpeakColour.g, this.SpeakColour.b, 60.0F, 0, "shout");
   }

   public void SayRadio(String var1, float var2, float var3, float var4, UIFont var5, float var6, int var7, String var8) {
      this.ProcessSay(var1, var2, var3, var4, var6, var7, var8);
   }

   private void ProcessSay(String var1, float var2, float var3, float var4, float var5, int var6, String var7) {
      if (this.AllowConversation) {
         if (TutorialManager.instance.ProfanityFilter) {
            var1 = ProfanityFilter.getInstance().filterString(var1);
         }

         if (var7.equals("default")) {
            ChatManager.getInstance().showInfoMessage(((IsoPlayer)this).getUsername(), var1);
            this.lastSpokenLine = var1;
         } else if (var7.equals("whisper")) {
            this.lastSpokenLine = var1;
         } else if (var7.equals("shout")) {
            ChatManager.getInstance().sendMessageToChat(((IsoPlayer)this).getUsername(), ChatType.shout, var1);
            this.lastSpokenLine = var1;
         } else if (var7.equals("radio")) {
            UIFont var8 = UIFont.Medium;
            boolean var9 = true;
            boolean var10 = true;
            boolean var11 = true;
            boolean var12 = false;
            boolean var13 = false;
            boolean var14 = true;
            this.chatElement.addChatLine(var1, var2, var3, var4, var8, var5, var7, var9, var10, var11, var12, var13, var14);
            if (ZomboidRadio.isStaticSound(var1)) {
               ChatManager.getInstance().showStaticRadioSound(var1);
            } else {
               ChatManager.getInstance().showRadioMessage(var1, var6);
            }
         }

      }
   }

   public void addLineChatElement(String var1) {
      this.addLineChatElement(var1, 1.0F, 1.0F, 1.0F);
   }

   public void addLineChatElement(String var1, float var2, float var3, float var4) {
      this.addLineChatElement(var1, var2, var3, var4, UIFont.Dialogue, 30.0F, "default");
   }

   public void addLineChatElement(String var1, float var2, float var3, float var4, UIFont var5, float var6, String var7) {
      this.addLineChatElement(var1, var2, var3, var4, var5, var6, var7, false, false, false, false, false, true);
   }

   public void addLineChatElement(String var1, float var2, float var3, float var4, UIFont var5, float var6, String var7, boolean var8, boolean var9, boolean var10, boolean var11, boolean var12, boolean var13) {
      this.chatElement.addChatLine(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12, var13);
   }

   protected boolean playerIsSelf() {
      return IsoPlayer.getInstance() == this;
   }

   public int getUserNameHeight() {
      if (!GameClient.bClient) {
         return 0;
      } else {
         return this.userName != null ? this.userName.getHeight() : 0;
      }
   }

   protected void initTextObjects() {
      this.hasInitTextObjects = true;
      if (this instanceof IsoPlayer) {
         this.chatElement.setMaxChatLines(5);
         if (IsoPlayer.getInstance() != null && !(this instanceof IsoAnimal)) {
            DebugLog.DetailedInfo.trace("FirstNAME:" + IsoPlayer.getInstance().username);
         }

         this.isoPlayer = (IsoPlayer)this;
         if (this.isoPlayer.username != null) {
            this.userName = new TextDrawObject();
            this.userName.setAllowAnyImage(true);
            this.userName.setDefaultFont(UIFont.Small);
            this.userName.setDefaultColors(255, 255, 255, 255);
            this.updateUserName();
         }

         if (this.haloNote == null) {
            this.haloNote = new TextDrawObject();
            this.haloNote.setDefaultFont(UIFont.Small);
            this.haloNote.setDefaultColors(0, 255, 0);
            this.haloNote.setDrawBackground(true);
            this.haloNote.setAllowImages(true);
            this.haloNote.setAllowAnyImage(true);
            this.haloNote.setOutlineColors(0.0F, 0.0F, 0.0F, 0.33F);
         }
      }

   }

   protected void updateUserName() {
      if (this.userName != null && this.isoPlayer != null) {
         String var1 = this.isoPlayer.getUsername(true, true);
         if (this != IsoPlayer.getInstance() && this.isInvisible() && IsoPlayer.getInstance() != null && !IsoPlayer.getInstance().role.haveCapability(Capability.CanSeePlayersStats) && (!Core.bDebug || !DebugOptions.instance.Cheat.Player.SeeEveryone.getValue())) {
            this.userName.ReadString("");
            return;
         }

         Faction var2 = Faction.getPlayerFaction(this.isoPlayer);
         if (var2 != null) {
            if (!this.isoPlayer.showTag && this.isoPlayer != IsoPlayer.getInstance() && Faction.getPlayerFaction(IsoPlayer.getInstance()) != var2) {
               this.isoPlayer.tagPrefix = "";
            } else {
               this.isoPlayer.tagPrefix = var2.getTag();
               if (var2.getTagColor() != null) {
                  this.isoPlayer.setTagColor(var2.getTagColor());
               }
            }
         } else {
            this.isoPlayer.tagPrefix = "";
         }

         IsoGameCharacter var3 = IsoCamera.getCameraCharacter();
         boolean var4 = this.isoPlayer != null && this.isoPlayer.bRemote || Core.getInstance().isShowYourUsername();
         boolean var5 = GameClient.bClient && var3 instanceof IsoPlayer && ((IsoPlayer)var3).role.haveCapability(Capability.CanSeePlayersStats);
         boolean var6 = var3 instanceof IsoPlayer && ((IsoPlayer)var3).canSeeAll;
         if (!ServerOptions.instance.DisplayUserName.getValue() && !ServerOptions.instance.ShowFirstAndLastName.getValue() && !var6) {
            var4 = false;
         }

         if (!var4) {
            var1 = "";
         }

         if (var4 && this.isoPlayer.tagPrefix != null && !this.isoPlayer.tagPrefix.equals("") && (!this.isDisguised() || var5)) {
            var1 = "[col=" + (int)(this.isoPlayer.getTagColor().r * 255.0F) + "," + (int)(this.isoPlayer.getTagColor().g * 255.0F) + "," + (int)(this.isoPlayer.getTagColor().b * 255.0F) + "][" + this.isoPlayer.tagPrefix + "][/] " + var1;
         }

         if (var4 && this.isoPlayer.role != null && this.isoPlayer.role.haveCapability(Capability.CanSeePlayersStats) && this.isoPlayer.isShowAdminTag()) {
            String var10000 = String.format("[col=%d,%d,%d]%s[/] ", (int)(this.isoPlayer.role.getColor().getR() * 255.0F), (int)(this.isoPlayer.role.getColor().getG() * 255.0F), (int)(this.isoPlayer.role.getColor().getB() * 255.0F), this.isoPlayer.role.getName());
            var1 = var10000 + var1;
         }

         if (var4 && this.checkPVP()) {
            String var7 = " [img=media/ui/Skull1.png]";
            if (this.isoPlayer.getSafety().getToggle() == 0.0F) {
               var7 = " [img=media/ui/Skull2.png]";
            }

            var1 = var1 + var7;
         }

         if (this.isoPlayer.isSpeek && !this.isoPlayer.isVoiceMute) {
            var1 = "[img=media/ui/voiceon.png] " + var1;
         }

         if (this.isoPlayer.isVoiceMute) {
            var1 = "[img=media/ui/voicemuted.png] " + var1;
         }

         BaseVehicle var9 = var3 == this.isoPlayer ? this.isoPlayer.getNearVehicle() : null;
         if (this.getVehicle() == null && var9 != null && (this.isoPlayer.getInventory().haveThisKeyId(var9.getKeyId()) != null || var9.isHotwired() || SandboxOptions.getInstance().VehicleEasyUse.getValue())) {
            Color var8 = Color.HSBtoRGB(var9.colorHue, var9.colorSaturation * 0.5F, var9.colorValue);
            int var10 = var8.getRedByte();
            var1 = " [img=media/ui/CarKey.png," + var10 + "," + var8.getGreenByte() + "," + var8.getBlueByte() + "]" + var1;
         }

         if (!var1.equals(this.userName.getOriginal())) {
            this.userName.ReadString(var1);
         }
      }

   }

   private boolean checkPVP() {
      if (this.isoPlayer.getSafety().isEnabled()) {
         return false;
      } else if (!ServerOptions.instance.ShowSafety.getValue()) {
         return false;
      } else if (NonPvpZone.getNonPvpZone(PZMath.fastfloor(this.isoPlayer.getX()), PZMath.fastfloor(this.isoPlayer.getY())) != null) {
         return false;
      } else {
         return IsoPlayer.getInstance() != this.isoPlayer && Faction.isInSameFaction(IsoPlayer.getInstance(), this.isoPlayer) ? this.isoPlayer.isFactionPvp() : true;
      }
   }

   public void updateTextObjects() {
      if (!GameServer.bServer) {
         if (!this.hasInitTextObjects) {
            this.initTextObjects();
         }

         if (!this.Speaking) {
            this.DoSneezeText();
            if (this.isAsleep() && this.getCurrentSquare() != null && this.getCurrentSquare().getCanSee(0)) {
               this.doSleepSpeech();
            }
         }

         if (this.isoPlayer != null) {
            this.radioEquipedCheck();
         }

         this.Speaking = false;
         this.drawUserName = false;
         this.canSeeCurrent = false;
         if (this.haloNote != null && this.haloNote.getInternalClock() > 0.0F) {
            this.haloNote.updateInternalTickClock();
         }

         this.legsSprite.PlayAnim("ZombieWalk1");
         this.chatElement.update();
         this.Speaking = this.chatElement.IsSpeaking();
         if (!this.Speaking || this.isDead()) {
            this.Speaking = false;
            this.callOut = false;
         }

      }
   }

   public void renderlast() {
      super.renderlast();
      int var1 = IsoCamera.frameState.playerIndex;
      float var2 = this.getX();
      float var3 = this.getY();
      if (this.sx == 0.0F && this.def != null) {
         this.sx = IsoUtils.XToScreen(var2 + this.def.offX, var3 + this.def.offY, this.getZ() + this.def.offZ, 0);
         this.sy = IsoUtils.YToScreen(var2 + this.def.offX, var3 + this.def.offY, this.getZ() + this.def.offZ, 0);
         this.sx -= this.offsetX - 8.0F;
         this.sy -= this.offsetY - 60.0F;
      }

      float var4;
      float var5;
      float var6;
      float var24;
      if (this.hasInitTextObjects && this.isoPlayer != null || this.chatElement.getHasChatToDisplay()) {
         var4 = IsoUtils.XToScreen(var2, var3, this.getZ(), 0);
         var5 = IsoUtils.YToScreen(var2, var3, this.getZ(), 0);
         var4 = var4 - IsoCamera.getOffX() - this.offsetX;
         var5 = var5 - IsoCamera.getOffY() - this.offsetY;
         var5 -= (float)(128 / (2 / Core.TileScale));
         var6 = Core.getInstance().getZoom(var1);
         var4 /= var6;
         var5 /= var6;
         var4 += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsX;
         var5 += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsY;
         this.canSeeCurrent = true;
         this.drawUserName = false;
         if (this.isoPlayer != null && (this == IsoCamera.frameState.CamCharacter || this.getCurrentSquare() != null && this.getCurrentSquare().getCanSee(var1)) || IsoPlayer.getInstance().isCanSeeAll()) {
            if (this == IsoPlayer.getInstance()) {
               this.canSeeCurrent = true;
            }

            if (GameClient.bClient && this.userName != null && !(this instanceof IsoAnimal)) {
               this.drawUserName = false;
               if (ServerOptions.getInstance().MouseOverToSeeDisplayName.getValue() && this != IsoPlayer.getInstance() && !IsoPlayer.getInstance().isCanSeeAll()) {
                  IsoObjectPicker.ClickObject var7 = IsoObjectPicker.Instance.ContextPick(Mouse.getXA(), Mouse.getYA());
                  if (var7 != null && var7.tile != null) {
                     for(int var8 = var7.tile.square.getX() - 1; var8 < var7.tile.square.getX() + 2; ++var8) {
                        for(int var9 = var7.tile.square.getY() - 1; var9 < var7.tile.square.getY() + 2; ++var9) {
                           IsoGridSquare var10 = IsoCell.getInstance().getGridSquare(var8, var9, var7.tile.square.getZ());
                           if (var10 != null) {
                              for(int var11 = 0; var11 < var10.getMovingObjects().size(); ++var11) {
                                 IsoMovingObject var12 = (IsoMovingObject)var10.getMovingObjects().get(var11);
                                 if (var12 instanceof IsoPlayer && this == var12) {
                                    this.drawUserName = true;
                                    break;
                                 }
                              }

                              if (this.drawUserName) {
                                 break;
                              }
                           }

                           if (this.drawUserName) {
                              break;
                           }
                        }
                     }
                  }
               } else {
                  this.drawUserName = true;
               }

               if (this.drawUserName) {
                  this.updateUserName();
               }
            }

            if (!GameClient.bClient && this.isoPlayer != null && !this.isAnimal() && this.isoPlayer.getVehicle() == null) {
               String var20 = "";
               BaseVehicle var23 = this.isoPlayer.getNearVehicle();
               if (this.getVehicle() == null && var23 != null && var23.getPartById("Engine") != null && (this.isoPlayer.getInventory().haveThisKeyId(var23.getKeyId()) != null || var23.isHotwired() || SandboxOptions.getInstance().VehicleEasyUse.getValue()) && UIManager.VisibleAllUI) {
                  Color var29 = Color.HSBtoRGB(var23.colorHue, var23.colorSaturation * 0.5F, var23.colorValue, IsoGameCharacter.L_renderLast.color);
                  int var10000 = var29.getRedByte();
                  var20 = " [img=media/ui/CarKey.png," + var10000 + "," + var29.getGreenByte() + "," + var29.getBlueByte() + "]";
               }

               if (!var20.equals("")) {
                  this.userName.ReadString(var20);
                  this.drawUserName = true;
               }
            }
         }

         if (this.isoPlayer != null && this.hasInitTextObjects && (this.playerIsSelf() || this.canSeeCurrent)) {
            if (this.canSeeCurrent && this.drawUserName) {
               var5 -= (float)this.userName.getHeight();
               this.userName.AddBatchedDraw((double)((int)var4), (double)((int)var5), true);
            }

            if (this.playerIsSelf()) {
               ActionProgressBar var21 = UIManager.getProgressBar((double)var1);
               if (var21 != null && var21.isVisible()) {
                  var5 -= (float)(var21.getHeight().intValue() + 2);
               }
            }

            if (this.playerIsSelf() && this.haloNote != null && this.haloNote.getInternalClock() > 0.0F) {
               var24 = this.haloNote.getInternalClock() / (this.haloDispTime / 4.0F);
               var24 = PZMath.min(var24, 1.0F);
               var5 -= (float)(this.haloNote.getHeight() + 2);
               this.haloNote.AddBatchedDraw((double)((int)var4), (double)((int)var5), true, var24);
            }
         }

         boolean var26 = false;
         if (IsoPlayer.getInstance() != this && this.equipedRadio != null && this.equipedRadio.getDeviceData() != null && this.equipedRadio.getDeviceData().getHeadphoneType() >= 0) {
            var26 = true;
         }

         if (this.equipedRadio != null && this.equipedRadio.getDeviceData() != null && !this.equipedRadio.getDeviceData().getIsTurnedOn()) {
            var26 = true;
         }

         IsoGameCharacter var25 = IsoCamera.getCameraCharacter();
         boolean var30 = GameClient.bClient && var25 instanceof IsoPlayer && ((IsoPlayer)var25).role.haveCapability(Capability.CanSeePlayersStats);
         if (!this.m_cheats.m_invisible || this == IsoCamera.frameState.CamCharacter || var30) {
            this.chatElement.renderBatched(IsoPlayer.getPlayerIndex(), (int)var4, (int)var5, var26);
         }
      }

      Vector2 var18;
      AnimationPlayer var22;
      if (Core.bDebug && DebugOptions.instance.Character.Debug.Render.Angle.getValue() && this.hasActiveModel()) {
         var18 = tempo;
         var22 = this.getAnimationPlayer();
         var18.set(this.dir.ToVector());
         this.drawDirectionLine(var18, 2.4F, 0.0F, 1.0F, 0.0F);
         var18.setLengthAndDirection(this.getLookAngleRadians(), 1.0F);
         this.drawDirectionLine(var18, 2.0F, 1.0F, 1.0F, 1.0F);
         var18.setLengthAndDirection(this.getAnimAngleRadians(), 1.0F);
         this.drawDirectionLine(var18, 2.0F, 1.0F, 1.0F, 0.0F);
         var6 = this.getForwardDirection().getDirection();
         var18.setLengthAndDirection(var6, 1.0F);
         this.drawDirectionLine(var18, 2.0F, 0.0F, 0.0F, 1.0F);
      }

      if (Core.bDebug && DebugOptions.instance.Character.Debug.Render.DeferredMovement.getValue() && this.hasActiveModel()) {
         var18 = tempo;
         this.getDeferredMovement(var18);
         this.drawDirectionLine(var18, 1000.0F * var18.getLength() / GameTime.instance.getMultiplier() * 2.0F, 1.0F, 0.5F, 0.5F);
      }

      if (Core.bDebug && DebugOptions.instance.Character.Debug.Render.DeferredMovement.getValue() && this.hasActiveModel()) {
         var18 = tempo;
         this.getDeferredMovementFromRagdoll(var18);
         this.drawDirectionLine(var18, 1000.0F * var18.getLength() / GameTime.instance.getMultiplier() * 2.0F, 0.0F, 1.0F, 0.5F);
      }

      if (Core.bDebug && DebugOptions.instance.Character.Debug.Render.DeferredAngles.getValue() && this.hasActiveModel()) {
         var18 = tempo;
         var22 = this.getAnimationPlayer();
         this.getDeferredMovement(var18);
         this.drawDirectionLine(var18, 1000.0F * var18.getLength() / GameTime.instance.getMultiplier() * 2.0F, 1.0F, 0.5F, 0.5F);
      }

      if (Core.bDebug && DebugOptions.instance.Character.Debug.Render.AimCone.getValue()) {
         this.debugAim();
      }

      if (Core.bDebug && DebugOptions.instance.Character.Debug.Render.TestDotSide.getValue()) {
         this.debugTestDotSide();
      }

      if (Core.bDebug && DebugOptions.instance.Character.Debug.Render.Vision.getValue()) {
         this.debugVision();
      }

      if (Core.bDebug) {
         this.renderDebugData();
      }

      if (this.inventory != null) {
         int var19;
         for(var19 = 0; var19 < this.inventory.Items.size(); ++var19) {
            InventoryItem var27 = (InventoryItem)this.inventory.Items.get(var19);
            if (var27 instanceof IUpdater) {
               ((IUpdater)var27).renderlast();
            }
         }

         if (Core.bDebug && DebugOptions.instance.PathfindRenderPath.getValue() && this.pfb2 != null) {
            this.pfb2.render();
         }

         if (Core.bDebug && DebugOptions.instance.CollideWithObstacles.Render.Radius.getValue()) {
            var4 = 0.3F;
            var5 = 1.0F;
            var6 = 1.0F;
            var24 = 1.0F;
            if (!this.isCollidable()) {
               var24 = 0.0F;
            }

            if (PZMath.fastfloor(this.getZ()) != PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ)) {
               var24 = 0.5F;
               var6 = 0.5F;
               var5 = 0.5F;
            }

            LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), var4, 16, var5, var6, var24, 1.0F);
         }

         IndieGL.glBlendFunc(770, 771);
         if (DebugOptions.instance.Animation.Debug.getValue() && this.hasActiveModel() && !(this instanceof IsoAnimal)) {
            IndieGL.disableDepthTest();
            IndieGL.StartShader(0);
            var19 = (int)IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
            int var33 = (int)IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ() + 1.0F, 0);
            TextManager.instance.DrawString(UIFont.Dialogue, (double)var19, (double)var33, 1.0, this.getAnimationDebug(), 1.0, 1.0, 1.0, 1.0);
         }

         if (this.getIsNPC() && this.ai.brain != null) {
            this.ai.brain.renderlast();
         }

         if (this instanceof IsoPlayer) {
            IsoBulletTracerEffects.getInstance().render();
         }

         if (Core.bDebug && DebugOptions.instance.Character.Debug.Render.CarStopDebug.getValue() && this.hasActiveModel()) {
            var18 = tempo;
            var5 = 0.0F;
            if (this.vehicle != null && this.vbdebugHitTarget != null) {
               var6 = 0.3F;
               var24 = 6.0F;
               Vector2 var28 = this.calcCarForwardVector();
               Vector2 var31 = this.calcCarSpeedVector();
               boolean var32 = this.carMovingBackward(var31);
               Vector2 var34 = this.calcCarPositionOffset(var32);
               Vector2 var35 = this.calcCarToPlayerVector(this.vbdebugHitTarget, var34);
               var31 = this.calcCarSpeedVector(var34);
               float var13 = this.calcLengthMultiplier(var31, var32);
               float var14 = this.calcConeAngleMultiplier(this.vbdebugHitTarget, var32);
               float var15 = this.calcConeAngleOffset(this.vbdebugHitTarget, var32);
               var24 += var13;
               var6 *= var14;
               var5 = var31.getDirection();
               var5 -= var15;
               var18.setLengthAndDirection(var5, var24);
               Vector2 var16 = new Vector2();
               var16.x = this.vehicle.getX() + var34.x;
               var16.y = this.vehicle.getY() + var34.y;
               this.drawLine(var16, var18, 2.0F, 1.0F, 0.0F, 1.0F);
               float var17 = (float)Math.cos((double)var6) * var24;
               var18.setLengthAndDirection(var5 + var6, var17);
               this.drawLine(var16, var18, 2.0F, 1.0F, 0.0F, 0.0F);
               var18.setLengthAndDirection(var5 - var6, var17);
               this.drawLine(var16, var18, 2.0F, 1.0F, 0.0F, 0.0F);
               var5 = var35.getDirection();
               var18.setLengthAndDirection(var5, var35.getLength() / 2.0F);
               this.drawLine(var16, var18, 2.0F, 0.0F, 0.0F, 1.0F);
            }
         }

         if (Core.bDebug && DebugOptions.instance.Character.Debug.Render.AimVector.getValue() && this.ballisticsController != null) {
            this.ballisticsController.renderlast();
         }

      }
   }

   public void drawLine(Vector2 var1, Vector2 var2, float var3, float var4, float var5, float var6) {
      float var7 = var1.x + var2.x * var3;
      float var8 = var1.y + var2.y * var3;
      float var9 = IsoUtils.XToScreenExact(var1.x, var1.y, this.getZ(), 0);
      float var10 = IsoUtils.YToScreenExact(var1.x, var1.y, this.getZ(), 0);
      float var11 = IsoUtils.XToScreenExact(var7, var8, this.getZ(), 0);
      float var12 = IsoUtils.YToScreenExact(var7, var8, this.getZ(), 0);
      LineDrawer.drawLine(var9, var10, var11, var12, var4, var5, var6, 0.5F, 1);
   }

   public Vector2 calcCarForwardVector() {
      Vector3f var1 = new Vector3f();
      this.vehicle.getForwardVector(var1);
      Vector2 var2 = new Vector2();
      var2.x = var1.x;
      var2.y = var1.z;
      return var2;
   }

   public boolean carMovingBackward(Vector2 var1) {
      return this.calcCarForwardVector().dot(var1) < 0.0F;
   }

   public Vector2 calcCarPositionOffset(boolean var1) {
      new Vector2();
      Vector2 var2;
      if (!var1) {
         var2 = this.calcCarForwardVector().setLength(1.0F);
         var2.x *= -1.0F;
         var2.y *= -1.0F;
      } else {
         var2 = this.calcCarForwardVector().setLength(2.0F);
      }

      return var2;
   }

   public float calcLengthMultiplier(Vector2 var1, boolean var2) {
      float var3 = 0.0F;
      if (var2) {
         var3 = var1.getLength();
      } else {
         var3 = var1.getLength();
      }

      if (var3 < 1.0F) {
         var3 = 1.0F;
      }

      return var3;
   }

   public Vector2 calcCarSpeedVector(Vector2 var1) {
      Vector2 var2 = this.calcCarSpeedVector();
      var2.x -= var1.x;
      var2.y -= var1.y;
      return var2;
   }

   public Vector2 calcCarSpeedVector() {
      Vector2 var1 = new Vector2();
      Vector3f var2 = new Vector3f();
      var1.x = this.vehicle.getLinearVelocity(var2).x;
      var1.y = this.vehicle.getLinearVelocity(var2).z;
      return var1;
   }

   public Vector2 calcCarToPlayerVector(IsoGameCharacter var1, Vector2 var2) {
      Vector2 var3 = new Vector2();
      var3.x = var1.getX() - this.vehicle.getX();
      var3.y = var1.getY() - this.vehicle.getY();
      var3.x -= var2.x;
      var3.y -= var2.y;
      return var3;
   }

   public Vector2 calcCarToPlayerVector(IsoGameCharacter var1) {
      Vector2 var2 = new Vector2();
      var2.x = var1.getX() - this.vehicle.getX();
      var2.y = var1.getY() - this.vehicle.getY();
      return var2;
   }

   public float calcConeAngleOffset(IsoGameCharacter var1, boolean var2) {
      float var3 = 0.0F;
      if (var2 && (this.vehicle.getCurrentSteering() > 0.1F || this.vehicle.getCurrentSteering() < -0.1F)) {
         var3 = this.vehicle.getCurrentSteering() * 0.3F;
      }

      if (!var2 && (this.vehicle.getCurrentSteering() > 0.1F || this.vehicle.getCurrentSteering() < -0.1F)) {
         var3 = this.vehicle.getCurrentSteering() * 0.3F;
      }

      return var3;
   }

   public float calcConeAngleMultiplier(IsoGameCharacter var1, boolean var2) {
      float var3 = 0.0F;
      if (var2 && (this.vehicle.getCurrentSteering() > 0.1F || this.vehicle.getCurrentSteering() < -0.1F)) {
         var3 = this.vehicle.getCurrentSteering() * 3.0F;
      }

      if (!var2 && (this.vehicle.getCurrentSteering() > 0.1F || this.vehicle.getCurrentSteering() < -0.1F)) {
         var3 = this.vehicle.getCurrentSteering() * 2.0F;
      }

      if (this.vehicle.getCurrentSteering() < 0.0F) {
         var3 *= -1.0F;
      }

      if (var3 < 1.0F) {
         var3 = 1.0F;
      }

      return var3;
   }

   protected boolean renderTextureInsteadOfModel(float var1, float var2) {
      return false;
   }

   public void drawDirectionLine(Vector2 var1, float var2, float var3, float var4, float var5) {
      float var6 = this.getX() + var1.x * var2;
      float var7 = this.getY() + var1.y * var2;
      float var8 = IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
      float var9 = IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
      float var10 = IsoUtils.XToScreenExact(var6, var7, this.getZ(), 0);
      float var11 = IsoUtils.YToScreenExact(var6, var7, this.getZ(), 0);
      SpriteRenderer.instance.StartShader(0, IsoCamera.frameState.playerIndex);
      IndieGL.disableDepthTest();
      LineDrawer.drawLine(var8, var9, var10, var11, var3, var4, var5, 0.5F, 1);
   }

   public void drawDebugTextBelow(String var1) {
      int var2 = TextManager.instance.MeasureStringX(UIFont.Small, var1) + 32;
      int var3 = TextManager.instance.MeasureStringY(UIFont.Small, var1);
      int var4 = (int)Math.ceil((double)var3 * 1.25);
      float var5 = IsoUtils.XToScreenExact(this.getX() + 0.25F, this.getY() + 0.25F, this.getZ(), 0);
      float var6 = IsoUtils.YToScreenExact(this.getX() + 0.25F, this.getY() + 0.25F, this.getZ(), 0);
      IndieGL.glBlendFunc(770, 771);
      SpriteRenderer.instance.StartShader(0, IsoCamera.frameState.playerIndex);
      SpriteRenderer.instance.renderi((Texture)null, (int)(var5 - (float)(var2 / 2)), (int)(var6 - (float)((var4 - var3) / 2)), var2, var4, 0.0F, 0.0F, 0.0F, 0.5F, (Consumer)null);
      TextManager.instance.DrawStringCentre(UIFont.Small, (double)var5, (double)var6, var1, 1.0, 1.0, 1.0, 1.0);
      SpriteRenderer.instance.EndShader();
   }

   public Radio getEquipedRadio() {
      return this.equipedRadio;
   }

   private void radioEquipedCheck() {
      if (this.leftHandItem != this.leftHandCache) {
         this.leftHandCache = this.leftHandItem;
         if (this.leftHandItem != null && (this.equipedRadio == null || this.equipedRadio != this.rightHandItem) && this.leftHandItem instanceof Radio) {
            this.equipedRadio = (Radio)this.leftHandItem;
         } else if (this.equipedRadio != null && this.equipedRadio != this.rightHandItem && this.equipedRadio != this.getClothingItem_Back()) {
            if (this.equipedRadio.getDeviceData() != null) {
               this.equipedRadio.getDeviceData().cleanSoundsAndEmitter();
            }

            this.equipedRadio = null;
         }
      }

      if (this.rightHandItem != this.rightHandCache) {
         this.rightHandCache = this.rightHandItem;
         if (this.rightHandItem != null && this.rightHandItem instanceof Radio) {
            this.equipedRadio = (Radio)this.rightHandItem;
         } else if (this.equipedRadio != null && this.equipedRadio != this.leftHandItem && this.equipedRadio != this.getClothingItem_Back()) {
            if (this.equipedRadio.getDeviceData() != null) {
               this.equipedRadio.getDeviceData().cleanSoundsAndEmitter();
            }

            this.equipedRadio = null;
         }
      }

      if (this.getClothingItem_Back() != this.backCache) {
         this.backCache = this.getClothingItem_Back();
         if (this.getClothingItem_Back() != null && this.getClothingItem_Back() instanceof Radio) {
            this.equipedRadio = (Radio)this.getClothingItem_Back();
         } else if (this.equipedRadio != null && this.equipedRadio != this.leftHandItem && this.equipedRadio != this.rightHandItem) {
            if (this.equipedRadio.getDeviceData() != null) {
               this.equipedRadio.getDeviceData().cleanSoundsAndEmitter();
            }

            this.equipedRadio = null;
         }
      }

   }

   private void debugAim() {
      if (this instanceof IsoPlayer var1) {
         if (var1.IsAiming()) {
            HandWeapon var2 = (HandWeapon)Type.tryCastTo(this.getPrimaryHandItem(), HandWeapon.class);
            if (var2 == null) {
               var2 = var1.bareHands;
            }

            float var3 = var2.getMaxRange(var1) * var2.getRangeMod(var1);
            float var4 = var2.getMinRange();
            float var5 = PZMath.min(var3 + 1.0F, 2.0F);
            float var6 = this.getLookAngleRadians();
            IndieGL.disableDepthTest();
            IndieGL.StartShader(0);
            float var13;
            if (this.ballisticsController != null) {
               Color var7 = Color.white;
               Vector3 var8 = this.ballisticsController.getMuzzlePosition();
               float var9 = var8.x + (float)Math.cos((double)var6) * var3;
               float var10 = var8.y + (float)Math.sin((double)var6) * var3;
               float var11 = (float)Math.cos((double)var6);
               float var12 = (float)Math.sin((double)var6);
               var13 = this.getZ();
               GeometryUtils.perpendicular(var11, var12, var13, tempVector3f00, tempVector3f01);
               tempVector3f00.mul(CombatManager.BallisticsControllerDistanceThreshold);
               LineDrawer.DrawIsoLine(var8.x, var8.y, var8.z, var9, var10, var8.z, var7.r, var7.g, var7.b, 1.0F, 1);
               LineDrawer.drawDirectionLine(var8.x + tempVector3f00.x, var8.y + tempVector3f00.y, var8.z + tempVector3f00.z, var3, var6, var7.r, var7.g, var7.b, 1.0F, 1);
               LineDrawer.drawDirectionLine(var8.x - tempVector3f00.x, var8.y - tempVector3f00.y, var8.z - tempVector3f00.z, var3, var6, var7.r, var7.g, var7.b, 1.0F, 1);
               LineDrawer.DrawIsoLine(var8.x - tempVector3f00.x, var8.y - tempVector3f00.y, var8.z - tempVector3f00.z, var8.x + tempVector3f00.x, var8.y + tempVector3f00.y, var8.z + tempVector3f00.z, var7.r, var7.g, var7.b, 1.0F, 1);
               LineDrawer.DrawIsoLine(var9 - tempVector3f00.x, var10 - tempVector3f00.y, var8.z - tempVector3f00.z, var9 + tempVector3f00.x, var10 + tempVector3f00.y, var8.z + tempVector3f00.z, var7.r, var7.g, var7.b, 1.0F, 1);
               LineDrawer.DrawIsoLine(var8.x, var8.y, var13, var9, var10, var13, var7.r, var7.g, var7.b, 1.0F, 1);
               LineDrawer.drawDirectionLine(var8.x + tempVector3f00.x, var8.y + tempVector3f00.y, var13 + tempVector3f00.z, var3, var6, var7.r, var7.g, var7.b, 1.0F, 1);
               LineDrawer.drawDirectionLine(var8.x - tempVector3f00.x, var8.y - tempVector3f00.y, var13 - tempVector3f00.z, var3, var6, var7.r, var7.g, var7.b, 1.0F, 1);
               LineDrawer.DrawIsoLine(var8.x - tempVector3f00.x, var8.y - tempVector3f00.y, var13 - tempVector3f00.z, var8.x + tempVector3f00.x, var8.y + tempVector3f00.y, var13 + tempVector3f00.z, var7.r, var7.g, var7.b, 1.0F, 1);
               LineDrawer.DrawIsoLine(var9 - tempVector3f00.x, var10 - tempVector3f00.y, var13 - tempVector3f00.z, var9 + tempVector3f00.x, var10 + tempVector3f00.y, var13 + tempVector3f00.z, var7.r, var7.g, var7.b, 1.0F, 1);
            } else {
               float var27 = var2.getMinAngle();
               var27 -= var2.getAimingPerkMinAngleModifier() * ((float)this.getPerkLevel(PerkFactory.Perks.Aiming) / 2.0F);
               LineDrawer.drawDirectionLine(this.getX(), this.getY(), this.getZ(), var3, var6, 1.0F, 1.0F, 1.0F, 0.5F, 1);
               LineDrawer.drawDotLines(this.getX(), this.getY(), this.getZ(), var3, var6, var27, 1.0F, 1.0F, 1.0F, 0.5F, 1);
               LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), var4, var6, var27, 6, 1.0F, 1.0F, 1.0F, 0.5F);
               if (var4 != var3) {
                  LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), var3, var6, var27, 6, 1.0F, 1.0F, 1.0F, 0.5F);
               }

               LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), var5, var6, var27, 6, 0.75F, 0.75F, 0.75F, 0.5F);
               float var28 = Core.getInstance().getIgnoreProneZombieRange();
               if (var28 > 0.0F) {
                  LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), var28, var6, 0.0F, 12, 0.0F, 0.0F, 1.0F, 0.25F);
                  LineDrawer.drawDotLines(this.getX(), this.getY(), this.getZ(), var28, var6, 0.0F, 0.0F, 0.0F, 1.0F, 0.25F, 1);
               }

               HitInfo var29;
               if (this.attackVars.targetOnGround.getMovingObject() != null) {
                  var29 = (HitInfo)this.attackVars.targetsProne.get(0);
                  LineDrawer.DrawIsoCircle(var29.x, var29.y, var29.z, 0.1F, 8, 1.0F, 1.0F, 0.0F, 1.0F);
               } else if (!this.attackVars.targetsStanding.isEmpty()) {
                  var29 = (HitInfo)this.attackVars.targetsStanding.get(0);
                  LineDrawer.DrawIsoCircle(var29.x, var29.y, var29.z, 0.1F, 8, 1.0F, 1.0F, 0.0F, 1.0F);
               }

               for(int var30 = 0; var30 < this.hitInfoList.size(); ++var30) {
                  HitInfo var31 = (HitInfo)this.hitInfoList.get(var30);
                  IsoMovingObject var32 = var31.getObject();
                  if (var32 != null) {
                     int var33 = var31.chance;
                     var13 = 1.0F - (float)var33 / 100.0F;
                     float var14 = 1.0F - var13;
                     float var15 = Math.max(0.2F, (float)var33 / 100.0F) / 2.0F;
                     float var16 = IsoUtils.XToScreenExact(var32.getX() - var15, var32.getY() + var15, var32.getZ(), 0);
                     float var17 = IsoUtils.YToScreenExact(var32.getX() - var15, var32.getY() + var15, var32.getZ(), 0);
                     float var18 = IsoUtils.XToScreenExact(var32.getX() - var15, var32.getY() - var15, var32.getZ(), 0);
                     float var19 = IsoUtils.YToScreenExact(var32.getX() - var15, var32.getY() - var15, var32.getZ(), 0);
                     float var20 = IsoUtils.XToScreenExact(var32.getX() + var15, var32.getY() - var15, var32.getZ(), 0);
                     float var21 = IsoUtils.YToScreenExact(var32.getX() + var15, var32.getY() - var15, var32.getZ(), 0);
                     float var22 = IsoUtils.XToScreenExact(var32.getX() + var15, var32.getY() + var15, var32.getZ(), 0);
                     float var23 = IsoUtils.YToScreenExact(var32.getX() + var15, var32.getY() + var15, var32.getZ(), 0);
                     SpriteRenderer.instance.renderPoly(var16, var17, var18, var19, var20, var21, var22, var23, var13, var14, 0.0F, 0.5F);
                     UIFont var24 = UIFont.Dialogue;
                     TextManager.instance.DrawStringCentre(var24, (double)var22, (double)var23, String.valueOf(var31.dot), 1.0, 1.0, 1.0, 1.0);
                     TextManager.instance.DrawStringCentre(var24, (double)var22, (double)(var23 + (float)TextManager.instance.getFontHeight(var24)), var31.chance + "%", 1.0, 1.0, 1.0, 1.0);
                     var13 = 1.0F;
                     var14 = 1.0F;
                     float var25 = 1.0F;
                     float var26 = PZMath.sqrt(var31.distSq);
                     if (var26 < var2.getMinRange()) {
                        var25 = 0.0F;
                        var13 = 0.0F;
                     }

                     TextManager.instance.DrawStringCentre(var24, (double)var22, (double)(var23 + (float)(TextManager.instance.getFontHeight(var24) * 2)), "DIST: " + var26, (double)var13, (double)var14, (double)var25, 1.0);
                  }

                  if (var31.window.getObject() != null) {
                     var31.window.getObject().setHighlighted(true);
                  }
               }
            }

         }
      }
   }

   private void debugTestDotSide() {
      if (this == IsoPlayer.getInstance()) {
         float var1 = this.getLookAngleRadians();
         float var2 = 2.0F;
         float var3 = 0.7F;
         LineDrawer.drawDotLines(this.getX(), this.getY(), this.getZ(), var2, var1, var3, 1.0F, 1.0F, 1.0F, 0.5F, 1);
         var3 = -0.5F;
         LineDrawer.drawDotLines(this.getX(), this.getY(), this.getZ(), var2, var1, var3, 1.0F, 1.0F, 1.0F, 0.5F, 1);
         LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), var2, var1, -1.0F, 16, 1.0F, 1.0F, 1.0F, 0.5F);
         ArrayList var4 = this.getCell().getZombieList();

         for(int var5 = 0; var5 < var4.size(); ++var5) {
            IsoMovingObject var6 = (IsoMovingObject)var4.get(var5);
            if (this.DistToSquared(var6) < var2 * var2) {
               LineDrawer.DrawIsoCircle(var6.getX(), var6.getY(), var6.getZ(), 0.3F, 1.0F, 1.0F, 1.0F, 1.0F);
               float var7 = 0.2F;
               float var8 = IsoUtils.XToScreenExact(var6.getX() + var7, var6.getY() + var7, var6.getZ(), 0);
               float var9 = IsoUtils.YToScreenExact(var6.getX() + var7, var6.getY() + var7, var6.getZ(), 0);
               UIFont var10 = UIFont.DebugConsole;
               int var11 = TextManager.instance.getFontHeight(var10);
               TextManager.instance.DrawStringCentre(var10, (double)var8, (double)(var9 + (float)var11), "SIDE: " + this.testDotSide(var6), 1.0, 1.0, 1.0, 1.0);
               Vector2 var12 = this.getLookVector(tempo2);
               Vector2 var13 = tempo.set(var6.getX() - this.getX(), var6.getY() - this.getY());
               var13.normalize();
               float var14 = PZMath.wrap(var13.getDirection() - var12.getDirection(), 0.0F, 6.2831855F);
               TextManager.instance.DrawStringCentre(var10, (double)var8, (double)(var9 + (float)(var11 * 2)), "ANGLE (0-360): " + PZMath.radToDeg(var14), 1.0, 1.0, 1.0, 1.0);
               var14 = (float)Math.acos((double)this.getDotWithForwardDirection(var6.getX(), var6.getY()));
               TextManager.instance.DrawStringCentre(var10, (double)var8, (double)(var9 + (float)(var11 * 3)), "ANGLE (0-180): " + PZMath.radToDeg(var14), 1.0, 1.0, 1.0, 1.0);
            }
         }

      }
   }

   private void debugVision() {
      if (this == IsoPlayer.getInstance()) {
         float var1 = LightingJNI.calculateVisionCone(this);
         LineDrawer.drawDotLines(this.getX(), this.getY(), this.getZ(), GameTime.getInstance().getViewDist(), this.getLookAngleRadians(), -var1, 1.0F, 1.0F, 1.0F, 0.5F, 1);
         LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), GameTime.getInstance().getViewDist(), this.getLookAngleRadians(), -var1, 16, 1.0F, 1.0F, 1.0F, 0.5F);
         float var2 = LightingJNI.calculateRearZombieDistance(this);
         LineDrawer.drawArc(this.getX(), this.getY(), this.getZ(), var2, this.getLookAngleRadians(), -1.0F, 32, 1.0F, 1.0F, 1.0F, 0.5F);
      }
   }

   public void setDefaultState() {
      this.stateMachine.changeState(this.defaultState, (Iterable)null);
   }

   public void SetOnFire() {
      if (!this.OnFire) {
         this.setOnFire(true);
         float var1 = (float)Core.TileScale;
         this.AttachAnim("Fire", "01", 4, IsoFireManager.FireAnimDelay, (int)(-(this.offsetX + 1.0F * var1)) + (8 - Rand.Next(16)), (int)(-(this.offsetY + -89.0F * var1)) + (int)((float)(10 + Rand.Next(20)) * var1), true, 0, false, 0.7F, IsoFireManager.FireTintMod);
         IsoFireManager.AddBurningCharacter(this);
         int var2 = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.MAX));
         if (this instanceof IsoPlayer) {
            ((BodyPart)this.getBodyDamage().getBodyParts().get(var2)).setBurned();
         }

         if (var1 == 2.0F) {
            int var3 = this.AttachedAnimSprite.size() - 1;
            ((IsoSpriteInstance)this.AttachedAnimSprite.get(var3)).setScale(var1, var1);
         }

         if (!this.getEmitter().isPlaying("BurningFlesh")) {
            this.getEmitter().playSoundImpl("BurningFlesh", this);
         }

      }
   }

   public void StopBurning() {
      if (this.OnFire) {
         IsoFireManager.RemoveBurningCharacter(this);
         this.setOnFire(false);
         if (this.AttachedAnimSprite != null) {
            this.AttachedAnimSprite.clear();
         }

         this.getEmitter().stopOrTriggerSoundByName("BurningFlesh");
      }
   }

   public void SpreadFireMP() {
      if (this.OnFire && GameServer.bServer && SandboxOptions.instance.FireSpread.getValue()) {
         IsoGridSquare var1 = ServerMap.instance.getGridSquare((int)this.getX(), (int)this.getY(), (int)this.getZ());
         if (var1 != null && !var1.getProperties().Is(IsoFlagType.burning) && Rand.Next(Rand.AdjustForFramerate(3000)) < this.FireSpreadProbability) {
            IsoFireManager.StartFire(this.getCell(), var1, false, 80);
         }

      }
   }

   public void SpreadFire() {
      if (this.OnFire && !GameServer.bServer && !GameClient.bClient && SandboxOptions.instance.FireSpread.getValue()) {
         if (this.square != null && !this.square.getProperties().Is(IsoFlagType.burning) && Rand.Next(Rand.AdjustForFramerate(3000)) < this.FireSpreadProbability) {
            IsoFireManager.StartFire(this.getCell(), this.square, false, 80);
         }

      }
   }

   public void Throw(HandWeapon var1) {
      if (this instanceof IsoPlayer && ((IsoPlayer)this).getJoypadBind() != -1) {
         Vector2 var2 = tempo.set(this.m_forwardDirection);
         var2.setLength(var1.getMaxRange());
         this.attackTargetSquare = this.getCell().getGridSquare((double)(this.getX() + var2.getX()), (double)(this.getY() + var2.getY()), (double)this.getZ());
         if (this.attackTargetSquare == null) {
            this.attackTargetSquare = this.getCell().getGridSquare((double)(this.getX() + var2.getX()), (double)(this.getY() + var2.getY()), 0.0);
         }
      }

      float var5 = (float)this.attackTargetSquare.getX() - this.getX();
      if (var5 > 0.0F) {
         if ((float)this.attackTargetSquare.getX() - this.getX() > var1.getMaxRange()) {
            var5 = var1.getMaxRange();
         }
      } else if ((float)this.attackTargetSquare.getX() - this.getX() < -var1.getMaxRange()) {
         var5 = -var1.getMaxRange();
      }

      float var3 = (float)this.attackTargetSquare.getY() - this.getY();
      if (var3 > 0.0F) {
         if ((float)this.attackTargetSquare.getY() - this.getY() > var1.getMaxRange()) {
            var3 = var1.getMaxRange();
         }
      } else if ((float)this.attackTargetSquare.getY() - this.getY() < -var1.getMaxRange()) {
         var3 = -var1.getMaxRange();
      }

      if (var1.getPhysicsObject().equals("Ball")) {
         new IsoBall(this.getCell(), this.getX(), this.getY(), this.getZ() + 0.6F, var5 * 0.4F, var3 * 0.4F, var1, this);
      } else {
         new IsoMolotovCocktail(this.getCell(), this.getX(), this.getY(), this.getZ() + 0.6F, var5 * 0.4F, var3 * 0.4F, var1, this);
      }

      this.setPrimaryHandItem((InventoryItem)null);
      if (this instanceof IsoPlayer) {
         ((IsoPlayer)this).setAttackAnimThrowTimer(0L);
      }

   }

   public boolean helmetFall(boolean var1) {
      if (GameClient.bClient) {
         return false;
      } else if (GameServer.bServer) {
         return GameServer.helmetFall(this, var1);
      } else {
         IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(this, IsoPlayer.class);
         boolean var3 = false;
         IsoZombie var4 = (IsoZombie)Type.tryCastTo(this, IsoZombie.class);
         int var5;
         if (var4 != null && !var4.isUsingWornItems()) {
            this.getItemVisuals(tempItemVisuals);

            for(var5 = 0; var5 < tempItemVisuals.size(); ++var5) {
               ItemVisual var11 = (ItemVisual)tempItemVisuals.get(var5);
               Item var12 = var11.getScriptItem();
               if (var12 != null && var12.getType() == Item.Type.Clothing && var12.getChanceToFall() > 0) {
                  int var13 = var12.getChanceToFall();
                  if (var1) {
                     var13 += 40;
                  }

                  if (Rand.Next(100) > var13) {
                     InventoryItem var14 = InventoryItemFactory.CreateItem(var12.getFullName());
                     if (var14 != null) {
                        if (var14.getVisual() != null) {
                           var14.getVisual().copyFrom(var11);
                           var14.synchWithVisual();
                        }

                        new IsoFallingClothing(this.getCell(), this.getX(), this.getY(), PZMath.min(this.getZ() + 0.4F, (float)PZMath.fastfloor(this.getZ()) + 0.95F), 0.2F, 0.2F, var14);
                        tempItemVisuals.remove(var5--);
                        var4.itemVisuals.clear();
                        var4.itemVisuals.addAll(tempItemVisuals);
                        this.resetModelNextFrame();
                        this.onWornItemsChanged();
                        var3 = true;
                     }
                  }
               }
            }
         } else if (this.getWornItems() != null && !this.getWornItems().isEmpty()) {
            for(var5 = 0; var5 < this.getWornItems().size(); ++var5) {
               WornItem var6 = this.getWornItems().get(var5);
               InventoryItem var7 = var6.getItem();
               String var8 = var6.getLocation();
               if (var7 instanceof Clothing) {
                  int var9 = ((Clothing)var7).getChanceToFall();
                  if (var1) {
                     var9 += 40;
                  }

                  if (((Clothing)var7).getChanceToFall() > 0 && Rand.Next(100) <= var9) {
                     new IsoFallingClothing(this.getCell(), this.getX(), this.getY(), PZMath.min(this.getZ() + 0.4F, (float)((int)this.getZ()) + 0.95F), Rand.Next(-0.2F, 0.2F), Rand.Next(-0.2F, 0.2F), var7);
                     this.getInventory().Remove(var7);
                     this.getWornItems().remove(var7);
                     this.resetModelNextFrame();
                     this.onWornItemsChanged();
                     var3 = true;
                     if (GameClient.bClient && var2 != null && var2.isLocalPlayer()) {
                        INetworkPacket.send(PacketTypes.PacketType.SyncClothing, var2);
                     }
                  }
               }
            }
         }

         if (var3 && var2 != null && var2.isLocalPlayer()) {
            LuaEventManager.triggerEvent("OnClothingUpdated", this);
         }

         if (var3 && this.isZombie()) {
            PersistentOutfits.instance.setFallenHat(this, true);
         }

         return var3;
      }
   }

   public void smashCarWindow(VehiclePart var1) {
      HashMap var2 = this.getStateMachineParams(SmashWindowState.instance());
      var2.clear();
      var2.put(0, var1.getWindow());
      var2.put(1, var1.getVehicle());
      var2.put(2, var1);
      this.actionContext.reportEvent("EventSmashWindow");
   }

   public void smashWindow(IsoWindow var1) {
      if (!var1.isInvincible()) {
         HashMap var2 = this.getStateMachineParams(SmashWindowState.instance());
         var2.clear();
         var2.put(0, var1);
         this.actionContext.reportEvent("EventSmashWindow");
      }

   }

   public void openWindow(IsoWindow var1) {
      if (!var1.isInvincible()) {
         OpenWindowState.instance().setParams(this, var1);
         this.actionContext.reportEvent("EventOpenWindow");
      }

   }

   public void closeWindow(IsoWindow var1) {
      if (!var1.isInvincible()) {
         HashMap var2 = this.getStateMachineParams(CloseWindowState.instance());
         var2.clear();
         var2.put(0, var1);
         this.actionContext.reportEvent("EventCloseWindow");
      }

   }

   public void climbThroughWindow(IsoWindow var1) {
      if (var1.canClimbThrough(this)) {
         this.dropHeavyItems();
         float var2 = this.getX() - (float)PZMath.fastfloor(this.getX());
         float var3 = this.getY() - (float)PZMath.fastfloor(this.getY());
         byte var4 = 0;
         byte var5 = 0;
         if (var1.getX() > this.getX() && !var1.north) {
            var4 = -1;
         }

         if (var1.getY() > this.getY() && var1.north) {
            var5 = -1;
         }

         this.setX(var1.getX() + var2 + (float)var4);
         this.setY(var1.getY() + var3 + (float)var5);
         ClimbThroughWindowState.instance().setParams(this, var1);
         this.actionContext.reportEvent("EventClimbWindow");
      }

   }

   public void climbThroughWindow(IsoWindow var1, Integer var2) {
      if (var1.canClimbThrough(this)) {
         this.dropHeavyItems();
         ClimbThroughWindowState.instance().setParams(this, var1);
         this.actionContext.reportEvent("EventClimbWindow");
      }

   }

   public boolean isClosingWindow(IsoWindow var1) {
      if (var1 == null) {
         return false;
      } else if (!this.isCurrentState(CloseWindowState.instance())) {
         return false;
      } else {
         return CloseWindowState.instance().getWindow(this) == var1;
      }
   }

   public boolean isClimbingThroughWindow(IsoWindow var1) {
      if (var1 == null) {
         return false;
      } else if (!this.isCurrentState(ClimbThroughWindowState.instance())) {
         return false;
      } else if (!this.getVariableBoolean("BlockWindow")) {
         return false;
      } else {
         return ClimbThroughWindowState.instance().getWindow(this) == var1;
      }
   }

   public void climbThroughWindowFrame(IsoWindowFrame var1) {
      if (var1.canClimbThrough(this)) {
         this.dropHeavyItems();
         ClimbThroughWindowState.instance().setParams(this, var1);
         this.actionContext.reportEvent("EventClimbWindow");
      }

   }

   public void climbSheetRope() {
      if (this.canClimbSheetRope(this.current)) {
         this.dropHeavyItems();
         HashMap var1 = this.getStateMachineParams(ClimbSheetRopeState.instance());
         var1.clear();
         this.actionContext.reportEvent("EventClimbRope");
      }
   }

   public void climbDownSheetRope() {
      if (this.canClimbDownSheetRope(this.current)) {
         this.dropHeavyItems();
         HashMap var1 = this.getStateMachineParams(ClimbDownSheetRopeState.instance());
         var1.clear();
         this.actionContext.reportEvent("EventClimbDownRope");
      }
   }

   public boolean canClimbSheetRope(IsoGridSquare var1) {
      if (var1 == null) {
         return false;
      } else {
         for(int var2 = var1.getZ(); var1 != null; var1 = this.getCell().getGridSquare(var1.getX(), var1.getY(), var1.getZ() + 1)) {
            if (!IsoWindow.isSheetRopeHere(var1)) {
               return false;
            }

            if (!IsoWindow.canClimbHere(var1)) {
               return false;
            }

            if (var1.TreatAsSolidFloor() && var1.getZ() > var2) {
               return false;
            }

            if (IsoWindow.isTopOfSheetRopeHere(var1)) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean canClimbDownSheetRopeInCurrentSquare() {
      return this.canClimbDownSheetRope(this.current);
   }

   public boolean canClimbDownSheetRope(IsoGridSquare var1) {
      if (var1 == null) {
         return false;
      } else {
         for(int var2 = var1.getZ(); var1 != null; var1 = this.getCell().getGridSquare(var1.getX(), var1.getY(), var1.getZ() - 1)) {
            if (!IsoWindow.isSheetRopeHere(var1)) {
               return false;
            }

            if (!IsoWindow.canClimbHere(var1)) {
               return false;
            }

            if (var1.TreatAsSolidFloor()) {
               return var1.getZ() < var2;
            }
         }

         return false;
      }
   }

   public void climbThroughWindow(IsoThumpable var1) {
      if (var1.canClimbThrough(this)) {
         this.dropHeavyItems();
         float var2 = this.getX() - (float)PZMath.fastfloor(this.getX());
         float var3 = this.getY() - (float)PZMath.fastfloor(this.getY());
         byte var4 = 0;
         byte var5 = 0;
         if (var1.getX() > this.getX() && !var1.north) {
            var4 = -1;
         }

         if (var1.getY() > this.getY() && var1.north) {
            var5 = -1;
         }

         this.setX(var1.getX() + var2 + (float)var4);
         this.setY(var1.getY() + var3 + (float)var5);
         ClimbThroughWindowState.instance().setParams(this, var1);
         this.actionContext.reportEvent("EventClimbWindow");
      }

   }

   public void climbThroughWindow(IsoThumpable var1, Integer var2) {
      if (var1.canClimbThrough(this)) {
         this.dropHeavyItems();
         ClimbThroughWindowState.instance().setParams(this, var1);
         this.actionContext.reportEvent("EventClimbWindow");
      }

   }

   public void climbOverFence(IsoDirections var1) {
      if (this.current != null) {
         IsoGridSquare var2 = this.current.nav[var1.index()];
         if (IsoWindow.canClimbThroughHelper(this, this.current, var2, var1 == IsoDirections.N || var1 == IsoDirections.S)) {
            ClimbOverFenceState.instance().setParams(this, var1);
            this.actionContext.reportEvent("EventClimbFence");
         }
      }
   }

   public boolean isAboveTopOfStairs() {
      if (this.getZ() != 0.0F && !((double)(this.getZ() - (float)PZMath.fastfloor(this.getZ())) > 0.01) && (this.current == null || !this.current.TreatAsSolidFloor())) {
         IsoGridSquare var1 = this.getCell().getGridSquare((double)this.getX(), (double)this.getY(), (double)(this.getZ() - 1.0F));
         return var1 != null && (var1.Has(IsoObjectType.stairsTN) || var1.Has(IsoObjectType.stairsTW));
      } else {
         return false;
      }
   }

   public void throwGrappledTargetOutWindow(IsoObject var1) {
      DebugType.Grapple.debugln("Attempting to throw out window: %s", var1);
      if (!this.isGrappling()) {
         DebugLog.Grapple.warn("Not currently grapling anything. Nothing to throw out the window, windowObject:%s", var1);
      } else {
         IGrappleable var2 = this.getGrapplingTarget();
         IsoGameCharacter var3 = (IsoGameCharacter)Type.tryCastTo(var2, IsoGameCharacter.class);
         if (var3 != null) {
            ClimbThroughWindowPositioningParams var4 = ClimbThroughWindowPositioningParams.alloc();
            ClimbThroughWindowState.getClimbThroughWindowPositioningParams(this, var1, var4);
            if (!var4.canClimb) {
               DebugType.Grapple.error("Cannot climb through, cannot throw out through.");
               var4.release();
            } else {
               this.setDoGrapple(false);
               this.setDoGrappleLetGo();
               this.setDir(var4.climbDir.Rot180());
               ClimbThroughWindowState.slideCharacterToWindowOpening(this, var4);
               GrappledThrownOutWindowState.instance().setParams(var3, var4.windowObject);
               this.actionContext.reportEvent("GrappleThrowOutWindow");
               this.setGrappleThrowOutWindow(true);
               var4.release();
            }
         }
      }
   }

   public void pickUpCorpse(IsoDeadBody var1) {
      DebugType.Grapple.debugln("Attempting to pick up corpse: %s", var1);
      if (var1 == null) {
         DebugType.Grapple.error("Body is null.");
      } else {
         this.setDoGrapple(true);
         float var2 = this.calculateGrappleEffectivenessFromTraits();
         var1.Grappled(this, this.getAttackingWeapon(), var2, "BwdDrag");
         this.setDoGrapple(false);
      }
   }

   public float calculateGrappleEffectivenessFromTraits() {
      float var1 = 1.0F;
      if (this.Traits.Underweight.isSet()) {
         var1 *= 0.8F;
      }

      if (this.Traits.VeryUnderweight.isSet()) {
         var1 *= 0.6F;
      }

      if (this.Traits.Emaciated.isSet()) {
         var1 *= 0.4F;
      }

      if (this.Traits.Overweight.isSet()) {
         var1 *= 1.1F;
      }

      if (this.Traits.Obese.isSet()) {
         var1 *= 1.05F;
      }

      if (this.Traits.Strong.isSet()) {
         var1 *= 1.25F;
      }

      if (this.Traits.Athletic.isSet()) {
         var1 *= 1.25F;
      }

      if (this.Traits.PlaysFootball.isSet()) {
         var1 *= 1.3F;
      }

      if (this.Traits.Lucky.isSet()) {
         var1 *= 1.1F;
      }

      if (this.Traits.Brave.isSet()) {
         var1 *= 1.1F;
      }

      if (this.Traits.Cowardly.isSet()) {
         var1 *= 0.9F;
      }

      if (this.Traits.SpeedDemon.isSet()) {
         var1 *= 1.15F;
      }

      return var1;
   }

   public void preupdate() {
      super.preupdate();
      this.updateAnimationTimeDelta();
      if (!this.m_bDebugVariablesRegistered && DebugOptions.instance.Character.Debug.RegisterDebugVariables.getValue()) {
         this.registerDebugGameVariables();
      }

      this.updateAnimationRecorderState();
      if (this.m_animationRecorder != null && (this.m_animationRecorder.isRecording() || this.m_animationRecorder.hasActiveLine())) {
         int var1 = IsoWorld.instance.getFrameNo();
         this.m_animationRecorder.newFrame(var1);
      }

      if (GameServer.bServer && this.handItemShouldSendToClients) {
         if (GameServer.bServer) {
            INetworkPacket.send((IsoPlayer)this, PacketTypes.PacketType.Equip, this);
         }

         this.handItemShouldSendToClients = false;
      }

   }

   private void updateAnimationTimeDelta() {
      this.m_animationTimeScale = 1.0F;
      this.m_animationUpdatingThisFrame = true;
      if (DebugOptions.instance.ZombieAnimationDelay.getValue()) {
         if (this.getAlpha(IsoCamera.frameState.playerIndex) < 0.03F) {
            this.m_animationUpdatingThisFrame = this.m_animationInvisibleFrameDelay.update();
            if (this.m_animationUpdatingThisFrame) {
               if (GameClient.bClient && this instanceof IsoZombie && ((IsoZombie)this).isRemoteZombie()) {
                  this.m_animationTimeScale = 1.0F;
               } else {
                  this.m_animationTimeScale = (float)(this.m_animationInvisibleFrameDelay.delay + 1);
               }
            }
         }

      }
   }

   public void updateHandEquips() {
      if (GameServer.bServer) {
         INetworkPacket.send((IsoPlayer)this, PacketTypes.PacketType.Equip, this);
      } else {
         INetworkPacket.send(PacketTypes.PacketType.Equip, this);
      }

      this.handItemShouldSendToClients = false;
   }

   public void setTeleport(NetworkTeleport var1) {
      this.teleport = var1;
   }

   public NetworkTeleport getTeleport() {
      return this.teleport;
   }

   public boolean isTeleporting() {
      return this.teleport != null;
   }

   public void update() {
      this.updateInternal.invoke();
   }

   public boolean isPushedByForSeparate(IsoMovingObject var1) {
      if (var1 instanceof IGrappleable var2) {
         if (this.isGrapplingTarget(var2)) {
            return false;
         }

         if (this.isBeingGrappledBy(var2)) {
            return false;
         }
      }

      return super.isPushedByForSeparate(var1);
   }

   public void setHitDir(Vector2 var1) {
      super.setHitDir(var1);
      this.setHitDirEnum(this.determineHitDirEnum(var1));
   }

   private void setHitDirEnum(String var1) {
      this.m_hitDirEnum = var1;
   }

   public String getHitDirEnum() {
      return this.m_hitDirEnum;
   }

   private String determineHitDirEnum(Vector2 var1) {
      Vector2 var2 = this.getLookVector(IsoGameCharacter.l_testDotSide.v1);
      Vector2 var3 = IsoGameCharacter.l_testDotSide.v3.set(var1);
      var3.normalize();
      float var4 = Vector2.dot(var3.x, var3.y, var2.x, var2.y);
      if ((double)var4 < -0.5) {
         return "FRONT";
      } else if ((double)var4 > 0.5) {
         return "BEHIND";
      } else {
         float var5 = var3.x * var2.y - var3.y * var2.x;
         return var5 > 0.0F ? "RIGHT" : "LEFT";
      }
   }

   private void updateInternal() {
      if (this.current != null) {
         if (this.teleport != null) {
            this.teleport.process(IsoPlayer.getPlayerIndex());
         }

         this.updateAlpha();
         if (!this.isAnimal()) {
            this.updateBallisticsTarget();
         }

         if (this.isNPC) {
            this.ai.update();
         }

         if (this.sprite != null) {
            this.legsSprite = this.sprite;
         }

         if (this.isGrappling() && !this.isPerformingAnyGrappleAnimation() && !this.isInGrapplerState()) {
            this.LetGoOfGrappled("Aborted");
         }

         if (!this.isDead() || this.current != null && this.current.getMovingObjects().contains(this)) {
            this.checkSCBADrain();
            float var1;
            if (this.getBodyDamage() != null && this.getCurrentBuilding() != null && this.getCurrentBuilding().isToxic() && !this.isProtectedFromToxic(true)) {
               var1 = GameTime.getInstance().getThirtyFPSMultiplier();
               if (this.getStats().getFatigue() < 1.0F) {
                  this.getStats().setFatigue(this.getStats().getFatigue() + 1.0E-4F * var1);
               }

               if ((double)this.getStats().getFatigue() > 0.8) {
                  this.getBodyDamage().getBodyPart(BodyPartType.Head).ReduceHealth(0.1F * var1);
               }

               this.getBodyDamage().getBodyPart(BodyPartType.Torso_Upper).ReduceHealth(0.1F * var1);
            }

            if (this.lungeFallTimer > 0.0F) {
               this.lungeFallTimer -= GameTime.getInstance().getThirtyFPSMultiplier();
            }

            if (this.getMeleeDelay() > 0.0F) {
               this.setMeleeDelay(this.getMeleeDelay() - 0.625F * GameTime.getInstance().getMultiplier());
            }

            if (this.getRecoilDelay() > 0.0F) {
               this.setRecoilDelay(this.getRecoilDelay() - 0.625F * GameTime.getInstance().getMultiplier());
            }

            this.sx = 0.0F;
            this.sy = 0.0F;
            if (this.current.getRoom() != null && this.current.getRoom().building.def.isAlarmed() && (!this.isZombie() || Core.bTutorial) && !GameClient.bClient) {
               boolean var5 = false;
               if (this instanceof IsoPlayer && (((IsoPlayer)this).isInvisible() || ((IsoPlayer)this).isGhostMode())) {
                  var5 = true;
               }

               if (!var5 && !this.isAnimal()) {
                  AmbientStreamManager.instance.doAlarm(this.current.getRoom().def);
               }
            }

            this.updateSeenVisibility();
            this.llx = this.getLastX();
            this.lly = this.getLastY();
            if (this.getClimbRopeTime() != 0.0F && !this.isClimbingRope()) {
               this.setClimbRopeTime(0.0F);
            }

            this.setLastX(this.getX());
            this.setLastY(this.getY());
            this.setLastZ(this.getZ());
            this.updateBeardAndHair();
            this.updateFalling();
            if (this.descriptor != null) {
               this.descriptor.Instance = this;
            }

            int var6;
            int var8;
            if (!this.isZombie()) {
               if (this.Traits.PoorPassenger.isSet() && this.getVehicle() != null && !this.getVehicle().isDriver(this) && this.getVehicle().getCurrentSpeedKmHour() > 50.0F) {
                  var1 = 0.1F + (this.getVehicle().getCurrentSpeedKmHour() - 50.0F) / 100.0F;
                  if (this.getVehicle().skidding) {
                     var1 *= 2.0F;
                  }

                  if ((double)this.stats.getSickness() < 0.3) {
                     this.stats.setSickness(this.stats.getSickness() + var1);
                  }
               }

               Stats var10000;
               if (this.Traits.Agoraphobic.isSet() && !this.getCurrentSquare().isInARoom()) {
                  var10000 = this.stats;
                  var10000.Panic += 0.5F * GameTime.getInstance().getThirtyFPSMultiplier();
               }

               if (this.Traits.Claustophobic.isSet() && this.getCurrentSquare().isInARoom()) {
                  var6 = this.getCurrentSquare().getRoomSize();
                  if (var6 > 0 && var6 < 70) {
                     float var2 = 1.0F;
                     var2 = 1.0F - (float)var6 / 70.0F;
                     if (var2 < 0.0F) {
                        var2 = 0.0F;
                     }

                     float var3 = 0.6F * var2 * GameTime.getInstance().getThirtyFPSMultiplier();
                     var10000 = this.stats;
                     var10000.Panic += var3;
                  }
               }

               if (this.getBodyDamage().getNumPartsBleeding() > 0) {
                  var1 = (1.0F - this.getBodyDamage().getOverallBodyHealth() / 100.0F) * (float)this.getBodyDamage().getNumPartsBleeding();
                  var10000 = this.stats;
                  var10000.Panic += (this.Traits.Hemophobic.isSet() ? 0.4F : 0.2F) * var1 * GameTime.getInstance().getThirtyFPSMultiplier();
               }

               if (this.Moodles != null) {
                  this.Moodles.Update();
               }

               if (this.Asleep) {
                  this.BetaEffect = 0.0F;
                  this.SleepingTabletEffect = 0.0F;
                  this.StopAllActionQueue();
               }

               if (this.BetaEffect > 0.0F) {
                  this.BetaEffect -= GameTime.getInstance().getThirtyFPSMultiplier();
                  var10000 = this.stats;
                  var10000.Panic -= 0.6F * GameTime.getInstance().getThirtyFPSMultiplier();
                  if (this.stats.Panic < 0.0F) {
                     this.stats.Panic = 0.0F;
                  }
               } else {
                  this.BetaDelta = 0.0F;
               }

               if (this.DepressFirstTakeTime > 0.0F || this.DepressEffect > 0.0F) {
                  this.DepressFirstTakeTime -= GameTime.getInstance().getThirtyFPSMultiplier();
                  if (this.DepressFirstTakeTime < 0.0F) {
                     this.DepressFirstTakeTime = -1.0F;
                     this.DepressEffect -= GameTime.getInstance().getThirtyFPSMultiplier();
                     this.getBodyDamage().setUnhappynessLevel(this.getBodyDamage().getUnhappynessLevel() - 0.03F * GameTime.getInstance().getThirtyFPSMultiplier());
                     if (this.getBodyDamage().getUnhappynessLevel() < 0.0F) {
                        this.getBodyDamage().setUnhappynessLevel(0.0F);
                     }
                  }
               }

               if (this.DepressEffect < 0.0F) {
                  this.DepressEffect = 0.0F;
               }

               if (this.SleepingTabletEffect > 0.0F) {
                  this.SleepingTabletEffect -= GameTime.getInstance().getThirtyFPSMultiplier();
                  var10000 = this.stats;
                  var10000.fatigue += 0.0016666667F * this.SleepingTabletDelta * GameTime.getInstance().getThirtyFPSMultiplier();
               } else {
                  this.SleepingTabletDelta = 0.0F;
               }

               if (this.Moodles != null) {
                  var6 = this.Moodles.getMoodleLevel(MoodleType.Panic);
                  if (var6 == 2) {
                     var10000 = this.stats;
                     var10000.Sanity -= 3.2E-7F;
                  } else if (var6 == 3) {
                     var10000 = this.stats;
                     var10000.Sanity -= 4.8000004E-7F;
                  } else if (var6 == 4) {
                     var10000 = this.stats;
                     var10000.Sanity -= 8.0E-7F;
                  } else if (var6 == 0) {
                     var10000 = this.stats;
                     var10000.Sanity += 1.0E-7F;
                  }

                  var8 = this.Moodles.getMoodleLevel(MoodleType.Tired);
                  if (var8 == 4) {
                     var10000 = this.stats;
                     var10000.Sanity -= 2.0E-6F;
                  }

                  if (this.stats.Sanity < 0.0F) {
                     this.stats.Sanity = 0.0F;
                  }

                  if (this.stats.Sanity > 1.0F) {
                     this.stats.Sanity = 1.0F;
                  }
               }
            }

            if (!this.CharacterActions.isEmpty()) {
               BaseAction var10 = (BaseAction)this.CharacterActions.get(0);
               boolean var9 = var10.valid();
               if (var9 && !var10.bStarted) {
                  var10.waitToStart();
               } else if (var9 && !var10.finished() && !var10.forceComplete && !var10.forceStop) {
                  var10.update();
               }

               if (!var9 || var10.finished() || var10.forceComplete || var10.forceStop) {
                  if (var10.finished() || var10.forceComplete) {
                     var10.perform();
                     if (!GameClient.bClient) {
                        var10.complete();
                     }

                     var9 = true;
                  }

                  if ((var10.finished() || var10.forceComplete) && !var10.loopAction || var10.forceStop || !var9) {
                     if (var10.bStarted && (var10.forceStop || !var9)) {
                        var10.stop();
                     }

                     this.CharacterActions.removeElement(var10);
                     if (this == IsoPlayer.players[0] || this == IsoPlayer.players[1] || this == IsoPlayer.players[2] || this == IsoPlayer.players[3]) {
                        UIManager.getProgressBar((double)((IsoPlayer)this).getPlayerNum()).setValue(0.0F);
                     }
                  }

                  var10.forceComplete = false;
               }

               for(int var7 = 0; var7 < this.EnemyList.size(); ++var7) {
                  IsoGameCharacter var4 = (IsoGameCharacter)this.EnemyList.get(var7);
                  if (var4.isDead()) {
                     this.EnemyList.remove(var4);
                     --var7;
                  }
               }
            }

            if (SystemDisabler.doCharacterStats && this.getBodyDamage() != null) {
               this.getBodyDamage().Update();
               this.updateBandages();
            }

            if (SystemDisabler.doCharacterStats) {
               this.calculateStats();
            }

            this.moveForwardVec.x = 0.0F;
            this.moveForwardVec.y = 0.0F;
            if (!this.Asleep || !(this instanceof IsoPlayer)) {
               this.setLastX(this.getX());
               this.setLastY(this.getY());
               this.setLastZ(this.getZ());
               this.square = this.getCurrentSquare();
               if (this.sprite != null) {
                  if (!this.bUseParts) {
                     this.sprite.update(this.def);
                  } else {
                     this.legsSprite.update(this.def);
                  }
               }

               this.setStateEventDelayTimer(this.getStateEventDelayTimer() - GameTime.getInstance().getThirtyFPSMultiplier());
            }

            this.stateMachine.update();
            if (this.isZombie() && VirtualZombieManager.instance.isReused((IsoZombie)this)) {
               DebugLog.log(DebugType.Zombie, "Zombie added to ReusableZombies after stateMachine.update - RETURNING " + this);
            } else {
               if (this instanceof IsoPlayer) {
                  this.ensureOnTile();
               }

               if ((this instanceof IsoPlayer || this instanceof IsoSurvivor) && this.RemoteID == -1 && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
                  RainManager.SetPlayerLocation(((IsoPlayer)this).getPlayerNum(), this.getCurrentSquare());
               }

               this.FireCheck();
               this.SpreadFire();
               this.ReduceHealthWhenBurning();
               this.updateTextObjects();
               if (this.stateMachine.getCurrent() == StaggerBackState.instance()) {
                  if (this.getStateEventDelayTimer() > 20.0F) {
                     this.BloodImpactX = this.getX();
                     this.BloodImpactY = this.getY();
                     this.BloodImpactZ = this.getZ();
                  }
               } else {
                  this.BloodImpactX = this.getX();
                  this.BloodImpactY = this.getY();
                  this.BloodImpactZ = this.getZ();
               }

               if (!this.isZombie()) {
                  this.recursiveItemUpdater(this.inventory);
               }

               this.LastZombieKills = this.ZombieKills;
               if (this.AttachedAnimSprite != null) {
                  var6 = this.AttachedAnimSprite.size();

                  for(var8 = 0; var8 < var6; ++var8) {
                     IsoSpriteInstance var12 = (IsoSpriteInstance)this.AttachedAnimSprite.get(var8);
                     IsoSprite var11 = var12.parentSprite;
                     var12.update();
                     if (var11.hasAnimation()) {
                        var12.Frame += var12.AnimFrameIncrease * GameTime.instance.getMultipliedSecondsSinceLastUpdate() * 60.0F;
                        if ((int)var12.Frame >= var11.CurrentAnim.Frames.size() && var11.Loop && var12.Looped) {
                           var12.Frame = 0.0F;
                        }
                     }
                  }
               }

               if (this.isGodMod()) {
                  this.getStats().setFatigue(0.0F);
                  this.getStats().setEndurance(1.0F);
                  BodyDamage var13 = this.getBodyDamage();
                  if (var13 != null) {
                     var13.setTemperature(37.0F);
                  }

                  this.getStats().setHunger(0.0F);
                  if (this instanceof IsoPlayer) {
                     ((IsoPlayer)this).resetSleepingPillsTaken();
                  }
               }

               this.updateMovementMomentum();
               if (this.effectiveEdibleBuffTimer > 0.0F) {
                  this.effectiveEdibleBuffTimer -= GameTime.getInstance().getMultiplier() * 0.015F;
                  if (this.effectiveEdibleBuffTimer < 0.0F) {
                     this.effectiveEdibleBuffTimer = 0.0F;
                  }
               }

               if (!GameServer.bServer || GameClient.bClient || !GameClient.bClient) {
                  this.updateDirt();
               }

               if (this.useHandWeapon != null && this.useHandWeapon.isAimedFirearm() && this.isAiming()) {
                  if (this instanceof IsoPlayer && this.isLocal()) {
                     ((IsoPlayer)this).setAngleFromAim();
                  }

                  this.updateBallistics();
               }

            }
         }
      }
   }

   private boolean isInGrapplerState() {
      if (this.actionContext == null) {
         return false;
      } else {
         ActionState var1 = this.actionContext.getCurrentState();
         return var1 == null ? false : var1.isGrapplerState();
      }
   }

   private void updateSeenVisibility() {
      for(int var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
         this.updateSeenVisibility(var1);
      }

   }

   private void updateSeenVisibility(int var1) {
      IsoPlayer var2 = IsoPlayer.players[var1];
      if (var2 != null) {
         this.IsVisibleToPlayer[var1] = this.TestIfSeen(var1);
         if (!this.IsVisibleToPlayer[var1]) {
            if (!(this instanceof IsoPlayer)) {
               if (!var2.isSeeEveryone()) {
                  this.setTargetAlpha(var1, 0.0F);
               }
            }
         }
      }
   }

   private void recursiveItemUpdater(ItemContainer var1) {
      for(int var2 = 0; var2 < var1.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)var1.Items.get(var2);
         if (var3 instanceof InventoryContainer) {
            this.recursiveItemUpdater((InventoryContainer)var3);
         }

         if (var3 instanceof IUpdater) {
            var3.update();
         }
      }

   }

   private void recursiveItemUpdater(InventoryContainer var1) {
      for(int var2 = 0; var2 < var1.getInventory().getItems().size(); ++var2) {
         InventoryItem var3 = (InventoryItem)var1.getInventory().getItems().get(var2);
         if (var3 instanceof InventoryContainer) {
            this.recursiveItemUpdater((InventoryContainer)var3);
         }

         if (var3 instanceof IUpdater) {
            var3.update();
         }
      }

   }

   private void updateDirt() {
      if (!this.isZombie() && this.getBodyDamage() != null) {
         int var1 = 0;
         if (this.isRunning() && Rand.NextBool(Rand.AdjustForFramerate(3500))) {
            var1 = 1;
         }

         if (this.isSprinting() && Rand.NextBool(Rand.AdjustForFramerate(2500))) {
            var1 += Rand.Next(1, 3);
         }

         if (this.getBodyDamage().getTemperature() > 37.0F && Rand.NextBool(Rand.AdjustForFramerate(5000))) {
            ++var1;
         }

         if (this.getBodyDamage().getTemperature() > 38.0F && Rand.NextBool(Rand.AdjustForFramerate(3000))) {
            ++var1;
         }

         IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(this, IsoPlayer.class);
         if (var2 != null && var2.isPlayerMoving() || var2 == null && this.isMoving()) {
            float var3 = this.square == null ? 0.0F : this.square.getPuddlesInGround();
            boolean var4 = this.square != null && this.isOutside() && this.square.hasNaturalFloor() && IsoPuddles.getInstance().getWetGroundFinalValue() > 0.5F;
            if (var3 > 0.09F && Rand.NextBool(Rand.AdjustForFramerate(1500))) {
               ++var1;
            } else if (var4 && Rand.NextBool(Rand.AdjustForFramerate(3000))) {
               ++var1;
            }

            if (var1 > 0) {
               this.addDirt((BloodBodyPartType)null, var1, true);
            }

            var1 = 0;
            if (var3 > 0.09F && Rand.NextBool(Rand.AdjustForFramerate(1500))) {
               ++var1;
            } else if (var4 && Rand.NextBool(Rand.AdjustForFramerate(3000))) {
               ++var1;
            }

            if (this.isInTrees() && Rand.NextBool(Rand.AdjustForFramerate(1500))) {
               ++var1;
            }

            if (var1 > 0) {
               this.addDirt((BloodBodyPartType)null, var1, false);
            }
         }

      }
   }

   protected void updateMovementMomentum() {
      float var1 = GameTime.instance.getTimeDelta();
      float var2;
      float var3;
      float var4;
      if (this.isPlayerMoving() && !this.isAiming()) {
         var2 = this.m_momentumScalar * 0.55F;
         if (var2 >= 0.55F) {
            this.m_momentumScalar = 1.0F;
            return;
         }

         var3 = var2 + var1;
         var4 = var3 / 0.55F;
         this.m_momentumScalar = PZMath.clamp(var4, 0.0F, 1.0F);
      } else {
         var2 = (1.0F - this.m_momentumScalar) * 0.25F;
         if (var2 >= 0.25F) {
            this.m_momentumScalar = 0.0F;
            return;
         }

         var3 = var2 + var1;
         var4 = var3 / 0.25F;
         float var5 = PZMath.clamp(var4, 0.0F, 1.0F);
         this.m_momentumScalar = 1.0F - var5;
      }

   }

   public double getHoursSurvived() {
      return GameTime.instance.getWorldAgeHours();
   }

   private void updateBeardAndHair() {
      if (!this.isZombie() && !this.isAnimal()) {
         if (!(this instanceof IsoPlayer) || ((IsoPlayer)this).isLocalPlayer()) {
            float var1 = (float)this.getHoursSurvived();
            if (this.beardGrowTiming < 0.0F || this.beardGrowTiming > var1) {
               this.beardGrowTiming = var1;
            }

            if (this.hairGrowTiming < 0.0F || this.hairGrowTiming > var1) {
               this.hairGrowTiming = var1;
            }

            boolean var2 = !GameClient.bClient && !GameServer.bServer || ServerOptions.instance.SleepAllowed.getValue() && ServerOptions.instance.SleepNeeded.getValue();
            boolean var3 = false;
            int var5;
            ArrayList var6;
            int var7;
            if ((this.isAsleep() || !var2) && var1 - this.beardGrowTiming > 120.0F) {
               this.beardGrowTiming = var1;
               BeardStyle var4 = BeardStyles.instance.FindStyle(((HumanVisual)this.getVisual()).getBeardModel());
               var5 = 1;
               if (var4 != null) {
                  var5 = var4.level;
               }

               var6 = BeardStyles.instance.getAllStyles();

               for(var7 = 0; var7 < var6.size(); ++var7) {
                  if (((BeardStyle)var6.get(var7)).growReference && ((BeardStyle)var6.get(var7)).level == var5 + 1) {
                     ((HumanVisual)this.getVisual()).setBeardModel(((BeardStyle)var6.get(var7)).name);
                     var3 = true;
                     break;
                  }
               }
            }

            if ((this.isAsleep() || !var2) && var1 - this.hairGrowTiming > 480.0F) {
               this.hairGrowTiming = var1;
               HairStyle var9 = HairStyles.instance.FindMaleStyle(((HumanVisual)this.getVisual()).getHairModel());
               if (this.isFemale()) {
                  var9 = HairStyles.instance.FindFemaleStyle(((HumanVisual)this.getVisual()).getHairModel());
               }

               var5 = 1;
               if (var9 != null) {
                  var5 = var9.level;
               }

               var6 = HairStyles.instance.m_MaleStyles;
               if (this.isFemale()) {
                  var6 = HairStyles.instance.m_FemaleStyles;
               }

               for(var7 = 0; var7 < var6.size(); ++var7) {
                  HairStyle var8 = (HairStyle)var6.get(var7);
                  if (var8.growReference && var8.level == var5 + 1) {
                     ((HumanVisual)this.getVisual()).setHairModel(var8.name);
                     ((HumanVisual)this.getVisual()).setNonAttachedHair((String)null);
                     var3 = true;
                     break;
                  }
               }
            }

            if (var3) {
               this.resetModelNextFrame();
               LuaEventManager.triggerEvent("OnClothingUpdated", this);
               if (GameClient.bClient) {
                  GameClient.instance.sendVisual((IsoPlayer)this);
               }
            }

         }
      }
   }

   private void updateFalling() {
      if (this instanceof IsoPlayer && !this.isClimbing()) {
         IsoRoofFixer.FixRoofsAt(this.current);
      }

      if (!this.shouldBeFalling()) {
         this.fallTime = 0.0F;
         this.lastFallSpeed = 0.0F;
         this.bFalling = false;
         this.wasOnStairs = false;
      } else {
         float var1 = 0.125F * GameTime.getInstance().getThirtyFPSMultiplier();
         var1 = (float)((double)var1 * 1.75);
         this.lastFallSpeed = var1;
         float var2;
         float var3;
         if (!this.current.TreatAsSolidFloor()) {
            var2 = 6.0F * GameTime.getInstance().getThirtyFPSMultiplier();
            var3 = this.getHeightAboveFloor();
            if (var1 > var3) {
               var2 *= var3 / var1;
            }

            this.fallTime += var2;
            if (this.fallTime < 20.0F && var3 < 0.2F) {
               this.fallTime = 0.0F;
            }

            this.setZ(this.getZ() - Math.min(var1, var3));
         } else if (!(this.getZ() > (float)PZMath.fastfloor(this.getZ())) && !(var1 < 0.0F)) {
            this.DoLand();
            this.fallTime = 0.0F;
            this.bFalling = false;
         } else if (!this.current.HasStairs() && !this.current.hasSlopedSurface()) {
            if (!this.wasOnStairs) {
               var2 = 6.0F * GameTime.getInstance().getThirtyFPSMultiplier();
               var3 = this.getHeightAboveFloor();
               if (var1 > var3) {
                  var2 *= var3 / var1;
               }

               this.fallTime += var2;
               this.setZ(this.getZ() - var1);
               if (this.getZ() < (float)PZMath.fastfloor(this.llz)) {
                  this.setZ((float)PZMath.fastfloor(this.llz));
                  this.DoLand();
                  this.fallTime = 0.0F;
                  this.bFalling = false;
               }
            } else {
               this.wasOnStairs = false;
            }
         } else {
            this.DoLand();
            this.fallTime = 0.0F;
            this.bFalling = false;
            this.wasOnStairs = true;
         }

         this.llz = this.getLastZ();
      }
   }

   public boolean shouldBeFalling() {
      if (this instanceof IsoAnimal && ((IsoAnimal)this).isOnHook()) {
         return false;
      } else if (this.isSeatedInVehicle()) {
         return false;
      } else if (this.isClimbing()) {
         return false;
      } else if (this.isRagdollFall()) {
         return false;
      } else if (this.isCurrentState(ClimbOverFenceState.instance())) {
         return false;
      } else {
         return !this.isCurrentState(ClimbThroughWindowState.instance());
      }
   }

   public float getHeightAboveFloor() {
      if (this.current == null) {
         return 1.0F;
      } else {
         float var1;
         if (this.current.HasStairs()) {
            var1 = this.current.getApparentZ(this.getX() - (float)PZMath.fastfloor(this.getX()), this.getY() - (float)PZMath.fastfloor(this.getY()));
            if (this.getZ() >= var1) {
               return this.getZ() - var1;
            }
         }

         if (this.current.hasSlopedSurface()) {
            var1 = this.current.getApparentZ(this.getX() - (float)PZMath.fastfloor(this.getX()), this.getY() - (float)PZMath.fastfloor(this.getY()));
            if (this.getZ() >= var1) {
               return this.getZ() - var1;
            }
         }

         if (this.current.TreatAsSolidFloor()) {
            return this.getZ() - (float)PZMath.fastfloor(this.getZ());
         } else if (this.current.chunk == null) {
            return this.getZ();
         } else if (this.current.z == this.current.chunk.minLevel) {
            return this.getZ();
         } else {
            for(int var4 = this.current.z; var4 >= this.current.chunk.minLevel; --var4) {
               IsoGridSquare var2 = this.getCell().getGridSquare(this.current.x, this.current.y, var4);
               if (var2 != null) {
                  float var3;
                  if (var2.HasStairs()) {
                     var3 = var2.getApparentZ(this.getX() - (float)PZMath.fastfloor(this.getX()), this.getY() - (float)PZMath.fastfloor(this.getY()));
                     return this.getZ() - var3;
                  }

                  if (var2.hasSlopedSurface()) {
                     var3 = var2.getApparentZ(this.getX() - (float)PZMath.fastfloor(this.getX()), this.getY() - (float)PZMath.fastfloor(this.getY()));
                     return this.getZ() - var3;
                  }

                  if (var2.TreatAsSolidFloor()) {
                     return this.getZ() - (float)var2.getZ();
                  }
               }
            }

            return 1.0F;
         }
      }
   }

   protected void updateMovementRates() {
   }

   protected float calculateIdleSpeed() {
      float var1 = 0.01F;
      var1 = (float)((double)var1 + (double)this.getMoodles().getMoodleLevel(MoodleType.Endurance) * 2.5 / 10.0);
      var1 *= GameTime.getAnimSpeedFix();
      return var1;
   }

   public float calculateBaseSpeed() {
      float var1 = 0.8F;
      float var2 = 1.0F;
      if (this.getMoodles() != null) {
         var1 -= (float)this.getMoodles().getMoodleLevel(MoodleType.Endurance) * 0.15F;
         var1 -= (float)this.getMoodles().getMoodleLevel(MoodleType.HeavyLoad) * 0.15F;
      }

      int var3;
      if (this.getMoodles().getMoodleLevel(MoodleType.Panic) >= 3 && this.Traits.AdrenalineJunkie.isSet()) {
         var3 = this.getMoodles().getMoodleLevel(MoodleType.Panic) + 1;
         var1 += (float)var3 / 20.0F;
      }

      for(var3 = BodyPartType.ToIndex(BodyPartType.Torso_Upper); var3 < BodyPartType.ToIndex(BodyPartType.Neck) + 1; ++var3) {
         BodyPart var4 = this.getBodyDamage().getBodyPart(BodyPartType.FromIndex(var3));
         if (var4.HasInjury()) {
            var1 -= 0.1F;
         }

         if (var4.bandaged()) {
            var1 += 0.05F;
         }
      }

      BodyPart var6 = this.getBodyDamage().getBodyPart(BodyPartType.UpperLeg_L);
      if (var6.getAdditionalPain(true) > 20.0F) {
         var1 -= (var6.getAdditionalPain(true) - 20.0F) / 100.0F;
      }

      for(int var7 = 0; var7 < this.bagsWorn.size(); ++var7) {
         InventoryContainer var5 = (InventoryContainer)this.bagsWorn.get(var7);
         var2 += this.calcRunSpeedModByBag(var5);
      }

      if (this.getPrimaryHandItem() != null && this.getPrimaryHandItem() instanceof InventoryContainer) {
         var2 += this.calcRunSpeedModByBag((InventoryContainer)this.getPrimaryHandItem());
      }

      if (this.getSecondaryHandItem() != null && this.getSecondaryHandItem() instanceof InventoryContainer) {
         var2 += this.calcRunSpeedModByBag((InventoryContainer)this.getSecondaryHandItem());
      }

      if (this.isOutside()) {
         if (this.getCurrentSquare().hasNaturalFloor()) {
            var1 -= IsoPuddles.getInstance().getWetGroundFinalValue() * 0.25F;
         }

         if (this.getCurrentSquare().hasSand()) {
            var1 -= 0.05F;
         }
      }

      this.fullSpeedMod = this.runSpeedModifier + (var2 - 1.0F);
      return var1 * (1.0F - Math.abs(1.0F - this.fullSpeedMod) / 2.0F);
   }

   private float calcRunSpeedModByClothing() {
      float var1 = 0.0F;
      int var2 = 0;

      for(int var3 = 0; var3 < this.wornItems.size(); ++var3) {
         InventoryItem var4 = this.wornItems.getItemByIndex(var3);
         if (var4 instanceof Clothing && ((Clothing)var4).getRunSpeedModifier() != 1.0F) {
            var1 += ((Clothing)var4).getRunSpeedModifier();
            ++var2;
         }
      }

      if (var1 == 0.0F && var2 == 0) {
         var1 = 1.0F;
         var2 = 1;
      }

      if (this.getWornItem("Shoes") == null) {
         var1 *= 0.8F;
      }

      return var1 / (float)var2;
   }

   private float calcRunSpeedModByBag(InventoryContainer var1) {
      float var2 = var1.getScriptItem().runSpeedModifier - 1.0F;
      float var3 = var1.getContentsWeight() / (float)var1.getEffectiveCapacity(this);
      var2 *= 1.0F + var3 / 2.0F;
      return var2;
   }

   public float calculateCombatSpeed() {
      boolean var1 = true;
      float var2 = 1.0F;
      HandWeapon var3 = null;
      if (this.getPrimaryHandItem() != null && this.getPrimaryHandItem() instanceof HandWeapon) {
         var3 = (HandWeapon)this.getPrimaryHandItem();
         var2 *= ((HandWeapon)this.getPrimaryHandItem()).getBaseSpeed();
      }

      WeaponType var4 = WeaponType.getWeaponType(this);
      if (var3 != null && var3.isTwoHandWeapon() && this.getSecondaryHandItem() != var3) {
         var2 *= 0.77F;
      }

      if (var3 != null && this.Traits.Axeman.isSet() && var3.getCategories().contains("Axe")) {
         var2 *= this.getChopTreeSpeed();
         var1 = false;
      }

      var2 -= (float)this.getMoodles().getMoodleLevel(MoodleType.Endurance) * 0.07F;
      var2 -= (float)this.getMoodles().getMoodleLevel(MoodleType.HeavyLoad) * 0.07F;
      var2 += (float)this.getWeaponLevel() * 0.03F;
      var2 += (float)this.getPerkLevel(PerkFactory.Perks.Fitness) * 0.02F;
      if (this.getSecondaryHandItem() != null && this.getSecondaryHandItem() instanceof InventoryContainer) {
         var2 *= 0.95F;
      }

      var2 *= Rand.Next(1.1F, 1.2F);
      var2 *= this.combatSpeedModifier;
      var2 *= this.getArmsInjurySpeedModifier();
      if (this.getBodyDamage() != null && this.getBodyDamage().getThermoregulator() != null) {
         var2 *= this.getBodyDamage().getThermoregulator().getCombatModifier();
      }

      var2 = Math.min(1.6F, var2);
      var2 = Math.max(0.8F, var2);
      if (var3 != null && var3.isTwoHandWeapon() && var4.type.equalsIgnoreCase("heavy")) {
         var2 *= 1.2F;
      }

      return var2 * (var1 ? GameTime.getAnimSpeedFix() : 1.0F);
   }

   private float getArmsInjurySpeedModifier() {
      float var1 = 1.0F;
      float var2 = 0.0F;
      BodyPart var3 = this.getBodyDamage().getBodyPart(BodyPartType.Hand_R);
      var2 = this.calculateInjurySpeed(var3, true);
      if (var2 > 0.0F) {
         var1 -= var2;
      }

      var3 = this.getBodyDamage().getBodyPart(BodyPartType.ForeArm_R);
      var2 = this.calculateInjurySpeed(var3, true);
      if (var2 > 0.0F) {
         var1 -= var2;
      }

      var3 = this.getBodyDamage().getBodyPart(BodyPartType.UpperArm_R);
      var2 = this.calculateInjurySpeed(var3, true);
      if (var2 > 0.0F) {
         var1 -= var2;
      }

      return var1;
   }

   private float getFootInjurySpeedModifier() {
      float var1 = 0.0F;
      boolean var2 = true;
      float var3 = 0.0F;
      float var4 = 0.0F;

      for(int var5 = BodyPartType.ToIndex(BodyPartType.Groin); var5 < BodyPartType.ToIndex(BodyPartType.MAX); ++var5) {
         var1 = this.calculateInjurySpeed(this.getBodyDamage().getBodyPart(BodyPartType.FromIndex(var5)), false);
         if (var2) {
            var3 += var1;
         } else {
            var4 += var1;
         }

         var2 = !var2;
      }

      if (var3 > var4) {
         return -(var3 + var4);
      } else {
         return var3 + var4;
      }
   }

   private float calculateInjurySpeed(BodyPart var1, boolean var2) {
      float var3 = var1.getScratchSpeedModifier();
      float var4 = var1.getCutSpeedModifier();
      float var5 = var1.getBurnSpeedModifier();
      float var6 = var1.getDeepWoundSpeedModifier();
      float var7 = 0.0F;
      if ((var1.getType() == BodyPartType.Foot_L || var1.getType() == BodyPartType.Foot_R) && (var1.getBurnTime() > 5.0F || var1.getBiteTime() > 0.0F || var1.deepWounded() || var1.isSplint() || var1.getFractureTime() > 0.0F || var1.haveGlass())) {
         var7 = 1.0F;
         if (var1.bandaged()) {
            var7 = 0.7F;
         }

         if (var1.getFractureTime() > 0.0F) {
            var7 = this.calcFractureInjurySpeed(var1);
         }
      }

      if (var1.haveBullet()) {
         return 1.0F;
      } else {
         if (var1.getScratchTime() > 2.0F || var1.getCutTime() > 5.0F || var1.getBurnTime() > 0.0F || var1.getDeepWoundTime() > 0.0F || var1.isSplint() || var1.getFractureTime() > 0.0F || var1.getBiteTime() > 0.0F) {
            var7 += var1.getScratchTime() / var3 + var1.getCutTime() / var4 + var1.getBurnTime() / var5 + var1.getDeepWoundTime() / var6;
            var7 += var1.getBiteTime() / 20.0F;
            if (var1.bandaged()) {
               var7 /= 2.0F;
            }

            if (var1.getFractureTime() > 0.0F) {
               var7 = this.calcFractureInjurySpeed(var1);
            }
         }

         if (var2 && var1.getPain() > 20.0F) {
            var7 += var1.getPain() / 10.0F;
         }

         return var7;
      }
   }

   private float calcFractureInjurySpeed(BodyPart var1) {
      float var2 = 0.4F;
      if (var1.getFractureTime() > 10.0F) {
         var2 = 0.7F;
      }

      if (var1.getFractureTime() > 20.0F) {
         var2 = 1.0F;
      }

      if (var1.getSplintFactor() > 0.0F) {
         var2 -= 0.2F;
         var2 -= Math.min(var1.getSplintFactor() / 10.0F, 0.8F);
      }

      return Math.max(0.0F, var2);
   }

   protected void calculateWalkSpeed() {
      if (!(this instanceof IsoPlayer) || ((IsoPlayer)this).isLocalPlayer()) {
         float var1;
         if (this instanceof IsoPlayer && !((IsoPlayer)this).getAttachedAnimals().isEmpty()) {
            var1 = this.getFootInjurySpeedModifier();
            this.setVariable("WalkInjury", var1);
            this.setVariable("WalkSpeed", 0.0F * GameTime.getAnimSpeedFix());
         } else {
            var1 = 0.0F;
            float var2 = this.getFootInjurySpeedModifier();
            this.setVariable("WalkInjury", var2);
            var1 = this.calculateBaseSpeed();
            if (!this.bRunning && !this.bSprinting) {
               var1 *= this.walkSpeedModifier;
            } else {
               var1 -= 0.15F;
               var1 *= this.fullSpeedMod;
               var1 += (float)this.getPerkLevel(PerkFactory.Perks.Sprinting) / 20.0F;
               var1 = (float)((double)var1 - Math.abs((double)var2 / 1.5));
               if ("Tutorial".equals(Core.GameMode)) {
                  var1 = Math.max(1.0F, var1);
               }
            }

            if (this.getSlowFactor() > 0.0F) {
               var1 *= 0.05F;
            }

            var1 = Math.min(1.0F, var1);
            if (this.getBodyDamage() != null && this.getBodyDamage().getThermoregulator() != null) {
               var1 *= this.getBodyDamage().getThermoregulator().getMovementModifier();
            }

            if (this.isAiming()) {
               float var3 = Math.min(0.9F + (float)this.getPerkLevel(PerkFactory.Perks.Nimble) / 10.0F, 1.5F);
               float var4 = Math.min(var1 * 2.5F, 1.0F);
               var3 *= var4;
               var3 = Math.max(var3, 0.6F);
               this.setVariable("StrafeSpeed", var3 * GameTime.getAnimSpeedFix());
            }

            if (this.isInTreesNoBush()) {
               IsoGridSquare var5 = this.getCurrentSquare();
               if (var5 != null && var5.Has(IsoObjectType.tree)) {
                  IsoTree var6 = var5.getTree();
                  if (var6 != null) {
                     var1 *= var6.getSlowFactor(this);
                  }
               }
            }

            this.setVariable("WalkSpeed", var1 * GameTime.getAnimSpeedFix());
         }
      }
   }

   public void updateSpeedModifiers() {
      this.runSpeedModifier = 1.0F;
      this.walkSpeedModifier = 1.0F;
      this.combatSpeedModifier = 1.0F;
      this.bagsWorn = new ArrayList();

      for(int var1 = 0; var1 < this.getWornItems().size(); ++var1) {
         InventoryItem var2 = this.getWornItems().getItemByIndex(var1);
         if (var2 instanceof Clothing var3) {
            this.combatSpeedModifier += var3.getCombatSpeedModifier() - 1.0F;
         }

         if (var2 instanceof InventoryContainer var5) {
            this.combatSpeedModifier += var5.getScriptItem().combatSpeedModifier - 1.0F;
            this.bagsWorn.add(var5);
         }
      }

      InventoryItem var4 = this.getWornItems().getItem("Shoes");
      if (var4 == null || var4.getCondition() == 0) {
         this.runSpeedModifier *= 0.85F;
         this.walkSpeedModifier *= 0.85F;
      }

   }

   public void updateDiscomfortModifiers() {
      this.clothingDiscomfortModifier = 0.0F;

      for(int var1 = 0; var1 < this.getWornItems().size(); ++var1) {
         InventoryItem var2 = this.getWornItems().getItemByIndex(var1);
         if (var2 instanceof Clothing var3) {
            this.clothingDiscomfortModifier += var3.getDiscomfortModifier();
         }

         if (var2 instanceof InventoryContainer var4) {
            this.clothingDiscomfortModifier += var4.getScriptItem().discomfortModifier;
         }
      }

      this.clothingDiscomfortModifier = Math.max(this.clothingDiscomfortModifier, 0.0F);
   }

   public void DoFloorSplat(IsoGridSquare var1, String var2, boolean var3, float var4, float var5) {
      if (var1 != null) {
         var1.DirtySlice();
         IsoObject var6 = null;

         for(int var7 = 0; var7 < var1.getObjects().size(); ++var7) {
            IsoObject var8 = (IsoObject)var1.getObjects().get(var7);
            if (var8.sprite != null && var8.sprite.getProperties().Is(IsoFlagType.solidfloor) && var6 == null) {
               var6 = var8;
            }
         }

         if (var6 != null && var6.sprite != null && (var6.sprite.getProperties().Is(IsoFlagType.vegitation) || var6.sprite.getProperties().Is(IsoFlagType.solidfloor))) {
            IsoSprite var9 = IsoSprite.getSprite(IsoSpriteManager.instance, (String)var2, 0);
            if (var9 == null) {
               return;
            }

            if (var6.AttachedAnimSprite.size() > 7) {
               return;
            }

            IsoSpriteInstance var10 = IsoSpriteInstance.get(var9);
            var6.AttachedAnimSprite.add(var10);
            ((IsoSpriteInstance)var6.AttachedAnimSprite.get(var6.AttachedAnimSprite.size() - 1)).Flip = var3;
            ((IsoSpriteInstance)var6.AttachedAnimSprite.get(var6.AttachedAnimSprite.size() - 1)).tintr = 0.5F + (float)Rand.Next(100) / 2000.0F;
            ((IsoSpriteInstance)var6.AttachedAnimSprite.get(var6.AttachedAnimSprite.size() - 1)).tintg = 0.7F + (float)Rand.Next(300) / 1000.0F;
            ((IsoSpriteInstance)var6.AttachedAnimSprite.get(var6.AttachedAnimSprite.size() - 1)).tintb = 0.7F + (float)Rand.Next(300) / 1000.0F;
            ((IsoSpriteInstance)var6.AttachedAnimSprite.get(var6.AttachedAnimSprite.size() - 1)).SetAlpha(0.4F * var5 * 0.6F);
            ((IsoSpriteInstance)var6.AttachedAnimSprite.get(var6.AttachedAnimSprite.size() - 1)).SetTargetAlpha(0.4F * var5 * 0.6F);
            ((IsoSpriteInstance)var6.AttachedAnimSprite.get(var6.AttachedAnimSprite.size() - 1)).offZ = -var4;
            ((IsoSpriteInstance)var6.AttachedAnimSprite.get(var6.AttachedAnimSprite.size() - 1)).offX = 0.0F;
         }

      }
   }

   void DoSplat(IsoGridSquare var1, String var2, boolean var3, IsoFlagType var4, float var5, float var6, float var7) {
      if (var1 != null) {
         var1.DoSplat(var2, var3, var4, var5, var6, var7);
      }
   }

   public boolean onMouseLeftClick(int var1, int var2) {
      if (IsoCamera.getCameraCharacter() != IsoPlayer.getInstance() && Core.bDebug) {
         IsoCamera.setCameraCharacter(this);
      }

      return super.onMouseLeftClick(var1, var2);
   }

   protected void calculateStats() {
      if (!this.isAnimal()) {
         if (GameServer.bServer) {
            this.stats.fatigue = 0.0F;
         } else if (GameClient.bClient && (!ServerOptions.instance.SleepAllowed.getValue() || !ServerOptions.instance.SleepNeeded.getValue())) {
            this.stats.fatigue = 0.0F;
         }

         if (!LuaHookManager.TriggerHook("CalculateStats", this)) {
            this.updateEndurance();
            this.updateTripping();
            this.updateThirst();
            this.updateStress();
            this.updateStats_WakeState();
            this.stats.endurance = PZMath.clamp(this.stats.endurance, 0.0F, 1.0F);
            this.stats.hunger = PZMath.clamp(this.stats.hunger, 0.0F, 1.0F);
            this.stats.stress = PZMath.clamp(this.stats.stress, 0.0F, 1.0F);
            this.stats.fatigue = PZMath.clamp(this.stats.fatigue, 0.0F, 1.0F);
            this.updateMorale();
            this.updateFitness();
         }
      }
   }

   protected void updateStats_WakeState() {
      if (!this.isAnimal()) {
         if (IsoPlayer.getInstance() == this && this.Asleep) {
            this.updateStats_Sleeping();
         } else {
            this.updateStats_Awake();
         }

      }
   }

   protected void updateStats_Sleeping() {
   }

   protected void updateStats_Awake() {
      Stats var10000 = this.stats;
      var10000.stress = (float)((double)var10000.stress - ZomboidGlobals.StressReduction * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay());
      float var1 = 1.0F - this.stats.endurance;
      if (var1 < 0.3F) {
         var1 = 0.3F;
      }

      float var2 = 1.0F;
      if (this.Traits.NeedsLessSleep.isSet()) {
         var2 = 0.7F;
      }

      if (this.Traits.NeedsMoreSleep.isSet()) {
         var2 = 1.3F;
      }

      double var3 = SandboxOptions.instance.getStatsDecreaseMultiplier();
      if (var3 < 1.0) {
         var3 = 1.0;
      }

      float var5 = 1.0F;
      if (this.isSitOnGround() || this.isSittingOnFurniture() || this.isResting()) {
         var5 = 1.5F;
      }

      var10000 = this.stats;
      var10000.fatigue = (float)((double)var10000.fatigue + ZomboidGlobals.FatigueIncrease * SandboxOptions.instance.getStatsDecreaseMultiplier() * (double)var1 * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay() * (double)var2 * this.getFatiqueMultiplier() / (double)var5);
      float var6 = this.getAppetiteMultiplier();
      if ((!(this instanceof IsoPlayer) || !((IsoPlayer)this).IsRunning() || !this.isPlayerMoving()) && !this.isCurrentState(SwipeStatePlayer.instance())) {
         if (this.Moodles.getMoodleLevel(MoodleType.FoodEaten) == 0) {
            var10000 = this.stats;
            var10000.hunger = (float)((double)var10000.hunger + ZomboidGlobals.HungerIncrease * SandboxOptions.instance.getStatsDecreaseMultiplier() * (double)var6 * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay() * this.getHungerMultiplier());
         } else {
            var10000 = this.stats;
            var10000.hunger = (float)((double)var10000.hunger + (double)((float)ZomboidGlobals.HungerIncreaseWhenWellFed) * SandboxOptions.instance.getStatsDecreaseMultiplier() * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay() * this.getHungerMultiplier());
         }
      } else if (this.Moodles.getMoodleLevel(MoodleType.FoodEaten) == 0) {
         var10000 = this.stats;
         var10000.hunger = (float)((double)var10000.hunger + ZomboidGlobals.HungerIncreaseWhenExercise / 3.0 * SandboxOptions.instance.getStatsDecreaseMultiplier() * (double)var6 * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay() * this.getHungerMultiplier());
      } else {
         var10000 = this.stats;
         var10000.hunger = (float)((double)var10000.hunger + ZomboidGlobals.HungerIncreaseWhenExercise * SandboxOptions.instance.getStatsDecreaseMultiplier() * (double)var6 * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay() * this.getHungerMultiplier());
      }

      if (this.getCurrentSquare() == this.getLastSquare() && !this.isReading()) {
         var10000 = this.stats;
         var10000.idleboredom += 5.0E-5F * GameTime.instance.getMultiplier() * GameTime.instance.getDeltaMinutesPerDay();
         var10000 = this.stats;
         var10000.idleboredom += 0.00125F * GameTime.instance.getMultiplier() * GameTime.instance.getDeltaMinutesPerDay();
      }

      if (this.getCurrentSquare() != null && this.getLastSquare() != null && this.getCurrentSquare().getRoom() == this.getLastSquare().getRoom() && this.getCurrentSquare().getRoom() != null && !this.isReading()) {
         var10000 = this.stats;
         var10000.idleboredom += 1.0E-4F * GameTime.instance.getMultiplier() * GameTime.instance.getDeltaMinutesPerDay();
         var10000 = this.stats;
         var10000.idleboredom += 0.00125F * GameTime.instance.getMultiplier() * GameTime.instance.getDeltaMinutesPerDay();
      }

   }

   private void updateMorale() {
      float var1 = 1.0F - this.stats.getStress() - 0.5F;
      var1 *= 1.0E-4F;
      if (var1 > 0.0F) {
         var1 += 0.5F;
      }

      Stats var10000 = this.stats;
      var10000.morale += var1;
      this.stats.morale = PZMath.clamp(this.stats.morale, 0.0F, 1.0F);
   }

   private void updateFitness() {
      this.stats.fitness = (float)this.getPerkLevel(PerkFactory.Perks.Fitness) / 5.0F - 1.0F;
      if (this.stats.fitness > 1.0F) {
         this.stats.fitness = 1.0F;
      }

      if (this.stats.fitness < -1.0F) {
         this.stats.fitness = -1.0F;
      }

   }

   private void updateTripping() {
      Stats var10000;
      if (this.stats.Tripping) {
         var10000 = this.stats;
         var10000.TrippingRotAngle += 0.06F;
      } else {
         var10000 = this.stats;
         var10000.TrippingRotAngle += 0.0F;
      }

   }

   protected float getAppetiteMultiplier() {
      float var1 = 1.0F - this.stats.hunger;
      if (this.Traits.HeartyAppitite.isSet()) {
         var1 *= 1.5F;
      }

      if (this.Traits.LightEater.isSet()) {
         var1 *= 0.75F;
      }

      return var1;
   }

   private void updateStress() {
      if (!this.isAnimal()) {
         float var1 = 1.0F;
         if (this.Traits.Cowardly.isSet()) {
            var1 = 2.0F;
         }

         if (this.Traits.Brave.isSet()) {
            var1 = 0.3F;
         }

         if (this.stats.Panic > 100.0F) {
            this.stats.Panic = 100.0F;
         }

         Stats var10000 = this.stats;
         var10000.stress = (float)((double)var10000.stress + (double)WorldSoundManager.instance.getStressFromSounds(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ())) * ZomboidGlobals.StressFromSoundsMultiplier);
         if (this.getBodyDamage().getNumPartsBitten() > 0) {
            var10000 = this.stats;
            var10000.stress = (float)((double)var10000.stress + ZomboidGlobals.StressFromBiteOrScratch * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay());
         }

         if (this.getBodyDamage().getNumPartsScratched() > 0) {
            var10000 = this.stats;
            var10000.stress = (float)((double)var10000.stress + ZomboidGlobals.StressFromBiteOrScratch * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay());
         }

         if (this.getBodyDamage().IsInfected() || this.getBodyDamage().IsFakeInfected()) {
            var10000 = this.stats;
            var10000.stress = (float)((double)var10000.stress + ZomboidGlobals.StressFromBiteOrScratch * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay());
         }

         if (this.Traits.Hemophobic.isSet()) {
            var10000 = this.stats;
            var10000.stress = (float)((double)var10000.stress + (double)this.getTotalBlood() * ZomboidGlobals.StressFromHemophobic * (double)(GameTime.instance.getMultiplier() / 0.8F) * (double)GameTime.instance.getDeltaMinutesPerDay());
         }

         if (this.Traits.Brooding.isSet()) {
            var10000 = this.stats;
            var10000.Anger = (float)((double)var10000.Anger - ZomboidGlobals.AngerDecrease * ZomboidGlobals.BroodingAngerDecreaseMultiplier * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay());
         } else {
            var10000 = this.stats;
            var10000.Anger = (float)((double)var10000.Anger - ZomboidGlobals.AngerDecrease * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay());
         }

         this.stats.Anger = PZMath.clamp(this.stats.Anger, 0.0F, 1.0F);
      }
   }

   private void updateEndurance() {
      this.stats.endurance = PZMath.clamp(this.stats.endurance, 0.0F, 1.0F);
      this.stats.endurancelast = this.stats.endurance;
      if (this.isUnlimitedEndurance()) {
         this.stats.endurance = 1.0F;
      }

   }

   private void updateThirst() {
      float var1 = 1.0F;
      if (this.Traits.HighThirst.isSet()) {
         var1 = (float)((double)var1 * 2.0);
      }

      if (this.Traits.LowThirst.isSet()) {
         var1 = (float)((double)var1 * 0.5);
      }

      if (IsoPlayer.getInstance() == this && !IsoPlayer.getInstance().isGhostMode()) {
         Stats var10000;
         if (this.Asleep) {
            var10000 = this.stats;
            var10000.thirst = (float)((double)var10000.thirst + ZomboidGlobals.ThirstSleepingIncrease * SandboxOptions.instance.getStatsDecreaseMultiplier() * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay() * (double)var1);
         } else {
            var10000 = this.stats;
            var10000.thirst = (float)((double)var10000.thirst + ZomboidGlobals.ThirstIncrease * SandboxOptions.instance.getStatsDecreaseMultiplier() * (double)GameTime.instance.getMultiplier() * this.getRunningThirstReduction() * (double)GameTime.instance.getDeltaMinutesPerDay() * (double)var1 * this.getThirstMultiplier());
         }

         if (this.stats.thirst > 1.0F) {
            this.stats.thirst = 1.0F;
         }
      }

      this.autoDrink();
   }

   private double getRunningThirstReduction() {
      return this == IsoPlayer.getInstance() && IsoPlayer.getInstance().IsRunning() ? 1.2 : 1.0;
   }

   public void faceDirection(IsoDirections var1) {
      this.dir = var1;
      this.getVectorFromDirection(this.m_forwardDirection);
      AnimationPlayer var2 = this.getAnimationPlayer();
      if (var2 != null && var2.isReady()) {
         var2.updateForwardDirection(this);
      }

   }

   public void faceLocation(float var1, float var2) {
      tempo.x = var1 + 0.5F;
      tempo.y = var2 + 0.5F;
      Vector2 var10000 = tempo;
      var10000.x -= this.getX();
      var10000 = tempo;
      var10000.y -= this.getY();
      this.DirectionFromVector(tempo);
      this.getVectorFromDirection(this.m_forwardDirection);
      AnimationPlayer var3 = this.getAnimationPlayer();
      if (var3 != null && var3.isReady()) {
         var3.updateForwardDirection(this);
      }

   }

   public void faceLocationF(float var1, float var2) {
      tempo.x = var1;
      tempo.y = var2;
      Vector2 var10000 = tempo;
      var10000.x -= this.getX();
      var10000 = tempo;
      var10000.y -= this.getY();
      if (tempo.getLengthSquared() != 0.0F) {
         this.DirectionFromVector(tempo);
         tempo.normalize();
         this.m_forwardDirection.set(tempo.x, tempo.y);
         AnimationPlayer var3 = this.getAnimationPlayer();
         if (var3 != null && var3.isReady()) {
            var3.updateForwardDirection(this);
         }

      }
   }

   public boolean isFacingLocation(float var1, float var2, float var3) {
      Vector2 var4 = BaseVehicle.allocVector2().set(var1 - this.getX(), var2 - this.getY());
      var4.normalize();
      Vector2 var5 = this.getLookVector(BaseVehicle.allocVector2());
      float var6 = var4.dot(var5);
      BaseVehicle.releaseVector2(var4);
      BaseVehicle.releaseVector2(var5);
      return var6 >= var3;
   }

   public boolean isFacingObject(IsoObject var1, float var2) {
      Vector2 var3 = BaseVehicle.allocVector2();
      var1.getFacingPosition(var3);
      boolean var4 = this.isFacingLocation(var3.x, var3.y, var2);
      BaseVehicle.releaseVector2(var3);
      return var4;
   }

   public void splatBlood(int var1, float var2) {
      if (this.getCurrentSquare() != null) {
         this.getCurrentSquare().splatBlood(var1, var2);
      }
   }

   public boolean isOutside() {
      return this.getCurrentSquare() == null ? false : this.getCurrentSquare().isOutside();
   }

   public boolean isFemale() {
      return this.bFemale;
   }

   public void setFemale(boolean var1) {
      this.bFemale = var1;
   }

   public boolean isZombie() {
      return false;
   }

   public int getLastHitCount() {
      return this.lastHitCount;
   }

   public void setLastHitCount(int var1) {
      this.lastHitCount = var1;
   }

   public int getSurvivorKills() {
      return this.SurvivorKills;
   }

   public void setSurvivorKills(int var1) {
      this.SurvivorKills = var1;
   }

   public int getAge() {
      return this.age;
   }

   public void setAge(int var1) {
      this.age = var1;
   }

   public void exert(float var1) {
      if (this.Traits.PlaysFootball.isSet()) {
         var1 *= 0.9F;
      }

      if (this.Traits.Jogger.isSet()) {
         var1 *= 0.9F;
      }

      Stats var10000 = this.stats;
      var10000.endurance -= var1;
   }

   public PerkInfo getPerkInfo(PerkFactory.Perk var1) {
      for(int var2 = 0; var2 < this.PerkList.size(); ++var2) {
         PerkInfo var3 = (PerkInfo)this.PerkList.get(var2);
         if (var3.perk == var1) {
            return var3;
         }
      }

      return null;
   }

   public boolean isEquipped(InventoryItem var1) {
      return this.isEquippedClothing(var1) || this.isHandItem(var1);
   }

   public boolean isEquippedClothing(InventoryItem var1) {
      return this.wornItems.contains(var1);
   }

   public boolean isAttachedItem(InventoryItem var1) {
      return this.getAttachedItems().contains(var1);
   }

   public void faceThisObject(IsoObject var1) {
      if (var1 != null) {
         Vector2 var2 = tempo;
         BaseVehicle var3 = (BaseVehicle)Type.tryCastTo(var1, BaseVehicle.class);
         BarricadeAble var4 = (BarricadeAble)Type.tryCastTo(var1, BarricadeAble.class);
         if (var3 != null) {
            var3.getFacingPosition(this, var2);
            var2.x -= this.getX();
            var2.y -= this.getY();
            this.DirectionFromVector(var2);
            var2.normalize();
            this.m_forwardDirection.set(var2.x, var2.y);
         } else if (var4 != null && this.current == var4.getSquare()) {
            this.dir = var4.getNorth() ? IsoDirections.N : IsoDirections.W;
            this.getVectorFromDirection(this.m_forwardDirection);
         } else if (var4 != null && this.current == var4.getOppositeSquare()) {
            this.dir = var4.getNorth() ? IsoDirections.S : IsoDirections.E;
            this.getVectorFromDirection(this.m_forwardDirection);
         } else {
            var1.getFacingPosition(var2);
            var2.x -= this.getX();
            var2.y -= this.getY();
            var2.normalize();
            this.DirectionFromVector(var2);
            this.m_forwardDirection.set(var2);
         }

         AnimationPlayer var5 = this.getAnimationPlayer();
         if (var5 != null && var5.isReady()) {
            var5.updateForwardDirection(this);
         }

      }
   }

   public void facePosition(int var1, int var2) {
      tempo.x = (float)var1;
      tempo.y = (float)var2;
      Vector2 var10000 = tempo;
      var10000.x -= this.getX();
      var10000 = tempo;
      var10000.y -= this.getY();
      this.DirectionFromVector(tempo);
      this.getVectorFromDirection(this.m_forwardDirection);
      AnimationPlayer var3 = this.getAnimationPlayer();
      if (var3 != null && var3.isReady()) {
         var3.updateForwardDirection(this);
      }

   }

   public void faceThisObjectAlt(IsoObject var1) {
      if (var1 != null) {
         var1.getFacingPositionAlt(tempo);
         Vector2 var10000 = tempo;
         var10000.x -= this.getX();
         var10000 = tempo;
         var10000.y -= this.getY();
         this.DirectionFromVector(tempo);
         this.getVectorFromDirection(this.m_forwardDirection);
         AnimationPlayer var2 = this.getAnimationPlayer();
         if (var2 != null && var2.isReady()) {
            var2.updateForwardDirection(this);
         }

      }
   }

   public void setAnimated(boolean var1) {
      this.legsSprite.Animate = true;
   }

   public long playHurtSound() {
      return this.getEmitter().playVocals(this.getHurtSound());
   }

   public void playDeadSound() {
      if (!(this instanceof IsoAnimal)) {
         if (this.isCloseKilled()) {
            this.getEmitter().playSoundImpl("HeadStab", this);
         } else if (this.isKilledBySlicingWeapon()) {
            this.getEmitter().playSoundImpl("HeadSlice", this);
         } else if (this instanceof IsoZombie) {
            this.getEmitter().playSoundImpl("HeadSmash", this);
         } else if (this instanceof IsoPlayer) {
            IsoPlayer var1 = (IsoPlayer)this;
            if (!this.isDeathDragDown() && !this.isKilledByFall()) {
               var1.playerVoiceSound("DeathAlone");
            }
         }

         if (this.isZombie()) {
            ((IsoZombie)this).parameterZombieState.setState(ParameterZombieState.State.Death);
         }

      }
   }

   public void saveChange(String var1, KahluaTable var2, ByteBuffer var3) {
      super.saveChange(var1, var2, var3);
      if ("addItem".equals(var1)) {
         DebugLog.General.warn("The addItem change type in the IsoGameCharacter.saveChange function  was disabled. The server should create item and sent it using the sendAddItemToContainer function.");
      } else if ("addItemOfType".equals(var1)) {
         DebugLog.General.warn("The addItemOfType change type in the IsoGameCharacter.saveChange function  was disabled. The server should create item and sent it using the sendAddItemToContainer function.");
      } else if ("AddRandomDamageFromZombie".equals(var1)) {
         if (var2 != null && var2.rawget("zombie") instanceof Double) {
            var3.putShort(((Double)var2.rawget("zombie")).shortValue());
         }
      } else if (!"AddZombieKill".equals(var1)) {
         if ("removeItem".equals(var1)) {
            DebugLog.General.warn("The removeItem change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function.");
         } else if ("removeItemID".equals(var1)) {
            DebugLog.General.warn("The removeItemID change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function.");
         } else if ("removeItemType".equals(var1)) {
            DebugLog.General.warn("The removeItemType change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function.");
         } else if ("removeOneOf".equals(var1)) {
            DebugLog.General.warn("The removeOneOf change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function.");
         } else if ("reanimatedID".equals(var1)) {
            if (var2 != null && var2.rawget("ID") instanceof Double) {
               int var4 = ((Double)var2.rawget("ID")).intValue();
               var3.putInt(var4);
            }
         } else if ("Shove".equals(var1)) {
            if (var2 != null && var2.rawget("hitDirX") instanceof Double && var2.rawget("hitDirY") instanceof Double && var2.rawget("force") instanceof Double) {
               var3.putFloat(((Double)var2.rawget("hitDirX")).floatValue());
               var3.putFloat(((Double)var2.rawget("hitDirY")).floatValue());
               var3.putFloat(((Double)var2.rawget("force")).floatValue());
            }
         } else if (!"wakeUp".equals(var1) && "mechanicActionDone".equals(var1) && var2 != null) {
            var3.put((byte)((Boolean)var2.rawget("success") ? 1 : 0));
         }
      }

   }

   public void loadChange(String var1, ByteBuffer var2) {
      super.loadChange(var1, var2);
      if ("addItem".equals(var1)) {
         DebugLog.General.warn("The addItem change type in the IsoGameCharacter.saveChange function  was disabled. The server should create item and sent it using the sendAddItemToContainer function.");
      } else if ("addItemOfType".equals(var1)) {
         DebugLog.General.warn("The addItemOfType change type in the IsoGameCharacter.saveChange function  was disabled. The server should create item and sent it using the sendAddItemToContainer function.");
      } else if ("AddRandomDamageFromZombie".equals(var1)) {
         short var3 = var2.getShort();
         IsoZombie var4 = GameClient.getZombie(var3);
         if (var4 != null && !this.isDead()) {
            this.getBodyDamage().AddRandomDamageFromZombie(var4, (String)null);
            this.getBodyDamage().Update();
            if (this.isDead()) {
               if (this.isFemale()) {
                  var4.getEmitter().playSound("FemaleBeingEatenDeath");
               } else {
                  var4.getEmitter().playSound("MaleBeingEatenDeath");
               }
            }
         }
      } else if ("AddZombieKill".equals(var1)) {
         this.setZombieKills(this.getZombieKills() + 1);
      } else if ("exitVehicle".equals(var1)) {
         BaseVehicle var5 = this.getVehicle();
         if (var5 != null) {
            var5.exit(this);
            this.setVehicle((BaseVehicle)null);
         }
      } else if ("removeItem".equals(var1)) {
         DebugLog.General.warn("The removeItem change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function.");
      } else if ("removeItemID".equals(var1)) {
         DebugLog.General.warn("The removeItemID change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function.");
      } else if ("removeItemType".equals(var1)) {
         DebugLog.General.warn("The removeItemType change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function.");
      } else if ("removeOneOf".equals(var1)) {
         DebugLog.General.warn("The removeOneOf change type in the IsoGameCharacter.saveChange function  was disabled. The server must use the sendRemoveItemFromContainer function.");
      } else if ("reanimatedID".equals(var1)) {
         this.ReanimatedCorpseID = var2.getInt();
      } else if (!"Shove".equals(var1)) {
         if ("StopBurning".equals(var1)) {
            this.StopBurning();
         } else if ("wakeUp".equals(var1)) {
            if (this.isAsleep()) {
               this.Asleep = false;
               this.ForceWakeUpTime = -1.0F;
               TutorialManager.instance.StealControl = false;
               if (this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
                  UIManager.setFadeBeforeUI(((IsoPlayer)this).getPlayerNum(), true);
                  UIManager.FadeIn((double)((IsoPlayer)this).getPlayerNum(), 2.0);
                  GameClient.instance.sendPlayer((IsoPlayer)this);
               }
            }
         } else if ("mechanicActionDone".equals(var1)) {
            boolean var6 = var2.get() == 1;
            LuaEventManager.triggerEvent("OnMechanicActionDone", this, var6);
         } else if ("vehicleNoKey".equals(var1)) {
            this.SayDebug(" [img=media/ui/CarKey_none.png]");
         }
      }

   }

   public int getAlreadyReadPages(String var1) {
      for(int var2 = 0; var2 < this.ReadBooks.size(); ++var2) {
         ReadBook var3 = (ReadBook)this.ReadBooks.get(var2);
         if (var3.fullType.equals(var1)) {
            return var3.alreadyReadPages;
         }
      }

      return 0;
   }

   public void setAlreadyReadPages(String var1, int var2) {
      for(int var3 = 0; var3 < this.ReadBooks.size(); ++var3) {
         ReadBook var4 = (ReadBook)this.ReadBooks.get(var3);
         if (var4.fullType.equals(var1)) {
            var4.alreadyReadPages = var2;
            return;
         }
      }

      ReadBook var5 = new ReadBook();
      var5.fullType = var1;
      var5.alreadyReadPages = var2;
      this.ReadBooks.add(var5);
   }

   public void updateLightInfo() {
      if (GameServer.bServer) {
         if (!this.isZombie()) {
            synchronized(this.lightInfo) {
               this.lightInfo.square = this.movingSq;
               if (this.lightInfo.square == null) {
                  this.lightInfo.square = this.getCell().getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()));
               }

               if (this.ReanimatedCorpse != null) {
                  this.lightInfo.square = this.getCell().getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()));
               }

               this.lightInfo.x = this.getX();
               this.lightInfo.y = this.getY();
               this.lightInfo.z = this.getZ();
               this.lightInfo.angleX = this.getForwardDirection().getX();
               this.lightInfo.angleY = this.getForwardDirection().getY();
               this.lightInfo.torches.clear();
               this.lightInfo.night = GameTime.getInstance().getNight();
            }
         }
      }
   }

   public LightInfo initLightInfo2() {
      synchronized(this.lightInfo) {
         for(int var2 = 0; var2 < this.lightInfo2.torches.size(); ++var2) {
            IsoGameCharacter.TorchInfo.release((TorchInfo)this.lightInfo2.torches.get(var2));
         }

         this.lightInfo2.initFrom(this.lightInfo);
         return this.lightInfo2;
      }
   }

   public LightInfo getLightInfo2() {
      return this.lightInfo2;
   }

   public void postupdate() {
      this.postUpdateInternal.invoke();
   }

   public float getAnimationTimeDelta() {
      float var1 = GameTime.instance.getTimeDelta() * this.m_animationTimeScale;
      return var1;
   }

   private void postUpdateInternal() {
      super.postupdate();
      this.clearHitInfo();
      this.clearAttackVars();
      if (this.useBallistics() && this.ballisticsController != null) {
         this.ballisticsController.postUpdate();
      }

      if (this.isAnimationUpdatingThisFrame()) {
         AnimationPlayer var1 = this.getAnimationPlayer();
         var1.updateForwardDirection(this);
         boolean var2 = this.shouldBeTurning();
         this.setTurning(var2);
         boolean var3 = this.shouldBeTurning90();
         this.setTurning90(var3);
         boolean var4 = this.shouldBeTurningAround();
         this.setTurningAround(var4);
         this.actionContext.update();
         if (this.getCurrentSquare() != null) {
            float var5 = this.getAnimationTimeDelta();
            this.advancedAnimator.update(var5);
         }

         this.actionContext.clearEvent("ActiveAnimFinished");
         this.actionContext.clearEvent("ActiveAnimFinishing");
         this.actionContext.clearEvent("ActiveAnimLooped");
         GameProfiler.getInstance().invokeAndMeasure("Deltas", this, var1, IsoGameCharacter::applyDeltas);
         if (!this.hasActiveModel()) {
            GameProfiler.getInstance().invokeAndMeasure("Anim Player", this, var1, IsoGameCharacter::updateAnimPlayer);
         }

         if (this.isAnimationRecorderActive()) {
            for(int var6 = 0; var6 < var1.getNumTwistBones(); ++var6) {
               AnimatorsBoneTransform var7 = var1.getTwistBoneAt(var6);
               this.setVariable("twistBone_" + var6 + "_Name", var1.getTwistBoneNameAt(var6));
               this.setVariable("twistBone_" + var6 + "_Twist", 57.295776F * var7.Twist);
               this.setVariable("twistBone_" + var6 + "_BlendWeight", var7.BlendWeight);
            }

            this.m_animationRecorder.logVariables(this);
         }

         if (this.hasActiveModel()) {
            GameProfiler.getInstance().invokeAndMeasure("Model Slot", this, IsoGameCharacter::updateModelSlot);
         }

         this.doDeferredMovementFromRagdoll();
         this.updateLightInfo();
      }
   }

   public boolean isAnimationUpdatingThisFrame() {
      return this.m_animationUpdatingThisFrame;
   }

   private void clearHitInfo() {
      this.hitInfoList.clear();
   }

   private void clearAttackVars() {
      this.attackVars.clear();
   }

   private void updateAnimPlayer(AnimationPlayer var1) {
      var1.bUpdateBones = false;
      boolean var2 = PerformanceSettings.InterpolateAnims;
      PerformanceSettings.InterpolateAnims = false;

      try {
         var1.updateForwardDirection(this);
         float var3 = this.getAnimationTimeDelta();
         var1.Update(var3);
      } catch (Throwable var7) {
         ExceptionLogger.logException(var7);
      } finally {
         var1.bUpdateBones = true;
         PerformanceSettings.InterpolateAnims = var2;
      }

   }

   private void updateModelSlot() {
      try {
         ModelManager.ModelSlot var1 = this.legsSprite.modelSlot;
         float var2 = this.getAnimationTimeDelta();
         var1.Update(var2);
      } catch (Throwable var3) {
         ExceptionLogger.logException(var3);
      }

   }

   private void applyDeltas(AnimationPlayer var1) {
      MoveDeltaModifiers var2 = IsoGameCharacter.L_postUpdate.moveDeltas;
      var2.moveDelta = this.getMoveDelta();
      var2.turnDelta = this.getTurnDelta();
      boolean var3 = this.hasPath();
      boolean var4 = this instanceof IsoPlayer;
      if (var4 && var3 && this.isRunning()) {
         var2.turnDelta = Math.max(var2.turnDelta, 2.0F);
      }

      State var5 = this.getCurrentState();
      if (var5 != null) {
         var5.getDeltaModifiers(this, var2);
      }

      if (this.hasPath() && this.getPathFindBehavior2().isTurningToObstacle()) {
         var2.setMaxTurnDelta(2.0F);
      }

      this.getCurrentTimedActionDeltaModifiers(var2);
      if (var2.twistDelta == -1.0F) {
         var2.twistDelta = var2.turnDelta * 1.8F;
      }

      if (!this.isTurning()) {
         var2.turnDelta = 0.0F;
      }

      float var6 = Math.max(1.0F - var2.moveDelta / 2.0F, 0.0F);
      var1.angleStepDelta = var6 * var2.turnDelta;
      var1.angleTwistDelta = var6 * var2.twistDelta;
      var1.setMaxTwistAngle(0.017453292F * this.getMaxTwist());
   }

   private void getCurrentTimedActionDeltaModifiers(MoveDeltaModifiers var1) {
      if (!this.getCharacterActions().isEmpty()) {
         BaseAction var2 = (BaseAction)this.getCharacterActions().get(0);
         if (var2 != null) {
            if (!var2.finished()) {
               var2.getDeltaModifiers(var1);
            }
         }
      }
   }

   public boolean shouldBeTurning() {
      boolean var1 = this.isTwisting();
      if (this.isZombie() && this.getCurrentState() == ZombieFallDownState.instance()) {
         return false;
      } else if (this.blockTurning) {
         return false;
      } else if (this.isBehaviourMoving()) {
         return var1;
      } else if (this.isPlayerMoving()) {
         return var1;
      } else if (this.isAttacking()) {
         return !this.bAimAtFloor;
      } else {
         float var2 = this.getAbsoluteExcessTwist();
         if (var2 > 1.0F) {
            return true;
         } else {
            return this.isTurning() ? var1 : false;
         }
      }
   }

   public boolean shouldBeTurning90() {
      if (!this.isTurning()) {
         return false;
      } else if (this.isTurning90()) {
         return true;
      } else {
         float var1 = this.getTargetTwist();
         float var2 = Math.abs(var1);
         return var2 > 65.0F;
      }
   }

   public boolean shouldBeTurningAround() {
      if (!this.isTurning()) {
         return false;
      } else if (this.isTurningAround()) {
         return true;
      } else {
         float var1 = this.getTargetTwist();
         float var2 = Math.abs(var1);
         return var2 > 110.0F;
      }
   }

   public boolean isTurning() {
      return this.m_isTurning;
   }

   private void setTurning(boolean var1) {
      this.m_isTurning = var1;
   }

   public boolean isTurningAround() {
      return this.m_isTurningAround;
   }

   private void setTurningAround(boolean var1) {
      this.m_isTurningAround = var1;
   }

   public boolean isTurning90() {
      return this.m_isTurning90;
   }

   private void setTurning90(boolean var1) {
      this.m_isTurning90 = var1;
   }

   public boolean hasPath() {
      return this.getPath2() != null;
   }

   public boolean isAnimationRecorderActive() {
      return this.m_animationRecorder != null && this.m_animationRecorder.isRecording();
   }

   public AnimationPlayerRecorder getAnimationPlayerRecorder() {
      return this.m_animationRecorder;
   }

   public float getMeleeDelay() {
      return this.meleeDelay;
   }

   public void setMeleeDelay(float var1) {
      this.meleeDelay = Math.max(var1, 0.0F);
   }

   public float getRecoilDelay() {
      return this.RecoilDelay;
   }

   public void setRecoilDelay(float var1) {
      this.RecoilDelay = PZMath.max(0.0F, var1);
   }

   public float getAimingDelay() {
      return this.AimingDelay;
   }

   public void setAimingDelay(float var1) {
      float var2 = this.getPrimaryHandItem() instanceof HandWeapon ? (float)((HandWeapon)this.getPrimaryHandItem()).getAimingTime() : 0.0F;
      this.AimingDelay = PZMath.clamp(var1, 0.0F, var2);
   }

   public void resetAimingDelay() {
      if (!(this.getPrimaryHandItem() instanceof HandWeapon)) {
         this.AimingDelay = 0.0F;
      } else {
         this.AimingDelay = (float)((HandWeapon)this.getPrimaryHandItem()).getAimingTime();
         this.AimingDelay *= this.Traits.Dextrous.isSet() ? 0.8F : (this.Traits.AllThumbs.isSet() ? 1.2F : 1.0F);
         this.AimingDelay *= this.getVehicle() != null ? 1.5F : 1.0F;
      }

   }

   public void updateAimingDelay() {
      float var1 = 0.0F;
      if (this.getPrimaryHandItem() instanceof HandWeapon) {
         var1 = (float)((HandWeapon)this.getPrimaryHandItem()).getRecoilDelay(this) * ((float)this.getPerkLevel(PerkFactory.Perks.Aiming) / 30.0F);
      }

      if (this.isAiming() && this.getRecoilDelay() <= 0.0F + var1 && !this.getVariableBoolean("isracking")) {
         this.AimingDelay = PZMath.max(this.AimingDelay - 0.625F * GameTime.getInstance().getMultiplier() * (1.0F + 0.05F * (float)this.getPerkLevel(PerkFactory.Perks.Aiming) + (this.Traits.Marksman.isSet() ? 0.1F : 0.0F)), 0.0F);
      } else if (!this.isAiming()) {
         this.resetAimingDelay();
      }

   }

   public float getBeenMovingFor() {
      return this.BeenMovingFor;
   }

   public void setBeenMovingFor(float var1) {
      this.BeenMovingFor = PZMath.clamp(var1, 0.0F, 70.0F);
   }

   public boolean isForceShove() {
      return this.forceShove;
   }

   public void setForceShove(boolean var1) {
      this.forceShove = var1;
   }

   public String getClickSound() {
      return this.clickSound;
   }

   public void setClickSound(String var1) {
      this.clickSound = var1;
   }

   public int getMeleeCombatMod() {
      int var1 = this.getWeaponLevel();
      if (var1 == 1) {
         return -2;
      } else if (var1 == 2) {
         return 0;
      } else if (var1 == 3) {
         return 1;
      } else if (var1 == 4) {
         return 2;
      } else if (var1 == 5) {
         return 3;
      } else if (var1 == 6) {
         return 4;
      } else if (var1 == 7) {
         return 5;
      } else if (var1 == 8) {
         return 5;
      } else if (var1 == 9) {
         return 6;
      } else {
         return var1 >= 10 ? 7 : -5;
      }
   }

   public int getWeaponLevel() {
      return this.getWeaponLevel((HandWeapon)null);
   }

   public int getWeaponLevel(HandWeapon var1) {
      WeaponType var2 = WeaponType.getWeaponType(this);
      if (var1 != null) {
         var2 = WeaponType.getWeaponType(this);
      }

      if (var1 == null) {
         var1 = (HandWeapon)Type.tryCastTo(this.getPrimaryHandItem(), HandWeapon.class);
      }

      int var3 = -1;
      if (var2 != null && var2 != WeaponType.barehand && var1 != null) {
         if (((HandWeapon)this.getPrimaryHandItem()).getCategories().contains("Axe")) {
            var3 = this.getPerkLevel(PerkFactory.Perks.Axe);
         }

         if (((HandWeapon)this.getPrimaryHandItem()).getCategories().contains("Spear")) {
            var3 += this.getPerkLevel(PerkFactory.Perks.Spear);
         }

         if (((HandWeapon)this.getPrimaryHandItem()).getCategories().contains("SmallBlade")) {
            var3 += this.getPerkLevel(PerkFactory.Perks.SmallBlade);
         }

         if (((HandWeapon)this.getPrimaryHandItem()).getCategories().contains("LongBlade")) {
            var3 += this.getPerkLevel(PerkFactory.Perks.LongBlade);
         }

         if (((HandWeapon)this.getPrimaryHandItem()).getCategories().contains("Blunt")) {
            var3 += this.getPerkLevel(PerkFactory.Perks.Blunt);
         }

         if (((HandWeapon)this.getPrimaryHandItem()).getCategories().contains("SmallBlunt")) {
            var3 += this.getPerkLevel(PerkFactory.Perks.SmallBlunt);
         }
      }

      if (var3 > 10) {
         var3 = 10;
      }

      return var3 == -1 ? 0 : var3;
   }

   public int getMaintenanceMod() {
      int var1 = this.getPerkLevel(PerkFactory.Perks.Maintenance);
      var1 += this.getWeaponLevel() / 2;
      return var1;
   }

   public BaseVehicle getVehicle() {
      return this.vehicle;
   }

   public void setVehicle(BaseVehicle var1) {
      this.vehicle = var1;
   }

   public boolean isUnderVehicle() {
      return this.isUnderVehicleRadius(0.3F);
   }

   public boolean isUnderVehicleRadius(float var1) {
      int var2 = (PZMath.fastfloor(this.getX()) - 4) / 8;
      int var3 = (PZMath.fastfloor(this.getY()) - 4) / 8;
      int var4 = (int)Math.ceil((double)((this.getX() + 4.0F) / 8.0F));
      int var5 = (int)Math.ceil((double)((this.getY() + 4.0F) / 8.0F));
      Vector2 var6 = (Vector2)((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).alloc();

      for(int var7 = var3; var7 < var5; ++var7) {
         for(int var8 = var2; var8 < var4; ++var8) {
            IsoChunk var9 = GameServer.bServer ? ServerMap.instance.getChunk(var8, var7) : IsoWorld.instance.CurrentCell.getChunkForGridSquare(var8 * 8, var7 * 8, 0);
            if (var9 != null) {
               for(int var10 = 0; var10 < var9.vehicles.size(); ++var10) {
                  BaseVehicle var11 = (BaseVehicle)var9.vehicles.get(var10);
                  Vector2 var12 = var11.testCollisionWithCharacter(this, var1, var6);
                  if (var12 != null && var12.x != -1.0F) {
                     ((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).release(var6);
                     return true;
                  }
               }
            }
         }
      }

      ((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).release(var6);
      return false;
   }

   public boolean isProne() {
      return this.isOnFloor();
   }

   public boolean isBeingSteppedOn() {
      if (!this.isOnFloor()) {
         return false;
      } else {
         for(int var1 = -1; var1 <= 1; ++var1) {
            for(int var2 = -1; var2 <= 1; ++var2) {
               IsoGridSquare var3 = this.getCell().getGridSquare(PZMath.fastfloor(this.getX()) + var2, PZMath.fastfloor(this.getY()) + var1, PZMath.fastfloor(this.getZ()));
               if (var3 != null) {
                  ArrayList var4 = var3.getMovingObjects();

                  for(int var5 = 0; var5 < var4.size(); ++var5) {
                     IsoMovingObject var6 = (IsoMovingObject)var4.get(var5);
                     if (var6 != this) {
                        IsoGameCharacter var7 = (IsoGameCharacter)Type.tryCastTo(var6, IsoGameCharacter.class);
                        if (var7 != null && var7.getVehicle() == null && !var6.isOnFloor() && ZombieOnGroundState.isCharacterStandingOnOther(var7, this)) {
                           return true;
                        }
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   public float getTemperature() {
      return this.getBodyDamage().getTemperature();
   }

   public void setTemperature(float var1) {
      this.getBodyDamage().setTemperature(var1);
   }

   public float getReduceInfectionPower() {
      return this.reduceInfectionPower;
   }

   public void setReduceInfectionPower(float var1) {
      this.reduceInfectionPower = var1;
   }

   public float getInventoryWeight() {
      if (this.getInventory() == null) {
         return 0.0F;
      } else {
         float var1 = 0.0F;
         ArrayList var2 = this.getInventory().getItems();

         for(int var3 = 0; var3 < var2.size(); ++var3) {
            InventoryItem var4 = (InventoryItem)var2.get(var3);
            if (var4.getAttachedSlot() > -1 && !this.isEquipped(var4)) {
               var1 += var4.getHotbarEquippedWeight();
            } else if (this.isEquipped(var4)) {
               var1 += var4.getEquippedWeight();
            } else {
               var1 += var4.getUnequippedWeight();
            }
         }

         return var1;
      }
   }

   public void dropHandItems() {
      if (!"Tutorial".equals(Core.GameMode)) {
         if (!(this instanceof IsoPlayer) || ((IsoPlayer)this).isLocalPlayer()) {
            this.dropHeavyItems();
            IsoGridSquare var1 = this.getCurrentSquare();
            if (var1 != null) {
               InventoryItem var2 = this.getPrimaryHandItem();
               InventoryItem var3 = this.getSecondaryHandItem();
               if (var2 != null || var3 != null) {
                  var1 = this.getSolidFloorAt(var1.x, var1.y, var1.z);
                  if (var1 != null) {
                     float var4 = Rand.Next(0.1F, 0.9F);
                     float var5 = Rand.Next(0.1F, 0.9F);
                     float var6 = var1.getApparentZ(var4, var5) - (float)var1.getZ();
                     boolean var7 = false;
                     if (var3 == var2) {
                        var7 = true;
                     }

                     if (var2 != null) {
                        this.setPrimaryHandItem((InventoryItem)null);
                        this.getInventory().DoRemoveItem(var2);
                        var1.AddWorldInventoryItem(var2, var4, var5, var6);
                        LuaEventManager.triggerEvent("OnContainerUpdate");
                        LuaEventManager.triggerEvent("onItemFall", var2);
                        this.playDropItemSound(var2);
                     }

                     if (var3 != null) {
                        this.setSecondaryHandItem((InventoryItem)null);
                        if (!var7) {
                           this.getInventory().DoRemoveItem(var3);
                           var1.AddWorldInventoryItem(var3, var4, var5, var6);
                           LuaEventManager.triggerEvent("OnContainerUpdate");
                           LuaEventManager.triggerEvent("onItemFall", var3);
                           this.playDropItemSound(var3);
                        }
                     }

                     this.resetEquippedHandsModels();
                  }
               }
            }
         }
      }
   }

   public boolean shouldBecomeZombieAfterDeath() {
      float var10000;
      boolean var1;
      switch (SandboxOptions.instance.Lore.Transmission.getValue()) {
         case 1:
            if (!this.getBodyDamage().IsFakeInfected()) {
               var10000 = this.getBodyDamage().getInfectionLevel();
               this.getBodyDamage();
               if (var10000 >= 0.001F) {
                  var1 = true;
                  return var1;
               }
            }

            var1 = false;
            return var1;
         case 2:
            if (!this.getBodyDamage().IsFakeInfected()) {
               var10000 = this.getBodyDamage().getInfectionLevel();
               this.getBodyDamage();
               if (var10000 >= 0.001F) {
                  var1 = true;
                  return var1;
               }
            }

            var1 = false;
            return var1;
         case 3:
            return true;
         case 4:
            return false;
         default:
            return false;
      }
   }

   public void applyTraits(ArrayList<String> var1) {
      if (var1 != null) {
         HashMap var2 = new HashMap();
         var2.put(PerkFactory.Perks.Fitness, 5);
         var2.put(PerkFactory.Perks.Strength, 5);

         for(int var3 = 0; var3 < var1.size(); ++var3) {
            String var4 = (String)var1.get(var3);
            if (var4 != null && !var4.isEmpty()) {
               TraitFactory.Trait var5 = TraitFactory.getTrait(var4);
               if (var5 != null) {
                  if (!this.HasTrait(var4)) {
                     this.getTraits().add(var4);
                  }

                  HashMap var6 = var5.getXPBoostMap();
                  PerkFactory.Perk var9;
                  int var10;
                  if (var6 != null) {
                     for(Iterator var7 = var6.entrySet().iterator(); var7.hasNext(); var2.put(var9, var10)) {
                        Map.Entry var8 = (Map.Entry)var7.next();
                        var9 = (PerkFactory.Perk)var8.getKey();
                        var10 = (Integer)var8.getValue();
                        if (var2.containsKey(var9)) {
                           var10 += (Integer)var2.get(var9);
                        }
                     }
                  }
               }
            }
         }

         if (this instanceof IsoPlayer) {
            ((IsoPlayer)this).getNutrition().applyWeightFromTraits();
         }

         HashMap var11 = this.getDescriptor().getXPBoostMap();

         Iterator var12;
         Map.Entry var13;
         PerkFactory.Perk var14;
         int var15;
         for(var12 = var11.entrySet().iterator(); var12.hasNext(); var2.put(var14, var15)) {
            var13 = (Map.Entry)var12.next();
            var14 = (PerkFactory.Perk)var13.getKey();
            var15 = (Integer)var13.getValue();
            if (var2.containsKey(var14)) {
               var15 += (Integer)var2.get(var14);
            }
         }

         var12 = var2.entrySet().iterator();

         while(var12.hasNext()) {
            var13 = (Map.Entry)var12.next();
            var14 = (PerkFactory.Perk)var13.getKey();
            var15 = (Integer)var13.getValue();
            var15 = Math.max(0, var15);
            var15 = Math.min(10, var15);
            this.getDescriptor().getXPBoostMap().put(var14, Math.min(3, var15));

            for(int var16 = 0; var16 < var15; ++var16) {
               this.LevelPerk(var14);
            }

            this.getXp().setXPToLevel(var14, this.getPerkLevel(var14));
         }

      }
   }

   public InventoryItem createKeyRing() {
      String var1 = "Base.KeyRing";
      if (this.Traits.Lucky.isSet() && Rand.NextBool(100)) {
         var1 = "Base.KeyRing_RabbitFoot";
         if (Rand.NextBool(2)) {
            var1 = "Base.KeyRing_Clover";
         }
      }

      return this.createKeyRing(var1);
   }

   public InventoryItem createKeyRing(String var1) {
      if (var1 == null) {
         var1 = "Base.KeyRing";
      }

      InventoryItem var2 = this.getInventory().AddItem(var1);
      if (var2 != null && var2 instanceof InventoryContainer var3) {
         var3.setName(Translator.getText("IGUI_KeyRingName", this.getDescriptor().getForename(), this.getDescriptor().getSurname()));
         if (Rand.Next(100) < 40) {
            RoomDef var4 = IsoWorld.instance.MetaGrid.getRoomAt(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()));
            if (var4 != null && var4.getBuilding() != null) {
               String var5 = "Base.Key1";
               InventoryItem var6 = var3.getInventory().AddItem(var5);
               var6.setKeyId(var4.getBuilding().getKeyId());
            }
         }

         return var2;
      } else {
         return null;
      }
   }

   public void autoDrink() {
      if (!GameServer.bServer) {
         if (!GameClient.bClient || ((IsoPlayer)this).isLocalPlayer()) {
            if (Core.getInstance().getOptionAutoDrink()) {
               if (!this.isAsleep() && !this.isPerformingGrappleAnimation() && !this.isKnockedDown() && !this.isbFalling() && !this.isAiming() && !this.isClimbing()) {
                  if (!LuaHookManager.TriggerHook("AutoDrink", this)) {
                     if (!(this.stats.thirst <= 0.1F)) {
                        InventoryItem var1 = this.getWaterSource(this.getInventory().getItems());
                        if (var1 != null) {
                           Stats var10000 = this.stats;
                           var10000.thirst -= 0.1F;
                           if (GameClient.bClient) {
                              GameClient.instance.drink((IsoPlayer)this, 0.1F);
                           }

                           if (var1.hasComponent(ComponentType.FluidContainer) && (var1.getFluidContainer().getPrimaryFluid() == Fluid.Water || var1.getFluidContainer().getPrimaryFluid() == Fluid.CarbonatedWater)) {
                              float var2 = var1.getFluidContainer().getAmount() - 0.12F;
                              var1.getFluidContainer().adjustAmount(var2);
                           }

                           if (var1.getFluidContainer().getPrimaryFluid() == Fluid.TaintedWater) {
                              BodyDamage var6 = this.getBodyDamage();
                              Stats var3 = this.getStats();
                              if (var6.getPoisonLevel() < 20.0F && (double)var3.getSickness() < 0.3) {
                                 float var4 = 10.0F;
                                 if (this.Traits.IronGut.isSet()) {
                                    var4 = 5.0F;
                                 } else if (this.Traits.WeakStomach.isSet()) {
                                    var4 = 15.0F;
                                 }

                                 float var5 = var6.getPoisonLevel() + var4;
                                 if (var5 < 5.0F) {
                                    var5 = 5.0F;
                                 }

                                 var6.setPoisonLevel(var5);
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

   public InventoryItem getWaterSource(ArrayList<InventoryItem> var1) {
      InventoryItem var2 = null;
      new ArrayList();

      for(int var4 = 0; var4 < var1.size(); ++var4) {
         InventoryItem var5 = (InventoryItem)var1.get(var4);
         boolean var6 = false;
         boolean var7 = true;
         if (var5.isWaterSource() || var5.isBeingFilled()) {
            var6 = true;
            var7 = !var5.getFluidContainer().isCategory(FluidCategory.Hazardous) || !SandboxOptions.instance.EnableTaintedWaterText.getValue();
         }

         if (var5.hasComponent(ComponentType.FluidContainer) && !var5.getFluidContainer().isEmpty() && (var5.getFluidContainer().getPrimaryFluid() == Fluid.Water || var5.getFluidContainer().getPrimaryFluid() == Fluid.CarbonatedWater)) {
            var6 = true;
         }

         if (var6 && var7) {
            if (var5.hasComponent(ComponentType.FluidContainer) && (double)var5.getFluidContainer().getAmount() >= 0.12) {
               var2 = var5;
               break;
            }

            if (!(var5 instanceof InventoryContainer)) {
               var2 = var5;
               break;
            }
         }
      }

      return var2;
   }

   public List<String> getKnownRecipes() {
      return this.knownRecipes;
   }

   public boolean isRecipeKnown(Recipe var1) {
      return !DebugOptions.instance.Cheat.Recipe.KnowAll.getValue() && !SandboxOptions.instance.SeeNotLearntRecipe.getValue() ? this.getKnownRecipes().contains(var1.getOriginalname()) : true;
   }

   public boolean isRecipeKnown(CraftRecipe var1) {
      return this.isRecipeKnown(var1, false);
   }

   public boolean isRecipeKnown(CraftRecipe var1, boolean var2) {
      if ((var2 || !SandboxOptions.instance.SeeNotLearntRecipe.getValue()) && !DebugOptions.instance.Cheat.Recipe.KnowAll.getValue()) {
         return !var1.needToBeLearn() || this.getKnownRecipes().contains(var1.getName()) || this.getKnownRecipes().contains(var1.getMetaRecipe()) || this.getKnownRecipes().contains(var1.getTranslationName());
      } else {
         return true;
      }
   }

   public boolean isRecipeKnown(String var1) {
      return this.isRecipeKnown(var1, false);
   }

   public boolean isRecipeKnown(String var1, boolean var2) {
      Recipe var3 = ScriptManager.instance.getRecipe(var1);
      if (var3 != null) {
         return this.isRecipeKnown(var3);
      } else {
         return (var2 || !SandboxOptions.instance.SeeNotLearntRecipe.getValue()) && !DebugOptions.instance.Cheat.Recipe.KnowAll.getValue() ? this.getKnownRecipes().contains(var1) : true;
      }
   }

   public boolean isRecipeActuallyKnown(CraftRecipe var1) {
      return this.isRecipeKnown(var1, true);
   }

   public boolean isRecipeActuallyKnown(String var1) {
      return this.isRecipeKnown(var1, true);
   }

   public boolean learnRecipe(String var1) {
      return this.learnRecipe(var1, true);
   }

   public boolean learnRecipe(String var1, boolean var2) {
      if (!this.isRecipeKnown(var1, true)) {
         this.getKnownRecipes().add(var1);
         if (var2) {
            ScriptManager.instance.checkMetaRecipe(this, var1);
         }

         return true;
      } else {
         return false;
      }
   }

   public void addKnownMediaLine(String var1) {
      if (!StringUtils.isNullOrWhitespace(var1)) {
         this.knownMediaLines.add(var1.trim());
      }
   }

   public void removeKnownMediaLine(String var1) {
      if (!StringUtils.isNullOrWhitespace(var1)) {
         this.knownMediaLines.remove(var1.trim());
      }
   }

   public void clearKnownMediaLines() {
      this.knownMediaLines.clear();
   }

   public boolean isKnownMediaLine(String var1) {
      return StringUtils.isNullOrWhitespace(var1) ? false : this.knownMediaLines.contains(var1.trim());
   }

   protected void saveKnownMediaLines(ByteBuffer var1) {
      var1.putShort((short)this.knownMediaLines.size());
      Iterator var2 = this.knownMediaLines.iterator();

      while(var2.hasNext()) {
         String var3 = (String)var2.next();
         GameWindow.WriteStringUTF(var1, var3);
      }

   }

   protected void loadKnownMediaLines(ByteBuffer var1, int var2) {
      this.knownMediaLines.clear();
      short var3 = var1.getShort();

      for(int var4 = 0; var4 < var3; ++var4) {
         String var5 = GameWindow.ReadStringUTF(var1);
         this.knownMediaLines.add(var5);
      }

   }

   public boolean isMoving() {
      return this instanceof IsoPlayer && !((IsoPlayer)this).isAttackAnimThrowTimeOut() ? false : this.m_isMoving;
   }

   public boolean isBehaviourMoving() {
      State var1 = this.getCurrentState();
      return var1 != null && var1.isMoving(this);
   }

   public boolean isPlayerMoving() {
      return false;
   }

   public void setMoving(boolean var1) {
      this.m_isMoving = var1;
      if (GameClient.bClient && this instanceof IsoPlayer && ((IsoPlayer)this).bRemote) {
         ((IsoPlayer)this).m_isPlayerMoving = var1;
         ((IsoPlayer)this).setJustMoved(var1);
      }

   }

   private boolean isFacingNorthWesterly() {
      return this.dir == IsoDirections.W || this.dir == IsoDirections.NW || this.dir == IsoDirections.N || this.dir == IsoDirections.NE;
   }

   public boolean isAttacking() {
      return false;
   }

   public boolean isZombieAttacking() {
      return false;
   }

   public boolean isZombieAttacking(IsoMovingObject var1) {
      return false;
   }

   private boolean isZombieThumping() {
      if (this.isZombie()) {
         return this.getCurrentState() == ThumpState.instance();
      } else {
         return false;
      }
   }

   public int compareMovePriority(IsoGameCharacter var1) {
      if (var1 == null) {
         return 1;
      } else if (this.isZombieThumping() && !var1.isZombieThumping()) {
         return 1;
      } else if (!this.isZombieThumping() && var1.isZombieThumping()) {
         return -1;
      } else if (var1 instanceof IsoPlayer) {
         return GameClient.bClient && this.isZombieAttacking(var1) ? -1 : 0;
      } else if (this.isZombieAttacking() && !var1.isZombieAttacking()) {
         return 1;
      } else if (!this.isZombieAttacking() && var1.isZombieAttacking()) {
         return -1;
      } else if (this.isBehaviourMoving() && !var1.isBehaviourMoving()) {
         return 1;
      } else if (!this.isBehaviourMoving() && var1.isBehaviourMoving()) {
         return -1;
      } else if (this.isFacingNorthWesterly() && !var1.isFacingNorthWesterly()) {
         return 1;
      } else {
         return !this.isFacingNorthWesterly() && var1.isFacingNorthWesterly() ? -1 : 0;
      }
   }

   public long playSound(String var1) {
      return this.getEmitter().playSound(var1);
   }

   public long playSoundLocal(String var1) {
      return this.getEmitter().playSoundImpl(var1, (IsoObject)null);
   }

   public void stopOrTriggerSound(long var1) {
      this.getEmitter().stopOrTriggerSound(var1);
   }

   public long playDropItemSound(InventoryItem var1) {
      if (var1 == null) {
         return 0L;
      } else {
         String var2 = var1.getDropSound();
         if (var2 == null && var1 instanceof InventoryContainer) {
            var2 = "DropBag";
         }

         return var2 == null ? 0L : this.playSound(var2);
      }
   }

   public void addWorldSoundUnlessInvisible(int var1, int var2, boolean var3) {
      if (!this.isInvisible()) {
         WorldSoundManager.instance.addSound(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), var1, var2, var3);
      }
   }

   public boolean isKnownPoison(InventoryItem var1) {
      if (var1.hasTag("NoDetect")) {
         return false;
      } else if (var1 instanceof Food) {
         Food var2 = (Food)var1;
         if (var2.getPoisonPower() <= 0) {
            return false;
         } else if (var2.getHerbalistType() != null && !var2.getHerbalistType().isEmpty()) {
            return this.isRecipeActuallyKnown("Herbalist");
         } else if (var2.getPoisonDetectionLevel() >= 0 && this.getPerkLevel(PerkFactory.Perks.Cooking) >= 10 - var2.getPoisonDetectionLevel()) {
            return true;
         } else {
            return var2.getPoisonLevelForRecipe() != null;
         }
      } else {
         return false;
      }
   }

   public boolean isKnownPoison(Item var1) {
      if (var1.getType() == Item.Type.Food) {
         if (var1.PoisonPower <= 0) {
            return false;
         } else if (var1.HerbalistType != null && !var1.HerbalistType.isEmpty()) {
            return this.isRecipeActuallyKnown("Herbalist");
         } else if (var1.PoisonDetectionLevel >= 0 && this.getPerkLevel(PerkFactory.Perks.Cooking) >= 10 - var1.PoisonDetectionLevel) {
            return true;
         } else {
            return var1.PoisonDetectionLevel != null;
         }
      } else {
         return false;
      }
   }

   public int getLastHourSleeped() {
      return this.lastHourSleeped;
   }

   public void setLastHourSleeped(int var1) {
      this.lastHourSleeped = var1;
   }

   public void setTimeOfSleep(float var1) {
      this.timeOfSleep = var1;
   }

   public void setDelayToSleep(float var1) {
      this.delayToActuallySleep = var1;
   }

   public String getBedType() {
      return this.bedType;
   }

   public void setBedType(String var1) {
      this.bedType = var1;
   }

   public void enterVehicle(BaseVehicle var1, int var2, Vector3f var3) {
      if (this.vehicle != null) {
         this.vehicle.exit(this);
      }

      if (var1 != null) {
         var1.enter(var2, this, var3);
      }

   }

   public float Hit(BaseVehicle var1, float var2, boolean var3, float var4, float var5) {
      this.setHitFromBehind(var3);
      if (GameClient.bClient) {
         this.setAttackedBy((IsoGameCharacter)GameClient.IDToPlayerMap.get(var1.getNetPlayerId()));
      } else if (GameServer.bServer) {
         this.setAttackedBy((IsoGameCharacter)GameServer.IDToPlayerMap.get(var1.getNetPlayerId()));
      } else {
         this.setAttackedBy(var1.getDriver());
      }

      this.getHitDir().set(var4, var5);
      if (!this.isKnockedDown()) {
         this.setHitForce(Math.max(0.5F, var2 * 0.15F));
      } else {
         this.setHitForce(Math.min(2.5F, var2 * 0.15F));
      }

      if (GameClient.bClient) {
         HitReactionNetworkAI.CalcHitReactionVehicle(this, var1);
      }

      DebugLog.Damage.noise("Vehicle id=%d hit %s id=%d: speed=%f force=%f hitDir=%s", var1.getId(), this.getClass().getSimpleName(), this.getOnlineID(), var2, this.getHitForce(), this.getHitDir());
      return this.getHealth();
   }

   public Path getPath2() {
      return this.path2;
   }

   public void setPath2(Path var1) {
      this.path2 = var1;
   }

   public PathFindBehavior2 getPathFindBehavior2() {
      return this.pfb2;
   }

   public MapKnowledge getMapKnowledge() {
      return this.mapKnowledge;
   }

   public IsoObject getBed() {
      return !this.isAsleep() && !this.isResting() ? null : this.bed;
   }

   public void setBed(IsoObject var1) {
      this.bed = var1;
   }

   public boolean avoidDamage() {
      return this.m_avoidDamage;
   }

   public void setAvoidDamage(boolean var1) {
      this.m_avoidDamage = var1;
   }

   public boolean isReading() {
      return this.isReading;
   }

   public void setReading(boolean var1) {
      this.isReading = var1;
   }

   public float getTimeSinceLastSmoke() {
      return this.timeSinceLastSmoke;
   }

   public void setTimeSinceLastSmoke(float var1) {
      this.timeSinceLastSmoke = PZMath.clamp(var1, 0.0F, 10.0F);
   }

   public boolean isInvisible() {
      return this.m_cheats.m_invisible;
   }

   public void setInvisible(boolean var1) {
      if (!Role.haveCapability(this, Capability.ToggleInvisibleHimself)) {
         this.m_cheats.m_invisible = false;
      } else {
         this.m_cheats.m_invisible = var1;
      }
   }

   public boolean isCanUseBrushTool() {
      return this.m_cheats.m_canUseBrushTool;
   }

   public void setCanUseBrushTool(boolean var1) {
      if (!Role.haveCapability(this, Capability.UseBrushToolManager)) {
         this.m_cheats.m_canUseBrushTool = false;
      } else {
         this.m_cheats.m_canUseBrushTool = var1;
      }
   }

   public boolean isDriving() {
      return this.getVehicle() != null && this.getVehicle().getDriver() == this && !this.getVehicle().isStopped();
   }

   public boolean isInARoom() {
      return this.square != null && this.square.isInARoom();
   }

   public boolean isGodMod() {
      return this.m_cheats.m_godMod;
   }

   public boolean isInvulnerable() {
      return this.m_cheats.m_invulnerable;
   }

   public void setInvulnerable(boolean var1) {
      this.m_cheats.m_invulnerable = var1;
   }

   public void setGodMod(boolean var1) {
      if (!Role.haveCapability(this, Capability.ToggleGodModHimself)) {
         this.m_cheats.m_godMod = false;
      } else {
         if (!this.isDead()) {
            this.m_cheats.m_godMod = var1;
            if (this instanceof IsoPlayer && GameClient.bClient && ((IsoPlayer)this).isLocalPlayer() && GameWindow.states.current == IngameState.instance) {
               this.updateMovementRates();
               GameClient.sendPlayerInjuries((IsoPlayer)this);
               GameClient.sendPlayerDamage((IsoPlayer)this);
            }
         }

      }
   }

   public boolean isUnlimitedCarry() {
      return this.m_cheats.m_unlimitedCarry;
   }

   public void setUnlimitedCarry(boolean var1) {
      if (!Role.haveCapability(this, Capability.ToggleUnlimitedCarry)) {
         this.m_cheats.m_unlimitedCarry = false;
      } else {
         this.m_cheats.m_unlimitedCarry = var1;
      }
   }

   public boolean isBuildCheat() {
      return this.m_cheats.m_buildCheat;
   }

   public void setBuildCheat(boolean var1) {
      if (!Role.haveCapability(this, Capability.UseBuildCheat)) {
         this.m_cheats.m_buildCheat = false;
      } else {
         this.m_cheats.m_buildCheat = var1;
      }
   }

   public boolean isFarmingCheat() {
      return this.m_cheats.m_farmingCheat;
   }

   public void setFarmingCheat(boolean var1) {
      if (!Role.haveCapability(this, Capability.UseFarmingCheat)) {
         this.m_cheats.m_farmingCheat = false;
      } else {
         this.m_cheats.m_farmingCheat = var1;
      }
   }

   public boolean isFishingCheat() {
      return this.m_cheats.m_fishingCheat;
   }

   public void setFishingCheat(boolean var1) {
      if (!Role.haveCapability(this, Capability.UseFishingCheat)) {
         this.m_cheats.m_fishingCheat = false;
      } else {
         this.m_cheats.m_fishingCheat = var1;
      }
   }

   public boolean isHealthCheat() {
      return this.m_cheats.m_healthCheat;
   }

   public void setHealthCheat(boolean var1) {
      if (!Role.haveCapability(this, Capability.UseHealthCheat)) {
         this.m_cheats.m_healthCheat = false;
      } else {
         this.m_cheats.m_healthCheat = var1;
      }
   }

   public boolean isMechanicsCheat() {
      return this.m_cheats.m_mechanicsCheat;
   }

   public void setMechanicsCheat(boolean var1) {
      if (!Role.haveCapability(this, Capability.UseMechanicsCheat)) {
         this.m_cheats.m_mechanicsCheat = false;
      } else {
         this.m_cheats.m_mechanicsCheat = var1;
      }
   }

   public boolean isFastMoveCheat() {
      return this.m_cheats.m_fastMoveCheat;
   }

   public void setFastMoveCheat(boolean var1) {
      if (!Role.haveCapability(this, Capability.UseFastMoveCheat)) {
         this.m_cheats.m_fastMoveCheat = false;
      } else {
         this.m_cheats.m_fastMoveCheat = var1;
      }
   }

   public boolean isMovablesCheat() {
      return this.m_cheats.m_movablesCheat;
   }

   public void setMovablesCheat(boolean var1) {
      if (!Role.haveCapability(this, Capability.UseMovablesCheat)) {
         this.m_cheats.m_movablesCheat = false;
      } else {
         this.m_cheats.m_movablesCheat = var1;
      }
   }

   public boolean isTimedActionInstantCheat() {
      return this.m_cheats.m_timedActionInstantCheat;
   }

   public void setTimedActionInstantCheat(boolean var1) {
      if (!Role.haveCapability(this, Capability.UseTimedActionInstantCheat)) {
         this.m_cheats.m_timedActionInstantCheat = false;
      } else {
         this.m_cheats.m_timedActionInstantCheat = var1;
      }
   }

   public boolean isTimedActionInstant() {
      return Core.bDebug && DebugOptions.instance.Cheat.TimedAction.Instant.getValue() ? true : this.isTimedActionInstantCheat();
   }

   public boolean isShowAdminTag() {
      return this.showAdminTag;
   }

   public void setShowAdminTag(boolean var1) {
      this.showAdminTag = var1;
   }

   public IAnimationVariableSlot getVariable(AnimationVariableHandle var1) {
      return this.getGameVariablesInternal().getVariable(var1);
   }

   public IAnimationVariableSlot getVariable(String var1) {
      return this.getGameVariablesInternal().getVariable(var1);
   }

   public void setVariable(IAnimationVariableSlot var1) {
      this.getGameVariablesInternal().setVariable(var1);
   }

   public void setVariable(String var1, String var2) {
      if (GameClient.bClient && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer() && VariableSyncPacket.syncedVariables.contains(var1)) {
         INetworkPacket.send(PacketTypes.PacketType.VariableSync, this, var1, var2);
      }

      this.getGameVariablesInternal().setVariable(var1, var2);
   }

   public void setVariable(String var1, boolean var2) {
      if (GameClient.bClient && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer() && VariableSyncPacket.syncedVariables.contains(var1)) {
         INetworkPacket.send(PacketTypes.PacketType.VariableSync, this, var1, var2);
      }

      this.getGameVariablesInternal().setVariable(var1, var2);
   }

   public void setVariable(String var1, float var2) {
      if (GameClient.bClient && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer() && VariableSyncPacket.syncedVariables.contains(var1)) {
         INetworkPacket.send(PacketTypes.PacketType.VariableSync, this, var1, var2);
      }

      this.getGameVariablesInternal().setVariable(var1, var2);
   }

   public void setVariable(String var1, AnimationVariableSlotCallbackBool.CallbackGetStrongTyped var2) {
      this.getGameVariablesInternal().setVariable(var1, var2);
   }

   public void setVariable(String var1, AnimationVariableSlotCallbackBool.CallbackGetStrongTyped var2, AnimationVariableSlotCallbackBool.CallbackSetStrongTyped var3) {
      this.getGameVariablesInternal().setVariable(var1, var2, var3);
   }

   public void setVariable(String var1, AnimationVariableSlotCallbackString.CallbackGetStrongTyped var2) {
      this.getGameVariablesInternal().setVariable(var1, var2);
   }

   public void setVariable(String var1, AnimationVariableSlotCallbackString.CallbackGetStrongTyped var2, AnimationVariableSlotCallbackString.CallbackSetStrongTyped var3) {
      this.getGameVariablesInternal().setVariable(var1, var2, var3);
   }

   public void setVariable(String var1, AnimationVariableSlotCallbackFloat.CallbackGetStrongTyped var2) {
      this.getGameVariablesInternal().setVariable(var1, var2);
   }

   public void setVariable(String var1, AnimationVariableSlotCallbackFloat.CallbackGetStrongTyped var2, AnimationVariableSlotCallbackFloat.CallbackSetStrongTyped var3) {
      this.getGameVariablesInternal().setVariable(var1, var2, var3);
   }

   public void setVariable(String var1, AnimationVariableSlotCallbackInt.CallbackGetStrongTyped var2) {
      this.getGameVariablesInternal().setVariable(var1, var2);
   }

   public void setVariable(String var1, AnimationVariableSlotCallbackInt.CallbackGetStrongTyped var2, AnimationVariableSlotCallbackInt.CallbackSetStrongTyped var3) {
      this.getGameVariablesInternal().setVariable(var1, var2, var3);
   }

   public void setVariable(String var1, boolean var2, AnimationVariableSlotCallbackBool.CallbackGetStrongTyped var3) {
      this.getGameVariablesInternal().setVariable(var1, var2, var3);
   }

   public void setVariable(String var1, boolean var2, AnimationVariableSlotCallbackBool.CallbackGetStrongTyped var3, AnimationVariableSlotCallbackBool.CallbackSetStrongTyped var4) {
      this.getGameVariablesInternal().setVariable(var1, var2, var3, var4);
   }

   public void setVariable(String var1, String var2, AnimationVariableSlotCallbackString.CallbackGetStrongTyped var3) {
      this.getGameVariablesInternal().setVariable(var1, var2, var3);
   }

   public void setVariable(String var1, String var2, AnimationVariableSlotCallbackString.CallbackGetStrongTyped var3, AnimationVariableSlotCallbackString.CallbackSetStrongTyped var4) {
      this.getGameVariablesInternal().setVariable(var1, var2, var3, var4);
   }

   public void setVariable(String var1, float var2, AnimationVariableSlotCallbackFloat.CallbackGetStrongTyped var3) {
      this.getGameVariablesInternal().setVariable(var1, var2, var3);
   }

   public void setVariable(String var1, float var2, AnimationVariableSlotCallbackFloat.CallbackGetStrongTyped var3, AnimationVariableSlotCallbackFloat.CallbackSetStrongTyped var4) {
      this.getGameVariablesInternal().setVariable(var1, var2, var3, var4);
   }

   public void setVariable(String var1, int var2, AnimationVariableSlotCallbackInt.CallbackGetStrongTyped var3) {
      this.getGameVariablesInternal().setVariable(var1, var2, var3);
   }

   public void setVariable(String var1, int var2, AnimationVariableSlotCallbackInt.CallbackGetStrongTyped var3, AnimationVariableSlotCallbackInt.CallbackSetStrongTyped var4) {
      this.getGameVariablesInternal().setVariable(var1, var2, var3, var4);
   }

   public void clearVariable(String var1) {
      this.getGameVariablesInternal().clearVariable(var1);
   }

   public void clearVariables() {
      this.getGameVariablesInternal().clearVariables();
   }

   public String getVariableString(String var1) {
      return this.getGameVariablesInternal().getVariableString(var1);
   }

   private String getFootInjuryType() {
      if (!(this instanceof IsoPlayer)) {
         return "";
      } else {
         BodyPart var1 = this.getBodyDamage().getBodyPart(BodyPartType.Foot_L);
         BodyPart var2 = this.getBodyDamage().getBodyPart(BodyPartType.Foot_R);
         if (!this.bRunning) {
            if (var1.haveBullet() || var1.getBurnTime() > 5.0F || var1.bitten() || var1.deepWounded() || var1.isSplint() || var1.getFractureTime() > 0.0F || var1.haveGlass()) {
               return "leftheavy";
            }

            if (var2.haveBullet() || var2.getBurnTime() > 5.0F || var2.bitten() || var2.deepWounded() || var2.isSplint() || var2.getFractureTime() > 0.0F || var2.haveGlass()) {
               return "rightheavy";
            }
         }

         if (!(var1.getScratchTime() > 5.0F) && !(var1.getCutTime() > 7.0F) && !(var1.getBurnTime() > 0.0F)) {
            if (!(var2.getScratchTime() > 5.0F) && !(var2.getCutTime() > 7.0F) && !(var2.getBurnTime() > 0.0F)) {
               return "";
            } else {
               return "rightlight";
            }
         } else {
            return "leftlight";
         }
      }
   }

   public float getVariableFloat(String var1, float var2) {
      return this.getGameVariablesInternal().getVariableFloat(var1, var2);
   }

   public boolean getVariableBoolean(String var1) {
      return this.getGameVariablesInternal().getVariableBoolean(var1);
   }

   public boolean getVariableBoolean(String var1, boolean var2) {
      return this.getGameVariablesInternal().getVariableBoolean(var1, var2);
   }

   public boolean isVariable(String var1, String var2) {
      return this.getGameVariablesInternal().isVariable(var1, var2);
   }

   public boolean containsVariable(String var1) {
      return this.getGameVariablesInternal().containsVariable(var1);
   }

   public IAnimationVariableSource getSubVariableSource(String var1) {
      if (var1.equals("GrappledTarget")) {
         return IGrappleable.getAnimatable(this.getGrapplingTarget());
      } else {
         return var1.equals("GrappledBy") ? IGrappleable.getAnimatable(this.getGrappledBy()) : null;
      }
   }

   public Iterable<IAnimationVariableSlot> getGameVariables() {
      return this.getGameVariablesInternal().getGameVariables();
   }

   private AnimationVariableSource getGameVariablesInternal() {
      return this.m_PlaybackGameVariables != null ? this.m_PlaybackGameVariables : this.m_GameVariables;
   }

   public AnimationVariableSource startPlaybackGameVariables() {
      if (this.m_PlaybackGameVariables != null) {
         DebugLog.General.error("Error! PlaybackGameVariables is already active.");
         return this.m_PlaybackGameVariables;
      } else {
         AnimationVariableSource var1 = new AnimationVariableSource();
         Iterator var2 = this.getGameVariables().iterator();

         while(var2.hasNext()) {
            IAnimationVariableSlot var3 = (IAnimationVariableSlot)var2.next();
            AnimationVariableType var4 = var3.getType();
            switch (var4) {
               case String:
                  var1.setVariable(var3.getKey(), var3.getValueString());
                  break;
               case Float:
                  var1.setVariable(var3.getKey(), var3.getValueFloat());
                  break;
               case Boolean:
                  var1.setVariable(var3.getKey(), var3.getValueBool());
               case Void:
                  break;
               default:
                  DebugLog.General.error("Error! Variable type not handled: %s", var4.toString());
            }
         }

         this.m_PlaybackGameVariables = var1;
         return this.m_PlaybackGameVariables;
      }
   }

   public void endPlaybackGameVariables(AnimationVariableSource var1) {
      if (this.m_PlaybackGameVariables != var1) {
         DebugLog.General.error("Error! Playback GameVariables do not match.");
      }

      this.m_PlaybackGameVariables = null;
   }

   public void playbackSetCurrentStateSnapshot(ActionStateSnapshot var1) {
      if (this.actionContext != null) {
         this.actionContext.setPlaybackStateSnapshot(var1);
      }
   }

   public ActionStateSnapshot playbackRecordCurrentStateSnapshot() {
      return this.actionContext == null ? null : this.actionContext.getPlaybackStateSnapshot();
   }

   public String GetVariable(String var1) {
      return this.getVariableString(var1);
   }

   public void SetVariable(String var1, String var2) {
      this.setVariable(var1, var2);
   }

   public void ClearVariable(String var1) {
      this.clearVariable(var1);
   }

   public void actionStateChanged(ActionContext var1) {
      for(int var2 = 0; var2 < IsoGameCharacter.L_actionStateChanged.stateNames.size(); ++var2) {
         DebugLog.AnimationDetailed.debugln("************* stateNames: %s", IsoGameCharacter.L_actionStateChanged.stateNames.get(var2));
      }

      ArrayList var8 = IsoGameCharacter.L_actionStateChanged.stateNames;
      PZArrayUtil.listConvert(var1.getChildStates(), var8, (var0) -> {
         return var0.getName();
      });

      for(int var3 = 0; var3 < IsoGameCharacter.L_actionStateChanged.stateNames.size(); ++var3) {
         DebugLog.AnimationDetailed.debugln("************* stateNames: %s", IsoGameCharacter.L_actionStateChanged.stateNames.get(var3));
      }

      this.advancedAnimator.SetState(var1.getCurrentStateName(), var8);

      try {
         ++this.stateMachine.activeStateChanged;
         State var9 = this.tryGetAIState(var1.getCurrentStateName());
         if (var9 == null) {
            var9 = this.defaultState;
         }

         ArrayList var4 = IsoGameCharacter.L_actionStateChanged.states;
         PZArrayUtil.listConvert(var1.getChildStates(), var4, this.m_aiStateMap, (var0, var1x) -> {
            return (State)var1x.get(var0.getName().toLowerCase());
         });
         this.stateMachine.changeState(var9, var4);
      } finally {
         --this.stateMachine.activeStateChanged;
      }

   }

   public boolean isFallOnFront() {
      return this.fallOnFront;
   }

   public void setFallOnFront(boolean var1) {
      this.fallOnFront = var1;
   }

   public boolean isHitFromBehind() {
      return this.hitFromBehind;
   }

   public void setHitFromBehind(boolean var1) {
      this.hitFromBehind = var1;
   }

   public boolean isKilledBySlicingWeapon() {
      IsoGameCharacter var1 = this.getAttackedBy();
      if (var1 == null) {
         return false;
      } else {
         HandWeapon var2 = var1.getAttackingWeapon();
         if (var2 == null) {
            return false;
         } else {
            return var2.getCategories().contains("LongBlade");
         }
      }
   }

   public void reportEvent(String var1) {
      this.actionContext.reportEvent(var1);
   }

   public void StartTimedActionAnim(String var1) {
      this.StartTimedActionAnim(var1, (String)null);
   }

   public void StartTimedActionAnim(String var1, String var2) {
      this.reportEvent(var1);
      if (var2 != null) {
         this.setVariable("TimedActionType", var2);
      }

      this.resetModelNextFrame();
   }

   public void StopTimedActionAnim() {
      this.clearVariable("TimedActionType");
      this.reportEvent("Event_TA_Exit");
      this.resetModelNextFrame();
   }

   public boolean hasHitReaction() {
      return !StringUtils.isNullOrEmpty(this.getHitReaction());
   }

   public String getHitReaction() {
      return this.hitReaction;
   }

   public void setHitReaction(String var1) {
      if (!StringUtils.equals(this.hitReaction, var1)) {
         this.hitReaction = var1;
      }

   }

   public void CacheEquipped() {
      this.cacheEquiped[0] = this.getPrimaryHandItem();
      this.cacheEquiped[1] = this.getSecondaryHandItem();
   }

   public InventoryItem GetPrimaryEquippedCache() {
      return this.cacheEquiped[0] != null && this.inventory.contains(this.cacheEquiped[0]) ? this.cacheEquiped[0] : null;
   }

   public InventoryItem GetSecondaryEquippedCache() {
      return this.cacheEquiped[1] != null && this.inventory.contains(this.cacheEquiped[1]) ? this.cacheEquiped[1] : null;
   }

   public void ClearEquippedCache() {
      this.cacheEquiped[0] = null;
      this.cacheEquiped[1] = null;
   }

   public boolean isBehind(IsoGameCharacter var1) {
      Vector2 var2 = tempVector2_1.set(this.getX(), this.getY());
      Vector2 var3 = tempVector2_2.set(var1.getX(), var1.getY());
      var3.x -= var2.x;
      var3.y -= var2.y;
      Vector2 var4 = var1.getForwardDirection();
      var3.normalize();
      var4.normalize();
      float var5 = var3.dot(var4);
      return (double)var5 > 0.6;
   }

   public void resetEquippedHandsModels() {
      if (!GameServer.bServer || ServerGUI.isCreated()) {
         if (this.hasActiveModel()) {
            ModelManager.instance.ResetEquippedNextFrame(this);
         }
      }
   }

   public AnimatorDebugMonitor getDebugMonitor() {
      return this.advancedAnimator.getDebugMonitor();
   }

   public void setDebugMonitor(AnimatorDebugMonitor var1) {
      this.advancedAnimator.setDebugMonitor(var1);
   }

   public boolean isAimAtFloor() {
      return this.bAimAtFloor;
   }

   public void setAimAtFloor(boolean var1) {
      this.bAimAtFloor = var1;
   }

   public boolean isDeferredMovementEnabled() {
      return this.m_deferredMovementEnabled;
   }

   public void setDeferredMovementEnabled(boolean var1) {
      this.m_deferredMovementEnabled = var1;
   }

   public String testDotSide(IsoMovingObject var1) {
      Vector2 var2 = this.getLookVector(IsoGameCharacter.l_testDotSide.v1);
      Vector2 var3 = IsoGameCharacter.l_testDotSide.v2.set(this.getX(), this.getY());
      Vector2 var4 = IsoGameCharacter.l_testDotSide.v3.set(var1.getX() - var3.x, var1.getY() - var3.y);
      var4.normalize();
      float var5 = Vector2.dot(var4.x, var4.y, var2.x, var2.y);
      if ((double)var5 > 0.7) {
         return "FRONT";
      } else if (var5 < 0.0F && (double)var5 < -0.5) {
         return "BEHIND";
      } else {
         float var6 = var1.getX();
         float var7 = var1.getY();
         float var8 = var3.x;
         float var9 = var3.y;
         float var10 = var3.x + var2.x;
         float var11 = var3.y + var2.y;
         float var12 = (var6 - var8) * (var11 - var9) - (var7 - var9) * (var10 - var8);
         return var12 > 0.0F ? "RIGHT" : "LEFT";
      }
   }

   public void addBasicPatch(BloodBodyPartType var1) {
      if (this instanceof IHumanVisual) {
         if (var1 == null) {
            var1 = BloodBodyPartType.FromIndex(Rand.Next(0, BloodBodyPartType.MAX.index()));
         }

         HumanVisual var2 = ((IHumanVisual)this).getHumanVisual();
         this.getItemVisuals(tempItemVisuals);
         BloodClothingType.addBasicPatch(var1, var2, tempItemVisuals);
         this.bUpdateModelTextures = true;
         this.bUpdateEquippedTextures = true;
         if (!GameServer.bServer && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
            LuaEventManager.triggerEvent("OnClothingUpdated", this);
         }

      }
   }

   public boolean addHole(BloodBodyPartType var1) {
      return this.addHole(var1, false);
   }

   public boolean addHole(BloodBodyPartType var1, boolean var2) {
      if (!(this instanceof IHumanVisual)) {
         return false;
      } else {
         if (var1 == null) {
            var1 = BloodBodyPartType.FromIndex(OutfitRNG.Next(0, BloodBodyPartType.MAX.index()));
         }

         HumanVisual var3 = ((IHumanVisual)this).getHumanVisual();
         this.getItemVisuals(tempItemVisuals);
         boolean var4 = BloodClothingType.addHole(var1, var3, tempItemVisuals, var2);
         this.bUpdateModelTextures = true;
         if (!GameServer.bServer && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
            LuaEventManager.triggerEvent("OnClothingUpdated", this);
            if (GameClient.bClient) {
               INetworkPacket.send(PacketTypes.PacketType.SyncInventory, this);
               INetworkPacket.send(PacketTypes.PacketType.SyncClothing, this);
               INetworkPacket.send(PacketTypes.PacketType.SyncVisuals, this);
            }
         }

         return var4;
      }
   }

   public void addDirt(BloodBodyPartType var1, Integer var2, boolean var3) {
      if (!(this instanceof IsoAnimal)) {
         HumanVisual var4 = ((IHumanVisual)this).getHumanVisual();
         if (var2 == null) {
            var2 = OutfitRNG.Next(5, 10);
         }

         boolean var5 = false;
         if (var1 == null) {
            var5 = true;
         }

         this.getItemVisuals(tempItemVisuals);

         for(int var6 = 0; var6 < var2; ++var6) {
            if (var5) {
               var1 = BloodBodyPartType.FromIndex(OutfitRNG.Next(0, BloodBodyPartType.MAX.index()));
            }

            BloodClothingType.addDirt(var1, var4, tempItemVisuals, var3);
         }

         this.bUpdateModelTextures = true;
         if (!GameServer.bServer && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
            LuaEventManager.triggerEvent("OnClothingUpdated", this);
         }

      }
   }

   public void addLotsOfDirt(BloodBodyPartType var1, Integer var2, boolean var3) {
      if (!(this instanceof IsoAnimal)) {
         HumanVisual var4 = ((IHumanVisual)this).getHumanVisual();
         if (var2 == null) {
            var2 = OutfitRNG.Next(5, 10);
         }

         boolean var5 = false;
         if (var1 == null) {
            var5 = true;
         }

         this.getItemVisuals(tempItemVisuals);

         for(int var6 = 0; var6 < var2; ++var6) {
            if (var5) {
               var1 = BloodBodyPartType.FromIndex(OutfitRNG.Next(0, BloodBodyPartType.MAX.index()));
            }

            BloodClothingType.addDirt(var1, Rand.Next(0.01F, 1.0F), var4, tempItemVisuals, var3);
         }

         this.bUpdateModelTextures = true;
         if (!GameServer.bServer && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
            LuaEventManager.triggerEvent("OnClothingUpdated", this);
         }

      }
   }

   public void addBlood(BloodBodyPartType var1, boolean var2, boolean var3, boolean var4) {
      if (!(this instanceof IsoAnimal)) {
         HumanVisual var5 = ((IHumanVisual)this).getHumanVisual();
         int var6 = 1;
         boolean var7 = false;
         if (var1 == null) {
            var7 = true;
         }

         if (this.getPrimaryHandItem() instanceof HandWeapon) {
            var6 = ((HandWeapon)this.getPrimaryHandItem()).getSplatNumber();
            if (OutfitRNG.Next(15) < this.getWeaponLevel()) {
               --var6;
            }
         }

         if (var3) {
            var6 = 20;
         }

         if (var2) {
            var6 = 5;
         }

         if (this.isZombie()) {
            var6 += 8;
         }

         this.getItemVisuals(tempItemVisuals);

         for(int var8 = 0; var8 < var6; ++var8) {
            if (var7) {
               var1 = BloodBodyPartType.FromIndex(OutfitRNG.Next(0, BloodBodyPartType.MAX.index()));
               if (this.getPrimaryHandItem() != null && this.getPrimaryHandItem() instanceof HandWeapon) {
                  HandWeapon var9 = (HandWeapon)this.getPrimaryHandItem();
                  if (var9.getBloodLevel() < 1.0F) {
                     float var10 = var9.getBloodLevel() + 0.02F;
                     var9.setBloodLevel(var10);
                     this.bUpdateEquippedTextures = true;
                  }
               }
            }

            BloodClothingType.addBlood(var1, var5, tempItemVisuals, var4);
         }

         this.bUpdateModelTextures = true;
         if (!GameServer.bServer && this instanceof IsoPlayer && ((IsoPlayer)this).isLocalPlayer()) {
            LuaEventManager.triggerEvent("OnClothingUpdated", this);
         }

      }
   }

   public boolean bodyPartHasTag(Integer var1, String var2) {
      this.getItemVisuals(tempItemVisuals);

      for(int var3 = tempItemVisuals.size() - 1; var3 >= 0; --var3) {
         ItemVisual var4 = (ItemVisual)tempItemVisuals.get(var3);
         Item var5 = var4.getScriptItem();
         if (var5 != null) {
            ArrayList var6 = var5.getBloodClothingType();
            if (var6 != null) {
               ArrayList var7 = BloodClothingType.getCoveredParts(var6);
               if (var7 != null) {
                  InventoryItem var8 = var4.getInventoryItem();
                  if (var8 == null) {
                     var8 = InventoryItemFactory.CreateItem(var4.getItemType());
                     if (var8 == null) {
                        continue;
                     }
                  }

                  for(int var9 = 0; var9 < var7.size(); ++var9) {
                     if (var8 instanceof Clothing && ((BloodBodyPartType)var7.get(var9)).index() == var1 && var4.getHole((BloodBodyPartType)var7.get(var9)) == 0.0F && var8.hasTag(var2)) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   public boolean bodyPartIsSpiked(Integer var1) {
      return this.bodyPartHasTag(var1, "Spiked");
   }

   public boolean bodyPartIsSpikedBehind(Integer var1) {
      return this.bodyPartHasTag(var1, "Spiked");
   }

   public float getBodyPartClothingDefense(Integer var1, boolean var2, boolean var3) {
      float var4 = 0.0F;
      this.getItemVisuals(tempItemVisuals);

      for(int var5 = tempItemVisuals.size() - 1; var5 >= 0; --var5) {
         ItemVisual var6 = (ItemVisual)tempItemVisuals.get(var5);
         Item var7 = var6.getScriptItem();
         if (var7 != null) {
            ArrayList var8 = var7.getBloodClothingType();
            if (var8 != null) {
               ArrayList var9 = BloodClothingType.getCoveredParts(var8);
               if (var9 != null) {
                  InventoryItem var10 = var6.getInventoryItem();
                  if (var10 == null) {
                     var10 = InventoryItemFactory.CreateItem(var6.getItemType());
                     if (var10 == null) {
                        continue;
                     }
                  }

                  for(int var11 = 0; var11 < var9.size(); ++var11) {
                     if (var10 instanceof Clothing && ((BloodBodyPartType)var9.get(var11)).index() == var1 && var6.getHole((BloodBodyPartType)var9.get(var11)) == 0.0F) {
                        Clothing var12 = (Clothing)var10;
                        var4 += var12.getDefForPart((BloodBodyPartType)var9.get(var11), var2, var3);
                        break;
                     }
                  }
               }
            }
         }
      }

      var4 = Math.min(100.0F, var4);
      return var4;
   }

   public boolean isBumped() {
      return !StringUtils.isNullOrWhitespace(this.getBumpType());
   }

   public boolean isBumpDone() {
      return this.m_isBumpDone;
   }

   public void setBumpDone(boolean var1) {
      this.m_isBumpDone = var1;
   }

   public boolean isBumpFall() {
      return this.m_bumpFall;
   }

   public void setBumpFall(boolean var1) {
      this.m_bumpFall = var1;
   }

   public boolean isBumpStaggered() {
      return this.m_bumpStaggered;
   }

   public void setBumpStaggered(boolean var1) {
      this.m_bumpStaggered = var1;
   }

   public String getBumpType() {
      return this.bumpType;
   }

   public void setBumpType(String var1) {
      if (StringUtils.equalsIgnoreCase(this.bumpType, var1)) {
         this.bumpType = var1;
      } else {
         boolean var2 = this.isBumped();
         this.bumpType = var1;
         boolean var3 = this.isBumped();
         if (var3 != var2) {
            this.setBumpStaggered(var3);
         }

      }
   }

   public String getBumpFallType() {
      return this.m_bumpFallType;
   }

   public void setBumpFallType(String var1) {
      this.m_bumpFallType = var1;
   }

   public IsoGameCharacter getBumpedChr() {
      return this.bumpedChr;
   }

   public void setBumpedChr(IsoGameCharacter var1) {
      this.bumpedChr = var1;
   }

   public long getLastBump() {
      return this.lastBump;
   }

   public void setLastBump(long var1) {
      this.lastBump = var1;
   }

   public boolean isSitOnGround() {
      return this.sitOnGround;
   }

   public void setSitOnGround(boolean var1) {
      this.sitOnGround = var1;
   }

   public boolean isSittingOnFurniture() {
      return this.m_isSitOnFurniture;
   }

   public void setSittingOnFurniture(boolean var1) {
      this.m_isSitOnFurniture = var1;
   }

   public IsoObject getSitOnFurnitureObject() {
      return this.m_sitOnFurnitureObject;
   }

   public void setSitOnFurnitureObject(IsoObject var1) {
      this.m_sitOnFurnitureObject = var1;
   }

   public IsoDirections getSitOnFurnitureDirection() {
      return this.m_sitOnFurnitureDirection;
   }

   public void setSitOnFurnitureDirection(IsoDirections var1) {
      this.m_sitOnFurnitureDirection = var1;
   }

   public boolean isSitOnFurnitureObject(IsoObject var1) {
      IsoObject var2 = this.getSitOnFurnitureObject();
      if (var2 == null) {
         return false;
      } else {
         return var1 == var2 ? true : var2.isConnectedSpriteGridObject(var1);
      }
   }

   public boolean shouldIgnoreCollisionWithSquare(IsoGridSquare var1) {
      if (this.getSitOnFurnitureObject() != null && this.getSitOnFurnitureObject().getSquare() == var1) {
         return true;
      } else {
         return this.hasPath() && this.getPathFindBehavior2().shouldIgnoreCollisionWithSquare(var1);
      }
   }

   public boolean canStandAt(float var1, float var2, float var3) {
      int var4 = 17;
      boolean var5 = false;
      if (var5) {
         var4 |= 2;
      }

      return PolygonalMap2.instance.canStandAt(var1, var2, (int)var3, (BaseVehicle)null, var4);
   }

   public String getUID() {
      return this.m_UID;
   }

   protected void clearAIStateMap() {
      this.m_aiStateMap.clear();
   }

   protected void registerAIState(String var1, State var2) {
      this.m_aiStateMap.put(var1.toLowerCase(Locale.ENGLISH), var2);
   }

   public State tryGetAIState(String var1) {
      return (State)this.m_aiStateMap.get(var1.toLowerCase(Locale.ENGLISH));
   }

   public boolean isRunning() {
      return this.getMoodles() != null && this.getMoodles().getMoodleLevel(MoodleType.Endurance) >= 3 ? false : this.bRunning;
   }

   public void setRunning(boolean var1) {
      this.bRunning = var1;
   }

   public boolean isSprinting() {
      return this.bSprinting && !this.canSprint() ? false : this.bSprinting;
   }

   public void setSprinting(boolean var1) {
      this.bSprinting = var1;
   }

   public boolean canSprint() {
      if (this instanceof IsoPlayer && !((IsoPlayer)this).isAllowSprint()) {
         return false;
      } else if ("Tutorial".equals(Core.GameMode)) {
         return true;
      } else {
         InventoryItem var1 = this.getPrimaryHandItem();
         if (var1 != null && var1.isEquippedNoSprint()) {
            return false;
         } else {
            var1 = this.getSecondaryHandItem();
            if (var1 != null && var1.isEquippedNoSprint()) {
               return false;
            } else {
               return this.getMoodles() == null || this.getMoodles().getMoodleLevel(MoodleType.Endurance) < 2;
            }
         }
      }
   }

   public void postUpdateModelTextures() {
      this.bUpdateModelTextures = true;
   }

   public ModelInstanceTextureCreator getTextureCreator() {
      return this.textureCreator;
   }

   public void setTextureCreator(ModelInstanceTextureCreator var1) {
      this.textureCreator = var1;
   }

   public void postUpdateEquippedTextures() {
      this.bUpdateEquippedTextures = true;
   }

   public ArrayList<ModelInstance> getReadyModelData() {
      return this.readyModelData;
   }

   public boolean getIgnoreMovement() {
      return this.ignoreMovement;
   }

   public void setIgnoreMovement(boolean var1) {
      if (this instanceof IsoPlayer && var1) {
         ((IsoPlayer)this).networkAI.needToUpdate();
      }

      this.ignoreMovement = var1;
   }

   public boolean isAutoWalk() {
      return this.bAutoWalk;
   }

   public void setAutoWalk(boolean var1) {
      this.bAutoWalk = var1;
   }

   public void setAutoWalkDirection(Vector2 var1) {
      this.autoWalkDirection.set(var1);
   }

   public Vector2 getAutoWalkDirection() {
      return this.autoWalkDirection;
   }

   public boolean isSneaking() {
      return this.getVariableFloat("WalkInjury", 0.0F) > 0.5F ? false : this.bSneaking;
   }

   public void setSneaking(boolean var1) {
      this.bSneaking = var1;
   }

   public GameCharacterAIBrain getGameCharacterAIBrain() {
      return this.ai.brain;
   }

   public float getMoveDelta() {
      return this.m_moveDelta;
   }

   public void setMoveDelta(float var1) {
      this.m_moveDelta = var1;
   }

   public float getTurnDelta() {
      if (this.isSprinting()) {
         return this.m_turnDeltaSprinting;
      } else {
         return this.isRunning() ? this.m_turnDeltaRunning : this.m_turnDeltaNormal;
      }
   }

   public void setTurnDelta(float var1) {
      this.m_turnDeltaNormal = var1;
   }

   public float getChopTreeSpeed() {
      return (this.Traits.Axeman.isSet() ? 1.25F : 1.0F) * GameTime.getAnimSpeedFix();
   }

   public boolean testDefense(IsoZombie var1) {
      if (this.testDotSide(var1).equals("FRONT") && !var1.bCrawling && this.getSurroundingAttackingZombies() <= 3) {
         int var2 = 0;
         if ("KnifeDeath".equals(this.getVariableString("ZombieHitReaction"))) {
            var2 += 30;
         }

         var2 += this.getWeaponLevel() * 3;
         var2 += this.getPerkLevel(PerkFactory.Perks.Fitness) * 2;
         var2 += this.getPerkLevel(PerkFactory.Perks.Strength) * 2;
         var2 -= this.getSurroundingAttackingZombies() * 5;
         var2 -= this.getMoodles().getMoodleLevel(MoodleType.Endurance) * 2;
         var2 -= this.getMoodles().getMoodleLevel(MoodleType.HeavyLoad) * 2;
         var2 -= this.getMoodles().getMoodleLevel(MoodleType.Tired) * 3;
         var2 -= this.getMoodles().getMoodleLevel(MoodleType.Drunk) * 2;
         if (SandboxOptions.instance.Lore.Strength.getValue() == 1) {
            var2 -= 7;
         }

         if (SandboxOptions.instance.Lore.Strength.getValue() == 3) {
            var2 += 7;
         }

         if (Rand.Next(100) < var2) {
            this.setAttackedBy(var1);
            this.setHitReaction(var1.getVariableString("PlayerHitReaction") + "Defended");
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public int getSurroundingAttackingZombies() {
      return this.getSurroundingAttackingZombies(false);
   }

   public int getSurroundingAttackingZombies(boolean var1) {
      movingStatic.clear();
      IsoGridSquare var2 = this.getCurrentSquare();
      if (var2 == null) {
         return 0;
      } else {
         movingStatic.addAll(var2.getMovingObjects());
         if (var2.n != null) {
            movingStatic.addAll(var2.n.getMovingObjects());
         }

         if (var2.s != null) {
            movingStatic.addAll(var2.s.getMovingObjects());
         }

         if (var2.e != null) {
            movingStatic.addAll(var2.e.getMovingObjects());
         }

         if (var2.w != null) {
            movingStatic.addAll(var2.w.getMovingObjects());
         }

         if (var2.nw != null) {
            movingStatic.addAll(var2.nw.getMovingObjects());
         }

         if (var2.sw != null) {
            movingStatic.addAll(var2.sw.getMovingObjects());
         }

         if (var2.se != null) {
            movingStatic.addAll(var2.se.getMovingObjects());
         }

         if (var2.ne != null) {
            movingStatic.addAll(var2.ne.getMovingObjects());
         }

         int var3 = 0;

         for(int var4 = 0; var4 < movingStatic.size(); ++var4) {
            IsoZombie var5 = (IsoZombie)Type.tryCastTo((IsoMovingObject)movingStatic.get(var4), IsoZombie.class);
            if (var5 != null && var5.target == this && !(this.DistToSquared(var5) >= 0.80999994F) && (!var5.isCrawling() || var1) && (var5.isCurrentState(AttackState.instance()) || var5.isCurrentState(AttackNetworkState.instance()) || var5.isCurrentState(LungeState.instance()) || var5.isCurrentState(LungeNetworkState.instance()))) {
               ++var3;
            }
         }

         return var3;
      }
   }

   public boolean checkIsNearVehicle() {
      for(int var1 = 0; var1 < IsoWorld.instance.CurrentCell.getVehicles().size(); ++var1) {
         BaseVehicle var2 = (BaseVehicle)IsoWorld.instance.CurrentCell.getVehicles().get(var1);
         if (var2.DistTo(this) < 3.5F) {
            if (this.bSneaking) {
               this.setVariable("nearWallCrouching", true);
            }

            return true;
         }
      }

      return false;
   }

   public float checkIsNearWall() {
      if (this.bSneaking && this.getCurrentSquare() != null) {
         IsoGridSquare var1 = this.getCurrentSquare().nav[IsoDirections.N.index()];
         IsoGridSquare var2 = this.getCurrentSquare().nav[IsoDirections.S.index()];
         IsoGridSquare var3 = this.getCurrentSquare().nav[IsoDirections.E.index()];
         IsoGridSquare var4 = this.getCurrentSquare().nav[IsoDirections.W.index()];
         float var5 = 0.0F;
         float var6 = 0.0F;
         if (var1 != null) {
            var5 = var1.getGridSneakModifier(true);
            if (var5 > 1.0F) {
               this.setVariable("nearWallCrouching", true);
               return var5;
            }
         }

         if (var2 != null) {
            var5 = var2.getGridSneakModifier(false);
            var6 = var2.getGridSneakModifier(true);
            if (var5 > 1.0F || var6 > 1.0F) {
               this.setVariable("nearWallCrouching", true);
               return var5 > 1.0F ? var5 : var6;
            }
         }

         if (var3 != null) {
            var5 = var3.getGridSneakModifier(false);
            var6 = var3.getGridSneakModifier(true);
            if (var5 > 1.0F || var6 > 1.0F) {
               this.setVariable("nearWallCrouching", true);
               return var5 > 1.0F ? var5 : var6;
            }
         }

         if (var4 != null) {
            var5 = var4.getGridSneakModifier(false);
            var6 = var4.getGridSneakModifier(true);
            if (var5 > 1.0F || var6 > 1.0F) {
               this.setVariable("nearWallCrouching", true);
               return var5 > 1.0F ? var5 : var6;
            }
         }

         var5 = this.getCurrentSquare().getGridSneakModifier(false);
         if (var5 > 1.0F) {
            this.setVariable("nearWallCrouching", true);
            return var5;
         } else if (this instanceof IsoPlayer && ((IsoPlayer)this).isNearVehicle()) {
            this.setVariable("nearWallCrouching", true);
            return 6.0F;
         } else {
            this.setVariable("nearWallCrouching", false);
            return 0.0F;
         }
      } else {
         this.setVariable("nearWallCrouching", false);
         return 0.0F;
      }
   }

   public float getBeenSprintingFor() {
      return this.BeenSprintingFor;
   }

   public void setBeenSprintingFor(float var1) {
      if (var1 < 0.0F) {
         var1 = 0.0F;
      }

      if (var1 > 100.0F) {
         var1 = 100.0F;
      }

      this.BeenSprintingFor = var1;
   }

   public boolean isHideWeaponModel() {
      return this.hideWeaponModel;
   }

   public void setHideWeaponModel(boolean var1) {
      if (this.hideWeaponModel != var1) {
         this.hideWeaponModel = var1;
         this.resetEquippedHandsModels();
      }

   }

   public void setIsAiming(boolean var1) {
      this.m_isAiming = var1;
   }

   /** @deprecated */
   @Deprecated
   public boolean IsAiming() {
      return this.isAiming();
   }

   public void setFireMode(String var1) {
   }

   public String getFireMode() {
      InventoryItem var2 = this.leftHandItem;
      String var10000;
      if (var2 instanceof HandWeapon var1) {
         var10000 = var1.getFireMode();
      } else {
         var10000 = "";
      }

      return var10000;
   }

   public boolean isAiming() {
      if (this.isNPC) {
         return this.NPCGetAiming();
      } else if (this.isPerformingHostileAnimation()) {
         return true;
      } else {
         return this.isIgnoringAimingInput() ? false : this.m_isAiming;
      }
   }

   public boolean isTwisting() {
      float var1 = this.getTargetTwist();
      float var2 = PZMath.abs(var1);
      boolean var3 = var2 > 1.0F;
      return var3;
   }

   public boolean allowsTwist() {
      return false;
   }

   public float getShoulderTwistWeight() {
      if (this.isAiming()) {
         return 1.0F;
      } else {
         return this.isSneaking() ? 0.6F : 0.75F;
      }
   }

   public void resetBeardGrowingTime() {
      this.beardGrowTiming = (float)this.getHoursSurvived();
      if (GameClient.bClient && this instanceof IsoPlayer) {
         GameClient.instance.sendVisual((IsoPlayer)this);
      }

   }

   public void resetHairGrowingTime() {
      this.hairGrowTiming = (float)this.getHoursSurvived();
      if (GameClient.bClient && this instanceof IsoPlayer) {
         GameClient.instance.sendVisual((IsoPlayer)this);
      }

   }

   public void fallenOnKnees() {
      this.fallenOnKnees(false);
   }

   public void fallenOnKnees(boolean var1) {
      if (!(this instanceof IsoPlayer) || ((IsoPlayer)this).isLocalPlayer()) {
         if (!this.isInvincible()) {
            this.helmetFall(var1);
            BloodBodyPartType var2 = BloodBodyPartType.FromIndex(Rand.Next(BloodBodyPartType.Hand_L.index(), BloodBodyPartType.Torso_Upper.index()));
            if (Rand.NextBool(2)) {
               var2 = BloodBodyPartType.FromIndex(Rand.Next(BloodBodyPartType.UpperLeg_L.index(), BloodBodyPartType.Back.index()));
            }

            for(int var3 = 0; var3 < 4; ++var3) {
               BloodBodyPartType var4 = BloodBodyPartType.FromIndex(Rand.Next(BloodBodyPartType.Hand_L.index(), BloodBodyPartType.Torso_Upper.index()));
               if (Rand.NextBool(2)) {
                  var4 = BloodBodyPartType.FromIndex(Rand.Next(BloodBodyPartType.UpperLeg_L.index(), BloodBodyPartType.Back.index()));
               }

               this.addDirt(var4, Rand.Next(2, 6), false);
            }

            if (DebugOptions.instance.Character.Debug.AlwaysTripOverFence.getValue()) {
               this.dropHandItems();
            }

            if (Rand.NextBool(4 + this.getPerkLevel(PerkFactory.Perks.Nimble))) {
               if (Rand.NextBool(4)) {
                  this.dropHandItems();
               }

               this.addHole(var2);
               this.addBlood(var2, true, false, false);
               BodyPart var5 = this.getBodyDamage().getBodyPart(BodyPartType.FromIndex(var2.index()));
               float var6 = this.getBodyPartClothingDefense(var2.index(), false, false);
               if ((float)Rand.Next(100) < var6) {
                  if (var5.scratched()) {
                     var5.generateDeepWound();
                  } else {
                     var5.setScratched(true, true);
                  }
               }

            }
         }
      }
   }

   public void addVisualDamage(String var1) {
      this.addBodyVisualFromItemType("Base." + var1);
   }

   public ItemVisual addBodyVisualFromItemType(String var1) {
      IHumanVisual var2 = (IHumanVisual)Type.tryCastTo(this, IHumanVisual.class);
      if (var2 == null) {
         return null;
      } else {
         Item var3 = ScriptManager.instance.getItem(var1);
         if (var3 == null) {
            return null;
         } else {
            ClothingItem var4 = var3.getClothingItemAsset();
            if (var4 == null) {
               return null;
            } else {
               ClothingItemReference var5 = new ClothingItemReference();
               var5.itemGUID = var4.m_GUID;
               var5.randomize();
               ItemVisual var6 = new ItemVisual();
               var6.setItemType(var1);
               var6.synchWithOutfit(var5);
               if (!this.isDuplicateBodyVisual(var6)) {
                  ItemVisuals var7 = var2.getHumanVisual().getBodyVisuals();
                  var7.add(var6);
                  return var6;
               } else {
                  return null;
               }
            }
         }
      }
   }

   protected boolean isDuplicateBodyVisual(ItemVisual var1) {
      IHumanVisual var2 = (IHumanVisual)Type.tryCastTo(this, IHumanVisual.class);
      if (var2 == null) {
         return false;
      } else {
         ItemVisuals var3 = var2.getHumanVisual().getBodyVisuals();

         for(int var4 = 0; var4 < var3.size(); ++var4) {
            ItemVisual var5 = (ItemVisual)var3.get(var4);
            if (var1.getClothingItemName().equals(var5.getClothingItemName()) && var1.getTextureChoice() == var5.getTextureChoice() && var1.getBaseTexture() == var5.getBaseTexture()) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean isCriticalHit() {
      return this.isCrit;
   }

   public void setCriticalHit(boolean var1) {
      this.isCrit = var1;
   }

   public float getRunSpeedModifier() {
      return this.runSpeedModifier;
   }

   public void startMuzzleFlash() {
   }

   public boolean isMuzzleFlash() {
      if (Core.bDebug && DebugOptions.instance.Model.Render.Muzzleflash.getValue()) {
         return true;
      } else {
         return this.muzzleFlashDuration > System.currentTimeMillis() - 50L;
      }
   }

   public boolean isNPC() {
      return this.isNPC;
   }

   public void setNPC(boolean var1) {
      this.ai.setNPC(var1);
      this.isNPC = var1;
   }

   public void NPCSetRunning(boolean var1) {
      this.ai.brain.HumanControlVars.bRunning = var1;
   }

   public boolean NPCGetRunning() {
      return this.ai.brain.HumanControlVars.bRunning;
   }

   public void NPCSetJustMoved(boolean var1) {
      this.ai.brain.HumanControlVars.JustMoved = var1;
   }

   public void NPCSetAiming(boolean var1) {
      this.ai.brain.HumanControlVars.bAiming = var1;
   }

   public boolean NPCGetAiming() {
      return this.ai.brain.HumanControlVars.bAiming;
   }

   public void NPCSetAttack(boolean var1) {
      this.ai.brain.HumanControlVars.initiateAttack = var1;
   }

   public void NPCSetMelee(boolean var1) {
      this.ai.brain.HumanControlVars.bMelee = var1;
   }

   public void setMetabolicTarget(Metabolics var1) {
      if (var1 != null) {
         this.setMetabolicTarget(var1.getMet());
      }

   }

   public void setMetabolicTarget(float var1) {
      if (this.getBodyDamage() != null && this.getBodyDamage().getThermoregulator() != null) {
         this.getBodyDamage().getThermoregulator().setMetabolicTarget(var1);
      }

   }

   public double getThirstMultiplier() {
      return this.getBodyDamage() != null && this.getBodyDamage().getThermoregulator() != null ? this.getBodyDamage().getThermoregulator().getFluidsMultiplier() : 1.0;
   }

   public double getHungerMultiplier() {
      return 1.0;
   }

   public double getFatiqueMultiplier() {
      return this.getBodyDamage() != null && this.getBodyDamage().getThermoregulator() != null ? this.getBodyDamage().getThermoregulator().getFatigueMultiplier() : 1.0;
   }

   public float getTimedActionTimeModifier() {
      return 1.0F;
   }

   public boolean addHoleFromZombieAttacks(BloodBodyPartType var1, boolean var2) {
      this.getItemVisuals(tempItemVisuals);
      ItemVisual var3 = null;

      for(int var4 = tempItemVisuals.size() - 1; var4 >= 0; --var4) {
         ItemVisual var5 = (ItemVisual)tempItemVisuals.get(var4);
         Item var6 = var5.getScriptItem();
         if (var6 != null) {
            ArrayList var7 = var6.getBloodClothingType();
            if (var7 != null) {
               ArrayList var8 = BloodClothingType.getCoveredParts(var7);

               for(int var9 = 0; var9 < var8.size(); ++var9) {
                  BloodBodyPartType var10 = (BloodBodyPartType)var8.get(var9);
                  if (var1 == var10) {
                     var3 = var5;
                     break;
                  }
               }

               if (var3 != null) {
                  break;
               }
            }
         }
      }

      float var11 = 0.0F;
      boolean var12 = false;
      if (var3 != null && var3.getInventoryItem() != null && var3.getInventoryItem() instanceof Clothing) {
         Clothing var13 = (Clothing)var3.getInventoryItem();
         var13.getPatchType(var1);
         var11 = Math.max(30.0F, 100.0F - var13.getDefForPart(var1, !var2, false) / 1.5F);
      }

      if ((float)Rand.Next(100) < var11) {
         boolean var14 = this.addHole(var1);
         if (var14) {
            this.getEmitter().playSoundImpl("ZombieRipClothing", (IsoObject)null);
         }

         var12 = true;
      }

      return var12;
   }

   protected void updateBandages() {
      s_bandages.update(this);
   }

   public float getTotalBlood() {
      float var1 = 0.0F;
      if (this.getWornItems() == null) {
         return var1;
      } else {
         for(int var2 = 0; var2 < this.getWornItems().size(); ++var2) {
            InventoryItem var3 = this.getWornItems().get(var2).getItem();
            if (var3 instanceof Clothing) {
               var1 += ((Clothing)var3).getBloodlevel();
            }
         }

         if (this.getPrimaryHandItem() != null && !this.getWornItems().contains(this.getPrimaryHandItem())) {
            var1 += this.getPrimaryHandItem().getBloodLevelAdjustedHigh();
         }

         if (this.getSecondaryHandItem() != null && this.getPrimaryHandItem() != this.getSecondaryHandItem() && !this.getWornItems().contains(this.getSecondaryHandItem())) {
            var1 += this.getSecondaryHandItem().getBloodLevelAdjustedHigh();
         }

         var1 += ((HumanVisual)this.getVisual()).getTotalBlood();
         return var1;
      }
   }

   public void attackFromWindowsLunge(IsoZombie var1) {
      if (!(this.lungeFallTimer > 0.0F) && PZMath.fastfloor(this.getZ()) == PZMath.fastfloor(var1.getZ()) && !var1.isDead() && this.getCurrentSquare() != null && !this.getCurrentSquare().isDoorBlockedTo(var1.getCurrentSquare()) && !this.getCurrentSquare().isWallTo(var1.getCurrentSquare()) && !this.getCurrentSquare().isWindowTo(var1.getCurrentSquare())) {
         if (this.getVehicle() == null) {
            boolean var2 = this.DoSwingCollisionBoneCheck(var1, var1.getAnimationPlayer().getSkinningBoneIndex("Bip01_R_Hand", -1), 1.0F);
            if (var2) {
               var1.playSound("ZombieCrawlLungeHit");
               this.lungeFallTimer = 200.0F;
               this.setIsAiming(false);
               boolean var3 = false;
               int var4 = 30;
               var4 += this.getMoodles().getMoodleLevel(MoodleType.Drunk) * 3;
               var4 += this.getMoodles().getMoodleLevel(MoodleType.Endurance) * 3;
               var4 += this.getMoodles().getMoodleLevel(MoodleType.HeavyLoad) * 5;
               var4 -= this.getPerkLevel(PerkFactory.Perks.Fitness) * 2;
               var4 -= this.getPerkLevel(PerkFactory.Perks.Nimble);
               BodyPart var5 = this.getBodyDamage().getBodyPart(BodyPartType.Torso_Lower);
               if (var5.getAdditionalPain(true) > 20.0F) {
                  var4 = (int)((float)var4 + (var5.getAdditionalPain(true) - 20.0F) / 10.0F);
               }

               if (this.Traits.Clumsy.isSet()) {
                  var4 += 10;
               }

               if (this.Traits.Graceful.isSet()) {
                  var4 -= 10;
               }

               if (this.Traits.VeryUnderweight.isSet()) {
                  var4 += 20;
               }

               if (this.Traits.Underweight.isSet()) {
                  var4 += 10;
               }

               if (this.Traits.Obese.isSet()) {
                  var4 -= 10;
               }

               if (this.Traits.Overweight.isSet()) {
                  var4 -= 5;
               }

               var4 = Math.max(5, var4);
               this.clearVariable("BumpFallType");
               this.setBumpType("stagger");
               if (Rand.Next(100) < var4) {
                  var3 = true;
               }

               this.setBumpDone(false);
               this.setBumpFall(var3);
               if (var1.isBehind(this)) {
                  this.setBumpFallType("pushedBehind");
               } else {
                  this.setBumpFallType("pushedFront");
               }

               this.actionContext.reportEvent("wasBumped");
            }
         }
      }
   }

   public boolean DoSwingCollisionBoneCheck(IsoGameCharacter var1, int var2, float var3) {
      Model.BoneToWorldCoords(var1, var2, tempVectorBonePos);
      float var4 = IsoUtils.DistanceToSquared(tempVectorBonePos.x, tempVectorBonePos.y, this.getX(), this.getY());
      return var4 < var3 * var3;
   }

   public boolean isInvincible() {
      return this.invincible;
   }

   public void setInvincible(boolean var1) {
      if (!Role.haveCapability(this, Capability.ToggleInvincibleHimself)) {
         this.invincible = false;
      } else {
         this.invincible = var1;
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
                     if (var9.getScript() != null && PZMath.fastfloor(this.getZ()) == PZMath.fastfloor(var9.getZ()) && (!(this instanceof IsoPlayer) || !((IsoPlayer)this).isLocalPlayer() || var9.getTargetAlpha(((IsoPlayer)this).PlayerIndex) != 0.0F) && !(this.DistToSquared((float)PZMath.fastfloor(var9.getX()), (float)PZMath.fastfloor(var9.getY())) >= 16.0F)) {
                        return var9;
                     }
                  }
               }
            }
         }

         return null;
      }
   }

   public boolean isNearSirenVehicle() {
      if (this.getVehicle() != null) {
         return false;
      } else {
         int var1 = (PZMath.fastfloor(this.getX()) - 5) / 8 - 1;
         int var2 = (PZMath.fastfloor(this.getY()) - 5) / 8 - 1;
         int var3 = (int)Math.ceil((double)((this.getX() + 5.0F) / 8.0F)) + 1;
         int var4 = (int)Math.ceil((double)((this.getY() + 5.0F) / 8.0F)) + 1;

         for(int var5 = var2; var5 < var4; ++var5) {
            for(int var6 = var1; var6 < var3; ++var6) {
               IsoChunk var7 = GameServer.bServer ? ServerMap.instance.getChunk(var6, var5) : IsoWorld.instance.CurrentCell.getChunk(var6, var5);
               if (var7 != null) {
                  for(int var8 = 0; var8 < var7.vehicles.size(); ++var8) {
                     BaseVehicle var9 = (BaseVehicle)var7.vehicles.get(var8);
                     if (var9.getScript() != null && PZMath.fastfloor(this.getZ()) == PZMath.fastfloor(var9.getZ()) && (!(this instanceof IsoPlayer) || !((IsoPlayer)this).isLocalPlayer() || var9.getTargetAlpha(((IsoPlayer)this).PlayerIndex) != 0.0F) && !(this.DistToSquared((float)PZMath.fastfloor(var9.getX()), (float)PZMath.fastfloor(var9.getY())) >= 25.0F) && var9.isSirening()) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   private IsoGridSquare getSolidFloorAt(int var1, int var2, int var3) {
      while(var3 >= 0) {
         IsoGridSquare var4 = this.getCell().getGridSquare(var1, var2, var3);
         if (var4 != null && var4.TreatAsSolidFloor()) {
            return var4;
         }

         --var3;
      }

      return null;
   }

   public void dropHeavyItems() {
      IsoGridSquare var1 = this.getCurrentSquare();
      if (var1 != null) {
         InventoryItem var2 = this.getPrimaryHandItem();
         InventoryItem var3 = this.getSecondaryHandItem();
         if (var2 != null || var3 != null) {
            var1 = this.getSolidFloorAt(var1.x, var1.y, var1.z);
            if (var1 != null) {
               boolean var4 = var2 == var3;
               float var5;
               float var6;
               float var7;
               if (this.isHeavyItem(var2)) {
                  var5 = Rand.Next(0.1F, 0.9F);
                  var6 = Rand.Next(0.1F, 0.9F);
                  var7 = var1.getApparentZ(var5, var6) - (float)var1.getZ();
                  this.setPrimaryHandItem((InventoryItem)null);
                  this.getInventory().DoRemoveItem(var2);
                  var1.AddWorldInventoryItem(var2, var5, var6, var7);
                  LuaEventManager.triggerEvent("OnContainerUpdate");
                  LuaEventManager.triggerEvent("onItemFall", var2);
                  this.playDropItemSound(var2);
               }

               if (this.isHeavyItem(var3)) {
                  this.setSecondaryHandItem((InventoryItem)null);
                  if (!var4) {
                     var5 = Rand.Next(0.1F, 0.9F);
                     var6 = Rand.Next(0.1F, 0.9F);
                     var7 = var1.getApparentZ(var5, var6) - (float)var1.getZ();
                     this.getInventory().DoRemoveItem(var3);
                     var1.AddWorldInventoryItem(var3, var5, var6, var7);
                     LuaEventManager.triggerEvent("OnContainerUpdate");
                     LuaEventManager.triggerEvent("onItemFall", var3);
                     this.playDropItemSound(var3);
                  }
               }

            }
         }
      }
   }

   public boolean isHeavyItem(InventoryItem var1) {
      if (var1 == null) {
         return false;
      } else if (var1 instanceof InventoryContainer) {
         return true;
      } else if (var1.hasTag("HeavyItem")) {
         return true;
      } else {
         return !var1.getType().equals("CorpseMale") && !var1.getType().equals("CorpseFemale") ? var1.getType().equals("Generator") : true;
      }
   }

   public boolean isCanShout() {
      return this.canShout;
   }

   public void setCanShout(boolean var1) {
      this.canShout = var1;
   }

   public boolean isUnlimitedEndurance() {
      return this.m_cheats.m_unlimitedEndurance;
   }

   public void setUnlimitedEndurance(boolean var1) {
      if (!Role.haveCapability(this, Capability.ToggleUnlimitedEndurance)) {
         this.m_cheats.m_unlimitedEndurance = false;
      } else {
         this.m_cheats.m_unlimitedEndurance = var1;
      }
   }

   private void addActiveLightItem(InventoryItem var1, ArrayList<InventoryItem> var2) {
      if (var1 != null && var1.isEmittingLight() && !var2.contains(var1)) {
         var2.add(var1);
      }

   }

   public ArrayList<InventoryItem> getActiveLightItems(ArrayList<InventoryItem> var1) {
      this.addActiveLightItem(this.getSecondaryHandItem(), var1);
      this.addActiveLightItem(this.getPrimaryHandItem(), var1);
      AttachedItems var2 = this.getAttachedItems();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         InventoryItem var4 = var2.getItemByIndex(var3);
         this.addActiveLightItem(var4, var1);
      }

      return var1;
   }

   public SleepingEventData getOrCreateSleepingEventData() {
      if (this.m_sleepingEventData == null) {
         this.m_sleepingEventData = new SleepingEventData();
      }

      return this.m_sleepingEventData;
   }

   public void playEmote(String var1) {
      this.setVariable("emote", var1);
      this.setVariable("EmotePlaying", true);
      this.actionContext.reportEvent("EventEmote");
   }

   public String getAnimationStateName() {
      return this.advancedAnimator.getCurrentStateName();
   }

   public String getActionStateName() {
      return this.actionContext.getCurrentStateName();
   }

   public boolean shouldWaitToStartTimedAction() {
      if (!this.isSitOnGround()) {
         return false;
      } else if (this.getCurrentState().equals(FishingState.instance())) {
         return false;
      } else {
         AdvancedAnimator var1 = this.getAdvancedAnimator();
         if (var1.getRootLayer() == null) {
            return false;
         } else if (var1.animSet != null && var1.animSet.containsState("sitonground")) {
            AnimState var2 = var1.animSet.GetState("sitonground");
            if (!PZArrayUtil.contains(var2.m_Nodes, (var0) -> {
               return "sit_action".equalsIgnoreCase(var0.m_Name);
            })) {
               return false;
            } else {
               LiveAnimNode var3 = (LiveAnimNode)PZArrayUtil.find(var1.getRootLayer().getLiveAnimNodes(), (var0) -> {
                  return var0.isActive() && "sit_action".equalsIgnoreCase(var0.getName());
               });
               return var3 == null || !var3.isMainAnimActive();
            }
         } else {
            return false;
         }
      }
   }

   public void setPersistentOutfitID(int var1) {
      this.setPersistentOutfitID(var1, false);
   }

   public void setPersistentOutfitID(int var1, boolean var2) {
      this.m_persistentOutfitId = var1;
      this.m_bPersistentOutfitInit = var2;
   }

   public int getPersistentOutfitID() {
      return this.m_persistentOutfitId;
   }

   public boolean isPersistentOutfitInit() {
      return this.m_bPersistentOutfitInit;
   }

   public boolean isDoingActionThatCanBeCancelled() {
      return false;
   }

   public boolean isDoDeathSound() {
      return this.doDeathSound;
   }

   public void setDoDeathSound(boolean var1) {
      this.doDeathSound = var1;
   }

   public boolean isKilledByFall() {
      return this.bKilledByFall;
   }

   public void setKilledByFall(boolean var1) {
      this.bKilledByFall = var1;
   }

   public void updateEquippedRadioFreq() {
      this.invRadioFreq.clear();

      int var1;
      for(var1 = 0; var1 < this.getInventory().getItems().size(); ++var1) {
         InventoryItem var2 = (InventoryItem)this.getInventory().getItems().get(var1);
         if (var2 instanceof Radio var3) {
            if (var3.getDeviceData() != null && var3.getDeviceData().getIsTurnedOn() && !var3.getDeviceData().getMicIsMuted() && !this.invRadioFreq.contains(var3.getDeviceData().getChannel())) {
               this.invRadioFreq.add(var3.getDeviceData().getChannel());
            }
         }
      }

      for(var1 = 0; var1 < this.invRadioFreq.size(); ++var1) {
         System.out.println(this.invRadioFreq.get(var1));
      }

      if (this instanceof IsoPlayer && GameClient.bClient) {
         GameClient.sendEquippedRadioFreq((IsoPlayer)this);
      }

   }

   public void updateEquippedItemSounds() {
      if (this.leftHandItem != null) {
         this.leftHandItem.updateEquippedAndActivatedSound();
      }

      if (this.rightHandItem != null) {
         this.rightHandItem.updateEquippedAndActivatedSound();
      }

      WornItems var1 = this.getWornItems();
      if (var1 != null) {
         for(int var2 = 0; var2 < var1.size(); ++var2) {
            InventoryItem var3 = var1.getItemByIndex(var2);
            var3.updateEquippedAndActivatedSound();
         }

      }
   }

   public FMODParameterList getFMODParameters() {
      return this.fmodParameters;
   }

   public void startEvent(long var1, GameSoundClip var3, BitSet var4) {
      FMODParameterList var5 = this.getFMODParameters();
      ArrayList var6 = var3.eventDescription.parameters;

      for(int var7 = 0; var7 < var6.size(); ++var7) {
         FMOD_STUDIO_PARAMETER_DESCRIPTION var8 = (FMOD_STUDIO_PARAMETER_DESCRIPTION)var6.get(var7);
         if (!var4.get(var8.globalIndex)) {
            FMODParameter var9 = var5.get(var8);
            if (var9 != null) {
               var9.startEventInstance(var1);
            }
         }
      }

   }

   public void updateEvent(long var1, GameSoundClip var3) {
   }

   public void stopEvent(long var1, GameSoundClip var3, BitSet var4) {
      FMODParameterList var5 = this.getFMODParameters();
      ArrayList var6 = var3.eventDescription.parameters;

      for(int var7 = 0; var7 < var6.size(); ++var7) {
         FMOD_STUDIO_PARAMETER_DESCRIPTION var8 = (FMOD_STUDIO_PARAMETER_DESCRIPTION)var6.get(var7);
         if (!var4.get(var8.globalIndex)) {
            FMODParameter var9 = var5.get(var8);
            if (var9 != null) {
               var9.stopEventInstance(var1);
            }
         }
      }

   }

   public void playBloodSplatterSound() {
      if (this.getEmitter().isPlaying("BloodSplatter")) {
      }

      this.getEmitter().playSoundImpl("BloodSplatter", this);
   }

   public void setIgnoreAimingInput(boolean var1) {
      this.ignoreAimingInput = var1;
   }

   public boolean isIgnoringAimingInput() {
      return this.ignoreAimingInput;
   }

   public void addBlood(float var1) {
      if (!((float)Rand.Next(10) > var1)) {
         if (SandboxOptions.instance.BloodLevel.getValue() > 1) {
            int var2 = Rand.Next(4, 10);
            if (var2 < 1) {
               var2 = 1;
            }

            if (Core.bLastStand) {
               var2 *= 3;
            }

            switch (SandboxOptions.instance.BloodLevel.getValue()) {
               case 2:
                  var2 /= 2;
               case 3:
               default:
                  break;
               case 4:
                  var2 *= 2;
                  break;
               case 5:
                  var2 *= 5;
            }

            for(int var3 = 0; var3 < var2; ++var3) {
               this.splatBlood(2, 0.3F);
            }
         }

         if (SandboxOptions.instance.BloodLevel.getValue() > 1) {
            this.splatBloodFloorBig();
            this.playBloodSplatterSound();
         }

      }
   }

   public boolean isKnockedDown() {
      return this.bKnockedDown;
   }

   public void setKnockedDown(boolean var1) {
      this.bKnockedDown = var1;
   }

   public void writeInventory(ByteBuffer var1) {
      String var2 = this.isFemale() ? "inventoryfemale" : "inventorymale";
      GameWindow.WriteString(var1, var2);
      if (this.getInventory() != null) {
         var1.put((byte)1);

         try {
            ArrayList var3 = this.getInventory().save(var1);
            WornItems var5 = this.getWornItems();
            int var6;
            if (var5 == null) {
               byte var11 = 0;
               var1.put((byte)var11);
            } else {
               int var4 = var5.size();
               if (var4 > 127) {
                  DebugLog.Multiplayer.warn("Too many worn items");
                  var4 = 127;
               }

               var1.put((byte)var4);

               for(var6 = 0; var6 < var4; ++var6) {
                  WornItem var7 = var5.get(var6);
                  GameWindow.WriteString(var1, var7.getLocation());
                  var1.putShort((short)var3.indexOf(var7.getItem()));
               }
            }

            AttachedItems var13 = this.getAttachedItems();
            if (var13 == null) {
               boolean var12 = false;
               var1.put((byte)0);
            } else {
               var6 = var13.size();
               if (var6 > 127) {
                  DebugLog.Multiplayer.warn("Too many attached items");
                  var6 = 127;
               }

               var1.put((byte)var6);

               for(int var8 = 0; var8 < var6; ++var8) {
                  AttachedItem var9 = var13.get(var8);
                  GameWindow.WriteString(var1, var9.getLocation());
                  var1.putShort((short)var3.indexOf(var9.getItem()));
               }
            }
         } catch (IOException var10) {
            DebugLog.Multiplayer.printException(var10, "WriteInventory error for character " + this.getOnlineID(), LogSeverity.Error);
         }
      } else {
         var1.put((byte)0);
      }

   }

   public String readInventory(ByteBuffer var1) {
      String var2 = GameWindow.ReadString(var1);
      boolean var3 = var1.get() == 1;
      if (var3) {
         try {
            ArrayList var4 = this.getInventory().load(var1, IsoWorld.getWorldVersion());
            byte var5 = var1.get();

            for(int var6 = 0; var6 < var5; ++var6) {
               String var7 = GameWindow.ReadStringUTF(var1);
               short var8 = var1.getShort();
               if (var8 >= 0 && var8 < var4.size() && this.getBodyLocationGroup().getLocation(var7) != null) {
                  this.getWornItems().setItem(var7, (InventoryItem)var4.get(var8));
               }
            }

            byte var11 = var1.get();

            for(int var12 = 0; var12 < var11; ++var12) {
               String var13 = GameWindow.ReadStringUTF(var1);
               short var9 = var1.getShort();
               if (var9 >= 0 && var9 < var4.size() && this.getAttachedLocationGroup().getLocation(var13) != null) {
                  this.getAttachedItems().setItem(var13, (InventoryItem)var4.get(var9));
               }
            }
         } catch (IOException var10) {
            DebugLog.Multiplayer.printException(var10, "ReadInventory error for character " + this.getOnlineID(), LogSeverity.Error);
         }
      }

      return var2;
   }

   public void Kill(IsoGameCharacter var1) {
      this.setAttackedBy(var1);
      this.setHealth(0.0F);
      this.setOnKillDone(true);
   }

   public boolean shouldDoInventory() {
      return true;
   }

   public void die() {
      if (!this.isOnDeathDone()) {
         if (GameClient.bClient) {
            if (this.shouldDoInventory()) {
               this.becomeCorpse();
            } else {
               this.getNetworkCharacterAI().processDeadBody();
            }
         } else {
            this.becomeCorpse();
         }

      }
   }

   public void becomeCorpse() {
      this.Kill(this.getAttackedBy());
      this.setOnDeathDone(true);
   }

   public boolean shouldBecomeCorpse() {
      if (GameClient.bClient || GameServer.bServer) {
         if (this.getHitReactionNetworkAI().isSetup() || this.getHitReactionNetworkAI().isStarted()) {
            return false;
         }

         if (GameServer.bServer) {
            if (this instanceof IsoPlayer && GameServer.isDelayedDisconnect((IsoPlayer)this)) {
               return true;
            }

            return this.getNetworkCharacterAI().isSetDeadBody();
         }

         if (GameClient.bClient) {
            return this.isCurrentState(ZombieOnGroundState.instance()) || this.isCurrentState(PlayerOnGroundState.instance()) || this.isCurrentState(AnimalOnGroundState.instance());
         }
      }

      return true;
   }

   public HitReactionNetworkAI getHitReactionNetworkAI() {
      return null;
   }

   public NetworkCharacterAI getNetworkCharacterAI() {
      return null;
   }

   public boolean wasLocal() {
      return this.getNetworkCharacterAI() == null || this.getNetworkCharacterAI().wasLocal();
   }

   public boolean isLocal() {
      return !GameClient.bClient && !GameServer.bServer;
   }

   public boolean isVehicleCollisionActive(BaseVehicle var1) {
      if (!GameClient.bClient) {
         return false;
      } else if (!this.isAlive()) {
         return false;
      } else if (var1 == null) {
         return false;
      } else if (!var1.shouldCollideWithCharacters()) {
         return false;
      } else if (var1.isNetPlayerAuthorization(BaseVehicle.Authorization.Server)) {
         return false;
      } else if (var1.isEngineRunning() || var1.getVehicleTowing() != null && var1.getVehicleTowing().isEngineRunning() || var1.getVehicleTowedBy() != null && var1.getVehicleTowedBy().isEngineRunning()) {
         if (var1.getDriver() == null && (var1.getVehicleTowing() == null || var1.getVehicleTowing().getDriver() == null) && (var1.getVehicleTowedBy() == null || var1.getVehicleTowedBy().getDriver() == null)) {
            return false;
         } else if (!(Math.abs(var1.getX() - this.getX()) < 0.01F) && !(Math.abs(var1.getY() - this.getY()) < 0.01F)) {
            return (!this.isKnockedDown() || this.isOnFloor()) && (this.getHitReactionNetworkAI() == null || !this.getHitReactionNetworkAI().isStarted());
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void doHitByVehicle(BaseVehicle var1, BaseVehicle.HitVars var2) {
      if (GameClient.bClient) {
         IsoPlayer var3 = (IsoPlayer)GameClient.IDToPlayerMap.get(var1.getNetPlayerId());
         if (var3 != null) {
            if (var3.isLocal()) {
               SoundManager.instance.PlayWorldSound("VehicleHitCharacter", this.getCurrentSquare(), 0.0F, 20.0F, 0.9F, true);
               float var4 = this.Hit(var1, var2.hitSpeed, var2.isTargetHitFromBehind, -var2.targetImpulse.x, -var2.targetImpulse.z);
               GameClient.sendVehicleHit(var3, this, var1, var4, var2.isTargetHitFromBehind, var2.vehicleDamage, var2.hitSpeed, var2.isVehicleHitFromFront);
               var3.triggerMusicIntensityEvent("VehicleHitCharacter");
            } else {
               this.getNetworkCharacterAI().resetVehicleHitTimeout();
            }
         }
      } else if (!GameServer.bServer) {
         BaseSoundEmitter var7 = IsoWorld.instance.getFreeEmitter(this.getX(), this.getY(), this.getZ());
         long var8 = var7.playSound("VehicleHitCharacter");
         var7.setParameterValue(var8, FMODManager.instance.getParameterDescription("VehicleHitLocation"), (float)ParameterVehicleHitLocation.calculateLocation(var1, this.getX(), this.getY(), this.getZ()).getValue());
         var7.setParameterValue(var8, FMODManager.instance.getParameterDescription("VehicleSpeed"), var1.getCurrentSpeedKmHour());
         this.Hit(var1, var2.hitSpeed, var2.isTargetHitFromBehind, -var2.targetImpulse.x, -var2.targetImpulse.z);
         IsoPlayer var6 = (IsoPlayer)Type.tryCastTo(var1.getDriver(), IsoPlayer.class);
         if (var6 != null) {
            var6.triggerMusicIntensityEvent("VehicleHitCharacter");
         }
      }

   }

   public boolean isSkipResolveCollision() {
      return this.isBeingGrappled();
   }

   public boolean isPerformingAttackAnimation() {
      return this.m_isPerformingAttackAnim;
   }

   public void setPerformingAttackAnimation(boolean var1) {
      this.m_isPerformingAttackAnim = var1;
   }

   public boolean isPerformingShoveAnimation() {
      return this.m_isPerformingShoveAnim;
   }

   public void setPerformingShoveAnimation(boolean var1) {
      this.m_isPerformingShoveAnim = var1;
   }

   public boolean isPerformingStompAnimation() {
      return this.m_isPerformingStompAnim;
   }

   public void setPerformingStompAnimation(boolean var1) {
      this.m_isPerformingStompAnim = var1;
   }

   public boolean isPerformingHostileAnimation() {
      return this.isPerformingAttackAnimation() || this.isPerformingShoveAnimation() || this.isPerformingStompAnimation() || this.isPerformingGrappleGrabAnimation();
   }

   public Float getNextAnimationTranslationLength() {
      ActionContext var1 = this.getActionContext();
      AnimationPlayer var2 = this.getAnimationPlayer();
      AdvancedAnimator var3 = this.getAdvancedAnimator();
      if (var1 != null && var2 != null && var3 != null) {
         ActionState var4 = var1.getNextState();
         if (var4 != null && !StringUtils.isNullOrEmpty(var4.getName())) {
            AnimationSet var5 = var3.animSet;
            AnimState var6 = var5.GetState(var4.getName());
            SkinningData var7 = var2.getSkinningData();
            ArrayList var8 = new ArrayList();
            var6.getAnimNodes(this, var8);
            Iterator var9 = var8.iterator();

            while(var9.hasNext()) {
               AnimNode var10 = (AnimNode)var9.next();
               if (!StringUtils.isNullOrEmpty(var10.m_AnimName)) {
                  AnimationClip var11 = (AnimationClip)var7.AnimationClips.get(var10.m_AnimName);
                  if (var11 != null) {
                     return var11.getTranslationLength(var10.m_deferredBoneAxis);
                  }
               }
            }

            return null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public Float calcHitDir(IsoGameCharacter var1, HandWeapon var2, Vector2 var3) {
      Float var4 = this.getNextAnimationTranslationLength();
      var3.set(this.getX() - var1.getX(), this.getY() - var1.getY()).normalize();
      if (var4 == null) {
         var3.setLength(this.getHitForce() * 0.1F);
         var3.scale(var2.getPushBackMod());
         var3.rotate(var2.HitAngleMod);
      } else {
         var3.scale(var4);
      }

      return null;
   }

   public void calcHitDir(Vector2 var1) {
      var1.set(this.getHitDir());
      var1.setLength(this.getHitForce());
   }

   public Safety getSafety() {
      return this.safety;
   }

   public void setSafety(Safety var1) {
      this.safety.copyFrom(var1);
   }

   public void burnCorpse(IsoDeadBody var1) {
      if (GameClient.bClient) {
         INetworkPacket.send(PacketTypes.PacketType.BurnCorpse, (IsoPlayer)this, var1.getObjectID());
      } else {
         IsoFireManager.StartFire(var1.getCell(), var1.getSquare(), true, 100, 700);
      }
   }

   public void setIsAnimal(boolean var1) {
      this.animal = var1;
   }

   public boolean isAnimal() {
      return this.animal;
   }

   public float getPerkToUnit(PerkFactory.Perk var1) {
      int var2 = this.getPerkLevel(var1);
      if (var2 == 10) {
         return 1.0F;
      } else {
         float var3 = var1.getXpForLevel(var2 + 1);
         float var4 = this.getXp().getXP(var1);
         float var5 = 0.0F;
         if (var2 > 0) {
            var5 = var1.getTotalXpForLevel(var2);
         }

         var4 -= var5;
         float var6 = (float)var2 * 0.1F + var4 / var3 * 0.1F;
         return PZMath.clamp(var6, 0.0F, 1.0F);
      }
   }

   public HashMap<String, Integer> getReadLiterature() {
      return this.readLiterature;
   }

   public boolean isLiteratureRead(String var1) {
      HashMap var2 = this.getReadLiterature();
      if (var2.containsKey(var1)) {
         int var3 = (Integer)var2.get(var1);
         int var4 = GameTime.getInstance().getNightsSurvived();
         return var4 < var3 + SandboxOptions.getInstance().LiteratureCooldown.getValue();
      } else {
         return false;
      }
   }

   public void addReadLiterature(String var1) {
      if (!this.isLiteratureRead(var1)) {
         HashMap var2 = this.getReadLiterature();
         int var3 = GameTime.getInstance().getNightsSurvived();
         var2.put(var1, var3);
      }
   }

   public void addReadLiterature(String var1, int var2) {
      if (!this.isLiteratureRead(var1)) {
         HashMap var3 = this.getReadLiterature();
         var3.put(var1, var2);
      }
   }

   public void setMusicIntensityEventModData(String var1, Object var2) {
      if (!StringUtils.isNullOrWhitespace(var1)) {
         KahluaTable var3 = this.hasModData() ? (KahluaTable)Type.tryCastTo(this.getModData().rawget("MusicIntensityEvent"), KahluaTable.class) : null;
         if (var3 != null || var2 != null) {
            if (var3 == null) {
               var3 = LuaManager.platform.newTable();
               this.getModData().rawset("MusicIntensityEvent", var3);
            }

            var3.rawset(var1, var2);
         }
      }
   }

   public Object getMusicIntensityEventModData(String var1) {
      if (this.hasModData()) {
         KahluaTable var2 = (KahluaTable)Type.tryCastTo(this.getModData().rawget("MusicIntensityEvent"), KahluaTable.class);
         return var2 == null ? null : var2.rawget(var1);
      } else {
         return null;
      }
   }

   public boolean isWearingTag(String var1) {
      for(int var2 = 0; var2 < this.getWornItems().size(); ++var2) {
         InventoryItem var3 = this.getWornItems().get(var2).getItem();
         if (var3.hasTag(var1)) {
            return true;
         }
      }

      return false;
   }

   public float getCorpseSicknessDefense() {
      return this.getCorpseSicknessDefense(0.0F, false);
   }

   public float getCorpseSicknessDefense(float var1) {
      return this.getCorpseSicknessDefense(var1, true);
   }

   public float getCorpseSicknessDefense(float var1, boolean var2) {
      float var3 = 0.0F;

      for(int var4 = 0; var4 < this.getWornItems().size(); ++var4) {
         InventoryItem var5 = this.getWornItems().getItemByIndex(var4);
         if (var5 instanceof Clothing var6) {
            if (var6.hasTag("SCBA") && var6.isActivated() && var6.hasTank() && var6.getUsedDelta() > 0.0F) {
               return 100.0F;
            }

            if (((Clothing)var5).getCorpseSicknessDefense() > var3) {
               var3 = ((Clothing)var5).getCorpseSicknessDefense();
            }

            if (var6.hasTag("GasMask") && var6.hasFilter()) {
               var3 = 25.0F;
            }

            if (var6.hasTag("GasMask") && var6.getUsedDelta() > 0.0F) {
               var3 = 100.0F;
               if (var2 && var1 > 0.0F) {
                  var6.drainGasMask(var1);
               }
            }
         }

         if (var3 >= 100.0F) {
            return 100.0F;
         }
      }

      return var3;
   }

   public boolean isProtectedFromToxic() {
      return this.isProtectedFromToxic(false);
   }

   public boolean isProtectedFromToxic(boolean var1) {
      for(int var2 = 0; var2 < this.getWornItems().size(); ++var2) {
         InventoryItem var3 = this.getWornItems().getItemByIndex(var2);
         if (var3 instanceof Clothing var4) {
            if (var4.hasTag("SCBA") && var4.isActivated() && var4.hasTank() && var4.getUsedDelta() > 0.0F) {
               return true;
            }

            if (var4.hasTag("GasMask") && var4.hasFilter() && var4.getUsedDelta() > 0.0F) {
               if (var1) {
                  var4.drainGasMask(0.01F);
               }

               return true;
            }
         }
      }

      return false;
   }

   private void checkSCBADrain() {
      for(int var1 = 0; var1 < this.getWornItems().size(); ++var1) {
         InventoryItem var2 = this.getWornItems().getItemByIndex(var1);
         if (var2 instanceof Clothing var3) {
            if (var3.hasTag("SCBA") && var3.isActivated() && var3.hasTank() && var3.getUsedDelta() > 0.0F) {
               var3.drainSCBA();
               return;
            }

            if (var3.hasTag("SCBA") && var3.isActivated() && (!var3.hasTank() || var3.getUsedDelta() <= 0.0F)) {
               var3.setActivated(false);
               return;
            }
         }
      }

   }

   public boolean isOverEncumbered() {
      float var1 = this.getInventory().getCapacityWeight();
      float var2 = (float)this.getMaxWeight();
      return var1 / var2 > 1.0F;
   }

   public void updateWornItemsVisionModifier() {
      float var1 = 1.0F;
      int var2;
      if (this instanceof IsoZombie) {
         this.getItemVisuals(tempItemVisuals);
         if (tempItemVisuals != null) {
            for(var2 = tempItemVisuals.size() - 1; var2 >= 0; --var2) {
               ItemVisual var3 = (ItemVisual)tempItemVisuals.get(var2);
               if (var3 != null) {
                  Item var4 = var3.getScriptItem();
                  if (var4 != null && var4.getVisionModifier() != 1.0F && var4.getVisionModifier() > 0.0F) {
                     var1 /= var4.getVisionModifier();
                  }
               }
            }
         }
      } else if (this.getWornItems() != null) {
         for(var2 = 0; var2 < this.getWornItems().size(); ++var2) {
            InventoryItem var5 = this.getWornItems().getItemByIndex(var2);
            if (var5 != null && var5.getVisionModifier() != 1.0F && var5.getVisionModifier() > 0.0F) {
               var1 /= var5.getVisionModifier();
            }
         }
      }

      this.wornItemsVisionModifier = var1;
   }

   public float getWornItemsVisionModifier() {
      return this.wornItemsVisionModifier;
   }

   public float getWornItemsVisionMultiplier() {
      return 1.0F / this.getWornItemsVisionModifier();
   }

   public void updateWornItemsHearingModifier() {
      float var1 = 1.0F;
      int var2;
      if (this instanceof IsoZombie) {
         this.getItemVisuals(tempItemVisuals);

         for(var2 = tempItemVisuals.size() - 1; var2 >= 0; --var2) {
            ItemVisual var3 = (ItemVisual)tempItemVisuals.get(var2);
            Item var4 = var3.getScriptItem();
            if (var4 != null && var4.getHearingModifier() != 1.0F && var4.getHearingModifier() > 0.0F) {
               var1 /= var4.getHearingModifier();
            }
         }
      } else {
         for(var2 = 0; var2 < this.getWornItems().size(); ++var2) {
            InventoryItem var5 = this.getWornItems().getItemByIndex(var2);
            if (var5 != null && var5.getHearingModifier() != 1.0F && var5.getHearingModifier() > 0.0F) {
               var1 /= var5.getHearingModifier();
            }
         }
      }

      this.wornItemsHearingModifier = var1;
   }

   public float getWornItemsHearingModifier() {
      return this.wornItemsHearingModifier;
   }

   public float getWornItemsHearingMultiplier() {
      return 1.0F / this.getWornItemsHearingModifier();
   }

   public float getHearDistanceModifier() {
      float var1 = 1.0F;
      if (this.Traits.HardOfHearing.isSet()) {
         var1 *= 4.5F;
      }

      var1 *= this.getWornItemsHearingModifier();
      return var1;
   }

   public float getWeatherHearingMultiplier() {
      float var1 = 1.0F;
      var1 -= ClimateManager.getInstance().getRainIntensity() * 0.33F;
      var1 -= ClimateManager.getInstance().getFogIntensity() * 0.1F;
      return var1;
   }

   public float getSeeNearbyCharacterDistance() {
      return (3.5F - this.stats.getFatigue() - this.stats.Drunkenness * 0.01F) * this.getWornItemsVisionMultiplier();
   }

   public void setLastHitCharacter(IsoGameCharacter var1) {
      this.lastHitCharacter = var1;
   }

   public IsoGameCharacter getLastHitCharacter() {
      return this.lastHitCharacter;
   }

   public void triggerCough() {
      this.setVariable("Ext", "Cough");
      this.Say(Translator.getText("IGUI_PlayerText_Cough"));
      this.reportEvent("EventDoExt");
      WorldSoundManager.instance.addSound(this, (int)this.getX(), (int)this.getY(), (int)this.getZ(), 35, 40, true);
   }

   public boolean hasDirtyClothing(Integer var1) {
      this.getItemVisuals(tempItemVisuals);

      for(int var2 = tempItemVisuals.size() - 1; var2 >= 0; --var2) {
         ItemVisual var3 = (ItemVisual)tempItemVisuals.get(var2);
         Item var4 = var3.getScriptItem();
         if (var4 != null) {
            ArrayList var5 = var4.getBloodClothingType();
            if (var5 != null) {
               ArrayList var6 = BloodClothingType.getCoveredParts(var5);
               if (var6 != null) {
                  InventoryItem var7 = var3.getInventoryItem();
                  if (var7 == null) {
                     var7 = InventoryItemFactory.CreateItem(var3.getItemType());
                     if (var7 == null) {
                        continue;
                     }
                  }

                  for(int var8 = 0; var8 < var6.size(); ++var8) {
                     if (var7 instanceof Clothing && ((BloodBodyPartType)var6.get(var8)).index() == var1 && var3.getDirt((BloodBodyPartType)var6.get(var8)) > 0.15F) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   public boolean hasBloodyClothing(Integer var1) {
      this.getItemVisuals(tempItemVisuals);

      for(int var2 = tempItemVisuals.size() - 1; var2 >= 0; --var2) {
         ItemVisual var3 = (ItemVisual)tempItemVisuals.get(var2);
         Item var4 = var3.getScriptItem();
         if (var4 != null) {
            ArrayList var5 = var4.getBloodClothingType();
            if (var5 != null) {
               ArrayList var6 = BloodClothingType.getCoveredParts(var5);
               if (var6 != null) {
                  InventoryItem var7 = var3.getInventoryItem();
                  if (var7 == null) {
                     var7 = InventoryItemFactory.CreateItem(var3.getItemType());
                     if (var7 == null) {
                        continue;
                     }
                  }

                  for(int var8 = 0; var8 < var6.size(); ++var8) {
                     if (var7 instanceof Clothing && ((BloodBodyPartType)var6.get(var8)).index() == var1 && var3.getBlood((BloodBodyPartType)var6.get(var8)) > 0.25F) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   public IAnimatable getAnimatable() {
      return this;
   }

   public IGrappleable getGrappleable() {
      return this;
   }

   public BaseGrappleable getWrappedGrappleable() {
      return this.m_grappleable;
   }

   public boolean canBeGrappled() {
      return this.isBeingGrappled() ? false : this.canTransitionToState("grappled");
   }

   public boolean isPerformingGrappleAnimation() {
      AnimationPlayer var1 = this.getAnimationPlayer();
      if (var1 != null && var1.isReady()) {
         List var2 = var1.getMultiTrack().getTracks();

         for(int var3 = 0; var3 < var2.size(); ++var3) {
            AnimationTrack var4 = (AnimationTrack)var2.get(var3);
            if (var4.isGrappler()) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public String getShoutType() {
      if (this.getPrimaryHandItem() != null && this.getPrimaryHandItem().getShoutType() != null) {
         return this.getPrimaryHandItem().getShoutType() + "_primary";
      } else {
         return this.getSecondaryHandItem() != null && this.getSecondaryHandItem().getShoutType() != null ? this.getSecondaryHandItem().getShoutType() + "_secondary" : null;
      }
   }

   public String getShoutItemModel() {
      if (this.getPrimaryHandItem() != null && this.getPrimaryHandItem().getShoutType() != null) {
         return this.getPrimaryHandItem().getStaticModel();
      } else {
         return this.getSecondaryHandItem() != null && this.getSecondaryHandItem().getShoutType() != null ? this.getSecondaryHandItem().getStaticModel() : null;
      }
   }

   public boolean isWearingGlasses() {
      InventoryItem var1 = this.getWornItem("Eyes");
      return var1 != null && var1.isVisualAid();
   }

   public float getClothingDiscomfortModifier() {
      return this.clothingDiscomfortModifier;
   }

   public void updateVisionEffectTargets() {
      this.blurFactor = PZMath.lerp(this.blurFactor, this.blurFactorTarget, this.blurFactor < this.blurFactorTarget ? 0.1F : 0.01F);
   }

   public void updateVisionEffects() {
      boolean var1 = this.Traits.ShortSighted.isSet();
      boolean var2 = this.isWearingGlasses();
      if ((var2 || !var1) && (!var2 || var1)) {
         this.blurFactorTarget = 0.0F;
      } else {
         this.blurFactorTarget = 1.0F;
      }

   }

   public float getBlurFactor() {
      return this.blurFactor;
   }

   public boolean isDisguised() {
      return this.usernameDisguised;
   }

   public void updateDisguisedState() {
      if ((GameClient.bClient || GameServer.bServer) && ServerOptions.instance.UsernameDisguises.getValue()) {
         this.usernameDisguised = false;
         if (this.isoPlayer == null) {
            return;
         }

         SafeHouse var1 = SafeHouse.isSafeHouse(this.getCurrentSquare(), (String)null, false);
         if (var1 == null || !ServerOptions.instance.SafehouseDisableDisguises.getValue() || this.isoPlayer.role.haveCapability(Capability.CanGoInsideSafehouses)) {
            HashSet var2 = new HashSet();
            this.getItemVisuals(tempItemVisuals);
            int var3;
            Item var5;
            if (tempItemVisuals != null) {
               for(var3 = tempItemVisuals.size() - 1; var3 >= 0; --var3) {
                  ItemVisual var6 = (ItemVisual)tempItemVisuals.get(var3);
                  if (var6 != null) {
                     var5 = var6.getScriptItem();
                     if (var5 != null) {
                        var2.addAll(var5.getTags());
                     }
                  }
               }
            } else if (this.getWornItems() != null) {
               for(var3 = 0; var3 < this.getWornItems().size(); ++var3) {
                  InventoryItem var4 = this.getWornItems().getItemByIndex(var3);
                  if (var4 != null) {
                     var5 = var4.getScriptItem();
                     if (var5 != null) {
                        var2.addAll(var5.getTags());
                     }
                  }
               }
            }

            if (!var2.isEmpty() && (var2.contains("IsDisguise") || var2.contains("IsLowerDisguise") && var2.contains("IsUpperDisguise"))) {
               this.usernameDisguised = true;
            }
         }
      }

   }

   public void OnClothingUpdated() {
      this.updateSpeedModifiers();
      if (this instanceof IsoPlayer) {
         this.updateVisionEffects();
         this.updateDiscomfortModifiers();
      }

      this.updateWornItemsVisionModifier();
      this.updateWornItemsHearingModifier();
   }

   public void OnEquipmentUpdated() {
   }

   private void renderDebugData() {
      IndieGL.StartShader(0);
      IndieGL.disableDepthTest();
      IsoZombie var1 = (IsoZombie)Type.tryCastTo(this, IsoZombie.class);
      IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(this, IsoPlayer.class);
      IsoAnimal var3 = (IsoAnimal)Type.tryCastTo(this, IsoAnimal.class);
      boolean var4 = var2 != null && var3 == null && this.isLocal();
      TextManager var10000 = TextManager.instance;
      Objects.requireNonNull(var10000);
      TextManager.StringDrawer var5 = var10000::DrawString;
      if (var4) {
         var10000 = TextManager.instance;
         Objects.requireNonNull(var10000);
         var5 = var10000::DrawStringRight;
      }

      Color var6 = Colors.Chartreuse;
      if (this.isDead()) {
         var6 = Colors.Yellow;
      } else if (this.getNetworkCharacterAI().isRemote()) {
         var6 = Colors.OrangeRed;
      }

      UIFont var7 = UIFont.Dialogue;
      Color var8 = Colors.NavajoWhite;
      float var9 = IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
      float var10 = IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
      float var11 = 10.0F;
      if (this.getNetworkCharacterAI().getBooleanDebugOptions().Enable.getValue()) {
         if (var2 != null && var2.getRole() != null && !(this instanceof IsoAnimal)) {
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 11.0F)), String.format("%d (%s)", this.getOnlineID(), var2.getRole().getName()), (double)var6.r, (double)var6.g, (double)var6.b, (double)var6.a);
         } else {
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 11.0F)), String.format("%d", this.getOnlineID()), (double)var6.r, (double)var6.g, (double)var6.b, (double)var6.a);
         }
      }

      float var12;
      float var13;
      if (this.getNetworkCharacterAI().getBooleanDebugOptions().Enable.getValue() && this.getNetworkCharacterAI().getBooleanDebugOptions().Owner.getValue() && var2 != null && var2.isLocalPlayer() && var3 == null) {
         var12 = var10 - 150.0F;
         var13 = 0.0F;
         Iterator var14 = AnimalInstanceManager.getInstance().getAnimals().iterator();

         while(var14.hasNext()) {
            IsoAnimal var15 = (IsoAnimal)var14.next();
            if (var15.isLocalPlayer()) {
               if (var4) {
                  var5.draw(var7, (double)var9, (double)(var12 + (var13 -= 14.0F)), String.format("%s %s - %d", var15.getAnimalType(), var15.getBreed().getName(), var15.getOnlineID()), (double)var6.r, (double)var6.g, (double)var6.b, (double)var6.a);
               } else {
                  var5.draw(var7, (double)var9, (double)(var12 + (var13 -= 14.0F)), String.format("%d - %s %s", var15.getOnlineID(), var15.getAnimalType(), var15.getBreed().getName()), (double)var6.r, (double)var6.g, (double)var6.b, (double)var6.a);
               }
            }
         }
      }

      if (this.getNetworkCharacterAI().getBooleanDebugOptions().Enable.getValue() && this.getNetworkCharacterAI().getBooleanDebugOptions().Desync.getValue() && (var1 != null || var3 != null)) {
         var8 = Colors.NavajoWhite;
         var12 = IsoUtils.DistanceTo(this.getX(), this.getY(), this.realx, this.realy);
         if (this.getNetworkCharacterAI().isRemote() && var12 > 1.0F) {
            LineDrawer.DrawIsoLine(this.realx, this.realy, this.getZ(), this.getX(), this.getY(), this.getZ(), var8.r, var8.g, var8.b, var8.a, 1);
            LineDrawer.DrawIsoTransform(this.realx, this.realy, this.getZ(), this.realdir.ToVector().x, this.realdir.ToVector().y, 0.35F, 16, var8.r, var8.g, var8.b, var8.a, 1);
            LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), 0.4F, 4, var8.r, var8.g, var8.b, var8.a);
            var13 = IsoUtils.DistanceTo(this.realx, this.realy, this.getNetworkCharacterAI().targetX, this.getNetworkCharacterAI().targetY);
            float var18 = IsoUtils.DistanceTo(this.getX(), this.getY(), this.getNetworkCharacterAI().targetX, this.getNetworkCharacterAI().targetY) / var13;
            TextManager.instance.DrawStringCentre(var7, (double)var9, (double)var10, String.format("dist:%f scale:%f", var12, var18), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         }
      }

      int var16;
      if (this.getNetworkCharacterAI().getBooleanDebugOptions().Enable.getValue() && this.getNetworkCharacterAI().getBooleanDebugOptions().Hit.getValue() && this.getHitReactionNetworkAI() != null) {
         if (this.getHitReactionNetworkAI().isSetup()) {
            LineDrawer.DrawIsoLine(this.getX(), this.getY(), this.getZ(), this.getX() + this.getHitDir().getX(), this.getY() + this.getHitDir().getY(), this.getZ(), Colors.BlueViolet.r, Colors.BlueViolet.g, Colors.BlueViolet.b, Colors.BlueViolet.a, 1);
            LineDrawer.DrawIsoLine(this.getHitReactionNetworkAI().startPosition.x, this.getHitReactionNetworkAI().startPosition.y, this.getZ(), this.getHitReactionNetworkAI().finalPosition.x, this.getHitReactionNetworkAI().finalPosition.y, this.getZ(), Colors.Salmon.r, Colors.Salmon.g, Colors.Salmon.b, Colors.Salmon.a, 1);
            float var23 = this.getHitReactionNetworkAI().startPosition.x;
            float var10001 = this.getHitReactionNetworkAI().startPosition.y;
            float var10002 = this.getZ();
            float var10007 = Colors.Salmon.r - 0.2F;
            float var10008 = Colors.Salmon.g + 0.2F;
            LineDrawer.DrawIsoTransform(var23, var10001, var10002, this.getHitReactionNetworkAI().startDirection.x, this.getHitReactionNetworkAI().startDirection.y, 0.4F, 16, var10007, var10008, Colors.Salmon.b, Colors.Salmon.a, 1);
            var23 = this.getHitReactionNetworkAI().finalPosition.x;
            var10001 = this.getHitReactionNetworkAI().finalPosition.y;
            var10002 = this.getZ();
            var10008 = Colors.Salmon.g - 0.2F;
            LineDrawer.DrawIsoTransform(var23, var10001, var10002, this.getHitReactionNetworkAI().finalDirection.x, this.getHitReactionNetworkAI().finalDirection.y, 0.4F, 16, Colors.Salmon.r, var10008, Colors.Salmon.b, Colors.Salmon.a, 1);
         }

         if (var1 != null) {
            var8 = Colors.Red;
            var16 = var1.canHaveMultipleHits();
            if (var16 == 0) {
               var8 = Colors.Green;
            } else if (var16 == 1) {
               var8 = Colors.Yellow;
            }

            LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), 0.45F, 4, var8.r, var8.g, var8.b, 0.5F);
            TextManager.instance.DrawStringCentre(var7, (double)IsoUtils.XToScreenExact(this.getX() + 0.4F, this.getY() + 0.4F, this.getZ(), 0), (double)IsoUtils.YToScreenExact(this.getX() + 0.4F, this.getY() - 1.4F, this.getZ(), 0), String.valueOf(this.getOnlineID()), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         }

         if (var2 != null && var2.isLocalPlayer() && var3 == null) {
            HandWeapon var17 = (HandWeapon)Type.tryCastTo(var2.getPrimaryHandItem(), HandWeapon.class);
            if (var17 != null) {
               LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), var17.getMaxRange(var2), 16, var8.r, var8.g, var8.b, 0.5F);
            }
         }
      }

      if (this.getNetworkCharacterAI().getBooleanDebugOptions().Enable.getValue() && this.getNetworkCharacterAI().getBooleanDebugOptions().Prediction.getValue()) {
         if (var1 != null) {
            LineDrawer.DrawIsoTransform(this.realx, this.realy, this.getZ(), this.realdir.ToVector().x, this.realdir.ToVector().y, 0.35F, 16, Colors.Blue.r, Colors.Blue.g, Colors.Blue.b, Colors.Blue.a, 1);
            if (var1.networkAI.DebugInterfaceActive) {
               LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), 0.4F, 4, Colors.NavajoWhite.r, Colors.NavajoWhite.g, Colors.NavajoWhite.b, Colors.NavajoWhite.a);
            } else if (!this.getNetworkCharacterAI().isRemote()) {
               LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), 0.3F, 3, Colors.Magenta.r, Colors.Magenta.g, Colors.Magenta.b, Colors.Magenta.a);
            } else {
               LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), 0.3F, 5, Colors.Magenta.r, Colors.Magenta.g, Colors.Magenta.b, Colors.Magenta.a);
            }

            LineDrawer.DrawIsoTransform(this.getNetworkCharacterAI().targetX, this.getNetworkCharacterAI().targetY, this.getZ(), 1.0F, 0.0F, 0.4F, 16, Colors.LimeGreen.r, Colors.LimeGreen.g, Colors.LimeGreen.b, Colors.LimeGreen.a, 1);
            LineDrawer.DrawIsoLine(this.getX(), this.getY(), this.getZ(), this.getNetworkCharacterAI().targetX, this.getNetworkCharacterAI().targetY, this.getZ(), Colors.LimeGreen.r, Colors.LimeGreen.g, Colors.LimeGreen.b, Colors.LimeGreen.a, 1);
            if (IsoUtils.DistanceToSquared(this.getX(), this.getY(), this.realx, this.realy) > 4.5F) {
               LineDrawer.DrawIsoLine(this.realx, this.realy, this.getZ(), this.getX(), this.getY(), this.getZ(), Colors.Magenta.r, Colors.Magenta.g, Colors.Magenta.b, Colors.Magenta.a, 1);
            } else {
               LineDrawer.DrawIsoLine(this.realx, this.realy, this.getZ(), this.getX(), this.getY(), this.getZ(), Colors.Blue.r, Colors.Blue.g, Colors.Blue.b, Colors.Blue.a, 1);
            }
         } else if (var2 != null) {
            if (var2.networkAI.footstepSoundRadius != 0) {
               LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), (float)var2.networkAI.footstepSoundRadius, 32, Colors.Violet.r, Colors.Violet.g, Colors.Violet.b, Colors.Violet.a);
            }

            if (var2.bRemote || var3 != null && this.getNetworkCharacterAI().getOwnership().getConnection() == null) {
               LineDrawer.DrawIsoCircle(this.getX(), this.getY(), this.getZ(), 0.3F, 16, Colors.OrangeRed.r, Colors.OrangeRed.g, Colors.OrangeRed.b, Colors.OrangeRed.a);
               tempo.set(this.realdir.ToVector());
               LineDrawer.DrawIsoTransform(this.realx, this.realy, this.getZ(), tempo.x, tempo.y, 0.35F, 16, Colors.Blue.r, Colors.Blue.g, Colors.Blue.b, Colors.Blue.a, 1);
               LineDrawer.DrawIsoLine(this.realx, this.realy, this.getZ(), this.getX(), this.getY(), this.getZ(), Colors.Blue.r, Colors.Blue.g, Colors.Blue.b, Colors.Blue.a, 1);
               tempo.set(this.getNetworkCharacterAI().targetX, this.getNetworkCharacterAI().targetY);
               LineDrawer.DrawIsoTransform(tempo.x, tempo.y, this.getZ(), 1.0F, 0.0F, 0.4F, 16, Colors.LimeGreen.r, Colors.LimeGreen.g, Colors.LimeGreen.b, Colors.LimeGreen.a, 1);
               LineDrawer.DrawIsoLine(this.getX(), this.getY(), this.getZ(), tempo.x, tempo.y, this.getZ(), Colors.LimeGreen.r, Colors.LimeGreen.g, Colors.LimeGreen.b, Colors.LimeGreen.a, 1);
            }
         }
      }

      if (this.getNetworkCharacterAI().getBooleanDebugOptions().Enable.getValue() && this.getNetworkCharacterAI().getBooleanDebugOptions().Position.getValue()) {
         var8 = Colors.NavajoWhite;

         for(var16 = PZMath.fastfloor(this.getX()) - 10; var16 < PZMath.fastfloor(this.getX()) + 10; ++var16) {
            for(int var19 = PZMath.fastfloor(this.getY()) - 10; var19 < PZMath.fastfloor(this.getY()) + 10; ++var19) {
               LineDrawer.DrawIsoRect((float)var16, (float)var19, 1.0F, 1.0F, 0, 0.0F, 0.0F, 0.0F);
            }
         }

         var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("x=%09.3f", this.getX()), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("y=%09.3f", this.getY()), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("z=%09.3f", this.getZ()), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         var12 = IsoUtils.DistanceTo(this.getX(), this.getY(), IsoPlayer.getInstance().getX(), IsoPlayer.getInstance().getY());
         var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("d=%09.3f", var12), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         if (this.getPathFindBehavior2().getTargetX() != 0.0F || this.getPathFindBehavior2().getTargetY() != 0.0F) {
            LineDrawer.DrawIsoCircle(this.getPathFindBehavior2().getTargetX(), this.getPathFindBehavior2().getTargetY(), this.getPathFindBehavior2().getTargetZ(), 0.3F, 16, var8.r, var8.g, var8.b, var8.a);
            LineDrawer.DrawIsoLine(this.getPathFindBehavior2().getTargetX(), this.getPathFindBehavior2().getTargetY(), this.getPathFindBehavior2().getTargetZ(), this.getX(), this.getY(), this.getZ(), var8.r, var8.g, var8.b, var8.a, 1);
         }

         if (var3 != null && this.getNetworkCharacterAI() != null && this.getNetworkCharacterAI().getAnimalPacket().pfbData.targetX > 0.0F && this.getNetworkCharacterAI().getAnimalPacket().pfbData.targetY > 0.0F) {
            LineDrawer.DrawIsoCircle(this.getNetworkCharacterAI().getAnimalPacket().pfbData.targetX, this.getNetworkCharacterAI().getAnimalPacket().pfbData.targetY, this.getNetworkCharacterAI().getAnimalPacket().pfbData.targetZ, 0.3F, 16, Colors.Pink.r, Colors.Pink.g, Colors.Pink.b, Colors.Pink.a);
            LineDrawer.DrawIsoLine(this.getNetworkCharacterAI().getAnimalPacket().pfbData.targetX, this.getNetworkCharacterAI().getAnimalPacket().pfbData.targetY, this.getNetworkCharacterAI().getAnimalPacket().pfbData.targetZ, this.getX(), this.getY(), this.getZ(), Colors.Pink.r, Colors.Pink.g, Colors.Pink.b, Colors.Pink.a, 1);
         }
      }

      if (this.getNetworkCharacterAI().getBooleanDebugOptions().Enable.getValue() && this.getNetworkCharacterAI().getBooleanDebugOptions().Variables.getValue()) {
         var8 = Colors.GreenYellow;
         var5.draw(var7, (double)var9, (double)(var10 + (var11 += 16.0F)), String.format("Health: %.03f", var1 == null && var3 == null ? this.getBodyDamage().getOverallBodyHealth() : this.getHealth()), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         if (var2 != null && var3 == null) {
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("IdleSpeed: %s , targetDist: %s ", var2.getVariableString("IdleSpeed"), var2.getVariableString("targetDist")), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("WalkInjury: %s , WalkSpeed: %s", var2.getVariableString("WalkInjury"), var2.getVariableString("WalkSpeed")), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("DeltaX: %s , DeltaY: %s", var2.getVariableString("DeltaX"), var2.getVariableString("DeltaY")), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("AttackVariationX: %s , AttackVariationY: %s", var2.getVariableString("AttackVariationX"), var2.getVariableString("AttackVariationY")), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("autoShootVarX: %s , autoShootVarY: %s", var2.getVariableString("autoShootVarX"), var2.getVariableString("autoShootVarY")), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("recoilVarX: %s , recoilVarY: %s", var2.getVariableString("recoilVarX"), var2.getVariableString("recoilVarY")), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("ShoveAimX: %s , ShoveAimY: %s", var2.getVariableString("ShoveAimX"), var2.getVariableString("ShoveAimY")), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("ForwardDirection: %f", var2.getForwardDirection().getDirection()), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         }

         if (var3 != null) {
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("Stress:%.02f", var3.getStress()), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("Milk: %.02f", var3.getData().getMilkQuantity()), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("Acceptance:%.02f", var3.getAcceptanceLevel(IsoPlayer.getInstance())), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("AlertX:%.02f", var3.getVariableFloat("AlertX", 0.0F)), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         }

         if (var2 != null && var3 == null || var1 != null) {
            var8 = Colors.OrangeRed;
            if (this.getReanimateTimer() <= 0.0F) {
               var8 = Colors.GreenYellow;
            } else if (this.isBeingSteppedOn()) {
               var8 = Colors.Blue;
            }

            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 18.0F)), "Reanimate: " + this.getReanimateTimer(), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         }

         if (var1 != null) {
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 18.0F)), "Prediction: " + this.getNetworkCharacterAI().predictionType, (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("Real state: %s", var1.realState), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            if (var1.target instanceof IsoPlayer) {
               var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), "Target: " + ((IsoPlayer)var1.target).username + "  =" + var1.vectorToTarget.getLength(), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            } else {
               var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), "Target: " + var1.target + "  =" + var1.vectorToTarget.getLength(), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            }
         }
      }

      if (this.getNetworkCharacterAI().getBooleanDebugOptions().Enable.getValue() && this.getNetworkCharacterAI().getBooleanDebugOptions().State.getValue()) {
         var8 = Colors.LightBlue;
         if (this.isCurrentState(AnimalPathFindState.instance())) {
            var8 = Colors.Pink;
         }

         if (var3 != null && var3.alertedChr != null) {
            LineDrawer.DrawIsoCircle(var3.alertedChr.getX(), var3.alertedChr.getY(), var3.alertedChr.getZ(), 0.3F, Colors.OrangeRed.r, Colors.OrangeRed.g, Colors.OrangeRed.b, Colors.OrangeRed.a);
            LineDrawer.DrawIsoLine(var3.alertedChr.getX(), var3.alertedChr.getY(), var3.alertedChr.getZ(), var3.getX(), var3.getY(), var3.getZ(), Colors.OrangeRed.r, Colors.OrangeRed.g, Colors.OrangeRed.b, Colors.OrangeRed.a, 1);
         }

         String var20;
         if (this.advancedAnimator.getRootLayer() != null) {
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 16.0F)), "Set: " + this.advancedAnimator.animSet.m_Name, (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), "State: " + this.advancedAnimator.getCurrentStateName(), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            var20 = this.advancedAnimator.getRootLayer().getDebugNodeName().replace(this.advancedAnimator.animSet.m_Name + "/", "").replace(this.advancedAnimator.getCurrentStateName() + "/", "");
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), "Node: " + var20, (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         }

         var5.draw(var7, (double)var9, (double)(var10 + (var11 += 16.0F)), String.format("Previous: %s", this.getPreviousStateName()), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("( %s )", this.getPreviousActionContextStateName()), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("Current: %s", this.getCurrentStateName()), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("( %s )", this.getCurrentActionContextStateName()), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         var20 = this.getActionContext() != null && this.getActionContext().getChildStates() != null && !this.getActionContext().getChildStates().isEmpty() && this.getActionContext().getChildStateAt(0) != null ? this.getActionContext().getChildStateAt(0).getName() : "";
         var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("Child: %s", var20), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         if (this.CharacterActions != null) {
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("Character actions: %d", this.CharacterActions.size()), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            Iterator var22 = this.CharacterActions.iterator();

            while(var22.hasNext()) {
               BaseAction var21 = (BaseAction)var22.next();
               if (var21 instanceof LuaTimedActionNew) {
                  var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("Action: %s", ((LuaTimedActionNew)var21).getMetaType()), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
               }
            }
         }

         var5.draw(var7, (double)var9, (double)(var10 + (var11 += 16.0F)), String.format("isHitFromBehind=%b/%b", this.isHitFromBehind(), this.getVariableBoolean("frombehind")), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("bKnockedDown=%b/%b", this.isKnockedDown(), this.getVariableBoolean("bknockeddown")), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("isFallOnFront=%b/%b", this.isFallOnFront(), this.getVariableBoolean("fallonfront")), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("isOnFloor=%b/%b", this.isOnFloor(), this.getVariableBoolean("bonfloor")), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("isSitOnGround=%b/%b", this.isSitOnGround(), this.getVariableBoolean("sitonground")), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("isDead=%b/%b", this.isDead(), this.getVariableBoolean("bdead")), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         if (var1 != null) {
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("bThump=%b", this.getVariableString("bThump")), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("ThumpType=%s", this.getVariableString("ThumpType")), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("onknees=%b", this.getVariableBoolean("onknees")), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
         } else {
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("isBumped=%b/%s", this.isBumped(), this.getBumpType()), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("bMoving=%b/%s", this.isMoving(), this.getVariableBoolean("bMoving")), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("bPathfind=%s", this.getVariableBoolean("bPathfind")), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            if (var3 != null) {
               var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("isAlerted=%b", var3.isAlerted()), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
               var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("animalSpeed=%f", this.getVariableFloat("animalSpeed", -1.0F)), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
               var5.draw(var7, (double)var9, (double)(var10 + (var11 += 12.0F)), String.format("animalRunning=%s", this.getVariableBoolean("animalRunning")), (double)var8.r, (double)var8.g, (double)var8.b, (double)var8.a);
            }
         }
      }

   }

   public float getCorpseSicknessRate() {
      return this.corpseSicknessRate;
   }

   public void setCorpseSicknessRate(float var1) {
      this.corpseSicknessRate = Math.max(0.0F, var1);
   }

   public void spikePartIndex(int var1) {
      DebugLog.Combat.debugln("" + this + " got spiked in " + BodyPartType.getDisplayName(BodyPartType.FromIndex(var1)));
      HandWeapon var2 = (HandWeapon)InventoryItemFactory.CreateItem("Base.IcePick");
      if (this.getBodyDamage() == null) {
         this.splatBloodFloorBig();
         this.getEmitter().playSoundImpl(var2.getZombieHitSound(), (IsoObject)null);
         this.Hit(var2, IsoWorld.instance.CurrentCell.getFakeZombieForHit(), 0.0F, false, 0.0F);
      } else if (this instanceof IsoAnimal) {
         this.splatBloodFloorBig();
         this.getEmitter().playSoundImpl(var2.getZombieHitSound(), (IsoObject)null);
         this.Hit(var2, IsoWorld.instance.CurrentCell.getFakeZombieForHit(), 0.0F, false, 0.0F);
      } else {
         this.getBodyDamage().DamageFromWeapon(var2, var1);
      }
   }

   public void spikePart(BodyPartType var1) {
      int var2 = BodyPartType.ToIndex(var1);
      this.spikePartIndex(var2);
   }

   public IsoGameCharacter getReanimatedCorpse() {
      return this.ReanimatedCorpse;
   }

   public void applyDamage(float var1) {
      this.Health -= var1;
      if (this.Health < 0.0F) {
         this.Health = 0.0F;
      }

   }

   public boolean useRagdoll() {
      return this.useRagdoll;
   }

   public RagdollController getRagdollController() {
      AnimationPlayer var1 = this.getAnimationPlayer();
      return var1 == null ? null : var1.getRagdollController();
   }

   public void releaseRagdollController() {
      AnimationPlayer var1 = this.getAnimationPlayer();
      if (var1 != null) {
         var1.releaseRagdollController();
      }
   }

   public boolean useBallistics() {
      return useBallistics;
   }

   public BallisticsController getBallisticsController() {
      return this.ballisticsController;
   }

   public void updateBallistics() {
      if (!GameServer.bServer) {
         if (this.ballisticsController == null) {
            this.ballisticsController = BallisticsController.alloc();
            this.ballisticsController.setIsoGameCharacter(this);
         }

         this.ballisticsController.update();
      }
   }

   public void releaseBallisticsController() {
      if (this.ballisticsController != null) {
         this.ballisticsController.releaseController();
         this.ballisticsController = null;
      }

   }

   public BallisticsTarget getBallisticsTarget() {
      return this.ballisticsTarget;
   }

   public BallisticsTarget ensureExitsBallisticsTarget(IsoGameCharacter var1) {
      if (var1 == null) {
         return null;
      } else {
         if (this.ballisticsTarget == null) {
            this.ballisticsTarget = BallisticsTarget.alloc(var1);
         }

         this.ballisticsTarget.setIsoGameCharacter(var1);
         return this.ballisticsTarget;
      }
   }

   private void updateBallisticsTarget() {
      if (this.ballisticsTarget != null) {
         boolean var1 = this.ballisticsTarget.update();
         if (var1) {
            this.releaseBallisticsTarget();
         }

      }
   }

   public void releaseBallisticsTarget() {
      if (this.ballisticsTarget != null) {
         this.ballisticsTarget.releaseTarget();
         this.ballisticsTarget = null;
      }

   }

   public boolean canReachTo(IsoGridSquare var1) {
      return this.getSquare().canReachTo(var1);
   }

   public boolean canUseAsGenericCraftingSurface(IsoObject var1) {
      return var1.isGenericCraftingSurface() && this.getSquare().canReachTo(var1.getSquare());
   }

   public ArrayList<HitInfo> getHitInfoList() {
      return this.hitInfoList;
   }

   public AttackVars getAttackVars() {
      return this.attackVars;
   }

   public void addCombatMuscleStrain(HandWeapon var1) {
      this.addCombatMuscleStrain(var1, 1);
   }

   public void addCombatMuscleStrain(HandWeapon var1, int var2) {
      this.addCombatMuscleStrain(var1, var2, 1.0F);
   }

   public void addCombatMuscleStrain(HandWeapon var1, int var2, float var3) {
      float var10;
      float var11;
      if (this.isStomping()) {
         var10 = 0.3F;
         var11 = (float)(15 - this.getPerkLevel(PerkFactory.Perks.Strength)) / 10.0F;
         var10 *= var11;
         this.addRightLegMuscleStrain(var10);
      } else if (this.isShoving()) {
         var10 = 0.15F;
         var11 = (float)(15 - this.getPerkLevel(PerkFactory.Perks.Strength)) / 10.0F;
         var10 *= var11;
         this.addBothArmMuscleStrain(var10);
      } else if (var1.isAimedFirearm()) {
         var10 = (float)var1.getRecoilDelay(this) * CombatManager.FirearmRecoilMuscleStrainModifier * var1.muscleStrainMod(this) * ((float)(15 - this.getPerkLevel(PerkFactory.Perks.Strength)) / 10.0F);
         if ("Auto".equalsIgnoreCase(var1.getFireMode())) {
            var10 *= 0.5F;
         }

         var10 *= (float)SandboxOptions.instance.MuscleStrainFactor.getValue();
         if (var10 != 0.0F) {
            this.addStiffness(BodyPartType.Hand_R, var10);
            this.addStiffness(BodyPartType.ForeArm_R, var10);
            if (this.getSecondaryHandItem() == var1) {
               this.addStiffness(BodyPartType.UpperArm_R, var10);
               this.addStiffness(BodyPartType.Hand_L, var10 * 0.1F);
               this.addStiffness(BodyPartType.ForeArm_L, var10 * 0.1F);
            }

         }
      } else if (this.isActuallyAttackingWithMeleeWeapon()) {
         if (SandboxOptions.instance.MuscleStrainFactor.getValue() > 0.0 && var1.isUseEndurance() && WeaponType.getWeaponType(this) != WeaponType.barehand && !var1.isRanged() && !this.isForceShove()) {
            if (var2 <= 0) {
               var2 = 1;
            }

            if (var3 <= 0.0F) {
               var3 = 1.0F;
            }

            boolean var4 = var1.isTwoHandWeapon();
            boolean var5 = var1.isTwoHandWeapon() && (this.getPrimaryHandItem() != var1 || this.getSecondaryHandItem() != var1);
            float var6 = 0.0F;
            if (var5) {
               var6 = var1.getWeight() / 1.5F / 10.0F;
               var4 = false;
            }

            float var7 = (var1.getWeight() * 0.15F * var1.getEnduranceMod() * 0.3F + var6) * 4.0F;
            float var8 = 1.0F;
            var8 *= (float)(var2 + 1);
            var7 *= var8;
            float var9 = (float)(15 - this.getPerkLevel(PerkFactory.Perks.Strength)) / 10.0F;
            var7 *= var9;
            var7 *= var1.muscleStrainMod(this);
            var7 *= var3;
            if (var4) {
               var7 *= 0.5F;
            }

            this.addArmMuscleStrain(var7);
            if (var4) {
               this.addLeftArmMuscleStrain(var7);
            }
         }

      }
   }

   public void addRightLegMuscleStrain(float var1) {
      if (SandboxOptions.instance.MuscleStrainFactor.getValue() > 0.0) {
         var1 = (float)((double)var1 * 2.5);
         var1 = (float)((double)var1 * SandboxOptions.instance.MuscleStrainFactor.getValue());
         this.addStiffness(BodyPartType.UpperLeg_R, var1);
         this.addStiffness(BodyPartType.LowerLeg_R, var1);
         this.addStiffness(BodyPartType.Foot_R, var1);
      }

   }

   public void addBackMuscleStrain(float var1) {
      if (SandboxOptions.instance.MuscleStrainFactor.getValue() > 0.0) {
         var1 = (float)((double)var1 * 2.5);
         var1 = (float)((double)var1 * SandboxOptions.instance.MuscleStrainFactor.getValue());
         this.addStiffness(BodyPartType.Torso_Upper, var1);
         this.addStiffness(BodyPartType.Torso_Lower, var1);
      }

   }

   public void addNeckMuscleStrain(float var1) {
      if (SandboxOptions.instance.MuscleStrainFactor.getValue() > 0.0) {
         var1 = (float)((double)var1 * 2.5);
         var1 = (float)((double)var1 * SandboxOptions.instance.MuscleStrainFactor.getValue());
         this.addStiffness(BodyPartType.Neck, var1);
      }

   }

   public void addArmMuscleStrain(float var1) {
      if (SandboxOptions.instance.MuscleStrainFactor.getValue() > 0.0) {
         var1 = (float)((double)var1 * 2.5);
         var1 = (float)((double)var1 * SandboxOptions.instance.MuscleStrainFactor.getValue());
         this.addStiffness(BodyPartType.Hand_R, var1);
         this.addStiffness(BodyPartType.ForeArm_R, var1);
         this.addStiffness(BodyPartType.UpperArm_R, var1);
      }

   }

   public void addLeftArmMuscleStrain(float var1) {
      if (SandboxOptions.instance.MuscleStrainFactor.getValue() > 0.0) {
         var1 = (float)((double)var1 * 2.5);
         var1 = (float)((double)var1 * SandboxOptions.instance.MuscleStrainFactor.getValue());
         this.addStiffness(BodyPartType.Hand_L, var1);
         this.addStiffness(BodyPartType.ForeArm_L, var1);
         this.addStiffness(BodyPartType.UpperArm_L, var1);
      }

   }

   public void addBothArmMuscleStrain(float var1) {
      this.addArmMuscleStrain(var1);
      this.addLeftArmMuscleStrain(var1);
   }

   public void addStiffness(BodyPartType var1, float var2) {
      BodyPart var3 = this.getBodyDamage().getBodyPart(var1);
      var3.addStiffness(var2);
   }

   public int getClimbingFailChanceInt() {
      return (int)this.getClimbingFailChanceFloat();
   }

   public float getClimbingFailChanceFloat() {
      float var1 = 0.0F;
      var1 += (float)(this.getPerkLevel(PerkFactory.Perks.Fitness) * 2);
      var1 += (float)(this.getPerkLevel(PerkFactory.Perks.Strength) * 2);
      var1 += (float)(this.getPerkLevel(PerkFactory.Perks.Nimble) * 2);
      var1 -= (float)(this.getMoodles().getMoodleLevel(MoodleType.Endurance) * 5);
      var1 -= (float)(this.getMoodles().getMoodleLevel(MoodleType.Drunk) * 8);
      var1 -= (float)(this.getMoodles().getMoodleLevel(MoodleType.HeavyLoad) * 8);
      var1 -= (float)(this.getMoodles().getMoodleLevel(MoodleType.Pain) * 5);
      if (this.Traits.Obese.isSet()) {
         var1 -= 25.0F;
      } else if (this.getTraits().contains("Overweight")) {
         var1 -= 15.0F;
      }

      if (this.getTraits().contains("Clumsy")) {
         var1 /= 2.0F;
      }

      if (this.isWearingAwkwardGloves()) {
         var1 /= 2.0F;
      } else if (!this.isWearingAwkwardGloves() && this.isWearingGloves()) {
         var1 += 4.0F;
      }

      if (this.HasTrait("AllThumbs")) {
         var1 -= 4.0F;
      } else if (this.HasTrait("Dextrous")) {
         var1 += 4.0F;
      }

      if (this.HasTrait("Burglar")) {
         var1 += 4.0F;
      }

      if (this.HasTrait("Gymnast")) {
         var1 += 4.0F;
      }

      IsoGridSquare var2 = this.getCurrentSquare();
      if (var2 != null) {
         for(int var3 = 0; var3 < var2.getMovingObjects().size(); ++var3) {
            IsoMovingObject var4 = (IsoMovingObject)var2.getMovingObjects().get(var3);
            if (var4 instanceof IsoZombie) {
               if (((IsoZombie)var4).target == this && ((IsoZombie)var4).getCurrentState() == AttackState.instance()) {
                  var1 -= 25.0F;
               } else {
                  var1 -= 7.0F;
               }
            }
         }
      }

      var1 = Math.max(0.0F, var1);
      var1 = (float)((int)Math.sqrt((double)var1));
      return var1;
   }

   public boolean isClimbingRope() {
      return this.getCurrentState().equals(ClimbSheetRopeState.instance()) || this.getCurrentState().equals(ClimbDownSheetRopeState.instance());
   }

   public void fallFromRope() {
      if (this.isClimbingRope()) {
         this.setCollidable(true);
         this.setbClimbing(false);
         this.setbFalling(true);
         this.clearVariable("ClimbRope");
         this.setLlz(this.getZ());
      }
   }

   public boolean isWearingGloves() {
      return this.getWornItem("Hands") != null;
   }

   public boolean isWearingAwkwardGloves() {
      return this.getWornItem("Hands") != null && this.getWornItem("Hands").hasTag("AwkwardGloves");
   }

   public float getClimbRopeSpeed(boolean var1) {
      int var3 = Math.max(this.getPerkLevel(PerkFactory.Perks.Strength), this.getPerkLevel(PerkFactory.Perks.Fitness)) - (this.getMoodles().getMoodleLevel(MoodleType.Drunk) + this.getMoodles().getMoodleLevel(MoodleType.Endurance) + this.getMoodles().getMoodleLevel(MoodleType.Pain));
      if (!var1) {
         var3 -= this.getMoodles().getMoodleLevel(MoodleType.HeavyLoad);
         if (this.Traits.Obese.isSet()) {
            var3 -= 2;
         } else if (this.getTraits().contains("Overweight")) {
            --var3;
         }
      }

      if (this.HasTrait("AllThumbs")) {
         --var3;
      } else if (this.HasTrait("Dextrous")) {
         ++var3;
      }

      if (this.HasTrait("Burglar")) {
         ++var3;
      }

      if (this.HasTrait("Gymnast")) {
         ++var3;
      }

      if (this.isWearingAwkwardGloves()) {
         var3 /= 2;
      } else if (this.isWearingGloves()) {
         ++var3;
      }

      var3 = Math.max(0, var3);
      var3 = Math.min(10, var3);
      float var2;
      if (var1) {
         var2 = 0.16F;
      } else {
         var2 = 0.16F;
      }

      switch (var3) {
         case 0:
            var2 -= 0.12F;
            break;
         case 1:
            var2 -= 0.11F;
            break;
         case 2:
            var2 -= 0.1F;
            break;
         case 3:
            var2 -= 0.09F;
         case 4:
         case 5:
         default:
            break;
         case 6:
            var2 += 0.02F;
            break;
         case 7:
            var2 += 0.05F;
            break;
         case 8:
            var2 += 0.07F;
            break;
         case 9:
            var2 += 0.09F;
            break;
         case 10:
            var2 += 0.12F;
      }

      if (var1) {
         var2 *= 0.5F;
      } else {
         var2 *= 0.5F;
      }

      return var2;
   }

   public void setClimbRopeTime(float var1) {
      this.climbRopeTime = var1;
   }

   public float getClimbRopeTime() {
      return this.climbRopeTime;
   }

   public boolean hasAwkwardHands() {
      return this.isWearingAwkwardGloves() || this.HasTrait("AllThumbs");
   }

   public void triggerContextualAction(String var1) {
      LuaHookManager.TriggerHook("ContextualAction", var1, this);
   }

   public void triggerContextualAction(String var1, Object var2) {
      LuaHookManager.TriggerHook("ContextualAction", var1, this, var2);
   }

   public void triggerContextualAction(String var1, Object var2, Object var3) {
      LuaHookManager.TriggerHook("ContextualAction", var1, this, var2, var3);
   }

   public void triggerContextualAction(String var1, Object var2, Object var3, Object var4) {
      LuaHookManager.TriggerHook("ContextualAction", var1, this, var2, var3, var4);
   }

   public void triggerContextualAction(String var1, Object var2, Object var3, Object var4, Object var5) {
      LuaHookManager.TriggerHook("ContextualAction", var1, this, var2, var3, var4, var5);
   }

   public boolean isActuallyAttackingWithMeleeWeapon() {
      if (this.getPrimaryHandItem() == null) {
         return false;
      } else if (!(this.getPrimaryHandItem() instanceof HandWeapon)) {
         return false;
      } else if (this.getUseHandWeapon() == null) {
         return false;
      } else {
         HandWeapon var1 = this.getUseHandWeapon();
         if (WeaponType.getWeaponType(this) == WeaponType.barehand) {
            return false;
         } else if (var1.isRanged()) {
            return false;
         } else if (this.isShoving()) {
            return false;
         } else {
            return !this.isStomping();
         }
      }
   }

   public boolean isStomping() {
      return this.isAimAtFloor() && ((IsoPlayer)this).isDoShove();
   }

   public boolean isShoving() {
      return (this.isForceShove() || ((IsoPlayer)this).isDoShove()) && !this.isStomping();
   }

   public class XP implements AntiCheatXPUpdate.IAntiCheatUpdate {
      public int level = 0;
      public int lastlevel = 0;
      public float TotalXP = 0.0F;
      public HashMap<PerkFactory.Perk, Float> XPMap = new HashMap();
      public HashMap<PerkFactory.Perk, XPMultiplier> XPMapMultiplier = new HashMap();
      IsoGameCharacter chr = null;
      private static final long XP_INTERVAL = 60000L;
      private final UpdateLimit ulInterval = new UpdateLimit(60000L);
      private float sum = 0.0F;

      public XP(IsoGameCharacter var2) {
         this.chr = var2;
      }

      public boolean intervalCheck() {
         return this.ulInterval.Check();
      }

      public float getGrowthRate() {
         this.ulInterval.Reset(60000L);
         float var1 = 0.0F;

         Float var3;
         for(Iterator var2 = this.XPMap.values().iterator(); var2.hasNext(); var1 += var3) {
            var3 = (Float)var2.next();
         }

         float var4 = var1 - this.sum;
         this.sum = var1;
         return var4;
      }

      public float getMultiplier() {
         double var1 = 0.0;
         if (SandboxOptions.instance.multipliersConfig.XPMultiplierGlobalToggle.getValue()) {
            var1 = SandboxOptions.instance.multipliersConfig.XPMultiplierGlobal.getValue();
         } else {
            int var4 = 0;

            for(int var5 = 0; var5 < IsoGameCharacter.this.getPerkList().size(); ++var5) {
               String var3 = "MultiplierConfig." + IsoGameCharacter.this.getPerkList().get(var5);
               if (SandboxOptions.instance.getOptionByName(var3) != null) {
                  ++var4;
                  var1 += Double.parseDouble(SandboxOptions.instance.getOptionByName(var3).asConfigOption().getValueAsString());
               }
            }

            var1 /= (double)var4;
         }

         return (float)var1;
      }

      public void addXpMultiplier(PerkFactory.Perk var1, float var2, int var3, int var4) {
         XPMultiplier var5 = (XPMultiplier)this.XPMapMultiplier.get(var1);
         if (var5 == null) {
            var5 = new XPMultiplier();
         }

         var5.multiplier = var2;
         var5.minLevel = var3;
         var5.maxLevel = var4;
         this.XPMapMultiplier.put(var1, var5);
      }

      public HashMap<PerkFactory.Perk, XPMultiplier> getMultiplierMap() {
         return this.XPMapMultiplier;
      }

      public float getMultiplier(PerkFactory.Perk var1) {
         XPMultiplier var2 = (XPMultiplier)this.XPMapMultiplier.get(var1);
         return var2 == null ? 0.0F : var2.multiplier;
      }

      public int getPerkBoost(PerkFactory.Perk var1) {
         return IsoGameCharacter.this.getDescriptor().getXPBoostMap().get(var1) != null ? (Integer)IsoGameCharacter.this.getDescriptor().getXPBoostMap().get(var1) : 0;
      }

      public void setPerkBoost(PerkFactory.Perk var1, int var2) {
         if (var1 != null && var1 != PerkFactory.Perks.None && var1 != PerkFactory.Perks.MAX) {
            var2 = PZMath.clamp(var2, 0, 10);
            if (var2 == 0) {
               IsoGameCharacter.this.getDescriptor().getXPBoostMap().remove(var1);
            } else {
               IsoGameCharacter.this.getDescriptor().getXPBoostMap().put(var1, var2);
            }
         }
      }

      public int getLevel() {
         return this.level;
      }

      public void setLevel(int var1) {
         this.level = var1;
      }

      public float getTotalXp() {
         return this.TotalXP;
      }

      public void AddXP(PerkFactory.Perk var1, float var2) {
         if (this.chr instanceof IsoPlayer && ((IsoPlayer)this.chr).isLocalPlayer()) {
            this.AddXP(var1, var2, true, true, false);
         }

      }

      public void AddXP(PerkFactory.Perk var1, float var2, boolean var3) {
         if (this.chr instanceof IsoPlayer && ((IsoPlayer)this.chr).isLocalPlayer()) {
            this.AddXP(var1, var2, true, !var3, false);
         }

      }

      public void AddXPNoMultiplier(PerkFactory.Perk var1, float var2) {
         XPMultiplier var3 = (XPMultiplier)this.getMultiplierMap().remove(var1);

         try {
            this.AddXP(var1, var2);
         } finally {
            if (var3 != null) {
               this.getMultiplierMap().put(var1, var3);
            }

         }

      }

      public void AddXP(PerkFactory.Perk var1, float var2, boolean var3, boolean var4, boolean var5) {
         if (!var5 && GameClient.bClient && this.chr instanceof IsoPlayer) {
            GameClient.instance.sendAddXp((IsoPlayer)this.chr, var1, var2, !var4);
         }

         PerkFactory.Perk var6 = null;

         for(int var7 = 0; var7 < PerkFactory.PerkList.size(); ++var7) {
            PerkFactory.Perk var8 = (PerkFactory.Perk)PerkFactory.PerkList.get(var7);
            if (var8.getType() == var1) {
               var6 = var8;
               break;
            }
         }

         if (var6.getType() != PerkFactory.Perks.Fitness || !(this.chr instanceof IsoPlayer) || ((IsoPlayer)this.chr).getNutrition().canAddFitnessXp()) {
            if (var6.getType() == PerkFactory.Perks.Strength && this.chr instanceof IsoPlayer) {
               if (((IsoPlayer)this.chr).getNutrition().getProteins() > 50.0F && ((IsoPlayer)this.chr).getNutrition().getProteins() < 300.0F) {
                  var2 = (float)((double)var2 * 1.5);
               }

               if (((IsoPlayer)this.chr).getNutrition().getProteins() < -300.0F) {
                  var2 = (float)((double)var2 * 0.7);
               }
            }

            float var14 = this.getXP(var1);
            float var15 = var6.getTotalXpForLevel(10);
            if (!(var2 >= 0.0F) || !(var14 >= var15)) {
               float var9 = 1.0F;
               if (var4) {
                  boolean var10 = false;
                  Iterator var11 = IsoGameCharacter.this.getDescriptor().getXPBoostMap().entrySet().iterator();

                  label216:
                  while(true) {
                     while(true) {
                        Map.Entry var12;
                        do {
                           if (!var11.hasNext()) {
                              if (!var10 && !this.isSkillExcludedFromSpeedReduction(var6.getType())) {
                                 var9 = 0.25F;
                              }

                              if (IsoGameCharacter.this.Traits.FastLearner.isSet() && !this.isSkillExcludedFromSpeedIncrease(var6.getType())) {
                                 var9 *= 1.3F;
                              }

                              if (IsoGameCharacter.this.Traits.SlowLearner.isSet() && !this.isSkillExcludedFromSpeedReduction(var6.getType())) {
                                 var9 *= 0.7F;
                              }

                              if (IsoGameCharacter.this.Traits.Pacifist.isSet()) {
                                 if (var6.getType() != PerkFactory.Perks.SmallBlade && var6.getType() != PerkFactory.Perks.LongBlade && var6.getType() != PerkFactory.Perks.SmallBlunt && var6.getType() != PerkFactory.Perks.Spear && var6.getType() != PerkFactory.Perks.Blunt && var6.getType() != PerkFactory.Perks.Axe) {
                                    if (var6.getType() == PerkFactory.Perks.Aiming) {
                                       var9 *= 0.75F;
                                    }
                                 } else {
                                    var9 *= 0.75F;
                                 }
                              }

                              var2 *= var9;
                              float var17 = this.getMultiplier(var1);
                              if (var17 > 1.0F) {
                                 var2 *= var17;
                              }

                              if (SandboxOptions.instance.multipliersConfig.XPMultiplierGlobalToggle.getValue()) {
                                 var2 = (float)((double)var2 * SandboxOptions.instance.multipliersConfig.XPMultiplierGlobal.getValue());
                              } else {
                                 var2 *= Float.parseFloat(SandboxOptions.instance.getOptionByName("MultiplierConfig." + var6.getType()).asConfigOption().getValueAsString());
                              }
                              break label216;
                           }

                           var12 = (Map.Entry)var11.next();
                        } while(var12.getKey() != var6.getType());

                        var10 = true;
                        if ((Integer)var12.getValue() == 0 && !this.isSkillExcludedFromSpeedReduction((PerkFactory.Perk)var12.getKey())) {
                           var9 *= 0.25F;
                        } else if ((Integer)var12.getValue() == 1 && var12.getKey() == PerkFactory.Perks.Sprinting) {
                           var9 = (float)((double)var9 * 1.25);
                        } else if ((Integer)var12.getValue() == 1) {
                           var9 = (float)((double)var9 * 1.0);
                        } else if ((Integer)var12.getValue() == 2 && !this.isSkillExcludedFromSpeedIncrease((PerkFactory.Perk)var12.getKey())) {
                           var9 = (float)((double)var9 * 1.33);
                        } else if ((Integer)var12.getValue() >= 3 && !this.isSkillExcludedFromSpeedIncrease((PerkFactory.Perk)var12.getKey())) {
                           var9 = (float)((double)var9 * 1.66);
                        }
                     }
                  }
               }

               float var16 = var14 + var2;
               if (var16 < 0.0F) {
                  var16 = 0.0F;
                  var2 = -var14;
               }

               if (var16 > var15) {
                  var16 = var15;
                  var2 = var15 - var14;
               }

               this.XPMap.put(var1, var16);
               XPMultiplier var18 = (XPMultiplier)this.getMultiplierMap().get(var6);
               float var13;
               float var19;
               if (var18 != null) {
                  var19 = var6.getTotalXpForLevel(var18.minLevel - 1);
                  var13 = var6.getTotalXpForLevel(var18.maxLevel);
                  if (var14 >= var19 && var16 < var19 || var14 < var13 && var16 >= var13) {
                     this.getMultiplierMap().remove(var6);
                  }
               }

               for(var19 = var6.getTotalXpForLevel(this.chr.getPerkLevel(var6) + 1); var14 < var19 && var16 >= var19; var19 = var6.getTotalXpForLevel(this.chr.getPerkLevel(var6) + 1)) {
                  IsoGameCharacter.this.LevelPerk(var1);
                  if (this.chr instanceof IsoPlayer && ((IsoPlayer)this.chr).isLocalPlayer() && (var6 != PerkFactory.Perks.Strength && var6 != PerkFactory.Perks.Fitness || this.chr.getPerkLevel(var6) != 10) && !this.chr.getEmitter().isPlaying("GainExperienceLevel")) {
                     this.chr.getEmitter().playSoundImpl("GainExperienceLevel", (IsoObject)null);
                  }

                  if (this.chr.getPerkLevel(var6) >= 10) {
                     break;
                  }
               }

               for(var13 = var6.getTotalXpForLevel(this.chr.getPerkLevel(var6)); var14 >= var13 && var16 < var13; var13 = var6.getTotalXpForLevel(this.chr.getPerkLevel(var6))) {
                  IsoGameCharacter.this.LoseLevel(var6);
                  if (this.chr.getPerkLevel(var6) >= 10) {
                     break;
                  }
               }

               if (!GameServer.bServer) {
                  LuaEventManager.triggerEventGarbage("AddXP", this.chr, var1, var2);
               }

            }
         }
      }

      private boolean isSkillExcludedFromSpeedReduction(PerkFactory.Perk var1) {
         if (var1 == PerkFactory.Perks.Sprinting) {
            return true;
         } else if (var1 == PerkFactory.Perks.Fitness) {
            return true;
         } else {
            return var1 == PerkFactory.Perks.Strength;
         }
      }

      private boolean isSkillExcludedFromSpeedIncrease(PerkFactory.Perk var1) {
         if (var1 == PerkFactory.Perks.Fitness) {
            return true;
         } else {
            return var1 == PerkFactory.Perks.Strength;
         }
      }

      public float getXP(PerkFactory.Perk var1) {
         return this.XPMap.containsKey(var1) ? (Float)this.XPMap.get(var1) : 0.0F;
      }

      /** @deprecated */
      @Deprecated
      public void AddXP(HandWeapon var1, int var2) {
      }

      public void setTotalXP(float var1) {
         this.TotalXP = var1;
      }

      private void savePerk(ByteBuffer var1, PerkFactory.Perk var2) throws IOException {
         GameWindow.WriteStringUTF(var1, var2 == null ? "" : var2.getId());
      }

      private PerkFactory.Perk loadPerk(ByteBuffer var1, int var2) throws IOException {
         String var3 = GameWindow.ReadStringUTF(var1);
         PerkFactory.Perk var4 = PerkFactory.Perks.FromString(var3);
         return var4 == PerkFactory.Perks.MAX ? null : var4;
      }

      public void load(ByteBuffer var1, int var2) throws IOException {
         int var3 = var1.getInt();
         this.chr.Traits.clear();

         int var4;
         for(var4 = 0; var4 < var3; ++var4) {
            String var5 = GameWindow.ReadString(var1);
            if (TraitFactory.getTrait(var5) != null) {
               if (!this.chr.Traits.contains(var5)) {
                  this.chr.Traits.add(var5);
               }
            } else {
               DebugLog.General.error("unknown trait \"" + var5 + "\"");
            }
         }

         this.TotalXP = var1.getFloat();
         this.level = var1.getInt();
         this.lastlevel = var1.getInt();
         this.XPMap.clear();
         var4 = var1.getInt();

         int var12;
         for(var12 = 0; var12 < var4; ++var12) {
            PerkFactory.Perk var6 = this.loadPerk(var1, var2);
            float var7 = var1.getFloat();
            if (var6 != null) {
               this.XPMap.put(var6, var7);
            }
         }

         IsoGameCharacter.this.PerkList.clear();
         var12 = var1.getInt();

         int var13;
         for(var13 = 0; var13 < var12; ++var13) {
            PerkFactory.Perk var14 = this.loadPerk(var1, var2);
            int var8 = var1.getInt();
            if (var14 != null) {
               PerkInfo var9 = IsoGameCharacter.this.new PerkInfo();
               var9.perk = var14;
               var9.level = var8;
               IsoGameCharacter.this.PerkList.add(var9);
            }
         }

         var13 = var1.getInt();

         for(int var15 = 0; var15 < var13; ++var15) {
            PerkFactory.Perk var16 = this.loadPerk(var1, var2);
            float var17 = var1.getFloat();
            byte var10 = var1.get();
            byte var11 = var1.get();
            if (var16 != null) {
               this.addXpMultiplier(var16, var17, var10, var11);
            }
         }

         if (this.TotalXP > (float)IsoGameCharacter.this.getXpForLevel(this.getLevel() + 1)) {
            this.setTotalXP((float)this.chr.getXpForLevel(this.getLevel()));
         }

         this.getGrowthRate();
      }

      public void save(ByteBuffer var1) throws IOException {
         var1.putInt(this.chr.Traits.size());

         for(int var2 = 0; var2 < this.chr.Traits.size(); ++var2) {
            GameWindow.WriteString(var1, this.chr.Traits.get(var2));
         }

         var1.putFloat(this.TotalXP);
         var1.putInt(this.level);
         var1.putInt(this.lastlevel);
         var1.putInt(this.XPMap.size());
         Iterator var5 = this.XPMap.entrySet().iterator();

         while(var5 != null && var5.hasNext()) {
            Map.Entry var3 = (Map.Entry)var5.next();
            this.savePerk(var1, (PerkFactory.Perk)var3.getKey());
            var1.putFloat((Float)var3.getValue());
         }

         var1.putInt(IsoGameCharacter.this.PerkList.size());

         for(int var6 = 0; var6 < IsoGameCharacter.this.PerkList.size(); ++var6) {
            PerkInfo var4 = (PerkInfo)IsoGameCharacter.this.PerkList.get(var6);
            this.savePerk(var1, var4.perk);
            var1.putInt(var4.level);
         }

         var1.putInt(this.XPMapMultiplier.size());
         Iterator var7 = this.XPMapMultiplier.entrySet().iterator();

         while(var7 != null && var7.hasNext()) {
            Map.Entry var8 = (Map.Entry)var7.next();
            this.savePerk(var1, (PerkFactory.Perk)var8.getKey());
            var1.putFloat(((XPMultiplier)var8.getValue()).multiplier);
            var1.put((byte)((XPMultiplier)var8.getValue()).minLevel);
            var1.put((byte)((XPMultiplier)var8.getValue()).maxLevel);
         }

      }

      public void setXPToLevel(PerkFactory.Perk var1, int var2) {
         PerkFactory.Perk var3 = null;

         for(int var4 = 0; var4 < PerkFactory.PerkList.size(); ++var4) {
            PerkFactory.Perk var5 = (PerkFactory.Perk)PerkFactory.PerkList.get(var4);
            if (var5.getType() == var1) {
               var3 = var5;
               break;
            }
         }

         if (var3 != null) {
            this.XPMap.put(var1, var3.getTotalXpForLevel(var2));
         }

      }
   }

   public static class Location {
      public int x;
      public int y;
      public int z;

      public Location() {
      }

      public Location(int var1, int var2, int var3) {
         this.x = var1;
         this.y = var2;
         this.z = var3;
      }

      public Location set(int var1, int var2, int var3) {
         this.x = var1;
         this.y = var2;
         this.z = var3;
         return this;
      }

      public int getX() {
         return this.x;
      }

      public int getY() {
         return this.y;
      }

      public int getZ() {
         return this.z;
      }

      public boolean equals(int var1, int var2, int var3) {
         return this.x == var1 && this.y == var2 && this.z == var3;
      }

      public boolean equals(Object var1) {
         if (!(var1 instanceof Location var2)) {
            return false;
         } else {
            return this.x == var2.x && this.y == var2.y && this.z == var2.z;
         }
      }
   }

   private static final class Cheats {
      boolean m_godMod = false;
      boolean m_invulnerable = false;
      boolean m_invisible = false;
      boolean m_unlimitedEndurance = false;
      boolean m_unlimitedCarry = false;
      boolean m_buildCheat = false;
      boolean m_farmingCheat = false;
      boolean m_fishingCheat = false;
      boolean m_healthCheat = false;
      boolean m_mechanicsCheat = false;
      boolean m_fastMoveCheat = false;
      boolean m_movablesCheat = false;
      boolean m_timedActionInstantCheat = false;
      boolean m_canUseBrushTool = false;

      private Cheats() {
      }
   }

   public static class LightInfo {
      public IsoGridSquare square;
      public float x;
      public float y;
      public float z;
      public float angleX;
      public float angleY;
      public ArrayList<TorchInfo> torches = new ArrayList();
      public long time;
      public float night;
      public float rmod;
      public float gmod;
      public float bmod;

      public LightInfo() {
      }

      public void initFrom(LightInfo var1) {
         this.square = var1.square;
         this.x = var1.x;
         this.y = var1.y;
         this.z = var1.z;
         this.angleX = var1.angleX;
         this.angleY = var1.angleY;
         this.torches.clear();
         this.torches.addAll(var1.torches);
         this.time = (long)((double)System.nanoTime() / 1000000.0);
         this.night = var1.night;
         this.rmod = var1.rmod;
         this.gmod = var1.gmod;
         this.bmod = var1.bmod;
      }
   }

   private static class Recoil {
      public float m_recoilVarX = 0.0F;
      public float m_recoilVarY = 0.0F;

      private Recoil() {
      }
   }

   private static final class L_getDotWithForwardDirection {
      static final Vector2 v1 = new Vector2();
      static final Vector2 v2 = new Vector2();

      private L_getDotWithForwardDirection() {
      }
   }

   public class PerkInfo {
      public int level = 0;
      public PerkFactory.Perk perk;

      public PerkInfo() {
      }

      public int getLevel() {
         return this.level;
      }
   }

   private static class ReadBook {
      String fullType;
      int alreadyReadPages;

      private ReadBook() {
      }
   }

   private static final class L_renderShadow {
      static final ShadowParams shadowParams = new ShadowParams(1.0F, 1.0F, 1.0F);
      static final Vector2 vector2_1 = new Vector2();
      static final Vector2 vector2_2 = new Vector2();
      static final Vector3f forward = new Vector3f();
      static final Vector3 vector3 = new Vector3();
      static final Vector3f vector3f = new Vector3f();

      private L_renderShadow() {
      }
   }

   private static final class L_renderLast {
      static final Color color = new Color();

      private L_renderLast() {
      }
   }

   protected static final class l_testDotSide {
      static final Vector2 v1 = new Vector2();
      static final Vector2 v2 = new Vector2();
      static final Vector2 v3 = new Vector2();

      protected l_testDotSide() {
      }
   }

   public static class TorchInfo {
      private static final ObjectPool<TorchInfo> TorchInfoPool = new ObjectPool(TorchInfo::new);
      private static final Vector3f tempVector3f = new Vector3f();
      public int id;
      public float x;
      public float y;
      public float z;
      public float angleX;
      public float angleY;
      public float dist;
      public float strength;
      public boolean bCone;
      public float dot;
      public int focusing;

      public TorchInfo() {
      }

      public static TorchInfo alloc() {
         return (TorchInfo)TorchInfoPool.alloc();
      }

      public static void release(TorchInfo var0) {
         TorchInfoPool.release((Object)var0);
      }

      public TorchInfo set(IsoPlayer var1, InventoryItem var2) {
         this.x = var1.getX();
         this.y = var1.getY();
         this.z = var1.getZ();
         Vector2 var3 = var1.getLookVector(IsoGameCharacter.tempVector2);
         this.angleX = var3.x;
         this.angleY = var3.y;
         this.dist = (float)var2.getLightDistance();
         this.strength = var2.getLightStrength();
         this.bCone = var2.isTorchCone();
         this.dot = var2.getTorchDot();
         this.focusing = 0;
         return this;
      }

      public TorchInfo set(VehiclePart var1) {
         BaseVehicle var2 = var1.getVehicle();
         VehicleLight var3 = var1.getLight();
         VehicleScript var4 = var2.getScript();
         Vector3f var5 = tempVector3f;
         var5.set(var3.offset.x * var4.getExtents().x / 2.0F, 0.0F, var3.offset.y * var4.getExtents().z / 2.0F);
         var2.getWorldPos(var5, var5);
         this.x = var5.x;
         this.y = var5.y;
         this.z = var5.z;
         var5 = var2.getForwardVector(var5);
         this.angleX = var5.x;
         this.angleY = var5.z;
         this.dist = var1.getLightDistance();
         this.strength = var1.getLightIntensity();
         this.bCone = true;
         this.dot = var3.dot;
         this.focusing = (int)var1.getLightFocusing();
         return this;
      }
   }

   private static class L_postUpdate {
      static final MoveDeltaModifiers moveDeltas = new MoveDeltaModifiers();

      private L_postUpdate() {
      }
   }

   private static final class L_actionStateChanged {
      static final ArrayList<String> stateNames = new ArrayList();
      static final ArrayList<State> states = new ArrayList();

      private L_actionStateChanged() {
      }
   }

   private static final class Bandages {
      final HashMap<String, String> bandageTypeMap = new HashMap();
      final THashMap<String, InventoryItem> itemMap = new THashMap();

      private Bandages() {
      }

      String getBloodBandageType(String var1) {
         String var2 = (String)this.bandageTypeMap.get(var1);
         if (var2 == null) {
            this.bandageTypeMap.put(var1, var2 = var1 + "_Blood");
         }

         return var2;
      }

      void update(IsoGameCharacter var1) {
         if (!GameServer.bServer) {
            BodyDamage var2 = var1.getBodyDamage();
            WornItems var3 = var1.getWornItems();
            if (var2 != null && var3 != null) {
               assert !(var1 instanceof IsoZombie);

               this.itemMap.clear();

               int var4;
               for(var4 = 0; var4 < var3.size(); ++var4) {
                  InventoryItem var5 = var3.getItemByIndex(var4);
                  if (var5 != null) {
                     this.itemMap.put(var5.getFullType(), var5);
                  }
               }

               for(var4 = 0; var4 < BodyPartType.ToIndex(BodyPartType.MAX); ++var4) {
                  BodyPart var10 = var2.getBodyPart(BodyPartType.FromIndex(var4));
                  BodyPartLast var6 = var2.getBodyPartsLastState(BodyPartType.FromIndex(var4));
                  String var7 = var10.getType().getBandageModel();
                  if (!StringUtils.isNullOrWhitespace(var7)) {
                     String var8 = this.getBloodBandageType(var7);
                     if (var10.bandaged() != var6.bandaged()) {
                        if (var10.bandaged()) {
                           if (var10.isBandageDirty()) {
                              this.removeBandageModel(var1, var7);
                              this.addBandageModel(var1, var8);
                           } else {
                              this.removeBandageModel(var1, var8);
                              this.addBandageModel(var1, var7);
                           }
                        } else {
                           this.removeBandageModel(var1, var7);
                           this.removeBandageModel(var1, var8);
                        }
                     }

                     String var9;
                     if (var10.bitten() != var6.bitten()) {
                        if (var10.bitten()) {
                           var9 = var10.getType().getBiteWoundModel(var1.isFemale());
                           if (StringUtils.isNullOrWhitespace(var9)) {
                              continue;
                           }

                           this.addBandageModel(var1, var9);
                        } else {
                           this.removeBandageModel(var1, var10.getType().getBiteWoundModel(var1.isFemale()));
                        }
                     }

                     if (var10.scratched() != var6.scratched()) {
                        if (var10.scratched()) {
                           var9 = var10.getType().getScratchWoundModel(var1.isFemale());
                           if (StringUtils.isNullOrWhitespace(var9)) {
                              continue;
                           }

                           this.addBandageModel(var1, var9);
                        } else {
                           this.removeBandageModel(var1, var10.getType().getScratchWoundModel(var1.isFemale()));
                        }
                     }

                     if (var10.isCut() != var6.isCut()) {
                        if (var10.isCut()) {
                           var9 = var10.getType().getCutWoundModel(var1.isFemale());
                           if (!StringUtils.isNullOrWhitespace(var9)) {
                              this.addBandageModel(var1, var9);
                           }
                        } else {
                           this.removeBandageModel(var1, var10.getType().getCutWoundModel(var1.isFemale()));
                        }
                     }
                  }
               }

            }
         }
      }

      protected void addBandageModel(IsoGameCharacter var1, String var2) {
         if (!this.itemMap.containsKey(var2)) {
            InventoryItem var3 = InventoryItemFactory.CreateItem(var2);
            if (var3 instanceof Clothing) {
               Clothing var4 = (Clothing)var3;
               var1.getInventory().addItem(var4);
               var1.setWornItem(var4.getBodyLocation(), var4);
               var1.resetModelNextFrame();
            }
         }
      }

      protected void removeBandageModel(IsoGameCharacter var1, String var2) {
         InventoryItem var3 = (InventoryItem)this.itemMap.get(var2);
         if (var3 != null) {
            var1.getWornItems().remove(var3);
            var1.getInventory().Remove(var3);
            var1.resetModelNextFrame();
            var1.onWornItemsChanged();
         }
      }
   }

   public static class XPMultiplier {
      public float multiplier;
      public int minLevel;
      public int maxLevel;

      public XPMultiplier() {
      }
   }

   public static enum BodyLocation {
      Head,
      Leg,
      Arm,
      Chest,
      Stomach,
      Foot,
      Hand;

      private BodyLocation() {
      }
   }
}
