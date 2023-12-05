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
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.krka.kahlua.vm.KahluaTable;
import zombie.CollisionManager;
import zombie.DebugFileWatcher;
import zombie.FliesSound;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.MapCollisionData;
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
import zombie.characters.HaloTextHelper;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.characters.TriggerSetAnimationRecorderFile;
import zombie.characters.ZombieVocalsManager;
import zombie.characters.professions.ProfessionFactory;
import zombie.characters.traits.TraitFactory;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.Rand;
import zombie.core.SpriteRenderer;
import zombie.core.TilePropertyAliasMap;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.physics.WorldSimulation;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.properties.PropertyContainer;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.skinnedmodel.model.WorldItemAtlas;
import zombie.core.stash.StashSystem;
import zombie.core.textures.Texture;
import zombie.core.utils.OnceEvery;
import zombie.debug.DebugLog;
import zombie.debug.LineDrawer;
import zombie.erosion.ErosionGlobals;
import zombie.gameStates.GameLoadingState;
import zombie.globalObjects.GlobalObjectLookup;
import zombie.input.Mouse;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.MapItem;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.SafeHouse;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.ObjectRenderEffects;
import zombie.iso.objects.RainManager;
import zombie.iso.sprite.IsoDirectionFrame;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.sprite.SkyBox;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.WorldFlares;
import zombie.iso.weather.fog.ImprovedFog;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.network.BodyDamageSync;
import zombie.network.ClientServerMap;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.NetChecksum;
import zombie.network.PassengerMap;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.popman.ZombiePopulationManager;
import zombie.radio.ZomboidRadio;
import zombie.randomizedWorld.randomizedBuilding.RBBar;
import zombie.randomizedWorld.randomizedBuilding.RBBasic;
import zombie.randomizedWorld.randomizedBuilding.RBBurnt;
import zombie.randomizedWorld.randomizedBuilding.RBBurntCorpse;
import zombie.randomizedWorld.randomizedBuilding.RBBurntFireman;
import zombie.randomizedWorld.randomizedBuilding.RBCafe;
import zombie.randomizedWorld.randomizedBuilding.RBClinic;
import zombie.randomizedWorld.randomizedBuilding.RBHairSalon;
import zombie.randomizedWorld.randomizedBuilding.RBKateAndBaldspot;
import zombie.randomizedWorld.randomizedBuilding.RBLooted;
import zombie.randomizedWorld.randomizedBuilding.RBOffice;
import zombie.randomizedWorld.randomizedBuilding.RBOther;
import zombie.randomizedWorld.randomizedBuilding.RBPileOCrepe;
import zombie.randomizedWorld.randomizedBuilding.RBPizzaWhirled;
import zombie.randomizedWorld.randomizedBuilding.RBSafehouse;
import zombie.randomizedWorld.randomizedBuilding.RBSchool;
import zombie.randomizedWorld.randomizedBuilding.RBShopLooted;
import zombie.randomizedWorld.randomizedBuilding.RBSpiffo;
import zombie.randomizedWorld.randomizedBuilding.RBStripclub;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.randomizedWorld.randomizedVehicleStory.RVSAmbulanceCrash;
import zombie.randomizedWorld.randomizedVehicleStory.RVSBanditRoad;
import zombie.randomizedWorld.randomizedVehicleStory.RVSBurntCar;
import zombie.randomizedWorld.randomizedVehicleStory.RVSCarCrash;
import zombie.randomizedWorld.randomizedVehicleStory.RVSCarCrashCorpse;
import zombie.randomizedWorld.randomizedVehicleStory.RVSChangingTire;
import zombie.randomizedWorld.randomizedVehicleStory.RVSConstructionSite;
import zombie.randomizedWorld.randomizedVehicleStory.RVSCrashHorde;
import zombie.randomizedWorld.randomizedVehicleStory.RVSFlippedCrash;
import zombie.randomizedWorld.randomizedVehicleStory.RVSPoliceBlockade;
import zombie.randomizedWorld.randomizedVehicleStory.RVSPoliceBlockadeShooting;
import zombie.randomizedWorld.randomizedVehicleStory.RVSTrailerCrash;
import zombie.randomizedWorld.randomizedVehicleStory.RVSUtilityVehicle;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedZoneStory.RZSBBQParty;
import zombie.randomizedWorld.randomizedZoneStory.RZSBaseball;
import zombie.randomizedWorld.randomizedZoneStory.RZSBeachParty;
import zombie.randomizedWorld.randomizedZoneStory.RZSBuryingCamp;
import zombie.randomizedWorld.randomizedZoneStory.RZSFishingTrip;
import zombie.randomizedWorld.randomizedZoneStory.RZSForestCamp;
import zombie.randomizedWorld.randomizedZoneStory.RZSForestCampEaten;
import zombie.randomizedWorld.randomizedZoneStory.RZSHunterCamp;
import zombie.randomizedWorld.randomizedZoneStory.RZSMusicFest;
import zombie.randomizedWorld.randomizedZoneStory.RZSMusicFestStage;
import zombie.randomizedWorld.randomizedZoneStory.RZSSexyTime;
import zombie.randomizedWorld.randomizedZoneStory.RZSTrapperCamp;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;
import zombie.savefile.ClientPlayerDB;
import zombie.savefile.PlayerDB;
import zombie.savefile.PlayerDBHelper;
import zombie.savefile.ServerPlayerDB;
import zombie.text.templating.TemplateText;
import zombie.ui.TutorialManager;
import zombie.util.AddCoopPlayer;
import zombie.util.SharedStrings;
import zombie.util.Type;
import zombie.vehicles.PolygonalMap2;
import zombie.vehicles.VehicleIDMap;
import zombie.vehicles.VehicleManager;
import zombie.vehicles.VehiclesDB2;
import zombie.world.WorldDictionary;
import zombie.world.WorldDictionaryException;
import zombie.world.moddata.GlobalModData;

