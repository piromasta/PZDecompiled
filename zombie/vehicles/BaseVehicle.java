package zombie.vehicles;

import fmod.fmod.FMODManager;
import fmod.fmod.FMODSoundEmitter;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import fmod.fmod.IFMODParameterUpdater;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import zombie.AmbientStreamManager;
import zombie.CombatManager;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.SystemDisabler;
import zombie.WorldSoundManager;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.ai.states.StaggerBackState;
import zombie.ai.states.ZombieFallDownState;
import zombie.ai.states.animals.AnimalFalldownState;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.DummySoundEmitter;
import zombie.audio.FMODParameter;
import zombie.audio.FMODParameterList;
import zombie.audio.GameSoundClip;
import zombie.audio.parameters.ParameterVehicleBrake;
import zombie.audio.parameters.ParameterVehicleEngineCondition;
import zombie.audio.parameters.ParameterVehicleGear;
import zombie.audio.parameters.ParameterVehicleHitLocation;
import zombie.audio.parameters.ParameterVehicleLoad;
import zombie.audio.parameters.ParameterVehicleRPM;
import zombie.audio.parameters.ParameterVehicleRoadMaterial;
import zombie.audio.parameters.ParameterVehicleSkid;
import zombie.audio.parameters.ParameterVehicleSpeed;
import zombie.audio.parameters.ParameterVehicleSteer;
import zombie.audio.parameters.ParameterVehicleTireMissing;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.physics.BallisticsController;
import zombie.core.physics.Bullet;
import zombie.core.physics.CarController;
import zombie.core.physics.Transform;
import zombie.core.physics.WorldSimulation;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.Vector3;
import zombie.core.skinnedmodel.animation.AnimationMultiTrack;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.model.VehicleModelInstance;
import zombie.core.skinnedmodel.model.VehicleSubModelInstance;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureID;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.input.GameKeyboard;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Key;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.SafeHouse;
import zombie.iso.fboRenderChunk.FBORenderShadows;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.RainManager;
import zombie.iso.objects.RenderEffectType;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.weather.ClimateManager;
import zombie.iso.zones.VehicleZone;
import zombie.network.ClientServerMap;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.PassengerMap;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.VehiclePoly;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.popman.ObjectPool;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.popman.animal.AnimalSynchronizationManager;
import zombie.radio.ZomboidRadio;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.VehiclePartModel;
import zombie.scripting.objects.VehicleScript;
import zombie.ui.TextManager;
import zombie.ui.UIManager;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;

public final class BaseVehicle extends IsoMovingObject implements Thumpable, IFMODParameterUpdater {
   public static final float RADIUS = 0.3F;
   public static final int FADE_DISTANCE = 15;
   public static final int RANDOMIZE_CONTAINER_CHANCE = 100;
   public static final byte noAuthorization = -1;
   private static final Vector3f _UNIT_Y = new Vector3f(0.0F, 1.0F, 0.0F);
   private static final VehiclePoly tempPoly = new VehiclePoly();
   public static final boolean YURI_FORCE_FIELD = false;
   public static boolean RENDER_TO_TEXTURE = false;
   public static float CENTER_OF_MASS_MAGIC = 0.7F;
   private static final float[] wheelParams = new float[24];
   private static final float[] physicsParams = new float[27];
   static final byte POSITION_ORIENTATION_PACKET_SIZE = 102;
   public static Texture vehicleShadow = null;
   public int justBreakConstraintTimer = 0;
   public BaseVehicle wasTowedBy = null;
   protected static final ColorInfo inf = new ColorInfo();
   private static final float[] lowRiderParam = new float[4];
   private final VehicleImpulse impulseFromServer = new VehicleImpulse();
   private final VehicleImpulse[] impulseFromSquishedZombie = new VehicleImpulse[4];
   private final ArrayList<VehicleImpulse> impulseFromHitZombie = new ArrayList();
   private final int netPlayerTimeoutMax = 30;
   public final ArrayList<ModelInfo> models = new ArrayList();
   public IsoChunk chunk;
   public boolean polyDirty = true;
   private boolean polyGarageCheck = true;
   private float radiusReductionInGarage = 0.0F;
   public short VehicleID = -1;
   public int sqlID = -1;
   public boolean serverRemovedFromWorld = false;
   public VehicleInterpolation interpolation = null;
   public boolean waitFullUpdate;
   public float throttle = 0.0F;
   public double engineSpeed;
   public TransmissionNumber transmissionNumber;
   public final UpdateLimit transmissionChangeTime = new UpdateLimit(1000L);
   public boolean hasExtendOffset = true;
   public boolean hasExtendOffsetExiting = false;
   public float savedPhysicsZ = 0.0F / 0.0F;
   public final Quaternionf savedRot = new Quaternionf();
   public final Transform jniTransform = new Transform();
   public float jniSpeed;
   public boolean jniIsCollide;
   public final Vector3f jniLinearVelocity = new Vector3f();
   private final Vector3f lastLinearVelocity = new Vector3f();
   public Authorization netPlayerAuthorization;
   public short netPlayerId;
   public int netPlayerTimeout;
   public int authSimulationHash;
   public long authSimulationTime;
   public int frontEndDurability;
   public int rearEndDurability;
   public float rust;
   public float colorHue;
   public float colorSaturation;
   public float colorValue;
   public int currentFrontEndDurability;
   public int currentRearEndDurability;
   public float collideX;
   public float collideY;
   public final VehiclePoly shadowCoord;
   public engineStateTypes engineState;
   public long engineLastUpdateStateTime;
   public static final int MAX_WHEELS = 4;
   public static final int PHYSICS_PARAM_COUNT = 27;
   public final WheelInfo[] wheelInfo;
   public boolean skidding;
   public long skidSound;
   public long ramSound;
   public long ramSoundTime;
   private VehicleEngineRPM vehicleEngineRPM;
   public final long[] new_EngineSoundId;
   private long combinedEngineSound;
   public int engineSoundIndex;
   public BaseSoundEmitter hornemitter;
   public float startTime;
   public boolean headlightsOn;
   public boolean stoplightsOn;
   public boolean windowLightsOn;
   public boolean soundHornOn;
   public boolean soundBackMoveOn;
   public boolean previouslyEntered;
   public boolean previouslyMoved;
   public final LightbarLightsMode lightbarLightsMode;
   public final LightbarSirenMode lightbarSirenMode;
   private final IsoLightSource leftLight1;
   private final IsoLightSource leftLight2;
   private final IsoLightSource rightLight1;
   private final IsoLightSource rightLight2;
   private int leftLightIndex;
   private int rightLightIndex;
   public final ServerVehicleState[] connectionState;
   protected Passenger[] passengers;
   protected String scriptName;
   protected VehicleScript script;
   protected final ArrayList<VehiclePart> parts;
   protected VehiclePart battery;
   protected int engineQuality;
   protected int engineLoudness;
   protected int enginePower;
   protected long engineCheckTime;
   protected final ArrayList<VehiclePart> lights;
   protected boolean createdModel;
   protected int skinIndex;
   protected CarController physics;
   protected boolean bCreated;
   protected final VehiclePoly poly;
   protected final VehiclePoly polyPlusRadius;
   protected boolean bDoDamageOverlay;
   protected boolean loaded;
   protected short updateFlags;
   protected long updateLockTimeout;
   final UpdateLimit limitPhysicSend;
   Vector2 limitPhysicPositionSent;
   final UpdateLimit limitPhysicValid;
   private final UpdateLimit limitCrash;
   public boolean addedToWorld;
   boolean removedFromWorld;
   private float polyPlusRadiusMinX;
   private float polyPlusRadiusMinY;
   private float polyPlusRadiusMaxX;
   private float polyPlusRadiusMaxY;
   private float maxSpeed;
   private boolean keyIsOnDoor;
   private boolean hotwired;
   private boolean hotwiredBroken;
   private boolean keysInIgnition;
   private long soundHorn;
   private long soundScrapePastPlant;
   private long soundBackMoveSignal;
   public long soundSirenSignal;
   public long doorAlarmSound;
   private final HashMap<String, String> choosenParts;
   private String type;
   private String respawnZone;
   private float mass;
   private float initialMass;
   private float brakingForce;
   private float baseQuality;
   private float currentSteering;
   private boolean isBraking;
   private int mechanicalID;
   private boolean needPartsUpdate;
   private boolean alarmed;
   private int alarmTime;
   private float alarmAccumulator;
   private double sirenStartTime;
   private boolean mechanicUIOpen;
   private boolean isGoodCar;
   private InventoryItem currentKey;
   private boolean doColor;
   private float brekingSlowFactor;
   private final ArrayList<IsoObject> brekingObjectsList;
   private final UpdateLimit limitUpdate;
   public byte keySpawned;
   public final Matrix4f vehicleTransform;
   public final Matrix4f renderTransform;
   private BaseSoundEmitter emitter;
   private float brakeBetweenUpdatesSpeed;
   public long physicActiveCheck;
   private long constraintChangedTime;
   private AnimationPlayer m_animPlayer;
   public String specificDistributionId;
   private boolean bAddThumpWorldSound;
   private final SurroundVehicle m_surroundVehicle;
   private boolean regulator;
   private float regulatorSpeed;
   private static final HashMap<String, Integer> s_PartToMaskMap = new HashMap();
   private static final Byte BYTE_ZERO = 0;
   private final HashMap<String, Byte> bloodIntensity;
   private boolean OptionBloodDecals;
   private long createPhysicsTime;
   private BaseVehicle vehicleTowing;
   private BaseVehicle vehicleTowedBy;
   public int constraintTowing;
   private int vehicleTowingID;
   private int vehicleTowedByID;
   private String towAttachmentSelf;
   private String towAttachmentOther;
   private float towConstraintZOffset;
   private final ParameterVehicleBrake parameterVehicleBrake;
   private final ParameterVehicleEngineCondition parameterVehicleEngineCondition;
   private final ParameterVehicleGear parameterVehicleGear;
   private final ParameterVehicleLoad parameterVehicleLoad;
   private final ParameterVehicleRoadMaterial parameterVehicleRoadMaterial;
   private final ParameterVehicleRPM parameterVehicleRPM;
   private final ParameterVehicleSkid parameterVehicleSkid;
   private final ParameterVehicleSpeed parameterVehicleSpeed;
   private final ParameterVehicleSteer parameterVehicleSteer;
   private final ParameterVehicleTireMissing parameterVehicleTireMissing;
   private final FMODParameterList fmodParameters;
   public boolean isActive;
   public boolean isStatic;
   private final UpdateLimit physicReliableLimit;
   public boolean isReliable;
   public ArrayList<IsoAnimal> animals;
   private float totalAnimalSize;
   private float keySpawnChancedD100;
   public float timeSinceLastAuth;
   public static final ThreadLocal<TransformPool> TL_transform_pool = ThreadLocal.withInitial(TransformPool::new);
   public static final ThreadLocal<Vector2ObjectPool> TL_vector2_pool = ThreadLocal.withInitial(Vector2ObjectPool::new);
   public static final ThreadLocal<Vector2fObjectPool> TL_vector2f_pool = ThreadLocal.withInitial(Vector2fObjectPool::new);
   public static final ThreadLocal<Vector3fObjectPool> TL_vector3f_pool = ThreadLocal.withInitial(Vector3fObjectPool::new);
   public static final ThreadLocal<Vector4fObjectPool> TL_vector4f_pool = ThreadLocal.withInitial(Vector4fObjectPool::new);
   public static final ThreadLocal<Matrix4fObjectPool> TL_matrix4f_pool = ThreadLocal.withInitial(Matrix4fObjectPool::new);
   public static final ThreadLocal<QuaternionfObjectPool> TL_quaternionf_pool = ThreadLocal.withInitial(QuaternionfObjectPool::new);
   private int createPhysicsRecursion;
   private final UpdateLimit updateAnimal;
   public static final float PHYSICS_Z_SCALE = 0.8164967F;
   public static float PLUS_RADIUS = 0.15F;
   long[] lastImpulseMilli;
   private int zombiesHits;
   private long zombieHitTimestamp;
   public static final int MASK1_FRONT = 0;
   public static final int MASK1_REAR = 4;
   public static final int MASK1_DOOR_RIGHT_FRONT = 8;
   public static final int MASK1_DOOR_RIGHT_REAR = 12;
   public static final int MASK1_DOOR_LEFT_FRONT = 1;
   public static final int MASK1_DOOR_LEFT_REAR = 5;
   public static final int MASK1_WINDOW_RIGHT_FRONT = 9;
   public static final int MASK1_WINDOW_RIGHT_REAR = 13;
   public static final int MASK1_WINDOW_LEFT_FRONT = 2;
   public static final int MASK1_WINDOW_LEFT_REAR = 6;
   public static final int MASK1_WINDOW_FRONT = 10;
   public static final int MASK1_WINDOW_REAR = 14;
   public static final int MASK1_GUARD_RIGHT_FRONT = 3;
   public static final int MASK1_GUARD_RIGHT_REAR = 7;
   public static final int MASK1_GUARD_LEFT_FRONT = 11;
   public static final int MASK1_GUARD_LEFT_REAR = 15;
   public static final int MASK2_ROOF = 0;
   public static final int MASK2_LIGHT_RIGHT_FRONT = 4;
   public static final int MASK2_LIGHT_LEFT_FRONT = 8;
   public static final int MASK2_LIGHT_RIGHT_REAR = 12;
   public static final int MASK2_LIGHT_LEFT_REAR = 1;
   public static final int MASK2_BRAKE_RIGHT = 5;
   public static final int MASK2_BRAKE_LEFT = 9;
   public static final int MASK2_LIGHTBAR_RIGHT = 13;
   public static final int MASK2_LIGHTBAR_LEFT = 2;
   public static final int MASK2_HOOD = 6;
   public static final int MASK2_BOOT = 10;
   public float forcedFriction;
   protected final HitVars hitVars;

   public int getSqlId() {
      return this.sqlID;
   }

   public static Matrix4f allocMatrix4f() {
      return (Matrix4f)((Matrix4fObjectPool)TL_matrix4f_pool.get()).alloc();
   }

   public static void releaseMatrix4f(Matrix4f var0) {
      ((Matrix4fObjectPool)TL_matrix4f_pool.get()).release(var0);
   }

   public static Quaternionf allocQuaternionf() {
      return (Quaternionf)((QuaternionfObjectPool)TL_quaternionf_pool.get()).alloc();
   }

   public static void releaseQuaternionf(Quaternionf var0) {
      ((QuaternionfObjectPool)TL_quaternionf_pool.get()).release(var0);
   }

   public static Transform allocTransform() {
      return (Transform)((TransformPool)TL_transform_pool.get()).alloc();
   }

   public static void releaseTransform(Transform var0) {
      ((TransformPool)TL_transform_pool.get()).release(var0);
   }

   public static Vector2 allocVector2() {
      return (Vector2)((Vector2ObjectPool)TL_vector2_pool.get()).alloc();
   }

   public static void releaseVector2(Vector2 var0) {
      ((Vector2ObjectPool)TL_vector2_pool.get()).release(var0);
   }

   public static Vector2f allocVector2f() {
      return (Vector2f)((Vector2fObjectPool)TL_vector2f_pool.get()).alloc();
   }

   public static void releaseVector2f(Vector2f var0) {
      ((Vector2fObjectPool)TL_vector2f_pool.get()).release(var0);
   }

   public static Vector3f allocVector3f() {
      return (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
   }

   public static void releaseVector4f(Vector4f var0) {
      ((Vector4fObjectPool)TL_vector4f_pool.get()).release(var0);
   }

   public static Vector4f allocVector4f() {
      return (Vector4f)((Vector4fObjectPool)TL_vector4f_pool.get()).alloc();
   }

   public static void releaseVector3f(Vector3f var0) {
      ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var0);
   }

   public BaseVehicle(IsoCell var1) {
      super(var1, false);
      this.netPlayerAuthorization = BaseVehicle.Authorization.Server;
      this.netPlayerId = -1;
      this.netPlayerTimeout = 0;
      this.authSimulationHash = 0;
      this.authSimulationTime = 0L;
      this.frontEndDurability = 100;
      this.rearEndDurability = 100;
      this.rust = 0.0F;
      this.colorHue = 0.0F;
      this.colorSaturation = 0.0F;
      this.colorValue = 0.0F;
      this.currentFrontEndDurability = 100;
      this.currentRearEndDurability = 100;
      this.collideX = -1.0F;
      this.collideY = -1.0F;
      this.shadowCoord = new VehiclePoly();
      this.engineState = BaseVehicle.engineStateTypes.Idle;
      this.wheelInfo = new WheelInfo[4];
      this.skidding = false;
      this.vehicleEngineRPM = null;
      this.new_EngineSoundId = new long[8];
      this.combinedEngineSound = 0L;
      this.engineSoundIndex = 0;
      this.hornemitter = null;
      this.startTime = 0.0F;
      this.headlightsOn = false;
      this.stoplightsOn = false;
      this.windowLightsOn = false;
      this.soundHornOn = false;
      this.soundBackMoveOn = false;
      this.previouslyEntered = false;
      this.previouslyMoved = false;
      this.lightbarLightsMode = new LightbarLightsMode();
      this.lightbarSirenMode = new LightbarSirenMode();
      this.leftLight1 = new IsoLightSource(0, 0, 0, 0.0F, 0.0F, 1.0F, 8);
      this.leftLight2 = new IsoLightSource(0, 0, 0, 0.0F, 0.0F, 1.0F, 8);
      this.rightLight1 = new IsoLightSource(0, 0, 0, 1.0F, 0.0F, 0.0F, 8);
      this.rightLight2 = new IsoLightSource(0, 0, 0, 1.0F, 0.0F, 0.0F, 8);
      this.leftLightIndex = -1;
      this.rightLightIndex = -1;
      this.connectionState = new ServerVehicleState[512];
      this.passengers = new Passenger[1];
      this.parts = new ArrayList();
      this.lights = new ArrayList();
      this.createdModel = false;
      this.skinIndex = -1;
      this.poly = new VehiclePoly();
      this.polyPlusRadius = new VehiclePoly();
      this.bDoDamageOverlay = false;
      this.loaded = false;
      this.updateLockTimeout = 0L;
      this.limitPhysicSend = new UpdateLimit(300L);
      this.limitPhysicPositionSent = null;
      this.limitPhysicValid = new UpdateLimit(1000L);
      this.limitCrash = new UpdateLimit(600L);
      this.addedToWorld = false;
      this.removedFromWorld = false;
      this.polyPlusRadiusMinX = -123.0F;
      this.keyIsOnDoor = false;
      this.hotwired = false;
      this.hotwiredBroken = false;
      this.keysInIgnition = false;
      this.soundHorn = -1L;
      this.soundScrapePastPlant = -1L;
      this.soundBackMoveSignal = -1L;
      this.soundSirenSignal = -1L;
      this.doorAlarmSound = 0L;
      this.choosenParts = new HashMap();
      this.type = "";
      this.mass = 0.0F;
      this.initialMass = 0.0F;
      this.brakingForce = 0.0F;
      this.baseQuality = 0.0F;
      this.currentSteering = 0.0F;
      this.isBraking = false;
      this.mechanicalID = 0;
      this.needPartsUpdate = false;
      this.alarmed = false;
      this.alarmTime = -1;
      this.sirenStartTime = 0.0;
      this.mechanicUIOpen = false;
      this.isGoodCar = false;
      this.currentKey = null;
      this.doColor = true;
      this.brekingSlowFactor = 0.0F;
      this.brekingObjectsList = new ArrayList();
      this.limitUpdate = new UpdateLimit(333L);
      this.keySpawned = 0;
      this.vehicleTransform = new Matrix4f();
      this.renderTransform = new Matrix4f();
      this.brakeBetweenUpdatesSpeed = 0.0F;
      this.physicActiveCheck = -1L;
      this.constraintChangedTime = -1L;
      this.m_animPlayer = null;
      this.specificDistributionId = null;
      this.bAddThumpWorldSound = false;
      this.m_surroundVehicle = new SurroundVehicle(this);
      this.regulator = false;
      this.regulatorSpeed = 0.0F;
      this.bloodIntensity = new HashMap();
      this.OptionBloodDecals = false;
      this.createPhysicsTime = -1L;
      this.vehicleTowing = null;
      this.vehicleTowedBy = null;
      this.constraintTowing = -1;
      this.vehicleTowingID = -1;
      this.vehicleTowedByID = -1;
      this.towAttachmentSelf = null;
      this.towAttachmentOther = null;
      this.towConstraintZOffset = 0.0F;
      this.parameterVehicleBrake = new ParameterVehicleBrake(this);
      this.parameterVehicleEngineCondition = new ParameterVehicleEngineCondition(this);
      this.parameterVehicleGear = new ParameterVehicleGear(this);
      this.parameterVehicleLoad = new ParameterVehicleLoad(this);
      this.parameterVehicleRoadMaterial = new ParameterVehicleRoadMaterial(this);
      this.parameterVehicleRPM = new ParameterVehicleRPM(this);
      this.parameterVehicleSkid = new ParameterVehicleSkid(this);
      this.parameterVehicleSpeed = new ParameterVehicleSpeed(this);
      this.parameterVehicleSteer = new ParameterVehicleSteer(this);
      this.parameterVehicleTireMissing = new ParameterVehicleTireMissing(this);
      this.fmodParameters = new FMODParameterList();
      this.isActive = false;
      this.isStatic = false;
      this.physicReliableLimit = new UpdateLimit(500L);
      this.isReliable = false;
      this.animals = new ArrayList();
      this.totalAnimalSize = 0.0F;
      this.keySpawnChancedD100 = (float)SandboxOptions.getInstance().KeyLootNew.getValue() * 25.0F;
      this.timeSinceLastAuth = 10.0F;
      this.createPhysicsRecursion = 0;
      this.updateAnimal = new UpdateLimit(2100L);
      this.lastImpulseMilli = new long[4];
      this.zombiesHits = 0;
      this.zombieHitTimestamp = 0L;
      this.forcedFriction = -1.0F;
      this.hitVars = new HitVars();
      this.setCollidable(false);
      this.respawnZone = new String("");
      this.scriptName = "Base.PickUpTruck";
      this.passengers[0] = new Passenger();
      this.waitFullUpdate = false;
      this.savedRot.w = 1.0F;

      int var2;
      for(var2 = 0; var2 < this.wheelInfo.length; ++var2) {
         this.wheelInfo[var2] = new WheelInfo();
      }

      if (GameClient.bClient) {
         this.interpolation = new VehicleInterpolation();
      }

      this.setKeyId(Rand.Next(100000000));
      this.engineSpeed = 0.0;
      this.transmissionNumber = TransmissionNumber.N;
      this.rust = (float)Rand.Next(0, 2);
      this.jniIsCollide = false;

      for(var2 = 0; var2 < 4; ++var2) {
         lowRiderParam[var2] = 0.0F;
      }

      this.fmodParameters.add(this.parameterVehicleBrake);
      this.fmodParameters.add(this.parameterVehicleEngineCondition);
      this.fmodParameters.add(this.parameterVehicleGear);
      this.fmodParameters.add(this.parameterVehicleLoad);
      this.fmodParameters.add(this.parameterVehicleRPM);
      this.fmodParameters.add(this.parameterVehicleRoadMaterial);
      this.fmodParameters.add(this.parameterVehicleSkid);
      this.fmodParameters.add(this.parameterVehicleSpeed);
      this.fmodParameters.add(this.parameterVehicleSteer);
      this.fmodParameters.add(this.parameterVehicleTireMissing);
   }

   public static void LoadAllVehicleTextures() {
      DebugLog.Vehicle.println("BaseVehicle.LoadAllVehicleTextures...");
      ArrayList var0 = ScriptManager.instance.getAllVehicleScripts();
      Iterator var1 = var0.iterator();

      while(var1.hasNext()) {
         VehicleScript var2 = (VehicleScript)var1.next();
         LoadVehicleTextures(var2);
      }

   }

   public static void LoadVehicleTextures(VehicleScript var0) {
      if (SystemDisabler.doVehiclesWithoutTextures) {
         VehicleScript.Skin var1 = var0.getSkin(0);
         var1.textureData = LoadVehicleTexture(var1.texture);
         var1.textureDataMask = LoadVehicleTexture("vehicles_placeholder_mask");
         var1.textureDataDamage1Overlay = LoadVehicleTexture("vehicles_placeholder_damage1overlay");
         var1.textureDataDamage1Shell = LoadVehicleTexture("vehicles_placeholder_damage1shell");
         var1.textureDataDamage2Overlay = LoadVehicleTexture("vehicles_placeholder_damage2overlay");
         var1.textureDataDamage2Shell = LoadVehicleTexture("vehicles_placeholder_damage2shell");
         var1.textureDataLights = LoadVehicleTexture("vehicles_placeholder_lights");
         var1.textureDataRust = LoadVehicleTexture("vehicles_placeholder_rust");
      } else {
         for(int var3 = 0; var3 < var0.getSkinCount(); ++var3) {
            VehicleScript.Skin var2 = var0.getSkin(var3);
            var2.copyMissingFrom(var0.getTextures());
            LoadVehicleTextures(var2);
         }
      }

   }

   private static void LoadVehicleTextures(VehicleScript.Skin var0) {
      var0.textureData = LoadVehicleTexture(var0.texture);
      if (var0.textureMask != null) {
         int var1 = 0;
         var1 |= 256;
         var0.textureDataMask = LoadVehicleTexture(var0.textureMask, var1);
      }

      var0.textureDataDamage1Overlay = LoadVehicleTexture(var0.textureDamage1Overlay);
      var0.textureDataDamage1Shell = LoadVehicleTexture(var0.textureDamage1Shell);
      var0.textureDataDamage2Overlay = LoadVehicleTexture(var0.textureDamage2Overlay);
      var0.textureDataDamage2Shell = LoadVehicleTexture(var0.textureDamage2Shell);
      var0.textureDataLights = LoadVehicleTexture(var0.textureLights);
      var0.textureDataRust = LoadVehicleTexture(var0.textureRust);
      var0.textureDataShadow = LoadVehicleTexture(var0.textureShadow);
   }

   public static Texture LoadVehicleTexture(String var0) {
      int var1 = 0;
      var1 |= TextureID.bUseCompression ? 4 : 0;
      var1 |= 256;
      return LoadVehicleTexture(var0, var1);
   }

   public static Texture LoadVehicleTexture(String var0, int var1) {
      return StringUtils.isNullOrWhitespace(var0) ? null : Texture.getSharedTexture("media/textures/" + var0 + ".png", var1);
   }

   public void setNetPlayerAuthorization(Authorization var1, int var2) {
      this.netPlayerAuthorization = var1;
      this.timeSinceLastAuth = 10.0F;
      this.netPlayerId = (short)var2;
      this.netPlayerTimeout = var2 == -1 ? 0 : 30;
      if (GameClient.bClient) {
         boolean var3 = BaseVehicle.Authorization.Local.equals(var1) || BaseVehicle.Authorization.LocalCollide.equals(var1);
         if (this.getVehicleTowing() != null) {
            Bullet.setVehicleStatic(this, !var3);
            Bullet.setVehicleActive(this, var3);
            Bullet.setVehicleStatic(this.getVehicleTowing(), !var3);
            Bullet.setVehicleActive(this.getVehicleTowing(), var3);
         } else if (this.getVehicleTowedBy() != null) {
            Bullet.setVehicleStatic(this, !var3);
            Bullet.setVehicleActive(this, var3);
         } else {
            Bullet.setVehicleStatic(this, !var3);
            Bullet.setVehicleActive(this, var3);
         }
      }

      DebugLog.Vehicle.trace("vid%s=%d pid=%d %s", this.getVehicleTowing() != null ? "-a" : (this.getVehicleTowedBy() != null ? "-b" : ""), this.getId(), var2, var1.name());
   }

   public boolean isNetPlayerAuthorization(Authorization var1) {
      return this.netPlayerAuthorization.equals(var1);
   }

   public boolean isNetPlayerId(short var1) {
      return this.netPlayerId == var1;
   }

   public short getNetPlayerId() {
      return this.netPlayerId;
   }

   public String getAuthorizationDescription() {
      return String.format("vid:%s(%d) pid:(%d) auth=%s static=%b active=%b", this.scriptName, this.VehicleID, this.netPlayerId, this.netPlayerAuthorization.name(), this.isStatic, this.isActive);
   }

   public static float getFakeSpeedModifier() {
      if (!GameClient.bClient && !GameServer.bServer) {
         return 1.0F;
      } else {
         float var0 = (float)ServerOptions.instance.SpeedLimit.getValue();
         return 120.0F / Math.min(var0, 120.0F);
      }
   }

   public boolean isLocalPhysicSim() {
      if (GameServer.bServer) {
         return this.isNetPlayerAuthorization(BaseVehicle.Authorization.Server);
      } else {
         return this.isNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide) || this.isNetPlayerAuthorization(BaseVehicle.Authorization.Local);
      }
   }

   public void addImpulse(Vector3f var1, Vector3f var2) {
      if (!this.impulseFromServer.enable) {
         this.impulseFromServer.enable = true;
         this.impulseFromServer.impulse.set(var1);
         this.impulseFromServer.rel_pos.set(var2);
      } else if (this.impulseFromServer.impulse.length() < var1.length()) {
         this.impulseFromServer.impulse.set(var1);
         this.impulseFromServer.rel_pos.set(var2);
         this.impulseFromServer.enable = false;
         this.impulseFromServer.release();
      }

   }

   public double getEngineSpeed() {
      return this.engineSpeed;
   }

   public String getTransmissionNumberLetter() {
      return this.transmissionNumber.getString();
   }

   public int getTransmissionNumber() {
      return this.transmissionNumber.getIndex();
   }

   public void setClientForce(float var1) {
      this.physics.clientForce = var1;
   }

   public float getClientForce() {
      return this.physics.clientForce;
   }

   public float getForce() {
      return this.physics.EngineForce - this.physics.BrakingForce;
   }

   private void doVehicleColor() {
      if (!this.isDoColor()) {
         this.colorSaturation = 0.1F;
         this.colorValue = 0.9F;
      } else {
         this.colorHue = Rand.Next(0.0F, 0.0F);
         this.colorSaturation = 0.5F;
         this.colorValue = Rand.Next(0.3F, 0.6F);
         int var1 = Rand.Next(100);
         if (var1 < 20) {
            this.colorHue = Rand.Next(0.0F, 0.03F);
            this.colorSaturation = Rand.Next(0.85F, 1.0F);
            this.colorValue = Rand.Next(0.55F, 0.85F);
         } else if (var1 < 32) {
            this.colorHue = Rand.Next(0.55F, 0.61F);
            this.colorSaturation = Rand.Next(0.85F, 1.0F);
            this.colorValue = Rand.Next(0.65F, 0.75F);
         } else if (var1 < 67) {
            this.colorHue = 0.15F;
            this.colorSaturation = Rand.Next(0.0F, 0.1F);
            this.colorValue = Rand.Next(0.7F, 0.8F);
         } else if (var1 < 89) {
            this.colorHue = Rand.Next(0.0F, 1.0F);
            this.colorSaturation = Rand.Next(0.0F, 0.1F);
            this.colorValue = Rand.Next(0.1F, 0.25F);
         } else {
            this.colorHue = Rand.Next(0.0F, 1.0F);
            this.colorSaturation = Rand.Next(0.6F, 0.75F);
            this.colorValue = Rand.Next(0.3F, 0.7F);
         }

         if (this.getScript() != null) {
            if (this.getScript().getForcedHue() > -1.0F) {
               this.colorHue = this.getScript().getForcedHue();
            }

            if (this.getScript().getForcedSat() > -1.0F) {
               this.colorSaturation = this.getScript().getForcedSat();
            }

            if (this.getScript().getForcedVal() > -1.0F) {
               this.colorValue = this.getScript().getForcedVal();
            }
         }

      }
   }

   public String getObjectName() {
      return "Vehicle";
   }

   public boolean Serialize() {
      return true;
   }

   public void createPhysics() {
      this.createPhysics(false);
   }

   public void createPhysics(boolean var1) {
      if (!GameClient.bClient && this.VehicleID == -1) {
         this.VehicleID = VehicleIDMap.instance.allocateID();
         if (GameServer.bServer) {
            VehicleManager.instance.registerVehicle(this);
         } else {
            VehicleIDMap.instance.put(this.VehicleID, this);
         }
      }

      if (this.script == null) {
         this.setScript(this.scriptName);
      }

      label206: {
         try {
            ++this.createPhysicsRecursion;
            if (this.createPhysicsRecursion != 1) {
               break label206;
            }

            if (!var1) {
               LuaEventManager.triggerEvent("OnSpawnVehicleStart", this);
            }

            if (this.physics == null) {
               break label206;
            }
         } finally {
            --this.createPhysicsRecursion;
         }

         return;
      }

      if (this.script != null) {
         if (this.skinIndex == -1) {
            this.setSkinIndex(Rand.Next(this.getSkinCount()));
         }

         if (!GameServer.bServer) {
            WorldSimulation.instance.create();
         }

         this.jniTransform.origin.set(this.getX() - WorldSimulation.instance.offsetX, Float.isNaN(this.savedPhysicsZ) ? this.getZ() : this.savedPhysicsZ, this.getY() - WorldSimulation.instance.offsetY);
         this.physics = new CarController(this);
         this.savedPhysicsZ = 0.0F / 0.0F;
         this.createPhysicsTime = System.currentTimeMillis();
         int var2;
         if (!this.bCreated) {
            this.bCreated = true;
            var2 = 30;
            if (SandboxOptions.getInstance().RecentlySurvivorVehicles.getValue() == 1) {
               var2 = 0;
            }

            if (SandboxOptions.getInstance().RecentlySurvivorVehicles.getValue() == 2) {
               var2 = 10;
            }

            if (SandboxOptions.getInstance().RecentlySurvivorVehicles.getValue() == 3) {
               var2 = 30;
            }

            if (SandboxOptions.getInstance().RecentlySurvivorVehicles.getValue() == 4) {
               var2 = 50;
            }

            if (Rand.Next(100) < var2) {
               this.setGoodCar(true);
            }
         }

         this.createParts();
         this.initParts();
         if (!this.createdModel) {
            ModelManager.instance.addVehicle(this);
            this.createdModel = true;
         }

         this.updateTransform();
         this.lights.clear();

         for(var2 = 0; var2 < this.parts.size(); ++var2) {
            VehiclePart var3 = (VehiclePart)this.parts.get(var2);
            if (var3.getLight() != null) {
               this.lights.add(var3);
            }
         }

         this.setMaxSpeed(this.getScript().maxSpeed);
         this.setInitialMass(this.getScript().getMass());
         if (!this.getCell().getVehicles().contains(this) && !this.getCell().addVehicles.contains(this)) {
            this.getCell().addVehicles.add(this);
         }

         this.square = this.getCell().getGridSquare((double)this.getX(), (double)this.getY(), (double)this.getZ());
         if (!this.shouldNotHaveLoot()) {
            this.randomizeContainers();
         }

         if (this.engineState == BaseVehicle.engineStateTypes.Running) {
            this.engineDoRunning();
         }

         this.updateTotalMass();
         this.bDoDamageOverlay = true;
         this.updatePartStats();
         this.mechanicalID = Rand.Next(100000);
         LuaEventManager.triggerEvent("OnSpawnVehicleEnd", this);
      }
   }

   public boolean isPreviouslyEntered() {
      return this.previouslyEntered;
   }

   public void setPreviouslyEntered(boolean var1) {
      this.previouslyEntered = var1;
   }

   public boolean isPreviouslyMoved() {
      return this.previouslyMoved;
   }

   public void setPreviouslyMoved(boolean var1) {
      this.previouslyMoved = var1;
   }

   public int getKeyId() {
      return this.keyId;
   }

   public boolean getKeySpawned() {
      return this.keySpawned != 0;
   }

   public void putKeyToZombie(IsoZombie var1) {
      if (var1.shouldZombieHaveKey(true)) {
         if (this.checkZombieKeyForVehicle(var1)) {
            if ((float)Rand.Next(100) >= 1.0F * this.keySpawnChancedD100) {
               InventoryItem var2 = this.createVehicleKey();
               this.keySpawned = 1;
               this.keyNamerVehicle(var2);
               var1.getInventory().AddItem(var2);
            } else {
               String var13 = "Base.KeyRing";
               if (this.getScript().hasSpecialKeyRing() && (float)Rand.Next(100) < 1.0F * this.keySpawnChancedD100) {
                  var13 = this.getScript().getRandomSpecialKeyRing();
               }

               InventoryItem var3 = var1.getInventory().AddItem(var13);
               InventoryContainer var4 = (InventoryContainer)var3;
               if (var4 != null && var4.getInventory() != null) {
                  InventoryItem var5 = this.createVehicleKey();
                  this.keySpawned = 1;
                  var4.getInventory().AddItem(var5);
                  this.keyNamerVehicle(var5);
                  this.keyNamerVehicle(var4);
                  if ((float)Rand.Next(100) < 1.0F * this.keySpawnChancedD100) {
                     float var6 = this.getX();
                     float var7 = this.getY();
                     Vector2f var8 = new Vector2f();
                     BuildingDef var9 = AmbientStreamManager.getNearestBuilding(var6, var7, var8);
                     if (var9 != null && var9.getKeyId() != -1) {
                        String var10 = "Base.Key1";
                        InventoryItem var11 = InventoryItemFactory.CreateItem(var10);
                        var11.setKeyId(var9.getKeyId());
                        IsoGridSquare var12 = var9.getFreeSquareInRoom();
                        if (var12 != null) {
                           ItemPickerJava.KeyNamer.nameKey(var11, (IsoGridSquare)Objects.requireNonNull(var9.getFreeSquareInRoom()));
                        }

                        var4.getInventory().AddItem(var11);
                     }
                  }
               }
            }

         }
      }
   }

   public void putKeyToContainer(ItemContainer var1, IsoGridSquare var2, IsoObject var3) {
      if ((float)Rand.Next(100) >= 1.0F * this.keySpawnChancedD100) {
         InventoryItem var4 = this.createVehicleKey();
         this.keySpawned = 1;
         var1.AddItem(var4);
         this.keyNamerVehicle(var4);
         this.putKeyToContainerServer(var4, var2, var3);
      } else {
         String var10 = "Base.KeyRing";
         if (this.getScript().hasSpecialKeyRing() && (float)Rand.Next(100) < 1.0F * this.keySpawnChancedD100) {
            var10 = this.getScript().getRandomSpecialKeyRing();
         }

         InventoryItem var5 = InventoryItemFactory.CreateItem(var10);
         InventoryContainer var6 = (InventoryContainer)var5;
         InventoryItem var7 = this.createVehicleKey();
         this.keySpawned = 1;
         this.keyNamerVehicle(var6);
         this.keyNamerVehicle(var7);
         if (var6.getInventory() != null) {
            var6.getInventory().AddItem(var7);
            if (var2.getBuilding() != null && var2.getBuilding().getDef() != null && var2.getBuilding().getDef().getKeyId() != -1 && Rand.Next(10) != 0) {
               String var8 = "Base.Key1";
               InventoryItem var9 = InventoryItemFactory.CreateItem(var8);
               var9.setKeyId(var2.getBuilding().getDef().getKeyId());
               var6.getInventory().AddItem(var9);
               ItemPickerJava.KeyNamer.nameKey(var9, var2);
            }
         }

         var1.AddItem(var5);
         this.putKeyToContainerServer(var5, var2, var3);
      }

   }

   public void putKeyToContainerServer(InventoryItem var1, IsoGridSquare var2, IsoObject var3) {
      if (GameServer.bServer) {
         INetworkPacket.sendToRelative(PacketTypes.PacketType.AddInventoryItemToContainer, (float)var3.square.x, (float)var3.square.y, this.container, var1);
      }

   }

   public void putKeyToWorld(IsoGridSquare var1) {
      if ((float)Rand.Next(100) >= 1.0F * this.keySpawnChancedD100) {
         InventoryItem var2 = this.createVehicleKey();
         this.keySpawned = 1;
         this.keyNamerVehicle(var2);
         var1.AddWorldInventoryItem(var2, 0.0F, 0.0F, 0.0F);
      } else {
         String var13 = "Base.KeyRing";
         if (this.getScript().hasSpecialKeyRing() && (float)Rand.Next(100) < 1.0F * this.keySpawnChancedD100) {
            var13 = this.getScript().getRandomSpecialKeyRing();
         }

         InventoryItem var3 = InventoryItemFactory.CreateItem(var13);
         InventoryContainer var4 = (InventoryContainer)var3;
         this.keyNamerVehicle(var4);
         InventoryItem var5 = this.createVehicleKey();
         this.keySpawned = 1;
         this.keyNamerVehicle(var5);
         var4.getInventory().AddItem(var5);
         var1.AddWorldInventoryItem(var3, 0.0F, 0.0F, 0.0F);
         if ((float)Rand.Next(100) < 1.0F * this.keySpawnChancedD100) {
            float var6 = this.getX();
            float var7 = this.getY();
            Vector2f var8 = new Vector2f();
            BuildingDef var9 = AmbientStreamManager.getNearestBuilding(var6, var7, var8);
            if (var9 != null && var9.getKeyId() != -1) {
               String var10 = "Base.Key1";
               InventoryItem var11 = InventoryItemFactory.CreateItem(var10);
               var11.setKeyId(var9.getKeyId());
               IsoGridSquare var12 = var9.getFreeSquareInRoom();
               if (var12 != null) {
                  ItemPickerJava.KeyNamer.nameKey(var11, (IsoGridSquare)Objects.requireNonNull(var9.getFreeSquareInRoom()));
               }

               var4.getInventory().AddItem(var11);
            }
         }
      }

   }

   public void addKeyToWorld() {
      if (this.checkIfGoodVehicleForKey()) {
         if (!this.getScriptName().contains("Burnt") && !this.getScriptName().equals("Trailer") && !this.getScriptName().equals("TrailerAdvert")) {
            if (!this.getScriptName().contains("Smashed") && this.haveOneDoorUnlocked()) {
               if ((float)Rand.Next(100) < 1.0F * this.keySpawnChancedD100) {
                  this.keysInIgnition = true;
                  this.currentKey = this.createVehicleKey();
                  this.keySpawned = 1;
                  return;
               }

               if ((float)Rand.Next(100) < 1.0F * this.keySpawnChancedD100) {
                  this.addKeyToGloveBox();
                  return;
               }
            }

            IsoGridSquare var1 = this.getCell().getGridSquare((double)this.getX(), (double)this.getY(), (double)this.getZ());
            if (var1 != null) {
               this.addKeyToSquare(var1);
            }

         }
      }
   }

   public void addKeyToGloveBox() {
      if (this.keySpawned == 0) {
         if (this.getPartById("GloveBox") != null) {
            VehiclePart var1 = this.getPartById("GloveBox");
            if ((float)Rand.Next(100) >= 1.0F * this.keySpawnChancedD100) {
               InventoryItem var2 = this.createVehicleKey();
               this.keyNamerVehicle(var2);
               var1.container.addItem(var2);
               if ((float)Rand.Next(100) < 1.0F * this.keySpawnChancedD100) {
                  float var3 = this.getX();
                  float var4 = this.getY();
                  Vector2f var5 = new Vector2f();
                  BuildingDef var6 = AmbientStreamManager.getNearestBuilding(var3, var4, var5);
                  if (var6 != null && var6.getKeyId() != -1) {
                     String var7 = "Base.Key1";
                     InventoryItem var8 = InventoryItemFactory.CreateItem(var7);
                     var8.setKeyId(var6.getKeyId());
                     IsoGridSquare var9 = var6.getFreeSquareInRoom();
                     if (var9 != null) {
                        ItemPickerJava.KeyNamer.nameKey(var8, (IsoGridSquare)Objects.requireNonNull(var6.getFreeSquareInRoom()));
                     }

                     var1.container.AddItem(var8);
                  }
               }
            } else {
               String var13 = "Base.KeyRing";
               if (this.getScript().hasSpecialKeyRing() && (float)Rand.Next(100) < 1.0F * this.keySpawnChancedD100) {
                  var13 = this.getScript().getRandomSpecialKeyRing();
               }

               InventoryItem var14 = InventoryItemFactory.CreateItem(var13);
               InventoryContainer var15 = (InventoryContainer)var14;
               this.keyNamerVehicle(var15);
               InventoryItem var16 = this.createVehicleKey();
               this.keyNamerVehicle(var16);
               var15.getInventory().AddItem(var16);
               if ((float)Rand.Next(100) < 1.0F * this.keySpawnChancedD100) {
                  float var17 = this.getX();
                  float var18 = this.getY();
                  Vector2f var19 = new Vector2f();
                  BuildingDef var20 = AmbientStreamManager.getNearestBuilding(var17, var18, var19);
                  if (var20 != null && var20.getKeyId() != -1) {
                     String var10 = "Base.Key1";
                     InventoryItem var11 = InventoryItemFactory.CreateItem(var10);
                     var11.setKeyId(var20.getKeyId());
                     IsoGridSquare var12 = var20.getFreeSquareInRoom();
                     if (var12 != null) {
                        ItemPickerJava.KeyNamer.nameKey(var11, var12);
                     }

                     var15.getInventory().AddItem(var11);
                  }
               }

               var1.container.addItem(var14);
            }

            this.keySpawned = 1;
         }

      }
   }

   public void addBuildingKeyToGloveBox(IsoGridSquare var1) {
      if (this.getPartById("GloveBox") != null && var1.getBuilding() != null && var1.getBuilding().getDef() != null) {
         VehiclePart var2 = this.getPartById("GloveBox");
         String var3;
         InventoryItem var4;
         if ((float)Rand.Next(100) >= 1.0F * this.keySpawnChancedD100) {
            var3 = "Base.Key1";
            var4 = InventoryItemFactory.CreateItem(var3);
            BuildingDef var5 = var1.getBuilding().getDef();
            var4.setKeyId(var5.getKeyId());
            ItemPickerJava.KeyNamer.nameKey(var4, var1);
            var2.container.AddItem(var4);
         } else {
            var3 = "Base.KeyRing";
            if (this.getScript().hasSpecialKeyRing() && (float)Rand.Next(100) < 1.0F * this.keySpawnChancedD100) {
               var3 = this.getScript().getRandomSpecialKeyRing();
            }

            var4 = InventoryItemFactory.CreateItem(var3);
            InventoryContainer var9 = (InventoryContainer)var4;
            String var6 = "Base.Key1";
            InventoryItem var7 = InventoryItemFactory.CreateItem(var6);
            BuildingDef var8 = var1.getBuilding().getDef();
            var7.setKeyId(var8.getKeyId());
            ItemPickerJava.KeyNamer.nameKey(var7, var1);
            var9.getInventory().AddItem(var7);
            var2.container.addItem(var4);
         }
      }

   }

   public InventoryItem createVehicleKey() {
      InventoryItem var1 = InventoryItemFactory.CreateItem("CarKey");
      var1.setKeyId(this.getKeyId());
      keyNamerVehicle(var1, this);
      Color var2 = Color.HSBtoRGB(this.colorHue, this.colorSaturation * 0.5F, this.colorValue);
      var1.setColor(var2);
      var1.setCustomColor(true);
      return var1;
   }

   public boolean addKeyToSquare(IsoGridSquare var1) {
      boolean var2 = false;
      IsoGridSquare var3 = null;

      int var4;
      int var5;
      for(var4 = 0; var4 < 3; ++var4) {
         if (Rand.Next(2) == 0) {
            for(var5 = var1.getX() - 10; var5 < var1.getX() + 10; ++var5) {
               var2 = this.addKeyToSquare2(var1, var5);
               if (var2) {
                  return true;
               }
            }
         } else {
            for(var5 = var1.getX() + 10; var5 > var1.getX() - 10; --var5) {
               var2 = this.addKeyToSquare2(var1, var5);
               if (var2) {
                  return true;
               }
            }
         }
      }

      if ((float)Rand.Next(100) < 1.0F * this.keySpawnChancedD100) {
         for(var4 = 0; var4 < 100; ++var4) {
            var5 = var1.getX() - 10 + Rand.Next(20);
            int var6 = var1.getY() - 10 + Rand.Next(20);
            var3 = IsoWorld.instance.getCell().getGridSquare((double)var5, (double)var6, (double)this.getZ());
            if (var3 != null && !var3.isSolid() && !var3.isSolidTrans() && !var3.HasTree()) {
               this.putKeyToWorld(var3);
               var2 = true;
               return var2;
            }
         }
      }

      return var2;
   }

   public boolean addKeyToSquare2(IsoGridSquare var1, int var2) {
      boolean var3 = false;
      IsoGridSquare var4 = null;
      int var5;
      if (Rand.Next(100) < 50) {
         for(var5 = var1.getY() - 10; var5 < var1.getY() + 10; ++var5) {
            var4 = IsoWorld.instance.getCell().getGridSquare((double)var2, (double)var5, (double)this.getZ());
            if (var4 != null) {
               var3 = this.checkSquareForVehicleKeySpot(var4);
               if (var3) {
                  return true;
               }
            }
         }
      } else {
         for(var5 = var1.getY() + 10; var5 > var1.getY() - 10; --var5) {
            var4 = IsoWorld.instance.getCell().getGridSquare((double)var2, (double)var5, (double)this.getZ());
            if (var4 != null) {
               var3 = this.checkSquareForVehicleKeySpot(var4);
               if (var3) {
                  return true;
               }
            }
         }
      }

      return var3;
   }

   public void toggleLockedDoor(VehiclePart var1, IsoGameCharacter var2, boolean var3) {
      if (var3) {
         if (!this.canLockDoor(var1, var2)) {
            return;
         }

         var1.getDoor().setLocked(true);
      } else {
         if (!this.canUnlockDoor(var1, var2)) {
            return;
         }

         var1.getDoor().setLocked(false);
      }

   }

   public boolean canLockDoor(VehiclePart var1, IsoGameCharacter var2) {
      if (var1 == null) {
         return false;
      } else if (var2 == null) {
         return false;
      } else {
         VehicleDoor var3 = var1.getDoor();
         if (var3 == null) {
            return false;
         } else if (var3.lockBroken) {
            return false;
         } else if (var3.locked) {
            return false;
         } else if (this.getSeat(var2) != -1) {
            return true;
         } else if (var2.getInventory().haveThisKeyId(this.getKeyId()) != null) {
            return true;
         } else {
            VehiclePart var4 = var1.getChildWindow();
            if (var4 != null && var4.getInventoryItem() == null) {
               return true;
            } else {
               VehicleWindow var5 = var4 == null ? null : var4.getWindow();
               return var5 != null && (var5.isOpen() || var5.isDestroyed());
            }
         }
      }
   }

   public boolean canUnlockDoor(VehiclePart var1, IsoGameCharacter var2) {
      if (var1 == null) {
         return false;
      } else if (var2 == null) {
         return false;
      } else {
         VehicleDoor var3 = var1.getDoor();
         if (var3 == null) {
            return false;
         } else if (var3.lockBroken) {
            return false;
         } else if (!var3.locked) {
            return false;
         } else if (this.getSeat(var2) != -1) {
            return true;
         } else if (var2.getInventory().haveThisKeyId(this.getKeyId()) != null) {
            return true;
         } else {
            VehiclePart var4 = var1.getChildWindow();
            if (var4 != null && var4.getInventoryItem() == null) {
               return true;
            } else {
               VehicleWindow var5 = var4 == null ? null : var4.getWindow();
               return var5 != null && (var5.isOpen() || var5.isDestroyed());
            }
         }
      }
   }

   private void initParts() {
      for(int var1 = 0; var1 < this.parts.size(); ++var1) {
         VehiclePart var2 = (VehiclePart)this.parts.get(var1);
         String var3 = var2.getLuaFunction("init");
         if (var3 != null) {
            this.callLuaVoid(var3, this, var2);
         }
      }

   }

   public void setGeneralPartCondition(float var1, float var2) {
      for(int var3 = 0; var3 < this.parts.size(); ++var3) {
         VehiclePart var4 = (VehiclePart)this.parts.get(var3);
         var4.setGeneralCondition((InventoryItem)null, var1, var2);
      }

   }

   private void createParts() {
      for(int var1 = 0; var1 < this.parts.size(); ++var1) {
         VehiclePart var2 = (VehiclePart)this.parts.get(var1);
         ArrayList var3 = var2.getItemType();
         if (var2.bCreated && var3 != null && !var3.isEmpty() && var2.getInventoryItem() == null && var2.getTable("install") == null) {
            var2.bCreated = false;
         } else if ((var3 == null || var3.isEmpty()) && var2.getInventoryItem() != null) {
            var2.item = null;
         }

         if (!var2.bCreated) {
            var2.bCreated = true;
            String var4 = var2.getLuaFunction("create");
            if (var4 == null) {
               var2.setRandomCondition((InventoryItem)null);
            } else {
               this.callLuaVoid(var4, this, var2);
               if (var2.getCondition() == -1) {
                  var2.setRandomCondition((InventoryItem)null);
               }
            }
         }
      }

      if (this.hasLightbar() && this.getScript().rightSirenCol != null && this.getScript().leftSirenCol != null) {
         this.leftLight1.r = this.leftLight2.r = this.getScript().leftSirenCol.r;
         this.leftLight1.g = this.leftLight2.g = this.getScript().leftSirenCol.g;
         this.leftLight1.b = this.leftLight2.b = this.getScript().leftSirenCol.b;
         this.rightLight1.r = this.rightLight2.r = this.getScript().rightSirenCol.r;
         this.rightLight1.g = this.rightLight2.g = this.getScript().rightSirenCol.g;
         this.rightLight1.b = this.rightLight2.b = this.getScript().rightSirenCol.b;
      }

   }

   public CarController getController() {
      return this.physics;
   }

   public SurroundVehicle getSurroundVehicle() {
      return this.m_surroundVehicle;
   }

   public int getSkinCount() {
      return this.script.getSkinCount();
   }

   public int getSkinIndex() {
      return this.skinIndex;
   }

   public void setSkinIndex(int var1) {
      if (var1 >= 0 && var1 <= this.getSkinCount()) {
         this.skinIndex = var1;
      }
   }

   public void updateSkin() {
      if (this.sprite != null && this.sprite.modelSlot != null && this.sprite.modelSlot.model != null) {
         VehicleModelInstance var1 = (VehicleModelInstance)this.sprite.modelSlot.model;
         VehicleScript.Skin var2 = this.script.getTextures();
         VehicleScript var3 = this.getScript();
         if (this.getSkinIndex() >= 0 && this.getSkinIndex() < var3.getSkinCount()) {
            var2 = var3.getSkin(this.getSkinIndex());
         }

         var1.LoadTexture(var2.texture);
         var1.tex = var2.textureData;
         var1.textureMask = var2.textureDataMask;
         var1.textureDamage1Overlay = var2.textureDataDamage1Overlay;
         var1.textureDamage1Shell = var2.textureDataDamage1Shell;
         var1.textureDamage2Overlay = var2.textureDataDamage2Overlay;
         var1.textureDamage2Shell = var2.textureDataDamage2Shell;
         var1.textureLights = var2.textureDataLights;
         var1.textureRust = var2.textureDataRust;
         if (var1.tex != null) {
            var1.tex.bindAlways = true;
         } else {
            DebugLog.Animation.error("texture not found:", this.getSkin());
         }

      }
   }

   public Texture getShadowTexture() {
      if (this.getScript() != null) {
         VehicleScript.Skin var1 = this.getScript().getTextures();
         if (this.getSkinIndex() >= 0 && this.getSkinIndex() < this.getScript().getSkinCount()) {
            var1 = this.getScript().getSkin(this.getSkinIndex());
         }

         if (var1.textureDataShadow != null) {
            return var1.textureDataShadow;
         }
      }

      if (vehicleShadow == null) {
         int var2 = 0;
         var2 |= TextureID.bUseCompression ? 4 : 0;
         vehicleShadow = Texture.getSharedTexture("media/vehicleShadow.png", var2);
      }

      return vehicleShadow;
   }

   public VehicleScript getScript() {
      return this.script;
   }

   public void setScript(String var1) {
      if (!StringUtils.isNullOrWhitespace(var1)) {
         this.scriptName = var1;
         boolean var2 = this.script != null;
         this.script = ScriptManager.instance.getVehicle(this.scriptName);
         ArrayList var4;
         int var5;
         if (this.script == null) {
            ArrayList var3 = ScriptManager.instance.getAllVehicleScripts();
            if (!var3.isEmpty()) {
               var4 = new ArrayList();

               for(var5 = 0; var5 < var3.size(); ++var5) {
                  VehicleScript var6 = (VehicleScript)var3.get(var5);
                  if (var6.getWheelCount() == 0) {
                     var4.add(var6);
                     var3.remove(var5--);
                  }
               }

               boolean var12 = this.loaded && this.parts.isEmpty() || this.scriptName.contains("Burnt");
               if (var12 && !var4.isEmpty()) {
                  this.script = (VehicleScript)var4.get(Rand.Next(var4.size()));
               } else if (!var3.isEmpty()) {
                  this.script = (VehicleScript)var3.get(Rand.Next(var3.size()));
               }

               if (this.script != null) {
                  this.scriptName = this.script.getFullName();
               }
            }
         }

         this.battery = null;
         this.models.clear();
         if (this.script != null) {
            this.scriptName = this.script.getFullName();
            Passenger[] var10 = this.passengers;
            this.passengers = new Passenger[this.script.getPassengerCount()];

            for(int var11 = 0; var11 < this.passengers.length; ++var11) {
               if (var11 < var10.length) {
                  this.passengers[var11] = var10[var11];
               } else {
                  this.passengers[var11] = new Passenger();
               }
            }

            var4 = new ArrayList();
            var4.addAll(this.parts);
            this.parts.clear();

            for(var5 = 0; var5 < this.script.getPartCount(); ++var5) {
               VehicleScript.Part var13 = this.script.getPart(var5);
               VehiclePart var7 = null;

               for(int var8 = 0; var8 < var4.size(); ++var8) {
                  VehiclePart var9 = (VehiclePart)var4.get(var8);
                  if (var9.getScriptPart() != null && var13.id.equals(var9.getScriptPart().id)) {
                     var7 = var9;
                     break;
                  }

                  if (var9.partId != null && var13.id.equals(var9.partId)) {
                     var7 = var9;
                     break;
                  }
               }

               if (var7 == null) {
                  var7 = new VehiclePart(this);
               }

               var7.setScriptPart(var13);
               var7.category = var13.category;
               var7.specificItem = var13.specificItem;
               var7.setDurability(var13.getDurability());
               if (var13.container != null && var13.container.contentType == null) {
                  if (var7.getItemContainer() == null) {
                     ItemContainer var15 = new ItemContainer(var13.id, (IsoGridSquare)null, this);
                     var7.setItemContainer(var15);
                     var15.ID = 0;
                  }

                  var7.getItemContainer().Capacity = var13.container.capacity;
               } else {
                  var7.setItemContainer((ItemContainer)null);
               }

               if (var13.door == null) {
                  var7.door = null;
               } else if (var7.door == null) {
                  var7.door = new VehicleDoor(var7);
                  var7.door.init(var13.door);
               }

               if (var13.window == null) {
                  var7.window = null;
               } else if (var7.window == null) {
                  var7.window = new VehicleWindow(var7);
                  var7.window.init(var13.window);
               } else {
                  var7.window.openable = var13.window.openable;
               }

               var7.parent = null;
               if (var7.children != null) {
                  var7.children.clear();
               }

               this.parts.add(var7);
               if ("Battery".equals(var7.getId())) {
                  this.battery = var7;
               }
            }

            VehiclePart var14;
            for(var5 = 0; var5 < this.script.getPartCount(); ++var5) {
               var14 = (VehiclePart)this.parts.get(var5);
               VehicleScript.Part var16 = var14.getScriptPart();
               if (var16.parent != null) {
                  var14.parent = this.getPartById(var16.parent);
                  if (var14.parent != null) {
                     var14.parent.addChild(var14);
                  }
               }
            }

            if (!var2 && !this.loaded) {
               this.frontEndDurability = this.rearEndDurability = 99999;
            }

            this.frontEndDurability = Math.min(this.frontEndDurability, this.script.getFrontEndHealth());
            this.rearEndDurability = Math.min(this.rearEndDurability, this.script.getRearEndHealth());
            this.currentFrontEndDurability = this.frontEndDurability;
            this.currentRearEndDurability = this.rearEndDurability;

            for(var5 = 0; var5 < this.script.getPartCount(); ++var5) {
               var14 = (VehiclePart)this.parts.get(var5);
               var14.setInventoryItem(var14.item);
            }
         }

         if (!this.loaded || this.colorHue == 0.0F && this.colorSaturation == 0.0F && this.colorValue == 0.0F) {
            this.doVehicleColor();
         }

         this.m_surroundVehicle.reset();
      }
   }

   public String getScriptName() {
      return this.scriptName;
   }

   public void setScriptName(String var1) {
      assert var1 == null || var1.contains(".");

      this.scriptName = var1;
   }

   public void setScript() {
      this.setScript(this.scriptName);
   }

   public void scriptReloaded() {
      this.scriptReloaded(false);
   }

   public void scriptReloaded(boolean var1) {
      if (this.physics != null) {
         Transform var2 = allocTransform();
         var2.setIdentity();
         this.getWorldTransform(var2);
         var2.basis.getUnnormalizedRotation(this.savedRot);
         releaseTransform(var2);
         this.breakConstraint(false, false);
         Bullet.removeVehicle(this.VehicleID);
         this.physics = null;
      }

      if (this.createdModel) {
         ModelManager.instance.Remove(this);
         this.createdModel = false;
      }

      this.vehicleEngineRPM = null;

      int var5;
      for(var5 = 0; var5 < this.parts.size(); ++var5) {
         VehiclePart var3 = (VehiclePart)this.parts.get(var5);
         var3.setInventoryItem((InventoryItem)null);
         var3.bCreated = false;
      }

      this.setScript(this.scriptName);
      this.createPhysics(var1);
      if (this.script != null) {
         for(var5 = 0; var5 < this.passengers.length; ++var5) {
            Passenger var6 = this.passengers[var5];
            if (var6 != null && var6.character != null) {
               VehicleScript.Position var4 = this.getPassengerPosition(var5, "inside");
               if (var4 != null) {
                  var6.offset.set(var4.offset);
               }
            }
         }
      }

      this.polyDirty = true;
      if (this.isEngineRunning()) {
         this.engineDoShuttingDown();
         this.engineState = BaseVehicle.engineStateTypes.Idle;
      }

      if (this.addedToWorld) {
         if (PathfindNative.USE_NATIVE_CODE) {
            PathfindNative.instance.removeVehicle(this);
            PathfindNative.instance.addVehicle(this);
         } else {
            PolygonalMap2.instance.removeVehicleFromWorld(this);
            PolygonalMap2.instance.addVehicleToWorld(this);
         }
      }

   }

   public String getSkin() {
      if (this.script != null && this.script.getSkinCount() != 0) {
         if (this.skinIndex < 0 || this.skinIndex >= this.script.getSkinCount()) {
            this.skinIndex = Rand.Next(this.script.getSkinCount());
         }

         return this.script.getSkin(this.skinIndex).texture;
      } else {
         return "BOGUS";
      }
   }

   protected ModelInfo setModelVisible(VehiclePart var1, VehicleScript.Model var2, boolean var3) {
      ModelInfo var5;
      for(int var4 = 0; var4 < this.models.size(); ++var4) {
         var5 = (ModelInfo)this.models.get(var4);
         if (var5.part == var1 && var5.scriptModel == var2) {
            if (var3) {
               return var5;
            }

            if (var5.m_animPlayer != null) {
               var5.m_animPlayer = (AnimationPlayer)Pool.tryRelease((IPooledObject)var5.m_animPlayer);
            }

            this.models.remove(var4);
            if (this.createdModel) {
               ModelManager.instance.Remove(this);
               ModelManager.instance.addVehicle(this);
            }

            var1.updateFlags = (short)(var1.updateFlags | 64);
            this.updateFlags = (short)(this.updateFlags | 64);
            return null;
         }
      }

      if (var3) {
         String var6 = this.getModelScriptNameForPart(var1, var2);
         if (var6 == null) {
            return null;
         } else {
            var5 = new ModelInfo();
            var5.part = var1;
            var5.scriptModel = var2;
            var5.modelScript = ScriptManager.instance.getModelScript(var6);
            var5.wheelIndex = var1.getWheelIndex();
            this.models.add(var5);
            if (this.createdModel) {
               ModelManager.instance.Remove(this);
               ModelManager.instance.addVehicle(this);
            }

            var1.updateFlags = (short)(var1.updateFlags | 64);
            this.updateFlags = (short)(this.updateFlags | 64);
            return var5;
         }
      } else {
         return null;
      }
   }

   private String getModelScriptNameForPart(VehiclePart var1, VehicleScript.Model var2) {
      String var3 = var2.file;
      if (var3 == null) {
         InventoryItem var4 = var1.getInventoryItem();
         if (var4 == null) {
            return null;
         }

         ArrayList var5 = var4.getScriptItem().getVehiclePartModels();
         if (var5 == null || var5.isEmpty()) {
            return null;
         }

         for(int var6 = 0; var6 < var5.size(); ++var6) {
            VehiclePartModel var7 = (VehiclePartModel)var5.get(var6);
            if (var7.partId.equalsIgnoreCase(var1.getId()) && var7.partModelId.equalsIgnoreCase(var2.getId())) {
               var3 = var7.modelId;
               break;
            }
         }
      }

      return var3;
   }

   protected ModelInfo getModelInfoForPart(VehiclePart var1) {
      for(int var2 = 0; var2 < this.models.size(); ++var2) {
         ModelInfo var3 = (ModelInfo)this.models.get(var2);
         if (var3.part == var1) {
            return var3;
         }
      }

      return null;
   }

   protected VehicleScript.Passenger getScriptPassenger(int var1) {
      if (this.getScript() == null) {
         return null;
      } else {
         return var1 >= 0 && var1 < this.getScript().getPassengerCount() ? this.getScript().getPassenger(var1) : null;
      }
   }

   public int getMaxPassengers() {
      return this.passengers.length;
   }

   public boolean setPassenger(int var1, IsoGameCharacter var2, Vector3f var3) {
      if (var1 >= 0 && var1 < this.passengers.length) {
         if (var1 == 0) {
            this.setNeedPartsUpdate(true);
         }

         this.passengers[var1].character = var2;
         this.passengers[var1].offset.set(var3);
         return true;
      } else {
         return false;
      }
   }

   public boolean clearPassenger(int var1) {
      if (var1 >= 0 && var1 < this.passengers.length) {
         this.passengers[var1].character = null;
         this.passengers[var1].offset.set(0.0F, 0.0F, 0.0F);
         return true;
      } else {
         return false;
      }
   }

   public Passenger getPassenger(int var1) {
      return var1 >= 0 && var1 < this.passengers.length ? this.passengers[var1] : null;
   }

   public IsoGameCharacter getCharacter(int var1) {
      Passenger var2 = this.getPassenger(var1);
      return var2 != null ? var2.character : null;
   }

   public int getSeat(IsoGameCharacter var1) {
      for(int var2 = 0; var2 < this.getMaxPassengers(); ++var2) {
         if (this.getCharacter(var2) == var1) {
            return var2;
         }
      }

      return -1;
   }

   public boolean isDriver(IsoGameCharacter var1) {
      return this.getSeat(var1) == 0;
   }

   public Vector3f getWorldPos(Vector3f var1, Vector3f var2, VehicleScript var3) {
      return this.getWorldPos(var1.x, var1.y, var1.z, var2, var3);
   }

   public Vector3f getWorldPos(float var1, float var2, float var3, Vector3f var4, VehicleScript var5) {
      Transform var6 = this.getWorldTransform(allocTransform());
      var6.origin.set(0.0F, 0.0F, 0.0F);
      var4.set(var1, var2, var3);
      var6.transform(var4);
      releaseTransform(var6);
      float var7 = this.jniTransform.origin.x + WorldSimulation.instance.offsetX;
      float var8 = this.jniTransform.origin.z + WorldSimulation.instance.offsetY;
      float var9 = this.jniTransform.origin.y / 2.44949F;
      var4.set(var7 + var4.x, var8 + var4.z, var9 + var4.y);
      return var4;
   }

   public Vector3f getWorldPos(Vector3f var1, Vector3f var2) {
      return this.getWorldPos(var1.x, var1.y, var1.z, var2, this.getScript());
   }

   public Vector3f getWorldPos(float var1, float var2, float var3, Vector3f var4) {
      return this.getWorldPos(var1, var2, var3, var4, this.getScript());
   }

   public Vector3f getLocalPos(Vector3f var1, Vector3f var2) {
      return this.getLocalPos(var1.x, var1.y, var1.z, var2);
   }

   public Vector3f getLocalPos(float var1, float var2, float var3, Vector3f var4) {
      Transform var5 = this.getWorldTransform(allocTransform());
      var5.inverse();
      var4.set(var1 - WorldSimulation.instance.offsetX, 0.0F, var2 - WorldSimulation.instance.offsetY);
      var5.transform(var4);
      releaseTransform(var5);
      return var4;
   }

   public Vector3f getPassengerLocalPos(int var1, Vector3f var2) {
      Passenger var3 = this.getPassenger(var1);
      return var3 == null ? null : var2.set(this.script.getModel().getOffset()).add(var3.offset);
   }

   public Vector3f getPassengerWorldPos(int var1, Vector3f var2) {
      Passenger var3 = this.getPassenger(var1);
      return var3 == null ? null : this.getPassengerPositionWorldPos(var3.offset.x, var3.offset.y, var3.offset.z, var2);
   }

   public Vector3f getPassengerPositionWorldPos(VehicleScript.Position var1, Vector3f var2) {
      return this.getPassengerPositionWorldPos(var1.offset.x, var1.offset.y, var1.offset.z, var2);
   }

   public Vector3f getPassengerPositionWorldPos(float var1, float var2, float var3, Vector3f var4) {
      var4.set(this.script.getModel().offset);
      var4.add(var1, var2, var3);
      this.getWorldPos(var4.x, var4.y, var4.z, var4);
      var4.z = (float)PZMath.fastfloor(this.getZ());
      return var4;
   }

   public VehicleScript.Anim getPassengerAnim(int var1, String var2) {
      VehicleScript.Passenger var3 = this.getScriptPassenger(var1);
      if (var3 == null) {
         return null;
      } else {
         for(int var4 = 0; var4 < var3.anims.size(); ++var4) {
            VehicleScript.Anim var5 = (VehicleScript.Anim)var3.anims.get(var4);
            if (var2.equals(var5.id)) {
               return var5;
            }
         }

         return null;
      }
   }

   public VehicleScript.Position getPassengerPosition(int var1, String var2) {
      VehicleScript.Passenger var3 = this.getScriptPassenger(var1);
      return var3 == null ? null : var3.getPositionById(var2);
   }

   public VehiclePart getPassengerDoor(int var1) {
      VehicleScript.Passenger var2 = this.getScriptPassenger(var1);
      return var2 == null ? null : this.getPartById(var2.door);
   }

   public VehiclePart getPassengerDoor2(int var1) {
      VehicleScript.Passenger var2 = this.getScriptPassenger(var1);
      return var2 == null ? null : this.getPartById(var2.door2);
   }

   public boolean isPositionOnLeftOrRight(float var1, float var2) {
      Vector3f var3 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
      this.getLocalPos(var1, var2, 0.0F, var3);
      var1 = var3.x;
      ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var3);
      Vector3f var4 = this.script.getExtents();
      Vector3f var5 = this.script.getCenterOfMassOffset();
      float var6 = var5.x - var4.x / 2.0F;
      float var7 = var5.x + var4.x / 2.0F;
      return var1 < var6 * 0.98F || var1 > var7 * 0.98F;
   }

   public boolean haveOneDoorUnlocked() {
      for(int var1 = 0; var1 < this.getPartCount(); ++var1) {
         VehiclePart var2 = this.getPartByIndex(var1);
         if (var2.getDoor() != null && (var2.getId().contains("Left") || var2.getId().contains("Right")) && (!var2.getDoor().isLocked() || var2.getDoor().isOpen())) {
            return true;
         }
      }

      return false;
   }

   public String getPassengerArea(int var1) {
      VehicleScript.Passenger var2 = this.getScriptPassenger(var1);
      return var2 == null ? null : var2.area;
   }

   public void playPassengerAnim(int var1, String var2) {
      IsoGameCharacter var3 = this.getCharacter(var1);
      this.playPassengerAnim(var1, var2, var3);
   }

   public void playPassengerAnim(int var1, String var2, IsoGameCharacter var3) {
      if (var3 != null) {
         VehicleScript.Anim var4 = this.getPassengerAnim(var1, var2);
         if (var4 != null) {
            this.playCharacterAnim(var3, var4, true);
         }
      }
   }

   public void playPassengerSound(int var1, String var2) {
      VehicleScript.Anim var3 = this.getPassengerAnim(var1, var2);
      if (var3 != null && var3.sound != null) {
         this.playSound(var3.sound);
      }
   }

   public void playPartAnim(VehiclePart var1, String var2) {
      if (this.parts.contains(var1)) {
         VehicleScript.Anim var3 = var1.getAnimById(var2);
         if (var3 != null && !StringUtils.isNullOrWhitespace(var3.anim)) {
            ModelInfo var4 = this.getModelInfoForPart(var1);
            if (var4 != null) {
               AnimationPlayer var5 = var4.getAnimationPlayer();
               if (var5 != null && var5.isReady()) {
                  if (var5.getMultiTrack().getIndexOfTrack(var4.m_track) != -1) {
                     var5.getMultiTrack().removeTrack(var4.m_track);
                  }

                  var4.m_track = null;
                  SkinningData var6 = var5.getSkinningData();
                  if (var6 == null || var6.AnimationClips.containsKey(var3.anim)) {
                     AnimationTrack var7 = var5.play(var3.anim, var3.bLoop);
                     var4.m_track = var7;
                     if (var7 != null) {
                        var7.setLayerIdx(0);
                        var7.BlendDelta = 1.0F;
                        var7.SpeedDelta = var3.rate;
                        var7.IsPlaying = var3.bAnimate;
                        var7.reverse = var3.bReverse;
                        if (!var4.modelScript.boneWeights.isEmpty()) {
                           var7.setBoneWeights(var4.modelScript.boneWeights);
                           var7.initBoneWeights(var6);
                        }

                        if (var1.getWindow() != null) {
                           var7.setCurrentTimeValue(var7.getDuration() * var1.getWindow().getOpenDelta());
                        }
                     }

                  }
               }
            }
         }
      }
   }

   public void playActorAnim(VehiclePart var1, String var2, IsoGameCharacter var3) {
      if (var3 != null) {
         if (this.parts.contains(var1)) {
            VehicleScript.Anim var4 = var1.getAnimById("Actor" + var2);
            if (var4 != null) {
               this.playCharacterAnim(var3, var4, !"EngineDoor".equals(var1.getId()));
            }
         }
      }
   }

   private void playCharacterAnim(IsoGameCharacter var1, VehicleScript.Anim var2, boolean var3) {
      var1.PlayAnimUnlooped(var2.anim);
      var1.getSpriteDef().setFrameSpeedPerFrame(var2.rate);
      var1.getLegsSprite().Animate = true;
      Vector3f var4 = this.getForwardVector((Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
      if (var2.angle.lengthSquared() != 0.0F) {
         Matrix4f var5 = (Matrix4f)((Matrix4fObjectPool)TL_matrix4f_pool.get()).alloc();
         var5.rotationXYZ((float)Math.toRadians((double)var2.angle.x), (float)Math.toRadians((double)var2.angle.y), (float)Math.toRadians((double)var2.angle.z));
         Quaternionf var6 = allocQuaternionf();
         var4.rotate(var5.getNormalizedRotation(var6));
         releaseQuaternionf(var6);
         ((Matrix4fObjectPool)TL_matrix4f_pool.get()).release(var5);
      }

      Vector2 var7 = (Vector2)((Vector2ObjectPool)TL_vector2_pool.get()).alloc();
      var7.set(var4.x, var4.z);
      var1.DirectionFromVector(var7);
      ((Vector2ObjectPool)TL_vector2_pool.get()).release(var7);
      var1.setForwardDirection(var4.x, var4.z);
      if (var1.getAnimationPlayer() != null) {
         var1.getAnimationPlayer().setTargetAngle(var1.getForwardDirection().getDirection());
         if (var3) {
            var1.getAnimationPlayer().setAngleToTarget();
         }
      }

      ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var4);
   }

   public void playPartSound(VehiclePart var1, IsoPlayer var2, String var3) {
      if (this.parts.contains(var1)) {
         VehicleScript.Anim var4 = var1.getAnimById(var3);
         if (var4 != null && var4.sound != null) {
            this.getEmitter().playSound(var4.sound, (IsoGameCharacter)var2);
         }
      }
   }

   public void setCharacterPosition(IsoGameCharacter var1, int var2, String var3) {
      VehicleScript.Passenger var4 = this.getScriptPassenger(var2);
      if (var4 != null) {
         VehicleScript.Position var5 = var4.getPositionById(var3);
         if (var5 != null) {
            if (this.getCharacter(var2) == var1) {
               this.passengers[var2].offset.set(var5.offset);
            } else {
               Vector3f var6 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
               if (var5.area == null) {
                  this.getPassengerPositionWorldPos(var5, var6);
               } else {
                  VehicleScript.Area var7 = this.script.getAreaById(var5.area);
                  Vector2 var8 = (Vector2)((Vector2ObjectPool)TL_vector2_pool.get()).alloc();
                  Vector2 var9 = this.areaPositionWorld4PlayerInteract(var7, var8);
                  var6.x = var9.x;
                  var6.y = var9.y;
                  var6.z = (float)PZMath.fastfloor(this.getZ());
                  ((Vector2ObjectPool)TL_vector2_pool.get()).release(var8);
               }

               var1.setX(var6.x);
               var1.setY(var6.y);
               var1.setZ(var6.z);
               ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var6);
            }

            if (var1 instanceof IsoPlayer && ((IsoPlayer)var1).isLocalPlayer()) {
               ((IsoPlayer)var1).dirtyRecalcGridStackTime = 10.0F;
            }

         }
      }
   }

   public void transmitCharacterPosition(int var1, String var2) {
      if (GameClient.bClient) {
         VehicleManager.instance.sendPassengerPosition(this, var1, var2);
      }

   }

   public void setCharacterPositionToAnim(IsoGameCharacter var1, int var2, String var3) {
      VehicleScript.Anim var4 = this.getPassengerAnim(var2, var3);
      if (var4 != null) {
         if (this.getCharacter(var2) == var1) {
            this.passengers[var2].offset.set(var4.offset);
         } else {
            Vector3f var5 = this.getWorldPos(var4.offset, (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
            var1.setX(var5.x);
            var1.setY(var5.y);
            var1.setZ(0.0F);
            ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var5);
         }

      }
   }

   public int getPassengerSwitchSeatCount(int var1) {
      VehicleScript.Passenger var2 = this.getScriptPassenger(var1);
      return var2 == null ? -1 : var2.switchSeats.size();
   }

   public VehicleScript.Passenger.SwitchSeat getPassengerSwitchSeat(int var1, int var2) {
      VehicleScript.Passenger var3 = this.getScriptPassenger(var1);
      if (var3 == null) {
         return null;
      } else {
         return var2 >= 0 && var2 < var3.switchSeats.size() ? (VehicleScript.Passenger.SwitchSeat)var3.switchSeats.get(var2) : null;
      }
   }

   private VehicleScript.Passenger.SwitchSeat getSwitchSeat(int var1, int var2) {
      VehicleScript.Passenger var3 = this.getScriptPassenger(var1);
      if (var3 == null) {
         return null;
      } else {
         for(int var4 = 0; var4 < var3.switchSeats.size(); ++var4) {
            VehicleScript.Passenger.SwitchSeat var5 = (VehicleScript.Passenger.SwitchSeat)var3.switchSeats.get(var4);
            if (var5.seat == var2 && this.getPartForSeatContainer(var2) != null && this.getPartForSeatContainer(var2).getInventoryItem() != null) {
               return var5;
            }
         }

         return null;
      }
   }

   public String getSwitchSeatAnimName(int var1, int var2) {
      VehicleScript.Passenger.SwitchSeat var3 = this.getSwitchSeat(var1, var2);
      return var3 == null ? null : var3.anim;
   }

   public float getSwitchSeatAnimRate(int var1, int var2) {
      VehicleScript.Passenger.SwitchSeat var3 = this.getSwitchSeat(var1, var2);
      return var3 == null ? 0.0F : var3.rate;
   }

   public String getSwitchSeatSound(int var1, int var2) {
      VehicleScript.Passenger.SwitchSeat var3 = this.getSwitchSeat(var1, var2);
      return var3 == null ? null : var3.sound;
   }

   public boolean canSwitchSeat(int var1, int var2) {
      VehicleScript.Passenger.SwitchSeat var3 = this.getSwitchSeat(var1, var2);
      return var3 != null;
   }

   public void switchSeat(IsoGameCharacter var1, int var2) {
      int var3 = this.getSeat(var1);
      if (var3 != -1) {
         this.clearPassenger(var3);
         VehicleScript.Position var4 = this.getPassengerPosition(var2, "inside");
         if (var4 == null) {
            Vector3f var5 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
            var5.set(0.0F, 0.0F, 0.0F);
            this.setPassenger(var2, var1, var5);
            ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var5);
         } else {
            this.setPassenger(var2, var1, var4.offset);
         }

      }
   }

   public void playSwitchSeatAnim(int var1, int var2) {
      IsoGameCharacter var3 = this.getCharacter(var1);
      if (var3 != null) {
         VehicleScript.Passenger.SwitchSeat var4 = this.getSwitchSeat(var1, var2);
         if (var4 != null) {
            var3.PlayAnimUnlooped(var4.anim);
            var3.getSpriteDef().setFrameSpeedPerFrame(var4.rate);
            var3.getLegsSprite().Animate = true;
         }
      }
   }

   public boolean isSeatOccupied(int var1) {
      VehiclePart var2 = this.getPartForSeatContainer(var1);
      if (var2 != null && var2.getItemContainer() != null && !var2.getItemContainer().getItems().isEmpty() && this.getCharacter(var1) == null && var2.getItemContainer().getCapacityWeight() > (float)(var2.getItemContainer().getCapacity() / 4)) {
         return false;
      } else {
         return this.getCharacter(var1) != null;
      }
   }

   public boolean isSeatInstalled(int var1) {
      VehiclePart var2 = this.getPartForSeatContainer(var1);
      return var2 != null && var2.getInventoryItem() != null;
   }

   public int getBestSeat(IsoGameCharacter var1) {
      if (PZMath.fastfloor(this.getZ()) != PZMath.fastfloor(var1.getZ())) {
         return -1;
      } else if (var1.DistTo(this) > 5.0F) {
         return -1;
      } else {
         VehicleScript var2 = this.getScript();
         if (var2 == null) {
            return -1;
         } else {
            Vector3f var3 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
            Vector3f var4 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
            Vector2 var5 = (Vector2)((Vector2ObjectPool)TL_vector2_pool.get()).alloc();

            for(int var6 = 0; var6 < var2.getPassengerCount(); ++var6) {
               if (!this.isEnterBlocked(var1, var6) && !this.isSeatOccupied(var6)) {
                  VehicleScript.Position var7 = this.getPassengerPosition(var6, "outside");
                  float var8;
                  float var9;
                  float var10;
                  if (var7 != null) {
                     this.getPassengerPositionWorldPos(var7, var3);
                     var8 = var3.x;
                     var9 = var3.y;
                     this.getPassengerPositionWorldPos(0.0F, var7.offset.y, var7.offset.z, var4);
                     var5.set(var4.x - var1.getX(), var4.y - var1.getY());
                     var5.normalize();
                     var10 = var5.dot(var1.getForwardDirection());
                     if (var10 > 0.5F && IsoUtils.DistanceTo(var1.getX(), var1.getY(), var8, var9) < 1.0F) {
                        ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var3);
                        ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var4);
                        ((Vector2ObjectPool)TL_vector2_pool.get()).release(var5);
                        return var6;
                     }
                  }

                  var7 = this.getPassengerPosition(var6, "outside2");
                  if (var7 != null) {
                     this.getPassengerPositionWorldPos(var7, var3);
                     var8 = var3.x;
                     var9 = var3.y;
                     this.getPassengerPositionWorldPos(0.0F, var7.offset.y, var7.offset.z, var4);
                     var5.set(var4.x - var1.getX(), var4.y - var1.getY());
                     var5.normalize();
                     var10 = var5.dot(var1.getForwardDirection());
                     if (var10 > 0.5F && IsoUtils.DistanceTo(var1.getX(), var1.getY(), var8, var9) < 1.0F) {
                        ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var3);
                        ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var4);
                        ((Vector2ObjectPool)TL_vector2_pool.get()).release(var5);
                        return var6;
                     }
                  }
               }
            }

            ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var3);
            ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var4);
            ((Vector2ObjectPool)TL_vector2_pool.get()).release(var5);
            return -1;
         }
      }
   }

   public void updateHasExtendOffsetForExit(IsoGameCharacter var1) {
      this.hasExtendOffsetExiting = true;
      this.updateHasExtendOffset(var1);
      this.getPoly();
   }

   public void updateHasExtendOffsetForExitEnd(IsoGameCharacter var1) {
      this.hasExtendOffsetExiting = false;
      this.updateHasExtendOffset(var1);
      this.getPoly();
   }

   public void updateHasExtendOffset(IsoGameCharacter var1) {
      this.hasExtendOffset = false;
      this.hasExtendOffsetExiting = false;
   }

   public VehiclePart getUseablePart(IsoGameCharacter var1) {
      return this.getUseablePart(var1, true);
   }

   public VehiclePart getUseablePart(IsoGameCharacter var1, boolean var2) {
      if (var1.getVehicle() != null) {
         return null;
      } else if (PZMath.fastfloor(this.getZ()) != PZMath.fastfloor(var1.getZ())) {
         return null;
      } else if (var1.DistTo(this) > 6.0F) {
         return null;
      } else {
         VehicleScript var3 = this.getScript();
         if (var3 == null) {
            return null;
         } else {
            Vector3f var4 = var3.getExtents();
            Vector3f var5 = var3.getCenterOfMassOffset();
            float var6 = var5.z - var4.z / 2.0F;
            float var7 = var5.z + var4.z / 2.0F;
            Vector2 var8 = (Vector2)((Vector2ObjectPool)TL_vector2_pool.get()).alloc();
            Vector3f var9 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();

            for(int var10 = 0; var10 < this.parts.size(); ++var10) {
               VehiclePart var11 = (VehiclePart)this.parts.get(var10);
               if (var11.getArea() != null && this.isInArea(var11.getArea(), var1)) {
                  String var12 = var11.getLuaFunction("use");
                  if (var12 != null && !var12.equals("")) {
                     VehicleScript.Area var13 = var3.getAreaById(var11.getArea());
                     if (var13 != null) {
                        Vector2 var14 = this.areaPositionLocal(var13, var8);
                        if (var14 != null) {
                           float var15 = 0.0F;
                           float var16 = 0.0F;
                           float var17 = 0.0F;
                           if (!(var14.y >= var7) && !(var14.y <= var6)) {
                              var17 = var14.y;
                           } else {
                              var15 = var14.x;
                           }

                           if (!var2) {
                              return var11;
                           }

                           this.getWorldPos(var15, var16, var17, var9);
                           var8.set(var9.x - var1.getX(), var9.y - var1.getY());
                           var8.normalize();
                           float var19 = var8.dot(var1.getForwardDirection());
                           if (var19 > 0.5F && !PolygonalMap2.instance.lineClearCollide(var1.getX(), var1.getY(), var9.x, var9.y, PZMath.fastfloor(var1.getZ()), this, false, true)) {
                              ((Vector2ObjectPool)TL_vector2_pool.get()).release(var8);
                              ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var9);
                              return var11;
                           }
                           break;
                        }
                     }
                  }
               }
            }

            ((Vector2ObjectPool)TL_vector2_pool.get()).release(var8);
            ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var9);
            return null;
         }
      }
   }

   public VehiclePart getClosestWindow(IsoGameCharacter var1) {
      if (PZMath.fastfloor(this.getZ()) != PZMath.fastfloor(var1.getZ())) {
         return null;
      } else if (var1.DistTo(this) > 5.0F) {
         return null;
      } else {
         Vector3f var2 = this.script.getExtents();
         Vector3f var3 = this.script.getCenterOfMassOffset();
         float var4 = var3.z - var2.z / 2.0F;
         float var5 = var3.z + var2.z / 2.0F;
         Vector2 var6 = (Vector2)((Vector2ObjectPool)TL_vector2_pool.get()).alloc();
         Vector3f var7 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();

         for(int var8 = 0; var8 < this.parts.size(); ++var8) {
            VehiclePart var9 = (VehiclePart)this.parts.get(var8);
            if (var9.getWindow() != null && var9.getArea() != null && this.isInArea(var9.getArea(), var1)) {
               VehicleScript.Area var10 = this.script.getAreaById(var9.getArea());
               if (!(var10.y >= var5) && !(var10.y <= var4)) {
                  var7.set(0.0F, 0.0F, var10.y);
               } else {
                  var7.set(var10.x, 0.0F, 0.0F);
               }

               this.getWorldPos(var7, var7);
               var6.set(var7.x - var1.getX(), var7.y - var1.getY());
               var6.normalize();
               float var12 = var6.dot(var1.getForwardDirection());
               if (var12 > 0.5F) {
                  ((Vector2ObjectPool)TL_vector2_pool.get()).release(var6);
                  ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var7);
                  return var9;
               }
               break;
            }
         }

         ((Vector2ObjectPool)TL_vector2_pool.get()).release(var6);
         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var7);
         return null;
      }
   }

   public void getFacingPosition(IsoGameCharacter var1, Vector2 var2) {
      Vector3f var3 = this.getLocalPos(var1.getX(), var1.getY(), var1.getZ(), (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
      Vector3f var4 = this.script.getExtents();
      Vector3f var5 = this.script.getCenterOfMassOffset();
      float var6 = var5.x - var4.x / 2.0F;
      float var7 = var5.x + var4.x / 2.0F;
      float var8 = var5.z - var4.z / 2.0F;
      float var9 = var5.z + var4.z / 2.0F;
      float var10 = 0.0F;
      float var11 = 0.0F;
      if (var3.x <= 0.0F && var3.z >= var8 && var3.z <= var9) {
         var11 = var3.z;
      } else if (var3.x > 0.0F && var3.z >= var8 && var3.z <= var9) {
         var11 = var3.z;
      } else if (var3.z <= 0.0F && var3.x >= var6 && var3.x <= var7) {
         var10 = var3.x;
      } else if (var3.z > 0.0F && var3.x >= var6 && var3.x <= var7) {
         var10 = var3.x;
      }

      this.getWorldPos(var10, 0.0F, var11, var3);
      var2.set(var3.x, var3.y);
      ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var3);
   }

   public boolean enter(int var1, IsoGameCharacter var2, Vector3f var3) {
      if (!GameClient.bClient) {
         VehiclesDB2.instance.updateVehicleAndTrailer(this);
      }

      if (var2 == null) {
         return false;
      } else if (var2.getVehicle() != null && !var2.getVehicle().exit(var2)) {
         return false;
      } else if (this.setPassenger(var1, var2, var3)) {
         var2.setVehicle(this);
         var2.setCollidable(false);
         if (GameClient.bClient) {
            VehicleManager.instance.sendEnter(GameClient.connection, this, var2, var1);
         }

         if (var2 instanceof IsoPlayer && ((IsoPlayer)var2).isLocalPlayer()) {
            ((IsoPlayer)var2).dirtyRecalcGridStackTime = 10.0F;
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean enter(int var1, IsoGameCharacter var2) {
      if (this.getPartForSeatContainer(var1) != null && this.getPartForSeatContainer(var1).getInventoryItem() != null) {
         VehicleScript.Position var3 = this.getPassengerPosition(var1, "outside");
         return var3 != null ? this.enter(var1, var2, var3.offset) : false;
      } else {
         return false;
      }
   }

   public boolean enterRSync(int var1, IsoGameCharacter var2, BaseVehicle var3) {
      if (var2 == null) {
         return false;
      } else {
         VehicleScript.Position var4 = this.getPassengerPosition(var1, "inside");
         if (var4 != null) {
            if (this.setPassenger(var1, var2, var4.offset)) {
               var2.setVehicle(var3);
               var2.setCollidable(false);
               if (GameClient.bClient) {
                  LuaEventManager.triggerEvent("OnContainerUpdate");
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

   public boolean exit(IsoGameCharacter var1) {
      if (!GameClient.bClient) {
         VehiclesDB2.instance.updateVehicleAndTrailer(this);
      }

      if (var1 == null) {
         return false;
      } else {
         int var2 = this.getSeat(var1);
         if (var2 == -1) {
            return false;
         } else if (this.clearPassenger(var2)) {
            this.enginePower = (int)this.getScript().getEngineForce();
            var1.setVehicle((BaseVehicle)null);
            var1.savedVehicleSeat = -1;
            var1.setCollidable(true);
            if (GameClient.bClient) {
               VehicleManager.instance.sendExit(GameClient.connection, this, var1, var2);
            }

            if (this.getDriver() == null && this.soundHornOn) {
               this.onHornStop();
            }

            this.polyGarageCheck = true;
            this.polyDirty = true;
            return true;
         } else {
            return false;
         }
      }
   }

   public boolean exitRSync(IsoGameCharacter var1) {
      if (var1 == null) {
         return false;
      } else {
         int var2 = this.getSeat(var1);
         if (var2 == -1) {
            return false;
         } else if (this.clearPassenger(var2)) {
            var1.setVehicle((BaseVehicle)null);
            var1.setCollidable(true);
            if (GameClient.bClient) {
               LuaEventManager.triggerEvent("OnContainerUpdate");
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public boolean hasRoof(int var1) {
      VehicleScript.Passenger var2 = this.getScriptPassenger(var1);
      return var2 == null ? false : var2.hasRoof;
   }

   public boolean showPassenger(int var1) {
      VehicleScript.Passenger var2 = this.getScriptPassenger(var1);
      return var2 == null ? false : var2.showPassenger;
   }

   public boolean showPassenger(IsoGameCharacter var1) {
      int var2 = this.getSeat(var1);
      return this.showPassenger(var2);
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      float var3 = this.getX();
      float var4 = this.getY();
      if (this.square != null) {
         float var5 = 5.0E-4F;
         this.setX(PZMath.clamp(this.getX(), (float)this.square.x + var5, (float)this.square.x + 1.0F - var5));
         this.setY(PZMath.clamp(this.getY(), (float)this.square.y + var5, (float)this.square.y + 1.0F - var5));
      }

      super.save(var1, var2);
      this.setX(var3);
      this.setY(var4);
      Quaternionf var10 = this.savedRot;
      Transform var6 = this.getWorldTransform(allocTransform());
      var1.putFloat(var6.origin.y);
      var6.getRotation(var10);
      releaseTransform(var6);
      var1.putFloat(var10.x);
      var1.putFloat(var10.y);
      var1.putFloat(var10.z);
      var1.putFloat(var10.w);
      GameWindow.WriteStringUTF(var1, this.scriptName);
      var1.putInt(this.skinIndex);
      var1.put((byte)(this.isEngineRunning() ? 1 : 0));
      var1.putInt(this.frontEndDurability);
      var1.putInt(this.rearEndDurability);
      var1.putInt(this.currentFrontEndDurability);
      var1.putInt(this.currentRearEndDurability);
      var1.putInt(this.engineLoudness);
      var1.putInt(this.engineQuality);
      var1.putInt(this.keyId);
      var1.put(this.keySpawned);
      var1.put((byte)(this.headlightsOn ? 1 : 0));
      var1.put((byte)(this.bCreated ? 1 : 0));
      var1.put((byte)(this.soundHornOn ? 1 : 0));
      var1.put((byte)(this.soundBackMoveOn ? 1 : 0));
      var1.put((byte)this.lightbarLightsMode.get());
      var1.put((byte)this.lightbarSirenMode.get());
      var1.putShort((short)this.parts.size());

      int var7;
      for(var7 = 0; var7 < this.parts.size(); ++var7) {
         VehiclePart var8 = (VehiclePart)this.parts.get(var7);
         var8.save(var1);
      }

      var1.put((byte)(this.keyIsOnDoor ? 1 : 0));
      var1.put((byte)(this.hotwired ? 1 : 0));
      var1.put((byte)(this.hotwiredBroken ? 1 : 0));
      var1.put((byte)(this.keysInIgnition ? 1 : 0));
      var1.putFloat(this.rust);
      var1.putFloat(this.colorHue);
      var1.putFloat(this.colorSaturation);
      var1.putFloat(this.colorValue);
      var1.putInt(this.enginePower);
      var1.putShort(this.VehicleID);
      GameWindow.WriteString((ByteBuffer)var1, (String)null);
      var1.putInt(this.mechanicalID);
      var1.put((byte)(this.alarmed ? 1 : 0));
      var1.putDouble(this.sirenStartTime);
      if (this.getCurrentKey() != null) {
         var1.put((byte)1);
         this.getCurrentKey().saveWithSize(var1, false);
      } else {
         var1.put((byte)0);
      }

      var1.put((byte)this.bloodIntensity.size());
      Iterator var11 = this.bloodIntensity.entrySet().iterator();

      while(var11.hasNext()) {
         Map.Entry var12 = (Map.Entry)var11.next();
         GameWindow.WriteStringUTF(var1, (String)var12.getKey());
         var1.put((Byte)var12.getValue());
      }

      if (this.vehicleTowingID != -1) {
         var1.put((byte)1);
         var1.putInt(this.vehicleTowingID);
         GameWindow.WriteStringUTF(var1, this.towAttachmentSelf);
         GameWindow.WriteStringUTF(var1, this.towAttachmentOther);
         var1.putFloat(this.towConstraintZOffset);
      } else {
         var1.put((byte)0);
      }

      var1.putFloat(this.getRegulatorSpeed());
      var1.put((byte)(this.previouslyEntered ? 1 : 0));
      var1.put((byte)(this.previouslyMoved ? 1 : 0));
      var7 = var1.position();
      var1.putInt(0);
      int var13 = var1.position();
      int var9;
      if (this.animals.isEmpty()) {
         var1.put((byte)0);
      } else {
         var1.put((byte)1);
         var1.putInt(this.animals.size());

         for(var9 = 0; var9 < this.animals.size(); ++var9) {
            ((IsoAnimal)this.animals.get(var9)).save(var1, var2, false);
         }
      }

      var9 = var1.position();
      var1.position(var7);
      var1.putInt(var9 - var13);
      var1.position(var9);
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      super.load(var1, var2, var3);
      float var4 = var1.getFloat();
      int var5 = PZMath.fastfloor(this.getZ());
      float var6 = 2.44949F;
      this.savedPhysicsZ = PZMath.clamp(var4, (float)var5 * var6, ((float)var5 + 0.995F) * var6);
      float var7 = var1.getFloat();
      float var8 = var1.getFloat();
      float var9 = var1.getFloat();
      float var10 = var1.getFloat();
      this.savedRot.set(var7, var8, var9, var10);
      this.jniTransform.origin.set(this.getX() - WorldSimulation.instance.offsetX, Float.isNaN(this.savedPhysicsZ) ? this.getZ() : this.savedPhysicsZ, this.getY() - WorldSimulation.instance.offsetY);
      this.jniTransform.setRotation(this.savedRot);
      this.scriptName = GameWindow.ReadStringUTF(var1);
      this.skinIndex = var1.getInt();
      boolean var11 = var1.get() == 1;
      if (var11) {
         this.engineState = BaseVehicle.engineStateTypes.Running;
      }

      this.frontEndDurability = var1.getInt();
      this.rearEndDurability = var1.getInt();
      this.currentFrontEndDurability = var1.getInt();
      this.currentRearEndDurability = var1.getInt();
      this.engineLoudness = var1.getInt();
      this.engineQuality = var1.getInt();
      this.engineQuality = PZMath.clamp(this.engineQuality, 0, 100);
      this.keyId = var1.getInt();
      this.keySpawned = var1.get();
      this.headlightsOn = var1.get() == 1;
      this.bCreated = var1.get() == 1;
      this.soundHornOn = var1.get() == 1;
      this.soundBackMoveOn = var1.get() == 1;
      this.lightbarLightsMode.set(var1.get());
      this.lightbarSirenMode.set(var1.get());
      short var12 = var1.getShort();

      for(int var13 = 0; var13 < var12; ++var13) {
         VehiclePart var14 = new VehiclePart(this);
         var14.load(var1, var2);
         this.parts.add(var14);
      }

      this.keyIsOnDoor = var1.get() == 1;
      this.hotwired = var1.get() == 1;
      this.hotwiredBroken = var1.get() == 1;
      this.keysInIgnition = var1.get() == 1;
      this.rust = var1.getFloat();
      this.colorHue = var1.getFloat();
      this.colorSaturation = var1.getFloat();
      this.colorValue = var1.getFloat();
      this.enginePower = var1.getInt();
      var1.getShort();
      String var20 = GameWindow.ReadString(var1);
      this.mechanicalID = var1.getInt();
      this.alarmed = var1.get() == 1;
      this.sirenStartTime = var1.getDouble();
      if (var1.get() == 1) {
         InventoryItem var21 = null;

         try {
            var21 = InventoryItem.loadItem(var1, var2);
         } catch (Exception var19) {
            var19.printStackTrace();
         }

         if (var21 != null) {
            this.setCurrentKey(var21);
         }
      }

      byte var22 = var1.get();

      int var15;
      int var17;
      for(var15 = 0; var15 < var22; ++var15) {
         String var16 = GameWindow.ReadStringUTF(var1);
         var17 = var1.get();
         this.bloodIntensity.put(var16, Byte.valueOf((byte)var17));
      }

      if (var1.get() == 1) {
         this.vehicleTowingID = var1.getInt();
         this.towAttachmentSelf = GameWindow.ReadStringUTF(var1);
         this.towAttachmentOther = GameWindow.ReadStringUTF(var1);
         this.towConstraintZOffset = var1.getFloat();
      }

      this.setRegulatorSpeed(var1.getFloat());
      this.previouslyEntered = var1.get() == 1;
      if (var2 >= 196) {
         this.previouslyMoved = var1.get() == 1;
      }

      int var23;
      if (var2 >= 212) {
         var15 = var1.getInt();
         if (GameClient.bClient) {
            var1.position(var1.position() + var15);
         } else if (var1.get() == 1) {
            var23 = var1.getInt();

            for(var17 = 0; var17 < var23; ++var17) {
               IsoAnimal var18 = new IsoAnimal(IsoWorld.instance.getCell());
               var18.load(var1, var2, var3);
               this.addAnimalInTrailer(var18);
            }
         }
      } else if (var1.get() == 1) {
         var15 = var1.getInt();

         for(var23 = 0; var23 < var15; ++var23) {
            IsoAnimal var24 = new IsoAnimal(IsoWorld.instance.getCell());
            var24.load(var1, var2, var3);
            this.addAnimalInTrailer(var24);
         }
      }

      this.loaded = true;
   }

   public void softReset() {
      this.keySpawned = 0;
      this.keyIsOnDoor = false;
      this.keysInIgnition = false;
      this.currentKey = null;
      this.previouslyEntered = false;
      this.previouslyMoved = false;
      this.engineState = BaseVehicle.engineStateTypes.Idle;
      this.randomizeContainers();
   }

   public void trySpawnKey() {
      if (!GameClient.bClient) {
         if (this.script != null && this.script.getPartById("Engine") != null) {
            if (this.keySpawned != 1) {
               if (SandboxOptions.getInstance().VehicleEasyUse.getValue()) {
                  this.addKeyToGloveBox();
               } else {
                  VehicleType var1 = VehicleType.getTypeFromName(this.getVehicleType());
                  int var2 = var1 == null ? 70 : var1.getChanceToSpawnKey();
                  if (Rand.Next(100) <= var2) {
                     this.addKeyToWorld();
                  }

               }
            }
         }
      }
   }

   public boolean shouldCollideWithCharacters() {
      if (this.vehicleTowedBy != null) {
         return this.vehicleTowedBy.shouldCollideWithCharacters();
      } else {
         float var1 = this.getSpeed2D();
         return this.isEngineRunning() ? var1 > 0.05F : var1 > 1.0F;
      }
   }

   public boolean shouldCollideWithObjects() {
      return this.vehicleTowedBy != null ? this.vehicleTowedBy.shouldCollideWithObjects() : this.isEngineRunning();
   }

   public void brekingObjects() {
      boolean var1 = this.shouldCollideWithCharacters();
      boolean var2 = this.shouldCollideWithObjects();
      if (var1 || var2) {
         Vector3f var3 = this.script.getExtents();
         Vector2 var4 = (Vector2)((Vector2ObjectPool)TL_vector2_pool.get()).alloc();
         float var5 = Math.max(var3.x / 2.0F, var3.z / 2.0F) + 0.3F + 1.0F;
         int var6 = (int)Math.ceil((double)var5);

         int var8;
         for(int var7 = -var6; var7 < var6; ++var7) {
            for(var8 = -var6; var8 < var6; ++var8) {
               IsoGridSquare var9 = this.getCell().getGridSquare((double)(this.getX() + (float)var8), (double)(this.getY() + (float)var7), (double)this.getZ());
               if (var9 != null) {
                  int var10;
                  if (var2) {
                     for(var10 = 0; var10 < var9.getObjects().size(); ++var10) {
                        IsoObject var11 = (IsoObject)var9.getObjects().get(var10);
                        if (!(var11 instanceof IsoWorldInventoryObject)) {
                           Vector2 var12 = null;
                           if (!this.brekingObjectsList.contains(var11) && var11 != null && var11.getProperties() != null) {
                              if (var11.getProperties().Is("CarSlowFactor")) {
                                 var12 = this.testCollisionWithObject(var11, 0.3F, var4);
                              }

                              if (var12 != null) {
                                 this.brekingObjectsList.add(var11);
                                 if (!GameClient.bClient) {
                                    var11.Collision(var12, this);
                                 }
                              }

                              if (var11.getProperties().Is("HitByCar")) {
                                 var12 = this.testCollisionWithObject(var11, 0.3F, var4);
                              }

                              if (var12 != null && !GameClient.bClient) {
                                 var11.Collision(var12, this);
                              }

                              this.checkCollisionWithPlant(var9, var11, var4);
                           }
                        }
                     }
                  }

                  IsoMovingObject var17;
                  if (var1) {
                     for(var10 = 0; var10 < var9.getMovingObjects().size(); ++var10) {
                        var17 = (IsoMovingObject)var9.getMovingObjects().get(var10);
                        IsoZombie var19 = (IsoZombie)Type.tryCastTo(var17, IsoZombie.class);
                        if (var19 != null) {
                           if (var19.isProne()) {
                              this.testCollisionWithProneCharacter(var19, false);
                           }

                           var19.setVehicle4TestCollision(this);
                        }

                        IsoAnimal var13 = (IsoAnimal)Type.tryCastTo(var17, IsoAnimal.class);
                        if (var13 != null) {
                           var13.setVehicle4TestCollision(this);
                        }

                        if (var17 instanceof IsoPlayer && var17 != this.getDriver()) {
                           IsoPlayer var14 = (IsoPlayer)var17;
                           var14.setVehicle4TestCollision(this);
                        }
                     }
                  }

                  if (var2) {
                     for(var10 = 0; var10 < var9.getStaticMovingObjects().size(); ++var10) {
                        var17 = (IsoMovingObject)var9.getStaticMovingObjects().get(var10);
                        IsoDeadBody var20 = (IsoDeadBody)Type.tryCastTo(var17, IsoDeadBody.class);
                        if (var20 != null) {
                           this.testCollisionWithCorpse(var20, true);
                        }
                     }
                  }
               }
            }
         }

         float var15 = -999.0F;

         for(var8 = 0; var8 < this.brekingObjectsList.size(); ++var8) {
            IsoObject var16 = (IsoObject)this.brekingObjectsList.get(var8);
            Vector2 var18 = this.testCollisionWithObject(var16, 1.0F, var4);
            if (var18 != null && var16.getSquare().getObjects().contains(var16)) {
               if (var15 < var16.GetVehicleSlowFactor(this)) {
                  var15 = var16.GetVehicleSlowFactor(this);
               }
            } else {
               this.brekingObjectsList.remove(var16);
               var16.UnCollision(this);
            }
         }

         if (var15 != -999.0F) {
            this.brekingSlowFactor = PZMath.clamp(var15, 0.0F, 34.0F);
         } else {
            this.brekingSlowFactor = 0.0F;
         }

         ((Vector2ObjectPool)TL_vector2_pool.get()).release(var4);
      }
   }

   private void updateVelocityMultiplier() {
      if (this.physics != null && this.getScript() != null) {
         Vector3f var1 = this.getLinearVelocity((Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
         var1.y = 0.0F;
         float var2 = var1.length();
         float var3 = 100000.0F;
         float var4 = 1.0F;
         if (this.getScript().getWheelCount() > 0) {
            if (var2 > 0.0F && var2 > 34.0F - this.brekingSlowFactor) {
               var3 = 34.0F - this.brekingSlowFactor;
               var4 = (34.0F - this.brekingSlowFactor) / var2;
            }
         } else if (this.getVehicleTowedBy() == null) {
            var3 = 0.0F;
            var4 = 0.1F;
         }

         Bullet.setVehicleVelocityMultiplier(this.VehicleID, var3, var4);
         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var1);
      }
   }

   private void playScrapePastPlantSound(IsoGridSquare var1) {
      if (this.emitter != null && !this.emitter.isPlaying(this.soundScrapePastPlant)) {
         this.emitter.setPos((float)var1.x + 0.5F, (float)var1.y + 0.5F, (float)var1.z);
         this.soundScrapePastPlant = this.emitter.playSoundImpl("VehicleScrapePastPlant", var1);
      }

   }

   private void checkCollisionWithPlant(IsoGridSquare var1, IsoObject var2, Vector2 var3) {
      IsoTree var4 = (IsoTree)Type.tryCastTo(var2, IsoTree.class);
      if (var4 != null || var2.isBush()) {
         float var5 = Math.abs(this.getCurrentSpeedKmHour());
         if (!(var5 <= 1.0F)) {
            Vector2 var6 = this.testCollisionWithObject(var2, 0.3F, var3);
            if (var6 != null) {
               if (var4 != null && var4.getSize() == 1) {
                  this.ApplyImpulse4Break(var2, 0.025F);
                  this.playScrapePastPlantSound(var1);
               } else if (this.isPositionOnLeftOrRight(var6.x, var6.y)) {
                  this.ApplyImpulse4Break(var2, 0.025F);
                  this.playScrapePastPlantSound(var1);
               } else if (var5 < 10.0F) {
                  this.ApplyImpulse4Break(var2, 0.025F);
                  this.playScrapePastPlantSound(var1);
               } else {
                  this.ApplyImpulse4Break(var2, 0.1F);
                  this.playScrapePastPlantSound(var1);
               }
            }
         }
      }
   }

   public void damageObjects(float var1) {
      if (this.isEngineRunning()) {
         Vector3f var2 = this.script.getExtents();
         Vector2 var3 = (Vector2)((Vector2ObjectPool)TL_vector2_pool.get()).alloc();
         float var4 = Math.max(var2.x / 2.0F, var2.z / 2.0F) + 0.3F + 1.0F;
         int var5 = (int)Math.ceil((double)var4);

         for(int var6 = -var5; var6 < var5; ++var6) {
            for(int var7 = -var5; var7 < var5; ++var7) {
               IsoGridSquare var8 = this.getCell().getGridSquare((double)(this.getX() + (float)var7), (double)(this.getY() + (float)var6), (double)this.getZ());
               if (var8 != null) {
                  for(int var9 = 0; var9 < var8.getObjects().size(); ++var9) {
                     IsoObject var10 = (IsoObject)var8.getObjects().get(var9);
                     Vector2 var11 = null;
                     if (var10 instanceof IsoTree) {
                        var11 = this.testCollisionWithObject(var10, 2.0F, var3);
                        if (var11 != null) {
                           var10.setRenderEffect(RenderEffectType.Hit_Tree_Shudder);
                        }
                     }

                     if (var11 == null && var10 instanceof IsoWindow) {
                        var11 = this.testCollisionWithObject(var10, 1.0F, var3);
                     }

                     if (var11 == null && var10.sprite != null && (var10.sprite.getProperties().Is("HitByCar") || var10.sprite.getProperties().Is("CarSlowFactor"))) {
                        var11 = this.testCollisionWithObject(var10, 1.0F, var3);
                     }

                     IsoGridSquare var12;
                     if (var11 == null) {
                        var12 = this.getCell().getGridSquare((double)(this.getX() + (float)var7), (double)(this.getY() + (float)var6), 1.0);
                        if (var12 != null && var12.getHasTypes().isSet(IsoObjectType.lightswitch)) {
                           var11 = this.testCollisionWithObject(var10, 1.0F, var3);
                        }
                     }

                     if (var11 == null) {
                        var12 = this.getCell().getGridSquare((double)(this.getX() + (float)var7), (double)(this.getY() + (float)var6), 0.0);
                        if (var12 != null && var12.getHasTypes().isSet(IsoObjectType.lightswitch)) {
                           var11 = this.testCollisionWithObject(var10, 1.0F, var3);
                        }
                     }

                     if (var11 != null) {
                        var10.Hit(var11, this, var1);
                     }
                  }
               }
            }
         }

         ((Vector2ObjectPool)TL_vector2_pool.get()).release(var3);
      }
   }

   public void update() {
      if (!this.removedFromWorld) {
         if (!this.getCell().vehicles.contains(this)) {
            this.getCell().getRemoveList().add(this);
         } else {
            if (this.chunk != null) {
               if (!this.chunk.vehicles.contains(this)) {
                  if (GameClient.bClient) {
                     VehicleManager.instance.sendRequestGetPosition(this.VehicleID, PacketTypes.PacketType.VehiclesUnreliable);
                  }
               } else if (!GameServer.bServer && this.chunk.refs.isEmpty()) {
                  this.removeFromWorld();
                  return;
               }
            }

            super.update();
            if (this.timeSinceLastAuth > 0.0F) {
               --this.timeSinceLastAuth;
            }

            boolean var1;
            if (!GameClient.bClient) {
               var1 = this.updateAnimal.Check();

               for(int var2 = this.getAnimals().size() - 1; var2 >= 0; --var2) {
                  IsoAnimal var3 = (IsoAnimal)this.getAnimals().get(var2);
                  var3.setX(this.getX());
                  var3.setY(this.getY());
                  var3.setZ((float)PZMath.fastfloor(this.getZ()));
                  var3.update();
                  if (this.getAnimals().contains(var3)) {
                     if (!var3.isDead()) {
                        var3.updateVocalProperties();
                     }

                     this.setNeedPartsUpdate(true);
                     if (var1) {
                        var3.getNetworkCharacterAI().getAnimalPacket().reset(var3);
                        AnimalSynchronizationManager.getInstance().setReceived(var3.OnlineID);
                     }
                  }
               }
            } else {
               for(int var18 = 0; var18 < this.getAnimals().size(); ++var18) {
                  IsoAnimal var20 = (IsoAnimal)this.getAnimals().get(var18);
                  var20.setX(this.getX());
                  var20.setY(this.getY());
                  var20.setZ((float)PZMath.fastfloor(this.getZ()));
                  AnimalInstanceManager.getInstance().update(var20);
               }
            }

            if (GameClient.bClient || GameServer.bServer) {
               this.isReliable = this.physicReliableLimit.Check();
            }

            if (GameClient.bClient && this.hasAuthorization(GameClient.connection)) {
               this.updatePhysicsNetwork();
            }

            int var6;
            int var7;
            VehiclePart var8;
            float var19;
            float var34;
            int var35;
            if (this.getVehicleTowing() != null && this.getDriver() != null) {
               var19 = 2.5F;
               if (this.getVehicleTowing().getPartCount() == 0) {
                  var19 = 12.0F;
               }

               if (this.getVehicleTowing().scriptName.equals("Base.Trailer")) {
                  VehiclePart var21 = this.getVehicleTowing().getPartById("TrailerTrunk");
                  if (this.getCurrentSpeedKmHour() > 30.0F && (float)var21.getCondition() < 50.0F && !var21.container.Items.isEmpty()) {
                     ArrayList var23 = new ArrayList();

                     int var4;
                     for(var4 = 0; var4 < var21.container.Items.size(); ++var4) {
                        if ((double)((InventoryItem)var21.container.Items.get(var4)).getWeight() >= 3.5) {
                           var23.add((InventoryItem)var21.container.Items.get(var4));
                        }
                     }

                     if (!var23.isEmpty()) {
                        var4 = var21.getCondition();
                        int var5 = 0;
                        var6 = 0;

                        for(var7 = 0; var7 < this.getVehicleTowing().parts.size(); ++var7) {
                           var8 = this.getVehicleTowing().getPartByIndex(var7);
                           if (var8 != null && var8.item != null) {
                              if (var8.partId != null && var8.partId.contains("Suspension")) {
                                 var5 += var8.getCondition();
                              } else if (var8.partId != null && var8.partId.contains("Tire")) {
                                 var6 += var8.getCondition();
                              }
                           }
                        }

                        var34 = this.parameterVehicleSteer.getCurrentValue();
                        var35 = (int)(Math.pow((double)(100 - var4 * 2), 2.0) * 0.3 * (1.0 + (double)(100 - var5 / 2) * 0.005) * (1.0 + (double)(100 - var5 / 2) * 0.005) * (double)(1.0F + var34 / 3.0F));
                        if (Rand.Next(0, Math.max(10000 - var35, 1)) == 0) {
                           InventoryItem var9 = (InventoryItem)var23.get(Rand.Next(0, var23.size()));
                           var9.setCondition(var9.getCondition() - var9.getConditionMax() / 10, false);
                           var21.getSquare().AddWorldInventoryItem(var9, Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
                           var21.container.Items.remove(var9);
                           var21.getSquare().playSound("thumpa2");
                        }
                     }
                  }
               }
            }

            if (this.getVehicleTowedBy() != null && this.getDriver() != null) {
               var19 = 2.5F;
               if (this.getVehicleTowedBy().getPartCount() == 0) {
                  var19 = 12.0F;
               }
            }

            if (this.physics != null && this.vehicleTowingID != -1 && this.vehicleTowing == null) {
               this.tryReconnectToTowedVehicle();
            }

            var1 = false;
            boolean var22 = false;
            if (this.getVehicleTowedBy() != null && this.getVehicleTowedBy().getController() != null) {
               var1 = this.getVehicleTowedBy() != null && this.getVehicleTowedBy().getController().isEnable;
               var22 = this.getVehicleTowing() != null && this.getVehicleTowing().getDriver() != null;
            }

            IsoGridSquare var47;
            if (this.physics != null) {
               boolean var24 = this.getDriver() != null || var1 || var22;
               long var26 = System.currentTimeMillis();
               if (this.constraintChangedTime != -1L) {
                  if (this.constraintChangedTime + 3500L < var26) {
                     this.constraintChangedTime = -1L;
                     if (!var24 && this.physicActiveCheck < var26) {
                        this.setPhysicsActive(false);
                     }
                  }
               } else {
                  if (this.physicActiveCheck != -1L && (var24 || !this.physics.isEnable)) {
                     this.physicActiveCheck = -1L;
                  }

                  if (!var24 && this.physics.isEnable && this.physicActiveCheck != -1L && this.physicActiveCheck < var26) {
                     this.physicActiveCheck = -1L;
                     this.setPhysicsActive(false);
                  }
               }

               if (this.getVehicleTowedBy() != null && this.getScript().getWheelCount() > 0) {
                  this.physics.updateTrailer();
               } else if (this.getDriver() == null && !GameServer.bServer) {
                  this.physics.checkShouldBeActive();
               }

               this.doAlarm();
               VehicleImpulse var33 = this.impulseFromServer;
               if (!GameServer.bServer && var33 != null && var33.enable) {
                  var33.enable = false;
                  var34 = 1.0F;
                  Bullet.applyCentralForceToVehicle(this.VehicleID, var33.impulse.x * var34, var33.impulse.y * var34, var33.impulse.z * var34);
                  Vector3f var37 = var33.rel_pos.cross(var33.impulse, (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
                  Bullet.applyTorqueToVehicle(this.VehicleID, var37.x * var34, var37.y * var34, var37.z * var34);
                  ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var37);
               }

               short var36 = 1000;
               int var11;
               if (System.currentTimeMillis() - this.engineCheckTime > (long)var36 && !GameClient.bClient) {
                  this.engineCheckTime = System.currentTimeMillis();
                  if (!GameClient.bClient) {
                     if (this.engineState != BaseVehicle.engineStateTypes.Idle) {
                        var35 = (int)((double)this.engineLoudness * this.engineSpeed / 2500.0);
                        double var39 = Math.min(this.getEngineSpeed(), 2000.0);
                        var35 = (int)((double)var35 * (1.0 + var39 / 4000.0));
                        var11 = 120;
                        if (GameServer.bServer) {
                           var11 = (int)((double)var11 * ServerOptions.getInstance().CarEngineAttractionModifier.getValue());
                           var35 = (int)((double)var35 * ServerOptions.getInstance().CarEngineAttractionModifier.getValue());
                        }

                        WorldSoundManager.WorldSound var12;
                        if (Rand.Next((int)((float)var11 * GameTime.instance.getInvMultiplier())) == 0) {
                           var12 = WorldSoundManager.instance.addSoundRepeating(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), Math.max(8, var35), Math.max(6, var35 / 3), false);
                           var12.stressAnimals = true;
                        }

                        if (Rand.Next((int)((float)(var11 - 85) * GameTime.instance.getInvMultiplier())) == 0) {
                           var12 = WorldSoundManager.instance.addSoundRepeating(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), Math.max(8, var35 / 2), Math.max(6, var35 / 3), false);
                           var12.stressAnimals = true;
                        }

                        if (Rand.Next((int)((float)(var11 - 110) * GameTime.instance.getInvMultiplier())) == 0) {
                           var12 = WorldSoundManager.instance.addSoundRepeating(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), Math.max(8, var35 / 4), Math.max(6, var35 / 3), false);
                           var12.stressAnimals = true;
                        }

                        var12 = WorldSoundManager.instance.addSoundRepeating(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), Math.max(8, var35 / 6), Math.max(6, var35 / 3), false);
                        var12.stressAnimals = true;
                     }

                     if (this.lightbarSirenMode.isEnable() && this.getBatteryCharge() > 0.0F && SandboxOptions.instance.SirenEffectsZombies.getValue()) {
                        WorldSoundManager.WorldSound var46 = WorldSoundManager.instance.addSoundRepeating(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), 100, 60, false);
                        var46.stressAnimals = true;
                     }
                  }

                  if (this.engineState == BaseVehicle.engineStateTypes.Running && !this.isEngineWorking()) {
                     this.shutOff();
                     this.checkVehicleFailsToStartWithZombiesTargeting();
                  }

                  if (this.engineState == BaseVehicle.engineStateTypes.Running) {
                     var8 = this.getPartById("Engine");
                     if (var8 != null && var8.getCondition() < 50 && Rand.Next(Rand.AdjustForFramerate(var8.getCondition() * 12)) == 0) {
                        this.shutOff();
                        this.checkVehicleFailsToStartWithZombiesTargeting();
                     }
                  }

                  if (this.engineState == BaseVehicle.engineStateTypes.Starting) {
                     this.updateEngineStarting();
                  }

                  if (this.engineState == BaseVehicle.engineStateTypes.RetryingStarting && System.currentTimeMillis() - this.engineLastUpdateStateTime > 10L) {
                     this.engineDoStarting();
                  }

                  if (this.engineState == BaseVehicle.engineStateTypes.StartingSuccess && System.currentTimeMillis() - this.engineLastUpdateStateTime > 500L) {
                     this.engineDoRunning();
                  }

                  if (this.engineState == BaseVehicle.engineStateTypes.StartingFailed && System.currentTimeMillis() - this.engineLastUpdateStateTime > 500L) {
                     this.engineDoIdle();
                  }

                  if (this.engineState == BaseVehicle.engineStateTypes.StartingFailedNoPower && System.currentTimeMillis() - this.engineLastUpdateStateTime > 500L) {
                     this.engineDoIdle();
                  }

                  if (this.engineState == BaseVehicle.engineStateTypes.Stalling && System.currentTimeMillis() - this.engineLastUpdateStateTime > 3000L) {
                     this.engineDoIdle();
                  }

                  if (this.engineState == BaseVehicle.engineStateTypes.ShutingDown && System.currentTimeMillis() - this.engineLastUpdateStateTime > 2000L) {
                     this.engineDoIdle();
                  }
               }

               if (this.getDriver() == null && !var1) {
                  this.getController().park();
               }

               this.setX(this.jniTransform.origin.x + WorldSimulation.instance.offsetX);
               this.setY(this.jniTransform.origin.z + WorldSimulation.instance.offsetY);
               this.setZ(0.0F);
               var35 = PZMath.fastfloor(this.jniTransform.origin.y / 2.44949F + 0.05F);
               IsoGridSquare var40 = this.getCell().getGridSquare((double)this.getX(), (double)this.getY(), (double)var35);
               IsoGridSquare var10 = this.getCell().getGridSquare((double)this.getX(), (double)this.getY(), (double)(var35 - 1));
               if (var40 != null && (var40.getFloor() != null || var10 != null && var10.getFloor() != null)) {
                  this.setZ((float)var35);
               }

               var47 = this.getCell().getGridSquare((double)this.getX(), (double)this.getY(), (double)this.getZ());
               if (var47 == null && !this.chunk.refs.isEmpty()) {
                  float var41 = 5.0E-4F;
                  int var42 = this.chunk.wx * 8;
                  var11 = this.chunk.wy * 8;
                  int var49 = var42 + 8;
                  int var13 = var11 + 8;
                  float var14 = this.getX();
                  float var15 = this.getY();
                  this.setX(Math.max(this.getX(), (float)var42 + var41));
                  this.setX(Math.min(this.getX(), (float)var49 - var41));
                  this.setY(Math.max(this.getY(), (float)var11 + var41));
                  this.setY(Math.min(this.getY(), (float)var13 - var41));
                  this.setZ(0.2F);
                  Transform var16 = allocTransform();
                  Transform var17 = allocTransform();
                  this.getWorldTransform(var16);
                  var17.basis.set(var16.basis);
                  var17.origin.set(this.getX() - WorldSimulation.instance.offsetX, this.getZ(), this.getY() - WorldSimulation.instance.offsetY);
                  this.setWorldTransform(var17);
                  releaseTransform(var16);
                  releaseTransform(var17);
                  this.current = this.getCell().getGridSquare((double)this.getX(), (double)this.getY(), (double)PZMath.floor(this.getZ()));
               }

               if (this.current != null && this.current.chunk != null) {
                  if (this.current.getChunk() != this.chunk) {
                     assert this.chunk.vehicles.contains(this);

                     this.chunk.vehicles.remove(this);
                     this.chunk = this.current.getChunk();

                     assert !this.chunk.vehicles.contains(this);

                     this.chunk.vehicles.add(this);
                     IsoChunk.addFromCheckedVehicles(this);
                  }
               } else {
                  boolean var43 = false;
               }

               this.updateTransform();
               Vector3f var44 = allocVector3f().set(this.jniLinearVelocity);
               if (this.jniIsCollide && this.limitCrash.Check()) {
                  this.jniIsCollide = false;
                  this.limitCrash.Reset();
                  Vector3f var45 = allocVector3f();
                  var45.set(var44).sub(this.lastLinearVelocity);
                  var45.y = 0.0F;
                  float var48 = var45.length();
                  if (var44.lengthSquared() > this.lastLinearVelocity.lengthSquared() && var48 > 6.0F) {
                     DebugLog.Vehicle.trace("Vehicle vid=%d got sharp speed increase delta=%f", this.VehicleID, var48);
                     var48 = 6.0F;
                  }

                  if (var48 > 1.0F) {
                     if (this.lastLinearVelocity.length() < 6.0F) {
                        var48 /= 3.0F;
                     }

                     DebugLog.Vehicle.trace("Vehicle vid=%d crash delta=%f", this.VehicleID, var48);
                     Vector3f var51 = this.getForwardVector(allocVector3f());
                     float var50 = var45.dot(var51);
                     releaseVector3f(var51);
                     this.crash(var48 * 3.0F, var50 < 0.0F);
                     this.damageObjects(var48 * 30.0F);
                  }

                  releaseVector3f(var45);
               }

               this.lastLinearVelocity.set(var44);
               releaseVector3f(var44);
            }

            if (this.soundHornOn && this.hornemitter != null) {
               this.hornemitter.setPos(this.getX(), this.getY(), this.getZ());
            }

            int var25;
            for(var25 = 0; var25 < this.impulseFromSquishedZombie.length; ++var25) {
               VehicleImpulse var27 = this.impulseFromSquishedZombie[var25];
               if (var27 != null) {
                  var27.enable = false;
               }
            }

            this.updateSounds();
            this.brekingObjects();
            if (this.bAddThumpWorldSound) {
               this.bAddThumpWorldSound = false;
               WorldSoundManager.instance.addSound(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), 20, 20, true);
            }

            if (this.script.getLightbar().enable && this.lightbarLightsMode.isEnable() && this.getBatteryCharge() > 0.0F) {
               this.lightbarLightsMode.update();
            }

            this.updateWorldLights();

            for(var25 = 0; var25 < IsoPlayer.numPlayers; ++var25) {
               if (this.current == null || !this.current.lighting[var25].bCanSee()) {
                  this.setTargetAlpha(var25, 0.0F);
               }

               IsoPlayer var28 = IsoPlayer.players[var25];
               if (var28 != null && this.DistToSquared(var28) < 225.0F) {
                  this.setTargetAlpha(var25, 1.0F);
               }
            }

            for(var25 = 0; var25 < this.getScript().getPassengerCount(); ++var25) {
               if (this.getCharacter(var25) != null) {
                  Vector3f var31 = this.getPassengerWorldPos(var25, (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
                  this.getCharacter(var25).setX(var31.x);
                  this.getCharacter(var25).setY(var31.y);
                  this.getCharacter(var25).setZ(var31.z * 1.0F);
                  ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var31);
               }
            }

            VehiclePart var30 = this.getPartById("lightbar");
            if (var30 != null && this.lightbarLightsMode.isEnable() && var30.getCondition() == 0 && !GameClient.bClient) {
               this.setLightbarLightsMode(0);
            }

            if (var30 != null && this.lightbarSirenMode.isEnable() && var30.getCondition() == 0 && !GameClient.bClient) {
               this.setLightbarSirenMode(0);
            }

            if (!this.needPartsUpdate() && !this.isMechanicUIOpen()) {
               this.drainBatteryUpdateHack();
            } else {
               this.updateParts();
            }

            if (this.engineState == BaseVehicle.engineStateTypes.Running || var1) {
               this.updateBulletStats();
            }

            if (this.bDoDamageOverlay) {
               this.bDoDamageOverlay = false;
               this.doDamageOverlay();
            }

            if (GameClient.bClient) {
               this.checkPhysicsValidWithServer();
            }

            VehiclePart var32 = this.getPartById("GasTank");
            if (var32 != null && var32.getContainerContentAmount() > (float)var32.getContainerCapacity()) {
               var32.setContainerContentAmount((float)var32.getContainerCapacity());
            }

            boolean var29 = false;

            for(var6 = 0; var6 < this.getMaxPassengers(); ++var6) {
               Passenger var38 = this.getPassenger(var6);
               if (var38.character != null) {
                  var29 = true;
                  break;
               }
            }

            if (var29) {
               this.m_surroundVehicle.update();
            }

            if (!this.notKillCrops() && this.getSquare() != null) {
               for(var6 = -1; var6 < 1; ++var6) {
                  for(var7 = -1; var7 < 1; ++var7) {
                     var47 = IsoWorld.instance.CurrentCell.getGridSquare(this.getSquare().getX() + var6, this.getSquare().getY() + var7, this.getSquare().getZ());
                     if (var47 != null) {
                        var47.checkForIntersectingCrops(this);
                     }
                  }
               }
            }

            if (!GameServer.bServer) {
               if (this.physics != null) {
                  Bullet.setVehicleMass(this.VehicleID, this.getFudgedMass());
               }

               this.updateVelocityMultiplier();
            }

         }
      }
   }

   private void updateEngineStarting() {
      if (this.getBatteryCharge() <= 0.1F) {
         this.engineDoStartingFailedNoPower();
      } else {
         VehiclePart var1 = this.getPartById("GasTank");
         if (var1 != null && var1.getContainerContentAmount() <= 0.0F) {
            this.engineDoStartingFailed();
         } else {
            int var2 = 0;
            float var3 = ClimateManager.getInstance().getAirTemperatureForSquare(this.getSquare());
            if (this.engineQuality < 65 && var3 <= 2.0F) {
               var2 = Math.min((2 - (int)var3) * 2, 30);
            }

            if (!SandboxOptions.instance.VehicleEasyUse.getValue() && this.engineQuality < 100 && Rand.Next(this.engineQuality + 50 - var2) <= 30) {
               this.engineDoStartingFailed();
            } else {
               if (Rand.Next(this.engineQuality) != 0) {
                  this.engineDoStartingSuccess();
               } else {
                  this.engineDoRetryingStarting();
               }

            }
         }
      }
   }

   public void applyImpulseFromHitZombies() {
      if (!this.impulseFromHitZombie.isEmpty()) {
         if ((!GameClient.bClient || this.hasAuthorization(GameClient.connection)) && !GameServer.bServer) {
            Vector3f var7 = ((Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc()).set(0.0F, 0.0F, 0.0F);
            Vector3f var8 = ((Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc()).set(0.0F, 0.0F, 0.0F);
            Vector3f var9 = ((Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc()).set(0.0F, 0.0F, 0.0F);
            int var4 = this.impulseFromHitZombie.size();

            for(int var5 = 0; var5 < var4; ++var5) {
               VehicleImpulse var6 = (VehicleImpulse)this.impulseFromHitZombie.get(var5);
               var7.add(var6.impulse);
               var8.add(var6.rel_pos.cross(var6.impulse, var9));
               var6.release();
               var6.enable = false;
            }

            this.impulseFromHitZombie.clear();
            float var10 = 7.0F * this.getFudgedMass();
            if (var7.lengthSquared() > var10 * var10) {
               var7.mul(var10 / var7.length());
            }

            float var11 = 30.0F;
            Bullet.applyCentralForceToVehicle(this.VehicleID, var7.x * var11, var7.y * var11, var7.z * var11);
            Bullet.applyTorqueToVehicle(this.VehicleID, var8.x * var11, var8.y * var11, var8.z * var11);
            ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var7);
            ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var8);
            ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var9);
         } else {
            int var1 = 0;

            for(int var2 = this.impulseFromHitZombie.size(); var1 < var2; ++var1) {
               VehicleImpulse var3 = (VehicleImpulse)this.impulseFromHitZombie.get(var1);
               var3.release();
               var3.enable = false;
            }

            this.impulseFromHitZombie.clear();
         }
      }
   }

   public void applyImpulseFromProneCharacters() {
      if ((!GameClient.bClient || this.hasAuthorization(GameClient.connection)) && !GameServer.bServer) {
         boolean var1 = PZArrayUtil.contains((Object[])this.impulseFromSquishedZombie, (var0) -> {
            return var0 != null && var0.enable;
         });
         if (var1) {
            Vector3f var2 = ((Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc()).set(0.0F, 0.0F, 0.0F);
            Vector3f var3 = ((Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc()).set(0.0F, 0.0F, 0.0F);
            Vector3f var4 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();

            for(int var5 = 0; var5 < this.impulseFromSquishedZombie.length; ++var5) {
               VehicleImpulse var6 = this.impulseFromSquishedZombie[var5];
               if (var6 != null && var6.enable && !var6.applied) {
                  var2.add(var6.impulse);
                  var3.add(var6.rel_pos.cross(var6.impulse, var4));
                  var6.applied = true;
               }
            }

            if (var2.lengthSquared() > 0.0F) {
               float var7 = this.getFudgedMass() * 0.15F;
               if (var2.lengthSquared() > var7 * var7) {
                  var2.mul(var7 / var2.length());
               }

               float var8 = 30.0F;
               Bullet.applyCentralForceToVehicle(this.VehicleID, var2.x * var8, var2.y * var8, var2.z * var8);
               Bullet.applyTorqueToVehicle(this.VehicleID, var3.x * var8, var3.y * var8, var3.z * var8);
            }

            ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var2);
            ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var3);
            ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var4);
         }
      }
   }

   public float getFudgedMass() {
      if (this.getScriptName().contains("Trailer")) {
         return this.getMass();
      } else {
         BaseVehicle var1 = this.getVehicleTowedBy();
         if (var1 != null && var1.getDriver() != null && var1.isEngineRunning()) {
            float var2 = Math.max(250.0F, var1.getMass() / 3.7F);
            if (this.getScript().getWheelCount() == 0) {
               var2 = Math.min(var2, 200.0F);
            }

            return var2;
         } else {
            return this.getMass();
         }
      }
   }

   private boolean isNullChunk(int var1, int var2) {
      if (!IsoWorld.instance.getMetaGrid().isValidChunk(var1, var2)) {
         return false;
      } else if (GameClient.bClient && !ClientServerMap.isChunkLoaded(var1, var2)) {
         return true;
      } else if (GameClient.bClient && !PassengerMap.isChunkLoaded(this, var1, var2)) {
         return true;
      } else {
         return this.getCell().getChunk(var1, var2) == null;
      }
   }

   public boolean isInvalidChunkAround() {
      Vector3f var1 = this.getLinearVelocity((Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
      float var2 = Math.abs(var1.x);
      float var3 = Math.abs(var1.z);
      boolean var4 = var1.x < 0.0F && var2 > var3;
      boolean var5 = var1.x > 0.0F && var2 > var3;
      boolean var6 = var1.z < 0.0F && var3 > var2;
      boolean var7 = var1.z > 0.0F && var3 > var2;
      ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var1);
      return this.isInvalidChunkAround(var4, var5, var6, var7);
   }

   public boolean isInvalidChunkAhead() {
      Vector3f var1 = this.getForwardVector((Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
      boolean var2 = var1.x < -0.5F;
      boolean var3 = var1.z > 0.5F;
      boolean var4 = var1.x > 0.5F;
      boolean var5 = var1.z < -0.5F;
      return this.isInvalidChunkAround(var2, var4, var5, var3);
   }

   public boolean isInvalidChunkBehind() {
      Vector3f var1 = this.getForwardVector((Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
      boolean var2 = var1.x < -0.5F;
      boolean var3 = var1.z > 0.5F;
      boolean var4 = var1.x > 0.5F;
      boolean var5 = var1.z < -0.5F;
      return this.isInvalidChunkAround(var4, var2, var3, var5);
   }

   public boolean isInvalidChunkAround(boolean var1, boolean var2, boolean var3, boolean var4) {
      if (IsoChunkMap.ChunkGridWidth <= 7) {
         if (IsoChunkMap.ChunkGridWidth <= 4) {
            return false;
         } else if (var2 && this.isNullChunk(this.chunk.wx + 1, this.chunk.wy)) {
            return true;
         } else if (var1 && this.isNullChunk(this.chunk.wx - 1, this.chunk.wy)) {
            return true;
         } else if (var4 && this.isNullChunk(this.chunk.wx, this.chunk.wy + 1)) {
            return true;
         } else if (var3 && this.isNullChunk(this.chunk.wx, this.chunk.wy - 1)) {
            return true;
         } else {
            return false;
         }
      } else if (!var2 || !this.isNullChunk(this.chunk.wx + 1, this.chunk.wy) && !this.isNullChunk(this.chunk.wx + 2, this.chunk.wy)) {
         if (!var1 || !this.isNullChunk(this.chunk.wx - 1, this.chunk.wy) && !this.isNullChunk(this.chunk.wx - 2, this.chunk.wy)) {
            if (var4 && (this.isNullChunk(this.chunk.wx, this.chunk.wy + 1) || this.isNullChunk(this.chunk.wx, this.chunk.wy + 2))) {
               return true;
            } else if (!var3 || !this.isNullChunk(this.chunk.wx, this.chunk.wy - 1) && !this.isNullChunk(this.chunk.wx, this.chunk.wy - 2)) {
               return false;
            } else {
               return true;
            }
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   public void postupdate() {
      int var1 = PZMath.fastfloor(this.getZ());
      this.current = this.getCell().getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), var1);
      int var2;
      if (this.current == null) {
         for(var2 = PZMath.fastfloor(this.getZ()); var2 >= 0; --var2) {
            this.current = this.getCell().getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor((float)var2));
            if (this.current != null) {
               break;
            }
         }
      }

      if (this.movingSq != null) {
         this.movingSq.getMovingObjects().remove(this);
         this.movingSq = null;
      }

      if (this.current != null && !this.current.getMovingObjects().contains(this)) {
         this.current.getMovingObjects().add(this);
         this.movingSq = this.current;
      }

      this.square = this.current;
      if (this.sprite.hasActiveModel()) {
         this.updateAnimationPlayer(this.getAnimationPlayer(), (VehiclePart)null);

         for(var2 = 0; var2 < this.models.size(); ++var2) {
            ModelInfo var3 = (ModelInfo)this.models.get(var2);
            this.updateAnimationPlayer(var3.getAnimationPlayer(), var3.part);
         }
      }

   }

   protected void updateAnimationPlayer(AnimationPlayer var1, VehiclePart var2) {
      if (var1 != null && var1.isReady()) {
         AnimationMultiTrack var3 = var1.getMultiTrack();
         float var4 = 0.016666668F;
         var4 *= 0.8F;
         var4 *= GameTime.instance.getUnmoddedMultiplier();
         var1.Update(var4);

         for(int var5 = 0; var5 < var3.getTrackCount(); ++var5) {
            AnimationTrack var6 = (AnimationTrack)var3.getTracks().get(var5);
            if (var6.IsPlaying && var6.isFinished()) {
               var3.removeTrackAt(var5);
               --var5;
            }
         }

         if (var2 != null) {
            ModelInfo var8 = this.getModelInfoForPart(var2);
            if (var8.m_track != null && var3.getIndexOfTrack(var8.m_track) == -1) {
               var8.m_track = null;
            }

            if (var8.m_track != null) {
               VehicleWindow var10 = var2.getWindow();
               if (var10 != null) {
                  AnimationTrack var11 = var8.m_track;
                  var11.setCurrentTimeValue(var11.getDuration() * var10.getOpenDelta());
               }

            } else {
               VehicleDoor var9 = var2.getDoor();
               if (var9 != null) {
                  this.playPartAnim(var2, var9.isOpen() ? "Opened" : "Closed");
               }

               VehicleWindow var7 = var2.getWindow();
               if (var7 != null) {
                  this.playPartAnim(var2, "ClosedToOpen");
               }

            }
         }
      }
   }

   public void saveChange(String var1, KahluaTable var2, ByteBuffer var3) {
      super.saveChange(var1, var2, var3);
   }

   public void loadChange(String var1, ByteBuffer var2) {
      super.loadChange(var1, var2);
   }

   public void authorizationClientCollide(IsoPlayer var1) {
      if (var1 != null && this.getDriver() == null) {
         this.setNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide, var1.getOnlineID());
         this.authSimulationTime = System.currentTimeMillis();
         this.interpolation.clear();
         if (this.getVehicleTowing() != null) {
            this.getVehicleTowing().setNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide, var1.getOnlineID());
            this.getVehicleTowing().authSimulationTime = System.currentTimeMillis();
            this.getVehicleTowing().interpolation.clear();
         } else if (this.getVehicleTowedBy() != null) {
            this.getVehicleTowedBy().setNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide, var1.getOnlineID());
            this.getVehicleTowedBy().authSimulationTime = System.currentTimeMillis();
            this.getVehicleTowedBy().interpolation.clear();
         }
      }

   }

   public void authorizationServerCollide(short var1, boolean var2) {
      if (!this.isNetPlayerAuthorization(BaseVehicle.Authorization.Local)) {
         if (var2) {
            this.setNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide, var1);
            if (this.getVehicleTowing() != null) {
               this.getVehicleTowing().setNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide, var1);
            } else if (this.getVehicleTowedBy() != null) {
               this.getVehicleTowedBy().setNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide, var1);
            }
         } else {
            Authorization var3 = var1 == -1 ? BaseVehicle.Authorization.Server : BaseVehicle.Authorization.Local;
            this.setNetPlayerAuthorization(var3, var1);
            if (this.getVehicleTowing() != null) {
               this.getVehicleTowing().setNetPlayerAuthorization(var3, var1);
            } else if (this.getVehicleTowedBy() != null) {
               this.getVehicleTowedBy().setNetPlayerAuthorization(var3, var1);
            }
         }

      }
   }

   public void authorizationServerOnSeat(IsoPlayer var1, boolean var2) {
      BaseVehicle var3 = this.getVehicleTowing();
      BaseVehicle var4 = this.getVehicleTowedBy();
      if (this.isNetPlayerId((short)-1) && var2) {
         if (var3 != null && var3.getDriver() == null) {
            this.addPointConstraint((IsoPlayer)null, var3, this.getTowAttachmentSelf(), var3.getTowAttachmentSelf());
         } else if (var4 != null && var4.getDriver() == null) {
            this.addPointConstraint((IsoPlayer)null, var4, this.getTowAttachmentSelf(), var4.getTowAttachmentSelf());
         } else {
            this.setNetPlayerAuthorization(BaseVehicle.Authorization.Local, var1.getOnlineID());
         }
      } else if (this.isNetPlayerId(var1.getOnlineID()) && !var2) {
         if (var3 != null && var3.getDriver() != null) {
            var3.addPointConstraint((IsoPlayer)null, this, var3.getTowAttachmentSelf(), this.getTowAttachmentSelf());
         } else if (var4 != null && var4.getDriver() != null) {
            var4.addPointConstraint((IsoPlayer)null, this, var4.getTowAttachmentSelf(), this.getTowAttachmentSelf());
         } else {
            this.setNetPlayerAuthorization(BaseVehicle.Authorization.Server, -1);
            if (var3 != null) {
               var3.setNetPlayerAuthorization(BaseVehicle.Authorization.Server, -1);
            } else if (var4 != null) {
               var4.setNetPlayerAuthorization(BaseVehicle.Authorization.Server, -1);
            }
         }
      }

   }

   public boolean hasAuthorization(UdpConnection var1) {
      if (!this.isNetPlayerId((short)-1) && var1 != null) {
         if (GameServer.bServer) {
            for(int var2 = 0; var2 < var1.players.length; ++var2) {
               if (var1.players[var2] != null && this.isNetPlayerId(var1.players[var2].OnlineID)) {
                  return true;
               }
            }

            return false;
         } else {
            return this.isNetPlayerId(IsoPlayer.getInstance().getOnlineID());
         }
      } else {
         return false;
      }
   }

   public void netPlayerServerSendAuthorisation(ByteBuffer var1) {
      var1.put(this.netPlayerAuthorization.index());
      var1.putShort(this.netPlayerId);
   }

   public void netPlayerFromServerUpdate(Authorization var1, short var2) {
      if (!this.isNetPlayerAuthorization(var1) || !this.isNetPlayerId(var2)) {
         if (BaseVehicle.Authorization.Local.equals(var1)) {
            if (IsoPlayer.getLocalPlayerByOnlineID(var2) != null) {
               this.setNetPlayerAuthorization(BaseVehicle.Authorization.Local, var2);
            } else {
               this.setNetPlayerAuthorization(BaseVehicle.Authorization.Remote, var2);
            }
         } else if (BaseVehicle.Authorization.LocalCollide.equals(var1)) {
            if (IsoPlayer.getLocalPlayerByOnlineID(var2) != null) {
               this.setNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide, var2);
            } else {
               this.setNetPlayerAuthorization(BaseVehicle.Authorization.RemoteCollide, var2);
            }
         } else {
            this.setNetPlayerAuthorization(BaseVehicle.Authorization.Server, -1);
         }

      }
   }

   public Transform getWorldTransform(Transform var1) {
      var1.set(this.jniTransform);
      return var1;
   }

   public void setWorldTransform(Transform var1) {
      this.jniTransform.set(var1);
      Quaternionf var2 = allocQuaternionf();
      var1.getRotation(var2);
      if (!GameServer.bServer) {
         Bullet.teleportVehicle(this.VehicleID, var1.origin.x + WorldSimulation.instance.offsetX, var1.origin.z + WorldSimulation.instance.offsetY, var1.origin.y, var2.x, var2.y, var2.z, var2.w);
      }

      releaseQuaternionf(var2);
   }

   public void flipUpright() {
      Transform var1 = allocTransform();
      var1.set(this.jniTransform);
      Quaternionf var2 = allocQuaternionf();
      var2.setAngleAxis(0.0F, _UNIT_Y.x, _UNIT_Y.y, _UNIT_Y.z);
      var1.setRotation(var2);
      releaseQuaternionf(var2);
      this.setWorldTransform(var1);
      releaseTransform(var1);
   }

   public void setAngles(float var1, float var2, float var3) {
      if ((int)var1 != (int)this.getAngleX() || (int)var2 != (int)this.getAngleY() || var3 != (float)((int)this.getAngleZ())) {
         this.polyDirty = true;
         float var4 = var1 * 0.017453292F;
         float var5 = var2 * 0.017453292F;
         float var6 = var3 * 0.017453292F;
         Quaternionf var7 = allocQuaternionf();
         var7.rotationXYZ(var4, var5, var6);
         Transform var8 = allocTransform();
         var8.set(this.jniTransform);
         var8.setRotation(var7);
         releaseQuaternionf(var7);
         this.setWorldTransform(var8);
         releaseTransform(var8);
      }
   }

   public float getAngleX() {
      Vector3f var1 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
      Quaternionf var2 = allocQuaternionf();
      this.jniTransform.getRotation(var2).getEulerAnglesXYZ(var1);
      releaseQuaternionf(var2);
      float var3 = var1.x * 57.295776F;
      ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var1);
      return var3;
   }

   public float getAngleY() {
      Vector3f var1 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
      Quaternionf var2 = allocQuaternionf();
      this.jniTransform.getRotation(var2).getEulerAnglesXYZ(var1);
      releaseQuaternionf(var2);
      float var3 = var1.y * 57.295776F;
      ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var1);
      return var3;
   }

   public float getAngleZ() {
      Vector3f var1 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
      Quaternionf var2 = allocQuaternionf();
      this.jniTransform.getRotation(var2).getEulerAnglesXYZ(var1);
      releaseQuaternionf(var2);
      float var3 = var1.z * 57.295776F;
      ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var1);
      return var3;
   }

   public void setDebugZ(float var1) {
      Transform var2 = allocTransform();
      var2.set(this.jniTransform);
      int var3 = PZMath.fastfloor(this.jniTransform.origin.y / 2.44949F);
      var2.origin.y = ((float)var3 + PZMath.clamp(var1, 0.0F, 0.99F)) * 3.0F * 0.8164967F;
      this.setWorldTransform(var2);
      releaseTransform(var2);
   }

   public void setPhysicsActive(boolean var1) {
      if (this.physics != null && var1 != this.physics.isEnable) {
         this.physics.isEnable = var1;
         if (!GameServer.bServer) {
            Bullet.setVehicleActive(this.VehicleID, var1);
         }

         if (var1) {
            this.physicActiveCheck = System.currentTimeMillis() + 3000L;
         }

      }
   }

   public float getDebugZ() {
      return this.jniTransform.origin.y / 2.44949F;
   }

   public VehiclePoly getPoly() {
      if (this.polyDirty) {
         if (this.polyGarageCheck && this.square != null) {
            if (this.square.getRoom() != null && this.square.getRoom().RoomDef != null && this.square.getRoom().RoomDef.contains("garagestorage")) {
               this.radiusReductionInGarage = -0.3F;
            } else {
               this.radiusReductionInGarage = 0.0F;
            }

            this.polyGarageCheck = false;
         }

         this.poly.init(this, 0.0F);
         this.polyPlusRadius.init(this, PLUS_RADIUS + this.radiusReductionInGarage);
         this.polyDirty = false;
         this.polyPlusRadiusMinX = -123.0F;
         this.initShadowPoly();
      }

      return this.poly;
   }

   public VehiclePoly getPolyPlusRadius() {
      if (this.polyDirty) {
         if (this.polyGarageCheck && this.square != null) {
            if (this.square.getRoom() != null && this.square.getRoom().RoomDef != null && this.square.getRoom().RoomDef.contains("garagestorage")) {
               this.radiusReductionInGarage = -0.3F;
            } else {
               this.radiusReductionInGarage = 0.0F;
            }

            this.polyGarageCheck = false;
         }

         this.poly.init(this, 0.0F);
         this.polyPlusRadius.init(this, PLUS_RADIUS + this.radiusReductionInGarage);
         this.polyDirty = false;
         this.polyPlusRadiusMinX = -123.0F;
         this.initShadowPoly();
      }

      return this.polyPlusRadius;
   }

   private void initShadowPoly() {
      Transform var1 = this.getWorldTransform(allocTransform());
      Quaternionf var2 = var1.getRotation(allocQuaternionf());
      releaseTransform(var1);
      Vector2f var3 = this.script.getShadowExtents();
      Vector2f var4 = this.script.getShadowOffset();
      float var5 = var3.x / 2.0F;
      float var6 = var3.y / 2.0F;
      Vector3f var7 = allocVector3f();
      if (var2.x < 0.0F) {
         this.getWorldPos(var4.x - var5, 0.0F, var4.y + var6, var7);
         this.shadowCoord.x1 = var7.x;
         this.shadowCoord.y1 = var7.y;
         this.getWorldPos(var4.x + var5, 0.0F, var4.y + var6, var7);
         this.shadowCoord.x2 = var7.x;
         this.shadowCoord.y2 = var7.y;
         this.getWorldPos(var4.x + var5, 0.0F, var4.y - var6, var7);
         this.shadowCoord.x3 = var7.x;
         this.shadowCoord.y3 = var7.y;
         this.getWorldPos(var4.x - var5, 0.0F, var4.y - var6, var7);
         this.shadowCoord.x4 = var7.x;
         this.shadowCoord.y4 = var7.y;
      } else {
         this.getWorldPos(var4.x - var5, 0.0F, var4.y + var6, var7);
         this.shadowCoord.x1 = var7.x;
         this.shadowCoord.y1 = var7.y;
         this.getWorldPos(var4.x + var5, 0.0F, var4.y + var6, var7);
         this.shadowCoord.x2 = var7.x;
         this.shadowCoord.y2 = var7.y;
         this.getWorldPos(var4.x + var5, 0.0F, var4.y - var6, var7);
         this.shadowCoord.x3 = var7.x;
         this.shadowCoord.y3 = var7.y;
         this.getWorldPos(var4.x - var5, 0.0F, var4.y - var6, var7);
         this.shadowCoord.x4 = var7.x;
         this.shadowCoord.y4 = var7.y;
      }

      releaseVector3f(var7);
      releaseQuaternionf(var2);
   }

   private void initPolyPlusRadiusBounds() {
      if (this.polyPlusRadiusMinX == -123.0F) {
         VehiclePoly var1 = this.getPolyPlusRadius();
         Vector3f var10 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
         Vector3f var11 = this.getLocalPos(var1.x1, var1.y1, var1.z, var10);
         float var2 = (float)PZMath.fastfloor(var11.x * 100.0F) / 100.0F;
         float var3 = (float)PZMath.fastfloor(var11.z * 100.0F) / 100.0F;
         var11 = this.getLocalPos(var1.x2, var1.y2, var1.z, var10);
         float var4 = (float)PZMath.fastfloor(var11.x * 100.0F) / 100.0F;
         float var5 = (float)PZMath.fastfloor(var11.z * 100.0F) / 100.0F;
         var11 = this.getLocalPos(var1.x3, var1.y3, var1.z, var10);
         float var6 = (float)PZMath.fastfloor(var11.x * 100.0F) / 100.0F;
         float var7 = (float)PZMath.fastfloor(var11.z * 100.0F) / 100.0F;
         var11 = this.getLocalPos(var1.x4, var1.y4, var1.z, var10);
         float var8 = (float)PZMath.fastfloor(var11.x * 100.0F) / 100.0F;
         float var9 = (float)PZMath.fastfloor(var11.z * 100.0F) / 100.0F;
         this.polyPlusRadiusMinX = Math.min(var2, Math.min(var4, Math.min(var6, var8)));
         this.polyPlusRadiusMaxX = Math.max(var2, Math.max(var4, Math.max(var6, var8)));
         this.polyPlusRadiusMinY = Math.min(var3, Math.min(var5, Math.min(var7, var9)));
         this.polyPlusRadiusMaxY = Math.max(var3, Math.max(var5, Math.max(var7, var9)));
         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var10);
      }
   }

   public Vector3f getForwardVector(Vector3f var1) {
      byte var2 = 2;
      return this.jniTransform.basis.getColumn(var2, var1);
   }

   public Vector3f getUpVector(Vector3f var1) {
      byte var2 = 1;
      return this.jniTransform.basis.getColumn(var2, var1);
   }

   public float getUpVectorDot() {
      Vector3f var1 = this.getUpVector((Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
      float var2 = var1.dot(_UNIT_Y);
      ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var1);
      return var2;
   }

   public boolean isStopped() {
      return this.jniSpeed > -0.8F && this.jniSpeed < 0.8F && !this.getController().isGasPedalPressed();
   }

   public float getCurrentSpeedKmHour() {
      return this.jniSpeed;
   }

   public Vector3f getLinearVelocity(Vector3f var1) {
      return var1.set(this.jniLinearVelocity);
   }

   public float getSpeed2D() {
      float var1 = this.jniLinearVelocity.x;
      float var2 = this.jniLinearVelocity.z;
      return (float)Math.sqrt((double)(var1 * var1 + var2 * var2));
   }

   public boolean isAtRest() {
      if (this.physics == null) {
         return true;
      } else {
         float var1 = this.jniLinearVelocity.y;
         return Math.abs(this.physics.EngineForce) < 0.01F && this.getSpeed2D() < 0.02F && Math.abs(var1) < 0.5F;
      }
   }

   protected void updateTransform() {
      if (this.sprite.modelSlot != null) {
         float var1 = this.getScript().getModelScale();
         float var2 = 1.0F;
         if (this.sprite.modelSlot != null && this.sprite.modelSlot.model.scale != 1.0F) {
            var2 = this.sprite.modelSlot.model.scale;
         }

         Quaternionf var3 = (Quaternionf)((QuaternionfObjectPool)TL_quaternionf_pool.get()).alloc();
         Quaternionf var4 = (Quaternionf)((QuaternionfObjectPool)TL_quaternionf_pool.get()).alloc();
         Matrix4f var5 = (Matrix4f)((Matrix4fObjectPool)TL_matrix4f_pool.get()).alloc();
         Transform var6 = this.getWorldTransform(allocTransform());
         var6.getRotation(var3);
         releaseTransform(var6);
         var3.y *= -1.0F;
         var3.z *= -1.0F;
         Matrix4f var7 = var3.get(var5);
         float var8 = 1.0F;
         if (this.sprite.modelSlot.model.m_modelScript != null) {
            var8 = this.sprite.modelSlot.model.m_modelScript.invertX ? -1.0F : 1.0F;
         }

         Vector3f var9 = this.script.getModel().getOffset();
         Vector3f var10 = this.getScript().getModel().getRotate();
         var4.rotationXYZ(var10.x * 0.017453292F, var10.y * 0.017453292F, var10.z * 0.017453292F);
         this.renderTransform.translationRotateScale(var9.x * -1.0F, var9.y, var9.z, var4.x, var4.y, var4.z, var4.w, var1 * var2 * var8, var1 * var2, var1 * var2);
         var7.mul(this.renderTransform, this.renderTransform);
         this.vehicleTransform.translationRotateScale(var9.x * -1.0F, var9.y, var9.z, 0.0F, 0.0F, 0.0F, 1.0F, var1);
         var7.mul(this.vehicleTransform, this.vehicleTransform);

         for(int var21 = 0; var21 < this.models.size(); ++var21) {
            ModelInfo var22 = (ModelInfo)this.models.get(var21);
            VehicleScript.Model var23 = var22.scriptModel;
            var9 = var23.getOffset();
            var10 = var23.getRotate();
            float var11 = var23.scale;
            var2 = 1.0F;
            float var12 = 1.0F;
            if (var22.modelScript != null) {
               var2 = var22.modelScript.scale;
               var12 = var22.modelScript.invertX ? -1.0F : 1.0F;
            }

            byte var13 = 1;
            if (var22.wheelIndex == -1) {
               var13 = -1;
            }

            var4.rotationXYZ(var10.x * 0.017453292F, var10.y * 0.017453292F * (float)var13, var10.z * 0.017453292F * (float)var13);
            if (var22.wheelIndex == -1) {
               if (var22.part != null && var22.part.scriptPart != null && var22.part.scriptPart.parent != null && var23.attachmentNameParent != null) {
                  ModelInfo var24 = this.getModelInfoForPart(var22.part.getParent());
                  Matrix4f var25 = (Matrix4f)((Matrix4fObjectPool)TL_matrix4f_pool.get()).alloc();
                  this.initTransform(var24.modelInstance, var24.modelScript, var22.modelScript, var23.attachmentNameParent, var23.attachmentNameSelf, var25);
                  Model var26 = var24.modelInstance.model;
                  ModelInstanceRenderData.preMultiplyMeshTransform(var25, var26.Mesh);
                  var24.renderTransform.mul(var25, var22.renderTransform);
                  boolean var27 = var23.bIgnoreVehicleScale;
                  float var28 = var27 ? 1.5F / this.getScript().getModelScale() : 1.0F;
                  var22.renderTransform.scale(var11 * var2 * var28);
                  ((Matrix4fObjectPool)TL_matrix4f_pool.get()).release(var25);
               } else {
                  var22.renderTransform.translationRotateScale(var9.x * -1.0F, var9.y, var9.z, var4.x, var4.y, var4.z, var4.w, var11 * var2 * var12, var11 * var2, var11 * var2);
                  this.vehicleTransform.mul(var22.renderTransform, var22.renderTransform);
               }
            } else {
               WheelInfo var14 = this.wheelInfo[var22.wheelIndex];
               float var15 = var14.steering;
               float var16 = var14.rotation;
               VehicleScript.Wheel var17 = this.getScript().getWheel(var22.wheelIndex);
               VehicleImpulse var18 = var22.wheelIndex < this.impulseFromSquishedZombie.length ? this.impulseFromSquishedZombie[var22.wheelIndex] : null;
               float var19 = var18 != null && var18.enable ? 0.05F : 0.0F;
               if (var14.suspensionLength == 0.0F) {
                  var5.translation(var17.offset.x / var1 * -1.0F, var17.offset.y / var1, var17.offset.z / var1);
               } else {
                  var5.translation(var17.offset.x / var1 * -1.0F, (var17.offset.y + this.script.getSuspensionRestLength() - var14.suspensionLength) / var1 + var19 * 0.5F, var17.offset.z / var1);
               }

               var22.renderTransform.identity();
               var22.renderTransform.mul(var5);
               var22.renderTransform.rotateY(var15 * -1.0F);
               var22.renderTransform.rotateX(var16);
               var5.translationRotateScale(var9.x * -1.0F, var9.y, var9.z, var4.x, var4.y, var4.z, var4.w, var11 * var2 * var12, var11 * var2, var11 * var2);
               var22.renderTransform.mul(var5);
               this.vehicleTransform.mul(var22.renderTransform, var22.renderTransform);
            }
         }

         ((Matrix4fObjectPool)TL_matrix4f_pool.get()).release(var5);
         ((QuaternionfObjectPool)TL_quaternionf_pool.get()).release(var3);
         ((QuaternionfObjectPool)TL_quaternionf_pool.get()).release(var4);
      }
   }

   void initTransform(ModelInstance var1, ModelScript var2, ModelScript var3, String var4, String var5, Matrix4f var6) {
      var6.identity();
      Matrix4f var7 = (Matrix4f)((Matrix4fObjectPool)TL_matrix4f_pool.get()).alloc();
      ModelAttachment var8 = var2.getAttachmentById(var4);
      if (var8 == null) {
         var8 = this.getScript().getAttachmentById(var4);
      }

      if (var8 != null) {
         ModelInstanceRenderData.makeBoneTransform(var1.AnimPlayer, var8.getBone(), var6);
         var6.scale(1.0F / var2.scale);
         ModelInstanceRenderData.makeAttachmentTransform(var8, var7);
         var6.mul(var7);
      }

      ModelAttachment var9 = var3.getAttachmentById(var5);
      if (var9 != null) {
         ModelInstanceRenderData.makeAttachmentTransform(var9, var7);
         if (ModelInstanceRenderData.INVERT_ATTACHMENT_SELF_TRANSFORM) {
            var7.invert();
         }

         var6.mul(var7);
      }

      ((Matrix4fObjectPool)TL_matrix4f_pool.get()).release(var7);
   }

   public void updatePhysics() {
      this.physics.update();
   }

   public void updatePhysicsNetwork() {
      if (this.limitPhysicSend.Check()) {
         VehicleManager.instance.sendPhysic(this);
         if (this.limitPhysicPositionSent == null) {
            this.limitPhysicPositionSent = new Vector2();
         } else if (IsoUtils.DistanceToSquared(this.limitPhysicPositionSent.x, this.limitPhysicPositionSent.y, this.getX(), this.getY()) > 0.001F) {
            this.limitPhysicSend.setUpdatePeriod(150L);
         } else {
            this.limitPhysicSend.setSmoothUpdatePeriod(300L);
         }

         this.limitPhysicPositionSent.set(this.getX(), this.getY());
      }

   }

   public void checkPhysicsValidWithServer() {
      float var1 = 0.05F;
      if (this.limitPhysicValid.Check() && Bullet.getOwnVehiclePhysics(this.VehicleID, physicsParams) == 0) {
         float var2 = Math.abs(physicsParams[0] - this.getX());
         float var3 = Math.abs(physicsParams[1] - this.getY());
         if (var2 > var1 || var3 > var1) {
            VehicleManager.instance.sendRequestGetPosition(this.VehicleID, PacketTypes.PacketType.Vehicles);
            DebugLog.Vehicle.trace("diff-x=%f diff-y=%f delta=%f", var2, var3, var1);
         }
      }

   }

   public void updateControls() {
      if (this.getController() != null) {
         if (this.isOperational()) {
            IsoPlayer var1 = (IsoPlayer)Type.tryCastTo(this.getDriver(), IsoPlayer.class);
            if (var1 == null || !var1.isBlockMovement()) {
               this.getController().updateControls();
            }
         }
      }
   }

   public boolean isKeyboardControlled() {
      IsoGameCharacter var1 = this.getCharacter(0);
      return var1 != null && var1 == IsoPlayer.players[0] && this.getVehicleTowedBy() == null;
   }

   public int getJoypad() {
      IsoGameCharacter var1 = this.getCharacter(0);
      return var1 != null && var1 instanceof IsoPlayer ? ((IsoPlayer)var1).JoypadBind : -1;
   }

   public void Damage(float var1) {
      this.crash(var1, true);
   }

   public void HitByVehicle(BaseVehicle var1, float var2) {
      this.crash(var2, true);
   }

   public void crash(float var1, boolean var2) {
      if (GameClient.bClient) {
         SoundManager.instance.PlayWorldSound(this.getCrashSound(var1), this.square, 1.0F, 20.0F, 1.0F, true);
         GameClient.instance.sendClientCommandV((IsoPlayer)null, "vehicle", "crash", "vehicle", this.getId(), "amount", var1, "front", var2);
      } else {
         float var3 = 1.3F;
         float var4 = var1;
         switch (SandboxOptions.instance.CarDamageOnImpact.getValue()) {
            case 1:
               var3 = 1.9F;
               break;
            case 2:
               var3 = 1.6F;
            case 3:
            default:
               break;
            case 4:
               var3 = 1.1F;
               break;
            case 5:
               var3 = 0.9F;
         }

         var1 = Math.abs(var1) / var3;
         if (var2) {
            this.addDamageFront((int)var1);
         } else {
            this.addDamageRear((int)Math.abs(var1 / var3));
         }

         this.damagePlayers(Math.abs(var4));
         SoundManager.instance.PlayWorldSound(this.getCrashSound(var4), this.square, 1.0F, 20.0F, 1.0F, true);
         if (this.getVehicleTowing() != null) {
            this.getVehicleTowing().crash(var1, var2);
         }

         if (this.getAnimals() != null && !this.getAnimals().isEmpty()) {
            for(int var5 = 0; var5 < this.getAnimals().size(); ++var5) {
               ((IsoAnimal)this.getAnimals().get(var5)).carCrash(var1, var2);
            }
         }

         IsoPlayer var6 = (IsoPlayer)Type.tryCastTo(this.getDriver(), IsoPlayer.class);
         if (var6 != null && var6.isLocalPlayer()) {
            var6.triggerMusicIntensityEvent("VehicleCrash");
         }

      }
   }

   private String getCrashSound(float var1) {
      if (var1 < 5.0F) {
         return "VehicleCrash1";
      } else {
         return var1 < 30.0F ? "VehicleCrash2" : "VehicleCrash";
      }
   }

   public void addDamageFrontHitAChr(int var1) {
      if (!this.isDriverGodMode()) {
         if (var1 >= 4 || !Rand.NextBool(7)) {
            VehiclePart var2 = this.getPartById("EngineDoor");
            if (var2 != null && var2.getInventoryItem() != null) {
               var2.damage(Rand.Next(Math.max(1, var1 - 10), var1 + 3));
            }

            if (var2 != null && (var2.getCondition() <= 0 || var2.getInventoryItem() == null) && Rand.NextBool(4)) {
               var2 = this.getPartById("Engine");
               if (var2 != null) {
                  var2.damage(Rand.Next(2, 4));
               }
            }

            if (var1 > 12) {
               var2 = this.getPartById("Windshield");
               if (var2 != null && var2.getInventoryItem() != null) {
                  var2.damage(Rand.Next(Math.max(1, var1 - 10), var1 + 3));
               }
            }

            if (Rand.Next(5) < var1) {
               if (Rand.NextBool(2)) {
                  var2 = this.getPartById("TireFrontLeft");
               } else {
                  var2 = this.getPartById("TireFrontRight");
               }

               if (var2 != null && var2.getInventoryItem() != null) {
                  var2.damage(Rand.Next(1, 3));
               }
            }

            if (Rand.Next(7) < var1) {
               this.damageHeadlight("HeadlightLeft", Rand.Next(1, 4));
            }

            if (Rand.Next(7) < var1) {
               this.damageHeadlight("HeadlightRight", Rand.Next(1, 4));
            }

            float var3 = this.getBloodIntensity("Front");
            this.setBloodIntensity("Front", var3 + 0.01F);
         }
      }
   }

   public void addDamageRearHitAChr(int var1) {
      if (!this.isDriverGodMode()) {
         if (var1 >= 4 || !Rand.NextBool(7)) {
            VehiclePart var2 = this.getPartById("TruckBed");
            if (var2 != null && var2.getInventoryItem() != null) {
               var2.setCondition(var2.getCondition() - Rand.Next(Math.max(1, var1 - 10), var1 + 3));
               var2.doInventoryItemStats(var2.getInventoryItem(), 0);
               this.transmitPartCondition(var2);
            }

            var2 = this.getPartById("DoorRear");
            if (var2 != null && var2.getInventoryItem() != null) {
               var2.damage(Rand.Next(Math.max(1, var1 - 10), var1 + 3));
            }

            var2 = this.getPartById("TrunkDoor");
            if (var2 != null && var2.getInventoryItem() != null) {
               var2.damage(Rand.Next(Math.max(1, var1 - 10), var1 + 3));
            }

            if (var1 > 12) {
               var2 = this.getPartById("WindshieldRear");
               if (var2 != null && var2.getInventoryItem() != null) {
                  var2.damage(var1);
               }
            }

            if (Rand.Next(5) < var1) {
               if (Rand.NextBool(2)) {
                  var2 = this.getPartById("TireRearLeft");
               } else {
                  var2 = this.getPartById("TireRearRight");
               }

               if (var2 != null && var2.getInventoryItem() != null) {
                  var2.damage(Rand.Next(1, 3));
               }
            }

            if (Rand.Next(7) < var1) {
               this.damageHeadlight("HeadlightRearLeft", Rand.Next(1, 4));
            }

            if (Rand.Next(7) < var1) {
               this.damageHeadlight("HeadlightRearRight", Rand.Next(1, 4));
            }

            if (Rand.Next(6) < var1) {
               var2 = this.getPartById("GasTank");
               if (var2 != null && var2.getInventoryItem() != null) {
                  var2.damage(Rand.Next(1, 3));
               }
            }

            float var3 = this.getBloodIntensity("Rear");
            this.setBloodIntensity("Rear", var3 + 0.01F);
         }
      }
   }

   private void addDamageFront(int var1) {
      if (!this.isDriverGodMode()) {
         this.currentFrontEndDurability -= var1;
         VehiclePart var2 = this.getPartById("EngineDoor");
         if (var2 != null && var2.getInventoryItem() != null) {
            var2.damage(Rand.Next(Math.max(1, var1 - 5), var1 + 5));
         }

         if (var2 == null || var2.getInventoryItem() == null || var2.getCondition() < 25) {
            var2 = this.getPartById("Engine");
            if (var2 != null) {
               var2.damage(Rand.Next(Math.max(1, var1 - 3), var1 + 3));
            }
         }

         var2 = this.getPartById("Windshield");
         if (var2 != null && var2.getInventoryItem() != null) {
            var2.damage(Rand.Next(Math.max(1, var1 - 5), var1 + 5));
         }

         if (Rand.Next(4) == 0) {
            var2 = this.getPartById("DoorFrontLeft");
            if (var2 != null && var2.getInventoryItem() != null) {
               var2.damage(Rand.Next(Math.max(1, var1 - 5), var1 + 5));
            }

            var2 = this.getPartById("WindowFrontLeft");
            if (var2 != null && var2.getInventoryItem() != null) {
               var2.damage(Rand.Next(Math.max(1, var1 - 5), var1 + 5));
            }
         }

         if (Rand.Next(4) == 0) {
            var2 = this.getPartById("DoorFrontRight");
            if (var2 != null && var2.getInventoryItem() != null) {
               var2.damage(Rand.Next(Math.max(1, var1 - 5), var1 + 5));
            }

            var2 = this.getPartById("WindowFrontRight");
            if (var2 != null && var2.getInventoryItem() != null) {
               var2.damage(Rand.Next(Math.max(1, var1 - 5), var1 + 5));
            }
         }

         if (Rand.Next(20) < var1) {
            this.damageHeadlight("HeadlightLeft", var1);
         }

         if (Rand.Next(20) < var1) {
            this.damageHeadlight("HeadlightRight", var1);
         }

      }
   }

   private void addDamageRear(int var1) {
      if (!this.isDriverGodMode()) {
         this.currentRearEndDurability -= var1;
         VehiclePart var2 = this.getPartById("TruckBed");
         if (var2 != null && var2.getInventoryItem() != null) {
            var2.setCondition(var2.getCondition() - Rand.Next(Math.max(1, var1 - 5), var1 + 5));
            var2.doInventoryItemStats(var2.getInventoryItem(), 0);
            this.transmitPartCondition(var2);
         }

         var2 = this.getPartById("DoorRear");
         if (var2 != null && var2.getInventoryItem() != null) {
            var2.damage(Rand.Next(Math.max(1, var1 - 5), var1 + 5));
         }

         var2 = this.getPartById("TrunkDoor");
         if (var2 != null && var2.getInventoryItem() != null) {
            var2.damage(Rand.Next(Math.max(1, var1 - 5), var1 + 5));
         }

         var2 = this.getPartById("WindshieldRear");
         if (var2 != null && var2.getInventoryItem() != null) {
            var2.damage(var1);
         }

         if (Rand.Next(4) == 0) {
            var2 = this.getPartById("DoorRearLeft");
            if (var2 != null && var2.getInventoryItem() != null) {
               var2.damage(Rand.Next(Math.max(1, var1 - 5), var1 + 5));
            }

            var2 = this.getPartById("WindowRearLeft");
            if (var2 != null && var2.getInventoryItem() != null) {
               var2.damage(Rand.Next(Math.max(1, var1 - 5), var1 + 5));
            }
         }

         if (Rand.Next(4) == 0) {
            var2 = this.getPartById("DoorRearRight");
            if (var2 != null && var2.getInventoryItem() != null) {
               var2.damage(Rand.Next(Math.max(1, var1 - 5), var1 + 5));
            }

            var2 = this.getPartById("WindowRearRight");
            if (var2 != null && var2.getInventoryItem() != null) {
               var2.damage(Rand.Next(Math.max(1, var1 - 5), var1 + 5));
            }
         }

         if (Rand.Next(20) < var1) {
            this.damageHeadlight("HeadlightRearLeft", var1);
         }

         if (Rand.Next(20) < var1) {
            this.damageHeadlight("HeadlightRearRight", var1);
         }

         if (Rand.Next(20) < var1) {
            var2 = this.getPartById("Muffler");
            if (var2 != null && var2.getInventoryItem() != null) {
               var2.damage(Rand.Next(Math.max(1, var1 - 5), var1 + 5));
            }
         }

      }
   }

   private void damageHeadlight(String var1, int var2) {
      if (!this.isDriverGodMode()) {
         VehiclePart var3 = this.getPartById(var1);
         if (var3 != null && var3.getInventoryItem() != null) {
            var3.damage(var2);
            if (var3.getCondition() <= 0) {
               var3.setInventoryItem((InventoryItem)null);
               this.transmitPartItem(var3);
            }
         }

      }
   }

   private float clamp(float var1, float var2, float var3) {
      if (var1 < var2) {
         var1 = var2;
      }

      if (var1 > var3) {
         var1 = var3;
      }

      return var1;
   }

   public boolean isCharacterAdjacentTo(IsoGameCharacter var1) {
      if (PZMath.fastfloor(var1.getZ()) != PZMath.fastfloor(this.getZ())) {
         return false;
      } else {
         Transform var2 = this.getWorldTransform(allocTransform());
         var2.inverse();
         Vector3f var3 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
         var3.set(var1.getX() - WorldSimulation.instance.offsetX, 0.0F, var1.getY() - WorldSimulation.instance.offsetY);
         var2.transform(var3);
         releaseTransform(var2);
         Vector3f var4 = this.script.getExtents();
         Vector3f var5 = this.script.getCenterOfMassOffset();
         float var6 = var5.x - var4.x / 2.0F;
         float var7 = var5.x + var4.x / 2.0F;
         float var8 = var5.z - var4.z / 2.0F;
         float var9 = var5.z + var4.z / 2.0F;
         if (var3.x >= var6 - 0.5F && var3.x < var7 + 0.5F && var3.z >= var8 - 0.5F && var3.z < var9 + 0.5F) {
            ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var3);
            return true;
         } else {
            ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var3);
            return false;
         }
      }
   }

   public Vector2 testCollisionWithCharacter(IsoGameCharacter var1, float var2, Vector2 var3) {
      if (this.physics == null) {
         return null;
      } else {
         Vector3f var4 = this.script.getExtents();
         Vector3f var5 = this.script.getCenterOfMassOffset();
         if (this.DistToProper(var1) > Math.max(var4.x / 2.0F, var4.z / 2.0F) + var2 + 1.0F) {
            return null;
         } else {
            Vector3f var6 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
            this.getLocalPos(var1.getNextX(), var1.getNextY(), 0.0F, var6);
            float var7 = var5.x - var4.x / 2.0F;
            float var8 = var5.x + var4.x / 2.0F;
            float var9 = var5.z - var4.z / 2.0F;
            float var10 = var5.z + var4.z / 2.0F;
            float var11;
            float var12;
            float var13;
            float var14;
            if (var6.x > var7 && var6.x < var8 && var6.z > var9 && var6.z < var10) {
               var11 = var6.x - var7;
               var12 = var8 - var6.x;
               var13 = var6.z - var9;
               var14 = var10 - var6.z;
               Vector3f var18 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
               if (var11 < var12 && var11 < var13 && var11 < var14) {
                  var18.set(var7 - var2 - 0.015F, 0.0F, var6.z);
               } else if (var12 < var11 && var12 < var13 && var12 < var14) {
                  var18.set(var8 + var2 + 0.015F, 0.0F, var6.z);
               } else if (var13 < var11 && var13 < var12 && var13 < var14) {
                  var18.set(var6.x, 0.0F, var9 - var2 - 0.015F);
               } else if (var14 < var11 && var14 < var12 && var14 < var13) {
                  var18.set(var6.x, 0.0F, var10 + var2 + 0.015F);
               }

               ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var6);
               Transform var19 = this.getWorldTransform(allocTransform());
               var19.origin.set(0.0F, 0.0F, 0.0F);
               var19.transform(var18);
               releaseTransform(var19);
               var18.x += this.getX();
               var18.z += this.getY();
               this.collideX = var18.x;
               this.collideY = var18.z;
               var3.set(var18.x, var18.z);
               ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var18);
               return var3;
            } else {
               var11 = this.clamp(var6.x, var7, var8);
               var12 = this.clamp(var6.z, var9, var10);
               var13 = var6.x - var11;
               var14 = var6.z - var12;
               ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var6);
               float var15 = var13 * var13 + var14 * var14;
               if (var15 < var2 * var2) {
                  if (var13 == 0.0F && var14 == 0.0F) {
                     return var3.set(-1.0F, -1.0F);
                  } else {
                     Vector3f var16 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
                     var16.set(var13, 0.0F, var14);
                     var16.normalize();
                     var16.mul(var2 + 0.015F);
                     var16.x += var11;
                     var16.z += var12;
                     Transform var17 = this.getWorldTransform(allocTransform());
                     var17.origin.set(0.0F, 0.0F, 0.0F);
                     var17.transform(var16);
                     releaseTransform(var17);
                     var16.x += this.getX();
                     var16.z += this.getY();
                     this.collideX = var16.x;
                     this.collideY = var16.z;
                     var3.set(var16.x, var16.z);
                     ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var16);
                     return var3;
                  }
               } else {
                  return null;
               }
            }
         }
      }
   }

   public int testCollisionWithProneCharacter(IsoGameCharacter var1, boolean var2) {
      Vector2 var3 = var1.getAnimVector((Vector2)((Vector2ObjectPool)TL_vector2_pool.get()).alloc());
      int var4 = this.testCollisionWithProneCharacter(var1, var3.x, var3.y, var2);
      ((Vector2ObjectPool)TL_vector2_pool.get()).release(var3);
      return var4;
   }

   public int testCollisionWithCorpse(IsoDeadBody var1, boolean var2) {
      float var3 = (float)Math.cos((double)var1.getAngle());
      float var4 = (float)Math.sin((double)var1.getAngle());
      int var5 = this.testCollisionWithProneCharacter(var1, var3, var4, var2);
      return var5;
   }

   public int testCollisionWithProneCharacter(IsoMovingObject var1, float var2, float var3, boolean var4) {
      if (this.physics == null) {
         return 0;
      } else if (GameServer.bServer) {
         return 0;
      } else {
         Vector3f var5 = this.script.getExtents();
         float var6 = 0.3F;
         if (this.DistToProper(var1) > Math.max(var5.x / 2.0F, var5.z / 2.0F) + var6 + 1.0F) {
            return 0;
         } else if (Math.abs(this.jniSpeed) < 3.0F) {
            return 0;
         } else {
            float var7 = 0.65F;
            if (var1 instanceof IsoDeadBody && var1.getModData() != null && var1.getModData().rawget("corpseLength") != null) {
            }

            float var8 = var1.getX() + var2 * var7;
            float var9 = var1.getY() + var3 * var7;
            float var10 = var1.getX() - var2 * var7;
            float var11 = var1.getY() - var3 * var7;
            int var12 = 0;
            Vector3f var13 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
            Vector3f var14 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();

            for(int var15 = 0; var15 < this.script.getWheelCount(); ++var15) {
               VehicleScript.Wheel var16 = this.script.getWheel(var15);
               boolean var17 = true;

               for(int var18 = 0; var18 < this.models.size(); ++var18) {
                  ModelInfo var19 = (ModelInfo)this.models.get(var18);
                  if (var19.wheelIndex == var15) {
                     this.getWorldPos(var16.offset.x, var16.offset.y - this.wheelInfo[var15].suspensionLength, var16.offset.z, var13);
                     if (var13.z > this.script.getWheel(var15).radius + 0.05F) {
                        var17 = false;
                     }
                     break;
                  }
               }

               if (var17) {
                  this.getWorldPos(var16.offset.x, var16.offset.y, var16.offset.z, var14);
                  float var22 = var14.x;
                  float var23 = var14.y;
                  double var24 = (double)((var22 - var10) * (var8 - var10) + (var23 - var11) * (var9 - var11)) / (Math.pow((double)(var8 - var10), 2.0) + Math.pow((double)(var9 - var11), 2.0));
                  float var26;
                  float var27;
                  if (var24 <= 0.0) {
                     var26 = var10;
                     var27 = var11;
                  } else if (var24 >= 1.0) {
                     var26 = var8;
                     var27 = var9;
                  } else {
                     var26 = var10 + (var8 - var10) * (float)var24;
                     var27 = var11 + (var9 - var11) * (float)var24;
                  }

                  if (!(IsoUtils.DistanceToSquared(var14.x, var14.y, var26, var27) > var16.radius * var16.radius)) {
                     if (var4 && Math.abs(this.jniSpeed) > 10.0F) {
                        if (GameServer.bServer && var1 instanceof IsoZombie) {
                           ((IsoZombie)var1).setThumpFlag(1);
                        } else {
                           SoundManager.instance.PlayWorldSound("VehicleRunOverBody", var1.getCurrentSquare(), 0.0F, 20.0F, 0.9F, true);
                        }

                        var4 = false;
                     }

                     if (var15 < this.impulseFromSquishedZombie.length) {
                        if (this.impulseFromSquishedZombie[var15] == null) {
                           this.impulseFromSquishedZombie[var15] = new VehicleImpulse();
                        }

                        this.impulseFromSquishedZombie[var15].impulse.set(0.0F, 1.0F, 0.0F);
                        float var28 = Math.max(Math.abs(this.jniSpeed), 10.0F) / 10.0F;
                        float var29 = 1.0F;
                        if (var1 instanceof IsoDeadBody && var1.getModData() != null && var1.getModData().rawget("corpseSize") != null) {
                           var29 = ((KahluaTableImpl)var1.getModData()).rawgetFloat("corpseSize");
                        }

                        this.impulseFromSquishedZombie[var15].impulse.mul(0.065F * this.getFudgedMass() * var28 * var29);
                        this.impulseFromSquishedZombie[var15].rel_pos.set(var14.x - this.getX(), 0.0F, var14.y - this.getY());
                        this.impulseFromSquishedZombie[var15].enable = true;
                        this.impulseFromSquishedZombie[var15].applied = false;
                        ++var12;
                     }
                  }
               }
            }

            ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var13);
            ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var14);
            return var12;
         }
      }
   }

   public Vector2 testCollisionWithObject(IsoObject var1, float var2, Vector2 var3) {
      if (this.physics == null) {
         return null;
      } else if (var1.square == null) {
         return null;
      } else {
         float var4 = this.getObjectX(var1);
         float var5 = this.getObjectY(var1);
         Vector3f var6 = this.script.getExtents();
         Vector3f var7 = this.script.getCenterOfMassOffset();
         float var8 = Math.max(var6.x / 2.0F, var6.z / 2.0F) + var2 + 1.0F;
         if (this.DistToSquared(var4, var5) > var8 * var8) {
            return null;
         } else {
            Vector3f var9 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
            this.getLocalPos(var4, var5, 0.0F, var9);
            float var10 = var7.x - var6.x / 2.0F;
            float var11 = var7.x + var6.x / 2.0F;
            float var12 = var7.z - var6.z / 2.0F;
            float var13 = var7.z + var6.z / 2.0F;
            float var14;
            float var15;
            float var16;
            float var17;
            if (var9.x > var10 && var9.x < var11 && var9.z > var12 && var9.z < var13) {
               var14 = var9.x - var10;
               var15 = var11 - var9.x;
               var16 = var9.z - var12;
               var17 = var13 - var9.z;
               Vector3f var21 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
               if (var14 < var15 && var14 < var16 && var14 < var17) {
                  var21.set(var10 - var2 - 0.015F, 0.0F, var9.z);
               } else if (var15 < var14 && var15 < var16 && var15 < var17) {
                  var21.set(var11 + var2 + 0.015F, 0.0F, var9.z);
               } else if (var16 < var14 && var16 < var15 && var16 < var17) {
                  var21.set(var9.x, 0.0F, var12 - var2 - 0.015F);
               } else if (var17 < var14 && var17 < var15 && var17 < var16) {
                  var21.set(var9.x, 0.0F, var13 + var2 + 0.015F);
               }

               ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var9);
               Transform var22 = this.getWorldTransform(allocTransform());
               var22.origin.set(0.0F, 0.0F, 0.0F);
               var22.transform(var21);
               releaseTransform(var22);
               var21.x += this.getX();
               var21.z += this.getY();
               this.collideX = var21.x;
               this.collideY = var21.z;
               var3.set(var21.x, var21.z);
               ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var21);
               return var3;
            } else {
               var14 = this.clamp(var9.x, var10, var11);
               var15 = this.clamp(var9.z, var12, var13);
               var16 = var9.x - var14;
               var17 = var9.z - var15;
               ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var9);
               float var18 = var16 * var16 + var17 * var17;
               if (var18 < var2 * var2) {
                  if (var16 == 0.0F && var17 == 0.0F) {
                     return var3.set(-1.0F, -1.0F);
                  } else {
                     Vector3f var19 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
                     var19.set(var16, 0.0F, var17);
                     var19.normalize();
                     var19.mul(var2 + 0.015F);
                     var19.x += var14;
                     var19.z += var15;
                     Transform var20 = this.getWorldTransform(allocTransform());
                     var20.origin.set(0.0F, 0.0F, 0.0F);
                     var20.transform(var19);
                     releaseTransform(var20);
                     var19.x += this.getX();
                     var19.z += this.getY();
                     this.collideX = var19.x;
                     this.collideY = var19.z;
                     var3.set(var19.x, var19.z);
                     ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var19);
                     return var3;
                  }
               } else {
                  return null;
               }
            }
         }
      }
   }

   public boolean testCollisionWithVehicle(BaseVehicle var1) {
      VehicleScript var2 = this.script;
      if (var2 == null) {
         var2 = ScriptManager.instance.getVehicle(this.scriptName);
      }

      VehicleScript var3 = var1.script;
      if (var3 == null) {
         var3 = ScriptManager.instance.getVehicle(var1.scriptName);
      }

      if (var2 != null && var3 != null) {
         Vector2[] var4 = BaseVehicle.L_testCollisionWithVehicle.testVecs1;
         Vector2[] var5 = BaseVehicle.L_testCollisionWithVehicle.testVecs2;
         if (var4[0] == null) {
            for(int var6 = 0; var6 < var4.length; ++var6) {
               var4[var6] = new Vector2();
               var5[var6] = new Vector2();
            }
         }

         Vector3f var12 = var2.getExtents();
         Vector3f var7 = var2.getCenterOfMassOffset();
         Vector3f var8 = var3.getExtents();
         Vector3f var9 = var3.getCenterOfMassOffset();
         Vector3f var10 = BaseVehicle.L_testCollisionWithVehicle.worldPos;
         float var11 = 0.5F;
         this.getWorldPos(var7.x + var12.x * var11, 0.0F, var7.z + var12.z * var11, var10, var2);
         var4[0].set(var10.x, var10.y);
         this.getWorldPos(var7.x - var12.x * var11, 0.0F, var7.z + var12.z * var11, var10, var2);
         var4[1].set(var10.x, var10.y);
         this.getWorldPos(var7.x - var12.x * var11, 0.0F, var7.z - var12.z * var11, var10, var2);
         var4[2].set(var10.x, var10.y);
         this.getWorldPos(var7.x + var12.x * var11, 0.0F, var7.z - var12.z * var11, var10, var2);
         var4[3].set(var10.x, var10.y);
         var1.getWorldPos(var9.x + var8.x * var11, 0.0F, var9.z + var8.z * var11, var10, var3);
         var5[0].set(var10.x, var10.y);
         var1.getWorldPos(var9.x - var8.x * var11, 0.0F, var9.z + var8.z * var11, var10, var3);
         var5[1].set(var10.x, var10.y);
         var1.getWorldPos(var9.x - var8.x * var11, 0.0F, var9.z - var8.z * var11, var10, var3);
         var5[2].set(var10.x, var10.y);
         var1.getWorldPos(var9.x + var8.x * var11, 0.0F, var9.z - var8.z * var11, var10, var3);
         var5[3].set(var10.x, var10.y);
         return QuadranglesIntersection.IsQuadranglesAreIntersected(var4, var5);
      } else {
         return false;
      }
   }

   protected float getObjectX(IsoObject var1) {
      return var1 instanceof IsoMovingObject ? var1.getX() : (float)var1.getSquare().getX() + 0.5F;
   }

   protected float getObjectY(IsoObject var1) {
      return var1 instanceof IsoMovingObject ? var1.getY() : (float)var1.getSquare().getY() + 0.5F;
   }

   public void ApplyImpulse(IsoObject var1, float var2) {
      float var3 = this.getObjectX(var1);
      float var4 = this.getObjectY(var1);
      VehicleImpulse var5 = BaseVehicle.VehicleImpulse.alloc();
      var5.impulse.set(this.getX() - var3, 0.0F, this.getY() - var4);
      var5.impulse.normalize();
      var5.impulse.mul(var2);
      var5.rel_pos.set(var3 - this.getX(), 0.0F, var4 - this.getY());
      this.impulseFromHitZombie.add(var5);
   }

   public void ApplyImpulse4Break(IsoObject var1, float var2) {
      float var3 = this.getObjectX(var1);
      float var4 = this.getObjectY(var1);
      VehicleImpulse var5 = BaseVehicle.VehicleImpulse.alloc();
      this.getLinearVelocity(var5.impulse);
      var5.impulse.mul(-var2 * this.getFudgedMass());
      var5.rel_pos.set(var3 - this.getX(), 0.0F, var4 - this.getY());
      this.impulseFromHitZombie.add(var5);
   }

   public void hitCharacter(IsoZombie var1) {
      IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      IsoZombie var3 = (IsoZombie)Type.tryCastTo(var1, IsoZombie.class);
      if (var1.getCurrentState() != StaggerBackState.instance() && var1.getCurrentState() != ZombieFallDownState.instance()) {
         if (!(Math.abs(var1.getX() - this.getX()) < 0.01F) && !(Math.abs(var1.getY() - this.getY()) < 0.01F)) {
            float var4 = 15.0F;
            Vector3f var5 = this.getLinearVelocity((Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
            var5.y = 0.0F;
            float var6 = var5.length();
            var6 = Math.min(var6, var4);
            if (var6 < 0.05F) {
               ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var5);
            } else {
               Vector3f var7 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
               var7.set(this.getX() - var1.getX(), 0.0F, this.getY() - var1.getY());
               var7.normalize();
               var5.normalize();
               float var8 = var5.dot(var7);
               ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var5);
               if (var8 < 0.0F && !GameServer.bServer) {
                  this.ApplyImpulse(var1, this.getFudgedMass() * 7.0F * var6 / var4 * Math.abs(var8));
               }

               var7.normalize();
               var7.mul(3.0F * var6 / var4);
               Vector2 var9 = (Vector2)((Vector2ObjectPool)TL_vector2_pool.get()).alloc();
               float var10 = var6 + this.physics.clientForce / this.getFudgedMass();
               if (var2 != null) {
                  var2.setVehicleHitLocation(this);
               } else if (var3 != null) {
                  var3.setVehicleHitLocation(this);
               }

               BaseSoundEmitter var11 = IsoWorld.instance.getFreeEmitter(var1.getX(), var1.getY(), var1.getZ());
               long var12 = var11.playSound("VehicleHitCharacter");
               var11.setParameterValue(var12, FMODManager.instance.getParameterDescription("VehicleHitLocation"), (float)ParameterVehicleHitLocation.calculateLocation(this, var1.getX(), var1.getY(), var1.getZ()).getValue());
               var11.setParameterValue(var12, FMODManager.instance.getParameterDescription("VehicleSpeed"), this.getCurrentSpeedKmHour());
               var1.Hit(this, var10, var8 > 0.0F, var9.set(-var7.x, -var7.z));
               IsoPlayer var14 = (IsoPlayer)Type.tryCastTo(this.getDriver(), IsoPlayer.class);
               if (var14 != null && var14.isLocalPlayer()) {
                  var14.triggerMusicIntensityEvent("VehicleHitCharacter");
               }

               ((Vector2ObjectPool)TL_vector2_pool.get()).release(var9);
               ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var7);
               long var17 = System.currentTimeMillis();
               long var13 = (var17 - this.zombieHitTimestamp) / 1000L;
               this.zombiesHits = Math.max(this.zombiesHits - (int)var13, 0);
               if (var17 - this.zombieHitTimestamp > 700L) {
                  this.zombieHitTimestamp = var17;
                  ++this.zombiesHits;
                  this.zombiesHits = Math.min(this.zombiesHits, 20);
               }

               if (var6 >= 5.0F || this.zombiesHits > 10) {
                  var6 = this.getCurrentSpeedKmHour() / 5.0F;
                  Vector3f var15 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
                  this.getLocalPos(var1.getX(), var1.getY(), var1.getZ(), var15);
                  int var16;
                  if (var15.z > 0.0F) {
                     var16 = this.caclulateDamageWithBodies(true);
                     this.addDamageFrontHitAChr(var16);
                  } else {
                     var16 = this.caclulateDamageWithBodies(false);
                     this.addDamageRearHitAChr(var16);
                  }

                  ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var15);
               }

            }
         }
      }
   }

   public void hitCharacter(IsoAnimal var1) {
      IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      IsoAnimal var3 = (IsoAnimal)Type.tryCastTo(var1, IsoAnimal.class);
      if (var1.getCurrentState() != AnimalFalldownState.instance()) {
         if (!(Math.abs(var1.getX() - this.getX()) < 0.01F) && !(Math.abs(var1.getY() - this.getY()) < 0.01F)) {
            float var4 = 15.0F;
            Vector3f var5 = this.getLinearVelocity((Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
            var5.y = 0.0F;
            float var6 = var5.length();
            var6 = Math.min(var6, var4);
            if (var6 < 0.05F) {
               ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var5);
            } else {
               Vector3f var7 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
               var7.set(this.getX() - var1.getX(), 0.0F, this.getY() - var1.getY());
               var7.normalize();
               var5.normalize();
               float var8 = var5.dot(var7);
               ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var5);
               if (var8 < 0.0F && !GameServer.bServer) {
                  this.ApplyImpulse(var1, this.getFudgedMass() * 7.0F * var6 / var4 * Math.abs(var8));
               }

               var7.normalize();
               var7.mul(3.0F * var6 / var4);
               Vector2 var9 = (Vector2)((Vector2ObjectPool)TL_vector2_pool.get()).alloc();
               float var10 = var6 + this.physics.clientForce / this.getFudgedMass();
               if (var2 != null) {
                  var2.setVehicleHitLocation(this);
               } else if (var3 != null) {
                  var3.setVehicleHitLocation(this);
               }

               BaseSoundEmitter var11 = IsoWorld.instance.getFreeEmitter(var1.getX(), var1.getY(), var1.getZ());
               long var12 = var11.playSound("VehicleHitCharacter");
               var11.setParameterValue(var12, FMODManager.instance.getParameterDescription("VehicleHitLocation"), (float)ParameterVehicleHitLocation.calculateLocation(this, var1.getX(), var1.getY(), var1.getZ()).getValue());
               var11.setParameterValue(var12, FMODManager.instance.getParameterDescription("VehicleSpeed"), this.getCurrentSpeedKmHour());
               var1.Hit(this, var10, var8 > 0.0F, var9.set(-var7.x, -var7.z));
               IsoPlayer var14 = (IsoPlayer)Type.tryCastTo(this.getDriver(), IsoPlayer.class);
               if (var14 != null && var14.isLocalPlayer()) {
                  var14.triggerMusicIntensityEvent("VehicleHitCharacter");
               }

               ((Vector2ObjectPool)TL_vector2_pool.get()).release(var9);
               ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var7);
               long var17 = System.currentTimeMillis();
               long var13 = (var17 - this.zombieHitTimestamp) / 1000L;
               this.zombiesHits = Math.max(this.zombiesHits - (int)var13, 0);
               if (var17 - this.zombieHitTimestamp > 700L) {
                  this.zombieHitTimestamp = var17;
                  ++this.zombiesHits;
                  this.zombiesHits = Math.min(this.zombiesHits, 20);
               }

               if (var6 >= 5.0F || this.zombiesHits > 10) {
                  var6 = this.getCurrentSpeedKmHour() / 5.0F;
                  Vector3f var15 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
                  this.getLocalPos(var1.getX(), var1.getY(), var1.getZ(), var15);
                  int var16;
                  if (var15.z > 0.0F) {
                     var16 = this.caclulateDamageWithBodies(true);
                     this.addDamageFrontHitAChr(var16);
                  } else {
                     var16 = this.caclulateDamageWithBodies(false);
                     this.addDamageRearHitAChr(var16);
                  }

                  ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var15);
               }

            }
         }
      }
   }

   private int caclulateDamageWithBodies(boolean var1) {
      boolean var2 = this.getCurrentSpeedKmHour() > 0.0F;
      float var3 = Math.abs(this.getCurrentSpeedKmHour());
      float var4 = var3 / 160.0F;
      var4 = PZMath.clamp(var4 * var4, 0.0F, 1.0F);
      float var5 = 60.0F * var4;
      float var6 = PZMath.max(1.0F, (float)this.zombiesHits / 3.0F);
      if (!var1 && !var2) {
         var6 = 1.0F;
      }

      if (this.zombiesHits > 10 && var5 < Math.abs(this.getCurrentSpeedKmHour()) / 5.0F) {
         var5 = Math.abs(this.getCurrentSpeedKmHour()) / 5.0F;
      }

      return (int)(var6 * var5);
   }

   public int calculateDamageWithCharacter(IsoGameCharacter var1) {
      Vector3f var2 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
      this.getLocalPos(var1.getX(), var1.getY(), var1.getZ(), var2);
      int var3;
      if (var2.z > 0.0F) {
         var3 = this.caclulateDamageWithBodies(true);
      } else {
         var3 = -1 * this.caclulateDamageWithBodies(false);
      }

      ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var2);
      return var3;
   }

   public boolean blocked(int var1, int var2, int var3) {
      if (!this.removedFromWorld && this.current != null && this.getController() != null) {
         if (this.getController() == null) {
            return false;
         } else if (var3 != PZMath.fastfloor(this.getZ())) {
            return false;
         } else if (IsoUtils.DistanceTo2D((float)var1 + 0.5F, (float)var2 + 0.5F, this.getX(), this.getY()) > 5.0F) {
            return false;
         } else {
            float var4 = 0.3F;
            Transform var5 = allocTransform();
            this.getWorldTransform(var5);
            var5.inverse();
            Vector3f var6 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
            var6.set((float)var1 + 0.5F - WorldSimulation.instance.offsetX, 0.0F, (float)var2 + 0.5F - WorldSimulation.instance.offsetY);
            var5.transform(var6);
            releaseTransform(var5);
            Vector3f var7 = this.script.getExtents();
            Vector3f var8 = this.script.getCenterOfMassOffset();
            float var9 = this.clamp(var6.x, var8.x - var7.x / 2.0F, var8.x + var7.x / 2.0F);
            float var10 = this.clamp(var6.z, var8.z - var7.z / 2.0F, var8.z + var7.z / 2.0F);
            float var11 = var6.x - var9;
            float var12 = var6.z - var10;
            ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var6);
            float var13 = var11 * var11 + var12 * var12;
            return var13 < var4 * var4;
         }
      } else {
         return false;
      }
   }

   public boolean isIntersectingSquare(int var1, int var2, int var3) {
      if (var3 != PZMath.fastfloor(this.getZ())) {
         return false;
      } else if (!this.removedFromWorld && this.current != null && this.getController() != null) {
         tempPoly.x1 = tempPoly.x4 = (float)var1;
         tempPoly.y1 = tempPoly.y2 = (float)var2;
         tempPoly.x2 = tempPoly.x3 = (float)(var1 + 1);
         tempPoly.y3 = tempPoly.y4 = (float)(var2 + 1);
         return PolyPolyIntersect.intersects(tempPoly, this.getPoly());
      } else {
         return false;
      }
   }

   public boolean isIntersectingSquare(IsoGridSquare var1) {
      return this.isIntersectingSquare(var1.getX(), var1.getY(), var1.getZ());
   }

   public boolean isIntersectingSquareWithShadow(int var1, int var2, int var3) {
      if (var3 != PZMath.fastfloor(this.getZ())) {
         return false;
      } else if (!this.removedFromWorld && this.current != null && this.getController() != null) {
         tempPoly.x1 = tempPoly.x4 = (float)var1;
         tempPoly.y1 = tempPoly.y2 = (float)var2;
         tempPoly.x2 = tempPoly.x3 = (float)(var1 + 1);
         tempPoly.y3 = tempPoly.y4 = (float)(var2 + 1);
         return PolyPolyIntersect.intersects(tempPoly, this.shadowCoord);
      } else {
         return false;
      }
   }

   public boolean circleIntersects(float var1, float var2, float var3, float var4) {
      if (this.getController() == null) {
         return false;
      } else if (PZMath.fastfloor(var3) != PZMath.fastfloor(this.getZ())) {
         return false;
      } else if (IsoUtils.DistanceTo2D(var1, var2, this.getX(), this.getY()) > 5.0F) {
         return false;
      } else {
         Vector3f var5 = this.script.getExtents();
         Vector3f var6 = this.script.getCenterOfMassOffset();
         Vector3f var7 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
         this.getLocalPos(var1, var2, var3, var7);
         float var8 = var6.x - var5.x / 2.0F;
         float var9 = var6.x + var5.x / 2.0F;
         float var10 = var6.z - var5.z / 2.0F;
         float var11 = var6.z + var5.z / 2.0F;
         if (var7.x > var8 && var7.x < var9 && var7.z > var10 && var7.z < var11) {
            return true;
         } else {
            float var12 = this.clamp(var7.x, var8, var9);
            float var13 = this.clamp(var7.z, var10, var11);
            float var14 = var7.x - var12;
            float var15 = var7.z - var13;
            ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var7);
            float var16 = var14 * var14 + var15 * var15;
            return var16 < var4 * var4;
         }
      }
   }

   public void updateLights() {
      VehicleModelInstance var1 = (VehicleModelInstance)this.sprite.modelSlot.model;
      var1.textureRustA = this.rust;
      if (this.script.getWheelCount() == 0) {
         var1.textureRustA = 0.0F;
      }

      var1.painColor.x = this.colorHue;
      var1.painColor.y = this.colorSaturation;
      var1.painColor.z = this.colorValue;
      boolean var2 = false;
      boolean var3 = false;
      boolean var4 = false;
      boolean var5 = false;
      boolean var6 = false;
      boolean var7 = false;
      boolean var8 = false;
      boolean var9 = false;
      if (this.windowLightsOn) {
         VehiclePart var10 = this.getPartById("Windshield");
         var2 = var10 != null && var10.getInventoryItem() != null;
         var10 = this.getPartById("WindshieldRear");
         var3 = var10 != null && var10.getInventoryItem() != null;
         var10 = this.getPartById("WindowFrontLeft");
         var4 = var10 != null && var10.getInventoryItem() != null;
         var10 = this.getPartById("WindowMiddleLeft");
         var5 = var10 != null && var10.getInventoryItem() != null;
         var10 = this.getPartById("WindowRearLeft");
         var6 = var10 != null && var10.getInventoryItem() != null;
         var10 = this.getPartById("WindowFrontRight");
         var7 = var10 != null && var10.getInventoryItem() != null;
         var10 = this.getPartById("WindowMiddleRight");
         var8 = var10 != null && var10.getInventoryItem() != null;
         var10 = this.getPartById("WindowRearRight");
         var9 = var10 != null && var10.getInventoryItem() != null;
      }

      var1.textureLightsEnables1[10] = var2 ? 1.0F : 0.0F;
      var1.textureLightsEnables1[14] = var3 ? 1.0F : 0.0F;
      var1.textureLightsEnables1[2] = var4 ? 1.0F : 0.0F;
      var1.textureLightsEnables1[6] = var5 | var6 ? 1.0F : 0.0F;
      var1.textureLightsEnables1[9] = var7 ? 1.0F : 0.0F;
      var1.textureLightsEnables1[13] = var8 | var9 ? 1.0F : 0.0F;
      boolean var18 = false;
      boolean var11 = false;
      boolean var12 = false;
      boolean var13 = false;
      if (this.headlightsOn && this.getBatteryCharge() > 0.0F) {
         VehiclePart var14 = this.getPartById("HeadlightLeft");
         if (var14 != null && var14.getInventoryItem() != null) {
            var18 = true;
         }

         var14 = this.getPartById("HeadlightRight");
         if (var14 != null && var14.getInventoryItem() != null) {
            var11 = true;
         }

         var14 = this.getPartById("HeadlightRearLeft");
         if (var14 != null && var14.getInventoryItem() != null) {
            var13 = true;
         }

         var14 = this.getPartById("HeadlightRearRight");
         if (var14 != null && var14.getInventoryItem() != null) {
            var12 = true;
         }
      }

      var1.textureLightsEnables2[4] = var11 ? 1.0F : 0.0F;
      var1.textureLightsEnables2[8] = var18 ? 1.0F : 0.0F;
      var1.textureLightsEnables2[12] = var12 ? 1.0F : 0.0F;
      var1.textureLightsEnables2[1] = var13 ? 1.0F : 0.0F;
      boolean var19 = this.stoplightsOn && this.getBatteryCharge() > 0.0F;
      if (this.scriptName.contains("Trailer") && this.vehicleTowedBy != null && this.vehicleTowedBy.stoplightsOn && this.vehicleTowedBy.getBatteryCharge() > 0.0F) {
         var19 = true;
      }

      if (var19) {
         var1.textureLightsEnables2[5] = 1.0F;
         var1.textureLightsEnables2[9] = 1.0F;
      } else {
         var1.textureLightsEnables2[5] = 0.0F;
         var1.textureLightsEnables2[9] = 0.0F;
      }

      if (this.script.getLightbar().enable) {
         if (this.lightbarLightsMode.isEnable() && this.getBatteryCharge() > 0.0F) {
            switch (this.lightbarLightsMode.getLightTexIndex()) {
               case 0:
                  var1.textureLightsEnables2[13] = 0.0F;
                  var1.textureLightsEnables2[2] = 0.0F;
                  break;
               case 1:
                  var1.textureLightsEnables2[13] = 0.0F;
                  var1.textureLightsEnables2[2] = 1.0F;
                  break;
               case 2:
                  var1.textureLightsEnables2[13] = 1.0F;
                  var1.textureLightsEnables2[2] = 0.0F;
                  break;
               default:
                  var1.textureLightsEnables2[13] = 0.0F;
                  var1.textureLightsEnables2[2] = 0.0F;
            }
         } else {
            var1.textureLightsEnables2[13] = 0.0F;
            var1.textureLightsEnables2[2] = 0.0F;
         }
      }

      if (DebugOptions.instance.VehicleCycleColor.getValue()) {
         float var15 = (float)(System.currentTimeMillis() % 2000L);
         float var16 = (float)(System.currentTimeMillis() % 7000L);
         float var17 = (float)(System.currentTimeMillis() % 11000L);
         var1.painColor.x = var15 / 2000.0F;
         var1.painColor.y = var16 / 7000.0F;
         var1.painColor.z = var17 / 11000.0F;
      }

      if (DebugOptions.instance.VehicleRenderBlood0.getValue()) {
         Arrays.fill(var1.matrixBlood1Enables1, 0.0F);
         Arrays.fill(var1.matrixBlood1Enables2, 0.0F);
         Arrays.fill(var1.matrixBlood2Enables1, 0.0F);
         Arrays.fill(var1.matrixBlood2Enables2, 0.0F);
      }

      if (DebugOptions.instance.VehicleRenderBlood50.getValue()) {
         Arrays.fill(var1.matrixBlood1Enables1, 0.5F);
         Arrays.fill(var1.matrixBlood1Enables2, 0.5F);
         Arrays.fill(var1.matrixBlood2Enables1, 1.0F);
         Arrays.fill(var1.matrixBlood2Enables2, 1.0F);
      }

      if (DebugOptions.instance.VehicleRenderBlood100.getValue()) {
         Arrays.fill(var1.matrixBlood1Enables1, 1.0F);
         Arrays.fill(var1.matrixBlood1Enables2, 1.0F);
         Arrays.fill(var1.matrixBlood2Enables1, 1.0F);
         Arrays.fill(var1.matrixBlood2Enables2, 1.0F);
      }

      if (DebugOptions.instance.VehicleRenderDamage0.getValue()) {
         Arrays.fill(var1.textureDamage1Enables1, 0.0F);
         Arrays.fill(var1.textureDamage1Enables2, 0.0F);
         Arrays.fill(var1.textureDamage2Enables1, 0.0F);
         Arrays.fill(var1.textureDamage2Enables2, 0.0F);
      }

      if (DebugOptions.instance.VehicleRenderDamage1.getValue()) {
         Arrays.fill(var1.textureDamage1Enables1, 1.0F);
         Arrays.fill(var1.textureDamage1Enables2, 1.0F);
         Arrays.fill(var1.textureDamage2Enables1, 0.0F);
         Arrays.fill(var1.textureDamage2Enables2, 0.0F);
      }

      if (DebugOptions.instance.VehicleRenderDamage2.getValue()) {
         Arrays.fill(var1.textureDamage1Enables1, 0.0F);
         Arrays.fill(var1.textureDamage1Enables2, 0.0F);
         Arrays.fill(var1.textureDamage2Enables1, 1.0F);
         Arrays.fill(var1.textureDamage2Enables2, 1.0F);
      }

      if (DebugOptions.instance.VehicleRenderRust0.getValue()) {
         var1.textureRustA = 0.0F;
      }

      if (DebugOptions.instance.VehicleRenderRust50.getValue()) {
         var1.textureRustA = 0.5F;
      }

      if (DebugOptions.instance.VehicleRenderRust100.getValue()) {
         var1.textureRustA = 1.0F;
      }

      var1.refBody = 0.3F;
      var1.refWindows = 0.4F;
      if (this.rust > 0.8F) {
         var1.refBody = 0.1F;
         var1.refWindows = 0.2F;
      }

   }

   private void updateWorldLights() {
      if (!this.script.getLightbar().enable) {
         this.removeWorldLights();
      } else if (this.lightbarLightsMode.isEnable() && !(this.getBatteryCharge() <= 0.0F)) {
         if (this.lightbarLightsMode.getLightTexIndex() == 0) {
            this.removeWorldLights();
         } else {
            this.leftLight1.radius = this.leftLight2.radius = this.rightLight1.radius = this.rightLight2.radius = 8;
            Vector3f var1;
            int var2;
            int var3;
            int var4;
            int var5;
            IsoLightSource var6;
            if (this.lightbarLightsMode.getLightTexIndex() == 1) {
               var1 = this.getWorldPos(0.4F, 0.0F, 0.0F, (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
               var2 = PZMath.fastfloor(var1.x);
               var3 = PZMath.fastfloor(var1.y);
               var4 = PZMath.fastfloor(this.getZ());
               ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var1);
               var5 = this.leftLightIndex;
               if (var5 == 1 && this.leftLight1.x == var2 && this.leftLight1.y == var3 && this.leftLight1.z == var4) {
                  return;
               }

               if (var5 == 2 && this.leftLight2.x == var2 && this.leftLight2.y == var3 && this.leftLight2.z == var4) {
                  return;
               }

               this.removeWorldLights();
               if (var5 == 1) {
                  var6 = this.leftLight2;
                  this.leftLightIndex = 2;
               } else {
                  var6 = this.leftLight1;
                  this.leftLightIndex = 1;
               }

               var6.life = -1;
               var6.x = var2;
               var6.y = var3;
               var6.z = var4;
               IsoWorld.instance.CurrentCell.addLamppost(var6);
            } else {
               var1 = this.getWorldPos(-0.4F, 0.0F, 0.0F, (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
               var2 = PZMath.fastfloor(var1.x);
               var3 = PZMath.fastfloor(var1.y);
               var4 = PZMath.fastfloor(this.getZ());
               ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var1);
               var5 = this.rightLightIndex;
               if (var5 == 1 && this.rightLight1.x == var2 && this.rightLight1.y == var3 && this.rightLight1.z == var4) {
                  return;
               }

               if (var5 == 2 && this.rightLight2.x == var2 && this.rightLight2.y == var3 && this.rightLight2.z == var4) {
                  return;
               }

               this.removeWorldLights();
               if (var5 == 1) {
                  var6 = this.rightLight2;
                  this.rightLightIndex = 2;
               } else {
                  var6 = this.rightLight1;
                  this.rightLightIndex = 1;
               }

               var6.life = -1;
               var6.x = var2;
               var6.y = var3;
               var6.z = var4;
               IsoWorld.instance.CurrentCell.addLamppost(var6);
            }

         }
      } else {
         this.removeWorldLights();
      }
   }

   public void fixLightbarModelLighting(IsoLightSource var1, Vector3f var2) {
      if (var1 != this.leftLight1 && var1 != this.leftLight2) {
         if (var1 == this.rightLight1 || var1 == this.rightLight2) {
            var2.set(-1.0F, 0.0F, 0.0F);
         }
      } else {
         var2.set(1.0F, 0.0F, 0.0F);
      }

   }

   private void removeWorldLights() {
      if (this.leftLightIndex == 1) {
         IsoWorld.instance.CurrentCell.removeLamppost(this.leftLight1);
         this.leftLightIndex = -1;
      }

      if (this.leftLightIndex == 2) {
         IsoWorld.instance.CurrentCell.removeLamppost(this.leftLight2);
         this.leftLightIndex = -1;
      }

      if (this.rightLightIndex == 1) {
         IsoWorld.instance.CurrentCell.removeLamppost(this.rightLight1);
         this.rightLightIndex = -1;
      }

      if (this.rightLightIndex == 2) {
         IsoWorld.instance.CurrentCell.removeLamppost(this.rightLight2);
         this.rightLightIndex = -1;
      }

   }

   public void doDamageOverlay() {
      if (this.sprite.modelSlot != null) {
         this.doDoorDamage();
         this.doWindowDamage();
         this.doOtherBodyWorkDamage();
         this.doBloodOverlay();
      }
   }

   private void checkDamage(VehiclePart var1, int var2, boolean var3) {
      if (var3 && var1 != null && var1.getId().startsWith("Window") && var1.getScriptModelById("Default") != null) {
         var3 = false;
      }

      VehicleModelInstance var4 = (VehicleModelInstance)this.sprite.modelSlot.model;

      try {
         var4.textureDamage1Enables1[var2] = 0.0F;
         var4.textureDamage2Enables1[var2] = 0.0F;
         var4.textureUninstall1[var2] = 0.0F;
         if (var1 != null && var1.getInventoryItem() != null) {
            if (var1.getInventoryItem().getCondition() < 60 && var1.getInventoryItem().getCondition() >= 40) {
               var4.textureDamage1Enables1[var2] = 1.0F;
            }

            if (var1.getInventoryItem().getCondition() < 40) {
               var4.textureDamage2Enables1[var2] = 1.0F;
            }

            if (var1.window != null && var1.window.isOpen() && var3) {
               var4.textureUninstall1[var2] = 1.0F;
            }
         } else if (var1 != null && var3) {
            var4.textureUninstall1[var2] = 1.0F;
         }
      } catch (Exception var6) {
         var6.printStackTrace();
      }

   }

   private void checkDamage2(VehiclePart var1, int var2, boolean var3) {
      VehicleModelInstance var4 = (VehicleModelInstance)this.sprite.modelSlot.model;

      try {
         var4.textureDamage1Enables2[var2] = 0.0F;
         var4.textureDamage2Enables2[var2] = 0.0F;
         var4.textureUninstall2[var2] = 0.0F;
         if (var1 != null && var1.getInventoryItem() != null) {
            if (var1.getInventoryItem().getCondition() < 60 && var1.getInventoryItem().getCondition() >= 40) {
               var4.textureDamage1Enables2[var2] = 1.0F;
            }

            if (var1.getInventoryItem().getCondition() < 40) {
               var4.textureDamage2Enables2[var2] = 1.0F;
            }

            if (var1.window != null && var1.window.isOpen() && var3) {
               var4.textureUninstall2[var2] = 1.0F;
            }
         } else if (var1 != null && var3) {
            var4.textureUninstall2[var2] = 1.0F;
         }
      } catch (Exception var6) {
         var6.printStackTrace();
      }

   }

   private void checkUninstall2(VehiclePart var1, int var2) {
      VehicleModelInstance var3 = (VehicleModelInstance)this.sprite.modelSlot.model;

      try {
         var3.textureUninstall2[var2] = 0.0F;
         if (var1 != null && var1.getInventoryItem() == null) {
            var3.textureUninstall2[var2] = 1.0F;
         }
      } catch (Exception var5) {
         var5.printStackTrace();
      }

   }

   private void doOtherBodyWorkDamage() {
      this.checkDamage(this.getPartById("EngineDoor"), 0, false);
      this.checkDamage(this.getPartById("EngineDoor"), 3, false);
      this.checkDamage(this.getPartById("EngineDoor"), 11, false);
      this.checkDamage2(this.getPartById("EngineDoor"), 6, true);
      this.checkDamage(this.getPartById("TruckBed"), 4, false);
      this.checkDamage(this.getPartById("TruckBed"), 7, false);
      this.checkDamage(this.getPartById("TruckBed"), 15, false);
      VehiclePart var1 = this.getPartById("TrunkDoor");
      if (var1 != null) {
         this.checkDamage2(var1, 10, true);
         if (var1.scriptPart.hasLightsRear) {
            this.checkUninstall2(var1, 12);
            this.checkUninstall2(var1, 1);
            this.checkUninstall2(var1, 5);
            this.checkUninstall2(var1, 9);
         }
      } else {
         var1 = this.getPartById("DoorRear");
         if (var1 != null) {
            this.checkDamage2(var1, 10, true);
            if (var1.scriptPart.hasLightsRear) {
               this.checkUninstall2(var1, 12);
               this.checkUninstall2(var1, 1);
               this.checkUninstall2(var1, 5);
               this.checkUninstall2(var1, 9);
            }
         }
      }

   }

   private void doWindowDamage() {
      this.checkDamage(this.getPartById("WindowFrontLeft"), 2, true);
      this.checkDamage(this.getPartById("WindowFrontRight"), 9, true);
      VehiclePart var1 = this.getPartById("WindowRearLeft");
      if (var1 != null) {
         this.checkDamage(var1, 6, true);
      } else {
         var1 = this.getPartById("WindowMiddleLeft");
         if (var1 != null) {
            this.checkDamage(var1, 6, true);
         }
      }

      var1 = this.getPartById("WindowRearRight");
      if (var1 != null) {
         this.checkDamage(var1, 13, true);
      } else {
         var1 = this.getPartById("WindowMiddleRight");
         if (var1 != null) {
            this.checkDamage(var1, 13, true);
         }
      }

      this.checkDamage(this.getPartById("Windshield"), 10, true);
      this.checkDamage(this.getPartById("WindshieldRear"), 14, true);
   }

   private void doDoorDamage() {
      this.checkDamage(this.getPartById("DoorFrontLeft"), 1, true);
      this.checkDamage(this.getPartById("DoorFrontRight"), 8, true);
      VehiclePart var1 = this.getPartById("DoorRearLeft");
      if (var1 != null) {
         this.checkDamage(var1, 5, true);
      } else {
         var1 = this.getPartById("DoorMiddleLeft");
         if (var1 != null) {
            this.checkDamage(var1, 5, true);
         }
      }

      var1 = this.getPartById("DoorRearRight");
      if (var1 != null) {
         this.checkDamage(var1, 12, true);
      } else {
         var1 = this.getPartById("DoorMiddleRight");
         if (var1 != null) {
            this.checkDamage(var1, 12, true);
         }
      }

   }

   public float getBloodIntensity(String var1) {
      return (float)((Byte)this.bloodIntensity.getOrDefault(var1, BYTE_ZERO) & 255) / 100.0F;
   }

   public void setBloodIntensity(String var1, float var2) {
      byte var3 = (byte)((int)(PZMath.clamp(var2, 0.0F, 1.0F) * 100.0F));
      if (!this.bloodIntensity.containsKey(var1) || var3 != (Byte)this.bloodIntensity.get(var1)) {
         this.bloodIntensity.put(var1, var3);
         this.doBloodOverlay();
         this.transmitBlood();
      }
   }

   public void transmitBlood() {
      if (GameServer.bServer) {
         this.updateFlags = (short)(this.updateFlags | 4096);
      }
   }

   public void doBloodOverlay() {
      if (this.sprite.modelSlot != null) {
         VehicleModelInstance var1 = (VehicleModelInstance)this.sprite.modelSlot.model;
         Arrays.fill(var1.matrixBlood1Enables1, 0.0F);
         Arrays.fill(var1.matrixBlood1Enables2, 0.0F);
         Arrays.fill(var1.matrixBlood2Enables1, 0.0F);
         Arrays.fill(var1.matrixBlood2Enables2, 0.0F);
         if (Core.getInstance().getOptionBloodDecals() != 0) {
            this.doBloodOverlayFront(var1.matrixBlood1Enables1, var1.matrixBlood1Enables2, this.getBloodIntensity("Front"));
            this.doBloodOverlayRear(var1.matrixBlood1Enables1, var1.matrixBlood1Enables2, this.getBloodIntensity("Rear"));
            this.doBloodOverlayLeft(var1.matrixBlood1Enables1, var1.matrixBlood1Enables2, this.getBloodIntensity("Left"));
            this.doBloodOverlayRight(var1.matrixBlood1Enables1, var1.matrixBlood1Enables2, this.getBloodIntensity("Right"));
            Iterator var2 = this.bloodIntensity.entrySet().iterator();

            while(var2.hasNext()) {
               Map.Entry var3 = (Map.Entry)var2.next();
               Integer var4 = (Integer)s_PartToMaskMap.get(var3.getKey());
               if (var4 != null) {
                  var1.matrixBlood1Enables1[var4] = (float)((Byte)var3.getValue() & 255) / 100.0F;
               }
            }

            this.doBloodOverlayAux(var1.matrixBlood2Enables1, var1.matrixBlood2Enables2, 1.0F);
         }
      }
   }

   private void doBloodOverlayAux(float[] var1, float[] var2, float var3) {
      var1[0] = var3;
      var2[6] = var3;
      var2[4] = var3;
      var2[8] = var3;
      var1[4] = var3;
      var1[7] = var3;
      var1[15] = var3;
      var2[10] = var3;
      var2[12] = var3;
      var2[1] = var3;
      var2[5] = var3;
      var2[9] = var3;
      var1[3] = var3;
      var1[8] = var3;
      var1[12] = var3;
      var1[11] = var3;
      var1[1] = var3;
      var1[5] = var3;
      var2[0] = var3;
      var1[10] = var3;
      var1[14] = var3;
      var1[9] = var3;
      var1[13] = var3;
      var1[2] = var3;
      var1[6] = var3;
   }

   private void doBloodOverlayFront(float[] var1, float[] var2, float var3) {
      var1[0] = var3;
      var2[6] = var3;
      var2[4] = var3;
      var2[8] = var3;
      var1[10] = var3;
   }

   private void doBloodOverlayRear(float[] var1, float[] var2, float var3) {
      var1[4] = var3;
      var2[10] = var3;
      var2[12] = var3;
      var2[1] = var3;
      var2[5] = var3;
      var2[9] = var3;
      var1[14] = var3;
   }

   private void doBloodOverlayLeft(float[] var1, float[] var2, float var3) {
      var1[11] = var3;
      var1[1] = var3;
      var1[5] = var3;
      var1[15] = var3;
      var1[2] = var3;
      var1[6] = var3;
   }

   private void doBloodOverlayRight(float[] var1, float[] var2, float var3) {
      var1[3] = var3;
      var1[8] = var3;
      var1[12] = var3;
      var1[7] = var3;
      var1[9] = var3;
      var1[13] = var3;
   }

   public boolean isOnScreen() {
      if (super.isOnScreen()) {
         return true;
      } else if (this.physics == null) {
         return false;
      } else if (this.script == null) {
         return false;
      } else {
         int var1 = IsoCamera.frameState.playerIndex;
         if (this.polyDirty) {
            this.getPoly();
         }

         float var2 = IsoUtils.XToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0F, 0);
         float var3 = IsoUtils.YToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0F, 0);
         float var4 = IsoUtils.XToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0F, 0);
         float var5 = IsoUtils.YToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0F, 0);
         float var6 = IsoUtils.XToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0F, 0);
         float var7 = IsoUtils.YToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0F, 0);
         float var8 = IsoUtils.XToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0F, 0);
         float var9 = IsoUtils.YToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0F, 0);
         float var10 = (this.script.getCenterOfMassOffset().y + this.script.getExtents().y) / 0.8164967F * 24.0F * (float)Core.TileScale;
         float var11 = Core.getInstance().getZoom(var1);
         float var12 = PZMath.min(var2, var4, var6, var8) / var11;
         float var13 = PZMath.max(var2, var4, var6, var8) / var11;
         float var14 = PZMath.min(var3, var5, var7, var9) / var11;
         float var15 = PZMath.max(var3, var5, var7, var9) / var11;
         if (var12 < (float)(IsoCamera.getScreenLeft(var1) + IsoCamera.getScreenWidth(var1)) && var13 > (float)IsoCamera.getScreenLeft(var1) && var14 < (float)(IsoCamera.getScreenTop(var1) + IsoCamera.getScreenHeight(var1)) && var15 > (float)IsoCamera.getScreenTop(var1)) {
            return true;
         } else {
            var3 -= var10;
            var5 -= var10;
            var7 -= var10;
            var9 -= var10;
            var12 = PZMath.min(var2, var4, var6, var8) / var11;
            var13 = PZMath.max(var2, var4, var6, var8) / var11;
            var14 = PZMath.min(var3, var5, var7, var9) / var11;
            var15 = PZMath.max(var3, var5, var7, var9) / var11;
            return var12 < (float)(IsoCamera.getScreenLeft(var1) + IsoCamera.getScreenWidth(var1)) && var13 > (float)IsoCamera.getScreenLeft(var1) && var14 < (float)(IsoCamera.getScreenTop(var1) + IsoCamera.getScreenHeight(var1)) && var15 > (float)IsoCamera.getScreenTop(var1);
         }
      }
   }

   public void render(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      if (this.script != null) {
         if (this.physics != null) {
            this.physics.debug();
         }

         int var8 = IsoCamera.frameState.playerIndex;
         IsoGameCharacter var9 = IsoCamera.getCameraCharacter();
         boolean var10 = var9 != null && var9.getVehicle() == this;
         if (var10 || this.square.lighting[var8].bSeen()) {
            if (!var10 && !this.square.lighting[var8].bCouldSee()) {
               this.setTargetAlpha(var8, 0.0F);
            } else {
               this.setTargetAlpha(var8, 1.0F);
            }

            if (this.sprite.hasActiveModel()) {
               this.updateLights();
               boolean var11 = Core.getInstance().getOptionBloodDecals() != 0;
               if (this.OptionBloodDecals != var11) {
                  this.OptionBloodDecals = var11;
                  this.doBloodOverlay();
               }

               if (var4 == null) {
                  inf.set(1.0F, 1.0F, 1.0F, 1.0F);
               } else {
                  var4.a = this.getAlpha(var8);
                  inf.a = var4.a;
                  inf.r = var4.r;
                  inf.g = var4.g;
                  inf.b = var4.b;
               }

               this.sprite.renderVehicle(this.def, this, var1, var2, 0.0F, 0.0F, 0.0F, inf, true);
            }

            this.updateAlpha(var8);
            if (Core.bDebug && DebugOptions.instance.VehicleRenderArea.getValue()) {
               this.renderAreas();
            }

            if (Core.bDebug && DebugOptions.instance.VehicleRenderAttackPositions.getValue()) {
               this.m_surroundVehicle.render();
            }

            if (Core.bDebug && DebugOptions.instance.VehicleRenderExit.getValue()) {
               this.renderExits();
            }

            if (Core.bDebug && DebugOptions.instance.VehicleRenderIntersectedSquares.getValue()) {
               this.renderIntersectedSquares();
            }

            if (Core.bDebug && DebugOptions.instance.VehicleRenderAuthorizations.getValue()) {
               this.renderAuthorizations();
            }

            if (Core.bDebug && DebugOptions.instance.VehicleRenderInterpolateBuffer.getValue()) {
               this.renderInterpolateBuffer();
            }

            if (DebugOptions.instance.VehicleRenderTrailerPositions.getValue()) {
               this.renderTrailerPositions();
            }

            this.renderUsableArea();
         }
      }
   }

   public void renderlast() {
      int var1 = IsoCamera.frameState.playerIndex;

      for(int var2 = 0; var2 < this.parts.size(); ++var2) {
         VehiclePart var3 = (VehiclePart)this.parts.get(var2);
         if (var3.chatElement != null && var3.chatElement.getHasChatToDisplay()) {
            if (var3.getDeviceData() != null && !var3.getDeviceData().getIsTurnedOn()) {
               var3.chatElement.clear(var1);
            } else {
               float var4 = IsoUtils.XToScreen(this.getX(), this.getY(), this.getZ(), 0);
               float var5 = IsoUtils.YToScreen(this.getX(), this.getY(), this.getZ(), 0);
               var4 = var4 - IsoCamera.getOffX() - this.offsetX;
               var5 = var5 - IsoCamera.getOffY() - this.offsetY;
               var4 += (float)(32 * Core.TileScale);
               var5 += (float)(20 * Core.TileScale);
               var4 /= Core.getInstance().getZoom(var1);
               var5 /= Core.getInstance().getZoom(var1);
               var4 += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsX;
               var5 += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsY;
               var3.chatElement.renderBatched(var1, (int)var4, (int)var5);
            }
         }
      }

   }

   public void renderShadow() {
      if (this.physics != null) {
         if (this.script != null) {
            int var1 = IsoCamera.frameState.playerIndex;
            if (this.square.lighting[var1].bSeen()) {
               if (this.square.lighting[var1].bCouldSee()) {
                  this.setTargetAlpha(var1, 1.0F);
               } else {
                  this.setTargetAlpha(var1, 0.0F);
               }

               Texture var2 = this.getShadowTexture();
               if (var2 != null && var2.isReady() && this.getCurrentSquare() != null) {
                  float var3 = 0.6F * this.getAlpha(var1);
                  ColorInfo var4 = this.getCurrentSquare().lighting[var1].lightInfo();
                  var3 *= (var4.r + var4.g + var4.b) / 3.0F;
                  if (this.polyDirty) {
                     this.getPoly();
                  }

                  if (PerformanceSettings.FBORenderChunk) {
                     float var5 = (float)PZMath.fastfloor(this.getZ());
                     if (this.current != null && this.current.hasSlopedSurface()) {
                        var5 = this.current.getApparentZ(this.getX() % 1.0F, this.getY() % 1.0F);
                     }

                     FBORenderShadows.getInstance().addShadow(this.getX(), this.getY(), var5, this.shadowCoord.x2, this.shadowCoord.y2, this.shadowCoord.x1, this.shadowCoord.y1, this.shadowCoord.x4, this.shadowCoord.y4, this.shadowCoord.x3, this.shadowCoord.y3, 1.0F, 1.0F, 1.0F, 0.8F * var3, var2, true);
                     return;
                  }

                  SpriteRenderer.instance.renderPoly(var2, (float)((int)IsoUtils.XToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0F, 0)), (float)((int)IsoUtils.YToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0F, 0)), (float)((int)IsoUtils.XToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0F, 0)), (float)((int)IsoUtils.YToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0F, 0)), (float)((int)IsoUtils.XToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0F, 0)), (float)((int)IsoUtils.YToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0F, 0)), (float)((int)IsoUtils.XToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0F, 0)), (float)((int)IsoUtils.YToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0F, 0)), 1.0F, 1.0F, 1.0F, 0.8F * var3);
               }

            }
         }
      }
   }

   public boolean isEnterBlocked(IsoGameCharacter var1, int var2) {
      return this.isExitBlocked(var1, var2);
   }

   public boolean isExitBlocked(int var1) {
      VehicleScript.Position var2 = this.getPassengerPosition(var1, "inside");
      VehicleScript.Position var3 = this.getPassengerPosition(var1, "outside");
      if (var2 != null && var3 != null) {
         Vector3f var4 = this.getPassengerPositionWorldPos(var3, (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
         if (var3.area != null) {
            Vector2 var5 = (Vector2)((Vector2ObjectPool)TL_vector2_pool.get()).alloc();
            VehicleScript.Area var6 = this.script.getAreaById(var3.area);
            Vector2 var7 = this.areaPositionWorld4PlayerInteract(var6, var5);
            if (var7 != null) {
               var4.x = var7.x;
               var4.y = var7.y;
            }

            ((Vector2ObjectPool)TL_vector2_pool.get()).release(var5);
         }

         var4.z = 0.0F;
         Vector3f var8 = this.getPassengerPositionWorldPos(var2, (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
         boolean var9 = PolygonalMap2.instance.lineClearCollide(var8.x, var8.y, var4.x, var4.y, PZMath.fastfloor(this.getZ()), this, false, false);
         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var4);
         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var8);
         return var9;
      } else {
         return true;
      }
   }

   public boolean isExitBlocked(IsoGameCharacter var1, int var2) {
      VehicleScript.Position var3 = this.getPassengerPosition(var2, "inside");
      VehicleScript.Position var4 = this.getPassengerPosition(var2, "outside");
      if (var3 != null && var4 != null) {
         Vector3f var5 = this.getPassengerPositionWorldPos(var4, (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
         if (var4.area != null) {
            Vector2 var6 = (Vector2)((Vector2ObjectPool)TL_vector2_pool.get()).alloc();
            VehicleScript.Area var7 = this.script.getAreaById(var4.area);
            Vector2 var8 = this.areaPositionWorld4PlayerInteract(var7, var6);
            if (var8 != null) {
               var5.x = var8.x;
               var5.y = var8.y;
            }

            ((Vector2ObjectPool)TL_vector2_pool.get()).release(var6);
         }

         var5.z = 0.0F;
         Vector3f var9 = this.getPassengerPositionWorldPos(var3, (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
         boolean var10 = PolygonalMap2.instance.lineClearCollide(var9.x, var9.y, var5.x, var5.y, (int)this.getZ(), this, false, false);
         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var5);
         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var9);
         if (!var10 && GameClient.bClient) {
            IsoGridSquare var11 = IsoWorld.instance.CurrentCell.getGridSquare((double)var5.x, (double)var5.y, (double)var5.z);
            if (var11 != null && var1 instanceof IsoPlayer && !SafeHouse.isPlayerAllowedOnSquare((IsoPlayer)var1, var11)) {
               var10 = true;
            }
         }

         return var10;
      } else {
         return true;
      }
   }

   public boolean isPassengerUseDoor2(IsoGameCharacter var1, int var2) {
      VehicleScript.Position var3 = this.getPassengerPosition(var2, "outside2");
      if (var3 != null) {
         Vector3f var4 = this.getPassengerPositionWorldPos(var3, (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
         var4.sub(var1.getX(), var1.getY(), var1.getZ());
         float var5 = var4.length();
         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var4);
         if (var5 < 2.0F) {
            return true;
         }
      }

      return false;
   }

   public boolean isEnterBlocked2(IsoGameCharacter var1, int var2) {
      return this.isExitBlocked2(var2);
   }

   public boolean isExitBlocked2(int var1) {
      VehicleScript.Position var2 = this.getPassengerPosition(var1, "inside");
      VehicleScript.Position var3 = this.getPassengerPosition(var1, "outside2");
      if (var2 != null && var3 != null) {
         Vector3f var4 = this.getPassengerPositionWorldPos(var3, (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
         var4.z = 0.0F;
         Vector3f var5 = this.getPassengerPositionWorldPos(var2, (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
         boolean var6 = PolygonalMap2.instance.lineClearCollide(var5.x, var5.y, var4.x, var4.y, PZMath.fastfloor(this.getZ()), this, false, false);
         IsoGridSquare var7 = IsoWorld.instance.CurrentCell.getGridSquare((double)var4.x, (double)var4.y, (double)var4.z);
         IsoGameCharacter var8 = this.getCharacter(var1);
         if (var8 instanceof IsoPlayer && !SafeHouse.isPlayerAllowedOnSquare((IsoPlayer)var8, var7)) {
            var6 = true;
         }

         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var4);
         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var5);
         return var6;
      } else {
         return true;
      }
   }

   private void renderExits() {
      int var1 = Core.TileScale;
      Vector3f var2 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
      Vector3f var3 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();

      for(int var4 = 0; var4 < this.getMaxPassengers(); ++var4) {
         VehicleScript.Position var5 = this.getPassengerPosition(var4, "inside");
         VehicleScript.Position var6 = this.getPassengerPosition(var4, "outside");
         if (var5 != null && var6 != null) {
            float var7 = 0.3F;
            this.getPassengerPositionWorldPos(var6, var2);
            this.getPassengerPositionWorldPos(var5, var3);
            int var8 = PZMath.fastfloor(var2.x - var7);
            int var9 = PZMath.fastfloor(var2.x + var7);
            int var10 = PZMath.fastfloor(var2.y - var7);
            int var11 = PZMath.fastfloor(var2.y + var7);

            float var14;
            for(int var12 = var10; var12 <= var11; ++var12) {
               for(int var13 = var8; var13 <= var9; ++var13) {
                  var14 = IsoUtils.XToScreenExact((float)var13, (float)(var12 + 1), (float)PZMath.fastfloor(this.getZ()), 0);
                  float var15 = IsoUtils.YToScreenExact((float)var13, (float)(var12 + 1), (float)PZMath.fastfloor(this.getZ()), 0);
                  if (PerformanceSettings.FBORenderChunk) {
                     var14 += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsX * IsoCamera.frameState.zoom;
                     var15 += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsY * IsoCamera.frameState.zoom;
                  }

                  IndieGL.glBlendFunc(770, 771);
                  SpriteRenderer.instance.renderPoly(var14, var15, var14 + (float)(32 * var1), var15 - (float)(16 * var1), var14 + (float)(64 * var1), var15, var14 + (float)(32 * var1), var15 + (float)(16 * var1), 1.0F, 1.0F, 1.0F, 0.5F);
               }
            }

            float var16 = 1.0F;
            float var17 = 1.0F;
            var14 = 1.0F;
            if (this.isExitBlocked(var4)) {
               var14 = 0.0F;
               var17 = 0.0F;
            }

            this.getController().drawCircle(var3.x, var3.y, var7, 0.0F, 0.0F, 1.0F, 1.0F);
            this.getController().drawCircle(var2.x, var2.y, var7, var16, var17, var14, 1.0F);
         }
      }

      ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var2);
      ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var3);
   }

   private Vector2 areaPositionLocal(VehicleScript.Area var1) {
      return this.areaPositionLocal(var1, new Vector2());
   }

   private Vector2 areaPositionLocal(VehicleScript.Area var1, Vector2 var2) {
      Vector2 var3 = this.areaPositionWorld(var1, var2);
      Vector3f var4 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
      this.getLocalPos(var3.x, var3.y, 0.0F, var4);
      var3.set(var4.x, var4.z);
      ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var4);
      return var3;
   }

   public Vector2 areaPositionWorld(VehicleScript.Area var1) {
      return this.areaPositionWorld(var1, new Vector2());
   }

   public Vector2 areaPositionWorld(VehicleScript.Area var1, Vector2 var2) {
      if (var1 == null) {
         return null;
      } else {
         Vector3f var3 = this.getWorldPos(var1.x, 0.0F, var1.y, (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
         var2.set(var3.x, var3.y);
         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var3);
         return var2;
      }
   }

   public Vector2 areaPositionWorld4PlayerInteract(VehicleScript.Area var1) {
      return this.areaPositionWorld4PlayerInteract(var1, new Vector2());
   }

   public Vector2 areaPositionWorld4PlayerInteract(VehicleScript.Area var1, Vector2 var2) {
      Vector3f var3 = this.script.getExtents();
      Vector3f var4 = this.script.getCenterOfMassOffset();
      Vector2 var5 = this.areaPositionWorld(var1, var2);
      Vector3f var6 = this.getLocalPos(var5.x, var5.y, 0.0F, (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
      if (!(var1.x > var4.x + var3.x / 2.0F) && !(var1.x < var4.x - var3.x / 2.0F)) {
         if (var1.y > 0.0F) {
            var6.z -= var1.h * 0.3F;
         } else {
            var6.z += var1.h * 0.3F;
         }
      } else if (var1.x > 0.0F) {
         var6.x -= var1.w * 0.3F;
      } else {
         var6.x += var1.w * 0.3F;
      }

      this.getWorldPos(var6, var6);
      var2.set(var6.x, var6.y);
      ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var6);
      return var2;
   }

   private void renderAreas() {
      if (this.getScript() != null) {
         Vector3f var1 = this.getForwardVector((Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
         Vector2 var2 = (Vector2)((Vector2ObjectPool)TL_vector2_pool.get()).alloc();

         for(int var3 = 0; var3 < this.parts.size(); ++var3) {
            VehiclePart var4 = (VehiclePart)this.parts.get(var3);
            if (var4.getArea() != null) {
               VehicleScript.Area var5 = this.getScript().getAreaById(var4.getArea());
               if (var5 != null) {
                  Vector2 var6 = this.areaPositionWorld(var5, var2);
                  if (var6 != null) {
                     boolean var7 = this.isInArea(var5.id, (IsoGameCharacter)IsoPlayer.getInstance());
                     this.getController().drawRect(var1, var6.x - WorldSimulation.instance.offsetX, var6.y - WorldSimulation.instance.offsetY, var5.w, var5.h / 2.0F, var7 ? 0.0F : 0.65F, var7 ? 1.0F : 0.65F, var7 ? 1.0F : 0.65F);
                     var6 = this.areaPositionWorld4PlayerInteract(var5, var2);
                     this.getController().drawRect(var1, var6.x - WorldSimulation.instance.offsetX, var6.y - WorldSimulation.instance.offsetY, 0.1F, 0.1F, 1.0F, 0.0F, 0.0F);
                  }
               }
            }
         }

         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var1);
         ((Vector2ObjectPool)TL_vector2_pool.get()).release(var2);
         LineDrawer.drawLine(IsoUtils.XToScreenExact(this.poly.x1, this.poly.y1, 0.0F, 0), IsoUtils.YToScreenExact(this.poly.x1, this.poly.y1, 0.0F, 0), IsoUtils.XToScreenExact(this.poly.x2, this.poly.y2, 0.0F, 0), IsoUtils.YToScreenExact(this.poly.x2, this.poly.y2, 0.0F, 0), 1.0F, 0.5F, 0.5F, 1.0F, 0);
         LineDrawer.drawLine(IsoUtils.XToScreenExact(this.poly.x2, this.poly.y2, 0.0F, 0), IsoUtils.YToScreenExact(this.poly.x2, this.poly.y2, 0.0F, 0), IsoUtils.XToScreenExact(this.poly.x3, this.poly.y3, 0.0F, 0), IsoUtils.YToScreenExact(this.poly.x3, this.poly.y3, 0.0F, 0), 1.0F, 0.5F, 0.5F, 1.0F, 0);
         LineDrawer.drawLine(IsoUtils.XToScreenExact(this.poly.x3, this.poly.y3, 0.0F, 0), IsoUtils.YToScreenExact(this.poly.x3, this.poly.y3, 0.0F, 0), IsoUtils.XToScreenExact(this.poly.x4, this.poly.y4, 0.0F, 0), IsoUtils.YToScreenExact(this.poly.x4, this.poly.y4, 0.0F, 0), 1.0F, 0.5F, 0.5F, 1.0F, 0);
         LineDrawer.drawLine(IsoUtils.XToScreenExact(this.poly.x4, this.poly.y4, 0.0F, 0), IsoUtils.YToScreenExact(this.poly.x4, this.poly.y4, 0.0F, 0), IsoUtils.XToScreenExact(this.poly.x1, this.poly.y1, 0.0F, 0), IsoUtils.YToScreenExact(this.poly.x1, this.poly.y1, 0.0F, 0), 1.0F, 0.5F, 0.5F, 1.0F, 0);
         LineDrawer.drawLine(IsoUtils.XToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0F, 0), IsoUtils.YToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0F, 0), IsoUtils.XToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0F, 0), IsoUtils.YToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0F, 0), 0.5F, 1.0F, 0.5F, 1.0F, 0);
         LineDrawer.drawLine(IsoUtils.XToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0F, 0), IsoUtils.YToScreenExact(this.shadowCoord.x2, this.shadowCoord.y2, 0.0F, 0), IsoUtils.XToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0F, 0), IsoUtils.YToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0F, 0), 0.5F, 1.0F, 0.5F, 1.0F, 0);
         LineDrawer.drawLine(IsoUtils.XToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0F, 0), IsoUtils.YToScreenExact(this.shadowCoord.x3, this.shadowCoord.y3, 0.0F, 0), IsoUtils.XToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0F, 0), IsoUtils.YToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0F, 0), 0.5F, 1.0F, 0.5F, 1.0F, 0);
         LineDrawer.drawLine(IsoUtils.XToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0F, 0), IsoUtils.YToScreenExact(this.shadowCoord.x4, this.shadowCoord.y4, 0.0F, 0), IsoUtils.XToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0F, 0), IsoUtils.YToScreenExact(this.shadowCoord.x1, this.shadowCoord.y1, 0.0F, 0), 0.5F, 1.0F, 0.5F, 1.0F, 0);
      }
   }

   private void renderInterpolateBuffer() {
      if (this.netPlayerAuthorization == BaseVehicle.Authorization.Remote) {
         float var1 = IsoUtils.XToScreenExact(this.getX(), this.getY(), 0.0F, 0);
         float var2 = IsoUtils.YToScreenExact(this.getX(), this.getY(), 0.0F, 0);
         float var3 = var1 - 310.0F;
         float var4 = var2 + 22.0F;
         float var5 = 300.0F;
         float var6 = 150.0F;
         float var7 = 4.0F;
         Color var8 = Color.lightGray;
         Color var9 = Color.green;
         Color var10 = Color.cyan;
         Color var11 = Color.yellow;
         Color var12 = Color.blue;
         Color var13 = Color.red;
         LineDrawer.drawLine(var3, var4, var3 + var5, var4, var8.r, var8.g, var8.b, var8.a, 1);
         LineDrawer.drawLine(var3, var4 + var6, var3 + var5, var4 + var6, var8.r, var8.g, var8.b, var8.a, 1);
         long var14 = GameTime.getServerTimeMills();
         long var16 = var14 - 150L - (long)this.interpolation.history;
         long var18 = var14 + 150L;
         this.renderInterpolateBuffer_drawVertLine(var16, var8, var3, var4, var5, var6, var16, var18, true);
         this.renderInterpolateBuffer_drawVertLine(var18, var8, var3, var4, var5, var6, var16, var18, true);
         this.renderInterpolateBuffer_drawVertLine(var14, var9, var3, var4, var5, var6, var16, var18, true);
         this.renderInterpolateBuffer_drawVertLine(var14 - (long)this.interpolation.delay, var10, var3, var4, var5, var6, var16, var18, true);
         this.renderInterpolateBuffer_drawPoint(var14 - (long)this.interpolation.delay, this.getX(), var12, 5, var3, var4, var5, var6, var16, var18, this.getX() - var7, this.getX() + var7);
         this.renderInterpolateBuffer_drawPoint(var14 - (long)this.interpolation.delay, this.getY(), var13, 5, var3, var4, var5, var6, var16, var18, this.getY() - var7, this.getY() + var7);
         long var20 = 0L;
         float var22 = 0.0F / 0.0F;
         float var23 = 0.0F / 0.0F;
         VehicleInterpolationData var25 = new VehicleInterpolationData();
         var25.time = var14 - (long)this.interpolation.delay;
         VehicleInterpolationData var26 = (VehicleInterpolationData)this.interpolation.buffer.higher(var25);
         VehicleInterpolationData var27 = (VehicleInterpolationData)this.interpolation.buffer.floor(var25);

         VehicleInterpolationData var29;
         for(Iterator var28 = this.interpolation.buffer.iterator(); var28.hasNext(); var23 = var29.y) {
            var29 = (VehicleInterpolationData)var28.next();
            boolean var24 = (var29.hashCode() & 1) == 0;
            this.renderInterpolateBuffer_drawVertLine(var29.time, var11, var3, var4, var5, var6, var16, var18, var24);
            if (var29 == var26) {
               this.renderInterpolateBuffer_drawTextHL(var29.time, "H", var10, var3, var4, var5, var6, var16, var18);
            }

            if (var29 == var27) {
               this.renderInterpolateBuffer_drawTextHL(var29.time, "L", var10, var3, var4, var5, var6, var16, var18);
            }

            this.renderInterpolateBuffer_drawPoint(var29.time, var29.x, var12, 5, var3, var4, var5, var6, var16, var18, this.getX() - var7, this.getX() + var7);
            this.renderInterpolateBuffer_drawPoint(var29.time, var29.y, var13, 5, var3, var4, var5, var6, var16, var18, this.getY() - var7, this.getY() + var7);
            if (!Float.isNaN(var22)) {
               this.renderInterpolateBuffer_drawLine(var20, var22, var29.time, var29.x, var12, var3, var4, var5, var6, var16, var18, this.getX() - var7, this.getX() + var7);
               this.renderInterpolateBuffer_drawLine(var20, var23, var29.time, var29.y, var13, var3, var4, var5, var6, var16, var18, this.getY() - var7, this.getY() + var7);
            }

            var20 = var29.time;
            var22 = var29.x;
         }

         float[] var31 = new float[27];
         float[] var32 = new float[2];
         boolean var30 = this.interpolation.interpolationDataGet(var31, var32, var14 - (long)this.interpolation.delay);
         TextManager.instance.DrawString((double)var3, (double)(var4 + var6 + 20.0F), String.format("interpolationDataGet=%s", var30 ? "True" : "False"), (double)var10.r, (double)var10.g, (double)var10.b, (double)var10.a);
         TextManager.instance.DrawString((double)var3, (double)(var4 + var6 + 30.0F), String.format("buffer.size=%d buffering=%s", this.interpolation.buffer.size(), String.valueOf(this.interpolation.buffering)), (double)var10.r, (double)var10.g, (double)var10.b, (double)var10.a);
         TextManager.instance.DrawString((double)var3, (double)(var4 + var6 + 40.0F), String.format("delayTarget=%d", this.interpolation.delayTarget), (double)var10.r, (double)var10.g, (double)var10.b, (double)var10.a);
         if (this.interpolation.buffer.size() >= 2) {
            TextManager.instance.DrawString((double)var3, (double)(var4 + var6 + 50.0F), String.format("last=%d first=%d", ((VehicleInterpolationData)this.interpolation.buffer.last()).time, ((VehicleInterpolationData)this.interpolation.buffer.first()).time), (double)var10.r, (double)var10.g, (double)var10.b, (double)var10.a);
            TextManager.instance.DrawString((double)var3, (double)(var4 + var6 + 60.0F), String.format("(last-first).time=%d delay=%d", ((VehicleInterpolationData)this.interpolation.buffer.last()).time - ((VehicleInterpolationData)this.interpolation.buffer.first()).time, this.interpolation.delay), (double)var10.r, (double)var10.g, (double)var10.b, (double)var10.a);
         }

      }
   }

   private void renderInterpolateBuffer_drawTextHL(long var1, String var3, Color var4, float var5, float var6, float var7, float var8, long var9, long var11) {
      float var13 = var7 / (float)(var11 - var9);
      float var14 = (float)(var1 - var9) * var13;
      TextManager.instance.DrawString((double)(var14 + var5), (double)var6, var3, (double)var4.r, (double)var4.g, (double)var4.b, (double)var4.a);
   }

   private void renderInterpolateBuffer_drawVertLine(long var1, Color var3, float var4, float var5, float var6, float var7, long var8, long var10, boolean var12) {
      float var13 = var6 / (float)(var10 - var8);
      float var14 = (float)(var1 - var8) * var13;
      LineDrawer.drawLine(var14 + var4, var5, var14 + var4, var5 + var7, var3.r, var3.g, var3.b, var3.a, 1);
      TextManager.instance.DrawString((double)(var14 + var4), (double)(var5 + var7 + (var12 ? 0.0F : 10.0F)), String.format("%.1f", (float)(var1 - var1 / 100000L * 100000L) / 1000.0F), (double)var3.r, (double)var3.g, (double)var3.b, (double)var3.a);
   }

   private void renderInterpolateBuffer_drawLine(long var1, float var3, long var4, float var6, Color var7, float var8, float var9, float var10, float var11, long var12, long var14, float var16, float var17) {
      float var18 = var10 / (float)(var14 - var12);
      float var19 = (float)(var1 - var12) * var18;
      float var20 = (float)(var4 - var12) * var18;
      float var21 = var11 / (var17 - var16);
      float var22 = (var3 - var16) * var21;
      float var23 = (var6 - var16) * var21;
      LineDrawer.drawLine(var19 + var8, var22 + var9, var20 + var8, var23 + var9, var7.r, var7.g, var7.b, var7.a, 1);
   }

   private void renderInterpolateBuffer_drawPoint(long var1, float var3, Color var4, int var5, float var6, float var7, float var8, float var9, long var10, long var12, float var14, float var15) {
      float var16 = var8 / (float)(var12 - var10);
      float var17 = (float)(var1 - var10) * var16;
      float var18 = var9 / (var15 - var14);
      float var19 = (var3 - var14) * var18;
      LineDrawer.drawCircle(var17 + var6, var19 + var7, (float)var5, 10, var4.r, var4.g, var4.b);
   }

   private void renderAuthorizations() {
      float var1 = 0.3F;
      float var2 = 0.3F;
      float var3 = 0.3F;
      float var4 = 0.5F;
      switch (this.netPlayerAuthorization) {
         case Server:
            var1 = 1.0F;
            break;
         case LocalCollide:
            var3 = 1.0F;
            break;
         case Local:
            var2 = 1.0F;
            break;
         case Remote:
            var2 = 1.0F;
            var1 = 1.0F;
            break;
         case RemoteCollide:
            var3 = 1.0F;
            var1 = 1.0F;
      }

      LineDrawer.drawLine(IsoUtils.XToScreenExact(this.poly.x1, this.poly.y1, 0.0F, 0), IsoUtils.YToScreenExact(this.poly.x1, this.poly.y1, 0.0F, 0), IsoUtils.XToScreenExact(this.poly.x2, this.poly.y2, 0.0F, 0), IsoUtils.YToScreenExact(this.poly.x2, this.poly.y2, 0.0F, 0), var1, var2, var3, var4, 1);
      LineDrawer.drawLine(IsoUtils.XToScreenExact(this.poly.x2, this.poly.y2, 0.0F, 0), IsoUtils.YToScreenExact(this.poly.x2, this.poly.y2, 0.0F, 0), IsoUtils.XToScreenExact(this.poly.x3, this.poly.y3, 0.0F, 0), IsoUtils.YToScreenExact(this.poly.x3, this.poly.y3, 0.0F, 0), var1, var2, var3, var4, 1);
      LineDrawer.drawLine(IsoUtils.XToScreenExact(this.poly.x3, this.poly.y3, 0.0F, 0), IsoUtils.YToScreenExact(this.poly.x3, this.poly.y3, 0.0F, 0), IsoUtils.XToScreenExact(this.poly.x4, this.poly.y4, 0.0F, 0), IsoUtils.YToScreenExact(this.poly.x4, this.poly.y4, 0.0F, 0), var1, var2, var3, var4, 1);
      LineDrawer.drawLine(IsoUtils.XToScreenExact(this.poly.x4, this.poly.y4, 0.0F, 0), IsoUtils.YToScreenExact(this.poly.x4, this.poly.y4, 0.0F, 0), IsoUtils.XToScreenExact(this.poly.x1, this.poly.y1, 0.0F, 0), IsoUtils.YToScreenExact(this.poly.x1, this.poly.y1, 0.0F, 0), var1, var2, var3, var4, 1);
      float var5 = 0.0F;
      if (this.getVehicleTowing() != null) {
         Vector3fObjectPool var6 = (Vector3fObjectPool)TL_vector3f_pool.get();
         Vector3f var7 = (Vector3f)var6.alloc();
         Vector3f var8 = this.getTowingWorldPos(this.getTowAttachmentSelf(), var7);
         Vector3f var9 = (Vector3f)var6.alloc();
         Vector3f var10 = this.getVehicleTowing().getTowingWorldPos(this.getVehicleTowing().getTowAttachmentSelf(), var9);
         if (var8 != null && var10 != null) {
            LineDrawer.DrawIsoLine(var8.x, var8.y, var8.z, var10.x, var10.y, var10.z, var1, var2, var3, var4, 1);
            LineDrawer.DrawIsoCircle(var8.x, var8.y, var8.z, 0.2F, 16, var1, var2, var3, var4);
            var5 = IsoUtils.DistanceTo(var8.x, var8.y, var8.z, var10.x, var10.y, var10.z);
         }

         var6.release(var7);
         var6.release(var9);
      }

      var1 = 1.0F;
      var2 = 1.0F;
      var3 = 0.75F;
      var4 = 1.0F;
      float var11 = 10.0F;
      float var12 = IsoUtils.XToScreenExact(this.getX(), this.getY(), 0.0F, 0);
      float var13 = IsoUtils.YToScreenExact(this.getX(), this.getY(), 0.0F, 0);
      IsoPlayer var14 = (IsoPlayer)GameClient.IDToPlayerMap.get(this.netPlayerId);
      String var10000 = var14 == null ? "@server" : var14.getUsername();
      String var15 = var10000 + " ( " + this.netPlayerId + " )";
      TextManager var16 = TextManager.instance;
      double var10001 = (double)var12;
      double var10002 = (double)(var13 + (var11 += 12.0F));
      String var10003 = this.getScriptName();
      var16.DrawString(var10001, var10002, "VID: " + var10003 + " ( " + this.getId() + " )", (double)var1, (double)var2, (double)var3, (double)var4);
      TextManager.instance.DrawString((double)var12, (double)(var13 + (var11 += 12.0F)), "PID: " + var15, (double)var1, (double)var2, (double)var3, (double)var4);
      var16 = TextManager.instance;
      var10001 = (double)var12;
      var10002 = (double)(var13 + (var11 += 12.0F));
      var10003 = this.netPlayerAuthorization.name();
      var16.DrawString(var10001, var10002, "Auth: " + var10003, (double)var1, (double)var2, (double)var3, (double)var4);
      var10001 = (double)var12;
      var10002 = (double)(var13 + (var11 += 12.0F));
      boolean var17 = this.isStatic;
      TextManager.instance.DrawString(var10001, var10002, "Static/active: " + var17 + "/" + this.isActive, (double)var1, (double)var2, (double)var3, (double)var4);
      var16 = TextManager.instance;
      var10001 = (double)var12;
      var10002 = (double)(var13 + (var11 += 12.0F));
      float var18 = this.getX();
      var16.DrawString(var10001, var10002, "x=" + var18 + " / y=" + this.getY(), (double)var1, (double)var2, (double)var3, (double)var4);
      TextManager.instance.DrawString((double)var12, (double)(var13 + (var11 += 14.0F)), String.format("Passengers: %d/%d", Arrays.stream(this.passengers).filter((var0) -> {
         return var0.character != null;
      }).count(), this.passengers.length), (double)var1, (double)var2, (double)var3, (double)var4);
      TextManager.instance.DrawString((double)var12, (double)(var13 + (var11 += 12.0F)), String.format("Speed: %s%.3f kmph", this.getCurrentSpeedKmHour() >= 0.0F ? "+" : "", this.getCurrentSpeedKmHour()), (double)var1, (double)var2, (double)var3, (double)var4);
      TextManager.instance.DrawString((double)var12, (double)(var13 + (var11 += 12.0F)), String.format("Engine speed: %.3f", this.engineSpeed), (double)var1, (double)var2, (double)var3, (double)var4);
      TextManager.instance.DrawString((double)var12, (double)(var13 + (var11 += 12.0F)), String.format("Mass: %.3f/%.3f", this.getMass(), this.getFudgedMass()), (double)var1, (double)var2, (double)var3, (double)var4);
      if (var5 > 1.5F) {
         var2 = 0.75F;
      }

      if (this.getVehicleTowing() != null) {
         TextManager.instance.DrawString((double)var12, (double)(var13 + (var11 += 14.0F)), "Towing: " + this.getVehicleTowing().getId(), (double)var1, (double)var2, (double)var3, (double)var4);
         TextManager.instance.DrawString((double)var12, (double)(var13 + (var11 += 12.0F)), String.format("Distance: %.3f", var5), (double)var1, (double)var2, (double)var3, (double)var4);
      }

      if (this.getVehicleTowedBy() != null) {
         TextManager.instance.DrawString((double)var12, (double)(var13 + (var11 += 14.0F)), "TowedBy: " + this.getVehicleTowedBy().getId(), (double)var1, (double)var2, (double)var3, (double)var4);
         TextManager.instance.DrawString((double)var12, (double)(var13 + (var11 += 12.0F)), String.format("Distance: %.3f", var5), (double)var1, (double)var2, (double)var3, (double)var4);
      }

   }

   private void renderUsableArea() {
      if (this.getScript() != null && UIManager.VisibleAllUI) {
         VehiclePart var1 = this.getUseablePart(IsoPlayer.getInstance());
         if (var1 != null) {
            VehicleScript.Area var2 = this.getScript().getAreaById(var1.getArea());
            if (var2 != null) {
               Vector2 var3 = (Vector2)((Vector2ObjectPool)TL_vector2_pool.get()).alloc();
               Vector2 var4 = this.areaPositionWorld(var2, var3);
               if (var4 == null) {
                  ((Vector2ObjectPool)TL_vector2_pool.get()).release(var3);
               } else {
                  Vector3f var5 = this.getForwardVector((Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
                  float var6 = Core.getInstance().getGoodHighlitedColor().getR();
                  float var7 = Core.getInstance().getGoodHighlitedColor().getG();
                  float var8 = Core.getInstance().getGoodHighlitedColor().getB();
                  this.getController().drawRect(var5, var4.x - WorldSimulation.instance.offsetX, var4.y - WorldSimulation.instance.offsetY, var2.w, var2.h / 2.0F, var6, var7, var8);
                  var5.x *= var2.h / this.script.getModelScale();
                  var5.z *= var2.h / this.script.getModelScale();
                  if (var1.getDoor() != null && (var1.getId().contains("Left") || var1.getId().contains("Right"))) {
                     if (var1.getId().contains("Front")) {
                        this.getController().drawRect(var5, var4.x - WorldSimulation.instance.offsetX + var5.x * var2.h / 2.0F, var4.y - WorldSimulation.instance.offsetY + var5.z * var2.h / 2.0F, var2.w, var2.h / 8.0F, var6, var7, var8);
                     } else if (var1.getId().contains("Rear")) {
                        this.getController().drawRect(var5, var4.x - WorldSimulation.instance.offsetX - var5.x * var2.h / 2.0F, var4.y - WorldSimulation.instance.offsetY - var5.z * var2.h / 2.0F, var2.w, var2.h / 8.0F, var6, var7, var8);
                     }
                  }

                  ((Vector2ObjectPool)TL_vector2_pool.get()).release(var3);
                  ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var5);
               }
            }
         }
      }
   }

   private void renderIntersectedSquares() {
      VehiclePoly var1 = this.getPoly();
      float var2 = Math.min(var1.x1, Math.min(var1.x2, Math.min(var1.x3, var1.x4)));
      float var3 = Math.min(var1.y1, Math.min(var1.y2, Math.min(var1.y3, var1.y4)));
      float var4 = Math.max(var1.x1, Math.max(var1.x2, Math.max(var1.x3, var1.x4)));
      float var5 = Math.max(var1.y1, Math.max(var1.y2, Math.max(var1.y3, var1.y4)));

      for(int var6 = (int)var3; var6 < (int)Math.ceil((double)var5); ++var6) {
         for(int var7 = (int)var2; var7 < (int)Math.ceil((double)var4); ++var7) {
            if (this.isIntersectingSquare(var7, var6, PZMath.fastfloor(this.getZ()))) {
               LineDrawer.addLine((float)var7, (float)var6, (float)PZMath.fastfloor(this.getZ()), (float)(var7 + 1), (float)(var6 + 1), (float)PZMath.fastfloor(this.getZ()), 1.0F, 1.0F, 1.0F, (String)null, false);
            }
         }
      }

   }

   private void renderTrailerPositions() {
      if (this.script != null && this.physics != null) {
         Vector3f var1 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
         Vector3f var2 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
         Vector3f var3 = this.getTowingWorldPos("trailer", var2);
         if (var3 != null) {
            this.physics.drawCircle(var3.x, var3.y, 0.3F, 1.0F, 1.0F, 1.0F, 1.0F);
         }

         Vector3f var4 = this.getPlayerTrailerLocalPos("trailer", false, var1);
         boolean var5;
         if (var4 != null) {
            this.getWorldPos(var4, var4);
            var5 = PolygonalMap2.instance.lineClearCollide(var2.x, var2.y, var4.x, var4.y, PZMath.fastfloor(this.getZ()), this, false, false);
            this.physics.drawCircle(var4.x, var4.y, 0.3F, 1.0F, var5 ? 0.0F : 1.0F, var5 ? 0.0F : 1.0F, 1.0F);
            if (var5) {
               LineDrawer.addLine(var4.x, var4.y, 0.0F, var2.x, var2.y, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F);
            }
         }

         var4 = this.getPlayerTrailerLocalPos("trailer", true, var1);
         if (var4 != null) {
            this.getWorldPos(var4, var4);
            var5 = PolygonalMap2.instance.lineClearCollide(var2.x, var2.y, var4.x, var4.y, PZMath.fastfloor(this.getZ()), this, false, false);
            this.physics.drawCircle(var4.x, var4.y, 0.3F, 1.0F, var5 ? 0.0F : 1.0F, var5 ? 0.0F : 1.0F, 1.0F);
            if (var5) {
               LineDrawer.addLine(var4.x, var4.y, 0.0F, var2.x, var2.y, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F);
            }
         }

         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var1);
         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var2);
      }
   }

   public void getWheelForwardVector(int var1, Vector3f var2) {
      WheelInfo var3 = this.wheelInfo[var1];
      Matrix4f var4 = (Matrix4f)((Matrix4fObjectPool)TL_matrix4f_pool.get()).alloc();
      var4.rotationY(var3.steering);
      Matrix4f var5 = this.jniTransform.getMatrix((Matrix4f)((Matrix4fObjectPool)TL_matrix4f_pool.get()).alloc());
      var5.setTranslation(0.0F, 0.0F, 0.0F);
      var4.mul(var5, var4);
      ((Matrix4fObjectPool)TL_matrix4f_pool.get()).release(var5);
      ((Matrix4fObjectPool)TL_matrix4f_pool.get()).release(var4);
      Vector4f var6 = allocVector4f();
      var4.getColumn(2, var6);
      var2.set(var6.x, 0.0F, var6.z);
      releaseVector4f(var6);
   }

   public void tryStartEngine(boolean var1) {
      if (this.getDriver() == null || !(this.getDriver() instanceof IsoPlayer) || !((IsoPlayer)this.getDriver()).isBlockMovement()) {
         VehiclePart var2 = this.getPartById("Engine");
         if (var2 != null && var2.getCondition() > 0) {
            if (this.getEngineQuality() > 0) {
               if (this.engineState == BaseVehicle.engineStateTypes.Idle) {
                  if ((!Core.bDebug || !DebugOptions.instance.Cheat.Vehicle.StartWithoutKey.getValue()) && !SandboxOptions.instance.VehicleEasyUse.getValue() && !this.isKeysInIgnition() && !var1 && this.getDriver().getInventory().haveThisKeyId(this.getKeyId()) == null && !this.isHotwired()) {
                     if (GameServer.bServer) {
                        this.getDriver().sendObjectChange("vehicleNoKey");
                     } else {
                        this.getDriver().SayDebug(" [img=media/ui/CarKey_none.png]");
                        this.checkVehicleFailsToStartWithZombiesTargeting();
                     }
                  } else {
                     this.engineDoStarting();
                  }

               }
            }
         }
      }
   }

   public void tryStartEngine() {
      this.tryStartEngine(false);
   }

   public void engineDoIdle() {
      this.engineState = BaseVehicle.engineStateTypes.Idle;
      this.engineLastUpdateStateTime = System.currentTimeMillis();
      this.transmitEngine();
   }

   public void engineDoStarting() {
      this.engineState = BaseVehicle.engineStateTypes.Starting;
      this.engineLastUpdateStateTime = System.currentTimeMillis();
      this.transmitEngine();
      this.setKeysInIgnition(true);
      this.setPreviouslyMoved(true);
   }

   public boolean isStarting() {
      return this.engineState == BaseVehicle.engineStateTypes.Starting || this.engineState == BaseVehicle.engineStateTypes.StartingFailed || this.engineState == BaseVehicle.engineStateTypes.StartingSuccess || this.engineState == BaseVehicle.engineStateTypes.StartingFailedNoPower;
   }

   private String getEngineSound() {
      return this.getScript() != null && this.getScript().getSounds().engine != null ? this.getScript().getSounds().engine : "VehicleEngineDefault";
   }

   private String getEngineStartSound() {
      return this.getScript() != null && this.getScript().getSounds().engineStart != null ? this.getScript().getSounds().engineStart : "VehicleStarted";
   }

   private String getEngineTurnOffSound() {
      return this.getScript() != null && this.getScript().getSounds().engineTurnOff != null ? this.getScript().getSounds().engineTurnOff : "VehicleTurnedOff";
   }

   private String getIgnitionFailSound() {
      return this.getScript() != null && this.getScript().getSounds().ignitionFail != null ? this.getScript().getSounds().ignitionFail : "VehicleFailingToStart";
   }

   private String getIgnitionFailNoPowerSound() {
      return this.getScript() != null && this.getScript().getSounds().ignitionFailNoPower != null ? this.getScript().getSounds().ignitionFailNoPower : "VehicleFailingToStartNoPower";
   }

   public void engineDoRetryingStarting() {
      this.getEmitter().stopSoundByName(this.getIgnitionFailSound());
      this.getEmitter().playSoundImpl(this.getIgnitionFailSound(), (IsoObject)null);
      this.engineState = BaseVehicle.engineStateTypes.RetryingStarting;
      this.engineLastUpdateStateTime = System.currentTimeMillis();
      this.transmitEngine();
      this.checkVehicleFailsToStartWithZombiesTargeting();
   }

   public void engineDoStartingSuccess() {
      this.getEmitter().stopSoundByName(this.getIgnitionFailSound());
      this.engineState = BaseVehicle.engineStateTypes.StartingSuccess;
      this.engineLastUpdateStateTime = System.currentTimeMillis();
      if (this.getEngineStartSound().equals(this.getEngineSound())) {
         if (!this.getEmitter().isPlaying(this.combinedEngineSound)) {
            this.combinedEngineSound = this.emitter.playSoundImpl(this.getEngineSound(), (IsoObject)null);
         }
      } else {
         this.getEmitter().playSoundImpl(this.getEngineStartSound(), (IsoObject)null);
      }

      this.transmitEngine();
      this.setKeysInIgnition(true);
      this.checkVehicleStartsWithZombiesTargeting();
   }

   public void engineDoStartingFailed() {
      this.getEmitter().stopSoundByName(this.getIgnitionFailSound());
      this.getEmitter().playSoundImpl(this.getIgnitionFailSound(), (IsoObject)null);
      this.stopEngineSounds();
      this.engineState = BaseVehicle.engineStateTypes.StartingFailed;
      this.engineLastUpdateStateTime = System.currentTimeMillis();
      this.transmitEngine();
      this.checkVehicleFailsToStartWithZombiesTargeting();
   }

   public void engineDoStartingFailedNoPower() {
      this.getEmitter().stopSoundByName(this.getIgnitionFailNoPowerSound());
      this.getEmitter().playSoundImpl(this.getIgnitionFailNoPowerSound(), (IsoObject)null);
      this.stopEngineSounds();
      this.engineState = BaseVehicle.engineStateTypes.StartingFailedNoPower;
      this.engineLastUpdateStateTime = System.currentTimeMillis();
      this.transmitEngine();
      this.checkVehicleFailsToStartWithZombiesTargeting();
   }

   public void engineDoRunning() {
      this.setNeedPartsUpdate(true);
      this.engineState = BaseVehicle.engineStateTypes.Running;
      this.engineLastUpdateStateTime = System.currentTimeMillis();
      this.transmitEngine();
   }

   public void engineDoStalling() {
      this.getEmitter().playSoundImpl("VehicleRunningOutOfGas", (IsoObject)null);
      this.engineState = BaseVehicle.engineStateTypes.Stalling;
      this.engineLastUpdateStateTime = System.currentTimeMillis();
      this.stopEngineSounds();
      this.engineSoundIndex = 0;
      this.transmitEngine();
      this.checkVehicleFailsToStartWithZombiesTargeting();
      if (!Core.getInstance().getOptionLeaveKeyInIgnition()) {
         this.setKeysInIgnition(false);
      }

   }

   public void engineDoShuttingDown() {
      if (!this.getEngineTurnOffSound().equals(this.getEngineSound())) {
         this.getEmitter().playSoundImpl(this.getEngineTurnOffSound(), (IsoObject)null);
      }

      this.stopEngineSounds();
      this.engineSoundIndex = 0;
      this.engineState = BaseVehicle.engineStateTypes.ShutingDown;
      this.engineLastUpdateStateTime = System.currentTimeMillis();
      this.transmitEngine();
      if (!Core.getInstance().getOptionLeaveKeyInIgnition()) {
         this.setKeysInIgnition(false);
      }

      VehiclePart var1 = this.getHeater();
      if (var1 != null) {
         var1.getModData().rawset("active", false);
      }

   }

   public void shutOff() {
      if (this.getPartById("GasTank").getContainerContentAmount() == 0.0F) {
         this.engineDoStalling();
      } else {
         this.engineDoShuttingDown();
      }
   }

   public void resumeRunningAfterLoad() {
      if (GameClient.bClient) {
         IsoGameCharacter var1 = this.getDriver();
         if (var1 != null) {
            Boolean var2 = this.getDriver().getInventory().haveThisKeyId(this.getKeyId()) != null ? Boolean.TRUE : Boolean.FALSE;
            GameClient.instance.sendClientCommandV((IsoPlayer)this.getDriver(), "vehicle", "startEngine", "haveKey", var2);
         }
      } else if (this.isEngineWorking()) {
         this.getEmitter();
         this.engineDoStartingSuccess();
      }
   }

   public boolean isEngineStarted() {
      return this.engineState == BaseVehicle.engineStateTypes.Starting || this.engineState == BaseVehicle.engineStateTypes.StartingFailed || this.engineState == BaseVehicle.engineStateTypes.StartingSuccess || this.engineState == BaseVehicle.engineStateTypes.RetryingStarting;
   }

   public boolean isEngineRunning() {
      return this.engineState == BaseVehicle.engineStateTypes.Running;
   }

   public boolean isEngineWorking() {
      for(int var1 = 0; var1 < this.parts.size(); ++var1) {
         VehiclePart var2 = (VehiclePart)this.parts.get(var1);
         String var3 = var2.getLuaFunction("checkEngine");
         if (var3 != null && !Boolean.TRUE.equals(this.callLuaBoolean(var3, this, var2))) {
            return false;
         }
      }

      return true;
   }

   public boolean isOperational() {
      for(int var1 = 0; var1 < this.parts.size(); ++var1) {
         VehiclePart var2 = (VehiclePart)this.parts.get(var1);
         String var3 = var2.getLuaFunction("checkOperate");
         if (var3 != null && !Boolean.TRUE.equals(this.callLuaBoolean(var3, this, var2))) {
            return false;
         }
      }

      return true;
   }

   public boolean isDriveable() {
      if (!this.isEngineWorking()) {
         return false;
      } else {
         return this.isOperational();
      }
   }

   public BaseSoundEmitter getEmitter() {
      if (this.emitter == null) {
         if (!Core.SoundDisabled && !GameServer.bServer) {
            FMODSoundEmitter var1 = new FMODSoundEmitter();
            var1.parameterUpdater = this;
            this.emitter = var1;
         } else {
            this.emitter = new DummySoundEmitter();
         }
      }

      return this.emitter;
   }

   public long playSoundImpl(String var1, IsoObject var2) {
      return this.getEmitter().playSoundImpl(var1, var2);
   }

   public int stopSound(long var1) {
      return this.getEmitter().stopSound(var1);
   }

   public void playSound(String var1) {
      this.getEmitter().playSound(var1);
   }

   public void updateSounds() {
      if (!GameServer.bServer) {
         if (this.getBatteryCharge() > 0.0F) {
            if (this.lightbarSirenMode.isEnable() && this.soundSirenSignal == -1L) {
               this.setLightbarSirenMode(this.lightbarSirenMode.get());
            }
         } else if (this.soundSirenSignal != -1L) {
            this.getEmitter().stopSound(this.soundSirenSignal);
            this.soundSirenSignal = -1L;
         }
      }

      IsoPlayer var1 = null;
      float var2 = 3.4028235E38F;

      for(int var3 = 0; var3 < IsoPlayer.numPlayers; ++var3) {
         IsoPlayer var4 = IsoPlayer.players[var3];
         if (var4 != null && var4.getCurrentSquare() != null) {
            float var5 = var4.getX();
            float var6 = var4.getY();
            float var7 = IsoUtils.DistanceToSquared(var5, var6, this.getX(), this.getY());
            var7 *= var4.getHearDistanceModifier();
            if (var4.Traits.Deaf.isSet()) {
               var7 = 3.4028235E38F;
            }

            if (var7 < var2) {
               var1 = var4;
               var2 = var7;
            }
         }
      }

      if (var1 == null) {
         if (this.emitter != null) {
            this.emitter.setPos(this.getX(), this.getY(), this.getZ());
            if (!this.emitter.isEmpty()) {
               this.emitter.tick();
            }
         }

      } else {
         float var9;
         if (!GameServer.bServer) {
            float var8 = ClimateManager.getInstance().isRaining() ? ClimateManager.getInstance().getPrecipitationIntensity() : 0.0F;
            if (this.getSquare() != null && this.getSquare().isInARoom()) {
               var8 = 0.0F;
            }

            if (this.getEmitter().isPlaying("VehicleAmbiance")) {
               if (var8 == 0.0F) {
                  this.getEmitter().stopOrTriggerSoundByName("VehicleAmbiance");
               }
            } else if (var8 > 0.0F && var2 < 100.0F) {
               this.emitter.playAmbientLoopedImpl("VehicleAmbiance");
            }

            var9 = var2;
            if (var2 > 1200.0F) {
               this.stopEngineSounds();
               if (this.emitter != null && !this.emitter.isEmpty()) {
                  this.emitter.setPos(this.getX(), this.getY(), this.getZ());
                  this.emitter.tick();
               }

               return;
            }

            for(int var11 = 0; var11 < this.new_EngineSoundId.length; ++var11) {
               if (this.new_EngineSoundId[var11] != 0L) {
                  this.getEmitter().setVolume(this.new_EngineSoundId[var11], 1.0F - var9 / 1200.0F);
               }
            }
         }

         if (this.getController() != null) {
            if (!GameServer.bServer) {
               if (this.emitter == null) {
                  if (this.engineState != BaseVehicle.engineStateTypes.Running) {
                     return;
                  }

                  this.getEmitter();
               }

               boolean var10 = this.isAnyListenerInside();
               var9 = Math.abs(this.getCurrentSpeedKmHour());
               if (this.startTime <= 0.0F && this.engineState == BaseVehicle.engineStateTypes.Running && !this.getEmitter().isPlaying(this.combinedEngineSound)) {
                  this.combinedEngineSound = this.emitter.playSoundImpl(this.getEngineSound(), (IsoObject)null);
                  if (this.getEngineSound().equals(this.getEngineStartSound())) {
                  }
               }

               boolean var12 = false;
               if (!GameClient.bClient || this.isLocalPhysicSim()) {
                  for(int var13 = 0; var13 < this.script.getWheelCount(); ++var13) {
                     if (this.wheelInfo[var13].skidInfo < 0.15F) {
                        var12 = true;
                        break;
                     }
                  }
               }

               if (this.getDriver() == null) {
                  var12 = false;
               }

               if (var12 != this.skidding) {
                  if (var12) {
                     this.skidSound = this.getEmitter().playSoundImpl("VehicleSkid", (IsoObject)null);
                  } else if (this.skidSound != 0L) {
                     this.emitter.stopSound(this.skidSound);
                     this.skidSound = 0L;
                  }

                  this.skidding = var12;
               }

               if (this.soundBackMoveSignal != -1L && this.emitter != null) {
                  this.emitter.set3D(this.soundBackMoveSignal, !var10);
               }

               if (this.soundHorn != -1L && this.emitter != null) {
                  this.emitter.set3D(this.soundHorn, !var10);
               }

               if (this.soundSirenSignal != -1L && this.emitter != null) {
                  this.emitter.set3D(this.soundSirenSignal, !var10);
               }

               this.updateDoorAlarmSound();
               if (this.emitter != null && (this.engineState != BaseVehicle.engineStateTypes.Idle || !this.emitter.isEmpty())) {
                  this.getFMODParameters().update();
                  this.emitter.setPos(this.getX(), this.getY(), this.getZ());
                  this.emitter.tick();
               }

            }
         }
      }
   }

   private void updateDoorAlarmSound() {
      if (this.emitter != null) {
         boolean var1 = false;
         if (this.isEngineRunning()) {
            for(int var2 = 0; var2 < this.getMaxPassengers(); ++var2) {
               VehiclePart var3 = this.getPassengerDoor(var2);
               if (var3 != null && !var3.isInventoryItemUninstalled() && var3.getDoor().isOpen()) {
                  var1 = true;
                  break;
               }
            }
         }

         if (var1) {
            if (!this.emitter.isPlaying(this.doorAlarmSound)) {
               this.doorAlarmSound = this.emitter.playSoundImpl("VehicleDoorAlarm", (IsoObject)null);
            }
         } else if (this.emitter.isPlaying(this.doorAlarmSound)) {
            this.emitter.stopSound(this.doorAlarmSound);
            this.doorAlarmSound = 0L;
         }

      }
   }

   private boolean updatePart(VehiclePart var1) {
      var1.updateSignalDevice();
      VehicleLight var2 = var1.getLight();
      if (var2 != null && var1.getId().contains("Headlight")) {
         var1.setLightActive(this.getHeadlightsOn() && var1.getInventoryItem() != null && this.getBatteryCharge() > 0.0F);
      }

      String var3 = var1.getLuaFunction("update");
      if (var3 == null) {
         return false;
      } else {
         float var4 = (float)GameTime.getInstance().getWorldAgeHours();
         if (var1.getLastUpdated() < 0.0F) {
            var1.setLastUpdated(var4);
         } else if (var1.getLastUpdated() > var4) {
            var1.setLastUpdated(var4);
         }

         float var5 = var4 - var1.getLastUpdated();
         if ((int)(var5 * 60.0F) > 0) {
            var1.setLastUpdated(var4);
            this.callLuaVoid(var3, this, var1, (double)(var5 * 60.0F));
            return true;
         } else {
            return false;
         }
      }
   }

   public void updateParts() {
      if (!GameClient.bClient) {
         boolean var4 = false;

         for(int var5 = 0; var5 < this.getPartCount(); ++var5) {
            VehiclePart var3 = this.getPartByIndex(var5);
            if (this.updatePart(var3) && !var4) {
               var4 = true;
            }

            if (var5 == this.getPartCount() - 1 && var4) {
               this.brakeBetweenUpdatesSpeed = 0.0F;
            }
         }

      } else {
         for(int var1 = 0; var1 < this.getPartCount(); ++var1) {
            VehiclePart var2 = this.getPartByIndex(var1);
            var2.updateSignalDevice();
         }

      }
   }

   public void drainBatteryUpdateHack() {
      boolean var1 = this.isEngineRunning();
      if (!var1) {
         for(int var2 = 0; var2 < this.parts.size(); ++var2) {
            VehiclePart var3 = (VehiclePart)this.parts.get(var2);
            if (var3.getDeviceData() != null && var3.getDeviceData().getIsTurnedOn()) {
               this.updatePart(var3);
            } else if (var3.getLight() != null && var3.getLight().getActive()) {
               this.updatePart(var3);
            }
         }

         if (this.hasLightbar() && (this.lightbarLightsMode.isEnable() || this.lightbarSirenMode.isEnable()) && this.getBattery() != null) {
            this.updatePart(this.getBattery());
         }

      }
   }

   public boolean getHeadlightsOn() {
      return this.headlightsOn;
   }

   public void setHeadlightsOn(boolean var1) {
      if (this.headlightsOn != var1) {
         this.headlightsOn = var1;
         if (GameServer.bServer) {
            this.updateFlags = (short)(this.updateFlags | 8);
         } else {
            this.playSound(this.headlightsOn ? "VehicleHeadlightsOn" : "VehicleHeadlightsOff");
         }

      }
   }

   public boolean getWindowLightsOn() {
      return this.windowLightsOn;
   }

   public void setWindowLightsOn(boolean var1) {
      this.windowLightsOn = var1;
   }

   public boolean getHeadlightCanEmmitLight() {
      if (this.getBatteryCharge() <= 0.0F) {
         return false;
      } else {
         VehiclePart var1 = this.getPartById("HeadlightLeft");
         if (var1 != null && var1.getInventoryItem() != null) {
            return true;
         } else {
            var1 = this.getPartById("HeadlightRight");
            return var1 != null && var1.getInventoryItem() != null;
         }
      }
   }

   public boolean getStoplightsOn() {
      return this.stoplightsOn;
   }

   public void setStoplightsOn(boolean var1) {
      if (this.stoplightsOn != var1) {
         this.stoplightsOn = var1;
         if (GameServer.bServer) {
            this.updateFlags = (short)(this.updateFlags | 8);
         }

      }
   }

   public boolean hasHeadlights() {
      return this.getLightCount() > 0;
   }

   public void addToWorld() {
      if (this.addedToWorld) {
         DebugLog.Vehicle.error("added vehicle twice " + this + " id=" + this.VehicleID);
      } else {
         VehiclesDB2.instance.setVehicleLoaded(this);
         this.addedToWorld = true;
         this.removedFromWorld = false;
         super.addToWorld();
         this.createPhysics();

         for(int var1 = 0; var1 < this.parts.size(); ++var1) {
            VehiclePart var2 = (VehiclePart)this.parts.get(var1);
            if (var2.getItemContainer() != null) {
               var2.getItemContainer().addItemsToProcessItems();
            }

            if (var2.getDeviceData() != null && !GameServer.bServer) {
               ZomboidRadio.getInstance().RegisterDevice(var2);
            }
         }

         if (this.lightbarSirenMode.isEnable()) {
            this.setLightbarSirenMode(this.lightbarSirenMode.get());
            if (this.sirenStartTime <= 0.0) {
               this.sirenStartTime = GameTime.instance.getWorldAgeHours();
            }
         }

         if (this.chunk != null && this.chunk.jobType != IsoChunk.JobType.SoftReset) {
            if (PathfindNative.USE_NATIVE_CODE) {
               PathfindNative.instance.addVehicle(this);
            } else {
               PolygonalMap2.instance.addVehicleToWorld(this);
            }
         }

         if (this.engineState != BaseVehicle.engineStateTypes.Idle) {
            this.engineSpeed = this.getScript() == null ? 1000.0 : (double)this.getScript().getEngineIdleSpeed();
         }

         if (this.chunk != null && this.chunk.jobType != IsoChunk.JobType.SoftReset) {
            this.trySpawnKey();
         }

         if (this.emitter != null) {
            SoundManager.instance.registerEmitter(this.emitter);
         }

      }
   }

   public void removeFromWorld() {
      this.breakConstraint(false, false);
      VehiclesDB2.instance.setVehicleUnloaded(this);

      int var1;
      for(var1 = 0; var1 < this.passengers.length; ++var1) {
         if (this.getPassenger(var1).character != null) {
            for(int var2 = 0; var2 < 4; ++var2) {
               if (this.getPassenger(var1).character == IsoPlayer.players[var2]) {
                  return;
               }
            }
         }
      }

      IsoChunk.removeFromCheckedVehicles(this);
      DebugLog.Vehicle.trace("BaseVehicle.removeFromWorld() %s id=%d", this, this.VehicleID);
      if (!this.removedFromWorld) {
         if (!this.addedToWorld) {
            DebugLog.Vehicle.debugln("ERROR: removing vehicle but addedToWorld=false %s id=%d", this, this.VehicleID);
         }

         this.removedFromWorld = true;
         this.addedToWorld = false;

         for(var1 = 0; var1 < this.parts.size(); ++var1) {
            VehiclePart var4 = (VehiclePart)this.parts.get(var1);
            if (var4.getItemContainer() != null) {
               var4.getItemContainer().removeItemsFromProcessItems();
            }

            if (var4.getDeviceData() != null) {
               var4.getDeviceData().cleanSoundsAndEmitter();
               if (!GameServer.bServer) {
                  ZomboidRadio.getInstance().UnRegisterDevice(var4);
               }
            }
         }

         if (this.emitter != null) {
            this.emitter.stopAll();
            SoundManager.instance.unregisterEmitter(this.emitter);
            this.emitter = null;
         }

         if (this.hornemitter != null && this.soundHorn != -1L) {
            this.hornemitter.stopAll();
            this.soundHorn = -1L;
         }

         if (this.createdModel) {
            ModelManager.instance.Remove(this);
            this.createdModel = false;
         }

         this.releaseAnimationPlayers();
         if (this.getController() != null) {
            if (!GameServer.bServer) {
               Bullet.removeVehicle(this.VehicleID);
            }

            this.physics = null;
         }

         if (!GameServer.bServer && !GameClient.bClient) {
            if (this.VehicleID != -1) {
               VehicleIDMap.instance.remove(this.VehicleID);
            }
         } else {
            VehicleManager.instance.removeFromWorld(this);
         }

         IsoWorld.instance.CurrentCell.addVehicles.remove(this);
         IsoWorld.instance.CurrentCell.vehicles.remove(this);
         if (PathfindNative.USE_NATIVE_CODE) {
            PathfindNative.instance.removeVehicle(this);
         } else {
            PolygonalMap2.instance.removeVehicleFromWorld(this);
         }

         if (GameClient.bClient) {
            this.chunk.vehicles.remove(this);
         }

         this.m_surroundVehicle.reset();
         this.removeWorldLights();
         Iterator var3 = this.animals.iterator();

         while(var3.hasNext()) {
            IsoAnimal var5 = (IsoAnimal)var3.next();
            var5.delete();
         }

         super.removeFromWorld();
      }
   }

   public void permanentlyRemove() {
      for(int var1 = 0; var1 < this.getMaxPassengers(); ++var1) {
         IsoGameCharacter var2 = this.getCharacter(var1);
         if (var2 != null) {
            if (GameServer.bServer) {
               var2.sendObjectChange("exitVehicle");
            }

            this.exit(var2);
         }
      }

      this.breakConstraint(true, false);
      this.removeFromWorld();
      this.removeFromSquare();
      if (this.chunk != null) {
         this.chunk.vehicles.remove(this);
      }

      VehiclesDB2.instance.removeVehicle(this);
   }

   public VehiclePart getBattery() {
      return this.battery;
   }

   public void setEngineFeature(int var1, int var2, int var3) {
      this.engineQuality = PZMath.clamp(var1, 0, 100);
      this.engineLoudness = (int)((float)var2 / 2.7F);
      this.enginePower = var3;
   }

   public int getEngineQuality() {
      return this.engineQuality;
   }

   public int getEngineLoudness() {
      return this.engineLoudness;
   }

   public int getEnginePower() {
      return this.enginePower;
   }

   public float getBatteryCharge() {
      VehiclePart var1 = this.getBattery();
      return var1 != null && var1.getInventoryItem() instanceof DrainableComboItem ? ((DrainableComboItem)var1.getInventoryItem()).getCurrentUsesFloat() : 0.0F;
   }

   public int getPartCount() {
      return this.parts.size();
   }

   public VehiclePart getPartByIndex(int var1) {
      return var1 >= 0 && var1 < this.parts.size() ? (VehiclePart)this.parts.get(var1) : null;
   }

   public VehiclePart getPartById(String var1) {
      if (var1 == null) {
         return null;
      } else {
         for(int var2 = 0; var2 < this.parts.size(); ++var2) {
            VehiclePart var3 = (VehiclePart)this.parts.get(var2);
            VehicleScript.Part var4 = var3.getScriptPart();
            if (var4 != null && var1.equals(var4.id)) {
               return var3;
            }
         }

         return null;
      }
   }

   public int getNumberOfPartsWithContainers() {
      if (this.getScript() == null) {
         return 0;
      } else {
         int var1 = 0;

         for(int var2 = 0; var2 < this.getScript().getPartCount(); ++var2) {
            if (this.getScript().getPart(var2).container != null) {
               ++var1;
            }
         }

         return var1;
      }
   }

   public VehiclePart getPartForSeatContainer(int var1) {
      if (this.getScript() != null && var1 >= 0 && var1 < this.getMaxPassengers()) {
         for(int var2 = 0; var2 < this.getPartCount(); ++var2) {
            VehiclePart var3 = this.getPartByIndex(var2);
            if (var3.getContainerSeatNumber() == var1) {
               return var3;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public void transmitPartCondition(VehiclePart var1) {
      if (GameServer.bServer) {
         if (this.parts.contains(var1)) {
            var1.updateFlags = (short)(var1.updateFlags | 2048);
            this.updateFlags = (short)(this.updateFlags | 2048);
         }
      }
   }

   public void transmitPartItem(VehiclePart var1) {
      if (GameServer.bServer) {
         if (this.parts.contains(var1)) {
            var1.updateFlags = (short)(var1.updateFlags | 128);
            this.updateFlags = (short)(this.updateFlags | 128);
         }
      }
   }

   public void transmitPartModData(VehiclePart var1) {
      if (GameServer.bServer) {
         if (this.parts.contains(var1)) {
            var1.updateFlags = (short)(var1.updateFlags | 16);
            this.updateFlags = (short)(this.updateFlags | 16);
         }
      }
   }

   public void transmitPartUsedDelta(VehiclePart var1) {
      if (GameServer.bServer) {
         if (this.parts.contains(var1)) {
            if (var1.getInventoryItem() instanceof DrainableComboItem) {
               var1.updateFlags = (short)(var1.updateFlags | 32);
               this.updateFlags = (short)(this.updateFlags | 32);
            }
         }
      }
   }

   public void transmitPartDoor(VehiclePart var1) {
      if (GameServer.bServer) {
         if (this.parts.contains(var1)) {
            if (var1.getDoor() != null) {
               var1.updateFlags = (short)(var1.updateFlags | 512);
               this.updateFlags = (short)(this.updateFlags | 512);
            }
         }
      }
   }

   public void transmitPartWindow(VehiclePart var1) {
      if (GameServer.bServer) {
         if (this.parts.contains(var1)) {
            if (var1.getWindow() != null) {
               var1.updateFlags = (short)(var1.updateFlags | 256);
               this.updateFlags = (short)(this.updateFlags | 256);
            }
         }
      }
   }

   public int getLightCount() {
      return this.lights.size();
   }

   public VehiclePart getLightByIndex(int var1) {
      return var1 >= 0 && var1 < this.lights.size() ? (VehiclePart)this.lights.get(var1) : null;
   }

   public String getZone() {
      return this.respawnZone;
   }

   public void setZone(String var1) {
      this.respawnZone = var1;
   }

   public boolean isInArea(String var1, IsoGameCharacter var2) {
      if (var1 != null && this.getScript() != null) {
         VehicleScript.Area var3 = this.getScript().getAreaById(var1);
         if (var3 == null) {
            return false;
         } else {
            Vector2 var4 = (Vector2)((Vector2ObjectPool)TL_vector2_pool.get()).alloc();
            Vector2 var5 = this.areaPositionLocal(var3, var4);
            if (var5 == null) {
               ((Vector2ObjectPool)TL_vector2_pool.get()).release(var4);
               return false;
            } else {
               Vector3f var6 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
               this.getLocalPos(var2.getX(), var2.getY(), this.getZ(), var6);
               float var7 = var5.x - var3.w / 2.0F;
               float var8 = var5.y - var3.h / 2.0F;
               float var9 = var5.x + var3.w / 2.0F;
               float var10 = var5.y + var3.h / 2.0F;
               ((Vector2ObjectPool)TL_vector2_pool.get()).release(var4);
               boolean var11 = var6.x >= var7 && var6.x < var9 && var6.z >= var8 && var6.z < var10;
               ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var6);
               return var11;
            }
         }
      } else {
         return false;
      }
   }

   public float getAreaDist(String var1, float var2, float var3, float var4) {
      if (var1 != null && this.getScript() != null) {
         VehicleScript.Area var5 = this.getScript().getAreaById(var1);
         if (var5 != null) {
            Vector3f var6 = this.getLocalPos(var2, var3, var4, (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
            float var7 = Math.abs(var5.x - var5.w / 2.0F);
            float var8 = Math.abs(var5.y - var5.h / 2.0F);
            float var9 = Math.abs(var5.x + var5.w / 2.0F);
            float var10 = Math.abs(var5.y + var5.h / 2.0F);
            float var11 = Math.abs(var6.x + var7) + Math.abs(var6.z + var8);
            ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var6);
            return var11;
         } else {
            return 999.0F;
         }
      } else {
         return 999.0F;
      }
   }

   public float getAreaDist(String var1, IsoGameCharacter var2) {
      if (var1 != null && this.getScript() != null) {
         VehicleScript.Area var3 = this.getScript().getAreaById(var1);
         if (var3 != null) {
            Vector3f var4 = this.getLocalPos(var2.getX(), var2.getY(), this.getZ(), (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
            float var5 = Math.abs(var3.x - var3.w / 2.0F);
            float var6 = Math.abs(var3.y - var3.h / 2.0F);
            float var7 = Math.abs(var3.x + var3.w / 2.0F);
            float var8 = Math.abs(var3.y + var3.h / 2.0F);
            float var9 = Math.abs(var4.x + var5) + Math.abs(var4.z + var6);
            ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var4);
            return var9;
         } else {
            return 999.0F;
         }
      } else {
         return 999.0F;
      }
   }

   public Vector2 getAreaCenter(String var1) {
      return this.getAreaCenter(var1, new Vector2());
   }

   public Vector2 getAreaCenter(String var1, Vector2 var2) {
      if (var1 != null && this.getScript() != null) {
         VehicleScript.Area var3 = this.getScript().getAreaById(var1);
         return var3 == null ? null : this.areaPositionWorld(var3, var2);
      } else {
         return null;
      }
   }

   public boolean isInBounds(float var1, float var2) {
      return this.getPoly().containsPoint(var1, var2);
   }

   public boolean canAccessContainer(int var1, IsoGameCharacter var2) {
      VehiclePart var3 = this.getPartByIndex(var1);
      if (var3 == null) {
         return false;
      } else {
         VehicleScript.Part var4 = var3.getScriptPart();
         if (var4 == null) {
            return false;
         } else if (var4.container == null) {
            return false;
         } else if (var3.isInventoryItemUninstalled() && var4.container.capacity == 0) {
            return false;
         } else {
            return var4.container.luaTest != null && !var4.container.luaTest.isEmpty() ? Boolean.TRUE.equals(this.callLuaBoolean(var4.container.luaTest, this, var3, var2)) : true;
         }
      }
   }

   public boolean canInstallPart(IsoGameCharacter var1, VehiclePart var2) {
      if (!this.parts.contains(var2)) {
         return false;
      } else {
         KahluaTable var3 = var2.getTable("install");
         return var3 != null && var3.rawget("test") instanceof String ? Boolean.TRUE.equals(this.callLuaBoolean((String)var3.rawget("test"), this, var2, var1)) : false;
      }
   }

   public boolean canUninstallPart(IsoGameCharacter var1, VehiclePart var2) {
      if (!this.parts.contains(var2)) {
         return false;
      } else {
         KahluaTable var3 = var2.getTable("uninstall");
         return var3 != null && var3.rawget("test") instanceof String ? Boolean.TRUE.equals(this.callLuaBoolean((String)var3.rawget("test"), this, var2, var1)) : false;
      }
   }

   private void callLuaVoid(String var1, Object var2, Object var3) {
      Object var4 = LuaManager.getFunctionObject(var1);
      if (var4 != null) {
         LuaManager.caller.protectedCallVoid(LuaManager.thread, var4, var2, var3);
      }
   }

   private void callLuaVoid(String var1, Object var2) {
      Object var3 = LuaManager.getFunctionObject(var1);
      if (var3 != null) {
         LuaManager.caller.protectedCallVoid(LuaManager.thread, var3, var2);
      }
   }

   private void callLuaVoid(String var1, Object var2, Object var3, Object var4) {
      Object var5 = LuaManager.getFunctionObject(var1);
      if (var5 != null) {
         LuaManager.caller.protectedCallVoid(LuaManager.thread, var5, var2, var3, var4);
      }
   }

   private Boolean callLuaBoolean(String var1, Object var2, Object var3) {
      Object var4 = LuaManager.getFunctionObject(var1);
      return var4 == null ? null : LuaManager.caller.protectedCallBoolean(LuaManager.thread, var4, var2, var3);
   }

   private Boolean callLuaBoolean(String var1, Object var2, Object var3, Object var4) {
      Object var5 = LuaManager.getFunctionObject(var1);
      return var5 == null ? null : LuaManager.caller.protectedCallBoolean(LuaManager.thread, var5, var2, var3, var4);
   }

   public short getId() {
      return this.VehicleID;
   }

   public void setTireInflation(int var1, float var2) {
      Bullet.setTireInflation(this.VehicleID, var1, var2);
   }

   public void setTireRemoved(int var1, boolean var2) {
      if (!GameServer.bServer) {
         Bullet.setTireRemoved(this.VehicleID, var1, var2);
      }

   }

   public Vector3f chooseBestAttackPosition(IsoGameCharacter var1, IsoGameCharacter var2, Vector3f var3) {
      if (var2 instanceof IsoAnimal) {
         return null;
      } else {
         Vector2f var4 = allocVector2f();
         Vector2f var5 = var1.getVehicle().getSurroundVehicle().getPositionForZombie((IsoZombie)var2, var4);
         float var6 = var4.x;
         float var7 = var4.y;
         releaseVector2f(var4);
         return var5 != null ? var3.set(var6, var7, this.getZ()) : null;
      }
   }

   public MinMaxPosition getMinMaxPosition() {
      MinMaxPosition var1 = new MinMaxPosition();
      float var2 = this.getX();
      float var3 = this.getY();
      Vector3f var4 = this.getScript().getExtents();
      float var5 = var4.x;
      float var6 = var4.z;
      IsoDirections var7 = this.getDir();
      switch (var7) {
         case E:
         case W:
            var1.minX = var2 - var5 / 2.0F;
            var1.maxX = var2 + var5 / 2.0F;
            var1.minY = var3 - var6 / 2.0F;
            var1.maxY = var3 + var6 / 2.0F;
            break;
         case N:
         case S:
            var1.minX = var2 - var6 / 2.0F;
            var1.maxX = var2 + var6 / 2.0F;
            var1.minY = var3 - var5 / 2.0F;
            var1.maxY = var3 + var5 / 2.0F;
            break;
         default:
            return null;
      }

      return var1;
   }

   public String getVehicleType() {
      return this.type;
   }

   public void setVehicleType(String var1) {
      this.type = var1;
   }

   public float getMaxSpeed() {
      return this.maxSpeed;
   }

   public void setMaxSpeed(float var1) {
      this.maxSpeed = var1;
   }

   public void lockServerUpdate(long var1) {
      this.updateLockTimeout = System.currentTimeMillis() + var1;
   }

   public void changeTransmission(TransmissionNumber var1) {
      this.transmissionNumber = var1;
   }

   public void tryHotwire(int var1) {
      int var2 = Math.max(100 - this.getEngineQuality(), 5);
      var2 = Math.min(var2, 50);
      int var3 = var1 * 4;
      int var4 = var2 + var3;
      boolean var5 = false;
      String var6 = null;
      if (Rand.Next(100) <= 11 - var1 && this.alarmed) {
         this.triggerAlarm();
      }

      if (Rand.Next(100) <= var4) {
         this.setHotwired(true);
         var5 = true;
         var6 = "VehicleHotwireSuccess";
      } else if (Rand.Next(100) <= 10 - var1) {
         this.setHotwiredBroken(true);
         var5 = true;
         var6 = "VehicleHotwireFail";
      } else {
         var6 = "VehicleHotwireFail";
      }

      if (var6 != null) {
         if (GameServer.bServer) {
            LuaManager.GlobalObject.playServerSound(var6, this.square);
         } else if (this.getDriver() != null) {
            this.getDriver().getEmitter().playSound(var6);
         }
      }

      if (var5 && GameServer.bServer) {
         this.updateFlags = (short)(this.updateFlags | 4096);
      }

   }

   public void cheatHotwire(boolean var1, boolean var2) {
      if (var1 != this.hotwired || var2 != this.hotwiredBroken) {
         this.hotwired = var1;
         this.hotwiredBroken = var2;
         if (GameServer.bServer) {
            this.updateFlags = (short)(this.updateFlags | 4096);
         }
      }

   }

   public boolean isKeyIsOnDoor() {
      return this.keyIsOnDoor;
   }

   public void setKeyIsOnDoor(boolean var1) {
      this.keyIsOnDoor = var1;
   }

   public boolean isHotwired() {
      return this.hotwired;
   }

   public void setHotwired(boolean var1) {
      this.hotwired = var1;
   }

   public boolean isHotwiredBroken() {
      return this.hotwiredBroken;
   }

   public void setHotwiredBroken(boolean var1) {
      this.hotwiredBroken = var1;
   }

   public IsoGameCharacter getDriver() {
      Passenger var1 = this.getPassenger(0);
      return var1 == null ? null : var1.character;
   }

   public boolean isKeysInIgnition() {
      return this.keysInIgnition;
   }

   public void setKeysInIgnition(boolean var1) {
      IsoGameCharacter var2 = this.getDriver();
      if (var2 != null) {
         this.setAlarmed(false);
         if (!GameClient.bClient || var2 instanceof IsoPlayer && ((IsoPlayer)var2).isLocalPlayer()) {
            if (!this.isHotwired()) {
               InventoryItem var3;
               if (!GameServer.bServer && var1 && !this.keysInIgnition) {
                  var3 = this.getDriver().getInventory().haveThisKeyId(this.getKeyId());
                  if (var3 != null) {
                     this.setCurrentKey(var3);
                     InventoryItem var4 = var3.getContainer().getContainingItem();
                     if (var4 instanceof InventoryContainer && (var4.hasTag("KeyRing") || "KeyRing".equals(var4.getType()))) {
                        var3.getModData().rawset("keyRing", (double)var4.getID());
                     } else if (var3.hasModData()) {
                        var3.getModData().rawset("keyRing", (Object)null);
                     }

                     var3.getContainer().DoRemoveItem(var3);
                     this.keysInIgnition = var1;
                     if (GameClient.bClient) {
                        GameClient.instance.sendClientCommandV((IsoPlayer)this.getDriver(), "vehicle", "putKeyInIgnition", "key", var3);
                     }
                  }
               }

               if (!var1 && this.keysInIgnition && !GameServer.bServer) {
                  if (this.currentKey == null) {
                     this.currentKey = this.createVehicleKey();
                  }

                  var3 = this.getCurrentKey();
                  ItemContainer var7 = this.getDriver().getInventory();
                  if (var3.hasModData() && var3.getModData().rawget("keyRing") instanceof Double) {
                     Double var5 = (Double)var3.getModData().rawget("keyRing");
                     InventoryItem var6 = var7.getItemWithID(var5.intValue());
                     if (var6 instanceof InventoryContainer && (var6.hasTag("KeyRing") || "KeyRing".equals(var6.getType()))) {
                        var7 = ((InventoryContainer)var6).getInventory();
                     }

                     var3.getModData().rawset("keyRing", (Object)null);
                  }

                  var7.addItem(var3);
                  this.setCurrentKey((InventoryItem)null);
                  this.keysInIgnition = var1;
                  if (GameClient.bClient) {
                     GameClient.instance.sendClientCommand((IsoPlayer)this.getDriver(), "vehicle", "removeKeyFromIgnition", (KahluaTable)null);
                  }
               }
            }

         }
      }
   }

   public void putKeyInIgnition(InventoryItem var1) {
      if (GameServer.bServer) {
         if (var1 instanceof Key) {
            if (!this.keysInIgnition) {
               this.keysInIgnition = true;
               this.keyIsOnDoor = false;
               this.currentKey = var1;
               this.updateFlags = (short)(this.updateFlags | 4096);
            }
         }
      }
   }

   public void removeKeyFromIgnition() {
      if (GameServer.bServer) {
         if (this.keysInIgnition) {
            this.keysInIgnition = false;
            this.currentKey = null;
            this.updateFlags = (short)(this.updateFlags | 4096);
         }
      }
   }

   public void putKeyOnDoor(InventoryItem var1) {
      if (GameServer.bServer) {
         if (var1 instanceof Key) {
            if (!this.keyIsOnDoor) {
               this.keyIsOnDoor = true;
               this.keysInIgnition = false;
               this.currentKey = var1;
               this.updateFlags = (short)(this.updateFlags | 4096);
            }
         }
      }
   }

   public void removeKeyFromDoor() {
      if (GameServer.bServer) {
         if (this.keyIsOnDoor) {
            this.keyIsOnDoor = false;
            this.currentKey = null;
            this.updateFlags = (short)(this.updateFlags | 4096);
         }
      }
   }

   public void syncKeyInIgnition(boolean var1, boolean var2, InventoryItem var3) {
      if (GameClient.bClient) {
         if (!(this.getDriver() instanceof IsoPlayer) || !((IsoPlayer)this.getDriver()).isLocalPlayer()) {
            this.keysInIgnition = var1;
            this.keyIsOnDoor = var2;
            this.currentKey = var3;
         }
      }
   }

   private void randomizeContainers() {
      if (!GameClient.bClient) {
         boolean var1 = true;
         String var2 = this.getScriptName().substring(this.getScriptName().indexOf(46) + 1);
         ItemPickerJava.VehicleDistribution var3 = (ItemPickerJava.VehicleDistribution)ItemPickerJava.VehicleDistributions.get(var2 + this.getSkinIndex());
         if (var3 != null) {
            var1 = false;
         } else {
            var3 = (ItemPickerJava.VehicleDistribution)ItemPickerJava.VehicleDistributions.get(var2);
         }

         if (var3 == null) {
            for(int var7 = 0; var7 < this.parts.size(); ++var7) {
               VehiclePart var8 = (VehiclePart)this.parts.get(var7);
               if (var8.getItemContainer() != null) {
                  if (Core.bDebug) {
                     DebugLog.Vehicle.debugln("VEHICLE MISSING CONT DISTRIBUTION: " + var2);
                  }

                  return;
               }
            }

         } else {
            ItemPickerJava.ItemPickerRoom var4;
            if (var1 && Rand.Next(100) <= 8 && !var3.Specific.isEmpty()) {
               var4 = (ItemPickerJava.ItemPickerRoom)PZArrayUtil.pickRandom((List)var3.Specific);
            } else {
               var4 = var3.Normal;
            }

            int var5;
            if (!StringUtils.isNullOrWhitespace(this.specificDistributionId)) {
               for(var5 = 0; var5 < var3.Specific.size(); ++var5) {
                  ItemPickerJava.ItemPickerRoom var6 = (ItemPickerJava.ItemPickerRoom)var3.Specific.get(var5);
                  if (this.specificDistributionId.equals(var6.specificId)) {
                     var4 = var6;
                     break;
                  }
               }
            }

            for(var5 = 0; var5 < this.parts.size(); ++var5) {
               VehiclePart var9 = (VehiclePart)this.parts.get(var5);
               if (var9.getItemContainer() != null) {
                  if (GameServer.bServer && GameServer.bSoftReset) {
                     var9.getItemContainer().setExplored(false);
                  }

                  if (!var9.getItemContainer().bExplored) {
                     var9.getItemContainer().clear();
                     if (Rand.Next(100) <= 100) {
                        this.randomizeContainer(var9, var4);
                     }

                     var9.getItemContainer().setExplored(true);
                  }
               }
            }

         }
      }
   }

   private void randomizeContainers(ItemPickerJava.ItemPickerRoom var1) {
      for(int var2 = 0; var2 < this.parts.size(); ++var2) {
         VehiclePart var3 = (VehiclePart)this.parts.get(var2);
         if (var3.getItemContainer() != null) {
            if (GameServer.bServer && GameServer.bSoftReset) {
               var3.getItemContainer().setExplored(false);
            }

            if (!var3.getItemContainer().bExplored) {
               var3.getItemContainer().clear();
               if (Rand.Next(100) <= 100) {
                  this.randomizeContainer(var3, var1);
               }

               var3.getItemContainer().setExplored(true);
            }
         }
      }

   }

   private void randomizeContainer(VehiclePart var1, ItemPickerJava.ItemPickerRoom var2) {
      if (!GameClient.bClient) {
         if (var2 != null) {
            if (!var1.getId().contains("Seat") && !var2.Containers.containsKey(var1.getId())) {
               DebugLogStream var10000 = DebugLog.Vehicle;
               String var10001 = var1.getId();
               var10000.debugln("NO CONT DISTRIB FOR PART: " + var10001 + " CAR: " + this.getScriptName().replaceFirst("Base.", ""));
            }

            ItemPickerJava.fillContainerType(var2, var1.getItemContainer(), "", (IsoGameCharacter)null);
            String var3 = this.getScriptName().substring(this.getScriptName().indexOf(46) + 1);
            LuaEventManager.triggerEvent("OnFillContainer", var3, var1.getItemContainer().getType(), var1.getItemContainer());
         }
      }
   }

   public boolean hasHorn() {
      return this.script.getSounds().hornEnable;
   }

   public boolean hasLightbar() {
      VehiclePart var1 = this.getPartById("lightbar");
      return this.script.getLightbar().enable;
   }

   public void onHornStart() {
      this.soundHornOn = true;
      WorldSoundManager.WorldSound var1;
      if (GameServer.bServer) {
         this.updateFlags = (short)(this.updateFlags | 1024);
         if (this.script.getSounds().hornEnable) {
            var1 = WorldSoundManager.instance.addSound(this, (int)this.getX(), (int)this.getY(), (int)this.getZ(), 150, 150, false);
            var1.stressAnimals = true;
         }

      } else {
         if (this.soundHorn != -1L) {
            this.hornemitter.stopSound(this.soundHorn);
         }

         if (this.script.getSounds().hornEnable) {
            this.hornemitter = IsoWorld.instance.getFreeEmitter(this.getX(), this.getY(), (float)((int)this.getZ()));
            this.soundHorn = this.hornemitter.playSoundLoopedImpl(this.script.getSounds().horn);
            this.hornemitter.set3D(this.soundHorn, !this.isAnyListenerInside());
            this.hornemitter.setVolume(this.soundHorn, 1.0F);
            this.hornemitter.setPitch(this.soundHorn, 1.0F);
            if (!GameClient.bClient) {
               var1 = WorldSoundManager.instance.addSound(this, (int)this.getX(), (int)this.getY(), (int)this.getZ(), 150, 150, false);
               var1.stressAnimals = true;
            }

            IsoGameCharacter var3 = this.getDriver();
            IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(var3, IsoPlayer.class);
            if (var2 != null && var2.isLocalPlayer()) {
               var2.triggerMusicIntensityEvent("VehicleHorn");
            }
         }

      }
   }

   public void onHornStop() {
      this.soundHornOn = false;
      if (GameServer.bServer) {
         this.updateFlags = (short)(this.updateFlags | 1024);
      } else {
         if (this.script.getSounds().hornEnable && this.soundHorn != -1L) {
            this.hornemitter.stopSound(this.soundHorn);
            this.soundHorn = -1L;
         }

      }
   }

   public boolean hasBackSignal() {
      return this.script != null && this.script.getSounds().backSignalEnable;
   }

   public boolean isBackSignalEmitting() {
      return this.soundBackMoveSignal != -1L;
   }

   public void onBackMoveSignalStart() {
      this.soundBackMoveOn = true;
      if (GameServer.bServer) {
         this.updateFlags = (short)(this.updateFlags | 1024);
      } else {
         if (this.soundBackMoveSignal != -1L) {
            this.emitter.stopSound(this.soundBackMoveSignal);
         }

         if (this.script.getSounds().backSignalEnable) {
            this.soundBackMoveSignal = this.emitter.playSoundLoopedImpl(this.script.getSounds().backSignal);
            this.emitter.set3D(this.soundBackMoveSignal, !this.isAnyListenerInside());
         }

      }
   }

   public void onBackMoveSignalStop() {
      this.soundBackMoveOn = false;
      if (GameServer.bServer) {
         this.updateFlags = (short)(this.updateFlags | 1024);
      } else {
         if (this.script.getSounds().backSignalEnable && this.soundBackMoveSignal != -1L) {
            this.emitter.stopSound(this.soundBackMoveSignal);
            this.soundBackMoveSignal = -1L;
         }

      }
   }

   public int getLightbarLightsMode() {
      return this.lightbarLightsMode.get();
   }

   public void setLightbarLightsMode(int var1) {
      this.lightbarLightsMode.set(var1);
      if (GameServer.bServer) {
         this.updateFlags = (short)(this.updateFlags | 1024);
      }

   }

   public int getLightbarSirenMode() {
      return this.lightbarSirenMode.get();
   }

   public void setLightbarSirenMode(int var1) {
      if (this.soundSirenSignal != -1L) {
         this.getEmitter().stopSound(this.soundSirenSignal);
         this.soundSirenSignal = -1L;
      }

      this.lightbarSirenMode.set(var1);
      if (GameServer.bServer) {
         this.updateFlags = (short)(this.updateFlags | 1024);
      } else {
         if (this.lightbarSirenMode.isEnable() && this.getBatteryCharge() > 0.0F) {
            this.soundSirenSignal = this.getEmitter().playSoundLoopedImpl(this.lightbarSirenMode.getSoundName(this.script.getLightbar()));
            this.getEmitter().set3D(this.soundSirenSignal, !this.isAnyListenerInside());
         }

      }
   }

   public HashMap<String, String> getChoosenParts() {
      return this.choosenParts;
   }

   public float getMass() {
      float var1 = this.mass;
      if (var1 < 0.0F) {
         var1 = 1.0F;
      }

      return var1;
   }

   public void setMass(float var1) {
      this.mass = var1;
   }

   public float getInitialMass() {
      return this.initialMass;
   }

   public void setInitialMass(float var1) {
      this.initialMass = var1;
   }

   public void updateTotalMass() {
      float var1 = 0.0F;

      for(int var2 = 0; var2 < this.parts.size(); ++var2) {
         VehiclePart var3 = (VehiclePart)this.parts.get(var2);
         if (var3.getItemContainer() != null) {
            var1 += var3.getItemContainer().getCapacityWeight();
         }

         if (var3.getInventoryItem() != null) {
            var1 += var3.getInventoryItem().getWeight();
         }
      }

      this.setMass((float)Math.round(this.getInitialMass() + var1));
      if (this.physics != null && !GameServer.bServer) {
         Bullet.setVehicleMass(this.VehicleID, this.getMass());
      }

   }

   public float getBrakingForce() {
      return this.brakingForce;
   }

   public void setBrakingForce(float var1) {
      this.brakingForce = var1;
   }

   public float getBaseQuality() {
      return this.baseQuality;
   }

   public void setBaseQuality(float var1) {
      this.baseQuality = var1;
   }

   public float getCurrentSteering() {
      return this.currentSteering;
   }

   public void setCurrentSteering(float var1) {
      this.currentSteering = var1;
   }

   public boolean isDoingOffroad() {
      if (this.getCurrentSquare() == null) {
         return false;
      } else {
         IsoObject var1 = this.getCurrentSquare().getFloor();
         if (var1 != null && var1.getSprite() != null) {
            String var2 = var1.getSprite().getName();
            if (var2 == null) {
               return false;
            } else {
               return !var2.contains("carpentry_02") && !var2.contains("blends_street") && !var2.contains("floors_exterior_street");
            }
         } else {
            return false;
         }
      }
   }

   public boolean isBraking() {
      return this.isBraking;
   }

   public void setBraking(boolean var1) {
      this.isBraking = var1;
      if (var1 && this.brakeBetweenUpdatesSpeed == 0.0F) {
         this.brakeBetweenUpdatesSpeed = Math.abs(this.getCurrentSpeedKmHour());
      }

   }

   public void updatePartStats() {
      this.setBrakingForce(0.0F);
      this.engineLoudness = (int)((double)this.getScript().getEngineLoudness() * SandboxOptions.instance.ZombieAttractionMultiplier.getValue() / 2.0);
      boolean var1 = false;

      for(int var2 = 0; var2 < this.getPartCount(); ++var2) {
         VehiclePart var3 = this.getPartByIndex(var2);
         if (var3.getInventoryItem() != null) {
            float var4;
            if (var3.getInventoryItem().getBrakeForce() > 0.0F) {
               var4 = VehiclePart.getNumberByCondition(var3.getInventoryItem().getBrakeForce(), (float)var3.getInventoryItem().getCondition(), 5.0F);
               var4 += var4 / 50.0F * (float)var3.getMechanicSkillInstaller();
               this.setBrakingForce(this.getBrakingForce() + var4);
            }

            if (var3.getInventoryItem().getWheelFriction() > 0.0F) {
               var3.setWheelFriction(0.0F);
               var4 = VehiclePart.getNumberByCondition(var3.getInventoryItem().getWheelFriction(), (float)var3.getInventoryItem().getCondition(), 0.2F);
               var4 += 0.1F * (float)var3.getMechanicSkillInstaller();
               var4 = Math.min(2.3F, var4);
               var3.setWheelFriction(var4);
            }

            if (var3.getInventoryItem().getSuspensionCompression() > 0.0F) {
               var3.setSuspensionCompression(VehiclePart.getNumberByCondition(var3.getInventoryItem().getSuspensionCompression(), (float)var3.getInventoryItem().getCondition(), 0.6F));
               var3.setSuspensionDamping(VehiclePart.getNumberByCondition(var3.getInventoryItem().getSuspensionDamping(), (float)var3.getInventoryItem().getCondition(), 0.6F));
            }

            if (var3.getInventoryItem().getEngineLoudness() > 0.0F) {
               var3.setEngineLoudness(VehiclePart.getNumberByCondition(var3.getInventoryItem().getEngineLoudness(), (float)var3.getInventoryItem().getCondition(), 10.0F));
               this.engineLoudness = (int)((float)this.engineLoudness * (1.0F + (100.0F - var3.getEngineLoudness()) / 100.0F));
               var1 = true;
            }

            if (var3.getInventoryItem().getDurability() > 0.0F) {
               var3.setDurability(var3.getInventoryItem().getDurability());
            }
         }
      }

      if (!var1) {
         this.engineLoudness *= 2;
      }

   }

   public void transmitEngine() {
      if (GameServer.bServer) {
         this.updateFlags = (short)(this.updateFlags | 4);
      }
   }

   public void setRust(float var1) {
      this.rust = PZMath.clamp(var1, 0.0F, 1.0F);
   }

   public float getRust() {
      return this.rust;
   }

   public void transmitRust() {
      if (GameServer.bServer) {
         this.updateFlags = (short)(this.updateFlags | 4096);
      }
   }

   public void transmitColorHSV() {
      if (GameServer.bServer) {
         this.updateFlags = (short)(this.updateFlags | 4096);
      }
   }

   public void transmitSkinIndex() {
      if (GameServer.bServer) {
         this.updateFlags = (short)(this.updateFlags | 4096);
      }
   }

   public void updateBulletStats() {
      if (!this.getScriptName().contains("Burnt") && WorldSimulation.instance.created) {
         float[] var1 = wheelParams;
         double var2 = 0.0;
         double var4 = 2.4;
         byte var6 = 100;
         float var7 = 1.0F;
         if (this.isInForest() && this.isDoingOffroad() && Math.abs(this.getCurrentSpeedKmHour()) > 1.0F) {
            var2 = (double)Rand.Next(0.04F, 0.13F);
            var7 = 0.7F;
            var6 = 15;
         } else if (this.isDoingOffroad() && Math.abs(this.getCurrentSpeedKmHour()) > 1.0F) {
            var6 = 25;
            var2 = (double)Rand.Next(0.02F, 0.1F);
            var7 = 0.7F;
         } else {
            if (Math.abs(this.getCurrentSpeedKmHour()) > 1.0F && Rand.Next(100) < 10) {
               var2 = (double)Rand.Next(0.01F, 0.05F);
            } else {
               var2 = 0.0;
            }

            var7 = 1.0F;
         }

         if (RainManager.isRaining()) {
            var7 -= 0.3F;
         }

         Vector3f var8 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();

         for(int var9 = 0; var9 < this.script.getWheelCount(); ++var9) {
            this.updateBulletStatsWheel(var9, var1, var8, var7, var6, var4, var2);
         }

         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var8);
         if (SystemDisabler.getdoVehicleLowRider() && this.isKeyboardControlled()) {
            float var11 = 1.6F;
            float var10 = 1.0F;
            float[] var10000;
            if (GameKeyboard.isKeyDown(79)) {
               var10000 = lowRiderParam;
               var10000[0] += (var11 - lowRiderParam[0]) * var10;
            } else {
               var10000 = lowRiderParam;
               var10000[0] += (0.0F - lowRiderParam[0]) * 0.05F;
            }

            if (GameKeyboard.isKeyDown(80)) {
               var10000 = lowRiderParam;
               var10000[1] += (var11 - lowRiderParam[1]) * var10;
            } else {
               var10000 = lowRiderParam;
               var10000[1] += (0.0F - lowRiderParam[1]) * 0.05F;
            }

            if (GameKeyboard.isKeyDown(75)) {
               var10000 = lowRiderParam;
               var10000[2] += (var11 - lowRiderParam[2]) * var10;
            } else {
               var10000 = lowRiderParam;
               var10000[2] += (0.0F - lowRiderParam[2]) * 0.05F;
            }

            if (GameKeyboard.isKeyDown(76)) {
               var10000 = lowRiderParam;
               var10000[3] += (var11 - lowRiderParam[3]) * var10;
            } else {
               var10000 = lowRiderParam;
               var10000[3] += (0.0F - lowRiderParam[3]) * 0.05F;
            }

            var1[23] = lowRiderParam[0];
            var1[22] = lowRiderParam[1];
            var1[21] = lowRiderParam[2];
            var1[20] = lowRiderParam[3];
         }

         Bullet.setVehicleParams(this.VehicleID, var1);
      }
   }

   private void updateBulletStatsWheel(int var1, float[] var2, Vector3f var3, float var4, int var5, double var6, double var8) {
      int var10 = var1 * 6;
      VehicleScript.Wheel var11 = this.script.getWheel(var1);
      Vector3f var12 = this.getWorldPos(var11.offset.x, var11.offset.y, var11.offset.z, var3);
      VehiclePart var13 = this.getPartById("Tire" + var11.getId());
      VehiclePart var14 = this.getPartById("Suspension" + var11.getId());
      if (var13 != null && var13.getInventoryItem() != null) {
         var2[var10 + 0] = 1.0F;
         var2[var10 + 1] = Math.min(var13.getContainerContentAmount() / (float)(var13.getContainerCapacity() - 10), 1.0F);
         var2[var10 + 2] = var4 * var13.getWheelFriction();
         if (var14 != null && var14.getInventoryItem() != null) {
            var2[var10 + 3] = var14.getSuspensionDamping();
            var2[var10 + 4] = var14.getSuspensionCompression();
         } else {
            var2[var10 + 3] = 0.1F;
            var2[var10 + 4] = 0.1F;
         }

         if (var5 > 0 && Rand.Next(var5) == 0) {
            var2[var10 + 5] = (float)(Math.sin(var6 * (double)var12.x()) * Math.sin(var6 * (double)var12.y()) * var8);
         } else {
            var2[var10 + 5] = 0.0F;
         }
      } else {
         var2[var10 + 0] = 0.0F;
         var2[var10 + 1] = 30.0F;
         var2[var10 + 2] = 0.0F;
         var2[var10 + 3] = 2.88F;
         var2[var10 + 4] = 3.83F;
         if (Rand.Next(var5) == 0) {
            var2[var10 + 5] = (float)(Math.sin(var6 * (double)var12.x()) * Math.sin(var6 * (double)var12.y()) * var8);
         } else {
            var2[var10 + 5] = 0.0F;
         }
      }

      if (this.forcedFriction > -1.0F) {
         var2[var10 + 2] = this.forcedFriction;
      }

   }

   public void setActiveInBullet(boolean var1) {
      if (var1 || !this.isEngineRunning()) {
         ;
      }
   }

   public boolean areAllDoorsLocked() {
      for(int var1 = 0; var1 < this.getMaxPassengers(); ++var1) {
         VehiclePart var2 = this.getPassengerDoor(var1);
         if (var2 != null && var2.getDoor() != null && !var2.getDoor().isLocked()) {
            return false;
         }
      }

      return true;
   }

   public boolean isAnyDoorLocked() {
      for(int var1 = 0; var1 < this.getMaxPassengers(); ++var1) {
         VehiclePart var2 = this.getPassengerDoor(var1);
         if (var2 != null && var2.getDoor() != null && var2.getDoor().isLocked()) {
            return true;
         }
      }

      return false;
   }

   public float getRemainingFuelPercentage() {
      VehiclePart var1 = this.getPartById("GasTank");
      return var1 == null ? 0.0F : var1.getContainerContentAmount() / (float)var1.getContainerCapacity() * 100.0F;
   }

   public int getMechanicalID() {
      return this.mechanicalID;
   }

   public void setMechanicalID(int var1) {
      this.mechanicalID = var1;
   }

   public boolean needPartsUpdate() {
      return this.needPartsUpdate;
   }

   public void setNeedPartsUpdate(boolean var1) {
      this.needPartsUpdate = var1;
   }

   public VehiclePart getHeater() {
      return this.getPartById("Heater");
   }

   public int windowsOpen() {
      int var1 = 0;

      for(int var2 = 0; var2 < this.getPartCount(); ++var2) {
         VehiclePart var3 = this.getPartByIndex(var2);
         if (var3.window != null && var3.window.open) {
            ++var1;
         }
      }

      return var1;
   }

   public boolean isAlarmed() {
      return this.alarmed;
   }

   public void setAlarmed(boolean var1) {
      this.alarmed = var1;
      if (var1) {
         this.setPreviouslyEntered(false);
      }

   }

   public void triggerAlarm() {
      if (this.alarmed && !this.previouslyEntered) {
         this.alarmed = false;
         this.alarmTime = Rand.Next(1500, 3000);
         this.alarmAccumulator = 0.0F;
      } else {
         this.alarmed = false;
      }
   }

   private void doAlarm() {
      if (this.alarmTime > 0) {
         if (this.getBatteryCharge() <= 0.0F) {
            if (this.soundHornOn) {
               this.onHornStop();
            }

            this.alarmTime = -1;
            return;
         }

         this.alarmAccumulator += GameTime.instance.getThirtyFPSMultiplier();
         if (this.alarmAccumulator >= (float)this.alarmTime) {
            this.onHornStop();
            this.setHeadlightsOn(false);
            this.alarmTime = -1;
            return;
         }

         int var1 = (int)this.alarmAccumulator / 20;
         if (!this.soundHornOn && var1 % 2 == 0) {
            this.onHornStart();
            this.setHeadlightsOn(true);
         }

         if (this.soundHornOn && var1 % 2 == 1) {
            this.onHornStop();
            this.setHeadlightsOn(false);
         }

         this.checkMusicIntensityEvent_AlarmNearby();
      }

   }

   private void checkMusicIntensityEvent_AlarmNearby() {
      if (!GameServer.bServer) {
         for(int var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
            IsoPlayer var2 = IsoPlayer.players[var1];
            if (var2 != null && !var2.isDeaf() && !var2.isDead()) {
               float var3 = IsoUtils.DistanceToSquared(this.getX(), this.getY(), var2.getX(), var2.getY());
               if (!(var3 > 2500.0F)) {
                  var2.triggerMusicIntensityEvent("AlarmNearby");
                  break;
               }
            }
         }

      }
   }

   public boolean isMechanicUIOpen() {
      return this.mechanicUIOpen;
   }

   public void setMechanicUIOpen(boolean var1) {
      this.mechanicUIOpen = var1;
   }

   public void damagePlayers(float var1) {
      if (SandboxOptions.instance.PlayerDamageFromCrash.getValue()) {
         if (!GameClient.bClient) {
            for(int var2 = 0; var2 < this.passengers.length; ++var2) {
               if (this.getPassenger(var2).character != null) {
                  IsoGameCharacter var3 = this.getPassenger(var2).character;
                  if (!var3.isGodMod()) {
                     if (GameServer.bServer && var3 instanceof IsoPlayer) {
                        GameServer.sendPlayerDamagedByCarCrash((IsoPlayer)var3, var1);
                     } else {
                        this.addRandomDamageFromCrash(var3, var1);
                        LuaEventManager.triggerEvent("OnPlayerGetDamage", var3, "CARCRASHDAMAGE", var1);
                     }
                  }
               }
            }

         }
      }
   }

   public void addRandomDamageFromCrash(IsoGameCharacter var1, float var2) {
      int var3 = 1;
      if (var2 > 40.0F) {
         var3 = Rand.Next(1, 3);
      }

      if (var2 > 70.0F) {
         var3 = Rand.Next(2, 4);
      }

      int var4 = 0;

      int var5;
      for(var5 = 0; var5 < var1.getVehicle().getPartCount(); ++var5) {
         VehiclePart var6 = var1.getVehicle().getPartByIndex(var5);
         if (var6.window != null && var6.getCondition() < 15) {
            ++var4;
         }
      }

      for(var5 = 0; var5 < var3; ++var5) {
         int var9 = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.MAX));
         BodyPart var7 = var1.getBodyDamage().getBodyPart(BodyPartType.FromIndex(var9));
         float var8 = Math.max(Rand.Next(var2 - 15.0F, var2), 5.0F);
         if (var1.Traits.FastHealer.isSet()) {
            var8 = (float)((double)var8 * 0.8);
         } else if (var1.Traits.SlowHealer.isSet()) {
            var8 = (float)((double)var8 * 1.2);
         }

         switch (SandboxOptions.instance.InjurySeverity.getValue()) {
            case 1:
               var8 *= 0.5F;
               break;
            case 3:
               var8 *= 1.5F;
         }

         var8 *= this.getScript().getPlayerDamageProtection();
         var8 = (float)((double)var8 * 0.9);
         var7.AddDamage(var8);
         if (var8 > 40.0F && Rand.Next(12) == 0) {
            var7.generateDeepWound();
         } else if (var8 > 50.0F && Rand.Next(10) == 0 && SandboxOptions.instance.BoneFracture.getValue()) {
            if (var7.getType() != BodyPartType.Neck && var7.getType() != BodyPartType.Groin) {
               var7.generateFracture(Rand.Next(Rand.Next(10.0F, var8 + 10.0F), Rand.Next(var8 + 20.0F, var8 + 30.0F)));
            } else {
               var7.generateDeepWound();
            }
         }

         if (var8 > 30.0F && Rand.Next(12 - var4) == 0) {
            var7 = var1.getBodyDamage().setScratchedWindow();
            if (Rand.Next(5) == 0) {
               var7.generateDeepWound();
               var7.setHaveGlass(true);
            }
         }
      }

   }

   public boolean isTrunkLocked() {
      VehiclePart var1 = this.getPartById("TrunkDoor");
      if (var1 == null) {
         var1 = this.getPartById("DoorRear");
      }

      return var1 != null && var1.getDoor() != null && var1.getInventoryItem() != null ? var1.getDoor().isLocked() : false;
   }

   public void setTrunkLocked(boolean var1) {
      VehiclePart var2 = this.getPartById("TrunkDoor");
      if (var2 == null) {
         var2 = this.getPartById("DoorRear");
      }

      if (var2 != null && var2.getDoor() != null && var2.getInventoryItem() != null) {
         var2.getDoor().setLocked(var1);
         if (GameServer.bServer) {
            this.transmitPartDoor(var2);
         }
      }

   }

   public VehiclePart getNearestBodyworkPart(IsoGameCharacter var1) {
      for(int var2 = 0; var2 < this.getPartCount(); ++var2) {
         VehiclePart var3 = this.getPartByIndex(var2);
         if (("door".equals(var3.getCategory()) || "bodywork".equals(var3.getCategory())) && this.isInArea(var3.getArea(), var1) && var3.getCondition() > 0) {
            return var3;
         }
      }

      return null;
   }

   public double getSirenStartTime() {
      return this.sirenStartTime;
   }

   public void setSirenStartTime(double var1) {
      this.sirenStartTime = var1;
   }

   public boolean sirenShutoffTimeExpired() {
      double var1 = SandboxOptions.instance.SirenShutoffHours.getValue();
      if (var1 <= 0.0) {
         return false;
      } else {
         double var3 = GameTime.instance.getWorldAgeHours();
         if (this.sirenStartTime > var3) {
            this.sirenStartTime = var3;
         }

         return this.sirenStartTime + var1 < var3;
      }
   }

   public void repair() {
      for(int var1 = 0; var1 < this.getPartCount(); ++var1) {
         VehiclePart var2 = this.getPartByIndex(var1);
         var2.repair();
      }

      this.rust = 0.0F;
      this.transmitRust();
      this.bloodIntensity.clear();
      this.transmitBlood();
      this.doBloodOverlay();
   }

   public boolean isAnyListenerInside() {
      for(int var1 = 0; var1 < this.getMaxPassengers(); ++var1) {
         IsoGameCharacter var2 = this.getCharacter(var1);
         if (var2 instanceof IsoPlayer && ((IsoPlayer)var2).isLocalPlayer() && !var2.Traits.Deaf.isSet()) {
            return true;
         }
      }

      return false;
   }

   public boolean couldCrawlerAttackPassenger(IsoGameCharacter var1) {
      int var2 = this.getSeat(var1);
      return var2 == -1 ? false : false;
   }

   public boolean isGoodCar() {
      return this.isGoodCar;
   }

   public void setGoodCar(boolean var1) {
      this.isGoodCar = var1;
   }

   public InventoryItem getCurrentKey() {
      return this.currentKey;
   }

   public void setCurrentKey(InventoryItem var1) {
      this.currentKey = var1;
   }

   public boolean isInForest() {
      return this.getSquare() != null && this.getSquare().getZone() != null && ("Forest".equals(this.getSquare().getZone().getType()) || "DeepForest".equals(this.getSquare().getZone().getType()) || "FarmLand".equals(this.getSquare().getZone().getType()));
   }

   public boolean shouldNotHaveLoot() {
      if (this.getSquare() != null && IsoWorld.instance.MetaGrid.getVehicleZoneAt(this.getSquare().getX(), this.getSquare().getY(), this.getSquare().getZ()) != null && Objects.equals(((VehicleZone)Objects.requireNonNull(IsoWorld.instance.MetaGrid.getVehicleZoneAt(this.getSquare().getX(), this.getSquare().getY(), this.getSquare().getZ()))).name, "junkyard")) {
         return true;
      } else {
         return this.getSquare() != null && IsoWorld.instance.MetaGrid.getVehicleZoneAt(this.getSquare().getX(), this.getSquare().getY(), this.getSquare().getZ()) != null && Objects.equals(((VehicleZone)Objects.requireNonNull(IsoWorld.instance.MetaGrid.getVehicleZoneAt(this.getSquare().getX(), this.getSquare().getY(), this.getSquare().getZ()))).name, "luxuryDealership");
      }
   }

   public float getOffroadEfficiency() {
      return this.isInForest() ? this.script.getOffroadEfficiency() * 1.5F : this.script.getOffroadEfficiency() * 2.0F;
   }

   public void doChrHitImpulse(IsoObject var1) {
      float var2 = 22.0F;
      Vector3f var3 = this.getLinearVelocity((Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
      var3.y = 0.0F;
      Vector3f var4 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
      var4.set(this.getX() - var1.getX(), 0.0F, this.getZ() - var1.getY());
      var4.normalize();
      var3.mul(var4);
      ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var4);
      float var5 = var3.length();
      var5 = Math.min(var5, var2);
      if (var5 < 0.05F) {
         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var3);
      } else {
         if (GameServer.bServer) {
            if (var1 instanceof IsoZombie) {
               ((IsoZombie)var1).setThumpFlag(1);
            }
         } else {
            SoundManager.instance.PlayWorldSound("ZombieThumpGeneric", var1.square, 0.0F, 20.0F, 0.9F, true);
         }

         Vector3f var6 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
         var6.set(this.getX() - var1.getX(), 0.0F, this.getY() - var1.getY());
         var6.normalize();
         var3.normalize();
         float var7 = var3.dot(var6);
         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var3);
         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var6);
         this.ApplyImpulse(var1, this.getFudgedMass() * 3.0F * var5 / var2 * Math.abs(var7));
      }
   }

   public boolean isDoColor() {
      return this.doColor;
   }

   public void setDoColor(boolean var1) {
      this.doColor = var1;
   }

   public float getBrakeSpeedBetweenUpdate() {
      return this.brakeBetweenUpdatesSpeed;
   }

   public IsoGridSquare getSquare() {
      return this.getCell().getGridSquare((double)this.getX(), (double)this.getY(), (double)this.getZ());
   }

   public void setColor(float var1, float var2, float var3) {
      this.colorValue = var1;
      this.colorSaturation = var2;
      this.colorHue = var3;
   }

   public void setColorHSV(float var1, float var2, float var3) {
      this.colorHue = var1;
      this.colorSaturation = var2;
      this.colorValue = var3;
   }

   public float getColorHue() {
      return this.colorHue;
   }

   public float getColorSaturation() {
      return this.colorSaturation;
   }

   public float getColorValue() {
      return this.colorValue;
   }

   public boolean isRemovedFromWorld() {
      return this.removedFromWorld;
   }

   public float getInsideTemperature() {
      VehiclePart var1 = this.getPartById("PassengerCompartment");
      float var2 = 0.0F;
      if (var1 != null && var1.getModData() != null) {
         if (var1.getModData().rawget("temperature") != null) {
            var2 += ((Double)var1.getModData().rawget("temperature")).floatValue();
         }

         if (var1.getModData().rawget("windowtemperature") != null) {
            var2 += ((Double)var1.getModData().rawget("windowtemperature")).floatValue();
         }
      }

      return var2;
   }

   public AnimationPlayer getAnimationPlayer() {
      String var1 = this.getScript().getModel().file;
      Model var2 = ModelManager.instance.getLoadedModel(var1);
      if (var2 != null && !var2.bStatic) {
         if (this.m_animPlayer != null && this.m_animPlayer.getModel() != var2) {
            this.m_animPlayer = (AnimationPlayer)Pool.tryRelease((IPooledObject)this.m_animPlayer);
         }

         if (this.m_animPlayer == null) {
            this.m_animPlayer = AnimationPlayer.alloc(var2);
         }

         return this.m_animPlayer;
      } else {
         return null;
      }
   }

   public void releaseAnimationPlayers() {
      this.m_animPlayer = (AnimationPlayer)Pool.tryRelease((IPooledObject)this.m_animPlayer);
      PZArrayUtil.forEach((List)this.models, ModelInfo::releaseAnimationPlayer);
   }

   public void setAddThumpWorldSound(boolean var1) {
      this.bAddThumpWorldSound = var1;
   }

   public void createImpulse(Vector3f var1) {
   }

   public void Thump(IsoMovingObject var1) {
      VehiclePart var2 = this.getPartById("lightbar");
      if (var2 != null) {
         if (var2.getCondition() <= 0) {
            var1.setThumpTarget((Thumpable)null);
         }

         VehiclePart var3 = this.getUseablePart((IsoGameCharacter)var1);
         if (var3 != null) {
            var3.setCondition(var3.getCondition() - Rand.Next(1, 5));
         }

         var2.setCondition(var2.getCondition() - Rand.Next(1, 5));
      }
   }

   public void WeaponHit(IsoGameCharacter var1, HandWeapon var2) {
   }

   public Thumpable getThumpableFor(IsoGameCharacter var1) {
      return null;
   }

   public float getThumpCondition() {
      return 1.0F;
   }

   public boolean isRegulator() {
      return this.regulator;
   }

   public void setRegulator(boolean var1) {
      this.regulator = var1;
   }

   public float getRegulatorSpeed() {
      return this.regulatorSpeed;
   }

   public void setRegulatorSpeed(float var1) {
      this.regulatorSpeed = var1;
   }

   public float getCurrentSpeedForRegulator() {
      return (float)Math.max(5.0 * Math.floor((double)(this.jniSpeed / 5.0F)), 5.0);
   }

   public void setVehicleTowing(BaseVehicle var1, String var2, String var3) {
      this.vehicleTowing = var1;
      this.vehicleTowingID = this.vehicleTowing == null ? -1 : this.vehicleTowing.getSqlId();
      this.towAttachmentSelf = var2;
      this.towAttachmentOther = var3;
      this.towConstraintZOffset = 0.0F;
   }

   public void setVehicleTowedBy(BaseVehicle var1, String var2, String var3) {
      this.vehicleTowedBy = var1;
      this.vehicleTowedByID = this.vehicleTowedBy == null ? -1 : this.vehicleTowedBy.getSqlId();
      this.towAttachmentSelf = var3;
      this.towAttachmentOther = var2;
      this.towConstraintZOffset = 0.0F;
   }

   public BaseVehicle getVehicleTowing() {
      return this.vehicleTowing;
   }

   public BaseVehicle getVehicleTowedBy() {
      return this.vehicleTowedBy;
   }

   public boolean attachmentExist(String var1) {
      VehicleScript var2 = this.getScript();
      if (var2 == null) {
         return false;
      } else {
         ModelAttachment var3 = var2.getAttachmentById(var1);
         return var3 != null;
      }
   }

   public Vector3f getAttachmentLocalPos(String var1, Vector3f var2) {
      VehicleScript var3 = this.getScript();
      if (var3 == null) {
         return null;
      } else {
         ModelAttachment var4 = var3.getAttachmentById(var1);
         if (var4 == null) {
            return null;
         } else {
            var2.set(var4.getOffset());
            return var3.getModel() == null ? var2 : var2.add(var3.getModel().getOffset());
         }
      }
   }

   public Vector3f getAttachmentWorldPos(String var1, Vector3f var2) {
      var2 = this.getAttachmentLocalPos(var1, var2);
      return var2 == null ? null : this.getWorldPos(var2, var2);
   }

   public void setForceBrake() {
      this.getController().clientControls.forceBrake = System.currentTimeMillis();
   }

   public Vector3f getTowingLocalPos(String var1, Vector3f var2) {
      return this.getAttachmentLocalPos(var1, var2);
   }

   public Vector3f getTowedByLocalPos(String var1, Vector3f var2) {
      return this.getAttachmentLocalPos(var1, var2);
   }

   public Vector3f getTowingWorldPos(String var1, Vector3f var2) {
      var2 = this.getTowingLocalPos(var1, var2);
      return var2 == null ? null : this.getWorldPos(var2, var2);
   }

   public Vector3f getTowedByWorldPos(String var1, Vector3f var2) {
      var2 = this.getTowedByLocalPos(var1, var2);
      return var2 == null ? null : this.getWorldPos(var2, var2);
   }

   public Vector3f getPlayerTrailerLocalPos(String var1, boolean var2, Vector3f var3) {
      ModelAttachment var4 = this.getScript().getAttachmentById(var1);
      if (var4 == null) {
         return null;
      } else {
         Vector3f var5 = this.getScript().getExtents();
         Vector3f var6 = this.getScript().getCenterOfMassOffset();
         float var7 = var6.x + var5.x / 2.0F + 0.3F + 0.05F;
         if (!var2) {
            var7 *= -1.0F;
         }

         return var4.getOffset().z > 0.0F ? var3.set(var7, 0.0F, var6.z + var5.z / 2.0F + 0.3F + 0.05F) : var3.set(var7, 0.0F, var6.z - (var5.z / 2.0F + 0.3F + 0.05F));
      }
   }

   public Vector3f getPlayerTrailerWorldPos(String var1, boolean var2, Vector3f var3) {
      var3 = this.getPlayerTrailerLocalPos(var1, var2, var3);
      if (var3 == null) {
         return null;
      } else {
         this.getWorldPos(var3, var3);
         var3.z = (float)PZMath.fastfloor(this.getZ());
         Vector3f var4 = this.getTowingWorldPos(var1, (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc());
         boolean var5 = PolygonalMap2.instance.lineClearCollide(var3.x, var3.y, var4.x, var4.y, PZMath.fastfloor(this.getZ()), this, false, false);
         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var4);
         return var5 ? null : var3;
      }
   }

   private void drawTowingRope() {
      BaseVehicle var1 = this.getVehicleTowing();
      if (var1 != null) {
         Vector3fObjectPool var2 = (Vector3fObjectPool)TL_vector3f_pool.get();
         this.getAttachmentWorldPos("trailer", (Vector3f)var2.alloc());
         Vector3f var4 = this.getAttachmentWorldPos("trailerfront", (Vector3f)var2.alloc());
         ModelAttachment var5 = this.script.getAttachmentById("trailerfront");
         if (var5 != null) {
            var4.set(var5.getOffset());
         }

         Vector2 var6 = new Vector2();
         var6.x = var1.getX();
         var6.y = var1.getY();
         var6.x -= this.getX();
         var6.y -= this.getY();
         var6.setLength(2.0F);
         this.drawDirectionLine(var6, var6.getLength(), 1.0F, 0.5F, 0.5F);
      }
   }

   public void drawDirectionLine(Vector2 var1, float var2, float var3, float var4, float var5) {
      float var6 = this.getX() + var1.x * var2;
      float var7 = this.getY() + var1.y * var2;
      float var8 = IsoUtils.XToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
      float var9 = IsoUtils.YToScreenExact(this.getX(), this.getY(), this.getZ(), 0);
      float var10 = IsoUtils.XToScreenExact(var6, var7, this.getZ(), 0);
      float var11 = IsoUtils.YToScreenExact(var6, var7, this.getZ(), 0);
      LineDrawer.drawLine(var8, var9, var10, var11, var3, var4, var5, 0.5F, 1);
   }

   public void addPointConstraint(IsoPlayer var1, BaseVehicle var2, String var3, String var4) {
      this.addPointConstraint(var1, var2, var3, var4, false);
   }

   public void addPointConstraint(IsoPlayer var1, BaseVehicle var2, String var3, String var4, Boolean var5) {
      this.setPreviouslyMoved(true);
      if (var2 == null || var1 != null && (IsoUtils.DistanceToSquared(var1.getX(), var1.getY(), this.getX(), this.getY()) > 100.0F || IsoUtils.DistanceToSquared(var1.getX(), var1.getY(), var2.getX(), var2.getY()) > 100.0F)) {
         DebugLog.Vehicle.warn("The " + var1.getUsername() + " user attached vehicles at a long distance");
      }

      this.breakConstraint(true, var5);
      var2.breakConstraint(true, var5);
      Vector3fObjectPool var7 = (Vector3fObjectPool)TL_vector3f_pool.get();
      Vector3f var8 = this.getTowingLocalPos(var3, (Vector3f)var7.alloc());
      Vector3f var9 = var2.getTowedByLocalPos(var4, (Vector3f)var7.alloc());
      if (var8 != null && var9 != null) {
         if (!GameServer.bServer) {
            if (!this.getScriptName().contains("Trailer") && !var2.getScriptName().contains("Trailer")) {
               this.constraintTowing = Bullet.addRopeConstraint(this.VehicleID, var2.VehicleID, var8.x, var8.y, var8.z, var9.x, var9.y, var9.z, 1.5F);
            } else {
               this.constraintTowing = Bullet.addPointConstraint(this.VehicleID, var2.VehicleID, var8.x, var8.y, var8.z, var9.x, var9.y, var9.z);
            }
         }

         var2.constraintTowing = this.constraintTowing;
         this.setVehicleTowing(var2, var3, var4);
         var2.setVehicleTowedBy(this, var3, var4);
         var7.release(var8);
         var7.release(var9);
         this.constraintChanged();
         var2.constraintChanged();
         if (GameServer.bServer && var1 != null && this.netPlayerAuthorization == BaseVehicle.Authorization.Server && var2.netPlayerAuthorization == BaseVehicle.Authorization.Server) {
            this.setNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide, var1.OnlineID);
            this.authSimulationTime = System.currentTimeMillis();
            var2.setNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide, var1.OnlineID);
            var2.authSimulationTime = System.currentTimeMillis();
         }

         if (GameServer.bServer && !var5) {
            VehicleManager.instance.sendTowing(this, var2, var3, var4);
         }

      } else {
         if (var8 != null) {
            var7.release(var8);
         }

         if (var9 != null) {
            var7.release(var9);
         }

      }
   }

   public void authorizationChanged(IsoGameCharacter var1) {
      if (var1 != null) {
         this.setNetPlayerAuthorization(BaseVehicle.Authorization.Local, var1.getOnlineID());
      } else {
         this.setNetPlayerAuthorization(BaseVehicle.Authorization.Server, -1);
      }

   }

   public void constraintChanged() {
      long var1 = System.currentTimeMillis();
      this.setPhysicsActive(true);
      this.constraintChangedTime = var1;
      if (GameServer.bServer) {
         if (this.getVehicleTowing() != null) {
            this.authorizationChanged(this.getDriver());
            this.getVehicleTowing().authorizationChanged(this.getDriver());
         } else if (this.getVehicleTowedBy() != null) {
            this.authorizationChanged(this.getVehicleTowedBy().getDriver());
            this.getVehicleTowedBy().authorizationChanged(this.getVehicleTowedBy().getDriver());
         } else {
            this.authorizationChanged(this.getDriver());
         }
      }

   }

   public void breakConstraint(boolean var1, boolean var2) {
      if (GameServer.bServer || this.constraintTowing != -1) {
         if (!GameServer.bServer) {
            Bullet.removeConstraint(this.constraintTowing);
         }

         this.constraintTowing = -1;
         if (this.vehicleTowing != null) {
            if (GameServer.bServer && !var2) {
               VehicleManager.instance.sendDetachTowing(this, this.vehicleTowing);
            }

            this.vehicleTowing.vehicleTowedBy = null;
            this.vehicleTowing.constraintTowing = -1;
            if (var1) {
               this.vehicleTowingID = -1;
               this.vehicleTowing.vehicleTowedByID = -1;
            }

            this.vehicleTowing.constraintChanged();
            this.vehicleTowing = null;
         }

         if (this.vehicleTowedBy != null) {
            if (GameServer.bServer && !var2) {
               VehicleManager.instance.sendDetachTowing(this.vehicleTowedBy, this);
            }

            this.vehicleTowedBy.vehicleTowing = null;
            this.vehicleTowedBy.constraintTowing = -1;
            if (var1) {
               this.vehicleTowedBy.vehicleTowingID = -1;
               this.vehicleTowedByID = -1;
            }

            this.vehicleTowedBy.constraintChanged();
            this.vehicleTowedBy = null;
         }

         this.constraintChanged();
      }
   }

   public boolean canAttachTrailer(BaseVehicle var1, String var2, String var3) {
      return this.canAttachTrailer(var1, var2, var3, false);
   }

   public boolean canAttachTrailer(BaseVehicle var1, String var2, String var3, boolean var4) {
      if (this != var1 && this.physics != null && this.constraintTowing == -1) {
         if (var1 != null && var1.physics != null && var1.constraintTowing == -1) {
            Vector3fObjectPool var6 = (Vector3fObjectPool)TL_vector3f_pool.get();
            Vector3f var7 = this.getTowingWorldPos(var2, (Vector3f)var6.alloc());
            Vector3f var8 = var1.getTowedByWorldPos(var3, (Vector3f)var6.alloc());
            if (var7 != null && var8 != null) {
               float var9 = IsoUtils.DistanceToSquared(var7.x, var7.y, 0.0F, var8.x, var8.y, 0.0F);
               var6.release(var7);
               var6.release(var8);
               ModelAttachment var10 = this.script.getAttachmentById(var2);
               ModelAttachment var11 = var1.script.getAttachmentById(var3);
               if (var10 != null && var10.getCanAttach() != null && !var10.getCanAttach().contains(var3)) {
                  return false;
               } else if (var11 != null && var11.getCanAttach() != null && !var11.getCanAttach().contains(var2)) {
                  return false;
               } else {
                  boolean var12 = this.getScriptName().contains("Trailer") || var1.getScriptName().contains("Trailer");
                  return var9 < (var4 ? 10.0F : (var12 ? 1.0F : 4.0F));
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void tryReconnectToTowedVehicle() {
      if (GameClient.bClient) {
         short var5 = VehicleManager.instance.getTowedVehicleID(this.VehicleID);
         if (var5 != -1) {
            BaseVehicle var6 = VehicleManager.instance.getVehicleByID(var5);
            if (var6 != null) {
               if (this.canAttachTrailer(var6, this.towAttachmentSelf, this.towAttachmentOther, true)) {
                  this.addPointConstraint((IsoPlayer)null, var6, this.towAttachmentSelf, this.towAttachmentOther, true);
               }
            }
         }
      } else if (this.vehicleTowing == null) {
         if (this.vehicleTowingID != -1) {
            BaseVehicle var1 = null;
            ArrayList var2 = IsoWorld.instance.CurrentCell.getVehicles();

            for(int var3 = 0; var3 < var2.size(); ++var3) {
               BaseVehicle var4 = (BaseVehicle)var2.get(var3);
               if (var4.getSqlId() == this.vehicleTowingID) {
                  var1 = var4;
                  break;
               }
            }

            if (var1 != null) {
               if (this.canAttachTrailer(var1, this.towAttachmentSelf, this.towAttachmentOther, true)) {
                  this.addPointConstraint((IsoPlayer)null, var1, this.towAttachmentSelf, this.towAttachmentOther, false);
               }
            }
         }
      }
   }

   public void positionTrailer(BaseVehicle var1) {
      if (var1 != null) {
         Vector3fObjectPool var2 = (Vector3fObjectPool)TL_vector3f_pool.get();
         Vector3f var3 = this.getTowingWorldPos("trailer", (Vector3f)var2.alloc());
         Vector3f var4 = var1.getTowedByWorldPos("trailer", (Vector3f)var2.alloc());
         if (var3 != null && var4 != null) {
            var4.sub(var1.getX(), var1.getY(), var1.getZ());
            var3.sub(var4);
            Transform var5 = var1.getWorldTransform(allocTransform());
            var5.origin.set(var3.x - WorldSimulation.instance.offsetX, var1.jniTransform.origin.y, var3.y - WorldSimulation.instance.offsetY);
            var1.setWorldTransform(var5);
            releaseTransform(var5);
            var1.setX(var3.x);
            var1.setLastX(var3.x);
            var1.setY(var3.y);
            var1.setLastY(var3.y);
            var1.setCurrent(this.getCell().getGridSquare((double)var3.x, (double)var3.y, 0.0));
            this.addPointConstraint((IsoPlayer)null, var1, "trailer", "trailer");
            var2.release(var3);
            var2.release(var4);
         }
      }
   }

   public String getTowAttachmentSelf() {
      return this.towAttachmentSelf;
   }

   public String getTowAttachmentOther() {
      return this.towAttachmentOther;
   }

   public VehicleEngineRPM getVehicleEngineRPM() {
      if (this.vehicleEngineRPM == null) {
         this.vehicleEngineRPM = ScriptManager.instance.getVehicleEngineRPM(this.getScript().getEngineRPMType());
         if (this.vehicleEngineRPM == null) {
            DebugLog.Vehicle.warn("unknown vehicleEngineRPM \"%s\"", this.getScript().getEngineRPMType());
            this.vehicleEngineRPM = new VehicleEngineRPM();
         }
      }

      return this.vehicleEngineRPM;
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

   private void stopEngineSounds() {
      if (this.emitter != null) {
         for(int var1 = 0; var1 < this.new_EngineSoundId.length; ++var1) {
            if (this.new_EngineSoundId[var1] != 0L) {
               this.getEmitter().stopSound(this.new_EngineSoundId[var1]);
               this.new_EngineSoundId[var1] = 0L;
            }
         }

         if (this.combinedEngineSound != 0L) {
            if (this.getEmitter().hasSustainPoints(this.combinedEngineSound)) {
               this.getEmitter().triggerCue(this.combinedEngineSound);
            } else {
               this.getEmitter().stopSound(this.combinedEngineSound);
            }

            this.combinedEngineSound = 0L;
         }

      }
   }

   public BaseVehicle setSmashed(String var1) {
      return this.setSmashed(var1, false);
   }

   public BaseVehicle setSmashed(String var1, boolean var2) {
      String var3 = null;
      Integer var4 = null;
      KahluaTableImpl var5 = (KahluaTableImpl)LuaManager.env.rawget("SmashedCarDefinitions");
      if (var5 != null) {
         KahluaTableImpl var6 = (KahluaTableImpl)var5.rawget("cars");
         if (var6 != null) {
            KahluaTableImpl var7 = (KahluaTableImpl)var6.rawget(this.getScriptName());
            if (var7 != null) {
               var3 = var7.rawgetStr(var1.toLowerCase());
               var4 = var7.rawgetInt("skin");
               if (var4 == -1) {
                  var4 = this.getSkinIndex();
               }
            }
         }
      }

      int var10 = this.getKeyId();
      if (var3 != null) {
         this.removeFromWorld();
         this.permanentlyRemove();
         BaseVehicle var11 = new BaseVehicle(IsoWorld.instance.CurrentCell);
         var11.setScriptName(var3);
         var11.setScript();
         var11.setSkinIndex(var4);
         var11.setX(this.getX());
         var11.setY(this.getY());
         var11.setZ(this.getZ());
         var11.setDir(this.getDir());
         var11.savedRot.set(this.savedRot);
         var11.savedPhysicsZ = this.savedPhysicsZ;
         if (var2) {
            float var8 = this.getAngleY();
            var11.savedRot.rotationXYZ(0.0F, var8 * 0.017453292F, 3.1415927F);
         }

         var11.jniTransform.setRotation(var11.savedRot);
         if (IsoChunk.doSpawnedVehiclesInInvalidPosition(var11)) {
            var11.setSquare(this.square);
            var11.square.chunk.vehicles.add(var11);
            var11.chunk = var11.square.chunk;
            var11.addToWorld();
            VehiclesDB2.instance.addVehicle(var11);
         }

         var11.setGeneralPartCondition(0.5F, 60.0F);
         VehiclePart var12 = var11.getPartById("Engine");
         if (var12 != null) {
            var12.setCondition(0);
         }

         VehiclePart var9 = var11.getPartById("GloveBox");
         if (var9 != null) {
            var9.setInventoryItem((InventoryItem)null);
            var9.setCondition(0);
         }

         var11.engineQuality = 0;
         var11.setKeyId(var10);
         return var11;
      } else {
         return this;
      }
   }

   public boolean isCollided(IsoGameCharacter var1) {
      if (GameClient.bClient && this.getDriver() != null && !this.getDriver().isLocal()) {
         return true;
      } else {
         Vector2 var2 = this.testCollisionWithCharacter(var1, 0.20000002F, this.hitVars.collision);
         return var2 != null && var2.x != -1.0F;
      }
   }

   public HitVars checkCollision(IsoGameCharacter var1) {
      if (var1.isProne()) {
         int var2 = this.testCollisionWithProneCharacter(var1, true);
         if (var2 > 0) {
            this.hitVars.calc(var1, this);
            this.hitCharacter(var1, this.hitVars);
            return this.hitVars;
         } else {
            return null;
         }
      } else {
         this.hitVars.calc(var1, this);
         this.hitCharacter(var1, this.hitVars);
         return this.hitVars;
      }
   }

   public boolean updateHitByVehicle(IsoGameCharacter var1) {
      if (var1.isVehicleCollisionActive(this) && (this.isCollided(var1) || var1.isCollidedWithVehicle()) && this.physics != null) {
         HitVars var2 = this.checkCollision(var1);
         if (var2 != null) {
            var1.doHitByVehicle(this, var2);
            return true;
         }
      }

      return false;
   }

   public void hitCharacter(IsoGameCharacter var1, HitVars var2) {
      if (var2.dot < 0.0F && !GameServer.bServer) {
         this.ApplyImpulse(var1, var2.vehicleImpulse);
      }

      long var3 = System.currentTimeMillis();
      long var5 = (var3 - this.zombieHitTimestamp) / 1000L;
      this.zombiesHits = Math.max(this.zombiesHits - (int)var5, 0);
      if (var3 - this.zombieHitTimestamp > 700L) {
         this.zombieHitTimestamp = var3;
         ++this.zombiesHits;
         this.zombiesHits = Math.min(this.zombiesHits, 20);
      }

      if (var1 instanceof IsoPlayer) {
         ((IsoPlayer)var1).setVehicleHitLocation(this);
      } else if (var1 instanceof IsoZombie) {
         ((IsoZombie)var1).setVehicleHitLocation(this);
      }

      if (var2.vehicleSpeed >= 5.0F || this.zombiesHits > 10) {
         var2.vehicleSpeed = this.getCurrentSpeedKmHour() / 5.0F;
         Vector3f var7 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
         this.getLocalPos(var1.getX(), var1.getY(), var1.getZ(), var7);
         int var8;
         if (var7.z > 0.0F) {
            var8 = this.caclulateDamageWithBodies(true);
            if (!GameClient.bClient) {
               this.addDamageFrontHitAChr(var8);
            }

            DebugLog.Vehicle.trace("Damage car front hits=%d damage=%d", this.zombiesHits, var8);
            var2.vehicleDamage = var8;
            var2.isVehicleHitFromFront = true;
         } else {
            var8 = this.caclulateDamageWithBodies(false);
            if (!GameClient.bClient) {
               this.addDamageRearHitAChr(var8);
            }

            DebugLog.Vehicle.trace("Damage car rear hits=%d damage=%d", this.zombiesHits, var8);
            var2.vehicleDamage = var8;
            var2.isVehicleHitFromFront = false;
         }

         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var7);
      }

   }

   public float getAnimalTrailerSize() {
      return this.getScript().getAnimalTrailerSize();
   }

   public ArrayList<IsoAnimal> getAnimals() {
      return this.animals;
   }

   public void addAnimalFromHandsInTrailer(IsoAnimal var1, IsoPlayer var2) {
      this.animals.add(var1);
      var1.setVehicle(this);
      AnimalInventoryItem var3 = var2.getInventory().getAnimalInventoryItem(var1);
      if (var3 != null) {
         var2.getInventory().Remove((InventoryItem)var3);
      } else {
         DebugLog.Animal.error("Animal not found: id=%d/%d", var1.getAnimalID(), var1.getOnlineID());
      }

      var2.setPrimaryHandItem((InventoryItem)null);
      var2.setSecondaryHandItem((InventoryItem)null);
      this.recalcAnimalSize();
   }

   public void addAnimalFromHandsInTrailer(IsoDeadBody var1, IsoPlayer var2) {
      IsoAnimal var3 = IsoAnimal.createAnimalFromCorpse(var1);
      var3.setHealth(0.0F);
      InventoryItem var4 = var2.getPrimaryHandItem();
      var2.getInventory().Remove(var4);
      var2.setPrimaryHandItem((InventoryItem)null);
      var2.setSecondaryHandItem((InventoryItem)null);
      this.addAnimalInTrailer(var3);
   }

   public void addAnimalInTrailer(IsoDeadBody var1) {
      IsoAnimal var2 = IsoAnimal.createAnimalFromCorpse(var1);
      var2.setHealth(0.0F);
      this.addAnimalInTrailer(var2);
      var1.getSquare().removeCorpse(var1, false);
      var1.invalidateCorpse();
   }

   public void addAnimalInTrailer(IsoAnimal var1) {
      this.animals.add(var1);
      if (var1.mother != null) {
         var1.attachBackToMother = var1.mother.animalID;
      }

      var1.setVehicle(this);
      if (var1.getData().getAttachedPlayer() != null) {
         var1.getData().getAttachedPlayer().removeAttachedAnimal(var1);
         var1.getData().setAttachedPlayer((IsoPlayer)null);
      }

      var1.removeFromWorld();
      var1.removeFromSquare();
      var1.setX(this.getX());
      var1.setY(this.getY());
      this.recalcAnimalSize();
   }

   private void recalcAnimalSize() {
      this.totalAnimalSize = 0.0F;

      for(int var1 = 0; var1 < this.animals.size(); ++var1) {
         this.totalAnimalSize += ((IsoAnimal)this.animals.get(var1)).getAnimalTrailerSize();
      }

   }

   public IsoObject removeAnimalFromTrailer(IsoAnimal var1) {
      Object var2 = null;

      for(int var3 = 0; var3 < this.animals.size(); ++var3) {
         IsoAnimal var4 = (IsoAnimal)this.animals.get(var3);
         if (var4 == var1) {
            this.animals.remove(var4);
            Vector2 var5 = this.getAreaCenter("AnimalEntry");
            IsoAnimal var6 = new IsoAnimal(this.getSquare().getCell(), (int)var5.x, (int)var5.y, this.getSquare().z, var1.getAnimalType(), var1.getBreed());
            var6.copyFrom(var1);
            AnimalInstanceManager.getInstance().remove(var1);
            AnimalInstanceManager.getInstance().add(var6, var1.getOnlineID());
            var6.attachBackToMotherTimer = 10000.0F;
            var2 = var6;
            if (var1.getHealth() == 0.0F) {
               IsoDeadBody var7 = new IsoDeadBody(var6);
               if (var6.getSquare() != null) {
                  var6.getSquare().addCorpse(var7, false);
                  var7.invalidateCorpse();
               }

               var2 = var7;
            }
         }
      }

      this.recalcAnimalSize();
      return (IsoObject)var2;
   }

   public void replaceGrownAnimalInTrailer(IsoAnimal var1, IsoAnimal var2) {
      if (var1 != null && var2 != null && var1 != var2 && !this.animals.contains(var2)) {
         for(int var3 = 0; var3 < this.animals.size(); ++var3) {
            IsoAnimal var4 = (IsoAnimal)this.animals.get(var3);
            if (var4 == var1) {
               this.animals.set(var3, var2);
               break;
            }
         }

         this.recalcAnimalSize();
      }
   }

   public float getCurrentTotalAnimalSize() {
      return this.totalAnimalSize;
   }

   public void setCurrentTotalAnimalSize(float var1) {
      this.totalAnimalSize = var1;
   }

   public void keyNamerVehicle(InventoryItem var1) {
      keyNamerVehicle(var1, this);
   }

   public static void keyNamerVehicle(InventoryItem var0, BaseVehicle var1) {
      if (!var0.getType().equals("KeyRing") && !var0.hasTag("KeyRing")) {
         String var2 = var1.getScript().getName();
         if (var1.getScript().getCarModelName() != null) {
            var2 = var1.getScript().getCarModelName();
         }

         String var10001 = Translator.getText(var0.getScriptItem().getDisplayName());
         var0.setName(var10001 + " - " + Translator.getText("IGUI_VehicleName" + var2));
      }

   }

   public boolean checkZombieKeyForVehicle(IsoZombie var1) {
      return this.checkZombieKeyForVehicle(var1, this.getScriptName());
   }

   public boolean checkZombieKeyForVehicle(IsoZombie var1, String var2) {
      if (!var2.contains("Burnt") && !var2.equals("Trailer") && !var2.equals("TrailerAdvert")) {
         String var3 = var1.getOutfitName();
         if (var3 == null) {
            return false;
         } else if (this.getZombieType() != null && this.hasZombieType(var3)) {
            return true;
         } else if (var3.contains("Survivalist")) {
            return true;
         } else if (this.getZombieType() != null) {
            return false;
         } else if (this.checkForSpecialMatchOne("Fire", var2, var3)) {
            return this.checkForSpecialMatchTwo("Fire", var2, var3);
         } else if (this.checkForSpecialMatchOne("Police", var2, var3)) {
            return this.checkForSpecialMatchTwo("Police", var2, var3);
         } else if (this.checkForSpecialMatchOne("Spiffo", var2, var3)) {
            return this.checkForSpecialMatchTwo("Spiffo", var2, var3);
         } else if (!var2.contains("Ranger") && (!var2.contains("Lights") || this.getSkinIndex() != 0)) {
            if ((!var2.contains("Lights") || this.getSkinIndex() != 1) && (!var2.contains("VanSpecial") || this.getSkinIndex() != 0) && !var2.contains("Fossoil")) {
               if (var3.contains("Postal")) {
                  return var2.contains("Mail") || var2.contains("VanSpecial") && this.getSkinIndex() == 2;
               } else if (!var2.contains("Mccoy") && (!var2.contains("VanSpecial") || this.getSkinIndex() != 1)) {
                  if (var2.contains("Taxi")) {
                     return var3.contains("Generic");
                  } else if (!var3.contains("Cook") && !var3.contains("Security") && !var3.contains("Waiter")) {
                     if (!var3.contains("Farmer") && !var3.contains("Fisherman") && !var3.contains("Hunter")) {
                        if (var3.contains("Teacher")) {
                           return var2.contains("Normal") || var2.contains("Small") || var2.contains("StationWagon") || var2.contains("SUV");
                        } else if (!var3.contains("Young") && !var3.contains("Student") || var2.contains("Small") && Rand.Next(2) == 0) {
                           if (!var2.contains("Luxury") && !var2.contains("Modern") && !var2.contains("SUV")) {
                              if (var2.contains("Sports")) {
                                 return var3.contains("Classy") || var3.contains("Doctor") || var3.contains("Dress") || var3.contains("Generic") || var3.contains("Golfer") || var3.contains("OfficeWorker") || var3.contains("Trader") || var3.contains("Bandit") || var3.contains("Biker") || var3.contains("Redneck") || var3.contains("Veteran") || var3.contains("Thug") || var3.contains("Foreman");
                              } else if (!var2.contains("Small") && !var2.contains("StationWagon") || !var3.contains("Foreman") && !var3.contains("Classy") && !var3.contains("Doctor") && !var3.contains("Golfer") && !var3.contains("Trader") && !var3.contains("Biker")) {
                                 if (!var2.contains("Pickup") && !var2.contains("Van") || !var3.contains("Classy") && !var3.contains("Doctor") && !var3.contains("Golfer") && !var3.contains("Trader")) {
                                    if (!var3.contains("ConstructionWorker") && !var3.contains("Fossoil")) {
                                       if (var2.contains("OffRoad")) {
                                          return var3.contains("Classy") || var3.contains("Doctor") || var3.contains("Generic") || var3.contains("Golfer") || var3.contains("Foreman") || var3.contains("Trader") || var3.contains("Biker") || var3.contains("Redneck");
                                       } else if (!var3.contains("Redneck") && !var3.contains("Thug") && !var3.contains("Veteran")) {
                                          if (!var3.contains("Biker")) {
                                             return true;
                                          } else {
                                             return var2.contains("Normal") || var2.contains("Pickup") || var2.contains("Offroad");
                                          }
                                       } else {
                                          return var2.contains("Normal") || var2.contains("Pickup") || var2.contains("Offroad") || var2.contains("Small");
                                       }
                                    } else {
                                       return var2.contains("Pickup") || var2.contains("Offroad");
                                    }
                                 } else {
                                    return false;
                                 }
                              } else {
                                 return false;
                              }
                           } else {
                              return var3.contains("Classy") || var3.contains("Doctor") || var3.contains("Dress") || var3.contains("Generic") || var3.contains("Golfer") || var3.contains("OfficeWorker") || var3.contains("Foreman") || var3.contains("Priest") || var3.contains("Thug") || var3.contains("Trader") || var3.contains("FitnessInstructor");
                           }
                        } else {
                           return false;
                        }
                     } else {
                        return var2.contains("Pickup") || var2.contains("OffRoad") || var2.contains("SUV") || var2.equals("Trailer_Horsebox") || var2.equals("Trailer_Livestock");
                     }
                  } else {
                     return var2.contains("Normal") || var2.contains("Small");
                  }
               } else {
                  return var3.contains("Foreman") || var3.contains("Mccoy");
               }
            } else {
               return var3.contains("ConstructionWorker") || var3.contains("Fossoil") || var3.contains("Foreman") || var3.contains("Mechanic") || var3.contains("MetalWorker");
            }
         } else {
            return var3.contains("Ranger");
         }
      } else {
         return false;
      }
   }

   public boolean checkForSpecialMatchOne(String var1, String var2, String var3) {
      return var2.contains(var1) || var3.contains(var1);
   }

   public boolean checkForSpecialMatchTwo(String var1, String var2, String var3) {
      return var2.contains(var1) && var3.contains(var1);
   }

   public boolean checkIfGoodVehicleForKey() {
      return !this.getScriptName().contains("Burnt");
   }

   public boolean trySpawnVehicleKeyOnZombie(IsoZombie var1) {
      if (var1.shouldZombieHaveKey(true) && this.checkZombieKeyForVehicle(var1)) {
         InventoryItem var2;
         if (Rand.Next(2) == 0) {
            var2 = this.createVehicleKey();
            this.keySpawned = 1;
            this.keyNamerVehicle(var2);
            var1.addItemToSpawnAtDeath(var2);
            return true;
         } else {
            var2 = InventoryItemFactory.CreateItem("KeyRing");
            InventoryContainer var3 = (InventoryContainer)var2;
            this.keyNamerVehicle(var3);
            InventoryItem var4 = this.createVehicleKey();
            this.keySpawned = 1;
            this.keyNamerVehicle(var4);
            var3.getInventory().AddItem(var4);
            var1.addItemToSpawnAtDeath(var2);
            if ((float)Rand.Next(100) < 1.0F * this.keySpawnChancedD100) {
               float var5 = this.getX();
               float var6 = this.getY();
               Vector2f var7 = new Vector2f();
               BuildingDef var8 = AmbientStreamManager.getNearestBuilding(var5, var6, var7);
               if (var8 != null && var8.getKeyId() != -1) {
                  String var9 = "Base.Key1";
                  InventoryItem var10 = InventoryItemFactory.CreateItem(var9);
                  var10.setKeyId(var8.getKeyId());
                  IsoGridSquare var11 = var8.getFreeSquareInRoom();
                  if (var11 != null) {
                     ItemPickerJava.KeyNamer.nameKey(var10, (IsoGridSquare)Objects.requireNonNull(var8.getFreeSquareInRoom()));
                  }

                  var3.getInventory().AddItem(var10);
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public boolean trySpawnVehicleKeyInObject(IsoObject var1) {
      if (var1.container != null && var1.container.getFirstTagRecurse("CarKey") == null && (var1.container.type.equals("counter") || var1.container.type.equals("officedrawers") || var1.container.type.equals("shelves") || var1.container.type.equals("desk") || var1.container.type.equals("filingcabinet") || var1.container.type.equals("locker") || var1.container.type.equals("metal_shelves") || var1.container.type.equals("tent") || var1.container.type.equals("shelter") || var1.container.type.equals("sidetable") || var1.container.type.equals("plankstash") || var1.container.type.equals("wardrobe") || var1.container.type.equals("dresser"))) {
         this.putKeyToContainer(var1.container, this.square, var1);
         if ((float)Rand.Next(100) < 1.0F * this.keySpawnChancedD100 && this.square.getBuilding() != null && this.square.getBuilding().getDef() != null && this.square.getBuilding().getDef().getKeyId() != -1) {
            this.addBuildingKeyToGloveBox(this.square);
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean checkSquareForVehicleKeySpot(IsoGridSquare var1) {
      boolean var2 = false;
      if (var1 != null) {
         var2 = this.checkSquareForVehicleKeySpotContainer(var1) || this.checkSquareForVehicleKeySpotZombie(var1);
      }

      return var2;
   }

   public boolean checkSquareForVehicleKeySpotContainer(IsoGridSquare var1) {
      boolean var2 = false;
      if (var1 != null && !this.getScriptName().contains("Smashed")) {
         for(int var3 = 0; var3 < var1.getObjects().size(); ++var3) {
            IsoObject var4 = (IsoObject)var1.getObjects().get(var3);
            var2 = this.trySpawnVehicleKeyInObject(var4);
            if (var2) {
               return true;
            }
         }
      }

      return var2;
   }

   public boolean checkSquareForVehicleKeySpotZombie(IsoGridSquare var1) {
      boolean var2 = false;
      if (var1 != null) {
         for(int var3 = 0; var3 < var1.getMovingObjects().size(); ++var3) {
            if (var1.getMovingObjects().get(var3) instanceof IsoZombie && ((IsoZombie)var1.getMovingObjects().get(var3)).shouldZombieHaveKey(true)) {
               IsoZombie var4 = (IsoZombie)var1.getMovingObjects().get(var3);
               var2 = this.trySpawnVehicleKeyOnZombie(var4);
               if (var2) {
                  return true;
               }
            }
         }
      }

      return var2;
   }

   private static float doKeySandboxSettings(int var0) {
      switch (var0) {
         case 1:
            return 0.0F;
         case 2:
            return 0.05F;
         case 3:
            return 0.2F;
         case 4:
            return 0.6F;
         case 5:
            return 1.0F;
         case 6:
            return 2.0F;
         case 7:
            return 2.4F;
         default:
            return 0.6F;
      }
   }

   public void forceVehicleDistribution(String var1) {
      ItemPickerJava.VehicleDistribution var2 = (ItemPickerJava.VehicleDistribution)ItemPickerJava.VehicleDistributions.get(var1);
      ItemPickerJava.ItemPickerRoom var3 = var2.Normal;

      for(int var4 = 0; var4 < this.getPartCount(); ++var4) {
         VehiclePart var5 = this.getPartByIndex(var4);
         if (var5.getItemContainer() != null) {
            if (GameServer.bServer && GameServer.bSoftReset) {
               var5.getItemContainer().setExplored(false);
            }

            if (!var5.getItemContainer().bExplored) {
               var5.getItemContainer().clear();
               this.randomizeContainer(var5, var3);
               var5.getItemContainer().setExplored(true);
            }
         }
      }

   }

   public boolean canLightSmoke(IsoGameCharacter var1) {
      if (var1 == null) {
         return false;
      } else if (!this.hasLighter()) {
         return false;
      } else if (this.getBatteryCharge() <= 0.0F) {
         return false;
      } else {
         return this.getSeat(var1) <= 1;
      }
   }

   private void checkVehicleFailsToStartWithZombiesTargeting() {
      if (!GameServer.bServer) {
         for(int var1 = 0; var1 < this.getMaxPassengers(); ++var1) {
            Passenger var2 = this.getPassenger(var1);
            IsoPlayer var3 = (IsoPlayer)Type.tryCastTo(var2.character, IsoPlayer.class);
            if (var3 != null && var3.isLocalPlayer()) {
               int var4 = var3.getStats().MusicZombiesTargeting_NearbyMoving;
               var4 += var3.getStats().MusicZombiesTargeting_NearbyNotMoving;
               if (var4 > 0) {
                  var3.triggerMusicIntensityEvent("VehicleFailsToStartWithZombiesTargeting");
               }
            }
         }

      }
   }

   private void checkVehicleStartsWithZombiesTargeting() {
      if (!GameServer.bServer) {
         for(int var1 = 0; var1 < this.getMaxPassengers(); ++var1) {
            Passenger var2 = this.getPassenger(var1);
            IsoPlayer var3 = (IsoPlayer)Type.tryCastTo(var2.character, IsoPlayer.class);
            if (var3 != null && var3.isLocalPlayer()) {
               int var4 = var3.getStats().MusicZombiesTargeting_NearbyMoving;
               var4 += var3.getStats().MusicZombiesTargeting_NearbyNotMoving;
               if (var4 > 0) {
                  var3.triggerMusicIntensityEvent("VehicleStartsWithZombiesTargeting");
               }
            }
         }

      }
   }

   public ArrayList<String> getZombieType() {
      return this.script.getZombieType();
   }

   public String getRandomZombieType() {
      return this.script.getRandomZombieType();
   }

   public boolean hasZombieType(String var1) {
      return this.script.hasZombieType(var1);
   }

   public String getFirstZombieType() {
      return this.script.getFirstZombieType();
   }

   public boolean notKillCrops() {
      return this.script.notKillCrops();
   }

   public boolean hasLighter() {
      return this.script.hasLighter();
   }

   public boolean leftSideFuel() {
      VehicleScript.Area var1 = this.getScript().getAreaById("GasTank");
      return var1 != null && !(var1.x < 0.0F);
   }

   public boolean rightSideFuel() {
      VehicleScript.Area var1 = this.getScript().getAreaById("GasTank");
      return var1 != null && !(var1.x > 0.0F);
   }

   public boolean isCreated() {
      return this.bCreated;
   }

   public float getTotalContainerItemWeight() {
      float var1 = 0.0F;

      for(int var2 = 0; var2 < this.parts.size(); ++var2) {
         VehiclePart var3 = (VehiclePart)this.parts.get(var2);
         if (var3.getItemContainer() != null) {
            var1 += var3.getItemContainer().getCapacityWeight();
         }
      }

      return var1;
   }

   public boolean isSirening() {
      return this.hasLightbar() && this.lightbarSirenMode.get() > 0;
   }

   private boolean isDriverGodMode() {
      IsoGameCharacter var1 = this.getDriver();
      return var1 != null && var1.isGodMod();
   }

   public Vector3 getIntersectPoint(Vector3 var1, Vector3 var2) {
      Vector3f var3 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
      float var4 = this.script.getExtents().x * 0.5F + this.script.getCenterOfMassOffset().x();
      float var5 = this.script.getExtents().y * 0.5F + this.script.getCenterOfMassOffset().y();
      float var6 = this.script.getExtents().z * 0.5F + this.script.getCenterOfMassOffset().z();
      Vector3 var7 = new Vector3(var4, var5, var6);
      this.getLocalPos(var1.x(), var1.y(), var1.z(), var3);
      Vector3 var8 = new Vector3(var3.x(), var3.y(), var3.z());
      this.getLocalPos(var2.x(), var2.y(), var2.z(), var3);
      Vector3 var9 = new Vector3(var3.x(), var3.y(), var3.z());
      Vector3 var10 = this.getIntersectPoint(var8, var9, var7);
      if (var10 == null) {
         return null;
      } else {
         this.getWorldPos(var10.x(), var10.y(), var10.z(), var3);
         var10.set(var3.x(), var3.y(), var3.z());
         ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var3);
         return var10;
      }
   }

   private Vector3 getIntersectPoint(Vector3 var1, Vector3 var2, Vector3 var3) {
      Vector3 var4 = new Vector3(var3);
      Vector3 var5 = new Vector3(var3.mul(-1.0F));
      float var6 = 0.0F;
      float var7 = 1.0F;
      Vector3 var8 = var2.sub(var1);

      for(int var9 = 0; var9 < 3; ++var9) {
         float var10 = var1.get(var9);
         float var11 = var5.get(var9);
         float var12 = var4.get(var9);
         float var13 = var8.get(var9);
         if ((double)Math.abs(var13) < 1.0E-6) {
            if (var10 < var11 || var10 > var12) {
               return null;
            }
         } else {
            float var14 = (var11 - var10) / var13;
            float var15 = (var12 - var10) / var13;
            if (var14 > var15) {
               float var16 = var14;
               var14 = var15;
               var15 = var16;
            }

            var6 = Math.max(var6, var14);
            var7 = Math.min(var7, var15);
            if (var6 > var7) {
               return null;
            }
         }
      }

      return var1.add(var8.mul(var6));
   }

   public VehiclePart getNearestVehiclePart(float var1, float var2, float var3, boolean var4) {
      Vector3f var5 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
      Vector3f var6 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
      var5.set(var1, var2, var3);
      VehiclePart var7 = null;
      Object var8 = null;
      float var9 = 3.4028235E38F;

      for(int var10 = 0; var10 < this.script.getAreaCount(); ++var10) {
         VehicleScript.Area var11 = this.script.getArea(var10);
         String var12 = var11.getId();
         VehiclePart var13 = this.getPartById(var12);
         if (var13 != null && (var4 || var13.condition != 0) && this.isInArea(var12, var5)) {
            this.getWorldPos(var11.x, var11.y, var3, var6);
            float var14 = var5.distance(var6);
            if (var14 < var9) {
               var9 = var14;
               var7 = var13;
            }
         }
      }

      ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var6);
      ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var5);
      return var7;
   }

   public boolean isInArea(String var1, Vector3f var2) {
      if (var1 != null && this.getScript() != null) {
         VehicleScript.Area var3 = this.getScript().getAreaById(var1);
         if (var3 == null) {
            return false;
         } else {
            Vector2 var4 = (Vector2)((Vector2ObjectPool)TL_vector2_pool.get()).alloc();
            Vector2 var5 = this.areaPositionLocal(var3, var4);
            if (var5 == null) {
               ((Vector2ObjectPool)TL_vector2_pool.get()).release(var4);
               return false;
            } else {
               Vector3f var6 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
               this.getLocalPos(var2.x, var2.y, this.getZ(), var6);
               float var7 = var5.x - (var3.w + 0.01F) * 0.5F;
               float var8 = var5.y - (var3.h + 0.01F) * 0.5F;
               float var9 = var5.x + (var3.w + 0.01F) * 0.5F;
               float var10 = var5.y + (var3.h + 0.01F) * 0.5F;
               boolean var11 = var6.x >= var7 && var6.x < var9 && var6.z >= var8 && var6.z < var10;
               ((Vector2ObjectPool)TL_vector2_pool.get()).release(var4);
               ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var6);
               return var11;
            }
         }
      } else {
         return false;
      }
   }

   private boolean processRangeHit(IsoGameCharacter var1, HandWeapon var2, float var3) {
      VehiclePart var4 = null;
      float var5 = var2.getMaxRange(var1);
      Vector3 var6 = new Vector3();
      Vector3 var7 = new Vector3();
      BallisticsController var8 = var1.getBallisticsController();
      float var9 = var1.getLookAngleRadians();
      Vector3 var10 = new Vector3();
      var10.set((float)Math.cos((double)var9), (float)Math.sin((double)var9), 0.0F);
      var10.normalize();
      zombie.iso.Vector3 var11 = var8.getMuzzlePosition();
      var6.set(var11.x, var11.y, var11.z);
      var7.set(var6.x() + var10.x() * var5, var6.y() + var10.y() * var5, var6.z() + var10.z() * var5);
      Vector3 var12 = this.getIntersectPoint(var6, var7);
      if (var12 == null) {
         return false;
      } else {
         var4 = this.getPartByDirection(var1.getX(), var1.getY(), var1.getZ());
         if (var4 == null) {
            return false;
         } else {
            this.applyDamageToPart(var1, var2, var4, var3);
            return true;
         }
      }
   }

   private boolean processMeleeHit(IsoGameCharacter var1, HandWeapon var2, float var3) {
      IsoPlayer var4 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      if (var4 != null) {
         var4.setVehicleHitLocation(this);
      }

      VehiclePart var5 = this.getNearestBodyworkPart(var1);
      if (var5 == null) {
         return false;
      } else {
         this.applyDamageToPart(var1, var2, var5, var3);
         return true;
      }
   }

   private void applyDamageToPart(IsoGameCharacter var1, HandWeapon var2, VehiclePart var3, float var4) {
      if (var3 != null) {
         VehicleWindow var5 = var3.getWindow();

         for(int var6 = 0; var6 < var3.getChildCount(); ++var6) {
            VehiclePart var7 = var3.getChild(var6);
            if (var7 != null && var7.getWindow() != null) {
               var5 = var7.getWindow();
               break;
            }
         }

         float var8 = 0.0F;
         if (var3.light != null) {
            var8 = CombatManager.getInstance().calculateDamageToVehicle(var1, var3.getDurability(), var4, var2.getDoorDamage());
            var3.setCondition(var3.getCondition() - (int)var8);
            if (GameServer.bServer) {
               this.transmitPartItem(var3);
               GameServer.PlayWorldSoundServer(var1, "HitVehicleWindowWithWeapon", false, var3.getSquare(), 0.2F, 10.0F, 1.1F, true);
            } else if (!GameClient.bClient) {
               var1.playSound("HitVehicleWindowWithWeapon");
            }
         } else if (var5 != null && var5.isHittable()) {
            var8 = CombatManager.getInstance().calculateDamageToVehicle(var1, var3.getDurability(), var4, var2.getDoorDamage());
            var5.damage((int)var8);
            if (GameServer.bServer) {
               this.transmitPartWindow(var3);
               GameServer.PlayWorldSoundServer(var1, "HitVehicleWindowWithWeapon", false, var3.getSquare(), 0.2F, 10.0F, 1.1F, true);
            } else if (!GameClient.bClient) {
               var1.playSound("HitVehicleWindowWithWeapon");
            }
         } else {
            var8 = CombatManager.getInstance().calculateDamageToVehicle(var1, var3.getDurability(), var4, var2.getDoorDamage());
            var3.setCondition(var3.getCondition() - (int)var8);
            if (GameServer.bServer) {
               this.transmitPartItem(var3);
               GameServer.PlayWorldSoundServer(var1, "HitVehiclePartWithWeapon", false, var3.getSquare(), 0.2F, 10.0F, 1.1F, true);
            } else if (!GameClient.bClient) {
               var1.playSound("HitVehiclePartWithWeapon");
            }
         }

         var3.updateFlags = (short)(var3.updateFlags | 2048);
         this.updateFlags = (short)(this.updateFlags | 2048);
         DebugLog.Combat.debugln("VehiclePart = %s : durability = %f : damage = %f : conditionalDamage = %f", var3.getId(), var3.getDurability(), var4, var8);
      }

   }

   public boolean processHit(IsoGameCharacter var1, HandWeapon var2, float var3) {
      return var2.isRanged() ? this.processRangeHit(var1, var2, var3) : this.processMeleeHit(var1, var2, var3);
   }

   private VehiclePart getPartByDirection(float var1, float var2, float var3) {
      Vector3f var4 = (Vector3f)((Vector3fObjectPool)TL_vector3f_pool.get()).alloc();
      this.getLocalPos(var1, var2, var3, var4);
      var1 = var4.x;
      var3 = var4.z;
      ((Vector3fObjectPool)TL_vector3f_pool.get()).release(var4);
      Vector3f var5 = this.script.getExtents();
      Vector3f var6 = this.script.getCenterOfMassOffset();
      float var7 = var6.x - var5.x * 0.5F;
      float var8 = var6.x + var5.x * 0.5F;
      float var9 = var6.z - var5.z * 0.5F;
      float var10 = var6.z + var5.z * 0.5F;
      if (var1 < var7 * 0.98F) {
         return this.getWeightedRandomSidePart("Right");
      } else if (var1 > var8 * 0.98F) {
         return this.getWeightedRandomSidePart("Left");
      } else if (var3 < var9 * 0.98F) {
         return this.getWeightedRandomRearPart();
      } else {
         return var3 > var10 * 0.98F ? this.getWeightedRandomFrontPart() : this.getAnyRandomPart();
      }
   }

   private void buildVehiclePartList(String var1, float var2, ArrayList<WeightedVehiclePart> var3) {
      WeightedVehiclePart var4 = new WeightedVehiclePart();
      var4.vehiclePart = this.getPartById(var1);
      var4.weight = var2;
      if (var4.vehiclePart != null) {
         if (var4.vehiclePart.condition != 0) {
            var3.add(var4);
         }
      }
   }

   private VehiclePart getAnyRandomPart() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.getPartCount(); ++var2) {
         VehiclePart var3 = this.getPartByIndex(var2);
         if (var3 != null && var3.condition != 0) {
            var1.add(var3);
         }
      }

      if (!var1.isEmpty()) {
         return (VehiclePart)var1.get(Rand.Next(0, var1.size()));
      } else {
         return null;
      }
   }

   private boolean isGasTakeSide(String var1) {
      if (var1.contains("Left") && this.leftSideFuel()) {
         return true;
      } else {
         return var1.contains("Right") && this.rightSideFuel();
      }
   }

   private VehiclePart getWeightedRandomSidePart(String var1) {
      ArrayList var2 = new ArrayList();

      for(int var3 = 0; var3 < this.getPartCount(); ++var3) {
         VehiclePart var4 = this.getPartByIndex(var3);
         if (var4.getId().contains(var1) || this.isGasTakeSide(var1)) {
            this.buildVehiclePartList(var4.getId(), 1.0F, var2);
         }
      }

      if (var1.equals("Right")) {
         this.buildVehiclePartList("WindowRearRight", 19.0F, var2);
         this.buildVehiclePartList("WindowFrontRight", 19.0F, var2);
         this.buildVehiclePartList("TireRearRight", 19.0F, var2);
         this.buildVehiclePartList("TireFrontRight", 19.0F, var2);
         this.buildVehiclePartList("HeadlightRight", 19.0F, var2);
         this.buildVehiclePartList("DoorFrontRight", 19.0F, var2);
         this.buildVehiclePartList("DoorRearRight", 19.0F, var2);
         this.buildVehiclePartList("HeadlightRearRight", 19.0F, var2);
      } else {
         this.buildVehiclePartList("WindowRearLeft", 19.0F, var2);
         this.buildVehiclePartList("WindowFrontLeft", 19.0F, var2);
         this.buildVehiclePartList("TireRearLeft", 19.0F, var2);
         this.buildVehiclePartList("TireFrontLeft", 19.0F, var2);
         this.buildVehiclePartList("HeadlightLeft", 19.0F, var2);
         this.buildVehiclePartList("DoorFrontLeft", 19.0F, var2);
         this.buildVehiclePartList("DoorRearLeft", 19.0F, var2);
         this.buildVehiclePartList("HeadlightRearLeft", 19.0F, var2);
      }

      this.buildVehiclePartList("GasTank", 9.0F, var2);
      this.buildVehiclePartList("lightbar", 20.0F, var2);
      if (!var2.isEmpty()) {
         return this.getWeightedRandomPart(var2);
      } else {
         return null;
      }
   }

   private VehiclePart getWeightedRandomFrontPart() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.getPartCount(); ++var2) {
         VehiclePart var3 = this.getPartByIndex(var2);
         if (var3.getId().contains("Front")) {
            this.buildVehiclePartList(var3.getId(), 1.0F, var1);
         }
      }

      this.buildVehiclePartList("HeadlightRight", 20.0F, var1);
      this.buildVehiclePartList("HeadlightLeft", 20.0F, var1);
      this.buildVehiclePartList("TireFrontRight", 19.0F, var1);
      this.buildVehiclePartList("TireFrontLeft", 19.0F, var1);
      this.buildVehiclePartList("Engine", 5.0F, var1);
      this.buildVehiclePartList("EngineDoor", 10.0F, var1);
      this.buildVehiclePartList("Battery", 5.0F, var1);
      this.buildVehiclePartList("Windshield", 20.0F, var1);
      this.buildVehiclePartList("Heater", 2.0F, var1);
      this.buildVehiclePartList("Hood", 10.0F, var1);
      this.buildVehiclePartList("Radio", 0.5F, var1);
      this.buildVehiclePartList("GloveBox", 0.5F, var1);
      this.buildVehiclePartList("lightbar", 20.0F, var1);
      if (!var1.isEmpty()) {
         return this.getWeightedRandomPart(var1);
      } else {
         return null;
      }
   }

   private VehiclePart getWeightedRandomRearPart() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.getPartCount(); ++var2) {
         VehiclePart var3 = this.getPartByIndex(var2);
         if (var3.getId().contains("Rear")) {
            this.buildVehiclePartList(var3.getId(), 1.0F, var1);
         }
      }

      this.buildVehiclePartList("HeadlightRearRight", 19.0F, var1);
      this.buildVehiclePartList("HeadlightRearLeft", 19.0F, var1);
      this.buildVehiclePartList("TireRearRight", 19.0F, var1);
      this.buildVehiclePartList("TireRearLeft", 19.0F, var1);
      this.buildVehiclePartList("WindshieldRear", 19.0F, var1);
      this.buildVehiclePartList("TruckBed", 5.0F, var1);
      this.buildVehiclePartList("TrunkDoor", 10.0F, var1);
      this.buildVehiclePartList("DoorRear", 9.0F, var1);
      this.buildVehiclePartList("Muffler", 0.5F, var1);
      this.buildVehiclePartList("lightbar", 20.0F, var1);
      if (!var1.isEmpty()) {
         return this.getWeightedRandomPart(var1);
      } else {
         return null;
      }
   }

   private VehiclePart getWeightedRandomPart(ArrayList<WeightedVehiclePart> var1) {
      float var2 = 0.0F;

      WeightedVehiclePart var4;
      for(Iterator var3 = var1.iterator(); var3.hasNext(); var2 += var4.weight) {
         var4 = (WeightedVehiclePart)var3.next();
      }

      float var6 = Rand.Next(0.0F, 1.0F) * var2;
      Iterator var7 = var1.iterator();

      WeightedVehiclePart var5;
      do {
         if (!var7.hasNext()) {
            return null;
         }

         var5 = (WeightedVehiclePart)var7.next();
         var6 -= var5.weight;
      } while(!(var6 <= 0.0F));

      return var5.vehiclePart;
   }

   public boolean canAddAnimalInTrailer(IsoAnimal var1) {
      return this.getAnimalTrailerSize() >= this.getCurrentTotalAnimalSize() + var1.getAnimalTrailerSize();
   }

   public boolean canAddAnimalInTrailer(IsoDeadBody var1) {
      return this.getAnimalTrailerSize() >= this.getCurrentTotalAnimalSize() + ((KahluaTableImpl)var1.getModData()).rawgetFloat("animalTrailerSize");
   }

   protected static class UpdateFlags {
      public static final short Full = 1;
      public static final short PositionOrientation = 2;
      public static final short Engine = 4;
      public static final short Lights = 8;
      public static final short PartModData = 16;
      public static final short PartUsedDelta = 32;
      public static final short PartModels = 64;
      public static final short PartItem = 128;
      public static final short PartWindow = 256;
      public static final short PartDoor = 512;
      public static final short Sounds = 1024;
      public static final short PartCondition = 2048;
      public static final short UpdateCarProperties = 4096;
      public static final short Authorization = 16384;
      public static final short AllPartFlags = 19440;

      protected UpdateFlags() {
      }
   }

   public static final class Matrix4fObjectPool extends ObjectPool<Matrix4f> {
      int allocated = 0;

      Matrix4fObjectPool() {
         super(Matrix4f::new);
      }

      protected Matrix4f makeObject() {
         ++this.allocated;
         return (Matrix4f)super.makeObject();
      }
   }

   public static final class QuaternionfObjectPool extends ObjectPool<Quaternionf> {
      int allocated = 0;

      QuaternionfObjectPool() {
         super(Quaternionf::new);
      }

      protected Quaternionf makeObject() {
         ++this.allocated;
         return (Quaternionf)super.makeObject();
      }
   }

   public static final class TransformPool extends ObjectPool<Transform> {
      int allocated = 0;

      TransformPool() {
         super(Transform::new);
      }

      protected Transform makeObject() {
         ++this.allocated;
         return (Transform)super.makeObject();
      }
   }

   public static final class Vector2ObjectPool extends ObjectPool<Vector2> {
      int allocated = 0;

      Vector2ObjectPool() {
         super(Vector2::new);
      }

      protected Vector2 makeObject() {
         ++this.allocated;
         return (Vector2)super.makeObject();
      }
   }

   public static final class Vector2fObjectPool extends ObjectPool<Vector2f> {
      int allocated = 0;

      Vector2fObjectPool() {
         super(Vector2f::new);
      }

      protected Vector2f makeObject() {
         ++this.allocated;
         return (Vector2f)super.makeObject();
      }
   }

   public static final class Vector3fObjectPool extends ObjectPool<Vector3f> {
      int allocated = 0;

      Vector3fObjectPool() {
         super(Vector3f::new);
      }

      protected Vector3f makeObject() {
         ++this.allocated;
         return (Vector3f)super.makeObject();
      }
   }

   public static final class Vector4fObjectPool extends ObjectPool<Vector4f> {
      int allocated = 0;

      Vector4fObjectPool() {
         super(Vector4f::new);
      }

      protected Vector4f makeObject() {
         ++this.allocated;
         return (Vector4f)super.makeObject();
      }
   }

   private static final class VehicleImpulse {
      static final ArrayDeque<VehicleImpulse> pool = new ArrayDeque();
      final Vector3f impulse = new Vector3f();
      final Vector3f rel_pos = new Vector3f();
      boolean enable = false;
      boolean applied = false;

      private VehicleImpulse() {
      }

      static VehicleImpulse alloc() {
         return pool.isEmpty() ? new VehicleImpulse() : (VehicleImpulse)pool.pop();
      }

      void release() {
         pool.push(this);
      }
   }

   public static enum Authorization {
      Server,
      LocalCollide,
      RemoteCollide,
      Local,
      Remote;

      private static final HashMap<Byte, Authorization> authorizations = new HashMap();

      private Authorization() {
      }

      public static Authorization valueOf(byte var0) {
         return (Authorization)authorizations.getOrDefault(var0, Server);
      }

      public byte index() {
         return (byte)this.ordinal();
      }

      static {
         Arrays.stream(values()).forEach((var0) -> {
            authorizations.put(var0.index(), var0);
         });
      }
   }

   public static enum engineStateTypes {
      Idle,
      Starting,
      RetryingStarting,
      StartingSuccess,
      StartingFailed,
      Running,
      Stalling,
      ShutingDown,
      StartingFailedNoPower;

      public static final engineStateTypes[] Values = values();

      private engineStateTypes() {
      }
   }

   public static final class WheelInfo {
      public float steering;
      public float rotation;
      public float skidInfo;
      public float suspensionLength;

      public WheelInfo() {
      }
   }

   public static final class ServerVehicleState {
      public float x = -1.0F;
      public float y;
      public float z;
      public Quaternionf orient = new Quaternionf();
      public short flags;
      public Authorization netPlayerAuthorization;
      public short netPlayerId;

      public ServerVehicleState() {
         this.netPlayerAuthorization = BaseVehicle.Authorization.Server;
         this.netPlayerId = 0;
         this.flags = 0;
      }

      public void setAuthorization(BaseVehicle var1) {
         this.netPlayerAuthorization = var1.netPlayerAuthorization;
         this.netPlayerId = var1.netPlayerId;
      }

      public boolean shouldSend(BaseVehicle var1) {
         if (var1.getController() == null) {
            return false;
         } else if (var1.updateLockTimeout > System.currentTimeMillis()) {
            return false;
         } else {
            this.flags = (short)(this.flags & 1);
            if (!var1.isNetPlayerAuthorization(this.netPlayerAuthorization) || !var1.isNetPlayerId(this.netPlayerId)) {
               this.flags = (short)(this.flags | 16384);
            }

            this.flags |= var1.updateFlags;
            return this.flags != 0;
         }
      }
   }

   public static final class Passenger {
      public IsoGameCharacter character;
      final Vector3f offset = new Vector3f();

      public Passenger() {
      }
   }

   public static class HitVars {
      private static final float speedCap = 10.0F;
      private final Vector3f velocity = new Vector3f();
      private final Vector2 collision = new Vector2();
      private float dot;
      protected float vehicleImpulse;
      protected float vehicleSpeed;
      public final Vector3f targetImpulse = new Vector3f();
      public boolean isVehicleHitFromFront;
      public boolean isTargetHitFromBehind;
      public int vehicleDamage;
      public float hitSpeed;

      public HitVars() {
      }

      public void calc(IsoGameCharacter var1, BaseVehicle var2) {
         var2.getLinearVelocity(this.velocity);
         this.velocity.y = 0.0F;
         if (var1 instanceof IsoZombie) {
            this.vehicleSpeed = Math.min(this.velocity.length(), 10.0F);
            this.hitSpeed = this.vehicleSpeed + var2.getClientForce() / var2.getFudgedMass();
         } else {
            this.vehicleSpeed = (float)Math.sqrt((double)(this.velocity.x * this.velocity.x + this.velocity.z * this.velocity.z));
            if (var1.isOnFloor()) {
               this.hitSpeed = Math.max(this.vehicleSpeed * 6.0F, 5.0F);
            } else {
               this.hitSpeed = Math.max(this.vehicleSpeed * 2.0F, 5.0F);
            }
         }

         this.targetImpulse.set(var2.getX() - var1.getX(), 0.0F, var2.getY() - var1.getY());
         this.targetImpulse.normalize();
         this.velocity.normalize();
         this.dot = this.velocity.dot(this.targetImpulse);
         this.targetImpulse.normalize();
         this.targetImpulse.mul(3.0F * this.vehicleSpeed / 10.0F);
         this.targetImpulse.set(this.targetImpulse.x, this.targetImpulse.y, this.targetImpulse.z);
         this.vehicleImpulse = var2.getFudgedMass() * 7.0F * this.vehicleSpeed / 10.0F * Math.abs(this.dot);
         this.isTargetHitFromBehind = "BEHIND".equals(var1.testDotSide(var2));
      }
   }

   public static final class ModelInfo {
      public VehiclePart part;
      public VehicleScript.Model scriptModel;
      public ModelScript modelScript;
      public int wheelIndex;
      public final Matrix4f renderTransform = new Matrix4f();
      public VehicleSubModelInstance modelInstance;
      public AnimationPlayer m_animPlayer;
      public AnimationTrack m_track;

      public ModelInfo() {
      }

      public AnimationPlayer getAnimationPlayer() {
         if (this.part != null && this.part.getParent() != null) {
            ModelInfo var1 = this.part.getVehicle().getModelInfoForPart(this.part.getParent());
            if (var1 != null) {
               return var1.getAnimationPlayer();
            }
         }

         String var3 = this.scriptModel.file;
         Model var2 = ModelManager.instance.getLoadedModel(var3);
         if (var2 != null && !var2.bStatic) {
            if (this.m_animPlayer != null && this.m_animPlayer.getModel() != var2) {
               this.m_animPlayer = (AnimationPlayer)Pool.tryRelease((IPooledObject)this.m_animPlayer);
            }

            if (this.m_animPlayer == null) {
               this.m_animPlayer = AnimationPlayer.alloc(var2);
            }

            return this.m_animPlayer;
         } else {
            return null;
         }
      }

      public void releaseAnimationPlayer() {
         this.m_animPlayer = (AnimationPlayer)Pool.tryRelease((IPooledObject)this.m_animPlayer);
      }
   }

   private static final class L_testCollisionWithVehicle {
      static final Vector2[] testVecs1 = new Vector2[4];
      static final Vector2[] testVecs2 = new Vector2[4];
      static final Vector3f worldPos = new Vector3f();

      private L_testCollisionWithVehicle() {
      }
   }

   public static final class MinMaxPosition {
      public float minX;
      public float maxX;
      public float minY;
      public float maxY;

      public MinMaxPosition() {
      }
   }

   private class WeightedVehiclePart {
      public VehiclePart vehiclePart = null;
      public float weight = 1.0F;

      private WeightedVehiclePart() {
      }
   }
}
