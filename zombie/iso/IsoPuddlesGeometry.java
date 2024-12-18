package zombie.iso;

import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.popman.ObjectPool;

public final class IsoPuddlesGeometry {
   final float[] x = new float[4];
   final float[] y = new float[4];
   final float[] pdne = new float[4];
   final float[] pdnw = new float[4];
   final float[] pda = new float[4];
   final float[] pnon = new float[4];
   final int[] color = new int[4];
   IsoGridSquare square = null;
   boolean bRecalc = true;
   private boolean interiorCalc = false;
   public static final ObjectPool<IsoPuddlesGeometry> pool = new ObjectPool(IsoPuddlesGeometry::new);

   public IsoPuddlesGeometry() {
   }

   public IsoPuddlesGeometry init(IsoGridSquare var1) {
      this.interiorCalc = false;
      IsoObject var2 = var1.getFloor();
      boolean var3 = IsoWater.getInstance().getShaderEnable() && var1.getWater() != null && var1.getWater().isbShore();
      int var4;
      if (!PerformanceSettings.FBORenderChunk || PerformanceSettings.PuddlesQuality != 2 || var3 || var2 != null && var2.getProperties() != null && var2.getProperties().Is(IsoFlagType.transparentFloor)) {
         this.x[0] = IsoUtils.XToScreen((float)(var1.x - var1.z * 3), (float)(var1.y - var1.z * 3), (float)var1.z, var1.z);
         this.y[0] = IsoUtils.YToScreen((float)(var1.x - var1.z * 3), (float)(var1.y - var1.z * 3), (float)var1.z, var1.z);
         this.x[1] = IsoUtils.XToScreen((float)(var1.x - var1.z * 3), (float)(var1.y + 1 - var1.z * 3), 0.0F, 0);
         this.y[1] = IsoUtils.YToScreen((float)(var1.x - var1.z * 3), (float)(var1.y + 1 - var1.z * 3), 0.0F, 0);
         this.x[2] = IsoUtils.XToScreen((float)(var1.x + 1 - var1.z * 3), (float)(var1.y + 1 - var1.z * 3), 0.0F, 0);
         this.y[2] = IsoUtils.YToScreen((float)(var1.x + 1 - var1.z * 3), (float)(var1.y + 1 - var1.z * 3), 0.0F, 0);
         this.x[3] = IsoUtils.XToScreen((float)(var1.x + 1 - var1.z * 3), (float)(var1.y - var1.z * 3), 0.0F, 0);
         this.y[3] = IsoUtils.YToScreen((float)(var1.x + 1 - var1.z * 3), (float)(var1.y - var1.z * 3), 0.0F, 0);
         int var10002 = this.x[0]--;
         var10002 = this.x[1]--;
         var10002 = this.x[2]--;
         var10002 = this.x[3]--;
      } else {
         var4 = 8;
         int var5 = PZMath.coordmodulo(var1.x, var4);
         int var6 = PZMath.coordmodulo(var1.y, var4);
         this.x[0] = IsoUtils.XToScreen((float)(var5 - var1.z * 3), (float)(var6 - var1.z * 3), (float)var1.z, var1.z);
         this.y[0] = IsoUtils.YToScreen((float)(var5 - var1.z * 3), (float)(var6 - var1.z * 3), (float)var1.z, var1.z);
         this.x[1] = IsoUtils.XToScreen((float)(var5 - var1.z * 3), (float)(var6 + 1 - var1.z * 3), 0.0F, 0);
         this.y[1] = IsoUtils.YToScreen((float)(var5 - var1.z * 3), (float)(var6 + 1 - var1.z * 3), 0.0F, 0);
         this.x[2] = IsoUtils.XToScreen((float)(var5 + 1 - var1.z * 3), (float)(var6 + 1 - var1.z * 3), 0.0F, 0);
         this.y[2] = IsoUtils.YToScreen((float)(var5 + 1 - var1.z * 3), (float)(var6 + 1 - var1.z * 3), 0.0F, 0);
         this.x[3] = IsoUtils.XToScreen((float)(var5 + 1 - var1.z * 3), (float)(var6 - var1.z * 3), 0.0F, 0);
         this.y[3] = IsoUtils.YToScreen((float)(var5 + 1 - var1.z * 3), (float)(var6 - var1.z * 3), 0.0F, 0);
      }

      this.square = var1;
      if (!var1.getProperties().Is(IsoFlagType.water) && var1.getProperties().Is(IsoFlagType.exterior) && !var1.hasSlopedSurface()) {
         for(var4 = 0; var4 < 4; ++var4) {
            this.pdne[var4] = 0.0F;
            this.pdnw[var4] = 0.0F;
            this.pda[var4] = 1.0F;
            this.pnon[var4] = 0.0F;
         }

         if (Core.getInstance().getPerfPuddles() > 1) {
            return this;
         } else {
            IsoCell var14 = var1.getCell();
            IsoGridSquare var13 = var14.getGridSquare(var1.x - 1, var1.y, var1.z);
            IsoGridSquare var15 = var14.getGridSquare(var1.x - 1, var1.y - 1, var1.z);
            IsoGridSquare var7 = var14.getGridSquare(var1.x, var1.y - 1, var1.z);
            IsoGridSquare var8 = var14.getGridSquare(var1.x - 1, var1.y + 1, var1.z);
            IsoGridSquare var9 = var14.getGridSquare(var1.x, var1.y + 1, var1.z);
            IsoGridSquare var10 = var14.getGridSquare(var1.x + 1, var1.y + 1, var1.z);
            IsoGridSquare var11 = var14.getGridSquare(var1.x + 1, var1.y, var1.z);
            IsoGridSquare var12 = var14.getGridSquare(var1.x + 1, var1.y - 1, var1.z);
            if (var7 != null && var15 != null && var13 != null && var8 != null && var9 != null && var10 != null && var11 != null && var12 != null) {
               this.setFlags(0, var13.getPuddlesDir() | var15.getPuddlesDir() | var7.getPuddlesDir());
               this.setFlags(1, var13.getPuddlesDir() | var8.getPuddlesDir() | var9.getPuddlesDir());
               this.setFlags(2, var9.getPuddlesDir() | var10.getPuddlesDir() | var11.getPuddlesDir());
               this.setFlags(3, var11.getPuddlesDir() | var12.getPuddlesDir() | var7.getPuddlesDir());
               return this;
            } else {
               return this;
            }
         }
      } else {
         for(var4 = 0; var4 < 4; ++var4) {
            this.pdne[var4] = 0.0F;
            this.pdnw[var4] = 0.0F;
            this.pda[var4] = 0.0F;
            this.pnon[var4] = 0.0F;
         }

         return this;
      }
   }

