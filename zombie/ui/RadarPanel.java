package zombie.ui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import zombie.IndieGL;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LotHeader;
import zombie.iso.RoomDef;
import zombie.network.GameClient;
import zombie.popman.ZombiePopulationManager;

public final class RadarPanel extends UIElement {
   private int playerIndex;
   private float xPos;
   private float yPos;
   private float offx;
   private float offy;
   private float zoom;
   private float draww;
   private float drawh;
   private Texture mask;
   private Texture border;
   private ArrayList<ZombiePos> zombiePos = new ArrayList();
   private ZombiePosPool zombiePosPool = new ZombiePosPool();
   private int zombiePosFrameCount;
   private boolean[] zombiePosOccupied = new boolean[360];

   public RadarPanel(int var1) {
      this.setX((double)(IsoCamera.getScreenLeft(var1) + 20));
      this.setY((double)(IsoCamera.getScreenTop(var1) + IsoCamera.getScreenHeight(var1) - 120 - 20));
      this.setWidth(120.0);
      this.setHeight(120.0);
      this.mask = Texture.getSharedTexture("media/ui/RadarMask.png");
      this.border = Texture.getSharedTexture("media/ui/RadarBorder.png");
      this.playerIndex = var1;
   }

   public void update() {
      byte var1 = 0;
      if (IsoPlayer.players[this.playerIndex] != null && IsoPlayer.players[this.playerIndex].getJoypadBind() != -1) {
         var1 = -72;
      }

      this.setX((double)(IsoCamera.getScreenLeft(this.playerIndex) + 20));
      this.setY((double)(IsoCamera.getScreenTop(this.playerIndex) + IsoCamera.getScreenHeight(this.playerIndex)) - this.getHeight() - 20.0 + (double)var1);
   }

   public void render() {
      if (this.isVisible()) {
         if (IsoPlayer.players[this.playerIndex] != null) {
            if (!GameClient.bClient) {
               this.draww = (float)this.getWidth().intValue();
               this.drawh = (float)this.getHeight().intValue();
               this.xPos = IsoPlayer.players[this.playerIndex].getX();
               this.yPos = IsoPlayer.players[this.playerIndex].getY();
               this.offx = (float)this.getAbsoluteX().intValue();
               this.offy = (float)this.getAbsoluteY().intValue();
               this.zoom = 3.0F;
               this.stencilOn();
               SpriteRenderer.instance.render((Texture)null, this.offx, this.offy, (float)this.getWidth().intValue(), this.drawh, 0.0F, 0.2F, 0.0F, 0.66F, (Consumer)null);
               this.renderBuildings();
               this.renderRect(this.xPos - 0.5F, this.yPos - 0.5F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);
               this.stencilOff();
               this.renderZombies();
               SpriteRenderer.instance.render(this.border, this.offx - 4.0F, this.offy - 4.0F, this.draww + 8.0F, this.drawh + 8.0F, 1.0F, 1.0F, 1.0F, 0.25F, (Consumer)null);
            }
         }
      }
   }

   private void stencilOn() {
      IndieGL.glStencilMask(255);
      IndieGL.glClear(1280);
      IndieGL.enableStencilTest();
      IndieGL.glStencilFunc(519, 128, 255);
      IndieGL.glStencilOp(7680, 7680, 7681);
      IndieGL.enableAlphaTest();
      IndieGL.glAlphaFunc(516, 0.1F);
      IndieGL.glColorMask(false, false, false, false);
      SpriteRenderer.instance.renderi(this.mask, (int)this.x, (int)this.y, (int)this.width, (int)this.height, 1.0F, 1.0F, 1.0F, 1.0F, (Consumer)null);
      IndieGL.glColorMask(true, true, true, true);
      IndieGL.glAlphaFunc(516, 0.0F);
      IndieGL.glStencilFunc(514, 128, 128);
      IndieGL.glStencilOp(7680, 7680, 7680);
   }

   private void stencilOff() {
      IndieGL.glAlphaFunc(519, 0.0F);
      IndieGL.disableStencilTest();
      IndieGL.disableAlphaTest();
      IndieGL.glStencilFunc(519, 255, 255);
      IndieGL.glStencilOp(7680, 7680, 7680);
      IndieGL.glClear(1280);
   }

