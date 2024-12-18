package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.SandboxOptions;
import zombie.Lua.LuaEventManager;
import zombie.audio.FMODAmbientWalls;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PZForkJoinPool;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.properties.PropertyContainer;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.gameStates.DebugChunkState;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkLevel;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoFloorBloodSplat;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMarkers;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoPuddlesGeometry;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWater;
import zombie.iso.IsoWaterGeometry;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.WorldMarkers;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.objects.IsoCarBatteryCharger;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoFire;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.sprite.CorpseFlies;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.sprite.shapers.FloorShaper;
import zombie.iso.sprite.shapers.FloorShaperAttachedSprites;
import zombie.iso.sprite.shapers.FloorShaperDeDiamond;
import zombie.iso.sprite.shapers.FloorShaperDiamond;
import zombie.iso.sprite.shapers.WallShaperN;
import zombie.iso.sprite.shapers.WallShaperW;
import zombie.iso.weather.fog.ImprovedFog;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.network.GameClient;
import zombie.popman.ObjectPool;
import zombie.tileDepth.TileSeamManager;
import zombie.tileDepth.TileSeamModifier;
import zombie.ui.UIManager;
import zombie.util.Type;
import zombie.util.list.PZArrayList;
import zombie.vehicles.BaseVehicle;
import zombie.vispoly.VisibilityPolygon2;

public final class FBORenderCell {
   public static final FBORenderCell instance = new FBORenderCell();
   public IsoCell cell;
   public final ArrayList<IsoGridSquare> WaterSquares = new ArrayList();
   public final ArrayList<IsoGridSquare> WaterAttachSquares = new ArrayList();
   public final ArrayList<IsoGridSquare> FishSplashSquares = new ArrayList();
   public final ArrayList<IsoMannequin> mannequinList = new ArrayList();
   public boolean bRenderAnimatedAttachments = false;
   public boolean bRenderTranslucentOnly = false;
   private final PerPlayerData[] perPlayerData = new PerPlayerData[4];
   private long currentTimeMillis;
   private boolean bWindEffects = false;
   private int puddlesQuality = -1;
   private float puddlesValue = 0.0F;
   private float wetGroundValue = 0.0F;
   private long puddlesRedrawTimeMS = 0L;
   private float snowFracTarget = 0.0F;
   private final int maxChunksPerFrame = 5;
   private IsoChunk lastChunkUpdated = null;
   private final ColorInfo defColorInfo = new ColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
   private final ArrayList<ArrayList<IsoFloorBloodSplat>> splatByType = new ArrayList();
   private final PZArrayList<IsoWorldInventoryObject> tempWorldInventoryObjects = new PZArrayList(IsoWorldInventoryObject.class, 16);
   private final ArrayList<IsoGridSquare> tempSquares = new ArrayList();
   private final ArrayList<IsoGameCharacter.Location> tempLocations = new ArrayList();
   private final ObjectPool<IsoGameCharacter.Location> locationPool = new ObjectPool(IsoGameCharacter.Location::new);
   private long delayedLoadingTimerMS = 0L;
   private boolean m_bInvalidateDelayedLoadingLevels = false;
   public static final PerformanceProfileProbe calculateRenderInfo = new PerformanceProfileProbe("FBORenderCell.calculateRenderInfo");
   public static final PerformanceProfileProbe cutaways = new PerformanceProfileProbe("FBORenderCell.cutaways");
   public static final PerformanceProfileProbe fog = new PerformanceProfileProbe("FBORenderCell.fog");
   public static final PerformanceProfileProbe puddles = new PerformanceProfileProbe("FBORenderCell.puddles");
   public static final PerformanceProfileProbe renderOneChunk = new PerformanceProfileProbe("FBORenderCell.renderOneChunk");
   public static final PerformanceProfileProbe renderOneChunkLevel = new PerformanceProfileProbe("FBORenderCell.renderOneChunkLevel");
   public static final PerformanceProfileProbe renderOneChunkLevel2 = new PerformanceProfileProbe("FBORenderCell.renderOneChunkLevel2");
   public static final PerformanceProfileProbe translucentFloor = new PerformanceProfileProbe("FBORenderCell.translucentFloor");
   public static final PerformanceProfileProbe translucentNonFloor = new PerformanceProfileProbe("FBORenderCell.translucentNonFloor");
   public static final PerformanceProfileProbe updateLighting = new PerformanceProfileProbe("FBORenderCell.updateLighting");
   public static final PerformanceProfileProbe water = new PerformanceProfileProbe("FBORenderCell.water");
   public static final PerformanceProfileProbe tilesProbe = new PerformanceProfileProbe("renderTiles");
   public static final PerformanceProfileProbe itemsProbe = new PerformanceProfileProbe("renderItemsInWorld");
   public static final PerformanceProfileProbe movingObjectsProbe = new PerformanceProfileProbe("renderMovingObjects");
   public static final PerformanceProfileProbe shadowsProbe = new PerformanceProfileProbe("renderShadows");
   public static final PerformanceProfileProbe visibilityProbe = new PerformanceProfileProbe("VisibilityPolygon2");
   public static final PerformanceProfileProbe translucentFloorObjectsProbe = new PerformanceProfileProbe("renderTranslucentFloorObjects");
   public static final PerformanceProfileProbe translucentObjectsProbe = new PerformanceProfileProbe("renderTranslucentObjects");
   public static final PerformanceProfileProbe floorProbe = new PerformanceProfileProbe("renderFloor");
   public static final boolean FIX_CORPSE_CLIPPING = true;
   public static final boolean FIX_ITEM_CLIPPING = true;
   public static final boolean FIX_JUMBO_CLIPPING = true;
   private final ArrayList<IsoChunk> sortedChunks = new ArrayList();
   public static boolean OUTLINE_DOUBLEDOOR_FRAMES = true;
   public static IsoObject lowestCutawayObject = null;

   private FBORenderCell() {
      for(int var1 = 0; var1 < 4; ++var1) {
         this.perPlayerData[var1] = new PerPlayerData(var1);
      }

   }

   public void renderInternal() {
      int var1 = IsoCamera.frameState.playerIndex;
      int var2 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ);
      if (!PerformanceSettings.NewRoofHiding) {
         if (this.cell.bHideFloors[var1] && this.cell.unhideFloorsCounter[var1] > 0) {
            int var10002 = this.cell.unhideFloorsCounter[var1]--;
         }

         if (this.cell.unhideFloorsCounter[var1] <= 0) {
            this.cell.bHideFloors[var1] = false;
            this.cell.unhideFloorsCounter[var1] = 60;
         }
      }

      byte var3 = 0;
      byte var4 = 0;
      int var5 = var3 + IsoCamera.getOffscreenWidth(var1);
      int var6 = var4 + IsoCamera.getOffscreenHeight(var1);
      float var7 = IsoUtils.XToIso((float)var3, (float)var4, 0.0F);
      float var8 = IsoUtils.YToIso((float)var5, (float)var4, 0.0F);
      float var9 = IsoUtils.XToIso((float)var5, (float)var6, 6.0F);
      float var10 = IsoUtils.YToIso((float)var3, (float)var6, 6.0F);
      this.cell.minY = (int)var8;
      this.cell.maxY = (int)var10;
      this.cell.minX = (int)var7;
      this.cell.maxX = (int)var9;
      PerPlayerData var11 = this.perPlayerData[var1];
      var11.occludedGridX1 = this.cell.minX;
      var11.occludedGridY1 = this.cell.minY;
      var11.occludedGridX2 = this.cell.maxX;
      var11.occludedGridY2 = this.cell.maxY;
      IsoCell var10000 = this.cell;
      var10000.minX -= 2;
      var10000 = this.cell;
      var10000.minY -= 2;
      var10000 = this.cell;
      var10000.minX -= this.cell.minX % 8;
      var10000 = this.cell;
      var10000.minY -= this.cell.minY % 8;
      var10000 = this.cell;
      var10000.maxX += 8 - this.cell.maxX % 8;
      var10000 = this.cell;
      var10000.maxY += 8 - this.cell.maxY % 8;
      this.cell.maxZ = IsoCell.MaxHeight;
      IsoGameCharacter var12 = IsoCamera.getCameraCharacter();
      if (var12 == null) {
         this.cell.maxZ = 1;
      }

      if (IsoPlayer.getInstance().getZ() < 0.0F) {
         this.cell.maxZ = (int)Math.ceil((double)IsoPlayer.getInstance().getZ()) + 1;
      }

      if (this.cell.minX != this.cell.lastMinX || this.cell.minY != this.cell.lastMinY) {
         this.cell.lightUpdateCount = 10;
      }

      if (!PerformanceSettings.NewRoofHiding) {
         IsoGridSquare var13 = var12 == null ? null : var12.getCurrentSquare();
         if (var13 != null) {
            IsoGridSquare var14 = this.cell.getGridSquare(Math.round(var12.getX()), Math.round(var12.getY()), var2);
            if (var14 != null && this.cell.IsBehindStuff(var14)) {
               this.cell.bHideFloors[var1] = true;
            }

            if (!this.cell.bHideFloors[var1] && var13.getProperties().Is(IsoFlagType.hidewalls) || !var13.getProperties().Is(IsoFlagType.exterior)) {
               this.cell.bHideFloors[var1] = true;
            }
         }

         if (this.cell.bHideFloors[var1]) {
            this.cell.maxZ = var2 + 1;
         }
      }

      if (PerformanceSettings.LightingFrameSkip < 3) {
         this.cell.DrawStencilMask();
      }

      int var15;
      if (PerformanceSettings.LightingFrameSkip == 3) {
         int var20 = IsoCamera.getOffscreenWidth(var1) / 2;
         int var22 = IsoCamera.getOffscreenHeight(var1) / 2;
         var15 = 409;
         var20 -= var15 / (2 / Core.TileScale);
         var22 -= var15 / (2 / Core.TileScale);
         this.cell.StencilX1 = var20 - (int)IsoCamera.cameras[var1].RightClickX;
         this.cell.StencilY1 = var22 - (int)IsoCamera.cameras[var1].RightClickY;
         this.cell.StencilX2 = this.cell.StencilX1 + var15 * Core.TileScale;
         this.cell.StencilY2 = this.cell.StencilY1 + var15 * Core.TileScale;
      }

      long var21 = this.cell.playerWindowPeekingRoomId[var1];

      for(var15 = 0; var15 < IsoPlayer.numPlayers; ++var15) {
         this.cell.playerWindowPeekingRoomId[var15] = -1L;
         IsoPlayer var16 = IsoPlayer.players[var15];
         if (var16 != null) {
            IsoBuilding var17 = var16.getCurrentBuilding();
            if (var17 == null) {
               IsoDirections var18 = IsoDirections.fromAngle(var16.getForwardDirection());
               var17 = this.cell.GetPeekedInBuilding(var16.getCurrentSquare(), var18);
               if (var17 != null) {
                  this.cell.playerWindowPeekingRoomId[var15] = this.cell.playerPeekedRoomId;
               }
            }
         }
      }

      if (var21 != this.cell.playerWindowPeekingRoomId[var1]) {
         IsoPlayer.players[var1].dirtyRecalcGridStack = true;
      }

      if (var12 != null && var12.getCurrentSquare() != null && var12.getCurrentSquare().getProperties().Is(IsoFlagType.hidewalls)) {
         this.cell.maxZ = var2 + 1;
      }

      this.cell.bRendering = true;

      int var23;
      try {
         var15 = var2 < 0 ? var2 : IsoCell.getInstance().ChunkMap[var1].maxHeight;
         var23 = this.cell.ChunkMap[var1].minHeight;
         var23 = Math.max(var23, var2);
         this.RenderTiles(var23, var15);
      } catch (Exception var19) {
         this.cell.bRendering = false;
         ExceptionLogger.logException(var19);
      }

      this.cell.bRendering = false;
      if (IsoGridSquare.getRecalcLightTime() < 0.0F) {
         IsoGridSquare.setRecalcLightTime(60.0F);
      }

      if (IsoGridSquare.getLightcache() <= 0) {
         IsoGridSquare.setLightcache(90);
      }

      GameProfiler.ProfileArea var24 = GameProfiler.getInstance().startIfEnabled("renderLast");

      for(var23 = 0; var23 < this.cell.getObjectList().size(); ++var23) {
         IsoMovingObject var25 = (IsoMovingObject)this.cell.getObjectList().get(var23);
         var25.renderlast();
      }

      for(var23 = 0; var23 < this.cell.getStaticUpdaterObjectList().size(); ++var23) {
         IsoObject var26 = (IsoObject)this.cell.getStaticUpdaterObjectList().get(var23);
         var26.renderlast();
      }