   private void setFlags(int var1, int var2) {
      this.pdne[var1] = 0.0F;
      this.pdnw[var1] = 0.0F;
      this.pda[var1] = 0.0F;
      this.pnon[var1] = 0.0F;
      if ((var2 & IsoGridSquare.PuddlesDirection.PUDDLES_DIR_NE) != 0) {
         this.pdne[var1] = 1.0F;
      }

      if ((var2 & IsoGridSquare.PuddlesDirection.PUDDLES_DIR_NW) != 0) {
         this.pdnw[var1] = 1.0F;
      }

      if ((var2 & IsoGridSquare.PuddlesDirection.PUDDLES_DIR_ALL) != 0) {
         this.pda[var1] = 1.0F;
      }

   }

   public void recalcIfNeeded() {
      if (this.bRecalc) {
         this.bRecalc = false;

         try {
            this.init(this.square);
         } catch (Throwable var2) {
            ExceptionLogger.logException(var2);
         }
      }

   }

   public boolean shouldRender() {
      this.recalcIfNeeded();

      int var1;
      for(var1 = 0; var1 < 4; ++var1) {
         if (this.pdne[var1] + this.pdnw[var1] + this.pda[var1] + this.pnon[var1] > 0.0F) {
            return true;
         }
      }

      if (this.square.getProperties().Is(IsoFlagType.water)) {
         return false;
      } else if (IsoPuddles.leakingPuddlesInTheRoom && !this.interiorCalc && this.square != null) {
         for(var1 = 0; var1 < 4; ++var1) {
            this.pdne[var1] = 0.0F;
            this.pdnw[var1] = 0.0F;
            this.pda[var1] = 0.0F;
            this.pnon[var1] = 1.0F;
         }

         IsoGridSquare var10 = this.square.getAdjacentSquare(IsoDirections.W);
         IsoGridSquare var2 = this.square.getAdjacentSquare(IsoDirections.NW);
         IsoGridSquare var3 = this.square.getAdjacentSquare(IsoDirections.N);
         IsoGridSquare var4 = this.square.getAdjacentSquare(IsoDirections.SW);
         IsoGridSquare var5 = this.square.getAdjacentSquare(IsoDirections.S);
         IsoGridSquare var6 = this.square.getAdjacentSquare(IsoDirections.SE);
         IsoGridSquare var7 = this.square.getAdjacentSquare(IsoDirections.E);
         IsoGridSquare var8 = this.square.getAdjacentSquare(IsoDirections.NE);
         if (var10 == null || var3 == null || var5 == null || var7 == null || var2 == null || var8 == null || var4 == null || var6 == null || !var10.getProperties().Is(IsoFlagType.exterior) && !var3.getProperties().Is(IsoFlagType.exterior) && !var5.getProperties().Is(IsoFlagType.exterior) && !var7.getProperties().Is(IsoFlagType.exterior)) {
            return false;
         } else {
            int var9;
            if (!this.square.getProperties().Is(IsoFlagType.collideW) && var10.getProperties().Is(IsoFlagType.exterior)) {
               this.pnon[0] = 0.0F;
               this.pnon[1] = 0.0F;

               for(var9 = 0; var9 < 4; ++var9) {
                  this.pda[var9] = 1.0F;
               }
            }

            if (!var5.getProperties().Is(IsoFlagType.collideN) && var5.getProperties().Is(IsoFlagType.exterior)) {
               this.pnon[1] = 0.0F;
               this.pnon[2] = 0.0F;

               for(var9 = 0; var9 < 4; ++var9) {
                  this.pda[var9] = 1.0F;
               }
            }

            if (!var7.getProperties().Is(IsoFlagType.collideW) && var7.getProperties().Is(IsoFlagType.exterior)) {
               this.pnon[2] = 0.0F;
               this.pnon[3] = 0.0F;

               for(var9 = 0; var9 < 4; ++var9) {
                  this.pda[var9] = 1.0F;
               }
            }

            if (!this.square.getProperties().Is(IsoFlagType.collideN) && var3.getProperties().Is(IsoFlagType.exterior)) {
               this.pnon[3] = 0.0F;
               this.pnon[0] = 0.0F;

               for(var9 = 0; var9 < 4; ++var9) {
                  this.pda[var9] = 1.0F;
               }
            }

            if (var3.getProperties().Is(IsoFlagType.collideW) || !var2.getProperties().Is(IsoFlagType.exterior)) {
               this.pnon[0] = 1.0F;

               for(var9 = 0; var9 < 4; ++var9) {
                  this.pda[var9] = 1.0F;
               }
            }

            if (var5.getProperties().Is(IsoFlagType.collideW) || !var4.getProperties().Is(IsoFlagType.exterior)) {
               this.pnon[1] = 1.0F;

               for(var9 = 0; var9 < 4; ++var9) {
                  this.pda[var9] = 1.0F;
               }
            }

            if (var4.getProperties().Is(IsoFlagType.collideN) || !var4.getProperties().Is(IsoFlagType.exterior)) {
               this.pnon[1] = 1.0F;

               for(var9 = 0; var9 < 4; ++var9) {
                  this.pda[var9] = 1.0F;
               }
            }

            if (var6.getProperties().Is(IsoFlagType.collideN) || !var6.getProperties().Is(IsoFlagType.exterior)) {
               this.pnon[2] = 1.0F;

               for(var9 = 0; var9 < 4; ++var9) {
                  this.pda[var9] = 1.0F;
               }
            }

            if (var6.getProperties().Is(IsoFlagType.collideW) || !var6.getProperties().Is(IsoFlagType.exterior)) {
               this.pnon[2] = 1.0F;

               for(var9 = 0; var9 < 4; ++var9) {
                  this.pda[var9] = 1.0F;
               }
            }

            if (var8.getProperties().Is(IsoFlagType.collideW) || !var8.getProperties().Is(IsoFlagType.exterior)) {
               this.pnon[3] = 1.0F;

               for(var9 = 0; var9 < 4; ++var9) {
                  this.pda[var9] = 1.0F;
               }
            }

            if (var7.getProperties().Is(IsoFlagType.collideN) || !var8.getProperties().Is(IsoFlagType.exterior)) {
               this.pnon[3] = 1.0F;

               for(var9 = 0; var9 < 4; ++var9) {
                  this.pda[var9] = 1.0F;
               }
            }

            if (var10.getProperties().Is(IsoFlagType.collideN) || !var2.getProperties().Is(IsoFlagType.exterior)) {
               this.pnon[0] = 1.0F;

               for(var9 = 0; var9 < 4; ++var9) {
                  this.pda[var9] = 1.0F;
               }
            }

            this.interiorCalc = true;

            for(var9 = 0; var9 < 4; ++var9) {
               if (this.pdne[var9] + this.pdnw[var9] + this.pda[var9] + this.pnon[var9] > 0.0F) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public void updateLighting(int var1) {
      this.setLightingAtVert(0, this.square.getVertLight(0, var1));
      this.setLightingAtVert(1, this.square.getVertLight(3, var1));
      this.setLightingAtVert(2, this.square.getVertLight(2, var1));
      this.setLightingAtVert(3, this.square.getVertLight(1, var1));
   }

   private void setLightingAtVert(int var1, int var2) {
      this.color[var1] = var2;
   }
}