   private void renderBuildings() {
      IsoMetaGrid var1 = IsoWorld.instance.MetaGrid;
      int var2 = (int)((this.xPos - 100.0F) / (float)IsoCell.CellSizeInSquares) - var1.minX;
      int var3 = (int)((this.yPos - 100.0F) / (float)IsoCell.CellSizeInSquares) - var1.minY;
      int var4 = (int)((this.xPos + 100.0F) / (float)IsoCell.CellSizeInSquares) - var1.minX;
      int var5 = (int)((this.yPos + 100.0F) / (float)IsoCell.CellSizeInSquares) - var1.minY;
      var2 = Math.max(var2, 0);
      var3 = Math.max(var3, 0);
      var4 = Math.min(var4, var1.gridX() - 1);
      var5 = Math.min(var5, var1.gridY() - 1);

      for(int var6 = var2; var6 <= var4; ++var6) {
         for(int var7 = var3; var7 <= var5; ++var7) {
            if (var1.hasCell(var6, var7)) {
               LotHeader var8 = var1.getCell(var6, var7).info;
               if (var8 != null) {
                  for(int var9 = 0; var9 < var8.Buildings.size(); ++var9) {
                     BuildingDef var10 = (BuildingDef)var8.Buildings.get(var9);

                     for(int var11 = 0; var11 < var10.rooms.size(); ++var11) {
                        if (((RoomDef)var10.rooms.get(var11)).level <= 0) {
                           ArrayList var12 = ((RoomDef)var10.rooms.get(var11)).getRects();

                           for(int var13 = 0; var13 < var12.size(); ++var13) {
                              RoomDef.RoomRect var14 = (RoomDef.RoomRect)var12.get(var13);
                              this.renderRect((float)var14.getX(), (float)var14.getY(), (float)var14.getW(), (float)var14.getH(), 0.5F, 0.5F, 0.8F, 0.3F);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

   }

   private void renderZombies() {
      float var1 = this.offx + this.draww / 2.0F;
      float var2 = this.offy + this.drawh / 2.0F;
      float var3 = this.draww / 2.0F;
      float var4 = 0.5F * this.zoom;
      int var6;
      if (++this.zombiePosFrameCount >= PerformanceSettings.getLockFPS() / 5) {
         this.zombiePosFrameCount = 0;
         this.zombiePosPool.release(this.zombiePos);
         this.zombiePos.clear();
         Arrays.fill(this.zombiePosOccupied, false);
         ArrayList var5 = IsoWorld.instance.CurrentCell.getZombieList();

         float var9;
         float var10;
         for(var6 = 0; var6 < var5.size(); ++var6) {
            IsoZombie var7 = (IsoZombie)var5.get(var6);
            float var8 = this.worldToScreenX(var7.getX());
            var9 = this.worldToScreenY(var7.getY());
            var10 = IsoUtils.DistanceToSquared(var1, var2, var8, var9);
            if (var10 > var3 * var3) {
               double var11 = Math.atan2((double)(var9 - var2), (double)(var8 - var1)) + 3.141592653589793;
               double var13 = (Math.toDegrees(var11) + 180.0) % 360.0;
               this.zombiePosOccupied[(int)var13] = true;
            } else {
               this.zombiePos.add(this.zombiePosPool.alloc(var7.getX(), var7.getY()));
            }
         }

         if (Core.bLastStand) {
            if (ZombiePopulationManager.instance.radarXY == null) {
               ZombiePopulationManager.instance.radarXY = new float[2048];
            }

            float[] var21 = ZombiePopulationManager.instance.radarXY;
            synchronized(var21) {
               for(int var24 = 0; var24 < ZombiePopulationManager.instance.radarCount; ++var24) {
                  var9 = var21[var24 * 2 + 0];
                  var10 = var21[var24 * 2 + 1];
                  float var25 = this.worldToScreenX(var9);
                  float var12 = this.worldToScreenY(var10);
                  float var26 = IsoUtils.DistanceToSquared(var1, var2, var25, var12);
                  if (var26 > var3 * var3) {
                     double var14 = Math.atan2((double)(var12 - var2), (double)(var25 - var1)) + 3.141592653589793;
                     double var16 = (Math.toDegrees(var14) + 180.0) % 360.0;
                     this.zombiePosOccupied[(int)var16] = true;
                  } else {
                     this.zombiePos.add(this.zombiePosPool.alloc(var9, var10));
                  }
               }

               ZombiePopulationManager.instance.radarRenderFlag = true;
            }
         }
      }

      int var20 = this.zombiePos.size();

      for(var6 = 0; var6 < var20; ++var6) {
         ZombiePos var22 = (ZombiePos)this.zombiePos.get(var6);
         this.renderRect(var22.x - 0.5F, var22.y - 0.5F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F);
      }

      for(var6 = 0; var6 < this.zombiePosOccupied.length; ++var6) {
         if (this.zombiePosOccupied[var6]) {
            double var23 = Math.toRadians((double)((float)var6 / (float)this.zombiePosOccupied.length * 360.0F));
            SpriteRenderer.instance.render((Texture)null, var1 + (var3 + 1.0F) * (float)Math.cos(var23) - var4, var2 + (var3 + 1.0F) * (float)Math.sin(var23) - var4, 1.0F * this.zoom, 1.0F * this.zoom, 1.0F, 1.0F, 0.0F, 1.0F, (Consumer)null);
         }
      }

   }

   private float worldToScreenX(float var1) {
      var1 -= this.xPos;
      var1 *= this.zoom;
      var1 += this.offx;
      var1 += this.draww / 2.0F;
      return var1;
   }

   private float worldToScreenY(float var1) {
      var1 -= this.yPos;
      var1 *= this.zoom;
      var1 += this.offy;
      var1 += this.drawh / 2.0F;
      return var1;
   }

   private void renderRect(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
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

   private static class ZombiePosPool {
      private ArrayDeque<ZombiePos> pool = new ArrayDeque();

      private ZombiePosPool() {
      }

      public ZombiePos alloc(float var1, float var2) {
         return this.pool.isEmpty() ? new ZombiePos(var1, var2) : ((ZombiePos)this.pool.pop()).set(var1, var2);
      }

      public void release(Collection<ZombiePos> var1) {
         this.pool.addAll(var1);
      }
   }

   private static final class ZombiePos {
      public float x;
      public float y;

      public ZombiePos(float var1, float var2) {
         this.x = var1;
         this.y = var2;
      }

      public ZombiePos set(float var1, float var2) {
         this.x = var1;
         this.y = var2;
         return this;
      }
   }
}
