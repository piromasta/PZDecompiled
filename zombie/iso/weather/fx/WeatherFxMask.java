package zombie.iso.weather.fx;

import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import org.joml.Vector2i;
import org.joml.Vector3f;
import zombie.GameProfiler;
import zombie.IndieGL;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderSettings;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureFBO;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.debug.LogSeverity;
import zombie.input.GameKeyboard;
import zombie.iso.DiamondMatrixIterator;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.regions.IWorldRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameServer;
import zombie.util.Type;
import zombie.worldMap.Rasterize;

public class WeatherFxMask {
   private static boolean DEBUG_KEYS = false;
   private static TextureFBO fboMask;
   private static TextureFBO fboParticles;
   public static IsoSprite floorSprite;
   public static IsoSprite wallNSprite;
   public static IsoSprite wallWSprite;
   public static IsoSprite wallNWSprite;
   public static IsoSprite wallSESprite;
   private static Texture texWhite;
   private static boolean bRenderingMask = false;
   private static int curPlayerIndex;
   public static final int BIT_FLOOR = 0;
   public static final int BIT_WALLN = 1;
   public static final int BIT_WALLW = 2;
   public static final int BIT_IS_CUT = 4;
   public static final int BIT_CHARS = 8;
   public static final int BIT_OBJECTS = 16;
   public static final int BIT_WALL_SE = 32;
   public static final int BIT_DOOR = 64;
   public static float offsetX;
   public static float offsetY;
   public static ColorInfo defColorInfo;
   private static int DIAMOND_ROWS;
   public int x;
   public int y;
   public int z;
   public int flags;
   public IsoGridSquare gs;
   public boolean enabled;
   private static PlayerFxMask[] playerMasks;
   private static DiamondMatrixIterator dmiter;
   private static final Vector2i diamondMatrixPos;
   private static final RasterizeBounds tempRasterizeBounds;
   private static final RasterizeBounds[] rasterizeBounds;
   private static final Rasterize rasterize;
   private static IsoChunkMap rasterizeChunkMap;
   private static int rasterizeZ;
   private static Vector3f tmpVec;
   private static IsoGameCharacter.TorchInfo tmpTorch;
   private static ColorInfo tmpColInfo;
   private static int[] test;
   private static String[] testNames;
   private static int var1;
   private static int var2;
   private static float var3;
   private static int SCR_MASK_ADD;
   private static int DST_MASK_ADD;
   private static int SCR_MASK_SUB;
   private static int DST_MASK_SUB;
   private static int SCR_PARTICLES;
   private static int DST_PARTICLES;
   private static int SCR_MERGE;
   private static int DST_MERGE;
   private static int SCR_FINAL;
   private static int DST_FINAL;
   private static int ID_SCR_MASK_ADD;
   private static int ID_DST_MASK_ADD;
   private static int ID_SCR_MASK_SUB;
   private static int ID_DST_MASK_SUB;
   private static int ID_SCR_MERGE;
   private static int ID_DST_MERGE;
   private static int ID_SCR_FINAL;
   private static int ID_DST_FINAL;
   private static int ID_SCR_PARTICLES;
   private static int ID_DST_PARTICLES;
   private static int TARGET_BLEND;
   private static boolean DEBUG_MASK;
   public static boolean MASKING_ENABLED;
   private static boolean DEBUG_MASK_AND_PARTICLES;
   private static final boolean DEBUG_THROTTLE_KEYS = true;
   private static int keypause;

   public WeatherFxMask() {
   }

   public static TextureFBO getFboMask() {
      return fboMask;
   }

   public static TextureFBO getFboParticles() {
      return fboParticles;
   }

   public static boolean isRenderingMask() {
      return bRenderingMask;
   }

   public static void init() throws Exception {
      if (!GameServer.bServer) {
         for(int var0 = 0; var0 < playerMasks.length; ++var0) {
            playerMasks[var0] = new PlayerFxMask();
         }

         playerMasks[0].init();
         initGlIds();
         floorSprite = IsoSpriteManager.instance.getSprite("floors_interior_tilesandwood_01_16");
         wallNSprite = IsoSpriteManager.instance.getSprite("walls_interior_house_01_21");
         wallWSprite = IsoSpriteManager.instance.getSprite("walls_interior_house_01_20");
         wallNWSprite = IsoSpriteManager.instance.getSprite("walls_interior_house_01_22");
         wallSESprite = IsoSpriteManager.instance.getSprite("walls_interior_house_01_23");
         texWhite = Texture.getSharedTexture("media/textures/weather/fogwhite.png");
      }
   }

   public static boolean checkFbos() {
      if (GameServer.bServer) {
         return false;
      } else {
         TextureFBO var0 = Core.getInstance().getOffscreenBuffer();
         if (Core.getInstance().getOffscreenBuffer() == null) {
            DebugLog.log("fbo=" + (var0 != null));
            return false;
         } else {
            int var1x = Core.getInstance().getScreenWidth();
            int var2x = Core.getInstance().getScreenHeight();
            if (fboMask != null && fboParticles != null && fboMask.getTexture().getWidth() == var1x && fboMask.getTexture().getHeight() == var2x) {
               return fboMask != null && fboParticles != null;
            } else {
               if (fboMask != null) {
                  fboMask.destroy();
               }

               if (fboParticles != null) {
                  fboParticles.destroy();
               }

               fboMask = null;
               fboParticles = null;

               Texture var3x;
               try {
                  var3x = new Texture(var1x, var2x, 16);
                  fboMask = new TextureFBO(var3x);
               } catch (Exception var5) {
                  DebugLog.General.printException(var5, "", LogSeverity.Error);
               }

               try {
                  var3x = new Texture(var1x, var2x, 16);
                  fboParticles = new TextureFBO(var3x);
               } catch (Exception var4) {
                  DebugLog.General.printException(var4, "", LogSeverity.Error);
               }

               return fboMask != null && fboParticles != null;
            }
         }
      }
   }

   public static void destroy() {
      if (fboMask != null) {
         fboMask.destroy();
      }

      fboMask = null;
      if (fboParticles != null) {
         fboParticles.destroy();
      }

      fboParticles = null;
   }

   public static void initMask() {
      if (!GameServer.bServer) {
         curPlayerIndex = IsoCamera.frameState.playerIndex;
         playerMasks[curPlayerIndex].initMask();
      }
   }

   private static boolean isOnScreen(int var0, int var1x, int var2x) {
      float var3x = (float)((int)IsoUtils.XToScreenInt(var0, var1x, var2x, 0));
      float var4 = (float)((int)IsoUtils.YToScreenInt(var0, var1x, var2x, 0));
      var3x -= (float)((int)IsoCamera.frameState.OffX);
      var4 -= (float)((int)IsoCamera.frameState.OffY);
      if (var3x + (float)(32 * Core.TileScale) <= 0.0F) {
         return false;
      } else if (var4 + (float)(32 * Core.TileScale) <= 0.0F) {
         return false;
      } else if (var3x - (float)(32 * Core.TileScale) >= (float)IsoCamera.frameState.OffscreenWidth) {
         return false;
      } else {
         return !(var4 - (float)(96 * Core.TileScale) >= (float)IsoCamera.frameState.OffscreenHeight);
      }
   }

