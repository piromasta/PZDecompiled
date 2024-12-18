package zombie.iso;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.opengl.GL20;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.MapCollisionData;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.ZomboidBitFlag;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.Lua.MapObjects;
import zombie.ai.states.ZombieIdleState;
import zombie.audio.BaseSoundEmitter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.Moodles.MoodleType;
import zombie.characters.animals.AnimalSoundState;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.RenderSettings;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.properties.PropertyContainer;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.entity.GameEntityFactory;
import zombie.erosion.ErosionData;
import zombie.erosion.categories.ErosionCategory;
import zombie.globalObjects.GlobalObject;
import zombie.globalObjects.SGlobalObjectSystem;
import zombie.globalObjects.SGlobalObjects;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.DesignationZone;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.regions.IWorldRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.ObjectRenderLayer;
import zombie.iso.objects.IsoAnimalTrack;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoBrokenGlass;
import zombie.iso.objects.IsoCarBatteryCharger;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoFire;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoHutch;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoRainSplash;
import zombie.iso.objects.IsoRaindrop;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTrap;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWaveSignal;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.RainManager;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.sprite.shapers.FloorShaper;
import zombie.iso.sprite.shapers.FloorShaperAttachedSprites;
import zombie.iso.sprite.shapers.FloorShaperDeDiamond;
import zombie.iso.sprite.shapers.FloorShaperDiamond;
import zombie.iso.sprite.shapers.WallShaper;
import zombie.iso.sprite.shapers.WallShaperN;
import zombie.iso.sprite.shapers.WallShaperW;
import zombie.iso.sprite.shapers.WallShaperWhole;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.iso.worldgen.biomes.IBiome;
import zombie.iso.worldgen.utils.SquareCoord;
import zombie.iso.zones.Zone;
import zombie.meta.Meta;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerLOS;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.network.packets.AddExplosiveTrapPacket;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.RemoveItemFromSquarePacket;
import zombie.network.packets.actions.AddCorpseToMapPacket;
import zombie.network.packets.character.AnimalCommandPacket;
import zombie.network.packets.service.ReceiveModDataPacket;
import zombie.pathfind.PolygonalMap2;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.objects.Item;
import zombie.tileDepth.CutawayAttachedModifier;
import zombie.tileDepth.TileDepthMapManager;
import zombie.tileDepth.TileSeamModifier;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;

public final class IsoGridSquare {
   public BuildingDef associatedBuilding;
   private boolean hasTree;
   private ArrayList<Float> LightInfluenceB;
   private ArrayList<Float> LightInfluenceG;
   private ArrayList<Float> LightInfluenceR;
   public final IsoGridSquare[] nav = new IsoGridSquare[8];
   public int lightLevel = 0;
   public int collideMatrix = -1;
   public int pathMatrix = -1;
   public int visionMatrix = -1;
   public IsoRoom room = null;
   public IsoGridSquare w;
   public IsoGridSquare nw;
   public IsoGridSquare sw;
   public IsoGridSquare s;
   public IsoGridSquare n;
   public IsoGridSquare ne;
   public IsoGridSquare se;
   public IsoGridSquare e;
   public IsoGridSquare u;
   public IsoGridSquare d;
   public boolean haveSheetRope = false;
   private IWorldRegion isoWorldRegion;
   private boolean hasSetIsoWorldRegion = false;
   public int ObjectsSyncCount = 0;
   public IsoBuilding roofHideBuilding;
   public boolean bFlattenGrassEtc;
   private static final long VisiFlagTimerPeriod_ms = 750L;
   public static final byte PCF_NONE = 0;
   public static final byte PCF_NORTH = 1;
   public static final byte PCF_WEST = 2;
   private final byte[] playerCutawayFlags = new byte[4];
   private final long[] playerCutawayFlagLockUntilTimes = new long[4];
   private final byte[] targetPlayerCutawayFlags = new byte[4];
   private final boolean[] playerIsDissolvedFlags = new boolean[4];
   private final long[] playerIsDissolvedFlagLockUntilTimes = new long[4];
   private final boolean[] targetPlayerIsDissolvedFlags = new boolean[4];
   private IsoWaterGeometry water = null;
   private IsoPuddlesGeometry puddles = null;
   private float puddlesCacheSize = -1.0F;
   private float puddlesCacheLevel = -1.0F;
   public final ILighting[] lighting = new ILighting[4];
   public int x;
   public int y;
   public int z;
   private int CachedScreenValue = -1;
   public float CachedScreenX;
   public float CachedScreenY;
   private static long torchTimer = 0L;
   public boolean SolidFloorCached = false;
   public boolean SolidFloor = false;
   private boolean CacheIsFree = false;
   private boolean CachedIsFree = false;
   public IsoChunk chunk;
   public long roomID = -1L;
   public Integer ID = -999;
   public Zone zone;
   private final ArrayList<IsoGameCharacter> DeferedCharacters = new ArrayList();
   private int DeferredCharacterTick = -1;
   private final ArrayList<IsoMovingObject> StaticMovingObjects = new ArrayList(0);
   private final ArrayList<IsoMovingObject> MovingObjects = new ArrayList(0);
   protected final PZArrayList<IsoObject> Objects = new PZArrayList(IsoObject.class, 2);
   protected final PZArrayList<IsoObject> localTemporaryObjects = new PZArrayList(IsoObject.class, 2);
   private final ArrayList<IsoWorldInventoryObject> WorldObjects = new ArrayList();
   final ZomboidBitFlag hasTypes;
   private final PropertyContainer Properties;
   private final ArrayList<IsoObject> SpecialObjects;
   public boolean haveRoof;
   private boolean burntOut;
   private boolean bHasFlies;
   private IBiome biome;
   private IsoGridOcclusionData OcclusionDataCache;
   private static final PZArrayList<IsoWorldInventoryObject> tempWorldInventoryObjects = new PZArrayList(IsoWorldInventoryObject.class, 16);
   public static final ConcurrentLinkedQueue<IsoGridSquare> isoGridSquareCache = new ConcurrentLinkedQueue();
   public static ArrayDeque<IsoGridSquare> loadGridSquareCache;
   private boolean overlayDone;
   private KahluaTable table;
   private int trapPositionX;
   private int trapPositionY;
   private int trapPositionZ;
   public static final ArrayList<String> ignoreBlockingSprites = new ArrayList();
   public static int gridSquareCacheEmptyTimer = 0;
   private static float darkStep = 0.06F;
   public static float RecalcLightTime = 0.0F;
   private static int lightcache = 0;
   public static final ArrayList<IsoGridSquare> choices = new ArrayList();
   public static boolean USE_WALL_SHADER = true;
   private static final int cutawayY = 0;
   private static final int cutawayNWWidth = 66;
   private static final int cutawayNWHeight = 226;
   private static final int cutawaySEXCut = 1084;
   private static final int cutawaySEXUncut = 1212;
   private static final int cutawaySEWidth = 6;
   private static final int cutawaySEHeight = 196;
   private static final int cutawayNXFullyCut = 700;
   private static final int cutawayNXCutW = 444;
   private static final int cutawayNXUncut = 828;
   private static final int cutawayNXCutE = 956;
   private static final int cutawayWXFullyCut = 512;
   private static final int cutawayWXCutS = 768;
   private static final int cutawayWXUncut = 896;
   private static final int cutawayWXCutN = 256;
   private static final int cutawayFenceXOffset = 1;
   private static final int cutawayLogWallXOffset = 1;
   private static final int cutawayMedicalCurtainWXOffset = -3;
   private static final int cutawayTentWallXOffset = -3;
   private static final int cutawaySpiffoWindowXOffset = -24;
   private static final int cutawayRoof4XOffset = -60;
   private static final int cutawayRoof17XOffset = -46;
   private static final int cutawayRoof28XOffset = -60;
   private static final int cutawayRoof41XOffset = -46;
   private static final ColorInfo lightInfoTemp = new ColorInfo();
   private static final float doorWindowCutawayLightMin = 0.3F;
   private static boolean bWallCutawayW;
   private static boolean bWallCutawayN;
   public boolean isSolidFloorCache;
   public boolean isExteriorCache;
   public boolean isVegitationCache;
   public int hourLastSeen;
   static IsoGridSquare lastLoaded = null;
   public static int IDMax = -1;
   static int col = -1;
   static int path = -1;
   static int pathdoor = -1;
   static int vision = -1;
   public long hashCodeObjects;
   static final Color tr = new Color(1, 1, 1, 1);
   static final Color tl = new Color(1, 1, 1, 1);
   static final Color br = new Color(1, 1, 1, 1);
   static final Color bl = new Color(1, 1, 1, 1);
   static final Color interp1 = new Color(1, 1, 1, 1);
   static final Color interp2 = new Color(1, 1, 1, 1);
   static final Color finalCol = new Color(1, 1, 1, 1);
   public static final CellGetSquare cellGetSquare = new CellGetSquare();
   public boolean propertiesDirty;
   public static boolean UseSlowCollision = false;
   private static boolean bDoSlowPathfinding = false;
   private static final Comparator<IsoMovingObject> comp = (var0, var1) -> {
      return var0.compareToY(var1);
   };
   public static boolean isOnScreenLast = false;
   private float splashX;
   private float splashY;
   private float splashFrame;
   private int splashFrameNum;
   private static Texture[] waterSplashCache = new Texture[80];
   private static boolean isWaterSplashCacheInitialised = false;
   WaterSplashData waterSplashData;
   private final ColorInfo[] lightInfo;
   static String[] rainsplashCache = new String[50];
   private static final ColorInfo defColorInfo = new ColorInfo();
   private static final ColorInfo blackColorInfo = new ColorInfo();
   static int colu = 0;
   static int coll = 0;
   static int colr = 0;
   static int colu2 = 0;
   static int coll2 = 0;
   static int colr2 = 0;
   public static boolean CircleStencil = false;
   public static float rmod = 0.0F;
   public static float gmod = 0.0F;
   public static float bmod = 0.0F;
   static final Vector2 tempo = new Vector2();
   static final Vector2 tempo2 = new Vector2();
   private IsoRaindrop RainDrop;
   private IsoRainSplash RainSplash;
   private ErosionData.Square erosion;
   private static final int[] SURFACE_OFFSETS = new int[8];
   public static final int WALL_TYPE_N = 1;
   public static final int WALL_TYPE_S = 2;
   public static final int WALL_TYPE_W = 4;
   public static final int WALL_TYPE_E = 8;

   public static boolean getMatrixBit(int var0, int var1, int var2, int var3) {
      return getMatrixBit(var0, (byte)var1, (byte)var2, (byte)var3);
   }

   public static boolean getMatrixBit(int var0, byte var1, byte var2, byte var3) {
      return (var0 >> var1 + var2 * 3 + var3 * 9 & 1) != 0;
   }

   public static int setMatrixBit(int var0, int var1, int var2, int var3, boolean var4) {
      return setMatrixBit(var0, (byte)var1, (byte)var2, (byte)var3, var4);
   }

   public static int setMatrixBit(int var0, byte var1, byte var2, byte var3, boolean var4) {
      return var4 ? var0 | 1 << var1 + var2 * 3 + var3 * 9 : var0 & ~(1 << var1 + var2 * 3 + var3 * 9);
   }

   public int GetRLightLevel() {
      return (this.lightLevel & 16711680) >> 16;
   }

   public int GetGLightLevel() {
      return (this.lightLevel & '\uff00') >> 8;
   }

   public int GetBLightLevel() {
      return this.lightLevel & 255;
   }

   public void SetRLightLevel(int var1) {
      this.lightLevel = this.lightLevel & -16711681 | var1 << 16;
   }

   public void SetGLightLevel(int var1) {
      this.lightLevel = this.lightLevel & -65281 | var1 << 8;
   }

   public void SetBLightLevel(int var1) {
      this.lightLevel = this.lightLevel & -256 | var1;
   }

   public SquareCoord getCoords() {
      return new SquareCoord(this.x, this.y, this.z);
   }

   public void setPlayerCutawayFlag(int var1, int var2, long var3) {
      this.targetPlayerCutawayFlags[var1] = (byte)(var2 & 3);
      if (var3 > this.playerCutawayFlagLockUntilTimes[var1] && this.playerCutawayFlags[var1] != this.targetPlayerCutawayFlags[var1]) {
         this.playerCutawayFlags[var1] = this.targetPlayerCutawayFlags[var1];
         this.playerCutawayFlagLockUntilTimes[var1] = var3 + 750L;
      }

   }

   public void addPlayerCutawayFlag(int var1, int var2, long var3) {
      int var5 = this.targetPlayerCutawayFlags[var1] | var2;
      this.setPlayerCutawayFlag(var1, var5, var3);
   }

   public void clearPlayerCutawayFlag(int var1, int var2, long var3) {
      int var5 = this.targetPlayerCutawayFlags[var1] & ~var2;
      this.setPlayerCutawayFlag(var1, var5, var3);
   }

   public int getPlayerCutawayFlag(int var1, long var2) {
      if (PerformanceSettings.FBORenderChunk) {
         return this.targetPlayerCutawayFlags[var1];
      } else {
         return var2 > this.playerCutawayFlagLockUntilTimes[var1] ? this.targetPlayerCutawayFlags[var1] : this.playerCutawayFlags[var1];
      }
   }

   public void setIsDissolved(int var1, boolean var2, long var3) {
      this.targetPlayerIsDissolvedFlags[var1] = var2;
      if (var3 > this.playerIsDissolvedFlagLockUntilTimes[var1] && this.playerIsDissolvedFlags[var1] != this.targetPlayerIsDissolvedFlags[var1]) {
         this.playerIsDissolvedFlags[var1] = this.targetPlayerIsDissolvedFlags[var1];
         this.playerIsDissolvedFlagLockUntilTimes[var1] = var3 + 750L;
      }

   }

   public boolean getIsDissolved(int var1, long var2) {
      return var2 > this.playerIsDissolvedFlagLockUntilTimes[var1] ? this.targetPlayerIsDissolvedFlags[var1] : this.playerIsDissolvedFlags[var1];
   }

   public IsoWaterGeometry getWater() {
      if (this.water != null && this.water.m_adjacentChunkLoadedCounter != this.chunk.m_adjacentChunkLoadedCounter) {
         this.water.m_adjacentChunkLoadedCounter = this.chunk.m_adjacentChunkLoadedCounter;
         if (this.water.hasWater || this.water.bShore) {
            this.clearWater();
         }
      }

      if (this.water == null) {
         try {
            this.water = (IsoWaterGeometry)IsoWaterGeometry.pool.alloc();
            this.water.m_adjacentChunkLoadedCounter = this.chunk.m_adjacentChunkLoadedCounter;
            if (this.water.init(this) == null) {
               this.clearWater();
            }
         } catch (Exception var2) {
            this.clearWater();
         }
      }

      return this.water;
   }

   public void clearWater() {
      if (this.water != null) {
         IsoWaterGeometry.pool.release((Object)this.water);
         this.water = null;
      }

   }

   public IsoPuddlesGeometry getPuddles() {
      if (this.puddles == null) {
         try {
            synchronized(IsoPuddlesGeometry.pool) {
               this.puddles = (IsoPuddlesGeometry)IsoPuddlesGeometry.pool.alloc();
            }

            this.puddles.square = this;
            this.puddles.bRecalc = true;
         } catch (Exception var4) {
            this.clearPuddles();
         }
      }

      return this.puddles;
   }

   public void clearPuddles() {
      if (this.puddles != null) {
         this.puddles.square = null;
         synchronized(IsoPuddlesGeometry.pool) {
            IsoPuddlesGeometry.pool.release((Object)this.puddles);
         }

         this.puddles = null;
      }

   }

   public float getPuddlesInGround() {
      if (this.isInARoom()) {
         return -1.0F;
      } else {
         if ((double)Math.abs(IsoPuddles.getInstance().getPuddlesSize() + (float)Core.getInstance().getPerfPuddles() + (float)IsoCamera.frameState.OffscreenWidth - this.puddlesCacheSize) > 0.01) {
            this.puddlesCacheSize = IsoPuddles.getInstance().getPuddlesSize() + (float)Core.getInstance().getPerfPuddles() + (float)IsoCamera.frameState.OffscreenWidth;
            this.puddlesCacheLevel = IsoPuddlesCompute.computePuddle(this);
         }

         return this.puddlesCacheLevel;
      }
   }

   public void removeUnderground() {
      IsoObject[] var1 = (IsoObject[])this.Objects.getElements();

      for(int var2 = 0; var2 < var1.length; ++var2) {
         IsoObject var3 = var1[var2];
         if (var3 != null && var3.tile != null && var3.tile.startsWith("underground")) {
            this.getObjects().remove(var3);
            return;
         }
      }

   }

   public boolean isInsideRectangle(int var1, int var2, int var3, int var4) {
      return this.x >= var1 && this.y >= var2 && this.x < var1 + var3 && this.y < var2 + var4;
   }

   public IBiome getBiome() {
      return this.biome;
   }

   public void setBiome(IBiome var1) {
      this.biome = var1;
   }

   public IsoGridOcclusionData getOcclusionData() {
      return this.OcclusionDataCache;
   }

   public IsoGridOcclusionData getOrCreateOcclusionData() {
      assert !GameServer.bServer;

      if (this.OcclusionDataCache == null) {
         this.OcclusionDataCache = new IsoGridOcclusionData(this);
      }

      return this.OcclusionDataCache;
   }

   public void softClear() {
      this.zone = null;
      this.room = null;
      this.w = null;
      this.nw = null;
      this.sw = null;
      this.s = null;
      this.n = null;
      this.ne = null;
      this.se = null;
      this.e = null;
      this.u = null;
      this.d = null;
      this.isoWorldRegion = null;
      this.hasSetIsoWorldRegion = false;

      for(int var1 = 0; var1 < 8; ++var1) {
         this.nav[var1] = null;
      }

   }

   public float getGridSneakModifier(boolean var1) {
      if (!var1) {
         if (this.Properties.Is("CloseSneakBonus")) {
            return (float)Integer.parseInt(this.Properties.Val("CloseSneakBonus")) / 100.0F;
         }

         if (this.Properties.Is(IsoFlagType.collideN) || this.Properties.Is(IsoFlagType.collideW) || this.Properties.Is(IsoFlagType.WindowN) || this.Properties.Is(IsoFlagType.WindowW) || this.Properties.Is(IsoFlagType.doorN) || this.Properties.Is(IsoFlagType.doorW)) {
            return 8.0F;
         }
      } else if (this.Properties.Is(IsoFlagType.solidtrans)) {
         return 4.0F;
      }

      return 1.0F;
   }

   public boolean isSomethingTo(IsoGridSquare var1) {
      return this.isWallTo(var1) || this.isWindowTo(var1) || this.isDoorTo(var1);
   }

   public IsoObject getTransparentWallTo(IsoGridSquare var1) {
      if (var1 != null && var1 != this && this.isWallTo(var1)) {
         if (var1.x > this.x && var1.Properties.Is(IsoFlagType.SpearOnlyAttackThrough) && !var1.Properties.Is(IsoFlagType.WindowW)) {
            return var1.getWall();
         } else if (this.x > var1.x && this.Properties.Is(IsoFlagType.SpearOnlyAttackThrough) && !this.Properties.Is(IsoFlagType.WindowW)) {
            return this.getWall();
         } else if (var1.y > this.y && var1.Properties.Is(IsoFlagType.SpearOnlyAttackThrough) && !var1.Properties.Is(IsoFlagType.WindowN)) {
            return var1.getWall();
         } else if (this.y > var1.y && this.Properties.Is(IsoFlagType.SpearOnlyAttackThrough) && !this.Properties.Is(IsoFlagType.WindowN)) {
            return this.getWall();
         } else {
            if (var1.x != this.x && var1.y != this.y) {
               IsoObject var2 = this.getTransparentWallTo(IsoWorld.instance.CurrentCell.getGridSquare(var1.x, this.y, this.z));
               IsoObject var3 = this.getTransparentWallTo(IsoWorld.instance.CurrentCell.getGridSquare(this.x, var1.y, this.z));
               if (var2 != null) {
                  return var2;
               }

               if (var3 != null) {
                  return var3;
               }

               var2 = var1.getTransparentWallTo(IsoWorld.instance.CurrentCell.getGridSquare(var1.x, this.y, this.z));
               var3 = var1.getTransparentWallTo(IsoWorld.instance.CurrentCell.getGridSquare(this.x, var1.y, this.z));
               if (var2 != null) {
                  return var2;
               }

               if (var3 != null) {
                  return var3;
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   public boolean isWallTo(IsoGridSquare var1) {
      if (var1 != null && var1 != this) {
         if (var1.x > this.x && var1.Properties.Is(IsoFlagType.collideW) && !var1.Properties.Is(IsoFlagType.WindowW)) {
            return true;
         } else if (this.x > var1.x && this.Properties.Is(IsoFlagType.collideW) && !this.Properties.Is(IsoFlagType.WindowW)) {
            return true;
         } else if (var1.y > this.y && var1.Properties.Is(IsoFlagType.collideN) && !var1.Properties.Is(IsoFlagType.WindowN)) {
            return true;
         } else if (this.y > var1.y && this.Properties.Is(IsoFlagType.collideN) && !this.Properties.Is(IsoFlagType.WindowN)) {
            return true;
         } else {
            if (var1.x != this.x && var1.y != this.y) {
               if (this.isWallTo(IsoWorld.instance.CurrentCell.getGridSquare(var1.x, this.y, this.z), 1) || this.isWallTo(IsoWorld.instance.CurrentCell.getGridSquare(this.x, var1.y, this.z), 1)) {
                  return true;
               }

               if (var1.isWallTo(IsoWorld.instance.CurrentCell.getGridSquare(var1.x, this.y, this.z), 1) || var1.isWallTo(IsoWorld.instance.CurrentCell.getGridSquare(this.x, var1.y, this.z), 1)) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public boolean isWallTo(IsoGridSquare var1, int var2) {
      if (var2 > 100) {
         boolean var3 = false;
      }

      if (var1 != null && var1 != this) {
         if (var1.x > this.x && var1.Properties.Is(IsoFlagType.collideW) && !var1.Properties.Is(IsoFlagType.WindowW)) {
            return true;
         } else if (this.x > var1.x && this.Properties.Is(IsoFlagType.collideW) && !this.Properties.Is(IsoFlagType.WindowW)) {
            return true;
         } else if (var1.y > this.y && var1.Properties.Is(IsoFlagType.collideN) && !var1.Properties.Is(IsoFlagType.WindowN)) {
            return true;
         } else if (this.y > var1.y && this.Properties.Is(IsoFlagType.collideN) && !this.Properties.Is(IsoFlagType.WindowN)) {
            return true;
         } else {
            if (var1.x != this.x && var1.y != this.y) {
               if (this.isWallTo(IsoWorld.instance.CurrentCell.getGridSquare(var1.x, this.y, this.z), var2 + 1) || this.isWallTo(IsoWorld.instance.CurrentCell.getGridSquare(this.x, var1.y, this.z), var2 + 1)) {
                  return true;
               }

               if (var1.isWallTo(IsoWorld.instance.CurrentCell.getGridSquare(var1.x, this.y, this.z), var2 + 1) || var1.isWallTo(IsoWorld.instance.CurrentCell.getGridSquare(this.x, var1.y, this.z), var2 + 1)) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public boolean isWindowTo(IsoGridSquare var1) {
      if (var1 != null && var1 != this) {
         if (var1.x > this.x && var1.Properties.Is(IsoFlagType.windowW)) {
            return true;
         } else if (this.x > var1.x && this.Properties.Is(IsoFlagType.windowW)) {
            return true;
         } else if (var1.y > this.y && var1.Properties.Is(IsoFlagType.windowN)) {
            return true;
         } else if (this.y > var1.y && this.Properties.Is(IsoFlagType.windowN)) {
            return true;
         } else {
            if (var1.x != this.x && var1.y != this.y) {
               if (this.isWindowTo(IsoWorld.instance.CurrentCell.getGridSquare(var1.x, this.y, this.z)) || this.isWindowTo(IsoWorld.instance.CurrentCell.getGridSquare(this.x, var1.y, this.z))) {
                  return true;
               }

               if (var1.isWindowTo(IsoWorld.instance.CurrentCell.getGridSquare(var1.x, this.y, this.z)) || var1.isWindowTo(IsoWorld.instance.CurrentCell.getGridSquare(this.x, var1.y, this.z))) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public boolean haveDoor() {
      for(int var1 = 0; var1 < this.Objects.size(); ++var1) {
         if (this.Objects.get(var1) instanceof IsoDoor) {
            return true;
         }
      }

      return false;
   }

   public boolean hasDoorOnEdge(IsoDirections var1, boolean var2) {
      for(int var3 = 0; var3 < this.SpecialObjects.size(); ++var3) {
         IsoDoor var4 = (IsoDoor)Type.tryCastTo((IsoObject)this.SpecialObjects.get(var3), IsoDoor.class);
         if (var4 != null && var4.getSpriteEdge(var2) == var1) {
            return true;
         }

         IsoThumpable var5 = (IsoThumpable)Type.tryCastTo((IsoObject)this.SpecialObjects.get(var3), IsoThumpable.class);
         if (var5 != null && var5.getSpriteEdge(var2) == var1) {
            return true;
         }
      }

      return false;
   }

   public boolean hasClosedDoorOnEdge(IsoDirections var1) {
      boolean var2 = false;

      for(int var3 = 0; var3 < this.SpecialObjects.size(); ++var3) {
         IsoDoor var4 = (IsoDoor)Type.tryCastTo((IsoObject)this.SpecialObjects.get(var3), IsoDoor.class);
         if (var4 != null && !var4.IsOpen() && var4.getSpriteEdge(var2) == var1) {
            return true;
         }

         IsoThumpable var5 = (IsoThumpable)Type.tryCastTo((IsoObject)this.SpecialObjects.get(var3), IsoThumpable.class);
         if (var5 != null && !var5.IsOpen() && var5.getSpriteEdge(var2) == var1) {
            return true;
         }
      }

      return false;
   }

   public boolean hasOpenDoorOnEdge(IsoDirections var1) {
      boolean var2 = false;

      for(int var3 = 0; var3 < this.SpecialObjects.size(); ++var3) {
         IsoDoor var4 = (IsoDoor)Type.tryCastTo((IsoObject)this.SpecialObjects.get(var3), IsoDoor.class);
         if (var4 != null && var4.IsOpen() && var4.getSpriteEdge(var2) == var1) {
            return true;
         }

         IsoThumpable var5 = (IsoThumpable)Type.tryCastTo((IsoObject)this.SpecialObjects.get(var3), IsoThumpable.class);
         if (var5 != null && var5.IsOpen() && var5.getSpriteEdge(var2) == var1) {
            return true;
         }
      }

      return false;
   }

   public boolean isDoorTo(IsoGridSquare var1) {
      if (var1 != null && var1 != this) {
         if (var1.x > this.x && var1.Properties.Is(IsoFlagType.doorW)) {
            return true;
         } else if (this.x > var1.x && this.Properties.Is(IsoFlagType.doorW)) {
            return true;
         } else if (var1.y > this.y && var1.Properties.Is(IsoFlagType.doorN)) {
            return true;
         } else if (this.y > var1.y && this.Properties.Is(IsoFlagType.doorN)) {
            return true;
         } else {
            if (var1.x != this.x && var1.y != this.y) {
               if (this.isDoorTo(IsoWorld.instance.CurrentCell.getGridSquare(var1.x, this.y, this.z)) || this.isDoorTo(IsoWorld.instance.CurrentCell.getGridSquare(this.x, var1.y, this.z))) {
                  return true;
               }

               if (var1.isDoorTo(IsoWorld.instance.CurrentCell.getGridSquare(var1.x, this.y, this.z)) || var1.isDoorTo(IsoWorld.instance.CurrentCell.getGridSquare(this.x, var1.y, this.z))) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public boolean isBlockedTo(IsoGridSquare var1) {
      return this.isWallTo(var1) || this.isWindowBlockedTo(var1) || this.isDoorBlockedTo(var1);
   }

   public boolean canReachTo(IsoGridSquare var1) {
      if (var1 == this) {
         return true;
      } else if (Math.abs(var1.x - this.x) <= 1 && Math.abs(var1.y - this.y) <= 1 && !this.isWindowBlockedTo(var1) && !this.isDoorBlockedTo(var1)) {
         if (!this.isWallTo(var1)) {
            return true;
         } else if (var1.y < this.y && this.getWallExcludingList(true, ignoreBlockingSprites) != null) {
            return false;
         } else if (this.y < var1.y && var1.getWallExcludingList(true, ignoreBlockingSprites) != null) {
            return false;
         } else if (var1.x < this.x && this.getWallExcludingList(false, ignoreBlockingSprites) != null) {
            return false;
         } else {
            return this.x >= var1.x || var1.getWallExcludingList(false, ignoreBlockingSprites) == null;
         }
      } else {
         return false;
      }
   }

   public boolean isWindowBlockedTo(IsoGridSquare var1) {
      if (var1 == null) {
         return false;
      } else if (var1.x > this.x && var1.hasBlockedWindow(false)) {
         return true;
      } else if (this.x > var1.x && this.hasBlockedWindow(false)) {
         return true;
      } else if (var1.y > this.y && var1.hasBlockedWindow(true)) {
         return true;
      } else if (this.y > var1.y && this.hasBlockedWindow(true)) {
         return true;
      } else {
         if (var1.x != this.x && var1.y != this.y) {
            if (this.isWindowBlockedTo(IsoWorld.instance.CurrentCell.getGridSquare(var1.x, this.y, this.z)) || this.isWindowBlockedTo(IsoWorld.instance.CurrentCell.getGridSquare(this.x, var1.y, this.z))) {
               return true;
            }

            if (var1.isWindowBlockedTo(IsoWorld.instance.CurrentCell.getGridSquare(var1.x, this.y, this.z)) || var1.isWindowBlockedTo(IsoWorld.instance.CurrentCell.getGridSquare(this.x, var1.y, this.z))) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean hasBlockedWindow(boolean var1) {
      for(int var2 = 0; var2 < this.Objects.size(); ++var2) {
         IsoObject var3 = (IsoObject)this.Objects.get(var2);
         if (var3 instanceof IsoWindow var4) {
            if (var4.getNorth() == var1) {
               return !var4.isDestroyed() && !var4.open || var4.isBarricaded();
            }
         }
      }

      return false;
   }

   public boolean isDoorBlockedTo(IsoGridSquare var1) {
      if (var1 == null) {
         return false;
      } else if (var1.x > this.x && var1.hasBlockedDoor(false)) {
         return true;
      } else if (this.x > var1.x && this.hasBlockedDoor(false)) {
         return true;
      } else if (var1.y > this.y && var1.hasBlockedDoor(true)) {
         return true;
      } else if (this.y > var1.y && this.hasBlockedDoor(true)) {
         return true;
      } else {
         if (var1.x != this.x && var1.y != this.y) {
            if (this.isDoorBlockedTo(IsoWorld.instance.CurrentCell.getGridSquare(var1.x, this.y, this.z)) || this.isDoorBlockedTo(IsoWorld.instance.CurrentCell.getGridSquare(this.x, var1.y, this.z))) {
               return true;
            }

            if (var1.isDoorBlockedTo(IsoWorld.instance.CurrentCell.getGridSquare(var1.x, this.y, this.z)) || var1.isDoorBlockedTo(IsoWorld.instance.CurrentCell.getGridSquare(this.x, var1.y, this.z))) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean hasBlockedDoor(boolean var1) {
      for(int var2 = 0; var2 < this.Objects.size(); ++var2) {
         IsoObject var3 = (IsoObject)this.Objects.get(var2);
         if (var3 instanceof IsoDoor var4) {
            if (var4.getNorth() == var1) {
               return !var4.open || var4.isBarricaded();
            }
         }

         if (var3 instanceof IsoThumpable var5) {
            if (var5.isDoor() && var5.getNorth() == var1) {
               return !var5.open || var5.isBarricaded();
            }
         }
      }

      return false;
   }

   public IsoCurtain getCurtain(IsoObjectType var1) {
      for(int var2 = 0; var2 < this.getSpecialObjects().size(); ++var2) {
         IsoCurtain var3 = (IsoCurtain)Type.tryCastTo((IsoObject)this.getSpecialObjects().get(var2), IsoCurtain.class);
         if (var3 != null && var3.getType() == var1) {
            return var3;
         }
      }

      return null;
   }

   public IsoObject getHoppable(boolean var1) {
      for(int var2 = 0; var2 < this.Objects.size(); ++var2) {
         IsoObject var3 = (IsoObject)this.Objects.get(var2);
         PropertyContainer var4 = var3.getProperties();
         if (var4 != null && var4.Is(var1 ? IsoFlagType.HoppableN : IsoFlagType.HoppableW)) {
            return var3;
         }

         if (var4 != null && var4.Is(var1 ? IsoFlagType.WindowN : IsoFlagType.WindowW)) {
            return var3;
         }
      }

      return null;
   }

   public IsoObject getHoppableTo(IsoGridSquare var1) {
      if (var1 != null && var1 != this) {
         IsoObject var2;
         if (var1.x < this.x && var1.y == this.y) {
            var2 = this.getHoppable(false);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.x == this.x && var1.y < this.y) {
            var2 = this.getHoppable(true);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.x > this.x && var1.y == this.y) {
            var2 = var1.getHoppable(false);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.x == this.x && var1.y > this.y) {
            var2 = var1.getHoppable(true);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.x != this.x && var1.y != this.y) {
            IsoGridSquare var3 = this.getCell().getGridSquare(this.x, var1.y, this.z);
            IsoGridSquare var4 = this.getCell().getGridSquare(var1.x, this.y, this.z);
            var2 = this.getHoppableTo(var3);
            if (var2 != null) {
               return var2;
            }

            var2 = this.getHoppableTo(var4);
            if (var2 != null) {
               return var2;
            }

            var2 = var1.getHoppableTo(var3);
            if (var2 != null) {
               return var2;
            }

            var2 = var1.getHoppableTo(var4);
            if (var2 != null) {
               return var2;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public boolean isHoppableTo(IsoGridSquare var1) {
      if (var1 == null) {
         return false;
      } else if (var1.x != this.x && var1.y != this.y) {
         return false;
      } else if (var1.x > this.x && var1.Properties.Is(IsoFlagType.HoppableW)) {
         return true;
      } else if (this.x > var1.x && this.Properties.Is(IsoFlagType.HoppableW)) {
         return true;
      } else if (var1.y > this.y && var1.Properties.Is(IsoFlagType.HoppableN)) {
         return true;
      } else {
         return this.y > var1.y && this.Properties.Is(IsoFlagType.HoppableN);
      }
   }

   public void discard() {
      this.hourLastSeen = -32768;
      this.chunk = null;
      this.zone = null;
      this.LightInfluenceB = null;
      this.LightInfluenceG = null;
      this.LightInfluenceR = null;
      this.room = null;
      this.w = null;
      this.nw = null;
      this.sw = null;
      this.s = null;
      this.n = null;
      this.ne = null;
      this.se = null;
      this.e = null;
      this.u = null;
      this.d = null;
      this.isoWorldRegion = null;
      this.hasSetIsoWorldRegion = false;
      this.nav[0] = null;
      this.nav[1] = null;
      this.nav[2] = null;
      this.nav[3] = null;
      this.nav[4] = null;
      this.nav[5] = null;
      this.nav[6] = null;
      this.nav[7] = null;

      for(int var1 = 0; var1 < 4; ++var1) {
         if (this.lighting[var1] != null) {
            this.lighting[var1].reset();
         }

         this.lightInfo[var1] = null;
      }

      this.SolidFloorCached = false;
      this.SolidFloor = false;
      this.CacheIsFree = false;
      this.CachedIsFree = false;
      this.chunk = null;
      this.roomID = -1L;
      this.DeferedCharacters.clear();
      this.DeferredCharacterTick = -1;
      this.StaticMovingObjects.clear();
      this.MovingObjects.clear();
      this.Objects.clear();
      this.WorldObjects.clear();
      this.hasTypes.clear();
      this.table = null;
      this.Properties.Clear();
      this.SpecialObjects.clear();
      this.RainDrop = null;
      this.RainSplash = null;
      this.overlayDone = false;
      this.haveRoof = false;
      this.burntOut = false;
      this.trapPositionX = this.trapPositionY = this.trapPositionZ = -1;
      this.haveSheetRope = false;
      if (this.erosion != null) {
         this.erosion.reset();
      }

      if (this.OcclusionDataCache != null) {
         this.OcclusionDataCache.Reset();
      }

      this.roofHideBuilding = null;
      this.bHasFlies = false;
      Arrays.fill(this.playerCutawayFlags, (byte)0);
      Arrays.fill(this.playerCutawayFlagLockUntilTimes, 0L);
      Arrays.fill(this.targetPlayerCutawayFlags, (byte)0);
      isoGridSquareCache.add(this);
   }

   private static boolean validateUser(String var0, String var1) throws MalformedURLException, IOException {
      URL var2 = new URL("http://www.projectzomboid.com/scripts/auth.php?username=" + var0 + "&password=" + var1);
      URLConnection var3 = var2.openConnection();
      BufferedReader var4 = new BufferedReader(new InputStreamReader(var3.getInputStream()));

      String var5;
      do {
         if ((var5 = var4.readLine()) == null) {
            return false;
         }
      } while(!var5.contains("success"));

      return true;
   }

   public float DistTo(int var1, int var2) {
      return IsoUtils.DistanceManhatten((float)var1 + 0.5F, (float)var2 + 0.5F, (float)this.x, (float)this.y);
   }

   public float DistTo(IsoGridSquare var1) {
      return IsoUtils.DistanceManhatten((float)this.x + 0.5F, (float)this.y + 0.5F, (float)var1.x + 0.5F, (float)var1.y + 0.5F);
   }

   public float DistToProper(int var1, int var2) {
      return IsoUtils.DistanceManhatten((float)var1 + 0.5F, (float)var2 + 0.5F, (float)this.x + 0.5F, (float)this.y + 0.5F);
   }

   public float DistToProper(IsoGridSquare var1) {
      return IsoUtils.DistanceTo((float)this.x + 0.5F, (float)this.y + 0.5F, (float)var1.x + 0.5F, (float)var1.y + 0.5F);
   }

   public float DistTo(IsoMovingObject var1) {
      return IsoUtils.DistanceManhatten((float)this.x + 0.5F, (float)this.y + 0.5F, var1.getX(), var1.getY());
   }

   public float DistToProper(IsoMovingObject var1) {
      return IsoUtils.DistanceTo((float)this.x + 0.5F, (float)this.y + 0.5F, var1.getX(), var1.getY());
   }

   public boolean isSafeToSpawn() {
      choices.clear();
      this.isSafeToSpawn(this, 0);
      if (choices.size() > 7) {
         choices.clear();
         return true;
      } else {
         choices.clear();
         return false;
      }
   }

   public void isSafeToSpawn(IsoGridSquare var1, int var2) {
      if (var2 <= 5) {
         choices.add(var1);
         if (var1.n != null && !choices.contains(var1.n)) {
            this.isSafeToSpawn(var1.n, var2 + 1);
         }

         if (var1.s != null && !choices.contains(var1.s)) {
            this.isSafeToSpawn(var1.s, var2 + 1);
         }

         if (var1.e != null && !choices.contains(var1.e)) {
            this.isSafeToSpawn(var1.e, var2 + 1);
         }

         if (var1.w != null && !choices.contains(var1.w)) {
            this.isSafeToSpawn(var1.w, var2 + 1);
         }

      }
   }

   public static boolean auth(String var0, char[] var1) {
      if (var0.length() > 64) {
         return false;
      } else {
         String var2 = var1.toString();
         if (var2.length() > 64) {
            return false;
         } else {
            try {
               return validateUser(var0, var2);
            } catch (MalformedURLException var4) {
               Logger.getLogger(IsoGridSquare.class.getName()).log(Level.SEVERE, (String)null, var4);
            } catch (IOException var5) {
               Logger.getLogger(IsoGridSquare.class.getName()).log(Level.SEVERE, (String)null, var5);
            }

            return false;
         }
      }
   }

   private void renderAttachedSpritesWithNoWallLighting(IsoObject var1, ColorInfo var2, Consumer<TextureDraw> var3) {
      if (var1.AttachedAnimSprite != null && !var1.AttachedAnimSprite.isEmpty()) {
         boolean var4 = false;

         for(int var5 = 0; var5 < var1.AttachedAnimSprite.size(); ++var5) {
            IsoSpriteInstance var6 = (IsoSpriteInstance)var1.AttachedAnimSprite.get(var5);
            if (var6.parentSprite != null && var6.parentSprite.Properties.Is(IsoFlagType.NoWallLighting)) {
               var4 = true;
               break;
            }
         }

         if (var4) {
            defColorInfo.r = var2.r;
            defColorInfo.g = var2.g;
            defColorInfo.b = var2.b;
            float var8 = defColorInfo.a;
            if (DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
               defColorInfo.set(1.0F, 1.0F, 1.0F, defColorInfo.a);
            }

            IsoSpriteInstance var7;
            int var9;
            if (CircleStencil) {
               IndieGL.enableStencilTest();
               IndieGL.enableAlphaTest();
               IndieGL.glAlphaFunc(516, 0.02F);
               IndieGL.glStencilFunc(517, 128, 128);

               for(var9 = 0; var9 < var1.AttachedAnimSprite.size(); ++var9) {
                  var7 = (IsoSpriteInstance)var1.AttachedAnimSprite.get(var9);
                  if (var7.parentSprite != null && var7.parentSprite.Properties.Is(IsoFlagType.NoWallLighting)) {
                     defColorInfo.a = var7.alpha;
                     var7.render(var1, (float)this.x, (float)this.y, (float)this.z, var1.dir, var1.offsetX, var1.offsetY + var1.getRenderYOffset() * (float)Core.TileScale, defColorInfo, true, var3);
                  }
               }

               IndieGL.glStencilFunc(519, 255, 255);
            } else {
               for(var9 = 0; var9 < var1.AttachedAnimSprite.size(); ++var9) {
                  var7 = (IsoSpriteInstance)var1.AttachedAnimSprite.get(var9);
                  if (var7.parentSprite != null && var7.parentSprite.Properties.Is(IsoFlagType.NoWallLighting)) {
                     defColorInfo.a = var7.alpha;
                     var7.render(var1, (float)this.x, (float)this.y, (float)this.z, var1.dir, var1.offsetX, var1.offsetY + var1.getRenderYOffset() * (float)Core.TileScale, defColorInfo);
                     var7.update();
                  }
               }
            }

            defColorInfo.r = 1.0F;
            defColorInfo.g = 1.0F;
            defColorInfo.b = 1.0F;
            defColorInfo.a = var8;
         }
      }
   }

   public void DoCutawayShader(IsoObject var1, IsoDirections var2, int var3, int var4, int var5, int var6, int var7, boolean var8, boolean var9, boolean var10, boolean var11, WallShaper var12) {
      Texture var13 = Texture.getSharedTexture("media/wallcutaways.png", 3);
      if (var13 != null && var13.getID() != -1) {
         boolean var14 = var1.sprite.getProperties().Is(IsoFlagType.NoWallLighting);
         int var15 = IsoCamera.frameState.playerIndex;
         var1.getRenderInfo(var15).m_bCutaway = true;
         IsoGridSquare var16 = this.getAdjacentSquare(IsoDirections.N);
         IsoGridSquare var17 = this.getAdjacentSquare(IsoDirections.S);
         IsoGridSquare var18 = this.getAdjacentSquare(IsoDirections.W);
         IsoGridSquare var19 = this.getAdjacentSquare(IsoDirections.E);
         ColorInfo var20 = this.lightInfo[var15];
         if (var1.getProperties().Is("GarageDoor")) {
            var1.renderWallTileOnly(var2, (float)this.x, (float)this.y, (float)this.z, var14 ? var20 : defColorInfo, (Shader)null, var12);
         }

         String var21 = var1.getProperties().Val("CutawayHint");

         try {
            String var22 = var1.getSprite().getName();
            String var23 = var1.getSprite().tilesetName;
            int var24 = var1.getSprite().tileSheetIndex;
            if (var23 == null) {
               if (var22 != null) {
                  int var25 = var22.lastIndexOf(95);
                  if (var25 != -1) {
                     var22.substring(0, var25);
                     var24 = PZMath.tryParseInt(var22.substring(var25 + 1), -1);
                  }
               } else {
                  var23 = "";
               }
            }

            CircleStencilShader var33 = IsoGridSquare.CircleStencilShader.instance;
            SpriteRenderer.WallShaderTexRender var26 = null;
            short var27;
            short var28;
            Texture var29;
            if (var2 == IsoDirections.N || var2 == IsoDirections.NW) {
               var27 = 700;
               if ((var3 & 1) != 0) {
                  if ((var7 & 1) == 0 && hasCutawayCapableWallNorth(var19)) {
                     var27 = 444;
                  }

                  if ((var6 & 1) == 0 && hasCutawayCapableWallNorth(var18)) {
                     var27 = 956;
                  } else if ((var4 & 2) == 0 && hasCutawayCapableWallWest(var16)) {
                     var27 = 956;
                  }
               } else if ((var7 & 1) == 0) {
                  var27 = 828;
               } else {
                  var27 = 956;
               }

               var28 = 0;
               if (!PerformanceSettings.FBORenderChunk || FBORenderCell.lowestCutawayObject == var1) {
                  if (var8) {
                     var28 = 904;
                     if (var21 != null) {
                        if ("DoubleDoorLeft".equals(var21)) {
                           var28 = 1130;
                        } else if ("DoubleDoorRight".equals(var21)) {
                           var28 = 1356;
                        } else if ("GarageDoorLeft".equals(var21)) {
                           var27 = 444;
                           var28 = 1808;
                        } else if ("GarageDoorMiddle".equals(var21)) {
                           var27 = 572;
                           var28 = 1808;
                        } else if ("GarageDoorRight".equals(var21)) {
                           var27 = 700;
                           var28 = 1808;
                        }
                     }
                  } else if (var10) {
                     var28 = 226;
                     if (var21 != null) {
                        if ("DoubleWindowLeft".equals(var21)) {
                           var28 = 678;
                        } else if ("DoubleWindowRight".equals(var21)) {
                           var28 = 452;
                        }
                     }
                  }
               }

               colu = this.getVertLight(0, var15);
               coll = this.getVertLight(1, var15);
               colu2 = this.getVertLight(4, var15);
               coll2 = this.getVertLight(5, var15);
               if (Core.bDebug && DebugOptions.instance.DebugDraw_SkipWorldShading.getValue()) {
                  coll2 = -1;
                  colu2 = -1;
                  coll = -1;
                  colu = -1;
                  var20 = defColorInfo;
               }

               SpriteRenderer.instance.setCutawayTexture(var13, var27, var28, 66, 226);
               if (var2 == IsoDirections.N) {
                  var29 = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.NWall);
                  SpriteRenderer.instance.setCutawayTexture2(var29, 0, 0, 0, 0);
               }

               if (var2 == IsoDirections.NW) {
                  var29 = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.NWWall);
                  SpriteRenderer.instance.setCutawayTexture2(var29, 0, 0, 0, 0);
               }

               if (var2 == IsoDirections.N) {
                  var26 = SpriteRenderer.WallShaderTexRender.All;
               } else {
                  var26 = SpriteRenderer.WallShaderTexRender.RightOnly;
               }

               SpriteRenderer.instance.setExtraWallShaderParams(var26);
               var12.col[0] = colu2;
               var12.col[1] = coll2;
               var12.col[2] = coll;
               var12.col[3] = colu;
               if (!var13.getTextureId().hasMipMaps()) {
                  IndieGL.glBlendFunc(770, 771);
               }

               var1.renderWallTileOnly(IsoDirections.Max, (float)this.x, (float)this.y, (float)this.z, var14 ? var20 : defColorInfo, var33, var12);
               if (PerformanceSettings.FBORenderChunk) {
                  this.DoCutawayShaderAttached(var1, var2, var13, var14, var20, var27, var28, 66, 226, colu2, coll2, coll, colu, var26);
               }

               if (!var13.getTextureId().hasMipMaps()) {
                  setBlendFunc();
               }
            }

            if (var2 == IsoDirections.W || var2 == IsoDirections.NW) {
               var27 = 512;
               if ((var5 & 2) != 0) {
                  if ((var4 & 2) == 0 && hasCutawayCapableWallWest(var16)) {
                     var27 = 768;
                  } else if ((var6 & 1) == 0 && hasCutawayCapableWallNorth(var18)) {
                     var27 = 768;
                  } else if ((var3 & 1) == 0 && hasCutawayCapableWallNorth(this)) {
                     var27 = 768;
                  }
               } else if ((var3 & 2) == 0) {
                  var27 = 896;
               } else if ((var4 & 2) == 0 && hasCutawayCapableWallWest(var16)) {
                  var27 = 768;
               } else if (hasCutawayCapableWallWest(var17)) {
                  var27 = 256;
               } else if ((var6 & 1) == 0 && hasCutawayCapableWallNorth(var18)) {
                  var27 = 768;
               }

               var28 = 0;
               if (!PerformanceSettings.FBORenderChunk || FBORenderCell.lowestCutawayObject == var1) {
                  if (var9) {
                     var28 = 904;
                     if (var21 != null) {
                        if ("GarageDoorLeft".equals(var21)) {
                           var27 = 0;
                           var28 = 1808;
                        } else if ("GarageDoorMiddle".equals(var21)) {
                           var27 = 128;
                           var28 = 1808;
                        } else if ("GarageDoorRight".equals(var21)) {
                           var27 = 256;
                           var28 = 1808;
                        } else if ("DoubleDoorLeft".equals(var21)) {
                           var28 = 1356;
                        } else if ("DoubleDoorRight".equals(var21)) {
                           var28 = 1130;
                        }
                     }
                  } else if (var11) {
                     var28 = 226;
                     if (var21 != null) {
                        if ("DoubleWindowLeft".equals(var21)) {
                           var28 = 452;
                        } else if ("DoubleWindowRight".equals(var21)) {
                           var28 = 678;
                        }
                     }
                  }
               }

               colu = this.getVertLight(0, var15);
               coll = this.getVertLight(3, var15);
               colu2 = this.getVertLight(4, var15);
               coll2 = this.getVertLight(7, var15);
               if (Core.bDebug && DebugOptions.instance.DebugDraw_SkipWorldShading.getValue()) {
                  coll2 = -1;
                  colu2 = -1;
                  coll = -1;
                  colu = -1;
                  var20 = defColorInfo;
               }

               SpriteRenderer.instance.setCutawayTexture(var13, var27, var28, 66, 226);
               if (var2 == IsoDirections.W) {
                  var29 = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.WWall);
                  SpriteRenderer.instance.setCutawayTexture2(var29, 0, 0, 0, 0);
               }

               if (var2 == IsoDirections.NW) {
                  var29 = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.NWWall);
                  SpriteRenderer.instance.setCutawayTexture2(var29, 0, 0, 0, 0);
               }

               if (var2 == IsoDirections.W) {
                  var26 = SpriteRenderer.WallShaderTexRender.All;
               } else {
                  var26 = SpriteRenderer.WallShaderTexRender.LeftOnly;
               }

               SpriteRenderer.instance.setExtraWallShaderParams(var26);
               var12.col[0] = coll2;
               var12.col[1] = colu2;
               var12.col[2] = colu;
               var12.col[3] = coll;
               if (!var13.getTextureId().hasMipMaps()) {
                  IndieGL.glBlendFunc(770, 771);
               }

               var1.renderWallTileOnly(IsoDirections.Max, (float)this.x, (float)this.y, (float)this.z, var14 ? var20 : defColorInfo, var33, var12);
               if (PerformanceSettings.FBORenderChunk) {
                  this.DoCutawayShaderAttached(var1, var2, var13, var14, var20, var27, var28, 66, 226, coll2, colu2, colu, coll, var26);
               }

               if (!var13.getTextureId().hasMipMaps()) {
                  setBlendFunc();
               }
            }

            if (var2 == IsoDirections.SE) {
               var27 = 1084;
               if ((var6 & 1) == 0 && hasCutawayCapableWallNorth(var18)) {
                  var27 = 1212;
               } else if ((var4 & 2) == 0 && hasCutawayCapableWallWest(var16)) {
                  var27 = 1212;
               }

               byte var34 = 0;
               SpriteRenderer.instance.setCutawayTexture(var13, var27, var34, 6, 196);
               var29 = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.SEWall);
               SpriteRenderer.instance.setCutawayTexture2(var29, 0, 0, 0, 0);
               SpriteRenderer.instance.setExtraWallShaderParams(SpriteRenderer.WallShaderTexRender.All);
               colu = this.getVertLight(0, var15);
               coll = this.getVertLight(3, var15);
               colu2 = this.getVertLight(4, var15);
               coll2 = this.getVertLight(7, var15);
               if (Core.bDebug && DebugOptions.instance.DebugDraw_SkipWorldShading.getValue()) {
                  coll2 = -1;
                  colu2 = -1;
                  coll = -1;
                  colu = -1;
                  var20 = defColorInfo;
               }

               var12.col[0] = coll2;
               var12.col[1] = colu2;
               var12.col[2] = colu;
               var12.col[3] = coll;
               if (!var13.getTextureId().hasMipMaps()) {
                  IndieGL.glBlendFunc(770, 771);
               }

               var1.renderWallTileOnly(IsoDirections.Max, (float)this.x, (float)this.y, (float)this.z, var14 ? var20 : defColorInfo, var33, var12);
               if (PerformanceSettings.FBORenderChunk) {
                  this.DoCutawayShaderAttached(var1, var2, var13, var14, var20, var27, var34, 66, 226, coll2, colu2, colu, coll, SpriteRenderer.WallShaderTexRender.All);
               }

               if (!var13.getTextureId().hasMipMaps()) {
                  setBlendFunc();
               }
            }
         } finally {
            SpriteRenderer.instance.setExtraWallShaderParams((SpriteRenderer.WallShaderTexRender)null);
            SpriteRenderer.instance.clearCutawayTexture();
            SpriteRenderer.instance.clearUseVertColorsArray();
         }

         if (!PerformanceSettings.FBORenderChunk) {
            var1.renderAttachedAndOverlaySprites(var1.dir, (float)this.x, (float)this.y, (float)this.z, var14 ? var20 : defColorInfo, false, !var14, (Shader)null, var12);
         }

      }
   }

   private void DoCutawayShaderAttached(IsoObject var1, IsoDirections var2, Texture var3, boolean var4, ColorInfo var5, int var6, int var7, int var8, int var9, int var10, int var11, int var12, int var13, SpriteRenderer.WallShaderTexRender var14) {
      if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Walls.AttachedSprites.getValue()) {
         if (SceneShaderStore.CutawayAttachedShader != null) {
            SpriteRenderer.instance.setExtraWallShaderParams((SpriteRenderer.WallShaderTexRender)null);
            SpriteRenderer.instance.clearCutawayTexture();
            SpriteRenderer.instance.clearUseVertColorsArray();
            IndieGL.pushShader(SceneShaderStore.CutawayAttachedShader);
            float var15 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), (float)this.x, (float)this.y, (float)this.z).depthStart;
            float var16 = (float)this.z + 1.0F;
            float var17 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), (float)(this.x + 1), (float)(this.y + 1), var16).depthStart;
            if (!FBORenderCell.instance.bRenderTranslucentOnly) {
               byte var18 = 8;
               IsoDepthHelper.Results var19 = IsoDepthHelper.getChunkDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX / (float)var18), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY / (float)var18), PZMath.fastfloor((float)this.x / (float)var18), PZMath.fastfloor((float)this.y / (float)var18), PZMath.fastfloor((float)this.z));
               float var20 = var19.depthStart;
               var15 -= var20;
               var17 -= var20;
            }

            IndieGL.shaderSetValue(SceneShaderStore.CutawayAttachedShader, "zDepth", var17);
            IndieGL.shaderSetValue(SceneShaderStore.CutawayAttachedShader, "zDepthBlendZ", var17);
            IndieGL.shaderSetValue(SceneShaderStore.CutawayAttachedShader, "zDepthBlendToZ", var15);
            CutawayAttachedModifier.instance.setVertColors(var10, var11, var12, var13);
            CutawayAttachedModifier.instance.setupWallDepth(var1.getSprite(), var2, var3, var6, var7, var8, var9, var14);
            IndieGL.glDefaultBlendFunc();
            var1.renderAttachedAndOverlaySprites(IsoDirections.Max, (float)this.x, (float)this.y, (float)this.z, var4 ? var5 : defColorInfo, true, !var4, (Shader)null, CutawayAttachedModifier.instance);
            this.renderAttachedSpritesWithNoWallLighting(var1, var5, CutawayAttachedModifier.instance);
            IndieGL.popShader(SceneShaderStore.CutawayAttachedShader);
         }
      }
   }

   public void DoCutawayShaderSprite(IsoSprite var1, IsoDirections var2, int var3, int var4, int var5, int var6, int var7) {
      CutawayNoDepthShader var8 = IsoGridSquare.CutawayNoDepthShader.getInstance();
      WallShaperWhole var9 = WallShaperWhole.instance;
      int var10 = IsoCamera.frameState.playerIndex;
      Texture var11 = Texture.getSharedTexture("media/wallcutaways.png", 3);
      if (var11 != null && var11.getID() != -1) {
         IsoGridSquare var12 = this.getAdjacentSquare(IsoDirections.N);
         IsoGridSquare var13 = this.getAdjacentSquare(IsoDirections.S);
         IsoGridSquare var14 = this.getAdjacentSquare(IsoDirections.W);
         IsoGridSquare var15 = this.getAdjacentSquare(IsoDirections.E);
         int var16 = 2 / Core.TileScale;

         try {
            Texture var17 = var1.getTextureForCurrentFrame(var2);
            float var18 = 0.0F;
            float var19 = var17.getOffsetY();
            int var20 = 0;
            int var21 = 226 - var17.getHeight() * var16;
            if (var2 != IsoDirections.NW) {
               var20 = 66 - var17.getWidth() * var16;
            }

            if (var1.getProperties().Is(IsoFlagType.WallSE)) {
               var20 = 6 - var17.getWidth() * var16;
               var21 = 196 - var17.getHeight() * var16;
            }

            if (var1.name.contains("fencing_01_11")) {
               var18 = 1.0F;
            } else if (var1.name.contains("carpentry_02_80")) {
               var18 = 1.0F;
            } else if (var1.name.contains("spiffos_01_71")) {
               var18 = -24.0F;
            } else {
               String var22;
               int var23;
               if (var1.name.contains("location_community_medical")) {
                  var22 = var1.name.replaceAll("(.*)_", "");
                  var23 = Integer.parseInt(var22);
                  switch (var23) {
                     case 45:
                     case 46:
                     case 47:
                     case 147:
                     case 148:
                     case 149:
                        var18 = -3.0F;
                  }
               } else if (var1.name.contains("walls_exterior_roofs")) {
                  var22 = var1.name.replaceAll("(.*)_", "");
                  var23 = Integer.parseInt(var22);
                  if (var23 == 4) {
                     var18 = -60.0F;
                  } else if (var23 == 17) {
                     var18 = -46.0F;
                  } else if (var23 == 28 && !var1.name.contains("03")) {
                     var18 = -60.0F;
                  } else if (var23 == 41) {
                     var18 = -46.0F;
                  }
               }
            }

            short var28;
            short var29;
            if (var2 == IsoDirections.N || var2 == IsoDirections.NW) {
               var28 = 700;
               var29 = 1084;
               if ((var3 & 1) == 0) {
                  var28 = 828;
                  var29 = 1212;
               } else if ((var3 & 1) != 0) {
                  var29 = 1212;
                  if ((var7 & 1) == 0 && hasCutawayCapableWallNorth(var15)) {
                     var28 = 444;
                  }

                  if ((var6 & 1) == 0 && hasCutawayCapableWallNorth(var14)) {
                     var28 = 956;
                  } else if ((var4 & 2) == 0 && hasCutawayCapableWallWest(var12)) {
                     var28 = 956;
                  }
               } else if ((var7 & 1) == 0) {
                  var28 = 828;
               } else {
                  var28 = 956;
               }

               colu = this.getVertLight(0, var10);
               coll = this.getVertLight(1, var10);
               colu2 = this.getVertLight(4, var10);
               coll2 = this.getVertLight(5, var10);
               if (var1.getProperties().Is(IsoFlagType.WallSE)) {
                  SpriteRenderer.instance.setCutawayTexture(var11, var29 + (int)var18, 0 + (int)var19, 6 - var20, 196 - var21);
               } else {
                  SpriteRenderer.instance.setCutawayTexture(var11, var28 + (int)var18, 0 + (int)var19, 66 - var20, 226 - var21);
               }

               if (var2 == IsoDirections.N) {
                  SpriteRenderer.instance.setExtraWallShaderParams(SpriteRenderer.WallShaderTexRender.All);
               } else {
                  SpriteRenderer.instance.setExtraWallShaderParams(SpriteRenderer.WallShaderTexRender.RightOnly);
               }

               var9.col[0] = colu2;
               var9.col[1] = coll2;
               var9.col[2] = coll;
               var9.col[3] = colu;
               IndieGL.bindShader(var8, var1, var2, var9, (var1x, var2x, var3x) -> {
                  var1x.render((IsoObject)null, (float)this.x, (float)this.y, (float)this.z, var2x, WeatherFxMask.offsetX, WeatherFxMask.offsetY, defColorInfo, false, var3x);
               });
            }

            if (var2 == IsoDirections.W || var2 == IsoDirections.NW) {
               var28 = 512;
               var29 = 1084;
               if ((var3 & 2) == 0) {
                  var28 = 896;
                  var29 = 1212;
               } else if ((var5 & 2) != 0) {
                  if ((var4 & 2) == 0 && hasCutawayCapableWallWest(var12)) {
                     var28 = 768;
                     var29 = 1212;
                  } else if ((var6 & 1) == 0 && hasCutawayCapableWallNorth(var14)) {
                     var28 = 768;
                     var29 = 1212;
                  }
               } else if ((var3 & 2) == 0) {
                  var28 = 896;
                  var29 = 1212;
               } else if ((var4 & 2) == 0 && hasCutawayCapableWallWest(var12)) {
                  var28 = 768;
               } else if (hasCutawayCapableWallWest(var13)) {
                  var28 = 256;
               }

               colu = this.getVertLight(0, var10);
               coll = this.getVertLight(3, var10);
               colu2 = this.getVertLight(4, var10);
               coll2 = this.getVertLight(7, var10);
               if (var1.getProperties().Is(IsoFlagType.WallSE)) {
                  SpriteRenderer.instance.setCutawayTexture(var11, var29 + (int)var18, 0 + (int)var19, 6 - var20, 196 - var21);
               } else {
                  SpriteRenderer.instance.setCutawayTexture(var11, var28 + (int)var18, 0 + (int)var19, 66 - var20, 226 - var21);
               }

               Texture var24;
               if (var2 == IsoDirections.W) {
                  var24 = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.WWall);
                  SpriteRenderer.instance.setCutawayTexture2(var24, 0, 0, 0, 0);
               }

               if (var2 == IsoDirections.NW) {
                  var24 = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.NWWall);
                  SpriteRenderer.instance.setCutawayTexture2(var24, 0, 0, 0, 0);
               }

               if (var1.getProperties().Is(IsoFlagType.WallSE)) {
                  var24 = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.SEWall);
                  SpriteRenderer.instance.setCutawayTexture2(var24, 0, 0, 0, 0);
               }

               if (var2 == IsoDirections.W) {
                  SpriteRenderer.instance.setExtraWallShaderParams(SpriteRenderer.WallShaderTexRender.All);
               } else {
                  SpriteRenderer.instance.setExtraWallShaderParams(SpriteRenderer.WallShaderTexRender.LeftOnly);
               }

               var9.col[0] = coll2;
               var9.col[1] = colu2;
               var9.col[2] = colu;
               var9.col[3] = coll;
               IndieGL.bindShader(var8, var1, var2, var9, (var1x, var2x, var3x) -> {
                  var1x.render((IsoObject)null, (float)this.x, (float)this.y, (float)this.z, var2x, WeatherFxMask.offsetX, WeatherFxMask.offsetY, defColorInfo, false, var3x);
               });
            }

            if (var2 == IsoDirections.SE) {
               var28 = 1084;
               if ((var6 & 1) == 0 && hasCutawayCapableWallNorth(var14)) {
                  var28 = 1212;
               } else if ((var4 & 2) == 0 && hasCutawayCapableWallWest(var12)) {
                  var28 = 1212;
               }

               SpriteRenderer.instance.setCutawayTexture(var11, var28 + (int)var18, 0 + (int)var19, 6 - var20, 196 - var21);
               Texture var30 = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.SEWall);
               SpriteRenderer.instance.setCutawayTexture2(var30, 0, 0, 0, 0);
               SpriteRenderer.instance.setExtraWallShaderParams(SpriteRenderer.WallShaderTexRender.All);
               colu = this.getVertLight(0, var10);
               coll = this.getVertLight(3, var10);
               colu2 = this.getVertLight(4, var10);
               coll2 = this.getVertLight(7, var10);
               var9.col[0] = coll2;
               var9.col[1] = colu2;
               var9.col[2] = colu;
               var9.col[3] = coll;
               IndieGL.bindShader(var8, var1, var2, var9, (var1x, var2x, var3x) -> {
                  var1x.render((IsoObject)null, (float)this.x, (float)this.y, (float)this.z, var2x, WeatherFxMask.offsetX, WeatherFxMask.offsetY, defColorInfo, false, var3x);
               });
            }
         } finally {
            SpriteRenderer.instance.setExtraWallShaderParams((SpriteRenderer.WallShaderTexRender)null);
            SpriteRenderer.instance.clearCutawayTexture();
            SpriteRenderer.instance.clearUseVertColorsArray();
         }

      }
   }

   public int DoWallLightingNW(IsoObject var1, int var2, int var3, int var4, int var5, int var6, int var7, boolean var8, boolean var9, boolean var10, boolean var11, Shader var12) {
      if (!DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Walls.NW.getValue()) {
         return var2;
      } else {
         boolean var13 = var3 != 0;
         IsoDirections var14 = IsoDirections.NW;
         int var15 = IsoCamera.frameState.playerIndex;
         colu = this.getVertLight(0, var15);
         coll = this.getVertLight(3, var15);
         colr = this.getVertLight(1, var15);
         colu2 = this.getVertLight(4, var15);
         coll2 = this.getVertLight(7, var15);
         colr2 = this.getVertLight(5, var15);
         if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Walls.LightingDebug.getValue()) {
            colu = -65536;
            coll = -16711936;
            colr = -16711681;
            colu2 = -16776961;
            coll2 = -65281;
            colr2 = -256;
         }

         boolean var16 = CircleStencil;
         if (this.z != PZMath.fastfloor(IsoCamera.getCameraCharacterZ())) {
            var16 = false;
         }

         boolean var17 = var1.sprite.getType() == IsoObjectType.doorFrN || var1.sprite.getType() == IsoObjectType.doorN;
         boolean var18 = var1.sprite.getType() == IsoObjectType.doorFrW || var1.sprite.getType() == IsoObjectType.doorW;
         boolean var19 = false;
         boolean var20 = false;
         boolean var21 = (var17 || var19 || var18 || var20) && var13 || var1.sprite.getProperties().Is(IsoFlagType.NoWallLighting);
         var16 = this.calculateWallAlphaAndCircleStencilCorner(var1, var3, var8, var9, var10, var11, var16, var15, var17, var18, var19, var20);
         if (USE_WALL_SHADER && var16 && var13) {
            this.DoCutawayShader(var1, var14, var3, var4, var5, var6, var7, var8, var9, var10, var11, WallShaperWhole.instance);
            bWallCutawayN = true;
            bWallCutawayW = true;
            return var2;
         } else {
            WallShaperWhole.instance.col[0] = colu2;
            WallShaperWhole.instance.col[1] = colr2;
            WallShaperWhole.instance.col[2] = colr;
            WallShaperWhole.instance.col[3] = colu;
            WallShaperN var22 = WallShaperN.instance;
            var22.col[0] = colu2;
            var22.col[1] = colr2;
            var22.col[2] = colr;
            var22.col[3] = colu;
            TileSeamModifier.instance.setVertColors(colu2, colr2, colr, colu);
            var2 = this.performDrawWall(var1, var14, var2, var15, var21, var22, var12);
            WallShaperWhole.instance.col[0] = coll2;
            WallShaperWhole.instance.col[1] = colu2;
            WallShaperWhole.instance.col[2] = colu;
            WallShaperWhole.instance.col[3] = coll;
            WallShaperW var23 = WallShaperW.instance;
            var23.col[0] = coll2;
            var23.col[1] = colu2;
            var23.col[2] = colu;
            var23.col[3] = coll;
            TileSeamModifier.instance.setVertColors(coll2, colu2, colu, coll);
            var2 = this.performDrawWall(var1, var14, var2, var15, var21, var23, var12);
            return var2;
         }
      }
   }

   public int DoWallLightingN(IsoObject var1, int var2, int var3, int var4, int var5, int var6, int var7, boolean var8, boolean var9, Shader var10) {
      if (!DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Walls.N.getValue()) {
         return var2;
      } else {
         boolean var11 = !var8;
         boolean var12 = !var9;
         IsoObjectType var13 = IsoObjectType.doorFrN;
         IsoObjectType var14 = IsoObjectType.doorN;
         boolean var15 = (var3 & 1) != 0;
         IsoFlagType var16 = IsoFlagType.transparentN;
         IsoFlagType var17 = IsoFlagType.WindowN;
         IsoFlagType var18 = IsoFlagType.HoppableN;
         IsoDirections var19 = IsoDirections.N;
         boolean var20 = CircleStencil;
         int var21 = IsoCamera.frameState.playerIndex;
         colu = this.getVertLight(0, var21);
         coll = this.getVertLight(1, var21);
         colu2 = this.getVertLight(4, var21);
         coll2 = this.getVertLight(5, var21);
         float var22 = Color.getRedChannelFromABGR(colu2);
         float var23 = Color.getGreenChannelFromABGR(colu2);
         float var24 = Color.getBlueChannelFromABGR(colu2);
         float var25 = Color.getRedChannelFromABGR(colu);
         float var26 = Color.getGreenChannelFromABGR(colu);
         float var27 = Color.getBlueChannelFromABGR(colu);
         float var28 = 0.045F;
         float var29 = PZMath.clamp(var25 * (var25 >= var22 ? 1.0F + var28 : 1.0F - var28), 0.0F, 1.0F);
         float var30 = PZMath.clamp(var26 * (var26 >= var23 ? 1.0F + var28 : 1.0F - var28), 0.0F, 1.0F);
         float var31 = PZMath.clamp(var27 * (var27 >= var24 ? 1.0F + var28 : 1.0F - var28), 0.0F, 1.0F);
         colu = Color.colorToABGR(var29, var30, var31, 1.0F);
         var22 = Color.getRedChannelFromABGR(coll);
         var23 = Color.getGreenChannelFromABGR(coll);
         var24 = Color.getBlueChannelFromABGR(coll);
         var25 = Color.getRedChannelFromABGR(coll2);
         var26 = Color.getGreenChannelFromABGR(coll2);
         var27 = Color.getBlueChannelFromABGR(coll2);
         var29 = PZMath.clamp(var25 * (var25 >= var22 ? 1.0F + var28 : 1.0F - var28), 0.0F, 1.0F);
         var30 = PZMath.clamp(var26 * (var26 >= var23 ? 1.0F + var28 : 1.0F - var28), 0.0F, 1.0F);
         var31 = PZMath.clamp(var27 * (var27 >= var24 ? 1.0F + var28 : 1.0F - var28), 0.0F, 1.0F);
         coll2 = Color.colorToABGR(var29, var30, var31, 1.0F);
         if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Walls.LightingDebug.getValue()) {
            colu = -65536;
            coll = -16711936;
            colu2 = -16776961;
            coll2 = -65281;
         }

         WallShaperWhole var32 = WallShaperWhole.instance;
         var32.col[0] = colu2;
         var32.col[1] = coll2;
         var32.col[2] = coll;
         var32.col[3] = colu;
         TileSeamModifier.instance.setVertColors(coll2, colu2, colu, coll);
         return this.performDrawWallSegmentSingle(var1, var2, var3, var4, var5, var6, var7, false, false, var8, var9, var11, var12, var13, var14, var15, var16, var17, var18, var19, var20, var32, var10);
      }
   }

   public int DoWallLightingW(IsoObject var1, int var2, int var3, int var4, int var5, int var6, int var7, boolean var8, boolean var9, Shader var10) {
      if (!DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Walls.W.getValue()) {
         return var2;
      } else {
         boolean var11 = !var8;
         boolean var12 = !var9;
         IsoObjectType var13 = IsoObjectType.doorFrW;
         IsoObjectType var14 = IsoObjectType.doorW;
         boolean var15 = var1.getSprite() != null && var1.getProperties().Is(IsoFlagType.WallSE);
         boolean var16 = false;
         if (var15) {
            if ((var6 & 1) != 0) {
               var16 = true;
            }

            if ((var4 & 2) != 0) {
               var16 = true;
            }
         }

         boolean var17 = (var3 & 2) != 0 || var16;
         IsoFlagType var18 = IsoFlagType.transparentW;
         IsoFlagType var19 = IsoFlagType.WindowW;
         IsoFlagType var20 = IsoFlagType.HoppableW;
         IsoDirections var21 = var15 ? IsoDirections.SE : IsoDirections.W;
         boolean var22 = CircleStencil;
         int var23 = IsoCamera.frameState.playerIndex;
         colu = this.getVertLight(0, var23);
         coll = this.getVertLight(3, var23);
         colu2 = this.getVertLight(4, var23);
         coll2 = this.getVertLight(7, var23);
         float var24 = Color.getRedChannelFromABGR(colu2);
         float var25 = Color.getGreenChannelFromABGR(colu2);
         float var26 = Color.getBlueChannelFromABGR(colu2);
         float var27 = Color.getRedChannelFromABGR(colu);
         float var28 = Color.getGreenChannelFromABGR(colu);
         float var29 = Color.getBlueChannelFromABGR(colu);
         float var30 = 0.045F;
         float var31 = PZMath.clamp(var27 * (var27 >= var24 ? 1.0F + var30 : 1.0F - var30), 0.0F, 1.0F);
         float var32 = PZMath.clamp(var28 * (var28 >= var25 ? 1.0F + var30 : 1.0F - var30), 0.0F, 1.0F);
         float var33 = PZMath.clamp(var29 * (var29 >= var26 ? 1.0F + var30 : 1.0F - var30), 0.0F, 1.0F);
         colu = Color.colorToABGR(var31, var32, var33, 1.0F);
         var24 = Color.getRedChannelFromABGR(coll);
         var25 = Color.getGreenChannelFromABGR(coll);
         var26 = Color.getBlueChannelFromABGR(coll);
         var27 = Color.getRedChannelFromABGR(coll2);
         var28 = Color.getGreenChannelFromABGR(coll2);
         var29 = Color.getBlueChannelFromABGR(coll2);
         var31 = PZMath.clamp(var27 * (var27 >= var24 ? 1.0F + var30 : 1.0F - var30), 0.0F, 1.0F);
         var32 = PZMath.clamp(var28 * (var28 >= var25 ? 1.0F + var30 : 1.0F - var30), 0.0F, 1.0F);
         var33 = PZMath.clamp(var29 * (var29 >= var26 ? 1.0F + var30 : 1.0F - var30), 0.0F, 1.0F);
         coll2 = Color.colorToABGR(var31, var32, var33, 1.0F);
         if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Walls.LightingDebug.getValue()) {
            colu = -65536;
            coll = -16711936;
            colu2 = -16776961;
            coll2 = -65281;
         }

         WallShaperWhole var34 = WallShaperWhole.instance;
         var34.col[0] = coll2;
         var34.col[1] = colu2;
         var34.col[2] = colu;
         var34.col[3] = coll;
         TileSeamModifier.instance.setVertColors(coll2, colu2, colu, coll);
         return this.performDrawWallSegmentSingle(var1, var2, var3, var4, var5, var6, var7, var8, var9, false, false, var11, var12, var13, var14, var17, var18, var19, var20, var21, var22, var34, var10);
      }
   }

   private int performDrawWallSegmentSingle(IsoObject var1, int var2, int var3, int var4, int var5, int var6, int var7, boolean var8, boolean var9, boolean var10, boolean var11, boolean var12, boolean var13, IsoObjectType var14, IsoObjectType var15, boolean var16, IsoFlagType var17, IsoFlagType var18, IsoFlagType var19, IsoDirections var20, boolean var21, WallShaperWhole var22, Shader var23) {
      int var24 = IsoCamera.frameState.playerIndex;
      if (this.z != PZMath.fastfloor(IsoCamera.getCameraCharacterZ())) {
         var21 = false;
      }

      boolean var25 = var1.sprite.getType() == var14 || var1.sprite.getType() == var15;
      boolean var26 = var1 instanceof IsoWindow;
      boolean var27 = (var25 || var26) && var16 || var1.sprite.getProperties().Is(IsoFlagType.NoWallLighting);
      var21 = this.calculateWallAlphaAndCircleStencilEdge(var1, var12, var13, var16, var17, var18, var19, var21, var24, var25, var26);
      if (USE_WALL_SHADER && var21 && var16) {
         this.DoCutawayShader(var1, var20, var3, var4, var5, var6, var7, var10, var8, var11, var9, var22);
         bWallCutawayN |= var20 == IsoDirections.N;
         bWallCutawayW |= var20 == IsoDirections.W;
         return var2;
      } else {
         return this.performDrawWall(var1, var20, var2, var24, var27, var22, var23);
      }
   }

   private int performDrawWallOnly(IsoObject var1, IsoDirections var2, int var3, int var4, boolean var5, Consumer<TextureDraw> var6, Shader var7) {
      IndieGL.enableAlphaTest();
      if (!var5) {
      }

      if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Walls.Render.getValue()) {
         var1.renderWallTile(var2, (float)this.x, (float)this.y, (float)this.z, var5 ? lightInfoTemp : defColorInfo, true, !var5, var7, var6);
      }

      if (PerformanceSettings.FBORenderChunk && var1 instanceof IsoWindow) {
         if (!var1.bAlphaForced && var1.isUpdateAlphaDuringRender()) {
            var1.updateAlpha(var4);
         }

         return var3;
      } else {
         var1.setAlpha(var4, 1.0F);
         return var5 ? var3 : var3 + 1;
      }
   }

   private int performDrawWall(IsoObject var1, IsoDirections var2, int var3, int var4, boolean var5, Consumer<TextureDraw> var6, Shader var7) {
      lightInfoTemp.set(this.lightInfo[var4]);
      if (Core.bDebug && DebugOptions.instance.DebugDraw_SkipWorldShading.getValue()) {
         var1.render((float)this.x, (float)this.y, (float)this.z, defColorInfo, true, !var5, (Shader)null);
         return var3;
      } else {
         int var8 = this.performDrawWallOnly(var1, var2, var3, var4, var5, var6, var7);
         if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Walls.AttachedSprites.getValue()) {
            this.renderAttachedSpritesWithNoWallLighting(var1, lightInfoTemp, (Consumer)null);
         }

         return var8;
      }
   }

   private void calculateWallAlphaCommon(IsoObject var1, boolean var2, boolean var3, boolean var4, int var5, boolean var6, boolean var7) {
      if (var6 || var7) {
         if (!var7 || !PerformanceSettings.FBORenderChunk) {
            if (var2) {
               var1.setAlpha(var5, 0.4F);
               var1.setTargetAlpha(var5, 0.4F);
               lightInfoTemp.r = Math.max(0.3F, lightInfoTemp.r);
               lightInfoTemp.g = Math.max(0.3F, lightInfoTemp.g);
               lightInfoTemp.b = Math.max(0.3F, lightInfoTemp.b);
               if (var6 && !var3) {
                  var1.setAlpha(var5, 0.0F);
                  var1.setTargetAlpha(var5, 0.0F);
               }

               if (var7 && !var4) {
                  var1.setAlpha(var5, 0.0F);
                  var1.setTargetAlpha(var5, 0.0F);
               }
            }

         }
      }
   }

   private boolean calculateWallAlphaAndCircleStencilEdge(IsoObject var1, boolean var2, boolean var3, boolean var4, IsoFlagType var5, IsoFlagType var6, IsoFlagType var7, boolean var8, int var9, boolean var10, boolean var11) {
      if (!FBORenderCell.OUTLINE_DOUBLEDOOR_FRAMES && (var1.sprite.getProperties().Is(IsoFlagType.DoubleDoor1) || var1.sprite.getProperties().Is(IsoFlagType.DoubleDoor2))) {
         return false;
      } else {
         if (var10 || var11) {
            if (!var1.sprite.getProperties().Is("GarageDoor")) {
               var8 = false;
            }

            this.calculateWallAlphaCommon(var1, var4, !var2, !var3, var9, var10, var11);
         }

         if (var8 && (var1.sprite.getType() != IsoObjectType.wall || !var1.getProperties().Is(var5) || !"walls_burnt_01".equals(var1.sprite.tilesetName)) && var1.sprite.getType() == IsoObjectType.wall && var1.sprite.getProperties().Is(var5) && !var1.getSprite().getProperties().Is(IsoFlagType.exterior) && !var1.sprite.getProperties().Is(var6)) {
            var8 = false;
         }

         return var8;
      }
   }

   private boolean calculateWallAlphaAndCircleStencilCorner(IsoObject var1, int var2, boolean var3, boolean var4, boolean var5, boolean var6, boolean var7, int var8, boolean var9, boolean var10, boolean var11, boolean var12) {
      this.calculateWallAlphaCommon(var1, (var2 & 1) != 0, var3, var5, var8, var9, var11);
      this.calculateWallAlphaCommon(var1, (var2 & 2) != 0, var4, var6, var8, var10, var12);
      var7 = var7 && !var9 && !var11;
      if (var7 && var1.sprite.getType() == IsoObjectType.wall && (var1.sprite.getProperties().Is(IsoFlagType.transparentN) || var1.sprite.getProperties().Is(IsoFlagType.transparentW)) && !var1.getSprite().getProperties().Is(IsoFlagType.exterior) && !var1.sprite.getProperties().Is(IsoFlagType.WindowN) && !var1.sprite.getProperties().Is(IsoFlagType.WindowW)) {
         var7 = false;
      }

      return var7;
   }

   public KahluaTable getLuaMovingObjectList() {
      KahluaTable var1 = LuaManager.platform.newTable();
      LuaManager.env.rawset("Objects", var1);

      for(int var2 = 0; var2 < this.MovingObjects.size(); ++var2) {
         var1.rawset(var2 + 1, this.MovingObjects.get(var2));
      }

      return var1;
   }

   public boolean Is(IsoFlagType var1) {
      return this.Properties.Is(var1);
   }

   public boolean Is(String var1) {
      return this.Properties.Is(var1);
   }

   public boolean Has(IsoObjectType var1) {
      return this.hasTypes.isSet(var1);
   }

   public void DeleteTileObject(IsoObject var1) {
      int var2 = this.Objects.indexOf(var1);
      if (var2 != -1) {
         this.Objects.remove(var2);
         if (var1 instanceof IsoTree) {
            IsoTree var3 = (IsoTree)var1;
            var1.reset();
            synchronized(CellLoader.isoTreeCache) {
               CellLoader.isoTreeCache.add(var3);
            }
         } else if (var1.getObjectName().equals("IsoObject")) {
            var1.reset();
            synchronized(CellLoader.isoObjectCache) {
               CellLoader.isoObjectCache.add(var1);
            }
         }

      }
   }

   public KahluaTable getLuaTileObjectList() {
      KahluaTable var1 = LuaManager.platform.newTable();
      LuaManager.env.rawset("Objects", var1);

      for(int var2 = 0; var2 < this.Objects.size(); ++var2) {
         var1.rawset(var2 + 1, this.Objects.get(var2));
      }

      return var1;
   }

   boolean HasDoor(boolean var1) {
      for(int var2 = 0; var2 < this.SpecialObjects.size(); ++var2) {
         if (this.SpecialObjects.get(var2) instanceof IsoDoor && ((IsoDoor)this.SpecialObjects.get(var2)).north == var1) {
            return true;
         }

         if (this.SpecialObjects.get(var2) instanceof IsoThumpable && ((IsoThumpable)this.SpecialObjects.get(var2)).isDoor && ((IsoThumpable)this.SpecialObjects.get(var2)).north == var1) {
            return true;
         }
      }

      return false;
   }

   public boolean HasStairs() {
      return this.HasStairsNorth() || this.HasStairsWest();
   }

   public boolean HasStairsNorth() {
      return this.Has(IsoObjectType.stairsTN) || this.Has(IsoObjectType.stairsMN) || this.Has(IsoObjectType.stairsBN);
   }

   public boolean HasStairsWest() {
      return this.Has(IsoObjectType.stairsTW) || this.Has(IsoObjectType.stairsMW) || this.Has(IsoObjectType.stairsBW);
   }

   public boolean HasStairsBelow() {
      IsoGridSquare var1 = this.getCell().getGridSquare(this.x, this.y, this.z - 1);
      return var1 != null && var1.HasStairs();
   }

   public boolean HasElevatedFloor() {
      return this.Has(IsoObjectType.stairsTN) || this.Has(IsoObjectType.stairsMN) || this.Has(IsoObjectType.stairsTW) || this.Has(IsoObjectType.stairsMW);
   }

   public boolean isSameStaircase(int var1, int var2, int var3) {
      if (var3 != this.getZ()) {
         return false;
      } else {
         int var4 = this.getX();
         int var5 = this.getY();
         int var6 = var4;
         int var7 = var5;
         if (this.Has(IsoObjectType.stairsTN)) {
            var7 = var5 + 2;
         } else if (this.Has(IsoObjectType.stairsMN)) {
            --var5;
            ++var7;
         } else if (this.Has(IsoObjectType.stairsBN)) {
            var5 -= 2;
         } else if (this.Has(IsoObjectType.stairsTW)) {
            var6 = var4 + 2;
         } else if (this.Has(IsoObjectType.stairsMW)) {
            --var4;
            ++var6;
         } else {
            if (!this.Has(IsoObjectType.stairsBW)) {
               return false;
            }

            var4 -= 2;
         }

         if (var1 >= var4 && var2 >= var5 && var1 <= var6 && var2 <= var7) {
            IsoGridSquare var8 = this.getCell().getGridSquare(var1, var2, var3);
            return var8 != null && var8.HasStairs();
         } else {
            return false;
         }
      }
   }

   public boolean hasRainBlockingTile() {
      return this.Is(IsoFlagType.solidfloor) || this.Is(IsoFlagType.BlockRain);
   }

   public boolean haveRoofFull() {
      if (this.haveRoof) {
         return true;
      } else if (this.chunk == null) {
         return false;
      } else {
         int var1 = 1;
         byte var2 = 8;

         for(IsoGridSquare var3 = this.chunk.getGridSquare(this.x % var2, this.y % var2, var1); var1 <= this.chunk.getMaxLevel(); var3 = this.chunk.getGridSquare(this.x % var2, this.y % var2, var1)) {
            if (var3 != null && var3.haveRoof) {
               return true;
            }

            ++var1;
         }

         return false;
      }
   }

   public boolean HasSlopedRoof() {
      return this.HasSlopedRoofWest() || this.HasSlopedRoofNorth();
   }

   public boolean HasSlopedRoofWest() {
      return this.Has(IsoObjectType.WestRoofB) || this.Has(IsoObjectType.WestRoofM) || this.Has(IsoObjectType.WestRoofT);
   }

   public boolean HasSlopedRoofNorth() {
      return this.Has(IsoObjectType.WestRoofB) || this.Has(IsoObjectType.WestRoofM) || this.Has(IsoObjectType.WestRoofT);
   }

   public boolean HasEave() {
      return this.getProperties().Is(IsoFlagType.isEave);
   }

   public boolean HasTree() {
      return this.hasTree;
   }

   public IsoTree getTree() {
      for(int var1 = 0; var1 < this.Objects.size(); ++var1) {
         IsoTree var2 = (IsoTree)Type.tryCastTo((IsoObject)this.Objects.get(var1), IsoTree.class);
         if (var2 != null) {
            return var2;
         }
      }

      return null;
   }

   public boolean hasBush() {
      return this.getBush() != null;
   }

   public IsoObject getBush() {
      for(int var1 = 0; var1 < this.Objects.size(); ++var1) {
         IsoObject var2 = (IsoObject)this.Objects.get(var1);
         if (var2.isBush()) {
            return var2;
         }
      }

      return null;
   }

   public List<IsoObject> getBushes() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.Objects.size(); ++var2) {
         IsoObject var3 = (IsoObject)this.Objects.get(var2);
         if (var3.isBush()) {
            var1.add(var3);
         }
      }

      return var1;
   }

   public IsoObject getGrass() {
      for(int var1 = 0; var1 < this.Objects.size(); ++var1) {
         String var2 = ((IsoObject)this.Objects.get(var1)).getSprite().getName();
         if (var2 != null && (var2.startsWith("e_newgrass_") || var2.startsWith("blends_grassoverlays_"))) {
            return (IsoObject)this.Objects.get(var1);
         }
      }

      return null;
   }

   public boolean hasGrassLike() {
      return !this.getGrassLike().isEmpty();
   }

   public List<IsoObject> getGrassLike() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.Objects.size(); ++var2) {
         String var3 = ((IsoObject)this.Objects.get(var2)).getSprite().getName();
         if (var3.startsWith("e_newgrass_") || var3.startsWith("blends_grassoverlays_") || var3.startsWith("d_plants_") || var3.startsWith("d_generic_1_") || var3.startsWith("d_floorleaves_")) {
            var1.add((IsoObject)this.Objects.get(var2));
         }
      }

      return var1;
   }

   private void fudgeShadowsToAlpha(IsoObject var1, Color var2) {
      float var3 = 1.0F - var1.getAlpha();
      if (var2.r < var3) {
         var2.r = var3;
      }

      if (var2.g < var3) {
         var2.g = var3;
      }

      if (var2.b < var3) {
         var2.b = var3;
      }

   }

   public boolean shouldSave() {
      return !this.Objects.isEmpty() || this.z == 0;
   }

   public void save(ByteBuffer var1, ObjectOutputStream var2) throws IOException {
      this.save(var1, var2, false);
   }

   public void save(ByteBuffer var1, ObjectOutputStream var2, boolean var3) throws IOException {
      this.getErosionData().save(var1);
      BitHeaderWrite var4 = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, var1);
      int var5 = this.Objects.size();
      int var7;
      int var8;
      int var9;
      if (this.Objects.size() > 0) {
         var4.addFlags(1);
         if (var5 == 2) {
            var4.addFlags(2);
         } else if (var5 == 3) {
            var4.addFlags(4);
         } else if (var5 >= 4) {
            var4.addFlags(8);
         }

         if (var3) {
            GameWindow.WriteString(var1, "Number of objects (" + var5 + ")");
         }

         if (var5 >= 4) {
            var1.putShort((short)this.Objects.size());
         }

         for(int var6 = 0; var6 < this.Objects.size(); ++var6) {
            var7 = var1.position();
            if (var3) {
               var1.putInt(0);
            }

            var8 = 0;
            if (this.SpecialObjects.contains(this.Objects.get(var6))) {
               var8 = (byte)(var8 | 2);
            }

            if (this.WorldObjects.contains(this.Objects.get(var6))) {
               var8 = (byte)(var8 | 4);
            }

            var1.put((byte)var8);
            if (var3) {
               GameWindow.WriteStringUTF(var1, ((IsoObject)this.Objects.get(var6)).getClass().getName());
            }

            ((IsoObject)this.Objects.get(var6)).save(var1, var3);
            if (var3) {
               var9 = var1.position();
               var1.position(var7);
               var1.putInt(var9 - var7);
               var1.position(var9);
            }
         }

         if (var3) {
            var1.put((byte)67);
            var1.put((byte)82);
            var1.put((byte)80);
            var1.put((byte)83);
         }
      }

      if (this.isOverlayDone()) {
         var4.addFlags(16);
      }

      if (this.haveRoof) {
         var4.addFlags(32);
      }

      BitHeaderWrite var10 = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, var1);
      var7 = 0;

      for(var8 = 0; var8 < this.StaticMovingObjects.size(); ++var8) {
         if (this.StaticMovingObjects.get(var8) instanceof IsoDeadBody) {
            ++var7;
         }
      }

      if (var7 > 0) {
         var10.addFlags(1);
         if (var3) {
            GameWindow.WriteString(var1, "Number of bodies");
         }

         var1.putShort((short)var7);

         for(var8 = 0; var8 < this.StaticMovingObjects.size(); ++var8) {
            IsoMovingObject var11 = (IsoMovingObject)this.StaticMovingObjects.get(var8);
            if (var11 instanceof IsoDeadBody) {
               if (var3) {
                  GameWindow.WriteStringUTF(var1, var11.getClass().getName());
               }

               var11.save(var1, var3);
            }
         }
      }

      if (this.table != null && !this.table.isEmpty()) {
         var10.addFlags(2);
         this.table.save(var1);
      }

      if (this.burntOut) {
         var10.addFlags(4);
      }

      if (this.getTrapPositionX() > 0) {
         var10.addFlags(8);
         var1.putInt(this.getTrapPositionX());
         var1.putInt(this.getTrapPositionY());
         var1.putInt(this.getTrapPositionZ());
      }

      if (this.haveSheetRope) {
         var10.addFlags(16);
      }

      if (!var10.equals(0)) {
         var4.addFlags(64);
         var10.write();
      } else {
         var1.position(var10.getStartPosition());
      }

      var8 = 0;
      if (!GameClient.bClient && !GameServer.bServer) {
         for(var9 = 0; var9 < 4; ++var9) {
            if (this.isSeen(var9)) {
               var8 |= 1 << var9;
            }
         }
      }

      var1.put((byte)var8);
      var4.write();
      var4.release();
      var10.release();
   }

   static void loadmatrix(boolean[][][] var0, DataInputStream var1) throws IOException {
   }

   static void savematrix(boolean[][][] var0, DataOutputStream var1) throws IOException {
      for(int var2 = 0; var2 < 3; ++var2) {
         for(int var3 = 0; var3 < 3; ++var3) {
            for(int var4 = 0; var4 < 3; ++var4) {
               var1.writeBoolean(var0[var2][var3][var4]);
            }
         }
      }

   }

   public boolean isCommonGrass() {
      if (this.Objects.isEmpty()) {
         return false;
      } else {
         IsoObject var1 = (IsoObject)this.Objects.get(0);
         return var1.sprite.getProperties().Is(IsoFlagType.solidfloor) && ("TileFloorExt_3".equals(var1.tile) || "TileFloorExt_4".equals(var1.tile));
      }
   }

   public static boolean toBoolean(byte[] var0) {
      return var0 != null && var0.length != 0 ? var0[0] != 0 : false;
   }

   public void removeCorpse(IsoDeadBody var1, boolean var2) {
      if (GameClient.bClient && !var2) {
         try {
            GameClient.instance.checkAddedRemovedItems(var1);
         } catch (Exception var4) {
            GameClient.connection.cancelPacket();
            ExceptionLogger.logException(var4);
         }

         GameClient.sendRemoveCorpseFromMap(var1);
      }

      if (GameServer.bServer && !var2) {
         GameServer.sendRemoveCorpseFromMap(var1);
      }

      var1.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_CORPSE | FBORenderChunk.DIRTY_OBJECT_REMOVE);
      var1.removeFromWorld();
      var1.removeFromSquare();
      if (!GameServer.bServer) {
         LuaEventManager.triggerEvent("OnContainerUpdate", this);
      }

   }

   public IsoDeadBody getDeadBody() {
      for(int var1 = 0; var1 < this.StaticMovingObjects.size(); ++var1) {
         if (this.StaticMovingObjects.get(var1) instanceof IsoDeadBody) {
            return (IsoDeadBody)this.StaticMovingObjects.get(var1);
         }
      }

      return null;
   }

   public List<IsoDeadBody> getDeadBodys() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.StaticMovingObjects.size(); ++var2) {
         if (this.StaticMovingObjects.get(var2) instanceof IsoDeadBody) {
            var1.add((IsoDeadBody)this.StaticMovingObjects.get(var2));
         }
      }

      return var1;
   }

   public void addCorpse(IsoDeadBody var1, boolean var2) {
      if (GameClient.bClient && !var2) {
         AddCorpseToMapPacket var3 = new AddCorpseToMapPacket();
         var3.set(this, var1);
         ByteBufferWriter var4 = GameClient.connection.startPacket();
         PacketTypes.PacketType.AddCorpseToMap.doPacket(var4);
         var3.write(var4);
         PacketTypes.PacketType.AddCorpseToMap.send(GameClient.connection);
      }

      if (!this.StaticMovingObjects.contains(var1)) {
         this.StaticMovingObjects.add(var1);
      }

      var1.addToWorld();
      this.burntOut = false;
      this.Properties.UnSet(IsoFlagType.burntOut);
      var1.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_CORPSE | FBORenderChunk.DIRTY_OBJECT_ADD);
   }

   public IsoBrokenGlass getBrokenGlass() {
      for(int var1 = 0; var1 < this.SpecialObjects.size(); ++var1) {
         IsoObject var2 = (IsoObject)this.SpecialObjects.get(var1);
         if (var2 instanceof IsoBrokenGlass) {
            return (IsoBrokenGlass)var2;
         }
      }

      return null;
   }

   public IsoBrokenGlass addBrokenGlass() {
      if (!this.isFree(false)) {
         return this.getBrokenGlass();
      } else {
         IsoBrokenGlass var1 = this.getBrokenGlass();
         if (var1 == null) {
            var1 = new IsoBrokenGlass(this.getCell());
            var1.setSquare(this);
            this.AddSpecialObject(var1);
            if (GameServer.bServer) {
               GameServer.transmitBrokenGlass(this);
            }
         }

         return var1;
      }
   }

   public IsoFire getFire() {
      for(int var1 = 0; var1 < this.Objects.size(); ++var1) {
         Object var3 = this.Objects.get(var1);
         if (var3 instanceof IsoFire var2) {
            return var2;
         }
      }

      return null;
   }

   public IsoObject getHiddenStash() {
      for(int var1 = 0; var1 < this.SpecialObjects.size(); ++var1) {
         IsoObject var2 = (IsoObject)this.SpecialObjects.get(var1);
         IsoSprite var3 = var2.getSprite();
         if (var3 != null && StringUtils.equalsIgnoreCase("floors_interior_tilesandwood_01_62", var3.getName())) {
            return var2;
         }
      }

      return null;
   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      this.load(var1, var2, false);
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      this.getErosionData().load(var1, var2);
      BitHeaderRead var4 = BitHeader.allocRead(BitHeader.HeaderSize.Byte, var1);
      int var6;
      if (!var4.equals(0)) {
         int var7;
         short var20;
         if (var4.hasFlags(1)) {
            if (var3) {
               String var5 = GameWindow.ReadStringUTF(var1);
               DebugLog.log(var5);
            }

            short var19 = 1;
            if (var4.hasFlags(2)) {
               var19 = 2;
            } else if (var4.hasFlags(4)) {
               var19 = 3;
            } else if (var4.hasFlags(8)) {
               var19 = var1.getShort();
            }

            var6 = 0;

            while(true) {
               byte var9;
               if (var6 >= var19) {
                  if (!var3) {
                     break;
                  }

                  var20 = var1.get();
                  byte var24 = var1.get();
                  byte var25 = var1.get();
                  var9 = var1.get();
                  if (var20 != 67 || var24 != 82 || var25 != 80 || var9 != 83) {
                     DebugLog.log("***** Expected CRPS here");
                  }
                  break;
               }

               var7 = var1.position();
               int var8 = 0;
               if (var3) {
                  var8 = var1.getInt();
               }

               var9 = var1.get();
               boolean var10 = (var9 & 2) != 0;
               boolean var11 = (var9 & 4) != 0;
               IsoObject var12 = null;
               String var13;
               if (var3) {
                  var13 = GameWindow.ReadStringUTF(var1);
                  DebugLog.log(var13);
               }

               var12 = IsoObject.factoryFromFileInput(this.getCell(), var1);
               int var28;
               if (var12 == null) {
                  if (var3) {
                     var28 = var1.position();
                     if (var28 - var7 != var8) {
                        DebugLog.log("***** Object loaded size " + (var28 - var7) + " != saved size " + var8 + ", reading obj size: " + var19 + ", Object == null");
                        if (var12.getSprite() != null && var12.getSprite().getName() != null) {
                           DebugLog.log("Obj sprite = " + var12.getSprite().getName());
                        }
                     }
                  }
               } else {
                  label272: {
                     var12.square = this;

                     try {
                        var12.load(var1, var2, var3);
                     } catch (Exception var18) {
                        this.debugPrintGridSquare();
                        if (lastLoaded != null) {
                           lastLoaded.debugPrintGridSquare();
                        }

                        throw new RuntimeException(var18);
                     }

                     if (var3) {
                        var28 = var1.position();
                        if (var28 - var7 != var8) {
                           DebugLog.log("***** Object loaded size " + (var28 - var7) + " != saved size " + var8 + ", reading obj size: " + var19);
                           if (var12.getSprite() != null && var12.getSprite().getName() != null) {
                              DebugLog.log("Obj sprite = " + var12.getSprite().getName());
                           }
                        }
                     }

                     if (var12 instanceof IsoWorldInventoryObject) {
                        if (((IsoWorldInventoryObject)var12).getItem() == null) {
                           break label272;
                        }

                        var13 = ((IsoWorldInventoryObject)var12).getItem().getFullType();
                        Item var14 = ScriptManager.instance.FindItem(var13);
                        if (var14 != null && var14.getObsolete()) {
                           break label272;
                        }

                        String[] var15 = var13.split("_");
                        if (((IsoWorldInventoryObject)var12).dropTime > -1.0 && SandboxOptions.instance.HoursForWorldItemRemoval.getValue() > 0.0 && (SandboxOptions.instance.WorldItemRemovalList.getSplitCSVList().contains(var15[0]) && !SandboxOptions.instance.ItemRemovalListBlacklistToggle.getValue() || !SandboxOptions.instance.WorldItemRemovalList.getSplitCSVList().contains(var15[0]) && SandboxOptions.instance.ItemRemovalListBlacklistToggle.getValue()) && !((IsoWorldInventoryObject)var12).isIgnoreRemoveSandbox() && GameTime.instance.getWorldAgeHours() > ((IsoWorldInventoryObject)var12).dropTime + SandboxOptions.instance.HoursForWorldItemRemoval.getValue()) {
                           break label272;
                        }
                     }

                     if (!(var12 instanceof IsoWindow) || var12.getSprite() == null || !"walls_special_01_8".equals(var12.getSprite().getName()) && !"walls_special_01_9".equals(var12.getSprite().getName())) {
                        this.Objects.add(var12);
                        if (var10) {
                           this.SpecialObjects.add(var12);
                        }

                        if (var11) {
                           if (Core.bDebug && !(var12 instanceof IsoWorldInventoryObject)) {
                              DebugLog.log("Bitflags = " + var9 + ", obj name = " + var12.getObjectName() + ", sprite = " + (var12.getSprite() != null ? var12.getSprite().getName() : "unknown"));
                           }

                           this.WorldObjects.add((IsoWorldInventoryObject)var12);
                           var12.square.chunk.recalcHashCodeObjects();
                        }
                     }
                  }
               }

               ++var6;
            }
         }

         this.setOverlayDone(var4.hasFlags(16));
         this.haveRoof = var4.hasFlags(32);
         if (var4.hasFlags(64)) {
            BitHeaderRead var21 = BitHeader.allocRead(BitHeader.HeaderSize.Byte, var1);
            if (var21.hasFlags(1)) {
               if (var3) {
                  String var22 = GameWindow.ReadStringUTF(var1);
                  DebugLog.log(var22);
               }

               var20 = var1.getShort();

               for(var7 = 0; var7 < var20; ++var7) {
                  IsoMovingObject var26 = null;
                  if (var3) {
                     String var27 = GameWindow.ReadStringUTF(var1);
                     DebugLog.log(var27);
                  }

                  try {
                     var26 = (IsoMovingObject)IsoObject.factoryFromFileInput(this.getCell(), var1);
                  } catch (Exception var17) {
                     this.debugPrintGridSquare();
                     if (lastLoaded != null) {
                        lastLoaded.debugPrintGridSquare();
                     }

                     throw new RuntimeException(var17);
                  }

                  if (var26 != null) {
                     var26.square = this;
                     var26.current = this;

                     try {
                        var26.load(var1, var2, var3);
                     } catch (Exception var16) {
                        this.debugPrintGridSquare();
                        if (lastLoaded != null) {
                           lastLoaded.debugPrintGridSquare();
                        }

                        throw new RuntimeException(var16);
                     }

                     this.StaticMovingObjects.add(var26);
                     this.recalcHashCodeObjects();
                  }
               }
            }

            if (var21.hasFlags(2)) {
               if (this.table == null) {
                  this.table = LuaManager.platform.newTable();
               }

               this.table.load(var1, var2);
            }

            this.burntOut = var21.hasFlags(4);
            if (var21.hasFlags(8)) {
               this.setTrapPositionX(var1.getInt());
               this.setTrapPositionY(var1.getInt());
               this.setTrapPositionZ(var1.getInt());
            }

            this.haveSheetRope = var21.hasFlags(16);
            var21.release();
         }
      }

      var4.release();
      byte var23 = var1.get();
      if (!GameClient.bClient && !GameServer.bServer) {
         for(var6 = 0; var6 < 4; ++var6) {
            this.setIsSeen(var6, (var23 & 1 << var6) != 0);
         }
      }

      lastLoaded = this;
   }

   private void debugPrintGridSquare() {
      System.out.println("x=" + this.x + " y=" + this.y + " z=" + this.z);
      System.out.println("objects");

      int var1;
      for(var1 = 0; var1 < this.Objects.size(); ++var1) {
         ((IsoObject)this.Objects.get(var1)).debugPrintout();
      }

      System.out.println("staticmovingobjects");

      for(var1 = 0; var1 < this.StaticMovingObjects.size(); ++var1) {
         ((IsoObject)this.Objects.get(var1)).debugPrintout();
      }

   }

   public float scoreAsWaypoint(int var1, int var2) {
      float var3 = 2.0F;
      var3 -= IsoUtils.DistanceManhatten((float)var1, (float)var2, (float)this.getX(), (float)this.getY()) * 5.0F;
      return var3;
   }

   public void InvalidateSpecialObjectPaths() {
   }

   public boolean isSolid() {
      return this.Properties.Is(IsoFlagType.solid);
   }

   public boolean isSolidTrans() {
      return this.Properties.Is(IsoFlagType.solidtrans);
   }

   public boolean isFree(boolean var1) {
      if (var1 && this.MovingObjects.size() > 0) {
         return false;
      } else if (this.CachedIsFree) {
         return this.CacheIsFree;
      } else {
         this.CachedIsFree = true;
         this.CacheIsFree = true;
         if (this.Properties.Is(IsoFlagType.solid) || this.Properties.Is(IsoFlagType.solidtrans) || this.Has(IsoObjectType.tree)) {
            this.CacheIsFree = false;
         }

         if (!this.Properties.Is(IsoFlagType.solidfloor)) {
            this.CacheIsFree = false;
         }

         if (!this.Has(IsoObjectType.stairsBN) && !this.Has(IsoObjectType.stairsMN) && !this.Has(IsoObjectType.stairsTN)) {
            if (this.Has(IsoObjectType.stairsBW) || this.Has(IsoObjectType.stairsMW) || this.Has(IsoObjectType.stairsTW)) {
               this.CacheIsFree = true;
            }
         } else {
            this.CacheIsFree = true;
         }

         return this.CacheIsFree;
      }
   }

   public boolean isFreeOrMidair(boolean var1) {
      if (var1 && this.MovingObjects.size() > 0) {
         return false;
      } else {
         boolean var2 = true;
         if (this.Properties.Is(IsoFlagType.solid) || this.Properties.Is(IsoFlagType.solidtrans) || this.Has(IsoObjectType.tree)) {
            var2 = false;
         }

         if (!this.Has(IsoObjectType.stairsBN) && !this.Has(IsoObjectType.stairsMN) && !this.Has(IsoObjectType.stairsTN)) {
            if (this.Has(IsoObjectType.stairsBW) || this.Has(IsoObjectType.stairsMW) || this.Has(IsoObjectType.stairsTW)) {
               var2 = true;
            }
         } else {
            var2 = true;
         }

         return var2;
      }
   }

   public boolean isFreeOrMidair(boolean var1, boolean var2) {
      if (var1 && this.MovingObjects.size() > 0) {
         if (!var2) {
            return false;
         }

         for(int var3 = 0; var3 < this.MovingObjects.size(); ++var3) {
            IsoMovingObject var4 = (IsoMovingObject)this.MovingObjects.get(var3);
            if (!(var4 instanceof IsoDeadBody)) {
               return false;
            }
         }
      }

      boolean var5 = true;
      if (this.Properties.Is(IsoFlagType.solid) || this.Properties.Is(IsoFlagType.solidtrans) || this.Has(IsoObjectType.tree)) {
         var5 = false;
      }

      if (!this.Has(IsoObjectType.stairsBN) && !this.Has(IsoObjectType.stairsMN) && !this.Has(IsoObjectType.stairsTN)) {
         if (this.Has(IsoObjectType.stairsBW) || this.Has(IsoObjectType.stairsMW) || this.Has(IsoObjectType.stairsTW)) {
            var5 = true;
         }
      } else {
         var5 = true;
      }

      return var5;
   }

   public boolean connectedWithFloor() {
      if (this.getZ() == 0) {
         return true;
      } else {
         IsoGridSquare var1 = null;
         var1 = this.getCell().getGridSquare(this.getX() - 1, this.getY(), this.getZ());
         if (var1 != null && var1.Properties.Is(IsoFlagType.solidfloor)) {
            return true;
         } else {
            var1 = this.getCell().getGridSquare(this.getX() + 1, this.getY(), this.getZ());
            if (var1 != null && var1.Properties.Is(IsoFlagType.solidfloor)) {
               return true;
            } else {
               var1 = this.getCell().getGridSquare(this.getX(), this.getY() - 1, this.getZ());
               if (var1 != null && var1.Properties.Is(IsoFlagType.solidfloor)) {
                  return true;
               } else {
                  var1 = this.getCell().getGridSquare(this.getX(), this.getY() + 1, this.getZ());
                  if (var1 != null && var1.Properties.Is(IsoFlagType.solidfloor)) {
                     return true;
                  } else {
                     var1 = this.getCell().getGridSquare(this.getX(), this.getY() - 1, this.getZ() - 1);
                     if (var1 != null && var1.getSlopedSurfaceHeight(IsoDirections.S) == 1.0F) {
                        return true;
                     } else {
                        var1 = this.getCell().getGridSquare(this.getX(), this.getY() + 1, this.getZ() - 1);
                        if (var1 != null && var1.getSlopedSurfaceHeight(IsoDirections.N) == 1.0F) {
                           return true;
                        } else {
                           var1 = this.getCell().getGridSquare(this.getX() - 1, this.getY(), this.getZ() - 1);
                           if (var1 != null && var1.getSlopedSurfaceHeight(IsoDirections.E) == 1.0F) {
                              return true;
                           } else {
                              var1 = this.getCell().getGridSquare(this.getX() + 1, this.getY(), this.getZ() - 1);
                              return var1 != null && var1.getSlopedSurfaceHeight(IsoDirections.W) == 1.0F;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public boolean hasFloor(boolean var1) {
      if (this.Properties.Is(IsoFlagType.solidfloor)) {
         return true;
      } else {
         IsoGridSquare var2 = null;
         if (var1) {
            var2 = this.getCell().getGridSquare(this.getX(), this.getY() - 1, this.getZ());
         } else {
            var2 = this.getCell().getGridSquare(this.getX() - 1, this.getY(), this.getZ());
         }

         return var2 != null && var2.Properties.Is(IsoFlagType.solidfloor);
      }
   }

   public boolean hasFloor() {
      return this.Properties.Is(IsoFlagType.solidfloor);
   }

   public boolean isNotBlocked(boolean var1) {
      if (!this.CachedIsFree) {
         this.CacheIsFree = true;
         this.CachedIsFree = true;
         if (this.Properties.Is(IsoFlagType.solid) || this.Properties.Is(IsoFlagType.solidtrans)) {
            this.CacheIsFree = false;
         }

         if (!this.Properties.Is(IsoFlagType.solidfloor)) {
            this.CacheIsFree = false;
         }
      } else if (!this.CacheIsFree) {
         return false;
      }

      return !var1 || this.MovingObjects.size() <= 0;
   }

   public IsoObject getDoor(boolean var1) {
      for(int var2 = 0; var2 < this.SpecialObjects.size(); ++var2) {
         IsoObject var3 = (IsoObject)this.SpecialObjects.get(var2);
         if (var3 instanceof IsoThumpable var4) {
            if (var4.isDoor() && var1 == var4.north) {
               return var4;
            }
         }

         if (var3 instanceof IsoDoor var5) {
            if (var1 == var5.north) {
               return var5;
            }
         }
      }

      return null;
   }

   public IsoDoor getIsoDoor() {
      for(int var1 = 0; var1 < this.SpecialObjects.size(); ++var1) {
         IsoObject var2 = (IsoObject)this.SpecialObjects.get(var1);
         if (var2 instanceof IsoDoor) {
            return (IsoDoor)var2;
         }
      }

      return null;
   }

   public IsoObject getDoorTo(IsoGridSquare var1) {
      if (var1 != null && var1 != this) {
         IsoObject var2 = null;
         if (var1.x < this.x) {
            var2 = this.getDoor(false);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.y < this.y) {
            var2 = this.getDoor(true);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.x > this.x) {
            var2 = var1.getDoor(false);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.y > this.y) {
            var2 = var1.getDoor(true);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.x != this.x && var1.y != this.y) {
            IsoGridSquare var3 = this.getCell().getGridSquare(this.x, var1.y, this.z);
            IsoGridSquare var4 = this.getCell().getGridSquare(var1.x, this.y, this.z);
            var2 = this.getDoorTo(var3);
            if (var2 != null) {
               return var2;
            }

            var2 = this.getDoorTo(var4);
            if (var2 != null) {
               return var2;
            }

            var2 = var1.getDoorTo(var3);
            if (var2 != null) {
               return var2;
            }

            var2 = var1.getDoorTo(var4);
            if (var2 != null) {
               return var2;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public IsoWindow getWindow(boolean var1) {
      for(int var2 = 0; var2 < this.SpecialObjects.size(); ++var2) {
         IsoObject var3 = (IsoObject)this.SpecialObjects.get(var2);
         if (var3 instanceof IsoWindow var4) {
            if (var1 == var4.north) {
               return var4;
            }
         }
      }

      return null;
   }

   public IsoWindow getWindow() {
      for(int var1 = 0; var1 < this.SpecialObjects.size(); ++var1) {
         IsoObject var2 = (IsoObject)this.SpecialObjects.get(var1);
         if (var2 instanceof IsoWindow) {
            return (IsoWindow)var2;
         }
      }

      return null;
   }

   public IsoWindow getWindowTo(IsoGridSquare var1) {
      if (var1 != null && var1 != this) {
         IsoWindow var2 = null;
         if (var1.x < this.x) {
            var2 = this.getWindow(false);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.y < this.y) {
            var2 = this.getWindow(true);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.x > this.x) {
            var2 = var1.getWindow(false);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.y > this.y) {
            var2 = var1.getWindow(true);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.x != this.x && var1.y != this.y) {
            IsoGridSquare var3 = this.getCell().getGridSquare(this.x, var1.y, this.z);
            IsoGridSquare var4 = this.getCell().getGridSquare(var1.x, this.y, this.z);
            var2 = this.getWindowTo(var3);
            if (var2 != null) {
               return var2;
            }

            var2 = this.getWindowTo(var4);
            if (var2 != null) {
               return var2;
            }

            var2 = var1.getWindowTo(var3);
            if (var2 != null) {
               return var2;
            }

            var2 = var1.getWindowTo(var4);
            if (var2 != null) {
               return var2;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public boolean isAdjacentToWindow() {
      if (this.getWindow() != null) {
         return true;
      } else if (this.hasWindowFrame()) {
         return true;
      } else if (this.getThumpableWindow(false) == null && this.getThumpableWindow(true) == null) {
         IsoGridSquare var1 = this.nav[IsoDirections.S.index()];
         if (var1 != null && (var1.getWindow(true) != null || var1.getWindowFrame(true) != null || var1.getThumpableWindow(true) != null)) {
            return true;
         } else {
            IsoGridSquare var2 = this.nav[IsoDirections.E.index()];
            return var2 != null && (var2.getWindow(false) != null || var2.getWindowFrame(false) != null || var2.getThumpableWindow(false) != null);
         }
      } else {
         return true;
      }
   }

   public boolean isAdjacentToHoppable() {
      if (this.getHoppable(true) == null && this.getHoppable(false) == null) {
         if (this.getHoppableThumpable(true) == null && this.getHoppableThumpable(false) == null) {
            IsoGridSquare var1 = this.nav[IsoDirections.S.index()];
            if (var1 != null && (var1.getHoppable(true) != null || var1.getHoppable(false) != null || var1.getHoppableThumpable(true) != null || var1.getHoppableThumpable(false) != null)) {
               return true;
            } else {
               IsoGridSquare var2 = this.nav[IsoDirections.E.index()];
               return var2 != null && (var2.getHoppable(true) != null || var2.getHoppable(false) != null || var2.getHoppableThumpable(true) != null || var2.getHoppableThumpable(false) != null);
            }
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   public IsoThumpable getThumpableWindow(boolean var1) {
      for(int var2 = 0; var2 < this.SpecialObjects.size(); ++var2) {
         IsoObject var3 = (IsoObject)this.SpecialObjects.get(var2);
         if (var3 instanceof IsoThumpable var4) {
            if (var4.isWindow() && var1 == var4.north) {
               return var4;
            }
         }
      }

      return null;
   }

   public IsoThumpable getWindowThumpableTo(IsoGridSquare var1) {
      if (var1 != null && var1 != this) {
         IsoThumpable var2 = null;
         if (var1.x < this.x) {
            var2 = this.getThumpableWindow(false);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.y < this.y) {
            var2 = this.getThumpableWindow(true);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.x > this.x) {
            var2 = var1.getThumpableWindow(false);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.y > this.y) {
            var2 = var1.getThumpableWindow(true);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.x != this.x && var1.y != this.y) {
            IsoGridSquare var3 = this.getCell().getGridSquare(this.x, var1.y, this.z);
            IsoGridSquare var4 = this.getCell().getGridSquare(var1.x, this.y, this.z);
            var2 = this.getWindowThumpableTo(var3);
            if (var2 != null) {
               return var2;
            }

            var2 = this.getWindowThumpableTo(var4);
            if (var2 != null) {
               return var2;
            }

            var2 = var1.getWindowThumpableTo(var3);
            if (var2 != null) {
               return var2;
            }

            var2 = var1.getWindowThumpableTo(var4);
            if (var2 != null) {
               return var2;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public IsoThumpable getHoppableThumpable(boolean var1) {
      for(int var2 = 0; var2 < this.SpecialObjects.size(); ++var2) {
         IsoObject var3 = (IsoObject)this.SpecialObjects.get(var2);
         if (var3 instanceof IsoThumpable var4) {
            if (var4.isHoppable() && var1 == var4.north) {
               return var4;
            }
         }
      }

      return null;
   }

   public IsoThumpable getHoppableThumpableTo(IsoGridSquare var1) {
      if (var1 != null && var1 != this) {
         IsoThumpable var2 = null;
         if (var1.x < this.x) {
            var2 = this.getHoppableThumpable(false);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.y < this.y) {
            var2 = this.getHoppableThumpable(true);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.x > this.x) {
            var2 = var1.getHoppableThumpable(false);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.y > this.y) {
            var2 = var1.getHoppableThumpable(true);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.x != this.x && var1.y != this.y) {
            IsoGridSquare var3 = this.getCell().getGridSquare(this.x, var1.y, this.z);
            IsoGridSquare var4 = this.getCell().getGridSquare(var1.x, this.y, this.z);
            var2 = this.getHoppableThumpableTo(var3);
            if (var2 != null) {
               return var2;
            }

            var2 = this.getHoppableThumpableTo(var4);
            if (var2 != null) {
               return var2;
            }

            var2 = var1.getHoppableThumpableTo(var3);
            if (var2 != null) {
               return var2;
            }

            var2 = var1.getHoppableThumpableTo(var4);
            if (var2 != null) {
               return var2;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public IsoObject getWallHoppable(boolean var1) {
      for(int var2 = 0; var2 < this.Objects.size(); ++var2) {
         if (((IsoObject)this.Objects.get(var2)).isHoppable() && var1 == ((IsoObject)this.Objects.get(var2)).isNorthHoppable()) {
            return (IsoObject)this.Objects.get(var2);
         }
      }

      return null;
   }

   public IsoObject getWallHoppableTo(IsoGridSquare var1) {
      if (var1 != null && var1 != this) {
         IsoObject var2 = null;
         if (var1.x < this.x) {
            var2 = this.getWallHoppable(false);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.y < this.y) {
            var2 = this.getWallHoppable(true);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.x > this.x) {
            var2 = var1.getWallHoppable(false);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.y > this.y) {
            var2 = var1.getWallHoppable(true);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.x != this.x && var1.y != this.y) {
            IsoGridSquare var3 = this.getCell().getGridSquare(this.x, var1.y, this.z);
            IsoGridSquare var4 = this.getCell().getGridSquare(var1.x, this.y, this.z);
            var2 = this.getWallHoppableTo(var3);
            if (var2 != null) {
               return var2;
            }

            var2 = this.getWallHoppableTo(var4);
            if (var2 != null) {
               return var2;
            }

            var2 = var1.getWallHoppableTo(var3);
            if (var2 != null) {
               return var2;
            }

            var2 = var1.getWallHoppableTo(var4);
            if (var2 != null) {
               return var2;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public IsoObject getBedTo(IsoGridSquare var1) {
      ArrayList var2 = null;
      if (var1.y >= this.y && var1.x >= this.x) {
         var2 = var1.SpecialObjects;
      } else {
         var2 = this.SpecialObjects;
      }

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         IsoObject var4 = (IsoObject)var2.get(var3);
         if (var4.getProperties().Is(IsoFlagType.bed)) {
            return var4;
         }
      }

      return null;
   }

   public IsoWindowFrame getWindowFrame(boolean var1) {
      for(int var2 = 0; var2 < this.Objects.size(); ++var2) {
         IsoObject var3 = (IsoObject)this.Objects.get(var2);
         if (var3 instanceof IsoWindowFrame var4) {
            if (var4.getNorth() == var1) {
               return var4;
            }
         }
      }

      return null;
   }

   public IsoWindowFrame getWindowFrameTo(IsoGridSquare var1) {
      if (var1 != null && var1 != this) {
         IsoWindowFrame var2 = null;
         if (var1.x < this.x) {
            var2 = this.getWindowFrame(false);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.y < this.y) {
            var2 = this.getWindowFrame(true);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.x > this.x) {
            var2 = var1.getWindowFrame(false);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.y > this.y) {
            var2 = var1.getWindowFrame(true);
            if (var2 != null) {
               return var2;
            }
         }

         if (var1.x != this.x && var1.y != this.y) {
            IsoGridSquare var3 = this.getCell().getGridSquare(this.x, var1.y, this.z);
            IsoGridSquare var4 = this.getCell().getGridSquare(var1.x, this.y, this.z);
            var2 = this.getWindowFrameTo(var3);
            if (var2 != null) {
               return var2;
            }

            var2 = this.getWindowFrameTo(var4);
            if (var2 != null) {
               return var2;
            }

            var2 = var1.getWindowFrameTo(var3);
            if (var2 != null) {
               return var2;
            }

            var2 = var1.getWindowFrameTo(var4);
            if (var2 != null) {
               return var2;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public boolean hasWindowFrame() {
      for(int var1 = 0; var1 < this.Objects.size(); ++var1) {
         IsoObject var2 = (IsoObject)this.Objects.get(var1);
         if (var2 instanceof IsoWindowFrame) {
            return true;
         }
      }

      return false;
   }

   public boolean hasWindowOrWindowFrame() {
      for(int var1 = 0; var1 < this.Objects.size(); ++var1) {
         IsoObject var2 = (IsoObject)this.Objects.get(var1);
         if (!(var2 instanceof IsoWorldInventoryObject) && (this.isWindowOrWindowFrame(var2, true) || this.isWindowOrWindowFrame(var2, false))) {
            return true;
         }
      }

      return false;
   }

   private IsoObject getSpecialWall(boolean var1) {
      for(int var2 = this.SpecialObjects.size() - 1; var2 >= 0; --var2) {
         IsoObject var3 = (IsoObject)this.SpecialObjects.get(var2);
         if (var3 instanceof IsoThumpable var4) {
            if (var4.isStairs() || !var4.isThumpable() && !var4.isWindow() && !var4.isDoor() || var4.isDoor() && var4.open || var4.isBlockAllTheSquare()) {
               continue;
            }

            if (var1 == var4.north && !var4.isCorner()) {
               return var4;
            }
         }

         if (var3 instanceof IsoWindow var6) {
            if (var1 == var6.north) {
               return var6;
            }
         }

         if (var3 instanceof IsoDoor var7) {
            if (var1 == var7.north && !var7.open) {
               return var7;
            }
         }
      }

      if (var1 && !this.Is(IsoFlagType.WindowN) || !var1 && !this.Is(IsoFlagType.WindowW)) {
         return null;
      } else {
         IsoWindowFrame var5 = this.getWindowFrame(var1);
         if (var5 != null) {
            return var5;
         } else {
            return null;
         }
      }
   }

   public IsoObject getSheetRope() {
      for(int var1 = 0; var1 < this.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)this.getObjects().get(var1);
         if (var2.sheetRope) {
            return var2;
         }
      }

      return null;
   }

   public boolean damageSpriteSheetRopeFromBottom(IsoPlayer var1, boolean var2) {
      IsoGridSquare var4 = this;
      IsoFlagType var3;
      if (var2) {
         if (this.Is(IsoFlagType.climbSheetN)) {
            var3 = IsoFlagType.climbSheetN;
         } else {
            if (!this.Is(IsoFlagType.climbSheetS)) {
               return false;
            }

            var3 = IsoFlagType.climbSheetS;
         }
      } else if (this.Is(IsoFlagType.climbSheetW)) {
         var3 = IsoFlagType.climbSheetW;
      } else {
         if (!this.Is(IsoFlagType.climbSheetE)) {
            return false;
         }

         var3 = IsoFlagType.climbSheetE;
      }

      while(var4 != null) {
         for(int var5 = 0; var5 < var4.getObjects().size(); ++var5) {
            IsoObject var6 = (IsoObject)var4.getObjects().get(var5);
            if (var6.getProperties() != null && var6.getProperties().Is(var3)) {
               int var7 = Integer.parseInt(var6.getSprite().getName().split("_")[2]);
               if (var7 > 14) {
                  return false;
               }

               String var10000 = var6.getSprite().getName().split("_")[0];
               String var8 = var10000 + "_" + var6.getSprite().getName().split("_")[1];
               var7 += 40;
               var6.setSprite(IsoSpriteManager.instance.getSprite(var8 + "_" + var7));
               var6.transmitUpdatedSprite();
               break;
            }
         }

         if (var4.getZ() == 7) {
            break;
         }

         var4 = var4.getCell().getGridSquare(var4.getX(), var4.getY(), var4.getZ() + 1);
      }

      return true;
   }

   public boolean removeSheetRopeFromBottom(IsoPlayer var1, boolean var2) {
      IsoGridSquare var6 = this;
      IsoFlagType var3;
      IsoFlagType var4;
      String var5;
      int var7;
      IsoObject var8;
      if (var2) {
         if (this.Is(IsoFlagType.climbSheetN)) {
            var3 = IsoFlagType.climbSheetTopN;
            var4 = IsoFlagType.climbSheetN;
         } else {
            if (!this.Is(IsoFlagType.climbSheetS)) {
               return false;
            }

            var3 = IsoFlagType.climbSheetTopS;
            var4 = IsoFlagType.climbSheetS;
            var5 = "crafted_01_4";

            for(var7 = 0; var7 < var6.getObjects().size(); ++var7) {
               var8 = (IsoObject)var6.getObjects().get(var7);
               if (var8.sprite != null && var8.sprite.getName() != null && var8.sprite.getName().equals(var5)) {
                  var6.transmitRemoveItemFromSquare(var8);
                  break;
               }
            }
         }
      } else if (this.Is(IsoFlagType.climbSheetW)) {
         var3 = IsoFlagType.climbSheetTopW;
         var4 = IsoFlagType.climbSheetW;
      } else {
         if (!this.Is(IsoFlagType.climbSheetE)) {
            return false;
         }

         var3 = IsoFlagType.climbSheetTopE;
         var4 = IsoFlagType.climbSheetE;
         var5 = "crafted_01_3";

         for(var7 = 0; var7 < var6.getObjects().size(); ++var7) {
            var8 = (IsoObject)var6.getObjects().get(var7);
            if (var8.sprite != null && var8.sprite.getName() != null && var8.sprite.getName().equals(var5)) {
               var6.transmitRemoveItemFromSquare(var8);
               break;
            }
         }
      }

      boolean var12 = false;

      IsoGridSquare var13;
      for(var13 = null; var6 != null; var12 = false) {
         for(int var9 = 0; var9 < var6.getObjects().size(); ++var9) {
            IsoObject var10 = (IsoObject)var6.getObjects().get(var9);
            if (var10.getProperties() != null && (var10.getProperties().Is(var3) || var10.getProperties().Is(var4))) {
               var13 = var6;
               var12 = true;
               var6.transmitRemoveItemFromSquare(var10);
               if (GameServer.bServer) {
                  if (var1 != null) {
                     var1.sendObjectChange("addItemOfType", new Object[]{"type", var10.getName()});
                  }
               } else if (var1 != null) {
                  var1.getInventory().AddItem(var10.getName());
               }
               break;
            }
         }

         if (var6.getZ() == 7) {
            break;
         }

         var6 = var6.getCell().getGridSquare(var6.getX(), var6.getY(), var6.getZ() + 1);
      }

      if (!var12) {
         var6 = var13.getCell().getGridSquare(var13.getX(), var13.getY(), var13.getZ());
         IsoGridSquare var14 = var2 ? var6.nav[IsoDirections.S.index()] : var6.nav[IsoDirections.E.index()];
         if (var14 == null) {
            return true;
         }

         for(int var15 = 0; var15 < var14.getObjects().size(); ++var15) {
            IsoObject var11 = (IsoObject)var14.getObjects().get(var15);
            if (var11.getProperties() != null && (var11.getProperties().Is(var3) || var11.getProperties().Is(var4))) {
               var14.transmitRemoveItemFromSquare(var11);
               break;
            }
         }
      }

      return true;
   }

   private IsoObject getSpecialSolid() {
      int var1;
      IsoObject var2;
      for(var1 = 0; var1 < this.SpecialObjects.size(); ++var1) {
         var2 = (IsoObject)this.SpecialObjects.get(var1);
         if (var2 instanceof IsoThumpable var3) {
            if (!var3.isStairs() && var3.isThumpable() && var3.isBlockAllTheSquare()) {
               if (!var3.getProperties().Is(IsoFlagType.solidtrans) || !this.isAdjacentToWindow() && !this.isAdjacentToHoppable()) {
                  return var3;
               }

               return null;
            }
         }
      }

      for(var1 = 0; var1 < this.Objects.size(); ++var1) {
         var2 = (IsoObject)this.Objects.get(var1);
         if (var2.isMovedThumpable()) {
            if (!this.isAdjacentToWindow() && !this.isAdjacentToHoppable()) {
               return var2;
            }

            return null;
         }
      }

      return null;
   }

   public IsoObject testCollideSpecialObjects(IsoGridSquare var1) {
      if (var1 != null && var1 != this) {
         IsoObject var2;
         if (var1.x < this.x && var1.y == this.y) {
            if (var1.z == this.z && this.Has(IsoObjectType.stairsTW)) {
               return null;
            } else if (var1.z == this.z && this.hasSlopedSurfaceToLevelAbove(IsoDirections.W)) {
               return null;
            } else {
               var2 = this.getSpecialWall(false);
               if (var2 != null) {
                  return var2;
               } else if (this.isBlockedTo(var1)) {
                  return null;
               } else {
                  var2 = var1.getSpecialSolid();
                  return var2 != null ? var2 : null;
               }
            }
         } else if (var1.x == this.x && var1.y < this.y) {
            if (var1.z == this.z && this.Has(IsoObjectType.stairsTN)) {
               return null;
            } else if (var1.z == this.z && this.hasSlopedSurfaceToLevelAbove(IsoDirections.N)) {
               return null;
            } else {
               var2 = this.getSpecialWall(true);
               if (var2 != null) {
                  return var2;
               } else if (this.isBlockedTo(var1)) {
                  return null;
               } else {
                  var2 = var1.getSpecialSolid();
                  return var2 != null ? var2 : null;
               }
            }
         } else if (var1.x > this.x && var1.y == this.y) {
            var2 = var1.getSpecialWall(false);
            if (var2 != null) {
               return var2;
            } else if (this.isBlockedTo(var1)) {
               return null;
            } else {
               var2 = var1.getSpecialSolid();
               return var2 != null ? var2 : null;
            }
         } else if (var1.x == this.x && var1.y > this.y) {
            var2 = var1.getSpecialWall(true);
            if (var2 != null) {
               return var2;
            } else if (this.isBlockedTo(var1)) {
               return null;
            } else {
               var2 = var1.getSpecialSolid();
               return var2 != null ? var2 : null;
            }
         } else {
            IsoGridSquare var3;
            IsoGridSquare var4;
            if (var1.x < this.x && var1.y < this.y) {
               var2 = this.getSpecialWall(true);
               if (var2 != null) {
                  return var2;
               } else {
                  var2 = this.getSpecialWall(false);
                  if (var2 != null) {
                     return var2;
                  } else {
                     var3 = this.getCell().getGridSquare(this.x, this.y - 1, this.z);
                     if (var3 != null && !this.isBlockedTo(var3)) {
                        var2 = var3.getSpecialSolid();
                        if (var2 != null) {
                           return var2;
                        }

                        var2 = var3.getSpecialWall(false);
                        if (var2 != null) {
                           return var2;
                        }
                     }

                     var4 = this.getCell().getGridSquare(this.x - 1, this.y, this.z);
                     if (var4 != null && !this.isBlockedTo(var4)) {
                        var2 = var4.getSpecialSolid();
                        if (var2 != null) {
                           return var2;
                        }

                        var2 = var4.getSpecialWall(true);
                        if (var2 != null) {
                           return var2;
                        }
                     }

                     if (var3 != null && !this.isBlockedTo(var3) && var4 != null && !this.isBlockedTo(var4)) {
                        if (!var3.isBlockedTo(var1) && !var4.isBlockedTo(var1)) {
                           var2 = var1.getSpecialSolid();
                           return var2 != null ? var2 : null;
                        } else {
                           return null;
                        }
                     } else {
                        return null;
                     }
                  }
               }
            } else if (var1.x > this.x && var1.y < this.y) {
               var2 = this.getSpecialWall(true);
               if (var2 != null) {
                  return var2;
               } else {
                  var3 = this.getCell().getGridSquare(this.x, this.y - 1, this.z);
                  if (var3 != null && !this.isBlockedTo(var3)) {
                     var2 = var3.getSpecialSolid();
                     if (var2 != null) {
                        return var2;
                     }
                  }

                  var4 = this.getCell().getGridSquare(this.x + 1, this.y, this.z);
                  if (var4 != null) {
                     var2 = var4.getSpecialWall(false);
                     if (var2 != null) {
                        return var2;
                     }

                     if (!this.isBlockedTo(var4)) {
                        var2 = var4.getSpecialSolid();
                        if (var2 != null) {
                           return var2;
                        }

                        var2 = var4.getSpecialWall(true);
                        if (var2 != null) {
                           return var2;
                        }
                     }
                  }

                  if (var3 != null && !this.isBlockedTo(var3) && var4 != null && !this.isBlockedTo(var4)) {
                     var2 = var1.getSpecialWall(false);
                     if (var2 != null) {
                        return var2;
                     } else if (!var3.isBlockedTo(var1) && !var4.isBlockedTo(var1)) {
                        var2 = var1.getSpecialSolid();
                        return var2 != null ? var2 : null;
                     } else {
                        return null;
                     }
                  } else {
                     return null;
                  }
               }
            } else if (var1.x > this.x && var1.y > this.y) {
               var3 = this.getCell().getGridSquare(this.x, this.y + 1, this.z);
               if (var3 != null) {
                  var2 = var3.getSpecialWall(true);
                  if (var2 != null) {
                     return var2;
                  }

                  if (!this.isBlockedTo(var3)) {
                     var2 = var3.getSpecialSolid();
                     if (var2 != null) {
                        return var2;
                     }
                  }
               }

               var4 = this.getCell().getGridSquare(this.x + 1, this.y, this.z);
               if (var4 != null) {
                  var2 = var4.getSpecialWall(false);
                  if (var2 != null) {
                     return var2;
                  }

                  if (!this.isBlockedTo(var4)) {
                     var2 = var4.getSpecialSolid();
                     if (var2 != null) {
                        return var2;
                     }
                  }
               }

               if (var3 != null && !this.isBlockedTo(var3) && var4 != null && !this.isBlockedTo(var4)) {
                  var2 = var1.getSpecialWall(false);
                  if (var2 != null) {
                     return var2;
                  } else {
                     var2 = var1.getSpecialWall(true);
                     if (var2 != null) {
                        return var2;
                     } else if (!var3.isBlockedTo(var1) && !var4.isBlockedTo(var1)) {
                        var2 = var1.getSpecialSolid();
                        return var2 != null ? var2 : null;
                     } else {
                        return null;
                     }
                  }
               } else {
                  return null;
               }
            } else if (var1.x < this.x && var1.y > this.y) {
               var2 = this.getSpecialWall(false);
               if (var2 != null) {
                  return var2;
               } else {
                  var3 = this.getCell().getGridSquare(this.x, this.y + 1, this.z);
                  if (var3 != null) {
                     var2 = var3.getSpecialWall(true);
                     if (var2 != null) {
                        return var2;
                     }

                     if (!this.isBlockedTo(var3)) {
                        var2 = var3.getSpecialSolid();
                        if (var2 != null) {
                           return var2;
                        }
                     }
                  }

                  var4 = this.getCell().getGridSquare(this.x - 1, this.y, this.z);
                  if (var4 != null && !this.isBlockedTo(var4)) {
                     var2 = var4.getSpecialSolid();
                     if (var2 != null) {
                        return var2;
                     }
                  }

                  if (var3 != null && !this.isBlockedTo(var3) && var4 != null && !this.isBlockedTo(var4)) {
                     var2 = var1.getSpecialWall(true);
                     if (var2 != null) {
                        return var2;
                     } else if (!var3.isBlockedTo(var1) && !var4.isBlockedTo(var1)) {
                        var2 = var1.getSpecialSolid();
                        return var2 != null ? var2 : null;
                     } else {
                        return null;
                     }
                  } else {
                     return null;
                  }
               }
            } else {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   public IsoObject getDoorFrameTo(IsoGridSquare var1) {
      ArrayList var2 = null;
      if (var1.y >= this.y && var1.x >= this.x) {
         var2 = var1.SpecialObjects;
      } else {
         var2 = this.SpecialObjects;
      }

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         boolean var5;
         if (var2.get(var3) instanceof IsoDoor) {
            IsoDoor var4 = (IsoDoor)var2.get(var3);
            var5 = var4.north;
            if (var5 && var1.y != this.y) {
               return var4;
            }

            if (!var5 && var1.x != this.x) {
               return var4;
            }
         } else if (var2.get(var3) instanceof IsoThumpable && ((IsoThumpable)var2.get(var3)).isDoor) {
            IsoThumpable var6 = (IsoThumpable)var2.get(var3);
            var5 = var6.north;
            if (var5 && var1.y != this.y) {
               return var6;
            }

            if (!var5 && var1.x != this.x) {
               return var6;
            }
         }
      }

      return null;
   }

   public static void getSquaresForThread(ArrayDeque<IsoGridSquare> var0, int var1) {
      for(int var2 = 0; var2 < var1; ++var2) {
         IsoGridSquare var3 = (IsoGridSquare)isoGridSquareCache.poll();
         if (var3 == null) {
            var0.add(new IsoGridSquare((IsoCell)null, (SliceY)null, 0, 0, 0));
         } else {
            var0.add(var3);
         }
      }

   }

   public static IsoGridSquare getNew(IsoCell var0, SliceY var1, int var2, int var3, int var4) {
      IsoGridSquare var5 = (IsoGridSquare)isoGridSquareCache.poll();
      if (var5 == null) {
         return new IsoGridSquare(var0, var1, var2, var3, var4);
      } else {
         var5.x = var2;
         var5.y = var3;
         var5.z = var4;
         var5.CachedScreenValue = -1;
         col = 0;
         path = 0;
         pathdoor = 0;
         vision = 0;
         var5.collideMatrix = 134217727;
         var5.pathMatrix = 134217727;
         var5.visionMatrix = 0;
         return var5;
      }
   }

   public static IsoGridSquare getNew(ArrayDeque<IsoGridSquare> var0, IsoCell var1, SliceY var2, int var3, int var4, int var5) {
      IsoGridSquare var6 = null;
      if (var0.isEmpty()) {
         return new IsoGridSquare(var1, var2, var3, var4, var5);
      } else {
         var6 = (IsoGridSquare)var0.pop();
         var6.x = var3;
         var6.y = var4;
         var6.z = var5;
         var6.CachedScreenValue = -1;
         col = 0;
         path = 0;
         pathdoor = 0;
         vision = 0;
         var6.collideMatrix = 134217727;
         var6.pathMatrix = 134217727;
         var6.visionMatrix = 0;
         return var6;
      }
   }

   /** @deprecated */
   @Deprecated
   public long getHashCodeObjects() {
      this.recalcHashCodeObjects();
      return this.hashCodeObjects;
   }

   /** @deprecated */
   @Deprecated
   public int getHashCodeObjectsInt() {
      this.recalcHashCodeObjects();
      return (int)this.hashCodeObjects;
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
      int var1 = 0;
      this.recalcHashCodeObjects();
      var1 = var1 * 2 + this.Objects.size();
      var1 = (int)((long)var1 + this.getHashCodeObjects());

      int var2;
      for(var2 = 0; var2 < this.Objects.size(); ++var2) {
         var1 = var1 * 2 + ((IsoObject)this.Objects.get(var2)).hashCode();
      }

      var2 = 0;

      int var3;
      for(var3 = 0; var3 < this.StaticMovingObjects.size(); ++var3) {
         if (this.StaticMovingObjects.get(var3) instanceof IsoDeadBody) {
            ++var2;
         }
      }

      var1 = var1 * 2 + var2;

      for(var3 = 0; var3 < this.StaticMovingObjects.size(); ++var3) {
         IsoMovingObject var4 = (IsoMovingObject)this.StaticMovingObjects.get(var3);
         if (var4 instanceof IsoDeadBody) {
            var1 = var1 * 2 + var4.hashCode();
         }
      }

      if (this.table != null && !this.table.isEmpty()) {
         var1 = var1 * 2 + this.table.hashCode();
      }

      byte var5 = 0;
      if (this.isOverlayDone()) {
         var5 = (byte)(var5 | 1);
      }

      if (this.haveRoof) {
         var5 = (byte)(var5 | 2);
      }

      if (this.burntOut) {
         var5 = (byte)(var5 | 4);
      }

      var1 = var1 * 2 + var5;
      var1 = var1 * 2 + this.getErosionData().hashCode();
      if (this.getTrapPositionX() > 0) {
         var1 = var1 * 2 + this.getTrapPositionX();
         var1 = var1 * 2 + this.getTrapPositionY();
         var1 = var1 * 2 + this.getTrapPositionZ();
      }

      var1 = var1 * 2 + (this.haveElectricity() ? 1 : 0);
      var1 = var1 * 2 + (this.haveSheetRope ? 1 : 0);
      return var1;
   }

   public IsoGridSquare(IsoCell var1, SliceY var2, int var3, int var4, int var5) {
      this.hasTypes = new ZomboidBitFlag(IsoObjectType.MAX.index());
      this.Properties = new PropertyContainer();
      this.SpecialObjects = new ArrayList(0);
      this.haveRoof = false;
      this.burntOut = false;
      this.bHasFlies = false;
      this.OcclusionDataCache = null;
      this.overlayDone = false;
      this.table = null;
      this.trapPositionX = -1;
      this.trapPositionY = -1;
      this.trapPositionZ = -1;
      this.hourLastSeen = -2147483648;
      this.propertiesDirty = true;
      this.splashFrame = -1.0F;
      this.waterSplashData = new WaterSplashData();
      this.lightInfo = new ColorInfo[4];
      this.RainDrop = null;
      this.RainSplash = null;
      this.ID = ++IDMax;
      this.x = var3;
      this.y = var4;
      this.z = var5;
      this.CachedScreenValue = -1;
      col = 0;
      path = 0;
      pathdoor = 0;
      vision = 0;
      this.collideMatrix = 134217727;
      this.pathMatrix = 134217727;
      this.visionMatrix = 0;

      for(int var6 = 0; var6 < 4; ++var6) {
         if (GameServer.bServer) {
            if (var6 == 0) {
               this.lighting[var6] = new ServerLOS.ServerLighting();
            }
         } else if (LightingJNI.init) {
            this.lighting[var6] = new LightingJNI.JNILighting(var6, this);
         } else {
            this.lighting[var6] = new Lighting();
         }
      }

   }

   public IsoGridSquare getTileInDirection(IsoDirections var1) {
      if (var1 == IsoDirections.N) {
         return this.getCell().getGridSquare(this.x, this.y - 1, this.z);
      } else if (var1 == IsoDirections.NE) {
         return this.getCell().getGridSquare(this.x + 1, this.y - 1, this.z);
      } else if (var1 == IsoDirections.NW) {
         return this.getCell().getGridSquare(this.x - 1, this.y - 1, this.z);
      } else if (var1 == IsoDirections.E) {
         return this.getCell().getGridSquare(this.x + 1, this.y, this.z);
      } else if (var1 == IsoDirections.W) {
         return this.getCell().getGridSquare(this.x - 1, this.y, this.z);
      } else if (var1 == IsoDirections.SE) {
         return this.getCell().getGridSquare(this.x + 1, this.y + 1, this.z);
      } else if (var1 == IsoDirections.SW) {
         return this.getCell().getGridSquare(this.x - 1, this.y + 1, this.z);
      } else {
         return var1 == IsoDirections.S ? this.getCell().getGridSquare(this.x, this.y + 1, this.z) : null;
      }
   }

   public IsoObject getWall() {
      for(int var1 = 0; var1 < this.Objects.size(); ++var1) {
         IsoObject var2 = (IsoObject)this.Objects.get(var1);
         if (var2 != null && var2.sprite != null && (var2.sprite.cutW || var2.sprite.cutN)) {
            return var2;
         }
      }

      return null;
   }

   public IsoObject getThumpableWall(boolean var1) {
      IsoObject var2 = this.getWall(var1);
      return var2 != null && var2 instanceof IsoThumpable ? var2 : null;
   }

   public IsoObject getHoppableWall(boolean var1) {
      for(int var2 = 0; var2 < this.Objects.size(); ++var2) {
         IsoObject var3 = (IsoObject)this.Objects.get(var2);
         if (var3 != null && var3.sprite != null) {
            PropertyContainer var4 = var3.getProperties();
            boolean var5 = var4.Is(IsoFlagType.TallHoppableW) && !var4.Is(IsoFlagType.WallWTrans);
            boolean var6 = var4.Is(IsoFlagType.TallHoppableN) && !var4.Is(IsoFlagType.WallNTrans);
            if (var5 && !var1 || var6 && var1) {
               return var3;
            }
         }
      }

      return null;
   }

   public IsoObject getThumpableWallOrHoppable(boolean var1) {
      IsoObject var2 = this.getThumpableWall(var1);
      IsoObject var3 = this.getHoppableWall(var1);
      if (var2 != null && var3 != null && var2 == var3) {
         return var2;
      } else if (var2 == null && var3 != null) {
         return var3;
      } else {
         return var2 != null && var3 == null ? var2 : null;
      }
   }

   public Boolean getWallFull() {
      for(int var1 = 0; var1 < this.Objects.size(); ++var1) {
         IsoObject var2 = (IsoObject)this.Objects.get(var1);
         if (var2 != null && var2.sprite != null && (var2.sprite.cutN || var2.sprite.cutW || var2.sprite.getProperties().Is(IsoFlagType.WallN) || var2.sprite.getProperties().Is(IsoFlagType.WallW))) {
            return true;
         }
      }

      return false;
   }

   IsoObject getWallExcludingList(boolean var1, ArrayList<String> var2) {
      for(int var3 = 0; var3 < this.Objects.size(); ++var3) {
         IsoObject var4 = (IsoObject)this.Objects.get(var3);
         if (var4 != null && var4.sprite != null && !var2.contains(var4.sprite.name) && (var1 && (var4.sprite.cutN || var4.sprite.getProperties().Is(IsoFlagType.WallN)) || !var1 && (var4.sprite.cutW || var4.sprite.getProperties().Is(IsoFlagType.WallW)))) {
            return var4;
         }
      }

      return null;
   }

   public IsoObject getWallExcludingObject(boolean var1, IsoObject var2) {
      for(int var3 = 0; var3 < this.Objects.size(); ++var3) {
         IsoObject var4 = (IsoObject)this.Objects.get(var3);
         if (var4 != null && var4.sprite != null && var4 != var2 && (var1 && (var4.sprite.cutN || var4.sprite.getProperties().Is(IsoFlagType.WallN)) || !var1 && (var4.sprite.cutW || var4.sprite.getProperties().Is(IsoFlagType.WallW)))) {
            return var4;
         }
      }

      return null;
   }

   public IsoObject getWall(boolean var1) {
      for(int var2 = 0; var2 < this.Objects.size(); ++var2) {
         IsoObject var3 = (IsoObject)this.Objects.get(var2);
         if (var3 != null && var3.sprite != null && (var3.sprite.cutN && var1 || var3.sprite.cutW && !var1)) {
            return var3;
         }
      }

      return null;
   }

   public IsoObject getWallSE() {
      for(int var1 = 0; var1 < this.Objects.size(); ++var1) {
         IsoObject var2 = (IsoObject)this.Objects.get(var1);
         if (var2 != null && var2.sprite != null && var2.sprite.getProperties().Is(IsoFlagType.WallSE)) {
            return var2;
         }
      }

      return null;
   }

   public IsoObject getWallNW() {
      for(int var1 = 0; var1 < this.Objects.size(); ++var1) {
         IsoObject var2 = (IsoObject)this.Objects.get(var1);
         if (var2 != null && var2.sprite != null && var2.sprite.getProperties().Is(IsoFlagType.WallNW)) {
            return var2;
         }
      }

      return null;
   }

   public IsoObject getGarageDoor(boolean var1) {
      for(int var2 = 0; var2 < this.Objects.size(); ++var2) {
         IsoObject var3 = (IsoObject)this.Objects.get(var2);
         if (IsoDoor.getGarageDoorIndex(var3) != -1) {
            boolean var4 = var3 instanceof IsoDoor ? ((IsoDoor)var3).getNorth() : ((IsoThumpable)var3).getNorth();
            if (var1 == var4) {
               return var3;
            }
         }
      }

      return null;
   }

   public IsoObject getFloor() {
      for(int var1 = 0; var1 < this.Objects.size(); ++var1) {
         IsoObject var2 = (IsoObject)this.Objects.get(var1);
         if (var2.sprite != null && var2.sprite.getProperties().Is(IsoFlagType.solidfloor)) {
            return var2;
         }
      }

      return null;
   }

   public IsoObject getPlayerBuiltFloor() {
      return this.getBuilding() == null && (this.roofHideBuilding == null || this.roofHideBuilding.isEntirelyEmptyOutside()) ? this.getFloor() : null;
   }

   public IsoObject getWaterObject() {
      for(int var1 = 0; var1 < this.Objects.size(); ++var1) {
         IsoObject var2 = (IsoObject)this.Objects.get(var1);
         if (var2.sprite != null && var2.sprite.getProperties().Is(IsoFlagType.water)) {
            return var2;
         }
      }

      return null;
   }

   public void interpolateLight(ColorInfo var1, float var2, float var3) {
      IsoCell var4 = this.getCell();
      if (var2 < 0.0F) {
         var2 = 0.0F;
      }

      if (var2 > 1.0F) {
         var2 = 1.0F;
      }

      if (var3 < 0.0F) {
         var3 = 0.0F;
      }

      if (var3 > 1.0F) {
         var3 = 1.0F;
      }

      int var5 = IsoCamera.frameState.playerIndex;
      int var6 = this.getVertLight(0, var5);
      int var7 = this.getVertLight(1, var5);
      int var8 = this.getVertLight(2, var5);
      int var9 = this.getVertLight(3, var5);
      Color.abgrToColor(var6, tl);
      Color.abgrToColor(var9, bl);
      Color.abgrToColor(var7, tr);
      Color.abgrToColor(var8, br);
      tl.interp(tr, var2, interp1);
      bl.interp(br, var2, interp2);
      interp1.interp(interp2, var3, finalCol);
      var1.r = finalCol.r;
      var1.g = finalCol.g;
      var1.b = finalCol.b;
      var1.a = finalCol.a;
   }

   public void EnsureSurroundNotNull() {
      assert !GameServer.bServer;

      for(int var1 = -1; var1 <= 1; ++var1) {
         for(int var2 = -1; var2 <= 1; ++var2) {
            if ((var1 != 0 || var2 != 0) && IsoWorld.instance.isValidSquare(this.x + var1, this.y + var2, this.z) && this.getCell().getChunkForGridSquare(this.x + var1, this.y + var2, this.z) != null) {
               boolean var3 = false;
               IsoGridSquare var4 = this.getCell().getGridSquare(this.x + var1, this.y + var2, this.z);
               if (var4 == null) {
                  var4 = getNew(this.getCell(), (SliceY)null, this.x + var1, this.y + var2, this.z);
                  IsoGridSquare var5 = this.getCell().ConnectNewSquare(var4, false);
                  var3 = true;
               }

               if (var3 && var4.z < 0) {
                  var4.addUndergroundBlock("underground_01_0");
               }
            }
         }
      }

   }

   public void setSquareChanged() {
      this.setCachedIsFree(false);
      PolygonalMap2.instance.squareChanged(this);
      IsoGridOcclusionData.SquareChanged();
      IsoRegions.squareChanged(this);
   }

   public IsoObject addFloor(String var1) {
      IsoRegions.setPreviousFlags(this);
      IsoObject var2 = new IsoObject(this.getCell(), this, var1);
      boolean var3 = false;

      int var4;
      for(var4 = 0; var4 < this.getObjects().size(); ++var4) {
         IsoObject var5 = (IsoObject)this.getObjects().get(var4);
         IsoSprite var6 = var5.sprite;
         if (var6 != null && (var6.getProperties().Is(IsoFlagType.solidfloor) || var6.getProperties().Is(IsoFlagType.noStart) || var6.getProperties().Is(IsoFlagType.vegitation) && var5.getType() != IsoObjectType.tree || var6.getProperties().Is(IsoFlagType.taintedWater) || var6.getName() != null && var6.getName().startsWith("blends_grassoverlays"))) {
            if (var6.getName() != null && var6.getName().startsWith("floors_rugs")) {
               var3 = true;
            } else {
               this.transmitRemoveItemFromSquare(var5);
               --var4;
            }
         }
      }

      var2.sprite.getProperties().Set(IsoFlagType.solidfloor);
      if (var3) {
         this.getObjects().add(0, var2);
      } else {
         this.getObjects().add(var2);
      }

      this.EnsureSurroundNotNull();
      this.RecalcProperties();
      DesignationZoneAnimal.addNewRoof(this.x, this.y, this.z);
      this.getCell().checkHaveRoof(this.x, this.y);

      for(var4 = 0; var4 < IsoPlayer.numPlayers; ++var4) {
         LosUtil.cachecleared[var4] = true;
      }

      setRecalcLightTime(-1.0F);
      if (PerformanceSettings.FBORenderChunk) {
         ++Core.dirtyGlobalLightsCount;
      }

      GameTime.getInstance().lightSourceUpdate = 100.0F;
      var2.transmitCompleteItemToServer();
      this.RecalcAllWithNeighbours(true);

      for(var4 = this.z - 1; var4 > 0; --var4) {
         IsoGridSquare var7 = this.getCell().getGridSquare(this.x, this.y, var4);
         if (var7 == null) {
            var7 = getNew(this.getCell(), (SliceY)null, this.x, this.y, var4);
            this.getCell().ConnectNewSquare(var7, false);
         }

         var7.EnsureSurroundNotNull();
         var7.RecalcAllWithNeighbours(true);
      }

      this.setCachedIsFree(false);
      PolygonalMap2.instance.squareChanged(this);
      IsoGridOcclusionData.SquareChanged();
      IsoRegions.squareChanged(this);
      this.clearWater();
      var2.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_ADD);
      return var2;
   }

   public IsoObject addUndergroundBlock(String var1) {
      IsoRegions.setPreviousFlags(this);
      IsoObject var2 = new IsoObject(this.getCell(), this, var1);
      var2.sprite.getProperties().Set(IsoFlagType.solid);
      this.getObjects().add(var2);
      this.RecalcProperties();

      for(int var3 = 0; var3 < IsoPlayer.numPlayers; ++var3) {
         LosUtil.cachecleared[var3] = true;
      }

      ++Core.dirtyGlobalLightsCount;
      setRecalcLightTime(-1.0F);
      GameTime.getInstance().lightSourceUpdate = 100.0F;
      var2.transmitCompleteItemToServer();
      this.RecalcAllWithNeighbours(true);
      this.setCachedIsFree(false);
      PolygonalMap2.instance.squareChanged(this);
      IsoGridOcclusionData.SquareChanged();
      IsoRegions.squareChanged(this);
      this.clearWater();
      return var2;
   }

   public boolean isUndergroundBlock() {
      if (this.getObjects().size() != 1) {
         return false;
      } else {
         IsoObject var1 = (IsoObject)this.getObjects().get(0);
         return var1 != null && var1.getSprite() != null && var1.getSprite().getName() != null && var1.getSprite().getName().startsWith("underground_01");
      }
   }

   public IsoThumpable AddStairs(boolean var1, int var2, String var3, String var4, KahluaTable var5) {
      IsoRegions.setPreviousFlags(this);
      this.EnsureSurroundNotNull();
      boolean var6 = !this.TreatAsSolidFloor() && !this.HasStairsBelow();
      this.CachedIsFree = false;
      IsoThumpable var7 = new IsoThumpable(this.getCell(), this, var3, var1, var5);
      if (var1) {
         if (var2 == 0) {
            var7.setType(IsoObjectType.stairsBN);
         }

         if (var2 == 1) {
            var7.setType(IsoObjectType.stairsMN);
         }

         if (var2 == 2) {
            var7.setType(IsoObjectType.stairsTN);
            var7.sprite.getProperties().Set(var1 ? IsoFlagType.cutN : IsoFlagType.cutW);
         }
      }

      if (!var1) {
         if (var2 == 0) {
            var7.setType(IsoObjectType.stairsBW);
         }

         if (var2 == 1) {
            var7.setType(IsoObjectType.stairsMW);
         }

         if (var2 == 2) {
            var7.setType(IsoObjectType.stairsTW);
            var7.sprite.getProperties().Set(var1 ? IsoFlagType.cutN : IsoFlagType.cutW);
         }
      }

      this.AddSpecialObject(var7);
      int var8;
      if (var6 && var2 == 2) {
         var8 = this.z - 1;
         IsoGridSquare var9 = this.getCell().getGridSquare(this.x, this.y, var8);
         if (var9 == null) {
            var9 = new IsoGridSquare(this.getCell(), (SliceY)null, this.x, this.y, var8);
            this.getCell().ConnectNewSquare(var9, true);
         }

         while(var8 >= 0) {
            IsoThumpable var10 = new IsoThumpable(this.getCell(), var9, var4, var1, var5);
            var9.AddSpecialObject(var10);
            var10.transmitCompleteItemToServer();
            if (var9.TreatAsSolidFloor()) {
               break;
            }

            --var8;
            if (this.getCell().getGridSquare(var9.x, var9.y, var8) == null) {
               var9 = new IsoGridSquare(this.getCell(), (SliceY)null, var9.x, var9.y, var8);
               this.getCell().ConnectNewSquare(var9, true);
            } else {
               var9 = this.getCell().getGridSquare(var9.x, var9.y, var8);
            }
         }
      }

      if (var2 == 2) {
         IsoGridSquare var12 = null;
         if (var1) {
            if (IsoWorld.instance.isValidSquare(this.x, this.y - 1, this.z + 1)) {
               var12 = this.getCell().getGridSquare(this.x, this.y - 1, this.z + 1);
               if (var12 == null) {
                  var12 = new IsoGridSquare(this.getCell(), (SliceY)null, this.x, this.y - 1, this.z + 1);
                  this.getCell().ConnectNewSquare(var12, false);
               }

               if (!var12.Properties.Is(IsoFlagType.solidfloor)) {
                  var12.addFloor("carpentry_02_57");
               }
            }
         } else if (IsoWorld.instance.isValidSquare(this.x - 1, this.y, this.z + 1)) {
            var12 = this.getCell().getGridSquare(this.x - 1, this.y, this.z + 1);
            if (var12 == null) {
               var12 = new IsoGridSquare(this.getCell(), (SliceY)null, this.x - 1, this.y, this.z + 1);
               this.getCell().ConnectNewSquare(var12, false);
            }

            if (!var12.Properties.Is(IsoFlagType.solidfloor)) {
               var12.addFloor("carpentry_02_57");
            }
         }

         var12.getModData().rawset("ConnectedToStairs" + var1, true);
         var12 = this.getCell().getGridSquare(this.x, this.y, this.z + 1);
         if (var12 == null) {
            var12 = new IsoGridSquare(this.getCell(), (SliceY)null, this.x, this.y, this.z + 1);
            this.getCell().ConnectNewSquare(var12, false);
         }
      }

      for(var8 = this.getX() - 1; var8 <= this.getX() + 1; ++var8) {
         for(int var14 = this.getY() - 1; var14 <= this.getY() + 1; ++var14) {
            for(int var13 = this.getZ() - 1; var13 <= this.getZ() + 1; ++var13) {
               if (IsoWorld.instance.isValidSquare(var8, var14, var13)) {
                  IsoGridSquare var11 = this.getCell().getGridSquare(var8, var14, var13);
                  if (var11 != this) {
                     if (var11 == null) {
                        var11 = new IsoGridSquare(this.getCell(), (SliceY)null, var8, var14, var13);
                        this.getCell().ConnectNewSquare(var11, false);
                     }

                     var11.RecalcAllWithNeighbours(true);
                  }
               }
            }
         }
      }

      return var7;
   }

   void ReCalculateAll(IsoGridSquare var1) {
      this.ReCalculateAll(var1, cellGetSquare);
   }

   void ReCalculateAll(IsoGridSquare var1, GetSquare var2) {
      if (var1 != null && var1 != this) {
         this.SolidFloorCached = false;
         var1.SolidFloorCached = false;
         this.RecalcPropertiesIfNeeded();
         var1.RecalcPropertiesIfNeeded();
         this.ReCalculateCollide(var1, var2);
         var1.ReCalculateCollide(this, var2);
         this.ReCalculatePathFind(var1, var2);
         var1.ReCalculatePathFind(this, var2);
         this.ReCalculateVisionBlocked(var1, var2);
         var1.ReCalculateVisionBlocked(this, var2);
         this.setBlockedGridPointers(var2);
         var1.setBlockedGridPointers(var2);
      }
   }

   void ReCalculateAll(boolean var1, IsoGridSquare var2, GetSquare var3) {
      if (var2 != null && var2 != this) {
         this.SolidFloorCached = false;
         var2.SolidFloorCached = false;
         this.RecalcPropertiesIfNeeded();
         if (var1) {
            var2.RecalcPropertiesIfNeeded();
         }

         this.ReCalculateCollide(var2, var3);
         if (var1) {
            var2.ReCalculateCollide(this, var3);
         }

         this.ReCalculatePathFind(var2, var3);
         if (var1) {
            var2.ReCalculatePathFind(this, var3);
         }

         this.ReCalculateVisionBlocked(var2, var3);
         if (var1) {
            var2.ReCalculateVisionBlocked(this, var3);
         }

         this.setBlockedGridPointers(var3);
         if (var1) {
            var2.setBlockedGridPointers(var3);
         }

      }
   }

   void ReCalculateMineOnly(IsoGridSquare var1) {
      this.SolidFloorCached = false;
      this.RecalcProperties();
      this.ReCalculateCollide(var1);
      this.ReCalculatePathFind(var1);
      this.ReCalculateVisionBlocked(var1);
      this.setBlockedGridPointers(cellGetSquare);
   }

   public boolean getOpenAir() {
      if (!this.getProperties().Is(IsoFlagType.exterior)) {
         return false;
      } else {
         IsoGridSquare var1 = this.u;
         int var2 = this.z;
         if (var1 != null) {
            return var1.getOpenAir();
         } else {
            return IsoCell.getInstance().getGridSquare(this.x, this.y, this.z + 1) == null && this.z >= 0;
         }
      }
   }

   public void RecalcAllWithNeighbours(boolean var1) {
      this.RecalcAllWithNeighbours(var1, cellGetSquare);
   }

   public void RecalcAllWithNeighbours(boolean var1, GetSquare var2) {
      this.SolidFloorCached = false;
      this.RecalcPropertiesIfNeeded();

      for(int var3 = this.getX() - 1; var3 <= this.getX() + 1; ++var3) {
         for(int var4 = this.getY() - 1; var4 <= this.getY() + 1; ++var4) {
            for(int var5 = this.getZ() - 1; var5 <= this.getZ() + 1; ++var5) {
               if (IsoWorld.instance.isValidSquare(var3, var4, var5)) {
                  int var6 = var3 - this.getX();
                  int var7 = var4 - this.getY();
                  int var8 = var5 - this.getZ();
                  if (var6 != 0 || var7 != 0 || var8 != 0) {
                     IsoGridSquare var9 = var2.getGridSquare(var3, var4, var5);
                     if (var9 != null) {
                        var9.DirtySlice();
                        this.ReCalculateAll(var1, var9, var2);
                     }
                  }
               }
            }
         }
      }

      IsoWorld.instance.CurrentCell.DoGridNav(this, var2);
      IsoGridSquare var10 = this.nav[IsoDirections.N.index()];
      IsoGridSquare var11 = this.nav[IsoDirections.S.index()];
      IsoGridSquare var12 = this.nav[IsoDirections.W.index()];
      IsoGridSquare var13 = this.nav[IsoDirections.E.index()];
      if (var10 != null && var12 != null) {
         var10.ReCalculateAll(var12, var2);
      }

      if (var10 != null && var13 != null) {
         var10.ReCalculateAll(var13, var2);
      }

      if (var11 != null && var12 != null) {
         var11.ReCalculateAll(var12, var2);
      }

      if (var11 != null && var13 != null) {
         var11.ReCalculateAll(var13, var2);
      }

   }

   public void RecalcAllWithNeighboursMineOnly() {
      this.SolidFloorCached = false;
      this.RecalcProperties();

      for(int var1 = this.getX() - 1; var1 <= this.getX() + 1; ++var1) {
         for(int var2 = this.getY() - 1; var2 <= this.getY() + 1; ++var2) {
            for(int var3 = this.getZ() - 1; var3 <= this.getZ() + 1; ++var3) {
               if (var3 >= 0) {
                  int var4 = var1 - this.getX();
                  int var5 = var2 - this.getY();
                  int var6 = var3 - this.getZ();
                  if (var4 != 0 || var5 != 0 || var6 != 0) {
                     IsoGridSquare var7 = this.getCell().getGridSquare(var1, var2, var3);
                     if (var7 != null) {
                        var7.DirtySlice();
                        this.ReCalculateMineOnly(var7);
                     }
                  }
               }
            }
         }
      }

   }

   boolean IsWindow(int var1, int var2, int var3) {
      IsoGridSquare var4 = this.getCell().getGridSquare(this.x + var1, this.y + var2, this.z + var3);
      return this.getWindowTo(var4) != null || this.getWindowThumpableTo(var4) != null;
   }

   void RemoveAllWith(IsoFlagType var1) {
      for(int var2 = 0; var2 < this.Objects.size(); ++var2) {
         IsoObject var3 = (IsoObject)this.Objects.get(var2);
         if (var3.sprite != null && var3.sprite.getProperties().Is(var1)) {
            this.Objects.remove(var3);
            this.SpecialObjects.remove(var3);
            --var2;
         }
      }

      this.RecalcAllWithNeighbours(true);
   }

   public boolean hasSupport() {
      IsoGridSquare var1 = this.getCell().getGridSquare(this.x, this.y + 1, this.z);
      IsoGridSquare var2 = this.getCell().getGridSquare(this.x + 1, this.y, this.z);

      for(int var3 = 0; var3 < this.Objects.size(); ++var3) {
         IsoObject var4 = (IsoObject)this.Objects.get(var3);
         if (var4.sprite != null && (var4.sprite.getProperties().Is(IsoFlagType.solid) || (var4.sprite.getProperties().Is(IsoFlagType.cutW) || var4.sprite.getProperties().Is(IsoFlagType.cutN)) && !var4.sprite.Properties.Is(IsoFlagType.halfheight))) {
            return true;
         }
      }

      if (var1 != null && var1.Properties.Is(IsoFlagType.cutN) && !var1.Properties.Is(IsoFlagType.halfheight)) {
         return true;
      } else if (var2 != null && var2.Properties.Is(IsoFlagType.cutW) && !var1.Properties.Is(IsoFlagType.halfheight)) {
         return true;
      } else {
         return false;
      }
   }

   public Integer getID() {
      return this.ID;
   }

   public void setID(int var1) {
      this.ID = var1;
   }

   private int savematrix(boolean[][][] var1, byte[] var2, int var3) {
      for(int var4 = 0; var4 <= 2; ++var4) {
         for(int var5 = 0; var5 <= 2; ++var5) {
            for(int var6 = 0; var6 <= 2; ++var6) {
               var2[var3] = (byte)(var1[var4][var5][var6] ? 1 : 0);
               ++var3;
            }
         }
      }

      return var3;
   }

   private int loadmatrix(boolean[][][] var1, byte[] var2, int var3) {
      for(int var4 = 0; var4 <= 2; ++var4) {
         for(int var5 = 0; var5 <= 2; ++var5) {
            for(int var6 = 0; var6 <= 2; ++var6) {
               var1[var4][var5][var6] = var2[var3] != 0;
               ++var3;
            }
         }
      }

      return var3;
   }

   private void savematrix(boolean[][][] var1, ByteBuffer var2) {
      for(int var3 = 0; var3 <= 2; ++var3) {
         for(int var4 = 0; var4 <= 2; ++var4) {
            for(int var5 = 0; var5 <= 2; ++var5) {
               var2.put((byte)(var1[var3][var4][var5] ? 1 : 0));
            }
         }
      }

   }

   private void loadmatrix(boolean[][][] var1, ByteBuffer var2) {
      for(int var3 = 0; var3 <= 2; ++var3) {
         for(int var4 = 0; var4 <= 2; ++var4) {
            for(int var5 = 0; var5 <= 2; ++var5) {
               var1[var3][var4][var5] = var2.get() != 0;
            }
         }
      }

   }

   public void DirtySlice() {
   }

   public void setHourSeenToCurrent() {
      this.hourLastSeen = (int)GameTime.instance.getWorldAgeHours();
   }

   public void splatBlood(int var1, float var2) {
      // $FF: Couldn't be decompiled
   }

   public boolean haveBlood() {
      if (Core.getInstance().getOptionBloodDecals() == 0) {
         return false;
      } else {
         int var1;
         for(var1 = 0; var1 < this.getObjects().size(); ++var1) {
            IsoObject var2 = (IsoObject)this.getObjects().get(var1);
            if (var2.wallBloodSplats != null && !var2.wallBloodSplats.isEmpty()) {
               return true;
            }
         }

         for(var1 = 0; var1 < this.getChunk().FloorBloodSplats.size(); ++var1) {
            IsoFloorBloodSplat var5 = (IsoFloorBloodSplat)this.getChunk().FloorBloodSplats.get(var1);
            float var3 = var5.x + (float)(this.getChunk().wx * 8);
            float var4 = var5.y + (float)(this.getChunk().wy * 8);
            if (PZMath.fastfloor(var3) - 1 <= this.x && PZMath.fastfloor(var3) + 1 >= this.x && PZMath.fastfloor(var4) - 1 <= this.y && PZMath.fastfloor(var4) + 1 >= this.y) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean haveBloodWall() {
      if (Core.getInstance().getOptionBloodDecals() == 0) {
         return false;
      } else {
         for(int var1 = 0; var1 < this.getObjects().size(); ++var1) {
            IsoObject var2 = (IsoObject)this.getObjects().get(var1);
            if (var2.wallBloodSplats != null && !var2.wallBloodSplats.isEmpty()) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean haveBloodFloor() {
      if (Core.getInstance().getOptionBloodDecals() == 0) {
         return false;
      } else {
         for(int var1 = 0; var1 < this.getChunk().FloorBloodSplats.size(); ++var1) {
            IsoFloorBloodSplat var2 = (IsoFloorBloodSplat)this.getChunk().FloorBloodSplats.get(var1);
            float var3 = var2.x + (float)(this.getChunk().wx * 8);
            float var4 = var2.y + (float)(this.getChunk().wy * 8);
            if ((int)var3 - 1 <= this.x && (int)var3 + 1 >= this.x && (int)var4 - 1 <= this.y && (int)var4 + 1 >= this.y) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean haveGrime() {
      for(int var1 = 0; var1 < this.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)this.getObjects().get(var1);
         if (var2 != null && var2.getAttachedAnimSprite() != null) {
            ArrayList var3 = var2.getAttachedAnimSprite();

            for(int var4 = 0; var4 < var3.size(); ++var4) {
               IsoSpriteInstance var5 = (IsoSpriteInstance)var3.get(var4);
               if (var5 != null && var5.getParentSprite() != null && var5.getParentSprite().getName() != null && var5.getParentSprite().getName().contains("overlay_grime")) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public boolean haveGrimeWall() {
      for(int var1 = 0; var1 < this.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)this.getObjects().get(var1);
         if (var2 != null && !var2.isFloor() && var2.getAttachedAnimSprite() != null) {
            ArrayList var3 = var2.getAttachedAnimSprite();

            for(int var4 = 0; var4 < var3.size(); ++var4) {
               IsoSpriteInstance var5 = (IsoSpriteInstance)var3.get(var4);
               if (var5 != null && var5.getParentSprite() != null && var5.getParentSprite().getName() != null && var5.getParentSprite().getName().contains("overlay_grime")) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public boolean haveGrimeFloor() {
      for(int var1 = 0; var1 < this.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)this.getObjects().get(var1);
         if (var2 != null && var2.isFloor() && var2.getAttachedAnimSprite() != null) {
            ArrayList var3 = var2.getAttachedAnimSprite();

            for(int var4 = 0; var4 < var3.size(); ++var4) {
               IsoSpriteInstance var5 = (IsoSpriteInstance)var3.get(var4);
               if (var5 != null && var5.getParentSprite() != null && var5.getParentSprite().getName() != null && var5.getParentSprite().getName().contains("overlay_grime")) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public boolean haveGraffiti() {
      for(int var1 = 0; var1 < this.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)this.getObjects().get(var1);
         if (var2 != null && var2.getAttachedAnimSprite() != null) {
            ArrayList var3 = var2.getAttachedAnimSprite();

            for(int var4 = 0; var4 < var3.size(); ++var4) {
               IsoSpriteInstance var5 = (IsoSpriteInstance)var3.get(var4);
               if (var5 != null && var5.getParentSprite() != null && var5.getParentSprite().getName() != null && (var5.getParentSprite().getName().contains("overlay_graffiti") || var5.getParentSprite().getName().contains("overlay_messages") || var5.getParentSprite().getName().contains("constructedobjects_signs_01_4"))) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public IsoObject getGraffitiObject() {
      Object var1 = null;

      for(int var2 = 0; var2 < this.getObjects().size(); ++var2) {
         IsoObject var3 = (IsoObject)this.getObjects().get(var2);
         if (var3 != null && var3.getAttachedAnimSprite() != null) {
            ArrayList var4 = var3.getAttachedAnimSprite();

            for(int var5 = 0; var5 < var4.size(); ++var5) {
               IsoSpriteInstance var6 = (IsoSpriteInstance)var4.get(var5);
               if (var6 != null && var6.getParentSprite() != null && var6.getParentSprite().getName() != null && (var6.getParentSprite().getName().contains("overlay_graffiti") || var6.getParentSprite().getName().contains("overlay_messages") || var6.getParentSprite().getName().contains("constructedobjects_signs_01_4"))) {
                  return var3;
               }
            }
         }
      }

      return (IsoObject)var1;
   }

   public boolean haveStains() {
      return this.haveBlood() || this.haveGrime();
   }

   public void removeGrime() {
      while(this.haveGrime()) {
         for(int var1 = 0; var1 < this.getObjects().size(); ++var1) {
            IsoObject var2 = (IsoObject)this.getObjects().get(var1);
            if (var2 != null && var2.getAttachedAnimSprite() != null) {
               boolean var3 = false;
               ArrayList var4 = var2.getAttachedAnimSprite();

               for(int var5 = 0; var5 < var4.size(); ++var5) {
                  IsoSpriteInstance var6 = (IsoSpriteInstance)var4.get(var5);
                  if (var6 != null && var6.getParentSprite() != null && var6.getParentSprite().getName() != null && var6.getParentSprite().getName().contains("overlay_grime")) {
                     var2.RemoveAttachedAnim(var5);
                  }

                  var3 = true;
               }

               if (var3) {
                  var2.transmitUpdatedSpriteToClients();
               }
            }
         }
      }

   }

   public void removeGraffiti() {
      while(this.haveGraffiti()) {
         for(int var1 = 0; var1 < this.getObjects().size(); ++var1) {
            IsoObject var2 = (IsoObject)this.getObjects().get(var1);
            if (var2 != null && var2.getAttachedAnimSprite() != null) {
               boolean var3 = false;
               ArrayList var4 = var2.getAttachedAnimSprite();

               for(int var5 = 0; var5 < var4.size(); ++var5) {
                  IsoSpriteInstance var6 = (IsoSpriteInstance)var4.get(var5);
                  if (var6 != null && var6.getParentSprite() != null && var6.getParentSprite().getName() != null && (var6.getParentSprite().getName().contains("overlay_graffiti") || var6.getParentSprite().getName().contains("overlay_messages") || var6.getParentSprite().getName().contains("constructedobjects_signs_01_4"))) {
                     var2.RemoveAttachedAnim(var5);
                  }

                  var3 = true;
               }

               if (var3) {
                  var2.transmitUpdatedSpriteToClients();
               }
            }
         }
      }

   }

   public void removeBlood(boolean var1, boolean var2) {
      int var3;
      for(var3 = 0; var3 < this.getObjects().size(); ++var3) {
         IsoObject var4 = (IsoObject)this.getObjects().get(var3);
         if (var4.wallBloodSplats != null) {
            var4.wallBloodSplats.clear();
         }
      }

      if (!var2) {
         for(var3 = 0; var3 < this.getChunk().FloorBloodSplats.size(); ++var3) {
            IsoFloorBloodSplat var7 = (IsoFloorBloodSplat)this.getChunk().FloorBloodSplats.get(var3);
            int var5 = (int)((float)(this.getChunk().wx * 8) + var7.x);
            int var6 = (int)((float)(this.getChunk().wy * 8) + var7.y);
            if (var5 >= this.getX() - 1 && var5 <= this.getX() + 1 && var6 >= this.getY() - 1 && var6 <= this.getY() + 1) {
               this.getChunk().FloorBloodSplats.remove(var3);
               --var3;
            }
         }
      }

      if (GameServer.bServer && !var1) {
         INetworkPacket.sendToRelative(PacketTypes.PacketType.RemoveBlood, (float)this.x, (float)this.y, this, var2);
      }

      if (PerformanceSettings.FBORenderChunk && Thread.currentThread() == GameWindow.GameThread) {
         this.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_BLOOD);
      }

   }

   public void DoSplat(String var1, boolean var2, IsoFlagType var3, float var4, float var5, float var6) {
      for(int var7 = 0; var7 < this.getObjects().size(); ++var7) {
         IsoObject var8 = (IsoObject)this.getObjects().get(var7);
         if (var8.sprite != null && var8.sprite.getProperties().Is(var3) && (!(var8 instanceof IsoWindow) || !var8.isDestroyed())) {
            IsoSprite var9 = IsoSprite.getSprite(IsoSpriteManager.instance, (String)var1, 0);
            if (var9 == null) {
               return;
            }

            if (var8.wallBloodSplats == null) {
               var8.wallBloodSplats = new ArrayList();
            }

            IsoWallBloodSplat var10 = new IsoWallBloodSplat((float)GameTime.getInstance().getWorldAgeHours(), var9);
            var8.wallBloodSplats.add(var10);
            if (PerformanceSettings.FBORenderChunk && Thread.currentThread() == GameWindow.GameThread) {
               this.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_BLOOD);
            }
            break;
         }
      }

   }

   public void ClearTileObjects() {
      this.Objects.clear();
      this.RecalcProperties();
   }

   public void ClearTileObjectsExceptFloor() {
      for(int var1 = 0; var1 < this.Objects.size(); ++var1) {
         IsoObject var2 = (IsoObject)this.Objects.get(var1);
         if (var2.sprite == null || !var2.sprite.getProperties().Is(IsoFlagType.solidfloor)) {
            this.Objects.remove(var2);
            --var1;
         }
      }

      this.RecalcProperties();
   }

   public int RemoveTileObject(IsoObject var1) {
      IsoRegions.setPreviousFlags(this);
      int var2 = this.Objects.indexOf(var1);
      if (!this.Objects.contains(var1)) {
         var2 = this.SpecialObjects.indexOf(var1);
      }

      if (var1 != null && this.Objects.contains(var1)) {
         if (var1.isTableSurface()) {
            for(int var3 = this.Objects.indexOf(var1) + 1; var3 < this.Objects.size(); ++var3) {
               IsoObject var4 = (IsoObject)this.Objects.get(var3);
               if (var4.isTableTopObject() || var4.isTableSurface()) {
                  var4.setRenderYOffset(var4.getRenderYOffset() - var1.getSurfaceOffset());
                  var4.sx = 0.0F;
                  var4.sy = 0.0F;
               }
            }
         }

         IsoObject var5 = this.getPlayerBuiltFloor();
         if (var1 == var5) {
            IsoGridOcclusionData.SquareChanged();
         }

         LuaEventManager.triggerEvent("OnObjectAboutToBeRemoved", var1);
         if (!this.Objects.contains(var1)) {
            throw new IllegalArgumentException("OnObjectAboutToBeRemoved not allowed to remove the object");
         }

         var2 = this.Objects.indexOf(var1);
         if (var1 instanceof IsoWorldInventoryObject) {
            var1.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_ITEM_REMOVE | FBORenderChunk.DIRTY_OBJECT_REMOVE);
         } else {
            var1.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_REMOVE);
         }

         var1.removeFromWorld();
         var1.removeFromSquare();

         assert !this.Objects.contains(var1);

         assert !this.SpecialObjects.contains(var1);

         if (!(var1 instanceof IsoWorldInventoryObject)) {
            this.RecalcAllWithNeighbours(true);
            this.getCell().checkHaveRoof(this.getX(), this.getY());

            for(int var6 = 0; var6 < IsoPlayer.numPlayers; ++var6) {
               LosUtil.cachecleared[var6] = true;
            }

            setRecalcLightTime(-1.0F);
            if (PerformanceSettings.FBORenderChunk) {
               ++Core.dirtyGlobalLightsCount;
            }

            GameTime.instance.lightSourceUpdate = 100.0F;
         }

         this.fixPlacedItemRenderOffsets();
      }

      MapCollisionData.instance.squareChanged(this);
      LuaEventManager.triggerEvent("OnTileRemoved", var1);
      PolygonalMap2.instance.squareChanged(this);
      IsoRegions.squareChanged(this, true);
      return var2;
   }

   public int RemoveTileObjectErosionNoRecalc(IsoObject var1) {
      int var2 = this.Objects.indexOf(var1);
      IsoGridSquare var3 = var1.square;
      var1.removeFromWorld();
      var1.removeFromSquare();
      var3.RecalcPropertiesIfNeeded();

      assert !this.Objects.contains(var1);

      assert !this.SpecialObjects.contains(var1);

      return var2;
   }

   public void AddSpecialObject(IsoObject var1) {
      this.AddSpecialObject(var1, -1);
   }

   public void AddSpecialObject(IsoObject var1, int var2) {
      if (var1 != null) {
         IsoRegions.setPreviousFlags(this);
         var2 = this.placeWallAndDoorCheck(var1, var2);
         if (var2 != -1 && var2 >= 0 && var2 <= this.Objects.size()) {
            this.Objects.add(var2, var1);
         } else {
            this.Objects.add(var1);
         }

         this.SpecialObjects.add(var1);
         this.burntOut = false;
         var1.addToWorld();
         if (!GameServer.bServer && !GameClient.bClient) {
            this.restackSheetRope();
         }

         this.RecalcAllWithNeighbours(true);
         this.fixPlacedItemRenderOffsets();
         if (!(var1 instanceof IsoWorldInventoryObject)) {
            for(int var3 = 0; var3 < IsoPlayer.numPlayers; ++var3) {
               LosUtil.cachecleared[var3] = true;
            }

            setRecalcLightTime(-1.0F);
            if (PerformanceSettings.FBORenderChunk) {
               ++Core.dirtyGlobalLightsCount;
            }

            GameTime.instance.lightSourceUpdate = 100.0F;
            if (var1 == this.getPlayerBuiltFloor()) {
               IsoGridOcclusionData.SquareChanged();
            }
         }

         MapCollisionData.instance.squareChanged(this);
         PolygonalMap2.instance.squareChanged(this);
         IsoRegions.squareChanged(this);
         this.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_ADD);
      }
   }

   public void AddTileObject(IsoObject var1) {
      this.AddTileObject(var1, -1);
   }

   public void AddTileObject(IsoObject var1, int var2) {
      if (var1 != null) {
         IsoRegions.setPreviousFlags(this);
         var2 = this.placeWallAndDoorCheck(var1, var2);
         if (var2 != -1 && var2 >= 0 && var2 <= this.Objects.size()) {
            this.Objects.add(var2, var1);
         } else {
            this.Objects.add(var1);
         }

         this.burntOut = false;
         var1.addToWorld();
         this.RecalcAllWithNeighbours(true);
         this.fixPlacedItemRenderOffsets();
         if (!(var1 instanceof IsoWorldInventoryObject)) {
            for(int var3 = 0; var3 < IsoPlayer.numPlayers; ++var3) {
               LosUtil.cachecleared[var3] = true;
            }

            setRecalcLightTime(-1.0F);
            if (PerformanceSettings.FBORenderChunk) {
               ++Core.dirtyGlobalLightsCount;
            }

            GameTime.instance.lightSourceUpdate = 100.0F;
            if (var1 == this.getPlayerBuiltFloor()) {
               IsoGridOcclusionData.SquareChanged();
            }
         }

         MapCollisionData.instance.squareChanged(this);
         PolygonalMap2.instance.squareChanged(this);
         IsoRegions.squareChanged(this);
         this.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_ADD);
      }
   }

   public int placeWallAndDoorCheck(IsoObject var1, int var2) {
      int var4 = -1;
      if (var1.sprite != null) {
         IsoObjectType var3 = var1.sprite.getType();
         boolean var5 = var3 == IsoObjectType.doorN || var3 == IsoObjectType.doorW;
         boolean var6 = !var5 && (var1.sprite.cutW || var1.sprite.cutN || var3 == IsoObjectType.doorFrN || var3 == IsoObjectType.doorFrW || var1.sprite.treatAsWallOrder);
         if (var6 || var5) {
            int var8 = 0;

            while(true) {
               if (var8 >= this.Objects.size()) {
                  if (var5 && var4 > var2) {
                     var2 = var4 + 1;
                     return var2;
                  }

                  if (var6 && var4 >= 0 && (var4 < var2 || var2 < 0)) {
                     return var4;
                  }
                  break;
               }

               IsoObject var7 = (IsoObject)this.Objects.get(var8);
               var3 = IsoObjectType.MAX;
               if (var7.sprite != null) {
                  var3 = var7.sprite.getType();
                  if (var6 && (var3 == IsoObjectType.doorN || var3 == IsoObjectType.doorW)) {
                     var4 = var8;
                  }

                  if (var5 && (var3 == IsoObjectType.doorFrN || var3 == IsoObjectType.doorFrW || var7.sprite.cutW || var7.sprite.cutN || var7.sprite.treatAsWallOrder)) {
                     var4 = var8;
                  }
               }

               ++var8;
            }
         }
      }

      return var2;
   }

   public void transmitAddObjectToSquare(IsoObject var1, int var2) {
      if (var1 != null && !this.Objects.contains(var1)) {
         this.AddTileObject(var1, var2);
         if (GameClient.bClient) {
            var1.transmitCompleteItemToServer();
         }

         if (GameServer.bServer) {
            var1.transmitCompleteItemToClients();
         }

      }
   }

   public int transmitRemoveItemFromSquare(IsoObject var1) {
      if (var1 != null && this.Objects.contains(var1)) {
         if (GameClient.bClient) {
            try {
               GameClient.instance.checkAddedRemovedItems(var1);
            } catch (Exception var4) {
               GameClient.connection.cancelPacket();
               ExceptionLogger.logException(var4);
            }

            RemoveItemFromSquarePacket var2 = new RemoveItemFromSquarePacket();
            var2.set(var1);
            ByteBufferWriter var3 = GameClient.connection.startPacket();
            PacketTypes.PacketType.RemoveItemFromSquare.doPacket(var3);
            var2.write(var3);
            PacketTypes.PacketType.RemoveItemFromSquare.send(GameClient.connection);
         }

         return GameServer.bServer ? GameServer.RemoveItemFromMap(var1) : this.RemoveTileObject(var1);
      } else {
         return -1;
      }
   }

   public void transmitRemoveItemFromSquareOnClients(IsoObject var1) {
      if (var1 != null && this.Objects.contains(var1)) {
         if (GameServer.bServer) {
            GameServer.RemoveItemFromMap(var1);
         }

      }
   }

   public void transmitModdata() {
      if (GameClient.bClient) {
         ReceiveModDataPacket var1 = new ReceiveModDataPacket();
         var1.set(this);
         ByteBufferWriter var2 = GameClient.connection.startPacket();
         PacketTypes.PacketType.ReceiveModData.doPacket(var2);
         var1.write(var2);
         PacketTypes.PacketType.ReceiveModData.send(GameClient.connection);
      } else if (GameServer.bServer) {
         GameServer.loadModData(this);
      }

   }

   public void AddWorldInventoryItem(String var1, float var2, float var3, float var4, int var5) {
      for(int var6 = 0; var6 < var5; ++var6) {
         this.AddWorldInventoryItem(var1, var2, var3, var4);
      }

   }

   public InventoryItem AddWorldInventoryItem(String var1, float var2, float var3, float var4) {
      InventoryItem var5 = InventoryItemFactory.CreateItem(var1);
      if (var5 == null) {
         return null;
      } else {
         IsoWorldInventoryObject var6 = new IsoWorldInventoryObject(var5, this, var2, var3, var4);
         var5.setAutoAge();
         var5.setWorldItem(var6);
         var6.setKeyId(var5.getKeyId());
         var6.setName(var5.getName());
         this.Objects.add(var6);
         this.WorldObjects.add(var6);
         var6.square.chunk.recalcHashCodeObjects();
         if (var6.getRenderSquare() != null) {
            var6.getRenderSquare().invalidateRenderChunkLevel(FBORenderChunk.DIRTY_ITEM_ADD | FBORenderChunk.DIRTY_OBJECT_ADD);
         }

         if (GameClient.bClient) {
            var6.transmitCompleteItemToServer();
         }

         if (GameServer.bServer) {
            var6.transmitCompleteItemToClients();
         }

         return var5;
      }
   }

   public InventoryItem AddWorldInventoryItem(InventoryItem var1, float var2, float var3, float var4) {
      return this.AddWorldInventoryItem(var1, var2, var3, var4, true);
   }

   public IsoDeadBody createAnimalCorpseFromItem(InventoryItem var1) {
      return var1.getFullType().equals("Base.CorpseAnimal") ? var1.loadCorpseFromByteData((IsoGridSquare)null) : null;
   }

   public InventoryItem AddWorldInventoryItem(InventoryItem var1, float var2, float var3, float var4, boolean var5) {
      if (var1.getFullType().contains(".Corpse")) {
         IsoDeadBody var11 = var1.loadCorpseFromByteData((IsoGridSquare)null);
         var11.setX((float)this.x + var2);
         var11.setY((float)this.y + var3);
         var11.setZ(this.getApparentZ(var2, var3));
         var11.square = this;
         this.addCorpse(var11, false);
         if (GameServer.bServer) {
            GameServer.sendCorpse(var11);
         }

         return var1;
      } else {
         this.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_ITEM_ADD | FBORenderChunk.DIRTY_OBJECT_ADD);
         if (var1.getFullType().contains(".Generator") || var1.hasTag("Generator") && var1.getWorldObjectSprite() != null) {
            new IsoGenerator(var1, IsoWorld.instance.CurrentCell, this);
            return var1;
         } else if (var1 instanceof AnimalInventoryItem) {
            AnimalInventoryItem var10 = (AnimalInventoryItem)var1;
            IsoAnimal var7 = new IsoAnimal(IsoWorld.instance.getCell(), this.x, this.y, this.z, var10.getAnimal().getAnimalType(), var10.getAnimal().getBreed());
            var7.copyFrom(var10.getAnimal());
            AnimalInstanceManager.getInstance().add(var7, var10.getAnimal().getOnlineID());
            var7.addToWorld();
            var7.attachBackToMotherTimer = 10000.0F;
            var7.setSquare(this);
            var7.playBreedSound("put_down");
            AnimalSoundState var8 = var7.getAnimalSoundState("voice");
            if (var8 != null && var7.getBreed() != null) {
               AnimalBreed.Sound var9 = var7.getBreed().getSound("idle");
               if (var9 != null) {
                  var8.setIntervalExpireTime(var9.soundName, System.currentTimeMillis() + (long)Rand.Next(var9.intervalMin, var9.intervalMax) * 1000L);
               }

               var9 = var7.getBreed().getSound("stressed");
               if (var9 != null) {
                  var8.setIntervalExpireTime(var9.soundName, System.currentTimeMillis() + (long)Rand.Next(var9.intervalMin, var9.intervalMax) * 1000L);
               }
            }

            if (var5 && GameServer.bServer) {
               INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalCommand, (float)this.getX(), (float)this.getY(), AnimalCommandPacket.Type.DropAnimal, var7, this);
            }

            return var10;
         } else {
            IsoWorldInventoryObject var6 = new IsoWorldInventoryObject(var1, this, var2, var3, var4);
            var6.setName(var1.getName());
            var6.setKeyId(var1.getKeyId());
            this.Objects.add(var6);
            this.WorldObjects.add(var6);
            var1.setWorldItem(var6);
            var6.addToWorld();
            DesignationZoneAnimal.addFoodOnGround(var6, this);
            if (var6.getRenderSquare() != null) {
               var6.getRenderSquare().invalidateRenderChunkLevel(FBORenderChunk.DIRTY_ITEM_ADD | FBORenderChunk.DIRTY_OBJECT_ADD);
            }

            if (var5) {
               if (GameClient.bClient) {
                  var6.transmitCompleteItemToServer();
               }

               if (GameServer.bServer) {
                  var6.transmitCompleteItemToClients();
               }
            }

            return var1;
         }
      }
   }

   public void restackSheetRope() {
      if (this.Is(IsoFlagType.climbSheetW) || this.Is(IsoFlagType.climbSheetN) || this.Is(IsoFlagType.climbSheetE) || this.Is(IsoFlagType.climbSheetS)) {
         for(int var1 = 0; var1 < this.getObjects().size() - 1; ++var1) {
            IsoObject var2 = (IsoObject)this.getObjects().get(var1);
            if (var2.getProperties() != null && (var2.getProperties().Is(IsoFlagType.climbSheetW) || var2.getProperties().Is(IsoFlagType.climbSheetN) || var2.getProperties().Is(IsoFlagType.climbSheetE) || var2.getProperties().Is(IsoFlagType.climbSheetS))) {
               if (GameServer.bServer) {
                  this.transmitRemoveItemFromSquare(var2);
                  this.Objects.add(var2);
                  var2.transmitCompleteItemToClients();
               } else if (!GameClient.bClient) {
                  this.Objects.remove(var2);
                  this.Objects.add(var2);
               }
               break;
            }
         }

      }
   }

   public void Burn() {
      if (!GameServer.bServer && !GameClient.bClient || !ServerOptions.instance.NoFire.getValue()) {
         if (this.getCell() != null) {
            this.BurnWalls(true);
            LuaEventManager.triggerEvent("OnGridBurnt", this);
         }
      }
   }

   public void Burn(boolean var1) {
      if (!GameServer.bServer && !GameClient.bClient || !ServerOptions.instance.NoFire.getValue()) {
         if (this.getCell() != null) {
            this.BurnWalls(var1);
         }
      }
   }

   public void BurnWalls(boolean var1) {
      if (!GameClient.bClient) {
         if (GameServer.bServer && SafeHouse.isSafeHouse(this, (String)null, false) != null) {
            if (ServerOptions.instance.NoFire.getValue()) {
               return;
            }

            if (!ServerOptions.instance.SafehouseAllowFire.getValue()) {
               return;
            }
         }

         for(int var2 = 0; var2 < this.SpecialObjects.size(); ++var2) {
            IsoObject var3 = (IsoObject)this.SpecialObjects.get(var2);
            if (var3 instanceof IsoThumpable && ((IsoThumpable)var3).haveSheetRope()) {
               ((IsoThumpable)var3).removeSheetRope((IsoPlayer)null);
            }

            if (var3 instanceof IsoWindow) {
               if (((IsoWindow)var3).haveSheetRope()) {
                  ((IsoWindow)var3).removeSheetRope((IsoPlayer)null);
               }

               ((IsoWindow)var3).removeSheet((IsoGameCharacter)null);
            }

            if (var3 instanceof IsoWindowFrame) {
               IsoWindowFrame var4 = (IsoWindowFrame)var3;
               if (var4.haveSheetRope()) {
                  var4.removeSheetRope((IsoPlayer)null);
               }
            }

            if (var3 instanceof BarricadeAble) {
               IsoBarricade var12 = ((BarricadeAble)var3).getBarricadeOnSameSquare();
               IsoBarricade var5 = ((BarricadeAble)var3).getBarricadeOnOppositeSquare();
               if (var12 != null) {
                  if (GameServer.bServer) {
                     GameServer.RemoveItemFromMap(var12);
                  } else {
                     this.RemoveTileObject(var12);
                  }
               }

               if (var5 != null) {
                  if (GameServer.bServer) {
                     GameServer.RemoveItemFromMap(var5);
                  } else {
                     var5.getSquare().RemoveTileObject(var5);
                  }
               }
            }
         }

         this.SpecialObjects.clear();
         boolean var10 = false;
         if (!this.getProperties().Is(IsoFlagType.burntOut)) {
            int var11 = 0;

            for(int var13 = 0; var13 < this.Objects.size(); ++var13) {
               IsoObject var14 = (IsoObject)this.Objects.get(var13);
               boolean var6 = false;
               if (var14.getSprite() != null && var14.getSprite().getName() != null && !var14.getSprite().getProperties().Is(IsoFlagType.water) && !var14.getSprite().getName().contains("_burnt_")) {
                  IsoObject var15;
                  if (var14 instanceof IsoThumpable && var14.getSprite().burntTile != null) {
                     var15 = IsoObject.getNew();
                     var15.setSprite(IsoSpriteManager.instance.getSprite(var14.getSprite().burntTile));
                     var15.setSquare(this);
                     if (GameServer.bServer) {
                        var14.sendObjectChange("replaceWith", "object", var15);
                     }

                     var14.removeFromWorld();
                     this.Objects.set(var13, var15);
                  } else if (var14.getSprite().burntTile != null) {
                     var14.sprite = IsoSpriteManager.instance.getSprite(var14.getSprite().burntTile);
                     var14.RemoveAttachedAnims();
                     if (var14.Children != null) {
                        var14.Children.clear();
                     }

                     var14.transmitUpdatedSpriteToClients();
                     var14.setOverlaySprite((String)null);
                  } else {
                     IsoSpriteManager var10001;
                     if (var14.getType() == IsoObjectType.tree) {
                        var10001 = IsoSpriteManager.instance;
                        int var17 = Rand.Next(15, 19);
                        var14.sprite = var10001.getSprite("fencing_burnt_01_" + (var17 + 1));
                        var14.RemoveAttachedAnims();
                        if (var14.Children != null) {
                           var14.Children.clear();
                        }

                        var14.transmitUpdatedSpriteToClients();
                        var14.setOverlaySprite((String)null);
                     } else if (!(var14 instanceof IsoTrap)) {
                        if (!(var14 instanceof IsoBarricade) && !(var14 instanceof IsoMannequin)) {
                           if (var14 instanceof IsoGenerator) {
                              IsoGenerator var16 = (IsoGenerator)var14;
                              if (var16.getFuel() > 0.0F) {
                                 var11 += 20;
                              }

                              if (var16.isActivated()) {
                                 var16.activated = false;
                                 var16.setSurroundingElectricity();
                                 if (GameServer.bServer) {
                                    var16.syncIsoObject(false, (byte)0, (UdpConnection)null, (ByteBuffer)null);
                                 }
                              }

                              if (GameServer.bServer) {
                                 GameServer.RemoveItemFromMap(var14);
                              } else {
                                 this.RemoveTileObject(var14);
                              }

                              --var13;
                           } else {
                              if (var14.getType() == IsoObjectType.wall && !var14.getProperties().Is(IsoFlagType.DoorWallW) && !var14.getProperties().Is(IsoFlagType.DoorWallN) && !var14.getProperties().Is("WindowN") && !var14.getProperties().Is(IsoFlagType.WindowW) && !var14.getSprite().getName().startsWith("walls_exterior_roofs_") && !var14.getSprite().getName().startsWith("fencing_") && !var14.getSprite().getName().startsWith("fixtures_railings_")) {
                                 if (var14.getSprite().getProperties().Is(IsoFlagType.collideW) && !var14.getSprite().getProperties().Is(IsoFlagType.collideN)) {
                                    var14.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next(2) == 0 ? "0" : "4"));
                                 } else if (var14.getSprite().getProperties().Is(IsoFlagType.collideN) && !var14.getSprite().getProperties().Is(IsoFlagType.collideW)) {
                                    var14.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next(2) == 0 ? "1" : "5"));
                                 } else if (var14.getSprite().getProperties().Is(IsoFlagType.collideW) && var14.getSprite().getProperties().Is(IsoFlagType.collideN)) {
                                    var14.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next(2) == 0 ? "2" : "6"));
                                 } else if (var14.getProperties().Is(IsoFlagType.WallSE)) {
                                    var14.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next(2) == 0 ? "3" : "7"));
                                 }
                              } else {
                                 if (var14 instanceof IsoDoor || var14 instanceof IsoWindow || var14 instanceof IsoCurtain) {
                                    if (GameServer.bServer) {
                                       GameServer.RemoveItemFromMap(var14);
                                    } else {
                                       this.RemoveTileObject(var14);
                                       var10 = true;
                                    }

                                    --var13;
                                    continue;
                                 }

                                 if (var14.getProperties().Is(IsoFlagType.WindowW)) {
                                    var14.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next(2) == 0 ? "8" : "12"));
                                 } else if (var14.getProperties().Is("WindowN")) {
                                    var14.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next(2) == 0 ? "9" : "13"));
                                 } else if (var14.getProperties().Is(IsoFlagType.DoorWallW)) {
                                    var14.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next(2) == 0 ? "10" : "14"));
                                 } else if (var14.getProperties().Is(IsoFlagType.DoorWallN)) {
                                    var14.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next(2) == 0 ? "11" : "15"));
                                 } else if (var14.getSprite().getProperties().Is(IsoFlagType.solidfloor) && !var14.getSprite().getProperties().Is(IsoFlagType.exterior)) {
                                    var14.sprite = IsoSpriteManager.instance.getSprite("floors_burnt_01_0");
                                 } else {
                                    if (var14 instanceof IsoWaveSignal) {
                                       if (GameServer.bServer) {
                                          GameServer.RemoveItemFromMap(var14);
                                       } else {
                                          this.RemoveTileObject(var14);
                                          var10 = true;
                                       }

                                       --var13;
                                       continue;
                                    }

                                    if (var14.getContainer() != null && var14.getContainer().getItems() != null) {
                                       InventoryItem var7 = null;

                                       int var8;
                                       for(var8 = 0; var8 < var14.getContainer().getItems().size(); ++var8) {
                                          var7 = (InventoryItem)var14.getContainer().getItems().get(var8);
                                          if (var7 instanceof Food && ((Food)var7).isAlcoholic() || var7.getType().equals("PetrolCan") || var7.getType().equals("Bleach")) {
                                             var11 += 20;
                                             if (var11 > 100) {
                                                var11 = 100;
                                                break;
                                             }
                                          }
                                       }

                                       var14.sprite = IsoSpriteManager.instance.getSprite("floors_burnt_01_" + Rand.Next(1, 2));

                                       for(var8 = 0; var8 < var14.getContainerCount(); ++var8) {
                                          ItemContainer var9 = var14.getContainerByIndex(var8);
                                          var9.removeItemsFromProcessItems();
                                          var9.removeAllItems();
                                       }

                                       var14.removeAllContainers();
                                       if (var14.getOverlaySprite() != null) {
                                          var14.setOverlaySprite((String)null);
                                       }

                                       var6 = true;
                                    } else if (!var14.getSprite().getProperties().Is(IsoFlagType.solidtrans) && !var14.getSprite().getProperties().Is(IsoFlagType.bed) && !var14.getSprite().getProperties().Is(IsoFlagType.waterPiped)) {
                                       String var10002;
                                       if (var14.getSprite().getName().startsWith("walls_exterior_roofs_")) {
                                          var10001 = IsoSpriteManager.instance;
                                          var10002 = var14.getSprite().getName();
                                          var14.sprite = var10001.getSprite("walls_burnt_roofs_01_" + var10002.substring(var14.getSprite().getName().lastIndexOf("_") + 1, var14.getSprite().getName().length()));
                                       } else if (!var14.getSprite().getName().startsWith("roofs_accents")) {
                                          if (var14.getSprite().getName().startsWith("roofs_")) {
                                             var10001 = IsoSpriteManager.instance;
                                             var10002 = var14.getSprite().getName();
                                             var14.sprite = var10001.getSprite("roofs_burnt_01_" + var10002.substring(var14.getSprite().getName().lastIndexOf("_") + 1, var14.getSprite().getName().length()));
                                          } else if ((var14.getSprite().getName().startsWith("fencing_") || var14.getSprite().getName().startsWith("fixtures_railings_")) && (var14.getSprite().getProperties().Is(IsoFlagType.HoppableN) || var14.getSprite().getProperties().Is(IsoFlagType.HoppableW))) {
                                             if (var14.getSprite().getProperties().Is(IsoFlagType.transparentW) && !var14.getSprite().getProperties().Is(IsoFlagType.transparentN)) {
                                                var14.sprite = IsoSpriteManager.instance.getSprite("fencing_burnt_01_0");
                                             } else if (var14.getSprite().getProperties().Is(IsoFlagType.transparentN) && !var14.getSprite().getProperties().Is(IsoFlagType.transparentW)) {
                                                var14.sprite = IsoSpriteManager.instance.getSprite("fencing_burnt_01_1");
                                             } else {
                                                var14.sprite = IsoSpriteManager.instance.getSprite("fencing_burnt_01_2");
                                             }
                                          }
                                       }
                                    } else {
                                       var14.sprite = IsoSpriteManager.instance.getSprite("floors_burnt_01_" + Rand.Next(1, 2));
                                       if (var14.getOverlaySprite() != null) {
                                          var14.setOverlaySprite((String)null);
                                       }
                                    }
                                 }
                              }

                              if (!var6 && !(var14 instanceof IsoThumpable)) {
                                 var14.RemoveAttachedAnims();
                                 var14.transmitUpdatedSpriteToClients();
                                 var14.setOverlaySprite((String)null);
                              } else {
                                 var15 = IsoObject.getNew();
                                 var15.setSprite(var14.getSprite());
                                 var15.setSquare(this);
                                 if (GameServer.bServer) {
                                    var14.sendObjectChange("replaceWith", "object", var15);
                                 }

                                 this.Objects.set(var13, var15);
                              }

                              if (var14.emitter != null) {
                                 var14.emitter.stopAll();
                                 var14.emitter = null;
                              }
                           }
                        } else {
                           if (GameServer.bServer) {
                              GameServer.RemoveItemFromMap(var14);
                           } else {
                              this.Objects.remove(var14);
                           }

                           --var13;
                        }
                     }
                  }
               }
            }

            if (var11 > 0 && var1) {
               if (GameServer.bServer) {
                  GameServer.PlayWorldSoundServer("BurnedObjectExploded", false, this, 0.0F, 50.0F, 1.0F, false);
               } else {
                  SoundManager.instance.PlayWorldSound("BurnedObjectExploded", this, 0.0F, 50.0F, 1.0F, false);
               }

               IsoFireManager.explode(this.getCell(), this, var11);
            }
         }

         if (!var10) {
            this.RecalcProperties();
         }

         this.getProperties().Set(IsoFlagType.burntOut);
         this.burntOut = true;
         MapCollisionData.instance.squareChanged(this);
         PolygonalMap2.instance.squareChanged(this);
         this.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_MODIFY | FBORenderChunk.DIRTY_OBJECT_REMOVE);
      }
   }

   public void BurnWallsTCOnly() {
      for(int var1 = 0; var1 < this.Objects.size(); ++var1) {
         IsoObject var2 = (IsoObject)this.Objects.get(var1);
         if (var2.sprite == null) {
         }
      }

   }

   public void BurnTick() {
      if (!GameClient.bClient) {
         for(int var1 = 0; var1 < this.StaticMovingObjects.size(); ++var1) {
            IsoMovingObject var2 = (IsoMovingObject)this.StaticMovingObjects.get(var1);
            if (var2 instanceof IsoDeadBody) {
               ((IsoDeadBody)var2).Burn();
               if (!this.StaticMovingObjects.contains(var2)) {
                  --var1;
               }
            }
         }

      }
   }

   public boolean CalculateCollide(IsoGridSquare var1, boolean var2, boolean var3, boolean var4) {
      return this.CalculateCollide(var1, var2, var3, var4, false);
   }

   public boolean CalculateCollide(IsoGridSquare var1, boolean var2, boolean var3, boolean var4, boolean var5) {
      return this.CalculateCollide(var1, var2, var3, var4, var5, cellGetSquare);
   }

   public boolean CalculateCollide(IsoGridSquare var1, boolean var2, boolean var3, boolean var4, boolean var5, GetSquare var6) {
      if (var1 == null && var3) {
         return true;
      } else if (var1 == null) {
         return false;
      } else {
         if (var2 && var1.Properties.Is(IsoFlagType.trans)) {
         }

         boolean var7 = false;
         boolean var8 = false;
         boolean var9 = false;
         boolean var10 = false;
         if (var1.x < this.x) {
            var7 = true;
         }

         if (var1.y < this.y) {
            var9 = true;
         }

         if (var1.x > this.x) {
            var8 = true;
         }

         if (var1.y > this.y) {
            var10 = true;
         }

         if (!var5 && var1.Properties.Is(IsoFlagType.solid)) {
            if (this.Has(IsoObjectType.stairsTW) && !var3 && var1.x < this.x && var1.y == this.y && var1.z == this.z) {
               return false;
            } else {
               return !this.Has(IsoObjectType.stairsTN) || var3 || var1.x != this.x || var1.y >= this.y || var1.z != this.z;
            }
         } else {
            boolean var19;
            if (!var4 && var1.Properties.Is(IsoFlagType.solidtrans)) {
               if (this.Has(IsoObjectType.stairsTW) && !var3 && var1.x < this.x && var1.y == this.y && var1.z == this.z) {
                  return false;
               }

               if (this.Has(IsoObjectType.stairsTN) && !var3 && var1.x == this.x && var1.y < this.y && var1.z == this.z) {
                  return false;
               }

               boolean var11 = false;
               if (var1.Properties.Is(IsoFlagType.HoppableN) || var1.Properties.Is(IsoFlagType.HoppableW)) {
                  var11 = true;
               }

               IsoGridSquare var12;
               if (!var11) {
                  var12 = var6.getGridSquare(var1.x, var1.y + 1, this.z);
                  if (var12 != null && (var12.Is(IsoFlagType.HoppableN) || var12.Is(IsoFlagType.HoppableW))) {
                     var11 = true;
                  }
               }

               if (!var11) {
                  var12 = var6.getGridSquare(var1.x + 1, var1.y, this.z);
                  if (var12 != null && (var12.Is(IsoFlagType.HoppableN) || var12.Is(IsoFlagType.HoppableW))) {
                     var11 = true;
                  }
               }

               var19 = false;
               if (var1.Properties.Is(IsoFlagType.windowW) || var1.Properties.Is(IsoFlagType.windowN)) {
                  var19 = true;
               }

               if (!var19 && (var1.Properties.Is(IsoFlagType.WindowW) || var1.Properties.Is(IsoFlagType.WindowN))) {
                  var19 = true;
               }

               IsoGridSquare var13;
               if (!var19) {
                  var13 = var6.getGridSquare(var1.x, var1.y + 1, this.z);
                  if (var13 != null && (var13.Is(IsoFlagType.windowN) || var13.Is(IsoFlagType.WindowN))) {
                     var19 = true;
                  }
               }

               if (!var19) {
                  var13 = var6.getGridSquare(var1.x + 1, var1.y, this.z);
                  if (var13 != null && (var13.Is(IsoFlagType.windowW) || var13.Is(IsoFlagType.WindowW))) {
                     var19 = true;
                  }
               }

               if (!var19 && !var11) {
                  return true;
               }
            }

            if (var1.x != this.x && var1.y != this.y && this.z != var1.z && var3) {
               return true;
            } else {
               if (var3 && var1.z < this.z) {
                  label890: {
                     if (this.SolidFloorCached) {
                        if (this.SolidFloor) {
                           break label890;
                        }
                     } else if (this.TreatAsSolidFloor()) {
                        break label890;
                     }

                     if (!var1.Has(IsoObjectType.stairsTN) && !var1.Has(IsoObjectType.stairsTW)) {
                        return false;
                     }

                     return true;
                  }
               }

               if (var3 && var1.z == this.z) {
                  if (var1.x > this.x && var1.y == this.y && var1.Properties.Is(IsoFlagType.windowW)) {
                     return false;
                  }

                  if (var1.y > this.y && var1.x == this.x && var1.Properties.Is(IsoFlagType.windowN)) {
                     return false;
                  }

                  if (var1.x < this.x && var1.y == this.y && this.Properties.Is(IsoFlagType.windowW)) {
                     return false;
                  }

                  if (var1.y < this.y && var1.x == this.x && this.Properties.Is(IsoFlagType.windowN)) {
                     return false;
                  }
               }

               if (var1.x > this.x && var1.z < this.z && var1.Has(IsoObjectType.stairsTW)) {
                  return false;
               } else if (var1.y > this.y && var1.z < this.z && var1.Has(IsoObjectType.stairsTN)) {
                  return false;
               } else {
                  IsoGridSquare var20 = var6.getGridSquare(var1.x, var1.y, var1.z - 1);
                  if (var1.x != this.x && var1.z == this.z && var1.Has(IsoObjectType.stairsTN) && (var20 == null || !var20.Has(IsoObjectType.stairsTN) || var3)) {
                     return true;
                  } else if (var1.y > this.y && var1.x == this.x && var1.z == this.z && var1.Has(IsoObjectType.stairsTN) && (var20 == null || !var20.Has(IsoObjectType.stairsTN) || var3)) {
                     return true;
                  } else if (var1.x > this.x && var1.y == this.y && var1.z == this.z && var1.Has(IsoObjectType.stairsTW) && (var20 == null || !var20.Has(IsoObjectType.stairsTW) || var3)) {
                     return true;
                  } else if (var1.y == this.y || var1.z != this.z || !var1.Has(IsoObjectType.stairsTW) || var20 != null && var20.Has(IsoObjectType.stairsTW) && !var3) {
                     if (var1.x != this.x && var1.z == this.z && var1.Has(IsoObjectType.stairsMN)) {
                        return true;
                     } else if (var1.y != this.y && var1.z == this.z && var1.Has(IsoObjectType.stairsMW)) {
                        return true;
                     } else if (var1.x != this.x && var1.z == this.z && var1.Has(IsoObjectType.stairsBN)) {
                        return true;
                     } else if (var1.y != this.y && var1.z == this.z && var1.Has(IsoObjectType.stairsBW)) {
                        return true;
                     } else if (var1.x != this.x && var1.z == this.z && this.Has(IsoObjectType.stairsTN)) {
                        return true;
                     } else if (var1.y != this.y && var1.z == this.z && this.Has(IsoObjectType.stairsTW)) {
                        return true;
                     } else if (var1.x != this.x && var1.z == this.z && this.Has(IsoObjectType.stairsMN)) {
                        return true;
                     } else if (var1.y != this.y && var1.z == this.z && this.Has(IsoObjectType.stairsMW)) {
                        return true;
                     } else if (var1.x != this.x && var1.z == this.z && this.Has(IsoObjectType.stairsBN)) {
                        return true;
                     } else if (var1.y != this.y && var1.z == this.z && this.Has(IsoObjectType.stairsBW)) {
                        return true;
                     } else if (var1.y < this.y && var1.x == this.x && var1.z > this.z && this.Has(IsoObjectType.stairsTN)) {
                        return false;
                     } else if (var1.x < this.x && var1.y == this.y && var1.z > this.z && this.Has(IsoObjectType.stairsTW)) {
                        return false;
                     } else if (var1.y > this.y && var1.x == this.x && var1.z < this.z && var1.Has(IsoObjectType.stairsTN)) {
                        return false;
                     } else if (var1.x > this.x && var1.y == this.y && var1.z < this.z && var1.Has(IsoObjectType.stairsTW)) {
                        return false;
                     } else {
                        if (var1.z == this.z) {
                           if (var1.x == this.x && var1.y == this.y - 1 && !this.hasSlopedSurfaceToLevelAbove(IsoDirections.N) && (this.isSlopedSurfaceEdgeBlocked(IsoDirections.N) || var1.isSlopedSurfaceEdgeBlocked(IsoDirections.S))) {
                              return true;
                           }

                           if (var1.x == this.x && var1.y == this.y + 1 && !this.hasSlopedSurfaceToLevelAbove(IsoDirections.S) && (this.isSlopedSurfaceEdgeBlocked(IsoDirections.S) || var1.isSlopedSurfaceEdgeBlocked(IsoDirections.N))) {
                              return true;
                           }

                           if (var1.x == this.x - 1 && var1.y == this.y && !this.hasSlopedSurfaceToLevelAbove(IsoDirections.W) && (this.isSlopedSurfaceEdgeBlocked(IsoDirections.W) || var1.isSlopedSurfaceEdgeBlocked(IsoDirections.E))) {
                              return true;
                           }

                           if (var1.x == this.x + 1 && var1.y == this.y && !this.hasSlopedSurfaceToLevelAbove(IsoDirections.E) && (this.isSlopedSurfaceEdgeBlocked(IsoDirections.E) || var1.isSlopedSurfaceEdgeBlocked(IsoDirections.W))) {
                              return true;
                           }
                        }

                        if (var1.z > this.z) {
                           if (var1.y < this.y && var1.x == this.x && this.hasSlopedSurfaceToLevelAbove(IsoDirections.N)) {
                              return false;
                           }

                           if (var1.y > this.y && var1.x == this.x && this.hasSlopedSurfaceToLevelAbove(IsoDirections.S)) {
                              return false;
                           }

                           if (var1.x < this.x && var1.y == this.y && this.hasSlopedSurfaceToLevelAbove(IsoDirections.W)) {
                              return false;
                           }

                           if (var1.x > this.x && var1.y == this.y && this.hasSlopedSurfaceToLevelAbove(IsoDirections.E)) {
                              return false;
                           }
                        }

                        if (var1.z < this.z) {
                           if (var1.y > this.y && var1.x == this.x && var1.hasSlopedSurfaceToLevelAbove(IsoDirections.N)) {
                              return false;
                           }

                           if (var1.y < this.y && var1.x == this.x && var1.hasSlopedSurfaceToLevelAbove(IsoDirections.S)) {
                              return false;
                           }

                           if (var1.x > this.x && var1.y == this.y && var1.hasSlopedSurfaceToLevelAbove(IsoDirections.W)) {
                              return false;
                           }

                           if (var1.x < this.x && var1.y == this.y && var1.hasSlopedSurfaceToLevelAbove(IsoDirections.E)) {
                              return false;
                           }
                        }

                        if (var1.z == this.z) {
                           label639: {
                              if (var1.SolidFloorCached) {
                                 if (var1.SolidFloor) {
                                    break label639;
                                 }
                              } else if (var1.TreatAsSolidFloor()) {
                                 break label639;
                              }

                              if (var3) {
                                 return true;
                              }
                           }
                        }

                        if (var1.z == this.z) {
                           label632: {
                              if (var1.SolidFloorCached) {
                                 if (var1.SolidFloor) {
                                    break label632;
                                 }
                              } else if (var1.TreatAsSolidFloor()) {
                                 break label632;
                              }

                              if (var1.z > 0) {
                                 var20 = var6.getGridSquare(var1.x, var1.y, var1.z - 1);
                                 if (var20 == null) {
                                    return true;
                                 }
                              }
                           }
                        }

                        if (this.z != var1.z) {
                           if (var1.z < this.z && var1.x == this.x && var1.y == this.y) {
                              if (this.SolidFloorCached) {
                                 if (!this.SolidFloor) {
                                    return false;
                                 }
                              } else if (!this.TreatAsSolidFloor()) {
                                 return false;
                              }
                           }

                           return true;
                        } else {
                           var19 = var9 && this.Properties.Is(IsoFlagType.collideN);
                           boolean var21 = var7 && this.Properties.Is(IsoFlagType.collideW);
                           boolean var14 = var10 && var1.Properties.Is(IsoFlagType.collideN);
                           boolean var15 = var8 && var1.Properties.Is(IsoFlagType.collideW);
                           if (var19 && var3 && this.Properties.Is(IsoFlagType.canPathN)) {
                              var19 = false;
                           }

                           if (var21 && var3 && this.Properties.Is(IsoFlagType.canPathW)) {
                              var21 = false;
                           }

                           if (var14 && var3 && var1.Properties.Is(IsoFlagType.canPathN)) {
                              var14 = false;
                           }

                           if (var15 && var3 && var1.Properties.Is(IsoFlagType.canPathW)) {
                              var15 = false;
                           }

                           if (var21 && this.Has(IsoObjectType.stairsTW) && !var3) {
                              var21 = false;
                           }

                           if (var19 && this.Has(IsoObjectType.stairsTN) && !var3) {
                              var19 = false;
                           }

                           if (!var19 && !var21 && !var14 && !var15) {
                              boolean var16 = var1.x != this.x && var1.y != this.y;
                              if (var16) {
                                 IsoGridSquare var17 = var6.getGridSquare(this.x, var1.y, this.z);
                                 IsoGridSquare var18 = var6.getGridSquare(var1.x, this.y, this.z);
                                 if (var17 != null && var17 != this && var17 != var1) {
                                    var17.RecalcPropertiesIfNeeded();
                                 }

                                 if (var18 != null && var18 != this && var18 != var1) {
                                    var18.RecalcPropertiesIfNeeded();
                                 }

                                 if (var1 == this || var17 == var18 || var17 == this || var18 == this || var17 == var1 || var18 == var1) {
                                    return true;
                                 }

                                 if (var1.x == this.x + 1 && var1.y == this.y + 1 && var17 != null && var18 != null && var17.Is(IsoFlagType.windowN) && var18.Is(IsoFlagType.windowW)) {
                                    return true;
                                 }

                                 if (var1.x == this.x - 1 && var1.y == this.y - 1 && var17 != null && var18 != null && var17.Is(IsoFlagType.windowW) && var18.Is(IsoFlagType.windowN)) {
                                    return true;
                                 }

                                 if (this.CalculateCollide(var17, var2, var3, var4, false, var6)) {
                                    return true;
                                 }

                                 if (this.CalculateCollide(var18, var2, var3, var4, false, var6)) {
                                    return true;
                                 }

                                 if (var1.CalculateCollide(var17, var2, var3, var4, false, var6)) {
                                    return true;
                                 }

                                 if (var1.CalculateCollide(var18, var2, var3, var4, false, var6)) {
                                    return true;
                                 }
                              }

                              return false;
                           } else {
                              return true;
                           }
                        }
                     }
                  } else {
                     return true;
                  }
               }
            }
         }
      }
   }

   public boolean CalculateVisionBlocked(IsoGridSquare var1) {
      return this.CalculateVisionBlocked(var1, cellGetSquare);
   }

   public boolean CalculateVisionBlocked(IsoGridSquare var1, GetSquare var2) {
      if (var1 == null) {
         return false;
      } else if (Math.abs(var1.getX() - this.getX()) <= 1 && Math.abs(var1.getY() - this.getY()) <= 1) {
         boolean var3 = false;
         boolean var4 = false;
         boolean var5 = false;
         boolean var6 = false;
         if (var1.x < this.x) {
            var3 = true;
         }

         if (var1.y < this.y) {
            var5 = true;
         }

         if (var1.x > this.x) {
            var4 = true;
         }

         if (var1.y > this.y) {
            var6 = true;
         }

         if (!var1.Properties.Is(IsoFlagType.trans) && !this.Properties.Is(IsoFlagType.trans)) {
            if (this.z != var1.z) {
               IsoGridSquare var7;
               if (var1.z > this.z) {
                  if (var1.Properties.Is(IsoFlagType.solidfloor) && !var1.getProperties().Is(IsoFlagType.transparentFloor)) {
                     return true;
                  }

                  if (this.Properties.Is(IsoFlagType.noStart)) {
                     return true;
                  }

                  var7 = var2.getGridSquare(this.x, this.y, var1.z);
                  if (var7 == null) {
                     return false;
                  }

                  if (var7.Properties.Is(IsoFlagType.solidfloor) && !var7.getProperties().Is(IsoFlagType.transparentFloor)) {
                     return true;
                  }
               } else {
                  if (this.Properties.Is(IsoFlagType.solidfloor) && !this.getProperties().Is(IsoFlagType.transparentFloor)) {
                     return true;
                  }

                  if (this.Properties.Is(IsoFlagType.noStart)) {
                     return true;
                  }

                  var7 = var2.getGridSquare(var1.x, var1.y, this.z);
                  if (var7 == null) {
                     return false;
                  }

                  if (var7.Properties.Is(IsoFlagType.solidfloor) && !var7.getProperties().Is(IsoFlagType.transparentFloor)) {
                     return true;
                  }
               }
            }

            boolean var14 = var5 && this.Properties.Is(IsoFlagType.collideN) && !this.Properties.Is(IsoFlagType.transparentN) && !this.Properties.Is(IsoFlagType.doorN);
            boolean var8 = var3 && this.Properties.Is(IsoFlagType.collideW) && !this.Properties.Is(IsoFlagType.transparentW) && !this.Properties.Is(IsoFlagType.doorW);
            boolean var9 = var6 && var1.Properties.Is(IsoFlagType.collideN) && !var1.Properties.Is(IsoFlagType.transparentN) && !var1.Properties.Is(IsoFlagType.doorN);
            boolean var10 = var4 && var1.Properties.Is(IsoFlagType.collideW) && !var1.Properties.Is(IsoFlagType.transparentW) && !var1.Properties.Is(IsoFlagType.doorW);
            if (!var14 && !var8 && !var9 && !var10) {
               boolean var11 = var1.x != this.x && var1.y != this.y;
               if (!var1.Properties.Is(IsoFlagType.solid) && !var1.Properties.Is(IsoFlagType.blocksight)) {
                  if (var11) {
                     IsoGridSquare var12 = var2.getGridSquare(this.x, var1.y, this.z);
                     IsoGridSquare var13 = var2.getGridSquare(var1.x, this.y, this.z);
                     if (var12 != null && var12 != this && var12 != var1) {
                        var12.RecalcPropertiesIfNeeded();
                     }

                     if (var13 != null && var13 != this && var13 != var1) {
                        var13.RecalcPropertiesIfNeeded();
                     }

                     if (this.CalculateVisionBlocked(var12, var2)) {
                        return true;
                     }

                     if (this.CalculateVisionBlocked(var13, var2)) {
                        return true;
                     }

                     if (var1.CalculateVisionBlocked(var12, var2)) {
                        return true;
                     }

                     if (var1.CalculateVisionBlocked(var13, var2)) {
                        return true;
                     }
                  }

                  return false;
               } else {
                  return true;
               }
            } else {
               return true;
            }
         } else {
            return false;
         }
      } else {
         return true;
      }
   }

   public IsoGameCharacter FindFriend(IsoGameCharacter var1, int var2, Stack<IsoGameCharacter> var3) {
      Stack var4 = new Stack();

      for(int var5 = 0; var5 < var1.getLocalList().size(); ++var5) {
         IsoMovingObject var6 = (IsoMovingObject)var1.getLocalList().get(var5);
         if (var6 != var1 && var6 != var1.getFollowingTarget() && var6 instanceof IsoGameCharacter && !(var6 instanceof IsoZombie) && !var3.contains(var6)) {
            var4.add((IsoGameCharacter)var6);
         }
      }

      float var10 = 1000000.0F;
      IsoGameCharacter var11 = null;
      Iterator var7 = var4.iterator();

      while(var7.hasNext()) {
         IsoGameCharacter var8 = (IsoGameCharacter)var7.next();
         float var9 = 0.0F;
         var9 += Math.abs((float)this.getX() - var8.getX());
         var9 += Math.abs((float)this.getY() - var8.getY());
         var9 += Math.abs((float)this.getZ() - var8.getZ());
         if (var9 < var10) {
            var11 = var8;
            var10 = var9;
         }

         if (var8 == IsoPlayer.getInstance()) {
            var11 = var8;
            var9 = 0.0F;
         }
      }

      if (var10 > (float)var2) {
         return null;
      } else {
         return var11;
      }
   }

   public IsoGameCharacter FindEnemy(IsoGameCharacter var1, int var2, ArrayList<IsoMovingObject> var3, IsoGameCharacter var4, int var5) {
      float var6 = 1000000.0F;
      IsoGameCharacter var7 = null;

      for(int var8 = 0; var8 < var3.size(); ++var8) {
         IsoGameCharacter var9 = (IsoGameCharacter)var3.get(var8);
         float var10 = 0.0F;
         var10 += Math.abs((float)this.getX() - var9.getX());
         var10 += Math.abs((float)this.getY() - var9.getY());
         var10 += Math.abs((float)this.getZ() - var9.getZ());
         if (var10 < (float)var2 && var10 < var6 && var9.DistTo(var4) < (float)var5) {
            var7 = var9;
            var6 = var10;
         }
      }

      if (var6 > (float)var2) {
         return null;
      } else {
         return var7;
      }
   }

   public IsoGameCharacter FindEnemy(IsoGameCharacter var1, int var2, ArrayList<IsoMovingObject> var3) {
      float var4 = 1000000.0F;
      IsoGameCharacter var5 = null;

      for(int var6 = 0; var6 < var3.size(); ++var6) {
         IsoGameCharacter var7 = (IsoGameCharacter)var3.get(var6);
         float var8 = 0.0F;
         var8 += Math.abs((float)this.getX() - var7.getX());
         var8 += Math.abs((float)this.getY() - var7.getY());
         var8 += Math.abs((float)this.getZ() - var7.getZ());
         if (var8 < var4) {
            var5 = var7;
            var4 = var8;
         }
      }

      if (var4 > (float)var2) {
         return null;
      } else {
         return var5;
      }
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

   public void RecalcProperties() {
      this.CachedIsFree = false;
      String var1 = null;
      if (this.Properties.Is("waterAmount")) {
         var1 = this.Properties.Val("waterAmount");
      }

      String var2 = null;
      if (this.Properties.Is("fuelAmount")) {
         var2 = this.Properties.Val("fuelAmount");
      }

      if (this.zone == null) {
         this.zone = IsoWorld.instance.MetaGrid.getZoneAt(this.x, this.y, this.z);
      }

      this.Properties.Clear();
      this.hasTypes.clear();
      this.hasTree = false;
      boolean var3 = false;
      boolean var4 = false;
      boolean var5 = false;
      boolean var6 = false;
      boolean var7 = false;
      boolean var8 = false;
      boolean var9 = false;
      boolean var10 = false;
      int var11 = this.Objects.size();
      IsoObject[] var12 = (IsoObject[])this.Objects.getElements();

      for(int var13 = 0; var13 < var11; ++var13) {
         IsoObject var14 = var12[var13];
         if (var14 != null) {
            PropertyContainer var15 = var14.getProperties();
            if (var15 != null && !var15.Is(IsoFlagType.blueprint)) {
               if (var14.sprite.forceRender) {
                  var10 = true;
               }

               if (var14.getType() == IsoObjectType.tree) {
                  this.hasTree = true;
               }

               this.hasTypes.set(var14.getType(), true);
               this.Properties.AddProperties(var15);
               if (var15.Is(IsoFlagType.water)) {
                  var4 = false;
               } else {
                  if (!var4 && var15.Is(IsoFlagType.solidfloor)) {
                     var4 = true;
                  }

                  if (!var3 && var15.Is(IsoFlagType.solidtrans)) {
                     var3 = true;
                  }

                  if (!var5 && var15.Is(IsoFlagType.solidfloor) && !var15.Is(IsoFlagType.transparentFloor)) {
                     var5 = true;
                  }
               }

               if (!var6 && var15.Is(IsoFlagType.collideN) && !var15.Is(IsoFlagType.HoppableN)) {
                  var6 = true;
               }

               if (!var7 && var15.Is(IsoFlagType.collideW) && !var15.Is(IsoFlagType.HoppableW)) {
                  var7 = true;
               }

               if (!var8 && var15.Is(IsoFlagType.cutN) && !var15.Is(IsoFlagType.transparentN) && !var15.Is(IsoFlagType.WallSE)) {
                  var8 = true;
               }

               if (!var9 && var15.Is(IsoFlagType.cutW) && !var15.Is(IsoFlagType.transparentW) && !var15.Is(IsoFlagType.WallSE)) {
                  var9 = true;
               }
            }
         }
      }

      if (this.roomID == -1L && !this.haveRoof) {
         this.getProperties().Set(IsoFlagType.exterior);

         try {
            this.getPuddles().bRecalc = true;
         } catch (Exception var16) {
            var16.printStackTrace();
         }
      } else {
         this.getProperties().UnSet(IsoFlagType.exterior);

         try {
            this.getPuddles().bRecalc = true;
         } catch (Exception var17) {
            var17.printStackTrace();
         }
      }

      if (var1 != null) {
         this.getProperties().Set("waterAmount", var1, false);
      }

      if (var2 != null) {
         this.getProperties().Set("fuelAmount", var2, false);
      }

      if (this.RainDrop != null) {
         this.Properties.Set(IsoFlagType.HasRaindrop);
      }

      if (var10) {
         this.Properties.Set(IsoFlagType.forceRender);
      }

      if (this.RainSplash != null) {
         this.Properties.Set(IsoFlagType.HasRainSplashes);
      }

      if (this.burntOut) {
         this.Properties.Set(IsoFlagType.burntOut);
      }

      if (!var3 && var4 && this.Properties.Is(IsoFlagType.water)) {
         this.Properties.UnSet(IsoFlagType.solidtrans);
      }

      if (var5 && this.Properties.Is(IsoFlagType.transparentFloor)) {
         this.Properties.UnSet(IsoFlagType.transparentFloor);
      }

      if (var6 && this.Properties.Is(IsoFlagType.HoppableN)) {
         this.Properties.UnSet(IsoFlagType.canPathN);
         this.Properties.UnSet(IsoFlagType.HoppableN);
      }

      if (var7 && this.Properties.Is(IsoFlagType.HoppableW)) {
         this.Properties.UnSet(IsoFlagType.canPathW);
         this.Properties.UnSet(IsoFlagType.HoppableW);
      }

      if (var8 && this.Properties.Is(IsoFlagType.transparentN)) {
         this.Properties.UnSet(IsoFlagType.transparentN);
      }

      if (var9 && this.Properties.Is(IsoFlagType.transparentW)) {
         this.Properties.UnSet(IsoFlagType.transparentW);
      }

      this.propertiesDirty = this.chunk == null || this.chunk.bLoaded;
      if (this.chunk != null) {
         this.chunk.checkLightingLater_AllPlayers_OneLevel(this.z);
      }

      if (this.chunk != null) {
         this.chunk.checkPhysicsLater(this.z);
         this.chunk.collision.clear();
      }

      this.isExteriorCache = this.Is(IsoFlagType.exterior);
      this.isSolidFloorCache = this.Is(IsoFlagType.solidfloor);
      this.isVegitationCache = this.Is(IsoFlagType.vegitation);
   }

   public void RecalcPropertiesIfNeeded() {
      if (this.propertiesDirty) {
         this.RecalcProperties();
      }

   }

   public void ReCalculateCollide(IsoGridSquare var1) {
      this.ReCalculateCollide(var1, cellGetSquare);
   }

   public void ReCalculateCollide(IsoGridSquare var1, GetSquare var2) {
      if (1 + (var1.x - this.x) < 0 || 1 + (var1.y - this.y) < 0 || 1 + (var1.z - this.z) < 0) {
         DebugLog.log("ERROR");
      }

      boolean var3 = this.CalculateCollide(var1, false, false, false, false, var2);
      this.collideMatrix = setMatrixBit(this.collideMatrix, 1 + (var1.x - this.x), 1 + (var1.y - this.y), 1 + (var1.z - this.z), var3);
   }

   public void ReCalculatePathFind(IsoGridSquare var1) {
      this.ReCalculatePathFind(var1, cellGetSquare);
   }

   public void ReCalculatePathFind(IsoGridSquare var1, GetSquare var2) {
      boolean var3 = this.CalculateCollide(var1, false, true, false, false, var2);
      this.pathMatrix = setMatrixBit(this.pathMatrix, 1 + (var1.x - this.x), 1 + (var1.y - this.y), 1 + (var1.z - this.z), var3);
   }

   public void ReCalculateVisionBlocked(IsoGridSquare var1) {
      this.ReCalculateVisionBlocked(var1, cellGetSquare);
   }

   public void ReCalculateVisionBlocked(IsoGridSquare var1, GetSquare var2) {
      boolean var3 = this.CalculateVisionBlocked(var1, var2);
      this.visionMatrix = setMatrixBit(this.visionMatrix, 1 + (var1.x - this.x), 1 + (var1.y - this.y), 1 + (var1.z - this.z), var3);
   }

   private static boolean testCollideSpecialObjects(IsoMovingObject var0, IsoGridSquare var1, IsoGridSquare var2) {
      for(int var3 = 0; var3 < var2.SpecialObjects.size(); ++var3) {
         IsoObject var4 = (IsoObject)var2.SpecialObjects.get(var3);
         if (var4.TestCollide(var0, var1, var2)) {
            if (var4 instanceof IsoDoor) {
               var0.setCollidedWithDoor(true);
            } else if (var4 instanceof IsoThumpable && ((IsoThumpable)var4).isDoor) {
               var0.setCollidedWithDoor(true);
            }

            var0.setCollidedObject(var4);
            return true;
         }
      }

      return false;
   }

   public boolean testCollideAdjacent(IsoMovingObject var1, int var2, int var3, int var4) {
      if (var1 instanceof IsoPlayer && ((IsoPlayer)var1).isNoClip()) {
         return false;
      } else if (this.collideMatrix == -1) {
         return true;
      } else if (var2 >= -1 && var2 <= 1 && var3 >= -1 && var3 <= 1 && var4 >= -1 && var4 <= 1) {
         if (!IsoWorld.instance.MetaGrid.isValidChunk((this.x + var2) / 8, (this.y + var3) / 8)) {
            return true;
         } else {
            IsoGridSquare var5 = this.getCell().getGridSquare(this.x + var2, this.y + var3, this.z + var4);
            if (var1 != null && var1.shouldIgnoreCollisionWithSquare(var5)) {
               return false;
            } else {
               if ((GameServer.bServer || GameClient.bClient) && var1 instanceof IsoPlayer) {
                  IsoGridSquare var6 = this.getCell().getGridSquare(this.x + var2, this.y + var3, 0);
                  boolean var7 = SafeHouse.isSafehouseAllowTrepass(var6, (IsoPlayer)var1);
                  if (!var7 && (GameServer.bServer || ((IsoPlayer)var1).isLocalPlayer())) {
                     return true;
                  }
               }

               if (var5 != null && var1 != null) {
                  IsoObject var8 = this.testCollideSpecialObjects(var5);
                  if (var8 != null) {
                     var1.collideWith(var8);
                     if (var8 instanceof IsoDoor) {
                        var1.setCollidedWithDoor(true);
                     } else if (var8 instanceof IsoThumpable && ((IsoThumpable)var8).isDoor) {
                        var1.setCollidedWithDoor(true);
                     }

                     var1.setCollidedObject(var8);
                     return true;
                  }
               }

               if (UseSlowCollision) {
                  return this.CalculateCollide(var5, false, false, false);
               } else {
                  if (var1 instanceof IsoPlayer && getMatrixBit(this.collideMatrix, var2 + 1, var3 + 1, var4 + 1)) {
                     this.RecalcAllWithNeighbours(true);
                  }

                  return getMatrixBit(this.collideMatrix, var2 + 1, var3 + 1, var4 + 1);
               }
            }
         }
      } else {
         return true;
      }
   }

   public boolean testCollideAdjacentAdvanced(int var1, int var2, int var3, boolean var4) {
      if (this.collideMatrix == -1) {
         return true;
      } else if (var1 >= -1 && var1 <= 1 && var2 >= -1 && var2 <= 1 && var3 >= -1 && var3 <= 1) {
         IsoGridSquare var5 = this.getCell().getGridSquare(this.x + var1, this.y + var2, this.z + var3);
         if (var5 != null) {
            int var6;
            IsoObject var7;
            if (!var5.SpecialObjects.isEmpty()) {
               for(var6 = 0; var6 < var5.SpecialObjects.size(); ++var6) {
                  var7 = (IsoObject)var5.SpecialObjects.get(var6);
                  if (var7.TestCollide((IsoMovingObject)null, this, var5)) {
                     return true;
                  }
               }
            }

            if (!this.SpecialObjects.isEmpty()) {
               for(var6 = 0; var6 < this.SpecialObjects.size(); ++var6) {
                  var7 = (IsoObject)this.SpecialObjects.get(var6);
                  if (var7.TestCollide((IsoMovingObject)null, this, var5)) {
                     return true;
                  }
               }
            }
         }

         return UseSlowCollision ? this.CalculateCollide(var5, false, false, false) : getMatrixBit(this.collideMatrix, var1 + 1, var2 + 1, var3 + 1);
      } else {
         return true;
      }
   }

   public static void setCollisionMode() {
      UseSlowCollision = !UseSlowCollision;
   }

   public boolean testPathFindAdjacent(IsoMovingObject var1, int var2, int var3, int var4) {
      return this.testPathFindAdjacent(var1, var2, var3, var4, cellGetSquare);
   }

   public boolean testPathFindAdjacent(IsoMovingObject var1, int var2, int var3, int var4, GetSquare var5) {
      if (var2 >= -1 && var2 <= 1 && var3 >= -1 && var3 <= 1 && var4 >= -1 && var4 <= 1) {
         IsoGridSquare var6;
         if (this.Has(IsoObjectType.stairsTN) || this.Has(IsoObjectType.stairsTW)) {
            var6 = var5.getGridSquare(var2 + this.x, var3 + this.y, var4 + this.z);
            if (var6 == null) {
               return true;
            }

            if (this.Has(IsoObjectType.stairsTN) && var6.y < this.y && var6.z == this.z) {
               return true;
            }

            if (this.Has(IsoObjectType.stairsTW) && var6.x < this.x && var6.z == this.z) {
               return true;
            }
         }

         if (bDoSlowPathfinding) {
            var6 = var5.getGridSquare(var2 + this.x, var3 + this.y, var4 + this.z);
            return this.CalculateCollide(var6, false, true, false, false, var5);
         } else {
            return getMatrixBit(this.pathMatrix, var2 + 1, var3 + 1, var4 + 1);
         }
      } else {
         return true;
      }
   }

   public LosUtil.TestResults testVisionAdjacent(int var1, int var2, int var3, boolean var4, boolean var5) {
      if (var1 >= -1 && var1 <= 1 && var2 >= -1 && var2 <= 1 && var3 >= -1 && var3 <= 1) {
         IsoGridSquare var6;
         if (var3 == 1 && (var1 != 0 || var2 != 0) && this.HasElevatedFloor()) {
            var6 = this.getCell().getGridSquare(this.x, this.y, this.z + var3);
            if (var6 != null) {
               return var6.testVisionAdjacent(var1, var2, 0, var4, var5);
            }
         }

         if (var3 == -1 && (var1 != 0 || var2 != 0)) {
            var6 = this.getCell().getGridSquare(this.x + var1, this.y + var2, this.z + var3);
            if (var6 != null && var6.HasElevatedFloor()) {
               return this.testVisionAdjacent(var1, var2, 0, var4, var5);
            }
         }

         LosUtil.TestResults var12 = LosUtil.TestResults.Clear;
         IsoGridSquare var7;
         if (var1 != 0 && var2 != 0 && var4) {
            var12 = this.DoDiagnalCheck(var1, var2, var3, var5);
            if (var12 == LosUtil.TestResults.Clear || var12 == LosUtil.TestResults.ClearThroughWindow || var12 == LosUtil.TestResults.ClearThroughOpenDoor || var12 == LosUtil.TestResults.ClearThroughClosedDoor) {
               var7 = this.getCell().getGridSquare(this.x + var1, this.y + var2, this.z + var3);
               if (var7 != null) {
                  var12 = var7.DoDiagnalCheck(-var1, -var2, -var3, var5);
               }
            }

            return var12;
         } else {
            var7 = this.getCell().getGridSquare(this.x + var1, this.y + var2, this.z + var3);
            LosUtil.TestResults var8 = LosUtil.TestResults.Clear;
            if (var7 != null && var7.z == this.z) {
               int var9;
               IsoObject var10;
               IsoObject.VisionResult var11;
               if (!this.SpecialObjects.isEmpty()) {
                  for(var9 = 0; var9 < this.SpecialObjects.size(); ++var9) {
                     var10 = (IsoObject)this.SpecialObjects.get(var9);
                     if (var10 == null) {
                        return LosUtil.TestResults.Clear;
                     }

                     var11 = var10.TestVision(this, var7);
                     if (var11 != IsoObject.VisionResult.NoEffect) {
                        if (var11 == IsoObject.VisionResult.Unblocked && var10 instanceof IsoDoor) {
                           var8 = ((IsoDoor)var10).IsOpen() ? LosUtil.TestResults.ClearThroughOpenDoor : LosUtil.TestResults.ClearThroughClosedDoor;
                        } else if (var11 == IsoObject.VisionResult.Unblocked && var10 instanceof IsoThumpable && ((IsoThumpable)var10).isDoor) {
                           var8 = LosUtil.TestResults.ClearThroughOpenDoor;
                        } else if (var11 == IsoObject.VisionResult.Unblocked && var10 instanceof IsoWindow) {
                           var8 = LosUtil.TestResults.ClearThroughWindow;
                        } else {
                           if (var11 == IsoObject.VisionResult.Blocked && var10 instanceof IsoDoor && !var5) {
                              return LosUtil.TestResults.Blocked;
                           }

                           if (var11 == IsoObject.VisionResult.Blocked && var10 instanceof IsoThumpable && ((IsoThumpable)var10).isDoor && !var5) {
                              return LosUtil.TestResults.Blocked;
                           }

                           if (var11 == IsoObject.VisionResult.Blocked && var10 instanceof IsoThumpable && ((IsoThumpable)var10).isWindow()) {
                              return LosUtil.TestResults.Blocked;
                           }

                           if (var11 == IsoObject.VisionResult.Blocked && var10 instanceof IsoCurtain) {
                              return LosUtil.TestResults.Blocked;
                           }

                           if (var11 == IsoObject.VisionResult.Blocked && var10 instanceof IsoWindow) {
                              return LosUtil.TestResults.Blocked;
                           }

                           if (var11 == IsoObject.VisionResult.Blocked && var10 instanceof IsoBarricade) {
                              return LosUtil.TestResults.Blocked;
                           }
                        }
                     }
                  }
               }

               if (!var7.SpecialObjects.isEmpty()) {
                  for(var9 = 0; var9 < var7.SpecialObjects.size(); ++var9) {
                     var10 = (IsoObject)var7.SpecialObjects.get(var9);
                     if (var10 == null) {
                        return LosUtil.TestResults.Clear;
                     }

                     var11 = var10.TestVision(this, var7);
                     if (var11 != IsoObject.VisionResult.NoEffect) {
                        if (var11 == IsoObject.VisionResult.Unblocked && var10 instanceof IsoDoor) {
                           var8 = ((IsoDoor)var10).IsOpen() ? LosUtil.TestResults.ClearThroughOpenDoor : LosUtil.TestResults.ClearThroughClosedDoor;
                        } else if (var11 == IsoObject.VisionResult.Unblocked && var10 instanceof IsoThumpable && ((IsoThumpable)var10).isDoor) {
                           var8 = LosUtil.TestResults.ClearThroughOpenDoor;
                        } else if (var11 == IsoObject.VisionResult.Unblocked && var10 instanceof IsoWindow) {
                           var8 = LosUtil.TestResults.ClearThroughWindow;
                        } else {
                           if (var11 == IsoObject.VisionResult.Blocked && var10 instanceof IsoDoor && !var5) {
                              return LosUtil.TestResults.Blocked;
                           }

                           if (var11 == IsoObject.VisionResult.Blocked && var10 instanceof IsoThumpable && ((IsoThumpable)var10).isDoor && !var5) {
                              return LosUtil.TestResults.Blocked;
                           }

                           if (var11 == IsoObject.VisionResult.Blocked && var10 instanceof IsoThumpable && ((IsoThumpable)var10).isWindow()) {
                              return LosUtil.TestResults.Blocked;
                           }

                           if (var11 == IsoObject.VisionResult.Blocked && var10 instanceof IsoCurtain) {
                              return LosUtil.TestResults.Blocked;
                           }

                           if (var11 == IsoObject.VisionResult.Blocked && var10 instanceof IsoWindow) {
                              return LosUtil.TestResults.Blocked;
                           }

                           if (var11 == IsoObject.VisionResult.Blocked && var10 instanceof IsoBarricade) {
                              return LosUtil.TestResults.Blocked;
                           }
                        }
                     }
                  }
               }
            } else if (var3 > 0 && var7 != null && (this.z != -1 || var7.z != 0) && var7.getProperties().Is(IsoFlagType.exterior) && !this.getProperties().Is(IsoFlagType.exterior)) {
               var8 = LosUtil.TestResults.Blocked;
            }

            return !getMatrixBit(this.visionMatrix, var1 + 1, var2 + 1, var3 + 1) ? var8 : LosUtil.TestResults.Blocked;
         }
      } else {
         return LosUtil.TestResults.Blocked;
      }
   }

   public boolean TreatAsSolidFloor() {
      if (this.SolidFloorCached) {
         return this.SolidFloor;
      } else {
         if (!this.Properties.Is(IsoFlagType.solidfloor) && !this.HasStairs()) {
            this.SolidFloor = false;
         } else {
            this.SolidFloor = true;
         }

         this.SolidFloorCached = true;
         return this.SolidFloor;
      }
   }

   public void AddSpecialTileObject(IsoObject var1) {
      this.AddSpecialObject(var1);
   }

   public void renderCharacters(int var1, boolean var2, boolean var3) {
      if (this.z < var1) {
         if (!isOnScreenLast) {
         }

         if (var3) {
            setBlendFunc();
         }

         if (this.MovingObjects.size() > 1) {
            Collections.sort(this.MovingObjects, comp);
         }

         int var4 = IsoCamera.frameState.playerIndex;
         ColorInfo var5 = this.lightInfo[var4];
         int var6 = this.StaticMovingObjects.size();

         int var7;
         IsoMovingObject var8;
         for(var7 = 0; var7 < var6; ++var7) {
            var8 = (IsoMovingObject)this.StaticMovingObjects.get(var7);
            if ((var8.sprite != null || var8 instanceof IsoDeadBody) && (!var2 || var8 instanceof IsoDeadBody && !this.HasStairs()) && (var2 || !(var8 instanceof IsoDeadBody) || this.HasStairs())) {
               var8.render(var8.getX(), var8.getY(), var8.getZ(), var5, true, false, (Shader)null);
            }
         }

         var6 = this.MovingObjects.size();

         for(var7 = 0; var7 < var6; ++var7) {
            var8 = (IsoMovingObject)this.MovingObjects.get(var7);
            if (var8 != null && var8.sprite != null) {
               boolean var9 = var8.bOnFloor;
               if (var9 && var8 instanceof IsoZombie) {
                  IsoZombie var10 = (IsoZombie)var8;
                  var9 = var10.isProne();
                  if (!BaseVehicle.RENDER_TO_TEXTURE) {
                     var9 = false;
                  }
               }

               if ((!var2 || var9) && (var2 || !var9)) {
                  var8.render(var8.getX(), var8.getY(), var8.getZ(), var5, true, false, (Shader)null);
               }
            }
         }

      }
   }

   public void renderDeferredCharacters(int var1) {
      if (!this.DeferedCharacters.isEmpty()) {
         if (this.DeferredCharacterTick != this.getCell().DeferredCharacterTick) {
            this.DeferedCharacters.clear();
         } else if (this.z >= var1) {
            this.DeferedCharacters.clear();
         } else if (PerformanceSettings.LightingFrameSkip != 3) {
            IndieGL.enableAlphaTest();
            IndieGL.glAlphaFunc(516, 0.0F);
            float var2 = IsoUtils.XToScreen((float)this.x, (float)this.y, (float)this.z, 0);
            float var3 = IsoUtils.YToScreen((float)this.x, (float)this.y, (float)this.z, 0);
            var2 -= IsoCamera.frameState.OffX;
            var3 -= IsoCamera.frameState.OffY;
            IndieGL.glColorMask(false, false, false, false);
            Texture.getWhite().renderwallnw(var2, var3, (float)(64 * Core.TileScale), (float)(32 * Core.TileScale), -1, -1, -1, -1, -1, -1);
            IndieGL.glColorMask(true, true, true, true);
            IndieGL.enableAlphaTest();
            IndieGL.glAlphaFunc(516, 0.0F);
            ColorInfo var4 = this.lightInfo[IsoCamera.frameState.playerIndex];
            Collections.sort(this.DeferedCharacters, comp);

            for(int var5 = 0; var5 < this.DeferedCharacters.size(); ++var5) {
               IsoGameCharacter var6 = (IsoGameCharacter)this.DeferedCharacters.get(var5);
               if (var6.sprite != null) {
                  var6.setbDoDefer(false);
                  var6.render(var6.getX(), var6.getY(), var6.getZ(), var4, true, false, (Shader)null);
                  var6.renderObjectPicker(var6.getX(), var6.getY(), var6.getZ(), var4);
                  var6.setbDoDefer(true);
               }
            }

            this.DeferedCharacters.clear();
            IndieGL.glAlphaFunc(516, 0.0F);
         }
      }
   }

   public void switchLight(boolean var1) {
      for(int var2 = 0; var2 < this.Objects.size(); ++var2) {
         IsoObject var3 = (IsoObject)this.Objects.get(var2);
         if (var3 instanceof IsoLightSwitch) {
            ((IsoLightSwitch)var3).setActive(var1);
         }
      }

   }

   public void removeLightSwitch() {
      for(int var1 = 0; var1 < this.Objects.size(); ++var1) {
         IsoObject var2 = (IsoObject)this.Objects.get(var1);
         if (var2 instanceof IsoLightSwitch) {
            this.Objects.remove(var1);
            --var1;
         }
      }

   }

   public boolean IsOnScreen() {
      return this.IsOnScreen(false);
   }

   public boolean IsOnScreen(boolean var1) {
      if (this.CachedScreenValue != Core.TileScale) {
         this.CachedScreenX = IsoUtils.XToScreen((float)this.x, (float)this.y, (float)this.z, 0);
         this.CachedScreenY = IsoUtils.YToScreen((float)this.x, (float)this.y, (float)this.z, 0);
         this.CachedScreenValue = Core.TileScale;
      }

      float var2 = this.CachedScreenX;
      float var3 = this.CachedScreenY;
      var2 -= IsoCamera.frameState.OffX;
      var3 -= IsoCamera.frameState.OffY;
      int var4 = var1 ? 32 * Core.TileScale : 0;
      if (this.hasTree) {
         int var5 = 384 * Core.TileScale / 2 - 96 * Core.TileScale;
         int var6 = 256 * Core.TileScale - 32 * Core.TileScale;
         if (var2 + (float)var5 <= (float)(0 - var4)) {
            return false;
         } else if (var3 + (float)(32 * Core.TileScale) <= (float)(0 - var4)) {
            return false;
         } else if (var2 - (float)var5 >= (float)(IsoCamera.frameState.OffscreenWidth + var4)) {
            return false;
         } else {
            return !(var3 - (float)var6 >= (float)(IsoCamera.frameState.OffscreenHeight + var4));
         }
      } else if (var2 + (float)(32 * Core.TileScale) <= (float)(0 - var4)) {
         return false;
      } else if (var3 + (float)(32 * Core.TileScale) <= (float)(0 - var4)) {
         return false;
      } else if (var2 - (float)(32 * Core.TileScale) >= (float)(IsoCamera.frameState.OffscreenWidth + var4)) {
         return false;
      } else {
         return !(var3 - (float)(96 * Core.TileScale) >= (float)(IsoCamera.frameState.OffscreenHeight + var4));
      }
   }

   private static void initWaterSplashCache() {
      int var0;
      for(var0 = 0; var0 < 16; ++var0) {
         waterSplashCache[var0] = Texture.getSharedTexture("media/textures/waterSplashes/WaterSplashSmall0_" + var0 + ".png");
      }

      for(var0 = 16; var0 < 48; ++var0) {
         waterSplashCache[var0] = Texture.getSharedTexture("media/textures/waterSplashes/WaterSplashBig0_" + var0 + ".png");
      }

      for(var0 = 48; var0 < 80; ++var0) {
         waterSplashCache[var0] = Texture.getSharedTexture("media/textures/waterSplashes/WaterSplashBig1_" + var0 + ".png");
      }

      isWaterSplashCacheInitialised = true;
   }

   public void startWaterSplash(boolean var1, float var2, float var3) {
      if (this.isSeen(IsoCamera.frameState.playerIndex) && !this.waterSplashData.isSplashNow()) {
         if (var1) {
            this.waterSplashData.initBigSplash(var2, var3);
         } else {
            this.waterSplashData.initSmallSplash(var2, var3);
         }

         FishSplashSoundManager.instance.addSquare(this);
      }

   }

   public void startWaterSplash(boolean var1) {
      this.startWaterSplash(var1, Rand.Next(0.0F, 0.5F) - 0.25F, Rand.Next(0.0F, 0.5F) - 0.25F);
   }

   public boolean shouldRenderFishSplash(int var1) {
      if (this.Objects.size() != 1) {
         return false;
      } else {
         IsoObject var2 = (IsoObject)this.Objects.get(0);
         if (var2.AttachedAnimSprite != null && !var2.AttachedAnimSprite.isEmpty()) {
            return false;
         } else {
            return this.chunk != null && this.isCouldSee(var1) && this.waterSplashData.isSplashNow();
         }
      }
   }

   public ColorInfo getLightInfo(int var1) {
      return this.lightInfo[var1];
   }

   public void cacheLightInfo() {
      int var1 = IsoCamera.frameState.playerIndex;
      this.lightInfo[var1] = this.lighting[var1].lightInfo();
   }

   public void setLightInfoServerGUIOnly(ColorInfo var1) {
      this.lightInfo[0] = var1;
   }

   public int renderFloor(Shader var1) {
      int var2;
      try {
         IsoGridSquare.s_performance.renderFloor.start();
         var2 = this.renderFloorInternal(var1);
      } finally {
         IsoGridSquare.s_performance.renderFloor.end();
      }

      return var2;
   }

   private int renderFloorInternal(Shader var1) {
      int var2 = IsoCamera.frameState.playerIndex;
      ColorInfo var3 = this.lightInfo[var2];
      IsoGridSquare var4 = IsoCamera.frameState.CamCharacterSquare;
      boolean var5 = this.lighting[var2].bCouldSee();
      float var6 = this.lighting[var2].darkMulti();
      boolean var7 = GameClient.bClient && IsoPlayer.players[var2] != null && IsoPlayer.players[var2].isSeeNonPvpZone();
      boolean var8 = Core.bDebug && GameClient.bClient && SafeHouse.isSafeHouse(this, (String)null, true) != null;
      boolean var9 = IsoPlayer.players[var2] != null && IsoPlayer.players[var2].isSeeDesignationZone();
      Double var10 = IsoPlayer.players[var2] != null ? IsoPlayer.players[var2].getSelectedZoneForHighlight() : 0.0;
      boolean var11 = true;
      float var12 = 1.0F;
      float var13 = 1.0F;
      if (var4 != null) {
         long var14 = this.getRoomID();
         if (var14 != -1L) {
            long var16 = IsoWorld.instance.CurrentCell.GetEffectivePlayerRoomId();
            if (var16 == -1L && IsoWorld.instance.CurrentCell.CanBuildingSquareOccludePlayer(this, var2)) {
               var11 = false;
               var12 = 1.0F;
               var13 = 1.0F;
            } else if (!var5 && var14 != var16 && var6 < 0.5F) {
               var11 = false;
               var12 = 0.0F;
               var13 = var6 * 2.0F;
            }
         }
      }

      IsoWaterGeometry var32 = this.z == 0 ? this.getWater() : null;
      boolean var15 = var32 != null && var32.bShore;
      float var33 = var32 == null ? 0.0F : var32.depth[0];
      float var17 = var32 == null ? 0.0F : var32.depth[3];
      float var18 = var32 == null ? 0.0F : var32.depth[2];
      float var19 = var32 == null ? 0.0F : var32.depth[1];
      setBlendFunc();
      int var20 = 0;
      int var21 = this.Objects.size();
      IsoObject[] var22 = (IsoObject[])this.Objects.getElements();

      for(int var23 = 0; var23 < var21; ++var23) {
         IsoObject var24 = var22[var23];
         if (var7 && (var24.highlightFlags & 1) == 0) {
            var24.setHighlighted(true);
            if (NonPvpZone.getNonPvpZone(this.x, this.y) != null) {
               var24.setHighlightColor(0.6F, 0.6F, 1.0F, 0.5F);
            } else {
               var24.setHighlightColor(1.0F, 0.6F, 0.6F, 0.5F);
            }
         }

         if (var9) {
            DesignationZone var25 = DesignationZone.getZone(this.x, this.y, this.z);
            if (var25 != null) {
               var24.setHighlighted(true);
               if (var10 > 0.0 && var25.getId().intValue() == var10.intValue()) {
                  var24.setHighlightColor(DesignationZoneAnimal.ZONESELECTEDCOLORR, DesignationZoneAnimal.ZONESELECTEDCOLORG, DesignationZoneAnimal.ZONESELECTEDCOLORB, 0.8F);
               } else {
                  var24.setHighlightColor(DesignationZoneAnimal.ZONECOLORR, DesignationZoneAnimal.ZONECOLORG, DesignationZoneAnimal.ZONECOLORB, 0.8F);
               }
            }
         }

         if (var8) {
            var24.setHighlighted(true);
            var24.setHighlightColor(1.0F, 0.0F, 0.0F, 1.0F);
         }

         boolean var34 = true;
         if (var24.sprite != null && !var24.sprite.solidfloor && var24.sprite.renderLayer != 1) {
            var34 = false;
            var20 |= 4;
         }

         if (var24 instanceof IsoFire || var24 instanceof IsoCarBatteryCharger) {
            var34 = false;
            var20 |= 4;
         }

         if (PerformanceSettings.FBORenderChunk && IsoWater.getInstance().getShaderEnable() && var32 != null && var32.isValid() && var24.sprite != null && var24.sprite.Properties.Is(IsoFlagType.water)) {
            var34 = false;
         }

         if (!var34) {
            boolean var35 = var24.sprite != null && (var24.sprite.isBush || var24.sprite.canBeRemoved || var24.sprite.attachedFloor);
            if (this.bFlattenGrassEtc && var35) {
               var20 |= 2;
            }
         } else {
            IndieGL.glAlphaFunc(516, 0.0F);
            var24.setTargetAlpha(var2, var13);
            if (var11) {
               var24.setAlpha(var2, var12);
            }

            if (DebugOptions.instance.Terrain.RenderTiles.RenderGridSquares.getValue() && var24.sprite != null) {
               IndieGL.StartShader(var1, var2);
               FloorShaperAttachedSprites var27 = FloorShaperAttachedSprites.instance;
               Object var26;
               if (!var24.getProperties().Is(IsoFlagType.diamondFloor) && !var24.getProperties().Is(IsoFlagType.water)) {
                  var26 = FloorShaperDeDiamond.instance;
               } else {
                  var26 = FloorShaperDiamond.instance;
               }

               int var28 = this.getVertLight(0, var2);
               int var29 = this.getVertLight(1, var2);
               int var30 = this.getVertLight(2, var2);
               int var31 = this.getVertLight(3, var2);
               if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Floor.LightingDebug.getValue()) {
                  var28 = -65536;
                  var29 = -65536;
                  var30 = -16776961;
                  var31 = -16776961;
               }

               var27.setShore(var15);
               var27.setWaterDepth(var33, var17, var18, var19);
               var27.setVertColors(var28, var29, var30, var31);
               ((FloorShaper)var26).setShore(var15);
               ((FloorShaper)var26).setWaterDepth(var33, var17, var18, var19);
               ((FloorShaper)var26).setVertColors(var28, var29, var30, var31);
               var24.renderFloorTile((float)this.x, (float)this.y, (float)this.z, PerformanceSettings.LightingFrameSkip < 3 ? defColorInfo : var3, true, false, var1, (Consumer)var26, var27);
               IndieGL.StartShader((Shader)null);
            }

            var20 |= 1;
            if ((var24.highlightFlags & 1) == 0) {
               var20 |= 8;
            }

            if (!PerformanceSettings.FBORenderChunk && (var24.highlightFlags & 2) != 0) {
               var24.highlightFlags &= -4;
            }
         }
      }

      if (!FBORenderChunkManager.instance.isCaching() && this.IsOnScreen(true)) {
         IndieGL.glBlendFunc(770, 771);
         this.renderRainSplash(var2, var3);
         this.renderFishSplash(var2, var3);
      }

      return var20;
   }

   public void renderRainSplash(int var1, ColorInfo var2) {
      if ((this.getCell().rainIntensity > 0 || RainManager.isRaining() && RainManager.RainIntensity > 0.0F) && this.isExteriorCache && !this.isVegitationCache && this.isSolidFloorCache && this.isCouldSee(var1)) {
         int var3;
         if (!IsoCamera.frameState.Paused) {
            var3 = this.getCell().rainIntensity == 0 ? Math.min(PZMath.fastfloor(RainManager.RainIntensity / 0.2F) + 1, 5) : this.getCell().rainIntensity;
            if (this.splashFrame < 0.0F && Rand.Next(Rand.AdjustForFramerate((int)(5.0F / (float)var3) * 100)) == 0) {
               this.splashFrame = 0.0F;
            }
         }

         if (this.splashFrame >= 0.0F) {
            var3 = (int)(this.splashFrame * 4.0F);
            if (rainsplashCache[var3] == null) {
               rainsplashCache[var3] = "RainSplash_00_" + var3;
            }

            Texture var4 = Texture.getSharedTexture(rainsplashCache[var3]);
            if (var4 != null) {
               float var5 = IsoUtils.XToScreen((float)this.x + this.splashX, (float)this.y + this.splashY, (float)this.z, 0) - IsoCamera.frameState.OffX;
               float var6 = IsoUtils.YToScreen((float)this.x + this.splashX, (float)this.y + this.splashY, (float)this.z, 0) - IsoCamera.frameState.OffY;
               var5 -= (float)(var4.getWidth() / 2 * Core.TileScale);
               var6 -= (float)(var4.getHeight() / 2 * Core.TileScale);
               float var7 = 0.6F * (this.getCell().rainIntensity > 0 ? 1.0F : RainManager.RainIntensity);
               float var8 = SceneShaderStore.WeatherShader != null ? 0.6F : 1.0F;
               SpriteRenderer.instance.render(var4, var5, var6, (float)(var4.getWidth() * Core.TileScale), (float)(var4.getHeight() * Core.TileScale), 0.8F * var2.r, 0.9F * var2.g, 1.0F * var2.b, var7 * var8, (Consumer)null);
            }

            if (!IsoCamera.frameState.Paused && this.splashFrameNum != IsoCamera.frameState.frameCount) {
               this.splashFrame += 0.08F * (30.0F / (float)PerformanceSettings.getLockFPS());
               if (this.splashFrame >= 1.0F) {
                  this.splashX = Rand.Next(0.1F, 0.9F);
                  this.splashY = Rand.Next(0.1F, 0.9F);
                  this.splashFrame = -1.0F;
               }

               this.splashFrameNum = IsoCamera.frameState.frameCount;
            }
         }
      } else {
         this.splashFrame = -1.0F;
      }

   }

   public void renderRainSplash(int var1, ColorInfo var2, float var3, boolean var4) {
      if (!(var3 < 0.0F) && (int)(var3 * 4.0F) < rainsplashCache.length) {
         if (this.isCouldSee(var1)) {
            if (this.isExteriorCache && !this.isVegitationCache && this.isSolidFloorCache) {
               int var5 = (int)(var3 * 4.0F);
               if (rainsplashCache[var5] == null) {
                  rainsplashCache[var5] = "RainSplash_00_" + var5;
               }

               Texture var6 = Texture.getSharedTexture(rainsplashCache[var5]);
               if (var6 != null) {
                  if (var4) {
                     this.splashX = Rand.Next(0.1F, 0.9F);
                     this.splashY = Rand.Next(0.1F, 0.9F);
                  }

                  float var7 = IsoUtils.XToScreen((float)this.x + this.splashX, (float)this.y + this.splashY, (float)this.z, 0) - IsoCamera.frameState.OffX;
                  float var8 = IsoUtils.YToScreen((float)this.x + this.splashX, (float)this.y + this.splashY, (float)this.z, 0) - IsoCamera.frameState.OffY;
                  var7 -= (float)(var6.getWidth() / 2 * Core.TileScale);
                  var8 -= (float)(var6.getHeight() / 2 * Core.TileScale);
                  if (PerformanceSettings.FBORenderChunk) {
                     TextureDraw.nextZ = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), (float)this.x + this.splashX + 0.1F, (float)this.y + this.splashY + 0.1F, (float)this.z).depthStart * 2.0F - 1.0F;
                     SpriteRenderer.instance.StartShader(0, var1);
                     IndieGL.enableDepthTest();
                     IndieGL.glDepthFunc(515);
                     IndieGL.glDepthMask(false);
                  }

                  IndieGL.glBlendFunc(770, 771);
                  float var9 = 0.6F * (this.getCell().rainIntensity > 0 ? 1.0F : RainManager.RainIntensity);
                  float var10 = 1.0F;
                  SpriteRenderer.instance.render(var6, var7, var8, (float)(var6.getWidth() * Core.TileScale), (float)(var6.getHeight() * Core.TileScale), 0.8F * var2.r, 0.9F * var2.g, 1.0F * var2.b, var9 * var10, (Consumer)null);
               }
            }
         }
      }
   }

   public void renderFishSplash(int var1, ColorInfo var2) {
      if (this.isCouldSee(var1) && this.waterSplashData.isSplashNow()) {
         Texture var3 = this.waterSplashData.getTexture();
         if (var3 != null) {
            float var4 = (float)var3.getWidth() * this.waterSplashData.size;
            float var5 = (float)var3.getHeight() * this.waterSplashData.size;
            float var6 = IsoUtils.XToScreen((float)this.x + this.waterSplashData.dx, (float)this.y + this.waterSplashData.dy, 0.0F, 0) - IsoCamera.frameState.OffX;
            float var7 = IsoUtils.YToScreen((float)this.x + this.waterSplashData.dx, (float)this.y + this.waterSplashData.dy, 0.0F, 0) - IsoCamera.frameState.OffY;
            if (PerformanceSettings.FBORenderChunk) {
               var6 = IsoUtils.XToScreen((float)this.x + 0.5F, (float)this.y + 0.5F, (float)this.z, 0) - IsoCamera.frameState.OffX - var4 / 2.0F;
               var7 = IsoUtils.YToScreen((float)this.x + 0.5F, (float)this.y + 0.5F, (float)this.z, 0) - IsoCamera.frameState.OffY - var5 / 2.0F;
               TextureDraw.nextZ = (IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), (float)this.x + 0.99F, (float)this.y + 0.99F, (float)this.z).depthStart + IsoWater.DEPTH_ADJUST) * 2.0F - 1.0F;
               SpriteRenderer.instance.StartShader(0, var1);
               IndieGL.enableDepthTest();
               IndieGL.glDepthFunc(515);
               IndieGL.glDepthMask(false);
            }

            var6 += IsoCamera.cameras[var1].fixJigglyModelsX * IsoCamera.frameState.zoom;
            var7 += IsoCamera.cameras[var1].fixJigglyModelsY * IsoCamera.frameState.zoom;
            float var8 = 1.0F;
            float var9 = 1.0F;
            SpriteRenderer.instance.render(var3, var6, var7, var4, var5, 0.8F * var2.r, 0.9F * var2.g, var2.b, var8 * var9, (Consumer)null);
            this.waterSplashData.update();
         }
      }

   }

   public boolean isSpriteOnSouthOrEastWall(IsoObject var1) {
      if (var1 instanceof IsoBarricade) {
         return var1.getDir() == IsoDirections.S || var1.getDir() == IsoDirections.E;
      } else if (var1 instanceof IsoCurtain) {
         IsoCurtain var3 = (IsoCurtain)var1;
         return var3.getType() == IsoObjectType.curtainS || var3.getType() == IsoObjectType.curtainE;
      } else {
         PropertyContainer var2 = var1.getProperties();
         return var2 != null && (var2.Is(IsoFlagType.attachedE) || var2.Is(IsoFlagType.attachedS));
      }
   }

   public void RenderOpenDoorOnly() {
      int var1 = this.Objects.size();
      IsoObject[] var2 = (IsoObject[])this.Objects.getElements();

      try {
         byte var3 = 0;
         int var4 = var1 - 1;

         for(int var5 = var3; var5 <= var4; ++var5) {
            IsoObject var6 = var2[var5];
            if (var6.sprite != null && (var6.sprite.getProperties().Is(IsoFlagType.attachedN) || var6.sprite.getProperties().Is(IsoFlagType.attachedW))) {
               var6.renderFxMask((float)this.x, (float)this.y, (float)this.z, false);
            }
         }
      } catch (Exception var7) {
         ExceptionLogger.logException(var7);
      }

   }

   public boolean RenderMinusFloorFxMask(int var1, boolean var2, boolean var3) {
      boolean var4 = false;
      int var5 = this.Objects.size();
      IsoObject[] var6 = (IsoObject[])this.Objects.getElements();
      long var7 = System.currentTimeMillis();

      try {
         int var9 = var2 ? var5 - 1 : 0;
         int var10 = var2 ? 0 : var5 - 1;
         int var11 = var9;

         while(true) {
            if (var2) {
               if (var11 < var10) {
                  break;
               }
            } else if (var11 > var10) {
               break;
            }

            IsoObject var12 = var6[var11];
            if (var12.sprite != null) {
               boolean var13 = true;
               IsoObjectType var14 = var12.sprite.getType();
               if (var12.sprite.solidfloor || var12.sprite.renderLayer == 1) {
                  var13 = false;
               }

               if (this.z >= var1 && !var12.sprite.alwaysDraw) {
                  var13 = false;
               }

               boolean var15 = var12.sprite.isBush || var12.sprite.canBeRemoved || var12.sprite.attachedFloor;
               if ((!var3 || var15 && this.bFlattenGrassEtc) && (var3 || !var15 || !this.bFlattenGrassEtc)) {
                  if ((var14 == IsoObjectType.WestRoofB || var14 == IsoObjectType.WestRoofM || var14 == IsoObjectType.WestRoofT) && this.z == var1 - 1 && this.z == PZMath.fastfloor(IsoCamera.getCameraCharacterZ())) {
                     var13 = false;
                  }

                  if (this.isSpriteOnSouthOrEastWall(var12)) {
                     if (!var2) {
                        var13 = false;
                     }

                     var4 = true;
                  } else if (var2) {
                     var13 = false;
                  }

                  if (var13) {
                     if (!var12.sprite.cutW && !var12.sprite.cutN) {
                        var12.renderFxMask((float)this.x, (float)this.y, (float)this.z, false);
                     } else {
                        int var16 = IsoCamera.frameState.playerIndex;
                        boolean var17 = var12.sprite.cutN;
                        boolean var18 = var12.sprite.cutW;
                        IsoGridSquare var19 = this.nav[IsoDirections.N.index()];
                        IsoGridSquare var20 = this.nav[IsoDirections.S.index()];
                        IsoGridSquare var21 = this.nav[IsoDirections.W.index()];
                        IsoGridSquare var22 = this.nav[IsoDirections.E.index()];
                        int var23 = this.getPlayerCutawayFlag(var16, var7);
                        int var24 = var19 == null ? 0 : var19.getPlayerCutawayFlag(var16, var7);
                        int var25 = var20 == null ? 0 : var20.getPlayerCutawayFlag(var16, var7);
                        int var26 = var21 == null ? 0 : var21.getPlayerCutawayFlag(var16, var7);
                        int var27 = var22 == null ? 0 : var22.getPlayerCutawayFlag(var16, var7);
                        IsoDirections var28;
                        if (var17 && var18) {
                           var28 = IsoDirections.NW;
                        } else if (var17) {
                           var28 = IsoDirections.N;
                        } else if (var18) {
                           var28 = IsoDirections.W;
                        } else {
                           var28 = IsoDirections.W;
                        }

                        this.DoCutawayShaderSprite(var12.sprite, var28, var23, var24, var25, var26, var27);
                     }
                  }
               }
            }

            var11 += var2 ? -1 : 1;
         }
      } catch (Exception var29) {
         ExceptionLogger.logException(var29);
      }

      return var4;
   }

   public boolean isWindowOrWindowFrame(IsoObject var1, boolean var2) {
      if (var1 != null && var1.sprite != null) {
         if (var2 && var1.sprite.getProperties().Is(IsoFlagType.windowN)) {
            return true;
         } else if (!var2 && var1.sprite.getProperties().Is(IsoFlagType.windowW)) {
            return true;
         } else {
            IsoThumpable var3 = (IsoThumpable)Type.tryCastTo(var1, IsoThumpable.class);
            if (var3 != null && var3.isWindow()) {
               return var2 == var3.getNorth();
            } else if (var1 instanceof IsoWindowFrame) {
               IsoWindowFrame var4 = (IsoWindowFrame)var1;
               return var4.getNorth() == var2;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public boolean renderMinusFloor(int var1, boolean var2, boolean var3, int var4, int var5, int var6, int var7, int var8, Shader var9) {
      boolean var10 = false;
      if (!this.localTemporaryObjects.isEmpty()) {
         var10 = this.renderMinusFloor(this.localTemporaryObjects, var1, var2, var3, var4, var5, var6, var7, var8, var9);
      }

      return this.renderMinusFloor(this.Objects, var1, var2, var3, var4, var5, var6, var7, var8, var9) || var10;
   }

   boolean renderMinusFloor(PZArrayList<IsoObject> var1, int var2, boolean var3, boolean var4, int var5, int var6, int var7, int var8, int var9, Shader var10) {
      if (!DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.RenderMinusFloor.getValue()) {
         return false;
      } else {
         setBlendFunc();
         int var11 = 0;
         isOnScreenLast = this.IsOnScreen();
         int var12 = IsoCamera.frameState.playerIndex;
         IsoGridSquare var13 = IsoCamera.frameState.CamCharacterSquare;
         ColorInfo var14 = this.lightInfo[var12];
         boolean var15 = this.lighting[var12].bCouldSee();
         float var16 = this.lighting[var12].darkMulti();
         boolean var17 = IsoWorld.instance.CurrentCell.CanBuildingSquareOccludePlayer(this, var12);
         var14.a = 1.0F;
         defColorInfo.r = 1.0F;
         defColorInfo.g = 1.0F;
         defColorInfo.b = 1.0F;
         defColorInfo.a = 1.0F;
         if (Core.bDebug && DebugOptions.instance.DebugDraw_SkipWorldShading.getValue()) {
            var14 = defColorInfo;
         }

         float var18 = this.CachedScreenX - IsoCamera.frameState.OffX;
         float var19 = this.CachedScreenY - IsoCamera.frameState.OffY;
         boolean var20 = true;
         IsoCell var21 = this.getCell();
         if (var18 + (float)(32 * Core.TileScale) <= (float)var21.StencilX1 || var18 - (float)(32 * Core.TileScale) >= (float)var21.StencilX2 || var19 + (float)(32 * Core.TileScale) <= (float)var21.StencilY1 || var19 - (float)(96 * Core.TileScale) >= (float)var21.StencilY2) {
            var20 = false;
         }

         boolean var22 = false;
         int var23 = var1.size();
         IsoObject[] var24 = (IsoObject[])var1.getElements();
         tempWorldInventoryObjects.clear();
         int var25 = var3 ? var23 - 1 : 0;
         int var26 = var3 ? 0 : var23 - 1;
         boolean var27 = false;
         boolean var28 = false;
         boolean var29 = false;
         boolean var30 = false;
         if (!var3) {
            for(int var31 = var25; var31 <= var26; ++var31) {
               IsoObject var32 = var24[var31];
               IsoGridSquare var33;
               if (this.isWindowOrWindowFrame(var32, true) && (var5 & 1) != 0) {
                  var33 = this.nav[IsoDirections.N.index()];
                  var29 = var15 || var33 != null && var33.isCouldSee(var12);
               }

               if (this.isWindowOrWindowFrame(var32, false) && (var5 & 2) != 0) {
                  var33 = this.nav[IsoDirections.W.index()];
                  var30 = var15 || var33 != null && var33.isCouldSee(var12);
               }

               if (var32.sprite != null && (var32.sprite.getType() == IsoObjectType.doorFrN || var32.sprite.getType() == IsoObjectType.doorN) && (var5 & 1) != 0) {
                  var33 = this.nav[IsoDirections.N.index()];
                  var27 = var15 || var33 != null && var33.isCouldSee(var12);
               }

               if (var32.sprite != null && (var32.sprite.getType() == IsoObjectType.doorFrW || var32.sprite.getType() == IsoObjectType.doorW) && (var5 & 2) != 0) {
                  var33 = this.nav[IsoDirections.W.index()];
                  var28 = var15 || var33 != null && var33.isCouldSee(var12);
               }
            }
         }

         long var58 = IsoWorld.instance.CurrentCell.GetEffectivePlayerRoomId();
         bWallCutawayN = false;
         bWallCutawayW = false;
         int var59 = var25;

         while(true) {
            if (var3) {
               if (var59 < var26) {
                  break;
               }
            } else if (var59 > var26) {
               break;
            }

            IsoObject var34 = var24[var59];
            boolean var35 = true;
            IsoObjectType var36 = IsoObjectType.MAX;
            if (var34.sprite != null) {
               var36 = var34.sprite.getType();
            }

            CircleStencil = false;
            if (var34.sprite != null && (var34.sprite.solidfloor || var34.sprite.renderLayer == 1)) {
               var35 = false;
            }

            if (var34 instanceof IsoFire) {
               var35 = !var4;
            }

            if (this.z >= var2 && (var34.sprite == null || !var34.sprite.alwaysDraw)) {
               var35 = false;
            }

            boolean var37 = var34.sprite != null && (var34.sprite.isBush || var34.sprite.canBeRemoved || var34.sprite.attachedFloor);
            if ((!var4 || var37 && this.bFlattenGrassEtc) && (var4 || !var37 || !this.bFlattenGrassEtc)) {
               if (var34.sprite != null && (var36 == IsoObjectType.WestRoofB || var36 == IsoObjectType.WestRoofM || var36 == IsoObjectType.WestRoofT) && this.z == var2 - 1 && this.z == PZMath.fastfloor(IsoCamera.getCameraCharacterZ())) {
                  var35 = false;
               }

               boolean var38 = var36 == IsoObjectType.doorFrW || var36 == IsoObjectType.doorW || var34.sprite != null && var34.sprite.cutW;
               boolean var39 = var36 == IsoObjectType.doorFrN || var36 == IsoObjectType.doorN || var34.sprite != null && var34.sprite.cutN;
               boolean var40 = var34 instanceof IsoDoor && ((IsoDoor)var34).open || var34 instanceof IsoThumpable && ((IsoThumpable)var34).open;
               boolean var41 = var34.container != null;
               boolean var42 = var34.sprite != null && var34.sprite.getProperties().Is(IsoFlagType.waterPiped);
               if (var34.sprite != null && var36 == IsoObjectType.MAX && !(var34 instanceof IsoDoor) && !(var34 instanceof IsoWindow) && !var41 && !var42) {
                  if (!var38 && var34.sprite.getProperties().Is(IsoFlagType.attachedW) && (var17 || (var5 & 2) != 0)) {
                     var35 = !bWallCutawayW;
                  } else if (!var39 && var34.sprite.getProperties().Is(IsoFlagType.attachedN) && (var17 || (var5 & 1) != 0)) {
                     var35 = !bWallCutawayN;
                  }
               }

               if (var34.sprite != null && !var34.sprite.solidfloor && IsoPlayer.getInstance().isClimbing()) {
                  var35 = true;
               }

               if (this.isSpriteOnSouthOrEastWall(var34)) {
                  if (!var3) {
                     var35 = false;
                  }

                  var22 = true;
               } else if (var3) {
                  var35 = false;
               }

               if (PerformanceSettings.FBORenderChunk) {
                  boolean var43 = var34.getRenderInfo(var12).m_layer == ObjectRenderLayer.Translucent;
                  if (FBORenderCell.instance.bRenderTranslucentOnly != var43) {
                     var35 = false;
                  }
               }

               if (var35) {
                  IndieGL.glAlphaFunc(516, 0.0F);
                  var34.bAlphaForced = false;
                  if (var40) {
                     var34.setTargetAlpha(var12, 0.6F);
                     var34.setAlpha(var12, 0.6F);
                  }

                  if (var34.sprite != null && (var38 || var39)) {
                     if (PerformanceSettings.LightingFrameSkip < 3) {
                        if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.DoorsAndWalls.getValue()) {
                           CircleStencil = true;
                           if (var13 != null && this.getRoomID() != -1L && var58 == -1L && var17) {
                              var34.setTargetAlpha(var12, 0.5F);
                              var34.setAlpha(var12, 0.5F);
                           } else if (this.getRoomID() != var58 && !var15 && (var34.getProperties().Is(IsoFlagType.transparentN) || var34.getProperties().Is(IsoFlagType.transparentW)) && var34.getSpriteName() != null && var34.getSpriteName().contains("police")) {
                              var34.setTargetAlpha(var12, 0.0F);
                              var34.setAlpha(var12, 0.0F);
                           } else if (!var40) {
                              var34.setTargetAlpha(var12, 1.0F);
                              var34.setAlpha(var12, 1.0F);
                           }

                           var34.bAlphaForced = true;
                           if (var34.sprite.cutW && var34.sprite.cutN) {
                              var11 = this.DoWallLightingNW(var34, var11, var5, var6, var7, var8, var9, var27, var28, var29, var30, var10);
                           } else if (var34.sprite.getType() != IsoObjectType.doorFrW && var36 != IsoObjectType.doorW && !var34.sprite.cutW) {
                              if (var36 == IsoObjectType.doorFrN || var36 == IsoObjectType.doorN || var34.sprite.cutN) {
                                 var11 = this.DoWallLightingN(var34, var11, var5, var6, var7, var8, var9, var27, var29, var10);
                              }
                           } else {
                              var11 = this.DoWallLightingW(var34, var11, var5, var6, var7, var8, var9, var28, var30, var10);
                           }

                           if (var34 instanceof IsoWindow && var34.getTargetAlpha(var12) < 1.0F) {
                              bWallCutawayN |= var34.sprite.cutN;
                              bWallCutawayW |= var34.sprite.cutW;
                           }
                        }
                     } else if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.DoorsAndWalls_SimpleLighting.getValue()) {
                        if (this.z != PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ) || var36 == IsoObjectType.doorFrW || var36 == IsoObjectType.doorFrN || var34 instanceof IsoWindow) {
                           var20 = false;
                        }

                        if (var34.getTargetAlpha(var12) < 1.0F) {
                           if (!var20) {
                              var34.setTargetAlpha(var12, 1.0F);
                           }

                           var34.setAlphaToTarget(var12);
                           IsoObject.LowLightingQualityHack = false;
                           var34.render((float)this.x, (float)this.y, (float)this.z, var14, true, false, (Shader)null);
                           if (!IsoObject.LowLightingQualityHack) {
                              var34.setTargetAlpha(var12, 1.0F);
                           }
                        } else {
                           var34.render((float)this.x, (float)this.y, (float)this.z, var14, true, false, (Shader)null);
                        }
                     }
                  } else if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Objects.getValue()) {
                     if (this.getRoomID() != -1L && this.getRoomID() != var58 && IsoPlayer.players[var12].isSeatedInVehicle() && IsoPlayer.players[var12].getVehicle().getCurrentSpeedKmHour() >= 50.0F) {
                        break;
                     }

                     IsoPlayer var61 = IsoPlayer.players[var12];
                     boolean var44 = IsoUtils.DistanceToSquared(var61.getX(), var61.getY(), (float)this.x - 1.5F - ((float)this.z - var61.getZ() * 3.0F), (float)this.y - 1.5F - ((float)this.z - var61.getZ() * 3.0F)) <= 30.0F;
                     IsoGridSquare var45 = IsoWorld.instance.CurrentCell.getGridSquare(this.x, this.y, 0);
                     IsoGridSquare var46 = null;
                     IsoGridSquare var47 = null;
                     boolean var48 = false;
                     boolean var49 = false;
                     if (var45 != null) {
                        var46 = var45.nav[IsoDirections.N.index()];
                        var47 = var45.nav[IsoDirections.W.index()];
                        var48 = var45.getWall(true) != null || var45.getDoor(true) != null || var45.getWindow(true) != null;
                        var49 = var45.getWall(false) != null || var45.getDoor(false) != null || var45.getWindow(false) != null;
                     }

                     boolean var50 = IsoUtils.DistanceToSquared(var61.getX(), var61.getY(), (float)this.x - 1.5F - ((float)this.z - var61.getZ() * 3.0F), (float)this.y - 1.5F - ((float)this.z - var61.getZ() * 3.0F)) <= 30.0F;
                     if ((var36 == IsoObjectType.WestRoofB || var36 == IsoObjectType.WestRoofM || var36 == IsoObjectType.WestRoofT) && (this.getRoomID() == -1L && ((var9 != 0 || var7 != 0) && (var61.getX() < (float)this.x && var61.getY() < (float)this.y || var61.getZ() < (float)this.z) || var45 != null && var45.getRoomID() == -1L && var50 && var61.getZ() < (float)(this.z + 1)) || (this.getRoomID() != -1L || var58 != -1L) && var50 && var61.getZ() < (float)(this.z + 1))) {
                        var46 = var45.nav[IsoDirections.N.index()];
                        var47 = var45.nav[IsoDirections.W.index()];
                        var48 = var45.getWall(true) != null || var45.getDoor(true) != null || var45.getWindow(true) != null;
                        var49 = var45.getWall(false) != null || var45.getDoor(false) != null || var45.getWindow(false) != null;
                     }

                     float var52;
                     if (var36 != IsoObjectType.WestRoofB && var36 != IsoObjectType.WestRoofM && var36 != IsoObjectType.WestRoofT || (this.getRoomID() != -1L || (var9 == 0 && var7 == 0 || (!(var61.getX() < (float)this.x) || !(var61.getY() < (float)this.y)) && !(var61.getZ() < (float)this.z)) && (var45 == null || var45.getRoomID() != -1L || !var44 && var58 == -1L || !(var61.getZ() < (float)(this.z + 1)) || (!var48 || var46 == null || var46.getRoomID() != -1L) && (!var49 || var47 == null || var47.getRoomID() != -1L) && (var48 || var49))) && (this.getRoomID() == -1L || var58 == -1L || !var44 || !(var61.getZ() < (float)(this.z + 1)))) {
                        if (var13 != null && !var15 && this.getRoomID() != var58 && var16 < 0.5F) {
                           if (var34.getProperties() != null && var34.getProperties().Is("forceFade")) {
                              var34.setTargetAlpha(var12, 0.0F);
                           } else {
                              var34.setTargetAlpha(var12, var16 * 2.0F);
                           }
                        } else {
                           if (!var40) {
                              var34.setTargetAlpha(var12, 1.0F);
                           }

                           if (IsoPlayer.getInstance() != null && var34.getProperties() != null && (var34.getProperties().Is(IsoFlagType.solid) || var34.getProperties().Is(IsoFlagType.solidtrans) || var34.getProperties().Is(IsoFlagType.attachedCeiling) || var34.getSprite().getProperties().Is(IsoFlagType.attachedE) || var34.getSprite().getProperties().Is(IsoFlagType.attachedS)) || var36.index() > 2 && var36.index() < 9 && IsoCamera.frameState.CamCharacterZ <= var34.getZ()) {
                              byte var51 = 3;
                              var52 = 0.75F;
                              if (var36.index() > 2 && var36.index() < 9 || var34.getSprite().getProperties().Is(IsoFlagType.attachedE) || var34.getSprite().getProperties().Is(IsoFlagType.attachedS) || var34.getProperties().Is(IsoFlagType.attachedCeiling)) {
                                 var51 = 4;
                                 if (var36.index() > 2 && var36.index() < 9) {
                                    var52 = 0.5F;
                                 }
                              }

                              if (var34.sprite.solid || var34.sprite.solidTrans) {
                                 var51 = 5;
                                 var52 = 0.25F;
                              }

                              int var53 = this.getX() - PZMath.fastfloor(IsoPlayer.getInstance().getX());
                              int var54 = this.getY() - PZMath.fastfloor(IsoPlayer.getInstance().getY());
                              if (var53 >= 0 && var53 < var51 && var54 >= 0 && var54 < var51 || var54 >= 0 && var54 < var51 && var53 >= 0 && var53 < var51) {
                                 var34.setTargetAlpha(var12, var52);
                              }

                              IsoZombie var55 = IsoCell.getInstance().getNearestVisibleZombie(var12);
                              if (var55 != null && var55.getCurrentSquare() != null && var55.getCurrentSquare().isCanSee(var12)) {
                                 int var56 = this.getX() - PZMath.fastfloor(var55.getX());
                                 int var57 = this.getY() - PZMath.fastfloor(var55.getY());
                                 if (var56 > 0 && var56 < var51 && var57 >= 0 && var57 < var51 || var57 > 0 && var57 < var51 && var56 >= 0 && var56 < var51) {
                                    var34.setTargetAlpha(var12, var52);
                                 }
                              }
                           }
                        }
                     } else {
                        var34.setTargetAlpha(var12, 0.0F);
                     }

                     if (var34 instanceof IsoWindow) {
                        IsoWindow var62 = (IsoWindow)var34;
                        if (var34.getTargetAlpha(var12) < 1.0E-4F) {
                           IsoGridSquare var65 = var62.getOppositeSquare();
                           if (var65 != null && var65 != this && var65.lighting[var12].bSeen()) {
                              var34.setTargetAlpha(var12, var65.lighting[var12].darkMulti() * 2.0F);
                           }
                        }

                        if (var34.getTargetAlpha(var12) > 0.4F && var5 != 0 && (var9 != 0 && var34.sprite.getProperties().Is(IsoFlagType.windowN) || var7 != 0 && var34.sprite.getProperties().Is(IsoFlagType.windowW))) {
                           var52 = 0.4F;
                           float var64 = 0.1F;
                           IsoPlayer var66 = IsoPlayer.players[var12];
                           if (var66 != null) {
                              float var67 = 5.0F;
                              float var68 = Math.abs(var66.getX() - (float)this.x) * Math.abs(var66.getX() - (float)this.x) + Math.abs(var66.getY() - (float)this.y) * Math.abs(var66.getY() - (float)this.y);
                              float var69 = var52 * (float)(1.0 - Math.sqrt((double)(var68 / var67)));
                              var34.setTargetAlpha(var12, Math.max(var69, var64));
                           } else {
                              var34.setTargetAlpha(var12, var64);
                           }

                           if (var9 != 0) {
                              bWallCutawayN = true;
                           } else {
                              bWallCutawayW = true;
                           }
                        }
                     }

                     if (var34 instanceof IsoTree) {
                        if (var20 && this.x >= PZMath.fastfloor(IsoCamera.frameState.CamCharacterX) && this.y >= PZMath.fastfloor(IsoCamera.frameState.CamCharacterY) && var13 != null && var13.Is(IsoFlagType.exterior)) {
                           ((IsoTree)var34).bRenderFlag = true;
                           var34.setTargetAlpha(var12, Math.min(0.99F, var34.getTargetAlpha(var12)));
                        } else {
                           ((IsoTree)var34).bRenderFlag = false;
                        }
                     }

                     IsoWorldInventoryObject var63 = (IsoWorldInventoryObject)Type.tryCastTo(var34, IsoWorldInventoryObject.class);
                     if (var63 != null) {
                        tempWorldInventoryObjects.add(var63);
                     } else {
                        if (!PerformanceSettings.FBORenderChunk && var34.getAlpha(var12) < 1.0F) {
                           IndieGL.glBlendFunc(770, 771);
                        }

                        var34.render((float)this.x, (float)this.y, (float)this.z, var14, true, false, (Shader)null);
                     }
                  }

                  if (!PerformanceSettings.FBORenderChunk && (var34.highlightFlags & 2) != 0) {
                     var34.highlightFlags &= -4;
                  }
               }
            }

            var59 += var3 ? -1 : 1;
         }

         Arrays.sort((IsoWorldInventoryObject[])tempWorldInventoryObjects.getElements(), 0, tempWorldInventoryObjects.size(), (var0, var1x) -> {
            float var2 = var0.xoff * var0.xoff + var0.yoff * var0.yoff;
            float var3 = var1x.xoff * var1x.xoff + var1x.yoff * var1x.yoff;
            if (var2 == var3) {
               return 0;
            } else {
               return var2 > var3 ? 1 : -1;
            }
         });

         for(var59 = 0; var59 < tempWorldInventoryObjects.size(); ++var59) {
            IsoWorldInventoryObject var60 = (IsoWorldInventoryObject)tempWorldInventoryObjects.get(var59);
            var60.render((float)this.x, (float)this.y, (float)this.z, var14, true, false, (Shader)null);
         }

         return var22;
      }
   }

   void RereouteWallMaskTo(IsoObject var1) {
      for(int var2 = 0; var2 < this.Objects.size(); ++var2) {
         IsoObject var3 = (IsoObject)this.Objects.get(var2);
         if (var3.sprite.getProperties().Is(IsoFlagType.collideW) || var3.sprite.getProperties().Is(IsoFlagType.collideN)) {
            var3.rerouteMask = var1;
         }
      }

   }

   void setBlockedGridPointers(GetSquare var1) {
      this.w = var1.getGridSquare(this.x - 1, this.y, this.z);
      this.e = var1.getGridSquare(this.x + 1, this.y, this.z);
      this.s = var1.getGridSquare(this.x, this.y + 1, this.z);
      this.n = var1.getGridSquare(this.x, this.y - 1, this.z);
      this.ne = var1.getGridSquare(this.x + 1, this.y - 1, this.z);
      this.nw = var1.getGridSquare(this.x - 1, this.y - 1, this.z);
      this.se = var1.getGridSquare(this.x + 1, this.y + 1, this.z);
      this.sw = var1.getGridSquare(this.x - 1, this.y + 1, this.z);
      this.u = var1.getGridSquare(this.x, this.y, this.z + 1);
      this.d = var1.getGridSquare(this.x, this.y, this.z - 1);
      if (this.u != null && (this.u.Properties.Is(IsoFlagType.solidfloor) || this.u.Properties.Is(IsoFlagType.solid))) {
         this.u = null;
      }

      if (this.d != null && (this.Properties.Is(IsoFlagType.solidfloor) || this.Properties.Is(IsoFlagType.solid))) {
         this.d = null;
      }

      if (this.s != null && this.testPathFindAdjacent((IsoMovingObject)null, this.s.x - this.x, this.s.y - this.y, this.s.z - this.z, var1)) {
         this.s = null;
      }

      if (this.w != null && this.testPathFindAdjacent((IsoMovingObject)null, this.w.x - this.x, this.w.y - this.y, this.w.z - this.z, var1)) {
         this.w = null;
      }

      if (this.n != null && this.testPathFindAdjacent((IsoMovingObject)null, this.n.x - this.x, this.n.y - this.y, this.n.z - this.z, var1)) {
         this.n = null;
      }

      if (this.e != null && this.testPathFindAdjacent((IsoMovingObject)null, this.e.x - this.x, this.e.y - this.y, this.e.z - this.z, var1)) {
         this.e = null;
      }

      if (this.sw != null && this.testPathFindAdjacent((IsoMovingObject)null, this.sw.x - this.x, this.sw.y - this.y, this.sw.z - this.z, var1)) {
         this.sw = null;
      }

      if (this.se != null && this.testPathFindAdjacent((IsoMovingObject)null, this.se.x - this.x, this.se.y - this.y, this.se.z - this.z, var1)) {
         this.se = null;
      }

      if (this.nw != null && this.testPathFindAdjacent((IsoMovingObject)null, this.nw.x - this.x, this.nw.y - this.y, this.nw.z - this.z, var1)) {
         this.nw = null;
      }

      if (this.ne != null && this.testPathFindAdjacent((IsoMovingObject)null, this.ne.x - this.x, this.ne.y - this.y, this.ne.z - this.z, var1)) {
         this.ne = null;
      }

   }

   public IsoObject getContainerItem(String var1) {
      int var2 = this.getObjects().size();
      IsoObject[] var3 = (IsoObject[])this.getObjects().getElements();

      for(int var4 = 0; var4 < var2; ++var4) {
         IsoObject var5 = var3[var4];
         if (var5.getContainer() != null && var1.equals(var5.getContainer().getType())) {
            return var5;
         }
      }

      return null;
   }

   /** @deprecated */
   @Deprecated
   public void StartFire() {
   }

   public void explode() {
      IsoFireManager.explode(this.getCell(), this, 100000);
   }

   public int getHourLastSeen() {
      return this.hourLastSeen;
   }

   public float getHoursSinceLastSeen() {
      return (float)GameTime.instance.getWorldAgeHours() - (float)this.hourLastSeen;
   }

   public void CalcVisibility(int var1) {
      IsoPlayer var2 = IsoPlayer.players[var1];
      ILighting var3 = this.lighting[var1];
      var3.bCanSee(false);
      var3.bCouldSee(false);
      if (GameServer.bServer || var2 != null && (!var2.isDead() || var2.ReanimatedCorpse != null)) {
         if (var2 != null) {
            IsoGameCharacter.LightInfo var4 = var2.getLightInfo2();
            IsoGridSquare var5 = var4.square;
            if (var5 != null) {
               IsoChunk var6 = this.getChunk();
               if (var6 != null) {
                  tempo.x = (float)this.x + 0.5F;
                  tempo.y = (float)this.y + 0.5F;
                  tempo2.x = var4.x;
                  tempo2.y = var4.y;
                  Vector2 var10000 = tempo2;
                  var10000.x -= tempo.x;
                  var10000 = tempo2;
                  var10000.y -= tempo.y;
                  Vector2 var7 = tempo;
                  float var8 = tempo2.getLength();
                  tempo2.normalize();
                  if (var2 instanceof IsoSurvivor) {
                     var2.setForwardDirection(var7);
                     var4.angleX = var7.x;
                     var4.angleY = var7.y;
                  }

                  var7.x = var4.angleX;
                  var7.y = var4.angleY;
                  var7.normalize();
                  float var9 = tempo2.dot(var7);
                  if (var5 == this) {
                     var9 = -1.0F;
                  }

                  float var11;
                  if (!GameServer.bServer) {
                     float var10 = var2.getStats().fatigue - 0.6F;
                     if (var10 < 0.0F) {
                        var10 = 0.0F;
                     }

                     var10 *= 2.5F;
                     var11 = 2.0F;
                     if (var2.Traits.HardOfHearing.isSet()) {
                        --var11;
                     }

                     if (var2.Traits.KeenHearing.isSet()) {
                        var11 += 3.0F;
                     }

                     var11 *= var2.getWornItemsHearingMultiplier();
                     if (var8 < var11 * (1.0F - var10) && !var2.Traits.Deaf.isSet()) {
                        var9 = -1.0F;
                     }
                  }

                  LosUtil.TestResults var16 = LosUtil.lineClearCached(this.getCell(), this.x, this.y, this.z, PZMath.fastfloor(var4.x), PZMath.fastfloor(var4.y), PZMath.fastfloor(var4.z), false, var1);
                  var11 = -0.2F;
                  var11 -= var2.getStats().fatigue - 0.6F;
                  if (var11 > -0.2F) {
                     var11 = -0.2F;
                  }

                  if (var2.getStats().fatigue >= 1.0F) {
                     var11 -= 0.2F;
                  }

                  if (var2.getMoodles().getMoodleLevel(MoodleType.Drunk) >= 2) {
                     var11 -= var2.getStats().Drunkenness * 0.002F;
                  }

                  if (var2.getMoodles().getMoodleLevel(MoodleType.Panic) == 4) {
                     var11 -= 0.2F;
                  }

                  if (var11 < -0.9F) {
                     var11 = -0.9F;
                  }

                  if (var2.Traits.EagleEyed.isSet()) {
                     var11 += 0.2F;
                  }

                  if (var2 instanceof IsoPlayer && var2.getVehicle() != null) {
                     var11 = 1.0F;
                  }

                  int var17;
                  if (!(var9 > var11) && var16 != LosUtil.TestResults.Blocked) {
                     var3.bCouldSee(true);
                     if (this.room != null && this.room.def != null && !this.room.def.bExplored) {
                        var17 = 10;
                        if (var4.square != null && var4.square.getBuilding() == this.room.building) {
                           var17 = 50;
                        }

                        if ((!GameServer.bServer || !(var2 instanceof IsoPlayer) || !((IsoPlayer)var2).isGhostMode()) && IsoUtils.DistanceManhatten(var4.x, var4.y, (float)this.x, (float)this.y) < (float)var17 && this.z == PZMath.fastfloor(var4.z)) {
                           if (GameServer.bServer) {
                              DebugLog.log(DebugType.Zombie, "bExplored room=" + this.room.def.ID);
                           }

                           this.room.def.bExplored = true;
                           this.room.onSee();
                           this.room.seen = 0;
                        }
                     }

                     if (!GameClient.bClient) {
                        Meta.instance.dealWithSquareSeen(this);
                     }

                     var3.bCanSee(true);
                     var3.bSeen(true);
                     var3.targetDarkMulti(1.0F);
                  } else {
                     if (var16 == LosUtil.TestResults.Blocked) {
                        var3.bCouldSee(false);
                     } else {
                        var3.bCouldSee(true);
                     }

                     if (!GameServer.bServer) {
                        if (var3.bSeen()) {
                           float var12 = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex());
                           if (!var3.bCouldSee()) {
                              var12 *= 0.5F;
                           } else {
                              var12 *= 0.94F;
                           }

                           if (this.room == null && var5.getRoom() == null) {
                              var3.targetDarkMulti(var12);
                           } else if (this.room != null && var5.getRoom() != null && this.room.building == var5.getRoom().building) {
                              if (this.room != var5.getRoom() && !var3.bCouldSee()) {
                                 var3.targetDarkMulti(0.0F);
                              } else {
                                 var3.targetDarkMulti(var12);
                              }
                           } else if (this.room == null) {
                              var3.targetDarkMulti(var12 / 2.0F);
                           } else if (var3.lampostTotalR() + var3.lampostTotalG() + var3.lampostTotalB() == 0.0F) {
                              var3.targetDarkMulti(0.0F);
                           }

                           if (this.room != null) {
                              var3.targetDarkMulti(var3.targetDarkMulti() * 0.7F);
                           }
                        } else {
                           var3.targetDarkMulti(0.0F);
                           var3.darkMulti(0.0F);
                        }
                     }
                  }

                  if (var9 > var11) {
                     var3.targetDarkMulti(var3.targetDarkMulti() * 0.85F);
                  }

                  if (!GameServer.bServer) {
                     for(var17 = 0; var17 < var4.torches.size(); ++var17) {
                        IsoGameCharacter.TorchInfo var13 = (IsoGameCharacter.TorchInfo)var4.torches.get(var17);
                        tempo2.x = var13.x;
                        tempo2.y = var13.y;
                        var10000 = tempo2;
                        var10000.x -= (float)this.x + 0.5F;
                        var10000 = tempo2;
                        var10000.y -= (float)this.y + 0.5F;
                        var8 = tempo2.getLength();
                        tempo2.normalize();
                        var7.x = var13.angleX;
                        var7.y = var13.angleY;
                        var7.normalize();
                        var9 = tempo2.dot(var7);
                        if (PZMath.fastfloor(var13.x) == this.getX() && PZMath.fastfloor(var13.y) == this.getY() && PZMath.fastfloor(var13.z) == this.getZ()) {
                           var9 = -1.0F;
                        }

                        boolean var14 = false;
                        if (IsoUtils.DistanceManhatten((float)this.getX(), (float)this.getY(), var13.x, var13.y) < var13.dist && (var13.bCone && var9 < -var13.dot || var9 == -1.0F || !var13.bCone && var9 < 0.8F)) {
                           var14 = true;
                        }

                        if ((var13.bCone && var8 < var13.dist || !var13.bCone && var8 < var13.dist) && var3.bCanSee() && var14 && this.z == PZMath.fastfloor(var2.getZ())) {
                           float var15 = var8 / var13.dist;
                           if (var15 > 1.0F) {
                              var15 = 1.0F;
                           }

                           if (var15 < 0.0F) {
                              var15 = 0.0F;
                           }

                           var3.targetDarkMulti(var3.targetDarkMulti() + var13.strength * (1.0F - var15) * 3.0F);
                           if (var3.targetDarkMulti() > 2.5F) {
                              var3.targetDarkMulti(2.5F);
                           }

                           torchTimer = var4.time;
                        }
                     }

                  }
               }
            }
         }
      } else {
         var3.bSeen(true);
         var3.bCanSee(true);
         var3.bCouldSee(true);
      }
   }

   private LosUtil.TestResults DoDiagnalCheck(int var1, int var2, int var3, boolean var4) {
      LosUtil.TestResults var5 = this.testVisionAdjacent(var1, 0, var3, false, var4);
      if (var5 == LosUtil.TestResults.Blocked) {
         return LosUtil.TestResults.Blocked;
      } else {
         LosUtil.TestResults var6 = this.testVisionAdjacent(0, var2, var3, false, var4);
         if (var6 == LosUtil.TestResults.Blocked) {
            return LosUtil.TestResults.Blocked;
         } else {
            return var5 != LosUtil.TestResults.ClearThroughWindow && var6 != LosUtil.TestResults.ClearThroughWindow ? this.testVisionAdjacent(var1, var2, var3, false, var4) : LosUtil.TestResults.ClearThroughWindow;
         }
      }
   }

   boolean HasNoCharacters() {
      int var1;
      for(var1 = 0; var1 < this.MovingObjects.size(); ++var1) {
         if (this.MovingObjects.get(var1) instanceof IsoGameCharacter) {
            return false;
         }
      }

      for(var1 = 0; var1 < this.SpecialObjects.size(); ++var1) {
         if (this.SpecialObjects.get(var1) instanceof IsoBarricade) {
            return false;
         }
      }

      return true;
   }

   public IsoZombie getZombie() {
      for(int var1 = 0; var1 < this.MovingObjects.size(); ++var1) {
         if (this.MovingObjects.get(var1) instanceof IsoZombie) {
            return (IsoZombie)this.MovingObjects.get(var1);
         }
      }

      return null;
   }

   public IsoPlayer getPlayer() {
      for(int var1 = 0; var1 < this.MovingObjects.size(); ++var1) {
         if (this.MovingObjects.get(var1) instanceof IsoPlayer) {
            return (IsoPlayer)this.MovingObjects.get(var1);
         }
      }

      return null;
   }

   public static float getDarkStep() {
      return darkStep;
   }

   public static void setDarkStep(float var0) {
      darkStep = var0;
   }

   public static float getRecalcLightTime() {
      return RecalcLightTime;
   }

   public static void setRecalcLightTime(float var0) {
      RecalcLightTime = var0;
      if (PerformanceSettings.FBORenderChunk && var0 < 0.0F) {
         ++Core.dirtyGlobalLightsCount;
      }

   }

   public static int getLightcache() {
      return lightcache;
   }

   public static void setLightcache(int var0) {
      lightcache = var0;
   }

   public boolean isCouldSee(int var1) {
      return this.lighting[var1].bCouldSee();
   }

   public void setCouldSee(int var1, boolean var2) {
      this.lighting[var1].bCouldSee(var2);
   }

   public boolean isCanSee(int var1) {
      return this.lighting[var1].bCanSee();
   }

   public void setCanSee(int var1, boolean var2) {
      this.lighting[var1].bCanSee(var2);
   }

   public IsoCell getCell() {
      return IsoWorld.instance.CurrentCell;
   }

   public IsoGridSquare getE() {
      return this.e;
   }

   public void setE(IsoGridSquare var1) {
      this.e = var1;
   }

   public ArrayList<Float> getLightInfluenceB() {
      return this.LightInfluenceB;
   }

   public void setLightInfluenceB(ArrayList<Float> var1) {
      this.LightInfluenceB = var1;
   }

   public ArrayList<Float> getLightInfluenceG() {
      return this.LightInfluenceG;
   }

   public void setLightInfluenceG(ArrayList<Float> var1) {
      this.LightInfluenceG = var1;
   }

   public ArrayList<Float> getLightInfluenceR() {
      return this.LightInfluenceR;
   }

   public void setLightInfluenceR(ArrayList<Float> var1) {
      this.LightInfluenceR = var1;
   }

   public ArrayList<IsoMovingObject> getStaticMovingObjects() {
      return this.StaticMovingObjects;
   }

   public ArrayList<IsoMovingObject> getMovingObjects() {
      return this.MovingObjects;
   }

   public IsoGridSquare getN() {
      return this.n;
   }

   public void setN(IsoGridSquare var1) {
      this.n = var1;
   }

   public PZArrayList<IsoObject> getObjects() {
      return this.Objects;
   }

   public PropertyContainer getProperties() {
      return this.Properties;
   }

   public IsoRoom getRoom() {
      return this.roomID == -1L ? null : this.room;
   }

   public void setRoom(IsoRoom var1) {
      this.room = var1;
   }

   public IsoBuilding getBuilding() {
      IsoRoom var1 = this.getRoom();
      return var1 != null ? var1.getBuilding() : null;
   }

   public IsoGridSquare getS() {
      return this.s;
   }

   public void setS(IsoGridSquare var1) {
      this.s = var1;
   }

   public ArrayList<IsoObject> getSpecialObjects() {
      return this.SpecialObjects;
   }

   public IsoGridSquare getW() {
      return this.w;
   }

   public void setW(IsoGridSquare var1) {
      this.w = var1;
   }

   public float getLampostTotalR() {
      return this.lighting[0].lampostTotalR();
   }

   public void setLampostTotalR(float var1) {
      this.lighting[0].lampostTotalR(var1);
   }

   public float getLampostTotalG() {
      return this.lighting[0].lampostTotalG();
   }

   public void setLampostTotalG(float var1) {
      this.lighting[0].lampostTotalG(var1);
   }

   public float getLampostTotalB() {
      return this.lighting[0].lampostTotalB();
   }

   public void setLampostTotalB(float var1) {
      this.lighting[0].lampostTotalB(var1);
   }

   public boolean isSeen(int var1) {
      return this.lighting[var1].bSeen();
   }

   public void setIsSeen(int var1, boolean var2) {
      this.lighting[var1].bSeen(var2);
   }

   public float getDarkMulti(int var1) {
      return this.lighting[var1].darkMulti();
   }

   public void setDarkMulti(int var1, float var2) {
      this.lighting[var1].darkMulti(var2);
   }

   public float getTargetDarkMulti(int var1) {
      return this.lighting[var1].targetDarkMulti();
   }

   public void setTargetDarkMulti(int var1, float var2) {
      this.lighting[var1].targetDarkMulti(var2);
   }

   public void setX(int var1) {
      this.x = var1;
      this.CachedScreenValue = -1;
   }

   public void setY(int var1) {
      this.y = var1;
      this.CachedScreenValue = -1;
   }

   public void setZ(int var1) {
      this.z = var1;
      this.CachedScreenValue = -1;
   }

   public ArrayList<IsoGameCharacter> getDeferedCharacters() {
      return this.DeferedCharacters;
   }

   public void addDeferredCharacter(IsoGameCharacter var1) {
      if (this.DeferredCharacterTick != this.getCell().DeferredCharacterTick) {
         if (!this.DeferedCharacters.isEmpty()) {
            this.DeferedCharacters.clear();
         }

         this.DeferredCharacterTick = this.getCell().DeferredCharacterTick;
      }

      this.DeferedCharacters.add(var1);
   }

   public boolean isCacheIsFree() {
      return this.CacheIsFree;
   }

   public void setCacheIsFree(boolean var1) {
      this.CacheIsFree = var1;
   }

   public boolean isCachedIsFree() {
      return this.CachedIsFree;
   }

   public void setCachedIsFree(boolean var1) {
      this.CachedIsFree = var1;
   }

   public static boolean isbDoSlowPathfinding() {
      return bDoSlowPathfinding;
   }

   public static void setbDoSlowPathfinding(boolean var0) {
      bDoSlowPathfinding = var0;
   }

   public boolean isSolidFloorCached() {
      return this.SolidFloorCached;
   }

   public void setSolidFloorCached(boolean var1) {
      this.SolidFloorCached = var1;
   }

   public boolean isSolidFloor() {
      return this.SolidFloor;
   }

   public void setSolidFloor(boolean var1) {
      this.SolidFloor = var1;
   }

   public static ColorInfo getDefColorInfo() {
      return defColorInfo;
   }

   public boolean isOutside() {
      return this.Properties.Is(IsoFlagType.exterior);
   }

   public boolean HasPushable() {
      int var1 = this.MovingObjects.size();

      for(int var2 = 0; var2 < var1; ++var2) {
         if (this.MovingObjects.get(var2) instanceof IsoPushableObject) {
            return true;
         }
      }

      return false;
   }

   public void setRoomID(long var1) {
      this.roomID = var1;
      if (var1 != -1L) {
         this.getProperties().UnSet(IsoFlagType.exterior);
         this.room = this.chunk.getRoom(var1);
      }

   }

   public long getRoomID() {
      return this.roomID;
   }

   public boolean getCanSee(int var1) {
      return this.lighting[var1].bCanSee();
   }

   public boolean getSeen(int var1) {
      return this.lighting[var1].bSeen();
   }

   public IsoChunk getChunk() {
      return this.chunk;
   }

   public IsoObject getDoorOrWindow(boolean var1) {
      for(int var2 = this.SpecialObjects.size() - 1; var2 >= 0; --var2) {
         IsoObject var3 = (IsoObject)this.SpecialObjects.get(var2);
         if (var3 instanceof IsoDoor && ((IsoDoor)var3).north == var1) {
            return var3;
         }

         if (var3 instanceof IsoThumpable && ((IsoThumpable)var3).north == var1 && (((IsoThumpable)var3).isDoor() || ((IsoThumpable)var3).isWindow())) {
            return var3;
         }

         if (var3 instanceof IsoWindow && ((IsoWindow)var3).north == var1) {
            return var3;
         }
      }

      return null;
   }

   public IsoObject getDoorOrWindowOrWindowFrame(IsoDirections var1, boolean var2) {
      for(int var3 = this.Objects.size() - 1; var3 >= 0; --var3) {
         IsoObject var4 = (IsoObject)this.Objects.get(var3);
         IsoDoor var5 = (IsoDoor)Type.tryCastTo(var4, IsoDoor.class);
         IsoThumpable var6 = (IsoThumpable)Type.tryCastTo(var4, IsoThumpable.class);
         IsoWindow var7 = (IsoWindow)Type.tryCastTo(var4, IsoWindow.class);
         if (var5 != null && var5.getSpriteEdge(var2) == var1) {
            return var4;
         }

         if (var6 != null && var6.getSpriteEdge(var2) == var1) {
            return var4;
         }

         if (var7 != null) {
            if (var7.north && var1 == IsoDirections.N) {
               return var4;
            }

            if (!var7.north && var1 == IsoDirections.W) {
               return var4;
            }
         }

         if (var4 instanceof IsoWindowFrame var8) {
            if (var8.getNorth() && var1 == IsoDirections.N) {
               return var4;
            }

            if (!var8.getNorth() && var1 == IsoDirections.W) {
               return var4;
            }
         }
      }

      return null;
   }

   public IsoObject getOpenDoor(IsoDirections var1) {
      for(int var2 = 0; var2 < this.SpecialObjects.size(); ++var2) {
         IsoObject var3 = (IsoObject)this.SpecialObjects.get(var2);
         IsoDoor var4 = (IsoDoor)Type.tryCastTo(var3, IsoDoor.class);
         IsoThumpable var5 = (IsoThumpable)Type.tryCastTo(var3, IsoThumpable.class);
         if (var4 != null && var4.open && var4.getSpriteEdge(false) == var1) {
            return var4;
         }

         if (var5 != null && var5.open && var5.getSpriteEdge(false) == var1) {
            return var5;
         }
      }

      return null;
   }

   public void removeWorldObject(IsoWorldInventoryObject var1) {
      if (var1 != null) {
         var1.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_ITEM_REMOVE | FBORenderChunk.DIRTY_OBJECT_REMOVE);
         var1.removeFromWorld();
         var1.removeFromSquare();
      }
   }

   public void removeAllWorldObjects() {
      for(int var1 = 0; var1 < this.getWorldObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)this.getWorldObjects().get(var1);
         var2.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_ITEM_REMOVE | FBORenderChunk.DIRTY_OBJECT_REMOVE);
         var2.removeFromWorld();
         var2.removeFromSquare();
         --var1;
      }

   }

   public ArrayList<IsoWorldInventoryObject> getWorldObjects() {
      return this.WorldObjects;
   }

   public PZArrayList<IsoObject> getLocalTemporaryObjects() {
      return this.localTemporaryObjects;
   }

   public KahluaTable getModData() {
      if (this.table == null) {
         this.table = LuaManager.platform.newTable();
      }

      return this.table;
   }

   public boolean hasModData() {
      return this.table != null && !this.table.isEmpty();
   }

   public ZomboidBitFlag getHasTypes() {
      return this.hasTypes;
   }

   public void setVertLight(int var1, int var2, int var3) {
      this.lighting[var3].lightverts(var1, var2);
   }

   public int getVertLight(int var1, int var2) {
      return this.lighting[var2].lightverts(var1);
   }

   public void setRainDrop(IsoRaindrop var1) {
      this.RainDrop = var1;
   }

   public IsoRaindrop getRainDrop() {
      return this.RainDrop;
   }

   public void setRainSplash(IsoRainSplash var1) {
      this.RainSplash = var1;
   }

   public IsoRainSplash getRainSplash() {
      return this.RainSplash;
   }

   public Zone getZone() {
      return this.zone;
   }

   public String getZoneType() {
      return this.zone != null ? this.zone.getType() : null;
   }

   public boolean isOverlayDone() {
      return this.overlayDone;
   }

   public void setOverlayDone(boolean var1) {
      this.overlayDone = var1;
   }

   public ErosionData.Square getErosionData() {
      if (this.erosion == null) {
         this.erosion = new ErosionData.Square();
      }

      return this.erosion;
   }

   public void disableErosion() {
      ErosionData.Square var1 = this.getErosionData();
      if (var1 != null && !var1.doNothing) {
         var1.doNothing = true;
      }

   }

   public void removeErosionObject(String var1) {
      if (this.erosion != null) {
         if ("WallVines".equals(var1)) {
            for(int var2 = 0; var2 < this.erosion.regions.size(); ++var2) {
               ErosionCategory.Data var3 = (ErosionCategory.Data)this.erosion.regions.get(var2);
               if (var3.regionID == 2 && var3.categoryID == 0) {
                  this.erosion.regions.remove(var2);
                  break;
               }
            }
         }

      }
   }

   public void syncIsoTrap(HandWeapon var1) {
      AddExplosiveTrapPacket var2 = new AddExplosiveTrapPacket();
      var2.set(var1, this);
      ByteBufferWriter var3 = GameClient.connection.startPacket();
      PacketTypes.PacketType.AddExplosiveTrap.doPacket(var3);
      var2.write(var3);
      PacketTypes.PacketType.AddExplosiveTrap.send(GameClient.connection);
   }

   public void drawCircleExplosion(int var1, IsoTrap var2, IsoTrap.ExplosionMode var3) {
      if (var1 > 15) {
         var1 = 15;
      }

      for(int var4 = this.getX() - var1; var4 <= this.getX() + var1; ++var4) {
         for(int var5 = this.getY() - var1; var5 <= this.getY() + var1; ++var5) {
            if (!(IsoUtils.DistanceTo((float)var4 + 0.5F, (float)var5 + 0.5F, (float)this.getX() + 0.5F, (float)this.getY() + 0.5F) > (float)var1)) {
               LosUtil.TestResults var6 = LosUtil.lineClear(this.getCell(), PZMath.fastfloor(var2.getX()), PZMath.fastfloor(var2.getY()), PZMath.fastfloor(var2.getZ()), var4, var5, this.z, false);
               if (var6 != LosUtil.TestResults.Blocked && var6 != LosUtil.TestResults.ClearThroughClosedDoor) {
                  IsoGridSquare var7 = this.getCell().getGridSquare(var4, var5, this.getZ());
                  if (var7 != null && NonPvpZone.getNonPvpZone(var7.getX(), var7.getY()) == null) {
                     if (var3 == IsoTrap.ExplosionMode.Smoke) {
                        if (!GameClient.bClient && Rand.Next(2) == 0) {
                           IsoFireManager.StartSmoke(this.getCell(), var7, true, 40, 0);
                        }

                        var7.smoke();
                     }

                     if (var3 == IsoTrap.ExplosionMode.Explosion) {
                        if (!GameClient.bClient && var2.getExplosionPower() > 0 && Rand.Next(80 - var2.getExplosionPower()) <= 0) {
                           var7.Burn();
                        }

                        var7.explosion(var2);
                        if (!GameClient.bClient && var2.getExplosionPower() > 0 && Rand.Next(100 - var2.getExplosionPower()) == 0) {
                           IsoFireManager.StartFire(this.getCell(), var7, true, 20);
                        }
                     }

                     if (var3 == IsoTrap.ExplosionMode.Fire && !GameClient.bClient && Rand.Next(100 - var2.getFirePower()) == 0) {
                        IsoFireManager.StartFire(this.getCell(), var7, true, 40);
                     }

                     if (var3 == IsoTrap.ExplosionMode.Sensor) {
                        var7.setTrapPositionX(this.getX());
                        var7.setTrapPositionY(this.getY());
                        var7.setTrapPositionZ(this.getZ());
                     }
                  }
               }
            }
         }
      }

   }

   public void explosion(IsoTrap var1) {
      if (!GameServer.bServer || !var1.isInstantExplosion()) {
         for(int var2 = 0; var2 < this.getMovingObjects().size(); ++var2) {
            IsoMovingObject var3 = (IsoMovingObject)this.getMovingObjects().get(var2);
            if ((!(var3 instanceof IsoPlayer) || ServerOptions.getInstance().PVP.getValue()) && var3 instanceof IsoGameCharacter) {
               if (GameServer.bServer || !(var3 instanceof IsoZombie) || ((IsoZombie)var3).isLocal()) {
                  int var4 = Math.min(var1.getExplosionPower(), 80);
                  var3.Hit((HandWeapon)InventoryItemFactory.CreateItem("Base.Axe"), IsoWorld.instance.CurrentCell.getFakeZombieForHit(), Rand.Next((float)var4 / 30.0F, (float)var4 / 30.0F * 2.0F) + var1.getExtraDamage(), false, 1.0F);
                  if (var1.getExplosionPower() > 0) {
                     boolean var5 = !(var3 instanceof IsoZombie);

                     while(var5) {
                        var5 = false;
                        BodyPart var6 = ((IsoGameCharacter)var3).getBodyDamage().getBodyPart(BodyPartType.FromIndex(Rand.Next(15)));
                        var6.setBurned();
                        if (Rand.Next((100 - var4) / 2) == 0) {
                           var5 = true;
                        }
                     }
                  }
               }

               if (GameClient.bClient && var3 instanceof IsoZombie && ((IsoZombie)var3).isRemoteZombie()) {
                  var3.Hit((HandWeapon)InventoryItemFactory.CreateItem("Base.Axe"), IsoWorld.instance.CurrentCell.getFakeZombieForHit(), 0.0F, true, 0.0F);
               }
            }
         }

      }
   }

   public void smoke() {
      for(int var1 = 0; var1 < this.getMovingObjects().size(); ++var1) {
         IsoMovingObject var2 = (IsoMovingObject)this.getMovingObjects().get(var1);
         if (var2 instanceof IsoZombie) {
            ((IsoZombie)var2).setTarget((IsoMovingObject)null);
            ((IsoZombie)var2).changeState(ZombieIdleState.instance());
         }
      }

   }

   public void explodeTrap() {
      IsoGridSquare var1 = this.getCell().getGridSquare(this.getTrapPositionX(), this.getTrapPositionY(), this.getTrapPositionZ());
      if (var1 != null) {
         for(int var2 = 0; var2 < var1.getObjects().size(); ++var2) {
            IsoObject var3 = (IsoObject)var1.getObjects().get(var2);
            if (var3 instanceof IsoTrap) {
               IsoTrap var4 = (IsoTrap)var3;
               var4.triggerExplosion(false);
               IsoGridSquare var5 = null;
               int var6 = var4.getSensorRange();

               for(int var7 = var1.getX() - var6; var7 <= var1.getX() + var6; ++var7) {
                  for(int var8 = var1.getY() - var6; var8 <= var1.getY() + var6; ++var8) {
                     if (IsoUtils.DistanceTo((float)var7 + 0.5F, (float)var8 + 0.5F, (float)var1.getX() + 0.5F, (float)var1.getY() + 0.5F) <= (float)var6) {
                        var5 = this.getCell().getGridSquare(var7, var8, this.getZ());
                        if (var5 != null) {
                           var5.setTrapPositionX(-1);
                           var5.setTrapPositionY(-1);
                           var5.setTrapPositionZ(-1);
                        }
                     }
                  }
               }

               return;
            }
         }
      }

   }

   public int getTrapPositionX() {
      return this.trapPositionX;
   }

   public void setTrapPositionX(int var1) {
      this.trapPositionX = var1;
   }

   public int getTrapPositionY() {
      return this.trapPositionY;
   }

   public void setTrapPositionY(int var1) {
      this.trapPositionY = var1;
   }

   public int getTrapPositionZ() {
      return this.trapPositionZ;
   }

   public void setTrapPositionZ(int var1) {
      this.trapPositionZ = var1;
   }

   public boolean haveElectricity() {
      if (!SandboxOptions.getInstance().AllowExteriorGenerator.getValue() && this.Is(IsoFlagType.exterior)) {
         return false;
      } else {
         return this.chunk != null && this.chunk.isGeneratorPoweringSquare(this.x, this.y, this.z);
      }
   }

   /** @deprecated */
   @Deprecated
   public void setHaveElectricity(boolean var1) {
      if (this.getObjects() != null) {
         for(int var2 = 0; var2 < this.getObjects().size(); ++var2) {
            if (this.getObjects().get(var2) instanceof IsoLightSwitch) {
               ((IsoLightSwitch)this.getObjects().get(var2)).update();
            }
         }
      }

   }

   public IsoGenerator getGenerator() {
      if (this.getSpecialObjects() != null) {
         for(int var1 = 0; var1 < this.getSpecialObjects().size(); ++var1) {
            if (this.getSpecialObjects().get(var1) instanceof IsoGenerator) {
               return (IsoGenerator)this.getSpecialObjects().get(var1);
            }
         }
      }

      return null;
   }

   public void stopFire() {
      IsoFireManager.RemoveAllOn(this);
      this.getProperties().Set(IsoFlagType.burntOut);
      this.getProperties().UnSet(IsoFlagType.burning);
      this.burntOut = true;
   }

   public void transmitStopFire() {
      if (GameClient.bClient) {
         GameClient.sendStopFire(this);
      }

   }

   public long playSound(String var1) {
      BaseSoundEmitter var2 = IsoWorld.instance.getFreeEmitter((float)this.x + 0.5F, (float)this.y + 0.5F, (float)this.z);
      return var2.playSound(var1);
   }

   public long playSoundLocal(String var1) {
      BaseSoundEmitter var2 = IsoWorld.instance.getFreeEmitter((float)this.x + 0.5F, (float)this.y + 0.5F, (float)this.z);
      return var2.playSoundImpl(var1, this);
   }

   /** @deprecated */
   @Deprecated
   public long playSound(String var1, boolean var2) {
      BaseSoundEmitter var3 = IsoWorld.instance.getFreeEmitter((float)this.x + 0.5F, (float)this.y + 0.5F, (float)this.z);
      return var3.playSound(var1, var2);
   }

   public void FixStackableObjects() {
      IsoObject var1 = null;

      for(int var2 = 0; var2 < this.Objects.size(); ++var2) {
         IsoObject var3 = (IsoObject)this.Objects.get(var2);
         if (!(var3 instanceof IsoWorldInventoryObject) && var3.sprite != null) {
            PropertyContainer var4 = var3.sprite.getProperties();
            if (var4.getStackReplaceTileOffset() != 0) {
               var3.sprite = IsoSprite.getSprite(IsoSpriteManager.instance, var3.sprite.ID + var4.getStackReplaceTileOffset());
               if (var3.sprite == null) {
                  continue;
               }

               var4 = var3.sprite.getProperties();
            }

            if (var4.isTable() || var4.isTableTop()) {
               float var5 = var4.isSurfaceOffset() ? (float)var4.getSurface() : 0.0F;
               if (var1 != null) {
                  var3.setRenderYOffset(var1.getRenderYOffset() + var1.getSurfaceOffset() - var5);
               } else {
                  var3.setRenderYOffset(0.0F - var5);
               }
            }

            if (var4.isTable()) {
               var1 = var3;
            }

            if (var3 instanceof IsoLightSwitch && var4.isTableTop() && var1 != null && !var4.Is("IgnoreSurfaceSnap")) {
               int var14 = PZMath.tryParseInt(var4.Val("Noffset"), 0);
               int var6 = PZMath.tryParseInt(var4.Val("Soffset"), 0);
               int var7 = PZMath.tryParseInt(var4.Val("Woffset"), 0);
               int var8 = PZMath.tryParseInt(var4.Val("Eoffset"), 0);
               String var9 = var4.Val("Facing");
               PropertyContainer var10 = var1.getProperties();
               String var11 = var10.Val("Facing");
               if (!StringUtils.isNullOrWhitespace(var11) && !var11.equals(var9)) {
                  int var12 = 0;
                  if ("N".equals(var11)) {
                     if (var14 != 0) {
                        var12 = var14;
                     } else if (var6 != 0) {
                        var12 = var6;
                     }
                  } else if ("S".equals(var11)) {
                     if (var6 != 0) {
                        var12 = var6;
                     } else if (var14 != 0) {
                        var12 = var14;
                     }
                  } else if ("W".equals(var11)) {
                     if (var7 != 0) {
                        var12 = var7;
                     } else if (var8 != 0) {
                        var12 = var8;
                     }
                  } else if ("E".equals(var11)) {
                     if (var8 != 0) {
                        var12 = var8;
                     } else if (var7 != 0) {
                        var12 = var7;
                     }
                  }

                  if (var12 != 0) {
                     IsoSprite var13 = IsoSpriteManager.instance.getSprite(var3.sprite.ID + var12);
                     if (var13 != null) {
                        var3.setSprite(var13);
                     }
                  }
               }
            }
         }
      }

   }

   public void fixPlacedItemRenderOffsets() {
      IsoObject[] var1 = (IsoObject[])this.Objects.getElements();
      int var2 = this.Objects.size();
      int var3 = 0;

      int var4;
      IsoObject var5;
      for(var4 = 0; var4 < var2; ++var4) {
         var5 = var1[var4];
         int var6 = PZMath.roundToInt(var5.getSurfaceOffsetNoTable());
         if (!((float)var6 <= 0.0F) && !PZArrayUtil.contains(SURFACE_OFFSETS, var3, var6)) {
            SURFACE_OFFSETS[var3++] = var6;
         }
      }

      if (var3 == 0) {
         SURFACE_OFFSETS[var3++] = 0;
      }

      for(var4 = 0; var4 < var2; ++var4) {
         var5 = var1[var4];
         IsoWorldInventoryObject var10 = (IsoWorldInventoryObject)Type.tryCastTo(var5, IsoWorldInventoryObject.class);
         if (var10 != null) {
            int var7 = PZMath.roundToInt(var10.zoff * 96.0F);
            int var8 = 0;

            for(int var9 = 0; var9 < var3; ++var9) {
               if (var7 <= SURFACE_OFFSETS[var9]) {
                  var8 = SURFACE_OFFSETS[var9];
                  break;
               }

               var8 = SURFACE_OFFSETS[var9];
               if (var9 < var3 - 1 && var7 < SURFACE_OFFSETS[var9 + 1]) {
                  break;
               }
            }

            var10.zoff = (float)var8 / 96.0F;
         }
      }

   }

   public BaseVehicle getVehicleContainer() {
      int var1 = (int)(((float)this.x - 4.0F) / 8.0F);
      int var2 = (int)(((float)this.y - 4.0F) / 8.0F);
      int var3 = (int)Math.ceil((double)(((float)this.x + 4.0F) / 8.0F));
      int var4 = (int)Math.ceil((double)(((float)this.y + 4.0F) / 8.0F));

      for(int var5 = var2; var5 < var4; ++var5) {
         for(int var6 = var1; var6 < var3; ++var6) {
            IsoChunk var7 = GameServer.bServer ? ServerMap.instance.getChunk(var6, var5) : IsoWorld.instance.CurrentCell.getChunk(var6, var5);
            if (var7 != null) {
               for(int var8 = 0; var8 < var7.vehicles.size(); ++var8) {
                  BaseVehicle var9 = (BaseVehicle)var7.vehicles.get(var8);
                  if (var9.isIntersectingSquare(this.x, this.y, this.z)) {
                     return var9;
                  }
               }
            }
         }
      }

      return null;
   }

   public boolean isVehicleIntersecting() {
      int var1 = (int)(((float)this.x - 4.0F) / 8.0F);
      int var2 = (int)(((float)this.y - 4.0F) / 8.0F);
      int var3 = (int)Math.ceil((double)(((float)this.x + 4.0F) / 8.0F));
      int var4 = (int)Math.ceil((double)(((float)this.y + 4.0F) / 8.0F));

      for(int var5 = var2; var5 < var4; ++var5) {
         for(int var6 = var1; var6 < var3; ++var6) {
            IsoChunk var7 = GameServer.bServer ? ServerMap.instance.getChunk(var6, var5) : IsoWorld.instance.CurrentCell.getChunk(var6, var5);
            if (var7 != null) {
               for(int var8 = 0; var8 < var7.vehicles.size(); ++var8) {
                  BaseVehicle var9 = (BaseVehicle)var7.vehicles.get(var8);
                  if (var9.isIntersectingSquare(this.x, this.y, this.z)) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   public boolean isVehicleIntersectingCrops() {
      if (!this.hasFarmingPlant()) {
         return false;
      } else {
         int var1 = (int)(((float)this.x - 4.0F) / 8.0F);
         int var2 = (int)(((float)this.y - 4.0F) / 8.0F);
         int var3 = (int)Math.ceil((double)(((float)this.x + 4.0F) / 8.0F));
         int var4 = (int)Math.ceil((double)(((float)this.y + 4.0F) / 8.0F));

         for(int var5 = var2; var5 < var4; ++var5) {
            for(int var6 = var1; var6 < var3; ++var6) {
               IsoChunk var7 = GameServer.bServer ? ServerMap.instance.getChunk(var6, var5) : IsoWorld.instance.CurrentCell.getChunk(var6, var5);
               if (var7 != null) {
                  for(int var8 = 0; var8 < var7.vehicles.size(); ++var8) {
                     BaseVehicle var9 = (BaseVehicle)var7.vehicles.get(var8);
                     if (var9.isIntersectingSquare(this) && !var9.notKillCrops()) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   public void checkForIntersectingCrops(BaseVehicle var1) {
      if (this.hasFarmingPlant() && !var1.notKillCrops() && var1.isIntersectingSquare(this)) {
         this.destroyFarmingPlant();
      }

   }

   public IsoCompost getCompost() {
      if (this.getSpecialObjects() != null) {
         for(int var1 = 0; var1 < this.getSpecialObjects().size(); ++var1) {
            if (this.getSpecialObjects().get(var1) instanceof IsoCompost) {
               return (IsoCompost)this.getSpecialObjects().get(var1);
            }
         }
      }

      return null;
   }

   public void setIsoWorldRegion(IsoWorldRegion var1) {
      this.hasSetIsoWorldRegion = var1 != null;
      this.isoWorldRegion = var1;
   }

   public IWorldRegion getIsoWorldRegion() {
      if (this.z < 0) {
         return null;
      } else if (GameServer.bServer) {
         return IsoRegions.getIsoWorldRegion(this.x, this.y, this.z);
      } else {
         if (!this.hasSetIsoWorldRegion) {
            this.isoWorldRegion = IsoRegions.getIsoWorldRegion(this.x, this.y, this.z);
            this.hasSetIsoWorldRegion = true;
         }

         return this.isoWorldRegion;
      }
   }

   public void ResetIsoWorldRegion() {
      this.isoWorldRegion = null;
      this.hasSetIsoWorldRegion = false;
   }

   public boolean isInARoom() {
      return this.getRoom() != null || this.getIsoWorldRegion() != null && this.getIsoWorldRegion().isPlayerRoom();
   }

   public int getRoomSize() {
      if (this.getRoom() != null) {
         return this.getRoom().getSquares().size();
      } else {
         return this.getIsoWorldRegion() != null && this.getIsoWorldRegion().isPlayerRoom() ? this.getIsoWorldRegion().getSquareSize() : -1;
      }
   }

   public int getWallType() {
      int var1 = 0;
      if (this.getProperties().Is(IsoFlagType.WallN)) {
         var1 |= 1;
      }

      if (this.getProperties().Is(IsoFlagType.WallW)) {
         var1 |= 4;
      }

      if (this.getProperties().Is(IsoFlagType.WallNW)) {
         var1 |= 5;
      }

      IsoGridSquare var2 = this.nav[IsoDirections.E.index()];
      if (var2 != null && (var2.getProperties().Is(IsoFlagType.WallW) || var2.getProperties().Is(IsoFlagType.WallNW))) {
         var1 |= 8;
      }

      IsoGridSquare var3 = this.nav[IsoDirections.S.index()];
      if (var3 != null && (var3.getProperties().Is(IsoFlagType.WallN) || var3.getProperties().Is(IsoFlagType.WallNW))) {
         var1 |= 2;
      }

      return var1;
   }

   public int getPuddlesDir() {
      byte var1 = IsoGridSquare.PuddlesDirection.PUDDLES_DIR_ALL;
      if (this.isInARoom()) {
         return IsoGridSquare.PuddlesDirection.PUDDLES_DIR_NONE;
      } else {
         for(int var2 = 0; var2 < this.getObjects().size(); ++var2) {
            IsoObject var3 = (IsoObject)this.getObjects().get(var2);
            if (var3.AttachedAnimSprite != null) {
               for(int var4 = 0; var4 < var3.AttachedAnimSprite.size(); ++var4) {
                  IsoSprite var5 = ((IsoSpriteInstance)var3.AttachedAnimSprite.get(var4)).parentSprite;
                  if (var5.name != null) {
                     if (var5.name.equals("street_trafficlines_01_2") || var5.name.equals("street_trafficlines_01_6") || var5.name.equals("street_trafficlines_01_22") || var5.name.equals("street_trafficlines_01_32")) {
                        var1 = IsoGridSquare.PuddlesDirection.PUDDLES_DIR_NW;
                     }

                     if (var5.name.equals("street_trafficlines_01_4") || var5.name.equals("street_trafficlines_01_0") || var5.name.equals("street_trafficlines_01_16")) {
                        var1 = IsoGridSquare.PuddlesDirection.PUDDLES_DIR_NE;
                     }
                  }
               }
            }
         }

         return var1;
      }
   }

   public boolean haveFire() {
      int var1 = this.Objects.size();
      IsoObject[] var2 = (IsoObject[])this.Objects.getElements();

      for(int var3 = 0; var3 < var1; ++var3) {
         IsoObject var4 = var2[var3];
         if (var4 instanceof IsoFire) {
            return true;
         }
      }

      return false;
   }

   public IsoBuilding getRoofHideBuilding() {
      return this.roofHideBuilding;
   }

   public IsoGridSquare getAdjacentSquare(IsoDirections var1) {
      return this.nav[var1.index()];
   }

   public IsoGridSquare getAdjacentPathSquare(IsoDirections var1) {
      switch (var1) {
         case NW:
            return this.nw;
         case N:
            return this.n;
         case NE:
            return this.ne;
         case W:
            return this.w;
         case E:
            return this.e;
         case SW:
            return this.sw;
         case S:
            return this.s;
         case SE:
            return this.se;
         default:
            return null;
      }
   }

   public float getApparentZ(float var1, float var2) {
      var1 = PZMath.clamp(var1, 0.0F, 1.0F);
      var2 = PZMath.clamp(var2, 0.0F, 1.0F);
      float var3 = PerformanceSettings.FBORenderChunk ? 0.1F : 0.0F;
      if (this.Has(IsoObjectType.stairsTN)) {
         return (float)this.getZ() + PZMath.lerp(0.6666F + var3, 1.0F, 1.0F - var2);
      } else if (this.Has(IsoObjectType.stairsTW)) {
         return (float)this.getZ() + PZMath.lerp(0.6666F + var3, 1.0F, 1.0F - var1);
      } else if (this.Has(IsoObjectType.stairsMN)) {
         return (float)this.getZ() + PZMath.lerp(0.3333F + var3, 0.6666F + var3, 1.0F - var2);
      } else if (this.Has(IsoObjectType.stairsMW)) {
         return (float)this.getZ() + PZMath.lerp(0.3333F + var3, 0.6666F + var3, 1.0F - var1);
      } else if (this.Has(IsoObjectType.stairsBN)) {
         return (float)this.getZ() + PZMath.lerp(0.01F, 0.3333F + var3, 1.0F - var2);
      } else {
         return this.Has(IsoObjectType.stairsBW) ? (float)this.getZ() + PZMath.lerp(0.01F, 0.3333F + var3, 1.0F - var1) : (float)this.getZ() + this.getSlopedSurfaceHeight(var1, var2);
      }
   }

   public IsoDirections getStairsDirection() {
      if (this.HasStairsNorth()) {
         return IsoDirections.N;
      } else {
         return this.HasStairsWest() ? IsoDirections.W : null;
      }
   }

   public float getStairsHeightMax() {
      if (!this.Has(IsoObjectType.stairsTN) && !this.Has(IsoObjectType.stairsTW)) {
         if (!this.Has(IsoObjectType.stairsMN) && !this.Has(IsoObjectType.stairsMW)) {
            return !this.Has(IsoObjectType.stairsMN) && !this.Has(IsoObjectType.stairsMW) ? 0.0F : 0.33F;
         } else {
            return 0.66F;
         }
      } else {
         return 1.0F;
      }
   }

   public float getStairsHeightMin() {
      if (!this.Has(IsoObjectType.stairsTN) && !this.Has(IsoObjectType.stairsTW)) {
         return !this.Has(IsoObjectType.stairsMN) && !this.Has(IsoObjectType.stairsMW) ? 0.0F : 0.33F;
      } else {
         return 0.66F;
      }
   }

   public float getStairsHeight(IsoDirections var1) {
      IsoDirections var2 = this.getStairsDirection();
      if (var2 == null) {
         return 0.0F;
      } else if (var2 == var1) {
         return this.getStairsHeightMax();
      } else {
         return var2.Rot180() == var1 ? this.getStairsHeightMin() : -1.0F;
      }
   }

   public boolean isStairsEdgeBlocked(IsoDirections var1) {
      IsoDirections var2 = this.getStairsDirection();
      if (var2 == null) {
         return false;
      } else {
         IsoGridSquare var3 = this.getAdjacentSquare(var1);
         if (var3 == null) {
            return true;
         } else {
            return this.getStairsHeight(var1) != var3.getStairsHeight(var1.Rot180());
         }
      }
   }

   public boolean hasSlopedSurface() {
      return this.getSlopedSurfaceDirection() != null;
   }

   public IsoDirections getSlopedSurfaceDirection() {
      return this.getProperties().getSlopedSurfaceDirection();
   }

   public boolean hasIdenticalSlopedSurface(IsoGridSquare var1) {
      return this.getSlopedSurfaceDirection() == var1.getSlopedSurfaceDirection() && this.getSlopedSurfaceHeightMin() == var1.getSlopedSurfaceHeightMin() && this.getSlopedSurfaceHeightMax() == var1.getSlopedSurfaceHeightMax();
   }

   public float getSlopedSurfaceHeightMin() {
      return (float)this.getProperties().getSlopedSurfaceHeightMin() / 100.0F;
   }

   public float getSlopedSurfaceHeightMax() {
      return (float)this.getProperties().getSlopedSurfaceHeightMax() / 100.0F;
   }

   public float getSlopedSurfaceHeight(float var1, float var2) {
      IsoDirections var3 = this.getSlopedSurfaceDirection();
      if (var3 == null) {
         return 0.0F;
      } else {
         var1 = PZMath.clamp(var1, 0.0F, 1.0F);
         var2 = PZMath.clamp(var2, 0.0F, 1.0F);
         int var4 = this.getProperties().getSlopedSurfaceHeightMin();
         int var5 = this.getProperties().getSlopedSurfaceHeightMax();
         float var10000;
         switch (var3) {
            case N:
               var10000 = PZMath.lerp((float)var4, (float)var5, 1.0F - var2);
               break;
            case NE:
            case SW:
            default:
               var10000 = -1.0F;
               break;
            case W:
               var10000 = PZMath.lerp((float)var4, (float)var5, 1.0F - var1);
               break;
            case E:
               var10000 = PZMath.lerp((float)var4, (float)var5, var1);
               break;
            case S:
               var10000 = PZMath.lerp((float)var4, (float)var5, var2);
         }

         float var6 = var10000;
         return var6 < 0.0F ? 0.0F : var6 / 100.0F;
      }
   }

   public float getSlopedSurfaceHeight(IsoDirections var1) {
      IsoDirections var2 = this.getSlopedSurfaceDirection();
      if (var2 == null) {
         return 0.0F;
      } else if (var2 == var1) {
         return this.getSlopedSurfaceHeightMax();
      } else {
         return var2.Rot180() == var1 ? this.getSlopedSurfaceHeightMin() : -1.0F;
      }
   }

   public boolean isSlopedSurfaceEdgeBlocked(IsoDirections var1) {
      IsoDirections var2 = this.getSlopedSurfaceDirection();
      if (var2 == null) {
         return false;
      } else {
         IsoGridSquare var3 = this.getAdjacentSquare(var1);
         if (var3 == null) {
            return true;
         } else {
            return this.getSlopedSurfaceHeight(var1) != var3.getSlopedSurfaceHeight(var1.Rot180());
         }
      }
   }

   public boolean hasSlopedSurfaceToLevelAbove(IsoDirections var1) {
      IsoDirections var2 = this.getSlopedSurfaceDirection();
      if (var2 == null) {
         return false;
      } else {
         return this.getSlopedSurfaceHeight(var1) == 1.0F;
      }
   }

   public float getTotalWeightOfItemsOnFloor() {
      float var1 = 0.0F;

      for(int var2 = 0; var2 < this.WorldObjects.size(); ++var2) {
         InventoryItem var3 = ((IsoWorldInventoryObject)this.WorldObjects.get(var2)).getItem();
         if (var3 != null) {
            var1 += var3.getUnequippedWeight();
         }
      }

      return var1;
   }

   public boolean getCollideMatrix(int var1, int var2, int var3) {
      return getMatrixBit(this.collideMatrix, var1 + 1, var2 + 1, var3 + 1);
   }

   public boolean getPathMatrix(int var1, int var2, int var3) {
      return getMatrixBit(this.pathMatrix, var1 + 1, var2 + 1, var3 + 1);
   }

   public boolean getVisionMatrix(int var1, int var2, int var3) {
      return getMatrixBit(this.visionMatrix, var1 + 1, var2 + 1, var3 + 1);
   }

   public void checkRoomSeen(int var1) {
      IsoRoom var2 = this.getRoom();
      if (var2 != null && var2.def != null && !var2.def.bExplored) {
         IsoPlayer var3 = IsoPlayer.players[var1];
         if (var3 != null) {
            if (this.z == PZMath.fastfloor(var3.getZ())) {
               byte var4 = 10;
               if (var3.getBuilding() == var2.building) {
                  var4 = 50;
               }

               if (IsoUtils.DistanceToSquared(var3.getX(), var3.getY(), (float)this.x + 0.5F, (float)this.y + 0.5F) < (float)(var4 * var4)) {
                  var2.def.bExplored = true;
                  var2.onSee();
                  var2.seen = 0;
                  if (var3.isLocalPlayer()) {
                     var3.triggerMusicIntensityEvent("SeeUnexploredRoom");
                  }
               }

            }
         }
      }
   }

   public boolean hasFlies() {
      return this.bHasFlies;
   }

   public void setHasFlies(boolean var1) {
      if (var1 != this.bHasFlies) {
         this.invalidateRenderChunkLevel(var1 ? FBORenderChunk.DIRTY_OBJECT_ADD : FBORenderChunk.DIRTY_OBJECT_REMOVE);
      }

      this.bHasFlies = var1;
   }

   public float getLightLevel(int var1) {
      return var1 == -1 ? this.getLightLevel2() : (this.lighting[var1].lightInfo().r + this.lighting[var1].lightInfo().g + this.lighting[var1].lightInfo().b) / 3.0F;
   }

   public float getLightLevel2() {
      LightingJNI.JNILighting var1 = new LightingJNI.JNILighting(-1, this);
      float var2 = (var1.lightInfo().r + var1.lightInfo().g + var1.lightInfo().b) / 3.0F;
      return var2;
   }

   public ArrayList<IsoAnimal> getAnimals() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.getMovingObjects().size(); ++var2) {
         IsoMovingObject var3 = (IsoMovingObject)this.getMovingObjects().get(var2);
         if (var3 instanceof IsoAnimal) {
            var1.add((IsoAnimal)var3);
         }
      }

      return var1;
   }

   public boolean checkHaveGrass() {
      if (this.getFloor() != null && this.getFloor().getAttachedAnimSprite() != null) {
         for(int var1 = 0; var1 < this.getFloor().getAttachedAnimSprite().size(); ++var1) {
            IsoSprite var2 = ((IsoSpriteInstance)this.getFloor().getAttachedAnimSprite().get(var1)).parentSprite;
            if ("blends_natural_01_87".equals(var2.getName())) {
               return false;
            }
         }
      }

      return true;
   }

   public boolean checkHaveDung() {
      for(int var1 = 0; var1 < this.getWorldObjects().size(); ++var1) {
         InventoryItem var2 = ((IsoWorldInventoryObject)this.getWorldObjects().get(var1)).getItem();
         if (var2.getScriptItem().isDung) {
            return true;
         }
      }

      return false;
   }

   public ArrayList<InventoryItem> removeAllDung() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.getWorldObjects().size(); ++var2) {
         InventoryItem var3 = ((IsoWorldInventoryObject)this.getWorldObjects().get(var2)).getItem();
         if (var3.getScriptItem().isDung) {
            var1.add(var3);
            this.removeWorldObject((IsoWorldInventoryObject)this.getWorldObjects().get(var2));
            --var2;
         }
      }

      return var1;
   }

   public boolean removeGrass() {
      if (!this.isOutside()) {
         return false;
      } else {
         boolean var1 = false;
         if (this.getFloor() != null && this.getFloor().getSprite().getProperties().Is("grassFloor") && this.checkHaveGrass()) {
            this.getFloor().addAttachedAnimSpriteByName("blends_natural_01_87");
            var1 = true;

            for(int var2 = 0; var2 < this.getObjects().size(); ++var2) {
               IsoObject var3 = (IsoObject)this.getObjects().get(var2);
               if (var3.getSprite().getProperties().Is(IsoFlagType.canBeRemoved)) {
                  if (GameServer.bServer) {
                     this.transmitRemoveItemFromSquare(var3);
                  }

                  this.getObjects().remove(var3);
                  --var2;
               }
            }

            this.RecalcProperties();
            this.RecalcAllWithNeighbours(true);
            Zone var4 = this.getGrassRegrowthZone();
            if (var4 == null) {
               var4 = IsoWorld.instance.getMetaGrid().registerZone("GrassRegrowth", IsoWorld.instance.getMetaGrid().getZoneAt(this.x - 20, this.y - 20, this.z).getType(), this.x - 20, this.y - 20, this.z, 40, 40);
               var4.setLastActionTimestamp(Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue());
               INetworkPacket.sendToRelative(PacketTypes.PacketType.RegisterZone, (float)this.x, (float)this.y, var4);
            } else {
               var4.setLastActionTimestamp(Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue());
               INetworkPacket.sendToRelative(PacketTypes.PacketType.SyncZone, (float)this.x, (float)this.y, var4);
            }
         }

         return var1;
      }
   }

   private Zone getGrassRegrowthZone() {
      ArrayList var1 = IsoWorld.instance.getMetaGrid().getZonesAt(this.x, this.y, this.z);

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         Zone var3 = (Zone)var1.get(var2);
         if ("GrassRegrowth".equals(var3.name)) {
            return var3;
         }
      }

      return null;
   }

   public int getZombieCount() {
      int var1 = 0;

      for(int var2 = 0; var2 < this.MovingObjects.size(); ++var2) {
         IsoMovingObject var3 = (IsoMovingObject)this.MovingObjects.get(var2);
         if (var3 != null && var3 instanceof IsoZombie) {
            ++var1;
         }
      }

      return var1;
   }

   public String getSquareRegion() {
      ArrayList var1 = LuaManager.GlobalObject.getZones(this.x, this.y, 0);

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         if (java.util.Objects.equals(((Zone)var1.get(var2)).type, "Region")) {
            return ((Zone)var1.get(var2)).name;
         }
      }

      return "General";
   }

   public boolean containsVegetation() {
      for(int var1 = 0; var1 < this.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)this.getObjects().get(var1);
         if (var2 != null && var2.getSprite() != null && var2.getSprite().getName() != null && var2.getSprite().getName().contains("vegetation")) {
            return true;
         }
      }

      return false;
   }

   public IsoAnimalTrack getAnimalTrack() {
      for(int var1 = 0; var1 < this.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)this.getObjects().get(var1);
         if (var2 != null && var2 instanceof IsoAnimalTrack) {
            return (IsoAnimalTrack)var2;
         }
      }

      return null;
   }

   public boolean hasTrashReceptacle() {
      for(int var1 = 0; var1 < this.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)this.getObjects().get(var1);
         if (var2 != null && var2.getSprite() != null && var2.getSprite().getProperties() != null && var2.getSprite().getProperties().Is("IsTrashCan")) {
            return true;
         }
      }

      return false;
   }

   public boolean hasTrash() {
      for(int var1 = 0; var1 < this.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)this.getObjects().get(var1);
         if (var2 != null && var2.getSprite() != null && (var2.getSprite().getName() != null && var2.getSprite().getName().contains("trash") || var2.getSprite().getProperties() != null && var2.getSprite().getProperties().Is("IsTrashCan"))) {
            return true;
         }
      }

      return false;
   }

   public IsoObject getTrashReceptacle() {
      for(int var1 = 0; var1 < this.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)this.getObjects().get(var1);
         if (var2 != null && var2.getSprite() != null && var2.getSprite().getProperties() != null && var2.getSprite().getProperties().Is("IsTrashCan") && var2.getContainer() != null) {
            return var2;
         }
      }

      return null;
   }

   public boolean isExtraFreeSquare() {
      return this.isFree(false) && this.getObjects().size() < 2 && !this.HasStairs() && this.hasFloor();
   }

   public IsoGridSquare getRandomAdjacentFreeSameRoom() {
      if (this.getRoom() == null) {
         return null;
      } else {
         ArrayList var1 = new ArrayList();

         for(int var2 = 0; var2 < 7; ++var2) {
            IsoGridSquare var3 = this.getAdjacentSquare(IsoDirections.fromIndex(var2));
            if (var3 != null && var3.isExtraFreeSquare() && var3.getRoom() != null && var3.getRoom() == this.getRoom()) {
               var1.add(var3);
            }
         }

         if (var1.isEmpty()) {
            return null;
         } else {
            IsoGridSquare var4 = (IsoGridSquare)var1.get(Rand.Next(var1.size()));
            if (var4 != null && var4.isExtraFreeSquare() && var4.getRoom() != null && var4.getRoom() == this.getRoom()) {
               return var4;
            } else {
               return var4;
            }
         }
      }
   }

   public String getZombiesType() {
      ArrayList var1 = IsoWorld.instance.MetaGrid.getZonesAt(this.x, this.y, 0);

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         if (java.util.Objects.equals(((Zone)var1.get(var2)).type, "ZombiesType")) {
            return ((Zone)var1.get(var2)).name;
         }
      }

      return null;
   }

   public String getLootZone() {
      ArrayList var1 = IsoWorld.instance.MetaGrid.getZonesAt(this.x, this.y, 0);

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         if (java.util.Objects.equals(((Zone)var1.get(var2)).type, "LootZone")) {
            return ((Zone)var1.get(var2)).name;
         }
      }

      return null;
   }

   public IsoObject addTileObject(String var1) {
      if (this == null) {
         return null;
      } else {
         IsoObject var2 = IsoObject.getNew(this, var1, (String)null, false);
         this.AddTileObject(var2);
         MapObjects.newGridSquare(this);
         MapObjects.loadGridSquare(this);
         return var2;
      }
   }

   public boolean hasSand() {
      if (this.getFloor() != null && this.getFloor().getSprite() != null && this.getFloor().getSprite().getName() != null) {
         String var1 = this.getFloor().getSprite().getName();
         if (!var1.contains("blends_natural_01") && !var1.contains("floors_exterior_natural_01")) {
            return false;
         } else if (!var1.equals("blends_natural_01_0") && !var1.equals("blends_natural_01_5") && !var1.equals("blends_natural_01_6") && !var1.equals("blends_natural_01_7")) {
            return var1.contains("floors_exterior_natural_24");
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean hasDirt() {
      if (this.getFloor() != null && this.getFloor().getSprite() != null && this.getFloor().getSprite().getName() != null) {
         String var1 = this.getFloor().getSprite().getName();
         if (!var1.contains("blends_natural_01") && !var1.contains("floors_exterior_natural_01")) {
            return false;
         } else if (!var1.equals("blends_natural_01_64") && !var1.equals("blends_natural_01_69") && !var1.equals("blends_natural_01_70") && !var1.equals("blends_natural_01_71")) {
            if (!var1.equals("blends_natural_01_80") && !var1.equals("blends_natural_01_85") && !var1.equals("blends_natural_01_86") && !var1.equals("blends_natural_01_87")) {
               return var1.equals("floors_exterior_natural_16") || var1.equals("floors_exterior_natural_17") || var1.equals("floors_exterior_natural_18") || var1.equals("floors_exterior_natural_19");
            } else {
               return true;
            }
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean hasNaturalFloor() {
      if (this.getFloor() != null && this.getFloor().getSprite() != null && this.getFloor().getSprite().getName() != null) {
         String var1 = this.getFloor().getSprite().getName();
         return var1.startsWith("blends_natural_01") || var1.startsWith("floors_exterior_natural");
      } else {
         return false;
      }
   }

   public void dirtStamp() {
      if (!this.hasSand() && !this.hasDirt() && this.getFloor() != null) {
         this.getFloor().setAttachedAnimSprite((ArrayList)null);
         this.getFloor().setOverlaySprite((String)null);
         String var1 = "blends_natural_01_64";
         if (!Rand.NextBool(4)) {
            var1 = "blends_natural_01_" + (69 + Rand.Next(3));
         }

         this.getFloor().setSpriteFromName(var1);
         IsoGridSquare var2 = this.getAdjacentSquare(IsoDirections.N);
         IsoObject var3;
         if (var2 != null && !var2.hasSand() && !var2.hasDirt() && var2.getFloor() != null) {
            RandomizedZoneStoryBase.cleanSquareForStory(var2);
            var1 = "blends_natural_01_75";
            if (!Rand.NextBool(4)) {
               var1 = "blends_natural_01_79";
            }

            var2.getFloor().setOverlaySprite(var1);
            var2.RecalcAllWithNeighbours(true);
            var3 = var2.getFloor();
            if (var3 != null) {
               if (var3.AttachedAnimSprite == null) {
                  var3.AttachedAnimSprite = new ArrayList(4);
               }

               var3.AttachedAnimSprite.add(IsoSpriteInstance.get(var2.getFloor().getOverlaySprite()));
            }
         }

         var2 = this.getAdjacentSquare(IsoDirections.S);
         if (var2 != null && !var2.hasSand() && !var2.hasDirt() && var2.getFloor() != null) {
            RandomizedZoneStoryBase.cleanSquareForStory(var2);
            var1 = "blends_natural_01_72";
            if (!Rand.NextBool(4)) {
               var1 = "blends_natural_01_76";
            }

            var2.getFloor().setOverlaySprite(var1);
            var2.RecalcAllWithNeighbours(true);
            var3 = var2.getFloor();
            if (var3 != null) {
               if (var3.AttachedAnimSprite == null) {
                  var3.AttachedAnimSprite = new ArrayList(4);
               }

               var3.AttachedAnimSprite.add(IsoSpriteInstance.get(var2.getFloor().getOverlaySprite()));
            }
         }

         var2 = this.getAdjacentSquare(IsoDirections.E);
         if (var2 != null && !var2.hasSand() && !var2.hasDirt() && var2.getFloor() != null) {
            RandomizedZoneStoryBase.cleanSquareForStory(var2);
            var1 = "blends_natural_01_73";
            if (!Rand.NextBool(4)) {
               var1 = "blends_natural_01_77";
            }

            var2.getFloor().setOverlaySprite(var1);
            var2.RecalcAllWithNeighbours(true);
            var3 = var2.getFloor();
            if (var3 != null) {
               if (var3.AttachedAnimSprite == null) {
                  var3.AttachedAnimSprite = new ArrayList(4);
               }

               var3.AttachedAnimSprite.add(IsoSpriteInstance.get(var2.getFloor().getOverlaySprite()));
            }
         }

         var2 = this.getAdjacentSquare(IsoDirections.W);
         if (var2 != null && !var2.hasSand() && !var2.hasDirt() && var2.getFloor() != null) {
            RandomizedZoneStoryBase.cleanSquareForStory(var2);
            var1 = "blends_natural_01_74";
            if (!Rand.NextBool(4)) {
               var1 = "blends_natural_01_78";
            }

            var2.getFloor().setOverlaySprite(var1);
            var2.RecalcAllWithNeighbours(true);
            var3 = var2.getFloor();
            if (var3 != null) {
               if (var3.AttachedAnimSprite == null) {
                  var3.AttachedAnimSprite = new ArrayList(4);
               }

               var3.AttachedAnimSprite.add(IsoSpriteInstance.get(var2.getFloor().getOverlaySprite()));
            }
         }

      }
   }

   public IsoGridSquare getRandomAdjacent() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < 7; ++var2) {
         IsoGridSquare var3 = this.getAdjacentSquare(IsoDirections.fromIndex(var2));
         if (var3 != null && var3.isExtraFreeSquare()) {
            var1.add(var3);
         }
      }

      if (var1.isEmpty()) {
         return null;
      } else {
         IsoGridSquare var4 = (IsoGridSquare)var1.get(Rand.Next(var1.size()));
         return var4;
      }
   }

   public boolean isAdjacentTo(IsoGridSquare var1) {
      if (this == var1) {
         return true;
      } else {
         for(int var2 = 0; var2 < 7; ++var2) {
            IsoGridSquare var3 = this.getAdjacentSquare(IsoDirections.fromIndex(var2));
            if (var3 != null && var3 == var1) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean hasFireObject() {
      for(int var1 = 0; var1 < this.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)this.getObjects().get(var1);
         if (var2 != null) {
            if (var2 instanceof IsoFireplace && ((IsoFireplace)var2).isLit()) {
               return true;
            }

            if (var2 instanceof IsoBarbecue && ((IsoBarbecue)var2).isLit()) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean hasAdjacentFireObject() {
      for(int var1 = 0; var1 < 7; ++var1) {
         IsoGridSquare var2 = this.getAdjacentSquare(IsoDirections.fromIndex(var1));
         if (var2 != null && !this.isBlockedTo(var2) && var2.hasFireObject()) {
            return true;
         }
      }

      return false;
   }

   public void addGrindstone() {
      GameEntityScript var1 = ScriptManager.instance.getGameEntityScript("Base.Grindstone");
      if (var1 != null) {
         String var2 = "crafted_01_" + (120 + Rand.Next(4));
         if (this.getProperties().Is(IsoFlagType.WallNW)) {
            var2 = "crafted_01_" + (120 + Rand.Next(2));
         } else if (this.getProperties().Is(IsoFlagType.WallN)) {
            var2 = "crafted_01_120";
         } else if (this.getProperties().Is(IsoFlagType.WallW)) {
            var2 = "crafted_01_121";
         } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().Is(IsoFlagType.WallN)) {
            var2 = "crafted_01_122";
         } else if (this.getAdjacentSquare(IsoDirections.E) != null & this.getAdjacentSquare(IsoDirections.E).getProperties().Is(IsoFlagType.WallW)) {
            var2 = "crafted_01_123";
         }

         this.addWorkstationEntity(var1, var2);
      }
   }

   public void addMetalBandsaw() {
      GameEntityScript var1 = ScriptManager.instance.getGameEntityScript("Base.MetalBandsaw");
      if (var1 != null) {
         String var2 = "industry_02_" + (264 + Rand.Next(4));
         if (this.getProperties().Is(IsoFlagType.WallNW)) {
            var2 = "industry_02_" + (264 + Rand.Next(2));
         } else if (this.getProperties().Is(IsoFlagType.WallN)) {
            var2 = "industry_02_265";
         } else if (this.getProperties().Is(IsoFlagType.WallW)) {
            var2 = "industry_02_264";
         } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().Is(IsoFlagType.WallN)) {
            var2 = "industry_02_267";
         } else if (this.getAdjacentSquare(IsoDirections.E) != null & this.getAdjacentSquare(IsoDirections.E).getProperties().Is(IsoFlagType.WallW)) {
            var2 = "industry_02_266";
         }

         this.addWorkstationEntity(var1, var2);
      }
   }

   public void addStandingDrillPress() {
      GameEntityScript var1 = ScriptManager.instance.getGameEntityScript("Base.StandingDrillPress");
      if (var1 != null) {
         String var2 = "industry_02_" + (268 + Rand.Next(4));
         if (this.getProperties().Is(IsoFlagType.WallNW)) {
            var2 = "industry_02_" + (268 + Rand.Next(2));
         } else if (this.getProperties().Is(IsoFlagType.WallN)) {
            var2 = "industry_02_269";
         } else if (this.getProperties().Is(IsoFlagType.WallW)) {
            var2 = "industry_02_268";
         } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().Is(IsoFlagType.WallN)) {
            var2 = "industry_02_270";
         } else if (this.getAdjacentSquare(IsoDirections.E) != null & this.getAdjacentSquare(IsoDirections.E).getProperties().Is(IsoFlagType.WallW)) {
            var2 = "industry_02_271";
         }

         this.addWorkstationEntity(var1, var2);
      }
   }

   public void addFreezer() {
      String var1 = "appliances_refrigeration_01_" + (48 + Rand.Next(4));
      if (this.getProperties().Is(IsoFlagType.WallNW)) {
         var1 = "appliances_refrigeration_01_" + (48 + Rand.Next(2));
      } else if (this.getProperties().Is(IsoFlagType.WallN)) {
         var1 = "appliances_refrigeration_01_48";
      } else if (this.getProperties().Is(IsoFlagType.WallW)) {
         var1 = "appliances_refrigeration_01_49";
      } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().Is(IsoFlagType.WallN)) {
         var1 = "appliances_refrigeration_01_50";
      } else if (this.getAdjacentSquare(IsoDirections.E) != null & this.getAdjacentSquare(IsoDirections.E).getProperties().Is(IsoFlagType.WallW)) {
         var1 = "appliances_refrigeration_01_51";
      }

      this.addTileObject(var1);
   }

   public void addFloodLights() {
      String var1 = "lighting_outdoor_01_" + (48 + Rand.Next(4));
      if (this.getProperties().Is(IsoFlagType.WallNW)) {
         var1 = "lighting_outdoor_01_" + (48 + Rand.Next(2));
      } else if (this.getProperties().Is(IsoFlagType.WallN)) {
         var1 = "lighting_outdoor_01_01_48";
      } else if (this.getProperties().Is(IsoFlagType.WallW)) {
         var1 = "lighting_outdoor_01_01_49";
      } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().Is(IsoFlagType.WallN)) {
         var1 = "lighting_outdoor_01_01_51";
      } else if (this.getAdjacentSquare(IsoDirections.E) != null & this.getAdjacentSquare(IsoDirections.E).getProperties().Is(IsoFlagType.WallW)) {
         var1 = "lighting_outdoor_01_01_50";
      }

      this.addTileObject(var1);
   }

   public void addSpinningWheel() {
      GameEntityScript var1 = ScriptManager.instance.getGameEntityScript("Base.Spinning_Wheel");
      if (var1 != null) {
         String var2 = "crafted_04_" + (36 + Rand.Next(4));
         if (this.getProperties().Is(IsoFlagType.WallNW)) {
            var2 = "crafted_04_" + (36 + Rand.Next(2));
         } else if (this.getProperties().Is(IsoFlagType.WallN)) {
            var2 = "crafted_04_37";
         } else if (this.getProperties().Is(IsoFlagType.WallW)) {
            var2 = "crafted_04_36";
         } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().Is(IsoFlagType.WallN)) {
            var2 = "crafted_04_38";
         } else if (this.getAdjacentSquare(IsoDirections.E) != null & this.getAdjacentSquare(IsoDirections.E).getProperties().Is(IsoFlagType.WallW)) {
            var2 = "crafted_04_39";
         }

         this.addWorkstationEntity(var1, var2);
      }
   }

   public void addLoom() {
      GameEntityScript var1 = ScriptManager.instance.getGameEntityScript("Base.Loom");
      if (var1 != null) {
         String var2 = "crafted_04_" + (72 + Rand.Next(4));
         if (this.getProperties().Is(IsoFlagType.WallNW)) {
            var2 = "crafted_04_" + (72 + Rand.Next(2));
         } else if (this.getProperties().Is(IsoFlagType.WallN)) {
            var2 = "crafted_04_72";
         } else if (this.getProperties().Is(IsoFlagType.WallW)) {
            var2 = "crafted_04_73";
         } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().Is(IsoFlagType.WallN)) {
            var2 = "crafted_04_74";
         } else if (this.getAdjacentSquare(IsoDirections.E) != null & this.getAdjacentSquare(IsoDirections.E).getProperties().Is(IsoFlagType.WallW)) {
            var2 = "crafted_04_75";
         }

         this.addWorkstationEntity(var1, var2);
      }
   }

   public void addHandPress() {
      GameEntityScript var1 = ScriptManager.instance.getGameEntityScript("Base.Hand_Press");
      if (var1 != null) {
         String var2 = "crafted_01_" + (72 + Rand.Next(2));
         if (this.getProperties().Is(IsoFlagType.WallN)) {
            var2 = "crafted_01_72";
         } else if (this.getProperties().Is(IsoFlagType.WallW)) {
            var2 = "crafted_01_73";
         }

         this.addWorkstationEntity(var1, var2);
      }
   }

   public IsoThumpable addWorkstationEntity(String var1, String var2) {
      GameEntityScript var3 = ScriptManager.instance.getGameEntityScript(var1);
      return this.addWorkstationEntity(var3, var2);
   }

   public IsoThumpable addWorkstationEntity(GameEntityScript var1, String var2) {
      if (var1 == null) {
         return null;
      } else {
         IsoThumpable var3 = new IsoThumpable(IsoWorld.instance.getCell(), this, var2, false, (KahluaTable)null);
         this.addWorkstationEntity(var3, var1);
         return var3;
      }
   }

   public void addWorkstationEntity(IsoThumpable var1, GameEntityScript var2) {
      var1.setHealth(var1.getMaxHealth());
      var1.setBreakSound("BreakObject");
      GameEntityFactory.CreateIsoObjectEntity(var1, var2, true);
      this.AddSpecialObject(var1);
      var1.transmitCompleteItemToClients();
   }

   public boolean isDoorSquare() {
      if (!this.getProperties().Is(IsoFlagType.DoorWallN) && !this.getProperties().Is(IsoFlagType.DoorWallW) && !this.Has(IsoObjectType.doorN) && !this.Has(IsoObjectType.doorW)) {
         if (this.getAdjacentSquare(IsoDirections.S) == null || !this.getAdjacentSquare(IsoDirections.S).getProperties().Is(IsoFlagType.DoorWallN) && !this.getAdjacentSquare(IsoDirections.S).Has(IsoObjectType.doorN)) {
            return this.getAdjacentSquare(IsoDirections.E) != null && (this.getAdjacentSquare(IsoDirections.E).getProperties().Is(IsoFlagType.DoorWallW) || this.getAdjacentSquare(IsoDirections.E).Has(IsoObjectType.doorW));
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   public boolean isWallSquare() {
      if (!this.getProperties().Is(IsoFlagType.WallN) && !this.getProperties().Is(IsoFlagType.WallW) && !this.getProperties().Is(IsoFlagType.WallNW)) {
         return this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().Is(IsoFlagType.WallN) ? true : this.getAdjacentSquare(IsoDirections.E) != null & this.getAdjacentSquare(IsoDirections.E).getProperties().Is(IsoFlagType.WallW);
      } else {
         return true;
      }
   }

   public boolean isWallSquareNW() {
      return this.getProperties().Is(IsoFlagType.WallN) || this.getProperties().Is(IsoFlagType.WallW) || this.getProperties().Is(IsoFlagType.WallNW);
   }

   public boolean isFreeWallSquare() {
      if ((this.getProperties().Is(IsoFlagType.WallN) || this.getProperties().Is(IsoFlagType.WallW) || this.getProperties().Is(IsoFlagType.WallNW)) && this.getObjects().size() < 3) {
         return true;
      } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().Is(IsoFlagType.WallN) && this.getObjects().size() < 2) {
         return true;
      } else {
         return this.getAdjacentSquare(IsoDirections.E) != null & this.getAdjacentSquare(IsoDirections.E).getProperties().Is(IsoFlagType.WallW) && this.getObjects().size() < 2;
      }
   }

   public boolean isDoorOrWallSquare() {
      return this.isDoorSquare() || this.isWallSquare();
   }

   public void spawnRandomRuralWorkstation() {
      String var1 = null;
      int var2 = Rand.Next(9);
      switch (var2) {
         case 0:
            var1 = "Freezer";
            this.addFreezer();
            break;
         case 1:
            var1 = "FloodLights";
            this.addFloodLights();
            break;
         case 2:
            var1 = "MetalBandsaw";
            this.addMetalBandsaw();
            break;
         case 3:
            var1 = "DrillPress";
            this.addStandingDrillPress();
            break;
         case 4:
            var1 = "Electric Blower Forge Moveable";
            IsoBarbecue var3 = new IsoBarbecue(IsoWorld.instance.getCell(), this, (IsoSprite)IsoSpriteManager.instance.NamedMap.get("crafted_02_52"));
            this.getObjects().add(var3);
            this.addTileObject("crafted_02_52");
            break;
         case 5:
            var1 = "Hand_Press";
            this.addHandPress();
            break;
         case 6:
            var1 = "Grindstone";
            this.addGrindstone();
            break;
         case 7:
            var1 = "SpinningWheel";
            this.addSpinningWheel();
            break;
         case 8:
            var1 = "Loom";
            this.addLoom();
      }

      DebugLog.log("Special resource tile spawns: " + var1 + ", at " + this.x + ", " + this.y);
   }

   public void spawnRandomWorkstation() {
      if (this.isRural()) {
         this.spawnRandomRuralWorkstation();
      } else {
         String var1 = null;
         int var2 = Rand.Next(4);
         switch (var2) {
            case 0:
               var1 = "Freezer";
               this.addFreezer();
               break;
            case 1:
               var1 = "FloodLights";
               this.addFloodLights();
               break;
            case 2:
               var1 = "MetalBandsaw";
               this.addMetalBandsaw();
               break;
            case 3:
               var1 = "DrillPress";
               this.addStandingDrillPress();
         }

         DebugLog.log("Special resource tile spawns: " + var1 + ", at " + this.x + ", " + this.y);
      }
   }

   public boolean isRural() {
      return java.util.Objects.equals(this.getSquareRegion(), "General") || this.getSquareRegion() == null;
   }

   public boolean isFreeWallPair(IsoDirections var1, boolean var2) {
      IsoGridSquare var3 = this.getAdjacentSquare(var1);
      if (this.isAdjacentTo(var3) && this.getRoom() != null && var3 != null && var3.getRoom() != null && var3.getRoom() == this.getRoom() && this.canReachTo(var3) && !this.isDoorSquare() && this.getObjects().size() <= 2 && !var3.isDoorSquare() && var3.getObjects().size() <= 2) {
         boolean var4 = false;
         int var5 = var1.index();
         switch (var5) {
            case 0:
               if (this.getProperties().Is(IsoFlagType.WallN) || this.getProperties().Is(IsoFlagType.WallNW)) {
                  return false;
               }
            case 1:
            case 3:
            case 5:
            default:
               break;
            case 2:
               if (this.getProperties().Is(IsoFlagType.WallW) || this.getProperties().Is(IsoFlagType.WallNW)) {
                  return false;
               }
               break;
            case 4:
               if (var3.getProperties().Is(IsoFlagType.WallN) || var3.getProperties().Is(IsoFlagType.WallNW)) {
                  return false;
               }
               break;
            case 6:
               if (var3.getProperties().Is(IsoFlagType.WallW) || var3.getProperties().Is(IsoFlagType.WallNW)) {
                  return false;
               }
         }

         if (var5 > 4) {
            var5 -= 4;
         }

         IsoGridSquare var6;
         switch (var5) {
            case 0:
               if (this.getProperties().Is(IsoFlagType.WallW) && var3.getProperties().Is(IsoFlagType.WallW)) {
                  return true;
               }

               if (var2) {
                  var6 = this.getAdjacentSquare(IsoDirections.E);
                  if (var6.getProperties().Is(IsoFlagType.WallW) && this.getObjects().size() < 2 && var6.getProperties().Is(IsoFlagType.WallW) && var6.getObjects().size() < 2) {
                     return true;
                  }
               }
               break;
            case 2:
               if (this.getProperties().Is(IsoFlagType.WallN) && var3.getProperties().Is(IsoFlagType.WallN)) {
                  return true;
               }

               if (var2) {
                  var6 = this.getAdjacentSquare(IsoDirections.S);
                  if (var6.getProperties().Is(IsoFlagType.WallN) && this.getObjects().size() < 2 && var6.getProperties().Is(IsoFlagType.WallN) && var6.getObjects().size() < 2) {
                     return true;
                  }
               }
         }

         return var4;
      } else {
         return false;
      }
   }

   public boolean isGoodSquare() {
      if (this.getFloor() == null) {
         return false;
      } else if (this.isWaterSquare()) {
         return false;
      } else if (!this.isDoorSquare() && !this.HasStairs() && this.getObjects().size() <= 2) {
         return this.isWallSquareNW() || this.getObjects().size() <= 1;
      } else {
         return false;
      }
   }

   public boolean isWaterSquare() {
      if (this.getFloor() != null && this.getFloor().getSprite() != null && this.getFloor().getSprite().getName() != null) {
         String var1 = this.getFloor().getSprite().getName();
         return var1.contains("blends_natural_02_0") || var1.contains("blends_natural_02_1") || var1.contains("blends_natural_02_2") || var1.contains("blends_natural_02_3") || var1.contains("blends_natural_02_4") || var1.contains("blends_natural_02_5") || var1.contains("blends_natural_02_6") || var1.contains("blends_natural_02_7");
      } else {
         return false;
      }
   }

   public boolean isGoodOutsideSquare() {
      return this.isOutside() && this.isGoodSquare();
   }

   public void addStump() {
   }

   public static void setBlendFunc() {
      if (PerformanceSettings.FBORenderChunk) {
         IndieGL.glBlendFuncSeparate(1, 771, 773, 1);
      } else {
         IndieGL.glDefaultBlendFunc();
      }

   }

   public void invalidateRenderChunkLevel(long var1) {
      if (this.chunk != null) {
         this.chunk.invalidateRenderChunkLevel(this.z, var1);
      }
   }

   public void invalidateVispolyChunkLevel() {
      if (this.chunk != null) {
         this.chunk.invalidateVispolyChunkLevel(this.z);
      }
   }

   public ArrayList<IsoHutch> getHutchTiles() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.getSpecialObjects().size(); ++var2) {
         if (this.getSpecialObjects().get(var2) instanceof IsoHutch) {
            var1.add((IsoHutch)this.getSpecialObjects().get(var2));
         }
      }

      return var1;
   }

   public String getSquareZombiesType() {
      ArrayList var1 = IsoWorld.instance.MetaGrid.getZonesAt(this.x, this.y, 0);

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         if (java.util.Objects.equals(((Zone)var1.get(var2)).type, "ZombiesType")) {
            return ((Zone)var1.get(var2)).name;
         }
      }

      return null;
   }

   public boolean hasRoomDef() {
      return this.getRoom() != null && this.getRoom().getRoomDef() != null;
   }

   public void spawnRandomGenerator() {
      if (!this.isRural()) {
         this.spawnRandomNewGenerator();
      } else {
         String var1 = "Base.Generator";
         int var2 = Rand.Next(4);
         switch (var2) {
            case 0:
               var1 = "Base.Generator";
               break;
            case 1:
               var1 = "Base.Generator_Blue";
               break;
            case 2:
               var1 = "Base.Generator_Yellow";
               break;
            case 3:
               var1 = "Base.Generator_Old";
         }

         IsoGenerator var3 = new IsoGenerator(InventoryItemFactory.CreateItem(var1), this.getCell(), this);
         if (GameServer.bServer) {
            var3.transmitCompleteItemToClients();
         }

         DebugLog.log("Special resource tile spawns: " + var1 + ", at " + this.x + ", " + this.y);
      }
   }

   public void spawnRandomNewGenerator() {
      String var1 = "Base.Generator";
      int var2 = Rand.Next(3);
      switch (var2) {
         case 0:
            var1 = "Base.Generator";
            break;
         case 1:
            var1 = "Base.Generator_Blue";
            break;
         case 2:
            var1 = "Base.Generator_Yellow";
      }

      IsoGenerator var3 = new IsoGenerator(InventoryItemFactory.CreateItem(var1), this.getCell(), this);
      if (GameServer.bServer) {
         var3.transmitCompleteItemToClients();
      }

      DebugLog.log("Special resource tile spawns: " + var1 + ", at " + this.x + ", " + this.y);
   }

   public boolean hasGrave() {
      for(int var1 = 0; var1 < this.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)this.getObjects().get(var1);
         if (var2 != null && var2.isGrave()) {
            return true;
         }
      }

      return false;
   }

   public boolean hasFarmingPlant() {
      return this.getFarmingPlant() != null;
   }

   public GlobalObject getFarmingPlant() {
      SGlobalObjectSystem var1 = SGlobalObjects.getSystemByName("farming");
      return var1 == null ? null : var1.getObjectAt(this);
   }

   public void destroyFarmingPlant() {
      SGlobalObjectSystem var1 = SGlobalObjects.getSystemByName("farming");
      if (var1 != null) {
         GlobalObject var2 = var1.getObjectAt(this);
         if (var2 != null) {
            var2.destroyThisObject();
         }
      }
   }

   public boolean hasLitCampfire() {
      SGlobalObjectSystem var1 = SGlobalObjects.getSystemByName("campfire");
      if (var1 == null) {
         return false;
      } else {
         GlobalObject var2 = var1.getObjectAt(this);
         if (var2 == null) {
            return false;
         } else {
            Object var3 = var2.getModData().rawget("isLit");
            if (var3 != null && var3 instanceof Boolean) {
               return true;
            } else {
               return this.getCampfire() != null;
            }
         }
      }
   }

   public GlobalObject getCampfire() {
      SGlobalObjectSystem var1 = SGlobalObjects.getSystemByName("campfire");
      return var1 == null ? null : var1.getObjectAt(this);
   }

   public void putOutCampfire() {
      SGlobalObjectSystem var1 = SGlobalObjects.getSystemByName("campfire");
      if (var1 != null) {
         GlobalObject var2 = var1.getObjectAt(this);
         if (var2 != null) {
            var2.destroyThisObject();
         }
      }
   }

   private IsoGridSquareCollisionData DoDiagnalCheck(IsoGridSquareCollisionData var1, int var2, int var3, int var4, boolean var5) {
      LosUtil.TestResults var6 = this.testVisionAdjacent(var2, 0, var4, false, var5);
      if (var6 == LosUtil.TestResults.Blocked) {
         var1.testResults = LosUtil.TestResults.Blocked;
         return var1;
      } else {
         LosUtil.TestResults var7 = this.testVisionAdjacent(0, var3, var4, false, var5);
         if (var7 == LosUtil.TestResults.Blocked) {
            var1.testResults = LosUtil.TestResults.Blocked;
            return var1;
         } else if (var6 != LosUtil.TestResults.ClearThroughWindow && var7 != LosUtil.TestResults.ClearThroughWindow) {
            return this.getFirstBlocking(var1, var2, var3, var4, false, var5);
         } else {
            var1.testResults = LosUtil.TestResults.ClearThroughWindow;
            return var1;
         }
      }
   }

   public IsoGridSquareCollisionData getFirstBlocking(IsoGridSquareCollisionData var1, int var2, int var3, int var4, boolean var5, boolean var6) {
      if (var2 >= -1 && var2 <= 1 && var3 >= -1 && var3 <= 1 && var4 >= -1 && var4 <= 1) {
         IsoGridSquare var7;
         if (var4 == 1 && (var2 != 0 || var3 != 0) && this.HasElevatedFloor()) {
            var7 = this.getCell().getGridSquare(this.x, this.y, this.z + var4);
            if (var7 != null) {
               return var7.getFirstBlocking(var1, var2, var3, 0, var5, var6);
            }
         }

         if (var4 == -1 && (var2 != 0 || var3 != 0)) {
            var7 = this.getCell().getGridSquare(this.x + var2, this.y + var3, this.z + var4);
            if (var7 != null && var7.HasElevatedFloor()) {
               return this.getFirstBlocking(var1, var2, var3, 0, var5, var6);
            }
         }

         LosUtil.TestResults var13 = LosUtil.TestResults.Clear;
         IsoGridSquare var8;
         if (var2 != 0 && var3 != 0 && var5) {
            var13 = this.DoDiagnalCheck(var2, var3, var4, var6);
            if (var13 == LosUtil.TestResults.Clear || var13 == LosUtil.TestResults.ClearThroughWindow || var13 == LosUtil.TestResults.ClearThroughOpenDoor || var13 == LosUtil.TestResults.ClearThroughClosedDoor) {
               var8 = this.getCell().getGridSquare(this.x + var2, this.y + var3, this.z + var4);
               if (var8 != null) {
                  var1 = var8.DoDiagnalCheck(var1, -var2, -var3, -var4, var6);
               }
            }

            var1.testResults = var13;
            return var1;
         } else {
            var8 = this.getCell().getGridSquare(this.x + var2, this.y + var3, this.z + var4);
            LosUtil.TestResults var9 = LosUtil.TestResults.Clear;
            if (var8 != null && var8.z == this.z) {
               int var10;
               IsoObject var11;
               IsoObject.VisionResult var12;
               if (!this.SpecialObjects.isEmpty()) {
                  for(var10 = 0; var10 < this.SpecialObjects.size(); ++var10) {
                     var11 = (IsoObject)this.SpecialObjects.get(var10);
                     if (var11 == null) {
                        var1.testResults = LosUtil.TestResults.Clear;
                        return var1;
                     }

                     var12 = var11.TestVision(this, var8);
                     if (var12 != IsoObject.VisionResult.NoEffect) {
                        if (var12 == IsoObject.VisionResult.Unblocked && var11 instanceof IsoDoor) {
                           var9 = ((IsoDoor)var11).IsOpen() ? LosUtil.TestResults.ClearThroughOpenDoor : LosUtil.TestResults.ClearThroughClosedDoor;
                        } else if (var12 == IsoObject.VisionResult.Unblocked && var11 instanceof IsoThumpable && ((IsoThumpable)var11).isDoor) {
                           var9 = LosUtil.TestResults.ClearThroughOpenDoor;
                        } else if (var12 == IsoObject.VisionResult.Unblocked && var11 instanceof IsoWindow) {
                           var9 = LosUtil.TestResults.ClearThroughWindow;
                        } else {
                           if (var12 == IsoObject.VisionResult.Blocked && var11 instanceof IsoDoor && !var6) {
                              var1.testResults = LosUtil.TestResults.Blocked;
                              return var1;
                           }

                           if (var12 == IsoObject.VisionResult.Blocked && var11 instanceof IsoThumpable && ((IsoThumpable)var11).isDoor && !var6) {
                              var1.testResults = LosUtil.TestResults.Blocked;
                              return var1;
                           }

                           if (var12 == IsoObject.VisionResult.Blocked && var11 instanceof IsoThumpable && ((IsoThumpable)var11).isWindow()) {
                              var1.testResults = LosUtil.TestResults.Blocked;
                              return var1;
                           }

                           if (var12 == IsoObject.VisionResult.Blocked && var11 instanceof IsoCurtain) {
                              var1.testResults = LosUtil.TestResults.Blocked;
                              return var1;
                           }

                           if (var12 == IsoObject.VisionResult.Blocked && var11 instanceof IsoWindow) {
                              var1.testResults = LosUtil.TestResults.Blocked;
                              return var1;
                           }

                           if (var12 == IsoObject.VisionResult.Blocked && var11 instanceof IsoBarricade) {
                              var1.testResults = LosUtil.TestResults.Blocked;
                              return var1;
                           }
                        }
                     }
                  }
               }

               if (!var8.SpecialObjects.isEmpty()) {
                  for(var10 = 0; var10 < var8.SpecialObjects.size(); ++var10) {
                     var11 = (IsoObject)var8.SpecialObjects.get(var10);
                     if (var11 == null) {
                        var1.testResults = LosUtil.TestResults.Clear;
                        return var1;
                     }

                     var12 = var11.TestVision(this, var8);
                     if (var12 != IsoObject.VisionResult.NoEffect) {
                        if (var12 == IsoObject.VisionResult.Unblocked && var11 instanceof IsoDoor) {
                           var9 = ((IsoDoor)var11).IsOpen() ? LosUtil.TestResults.ClearThroughOpenDoor : LosUtil.TestResults.ClearThroughClosedDoor;
                        } else if (var12 == IsoObject.VisionResult.Unblocked && var11 instanceof IsoThumpable && ((IsoThumpable)var11).isDoor) {
                           var9 = LosUtil.TestResults.ClearThroughOpenDoor;
                        } else if (var12 == IsoObject.VisionResult.Unblocked && var11 instanceof IsoWindow) {
                           var9 = LosUtil.TestResults.ClearThroughWindow;
                        } else {
                           if (var12 == IsoObject.VisionResult.Blocked && var11 instanceof IsoDoor && !var6) {
                              var1.testResults = LosUtil.TestResults.Blocked;
                              return var1;
                           }

                           if (var12 == IsoObject.VisionResult.Blocked && var11 instanceof IsoThumpable && ((IsoThumpable)var11).isDoor && !var6) {
                              var1.testResults = LosUtil.TestResults.Blocked;
                              return var1;
                           }

                           if (var12 == IsoObject.VisionResult.Blocked && var11 instanceof IsoThumpable && ((IsoThumpable)var11).isWindow()) {
                              var1.testResults = LosUtil.TestResults.Blocked;
                              return var1;
                           }

                           if (var12 == IsoObject.VisionResult.Blocked && var11 instanceof IsoCurtain) {
                              var1.testResults = LosUtil.TestResults.Blocked;
                              return var1;
                           }

                           if (var12 == IsoObject.VisionResult.Blocked && var11 instanceof IsoWindow) {
                              var1.testResults = LosUtil.TestResults.Blocked;
                              return var1;
                           }

                           if (var12 == IsoObject.VisionResult.Blocked && var11 instanceof IsoBarricade) {
                              var1.testResults = LosUtil.TestResults.Blocked;
                              return var1;
                           }
                        }
                     }
                  }
               }
            } else if (var4 > 0 && var8 != null && (this.z != -1 || var8.z != 0) && var8.getProperties().Is(IsoFlagType.exterior) && !this.getProperties().Is(IsoFlagType.exterior)) {
               var9 = LosUtil.TestResults.Blocked;
            }

            var13 = !getMatrixBit(this.visionMatrix, var2 + 1, var3 + 1, var4 + 1) ? var9 : LosUtil.TestResults.Blocked;
            var1.testResults = var13;
            return var1;
         }
      } else {
         var1.testResults = LosUtil.TestResults.Blocked;
         return var1;
      }
   }

   private static boolean hasCutawayCapableWallNorth(IsoGridSquare var0) {
      if (var0 == null) {
         return false;
      } else if (var0.Is(IsoFlagType.WallSE)) {
         return false;
      } else {
         boolean var1 = (var0.getWall(true) != null || var0.Is(IsoFlagType.WindowN)) && (var0.Is(IsoFlagType.WallN) || var0.Is(IsoFlagType.WallNW) || var0.Is(IsoFlagType.DoorWallN) || var0.Is(IsoFlagType.WindowN));
         if (!var1) {
            var1 = var0.getGarageDoor(true) != null;
         }

         return var1;
      }
   }

   private static boolean hasCutawayCapableWallWest(IsoGridSquare var0) {
      if (var0 == null) {
         return false;
      } else if (var0.Is(IsoFlagType.WallSE)) {
         return false;
      } else {
         boolean var1 = (var0.getWall(false) != null || var0.Is(IsoFlagType.WindowW)) && (var0.Is(IsoFlagType.WallW) || var0.Is(IsoFlagType.WallNW) || var0.Is(IsoFlagType.DoorWallW) || var0.Is(IsoFlagType.WindowW));
         if (!var1) {
            var1 = var0.getGarageDoor(false) != null;
         }

         return var1;
      }
   }

   public boolean canSpawnVermin() {
      if (SandboxOptions.instance.getCurrentRatIndex() <= 0) {
         return false;
      } else if (!this.isVehicleIntersecting() && !this.isWaterSquare() && this.isSolidFloor()) {
         if (this.isOutside() && this.z != 0) {
            return false;
         } else if (this.z < 0) {
            return true;
         } else if (this.zone == null || !"TownZone".equals(this.zone.getType()) && !"TownZones".equals(this.zone.getType()) && !"TrailerPark".equals(this.zone.getType()) && !"Farm".equals(this.zone.getType())) {
            return this.getSquareRegion() != null || java.util.Objects.equals(this.getSquareZombiesType(), "StreetPoor") || java.util.Objects.equals(this.getSquareZombiesType(), "TrailerPark") || java.util.Objects.equals(this.getLootZone(), "Poor");
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean isNoGas() {
      ArrayList var1 = LuaManager.GlobalObject.getZones(this.x, this.y, 0);

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         if (java.util.Objects.equals(((Zone)var1.get(var2)).type, "NoGas")) {
            return true;
         }
      }

      return false;
   }

   public IsoButcherHook getButcherHook() {
      for(int var1 = 0; var1 < this.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)this.getObjects().get(var1);
         if (var2 instanceof IsoButcherHook) {
            return (IsoButcherHook)var2;
         }
      }

      return null;
   }

   public interface ILighting {
      int lightverts(int var1);

      float lampostTotalR();

      float lampostTotalG();

      float lampostTotalB();

      boolean bSeen();

      boolean bCanSee();

      boolean bCouldSee();

      float darkMulti();

      float targetDarkMulti();

      ColorInfo lightInfo();

      void lightverts(int var1, int var2);

      void lampostTotalR(float var1);

      void lampostTotalG(float var1);

      void lampostTotalB(float var1);

      void bSeen(boolean var1);

      void bCanSee(boolean var1);

      void bCouldSee(boolean var1);

      void darkMulti(float var1);

      void targetDarkMulti(float var1);

      int resultLightCount();

      ResultLight getResultLight(int var1);

      void reset();
   }

   public static final class CircleStencilShader extends Shader {
      public static final CircleStencilShader instance = new CircleStencilShader();
      public int a_wallShadeColor = -1;

      public CircleStencilShader() {
         super("CircleStencil");
      }

      public void startRenderThread(TextureDraw var1) {
         super.startRenderThread(var1);
         VertexBufferObject.setModelViewProjection(this.getProgram());
      }

      protected void onCompileSuccess(ShaderProgram var1) {
         this.Start();
         this.a_wallShadeColor = GL20.glGetAttribLocation(this.getID(), "a_wallShadeColor");
         var1.setSamplerUnit("texture", 0);
         var1.setSamplerUnit("CutawayStencil", 1);
         var1.setSamplerUnit("DEPTH", 2);
         this.End();
      }
   }

   public static final class CutawayNoDepthShader extends Shader {
      private static CutawayNoDepthShader instance = null;
      public int a_wallShadeColor = -1;

      public static CutawayNoDepthShader getInstance() {
         if (instance == null) {
            instance = new CutawayNoDepthShader();
         }

         return instance;
      }

      private CutawayNoDepthShader() {
         super("CutawayNoDepth");
      }

      public void startRenderThread(TextureDraw var1) {
         super.startRenderThread(var1);
         VertexBufferObject.setModelViewProjection(this.getProgram());
      }

      protected void onCompileSuccess(ShaderProgram var1) {
         this.Start();
         this.a_wallShadeColor = GL20.glGetAttribLocation(this.getID(), "a_wallShadeColor");
         var1.setSamplerUnit("texture", 0);
         var1.setSamplerUnit("CutawayStencil", 1);
         this.End();
      }
   }

   private static final class WaterSplashData {
      public float dx;
      public float dy;
      public float frame = -1.0F;
      public float size;
      public boolean isBigSplash = false;
      private int frameCount;
      private int frameCacheShift;
      private float unPausedAccumulator;

      private WaterSplashData() {
      }

      public Texture getTexture() {
         if (!IsoGridSquare.isWaterSplashCacheInitialised) {
            IsoGridSquare.initWaterSplashCache();
         }

         return IsoGridSquare.waterSplashCache[(int)(this.frame * (float)(this.frameCount - 1)) + this.frameCacheShift];
      }

      public void init(int var1, int var2, boolean var3, float var4, float var5) {
         this.frame = 0.0F;
         this.frameCount = var1;
         this.frameCacheShift = var2;
         this.unPausedAccumulator = IsoCamera.frameState.unPausedAccumulator;
         this.dx = var4;
         this.dy = var5;
         this.size = 0.5F;
         if (var3) {
            this.size = Rand.Next(0.25F, 0.75F);
         }

      }

      public void initSmallSplash(float var1, float var2) {
         this.init(16, 0, true, var1, var2);
         this.isBigSplash = false;
      }

      public void initBigSplash(float var1, float var2) {
         int var3 = Rand.Next(2);
         this.init(32, 16 + 32 * var3, false, var1, var2);
         this.isBigSplash = true;
      }

      public void update() {
         if (IsoCamera.frameState.unPausedAccumulator < this.unPausedAccumulator) {
            this.unPausedAccumulator = 0.0F;
         }

         if (!IsoCamera.frameState.Paused && IsoCamera.frameState.unPausedAccumulator > this.unPausedAccumulator) {
            this.frame += 0.0166F * (IsoCamera.frameState.unPausedAccumulator - this.unPausedAccumulator);
            if (this.frame > 1.0F) {
               this.frame = -1.0F;
               this.unPausedAccumulator = 0.0F;
            } else {
               this.unPausedAccumulator = IsoCamera.frameState.unPausedAccumulator;
            }
         }

      }

      public boolean isSplashNow() {
         if (!IsoCamera.frameState.Paused && this.frame >= 0.0F && this.frame <= 1.0F) {
            if (IsoCamera.frameState.unPausedAccumulator < this.unPausedAccumulator) {
               this.unPausedAccumulator = 0.0F;
            }

            if (this.frame + 0.0166F * (IsoCamera.frameState.unPausedAccumulator - this.unPausedAccumulator) > 1.5F) {
               this.frame = -1.0F;
            }
         }

         return this.frame >= 0.0F && this.frame <= 1.0F;
      }
   }

   public static final class Lighting implements ILighting {
      private final int[] lightverts = new int[8];
      private float lampostTotalR = 0.0F;
      private float lampostTotalG = 0.0F;
      private float lampostTotalB = 0.0F;
      private boolean bSeen;
      private boolean bCanSee;
      private boolean bCouldSee;
      private float darkMulti;
      private float targetDarkMulti;
      private final ColorInfo lightInfo = new ColorInfo();

      public Lighting() {
      }

      public int lightverts(int var1) {
         return this.lightverts[var1];
      }

      public float lampostTotalR() {
         return this.lampostTotalR;
      }

      public float lampostTotalG() {
         return this.lampostTotalG;
      }

      public float lampostTotalB() {
         return this.lampostTotalB;
      }

      public boolean bSeen() {
         return this.bSeen;
      }

      public boolean bCanSee() {
         return this.bCanSee;
      }

      public boolean bCouldSee() {
         return this.bCouldSee;
      }

      public float darkMulti() {
         return this.darkMulti;
      }

      public float targetDarkMulti() {
         return this.targetDarkMulti;
      }

      public ColorInfo lightInfo() {
         return this.lightInfo;
      }

      public void lightverts(int var1, int var2) {
         this.lightverts[var1] = var2;
      }

      public void lampostTotalR(float var1) {
         this.lampostTotalR = var1;
      }

      public void lampostTotalG(float var1) {
         this.lampostTotalG = var1;
      }

      public void lampostTotalB(float var1) {
         this.lampostTotalB = var1;
      }

      public void bSeen(boolean var1) {
         this.bSeen = var1;
      }

      public void bCanSee(boolean var1) {
         this.bCanSee = var1;
      }

      public void bCouldSee(boolean var1) {
         this.bCouldSee = var1;
      }

      public void darkMulti(float var1) {
         this.darkMulti = var1;
      }

      public void targetDarkMulti(float var1) {
         this.targetDarkMulti = var1;
      }

      public int resultLightCount() {
         return 0;
      }

      public ResultLight getResultLight(int var1) {
         return null;
      }

      public void reset() {
         this.lampostTotalR = 0.0F;
         this.lampostTotalG = 0.0F;
         this.lampostTotalB = 0.0F;
         this.bSeen = false;
         this.bCouldSee = false;
         this.bCanSee = false;
         this.targetDarkMulti = 0.0F;
         this.darkMulti = 0.0F;
         this.lightInfo.r = 0.0F;
         this.lightInfo.g = 0.0F;
         this.lightInfo.b = 0.0F;
         this.lightInfo.a = 1.0F;
      }
   }

   public static class CellGetSquare implements GetSquare {
      public CellGetSquare() {
      }

      public IsoGridSquare getGridSquare(int var1, int var2, int var3) {
         return IsoWorld.instance.CurrentCell.getGridSquare(var1, var2, var3);
      }
   }

   public interface GetSquare {
      IsoGridSquare getGridSquare(int var1, int var2, int var3);
   }

   private static final class s_performance {
      static final PerformanceProfileProbe renderFloor = new PerformanceProfileProbe("IsoGridSquare.renderFloor", false);

      private s_performance() {
      }
   }

   public static class PuddlesDirection {
      public static byte PUDDLES_DIR_NONE = 1;
      public static byte PUDDLES_DIR_NE = 2;
      public static byte PUDDLES_DIR_NW = 4;
      public static byte PUDDLES_DIR_ALL = 8;

      public PuddlesDirection() {
      }
   }

   public static final class NoCircleStencilShader {
      public static final NoCircleStencilShader instance = new NoCircleStencilShader();
      private ShaderProgram shaderProgram;
      public int ShaderID = -1;
      public int a_wallShadeColor = -1;

      public NoCircleStencilShader() {
      }

      private void initShader() {
         this.shaderProgram = ShaderProgram.createShaderProgram("NoCircleStencil", false, false, true);
         if (this.shaderProgram.isCompiled()) {
            this.ShaderID = this.shaderProgram.getShaderID();
            this.a_wallShadeColor = GL20.glGetAttribLocation(this.ShaderID, "a_wallShadeColor");
         }

      }
   }

   private interface RenderWallCallback {
      void invoke(Texture var1, float var2, float var3);
   }

   public static final class ResultLight {
      public int id;
      public int x;
      public int y;
      public int z;
      public int radius;
      public float r;
      public float g;
      public float b;
      public static final int RLF_NONE = 0;
      public static final int RLF_ROOMLIGHT = 1;
      public static final int RLF_TORCH = 2;
      public int flags;

      public ResultLight() {
      }

      public ResultLight copyFrom(ResultLight var1) {
         this.id = var1.id;
         this.x = var1.x;
         this.y = var1.y;
         this.z = var1.z;
         this.radius = var1.radius;
         this.r = var1.r;
         this.g = var1.g;
         this.b = var1.b;
         this.flags = var1.flags;
         return this;
      }
   }
}
