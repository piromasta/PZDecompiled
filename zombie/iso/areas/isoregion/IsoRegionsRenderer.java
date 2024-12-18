package zombie.iso.areas.isoregion;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.MapCollisionData;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.core.utils.Bits;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.LotHeader;
import zombie.iso.RoomDef;
import zombie.iso.areas.isoregion.data.DataChunk;
import zombie.iso.areas.isoregion.data.DataRoot;
import zombie.iso.areas.isoregion.regions.IsoChunkRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;
import zombie.iso.objects.IsoThumpable;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIFont;

public class IsoRegionsRenderer {
   private final List<DataChunk> tempChunkList = new ArrayList();
   private final List<String> debugLines = new ArrayList();
   private float xPos;
   private float yPos;
   private float offx;
   private float offy;
   private float zoom;
   private float draww;
   private float drawh;
   private boolean hasSelected = false;
   private boolean validSelection = false;
   private int selectedX;
   private int selectedY;
   private int selectedZ;
   private final HashSet<Integer> drawnCells = new HashSet();
   private boolean editSquareInRange = false;
   private int editSquareX;
   private int editSquareY;
   private final ArrayList<ConfigOption> editOptions = new ArrayList();
   private boolean EditingEnabled = false;
   private final BooleanDebugOption EditWallN;
   private final BooleanDebugOption EditWallW;
   private final BooleanDebugOption EditDoorN;
   private final BooleanDebugOption EditDoorW;
   private final BooleanDebugOption EditFloor;
   private final ArrayList<ConfigOption> zLevelOptions;
   private final BooleanDebugOption zLevelPlayer;
   private final BooleanDebugOption zLevel0;
   private final BooleanDebugOption zLevel1;
   private final BooleanDebugOption zLevel2;
   private final BooleanDebugOption zLevel3;
   private final BooleanDebugOption zLevel4;
   private final BooleanDebugOption zLevel5;
   private final BooleanDebugOption zLevel6;
   private final BooleanDebugOption zLevel7;
   private static final int VERSION = 1;
   private final ArrayList<ConfigOption> options;
   private final BooleanDebugOption CellGrid;
   private final BooleanDebugOption MetaGridBuildings;
   private final BooleanDebugOption IsoRegionRender;
   private final BooleanDebugOption IsoRegionRenderChunks;
   private final BooleanDebugOption IsoRegionRenderChunksPlus;

   public IsoRegionsRenderer() {
      this.EditWallN = new BooleanDebugOption(this.editOptions, "Edit.WallN", false);
      this.EditWallW = new BooleanDebugOption(this.editOptions, "Edit.WallW", false);
      this.EditDoorN = new BooleanDebugOption(this.editOptions, "Edit.DoorN", false);
      this.EditDoorW = new BooleanDebugOption(this.editOptions, "Edit.DoorW", false);
      this.EditFloor = new BooleanDebugOption(this.editOptions, "Edit.Floor", false);
      this.zLevelOptions = new ArrayList();
      this.zLevelPlayer = new BooleanDebugOption(this.zLevelOptions, "zLevel.Player", true);
      this.zLevel0 = new BooleanDebugOption(this.zLevelOptions, "zLevel.0", false, 0);
      this.zLevel1 = new BooleanDebugOption(this.zLevelOptions, "zLevel.1", false, 1);
      this.zLevel2 = new BooleanDebugOption(this.zLevelOptions, "zLevel.2", false, 2);
      this.zLevel3 = new BooleanDebugOption(this.zLevelOptions, "zLevel.3", false, 3);
      this.zLevel4 = new BooleanDebugOption(this.zLevelOptions, "zLevel.4", false, 4);
      this.zLevel5 = new BooleanDebugOption(this.zLevelOptions, "zLevel.5", false, 5);
      this.zLevel6 = new BooleanDebugOption(this.zLevelOptions, "zLevel.6", false, 6);
      this.zLevel7 = new BooleanDebugOption(this.zLevelOptions, "zLevel.7", false, 7);
      this.options = new ArrayList();
      this.CellGrid = new BooleanDebugOption(this.options, "CellGrid", true);
      this.MetaGridBuildings = new BooleanDebugOption(this.options, "MetaGrid.Buildings", true);
      this.IsoRegionRender = new BooleanDebugOption(this.options, "IsoRegion.Render", true);
      this.IsoRegionRenderChunks = new BooleanDebugOption(this.options, "IsoRegion.RenderChunks", false);
      this.IsoRegionRenderChunksPlus = new BooleanDebugOption(this.options, "IsoRegion.RenderChunksPlus", false);
   }

   public float worldToScreenX(float var1) {
      var1 -= this.xPos;
      var1 *= this.zoom;
      var1 += this.offx;
      var1 += this.draww / 2.0F;
      return var1;
   }

   public float worldToScreenY(float var1) {
      var1 -= this.yPos;
      var1 *= this.zoom;
      var1 += this.offy;
      var1 += this.drawh / 2.0F;
      return var1;
   }