      GameProfiler.getInstance().end(var24);
      IsoTree.checkChopTreeIndicators(var1);
      IsoTree.renderChopTreeIndicators();
      this.cell.lastMinX = this.cell.minX;
      this.cell.lastMinY = this.cell.minY;
      this.cell.DoBuilding(var1, true);
   }

   public void RenderTiles(int var1, int var2) {
      this.cell.minHeight = var1;
      IsoCell.s_performance.isoCellRenderTiles.invokeAndMeasure(this, var2, FBORenderCell::renderTilesInternal);
   }

   private void renderTilesInternal(int var1) {
      FBORenderChunkManager.instance.recycle();
      if (DebugOptions.instance.Terrain.RenderTiles.Enable.getValue()) {
         IsoCell var10000 = this.cell;
         if (IsoCell.m_floorRenderShader == null) {
            var10000 = this.cell;
            Objects.requireNonNull(var10000);
            RenderThread.invokeOnRenderContext(var10000::initTileShaders);
         }

         FBORenderLevels.bClearCachedSquares = true;
         int var2 = IsoCamera.frameState.playerIndex;
         IsoPlayer var3 = IsoPlayer.players[var2];
         PerPlayerData var4 = this.perPlayerData[var2];
         var3.dirtyRecalcGridStackTime -= GameTime.getInstance().getMultiplier() / 4.0F;
         IsoCell.PerPlayerRender var5 = this.cell.getPerPlayerRenderAt(var2);
         var5.setSize(this.cell.maxX - this.cell.minX + 1, this.cell.maxY - this.cell.minY + 1);
         this.currentTimeMillis = System.currentTimeMillis();
         if (this.cell.minX != var5.minX || this.cell.minY != var5.minY || this.cell.maxX != var5.maxX || this.cell.maxY != var5.maxY) {
            var5.minX = this.cell.minX;
            var5.minY = this.cell.minY;
            var5.maxX = this.cell.maxX;
            var5.maxY = this.cell.maxY;
         }

         int var6 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ);
         GameProfiler.getInstance().invokeAndMeasure("updateWeatherMask", var2, var6, this::updateWeatherMask);
         boolean var7 = false;
         if (var4.lastZ != var6) {
            if (var6 < 0 != var4.lastZ < 0) {
               var3.dirtyRecalcGridStack = true;
               this.invalidateAll(var2);
            } else if (var3.getBuilding() != null) {
               var3.getBuilding().getDef().invalidateOverlappedChunkLevelsAbove(var2, PZMath.min(var6, var4.lastZ), FBORenderChunk.DIRTY_CUTAWAYS);
            } else if (var3.isClimbing()) {
               var7 = true;
            }

            var4.lastZ = var6;
            this.checkSeenRooms(var3, var6);
         }

         int var8 = (int)Math.ceil((double)(IsoPuddles.getInstance().getPuddlesSizeFinalValue() * 500.0F));
         int var9 = (int)Math.ceil((double)(IsoPuddles.getInstance().getWetGroundFinalValue() * 500.0F));
         if (PerformanceSettings.PuddlesQuality == 2 && (this.puddlesValue != (float)var8 || this.wetGroundValue != (float)var9) && this.puddlesRedrawTimeMS + 1000L < this.currentTimeMillis) {
            this.puddlesValue = (float)var8;
            this.wetGroundValue = (float)var9;
            this.puddlesRedrawTimeMS = this.currentTimeMillis;
            this.invalidateAll(var2);
         }

         if (SandboxOptions.instance.EnableSnowOnGround.getValue() && this.snowFracTarget != (float)this.cell.getSnowTarget()) {
            this.snowFracTarget = (float)this.cell.getSnowTarget();
            this.invalidateAll(var2);
         }

         CompletableFuture var10 = null;
         if (DebugOptions.instance.ThreadGridStacks.getValue()) {
            var10 = CompletableFuture.supplyAsync(() -> {
               return this.recalculateGridStacks(var3, var2);
            }, PZForkJoinPool.commonPool());
         }

         GameProfiler.ProfileArea var11 = GameProfiler.getInstance().startIfEnabled("runChecks");
         var7 |= this.runChecks(var2);
         GameProfiler.getInstance().end(var11);
         IsoCell.s_performance.renderTiles.recalculateAnyGridStacks.start();
         if (var10 != null) {
            var7 |= (Boolean)var10.join();
         } else {
            var7 |= this.recalculateGridStacks(var3, var2);
         }

         IsoCell.s_performance.renderTiles.recalculateAnyGridStacks.end();

         int var12;
         for(var12 = 0; var12 < 8; ++var12) {
            var7 |= this.checkDebugKeys(var2, var12);
         }

         var7 |= this.checkDebugKeys(var2, var6);
         if (var7) {
            FBORenderCutaways.getInstance().squareChanged((IsoGridSquare)null);
         }

         cutaways.start();
         var7 |= FBORenderCutaways.getInstance().checkPlayerRoom(var2);
         var7 |= this.cell.SetCutawayRoomsForPlayer();
         var7 |= FBORenderCutaways.getInstance().checkExteriorWalls(var4.onScreenChunks);
         var7 |= FBORenderCutaways.getInstance().checkSlopedSurfaces(var4.onScreenChunks);
         if (var7) {
            FBORenderCutaways.getInstance().squareChanged((IsoGridSquare)null);
         }

         var7 |= FBORenderCutaways.getInstance().checkOccludedRooms(var2, var4.onScreenChunks);
         this.prepareChunksForUpdating(var2);
         if (var7) {
            FBORenderCutaways.getInstance().doCutawayVisitSquares(var2, var4.onScreenChunks);
         }

         cutaways.end();
         var4.bOcclusionChanged = false;
         if (FBORenderOcclusion.getInstance().bEnabled && this.hasAnyDirtyChunkTextures(var2)) {
            var4.bOcclusionChanged = true;
            var12 = (var4.occludedGridX2 - var4.occludedGridX1 + 1) * (var4.occludedGridY2 - var4.occludedGridY1 + 1);
            if (var4.occludedGrid == null || var4.occludedGrid.length < var12) {
               var4.occludedGrid = new int[var12];
            }

            Arrays.fill(var4.occludedGrid, -32);
            this.calculateOccludingSquares(var2);
            FBORenderOcclusion.getInstance().occludedGrid = var4.occludedGrid;
            FBORenderOcclusion.getInstance().occludedGridX1 = var4.occludedGridX1;
            FBORenderOcclusion.getInstance().occludedGridY1 = var4.occludedGridY1;
            FBORenderOcclusion.getInstance().occludedGridX2 = var4.occludedGridX2;
            FBORenderOcclusion.getInstance().occludedGridY2 = var4.occludedGridY2;
         }

         updateLighting.invokeAndMeasure(this, var2, FBORenderCell::updateChunkLighting);
         FBORenderLevels.bClearCachedSquares = false;
         IsoCell.s_performance.renderTiles.performRenderTiles.invokeAndMeasure(this, var5, var2, this.currentTimeMillis, FBORenderCell::performRenderTiles);
         FBORenderLevels.bClearCachedSquares = true;
         this.cell.playerCutawaysDirty[var2] = false;
         IsoCell.ShadowSquares.clear();
         IsoCell.MinusFloorCharacters.clear();
         IsoCell.ShadedFloor.clear();
         IsoCell.SolidFloor.clear();
         IsoCell.VegetationCorpses.clear();
         IsoCell.s_performance.renderTiles.renderDebugPhysics.invokeAndMeasure(this.cell, var2, IsoCell::renderDebugPhysics);
         IsoCell.s_performance.renderTiles.renderDebugLighting.invokeAndMeasure(this.cell, var5, var1, IsoCell::renderDebugLighting);
         FMODAmbientWalls.getInstance().render();
      }
   }

   private boolean recalculateGridStacks(IsoPlayer var1, int var2) {
      boolean var3 = false;
      FBORenderCutaways.getInstance().CalculatePointsOfInterest();
      var3 |= FBORenderCutaways.getInstance().CalculateBuildingsToCollapse();
      FBORenderCutaways.getInstance().checkHiddenBuildingLevels();
      var3 |= var1.dirtyRecalcGridStack;
      this.recalculateAnyGridStacks(var2);
      return var3;
   }

   private void updateWeatherMask(int var1, int var2) {
      if (WeatherFxMask.checkVisibleSquares(var1, var2)) {
         WeatherFxMask.forceMaskUpdate(var1);
         WeatherFxMask.initMask();
      }

   }

   private boolean runChecks(int var1) {
      this.checkWindEffectsOption(var1);
      GameProfiler.ProfileArea var2 = GameProfiler.getInstance().startIfEnabled("Newly");
      boolean var3 = this.checkNewlyOnScreenChunks(var1);
      GameProfiler.getInstance().end(var2);
      GameProfiler.ProfileArea var4 = GameProfiler.getInstance().startIfEnabled("Obscuring");
      this.checkObjectsObscuringPlayer(var1);
      this.checkFadingInObjectsObscuringPlayer(var1);
      GameProfiler.getInstance().end(var4);
      GameProfiler.ProfileArea var5 = GameProfiler.getInstance().startIfEnabled("Chunks");
      this.checkChunksWithTrees(var1);
      this.checkSeamChunks(var1);
      GameProfiler.getInstance().end(var5);
      this.checkMannequinRenderDirection(var1);
      this.checkPuddlesQualityOption(var1);
      return var3;
   }

   private void invalidateAll(int var1) {
      IsoChunkMap var2 = this.cell.ChunkMap[var1];

      for(int var3 = 0; var3 < IsoChunkMap.ChunkGridWidth; ++var3) {
         for(int var4 = 0; var4 < IsoChunkMap.ChunkGridWidth; ++var4) {
            IsoChunk var5 = var2.getChunk(var3, var4);
            if (var5 != null && !var5.bLightingNeverDone[var1]) {
               FBORenderLevels var6 = var5.getRenderLevels(var1);
               var6.invalidateAll(FBORenderChunk.DIRTY_CUTAWAYS);
            }
         }
      }

   }

   private void checkObjectsObscuringPlayer(int var1) {
      this.calculatePlayerRenderBounds(var1);
      this.calculateObjectsObscuringPlayer(var1, this.tempLocations);
      PerPlayerData var2 = this.perPlayerData[var1];
      if (this.tempLocations.equals(var2.squaresObscuringPlayer)) {
         this.locationPool.releaseAll(this.tempLocations);
         this.tempLocations.clear();
      } else {
         IsoChunkMap var3 = this.cell.getChunkMap(var1);

         int var4;
         IsoGameCharacter.Location var5;
         IsoGridSquare var6;
         for(var4 = 0; var4 < var2.squaresObscuringPlayer.size(); ++var4) {
            var5 = (IsoGameCharacter.Location)var2.squaresObscuringPlayer.get(var4);
            if (!this.listContainsLocation(this.tempLocations, var5)) {
               var6 = var3.getGridSquare(var5.x, var5.y, var5.z);
               if (var6 != null) {
                  var6.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBSCURING);
                  this.invalidateChunkLevelForRenderSquare(var6);
               }
            }
         }

         this.locationPool.releaseAll(var2.squaresObscuringPlayer);
         var2.squaresObscuringPlayer.clear();
         var2.squaresObscuringPlayer.addAll(this.tempLocations);

         for(var4 = 0; var4 < var2.squaresObscuringPlayer.size(); ++var4) {
            var5 = (IsoGameCharacter.Location)var2.squaresObscuringPlayer.get(var4);
            var6 = var3.getGridSquare(var5.x, var5.y, var5.z);
            if (var6 != null) {
               var6.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBSCURING);
               this.invalidateChunkLevelForRenderSquare(var6);
            }
         }

         this.tempLocations.clear();
      }
   }

   private void calculatePlayerRenderBounds(int var1) {
      PerPlayerData var2 = this.perPlayerData[var1];
      float var3 = IsoCamera.frameState.CamCharacterX;
      float var4 = IsoCamera.frameState.CamCharacterY;
      float var5 = IsoCamera.frameState.CamCharacterZ;
      var2.playerBoundsX = IsoUtils.XToScreen(var3, var4, var5, 0);
      var2.playerBoundsY = IsoUtils.YToScreen(var3, var4, var5, 0);
      var2.playerBoundsX -= (float)(32 * Core.TileScale);
      var2.playerBoundsY -= (float)(112 * Core.TileScale);
      var2.playerBoundsW = (float)(64 * Core.TileScale);
      var2.playerBoundsH = (float)(128 * Core.TileScale);
   }

   private boolean isPotentiallyObscuringObject(IsoObject var1) {
      if (var1 == null) {
         return false;
      } else {
         IsoSprite var2 = var1.getSprite();
         if (var2 == null) {
            return false;
         } else {
            IsoGameCharacter var3 = IsoCamera.frameState.CamCharacter;
            if (var3 != null && var3.isSittingOnFurniture() && var3.isSitOnFurnitureObject(var1)) {
               return false;
            } else if (var3 != null && var3.isOnBed() && var1 == var3.getBed()) {
               return false;
            } else if (var2.getProperties().Is(IsoFlagType.water)) {
               return false;
            } else if (!var2.getProperties().Is(IsoFlagType.attachedSurface) || !var1.square.Is(IsoFlagType.solid) && !var1.square.Is(IsoFlagType.solidtrans)) {
               if (!var2.getProperties().Is(IsoFlagType.attachedE) && !var2.getProperties().Is(IsoFlagType.attachedS) && !var2.getProperties().Is(IsoFlagType.attachedCeiling)) {
                  if (var1.isStairsNorth()) {
                     if (IsoCamera.frameState.CamCharacterSquare != null && IsoCamera.frameState.CamCharacterSquare.HasStairs()) {
                        return false;
                     } else {
                        return var1.getX() > (float)PZMath.fastfloor(IsoCamera.frameState.CamCharacterX);
                     }
                  } else if (var1.isStairsWest()) {
                     if (IsoCamera.frameState.CamCharacterSquare != null && IsoCamera.frameState.CamCharacterSquare.HasStairs()) {
                        return false;
                     } else {
                        return var1.getY() > (float)PZMath.fastfloor(IsoCamera.frameState.CamCharacterY);
                     }
                  } else {
                     return var2.solid || var2.solidTrans;
                  }
               } else {
                  return true;
               }
            } else {
               return true;
            }
         }
      }
   }

   private void calculateObjectsObscuringPlayer(int var1, ArrayList<IsoGameCharacter.Location> var2) {
      this.locationPool.releaseAll(var2);
      var2.clear();
      IsoPlayer var3 = IsoPlayer.players[var1];
      if (var3 != null && var3.getCurrentSquare() != null) {
         IsoChunkMap var4 = this.cell.getChunkMap(var1);
         int var5 = var3.getCurrentSquare().getX();
         int var6 = var3.getCurrentSquare().getY();
         int var7 = var3.getCurrentSquare().getZ();
         int var8 = var5 - 1;
         int var9 = var6;
         int var10 = var5;
         int var11 = var6 - 1;
         this.testSquareObscuringPlayer(var1, var5, var6, var7, var2);

         int var12;
         for(var12 = 1; var12 <= 3; ++var12) {
            this.testSquareObscuringPlayer(var1, var5 + var12, var6 + var12, var7, var2);
            this.testSquareObscuringPlayer(var1, var8 + var12, var9 + var12, var7, var2);
            this.testSquareObscuringPlayer(var1, var10 + var12, var11 + var12, var7, var2);
            this.testSquareObscuringPlayer(var1, var5 - 1 + var12, var6 + 1 + var12, var7, var2);
            this.testSquareObscuringPlayer(var1, var5 + 1 + var12, var6 - 1 + var12, var7, var2);
         }

         for(var12 = 0; var12 < var2.size(); ++var12) {
            IsoGameCharacter.Location var13 = (IsoGameCharacter.Location)var2.get(var12);
            IsoGridSquare var14 = var4.getGridSquare(var13.x, var13.y, var13.z);
            if (var14 != null) {
               for(int var15 = 0; var15 < var14.getObjects().size(); ++var15) {
                  IsoObject var16 = (IsoObject)var14.getObjects().get(var15);
                  if (this.isPotentiallyObscuringObject(var16)) {
                     this.addObscuringStairObjects(var2, var14, var16);
                     IsoSprite var17 = var16.getSprite();
                     if (var17.getSpriteGrid() != null) {
                        IsoSpriteGrid var18 = var17.getSpriteGrid();
                        int var19 = var18.getSpriteGridPosX(var17);
                        int var20 = var18.getSpriteGridPosY(var17);
                        int var21 = var18.getSpriteGridPosZ(var17);

                        for(int var22 = 0; var22 < var18.getLevels(); ++var22) {
                           for(int var23 = 0; var23 < var18.getHeight(); ++var23) {
                              for(int var24 = 0; var24 < var18.getWidth(); ++var24) {
                                 if (var18.getSprite(var24, var23) != null) {
                                    int var25 = var14.x - var19 + var24;
                                    int var26 = var14.y - var20 + var23;
                                    int var27 = var14.z - var21 + var22;
                                    if (var4.getGridSquare(var25, var26, var27) != null && !this.listContainsLocation(var2, var25, var26, var27)) {
                                       IsoGameCharacter.Location var28 = (IsoGameCharacter.Location)this.locationPool.alloc();
                                       var28.set(var25, var26, var27);
                                       var2.add(var28);
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

         PerPlayerData var29 = this.perPlayerData[var1];

         for(int var30 = 0; var30 < var29.squaresObscuringPlayer.size(); ++var30) {
            IsoGameCharacter.Location var31 = (IsoGameCharacter.Location)var29.squaresObscuringPlayer.get(var30);
            if (!this.listContainsLocation(var2, var31) && !this.listContainsLocation(var29.fadingInSquares, var31)) {
               IsoGridSquare var32 = var4.getGridSquare(var31.x, var31.y, var31.z);
               if (this.squareHasFadingInObjects(var1, var32)) {
                  IsoGameCharacter.Location var33 = (IsoGameCharacter.Location)this.locationPool.alloc();
                  var33.set(var32.x, var32.y, var32.z);
                  var29.fadingInSquares.add(var33);
               }
            }
         }

      }
   }

   private void addObscuringStairObjects(ArrayList<IsoGameCharacter.Location> var1, IsoGridSquare var2, IsoObject var3) {
      byte var4;
      byte var5;
      int var6;
      IsoGridSquare var7;
      IsoGameCharacter.Location var8;
      if (var3.isStairsNorth()) {
         var4 = 0;
         var5 = 0;
         if (var3.getType() == IsoObjectType.stairsTN) {
            var5 = 2;
         }

         if (var3.getType() == IsoObjectType.stairsMN) {
            var4 = -1;
            var5 = 1;
         }

         if (var3.getType() == IsoObjectType.stairsBN) {
            var4 = -2;
            var5 = 0;
         }

         if (var4 < var5) {
            for(var6 = var4; var6 <= var5; ++var6) {
               var7 = IsoWorld.instance.CurrentCell.getGridSquare(var2.x, var2.y + var6, var2.z);
               if (var7 != null && !this.listContainsLocation(var1, var2.x, var2.y + var6, var2.z)) {
                  var8 = (IsoGameCharacter.Location)this.locationPool.alloc();
                  var8.set(var2.x, var2.y + var6, var2.z);
                  var1.add(var8);
               }
            }
         }
      }

      if (var3.isStairsWest()) {
         var4 = 0;
         var5 = 0;
         if (var3.getType() == IsoObjectType.stairsTW) {
            var5 = 2;
         }

         if (var3.getType() == IsoObjectType.stairsMW) {
            var4 = -1;
            var5 = 1;
         }

         if (var3.getType() == IsoObjectType.stairsBW) {
            var4 = -2;
            var5 = 0;
         }

         if (var4 < var5) {
            for(var6 = var4; var6 <= var5; ++var6) {
               var7 = IsoWorld.instance.CurrentCell.getGridSquare(var2.x + var6, var2.y, var2.z);
               if (var7 != null && !this.listContainsLocation(var1, var2.x + var6, var2.y, var2.z)) {
                  var8 = (IsoGameCharacter.Location)this.locationPool.alloc();
                  var8.set(var2.x + var6, var2.y, var2.z);
                  var1.add(var8);
               }
            }
         }
      }

   }

   private void checkFadingInObjectsObscuringPlayer(int var1) {
      IsoChunkMap var2 = this.cell.getChunkMap(var1);
      PerPlayerData var3 = this.perPlayerData[var1];

      for(int var4 = 0; var4 < var3.fadingInSquares.size(); ++var4) {
         IsoGameCharacter.Location var5 = (IsoGameCharacter.Location)var3.fadingInSquares.get(var4);
         IsoGridSquare var6 = var2.getGridSquare(var5.x, var5.y, var5.z);
         if (var6 != null && !this.squareHasFadingInObjects(var1, var6)) {
            var6.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBSCURING);
            this.invalidateChunkLevelForRenderSquare(var6);
            var3.fadingInSquares.remove(var4--);
            this.locationPool.release((Object)var5);
         }
      }

   }

   private boolean squareHasFadingInObjects(int var1, IsoGridSquare var2) {
      if (var2 == null) {
         return false;
      } else {
         for(int var3 = 0; var3 < var2.getObjects().size(); ++var3) {
            IsoObject var4 = (IsoObject)var2.getObjects().get(var3);
            if (this.isPotentiallyObscuringObject(var4) && var4.getAlpha(var1) < 1.0F) {
               return true;
            }
         }

         return false;
      }
   }

   private void invalidateChunkLevelForRenderSquare(IsoGridSquare var1) {
      if (!var1.getWorldObjects().isEmpty()) {
         byte var2 = 8;
         IsoGridSquare var3;
         if (PZMath.coordmodulo(var1.x, var2) == 0 && PZMath.coordmodulo(var1.y, var2) == var2 - 1) {
            var3 = var1.getAdjacentSquare(IsoDirections.S);
            if (var3 != null) {
               var3.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBSCURING);
            }
         }

         if (PZMath.coordmodulo(var1.x, var2) == var2 - 1 && PZMath.coordmodulo(var1.y, var2) == 0) {
            var3 = var1.getAdjacentSquare(IsoDirections.E);
            if (var3 != null) {
               var3.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBSCURING);
            }
         }

      }
   }

   private boolean listContainsLocation(ArrayList<IsoGameCharacter.Location> var1, IsoGameCharacter.Location var2) {
      return this.listContainsLocation(var1, var2.x, var2.y, var2.z);
   }

   private boolean listContainsLocation(ArrayList<IsoGameCharacter.Location> var1, int var2, int var3, int var4) {
      for(int var5 = 0; var5 < var1.size(); ++var5) {
         if (((IsoGameCharacter.Location)var1.get(var5)).equals(var2, var3, var4)) {
            return true;
         }
      }

      return false;
   }

   private void testSquareObscuringPlayer(int var1, int var2, int var3, int var4, ArrayList<IsoGameCharacter.Location> var5) {
      IsoChunkMap var6 = this.cell.getChunkMap(var1);
      IsoGridSquare var7 = var6.getGridSquare(var2, var3, var4);
      if (this.isSquareObscuringPlayer(var1, var7)) {
         if (this.listContainsLocation(var5, var7.x, var7.y, var7.z)) {
            return;
         }

         IsoGameCharacter.Location var8 = (IsoGameCharacter.Location)this.locationPool.alloc();
         var8.set(var7.x, var7.y, var7.z);
         var5.add(var8);
      }

   }

   private boolean isSquareObscuringPlayer(int var1, IsoGridSquare var2) {
      if (var2 == null) {
         return false;
      } else {
         PerPlayerData var3 = this.perPlayerData[var1];
         if (!var2.Is(IsoFlagType.attachedE) && !var2.Is(IsoFlagType.attachedS) && !var2.Is(IsoFlagType.attachedCeiling) && !var2.HasStairs() && !var2.Is(IsoFlagType.solid) && !var2.Is(IsoFlagType.solidtrans)) {
            return false;
         } else {
            for(int var4 = 0; var4 < var2.getObjects().size(); ++var4) {
               IsoObject var5 = (IsoObject)var2.getObjects().get(var4);
               if (this.isPotentiallyObscuringObject(var5)) {
                  Texture var6 = var5.sprite.getTextureForCurrentFrame(var5.dir);
                  if (var6 != null && var3.isObjectObscuringPlayer(var2, var6, var5.offsetX, var5.offsetY + var5.getRenderYOffset() * (float)Core.TileScale)) {
                     return true;
                  }
               }
            }

            return false;
         }
      }
   }

   private void checkChunksWithTrees(int var1) {
      PerPlayerData var2 = this.perPlayerData[var1];

      for(int var3 = 0; var3 < var2.onScreenChunks.size(); ++var3) {
         IsoChunk var4 = (IsoChunk)var2.onScreenChunks.get(var3);
         if (0 >= var4.minLevel && 0 <= var4.maxLevel) {
            FBORenderLevels var5 = var4.getRenderLevels(var1);
            if (var5.isOnScreen(0)) {
               boolean var6 = var5.calculateInStencilRect(0);
               if (!var6 && var5.m_bInStencilRect) {
                  var5.m_bInStencilRect = false;
                  var5.invalidateLevel(0, FBORenderChunk.DIRTY_TREES);
               } else {
                  var5.m_bInStencilRect = var6;
                  if (this.checkTreeTranslucency(var1, var5)) {
                     var5.invalidateLevel(0, FBORenderChunk.DIRTY_TREES);
                  }
               }
            }
         }
      }

   }

   private void checkSeamChunks(int var1) {
      IsoChunkMap var10000 = this.cell.ChunkMap[var1];
      PerPlayerData var3 = this.perPlayerData[var1];

      for(int var4 = 0; var4 < var3.onScreenChunks.size(); ++var4) {
         IsoChunk var5 = (IsoChunk)var3.onScreenChunks.get(var4);
         FBORenderLevels var6 = var5.getRenderLevels(var1);
         if (var6.m_adjacentChunkLoadedCounter != var5.m_adjacentChunkLoadedCounter) {
            var6.m_adjacentChunkLoadedCounter = var5.m_adjacentChunkLoadedCounter;
            var6.invalidateAll(FBORenderChunk.DIRTY_REDRAW);
         }
      }

   }

   private boolean checkTreeTranslucency(int var1, FBORenderLevels var2) {
      if (Core.getInstance().getOptionDoWindSpriteEffects()) {
         return false;
      } else {
         float var3 = Core.getInstance().getZoom(var1);
         if (var2.isDirty(0, var3)) {
            return false;
         } else {
            ArrayList var4 = var2.m_treeSquares;
            boolean var5 = false;

            for(int var6 = 0; var6 < var4.size(); ++var6) {
               IsoGridSquare var7 = (IsoGridSquare)var4.get(var6);
               if (var7.chunk != null) {
                  IsoTree var8 = var7.getTree();
                  if (var8 != null) {
                     boolean var9 = false;
                     if (var8.fadeAlpha < 1.0F != var8.bWasFaded) {
                        var8.bWasFaded = var8.fadeAlpha < 1.0F;
                        var5 = true;
                        var9 = true;
                     }

                     if (this.isTranslucentTree(var8) != var8.bRenderFlag) {
                        var8.bRenderFlag = !var8.bRenderFlag;
                        var5 = true;
                        var9 = true;
                     }

                     if (var9) {
                        IsoGridSquare var10 = var8.getRenderSquare();
                        if (var10 != null && var8.getSquare() != var10) {
                           var10.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_TREES);
                        }
                     }
                  }
               }
            }

            return var5;
         }
      }
   }

   private void checkWindEffectsOption(int var1) {
      if (this.bWindEffects != Core.getInstance().getOptionDoWindSpriteEffects()) {
         this.bWindEffects = Core.getInstance().getOptionDoWindSpriteEffects();
         PerPlayerData var2 = this.perPlayerData[var1];

         for(int var3 = 0; var3 < var2.onScreenChunks.size(); ++var3) {
            IsoChunk var4 = (IsoChunk)var2.onScreenChunks.get(var3);
            if (0 >= var4.minLevel && 0 <= var4.maxLevel) {
               FBORenderLevels var5 = var4.getRenderLevels(var1);
               if (var5.calculateOnScreen(0)) {
                  var5.invalidateLevel(0, FBORenderChunk.DIRTY_TREES);
               }
            }
         }

      }
   }

   private void checkPuddlesQualityOption(int var1) {
      if (this.puddlesQuality == 2 != (PerformanceSettings.PuddlesQuality == 2)) {
         this.puddlesQuality = PerformanceSettings.PuddlesQuality;
         IsoChunkMap var2 = IsoWorld.instance.CurrentCell.ChunkMap[var1];

         for(int var3 = 0; var3 < IsoChunkMap.ChunkGridWidth; ++var3) {
            for(int var4 = 0; var4 < IsoChunkMap.ChunkGridWidth; ++var4) {
               IsoChunk var5 = var2.getChunk(var4, var3);
               if (var5 != null) {
                  for(int var6 = var5.minLevel; var6 <= var5.maxLevel; ++var6) {
                     IsoGridSquare[] var7 = var5.squares[var5.squaresIndexOfLevel(var6)];

                     for(int var8 = 0; var8 < var7.length; ++var8) {
                        IsoGridSquare var9 = var7[var8];
                        if (var9 != null) {
                           IsoPuddlesGeometry var10 = var9.getPuddles();
                           if (var10 != null) {
                              var10.init(var9);
                           }
                        }
                     }
                  }
               }
            }
         }

         this.invalidateAll(var1);
      }
   }

   private boolean checkNewlyOnScreenChunks(int var1) {
      boolean var2 = false;
      float var3 = Core.getInstance().getZoom(var1);
      PerPlayerData var4 = this.perPlayerData[var1];
      var4.onScreenChunks.clear();
      var4.chunksWith_AnimatedAttachments.clear();
      var4.chunksWith_Flies.clear();
      IsoChunkMap var5 = this.cell.ChunkMap[var1];

      for(int var6 = 0; var6 < IsoChunkMap.ChunkGridWidth; ++var6) {
         for(int var7 = 0; var7 < IsoChunkMap.ChunkGridWidth; ++var7) {
            IsoChunk var8 = var5.getChunk(var6, var7);
            if (var8 != null && !var8.bLightingNeverDone[var1]) {
               FBORenderLevels var9 = var8.getRenderLevels(var1);
               int var10;
               if (!var8.IsOnScreen(true)) {
                  for(var10 = var8.minLevel; var10 <= var8.maxLevel; ++var10) {
                     var9.setOnScreen(var10, false);
                     var9.freeFBOsForLevel(var10);
                  }
               } else {
                  var4.onScreenChunks.add(var8);
                  if (var9.m_prevMinZ != var8.minLevel || var9.m_prevMaxZ != var8.maxLevel) {
                     for(var10 = var8.minLevel; var10 <= var8.maxLevel; ++var10) {
                        var9.invalidateLevel(var10, FBORenderChunk.DIRTY_OBJECT_ADD);
                     }
                  }

                  for(var10 = var8.minLevel; var10 <= var8.maxLevel; ++var10) {
                     if (var10 == var9.getMinLevel(var10)) {
                        boolean var11 = var9.isOnScreen(var10);
                        boolean var12 = var9.calculateOnScreen(var10);
                        if (var12 && var9.getCachedSquares_Flies(var10).size() > 0) {
                           var4.addChunkWith_Flies(var8);
                        }

                        if (var11 != var12) {
                           if (var12) {
                              var9.setOnScreen(var10, true);
                              var9.invalidateLevel(var10, FBORenderChunk.DIRTY_REDRAW);
                              if (var9.isDirty(var10, FBORenderChunk.DIRTY_REDO_CUTAWAYS, var3)) {
                                 var2 = true;
                              }
                           } else {
                              var9.setOnScreen(var10, false);
                              var9.freeFBOsForLevel(var10);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      FBORenderChunkManager.instance.recycle();
      return var2;
   }

   private void performRenderTiles(IsoCell.PerPlayerRender var1, int var2, long var3) {
      Object var5 = null;
      Object var6 = null;
      this.bRenderAnimatedAttachments = false;
      this.bRenderTranslucentOnly = false;
      FBORenderChunkManager.instance.startFrame();
      IsoPuddles.getInstance().clearThreadData();
      this.m_bInvalidateDelayedLoadingLevels = false;
      if (this.delayedLoadingTimerMS != 0L && this.delayedLoadingTimerMS <= var3) {
         this.delayedLoadingTimerMS = 0L;
         this.m_bInvalidateDelayedLoadingLevels = true;
      }

      SpriteRenderer.instance.beginProfile(tilesProbe);
      PerPlayerData var7 = this.perPlayerData[var2];
      var7.chunksWith_TranslucentFloor.clear();
      var7.chunksWith_TranslucentNonFloor.clear();

      int var8;
      for(var8 = 0; var8 < var7.onScreenChunks.size(); ++var8) {
         IsoChunk var9 = (IsoChunk)var7.onScreenChunks.get(var8);
         renderOneChunk.start();
         this.renderOneChunk(var9, var1, var2, var3, (Shader)var5, (Shader)var6);
         renderOneChunk.end();
      }

      SpriteRenderer.instance.endProfile(tilesProbe);
      FBORenderCorpses.getInstance().update();
      FBORenderItems.getInstance().update();
      FBORenderChunkManager.instance.endFrame();
      FBORenderShadows.getInstance().clear();
      IsoPlayer var11;
      if (GameClient.bClient) {
         Iterator var10 = GameClient.IDToPlayerMap.values().iterator();

         while(var10.hasNext()) {
            var11 = (IsoPlayer)var10.next();
            if (this.cell.getObjectList().contains(var11)) {
               if (var11.getCurrentSquare() != null) {
                  if (DebugOptions.instance.Terrain.RenderTiles.Shadows.getValue()) {
                     var11.renderShadow(var11.getX(), var11.getY(), var11.getZ());
                  }

                  var11.render(var11.getX(), var11.getY(), var11.getZ(), var11.getCurrentSquare().getLightInfo(IsoPlayer.getPlayerIndex()), true, false, (Shader)null);
               }

               this.debugChunkStateRenderPlayer(var11);
            }
         }
      } else {
         for(var8 = 0; var8 < IsoPlayer.numPlayers; ++var8) {
            var11 = IsoPlayer.players[var8];
            if (var11 != null && this.cell.getObjectList().contains(var11)) {
               if (var11.getCurrentSquare() != null) {
                  if (DebugOptions.instance.Terrain.RenderTiles.Shadows.getValue()) {
                     var11.renderShadow(var11.getX(), var11.getY(), var11.getZ());
                  }

                  var11.render(var11.getX(), var11.getY(), var11.getZ(), var11.getCurrentSquare().getLightInfo(IsoPlayer.getPlayerIndex()), true, false, (Shader)null);
               }

               this.debugChunkStateRenderPlayer(var11);
            }
         }
      }

      this.renderCorpseShadows(var2);
      this.renderMannequinShadows(var2);
      if (!DebugOptions.instance.FBORenderChunk.CorpsesInChunkTexture.getValue()) {
         this.renderCorpsesInWorld(var2);
      }

      if (!DebugOptions.instance.FBORenderChunk.ItemsInChunkTexture.getValue()) {
         SpriteRenderer.instance.beginProfile(itemsProbe);
         this.renderItemsInWorld(var2);
         SpriteRenderer.instance.endProfile(itemsProbe);
      }

      if (PerformanceSettings.PuddlesQuality < 2) {
         puddles.invokeAndMeasure(this, var2, FBORenderCell::renderPuddles);
      }

      this.renderOpaqueObjectsEvent(var2);
      SpriteRenderer.instance.beginProfile(movingObjectsProbe);
      this.renderMovingObjects();
      SpriteRenderer.instance.endProfile(movingObjectsProbe);
      water.invokeAndMeasure(this, var2, FBORenderCell::renderWater);
      this.renderAnimatedAttachments(var2);
      this.renderFlies(var2);
      FBORenderObjectHighlight.getInstance().render(var2);
      SpriteRenderer.instance.beginProfile(translucentFloorObjectsProbe);
      translucentFloor.invokeAndMeasure(this, var2, var5, var6, var3, FBORenderCell::renderTranslucentFloorObjects);
      SpriteRenderer.instance.endProfile(translucentFloorObjectsProbe);
      puddles.invokeAndMeasure(this, var2, FBORenderCell::renderPuddlesTranslucentFloorsOnly);
      this.renderWaterShore(var2);
      this.renderRainSplashes(var2);
      SpriteRenderer.instance.beginProfile(shadowsProbe);
      FBORenderShadows.getInstance().renderMain();
      SpriteRenderer.instance.endProfile(shadowsProbe);
      SpriteRenderer.instance.beginProfile(visibilityProbe);
      GameProfiler var10000 = GameProfiler.getInstance();
      Integer var10002 = var2;
      VisibilityPolygon2 var10003 = VisibilityPolygon2.getInstance();
      Objects.requireNonNull(var10003);
      var10000.invokeAndMeasure("Visibility", var10002, var10003::renderMain);
      SpriteRenderer.instance.endProfile(visibilityProbe);
      WorldMarkers.instance.renderGridSquareMarkers();
      this.bRenderTranslucentOnly = true;
      IsoMarkers.instance.renderIsoMarkers(var1, 0, var2);
      this.bRenderTranslucentOnly = false;
      SpriteRenderer.instance.beginProfile(translucentObjectsProbe);
      translucentNonFloor.invokeAndMeasure(this, var2, var5, var6, var3, FBORenderCell::renderTranslucentObjects);
      SpriteRenderer.instance.endProfile(translucentObjectsProbe);
      fog.invokeAndMeasure(this, var2, FBORenderCell::renderFog);
      FBORenderObjectOutline.getInstance().render(var2);
      FBORenderTracerEffects.getInstance().render();
   }

   private void renderOneChunk(IsoChunk var1, IsoCell.PerPlayerRender var2, int var3, long var4, Shader var6, Shader var7) {
      if (var1 != null && var1.IsOnScreen(true)) {
         if (!var1.bLightingNeverDone[var3]) {
            FBORenderLevels var8 = var1.getRenderLevels(var3);
            var8.m_prevMinZ = var1.minLevel;
            var8.m_prevMaxZ = var1.maxLevel;

            for(int var9 = var1.minLevel; var9 <= var1.maxLevel; ++var9) {
               renderOneChunkLevel.start();
               this.renderOneLevel(var1, var9, var2, var3, var4, var6, var7);
               renderOneChunkLevel.end();
               if (DebugOptions.instance.FBORenderChunk.RenderWallLines.getValue() && var9 == PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ)) {
                  var1.getCutawayData().debugRender(var9);
               }
            }

            IndieGL.glDepthMask(false);
            IndieGL.glDepthFunc(519);
         }
      }
   }

   private void renderOneLevel(IsoChunk var1, int var2, IsoCell.PerPlayerRender var3, int var4, long var5, Shader var7, Shader var8) {
      PerPlayerData var9 = this.perPlayerData[var4];
      FBORenderLevels var10 = var1.getRenderLevels(var4);
      if (!var10.isOnScreen(var2)) {
         var10.freeFBOsForLevel(var2);
      } else {
         float var11 = Core.getInstance().getZoom(var4);
         if (this.m_bInvalidateDelayedLoadingLevels && var10.isDelayedLoading(var2) && var2 == var10.getMinLevel(var2)) {
            var10.invalidateLevel(var2, FBORenderChunk.DIRTY_REDRAW);
         }

         if (FBORenderOcclusion.getInstance().bEnabled) {
            if (var2 == var10.getMinLevel(var2) && var9.bOcclusionChanged) {
               var10.setRenderedSquaresCount(var2, this.calculateRenderedSquaresCount(var4, var1, var2));
            }

            if (var10.getRenderedSquaresCount(var2) == 0) {
               if (var2 == var10.getMaxLevel(var2)) {
                  var10.clearDirty(var2, var11);
                  if (var10.getFBOForLevel(var2, var11) != null) {
                     var10.freeFBOsForLevel(var2);
                     FBORenderChunkManager.instance.recycle();
                  }
               }

               return;
            }
         }

         int var12 = IsoWorld.instance.getFrameNo();
         boolean var13 = true;
         boolean var14 = true;
         boolean var15 = FBORenderChunkManager.instance.beginRenderChunkLevel(var1, var2, var11, var13, var14);
         if (DebugOptions.instance.DelayObjectRender.getValue()) {
            var13 = (long)var12 == var1.loadedFrame || (long)var12 >= var1.renderFrame;
            boolean var10000;
            if ((long)var12 != var1.loadedFrame && (long)var12 <= var1.renderFrame) {
               var10000 = false;
            } else {
               var10000 = true;
            }
         }

         if (var15 && var13) {
            boolean[][] var16 = var3.FlattenGrassEtc;
            IsoCell.ShadowSquares.clear();
            IsoCell.SolidFloor.clear();
            IsoCell.ShadedFloor.clear();
            IsoCell.VegetationCorpses.clear();
            IsoCell.MinusFloorCharacters.clear();
            calculateRenderInfo.start();
            if (var2 == var10.getMinLevel(var2)) {
               var10.clearCachedSquares(var2);
            }

            if (var2 == 0) {
               var10.m_treeSquares.clear();
               var10.m_waterSquares.clear();
               var10.m_waterShoreSquares.clear();
               var10.m_waterAttachSquares.clear();
            }

            FBORenderCutaways.ChunkLevelData var17 = var1.getCutawayDataForLevel(var2);
            IsoGridSquare[] var18 = var1.squares[var1.squaresIndexOfLevel(var2)];

            for(int var19 = 0; var19 < var18.length; ++var19) {
               IsoGridSquare var20 = var18[var19];
               if (var17.shouldRenderSquare(var4, var20) && !FBORenderOcclusion.getInstance().isOccluded(var20.x, var20.y, var20.z)) {
                  if (!var20.getObjects().isEmpty()) {
                     var20.bFlattenGrassEtc = false;
                     GameProfiler.getInstance().invokeAndMeasure("Calculate", var20, this::calculateObjectRenderInfo);
                     int var21 = 0;
                     boolean var22 = false;
                     boolean var23 = false;
                     boolean var24 = false;
                     boolean var25 = false;
                     boolean var26 = false;
                     IsoObject[] var27 = (IsoObject[])var20.getObjects().getElements();
                     int var28 = var20.getObjects().size();

                     for(int var29 = 0; var29 < var28; ++var29) {
                        IsoObject var30 = var27[var29];
                        ObjectRenderInfo var31 = var30.getRenderInfo(var4);
                        switch (var31.m_layer) {
                           case Floor:
                              var21 |= 1;
                              break;
                           case Vegetation:
                              var21 |= 2;
                              break;
                           case MinusFloor:
                              var21 |= 4;
                              break;
                           case MinusFloorSE:
                              var21 |= 4;
                              break;
                           case WorldInventoryObject:
                              var21 |= 4;
                              break;
                           case Translucent:
                              var23 = true;
                              break;
                           case TranslucentFloor:
                              var22 = true;
                        }

                        if (var31.m_layer == ObjectRenderLayer.None && var30.sprite != null && var30.sprite.getProperties().Is(IsoFlagType.water) && var30.getAttachedAnimSprite() != null && !var30.getAttachedAnimSprite().isEmpty()) {
                           var24 = true;
                        }

                        var25 |= var30.hasAnimatedAttachments();
                        if (!DebugOptions.instance.FBORenderChunk.ItemsInChunkTexture.getValue()) {
                           var26 |= var30 instanceof IsoWorldInventoryObject;
                        }
                     }

                     if (var25) {
                        var10.getCachedSquares_AnimatedAttachments(var2).add(var20);
                     }

                     if (!var20.getStaticMovingObjects().isEmpty()) {
                        var10.getCachedSquares_Corpses(var2).add(var20);
                     }

                     if (var20.hasFlies()) {
                        var10.getCachedSquares_Flies(var2).add(var20);
                     }

                     if (var26) {
                        var10.getCachedSquares_Items(var2).add(var20);
                     }

                     GameProfiler.ProfileArea var44 = GameProfiler.getInstance().startIfEnabled("Puddles");
                     if (var20.getPuddles() != null && var20.getPuddles().shouldRender()) {
                        var10.getCachedSquares_Puddles(var2).add(var20);
                     }

                     GameProfiler.getInstance().end(var44);
                     if (var22) {
                        var10.getCachedSquares_TranslucentFloor(var2).add(var20);
                     }

                     if (var23) {
                        var10.getCachedSquares_TranslucentNonFloor(var2).add(var20);
                     }

                     if (!var20.getStaticMovingObjects().isEmpty()) {
                        var21 |= 2;
                        var21 |= 16;
                        if (var20.HasStairs()) {
                           var21 |= 4;
                        }
                     }

                     if (!var20.getWorldObjects().isEmpty()) {
                        var21 |= 2;
                     }

                     if (!var20.getLocalTemporaryObjects().isEmpty()) {
                        var21 |= 4;
                     }

                     for(int var45 = 0; var45 < var20.getMovingObjects().size(); ++var45) {
                        IsoMovingObject var46 = (IsoMovingObject)var20.getMovingObjects().get(var45);
                        boolean var32 = var46.isOnFloor();
                        if (var32 && var46 instanceof IsoZombie) {
                           IsoZombie var33 = (IsoZombie)var46;
                           var32 = var33.isProne();
                           if (!BaseVehicle.RENDER_TO_TEXTURE) {
                              var32 = false;
                           }
                        }

                        if (var32) {
                           var21 |= 2;
                        } else {
                           var21 |= 4;
                        }

                        var21 |= 16;
                     }

                     if (var20.hasFlies()) {
                        var21 |= 4;
                     }

                     if ((var21 & 1) != 0) {
                        IsoCell.SolidFloor.add(var20);
                     }

                     if ((var21 & 8) != 0) {
                        IsoCell.ShadedFloor.add(var20);
                     }

                     if ((var21 & 2) != 0) {
                        IsoCell.VegetationCorpses.add(var20);
                     }

                     if ((var21 & 4) != 0) {
                        IsoCell.MinusFloorCharacters.add(var20);
                     }

                     if ((var21 & 16) != 0) {
                        IsoCell.ShadowSquares.add(var20);
                     }

                     if (var2 == 0 && var20.Has(IsoObjectType.tree)) {
                        var10.m_treeSquares.add(var20);
                     }

                     if (var2 == 0 && var20.getWater() != null && var20.getWater().hasWater()) {
                        var10.m_waterSquares.add(var20);
                     }

                     if (var2 == 0 && var20.getWater() != null && var20.getWater().isbShore()) {
                        var10.m_waterShoreSquares.add(var20);
                     }

                     if (var24) {
                        var10.m_waterAttachSquares.add(var20);
                     }
                  }
               } else {
                  this.setNotRendered(var20);
               }
            }

            calculateRenderInfo.end();
            renderOneChunkLevel2.start();
            if (var2 == var10.getMinLevel(var2)) {
               var10.clearDelayedLoading(var2);
            }

            boolean var34 = true;
            boolean var35 = true;
            if (DebugOptions.instance.DelayObjectRender.getValue()) {
               var34 = (long)var12 == var1.loadedFrame || (long)var12 > var1.renderFrame;
               var35 = (long)var12 >= var1.renderFrame;
            }

            int var36;
            IsoGridSquare var38;
            GameProfiler.ProfileArea var43;
            if (var34) {
               var43 = GameProfiler.getInstance().startIfEnabled("Floor");

               for(var36 = 0; var36 < IsoCell.SolidFloor.size(); ++var36) {
                  var38 = (IsoGridSquare)IsoCell.SolidFloor.get(var36);
                  this.renderFloor(var38);
               }

               GameProfiler.getInstance().end(var43);
               GameProfiler.ProfileArea var37 = GameProfiler.getInstance().startIfEnabled("Snow");
               IndieGL.disableDepthTest();
               FBORenderSnow.getInstance().RenderSnow(var1, var2);
               IndieGL.enableDepthTest();
               GameProfiler.getInstance().end(var37);
               GameProfiler.ProfileArea var40 = GameProfiler.getInstance().startIfEnabled("Blood");
               if (IsoCamera.frameState.CamCharacterZ >= 0.0F || var2 <= PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ)) {
                  byte var41 = 8;
                  this.renderOneLevel_Blood(var1, var2, var1.wx * var41, var1.wy * var41, (var1.wx + 1) * var41, (var1.wy + 1) * var41);
                  this.renderOneLevel_Blood(var1, -1, -1, var2);
                  this.renderOneLevel_Blood(var1, 0, -1, var2);
                  this.renderOneLevel_Blood(var1, 1, -1, var2);
                  this.renderOneLevel_Blood(var1, -1, 0, var2);
                  this.renderOneLevel_Blood(var1, 1, 0, var2);
                  this.renderOneLevel_Blood(var1, -1, 1, var2);
                  this.renderOneLevel_Blood(var1, 0, 1, var2);
                  this.renderOneLevel_Blood(var1, 1, 1, var2);
               }

               GameProfiler.getInstance().end(var40);
            }

            if (!var35) {
               FBORenderChunkManager.instance.endRenderChunkLevel(var1, var2, var11, false);
            } else {
               if (DebugOptions.instance.Terrain.RenderTiles.Lua.getValue()) {
               }

               IsoGridSquare var39;
               IsoGridSquare var42;
               if (DebugOptions.instance.Terrain.RenderTiles.VegetationCorpses.getValue()) {
                  var43 = GameProfiler.getInstance().startIfEnabled("Vegetation Corpses");
                  if (DebugOptions.instance.FBORenderChunk.CorpsesInChunkTexture.getValue()) {
                     var39 = var1.getGridSquare(0, 0, var2);
                     var38 = var39 == null ? null : var39.getAdjacentSquare(IsoDirections.N);
                     if (var39 != null && var38 != null) {
                        var38.cacheLightInfo();
                        this.renderCorpses(var38, var39, true);
                     }

                     var42 = var39 == null ? null : var39.getAdjacentSquare(IsoDirections.W);
                     if (var39 != null && var42 != null) {
                        var42.cacheLightInfo();
                        this.renderCorpses(var42, var39, true);
                     }
                  }

                  for(var36 = 0; var36 < IsoCell.VegetationCorpses.size(); ++var36) {
                     var38 = (IsoGridSquare)IsoCell.VegetationCorpses.get(var36);
                     this.renderVegetation(var38);
                     if (DebugOptions.instance.FBORenderChunk.CorpsesInChunkTexture.getValue()) {
                        this.renderCorpses(var38, var38, true);
                     }
                  }

                  GameProfiler.getInstance().end(var43);
               }

               if (DebugOptions.instance.Terrain.RenderTiles.MinusFloorCharacters.getValue()) {
                  var43 = GameProfiler.getInstance().startIfEnabled("Minus Floor Chars");
                  if (DebugOptions.instance.FBORenderChunk.ItemsInChunkTexture.getValue()) {
                     var39 = var1.getGridSquare(0, 0, var2);
                     var38 = var39 == null ? null : var39.getAdjacentSquare(IsoDirections.N);
                     if (var39 != null && var38 != null) {
                        var38.cacheLightInfo();
                        this.renderWorldInventoryObjects(var38, var39, true);
                     }

                     var42 = var39 == null ? null : var39.getAdjacentSquare(IsoDirections.W);
                     if (var39 != null && var42 != null) {
                        var42.cacheLightInfo();
                        this.renderWorldInventoryObjects(var42, var39, true);
                     }
                  }

                  var39 = var1.getGridSquare(0, 0, var2);
                  var38 = var39 == null ? null : var39.getAdjacentSquare(IsoDirections.N);
                  if (var39 != null && var38 != null) {
                     var38.cacheLightInfo();
                     this.renderMinusFloor(var1, var38);
                  }

                  var42 = var39 == null ? null : var39.getAdjacentSquare(IsoDirections.W);
                  if (var39 != null && var42 != null) {
                     var42.cacheLightInfo();
                     this.renderMinusFloor(var1, var42);
                  }

                  for(var36 = 0; var36 < IsoCell.MinusFloorCharacters.size(); ++var36) {
                     var38 = (IsoGridSquare)IsoCell.MinusFloorCharacters.get(var36);
                     if (var38.getLightInfo(var4) != null) {
                        this.renderMinusFloor(var1, var38);
                        if (DebugOptions.instance.FBORenderChunk.ItemsInChunkTexture.getValue()) {
                           this.renderWorldInventoryObjects(var38, var38, true);
                        }

                        this.renderMinusFloorSE(var38);
                     }
                  }

                  GameProfiler.getInstance().end(var43);
               }

               if (PerformanceSettings.PuddlesQuality == 2) {
                  GameProfiler.getInstance().invokeAndMeasure("Low Puddles", var4, var2, var1, this::renderPuddlesToChunkTexture);
               }

               var43 = GameProfiler.getInstance().startIfEnabled("Add Chunk");
               if (var10.getCachedSquares_AnimatedAttachments(var2).size() > 0) {
                  var9.addChunkWith_AnimatedAttachments(var1);
               }

               if (var10.getCachedSquares_TranslucentFloor(var2).size() > 0) {
                  var9.addChunkWith_TranslucentFloor(var1);
               }

               if (var10.getCachedSquares_Items(var2).size() + var10.getCachedSquares_TranslucentNonFloor(var2).size() > 0) {
                  var9.addChunkWith_TranslucentNonFloor(var1);
               }

               GameProfiler.getInstance().end(var43);
               renderOneChunkLevel2.end();
               FBORenderChunkManager.instance.endRenderChunkLevel(var1, var2, var11, true);
            }
         } else {
            FBORenderChunkManager.instance.endRenderChunkLevel(var1, var2, var11, false);
            if (var10.getCachedSquares_AnimatedAttachments(var2).size() > 0) {
               var9.addChunkWith_AnimatedAttachments(var1);
            }

            if (var10.getCachedSquares_TranslucentFloor(var2).size() > 0) {
               var9.addChunkWith_TranslucentFloor(var1);
            }

            if (var10.getCachedSquares_Items(var2).size() + var10.getCachedSquares_TranslucentNonFloor(var2).size() > 0) {
               var9.addChunkWith_TranslucentNonFloor(var1);
            }

         }
      }
   }

   void calculateObjectRenderInfo(IsoGridSquare var1) {
      int var2 = IsoCamera.frameState.playerIndex;
      this.calculateObjectRenderInfo(var2, var1, var1.getLocalTemporaryObjects());
      this.calculateObjectRenderInfo(var2, var1, var1.getObjects());

      int var3;
      for(var3 = 0; var3 < var1.getStaticMovingObjects().size(); ++var3) {
         IsoMovingObject var4 = (IsoMovingObject)var1.getStaticMovingObjects().get(var3);
         IsoDeadBody var5 = (IsoDeadBody)Type.tryCastTo(var4, IsoDeadBody.class);
         if (var5 != null) {
            ObjectRenderInfo var6 = var4.getRenderInfo(var2);
            var6.m_layer = ObjectRenderLayer.Corpse;
            var6.m_renderAlpha = 1.0F;
            var6.m_bCutaway = false;
         }
      }

      for(var3 = 0; var3 < var1.getWorldObjects().size(); ++var3) {
         IsoWorldInventoryObject var7 = (IsoWorldInventoryObject)var1.getWorldObjects().get(var3);
         ObjectRenderInfo var8 = var7.getRenderInfo(var2);
         var8.m_layer = ObjectRenderLayer.WorldInventoryObject;
         var8.m_renderAlpha = 1.0F;
         var8.m_bCutaway = false;
      }

   }

   void calculateObjectRenderInfo(int var1, IsoGridSquare var2, PZArrayList<IsoObject> var3) {
      IsoObject[] var4 = (IsoObject[])var3.getElements();
      int var5 = var3.size();

      for(int var6 = 0; var6 < var5; ++var6) {
         IsoObject var7 = var4[var6];
         ObjectRenderInfo var8 = var7.getRenderInfo(var1);
         var8.m_layer = this.calculateObjectRenderLayer(var7);
         var8.m_targetAlpha = this.calculateObjectTargetAlpha(var7);
         var8.m_renderAlpha = 0.0F;
         var8.m_bCutaway = false;
         if (var8.m_targetAlpha < 1.0F) {
            if (var7 instanceof IsoMannequin) {
               boolean var9 = true;
            } else if (var8.m_layer == ObjectRenderLayer.MinusFloor || var8.m_layer == ObjectRenderLayer.MinusFloorSE) {
               var8.m_layer = ObjectRenderLayer.Translucent;
            }
         }

         if (var8.m_layer == ObjectRenderLayer.Translucent && var2.getLightLevel(var1) == 0.0F) {
            var7.setAlpha(var1, var8.m_targetAlpha);
         }

         var8.m_renderWidth = var8.m_renderHeight = 0.0F;
      }

   }

   void setNotRendered(IsoGridSquare var1) {
      if (var1 != null) {
         int var2 = IsoCamera.frameState.playerIndex;
         IsoObject[] var3 = (IsoObject[])var1.getObjects().getElements();
         int var4 = var1.getObjects().size();

         int var5;
         for(var5 = 0; var5 < var4; ++var5) {
            IsoObject var6 = var3[var5];
            ObjectRenderInfo var7 = var6.getRenderInfo(var2);
            var7.m_layer = ObjectRenderLayer.None;
            var7.m_targetAlpha = 0.0F;
         }

         for(var5 = 0; var5 < var1.getStaticMovingObjects().size(); ++var5) {
         }

         for(var5 = 0; var5 < var1.getWorldObjects().size(); ++var5) {
         }

      }
   }

   ObjectRenderLayer calculateObjectRenderLayer(IsoObject var1) {
      if (var1 instanceof IsoWorldInventoryObject) {
         return ObjectRenderLayer.WorldInventoryObject;
      } else if (this.isObjectRenderLayer_TranslucentFloor(var1)) {
         return ObjectRenderLayer.TranslucentFloor;
      } else if (this.isObjectRenderLayer_Floor(var1)) {
         return ObjectRenderLayer.Floor;
      } else if (this.isObjectRenderLayer_Vegetation(var1)) {
         return ObjectRenderLayer.Vegetation;
      } else if (this.isObjectRenderLayer_MinusFloor(var1)) {
         return ObjectRenderLayer.MinusFloor;
      } else if (this.isObjectRenderLayer_MinusFloorSE(var1)) {
         return ObjectRenderLayer.MinusFloorSE;
      } else {
         return this.isObjectRenderLayer_Translucent(var1) ? ObjectRenderLayer.Translucent : ObjectRenderLayer.None;
      }
   }

   boolean isObjectRenderLayer_Floor(IsoObject var1) {
      IsoGridSquare var2 = var1.square;
      if (var2 == null) {
         return false;
      } else {
         boolean var3 = true;
         if (var1.sprite != null && !var1.sprite.solidfloor && var1.sprite.renderLayer != 1) {
            var3 = false;
         }

         if (var1 instanceof IsoFire || var1 instanceof IsoCarBatteryCharger) {
            var3 = false;
         }

         IsoWaterGeometry var4 = var2.z == 0 ? var2.getWater() : null;
         if (IsoWater.getInstance().getShaderEnable() && var4 != null && var4.isValid() && var1.sprite != null && var1.sprite.Properties.Is(IsoFlagType.water)) {
            var3 = var4.isbShore();
         }

         if (var3 && IsoWater.getInstance().getShaderEnable() && var4 != null && var4.isValid() && !var4.isbShore()) {
            IsoObject var5 = var2.getWaterObject();
            var3 = var5 != null && var5.getObjectIndex() < var1.getObjectIndex();
         }

         if (var3 && var1.sprite != null && var1.sprite.getProperties().getSlopedSurfaceDirection() != null) {
            return false;
         } else {
            int var6 = IsoCamera.frameState.playerIndex;
            return FBORenderCutaways.getInstance().shouldHideElevatedFloor(var6, var1) ? false : var3;
         }
      }
   }

   boolean isObjectRenderLayer_Vegetation(IsoObject var1) {
      IsoGridSquare var2 = var1.square;
      boolean var3 = var1.sprite != null && (var1.sprite.isBush || var1.sprite.canBeRemoved || var1.sprite.attachedFloor);
      return var3 && var2.bFlattenGrassEtc;
   }

   boolean isObjectRenderLayer_MinusFloor(IsoObject var1) {
      IsoSprite var2 = var1.getSprite();
      if (var2 != null && (var2.depthFlags & 2) != 0) {
         return false;
      } else if (var1.isAnimating()) {
         return false;
      } else {
         if (Core.getInstance().getOptionDoWindSpriteEffects()) {
            if (var1 instanceof IsoTree) {
               return false;
            }

            if (var1.getWindRenderEffects() != null) {
               return false;
            }
         } else {
            if (this.isTranslucentTree(var1)) {
               return false;
            }

            if (var1 instanceof IsoTree && var1.getObjectRenderEffects() != null) {
               return false;
            }
         }

         if (var1.getObjectRenderEffectsToApply() != null) {
            return false;
         } else if (var1 instanceof IsoTree && ((IsoTree)var1).fadeAlpha < 1.0F) {
            return false;
         } else {
            IsoMannequin var3 = (IsoMannequin)Type.tryCastTo(var1, IsoMannequin.class);
            if (var3 != null && var3.shouldRenderEachFrame()) {
               return false;
            } else {
               IsoGridSquare var4 = var1.square;
               boolean var5 = true;
               IsoObjectType var6 = IsoObjectType.MAX;
               if (var2 != null) {
                  var6 = var2.getType();
               }

               if (var2 != null && (var2.solidfloor || var2.renderLayer == 1) && var2.getProperties().getSlopedSurfaceDirection() == null) {
                  var5 = false;
               }

               if (var1 instanceof IsoFire) {
                  var5 = false;
               }

               short var7 = 1000;
               if (var4.z >= var7 && (var2 == null || !var2.alwaysDraw)) {
                  var5 = false;
               }

               boolean var8 = var2 != null && (var2.isBush || var2.canBeRemoved || var2.attachedFloor);
               if (var8 && var4.bFlattenGrassEtc) {
                  return false;
               } else {
                  if (var2 != null && (var6 == IsoObjectType.WestRoofB || var6 == IsoObjectType.WestRoofM || var6 == IsoObjectType.WestRoofT) && var4.z == var7 - 1 && var4.z == PZMath.fastfloor(IsoCamera.getCameraCharacterZ())) {
                     var5 = false;
                  }

                  if (var2 != null && !var2.solidfloor && IsoPlayer.getInstance().isClimbing()) {
                     var5 = true;
                  }

                  if (var4.isSpriteOnSouthOrEastWall(var1)) {
                     var5 = false;
                  }

                  boolean var9 = var1 instanceof IsoWindow;
                  IsoDoor var10 = (IsoDoor)Type.tryCastTo(var1, IsoDoor.class);
                  var9 |= var10 != null && var10.getProperties() != null && var10.getProperties().Is("doorTrans");
                  int var11 = IsoCamera.frameState.playerIndex;
                  PerPlayerData var12 = this.perPlayerData[var11];
                  var9 |= var2 != null && (var2.solid || var2.solidTrans) && (var1.getAlpha(var11) < 1.0F && var12.isFadingInSquare(var4) || var12.isSquareObscuringPlayer(var4));
                  if (var9) {
                     var5 = false;
                  }

                  return var5;
               }
            }
         }
      }
   }

   boolean isObjectRenderLayer_MinusFloorSE(IsoObject var1) {
      IsoGridSquare var2 = var1.square;
      boolean var3 = true;
      IsoObjectType var4 = IsoObjectType.MAX;
      if (var1.sprite != null) {
         var4 = var1.sprite.getType();
      }

      if (var1.sprite != null && (var1.sprite.solidfloor || var1.sprite.renderLayer == 1)) {
         var3 = false;
      }

      if (var1 instanceof IsoFire) {
         var3 = false;
      }

      short var5 = 1000;
      if (var2.z >= var5 && (var1.sprite == null || !var1.sprite.alwaysDraw)) {
         var3 = false;
      }

      boolean var6 = var1.sprite != null && (var1.sprite.isBush || var1.sprite.canBeRemoved || var1.sprite.attachedFloor);
      if (var6) {
         return false;
      } else {
         if (var1.sprite != null && (var4 == IsoObjectType.WestRoofB || var4 == IsoObjectType.WestRoofM || var4 == IsoObjectType.WestRoofT) && var2.z == var5 - 1 && var2.z == PZMath.fastfloor(IsoCamera.getCameraCharacterZ())) {
            var3 = false;
         }

         if (var1.sprite != null && !var1.sprite.solidfloor && IsoPlayer.getInstance().isClimbing()) {
            var3 = true;
         }

         if (!var2.isSpriteOnSouthOrEastWall(var1)) {
            var3 = false;
         }

         boolean var7 = var1 instanceof IsoWindow;
         IsoDoor var8 = (IsoDoor)Type.tryCastTo(var1, IsoDoor.class);
         var7 |= var8 != null && var8.getProperties() != null && var8.getProperties().Is("doorTrans");
         if (var7) {
            var3 = false;
         }

         return var3;
      }
   }

   boolean isObjectRenderLayer_TranslucentFloor(IsoObject var1) {
      boolean var2 = var1.getSprite() != null && var1.getSprite().getProperties().Is(IsoFlagType.transparentFloor);
      if (var1.getSprite() != null && var1.getSprite().solidfloor && DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.ShoreFade.getValue()) {
         IsoWaterGeometry var3 = var1.square.z == 0 ? var1.square.getWater() : null;
         boolean var4 = var3 != null && var3.isbShore();
         if (var4) {
            return true;
         }
      }

      return var2;
   }

   boolean isObjectRenderLayer_Translucent(IsoObject var1) {
      IsoSprite var2 = var1.getSprite();
      if (var2 != null && (var2.depthFlags & 2) != 0) {
         return true;
      } else if (var1 instanceof IsoFire) {
         return true;
      } else if (var1.isAnimating()) {
         return true;
      } else {
         if (Core.getInstance().getOptionDoWindSpriteEffects()) {
            if (var1 instanceof IsoTree) {
               return true;
            }

            if (var1.getWindRenderEffects() != null) {
               return true;
            }
         } else {
            if (this.isTranslucentTree(var1)) {
               return true;
            }

            if (var1 instanceof IsoTree && var1.getObjectRenderEffects() != null) {
               return true;
            }
         }

         if (var1.getObjectRenderEffectsToApply() != null) {
            return true;
         } else if (var1 instanceof IsoTree && ((IsoTree)var1).fadeAlpha < 1.0F) {
            return true;
         } else {
            boolean var3 = var1 instanceof IsoWindow;
            IsoDoor var4 = (IsoDoor)Type.tryCastTo(var1, IsoDoor.class);
            var3 |= var4 != null && var4.getProperties() != null && var4.getProperties().Is("doorTrans");
            int var5 = IsoCamera.frameState.playerIndex;
            PerPlayerData var6 = this.perPlayerData[var5];
            var3 |= var2 != null && (var2.solid || var2.solidTrans) && (var1.getAlpha(var5) < 1.0F && var6.isFadingInSquare(var1.square) || var6.isSquareObscuringPlayer(var1.square));
            return var3;
         }
      }
   }

   boolean isTranslucentTree(IsoObject var1) {
      IsoTree var2 = (IsoTree)Type.tryCastTo(var1, IsoTree.class);
      if (var2 == null) {
         return false;
      } else {
         int var3 = IsoCamera.frameState.playerIndex;
         FBORenderLevels var4 = var1.square.chunk.getRenderLevels(var3);
         if (!var4.m_bInStencilRect) {
            return false;
         } else {
            IsoGridSquare var5 = var1.square;
            var5.IsOnScreen();
            float var6 = var5.CachedScreenX - IsoCamera.frameState.OffX;
            float var7 = var5.CachedScreenY - IsoCamera.frameState.OffY;
            IsoCell var8 = IsoWorld.instance.CurrentCell;
            if (!(var6 + (float)(32 * Core.TileScale) <= (float)var8.StencilX1) && !(var6 - (float)(32 * Core.TileScale) >= (float)var8.StencilX2) && !(var7 + (float)(32 * Core.TileScale) <= (float)var8.StencilY1) && !(var7 - (float)(96 * Core.TileScale) >= (float)var8.StencilY2)) {
               return var5.x >= PZMath.fastfloor(IsoCamera.frameState.CamCharacterX) && var5.y >= PZMath.fastfloor(IsoCamera.frameState.CamCharacterY) && IsoCamera.frameState.CamCharacterSquare != null;
            } else {
               return false;
            }
         }
      }
   }

   float calculateObjectTargetAlpha(IsoObject var1) {
      int var2 = IsoCamera.frameState.playerIndex;
      IsoGridSquare var3 = var1.getSquare();
      ObjectRenderLayer var4 = var1.getRenderInfo(var2).m_layer;
      IsoObjectType var5 = IsoObjectType.MAX;
      if (var1.sprite != null) {
         var5 = var1.sprite.getType();
      }

      if (var4 != ObjectRenderLayer.MinusFloor && var4 != ObjectRenderLayer.MinusFloorSE && var4 != ObjectRenderLayer.Translucent) {
         return 1.0F;
      } else {
         boolean var6 = var1 instanceof IsoDoor && ((IsoDoor)var1).open || var1 instanceof IsoThumpable && ((IsoThumpable)var1).open;
         if (var6) {
            return 0.6F;
         } else {
            boolean var7 = var5 == IsoObjectType.doorFrW || var5 == IsoObjectType.doorW || var1.sprite != null && var1.sprite.cutW;
            boolean var8 = var5 == IsoObjectType.doorFrN || var5 == IsoObjectType.doorN || var1.sprite != null && var1.sprite.cutN;
            return !var7 && !var8 ? this.calculateObjectTargetAlpha_NotDoorOrWall(var1) : this.calculateObjectTargetAlpha_DoorOrWall(var1);
         }
      }
   }

   float calculateObjectTargetAlpha_DoorOrWall(IsoObject var1) {
      if (var1.sprite == null) {
         return 1.0F;
      } else if (var1.sprite.cutW && var1.sprite.cutN) {
         return 1.0F;
      } else {
         IsoObjectType var2 = var1.sprite.getType();
         int var3 = IsoCamera.frameState.playerIndex;
         IsoGridSquare var4 = var1.getSquare();
         IsoGridSquare var5 = var4.nav[IsoDirections.S.index()];
         IsoGridSquare var6 = var4.nav[IsoDirections.E.index()];
         if (var1.isFascia() && this.shouldHideFascia(var3, var1)) {
            var1.setAlphaAndTarget(var3, 0.0F);
            return 0.0F;
         } else {
            int var7 = var4.getPlayerCutawayFlag(var3, this.currentTimeMillis);
            int var8 = var5 == null ? 0 : var5.getPlayerCutawayFlag(var3, this.currentTimeMillis);
            int var9 = var6 == null ? 0 : var6.getPlayerCutawayFlag(var3, this.currentTimeMillis);
            if (!OUTLINE_DOUBLEDOOR_FRAMES && (var1.sprite.getProperties().Is(IsoFlagType.DoubleDoor1) || var1.sprite.getProperties().Is(IsoFlagType.DoubleDoor2))) {
               if ((var8 | var7) != 0) {
                  return 0.4F;
               }

               if ((var7 | var9) != 0) {
                  return 0.4F;
               }
            }

            IsoObjectType var10;
            IsoObjectType var11;
            boolean var12;
            boolean var13;
            boolean var14;
            if (var2 != IsoObjectType.doorFrW && var2 != IsoObjectType.doorW && !var1.sprite.cutW) {
               if (var2 == IsoObjectType.doorFrN || var2 == IsoObjectType.doorN || var1.sprite.cutN) {
                  var10 = IsoObjectType.doorFrN;
                  var11 = IsoObjectType.doorN;
                  var12 = var2 == var10 || var2 == var11;
                  var13 = var1 instanceof IsoWindow;
                  var14 = (var7 & 1) != 0;
                  if ((var12 || var13) && var14) {
                     if (var12 && !this.hasSeenDoorN(var3, var4)) {
                        return 0.0F;
                     }

                     if (var13 && !this.hasSeenWindowN(var3, var4)) {
                        return 0.0F;
                     }

                     return 0.4F;
                  }
               }
            } else {
               var10 = IsoObjectType.doorFrW;
               var11 = IsoObjectType.doorW;
               var12 = var2 == var10 || var2 == var11;
               var13 = var1 instanceof IsoWindow;
               var14 = (var7 & 2) != 0;
               if ((var12 || var13) && var14) {
                  if (var12 && !this.hasSeenDoorW(var3, var4)) {
                     return 0.0F;
                  }

                  if (var13 && !this.hasSeenWindowW(var3, var4)) {
                     return 0.0F;
                  }

                  return 0.4F;
               }
            }

            return 1.0F;
         }
      }
   }

   boolean hasSeenDoorW(int var1, IsoGridSquare var2) {
      boolean var3 = true;
      boolean var4 = false;
      IsoObject[] var5 = (IsoObject[])var2.getObjects().getElements();
      int var6 = var2.getObjects().size();

      for(int var7 = 0; var7 < var6; ++var7) {
         IsoObject var8 = var5[var7];
         IsoSprite var9 = var8.sprite;
         IsoObjectType var10 = var9 == null ? IsoObjectType.MAX : var9.getType();
         if (var10 == IsoObjectType.doorFrW || var10 == IsoObjectType.doorW) {
            IsoGridSquare var11 = var2.nav[IsoDirections.W.index()];
            var4 |= var3 || var11 != null && var11.isCouldSee(var1);
         }
      }

      return var4;
   }

   boolean hasSeenDoorN(int var1, IsoGridSquare var2) {
      boolean var3 = true;
      boolean var4 = false;
      IsoObject[] var5 = (IsoObject[])var2.getObjects().getElements();
      int var6 = var2.getObjects().size();

      for(int var7 = 0; var7 < var6; ++var7) {
         IsoObject var8 = var5[var7];
         IsoSprite var9 = var8.sprite;
         IsoObjectType var10 = var9 == null ? IsoObjectType.MAX : var9.getType();
         if (var10 == IsoObjectType.doorFrN || var10 == IsoObjectType.doorN) {
            IsoGridSquare var11 = var2.nav[IsoDirections.N.index()];
            var4 |= var3 || var11 != null && var11.isCouldSee(var1);
         }
      }

      return var4;
   }

   boolean hasSeenWindowW(int var1, IsoGridSquare var2) {
      boolean var3 = true;
      boolean var4 = false;
      IsoObject[] var5 = (IsoObject[])var2.getObjects().getElements();
      int var6 = var2.getObjects().size();

      for(int var7 = 0; var7 < var6; ++var7) {
         IsoObject var8 = var5[var7];
         if (var2.isWindowOrWindowFrame(var8, false)) {
            IsoGridSquare var9 = var2.nav[IsoDirections.W.index()];
            var4 |= var3 || var9 != null && var9.isCouldSee(var1);
         }
      }

      return var4;
   }

   boolean hasSeenWindowN(int var1, IsoGridSquare var2) {
      boolean var3 = true;
      boolean var4 = false;
      IsoObject[] var5 = (IsoObject[])var2.getObjects().getElements();
      int var6 = var2.getObjects().size();

      for(int var7 = 0; var7 < var6; ++var7) {
         IsoObject var8 = var5[var7];
         if (var2.isWindowOrWindowFrame(var8, true)) {
            IsoGridSquare var9 = var2.nav[IsoDirections.N.index()];
            var4 |= var3 || var9 != null && var9.isCouldSee(var1);
         }
      }

      return var4;
   }

   float calculateObjectTargetAlpha_NotDoorOrWall(IsoObject var1) {
      int var2 = IsoCamera.frameState.playerIndex;
      IsoGridSquare var3 = var1.getSquare();
      IsoObjectType var4 = IsoObjectType.MAX;
      if (var1.sprite != null) {
         var4 = var1.sprite.getType();
      }

      IsoCurtain var5 = (IsoCurtain)Type.tryCastTo(var1, IsoCurtain.class);
      if (var5 != null) {
         IsoObject var6 = var5.getObjectAttachedTo();
         if (var6 != null && var3.getTargetDarkMulti(var2) <= var6.getSquare().getTargetDarkMulti(var2)) {
            return this.calculateObjectTargetAlpha_NotDoorOrWall(var6);
         }
      }

      int var12 = var3.getPlayerCutawayFlag(var2, this.currentTimeMillis);
      boolean var7 = (var12 & 1) != 0;
      boolean var8 = (var12 & 2) != 0;
      if (var1 instanceof IsoWindowFrame var14) {
         return this.calculateWindowTargetAlpha(var2, var1, var14.getOppositeSquare(), var14.getNorth());
      } else if (var1 instanceof IsoWindow var13) {
         return this.calculateWindowTargetAlpha(var2, var1, var13.getOppositeSquare(), var13.getNorth());
      } else {
         boolean var9 = var4 == IsoObjectType.WestRoofB || var4 == IsoObjectType.WestRoofM || var4 == IsoObjectType.WestRoofT;
         boolean var10 = var9 && PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ) == var3.getZ() && var3.getBuilding() == null;
         if (var10 && FBORenderCutaways.getInstance().CanBuildingSquareOccludePlayer(var3, var2)) {
            return 0.05F;
         } else if (var1.isFascia() && this.shouldHideFascia(var2, var1)) {
            var1.setAlphaAndTarget(var2, 0.0F);
            return 0.0F;
         } else {
            if (IsoCamera.frameState.CamCharacterSquare == null || IsoCamera.frameState.CamCharacterSquare.getRoom() != var3.getRoom()) {
               boolean var11 = false;
               if (var3.Is(IsoFlagType.cutN) && var3.Is(IsoFlagType.cutW)) {
                  var11 = var7 || var8;
               } else if (var3.Is(IsoFlagType.cutW)) {
                  var11 = var8;
               } else if (var3.Is(IsoFlagType.cutN)) {
                  var11 = var7;
               }

               if (var11) {
                  return var3.isCanSee(var2) ? 0.25F : 0.0F;
               }
            }

            if (this.isPotentiallyObscuringObject(var1) && this.perPlayerData[var2].isSquareObscuringPlayer(var3)) {
               if (var1.sprite != null && var1.sprite.getProperties().Is(IsoFlagType.attachedCeiling)) {
                  return 0.25F;
               } else {
                  return var1.isStairsObject() ? 0.5F : 0.66F;
               }
            } else {
               return 1.0F;
            }
         }
      }
   }

   public float calculateWindowTargetAlpha(int var1, IsoObject var2, IsoGridSquare var3, boolean var4) {
      IsoGridSquare var5 = var2.getSquare();
      int var6 = var5.getPlayerCutawayFlag(var1, this.currentTimeMillis);
      boolean var7 = (var6 & 1) != 0;
      boolean var8 = (var6 & 2) != 0;
      float var9 = 1.0F;
      if (var2.getTargetAlpha(var1) < 1.0E-4F && var3 != null && var3 != var5 && var3.lighting[var1].bSeen()) {
         var9 = var3.lighting[var1].darkMulti() * 2.0F;
      }

      if (var9 > 0.75F && (var7 && var4 || var8 && !var4)) {
         float var10 = 0.75F;
         float var11 = 0.1F;
         IsoPlayer var12 = IsoPlayer.players[var1];
         if (var12 != null) {
            float var13 = 25.0F;
            float var14 = PZMath.min(IsoUtils.DistanceToSquared(var12.getX(), var12.getY(), (float)var5.x + 0.5F, (float)var5.y + 0.5F), var13);
            float var15 = PZMath.lerp(var11, var10, 1.0F - var14 / var13);
            var9 = Math.max(var15, var11);
         } else {
            var9 = var11;
         }
      }

      return var9;
   }

   void renderFloor(IsoGridSquare var1) {
      int var2 = IsoCamera.frameState.playerIndex;
      IsoObject[] var3 = (IsoObject[])var1.getObjects().getElements();
      int var4 = var1.getObjects().size();

      for(int var5 = 0; var5 < var4; ++var5) {
         IsoObject var6 = var3[var5];
         ObjectRenderInfo var7 = var6.getRenderInfo(var2);
         if (var7.m_layer == ObjectRenderLayer.Floor) {
            this.renderFloor(var6);
         }
      }

   }

   void renderFloor(IsoObject var1) {
      int var2 = IsoCamera.frameState.playerIndex;
      ObjectRenderInfo var3 = var1.getRenderInfo(var2);
      IsoGridSquare var4 = var1.square;
      IndieGL.glAlphaFunc(516, 0.0F);
      var1.setTargetAlpha(var2, var3.m_targetAlpha);
      var1.setAlpha(var2, var3.m_targetAlpha);
      if (DebugOptions.instance.Terrain.RenderTiles.RenderGridSquares.getValue()) {
         if (var1.sprite != null) {
            IndieGL.glDepthMask(true);
            if (var1.sprite.getProperties().getSlopedSurfaceDirection() != null && var4.getPlayerCutawayFlag(var2, this.currentTimeMillis) != 0) {
               IsoSprite var18 = IsoSpriteManager.instance.getSprite("ramps_01_23");
               this.defColorInfo.set(var4.getLightInfo(var2));
               if (DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
                  this.defColorInfo.set(1.0F, 1.0F, 1.0F, this.defColorInfo.a);
               }

               var18.render(var1, (float)var4.x, (float)var4.y, (float)var4.z, var1.getDir(), var1.offsetX, var1.offsetY, this.defColorInfo, true);
            } else {
               FloorShaperAttachedSprites var6 = FloorShaperAttachedSprites.instance;
               Object var5;
               if (!var1.getProperties().Is(IsoFlagType.diamondFloor) && !var1.getProperties().Is(IsoFlagType.water)) {
                  var5 = FloorShaperDeDiamond.instance;
               } else {
                  var5 = FloorShaperDiamond.instance;
               }

               IsoWaterGeometry var7 = var4.z == 0 ? var4.getWater() : null;
               boolean var8 = var7 != null && var7.isbShore();
               float var9 = var7 == null ? 0.0F : var7.depth[0];
               float var10 = var7 == null ? 0.0F : var7.depth[3];
               float var11 = var7 == null ? 0.0F : var7.depth[2];
               float var12 = var7 == null ? 0.0F : var7.depth[1];
               int var13 = var4.getVertLight(0, var2);
               int var14 = var4.getVertLight(1, var2);
               int var15 = var4.getVertLight(2, var2);
               int var16 = var4.getVertLight(3, var2);
               if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Floor.LightingDebug.getValue()) {
                  var13 = -65536;
                  var14 = -65536;
                  var15 = -16776961;
                  var16 = -16776961;
               }

               var6.setShore(var8);
               var6.setWaterDepth(var9, var10, var11, var12);
               var6.setVertColors(var13, var14, var15, var16);
               ((FloorShaper)var5).setShore(var8);
               ((FloorShaper)var5).setWaterDepth(var9, var10, var11, var12);
               ((FloorShaper)var5).setVertColors(var13, var14, var15, var16);
               TileSeamModifier.instance.setShore(var8);
               TileSeamModifier.instance.setWaterDepth(var9, var10, var11, var12);
               TileSeamModifier.instance.setVertColors(var13, var14, var15, var16);
               IsoGridSquare.setBlendFunc();
               Object var17 = null;
               IndieGL.StartShader((Shader)var17, var2);
               this.defColorInfo.set(1.0F, 1.0F, 1.0F, 1.0F);
               var1.renderFloorTile((float)var4.x, (float)var4.y, (float)var4.z, this.defColorInfo, true, false, (Shader)var17, (Consumer)var5, var6);
               IndieGL.EndShader();
            }
         }
      }
   }

   void renderFishSplashes(int var1, ArrayList<IsoGridSquare> var2) {
      IndieGL.glBlendFunc(770, 771);

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         IsoGridSquare var4 = (IsoGridSquare)var2.get(var3);
         ColorInfo var5 = var4.getLightInfo(var1);
         var4.renderFishSplash(var1, var5);
      }

   }

   void renderVegetation(IsoGridSquare var1) {
      int var2 = IsoCamera.frameState.playerIndex;
      IsoObject[] var3 = (IsoObject[])var1.getObjects().getElements();
      int var4 = var1.getObjects().size();

      for(int var5 = 0; var5 < var4; ++var5) {
         IsoObject var6 = var3[var5];
         ObjectRenderInfo var7 = var6.getRenderInfo(var2);
         if (var7.m_layer == ObjectRenderLayer.Vegetation) {
            this.renderVegetation(var6);
         }
      }

   }

   void renderVegetation(IsoObject var1) {
      this.renderMinusFloor(var1);
   }

   void renderCorpsesInWorld(int var1) {
      PerPlayerData var2 = this.perPlayerData[var1];

      for(int var3 = 0; var3 < var2.onScreenChunks.size(); ++var3) {
         IsoChunk var4 = (IsoChunk)var2.onScreenChunks.get(var3);
         FBORenderLevels var5 = var4.getRenderLevels(var1);

         for(int var6 = var4.minLevel; var6 <= var4.maxLevel; ++var6) {
            if (var5.isOnScreen(var6) && var6 == var5.getMinLevel(var6)) {
               ArrayList var7 = var5.getCachedSquares_Corpses(var6);

               for(int var8 = 0; var8 < var7.size(); ++var8) {
                  IsoGridSquare var9 = (IsoGridSquare)var7.get(var8);
                  this.renderCorpses(var9, var9, false);
               }
            }
         }
      }

   }

   void renderCorpses(IsoGridSquare var1, IsoGridSquare var2, boolean var3) {
      if (this.shouldRenderSquare(var2)) {
         int var4 = IsoCamera.frameState.playerIndex;
         ColorInfo var5 = var1.getLightInfo(var4);
         FBORenderLevels var6 = var2.chunk.getRenderLevels(var4);

         int var7;
         for(var7 = 0; var7 < var1.getStaticMovingObjects().size(); ++var7) {
            IsoMovingObject var8 = (IsoMovingObject)var1.getStaticMovingObjects().get(var7);
            if ((var8.sprite != null || var8 instanceof IsoDeadBody) && var8 instanceof IsoDeadBody) {
               if (var3) {
                  if (var2 == var8.getRenderSquare()) {
                     FBORenderChunk var9 = var6.getFBOForLevel(var1.z, Core.getInstance().getZoom(var4));
                     FBORenderCorpses.getInstance().render(var9.index, (IsoDeadBody)var8);
                  }
               } else {
                  var8.render(var8.getX(), var8.getY(), var8.getZ(), var5, true, false, (Shader)null);
               }
            }
         }

         var7 = var1.getMovingObjects().size();

         for(int var12 = 0; var12 < var7; ++var12) {
            IsoMovingObject var13 = (IsoMovingObject)var1.getMovingObjects().get(var12);
            if (var13 != null && var13.sprite != null) {
               boolean var10 = var13.isOnFloor();
               if (var10 && var13 instanceof IsoZombie) {
                  IsoZombie var11 = (IsoZombie)var13;
                  var10 = var11.isProne();
                  if (!BaseVehicle.RENDER_TO_TEXTURE) {
                     var10 = false;
                  }
               }

               if (var10) {
                  var13.render(var13.getX(), var13.getY(), var13.getZ(), var5, true, false, (Shader)null);
               }
            }
         }

      }
   }

   void renderItemsInWorld(int var1) {
      PerPlayerData var2 = this.perPlayerData[var1];

      for(int var3 = 0; var3 < var2.onScreenChunks.size(); ++var3) {
         IsoChunk var4 = (IsoChunk)var2.onScreenChunks.get(var3);
         FBORenderLevels var5 = var4.getRenderLevels(var1);

         for(int var6 = var4.minLevel; var6 <= var4.maxLevel; ++var6) {
            if (var5.isOnScreen(var6) && var6 == var5.getMinLevel(var6)) {
               ArrayList var7 = var5.getCachedSquares_Items(var6);

               for(int var8 = 0; var8 < var7.size(); ++var8) {
                  IsoGridSquare var9 = (IsoGridSquare)var7.get(var8);
                  if (var9.chunk == var4) {
                     this.renderWorldInventoryObjects(var9, var9, false);
                  } else {
                     IsoGridSquare var10 = var4.getGridSquare(0, 0, var9.z);
                     this.renderWorldInventoryObjects(var9, var10, false);
                  }
               }
            }
         }
      }

   }

   void renderMinusFloor(IsoChunk var1, IsoGridSquare var2) {
      this.renderMinusFloor(var1, var2, var2.getLocalTemporaryObjects());
      this.renderMinusFloor(var1, var2, var2.getObjects());
   }

   void renderMinusFloor(IsoChunk var1, IsoGridSquare var2, PZArrayList<IsoObject> var3) {
      int var4 = IsoCamera.frameState.playerIndex;
      boolean var5 = FBORenderCutaways.getInstance().isForceRenderSquare(var4, var2);
      IsoObject[] var6 = (IsoObject[])var3.getElements();
      int var7 = var3.size();

      for(int var8 = 0; var8 < var7; ++var8) {
         IsoObject var9 = var6[var8];
         ObjectRenderInfo var10 = var9.getRenderInfo(var4);
         if (var10.m_layer == ObjectRenderLayer.MinusFloor) {
            IsoGridSquare var11 = var9.getRenderSquare();
            if (var11 != null && var1 == var11.chunk) {
               if (var5) {
                  IsoSpriteGrid var12 = var9.getSpriteGrid();
                  if (var12 == null || var12.getLevels() == 1) {
                     continue;
                  }
               }

               this.renderMinusFloor(var9);
            }
         }
      }

   }

   void renderMinusFloor(IsoObject var1) {
      int var2 = IsoCamera.frameState.playerIndex;
      ObjectRenderInfo var3 = var1.getRenderInfo(var2);
      IsoObjectType var4 = IsoObjectType.MAX;
      if (var1.sprite != null) {
         var4 = var1.sprite.getType();
      }

      boolean var10000;
      if (var1.getProperties() != null && var1.getProperties().Is(IsoFlagType.NeverCutaway)) {
         var10000 = true;
      } else {
         var10000 = false;
      }

      boolean var5 = false;
      boolean var6 = !var5 && (var4 == IsoObjectType.doorFrW || var4 == IsoObjectType.doorW || var1.sprite != null && var1.sprite.cutW);
      boolean var7 = !var5 && (var4 == IsoObjectType.doorFrN || var4 == IsoObjectType.doorN || var1.sprite != null && var1.sprite.cutN);
      IndieGL.glAlphaFunc(516, 0.0F);
      var1.setAlphaAndTarget(var2, var3.m_targetAlpha);
      IsoGridSquare.setBlendFunc();
      if (var1.sprite != null && (var6 || var7)) {
         if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.DoorsAndWalls.getValue()) {
            this.renderMinusFloor_DoorOrWall(var1);
         }
      } else if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Objects.getValue()) {
         this.renderMinusFloor_NotDoorOrWall(var1);
      }

   }

   void renderMinusFloor_DoorOrWall(IsoObject var1) {
      int var2 = IsoCamera.frameState.playerIndex;
      ObjectRenderInfo var3 = var1.getRenderInfo(var2);
      IsoGridSquare var4 = var1.square;
      IsoGridSquare var5 = var4.nav[IsoDirections.N.index()];
      IsoGridSquare var6 = var4.nav[IsoDirections.S.index()];
      IsoGridSquare var7 = var4.nav[IsoDirections.W.index()];
      IsoGridSquare var8 = var4.nav[IsoDirections.E.index()];
      int var9 = var4.getPlayerCutawayFlag(var2, this.currentTimeMillis);
      int var10 = var5 == null ? 0 : var5.getPlayerCutawayFlag(var2, this.currentTimeMillis);
      int var11 = var6 == null ? 0 : var6.getPlayerCutawayFlag(var2, this.currentTimeMillis);
      int var12 = var7 == null ? 0 : var7.getPlayerCutawayFlag(var2, this.currentTimeMillis);
      int var13 = var8 == null ? 0 : var8.getPlayerCutawayFlag(var2, this.currentTimeMillis);
      IsoObjectType var14 = IsoObjectType.MAX;
      if (var1.sprite != null) {
         var14 = var1.sprite.getType();
      }

      IndieGL.glAlphaFunc(516, 0.0F);
      var1.setAlphaAndTarget(var2, var3.m_targetAlpha);
      var4.getLightInfo(var2);
      this.defColorInfo.set(1.0F, 1.0F, 1.0F, 1.0F);
      ColorInfo var15 = this.defColorInfo;
      byte var16 = 0;
      Object var17 = null;
      boolean var18 = false;
      boolean var19 = false;
      boolean var20 = false;
      boolean var21 = false;
      boolean var22 = var4.lighting[var2].bCouldSee();
      lowestCutawayObject = null;
      IsoObject[] var23 = (IsoObject[])var4.getObjects().getElements();
      int var24 = var4.getObjects().size();

      for(int var25 = 0; var25 < var24; ++var25) {
         IsoObject var26 = var23[var25];
         IsoObjectType var27 = var26.sprite == null ? IsoObjectType.MAX : var26.sprite.getType();
         IsoGridSquare var28;
         if (var4.isWindowOrWindowFrame(var26, true) && (var9 & 1) != 0) {
            var28 = var4.nav[IsoDirections.N.index()];
            var20 = var22 || var28 != null && var28.isCouldSee(var2);
            lowestCutawayObject = var26;
            break;
         }

         if (var4.isWindowOrWindowFrame(var26, false) && (var9 & 2) != 0) {
            var28 = var4.nav[IsoDirections.W.index()];
            var21 = var22 || var28 != null && var28.isCouldSee(var2);
            lowestCutawayObject = var26;
            break;
         }

         if (var26.sprite != null && (var27 == IsoObjectType.doorFrN || var27 == IsoObjectType.doorN || var26.sprite.getProperties().Is(IsoFlagType.DoorWallN)) && (var9 & 1) != 0) {
            var28 = var4.nav[IsoDirections.N.index()];
            var18 = var22 || var28 != null && var28.isCouldSee(var2);
            lowestCutawayObject = var26;
            break;
         }

         if (var26.sprite != null && (var27 == IsoObjectType.doorFrW || var27 == IsoObjectType.doorW || var26.sprite.getProperties().Is(IsoFlagType.DoorWallW)) && (var9 & 2) != 0) {
            var28 = var4.nav[IsoDirections.W.index()];
            var19 = var22 || var28 != null && var28.isCouldSee(var2);
            lowestCutawayObject = var26;
            break;
         }
      }

      IsoGridSquare.CircleStencil = true;
      boolean var29 = var1.getProperties() != null && var1.getProperties().Is(IsoFlagType.NeverCutaway);
      if (var29) {
         IsoGridSquare.CircleStencil = false;
      }

      IndieGL.glDepthMask(true);
      if (var1.sprite.getProperties().Is(IsoFlagType.WallSE)) {
         var4.DoWallLightingW(var1, var16, var9, var10, var11, var12, var13, var19, var21, (Shader)var17);
      } else if (var1.sprite.cutW && var1.sprite.cutN) {
         var4.DoWallLightingNW(var1, var16, var9, var10, var11, var12, var13, var18, var19, var20, var21, (Shader)var17);
      } else if (var14 != IsoObjectType.doorFrW && var14 != IsoObjectType.doorW && !var1.sprite.cutW) {
         if (var14 == IsoObjectType.doorFrN || var14 == IsoObjectType.doorN || var1.sprite.cutN) {
            var4.DoWallLightingN(var1, var16, var9, var10, var11, var12, var13, var18, var20, (Shader)var17);
         }
      } else {
         var4.DoWallLightingW(var1, var16, var9, var10, var11, var12, var13, var19, var21, (Shader)var17);
      }

   }

   void renderMinusFloor_NotDoorOrWall(IsoObject var1) {
      int var2 = IsoCamera.frameState.playerIndex;
      ObjectRenderInfo var3 = var1.getRenderInfo(var2);
      IsoGridSquare var4 = var1.square;
      IndieGL.glAlphaFunc(516, 0.0F);
      if (this.bRenderTranslucentOnly) {
         var1.setTargetAlpha(var2, var3.m_targetAlpha);
         if (var1.getType() == IsoObjectType.WestRoofT) {
            var1.setAlphaAndTarget(var2, var3.m_targetAlpha);
         }
      } else {
         var1.setAlphaAndTarget(var2, var3.m_targetAlpha);
      }

      ColorInfo var5 = this.sanitizeLightInfo(var2, var4);
      boolean var6 = FBORenderCutaways.getInstance().isForceRenderSquare(var2, var4);
      IsoGridSquare var7;
      if (var6) {
         var7 = this.cell.getGridSquare(var4.x, var4.y, var4.z - 1);
         if (var7 != null) {
            var5 = this.sanitizeLightInfo(var2, var7);
         }
      }

      if (var1 instanceof IsoTree) {
         ((IsoTree)var1).bRenderFlag = this.isTranslucentTree(var1);
      }

      IndieGL.glDepthMask(true);
      if (this.isRoofTileWithPossibleSeamSameLevel(var4, var1.sprite, IsoDirections.E)) {
         var7 = var4.getAdjacentSquare(IsoDirections.E);
         this.renderJoinedRoofTile(var2, var1, var7, IsoDirections.E);
      }

      if (this.isRoofTileWithPossibleSeamSameLevel(var4, var1.sprite, IsoDirections.S)) {
         var7 = var4.getAdjacentSquare(IsoDirections.S);
         this.renderJoinedRoofTile(var2, var1, var7, IsoDirections.S);
      }

      if (this.isRoofTileWithPossibleSeamBelow(var4, var1.sprite, IsoDirections.E)) {
         var7 = this.cell.getGridSquare(var4.x + 1, var4.y, var4.z - 1);
         this.renderJoinedRoofTile(var2, var1, var7, IsoDirections.E);
      }

      if (this.isRoofTileWithPossibleSeamBelow(var4, var1.sprite, IsoDirections.S)) {
         var7 = this.cell.getGridSquare(var4.x, var4.y + 1, var4.z - 1);
         this.renderJoinedRoofTile(var2, var1, var7, IsoDirections.S);
      }

      if (var1 instanceof IsoWindow var23) {
         IsoGridSquare var8 = var4.nav[IsoDirections.N.index()];
         IsoGridSquare var9 = var4.nav[IsoDirections.S.index()];
         IsoGridSquare var10 = var4.nav[IsoDirections.W.index()];
         IsoGridSquare var11 = var4.nav[IsoDirections.E.index()];
         int var12 = var4.getPlayerCutawayFlag(var2, this.currentTimeMillis);
         int var13 = var8 == null ? 0 : var8.getPlayerCutawayFlag(var2, this.currentTimeMillis);
         int var14 = var9 == null ? 0 : var9.getPlayerCutawayFlag(var2, this.currentTimeMillis);
         int var15 = var10 == null ? 0 : var10.getPlayerCutawayFlag(var2, this.currentTimeMillis);
         int var16 = var11 == null ? 0 : var11.getPlayerCutawayFlag(var2, this.currentTimeMillis);
         byte var17 = 0;
         Object var18 = null;
         boolean var19 = false;
         boolean var20 = false;
         boolean var21 = false;
         boolean var22 = false;
         if (var23.getNorth() && var1 == var4.getWall(true)) {
            IsoGridSquare.CircleStencil = false;
            var4.DoWallLightingN(var1, var17, var12, var13, var14, var15, var16, var19, var21, (Shader)var18);
            return;
         }

         if (!var23.getNorth() && var1 == var4.getWall(false)) {
            IsoGridSquare.CircleStencil = false;
            var4.DoWallLightingW(var1, var17, var12, var13, var14, var15, var16, var20, var22, (Shader)var18);
            return;
         }
      }

      var1.render((float)var4.x, (float)var4.y, (float)var4.z, var5, true, false, (Shader)null);
   }

   boolean isRoofTileset(IsoSprite var1) {
      if (var1 == null) {
         return false;
      } else {
         return var1.getRoofProperties() != null;
      }
   }

   boolean isRoofTileWithPossibleSeamSameLevel(IsoGridSquare var1, IsoSprite var2, IsoDirections var3) {
      if (var2 == null) {
         return false;
      } else if (var3 == IsoDirections.E && PZMath.coordmodulo(var1.x, 8) != 7) {
         return false;
      } else if (var3 == IsoDirections.S && PZMath.coordmodulo(var1.y, 8) != 7) {
         return false;
      } else {
         return !this.isRoofTileset(var2) ? false : var2.getRoofProperties().hasPossibleSeamSameLevel(var3);
      }
   }

   boolean isRoofTileWithPossibleSeamBelow(IsoGridSquare var1, IsoSprite var2, IsoDirections var3) {
      if (var2 == null) {
         return false;
      } else {
         return !this.isRoofTileset(var2) ? false : var2.getRoofProperties().hasPossibleSeamLevelBelow(var3);
      }
   }

   boolean areRoofTilesJoinedSameLevel(IsoSprite var1, IsoSprite var2, IsoDirections var3) {
      if (!this.isRoofTileset(var1)) {
         return false;
      } else if (!this.isRoofTileset(var2)) {
         return false;
      } else if (var3 == IsoDirections.E) {
         return var1.getRoofProperties().isJoinedSameLevelEast(var2.getRoofProperties());
      } else {
         return var3 == IsoDirections.S ? var1.getRoofProperties().isJoinedSameLevelSouth(var2.getRoofProperties()) : false;
      }
   }

   boolean areRoofTilesJoinedLevelBelow(IsoSprite var1, IsoSprite var2, IsoDirections var3) {
      if (!this.isRoofTileset(var1)) {
         return false;
      } else if (!this.isRoofTileset(var2)) {
         return false;
      } else if (var3 == IsoDirections.E) {
         return var1.getRoofProperties().isJoinedLevelBelowEast(var2.getRoofProperties());
      } else {
         return var3 == IsoDirections.S ? var1.getRoofProperties().isJoinedLevelBelowSouth(var2.getRoofProperties()) : false;
      }
   }

   void renderJoinedRoofTile(int var1, IsoObject var2, IsoGridSquare var3, IsoDirections var4) {
      if (var3 != null) {
         IsoGridSquare var5 = var2.getSquare();

         for(int var6 = 0; var6 < var3.getObjects().size(); ++var6) {
            IsoObject var7 = (IsoObject)var3.getObjects().get(var6);
            if (var2.getRenderInfo(var1).m_layer != var7.getRenderInfo(var1).m_layer) {
            }

            if (var5.z == var3.z) {
               if (!this.areRoofTilesJoinedSameLevel(var2.sprite, var7.sprite, var4)) {
                  continue;
               }
            } else if (!this.areRoofTilesJoinedLevelBelow(var2.sprite, var7.sprite, var4)) {
               continue;
            }

            if (var7.getRenderInfo(var1).m_targetAlpha == 0.0F || var5.chunk != var3.chunk) {
               this.calculateObjectRenderInfo(var7.square);
               if (var7.getRenderInfo(var1).m_targetAlpha == 0.0F) {
                  continue;
               }
            }

            if (!(var7.getRenderInfo(var1).m_targetAlpha < 1.0F)) {
               var7.renderSquareOverride = var5;
               var7.renderDepthAdjust = -1.0E-5F;
               var7.sx = 0.0F;
               if (this.bRenderTranslucentOnly) {
                  var7.setTargetAlpha(var1, var7.getRenderInfo(var1).m_targetAlpha);
               } else {
                  var7.setAlphaAndTarget(var1, var7.getRenderInfo(var1).m_targetAlpha);
               }

               var7.render((float)var3.x, (float)var3.y, (float)var3.z, this.sanitizeLightInfo(var1, var3), true, false, (Shader)null);
               var7.sx = 0.0F;
               var7.renderSquareOverride = null;
               var7.renderDepthAdjust = 0.0F;
               break;
            }
         }

      }
   }

   void renderWorldInventoryObjects(IsoGridSquare var1, IsoGridSquare var2, boolean var3) {
      if (this.shouldRenderSquare(var2)) {
         this.tempWorldInventoryObjects.clear();
         this.tempWorldInventoryObjects.addAll(var1.getWorldObjects());
         Arrays.sort((IsoWorldInventoryObject[])this.tempWorldInventoryObjects.getElements(), 0, this.tempWorldInventoryObjects.size(), (var0, var1x) -> {
            float var2 = var0.xoff * var0.xoff + var0.yoff * var0.yoff;
            float var3 = var1x.xoff * var1x.xoff + var1x.yoff * var1x.yoff;
            if (var2 == var3) {
               return 0;
            } else {
               return var2 > var3 ? 1 : -1;
            }
         });
         int var4 = IsoCamera.frameState.playerIndex;

         for(int var5 = 0; var5 < this.tempWorldInventoryObjects.size(); ++var5) {
            IsoWorldInventoryObject var6 = (IsoWorldInventoryObject)this.tempWorldInventoryObjects.get(var5);
            ObjectRenderInfo var7 = var6.getRenderInfo(var4);
            if (var7.m_layer == ObjectRenderLayer.WorldInventoryObject && (!var3 || var2 == var6.getRenderSquare())) {
               this.renderWorldInventoryObject(var6, var3);
            }
         }

      }
   }

   void renderWorldInventoryObject(IsoWorldInventoryObject var1, boolean var2) {
      IsoGridSquare var3 = var1.getSquare();
      int var4 = IsoCamera.frameState.playerIndex;
      if (var2) {
         IsoGridSquare var5 = var1.getRenderSquare();
         FBORenderLevels var6;
         if (!(var1.zoff < 0.01F) && (this.isTableTopObjectFadedOut(var4, var3) || this.isTableTopObjectSquareCutaway(var4, var3, var1.zoff))) {
            var6 = var5.chunk.getRenderLevels(var4);
            ArrayList var9 = var6.getCachedSquares_Items(var5.z);
            if (!var9.contains(var3)) {
               var9.add(var3);
            }

            return;
         }

         if (!var1.getItem().getScriptItem().isWorldRender()) {
            return;
         }

         if (Core.getInstance().isOption3DGroundItem() && ItemModelRenderer.itemHasModel(var1.getItem())) {
            var6 = var5.chunk.getRenderLevels(var4);
            FBORenderChunk var7 = var6.getFBOForLevel(var5.z, Core.getInstance().getZoom(var4));
            FBORenderItems.getInstance().render(var7.index, var1);
            return;
         }
      }

      if (this.bRenderTranslucentOnly) {
         if (var1.zoff < 0.01F) {
            return;
         }

         if (!this.isTableTopObjectFadedOut(var4, var3) && !this.isTableTopObjectSquareCutaway(var4, var3, var1.zoff)) {
            return;
         }
      }

      ColorInfo var8 = var3.getLightInfo(var4);
      var1.render((float)var3.x, (float)var3.y, (float)var3.z, var8, true, false, (Shader)null);
   }

   boolean isTableTopObjectSquareCutaway(int var1, IsoGridSquare var2, float var3) {
      IsoGridSquare var4 = var2.nav[IsoDirections.S.index()];
      IsoGridSquare var5 = var2.nav[IsoDirections.E.index()];
      boolean var6 = var2.getPlayerCutawayFlag(var1, this.currentTimeMillis) != 0;
      boolean var7 = var4 != null && var4.getPlayerCutawayFlag(var1, this.currentTimeMillis) != 0;
      boolean var8 = var5 != null && var5.getPlayerCutawayFlag(var1, this.currentTimeMillis) != 0;
      if (IsoCamera.frameState.CamCharacterSquare == null || IsoCamera.frameState.CamCharacterSquare.getRoom() != var2.getRoom()) {
         boolean var9 = var6;
         if (var2.Is(IsoFlagType.cutN) && var2.Is(IsoFlagType.cutW)) {
            var9 = var6 | var7 | var8;
         } else if (var2.Is(IsoFlagType.cutW)) {
            var9 = var6 | var7;
         } else if (var2.Is(IsoFlagType.cutN)) {
            var9 = var6 | var8;
         }

         if (var9) {
            return true;
         }
      }

      return false;
   }

   boolean isTableTopObjectFadedOut(int var1, IsoGridSquare var2) {
      PerPlayerData var3 = this.perPlayerData[var1];
      return this.listContainsLocation(var3.squaresObscuringPlayer, var2.x, var2.y, var2.z) || this.listContainsLocation(var3.fadingInSquares, var2.x, var2.y, var2.z);
   }

   void renderMinusFloorSE(IsoGridSquare var1) {
      int var2 = IsoCamera.frameState.playerIndex;
      IsoObject[] var3 = (IsoObject[])var1.getObjects().getElements();
      int var4 = var1.getObjects().size();

      for(int var5 = var4 - 1; var5 >= 0; --var5) {
         IsoObject var6 = var3[var5];
         ObjectRenderInfo var7 = var6.getRenderInfo(var2);
         if (var7.m_layer == ObjectRenderLayer.MinusFloorSE) {
            this.renderMinusFloorSE(var6);
         }
      }

   }

   void renderMinusFloorSE(IsoObject var1) {
      this.renderMinusFloor(var1);
   }

   void renderTranslucentFloor(IsoGridSquare var1) {
      int var2 = IsoCamera.frameState.playerIndex;
      IsoObject[] var3 = (IsoObject[])var1.getObjects().getElements();
      int var4 = var1.getObjects().size();

      for(int var5 = 0; var5 < var4; ++var5) {
         IsoObject var6 = var3[var5];
         ObjectRenderInfo var7 = var6.getRenderInfo(var2);
         if (var7.m_layer == ObjectRenderLayer.TranslucentFloor) {
            this.renderTranslucent(var6);
         }
      }

   }

   void renderTranslucent(IsoGridSquare var1, boolean var2) {
      int var3 = IsoCamera.frameState.playerIndex;
      IsoObject[] var4 = (IsoObject[])var1.getObjects().getElements();
      int var5 = var1.getObjects().size();

      for(int var6 = 0; var6 < var5; ++var6) {
         IsoObject var7 = var4[var6];
         ObjectRenderInfo var8 = var7.getRenderInfo(var3);
         if (var8.m_layer == ObjectRenderLayer.Translucent) {
            PropertyContainer var9 = var7.getProperties();
            if (var9 != null) {
               boolean var10 = var9.Is(IsoFlagType.attachedE) || var9.Is(IsoFlagType.attachedS);
               if (var10 != var2) {
                  continue;
               }
            }

            this.renderTranslucent(var7);
         }
      }

   }

   public void renderTranslucent(IsoObject var1) {
      IndieGL.glDefaultBlendFunc();
      IsoSprite var2 = var1.getSprite();
      if (var2 != null && var2.getProperties().Is(IsoFlagType.transparentFloor)) {
         this.renderFloor(var1);
      } else if (!(var1 instanceof IsoDoor) && (!(var1 instanceof IsoThumpable) || !((IsoThumpable)var1).isDoor())) {
         if (var1.getType() != IsoObjectType.doorFrW && var1.getType() != IsoObjectType.doorFrN) {
            if (var2 != null && var2.solidfloor && var1.square.getWater() != null && var1.square.getWater().isbShore()) {
               this.renderFloor(var1);
            } else {
               if (var2 == null || !var2.cutN && !var2.cutW) {
                  var1.getRenderInfo(IsoCamera.frameState.playerIndex).m_targetAlpha = this.calculateObjectTargetAlpha_NotDoorOrWall(var1);
               } else {
                  var1.getRenderInfo(IsoCamera.frameState.playerIndex).m_targetAlpha = this.calculateObjectTargetAlpha_DoorOrWall(var1);
               }

               IsoObjectType var3 = IsoObjectType.MAX;
               if (var1.sprite != null) {
                  var3 = var1.sprite.getType();
               }

               boolean var4 = var3 == IsoObjectType.doorFrW || var3 == IsoObjectType.doorW || var1.sprite != null && var1.sprite.cutW;
               boolean var5 = var3 == IsoObjectType.doorFrN || var3 == IsoObjectType.doorN || var1.sprite != null && var1.sprite.cutN;
               if (var1.sprite != null && (var4 || var5)) {
                  if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.DoorsAndWalls.getValue()) {
                     this.renderMinusFloor_DoorOrWall(var1);
                  }
               } else if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Objects.getValue()) {
                  this.renderMinusFloor_NotDoorOrWall(var1);
               }

               if (var1.hasAnimatedAttachments()) {
                  this.renderAnimatedAttachments(var1);
               }

            }
         } else {
            this.renderMinusFloor_DoorOrWall(var1);
         }
      } else {
         var1.sx = 0.0F;
         this.renderMinusFloor_DoorOrWall(var1);
      }
   }

   void renderAnimatedAttachments(int var1) {
      this.bRenderTranslucentOnly = true;
      PerPlayerData var2 = this.perPlayerData[var1];

      for(int var3 = 0; var3 < var2.chunksWith_AnimatedAttachments.size(); ++var3) {
         IsoChunk var4 = (IsoChunk)var2.chunksWith_AnimatedAttachments.get(var3);
         this.renderOneChunk_AnimatedAttachments(var1, var4);
      }

   }

   void renderOneChunk_AnimatedAttachments(int var1, IsoChunk var2) {
      IndieGL.enableDepthTest();
      IndieGL.glDepthFunc(515);
      IndieGL.glDepthMask(false);

      for(int var3 = var2.minLevel; var3 <= var2.maxLevel; ++var3) {
         this.renderOneLevel_AnimatedAttachments(var1, var2, var3);
      }

      IndieGL.glDepthMask(false);
      IndieGL.glDepthFunc(519);
   }

   private void renderOneLevel_AnimatedAttachments(int var1, IsoChunk var2, int var3) {
      FBORenderLevels var4 = var2.getRenderLevels(var1);
      if (var4.isOnScreen(var3)) {
         FBORenderCutaways.ChunkLevelData var5 = var2.getCutawayDataForLevel(var3);
         ArrayList var6 = var4.getCachedSquares_AnimatedAttachments(var3);

         for(int var7 = 0; var7 < var6.size(); ++var7) {
            IsoGridSquare var8 = (IsoGridSquare)var6.get(var7);
            if (var8.z == var3 && var5.shouldRenderSquare(var1, var8) && var8.IsOnScreen()) {
               this.renderAnimatedAttachments(var8);
            }
         }

      }
   }

   void renderAnimatedAttachments(IsoGridSquare var1) {
      this.bRenderAnimatedAttachments = true;
      int var2 = IsoCamera.frameState.playerIndex;
      IsoObject[] var3 = (IsoObject[])var1.getObjects().getElements();
      int var4 = var1.getObjects().size();

      for(int var5 = 0; var5 < var4; ++var5) {
         IsoObject var6 = var3[var5];
         if (var6.getRenderInfo(var2).m_layer != ObjectRenderLayer.Translucent && var6.hasAnimatedAttachments()) {
            this.renderAnimatedAttachments(var6);
         }
      }

      this.bRenderAnimatedAttachments = false;
   }

   public void renderAnimatedAttachments(IsoObject var1) {
      int var2 = IsoCamera.frameState.playerIndex;
      ColorInfo var3 = var1.square.getLightInfo(var2);
      if (DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
         this.defColorInfo.set(1.0F, 1.0F, 1.0F, var3.a);
         var3 = this.defColorInfo;
      }

      IndieGL.glDefaultBlendFunc();
      var1.renderAnimatedAttachments(var1.getX(), var1.getY(), var1.getZ(), var3);
   }

   void renderFlies(int var1) {
      this.bRenderTranslucentOnly = true;
      PerPlayerData var2 = this.perPlayerData[var1];

      for(int var3 = 0; var3 < var2.chunksWith_Flies.size(); ++var3) {
         IsoChunk var4 = (IsoChunk)var2.chunksWith_Flies.get(var3);
         this.renderOneChunk_Flies(var1, var4);
      }

   }

   void renderOneChunk_Flies(int var1, IsoChunk var2) {
      IndieGL.enableDepthTest();
      IndieGL.glDepthFunc(515);
      IndieGL.glDepthMask(false);
      IndieGL.enableBlend();
      IndieGL.glBlendFunc(770, 771);

      for(int var3 = var2.minLevel; var3 <= var2.maxLevel; ++var3) {
         this.renderOneLevel_Flies(var1, var2, var3);
      }

      IndieGL.glDepthMask(false);
      IndieGL.glDepthFunc(519);
   }

   private void renderOneLevel_Flies(int var1, IsoChunk var2, int var3) {
      FBORenderLevels var4 = var2.getRenderLevels(var1);
      if (var4.isOnScreen(var3)) {
         FBORenderCutaways.ChunkLevelData var5 = var2.getCutawayDataForLevel(var3);
         ArrayList var6 = var4.getCachedSquares_Flies(var3);

         for(int var7 = 0; var7 < var6.size(); ++var7) {
            IsoGridSquare var8 = (IsoGridSquare)var6.get(var7);
            if (var8.z == var3 && var5.shouldRenderSquare(var1, var8) && var8.IsOnScreen() && var8.hasFlies()) {
               CorpseFlies.render(var8.x, var8.y, var8.z);
            }
         }

      }
   }

   void updateChunkLighting(int var1) {
      if (!DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
         if (DebugOptions.instance.FBORenderChunk.UpdateSquareLightInfo.getValue()) {
            PerPlayerData var2 = this.perPlayerData[var1];
            if (var2.lightingUpdateCounter != LightingJNI.getUpdateCounter(var1)) {
               var2.lightingUpdateCounter = LightingJNI.getUpdateCounter(var1);
               int var3 = 0;
               this.sortedChunks.clear();
               this.sortedChunks.addAll(var2.onScreenChunks);
               this.sortedChunks.sort(Comparator.comparingInt((var0) -> {
                  return var0.lightingUpdateCounter;
               }));

               for(int var4 = 0; var4 < this.sortedChunks.size(); ++var4) {
                  IsoChunk var5 = (IsoChunk)this.sortedChunks.get(var4);
                  boolean var6 = false;

                  for(int var7 = var5.minLevel; var7 <= var5.maxLevel; ++var7) {
                     if (this.updateChunkLevelLighting(var1, var5, var7)) {
                        var6 = true;
                        var5.lightingUpdateCounter = var2.lightingUpdateCounter;
                     }
                  }

                  if (DebugOptions.instance.LightingSplitUpdate.getValue() && var6) {
                     ++var3;
                     if (var3 >= 5) {
                        return;
                     }
                  }
               }

            }
         }
      }
   }

   boolean updateChunkLevelLighting(int var1, IsoChunk var2, int var3) {
      if (var3 >= var2.minLevel && var3 <= var2.maxLevel) {
         FBORenderLevels var4 = var2.getRenderLevels(var1);
         if (!var4.isOnScreen(var3)) {
            return false;
         } else if (!LightingJNI.getChunkDirty(var1, var2.wx, var2.wy, var3 + 32)) {
            return false;
         } else {
            FBORenderCutaways.ChunkLevelData var5 = var2.getCutawayDataForLevel(var3);
            IsoGridSquare[] var6 = var2.squares[var2.squaresIndexOfLevel(var3)];

            for(int var7 = 0; var7 < var6.length; ++var7) {
               IsoGridSquare var8 = var6[var7];
               if (var8 != null && var5.shouldRenderSquare(var1, var8)) {
                  var8.cacheLightInfo();
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   void renderOneLevel_Blood(IsoChunk var1, int var2, int var3, int var4, int var5, int var6) {
      if (DebugOptions.instance.Terrain.RenderTiles.BloodDecals.getValue()) {
         int var7 = Core.getInstance().getOptionBloodDecals();
         if (var7 != 0) {
            float var8 = (float)GameTime.getInstance().getWorldAgeHours();
            int var9 = IsoCamera.frameState.playerIndex;
            FBORenderCutaways.ChunkLevelData var10 = var1.getCutawayDataForLevel(var2);
            int var11;
            if (this.splatByType.isEmpty()) {
               for(var11 = 0; var11 < IsoFloorBloodSplat.FloorBloodTypes.length; ++var11) {
                  this.splatByType.add(new ArrayList());
               }
            }

            for(var11 = 0; var11 < IsoFloorBloodSplat.FloorBloodTypes.length; ++var11) {
               ((ArrayList)this.splatByType.get(var11)).clear();
            }

            IsoChunk var42 = var1;
            int var12 = var1.wx * 8;
            int var13 = var1.wy * 8;

            int var14;
            IsoFloorBloodSplat var15;
            for(var14 = 0; var14 < var42.FloorBloodSplatsFade.size(); ++var14) {
               var15 = (IsoFloorBloodSplat)var42.FloorBloodSplatsFade.get(var14);
               if ((var15.index < 1 || var15.index > 10 || IsoChunk.renderByIndex[var7 - 1][var15.index - 1] != 0) && !((float)var12 + var15.x < (float)var3) && !((float)var12 + var15.x > (float)var5) && !((float)var13 + var15.y < (float)var4) && !((float)var13 + var15.y > (float)var6) && PZMath.fastfloor(var15.z) == var2 && var15.Type >= 0 && var15.Type < IsoFloorBloodSplat.FloorBloodTypes.length) {
                  var15.chunk = var42;
                  ((ArrayList)this.splatByType.get(var15.Type)).add(var15);
               }
            }

            for(var14 = 0; var14 < var42.FloorBloodSplats.size(); ++var14) {
               var15 = (IsoFloorBloodSplat)var42.FloorBloodSplats.get(var14);
               if ((var15.index < 1 || var15.index > 10 || IsoChunk.renderByIndex[var7 - 1][var15.index - 1] != 0) && !((float)var12 + var15.x < (float)var3) && !((float)var12 + var15.x > (float)var5) && !((float)var13 + var15.y < (float)var4) && !((float)var13 + var15.y > (float)var6) && PZMath.fastfloor(var15.z) == var2 && var15.Type >= 0 && var15.Type < IsoFloorBloodSplat.FloorBloodTypes.length) {
                  var15.chunk = var42;
                  ((ArrayList)this.splatByType.get(var15.Type)).add(var15);
               }
            }

            for(var14 = 0; var14 < this.splatByType.size(); ++var14) {
               ArrayList var43 = (ArrayList)this.splatByType.get(var14);
               if (!var43.isEmpty()) {
                  String var16 = IsoFloorBloodSplat.FloorBloodTypes[var14];
                  IsoSprite var17 = null;
                  if (!IsoFloorBloodSplat.SpriteMap.containsKey(var16)) {
                     IsoSprite var18 = IsoSprite.CreateSprite(IsoSpriteManager.instance);
                     var18.LoadSingleTexture(var16);
                     IsoFloorBloodSplat.SpriteMap.put(var16, var18);
                     var17 = var18;
                  } else {
                     var17 = (IsoSprite)IsoFloorBloodSplat.SpriteMap.get(var16);
                  }

                  for(int var44 = 0; var44 < var43.size(); ++var44) {
                     IsoFloorBloodSplat var19 = (IsoFloorBloodSplat)var43.get(var44);
                     ColorInfo var20 = this.defColorInfo;
                     var20.r = 1.0F;
                     var20.g = 1.0F;
                     var20.b = 1.0F;
                     var20.a = 0.27F;
                     float var21 = (var19.x + var19.y / var19.x) * (float)(var19.Type + 1);
                     float var22 = var21 * var19.x / var19.y * (float)(var19.Type + 1) / (var21 + var19.y);
                     float var23 = var22 * var21 * var22 * var19.x / (var19.y + 2.0F);
                     var21 *= 42367.543F;
                     var22 *= 6367.123F;
                     var23 *= 23367.133F;
                     var21 %= 1000.0F;
                     var22 %= 1000.0F;
                     var23 %= 1000.0F;
                     var21 /= 1000.0F;
                     var22 /= 1000.0F;
                     var23 /= 1000.0F;
                     if (var21 > 0.25F) {
                        var21 = 0.25F;
                     }

                     var20.r -= var21 * 2.0F;
                     var20.g -= var21 * 2.0F;
                     var20.b -= var21 * 2.0F;
                     var20.r += var22 / 3.0F;
                     var20.g -= var23 / 3.0F;
                     var20.b -= var23 / 3.0F;
                     float var24 = var8 - var19.worldAge;
                     if (var24 >= 0.0F && var24 < 72.0F) {
                        float var25 = 1.0F - var24 / 72.0F;
                        var20.r *= 0.2F + var25 * 0.8F;
                        var20.g *= 0.2F + var25 * 0.8F;
                        var20.b *= 0.2F + var25 * 0.8F;
                        var20.a *= 0.25F + var25 * 0.75F;
                     } else {
                        var20.r *= 0.2F;
                        var20.g *= 0.2F;
                        var20.b *= 0.2F;
                        var20.a *= 0.25F;
                     }

                     if (var19.fade > 0) {
                        var20.a *= (float)var19.fade / ((float)PerformanceSettings.getLockFPS() * 5.0F);
                        if (--var19.fade == 0) {
                           var19.chunk.FloorBloodSplatsFade.remove(var19);
                        }
                     }

                     IsoGridSquare var45 = var19.chunk.getGridSquare(PZMath.fastfloor(var19.x), PZMath.fastfloor(var19.y), PZMath.fastfloor(var19.z));
                     if (var10.shouldRenderSquare(var9, var45)) {
                        if (var45 != null) {
                           int var26 = var45.getVertLight(0, var9);
                           int var27 = var45.getVertLight(1, var9);
                           int var28 = var45.getVertLight(2, var9);
                           int var29 = var45.getVertLight(3, var9);
                           float var30 = Color.getRedChannelFromABGR(var26);
                           float var31 = Color.getGreenChannelFromABGR(var26);
                           float var32 = Color.getBlueChannelFromABGR(var26);
                           float var33 = Color.getRedChannelFromABGR(var27);
                           float var34 = Color.getGreenChannelFromABGR(var27);
                           float var35 = Color.getBlueChannelFromABGR(var27);
                           float var36 = Color.getRedChannelFromABGR(var28);
                           float var37 = Color.getGreenChannelFromABGR(var28);
                           float var38 = Color.getBlueChannelFromABGR(var28);
                           float var39 = Color.getRedChannelFromABGR(var29);
                           float var40 = Color.getGreenChannelFromABGR(var29);
                           float var41 = Color.getBlueChannelFromABGR(var29);
                           var20.r *= (var30 + var33 + var36 + var39) / 4.0F;
                           var20.g *= (var31 + var34 + var37 + var40) / 4.0F;
                           var20.b *= (var32 + var35 + var38 + var41) / 4.0F;
                        }

                        var17.renderBloodSplat((float)(var19.chunk.wx * 8) + var19.x, (float)(var19.chunk.wy * 8) + var19.y, var19.z, var20);
                     }
                  }
               }
            }

         }
      }
   }

   void renderOneLevel_Blood(IsoChunk var1, int var2, int var3, int var4) {
      IsoChunk var5 = IsoWorld.instance.CurrentCell.getChunk(var1.wx + var2, var1.wy + var3);
      if (var5 != null) {
         byte var6 = 8;
         int var7 = var1.wx * var6 - 1;
         int var8 = var1.wy * var6 - 1;
         int var9 = (var1.wx + 1) * var6 + 1;
         int var10 = (var1.wy + 1) * var6 + 1;
         this.renderOneLevel_Blood(var5, var4, var7, var8, var9, var10);
      }
   }

   private void recalculateAnyGridStacks(int var1) {
      IsoPlayer var2 = IsoPlayer.players[var1];
      if (var2.dirtyRecalcGridStack) {
         var2.dirtyRecalcGridStack = false;
         WeatherFxMask.setDiamondIterDone(var1);
      }
   }

   private boolean hasAnyDirtyChunkTextures(int var1) {
      float var2 = Core.getInstance().getZoom(var1);
      PerPlayerData var3 = this.perPlayerData[var1];

      for(int var4 = 0; var4 < var3.onScreenChunks.size(); ++var4) {
         IsoChunk var5 = (IsoChunk)var3.onScreenChunks.get(var4);
         FBORenderLevels var6 = var5.getRenderLevels(var1);

         for(int var7 = var5.minLevel; var7 <= var5.maxLevel; ++var7) {
            if (var6.isOnScreen(var7) && var6.isDirty(var7, var2)) {
               return true;
            }
         }
      }

      return false;
   }

   private void calculateOccludingSquares(int var1) {
      float var2 = Core.getInstance().getZoom(var1);
      PerPlayerData var3 = this.perPlayerData[var1];

      for(int var4 = 0; var4 < var3.onScreenChunks.size(); ++var4) {
         IsoChunk var5 = (IsoChunk)var3.onScreenChunks.get(var4);
         FBORenderLevels var6 = var5.getRenderLevels(var1);

         for(int var7 = var5.minLevel; var7 <= var5.maxLevel; ++var7) {
            if (var6.isOnScreen(var7)) {
               if (!var6.isDirty(var7, var2)) {
               }

               FBORenderCutaways.ChunkLevelData var8 = var5.getCutawayData().getDataForLevel(var7);
               boolean var9 = var8.calculateOccludingSquares(var1, var3.occludedGridX1, var3.occludedGridY1, var3.occludedGridX2, var3.occludedGridY2, var3.occludedGrid);
               if (var9) {
                  FBORenderOcclusion.getInstance().invalidateOverlappedChunkLevels(var1, var5, var7);
               }
            }
         }
      }

   }

   private int calculateRenderedSquaresCount(int var1, IsoChunk var2, int var3) {
      int var4 = var2.getRenderLevels(var1).getMinLevel(var3);
      int var5 = var2.getRenderLevels(var1).getMaxLevel(var3);
      int var6 = 0;

      for(int var7 = var4; var7 <= var5; ++var7) {
         FBORenderCutaways.ChunkLevelData var8 = var2.getCutawayDataForLevel(var7);
         IsoGridSquare[] var9 = var2.getSquaresForLevel(var7);

         for(int var10 = 0; var10 < var9.length; ++var10) {
            IsoGridSquare var11 = var9[var10];
            if (var8.shouldRenderSquare(var1, var11) && !FBORenderOcclusion.getInstance().isOccluded(var11.x, var11.y, var7)) {
               if (DebugOptions.instance.CheapOcclusionCount.getValue()) {
                  return 1;
               }

               ++var6;
            }
         }
      }

      return var6;
   }

   private void prepareChunksForUpdating(int var1) {
      float var2 = Core.getInstance().getZoom(var1);
      PerPlayerData var3 = this.perPlayerData[var1];

      for(int var4 = 0; var4 < var3.onScreenChunks.size(); ++var4) {
         IsoChunk var5 = (IsoChunk)var3.onScreenChunks.get(var4);
         FBORenderLevels var6 = var5.getRenderLevels(var1);

         for(int var7 = var5.minLevel; var7 <= var5.maxLevel; ++var7) {
            if (var7 == var6.getMinLevel(var7) && var6.isOnScreen(var7) && var6.isDirty(var7, var2)) {
               this.prepareChunkForUpdating(var1, var5, var7);
            }
         }
      }

   }

   void prepareChunkForUpdating(int var1, IsoChunk var2, int var3) {
      if (var2 != null) {
         FBORenderLevels var4 = var2.getRenderLevels(var1);
         if (var4.isOnScreen(var3)) {
            int var5 = var4.getMinLevel(var3);
            int var6 = var4.getMaxLevel(var3);

            for(int var7 = var5; var7 <= var6; ++var7) {
               FBORenderCutaways.ChunkLevelData var8 = var2.getCutawayDataForLevel(var7);

               for(int var9 = 0; var9 < 8; ++var9) {
                  for(int var10 = 0; var10 < 8; ++var10) {
                     IsoGridSquare var11 = var2.getGridSquare(var10, var9, var7);
                     var8.m_squareFlags[var1][var10 + var9 * 8] = 0;
                     if (var11 != null) {
                        var11.cacheLightInfo();
                        if (var11.getLightInfo(var1) != null && this.shouldRenderSquare(var11)) {
                           byte[] var10000 = var8.m_squareFlags[var1];
                           var10000[var10 + var9 * 8] = (byte)(var10000[var10 + var9 * 8] | 1);
                        }
                     }
                  }
               }
            }

         }
      }
   }

   boolean shouldRenderSquare(IsoGridSquare var1) {
      if (var1 == null) {
         return false;
      } else {
         int var2 = IsoCamera.frameState.playerIndex;
         if (var1.getLightInfo(var2) != null && var1.lighting[var2] != null) {
            return FBORenderCutaways.getInstance().shouldRenderBuildingSquare(var2, var1);
         } else {
            return false;
         }
      }
   }

   void renderTranslucentFloorObjects(int var1, Shader var2, Shader var3, long var4) {
      if (DebugOptions.instance.FBORenderChunk.RenderTranslucentFloor.getValue()) {
         this.bRenderTranslucentOnly = true;
         PerPlayerData var6 = this.perPlayerData[var1];

         for(int var7 = 0; var7 < var6.chunksWith_TranslucentFloor.size(); ++var7) {
            IsoChunk var8 = (IsoChunk)var6.chunksWith_TranslucentFloor.get(var7);
            this.renderOneChunk_TranslucentFloor(var8, var1, var2, var3, var4);
         }

      }
   }

   void renderOneChunk_TranslucentFloor(IsoChunk var1, int var2, Shader var3, Shader var4, long var5) {
      if (var1 != null) {
         if (!var1.bLightingNeverDone[var2]) {
            IndieGL.enableDepthTest();
            IndieGL.glDepthFunc(515);
            IndieGL.glDepthMask(false);

            for(int var7 = var1.minLevel; var7 <= var1.maxLevel; ++var7) {
               this.renderOneLevel_TranslucentFloor(var2, var1, var7);
            }

            IndieGL.glDepthMask(false);
            IndieGL.glDepthFunc(519);
         }
      }
   }

   private void renderOneLevel_TranslucentFloor(int var1, IsoChunk var2, int var3) {
      FBORenderLevels var4 = var2.getRenderLevels(var1);
      if (var4.isOnScreen(var3)) {
         ArrayList var5 = var4.getCachedSquares_TranslucentFloor(var3);

         for(int var6 = 0; var6 < var5.size(); ++var6) {
            IsoGridSquare var7 = (IsoGridSquare)var5.get(var6);
            if (var7.z == var3 && var7.IsOnScreen()) {
               this.renderTranslucentFloor(var7);
            }
         }

      }
   }

   void renderTranslucentObjects(int var1, Shader var2, Shader var3, long var4) {
      if (DebugOptions.instance.FBORenderChunk.RenderTranslucentNonFloor.getValue()) {
         this.bRenderTranslucentOnly = true;
         PerPlayerData var6 = this.perPlayerData[var1];

         for(int var7 = 0; var7 < var6.chunksWith_TranslucentNonFloor.size(); ++var7) {
            IsoChunk var8 = (IsoChunk)var6.chunksWith_TranslucentNonFloor.get(var7);
            this.renderOneChunk_Translucent(var8, var1, var2, var3, var4);
         }

      }
   }

   void renderOneChunk_Translucent(IsoChunk var1, int var2, Shader var3, Shader var4, long var5) {
      if (var1 != null && var1.IsOnScreen(true)) {
         if (!var1.bLightingNeverDone[var2]) {
            IndieGL.enableDepthTest();
            IndieGL.glDepthFunc(515);
            IndieGL.glDepthMask(false);

            for(int var7 = var1.minLevel; var7 <= var1.maxLevel; ++var7) {
               this.renderOneLevel_Translucent(var2, var1, var7);
            }

            IndieGL.glDepthMask(false);
            IndieGL.glDepthFunc(519);
         }
      }
   }

   private void renderOneLevel_Translucent(int var1, IsoChunk var2, int var3) {
      FBORenderLevels var4 = var2.getRenderLevels(var1);
      if (var4.isOnScreen(var3)) {
         FBORenderCutaways.ChunkLevelData var5 = var2.getCutawayDataForLevel(var3);
         ArrayList var6 = var4.getCachedSquares_TranslucentNonFloor(var3);
         ArrayList var7 = var4.getCachedSquares_Items(var3);
         if (var6.size() + var7.size() != 0) {
            ArrayList var8 = this.tempSquares;
            var8.clear();
            GameProfiler.getInstance().invokeAndMeasure("Sort", () -> {
               var8.addAll(var7);

               for(int var3 = 0; var3 < var6.size(); ++var3) {
                  IsoGridSquare var4 = (IsoGridSquare)var6.get(var3);
                  if (!var8.contains(var4)) {
                     var8.add(var4);
                  }
               }

            });
            var8.sort((var0, var1x) -> {
               int var2 = IsoWorld.instance.getMetaGrid().getMaxX() * IsoCell.CellSizeInSquares;
               int var3 = var0.x + var0.y * var2;
               int var4 = var1x.x + var1x.y * var2;
               return var3 - var4;
            });

            for(int var9 = 0; var9 < var8.size(); ++var9) {
               IsoGridSquare var10 = (IsoGridSquare)var8.get(var9);
               if (var10.z == var3 && var5.shouldRenderSquare(var1, var10) && var10.IsOnScreen()) {
                  this.renderTranslucent(var10, false);
                  if (DebugOptions.instance.FBORenderChunk.ItemsInChunkTexture.getValue() && !var10.getWorldObjects().isEmpty()) {
                     if (var10.chunk == var2) {
                        this.renderWorldInventoryObjects(var10, var10, false);
                     } else {
                        IsoGridSquare var11 = var2.getGridSquare(0, 0, var10.z);
                        this.renderWorldInventoryObjects(var10, var11, false);
                     }
                  }

                  this.renderTranslucent(var10, true);
               }
            }

         }
      }
   }

   void renderCorpseShadows(int var1) {
      if (DebugOptions.instance.Terrain.RenderTiles.Shadows.getValue()) {
         if (Core.getInstance().getOptionCorpseShadows()) {
            PerPlayerData var2 = this.perPlayerData[var1];

            for(int var3 = 0; var3 < var2.onScreenChunks.size(); ++var3) {
               IsoChunk var4 = (IsoChunk)var2.onScreenChunks.get(var3);
               FBORenderLevels var5 = var4.getRenderLevels(var1);

               for(int var6 = var4.minLevel; var6 <= var4.maxLevel; ++var6) {
                  if (var5.isOnScreen(var6) && var6 == var5.getMinLevel(var6)) {
                     ArrayList var7 = var5.getCachedSquares_Corpses(var6);

                     for(int var8 = 0; var8 < var7.size(); ++var8) {
                        IsoGridSquare var9 = (IsoGridSquare)var7.get(var8);

                        for(int var10 = 0; var10 < var9.getStaticMovingObjects().size(); ++var10) {
                           IsoDeadBody var11 = (IsoDeadBody)Type.tryCastTo((IsoMovingObject)var9.getStaticMovingObjects().get(var10), IsoDeadBody.class);
                           if (var11 != null) {
                              var11.renderShadow();
                           }
                        }
                     }
                  }
               }
            }

         }
      }
   }

   void checkMannequinRenderDirection(int var1) {
      for(int var2 = 0; var2 < this.mannequinList.size(); ++var2) {
         IsoMannequin var3 = (IsoMannequin)this.mannequinList.get(var2);
         if (var3.getObjectIndex() == -1) {
            this.mannequinList.remove(var2--);
         } else {
            var3.checkRenderDirection(var1);
         }
      }

   }

   void renderMannequinShadows(int var1) {
      for(int var2 = 0; var2 < this.mannequinList.size(); ++var2) {
         IsoMannequin var3 = (IsoMannequin)this.mannequinList.get(var2);
         if (var3.getObjectIndex() == -1) {
            this.mannequinList.remove(var2--);
         } else if (this.shouldRenderSquare(var3.getSquare())) {
            var3.renderShadow(var3.getX() + 0.5F, var3.getY() + 0.5F, var3.getZ());
            if (var3.shouldRenderEachFrame()) {
               ColorInfo var4 = var3.getSquare().getLightInfo(var1);
               var3.render(var3.getX(), var3.getY(), var3.getZ(), var4, true, false, (Shader)null);
            }
         }
      }

   }

   void renderOpaqueObjectsEvent(int var1) {
      int var2;
      int var3;
      int var4;
      if (JoypadManager.instance.getFromPlayer(var1) == null) {
         if (UIManager.getPickedTile() == null) {
            return;
         }

         var2 = PZMath.fastfloor(UIManager.getPickedTile().x);
         var3 = PZMath.fastfloor(UIManager.getPickedTile().y);
         var4 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ);
      } else {
         var2 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterX);
         var3 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterY);
         var4 = PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ);
      }

      if (IsoWorld.instance.isValidSquare(var2, var3, var4)) {
         IsoGridSquare var5 = this.cell.getGridSquare(var2, var3, var4);
         LuaEventManager.triggerEvent("RenderOpaqueObjectsInWorld", var1, var2, var3, var4, var5);
      }

   }

   void renderMovingObjects() {
      this.bRenderTranslucentOnly = true;
      ArrayList var1 = IsoWorld.instance.getCell().getObjectList();

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         IsoMovingObject var3 = (IsoMovingObject)var1.get(var2);
         this.renderMovingObject(var3);
      }

      this.bRenderTranslucentOnly = false;
      SpriteRenderer.instance.renderQueued();
   }

   void renderMovingObject(IsoMovingObject var1) {
      int var2 = IsoCamera.frameState.playerIndex;
      if (var1.getClass() != IsoPlayer.class) {
         if (var1.getCurrentSquare() != null) {
            if (var1.isOnScreen()) {
               if (var1.getCurrentSquare().getLightInfo(var2) != null) {
                  if (this.shouldRenderSquare(var1.getCurrentSquare())) {
                     if (DebugOptions.instance.Terrain.RenderTiles.Shadows.getValue()) {
                        IsoGameCharacter var3 = (IsoGameCharacter)Type.tryCastTo(var1, IsoGameCharacter.class);
                        if (var3 != null) {
                           var3.renderShadow(var1.getX(), var1.getY(), var1.getZ());
                        }

                        BaseVehicle var4 = (BaseVehicle)Type.tryCastTo(var1, BaseVehicle.class);
                        if (var4 != null) {
                           var4.renderShadow();
                        }
                     }

                     var1.render(var1.getX(), var1.getY(), var1.getZ(), var1.getCurrentSquare().getLightInfo(var2), true, false, (Shader)null);
                  }
               }
            }
         }
      }
   }

   void renderWater(int var1) {
      if (DebugOptions.instance.Weather.WaterPuddles.getValue() && DebugOptions.instance.Terrain.RenderTiles.Water.getValue() && DebugOptions.instance.Terrain.RenderTiles.WaterBody.getValue()) {
         if (!(IsoCamera.frameState.CamCharacterZ < 0.0F)) {
            this.WaterSquares.clear();
            this.WaterAttachSquares.clear();
            this.FishSplashSquares.clear();
            PerPlayerData var2 = this.perPlayerData[var1];

            int var3;
            int var6;
            IsoObject var8;
            for(var3 = 0; var3 < var2.onScreenChunks.size(); ++var3) {
               IsoChunk var4 = (IsoChunk)var2.onScreenChunks.get(var3);
               if (0 >= var4.minLevel && 0 <= var4.maxLevel && var4.getRenderLevels(var1).isOnScreen(0)) {
                  FBORenderLevels var5 = var4.getRenderLevels(var1);

                  IsoGridSquare var7;
                  for(var6 = 0; var6 < var5.m_waterSquares.size(); ++var6) {
                     var7 = (IsoGridSquare)var5.m_waterSquares.get(var6);
                     if (var7.IsOnScreen()) {
                        var8 = var7.getFloor();
                        if (var8 == null || var8.getRenderInfo(var1).m_layer != ObjectRenderLayer.TranslucentFloor) {
                           if (IsoWater.getInstance().getShaderEnable() && var7.getWater() != null && var7.getWater().isValid()) {
                              this.WaterSquares.add(var7);
                           }

                           if (var7.shouldRenderFishSplash(var1)) {
                              this.FishSplashSquares.add(var7);
                           }
                        }
                     }
                  }

                  for(var6 = 0; var6 < var5.m_waterAttachSquares.size(); ++var6) {
                     var7 = (IsoGridSquare)var5.m_waterAttachSquares.get(var6);
                     if (var7.IsOnScreen() && IsoWater.getInstance().getShaderEnable() && var7.getWater() != null && var7.getWater().isValid()) {
                        this.WaterAttachSquares.add(var7);
                     }
                  }
               }
            }

            if (!this.WaterSquares.isEmpty()) {
               IsoWater.getInstance().render(this.WaterSquares);
            }

            for(var3 = 0; var3 < this.WaterAttachSquares.size(); ++var3) {
               IsoGridSquare var9 = (IsoGridSquare)this.WaterAttachSquares.get(var3);
               IsoObject[] var10 = (IsoObject[])var9.getObjects().getElements();
               var6 = var9.getObjects().size();

               for(int var11 = 0; var11 < var6; ++var11) {
                  var8 = var10[var11];
                  if (var8 != null && var8.getRenderInfo(var1).m_layer == ObjectRenderLayer.None && var8.getAttachedAnimSprite() != null && !var8.getAttachedAnimSprite().isEmpty()) {
                     this.bRenderTranslucentOnly = true;
                     var8.renderAttachedAndOverlaySprites(var8.dir, (float)var9.x, (float)var9.y, (float)var9.z, var9.getLightInfo(var1), true, false, (Shader)null, (Consumer)null);
                     this.bRenderTranslucentOnly = false;
                  }
               }
            }

            if (!this.FishSplashSquares.isEmpty()) {
               this.renderFishSplashes(var1, this.FishSplashSquares);
            }

         }
      }
   }

   void renderWaterShore(int var1) {
      if (DebugOptions.instance.Weather.WaterPuddles.getValue() && DebugOptions.instance.Terrain.RenderTiles.Water.getValue() && DebugOptions.instance.Terrain.RenderTiles.WaterShore.getValue()) {
         if (!(IsoCamera.frameState.CamCharacterZ < 0.0F)) {
            this.WaterSquares.clear();
            this.WaterAttachSquares.clear();
            PerPlayerData var2 = this.perPlayerData[var1];

            int var3;
            int var6;
            for(var3 = 0; var3 < var2.onScreenChunks.size(); ++var3) {
               IsoChunk var4 = (IsoChunk)var2.onScreenChunks.get(var3);
               if (0 >= var4.minLevel && 0 <= var4.maxLevel && var4.getRenderLevels(var1).isOnScreen(0)) {
                  FBORenderLevels var5 = var4.getRenderLevels(var1);

                  for(var6 = 0; var6 < var5.m_waterShoreSquares.size(); ++var6) {
                     IsoGridSquare var7 = (IsoGridSquare)var5.m_waterShoreSquares.get(var6);
                     if (var7.IsOnScreen() && IsoWater.getInstance().getShaderEnable() && var7.getWater() != null && var7.getWater().isbShore()) {
                        this.WaterSquares.add(var7);
                        this.WaterAttachSquares.add(var7);
                     }
                  }
               }
            }

            if (!this.WaterSquares.isEmpty()) {
               IsoWater.getInstance().renderShore(this.WaterSquares);
            }

            for(var3 = 0; var3 < this.WaterAttachSquares.size(); ++var3) {
               IsoGridSquare var9 = (IsoGridSquare)this.WaterAttachSquares.get(var3);
               IsoObject[] var10 = (IsoObject[])var9.getObjects().getElements();
               var6 = var9.getObjects().size();

               for(int var11 = 0; var11 < var6; ++var11) {
                  IsoObject var8 = var10[var11];
                  if (var8 != null && var8.getRenderInfo(var1).m_layer == ObjectRenderLayer.None && var8.getAttachedAnimSprite() != null && !var8.getAttachedAnimSprite().isEmpty()) {
                     this.bRenderTranslucentOnly = true;
                     var8.renderAttachedAndOverlaySprites(var8.dir, (float)var9.x, (float)var9.y, (float)var9.z, var9.getLightInfo(var1), true, false, (Shader)null, (Consumer)null);
                     this.bRenderTranslucentOnly = false;
                  }
               }
            }

         }
      }
   }

   void renderPuddles(int var1) {
      if (IsoPuddles.getInstance().shouldRenderPuddles()) {
         IsoChunkMap var2 = this.cell.ChunkMap[var1];
         int var3 = var2.maxHeight;
         if (Core.getInstance().getPerfPuddles() > 0) {
            var3 = 0;
         }

         PerPlayerData var4 = this.perPlayerData[var1];

         for(int var5 = 0; var5 <= var3; ++var5) {
            this.WaterSquares.clear();

            for(int var6 = 0; var6 < var4.onScreenChunks.size(); ++var6) {
               IsoChunk var7 = (IsoChunk)var4.onScreenChunks.get(var6);
               if (var5 >= var7.minLevel && var5 <= var7.maxLevel) {
                  FBORenderLevels var8 = var7.getRenderLevels(var1);
                  if (var8.isOnScreen(var5)) {
                     ArrayList var9 = var8.getCachedSquares_Puddles(var5);
                     if (!var9.isEmpty()) {
                        FBORenderCutaways.ChunkLevelData var10 = var7.getCutawayDataForLevel(var5);

                        for(int var11 = 0; var11 < var9.size(); ++var11) {
                           IsoGridSquare var12 = (IsoGridSquare)var9.get(var11);
                           if (var12.getZ() == var5 && var10.shouldRenderSquare(var1, var12) && var12.IsOnScreen()) {
                              IsoObject var13 = var12.getFloor();
                              if (var13 != null && (PerformanceSettings.PuddlesQuality >= 2 || var13.getRenderInfo(var1).m_layer != ObjectRenderLayer.TranslucentFloor)) {
                                 IsoPuddlesGeometry var14 = var12.getPuddles();
                                 if (var14 != null && var14.shouldRender()) {
                                    this.WaterSquares.add(var12);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            IsoPuddles.getInstance().render(this.WaterSquares, var5);
         }

      }
   }

   void renderPuddlesTranslucentFloorsOnly(int var1) {
      if (IsoPuddles.getInstance().shouldRenderPuddles()) {
         IsoChunkMap var2 = this.cell.ChunkMap[var1];
         int var3 = var2.maxHeight;
         if (Core.getInstance().getPerfPuddles() > 0) {
            var3 = 0;
         }

         PerPlayerData var4 = this.perPlayerData[var1];

         for(int var5 = 0; var5 <= var3; ++var5) {
            this.WaterSquares.clear();

            for(int var6 = 0; var6 < var4.onScreenChunks.size(); ++var6) {
               IsoChunk var7 = (IsoChunk)var4.onScreenChunks.get(var6);
               if (var5 >= var7.minLevel && var5 <= var7.maxLevel) {
                  FBORenderLevels var8 = var7.getRenderLevels(var1);
                  if (var8.isOnScreen(var5) && !var8.getCachedSquares_Puddles(var5).isEmpty()) {
                     FBORenderCutaways.ChunkLevelData var9 = var7.getCutawayDataForLevel(var5);
                     ArrayList var10 = var8.getCachedSquares_TranslucentFloor(var5);

                     for(int var11 = 0; var11 < var10.size(); ++var11) {
                        IsoGridSquare var12 = (IsoGridSquare)var10.get(var11);
                        if (var9.shouldRenderSquare(var1, var12) && var12.IsOnScreen()) {
                           IsoObject var13 = var12.getFloor();
                           if (var13 != null && var13.getRenderInfo(var1).m_layer == ObjectRenderLayer.TranslucentFloor) {
                              IsoPuddlesGeometry var14 = var12.getPuddles();
                              if (var14 != null && var14.shouldRender()) {
                                 this.WaterSquares.add(var12);
                              }
                           }
                        }
                     }
                  }
               }
            }

            IsoPuddles.getInstance().render(this.WaterSquares, var5);
         }

      }
   }

   void renderPuddlesToChunkTexture(int var1, int var2, IsoChunk var3) {
      if (IsoPuddles.getInstance().shouldRenderPuddles()) {
         if (var2 >= var3.minLevel && var2 <= var3.maxLevel) {
            if (var3.getRenderLevels(var1).isOnScreen(var2)) {
               this.WaterSquares.clear();
               FBORenderCutaways.ChunkLevelData var4 = var3.getCutawayDataForLevel(var2);
               IsoGridSquare[] var5 = var3.squares[var3.squaresIndexOfLevel(var2)];

               for(int var6 = 0; var6 < var5.length; ++var6) {
                  IsoGridSquare var7 = var5[var6];
                  if (var4.shouldRenderSquare(var1, var7)) {
                     IsoObject var8 = var7.getFloor();
                     if (var8 != null && var8.getRenderInfo(var1).m_layer != ObjectRenderLayer.TranslucentFloor) {
                        IsoPuddlesGeometry var9 = var7.getPuddles();
                        if (var9 != null && var9.shouldRender()) {
                           this.WaterSquares.add(var7);
                        }
                     }
                  }
               }

               IsoPuddles.getInstance().renderToChunkTexture(this.WaterSquares, var2);
            }
         }
      }
   }

   void renderRainSplashes(int var1) {
      PerPlayerData var2 = this.perPlayerData[var1];
      IsoChunkMap var3 = this.cell.ChunkMap[var1];
      int var4 = var3.maxHeight;

      for(int var5 = 0; var5 <= var4; ++var5) {
         this.WaterSquares.clear();

         for(int var6 = 0; var6 < var2.onScreenChunks.size(); ++var6) {
            IsoChunk var7 = (IsoChunk)var2.onScreenChunks.get(var6);
            if (var5 >= var7.minLevel && var5 <= var7.maxLevel && var7.getRenderLevels(var1).isOnScreen(var5)) {
               IsoChunkLevel var8 = var7.getLevelData(var5);
               var8.updateRainSplashes();
               var8.renderRainSplashes(var1);
            }
         }
      }

   }

   void renderFog(int var1) {
      if (!(IsoCamera.frameState.CamCharacterZ < 0.0F)) {
         if (PerformanceSettings.FogQuality != 2) {
            PerPlayerData var2 = this.perPlayerData[var1];
            ImprovedFog.getDrawer().startFrame();
            boolean var3 = true;

            for(int var4 = 0; var4 <= 1; ++var4) {
               if (ImprovedFog.startRender(var1, var4)) {
                  if (var3) {
                     var3 = false;
                     ImprovedFog.startFrame(ImprovedFog.getDrawer());
                  }

                  for(int var5 = 0; var5 < var2.onScreenChunks.size(); ++var5) {
                     IsoChunk var6 = (IsoChunk)var2.onScreenChunks.get(var5);
                     if (var4 >= var6.minLevel && var4 <= var6.maxLevel) {
                        FBORenderLevels var7 = var6.getRenderLevels(var1);
                        if (var7.isOnScreen(var4)) {
                           FBORenderCutaways.ChunkLevelData var8 = var6.getCutawayDataForLevel(var4);
                           IsoGridSquare[] var9 = var6.squares[var6.squaresIndexOfLevel(var4)];

                           for(int var10 = 0; var10 < var9.length; ++var10) {
                              IsoGridSquare var11 = var9[var10];
                              if (var8.shouldRenderSquare(var1, var11)) {
                                 IsoObject[] var12 = (IsoObject[])var11.getObjects().getElements();
                                 int var13 = var11.getObjects().size();

                                 for(int var14 = 0; var14 < var13; ++var14) {
                                    IsoObject var15 = var12[var14];
                                    ObjectRenderInfo var16 = var15.getRenderInfo(var1);
                                    if (var16.m_layer == ObjectRenderLayer.MinusFloor) {
                                       GameProfiler.getInstance().invokeAndMeasure("ImprovedFog", var11, ImprovedFog::renderRowsBehind);
                                       break;
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }

                  ImprovedFog.endRender();
               }
            }

            ImprovedFog.getDrawer().endFrame();
         }
      }
   }

   public void handleDelayedLoading(IsoObject var1) {
      int var2 = IsoCamera.frameState.playerIndex;
      var1.getChunk().getRenderLevels(var2).handleDelayedLoading(var1);
      if (this.delayedLoadingTimerMS == 0L) {
         this.delayedLoadingTimerMS = System.currentTimeMillis() + 250L;
      }

   }

   private ColorInfo sanitizeLightInfo(int var1, IsoGridSquare var2) {
      ColorInfo var3 = var2.getLightInfo(var1);
      if (var3 == null) {
         var3 = this.defColorInfo;
      }

      if (DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
         this.defColorInfo.set(1.0F, 1.0F, 1.0F, var3.a);
         var3 = this.defColorInfo;
      }

      return var3;
   }

   private void debugChunkStateRenderPlayer(IsoPlayer var1) {
      if (GameWindow.states.current == DebugChunkState.instance) {
         DebugChunkState.instance.drawObjectAtCursor();
         if (DebugChunkState.instance.getBoolean("ObjectAtCursor")) {
            if ("player".equals(DebugChunkState.instance.fromLua1("getObjectAtCursor", "id"))) {
               float var2 = DebugChunkState.instance.gridXf;
               float var3 = DebugChunkState.instance.gridYf;
               int var4 = DebugChunkState.instance.m_z;
               IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare((double)var2, (double)var3, (double)var4);
               if (var5 != null) {
                  float var6 = var1.getX();
                  float var7 = var1.getY();
                  float var8 = var1.getZ();
                  IsoGridSquare var9 = var1.getCurrentSquare();
                  float var10 = var5.getApparentZ(var2 % 1.0F, var3 % 1.0F);
                  var1.setX(var2);
                  var1.setY(var3);
                  var1.setZ(var10);
                  var1.setCurrent(var5);
                  var1.render(var2, var3, var10, new ColorInfo(), true, false, (Shader)null);
                  var1.setX(var6);
                  var1.setY(var7);
                  var1.setZ(var8);
                  var1.setCurrent(var9);
               }
            }
         }
      }
   }

   private boolean checkDebugKeys(int var1, int var2) {
      boolean var3 = false;
      if (Core.bDebug && GameKeyboard.isKeyPressed(28)) {
         IsoChunkMap var4 = this.cell.getChunkMap(var1);

         for(int var5 = 0; var5 < IsoChunkMap.ChunkGridWidth; ++var5) {
            for(int var6 = 0; var6 < IsoChunkMap.ChunkGridWidth; ++var6) {
               IsoChunk var7 = var4.getChunk(var6, var5);
               if (var7 != null && var2 >= var7.minLevel && var2 <= var7.maxLevel && var7.IsOnScreen(true)) {
                  FBORenderLevels var8 = var7.getRenderLevels(var1);
                  if (var8.isOnScreen(var2)) {
                     var8.invalidateLevel(var2, FBORenderChunk.DIRTY_OBJECT_ADD);
                     this.prepareChunkForUpdating(var1, var7, var2);
                     IsoGridSquare[] var9 = var7.squares[var7.squaresIndexOfLevel(var2)];

                     for(int var10 = 0; var10 < var9.length; ++var10) {
                        if (var9[var10] != null) {
                           var9[var10].setPlayerCutawayFlag(var1, 0, 0L);
                        }
                     }

                     var7.getCutawayData().invalidateOccludedSquaresMaskForSeenRooms(var1, var2);
                  }
               }
            }
         }

         var3 = true;
      }

      return var3;
   }

   public void renderSeamFix1_Floor(IsoObject var1, float var2, float var3, float var4, ColorInfo var5, Consumer<TextureDraw> var6) {
      if (PerformanceSettings.FBORenderChunk && DebugOptions.instance.FBORenderChunk.SeamFix1.getValue()) {
         IsoGridSquare var7 = var1.getSquare();
         IsoSprite var8 = var1.getSprite();
         IsoGridSquare var9;
         if (PZMath.coordmodulo(var7.y, 8) == 7) {
            var9 = var7.getAdjacentSquare(IsoDirections.S);
            if (var9 != null && var9.getFloor() != null) {
               var8.render(var1, var2, var3, var4, var1.dir, var1.offsetX + 5.0F, var1.offsetY + var1.getRenderYOffset() * (float)Core.TileScale - 5.0F, var5, !var1.isBlink(), var6);
            }
         }

         if (PZMath.coordmodulo(var7.x, 8) == 7) {
            var9 = var7.getAdjacentSquare(IsoDirections.E);
            if (var9 != null && var9.getFloor() != null) {
               var8.render(var1, var2, var3, var4, var1.dir, var1.offsetX - 5.0F, var1.offsetY + var1.getRenderYOffset() * (float)Core.TileScale - 5.0F, var5, !var1.isBlink(), var6);
            }
         }
      }

   }

   public void renderSeamFix2_Floor(IsoObject var1, float var2, float var3, float var4, ColorInfo var5, Consumer<TextureDraw> var6) {
      if (!this.bRenderTranslucentOnly) {
         if (PerformanceSettings.FBORenderChunk && DebugOptions.instance.FBORenderChunk.SeamFix2.getValue()) {
            IsoGridSquare var7 = var1.getSquare();
            IsoSprite var8 = var1.getSprite();
            IsoGridSquare var9 = var7.getAdjacentSquare(IsoDirections.S);
            boolean var10 = var9 != null && var9.getWater() != null && var9.getWater().isbShore();
            IsoGridSquare var11;
            if (PZMath.coordmodulo(var7.y, 8) == 7 || var10) {
               var11 = var7.getAdjacentSquare(IsoDirections.S);
               if (var11 != null && var11.getFloor() != null && (var10 || !var11.Is(IsoFlagType.water) || PerformanceSettings.WaterQuality == 2)) {
                  IsoSprite.SEAM_FIX2 = TileSeamManager.Tiles.FloorSouth;
                  if (var8.getProperties().Is(IsoFlagType.FloorHeightOneThird)) {
                     IsoSprite.SEAM_FIX2 = TileSeamManager.Tiles.FloorSouthOneThird;
                  }

                  if (var8.getProperties().Is(IsoFlagType.FloorHeightTwoThirds)) {
                     IsoSprite.SEAM_FIX2 = TileSeamManager.Tiles.FloorSouthTwoThirds;
                  }

                  var1.sx = 0.0F;
                  if (var10) {
                     var1.renderDepthAdjust = -IsoWater.DEPTH_ADJUST;
                  }

                  var8.render(var1, var2, var3, var4, var1.dir, var1.offsetX + 6.0F, var1.offsetY + var1.getRenderYOffset() * (float)Core.TileScale - 3.0F, var5, !var1.isBlink(), var6);
                  var1.sx = 0.0F;
                  var1.renderDepthAdjust = 0.0F;
                  IsoSprite.SEAM_FIX2 = null;
               }
            }

            if (PZMath.coordmodulo(var7.x, 8) == 7) {
               var11 = var7.getAdjacentSquare(IsoDirections.E);
               if (var11 != null && var11.getFloor() != null && (!var11.Is(IsoFlagType.water) || PerformanceSettings.WaterQuality == 2)) {
                  IsoSprite.SEAM_FIX2 = TileSeamManager.Tiles.FloorEast;
                  if (var8.getProperties().Is(IsoFlagType.FloorHeightOneThird)) {
                     IsoSprite.SEAM_FIX2 = TileSeamManager.Tiles.FloorEastOneThird;
                  }

                  if (var8.getProperties().Is(IsoFlagType.FloorHeightTwoThirds)) {
                     IsoSprite.SEAM_FIX2 = TileSeamManager.Tiles.FloorEastTwoThirds;
                  }

                  var1.sx = 0.0F;
                  var8.render(var1, var2, var3, var4, var1.dir, var1.offsetX - 6.0F, var1.offsetY + var1.getRenderYOffset() * (float)Core.TileScale - 3.0F, var5, !var1.isBlink(), var6);
                  var1.sx = 0.0F;
                  IsoSprite.SEAM_FIX2 = null;
               }
            }

            var11 = var7.getAdjacentSquare(IsoDirections.N);
            boolean var12 = var11 != null && var11.getWater() != null && var11.getWater().isbShore();
            if (var12) {
               IsoSprite.SEAM_FIX2 = TileSeamManager.Tiles.FloorSouth;
               var1.sx = 0.0F;
               var1.renderSquareOverride2 = var11;
               var1.renderDepthAdjust = -IsoWater.DEPTH_ADJUST;
               var8.render(var1, var2, var3 - 1.0F, var4, var1.dir, var1.offsetX, var1.offsetY + var1.getRenderYOffset() * (float)Core.TileScale, var5, !var1.isBlink(), var6);
               var1.sx = 0.0F;
               var1.renderSquareOverride2 = null;
               var1.renderDepthAdjust = 0.0F;
               IsoSprite.SEAM_FIX2 = null;
            }

            IsoGridSquare var13 = var7.getAdjacentSquare(IsoDirections.W);
            boolean var14 = var13 != null && var13.getWater() != null && var13.getWater().isbShore();
            if (var14) {
               IsoSprite.SEAM_FIX2 = TileSeamManager.Tiles.FloorEast;
               var1.sx = 0.0F;
               var1.renderSquareOverride2 = var13;
               var1.renderDepthAdjust = -IsoWater.DEPTH_ADJUST;
               var8.render(var1, var2 - 1.0F, var3, var4, var1.dir, var1.offsetX - 2.0F, var1.offsetY - 1.0F + var1.getRenderYOffset() * (float)Core.TileScale, var5, !var1.isBlink(), var6);
               var1.sx = 0.0F;
               var1.renderSquareOverride2 = null;
               var1.renderDepthAdjust = 0.0F;
               IsoSprite.SEAM_FIX2 = null;
            }

            IsoGridSquare var15 = var7.getAdjacentSquare(IsoDirections.E);
            boolean var16 = var15 != null && var15.getWater() != null && var15.getWater().isbShore();
            if (var16) {
               IsoSprite.SEAM_FIX2 = TileSeamManager.Tiles.FloorEast;
               var1.sx = 0.0F;
               var8.render(var1, var2, var3, var4, var1.dir, var1.offsetX - 6.0F, var1.offsetY + var1.getRenderYOffset() * (float)Core.TileScale - 3.0F, var5, !var1.isBlink(), var6);
               var1.sx = 0.0F;
               IsoSprite.SEAM_FIX2 = null;
            }

         }
      }
   }

   public void renderSeamFix1_Wall(IsoObject var1, float var2, float var3, float var4, ColorInfo var5, Consumer<TextureDraw> var6) {
      if (PerformanceSettings.FBORenderChunk && DebugOptions.instance.FBORenderChunk.SeamFix1.getValue()) {
         IsoGridSquare var7 = var1.getSquare();
         IsoSprite var8 = var1.getSprite();
         IsoGridSquare var9;
         if (IsoSprite.SEAM_SOUTH) {
            if (var8.getProperties().Is(IsoFlagType.WallW) && PZMath.coordmodulo(var7.y, 8) == 7) {
               var9 = var7.getAdjacentSquare(IsoDirections.S);
               if (var9 != null && ((var9.getWallType() & 4) != 0 || var9.getWindowFrame(false) != null || var9.Is(IsoFlagType.DoorWallW))) {
                  var8.renderWallSliceW(var1, var2, var3, var4, var1.dir, var1.offsetX, var1.offsetY + var1.getRenderYOffset() * (float)Core.TileScale, var5, !var1.isBlink(), var6);
               }
            }
         } else if (var8.getProperties().Is(IsoFlagType.WallW) && PZMath.coordmodulo(var7.y, 8) == 0) {
            var9 = var7.getAdjacentSquare(IsoDirections.N);
            if (var9 != null && ((var9.getWallType() & 4) != 0 || var9.getWindowFrame(false) != null || var9.Is(IsoFlagType.DoorWallW))) {
               var8.renderWallSliceW(var1, var2, var3, var4, var1.dir, var1.offsetX, var1.offsetY + var1.getRenderYOffset() * (float)Core.TileScale, var5, !var1.isBlink(), var6);
            }
         }

         if (var8.getProperties().Is(IsoFlagType.WallN) && PZMath.coordmodulo(var7.x, 8) == 7) {
            var9 = var7.getAdjacentSquare(IsoDirections.E);
            if (var9 != null && ((var9.getWallType() & 1) != 0 || var9.getWindowFrame(true) != null || var9.Is(IsoFlagType.DoorWallN))) {
               var8.renderWallSliceN(var1, var2, var3, var4, var1.dir, var1.offsetX, var1.offsetY + var1.getRenderYOffset() * (float)Core.TileScale, var5, !var1.isBlink(), var6);
            }
         }
      }

   }

   public void renderSeamFix2_Wall(IsoObject var1, float var2, float var3, float var4, ColorInfo var5, Consumer<TextureDraw> var6) {
      if (PerformanceSettings.FBORenderChunk && DebugOptions.instance.FBORenderChunk.SeamFix2.getValue()) {
         IsoGridSquare var7 = var1.getSquare();
         IsoSprite var8 = var1.getSprite();
         if (!var8.getProperties().Is(IsoFlagType.HoppableN) && !var8.getProperties().Is(IsoFlagType.HoppableW)) {
            if (var8.tilesetName == null || !var8.tilesetName.contains("walls_exterior_roofs")) {
               IsoGridSquare var9;
               if (var8.getProperties().Is(IsoFlagType.WallNW) && var6 == WallShaperW.instance && PZMath.coordmodulo(var7.y, 8) == 7) {
                  var9 = var7.getAdjacentSquare(IsoDirections.S);
                  if (var9 != null && ((var9.getWallType() & 4) != 0 || var9.getWindowFrame(false) != null || var9.Is(IsoFlagType.DoorWallW))) {
                     IsoSprite.SEAM_FIX2 = TileSeamManager.Tiles.WallSouth;
                     var1.sx = 0.0F;
                     var8.render(var1, var2, var3, var4, IsoDirections.NW, var1.offsetX + 6.0F, var1.offsetY + var1.getRenderYOffset() * (float)Core.TileScale - 3.0F, var5, !var1.isBlink(), var6);
                     var1.sx = 0.0F;
                     IsoSprite.SEAM_FIX2 = null;
                  }
               }

               if (var8.getProperties().Is(IsoFlagType.WallNW) && var6 == WallShaperN.instance && PZMath.coordmodulo(var7.x, 8) == 7) {
                  var9 = var7.getAdjacentSquare(IsoDirections.E);
                  if (var9 != null && ((var9.getWallType() & 1) != 0 || var9.getWindowFrame(true) != null || var9.Is(IsoFlagType.DoorWallN))) {
                     IsoSprite.SEAM_FIX2 = TileSeamManager.Tiles.WallEast;
                     var1.sx = 0.0F;
                     var8.render(var1, var2, var3, var4, IsoDirections.NW, var1.offsetX - 6.0F, var1.offsetY + var1.getRenderYOffset() * (float)Core.TileScale - 3.0F, var5, !var1.isBlink(), var6);
                     var1.sx = 0.0F;
                     IsoSprite.SEAM_FIX2 = null;
                  }
               }

               if ((var8.getProperties().Is(IsoFlagType.WallW) || var8.getProperties().Is(IsoFlagType.WindowW)) && PZMath.coordmodulo(var7.y, 8) == 7) {
                  var9 = var7.getAdjacentSquare(IsoDirections.S);
                  if (var9 != null && ((var9.getWallType() & 4) != 0 || var9.getWindowFrame(false) != null || var9.Is(IsoFlagType.DoorWallW))) {
                     IsoSprite.SEAM_FIX2 = TileSeamManager.Tiles.WallSouth;
                     var1.sx = 0.0F;
                     var8.render(var1, var2, var3, var4, IsoDirections.W, var1.offsetX + 6.0F, var1.offsetY + var1.getRenderYOffset() * (float)Core.TileScale - 3.0F, var5, !var1.isBlink(), var6);
                     var1.sx = 0.0F;
                     IsoSprite.SEAM_FIX2 = null;
                  }
               }

               if ((var8.getProperties().Is(IsoFlagType.WallN) || var8.getProperties().Is(IsoFlagType.WindowN)) && PZMath.coordmodulo(var7.x, 8) == 7) {
                  var9 = var7.getAdjacentSquare(IsoDirections.E);
                  if (var9 != null && ((var9.getWallType() & 1) != 0 || var9.getWindowFrame(true) != null || var9.Is(IsoFlagType.DoorWallN))) {
                     IsoSprite.SEAM_FIX2 = TileSeamManager.Tiles.WallEast;
                     var1.sx = 0.0F;
                     var8.render(var1, var2, var3, var4, IsoDirections.N, var1.offsetX - 6.0F, var1.offsetY + var1.getRenderYOffset() * (float)Core.TileScale - 3.0F, var5, !var1.isBlink(), var6);
                     var1.sx = 0.0F;
                     IsoSprite.SEAM_FIX2 = null;
                  }
               }

            }
         }
      }
   }

   void checkSeenRooms(IsoPlayer var1, int var2) {
      if (!GameClient.bClient) {
         IsoBuilding var3 = var1.getBuilding();
         if (var3 != null) {
            Iterator var4 = var3.Rooms.iterator();

            while(var4.hasNext()) {
               IsoRoom var5 = (IsoRoom)var4.next();
               if (!var5.def.bExplored && PZMath.abs((float)(var5.def.level - var2)) <= 1.0F) {
                  var5.def.bExplored = true;
                  IsoWorld.instance.getCell().roomSpotted(var5);
               }
            }

         }
      }
   }

   boolean shouldHideFascia(int var1, IsoObject var2) {
      IsoGridSquare var3 = var2.getFasciaAttachedSquare();
      if (var3 == null) {
         return false;
      } else {
         return !FBORenderCutaways.getInstance().shouldRenderBuildingSquare(var1, var3);
      }
   }

   private static final class PerPlayerData {
      final int playerIndex;
      int lastZ;
      final ArrayList<IsoChunk> onScreenChunks = new ArrayList();
      final ArrayList<IsoChunk> chunksWith_AnimatedAttachments = new ArrayList();
      final ArrayList<IsoChunk> chunksWith_Flies = new ArrayList();
      final ArrayList<IsoChunk> chunksWith_TranslucentFloor = new ArrayList();
      final ArrayList<IsoChunk> chunksWith_TranslucentNonFloor = new ArrayList();
      float playerBoundsX;
      float playerBoundsY;
      float playerBoundsW;
      float playerBoundsH;
      final ArrayList<IsoGameCharacter.Location> squaresObscuringPlayer = new ArrayList();
      final ArrayList<IsoGameCharacter.Location> fadingInSquares = new ArrayList();
      private int lightingUpdateCounter;
      int occludedGridX1;
      int occludedGridY1;
      int occludedGridX2;
      int occludedGridY2;
      int[] occludedGrid;
      boolean bOcclusionChanged = false;

      PerPlayerData(int var1) {
         this.playerIndex = var1;
      }

      void addChunkWith_AnimatedAttachments(IsoChunk var1) {
         if (!this.chunksWith_AnimatedAttachments.contains(var1)) {
            this.chunksWith_AnimatedAttachments.add(var1);
         }
      }

      void addChunkWith_Flies(IsoChunk var1) {
         if (!this.chunksWith_Flies.contains(var1)) {
            this.chunksWith_Flies.add(var1);
         }
      }

      void addChunkWith_TranslucentFloor(IsoChunk var1) {
         if (!this.chunksWith_TranslucentFloor.contains(var1)) {
            this.chunksWith_TranslucentFloor.add(var1);
         }
      }

      void addChunkWith_TranslucentNonFloor(IsoChunk var1) {
         if (!this.chunksWith_TranslucentNonFloor.contains(var1)) {
            this.chunksWith_TranslucentNonFloor.add(var1);
         }
      }

      boolean isSquareObscuringPlayer(IsoGridSquare var1) {
         for(int var2 = 0; var2 < this.squaresObscuringPlayer.size(); ++var2) {
            IsoGameCharacter.Location var3 = (IsoGameCharacter.Location)this.squaresObscuringPlayer.get(var2);
            if (var3.equals(var1.x, var1.y, var1.z)) {
               return true;
            }
         }

         return false;
      }

      boolean isFadingInSquare(IsoGridSquare var1) {
         for(int var2 = 0; var2 < this.fadingInSquares.size(); ++var2) {
            IsoGameCharacter.Location var3 = (IsoGameCharacter.Location)this.fadingInSquares.get(var2);
            if (var3.equals(var1.x, var1.y, var1.z)) {
               return true;
            }
         }

         return false;
      }

      boolean isObjectObscuringPlayer(IsoGridSquare var1, Texture var2, float var3, float var4) {
         var1.CachedScreenX = IsoUtils.XToScreen((float)var1.x, (float)var1.y, (float)var1.z, 0);
         var1.CachedScreenY = IsoUtils.YToScreen((float)var1.x, (float)var1.y, (float)var1.z, 0);
         float var5 = var1.CachedScreenX - var3 + var2.getOffsetX();
         float var6 = var1.CachedScreenY - var4 + var2.getOffsetY();
         return var5 < this.playerBoundsX + this.playerBoundsW && var5 + (float)var2.getWidth() > this.playerBoundsX && var6 < this.playerBoundsY + this.playerBoundsH && var6 + (float)var2.getHeight() > this.playerBoundsY;
      }
   }
}