   public boolean isLoc(int var1x, int var2x, int var3x) {
      return this.x == var1x && this.y == var2x && this.z == var3x;
   }

   public static boolean playerHasMaskToDraw(int var0) {
      return var0 < playerMasks.length ? playerMasks[var0].hasMaskToDraw : false;
   }

   public static void setDiamondIterDone(int var0) {
      if (var0 < playerMasks.length) {
         playerMasks[var0].DIAMOND_ITER_DONE = true;
      }

   }

   public static void forceMaskUpdate(int var0) {
      if (var0 < playerMasks.length) {
         playerMasks[var0].plrSquare = null;
      }

   }

   public static void forceMaskUpdateAll() {
      if (!GameServer.bServer) {
         for(int var0 = 0; var0 < playerMasks.length; ++var0) {
            playerMasks[var0].plrSquare = null;
         }

      }
   }

   private static boolean getIsStairs(IsoGridSquare var0) {
      return var0 != null && (var0.Has(IsoObjectType.stairsBN) || var0.Has(IsoObjectType.stairsBW) || var0.Has(IsoObjectType.stairsMN) || var0.Has(IsoObjectType.stairsMW) || var0.Has(IsoObjectType.stairsTN) || var0.Has(IsoObjectType.stairsTW));
   }

   private static boolean getHasDoor(IsoGridSquare var0) {
      return var0 != null && (var0.Is(IsoFlagType.cutN) || var0.Is(IsoFlagType.cutW)) && (var0.Is(IsoFlagType.DoorWallN) || var0.Is(IsoFlagType.DoorWallW)) && !var0.Is(IsoFlagType.doorN) && !var0.Is(IsoFlagType.doorW) ? var0.getCanSee(curPlayerIndex) : false;
   }

   public static void addMaskLocation(IsoGridSquare var0, int var1x, int var2x, int var3x) {
      if (!GameServer.bServer) {
         PlayerFxMask var4 = playerMasks[curPlayerIndex];
         if (var4.requiresUpdate) {
            if (var4.hasMaskToDraw && var4.playerZ == var3x) {
               IsoChunkMap var5 = IsoWorld.instance.getCell().getChunkMap(curPlayerIndex);
               IsoGridSquare var6;
               boolean var7;
               boolean var8;
               if (isInPlayerBuilding(var0, var1x, var2x, var3x)) {
                  var6 = var5.getGridSquare(var1x, var2x - 1, var3x);
                  var7 = !isInPlayerBuilding(var6, var1x, var2x - 1, var3x);
                  var6 = var5.getGridSquare(var1x - 1, var2x, var3x);
                  var8 = !isInPlayerBuilding(var6, var1x - 1, var2x, var3x);
                  var6 = var5.getGridSquare(var1x - 1, var2x - 1, var3x);
                  boolean var9 = !isInPlayerBuilding(var6, var1x - 1, var2x - 1, var3x);
                  int var10 = 0;
                  if (var7) {
                     var10 |= 1;
                  }

                  if (var8) {
                     var10 |= 2;
                  }

                  if (var9) {
                     var10 |= 32;
                  }

                  boolean var11 = false;
                  boolean var12 = getIsStairs(var0);
                  int var13;
                  if (var0 != null && (var7 || var8 || var9)) {
                     var13 = 24;
                     if (var7 && !var0.getProperties().Is(IsoFlagType.WallN) && !var0.Is(IsoFlagType.WallNW)) {
                        var4.addMask(var1x - 1, var2x, var3x, (IsoGridSquare)null, 8, false);
                        var4.addMask(var1x, var2x, var3x, var0, var13);
                        var4.addMask(var1x + 1, var2x, var3x, (IsoGridSquare)null, var13, false);
                        var4.addMask(var1x + 2, var2x, var3x, (IsoGridSquare)null, 8, false);
                        var4.addMask(var1x, var2x + 1, var3x, (IsoGridSquare)null, 8, false);
                        var4.addMask(var1x + 1, var2x + 1, var3x, (IsoGridSquare)null, var13, false);
                        var4.addMask(var1x + 2, var2x + 1, var3x, (IsoGridSquare)null, var13, false);
                        var4.addMask(var1x + 2, var2x + 2, var3x, (IsoGridSquare)null, 16, false);
                        var4.addMask(var1x + 3, var2x + 2, var3x, (IsoGridSquare)null, 16, false);
                        var11 = true;
                     }

                     if (var8 && !var0.getProperties().Is(IsoFlagType.WallW) && !var0.getProperties().Is(IsoFlagType.WallNW)) {
                        var4.addMask(var1x, var2x - 1, var3x, (IsoGridSquare)null, 8, false);
                        var4.addMask(var1x, var2x, var3x, var0, var13);
                        var4.addMask(var1x, var2x + 1, var3x, (IsoGridSquare)null, var13, false);
                        var4.addMask(var1x, var2x + 2, var3x, (IsoGridSquare)null, 8, false);
                        var4.addMask(var1x + 1, var2x, var3x, (IsoGridSquare)null, 8, false);
                        var4.addMask(var1x + 1, var2x + 1, var3x, (IsoGridSquare)null, var13, false);
                        var4.addMask(var1x + 1, var2x + 2, var3x, (IsoGridSquare)null, var13, false);
                        var4.addMask(var1x + 2, var2x + 2, var3x, (IsoGridSquare)null, 16, false);
                        var4.addMask(var1x + 2, var2x + 3, var3x, (IsoGridSquare)null, 16, false);
                        var11 = true;
                     }

                     if (var9) {
                        int var14 = var12 ? var13 : var10;
                        var4.addMask(var1x, var2x, var3x, var0, var14);
                        var11 = true;
                     }
                  }

                  if (!var11) {
                     var13 = var12 ? 24 : var10;
                     var4.addMask(var1x, var2x, var3x, var0, var13);
                  }
               } else {
                  var6 = var5.getGridSquare(var1x, var2x - 1, var3x);
                  var7 = isInPlayerBuilding(var6, var1x, var2x - 1, var3x);
                  var6 = var5.getGridSquare(var1x - 1, var2x, var3x);
                  var8 = isInPlayerBuilding(var6, var1x - 1, var2x, var3x);
                  if (!var7 && !var8) {
                     var6 = var5.getGridSquare(var1x - 1, var2x - 1, var3x);
                     if (isInPlayerBuilding(var6, var1x - 1, var2x - 1, var3x)) {
                        var4.addMask(var1x, var2x, var3x, var0, 4);
                     }
                  } else {
                     int var15 = 4;
                     if (var7) {
                        var15 |= 1;
                     }

                     if (var8) {
                        var15 |= 2;
                     }

                     if (getHasDoor(var0)) {
                        var15 |= 64;
                     }

                     var4.addMask(var1x, var2x, var3x, var0, var15);
                  }
               }

            }
         }
      }
   }