   public float uiToWorldX(float var1) {
      var1 -= this.draww / 2.0F;
      var1 /= this.zoom;
      var1 += this.xPos;
      return var1;
   }

   public float uiToWorldY(float var1) {
      var1 -= this.drawh / 2.0F;
      var1 /= this.zoom;
      var1 += this.yPos;
      return var1;
   }

   public void renderStringUI(float var1, float var2, String var3, Color var4) {
      this.renderStringUI(var1, var2, var3, (double)var4.r, (double)var4.g, (double)var4.b, (double)var4.a);
   }

   public void renderStringUI(float var1, float var2, String var3, double var4, double var6, double var8, double var10) {
      float var12 = this.offx + var1;
      float var13 = this.offy + var2;
      SpriteRenderer.instance.render((Texture)null, var12 - 2.0F, var13 - 2.0F, (float)(TextManager.instance.MeasureStringX(UIFont.Small, var3) + 4), (float)(TextManager.instance.font.getLineHeight() + 4), 0.0F, 0.0F, 0.0F, 0.75F, (Consumer)null);
      TextManager.instance.DrawString((double)var12, (double)var13, var3, var4, var6, var8, var10);
   }

   public void renderString(float var1, float var2, String var3, double var4, double var6, double var8, double var10) {
      float var12 = this.worldToScreenX(var1);
      float var13 = this.worldToScreenY(var2);
      SpriteRenderer.instance.render((Texture)null, var12 - 2.0F, var13 - 2.0F, (float)(TextManager.instance.MeasureStringX(UIFont.Small, var3) + 4), (float)(TextManager.instance.font.getLineHeight() + 4), 0.0F, 0.0F, 0.0F, 0.75F, (Consumer)null);
      TextManager.instance.DrawString((double)var12, (double)var13, var3, var4, var6, var8, var10);
   }

   public void renderRect(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      float var9 = this.worldToScreenX(var1);
      float var10 = this.worldToScreenY(var2);
      float var11 = this.worldToScreenX(var1 + var3);
      float var12 = this.worldToScreenY(var2 + var4);
      var3 = var11 - var9;
      var4 = var12 - var10;
      if (!(var9 >= this.offx + this.draww) && !(var11 < this.offx) && !(var10 >= this.offy + this.drawh) && !(var12 < this.offy)) {
         SpriteRenderer.instance.render((Texture)null, var9, var10, var3, var4, var5, var6, var7, var8, (Consumer)null);
      }
   }

   public void renderLine(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      float var9 = this.worldToScreenX(var1);
      float var10 = this.worldToScreenY(var2);
      float var11 = this.worldToScreenX(var3);
      float var12 = this.worldToScreenY(var4);
      if ((!(var9 >= (float)Core.getInstance().getScreenWidth()) || !(var11 >= (float)Core.getInstance().getScreenWidth())) && (!(var10 >= (float)Core.getInstance().getScreenHeight()) || !(var12 >= (float)Core.getInstance().getScreenHeight())) && (!(var9 < 0.0F) || !(var11 < 0.0F)) && (!(var10 < 0.0F) || !(var12 < 0.0F))) {
         SpriteRenderer.instance.renderline((Texture)null, (int)var9, (int)var10, (int)var11, (int)var12, var5, var6, var7, var8);
      }
   }

   public void outlineRect(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      this.renderLine(var1, var2, var1 + var3, var2, var5, var6, var7, var8);
      this.renderLine(var1 + var3, var2, var1 + var3, var2 + var4, var5, var6, var7, var8);
      this.renderLine(var1, var2 + var4, var1 + var3, var2 + var4, var5, var6, var7, var8);
      this.renderLine(var1, var2, var1, var2 + var4, var5, var6, var7, var8);
   }

   public void renderCellInfo(int var1, int var2, int var3, int var4, float var5) {
      float var6 = this.worldToScreenX((float)(var1 * IsoRegions.CELL_DIM)) + 4.0F;
      float var7 = this.worldToScreenY((float)(var2 * IsoRegions.CELL_DIM)) + 4.0F;
      String var8 = "" + var3 + " / " + var4;
      if (var5 > 0.0F) {
         var8 = var8 + String.format(" %.2f", var5);
      }

      SpriteRenderer.instance.render((Texture)null, var6 - 2.0F, var7 - 2.0F, (float)(TextManager.instance.MeasureStringX(UIFont.Small, var8) + 4), (float)(TextManager.instance.font.getLineHeight() + 4), 0.0F, 0.0F, 0.0F, 0.75F, (Consumer)null);
      TextManager.instance.DrawString((double)var6, (double)var7, var8, 1.0, 1.0, 1.0, 1.0);
   }