public final class IsoWorld {
   private String weather = "sunny";
   public final IsoMetaGrid MetaGrid = new IsoMetaGrid();
   private final ArrayList<RandomizedBuildingBase> randomizedBuildingList = new ArrayList();
   private final ArrayList<RandomizedZoneStoryBase> randomizedZoneList = new ArrayList();
   private final ArrayList<RandomizedVehicleStoryBase> randomizedVehicleStoryList = new ArrayList();
   private final RandomizedBuildingBase RBBasic = new RBBasic();
   private final HashMap<String, ArrayList<Double>> spawnedZombieZone = new HashMap();
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
   static CompDistToPlayer compDistToPlayer = new CompDistToPlayer();
   public static String mapPath = "media/";
   public static boolean mapUseJar = true;
   boolean bLoaded = false;
   public static final HashMap<String, ArrayList<String>> PropertyValueMap = new HashMap();
   private static int WorldX = 0;
   private static int WorldY = 0;
   private SurvivorDesc luaDesc;
   private ArrayList<String> luatraits;
   private int luaSpawnCellX = -1;
   private int luaSpawnCellY = -1;
   private int luaPosX = -1;
   private int luaPosY = -1;
   private int luaPosZ = -1;
   public static final int WorldVersion = 195;
   public static final int WorldVersion_Barricade = 87;
   public static final int WorldVersion_SandboxOptions = 88;
   public static final int WorldVersion_FliesSound = 121;
   public static final int WorldVersion_LootRespawn = 125;
   public static final int WorldVersion_OverlappingGenerators = 127;
   public static final int WorldVersion_ItemContainerIdenticalItems = 128;
   public static final int WorldVersion_VehicleSirenStartTime = 129;
   public static final int WorldVersion_CompostLastUpdated = 130;
   public static final int WorldVersion_DayLengthHours = 131;
   public static final int WorldVersion_LampOnPillar = 132;
   public static final int WorldVersion_AlarmClockRingSince = 134;
   public static final int WorldVersion_ClimateAdded = 135;
   public static final int WorldVersion_VehicleLightFocusing = 135;
   public static final int WorldVersion_GeneratorFuelFloat = 138;
   public static final int WorldVersion_InfectionTime = 142;
   public static final int WorldVersion_ClimateColors = 143;
   public static final int WorldVersion_BodyLocation = 144;
   public static final int WorldVersion_CharacterModelData = 145;
   public static final int WorldVersion_CharacterModelData2 = 146;
   public static final int WorldVersion_CharacterModelData3 = 147;
   public static final int WorldVersion_HumanVisualBlood = 148;
   public static final int WorldVersion_ItemContainerIdenticalItemsInt = 149;
   public static final int WorldVersion_PerkName = 152;
   public static final int WorldVersion_Thermos = 153;
   public static final int WorldVersion_AllPatches = 155;
   public static final int WorldVersion_ZombieRotStage = 156;
   public static final int WorldVersion_NewSandboxLootModifier = 157;
   public static final int WorldVersion_KateBobStorm = 158;
   public static final int WorldVersion_DeadBodyAngle = 159;
   public static final int WorldVersion_ChunkSpawnedRooms = 160;
   public static final int WorldVersion_DeathDragDown = 161;
   public static final int WorldVersion_CanUpgradePerk = 162;
   public static final int WorldVersion_ItemVisualFullType = 164;
   public static final int WorldVersion_VehicleBlood = 165;
   public static final int WorldVersion_DeadBodyZombieRotStage = 166;
   public static final int WorldVersion_Fitness = 167;
   public static final int WorldVersion_DeadBodyFakeDead = 168;
   public static final int WorldVersion_Fitness2 = 169;
   public static final int WorldVersion_NewFog = 170;
   public static final int WorldVersion_DeadBodyPersistentOutfitID = 171;
   public static final int WorldVersion_VehicleTowingID = 172;
   public static final int WorldVersion_VehicleJNITransform = 173;
   public static final int WorldVersion_VehicleTowAttachment = 174;
   public static final int WorldVersion_ContainerMaxCapacity = 175;
   public static final int WorldVersion_TimedActionInstantCheat = 176;
   public static final int WorldVersion_ClothingPatchSaveLoad = 178;
   public static final int WorldVersion_AttachedSlotType = 179;
   public static final int WorldVersion_NoiseMakerDuration = 180;
   public static final int WorldVersion_ChunkVehicles = 91;
   public static final int WorldVersion_PlayerVehicleSeat = 91;
   public static final int WorldVersion_MediaDisksAndTapes = 181;
   public static final int WorldVersion_AlreadyReadBooks1 = 182;
   public static final int WorldVersion_LampOnPillar2 = 183;
   public static final int WorldVersion_AlreadyReadBooks2 = 184;
   public static final int WorldVersion_PolygonZone = 185;
   public static final int WorldVersion_PolylineZone = 186;
   public static final int WorldVersion_NaturalHairBeardColor = 187;
   public static final int WorldVersion_CruiseSpeedSaving = 188;
   public static final int WorldVersion_KnownMediaLines = 189;
   public static final int WorldVersion_DeadBodyAtlas = 190;
   public static final int WorldVersion_Scarecrow = 191;
   public static final int WorldVersion_DeadBodyID = 192;
   public static final int WorldVersion_IgnoreRemoveSandbox = 193;
   public static final int WorldVersion_MapMetaBounds = 194;
   public static final int WorldVersion_PreviouslyEntered = 195;
   public static int SavedWorldVersion = -1;
   private boolean bDrawWorld = true;
   private final ArrayList<IsoZombie> zombieWithModel = new ArrayList();
   private final ArrayList<IsoZombie> zombieWithoutModel = new ArrayList();
   public static boolean NoZombies = false;
   public static int TotalWorldVersion = -1;
   public static int saveoffsetx;
   public static int saveoffsety;
   public boolean bDoChunkMapUpdate = true;
   private long emitterUpdateMS;
   public boolean emitterUpdate;
   private int updateSafehousePlayers = 200;

   public IsoMetaGrid getMetaGrid() {
      return this.MetaGrid;
   }

   public IsoMetaGrid.Zone registerZone(String var1, String var2, int var3, int var4, int var5, int var6, int var7) {
      return this.MetaGrid.registerZone(var1, var2, var3, var4, var5, var6, var7);
   }