   private static boolean isInPlayerBuilding(IsoGridSquare var0, int var1x, int var2x, int var3x) {
      PlayerFxMask var4 = playerMasks[curPlayerIndex];
      if (var0 != null && var0.Is(IsoFlagType.solidfloor)) {
         if (var0.getBuilding() != null && var0.getBuilding() == var4.player.getBuilding()) {
            return true;
         }

         if (var0.getBuilding() == null) {
            return var4.curIsoWorldRegion != null && var0.getIsoWorldRegion() != null && var0.getIsoWorldRegion().isFogMask() && (var0.getIsoWorldRegion() == var4.curIsoWorldRegion || var4.curConnectedRegions.contains(var0.getIsoWorldRegion()));
         }
      } else {
         if (isInteriorLocation(var1x, var2x, var3x)) {
            return true;
         }

         if (var0 != null && var0.getBuilding() == null) {
            return var4.curIsoWorldRegion != null && var0.getIsoWorldRegion() != null && var0.getIsoWorldRegion().isFogMask() && (var0.getIsoWorldRegion() == var4.curIsoWorldRegion || var4.curConnectedRegions.contains(var0.getIsoWorldRegion()));
         }

         if (var0 == null && var4.curIsoWorldRegion != null) {
            IWorldRegion var5 = IsoRegions.getIsoWorldRegion(var1x, var2x, var3x);
            return var5 != null && var5.isFogMask() && (var5 == var4.curIsoWorldRegion || var4.curConnectedRegions.contains(var5));
         }
      }

      return false;
   }

   private static boolean isInteriorLocation(int var0, int var1x, int var2x) {
      PlayerFxMask var3x = playerMasks[curPlayerIndex];

      for(int var5 = var2x; var5 >= 0; --var5) {
         IsoGridSquare var4 = IsoWorld.instance.getCell().getChunkMap(curPlayerIndex).getGridSquare(var0, var1x, var5);
         if (var4 != null) {
            if (var4.getBuilding() != null && var4.getBuilding() == var3x.player.getBuilding()) {
               return true;
            }

            if (var4.Is(IsoFlagType.exterior)) {
               return false;
            }
         }
      }

      return false;
   }

   private static void scanForTilesOld(int var0) {
      PlayerFxMask var1x = playerMasks[curPlayerIndex];
      if (!var1x.DIAMOND_ITER_DONE) {
         IsoPlayer var2x = IsoPlayer.players[var0];
         int var3x = PZMath.fastfloor(var2x.getZ());
         byte var4 = 0;
         byte var5 = 0;
         int var6 = var4 + IsoCamera.getOffscreenWidth(var0);
         int var7 = var5 + IsoCamera.getOffscreenHeight(var0);
         float var8 = IsoUtils.XToIso((float)var4, (float)var5, 0.0F);
         float var9 = IsoUtils.YToIso((float)var6, (float)var5, 0.0F);
         float var10 = IsoUtils.XToIso((float)var6, (float)var7, 6.0F);
         float var11 = IsoUtils.YToIso((float)var4, (float)var7, 6.0F);
         float var12 = IsoUtils.XToIso((float)var6, (float)var5, 0.0F);
         int var13 = (int)var9;
         int var14 = (int)var11;
         int var15 = (int)var8;
         int var16 = (int)var10;
         DIAMOND_ROWS = (int)var12 * 4;
         var15 -= 2;
         var13 -= 2;
         dmiter.reset(var16 - var15);
         Vector2i var18 = diamondMatrixPos;
         IsoChunkMap var19 = IsoWorld.instance.getCell().getChunkMap(var0);

         while(dmiter.next(var18)) {
            if (var18 != null) {
               IsoGridSquare var17 = var19.getGridSquare(var18.x + var15, var18.y + var13, var3x);
               if (var17 == null) {
                  addMaskLocation((IsoGridSquare)null, var18.x + var15, var18.y + var13, var3x);
               } else {
                  IsoChunk var20 = var17.getChunk();
                  if (var20 != null && var17.IsOnScreen()) {
                     addMaskLocation(var17, var18.x + var15, var18.y + var13, var3x);
                  }
               }
            }
         }

      }
   }

   public static boolean checkVisibleSquares(int var0, int var1x) {
      if (!playerMasks[var0].hasMaskToDraw) {
         return false;
      } else if (rasterizeBounds[var0] == null) {
         return true;
      } else {
         tempRasterizeBounds.calculate(var0, var1x);
         return !tempRasterizeBounds.equals(rasterizeBounds[var0]);
      }
   }

   private static void scanForTiles(int var0) {
      PlayerFxMask var1x = playerMasks[var0];
      if (!var1x.DIAMOND_ITER_DONE) {
         IsoPlayer var2x = IsoPlayer.players[var0];
         int var3x = PZMath.fastfloor(var2x.getZ());
         if (rasterizeBounds[var0] == null) {
            rasterizeBounds[var0] = new RasterizeBounds();
         }

         RasterizeBounds var4 = rasterizeBounds[var0];
         GameProfiler var10000 = GameProfiler.getInstance();
         Integer var10002 = var0;
         Integer var10003 = var3x;
         Objects.requireNonNull(var4);
         var10000.invokeAndMeasure("Calc Bounds", var10002, var10003, var4::calculate);
         if (Core.bDebug) {
         }

         boolean var5 = false;
         GameProfiler.ProfileArea var6 = GameProfiler.getInstance().startIfEnabled("scanTriangle");
         rasterizeChunkMap = IsoWorld.instance.getCell().getChunkMap(var0);
         rasterizeZ = var3x;
         rasterize.scanTriangle(var4.x1, var4.y1, var4.x2, var4.y2, var4.x4, var4.y4, 0, 100000, (var1xx, var2xx) -> {
            IsoGridSquare var3x = rasterizeChunkMap.getGridSquare(var1xx, var2xx, rasterizeZ);
            addMaskLocation(var3x, var1xx, var2xx, rasterizeZ);
            if (var5) {
               LineDrawer.addRect((float)var1xx + 0.05F, (float)var2xx + 0.05F, (float)rasterizeZ, 0.9F, 0.9F, 1.0F, 0.0F, 0.0F);
            }

         });
         rasterize.scanTriangle(var4.x2, var4.y2, var4.x3, var4.y3, var4.x4, var4.y4, 0, 100000, (var1xx, var2xx) -> {
            IsoGridSquare var3x = rasterizeChunkMap.getGridSquare(var1xx, var2xx, rasterizeZ);
            addMaskLocation(var3x, var1xx, var2xx, rasterizeZ);
            if (var5) {
               LineDrawer.addRect((float)var1xx, (float)var2xx, (float)rasterizeZ, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F);
            }

         });
         GameProfiler.getInstance().end(var6);
         if (var5) {
            LineDrawer.addLine(var4.x1, var4.y1, (float)rasterizeZ, var4.x2, var4.y2, (float)rasterizeZ, 1.0F, 1.0F, 1.0F, 0.5F);
            LineDrawer.addLine(var4.x2, var4.y2, (float)rasterizeZ, var4.x3, var4.y3, (float)rasterizeZ, 1.0F, 1.0F, 1.0F, 0.5F);
            LineDrawer.addLine(var4.x3, var4.y3, (float)rasterizeZ, var4.x4, var4.y4, (float)rasterizeZ, 1.0F, 1.0F, 1.0F, 0.5F);
            LineDrawer.addLine(var4.x1, var4.y1, (float)rasterizeZ, var4.x4, var4.y4, (float)rasterizeZ, 1.0F, 1.0F, 1.0F, 0.5F);
            float var7 = IsoCamera.getOffX();
            float var8 = IsoCamera.getOffY();
            LineDrawer.drawLine((float)var4.cx1 - var7, (float)var4.cy1 - var8, (float)var4.cx2 - var7, (float)var4.cy2 - var8, 1.0F, 1.0F, 1.0F, 0.5F, 2);
            LineDrawer.drawLine((float)var4.cx2 - var7, (float)var4.cy2 - var8, (float)var4.cx3 - var7, (float)var4.cy3 - var8, 1.0F, 1.0F, 1.0F, 0.5F, 2);
            LineDrawer.drawLine((float)var4.cx3 - var7, (float)var4.cy3 - var8, (float)var4.cx4 - var7, (float)var4.cy4 - var8, 1.0F, 1.0F, 1.0F, 0.5F, 2);
            LineDrawer.drawLine((float)var4.cx4 - var7, (float)var4.cy4 - var8, (float)var4.cx1 - var7, (float)var4.cy1 - var8, 1.0F, 1.0F, 1.0F, 0.5F, 2);
         }

      }
   }