   public void renderZombie(float var1, float var2, float var3, float var4, float var5) {
      float var6 = 1.0F / this.zoom + 0.5F;
      this.renderRect(var1 - var6 / 2.0F, var2 - var6 / 2.0F, var6, var6, var3, var4, var5, 1.0F);
   }

   public void renderSquare(float var1, float var2, float var3, float var4, float var5, float var6) {
      float var7 = 1.0F;
      this.renderRect(var1, var2, var7, var7, var3, var4, var5, var6);
   }

   public void renderEntity(float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      float var8 = var1 / this.zoom + 0.5F;
      this.renderRect(var2 - var8 / 2.0F, var3 - var8 / 2.0F, var8, var8, var4, var5, var6, var7);
   }

   public void render(UIElement var1, float var2, float var3, float var4) {
      synchronized(MapCollisionData.instance.renderLock) {
         this._render(var1, var2, var3, var4);
      }
   }

   private void debugLine(String var1) {
      this.debugLines.add(var1);
   }

   public void recalcSurroundings() {
      IsoRegions.forceRecalcSurroundingChunks();
   }

   public boolean hasChunkRegion(int var1, int var2) {
      int var3 = this.getZLevel();
      DataRoot var4 = IsoRegions.getDataRoot();
      return var4.getIsoChunkRegion(var1, var2, var3) != null;
   }

   public IsoChunkRegion getChunkRegion(int var1, int var2) {
      int var3 = this.getZLevel();
      DataRoot var4 = IsoRegions.getDataRoot();
      return var4.getIsoChunkRegion(var1, var2, var3);
   }

   public void setSelected(int var1, int var2) {
      this.setSelectedWorld((int)this.uiToWorldX((float)var1), (int)this.uiToWorldY((float)var2));
   }

   public void setSelectedWorld(int var1, int var2) {
      this.selectedZ = this.getZLevel();
      this.hasSelected = true;
      this.selectedX = var1;
      this.selectedY = var2;
   }

   public void unsetSelected() {
      this.hasSelected = false;
   }

   public boolean isHasSelected() {
      return this.hasSelected;
   }

