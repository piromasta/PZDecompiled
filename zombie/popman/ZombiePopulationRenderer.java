package zombie.popman;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameWindow;
import zombie.MapCollisionData;
import zombie.ZomboidFileSystem;
import zombie.ai.states.WalkTowardState;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.opengl.VBORenderer;
import zombie.core.stash.StashSystem;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.input.Mouse;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.iso.LotHeader;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIFont;
import zombie.vehicles.VehiclesDB2;

public final class ZombiePopulationRenderer {
   private float xPos;
   private float yPos;
   private float offx;
   private float offy;
   private float zoom;
   private float draww;
   private float drawh;
   static final byte RENDER_RECT_FILLED = 0;
   static final byte RENDER_RECT_OUTLINE = 1;
   static final byte RENDER_LINE = 2;
   static final byte RENDER_CIRCLE = 3;
   static final byte RENDER_TEXT = 4;
   private final Drawer[] m_drawers = new Drawer[3];
   private DrawerImpl m_currentDrawer = null;
   private final DrawerImpl m_textDrawer = new DrawerImpl();
   private static final int VERSION = 1;
   private final ArrayList<ConfigOption> options = new ArrayList();
   private BooleanDebugOption CellGrid = new BooleanDebugOption("CellGrid.256x256", true);
   private BooleanDebugOption CellGrid300 = new BooleanDebugOption("CellGrid.300x300", true);
   private BooleanDebugOption CellInfo = new BooleanDebugOption("CellInfo", true);
   private BooleanDebugOption MetaGridBuildings = new BooleanDebugOption("MetaGrid.Buildings", true);
   private BooleanDebugOption ZombiesReal = new BooleanDebugOption("Zombies.Real", true);
   private BooleanDebugOption ZombiesStanding = new BooleanDebugOption("Zombies.Standing", true);
   private BooleanDebugOption ZombiesMoving = new BooleanDebugOption("Zombies.Moving", true);
   private BooleanDebugOption MCDObstacles = new BooleanDebugOption("MapCollisionData.Obstacles", true);
   private BooleanDebugOption MCDRegularChunkOutlines = new BooleanDebugOption("MapCollisionData.RegularChunkOutlines", true);
   private BooleanDebugOption MCDRooms = new BooleanDebugOption("MapCollisionData.Rooms", true);
   private BooleanDebugOption Vehicles = new BooleanDebugOption("Vehicles", true);
   private BooleanDebugOption ZombieIntensity = new BooleanDebugOption("ZombieIntensity", false);

   private native void n_render(float var1, int var2, int var3, float var4, float var5, int var6, int var7);

   private native void n_setWallFollowerStart(int var1, int var2);

   private native void n_setWallFollowerEnd(int var1, int var2);

   private native void n_wallFollowerMouseMove(int var1, int var2);

   private native void n_setDebugOption(String var1, String var2);

