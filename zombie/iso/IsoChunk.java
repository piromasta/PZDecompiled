package zombie.iso;

import gnu.trove.list.array.TLongArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.CRC32;
import zombie.ChunkMapFilenames;
import zombie.FliesSound;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.LoadGridsquarePerformanceWorkaround;
import zombie.LootRespawn;
import zombie.MapCollisionData;
import zombie.ReanimatedPlayers;
import zombie.SandboxOptions;
import zombie.SystemDisabler;
import zombie.VirtualZombieManager;
import zombie.WorldSoundManager;
import zombie.ZombieSpawnRecorder;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaEventManager;
import zombie.Lua.MapObjects;
import zombie.audio.FMODAmbientWalls;
import zombie.audio.ObjectAmbientEmitters;
import zombie.basements.Basements;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.RagdollBuilder;
import zombie.characters.animals.AnimalPopulationManager;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.physics.Bullet;
import zombie.core.physics.WorldSimulation;
import zombie.core.properties.PropertyContainer;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.core.utils.BoundedQueue;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.erosion.ErosionData;
import zombie.erosion.ErosionMain;
import zombie.globalObjects.SGlobalObjects;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.ItemSpawner;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.enums.ChunkGenerationStatus;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderCutaways;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.fboRenderChunk.FBORenderOcclusion;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.RainManager;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.worldgen.WGChunk;
import zombie.iso.worldgen.blending.BlendDirection;
import zombie.iso.zones.VehicleZone;
import zombie.iso.zones.Zone;
import zombie.network.ChunkChecksum;
import zombie.network.ClientChunkRequest;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.MPStatistics;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.pathfind.CollideWithObstaclesPoly;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.popman.ZombiePopulationManager;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.popman.animal.AnimalOwnershipManager;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.randomizedWorld.randomizedRanch.RandomizedRanchBase;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedVehicleStory.VehicleStorySpawnData;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.VehicleScript;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleType;
import zombie.vehicles.VehiclesDB2;
import zombie.vispoly.VisibilityPolygon2;