   private void _render(UIElement var1, float var2, float var3, float var4) {
      this.debugLines.clear();
      this.drawnCells.clear();
      this.draww = (float)var1.getWidth().intValue();
      this.drawh = (float)var1.getHeight().intValue();
      this.xPos = var3;
      this.yPos = var4;
      this.offx = (float)var1.getAbsoluteX().intValue();
      this.offy = (float)var1.getAbsoluteY().intValue();
      this.zoom = var2;
      this.debugLine("Zoom: " + var2);
      this.debugLine("zLevel: " + this.getZLevel());
      IsoMetaGrid var5 = IsoWorld.instance.MetaGrid;
      int var6 = (int)(this.uiToWorldX(0.0F) / (float)IsoCell.CellSizeInSquares) - var5.minX;
      int var7 = (int)(this.uiToWorldY(0.0F) / (float)IsoCell.CellSizeInSquares) - var5.minY;
      int var8 = (int)(this.uiToWorldX(this.draww) / (float)IsoCell.CellSizeInSquares) + 1 - var5.minX;
      int var9 = (int)(this.uiToWorldY(this.drawh) / (float)IsoCell.CellSizeInSquares) + 1 - var5.minY;
      var6 = PZMath.clamp(var6, 0, var5.getWidth() - 1);
      var7 = PZMath.clamp(var7, 0, var5.getHeight() - 1);
      var8 = PZMath.clamp(var8, 0, var5.getWidth() - 1);
      var9 = PZMath.clamp(var9, 0, var5.getHeight() - 1);
      float var10 = Math.max(1.0F - var2 / 2.0F, 0.1F);
      IsoChunkRegion var11 = null;
      IsoWorldRegion var12 = null;
      this.validSelection = false;
      DataRoot var14;
      DataChunk var15;
      int var16;
      int var17;
      float var21;
      if (this.IsoRegionRender.getValue()) {
         IsoPlayer var13 = IsoPlayer.getInstance();
         var14 = IsoRegions.getDataRoot();
         this.tempChunkList.clear();
         var14.getAllChunks(this.tempChunkList);
         this.debugLine("DataChunks: " + this.tempChunkList.size());
         this.debugLine("IsoChunkRegions: " + var14.regionManager.getChunkRegionCount());
         this.debugLine("IsoWorldRegios: " + var14.regionManager.getWorldRegionCount());
         if (this.hasSelected) {
            var11 = var14.getIsoChunkRegion(this.selectedX, this.selectedY, this.selectedZ);
            var12 = var14.getIsoWorldRegion(this.selectedX, this.selectedY, this.selectedZ);
            if (var12 != null && !var12.isEnclosed() && (!this.IsoRegionRenderChunks.getValue() || !this.IsoRegionRenderChunksPlus.getValue())) {
               var12 = null;
               var11 = null;
            }

            if (var11 != null) {
               this.validSelection = true;
            }
         }

         for(int var22 = 0; var22 < this.tempChunkList.size(); ++var22) {
            var15 = (DataChunk)this.tempChunkList.get(var22);
            var16 = var15.getChunkX() * 8;
            var17 = var15.getChunkY() * 8;
            if (var2 > 0.1F) {
               float var18 = this.worldToScreenX((float)var16);
               float var20 = this.worldToScreenY((float)var17);
               float var19 = this.worldToScreenX((float)(var16 + 8));
               var21 = this.worldToScreenY((float)(var17 + 8));
               if (!(var18 >= this.offx + this.draww) && !(var19 < this.offx) && !(var20 >= this.offy + this.drawh) && !(var21 < this.offy)) {
                  this.renderRect((float)var16, (float)var17, 8.0F, 8.0F, 0.0F, var10, 0.0F, 1.0F);
               }
            }
         }
      }

      float var34;
      int var36;
      int var37;
      if (this.MetaGridBuildings.getValue()) {
         var34 = PZMath.clamp(0.3F * (var2 / 5.0F), 0.15F, 0.3F);

         for(var36 = var6; var36 < var8; ++var36) {
            for(var37 = var7; var37 < var9; ++var37) {
               if (var5.hasCell(var36, var37)) {
                  LotHeader var38 = var5.getCell(var36, var37).info;
                  if (var38 != null) {
                     for(var17 = 0; var17 < var38.Buildings.size(); ++var17) {
                        BuildingDef var41 = (BuildingDef)var38.Buildings.get(var17);

                        for(int var43 = 0; var43 < var41.rooms.size(); ++var43) {
                           if (((RoomDef)var41.rooms.get(var43)).level <= 0) {
                              ArrayList var45 = ((RoomDef)var41.rooms.get(var43)).getRects();

                              for(int var47 = 0; var47 < var45.size(); ++var47) {
                                 RoomDef.RoomRect var48 = (RoomDef.RoomRect)var45.get(var47);
                                 if (var41.bAlarmed) {
                                    this.renderRect((float)var48.getX(), (float)var48.getY(), (float)var48.getW(), (float)var48.getH(), 0.8F * var34, 0.8F * var34, 0.5F * var34, 1.0F);
                                 } else {
                                    this.renderRect((float)var48.getX(), (float)var48.getY(), (float)var48.getW(), (float)var48.getH(), 0.5F * var34, 0.5F * var34, 0.8F * var34, 1.0F);
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

      int var35;
      if (this.IsoRegionRender.getValue()) {
         var35 = this.getZLevel();
         var14 = IsoRegions.getDataRoot();
         this.tempChunkList.clear();
         var14.getAllChunks(this.tempChunkList);
         float var26 = 1.0F;

         for(int var27 = 0; var27 < this.tempChunkList.size(); ++var27) {
            var15 = (DataChunk)this.tempChunkList.get(var27);
            var16 = var15.getChunkX() * 8;
            var17 = var15.getChunkY() * 8;
            int var28;
            int var29;
            int var30;
            if (var2 <= 0.1F) {
               var28 = var16 / IsoRegions.CELL_DIM;
               var29 = var17 / IsoRegions.CELL_DIM;
               var30 = IsoRegions.hash(var28, var29);
               if (!this.drawnCells.contains(var30)) {
                  this.drawnCells.add(var30);
                  this.renderRect((float)(var28 * IsoRegions.CELL_DIM), (float)(var29 * IsoRegions.CELL_DIM), (float)IsoRegions.CELL_DIM, (float)IsoRegions.CELL_DIM, 0.0F, var10, 0.0F, 1.0F);
               }
            } else if (!(var2 < 1.0F)) {
               var21 = this.worldToScreenX((float)var16);
               float var23 = this.worldToScreenY((float)var17);
               float var49 = this.worldToScreenX((float)(var16 + 8));
               float var24 = this.worldToScreenY((float)(var17 + 8));
               if (!(var21 >= this.offx + this.draww) && !(var49 < this.offx) && !(var23 >= this.offy + this.drawh) && !(var24 < this.offy)) {
                  for(var28 = 0; var28 < 8; ++var28) {
                     for(var29 = 0; var29 < 8; ++var29) {
                        var30 = var35 > 0 ? var35 - 1 : var35;

                        for(int var31 = var30; var31 <= var35; ++var31) {
                           float var32 = var31 < var35 ? 0.25F : 1.0F;
                           byte var25 = var15.getSquare(var28, var29, var31);
                           if (var25 >= 0) {
                              IsoChunkRegion var42 = var15.getIsoChunkRegion(var28, var29, var31);
                              IsoWorldRegion var44;
                              if (var42 != null) {
                                 Color var46;
                                 if (var2 > 6.0F && this.IsoRegionRenderChunks.getValue() && this.IsoRegionRenderChunksPlus.getValue()) {
                                    var46 = var42.getColor();
                                    var26 = 1.0F;
                                    if (var11 != null && var42 != var11) {
                                       var26 = 0.25F;
                                    }

                                    this.renderSquare((float)(var16 + var28), (float)(var17 + var29), var46.r, var46.g, var46.b, var26 * var32);
                                 } else {
                                    var44 = var42.getIsoWorldRegion();
                                    if (var44 != null && var44.isEnclosed()) {
                                       var26 = 1.0F;
                                       if (this.IsoRegionRenderChunks.getValue()) {
                                          var46 = var42.getColor();
                                          if (var11 != null && var42 != var11) {
                                             var26 = 0.25F;
                                          }
                                       } else {
                                          var46 = var44.getColor();
                                          if (var12 != null && var44 != var12) {
                                             var26 = 0.25F;
                                          }
                                       }

                                       this.renderSquare((float)(var16 + var28), (float)(var17 + var29), var46.r, var46.g, var46.b, var26 * var32);
                                    }
                                 }
                              }

                              if (var31 > 0 && var31 == var35) {
                                 var42 = var15.getIsoChunkRegion(var28, var29, var31);
                                 var44 = var42 != null ? var42.getIsoWorldRegion() : null;
                                 boolean var33 = var42 == null || var44 == null || !var44.isEnclosed();
                                 if (var33 && Bits.hasFlags((byte)var25, 16)) {
                                    this.renderSquare((float)(var16 + var28), (float)(var17 + var29), 0.5F, 0.5F, 0.5F, 1.0F);
                                 }
                              }

                              if (Bits.hasFlags((byte)var25, 1) || Bits.hasFlags((byte)var25, 4)) {
                                 this.renderRect((float)(var16 + var28), (float)(var17 + var29), 1.0F, 0.1F, 1.0F, 1.0F, 1.0F, 1.0F * var32);
                              }

                              if (Bits.hasFlags((byte)var25, 2) || Bits.hasFlags((byte)var25, 8)) {
                                 this.renderRect((float)(var16 + var28), (float)(var17 + var29), 0.1F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F * var32);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (this.CellGrid.getValue()) {
         var34 = 1.0F;
         if (var2 < 0.1F) {
            var34 = Math.max(var2 / 0.1F, 0.25F);
         }

         for(var36 = var7; var36 <= var9; ++var36) {
            this.renderLine((float)(var5.minX * IsoCell.CellSizeInSquares), (float)((var5.minY + var36) * IsoCell.CellSizeInSquares), (float)((var5.maxX + 1) * IsoCell.CellSizeInSquares), (float)((var5.minY + var36) * IsoCell.CellSizeInSquares), 1.0F, 1.0F, 1.0F, 0.15F * var34);
            if (var2 > 1.0F) {
               for(var37 = 1; var37 < IsoCell.CellSizeInChunks; ++var37) {
                  this.renderLine((float)(var5.minX * IsoCell.CellSizeInSquares), (float)((var5.minY + var36) * IsoCell.CellSizeInSquares + var37 * 8), (float)((var5.maxX + 1) * IsoCell.CellSizeInSquares), (float)((var5.minY + var36) * IsoCell.CellSizeInSquares + var37 * 8), 1.0F, 1.0F, 1.0F, 0.0325F);
               }
            } else if (var2 > 0.15F) {
               this.renderLine((float)(var5.minX * IsoCell.CellSizeInSquares), (float)((var5.minY + var36) * IsoCell.CellSizeInSquares + 100), (float)((var5.maxX + 1) * IsoCell.CellSizeInSquares), (float)((var5.minY + var36) * IsoCell.CellSizeInSquares + 100), 1.0F, 1.0F, 1.0F, 0.075F);
               this.renderLine((float)(var5.minX * IsoCell.CellSizeInSquares), (float)((var5.minY + var36) * IsoCell.CellSizeInSquares + 200), (float)((var5.maxX + 1) * IsoCell.CellSizeInSquares), (float)((var5.minY + var36) * IsoCell.CellSizeInSquares + 200), 1.0F, 1.0F, 1.0F, 0.075F);
            }
         }

         for(var36 = var6; var36 <= var8; ++var36) {
            this.renderLine((float)((var5.minX + var36) * IsoCell.CellSizeInSquares), (float)(var5.minY * IsoCell.CellSizeInSquares), (float)((var5.minX + var36) * IsoCell.CellSizeInSquares), (float)((var5.maxY + 1) * IsoCell.CellSizeInSquares), 1.0F, 1.0F, 1.0F, 0.15F * var34);
            if (var2 > 1.0F) {
               for(var37 = 1; var37 < IsoCell.CellSizeInChunks; ++var37) {
                  this.renderLine((float)((var5.minX + var36) * IsoCell.CellSizeInSquares + var37 * 8), (float)(var5.minY * IsoCell.CellSizeInSquares), (float)((var5.minX + var36) * IsoCell.CellSizeInSquares + var37 * 8), (float)((var5.maxY + 1) * IsoCell.CellSizeInSquares), 1.0F, 1.0F, 1.0F, 0.0325F);
               }
            } else if (var2 > 0.15F) {
               this.renderLine((float)((var5.minX + var36) * IsoCell.CellSizeInSquares + 100), (float)(var5.minY * IsoCell.CellSizeInSquares), (float)((var5.minX + var36) * IsoCell.CellSizeInSquares + 100), (float)((var5.maxY + 1) * IsoCell.CellSizeInSquares), 1.0F, 1.0F, 1.0F, 0.075F);
               this.renderLine((float)((var5.minX + var36) * IsoCell.CellSizeInSquares + 200), (float)(var5.minY * IsoCell.CellSizeInSquares), (float)((var5.minX + var36) * IsoCell.CellSizeInSquares + 200), (float)((var5.maxY + 1) * IsoCell.CellSizeInSquares), 1.0F, 1.0F, 1.0F, 0.075F);
            }
         }
      }

      for(var35 = 0; var35 < IsoPlayer.numPlayers; ++var35) {
         IsoPlayer var39 = IsoPlayer.players[var35];
         if (var39 != null) {
            this.renderZombie(var39.getX(), var39.getY(), 0.0F, 0.5F, 0.0F);
         }
      }

      if (this.isEditingEnabled()) {
         var34 = this.editSquareInRange ? 0.0F : 1.0F;
         float var40 = this.editSquareInRange ? 1.0F : 0.0F;
         if (!this.EditWallN.getValue() && !this.EditDoorN.getValue()) {
            if (!this.EditWallW.getValue() && !this.EditDoorW.getValue()) {
               this.renderRect((float)this.editSquareX, (float)this.editSquareY, 1.0F, 1.0F, var34, var40, 0.0F, 0.5F);
               this.renderRect((float)this.editSquareX, (float)this.editSquareY, 1.0F, 0.05F, var34, var40, 0.0F, 1.0F);
               this.renderRect((float)this.editSquareX, (float)this.editSquareY, 0.05F, 1.0F, var34, var40, 0.0F, 1.0F);
               this.renderRect((float)this.editSquareX, (float)this.editSquareY + 0.95F, 1.0F, 0.05F, var34, var40, 0.0F, 1.0F);
               this.renderRect((float)this.editSquareX + 0.95F, (float)this.editSquareY, 0.05F, 1.0F, var34, var40, 0.0F, 1.0F);
            } else {
               this.renderRect((float)this.editSquareX, (float)this.editSquareY, 0.25F, 1.0F, var34, var40, 0.0F, 0.5F);
               this.renderRect((float)this.editSquareX, (float)this.editSquareY, 0.25F, 0.05F, var34, var40, 0.0F, 1.0F);
               this.renderRect((float)this.editSquareX, (float)this.editSquareY, 0.05F, 1.0F, var34, var40, 0.0F, 1.0F);
               this.renderRect((float)this.editSquareX, (float)this.editSquareY + 0.95F, 0.25F, 0.05F, var34, var40, 0.0F, 1.0F);
               this.renderRect((float)this.editSquareX + 0.2F, (float)this.editSquareY, 0.05F, 1.0F, var34, var40, 0.0F, 1.0F);
            }
         } else {
            this.renderRect((float)this.editSquareX, (float)this.editSquareY, 1.0F, 0.25F, var34, var40, 0.0F, 0.5F);
            this.renderRect((float)this.editSquareX, (float)this.editSquareY, 1.0F, 0.05F, var34, var40, 0.0F, 1.0F);
            this.renderRect((float)this.editSquareX, (float)this.editSquareY, 0.05F, 0.25F, var34, var40, 0.0F, 1.0F);
            this.renderRect((float)this.editSquareX, (float)this.editSquareY + 0.2F, 1.0F, 0.05F, var34, var40, 0.0F, 1.0F);
            this.renderRect((float)this.editSquareX + 0.95F, (float)this.editSquareY, 0.05F, 0.25F, var34, var40, 0.0F, 1.0F);
         }
      }

      if (var11 != null) {
         this.debugLine("- ChunkRegion -");
         this.debugLine("ID: " + var11.getID());
         this.debugLine("Squares: " + var11.getSquareSize());
         this.debugLine("Roofs: " + var11.getRoofCnt());
         this.debugLine("Neighbors: " + var11.getNeighborCount());
         this.debugLine("ConnectedNeighbors: " + var11.getConnectedNeighbors().size());
         this.debugLine("FullyEnclosed: " + var11.getIsEnclosed());
      }

      if (var12 != null) {
         this.debugLine("- WorldRegion -");
         this.debugLine("ID: " + var12.getID());
         this.debugLine("Squares: " + var12.getSquareSize());
         this.debugLine("Roofs: " + var12.getRoofCnt());
         this.debugLine("IsFullyRoofed: " + var12.isFullyRoofed());
         this.debugLine("RoofPercentage: " + var12.getRoofedPercentage());
         this.debugLine("IsEnclosed: " + var12.isEnclosed());
         this.debugLine("Neighbors: " + var12.getNeighbors().size());
         this.debugLine("ChunkRegionCount: " + var12.size());
      }

      var35 = 15;

      for(var36 = 0; var36 < this.debugLines.size(); ++var36) {
         this.renderStringUI(10.0F, (float)var35, (String)this.debugLines.get(var36), Colors.CornFlowerBlue);
         var35 += TextManager.instance.getFontHeight(UIFont.Small);
      }

   }

   public void setEditSquareCoord(int var1, int var2) {
      this.editSquareX = var1;
      this.editSquareY = var2;
      this.editSquareInRange = false;
      if (this.editCoordInRange(var1, var2)) {
         this.editSquareInRange = true;
      }

   }

   private boolean editCoordInRange(int var1, int var2) {
      IsoGridSquare var3 = IsoWorld.instance.getCell().getGridSquare(var1, var2, 0);
      return var3 != null;
   }

   public void editSquare(int var1, int var2) {
      if (this.isEditingEnabled()) {
         int var3 = this.getZLevel();
         IsoGridSquare var4 = IsoWorld.instance.getCell().getGridSquare(var1, var2, var3);
         DataRoot var5 = IsoRegions.getDataRoot();
         byte var6 = var5.getSquareFlags(var1, var2, var3);
         if (this.editCoordInRange(var1, var2)) {
            if (var4 == null) {
               var4 = IsoWorld.instance.getCell().createNewGridSquare(var1, var2, var3, true);
               if (var4 == null) {
                  return;
               }
            }

            this.editSquareInRange = true;

            for(int var7 = 0; var7 < this.editOptions.size(); ++var7) {
               BooleanDebugOption var8 = (BooleanDebugOption)this.editOptions.get(var7);
               if (var8.getValue()) {
                  IsoThumpable var11;
                  switch (var8.getName()) {
                     case "Edit.WallW":
                     case "Edit.WallN":
                        if (var8.getName().equals("Edit.WallN")) {
                           if (var6 > 0 && Bits.hasFlags((byte)var6, 1)) {
                              return;
                           }

                           var11 = new IsoThumpable(IsoWorld.instance.getCell(), var4, "walls_exterior_wooden_01_25", true, (KahluaTable)null);
                        } else {
                           if (var6 > 0 && Bits.hasFlags((byte)var6, 2)) {
                              return;
                           }

                           var11 = new IsoThumpable(IsoWorld.instance.getCell(), var4, "walls_exterior_wooden_01_24", true, (KahluaTable)null);
                        }

                        var11.setMaxHealth(100);
                        var11.setName("Wall Debug");
                        var11.setBreakSound("BreakObject");
                        var4.AddSpecialObject(var11);
                        var4.RecalcAllWithNeighbours(true);
                        var11.transmitCompleteItemToServer();
                        if (var4.getZone() != null) {
                           var4.getZone().setHaveConstruction(true);
                        }
                        break;
                     case "Edit.DoorW":
                     case "Edit.DoorN":
                        if (var8.getName().equals("Edit.DoorN")) {
                           if (var6 > 0 && Bits.hasFlags((byte)var6, 1)) {
                              return;
                           }

                           var11 = new IsoThumpable(IsoWorld.instance.getCell(), var4, "walls_exterior_wooden_01_35", true, (KahluaTable)null);
                        } else {
                           if (var6 > 0 && Bits.hasFlags((byte)var6, 2)) {
                              return;
                           }

                           var11 = new IsoThumpable(IsoWorld.instance.getCell(), var4, "walls_exterior_wooden_01_34", true, (KahluaTable)null);
                        }

                        var11.setMaxHealth(100);
                        var11.setName("Door Frame Debug");
                        var11.setBreakSound("BreakObject");
                        var4.AddSpecialObject(var11);
                        var4.RecalcAllWithNeighbours(true);
                        var11.transmitCompleteItemToServer();
                        if (var4.getZone() != null) {
                           var4.getZone().setHaveConstruction(true);
                        }
                        break;
                     case "Edit.Floor":
                        if (var6 > 0 && Bits.hasFlags((byte)var6, 16)) {
                           return;
                        }

                        if (var3 == 0) {
                           return;
                        }

                        var4.addFloor("carpentry_02_56");
                        if (var4.getZone() != null) {
                           var4.getZone().setHaveConstruction(true);
                        }
                  }
               }
            }
         } else {
            this.editSquareInRange = false;
         }
      }

   }

   public boolean isEditingEnabled() {
      return this.EditingEnabled;
   }

   public void editRotate() {
      if (this.EditWallN.getValue()) {
         this.EditWallN.setValue(false);
         this.EditWallW.setValue(true);
      } else if (this.EditWallW.getValue()) {
         this.EditWallW.setValue(false);
         this.EditWallN.setValue(true);
      }

      if (this.EditDoorN.getValue()) {
         this.EditDoorN.setValue(false);
         this.EditDoorW.setValue(true);
      } else if (this.EditDoorW.getValue()) {
         this.EditDoorW.setValue(false);
         this.EditDoorN.setValue(true);
      }

   }

   public ConfigOption getEditOptionByName(String var1) {
      for(int var2 = 0; var2 < this.editOptions.size(); ++var2) {
         ConfigOption var3 = (ConfigOption)this.editOptions.get(var2);
         if (var3.getName().equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public int getEditOptionCount() {
      return this.editOptions.size();
   }

   public ConfigOption getEditOptionByIndex(int var1) {
      return (ConfigOption)this.editOptions.get(var1);
   }

   public void setEditOption(int var1, boolean var2) {
      for(int var3 = 0; var3 < this.editOptions.size(); ++var3) {
         BooleanDebugOption var4 = (BooleanDebugOption)this.editOptions.get(var3);
         if (var3 != var1) {
            var4.setValue(false);
         } else {
            var4.setValue(var2);
            this.EditingEnabled = var2;
         }
      }

   }

   public int getZLevel() {
      if (this.zLevelPlayer.getValue()) {
         return PZMath.fastfloor(IsoPlayer.getInstance().getZ());
      } else {
         for(int var1 = 0; var1 < this.zLevelOptions.size(); ++var1) {
            BooleanDebugOption var2 = (BooleanDebugOption)this.zLevelOptions.get(var1);
            if (var2.getValue()) {
               return var2.zLevel;
            }
         }

         return 0;
      }
   }

   public ConfigOption getZLevelOptionByName(String var1) {
      for(int var2 = 0; var2 < this.zLevelOptions.size(); ++var2) {
         ConfigOption var3 = (ConfigOption)this.zLevelOptions.get(var2);
         if (var3.getName().equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public int getZLevelOptionCount() {
      return this.zLevelOptions.size();
   }

   public ConfigOption getZLevelOptionByIndex(int var1) {
      return (ConfigOption)this.zLevelOptions.get(var1);
   }

   public void setZLevelOption(int var1, boolean var2) {
      for(int var3 = 0; var3 < this.zLevelOptions.size(); ++var3) {
         BooleanDebugOption var4 = (BooleanDebugOption)this.zLevelOptions.get(var3);
         if (var3 != var1) {
            var4.setValue(false);
         } else {
            var4.setValue(var2);
         }
      }

      if (!var2) {
         this.zLevelPlayer.setValue(true);
      }

   }

   public ConfigOption getOptionByName(String var1) {
      for(int var2 = 0; var2 < this.options.size(); ++var2) {
         ConfigOption var3 = (ConfigOption)this.options.get(var2);
         if (var3.getName().equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public int getOptionCount() {
      return this.options.size();
   }

   public ConfigOption getOptionByIndex(int var1) {
      return (ConfigOption)this.options.get(var1);
   }

   public void setBoolean(String var1, boolean var2) {
      ConfigOption var3 = this.getOptionByName(var1);
      if (var3 instanceof BooleanConfigOption) {
         ((BooleanConfigOption)var3).setValue(var2);
      }

   }

   public boolean getBoolean(String var1) {
      ConfigOption var2 = this.getOptionByName(var1);
      return var2 instanceof BooleanConfigOption ? ((BooleanConfigOption)var2).getValue() : false;
   }

   public void save() {
      String var10000 = ZomboidFileSystem.instance.getCacheDir();
      String var1 = var10000 + File.separator + "isoregions-options.ini";
      ConfigFile var2 = new ConfigFile();
      var2.write(var1, 1, this.options);
   }

   public void load() {
      String var10000 = ZomboidFileSystem.instance.getCacheDir();
      String var1 = var10000 + File.separator + "isoregions-options.ini";
      ConfigFile var2 = new ConfigFile();
      if (var2.read(var1)) {
         for(int var3 = 0; var3 < var2.getOptions().size(); ++var3) {
            ConfigOption var4 = (ConfigOption)var2.getOptions().get(var3);
            ConfigOption var5 = this.getOptionByName(var4.getName());
            if (var5 != null) {
               var5.parse(var4.getValueAsString());
            }
         }
      }

   }

   public static class BooleanDebugOption extends BooleanConfigOption {
      private int index;
      private int zLevel = 0;

      public BooleanDebugOption(ArrayList<ConfigOption> var1, String var2, boolean var3, int var4) {
         super(var2, var3);
         this.index = var1.size();
         this.zLevel = var4;
         var1.add(this);
      }

      public BooleanDebugOption(ArrayList<ConfigOption> var1, String var2, boolean var3) {
         super(var2, var3);
         this.index = var1.size();
         var1.add(this);
      }

      public int getIndex() {
         return this.index;
      }
   }
}