   public ZombiePopulationRenderer() {
      for(int var1 = 0; var1 < this.m_drawers.length; ++var1) {
         this.m_drawers[var1] = new Drawer();
      }

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

   public void renderString(float var1, float var2, String var3, double var4, double var6, double var8, double var10) {
      float var12 = this.worldToScreenX(var1);
      float var13 = this.worldToScreenY(var2);
      this.m_currentDrawer.renderRectFilledUI(var12 - 2.0F, var13 - 2.0F, (float)(TextManager.instance.MeasureStringX(UIFont.Small, var3) + 4), (float)(TextManager.instance.font.getLineHeight() + 4), 0.0F, 0.0F, 0.0F, 0.75F);
      this.m_textDrawer.renderStringUI(var12, var13, var3, var4, var6, var8, var10);
   }

   public void renderRect(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      float var9 = this.worldToScreenX(var1);
      float var10 = this.worldToScreenY(var2);
      float var11 = this.worldToScreenX(var1 + var3);
      float var12 = this.worldToScreenY(var2 + var4);
      var3 = var11 - var9;
      var4 = var12 - var10;
      if (!(var9 >= this.offx + this.draww) && !(var11 < this.offx) && !(var10 >= this.offy + this.drawh) && !(var12 < this.offy)) {
         this.m_currentDrawer.renderRectFilledUI(var9, var10, var3, var4, var5, var6, var7, var8);
      }
   }

   public void renderLine(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      float var9 = this.worldToScreenX(var1);
      float var10 = this.worldToScreenY(var2);
      float var11 = this.worldToScreenX(var3);
      float var12 = this.worldToScreenY(var4);
      if ((!(var9 >= (float)Core.getInstance().getScreenWidth()) || !(var11 >= (float)Core.getInstance().getScreenWidth())) && (!(var10 >= (float)Core.getInstance().getScreenHeight()) || !(var12 >= (float)Core.getInstance().getScreenHeight())) && (!(var9 < 0.0F) || !(var11 < 0.0F)) && (!(var10 < 0.0F) || !(var12 < 0.0F))) {
         this.m_currentDrawer.renderLineUI(var9, var10, var11, var12, var5, var6, var7, var8);
      }
   }

   public void renderCircle(float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      byte var8 = 32;
      double var9 = (double)var1 + (double)var3 * Math.cos(Math.toRadians((double)(0.0F / (float)var8)));
      double var11 = (double)var2 + (double)var3 * Math.sin(Math.toRadians((double)(0.0F / (float)var8)));

      for(int var13 = 1; var13 <= var8; ++var13) {
         double var14 = (double)var1 + (double)var3 * Math.cos(Math.toRadians((double)((float)var13 * 360.0F / (float)var8)));
         double var16 = (double)var2 + (double)var3 * Math.sin(Math.toRadians((double)((float)var13 * 360.0F / (float)var8)));
         int var18 = (int)this.worldToScreenX((float)var9);
         int var19 = (int)this.worldToScreenY((float)var11);
         int var20 = (int)this.worldToScreenX((float)var14);
         int var21 = (int)this.worldToScreenY((float)var16);
         this.m_currentDrawer.renderLineUI((float)var18, (float)var19, (float)var20, (float)var21, var4, var5, var6, var7);
         var9 = var14;
         var11 = var16;
      }

   }

   public void renderZombie(float var1, float var2, float var3, float var4, float var5) {
      if (!(this.zoom < 0.2F)) {
         float var6 = 1.0F / this.zoom + 0.5F;
         this.renderRect(var1 - var6 / 2.0F, var2 - var6 / 2.0F, var6, var6, var3, var4, var5, 1.0F);
      }
   }

   public void renderVehicle(int var1, float var2, float var3, float var4, float var5, float var6) {
      float var7 = 2.0F / this.zoom + 0.5F;
      this.renderRect(var2 - var7 / 2.0F, var3 - var7 / 2.0F, var7, var7, var4, var5, var6, 1.0F);
      this.renderString(var2, var3, String.format("%d", var1), (double)var4, (double)var5, (double)var6, 1.0);
   }

   public void outlineRect(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      this.renderLine(var1, var2, var1 + var3, var2, var5, var6, var7, var8);
      this.renderLine(var1 + var3, var2, var1 + var3, var2 + var4, var5, var6, var7, var8);
      this.renderLine(var1, var2 + var4, var1 + var3, var2 + var4, var5, var6, var7, var8);
      this.renderLine(var1, var2, var1, var2 + var4, var5, var6, var7, var8);
   }

   public void renderCellInfo(int var1, int var2, int var3, int var4, float var5) {
      if (this.CellInfo.getValue()) {
         float var6 = this.worldToScreenX((float)(var1 * ZombiePopulationManager.SQUARES_PER_CELL)) + 4.0F;
         float var7 = this.worldToScreenY((float)(var2 * ZombiePopulationManager.SQUARES_PER_CELL)) + 4.0F;
         String var8 = System.getProperty("line.separator");
         String var9 = Translator.getText("IGUI_ZombiePopulation_PopulationEffective") + ": " + var3 + var8 + Translator.getText("IGUI_ZombiePopulation_PopulationTarget") + ": " + var4;
         if (var5 > 0.0F) {
            var9 = var9 + var8 + Translator.getText("IGUI_ZombiePopulation_LastRepopTime") + ": " + String.format(" %.2f", var5);
         }

         this.m_currentDrawer.renderRectFilledUI(var6 - 2.0F, var7 - 2.0F, (float)(TextManager.instance.MeasureStringX(UIFont.Small, var9) + 4), (float)(TextManager.instance.MeasureStringY(UIFont.Small, var9) + 4), 0.0F, 0.0F, 0.0F, 0.75F);
         this.m_textDrawer.renderStringUI(var6, var7, var9, 1.0, 1.0, 1.0, 1.0);
      }
   }

   public void render(UIElement var1, float var2, float var3, float var4) {
      int var5 = SpriteRenderer.instance.getMainStateIndex();
      this.m_currentDrawer = this.m_drawers[var5].impl;
      this.m_currentDrawer.renderBuffer.clear();
      this.m_textDrawer.renderBuffer.clear();
      synchronized(MapCollisionData.instance.renderLock) {
         this._render(var1, var2, var3, var4);
         this.m_currentDrawer.renderBuffer.flip();
         SpriteRenderer.instance.drawGeneric(this.m_drawers[var5]);
      }

      this.renderAllText(var1);
   }

   private void renderAllText(UIElement var1) {
      ByteBuffer var2 = this.m_textDrawer.renderBuffer;
      if (var2.position() != 0) {
         var2.flip();

         while(var2.position() < var2.limit()) {
            byte var3 = var2.get();
            float var4;
            float var5;
            float var7;
            float var8;
            float var9;
            float var10;
            switch (var3) {
               case 0:
                  var4 = var2.getFloat();
                  var5 = var2.getFloat();
                  float var12 = var2.getFloat();
                  var7 = var2.getFloat();
                  var8 = var2.getFloat();
                  var9 = var2.getFloat();
                  var10 = var2.getFloat();
                  float var11 = var2.getFloat();
                  var4 = (float)((double)var4 - var1.getAbsoluteX());
                  var5 = (float)((double)var5 - var1.getAbsoluteY());
                  var1.DrawTextureScaledColor((Texture)null, (double)var4, (double)var5, (double)var12, (double)var7, (double)var8, (double)var9, (double)var10, (double)var11);
                  break;
               case 4:
                  var4 = var2.getFloat();
                  var5 = var2.getFloat();
                  String var6 = GameWindow.ReadStringUTF(var2);
                  var7 = var2.getFloat();
                  var8 = var2.getFloat();
                  var9 = var2.getFloat();
                  var10 = var2.getFloat();
                  TextManager.instance.DrawString((double)var4, (double)var5, var6, (double)var7, (double)var8, (double)var9, (double)var10);
            }
         }

      }
   }

   private void _render(UIElement var1, float var2, float var3, float var4) {
      this.draww = (float)var1.getWidth().intValue();
      this.drawh = (float)var1.getHeight().intValue();
      this.xPos = var3;
      this.yPos = var4;
      this.offx = (float)var1.getAbsoluteX().intValue();
      this.offy = (float)var1.getAbsoluteY().intValue();
      this.zoom = var2;
      IsoCell var5 = IsoWorld.instance.CurrentCell;
      IsoChunkMap var6 = IsoWorld.instance.CurrentCell.ChunkMap[0];
      IsoMetaGrid var7 = IsoWorld.instance.MetaGrid;
      int var8 = (int)(this.uiToWorldX(0.0F) / (float)IsoCell.CellSizeInSquares);
      int var9 = (int)(this.uiToWorldY(0.0F) / (float)IsoCell.CellSizeInSquares);
      int var10 = (int)(this.uiToWorldX(this.draww) / (float)IsoCell.CellSizeInSquares) + 1;
      int var11 = (int)(this.uiToWorldY(this.drawh) / (float)IsoCell.CellSizeInSquares) + 1;
      var8 = PZMath.clamp(var8, var7.getMinX(), var7.getMaxX());
      var9 = PZMath.clamp(var9, var7.getMinY(), var7.getMaxY());
      var10 = PZMath.clamp(var10, var7.getMinX(), var7.getMaxX());
      var11 = PZMath.clamp(var11, var7.getMinY(), var7.getMaxY());
      int var12;
      int var13;
      int var15;
      int var19;
      int var21;
      if (this.MetaGridBuildings.getValue()) {
         for(var12 = var8; var12 <= var10; ++var12) {
            for(var13 = var9; var13 <= var11; ++var13) {
               if (var7.hasCell(var12 - var7.minX, var13 - var7.minY)) {
                  LotHeader var14 = var7.getCell(var12 - var7.minX, var13 - var7.minY).info;
                  if (var14 != null) {
                     for(var15 = 0; var15 < var14.Buildings.size(); ++var15) {
                        BuildingDef var16 = (BuildingDef)var14.Buildings.get(var15);
                        boolean var17 = StashSystem.isStashBuilding(var16);
                        boolean var18 = SpawnPoints.instance.isSpawnBuilding(var16);

                        for(var19 = 0; var19 < var16.rooms.size(); ++var19) {
                           if (((RoomDef)var16.rooms.get(var19)).level <= 0) {
                              ArrayList var20 = ((RoomDef)var16.rooms.get(var19)).getRects();

                              for(var21 = 0; var21 < var20.size(); ++var21) {
                                 RoomDef.RoomRect var22 = (RoomDef.RoomRect)var20.get(var21);
                                 if (var17 && var18) {
                                    this.renderRect((float)var22.getX(), (float)var22.getY(), (float)var22.getW(), (float)var22.getH(), 0.8F, 0.5F, 0.8F, 0.9F);
                                 } else if (var17) {
                                    this.renderRect((float)var22.getX(), (float)var22.getY(), (float)var22.getW(), (float)var22.getH(), 0.8F, 0.5F, 0.5F, 0.6F);
                                 } else if (var16.bAlarmed) {
                                    this.renderRect((float)var22.getX(), (float)var22.getY(), (float)var22.getW(), (float)var22.getH(), 0.8F, 0.8F, 0.5F, 0.3F);
                                 } else if (var18) {
                                    this.renderRect((float)var22.getX(), (float)var22.getY(), (float)var22.getW(), (float)var22.getH(), 0.5F, 0.8F, 0.5F, 0.6F);
                                 } else {
                                    this.renderRect((float)var22.getX(), (float)var22.getY(), (float)var22.getW(), (float)var22.getH(), 0.5F, 0.5F, 0.8F, 0.3F);
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

      int var36;
      if (this.CellGrid.getValue()) {
         float var34 = (float)IsoCell.CellSizeInSquares * var2;

         for(var13 = 1; var34 * (float)var13 < 10.0F; var13 += 2) {
         }

         var8 -= PZMath.coordmodulo(var8, var13);
         var9 -= PZMath.coordmodulo(var9, var13);
         var8 = PZMath.max(var8, IsoWorld.instance.MetaGrid.minX);
         var9 = PZMath.max(var9, IsoWorld.instance.MetaGrid.minY);

         for(var36 = var9; var36 <= var11; var36 += var13) {
            this.renderLine((float)(var8 * IsoCell.CellSizeInSquares), (float)(var36 * IsoCell.CellSizeInSquares), (float)((var10 + 1) * IsoCell.CellSizeInSquares), (float)(var36 * IsoCell.CellSizeInSquares), 1.0F, 1.0F, 1.0F, 0.15F);
         }

         for(var36 = var8; var36 <= var10; var36 += var13) {
            this.renderLine((float)(var36 * IsoCell.CellSizeInSquares), (float)(var9 * IsoCell.CellSizeInSquares), (float)(var36 * IsoCell.CellSizeInSquares), (float)((var11 + 1) * IsoCell.CellSizeInSquares), 1.0F, 1.0F, 1.0F, 0.15F);
         }
      }

      float var38;
      int var44;
      int var46;
      int var50;
      int var52;
      if (this.CellGrid300.getValue()) {
         var12 = (int)(this.uiToWorldX(0.0F) / 300.0F);
         var13 = (int)(this.uiToWorldY(0.0F) / 300.0F);
         var36 = (int)(this.uiToWorldX(this.draww) / 300.0F) + 1;
         var15 = (int)(this.uiToWorldY(this.drawh) / 300.0F) + 1;
         var38 = 300.0F * var2;

         for(var44 = 1; var38 * (float)var44 < 10.0F; var44 += 2) {
         }

         var12 -= PZMath.coordmodulo(var12, var44);
         var13 -= PZMath.coordmodulo(var13, var44);
         var46 = PZMath.fastfloor((float)IsoWorld.instance.MetaGrid.minX * 256.0F / 300.0F);
         var19 = PZMath.fastfloor((float)IsoWorld.instance.MetaGrid.minY * 256.0F / 300.0F);
         var50 = PZMath.fastfloor((float)(IsoWorld.instance.MetaGrid.maxX + 1) * 256.0F / 300.0F) + 1;
         var21 = PZMath.fastfloor((float)(IsoWorld.instance.MetaGrid.maxY + 1) * 256.0F / 300.0F) + 1;
         var12 = PZMath.max(var12, var46);
         var13 = PZMath.max(var13, var19);
         var36 = PZMath.min(var36, var50);
         var15 = PZMath.min(var15, var21);

         for(var52 = var13; var52 <= var15; var52 += var44) {
            this.renderLine((float)(var12 * 300), (float)(var52 * 300), (float)(var36 * 300), (float)(var52 * 300), 0.0F, 1.0F, 1.0F, 0.25F);
         }

         for(var52 = var12; var52 <= var36; var52 += var44) {
            this.renderLine((float)(var52 * 300), (float)(var13 * 300), (float)(var52 * 300), (float)(var15 * 300), 0.0F, 1.0F, 1.0F, 0.25F);
         }

         this.outlineRect((float)(var46 * 300), (float)(var19 * 300), (float)((var50 - var46) * 300), (float)((var21 - var19) * 300), 0.0F, 1.0F, 1.0F, 0.25F);
      }

      boolean var35 = this.ZombieIntensity.getValue() && var2 > 0.5F;
      float var47;
      if (var35) {
         for(var13 = var9; var13 <= var11; ++var13) {
            for(var36 = var8; var36 <= var10; ++var36) {
               if (var7.hasCell(var36 - var7.minX, var13 - var7.minY)) {
                  IsoMetaCell var39 = var7.getCell(var36 - var7.minX, var13 - var7.minY);
                  if (var39 != null && var39.info != null) {
                     for(int var41 = 0; var41 < ZombiePopulationManager.CHUNKS_PER_CELL; ++var41) {
                        for(var44 = 0; var44 < ZombiePopulationManager.CHUNKS_PER_CELL; ++var44) {
                           var46 = LotHeader.getZombieIntensityForChunk(var39.info, var44, var41);
                           if (var46 > 0) {
                              float var49 = (float)var46 / 255.0F;
                              var49 = PZMath.min(1.0F, var49 + 0.1F);
                              float var51 = 0.0F;
                              float var55 = 0.0F;
                              float var23 = 0.9F;
                              this.renderRect((float)(var36 * ZombiePopulationManager.SQUARES_PER_CELL + var44 * 8), (float)(var13 * ZombiePopulationManager.SQUARES_PER_CELL + var41 * 8), 8.0F, 8.0F, var49, var51, var55, var23);
                           }
                        }
                     }
                  }
               }
            }
         }

         double var37 = (double)Mouse.getXA() - var1.getAbsoluteX();
         double var40 = (double)Mouse.getYA() - var1.getAbsoluteY();
         var47 = this.uiToWorldX((float)var37);
         float var48 = this.uiToWorldY((float)var40);
         var19 = PZMath.fastfloor(var47 / (float)ZombiePopulationManager.SQUARES_PER_CELL);
         var50 = PZMath.fastfloor(var48 / (float)ZombiePopulationManager.SQUARES_PER_CELL);
         IsoMetaCell var53 = var7.getCellData(var19, var50);
         if (var53 != null && var53.info != null) {
            var52 = (int)(var47 - (float)(var19 * ZombiePopulationManager.SQUARES_PER_CELL)) / 8;
            int var54 = (int)(var48 - (float)(var50 * ZombiePopulationManager.SQUARES_PER_CELL)) / 8;
            this.outlineRect((float)(var19 * ZombiePopulationManager.SQUARES_PER_CELL + var52 * 8), (float)(var50 * ZombiePopulationManager.SQUARES_PER_CELL + var54 * 8), 8.0F, 8.0F, 1.0F, 1.0F, 1.0F, 1.0F);
            int var24 = LotHeader.getZombieIntensityForChunk(var53.info, var52, var54);
            var24 = PZMath.max(var24, 0);
            String var25 = String.format(Translator.getText("IGUI_ZombiePopulation_Intensity") + ": %d", var24);
            IsoChunk var26 = IsoWorld.instance.CurrentCell.getChunkForGridSquare(PZMath.fastfloor(var47), PZMath.fastfloor(var48), 0);
            if (var26 != null) {
               int var27 = 0;
               int var28 = var26.getMinLevel();

               while(true) {
                  if (var28 > var26.getMaxLevel()) {
                     if (var27 > 0) {
                        var25 = var25 + "\n" + String.format(Translator.getText("IGUI_ZombiePopulation_Zombies") + ": %d", var27);
                     }
                     break;
                  }

                  IsoGridSquare[] var29 = var26.getSquaresForLevel(var28);

                  for(int var30 = 0; var30 < 64; ++var30) {
                     IsoGridSquare var31 = var29[var30];
                     if (var31 != null) {
                        for(int var32 = 0; var32 < var31.getMovingObjects().size(); ++var32) {
                           IsoMovingObject var33 = (IsoMovingObject)var31.getMovingObjects().get(var32);
                           if (var33 instanceof IsoZombie) {
                              ++var27;
                           }
                        }
                     }
                  }

                  ++var28;
               }
            }

            float var56 = this.worldToScreenX((float)(var19 * ZombiePopulationManager.SQUARES_PER_CELL + var52 * 8));
            float var57 = this.worldToScreenY((float)(var50 * ZombiePopulationManager.SQUARES_PER_CELL + var54 * 8 + 8)) + 1.0F;
            this.m_textDrawer.renderRectFilledUI(var56 - 4.0F, var57, (float)(TextManager.instance.MeasureStringX(UIFont.Small, var25) + 8), (float)TextManager.instance.MeasureStringY(UIFont.Small, var25), 0.0F, 0.0F, 0.0F, 0.75F);
            this.m_textDrawer.renderStringUI(var56, var57, var25, 1.0, 1.0, 1.0, 1.0);
         }
      }

      if (this.ZombiesReal.getValue()) {
         for(var13 = 0; var13 < IsoWorld.instance.CurrentCell.getZombieList().size(); ++var13) {
            IsoZombie var42 = (IsoZombie)IsoWorld.instance.CurrentCell.getZombieList().get(var13);
            float var43 = 1.0F;
            var38 = 1.0F;
            var47 = 0.0F;
            if (var42.isReanimatedPlayer()) {
               var43 = 0.0F;
            }

            this.renderZombie(var42.getX(), var42.getY(), var43, var38, var47);
            if (var42.getCurrentState() == WalkTowardState.instance()) {
               this.renderLine(var42.getX(), var42.getY(), (float)var42.getPathTargetX(), (float)var42.getPathTargetY(), 1.0F, 1.0F, 1.0F, 0.5F);
            }
         }
      }

      for(var13 = 0; var13 < IsoPlayer.numPlayers; ++var13) {
         IsoPlayer var45 = IsoPlayer.players[var13];
         if (var45 != null) {
            this.renderZombie(var45.getX(), var45.getY(), 0.0F, 0.5F, 0.0F);
         }
      }

      if (GameClient.bClient) {
         MPDebugInfo.instance.render(this, var2);
      } else {
         if (this.Vehicles.getValue()) {
            VehiclesDB2.instance.renderDebug(this);
         }

         this.n_render(var2, (int)this.offx, (int)this.offy, var3, var4, (int)this.draww, (int)this.drawh);
      }
   }

   public void setWallFollowerStart(int var1, int var2) {
      if (!GameClient.bClient) {
         this.n_setWallFollowerStart(var1, var2);
      }
   }

   public void setWallFollowerEnd(int var1, int var2) {
      if (!GameClient.bClient) {
         this.n_setWallFollowerEnd(var1, var2);
      }
   }

   public void wallFollowerMouseMove(int var1, int var2) {
      if (!GameClient.bClient) {
         this.n_wallFollowerMouseMove(var1, var2);
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
      String var1 = var10000 + File.separator + "popman-options.ini";
      ConfigFile var2 = new ConfigFile();
      var2.write(var1, 1, this.options);

      for(int var3 = 0; var3 < this.options.size(); ++var3) {
         ConfigOption var4 = (ConfigOption)this.options.get(var3);
         this.n_setDebugOption(var4.getName(), var4.getValueAsString());
      }

   }

   public void load() {
      String var10000 = ZomboidFileSystem.instance.getCacheDir();
      String var1 = var10000 + File.separator + "popman-options.ini";
      ConfigFile var2 = new ConfigFile();
      int var3;
      ConfigOption var4;
      if (var2.read(var1)) {
         for(var3 = 0; var3 < var2.getOptions().size(); ++var3) {
            var4 = (ConfigOption)var2.getOptions().get(var3);
            ConfigOption var5 = this.getOptionByName(var4.getName());
            if (var5 != null) {
               var5.parse(var4.getValueAsString());
            }
         }
      }

      for(var3 = 0; var3 < this.options.size(); ++var3) {
         var4 = (ConfigOption)this.options.get(var3);
         this.n_setDebugOption(var4.getName(), var4.getValueAsString());
      }

   }

   private static final class Drawer extends TextureDraw.GenericDrawer {
      final DrawerImpl impl = new DrawerImpl();

      private Drawer() {
      }

      public void render() {
         VBORenderer var1 = VBORenderer.getInstance();
         ByteBuffer var2 = this.impl.renderBuffer;
         float var3 = 0.0F;

         while(var2.position() < var2.limit()) {
            byte var4 = var2.get();
            float var5;
            float var6;
            float var8;
            float var9;
            float var10;
            float var11;
            float var12;
            float var13;
            switch (var4) {
               case 0:
                  var5 = var2.getFloat();
                  var6 = var2.getFloat();
                  var13 = var2.getFloat();
                  var8 = var2.getFloat();
                  var9 = var2.getFloat();
                  var10 = var2.getFloat();
                  var11 = var2.getFloat();
                  var12 = var2.getFloat();
                  var1.startRun(var1.FORMAT_PositionColor);
                  var1.setMode(7);
                  var1.addQuad(var5, var6, var5 + var13, var6 + var8, var3, var9, var10, var11, var12);
                  var1.endRun();
               case 1:
               case 3:
               default:
                  break;
               case 2:
                  var5 = var2.getFloat();
                  var6 = var2.getFloat();
                  var13 = var2.getFloat();
                  var8 = var2.getFloat();
                  var9 = var2.getFloat();
                  var10 = var2.getFloat();
                  var11 = var2.getFloat();
                  var12 = var2.getFloat();
                  var1.startRun(var1.FORMAT_PositionColor);
                  var1.setMode(1);
                  var1.addLine(var5, var6, var3, var13, var8, var3, var9, var10, var11, var12);
                  var1.endRun();
                  break;
               case 4:
                  var5 = var2.getFloat();
                  var6 = var2.getFloat();
                  String var7 = GameWindow.ReadStringUTF(var2);
                  var8 = var2.getFloat();
                  var9 = var2.getFloat();
                  var10 = var2.getFloat();
                  var11 = var2.getFloat();
            }
         }

         var1.flush();
      }
   }

   private static class DrawerImpl {
      ByteBuffer renderBuffer = ByteBuffer.allocate(1024);

      private DrawerImpl() {
      }

      private void reserve(int var1) {
         if (this.renderBuffer.position() + var1 > this.renderBuffer.capacity()) {
            ByteBuffer var2 = ByteBuffer.allocate(this.renderBuffer.capacity() * 2);
            this.renderBuffer.flip();
            var2.put(this.renderBuffer);
            this.renderBuffer = var2;
         }
      }

      private void renderBufferByte(byte var1) {
         this.reserve(1);
         this.renderBuffer.put(var1);
      }

      private void renderBufferFloat(double var1) {
         this.reserve(4);
         this.renderBuffer.putFloat((float)var1);
      }

      private void renderBufferFloat(float var1) {
         this.reserve(4);
         this.renderBuffer.putFloat(var1);
      }

      private void renderBufferInt(int var1) {
         this.reserve(4);
         this.renderBuffer.putInt(var1);
      }

      private void renderBufferString(String var1) {
         ByteBuffer var2 = GameWindow.getEncodedBytesUTF(var1);
         this.reserve(2 + var2.position());
         this.renderBuffer.putShort((short)var2.position());
         var2.flip();
         this.renderBuffer.put(var2);
      }

      private void renderLineUI(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
         this.renderBufferByte((byte)2);
         this.renderBufferFloat(var1);
         this.renderBufferFloat(var2);
         this.renderBufferFloat(var3);
         this.renderBufferFloat(var4);
         this.renderBufferFloat(var5);
         this.renderBufferFloat(var6);
         this.renderBufferFloat(var7);
         this.renderBufferFloat(var8);
      }

      private void renderRectFilledUI(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
         this.renderBufferByte((byte)0);
         this.renderBufferFloat(var1);
         this.renderBufferFloat(var2);
         this.renderBufferFloat(var3);
         this.renderBufferFloat(var4);
         this.renderBufferFloat(var5);
         this.renderBufferFloat(var6);
         this.renderBufferFloat(var7);
         this.renderBufferFloat(var8);
      }

      private void renderStringUI(float var1, float var2, String var3, double var4, double var6, double var8, double var10) {
         this.renderBufferByte((byte)4);
         this.renderBufferFloat(var1);
         this.renderBufferFloat(var2);
         this.renderBufferString(var3);
         this.renderBufferFloat(var4);
         this.renderBufferFloat(var6);
         this.renderBufferFloat(var8);
         this.renderBufferFloat(var10);
      }
   }

   public class BooleanDebugOption extends BooleanConfigOption {
      public BooleanDebugOption(String var2, boolean var3) {
         super(var2, var3);
         ZombiePopulationRenderer.this.options.add(this);
      }
   }
}