   private static void renderMaskFloor(int var0, int var1x, int var2x) {
      floorSprite.render((IsoObject)null, (float)var0, (float)var1x, (float)var2x, IsoDirections.N, offsetX, offsetY, defColorInfo, false);
   }

   private static void renderMaskWall(IsoGridSquare var0, int var1x, int var2x, int var3x, boolean var4, boolean var5, int var6) {
      if (var0 != null) {
         IsoGridSquare var7 = var0.nav[IsoDirections.N.index()];
         IsoGridSquare var8 = var0.nav[IsoDirections.S.index()];
         IsoGridSquare var9 = var0.nav[IsoDirections.W.index()];
         IsoGridSquare var10 = var0.nav[IsoDirections.E.index()];
         long var11 = System.currentTimeMillis();
         int var13 = var0.getPlayerCutawayFlag(var6, var11);
         int var14 = var7 == null ? 0 : var7.getPlayerCutawayFlag(var6, var11);
         int var15 = var8 == null ? 0 : var8.getPlayerCutawayFlag(var6, var11);
         int var16 = var9 == null ? 0 : var9.getPlayerCutawayFlag(var6, var11);
         int var17 = var10 == null ? 0 : var10.getPlayerCutawayFlag(var6, var11);
         IsoSprite var18;
         IsoDirections var19;
         if (var4 && var5) {
            var18 = wallNWSprite;
            var19 = IsoDirections.NW;
         } else if (var4) {
            var18 = wallNSprite;
            var19 = IsoDirections.N;
         } else if (var5) {
            var18 = wallWSprite;
            var19 = IsoDirections.W;
         } else {
            var18 = wallSESprite;
            var19 = IsoDirections.SE;
         }

         var0.DoCutawayShaderSprite(var18, var19, var13, var14, var15, var16, var17);
      }
   }

   private static void renderMaskWallNoCuts(int var0, int var1x, int var2x, boolean var3x, boolean var4) {
      if (var3x && var4) {
         wallNWSprite.render((IsoObject)null, (float)var0, (float)var1x, (float)var2x, IsoDirections.N, offsetX, offsetY, defColorInfo, false);
      } else if (var3x) {
         wallNSprite.render((IsoObject)null, (float)var0, (float)var1x, (float)var2x, IsoDirections.N, offsetX, offsetY, defColorInfo, false);
      } else if (var4) {
         wallWSprite.render((IsoObject)null, (float)var0, (float)var1x, (float)var2x, IsoDirections.N, offsetX, offsetY, defColorInfo, false);
      } else {
         wallSESprite.render((IsoObject)null, (float)var0, (float)var1x, (float)var2x, IsoDirections.N, offsetX, offsetY, defColorInfo, false);
      }

   }

