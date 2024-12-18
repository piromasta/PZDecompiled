package zombie.iso;

import fmod.fmod.FMODSoundEmitter;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.krka.kahlua.vm.KahluaTable;
import zombie.CollisionManager;
import zombie.DebugFileWatcher;
import zombie.FliesSound;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.MapCollisionData;
import zombie.MovingObjectUpdateScheduler;
import zombie.PersistentOutfits;
import zombie.PredicatedFileWatcher;
import zombie.ReanimatedPlayers;
import zombie.SandboxOptions;
import zombie.SharedDescriptors;
import zombie.SoundManager;
import zombie.SystemDisabler;
import zombie.VirtualZombieManager;
import zombie.WorldSoundManager;
import zombie.ZomboidFileSystem;
import zombie.ZomboidGlobals;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.ai.ZombieGroupManager;
import zombie.ai.states.FakeDeadZombieState;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.DummySoundEmitter;
import zombie.audio.ObjectAmbientEmitters;
import zombie.audio.parameters.ParameterInside;
import zombie.basements.Basements;
import zombie.characters.AnimalVocalsManager;
import zombie.characters.HaloTextHelper;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.characters.TriggerSetAnimationRecorderFile;
import zombie.characters.ZombieVocalsManager;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.AnimalPopulationManager;
import zombie.characters.animals.AnimalTracksDefinitions;
import zombie.characters.animals.AnimalZones;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.professions.ProfessionFactory;
import zombie.characters.traits.TraitFactory;
import zombie.core.Core;
import zombie.core.ImportantAreaManager;
import zombie.core.PZForkJoinPool;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.TilePropertyAliasMap;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.physics.WorldSimulation;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.core.skinnedmodel.model.WorldItemAtlas;
import zombie.core.stash.StashSystem;
import zombie.core.textures.Texture;
import zombie.core.utils.OnceEvery;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.entity.GameEntityManager;
import zombie.entity.components.spriteconfig.SpriteConfigManager;
import zombie.erosion.ErosionGlobals;
import zombie.gameStates.GameLoadingState;
import zombie.globalObjects.GlobalObjectLookup;
import zombie.input.Mouse;
import zombie.inventory.ItemConfigurator;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.MapItem;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.DesignationZone;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.SafeHouse;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.fboRenderChunk.FBORenderAreaHighlights;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.ObjectRenderEffects;
import zombie.iso.objects.RainManager;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.sprite.SkyBox;
import zombie.iso.sprite.SpriteGridParseData;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.WorldFlares;
import zombie.iso.weather.fog.ImprovedFog;
import zombie.iso.weather.fx.IsoWeatherFX;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.iso.worldgen.WGChunk;
import zombie.iso.worldgen.WGParams;
import zombie.iso.worldgen.attachments.AttachmentsHandler;
import zombie.iso.worldgen.blending.Blending;
import zombie.iso.worldgen.maps.BiomeMap;
import zombie.iso.worldgen.rules.Rules;
import zombie.iso.worldgen.zones.ZoneGenerator;
import zombie.iso.zones.Zone;
import zombie.network.BodyDamageSync;
import zombie.network.ClientServerMap;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.NetChecksum;
import zombie.network.PassengerMap;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.network.id.ObjectIDManager;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.extra.BorderFinderRenderer;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.pathfind.nativeCode.PathfindNativeRenderer;
import zombie.popman.ZombiePopulationManager;
import zombie.popman.animal.HutchManager;
import zombie.radio.ZomboidRadio;
import zombie.randomizedWorld.RandomizedWorldBase;
import zombie.randomizedWorld.randomizedBuilding.RBBar;
import zombie.randomizedWorld.randomizedBuilding.RBBarn;
import zombie.randomizedWorld.randomizedBuilding.RBBasic;
import zombie.randomizedWorld.randomizedBuilding.RBBurnt;
import zombie.randomizedWorld.randomizedBuilding.RBBurntCorpse;
import zombie.randomizedWorld.randomizedBuilding.RBBurntFireman;
import zombie.randomizedWorld.randomizedBuilding.RBCafe;
import zombie.randomizedWorld.randomizedBuilding.RBClinic;
import zombie.randomizedWorld.randomizedBuilding.RBDorm;
import zombie.randomizedWorld.randomizedBuilding.RBGunstoreSiege;
import zombie.randomizedWorld.randomizedBuilding.RBHairSalon;
import zombie.randomizedWorld.randomizedBuilding.RBHeatBreakAfternoon;
import zombie.randomizedWorld.randomizedBuilding.RBJackieJaye;
import zombie.randomizedWorld.randomizedBuilding.RBJoanHartford;
import zombie.randomizedWorld.randomizedBuilding.RBKateAndBaldspot;
import zombie.randomizedWorld.randomizedBuilding.RBLooted;
import zombie.randomizedWorld.randomizedBuilding.RBMayorWestPoint;
import zombie.randomizedWorld.randomizedBuilding.RBNolans;
import zombie.randomizedWorld.randomizedBuilding.RBOffice;
import zombie.randomizedWorld.randomizedBuilding.RBOther;
import zombie.randomizedWorld.randomizedBuilding.RBPileOCrepe;
import zombie.randomizedWorld.randomizedBuilding.RBPizzaWhirled;
import zombie.randomizedWorld.randomizedBuilding.RBPoliceSiege;
import zombie.randomizedWorld.randomizedBuilding.RBSafehouse;
import zombie.randomizedWorld.randomizedBuilding.RBSchool;
import zombie.randomizedWorld.randomizedBuilding.RBShopLooted;
import zombie.randomizedWorld.randomizedBuilding.RBSpiffo;
import zombie.randomizedWorld.randomizedBuilding.RBStripclub;
import zombie.randomizedWorld.randomizedBuilding.RBTrashed;
import zombie.randomizedWorld.randomizedBuilding.RBTwiggy;
import zombie.randomizedWorld.randomizedBuilding.RBWoodcraft;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.randomizedWorld.randomizedVehicleStory.RVSAmbulanceCrash;
import zombie.randomizedWorld.randomizedVehicleStory.RVSAnimalOnRoad;
import zombie.randomizedWorld.randomizedVehicleStory.RVSAnimalTrailerOnRoad;
import zombie.randomizedWorld.randomizedVehicleStory.RVSBanditRoad;
import zombie.randomizedWorld.randomizedVehicleStory.RVSBurntCar;
import zombie.randomizedWorld.randomizedVehicleStory.RVSCarCrash;
import zombie.randomizedWorld.randomizedVehicleStory.RVSCarCrashCorpse;
import zombie.randomizedWorld.randomizedVehicleStory.RVSCarCrashDeer;
import zombie.randomizedWorld.randomizedVehicleStory.RVSChangingTire;
import zombie.randomizedWorld.randomizedVehicleStory.RVSConstructionSite;
import zombie.randomizedWorld.randomizedVehicleStory.RVSCrashHorde;
import zombie.randomizedWorld.randomizedVehicleStory.RVSDeadEnd;
import zombie.randomizedWorld.randomizedVehicleStory.RVSFlippedCrash;
import zombie.randomizedWorld.randomizedVehicleStory.RVSHerdOnRoad;
import zombie.randomizedWorld.randomizedVehicleStory.RVSPlonkies;
import zombie.randomizedWorld.randomizedVehicleStory.RVSPoliceBlockade;
import zombie.randomizedWorld.randomizedVehicleStory.RVSPoliceBlockadeShooting;
import zombie.randomizedWorld.randomizedVehicleStory.RVSRegionalProfessionVehicle;
import zombie.randomizedWorld.randomizedVehicleStory.RVSRichJerk;
import zombie.randomizedWorld.randomizedVehicleStory.RVSRoadKill;
import zombie.randomizedWorld.randomizedVehicleStory.RVSRoadKillSmall;
import zombie.randomizedWorld.randomizedVehicleStory.RVSTrailerCrash;
import zombie.randomizedWorld.randomizedVehicleStory.RVSUtilityVehicle;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedZoneStory.RZJackieJaye;
import zombie.randomizedWorld.randomizedZoneStory.RZSAttachedAnimal;
import zombie.randomizedWorld.randomizedZoneStory.RZSBBQParty;
import zombie.randomizedWorld.randomizedZoneStory.RZSBaseball;
import zombie.randomizedWorld.randomizedZoneStory.RZSBeachParty;
import zombie.randomizedWorld.randomizedZoneStory.RZSBurntWreck;
import zombie.randomizedWorld.randomizedZoneStory.RZSBuryingCamp;
import zombie.randomizedWorld.randomizedZoneStory.RZSCampsite;
import zombie.randomizedWorld.randomizedZoneStory.RZSCharcoalBurner;
import zombie.randomizedWorld.randomizedZoneStory.RZSDean;
import zombie.randomizedWorld.randomizedZoneStory.RZSDuke;
import zombie.randomizedWorld.randomizedZoneStory.RZSEscapedAnimal;
import zombie.randomizedWorld.randomizedZoneStory.RZSEscapedHerd;
import zombie.randomizedWorld.randomizedZoneStory.RZSFishingTrip;
import zombie.randomizedWorld.randomizedZoneStory.RZSForestCamp;
import zombie.randomizedWorld.randomizedZoneStory.RZSForestCampEaten;
import zombie.randomizedWorld.randomizedZoneStory.RZSFrankHemingway;
import zombie.randomizedWorld.randomizedZoneStory.RZSHermitCamp;
import zombie.randomizedWorld.randomizedZoneStory.RZSHillbillyHoedown;
import zombie.randomizedWorld.randomizedZoneStory.RZSHogWild;
import zombie.randomizedWorld.randomizedZoneStory.RZSHunterCamp;
import zombie.randomizedWorld.randomizedZoneStory.RZSKirstyKormick;
import zombie.randomizedWorld.randomizedZoneStory.RZSMurderScene;
import zombie.randomizedWorld.randomizedZoneStory.RZSMusicFest;
import zombie.randomizedWorld.randomizedZoneStory.RZSMusicFestStage;
import zombie.randomizedWorld.randomizedZoneStory.RZSNastyMattress;
import zombie.randomizedWorld.randomizedZoneStory.RZSOccultActivity;
import zombie.randomizedWorld.randomizedZoneStory.RZSOldFirepit;
import zombie.randomizedWorld.randomizedZoneStory.RZSOldShelter;
import zombie.randomizedWorld.randomizedZoneStory.RZSOrphanedFawn;
import zombie.randomizedWorld.randomizedZoneStory.RZSRangerSmith;
import zombie.randomizedWorld.randomizedZoneStory.RZSRockerParty;
import zombie.randomizedWorld.randomizedZoneStory.RZSSadCamp;
import zombie.randomizedWorld.randomizedZoneStory.RZSSexyTime;
import zombie.randomizedWorld.randomizedZoneStory.RZSSirTwiggy;
import zombie.randomizedWorld.randomizedZoneStory.RZSSurvivalistCamp;
import zombie.randomizedWorld.randomizedZoneStory.RZSTragicPicnic;
import zombie.randomizedWorld.randomizedZoneStory.RZSTrapperCamp;
import zombie.randomizedWorld.randomizedZoneStory.RZSVanCamp;
import zombie.randomizedWorld.randomizedZoneStory.RZSWasteDump;
import zombie.randomizedWorld.randomizedZoneStory.RZSWaterPump;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;
import zombie.savefile.ClientPlayerDB;
import zombie.savefile.PlayerDB;
import zombie.savefile.PlayerDBHelper;
import zombie.savefile.ServerPlayerDB;
import zombie.scripting.ScriptManager;
import zombie.spriteModel.SpriteModelManager;
import zombie.text.templating.TemplateText;
import zombie.tileDepth.TileDepthTextureManager;
import zombie.ui.TutorialManager;
import zombie.util.AddCoopPlayer;
import zombie.util.SharedStrings;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleIDMap;
import zombie.vehicles.VehicleManager;
import zombie.vehicles.VehiclesDB2;
import zombie.viewCone.ViewConeTextureFBO;
import zombie.world.WorldDictionary;
import zombie.world.WorldDictionaryException;
import zombie.world.moddata.GlobalModData;
import zombie.worldMap.network.WorldMapClient;

public final class IsoWorld {
   private String weather = "sunny";
   public final IsoMetaGrid MetaGrid = new IsoMetaGrid();
   private final ArrayList<RandomizedBuildingBase> randomizedBuildingList = new ArrayList();
   private final ArrayList<RandomizedZoneStoryBase> randomizedZoneList = new ArrayList();
   private final ArrayList<RandomizedVehicleStoryBase> randomizedVehicleStoryList = new ArrayList();
   private final RandomizedBuildingBase RBBasic = new RBBasic();
   private final RandomizedWorldBase RandomizedWorldBase = new RandomizedWorldBase();
   private final HashMap<String, ArrayList<UUID>> spawnedZombieZone = new HashMap();
   private final HashMap<String, ArrayList<String>> allTiles = new HashMap();
   private final ArrayList<String> tileImages = new ArrayList();
   private float flashIsoCursorA = 1.0F;
   private boolean flashIsoCursorInc = false;
   public SkyBox sky = null;
   private static PredicatedFileWatcher m_setAnimationRecordingTriggerWatcher;
   private static boolean m_animationRecorderActive = false;
   private static boolean m_animationRecorderDiscard = false;
   private int timeSinceLastSurvivorInHorde = 4000;
   private int m_frameNo = 0;
   public final Helicopter helicopter = new Helicopter();
   private boolean bHydroPowerOn = false;
   public final ArrayList<IsoGameCharacter> Characters = new ArrayList();
   private final ArrayDeque<BaseSoundEmitter> freeEmitters = new ArrayDeque();
   private final ArrayList<BaseSoundEmitter> currentEmitters = new ArrayList();
   private final HashMap<BaseSoundEmitter, IsoObject> emitterOwners = new HashMap();
   public int x = 50;
   public int y = 50;
   public IsoCell CurrentCell;
   public static IsoWorld instance = new IsoWorld();
   public int TotalSurvivorsDead = 0;
   public int TotalSurvivorNights = 0;
   public int SurvivorSurvivalRecord = 0;
   public HashMap<Integer, SurvivorDesc> SurvivorDescriptors = new HashMap();
   public ArrayList<AddCoopPlayer> AddCoopPlayers = new ArrayList();
   private static final CompScoreToPlayer compScoreToPlayer = new CompScoreToPlayer();
   public static String mapPath = "media/";
   public static boolean mapUseJar = true;
   boolean bLoaded = false;
   public static final HashMap<String, ArrayList<String>> PropertyValueMap = new HashMap();
   private static int WorldX = 0;
   private static int WorldY = 0;
   private SurvivorDesc luaDesc;
   private ArrayList<String> luatraits;
   private int luaPosX = -1;
   private int luaPosY = -1;
   private int luaPosZ = -1;
   private String spawnRegionName = "";
   public static final int WorldVersion = 219;
   public static final int WorldVersion_PreviouslyMoved = 196;
   public static final int WorldVersion_DesignationZone = 197;
   public static final int WorldVersion_PlayerExtraInfoFlags = 198;
   public static final int WorldVersion_ObjectID = 199;
   public static final int WorldVersion_CraftUpdateFoundations = 200;
   public static final int WorldVersion_AlarmDecay = 201;
   public static final int WorldVersion_FishingCheat = 202;
   public static final int WorldVersion_CharacterVoiceType = 203;
   public static final int WorldVersion_AnimalHutch = 204;
   public static final int WorldVersion_AlarmClock = 205;
   public static final int WorldVersion_VariableHeight = 206;
   public static final int WorldVersion_EnableWorldgen = 207;
   public static final int WorldVersion_CharacterVoiceOptions = 208;
   public static final int WorldVersion_ChunksWorldGeneratedBoolean = 209;
   public static final int WorldVersion_ChunksWorldModifiedBoolean = 210;
   public static final int WorldVersion_CharacterDiscomfort = 211;
   public static final int WorldVersion_HutchAndVehicleAnimalFormat = 212;
   public static final int WorldVersion_IsoCompostHealthValues = 213;
   public static final int WorldVersion_ChunksAttachmentsState = 214;
   public static final int WorldVersion_ZoneIDisUUID = 215;
   public static final int WorldVersion_SafeHouseHitPoints = 216;
   public static final int WorldVersion_FastMoveCheat = 217;
   public static final int WorldVersion_SquareSeen = 218;
   public static final int WorldVersion_TrapExplosionDuration = 219;
   public static int SavedWorldVersion = -1;
   private boolean bDrawWorld = true;
   private final ArrayList<IsoZombie> zombieWithModel = new ArrayList();
   private final ArrayList<IsoZombie> zombieWithoutModel = new ArrayList();
   private final ArrayList<IsoAnimal> animalWithModel = new ArrayList();
   private final ArrayList<IsoAnimal> animalWithoutModel = new ArrayList();
   Vector2 coneTempo1 = new Vector2();
   Vector2 coneTempo2 = new Vector2();
   Vector2 coneTempo3 = new Vector2();
   static float d;
   public static boolean NoZombies = false;
   public static int TotalWorldVersion = -1;
   public static int saveoffsetx;
   public static int saveoffsety;
   public boolean bDoChunkMapUpdate = true;
   private long emitterUpdateMS;
   public boolean emitterUpdate;
   private int updateSafehousePlayers = 200;
   public static CompletableFuture<Void> animationThread;
   private Rules rules;
   private WGChunk wgChunk;
   private Blending blending;
   private AttachmentsHandler attachmentsHandler;
   private ZoneGenerator zoneGenerator;
   private BiomeMap biomeMap;

   public IsoMetaGrid getMetaGrid() {
      return this.MetaGrid;
   }

