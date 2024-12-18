package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;
import zombie.GameProfiler;
import zombie.IndieGL;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.random.Rand;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.erosion.utils.Noise2D;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.shapers.FloorShaper;
import zombie.iso.sprite.shapers.FloorShaperAttachedSprites;
import zombie.iso.sprite.shapers.FloorShaperDiamond;
import zombie.iso.weather.ClimateManager;

public final class FBORenderSnow {
   private static FBORenderSnow instance = null;
   private static final int NoiseGridSize = 256;
   private static final int ChunkSnowGridSize = 10;
   private boolean hasSetupSnowGrid = false;
   private SnowGridTiles snowGridTiles_Square;
   private SnowGridTiles[] snowGridTiles_Strip;
   private SnowGridTiles[] snowGridTiles_Edge;
   private SnowGridTiles[] snowGridTiles_Cove;
   private SnowGridTiles snowGridTiles_Enclosed;
   private int m_snowFirstNonSquare = -1;
   private final boolean[] snowTileHasSeamE = new boolean[48];
   private final boolean[] snowTileHasSeamS = new boolean[48];
   private final HashMap<String, Integer> snowTileNameToTilesetIndex = new HashMap();
   private final Noise2D snowNoise2D = new Noise2D();
   private SnowGrid snowGridCur;
   private int snowFracTarget = 0;
   private static final int SNOWSHORE_NONE = 0;
   private static final int SNOWSHORE_N = 1;
   private static final int SNOWSHORE_E = 2;
   private static final int SNOWSHORE_S = 4;
   private static final int SNOWSHORE_W = 8;

   public FBORenderSnow() {
   }

   public static FBORenderSnow getInstance() {
      if (instance == null) {
         instance = new FBORenderSnow();
      }

      return instance;
   }

