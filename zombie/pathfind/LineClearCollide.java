package zombie.pathfind;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.iso.IsoDirections;
import zombie.iso.Vector2;
import zombie.vehicles.BaseVehicle;

final class LineClearCollide {
   final Vector2 perp = new Vector2();
   final ArrayList<Point> pts = new ArrayList();
   final VehicleRect sweepAABB = new VehicleRect();
   final VehicleRect vehicleAABB = new VehicleRect();
   final Vector2[] polyVec = new Vector2[4];
   final Vector2[] vehicleVec = new Vector2[4];
   final PointPool pointPool = new PointPool();
   final LiangBarsky LB = new LiangBarsky();

   LineClearCollide() {
      for(int var1 = 0; var1 < 4; ++var1) {
         this.polyVec[var1] = new Vector2();
         this.vehicleVec[var1] = new Vector2();
      }

   }

   private float clamp(float var1, float var2, float var3) {
      if (var1 < var2) {
         var1 = var2;
      }

      if (var1 > var3) {
         var1 = var3;
      }

      return var1;
   }

   /** @deprecated */
   @Deprecated
   boolean canStandAt(PolygonalMap2 var1, float var2, float var3, float var4, float var5, float var6, Vehicle var7) {
      if ((PZMath.fastfloor(var2) != PZMath.fastfloor(var4) || PZMath.fastfloor(var3) != PZMath.fastfloor(var5)) && var1.isBlockedInAllDirections(PZMath.fastfloor(var4), PZMath.fastfloor(var5), PZMath.fastfloor(var6))) {
         return false;
      } else {
         int var8 = PZMath.fastfloor(var4 - 0.3F);
         int var9 = PZMath.fastfloor(var5 - 0.3F);
         int var10 = (int)Math.ceil((double)(var4 + 0.3F));
         int var11 = (int)Math.ceil((double)(var5 + 0.3F));

         int var12;
         for(var12 = var9; var12 < var11; ++var12) {
            for(int var13 = var8; var13 < var10; ++var13) {
               Square var14 = var1.getSquare(var13, var12, PZMath.fastfloor(var6));
               boolean var15 = var4 >= (float)var13 && var5 >= (float)var12 && var4 < (float)(var13 + 1) && var5 < (float)(var12 + 1);
               if (var14 != null && !var14.isReallySolid() && var14.has(512)) {
                  if (!var14.has(504) && var14.hasSlopedSurface()) {
                  }
               } else if (var15) {
                  return false;
               }
            }
         }

         for(var12 = 0; var12 < var1.vehicles.size(); ++var12) {
            Vehicle var16 = (Vehicle)var1.vehicles.get(var12);
            if (var16 != var7 && PZMath.fastfloor(var16.polyPlusRadius.z) == PZMath.fastfloor(var6) && var16.polyPlusRadius.containsPoint(var4, var5)) {
               return false;
            }
         }

         return true;
      }
   }

