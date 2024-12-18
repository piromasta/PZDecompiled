package zombie.iso;

import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joml.Vector2i;
import se.krka.kahlua.integration.annotations.LuaMethod;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.LuaClosure;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.MovingObjectUpdateScheduler;
import zombie.ReanimatedPlayers;
import zombie.SandboxOptions;
import zombie.VirtualZombieManager;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaHookManager;
import zombie.Lua.LuaManager;
import zombie.Lua.MapObjects;
import zombie.ai.astar.Mover;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.physics.RagdollControllerDebugRenderer;
import zombie.core.physics.WorldSimulation;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbeList;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.utils.IntGrid;
import zombie.core.utils.OnceEvery;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.erosion.utils.Noise2D;
import zombie.gameStates.GameLoadingState;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.inventory.InventoryItem;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.BuildingScore;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.IsoRoomExit;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderCutaways;
import zombie.iso.fboRenderChunk.FBORenderSnow;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.sprite.CorpseFlies;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.sprite.shapers.FloorShaper;
import zombie.iso.sprite.shapers.FloorShaperAttachedSprites;
import zombie.iso.sprite.shapers.FloorShaperDiamond;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.fog.ImprovedFog;
import zombie.iso.weather.fx.IsoWeatherFX;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.popman.NetworkZombieSimulator;
import zombie.popman.animal.AnimalSynchronizationManager;
import zombie.savefile.ClientPlayerDB;
import zombie.savefile.PlayerDB;
import zombie.scripting.objects.VehicleScript;
import zombie.ui.UIManager;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public final class IsoCell {
   public static int CellSizeInChunks = 32;
   public static int CellSizeInSquares;
   public static int MaxHeight;
   public static Shader m_floorRenderShader;
   public static Shader m_wallRenderShader;
   public ArrayList<IsoGridSquare> Trees = new ArrayList();
   public int minHeight;
   static final ArrayList<IsoGridSquare> stchoices;
   public final IsoChunkMap[] ChunkMap = new IsoChunkMap[4];
   public final ArrayList<IsoBuilding> BuildingList = new ArrayList();
   private final ArrayList<IsoWindow> WindowList = new ArrayList();
   private final ArrayList<IsoMovingObject> ObjectList = new ArrayList();
   private final ArrayList<IsoPushableObject> PushableObjectList = new ArrayList();
   private final HashMap<Integer, BuildingScore> BuildingScores = new HashMap();
   private final ArrayList<IsoRoom> RoomList = new ArrayList();
   private final ArrayList<IsoObject> StaticUpdaterObjectList = new ArrayList();
   private final ArrayList<IsoZombie> ZombieList = new ArrayList();
   private final ArrayList<IsoGameCharacter> RemoteSurvivorList = new ArrayList();
   private final ArrayList<IsoMovingObject> removeList = new ArrayList();
   private final ArrayList<IsoMovingObject> addList = new ArrayList();
   private final ArrayList<IsoObject> ProcessIsoObject = new ArrayList();
   private final ArrayList<IsoObject> ProcessIsoObjectRemove = new ArrayList();
   private final ArrayList<InventoryItem> ProcessItems = new ArrayList();
   private final ArrayList<InventoryItem> ProcessItemsRemove = new ArrayList();
   private final ArrayList<IsoWorldInventoryObject> ProcessWorldItems = new ArrayList();
   public final ArrayList<IsoWorldInventoryObject> ProcessWorldItemsRemove = new ArrayList();
   private final IsoGridSquare[][] gridSquares;
   public static final boolean ENABLE_SQUARE_CACHE = false;
   private int height;
   private int width;
   private int worldX;
   private int worldY;
   public IntGrid DangerScore;
   private boolean safeToAdd;
   private final Stack<IsoLightSource> LamppostPositions;
   public final ArrayList<IsoRoomLight> roomLights;
   private final ArrayList<IsoHeatSource> heatSources;
   public final ArrayList<BaseVehicle> addVehicles;
   public final ArrayList<BaseVehicle> vehicles;
   public static final int ISOANGLEFACTOR = 3;
   public static final int ZOMBIESCANBUDGET = 10;
   public static final float NEARESTZOMBIEDISTSQRMAX = 150.0F;
   public int zombieScanCursor;
   public final IsoZombie[] nearestVisibleZombie;
   public final float[] nearestVisibleZombieDistSqr;
   private static Stack<BuildingScore> buildingscores;
   public static ArrayList<IsoGridSquare> GridStack;
   public static final int RTF_SolidFloor = 1;
   public static final int RTF_VegetationCorpses = 2;
   public static final int RTF_MinusFloorCharacters = 4;
   public static final int RTF_ShadedFloor = 8;
   public static final int RTF_Shadows = 16;
   public static final ArrayList<IsoGridSquare> ShadowSquares;
   public static final ArrayList<IsoGridSquare> MinusFloorCharacters;
   public static final ArrayList<IsoGridSquare> SolidFloor;
   public static final ArrayList<IsoGridSquare> ShadedFloor;
   public static final ArrayList<IsoGridSquare> VegetationCorpses;
   public static final PerPlayerRender[] perPlayerRender;
   public int StencilX1;
   public int StencilY1;
   public int StencilX2;
   public int StencilY2;
   private Texture m_stencilTexture;
   private final DiamondMatrixIterator diamondMatrixIterator;
   private final Vector2i diamondMatrixPos;
   public int DeferredCharacterTick;
   private boolean hasSetupSnowGrid;
   private SnowGridTiles snowGridTiles_Square;
   private SnowGridTiles[] snowGridTiles_Strip;
   private SnowGridTiles[] snowGridTiles_Edge;
   private SnowGridTiles[] snowGridTiles_Cove;
   private SnowGridTiles snowGridTiles_Enclosed;
   private int m_snowFirstNonSquare;
   private Noise2D snowNoise2D;
   private SnowGrid snowGridCur;
   private SnowGrid snowGridPrev;
   private int snowFracTarget;
   private long snowFadeTime;
   private float snowTransitionTime;
   private int raport;
   private static final int SNOWSHORE_NONE = 0;
   private static final int SNOWSHORE_N = 1;
   private static final int SNOWSHORE_E = 2;
   private static final int SNOWSHORE_S = 4;
   private static final int SNOWSHORE_W = 8;
   public boolean recalcFloors;
   static int wx;
   static int wy;
   final KahluaTable[] drag;
   final ArrayList<IsoSurvivor> SurvivorList;
   private static Texture texWhite;
   private static IsoCell instance;
   private int currentLX;
   private int currentLY;
   private int currentLZ;
   public int recalcShading;
   public int lastMinX;
   public int lastMinY;
   private float rainScroll;
   private int[] rainX;
   private int[] rainY;
   private Texture[] rainTextures;
   private long[] rainFileTime;
   private float rainAlphaMax;
   private float[] rainAlpha;
   protected int rainIntensity;
   protected int rainSpeed;
   public int lightUpdateCount;
   public boolean bRendering;
   public final boolean[] bHideFloors;
   public final int[] unhideFloorsCounter;
   public boolean bOccludedByOrphanStructureFlag;
   public long playerPeekedRoomId;
   public final ArrayList<ArrayList<IsoBuilding>> playerOccluderBuildings;
   public final IsoBuilding[][] playerOccluderBuildingsArr;
   public final long[] playerWindowPeekingRoomId;
   public final boolean[] playerHidesOrphanStructures;
   public final boolean[] playerCutawaysDirty;
   final Vector2 tempCutawaySqrVector;
   ArrayList<ArrayList<Long>> tempPrevPlayerCutawayRoomIDs;
   public ArrayList<ArrayList<Long>> tempPlayerCutawayRoomIDs;
   public final IsoGridSquare[] lastPlayerSquare;
   public final boolean[] lastPlayerSquareHalf;
   public final IsoDirections[] lastPlayerDir;
   public final Vector2[] lastPlayerAngle;
   public int hidesOrphanStructuresAbove;
   final Rectangle buildingRectTemp;
   public final ArrayList<ArrayList<IsoBuilding>> zombieOccluderBuildings;
   public final IsoBuilding[][] zombieOccluderBuildingsArr;
   public final IsoGridSquare[] lastZombieSquare;
   public final boolean[] lastZombieSquareHalf;
   public final ArrayList<ArrayList<IsoBuilding>> otherOccluderBuildings;
   public final IsoBuilding[][] otherOccluderBuildingsArr;
   final int mustSeeSquaresRadius;
   final int mustSeeSquaresGridSize;
   public final ArrayList<IsoGridSquare> gridSquaresTempLeft;
   public final ArrayList<IsoGridSquare> gridSquaresTempRight;
   private IsoWeatherFX weatherFX;
   public int minX;
   public int maxX;
   public int minY;
   public int maxY;
   public int minZ;
   public int maxZ;
   private OnceEvery dangerUpdate;
   private Thread LightInfoUpdate;
   long lastServerItemsUpdate;
   private final Stack<IsoRoom> SpottedRooms;
   private IsoZombie fakeZombieForHit;

   public static int getMaxHeight() {
      return MaxHeight;
   }

   public static int getCellSizeInChunks() {
      return CellSizeInChunks;
   }

   public static int getCellSizeInSquares() {
      return CellSizeInSquares;
   }

   public LotHeader getCurrentLotHeader() {
      IsoGameCharacter var1 = IsoCamera.getCameraCharacter();
      IsoChunk var2 = this.getChunkForGridSquare(PZMath.fastfloor(var1.getX()), PZMath.fastfloor(var1.getY()), PZMath.fastfloor(var1.getZ()));
      return var2.lotheader;
   }

   public IsoChunkMap getChunkMap(int var1) {
      return this.ChunkMap[var1];
   }

   public IsoGridSquare getFreeTile(RoomDef var1) {
      stchoices.clear();

      for(int var2 = 0; var2 < var1.rects.size(); ++var2) {
         RoomDef.RoomRect var3 = (RoomDef.RoomRect)var1.rects.get(var2);

         for(int var4 = var3.x; var4 < var3.x + var3.w; ++var4) {
            for(int var5 = var3.y; var5 < var3.y + var3.h; ++var5) {
               IsoGridSquare var6 = this.getGridSquare(var4, var5, var1.level);
               if (var6 != null) {
                  var6.setCachedIsFree(false);
                  var6.setCacheIsFree(false);
                  if (var6.isFree(false)) {
                     stchoices.add(var6);
                  }
               }
            }
         }
      }

      if (stchoices.isEmpty()) {
         return null;
      } else {
         IsoGridSquare var7 = (IsoGridSquare)stchoices.get(Rand.Next(stchoices.size()));
         stchoices.clear();
         return var7;
      }
   }

   public static Stack<BuildingScore> getBuildings() {
      return buildingscores;
   }

   public static void setBuildings(Stack<BuildingScore> var0) {
      buildingscores = var0;
   }

   public IsoZombie getNearestVisibleZombie(int var1) {
      return this.nearestVisibleZombie[var1];
   }

   public IsoChunk getChunkForGridSquare(int var1, int var2, int var3) {
      int var4 = var1;
      int var5 = var2;

      for(int var6 = 0; var6 < IsoPlayer.numPlayers; ++var6) {
         if (!this.ChunkMap[var6].ignore) {
            var1 = var4 - this.ChunkMap[var6].getWorldXMinTiles();
            var2 = var5 - this.ChunkMap[var6].getWorldYMinTiles();
            if (var1 >= 0 && var2 >= 0) {
               IsoChunkMap var10001 = this.ChunkMap[var6];
               var1 /= 8;
               var10001 = this.ChunkMap[var6];
               var2 /= 8;
               IsoChunk var7 = null;
               var7 = this.ChunkMap[var6].getChunk(var1, var2);
               if (var7 != null) {
                  return var7;
               }
            }
         }
      }

      return null;
   }

   public IsoChunk getChunk(int var1, int var2) {
      for(int var3 = 0; var3 < IsoPlayer.numPlayers; ++var3) {
         IsoChunkMap var4 = this.ChunkMap[var3];
         if (!var4.ignore) {
            IsoChunk var5 = var4.getChunk(var1 - var4.getWorldXMin(), var2 - var4.getWorldYMin());
            if (var5 != null) {
               return var5;
            }
         }
      }

      return null;
   }

   public IsoCell(int var1, int var2) {
      this.gridSquares = new IsoGridSquare[4][IsoChunkMap.ChunkWidthInTiles * IsoChunkMap.ChunkWidthInTiles * 64];
      this.safeToAdd = true;
      this.LamppostPositions = new Stack();
      this.roomLights = new ArrayList();
      this.heatSources = new ArrayList();
      this.addVehicles = new ArrayList();
      this.vehicles = new ArrayList();
      this.zombieScanCursor = 0;
      this.nearestVisibleZombie = new IsoZombie[4];
      this.nearestVisibleZombieDistSqr = new float[4];
      this.m_stencilTexture = null;
      this.diamondMatrixIterator = new DiamondMatrixIterator(123);
      this.diamondMatrixPos = new Vector2i();
      this.DeferredCharacterTick = 0;
      this.hasSetupSnowGrid = false;
      this.m_snowFirstNonSquare = -1;
      this.snowNoise2D = new Noise2D();
      this.snowFracTarget = 0;
      this.snowFadeTime = 0L;
      this.snowTransitionTime = 5000.0F;
      this.raport = 0;
      this.recalcFloors = false;
      this.drag = new KahluaTable[4];
      this.SurvivorList = new ArrayList();
      this.currentLX = 0;
      this.currentLY = 0;
      this.currentLZ = 0;
      this.recalcShading = 30;
      this.lastMinX = -1234567;
      this.lastMinY = -1234567;
      this.rainX = new int[4];
      this.rainY = new int[4];
      this.rainTextures = new Texture[5];
      this.rainFileTime = new long[5];
      this.rainAlphaMax = 0.6F;
      this.rainAlpha = new float[4];
      this.rainIntensity = 0;
      this.rainSpeed = 6;
      this.lightUpdateCount = 11;
      this.bRendering = false;
      this.bHideFloors = new boolean[4];
      this.unhideFloorsCounter = new int[4];
      this.bOccludedByOrphanStructureFlag = false;
      this.playerPeekedRoomId = -1L;
      this.playerOccluderBuildings = new ArrayList(4);
      this.playerOccluderBuildingsArr = new IsoBuilding[4][];
      this.playerWindowPeekingRoomId = new long[4];
      this.playerHidesOrphanStructures = new boolean[4];
      this.playerCutawaysDirty = new boolean[4];
      this.tempCutawaySqrVector = new Vector2();
      this.tempPrevPlayerCutawayRoomIDs = new ArrayList(4);
      this.tempPlayerCutawayRoomIDs = new ArrayList(4);
      this.lastPlayerSquare = new IsoGridSquare[4];
      this.lastPlayerSquareHalf = new boolean[4];
      this.lastPlayerDir = new IsoDirections[4];
      this.lastPlayerAngle = new Vector2[4];
      this.hidesOrphanStructuresAbove = MaxHeight;
      this.buildingRectTemp = new Rectangle();
      this.zombieOccluderBuildings = new ArrayList(4);
      this.zombieOccluderBuildingsArr = new IsoBuilding[4][];
      this.lastZombieSquare = new IsoGridSquare[4];
      this.lastZombieSquareHalf = new boolean[4];
      this.otherOccluderBuildings = new ArrayList(4);
      this.otherOccluderBuildingsArr = new IsoBuilding[4][];
      this.mustSeeSquaresRadius = 4;
      this.mustSeeSquaresGridSize = 10;
      this.gridSquaresTempLeft = new ArrayList(100);
      this.gridSquaresTempRight = new ArrayList(100);
      this.dangerUpdate = new OnceEvery(0.4F, false);
      this.LightInfoUpdate = null;
      this.lastServerItemsUpdate = System.currentTimeMillis();
      this.SpottedRooms = new Stack();
      IsoWorld.instance.CurrentCell = this;
      instance = this;
      this.width = var1;
      this.height = var2;

      for(int var3 = 0; var3 < 4; ++var3) {
         this.ChunkMap[var3] = new IsoChunkMap(this);
         this.ChunkMap[var3].PlayerID = var3;
         this.ChunkMap[var3].ignore = var3 > 0;
         this.tempPlayerCutawayRoomIDs.add(new ArrayList());
         this.tempPrevPlayerCutawayRoomIDs.add(new ArrayList());
         this.playerOccluderBuildings.add(new ArrayList(5));
         this.zombieOccluderBuildings.add(new ArrayList(5));
         this.otherOccluderBuildings.add(new ArrayList(5));
      }

      WorldReuserThread.instance.run();
   }

   public void CalculateVertColoursForTile(IsoGridSquare var1, int var2, int var3, int var4, int var5) {
      IsoGridSquare var6 = !IsoGridSquare.getMatrixBit(var1.visionMatrix, (int)0, (int)0, (int)1) ? var1.nav[IsoDirections.NW.index()] : null;
      IsoGridSquare var7 = !IsoGridSquare.getMatrixBit(var1.visionMatrix, (int)1, (int)0, (int)1) ? var1.nav[IsoDirections.N.index()] : null;
      IsoGridSquare var8 = !IsoGridSquare.getMatrixBit(var1.visionMatrix, (int)2, (int)0, (int)1) ? var1.nav[IsoDirections.NE.index()] : null;
      IsoGridSquare var9 = !IsoGridSquare.getMatrixBit(var1.visionMatrix, (int)2, (int)1, (int)1) ? var1.nav[IsoDirections.E.index()] : null;
      IsoGridSquare var10 = !IsoGridSquare.getMatrixBit(var1.visionMatrix, (int)2, (int)2, (int)1) ? var1.nav[IsoDirections.SE.index()] : null;
      IsoGridSquare var11 = !IsoGridSquare.getMatrixBit(var1.visionMatrix, (int)1, (int)2, (int)1) ? var1.nav[IsoDirections.S.index()] : null;
      IsoGridSquare var12 = !IsoGridSquare.getMatrixBit(var1.visionMatrix, (int)0, (int)2, (int)1) ? var1.nav[IsoDirections.SW.index()] : null;
      IsoGridSquare var13 = !IsoGridSquare.getMatrixBit(var1.visionMatrix, (int)0, (int)1, (int)1) ? var1.nav[IsoDirections.W.index()] : null;
      this.CalculateColor(var6, var7, var13, var1, 0, var5);
      this.CalculateColor(var7, var8, var9, var1, 1, var5);
      this.CalculateColor(var10, var11, var9, var1, 2, var5);
      this.CalculateColor(var12, var11, var13, var1, 3, var5);
   }

   private Texture getStencilTexture() {
      if (this.m_stencilTexture == null) {
         this.m_stencilTexture = Texture.getSharedTexture("media/mask_circledithernew.png");
      }

      return this.m_stencilTexture;
   }

   public void DrawStencilMask() {
      Texture var1 = this.getStencilTexture();
      if (var1 != null) {
         IndieGL.glStencilMask(255);
         IndieGL.glDepthMask(false);
         IndieGL.glClear(1280);
         int var2 = IsoCamera.getOffscreenWidth(IsoPlayer.getPlayerIndex()) / 2;
         int var3 = IsoCamera.getOffscreenHeight(IsoPlayer.getPlayerIndex()) / 2;
         var2 -= var1.getWidth() / (2 / Core.TileScale);
         var3 -= var1.getHeight() / (2 / Core.TileScale);
         IndieGL.enableStencilTest();
         IndieGL.enableAlphaTest();
         IndieGL.glAlphaFunc(516, 0.1F);
         IndieGL.glStencilFunc(519, 128, 255);
         IndieGL.glStencilOp(7680, 7680, 7681);
         IndieGL.glColorMask(false, false, false, false);
         var1.renderstrip(var2 - (int)IsoCamera.getRightClickOffX(), var3 - (int)IsoCamera.getRightClickOffY(), var1.getWidth() * Core.TileScale, var1.getHeight() * Core.TileScale, 1.0F, 1.0F, 1.0F, 1.0F, (Consumer)null);
         IndieGL.glColorMask(true, true, true, true);
         IndieGL.glStencilFunc(519, 0, 255);
         IndieGL.glStencilOp(7680, 7680, 7680);
         IndieGL.glStencilMask(127);
         IndieGL.glAlphaFunc(519, 0.0F);
         this.StencilX1 = var2 - (int)IsoCamera.getRightClickOffX();
         this.StencilY1 = var3 - (int)IsoCamera.getRightClickOffY();
         this.StencilX2 = this.StencilX1 + var1.getWidth() * Core.TileScale;
         this.StencilY2 = this.StencilY1 + var1.getHeight() * Core.TileScale;
      }
   }

   public void RenderTiles(int var1) {
      IsoCell.s_performance.isoCellRenderTiles.invokeAndMeasure(this, var1, IsoCell::renderTilesInternal);
   }

   private void renderTilesInternal(int var1) {
      if (DebugOptions.instance.Terrain.RenderTiles.Enable.getValue()) {
         if (m_floorRenderShader == null) {
            RenderThread.invokeOnRenderContext(this::initTileShaders);
         }

         int var2 = IsoCamera.frameState.playerIndex;
         IsoPlayer var3 = IsoPlayer.players[var2];
         var3.dirtyRecalcGridStackTime -= GameTime.getInstance().getMultiplier() / 4.0F;
         PerPlayerRender var4 = this.getPerPlayerRenderAt(var2);
         var4.setSize(this.maxX - this.minX + 1, this.maxY - this.minY + 1);
         long var5 = System.currentTimeMillis();
         if (this.minX != var4.minX || this.minY != var4.minY || this.maxX != var4.maxX || this.maxY != var4.maxY) {
            var4.minX = this.minX;
            var4.minY = this.minY;
            var4.maxX = this.maxX;
            var4.maxY = this.maxY;
            var3.dirtyRecalcGridStack = true;
            WeatherFxMask.forceMaskUpdate(var2);
         }

         IsoCell.s_performance.renderTiles.recalculateAnyGridStacks.start();
         boolean var7 = var3.dirtyRecalcGridStack;
         this.recalculateAnyGridStacks(var4, var1, var2, var5);
         IsoCell.s_performance.renderTiles.recalculateAnyGridStacks.end();
         ++this.DeferredCharacterTick;
         IsoCell.s_performance.renderTiles.flattenAnyFoliage.start();
         this.flattenAnyFoliage(var4, var2);
         IsoCell.s_performance.renderTiles.flattenAnyFoliage.end();
         if (this.SetCutawayRoomsForPlayer() || var7) {
            IsoGridStack var8 = var4.GridStacks;

            for(int var9 = 0; var9 < var1 + 1; ++var9) {
               GridStack = (ArrayList)var8.Squares.get(var9);

               for(int var10 = 0; var10 < GridStack.size(); ++var10) {
                  IsoGridSquare var11 = (IsoGridSquare)GridStack.get(var10);
                  var11.setPlayerCutawayFlag(var2, this.IsCutawaySquare(var11, var5) ? 3 : 0, var5);
               }
            }
         }

         IsoCell.s_performance.renderTiles.performRenderTiles.start();
         this.performRenderTiles(var4, var1, var2, var5);
         IsoCell.s_performance.renderTiles.performRenderTiles.end();
         this.playerCutawaysDirty[var2] = false;
         ShadowSquares.clear();
         MinusFloorCharacters.clear();
         ShadedFloor.clear();
         SolidFloor.clear();
         VegetationCorpses.clear();
         IsoCell.s_performance.renderTiles.renderDebugPhysics.start();
         this.renderDebugPhysics(var2);
         IsoCell.s_performance.renderTiles.renderDebugPhysics.end();
         IsoCell.s_performance.renderTiles.renderDebugLighting.start();
         this.renderDebugLighting(var4, var1);
         IsoCell.s_performance.renderTiles.renderDebugLighting.end();
      }
   }

   public void initTileShaders() {
      if (DebugLog.isEnabled(DebugType.Shader)) {
         DebugLog.Shader.debugln("Loading shader: \"floorTile\"");
      }

      m_floorRenderShader = new Shader("floorTile");
      if (DebugLog.isEnabled(DebugType.Shader)) {
         DebugLog.Shader.debugln("Loading shader: \"wallTile\"");
      }

      m_wallRenderShader = new Shader("wallTile");
      IsoGridSquare.CircleStencilShader var1 = IsoGridSquare.CircleStencilShader.instance;
      IsoGridSquare.CutawayNoDepthShader.getInstance();
   }

   public PerPlayerRender getPerPlayerRenderAt(int var1) {
      if (perPlayerRender[var1] == null) {
         perPlayerRender[var1] = new PerPlayerRender();
      }

      return perPlayerRender[var1];
   }

   private void recalculateAnyGridStacks(PerPlayerRender var1, int var2, int var3, long var4) {
      IsoPlayer var6 = IsoPlayer.players[var3];
      if (var6.dirtyRecalcGridStack) {
         var6.dirtyRecalcGridStack = false;
         IsoGridStack var7 = var1.GridStacks;
         boolean[][][] var8 = var1.VisiOccludedFlags;
         boolean[][] var9 = var1.VisiCulledFlags;
         IsoChunk var10 = -1;
         int var11 = -1;
         int var12 = -1;
         WeatherFxMask.setDiamondIterDone(var3);
         int var13 = this.ChunkMap[var3].maxHeight;
         int var14 = this.ChunkMap[var3].minHeight;
         if (IsoPlayer.getInstance().getZ() < 0.0F) {
            var13 = Math.min(-1, PZMath.fastfloor(IsoPlayer.getInstance().getZ()));
            var14 = Math.max(var14, PZMath.fastfloor(IsoPlayer.getInstance().getZ()));
         }

         int var15;
         for(var15 = 0; var15 < 64; ++var15) {
            GridStack = (ArrayList)var7.Squares.get(var15);
            GridStack.clear();
         }

         for(var15 = var13; var15 >= var14; --var15) {
            GridStack = (ArrayList)var7.Squares.get(var15 + 32);
            GridStack.clear();
            if (var15 < this.maxZ) {
               boolean var20;
               if (DebugOptions.instance.Terrain.RenderTiles.NewRender.getValue()) {
                  DiamondMatrixIterator var24 = this.diamondMatrixIterator.reset(this.maxX - this.minX);
                  IsoGridSquare var26 = null;
                  Vector2i var28 = this.diamondMatrixPos;

                  while(var24.next(var28)) {
                     if (var28.y < this.maxY - this.minY + 1) {
                        var26 = this.ChunkMap[var3].getGridSquare(var28.x + this.minX, var28.y + this.minY, var15);
                        if (var15 == 0) {
                           var8[var28.x][var28.y][0] = false;
                           var8[var28.x][var28.y][1] = false;
                           var9[var28.x][var28.y] = false;
                        }

                        if (var26 == null) {
                           WeatherFxMask.addMaskLocation((IsoGridSquare)null, var28.x + this.minX, var28.y + this.minY, var15);
                        } else {
                           IsoChunk var27 = var26.getChunk();
                           if (var27 != null && var26.IsOnScreen(true)) {
                              WeatherFxMask.addMaskLocation(var26, var28.x + this.minX, var28.y + this.minY, var15);
                              var20 = this.IsDissolvedSquare(var26, var3);
                              var26.setIsDissolved(var3, var20, var4);
                              if (!var26.getIsDissolved(var3, var4)) {
                                 var26.cacheLightInfo();
                                 GridStack.add(var26);
                              }
                           }
                        }
                     }
                  }
               } else {
                  label113:
                  for(int var16 = this.minY; var16 < this.maxY; ++var16) {
                     int var17 = this.minX;
                     IsoGridSquare var18 = this.ChunkMap[var3].getGridSquare(var17, var16, var15);
                     int var19 = IsoDirections.E.index();

                     while(true) {
                        while(true) {
                           if (var17 >= this.maxX) {
                              continue label113;
                           }

                           if (var15 == 0) {
                              var8[var17 - this.minX][var16 - this.minY][0] = false;
                              var8[var17 - this.minX][var16 - this.minY][1] = false;
                              var9[var17 - this.minX][var16 - this.minY] = false;
                           }

                           if (var18 != null && var18.getY() != var16) {
                              var18 = null;
                           }

                           var20 = true;
                           boolean var21 = true;
                           IsoChunkMap var10002 = this.ChunkMap[var3];
                           int var10001 = this.ChunkMap[var3].WorldX - IsoChunkMap.ChunkGridWidth / 2;
                           var10002 = this.ChunkMap[var3];
                           int var22 = var17 - var10001 * 8;
                           var10002 = this.ChunkMap[var3];
                           var10001 = this.ChunkMap[var3].WorldY - IsoChunkMap.ChunkGridWidth / 2;
                           var10002 = this.ChunkMap[var3];
                           int var23 = var16 - var10001 * 8;
                           IsoChunkMap var25 = this.ChunkMap[var3];
                           IsoChunk var29 = var22 / 8;
                           var25 = this.ChunkMap[var3];
                           var23 /= 8;
                           if (var29 != var10 || var23 != var11) {
                              var29 = this.ChunkMap[var3].getChunkForGridSquare(var17, var16);
                              if (var29 != null) {
                                 var12 = var29.maxLevel;
                              }
                           }

                           var10 = var29;
                           var11 = var23;
                           if (var12 < var15) {
                              ++var17;
                           } else {
                              if (var18 == null) {
                                 var18 = this.getGridSquare(var17, var16, var15);
                                 if (var18 == null) {
                                    var18 = this.ChunkMap[var3].getGridSquare(var17, var16, var15);
                                    if (var18 == null) {
                                       ++var17;
                                       continue;
                                    }
                                 }
                              }

                              var29 = var18.getChunk();
                              if (var29 != null && var18.IsOnScreen(true)) {
                                 WeatherFxMask.addMaskLocation(var18, var18.x, var18.y, var15);
                                 boolean var30 = this.IsDissolvedSquare(var18, var3);
                                 var18.setIsDissolved(var3, var30, var4);
                                 if (!var18.getIsDissolved(var3, var4)) {
                                    var18.cacheLightInfo();
                                    GridStack.add(var18);
                                 }
                              }

                              var18 = var18.nav[var19];
                              ++var17;
                           }
                        }
                     }
                  }
               }
            }
         }

         this.CullFullyOccludedSquares(var7, var8, var9);
      }
   }

   public void flattenAnyFoliage(PerPlayerRender var1, int var2) {
      boolean[][] var3 = var1.FlattenGrassEtc;

      int var4;
      for(var4 = this.minY; var4 <= this.maxY; ++var4) {
         for(int var5 = this.minX; var5 <= this.maxX; ++var5) {
            var3[var5 - this.minX][var4 - this.minY] = false;
         }
      }

      for(var4 = 0; var4 < this.vehicles.size(); ++var4) {
         BaseVehicle var10 = (BaseVehicle)this.vehicles.get(var4);
         if (!(var10.getAlpha(var2) <= 0.0F)) {
            for(int var6 = -2; var6 < 5; ++var6) {
               for(int var7 = -2; var7 < 5; ++var7) {
                  int var8 = PZMath.fastfloor(var10.getX()) + var7;
                  int var9 = PZMath.fastfloor(var10.getY()) + var6;
                  if (var8 >= this.minX && var8 <= this.maxX && var9 >= this.minY && var9 <= this.maxY) {
                     var3[var8 - this.minX][var9 - this.minY] = true;
                  }
               }
            }
         }
      }

   }

   private void performRenderTiles(PerPlayerRender var1, int var2, int var3, long var4) {
      IsoGridStack var6 = var1.GridStacks;
      boolean[][] var7 = var1.FlattenGrassEtc;
      Shader var8;
      Shader var9;
      if (Core.bDebug && !DebugOptions.instance.Terrain.RenderTiles.UseShaders.getValue()) {
         var8 = null;
         var9 = null;
      } else {
         var8 = m_floorRenderShader;
         var9 = m_wallRenderShader;
      }

      for(int var10 = -32; var10 < var2 + 1; ++var10) {
         s_performance.renderTiles.PerformRenderTilesLayer var11 = (s_performance.renderTiles.PerformRenderTilesLayer)IsoCell.s_performance.renderTiles.performRenderTilesLayers.start(var10 + 32);
         GridStack = (ArrayList)var6.Squares.get(var10 + 32);
         ShadowSquares.clear();
         SolidFloor.clear();
         ShadedFloor.clear();
         VegetationCorpses.clear();
         MinusFloorCharacters.clear();
         IndieGL.glClear(256);
         if (var10 == 0 && DebugOptions.instance.Terrain.RenderTiles.Water.getValue() && DebugOptions.instance.Terrain.RenderTiles.WaterBody.getValue()) {
            var11.renderIsoWater.start();
            IsoWater.getInstance().render(GridStack);
            var11.renderIsoWater.end();
         }

         var11.renderFloor.start();

         int var12;
         IsoGridSquare var13;
         for(var12 = 0; var12 < GridStack.size(); ++var12) {
            var13 = (IsoGridSquare)GridStack.get(var12);
            if (var13.chunk == null || !var13.chunk.bLightingNeverDone[var3]) {
               var13.bFlattenGrassEtc = var10 == 0 && var7[var13.x - this.minX][var13.y - this.minY];
               int var14 = var13.renderFloor(var8);
               if (!var13.getStaticMovingObjects().isEmpty()) {
                  var14 |= 2;
                  var14 |= 16;
                  if (var13.HasStairs()) {
                     var14 |= 4;
                  }
               }

               if (!var13.getWorldObjects().isEmpty()) {
                  var14 |= 2;
               }

               if (!var13.getLocalTemporaryObjects().isEmpty()) {
                  var14 |= 4;
               }

               for(int var15 = 0; var15 < var13.getMovingObjects().size(); ++var15) {
                  IsoMovingObject var16 = (IsoMovingObject)var13.getMovingObjects().get(var15);
                  boolean var17 = var16.bOnFloor;
                  if (var17 && var16 instanceof IsoZombie) {
                     IsoZombie var18 = (IsoZombie)var16;
                     var17 = var18.isProne();
                     if (!BaseVehicle.RENDER_TO_TEXTURE) {
                        var17 = false;
                     }
                  }

                  if (var17) {
                     var14 |= 2;
                  } else {
                     var14 |= 4;
                  }

                  var14 |= 16;
               }

               if (!var13.getDeferedCharacters().isEmpty()) {
                  var14 |= 4;
               }

               if (var13.hasFlies()) {
                  var14 |= 4;
               }

               if ((var14 & 1) != 0) {
                  SolidFloor.add(var13);
               }

               if ((var14 & 8) != 0) {
                  ShadedFloor.add(var13);
               }

               if ((var14 & 2) != 0) {
                  VegetationCorpses.add(var13);
               }

               if ((var14 & 4) != 0) {
                  MinusFloorCharacters.add(var13);
               }

               if ((var14 & 16) != 0) {
                  ShadowSquares.add(var13);
               }
            }
         }

         var11.renderFloor.end();
         var11.renderPuddles.start();
         IsoPuddles.getInstance().render(SolidFloor, var10);
         var11.renderPuddles.end();
         if (var10 == 0 && DebugOptions.instance.Terrain.RenderTiles.Water.getValue() && DebugOptions.instance.Terrain.RenderTiles.WaterShore.getValue()) {
            var11.renderShore.start();
            IsoWater.getInstance().renderShore(GridStack);
            var11.renderShore.end();
         }

         if (!SolidFloor.isEmpty()) {
            var11.renderSnow.start();
            this.RenderSnow(var10);
            var11.renderSnow.end();
         }

         if (!GridStack.isEmpty()) {
            var11.renderBlood.start();
            this.ChunkMap[var3].renderBloodForChunks(var10);
            var11.renderBlood.end();
         }

         if (!ShadedFloor.isEmpty()) {
            var11.renderFloorShading.start();
            this.RenderFloorShading(var10);
            var11.renderFloorShading.end();
         }

         WorldMarkers.instance.renderGridSquareMarkers(var1, var10, var3);
         if (DebugOptions.instance.Terrain.RenderTiles.Shadows.getValue()) {
            var11.renderShadows.start();
            this.renderShadows();
            var11.renderShadows.end();
         }

         if (DebugOptions.instance.Terrain.RenderTiles.Lua.getValue()) {
            var11.luaOnPostFloorLayerDraw.start();
            LuaEventManager.triggerEvent("OnPostFloorLayerDraw", var10);
            var11.luaOnPostFloorLayerDraw.end();
         }

         IsoMarkers.instance.renderIsoMarkers(var1, var10, var3);
         IsoMarkers.instance.renderCircleIsoMarkers(var1, var10, var3);
         int var29;
         if (DebugOptions.instance.Terrain.RenderTiles.VegetationCorpses.getValue()) {
            var11.vegetationCorpses.start();

            for(var12 = 0; var12 < VegetationCorpses.size(); ++var12) {
               var13 = (IsoGridSquare)VegetationCorpses.get(var12);
               byte var31 = 0;
               byte var24 = 0;
               byte var26 = 0;
               byte var28 = 0;
               var29 = 0;
               var13.renderMinusFloor(this.maxZ, false, true, var31, var24, var26, var28, var29, var9);
               var13.renderCharacters(this.maxZ, true, true);
            }

            var11.vegetationCorpses.end();
         }

         ImprovedFog.startRender(var3, var10);
         if (DebugOptions.instance.Terrain.RenderTiles.MinusFloorCharacters.getValue()) {
            var11.minusFloorCharacters.start();

            for(var12 = 0; var12 < MinusFloorCharacters.size(); ++var12) {
               var13 = (IsoGridSquare)MinusFloorCharacters.get(var12);
               IsoGridSquare var32 = var13.nav[IsoDirections.N.index()];
               IsoGridSquare var25 = var13.nav[IsoDirections.S.index()];
               IsoGridSquare var27 = var13.nav[IsoDirections.W.index()];
               IsoGridSquare var30 = var13.nav[IsoDirections.E.index()];
               var29 = var13.getPlayerCutawayFlag(var3, var4);
               int var19 = var32 == null ? 0 : var32.getPlayerCutawayFlag(var3, var4);
               int var20 = var25 == null ? 0 : var25.getPlayerCutawayFlag(var3, var4);
               int var21 = var27 == null ? 0 : var27.getPlayerCutawayFlag(var3, var4);
               int var22 = var30 == null ? 0 : var30.getPlayerCutawayFlag(var3, var4);
               this.currentLY = var13.getY() - this.minY;
               this.currentLZ = var10;
               ImprovedFog.renderRowsBehind(var13);
               boolean var23 = var13.renderMinusFloor(this.maxZ, false, false, var29, var19, var20, var21, var22, var9);
               var13.renderDeferredCharacters(this.maxZ);
               var13.renderCharacters(this.maxZ, false, true);
               if (var13.hasFlies()) {
                  CorpseFlies.render(var13.x, var13.y, var13.z);
               }

               if (var23) {
                  var13.renderMinusFloor(this.maxZ, true, false, var29, var19, var20, var21, var22, var9);
               }
            }

            var11.minusFloorCharacters.end();
         }

         IsoMarkers.instance.renderIsoMarkersDeferred(var1, var10, var3);
         ImprovedFog.endRender();
         var11.end();
      }

   }

   public void renderShadows() {
      boolean var1 = Core.getInstance().getOptionCorpseShadows();

      for(int var2 = 0; var2 < ShadowSquares.size(); ++var2) {
         IsoGridSquare var3 = (IsoGridSquare)ShadowSquares.get(var2);

         int var4;
         IsoMovingObject var5;
         for(var4 = 0; var4 < var3.getMovingObjects().size(); ++var4) {
            var5 = (IsoMovingObject)var3.getMovingObjects().get(var4);
            IsoGameCharacter var6 = (IsoGameCharacter)Type.tryCastTo(var5, IsoGameCharacter.class);
            if (var6 != null) {
               var6.renderShadow(var6.getX(), var6.getY(), var6.getZ());
            } else {
               BaseVehicle var7 = (BaseVehicle)Type.tryCastTo(var5, BaseVehicle.class);
               if (var7 != null) {
                  var7.renderShadow();
               }
            }
         }

         if (var1) {
            for(var4 = 0; var4 < var3.getStaticMovingObjects().size(); ++var4) {
               var5 = (IsoMovingObject)var3.getStaticMovingObjects().get(var4);
               IsoDeadBody var8 = (IsoDeadBody)Type.tryCastTo(var5, IsoDeadBody.class);
               if (var8 != null) {
                  var8.renderShadow();
               }
            }
         }
      }

   }

   public void renderDebugPhysics(int var1) {
      if (Core.bDebug && (DebugOptions.instance.PhysicsRender.getValue() || RagdollControllerDebugRenderer.renderDebugPhysics() || DebugOptions.instance.PhysicsRenderBallisticsTargets.getValue() || DebugOptions.instance.PhysicsRenderBallisticsControllers.getValue())) {
         TextureDraw.GenericDrawer var2 = WorldSimulation.getDrawer(var1);
         SpriteRenderer.instance.drawGeneric(var2);
      }

   }

   public void renderDebugLighting(PerPlayerRender var1, int var2) {
      if (Core.bDebug && DebugOptions.instance.LightingRender.getValue()) {
         IsoGridStack var3 = var1.GridStacks;
         byte var4 = 1;

         for(int var5 = this.ChunkMap[IsoPlayer.getPlayerIndex()].minHeight; var5 < var2 + 1; ++var5) {
            GridStack = (ArrayList)var3.Squares.get(var5 + 32);

            for(int var6 = 0; var6 < GridStack.size(); ++var6) {
               IsoGridSquare var7 = (IsoGridSquare)GridStack.get(var6);
               float var8 = IsoUtils.XToScreenExact((float)var7.x + 0.3F, (float)var7.y, 0.0F, 0);
               float var9 = IsoUtils.YToScreenExact((float)var7.x + 0.3F, (float)var7.y, 0.0F, 0);
               float var10 = IsoUtils.XToScreenExact((float)var7.x + 0.6F, (float)var7.y, 0.0F, 0);
               float var11 = IsoUtils.YToScreenExact((float)var7.x + 0.6F, (float)var7.y, 0.0F, 0);
               float var12 = IsoUtils.XToScreenExact((float)(var7.x + 1), (float)var7.y + 0.3F, 0.0F, 0);
               float var13 = IsoUtils.YToScreenExact((float)(var7.x + 1), (float)var7.y + 0.3F, 0.0F, 0);
               float var14 = IsoUtils.XToScreenExact((float)(var7.x + 1), (float)var7.y + 0.6F, 0.0F, 0);
               float var15 = IsoUtils.YToScreenExact((float)(var7.x + 1), (float)var7.y + 0.6F, 0.0F, 0);
               float var16 = IsoUtils.XToScreenExact((float)var7.x + 0.6F, (float)(var7.y + 1), 0.0F, 0);
               float var17 = IsoUtils.YToScreenExact((float)var7.x + 0.6F, (float)(var7.y + 1), 0.0F, 0);
               float var18 = IsoUtils.XToScreenExact((float)var7.x + 0.3F, (float)(var7.y + 1), 0.0F, 0);
               float var19 = IsoUtils.YToScreenExact((float)var7.x + 0.3F, (float)(var7.y + 1), 0.0F, 0);
               float var20 = IsoUtils.XToScreenExact((float)var7.x, (float)var7.y + 0.6F, 0.0F, 0);
               float var21 = IsoUtils.YToScreenExact((float)var7.x, (float)var7.y + 0.6F, 0.0F, 0);
               float var22 = IsoUtils.XToScreenExact((float)var7.x, (float)var7.y + 0.3F, 0.0F, 0);
               float var23 = IsoUtils.YToScreenExact((float)var7.x, (float)var7.y + 0.3F, 0.0F, 0);
               if (IsoGridSquare.getMatrixBit(var7.visionMatrix, (int)0, (int)0, (int)var4)) {
                  LineDrawer.drawLine(var8, var9, var10, var11, 1.0F, 0.0F, 0.0F, 1.0F, 0);
               }

               if (IsoGridSquare.getMatrixBit(var7.visionMatrix, (int)0, (int)1, (int)var4)) {
                  LineDrawer.drawLine(var10, var11, var12, var13, 1.0F, 0.0F, 0.0F, 1.0F, 0);
               }

               if (IsoGridSquare.getMatrixBit(var7.visionMatrix, (int)0, (int)2, (int)var4)) {
                  LineDrawer.drawLine(var12, var13, var14, var15, 1.0F, 0.0F, 0.0F, 1.0F, 0);
               }

               if (IsoGridSquare.getMatrixBit(var7.visionMatrix, (int)1, (int)2, (int)var4)) {
                  LineDrawer.drawLine(var14, var15, var16, var17, 1.0F, 0.0F, 0.0F, 1.0F, 0);
               }

               if (IsoGridSquare.getMatrixBit(var7.visionMatrix, (int)2, (int)2, (int)var4)) {
                  LineDrawer.drawLine(var16, var17, var18, var19, 1.0F, 0.0F, 0.0F, 1.0F, 0);
               }

               if (IsoGridSquare.getMatrixBit(var7.visionMatrix, (int)2, (int)1, (int)var4)) {
                  LineDrawer.drawLine(var18, var19, var20, var21, 1.0F, 0.0F, 0.0F, 1.0F, 0);
               }

               if (IsoGridSquare.getMatrixBit(var7.visionMatrix, (int)2, (int)0, (int)var4)) {
                  LineDrawer.drawLine(var20, var21, var22, var23, 1.0F, 0.0F, 0.0F, 1.0F, 0);
               }

               if (IsoGridSquare.getMatrixBit(var7.visionMatrix, (int)1, (int)0, (int)var4)) {
                  LineDrawer.drawLine(var22, var23, var8, var9, 1.0F, 0.0F, 0.0F, 1.0F, 0);
               }
            }
         }
      }

   }

   private void CullFullyOccludedSquares(IsoGridStack var1, boolean[][][] var2, boolean[][] var3) {
      int var4 = 0;

      int var5;
      for(var5 = 1; var5 < MaxHeight + 1; ++var5) {
         var4 += ((ArrayList)var1.Squares.get(var5 + 32)).size();
      }

      if (var4 >= 500) {
         var5 = 0;

         for(int var6 = this.ChunkMap[IsoPlayer.getPlayerIndex()].maxHeight; var6 >= this.ChunkMap[IsoPlayer.getPlayerIndex()].minHeight; --var6) {
            GridStack = (ArrayList)var1.Squares.get(var6 + 32);

            for(int var7 = GridStack.size() - 1; var7 >= 0; --var7) {
               IsoGridSquare var8 = (IsoGridSquare)GridStack.get(var7);
               int var9 = var8.getX() - var6 * 3 - this.minX;
               int var10 = var8.getY() - var6 * 3 - this.minY;
               if (var9 >= 0 && var9 < var3.length) {
                  if (var10 >= 0 && var10 < var3[0].length) {
                     boolean var11;
                     if (var6 < MaxHeight) {
                        var11 = !var3[var9][var10];
                        if (var11) {
                           var11 = false;
                           if (var9 > 2) {
                              if (var10 > 2) {
                                 var11 = !var2[var9 - 3][var10 - 3][0] || !var2[var9 - 3][var10 - 3][1] || !var2[var9 - 3][var10 - 2][0] || !var2[var9 - 2][var10 - 3][1] || !var2[var9 - 2][var10 - 2][0] || !var2[var9 - 2][var10 - 2][1] || !var2[var9 - 2][var10 - 1][0] || !var2[var9 - 1][var10 - 2][0] || !var2[var9 - 1][var10 - 1][1] || !var2[var9 - 1][var10 - 1][0] || !var2[var9 - 1][var10][0] || !var2[var9][var10 - 1][1] || !var2[var9][var10][0] || !var2[var9][var10][1];
                              } else if (var10 > 1) {
                                 var11 = !var2[var9 - 3][var10 - 2][0] || !var2[var9 - 2][var10 - 2][0] || !var2[var9 - 2][var10 - 2][1] || !var2[var9 - 2][var10 - 1][0] || !var2[var9 - 1][var10 - 2][0] || !var2[var9 - 1][var10 - 1][1] || !var2[var9 - 1][var10 - 1][0] || !var2[var9 - 1][var10][0] || !var2[var9][var10 - 1][1] || !var2[var9][var10][0] || !var2[var9][var10][1];
                              } else if (var10 > 0) {
                                 var11 = !var2[var9 - 2][var10 - 1][0] || !var2[var9 - 1][var10 - 1][1] || !var2[var9 - 1][var10 - 1][0] || !var2[var9 - 1][var10][0] || !var2[var9][var10 - 1][1] || !var2[var9][var10][0] || !var2[var9][var10][1];
                              } else {
                                 var11 = !var2[var9 - 1][var10][0] || !var2[var9][var10][0] || !var2[var9][var10][1];
                              }
                           } else if (var9 > 1) {
                              if (var10 > 2) {
                                 var11 = !var2[var9 - 2][var10 - 3][1] || !var2[var9 - 2][var10 - 2][0] || !var2[var9 - 2][var10 - 2][1] || !var2[var9 - 2][var10 - 1][0] || !var2[var9 - 1][var10 - 2][0] || !var2[var9 - 1][var10 - 1][1] || !var2[var9 - 1][var10 - 1][0] || !var2[var9 - 1][var10][0] || !var2[var9][var10 - 1][1] || !var2[var9][var10][0] || !var2[var9][var10][1];
                              } else if (var10 > 1) {
                                 var11 = !var2[var9 - 2][var10 - 2][0] || !var2[var9 - 2][var10 - 2][1] || !var2[var9 - 2][var10 - 1][0] || !var2[var9 - 1][var10 - 2][0] || !var2[var9 - 1][var10 - 1][1] || !var2[var9 - 1][var10 - 1][0] || !var2[var9 - 1][var10][0] || !var2[var9][var10 - 1][1] || !var2[var9][var10][0] || !var2[var9][var10][1];
                              } else if (var10 > 0) {
                                 var11 = !var2[var9 - 2][var10 - 1][0] || !var2[var9 - 1][var10 - 1][1] || !var2[var9 - 1][var10 - 1][0] || !var2[var9 - 1][var10][0] || !var2[var9][var10 - 1][1] || !var2[var9][var10][0] || !var2[var9][var10][1];
                              } else {
                                 var11 = !var2[var9 - 1][var10][0] || !var2[var9][var10][0] || !var2[var9][var10][1];
                              }
                           } else if (var9 > 0) {
                              if (var10 > 2) {
                                 var11 = !var2[var9 - 1][var10 - 2][0] || !var2[var9 - 1][var10 - 1][1] || !var2[var9 - 1][var10 - 1][0] || !var2[var9 - 1][var10][0] || !var2[var9][var10 - 1][1] || !var2[var9][var10][0] || !var2[var9][var10][1];
                              } else if (var10 > 1) {
                                 var11 = !var2[var9 - 1][var10 - 2][0] || !var2[var9 - 1][var10 - 1][1] || !var2[var9 - 1][var10 - 1][0] || !var2[var9 - 1][var10][0] || !var2[var9][var10 - 1][1] || !var2[var9][var10][0] || !var2[var9][var10][1];
                              } else if (var10 > 0) {
                                 var11 = !var2[var9 - 1][var10 - 1][1] || !var2[var9 - 1][var10 - 1][0] || !var2[var9 - 1][var10][0] || !var2[var9][var10 - 1][1] || !var2[var9][var10][0] || !var2[var9][var10][1];
                              } else {
                                 var11 = !var2[var9 - 1][var10][0] || !var2[var9][var10][0] || !var2[var9][var10][1];
                              }
                           } else if (var10 > 2) {
                              var11 = !var2[var9][var10 - 1][1] || !var2[var9][var10][0] || !var2[var9][var10][1];
                           } else if (var10 > 1) {
                              var11 = !var2[var9][var10 - 1][1] || !var2[var9][var10][0] || !var2[var9][var10][1];
                           } else if (var10 > 0) {
                              var11 = !var2[var9][var10 - 1][1] || !var2[var9][var10][0] || !var2[var9][var10][1];
                           } else {
                              var11 = !var2[var9][var10][0] || !var2[var9][var10][1];
                           }
                        }

                        if (!var11) {
                           GridStack.remove(var7);
                           var3[var9][var10] = true;
                           continue;
                        }
                     }

                     ++var5;
                     var11 = IsoGridSquare.getMatrixBit(var8.visionMatrix, (int)0, (int)1, (int)1) && var8.getProperties().Is(IsoFlagType.cutW);
                     boolean var12 = IsoGridSquare.getMatrixBit(var8.visionMatrix, (int)1, (int)0, (int)1) && var8.getProperties().Is(IsoFlagType.cutN);
                     boolean var13 = false;
                     int var14;
                     if (var11 || var12) {
                        var13 = ((float)var8.x > IsoCamera.frameState.CamCharacterX || (float)var8.y > IsoCamera.frameState.CamCharacterY) && var8.z >= PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ);
                        if (var13) {
                           var14 = (int)(var8.CachedScreenX - IsoCamera.frameState.OffX);
                           int var15 = (int)(var8.CachedScreenY - IsoCamera.frameState.OffY);
                           if (var14 + 32 * Core.TileScale <= this.StencilX1 || var14 - 32 * Core.TileScale >= this.StencilX2 || var15 + 32 * Core.TileScale <= this.StencilY1 || var15 - 96 * Core.TileScale >= this.StencilY2) {
                              var13 = false;
                           }
                        }
                     }

                     var14 = 0;
                     if (var11 && !var13) {
                        ++var14;
                        if (var9 > 0) {
                           var2[var9 - 1][var10][0] = true;
                           if (var10 > 0) {
                              var2[var9 - 1][var10 - 1][1] = true;
                           }
                        }

                        if (var9 > 1 && var10 > 0) {
                           var2[var9 - 2][var10 - 1][0] = true;
                           if (var10 > 1) {
                              var2[var9 - 2][var10 - 2][1] = true;
                           }
                        }

                        if (var9 > 2 && var10 > 1) {
                           var2[var9 - 3][var10 - 2][0] = true;
                           if (var10 > 2) {
                              var2[var9 - 3][var10 - 3][1] = true;
                           }
                        }
                     }

                     if (var12 && !var13) {
                        ++var14;
                        if (var10 > 0) {
                           var2[var9][var10 - 1][1] = true;
                           if (var9 > 0) {
                              var2[var9 - 1][var10 - 1][0] = true;
                           }
                        }

                        if (var10 > 1 && var9 > 0) {
                           var2[var9 - 1][var10 - 2][1] = true;
                           if (var9 > 1) {
                              var2[var9 - 2][var10 - 2][0] = true;
                           }
                        }

                        if (var10 > 2 && var9 > 1) {
                           var2[var9 - 2][var10 - 3][1] = true;
                           if (var9 > 2) {
                              var2[var9 - 3][var10 - 3][0] = true;
                           }
                        }
                     }

                     if (IsoGridSquare.getMatrixBit(var8.visionMatrix, (int)1, (int)1, (int)0)) {
                        ++var14;
                        var2[var9][var10][0] = true;
                        var2[var9][var10][1] = true;
                     }

                     if (var14 == 3) {
                        var3[var9][var10] = true;
                     }
                  } else {
                     GridStack.remove(var7);
                  }
               } else {
                  GridStack.remove(var7);
               }
            }
         }

      }
   }

   public void RenderFloorShading(int var1) {
      if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Floor.LightingOld.getValue() && !DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Floor.Lighting.getValue()) {
         if (var1 < this.maxZ && PerformanceSettings.LightingFrameSkip < 3) {
            if (!Core.bDebug || !DebugOptions.instance.DebugDraw_SkipWorldShading.getValue()) {
               if (texWhite == null) {
                  texWhite = Texture.getWhite();
               }

               Texture var2 = texWhite;
               if (var2 != null) {
                  int var3 = IsoCamera.frameState.playerIndex;
                  int var4 = (int)IsoCamera.frameState.OffX;
                  int var5 = (int)IsoCamera.frameState.OffY;

                  for(int var6 = 0; var6 < ShadedFloor.size(); ++var6) {
                     IsoGridSquare var7 = (IsoGridSquare)ShadedFloor.get(var6);
                     if (var7.getProperties().Is(IsoFlagType.solidfloor)) {
                        float var8 = 0.0F;
                        float var9 = 0.0F;
                        float var10 = 0.0F;
                        if (var7.getProperties().Is(IsoFlagType.FloorHeightOneThird)) {
                           var9 = -1.0F;
                           var8 = -1.0F;
                        } else if (var7.getProperties().Is(IsoFlagType.FloorHeightTwoThirds)) {
                           var9 = -2.0F;
                           var8 = -2.0F;
                        }

                        float var11 = IsoUtils.XToScreen((float)var7.getX() + var8, (float)var7.getY() + var9, (float)var1 + var10, 0);
                        float var12 = IsoUtils.YToScreen((float)var7.getX() + var8, (float)var7.getY() + var9, (float)var1 + var10, 0);
                        var11 -= (float)var4;
                        var12 -= (float)var5;
                        int var13 = var7.getVertLight(0, var3);
                        int var14 = var7.getVertLight(1, var3);
                        int var15 = var7.getVertLight(2, var3);
                        int var16 = var7.getVertLight(3, var3);
                        if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Floor.LightingDebug.getValue()) {
                           var13 = -65536;
                           var14 = -65536;
                           var15 = -16776961;
                           var16 = -16776961;
                        }

                        var2.renderdiamond(var11 - (float)(32 * Core.TileScale), var12 + (float)(16 * Core.TileScale), (float)(64 * Core.TileScale), (float)(32 * Core.TileScale), var16, var13, var14, var15);
                     }
                  }

               }
            }
         }
      }
   }

   public boolean IsPlayerWindowPeeking(int var1) {
      return this.playerWindowPeekingRoomId[var1] != -1L;
   }

   public boolean CanBuildingSquareOccludePlayer(IsoGridSquare var1, int var2) {
      ArrayList var3 = (ArrayList)this.playerOccluderBuildings.get(var2);

      for(int var4 = 0; var4 < var3.size(); ++var4) {
         IsoBuilding var5 = (IsoBuilding)var3.get(var4);
         int var6 = var5.getDef().getX();
         int var7 = var5.getDef().getY();
         int var8 = var5.getDef().getX2() - var6;
         int var9 = var5.getDef().getY2() - var7;
         this.buildingRectTemp.setBounds(var6 - 1, var7 - 1, var8 + 2, var9 + 2);
         if (this.buildingRectTemp.contains(var1.getX(), var1.getY())) {
            return true;
         }
      }

      return false;
   }

   public long GetEffectivePlayerRoomId() {
      int var1 = IsoCamera.frameState.playerIndex;
      long var2 = this.playerWindowPeekingRoomId[var1];
      if (IsoPlayer.players[var1] != null && IsoPlayer.players[var1].isClimbing()) {
         var2 = -1L;
      }

      if (var2 != -1L) {
         return var2;
      } else {
         IsoGridSquare var4 = IsoPlayer.players[var1].current;
         return var4 != null ? var4.getRoomID() : -1L;
      }
   }

   public boolean SetCutawayRoomsForPlayer() {
      int var1 = IsoCamera.frameState.playerIndex;
      IsoPlayer var2 = IsoPlayer.players[var1];
      ArrayList var3 = (ArrayList)this.tempPrevPlayerCutawayRoomIDs.get(var1);
      this.tempPrevPlayerCutawayRoomIDs.set(var1, (ArrayList)this.tempPlayerCutawayRoomIDs.get(var1));
      this.tempPlayerCutawayRoomIDs.set(var1, var3);
      ((ArrayList)this.tempPlayerCutawayRoomIDs.get(var1)).clear();
      IsoGridSquare var4 = var2.getSquare();
      if (var4 == null) {
         return false;
      } else {
         IsoBuilding var5 = var4.getBuilding();
         long var6 = var4.getRoomID();
         if (var6 == -1L && FBORenderCutaways.getInstance().isRoofRoomSquare(var4)) {
            var6 = var4.associatedBuilding.getRoofRoomID(var4.z);
         }

         boolean var8 = false;
         if (var6 == -1L) {
            if (this.playerWindowPeekingRoomId[var1] != -1L) {
               ((ArrayList)this.tempPlayerCutawayRoomIDs.get(var1)).add(this.playerWindowPeekingRoomId[var1]);
            } else {
               var8 = this.playerCutawaysDirty[var1];
            }
         } else {
            int var9 = (int)(var2.getX() - 1.5F);
            int var10 = (int)(var2.getY() - 1.5F);
            int var11 = (int)(var2.getX() + 1.5F);
            int var12 = (int)(var2.getY() + 1.5F);

            for(int var13 = var9; var13 <= var11; ++var13) {
               for(int var14 = var10; var14 <= var12; ++var14) {
                  IsoGridSquare var15 = this.getGridSquare(var13, var14, var4.getZ());
                  if (var15 != null) {
                     long var16 = var15.getRoomID();
                     if (var16 == -1L && FBORenderCutaways.getInstance().isRoofRoomSquare(var15)) {
                        var16 = var15.associatedBuilding.getRoofRoomID(var15.z);
                     }

                     if ((var15.getCanSee(var1) || var4 == var15) && var16 != -1L && !((ArrayList)this.tempPlayerCutawayRoomIDs.get(var1)).contains(var16)) {
                        this.tempCutawaySqrVector.set((float)var15.getX() + 0.5F - var2.getX(), (float)var15.getY() + 0.5F - var2.getY());
                        if (var4 == var15 || var2.getForwardDirection().dot(this.tempCutawaySqrVector) > 0.0F) {
                           ((ArrayList)this.tempPlayerCutawayRoomIDs.get(var1)).add(var16);
                        }
                     }
                  }
               }
            }

            Collections.sort((List)this.tempPlayerCutawayRoomIDs.get(var1));
         }

         return var8 || !((ArrayList)this.tempPlayerCutawayRoomIDs.get(var1)).equals(this.tempPrevPlayerCutawayRoomIDs.get(var1));
      }
   }

   public boolean IsCutawaySquare(IsoGridSquare var1, long var2) {
      int var4 = IsoCamera.frameState.playerIndex;
      IsoPlayer var5 = IsoPlayer.players[var4];
      if (var5.current == null) {
         return false;
      } else if (var1 == null) {
         return false;
      } else {
         IsoGridSquare var6 = var5.current;
         if (var6.getZ() != var1.getZ()) {
            return false;
         } else {
            IsoGridSquare var7;
            IsoGridSquare var8;
            IsoGridSquare var9;
            if (!((ArrayList)this.tempPlayerCutawayRoomIDs.get(var4)).isEmpty()) {
               var7 = var1.nav[IsoDirections.N.index()];
               var8 = var1.nav[IsoDirections.E.index()];
               var9 = var1.nav[IsoDirections.S.index()];
               IsoGridSquare var19 = var1.nav[IsoDirections.W.index()];
               IsoGridSquare var11 = var6.nav[IsoDirections.N.index()];
               IsoGridSquare var12 = var6.nav[IsoDirections.E.index()];
               IsoGridSquare var13 = var6.nav[IsoDirections.S.index()];
               IsoGridSquare var14 = var6.nav[IsoDirections.W.index()];
               boolean var15 = false;
               boolean var16 = false;

               int var17;
               for(var17 = 0; var17 < 8; ++var17) {
                  if (var1.nav[var17] != null && var1.nav[var17].getRoomID() != var1.getRoomID()) {
                     var15 = true;
                     break;
                  }
               }

               if (!((ArrayList)this.tempPlayerCutawayRoomIDs.get(var4)).contains(var1.getRoomID())) {
                  var16 = true;
               }

               int var18;
               if (var15 || var16 || var1.getWall() != null) {
                  IsoGridSquare var20 = var1;

                  for(var18 = 0; var18 < 3; ++var18) {
                     var20 = var20.nav[IsoDirections.NW.index()];
                     if (var20 == null) {
                        break;
                     }

                     if (var20.getRoomID() != -1L && ((ArrayList)this.tempPlayerCutawayRoomIDs.get(var4)).contains(var20.getRoomID())) {
                        if ((var15 || var16) && var20.getCanSee(var4)) {
                           return true;
                        }

                        if (var1.getWall() != null && var20.isCouldSee(var4)) {
                           return true;
                        }
                     }
                  }
               }

               if (var7 != null && var19 != null && (var7.getThumpableWallOrHoppable(false) != null || var19.getThumpableWallOrHoppable(true) != null || var1.getThumpableWallOrHoppable(true) != null || var1.getThumpableWallOrHoppable(false) != null)) {
                  return this.DoesSquareHaveValidCutaways(var6, var1, var4, var2);
               }

               if (var6.getRoomID() == -1L && (var11 != null && var11.getRoomID() != -1L || var12 != null && var12.getRoomID() != -1L || var13 != null && var13.getRoomID() != -1L || var14 != null && var14.getRoomID() != -1L)) {
                  var17 = var6.x - var1.x;
                  var18 = var6.y - var1.y;
                  if (var17 < 0 && var18 < 0) {
                     if (var17 >= -3) {
                        if (var18 >= -3) {
                           return true;
                        }

                        if (var7 != null && var9 != null && var1.getWall(false) != null && var7.getWall(false) != null && var9.getWall(false) != null && var9.getPlayerCutawayFlag(var4, var2) != 0) {
                           return true;
                        }
                     } else if (var8 != null && var19 != null) {
                        if (var1.getWall(true) != null && var19.getWall(true) != null && var8.getWall(true) != null && var8.getPlayerCutawayFlag(var4, var2) != 0) {
                           return true;
                        }

                        if (var1.getWall(true) != null && var19.getWall(true) != null && var8.getWall(true) != null && var8.getPlayerCutawayFlag(var4, var2) != 0) {
                           return true;
                        }
                     }
                  }
               }
            } else {
               var7 = var1.nav[IsoDirections.N.index()];
               var8 = var1.nav[IsoDirections.W.index()];
               if (this.IsCollapsibleBuildingSquare(var1)) {
                  if (var5.getZ() == 0.0F) {
                     return true;
                  }

                  if (var1.getBuilding() != null && (var6.getX() < var1.getBuilding().def.x || var6.getY() < var1.getBuilding().def.y)) {
                     return true;
                  }

                  var9 = var1;

                  for(int var10 = 0; var10 < 3; ++var10) {
                     var9 = var9.nav[IsoDirections.NW.index()];
                     if (var9 == null) {
                        break;
                     }

                     if (var9.isCanSee(var4)) {
                        return true;
                     }
                  }
               }

               if (var7 != null && var7.getRoomID() == -1L && var8 != null && var8.getRoomID() == -1L) {
                  return this.DoesSquareHaveValidCutaways(var6, var1, var4, var2);
               }
            }

            return false;
         }
      }
   }

   public boolean DoesSquareHaveValidCutaways(IsoGridSquare var1, IsoGridSquare var2, int var3, long var4) {
      IsoGridSquare var6 = var2.nav[IsoDirections.N.index()];
      IsoGridSquare var7 = var2.nav[IsoDirections.E.index()];
      IsoGridSquare var8 = var2.nav[IsoDirections.S.index()];
      IsoGridSquare var9 = var2.nav[IsoDirections.W.index()];
      IsoObject var10 = var2.getWall(true);
      IsoObject var11 = var2.getWall(false);
      IsoObject var12 = null;
      IsoObject var13 = null;
      if (var6 != null && var6.nav[IsoDirections.W.index()] != null && var6.nav[IsoDirections.W.index()].getRoomID() == var6.getRoomID()) {
         var13 = var6.getWall(false);
      }

      if (var9 != null && var9.nav[IsoDirections.N.index()] != null && var9.nav[IsoDirections.N.index()].getRoomID() == var9.getRoomID()) {
         var12 = var9.getWall(true);
      }

      int var15;
      if (var11 != null || var10 != null || var13 != null || var12 != null) {
         IsoGridSquare var14 = var2.nav[IsoDirections.NW.index()];

         for(var15 = 0; var15 < 2 && var14 != null && var14.getRoomID() == var1.getRoomID(); ++var15) {
            IsoGridSquare var16 = var14.nav[IsoDirections.S.index()];
            IsoGridSquare var17 = var14.nav[IsoDirections.E.index()];
            if (var16 != null && var16.getBuilding() != null || var17 != null && var17.getBuilding() != null) {
               break;
            }

            if (var14.isCanSee(var3) && var14.isCouldSee(var3) && var14.DistTo(var1) <= (float)(6 - (var15 + 1))) {
               return true;
            }

            if (var14.getBuilding() == null) {
               var14 = var14.nav[IsoDirections.NW.index()];
            }
         }
      }

      int var18 = var1.x - var2.x;
      var15 = var1.y - var2.y;
      if (var10 != null && var10.sprite.name.contains("fencing") || var11 != null && var11.sprite.name.contains("fencing")) {
         if (var10 != null && var12 != null && var15 >= -6 && var15 < 0) {
            return true;
         }

         if (var11 != null && var13 != null && var18 >= -6 && var18 < 0) {
            return true;
         }
      } else if (var2.DistTo(var1) <= 6.0F && var2.nav[IsoDirections.NW.index()] != null && var2.nav[IsoDirections.NW.index()].getRoomID() == var2.getRoomID() && (var2.getWall(true) == null || var2.getWall(true) == var10) && (var2.getWall(false) == null || var2.getWall(false) == var11)) {
         if (var8 != null && var6 != null && var15 != 0) {
            if (var15 > 0 && var11 != null && var8.getWall(false) != null && var6.getWall(false) != null && var8.getPlayerCutawayFlag(var3, var4) != 0) {
               return true;
            }

            if (var15 < 0 && var11 != null && var6.getWall(false) != null && var6.getPlayerCutawayFlag(var3, var4) != 0) {
               return true;
            }
         }

         if (var7 != null && var9 != null && var18 != 0) {
            if (var18 > 0 && var10 != null && var7.getWall(true) != null && var9.getWall(true) != null && var7.getPlayerCutawayFlag(var3, var4) != 0) {
               return true;
            }

            if (var18 < 0 && var10 != null && var9.getWall(true) != null && var9.getPlayerCutawayFlag(var3, var4) != 0) {
               return true;
            }
         }
      }

      if (var2 == var1 && var2.nav[IsoDirections.NW.index()] != null && var2.nav[IsoDirections.NW.index()].getRoomID() == var2.getRoomID()) {
         if (var10 != null && var6 != null && var6.getWall(false) == null && var6.isCanSee(var3) && var6.isCouldSee(var3)) {
            return true;
         }

         if (var11 != null && var9 != null && var9.getWall(true) != null && var9.isCanSee(var3) && var9.isCouldSee(var3)) {
            return true;
         }
      }

      if (var6 != null && var9 != null && var18 != 0 && var15 != 0 && var13 != null && var12 != null && var6.getPlayerCutawayFlag(var3, var4) != 0 && var9.getPlayerCutawayFlag(var3, var4) != 0) {
         return true;
      } else {
         return var18 < 0 && var18 >= -6 && var15 < 0 && var15 >= -6 && (var11 != null && var2.getWall(true) == null || var10 != null && var2.getWall(false) == null);
      }
   }

   public boolean IsCollapsibleBuildingSquare(IsoGridSquare var1) {
      if (var1.getProperties().Is(IsoFlagType.forceRender)) {
         return false;
      } else {
         int var2;
         int var4;
         IsoBuilding var5;
         BuildingDef var6;
         for(var2 = 0; var2 < 4; ++var2) {
            short var3 = 500;

            for(var4 = 0; var4 < var3 && this.playerOccluderBuildingsArr[var2] != null; ++var4) {
               var5 = this.playerOccluderBuildingsArr[var2][var4];
               if (var5 == null) {
                  break;
               }

               var6 = var5.getDef();
               if (this.collapsibleBuildingSquareAlgorithm(var6, var1, IsoPlayer.players[var2].getSquare())) {
                  return true;
               }

               if (var1.getY() - var6.getY2() == 1 && var1.getWall(true) != null) {
                  return true;
               }

               if (var1.getX() - var6.getX2() == 1 && var1.getWall(false) != null) {
                  return true;
               }
            }
         }

         var2 = IsoCamera.frameState.playerIndex;
         IsoPlayer var7 = IsoPlayer.players[var2];
         if (var7.getVehicle() != null) {
            return false;
         } else {
            for(var4 = 0; var4 < 500 && this.zombieOccluderBuildingsArr[var2] != null; ++var4) {
               var5 = this.zombieOccluderBuildingsArr[var2][var4];
               if (var5 == null) {
                  break;
               }

               var6 = var5.getDef();
               if (this.collapsibleBuildingSquareAlgorithm(var6, var1, var7.getSquare())) {
                  return true;
               }
            }

            for(var4 = 0; var4 < 500 && this.otherOccluderBuildingsArr[var2] != null; ++var4) {
               var5 = this.otherOccluderBuildingsArr[var2][var4];
               if (var5 == null) {
                  break;
               }

               var6 = var5.getDef();
               if (this.collapsibleBuildingSquareAlgorithm(var6, var1, var7.getSquare())) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   public boolean collapsibleBuildingSquareAlgorithm(BuildingDef var1, IsoGridSquare var2, IsoGridSquare var3) {
      int var4 = var1.getX();
      int var5 = var1.getY();
      int var6 = var1.getW();
      int var7 = var1.getH();
      this.buildingRectTemp.setBounds(var4, var5, var6, var7);
      if (var3.getRoomID() == -1L && this.buildingRectTemp.contains(var3.getX(), var3.getY())) {
         this.buildingRectTemp.setBounds(var4 - 1, var5 - 1, var6 + 2, var7 + 2);
         IsoGridSquare var8 = var2.nav[IsoDirections.N.index()];
         IsoGridSquare var9 = var2.nav[IsoDirections.W.index()];
         IsoGridSquare var10 = var2.nav[IsoDirections.NW.index()];
         if (var10 != null && var8 != null && var9 != null) {
            boolean var11 = var2.getRoomID() == -1L;
            boolean var12 = var8.getRoomID() == -1L;
            boolean var13 = var9.getRoomID() == -1L;
            boolean var14 = var10.getRoomID() == -1L;
            boolean var15 = var3.getY() < var2.getY();
            boolean var16 = var3.getX() < var2.getX();
            return this.buildingRectTemp.contains(var2.getX(), var2.getY()) && (var3.getZ() < var2.getZ() || var11 && (!var12 && var15 || !var13 && var16) || var11 && var12 && var13 && !var14 || !var11 && (var14 || var12 == var13 || var12 && var16 || var13 && var15));
         } else {
            return false;
         }
      } else {
         this.buildingRectTemp.setBounds(var4 - 1, var5 - 1, var6 + 2, var7 + 2);
         return this.buildingRectTemp.contains(var2.getX(), var2.getY());
      }
   }

   private boolean IsDissolvedSquare(IsoGridSquare var1, int var2) {
      IsoPlayer var3 = IsoPlayer.players[var2];
      if (var3.current == null) {
         return false;
      } else {
         IsoGridSquare var4 = var3.current;
         if (var4.getZ() >= var1.getZ()) {
            return false;
         } else if (!PerformanceSettings.NewRoofHiding) {
            return this.bHideFloors[var2] && var1.getZ() >= this.maxZ;
         } else {
            if (var1.getZ() > this.hidesOrphanStructuresAbove) {
               IsoBuilding var5 = var1.getBuilding();
               if (var5 == null) {
                  var5 = var1.roofHideBuilding;
               }

               IsoGridSquare var7;
               for(int var6 = var1.getZ() - 1; var6 >= 0 && var5 == null; --var6) {
                  var7 = this.getGridSquare(var1.x, var1.y, var6);
                  if (var7 != null) {
                     var5 = var7.getBuilding();
                     if (var5 == null) {
                        var5 = var7.roofHideBuilding;
                     }
                  }
               }

               if (var5 == null) {
                  if (var1.isSolidFloor()) {
                     return true;
                  }

                  IsoGridSquare var9 = var1.nav[IsoDirections.N.index()];
                  if (var9 != null && var9.getBuilding() == null) {
                     if (var9.getPlayerBuiltFloor() != null) {
                        return true;
                     }

                     if (var9.HasStairsBelow()) {
                        return true;
                     }
                  }

                  var7 = var1.nav[IsoDirections.W.index()];
                  if (var7 != null && var7.getBuilding() == null) {
                     if (var7.getPlayerBuiltFloor() != null) {
                        return true;
                     }

                     if (var7.HasStairsBelow()) {
                        return true;
                     }
                  }

                  if (var1.Is(IsoFlagType.WallSE)) {
                     IsoGridSquare var8 = var1.nav[IsoDirections.NW.index()];
                     if (var8 != null && var8.getBuilding() == null) {
                        if (var8.getPlayerBuiltFloor() != null) {
                           return true;
                        }

                        if (var8.HasStairsBelow()) {
                           return true;
                        }
                     }
                  }
               }
            }

            return this.IsCollapsibleBuildingSquare(var1);
         }
      }
   }

   private int GetBuildingHeightAt(IsoBuilding var1, int var2, int var3, int var4) {
      for(int var5 = MaxHeight; var5 > var4; --var5) {
         IsoGridSquare var6 = this.getGridSquare(var2, var3, var5);
         if (var6 != null && var6.getBuilding() == var1) {
            return var5;
         }
      }

      return var4;
   }

   private void updateSnow(int var1) {
      if (this.snowGridCur == null) {
         this.snowGridCur = new SnowGrid(var1);
         this.snowGridPrev = new SnowGrid(0);
      } else {
         if (var1 != this.snowGridCur.frac) {
            this.snowGridPrev.init(this.snowGridCur.frac);
            this.snowGridCur.init(var1);
            this.snowFadeTime = System.currentTimeMillis();
            DebugLog.log("snow from " + this.snowGridPrev.frac + " to " + this.snowGridCur.frac);
            if (PerformanceSettings.FBORenderChunk) {
               for(int var2 = 0; var2 < IsoPlayer.numPlayers; ++var2) {
                  IsoPlayer var3 = IsoPlayer.players[var2];
                  if (var3 != null) {
                     var3.dirtyRecalcGridStack = true;
                  }
               }
            }
         }

      }
   }

   public void setSnowTarget(int var1) {
      if (!SandboxOptions.instance.EnableSnowOnGround.getValue()) {
         var1 = 0;
      }

      this.snowFracTarget = var1;
   }

   public int getSnowTarget() {
      return this.snowFracTarget;
   }

   public boolean gridSquareIsSnow(int var1, int var2, int var3) {
      if (PerformanceSettings.FBORenderChunk) {
         return FBORenderSnow.getInstance().gridSquareIsSnow(var1, var2, var3);
      } else {
         IsoGridSquare var4 = this.getGridSquare(var1, var2, var3);
         if (var4 != null && this.snowGridCur != null) {
            if (!var4.getProperties().Is(IsoFlagType.solidfloor)) {
               return false;
            } else if (var4.getProperties().Is(IsoFlagType.water) || var4.getWater() != null && var4.getWater().isValid()) {
               return false;
            } else if (var4.getProperties().Is(IsoFlagType.exterior) && var4.room == null && !var4.isInARoom()) {
               int var5 = var4.getX() % this.snowGridCur.w;
               int var6 = var4.getY() % this.snowGridCur.h;
               return this.snowGridCur.check(var5, var6);
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   public void RenderSnow(int var1) {
      if (DebugOptions.instance.Weather.Snow.getValue()) {
         this.updateSnow(this.snowFracTarget);
         SnowGrid var2 = this.snowGridCur;
         if (var2 != null) {
            SnowGrid var3 = this.snowGridPrev;
            if (var2.frac > 0 || var3.frac > 0) {
               float var4 = 1.0F;
               float var5 = 0.0F;
               long var6 = System.currentTimeMillis();
               long var8 = var6 - this.snowFadeTime;
               if ((float)var8 < this.snowTransitionTime) {
                  float var10 = (float)var8 / this.snowTransitionTime;
                  var4 = var10;
                  var5 = 1.0F - var10;
               }

               if (PerformanceSettings.FBORenderChunk && FBORenderChunkManager.instance.isCaching()) {
                  var4 = 1.0F;
                  var5 = 0.0F;
               }

               Shader var27 = null;
               if (DebugOptions.instance.Terrain.RenderTiles.UseShaders.getValue()) {
                  var27 = m_floorRenderShader;
               }

               FloorShaperAttachedSprites.instance.setShore(false);
               FloorShaperDiamond.instance.setShore(false);
               IndieGL.StartShader(var27, IsoCamera.frameState.playerIndex);
               int var11 = (int)IsoCamera.frameState.OffX;
               int var12 = (int)IsoCamera.frameState.OffY;

               for(int var13 = 0; var13 < SolidFloor.size(); ++var13) {
                  IsoGridSquare var14 = (IsoGridSquare)SolidFloor.get(var13);
                  if (var14.room == null && var14.getProperties().Is(IsoFlagType.exterior) && var14.getProperties().Is(IsoFlagType.solidfloor)) {
                     int var15;
                     if (!var14.getProperties().Is(IsoFlagType.water) && (var14.getWater() == null || !var14.getWater().isValid())) {
                        var15 = 0;
                     } else {
                        var15 = getShoreInt(var14);
                        if (var15 == 0) {
                           continue;
                        }
                     }

                     int var16 = var14.getX() % var2.w;
                     int var17 = var14.getY() % var2.h;
                     float var18 = IsoUtils.XToScreen((float)var14.getX(), (float)var14.getY(), (float)var1, 0);
                     float var19 = IsoUtils.YToScreen((float)var14.getX(), (float)var14.getY(), (float)var1, 0);
                     var18 -= (float)var11;
                     var19 -= (float)var12;
                     if (PerformanceSettings.FBORenderChunk && FBORenderChunkManager.instance.isCaching()) {
                        var18 = IsoUtils.XToScreen((float)(var14.getX() % 8), (float)(var14.getY() % 8), (float)var1, 0);
                        var19 = IsoUtils.YToScreen((float)(var14.getX() % 8), (float)(var14.getY() % 8), (float)var1, 0);
                        var18 += FBORenderChunkManager.instance.getXOffset();
                        var19 += FBORenderChunkManager.instance.getYOffset();
                     }

                     float var20 = (float)(32 * Core.TileScale);
                     float var21 = (float)(96 * Core.TileScale);
                     var18 -= var20;
                     var19 -= var21;
                     int var22 = IsoCamera.frameState.playerIndex;
                     int var23 = var14.getVertLight(0, var22);
                     int var24 = var14.getVertLight(1, var22);
                     int var25 = var14.getVertLight(2, var22);
                     int var26 = var14.getVertLight(3, var22);
                     if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Floor.LightingDebug.getValue()) {
                        var23 = -65536;
                        var24 = -65536;
                        var25 = -16776961;
                        var26 = -16776961;
                     }

                     FloorShaperAttachedSprites.instance.setVertColors(var23, var24, var25, var26);
                     FloorShaperDiamond.instance.setVertColors(var23, var24, var25, var26);

                     for(var22 = 0; var22 < 2; ++var22) {
                        if (var5 > var4) {
                           this.renderSnowTileGeneral(var2, var4, var14, var15, var16, var17, (int)var18, (int)var19, var22);
                           this.renderSnowTileGeneral(var3, var5, var14, var15, var16, var17, (int)var18, (int)var19, var22);
                        } else {
                           this.renderSnowTileGeneral(var3, var5, var14, var15, var16, var17, (int)var18, (int)var19, var22);
                           this.renderSnowTileGeneral(var2, var4, var14, var15, var16, var17, (int)var18, (int)var19, var22);
                        }
                     }
                  }
               }

               IndieGL.StartShader((Shader)null);
            }
         }
      }
   }

   private void renderSnowTileGeneral(SnowGrid var1, float var2, IsoGridSquare var3, int var4, int var5, int var6, int var7, int var8, int var9) {
      if (!(var2 <= 0.0F)) {
         Texture var10 = var1.grid[var5][var6][var9];
         if (var10 != null) {
            if (var9 == 0) {
               this.renderSnowTile(var1, var5, var6, var9, var3, var4, var10, var7, var8, var2);
            } else if (var4 == 0) {
               byte var11 = var1.gridType[var5][var6][var9];
               this.renderSnowTileBase(var10, var7, var8, var2, var11 < this.m_snowFirstNonSquare);
            }

         }
      }
   }

   private void renderSnowTileBase(Texture var1, int var2, int var3, float var4, boolean var5) {
      Object var6 = var5 ? FloorShaperDiamond.instance : FloorShaperAttachedSprites.instance;
      ((FloorShaper)var6).setAlpha4(var4);
      var1.render((float)var2, (float)var3, (float)var1.getWidth(), (float)var1.getHeight(), 1.0F, 1.0F, 1.0F, var4, (Consumer)var6);
   }

   private void renderSnowTile(SnowGrid var1, int var2, int var3, int var4, IsoGridSquare var5, int var6, Texture var7, int var8, int var9, float var10) {
      if (var6 == 0) {
         byte var21 = var1.gridType[var2][var3][var4];
         this.renderSnowTileBase(var7, var8, var9, var10, var21 < this.m_snowFirstNonSquare);
      } else {
         int var11 = 0;
         boolean var16 = var1.check(var2, var3);
         boolean var12 = (var6 & 1) == 1 && (var16 || var1.check(var2, var3 - 1));
         boolean var15 = (var6 & 2) == 2 && (var16 || var1.check(var2 + 1, var3));
         boolean var13 = (var6 & 4) == 4 && (var16 || var1.check(var2, var3 + 1));
         boolean var14 = (var6 & 8) == 8 && (var16 || var1.check(var2 - 1, var3));
         if (var12) {
            ++var11;
         }

         if (var13) {
            ++var11;
         }

         if (var15) {
            ++var11;
         }

         if (var14) {
            ++var11;
         }

         SnowGridTiles var17 = null;
         SnowGridTiles var18 = null;
         boolean var19 = false;
         if (var11 != 0) {
            if (var11 == 1) {
               if (var12) {
                  var17 = this.snowGridTiles_Strip[0];
               } else if (var13) {
                  var17 = this.snowGridTiles_Strip[1];
               } else if (var15) {
                  var17 = this.snowGridTiles_Strip[3];
               } else if (var14) {
                  var17 = this.snowGridTiles_Strip[2];
               }
            } else if (var11 == 2) {
               if (var12 && var13) {
                  var17 = this.snowGridTiles_Strip[0];
                  var18 = this.snowGridTiles_Strip[1];
               } else if (var15 && var14) {
                  var17 = this.snowGridTiles_Strip[2];
                  var18 = this.snowGridTiles_Strip[3];
               } else if (var12) {
                  var17 = this.snowGridTiles_Edge[var14 ? 0 : 3];
               } else if (var13) {
                  var17 = this.snowGridTiles_Edge[var14 ? 2 : 1];
               } else if (var14) {
                  var17 = this.snowGridTiles_Edge[var12 ? 0 : 2];
               } else if (var15) {
                  var17 = this.snowGridTiles_Edge[var12 ? 3 : 1];
               }
            } else if (var11 == 3) {
               if (!var12) {
                  var17 = this.snowGridTiles_Cove[1];
               } else if (!var13) {
                  var17 = this.snowGridTiles_Cove[0];
               } else if (!var15) {
                  var17 = this.snowGridTiles_Cove[2];
               } else if (!var14) {
                  var17 = this.snowGridTiles_Cove[3];
               }

               var19 = true;
            } else if (var11 == 4) {
               var17 = this.snowGridTiles_Enclosed;
               var19 = true;
            }

            if (var17 != null) {
               int var20 = (var5.getX() + var5.getY()) % var17.size();
               var7 = var17.get(var20);
               if (var7 != null) {
                  this.renderSnowTileBase(var7, var8, var9, var10, var19);
               }

               if (var18 != null) {
                  var7 = var18.get(var20);
                  if (var7 != null) {
                     this.renderSnowTileBase(var7, var8, var9, var10, false);
                  }
               }
            }

         }
      }
   }

   private static int getShoreInt(IsoGridSquare var0) {
      int var1 = 0;
      if (isSnowShore(var0, 0, -1)) {
         var1 |= 1;
      }

      if (isSnowShore(var0, 1, 0)) {
         var1 |= 2;
      }

      if (isSnowShore(var0, 0, 1)) {
         var1 |= 4;
      }

      if (isSnowShore(var0, -1, 0)) {
         var1 |= 8;
      }

      return var1;
   }

   private static boolean isSnowShore(IsoGridSquare var0, int var1, int var2) {
      IsoGridSquare var3 = IsoWorld.instance.getCell().getGridSquare(var0.getX() + var1, var0.getY() + var2, 0);
      return var3 != null && !var3.getProperties().Is(IsoFlagType.water);
   }

   public IsoBuilding getClosestBuildingExcept(IsoGameCharacter var1, IsoRoom var2) {
      IsoBuilding var3 = null;
      float var4 = 1000000.0F;

      for(int var5 = 0; var5 < this.BuildingList.size(); ++var5) {
         IsoBuilding var6 = (IsoBuilding)this.BuildingList.get(var5);

         for(int var7 = 0; var7 < var6.Exits.size(); ++var7) {
            float var8 = var1.DistTo(((IsoRoomExit)var6.Exits.get(var7)).x, ((IsoRoomExit)var6.Exits.get(var7)).y);
            if (var8 < var4 && (var2 == null || var2.building != var6)) {
               var3 = var6;
               var4 = var8;
            }
         }
      }

      return var3;
   }

   public int getDangerScore(int var1, int var2) {
      return var1 >= 0 && var2 >= 0 && var1 < this.width && var2 < this.height ? this.DangerScore.getValue(var1, var2) : 1000000;
   }

   private void ObjectDeletionAddition() {
      int var1;
      IsoMovingObject var2;
      for(var1 = 0; var1 < this.removeList.size(); ++var1) {
         var2 = (IsoMovingObject)this.removeList.get(var1);
         if (var2 instanceof IsoZombie) {
            VirtualZombieManager.instance.RemoveZombie((IsoZombie)var2);
         }

         if (!(var2 instanceof IsoPlayer) || ((IsoPlayer)var2).isDead()) {
            MovingObjectUpdateScheduler.instance.removeObject(var2);
            this.ObjectList.remove(var2);
            if (var2.getCurrentSquare() != null) {
               var2.getCurrentSquare().getMovingObjects().remove(var2);
            }

            if (var2.getLastSquare() != null) {
               var2.getLastSquare().getMovingObjects().remove(var2);
            }
         }
      }

      this.removeList.clear();

      for(var1 = 0; var1 < this.addList.size(); ++var1) {
         var2 = (IsoMovingObject)this.addList.get(var1);
         this.ObjectList.add(var2);
      }

      this.addList.clear();

      for(var1 = 0; var1 < this.addVehicles.size(); ++var1) {
         BaseVehicle var3 = (BaseVehicle)this.addVehicles.get(var1);
         if (!this.ObjectList.contains(var3)) {
            this.ObjectList.add(var3);
         }

         if (!this.vehicles.contains(var3)) {
            this.vehicles.add(var3);
         }
      }

      this.addVehicles.clear();
   }

   private void ProcessItems(Iterator<InventoryItem> var1) {
      int var2 = this.ProcessItems.size();

      int var3;
      for(var3 = 0; var3 < var2; ++var3) {
         InventoryItem var4 = (InventoryItem)this.ProcessItems.get(var3);
         var4.update();
         if (var4.finishupdate()) {
            this.ProcessItemsRemove.add(var4);
         }
      }

      var2 = this.ProcessWorldItems.size();

      for(var3 = 0; var3 < var2; ++var3) {
         IsoWorldInventoryObject var5 = (IsoWorldInventoryObject)this.ProcessWorldItems.get(var3);
         var5.update();
         if (var5.finishupdate()) {
            this.ProcessWorldItemsRemove.add(var5);
         }
      }

   }

   private void ProcessIsoObject() {
      this.ProcessIsoObject.removeAll(this.ProcessIsoObjectRemove);
      this.ProcessIsoObjectRemove.clear();
      int var1 = this.ProcessIsoObject.size();

      for(int var2 = 0; var2 < var1; ++var2) {
         IsoObject var3 = (IsoObject)this.ProcessIsoObject.get(var2);
         if (var3 != null) {
            var3.update();
            if (var1 > this.ProcessIsoObject.size()) {
               --var2;
               --var1;
            }
         }
      }

   }

   private void ProcessObjects(Iterator<IsoMovingObject> var1) {
      MovingObjectUpdateScheduler.instance.update();

      for(int var2 = 0; var2 < this.ObjectList.size(); ++var2) {
         IsoAnimal var3 = (IsoAnimal)Type.tryCastTo((IsoMovingObject)this.ObjectList.get(var2), IsoAnimal.class);
         if (var3 != null) {
            var3.updateVocalProperties();
            var3.updateLoopingSounds();
         }
      }

      GameProfiler.getInstance().invokeAndMeasure("Zombie Vocals", this, IsoCell::updateZombieVocals);
   }

   private void updateZombieVocals() {
      for(int var1 = 0; var1 < this.ZombieList.size(); ++var1) {
         IsoZombie var2 = (IsoZombie)this.ZombieList.get(var1);
         var2.updateVocalProperties();
      }

   }

   private void ProcessRemoveItems(Iterator<InventoryItem> var1) {
      this.ProcessItems.removeAll(this.ProcessItemsRemove);
      this.ProcessWorldItems.removeAll(this.ProcessWorldItemsRemove);
      this.ProcessItemsRemove.clear();
      this.ProcessWorldItemsRemove.clear();
   }

   private void ProcessStaticUpdaters() {
      int var1 = this.StaticUpdaterObjectList.size();

      for(int var2 = 0; var2 < var1; ++var2) {
         try {
            ((IsoObject)this.StaticUpdaterObjectList.get(var2)).update();
         } catch (Exception var4) {
            Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, (String)null, var4);
         }

         if (var1 > this.StaticUpdaterObjectList.size()) {
            --var2;
            --var1;
         }
      }

   }

   public void addToProcessIsoObject(IsoObject var1) {
      if (var1 != null) {
         this.ProcessIsoObjectRemove.remove(var1);
         if (!this.ProcessIsoObject.contains(var1)) {
            this.ProcessIsoObject.add(var1);
         }

      }
   }

   public void addToProcessIsoObjectRemove(IsoObject var1) {
      if (var1 != null) {
         if (this.ProcessIsoObject.contains(var1)) {
            if (!this.ProcessIsoObjectRemove.contains(var1)) {
               this.ProcessIsoObjectRemove.add(var1);
            }

         }
      }
   }

   public void addToStaticUpdaterObjectList(IsoObject var1) {
      if (var1 != null) {
         if (!this.StaticUpdaterObjectList.contains(var1)) {
            this.StaticUpdaterObjectList.add(var1);
         }

      }
   }

   public void addToProcessItems(InventoryItem var1) {
      if (var1 != null && !GameClient.bClient) {
         this.ProcessItemsRemove.remove(var1);
         if (!this.ProcessItems.contains(var1)) {
            this.ProcessItems.add(var1);
         }

      }
   }

   public void addToProcessItems(ArrayList<InventoryItem> var1) {
      if (var1 != null && !GameClient.bClient) {
         for(int var2 = 0; var2 < var1.size(); ++var2) {
            InventoryItem var3 = (InventoryItem)var1.get(var2);
            if (var3 != null) {
               this.ProcessItemsRemove.remove(var3);
               if (!this.ProcessItems.contains(var3)) {
                  this.ProcessItems.add(var3);
               }
            }
         }

      }
   }

   public void addToProcessItemsRemove(InventoryItem var1) {
      if (var1 != null) {
         if (!this.ProcessItemsRemove.contains(var1)) {
            this.ProcessItemsRemove.add(var1);
         }

      }
   }

   public void addToProcessItemsRemove(ArrayList<InventoryItem> var1) {
      if (var1 != null) {
         for(int var2 = 0; var2 < var1.size(); ++var2) {
            InventoryItem var3 = (InventoryItem)var1.get(var2);
            if (var3 != null && !this.ProcessItemsRemove.contains(var3)) {
               this.ProcessItemsRemove.add(var3);
            }
         }

      }
   }

   public void addToProcessWorldItems(IsoWorldInventoryObject var1) {
      if (var1 != null) {
         this.ProcessWorldItemsRemove.remove(var1);
         if (!this.ProcessWorldItems.contains(var1)) {
            this.ProcessWorldItems.add(var1);
         }

      }
   }

   public void addToProcessWorldItemsRemove(IsoWorldInventoryObject var1) {
      if (var1 != null) {
         if (!this.ProcessWorldItemsRemove.contains(var1)) {
            this.ProcessWorldItemsRemove.add(var1);
         }

      }
   }

   public IsoSurvivor getNetworkPlayer(int var1) {
      int var2 = this.RemoteSurvivorList.size();

      for(int var3 = 0; var3 < var2; ++var3) {
         if (((IsoGameCharacter)this.RemoteSurvivorList.get(var3)).getRemoteID() == var1) {
            return (IsoSurvivor)this.RemoteSurvivorList.get(var3);
         }
      }

      return null;
   }

   IsoGridSquare ConnectNewSquare(IsoGridSquare var1, boolean var2, boolean var3) {
      int var4 = var1.getX();
      int var5 = var1.getY();
      int var6 = var1.getZ();
      this.setCacheGridSquare(var4, var5, var6, var1);
      this.DoGridNav(var1, IsoGridSquare.cellGetSquare);
      return var1;
   }

   public void DoGridNav(IsoGridSquare var1, IsoGridSquare.GetSquare var2) {
      int var3 = var1.getX();
      int var4 = var1.getY();
      int var5 = var1.getZ();
      var1.nav[IsoDirections.N.index()] = var2.getGridSquare(var3, var4 - 1, var5);
      var1.nav[IsoDirections.NW.index()] = var2.getGridSquare(var3 - 1, var4 - 1, var5);
      var1.nav[IsoDirections.W.index()] = var2.getGridSquare(var3 - 1, var4, var5);
      var1.nav[IsoDirections.SW.index()] = var2.getGridSquare(var3 - 1, var4 + 1, var5);
      var1.nav[IsoDirections.S.index()] = var2.getGridSquare(var3, var4 + 1, var5);
      var1.nav[IsoDirections.SE.index()] = var2.getGridSquare(var3 + 1, var4 + 1, var5);
      var1.nav[IsoDirections.E.index()] = var2.getGridSquare(var3 + 1, var4, var5);
      var1.nav[IsoDirections.NE.index()] = var2.getGridSquare(var3 + 1, var4 - 1, var5);
      if (var1.nav[IsoDirections.N.index()] != null) {
         var1.nav[IsoDirections.N.index()].nav[IsoDirections.S.index()] = var1;
      }

      if (var1.nav[IsoDirections.NW.index()] != null) {
         var1.nav[IsoDirections.NW.index()].nav[IsoDirections.SE.index()] = var1;
      }

      if (var1.nav[IsoDirections.W.index()] != null) {
         var1.nav[IsoDirections.W.index()].nav[IsoDirections.E.index()] = var1;
      }

      if (var1.nav[IsoDirections.SW.index()] != null) {
         var1.nav[IsoDirections.SW.index()].nav[IsoDirections.NE.index()] = var1;
      }

      if (var1.nav[IsoDirections.S.index()] != null) {
         var1.nav[IsoDirections.S.index()].nav[IsoDirections.N.index()] = var1;
      }

      if (var1.nav[IsoDirections.SE.index()] != null) {
         var1.nav[IsoDirections.SE.index()].nav[IsoDirections.NW.index()] = var1;
      }

      if (var1.nav[IsoDirections.E.index()] != null) {
         var1.nav[IsoDirections.E.index()].nav[IsoDirections.W.index()] = var1;
      }

      if (var1.nav[IsoDirections.NE.index()] != null) {
         var1.nav[IsoDirections.NE.index()].nav[IsoDirections.SW.index()] = var1;
      }

   }

   public IsoGridSquare ConnectNewSquare(IsoGridSquare var1, boolean var2) {
      for(int var3 = 0; var3 < IsoPlayer.numPlayers; ++var3) {
         if (!this.ChunkMap[var3].ignore) {
            this.ChunkMap[var3].setGridSquare(var1, var1.getX(), var1.getY(), var1.getZ());
         }
      }

      IsoGridSquare var4 = this.ConnectNewSquare(var1, var2, false);
      return var4;
   }

   public void PlaceLot(String var1, int var2, int var3, int var4, boolean var5) {
   }

   public void PlaceLot(IsoLot var1, int var2, int var3, int var4, boolean var5) {
      int var6 = Math.min(var4 + var1.info.maxLevel - var1.info.minLevel + 1, 32);

      for(int var7 = var2; var7 < var2 + var1.info.width; ++var7) {
         for(int var8 = var3; var8 < var3 + var1.info.height; ++var8) {
            for(int var9 = var4; var9 < var6; ++var9) {
               int var10 = var7 - var2;
               int var11 = var8 - var3;
               int var12 = var9 - var4;
               if (var7 < this.width && var8 < this.height && var7 >= 0 && var8 >= 0 && var9 >= 0) {
                  int var13 = var10 + var11 * 8 + var12 * 64;
                  int var14 = var1.m_offsetInData[var13];
                  if (var14 != -1) {
                     int var15 = var1.m_data.getQuick(var14);
                     if (var15 > 0) {
                        boolean var16 = false;

                        for(int var17 = 0; var17 < var15; ++var17) {
                           String var18 = (String)var1.info.tilesUsed.get(var1.m_data.getQuick(var14 + 1 + var17));
                           IsoSprite var19 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var18);
                           if (var19 == null) {
                              Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, "Missing tile definition: " + var18);
                           } else {
                              IsoGridSquare var20 = this.getGridSquare(var7, var8, var9);
                              if (var20 == null) {
                                 if (IsoGridSquare.loadGridSquareCache != null) {
                                    var20 = IsoGridSquare.getNew(IsoGridSquare.loadGridSquareCache, this, (SliceY)null, var7, var8, var9);
                                 } else {
                                    var20 = IsoGridSquare.getNew(this, (SliceY)null, var7, var8, var9);
                                 }

                                 this.ChunkMap[IsoPlayer.getPlayerIndex()].setGridSquare(var20, var7, var8, var9);
                              } else {
                                 if (var5 && var17 == 0 && var19.getProperties().Is(IsoFlagType.solidfloor) && (!var19.Properties.Is(IsoFlagType.hidewalls) || var15 > 1)) {
                                    var16 = true;
                                 }

                                 if (var16 && var17 == 0) {
                                    var20.getObjects().clear();
                                 }
                              }

                              CellLoader.DoTileObjectCreation(var19, var19.getType(), var20, this, var7, var8, var9, var18);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

   }

   public int PlaceLot(IsoLot var1, int var2, int var3, int var4, IsoChunk var5, int var6, int var7, boolean[] var8) {
      var6 *= 8;
      var7 *= 8;
      IsoMetaGrid var9 = IsoWorld.instance.getMetaGrid();
      int var10 = Math.max(var4, -32);
      int var11 = Math.min(var4 + (var1.maxLevel - var1.minLevel) - 1, 31);
      int var12 = 0;

      int var14;
      int var15;
      int var16;
      try {
         for(int var13 = var6 + var2; var13 < var6 + var2 + 8; ++var13) {
            for(var14 = var7 + var3; var14 < var7 + var3 + 8; ++var14) {
               if (var13 >= var6 && var14 >= var7 && var13 < var6 + 8 && var14 < var7 + 8) {
                  if (var8[var13 - var6 + (var14 - var7) * 8]) {
                     ++var12;
                  } else {
                     for(var15 = var10; var15 <= var11; ++var15) {
                        var16 = var13 - var6 - var2;
                        int var17 = var14 - var7 - var3;
                        int var18 = var15 - var1.info.minLevel;
                        int var19 = var16 + var17 * 8 + var18 * 64;
                        int var20 = var1.m_offsetInData[var19];
                        if (var20 != -1) {
                           int var21 = var1.m_data.getQuick(var20);
                           if (var21 > 0) {
                              if (!var8[var13 - var6 + (var14 - var7) * 8]) {
                                 var8[var13 - var6 + (var14 - var7) * 8] = true;
                                 ++var12;
                              }

                              IsoGridSquare var22 = var5.getGridSquare(var13 - var6, var14 - var7, var15);
                              if (var22 == null) {
                                 if (IsoGridSquare.loadGridSquareCache != null) {
                                    var22 = IsoGridSquare.getNew(IsoGridSquare.loadGridSquareCache, this, (SliceY)null, var13, var14, var15);
                                 } else {
                                    var22 = IsoGridSquare.getNew(this, (SliceY)null, var13, var14, var15);
                                 }

                                 var22.setX(var13);
                                 var22.setY(var14);
                                 var22.setZ(var15);
                                 var5.setSquare(var13 - var6, var14 - var7, var15, var22);
                              }

                              for(int var23 = -1; var23 <= 1; ++var23) {
                                 for(int var24 = -1; var24 <= 1; ++var24) {
                                    if ((var23 != 0 || var24 != 0) && var23 + var13 - var6 >= 0 && var23 + var13 - var6 < 8 && var24 + var14 - var7 >= 0 && var24 + var14 - var7 < 8) {
                                       IsoGridSquare var25 = var5.getGridSquare(var13 + var23 - var6, var14 + var24 - var7, var15);
                                       if (var25 == null) {
                                          var25 = IsoGridSquare.getNew(this, (SliceY)null, var13 + var23, var14 + var24, var15);
                                          var5.setSquare(var13 + var23 - var6, var14 + var24 - var7, var15, var25);
                                       }
                                    }
                                 }
                              }

                              RoomDef var31 = var9.getRoomAt(var13, var14, var15);
                              long var32 = var31 != null ? var31.ID : -1L;
                              var22.setRoomID(var32);
                              var22.ResetIsoWorldRegion();
                              var31 = var9.getEmptyOutsideAt(var13, var14, var15);
                              if (var31 != null) {
                                 IsoRoom var26 = var5.getRoom(var31.ID);
                                 var22.roofHideBuilding = var26 == null ? null : var26.building;
                              }

                              boolean var33 = true;

                              for(int var27 = 0; var27 < var21; ++var27) {
                                 String var28 = (String)var1.info.tilesUsed.get(var1.m_data.get(var20 + 1 + var27));
                                 if (!var1.info.bFixed2x) {
                                    var28 = IsoChunk.Fix2x(var28);
                                 }

                                 IsoSprite var29 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var28);
                                 if (var29 == null) {
                                    Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, "Missing tile definition: " + var28);
                                 } else {
                                    if (var27 == 0 && var29.getProperties().Is(IsoFlagType.solidfloor) && (!var29.Properties.Is(IsoFlagType.hidewalls) || var21 > 1)) {
                                       var33 = true;
                                    }

                                    if (var33 && var27 == 0) {
                                       var22.getObjects().clear();
                                    }

                                    CellLoader.DoTileObjectCreation(var29, var29.getType(), var22, this, var13, var14, var15, var28);
                                 }
                              }

                              var22.FixStackableObjects();
                           }
                        }
                     }
                  }
               }
            }
         }
      } catch (Exception var30) {
         DebugLog.log("Failed to load chunk, blocking out area");
         ExceptionLogger.logException(var30);

         for(var14 = var6 + var2; var14 < var6 + var2 + 8; ++var14) {
            for(var15 = var7 + var3; var15 < var7 + var3 + 8; ++var15) {
               for(var16 = var10; var16 <= var11; ++var16) {
                  var5.setSquare(var14 - var6 - var2, var15 - var7 - var3, var16 - var4, (IsoGridSquare)null);
                  this.setCacheGridSquare(var14, var15, var16, (IsoGridSquare)null);
               }
            }
         }
      }

      return var12;
   }

   public void setDrag(KahluaTable var1, int var2) {
      LuaEventManager.triggerEvent("SetDragItem", var1, var2);
      if (var2 >= 0 && var2 < 4) {
         if (this.drag[var2] != null && this.drag[var2] != var1) {
            Object var3 = this.drag[var2].rawget("deactivate");
            if (var3 instanceof JavaFunction || var3 instanceof LuaClosure) {
               LuaManager.caller.pcallvoid(LuaManager.thread, var3, this.drag[var2]);
            }
         }

         this.drag[var2] = var1;
      }
   }

   public KahluaTable getDrag(int var1) {
      return var1 >= 0 && var1 < 4 ? this.drag[var1] : null;
   }

   public boolean DoBuilding(int var1, boolean var2) {
      boolean var3;
      try {
         IsoCell.s_performance.isoCellDoBuilding.start();
         var3 = this.doBuildingInternal(var1, var2);
      } finally {
         IsoCell.s_performance.isoCellDoBuilding.end();
      }

      return var3;
   }

   private boolean doBuildingInternal(int var1, boolean var2) {
      if (UIManager.getPickedTile() != null && this.drag[var1] != null && JoypadManager.instance.getFromPlayer(var1) == null) {
         int var3 = PZMath.fastfloor(UIManager.getPickedTile().x);
         int var4 = PZMath.fastfloor(UIManager.getPickedTile().y);
         int var5 = PZMath.fastfloor(IsoCamera.getCameraCharacterZ());
         if (!IsoWorld.instance.isValidSquare(var3, var4, var5)) {
            return false;
         }

         IsoGridSquare var6 = this.getGridSquare(var3, var4, var5);
         if (!var2) {
            if (var6 == null) {
               var6 = this.createNewGridSquare(var3, var4, var5, true);
               if (var6 == null) {
                  return false;
               }
            }

            var6.EnsureSurroundNotNull();
         }

         LuaEventManager.triggerEvent("OnDoTileBuilding2", this.drag[var1], var2, var3, var4, var5, var6);
      }

      if (this.drag[var1] != null && JoypadManager.instance.getFromPlayer(var1) != null) {
         LuaEventManager.triggerEvent("OnDoTileBuilding3", this.drag[var1], var2, PZMath.fastfloor(IsoPlayer.players[var1].getX()), PZMath.fastfloor(IsoPlayer.players[var1].getY()), PZMath.fastfloor(IsoCamera.getCameraCharacterZ()));
      }

      if (var2) {
         if (PerformanceSettings.FBORenderChunk) {
            IndieGL.glBlendFuncSeparate(770, 771, 770, 1);
         } else {
            IndieGL.glBlendFunc(770, 771);
         }
      }

      return false;
   }

   public float DistanceFromSupport(int var1, int var2, int var3) {
      return 0.0F;
   }

   public ArrayList<IsoBuilding> getBuildingList() {
      return this.BuildingList;
   }

   public ArrayList<IsoWindow> getWindowList() {
      return this.WindowList;
   }

   public void addToWindowList(IsoWindow var1) {
      if (!GameServer.bServer) {
         if (var1 != null) {
            if (!this.WindowList.contains(var1)) {
               this.WindowList.add(var1);
            }

         }
      }
   }

   public void removeFromWindowList(IsoWindow var1) {
      this.WindowList.remove(var1);
   }

   public ArrayList<IsoMovingObject> getObjectList() {
      return this.ObjectList;
   }

   public IsoRoom getRoom(int var1) {
      IsoRoom var2 = this.ChunkMap[IsoPlayer.getPlayerIndex()].getRoom(var1);
      return var2;
   }

   public ArrayList<IsoPushableObject> getPushableObjectList() {
      return this.PushableObjectList;
   }

   public HashMap<Integer, BuildingScore> getBuildingScores() {
      return this.BuildingScores;
   }

   public ArrayList<IsoRoom> getRoomList() {
      return this.RoomList;
   }

   public ArrayList<IsoObject> getStaticUpdaterObjectList() {
      return this.StaticUpdaterObjectList;
   }

   public ArrayList<IsoZombie> getZombieList() {
      return this.ZombieList;
   }

   public ArrayList<IsoGameCharacter> getRemoteSurvivorList() {
      return this.RemoteSurvivorList;
   }

   public ArrayList<IsoMovingObject> getRemoveList() {
      return this.removeList;
   }

   public ArrayList<IsoMovingObject> getAddList() {
      return this.addList;
   }

   public void addMovingObject(IsoMovingObject var1) {
      this.addList.add(var1);
   }

   public ArrayList<InventoryItem> getProcessItems() {
      return this.ProcessItems;
   }

   public ArrayList<IsoWorldInventoryObject> getProcessWorldItems() {
      return this.ProcessWorldItems;
   }

   public ArrayList<IsoObject> getProcessIsoObjects() {
      return this.ProcessIsoObject;
   }

   public ArrayList<InventoryItem> getProcessItemsRemove() {
      return this.ProcessItemsRemove;
   }

   public ArrayList<BaseVehicle> getVehicles() {
      return this.vehicles;
   }

   public int getHeight() {
      return this.height;
   }

   public void setHeight(int var1) {
      this.height = var1;
   }

   public int getWidth() {
      return this.width;
   }

   public void setWidth(int var1) {
      this.width = var1;
   }

   public int getWorldX() {
      return this.worldX;
   }

   public void setWorldX(int var1) {
      this.worldX = var1;
   }

   public int getWorldY() {
      return this.worldY;
   }

   public void setWorldY(int var1) {
      this.worldY = var1;
   }

   public boolean isSafeToAdd() {
      return this.safeToAdd;
   }

   public void setSafeToAdd(boolean var1) {
      this.safeToAdd = var1;
   }

   public Stack<IsoLightSource> getLamppostPositions() {
      return this.LamppostPositions;
   }

   public IsoLightSource getLightSourceAt(int var1, int var2, int var3) {
      for(int var4 = 0; var4 < this.LamppostPositions.size(); ++var4) {
         IsoLightSource var5 = (IsoLightSource)this.LamppostPositions.get(var4);
         if (var5.getX() == var1 && var5.getY() == var2 && var5.getZ() == var3) {
            return var5;
         }
      }

      return null;
   }

   public void addLamppost(IsoLightSource var1) {
      if (var1 != null && !this.LamppostPositions.contains(var1)) {
         this.LamppostPositions.add(var1);
         IsoGridSquare.RecalcLightTime = -1.0F;
         if (PerformanceSettings.FBORenderChunk) {
            ++Core.dirtyGlobalLightsCount;
         }

         GameTime.instance.lightSourceUpdate = 100.0F;
      }
   }

   public IsoLightSource addLamppost(int var1, int var2, int var3, float var4, float var5, float var6, int var7) {
      IsoLightSource var8 = new IsoLightSource(var1, var2, var3, var4, var5, var6, var7);
      this.LamppostPositions.add(var8);
      IsoGridSquare.RecalcLightTime = -1.0F;
      if (PerformanceSettings.FBORenderChunk) {
         ++Core.dirtyGlobalLightsCount;
      }

      GameTime.instance.lightSourceUpdate = 100.0F;
      return var8;
   }

   public void removeLamppost(int var1, int var2, int var3) {
      for(int var4 = 0; var4 < this.LamppostPositions.size(); ++var4) {
         IsoLightSource var5 = (IsoLightSource)this.LamppostPositions.get(var4);
         if (var5.getX() == var1 && var5.getY() == var2 && var5.getZ() == var3) {
            var5.clearInfluence();
            this.LamppostPositions.remove(var5);
            IsoGridSquare.RecalcLightTime = -1.0F;
            if (PerformanceSettings.FBORenderChunk) {
               ++Core.dirtyGlobalLightsCount;
            }

            GameTime.instance.lightSourceUpdate = 100.0F;
            return;
         }
      }

   }

   public void removeLamppost(IsoLightSource var1) {
      var1.life = 0;
      IsoGridSquare.RecalcLightTime = -1.0F;
      if (PerformanceSettings.FBORenderChunk) {
         ++Core.dirtyGlobalLightsCount;
      }

      GameTime.instance.lightSourceUpdate = 100.0F;
   }

   public int getCurrentLightX() {
      return this.currentLX;
   }

   public void setCurrentLightX(int var1) {
      this.currentLX = var1;
   }

   public int getCurrentLightY() {
      return this.currentLY;
   }

   public void setCurrentLightY(int var1) {
      this.currentLY = var1;
   }

   public int getCurrentLightZ() {
      return this.currentLZ;
   }

   public void setCurrentLightZ(int var1) {
      this.currentLZ = var1;
   }

   public int getMinX() {
      return this.minX;
   }

   public void setMinX(int var1) {
      this.minX = var1;
   }

   public int getMaxX() {
      return this.maxX;
   }

   public void setMaxX(int var1) {
      this.maxX = var1;
   }

   public int getMinY() {
      return this.minY;
   }

   public void setMinY(int var1) {
      this.minY = var1;
   }

   public int getMaxY() {
      return this.maxY;
   }

   public void setMaxY(int var1) {
      this.maxY = var1;
   }

   public int getMinZ() {
      return this.minZ;
   }

   public void setMinZ(int var1) {
      this.minZ = var1;
   }

   public int getMaxZ() {
      return this.maxZ;
   }

   public void setMaxZ(int var1) {
      this.maxZ = var1;
   }

   public OnceEvery getDangerUpdate() {
      return this.dangerUpdate;
   }

   public void setDangerUpdate(OnceEvery var1) {
      this.dangerUpdate = var1;
   }

   public Thread getLightInfoUpdate() {
      return this.LightInfoUpdate;
   }

   public void setLightInfoUpdate(Thread var1) {
      this.LightInfoUpdate = var1;
   }

   public ArrayList<IsoSurvivor> getSurvivorList() {
      return this.SurvivorList;
   }

   public static int getRComponent(int var0) {
      return var0 & 255;
   }

   public static int getGComponent(int var0) {
      return (var0 & '\uff00') >> 8;
   }

   public static int getBComponent(int var0) {
      return (var0 & 16711680) >> 16;
   }

   public static int toIntColor(float var0, float var1, float var2, float var3) {
      return (int)(var0 * 255.0F) << 0 | (int)(var1 * 255.0F) << 8 | (int)(var2 * 255.0F) << 16 | (int)(var3 * 255.0F) << 24;
   }

   public IsoGridSquare getRandomOutdoorTile() {
      IsoGridSquare var1 = null;

      do {
         var1 = this.getGridSquare(this.ChunkMap[IsoPlayer.getPlayerIndex()].getWorldXMin() * 8 + Rand.Next(this.width), this.ChunkMap[IsoPlayer.getPlayerIndex()].getWorldYMin() * 8 + Rand.Next(this.height), 0);
         if (var1 != null) {
            var1.setCachedIsFree(false);
         }
      } while(var1 == null || !var1.isFree(false) || var1.getRoom() != null);

      return var1;
   }

   private static void InsertAt(int var0, BuildingScore var1, BuildingScore[] var2) {
      for(int var3 = var2.length - 1; var3 > var0; --var3) {
         var2[var3] = var2[var3 - 1];
      }

      var2[var0] = var1;
   }

   static void Place(BuildingScore var0, BuildingScore[] var1, BuildingSearchCriteria var2) {
      for(int var3 = 0; var3 < var1.length; ++var3) {
         if (var1[var3] != null) {
            boolean var4 = false;
            if (var1[var3] == null) {
               var4 = true;
            } else {
               switch (var2) {
                  case General:
                     if (var1[var3].food + var1[var3].defense + (float)var1[var3].size + var1[var3].weapons < var0.food + var0.defense + (float)var0.size + var0.weapons) {
                        var4 = true;
                     }
                     break;
                  case Food:
                     if (var1[var3].food < var0.food) {
                        var4 = true;
                     }
                     break;
                  case Wood:
                     if (var1[var3].wood < var0.wood) {
                        var4 = true;
                     }
                     break;
                  case Weapons:
                     if (var1[var3].weapons < var0.weapons) {
                        var4 = true;
                     }
                     break;
                  case Defense:
                     if (var1[var3].defense < var0.defense) {
                        var4 = true;
                     }
               }
            }

            if (var4) {
               InsertAt(var3, var0, var1);
               return;
            }
         }
      }

   }

   public Stack<BuildingScore> getBestBuildings(BuildingSearchCriteria var1, int var2) {
      BuildingScore[] var3 = new BuildingScore[var2];
      int var4;
      int var5;
      if (this.BuildingScores.isEmpty()) {
         var4 = this.BuildingList.size();

         for(var5 = 0; var5 < var4; ++var5) {
            ((IsoBuilding)this.BuildingList.get(var5)).update();
         }
      }

      var4 = this.BuildingScores.size();

      for(var5 = 0; var5 < var4; ++var5) {
         BuildingScore var6 = (BuildingScore)this.BuildingScores.get(var5);
         Place(var6, var3, var1);
      }

      buildingscores.clear();
      buildingscores.addAll(Arrays.asList(var3));
      return buildingscores;
   }

   public boolean blocked(Mover var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      IsoGridSquare var8 = this.getGridSquare(var5, var6, var7);
      if (var8 == null) {
         return true;
      } else {
         if (var1 instanceof IsoMovingObject) {
            if (var8.testPathFindAdjacent((IsoMovingObject)var1, var2 - var5, var3 - var6, var4 - var7)) {
               return true;
            }
         } else if (var8.testPathFindAdjacent((IsoMovingObject)null, var2 - var5, var3 - var6, var4 - var7)) {
            return true;
         }

         return false;
      }
   }

   public void Dispose() {
      int var1;
      for(var1 = 0; var1 < this.ObjectList.size(); ++var1) {
         IsoMovingObject var2 = (IsoMovingObject)this.ObjectList.get(var1);
         if (var2 instanceof IsoZombie) {
            var2.setCurrent((IsoGridSquare)null);
            var2.setLast((IsoGridSquare)null);
            VirtualZombieManager.instance.addToReusable((IsoZombie)var2);
         }
      }

      for(var1 = 0; var1 < this.RoomList.size(); ++var1) {
         ((IsoRoom)this.RoomList.get(var1)).TileList.clear();
         ((IsoRoom)this.RoomList.get(var1)).Exits.clear();
         ((IsoRoom)this.RoomList.get(var1)).WaterSources.clear();
         ((IsoRoom)this.RoomList.get(var1)).lightSwitches.clear();
         ((IsoRoom)this.RoomList.get(var1)).Beds.clear();
      }

      for(var1 = 0; var1 < this.BuildingList.size(); ++var1) {
         ((IsoBuilding)this.BuildingList.get(var1)).Exits.clear();
         ((IsoBuilding)this.BuildingList.get(var1)).Rooms.clear();
         ((IsoBuilding)this.BuildingList.get(var1)).container.clear();
         ((IsoBuilding)this.BuildingList.get(var1)).Windows.clear();
      }

      LuaEventManager.clear();
      LuaHookManager.clear();
      this.LamppostPositions.clear();
      this.ProcessItems.clear();
      this.ProcessItemsRemove.clear();
      this.ProcessWorldItems.clear();
      this.ProcessWorldItemsRemove.clear();
      this.BuildingScores.clear();
      this.BuildingList.clear();
      this.WindowList.clear();
      this.PushableObjectList.clear();
      this.RoomList.clear();
      this.SurvivorList.clear();
      this.ObjectList.clear();
      this.ZombieList.clear();

      for(var1 = 0; var1 < this.ChunkMap.length; ++var1) {
         this.ChunkMap[var1].Dispose();
         this.ChunkMap[var1] = null;
      }

      for(var1 = 0; var1 < this.gridSquares.length; ++var1) {
         if (this.gridSquares[var1] != null) {
            Arrays.fill(this.gridSquares[var1], (Object)null);
            this.gridSquares[var1] = null;
         }
      }

      if (this.weatherFX != null) {
         this.weatherFX.Reset();
         this.weatherFX = null;
      }

   }

   @LuaMethod(
      name = "getGridSquare"
   )
   public IsoGridSquare getGridSquare(double var1, double var3, double var5) {
      var1 = (double)PZMath.fastfloor(var1);
      var3 = (double)PZMath.fastfloor(var3);
      var5 = (double)PZMath.fastfloor(var5);
      return GameServer.bServer ? ServerMap.instance.getGridSquare((int)var1, (int)var3, (int)var5) : this.getGridSquare((int)var1, (int)var3, (int)var5);
   }

   @LuaMethod(
      name = "getOrCreateGridSquare"
   )
   public IsoGridSquare getOrCreateGridSquare(double var1, double var3, double var5) {
      var1 = (double)PZMath.fastfloor(var1);
      var3 = (double)PZMath.fastfloor(var3);
      var5 = (double)PZMath.fastfloor(var5);
      IsoGridSquare var7;
      if (GameServer.bServer) {
         var7 = ServerMap.instance.getGridSquare((int)var1, (int)var3, (int)var5);
         if (var7 == null) {
            var7 = IsoGridSquare.getNew(this, (SliceY)null, (int)var1, (int)var3, (int)var5);
            ServerMap.instance.setGridSquare((int)var1, (int)var3, (int)var5, var7);
            this.ConnectNewSquare(var7, true);
         }

         return var7;
      } else {
         var7 = this.getGridSquare((int)var1, (int)var3, (int)var5);
         if (var7 == null) {
            var7 = IsoGridSquare.getNew(this, (SliceY)null, (int)var1, (int)var3, (int)var5);
            this.ConnectNewSquare(var7, true);
         }

         return var7;
      }
   }

   public void setCacheGridSquare(int var1, int var2, int var3, IsoGridSquare var4) {
      assert var4 == null || var1 == var4.getX() && var2 == var4.getY() && var3 == var4.getZ();
   }

   public void setCacheChunk(IsoChunk var1) {
      if (!GameServer.bServer) {
         ;
      }
   }

   public void setCacheChunk(IsoChunk var1, int var2) {
      if (!GameServer.bServer) {
         ;
      }
   }

   public void clearCacheGridSquare(int var1) {
   }

   public void setCacheGridSquareLocal(int var1, int var2, int var3, IsoGridSquare var4, int var5) {
   }

   public IsoGridSquare getGridSquare(Double var1, Double var2, Double var3) {
      return this.getGridSquare(PZMath.fastfloor(var1), PZMath.fastfloor(var2), PZMath.fastfloor(var3));
   }

   public IsoGridSquare getGridSquare(int var1, int var2, int var3) {
      if (GameServer.bServer) {
         return ServerMap.instance.getGridSquare(var1, var2, var3);
      } else {
         int var4 = IsoChunkMap.ChunkWidthInTiles;

         for(int var5 = 0; var5 < IsoPlayer.numPlayers; ++var5) {
            IsoChunkMap var6 = this.ChunkMap[var5];
            if (!var6.ignore) {
               if (var3 == 0) {
                  boolean var7 = false;
               }

               int var11 = var1 - var6.getWorldXMinTiles();
               int var8 = var2 - var6.getWorldYMinTiles();
               int var9 = var3 + 32;
               if (var3 <= 31 && var3 >= -32 && var11 >= 0 && var11 < var4 && var8 >= 0 && var8 < var4) {
                  IsoGridSquare var10 = var6.getGridSquareDirect(var11, var8, var3);
                  if (var10 != null) {
                     return var10;
                  }
               }
            }
         }

         return null;
      }
   }

   public void EnsureSurroundNotNull(int var1, int var2, int var3) {
      for(int var4 = -1; var4 <= 1; ++var4) {
         for(int var5 = -1; var5 <= 1; ++var5) {
            this.createNewGridSquare(var1 + var4, var2 + var5, var3, false);
         }
      }

   }

   public void DeleteAllMovingObjects() {
      this.ObjectList.clear();
   }

   @LuaMethod(
      name = "getMaxFloors"
   )
   public int getMaxFloors() {
      return 32;
   }

   public KahluaTable getLuaObjectList() {
      KahluaTable var1 = LuaManager.platform.newTable();
      LuaManager.env.rawset("Objects", var1);

      for(int var2 = 0; var2 < this.ObjectList.size(); ++var2) {
         var1.rawset(var2 + 1, this.ObjectList.get(var2));
      }

      return var1;
   }

   public int getHeightInTiles() {
      return this.ChunkMap[IsoPlayer.getPlayerIndex()].getWidthInTiles();
   }

   public int getWidthInTiles() {
      return this.ChunkMap[IsoPlayer.getPlayerIndex()].getWidthInTiles();
   }

   public boolean isNull(int var1, int var2, int var3) {
      IsoGridSquare var4 = this.getGridSquare(var1, var2, var3);
      return var4 == null || !var4.isFree(false);
   }

   public void Remove(IsoMovingObject var1) {
      if (!(var1 instanceof IsoPlayer) || ((IsoPlayer)var1).isDead()) {
         this.removeList.add(var1);
      }
   }

   boolean isBlocked(IsoGridSquare var1, IsoGridSquare var2) {
      return var1.room != var2.room;
   }

   private int CalculateColor(IsoGridSquare var1, IsoGridSquare var2, IsoGridSquare var3, IsoGridSquare var4, int var5, int var6) {
      float var7 = 0.0F;
      float var8 = 0.0F;
      float var9 = 0.0F;
      float var10 = 1.0F;
      if (var4 == null) {
         return 0;
      } else {
         float var11 = 0.0F;
         boolean var12 = true;
         ColorInfo var13;
         if (var1 != null && var4.room == var1.room && var1.getChunk() != null) {
            ++var11;
            var13 = var1.lighting[var6].lightInfo();
            var7 += var13.r;
            var8 += var13.g;
            var9 += var13.b;
         }

         if (var2 != null && var4.room == var2.room && var2.getChunk() != null) {
            ++var11;
            var13 = var2.lighting[var6].lightInfo();
            var7 += var13.r;
            var8 += var13.g;
            var9 += var13.b;
         }

         if (var3 != null && var4.room == var3.room && var3.getChunk() != null) {
            ++var11;
            var13 = var3.lighting[var6].lightInfo();
            var7 += var13.r;
            var8 += var13.g;
            var9 += var13.b;
         }

         if (var4 != null) {
            ++var11;
            var13 = var4.lighting[var6].lightInfo();
            var7 += var13.r;
            var8 += var13.g;
            var9 += var13.b;
         }

         if (var11 != 0.0F) {
            var7 /= var11;
            var8 /= var11;
            var9 /= var11;
         }

         if (var7 > 1.0F) {
            var7 = 1.0F;
         }

         if (var8 > 1.0F) {
            var8 = 1.0F;
         }

         if (var9 > 1.0F) {
            var9 = 1.0F;
         }

         if (var7 < 0.0F) {
            var7 = 0.0F;
         }

         if (var8 < 0.0F) {
            var8 = 0.0F;
         }

         if (var9 < 0.0F) {
            var9 = 0.0F;
         }

         if (var4 != null) {
            var4.setVertLight(var5, (int)(var7 * 255.0F) << 0 | (int)(var8 * 255.0F) << 8 | (int)(var9 * 255.0F) << 16 | -16777216, var6);
            var4.setVertLight(var5 + 4, (int)(var7 * 255.0F) << 0 | (int)(var8 * 255.0F) << 8 | (int)(var9 * 255.0F) << 16 | -16777216, var6);
         }

         return var5;
      }
   }

   public static IsoCell getInstance() {
      return instance;
   }

   public void render() {
      if (IsoCamera.frameState.playerIndex == 0 && GameKeyboard.isKeyPressed(199)) {
         PerformanceSettings.FBORenderChunk = !PerformanceSettings.FBORenderChunk;
      }

      if (PerformanceSettings.FBORenderChunk) {
         FBORenderCell.instance.cell = this;
         FBORenderCutaways.getInstance().cell = this;
         IsoCell.s_performance.isoCellRender.invokeAndMeasure(FBORenderCell.instance, FBORenderCell::renderInternal);
      } else {
         IsoCell.s_performance.isoCellRender.invokeAndMeasure(this, IsoCell::renderInternal);
      }
   }

   private void renderInternal() {
      int var1 = IsoCamera.frameState.playerIndex;
      IsoPlayer var2 = IsoPlayer.players[var1];
      if (var2.dirtyRecalcGridStackTime > 0.0F) {
         var2.dirtyRecalcGridStack = true;
      } else {
         var2.dirtyRecalcGridStack = false;
      }

      if (!PerformanceSettings.NewRoofHiding) {
         if (this.bHideFloors[var1] && this.unhideFloorsCounter[var1] > 0) {
            int var10002 = this.unhideFloorsCounter[var1]--;
         }

         if (this.unhideFloorsCounter[var1] <= 0) {
            this.bHideFloors[var1] = false;
            this.unhideFloorsCounter[var1] = 60;
         }
      }

      int var3 = 32;
      if (var3 < 32) {
         ++var3;
      }

      --this.recalcShading;
      byte var4 = 0;
      byte var5 = 0;
      int var6 = var4 + IsoCamera.getOffscreenWidth(var1);
      int var7 = var5 + IsoCamera.getOffscreenHeight(var1);
      float var8 = IsoUtils.XToIso((float)var4, (float)var5, 0.0F);
      float var9 = IsoUtils.YToIso((float)var6, (float)var5, 0.0F);
      float var10 = IsoUtils.XToIso((float)var6, (float)var7, 6.0F);
      float var11 = IsoUtils.YToIso((float)var4, (float)var7, 6.0F);
      this.minY = (int)var9;
      this.maxY = (int)var11;
      this.minX = (int)var8;
      this.maxX = (int)var10;
      this.minX -= 2;
      this.minY -= 2;
      this.maxZ = MaxHeight;
      IsoGameCharacter var12 = IsoCamera.getCameraCharacter();
      if (var12 == null) {
         this.maxZ = 1;
      }

      boolean var13 = false;
      var13 = true;
      if (GameTime.instance.FPSMultiplier > 1.5F) {
         var13 = true;
      }

      if (this.minX != this.lastMinX || this.minY != this.lastMinY) {
         this.lightUpdateCount = 10;
      }

      if (!PerformanceSettings.NewRoofHiding) {
         IsoGridSquare var14 = var12 == null ? null : var12.getCurrentSquare();
         if (var14 != null) {
            IsoGridSquare var15 = this.getGridSquare((double)Math.round(var12.getX()), (double)Math.round(var12.getY()), (double)var12.getZ());
            if (var15 != null && this.IsBehindStuff(var15)) {
               this.bHideFloors[var1] = true;
            }

            if (!this.bHideFloors[var1] && var14.getProperties().Is(IsoFlagType.hidewalls) || !var14.getProperties().Is(IsoFlagType.exterior)) {
               this.bHideFloors[var1] = true;
            }
         }

         if (this.bHideFloors[var1]) {
            this.maxZ = (int)var12.getZ() + 1;
         }
      }

      if (PerformanceSettings.LightingFrameSkip < 3) {
         this.DrawStencilMask();
      }

      int var19;
      if (PerformanceSettings.LightingFrameSkip == 3) {
         var19 = IsoCamera.getOffscreenWidth(var1) / 2;
         int var20 = IsoCamera.getOffscreenHeight(var1) / 2;
         short var16 = 409;
         var19 -= var16 / (2 / Core.TileScale);
         var20 -= var16 / (2 / Core.TileScale);
         this.StencilX1 = var19 - (int)IsoCamera.cameras[var1].RightClickX;
         this.StencilY1 = var20 - (int)IsoCamera.cameras[var1].RightClickY;
         this.StencilX2 = this.StencilX1 + var16 * Core.TileScale;
         this.StencilY2 = this.StencilY1 + var16 * Core.TileScale;
      }

      if (PerformanceSettings.NewRoofHiding && var2.dirtyRecalcGridStack) {
         this.hidesOrphanStructuresAbove = var3;
         ((ArrayList)this.otherOccluderBuildings.get(var1)).clear();
         if (this.otherOccluderBuildingsArr[var1] != null) {
            this.otherOccluderBuildingsArr[var1][0] = null;
         } else {
            this.otherOccluderBuildingsArr[var1] = new IsoBuilding[500];
         }

         if (var12 != null && var12.getCurrentSquare() != null) {
            GameProfiler.getInstance().invokeAndMeasure("updateZombies", var12, var1, this::updateZombiesForRender);
         } else {
            for(var19 = 0; var19 < 4; ++var19) {
               ((ArrayList)this.playerOccluderBuildings.get(var19)).clear();
               if (this.playerOccluderBuildingsArr[var19] != null) {
                  this.playerOccluderBuildingsArr[var19][0] = null;
               } else {
                  this.playerOccluderBuildingsArr[var19] = new IsoBuilding[500];
               }

               this.lastPlayerSquare[var19] = null;
               this.playerCutawaysDirty[var19] = true;
            }

            this.playerWindowPeekingRoomId[var1] = -1L;
            ((ArrayList)this.zombieOccluderBuildings.get(var1)).clear();
            if (this.zombieOccluderBuildingsArr[var1] != null) {
               this.zombieOccluderBuildingsArr[var1][0] = null;
            } else {
               this.zombieOccluderBuildingsArr[var1] = new IsoBuilding[500];
            }

            this.lastZombieSquare[var1] = null;
         }
      }

      if (!PerformanceSettings.NewRoofHiding) {
         for(var19 = 0; var19 < IsoPlayer.numPlayers; ++var19) {
            this.playerWindowPeekingRoomId[var19] = -1L;
            IsoPlayer var21 = IsoPlayer.players[var19];
            if (var21 != null) {
               IsoBuilding var22 = var21.getCurrentBuilding();
               if (var22 == null) {
                  IsoDirections var17 = IsoDirections.fromAngle(var21.getForwardDirection());
                  var22 = this.GetPeekedInBuilding(var21.getCurrentSquare(), var17);
                  if (var22 != null) {
                     this.playerWindowPeekingRoomId[var19] = this.playerPeekedRoomId;
                  }
               }
            }
         }
      }

      if (var12 != null && var12.getCurrentSquare() != null && var12.getCurrentSquare().getProperties().Is(IsoFlagType.hidewalls)) {
         this.maxZ = (int)var12.getZ() + 1;
      }

      this.bRendering = true;

      try {
         this.RenderTiles(var3);
      } catch (Exception var18) {
         this.bRendering = false;
         Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, (String)null, var18);
      }

      this.bRendering = false;
      if (IsoGridSquare.getRecalcLightTime() < 0.0F) {
         IsoGridSquare.setRecalcLightTime(60.0F);
      }

      if (IsoGridSquare.getLightcache() <= 0) {
         IsoGridSquare.setLightcache(90);
      }

      GameProfiler.getInstance().invokeAndMeasure("renderLast", this::renderLast);
      IsoTree.renderChopTreeIndicators();
      if (Core.bDebug) {
      }

      this.lastMinX = this.minX;
      this.lastMinY = this.minY;
      GameProfiler.getInstance().invokeAndMeasure("DoBuilding", IsoPlayer.getPlayerIndex(), true, this::DoBuilding);
      GameProfiler.getInstance().invokeAndMeasure("renderRain", this::renderRain);
   }

   private void renderLast() {
      int var1;
      for(var1 = 0; var1 < this.ObjectList.size(); ++var1) {
         IsoMovingObject var2 = (IsoMovingObject)this.ObjectList.get(var1);
         var2.renderlast();
      }

      for(var1 = 0; var1 < this.StaticUpdaterObjectList.size(); ++var1) {
         IsoObject var3 = (IsoObject)this.StaticUpdaterObjectList.get(var1);
         var3.renderlast();
      }

   }

   private void updateZombiesForRender(IsoGameCharacter var1, int var2) {
      IsoGridSquare var3 = var1.getCurrentSquare();
      IsoGridSquare var4 = null;
      int var5 = 10;
      if (this.ZombieList.size() < 10) {
         var5 = this.ZombieList.size();
      }

      if (this.nearestVisibleZombie[var2] != null) {
         if (this.nearestVisibleZombie[var2].isDead()) {
            this.nearestVisibleZombie[var2] = null;
         } else {
            float var6 = this.nearestVisibleZombie[var2].getX() - var1.getX();
            float var7 = this.nearestVisibleZombie[var2].getY() - var1.getY();
            this.nearestVisibleZombieDistSqr[var2] = var6 * var6 + var7 * var7;
         }
      }

      IsoGridSquare var8;
      int var19;
      for(var19 = 0; var19 < var5; ++this.zombieScanCursor) {
         if (this.zombieScanCursor >= this.ZombieList.size()) {
            this.zombieScanCursor = 0;
         }

         IsoZombie var20 = (IsoZombie)this.ZombieList.get(this.zombieScanCursor);
         if (var20 != null) {
            var8 = var20.getCurrentSquare();
            if (var8 != null && var3.z == var8.z && var8.getCanSee(var2)) {
               float var9;
               float var10;
               if (this.nearestVisibleZombie[var2] == null) {
                  this.nearestVisibleZombie[var2] = var20;
                  var9 = this.nearestVisibleZombie[var2].getX() - var1.getX();
                  var10 = this.nearestVisibleZombie[var2].getY() - var1.getY();
                  this.nearestVisibleZombieDistSqr[var2] = var9 * var9 + var10 * var10;
               } else {
                  var9 = var20.getX() - var1.getX();
                  var10 = var20.getY() - var1.getY();
                  float var11 = var9 * var9 + var10 * var10;
                  if (var11 < this.nearestVisibleZombieDistSqr[var2]) {
                     this.nearestVisibleZombie[var2] = var20;
                     this.nearestVisibleZombieDistSqr[var2] = var11;
                  }
               }
            }
         }

         ++var19;
      }

      IsoBuilding var15;
      for(var19 = 0; var19 < 4; ++var19) {
         IsoPlayer var21 = IsoPlayer.players[var19];
         if (var21 != null && var21.getCurrentSquare() != null) {
            var8 = var21.getCurrentSquare();
            if (var19 == var2) {
               var4 = var8;
            }

            double var25 = (double)(var21.getX() - (float)PZMath.fastfloor(var21.getX()));
            double var27 = (double)(var21.getY() - (float)PZMath.fastfloor(var21.getY()));
            boolean var13 = var25 > var27;
            if (this.lastPlayerAngle[var19] == null) {
               this.lastPlayerAngle[var19] = new Vector2(var21.getForwardDirection());
               this.playerCutawaysDirty[var19] = true;
            } else if (var21.getForwardDirection().dot(this.lastPlayerAngle[var19]) < 0.98F) {
               this.lastPlayerAngle[var19].set(var21.getForwardDirection());
               this.playerCutawaysDirty[var19] = true;
            }

            IsoDirections var14 = IsoDirections.fromAngle(var21.getForwardDirection());
            if (this.lastPlayerSquare[var19] != var8 || this.lastPlayerSquareHalf[var19] != var13 || this.lastPlayerDir[var19] != var14) {
               this.playerCutawaysDirty[var19] = true;
               this.lastPlayerSquare[var19] = var8;
               this.lastPlayerSquareHalf[var19] = var13;
               this.lastPlayerDir[var19] = var14;
               var15 = var8.getBuilding();
               this.playerWindowPeekingRoomId[var19] = -1L;
               this.GetBuildingsInFrontOfCharacter((ArrayList)this.playerOccluderBuildings.get(var19), var8, var13);
               if (this.playerOccluderBuildingsArr[var2] == null) {
                  this.playerOccluderBuildingsArr[var2] = new IsoBuilding[500];
               }

               this.playerHidesOrphanStructures[var19] = this.bOccludedByOrphanStructureFlag;
               if (var15 == null && !var21.bRemote) {
                  var15 = this.GetPeekedInBuilding(var8, var14);
                  if (var15 != null) {
                     this.playerWindowPeekingRoomId[var19] = this.playerPeekedRoomId;
                  }
               }

               if (var15 != null) {
                  this.AddUniqueToBuildingList((ArrayList)this.playerOccluderBuildings.get(var19), var15);
               }

               ArrayList var16 = (ArrayList)this.playerOccluderBuildings.get(var19);

               for(int var17 = 0; var17 < var16.size(); ++var17) {
                  IsoBuilding var18 = (IsoBuilding)var16.get(var17);
                  this.playerOccluderBuildingsArr[var2][var17] = var18;
               }

               this.playerOccluderBuildingsArr[var2][var16.size()] = null;
            }

            if (var19 == var2 && var4 != null) {
               this.gridSquaresTempLeft.clear();
               this.gridSquaresTempRight.clear();
               this.GetSquaresAroundPlayerSquare(var21, var4, this.gridSquaresTempLeft, this.gridSquaresTempRight);

               boolean[] var10000;
               int var30;
               IsoGridSquare var31;
               ArrayList var34;
               int var35;
               for(var30 = 0; var30 < this.gridSquaresTempLeft.size(); ++var30) {
                  var31 = (IsoGridSquare)this.gridSquaresTempLeft.get(var30);
                  if (var31.getCanSee(var2) && (var31.getBuilding() == null || var31.getBuilding() == var4.getBuilding())) {
                     var34 = this.GetBuildingsInFrontOfMustSeeSquare(var31, IsoGridOcclusionData.OcclusionFilter.Right);

                     for(var35 = 0; var35 < var34.size(); ++var35) {
                        this.AddUniqueToBuildingList((ArrayList)this.otherOccluderBuildings.get(var2), (IsoBuilding)var34.get(var35));
                     }

                     var10000 = this.playerHidesOrphanStructures;
                     var10000[var2] |= this.bOccludedByOrphanStructureFlag;
                  }
               }

               for(var30 = 0; var30 < this.gridSquaresTempRight.size(); ++var30) {
                  var31 = (IsoGridSquare)this.gridSquaresTempRight.get(var30);
                  if (var31.getCanSee(var2) && (var31.getBuilding() == null || var31.getBuilding() == var4.getBuilding())) {
                     var34 = this.GetBuildingsInFrontOfMustSeeSquare(var31, IsoGridOcclusionData.OcclusionFilter.Left);

                     for(var35 = 0; var35 < var34.size(); ++var35) {
                        this.AddUniqueToBuildingList((ArrayList)this.otherOccluderBuildings.get(var2), (IsoBuilding)var34.get(var35));
                     }

                     var10000 = this.playerHidesOrphanStructures;
                     var10000[var2] |= this.bOccludedByOrphanStructureFlag;
                  }
               }

               ArrayList var33 = (ArrayList)this.otherOccluderBuildings.get(var2);
               if (this.otherOccluderBuildingsArr[var2] == null) {
                  this.otherOccluderBuildingsArr[var2] = new IsoBuilding[500];
               }

               for(int var32 = 0; var32 < var33.size(); ++var32) {
                  IsoBuilding var36 = (IsoBuilding)var33.get(var32);
                  this.otherOccluderBuildingsArr[var2][var32] = var36;
               }

               this.otherOccluderBuildingsArr[var2][var33.size()] = null;
            }

            if (this.playerHidesOrphanStructures[var19] && this.hidesOrphanStructuresAbove > var8.getZ()) {
               this.hidesOrphanStructuresAbove = var8.getZ();
            }
         }
      }

      if (var4 != null && this.hidesOrphanStructuresAbove < var4.getZ()) {
         this.hidesOrphanStructuresAbove = var4.getZ();
      }

      boolean var22 = false;
      if (this.nearestVisibleZombie[var2] != null && this.nearestVisibleZombieDistSqr[var2] < 150.0F) {
         IsoGridSquare var23 = this.nearestVisibleZombie[var2].getCurrentSquare();
         if (var23 != null && var23.getCanSee(var2)) {
            double var24 = (double)(this.nearestVisibleZombie[var2].getX() - (float)PZMath.fastfloor(this.nearestVisibleZombie[var2].getX()));
            double var26 = (double)(this.nearestVisibleZombie[var2].getY() - (float)PZMath.fastfloor(this.nearestVisibleZombie[var2].getY()));
            boolean var12 = var24 > var26;
            var22 = true;
            if (this.lastZombieSquare[var2] != var23 || this.lastZombieSquareHalf[var2] != var12) {
               this.lastZombieSquare[var2] = var23;
               this.lastZombieSquareHalf[var2] = var12;
               this.GetBuildingsInFrontOfCharacter((ArrayList)this.zombieOccluderBuildings.get(var2), var23, var12);
               ArrayList var28 = (ArrayList)this.zombieOccluderBuildings.get(var2);
               if (this.zombieOccluderBuildingsArr[var2] == null) {
                  this.zombieOccluderBuildingsArr[var2] = new IsoBuilding[500];
               }

               for(int var29 = 0; var29 < var28.size(); ++var29) {
                  var15 = (IsoBuilding)var28.get(var29);
                  this.zombieOccluderBuildingsArr[var2][var29] = var15;
               }

               this.zombieOccluderBuildingsArr[var2][var28.size()] = null;
            }
         }
      }

      if (!var22) {
         ((ArrayList)this.zombieOccluderBuildings.get(var2)).clear();
         if (this.zombieOccluderBuildingsArr[var2] != null) {
            this.zombieOccluderBuildingsArr[var2][0] = null;
         } else {
            this.zombieOccluderBuildingsArr[var2] = new IsoBuilding[500];
         }
      }

   }

   public void invalidatePeekedRoom(int var1) {
      this.lastPlayerDir[var1] = IsoDirections.Max;
   }

   protected boolean initWeatherFx() {
      if (GameServer.bServer) {
         return false;
      } else {
         if (this.weatherFX == null) {
            this.weatherFX = new IsoWeatherFX();
            this.weatherFX.init();
         }

         return true;
      }
   }

   private void updateWeatherFx() {
      if (this.initWeatherFx()) {
         this.weatherFX.update();
      }

   }

   private void renderWeatherFx() {
      if (this.initWeatherFx()) {
         this.weatherFX.render();
      }

   }

   public IsoWeatherFX getWeatherFX() {
      return this.weatherFX;
   }

   public void renderRain() {
   }

   public void setRainAlpha(int var1) {
      this.rainAlphaMax = (float)var1 / 100.0F;
   }

   public void setRainIntensity(int var1) {
      this.rainIntensity = var1;
   }

   public int getRainIntensity() {
      return this.rainIntensity;
   }

   public void setRainSpeed(int var1) {
      this.rainSpeed = var1;
   }

   public void reloadRainTextures() {
   }

   public void GetBuildingsInFrontOfCharacter(ArrayList<IsoBuilding> var1, IsoGridSquare var2, boolean var3) {
      var1.clear();
      this.bOccludedByOrphanStructureFlag = false;
      if (var2 != null) {
         int var4 = var2.getX();
         int var5 = var2.getY();
         int var6 = var2.getZ();
         this.GetBuildingsInFrontOfCharacterSquare(var4, var5, var6, var3, var1);
         if (var6 < MaxHeight) {
            this.GetBuildingsInFrontOfCharacterSquare(var4 - 1 + 3, var5 - 1 + 3, var6 + 1, var3, var1);
            this.GetBuildingsInFrontOfCharacterSquare(var4 - 2 + 3, var5 - 2 + 3, var6 + 1, var3, var1);
            if (var3) {
               this.GetBuildingsInFrontOfCharacterSquare(var4 + 3, var5 - 1 + 3, var6 + 1, !var3, var1);
               this.GetBuildingsInFrontOfCharacterSquare(var4 - 1 + 3, var5 - 2 + 3, var6 + 1, !var3, var1);
            } else {
               this.GetBuildingsInFrontOfCharacterSquare(var4 - 1 + 3, var5 + 3, var6 + 1, !var3, var1);
               this.GetBuildingsInFrontOfCharacterSquare(var4 - 2 + 3, var5 - 1 + 3, var6 + 1, !var3, var1);
            }
         }

      }
   }

   private void GetBuildingsInFrontOfCharacterSquare(int var1, int var2, int var3, boolean var4, ArrayList<IsoBuilding> var5) {
      IsoGridSquare var6 = this.getGridSquare(var1, var2, var3);
      if (var6 == null) {
         if (var3 < MaxHeight) {
            this.GetBuildingsInFrontOfCharacterSquare(var1 + 3, var2 + 3, var3 + 1, var4, var5);
         }

      } else {
         IsoGridOcclusionData var7 = var6.getOrCreateOcclusionData();
         IsoGridOcclusionData.OcclusionFilter var8 = var4 ? IsoGridOcclusionData.OcclusionFilter.Right : IsoGridOcclusionData.OcclusionFilter.Left;
         this.bOccludedByOrphanStructureFlag |= var7.getCouldBeOccludedByOrphanStructures(var8);
         ArrayList var9 = var7.getBuildingsCouldBeOccluders(var8);

         for(int var10 = 0; var10 < var9.size(); ++var10) {
            this.AddUniqueToBuildingList(var5, (IsoBuilding)var9.get(var10));
         }

      }
   }

   public ArrayList<IsoBuilding> GetBuildingsInFrontOfMustSeeSquare(IsoGridSquare var1, IsoGridOcclusionData.OcclusionFilter var2) {
      IsoGridOcclusionData var3 = var1.getOrCreateOcclusionData();
      this.bOccludedByOrphanStructureFlag = var3.getCouldBeOccludedByOrphanStructures(IsoGridOcclusionData.OcclusionFilter.All);
      return var3.getBuildingsCouldBeOccluders(var2);
   }

   public IsoBuilding GetPeekedInBuilding(IsoGridSquare var1, IsoDirections var2) {
      this.playerPeekedRoomId = -1L;
      if (var1 == null) {
         return null;
      } else {
         IsoGridSquare var3;
         IsoBuilding var4;
         if ((var2 == IsoDirections.NW || var2 == IsoDirections.N || var2 == IsoDirections.NE) && LosUtil.lineClear(this, var1.x, var1.y, var1.z, var1.x, var1.y - 1, var1.z, false) != LosUtil.TestResults.Blocked) {
            var3 = var1.nav[IsoDirections.N.index()];
            if (var3 != null) {
               var4 = var3.getBuilding();
               if (var4 != null) {
                  this.playerPeekedRoomId = var3.getRoomID();
                  return var4;
               }
            }
         }

         if ((var2 == IsoDirections.SW || var2 == IsoDirections.W || var2 == IsoDirections.NW) && LosUtil.lineClear(this, var1.x, var1.y, var1.z, var1.x - 1, var1.y, var1.z, false) != LosUtil.TestResults.Blocked) {
            var3 = var1.nav[IsoDirections.W.index()];
            if (var3 != null) {
               var4 = var3.getBuilding();
               if (var4 != null) {
                  this.playerPeekedRoomId = var3.getRoomID();
                  return var4;
               }
            }
         }

         if ((var2 == IsoDirections.SE || var2 == IsoDirections.S || var2 == IsoDirections.SW) && LosUtil.lineClear(this, var1.x, var1.y, var1.z, var1.x, var1.y + 1, var1.z, false) != LosUtil.TestResults.Blocked) {
            var3 = var1.nav[IsoDirections.S.index()];
            if (var3 != null) {
               var4 = var3.getBuilding();
               if (var4 != null) {
                  this.playerPeekedRoomId = var3.getRoomID();
                  return var4;
               }
            }
         }

         if ((var2 == IsoDirections.NE || var2 == IsoDirections.E || var2 == IsoDirections.SE) && LosUtil.lineClear(this, var1.x, var1.y, var1.z, var1.x + 1, var1.y, var1.z, false) != LosUtil.TestResults.Blocked) {
            var3 = var1.nav[IsoDirections.E.index()];
            if (var3 != null) {
               var4 = var3.getBuilding();
               if (var4 != null) {
                  this.playerPeekedRoomId = var3.getRoomID();
                  return var4;
               }
            }
         }

         return null;
      }
   }

   public void GetSquaresAroundPlayerSquare(IsoPlayer var1, IsoGridSquare var2, ArrayList<IsoGridSquare> var3, ArrayList<IsoGridSquare> var4) {
      float var5 = var1.getX() - 4.0F;
      float var6 = var1.getY() - 4.0F;
      int var7 = PZMath.fastfloor(var5);
      int var8 = PZMath.fastfloor(var6);
      int var9 = var2.getZ();

      for(int var10 = var8; var10 < var8 + 10; ++var10) {
         for(int var11 = var7; var11 < var7 + 10; ++var11) {
            if ((var11 >= PZMath.fastfloor(var1.getX()) || var10 >= PZMath.fastfloor(var1.getY())) && (var11 != PZMath.fastfloor(var1.getX()) || var10 != PZMath.fastfloor(var1.getY()))) {
               float var12 = (float)var11 - var1.getX();
               float var13 = (float)var10 - var1.getY();
               if ((double)var13 < (double)var12 + 4.5 && (double)var13 > (double)var12 - 4.5) {
                  IsoGridSquare var14 = this.getGridSquare(var11, var10, var9);
                  if (var14 != null) {
                     if (var13 >= var12) {
                        var3.add(var14);
                     }

                     if (var13 <= var12) {
                        var4.add(var14);
                     }
                  }
               }
            }
         }
      }

   }

   public boolean IsBehindStuff(IsoGridSquare var1) {
      if (!var1.getProperties().Is(IsoFlagType.exterior)) {
         return true;
      } else {
         for(int var2 = 1; var2 < 64 && var1.getZ() + var2 < MaxHeight; ++var2) {
            for(int var3 = -5; var3 <= 6; ++var3) {
               for(int var4 = -5; var4 <= 6; ++var4) {
                  if (var4 >= var3 - 5 && var4 <= var3 + 5) {
                     IsoGridSquare var6 = this.getGridSquare(var1.getX() + var4 + var2 * 3, var1.getY() + var3 + var2 * 3, var1.getZ() + var2);
                     if (var6 != null && !var6.getObjects().isEmpty()) {
                        if (var2 != 1 || var6.getObjects().size() != 1) {
                           return true;
                        }

                        IsoObject var7 = (IsoObject)var6.getObjects().get(0);
                        if (var7.sprite == null || var7.sprite.name == null || !var7.sprite.name.startsWith("lighting_outdoor")) {
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

   public static IsoDirections FromMouseTile() {
      IsoDirections var0 = IsoDirections.N;
      float var1 = UIManager.getPickedTileLocal().x;
      float var2 = UIManager.getPickedTileLocal().y;
      float var3 = 0.5F - Math.abs(0.5F - var2);
      float var4 = 0.5F - Math.abs(0.5F - var1);
      if (var1 > 0.5F && var4 < var3) {
         var0 = IsoDirections.E;
      } else if (var2 > 0.5F && var4 > var3) {
         var0 = IsoDirections.S;
      } else if (var1 < 0.5F && var4 < var3) {
         var0 = IsoDirections.W;
      } else if (var2 < 0.5F && var4 > var3) {
         var0 = IsoDirections.N;
      }

      return var0;
   }

   public void update() {
      IsoCell.s_performance.isoCellUpdate.invokeAndMeasure(this, IsoCell::updateInternal);
   }

   private void updateInternal() {
      MovingObjectUpdateScheduler.instance.startFrame();
      IsoSprite.alphaStep = 0.075F * GameTime.getInstance().getThirtyFPSMultiplier();
      ++IsoGridSquare.gridSquareCacheEmptyTimer;
      GameProfiler.getInstance().invokeAndMeasure("SpottedRooms", this::ProcessSpottedRooms);
      if (!GameServer.bServer) {
         for(int var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
            if (IsoPlayer.players[var1] != null && (!IsoPlayer.players[var1].isDead() || IsoPlayer.players[var1].ReanimatedCorpse != null)) {
               IsoPlayer.setInstance(IsoPlayer.players[var1]);
               IsoCamera.setCameraCharacter(IsoPlayer.players[var1]);
               this.ChunkMap[var1].update();
            }
         }
      }

      Object var2 = null;
      this.ProcessRemoveItems((Iterator)null);
      if (!GameClient.bClient && !GameServer.bServer || GameServer.bServer && System.currentTimeMillis() - this.lastServerItemsUpdate > 5000L) {
         this.lastServerItemsUpdate = System.currentTimeMillis();
         GameProfiler.getInstance().invokeAndMeasure("Items", (Object)null, this::ProcessItems);
      }

      this.ProcessRemoveItems((Iterator)null);
      GameProfiler.getInstance().invokeAndMeasure("IsoObject", this::ProcessIsoObject);
      this.safeToAdd = false;
      GameProfiler.getInstance().invokeAndMeasure("Objects", (Object)null, this::ProcessObjects);
      GameProfiler var10000;
      if (GameClient.bClient) {
         if (NetworkZombieSimulator.getInstance().anyUnknownZombies() && GameClient.instance.sendZombieRequestsTimer.Check() || GameClient.instance.sendZombieTimer.Check()) {
            NetworkZombieSimulator.getInstance().send();
            GameClient.instance.sendZombieTimer.Reset();
            GameClient.instance.sendZombieRequestsTimer.Reset();
         }

         var10000 = GameProfiler.getInstance();
         AnimalSynchronizationManager var10002 = AnimalSynchronizationManager.getInstance();
         Objects.requireNonNull(var10002);
         var10000.invokeAndMeasure("Animal Sync", var10002::updateInternal);
      }

      if (var2 != null) {
         var10000 = GameProfiler.getInstance();
         Objects.requireNonNull(var2);
         var10000.invokeAndMeasure("Items", var2::join);
      }

      this.safeToAdd = true;
      GameProfiler.getInstance().invokeAndMeasure("Static Updaters", this::ProcessStaticUpdaters);
      this.ObjectDeletionAddition();
      GameProfiler.getInstance().invokeAndMeasure("Update Dead Bodies", IsoDeadBody::updateBodies);
      GameProfiler.getInstance().invokeAndMeasure("Update Fish", FishSchoolManager.getInstance(), FishSchoolManager::update);
      IsoGridSquare.setLightcache(IsoGridSquare.getLightcache() - 1);
      IsoGridSquare.setRecalcLightTime(IsoGridSquare.getRecalcLightTime() - GameTime.getInstance().getThirtyFPSMultiplier());
      if (GameServer.bServer) {
         this.LamppostPositions.clear();
         this.roomLights.clear();
      }

      if (!GameTime.isGamePaused()) {
         this.rainScroll += (float)this.rainSpeed / 10.0F * 0.075F * (30.0F / (float)PerformanceSettings.getLockFPS());
         if (this.rainScroll > 1.0F) {
            this.rainScroll = 0.0F;
         }
      }

      if (!GameServer.bServer) {
         GameProfiler.getInstance().invokeAndMeasure("Update Weather", this, IsoCell::updateWeatherFx);
      }

   }

   IsoGridSquare getRandomFreeTile() {
      IsoGridSquare var1 = null;
      boolean var2 = true;

      do {
         var2 = true;
         var1 = this.getGridSquare(Rand.Next(this.width), Rand.Next(this.height), 0);
         if (var1 == null) {
            var2 = false;
         } else if (!var1.isFree(false)) {
            var2 = false;
         } else if (!var1.getProperties().Is(IsoFlagType.solid) && !var1.getProperties().Is(IsoFlagType.solidtrans)) {
            if (var1.getMovingObjects().size() > 0) {
               var2 = false;
            } else if (!var1.Has(IsoObjectType.stairsBN) && !var1.Has(IsoObjectType.stairsMN) && !var1.Has(IsoObjectType.stairsTN)) {
               if (var1.Has(IsoObjectType.stairsBW) || var1.Has(IsoObjectType.stairsMW) || var1.Has(IsoObjectType.stairsTW)) {
                  var2 = false;
               }
            } else {
               var2 = false;
            }
         } else {
            var2 = false;
         }
      } while(!var2);

      return var1;
   }

   IsoGridSquare getRandomOutdoorFreeTile() {
      IsoGridSquare var1 = null;
      boolean var2 = true;

      do {
         var2 = true;
         var1 = this.getGridSquare(Rand.Next(this.width), Rand.Next(this.height), 0);
         if (var1 == null) {
            var2 = false;
         } else if (!var1.isFree(false)) {
            var2 = false;
         } else if (var1.getRoom() != null) {
            var2 = false;
         } else if (!var1.getProperties().Is(IsoFlagType.solid) && !var1.getProperties().Is(IsoFlagType.solidtrans)) {
            if (var1.getMovingObjects().size() > 0) {
               var2 = false;
            } else if (!var1.Has(IsoObjectType.stairsBN) && !var1.Has(IsoObjectType.stairsMN) && !var1.Has(IsoObjectType.stairsTN)) {
               if (var1.Has(IsoObjectType.stairsBW) || var1.Has(IsoObjectType.stairsMW) || var1.Has(IsoObjectType.stairsTW)) {
                  var2 = false;
               }
            } else {
               var2 = false;
            }
         } else {
            var2 = false;
         }
      } while(!var2);

      return var1;
   }

   public IsoGridSquare getRandomFreeTileInRoom() {
      Stack var1 = new Stack();

      for(int var2 = 0; var2 < this.RoomList.size(); ++var2) {
         if (((IsoRoom)this.RoomList.get(var2)).TileList.size() > 9 && !((IsoRoom)this.RoomList.get(var2)).Exits.isEmpty() && ((IsoGridSquare)((IsoRoom)this.RoomList.get(var2)).TileList.get(0)).getProperties().Is(IsoFlagType.solidfloor)) {
            var1.add((IsoRoom)this.RoomList.get(var2));
         }
      }

      if (var1.isEmpty()) {
         return null;
      } else {
         IsoRoom var3 = (IsoRoom)var1.get(Rand.Next(var1.size()));
         return var3.getFreeTile();
      }
   }

   public void roomSpotted(IsoRoom var1) {
      synchronized(this.SpottedRooms) {
         if (!this.SpottedRooms.contains(var1)) {
            this.SpottedRooms.push(var1);
         }

      }
   }

   public void ProcessSpottedRooms() {
      synchronized(this.SpottedRooms) {
         for(int var2 = 0; var2 < this.SpottedRooms.size(); ++var2) {
            IsoRoom var3 = (IsoRoom)this.SpottedRooms.get(var2);
            if (!var3.def.bDoneSpawn) {
               var3.def.bDoneSpawn = true;
               LuaEventManager.triggerEvent("OnSeeNewRoom", var3);
               VirtualZombieManager.instance.roomSpotted(var3);
               if (!GameClient.bClient && !Core.bLastStand && ("shed".equals(var3.def.name) || "garage".equals(var3.def.name) || "garagestorage".equals(var3.def.name) || "storageunit".equals(var3.def.name) || "farmstorage".equals(var3.def.name))) {
                  int var4 = 7;
                  if ("shed".equals(var3.def.name) || "garagestorage".equals(var3.def.name) || "farmstorage".equals(var3.def.name)) {
                     var4 = 4;
                  }

                  switch (SandboxOptions.instance.GeneratorSpawning.getValue()) {
                     case 1:
                        var4 = 0;
                        break;
                     case 2:
                        var4 += 20;
                        break;
                     case 3:
                        var4 += 3;
                        break;
                     case 4:
                        var4 += 2;
                     case 5:
                     default:
                        break;
                     case 6:
                        var4 -= 2;
                        break;
                     case 7:
                        var4 -= 3;
                  }

                  if (var4 != 0 && Rand.Next(var4) == 0) {
                     var3.spawnRandomWorkstation();
                  }

                  if (var4 != 0 && Rand.Next(var4) == 0) {
                     IsoGridSquare var5 = var3.getRandomDoorFreeSquare();
                     if (var5 != null) {
                        var5.spawnRandomGenerator();
                     }
                  }
               }
            }
         }

         this.SpottedRooms.clear();
      }
   }

   public IsoObject addTileObject(IsoGridSquare var1, String var2) {
      if (var1 == null) {
         return null;
      } else {
         IsoObject var3 = IsoObject.getNew(var1, var2, (String)null, false);
         var1.AddTileObject(var3);
         MapObjects.newGridSquare(var1);
         MapObjects.loadGridSquare(var1);
         return var3;
      }
   }

   public void save(DataOutputStream var1, boolean var2) throws IOException {
      while(ChunkSaveWorker.instance.bSaving) {
         try {
            Thread.sleep(30L);
         } catch (InterruptedException var5) {
            var5.printStackTrace();
         }
      }

      for(int var3 = 0; var3 < IsoPlayer.numPlayers; ++var3) {
         this.ChunkMap[var3].Save();
      }

      var1.writeInt(this.width);
      var1.writeInt(this.height);
      var1.writeInt(MaxHeight);
      File var6 = ZomboidFileSystem.instance.getFileInCurrentSave("map_t.bin");
      FileOutputStream var4 = new FileOutputStream(var6);
      var1 = new DataOutputStream(new BufferedOutputStream(var4));
      GameTime.instance.save(var1);
      var1.flush();
      var1.close();
      IsoWorld.instance.MetaGrid.save();
      if (PlayerDB.isAllow()) {
         PlayerDB.getInstance().savePlayers();
      }

      ReanimatedPlayers.instance.saveReanimatedPlayers();
   }

   public boolean LoadPlayer(int var1) throws FileNotFoundException, IOException {
      if (GameClient.bClient) {
         return ClientPlayerDB.getInstance().loadNetworkPlayer();
      } else {
         File var2 = ZomboidFileSystem.instance.getFileInCurrentSave("map_p.bin");
         if (!var2.exists()) {
            PlayerDB.getInstance().importPlayersFromVehiclesDB();
            return PlayerDB.getInstance().loadLocalPlayer(1);
         } else {
            FileInputStream var3 = new FileInputStream(var2);
            BufferedInputStream var4 = new BufferedInputStream(var3);
            synchronized(SliceY.SliceBufferLock) {
               SliceY.SliceBuffer.clear();
               int var6 = var4.read(SliceY.SliceBuffer.array());
               SliceY.SliceBuffer.limit(var6);
               byte var7 = SliceY.SliceBuffer.get();
               byte var8 = SliceY.SliceBuffer.get();
               byte var9 = SliceY.SliceBuffer.get();
               byte var10 = SliceY.SliceBuffer.get();
               if (var7 == 80 && var8 == 76 && var9 == 89 && var10 == 82) {
                  var1 = SliceY.SliceBuffer.getInt();
               } else {
                  SliceY.SliceBuffer.rewind();
               }

               String var11 = GameWindow.ReadString(SliceY.SliceBuffer);
               if (GameClient.bClient && !IsoPlayer.isServerPlayerIDValid(var11)) {
                  GameLoadingState.GameLoadingString = Translator.getText("IGUI_MP_ServerPlayerIDMismatch");
                  GameLoadingState.playerWrongIP = true;
                  return false;
               }

               instance.ChunkMap[IsoPlayer.getPlayerIndex()].WorldX = SliceY.SliceBuffer.getInt() + IsoWorld.saveoffsetx * CellSizeInChunks;
               instance.ChunkMap[IsoPlayer.getPlayerIndex()].WorldY = SliceY.SliceBuffer.getInt() + IsoWorld.saveoffsety * CellSizeInChunks;
               SliceY.SliceBuffer.getInt();
               SliceY.SliceBuffer.getInt();
               SliceY.SliceBuffer.getInt();
               if (IsoPlayer.getInstance() == null) {
                  IsoPlayer.setInstance(new IsoPlayer(instance));
                  IsoPlayer.players[0] = IsoPlayer.getInstance();
               }

               IsoPlayer.getInstance().load(SliceY.SliceBuffer, var1);
               var3.close();
            }

            PlayerDB.getInstance().saveLocalPlayersForce();
            var2.delete();
            PlayerDB.getInstance().uploadLocalPlayers2DB();
            return true;
         }
      }
   }

   public IsoGridSquare getRelativeGridSquare(int var1, int var2, int var3) {
      int var10000 = this.ChunkMap[0].getWorldXMin();
      IsoChunkMap var10001 = this.ChunkMap[0];
      int var4 = var10000 * 8;
      var10000 = this.ChunkMap[0].getWorldYMin();
      var10001 = this.ChunkMap[0];
      int var5 = var10000 * 8;
      var1 += var4;
      var2 += var5;
      return this.getGridSquare(var1, var2, var3);
   }

   public IsoGridSquare createNewGridSquare(int var1, int var2, int var3, boolean var4) {
      if (!IsoWorld.instance.isValidSquare(var1, var2, var3)) {
         return null;
      } else {
         IsoGridSquare var5 = this.getGridSquare(var1, var2, var3);
         if (var5 != null) {
            return var5;
         } else {
            if (GameServer.bServer) {
               int var6 = var1 / 8;
               int var7 = var2 / 8;
               if (ServerMap.instance.getChunk(var6, var7) != null) {
                  var5 = IsoGridSquare.getNew(this, (SliceY)null, var1, var2, var3);
                  ServerMap.instance.setGridSquare(var1, var2, var3, var5);
               }
            } else if (this.getChunkForGridSquare(var1, var2, var3) != null) {
               var5 = IsoGridSquare.getNew(this, (SliceY)null, var1, var2, var3);
               this.ConnectNewSquare(var5, true);
            }

            if (var5 != null && var4) {
               var5.RecalcAllWithNeighbours(true);
            }

            return var5;
         }
      }
   }

   public IsoGridSquare getGridSquareDirect(int var1, int var2, int var3, int var4) {
      int var5 = IsoChunkMap.ChunkWidthInTiles;
      return this.ChunkMap[var4].getGridSquareDirect(var1, var2, var3);
   }

   public boolean isInChunkMap(int var1, int var2) {
      for(int var3 = 0; var3 < IsoPlayer.numPlayers; ++var3) {
         int var4 = this.ChunkMap[var3].getWorldXMinTiles();
         int var5 = this.ChunkMap[var3].getWorldXMaxTiles();
         int var6 = this.ChunkMap[var3].getWorldYMinTiles();
         int var7 = this.ChunkMap[var3].getWorldYMaxTiles();
         if (var1 >= var4 && var1 < var5 && var2 >= var6 && var2 < var7) {
            return true;
         }
      }

      return false;
   }

   public ArrayList<IsoObject> getProcessIsoObjectRemove() {
      return this.ProcessIsoObjectRemove;
   }

   public void checkHaveRoof(int var1, int var2) {
      boolean var3 = false;

      for(int var4 = 31; var4 >= 0; --var4) {
         IsoGridSquare var5 = this.getGridSquare(var1, var2, var4);
         if (var5 != null) {
            if (var3 != var5.haveRoof) {
               var5.haveRoof = var3;
               var5.RecalcAllWithNeighbours(true);
            }

            if (var5.hasRainBlockingTile()) {
               var3 = true;
            }
         }
      }

   }

   public IsoZombie getFakeZombieForHit() {
      if (this.fakeZombieForHit == null) {
         this.fakeZombieForHit = new IsoZombie(this);
      }

      return this.fakeZombieForHit;
   }

   public void addHeatSource(IsoHeatSource var1) {
      if (!GameServer.bServer) {
         if (this.heatSources.contains(var1)) {
            DebugLog.log("ERROR addHeatSource called again with the same HeatSource");
         } else {
            this.heatSources.add(var1);
         }
      }
   }

   public void removeHeatSource(IsoHeatSource var1) {
      if (!GameServer.bServer) {
         this.heatSources.remove(var1);
      }
   }

   public void updateHeatSources() {
      if (!GameServer.bServer) {
         for(int var1 = this.heatSources.size() - 1; var1 >= 0; --var1) {
            IsoHeatSource var2 = (IsoHeatSource)this.heatSources.get(var1);
            if (!var2.isInBounds()) {
               this.heatSources.remove(var1);
            }
         }

      }
   }

   public int getHeatSourceTemperature(int var1, int var2, int var3) {
      int var4 = 0;

      for(int var5 = 0; var5 < this.heatSources.size(); ++var5) {
         IsoHeatSource var6 = (IsoHeatSource)this.heatSources.get(var5);
         if (var6.getZ() == var3) {
            float var7 = IsoUtils.DistanceToSquared((float)var1, (float)var2, (float)var6.getX(), (float)var6.getY());
            if (var7 < (float)(var6.getRadius() * var6.getRadius())) {
               LosUtil.TestResults var8 = LosUtil.lineClear(this, var6.getX(), var6.getY(), var6.getZ(), var1, var2, var3, false);
               if (var8 == LosUtil.TestResults.Clear || var8 == LosUtil.TestResults.ClearThroughOpenDoor) {
                  var4 = (int)((double)var4 + (double)var6.getTemperature() * (1.0 - Math.sqrt((double)var7) / (double)var6.getRadius()));
               }
            }
         }
      }

      return var4;
   }

   public float getHeatSourceHighestTemperature(float var1, int var2, int var3, int var4) {
      float var5 = var1;
      float var6 = var1;
      float var7 = 0.0F;
      IsoGridSquare var8 = null;
      float var9 = 0.0F;

      for(int var10 = 0; var10 < this.heatSources.size(); ++var10) {
         IsoHeatSource var11 = (IsoHeatSource)this.heatSources.get(var10);
         if (var11.getZ() == var4) {
            float var12 = IsoUtils.DistanceToSquared((float)var2, (float)var3, (float)var11.getX(), (float)var11.getY());
            var8 = this.getGridSquare(var11.getX(), var11.getY(), var11.getZ());
            var9 = 0.0F;
            if (var8 != null) {
               if (!var8.isInARoom()) {
                  var9 = var5 - 30.0F;
                  if (var9 < -15.0F) {
                     var9 = -15.0F;
                  } else if (var9 > 5.0F) {
                     var9 = 5.0F;
                  }
               } else {
                  var9 = var5 - 30.0F;
                  if (var9 < -7.0F) {
                     var9 = -7.0F;
                  } else if (var9 > 7.0F) {
                     var9 = 7.0F;
                  }
               }
            }

            var7 = ClimateManager.lerp((float)(1.0 - Math.sqrt((double)var12) / (double)var11.getRadius()), var5, (float)var11.getTemperature() + var9);
            if (!(var7 <= var6) && var12 < (float)(var11.getRadius() * var11.getRadius())) {
               LosUtil.TestResults var13 = LosUtil.lineClear(this, var11.getX(), var11.getY(), var11.getZ(), var2, var3, var4, false);
               if (var13 == LosUtil.TestResults.Clear || var13 == LosUtil.TestResults.ClearThroughOpenDoor) {
                  var6 = var7;
               }
            }
         }
      }

      return var6;
   }

   public void putInVehicle(IsoGameCharacter var1) {
      if (var1 != null && var1.savedVehicleSeat != -1) {
         int var2 = (PZMath.fastfloor(var1.getX()) - 4) / 8;
         int var3 = (PZMath.fastfloor(var1.getY()) - 4) / 8;
         int var4 = (PZMath.fastfloor(var1.getX()) + 4) / 8;
         int var5 = (PZMath.fastfloor(var1.getY()) + 4) / 8;

         for(int var6 = var3; var6 <= var5; ++var6) {
            for(int var7 = var2; var7 <= var4; ++var7) {
               IsoChunk var8 = this.getChunkForGridSquare(var7 * 8, var6 * 8, PZMath.fastfloor(var1.getZ()));
               if (var8 != null) {
                  for(int var9 = 0; var9 < var8.vehicles.size(); ++var9) {
                     BaseVehicle var10 = (BaseVehicle)var8.vehicles.get(var9);
                     if (PZMath.fastfloor(var10.getZ()) == PZMath.fastfloor(var1.getZ()) && IsoUtils.DistanceToSquared(var10.getX(), var10.getY(), var1.savedVehicleX, var1.savedVehicleY) < 0.010000001F) {
                        if (var10.VehicleID == -1) {
                           return;
                        }

                        VehicleScript.Position var11 = var10.getPassengerPosition(var1.savedVehicleSeat, "inside");
                        if (var11 != null && !var10.isSeatOccupied(var1.savedVehicleSeat)) {
                           var10.enter(var1.savedVehicleSeat, var1, var11.offset);
                           LuaEventManager.triggerEvent("OnEnterVehicle", var1);
                           if (var10.getCharacter(var1.savedVehicleSeat) == var1 && var1.savedVehicleRunning) {
                              var10.resumeRunningAfterLoad();
                           }
                        }

                        return;
                     }
                  }
               }
            }
         }

      }
   }

   /** @deprecated */
   @Deprecated
   public void resumeVehicleSounds(IsoGameCharacter var1) {
      if (var1 != null && var1.savedVehicleSeat != -1) {
         int var2 = (PZMath.fastfloor(var1.getX()) - 4) / 8;
         int var3 = (PZMath.fastfloor(var1.getY()) - 4) / 8;
         int var4 = (PZMath.fastfloor(var1.getX()) + 4) / 8;
         int var5 = (PZMath.fastfloor(var1.getY()) + 4) / 8;

         for(int var6 = var3; var6 <= var5; ++var6) {
            for(int var7 = var2; var7 <= var4; ++var7) {
               IsoChunk var8 = this.getChunkForGridSquare(var7 * 8, var6 * 8, PZMath.fastfloor(var1.getZ()));
               if (var8 != null) {
                  for(int var9 = 0; var9 < var8.vehicles.size(); ++var9) {
                     BaseVehicle var10 = (BaseVehicle)var8.vehicles.get(var9);
                     if (var10.lightbarSirenMode.isEnable()) {
                        var10.setLightbarSirenMode(var10.lightbarSirenMode.get());
                     }
                  }
               }
            }
         }

      }
   }

   public void AddUniqueToBuildingList(ArrayList<IsoBuilding> var1, IsoBuilding var2) {
      for(int var3 = 0; var3 < var1.size(); ++var3) {
         if (var1.get(var3) == var2) {
            return;
         }
      }

      var1.add(var2);
   }

   public IsoSpriteManager getSpriteManager() {
      return IsoSpriteManager.instance;
   }

   static {
      CellSizeInSquares = 8 * CellSizeInChunks;
      MaxHeight = 32;
      stchoices = new ArrayList();
      buildingscores = new Stack();
      GridStack = null;
      ShadowSquares = new ArrayList(1000);
      MinusFloorCharacters = new ArrayList(1000);
      SolidFloor = new ArrayList(5000);
      ShadedFloor = new ArrayList(5000);
      VegetationCorpses = new ArrayList(5000);
      perPlayerRender = new PerPlayerRender[4];
   }

   public static class s_performance {
      static final PerformanceProfileProbe isoCellUpdate = new PerformanceProfileProbe("IsoCell.update");
      static final PerformanceProfileProbe isoCellRender = new PerformanceProfileProbe("IsoCell.render");
      public static final PerformanceProfileProbe isoCellRenderTiles = new PerformanceProfileProbe("IsoCell.renderTiles");
      static final PerformanceProfileProbe isoCellDoBuilding = new PerformanceProfileProbe("IsoCell.doBuilding");

      public s_performance() {
      }

      public static class renderTiles {
         public static final PerformanceProfileProbe performRenderTiles = new PerformanceProfileProbe("performRenderTiles");
         public static final PerformanceProfileProbe recalculateAnyGridStacks = new PerformanceProfileProbe("recalculateAnyGridStacks");
         public static final PerformanceProfileProbe flattenAnyFoliage = new PerformanceProfileProbe("flattenAnyFoliage");
         public static final PerformanceProfileProbe renderDebugPhysics = new PerformanceProfileProbe("renderDebugPhysics");
         public static final PerformanceProfileProbe renderDebugLighting = new PerformanceProfileProbe("renderDebugLighting");
         static PerformanceProfileProbeList<PerformRenderTilesLayer> performRenderTilesLayers = PerformanceProfileProbeList.construct("performRenderTiles", 8, PerformRenderTilesLayer.class, PerformRenderTilesLayer::new);

         public renderTiles() {
         }

         static class PerformRenderTilesLayer extends PerformanceProfileProbe {
            final PerformanceProfileProbe renderIsoWater = new PerformanceProfileProbe("renderIsoWater");
            final PerformanceProfileProbe renderFloor = new PerformanceProfileProbe("renderFloor");
            final PerformanceProfileProbe renderPuddles = new PerformanceProfileProbe("renderPuddles");
            final PerformanceProfileProbe renderShore = new PerformanceProfileProbe("renderShore");
            final PerformanceProfileProbe renderSnow = new PerformanceProfileProbe("renderSnow");
            final PerformanceProfileProbe renderBlood = new PerformanceProfileProbe("renderBlood");
            final PerformanceProfileProbe vegetationCorpses = new PerformanceProfileProbe("vegetationCorpses");
            final PerformanceProfileProbe renderFloorShading = new PerformanceProfileProbe("renderFloorShading");
            final PerformanceProfileProbe renderShadows = new PerformanceProfileProbe("renderShadows");
            final PerformanceProfileProbe luaOnPostFloorLayerDraw = new PerformanceProfileProbe("luaOnPostFloorLayerDraw");
            final PerformanceProfileProbe minusFloorCharacters = new PerformanceProfileProbe("minusFloorCharacters");

            PerformRenderTilesLayer(String var1) {
               super(var1);
            }
         }
      }
   }

   public static final class PerPlayerRender {
      public final IsoGridStack GridStacks = new IsoGridStack(65);
      public boolean[][][] VisiOccludedFlags;
      public boolean[][] VisiCulledFlags;
      public boolean[][] FlattenGrassEtc;
      public int minX;
      public int minY;
      public int maxX;
      public int maxY;

      public PerPlayerRender() {
      }

      public void setSize(int var1, int var2) {
         if (this.VisiOccludedFlags == null || this.VisiOccludedFlags.length < var1 || this.VisiOccludedFlags[0].length < var2) {
            this.VisiOccludedFlags = new boolean[var1][var2][2];
            this.VisiCulledFlags = new boolean[var1][var2];
            this.FlattenGrassEtc = new boolean[var1][var2];
         }

      }
   }

   private class SnowGrid {
      public int w = 256;
      public int h = 256;
      public int frac = 0;
      public static final int N = 0;
      public static final int S = 1;
      public static final int W = 2;
      public static final int E = 3;
      public static final int A = 0;
      public static final int B = 1;
      public final Texture[][][] grid;
      public final byte[][][] gridType;

      public SnowGrid(int var2) {
         this.grid = new Texture[this.w][this.h][2];
         this.gridType = new byte[this.w][this.h][2];
         this.init(var2);
      }

      public SnowGrid init(int var1) {
         int var2;
         int var3;
         int var4;
         int var5;
         if (!IsoCell.this.hasSetupSnowGrid) {
            IsoCell.this.snowNoise2D = new Noise2D();
            IsoCell.this.snowNoise2D.addLayer(16, 0.5F, 3.0F);
            IsoCell.this.snowNoise2D.addLayer(32, 2.0F, 5.0F);
            IsoCell.this.snowNoise2D.addLayer(64, 5.0F, 8.0F);
            var2 = 0;
            byte var11 = (byte)(var2 + 1);
            IsoCell.this.snowGridTiles_Square = IsoCell.this.new SnowGridTiles((byte)var2);
            var3 = 40;

            for(var4 = 0; var4 < 4; ++var4) {
               IsoCell.this.snowGridTiles_Square.add(Texture.getSharedTexture("e_newsnow_ground_1_" + (var3 + var4)));
            }

            IsoCell.this.snowGridTiles_Enclosed = IsoCell.this.new SnowGridTiles(var11++);
            var3 = 0;

            for(var4 = 0; var4 < 4; ++var4) {
               IsoCell.this.snowGridTiles_Enclosed.add(Texture.getSharedTexture("e_newsnow_ground_1_" + (var3 + var4)));
            }

            IsoCell.this.snowGridTiles_Cove = new SnowGridTiles[4];

            for(var4 = 0; var4 < 4; ++var4) {
               IsoCell.this.snowGridTiles_Cove[var4] = IsoCell.this.new SnowGridTiles(var11++);
               if (var4 == 0) {
                  var3 = 7;
               }

               if (var4 == 2) {
                  var3 = 4;
               }

               if (var4 == 1) {
                  var3 = 5;
               }

               if (var4 == 3) {
                  var3 = 6;
               }

               for(var5 = 0; var5 < 3; ++var5) {
                  IsoCell.this.snowGridTiles_Cove[var4].add(Texture.getSharedTexture("e_newsnow_ground_1_" + (var3 + var5 * 4)));
               }
            }

            IsoCell.this.m_snowFirstNonSquare = var11;
            IsoCell.this.snowGridTiles_Edge = new SnowGridTiles[4];

            for(var4 = 0; var4 < 4; ++var4) {
               IsoCell.this.snowGridTiles_Edge[var4] = IsoCell.this.new SnowGridTiles(var11++);
               if (var4 == 0) {
                  var3 = 16;
               }

               if (var4 == 2) {
                  var3 = 18;
               }

               if (var4 == 1) {
                  var3 = 17;
               }

               if (var4 == 3) {
                  var3 = 19;
               }

               for(var5 = 0; var5 < 3; ++var5) {
                  IsoCell.this.snowGridTiles_Edge[var4].add(Texture.getSharedTexture("e_newsnow_ground_1_" + (var3 + var5 * 4)));
               }
            }

            IsoCell.this.snowGridTiles_Strip = new SnowGridTiles[4];

            for(var4 = 0; var4 < 4; ++var4) {
               IsoCell.this.snowGridTiles_Strip[var4] = IsoCell.this.new SnowGridTiles(var11++);
               if (var4 == 0) {
                  var3 = 28;
               }

               if (var4 == 2) {
                  var3 = 29;
               }

               if (var4 == 1) {
                  var3 = 31;
               }

               if (var4 == 3) {
                  var3 = 30;
               }

               for(var5 = 0; var5 < 3; ++var5) {
                  IsoCell.this.snowGridTiles_Strip[var4].add(Texture.getSharedTexture("e_newsnow_ground_1_" + (var3 + var5 * 4)));
               }
            }

            IsoCell.this.hasSetupSnowGrid = true;
         }

         IsoCell.this.snowGridTiles_Square.resetCounter();
         IsoCell.this.snowGridTiles_Enclosed.resetCounter();

         for(var2 = 0; var2 < 4; ++var2) {
            IsoCell.this.snowGridTiles_Cove[var2].resetCounter();
            IsoCell.this.snowGridTiles_Edge[var2].resetCounter();
            IsoCell.this.snowGridTiles_Strip[var2].resetCounter();
         }

         this.frac = var1;
         Noise2D var12 = IsoCell.this.snowNoise2D;

         for(var3 = 0; var3 < this.h; ++var3) {
            for(var4 = 0; var4 < this.w; ++var4) {
               for(var5 = 0; var5 < 2; ++var5) {
                  this.grid[var4][var3][var5] = null;
                  this.gridType[var4][var3][var5] = -1;
               }

               if (var12.layeredNoise((float)var4 / 10.0F, (float)var3 / 10.0F) <= (float)var1 / 100.0F) {
                  this.grid[var4][var3][0] = IsoCell.this.snowGridTiles_Square.getNext();
                  this.gridType[var4][var3][0] = IsoCell.this.snowGridTiles_Square.ID;
               }
            }
         }

         for(int var8 = 0; var8 < this.h; ++var8) {
            for(int var9 = 0; var9 < this.w; ++var9) {
               Texture var10 = this.grid[var9][var8][0];
               if (var10 == null) {
                  boolean var14 = this.check(var9, var8 - 1);
                  boolean var13 = this.check(var9, var8 + 1);
                  boolean var6 = this.check(var9 - 1, var8);
                  boolean var7 = this.check(var9 + 1, var8);
                  var3 = 0;
                  if (var14) {
                     ++var3;
                  }

                  if (var13) {
                     ++var3;
                  }

                  if (var7) {
                     ++var3;
                  }

                  if (var6) {
                     ++var3;
                  }

                  if (var3 != 0) {
                     if (var3 == 1) {
                        if (var14) {
                           this.set(var9, var8, 0, IsoCell.this.snowGridTiles_Strip[0]);
                        } else if (var13) {
                           this.set(var9, var8, 0, IsoCell.this.snowGridTiles_Strip[1]);
                        } else if (var7) {
                           this.set(var9, var8, 0, IsoCell.this.snowGridTiles_Strip[3]);
                        } else if (var6) {
                           this.set(var9, var8, 0, IsoCell.this.snowGridTiles_Strip[2]);
                        }
                     } else if (var3 == 2) {
                        if (var14 && var13) {
                           this.set(var9, var8, 0, IsoCell.this.snowGridTiles_Strip[0]);
                           this.set(var9, var8, 1, IsoCell.this.snowGridTiles_Strip[1]);
                        } else if (var7 && var6) {
                           this.set(var9, var8, 0, IsoCell.this.snowGridTiles_Strip[2]);
                           this.set(var9, var8, 1, IsoCell.this.snowGridTiles_Strip[3]);
                        } else if (var14) {
                           this.set(var9, var8, 0, IsoCell.this.snowGridTiles_Edge[var6 ? 0 : 3]);
                        } else if (var13) {
                           this.set(var9, var8, 0, IsoCell.this.snowGridTiles_Edge[var6 ? 2 : 1]);
                        } else if (var6) {
                           this.set(var9, var8, 0, IsoCell.this.snowGridTiles_Edge[var14 ? 0 : 2]);
                        } else if (var7) {
                           this.set(var9, var8, 0, IsoCell.this.snowGridTiles_Edge[var14 ? 3 : 1]);
                        }
                     } else if (var3 == 3) {
                        if (!var14) {
                           this.set(var9, var8, 0, IsoCell.this.snowGridTiles_Cove[1]);
                        } else if (!var13) {
                           this.set(var9, var8, 0, IsoCell.this.snowGridTiles_Cove[0]);
                        } else if (!var7) {
                           this.set(var9, var8, 0, IsoCell.this.snowGridTiles_Cove[2]);
                        } else if (!var6) {
                           this.set(var9, var8, 0, IsoCell.this.snowGridTiles_Cove[3]);
                        }
                     } else if (var3 == 4) {
                        this.set(var9, var8, 0, IsoCell.this.snowGridTiles_Enclosed);
                     }
                  }
               }
            }
         }

         return this;
      }

      public boolean check(int var1, int var2) {
         if (var1 == this.w) {
            var1 = 0;
         }

         if (var1 == -1) {
            var1 = this.w - 1;
         }

         if (var2 == this.h) {
            var2 = 0;
         }

         if (var2 == -1) {
            var2 = this.h - 1;
         }

         if (var1 >= 0 && var1 < this.w) {
            if (var2 >= 0 && var2 < this.h) {
               Texture var3 = this.grid[var1][var2][0];
               return IsoCell.this.snowGridTiles_Square.contains(var3);
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      public boolean checkAny(int var1, int var2) {
         if (var1 == this.w) {
            var1 = 0;
         }

         if (var1 == -1) {
            var1 = this.w - 1;
         }

         if (var2 == this.h) {
            var2 = 0;
         }

         if (var2 == -1) {
            var2 = this.h - 1;
         }

         if (var1 >= 0 && var1 < this.w) {
            if (var2 >= 0 && var2 < this.h) {
               return this.grid[var1][var2][0] != null;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      public void set(int var1, int var2, int var3, SnowGridTiles var4) {
         if (var1 == this.w) {
            var1 = 0;
         }

         if (var1 == -1) {
            var1 = this.w - 1;
         }

         if (var2 == this.h) {
            var2 = 0;
         }

         if (var2 == -1) {
            var2 = this.h - 1;
         }

         if (var1 >= 0 && var1 < this.w) {
            if (var2 >= 0 && var2 < this.h) {
               this.grid[var1][var2][var3] = var4.getNext();
               this.gridType[var1][var2][var3] = var4.ID;
            }
         }
      }

      public void subtract(SnowGrid var1) {
         for(int var2 = 0; var2 < this.h; ++var2) {
            for(int var3 = 0; var3 < this.w; ++var3) {
               for(int var4 = 0; var4 < 2; ++var4) {
                  if (var1.gridType[var3][var2][var4] == this.gridType[var3][var2][var4]) {
                     this.grid[var3][var2][var4] = null;
                     this.gridType[var3][var2][var4] = -1;
                  }
               }
            }
         }

      }
   }

   protected class SnowGridTiles {
      protected byte ID = -1;
      private int counter = -1;
      private final ArrayList<Texture> textures = new ArrayList();

      public SnowGridTiles(byte var2) {
         this.ID = var2;
      }

      protected void add(Texture var1) {
         this.textures.add(var1);
      }

      protected Texture getNext() {
         ++this.counter;
         if (this.counter >= this.textures.size()) {
            this.counter = 0;
         }

         return (Texture)this.textures.get(this.counter);
      }

      protected Texture get(int var1) {
         return (Texture)this.textures.get(var1);
      }

      protected int size() {
         return this.textures.size();
      }

      protected Texture getRand() {
         return (Texture)this.textures.get(Rand.Next(4));
      }

      protected boolean contains(Texture var1) {
         return this.textures.contains(var1);
      }

      protected void resetCounter() {
         this.counter = 0;
      }
   }

   public static enum BuildingSearchCriteria {
      Food,
      Defense,
      Wood,
      Weapons,
      General;

      private BuildingSearchCriteria() {
      }
   }
}