   private void initSnowGrid() {
      this.snowNoise2D.reset();
      this.snowNoise2D.addLayer(16, 0.5F, 3.0F);
      this.snowNoise2D.addLayer(32, 2.0F, 5.0F);
      this.snowNoise2D.addLayer(64, 5.0F, 8.0F);
      byte var1 = 0;
      byte var5 = (byte)(var1 + 1);
      this.snowGridTiles_Square = new SnowGridTiles(var1);
      byte var2 = 40;

      int var3;
      for(var3 = 0; var3 < 4; ++var3) {
         this.snowGridTiles_Square.add(Texture.getSharedTexture("e_newsnow_ground_1_" + (var2 + var3)));
      }

      this.snowGridTiles_Enclosed = new SnowGridTiles(var5++);
      var2 = 0;

      for(var3 = 0; var3 < 4; ++var3) {
         this.snowGridTiles_Enclosed.add(Texture.getSharedTexture("e_newsnow_ground_1_" + (var2 + var3)));
      }

      this.snowGridTiles_Cove = new SnowGridTiles[4];

      int var4;
      for(var3 = 0; var3 < 4; ++var3) {
         this.snowGridTiles_Cove[var3] = new SnowGridTiles(var5++);
         if (var3 == 0) {
            var2 = 7;
         }

         if (var3 == 2) {
            var2 = 4;
         }

         if (var3 == 1) {
            var2 = 5;
         }

         if (var3 == 3) {
            var2 = 6;
         }

         for(var4 = 0; var4 < 3; ++var4) {
            this.snowGridTiles_Cove[var3].add(Texture.getSharedTexture("e_newsnow_ground_1_" + (var2 + var4 * 4)));
         }
      }

      this.m_snowFirstNonSquare = var5;
      this.snowGridTiles_Edge = new SnowGridTiles[4];

      for(var3 = 0; var3 < 4; ++var3) {
         this.snowGridTiles_Edge[var3] = new SnowGridTiles(var5++);
         if (var3 == 0) {
            var2 = 16;
         }

         if (var3 == 2) {
            var2 = 18;
         }

         if (var3 == 1) {
            var2 = 17;
         }

         if (var3 == 3) {
            var2 = 19;
         }

         for(var4 = 0; var4 < 3; ++var4) {
            this.snowGridTiles_Edge[var3].add(Texture.getSharedTexture("e_newsnow_ground_1_" + (var2 + var4 * 4)));
         }
      }

      this.snowGridTiles_Strip = new SnowGridTiles[4];

      for(var3 = 0; var3 < 4; ++var3) {
         this.snowGridTiles_Strip[var3] = new SnowGridTiles(var5++);
         if (var3 == 0) {
            var2 = 28;
         }

         if (var3 == 2) {
            var2 = 29;
         }

         if (var3 == 1) {
            var2 = 31;
         }

         if (var3 == 3) {
            var2 = 30;
         }

         for(var4 = 0; var4 < 3; ++var4) {
            this.snowGridTiles_Strip[var3].add(Texture.getSharedTexture("e_newsnow_ground_1_" + (var2 + var4 * 4)));
         }
      }

      this.snowTileHasSeamE[0] = this.snowTileHasSeamE[1] = this.snowTileHasSeamE[2] = this.snowTileHasSeamE[3] = this.snowTileHasSeamE[5] = this.snowTileHasSeamE[6] = this.snowTileHasSeamE[7] = true;
      this.snowTileHasSeamE[9] = this.snowTileHasSeamE[10] = this.snowTileHasSeamE[11] = this.snowTileHasSeamE[13] = this.snowTileHasSeamE[14] = this.snowTileHasSeamE[15] = true;
      this.snowTileHasSeamE[17] = this.snowTileHasSeamE[19] = this.snowTileHasSeamE[21] = this.snowTileHasSeamE[23] = true;
      this.snowTileHasSeamE[25] = this.snowTileHasSeamE[27] = this.snowTileHasSeamE[30] = true;
      this.snowTileHasSeamE[34] = this.snowTileHasSeamE[38] = true;
      this.snowTileHasSeamE[40] = this.snowTileHasSeamE[41] = this.snowTileHasSeamE[42] = this.snowTileHasSeamE[43] = this.snowTileHasSeamE[44] = this.snowTileHasSeamE[45] = this.snowTileHasSeamE[46] = this.snowTileHasSeamE[47] = true;
      this.snowTileHasSeamS[0] = this.snowTileHasSeamS[1] = this.snowTileHasSeamS[2] = this.snowTileHasSeamS[3] = this.snowTileHasSeamS[4] = this.snowTileHasSeamS[5] = this.snowTileHasSeamS[6] = true;
      this.snowTileHasSeamS[8] = this.snowTileHasSeamS[9] = this.snowTileHasSeamS[10] = this.snowTileHasSeamS[12] = this.snowTileHasSeamS[13] = this.snowTileHasSeamS[14] = true;
      this.snowTileHasSeamS[17] = this.snowTileHasSeamS[18] = this.snowTileHasSeamS[21] = this.snowTileHasSeamS[22] = true;
      this.snowTileHasSeamS[25] = this.snowTileHasSeamS[26] = this.snowTileHasSeamS[31] = true;
      this.snowTileHasSeamS[35] = this.snowTileHasSeamS[39] = true;
      this.snowTileHasSeamS[40] = this.snowTileHasSeamS[41] = this.snowTileHasSeamS[42] = this.snowTileHasSeamS[43] = this.snowTileHasSeamS[44] = this.snowTileHasSeamS[45] = this.snowTileHasSeamS[46] = this.snowTileHasSeamS[47] = true;

      for(var3 = 0; var3 < 48; ++var3) {
         this.snowTileNameToTilesetIndex.put("e_newsnow_ground_1_" + var3, var3);
      }

   }

   private ChunkLevel getChunkLevel(int var1, IsoChunk var2, int var3) {
      FBORenderLevels var4 = var2.getRenderLevels(var1);
      return var3 == 0 ? var4.m_snowLevelZero : var4.m_snowLevelNotZero;
   }

   private SnowGrid setChunkSnowGrid(int var1, IsoChunk var2, int var3, SnowGrid var4) {
      ChunkLevel var5 = this.getChunkLevel(var1, var2, var3);
      var5.m_snowGrid = var4;
      return var4;
   }

   private SnowGrid getChunkSnowGrid(int var1, IsoChunk var2, int var3) {
      ChunkLevel var4 = this.getChunkLevel(var1, var2, var3);
      return var4.m_snowGrid;
   }

   private void updateSnow(IsoChunk var1, int var2, int var3) {
      int var4 = var1.wx * 8 - 1;
      int var5 = var1.wy * 8 - 1;
      int var6 = IsoCamera.frameState.playerIndex;
      ChunkLevel var7 = this.getChunkLevel(var6, var1, var2);
      if (var7.m_snowGrid == null) {
         var7.m_snowGrid = new SnowGrid(var4, var5, var2, var3);
         var7.m_adjacentChunkLoadedCounter = var1.m_adjacentChunkLoadedCounter;
         this.snowGridCur = var7.m_snowGrid;
      } else {
         this.snowGridCur = var7.m_snowGrid;
         if (var1.m_adjacentChunkLoadedCounter != var7.m_adjacentChunkLoadedCounter) {
            var7.m_adjacentChunkLoadedCounter = var1.m_adjacentChunkLoadedCounter;
            this.snowGridCur.frac = -1;
         }

         if (var4 != this.snowGridCur.worldX || var5 != this.snowGridCur.worldY || var3 != this.snowGridCur.frac) {
            this.snowGridCur.init(var4, var5, var2, var3);
         }

      }
   }