   public IsoMetaGrid.Zone registerZoneNoOverlap(String var1, String var2, int var3, int var4, int var5, int var6, int var7) {
      return this.MetaGrid.registerZoneNoOverlap(var1, var2, var3, var4, var5, var6, var7);
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

   public IsoMetaGrid.Zone registerVehiclesZone(String var1, String var2, int var3, int var4, int var5, int var6, int var7, KahluaTable var8) {
      return this.MetaGrid.registerVehiclesZone(var1, var2, var3, var4, var5, var6, var7, var8);
   }

   public IsoMetaGrid.Zone registerMannequinZone(String var1, String var2, int var3, int var4, int var5, int var6, int var7, KahluaTable var8) {
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

   public IsoObject getItemFromXYZIndexBuffer(ByteBuffer var1) {
      int var2 = var1.getInt();
      int var3 = var1.getInt();
      int var4 = var1.getInt();
      IsoGridSquare var5 = this.CurrentCell.getGridSquare(var2, var3, var4);
      if (var5 == null) {
         return null;
      } else {
         byte var6 = var1.get();
         return var6 >= 0 && var6 < var5.getObjects().size() ? (IsoObject)var5.getObjects().get(var6) : null;
      }
   }

   public IsoWorld() {
      if (!GameServer.bServer) {
      }

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
      DebugLog.log("tiledef: loading " + var2);
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

               ArrayList var57 = new ArrayList();
               HashMap var19 = new HashMap();
               int var20 = 0;
               int var21 = 0;
               int var22 = 0;
               int var23 = 0;
               HashSet var24 = new HashSet();
               int var25 = 0;

               label731:
               while(true) {
                  String var27;
                  String var28;
                  if (var25 >= var9) {
                     String var10001;
                     ArrayList var58;
                     if (var12) {
                        var58 = new ArrayList(var24);
                        Collections.sort(var58);
                        Iterator var59 = var58.iterator();

                        while(var59.hasNext()) {
                           var27 = (String)var59.next();
                           PrintStream var87 = System.out;
                           var10001 = var27.replaceAll(" ", "_").replaceAll("-", "_").replaceAll("'", "").replaceAll("\\.", "");
                           var87.println(var10001 + " = \"" + var27 + "\",");
                        }
                     }

                     if (var13) {
                        var58 = new ArrayList(var24);
                        Collections.sort(var58);
                        StringBuilder var60 = new StringBuilder();
                        Iterator var61 = var58.iterator();

                        while(var61.hasNext()) {
                           var28 = (String)var61.next();
                           if (Translator.getMoveableDisplayNameOrNull(var28) == null) {
                              var10001 = var28.replaceAll(" ", "_").replaceAll("-", "_").replaceAll("'", "").replaceAll("\\.", "");
                              var60.append(var10001 + " = \"" + var28 + "\",\n");
                           }
                        }

                        var27 = var60.toString();
                        if (!var27.isEmpty() && Core.bDebug) {
                           System.out.println("Missing translations in Moveables_EN.txt:\n" + var27);
                        }
                     }

                     if (var11) {
                        try {
                           this.saveMovableStats(var19, var3, var21, var22, var23, var20);
                        } catch (Exception var53) {
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

                     int var63 = readInt((InputStream)var6);

                     for(int var36 = 0; var36 < var63; ++var36) {
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
                           } else if (var37.equals("solidTrans")) {
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
                                 DebugLog.log("ERROR: WindowW sprite shouldn't have HoppableW (" + var34.getName() + ")");
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
                                 DebugLog.log("ERROR: WindowN sprite shouldn't have HoppableN (" + var34.getName() + ")");
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
                        DebugLog.General.error("Window sprite has SmashedTileOffset but no GlassRemovedOffset (" + var34.getName() + ")");
                     }
                  }

                  this.setOpenDoorProperties(var27, var14);
                  var15.clear();
                  Iterator var62 = var14.iterator();

                  while(true) {
                     while(true) {
                        String var65;
                        do {
                           if (!var62.hasNext()) {
                              var62 = var15.entrySet().iterator();

                              while(true) {
                                 while(true) {
                                    while(true) {
                                       String var42;
                                       int var43;
                                       ArrayList var66;
                                       boolean var68;
                                       int var69;
                                       Iterator var71;
                                       boolean var72;
                                       IsoSprite var77;
                                       do {
                                          if (!var62.hasNext()) {
                                             var14.clear();
                                             ++var25;
                                             continue label731;
                                          }

                                          Map.Entry var64 = (Map.Entry)var62.next();
                                          var65 = (String)var64.getKey();
                                          if (!var19.containsKey(var27)) {
                                             var19.put(var27, new ArrayList());
                                          }

                                          if (!((ArrayList)var19.get(var27)).contains(var65)) {
                                             ((ArrayList)var19.get(var27)).add(var65);
                                          }

                                          var66 = (ArrayList)var64.getValue();
                                          if (var66.size() == 1) {
                                             DebugLog.log("MOVABLES: Object has only one face defined for group: (" + var65 + ") sheet = " + var27);
                                          }

                                          if (var66.size() == 3) {
                                             DebugLog.log("MOVABLES: Object only has 3 sprites, _might_ have a error in settings, group: (" + var65 + ") sheet = " + var27);
                                          }

                                          String[] var67 = var17;
                                          int var70 = var17.length;

                                          for(var69 = 0; var69 < var70; ++var69) {
                                             String var75 = var67[var69];
                                             ((ArrayList)var16.get(var75)).clear();
                                          }

                                          var68 = ((IsoSprite)var66.get(0)).getProperties().Is("SpriteGridPos") && !((IsoSprite)var66.get(0)).getProperties().Val("SpriteGridPos").equals("None");
                                          var72 = true;
                                          var71 = var66.iterator();

                                          while(var71.hasNext()) {
                                             var77 = (IsoSprite)var71.next();
                                             boolean var41 = var77.getProperties().Is("SpriteGridPos") && !var77.getProperties().Val("SpriteGridPos").equals("None");
                                             if (var68 != var41) {
                                                var72 = false;
                                                DebugLog.log("MOVABLES: Difference in SpriteGrid settings for members of group: (" + var65 + ") sheet = " + var27);
                                                break;
                                             }

                                             if (!var77.getProperties().Is("Facing")) {
                                                var72 = false;
                                             } else {
                                                switch (var77.getProperties().Val("Facing")) {
                                                   case "N":
                                                      ((ArrayList)var16.get("N")).add(var77);
                                                      break;
                                                   case "E":
                                                      ((ArrayList)var16.get("E")).add(var77);
                                                      break;
                                                   case "S":
                                                      ((ArrayList)var16.get("S")).add(var77);
                                                      break;
                                                   case "W":
                                                      ((ArrayList)var16.get("W")).add(var77);
                                                      break;
                                                   default:
                                                      DebugLog.log("MOVABLES: Invalid face (" + var77.getProperties().Val("Facing") + ") for group: (" + var65 + ") sheet = " + var27);
                                                      var72 = false;
                                                }
                                             }

                                             if (!var72) {
                                                DebugLog.log("MOVABLES: Not all members have a valid face defined for group: (" + var65 + ") sheet = " + var27);
                                                break;
                                             }
                                          }
                                       } while(!var72);

                                       int var73;
                                       if (!var68) {
                                          if (var66.size() > 4) {
                                             DebugLog.log("MOVABLES: Object has too many faces defined for group: (" + var65 + ") sheet = " + var27);
                                          } else {
                                             String[] var74 = var17;
                                             var40 = var17.length;

                                             for(var73 = 0; var73 < var40; ++var73) {
                                                var42 = var74[var73];
                                                if (((ArrayList)var16.get(var42)).size() > 1) {
                                                   DebugLog.log("MOVABLES: " + var42 + " face defined more than once for group: (" + var65 + ") sheet = " + var27);
                                                   var72 = false;
                                                }
                                             }

                                             if (var72) {
                                                ++var22;
                                                var71 = var66.iterator();

                                                while(var71.hasNext()) {
                                                   var77 = (IsoSprite)var71.next();
                                                   String[] var80 = var17;
                                                   int var81 = var17.length;

                                                   for(var43 = 0; var43 < var81; ++var43) {
                                                      String var82 = var80[var43];
                                                      ArrayList var85 = (ArrayList)var16.get(var82);
                                                      if (var85.size() > 0 && var85.get(0) != var77) {
                                                         var77.getProperties().Set(var82 + "offset", Integer.toString(var14.indexOf(var85.get(0)) - var14.indexOf(var77)));
                                                      }
                                                   }
                                                }
                                             }
                                          }
                                       } else {
                                          var69 = 0;
                                          IsoSpriteGrid[] var79 = new IsoSpriteGrid[var17.length];

                                          int var44;
                                          IsoSprite var46;
                                          label701:
                                          for(var73 = 0; var73 < var17.length; ++var73) {
                                             ArrayList var76 = (ArrayList)var16.get(var17[var73]);
                                             if (var76.size() > 0) {
                                                if (var69 == 0) {
                                                   var69 = var76.size();
                                                }

                                                if (var69 != var76.size()) {
                                                   DebugLog.log("MOVABLES: Sprite count mismatch for multi sprite movable, group: (" + var65 + ") sheet = " + var27);
                                                   var72 = false;
                                                   break;
                                                }

                                                var57.clear();
                                                var43 = -1;
                                                var44 = -1;
                                                Iterator var45 = var76.iterator();

                                                while(true) {
                                                   String var47;
                                                   String[] var48;
                                                   int var49;
                                                   int var50;
                                                   if (var45.hasNext()) {
                                                      var46 = (IsoSprite)var45.next();
                                                      var47 = var46.getProperties().Val("SpriteGridPos");
                                                      if (!var57.contains(var47)) {
                                                         var57.add(var47);
                                                         var48 = var47.split(",");
                                                         if (var48.length == 2) {
                                                            var49 = Integer.parseInt(var48[0]);
                                                            var50 = Integer.parseInt(var48[1]);
                                                            if (var49 > var43) {
                                                               var43 = var49;
                                                            }

                                                            if (var50 > var44) {
                                                               var44 = var50;
                                                            }
                                                            continue;
                                                         }

                                                         DebugLog.log("MOVABLES: SpriteGrid position error for multi sprite movable, group: (" + var65 + ") sheet = " + var27);
                                                         var72 = false;
                                                      } else {
                                                         DebugLog.log("MOVABLES: double SpriteGrid position (" + var47 + ") for multi sprite movable, group: (" + var65 + ") sheet = " + var27);
                                                         var72 = false;
                                                      }
                                                   }

                                                   if (var43 == -1 || var44 == -1 || (var43 + 1) * (var44 + 1) != var76.size()) {
                                                      DebugLog.log("MOVABLES: SpriteGrid dimensions error for multi sprite movable, group: (" + var65 + ") sheet = " + var27);
                                                      var72 = false;
                                                      break label701;
                                                   }

                                                   if (!var72) {
                                                      break label701;
                                                   }

                                                   var79[var73] = new IsoSpriteGrid(var43 + 1, var44 + 1);
                                                   var45 = var76.iterator();

                                                   while(var45.hasNext()) {
                                                      var46 = (IsoSprite)var45.next();
                                                      var47 = var46.getProperties().Val("SpriteGridPos");
                                                      var48 = var47.split(",");
                                                      var49 = Integer.parseInt(var48[0]);
                                                      var50 = Integer.parseInt(var48[1]);
                                                      var79[var73].setSprite(var49, var50, var46);
                                                   }

                                                   if (!var79[var73].validate()) {
                                                      DebugLog.log("MOVABLES: SpriteGrid didn't validate for multi sprite movable, group: (" + var65 + ") sheet = " + var27);
                                                      var72 = false;
                                                      break label701;
                                                   }
                                                   break;
                                                }
                                             }
                                          }

                                          if (var72 && var69 != 0) {
                                             ++var23;

                                             for(var73 = 0; var73 < var17.length; ++var73) {
                                                IsoSpriteGrid var78 = var79[var73];
                                                if (var78 != null) {
                                                   IsoSprite[] var83 = var78.getSprites();
                                                   var44 = var83.length;

                                                   for(int var84 = 0; var84 < var44; ++var84) {
                                                      var46 = var83[var84];
                                                      var46.setSpriteGrid(var78);

                                                      for(int var86 = 0; var86 < var17.length; ++var86) {
                                                         if (var86 != var73 && var79[var86] != null) {
                                                            var46.getProperties().Set(var17[var86] + "offset", Integer.toString(var14.indexOf(var79[var86].getAnchorSprite()) - var14.indexOf(var46)));
                                                         }
                                                      }
                                                   }
                                                }
                                             }
                                          } else {
                                             DebugLog.log("MOVABLES: Error in multi sprite movable, group: (" + var65 + ") sheet = " + var27);
                                          }
                                       }
                                    }
                                 }
                              }
                           }

                           var34 = (IsoSprite)var62.next();
                           if (var34.getProperties().Is("StopCar")) {
                              var34.setType(IsoObjectType.isMoveAbleObject);
                           }
                        } while(!var34.getProperties().Is("IsMoveAble"));

                        if (var34.getProperties().Is("CustomName") && !var34.getProperties().Val("CustomName").equals("")) {
                           ++var20;
                           if (var34.getProperties().Is("GroupName")) {
                              String var10000 = var34.getProperties().Val("GroupName");
                              var65 = var10000 + " " + var34.getProperties().Val("CustomName");
                              if (!var15.containsKey(var65)) {
                                 var15.put(var65, new ArrayList());
                              }

                              ((ArrayList)var15.get(var65)).add(var34);
                              var24.add(var65);
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
                           DebugLog.log("[IMPORTANT] MOVABLES: Object has no custom name defined: sheet = " + var27);
                        }
                     }
                  }
               }
            } catch (Throwable var54) {
               try {
                  var6.close();
               } catch (Throwable var52) {
                  var54.addSuppressed(var52);
               }

               throw var54;
            }

            var6.close();
         } catch (Throwable var55) {
            try {
               var5.close();
            } catch (Throwable var51) {
               var55.addSuppressed(var51);
            }

            throw var55;
         }

         var5.close();
      } catch (Exception var56) {
         ExceptionLogger.logException(var56);
      }

   }

   private void GenerateTilePropertyLookupTables() {
      TilePropertyAliasMap.instance.Generate(PropertyValueMap);
      PropertyValueMap.clear();
   }

   public void LoadTileDefinitionsPropertyStrings(IsoSpriteManager var1, String var2, int var3) {
      DebugLog.log("tiledef: loading " + var2);
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

      for(int var2 = -96; var2 <= 96; ++var2) {
         String var3 = Integer.toString(var2);
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

            assert GameServer.bServer || !var12.CurrentAnim.Frames.isEmpty() && ((IsoDirectionFrame)var12.CurrentAnim.Frames.get(0)).getTexture(IsoDirections.N) != null;

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
      CellLoader.smashedWindowSpriteMap.clear();
      Iterator var1 = IsoSpriteManager.instance.NamedMap.values().iterator();

      while(true) {
         IsoSprite var2;
         PropertyContainer var3;
         do {
            if (!var1.hasNext()) {
               return;
            }

            var2 = (IsoSprite)var1.next();
            var3 = var2.getProperties();
         } while(!var3.Is(IsoFlagType.windowW) && !var3.Is(IsoFlagType.windowN));

         String var4 = var3.Val("SmashedTileOffset");
         if (var4 != null) {
            int var5 = PZMath.tryParseInt(var4, 0);
            if (var5 != 0) {
               IsoSprite var6 = IsoSprite.getSprite(IsoSpriteManager.instance, var2, var5);
               if (var6 != null) {
                  CellLoader.smashedWindowSpriteMap.put(var6, var2);
               }
            }
         }
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
               int var10 = -1;
               if (var6 == 80 && var7 == 76 && var8 == 89 && var9 == 82) {
                  var10 = SliceY.SliceBuffer.getInt();
               } else {
                  SliceY.SliceBuffer.rewind();
               }

               if (var10 >= 69) {
                  String var11 = GameWindow.ReadString(SliceY.SliceBuffer);
                  if (GameClient.bClient && var10 < 71) {
                     var11 = ServerOptions.instance.ServerPlayerID.getValue();
                  }

                  if (GameClient.bClient && !IsoPlayer.isServerPlayerIDValid(var11)) {
                     GameLoadingState.GameLoadingString = Translator.getText("IGUI_MP_ServerPlayerIDMismatch");
                     GameLoadingState.playerWrongIP = true;
                     return false;
                  }
               } else if (GameClient.bClient && ServerOptions.instance.ServerPlayerID.getValue().isEmpty()) {
                  GameLoadingState.GameLoadingString = Translator.getText("IGUI_MP_ServerPlayerIDMissing");
                  GameLoadingState.playerWrongIP = true;
                  return false;
               }

               WorldX = SliceY.SliceBuffer.getInt();
               WorldY = SliceY.SliceBuffer.getInt();
               IsoChunkMap.WorldXA = SliceY.SliceBuffer.getInt();
               IsoChunkMap.WorldYA = SliceY.SliceBuffer.getInt();
               IsoChunkMap.WorldZA = SliceY.SliceBuffer.getInt();
               IsoChunkMap.WorldXA += 300 * saveoffsetx;
               IsoChunkMap.WorldYA += 300 * saveoffsety;
               IsoChunkMap.SWorldX[0] = WorldX;
               IsoChunkMap.SWorldY[0] = WorldY;
               int[] var10000 = IsoChunkMap.SWorldX;
               var10000[0] += 30 * saveoffsetx;
               var10000 = IsoChunkMap.SWorldY;
               var10000[0] += 30 * saveoffsety;
               return true;
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
      int var4;
      if (!GameServer.bServer) {
         File var31 = ZomboidFileSystem.instance.getFileInCurrentSave("map_ver.bin");

         try {
            FileInputStream var34 = new FileInputStream(var31);

            try {
               DataInputStream var3 = new DataInputStream(var34);

               try {
                  var4 = var3.readInt();
                  if (var4 >= 25) {
                     String var5 = GameWindow.ReadString(var3);
                     if (!GameClient.bClient) {
                        Core.GameMap = var5;
                     }
                  }

                  if (var4 >= 74) {
                     this.setDifficulty(GameWindow.ReadString(var3));
                  }
               } catch (Throwable var28) {
                  try {
                     var3.close();
                  } catch (Throwable var25) {
                     var28.addSuppressed(var25);
                  }

                  throw var28;
               }

               var3.close();
            } catch (Throwable var29) {
               try {
                  var34.close();
               } catch (Throwable var24) {
                  var29.addSuppressed(var24);
               }

               throw var29;
            }

            var34.close();
         } catch (FileNotFoundException var30) {
         }
      }

      if (!GameServer.bServer || !GameServer.bSoftReset) {
         this.MetaGrid.CreateStep1();
      }

      LuaEventManager.triggerEvent("OnPreDistributionMerge");
      LuaEventManager.triggerEvent("OnDistributionMerge");
      LuaEventManager.triggerEvent("OnPostDistributionMerge");
      ItemPickerJava.Parse();
      VehiclesDB2.instance.init();
      LuaEventManager.triggerEvent("OnInitWorld");
      if (!GameClient.bClient) {
         SandboxOptions.instance.load();
      }

      this.bHydroPowerOn = GameTime.getInstance().NightsSurvived < SandboxOptions.getInstance().getElecShutModifier();
      ZomboidGlobals.toLua();
      ItemPickerJava.InitSandboxLootSettings();
      this.SurvivorDescriptors.clear();
      IsoSpriteManager.instance.Dispose();
      if (GameClient.bClient && ServerOptions.instance.DoLuaChecksum.getValue()) {
         try {
            NetChecksum.comparer.beginCompare();
            GameLoadingState.GameLoadingString = Translator.getText("IGUI_MP_Checksum");
            long var32 = System.currentTimeMillis();
            long var36 = var32;

            while(!GameClient.checksumValid) {
               if (GameWindow.bServerDisconnected) {
                  return;
               }

               if (System.currentTimeMillis() > var32 + 8000L) {
                  DebugLog.log("checksum: timed out waiting for the server to respond");
                  GameClient.connection.forceDisconnect("world-timeout-response");
                  GameWindow.bServerDisconnected = true;
                  GameWindow.kickReason = Translator.getText("UI_GameLoad_TimedOut");
                  return;
               }

               if (System.currentTimeMillis() > var36 + 1000L) {
                  DebugLog.log("checksum: waited one second");
                  var36 += 1000L;
               }

               NetChecksum.comparer.update();
               if (GameClient.checksumValid) {
                  break;
               }

               Thread.sleep(100L);
            }
         } catch (Exception var27) {
            var27.printStackTrace();
         }
      }

      GameLoadingState.GameLoadingString = Translator.getText("IGUI_MP_LoadTileDef");
      IsoSpriteManager var33 = IsoSpriteManager.instance;
      this.tileImages.clear();
      ZomboidFileSystem var35 = ZomboidFileSystem.instance;
      this.LoadTileDefinitionsPropertyStrings(var33, var35.getMediaPath("tiledefinitions.tiles"), 0);
      this.LoadTileDefinitionsPropertyStrings(var33, var35.getMediaPath("newtiledefinitions.tiles"), 1);
      this.LoadTileDefinitionsPropertyStrings(var33, var35.getMediaPath("tiledefinitions_erosion.tiles"), 2);
      this.LoadTileDefinitionsPropertyStrings(var33, var35.getMediaPath("tiledefinitions_apcom.tiles"), 3);
      this.LoadTileDefinitionsPropertyStrings(var33, var35.getMediaPath("tiledefinitions_overlays.tiles"), 4);
      this.LoadTileDefinitionsPropertyStrings(var33, var35.getMediaPath("tiledefinitions_noiseworks.patch.tiles"), -1);
      ZomboidFileSystem.instance.loadModTileDefPropertyStrings();
      this.SetCustomPropertyValues();
      this.GenerateTilePropertyLookupTables();
      this.LoadTileDefinitions(var33, var35.getMediaPath("tiledefinitions.tiles"), 0);
      this.LoadTileDefinitions(var33, var35.getMediaPath("newtiledefinitions.tiles"), 1);
      this.LoadTileDefinitions(var33, var35.getMediaPath("tiledefinitions_erosion.tiles"), 2);
      this.LoadTileDefinitions(var33, var35.getMediaPath("tiledefinitions_apcom.tiles"), 3);
      this.LoadTileDefinitions(var33, var35.getMediaPath("tiledefinitions_overlays.tiles"), 4);
      this.LoadTileDefinitions(var33, var35.getMediaPath("tiledefinitions_noiseworks.patch.tiles"), -1);
      this.JumboTreeDefinitions(var33, 5);
      ZomboidFileSystem.instance.loadModTileDefs();
      GameLoadingState.GameLoadingString = "";
      var33.AddSprite("media/ui/missing-tile.png");
      LuaEventManager.triggerEvent("OnLoadedTileDefinitions", var33);
      this.loadedTileDefinitions();
      if (GameServer.bServer && GameServer.bSoftReset) {
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
      WorldConverter.instance.convert(Core.GameSaveWorld, var33);
      if (!GameLoadingState.build23Stop) {
         SandboxOptions.instance.handleOldZombiesFile2();
         GameTime.getInstance().init();
         GameTime.getInstance().load();
         ImprovedFog.init();
         ZomboidRadio.getInstance().Init(SavedWorldVersion);
         GlobalModData.instance.init();
         if (GameServer.bServer && Core.getInstance().getPoisonousBerry() == null) {
            Core.getInstance().initPoisonousBerry();
         }

         if (GameServer.bServer && Core.getInstance().getPoisonousMushroom() == null) {
            Core.getInstance().initPoisonousMushroom();
         }

         ErosionGlobals.Boot(var33);
         WorldDictionary.init();
         WorldMarkers.instance.init();
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

         LuaEventManager.triggerEvent("OnLoadMapZones");
         this.MetaGrid.load();
         this.MetaGrid.loadZones();
         this.MetaGrid.processZones();
         LuaEventManager.triggerEvent("OnLoadedMapZones");
         if (GameServer.bServer) {
            ServerMap.instance.init(this.MetaGrid);
         }

         boolean var37 = false;
         boolean var38 = false;
         if (GameClient.bClient) {
            if (ClientPlayerDB.getInstance().clientLoadNetworkPlayer() && ClientPlayerDB.getInstance().isAliveMainNetworkPlayer()) {
               var38 = true;
            }
         } else {
            var38 = PlayerDBHelper.isPlayerAlive(ZomboidFileSystem.instance.getCurrentSaveDir(), 1);
         }

         if (GameServer.bServer) {
            ServerPlayerDB.setAllow(true);
         }

         if (!GameClient.bClient && !GameServer.bServer) {
            PlayerDB.setAllow(true);
         }

         boolean var39 = false;
         boolean var6 = false;
         boolean var7 = false;
         SafeHouse var9;
         int var41;
         if (var38) {
            var37 = true;
            if (!this.LoadPlayerForInfo()) {
               return;
            }

            WorldX = IsoChunkMap.SWorldX[IsoPlayer.getPlayerIndex()];
            WorldY = IsoChunkMap.SWorldY[IsoPlayer.getPlayerIndex()];
            var41 = IsoChunkMap.WorldXA;
            int var42 = IsoChunkMap.WorldYA;
            int var44 = IsoChunkMap.WorldZA;
         } else {
            var37 = false;
            if (GameClient.bClient && !ServerOptions.instance.SpawnPoint.getValue().isEmpty()) {
               String[] var8 = ServerOptions.instance.SpawnPoint.getValue().split(",");
               if (var8.length == 3) {
                  try {
                     IsoChunkMap.MPWorldXA = new Integer(var8[0].trim());
                     IsoChunkMap.MPWorldYA = new Integer(var8[1].trim());
                     IsoChunkMap.MPWorldZA = new Integer(var8[2].trim());
                  } catch (NumberFormatException var22) {
                     DebugLog.log("ERROR: SpawnPoint must be x,y,z, got \"" + ServerOptions.instance.SpawnPoint.getValue() + "\"");
                     IsoChunkMap.MPWorldXA = 0;
                     IsoChunkMap.MPWorldYA = 0;
                     IsoChunkMap.MPWorldZA = 0;
                  }
               } else {
                  DebugLog.log("ERROR: SpawnPoint must be x,y,z, got \"" + ServerOptions.instance.SpawnPoint.getValue() + "\"");
               }
            }

            if (this.getLuaSpawnCellX() < 0 || GameClient.bClient && (IsoChunkMap.MPWorldXA != 0 || IsoChunkMap.MPWorldYA != 0)) {
               if (GameClient.bClient) {
                  IsoChunkMap.WorldXA = IsoChunkMap.MPWorldXA;
                  IsoChunkMap.WorldYA = IsoChunkMap.MPWorldYA;
                  IsoChunkMap.WorldZA = IsoChunkMap.MPWorldZA;
                  WorldX = IsoChunkMap.WorldXA / 10;
                  WorldY = IsoChunkMap.WorldYA / 10;
               }
            } else {
               IsoChunkMap.WorldXA = this.getLuaPosX() + 300 * this.getLuaSpawnCellX();
               IsoChunkMap.WorldYA = this.getLuaPosY() + 300 * this.getLuaSpawnCellY();
               IsoChunkMap.WorldZA = this.getLuaPosZ();
               if (GameClient.bClient && ServerOptions.instance.SafehouseAllowRespawn.getValue()) {
                  for(int var45 = 0; var45 < SafeHouse.getSafehouseList().size(); ++var45) {
                     var9 = (SafeHouse)SafeHouse.getSafehouseList().get(var45);
                     if (var9.getPlayers().contains(GameClient.username) && var9.isRespawnInSafehouse(GameClient.username)) {
                        IsoChunkMap.WorldXA = var9.getX() + var9.getH() / 2;
                        IsoChunkMap.WorldYA = var9.getY() + var9.getW() / 2;
                        IsoChunkMap.WorldZA = 0;
                     }
                  }
               }

               WorldX = IsoChunkMap.WorldXA / 10;
               WorldY = IsoChunkMap.WorldYA / 10;
            }
         }

         Core.getInstance();
         KahluaTable var46 = (KahluaTable)LuaManager.env.rawget("selectedDebugScenario");
         int var10;
         int var11;
         int var12;
         if (var46 != null) {
            KahluaTable var47 = (KahluaTable)var46.rawget("startLoc");
            var10 = ((Double)var47.rawget("x")).intValue();
            var11 = ((Double)var47.rawget("y")).intValue();
            var12 = ((Double)var47.rawget("z")).intValue();
            IsoChunkMap.WorldXA = var10;
            IsoChunkMap.WorldYA = var11;
            IsoChunkMap.WorldZA = var12;
            WorldX = IsoChunkMap.WorldXA / 10;
            WorldY = IsoChunkMap.WorldYA / 10;
         }

         MapCollisionData.instance.init(instance.getMetaGrid());
         ZombiePopulationManager.instance.init(instance.getMetaGrid());
         PolygonalMap2.instance.init(instance.getMetaGrid());
         GlobalObjectLookup.init(instance.getMetaGrid());
         if (!GameServer.bServer) {
            SpawnPoints.instance.initSinglePlayer();
         }

         WorldStreamer.instance.create();
         this.CurrentCell = CellLoader.LoadCellBinaryChunk(var33, WorldX, WorldY);
         ClimateManager.getInstance().postCellLoadSetSnow();
         GameLoadingState.GameLoadingString = Translator.getText("IGUI_MP_LoadWorld");
         MapCollisionData.instance.start();
         MapItem.LoadWorldMap();

         while(WorldStreamer.instance.isBusy()) {
            try {
               Thread.sleep(100L);
            } catch (InterruptedException var21) {
               var21.printStackTrace();
            }
         }

         ArrayList var48 = new ArrayList();
         var48.addAll(IsoChunk.loadGridSquare);
         Iterator var49 = var48.iterator();

         while(var49.hasNext()) {
            IsoChunk var50 = (IsoChunk)var49.next();
            this.CurrentCell.ChunkMap[0].setChunkDirect(var50, false);
         }

         IsoChunk.bDoServerRequests = true;
         if (var37 && SystemDisabler.doPlayerCreation) {
            this.CurrentCell.LoadPlayer(SavedWorldVersion);
            if (GameClient.bClient) {
               IsoPlayer.getInstance().setUsername(GameClient.username);
            }

            ZomboidRadio.getInstance().getRecordedMedia().handleLegacyListenedLines(IsoPlayer.getInstance());
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
               String[] var13 = ServerOptions.instance.SpawnPoint.getValue().split(",");
               if (var13.length != 3) {
                  DebugLog.log("ERROR: SpawnPoint must be x,y,z, got \"" + ServerOptions.instance.SpawnPoint.getValue() + "\"");
               } else {
                  try {
                     int var14 = new Integer(var13[0].trim());
                     int var15 = new Integer(var13[1].trim());
                     int var16 = new Integer(var13[2].trim());
                     if (GameClient.bClient && ServerOptions.instance.SafehouseAllowRespawn.getValue()) {
                        for(int var17 = 0; var17 < SafeHouse.getSafehouseList().size(); ++var17) {
                           SafeHouse var18 = (SafeHouse)SafeHouse.getSafehouseList().get(var17);
                           if (var18.getPlayers().contains(GameClient.username) && var18.isRespawnInSafehouse(GameClient.username)) {
                              var14 = var18.getX() + var18.getH() / 2;
                              var15 = var18.getY() + var18.getW() / 2;
                              var16 = 0;
                           }
                        }
                     }

                     if (this.CurrentCell.getGridSquare(var14, var15, var16) != null) {
                        var10 = var14;
                        var11 = var15;
                        var12 = var16;
                     }
                  } catch (NumberFormatException var26) {
                     DebugLog.log("ERROR: SpawnPoint must be x,y,z, got \"" + ServerOptions.instance.SpawnPoint.getValue() + "\"");
                  }
               }
            }

            IsoGridSquare var51 = this.CurrentCell.getGridSquare(var10, var11, var12);
            if (SystemDisabler.doPlayerCreation && !GameServer.bServer) {
               if (var51 != null && var51.isFree(false) && var51.getRoom() != null) {
                  IsoGridSquare var52 = var51;
                  var51 = var51.getRoom().getFreeTile();
                  if (var51 == null) {
                     var51 = var52;
                  }
               }

               IsoPlayer var53 = null;
               if (this.getLuaPlayerDesc() != null) {
                  if (GameClient.bClient && ServerOptions.instance.SafehouseAllowRespawn.getValue()) {
                     var51 = this.CurrentCell.getGridSquare(IsoChunkMap.WorldXA, IsoChunkMap.WorldYA, IsoChunkMap.WorldZA);
                     if (var51 != null && var51.isFree(false) && var51.getRoom() != null) {
                        IsoGridSquare var54 = var51;
                        var51 = var51.getRoom().getFreeTile();
                        if (var51 == null) {
                           var51 = var54;
                        }
                     }
                  }

                  if (var51 == null) {
                     throw new RuntimeException("can't create player at x,y,z=" + var10 + "," + var11 + "," + var12 + " because the square is null");
                  }

                  WorldSimulation.instance.create();
                  var53 = new IsoPlayer(instance.CurrentCell, this.getLuaPlayerDesc(), var51.getX(), var51.getY(), var51.getZ());
                  if (GameClient.bClient) {
                     var53.setUsername(GameClient.username);
                  }

                  var53.setDir(IsoDirections.SE);
                  var53.sqlID = 1;
                  IsoPlayer.players[0] = var53;
                  IsoPlayer.setInstance(var53);
                  IsoCamera.CamCharacter = var53;
               }

               IsoPlayer var55 = IsoPlayer.getInstance();
               var55.applyTraits(this.getLuaTraits());
               ProfessionFactory.Profession var56 = ProfessionFactory.getProfession(var55.getDescriptor().getProfession());
               Iterator var57;
               String var58;
               if (var56 != null && !var56.getFreeRecipes().isEmpty()) {
                  var57 = var56.getFreeRecipes().iterator();

                  while(var57.hasNext()) {
                     var58 = (String)var57.next();
                     var55.getKnownRecipes().add(var58);
                  }
               }

               var57 = this.getLuaTraits().iterator();

               label341:
               while(true) {
                  TraitFactory.Trait var59;
                  do {
                     do {
                        if (!var57.hasNext()) {
                           if (var51 != null && var51.getRoom() != null) {
                              var51.getRoom().def.setExplored(true);
                              var51.getRoom().building.setAllExplored(true);
                              if (!GameServer.bServer && !GameClient.bClient) {
                                 ZombiePopulationManager.instance.playerSpawnedAt(var51.getX(), var51.getY(), var51.getZ());
                              }
                           }

                           var55.createKeyRing();
                           if (!GameClient.bClient) {
                              Core.getInstance().initPoisonousBerry();
                              Core.getInstance().initPoisonousMushroom();
                           }

                           LuaEventManager.triggerEvent("OnNewGame", var53, var51);
                           break label341;
                        }

                        var58 = (String)var57.next();
                        var59 = TraitFactory.getTrait(var58);
                     } while(var59 == null);
                  } while(var59.getFreeRecipes().isEmpty());

                  Iterator var19 = var59.getFreeRecipes().iterator();

                  while(var19.hasNext()) {
                     String var20 = (String)var19.next();
                     var55.getKnownRecipes().add(var20);
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
               int var40 = (int)IsoPlayer.getInstance().getX();
               var4 = (int)IsoPlayer.getInstance().getY();
               var41 = (int)IsoPlayer.getInstance().getZ();

               while(var41 > 0) {
                  IsoGridSquare var43 = this.CurrentCell.getGridSquare(var40, var4, var41);
                  if (var43 != null && var43.TreatAsSolidFloor()) {
                     break;
                  }

                  --var41;
                  IsoPlayer.getInstance().setZ((float)var41);
               }
            }

            IsoPlayer.getInstance().setCurrent(this.CurrentCell.getGridSquare((int)IsoPlayer.getInstance().getX(), (int)IsoPlayer.getInstance().getY(), (int)IsoPlayer.getInstance().getZ()));
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
         GameLoadingState.GameLoadingString = "";
         initMessaging();
         WorldDictionary.onWorldLoaded();
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
      IsoDeadBody.Reset();
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
      ZombieVocalsManager.Reset();
      IsoWaterFlow.Reset();
      this.MetaGrid.Dispose();
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

   public void render() {
      IsoWorld.s_performance.isoWorldRender.invokeAndMeasure(this, IsoWorld::renderInternal);
   }

   private void renderInternal() {
      if (this.bDrawWorld) {
         if (IsoCamera.CamCharacter != null) {
            SpriteRenderer.instance.doCoreIntParam(0, IsoCamera.CamCharacter.x);
            SpriteRenderer.instance.doCoreIntParam(1, IsoCamera.CamCharacter.y);
            SpriteRenderer.instance.doCoreIntParam(2, IsoCamera.CamCharacter.z);

            try {
               this.sceneCullZombies();
            } catch (Throwable var3) {
               ExceptionLogger.logException(var3);
            }

            try {
               WeatherFxMask.initMask();
               DeadBodyAtlas.instance.render();
               WorldItemAtlas.instance.render();
               this.CurrentCell.render();
               this.DrawIsoCursorHelper();
               DeadBodyAtlas.instance.renderDebug();
               PolygonalMap2.instance.render();
               WorldSoundManager.instance.render();
               WorldFlares.debugRender();
               WorldMarkers.instance.debugRender();
               ObjectAmbientEmitters.getInstance().render();
               ZombieVocalsManager.instance.render();
               LineDrawer.render();
               WeatherFxMask.renderFxMask(IsoCamera.frameState.playerIndex);
               if (GameClient.bClient) {
                  ClientServerMap.render(IsoCamera.frameState.playerIndex);
                  PassengerMap.render(IsoCamera.frameState.playerIndex);
               }

               SkyBox.getInstance().render();
            } catch (Throwable var2) {
               ExceptionLogger.logException(var2);
            }

         }
      }
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

   public void update() {
      IsoWorld.s_performance.isoWorldUpdate.invokeAndMeasure(this, IsoWorld::updateInternal);
   }

   private void updateInternal() {
      ++this.m_frameNo;

      try {
         if (GameServer.bServer) {
            VehicleManager.instance.serverUpdate();
         }
      } catch (Exception var8) {
         var8.printStackTrace();
      }

      WorldSimulation.instance.update();
      ImprovedFog.update();
      this.helicopter.update();
      long var1 = System.currentTimeMillis();
      if (var1 - this.emitterUpdateMS >= 30L) {
         this.emitterUpdateMS = var1;
         this.emitterUpdate = true;
      } else {
         this.emitterUpdate = false;
      }

      int var3;
      for(var3 = 0; var3 < this.currentEmitters.size(); ++var3) {
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
         IsoMetaCell var9 = this.MetaGrid.getCurrentCellData();
         if (var9 != null) {
            var9.checkTriggers();
         }
      }

      WorldSoundManager.instance.initFrame();
      ZombieGroupManager.instance.preupdate();
      OnceEvery.update();
      CollisionManager.instance.initUpdate();

      for(var3 = 0; var3 < this.CurrentCell.getBuildingList().size(); ++var3) {
         ((IsoBuilding)this.CurrentCell.getBuildingList().get(var3)).update();
      }

      ClimateManager.getInstance().update();
      ObjectRenderEffects.updateStatic();
      this.CurrentCell.update();
      IsoRegions.update();
      HaloTextHelper.update();
      CollisionManager.instance.ResolveContacts();

      for(var3 = 0; var3 < this.AddCoopPlayers.size(); ++var3) {
         AddCoopPlayer var10 = (AddCoopPlayer)this.AddCoopPlayers.get(var3);
         var10.update();
         if (var10.isFinished()) {
            this.AddCoopPlayers.remove(var3--);
         }
      }

      if (!GameServer.bServer) {
         IsoPlayer.UpdateRemovedEmitters();
      }

      try {
         if (PlayerDB.isAvailable()) {
            PlayerDB.getInstance().updateMain();
         }

         if (ClientPlayerDB.isAvailable()) {
            ClientPlayerDB.getInstance().updateMain();
         }

         VehiclesDB2.instance.updateMain();
      } catch (Exception var7) {
         ExceptionLogger.logException(var7);
      }

      if (this.updateSafehousePlayers > 0 && (GameServer.bServer || GameClient.bClient)) {
         --this.updateSafehousePlayers;
         if (this.updateSafehousePlayers == 0) {
            this.updateSafehousePlayers = 200;
            SafeHouse.updateSafehousePlayersConnected();
         }
      }

      m_animationRecorderDiscard = false;
   }

   public IsoCell getCell() {
      return this.CurrentCell;
   }

   private void PopulateCellWithSurvivors() {
   }

   public int getWorldSquareY() {
      return this.CurrentCell.ChunkMap[IsoPlayer.getPlayerIndex()].WorldY * 10;
   }

   public int getWorldSquareX() {
      return this.CurrentCell.ChunkMap[IsoPlayer.getPlayerIndex()].WorldX * 10;
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

   /** @deprecated */
   @Deprecated
   public void setGlobalTemperature(float var1) {
   }

   public String getWeather() {
      return this.weather;
   }

   public void setWeather(String var1) {
      this.weather = var1;
   }

   public int getLuaSpawnCellX() {
      return this.luaSpawnCellX;
   }

   public void setLuaSpawnCellX(int var1) {
      this.luaSpawnCellX = var1;
   }

   public int getLuaSpawnCellY() {
      return this.luaSpawnCellY;
   }

   public void setLuaSpawnCellY(int var1) {
      this.luaSpawnCellY = var1;
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

   public String getWorld() {
      return Core.GameSaveWorld;
   }

   public void transmitWeather() {
      if (GameServer.bServer) {
         GameServer.sendWeather();
      }
   }

   public boolean isValidSquare(int var1, int var2, int var3) {
      return var3 >= 0 && var3 < 8 ? this.MetaGrid.isValidSquare(var1, var2) : false;
   }

   public ArrayList<RandomizedZoneStoryBase> getRandomizedZoneList() {
      return this.randomizedZoneList;
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
      return 195;
   }

   public HashMap<String, ArrayList<Double>> getSpawnedZombieZone() {
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

   private static class CompDistToPlayer implements Comparator<IsoZombie> {
      public float px;
      public float py;

      private CompDistToPlayer() {
      }

      public int compare(IsoZombie var1, IsoZombie var2) {
         float var3 = IsoUtils.DistanceManhatten((float)((int)var1.x), (float)((int)var1.y), this.px, this.py);
         float var4 = IsoUtils.DistanceManhatten((float)((int)var2.x), (float)((int)var2.y), this.px, this.py);
         if (var3 < var4) {
            return -1;
         } else {
            return var3 > var4 ? 1 : 0;
         }
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

            this.xPos.add((int)var3.getX());
            this.yPos.add((int)var3.getY());
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
}