   boolean canStandAtClipper(PolygonalMap2 var1, float var2, float var3, float var4, float var5, float var6, Vehicle var7, int var8) {
      if ((PZMath.fastfloor(var2) != PZMath.fastfloor(var4) || PZMath.fastfloor(var3) != PZMath.fastfloor(var5)) && var1.isBlockedInAllDirections(PZMath.fastfloor(var4), PZMath.fastfloor(var5), PZMath.fastfloor(var6))) {
         return false;
      } else {
         Chunk var9 = var1.getChunkFromSquarePos(PZMath.fastfloor(var4), PZMath.fastfloor(var5));
         if (var9 == null) {
            return false;
         } else {
            ChunkDataZ var10 = var9.collision.init(var9, PZMath.fastfloor(var6));

            for(int var11 = 0; var11 < var10.obstacles.size(); ++var11) {
               Obstacle var12 = (Obstacle)var10.obstacles.get(var11);
               if ((var7 == null || var12.vehicle != var7) && var12.bounds.containsPoint(var4, var5) && var12.isPointInside(var4, var5, var8)) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   float isLeft(float var1, float var2, float var3, float var4, float var5, float var6) {
      return (var3 - var1) * (var6 - var2) - (var5 - var1) * (var4 - var2);
   }

   boolean isPointInPolygon_WindingNumber(float var1, float var2, VehiclePoly var3) {
      this.polyVec[0].set(var3.x1, var3.y1);
      this.polyVec[1].set(var3.x2, var3.y2);
      this.polyVec[2].set(var3.x3, var3.y3);
      this.polyVec[3].set(var3.x4, var3.y4);
      int var4 = 0;

      for(int var5 = 0; var5 < 4; ++var5) {
         Vector2 var6 = this.polyVec[var5];
         Vector2 var7 = var5 == 3 ? this.polyVec[0] : this.polyVec[var5 + 1];
         if (var6.y <= var2) {
            if (var7.y > var2 && this.isLeft(var6.x, var6.y, var7.x, var7.y, var1, var2) > 0.0F) {
               ++var4;
            }
         } else if (var7.y <= var2 && this.isLeft(var6.x, var6.y, var7.x, var7.y, var1, var2) < 0.0F) {
            --var4;
         }
      }

      return var4 != 0;
   }

   /** @deprecated */
   @Deprecated
   boolean isNotClearOld(PolygonalMap2 var1, float var2, float var3, float var4, float var5, int var6, int var7) {
      boolean var8 = (var7 & 4) != 0;
      var1.getSquare(PZMath.fastfloor(var2), PZMath.fastfloor(var3), var6);
      if (!this.canStandAt(var1, var2, var3, var4, var5, (float)var6, (Vehicle)null)) {
         return true;
      } else {
         float var10 = var5 - var3;
         float var11 = -(var4 - var2);
         this.perp.set(var10, var11);
         this.perp.normalize();
         float var12 = var2 + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
         float var13 = var3 + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
         float var14 = var4 + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
         float var15 = var5 + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
         this.perp.set(-var10, -var11);
         this.perp.normalize();
         float var16 = var2 + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
         float var17 = var3 + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
         float var18 = var4 + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
         float var19 = var5 + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;

         int var20;
         for(var20 = 0; var20 < this.pts.size(); ++var20) {
            this.pointPool.release((Point)this.pts.get(var20));
         }

         this.pts.clear();
         this.pts.add(this.pointPool.alloc().init(PZMath.fastfloor(var2), PZMath.fastfloor(var3)));
         if (PZMath.fastfloor(var2) != PZMath.fastfloor(var4) || PZMath.fastfloor(var3) != PZMath.fastfloor(var5)) {
            this.pts.add(this.pointPool.alloc().init(PZMath.fastfloor(var4), PZMath.fastfloor(var5)));
         }

         var1.supercover(var12, var13, var14, var15, var6, this.pointPool, this.pts);
         var1.supercover(var16, var17, var18, var19, var6, this.pointPool, this.pts);

         float var22;
         float var23;
         float var24;
         for(var20 = 0; var20 < this.pts.size(); ++var20) {
            Point var21 = (Point)this.pts.get(var20);
            Square var9 = var1.getSquare(var21.x, var21.y, var6);
            if (var8 && var9 != null && var9.cost > 0) {
               return true;
            }

            float var25;
            if (var9 != null && !var9.isReallySolid() && var9.has(512)) {
               if (var9.has(504)) {
                  if (var9.has(448)) {
                     if (this.testCollisionVertical(var2, var3, var4, var5, (float)var21.x, (float)var21.y, false, true)) {
                        return true;
                     }

                     if (this.testCollisionVertical(var2, var3, var4, var5, (float)(var21.x + 1), (float)var21.y, true, false)) {
                        return true;
                     }

                     if (var9.has(64) && this.testCollisionHorizontal(var2, var3, var4, var5, (float)var21.x, (float)var21.y, false, true)) {
                        return true;
                     }
                  }

                  if (var9.has(56)) {
                     if (this.testCollisionHorizontal(var2, var3, var4, var5, (float)var21.x, (float)var21.y, false, true)) {
                        return true;
                     }

                     if (this.testCollisionHorizontal(var2, var3, var4, var5, (float)(var21.x + 1), (float)var21.y, true, false)) {
                        return true;
                     }

                     if (var9.has(8) && this.testCollisionVertical(var2, var3, var4, var5, (float)var21.x, (float)var21.y, false, true)) {
                        return true;
                     }
                  }
               } else if (var9.hasSlopedSurface()) {
                  IsoDirections var30 = var9.getSlopedSurfaceDirection();
                  Square var31;
                  Square var33;
                  if (var30 == IsoDirections.N || var30 == IsoDirections.S) {
                     if ((var9.getAdjacentSquare(IsoDirections.W) == null || !var9.hasIdenticalSlopedSurface(var9.getAdjacentSquare(IsoDirections.W))) && this.testCollisionVertical(var2, var3, var4, var5, (float)var21.x, (float)var21.y, false, true)) {
                        return true;
                     }

                     if ((var9.getAdjacentSquare(IsoDirections.E) == null || !var9.hasIdenticalSlopedSurface(var9.getAdjacentSquare(IsoDirections.E))) && this.testCollisionVertical(var2, var3, var4, var5, (float)(var21.x + 1), (float)var21.y, true, false)) {
                        return true;
                     }

                     var31 = var9.getAdjacentSquare(IsoDirections.N);
                     if (var30 == IsoDirections.N && (var31 == null || var9.getSlopedSurfaceHeightMax() != var31.getSlopedSurfaceHeight(0.5F, 1.0F)) && this.testCollisionHorizontal(var2, var3, var4, var5, (float)var21.x, (float)var21.y, false, true)) {
                        return true;
                     }

                     var33 = var9.getAdjacentSquare(IsoDirections.S);
                     if (var30 == IsoDirections.S && (var33 == null || var9.getSlopedSurfaceHeightMax() != var33.getSlopedSurfaceHeight(0.5F, 0.0F)) && this.testCollisionHorizontal(var2, var3, var4, var5, (float)var21.x, (float)(var21.y + 1), true, false)) {
                        return true;
                     }
                  }

                  if (var30 == IsoDirections.W || var30 == IsoDirections.E) {
                     var31 = var9.getAdjacentSquare(IsoDirections.N);
                     if ((var31 == null || !var9.hasIdenticalSlopedSurface(var31)) && this.testCollisionHorizontal(var2, var3, var4, var5, (float)var21.x, (float)var21.y, false, true)) {
                        return true;
                     }

                     var33 = var9.getAdjacentSquare(IsoDirections.S);
                     if ((var33 == null || !var9.hasIdenticalSlopedSurface(var33)) && this.testCollisionHorizontal(var2, var3, var4, var5, (float)var21.x, (float)(var21.y + 1), true, false)) {
                        return true;
                     }

                     Square var34 = var9.getAdjacentSquare(IsoDirections.W);
                     if (var30 == IsoDirections.W && (var34 == null || var9.getSlopedSurfaceHeightMax() != var34.getSlopedSurfaceHeight(1.0F, 0.5F)) && this.testCollisionVertical(var2, var3, var4, var5, (float)var21.x, (float)var21.y, false, true)) {
                        return true;
                     }

                     Square var26 = var9.getAdjacentSquare(IsoDirections.E);
                     if (var30 == IsoDirections.E && (var26 == null || var9.getSlopedSurfaceHeightMax() != var26.getSlopedSurfaceHeight(0.0F, 0.5F)) && this.testCollisionVertical(var2, var3, var4, var5, (float)(var21.x + 1), (float)var21.y, true, false)) {
                        return true;
                     }
                  }
               } else {
                  if (var9.isCollideW()) {
                     var22 = 0.3F;
                     var23 = 0.3F;
                     var24 = 0.3F;
                     var25 = 0.3F;
                     if (var2 < (float)var21.x && var4 < (float)var21.x) {
                        var22 = 0.0F;
                     } else if (var2 >= (float)var21.x && var4 >= (float)var21.x) {
                        var24 = 0.0F;
                     }

                     if (var3 < (float)var21.y && var5 < (float)var21.y) {
                        var23 = 0.0F;
                     } else if (var3 >= (float)(var21.y + 1) && var5 >= (float)(var21.y + 1)) {
                        var25 = 0.0F;
                     }

                     if (this.LB.lineRectIntersect(var2, var3, var4 - var2, var5 - var3, (float)var21.x - var22, (float)var21.y - var23, (float)var21.x + var24, (float)var21.y + 1.0F + var25)) {
                        return true;
                     }
                  }

                  if (var9.isCollideN()) {
                     var22 = 0.3F;
                     var23 = 0.3F;
                     var24 = 0.3F;
                     var25 = 0.3F;
                     if (var2 < (float)var21.x && var4 < (float)var21.x) {
                        var22 = 0.0F;
                     } else if (var2 >= (float)(var21.x + 1) && var4 >= (float)(var21.x + 1)) {
                        var24 = 0.0F;
                     }

                     if (var3 < (float)var21.y && var5 < (float)var21.y) {
                        var23 = 0.0F;
                     } else if (var3 >= (float)var21.y && var5 >= (float)var21.y) {
                        var25 = 0.0F;
                     }

                     if (this.LB.lineRectIntersect(var2, var3, var4 - var2, var5 - var3, (float)var21.x - var22, (float)var21.y - var23, (float)var21.x + 1.0F + var24, (float)var21.y + var25)) {
                        return true;
                     }
                  }
               }
            } else {
               var22 = 0.3F;
               var23 = 0.3F;
               var24 = 0.3F;
               var25 = 0.3F;
               if (var2 < (float)var21.x && var4 < (float)var21.x) {
                  var22 = 0.0F;
               } else if (var2 >= (float)(var21.x + 1) && var4 >= (float)(var21.x + 1)) {
                  var24 = 0.0F;
               }

               if (var3 < (float)var21.y && var5 < (float)var21.y) {
                  var23 = 0.0F;
               } else if (var3 >= (float)(var21.y + 1) && var5 >= (float)(var21.y + 1)) {
                  var25 = 0.0F;
               }

               if (this.LB.lineRectIntersect(var2, var3, var4 - var2, var5 - var3, (float)var21.x - var22, (float)var21.y - var23, (float)var21.x + 1.0F + var24, (float)var21.y + 1.0F + var25)) {
                  return true;
               }
            }
         }

         float var28 = BaseVehicle.PLUS_RADIUS;
         this.perp.set(var10, var11);
         this.perp.normalize();
         var12 = var2 + this.perp.x * var28;
         var13 = var3 + this.perp.y * var28;
         var14 = var4 + this.perp.x * var28;
         var15 = var5 + this.perp.y * var28;
         this.perp.set(-var10, -var11);
         this.perp.normalize();
         var16 = var2 + this.perp.x * var28;
         var17 = var3 + this.perp.y * var28;
         var18 = var4 + this.perp.x * var28;
         var19 = var5 + this.perp.y * var28;
         float var29 = Math.min(var12, Math.min(var14, Math.min(var16, var18)));
         var22 = Math.min(var13, Math.min(var15, Math.min(var17, var19)));
         var23 = Math.max(var12, Math.max(var14, Math.max(var16, var18)));
         var24 = Math.max(var13, Math.max(var15, Math.max(var17, var19)));
         this.sweepAABB.init(PZMath.fastfloor(var29), PZMath.fastfloor(var22), (int)Math.ceil((double)var23) - PZMath.fastfloor(var29), (int)Math.ceil((double)var24) - PZMath.fastfloor(var22), var6);
         this.polyVec[0].set(var12, var13);
         this.polyVec[1].set(var14, var15);
         this.polyVec[2].set(var18, var19);
         this.polyVec[3].set(var16, var17);

         for(int var35 = 0; var35 < var1.vehicles.size(); ++var35) {
            Vehicle var32 = (Vehicle)var1.vehicles.get(var35);
            VehicleRect var27 = var32.poly.getAABB(this.vehicleAABB);
            if (var27.intersects(this.sweepAABB) && this.polyVehicleIntersect(var32.poly)) {
               return true;
            }
         }

         return false;
      }
   }

   boolean testCollisionHorizontal(float var1, float var2, float var3, float var4, float var5, float var6, boolean var7, boolean var8) {
      float var9 = 0.3F;
      float var10 = 0.3F;
      float var11 = 0.3F;
      float var12 = 0.3F;
      if (var1 < var5 && var3 < var5) {
         var9 = 0.0F;
      } else if (var1 >= var5 + 1.0F && var3 >= var5 + 1.0F) {
         var11 = 0.0F;
      }

      if (var2 < var6 && var4 < var6) {
         var10 = 0.0F;
      } else if (var2 >= var6 && var4 >= var6) {
         var12 = 0.0F;
      }

      if (var7) {
         var10 = 0.0F;
      }

      if (var8) {
         var12 = 0.0F;
      }

      return this.LB.lineRectIntersect(var1, var2, var3 - var1, var4 - var2, var5 - var9, var6 - var10, var5 + 1.0F + var11, var6 + var12);
   }

   boolean testCollisionVertical(float var1, float var2, float var3, float var4, float var5, float var6, boolean var7, boolean var8) {
      float var9 = 0.3F;
      float var10 = 0.3F;
      float var11 = 0.3F;
      float var12 = 0.3F;
      if (var1 < var5 && var3 < var5) {
         var9 = 0.0F;
      } else if (var1 >= var5 && var3 >= var5) {
         var11 = 0.0F;
      }

      if (var2 < var6 && var4 < var6) {
         var10 = 0.0F;
      } else if (var2 >= var6 + 1.0F && var4 >= var6 + 1.0F) {
         var12 = 0.0F;
      }

      if (var7) {
         var9 = 0.0F;
      }

      if (var8) {
         var11 = 0.0F;
      }

      return this.LB.lineRectIntersect(var1, var2, var3 - var1, var4 - var2, var5 - var9, var6 - var10, var5 + var11, var6 + 1.0F + var12);
   }

   boolean isNotClearClipper(PolygonalMap2 var1, float var2, float var3, float var4, float var5, int var6, int var7) {
      boolean var8 = (var7 & 4) != 0;
      Square var9 = var1.getSquare(PZMath.fastfloor(var2), PZMath.fastfloor(var3), var6);
      if (var9 != null && var9.has(504)) {
         return true;
      } else if (!this.canStandAtClipper(var1, var2, var3, var4, var5, (float)var6, (Vehicle)null, var7)) {
         return true;
      } else {
         float var10 = var2 / 8.0F;
         float var11 = var3 / 8.0F;
         float var12 = var4 / 8.0F;
         float var13 = var5 / 8.0F;
         double var14 = (double)Math.abs(var12 - var10);
         double var16 = (double)Math.abs(var13 - var11);
         int var18 = PZMath.fastfloor(var10);
         int var19 = PZMath.fastfloor(var11);
         int var20 = 1;
         byte var21;
         double var23;
         if (var14 == 0.0) {
            var21 = 0;
            var23 = 1.0 / 0.0;
         } else if (var12 > var10) {
            var21 = 1;
            var20 += PZMath.fastfloor(var12) - var18;
            var23 = (double)((float)(PZMath.fastfloor(var10) + 1) - var10) * var16;
         } else {
            var21 = -1;
            var20 += var18 - PZMath.fastfloor(var12);
            var23 = (double)(var10 - (float)PZMath.fastfloor(var10)) * var16;
         }

         byte var22;
         if (var16 == 0.0) {
            var22 = 0;
            var23 -= 1.0 / 0.0;
         } else if (var13 > var11) {
            var22 = 1;
            var20 += PZMath.fastfloor(var13) - var19;
            var23 -= (double)((float)(PZMath.fastfloor(var11) + 1) - var11) * var14;
         } else {
            var22 = -1;
            var20 += var19 - PZMath.fastfloor(var13);
            var23 -= (double)(var11 - (float)PZMath.fastfloor(var11)) * var14;
         }

         for(; var20 > 0; --var20) {
            Chunk var25 = PolygonalMap2.instance.getChunkFromChunkPos(var18, var19);
            if (var25 != null) {
               ChunkDataZ var26 = var25.collision.init(var25, var6);
               ArrayList var27 = var26.obstacles;

               for(int var28 = 0; var28 < var27.size(); ++var28) {
                  Obstacle var29 = (Obstacle)var27.get(var28);
                  if (var29.lineSegmentIntersects(var2, var3, var4, var5)) {
                     return true;
                  }
               }
            }

            if (var23 > 0.0) {
               var19 += var22;
               var23 -= var14;
            } else {
               var18 += var21;
               var23 += var16;
            }
         }

         if (var8 && this.isNotClearCost(var2, var3, var4, var5, var6)) {
            return true;
         } else {
            return false;
         }
      }
   }

   boolean isNotClearCost(float var1, float var2, float var3, float var4, int var5) {
      float var6 = var4 - var2;
      float var7 = -(var3 - var1);
      this.perp.set(var6, var7);
      this.perp.normalize();
      float var8 = var1 + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
      float var9 = var2 + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
      float var10 = var3 + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
      float var11 = var4 + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
      this.perp.set(-var6, -var7);
      this.perp.normalize();
      float var12 = var1 + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
      float var13 = var2 + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
      float var14 = var3 + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
      float var15 = var4 + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;

      int var16;
      for(var16 = 0; var16 < this.pts.size(); ++var16) {
         this.pointPool.release((Point)this.pts.get(var16));
      }

      this.pts.clear();
      this.pts.add(this.pointPool.alloc().init(PZMath.fastfloor(var1), PZMath.fastfloor(var2)));
      if (PZMath.fastfloor(var1) != PZMath.fastfloor(var3) || PZMath.fastfloor(var2) != PZMath.fastfloor(var4)) {
         this.pts.add(this.pointPool.alloc().init(PZMath.fastfloor(var3), PZMath.fastfloor(var4)));
      }

      PolygonalMap2.instance.supercover(var8, var9, var10, var11, var5, this.pointPool, this.pts);
      PolygonalMap2.instance.supercover(var12, var13, var14, var15, var5, this.pointPool, this.pts);

      for(var16 = 0; var16 < this.pts.size(); ++var16) {
         Point var17 = (Point)this.pts.get(var16);
         Square var18 = PolygonalMap2.instance.getSquare(var17.x, var17.y, var5);
         if (var18 != null && var18.cost > 0) {
            return true;
         }
      }

      return false;
   }

   boolean isNotClear(PolygonalMap2 var1, float var2, float var3, float var4, float var5, int var6, int var7) {
      return this.isNotClearOld(var1, var2, var3, var4, var5, var6, var7);
   }

   boolean polyVehicleIntersect(VehiclePoly var1) {
      this.vehicleVec[0].set(var1.x1, var1.y1);
      this.vehicleVec[1].set(var1.x2, var1.y2);
      this.vehicleVec[2].set(var1.x3, var1.y3);
      this.vehicleVec[3].set(var1.x4, var1.y4);
      boolean var2 = false;

      for(int var3 = 0; var3 < 4; ++var3) {
         Vector2 var4 = this.polyVec[var3];
         Vector2 var5 = var3 == 3 ? this.polyVec[0] : this.polyVec[var3 + 1];

         for(int var6 = 0; var6 < 4; ++var6) {
            Vector2 var7 = this.vehicleVec[var6];
            Vector2 var8 = var6 == 3 ? this.vehicleVec[0] : this.vehicleVec[var6 + 1];
            if (Line2D.linesIntersect((double)var4.x, (double)var4.y, (double)var5.x, (double)var5.y, (double)var7.x, (double)var7.y, (double)var8.x, (double)var8.y)) {
               var2 = true;
            }
         }
      }

      return var2;
   }
}