   public static void renderFxMask(int var0) {
      if (!(IsoCamera.frameState.CamCharacterZ < 0.0F)) {
         if (DebugOptions.instance.Weather.Fx.getValue()) {
            if (!GameServer.bServer) {
               if (IsoWeatherFX.instance != null) {
                  if (LuaManager.thread == null || !LuaManager.thread.bStep) {
                     if (DEBUG_KEYS && Core.bDebug) {
                        updateDebugKeys();
                     }

                     if (playerMasks[var0].maskEnabled) {
                        PlayerFxMask var1x = playerMasks[curPlayerIndex];
                        if (var1x.maskEnabled) {
                           if (MASKING_ENABLED && !checkFbos()) {
                              MASKING_ENABLED = false;
                           }

                           if (MASKING_ENABLED && var1x.hasMaskToDraw) {
                              GameProfiler.getInstance().invokeAndMeasure("scanForTiles", var0, WeatherFxMask::scanForTiles);
                              SpriteRenderer.instance.glIgnoreStyles(true);
                              if (MASKING_ENABLED) {
                                 GameProfiler.getInstance().invokeAndMeasure("drawFxMask", var0, WeatherFxMask::drawFxMask);
                              }

                              if (DEBUG_MASK_AND_PARTICLES) {
                                 SpriteRenderer.instance.glClearColor(0, 0, 0, 255);
                                 SpriteRenderer.instance.glClear(16640);
                                 SpriteRenderer.instance.glClearColor(0, 0, 0, 255);
                              } else if (DEBUG_MASK) {
                                 SpriteRenderer.instance.glClearColor(0, 255, 0, 255);
                                 SpriteRenderer.instance.glClear(16640);
                                 SpriteRenderer.instance.glClearColor(0, 0, 0, 255);
                              }

                              GameProfiler.ProfileArea var2x = GameProfiler.getInstance().startIfEnabled("drawFxLayered");
                              if (!RenderSettings.getInstance().getPlayerSettings(var0).isExterior()) {
                                 drawFxLayered(var0, false, false, false);
                              }

                              if (IsoWeatherFX.instance.hasCloudsToRender()) {
                                 drawFxLayered(var0, true, false, false);
                              }

                              if (IsoWeatherFX.instance.hasFogToRender() && PerformanceSettings.FogQuality == 2) {
                                 drawFxLayered(var0, false, true, false);
                              }

                              if (Core.getInstance().getOptionRenderPrecipitation() == 1 && IsoWeatherFX.instance.hasPrecipitationToRender()) {
                                 drawFxLayered(var0, false, false, true);
                              }

                              GameProfiler.getInstance().end(var2x);
                              SpriteRenderer.GL_BLENDFUNC_ENABLED = true;
                              SpriteRenderer.instance.glIgnoreStyles(false);
                           } else {
                              if (IsoWorld.instance.getCell() != null && IsoWorld.instance.getCell().getWeatherFX() != null) {
                                 SpriteRenderer.instance.glIgnoreStyles(true);
                                 IndieGL.glBlendFunc(770, 771);
                                 IsoWorld.instance.getCell().getWeatherFX().render();
                                 SpriteRenderer.instance.glIgnoreStyles(false);
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

   private static void drawFxMask(int var0) {
      int var1x = IsoCamera.getOffscreenWidth(var0);
      int var2x = IsoCamera.getOffscreenHeight(var0);
      bRenderingMask = true;
      SpriteRenderer.instance.glBuffer(4, var0);
      SpriteRenderer.instance.glDoStartFrameFx(var1x, var2x, var0);
      if (PerformanceSettings.LightingFrameSkip < 3) {
         IsoWorld.instance.getCell().DrawStencilMask();
         IndieGL.glDepthMask(true);
         IndieGL.enableDepthTest();
         SpriteRenderer.instance.glClearColor(0, 0, 0, 0);
         SpriteRenderer.instance.glClear(16640);
         SpriteRenderer.instance.glClearColor(0, 0, 0, 255);
      }

      IndieGL.glDepthMask(false);
      IndieGL.disableDepthTest();
      SpriteRenderer.instance.StartShader(0, var0);
      boolean var3x = true;
      boolean var4 = false;
      WeatherFxMask[] var9 = playerMasks[var0].masks;
      int var10 = playerMasks[var0].maskPointer;

      for(int var11 = 0; var11 < var10; ++var11) {
         WeatherFxMask var12 = var9[var11];
         if (var12.enabled) {
            boolean var6;
            boolean var7;
            if ((var12.flags & 4) == 4) {
               SpriteRenderer.GL_BLENDFUNC_ENABLED = true;
               IndieGL.glBlendFunc(SCR_MASK_SUB, DST_MASK_SUB);
               SpriteRenderer.instance.glBlendEquation(32779);
               IndieGL.enableAlphaTest();
               IndieGL.glAlphaFunc(516, 0.02F);
               SpriteRenderer.GL_BLENDFUNC_ENABLED = false;
               var6 = (var12.flags & 1) == 1;
               var7 = (var12.flags & 2) == 2;
               renderMaskWall(var12.gs, var12.x, var12.y, var12.z, var6, var7, var0);
               SpriteRenderer.GL_BLENDFUNC_ENABLED = true;
               SpriteRenderer.instance.glBlendEquation(32774);
               SpriteRenderer.GL_BLENDFUNC_ENABLED = false;
               boolean var8 = (var12.flags & 64) == 64;
               if (var8 && var12.gs != null) {
                  SpriteRenderer.GL_BLENDFUNC_ENABLED = true;
                  IndieGL.glBlendFunc(SCR_MASK_ADD, DST_MASK_ADD);
                  SpriteRenderer.GL_BLENDFUNC_ENABLED = false;
                  var12.gs.RenderOpenDoorOnly();
               }
            } else {
               SpriteRenderer.GL_BLENDFUNC_ENABLED = true;
               IndieGL.glBlendFunc(SCR_MASK_ADD, DST_MASK_ADD);
               SpriteRenderer.GL_BLENDFUNC_ENABLED = false;
               renderMaskFloor(var12.x, var12.y, var12.z);
               var4 = (var12.flags & 16) == 16;
               boolean var5 = (var12.flags & 8) == 8;
               if (!var4) {
                  var6 = (var12.flags & 1) == 1;
                  var7 = (var12.flags & 2) == 2;
                  if (!var6 && !var7) {
                     if ((var12.flags & 32) == 32) {
                        renderMaskWall(var12.gs, var12.x, var12.y, var12.z, false, false, var0);
                     }
                  } else {
                     renderMaskWall(var12.gs, var12.x, var12.y, var12.z, var6, var7, var0);
                  }
               }

               if (var4 && var12.gs != null) {
                  var12.gs.RenderMinusFloorFxMask(var12.z + 1, false, false);
               }

               if (var5 && var12.gs != null) {
                  var12.gs.renderCharacters(var12.z + 1, false, false);
                  SpriteRenderer.GL_BLENDFUNC_ENABLED = true;
                  IndieGL.glBlendFunc(SCR_MASK_ADD, DST_MASK_ADD);
                  SpriteRenderer.GL_BLENDFUNC_ENABLED = false;
               }
            }
         }
      }

      IndieGL.glBlendFunc(770, 771);
      SpriteRenderer.instance.glBuffer(5, var0);
      SpriteRenderer.instance.glDoEndFrameFx(var0);
      bRenderingMask = false;
   }

   private static void drawFxLayered(int var0, boolean var1x, boolean var2x, boolean var3x) {
      int var4 = IsoCamera.getOffscreenLeft(var0);
      int var5 = IsoCamera.getOffscreenTop(var0);
      int var6 = IsoCamera.getOffscreenWidth(var0);
      int var7 = IsoCamera.getOffscreenHeight(var0);
      int var8 = IsoCamera.getScreenLeft(var0);
      int var9 = IsoCamera.getScreenTop(var0);
      int var10 = IsoCamera.getScreenWidth(var0);
      int var11 = IsoCamera.getScreenHeight(var0);
      IndieGL.glDepthMask(false);
      IndieGL.disableDepthTest();
      SpriteRenderer.instance.glBuffer(6, var0);
      SpriteRenderer.instance.glDoStartFrameFx(var6, var7, var0);
      if (!var1x && !var2x && !var3x) {
         Color var12 = RenderSettings.getInstance().getMaskClearColorForPlayer(var0);
         SpriteRenderer.GL_BLENDFUNC_ENABLED = true;
         IndieGL.glBlendFuncSeparate(SCR_PARTICLES, DST_PARTICLES, 1, 771);
         SpriteRenderer.GL_BLENDFUNC_ENABLED = false;
         SpriteRenderer.instance.renderi(texWhite, 0, 0, var6, var7, var12.r, var12.g, var12.b, var12.a, (Consumer)null);
         SpriteRenderer.GL_BLENDFUNC_ENABLED = true;
      } else if (IsoWorld.instance.getCell() != null && IsoWorld.instance.getCell().getWeatherFX() != null) {
         SpriteRenderer.GL_BLENDFUNC_ENABLED = true;
         IndieGL.glBlendFuncSeparate(SCR_PARTICLES, DST_PARTICLES, 1, 771);
         SpriteRenderer.GL_BLENDFUNC_ENABLED = false;
         IsoWorld.instance.getCell().getWeatherFX().renderLayered(var1x, var2x, var3x);
         SpriteRenderer.GL_BLENDFUNC_ENABLED = true;
      }

      if (MASKING_ENABLED) {
         IndieGL.glBlendFunc(SCR_MERGE, DST_MERGE);
         SpriteRenderer.instance.glBlendEquation(32779);
         ((Texture)fboMask.getTexture()).rendershader2(0.0F, 0.0F, (float)var6, (float)var7, var8, var9, var10, var11, 1.0F, 1.0F, 1.0F, 1.0F);
         SpriteRenderer.instance.glBlendEquation(32774);
      }

      IndieGL.glBlendFunc(770, 771);
      SpriteRenderer.instance.glBuffer(7, var0);
      SpriteRenderer.instance.glDoEndFrameFx(var0);
      Texture var25;
      if ((DEBUG_MASK || DEBUG_MASK_AND_PARTICLES) && !DEBUG_MASK_AND_PARTICLES) {
         var25 = (Texture)fboMask.getTexture();
         IndieGL.glBlendFunc(770, 771);
      } else {
         var25 = (Texture)fboParticles.getTexture();
         IndieGL.glBlendFunc(SCR_FINAL, DST_FINAL);
      }

      float var13 = 1.0F;
      float var14 = 1.0F;
      float var15 = 1.0F;
      float var16 = 1.0F;
      float var21 = (float)var8 / (float)var25.getWidthHW();
      float var22 = (float)var9 / (float)var25.getHeightHW();
      float var23 = (float)(var8 + var10) / (float)var25.getWidthHW();
      float var24 = (float)(var9 + var11) / (float)var25.getHeightHW();
      SpriteRenderer.instance.render(var25, 0.0F, 0.0F, (float)var6, (float)var7, var13, var14, var15, var16, var21, var24, var23, var24, var23, var22, var21, var22);
      IndieGL.glDefaultBlendFunc();
   }

   private static void initGlIds() {
      for(int var0 = 0; var0 < test.length; ++var0) {
         if (test[var0] == SCR_MASK_ADD) {
            ID_SCR_MASK_ADD = var0;
         } else if (test[var0] == DST_MASK_ADD) {
            ID_DST_MASK_ADD = var0;
         } else if (test[var0] == SCR_MASK_SUB) {
            ID_SCR_MASK_SUB = var0;
         } else if (test[var0] == DST_MASK_SUB) {
            ID_DST_MASK_SUB = var0;
         } else if (test[var0] == SCR_PARTICLES) {
            ID_SCR_PARTICLES = var0;
         } else if (test[var0] == DST_PARTICLES) {
            ID_DST_PARTICLES = var0;
         } else if (test[var0] == SCR_MERGE) {
            ID_SCR_MERGE = var0;
         } else if (test[var0] == DST_MERGE) {
            ID_DST_MERGE = var0;
         } else if (test[var0] == SCR_FINAL) {
            ID_SCR_FINAL = var0;
         } else if (test[var0] == DST_FINAL) {
            ID_DST_FINAL = var0;
         }
      }

   }

   private static void updateDebugKeys() {
      if (keypause > 0) {
         --keypause;
      }

      if (keypause == 0) {
         boolean var0 = false;
         boolean var1x = false;
         boolean var2x = false;
         boolean var3x = false;
         boolean var4 = false;
         if (TARGET_BLEND == 0) {
            var1 = ID_SCR_MASK_ADD;
            var2 = ID_DST_MASK_ADD;
         } else if (TARGET_BLEND == 1) {
            var1 = ID_SCR_MASK_SUB;
            var2 = ID_DST_MASK_SUB;
         } else if (TARGET_BLEND == 2) {
            var1 = ID_SCR_MERGE;
            var2 = ID_DST_MERGE;
         } else if (TARGET_BLEND == 3) {
            var1 = ID_SCR_FINAL;
            var2 = ID_DST_FINAL;
         } else if (TARGET_BLEND == 4) {
            var1 = ID_SCR_PARTICLES;
            var2 = ID_DST_PARTICLES;
         }

         if (GameKeyboard.isKeyDown(79)) {
            --var1;
            if (var1 < 0) {
               var1 = test.length - 1;
            }

            var0 = true;
         } else if (GameKeyboard.isKeyDown(81)) {
            ++var1;
            if (var1 >= test.length) {
               var1 = 0;
            }

            var0 = true;
         } else if (GameKeyboard.isKeyDown(75)) {
            --var2;
            if (var2 < 0) {
               var2 = test.length - 1;
            }

            var0 = true;
         } else if (GameKeyboard.isKeyDown(77)) {
            ++var2;
            if (var2 >= test.length) {
               var2 = 0;
            }

            var0 = true;
         } else if (GameKeyboard.isKeyDown(71)) {
            --TARGET_BLEND;
            if (TARGET_BLEND < 0) {
               TARGET_BLEND = 4;
            }

            var0 = true;
            var1x = true;
         } else if (GameKeyboard.isKeyDown(73)) {
            ++TARGET_BLEND;
            if (TARGET_BLEND >= 5) {
               TARGET_BLEND = 0;
            }

            var0 = true;
            var1x = true;
         } else if (MASKING_ENABLED && GameKeyboard.isKeyDown(82)) {
            DEBUG_MASK = !DEBUG_MASK;
            var0 = true;
            var2x = true;
         } else if (MASKING_ENABLED && GameKeyboard.isKeyDown(80)) {
            DEBUG_MASK_AND_PARTICLES = !DEBUG_MASK_AND_PARTICLES;
            var0 = true;
            var3x = true;
         } else if (!GameKeyboard.isKeyDown(72) && GameKeyboard.isKeyDown(76)) {
            MASKING_ENABLED = !MASKING_ENABLED;
            var0 = true;
            var4 = true;
         }

         if (var0) {
            if (var1x) {
               if (TARGET_BLEND == 0) {
                  DebugLog.log("TargetBlend = MASK_ADD");
               } else if (TARGET_BLEND == 1) {
                  DebugLog.log("TargetBlend = MASK_SUB");
               } else if (TARGET_BLEND == 2) {
                  DebugLog.log("TargetBlend = MERGE");
               } else if (TARGET_BLEND == 3) {
                  DebugLog.log("TargetBlend = FINAL");
               } else if (TARGET_BLEND == 4) {
                  DebugLog.log("TargetBlend = PARTICLES");
               }
            } else if (var2x) {
               DebugLog.log("DEBUG_MASK = " + DEBUG_MASK);
            } else if (var3x) {
               DebugLog.log("DEBUG_MASK_AND_PARTICLES = " + DEBUG_MASK_AND_PARTICLES);
            } else if (var4) {
               DebugLog.log("MASKING_ENABLED = " + MASKING_ENABLED);
            } else {
               if (TARGET_BLEND == 0) {
                  ID_SCR_MASK_ADD = var1;
                  ID_DST_MASK_ADD = var2;
                  SCR_MASK_ADD = test[ID_SCR_MASK_ADD];
                  DST_MASK_ADD = test[ID_DST_MASK_ADD];
               } else if (TARGET_BLEND == 1) {
                  ID_SCR_MASK_SUB = var1;
                  ID_DST_MASK_SUB = var2;
                  SCR_MASK_SUB = test[ID_SCR_MASK_SUB];
                  DST_MASK_SUB = test[ID_DST_MASK_SUB];
               } else if (TARGET_BLEND == 2) {
                  ID_SCR_MERGE = var1;
                  ID_DST_MERGE = var2;
                  SCR_MERGE = test[ID_SCR_MERGE];
                  DST_MERGE = test[ID_DST_MERGE];
               } else if (TARGET_BLEND == 3) {
                  ID_SCR_FINAL = var1;
                  ID_DST_FINAL = var2;
                  SCR_FINAL = test[ID_SCR_FINAL];
                  DST_FINAL = test[ID_DST_FINAL];
               } else if (TARGET_BLEND == 4) {
                  ID_SCR_PARTICLES = var1;
                  ID_DST_PARTICLES = var2;
                  SCR_PARTICLES = test[ID_SCR_PARTICLES];
                  DST_PARTICLES = test[ID_DST_PARTICLES];
               }

               String var10000 = testNames[var1];
               DebugLog.log("Blendmode = " + var10000 + " -> " + testNames[var2]);
            }

            keypause = 30;
         }
      }

   }

   static {
      offsetX = (float)(32 * Core.TileScale);
      offsetY = (float)(96 * Core.TileScale);
      defColorInfo = new ColorInfo();
      DIAMOND_ROWS = 1000;
      playerMasks = new PlayerFxMask[4];
      dmiter = new DiamondMatrixIterator(0);
      diamondMatrixPos = new Vector2i();
      tempRasterizeBounds = new RasterizeBounds();
      rasterizeBounds = new RasterizeBounds[4];
      rasterize = new Rasterize();
      tmpVec = new Vector3f();
      tmpTorch = new IsoGameCharacter.TorchInfo();
      tmpColInfo = new ColorInfo();
      test = new int[]{0, 1, 768, 769, 774, 775, 770, 771, 772, 773, 32769, 32770, 32771, 32772, 776, 35065, 35066, 34185, 35067};
      testNames = new String[]{"GL_ZERO", "GL_ONE", "GL_SRC_COLOR", "GL_ONE_MINUS_SRC_COLOR", "GL_DST_COLOR", "GL_ONE_MINUS_DST_COLOR", "GL_SRC_ALPHA", "GL_ONE_MINUS_SRC_ALPHA", "GL_DST_ALPHA", "GL_ONE_MINUS_DST_ALPHA", "GL_CONSTANT_COLOR", "GL_ONE_MINUS_CONSTANT_COLOR", "GL_CONSTANT_ALPHA", "GL_ONE_MINUS_CONSTANT_ALPHA", "GL_SRC_ALPHA_SATURATE", "GL_SRC1_COLOR (33)", "GL_ONE_MINUS_SRC1_COLOR (33)", "GL_SRC1_ALPHA (15)", "GL_ONE_MINUS_SRC1_ALPHA (33)"};
      var1 = 1;
      var2 = 1;
      var3 = 1.0F;
      SCR_MASK_ADD = 770;
      DST_MASK_ADD = 771;
      SCR_MASK_SUB = 0;
      DST_MASK_SUB = 0;
      SCR_PARTICLES = 1;
      DST_PARTICLES = 771;
      SCR_MERGE = 770;
      DST_MERGE = 771;
      SCR_FINAL = 770;
      DST_FINAL = 771;
      TARGET_BLEND = 0;
      DEBUG_MASK = false;
      MASKING_ENABLED = true;
      DEBUG_MASK_AND_PARTICLES = false;
      keypause = 0;
   }

   public static class PlayerFxMask {
      private WeatherFxMask[] masks;
      private int maskPointer = 0;
      private boolean maskEnabled = false;
      private IsoGridSquare plrSquare;
      private int DISABLED_MASKS = 0;
      private boolean requiresUpdate = false;
      private boolean hasMaskToDraw = true;
      private int playerIndex;
      private IsoPlayer player;
      private int playerZ;
      private IWorldRegion curIsoWorldRegion;
      private final ArrayList<IWorldRegion> curConnectedRegions = new ArrayList();
      private final ArrayList<IWorldRegion> isoWorldRegionTemp = new ArrayList();
      private final TLongObjectHashMap<WeatherFxMask> maskHashMap = new TLongObjectHashMap();
      private boolean DIAMOND_ITER_DONE = false;
      private boolean isFirstSquare = true;
      private IsoGridSquare firstSquare;

      public PlayerFxMask() {
      }

      private void init() {
         this.masks = new WeatherFxMask[30000];

         for(int var1 = 0; var1 < this.masks.length; ++var1) {
            if (this.masks[var1] == null) {
               this.masks[var1] = new WeatherFxMask();
            }
         }

         this.maskEnabled = true;
      }

      private void initMask() {
         if (!GameServer.bServer) {
            if (!this.maskEnabled) {
               this.init();
            }

            this.playerIndex = IsoCamera.frameState.playerIndex;
            this.player = IsoPlayer.players[this.playerIndex];
            this.playerZ = PZMath.fastfloor(this.player.getZ());
            this.DIAMOND_ITER_DONE = false;
            this.requiresUpdate = false;
            if (this.player != null) {
               if (this.isFirstSquare || this.plrSquare == null || this.plrSquare != this.player.getSquare()) {
                  this.plrSquare = this.player.getSquare();
                  this.maskPointer = 0;
                  this.maskHashMap.clear();
                  this.DISABLED_MASKS = 0;
                  this.requiresUpdate = true;
                  if (this.firstSquare == null) {
                     this.firstSquare = this.plrSquare;
                  }

                  if (this.firstSquare != null && this.firstSquare != this.plrSquare) {
                     this.isFirstSquare = false;
                  }
               }

               this.curIsoWorldRegion = this.player.getMasterRegion();
               this.curConnectedRegions.clear();
               if (this.curIsoWorldRegion != null && this.player.getMasterRegion().isFogMask()) {
                  this.isoWorldRegionTemp.clear();
                  this.isoWorldRegionTemp.add(this.curIsoWorldRegion);

                  label79:
                  while(true) {
                     IWorldRegion var1;
                     do {
                        if (this.isoWorldRegionTemp.size() <= 0) {
                           break label79;
                        }

                        var1 = (IWorldRegion)this.isoWorldRegionTemp.remove(0);
                        this.curConnectedRegions.add(var1);
                     } while(var1.getNeighbors().size() == 0);

                     Iterator var2 = var1.getNeighbors().iterator();

                     while(var2.hasNext()) {
                        IsoWorldRegion var3 = (IsoWorldRegion)var2.next();
                        if (!this.isoWorldRegionTemp.contains(var3) && !this.curConnectedRegions.contains(var3) && var3.isFogMask()) {
                           this.isoWorldRegionTemp.add(var3);
                        }
                     }
                  }
               } else {
                  this.curIsoWorldRegion = null;
               }
            }

            if (IsoWeatherFX.instance == null) {
               this.hasMaskToDraw = false;
            } else {
               this.hasMaskToDraw = true;
               if (this.hasMaskToDraw) {
                  if ((this.player.getSquare() == null || this.player.getSquare().getBuilding() == null && this.player.getSquare().Is(IsoFlagType.exterior)) && (this.curIsoWorldRegion == null || !this.curIsoWorldRegion.isFogMask())) {
                     this.hasMaskToDraw = false;
                  } else {
                     this.hasMaskToDraw = true;
                  }
               }

            }
         }
      }

      private void addMask(int var1, int var2, int var3, IsoGridSquare var4, int var5) {
         this.addMask(var1, var2, var3, var4, var5, true);
      }

      private void addMask(int var1, int var2, int var3, IsoGridSquare var4, int var5, boolean var6) {
         if (this.hasMaskToDraw && this.requiresUpdate) {
            if (!this.maskEnabled) {
               this.init();
            }

            WeatherFxMask var7 = this.getMask(var1, var2, var3);
            WeatherFxMask var8;
            if (var7 == null) {
               var8 = this.getFreeMask();
               var8.x = var1;
               var8.y = var2;
               var8.z = var3;
               var8.flags = var5;
               var8.gs = var4;
               var8.enabled = var6;
               if (!var6 && this.DISABLED_MASKS < WeatherFxMask.DIAMOND_ROWS) {
                  ++this.DISABLED_MASKS;
               }

               this.maskHashMap.put((long)var2 << 32 | (long)var1, var8);
            } else {
               if (var7.flags != var5) {
                  var7.flags |= var5;
               }

               if (!var7.enabled && var6) {
                  var8 = this.getFreeMask();
                  var8.x = var1;
                  var8.y = var2;
                  var8.z = var3;
                  var8.flags = var7.flags;
                  var8.gs = var4;
                  var8.enabled = var6;
                  this.maskHashMap.put((long)var2 << 32 | (long)var1, var8);
               } else {
                  var7.enabled = var7.enabled ? var7.enabled : var6;
                  if (var6 && var4 != null && var7.gs == null) {
                     var7.gs = var4;
                  }
               }
            }

         }
      }

      private WeatherFxMask getFreeMask() {
         if (this.maskPointer >= this.masks.length) {
            DebugLog.log("Weather Mask buffer out of bounds. Increasing cache.");
            WeatherFxMask[] var1 = this.masks;
            this.masks = new WeatherFxMask[this.masks.length + 10000];

            for(int var2 = 0; var2 < this.masks.length; ++var2) {
               if (var2 < var1.length && var1[var2] != null) {
                  this.masks[var2] = var1[var2];
               } else {
                  this.masks[var2] = new WeatherFxMask();
               }
            }
         }

         return this.masks[this.maskPointer++];
      }

      private boolean masksContains(int var1, int var2, int var3) {
         return this.getMask(var1, var2, var3) != null;
      }

      private WeatherFxMask getMask(int var1, int var2, int var3) {
         return (WeatherFxMask)this.maskHashMap.get((long)var2 << 32 | (long)var1);
      }
   }

   public static final class RasterizeBounds {
      float x1;
      float y1;
      float x2;
      float y2;
      float x3;
      float y3;
      float x4;
      float y4;
      int cx1;
      int cy1;
      int cx2;
      int cy2;
      int cx3;
      int cy3;
      int cx4;
      int cy4;

      public RasterizeBounds() {
      }

      public void calculate(int var1, int var2) {
         byte var3 = 0;
         byte var4 = 0;
         int var5 = var3 + IsoCamera.getOffscreenWidth(var1);
         int var6 = var4 + IsoCamera.getOffscreenHeight(var1);
         this.x1 = IsoUtils.XToIso((float)var3, (float)var4, (float)var2);
         this.y1 = IsoUtils.YToIso((float)var3, (float)var4, (float)var2);
         this.x2 = IsoUtils.XToIso((float)var5, (float)var4, (float)var2);
         this.y2 = IsoUtils.YToIso((float)var5, (float)var4, (float)var2);
         this.x3 = IsoUtils.XToIso((float)var5, (float)var6, (float)var2);
         this.y3 = IsoUtils.YToIso((float)var5, (float)var6, (float)var2);
         this.x4 = IsoUtils.XToIso((float)var3, (float)var6, (float)var2);
         this.y4 = IsoUtils.YToIso((float)var3, (float)var6, (float)var2);
         this.cx1 = (int)IsoUtils.XToScreen((float)PZMath.fastfloor(this.x1) - 0.5F, (float)PZMath.fastfloor(this.y1) + 0.5F, -666.0F, -666);
         this.cy1 = (int)IsoUtils.YToScreen((float)PZMath.fastfloor(this.x1) - 0.5F, (float)PZMath.fastfloor(this.y1) + 0.5F, -666.0F, -666);
         this.cx2 = (int)IsoUtils.XToScreen((float)PZMath.fastfloor(this.x2) + 0.5F, (float)PZMath.fastfloor(this.y2) - 0.5F, -666.0F, -666);
         this.cy2 = (int)IsoUtils.YToScreen((float)PZMath.fastfloor(this.x2) + 0.5F, (float)PZMath.fastfloor(this.y2) - 0.5F, -666.0F, -666);
         this.cx3 = (int)IsoUtils.XToScreen((float)PZMath.fastfloor(this.x3) + 1.5F, (float)PZMath.fastfloor(this.y3) + 0.5F, -666.0F, -666);
         this.cy3 = (int)IsoUtils.YToScreen((float)PZMath.fastfloor(this.x3) + 1.5F, (float)PZMath.fastfloor(this.y3) + 0.5F, -666.0F, -666);
         this.cx4 = (int)IsoUtils.XToScreen((float)PZMath.fastfloor(this.x4) + 0.5F, (float)PZMath.fastfloor(this.y4) + 1.5F, -666.0F, -666);
         this.cy4 = (int)IsoUtils.YToScreen((float)PZMath.fastfloor(this.x4) + 0.5F, (float)PZMath.fastfloor(this.y4) + 1.5F, -666.0F, -666);
         this.x3 += 3.0F;
         this.y3 += 3.0F;
         this.x4 += 3.0F;
         this.y4 += 3.0F;
      }

      public boolean equals(Object var1) {
         RasterizeBounds var2 = (RasterizeBounds)Type.tryCastTo(var1, RasterizeBounds.class);
         if (var2 == null) {
            return false;
         } else {
            return this.cx1 == var2.cx1 && this.cy1 == var2.cy1 && this.cx2 == var2.cx2 && this.cy2 == var2.cy2 && this.cx3 == var2.cx3 && this.cy3 == var2.cy3 && this.cx4 == var2.cx4 && this.cy4 == var2.cy4;
         }
      }
   }
}