   public Zone registerZone(String var1, String var2, int var3, int var4, int var5, int var6, int var7) {
      return this.MetaGrid.registerZone(var1, var2, var3, var4, var5, var6, var7);
   }

   /** @deprecated */
   @Deprecated
   public Zone registerZoneNoOverlap(String var1, String var2, int var3, int var4, int var5, int var6, int var7) {
      return this.registerZone(var1, var2, var3, var4, var5, var6, var7);
   }

   public void removeZonesForLotDirectory(String var1) {
      this.MetaGrid.removeZonesForLotDirectory(var1);
   }

   public BaseSoundEmitter getFreeEmitter() {
      Object var1 = null;
      if (this.freeEmitters.isEmpty()) {
         var1 = Core.SoundDisabled ? new DummySoundEmitter() : new FMODSoundEmitter();
      } else {
         var1 = (BaseSoundEmitter)this.freeEmitters.pop();
      }

      this.currentEmitters.add(var1);
      return (BaseSoundEmitter)var1;
   }

   public BaseSoundEmitter getFreeEmitter(float var1, float var2, float var3) {
      BaseSoundEmitter var4 = this.getFreeEmitter();
      var4.setPos(var1, var2, var3);
      return var4;
   }

   public void takeOwnershipOfEmitter(BaseSoundEmitter var1) {
      this.currentEmitters.remove(var1);
   }

   public void setEmitterOwner(BaseSoundEmitter var1, IsoObject var2) {
      if (var1 != null && var2 != null) {
         if (!this.emitterOwners.containsKey(var1)) {
            this.emitterOwners.put(var1, var2);
         }
      }
   }

   public void returnOwnershipOfEmitter(BaseSoundEmitter var1) {
      if (var1 != null) {
         if (!this.currentEmitters.contains(var1) && !this.freeEmitters.contains(var1)) {
            if (var1.isEmpty()) {
               FMODSoundEmitter var2 = (FMODSoundEmitter)Type.tryCastTo(var1, FMODSoundEmitter.class);
               if (var2 != null) {
                  var2.clearParameters();
               }

               this.freeEmitters.add(var1);
            } else {
               this.currentEmitters.add(var1);
            }

         }
      }
   }

   public Zone registerVehiclesZone(String var1, String var2, int var3, int var4, int var5, int var6, int var7, KahluaTable var8) {
      return this.MetaGrid.registerVehiclesZone(var1, var2, var3, var4, var5, var6, var7, var8);
   }

   public Zone registerMannequinZone(String var1, String var2, int var3, int var4, int var5, int var6, int var7, KahluaTable var8) {
      return this.MetaGrid.registerMannequinZone(var1, var2, var3, var4, var5, var6, var7, var8);
   }

   public void registerRoomTone(String var1, String var2, int var3, int var4, int var5, int var6, int var7, KahluaTable var8) {
      this.MetaGrid.registerRoomTone(var1, var2, var3, var4, var5, var6, var7, var8);
   }

   public void registerSpawnOrigin(int var1, int var2, int var3, int var4, KahluaTable var5) {
      ZombiePopulationManager.instance.registerSpawnOrigin(var1, var2, var3, var4, var5);
   }

   public void registerWaterFlow(float var1, float var2, float var3, float var4) {
      IsoWaterFlow.addFlow(var1, var2, var3, var4);
   }

   public void registerWaterZone(float var1, float var2, float var3, float var4, float var5, float var6) {
      IsoWaterFlow.addZone(var1, var2, var3, var4, var5, var6);
   }

   public void checkVehiclesZones() {
      this.MetaGrid.checkVehiclesZones();
   }

   public void setGameMode(String var1) {
      Core.GameMode = var1;
      Core.bLastStand = "LastStand".equals(var1);
      Core.getInstance().setChallenge(false);
      Core.ChallengeID = null;
   }

   public String getGameMode() {
      return Core.GameMode;
   }

   public void setPreset(String var1) {
      Core.Preset = var1;
   }

   public String getPreset() {
      return Core.Preset;
   }

   public void setWorld(String var1) {
      Core.GameSaveWorld = var1.trim();
   }

   public void setMap(String var1) {
      Core.GameMap = var1;
   }

   public String getMap() {
      return Core.GameMap;
   }

   public void renderTerrain() {
   }

   public int getFrameNo() {
      return this.m_frameNo;
   }

   public IsoWorld() {
   }

   private static void initMessaging() {
      if (m_setAnimationRecordingTriggerWatcher == null) {
         m_setAnimationRecordingTriggerWatcher = new PredicatedFileWatcher(ZomboidFileSystem.instance.getMessagingDirSub("Trigger_AnimationRecorder.xml"), TriggerSetAnimationRecorderFile.class, IsoWorld::onTrigger_setAnimationRecorderTriggerFile);
         DebugFileWatcher.instance.add(m_setAnimationRecordingTriggerWatcher);
      }

   }

   private static void onTrigger_setAnimationRecorderTriggerFile(TriggerSetAnimationRecorderFile var0) {
      m_animationRecorderActive = var0.isRecording;
      m_animationRecorderDiscard = var0.discard;
   }

   public static boolean isAnimRecorderActive() {
      return m_animationRecorderActive;
   }

   public static boolean isAnimRecorderDiscardTriggered() {
      return m_animationRecorderDiscard;
   }

   public IsoSurvivor CreateRandomSurvivor(SurvivorDesc var1, IsoGridSquare var2, IsoPlayer var3) {
      return null;
   }

   public void CreateSwarm(int var1, int var2, int var3, int var4, int var5) {
   }

   public void ForceKillAllZombies() {
      GameTime.getInstance().RemoveZombiesIndiscriminate(1000);
   }

   public static int readInt(RandomAccessFile var0) throws EOFException, IOException {
      int var1 = var0.read();
      int var2 = var0.read();
      int var3 = var0.read();
      int var4 = var0.read();
      if ((var1 | var2 | var3 | var4) < 0) {
         throw new EOFException();
      } else {
         return (var1 << 0) + (var2 << 8) + (var3 << 16) + (var4 << 24);
      }
   }

   public static String readString(RandomAccessFile var0) throws EOFException, IOException {
      String var1 = var0.readLine();
      return var1;
   }

   public static int readInt(InputStream var0) throws EOFException, IOException {
      int var1 = var0.read();
      int var2 = var0.read();
      int var3 = var0.read();
      int var4 = var0.read();
      if ((var1 | var2 | var3 | var4) < 0) {
         throw new EOFException();
      } else {
         return (var1 << 0) + (var2 << 8) + (var3 << 16) + (var4 << 24);
      }
   }

   public static String readString(InputStream var0) throws IOException {
      StringBuilder var1 = new StringBuilder();
      int var2 = -1;
      boolean var3 = false;

      while(!var3) {
         switch (var2 = var0.read()) {
            case -1:
            case 10:
               var3 = true;
               break;
            case 13:
               throw new IllegalStateException("\r\n unsupported");
            default:
               var1.append((char)var2);
         }
      }

      if (var2 == -1 && var1.length() == 0) {
         return null;
      } else {
         return var1.toString();
      }
   }