public final class IsoChunk {
   public static boolean bDoServerRequests = true;
   public int wx = 0;
   public int wy = 0;
   public IsoGridSquare[][] squares;
   public FliesSound.ChunkData corpseData;
   public final NearestWalls.ChunkData nearestWalls = new NearestWalls.ChunkData();
   private final FBORenderLevels[] m_renderLevels = new FBORenderLevels[4];
   private ArrayList<IsoGameCharacter.Location> generatorsTouchingThisChunk;
   private IsoChunkLevel[] levels = new IsoChunkLevel[1];
   public int maxLevel = 0;
   public int minLevel = 0;
   public final ArrayList<WorldSoundManager.WorldSound> SoundList = new ArrayList();
   private int m_treeCount = 0;
   private int m_numberOfWaterTiles = 0;
   public int lightingUpdateCounter = 0;
   private Zone m_scavengeZone = null;
   private final TLongArrayList m_spawnedRooms = new TLongArrayList();
   public IsoChunk next;
   public final CollideWithObstaclesPoly.ChunkData collision = new CollideWithObstaclesPoly.ChunkData();
   public int m_adjacentChunkLoadedCounter = 0;
   public VehicleStorySpawnData m_vehicleStorySpawnData;
   public Object m_loadVehiclesObject = null;
   public final ObjectAmbientEmitters.ChunkData m_objectEmitterData = new ObjectAmbientEmitters.ChunkData();
   public final FBORenderCutaways.ChunkLevelsData m_cutawayData = new FBORenderCutaways.ChunkLevelsData(this);
   public final VisibilityPolygon2.ChunkData m_vispolyData = new VisibilityPolygon2.ChunkData(this);
   private boolean blendingDoneFull;
   private boolean blendingDonePartial;
   private boolean[] blendingModified = new boolean[4];
   private byte[] blendingDepth;
   private boolean attachmentsDoneFull;
   private boolean[] attachmentsState;
   private static final boolean[] comparatorBool4 = new boolean[]{true, true, true, true};
   private static final boolean[] comparatorBool5 = new boolean[]{true, true, true, true, true};
   private EnumSet<ChunkGenerationStatus> chunkGenerationStatus;
   public long loadedFrame;
   public long renderFrame;
   private static int frameDelay = 0;
   private static final int maxFrameDelay = 5;
   public JobType jobType;
   public LotHeader lotheader;
   public final BoundedQueue<IsoFloorBloodSplat> FloorBloodSplats;
   public final ArrayList<IsoFloorBloodSplat> FloorBloodSplatsFade;
   private static final int MAX_BLOOD_SPLATS = 1000;
   private int nextSplatIndex;
   public static final byte[][] renderByIndex = new byte[][]{{1, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {1, 0, 0, 0, 0, 1, 0, 0, 0, 0}, {1, 0, 0, 1, 0, 0, 1, 0, 0, 0}, {1, 0, 0, 1, 0, 1, 0, 0, 1, 0}, {1, 0, 1, 0, 1, 0, 1, 0, 1, 0}, {1, 1, 0, 1, 1, 0, 1, 1, 0, 0}, {1, 1, 0, 1, 1, 0, 1, 1, 0, 1}, {1, 1, 1, 1, 0, 1, 1, 1, 1, 0}, {1, 1, 1, 1, 1, 1, 1, 1, 1, 0}, {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}};
   public final ArrayList<IsoChunkMap> refs;
   public boolean bLoaded;
   private boolean blam;
   private boolean addZombies;
   public ArrayList<IsoGridSquare> proceduralZombieSquares;
   private boolean bFixed2x;
   public final boolean[] lightCheck;
   public final boolean[] bLightingNeverDone;
   public final ArrayList<IsoRoomLight> roomLights;
   public final ArrayList<BaseVehicle> vehicles;
   public int lootRespawnHour;
   public static final short LB_PATHFIND = 2;
   public short loadedBits;
   private static final short INVALID_LOAD_ID = -1;
   private static short nextLoadID;
   private short loadID;
   private long hashCodeObjects;
   public int ObjectsSyncCount;
   private static int AddVehicles_ForTest_vtype = 0;
   private static int AddVehicles_ForTest_vskin = 0;
   private static int AddVehicles_ForTest_vrot = 0;
   private static final ArrayList<BaseVehicle> BaseVehicleCheckedVehicles = new ArrayList();
   private int minLevelPhysics;
   private int maxLevelPhysics;
   private static final int MAX_SHAPES = 4;
   private final int[] shapes;
   private static final byte[] bshapes = new byte[4];
   private static final ChunkGetter chunkGetter = new ChunkGetter();
   static final ArrayList<IsoGridSquare> newSquareList = new ArrayList();
   private boolean loadedPhysics;
   public final Object vehiclesForAddToWorldLock;
   public ArrayList<IsoGameCharacter> ragdollControllersForAddToWorld;
   public static final ConcurrentLinkedQueue<IsoChunk> loadGridSquare = new ConcurrentLinkedQueue();
   private static final int BLOCK_SIZE = 65536;
   private static ByteBuffer SliceBuffer = ByteBuffer.allocate(65536);
   private static ByteBuffer SliceBufferLoad = ByteBuffer.allocate(65536);
   public static final Object WriteLock = new Object();
   private static final ArrayList<RoomDef> tempRoomDefs = new ArrayList();
   private static final ArrayList<BuildingDef> tempBuildingDefs = new ArrayList();
   private static final ArrayList<IsoBuilding> tempBuildings = new ArrayList();
   private static final ArrayList<ChunkLock> Locks = new ArrayList();
   private static final Stack<ChunkLock> FreeLocks = new Stack();
   private static final SanityCheck sanityCheck = new SanityCheck();
   private static final CRC32 crcLoad = new CRC32();
   private static final CRC32 crcSave = new CRC32();
   private static String prefix = "map_";
   private ErosionData.Chunk erosion;
   private static final HashMap<String, String> Fix2xMap = new HashMap();
   public int randomID;
   public long revision;

   public void updateSounds() {
      synchronized(WorldSoundManager.instance.SoundList) {
         int var2 = this.SoundList.size();

         for(int var3 = 0; var3 < var2; ++var3) {
            WorldSoundManager.WorldSound var4 = (WorldSoundManager.WorldSound)this.SoundList.get(var3);
            if (var4 == null || var4.life <= 0) {
               this.SoundList.remove(var3);
               --var3;
               --var2;
            }
         }

      }
   }

   public boolean IsOnScreen(boolean var1) {
      float var8 = IsoUtils.XToScreen((float)(this.wx * 8), (float)(this.wy * 8), (float)this.minLevel, 0);
      float var9 = IsoUtils.YToScreen((float)(this.wx * 8), (float)(this.wy * 8), (float)this.minLevel, 0);
      float var10 = IsoUtils.XToScreen((float)(this.wx * 8 + 8), (float)(this.wy * 8), (float)this.minLevel, 0);
      float var11 = IsoUtils.YToScreen((float)(this.wx * 8 + 8), (float)(this.wy * 8), (float)this.minLevel, 0);
      float var12 = IsoUtils.XToScreen((float)(this.wx * 8 + 8), (float)(this.wy * 8 + 8), (float)this.minLevel, 0);
      float var13 = IsoUtils.YToScreen((float)(this.wx * 8 + 8), (float)(this.wy * 8 + 8), (float)this.minLevel, 0);
      float var14 = IsoUtils.XToScreen((float)(this.wx * 8), (float)(this.wy * 8 + 8), (float)this.minLevel, 0);
      float var15 = IsoUtils.YToScreen((float)(this.wx * 8), (float)(this.wy * 8 + 8), (float)this.minLevel, 0);
      float var4 = PZMath.min(var8, var10, var12, var14);
      float var5 = PZMath.max(var8, var10, var12, var14);
      float var6 = PZMath.min(var9, var11, var13, var15);
      float var7 = PZMath.max(var9, var11, var13, var15);
      var8 = IsoUtils.XToScreen((float)(this.wx * 8), (float)(this.wy * 8), (float)(this.maxLevel + 1), 0);
      var9 = IsoUtils.YToScreen((float)(this.wx * 8), (float)(this.wy * 8), (float)(this.maxLevel + 1), 0);
      var10 = IsoUtils.XToScreen((float)(this.wx * 8 + 8), (float)(this.wy * 8), (float)(this.maxLevel + 1), 0);
      var11 = IsoUtils.YToScreen((float)(this.wx * 8 + 8), (float)(this.wy * 8), (float)(this.maxLevel + 1), 0);
      var12 = IsoUtils.XToScreen((float)(this.wx * 8 + 8), (float)(this.wy * 8 + 8), (float)(this.maxLevel + 1), 0);
      var13 = IsoUtils.YToScreen((float)(this.wx * 8 + 8), (float)(this.wy * 8 + 8), (float)(this.maxLevel + 1), 0);
      var14 = IsoUtils.XToScreen((float)(this.wx * 8), (float)(this.wy * 8 + 8), (float)(this.maxLevel + 1), 0);
      var15 = IsoUtils.YToScreen((float)(this.wx * 8), (float)(this.wy * 8 + 8), (float)(this.maxLevel + 1), 0);
      var4 = PZMath.min(var4, var8, var10, var12, var14);
      var5 = PZMath.max(var5, var8, var10, var12, var14);
      var6 = PZMath.min(var6, var9, var11, var13, var15);
      var7 = PZMath.max(var7, var9, var11, var13, var15);
      var6 -= (float)FBORenderLevels.extraHeightForJumboTrees(this.minLevel, this.maxLevel);
      int var16 = IsoCamera.frameState.playerIndex;
      var9 = IsoCamera.frameState.OffX;
      var10 = IsoCamera.frameState.OffY;
      var4 -= var9;
      var6 -= var10;
      var5 -= var9;
      var7 -= var10;
      float var10000 = var5 - var4;
      var10000 = var7 - var6;
      byte var17 = 0;
      if (var5 <= (float)(0 - var17)) {
         return false;
      } else if (var7 <= (float)(0 - var17)) {
         return false;
      } else if (var4 >= (float)(IsoCamera.frameState.OffscreenWidth + var17)) {
         return false;
      } else {
         return !(var6 >= (float)(IsoCamera.frameState.OffscreenHeight + var17));
      }
   }

   public IsoChunk(IsoCell var1) {
      this.blendingDepth = new byte[]{BlendDirection.NORTH.defaultDepth, BlendDirection.SOUTH.defaultDepth, BlendDirection.WEST.defaultDepth, BlendDirection.EAST.defaultDepth};
      this.attachmentsDoneFull = true;
      this.attachmentsState = new boolean[]{true, true, true, true, true};
      this.chunkGenerationStatus = EnumSet.noneOf(ChunkGenerationStatus.class);
      this.jobType = IsoChunk.JobType.None;
      this.FloorBloodSplats = new BoundedQueue(1000);
      this.FloorBloodSplatsFade = new ArrayList();
      this.refs = new ArrayList();
      this.proceduralZombieSquares = new ArrayList();
      this.lightCheck = new boolean[4];
      this.bLightingNeverDone = new boolean[4];
      this.roomLights = new ArrayList();
      this.vehicles = new ArrayList();
      this.lootRespawnHour = -1;
      this.loadID = -1;
      this.ObjectsSyncCount = 0;
      this.minLevelPhysics = 1000;
      this.maxLevelPhysics = 1000;
      this.shapes = new int[4];
      this.loadedPhysics = false;
      this.vehiclesForAddToWorldLock = new Object();
      this.ragdollControllersForAddToWorld = null;
      this.levels[0] = IsoChunkLevel.alloc().init(this, this.minLevel);
      this.squares = new IsoGridSquare[1][];
      this.squares[0] = this.levels[0].squares;
      this.checkLightingLater_AllPlayers_OneLevel(this.levels[0].getLevel());

      for(int var2 = 0; var2 < 4; ++var2) {
         this.lightCheck[var2] = true;
         this.bLightingNeverDone[var2] = true;
      }

      MPStatistics.increaseRelevantChunk();
   }

   /** @deprecated */
   @Deprecated
   public long getHashCodeObjects() {
      this.recalcHashCodeObjects();
      return this.hashCodeObjects;
   }

   /** @deprecated */
   @Deprecated
   public void recalcHashCodeObjects() {
      long var1 = 0L;
      this.hashCodeObjects = var1;
   }

   /** @deprecated */
   @Deprecated
   public int hashCodeNoOverride() {
      return (int)this.hashCodeObjects;
   }

   public void checkLightingLater_AllPlayers_AllLevels() {
      Arrays.fill(this.lightCheck, true);

      for(int var1 = this.getMinLevel(); var1 <= this.getMaxLevel(); ++var1) {
         IsoChunkLevel var2 = this.getLevelData(var1);
         Arrays.fill(var2.lightCheck, true);
      }

   }

   public void checkLightingLater_AllPlayers_OneLevel(int var1) {
      IsoChunkLevel var2 = this.getLevelData(var1);
      if (var2 != null) {
         Arrays.fill(this.lightCheck, true);
         Arrays.fill(var2.lightCheck, true);
      }
   }

   public void checkLightingLater_OnePlayer_AllLevels(int var1) {
      this.lightCheck[var1] = true;

      for(int var2 = this.getMinLevel(); var2 <= this.getMaxLevel(); ++var2) {
         IsoChunkLevel var3 = this.getLevelData(var2);
         var3.lightCheck[var1] = true;
      }

   }

   public void checkLightingLater_OnePlayer_OneLevel(int var1, int var2) {
      IsoChunkLevel var3 = this.getLevelData(var2);
      if (var3 != null) {
         var3.lightCheck[var1] = true;
      }
   }

   public void addBloodSplat(float var1, float var2, float var3, int var4) {
      if (!(var1 < (float)(this.wx * 8)) && !(var1 >= (float)((this.wx + 1) * 8))) {
         if (!(var2 < (float)(this.wy * 8)) && !(var2 >= (float)((this.wy + 1) * 8))) {
            IsoGridSquare var5 = this.getGridSquare(PZMath.fastfloor(var1 - (float)(this.wx * 8)), PZMath.fastfloor(var2 - (float)(this.wy * 8)), PZMath.fastfloor(var3));
            if (var5 != null && var5.isSolidFloor()) {
               IsoFloorBloodSplat var6 = new IsoFloorBloodSplat(var1 - (float)(this.wx * 8), var2 - (float)(this.wy * 8), var3, var4, (float)GameTime.getInstance().getWorldAgeHours());
               if (var4 < 8) {
                  var6.index = ++this.nextSplatIndex;
                  if (this.nextSplatIndex >= 10) {
                     this.nextSplatIndex = 0;
                  }
               }

               if (this.FloorBloodSplats.isFull()) {
                  IsoFloorBloodSplat var7 = (IsoFloorBloodSplat)this.FloorBloodSplats.removeFirst();
                  var7.fade = PerformanceSettings.getLockFPS() * 5;
                  this.FloorBloodSplatsFade.add(var7);
               }

               this.FloorBloodSplats.add(var6);
               if (PerformanceSettings.FBORenderChunk && Thread.currentThread() == GameWindow.GameThread) {
                  this.invalidateRenderChunkLevel(var5.z, FBORenderChunk.DIRTY_BLOOD);
               }
            }

         }
      }
   }

   public void AddCorpses(int var1, int var2) {
      if (!IsoWorld.getZombiesDisabled() && !"Tutorial".equals(Core.GameMode)) {
         IsoMetaChunk var3 = IsoWorld.instance.getMetaChunk(var1, var2);
         if (var3 != null) {
            float var4 = var3.getZombieIntensity();
            var4 *= 0.1F;
            int var5 = 0;
            if (var4 < 1.0F) {
               if ((float)Rand.Next(100) < var4 * 100.0F) {
                  var5 = 1;
               }
            } else {
               var5 = Rand.Next(0, (int)var4);
            }

            if (var5 > 0) {
               IsoGridSquare var6 = null;
               int var7 = 0;

               int var9;
               do {
                  int var8 = Rand.Next(10);
                  var9 = Rand.Next(10);
                  var6 = this.getGridSquare(var8, var9, 0);
                  ++var7;
               } while(var7 < 100 && (var6 == null || !RandomizedBuildingBase.is2x2AreaClear(var6)));

               if (var7 == 100) {
                  return;
               }

               if (var6 != null) {
                  byte var20 = 14;
                  if (Rand.Next(10) == 0) {
                     var20 = 50;
                  }

                  if (Rand.Next(40) == 0) {
                     var20 = 100;
                  }

                  for(var9 = 0; var9 < var20; ++var9) {
                     float var10 = (float)Rand.Next(3000) / 1000.0F;
                     float var11 = (float)Rand.Next(3000) / 1000.0F;
                     --var10;
                     --var11;
                     this.addBloodSplat((float)var6.getX() + var10, (float)var6.getY() + var11, (float)var6.getZ(), Rand.Next(20));
                  }

                  boolean var21 = Rand.Next(15 - SandboxOptions.instance.TimeSinceApo.getValue()) == 0;
                  VirtualZombieManager.instance.choices.clear();
                  VirtualZombieManager.instance.choices.add(var6);
                  IsoZombie var22 = VirtualZombieManager.instance.createRealZombieAlways(Rand.Next(8), false);
                  if (var22 != null) {
                     var22.setX((float)var6.x);
                     var22.setY((float)var6.y);
                     var22.setFakeDead(false);
                     var22.setHealth(0.0F);
                     var22.upKillCount = false;
                     if (!var21) {
                        var22.dressInRandomOutfit();

                        for(int var23 = 0; var23 < 10; ++var23) {
                           var22.addHole((BloodBodyPartType)null);
                           var22.addBlood((BloodBodyPartType)null, false, true, false);
                           var22.addDirt((BloodBodyPartType)null, (Integer)null, false);
                        }

                        var22.DoCorpseInventory();
                     }

                     var22.setSkeleton(var21);
                     if (var21) {
                        var22.getHumanVisual().setSkinTextureIndex(2);
                     }

                     IsoDeadBody var24 = new IsoDeadBody(var22, true);
                     if (!var21 && Rand.Next(3) == 0) {
                        VirtualZombieManager.instance.createEatingZombies(var24, Rand.Next(1, 4));
                     } else if (var21 && Rand.Next(6) == 0) {
                        VirtualZombieManager.instance.createEatingZombies(var24, Rand.Next(1, 4));
                     } else if (!var21 && Rand.Next(10) == 0) {
                        var24.setFakeDead(true);
                        if (Rand.Next(5) == 0) {
                           var24.setCrawling(true);
                        }
                     }

                     int var12 = 300;
                     if (Objects.equals(var6.getSquareZombiesType(), "StreetPoor") || Objects.equals(var6.getZoneType(), "TrailerPark")) {
                        var12 /= 2;
                     }

                     if (Objects.equals(var6.getSquareZombiesType(), "Rich") || Objects.equals(var6.getLootZone(), "Rich")) {
                        var12 *= 2;
                     }

                     if (var6.getZ() < 0) {
                        var12 /= 2;
                     }

                     if (var6.canSpawnVermin() && Rand.Next(var12) < SandboxOptions.instance.getCurrentRatIndex()) {
                        int var13 = SandboxOptions.instance.getCurrentRatIndex() / 10;
                        if (Objects.equals(var6.getSquareZombiesType(), "StreetPoor") || Objects.equals(var6.getZoneType(), "TrailerPark")) {
                           var13 *= 2;
                        }

                        if (var13 < 1) {
                           var13 = 1;
                        }

                        if (var13 > 9) {
                           var13 = 9;
                        }

                        int var14 = Rand.Next(1, var13);
                        String var16 = "grey";
                        if (var6 != null && var6.getBuilding() != null && (var6.getBuilding().hasRoom("laboratory") || var6.getBuilding().hasRoom("classroom") || var6.getBuilding().hasRoom("secondaryclassroom") || Objects.equals(var6.getZombiesType(), "University")) && !Rand.NextBool(3)) {
                           var16 = "white";
                        }

                        IsoAnimal var15;
                        if (Rand.NextBool(2)) {
                           var15 = new IsoAnimal(IsoWorld.instance.getCell(), var6.getX(), var6.getY(), var6.getZ(), "rat", var16);
                        } else {
                           var15 = new IsoAnimal(IsoWorld.instance.getCell(), var6.getX(), var6.getY(), var6.getZ(), "ratfemale", var16);
                        }

                        var15.addToWorld();
                        var15.randomizeAge();
                        int var17;
                        if (var14 > 1) {
                           for(var17 = 1; var17 < var14; ++var17) {
                              IsoGridSquare var18 = var6.getAdjacentSquare(IsoDirections.getRandom());
                              if (var18 != null && var18.isFree(true) && var18.isSolidFloor()) {
                                 if (Rand.NextBool(2)) {
                                    var15 = new IsoAnimal(IsoWorld.instance.getCell(), var18.getX(), var18.getY(), var18.getZ(), "rat", var16);
                                 } else {
                                    var15 = new IsoAnimal(IsoWorld.instance.getCell(), var18.getX(), var18.getY(), var18.getZ(), "ratfemale", var16);
                                 }

                                 var15.addToWorld();
                                 var15.randomizeAge();
                                 if (Rand.NextBool(3)) {
                                    var15.setStateEventDelayTimer(0.0F);
                                 } else if (var18.canReachTo(var6)) {
                                    var15.fleeTo(var6);
                                 }
                              }
                           }
                        }

                        var17 = Rand.Next(0, var13);

                        for(int var25 = 0; var25 < var17; ++var25) {
                           IsoGridSquare var19 = var6.getAdjacentSquare(IsoDirections.getRandom());
                           if (var19 != null && var19.isFree(true) && var19.isSolidFloor()) {
                              this.addItemOnGround(var19, "Base.Dung_Rat");
                           }
                        }
                     }
                  }
               }
            }
         }

      }
   }

   public void AddBlood(int var1, int var2) {
      IsoMetaChunk var3 = IsoWorld.instance.getMetaChunk(var1, var2);
      if (var3 != null) {
         float var4 = var3.getZombieIntensity();
         var4 *= 0.1F;
         if (Rand.Next(40) == 0) {
            var4 += 10.0F;
         }

         int var5 = 0;
         if (var4 < 1.0F) {
            if ((float)Rand.Next(100) < var4 * 100.0F) {
               var5 = 1;
            }
         } else {
            var5 = Rand.Next(0, (int)var4);
         }

         if (var5 > 0) {
            VirtualZombieManager.instance.AddBloodToMap(var5, this);
         }
      }

   }

   private void checkVehiclePos(BaseVehicle var1, IsoChunk var2) {
      this.fixVehiclePos(var1, var2);
      IsoDirections var3 = var1.getDir();
      IsoGridSquare var4;
      switch (var3) {
         case E:
         case W:
            if (var1.getX() - (float)(var2.wx * 8) < var1.getScript().getExtents().x) {
               var4 = IsoWorld.instance.CurrentCell.getGridSquare((double)(var1.getX() - var1.getScript().getExtents().x), (double)var1.getY(), (double)var1.getZ());
               if (var4 == null) {
                  return;
               }

               this.fixVehiclePos(var1, var4.chunk);
            }

            if (var1.getX() - (float)(var2.wx * 8) > 8.0F - var1.getScript().getExtents().x) {
               var4 = IsoWorld.instance.CurrentCell.getGridSquare((double)(var1.getX() + var1.getScript().getExtents().x), (double)var1.getY(), (double)var1.getZ());
               if (var4 == null) {
                  return;
               }

               this.fixVehiclePos(var1, var4.chunk);
            }
            break;
         case N:
         case S:
            if (var1.getY() - (float)(var2.wy * 8) < var1.getScript().getExtents().z) {
               var4 = IsoWorld.instance.CurrentCell.getGridSquare((double)var1.getX(), (double)(var1.getY() - var1.getScript().getExtents().z), (double)var1.getZ());
               if (var4 == null) {
                  return;
               }

               this.fixVehiclePos(var1, var4.chunk);
            }

            if (var1.getY() - (float)(var2.wy * 8) > 8.0F - var1.getScript().getExtents().z) {
               var4 = IsoWorld.instance.CurrentCell.getGridSquare((double)var1.getX(), (double)(var1.getY() + var1.getScript().getExtents().z), (double)var1.getZ());
               if (var4 == null) {
                  return;
               }

               this.fixVehiclePos(var1, var4.chunk);
            }
      }

   }

   private boolean fixVehiclePos(BaseVehicle var1, IsoChunk var2) {
      BaseVehicle.MinMaxPosition var3 = var1.getMinMaxPosition();
      boolean var5 = false;
      IsoDirections var6 = var1.getDir();

      for(int var7 = 0; var7 < var2.vehicles.size(); ++var7) {
         BaseVehicle.MinMaxPosition var8 = ((BaseVehicle)var2.vehicles.get(var7)).getMinMaxPosition();
         float var4;
         switch (var6) {
            case E:
            case W:
               var4 = var8.minX - var3.maxX;
               if (var4 > 0.0F && var3.minY < var8.maxY && var3.maxY > var8.minY) {
                  var1.setX(var1.getX() - var4);
                  var3.minX -= var4;
                  var3.maxX -= var4;
                  var5 = true;
               } else {
                  var4 = var3.minX - var8.maxX;
                  if (var4 > 0.0F && var3.minY < var8.maxY && var3.maxY > var8.minY) {
                     var1.setX(var1.getX() + var4);
                     var3.minX += var4;
                     var3.maxX += var4;
                     var5 = true;
                  }
               }
               break;
            case N:
            case S:
               var4 = var8.minY - var3.maxY;
               if (var4 > 0.0F && var3.minX < var8.maxX && var3.maxX > var8.minX) {
                  var1.setY(var1.getY() - var4);
                  var3.minY -= var4;
                  var3.maxY -= var4;
                  var5 = true;
               } else {
                  var4 = var3.minY - var8.maxY;
                  if (var4 > 0.0F && var3.minX < var8.maxX && var3.maxX > var8.minX) {
                     var1.setY(var1.getY() + var4);
                     var3.minY += var4;
                     var3.maxY += var4;
                     var5 = true;
                  }
               }
         }
      }

      return var5;
   }

   private boolean isGoodVehiclePos(BaseVehicle var1, IsoChunk var2) {
      int var3 = (PZMath.fastfloor(var1.getX()) - 4) / 8 - 1;
      int var4 = (PZMath.fastfloor(var1.getY()) - 4) / 8 - 1;
      int var5 = (int)Math.ceil((double)((var1.getX() + 4.0F) / 8.0F)) + 1;
      int var6 = (int)Math.ceil((double)((var1.getY() + 4.0F) / 8.0F)) + 1;

      for(int var7 = var4; var7 < var6; ++var7) {
         for(int var8 = var3; var8 < var5; ++var8) {
            IsoChunk var9 = GameServer.bServer ? ServerMap.instance.getChunk(var8, var7) : IsoWorld.instance.CurrentCell.getChunkForGridSquare(var8 * 8, var7 * 8, 0);
            if (var9 != null) {
               for(int var10 = 0; var10 < var9.vehicles.size(); ++var10) {
                  BaseVehicle var11 = (BaseVehicle)var9.vehicles.get(var10);
                  if (PZMath.fastfloor(var11.getZ()) == PZMath.fastfloor(var1.getZ()) && var1.testCollisionWithVehicle(var11)) {
                     return false;
                  }
               }
            }
         }
      }

      return true;
   }

   private void AddVehicles_ForTest(Zone var1) {
      int var2;
      for(var2 = var1.y - this.wy * 8 + 3; var2 < 0; var2 += 6) {
      }

      int var3;
      for(var3 = var1.x - this.wx * 8 + 2; var3 < 0; var3 += 5) {
      }

      for(int var4 = var2; var4 < 8 && this.wy * 8 + var4 < var1.y + var1.h; var4 += 6) {
         for(int var5 = var3; var5 < 8 && this.wx * 8 + var5 < var1.x + var1.w; var5 += 5) {
            IsoGridSquare var6 = this.getGridSquare(var5, var4, 0);
            if (var6 != null) {
               BaseVehicle var7 = new BaseVehicle(IsoWorld.instance.CurrentCell);
               var7.setZone("Test");
               switch (AddVehicles_ForTest_vtype) {
                  case 0:
                     var7.setScriptName("Base.CarNormal");
                     break;
                  case 1:
                     var7.setScriptName("Base.SmallCar");
                     break;
                  case 2:
                     var7.setScriptName("Base.SmallCar02");
                     break;
                  case 3:
                     var7.setScriptName("Base.CarTaxi");
                     break;
                  case 4:
                     var7.setScriptName("Base.CarTaxi2");
                     break;
                  case 5:
                     var7.setScriptName("Base.PickUpTruck");
                     break;
                  case 6:
                     var7.setScriptName("Base.PickUpVan");
                     break;
                  case 7:
                     var7.setScriptName("Base.CarStationWagon");
                     break;
                  case 8:
                     var7.setScriptName("Base.CarStationWagon2");
                     break;
                  case 9:
                     var7.setScriptName("Base.VanSeats");
                     break;
                  case 10:
                     var7.setScriptName("Base.Van");
                     break;
                  case 11:
                     var7.setScriptName("Base.StepVan");
                     break;
                  case 12:
                     var7.setScriptName("Base.PickUpTruck");
                     break;
                  case 13:
                     var7.setScriptName("Base.PickUpVan");
                     break;
                  case 14:
                     var7.setScriptName("Base.CarStationWagon");
                     break;
                  case 15:
                     var7.setScriptName("Base.CarStationWagon2");
                     break;
                  case 16:
                     var7.setScriptName("Base.VanSeats");
                     break;
                  case 17:
                     var7.setScriptName("Base.Van");
                     break;
                  case 18:
                     var7.setScriptName("Base.StepVan");
                     break;
                  case 19:
                     var7.setScriptName("Base.SUV");
                     break;
                  case 20:
                     var7.setScriptName("Base.OffRoad");
                     break;
                  case 21:
                     var7.setScriptName("Base.ModernCar");
                     break;
                  case 22:
                     var7.setScriptName("Base.ModernCar02");
                     break;
                  case 23:
                     var7.setScriptName("Base.CarLuxury");
                     break;
                  case 24:
                     var7.setScriptName("Base.SportsCar");
                     break;
                  case 25:
                     var7.setScriptName("Base.PickUpVanLightsPolice");
                     break;
                  case 26:
                     var7.setScriptName("Base.CarLightsPolice");
                     break;
                  case 27:
                     var7.setScriptName("Base.PickUpVanLightsFire");
                     break;
                  case 28:
                     var7.setScriptName("Base.PickUpTruckLightsFire");
                     break;
                  case 29:
                     var7.setScriptName("Base.PickUpVanLightsFossoil");
                     break;
                  case 30:
                     var7.setScriptName("Base.PickUpTruckLightsFossoil");
                     break;
                  case 31:
                     var7.setScriptName("Base.CarLightsRanger");
                     break;
                  case 32:
                     var7.setScriptName("Base.StepVanMail");
                     break;
                  case 33:
                     var7.setScriptName("Base.VanSpiffo");
                     break;
                  case 34:
                     var7.setScriptName("Base.VanAmbulance");
                     break;
                  case 35:
                     var7.setScriptName("Base.VanRadio");
                     break;
                  case 36:
                     var7.setScriptName("Base.PickupBurnt");
                     break;
                  case 37:
                     var7.setScriptName("Base.CarNormalBurnt");
                     break;
                  case 38:
                     var7.setScriptName("Base.TaxiBurnt");
                     break;
                  case 39:
                     var7.setScriptName("Base.ModernCarBurnt");
                     break;
                  case 40:
                     var7.setScriptName("Base.ModernCar02Burnt");
                     break;
                  case 41:
                     var7.setScriptName("Base.SportsCarBurnt");
                     break;
                  case 42:
                     var7.setScriptName("Base.SmallCarBurnt");
                     break;
                  case 43:
                     var7.setScriptName("Base.SmallCar02Burnt");
                     break;
                  case 44:
                     var7.setScriptName("Base.VanSeatsBurnt");
                     break;
                  case 45:
                     var7.setScriptName("Base.VanBurnt");
                     break;
                  case 46:
                     var7.setScriptName("Base.SUVBurnt");
                     break;
                  case 47:
                     var7.setScriptName("Base.OffRoadBurnt");
                     break;
                  case 48:
                     var7.setScriptName("Base.PickUpVanLightsBurnt");
                     break;
                  case 49:
                     var7.setScriptName("Base.AmbulanceBurnt");
                     break;
                  case 50:
                     var7.setScriptName("Base.VanRadioBurnt");
                     break;
                  case 51:
                     var7.setScriptName("Base.PickupSpecialBurnt");
                     break;
                  case 52:
                     var7.setScriptName("Base.NormalCarBurntPolice");
                     break;
                  case 53:
                     var7.setScriptName("Base.LuxuryCarBurnt");
                     break;
                  case 54:
                     var7.setScriptName("Base.PickUpVanBurnt");
                     break;
                  case 55:
                     var7.setScriptName("Base.PickUpTruckMccoy");
                     break;
                  case 56:
                     var7.setScriptName("Base.PickUpTruckLightsRanger");
                     break;
                  case 57:
                     var7.setScriptName("Base.PickUpVanLightsRanger");
               }

               var7.setDir(IsoDirections.W);
               double var8 = (double)(var7.getDir().toAngle() + 3.1415927F) % 6.283185307179586;
               var7.savedRot.setAngleAxis(var8, 0.0, 1.0, 0.0);
               if (AddVehicles_ForTest_vrot == 1) {
                  var7.savedRot.setAngleAxis(1.5707963267948966, 0.0, 0.0, 1.0);
               }

               if (AddVehicles_ForTest_vrot == 2) {
                  var7.savedRot.setAngleAxis(3.141592653589793, 0.0, 0.0, 1.0);
               }

               var7.jniTransform.setRotation(var7.savedRot);
               var7.setX((float)var6.x);
               var7.setY((float)var6.y + 3.0F - 3.0F);
               var7.setZ((float)var6.z);
               var7.jniTransform.origin.set(var7.getX() - WorldSimulation.instance.offsetX, var7.getZ(), var7.getY() - WorldSimulation.instance.offsetY);
               var7.setScript();
               this.checkVehiclePos(var7, this);
               this.vehicles.add(var7);
               var7.setSkinIndex(AddVehicles_ForTest_vskin);
               ++AddVehicles_ForTest_vrot;
               if (AddVehicles_ForTest_vrot >= 2) {
                  AddVehicles_ForTest_vrot = 0;
                  ++AddVehicles_ForTest_vskin;
                  if (AddVehicles_ForTest_vskin >= var7.getSkinCount()) {
                     AddVehicles_ForTest_vtype = (AddVehicles_ForTest_vtype + 1) % 56;
                     AddVehicles_ForTest_vskin = 0;
                  }
               }
            }
         }
      }

   }

   private void AddVehicles_OnZone(VehicleZone var1, String var2) {
      IsoDirections var3 = IsoDirections.N;
      byte var4 = 3;
      byte var5 = 4;
      if ((var1.w == var5 || var1.w == var5 + 1 || var1.w == var5 + 2) && (var1.h <= var4 || var1.h >= var5 + 2)) {
         var3 = IsoDirections.W;
      }

      var5 = 5;
      if (var1.dir != IsoDirections.Max) {
         var3 = var1.dir;
      }

      if (var3 != IsoDirections.N && var3 != IsoDirections.S) {
         var5 = 3;
         var4 = 5;
      }

      byte var6 = 8;

      float var7;
      for(var7 = (float)(var1.y - this.wy * 8) + (float)var5 / 2.0F; var7 < 0.0F; var7 += (float)var5) {
      }

      float var8;
      for(var8 = (float)(var1.x - this.wx * 8) + (float)var4 / 2.0F; var8 < 0.0F; var8 += (float)var4) {
      }

      float var9 = var7;

      label214:
      while(true) {
         if (var9 < 8.0F && (float)(this.wy * 8) + var9 < (float)(var1.y + var1.h)) {
            float var10 = var8;

            while(true) {
               label207: {
                  if (var10 < 8.0F && (float)(this.wx * 8) + var10 < (float)(var1.x + var1.w)) {
                     IsoGridSquare var11 = this.getGridSquare(PZMath.fastfloor(var10), PZMath.fastfloor(var9), 0);
                     if (var11 == null) {
                        break label207;
                     }

                     VehicleType var12 = VehicleType.getRandomVehicleType(var2);
                     if (var12 != null) {
                        int var13 = var12.spawnRate;
                        switch (SandboxOptions.instance.CarSpawnRate.getValue()) {
                           case 1:
                           case 4:
                           default:
                              break;
                           case 2:
                              var13 = (int)Math.ceil((double)((float)var13 / 10.0F));
                              break;
                           case 3:
                              var13 = (int)Math.ceil((double)((float)var13 / 1.5F));
                              break;
                           case 5:
                              var13 *= 2;
                        }

                        if (SystemDisabler.doVehiclesEverywhere || DebugOptions.instance.VehicleSpawnEverywhere.getValue()) {
                           var13 = 100;
                        }

                        if (Rand.Next(100) <= var13) {
                           BaseVehicle var14 = new BaseVehicle(IsoWorld.instance.CurrentCell);
                           var14.setZone(var2);
                           var14.setVehicleType(var12.name);
                           if (var12.isSpecialCar) {
                              var14.setDoColor(false);
                           }

                           if (!this.RandomizeModel(var14, var1, var2, var12)) {
                              System.out.println("Problem with Vehicle spawning: " + var2 + " " + var12);
                              return;
                           }

                           byte var15 = 15;
                           switch (SandboxOptions.instance.CarAlarm.getValue()) {
                              case 1:
                                 var15 = -1;
                                 break;
                              case 2:
                                 var15 = 3;
                                 break;
                              case 3:
                                 var15 = 8;
                              case 4:
                              default:
                                 break;
                              case 5:
                                 var15 = 25;
                                 break;
                              case 6:
                                 var15 = 50;
                           }

                           boolean var16 = var14.getScriptName().toLowerCase().contains("burnt") || var14.getScriptName().toLowerCase().contains("smashed");
                           if (Rand.Next(100) < var15 && !var16) {
                              var14.setAlarmed(true);
                           }

                           if (var1.isFaceDirection()) {
                              var14.setDir(var3);
                           } else if (var3 != IsoDirections.N && var3 != IsoDirections.S) {
                              var14.setDir(Rand.Next(2) == 0 ? IsoDirections.W : IsoDirections.E);
                           } else {
                              var14.setDir(Rand.Next(2) == 0 ? IsoDirections.N : IsoDirections.S);
                           }

                           float var17;
                           for(var17 = var14.getDir().toAngle() + 3.1415927F; (double)var17 > 6.283185307179586; var17 = (float)((double)var17 - 6.283185307179586)) {
                           }

                           if (var12.randomAngle) {
                              var17 = Rand.Next(0.0F, 6.2831855F);
                           }

                           var14.savedRot.setAngleAxis(var17, 0.0F, 1.0F, 0.0F);
                           var14.jniTransform.setRotation(var14.savedRot);
                           float var18 = var14.getScript().getExtents().z;
                           float var19 = 0.5F;
                           float var20 = (float)var11.x + 0.5F;
                           float var21 = (float)var11.y + 0.5F;
                           if (var3 == IsoDirections.N) {
                              var20 = (float)var11.x + (float)var4 / 2.0F - (float)((int)((float)var4 / 2.0F));
                              var21 = (float)var1.y + var18 / 2.0F + var19;
                              if (var21 >= (float)(var11.y + 1) && PZMath.fastfloor(var9) < var6 - 1 && this.getGridSquare(PZMath.fastfloor(var10), PZMath.fastfloor(var9) + 1, 0) != null) {
                                 var11 = this.getGridSquare(PZMath.fastfloor(var10), PZMath.fastfloor(var9) + 1, 0);
                              }
                           } else if (var3 == IsoDirections.S) {
                              var20 = (float)var11.x + (float)var4 / 2.0F - (float)((int)((float)var4 / 2.0F));
                              var21 = (float)(var1.y + var1.h) - var18 / 2.0F - var19;
                              if (var21 < (float)var11.y && PZMath.fastfloor(var9) > 0 && this.getGridSquare(PZMath.fastfloor(var10), PZMath.fastfloor(var9) - 1, 0) != null) {
                                 var11 = this.getGridSquare(PZMath.fastfloor(var10), PZMath.fastfloor(var9) - 1, 0);
                              }
                           } else if (var3 == IsoDirections.W) {
                              var20 = (float)var1.x + var18 / 2.0F + var19;
                              var21 = (float)var11.y + (float)var5 / 2.0F - (float)((int)((float)var5 / 2.0F));
                              if (var20 >= (float)(var11.x + 1) && PZMath.fastfloor(var10) < var6 - 1 && this.getGridSquare(PZMath.fastfloor(var10) + 1, PZMath.fastfloor(var9), 0) != null) {
                                 var11 = this.getGridSquare(PZMath.fastfloor(var10) + 1, PZMath.fastfloor(var9), 0);
                              }
                           } else if (var3 == IsoDirections.E) {
                              var20 = (float)(var1.x + var1.w) - var18 / 2.0F - var19;
                              var21 = (float)var11.y + (float)var5 / 2.0F - (float)((int)((float)var5 / 2.0F));
                              if (var20 < (float)var11.x && PZMath.fastfloor(var10) > 0 && this.getGridSquare(PZMath.fastfloor(var10) - 1, PZMath.fastfloor(var9), 0) != null) {
                                 var11 = this.getGridSquare(PZMath.fastfloor(var10) - 1, PZMath.fastfloor(var9), 0);
                              }
                           }

                           if (var20 < (float)var11.x + 0.005F) {
                              var20 = (float)var11.x + 0.005F;
                           }

                           if (var20 > (float)(var11.x + 1) - 0.005F) {
                              var20 = (float)(var11.x + 1) - 0.005F;
                           }

                           if (var21 < (float)var11.y + 0.005F) {
                              var21 = (float)var11.y + 0.005F;
                           }

                           if (var21 > (float)(var11.y + 1) - 0.005F) {
                              var21 = (float)(var11.y + 1) - 0.005F;
                           }

                           var14.setX(var20);
                           var14.setY(var21);
                           var14.setZ((float)var11.z);
                           var14.jniTransform.origin.set(var14.getX() - WorldSimulation.instance.offsetX, var14.getZ(), var14.getY() - WorldSimulation.instance.offsetY);
                           float var22 = 100.0F - Math.min(var12.baseVehicleQuality * 120.0F, 100.0F);
                           var14.rust = (float)Rand.Next(100) < var22 ? 1.0F : 0.0F;
                           if (doSpawnedVehiclesInInvalidPosition(var14) || GameClient.bClient) {
                              this.vehicles.add(var14);
                           }

                           if (var12.chanceOfOverCar > 0 && Rand.Next(100) <= var12.chanceOfOverCar) {
                              this.spawnVehicleRandomAngle(var11, var1, var2);
                           }
                        }
                        break label207;
                     }

                     System.out.println("Can't find car: " + var2);
                  }

                  var9 += (float)var5;
                  continue label214;
               }

               var10 += (float)var4;
            }
         }

         return;
      }
   }

   private void AddVehicles_OnZonePolyline(VehicleZone var1, String var2) {
      byte var3 = 5;
      Vector2 var4 = new Vector2();

      for(int var5 = 0; var5 < var1.points.size() - 2; var5 += 2) {
         int var6 = var1.points.getQuick(var5);
         int var7 = var1.points.getQuick(var5 + 1);
         int var8 = var1.points.getQuick((var5 + 2) % var1.points.size());
         int var9 = var1.points.getQuick((var5 + 3) % var1.points.size());
         var4.set((float)(var8 - var6), (float)(var9 - var7));

         for(float var10 = (float)var3 / 2.0F; var10 < var4.getLength(); var10 += (float)var3) {
            float var11 = (float)var6 + var4.x / var4.getLength() * var10;
            float var12 = (float)var7 + var4.y / var4.getLength() * var10;
            if (var11 >= (float)(this.wx * 8) && var12 >= (float)(this.wy * 8) && var11 < (float)((this.wx + 1) * 8) && var12 < (float)((this.wy + 1) * 8)) {
               VehicleType var13 = VehicleType.getRandomVehicleType(var2);
               if (var13 == null) {
                  System.out.println("Can't find car: " + var2);
                  return;
               }

               BaseVehicle var14 = new BaseVehicle(IsoWorld.instance.CurrentCell);
               var14.setZone(var2);
               var14.setVehicleType(var13.name);
               if (var13.isSpecialCar) {
                  var14.setDoColor(false);
               }

               if (!this.RandomizeModel(var14, var1, var2, var13)) {
                  System.out.println("Problem with Vehicle spawning: " + var2 + " " + var13);
                  return;
               }

               byte var15 = 15;
               switch (SandboxOptions.instance.CarAlarm.getValue()) {
                  case 1:
                     var15 = -1;
                     break;
                  case 2:
                     var15 = 3;
                     break;
                  case 3:
                     var15 = 8;
                  case 4:
                  default:
                     break;
                  case 5:
                     var15 = 25;
                     break;
                  case 6:
                     var15 = 50;
               }

               if (Rand.Next(100) < var15) {
                  var14.setAlarmed(true);
               }

               float var16 = var4.x;
               float var17 = var4.y;
               var4.normalize();
               var14.setDir(IsoDirections.fromAngle(var4));

               float var18;
               for(var18 = var4.getDirectionNeg() + 0.0F; (double)var18 > 6.283185307179586; var18 = (float)((double)var18 - 6.283185307179586)) {
               }

               var4.x = var16;
               var4.y = var17;
               if (var13.randomAngle) {
                  var18 = Rand.Next(0.0F, 6.2831855F);
               }

               var14.savedRot.setAngleAxis(var18, 0.0F, 1.0F, 0.0F);
               var14.jniTransform.setRotation(var14.savedRot);
               IsoGridSquare var19 = this.getGridSquare(PZMath.fastfloor(var11) - this.wx * 8, PZMath.fastfloor(var12) - this.wy * 8, 0);
               if (var11 < (float)var19.x + 0.005F) {
                  var11 = (float)var19.x + 0.005F;
               }

               if (var11 > (float)(var19.x + 1) - 0.005F) {
                  var11 = (float)(var19.x + 1) - 0.005F;
               }

               if (var12 < (float)var19.y + 0.005F) {
                  var12 = (float)var19.y + 0.005F;
               }

               if (var12 > (float)(var19.y + 1) - 0.005F) {
                  var12 = (float)(var19.y + 1) - 0.005F;
               }

               var14.setX(var11);
               var14.setY(var12);
               var14.setZ((float)var19.z);
               var14.jniTransform.origin.set(var14.getX() - WorldSimulation.instance.offsetX, var14.getZ(), var14.getY() - WorldSimulation.instance.offsetY);
               float var20 = 100.0F - Math.min(var13.baseVehicleQuality * 120.0F, 100.0F);
               var14.rust = (float)Rand.Next(100) < var20 ? 1.0F : 0.0F;
               if (doSpawnedVehiclesInInvalidPosition(var14) || GameClient.bClient) {
                  this.vehicles.add(var14);
               }
            }
         }
      }

   }

   public static void removeFromCheckedVehicles(BaseVehicle var0) {
      BaseVehicleCheckedVehicles.remove(var0);
   }

   public static void addFromCheckedVehicles(BaseVehicle var0) {
      if (!BaseVehicleCheckedVehicles.contains(var0)) {
         BaseVehicleCheckedVehicles.add(var0);
      }

   }

   public static void Reset() {
      BaseVehicleCheckedVehicles.clear();
   }

   public static boolean doSpawnedVehiclesInInvalidPosition(BaseVehicle var0) {
      int var1 = PZMath.fastfloor(var0.getZ());
      boolean var2 = true;

      for(int var3 = 0; var3 < BaseVehicleCheckedVehicles.size(); ++var3) {
         if (((BaseVehicle)BaseVehicleCheckedVehicles.get(var3)).testCollisionWithVehicle(var0)) {
            var2 = false;
            return false;
         }
      }

      if (var2) {
         addFromCheckedVehicles(var0);
      }

      return var2;
   }

   private void spawnVehicleRandomAngle(IsoGridSquare var1, Zone var2, String var3) {
      boolean var4 = true;
      byte var5 = 3;
      byte var6 = 4;
      if ((var2.w == var6 || var2.w == var6 + 1 || var2.w == var6 + 2) && (var2.h <= var5 || var2.h >= var6 + 2)) {
         var4 = false;
      }

      var6 = 5;
      if (!var4) {
         var6 = 3;
         var5 = 5;
      }

      VehicleType var7 = VehicleType.getRandomVehicleType(var3);
      if (var7 == null) {
         System.out.println("Can't find car: " + var3);
      } else {
         BaseVehicle var8 = new BaseVehicle(IsoWorld.instance.CurrentCell);
         var8.setZone(var3);
         if (this.RandomizeModel(var8, var2, var3, var7)) {
            if (var4) {
               var8.setDir(Rand.Next(2) == 0 ? IsoDirections.N : IsoDirections.S);
            } else {
               var8.setDir(Rand.Next(2) == 0 ? IsoDirections.W : IsoDirections.E);
            }

            float var9 = Rand.Next(0.0F, 6.2831855F);
            var8.savedRot.setAngleAxis(var9, 0.0F, 1.0F, 0.0F);
            var8.jniTransform.setRotation(var8.savedRot);
            if (var4) {
               var8.setX((float)var1.x + (float)var5 / 2.0F - (float)((int)((float)var5 / 2.0F)));
               var8.setY((float)var1.y);
            } else {
               var8.setX((float)var1.x);
               var8.setY((float)var1.y + (float)var6 / 2.0F - (float)((int)((float)var6 / 2.0F)));
            }

            var8.setZ((float)var1.z);
            var8.jniTransform.origin.set(var8.getX() - WorldSimulation.instance.offsetX, var8.getZ(), var8.getY() - WorldSimulation.instance.offsetY);
            if (doSpawnedVehiclesInInvalidPosition(var8) || GameClient.bClient) {
               this.vehicles.add(var8);
            }

         }
      }
   }

   public boolean RandomizeModel(BaseVehicle var1, Zone var2, String var3, VehicleType var4) {
      if (var4.vehiclesDefinition.isEmpty()) {
         System.out.println("no vehicle definition found for " + var3);
         return false;
      } else {
         float var5 = Rand.Next(0.0F, 100.0F);
         float var6 = 0.0F;
         VehicleType.VehicleTypeDefinition var7 = null;

         for(int var8 = 0; var8 < var4.vehiclesDefinition.size(); ++var8) {
            var7 = (VehicleType.VehicleTypeDefinition)var4.vehiclesDefinition.get(var8);
            var6 += var7.spawnChance;
            if (var5 < var6) {
               break;
            }
         }

         String var13 = var7.vehicleType;
         VehicleScript var9 = ScriptManager.instance.getVehicle(var13);
         if (var9 == null) {
            DebugLog.log("no such vehicle script \"" + var13 + "\" in IsoChunk.RandomizeModel");
            return false;
         } else {
            int var10 = var7.index;
            var1.setScriptName(var13);
            var1.setScript();

            try {
               if (var10 > -1) {
                  var1.setSkinIndex(var10);
               } else {
                  var1.setSkinIndex(Rand.Next(var1.getSkinCount()));
               }

               return true;
            } catch (Exception var12) {
               DebugLog.log("problem with " + var1.getScriptName());
               var12.printStackTrace();
               return false;
            }
         }
      }
   }

   private void AddVehicles_TrafficJam_W(Zone var1, String var2) {
      int var3;
      for(var3 = var1.y - this.wy * 8 + 1; var3 < 0; var3 += 3) {
      }

      int var4;
      for(var4 = var1.x - this.wx * 8 + 3; var4 < 0; var4 += 6) {
      }

      for(int var5 = var3; var5 < 8 && this.wy * 8 + var5 < var1.y + var1.h; var5 += 3 + Rand.Next(1)) {
         for(int var6 = var4; var6 < 8 && this.wx * 8 + var6 < var1.x + var1.w; var6 += 6 + Rand.Next(1)) {
            IsoGridSquare var7 = this.getGridSquare(var6, var5, 0);
            if (var7 != null) {
               VehicleType var8 = VehicleType.getRandomVehicleType(var2);
               if (var8 == null) {
                  System.out.println("Can't find car: " + var2);
                  break;
               }

               byte var9 = 80;
               if (SystemDisabler.doVehiclesEverywhere || DebugOptions.instance.VehicleSpawnEverywhere.getValue()) {
                  var9 = 100;
               }

               if (Rand.Next(100) <= var9) {
                  BaseVehicle var10 = new BaseVehicle(IsoWorld.instance.CurrentCell);
                  var10.setZone("TrafficJam");
                  var10.setVehicleType(var8.name);
                  if (!this.RandomizeModel(var10, var1, var2, var8)) {
                     return;
                  }

                  var10.setScript();
                  var10.setX((float)var7.x + Rand.Next(0.0F, 1.0F));
                  var10.setY((float)var7.y + Rand.Next(0.0F, 1.0F));
                  var10.setZ((float)var7.z);
                  var10.jniTransform.origin.set(var10.getX() - WorldSimulation.instance.offsetX, var10.getZ(), var10.getY() - WorldSimulation.instance.offsetY);
                  if (this.isGoodVehiclePos(var10, this)) {
                     var10.setSkinIndex(Rand.Next(var10.getSkinCount() - 1));
                     var10.setDir(IsoDirections.W);
                     float var11 = (float)Math.abs(var1.x + var1.w - var7.x);
                     var11 /= 20.0F;
                     var11 = Math.min(2.0F, var11);

                     float var12;
                     for(var12 = var10.getDir().toAngle() + 3.1415927F - 0.25F + Rand.Next(0.0F, var11); (double)var12 > 6.283185307179586; var12 = (float)((double)var12 - 6.283185307179586)) {
                     }

                     var10.savedRot.setAngleAxis(var12, 0.0F, 1.0F, 0.0F);
                     var10.jniTransform.setRotation(var10.savedRot);
                     if (doSpawnedVehiclesInInvalidPosition(var10) || GameClient.bClient) {
                        this.vehicles.add(var10);
                     }
                  }
               }
            }
         }
      }

   }

   private void AddVehicles_TrafficJam_E(Zone var1, String var2) {
      int var3;
      for(var3 = var1.y - this.wy * 8 + 1; var3 < 0; var3 += 3) {
      }

      int var4;
      for(var4 = var1.x - this.wx * 8 + 3; var4 < 0; var4 += 6) {
      }

      for(int var5 = var3; var5 < 8 && this.wy * 8 + var5 < var1.y + var1.h; var5 += 3 + Rand.Next(1)) {
         for(int var6 = var4; var6 < 8 && this.wx * 8 + var6 < var1.x + var1.w; var6 += 6 + Rand.Next(1)) {
            IsoGridSquare var7 = this.getGridSquare(var6, var5, 0);
            if (var7 != null) {
               VehicleType var8 = VehicleType.getRandomVehicleType(var2);
               if (var8 == null) {
                  System.out.println("Can't find car: " + var2);
                  break;
               }

               byte var9 = 80;
               if (SystemDisabler.doVehiclesEverywhere || DebugOptions.instance.VehicleSpawnEverywhere.getValue()) {
                  var9 = 100;
               }

               if (Rand.Next(100) <= var9) {
                  BaseVehicle var10 = new BaseVehicle(IsoWorld.instance.CurrentCell);
                  var10.setZone("TrafficJam");
                  var10.setVehicleType(var8.name);
                  if (!this.RandomizeModel(var10, var1, var2, var8)) {
                     return;
                  }

                  var10.setScript();
                  var10.setX((float)var7.x + Rand.Next(0.0F, 1.0F));
                  var10.setY((float)var7.y + Rand.Next(0.0F, 1.0F));
                  var10.setZ((float)var7.z);
                  var10.jniTransform.origin.set(var10.getX() - WorldSimulation.instance.offsetX, var10.getZ(), var10.getY() - WorldSimulation.instance.offsetY);
                  if (this.isGoodVehiclePos(var10, this)) {
                     var10.setSkinIndex(Rand.Next(var10.getSkinCount() - 1));
                     var10.setDir(IsoDirections.E);
                     float var11 = (float)Math.abs(var1.x + var1.w - var7.x - var1.w);
                     var11 /= 20.0F;
                     var11 = Math.min(2.0F, var11);

                     float var12;
                     for(var12 = var10.getDir().toAngle() + 3.1415927F - 0.25F + Rand.Next(0.0F, var11); (double)var12 > 6.283185307179586; var12 = (float)((double)var12 - 6.283185307179586)) {
                     }

                     var10.savedRot.setAngleAxis(var12, 0.0F, 1.0F, 0.0F);
                     var10.jniTransform.setRotation(var10.savedRot);
                     if (doSpawnedVehiclesInInvalidPosition(var10) || GameClient.bClient) {
                        this.vehicles.add(var10);
                     }
                  }
               }
            }
         }
      }

   }

   private void AddVehicles_TrafficJam_S(Zone var1, String var2) {
      int var3;
      for(var3 = var1.y - this.wy * 8 + 3; var3 < 0; var3 += 6) {
      }

      int var4;
      for(var4 = var1.x - this.wx * 8 + 1; var4 < 0; var4 += 3) {
      }

      for(int var5 = var3; var5 < 8 && this.wy * 8 + var5 < var1.y + var1.h; var5 += 6 + Rand.Next(-1, 1)) {
         for(int var6 = var4; var6 < 8 && this.wx * 8 + var6 < var1.x + var1.w; var6 += 3 + Rand.Next(1)) {
            IsoGridSquare var7 = this.getGridSquare(var6, var5, 0);
            if (var7 != null) {
               VehicleType var8 = VehicleType.getRandomVehicleType(var2);
               if (var8 == null) {
                  System.out.println("Can't find car: " + var2);
                  break;
               }

               byte var9 = 80;
               if (SystemDisabler.doVehiclesEverywhere || DebugOptions.instance.VehicleSpawnEverywhere.getValue()) {
                  var9 = 100;
               }

               if (Rand.Next(100) <= var9) {
                  BaseVehicle var10 = new BaseVehicle(IsoWorld.instance.CurrentCell);
                  var10.setZone("TrafficJam");
                  var10.setVehicleType(var8.name);
                  if (!this.RandomizeModel(var10, var1, var2, var8)) {
                     return;
                  }

                  var10.setScript();
                  var10.setX((float)var7.x + Rand.Next(0.0F, 1.0F));
                  var10.setY((float)var7.y + Rand.Next(0.0F, 1.0F));
                  var10.setZ((float)var7.z);
                  var10.jniTransform.origin.set(var10.getX() - WorldSimulation.instance.offsetX, var10.getZ(), var10.getY() - WorldSimulation.instance.offsetY);
                  if (this.isGoodVehiclePos(var10, this)) {
                     var10.setSkinIndex(Rand.Next(var10.getSkinCount() - 1));
                     var10.setDir(IsoDirections.S);
                     float var11 = (float)Math.abs(var1.y + var1.h - var7.y - var1.h);
                     var11 /= 20.0F;
                     var11 = Math.min(2.0F, var11);

                     float var12;
                     for(var12 = var10.getDir().toAngle() + 3.1415927F - 0.25F + Rand.Next(0.0F, var11); (double)var12 > 6.283185307179586; var12 = (float)((double)var12 - 6.283185307179586)) {
                     }

                     var10.savedRot.setAngleAxis(var12, 0.0F, 1.0F, 0.0F);
                     var10.jniTransform.setRotation(var10.savedRot);
                     if (doSpawnedVehiclesInInvalidPosition(var10) || GameClient.bClient) {
                        this.vehicles.add(var10);
                     }
                  }
               }
            }
         }
      }

   }

   private void AddVehicles_TrafficJam_N(Zone var1, String var2) {
      int var3;
      for(var3 = var1.y - this.wy * 8 + 3; var3 < 0; var3 += 6) {
      }

      int var4;
      for(var4 = var1.x - this.wx * 8 + 1; var4 < 0; var4 += 3) {
      }

      for(int var5 = var3; var5 < 8 && this.wy * 8 + var5 < var1.y + var1.h; var5 += 6 + Rand.Next(-1, 1)) {
         for(int var6 = var4; var6 < 8 && this.wx * 8 + var6 < var1.x + var1.w; var6 += 3 + Rand.Next(1)) {
            IsoGridSquare var7 = this.getGridSquare(var6, var5, 0);
            if (var7 != null) {
               VehicleType var8 = VehicleType.getRandomVehicleType(var2);
               if (var8 == null) {
                  System.out.println("Can't find car: " + var2);
                  break;
               }

               byte var9 = 80;
               if (SystemDisabler.doVehiclesEverywhere || DebugOptions.instance.VehicleSpawnEverywhere.getValue()) {
                  var9 = 100;
               }

               if (Rand.Next(100) <= var9) {
                  BaseVehicle var10 = new BaseVehicle(IsoWorld.instance.CurrentCell);
                  var10.setZone("TrafficJam");
                  var10.setVehicleType(var8.name);
                  if (!this.RandomizeModel(var10, var1, var2, var8)) {
                     return;
                  }

                  var10.setScript();
                  var10.setX((float)var7.x + Rand.Next(0.0F, 1.0F));
                  var10.setY((float)var7.y + Rand.Next(0.0F, 1.0F));
                  var10.setZ((float)var7.z);
                  var10.jniTransform.origin.set(var10.getX() - WorldSimulation.instance.offsetX, var10.getZ(), var10.getY() - WorldSimulation.instance.offsetY);
                  if (this.isGoodVehiclePos(var10, this)) {
                     var10.setSkinIndex(Rand.Next(var10.getSkinCount() - 1));
                     var10.setDir(IsoDirections.N);
                     float var11 = (float)Math.abs(var1.y + var1.h - var7.y);
                     var11 /= 20.0F;
                     var11 = Math.min(2.0F, var11);

                     float var12;
                     for(var12 = var10.getDir().toAngle() + 3.1415927F - 0.25F + Rand.Next(0.0F, var11); (double)var12 > 6.283185307179586; var12 = (float)((double)var12 - 6.283185307179586)) {
                     }

                     var10.savedRot.setAngleAxis(var12, 0.0F, 1.0F, 0.0F);
                     var10.jniTransform.setRotation(var10.savedRot);
                     if (doSpawnedVehiclesInInvalidPosition(var10) || GameClient.bClient) {
                        this.vehicles.add(var10);
                     }
                  }
               }
            }
         }
      }

   }

   private void AddVehicles_TrafficJam_Polyline(Zone var1, String var2) {
      Vector2 var3 = new Vector2();
      Vector2 var4 = new Vector2();
      float var5 = 0.0F;
      float var6 = var1.getPolylineLength();

      for(int var7 = 0; var7 < var1.points.size() - 2; var7 += 2) {
         int var8 = var1.points.getQuick(var7);
         int var9 = var1.points.getQuick(var7 + 1);
         int var10 = var1.points.getQuick(var7 + 2);
         int var11 = var1.points.getQuick(var7 + 3);
         var3.set((float)(var10 - var8), (float)(var11 - var9));
         float var12 = var3.getLength();
         var4.set(var3);
         var4.tangent();
         var4.normalize();
         float var13 = var5;
         var5 += var12;

         for(float var14 = 3.0F; var14 <= var12 - 3.0F; var14 += (float)(6 + Rand.Next(-1, 1))) {
            float var15 = PZMath.clamp(var14 + Rand.Next(-1.0F, 1.0F), 3.0F, var12 - 3.0F);
            float var16 = Rand.Next(-1.0F, 1.0F);
            float var17 = (float)var8 + var3.x / var12 * var15 + var4.x * var16;
            float var18 = (float)var9 + var3.y / var12 * var15 + var4.y * var16;
            this.TryAddVehicle_TrafficJam(var1, var2, var17, var18, var3, var13 + var15, var6);

            for(float var19 = 2.0F; var19 + 1.5F <= (float)var1.polylineWidth / 2.0F; var19 += 2.0F) {
               var16 = var19 + Rand.Next(-1.0F, 1.0F);
               if (var16 + 1.5F <= (float)var1.polylineWidth / 2.0F) {
                  var15 = PZMath.clamp(var14 + Rand.Next(-2.0F, 2.0F), 3.0F, var12 - 3.0F);
                  var17 = (float)var8 + var3.x / var12 * var15 + var4.x * var16;
                  var18 = (float)var9 + var3.y / var12 * var15 + var4.y * var16;
                  this.TryAddVehicle_TrafficJam(var1, var2, var17, var18, var3, var13 + var15, var6);
               }

               var16 = var19 + Rand.Next(-1.0F, 1.0F);
               if (var16 + 1.5F <= (float)var1.polylineWidth / 2.0F) {
                  var15 = PZMath.clamp(var14 + Rand.Next(-2.0F, 2.0F), 3.0F, var12 - 3.0F);
                  var17 = (float)var8 + var3.x / var12 * var15 - var4.x * var16;
                  var18 = (float)var9 + var3.y / var12 * var15 - var4.y * var16;
                  this.TryAddVehicle_TrafficJam(var1, var2, var17, var18, var3, var13 + var15, var6);
               }
            }
         }
      }

   }

   private void TryAddVehicle_TrafficJam(Zone var1, String var2, float var3, float var4, Vector2 var5, float var6, float var7) {
      if (!(var3 < (float)(this.wx * 8)) && !(var3 >= (float)((this.wx + 1) * 8)) && !(var4 < (float)(this.wy * 8)) && !(var4 >= (float)((this.wy + 1) * 8))) {
         IsoGridSquare var8 = this.getGridSquare(PZMath.fastfloor(var3) - this.wx * 8, PZMath.fastfloor(var4) - this.wy * 8, 0);
         if (var8 != null) {
            VehicleType var9 = VehicleType.getRandomVehicleType(var2 + "W");
            if (var9 == null) {
               System.out.println("Can't find car: " + var2);
            } else {
               byte var10 = 80;
               if (SystemDisabler.doVehiclesEverywhere || DebugOptions.instance.VehicleSpawnEverywhere.getValue()) {
                  var10 = 100;
               }

               if (Rand.Next(100) <= var10) {
                  BaseVehicle var11 = new BaseVehicle(IsoWorld.instance.CurrentCell);
                  var11.setZone("TrafficJam");
                  var11.setVehicleType(var9.name);
                  if (this.RandomizeModel(var11, var1, var2, var9)) {
                     var11.setScript();
                     var11.setX(var3);
                     var11.setY(var4);
                     var11.setZ((float)var8.z);
                     float var12 = var5.x;
                     float var13 = var5.y;
                     var5.normalize();
                     var11.setDir(IsoDirections.fromAngle(var5));
                     float var14 = var5.getDirectionNeg();
                     var5.set(var12, var13);
                     float var15 = 90.0F * (var6 / var7);

                     for(var14 += Rand.Next(-var15, var15) * 0.017453292F; (double)var14 > 6.283185307179586; var14 = (float)((double)var14 - 6.283185307179586)) {
                     }

                     var11.savedRot.setAngleAxis(var14, 0.0F, 1.0F, 0.0F);
                     var11.jniTransform.setRotation(var11.savedRot);
                     var11.jniTransform.origin.set(var11.getX() - WorldSimulation.instance.offsetX, var11.getZ(), var11.getY() - WorldSimulation.instance.offsetY);
                     if (this.isGoodVehiclePos(var11, this)) {
                        var11.setSkinIndex(Rand.Next(var11.getSkinCount() - 1));
                        if (doSpawnedVehiclesInInvalidPosition(var11)) {
                           this.vehicles.add(var11);
                        }
                     }

                  }
               }
            }
         }
      }
   }

   public void AddVehicles() {
      if (SandboxOptions.instance.CarSpawnRate.getValue() != 1) {
         if (VehicleType.vehicles.isEmpty()) {
            VehicleType.init();
         }

         if (!GameClient.bClient) {
            if (SandboxOptions.instance.EnableVehicles.getValue()) {
               if (!GameServer.bServer) {
                  WorldSimulation.instance.create();
               }

               IsoMetaCell var1 = IsoWorld.instance.getMetaGrid().getCellData(this.wx / IsoCell.CellSizeInChunks, this.wy / IsoCell.CellSizeInChunks);
               ArrayList var2 = var1 == null ? null : var1.vehicleZones;

               for(int var3 = 0; var2 != null && var3 < var2.size(); ++var3) {
                  VehicleZone var4 = (VehicleZone)var2.get(var3);
                  if (var4.x + var4.w >= this.wx * 8 && var4.y + var4.h >= this.wy * 8 && var4.x < (this.wx + 1) * 8 && var4.y < (this.wy + 1) * 8) {
                     String var5 = var4.name;
                     if (var5.isEmpty()) {
                        var5 = var4.type;
                     }

                     if (SandboxOptions.instance.TrafficJam.getValue()) {
                        if (var4.isPolyline()) {
                           if ("TrafficJam".equalsIgnoreCase(var5)) {
                              this.AddVehicles_TrafficJam_Polyline(var4, var5);
                              continue;
                           }

                           if ("RTrafficJam".equalsIgnoreCase(var5) && Rand.Next(100) < 10) {
                              this.AddVehicles_TrafficJam_Polyline(var4, var5.replaceFirst("rtraffic", "traffic"));
                              continue;
                           }
                        }

                        if ("TrafficJamW".equalsIgnoreCase(var5)) {
                           this.AddVehicles_TrafficJam_W(var4, var5);
                        }

                        if ("TrafficJamE".equalsIgnoreCase(var5)) {
                           this.AddVehicles_TrafficJam_E(var4, var5);
                        }

                        if ("TrafficJamS".equalsIgnoreCase(var5)) {
                           this.AddVehicles_TrafficJam_S(var4, var5);
                        }

                        if ("TrafficJamN".equalsIgnoreCase(var5)) {
                           this.AddVehicles_TrafficJam_N(var4, var5);
                        }

                        if ("RTrafficJamW".equalsIgnoreCase(var5) && Rand.Next(100) < 10) {
                           this.AddVehicles_TrafficJam_W(var4, var5.replaceFirst("rtraffic", "traffic"));
                        }

                        if ("RTrafficJamE".equalsIgnoreCase(var5) && Rand.Next(100) < 10) {
                           this.AddVehicles_TrafficJam_E(var4, var5.replaceFirst("rtraffic", "traffic"));
                        }

                        if ("RTrafficJamS".equalsIgnoreCase(var5) && Rand.Next(100) < 10) {
                           this.AddVehicles_TrafficJam_S(var4, var5.replaceFirst("rtraffic", "traffic"));
                        }

                        if ("RTrafficJamN".equalsIgnoreCase(var5) && Rand.Next(100) < 10) {
                           this.AddVehicles_TrafficJam_N(var4, var5.replaceFirst("rtraffic", "traffic"));
                        }
                     }

                     if (!StringUtils.containsIgnoreCase(var5, "TrafficJam")) {
                        if ("TestVehicles".equals(var5)) {
                           this.AddVehicles_ForTest(var4);
                        } else if (VehicleType.hasTypeForZone(var5)) {
                           if (var4.isPolyline()) {
                              this.AddVehicles_OnZonePolyline(var4, var5);
                           } else {
                              this.AddVehicles_OnZone(var4, var5);
                           }
                        }
                     }
                  }
               }

               IsoMetaChunk var6 = IsoWorld.instance.getMetaChunk(this.wx, this.wy);
               if (var6 != null) {
                  for(int var7 = 0; var7 < var6.getZonesSize(); ++var7) {
                     Zone var8 = var6.getZone(var7);
                     this.addRandomCarCrash(var8, false);
                  }

               }
            }
         }
      }
   }

   public void addSurvivorInHorde(boolean var1) {
      if (var1 || !IsoWorld.getZombiesDisabled()) {
         IsoMetaChunk var2 = IsoWorld.instance.getMetaChunk(this.wx, this.wy);
         if (var2 != null) {
            for(int var3 = 0; var3 < var2.getZonesSize(); ++var3) {
               Zone var4 = var2.getZone(var3);
               if (this.canAddSurvivorInHorde(var4, var1)) {
                  int var5 = 4;
                  float var6 = (float)GameTime.getInstance().getWorldAgeHours() / 24.0F;
                  var6 += (float)((SandboxOptions.instance.TimeSinceApo.getValue() - 1) * 30);
                  var5 = (int)((float)var5 + var6 * 0.03F);
                  var5 = Math.max(15, var5);
                  if (var1 || Rand.Next(0.0F, 500.0F) < 0.4F * (float)var5) {
                     this.addSurvivorInHorde(var4);
                     if (var1) {
                        break;
                     }
                  }
               }
            }

         }
      }
   }

   private boolean canAddSurvivorInHorde(Zone var1, boolean var2) {
      if (!var2 && IsoWorld.instance.getTimeSinceLastSurvivorInHorde() > 0) {
         return false;
      } else if (!var2 && IsoWorld.getZombiesDisabled()) {
         return false;
      } else if (!var2 && var1.hourLastSeen != 0) {
         return false;
      } else if (!var2 && var1.haveConstruction) {
         return false;
      } else {
         return "Nav".equals(var1.getType());
      }
   }

   private void addSurvivorInHorde(Zone var1) {
      ++var1.hourLastSeen;
      IsoWorld.instance.setTimeSinceLastSurvivorInHorde(5000);
      int var2 = Math.max(var1.x, this.wx * 8);
      int var3 = Math.max(var1.y, this.wy * 8);
      int var4 = Math.min(var1.x + var1.w, (this.wx + 1) * 8);
      int var5 = Math.min(var1.y + var1.h, (this.wy + 1) * 8);
      float var6 = (float)var2 + (float)(var4 - var2) / 2.0F;
      float var7 = (float)var3 + (float)(var5 - var3) / 2.0F;
      VirtualZombieManager.instance.choices.clear();

      IsoGridSquare var8;
      int var9;
      int var10;
      for(var9 = -2; var9 < 2; ++var9) {
         for(var10 = -2; var10 < 2; ++var10) {
            var8 = this.getGridSquare((int)(var6 + (float)var9) - this.wx * 8, (int)(var7 + (float)var10) - this.wy * 8, 0);
            if (var8 != null && var8.getBuilding() == null && !var8.isVehicleIntersecting() && var8.isGoodSquare()) {
               VirtualZombieManager.instance.choices.add(var8);
            }
         }
      }

      if (VirtualZombieManager.instance.choices.size() >= 1) {
         var9 = Rand.Next(15, 20);

         for(var10 = 0; var10 < var9; ++var10) {
            IsoZombie var11 = VirtualZombieManager.instance.createRealZombieAlways(Rand.Next(8), false);
            if (var11 != null) {
               var11.dressInRandomOutfit();
               ZombieSpawnRecorder.instance.record(var11, "addSurvivorInHorde");
            }
         }

         VirtualZombieManager.instance.choices.clear();
         var8 = this.getGridSquare((int)var6 - this.wx * 8, (int)var7 - this.wy * 8, 0);
         if (var8 != null && var8.getBuilding() == null && !var8.isVehicleIntersecting() && var8.isGoodSquare()) {
            VirtualZombieManager.instance.choices.add(var8);
            IsoZombie var12 = VirtualZombieManager.instance.createRealZombieAlways(Rand.Next(8), false);
            if (var12 != null) {
               ZombieSpawnRecorder.instance.record(var12, "addSurvivorInHorde");
               var12.setAsSurvivor();
            }
         }

      }
   }

   public boolean canAddRandomCarCrash(Zone var1, boolean var2) {
      if (!var2 && var1.hourLastSeen != 0) {
         return false;
      } else if (!var2 && var1.haveConstruction) {
         return false;
      } else if (!"Nav".equals(var1.getType())) {
         return false;
      } else {
         int var3 = Math.max(var1.x, this.wx * 8);
         int var4 = Math.max(var1.y, this.wy * 8);
         int var5 = Math.min(var1.x + var1.w, (this.wx + 1) * 8);
         int var6 = Math.min(var1.y + var1.h, (this.wy + 1) * 8);
         if (var1.w > 30 && var1.h < 13) {
            return var5 - var3 >= 10 && var6 - var4 >= 5;
         } else if (var1.h > 30 && var1.w < 13) {
            return var5 - var3 >= 5 && var6 - var4 >= 10;
         } else {
            return false;
         }
      }
   }

   public void addRandomCarCrash(Zone var1, boolean var2) {
      if (var1 != null) {
         if (this.vehicles.isEmpty()) {
            if ("Nav".equals(var1.getType())) {
               RandomizedVehicleStoryBase.doRandomStory(var1, this, false);
            }
         }
      }
   }

   public static boolean FileExists(int var0, int var1) {
      File var2 = ChunkMapFilenames.instance.getFilename(var0, var1);
      if (var2 == null) {
         var2 = ZomboidFileSystem.instance.getFileInCurrentSave(prefix + var0 + "_" + var1 + ".bin");
      }

      long var3 = 0L;
      return var2.exists();
   }

   public void checkPhysicsLater(int var1) {
      IsoChunkLevel var2 = this.getLevelData(var1);
      if (var2 != null) {
         var2.physicsCheck = true;
      }
   }

   public void updatePhysicsForLevel(int var1) {
      Bullet.beginUpdateChunk(this, var1);

      for(int var2 = 0; var2 < 8; ++var2) {
         for(int var3 = 0; var3 < 8; ++var3) {
            this.calcPhysics(var3, var2, var1, this.shapes);
            int var4 = 0;

            for(int var5 = 0; var5 < 4; ++var5) {
               if (this.shapes[var5] != -1) {
                  bshapes[var4++] = (byte)(this.shapes[var5] + 1);
               }
            }

            Bullet.updateChunk(var3, var2, var4, bshapes);
         }
      }

      Bullet.endUpdateChunk();
   }

   private void calcPhysics(int var1, int var2, int var3, int[] var4) {
      for(int var5 = 0; var5 < 4; ++var5) {
         var4[var5] = -1;
      }

      IsoGridSquare var11 = this.getGridSquare(var1, var2, var3);
      if (var11 != null) {
         int var6 = 0;
         boolean var7;
         int var8;
         if (var3 == 0) {
            var7 = false;

            for(var8 = 0; var8 < var11.getObjects().size(); ++var8) {
               IsoObject var9 = (IsoObject)var11.getObjects().get(var8);
               if (var9.sprite != null && var9.sprite.name != null && (var9.sprite.name.contains("lighting_outdoor_") || var9.sprite.name.equals("recreational_sports_01_21") || var9.sprite.name.equals("recreational_sports_01_19") || var9.sprite.name.equals("recreational_sports_01_32")) && (!var9.getProperties().Is("MoveType") || !"WallObject".equals(var9.getProperties().Val("MoveType")))) {
                  var7 = true;
                  break;
               }
            }

            if (var7) {
               var4[var6++] = IsoChunk.PhysicsShapes.Tree.ordinal();
            }
         }

         var7 = false;
         if (!var11.getSpecialObjects().isEmpty()) {
            var8 = var11.getSpecialObjects().size();

            for(int var13 = 0; var13 < var8; ++var13) {
               IsoObject var10 = (IsoObject)var11.getSpecialObjects().get(var13);
               if (var10 instanceof IsoThumpable && ((IsoThumpable)var10).isBlockAllTheSquare()) {
                  var7 = true;
                  break;
               }
            }
         }

         PropertyContainer var12 = var11.getProperties();
         if (var11.HasStairs()) {
            if (var11.Has(IsoObjectType.stairsMN)) {
               var4[var6++] = IsoChunk.PhysicsShapes.StairsMiddleNorth.ordinal();
            }

            if (var11.Has(IsoObjectType.stairsMW)) {
               var4[var6++] = IsoChunk.PhysicsShapes.StairsMiddleWest.ordinal();
            }
         }

         if (var11.hasTypes.isSet(IsoObjectType.isMoveAbleObject)) {
            var4[var6++] = IsoChunk.PhysicsShapes.Tree.ordinal();
         }

         String var14;
         if (var11.hasTypes.isSet(IsoObjectType.tree)) {
            var14 = var11.getProperties().Val("tree");
            String var15 = var11.getProperties().Val("WindType");
            if (var14 == null) {
               var4[var6++] = IsoChunk.PhysicsShapes.Tree.ordinal();
            }

            if (var14 != null && !var14.equals("1") && (var15 == null || !var15.equals("2") || !var14.equals("2") && !var14.equals("1"))) {
               var4[var6++] = IsoChunk.PhysicsShapes.Tree.ordinal();
            }
         } else if (var12.Is(IsoFlagType.solid) || var12.Is(IsoFlagType.solidtrans) || var12.Is(IsoFlagType.blocksight) || var11.HasStairs() || var7) {
            if (var6 == var4.length) {
               DebugLog.log(DebugType.General, "Error: Too many physics objects on gridsquare: " + var11.x + ", " + var11.y + ", " + var11.z);
               return;
            }

            if (var11.HasStairs()) {
               var4[var6++] = IsoChunk.PhysicsShapes.SolidStairs.ordinal();
            } else {
               var4[var6++] = IsoChunk.PhysicsShapes.Solid.ordinal();
            }
         }

         label228: {
            if (var11.SolidFloorCached) {
               if (!var11.SolidFloor) {
                  break label228;
               }
            } else if (!var11.TreatAsSolidFloor()) {
               break label228;
            }

            if (var6 == var4.length) {
               DebugLog.log(DebugType.General, "Error: Too many physics objects on gridsquare: " + var11.x + ", " + var11.y + ", " + var11.z);
               return;
            }

            var4[var6++] = IsoChunk.PhysicsShapes.Floor.ordinal();
         }

         if (!var11.getProperties().Is("CarSlowFactor")) {
            if (var12.Is(IsoFlagType.collideW) || var12.Is(IsoFlagType.windowW) || var11.getProperties().Is(IsoFlagType.DoorWallW) && !var11.getProperties().Is("GarageDoor")) {
               if (var6 == var4.length) {
                  DebugLog.log(DebugType.General, "Error: Too many physics objects on gridsquare: " + var11.x + ", " + var11.y + ", " + var11.z);
                  return;
               }

               var4[var6++] = IsoChunk.PhysicsShapes.WallW.ordinal();
            }

            if (var12.Is(IsoFlagType.collideN) || var12.Is(IsoFlagType.windowN) || var11.getProperties().Is(IsoFlagType.DoorWallN) && !var11.getProperties().Is("GarageDoor")) {
               if (var6 == var4.length) {
                  DebugLog.log(DebugType.General, "Error: Too many physics objects on gridsquare: " + var11.x + ", " + var11.y + ", " + var11.z);
                  return;
               }

               var4[var6++] = IsoChunk.PhysicsShapes.WallN.ordinal();
            }

            if (var11.Is("PhysicsShape")) {
               if (var6 == var4.length) {
                  DebugLog.log(DebugType.General, "Error: Too many physics objects on gridsquare: " + var11.x + ", " + var11.y + ", " + var11.z);
                  return;
               }

               var14 = var11.getProperties().Val("PhysicsShape");
               if ("Solid".equals(var14)) {
                  var4[var6++] = IsoChunk.PhysicsShapes.Solid.ordinal();
               } else if ("WallN".equals(var14)) {
                  var4[var6++] = IsoChunk.PhysicsShapes.WallN.ordinal();
               } else if ("WallW".equals(var14)) {
                  var4[var6++] = IsoChunk.PhysicsShapes.WallW.ordinal();
               } else if ("WallS".equals(var14)) {
                  var4[var6++] = IsoChunk.PhysicsShapes.WallS.ordinal();
               } else if ("WallE".equals(var14)) {
                  var4[var6++] = IsoChunk.PhysicsShapes.WallE.ordinal();
               } else if ("Tree".equals(var14)) {
                  var4[var6++] = IsoChunk.PhysicsShapes.Tree.ordinal();
               } else if ("Floor".equals(var14)) {
                  var4[var6++] = IsoChunk.PhysicsShapes.Floor.ordinal();
               }
            }

            if (var11.Is("PhysicsMesh")) {
               var14 = var11.getProperties().Val("PhysicsMesh");
               if (!var14.contains(".")) {
                  var14 = "Base." + var14;
               }

               Integer var16 = (Integer)Bullet.physicsShapeNameToIndex.getOrDefault(var14, (Object)null);
               if (var16 != null) {
                  var4[var6++] = IsoChunk.PhysicsShapes.FIRST_MESH.ordinal() + var16;
               }
            }

         }
      }
   }

   public void setBlendingDoneFull(boolean var1) {
      this.blendingDoneFull = var1;
   }

   public boolean isBlendingDoneFull() {
      return this.blendingDoneFull;
   }

   public void setBlendingDonePartial(boolean var1) {
      this.blendingDonePartial = var1;
   }

   public boolean isBlendingDonePartial() {
      return this.blendingDonePartial;
   }

   public void setBlendingModified(int var1) {
      this.blendingModified[var1] = true;
   }

   public boolean isBlendingDone(int var1) {
      return this.blendingModified[var1];
   }

   public void setModifDepth(BlendDirection var1, byte var2) {
      this.blendingDepth[var1.index] = var2;
   }

   public void setModifDepth(BlendDirection var1, int var2) {
      this.setModifDepth(var1, (byte)var2);
   }

   public byte getModifDepth(BlendDirection var1) {
      return this.blendingDepth[var1.index];
   }

   public void setAttachmentsDoneFull(boolean var1) {
      this.attachmentsDoneFull = var1;
   }

   public boolean isAttachmentsDoneFull() {
      return this.attachmentsDoneFull;
   }

   public void setAttachmentsState(int var1, boolean var2) {
      this.attachmentsState[var1] = var2;
   }

   public boolean isAttachmentsDone(int var1) {
      return this.attachmentsState[var1];
   }

   public boolean[] getAttachmentsState() {
      return this.attachmentsState;
   }

   public EnumSet<ChunkGenerationStatus> isModded() {
      return this.chunkGenerationStatus;
   }

   public void isModded(EnumSet<ChunkGenerationStatus> var1) {
      this.chunkGenerationStatus = var1;
   }

   public void isModded(ChunkGenerationStatus var1) {
      this.chunkGenerationStatus = EnumSet.of(var1);
   }

   public void addModded(ChunkGenerationStatus var1) {
      this.chunkGenerationStatus.add(var1);
   }

   public void rmModded(ChunkGenerationStatus var1) {
      this.chunkGenerationStatus.remove(var1);
   }

   public boolean LoadBrandNew(int var1, int var2) {
      this.wx = var1;
      this.wy = var2;
      CellLoader.LoadCellBinaryChunk(IsoWorld.instance.CurrentCell, var1, var2, this);
      if (this.hasEmptySquaresOnLevelZero()) {
         IsoWorld.instance.getWgChunk().genRandomChunk(IsoWorld.instance.CurrentCell, this, var1, var2);
      } else {
         IsoWorld.instance.getWgChunk().genMapChunk(IsoWorld.instance.CurrentCell, this, var1, var2);
      }

      IsoWorld.instance.getZoneGenerator().genForaging(var1, var2);
      Basements.getInstance().onNewChunkLoaded(this);
      if (!Core.GameMode.equals("Tutorial") && !Core.GameMode.equals("LastStand") && !GameClient.bClient) {
         this.addZombies = true;
      }

      return true;
   }

   private boolean hasEmptySquaresOnLevelZero() {
      for(int var1 = 0; var1 < 8; ++var1) {
         for(int var2 = 0; var2 < 8; ++var2) {
            IsoGridSquare var3 = this.getGridSquare(var2, var1, 0);
            if ((var3 == null || var3.getObjects().isEmpty()) && !this.hasNonEmptySquareBelow(var2, var1, 0)) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean hasNonEmptySquareBelow(int var1, int var2, int var3) {
      --var3;

      while(var3 >= this.getMinLevel()) {
         IsoGridSquare var4 = this.getGridSquare(var1, var2, var3);
         if (var4 != null && !var4.getObjects().isEmpty()) {
            return true;
         }

         --var3;
      }

      return false;
   }

   public boolean LoadOrCreate(int var1, int var2, ByteBuffer var3) {
      this.wx = var1;
      this.wy = var2;
      boolean var4;
      if (var3 != null && !this.blam) {
         var4 = this.LoadFromBuffer(var1, var2, var3);
      } else {
         File var5 = ChunkMapFilenames.instance.getFilename(var1, var2);
         if (var5.exists() && !this.blam) {
            try {
               this.LoadFromDisk();
               var4 = true;
            } catch (Exception var7) {
               ExceptionLogger.logException(var7, "Error loading chunk " + var1 + "," + var2);
               if (GameServer.bServer) {
                  LoggerManager.getLogger("map").write("Error loading chunk " + var1 + "," + var2);
                  LoggerManager.getLogger("map").write(var7);
               }

               this.BackupBlam(var1, var2, var7);
               var4 = false;
            }
         } else {
            var4 = this.LoadBrandNew(var1, var2);
         }
      }

      return var4;
   }

   public boolean LoadFromBuffer(int var1, int var2, ByteBuffer var3) {
      this.wx = var1;
      this.wy = var2;
      if (!this.blam) {
         try {
            this.LoadFromDiskOrBuffer(var3);
            return true;
         } catch (Exception var5) {
            ExceptionLogger.logException(var5);
            if (GameServer.bServer) {
               LoggerManager.getLogger("map").write("Error loading chunk " + var1 + "," + var2);
               LoggerManager.getLogger("map").write(var5);
            }

            this.BackupBlam(var1, var2, var5);
            return false;
         }
      } else {
         return this.LoadBrandNew(var1, var2);
      }
   }

   private void assignRoom(IsoGridSquare var1) {
      if (var1 != null && var1.getRoom() == null) {
         RoomDef var2 = IsoWorld.instance.MetaGrid.getRoomAt(var1.x, var1.y, var1.z);
         var1.setRoomID(var2 == null ? -1L : var2.ID);
      }
   }

   private void ensureNotNull3x3(int var1, int var2, int var3) {
      IsoCell var4 = IsoWorld.instance.CurrentCell;

      for(int var5 = 0; var5 <= 8; ++var5) {
         IsoDirections var6 = IsoDirections.fromIndex(var5);
         int var7 = var6.dx();
         int var8 = var6.dy();
         if (var1 + var7 >= 0 && var1 + var7 < 8 && var2 + var8 >= 0 && var2 + var8 < 8) {
            IsoGridSquare var9 = this.getGridSquare(var1 + var7, var2 + var8, var3);
            if (var9 == null) {
               var9 = IsoGridSquare.getNew(var4, (SliceY)null, this.wx * 8 + var1 + var7, this.wy * 8 + var2 + var8, var3);
               this.setSquare(var1 + var7, var2 + var8, var3, var9);
               this.assignRoom(var9);
            }
         }
      }

   }

   public void loadInWorldStreamerThread() {
      int var1;
      int var2;
      int var3;
      IsoGridSquare var4;
      for(var1 = this.minLevel; var1 <= this.maxLevel; ++var1) {
         for(var2 = 0; var2 < 8; ++var2) {
            for(var3 = 0; var3 < 8; ++var3) {
               var4 = this.getGridSquare(var3, var2, var1);
               if (var4 == null && var1 == 0) {
                  var4 = IsoGridSquare.getNew(IsoWorld.instance.CurrentCell, (SliceY)null, this.wx * 8 + var3, this.wy * 8 + var2, var1);
                  this.setSquare(var3, var2, var1, var4);
               }

               if (var4 != null) {
                  if (!var4.getObjects().isEmpty()) {
                     this.ensureNotNull3x3(var3, var2, var1);

                     for(int var5 = var1 - 1; var5 > this.minLevel; --var5) {
                        this.ensureNotNull3x3(var3, var2, var5);
                     }
                  }

                  var4.RecalcProperties();
               }
            }
         }
      }

      assert chunkGetter.chunk == null;

      chunkGetter.chunk = this;

      for(var1 = 0; var1 < 8; ++var1) {
         label127:
         for(var2 = 0; var2 < 8; ++var2) {
            for(var3 = this.maxLevel; var3 > 0; --var3) {
               var4 = this.getGridSquare(var2, var1, var3);
               if (var4 != null && var4.hasRainBlockingTile()) {
                  --var3;

                  while(true) {
                     if (var3 < 0) {
                        continue label127;
                     }

                     var4 = this.getGridSquare(var2, var1, var3);
                     if (var4 != null && !var4.haveRoof) {
                        var4.haveRoof = true;
                        var4.getProperties().UnSet(IsoFlagType.exterior);
                     }

                     --var3;
                  }
               }
            }
         }
      }

      for(var1 = this.minLevel; var1 <= this.maxLevel; ++var1) {
         for(var2 = 0; var2 < 8; ++var2) {
            for(var3 = 0; var3 < 8; ++var3) {
               var4 = this.getGridSquare(var3, var2, var1);
               if (var4 != null) {
                  var4.RecalcAllWithNeighbours(true, chunkGetter);
               }
            }
         }
      }

      chunkGetter.chunk = null;

      for(var1 = this.minLevel; var1 <= this.maxLevel; ++var1) {
         for(var2 = 0; var2 < 8; ++var2) {
            for(var3 = 0; var3 < 8; ++var3) {
               var4 = this.getGridSquare(var3, var2, var1);
               if (var4 != null) {
                  var4.propertiesDirty = true;
               }
            }
         }
      }

   }

   private void RecalcAllWithNeighbour(IsoGridSquare var1, IsoDirections var2, int var3) {
      int var4 = var2.dx();
      int var5 = var2.dy();
      int var6 = var1.getX() + var4;
      int var7 = var1.getY() + var5;
      int var8 = var1.getZ() + var3;
      IsoGridSquare var9 = var3 == 0 ? var1.nav[var2.index()] : IsoWorld.instance.CurrentCell.getGridSquare(var6, var7, var8);
      if (var9 != null) {
         var1.ReCalculateCollide(var9);
         var9.ReCalculateCollide(var1);
         var1.ReCalculatePathFind(var9);
         var9.ReCalculatePathFind(var1);
         var1.ReCalculateVisionBlocked(var9);
         var9.ReCalculateVisionBlocked(var1);
      }

      if (var3 == 0) {
         switch (var2) {
            case E:
               if (var9 == null) {
                  var1.e = null;
               } else {
                  var1.e = var1.testPathFindAdjacent((IsoMovingObject)null, 1, 0, 0) ? null : var9;
                  var9.w = var9.testPathFindAdjacent((IsoMovingObject)null, -1, 0, 0) ? null : var1;
               }
               break;
            case W:
               if (var9 == null) {
                  var1.w = null;
               } else {
                  var1.w = var1.testPathFindAdjacent((IsoMovingObject)null, -1, 0, 0) ? null : var9;
                  var9.e = var9.testPathFindAdjacent((IsoMovingObject)null, 1, 0, 0) ? null : var1;
               }
               break;
            case N:
               if (var9 == null) {
                  var1.n = null;
               } else {
                  var1.n = var1.testPathFindAdjacent((IsoMovingObject)null, 0, -1, 0) ? null : var9;
                  var9.s = var9.testPathFindAdjacent((IsoMovingObject)null, 0, 1, 0) ? null : var1;
               }
               break;
            case S:
               if (var9 == null) {
                  var1.s = null;
               } else {
                  var1.s = var1.testPathFindAdjacent((IsoMovingObject)null, 0, 1, 0) ? null : var9;
                  var9.n = var9.testPathFindAdjacent((IsoMovingObject)null, 0, -1, 0) ? null : var1;
               }
               break;
            case NW:
               if (var9 == null) {
                  var1.nw = null;
               } else {
                  var1.nw = var1.testPathFindAdjacent((IsoMovingObject)null, -1, -1, 0) ? null : var9;
                  var9.se = var9.testPathFindAdjacent((IsoMovingObject)null, 1, 1, 0) ? null : var1;
               }
               break;
            case NE:
               if (var9 == null) {
                  var1.ne = null;
               } else {
                  var1.ne = var1.testPathFindAdjacent((IsoMovingObject)null, 1, -1, 0) ? null : var9;
                  var9.sw = var9.testPathFindAdjacent((IsoMovingObject)null, -1, 1, 0) ? null : var1;
               }
               break;
            case SE:
               if (var9 == null) {
                  var1.se = null;
               } else {
                  var1.se = var1.testPathFindAdjacent((IsoMovingObject)null, 1, 1, 0) ? null : var9;
                  var9.nw = var9.testPathFindAdjacent((IsoMovingObject)null, -1, -1, 0) ? null : var1;
               }
               break;
            case SW:
               if (var9 == null) {
                  var1.sw = null;
               } else {
                  var1.sw = var1.testPathFindAdjacent((IsoMovingObject)null, -1, 1, 0) ? null : var9;
                  var9.ne = var9.testPathFindAdjacent((IsoMovingObject)null, 1, -1, 0) ? null : var1;
               }
         }
      }

   }

   private void EnsureSurroundNotNullX(int var1, int var2, int var3) {
      for(int var4 = var1 - 1; var4 <= var1 + 1; ++var4) {
         if (var4 >= 0 && var4 < 8) {
            this.EnsureSurroundNotNull(var4, var2, var3);
         }
      }

   }

   private void EnsureSurroundNotNullY(int var1, int var2, int var3) {
      for(int var4 = var2 - 1; var4 <= var2 + 1; ++var4) {
         if (var4 >= 0 && var4 < 8) {
            this.EnsureSurroundNotNull(var1, var4, var3);
         }
      }

   }

   private void EnsureSurroundNotNull(int var1, int var2, int var3) {
      IsoCell var4 = IsoWorld.instance.CurrentCell;
      IsoGridSquare var5 = this.getGridSquare(var1, var2, var3);
      if (var5 == null) {
         var5 = IsoGridSquare.getNew(var4, (SliceY)null, this.wx * 8 + var1, this.wy * 8 + var2, var3);
         var4.ConnectNewSquare(var5, false);
         this.assignRoom(var5);
         newSquareList.add(var5);
      }
   }

   private static int getMinLevelOf(int var0, IsoChunk var1) {
      return var1 == null ? var0 : PZMath.min(var0, var1.minLevel);
   }

   private static int getMaxLevelOf(int var0, IsoChunk var1) {
      return var1 == null ? var0 : PZMath.max(var0, var1.maxLevel);
   }

   public void loadInMainThread() {
      IsoCell var1 = IsoWorld.instance.CurrentCell;
      IsoChunk var2 = var1.getChunk(this.wx - 1, this.wy);
      IsoChunk var3 = var1.getChunk(this.wx, this.wy - 1);
      IsoChunk var4 = var1.getChunk(this.wx + 1, this.wy);
      IsoChunk var5 = var1.getChunk(this.wx, this.wy + 1);
      IsoChunk var6 = var1.getChunk(this.wx - 1, this.wy - 1);
      IsoChunk var7 = var1.getChunk(this.wx + 1, this.wy - 1);
      IsoChunk var8 = var1.getChunk(this.wx + 1, this.wy + 1);
      IsoChunk var9 = var1.getChunk(this.wx - 1, this.wy + 1);
      int var10 = getMinLevelOf(this.minLevel, var2);
      var10 = getMinLevelOf(var10, var3);
      var10 = getMinLevelOf(var10, var4);
      var10 = getMinLevelOf(var10, var5);
      var10 = getMinLevelOf(var10, var6);
      var10 = getMinLevelOf(var10, var7);
      var10 = getMinLevelOf(var10, var8);
      var10 = getMinLevelOf(var10, var9);
      int var11 = getMaxLevelOf(this.maxLevel, var2);
      var11 = getMaxLevelOf(var11, var3);
      var11 = getMaxLevelOf(var11, var4);
      var11 = getMaxLevelOf(var11, var5);
      var11 = getMaxLevelOf(var11, var6);
      var11 = getMaxLevelOf(var11, var7);
      var11 = getMaxLevelOf(var11, var8);
      var11 = getMaxLevelOf(var11, var9);
      newSquareList.clear();

      IsoGridSquare var12;
      int var13;
      int var14;
      for(var13 = var10; var13 <= var11; ++var13) {
         for(var14 = 0; var14 < 8; ++var14) {
            if (var3 != null) {
               var12 = var3.getGridSquare(var14, 7, var13);
               if (var12 != null && !var12.getObjects().isEmpty()) {
                  this.EnsureSurroundNotNullX(var14, 0, var13);
               }
            }

            if (var5 != null) {
               var12 = var5.getGridSquare(var14, 0, var13);
               if (var12 != null && !var12.getObjects().isEmpty()) {
                  this.EnsureSurroundNotNullX(var14, 7, var13);
               }
            }
         }

         for(var14 = 0; var14 < 8; ++var14) {
            if (var2 != null) {
               var12 = var2.getGridSquare(7, var14, var13);
               if (var12 != null && !var12.getObjects().isEmpty()) {
                  this.EnsureSurroundNotNullY(0, var14, var13);
               }
            }

            if (var4 != null) {
               var12 = var4.getGridSquare(0, var14, var13);
               if (var12 != null && !var12.getObjects().isEmpty()) {
                  this.EnsureSurroundNotNullY(7, var14, var13);
               }
            }
         }

         if (var6 != null) {
            var12 = var6.getGridSquare(7, 7, var13);
            if (var12 != null && !var12.getObjects().isEmpty()) {
               this.EnsureSurroundNotNull(0, 0, var13);
            }
         }

         if (var7 != null) {
            var12 = var7.getGridSquare(0, 7, var13);
            if (var12 != null && !var12.getObjects().isEmpty()) {
               this.EnsureSurroundNotNull(7, 0, var13);
            }
         }

         if (var8 != null) {
            var12 = var8.getGridSquare(0, 0, var13);
            if (var12 != null && !var12.getObjects().isEmpty()) {
               this.EnsureSurroundNotNull(7, 7, var13);
            }
         }

         if (var9 != null) {
            var12 = var9.getGridSquare(7, 0, var13);
            if (var12 != null && !var12.getObjects().isEmpty()) {
               this.EnsureSurroundNotNull(0, 7, var13);
            }
         }
      }

      for(var13 = var10; var13 <= var11; ++var13) {
         for(var14 = 0; var14 < 8; ++var14) {
            if (var3 != null) {
               var12 = this.getGridSquare(var14, 0, var13);
               if (var12 != null && !var12.getObjects().isEmpty()) {
                  var3.EnsureSurroundNotNullX(var14, 7, var13);
               }
            }

            if (var5 != null) {
               var12 = this.getGridSquare(var14, 7, var13);
               if (var12 != null && !var12.getObjects().isEmpty()) {
                  var5.EnsureSurroundNotNullX(var14, 0, var13);
               }
            }
         }

         for(var14 = 0; var14 < 8; ++var14) {
            if (var2 != null) {
               var12 = this.getGridSquare(0, var14, var13);
               if (var12 != null && !var12.getObjects().isEmpty()) {
                  var2.EnsureSurroundNotNullY(7, var14, var13);
               }
            }

            if (var4 != null) {
               var12 = this.getGridSquare(7, var14, var13);
               if (var12 != null && !var12.getObjects().isEmpty()) {
                  var4.EnsureSurroundNotNullY(0, var14, var13);
               }
            }
         }

         if (var6 != null) {
            var12 = this.getGridSquare(0, 0, var13);
            if (var12 != null && !var12.getObjects().isEmpty()) {
               var6.EnsureSurroundNotNull(7, 7, var13);
            }
         }

         if (var7 != null) {
            var12 = this.getGridSquare(7, 0, var13);
            if (var12 != null && !var12.getObjects().isEmpty()) {
               var7.EnsureSurroundNotNull(0, 7, var13);
            }
         }

         if (var8 != null) {
            var12 = this.getGridSquare(7, 7, var13);
            if (var12 != null && !var12.getObjects().isEmpty()) {
               var8.EnsureSurroundNotNull(0, 0, var13);
            }
         }

         if (var9 != null) {
            var12 = this.getGridSquare(0, 7, var13);
            if (var12 != null && !var12.getObjects().isEmpty()) {
               var9.EnsureSurroundNotNull(7, 0, var13);
            }
         }
      }

      for(var13 = 0; var13 < newSquareList.size(); ++var13) {
         var12 = (IsoGridSquare)newSquareList.get(var13);
         var12.RecalcAllWithNeighbours(true);
      }

      newSquareList.clear();
      GameProfiler.ProfileArea var21 = GameProfiler.getInstance().startIfEnabled("Recalc Nav");

      int var15;
      for(var14 = this.minLevel; var14 <= this.maxLevel; ++var14) {
         IsoGridSquare var18;
         IsoGridSquare var24;
         for(var15 = 0; var15 < 8; ++var15) {
            for(int var16 = 0; var16 < 8; ++var16) {
               var12 = this.getGridSquare(var16, var15, var14);
               if (var12 != null) {
                  if (var16 == 0 || var16 == 7 || var15 == 0 || var15 == 7) {
                     IsoWorld.instance.CurrentCell.DoGridNav(var12, IsoGridSquare.cellGetSquare);

                     for(int var17 = -1; var17 <= 1; ++var17) {
                        if (var16 == 0) {
                           this.RecalcAllWithNeighbour(var12, IsoDirections.W, var17);
                           this.RecalcAllWithNeighbour(var12, IsoDirections.NW, var17);
                           this.RecalcAllWithNeighbour(var12, IsoDirections.SW, var17);
                        } else if (var16 == 7) {
                           this.RecalcAllWithNeighbour(var12, IsoDirections.E, var17);
                           this.RecalcAllWithNeighbour(var12, IsoDirections.NE, var17);
                           this.RecalcAllWithNeighbour(var12, IsoDirections.SE, var17);
                        }

                        if (var15 == 0) {
                           this.RecalcAllWithNeighbour(var12, IsoDirections.N, var17);
                           if (var16 != 0) {
                              this.RecalcAllWithNeighbour(var12, IsoDirections.NW, var17);
                           }

                           if (var16 != 7) {
                              this.RecalcAllWithNeighbour(var12, IsoDirections.NE, var17);
                           }
                        } else if (var15 == 7) {
                           this.RecalcAllWithNeighbour(var12, IsoDirections.S, var17);
                           if (var16 != 0) {
                              this.RecalcAllWithNeighbour(var12, IsoDirections.SW, var17);
                           }

                           if (var16 != 7) {
                              this.RecalcAllWithNeighbour(var12, IsoDirections.SE, var17);
                           }
                        }
                     }

                     var24 = var12.nav[IsoDirections.N.index()];
                     var18 = var12.nav[IsoDirections.S.index()];
                     IsoGridSquare var19 = var12.nav[IsoDirections.W.index()];
                     IsoGridSquare var20 = var12.nav[IsoDirections.E.index()];
                     if (var24 != null && var19 != null && (var16 == 0 || var15 == 0)) {
                        this.RecalcAllWithNeighbour(var24, IsoDirections.W, 0);
                     }

                     if (var24 != null && var20 != null && (var16 == 7 || var15 == 0)) {
                        this.RecalcAllWithNeighbour(var24, IsoDirections.E, 0);
                     }

                     if (var18 != null && var19 != null && (var16 == 0 || var15 == 7)) {
                        this.RecalcAllWithNeighbour(var18, IsoDirections.W, 0);
                     }

                     if (var18 != null && var20 != null && (var16 == 7 || var15 == 7)) {
                        this.RecalcAllWithNeighbour(var18, IsoDirections.E, 0);
                     }
                  }

                  IsoRoom var25 = var12.getRoom();
                  if (var25 != null) {
                     var25.addSquare(var12);
                  }
               }
            }
         }

         IsoGridSquare var22 = this.getGridSquare(0, 0, var14);
         if (var22 != null) {
            var22.RecalcAllWithNeighbours(true);
         }

         IsoGridSquare var23 = this.getGridSquare(7, 0, var14);
         if (var23 != null) {
            var23.RecalcAllWithNeighbours(true);
         }

         var24 = this.getGridSquare(0, 7, var14);
         if (var24 != null) {
            var24.RecalcAllWithNeighbours(true);
         }

         var18 = this.getGridSquare(7, 7, var14);
         if (var18 != null) {
            var18.RecalcAllWithNeighbours(true);
         }
      }

      GameProfiler.getInstance().end(var21);
      this.fixObjectAmbientEmittersOnAdjacentChunks(var4, var5);
      if (var2 != null) {
         var2.checkLightingLater_AllPlayers_AllLevels();
      }

      if (var3 != null) {
         var3.checkLightingLater_AllPlayers_AllLevels();
      }

      if (var4 != null) {
         var4.checkLightingLater_AllPlayers_AllLevels();
      }

      if (var5 != null) {
         var5.checkLightingLater_AllPlayers_AllLevels();
      }

      if (var6 != null) {
         var6.checkLightingLater_AllPlayers_AllLevels();
      }

      if (var7 != null) {
         var7.checkLightingLater_AllPlayers_AllLevels();
      }

      if (var8 != null) {
         var8.checkLightingLater_AllPlayers_AllLevels();
      }

      if (var9 != null) {
         var9.checkLightingLater_AllPlayers_AllLevels();
      }

      for(var14 = 0; var14 < IsoPlayer.numPlayers; ++var14) {
         LosUtil.cachecleared[var14] = true;
      }

      IsoLightSwitch.chunkLoaded(this);
      GameProfiler.ProfileArea var26 = GameProfiler.getInstance().startIfEnabled("Recreate Level Cutaway");

      for(var15 = this.minLevel; var15 <= this.maxLevel; ++var15) {
         this.getCutawayData().recreateLevel(var15);
      }

      GameProfiler.getInstance().end(var26);
   }

   private void fixObjectAmbientEmittersOnAdjacentChunks(IsoChunk var1, IsoChunk var2) {
      if (!GameServer.bServer) {
         if (var1 != null || var2 != null) {
            for(int var3 = 0; var3 < 64; ++var3) {
               int var4;
               IsoGridSquare var5;
               if (var1 != null) {
                  for(var4 = 0; var4 < 8; ++var4) {
                     var5 = var1.getGridSquare(0, var4, var3);
                     this.fixObjectAmbientEmittersOnSquare(var5, false);
                  }
               }

               if (var2 != null) {
                  for(var4 = 0; var4 < 8; ++var4) {
                     var5 = var2.getGridSquare(var4, 0, var3);
                     this.fixObjectAmbientEmittersOnSquare(var5, true);
                  }
               }
            }

         }
      }
   }

   private void fixObjectAmbientEmittersOnSquare(IsoGridSquare var1, boolean var2) {
      if (!FMODAmbientWalls.ENABLE) {
         if (var1 != null && !var1.getSpecialObjects().isEmpty()) {
            IsoObject var3 = var1.getDoor(var2);
            if (var3 instanceof IsoDoor && ((IsoDoor)var3).isExterior() && !var3.hasObjectAmbientEmitter()) {
               var3.addObjectAmbientEmitter((new ObjectAmbientEmitters.DoorLogic()).init(var3));
            }

            IsoWindow var4 = var1.getWindow(var2);
            if (var4 != null && var4.isExterior() && !var4.hasObjectAmbientEmitter()) {
               var4.addObjectAmbientEmitter((new ObjectAmbientEmitters.WindowLogic()).init(var4));
            }

         }
      }
   }

   /** @deprecated */
   @Deprecated
   public void recalcNeighboursNow() {
      IsoCell var1 = IsoWorld.instance.CurrentCell;

      int var2;
      int var3;
      int var4;
      IsoGridSquare var5;
      for(var2 = 0; var2 < 8; ++var2) {
         for(var3 = 0; var3 < 8; ++var3) {
            for(var4 = this.minLevel; var4 <= this.maxLevel; ++var4) {
               var5 = this.getGridSquare(var2, var3, var4);
               if (var5 != null) {
                  if (var4 > 0 && !var5.getObjects().isEmpty()) {
                     var5.EnsureSurroundNotNull();

                     for(int var6 = var4 - 1; var6 > this.minLevel; --var6) {
                        IsoGridSquare var7 = this.getGridSquare(var2, var3, var6);
                        if (var7 == null) {
                           var7 = IsoGridSquare.getNew(var1, (SliceY)null, this.wx * 8 + var2, this.wy * 8 + var3, var6);
                           var1.ConnectNewSquare(var7, false);
                           this.assignRoom(var7);
                        }
                     }
                  }

                  var5.RecalcProperties();
               }
            }
         }
      }

      for(var2 = this.minLevel; var2 <= this.maxLevel; ++var2) {
         IsoGridSquare var8;
         for(var3 = -1; var3 < 9; ++var3) {
            var8 = var1.getGridSquare(this.wx * 8 + var3, this.wy * 8 - 1, var2);
            if (var8 != null && !var8.getObjects().isEmpty()) {
               var8.EnsureSurroundNotNull();
            }

            var8 = var1.getGridSquare(this.wx * 8 + var3, this.wy * 8 + 8, var2);
            if (var8 != null && !var8.getObjects().isEmpty()) {
               var8.EnsureSurroundNotNull();
            }
         }

         for(var3 = 0; var3 < 8; ++var3) {
            var8 = var1.getGridSquare(this.wx * 8 - 1, this.wy * 8 + var3, var2);
            if (var8 != null && !var8.getObjects().isEmpty()) {
               var8.EnsureSurroundNotNull();
            }

            var8 = var1.getGridSquare(this.wx * 8 + 8, this.wy * 8 + var3, var2);
            if (var8 != null && !var8.getObjects().isEmpty()) {
               var8.EnsureSurroundNotNull();
            }
         }
      }

      for(var2 = 0; var2 < 8; ++var2) {
         for(var3 = 0; var3 < 8; ++var3) {
            for(var4 = this.minLevel; var4 <= this.maxLevel; ++var4) {
               var5 = this.getGridSquare(var2, var3, var4);
               if (var5 != null) {
                  var5.RecalcAllWithNeighbours(true);
                  IsoRoom var9 = var5.getRoom();
                  if (var9 != null) {
                     var9.addSquare(var5);
                  }
               }
            }
         }
      }

      for(var2 = 0; var2 < 8; ++var2) {
         for(var3 = 0; var3 < 8; ++var3) {
            for(var4 = this.minLevel; var4 <= this.maxLevel; ++var4) {
               var5 = this.getGridSquare(var2, var3, var4);
               if (var5 != null) {
                  var5.propertiesDirty = true;
               }
            }
         }
      }

      IsoLightSwitch.chunkLoaded(this);
   }

   public void updateBuildings() {
   }

   public static void updatePlayerInBullet() {
      ArrayList var0 = GameServer.getPlayers();
      Bullet.updatePlayerList(var0);
   }

   public void update() {
      if (!this.blendingDoneFull && !Arrays.equals(this.blendingModified, comparatorBool4)) {
         IsoWorld.instance.getBlending().applyBlending(this);
      }

      if (!this.attachmentsDoneFull && !Arrays.equals(this.attachmentsState, comparatorBool5)) {
         IsoWorld.instance.getAttachmentsHandler().applyAttachments(this);
      }

      if (!GameServer.bServer && (this.minLevelPhysics != this.minLevel || this.maxLevelPhysics != this.maxLevel)) {
         this.minLevelPhysics = this.minLevel;
         this.maxLevelPhysics = this.maxLevel;
         Bullet.setChunkMinMaxLevel(this.wx, this.wy, this.minLevel, this.maxLevel);
      }

      int var1;
      if (!this.loadedPhysics) {
         this.loadedPhysics = true;

         for(var1 = 0; var1 < this.vehicles.size(); ++var1) {
            ((BaseVehicle)this.vehicles.get(var1)).chunk = this;
         }
      }

      if (this.ragdollControllersForAddToWorld != null) {
         for(var1 = 0; var1 < this.ragdollControllersForAddToWorld.size(); ++var1) {
            ((IsoGameCharacter)this.ragdollControllersForAddToWorld.get(var1)).addToWorld();
         }

         this.ragdollControllersForAddToWorld.clear();
         this.ragdollControllersForAddToWorld = null;
      }

      this.updateVehicleStory();
   }

   public void updateVehicleStory() {
      if (this.bLoaded && this.m_vehicleStorySpawnData != null) {
         IsoMetaChunk var1 = IsoWorld.instance.getMetaChunk(this.wx, this.wy);
         if (var1 != null) {
            VehicleStorySpawnData var2 = this.m_vehicleStorySpawnData;

            for(int var3 = 0; var3 < var1.getZonesSize(); ++var3) {
               Zone var4 = var1.getZone(var3);
               if (var2.isValid(var4, this)) {
                  var2.m_story.randomizeVehicleStory(var4, this);
                  ++var4.hourLastSeen;
                  break;
               }
            }

         }
      }
   }

   public int squaresIndexOfLevel(int var1) {
      return var1 - this.minLevel;
   }

   public IsoGridSquare[] getSquaresForLevel(int var1) {
      return this.squares[this.squaresIndexOfLevel(var1)];
   }

   public void setSquare(int var1, int var2, int var3, IsoGridSquare var4) {
      assert var4 == null || var4.x - this.wx * 8 == var1 && var4.y - this.wy * 8 == var2 && var4.z == var3;

      boolean var5 = !this.isValidLevel(var3);
      this.setMinMaxLevel(PZMath.min(this.getMinLevel(), var3), PZMath.max(this.getMaxLevel(), var3));
      int var6 = this.squaresIndexOfLevel(var3);
      this.squares[var6][var1 + var2 * 8] = var4;
      if (var4 != null) {
         var4.chunk = this;
         var4.associatedBuilding = IsoWorld.instance.getMetaGrid().getAssociatedBuildingAt(var4.x, var4.y);
      }

      if (this.jobType != IsoChunk.JobType.SoftReset) {
         if (var5 && Thread.currentThread() == GameWindow.GameThread || Thread.currentThread() == GameServer.MainThread) {
            if (PathfindNative.USE_NATIVE_CODE) {
               PathfindNative.instance.addChunkToWorld(this);
            } else {
               PolygonalMap2.instance.addChunkToWorld(this);
            }
         }

      }
   }

   public int getMinLevel() {
      return this.minLevel;
   }

   public int getMaxLevel() {
      return this.maxLevel;
   }

   public boolean isValidLevel(int var1) {
      return var1 >= this.getMinLevel() && var1 <= this.getMaxLevel();
   }

   public void setMinMaxLevel(int var1, int var2) {
      if (var1 != this.minLevel || var2 != this.maxLevel) {
         for(int var3 = this.minLevel; var3 <= this.maxLevel; ++var3) {
            if (var3 < var1 || var3 > var2) {
               IsoChunkLevel var4 = this.levels[var3 - this.minLevel];
               if (var4 != null) {
                  var4.clear();
                  var4.release();
                  this.levels[var3 - this.minLevel] = null;
               }
            }
         }

         IsoChunkLevel[] var5 = new IsoChunkLevel[var2 - var1 + 1];

         int var6;
         for(var6 = var1; var6 <= var2; ++var6) {
            if (this.isValidLevel(var6)) {
               var5[var6 - var1] = this.levels[var6 - this.minLevel];
            } else {
               var5[var6 - var1] = IsoChunkLevel.alloc().init(this, var6);
            }
         }

         this.minLevel = var1;
         this.maxLevel = var2;
         this.levels = var5;
         this.squares = new IsoGridSquare[var2 - var1 + 1][];

         for(var6 = var1; var6 <= var2; ++var6) {
            this.squares[var6 - var1] = this.levels[var6 - var1].squares;
         }

      }
   }

   public IsoChunkLevel getLevelData(int var1) {
      return this.isValidLevel(var1) ? this.levels[var1 - this.minLevel] : null;
   }

   public IsoGridSquare getGridSquare(int var1, int var2, int var3) {
      if (var1 >= 0 && var1 < 8 && var2 >= 0 && var2 < 8 && var3 <= this.maxLevel && var3 >= this.minLevel) {
         int var4 = this.squaresIndexOfLevel(var3);
         return var4 < this.squares.length && var4 >= 0 ? this.squares[var4][var2 * 8 + var1] : null;
      } else {
         return null;
      }
   }

   public IsoRoom getRoom(long var1) {
      return this.lotheader.getRoom(var1);
   }

   public void removeFromWorld() {
      loadGridSquare.remove(this);
      if (GameClient.bClient && GameClient.instance.bConnected) {
         try {
            GameClient.instance.sendAddedRemovedItems(true);
         } catch (Exception var9) {
            ExceptionLogger.logException(var9);
         }
      }

      try {
         MapCollisionData.instance.removeChunkFromWorld(this);
         AnimalPopulationManager.getInstance().removeChunkFromWorld(this);
         ZombiePopulationManager.instance.removeChunkFromWorld(this);
         if (PathfindNative.USE_NATIVE_CODE) {
            PathfindNative.instance.removeChunkFromWorld(this);
         } else {
            PolygonalMap2.instance.removeChunkFromWorld(this);
         }

         this.collision.clear();
      } catch (Exception var8) {
         ExceptionLogger.logException(var8);
      }

      byte var1 = 64;

      int var2;
      for(var2 = this.minLevel; var2 <= this.maxLevel; ++var2) {
         for(int var3 = 0; var3 < var1; ++var3) {
            IsoGridSquare var4 = this.squares[this.squaresIndexOfLevel(var2)][var3];
            if (var4 != null) {
               RainManager.RemoveAllOn(var4);
               var4.clearWater();
               var4.clearPuddles();
               if (var4.getRoom() != null) {
                  var4.getRoom().removeSquare(var4);
               }

               if (var4.zone != null) {
                  var4.zone.removeSquare(var4);
               }

               ArrayList var5 = var4.getMovingObjects();

               int var6;
               IsoMovingObject var7;
               for(var6 = 0; var6 < var5.size(); ++var6) {
                  var7 = (IsoMovingObject)var5.get(var6);
                  if (var7 instanceof IsoSurvivor) {
                     IsoWorld.instance.CurrentCell.getSurvivorList().remove(var7);
                     var7.Despawn();
                  }

                  if (var7 instanceof IsoAnimal) {
                     if (GameClient.bClient) {
                        AnimalInstanceManager.getInstance().remove((IsoAnimal)var7);
                     }

                     if (GameServer.bServer) {
                        AnimalOwnershipManager.getInstance().setOwnershipServer(((IsoAnimal)var7).getNetworkCharacterAI(), (UdpConnection)null);
                     }
                  }

                  var7.removeFromWorld();
                  var7.current = var7.last = null;
                  if (!var5.contains(var7)) {
                     --var6;
                  }
               }

               var5.clear();

               for(var6 = 0; var6 < var4.getObjects().size(); ++var6) {
                  IsoObject var11 = (IsoObject)var4.getObjects().get(var6);
                  var11.removeFromWorldToMeta();
               }

               for(var6 = 0; var6 < var4.getStaticMovingObjects().size(); ++var6) {
                  var7 = (IsoMovingObject)var4.getStaticMovingObjects().get(var6);
                  var7.removeFromWorld();
               }

               this.disconnectFromAdjacentChunks(var4);
               var4.softClear();
               var4.chunk = null;
            }
         }
      }

      for(var2 = 0; var2 < this.vehicles.size(); ++var2) {
         BaseVehicle var10 = (BaseVehicle)this.vehicles.get(var2);
         if (IsoWorld.instance.CurrentCell.getVehicles().contains(var10) || IsoWorld.instance.CurrentCell.addVehicles.contains(var10)) {
            DebugLog.log("IsoChunk.removeFromWorld: vehicle wasn't removed from world id=" + var10.VehicleID);
            var10.removeFromWorld();
         }
      }

      if (!GameServer.bServer) {
         FBORenderOcclusion.getInstance().removeChunkFromWorld(this);
         FBORenderChunkManager.instance.freeChunk(this);
         this.m_cutawayData.removeFromWorld();
         this.getVispolyData().removeFromWorld();
      }

   }

   private void disconnectFromAdjacentChunks(IsoGridSquare var1) {
      int var2 = PZMath.coordmodulo(var1.x, 8);
      int var3 = PZMath.coordmodulo(var1.y, 8);
      if (var2 == 0 || var2 == 7 || var3 == 0 || var3 == 7) {
         int var4 = IsoDirections.N.index();
         int var5 = IsoDirections.S.index();
         if (var1.nav[var4] != null && var1.nav[var4].chunk != var1.chunk) {
            var1.nav[var4].nav[var5] = null;
            var1.nav[var4].s = null;
         }

         var4 = IsoDirections.NW.index();
         var5 = IsoDirections.SE.index();
         if (var1.nav[var4] != null && var1.nav[var4].chunk != var1.chunk) {
            var1.nav[var4].nav[var5] = null;
            var1.nav[var4].se = null;
         }

         var4 = IsoDirections.W.index();
         var5 = IsoDirections.E.index();
         if (var1.nav[var4] != null && var1.nav[var4].chunk != var1.chunk) {
            var1.nav[var4].nav[var5] = null;
            var1.nav[var4].e = null;
         }

         var4 = IsoDirections.SW.index();
         var5 = IsoDirections.NE.index();
         if (var1.nav[var4] != null && var1.nav[var4].chunk != var1.chunk) {
            var1.nav[var4].nav[var5] = null;
            var1.nav[var4].ne = null;
         }

         var4 = IsoDirections.S.index();
         var5 = IsoDirections.N.index();
         if (var1.nav[var4] != null && var1.nav[var4].chunk != var1.chunk) {
            var1.nav[var4].nav[var5] = null;
            var1.nav[var4].n = null;
         }

         var4 = IsoDirections.SE.index();
         var5 = IsoDirections.NW.index();
         if (var1.nav[var4] != null && var1.nav[var4].chunk != var1.chunk) {
            var1.nav[var4].nav[var5] = null;
            var1.nav[var4].nw = null;
         }

         var4 = IsoDirections.E.index();
         var5 = IsoDirections.W.index();
         if (var1.nav[var4] != null && var1.nav[var4].chunk != var1.chunk) {
            var1.nav[var4].nav[var5] = null;
            var1.nav[var4].w = null;
         }

         var4 = IsoDirections.NE.index();
         var5 = IsoDirections.SW.index();
         if (var1.nav[var4] != null && var1.nav[var4].chunk != var1.chunk) {
            var1.nav[var4].nav[var5] = null;
            var1.nav[var4].sw = null;
         }

      }
   }

   public void doReuseGridsquares() {
      byte var1 = 64;

      for(int var2 = 0; var2 < this.squares.length; ++var2) {
         for(int var3 = 0; var3 < var1; ++var3) {
            IsoGridSquare var4 = this.squares[var2][var3];
            if (var4 != null) {
               LuaEventManager.triggerEvent("ReuseGridsquare", var4);

               for(int var5 = 0; var5 < var4.getObjects().size(); ++var5) {
                  IsoObject var6 = (IsoObject)var4.getObjects().get(var5);
                  if (var6 instanceof IsoTree) {
                     var6.reset();
                     CellLoader.isoTreeCache.add((IsoTree)var6);
                  } else if (var6 instanceof IsoObject && var6.getObjectName().equals("IsoObject")) {
                     var6.reset();
                     CellLoader.isoObjectCache.add(var6);
                  } else {
                     var6.reuseGridSquare();
                  }
               }

               var4.discard();
               this.squares[var2][var3] = null;
            }
         }
      }

      this.resetForStore();

      assert !IsoChunkMap.chunkStore.contains(this);

      IsoChunkMap.chunkStore.add(this);
   }

   private static int bufferSize(int var0) {
      return (var0 + 65536 - 1) / 65536 * 65536;
   }

   private static ByteBuffer ensureCapacity(ByteBuffer var0, int var1) {
      if (var0 == null || var0.capacity() < var1) {
         var0 = ByteBuffer.allocate(bufferSize(var1));
      }

      return var0;
   }

   private static ByteBuffer ensureCapacity(ByteBuffer var0) {
      if (var0 == null) {
         return ByteBuffer.allocate(65536);
      } else if (var0.capacity() - var0.position() < 65536) {
         ByteBuffer var1 = ensureCapacity((ByteBuffer)null, var0.position() + 65536);
         return var1.put(var0.array(), 0, var0.position());
      } else {
         return var0;
      }
   }

   private boolean[] readFlags(ByteBuffer var1, int var2) {
      boolean[] var3 = new boolean[var2];
      byte var4 = var1.get();
      int var5 = 1;

      for(byte var6 = 0; var6 < var2; ++var6) {
         var3[var6] = (var4 & var5) == var5;
         var5 *= 2;
      }

      return var3;
   }

   private void writeFlags(ByteBuffer var1, boolean[] var2) {
      int var3 = 0;

      for(byte var4 = 0; var4 < var2.length; ++var4) {
         var3 += (var2[var4] ? 1 : 0) << var4;
      }

      var1.put((byte)var3);
   }

   public void LoadFromDisk() throws IOException {
      this.LoadFromDiskOrBuffer((ByteBuffer)null);
   }

   public void LoadFromDiskOrBuffer(ByteBuffer var1) throws IOException {
      sanityCheck.beginLoad(this);

      try {
         ByteBuffer var2;
         if (var1 == null) {
            SliceBufferLoad = SafeRead(prefix, this.wx, this.wy, SliceBufferLoad);
            var2 = SliceBufferLoad;
         } else {
            var2 = var1;
         }

         int var3 = this.wx * 8;
         int var4 = this.wy * 8;
         var3 /= IsoCell.CellSizeInSquares;
         var4 /= IsoCell.CellSizeInSquares;
         String var5 = ChunkMapFilenames.instance.getHeader(var3, var4);
         if (IsoLot.InfoHeaders.containsKey(var5)) {
            this.lotheader = (LotHeader)IsoLot.InfoHeaders.get(var5);
         }

         IsoCell.wx = this.wx;
         IsoCell.wy = this.wy;
         boolean var6 = var2.get() == 1;
         int var7 = var2.getInt();
         if (var6) {
            DebugLog.log("WorldVersion = " + var7 + ", debug = " + var6);
         }

         if (var7 > 219) {
            throw new RuntimeException("unknown world version " + var7 + " while reading chunk " + this.wx + "," + this.wy);
         }

         this.bFixed2x = true;
         int var8 = var2.getInt();
         sanityCheck.checkLength((long)var8, (long)var2.limit());
         long var9 = var2.getLong();
         crcLoad.reset();
         crcLoad.update(var2.array(), 17, var2.limit() - 1 - 4 - 4 - 8);
         sanityCheck.checkCRC(var9, crcLoad.getValue());
         if (var7 >= 209) {
            this.blendingDoneFull = var2.get() == 1;
         }

         int var11;
         if (var7 >= 210) {
            this.blendingModified = this.readFlags(var2, this.blendingModified.length);
            this.blendingDonePartial = var2.get() == 1;
            if (!Arrays.equals(this.blendingModified, comparatorBool4) && this.blendingDonePartial) {
               for(var11 = 0; var11 < 4; ++var11) {
                  this.blendingDepth[var11] = var2.get();
               }
            }
         }

         if (var7 >= 214) {
            this.attachmentsDoneFull = var2.get() == 1;
            this.attachmentsState = this.readFlags(var2, this.attachmentsState.length);
         }

         var11 = SandboxOptions.getInstance().BloodSplatLifespanDays.getValue();
         float var12 = (float)GameTime.getInstance().getWorldAgeHours();
         int var13;
         int var14;
         if (206 <= var7) {
            var14 = var2.getInt();
            var13 = var2.getInt();
         } else {
            var14 = 7;
            var13 = 0;
         }

         this.setMinMaxLevel(var13, var14);
         int var15 = var2.getInt();

         for(int var16 = 0; var16 < var15; ++var16) {
            IsoFloorBloodSplat var17 = new IsoFloorBloodSplat();
            var17.load(var2, var7);
            if (var17.worldAge > var12) {
               var17.worldAge = var12;
            }

            if (var11 <= 0 || !(var12 - var17.worldAge >= (float)(var11 * 24))) {
               if (var17.Type < 8) {
                  this.nextSplatIndex = var17.index % 10;
               }

               this.FloorBloodSplats.add(var17);
            }
         }

         IsoMetaGrid var33 = IsoWorld.instance.getMetaGrid();

         int var18;
         int var21;
         for(int var34 = 0; var34 < 8; ++var34) {
            for(var18 = 0; var18 < 8; ++var18) {
               long var19;
               if (var7 >= 206) {
                  var19 = var2.getLong();
               } else {
                  var19 = (long)var2.get();
                  var19 <<= 32;
               }

               for(var21 = var13; var21 <= var14; ++var21) {
                  IsoGridSquare var22 = null;
                  boolean var23 = false;
                  if ((var19 & 1L << var21 + 32) != 0L) {
                     var23 = true;
                  }

                  if (var23) {
                     if (var22 == null) {
                        if (IsoGridSquare.loadGridSquareCache != null) {
                           var22 = IsoGridSquare.getNew(IsoGridSquare.loadGridSquareCache, IsoWorld.instance.CurrentCell, (SliceY)null, var34 + this.wx * 8, var18 + this.wy * 8, var21);
                        } else {
                           var22 = IsoGridSquare.getNew(IsoWorld.instance.CurrentCell, (SliceY)null, var34 + this.wx * 8, var18 + this.wy * 8, var21);
                        }
                     }

                     var22.chunk = this;
                     if (this.lotheader != null) {
                        RoomDef var24 = var33.getRoomAt(var22.x, var22.y, var22.z);
                        long var25 = var24 != null ? var24.ID : -1L;
                        var22.setRoomID(var25);
                        var24 = var33.getEmptyOutsideAt(var22.x, var22.y, var22.z);
                        if (var24 != null) {
                           IsoRoom var27 = this.getRoom(var24.ID);
                           var22.roofHideBuilding = var27 == null ? null : var27.building;
                        }
                     }

                     var22.ResetIsoWorldRegion();
                     this.setSquare(var34, var18, var21, var22);
                  }

                  if (var23 && var22 != null) {
                     var22.load(var2, var7, var6);
                     var22.FixStackableObjects();
                     if (this.jobType == IsoChunk.JobType.SoftReset) {
                        if (!var22.getStaticMovingObjects().isEmpty()) {
                           var22.getStaticMovingObjects().clear();
                        }

                        for(int var46 = 0; var46 < var22.getObjects().size(); ++var46) {
                           IsoObject var47 = (IsoObject)var22.getObjects().get(var46);
                           var47.softReset();
                           if (var47.getObjectIndex() == -1) {
                              --var46;
                           }
                        }

                        var22.setOverlayDone(false);
                     }
                  }
               }
            }
         }

         this.getErosionData().load(var2, var7);
         this.getErosionData().set(this);
         short var35 = var2.getShort();
         if (var35 > 0 && this.generatorsTouchingThisChunk == null) {
            this.generatorsTouchingThisChunk = new ArrayList();
         }

         if (this.generatorsTouchingThisChunk != null) {
            this.generatorsTouchingThisChunk.clear();
         }

         int var20;
         int var37;
         byte var40;
         for(var18 = 0; var18 < var35; ++var18) {
            var37 = var2.getInt();
            var20 = var2.getInt();
            var40 = var2.get();
            IsoGameCharacter.Location var43 = new IsoGameCharacter.Location(var37, var20, var40);
            this.generatorsTouchingThisChunk.add(var43);
         }

         this.vehicles.clear();
         if (!GameClient.bClient) {
            short var36 = var2.getShort();

            for(var37 = 0; var37 < var36; ++var37) {
               byte var38 = var2.get();
               var40 = var2.get();
               byte var44 = var2.get();
               IsoObject var45 = IsoObject.factoryFromFileInput(IsoWorld.instance.CurrentCell, var2);
               if (var45 != null && var45 instanceof BaseVehicle) {
                  IsoGridSquare var48 = this.getGridSquare(var38, var40, var44);
                  var45.square = var48;
                  ((IsoMovingObject)var45).current = var48;

                  try {
                     var45.load(var2, var7, var6);
                     this.vehicles.add((BaseVehicle)var45);
                     addFromCheckedVehicles((BaseVehicle)var45);
                     if (this.jobType == IsoChunk.JobType.SoftReset) {
                        var45.softReset();
                     }
                  } catch (Exception var31) {
                     throw new RuntimeException(var31);
                  }
               }
            }

            this.lootRespawnHour = var2.getInt();
            if (var7 >= 206) {
               short var39 = var2.getShort();

               for(var20 = 0; var20 < var39; ++var20) {
                  long var42 = var2.getLong();
                  this.addSpawnedRoom(var42);
               }
            } else {
               byte var41 = var2.get();

               for(var20 = 0; var20 < var41; ++var20) {
                  var21 = var2.getInt();
                  this.addSpawnedRoom(RoomID.makeID(this.wx / 8, this.wy / 8, var21));
               }
            }
         }
      } finally {
         sanityCheck.endLoad(this);
         this.bFixed2x = true;
      }

      if (this.getGridSquare(0, 0, 0) == null && this.getGridSquare(9, 9, 0) == null) {
         if (var1 != null) {
            var1.rewind();
         }

         this.LoadFromDiskOrBuffer(var1);
         throw new RuntimeException("black chunk " + this.wx + "," + this.wy);
      }
   }

   public void doLoadGridsquare() {
      if (this.jobType == IsoChunk.JobType.SoftReset) {
         this.m_spawnedRooms.clear();
      }

      if (!GameServer.bServer) {
         this.loadInMainThread();
      }

      int var1 = PZMath.fastfloor((float)this.wx / (float)IsoCell.CellSizeInChunks);
      int var2 = PZMath.fastfloor((float)this.wy / (float)IsoCell.CellSizeInChunks);
      IsoMetaCell var3 = IsoWorld.instance.getMetaGrid().getCellData(var1, var2);
      if (var3 != null) {
         var3.checkAnimalZonesGenerated(this.wx, this.wy);
      }

      if (this.addZombies && !VehiclesDB2.instance.isChunkSeen(this.wx, this.wy)) {
         try {
            this.AddVehicles();
         } catch (Throwable var15) {
            ExceptionLogger.logException(var15);
         }
      }

      if (!GameClient.bClient) {
         this.AddZombieZoneStory();
      }

      this.AddRanchAnimals();
      this.CheckGrassRegrowth();
      VehiclesDB2.instance.setChunkSeen(this.wx, this.wy);
      int var5;
      if (this.addZombies) {
         if (IsoWorld.instance.getTimeSinceLastSurvivorInHorde() > 0) {
            IsoWorld.instance.setTimeSinceLastSurvivorInHorde(IsoWorld.instance.getTimeSinceLastSurvivorInHorde() - 1);
         }

         this.addSurvivorInHorde(false);
         WGChunk var4 = IsoWorld.instance.getWgChunk();

         for(var5 = 0; var5 < this.proceduralZombieSquares.size(); ++var5) {
            IsoGridSquare var6 = (IsoGridSquare)this.proceduralZombieSquares.get(var5);
            var4.addZombieToSquare(var6);
         }
      }

      this.proceduralZombieSquares.clear();
      this.update();
      this.addRagdollControllers();
      if (!GameServer.bServer) {
         FliesSound.instance.chunkLoaded(this);
         NearestWalls.chunkLoaded(this);
      }

      int var17;
      if (this.addZombies) {
         var17 = 5 + SandboxOptions.instance.TimeSinceApo.getValue();
         var17 = Math.min(20, var17);
         if (Rand.Next(var17) == 0) {
            this.AddCorpses(this.wx, this.wy);
         }

         if (Rand.Next(var17 * 2) == 0) {
            this.AddBlood(this.wx, this.wy);
         }
      }

      LoadGridsquarePerformanceWorkaround.init(this.wx, this.wy);
      int var8;
      if (!GameClient.bClient) {
         for(var17 = 0; var17 < this.vehicles.size(); ++var17) {
            BaseVehicle var18 = (BaseVehicle)this.vehicles.get(var17);
            if (!var18.addedToWorld && VehiclesDB2.instance.isVehicleLoaded(var18)) {
               var18.removeFromSquare();
               this.vehicles.remove(var17);
               --var17;
            } else {
               if (!var18.addedToWorld) {
                  var18.addToWorld();
               }

               if (var18.sqlID == -1) {
                  assert false;

                  if (var18.square == null) {
                     float var19 = 5.0E-4F;
                     int var7 = this.wx * 8;
                     var8 = this.wy * 8;
                     int var9 = var7 + 8;
                     int var10 = var8 + 8;
                     float var11 = PZMath.clamp(var18.getX(), (float)var7 + var19, (float)var9 - var19);
                     float var12 = PZMath.clamp(var18.getY(), (float)var8 + var19, (float)var10 - var19);
                     var18.square = this.getGridSquare(PZMath.fastfloor(var11) - this.wx * 8, PZMath.fastfloor(var12) - this.wy * 8, 0);
                  }

                  VehiclesDB2.instance.addVehicle(var18);
               }
            }
         }
      }

      this.m_treeCount = 0;
      this.m_scavengeZone = null;
      this.m_numberOfWaterTiles = 0;

      int var20;
      for(var17 = this.minLevel; var17 <= this.maxLevel; ++var17) {
         for(var5 = 0; var5 < 8; ++var5) {
            for(var20 = 0; var20 < 8; ++var20) {
               IsoGridSquare var22 = this.getGridSquare(var5, var20, var17);
               if (var22 != null && !var22.getObjects().isEmpty()) {
                  for(var8 = 0; var8 < var22.getObjects().size(); ++var8) {
                     IsoObject var30 = (IsoObject)var22.getObjects().get(var8);
                     var30.addToWorld();
                     if (var30.getSprite() != null && var30.getSprite().getProperties().Is("fuelAmount")) {
                        var30.getPipedFuelAmount();
                     }

                     if (var17 == 0 && var30.getSprite() != null && var30.getSprite().getProperties().Is(IsoFlagType.water)) {
                        ++this.m_numberOfWaterTiles;
                     }
                  }

                  if (var22.HasTree()) {
                     ++this.m_treeCount;
                  }

                  if (this.jobType != IsoChunk.JobType.SoftReset) {
                     ErosionMain.LoadGridsquare(var22);
                  }

                  if (this.addZombies) {
                     MapObjects.newGridSquare(var22);
                  }

                  MapObjects.loadGridSquare(var22);
                  this.addRatsAfterLoading(var22);

                  try {
                     LuaEventManager.triggerEvent("LoadGridsquare", var22);
                     LoadGridsquarePerformanceWorkaround.LoadGridsquare(var22);
                  } catch (Throwable var14) {
                     ExceptionLogger.logException(var14);
                  }
               }

               if (var22 != null && !var22.getStaticMovingObjects().isEmpty()) {
                  for(var8 = 0; var8 < var22.getStaticMovingObjects().size(); ++var8) {
                     IsoMovingObject var31 = (IsoMovingObject)var22.getStaticMovingObjects().get(var8);
                     var31.addToWorld();
                  }
               }
            }
         }
      }

      if (this.jobType != IsoChunk.JobType.SoftReset) {
         ErosionMain.ChunkLoaded(this);
      }

      if (this.jobType != IsoChunk.JobType.SoftReset) {
         SGlobalObjects.chunkLoaded(this.wx, this.wy);
      }

      ReanimatedPlayers.instance.addReanimatedPlayersToChunk(this);
      if (this.jobType != IsoChunk.JobType.SoftReset) {
         MapCollisionData.instance.addChunkToWorld(this);
         AnimalPopulationManager.getInstance().addChunkToWorld(this);
         ZombiePopulationManager.instance.addChunkToWorld(this);
         if (PathfindNative.USE_NATIVE_CODE) {
            PathfindNative.instance.addChunkToWorld(this);
         } else {
            PolygonalMap2.instance.addChunkToWorld(this);
         }

         IsoGenerator.chunkLoaded(this);
         LootRespawn.chunkLoaded(this);
      }

      if (!GameServer.bServer) {
         ArrayList var21 = IsoWorld.instance.CurrentCell.roomLights;

         for(var5 = 0; var5 < this.roomLights.size(); ++var5) {
            IsoRoomLight var23 = (IsoRoomLight)this.roomLights.get(var5);
            if (!var21.contains(var23)) {
               var21.add(var23);
            }
         }
      }

      this.roomLights.clear();
      if (this.jobType != IsoChunk.JobType.SoftReset) {
         tempBuildingDefs.clear();
         IsoWorld.instance.MetaGrid.getBuildingsIntersecting(this.wx * 8 - 1, this.wy * 8 - 1, 10, 10, tempBuildingDefs);
         tempBuildings.clear();

         for(var17 = 0; var17 < tempBuildingDefs.size(); ++var17) {
            BuildingDef var24 = (BuildingDef)tempBuildingDefs.get(var17);
            RoomDef var25 = (RoomDef)var24.rooms.get(0);
            IsoBuilding var26 = var25.getIsoRoom().getBuilding();
            tempBuildings.add(var26);
         }

         this.randomizeBuildingsEtc(tempBuildings);
      }

      this.checkAdjacentChunks();

      try {
         if (GameServer.bServer && this.jobType != IsoChunk.JobType.SoftReset) {
            for(var17 = 0; var17 < GameServer.udpEngine.connections.size(); ++var17) {
               UdpConnection var27 = (UdpConnection)GameServer.udpEngine.connections.get(var17);
               if (!var27.chunkObjectState.isEmpty()) {
                  for(var20 = 0; var20 < var27.chunkObjectState.size(); var20 += 2) {
                     short var28 = var27.chunkObjectState.get(var20);
                     short var32 = var27.chunkObjectState.get(var20 + 1);
                     if (var28 == this.wx && var32 == this.wy) {
                        var27.chunkObjectState.remove(var20, 2);
                        var20 -= 2;
                        ByteBufferWriter var33 = var27.startPacket();
                        PacketTypes.PacketType.ChunkObjectState.doPacket(var33);
                        var33.putShort((short)this.wx);
                        var33.putShort((short)this.wy);

                        try {
                           if (this.saveObjectState(var33.bb)) {
                              PacketTypes.PacketType.ChunkObjectState.send(var27);
                           } else {
                              var27.cancelPacket();
                           }
                        } catch (Throwable var13) {
                           var13.printStackTrace();
                           var27.cancelPacket();
                        }
                     }
                  }
               }
            }
         }

         if (GameClient.bClient) {
            ByteBufferWriter var29 = GameClient.connection.startPacket();
            PacketTypes.PacketType.ChunkObjectState.doPacket(var29);
            var29.putShort((short)this.wx);
            var29.putShort((short)this.wy);
            PacketTypes.PacketType.ChunkObjectState.send(GameClient.connection);
         }
      } catch (Throwable var16) {
         ExceptionLogger.logException(var16);
      }

      this.loadedFrame = (long)IsoWorld.instance.getFrameNo();
      this.renderFrame = this.loadedFrame + (long)frameDelay;
      frameDelay = (frameDelay + 1) % 5;
      LuaEventManager.triggerEvent("LoadChunk", this);
   }

   private void addRatsAfterLoading(IsoGridSquare var1) {
      Zone var2 = var1.getZone();
      boolean var3 = this.addZombies && var1.hasTrash() && SandboxOptions.instance.getCurrentRatIndex() > 0 && var1.canSpawnVermin();
      int var4 = 300;
      if (Objects.equals(var1.getSquareZombiesType(), "StreetPoor") || Objects.equals(var1.getZoneType(), "TrailerPark")) {
         var4 /= 2;
      }

      if (Objects.equals(var1.getSquareZombiesType(), "Rich") || Objects.equals(var1.getLootZone(), "Rich")) {
         var4 *= 2;
      }

      if (var1.getZ() < 0) {
         var4 /= 2;
      }

      if (var3 && Rand.Next(var4) < SandboxOptions.instance.getCurrentRatIndex()) {
         boolean var5 = !var1.isOutside() && Rand.NextBool(3);
         int var6 = SandboxOptions.instance.getCurrentRatIndex() / 10;
         if (Objects.equals(var1.getSquareZombiesType(), "StreetPoor") || Objects.equals(var1.getZoneType(), "TrailerPark")) {
            var6 *= 2;
         }

         if (var6 < 1) {
            var6 = 1;
         }

         if (var6 > 9) {
            var6 = 9;
         }

         int var7 = Rand.Next(1, var6);
         String var8 = "rat";
         String var9 = "grey";
         if (var5) {
            var8 = "mouse";
            var9 = "deer";
         }

         if (var1.getBuilding() != null && (var1.getBuilding().hasRoom("laboratory") || var1.getBuilding().hasRoom("classroom") || var1.getBuilding().hasRoom("secondaryclassroom") || Objects.equals(var1.getZombiesType(), "University")) && !Rand.NextBool(3)) {
            var9 = "white";
         }

         IsoGridSquare var12;
         if (var1.isFree(true)) {
            String var10 = var8;
            if (var8.equals("rat") && Rand.NextBool(2)) {
               var10 = "ratfemale";
            }

            if (var8.equals("mouse") && Rand.NextBool(2)) {
               var10 = "mousefemale";
            }

            IsoAnimal var11 = new IsoAnimal(IsoWorld.instance.getCell(), var1.getX(), var1.getY(), var1.getZ(), var10, var9);
            var11.addToWorld();
            var11.randomizeAge();
            var12 = var1.getAdjacentSquare(IsoDirections.getRandom());
            if (Rand.NextBool(3)) {
               var11.setStateEventDelayTimer(0.0F);
            } else if (var12 != null && var12.isFree(true) && var12.isSolidFloor() && var1.canReachTo(var12)) {
               var11.fleeTo(var12);
            }
         }

         ArrayList var15 = new ArrayList();

         int var16;
         for(var16 = 0; var16 < var7; ++var16) {
            var12 = var1.getAdjacentSquare(IsoDirections.getRandom());
            if (var12 != null && var12.isFree(true) && var12.isSolidFloor() && !var15.contains(var12)) {
               IsoAnimal var13 = new IsoAnimal(IsoWorld.instance.getCell(), var12.getX(), var12.getY(), var12.getZ(), var8, var9);
               var13.addToWorld();
               var13.randomizeAge();
               IsoGridSquare var14 = var1.getAdjacentSquare(IsoDirections.getRandom());
               if (Rand.NextBool(3)) {
                  var13.setStateEventDelayTimer(0.0F);
               } else if (var14 != null && var14.isFree(true) && var14.isSolidFloor() && !var15.contains(var14) && var12.canReachTo(var14)) {
                  var13.fleeTo(var14);
               } else {
                  var15.add(var12);
               }
            }
         }

         var16 = Rand.Next(0, var6);

         for(int var17 = 0; var17 < var16; ++var17) {
            IsoGridSquare var19 = var1.getAdjacentSquare(IsoDirections.getRandom());
            if (var19 != null && var19.isFree(true) && var19.isSolidFloor()) {
               if (var5) {
                  this.addItemOnGround(var19, "Base.Dung_Mouse");
               } else {
                  this.addItemOnGround(var19, "Base.Dung_Rat");
               }
            }
         }

         IsoObject var18 = var1.getTrashReceptacle();
         if (var18 != null) {
            var16 = Rand.Next(0, var6);

            for(int var20 = 0; var20 < var16; ++var20) {
               InventoryItem var21 = InventoryItemFactory.CreateItem("Base.Dung_Rat");
               if (var5) {
                  var21 = InventoryItemFactory.CreateItem("Base.Dung_Mouse");
               } else {
                  var21 = InventoryItemFactory.CreateItem("Base.Dung_Rat");
               }

               var18.getContainer().addItem(var21);
            }
         }
      }

   }

   private void CheckGrassRegrowth() {
      IsoMetaChunk var1 = IsoWorld.instance.getMetaChunk(this.wx, this.wy);
      if (var1 != null) {
         for(int var2 = 0; var2 < var1.getZonesSize(); ++var2) {
            Zone var3 = var1.getZone(var2);
            if ("GrassRegrowth".equals(var3.name) && var3.getLastActionTimestamp() > 0) {
               int var4 = Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue() - var3.getLastActionTimestamp();
               var4 = var4 / 60 / 60;
               if (var4 >= SandboxOptions.instance.AnimalGrassRegrowTime.getValue()) {
                  IsoGridSquare var5 = IsoWorld.instance.getCell().getGridSquare(var3.x, var3.y, var3.z);
                  IsoGridSquare var6 = IsoWorld.instance.getCell().getGridSquare(var3.x + var3.getWidth(), var3.y + var3.getHeight(), var3.z);
                  if (var5 != null && var6 != null) {
                     var3.setLastActionTimestamp(0);

                     for(int var7 = var3.x; var7 < var3.x + var3.getWidth(); ++var7) {
                        for(int var8 = var3.y; var8 < var3.y + var3.getHeight(); ++var8) {
                           var5 = IsoWorld.instance.getCell().getGridSquare(var7, var8, var3.z);
                           if (var5 != null && var5.getFloor() != null && var5.getFloor().getAttachedAnimSprite() != null) {
                              for(int var9 = 0; var9 < var5.getFloor().getAttachedAnimSprite().size(); ++var9) {
                                 IsoSprite var10 = ((IsoSpriteInstance)var5.getFloor().getAttachedAnimSprite().get(var9)).parentSprite;
                                 if ("blends_natural_01_87".equals(var10.getName())) {
                                    var5.getFloor().RemoveAttachedAnim(var9);
                                    break;
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

   private void randomizeBuildingsEtc(ArrayList<IsoBuilding> var1) {
      tempRoomDefs.clear();
      IsoWorld.instance.MetaGrid.getRoomsIntersecting(this.wx * 8 - 1, this.wy * 8 - 1, 9, 9, tempRoomDefs);

      int var2;
      for(var2 = 0; var2 < tempRoomDefs.size(); ++var2) {
         IsoRoom var3 = ((RoomDef)tempRoomDefs.get(var2)).getIsoRoom();
         if (var3 != null) {
            IsoBuilding var4 = var3.getBuilding();
            if (!var1.contains(var4)) {
               var1.add(var4);
            }
         }
      }

      IsoBuilding var6;
      for(var2 = 0; var2 < var1.size(); ++var2) {
         var6 = (IsoBuilding)var1.get(var2);
         if (!GameClient.bClient && var6.def != null && var6.def.isFullyStreamedIn()) {
            StashSystem.doBuildingStash(var6.def);
            if (var6.def != null && StashSystem.isStashBuilding(var6.def)) {
               StashSystem.visitedBuilding(var6.def);
            }
         }

         RandomizedBuildingBase.ChunkLoaded(var6);
      }

      if (!GameClient.bClient && !var1.isEmpty()) {
         for(var2 = 0; var2 < var1.size(); ++var2) {
            var6 = (IsoBuilding)var1.get(var2);

            for(int var7 = 0; var7 < var6.Rooms.size(); ++var7) {
               IsoRoom var5 = (IsoRoom)var6.Rooms.get(var7);
               if (var5.def.bDoneSpawn && !this.isSpawnedRoom(var5.def.ID) && VirtualZombieManager.instance.shouldSpawnZombiesOnLevel(var5.def.level) && var5.def.intersects(this.wx * 8, this.wy * 8, 8, 8)) {
                  this.addSpawnedRoom(var5.def.ID);
                  VirtualZombieManager.instance.addIndoorZombiesToChunk(this, var5);
               }
            }
         }
      }

   }

   private void checkAdjacentChunks() {
      IsoCell var1 = IsoWorld.instance.CurrentCell;

      for(int var2 = -1; var2 <= 1; ++var2) {
         for(int var3 = -1; var3 <= 1; ++var3) {
            if (var3 != 0 || var2 != 0) {
               IsoChunk var4 = var1.getChunk(this.wx + var3, this.wy + var2);
               if (var4 != null) {
                  ++var4.m_adjacentChunkLoadedCounter;
               }
            }
         }
      }

   }

   private void AddZombieZoneStory() {
      IsoMetaChunk var1 = IsoWorld.instance.getMetaChunk(this.wx, this.wy);
      if (var1 != null) {
         for(int var2 = 0; var2 < var1.getZonesSize(); ++var2) {
            Zone var3 = var1.getZone(var2);
            RandomizedZoneStoryBase.isValidForStory(var3, false);
         }

      }
   }

   private void AddRanchAnimals() {
      IsoMetaChunk var1 = IsoWorld.instance.getMetaChunk(this.wx, this.wy);
      if (var1 != null) {
         for(int var2 = 0; var2 < var1.getZonesSize(); ++var2) {
            Zone var3 = var1.getZone(var2);
            RandomizedRanchBase.checkRanchStory(var3, false);
         }

      }
   }

   public void setCache() {
      IsoWorld.instance.CurrentCell.setCacheChunk(this);
   }

   private static ChunkLock acquireLock(int var0, int var1) {
      synchronized(Locks) {
         for(int var3 = 0; var3 < Locks.size(); ++var3) {
            if (((ChunkLock)Locks.get(var3)).wx == var0 && ((ChunkLock)Locks.get(var3)).wy == var1) {
               return ((ChunkLock)Locks.get(var3)).ref();
            }
         }

         ChunkLock var6 = FreeLocks.isEmpty() ? new ChunkLock(var0, var1) : ((ChunkLock)FreeLocks.pop()).set(var0, var1);
         Locks.add(var6);
         return var6.ref();
      }
   }

   private static void releaseLock(ChunkLock var0) {
      synchronized(Locks) {
         if (var0.deref() == 0) {
            Locks.remove(var0);
            FreeLocks.push(var0);
         }

      }
   }

   public void setCacheIncludingNull() {
   }

   public void Save(boolean var1) throws IOException {
      if (!Core.getInstance().isNoSave() && !GameClient.bClient) {
         synchronized(WriteLock) {
            sanityCheck.beginSave(this);

            try {
               File var3 = ChunkMapFilenames.instance.getDir(Core.GameSaveWorld);
               if (!var3.exists()) {
                  var3.mkdir();
               }

               SliceBuffer = this.Save(SliceBuffer, crcSave);
               if (!GameClient.bClient && !GameServer.bServer) {
                  SafeWrite(prefix, this.wx, this.wy, SliceBuffer);
               } else {
                  long var4 = ChunkChecksum.getChecksumIfExists(this.wx, this.wy);
                  crcSave.reset();
                  crcSave.update(SliceBuffer.array(), 0, SliceBuffer.position());
                  if (var4 != crcSave.getValue()) {
                     ChunkChecksum.setChecksum(this.wx, this.wy, crcSave.getValue());
                     SafeWrite(prefix, this.wx, this.wy, SliceBuffer);
                  }
               }

               if (!var1 && !GameServer.bServer) {
                  if (this.jobType != IsoChunk.JobType.Convert) {
                     WorldReuserThread.instance.addReuseChunk(this);
                  } else {
                     this.doReuseGridsquares();
                  }
               }
            } finally {
               sanityCheck.endSave(this);
            }

         }
      } else {
         if (!var1 && !GameServer.bServer && this.jobType != IsoChunk.JobType.Convert) {
            WorldReuserThread.instance.addReuseChunk(this);
         }

      }
   }

   public static void SafeWrite(String var0, int var1, int var2, ByteBuffer var3) throws IOException {
      ChunkLock var4 = acquireLock(var1, var2);
      var4.lockForWriting();

      try {
         File var5 = ChunkMapFilenames.instance.getFilename(var1, var2);
         sanityCheck.beginSaveFile(var5.getAbsolutePath());

         try {
            FileOutputStream var6 = new FileOutputStream(var5);

            try {
               var6.getChannel().truncate(0L);
               var6.write(var3.array(), 0, var3.position());
            } catch (Throwable var20) {
               try {
                  var6.close();
               } catch (Throwable var19) {
                  var20.addSuppressed(var19);
               }

               throw var20;
            }

            var6.close();
         } finally {
            sanityCheck.endSaveFile();
         }
      } finally {
         var4.unlockForWriting();
         releaseLock(var4);
      }

   }

   public static ByteBuffer SafeRead(String var0, int var1, int var2, ByteBuffer var3) throws IOException {
      ChunkLock var4 = acquireLock(var1, var2);
      var4.lockForReading();

      try {
         File var5 = ChunkMapFilenames.instance.getFilename(var1, var2);
         if (var5 == null) {
            var5 = ZomboidFileSystem.instance.getFileInCurrentSave(var0 + var1 + "_" + var2 + ".bin");
         }

         sanityCheck.beginLoadFile(var5.getAbsolutePath());

         try {
            FileInputStream var6 = new FileInputStream(var5);

            try {
               var3 = ensureCapacity(var3, (int)var5.length());
               var3.clear();
               int var7 = var6.read(var3.array());
               var3.limit(PZMath.max(var7, 0));
            } catch (Throwable var20) {
               try {
                  var6.close();
               } catch (Throwable var19) {
                  var20.addSuppressed(var19);
               }

               throw var20;
            }

            var6.close();
         } finally {
            sanityCheck.endLoadFile(var5.getAbsolutePath());
         }
      } finally {
         var4.unlockForReading();
         releaseLock(var4);
      }

      return var3;
   }

   public void SaveLoadedChunk(ClientChunkRequest.Chunk var1, CRC32 var2) throws IOException {
      var1.bb = this.Save(var1.bb, var2);
   }

   public static boolean IsDebugSave() {
      return !Core.bDebug ? false : false;
   }

   public ByteBuffer Save(ByteBuffer var1, CRC32 var2) throws IOException {
      var1.rewind();
      var1 = ensureCapacity(var1);
      var1.clear();
      var1.put((byte)(IsDebugSave() ? 1 : 0));
      var1.putInt(219);
      var1.putInt(0);
      var1.putLong(0L);
      var1.put((byte)(this.blendingDoneFull ? 1 : 0));
      this.writeFlags(var1, this.blendingModified);
      var1.put((byte)(this.blendingDonePartial ? 1 : 0));
      int var3;
      if (!Arrays.equals(this.blendingModified, comparatorBool4) && this.blendingDonePartial) {
         for(var3 = 0; var3 < 4; ++var3) {
            var1.put(this.blendingDepth[var3]);
         }
      }

      var1.put((byte)(this.attachmentsDoneFull ? 1 : 0));
      this.writeFlags(var1, this.attachmentsState);
      var3 = Math.min(1000, this.FloorBloodSplats.size());
      int var4 = this.FloorBloodSplats.size() - var3;
      int var5 = var1.position();
      var1.putInt(this.maxLevel);
      var1.putInt(this.minLevel);
      var1.putInt(var3);

      int var6;
      for(var6 = var4; var6 < this.FloorBloodSplats.size(); ++var6) {
         IsoFloorBloodSplat var7 = (IsoFloorBloodSplat)this.FloorBloodSplats.get(var6);
         var7.save(var1);
      }

      var6 = var1.position();
      long var20 = 0L;
      boolean var9 = false;
      boolean var10 = false;
      int var11 = 2147483647;
      int var12 = -2147483648;

      int var13;
      for(var13 = 0; var13 < 8; ++var13) {
         for(int var14 = 0; var14 < 8; ++var14) {
            var20 = 0L;
            int var21 = var1.position();
            var1.putLong(var20);

            for(int var15 = this.minLevel; var15 <= this.maxLevel; ++var15) {
               IsoGridSquare var16 = this.getGridSquare(var13, var14, var15);
               var1 = ensureCapacity(var1);
               if (var16 != null && var16.shouldSave()) {
                  var20 |= 1L << var15 + 32;
                  var11 = PZMath.min(var11, var15);
                  var12 = PZMath.max(var12, var15);
                  int var17 = var1.position();

                  while(true) {
                     try {
                        var16.save(var1, (ObjectOutputStream)null, IsDebugSave());
                        break;
                     } catch (BufferOverflowException var19) {
                        DebugLog.log("IsoChunk.Save: BufferOverflowException, growing ByteBuffer");
                        var1 = ensureCapacity(var1);
                        var1.position(var17);
                     }
                  }
               }
            }

            int var22 = var1.position();
            var1.position(var21);
            var1.putLong(var20);
            var1.position(var22);
         }
      }

      if (var11 <= var12) {
         var13 = var1.position();
         var1.position(var5);
         var1.putInt(var12);
         var1.putInt(var11);
         var1.position(var13);
      }

      var1 = ensureCapacity(var1);
      this.getErosionData().save(var1);
      if (this.generatorsTouchingThisChunk == null) {
         var1.putShort((short)0);
      } else {
         var1.putShort((short)this.generatorsTouchingThisChunk.size());

         for(var13 = 0; var13 < this.generatorsTouchingThisChunk.size(); ++var13) {
            IsoGameCharacter.Location var23 = (IsoGameCharacter.Location)this.generatorsTouchingThisChunk.get(var13);
            var1.putInt(var23.x);
            var1.putInt(var23.y);
            var1.put((byte)var23.z);
         }
      }

      var1.putShort((short)0);
      if ((!GameServer.bServer || GameServer.bSoftReset) && !GameClient.bClient && !GameWindow.bLoadedAsClient) {
         VehiclesDB2.instance.unloadChunk(this);
      }

      if (GameClient.bClient) {
         var13 = SandboxOptions.instance.HoursForLootRespawn.getValue();
         if (var13 > 0 && !(GameTime.getInstance().getWorldAgeHours() < (double)var13)) {
            this.lootRespawnHour = 7 + (int)(GameTime.getInstance().getWorldAgeHours() / (double)var13) * var13;
         } else {
            this.lootRespawnHour = -1;
         }
      }

      var1.putInt(this.lootRespawnHour);

      assert this.m_spawnedRooms.size() <= 32767;

      var1.putShort((short)PZMath.min(this.m_spawnedRooms.size(), 32767));

      for(var13 = 0; var13 < this.m_spawnedRooms.size(); ++var13) {
         var1.putLong(this.m_spawnedRooms.get(var13));
      }

      var13 = var1.position();
      var2.reset();
      var2.update(var1.array(), 17, var13 - 1 - 4 - 4 - 8);
      var1.position(5);
      var1.putInt(var13);
      var1.putLong(var2.getValue());
      var1.position(var13);
      return var1;
   }

   public boolean saveObjectState(ByteBuffer var1) throws IOException {
      boolean var2 = true;

      for(int var3 = 0; var3 < this.maxLevel; ++var3) {
         for(int var4 = 0; var4 < 8; ++var4) {
            for(int var5 = 0; var5 < 8; ++var5) {
               IsoGridSquare var6 = this.getGridSquare(var5, var4, var3);
               if (var6 != null) {
                  int var7 = var6.getObjects().size();
                  IsoObject[] var8 = (IsoObject[])var6.getObjects().getElements();

                  for(int var9 = 0; var9 < var7; ++var9) {
                     IsoObject var10 = var8[var9];
                     int var11 = var1.position();
                     var1.position(var11 + 2 + 2 + 4 + 2);
                     int var12 = var1.position();
                     var10.saveState(var1);
                     int var13 = var1.position();
                     if (var13 > var12) {
                        var1.position(var11);
                        var1.putShort((short)(var5 + var4 * 8 + var3 * 8 * 8));
                        var1.putShort((short)var9);
                        var1.putInt(var10.getObjectName().hashCode());
                        var1.putShort((short)(var13 - var12));
                        var1.position(var13);
                        var2 = false;
                     } else {
                        var1.position(var11);
                     }
                  }
               }
            }
         }
      }

      if (var2) {
         return false;
      } else {
         var1.putShort((short)-1);
         return true;
      }
   }

   public void loadObjectState(ByteBuffer var1) throws IOException {
      for(short var2 = var1.getShort(); var2 != -1; var2 = var1.getShort()) {
         int var3 = var2 % 8;
         int var4 = var2 / 64;
         int var5 = (var2 - var4 * 8 * 8) / 8;
         short var6 = var1.getShort();
         int var7 = var1.getInt();
         short var8 = var1.getShort();
         int var9 = var1.position();
         IsoGridSquare var10 = this.getGridSquare(var3, var5, var4);
         if (var10 != null && var6 >= 0 && var6 < var10.getObjects().size()) {
            IsoObject var11 = (IsoObject)var10.getObjects().get(var6);
            if (var7 == var11.getObjectName().hashCode()) {
               var11.loadState(var1);

               assert var1.position() == var9 + var8;
            } else {
               var1.position(var9 + var8);
            }
         } else {
            var1.position(var9 + var8);
         }
      }

   }

   public void Blam(int var1, int var2) {
      for(int var3 = 0; var3 < this.maxLevel; ++var3) {
         for(int var4 = 0; var4 < 8; ++var4) {
            for(int var5 = 0; var5 < 8; ++var5) {
               this.setSquare(var4, var5, var3, (IsoGridSquare)null);
            }
         }
      }

      this.blam = true;
   }

   private void BackupBlam(int var1, int var2, Exception var3) {
      File var4 = ZomboidFileSystem.instance.getFileInCurrentSave("blam");
      var4.mkdirs();

      File var5;
      try {
         var5 = new File("" + var4 + File.separator + "map_" + var1 + "_" + var2 + "_error.txt");
         FileOutputStream var6 = new FileOutputStream(var5);
         PrintStream var7 = new PrintStream(var6);
         var3.printStackTrace(var7);
         var7.close();
      } catch (Exception var9) {
         var9.printStackTrace();
      }

      var5 = ZomboidFileSystem.instance.getFileInCurrentSave("map", "map_" + var1 + "_" + var2 + ".bin");
      if (var5.exists()) {
         File var10 = new File(var4.getPath() + File.separator + "map_" + var1 + "_" + var2 + ".bin");

         try {
            copyFile(var5, var10);
         } catch (Exception var8) {
            var8.printStackTrace();
         }

      }
   }

   private static void copyFile(File var0, File var1) throws IOException {
      if (!var1.exists()) {
         var1.createNewFile();
      }

      FileChannel var2 = null;
      FileChannel var3 = null;

      try {
         var2 = (new FileInputStream(var0)).getChannel();
         var3 = (new FileOutputStream(var1)).getChannel();
         var3.transferFrom(var2, 0L, var2.size());
      } finally {
         if (var2 != null) {
            var2.close();
         }

         if (var3 != null) {
            var3.close();
         }

      }

   }

   public ErosionData.Chunk getErosionData() {
      if (this.erosion == null) {
         this.erosion = new ErosionData.Chunk();
      }

      return this.erosion;
   }

   private static int newtiledefinitions(int var0, int var1) {
      byte var2 = 1;
      return var2 * 100 * 1000 + 10000 + var0 * 1000 + var1;
   }

   public static int Fix2x(IsoGridSquare var0, int var1) {
      if (var0 != null && var0.chunk != null) {
         if (var0.chunk.bFixed2x) {
            return var1;
         } else {
            HashMap var2 = IsoSpriteManager.instance.NamedMap;
            if (var1 >= newtiledefinitions(140, 48) && var1 <= newtiledefinitions(140, 51)) {
               return -1;
            } else if (var1 >= newtiledefinitions(8, 14) && var1 <= newtiledefinitions(8, 71) && var1 % 8 >= 6) {
               return -1;
            } else if (var1 == newtiledefinitions(92, 2)) {
               return var1 + 20;
            } else if (var1 == newtiledefinitions(92, 20)) {
               return var1 + 1;
            } else if (var1 == newtiledefinitions(92, 21)) {
               return var1 - 1;
            } else if (var1 >= newtiledefinitions(92, 26) && var1 <= newtiledefinitions(92, 29)) {
               return var1 + 6;
            } else if (var1 == newtiledefinitions(11, 16)) {
               return newtiledefinitions(11, 45);
            } else if (var1 == newtiledefinitions(11, 17)) {
               return newtiledefinitions(11, 43);
            } else if (var1 == newtiledefinitions(11, 18)) {
               return newtiledefinitions(11, 41);
            } else if (var1 == newtiledefinitions(11, 19)) {
               return newtiledefinitions(11, 47);
            } else if (var1 == newtiledefinitions(11, 24)) {
               return newtiledefinitions(11, 26);
            } else if (var1 == newtiledefinitions(11, 25)) {
               return newtiledefinitions(11, 27);
            } else if (var1 == newtiledefinitions(27, 42)) {
               return var1 + 1;
            } else if (var1 == newtiledefinitions(27, 43)) {
               return var1 - 1;
            } else if (var1 == newtiledefinitions(27, 44)) {
               return var1 + 3;
            } else if (var1 == newtiledefinitions(27, 47)) {
               return var1 - 2;
            } else if (var1 == newtiledefinitions(27, 45)) {
               return var1 + 1;
            } else if (var1 == newtiledefinitions(27, 46)) {
               return var1 - 2;
            } else if (var1 == newtiledefinitions(34, 4)) {
               return var1 + 1;
            } else if (var1 == newtiledefinitions(34, 5)) {
               return var1 - 1;
            } else if (var1 >= newtiledefinitions(14, 0) && var1 <= newtiledefinitions(14, 7)) {
               return -1;
            } else if (var1 >= newtiledefinitions(14, 8) && var1 <= newtiledefinitions(14, 12)) {
               return var1 + 72;
            } else if (var1 == newtiledefinitions(14, 13)) {
               return var1 + 71;
            } else if (var1 >= newtiledefinitions(14, 16) && var1 <= newtiledefinitions(14, 17)) {
               return var1 + 72;
            } else if (var1 == newtiledefinitions(14, 18)) {
               return var1 + 73;
            } else if (var1 == newtiledefinitions(14, 19)) {
               return var1 + 66;
            } else if (var1 == newtiledefinitions(14, 20)) {
               return -1;
            } else if (var1 == newtiledefinitions(14, 21)) {
               return newtiledefinitions(14, 89);
            } else if (var1 == newtiledefinitions(21, 0)) {
               return newtiledefinitions(125, 16);
            } else if (var1 == newtiledefinitions(21, 1)) {
               return newtiledefinitions(125, 32);
            } else if (var1 == newtiledefinitions(21, 2)) {
               return newtiledefinitions(125, 48);
            } else if (var1 == newtiledefinitions(26, 0)) {
               return newtiledefinitions(26, 6);
            } else if (var1 == newtiledefinitions(26, 6)) {
               return newtiledefinitions(26, 0);
            } else if (var1 == newtiledefinitions(26, 1)) {
               return newtiledefinitions(26, 7);
            } else if (var1 == newtiledefinitions(26, 7)) {
               return newtiledefinitions(26, 1);
            } else if (var1 == newtiledefinitions(26, 8)) {
               return newtiledefinitions(26, 14);
            } else if (var1 == newtiledefinitions(26, 14)) {
               return newtiledefinitions(26, 8);
            } else if (var1 == newtiledefinitions(26, 9)) {
               return newtiledefinitions(26, 15);
            } else if (var1 == newtiledefinitions(26, 15)) {
               return newtiledefinitions(26, 9);
            } else if (var1 == newtiledefinitions(26, 16)) {
               return newtiledefinitions(26, 22);
            } else if (var1 == newtiledefinitions(26, 22)) {
               return newtiledefinitions(26, 16);
            } else if (var1 == newtiledefinitions(26, 17)) {
               return newtiledefinitions(26, 23);
            } else if (var1 == newtiledefinitions(26, 23)) {
               return newtiledefinitions(26, 17);
            } else {
               int var3;
               if (var1 >= newtiledefinitions(148, 0) && var1 <= newtiledefinitions(148, 16)) {
                  var3 = var1 - newtiledefinitions(148, 0);
                  return newtiledefinitions(160, var3);
               } else if (var1 >= newtiledefinitions(42, 44) && var1 <= newtiledefinitions(42, 47) || var1 >= newtiledefinitions(42, 52) && var1 <= newtiledefinitions(42, 55)) {
                  return -1;
               } else if (var1 == newtiledefinitions(43, 24)) {
                  return var1 + 4;
               } else if (var1 == newtiledefinitions(43, 26)) {
                  return var1 + 2;
               } else if (var1 == newtiledefinitions(43, 33)) {
                  return var1 - 4;
               } else if (var1 == newtiledefinitions(44, 0)) {
                  return newtiledefinitions(44, 1);
               } else if (var1 == newtiledefinitions(44, 1)) {
                  return newtiledefinitions(44, 0);
               } else if (var1 == newtiledefinitions(44, 2)) {
                  return newtiledefinitions(44, 7);
               } else if (var1 == newtiledefinitions(44, 3)) {
                  return newtiledefinitions(44, 6);
               } else if (var1 == newtiledefinitions(44, 4)) {
                  return newtiledefinitions(44, 5);
               } else if (var1 == newtiledefinitions(44, 5)) {
                  return newtiledefinitions(44, 4);
               } else if (var1 == newtiledefinitions(44, 6)) {
                  return newtiledefinitions(44, 3);
               } else if (var1 == newtiledefinitions(44, 7)) {
                  return newtiledefinitions(44, 2);
               } else if (var1 == newtiledefinitions(44, 16)) {
                  return newtiledefinitions(44, 45);
               } else if (var1 == newtiledefinitions(44, 17)) {
                  return newtiledefinitions(44, 44);
               } else if (var1 == newtiledefinitions(44, 18)) {
                  return newtiledefinitions(44, 46);
               } else if (var1 >= newtiledefinitions(44, 19) && var1 <= newtiledefinitions(44, 22)) {
                  return var1 + 33;
               } else if (var1 == newtiledefinitions(44, 23)) {
                  return newtiledefinitions(44, 47);
               } else if (var1 == newtiledefinitions(46, 8)) {
                  return newtiledefinitions(46, 5);
               } else if (var1 == newtiledefinitions(46, 14)) {
                  return newtiledefinitions(46, 10);
               } else if (var1 == newtiledefinitions(46, 15)) {
                  return newtiledefinitions(46, 11);
               } else if (var1 == newtiledefinitions(46, 22)) {
                  return newtiledefinitions(46, 14);
               } else if (var1 == newtiledefinitions(46, 23)) {
                  return newtiledefinitions(46, 15);
               } else if (var1 == newtiledefinitions(46, 54)) {
                  return newtiledefinitions(46, 55);
               } else if (var1 == newtiledefinitions(46, 55)) {
                  return newtiledefinitions(46, 54);
               } else if (var1 == newtiledefinitions(106, 32)) {
                  return newtiledefinitions(106, 34);
               } else if (var1 == newtiledefinitions(106, 34)) {
                  return newtiledefinitions(106, 32);
               } else if (var1 != newtiledefinitions(47, 0) && var1 != newtiledefinitions(47, 4)) {
                  if (var1 != newtiledefinitions(47, 1) && var1 != newtiledefinitions(47, 5)) {
                     if (var1 >= newtiledefinitions(47, 8) && var1 <= newtiledefinitions(47, 13)) {
                        return var1 + 8;
                     } else if (var1 >= newtiledefinitions(47, 22) && var1 <= newtiledefinitions(47, 23)) {
                        return var1 - 12;
                     } else if (var1 >= newtiledefinitions(47, 44) && var1 <= newtiledefinitions(47, 47)) {
                        return var1 + 4;
                     } else if (var1 >= newtiledefinitions(47, 48) && var1 <= newtiledefinitions(47, 51)) {
                        return var1 - 4;
                     } else if (var1 == newtiledefinitions(48, 56)) {
                        return newtiledefinitions(48, 58);
                     } else if (var1 == newtiledefinitions(48, 58)) {
                        return newtiledefinitions(48, 56);
                     } else if (var1 == newtiledefinitions(52, 57)) {
                        return newtiledefinitions(52, 58);
                     } else if (var1 == newtiledefinitions(52, 58)) {
                        return newtiledefinitions(52, 59);
                     } else if (var1 == newtiledefinitions(52, 45)) {
                        return newtiledefinitions(52, 44);
                     } else if (var1 == newtiledefinitions(52, 46)) {
                        return newtiledefinitions(52, 45);
                     } else if (var1 == newtiledefinitions(54, 13)) {
                        return newtiledefinitions(54, 18);
                     } else if (var1 == newtiledefinitions(54, 15)) {
                        return newtiledefinitions(54, 19);
                     } else if (var1 == newtiledefinitions(54, 21)) {
                        return newtiledefinitions(54, 16);
                     } else if (var1 == newtiledefinitions(54, 22)) {
                        return newtiledefinitions(54, 13);
                     } else if (var1 == newtiledefinitions(54, 23)) {
                        return newtiledefinitions(54, 17);
                     } else if (var1 >= newtiledefinitions(67, 0) && var1 <= newtiledefinitions(67, 16)) {
                        var3 = 64 + Rand.Next(16);
                        return ((IsoSprite)var2.get("f_bushes_1_" + var3)).ID;
                     } else if (var1 == newtiledefinitions(68, 6)) {
                        return -1;
                     } else if (var1 >= newtiledefinitions(68, 16) && var1 <= newtiledefinitions(68, 17)) {
                        return ((IsoSprite)var2.get("d_plants_1_53")).ID;
                     } else if (var1 >= newtiledefinitions(68, 18) && var1 <= newtiledefinitions(68, 23)) {
                        var3 = Rand.Next(4) * 16 + Rand.Next(8);
                        return ((IsoSprite)var2.get("d_plants_1_" + var3)).ID;
                     } else {
                        return var1 >= newtiledefinitions(79, 24) && var1 <= newtiledefinitions(79, 41) ? newtiledefinitions(81, var1 - newtiledefinitions(79, 24)) : var1;
                     }
                  } else {
                     return var1 - 1;
                  }
               } else {
                  return var1 + 1;
               }
            }
         }
      } else {
         return var1;
      }
   }

   public static String Fix2x(String var0) {
      int var2;
      if (Fix2xMap.isEmpty()) {
         HashMap var1 = Fix2xMap;

         for(var2 = 48; var2 <= 51; ++var2) {
            var1.put("blends_streetoverlays_01_" + var2, "");
         }

         var1.put("fencing_01_14", "");
         var1.put("fencing_01_15", "");
         var1.put("fencing_01_22", "");
         var1.put("fencing_01_23", "");
         var1.put("fencing_01_30", "");
         var1.put("fencing_01_31", "");
         var1.put("fencing_01_38", "");
         var1.put("fencing_01_39", "");
         var1.put("fencing_01_46", "");
         var1.put("fencing_01_47", "");
         var1.put("fencing_01_62", "");
         var1.put("fencing_01_63", "");
         var1.put("fencing_01_70", "");
         var1.put("fencing_01_71", "");
         var1.put("fixtures_bathroom_02_2", "fixtures_bathroom_02_22");
         var1.put("fixtures_bathroom_02_20", "fixtures_bathroom_02_21");
         var1.put("fixtures_bathroom_02_21", "fixtures_bathroom_02_20");

         for(var2 = 26; var2 <= 29; ++var2) {
            var1.put("fixtures_bathroom_02_" + var2, "fixtures_bathroom_02_" + (var2 + 6));
         }

         var1.put("fixtures_counters_01_16", "fixtures_counters_01_45");
         var1.put("fixtures_counters_01_17", "fixtures_counters_01_43");
         var1.put("fixtures_counters_01_18", "fixtures_counters_01_41");
         var1.put("fixtures_counters_01_19", "fixtures_counters_01_47");
         var1.put("fixtures_counters_01_24", "fixtures_counters_01_26");
         var1.put("fixtures_counters_01_25", "fixtures_counters_01_27");

         for(var2 = 0; var2 <= 7; ++var2) {
            var1.put("fixtures_railings_01_" + var2, "");
         }

         for(var2 = 8; var2 <= 12; ++var2) {
            var1.put("fixtures_railings_01_" + var2, "fixtures_railings_01_" + (var2 + 72));
         }

         var1.put("fixtures_railings_01_13", "fixtures_railings_01_84");

         for(var2 = 16; var2 <= 17; ++var2) {
            var1.put("fixtures_railings_01_" + var2, "fixtures_railings_01_" + (var2 + 72));
         }

         var1.put("fixtures_railings_01_18", "fixtures_railings_01_91");
         var1.put("fixtures_railings_01_19", "fixtures_railings_01_85");
         var1.put("fixtures_railings_01_20", "");
         var1.put("fixtures_railings_01_21", "fixtures_railings_01_89");
         var1.put("floors_exterior_natural_01_0", "blends_natural_01_16");
         var1.put("floors_exterior_natural_01_1", "blends_natural_01_32");
         var1.put("floors_exterior_natural_01_2", "blends_natural_01_48");
         var1.put("floors_rugs_01_0", "floors_rugs_01_6");
         var1.put("floors_rugs_01_6", "floors_rugs_01_0");
         var1.put("floors_rugs_01_1", "floors_rugs_01_7");
         var1.put("floors_rugs_01_7", "floors_rugs_01_1");
         var1.put("floors_rugs_01_8", "floors_rugs_01_14");
         var1.put("floors_rugs_01_14", "floors_rugs_01_8");
         var1.put("floors_rugs_01_9", "floors_rugs_01_15");
         var1.put("floors_rugs_01_15", "floors_rugs_01_9");
         var1.put("floors_rugs_01_16", "floors_rugs_01_22");
         var1.put("floors_rugs_01_22", "floors_rugs_01_16");
         var1.put("floors_rugs_01_17", "floors_rugs_01_23");
         var1.put("floors_rugs_01_23", "floors_rugs_01_17");
         var1.put("furniture_bedding_01_42", "furniture_bedding_01_43");
         var1.put("furniture_bedding_01_43", "furniture_bedding_01_42");
         var1.put("furniture_bedding_01_44", "furniture_bedding_01_47");
         var1.put("furniture_bedding_01_47", "furniture_bedding_01_45");
         var1.put("furniture_bedding_01_45", "furniture_bedding_01_46");
         var1.put("furniture_bedding_01_46", "furniture_bedding_01_44");
         var1.put("furniture_tables_low_01_4", "furniture_tables_low_01_5");
         var1.put("furniture_tables_low_01_5", "furniture_tables_low_01_4");

         for(var2 = 0; var2 <= 5; ++var2) {
            var1.put("location_business_machinery_" + var2, "location_business_machinery_01_" + var2);
            var1.put("location_business_machinery_" + (var2 + 8), "location_business_machinery_01_" + (var2 + 8));
            var1.put("location_ business_machinery_" + var2, "location_business_machinery_01_" + var2);
            var1.put("location_ business_machinery_" + (var2 + 8), "location_business_machinery_01_" + (var2 + 8));
         }

         for(var2 = 44; var2 <= 47; ++var2) {
            var1.put("location_hospitality_sunstarmotel_01_" + var2, "");
         }

         for(var2 = 52; var2 <= 55; ++var2) {
            var1.put("location_hospitality_sunstarmotel_01_" + var2, "");
         }

         var1.put("location_hospitality_sunstarmotel_02_24", "location_hospitality_sunstarmotel_02_28");
         var1.put("location_hospitality_sunstarmotel_02_26", "location_hospitality_sunstarmotel_02_28");
         var1.put("location_hospitality_sunstarmotel_02_33", "location_hospitality_sunstarmotel_02_29");
         var1.put("location_restaurant_bar_01_0", "location_restaurant_bar_01_1");
         var1.put("location_restaurant_bar_01_1", "location_restaurant_bar_01_0");
         var1.put("location_restaurant_bar_01_2", "location_restaurant_bar_01_7");
         var1.put("location_restaurant_bar_01_3", "location_restaurant_bar_01_6");
         var1.put("location_restaurant_bar_01_4", "location_restaurant_bar_01_5");
         var1.put("location_restaurant_bar_01_5", "location_restaurant_bar_01_4");
         var1.put("location_restaurant_bar_01_6", "location_restaurant_bar_01_3");
         var1.put("location_restaurant_bar_01_7", "location_restaurant_bar_01_2");
         var1.put("location_restaurant_bar_01_16", "location_restaurant_bar_01_45");
         var1.put("location_restaurant_bar_01_17", "location_restaurant_bar_01_44");
         var1.put("location_restaurant_bar_01_18", "location_restaurant_bar_01_46");

         for(var2 = 19; var2 <= 22; ++var2) {
            var1.put("location_restaurant_bar_01_" + var2, "location_restaurant_bar_01_" + (var2 + 33));
         }

         var1.put("location_restaurant_bar_01_23", "location_restaurant_bar_01_47");
         var1.put("location_restaurant_pie_01_8", "location_restaurant_pie_01_5");
         var1.put("location_restaurant_pie_01_14", "location_restaurant_pie_01_10");
         var1.put("location_restaurant_pie_01_15", "location_restaurant_pie_01_11");
         var1.put("location_restaurant_pie_01_22", "location_restaurant_pie_01_14");
         var1.put("location_restaurant_pie_01_23", "location_restaurant_pie_01_15");
         var1.put("location_restaurant_pie_01_54", "location_restaurant_pie_01_55");
         var1.put("location_restaurant_pie_01_55", "location_restaurant_pie_01_54");
         var1.put("location_pizzawhirled_01_32", "location_pizzawhirled_01_34");
         var1.put("location_pizzawhirled_01_34", "location_pizzawhirled_01_32");
         var1.put("location_restaurant_seahorse_01_0", "location_restaurant_seahorse_01_1");
         var1.put("location_restaurant_seahorse_01_1", "location_restaurant_seahorse_01_0");
         var1.put("location_restaurant_seahorse_01_4", "location_restaurant_seahorse_01_5");
         var1.put("location_restaurant_seahorse_01_5", "location_restaurant_seahorse_01_4");

         for(var2 = 8; var2 <= 13; ++var2) {
            var1.put("location_restaurant_seahorse_01_" + var2, "location_restaurant_seahorse_01_" + (var2 + 8));
         }

         for(var2 = 22; var2 <= 23; ++var2) {
            var1.put("location_restaurant_seahorse_01_" + var2, "location_restaurant_seahorse_01_" + (var2 - 12));
         }

         for(var2 = 44; var2 <= 47; ++var2) {
            var1.put("location_restaurant_seahorse_01_" + var2, "location_restaurant_seahorse_01_" + (var2 + 4));
         }

         for(var2 = 48; var2 <= 51; ++var2) {
            var1.put("location_restaurant_seahorse_01_" + var2, "location_restaurant_seahorse_01_" + (var2 - 4));
         }

         var1.put("location_restaurant_spiffos_01_56", "location_restaurant_spiffos_01_58");
         var1.put("location_restaurant_spiffos_01_58", "location_restaurant_spiffos_01_56");
         var1.put("location_shop_fossoil_01_45", "location_shop_fossoil_01_44");
         var1.put("location_shop_fossoil_01_46", "location_shop_fossoil_01_45");
         var1.put("location_shop_fossoil_01_57", "location_shop_fossoil_01_58");
         var1.put("location_shop_fossoil_01_58", "location_shop_fossoil_01_59");
         var1.put("location_shop_greenes_01_13", "location_shop_greenes_01_18");
         var1.put("location_shop_greenes_01_15", "location_shop_greenes_01_19");
         var1.put("location_shop_greenes_01_21", "location_shop_greenes_01_16");
         var1.put("location_shop_greenes_01_22", "location_shop_greenes_01_13");
         var1.put("location_shop_greenes_01_23", "location_shop_greenes_01_17");
         var1.put("location_shop_greenes_01_67", "location_shop_greenes_01_70");
         var1.put("location_shop_greenes_01_68", "location_shop_greenes_01_67");
         var1.put("location_shop_greenes_01_70", "location_shop_greenes_01_71");
         var1.put("location_shop_greenes_01_75", "location_shop_greenes_01_78");
         var1.put("location_shop_greenes_01_76", "location_shop_greenes_01_75");
         var1.put("location_shop_greenes_01_78", "location_shop_greenes_01_79");

         for(var2 = 0; var2 <= 16; ++var2) {
            var1.put("vegetation_foliage_01_" + var2, "randBush");
         }

         var1.put("vegetation_groundcover_01_0", "blends_grassoverlays_01_16");
         var1.put("vegetation_groundcover_01_1", "blends_grassoverlays_01_8");
         var1.put("vegetation_groundcover_01_2", "blends_grassoverlays_01_0");
         var1.put("vegetation_groundcover_01_3", "blends_grassoverlays_01_64");
         var1.put("vegetation_groundcover_01_4", "blends_grassoverlays_01_56");
         var1.put("vegetation_groundcover_01_5", "blends_grassoverlays_01_48");
         var1.put("vegetation_groundcover_01_6", "");
         var1.put("vegetation_groundcover_01_44", "blends_grassoverlays_01_40");
         var1.put("vegetation_groundcover_01_45", "blends_grassoverlays_01_32");
         var1.put("vegetation_groundcover_01_46", "blends_grassoverlays_01_24");
         var1.put("vegetation_groundcover_01_16", "d_plants_1_53");
         var1.put("vegetation_groundcover_01_17", "d_plants_1_53");

         for(var2 = 18; var2 <= 23; ++var2) {
            var1.put("vegetation_groundcover_01_" + var2, "randPlant");
         }

         for(var2 = 20; var2 <= 23; ++var2) {
            var1.put("walls_exterior_house_01_" + var2, "walls_exterior_house_01_" + (var2 + 12));
            var1.put("walls_exterior_house_01_" + (var2 + 8), "walls_exterior_house_01_" + (var2 + 8 + 12));
         }

         for(var2 = 24; var2 <= 41; ++var2) {
            var1.put("walls_exterior_roofs_01_" + var2, "walls_exterior_roofs_03_" + var2);
         }
      }

      String var3 = (String)Fix2xMap.get(var0);
      if (var3 == null) {
         return var0;
      } else if ("randBush".equals(var3)) {
         var2 = 64 + Rand.Next(16);
         return "f_bushes_1_" + var2;
      } else if ("randPlant".equals(var3)) {
         var2 = Rand.Next(4) * 16 + Rand.Next(8);
         return "d_plants_1_" + var2;
      } else {
         return var3;
      }
   }

   public void addGeneratorPos(int var1, int var2, int var3) {
      if (this.generatorsTouchingThisChunk == null) {
         this.generatorsTouchingThisChunk = new ArrayList();
      }

      for(int var4 = 0; var4 < this.generatorsTouchingThisChunk.size(); ++var4) {
         IsoGameCharacter.Location var5 = (IsoGameCharacter.Location)this.generatorsTouchingThisChunk.get(var4);
         if (var5.x == var1 && var5.y == var2 && var5.z == var3) {
            return;
         }
      }

      IsoGameCharacter.Location var6 = new IsoGameCharacter.Location(var1, var2, var3);
      this.generatorsTouchingThisChunk.add(var6);
   }

   public void removeGeneratorPos(int var1, int var2, int var3) {
      if (this.generatorsTouchingThisChunk != null) {
         for(int var4 = 0; var4 < this.generatorsTouchingThisChunk.size(); ++var4) {
            IsoGameCharacter.Location var5 = (IsoGameCharacter.Location)this.generatorsTouchingThisChunk.get(var4);
            if (var5.x == var1 && var5.y == var2 && var5.z == var3) {
               this.generatorsTouchingThisChunk.remove(var4);
               --var4;
            }
         }

      }
   }

   public boolean isGeneratorPoweringSquare(int var1, int var2, int var3) {
      if (this.generatorsTouchingThisChunk == null) {
         return false;
      } else {
         for(int var4 = 0; var4 < this.generatorsTouchingThisChunk.size(); ++var4) {
            IsoGameCharacter.Location var5 = (IsoGameCharacter.Location)this.generatorsTouchingThisChunk.get(var4);
            if (IsoGenerator.isPoweringSquare(var5.x, var5.y, var5.z, var1, var2, var3)) {
               return true;
            }
         }

         return false;
      }
   }

   public void checkForMissingGenerators() {
      if (this.generatorsTouchingThisChunk != null) {
         for(int var1 = 0; var1 < this.generatorsTouchingThisChunk.size(); ++var1) {
            IsoGameCharacter.Location var2 = (IsoGameCharacter.Location)this.generatorsTouchingThisChunk.get(var1);
            IsoGridSquare var3 = IsoWorld.instance.CurrentCell.getGridSquare(var2.x, var2.y, var2.z);
            if (var3 != null) {
               IsoGenerator var4 = var3.getGenerator();
               if (var4 == null || !var4.isActivated()) {
                  this.generatorsTouchingThisChunk.remove(var1);
                  --var1;
               }
            }
         }

      }
   }

   public boolean isNewChunk() {
      return this.addZombies;
   }

   public void addSpawnedRoom(long var1) {
      if (!this.m_spawnedRooms.contains(var1)) {
         this.m_spawnedRooms.add(var1);
      }

   }

   public boolean isSpawnedRoom(long var1) {
      return this.m_spawnedRooms.contains(var1);
   }

   public Zone getScavengeZone() {
      if (this.m_scavengeZone != null) {
         return this.m_scavengeZone;
      } else {
         IsoMetaChunk var1 = IsoWorld.instance.getMetaGrid().getChunkData(this.wx, this.wy);
         if (var1 != null && var1.getZonesSize() > 0) {
            for(int var2 = 0; var2 < var1.getZonesSize(); ++var2) {
               Zone var3 = var1.getZone(var2);
               if ("DeepForest".equals(var3.type) || "Forest".equals(var3.type)) {
                  this.m_scavengeZone = var3;
                  return var3;
               }

               if ("Nav".equals(var3.type) || "Town".equals(var3.type)) {
                  return null;
               }
            }
         }

         byte var8 = 5;
         if (this.m_treeCount < var8) {
            return null;
         } else {
            int var9 = 0;

            for(int var4 = -1; var4 <= 1; ++var4) {
               for(int var5 = -1; var5 <= 1; ++var5) {
                  if (var5 != 0 || var4 != 0) {
                     IsoChunk var6 = GameServer.bServer ? ServerMap.instance.getChunk(this.wx + var5, this.wy + var4) : IsoWorld.instance.CurrentCell.getChunk(this.wx + var5, this.wy + var4);
                     if (var6 != null && var6.m_treeCount >= var8) {
                        ++var9;
                        if (var9 == 8) {
                           byte var7 = 8;
                           this.m_scavengeZone = new Zone("", "Forest", this.wx * var7, this.wy * var7, 0, var7, var7);
                           return this.m_scavengeZone;
                        }
                     }
                  }
               }
            }

            return null;
         }
      }
   }

   public void resetForStore() {
      this.randomID = 0;
      this.revision = 0L;
      this.nextSplatIndex = 0;
      this.FloorBloodSplats.clear();
      this.FloorBloodSplatsFade.clear();
      this.jobType = IsoChunk.JobType.None;

      int var1;
      for(var1 = this.minLevel; var1 <= this.maxLevel; ++var1) {
         this.levels[var1 - this.minLevel].clear();
         this.levels[var1 - this.minLevel].release();
         this.levels[var1 - this.minLevel] = null;
      }

      this.maxLevel = 0;
      this.minLevel = 0;
      this.minLevelPhysics = this.maxLevelPhysics = 1000;
      this.levels[0] = IsoChunkLevel.alloc().init(this, this.minLevel);
      this.bFixed2x = false;
      this.vehicles.clear();
      this.roomLights.clear();
      this.blam = false;
      this.lotheader = null;
      this.bLoaded = false;
      this.addZombies = false;
      this.proceduralZombieSquares.clear();
      this.loadedPhysics = false;
      this.wx = 0;
      this.wy = 0;
      this.erosion = null;
      this.lootRespawnHour = -1;
      if (this.generatorsTouchingThisChunk != null) {
         this.generatorsTouchingThisChunk.clear();
      }

      this.m_treeCount = 0;
      this.m_scavengeZone = null;
      this.m_numberOfWaterTiles = 0;
      this.m_spawnedRooms.resetQuick();
      this.m_adjacentChunkLoadedCounter = 0;
      this.loadedBits = 0;
      this.loadID = -1;
      this.squares = new IsoGridSquare[1][];
      this.squares[0] = this.levels[0].squares;

      for(var1 = 0; var1 < 4; ++var1) {
         this.lightCheck[var1] = true;
         this.bLightingNeverDone[var1] = true;
      }

      this.refs.clear();
      this.m_vehicleStorySpawnData = null;
      this.m_loadVehiclesObject = null;
      this.m_objectEmitterData.reset();
      MPStatistics.increaseStoredChunk();
      this.blendingDoneFull = false;
      this.blendingDonePartial = false;
      Arrays.fill(this.blendingModified, false);
      this.blendingDepth = new byte[]{BlendDirection.NORTH.defaultDepth, BlendDirection.SOUTH.defaultDepth, BlendDirection.WEST.defaultDepth, BlendDirection.EAST.defaultDepth};
      this.attachmentsDoneFull = true;
      Arrays.fill(this.attachmentsState, true);
      this.chunkGenerationStatus = EnumSet.noneOf(ChunkGenerationStatus.class);
   }

   public int getNumberOfWaterTiles() {
      return this.m_numberOfWaterTiles;
   }

   public void setRandomVehicleStoryToSpawnLater(VehicleStorySpawnData var1) {
      this.m_vehicleStorySpawnData = var1;
   }

   public boolean hasObjectAmbientEmitter(IsoObject var1) {
      return this.m_objectEmitterData.hasObject(var1);
   }

   public void addObjectAmbientEmitter(IsoObject var1, ObjectAmbientEmitters.PerObjectLogic var2) {
      this.m_objectEmitterData.addObject(var1, var2);
   }

   public void removeObjectAmbientEmitter(IsoObject var1) {
      this.m_objectEmitterData.removeObject(var1);
   }

   private void addItemOnGround(IsoGridSquare var1, String var2) {
      if (!SandboxOptions.instance.RemoveStoryLoot.getValue() || ItemPickerJava.getLootModifier(var2) != 0.0F) {
         if (var1 != null && !StringUtils.isNullOrWhitespace(var2)) {
            InventoryItem var3 = ItemSpawner.spawnItem(var2, var1, Rand.Next(0.2F, 0.8F), Rand.Next(0.2F, 0.8F), 0.0F);
            if (var3 instanceof InventoryContainer && ItemPickerJava.containers.containsKey(var3.getType())) {
               ItemPickerJava.rollContainerItem((InventoryContainer)var3, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var3.getType()));
               LuaEventManager.triggerEvent("OnFillContainer", "Container", var3.getType(), ((InventoryContainer)var3).getItemContainer());
            }

         }
      }
   }

   public void assignLoadID() {
      if (this.loadID != -1) {
         throw new IllegalStateException("IsoChunk was already assigned a valid loadID");
      } else {
         this.loadID = nextLoadID++;
         if (nextLoadID == 32767) {
            nextLoadID = 0;
         }

      }
   }

   public short getLoadID() {
      if (this.loadID == -1) {
         throw new IllegalStateException("IsoChunk.loadID is invalid");
      } else {
         return this.loadID;
      }
   }

   public boolean containsPoint(float var1, float var2) {
      byte var3 = 8;
      return Float.compare(var1, (float)(this.wx * var3)) >= 0 && Float.compare(var1, (float)((this.wx + 1) * var3)) < 0 && Float.compare(var2, (float)(this.wy * var3)) >= 0 && Float.compare(var2, (float)((this.wy + 1) * var3)) < 0;
   }

   public FBORenderLevels getRenderLevels(int var1) {
      if (this.m_renderLevels[var1] == null) {
         this.m_renderLevels[var1] = new FBORenderLevels(var1, this);
      }

      return this.m_renderLevels[var1];
   }

   public void invalidateRenderChunks(long var1) {
      if (!GameServer.bServer) {
         for(int var3 = 0; var3 < 4; ++var3) {
            this.getRenderLevels(var3).invalidateAll(var1);
         }

      }
   }

   public void invalidateRenderChunkLevel(int var1, long var2) {
      if (PerformanceSettings.FBORenderChunk) {
         if (!GameServer.bServer) {
            for(int var4 = 0; var4 < 4; ++var4) {
               this.getRenderLevels(var4).invalidateLevel(var1, var2);
            }

         }
      }
   }

   public FBORenderCutaways.ChunkLevelsData getCutawayData() {
      return this.m_cutawayData;
   }

   public FBORenderCutaways.ChunkLevelData getCutawayDataForLevel(int var1) {
      return this.getCutawayData().getDataForLevel(var1);
   }

   public void invalidateVispolyChunkLevel(int var1) {
      if (!GameServer.bServer) {
         this.getVispolyDataForLevel(var1).invalidate();
      }
   }

   public VisibilityPolygon2.ChunkData getVispolyData() {
      return this.m_vispolyData;
   }

   public VisibilityPolygon2.ChunkLevelData getVispolyDataForLevel(int var1) {
      return this.getVispolyData().getDataForLevel(var1);
   }

   public boolean hasWaterSquare() {
      for(int var1 = 0; var1 < 8; ++var1) {
         for(int var2 = 0; var2 < 8; ++var2) {
            IsoGridSquare var3 = this.getGridSquare(var2, var1, 0);
            if (var3 == null || var3.isWaterSquare()) {
               return true;
            }
         }
      }

      return false;
   }

   private void addRagdollControllers() {
      if (!RagdollBuilder.instance.isInitialized()) {
         RagdollBuilder.instance.Initialize();
      }

   }

   public static enum JobType {
      None,
      Convert,
      SoftReset;

      private JobType() {
      }
   }

   private static enum PhysicsShapes {
      Solid,
      WallN,
      WallW,
      WallS,
      WallE,
      Tree,
      Floor,
      StairsMiddleNorth,
      StairsMiddleWest,
      SolidStairs,
      FIRST_MESH;

      private PhysicsShapes() {
      }
   }

   private static class ChunkGetter implements IsoGridSquare.GetSquare {
      IsoChunk chunk;

      private ChunkGetter() {
      }

      public IsoGridSquare getGridSquare(int var1, int var2, int var3) {
         var1 -= this.chunk.wx * 8;
         var2 -= this.chunk.wy * 8;
         return var1 >= 0 && var1 < 8 && var2 >= 0 && var2 < 8 && var3 >= -32 && var3 <= 31 ? this.chunk.getGridSquare(var1, var2, var3) : null;
      }
   }

   private static class SanityCheck {
      public IsoChunk saveChunk;
      public String saveThread;
      public IsoChunk loadChunk;
      public String loadThread;
      public final ArrayList<String> loadFile = new ArrayList();
      public String saveFile;

      private SanityCheck() {
      }

      public synchronized void beginSave(IsoChunk var1) {
         if (this.saveChunk != null) {
            this.log("trying to save while already saving, wx,wy=" + var1.wx + "," + var1.wy);
         }

         if (this.loadChunk == var1) {
            this.log("trying to save the same IsoChunk being loaded");
         }

         this.saveChunk = var1;
         this.saveThread = Thread.currentThread().getName();
      }

      public synchronized void endSave(IsoChunk var1) {
         this.saveChunk = null;
         this.saveThread = null;
      }

      public synchronized void beginLoad(IsoChunk var1) {
         if (this.loadChunk != null) {
            this.log("trying to load while already loading, wx,wy=" + var1.wx + "," + var1.wy);
         }

         if (this.saveChunk == var1) {
            this.log("trying to load the same IsoChunk being saved");
         }

         this.loadChunk = var1;
         this.loadThread = Thread.currentThread().getName();
      }

      public synchronized void endLoad(IsoChunk var1) {
         this.loadChunk = null;
         this.loadThread = null;
      }

      public synchronized void checkCRC(long var1, long var3) {
         if (var1 != var3) {
            this.log("CRC mismatch save=" + var1 + " load=" + var3);
         }

      }

      public synchronized void checkLength(long var1, long var3) {
         if (var1 != var3) {
            this.log("LENGTH mismatch save=" + var1 + " load=" + var3);
         }

      }

      public synchronized void beginLoadFile(String var1) {
         if (var1.equals(this.saveFile)) {
            this.log("attempted to load file being saved " + var1);
         }

         this.loadFile.add(var1);
      }

      public synchronized void endLoadFile(String var1) {
         this.loadFile.remove(var1);
      }

      public synchronized void beginSaveFile(String var1) {
         if (this.loadFile.contains(var1)) {
            this.log("attempted to save file being loaded " + var1);
         }

         this.saveFile = var1;
      }

      public synchronized void endSaveFile() {
         this.saveFile = null;
      }

      public synchronized void log(String var1) {
         StringBuilder var2 = new StringBuilder();
         var2.append("SANITY CHECK FAIL! thread=\"" + Thread.currentThread().getName() + "\"\n");
         if (var1 != null) {
            var2.append(var1 + "\n");
         }

         if (this.saveChunk != null && this.saveChunk == this.loadChunk) {
            var2.append("exact same IsoChunk being saved + loaded\n");
         }

         if (this.saveChunk != null) {
            var2.append("save wx,wy=" + this.saveChunk.wx + "," + this.saveChunk.wy + " thread=\"" + this.saveThread + "\"\n");
         } else {
            var2.append("save chunk=null\n");
         }

         if (this.loadChunk != null) {
            var2.append("load wx,wy=" + this.loadChunk.wx + "," + this.loadChunk.wy + " thread=\"" + this.loadThread + "\"\n");
         } else {
            var2.append("load chunk=null\n");
         }

         String var3 = var2.toString();
         throw new RuntimeException(var3);
      }
   }

   private static class ChunkLock {
      public int wx;
      public int wy;
      public int count;
      public ReentrantReadWriteLock rw = new ReentrantReadWriteLock(true);

      public ChunkLock(int var1, int var2) {
         this.wx = var1;
         this.wy = var2;
      }

      public ChunkLock set(int var1, int var2) {
         assert this.count == 0;

         this.wx = var1;
         this.wy = var2;
         return this;
      }

      public ChunkLock ref() {
         ++this.count;
         return this;
      }

      public int deref() {
         assert this.count > 0;

         return --this.count;
      }

      public void lockForReading() {
         this.rw.readLock().lock();
      }

      public void unlockForReading() {
         this.rw.readLock().unlock();
      }

      public void lockForWriting() {
         this.rw.writeLock().lock();
      }

      public void unlockForWriting() {
         this.rw.writeLock().unlock();
      }
   }
}