   public boolean gridSquareIsSnow(int var1, int var2, int var3) {
      if (IsoWorld.instance.CurrentCell.getSnowTarget() <= 0) {
         return false;
      } else {
         IsoGridSquare var4 = IsoWorld.instance.CurrentCell.getGridSquare(var1, var2, var3);
         if (var4 != null && var4.chunk != null) {
            int var5 = IsoPlayer.getPlayerIndex();
            this.snowGridCur = this.getChunkSnowGrid(var5, var4.chunk, var3);
            if (this.snowGridCur != null) {
               if (!var4.getProperties().Is(IsoFlagType.solidfloor)) {
                  return false;
               } else if (var4.getProperties().Is(IsoFlagType.water) || var4.getWater() != null && var4.getWater().isValid()) {
                  return false;
               } else if (var4.getProperties().Is(IsoFlagType.exterior) && var4.room == null && !var4.isInARoom()) {
                  int var6 = this.snowGridCur.worldToSelfX(var4.getX());
                  int var7 = this.snowGridCur.worldToSelfY(var4.getY());
                  return this.snowGridCur.check(var6, var7);
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
   }

   public void RenderSnow(IsoChunk var1, int var2) {
      if (DebugOptions.instance.Weather.Snow.getValue()) {
         this.snowFracTarget = IsoWorld.instance.CurrentCell.getSnowTarget();
         this.updateSnow(var1, var2, this.snowFracTarget);
         SnowGrid var3 = this.snowGridCur;
         if (var3 != null) {
            if (var3.frac > 0) {
               float var4 = 1.0F;
               Shader var5 = null;
               if (DebugOptions.instance.Terrain.RenderTiles.UseShaders.getValue()) {
                  var5 = IsoCell.m_floorRenderShader;
               }

               FloorShaperAttachedSprites.instance.setShore(false);
               FloorShaperDiamond.instance.setShore(false);
               IndieGL.StartShader(var5, IsoCamera.frameState.playerIndex);
               int var6 = (int)IsoCamera.frameState.OffX;
               int var7 = (int)IsoCamera.frameState.OffY;

               for(int var8 = 0; var8 < IsoCell.SolidFloor.size(); ++var8) {
                  IsoGridSquare var9 = (IsoGridSquare)IsoCell.SolidFloor.get(var8);
                  if (var9.room == null && var9.getProperties().Is(IsoFlagType.exterior) && var9.getProperties().Is(IsoFlagType.solidfloor)) {
                     int var10;
                     if (var9.getProperties().Is(IsoFlagType.water) || var9.getWater() != null && var9.getWater().isValid()) {
                        var10 = this.getShoreInt(var9);
                        if (var10 == 0) {
                           continue;
                        }
                     } else {
                        var10 = 0;
                     }

                     int var11 = var3.worldToSelfX(var9.getX());
                     int var12 = var3.worldToSelfY(var9.getY());
                     float var13 = IsoUtils.XToScreen((float)var9.getX(), (float)var9.getY(), (float)var2, 0);
                     float var14 = IsoUtils.YToScreen((float)var9.getX(), (float)var9.getY(), (float)var2, 0);
                     var13 -= (float)var6;
                     var14 -= (float)var7;
                     if (PerformanceSettings.FBORenderChunk && FBORenderChunkManager.instance.isCaching()) {
                        var13 = IsoUtils.XToScreen((float)PZMath.coordmodulo(var9.getX(), 8), (float)PZMath.coordmodulo(var9.getY(), 8), (float)var2, 0);
                        var14 = IsoUtils.YToScreen((float)PZMath.coordmodulo(var9.getX(), 8), (float)PZMath.coordmodulo(var9.getY(), 8), (float)var2, 0);
                        var13 += FBORenderChunkManager.instance.getXOffset();
                        var14 += FBORenderChunkManager.instance.getYOffset();
                     }

                     float var15 = (float)(32 * Core.TileScale);
                     float var16 = (float)(96 * Core.TileScale);
                     var13 -= var15;
                     var14 -= var16;
                     if (var9.getProperties().Is(IsoFlagType.FloorHeightOneThird)) {
                        var14 -= (float)(32 * Core.TileScale);
                     }

                     if (var9.getProperties().Is(IsoFlagType.FloorHeightTwoThirds)) {
                        var14 -= (float)(64 * Core.TileScale);
                     }

                     this.setVertColors(var9);

                     for(int var17 = 0; var17 < 2; ++var17) {
                        this.renderSnowTileGeneral(var3, var4, var9, var10, var11, var12, (int)var13, (int)var14, var17);
                     }
                  }
               }

               IndieGL.StartShader((Shader)null);
            }
         }
      }
   }

   private void setVertColors(IsoGridSquare var1) {
      this.setVertColors(var1, 0, 1, 2, 3);
   }

   private void setVertColors(IsoGridSquare var1, int var2, int var3, int var4, int var5) {
      int var6 = IsoCamera.frameState.playerIndex;
      int var7 = var1.getVertLight(var2, var6);
      int var8 = var1.getVertLight(var3, var6);
      int var9 = var1.getVertLight(var4, var6);
      int var10 = var1.getVertLight(var5, var6);
      if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Floor.LightingDebug.getValue()) {
         var7 = -65536;
         var8 = -65536;
         var9 = -16776961;
         var10 = -16776961;
      }

      FloorShaperAttachedSprites.instance.setVertColors(var7, var8, var9, var10);
      FloorShaperDiamond.instance.setVertColors(var7, var8, var9, var10);
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
         byte var23 = var1.gridType[var2][var3][var4];
         this.renderSnowTileBase(var7, var8, var9, var10, var23 < this.m_snowFirstNonSquare);
         if (PerformanceSettings.FBORenderChunk && DebugOptions.instance.FBORenderChunk.SeamFix2.getValue()) {
            Integer var21 = (Integer)this.snowTileNameToTilesetIndex.getOrDefault(var7.getName(), (Object)null);
            IsoGridSquare var22;
            if (PZMath.coordmodulo(var5.y, 8) == 7 && var21 != null && this.snowTileHasSeamS[var21]) {
               var22 = var5.getAdjacentSquare(IsoDirections.S);
               if (var22 != null && var22.getFloor() != null) {
                  this.setVertColors(var5, 3, 2, 2, 3);
                  this.renderSnowTileBase(Texture.getSharedTexture("e_newsnow_ground_1_28"), var8 - 64, var9 + 32, var10, false);
               }
            }

            if (PZMath.coordmodulo(var5.x, 8) == 7 && var21 != null && this.snowTileHasSeamE[var21]) {
               var22 = var5.getAdjacentSquare(IsoDirections.E);
               if (var22 != null && var22.getFloor() != null) {
                  this.setVertColors(var5, 1, 1, 2, 2);
                  this.renderSnowTileBase(Texture.getSharedTexture("e_newsnow_ground_1_29"), var8 + 64, var9 + 32, var10, false);
               }
            }
         }

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

   private int getShoreInt(IsoGridSquare var1) {
      int var2 = 0;
      if (this.isSnowShore(var1, 0, -1)) {
         var2 |= 1;
      }

      if (this.isSnowShore(var1, 1, 0)) {
         var2 |= 2;
      }

      if (this.isSnowShore(var1, 0, 1)) {
         var2 |= 4;
      }

      if (this.isSnowShore(var1, -1, 0)) {
         var2 |= 8;
      }

      return var2;
   }

   private boolean isSnowShore(IsoGridSquare var1, int var2, int var3) {
      IsoGridSquare var4 = IsoWorld.instance.getCell().getGridSquare(var1.getX() + var2, var1.getY() + var3, 0);
      boolean var5 = var4 != null && var4.getWater() != null && var4.getWater().isValid();
      return var4 != null && !var4.getProperties().Is(IsoFlagType.water) && !var5;
   }

   private static final class SnowGridTiles {
      byte ID = -1;
      int counter = -1;
      final ArrayList<Texture> textures = new ArrayList();

      public SnowGridTiles(byte var1) {
         this.ID = var1;
      }

      void add(Texture var1) {
         this.textures.add(var1);
      }

      Texture getAt(int var1, int var2) {
         var1 -= IsoWorld.instance.MetaGrid.getMinX() * IsoCell.CellSizeInSquares;
         var2 -= IsoWorld.instance.MetaGrid.getMinX() * IsoCell.CellSizeInSquares;
         int var3 = (var1 + var2) % this.textures.size();
         return (Texture)this.textures.get(var3);
      }

      Texture getNext() {
         ++this.counter;
         if (this.counter >= this.textures.size()) {
            this.counter = 0;
         }

         return (Texture)this.textures.get(this.counter);
      }

      Texture get(int var1) {
         return (Texture)this.textures.get(var1);
      }

      int size() {
         return this.textures.size();
      }

      Texture getRand() {
         return (Texture)this.textures.get(Rand.Next(4));
      }

      boolean contains(Texture var1) {
         return this.textures.contains(var1);
      }

      void resetCounter() {
         this.counter = 0;
      }
   }

   private static final class SnowGrid {
      public int worldX;
      public int worldY;
      public int w = 10;
      public int h = 10;
      public int frac = 0;
      public static final int N = 0;
      public static final int S = 1;
      public static final int W = 2;
      public static final int E = 3;
      public static final int A = 0;
      public static final int B = 1;
      public final Texture[][][] grid;
      public final byte[][][] gridType;

      public SnowGrid(int var1, int var2, int var3, int var4) {
         this.grid = new Texture[this.w][this.h][2];
         this.gridType = new byte[this.w][this.h][2];
         this.init(var1, var2, var3, var4);
      }

      public SnowGrid init(int var1, int var2, int var3, int var4) {
         if (!FBORenderSnow.instance.hasSetupSnowGrid) {
            FBORenderSnow.instance.initSnowGrid();
            FBORenderSnow.instance.hasSetupSnowGrid = true;
         }

         FBORenderSnow.instance.snowGridTiles_Square.resetCounter();
         FBORenderSnow.instance.snowGridTiles_Enclosed.resetCounter();

         for(int var5 = 0; var5 < 4; ++var5) {
            FBORenderSnow.instance.snowGridTiles_Cove[var5].resetCounter();
            FBORenderSnow.instance.snowGridTiles_Edge[var5].resetCounter();
            FBORenderSnow.instance.snowGridTiles_Strip[var5].resetCounter();
         }

         this.worldX = var1;
         this.worldY = var2;
         this.frac = var4;
         boolean var17 = ClimateManager.getInstance().getSeason().isSeason(5);
         int var8;
         if (!var17) {
            for(int var18 = 0; var18 < this.h; ++var18) {
               for(int var19 = 0; var19 < this.w; ++var19) {
                  for(var8 = 0; var8 < 2; ++var8) {
                     this.grid[var19][var18][var8] = null;
                     this.gridType[var19][var18][var8] = -1;
                  }
               }
            }

            return this;
         } else {
            Noise2D var6 = FBORenderSnow.instance.snowNoise2D;
            GameProfiler.ProfileArea var7 = GameProfiler.getInstance().startIfEnabled("Noise");

            int var9;
            boolean var11;
            for(var8 = 0; var8 < this.h; ++var8) {
               for(var9 = 0; var9 < this.w; ++var9) {
                  int var10;
                  for(var10 = 0; var10 < 2; ++var10) {
                     this.grid[var9][var8][var10] = null;
                     this.gridType[var9][var8][var10] = -1;
                  }

                  if (var3 == 0) {
                     IsoGridSquare var21 = IsoWorld.instance.CurrentCell.getGridSquare(this.worldX + var9, this.worldY + var8, var3);
                     if (var21 == null) {
                        continue;
                     }

                     var11 = false;

                     for(int var12 = 0; var12 < 8; ++var12) {
                        IsoDirections var13 = IsoDirections.fromIndex(var12);
                        IsoGridSquare var14 = var21.getAdjacentSquare(var13);
                        if (var14 != null && var14.getWater() != null && var14.getWater().isValid()) {
                           var11 = true;
                           break;
                        }
                     }

                     if (var11) {
                        continue;
                     }
                  }

                  var10 = this.worldToNoiseX(this.worldX + var9);
                  int var23 = this.worldToNoiseY(this.worldY + var8);
                  if (var6.layeredNoise((float)var10 / 10.0F, (float)var23 / 10.0F) <= (float)var4 / 100.0F) {
                     this.grid[var9][var8][0] = FBORenderSnow.instance.snowGridTiles_Square.getAt(this.worldX + var9, this.worldY + var8);
                     this.gridType[var9][var8][0] = FBORenderSnow.instance.snowGridTiles_Square.ID;
                  }
               }
            }

            GameProfiler.getInstance().end(var7);
            GameProfiler.ProfileArea var20 = GameProfiler.getInstance().startIfEnabled("Check Set");

            for(int var26 = 0; var26 < this.h; ++var26) {
               for(int var15 = 0; var15 < this.w; ++var15) {
                  Texture var16 = this.grid[var15][var26][0];
                  if (var16 == null) {
                     boolean var22 = this.check(var15, var26 - 1);
                     var11 = this.check(var15, var26 + 1);
                     boolean var24 = this.check(var15 - 1, var26);
                     boolean var25 = this.check(var15 + 1, var26);
                     var9 = 0;
                     if (var22) {
                        ++var9;
                     }

                     if (var11) {
                        ++var9;
                     }

                     if (var25) {
                        ++var9;
                     }

                     if (var24) {
                        ++var9;
                     }

                     if (var9 != 0) {
                        if (var9 == 1) {
                           if (var22) {
                              this.set(var15, var26, 0, FBORenderSnow.instance.snowGridTiles_Strip[0]);
                           } else if (var11) {
                              this.set(var15, var26, 0, FBORenderSnow.instance.snowGridTiles_Strip[1]);
                           } else if (var25) {
                              this.set(var15, var26, 0, FBORenderSnow.instance.snowGridTiles_Strip[3]);
                           } else if (var24) {
                              this.set(var15, var26, 0, FBORenderSnow.instance.snowGridTiles_Strip[2]);
                           }
                        } else if (var9 == 2) {
                           if (var22 && var11) {
                              this.set(var15, var26, 0, FBORenderSnow.instance.snowGridTiles_Strip[0]);
                              this.set(var15, var26, 1, FBORenderSnow.instance.snowGridTiles_Strip[1]);
                           } else if (var25 && var24) {
                              this.set(var15, var26, 0, FBORenderSnow.instance.snowGridTiles_Strip[2]);
                              this.set(var15, var26, 1, FBORenderSnow.instance.snowGridTiles_Strip[3]);
                           } else if (var22) {
                              this.set(var15, var26, 0, FBORenderSnow.instance.snowGridTiles_Edge[var24 ? 0 : 3]);
                           } else if (var11) {
                              this.set(var15, var26, 0, FBORenderSnow.instance.snowGridTiles_Edge[var24 ? 2 : 1]);
                           } else if (var24) {
                              this.set(var15, var26, 0, FBORenderSnow.instance.snowGridTiles_Edge[var22 ? 0 : 2]);
                           } else if (var25) {
                              this.set(var15, var26, 0, FBORenderSnow.instance.snowGridTiles_Edge[var22 ? 3 : 1]);
                           }
                        } else if (var9 == 3) {
                           if (!var22) {
                              this.set(var15, var26, 0, FBORenderSnow.instance.snowGridTiles_Cove[1]);
                           } else if (!var11) {
                              this.set(var15, var26, 0, FBORenderSnow.instance.snowGridTiles_Cove[0]);
                           } else if (!var25) {
                              this.set(var15, var26, 0, FBORenderSnow.instance.snowGridTiles_Cove[2]);
                           } else if (!var24) {
                              this.set(var15, var26, 0, FBORenderSnow.instance.snowGridTiles_Cove[3]);
                           }
                        } else if (var9 == 4) {
                           this.set(var15, var26, 0, FBORenderSnow.instance.snowGridTiles_Enclosed);
                        }
                     }
                  }
               }
            }

            GameProfiler.getInstance().end(var20);
            return this;
         }
      }

      int worldToSelfX(int var1) {
         return var1 - this.worldX;
      }

      int worldToSelfY(int var1) {
         return var1 - this.worldY;
      }

      int worldToNoiseX(int var1) {
         return PZMath.coordmodulo(var1, 256);
      }

      int worldToNoiseY(int var1) {
         return PZMath.coordmodulo(var1, 256);
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
               return FBORenderSnow.instance.snowGridTiles_Square.contains(var3);
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
               this.grid[var1][var2][var3] = var4.getAt(this.worldX + var1, this.worldY + var2);
               this.gridType[var1][var2][var3] = var4.ID;
            }
         }
      }
   }

   public static final class ChunkLevel {
      public final IsoChunk m_chunk;
      private SnowGrid m_snowGrid = null;
      public int m_adjacentChunkLoadedCounter = -1;

      public ChunkLevel(IsoChunk var1) {
         this.m_chunk = var1;
      }
   }
}