   public void LoadTileDefinitions(IsoSpriteManager var1, String var2, int var3) {
      DebugLog.DetailedInfo.trace("tiledef: loading " + var2);
      boolean var4 = var2.endsWith(".patch.tiles");

      try {
         FileInputStream var5 = new FileInputStream(var2);

         try {
            BufferedInputStream var6 = new BufferedInputStream(var5);

            try {
               int var7 = readInt((InputStream)var6);
               int var8 = readInt((InputStream)var6);
               int var9 = readInt((InputStream)var6);
               SharedStrings var10 = new SharedStrings();
               boolean var11 = false;
               boolean var12 = false;
               boolean var13 = Core.bDebug && Translator.getLanguage() == Translator.getDefaultLanguage();
               ArrayList var14 = new ArrayList();
               HashMap var15 = new HashMap();
               HashMap var16 = new HashMap();
               String[] var17 = new String[]{"N", "E", "S", "W"};

               for(int var18 = 0; var18 < var17.length; ++var18) {
                  var16.put(var17[var18], new ArrayList());
               }

               SpriteGridParseData var58 = new SpriteGridParseData();
               HashMap var19 = new HashMap();
               int var20 = 0;
               int var21 = 0;
               int var22 = 0;
               int var23 = 0;
               HashSet var24 = new HashSet();
               int var25 = 0;

               label755:
               while(true) {
                  String var27;
                  String var28;
                  if (var25 >= var9) {
                     String var10001;
                     ArrayList var59;
                     if (var12) {
                        var59 = new ArrayList(var24);
                        Collections.sort(var59);
                        Iterator var60 = var59.iterator();

                        while(var60.hasNext()) {
                           var27 = (String)var60.next();
                           PrintStream var97 = System.out;
                           var10001 = var27.replaceAll(" ", "_").replaceAll("-", "_").replaceAll("'", "").replaceAll("\\.", "");
                           var97.println(var10001 + " = \"" + var27 + "\",");
                        }
                     }

                     if (var13) {
                        var59 = new ArrayList(var24);
                        Collections.sort(var59);
                        StringBuilder var61 = new StringBuilder();
                        Iterator var62 = var59.iterator();

                        while(var62.hasNext()) {
                           var28 = (String)var62.next();
                           if (Translator.getMoveableDisplayNameOrNull(var28) == null) {
                              var10001 = var28.replaceAll(" ", "_").replaceAll("-", "_").replaceAll("'", "").replaceAll("\\.", "");
                              var61.append(var10001 + " = \"" + var28 + "\",\n");
                           }
                        }

                        var27 = var61.toString();
                        if (!var27.isEmpty() && Core.bDebug) {
                           DebugLog.Translation.debugln("Missing translations in Moveables_EN.txt:\n" + var27);
                        }
                     }

                     if (var11) {
                        try {
                           this.saveMovableStats(var19, var3, var21, var22, var23, var20);
                        } catch (Exception var54) {
                        }
                     }
                     break;
                  }

                  String var26 = readString((InputStream)var6);
                  var27 = var26.trim();
                  var28 = readString((InputStream)var6);
                  int var29 = readInt((InputStream)var6);
                  int var30 = readInt((InputStream)var6);
                  int var31 = readInt((InputStream)var6);
                  int var32 = readInt((InputStream)var6);

                  IsoSprite var34;
                  int var40;
                  for(int var33 = 0; var33 < var32; ++var33) {
                     if (var4) {
                        var34 = (IsoSprite)var1.NamedMap.get(var27 + "_" + var33);
                        if (var34 == null) {
                           continue;
                        }
                     } else if (var3 < 2) {
                        var34 = var1.AddSprite(var27 + "_" + var33, var3 * 100 * 1000 + 10000 + var31 * 1000 + var33);
                     } else {
                        var34 = var1.AddSprite(var27 + "_" + var33, var3 * 512 * 512 + var31 * 512 + var33);
                     }

                     if (Core.bDebug) {
                        if (this.allTiles.containsKey(var27)) {
                           if (!var4) {
                              ((ArrayList)this.allTiles.get(var27)).add(var27 + "_" + var33);
                           }
                        } else {
                           ArrayList var35 = new ArrayList();
                           var35.add(var27 + "_" + var33);
                           this.allTiles.put(var27, var35);
                        }
                     }

                     var14.add(var34);
                     if (!var4) {
                        var34.setName(var27 + "_" + var33);
                        var34.tilesetName = var27;
                        var34.tileSheetIndex = var33;
                     }

                     if (var34.name.contains("damaged") || var34.name.contains("trash_")) {
                        var34.attachedFloor = true;
                        var34.getProperties().Set("attachedFloor", "true");
                     }

                     if (var34.name.startsWith("f_bushes") && var33 <= 31) {
                        var34.isBush = true;
                        var34.attachedFloor = true;
                     }

                     int var64 = readInt((InputStream)var6);

                     for(int var36 = 0; var36 < var64; ++var36) {
                        var26 = readString((InputStream)var6);
                        String var37 = var26.trim();
                        var26 = readString((InputStream)var6);
                        String var38 = var26.trim();
                        IsoObjectType var39 = IsoObjectType.FromString(var37);
                        if (var39 == IsoObjectType.MAX) {
                           var37 = var10.get(var37);
                           if (var37.equals("firerequirement")) {
                              var34.firerequirement = Integer.parseInt(var38);
                           } else if (var37.equals("fireRequirement")) {
                              var34.firerequirement = Integer.parseInt(var38);
                           } else if (var37.equals("BurntTile")) {
                              var34.burntTile = var38;
                           } else if (var37.equals("ForceAmbient")) {
                              var34.forceAmbient = true;
                              var34.getProperties().Set(var37, var38);
                           } else if (var37.equals("solidfloor")) {
                              var34.solidfloor = true;
                              var34.getProperties().Set(var37, var38);
                           } else if (var37.equals("canBeRemoved")) {
                              var34.canBeRemoved = true;
                              var34.getProperties().Set(var37, var38);
                           } else if (var37.equals("attachedFloor")) {
                              var34.attachedFloor = true;
                              var34.getProperties().Set(var37, var38);
                           } else if (var37.equals("cutW")) {
                              var34.cutW = true;
                              var34.getProperties().Set(var37, var38);
                           } else if (var37.equals("cutN")) {
                              var34.cutN = true;
                              var34.getProperties().Set(var37, var38);
                           } else if (var37.equals("solid")) {
                              var34.solid = true;
                              var34.getProperties().Set(var37, var38);
                           } else if (var37.equals("solidtrans")) {
                              var34.solidTrans = true;
                              var34.getProperties().Set(var37, var38);
                           } else if (var37.equals("invisible")) {
                              var34.invisible = true;
                              var34.getProperties().Set(var37, var38);
                           } else if (var37.equals("alwaysDraw")) {
                              var34.alwaysDraw = true;
                              var34.getProperties().Set(var37, var38);
                           } else if (var37.equals("forceRender")) {
                              var34.forceRender = true;
                              var34.getProperties().Set(var37, var38);
                           } else if ("FloorHeight".equals(var37)) {
                              if ("OneThird".equals(var38)) {
                                 var34.getProperties().Set(IsoFlagType.FloorHeightOneThird);
                              } else if ("TwoThirds".equals(var38)) {
                                 var34.getProperties().Set(IsoFlagType.FloorHeightTwoThirds);
                              }
                           } else if (var37.equals("MoveWithWind")) {
                              var34.moveWithWind = true;
                              var34.getProperties().Set(var37, var38);
                           } else if (var37.equals("WindType")) {
                              var34.windType = Integer.parseInt(var38);
                              var34.getProperties().Set(var37, var38);
                           } else if (var37.equals("RenderLayer")) {
                              var34.getProperties().Set(var37, var38);
                              if ("Default".equals(var38)) {
                                 var34.renderLayer = 0;
                              } else if ("Floor".equals(var38)) {
                                 var34.renderLayer = 1;
                              }
                           } else if (var37.equals("TreatAsWallOrder")) {
                              var34.treatAsWallOrder = true;
                              var34.getProperties().Set(var37, var38);
                           } else {
                              var34.getProperties().Set(var37, var38);
                              if ("WindowN".equals(var37) || "WindowW".equals(var37)) {
                                 var34.getProperties().Set(var37, var38, false);
                              }
                           }
                        } else {
                           if (var34.getType() != IsoObjectType.doorW && var34.getType() != IsoObjectType.doorN || var39 != IsoObjectType.wall) {
                              var34.setType(var39);
                           }

                           if (var39 == IsoObjectType.doorW) {
                              var34.getProperties().Set(IsoFlagType.doorW);
                           } else if (var39 == IsoObjectType.doorN) {
                              var34.getProperties().Set(IsoFlagType.doorN);
                           }
                        }

                        if (var39 == IsoObjectType.tree) {
                           if (var34.name.equals("e_riverbirch_1_1")) {
                              var38 = "1";
                           }

                           var34.getProperties().Set("tree", var38);
                           var34.getProperties().UnSet(IsoFlagType.solid);
                           var34.getProperties().Set(IsoFlagType.blocksight);
                           var40 = Integer.parseInt(var38);
                           if (var27.startsWith("vegetation_trees")) {
                              var40 = 4;
                           }

                           if (var40 < 1) {
                              var40 = 1;
                           }

                           if (var40 > 4) {
                              var40 = 4;
                           }

                           if (var40 == 1 || var40 == 2) {
                              var34.getProperties().UnSet(IsoFlagType.blocksight);
                           }
                        }

                        if (var37.equals("interior") && var38.equals("false")) {
                           var34.getProperties().Set(IsoFlagType.exterior);
                        }

                        if (var37.equals("HoppableN")) {
                           var34.getProperties().Set(IsoFlagType.collideN);
                           var34.getProperties().Set(IsoFlagType.canPathN);
                           var34.getProperties().Set(IsoFlagType.transparentN);
                        }

                        if (var37.equals("HoppableW")) {
                           var34.getProperties().Set(IsoFlagType.collideW);
                           var34.getProperties().Set(IsoFlagType.canPathW);
                           var34.getProperties().Set(IsoFlagType.transparentW);
                        }

                        if (var37.equals("WallN")) {
                           var34.getProperties().Set(IsoFlagType.collideN);
                           var34.getProperties().Set(IsoFlagType.cutN);
                           var34.setType(IsoObjectType.wall);
                           var34.cutN = true;
                           var34.getProperties().Set("WallN", "", false);
                        }

                        if (var37.equals("CantClimb")) {
                           var34.getProperties().Set(IsoFlagType.CantClimb);
                        } else if (var37.equals("container")) {
                           var34.getProperties().Set(var37, var38, false);
                        } else if (var37.equals("WallNTrans")) {
                           var34.getProperties().Set(IsoFlagType.collideN);
                           var34.getProperties().Set(IsoFlagType.cutN);
                           var34.getProperties().Set(IsoFlagType.transparentN);
                           var34.setType(IsoObjectType.wall);
                           var34.cutN = true;
                           var34.getProperties().Set("WallNTrans", "", false);
                        } else if (var37.equals("WallW")) {
                           var34.getProperties().Set(IsoFlagType.collideW);
                           var34.getProperties().Set(IsoFlagType.cutW);
                           var34.setType(IsoObjectType.wall);
                           var34.cutW = true;
                           var34.getProperties().Set("WallW", "", false);
                        } else if (var37.equals("windowN")) {
                           var34.getProperties().Set("WindowN", "WindowN");
                           var34.getProperties().Set(IsoFlagType.transparentN);
                           var34.getProperties().Set("WindowN", "WindowN", false);
                        } else if (var37.equals("windowW")) {
                           var34.getProperties().Set("WindowW", "WindowW");
                           var34.getProperties().Set(IsoFlagType.transparentW);
                           var34.getProperties().Set("WindowW", "WindowW", false);
                        } else if (var37.equals("cutW")) {
                           var34.getProperties().Set(IsoFlagType.cutW);
                           var34.cutW = true;
                        } else if (var37.equals("cutN")) {
                           var34.getProperties().Set(IsoFlagType.cutN);
                           var34.cutN = true;
                        } else if (var37.equals("WallWTrans")) {
                           var34.getProperties().Set(IsoFlagType.collideW);
                           var34.getProperties().Set(IsoFlagType.transparentW);
                           var34.getProperties().Set(IsoFlagType.cutW);
                           var34.setType(IsoObjectType.wall);
                           var34.cutW = true;
                           var34.getProperties().Set("WallWTrans", "", false);
                        } else if (var37.equals("DoorWallN")) {
                           var34.getProperties().Set(IsoFlagType.cutN);
                           var34.cutN = true;
                           var34.getProperties().Set("DoorWallN", "", false);
                        } else if (var37.equals("DoorWallNTrans")) {
                           var34.getProperties().Set(IsoFlagType.cutN);
                           var34.getProperties().Set(IsoFlagType.transparentN);
                           var34.cutN = true;
                           var34.getProperties().Set("DoorWallNTrans", "", false);
                        } else if (var37.equals("DoorWallW")) {
                           var34.getProperties().Set(IsoFlagType.cutW);
                           var34.cutW = true;
                           var34.getProperties().Set("DoorWallW", "", false);
                        } else if (var37.equals("DoorWallWTrans")) {
                           var34.getProperties().Set(IsoFlagType.cutW);
                           var34.getProperties().Set(IsoFlagType.transparentW);
                           var34.cutW = true;
                           var34.getProperties().Set("DoorWallWTrans", "", false);
                        } else if (var37.equals("WallNW")) {
                           var34.getProperties().Set(IsoFlagType.collideN);
                           var34.getProperties().Set(IsoFlagType.cutN);
                           var34.getProperties().Set(IsoFlagType.collideW);
                           var34.getProperties().Set(IsoFlagType.cutW);
                           var34.setType(IsoObjectType.wall);
                           var34.cutW = true;
                           var34.cutN = true;
                           var34.getProperties().Set("WallNW", "", false);
                        } else if (var37.equals("WallNWTrans")) {
                           var34.getProperties().Set(IsoFlagType.collideN);
                           var34.getProperties().Set(IsoFlagType.cutN);
                           var34.getProperties().Set(IsoFlagType.collideW);
                           var34.getProperties().Set(IsoFlagType.transparentN);
                           var34.getProperties().Set(IsoFlagType.transparentW);
                           var34.getProperties().Set(IsoFlagType.cutW);
                           var34.setType(IsoObjectType.wall);
                           var34.cutW = true;
                           var34.cutN = true;
                           var34.getProperties().Set("WallNWTrans", "", false);
                        } else if (var37.equals("WallSE")) {
                           var34.getProperties().Set(IsoFlagType.cutW);
                           var34.getProperties().Set(IsoFlagType.WallSE);
                           var34.getProperties().Set("WallSE", "WallSE");
                           var34.cutW = true;
                        } else if (var37.equals("WindowW")) {
                           var34.getProperties().Set(IsoFlagType.canPathW);
                           var34.getProperties().Set(IsoFlagType.collideW);
                           var34.getProperties().Set(IsoFlagType.cutW);
                           var34.getProperties().Set(IsoFlagType.transparentW);
                           var34.setType(IsoObjectType.windowFW);
                           if (var34.getProperties().Is(IsoFlagType.HoppableW)) {
                              if (Core.bDebug) {
                                 DebugLog.Moveable.println("ERROR: WindowW sprite shouldn't have HoppableW (" + var34.getName() + ")");
                              }

                              var34.getProperties().UnSet(IsoFlagType.HoppableW);
                           }

                           var34.cutW = true;
                        } else if (var37.equals("WindowN")) {
                           var34.getProperties().Set(IsoFlagType.canPathN);
                           var34.getProperties().Set(IsoFlagType.collideN);
                           var34.getProperties().Set(IsoFlagType.cutN);
                           var34.getProperties().Set(IsoFlagType.transparentN);
                           var34.setType(IsoObjectType.windowFN);
                           if (var34.getProperties().Is(IsoFlagType.HoppableN)) {
                              if (Core.bDebug) {
                                 DebugLog.Moveable.println("ERROR: WindowN sprite shouldn't have HoppableN (" + var34.getName() + ")");
                              }

                              var34.getProperties().UnSet(IsoFlagType.HoppableN);
                           }

                           var34.cutN = true;
                        } else if (var37.equals("UnbreakableWindowW")) {
                           var34.getProperties().Set(IsoFlagType.canPathW);
                           var34.getProperties().Set(IsoFlagType.collideW);
                           var34.getProperties().Set(IsoFlagType.cutW);
                           var34.getProperties().Set(IsoFlagType.transparentW);
                           var34.getProperties().Set(IsoFlagType.collideW);
                           var34.setType(IsoObjectType.wall);
                           var34.cutW = true;
                        } else if (var37.equals("UnbreakableWindowN")) {
                           var34.getProperties().Set(IsoFlagType.canPathN);
                           var34.getProperties().Set(IsoFlagType.collideN);
                           var34.getProperties().Set(IsoFlagType.cutN);
                           var34.getProperties().Set(IsoFlagType.transparentN);
                           var34.getProperties().Set(IsoFlagType.collideN);
                           var34.setType(IsoObjectType.wall);
                           var34.cutN = true;
                        } else if (var37.equals("UnbreakableWindowNW")) {
                           var34.getProperties().Set(IsoFlagType.cutN);
                           var34.getProperties().Set(IsoFlagType.transparentN);
                           var34.getProperties().Set(IsoFlagType.collideN);
                           var34.getProperties().Set(IsoFlagType.cutN);
                           var34.getProperties().Set(IsoFlagType.collideW);
                           var34.getProperties().Set(IsoFlagType.cutW);
                           var34.setType(IsoObjectType.wall);
                           var34.cutW = true;
                           var34.cutN = true;
                        } else if ("NoWallLighting".equals(var37)) {
                           var34.getProperties().Set(IsoFlagType.NoWallLighting);
                        } else if ("ForceAmbient".equals(var37)) {
                           var34.getProperties().Set(IsoFlagType.ForceAmbient);
                        }

                        if (var37.equals("name")) {
                           var34.setParentObjectName(var38);
                        }
                     }

                     if (var34.getProperties().Is("lightR") || var34.getProperties().Is("lightG") || var34.getProperties().Is("lightB")) {
                        if (!var34.getProperties().Is("lightR")) {
                           var34.getProperties().Set("lightR", "0");
                        }

                        if (!var34.getProperties().Is("lightG")) {
                           var34.getProperties().Set("lightG", "0");
                        }

                        if (!var34.getProperties().Is("lightB")) {
                           var34.getProperties().Set("lightB", "0");
                        }
                     }

                     var34.getProperties().CreateKeySet();
                     if (Core.bDebug && var34.getProperties().Is("SmashedTileOffset") && !var34.getProperties().Is("GlassRemovedOffset")) {
                        DebugLog.Sprite.error("Window sprite has SmashedTileOffset but no GlassRemovedOffset (" + var34.getName() + ")");
                     }
                  }

                  this.setOpenDoorProperties(var27, var14);
                  var15.clear();
                  Iterator var63 = var14.iterator();

                  while(true) {
                     while(true) {
                        String var66;
                        while(var63.hasNext()) {
                           var34 = (IsoSprite)var63.next();
                           if (var34.getProperties().Is("StopCar")) {
                              var34.setType(IsoObjectType.isMoveAbleObject);
                           }

                           String var10000;
                           if (var34.getProperties().Is("IsMoveAble")) {
                              if (var34.getProperties().Is("CustomName") && !var34.getProperties().Val("CustomName").equals("")) {
                                 ++var20;
                                 if (var34.getProperties().Is("GroupName")) {
                                    var10000 = var34.getProperties().Val("GroupName");
                                    var66 = var10000 + " " + var34.getProperties().Val("CustomName");
                                    if (!var15.containsKey(var66)) {
                                       var15.put(var66, new ArrayList());
                                    }

                                    ((ArrayList)var15.get(var66)).add(var34);
                                    var24.add(var66);
                                 } else {
                                    if (!var19.containsKey(var27)) {
                                       var19.put(var27, new ArrayList());
                                    }

                                    if (!((ArrayList)var19.get(var27)).contains(var34.getProperties().Val("CustomName"))) {
                                       ((ArrayList)var19.get(var27)).add(var34.getProperties().Val("CustomName"));
                                    }

                                    ++var21;
                                    var24.add(var34.getProperties().Val("CustomName"));
                                 }
                              } else {
                                 DebugLog.Moveable.println("[IMPORTANT] MOVABLES: Object has no custom name defined: sheet = " + var27);
                              }
                           } else if (var34.getProperties().Is("SpriteGridPos")) {
                              if (StringUtils.isNullOrWhitespace(var34.getProperties().Val("CustomName"))) {
                                 DebugLog.Moveable.println("[IMPORTANT] MOVABLES: Object has no custom name defined: sheet = " + var27);
                              } else if (var34.getProperties().Is("GroupName")) {
                                 var10000 = var34.getProperties().Val("GroupName");
                                 var66 = var10000 + " " + var34.getProperties().Val("CustomName");
                                 if (!var15.containsKey(var66)) {
                                    var15.put(var66, new ArrayList());
                                 }

                                 ((ArrayList)var15.get(var66)).add(var34);
                              }
                           }
                        }

                        var63 = var15.entrySet().iterator();

                        while(true) {
                           while(true) {
                              while(true) {
                                 String var42;
                                 int var43;
                                 ArrayList var67;
                                 boolean var69;
                                 int var70;
                                 Iterator var72;
                                 boolean var73;
                                 IsoSprite var78;
                                 do {
                                    if (!var63.hasNext()) {
                                       var14.clear();
                                       ++var25;
                                       continue label755;
                                    }

                                    Map.Entry var65 = (Map.Entry)var63.next();
                                    var66 = (String)var65.getKey();
                                    if (!var19.containsKey(var27)) {
                                       var19.put(var27, new ArrayList());
                                    }

                                    if (!((ArrayList)var19.get(var27)).contains(var66)) {
                                       ((ArrayList)var19.get(var27)).add(var66);
                                    }

                                    var67 = (ArrayList)var65.getValue();
                                    if (var67.size() == 1) {
                                       DebugLog.Moveable.println("MOVABLES: Object has only one face defined for group: (" + var66 + ") sheet = " + var27);
                                    }

                                    if (var67.size() == 3) {
                                       DebugLog.Moveable.println("MOVABLES: Object only has 3 sprites, _might_ have a error in settings, group: (" + var66 + ") sheet = " + var27);
                                    }

                                    String[] var68 = var17;
                                    int var71 = var17.length;

                                    for(var70 = 0; var70 < var71; ++var70) {
                                       String var76 = var68[var70];
                                       ((ArrayList)var16.get(var76)).clear();
                                    }

                                    var69 = ((IsoSprite)var67.get(0)).getProperties().Is("SpriteGridPos") && !((IsoSprite)var67.get(0)).getProperties().Val("SpriteGridPos").equals("None");
                                    var73 = true;
                                    var72 = var67.iterator();

                                    while(var72.hasNext()) {
                                       var78 = (IsoSprite)var72.next();
                                       boolean var41 = var78.getProperties().Is("SpriteGridPos") && !var78.getProperties().Val("SpriteGridPos").equals("None");
                                       if (var69 != var41) {
                                          var73 = false;
                                          DebugLog.Moveable.println("MOVABLES: Difference in SpriteGrid settings for members of group: (" + var66 + ") sheet = " + var27);
                                          break;
                                       }

                                       if (!var78.getProperties().Is("Facing")) {
                                          var73 = false;
                                       } else {
                                          switch (var78.getProperties().Val("Facing")) {
                                             case "N":
                                                ((ArrayList)var16.get("N")).add(var78);
                                                break;
                                             case "E":
                                                ((ArrayList)var16.get("E")).add(var78);
                                                break;
                                             case "S":
                                                ((ArrayList)var16.get("S")).add(var78);
                                                break;
                                             case "W":
                                                ((ArrayList)var16.get("W")).add(var78);
                                                break;
                                             default:
                                                DebugLog.Moveable.println("MOVABLES: Invalid face (" + var78.getProperties().Val("Facing") + ") for group: (" + var66 + ") sheet = " + var27);
                                                var73 = false;
                                          }
                                       }

                                       if (!var73) {
                                          DebugLog.Moveable.println("MOVABLES: Not all members have a valid face defined for group: (" + var66 + ") sheet = " + var27);
                                          break;
                                       }
                                    }
                                 } while(!var73);

                                 int var74;
                                 if (!var69) {
                                    if (var67.size() > 4) {
                                       DebugLog.Moveable.println("MOVABLES: Object has too many faces defined for group: (" + var66 + ") sheet = " + var27);
                                    } else {
                                       String[] var75 = var17;
                                       var40 = var17.length;

                                       for(var74 = 0; var74 < var40; ++var74) {
                                          var42 = var75[var74];
                                          if (((ArrayList)var16.get(var42)).size() > 1) {
                                             DebugLog.Moveable.println("MOVABLES: " + var42 + " face defined more than once for group: (" + var66 + ") sheet = " + var27);
                                             var73 = false;
                                          }
                                       }

                                       if (var73) {
                                          ++var22;
                                          var72 = var67.iterator();

                                          while(var72.hasNext()) {
                                             var78 = (IsoSprite)var72.next();
                                             String[] var81 = var17;
                                             int var82 = var17.length;

                                             for(var43 = 0; var43 < var82; ++var43) {
                                                String var86 = var81[var43];
                                                ArrayList var90 = (ArrayList)var16.get(var86);
                                                if (var90.size() > 0 && var90.get(0) != var78) {
                                                   var78.getProperties().Set(var86 + "offset", Integer.toString(var14.indexOf(var90.get(0)) - var14.indexOf(var78)));
                                                }
                                             }
                                          }
                                       }
                                    }
                                 } else {
                                    var70 = 0;
                                    IsoSpriteGrid[] var80 = new IsoSpriteGrid[var17.length];

                                    int var47;
                                    IsoSprite var94;
                                    for(var74 = 0; var74 < var17.length; ++var74) {
                                       ArrayList var77 = (ArrayList)var16.get(var17[var74]);
                                       if (!var77.isEmpty()) {
                                          if (var70 == 0) {
                                             var70 = var77.size();
                                          }

                                          if (var70 != var77.size()) {
                                             DebugLog.Moveable.println("MOVABLES: Sprite count mismatch for multi sprite movable, group: (" + var66 + ") sheet = " + var27);
                                             var73 = false;
                                             break;
                                          }

                                          var58.clear();

                                          SpriteGridParseData.Level var50;
                                          Iterator var85;
                                          for(var85 = var77.iterator(); var85.hasNext(); var58.height = PZMath.max(var58.height, var50.height)) {
                                             IsoSprite var44 = (IsoSprite)var85.next();
                                             String var45 = var44.getProperties().Val("SpriteGridPos");
                                             String[] var46 = var45.split(",");
                                             if (var46.length < 2 || var46.length > 3) {
                                                DebugLog.Moveable.println("MOVABLES: SpriteGrid position error for multi sprite movable, group: (" + var66 + ") sheet = " + var27);
                                                var73 = false;
                                                break;
                                             }

                                             var47 = Integer.parseInt(var46[0]);
                                             int var48 = Integer.parseInt(var46[1]);
                                             int var49 = 0;
                                             if (var46.length == 3) {
                                                var49 = Integer.parseInt(var46[2]);
                                             }

                                             if (var44.getProperties().Is("SpriteGridLevel")) {
                                                var49 = Integer.parseInt(var44.getProperties().Val("SpriteGridLevel"));
                                                if (var49 < 0) {
                                                   DebugLog.Moveable.println("MOVABLES: invalid SpriteGirdLevel for multi sprite movable, group: (" + var66 + ") sheet = " + var27);
                                                   var73 = false;
                                                   break;
                                                }
                                             }

                                             var50 = var58.getOrCreateLevel(var49);
                                             if (var50.xyToSprite.containsKey(var45)) {
                                                DebugLog.Moveable.println("MOVABLES: double SpriteGrid position (" + var45 + ") for multi sprite movable, group: (" + var66 + ") sheet = " + var27);
                                                var73 = false;
                                                break;
                                             }

                                             var50.xyToSprite.put(var45, var44);
                                             var50.width = PZMath.max(var50.width, var47 + 1);
                                             var50.height = PZMath.max(var50.height, var48 + 1);
                                             var58.width = PZMath.max(var58.width, var50.width);
                                          }

                                          if (!var73) {
                                             break;
                                          }

                                          if (!var58.isValid()) {
                                             DebugLog.Moveable.println("MOVABLES: SpriteGrid dimensions error for multi sprite movable, group: (" + var66 + ") sheet = " + var27);
                                             var73 = false;
                                             break;
                                          }

                                          var80[var74] = new IsoSpriteGrid(var58.width, var58.height, var58.levels.size());
                                          var85 = var58.levels.iterator();

                                          while(var85.hasNext()) {
                                             SpriteGridParseData.Level var83 = (SpriteGridParseData.Level)var85.next();
                                             Iterator var87 = var83.xyToSprite.entrySet().iterator();

                                             while(var87.hasNext()) {
                                                Map.Entry var91 = (Map.Entry)var87.next();
                                                String var93 = (String)var91.getKey();
                                                var94 = (IsoSprite)var91.getValue();
                                                String[] var95 = var93.split(",");
                                                int var96 = Integer.parseInt(var95[0]);
                                                int var51 = Integer.parseInt(var95[1]);
                                                var80[var74].setSprite(var96, var51, var83.z, var94);
                                             }
                                          }

                                          if (!var80[var74].validate()) {
                                             DebugLog.Moveable.println("MOVABLES: SpriteGrid didn't validate for multi sprite movable, group: (" + var66 + ") sheet = " + var27);
                                             var73 = false;
                                             break;
                                          }
                                       }
                                    }

                                    if (var73 && var70 != 0) {
                                       ++var23;

                                       for(var74 = 0; var74 < var17.length; ++var74) {
                                          IsoSpriteGrid var79 = var80[var74];
                                          if (var79 != null) {
                                             IsoSprite[] var88 = var79.getSprites();
                                             int var84 = var88.length;

                                             for(int var89 = 0; var89 < var84; ++var89) {
                                                IsoSprite var92 = var88[var89];
                                                if (var92 != null) {
                                                   var92.setSpriteGrid(var79);

                                                   for(var47 = 0; var47 < var17.length; ++var47) {
                                                      if (var47 != var74 && var80[var47] != null) {
                                                         var94 = var80[var47].getAnchorSprite();
                                                         var92.getProperties().Set(var17[var47] + "offset", Integer.toString(var94.tileSheetIndex - var92.tileSheetIndex));
                                                      }
                                                   }
                                                }
                                             }
                                          }
                                       }
                                    } else {
                                       DebugLog.Moveable.println("MOVABLES: Error in multi sprite movable, group: (" + var66 + ") sheet = " + var27);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            } catch (Throwable var55) {
               try {
                  var6.close();
               } catch (Throwable var53) {
                  var55.addSuppressed(var53);
               }

               throw var55;
            }

            var6.close();
         } catch (Throwable var56) {
            try {
               var5.close();
            } catch (Throwable var52) {
               var56.addSuppressed(var52);
            }

            throw var56;
         }

         var5.close();
      } catch (Exception var57) {
         ExceptionLogger.logException(var57);
      }

   }

   private void GenerateTilePropertyLookupTables() {
      TilePropertyAliasMap.instance.Generate(PropertyValueMap);
      PropertyValueMap.clear();
   }

   public void LoadTileDefinitionsPropertyStrings(IsoSpriteManager var1, String var2, int var3) {
      DebugLog.DetailedInfo.trace("tiledef: loading " + var2);
      if (!GameServer.bServer) {
         Thread.yield();
         Core.getInstance().DoFrameReady();
      }

      try {
         FileInputStream var4 = new FileInputStream(var2);

         try {
            BufferedInputStream var5 = new BufferedInputStream(var4);

            try {
               int var6 = readInt((InputStream)var5);
               int var7 = readInt((InputStream)var5);
               int var8 = readInt((InputStream)var5);
               SharedStrings var9 = new SharedStrings();

               for(int var10 = 0; var10 < var8; ++var10) {
                  String var11 = readString((InputStream)var5);
                  String var12 = var11.trim();
                  String var13 = readString((InputStream)var5);
                  this.tileImages.add(var13);
                  int var14 = readInt((InputStream)var5);
                  int var15 = readInt((InputStream)var5);
                  int var16 = readInt((InputStream)var5);
                  int var17 = readInt((InputStream)var5);

                  for(int var18 = 0; var18 < var17; ++var18) {
                     int var19 = readInt((InputStream)var5);

                     for(int var20 = 0; var20 < var19; ++var20) {
                        var11 = readString((InputStream)var5);
                        String var21 = var11.trim();
                        var11 = readString((InputStream)var5);
                        String var22 = var11.trim();
                        IsoObjectType var23 = IsoObjectType.FromString(var21);
                        var21 = var9.get(var21);
                        ArrayList var24 = null;
                        if (PropertyValueMap.containsKey(var21)) {
                           var24 = (ArrayList)PropertyValueMap.get(var21);
                        } else {
                           var24 = new ArrayList();
                           PropertyValueMap.put(var21, var24);
                        }

                        if (!var24.contains(var22)) {
                           var24.add(var22);
                        }
                     }
                  }
               }
            } catch (Throwable var27) {
               try {
                  var5.close();
               } catch (Throwable var26) {
                  var27.addSuppressed(var26);
               }

               throw var27;
            }

            var5.close();
         } catch (Throwable var28) {
            try {
               var4.close();
            } catch (Throwable var25) {
               var28.addSuppressed(var25);
            }

            throw var28;
         }

         var4.close();
      } catch (Exception var29) {
         Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, (String)null, var29);
      }

   }

   private void SetCustomPropertyValues() {
      ((ArrayList)PropertyValueMap.get("WindowN")).add("WindowN");
      ((ArrayList)PropertyValueMap.get("WindowW")).add("WindowW");
      ((ArrayList)PropertyValueMap.get("DoorWallN")).add("DoorWallN");
      ((ArrayList)PropertyValueMap.get("DoorWallW")).add("DoorWallW");
      ((ArrayList)PropertyValueMap.get("WallSE")).add("WallSE");
      ArrayList var1 = new ArrayList();

      int var2;
      String var3;
      for(var2 = -96; var2 <= 96; ++var2) {
         var3 = Integer.toString(var2);
         var1.add(var3);
      }

      PropertyValueMap.put("Noffset", var1);
      PropertyValueMap.put("Soffset", var1);
      PropertyValueMap.put("Woffset", var1);
      PropertyValueMap.put("Eoffset", var1);
      ((ArrayList)PropertyValueMap.get("tree")).add("5");
      ((ArrayList)PropertyValueMap.get("tree")).add("6");
      ((ArrayList)PropertyValueMap.get("lightR")).add("0");
      ((ArrayList)PropertyValueMap.get("lightG")).add("0");
      ((ArrayList)PropertyValueMap.get("lightB")).add("0");

      for(var2 = 0; var2 <= 96; ++var2) {
         var3 = String.valueOf(var2);
         ArrayList var4 = (ArrayList)PropertyValueMap.get("ItemHeight");
         if (!var4.contains(var3)) {
            var4.add(var3);
         }

         var4 = (ArrayList)PropertyValueMap.get("Surface");
         if (!var4.contains(var3)) {
            var4.add(var3);
         }
      }

   }

   private void setOpenDoorProperties(String var1, ArrayList<IsoSprite> var2) {
      for(int var3 = 0; var3 < var2.size(); ++var3) {
         IsoSprite var4 = (IsoSprite)var2.get(var3);
         if ((var4.getType() == IsoObjectType.doorN || var4.getType() == IsoObjectType.doorW) && !var4.getProperties().Is(IsoFlagType.open)) {
            String var5 = var4.getProperties().Val("DoubleDoor");
            if (var5 != null) {
               int var6 = PZMath.tryParseInt(var5, -1);
               if (var6 >= 5) {
                  var4.getProperties().Set(IsoFlagType.open);
               }
            } else {
               String var8 = var4.getProperties().Val("GarageDoor");
               if (var8 != null) {
                  int var7 = PZMath.tryParseInt(var8, -1);
                  if (var7 >= 4) {
                     var4.getProperties().Set(IsoFlagType.open);
                  }
               } else {
                  IsoSprite var9 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var1 + "_" + (var4.tileSheetIndex + 2));
                  if (var9 != null) {
                     var9.setType(var4.getType());
                     var9.getProperties().Set(var4.getType() == IsoObjectType.doorN ? IsoFlagType.doorN : IsoFlagType.doorW);
                     var9.getProperties().Set(IsoFlagType.open);
                  }
               }
            }
         }
      }

   }

   private void saveMovableStats(Map<String, ArrayList<String>> var1, int var2, int var3, int var4, int var5, int var6) throws FileNotFoundException, IOException {
      File var7 = new File(ZomboidFileSystem.instance.getCacheDir());
      if (var7.exists() && var7.isDirectory()) {
         String var10002 = ZomboidFileSystem.instance.getCacheDir();
         File var8 = new File(var10002 + File.separator + "movables_stats_" + var2 + ".txt");

         try {
            FileWriter var9 = new FileWriter(var8, false);

            try {
               var9.write("### Movable objects ###" + System.lineSeparator());
               var9.write("Single Face: " + var3 + System.lineSeparator());
               var9.write("Multi Face: " + var4 + System.lineSeparator());
               var9.write("Multi Face & Multi Sprite: " + var5 + System.lineSeparator());
               var9.write("Total objects : " + (var3 + var4 + var5) + System.lineSeparator());
               var9.write(" " + System.lineSeparator());
               var9.write("Total sprites : " + var6 + System.lineSeparator());
               var9.write(" " + System.lineSeparator());
               Iterator var10 = var1.entrySet().iterator();

               while(var10.hasNext()) {
                  Map.Entry var11 = (Map.Entry)var10.next();
                  String var10001 = (String)var11.getKey();
                  var9.write(var10001 + System.lineSeparator());
                  Iterator var12 = ((ArrayList)var11.getValue()).iterator();

                  while(var12.hasNext()) {
                     String var13 = (String)var12.next();
                     var9.write("\t" + var13 + System.lineSeparator());
                  }
               }
            } catch (Throwable var15) {
               try {
                  var9.close();
               } catch (Throwable var14) {
                  var15.addSuppressed(var14);
               }

               throw var15;
            }

            var9.close();
         } catch (Exception var16) {
            var16.printStackTrace();
         }
      }

   }

   private void addJumboTreeTileset(IsoSpriteManager var1, int var2, String var3, int var4, int var5, int var6) {
      byte var7 = 2;

      for(int var8 = 0; var8 < var5; ++var8) {
         for(int var9 = 0; var9 < var7; ++var9) {
            String var10 = "e_" + var3 + "JUMBO_1";
            int var11 = var8 * var7 + var9;
            IsoSprite var12 = var1.AddSprite(var10 + "_" + var11, var2 * 512 * 512 + var4 * 512 + var11);

            assert GameServer.bServer || !var12.hasNoTextures();

            var12.setName(var10 + "_" + var11);
            var12.setType(IsoObjectType.tree);
            var12.getProperties().Set("tree", var9 == 0 ? "5" : "6");
            var12.getProperties().UnSet(IsoFlagType.solid);
            var12.getProperties().Set(IsoFlagType.blocksight);
            var12.getProperties().CreateKeySet();
            var12.moveWithWind = true;
            var12.windType = var6;
         }
      }

   }

   private void JumboTreeDefinitions(IsoSpriteManager var1, int var2) {
      this.addJumboTreeTileset(var1, var2, "americanholly", 1, 2, 3);
      this.addJumboTreeTileset(var1, var2, "americanlinden", 2, 6, 2);
      this.addJumboTreeTileset(var1, var2, "canadianhemlock", 3, 2, 3);
      this.addJumboTreeTileset(var1, var2, "carolinasilverbell", 4, 6, 1);
      this.addJumboTreeTileset(var1, var2, "cockspurhawthorn", 5, 6, 2);
      this.addJumboTreeTileset(var1, var2, "dogwood", 6, 6, 2);
      this.addJumboTreeTileset(var1, var2, "easternredbud", 7, 6, 2);
      this.addJumboTreeTileset(var1, var2, "redmaple", 8, 6, 2);
      this.addJumboTreeTileset(var1, var2, "riverbirch", 9, 6, 1);
      this.addJumboTreeTileset(var1, var2, "virginiapine", 10, 2, 1);
      this.addJumboTreeTileset(var1, var2, "yellowwood", 11, 6, 2);
      byte var3 = 12;
      byte var4 = 0;
      IsoSprite var5 = var1.AddSprite("jumbo_tree_01_" + var4, var2 * 512 * 512 + var3 * 512 + var4);
      var5.setName("jumbo_tree_01_" + var4);
      var5.setType(IsoObjectType.tree);
      var5.getProperties().Set("tree", "4");
      var5.getProperties().UnSet(IsoFlagType.solid);
      var5.getProperties().Set(IsoFlagType.blocksight);
   }

   private void loadedTileDefinitions() {
      CellLoader.glassRemovedWindowSpriteMap.clear();
      CellLoader.smashedWindowSpriteMap.clear();
      Iterator var1 = IsoSpriteManager.instance.NamedMap.values().iterator();

      while(true) {
         while(var1.hasNext()) {
            IsoSprite var2 = (IsoSprite)var1.next();
            PropertyContainer var3 = var2.getProperties();
            if (var3.Is(IsoFlagType.windowW) || var3.Is(IsoFlagType.windowN)) {
               String var4 = var3.Val("GlassRemovedOffset");
               int var5;
               IsoSprite var6;
               if (var4 != null) {
                  var5 = PZMath.tryParseInt(var4, 0);
                  if (var5 != 0) {
                     var6 = IsoSprite.getSprite(IsoSpriteManager.instance, var2, var5);
                     if (var6 != null) {
                        CellLoader.glassRemovedWindowSpriteMap.put(var6, var2);
                     }
                  }
               }

               var4 = var3.Val("SmashedTileOffset");
               if (var4 != null) {
                  var5 = PZMath.tryParseInt(var4, 0);
                  if (var5 != 0) {
                     var6 = IsoSprite.getSprite(IsoSpriteManager.instance, var2, var5);
                     if (var6 != null) {
                        CellLoader.smashedWindowSpriteMap.put(var6, var2);
                     }
                  }
               }
            }

            if (var2.name != null && var2.name.startsWith("fixtures_railings_01")) {
               var2.getProperties().Set(IsoFlagType.NeverCutaway);
            }

            IsoSprite var7 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var2.tilesetName + "_on_" + var2.tileSheetIndex);
            if (var7 != null && !var7.hasNoTextures()) {
               var2.getProperties().Set(IsoFlagType.HasLightOnSprite);
            } else {
               var2.getProperties().UnSet(IsoFlagType.HasLightOnSprite);
            }
         }

         SpriteModelManager.getInstance().loadedTileDefinitions();
         TileDepthTextureManager.getInstance().loadedTileDefinitions();
         return;
      }
   }

   public boolean LoadPlayerForInfo() throws FileNotFoundException, IOException {
      if (GameClient.bClient) {
         return ClientPlayerDB.getInstance().loadNetworkPlayerInfo(1);
      } else {
         File var1 = ZomboidFileSystem.instance.getFileInCurrentSave("map_p.bin");
         if (!var1.exists()) {
            PlayerDB.getInstance().importPlayersFromVehiclesDB();
            return PlayerDB.getInstance().loadLocalPlayerInfo(1);
         } else {
            FileInputStream var2 = new FileInputStream(var1);
            BufferedInputStream var3 = new BufferedInputStream(var2);
            synchronized(SliceY.SliceBufferLock) {
               SliceY.SliceBuffer.clear();
               int var5 = var3.read(SliceY.SliceBuffer.array());
               SliceY.SliceBuffer.limit(var5);
               var3.close();
               byte var6 = SliceY.SliceBuffer.get();
               byte var7 = SliceY.SliceBuffer.get();
               byte var8 = SliceY.SliceBuffer.get();
               byte var9 = SliceY.SliceBuffer.get();
               boolean var10 = true;
               if (var6 == 80 && var7 == 76 && var8 == 89 && var9 == 82) {
                  int var14 = SliceY.SliceBuffer.getInt();
               } else {
                  SliceY.SliceBuffer.rewind();
               }

               String var11 = GameWindow.ReadString(SliceY.SliceBuffer);
               if (GameClient.bClient && !IsoPlayer.isServerPlayerIDValid(var11)) {
                  GameLoadingState.GameLoadingString = Translator.getText("IGUI_MP_ServerPlayerIDMismatch");
                  GameLoadingState.playerWrongIP = true;
                  return false;
               } else {
                  WorldX = SliceY.SliceBuffer.getInt();
                  WorldY = SliceY.SliceBuffer.getInt();
                  IsoChunkMap.WorldXA = SliceY.SliceBuffer.getInt();
                  IsoChunkMap.WorldYA = SliceY.SliceBuffer.getInt();
                  IsoChunkMap.WorldZA = SliceY.SliceBuffer.getInt();
                  IsoChunkMap.WorldXA += IsoCell.CellSizeInSquares * saveoffsetx;
                  IsoChunkMap.WorldYA += IsoCell.CellSizeInSquares * saveoffsety;
                  IsoChunkMap.SWorldX[0] = WorldX;
                  IsoChunkMap.SWorldY[0] = WorldY;
                  int[] var10000 = IsoChunkMap.SWorldX;
                  var10000[0] += IsoCell.CellSizeInChunks * saveoffsetx;
                  var10000 = IsoChunkMap.SWorldY;
                  var10000[0] += IsoCell.CellSizeInChunks * saveoffsety;
                  return true;
               }
            }
         }
      }
   }

   public void init() throws FileNotFoundException, IOException, WorldDictionaryException {
      if (!Core.bTutorial) {
         this.randomizedBuildingList.add(new RBSafehouse());
         this.randomizedBuildingList.add(new RBBurnt());
         this.randomizedBuildingList.add(new RBOther());
         this.randomizedBuildingList.add(new RBLooted());
         this.randomizedBuildingList.add(new RBBurntFireman());
         this.randomizedBuildingList.add(new RBBurntCorpse());
         this.randomizedBuildingList.add(new RBShopLooted());
         this.randomizedBuildingList.add(new RBKateAndBaldspot());
         this.randomizedBuildingList.add(new RBStripclub());
         this.randomizedBuildingList.add(new RBSchool());
         this.randomizedBuildingList.add(new RBSpiffo());
         this.randomizedBuildingList.add(new RBPizzaWhirled());
         this.randomizedBuildingList.add(new RBPileOCrepe());
         this.randomizedBuildingList.add(new RBCafe());
         this.randomizedBuildingList.add(new RBBar());
         this.randomizedBuildingList.add(new RBOffice());
         this.randomizedBuildingList.add(new RBHairSalon());
         this.randomizedBuildingList.add(new RBClinic());
         this.randomizedBuildingList.add(new RBGunstoreSiege());
         this.randomizedBuildingList.add(new RBPoliceSiege());
         this.randomizedBuildingList.add(new RBHeatBreakAfternoon());
         this.randomizedBuildingList.add(new RBTrashed());
         this.randomizedBuildingList.add(new RBBarn());
         this.randomizedBuildingList.add(new RBDorm());
         this.randomizedBuildingList.add(new RBNolans());
         this.randomizedBuildingList.add(new RBJackieJaye());
         this.randomizedBuildingList.add(new RBJoanHartford());
         this.randomizedBuildingList.add(new RBMayorWestPoint());
         this.randomizedBuildingList.add(new RBTwiggy());
         this.randomizedBuildingList.add(new RBWoodcraft());
         this.randomizedVehicleStoryList.add(new RVSUtilityVehicle());
         this.randomizedVehicleStoryList.add(new RVSConstructionSite());
         this.randomizedVehicleStoryList.add(new RVSBurntCar());
         this.randomizedVehicleStoryList.add(new RVSPoliceBlockadeShooting());
         this.randomizedVehicleStoryList.add(new RVSPoliceBlockade());
         this.randomizedVehicleStoryList.add(new RVSCarCrash());
         this.randomizedVehicleStoryList.add(new RVSAmbulanceCrash());
         this.randomizedVehicleStoryList.add(new RVSCarCrashCorpse());
         this.randomizedVehicleStoryList.add(new RVSChangingTire());
         this.randomizedVehicleStoryList.add(new RVSFlippedCrash());
         this.randomizedVehicleStoryList.add(new RVSBanditRoad());
         this.randomizedVehicleStoryList.add(new RVSTrailerCrash());
         this.randomizedVehicleStoryList.add(new RVSCrashHorde());
         this.randomizedVehicleStoryList.add(new RVSCarCrashDeer());
         this.randomizedVehicleStoryList.add(new RVSDeadEnd());
         this.randomizedVehicleStoryList.add(new RVSRegionalProfessionVehicle());
         this.randomizedVehicleStoryList.add(new RVSRoadKill());
         this.randomizedVehicleStoryList.add(new RVSRoadKillSmall());
         this.randomizedVehicleStoryList.add(new RVSAnimalOnRoad());
         this.randomizedVehicleStoryList.add(new RVSHerdOnRoad());
         this.randomizedVehicleStoryList.add(new RVSAnimalTrailerOnRoad());
         this.randomizedVehicleStoryList.add(new RVSRichJerk());
         this.randomizedVehicleStoryList.add(new RVSPlonkies());
         this.randomizedZoneList.add(new RZSForestCamp());
         this.randomizedZoneList.add(new RZSForestCampEaten());
         this.randomizedZoneList.add(new RZSBuryingCamp());
         this.randomizedZoneList.add(new RZSBeachParty());
         this.randomizedZoneList.add(new RZSFishingTrip());
         this.randomizedZoneList.add(new RZSBBQParty());
         this.randomizedZoneList.add(new RZSHunterCamp());
         this.randomizedZoneList.add(new RZSSexyTime());
         this.randomizedZoneList.add(new RZSTrapperCamp());
         this.randomizedZoneList.add(new RZSBaseball());
         this.randomizedZoneList.add(new RZSMusicFestStage());
         this.randomizedZoneList.add(new RZSMusicFest());
         this.randomizedZoneList.add(new RZSBurntWreck());
         this.randomizedZoneList.add(new RZSHermitCamp());
         this.randomizedZoneList.add(new RZSHillbillyHoedown());
         this.randomizedZoneList.add(new RZSHogWild());
         this.randomizedZoneList.add(new RZSRockerParty());
         this.randomizedZoneList.add(new RZSSadCamp());
         this.randomizedZoneList.add(new RZSSurvivalistCamp());
         this.randomizedZoneList.add(new RZSVanCamp());
         this.randomizedZoneList.add(new RZSEscapedAnimal());
         this.randomizedZoneList.add(new RZSEscapedHerd());
         this.randomizedZoneList.add(new RZSAttachedAnimal());
         this.randomizedZoneList.add(new RZSOrphanedFawn());
         this.randomizedZoneList.add(new RZSNastyMattress());
         this.randomizedZoneList.add(new RZSWasteDump());
         this.randomizedZoneList.add(new RZSMurderScene());
         this.randomizedZoneList.add(new RZSTragicPicnic());
         this.randomizedZoneList.add(new RZSRangerSmith());
         this.randomizedZoneList.add(new RZSOccultActivity());
         this.randomizedZoneList.add(new RZSWaterPump());
         this.randomizedZoneList.add(new RZSOldFirepit());
         this.randomizedZoneList.add(new RZSOldShelter());
         this.randomizedZoneList.add(new RZSCampsite());
         this.randomizedZoneList.add(new RZSCharcoalBurner());
         this.randomizedZoneList.add(new RZSDean());
         this.randomizedZoneList.add(new RZSDuke());
         this.randomizedZoneList.add(new RZSFrankHemingway());
         this.randomizedZoneList.add(new RZJackieJaye());
         this.randomizedZoneList.add(new RZSKirstyKormick());
         this.randomizedZoneList.add(new RZSSirTwiggy());
      }

      zombie.randomizedWorld.randomizedBuilding.RBBasic.getUniqueRDSSpawned().clear();
      if (!GameClient.bClient && !GameServer.bServer) {
         BodyDamageSync.instance = null;
      } else {
         BodyDamageSync.instance = new BodyDamageSync();
      }

      if (GameServer.bServer) {
         Core.GameSaveWorld = GameServer.ServerName;
         String var1 = ZomboidFileSystem.instance.getCurrentSaveDir();
         File var2 = new File(var1);
         if (!var2.exists()) {
            GameServer.ResetID = Rand.Next(10000000);
            ServerOptions.instance.putSaveOption("ResetID", String.valueOf(GameServer.ResetID));
         }

         LuaManager.GlobalObject.createWorld(Core.GameSaveWorld);
      }

      SavedWorldVersion = this.readWorldVersion();
      DataInputStream var3;
      int var4;
      File var43;
      FileInputStream var44;
      if (!GameServer.bServer) {
         var43 = ZomboidFileSystem.instance.getFileInCurrentSave("map_ver.bin");

         try {
            var44 = new FileInputStream(var43);

            try {
               var3 = new DataInputStream(var44);

               try {
                  var4 = var3.readInt();
                  String var5 = GameWindow.ReadString(var3);
                  if (!GameClient.bClient) {
                     Core.GameMap = var5;
                  }

                  this.setDifficulty(GameWindow.ReadString(var3));
               } catch (Throwable var40) {
                  try {
                     var3.close();
                  } catch (Throwable var36) {
                     var40.addSuppressed(var36);
                  }

                  throw var40;
               }

               var3.close();
            } catch (Throwable var41) {
               try {
                  var44.close();
               } catch (Throwable var35) {
                  var41.addSuppressed(var35);
               }

               throw var41;
            }

            var44.close();
         } catch (FileNotFoundException var42) {
         }
      }

      if (!GameClient.bClient) {
         var43 = ZomboidFileSystem.instance.getFileInCurrentSave("id_manager_data.bin");

         try {
            var44 = new FileInputStream(var43);

            try {
               var3 = new DataInputStream(var44);

               try {
                  var4 = var3.readInt();
                  ObjectIDManager.getInstance().load(var3, var4);
               } catch (Throwable var32) {
                  try {
                     var3.close();
                  } catch (Throwable var31) {
                     var32.addSuppressed(var31);
                  }

                  throw var32;
               }

               var3.close();
            } catch (Throwable var33) {
               try {
                  var44.close();
               } catch (Throwable var30) {
                  var33.addSuppressed(var30);
               }

               throw var33;
            }

            var44.close();
         } catch (FileNotFoundException var34) {
         }
      }

      int var6;
      int var52;
      if (!GameClient.bClient) {
         var43 = ZomboidFileSystem.instance.getFileInCurrentSave("important_area_data.bin");

         try {
            var44 = new FileInputStream(var43);

            try {
               BufferedInputStream var48 = new BufferedInputStream(var44);

               try {
                  synchronized(SliceY.SliceBufferLock) {
                     SliceY.SliceBuffer.clear();
                     var52 = var48.read(SliceY.SliceBuffer.array());
                     SliceY.SliceBuffer.limit(var52);
                     var48.close();
                     var6 = SliceY.SliceBuffer.getInt();
                     ImportantAreaManager.getInstance().load(SliceY.SliceBuffer, var6);
                  }
               } catch (Throwable var27) {
                  try {
                     var48.close();
                  } catch (Throwable var25) {
                     var27.addSuppressed(var25);
                  }

                  throw var27;
               }

               var48.close();
            } catch (Throwable var28) {
               try {
                  var44.close();
               } catch (Throwable var24) {
                  var28.addSuppressed(var24);
               }

               throw var28;
            }

            var44.close();
         } catch (FileNotFoundException var29) {
         }
      }

      WGParams.instance.load();
      if (!GameServer.bServer || !GameServer.bSoftReset) {
         this.MetaGrid.CreateStep1();
      }

      LuaEventManager.triggerEvent("OnPreDistributionMerge");
      LuaEventManager.triggerEvent("OnDistributionMerge");
      LuaEventManager.triggerEvent("OnPostDistributionMerge");
      VehiclesDB2.instance.init();
      LuaEventManager.triggerEvent("OnInitWorld");
      if (!GameClient.bClient) {
         SandboxOptions.instance.load();
      }

      ItemPickerJava.Parse();
      this.bHydroPowerOn = (float)(GameTime.getInstance().getWorldAgeHours() / 24.0 + (double)((SandboxOptions.instance.TimeSinceApo.getValue() - 1) * 30)) < (float)SandboxOptions.getInstance().getElecShutModifier();
      ZomboidGlobals.toLua();
      ItemPickerJava.InitSandboxLootSettings();
      this.SurvivorDescriptors.clear();
      IsoSpriteManager.instance.Dispose();
      if (GameClient.bClient && ServerOptions.instance.DoLuaChecksum.getValue()) {
         try {
            NetChecksum.comparer.beginCompare();
            GameLoadingState.GameLoadingString = Translator.getText("IGUI_MP_Checksum");
            long var45 = System.currentTimeMillis();
            long var49 = var45;

            while(!GameClient.checksumValid) {
               if (GameWindow.bServerDisconnected) {
                  return;
               }

               if (System.currentTimeMillis() > var45 + 8000L) {
                  DebugLog.Moveable.println("checksum: timed out waiting for the server to respond");
                  GameClient.connection.forceDisconnect("world-timeout-response");
                  GameWindow.bServerDisconnected = true;
                  GameWindow.kickReason = Translator.getText("UI_GameLoad_TimedOut");
                  return;
               }

               if (System.currentTimeMillis() > var49 + 1000L) {
                  DebugLog.Moveable.println("checksum: waited one second");
                  var49 += 1000L;
               }

               NetChecksum.comparer.update();
               if (GameClient.checksumValid) {
                  break;
               }

               Thread.sleep(100L);
            }
         } catch (Exception var39) {
            var39.printStackTrace();
         }
      }

      GameLoadingState.GameLoadingString = Translator.getText("IGUI_MP_LoadTileDef");
      IsoSpriteManager var46 = IsoSpriteManager.instance;
      this.tileImages.clear();
      ZomboidFileSystem var47 = ZomboidFileSystem.instance;
      this.LoadTileDefinitionsPropertyStrings(var46, var47.getMediaPath("tiledefinitions.tiles"), 0);
      this.LoadTileDefinitionsPropertyStrings(var46, var47.getMediaPath("newtiledefinitions.tiles"), 1);
      this.LoadTileDefinitionsPropertyStrings(var46, var47.getMediaPath("tiledefinitions_erosion.tiles"), 2);
      this.LoadTileDefinitionsPropertyStrings(var46, var47.getMediaPath("tiledefinitions_apcom.tiles"), 3);
      this.LoadTileDefinitionsPropertyStrings(var46, var47.getMediaPath("tiledefinitions_overlays.tiles"), 4);
      this.LoadTileDefinitionsPropertyStrings(var46, var47.getMediaPath("tiledefinitions_b42chunkcaching.tiles"), 5);
      this.LoadTileDefinitionsPropertyStrings(var46, var47.getMediaPath("tiledefinitions_noiseworks.patch.tiles"), -1);
      ZomboidFileSystem.instance.loadModTileDefPropertyStrings();
      this.SetCustomPropertyValues();
      this.GenerateTilePropertyLookupTables();
      this.LoadTileDefinitions(var46, var47.getMediaPath("tiledefinitions.tiles"), 0);
      this.LoadTileDefinitions(var46, var47.getMediaPath("newtiledefinitions.tiles"), 1);
      this.LoadTileDefinitions(var46, var47.getMediaPath("tiledefinitions_erosion.tiles"), 2);
      this.LoadTileDefinitions(var46, var47.getMediaPath("tiledefinitions_apcom.tiles"), 3);
      this.LoadTileDefinitions(var46, var47.getMediaPath("tiledefinitions_overlays.tiles"), 4);
      this.LoadTileDefinitions(var46, var47.getMediaPath("tiledefinitions_b42chunkcaching.tiles"), 5);
      this.LoadTileDefinitions(var46, var47.getMediaPath("tiledefinitions_noiseworks.patch.tiles"), -1);
      this.JumboTreeDefinitions(var46, 6);
      ZomboidFileSystem.instance.loadModTileDefs();
      GameLoadingState.GameLoadingString = "";
      var46.AddSprite("media/ui/missing-tile.png");
      ScriptManager.instance.PostTileDefinitions();
      LuaEventManager.triggerEvent("OnLoadedTileDefinitions", var46);
      this.loadedTileDefinitions();
      AnimalDefinitions.getAnimalDefs();
      if (GameServer.bServer && GameServer.bSoftReset) {
         IsoRegions.init();
         WorldConverter.instance.softreset();
      }

      try {
         WeatherFxMask.init();
      } catch (Exception var23) {
         System.out.print(var23.getStackTrace());
      }

      TemplateText.Initialize();
      IsoRegions.init();
      ObjectRenderEffects.init();
      WorldConverter.instance.convert(Core.GameSaveWorld, var46);
      if (!GameLoadingState.worldVersionError) {
         SandboxOptions.instance.handleOldZombiesFile2();
         GameTime.getInstance().init();
         GameTime.getInstance().load();
         ImprovedFog.init();
         ZomboidRadio.getInstance().Init(SavedWorldVersion);
         GlobalModData.instance.init();
         InstanceTracker.load();
         if (GameServer.bServer && Core.getInstance().getPoisonousBerry() == null) {
            Core.getInstance().initPoisonousBerry();
         }

         if (GameServer.bServer && Core.getInstance().getPoisonousMushroom() == null) {
            Core.getInstance().initPoisonousMushroom();
         }

         ErosionGlobals.Boot(var46);
         WorldDictionary.init();
         ScriptManager.instance.PostWorldDictionaryInit();
         FishSchoolManager.getInstance().init();
         WorldMarkers.instance.init();
         GameEntityManager.Init(SavedWorldVersion);
         if (GameServer.bServer) {
            SharedDescriptors.initSharedDescriptors();
         }

         PersistentOutfits.instance.init();
         VirtualZombieManager.instance.init();
         VehicleIDMap.instance.Reset();
         VehicleManager.instance = new VehicleManager();
         GameLoadingState.GameLoadingString = Translator.getText("IGUI_MP_InitMap");
         this.MetaGrid.CreateStep2();
         ClimateManager.getInstance().init(this.MetaGrid);
         SafeHouse.init();
         if (!GameClient.bClient) {
            StashSystem.init();
         }

         Basements.getInstance().beforeOnLoadMapZones();
         LuaEventManager.triggerEvent("OnLoadMapZones");
         Basements.getInstance().beforeLoadMetaGrid();
         this.MetaGrid.load();
         Basements.getInstance().afterLoadMetaGrid();
         IsoMetaGrid var10000 = this.MetaGrid;
         IsoMetaGrid var10002 = this.MetaGrid;
         Objects.requireNonNull(var10002);
         var10000.load("map_zone.bin", var10002::loadZone);
         this.MetaGrid.loadCells("metagrid", "metacell_(-?[0-9]+)_(-?[0-9]+)\\.bin", IsoMetaCell::load);
         var10000 = this.MetaGrid;
         var10002 = this.MetaGrid;
         Objects.requireNonNull(var10002);
         var10000.load("map_animals.bin", var10002::loadAnimalZones);
         this.MetaGrid.processZones();
         LuaEventManager.triggerEvent("OnLoadedMapZones");
         if (GameServer.bServer) {
            ServerMap.instance.init(this.MetaGrid);
         }

         ItemConfigurator.Preprocess();
         boolean var50 = false;
         boolean var51 = false;
         if (GameClient.bClient) {
            if (ClientPlayerDB.getInstance().clientLoadNetworkPlayer() && ClientPlayerDB.getInstance().isAliveMainNetworkPlayer()) {
               var51 = true;
            }
         } else {
            var51 = PlayerDBHelper.isPlayerAlive(ZomboidFileSystem.instance.getCurrentSaveDir(), 1);
         }

         if (GameServer.bServer) {
            ServerPlayerDB.setAllow(true);
         }

         if (!GameClient.bClient && !GameServer.bServer) {
            PlayerDB.setAllow(true);
         }

         boolean var53 = false;
         boolean var55 = false;
         boolean var7 = false;
         SafeHouse var9;
         if (var51) {
            var50 = true;
            if (!this.LoadPlayerForInfo()) {
               return;
            }

            WorldX = IsoChunkMap.SWorldX[IsoPlayer.getPlayerIndex()];
            WorldY = IsoChunkMap.SWorldY[IsoPlayer.getPlayerIndex()];
            var52 = IsoChunkMap.WorldXA;
            var6 = IsoChunkMap.WorldYA;
            int var57 = IsoChunkMap.WorldZA;
         } else {
            var50 = false;
            if (GameClient.bClient && !ServerOptions.instance.SpawnPoint.getValue().isEmpty()) {
               String[] var8 = ServerOptions.instance.SpawnPoint.getValue().split(",");
               if (var8.length == 3) {
                  try {
                     IsoChunkMap.MPWorldXA = Integer.valueOf(var8[0].trim());
                     IsoChunkMap.MPWorldYA = Integer.valueOf(var8[1].trim());
                     IsoChunkMap.MPWorldZA = Integer.valueOf(var8[2].trim());
                  } catch (NumberFormatException var22) {
                     DebugLog.Moveable.println("ERROR: SpawnPoint must be x,y,z, got \"" + ServerOptions.instance.SpawnPoint.getValue() + "\"");
                     IsoChunkMap.MPWorldXA = 0;
                     IsoChunkMap.MPWorldYA = 0;
                     IsoChunkMap.MPWorldZA = 0;
                  }
               } else {
                  DebugLog.Moveable.println("ERROR: SpawnPoint must be x,y,z, got \"" + ServerOptions.instance.SpawnPoint.getValue() + "\"");
               }
            }

            if (GameClient.bClient && (IsoChunkMap.MPWorldXA != 0 || IsoChunkMap.MPWorldYA != 0)) {
               if (GameClient.bClient) {
                  IsoChunkMap.WorldXA = IsoChunkMap.MPWorldXA;
                  IsoChunkMap.WorldYA = IsoChunkMap.MPWorldYA;
                  IsoChunkMap.WorldZA = IsoChunkMap.MPWorldZA;
                  WorldX = PZMath.fastfloor((float)IsoChunkMap.WorldXA / 8.0F);
                  WorldY = PZMath.fastfloor((float)IsoChunkMap.WorldYA / 8.0F);
               }
            } else {
               IsoChunkMap.WorldXA = this.getLuaPosX();
               IsoChunkMap.WorldYA = this.getLuaPosY();
               IsoChunkMap.WorldZA = this.getLuaPosZ();
               if (GameClient.bClient && ServerOptions.instance.SafehouseAllowRespawn.getValue()) {
                  for(int var58 = 0; var58 < SafeHouse.getSafehouseList().size(); ++var58) {
                     var9 = (SafeHouse)SafeHouse.getSafehouseList().get(var58);
                     if (var9.getPlayers().contains(GameClient.username) && var9.isRespawnInSafehouse(GameClient.username)) {
                        IsoChunkMap.WorldXA = var9.getX() + var9.getH() / 2;
                        IsoChunkMap.WorldYA = var9.getY() + var9.getW() / 2;
                        IsoChunkMap.WorldZA = 0;
                     }
                  }
               }

               WorldX = PZMath.fastfloor((float)IsoChunkMap.WorldXA / 8.0F);
               WorldY = PZMath.fastfloor((float)IsoChunkMap.WorldYA / 8.0F);
            }
         }

         Core.getInstance();
         KahluaTable var59 = (KahluaTable)LuaManager.env.rawget("selectedDebugScenario");
         int var10;
         int var11;
         int var12;
         if (var59 != null) {
            KahluaTable var60 = (KahluaTable)var59.rawget("startLoc");
            var10 = ((Double)var60.rawget("x")).intValue();
            var11 = ((Double)var60.rawget("y")).intValue();
            var12 = ((Double)var60.rawget("z")).intValue();
            IsoChunkMap.WorldXA = var10;
            IsoChunkMap.WorldYA = var11;
            IsoChunkMap.WorldZA = var12;
            WorldX = PZMath.fastfloor((float)IsoChunkMap.WorldXA / 8.0F);
            WorldY = PZMath.fastfloor((float)IsoChunkMap.WorldYA / 8.0F);
         }

         MapCollisionData.instance.init(instance.getMetaGrid());
         AnimalPopulationManager.getInstance().init(this.getMetaGrid());
         ZombiePopulationManager.instance.init(instance.getMetaGrid());
         PathfindNative.USE_NATIVE_CODE = DebugOptions.instance.PathfindUseNativeCode.getValue();
         if (PathfindNative.USE_NATIVE_CODE) {
            PathfindNative.instance.init(instance.getMetaGrid());
         } else {
            PolygonalMap2.instance.init(instance.getMetaGrid());
         }

         GlobalObjectLookup.init(instance.getMetaGrid());
         if (!GameServer.bServer) {
            SpawnPoints.instance.initSinglePlayer(this.MetaGrid);
         }

         WorldStreamer.instance.create();
         this.CurrentCell = CellLoader.LoadCellBinaryChunk(var46, WorldX, WorldY);
         ClimateManager.getInstance().postCellLoadSetSnow();
         GameLoadingState.GameLoadingString = Translator.getText("IGUI_MP_LoadWorld");
         MapCollisionData.instance.start();
         if (!GameServer.bServer) {
            MapItem.LoadWorldMap();
         }

         if (GameClient.bClient) {
            WorldMapClient.instance.worldMapLoaded();
         }

         while(WorldStreamer.instance.isBusy()) {
            try {
               Thread.sleep(100L);
            } catch (InterruptedException var21) {
               var21.printStackTrace();
            }
         }

         ArrayList var61 = new ArrayList();
         var61.addAll(IsoChunk.loadGridSquare);
         Iterator var62 = var61.iterator();

         while(var62.hasNext()) {
            IsoChunk var64 = (IsoChunk)var62.next();
            this.CurrentCell.ChunkMap[0].setChunkDirect(var64, false);
         }

         this.CurrentCell.ChunkMap[0].calculateZExtentsForChunkMap();
         IsoChunk.bDoServerRequests = true;
         if (var50 && SystemDisabler.doPlayerCreation) {
            this.CurrentCell.LoadPlayer(SavedWorldVersion);
            if (GameClient.bClient) {
               IsoPlayer.getInstance().setUsername(GameClient.username);
            }

            ZomboidRadio.getInstance().getRecordedMedia().handleLegacyListenedLines(IsoPlayer.getInstance());
         } else {
            IsoGridSquare var13;
            if (GameClient.bClient) {
               ZomboidRadio.getInstance().getRecordedMedia().handleLegacyListenedLines((IsoPlayer)null);
               LuaManager.thread.debugOwnerThread = GameWindow.GameThread;
               LuaManager.debugthread.debugOwnerThread = GameWindow.GameThread;
               GameClient.sendCreatePlayer((byte)0);
               long var63 = System.currentTimeMillis();
               boolean var66 = false;

               label474:
               while(true) {
                  while(true) {
                     try {
                        if (IsoPlayer.players[0] != null) {
                           var66 = true;
                        }

                        if (System.currentTimeMillis() - var63 > 30000L || var66) {
                           break label474;
                        }

                        Thread.sleep(100L);
                     } catch (InterruptedException var37) {
                        var37.printStackTrace();
                     }
                  }
               }

               LuaManager.thread.debugOwnerThread = GameLoadingState.loader;
               LuaManager.debugthread.debugOwnerThread = GameLoadingState.loader;
               if (!var66) {
                  throw new RuntimeException("Character can't be created");
               }

               IsoPlayer var67 = IsoPlayer.players[0];
               IsoChunkMap.WorldXA = (int)var67.getX();
               IsoChunkMap.WorldYA = (int)var67.getY();
               IsoChunkMap.WorldZA = (int)var67.getZ();
               var13 = this.CurrentCell.getGridSquare(IsoChunkMap.WorldXA, IsoChunkMap.WorldYA, IsoChunkMap.WorldZA);
               if (var13 != null && var13.getRoom() != null) {
                  var13.getRoom().def.setExplored(true);
                  var13.getRoom().building.setAllExplored(true);
                  if (!GameServer.bServer && !GameClient.bClient) {
                     ZombiePopulationManager.instance.playerSpawnedAt(var13.getX(), var13.getY(), var13.getZ());
                  }
               }

               if (!GameClient.bClient) {
                  Core.getInstance().initPoisonousBerry();
                  Core.getInstance().initPoisonousMushroom();
               }

               LuaEventManager.triggerEvent("OnNewGame", var67, var13);
            } else {
               ZomboidRadio.getInstance().getRecordedMedia().handleLegacyListenedLines((IsoPlayer)null);
               var9 = null;
               if (IsoPlayer.numPlayers == 0) {
                  IsoPlayer.numPlayers = 1;
               }

               var10 = IsoChunkMap.WorldXA;
               var11 = IsoChunkMap.WorldYA;
               var12 = IsoChunkMap.WorldZA;
               if (GameClient.bClient && !ServerOptions.instance.SpawnPoint.getValue().isEmpty()) {
                  String[] var68 = ServerOptions.instance.SpawnPoint.getValue().split(",");
                  if (var68.length != 3) {
                     DebugLog.Moveable.println("ERROR: SpawnPoint must be x,y,z, got \"" + ServerOptions.instance.SpawnPoint.getValue() + "\"");
                  } else {
                     try {
                        int var14 = Integer.valueOf(var68[1].trim());
                        int var15 = Integer.valueOf(var68[0].trim());
                        int var16 = Integer.valueOf(var68[2].trim());
                        if (GameClient.bClient && ServerOptions.instance.SafehouseAllowRespawn.getValue()) {
                           for(int var17 = 0; var17 < SafeHouse.getSafehouseList().size(); ++var17) {
                              SafeHouse var18 = (SafeHouse)SafeHouse.getSafehouseList().get(var17);
                              if (var18.getPlayers().contains(GameClient.username) && var18.isRespawnInSafehouse(GameClient.username)) {
                                 var15 = var18.getX() + var18.getH() / 2;
                                 var14 = var18.getY() + var18.getW() / 2;
                                 var16 = 0;
                              }
                           }
                        }

                        if (this.CurrentCell.getGridSquare(var15, var14, var16) != null) {
                           var10 = var15;
                           var11 = var14;
                           var12 = var16;
                        }
                     } catch (NumberFormatException var38) {
                        DebugLog.Moveable.println("ERROR: SpawnPoint must be x,y,z, got \"" + ServerOptions.instance.SpawnPoint.getValue() + "\"");
                     }
                  }
               }

               IsoGridSquare var65 = this.CurrentCell.getGridSquare(var10, var11, var12);
               if (SystemDisabler.doPlayerCreation && !GameServer.bServer) {
                  if (var65 != null && var65.isFree(false) && var65.getRoom() != null) {
                     var13 = var65;
                     var65 = var65.getRoom().getFreeTile();
                     if (var65 == null) {
                        var65 = var13;
                     }
                  }

                  IsoPlayer var69 = null;
                  if (this.getLuaPlayerDesc() != null) {
                     if (GameClient.bClient && ServerOptions.instance.SafehouseAllowRespawn.getValue()) {
                        var65 = this.CurrentCell.getGridSquare(IsoChunkMap.WorldXA, IsoChunkMap.WorldYA, IsoChunkMap.WorldZA);
                        if (var65 != null && var65.isFree(false) && var65.getRoom() != null) {
                           IsoGridSquare var70 = var65;
                           var65 = var65.getRoom().getFreeTile();
                           if (var65 == null) {
                              var65 = var70;
                           }
                        }
                     }

                     if (var65 == null) {
                        throw new RuntimeException("can't create player at x,y,z=" + var10 + "," + var11 + "," + var12 + " because the square is null");
                     }

                     WorldSimulation.instance.create();
                     var69 = new IsoPlayer(instance.CurrentCell, this.getLuaPlayerDesc(), var65.getX(), var65.getY(), var65.getZ());
                     if (GameClient.bClient) {
                        var69.setUsername(GameClient.username);
                     }

                     var69.setDir(IsoDirections.SE);
                     var69.sqlID = 1;
                     IsoPlayer.players[0] = var69;
                     IsoPlayer.setInstance(var69);
                     IsoCamera.setCameraCharacter(var69);
                  }

                  IsoPlayer var71 = IsoPlayer.getInstance();
                  var71.applyTraits(this.getLuaTraits());
                  ProfessionFactory.Profession var72 = ProfessionFactory.getProfession(var71.getDescriptor().getProfession());
                  Iterator var73;
                  String var74;
                  if (var72 != null && !var72.getFreeRecipes().isEmpty()) {
                     var73 = var72.getFreeRecipes().iterator();

                     while(var73.hasNext()) {
                        var74 = (String)var73.next();
                        var71.getKnownRecipes().add(var74);
                     }
                  }

                  var73 = this.getLuaTraits().iterator();

                  label493:
                  while(true) {
                     TraitFactory.Trait var75;
                     do {
                        do {
                           if (!var73.hasNext()) {
                              if (var65 != null && var65.getRoom() != null) {
                                 var65.getRoom().def.setExplored(true);
                                 var65.getRoom().building.setAllExplored(true);
                                 if (!GameServer.bServer && !GameClient.bClient) {
                                    ZombiePopulationManager.instance.playerSpawnedAt(var65.getX(), var65.getY(), var65.getZ());
                                 }
                              }

                              if (!GameClient.bClient) {
                                 Core.getInstance().initPoisonousBerry();
                                 Core.getInstance().initPoisonousMushroom();
                              }

                              LuaEventManager.triggerEvent("OnNewGame", var69, var65);
                              break label493;
                           }

                           var74 = (String)var73.next();
                           var75 = TraitFactory.getTrait(var74);
                        } while(var75 == null);
                     } while(var75.getFreeRecipes().isEmpty());

                     Iterator var19 = var75.getFreeRecipes().iterator();

                     while(var19.hasNext()) {
                        String var20 = (String)var19.next();
                        var71.getKnownRecipes().add(var20);
                     }
                  }
               }
            }
         }

         if (PlayerDB.isAllow()) {
            PlayerDB.getInstance().m_canSavePlayers = true;
         }

         if (ClientPlayerDB.isAllow()) {
            ClientPlayerDB.getInstance().canSavePlayers = true;
         }

         TutorialManager.instance.ActiveControlZombies = false;
         ReanimatedPlayers.instance.loadReanimatedPlayers();
         if (IsoPlayer.getInstance() != null) {
            if (GameClient.bClient) {
               int var54 = PZMath.fastfloor(IsoPlayer.getInstance().getX());
               var4 = PZMath.fastfloor(IsoPlayer.getInstance().getY());
               var52 = PZMath.fastfloor(IsoPlayer.getInstance().getZ());

               while(var52 > 0) {
                  IsoGridSquare var56 = this.CurrentCell.getGridSquare(var54, var4, PZMath.fastfloor((float)var52));
                  if (var56 != null && var56.TreatAsSolidFloor()) {
                     break;
                  }

                  --var52;
                  IsoPlayer.getInstance().setZ((float)var52);
               }
            }

            ScriptManager.instance.checkAutoLearn(IsoPlayer.getInstance());
            ScriptManager.instance.checkMetaRecipes(IsoPlayer.getInstance());
            IsoPlayer.getInstance().setCurrent(this.CurrentCell.getGridSquare(PZMath.fastfloor(IsoPlayer.getInstance().getX()), PZMath.fastfloor(IsoPlayer.getInstance().getY()), PZMath.fastfloor(IsoPlayer.getInstance().getZ())));
         }

         if (!this.bLoaded) {
            if (!this.CurrentCell.getBuildingList().isEmpty()) {
            }

            if (!this.bLoaded) {
               this.PopulateCellWithSurvivors();
            }
         }

         if (IsoPlayer.players[0] != null && !this.CurrentCell.getObjectList().contains(IsoPlayer.players[0])) {
            this.CurrentCell.getObjectList().add(IsoPlayer.players[0]);
         }

         LightingThread.instance.create();
         MetaTracker.load();
         GameLoadingState.GameLoadingString = "";
         initMessaging();
         WorldDictionary.onWorldLoaded();
         this.CurrentCell.initWeatherFx();
         if (ScriptManager.instance.hasLoadErrors(!Core.bDebug) || SpriteConfigManager.HasLoadErrors()) {
            DebugLogStream var76 = DebugLog.Moveable;
            boolean var10001 = ScriptManager.instance.hasLoadErrors(!Core.bDebug);
            var76.println("script error = " + var10001 + ", sprite error = " + SpriteConfigManager.HasLoadErrors());
            throw new WorldDictionaryException("World loading could not proceed, there are script load errors. (Actual error may be printed earlier in log)");
         }
      }
   }

   int readWorldVersion() {
      File var1;
      FileInputStream var2;
      DataInputStream var3;
      if (GameServer.bServer) {
         var1 = ZomboidFileSystem.instance.getFileInCurrentSave("map_t.bin");

         try {
            var2 = new FileInputStream(var1);

            label107: {
               int var8;
               try {
                  label118: {
                     var3 = new DataInputStream(var2);

                     try {
                        byte var21 = var3.readByte();
                        byte var5 = var3.readByte();
                        byte var6 = var3.readByte();
                        byte var7 = var3.readByte();
                        if (var21 == 71 && var5 == 77 && var6 == 84 && var7 == 77) {
                           var8 = var3.readInt();
                           break label118;
                        }
                     } catch (Throwable var17) {
                        try {
                           var3.close();
                        } catch (Throwable var12) {
                           var17.addSuppressed(var12);
                        }

                        throw var17;
                     }

                     var3.close();
                     break label107;
                  }

                  var3.close();
               } catch (Throwable var18) {
                  try {
                     var2.close();
                  } catch (Throwable var11) {
                     var18.addSuppressed(var11);
                  }

                  throw var18;
               }

               var2.close();
               return var8;
            }

            var2.close();
         } catch (FileNotFoundException var19) {
         } catch (IOException var20) {
            ExceptionLogger.logException(var20);
         }

         return -1;
      } else {
         var1 = ZomboidFileSystem.instance.getFileInCurrentSave("map_ver.bin");

         try {
            var2 = new FileInputStream(var1);

            int var4;
            try {
               var3 = new DataInputStream(var2);

               try {
                  var4 = var3.readInt();
               } catch (Throwable var13) {
                  try {
                     var3.close();
                  } catch (Throwable var10) {
                     var13.addSuppressed(var10);
                  }

                  throw var13;
               }

               var3.close();
            } catch (Throwable var14) {
               try {
                  var2.close();
               } catch (Throwable var9) {
                  var14.addSuppressed(var9);
               }

               throw var14;
            }

            var2.close();
            return var4;
         } catch (FileNotFoundException var15) {
         } catch (IOException var16) {
            ExceptionLogger.logException(var16);
         }

         return -1;
      }
   }

   public ArrayList<String> getLuaTraits() {
      if (this.luatraits == null) {
         this.luatraits = new ArrayList();
      }

      return this.luatraits;
   }

   public void addLuaTrait(String var1) {
      this.getLuaTraits().add(var1);
   }

   public SurvivorDesc getLuaPlayerDesc() {
      return this.luaDesc;
   }

   public void setLuaPlayerDesc(SurvivorDesc var1) {
      this.luaDesc = var1;
   }

   public void KillCell() {
      this.helicopter.deactivate();
      CollisionManager.instance.ContactMap.clear();
      ObjectIDManager.getInstance().clear();
      FliesSound.instance.Reset();
      IsoObjectPicker.Instance.Init();
      IsoChunkMap.SharedChunks.clear();
      SoundManager.instance.StopMusic();
      WorldSoundManager.instance.KillCell();
      ZombieGroupManager.instance.Reset();
      this.CurrentCell.Dispose();
      IsoSpriteManager.instance.Dispose();
      this.CurrentCell = null;
      CellLoader.wanderRoom = null;
      IsoLot.Dispose();
      IsoGameCharacter.getSurvivorMap().clear();
      IsoPlayer.getInstance().setCurrent((IsoGridSquare)null);
      IsoPlayer.getInstance().setLast((IsoGridSquare)null);
      IsoPlayer.getInstance().square = null;
      RainManager.reset();
      IsoFireManager.Reset();
      ObjectAmbientEmitters.Reset();
      AnimalVocalsManager.Reset();
      ZombieVocalsManager.Reset();
      IsoWaterFlow.Reset();
      this.MetaGrid.Dispose();
      this.biomeMap.Dispose();
      IsoBuilding.IDMax = 0;
      instance = new IsoWorld();
   }

   public void setDrawWorld(boolean var1) {
      this.bDrawWorld = var1;
   }

   public void sceneCullZombies() {
      this.zombieWithModel.clear();
      this.zombieWithoutModel.clear();

      int var1;
      for(var1 = 0; var1 < this.CurrentCell.getZombieList().size(); ++var1) {
         IsoZombie var2 = (IsoZombie)this.CurrentCell.getZombieList().get(var1);
         boolean var3 = false;

         for(int var4 = 0; var4 < IsoPlayer.numPlayers; ++var4) {
            IsoPlayer var5 = IsoPlayer.players[var4];
            if (var5 != null && var2.current != null) {
               float var6 = (float)var2.getScreenProperX(var4);
               float var7 = (float)var2.getScreenProperY(var4);
               if (!(var6 < -100.0F) && !(var7 < -100.0F) && !(var6 > (float)(Core.getInstance().getOffscreenWidth(var4) + 100)) && !(var7 > (float)(Core.getInstance().getOffscreenHeight(var4) + 100)) && (var2.getAlpha(var4) != 0.0F && var2.legsSprite.def.alpha != 0.0F || var2.current.isCouldSee(var4))) {
                  var3 = true;
                  break;
               }
            }
         }

         if (var3 && var2.isCurrentState(FakeDeadZombieState.instance())) {
            var3 = false;
         }

         if (var3) {
            this.zombieWithModel.add(var2);
         } else {
            this.zombieWithoutModel.add(var2);
         }
      }

      Collections.sort(this.zombieWithModel, compScoreToPlayer);
      var1 = 0;
      int var8 = 0;
      int var9 = 0;
      short var10 = 510;
      PerformanceSettings.AnimationSkip = 0;

      int var11;
      IsoZombie var12;
      for(var11 = 0; var11 < this.zombieWithModel.size(); ++var11) {
         var12 = (IsoZombie)this.zombieWithModel.get(var11);
         if (var9 < var10) {
            if (!var12.Ghost) {
               ++var8;
               ++var9;
               var12.setSceneCulled(false);
               if (var12.legsSprite != null && var12.legsSprite.modelSlot != null) {
                  if (var8 > PerformanceSettings.ZombieAnimationSpeedFalloffCount) {
                     ++var1;
                     var8 = 0;
                  }

                  if (var9 < PerformanceSettings.ZombieBonusFullspeedFalloff) {
                     var12.legsSprite.modelSlot.model.setInstanceSkip(var8 / PerformanceSettings.ZombieBonusFullspeedFalloff);
                     var8 = 0;
                  } else {
                     var12.legsSprite.modelSlot.model.setInstanceSkip(var1 + PerformanceSettings.AnimationSkip);
                  }

                  if (var12.legsSprite.modelSlot.model.AnimPlayer != null) {
                     if (var9 >= PerformanceSettings.numberZombiesBlended) {
                        var12.legsSprite.modelSlot.model.AnimPlayer.bDoBlending = false;
                     } else {
                        var12.legsSprite.modelSlot.model.AnimPlayer.bDoBlending = !var12.isAlphaAndTargetZero(0) || !var12.isAlphaAndTargetZero(1) || !var12.isAlphaAndTargetZero(2) || !var12.isAlphaAndTargetZero(3);
                     }
                  }
               }
            }
         } else {
            var12.setSceneCulled(true);
            if (var12.hasAnimationPlayer()) {
               var12.getAnimationPlayer().bDoBlending = false;
            }
         }
      }

      for(var11 = 0; var11 < this.zombieWithoutModel.size(); ++var11) {
         var12 = (IsoZombie)this.zombieWithoutModel.get(var11);
         if (var12.hasActiveModel()) {
            var12.setSceneCulled(true);
         }

         if (var12.hasAnimationPlayer()) {
            var12.getAnimationPlayer().bDoBlending = false;
         }
      }

   }

   public void sceneCullAnimals() {
      this.animalWithModel.clear();
      this.animalWithoutModel.clear();

      int var1;
      for(var1 = 0; var1 < this.CurrentCell.getObjectList().size(); ++var1) {
         IsoMovingObject var2 = (IsoMovingObject)this.CurrentCell.getObjectList().get(var1);
         IsoAnimal var3 = (IsoAnimal)Type.tryCastTo(var2, IsoAnimal.class);
         if (var3 != null) {
            boolean var4 = false;

            for(int var5 = 0; var5 < IsoPlayer.numPlayers; ++var5) {
               IsoPlayer var6 = IsoPlayer.players[var5];
               if (var6 != null && var3.current != null) {
                  float var7 = (float)((int)(IsoUtils.XToScreen(var3.getX(), var3.getY(), var3.getZ(), 0) - IsoCamera.cameras[var5].getOffX()));
                  float var8 = (float)((int)(IsoUtils.YToScreen(var3.getX(), var3.getY(), var3.getZ(), 0) - IsoCamera.cameras[var5].getOffY()));
                  if (!(var7 < -100.0F) && !(var8 < -100.0F) && !(var7 > (float)(Core.getInstance().getOffscreenWidth(var5) + 100)) && !(var8 > (float)(Core.getInstance().getOffscreenHeight(var5) + 100)) && (var3.getAlpha(var5) != 0.0F && var3.legsSprite.def.alpha != 0.0F || var3.current.isCouldSee(var5))) {
                     var4 = true;
                     break;
                  }
               }
            }

            if (var4 && var3.isCurrentState(FakeDeadZombieState.instance())) {
               var4 = false;
            }

            if (var4) {
               this.animalWithModel.add(var3);
            } else {
               this.animalWithoutModel.add(var3);
            }
         }
      }

      IsoAnimal var9;
      for(var1 = 0; var1 < this.animalWithModel.size(); ++var1) {
         var9 = (IsoAnimal)this.animalWithModel.get(var1);
         var9.setSceneCulled(false);
         if (var9.hasAnimationPlayer()) {
            var9.getAnimationPlayer().bDoBlending = true;
         }
      }

      for(var1 = 0; var1 < this.animalWithoutModel.size(); ++var1) {
         var9 = (IsoAnimal)this.animalWithoutModel.get(var1);
         if (var9.hasActiveModel()) {
            var9.setSceneCulled(true);
         }

         if (var9.hasAnimationPlayer()) {
            var9.getAnimationPlayer().bDoBlending = false;
         }
      }

   }

   public void render() {
      IsoWorld.s_performance.isoWorldRender.invokeAndMeasure(this, IsoWorld::renderInternal);
   }

   private void renderInternal() {
      if (this.bDrawWorld) {
         IsoGameCharacter var1 = IsoCamera.getCameraCharacter();
         if (var1 != null) {
            SpriteRenderer.instance.doCoreIntParam(0, var1.getX());
            SpriteRenderer.instance.doCoreIntParam(1, var1.getY());
            SpriteRenderer.instance.doCoreIntParam(2, IsoCamera.frameState.CamCharacterZ);

            try {
               GameProfiler.ProfileArea var2 = GameProfiler.getInstance().startIfEnabled("Cull");
               this.sceneCullZombies();
               this.sceneCullAnimals();
               GameProfiler.getInstance().end(var2);
            } catch (Throwable var4) {
               ExceptionLogger.logException(var4);
            }

            try {
               WeatherFxMask.initMask();
               DeadBodyAtlas.instance.render();
               WorldItemAtlas.instance.render();
               this.CurrentCell.render();
               this.DrawIsoCursorHelper();
               DeadBodyAtlas.instance.renderDebug();
               GameProfiler.getInstance().invokeAndMeasure("renderPathfinding", this::renderPathfinding);
               WorldSoundManager.instance.render();
               WorldFlares.debugRender();
               WorldMarkers.instance.debugRender();
               ObjectAmbientEmitters.getInstance().render();
               GameProfiler.getInstance().invokeAndMeasure("renderVocals", this::renderVocals);
               GameProfiler.getInstance().invokeAndMeasure("renderWeatherFX", this::renderWeatherFX);
               if (PerformanceSettings.FBORenderChunk) {
                  FBORenderAreaHighlights.getInstance().render();
               }

               ParameterInside.renderDebug();
               LineDrawer.render();
               if (GameClient.bClient) {
                  ClientServerMap.render(IsoCamera.frameState.playerIndex);
                  PassengerMap.render(IsoCamera.frameState.playerIndex);
               }

               GameProfiler var10000 = GameProfiler.getInstance();
               SkyBox var10002 = SkyBox.getInstance();
               Objects.requireNonNull(var10002);
               var10000.invokeAndMeasure("Skybox", var10002::render);
            } catch (Throwable var3) {
               ExceptionLogger.logException(var3);
            }

         }
      }
   }

   private void renderPathfinding() {
      if (PathfindNative.USE_NATIVE_CODE) {
         PathfindNative.instance.render();
         PathfindNativeRenderer.instance.render();
      } else {
         PolygonalMap2.instance.render();
      }

      BorderFinderRenderer.instance.render();
   }

   private void renderVocals() {
      AnimalVocalsManager.instance.render();
      ZombieVocalsManager.instance.render();
   }

   private void renderWeatherFX() {
      this.getCell().getWeatherFX().getDrawer(IsoWeatherFX.ID_CLOUD).startFrame();
      this.getCell().getWeatherFX().getDrawer(IsoWeatherFX.ID_FOG).startFrame();
      this.getCell().getWeatherFX().getDrawer(IsoWeatherFX.ID_SNOW).startFrame();
      this.getCell().getWeatherFX().getDrawer(IsoWeatherFX.ID_RAIN).startFrame();
      WeatherFxMask.renderFxMask(IsoCamera.frameState.playerIndex);
   }

   public void DrawPlayerCone() {
      int var1 = IsoCamera.frameState.playerIndex;
      SpriteRenderer.instance.pushIsoView(IsoPlayer.getInstance().getX(), IsoPlayer.getInstance().getY(), IsoPlayer.getInstance().getZ(), (float)Math.toRadians(180.0), false);
      IsoPlayer var2 = IsoPlayer.getInstance();
      float var3 = var2.getStats().fatigue - 0.6F;
      if (var3 < 0.0F) {
         var3 = 0.0F;
      }

      var3 *= 2.5F;
      if (var2.Traits.HardOfHearing.isSet() && var3 < 0.7F) {
         var3 = 0.7F;
      }

      float var4 = 2.0F;
      if (var2.Traits.KeenHearing.isSet()) {
         var4 += 3.0F;
      }

      float var5 = LightingJNI.calculateVisionCone(var2);
      var5 = -var5;
      var5 = 1.0F - var5;
      Vector2 var6 = var2.getLookVector(this.coneTempo1);
      BaseVehicle var7 = var2.getVehicle();
      if (var7 != null && !var2.isAiming() && !var2.isLookingWhileInVehicle() && var7.isDriver(var2) && var7.getCurrentSpeedKmHour() < -1.0F) {
         var6.rotate(3.1415927F);
      }

      if (var5 < 0.0F) {
         var5 = Math.abs(var5) + 1.0F;
      }

      var5 = (float)((double)var5 * 1.5707963267948966);
      this.coneTempo2.x = var6.x;
      this.coneTempo2.y = var6.y;
      this.coneTempo3.x = var6.x;
      this.coneTempo3.y = var6.y;
      this.coneTempo2.rotate(-var5);
      this.coneTempo3.rotate(var5);
      float var8 = this.coneTempo2.x * 1000.0F;
      float var9 = this.coneTempo2.y * 1000.0F;
      float var10 = var8 + -var6.x * 1000.0F;
      float var11 = var9 + -var6.y * 1000.0F;
      float var12 = -var6.x * 1000.0F;
      float var13 = -var6.y * 1000.0F;
      IndieGL.disableDepthTest();
      IndieGL.disableScissorTest();
      SpriteRenderer.instance.glBuffer(8, 0);
      if (ViewConeTextureFBO.instance.getTexture() != null) {
         SpriteRenderer.instance.glViewport(0, 0, ViewConeTextureFBO.instance.getTexture().getWidth(), ViewConeTextureFBO.instance.getTexture().getHeight());
      }

      IndieGL.StartShader(0);
      SpriteRenderer.instance.renderPoly(0.0F, 0.0F, var8, var9, var10, var11, var12, var13, 0.0F, 0.0F, 0.0F, 0.5F);
      IndieGL.EndShader();
      var8 = this.coneTempo3.x * 1000.0F;
      var9 = this.coneTempo3.y * 1000.0F;
      var10 = var8 + -var6.x * 1000.0F;
      var11 = var9 + -var6.y * 1000.0F;
      var12 = -var6.x * 1000.0F;
      var13 = -var6.y * 1000.0F;
      SpriteRenderer.instance.renderPoly(var12, var13, var10, var11, var8, var9, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F);
      SpriteRenderer.instance.glBuffer(9, 0);
      SpriteRenderer.instance.glViewport(IsoCamera.getScreenLeft(var1), IsoCamera.getScreenTop(var1), IsoCamera.getScreenWidth(var1), IsoCamera.getScreenHeight(var1));
      SpriteRenderer.instance.popIsoView();
      IndieGL.enableScissorTest();
   }

   public void DrawPlayerCone2() {
      IndieGL.glDepthMask(false);
      IndieGL.glBlendFunc(770, 771);
      if (SceneShaderStore.BlurShader != null) {
         SceneShaderStore.BlurShader.setTexture(ViewConeTextureFBO.instance.getTexture());
      }

      if (SceneShaderStore.BlurShader != null) {
         IndieGL.StartShader(SceneShaderStore.BlurShader, IsoPlayer.getPlayerIndex());
      }

      SpriteRenderer.instance.render(ViewConeTextureFBO.instance.getTexture(), 0.0F, (float)Core.getInstance().getOffscreenHeight(IsoPlayer.getPlayerIndex()), (float)Core.getInstance().getOffscreenWidth(IsoPlayer.getPlayerIndex()), (float)(-Core.getInstance().getOffscreenHeight(IsoPlayer.getPlayerIndex())), 1.0F, 1.0F, 1.0F, 1.0F, (Consumer)null);
      if (SceneShaderStore.BlurShader != null) {
         IndieGL.EndShader();
      }

      IndieGL.glDepthMask(true);
   }

   private void DrawIsoCursorHelper() {
      if (Core.getInstance().getOffscreenBuffer() == null) {
         IsoPlayer var1 = IsoPlayer.getInstance();
         if (var1 != null && !var1.isDead() && var1.isAiming() && var1.PlayerIndex == 0 && var1.JoypadBind == -1) {
            if (!GameTime.isGamePaused()) {
               float var2 = 0.05F;
               switch (Core.getInstance().getIsoCursorVisibility()) {
                  case 0:
                     return;
                  case 1:
                     var2 = 0.05F;
                     break;
                  case 2:
                     var2 = 0.1F;
                     break;
                  case 3:
                     var2 = 0.15F;
                     break;
                  case 4:
                     var2 = 0.3F;
                     break;
                  case 5:
                     var2 = 0.5F;
                     break;
                  case 6:
                     var2 = 0.75F;
               }

               if (Core.getInstance().isFlashIsoCursor()) {
                  if (this.flashIsoCursorInc) {
                     this.flashIsoCursorA += 0.1F;
                     if (this.flashIsoCursorA >= 1.0F) {
                        this.flashIsoCursorInc = false;
                     }
                  } else {
                     this.flashIsoCursorA -= 0.1F;
                     if (this.flashIsoCursorA <= 0.0F) {
                        this.flashIsoCursorInc = true;
                     }
                  }

                  var2 = this.flashIsoCursorA;
               }

               Texture var3 = Texture.getSharedTexture("media/ui/isocursor.png");
               int var4 = (int)((float)(var3.getWidth() * Core.TileScale) / 2.0F);
               int var5 = (int)((float)(var3.getHeight() * Core.TileScale) / 2.0F);
               SpriteRenderer.instance.setDoAdditive(true);
               SpriteRenderer.instance.renderi(var3, Mouse.getX() - var4 / 2, Mouse.getY() - var5 / 2, var4, var5, var2, var2, var2, var2, (Consumer)null);
               SpriteRenderer.instance.setDoAdditive(false);
            }
         }
      }
   }

   private void updateWorld() {
      this.CurrentCell.update();
      IsoRegions.update();
      HaloTextHelper.update();
      CollisionManager.instance.ResolveContacts();
      if (DebugOptions.instance.ThreadAnimation.getValue()) {
         MovingObjectUpdateScheduler var10000 = MovingObjectUpdateScheduler.instance;
         Objects.requireNonNull(var10000);
         animationThread = CompletableFuture.runAsync(var10000::postupdate);
      } else {
         GameProfiler.getInstance().invokeAndMeasure("Animation", MovingObjectUpdateScheduler.instance, MovingObjectUpdateScheduler::postupdate);
      }

   }

   public void FinishAnimation() {
      if (animationThread != null) {
         GameProfiler var10000 = GameProfiler.getInstance();
         CompletableFuture var10002 = animationThread;
         Objects.requireNonNull(var10002);
         var10000.invokeAndMeasure("Wait Animation", var10002::join);
         animationThread = null;
      }

   }

   public void update() {
      IsoWorld.s_performance.isoWorldUpdate.invokeAndMeasure(this, IsoWorld::updateInternal);
      GameProfiler.getInstance().invokeAndMeasure("Update DZ", DesignationZone::update);
   }

   private void updateInternal() {
      ++this.m_frameNo;

      try {
         if (GameServer.bServer) {
            VehicleManager.instance.serverUpdate();
         }
      } catch (Exception var7) {
         var7.printStackTrace();
      }

      WorldSimulation.instance.update();
      HutchManager.getInstance().updateAll();
      ImprovedFog.update();
      this.helicopter.update();
      long var1 = System.currentTimeMillis();
      if (var1 - this.emitterUpdateMS >= 30L) {
         this.emitterUpdateMS = var1;
         this.emitterUpdate = true;
      } else {
         this.emitterUpdate = false;
      }

      for(int var3 = 0; var3 < this.currentEmitters.size(); ++var3) {
         BaseSoundEmitter var4 = (BaseSoundEmitter)this.currentEmitters.get(var3);
         if (this.emitterUpdate || var4.hasSoundsToStart()) {
            var4.tick();
         }

         if (var4.isEmpty()) {
            FMODSoundEmitter var5 = (FMODSoundEmitter)Type.tryCastTo(var4, FMODSoundEmitter.class);
            if (var5 != null) {
               var5.clearParameters();
            }

            this.currentEmitters.remove(var3);
            this.freeEmitters.push(var4);
            IsoObject var6 = (IsoObject)this.emitterOwners.remove(var4);
            if (var6 != null && var6.emitter == var4) {
               var6.emitter = null;
            }

            --var3;
         }
      }

      if (!GameClient.bClient && !GameServer.bServer) {
         IsoMetaCell var8 = this.MetaGrid.getCurrentCellData();
         if (var8 != null) {
            var8.checkTriggers();
         }
      }

      WorldSoundManager.instance.initFrame();
      ZombieGroupManager.instance.preupdate();
      OnceEvery.update();
      CollisionManager.instance.initUpdate();
      CompletableFuture var9 = null;
      if (DebugOptions.instance.ThreadWorld.getValue()) {
         var9 = CompletableFuture.runAsync(this::updateThread, PZForkJoinPool.commonPool());
      }

      GameProfiler.getInstance().invokeAndMeasure("Update Climate", ClimateManager.getInstance(), ClimateManager::update);
      this.updateWorld();
      if (var9 != null) {
         GameProfiler.getInstance().invokeAndMeasure("Wait Thread", var9, CompletableFuture::join);
      } else {
         this.updateThread();
      }

      if (m_animationRecorderDiscard) {
         AnimationPlayerRecorder.discardOldRecordings();
         m_animationRecorderDiscard = false;
      }

   }

   private void updateThread() {
      GameProfiler.getInstance().invokeAndMeasure("Update Buildings", this, IsoWorld::updateBuildings);
      GameProfiler.getInstance().invokeAndMeasure("Update Static", ObjectRenderEffects::updateStatic);

      for(int var1 = 0; var1 < this.AddCoopPlayers.size(); ++var1) {
         AddCoopPlayer var2 = (AddCoopPlayer)this.AddCoopPlayers.get(var1);
         var2.update();
         if (var2.isFinished()) {
            this.AddCoopPlayers.remove(var1--);
         }
      }

      if (!GameServer.bServer) {
         IsoPlayer.UpdateRemovedEmitters();
      }

      GameProfiler.getInstance().invokeAndMeasure("Update DBs", this, IsoWorld::updateDBs);
      if (this.updateSafehousePlayers > 0 && (GameServer.bServer || GameClient.bClient)) {
         --this.updateSafehousePlayers;
         if (this.updateSafehousePlayers == 0) {
            this.updateSafehousePlayers = 200;
            SafeHouse.updateSafehousePlayersConnected();
         }
      }

      GameProfiler.getInstance().invokeAndMeasure("Update VA", AnimalZones::updateVirtualAnimals);
      GameProfiler.getInstance().invokeAndMeasure("Load Animal Defs", AnimalTracksDefinitions::loadTracksDefinitions);
   }

   private void updateBuildings() {
      for(int var1 = 0; var1 < this.CurrentCell.getBuildingList().size(); ++var1) {
         ((IsoBuilding)this.CurrentCell.getBuildingList().get(var1)).update();
      }

   }

   private void updateDBs() {
      try {
         if (PlayerDB.isAvailable()) {
            PlayerDB.getInstance().updateMain();
         }

         if (ClientPlayerDB.isAvailable()) {
            ClientPlayerDB.getInstance().updateMain();
         }

         VehiclesDB2.instance.updateMain();
      } catch (Exception var2) {
         ExceptionLogger.logException(var2);
      }

   }

   public IsoCell getCell() {
      return this.CurrentCell;
   }

   private void PopulateCellWithSurvivors() {
   }

   public int getWorldSquareY() {
      return this.CurrentCell.ChunkMap[IsoPlayer.getPlayerIndex()].WorldY * 8;
   }

   public int getWorldSquareX() {
      return this.CurrentCell.ChunkMap[IsoPlayer.getPlayerIndex()].WorldX * 8;
   }

   public IsoMetaChunk getMetaChunk(int var1, int var2) {
      return this.MetaGrid.getChunkData(var1, var2);
   }

   public IsoMetaChunk getMetaChunkFromTile(int var1, int var2) {
      return this.MetaGrid.getChunkDataFromTile(var1, var2);
   }

   public float getGlobalTemperature() {
      return ClimateManager.getInstance().getTemperature();
   }

   public String getWeather() {
      return this.weather;
   }

   public void setWeather(String var1) {
      this.weather = var1;
   }

   public int getLuaSpawnCellX() {
      return PZMath.coordmodulo(this.luaPosX, IsoCell.CellSizeInSquares);
   }

   /** @deprecated */
   @Deprecated
   public void setLuaSpawnCellX(int var1) {
   }

   public int getLuaSpawnCellY() {
      return PZMath.coordmodulo(this.luaPosY, IsoCell.CellSizeInSquares);
   }

   /** @deprecated */
   @Deprecated
   public void setLuaSpawnCellY(int var1) {
   }

   public int getLuaPosX() {
      return this.luaPosX;
   }

   public void setLuaPosX(int var1) {
      this.luaPosX = var1;
   }

   public int getLuaPosY() {
      return this.luaPosY;
   }

   public void setLuaPosY(int var1) {
      this.luaPosY = var1;
   }

   public int getLuaPosZ() {
      return this.luaPosZ;
   }

   public void setLuaPosZ(int var1) {
      this.luaPosZ = var1;
   }

   public void setSpawnRegion(String var1) {
      if (var1 != null) {
         this.spawnRegionName = var1;
      }

   }

   public String getSpawnRegion() {
      return this.spawnRegionName;
   }

   public String getWorld() {
      return Core.GameSaveWorld;
   }

   public void transmitWeather() {
      if (GameServer.bServer) {
         GameServer.sendWeather();
      }
   }

   public boolean isValidSquare(int var1, int var2, int var3) {
      return var3 >= -32 && var3 <= 31 ? this.MetaGrid.isValidSquare(var1, var2) : false;
   }

   public ArrayList<RandomizedZoneStoryBase> getRandomizedZoneList() {
      return this.randomizedZoneList;
   }

   public RandomizedZoneStoryBase getRandomizedZoneStoryByName(String var1) {
      for(int var2 = 0; var2 < this.randomizedZoneList.size(); ++var2) {
         RandomizedZoneStoryBase var3 = (RandomizedZoneStoryBase)this.randomizedZoneList.get(var2);
         if (var3.getName().equalsIgnoreCase(var1)) {
            return var3;
         }
      }

      return null;
   }

   public ArrayList<RandomizedBuildingBase> getRandomizedBuildingList() {
      return this.randomizedBuildingList;
   }

   public ArrayList<RandomizedVehicleStoryBase> getRandomizedVehicleStoryList() {
      return this.randomizedVehicleStoryList;
   }

   public RandomizedVehicleStoryBase getRandomizedVehicleStoryByName(String var1) {
      for(int var2 = 0; var2 < this.randomizedVehicleStoryList.size(); ++var2) {
         RandomizedVehicleStoryBase var3 = (RandomizedVehicleStoryBase)this.randomizedVehicleStoryList.get(var2);
         if (var3.getName().equalsIgnoreCase(var1)) {
            return var3;
         }
      }

      return null;
   }

   public RandomizedBuildingBase getRBBasic() {
      return this.RBBasic;
   }

   public RandomizedWorldBase getRandomizedWorldBase() {
      return this.RandomizedWorldBase;
   }

   public String getDifficulty() {
      return Core.getDifficulty();
   }

   public void setDifficulty(String var1) {
      Core.setDifficulty(var1);
   }

   public static boolean getZombiesDisabled() {
      return NoZombies || !SystemDisabler.doZombieCreation || SandboxOptions.instance.Zombies.getValue() == 6;
   }

   public static boolean getZombiesEnabled() {
      return !getZombiesDisabled();
   }

   public ClimateManager getClimateManager() {
      return ClimateManager.getInstance();
   }

   public IsoPuddles getPuddlesManager() {
      return IsoPuddles.getInstance();
   }

   public static int getWorldVersion() {
      return 219;
   }

   public HashMap<String, ArrayList<UUID>> getSpawnedZombieZone() {
      return this.spawnedZombieZone;
   }

   public int getTimeSinceLastSurvivorInHorde() {
      return this.timeSinceLastSurvivorInHorde;
   }

   public void setTimeSinceLastSurvivorInHorde(int var1) {
      this.timeSinceLastSurvivorInHorde = var1;
   }

   public float getWorldAgeDays() {
      float var1 = (float)GameTime.getInstance().getWorldAgeHours() / 24.0F;
      var1 += (float)((SandboxOptions.instance.TimeSinceApo.getValue() - 1) * 30);
      return var1;
   }

   public HashMap<String, ArrayList<String>> getAllTiles() {
      return this.allTiles;
   }

   public ArrayList<String> getAllTilesName() {
      ArrayList var1 = new ArrayList();
      Iterator var2 = this.allTiles.keySet().iterator();

      while(var2.hasNext()) {
         var1.add((String)var2.next());
      }

      Collections.sort(var1);
      return var1;
   }

   public ArrayList<String> getAllTiles(String var1) {
      return (ArrayList)this.allTiles.get(var1);
   }

   public boolean isHydroPowerOn() {
      return this.bHydroPowerOn;
   }

   public void setHydroPowerOn(boolean var1) {
      this.bHydroPowerOn = var1;
   }

   public ArrayList<String> getTileImageNames() {
      return this.tileImages;
   }

   public static void parseDistributions() {
      ItemPickerJava.Parse();
      ItemPickerJava.InitSandboxLootSettings();
   }

   public void setRules(Rules var1) {
      this.rules = var1;
   }

   public Rules getRules() {
      return this.rules;
   }

   public void setWgChunk(WGChunk var1) {
      this.wgChunk = var1;
   }

   public WGChunk getWgChunk() {
      return this.wgChunk;
   }

   public void setBlending(Blending var1) {
      this.blending = var1;
   }

   public Blending getBlending() {
      return this.blending;
   }

   public void setAttachmentsHandler(AttachmentsHandler var1) {
      this.attachmentsHandler = var1;
   }

   public AttachmentsHandler getAttachmentsHandler() {
      return this.attachmentsHandler;
   }

   public void setZoneGenerator(ZoneGenerator var1) {
      this.zoneGenerator = var1;
   }

   public ZoneGenerator getZoneGenerator() {
      return this.zoneGenerator;
   }

   public void setBiomeMap(BiomeMap var1) {
      this.biomeMap = var1;
   }

   public BiomeMap getBiomeMap() {
      return this.biomeMap;
   }

   private static class CompScoreToPlayer implements Comparator<IsoZombie> {
      private CompScoreToPlayer() {
      }

      public int compare(IsoZombie var1, IsoZombie var2) {
         float var3 = this.getScore(var1);
         float var4 = this.getScore(var2);
         if (var3 < var4) {
            return 1;
         } else {
            return var3 > var4 ? -1 : 0;
         }
      }

      public float getScore(IsoZombie var1) {
         float var2 = 1.4E-45F;

         for(int var3 = 0; var3 < 4; ++var3) {
            IsoPlayer var4 = IsoPlayer.players[var3];
            if (var4 != null && var4.current != null) {
               float var5 = var4.getZombieRelevenceScore(var1);
               var2 = Math.max(var2, var5);
            }
         }

         return var2;
      }
   }

   private static class s_performance {
      static final PerformanceProfileProbe isoWorldUpdate = new PerformanceProfileProbe("IsoWorld.update");
      static final PerformanceProfileProbe isoWorldRender = new PerformanceProfileProbe("IsoWorld.render");

      private s_performance() {
      }
   }

   public class Frame {
      public ArrayList<Integer> xPos = new ArrayList();
      public ArrayList<Integer> yPos = new ArrayList();
      public ArrayList<Integer> Type = new ArrayList();

      public Frame() {
         Iterator var2 = IsoWorld.instance.CurrentCell.getObjectList().iterator();

         while(var2 != null && var2.hasNext()) {
            IsoMovingObject var3 = (IsoMovingObject)var2.next();
            boolean var4 = true;
            byte var5;
            if (var3 instanceof IsoPlayer) {
               var5 = 0;
            } else if (var3 instanceof IsoSurvivor) {
               var5 = 1;
            } else {
               if (!(var3 instanceof IsoZombie) || ((IsoZombie)var3).Ghost) {
                  continue;
               }

               var5 = 2;
            }

            this.xPos.add(PZMath.fastfloor(var3.getX()));
            this.yPos.add(PZMath.fastfloor(var3.getY()));
            this.Type.add(Integer.valueOf(var5));
         }

      }
   }

   public static class MetaCell {
      public int x;
      public int y;
      public int zombieCount;
      public IsoDirections zombieMigrateDirection;
      public int[][] from = new int[3][3];

      public MetaCell() {
      }
   }

   private static class CompDistToPlayer implements Comparator<IsoZombie> {
      public float px;
      public float py;

      private CompDistToPlayer() {
      }

      public int compare(IsoZombie var1, IsoZombie var2) {
         float var3 = IsoUtils.DistanceManhatten((float)PZMath.fastfloor(var1.getX()), (float)PZMath.fastfloor(var1.getY()), this.px, this.py);
         float var4 = IsoUtils.DistanceManhatten((float)PZMath.fastfloor(var2.getX()), (float)PZMath.fastfloor(var2.getY()), this.px, this.py);
         if (var3 < var4) {
            return -1;
         } else {
            return var3 > var4 ? 1 : 0;
         }
      }
   }
}
