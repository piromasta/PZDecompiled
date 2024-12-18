package zombie.pathfind;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.debug.LineDrawer;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.vehicles.BaseVehicle;

final class LineClearCollideMain {
   final Vector2 perp = new Vector2();
   final ArrayList<Point> pts = new ArrayList();
   final VehicleRect sweepAABB = new VehicleRect();
   final VehicleRect vehicleAABB = new VehicleRect();
   final VehiclePoly vehiclePoly = new VehiclePoly();
   final Vector2[] polyVec = new Vector2[4];
   final Vector2[] vehicleVec = new Vector2[4];
   final PointPool pointPool = new PointPool();
   final LiangBarsky LB = new LiangBarsky();

   LineClearCollideMain() {
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
   boolean canStandAtOld(PolygonalMap2 var1, float var2, float var3, float var4, BaseVehicle var5, int var6) {
      boolean var7 = (var6 & 1) != 0;
      boolean var8 = (var6 & 2) != 0;
      int var9 = PZMath.fastfloor(var2 - 0.3F);
      int var10 = PZMath.fastfloor(var3 - 0.3F);
      int var11 = (int)Math.ceil((double)(var2 + 0.3F));
      int var12 = (int)Math.ceil((double)(var3 + 0.3F));

      int var13;
      int var14;
      for(var13 = var10; var13 < var12; ++var13) {
         for(var14 = var9; var14 < var11; ++var14) {
            boolean var15;
            IsoGridSquare var16;
            boolean var10000;
            label192: {
               label191: {
                  var15 = var2 >= (float)var14 && var3 >= (float)var13 && var2 < (float)(var14 + 1) && var3 < (float)(var13 + 1);
                  var16 = IsoWorld.instance.CurrentCell.getGridSquare(var14, var13, PZMath.fastfloor(var4));
                  if (var16 != null) {
                     if (var16.SolidFloorCached) {
                        if (var16.SolidFloor) {
                           break label191;
                        }
                     } else if (var16.TreatAsSolidFloor()) {
                        break label191;
                     }
                  }

                  var10000 = false;
                  break label192;
               }

               var10000 = true;
            }

            boolean var17 = var10000;
            if (var16 == null || var16.getObjects().isEmpty()) {
               IsoGridSquare var18 = IsoWorld.instance.CurrentCell.getGridSquare(var14, var13, PZMath.fastfloor(var4) - 1);
               if (var18 != null && var18.getSlopedSurfaceHeight(0.5F, 0.5F) > 0.9F) {
                  var17 = true;
               }
            }

            float var19;
            float var20;
            float var21;
            float var22;
            float var26;
            if (var16 != null && !var16.isSolid() && (!var16.isSolidTrans() || var16.isAdjacentToWindow() || var16.isAdjacentToHoppable()) && var17) {
               if (var16.HasStairs()) {
                  if (!var8 && !var15) {
                     if (var16.isStairsEdgeBlocked(IsoDirections.N)) {
                        var26 = this.clamp(var2, (float)var14, (float)(var14 + 1));
                        var19 = (float)var13;
                        var20 = var2 - var26;
                        var21 = var3 - var19;
                        var22 = var20 * var20 + var21 * var21;
                        if (var22 < 0.09F) {
                           return false;
                        }
                     }

                     if (var16.isStairsEdgeBlocked(IsoDirections.S)) {
                        var26 = this.clamp(var2, (float)var14, (float)(var14 + 1));
                        var19 = (float)(var13 + 1);
                        var20 = var2 - var26;
                        var21 = var3 - var19;
                        var22 = var20 * var20 + var21 * var21;
                        if (var22 < 0.09F) {
                           return false;
                        }
                     }

                     if (var16.isStairsEdgeBlocked(IsoDirections.W)) {
                        var26 = (float)var14;
                        var19 = this.clamp(var3, (float)var13, (float)(var13 + 1));
                        var20 = var2 - var26;
                        var21 = var3 - var19;
                        var22 = var20 * var20 + var21 * var21;
                        if (var22 < 0.09F) {
                           return false;
                        }
                     }

                     if (var16.isStairsEdgeBlocked(IsoDirections.E)) {
                        var26 = (float)(var14 + 1);
                        var19 = this.clamp(var3, (float)var13, (float)(var13 + 1));
                        var20 = var2 - var26;
                        var21 = var3 - var19;
                        var22 = var20 * var20 + var21 * var21;
                        if (var22 < 0.09F) {
                           return false;
                        }
                     }
                  }
               } else if (var16.hasSlopedSurface()) {
                  if (!var8 && !var15) {
                     if (var16.isSlopedSurfaceEdgeBlocked(IsoDirections.N)) {
                        var26 = this.clamp(var2, (float)var14, (float)(var14 + 1));
                        var19 = (float)var13;
                        var20 = var2 - var26;
                        var21 = var3 - var19;
                        var22 = var20 * var20 + var21 * var21;
                        if (var22 < 0.09F) {
                           return false;
                        }
                     }

                     if (var16.isSlopedSurfaceEdgeBlocked(IsoDirections.S)) {
                        var26 = this.clamp(var2, (float)var14, (float)(var14 + 1));
                        var19 = (float)(var13 + 1);
                        var20 = var2 - var26;
                        var21 = var3 - var19;
                        var22 = var20 * var20 + var21 * var21;
                        if (var22 < 0.09F) {
                           return false;
                        }
                     }

                     if (var16.isSlopedSurfaceEdgeBlocked(IsoDirections.W)) {
                        var26 = (float)var14;
                        var19 = this.clamp(var3, (float)var13, (float)(var13 + 1));
                        var20 = var2 - var26;
                        var21 = var3 - var19;
                        var22 = var20 * var20 + var21 * var21;
                        if (var22 < 0.09F) {
                           return false;
                        }
                     }

                     if (var16.isSlopedSurfaceEdgeBlocked(IsoDirections.E)) {
                        var26 = (float)(var14 + 1);
                        var19 = this.clamp(var3, (float)var13, (float)(var13 + 1));
                        var20 = var2 - var26;
                        var21 = var3 - var19;
                        var22 = var20 * var20 + var21 * var21;
                        if (var22 < 0.09F) {
                           return false;
                        }
                     }
                  }
               } else if (!var8) {
                  if (var16.Is(IsoFlagType.collideW) || !var7 && var16.hasBlockedDoor(false)) {
                     var26 = (float)var14;
                     var19 = this.clamp(var3, (float)var13, (float)(var13 + 1));
                     var20 = var2 - var26;
                     var21 = var3 - var19;
                     var22 = var20 * var20 + var21 * var21;
                     if (var22 < 0.09F) {
                        return false;
                     }
                  }

                  if (var16.Is(IsoFlagType.collideN) || !var7 && var16.hasBlockedDoor(true)) {
                     var26 = this.clamp(var2, (float)var14, (float)(var14 + 1));
                     var19 = (float)var13;
                     var20 = var2 - var26;
                     var21 = var3 - var19;
                     var22 = var20 * var20 + var21 * var21;
                     if (var22 < 0.09F) {
                        return false;
                     }
                  }
               }
            } else if (var8) {
               if (var15) {
                  return false;
               }
            } else {
               var26 = this.clamp(var2, (float)var14, (float)(var14 + 1));
               var19 = this.clamp(var3, (float)var13, (float)(var13 + 1));
               var20 = var2 - var26;
               var21 = var3 - var19;
               var22 = var20 * var20 + var21 * var21;
               if (var22 < 0.09F) {
                  return false;
               }
            }
         }
      }

      var13 = (PZMath.fastfloor(var2) - 4) / 8 - 1;
      var14 = (PZMath.fastfloor(var3) - 4) / 8 - 1;
      int var23 = (int)Math.ceil((double)((var2 + 4.0F) / 8.0F)) + 1;
      int var24 = (int)Math.ceil((double)((var3 + 4.0F) / 8.0F)) + 1;

      for(int var25 = var14; var25 < var24; ++var25) {
         for(int var27 = var13; var27 < var23; ++var27) {
            IsoChunk var28 = GameServer.bServer ? ServerMap.instance.getChunk(var27, var25) : IsoWorld.instance.CurrentCell.getChunkForGridSquare(var27 * 8, var25 * 8, 0);
            if (var28 != null) {
               for(int var29 = 0; var29 < var28.vehicles.size(); ++var29) {
                  BaseVehicle var30 = (BaseVehicle)var28.vehicles.get(var29);
                  if (var30 != var5 && var30.addedToWorld && PZMath.fastfloor(var30.getZ()) == PZMath.fastfloor(var4) && var30.getPolyPlusRadius().containsPoint(var2, var3)) {
                     return false;
                  }
               }
            }
         }
      }

      return true;
   }

   boolean canStandAtClipper(PolygonalMap2 var1, float var2, float var3, float var4, BaseVehicle var5, int var6) {
      return PolygonalMap2.instance.collideWithObstaclesPoly.canStandAt(var2, var3, var4, var5, var6);
   }

   public void drawCircle(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      LineDrawer.DrawIsoCircle(var1, var2, var3, var4, 16, var5, var6, var7, var8);
   }

   boolean isNotClearOld(PolygonalMap2 var1, float var2, float var3, float var4, float var5, int var6, BaseVehicle var7, int var8) {
      boolean var9 = (var8 & 1) != 0;
      boolean var10 = (var8 & 2) != 0;
      boolean var11 = (var8 & 4) != 0;
      boolean var12 = (var8 & 8) != 0;
      IsoGridSquare var13 = IsoWorld.instance.CurrentCell.getGridSquare(PZMath.fastfloor(var2), PZMath.fastfloor(var3), var6);
      if (!this.canStandAtOld(var1, var4, var5, (float)var6, var7, var8)) {
         if (var12) {
            this.drawCircle(var4, var5, (float)var6, 0.3F, 1.0F, 0.0F, 0.0F, 1.0F);
         }

         return true;
      } else {
         float var14 = var5 - var3;
         float var15 = -(var4 - var2);
         this.perp.set(var14, var15);
         this.perp.normalize();
         float var16 = var2 + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
         float var17 = var3 + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
         float var18 = var4 + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
         float var19 = var5 + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
         this.perp.set(-var14, -var15);
         this.perp.normalize();
         float var20 = var2 + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
         float var21 = var3 + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
         float var22 = var4 + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
         float var23 = var5 + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;

         int var24;
         for(var24 = 0; var24 < this.pts.size(); ++var24) {
            this.pointPool.release((Point)this.pts.get(var24));
         }

         this.pts.clear();
         this.pts.add(this.pointPool.alloc().init(PZMath.fastfloor(var2), PZMath.fastfloor(var3)));
         if (PZMath.fastfloor(var2) != PZMath.fastfloor(var4) || PZMath.fastfloor(var3) != PZMath.fastfloor(var5)) {
            this.pts.add(this.pointPool.alloc().init(PZMath.fastfloor(var4), PZMath.fastfloor(var5)));
         }

         var1.supercover(var16, var17, var18, var19, var6, this.pointPool, this.pts);
         var1.supercover(var20, var21, var22, var23, var6, this.pointPool, this.pts);
         if (var12) {
            for(var24 = 0; var24 < this.pts.size(); ++var24) {
               Point var25 = (Point)this.pts.get(var24);
               LineDrawer.addLine((float)var25.x, (float)var25.y, (float)var6, (float)var25.x + 1.0F, (float)var25.y + 1.0F, (float)var6, 1.0F, 1.0F, 0.0F, (String)null, false);
            }
         }

         boolean var39 = false;

         float var29;
         float var44;
         for(int var40 = 0; var40 < this.pts.size(); ++var40) {
            Point var26 = (Point)this.pts.get(var40);
            var13 = IsoWorld.instance.CurrentCell.getGridSquare(var26.x, var26.y, var6);
            if (var11 && var13 != null && SquareUpdateTask.getCost(var13) > 0) {
               return true;
            }

            boolean var10000;
            label430: {
               label429: {
                  if (var13 != null) {
                     if (var13.SolidFloorCached) {
                        if (var13.SolidFloor) {
                           break label429;
                        }
                     } else if (var13.TreatAsSolidFloor()) {
                        break label429;
                     }
                  }

                  var10000 = false;
                  break label430;
               }

               var10000 = true;
            }

            boolean var27 = var10000;
            if (var13 == null || var13.getObjects().isEmpty()) {
               IsoGridSquare var28 = IsoWorld.instance.CurrentCell.getGridSquare(var26.x, var26.y, var6 - 1);
               if (var28 != null && var28.getSlopedSurfaceHeight(0.5F, 0.5F) > 0.9F) {
                  var27 = true;
               }
            }

            float var30;
            float var31;
            if (var13 != null && !var13.isSolid() && (!var13.isSolidTrans() || var13.isAdjacentToWindow() || var13.isAdjacentToHoppable()) && var27) {
               if (var13.HasStairs()) {
                  if (var13.HasStairsNorth()) {
                     var39 |= this.testCollisionVertical(var2, var3, var4, var5, (float)var26.x, (float)var26.y, false, true, (float)var6, var12);
                     if (var39 && !var12) {
                        return true;
                     }

                     var39 |= this.testCollisionVertical(var2, var3, var4, var5, (float)(var26.x + 1), (float)var26.y, true, false, (float)var6, var12);
                     if (var39 && !var12) {
                        return true;
                     }

                     if (var13.Has(IsoObjectType.stairsTN)) {
                        var39 |= this.testCollisionHorizontal(var2, var3, var4, var5, (float)var26.x, (float)var26.y, false, true, (float)var6, var12);
                        if (var39 && !var12) {
                           return true;
                        }
                     }
                  }

                  if (var13.HasStairsWest()) {
                     var39 |= this.testCollisionHorizontal(var2, var3, var4, var5, (float)var26.x, (float)var26.y, false, true, (float)var6, var12);
                     if (var39 && !var12) {
                        return true;
                     }

                     var39 |= this.testCollisionHorizontal(var2, var3, var4, var5, (float)(var26.x + 1), (float)var26.y, true, false, (float)var6, var12);
                     if (var39 && !var12) {
                        return true;
                     }

                     if (var13.Has(IsoObjectType.stairsTW)) {
                        var39 |= this.testCollisionVertical(var2, var3, var4, var5, (float)var26.x, (float)var26.y, false, true, (float)var6, var12);
                        if (var39 && !var12) {
                           return true;
                        }
                     }
                  }
               } else if (var13.hasSlopedSurface()) {
                  IsoDirections var45 = var13.getSlopedSurfaceDirection();
                  IsoGridSquare var46;
                  IsoGridSquare var47;
                  if (var45 == IsoDirections.N || var45 == IsoDirections.S) {
                     if (var13.getAdjacentSquare(IsoDirections.W) == null || !var13.hasIdenticalSlopedSurface(var13.getAdjacentSquare(IsoDirections.W))) {
                        var39 |= this.testCollisionVertical(var2, var3, var4, var5, (float)var26.x, (float)var26.y, false, true, (float)var6, var12);
                        if (var39 && !var12) {
                           return true;
                        }
                     }

                     if (var13.getAdjacentSquare(IsoDirections.E) == null || !var13.hasIdenticalSlopedSurface(var13.getAdjacentSquare(IsoDirections.E))) {
                        var39 |= this.testCollisionVertical(var2, var3, var4, var5, (float)(var26.x + 1), (float)var26.y, true, false, (float)var6, var12);
                        if (var39 && !var12) {
                           return true;
                        }
                     }

                     var46 = var13.getAdjacentSquare(IsoDirections.N);
                     if (var45 == IsoDirections.N && (var46 == null || var13.getSlopedSurfaceHeightMax() != var46.getSlopedSurfaceHeight(0.5F, 1.0F))) {
                        var39 |= this.testCollisionHorizontal(var2, var3, var4, var5, (float)var26.x, (float)var26.y, false, true, (float)var6, var12);
                        if (var39 && !var12) {
                           return true;
                        }
                     }

                     var47 = var13.getAdjacentSquare(IsoDirections.S);
                     if (var45 == IsoDirections.S && (var47 == null || var13.getSlopedSurfaceHeightMax() != var47.getSlopedSurfaceHeight(0.5F, 0.0F))) {
                        var39 |= this.testCollisionHorizontal(var2, var3, var4, var5, (float)var26.x, (float)(var26.y + 1), true, false, (float)var6, var12);
                        if (var39 && !var12) {
                           return true;
                        }
                     }
                  }

                  if (var45 == IsoDirections.W || var45 == IsoDirections.E) {
                     var46 = var13.getAdjacentSquare(IsoDirections.N);
                     if (var46 == null || !var13.hasIdenticalSlopedSurface(var46)) {
                        var39 |= this.testCollisionHorizontal(var2, var3, var4, var5, (float)var26.x, (float)var26.y, false, true, (float)var6, var12);
                        if (var39 && !var12) {
                           return true;
                        }
                     }

                     var47 = var13.getAdjacentSquare(IsoDirections.S);
                     if (var47 == null || !var13.hasIdenticalSlopedSurface(var47)) {
                        var39 |= this.testCollisionHorizontal(var2, var3, var4, var5, (float)var26.x, (float)(var26.y + 1), true, false, (float)var6, var12);
                        if (var39 && !var12) {
                           return true;
                        }
                     }

                     IsoGridSquare var48 = var13.getAdjacentSquare(IsoDirections.W);
                     if (var45 == IsoDirections.W && (var48 == null || var13.getSlopedSurfaceHeightMax() != var48.getSlopedSurfaceHeight(1.0F, 0.5F))) {
                        var39 |= this.testCollisionVertical(var2, var3, var4, var5, (float)var26.x, (float)var26.y, false, true, (float)var6, var12);
                        if (var39 && !var12) {
                           return true;
                        }
                     }

                     IsoGridSquare var32 = var13.getAdjacentSquare(IsoDirections.E);
                     if (var45 == IsoDirections.E && (var32 == null || var13.getSlopedSurfaceHeightMax() != var32.getSlopedSurfaceHeight(0.0F, 0.5F))) {
                        var39 |= this.testCollisionVertical(var2, var3, var4, var5, (float)(var26.x + 1), (float)var26.y, true, false, (float)var6, var12);
                        if (var39 && !var12) {
                           return true;
                        }
                     }
                  }
               } else {
                  if (var13.Is(IsoFlagType.collideW) || !var9 && var13.hasBlockedDoor(false)) {
                     var44 = 0.3F;
                     var29 = 0.3F;
                     var30 = 0.3F;
                     var31 = 0.3F;
                     if (var2 < (float)var26.x && var4 < (float)var26.x) {
                        var44 = 0.0F;
                     } else if (var2 >= (float)var26.x && var4 >= (float)var26.x) {
                        var30 = 0.0F;
                     }

                     if (var3 < (float)var26.y && var5 < (float)var26.y) {
                        var29 = 0.0F;
                     } else if (var3 >= (float)(var26.y + 1) && var5 >= (float)(var26.y + 1)) {
                        var31 = 0.0F;
                     }

                     if (this.LB.lineRectIntersect(var2, var3, var4 - var2, var5 - var3, (float)var26.x - var44, (float)var26.y - var29, (float)var26.x + var30, (float)var26.y + 1.0F + var31)) {
                        if (!var12) {
                           return true;
                        }

                        LineDrawer.addLine((float)var26.x - var44, (float)var26.y - var29, (float)var6, (float)var26.x + var30, (float)var26.y + 1.0F + var31, (float)var6, 1.0F, 0.0F, 0.0F, (String)null, false);
                        var39 = true;
                     }
                  }

                  if (var13.Is(IsoFlagType.collideN) || !var9 && var13.hasBlockedDoor(true)) {
                     var44 = 0.3F;
                     var29 = 0.3F;
                     var30 = 0.3F;
                     var31 = 0.3F;
                     if (var2 < (float)var26.x && var4 < (float)var26.x) {
                        var44 = 0.0F;
                     } else if (var2 >= (float)(var26.x + 1) && var4 >= (float)(var26.x + 1)) {
                        var30 = 0.0F;
                     }

                     if (var3 < (float)var26.y && var5 < (float)var26.y) {
                        var29 = 0.0F;
                     } else if (var3 >= (float)var26.y && var5 >= (float)var26.y) {
                        var31 = 0.0F;
                     }

                     if (this.LB.lineRectIntersect(var2, var3, var4 - var2, var5 - var3, (float)var26.x - var44, (float)var26.y - var29, (float)var26.x + 1.0F + var30, (float)var26.y + var31)) {
                        if (!var12) {
                           return true;
                        }

                        LineDrawer.addLine((float)var26.x - var44, (float)var26.y - var29, (float)var6, (float)var26.x + 1.0F + var30, (float)var26.y + var31, (float)var6, 1.0F, 0.0F, 0.0F, (String)null, false);
                        var39 = true;
                     }
                  }
               }
            } else {
               var44 = 0.3F;
               var29 = 0.3F;
               var30 = 0.3F;
               var31 = 0.3F;
               if (var2 < (float)var26.x && var4 < (float)var26.x) {
                  var44 = 0.0F;
               } else if (var2 >= (float)(var26.x + 1) && var4 >= (float)(var26.x + 1)) {
                  var30 = 0.0F;
               }

               if (var3 < (float)var26.y && var5 < (float)var26.y) {
                  var29 = 0.0F;
               } else if (var3 >= (float)(var26.y + 1) && var5 >= (float)(var26.y + 1)) {
                  var31 = 0.0F;
               }

               if (this.LB.lineRectIntersect(var2, var3, var4 - var2, var5 - var3, (float)var26.x - var44, (float)var26.y - var29, (float)var26.x + 1.0F + var30, (float)var26.y + 1.0F + var31)) {
                  if (!var12) {
                     return true;
                  }

                  LineDrawer.addLine((float)var26.x - var44, (float)var26.y - var29, (float)var6, (float)var26.x + 1.0F + var30, (float)var26.y + 1.0F + var31, (float)var6, 1.0F, 0.0F, 0.0F, (String)null, false);
                  var39 = true;
               }
            }
         }

         float var41 = BaseVehicle.PLUS_RADIUS;
         this.perp.set(var14, var15);
         this.perp.normalize();
         var16 = var2 + this.perp.x * var41;
         var17 = var3 + this.perp.y * var41;
         var18 = var4 + this.perp.x * var41;
         var19 = var5 + this.perp.y * var41;
         this.perp.set(-var14, -var15);
         this.perp.normalize();
         var20 = var2 + this.perp.x * var41;
         var21 = var3 + this.perp.y * var41;
         var22 = var4 + this.perp.x * var41;
         var23 = var5 + this.perp.y * var41;
         float var42 = Math.min(var16, Math.min(var18, Math.min(var20, var22)));
         float var43 = Math.min(var17, Math.min(var19, Math.min(var21, var23)));
         var44 = Math.max(var16, Math.max(var18, Math.max(var20, var22)));
         var29 = Math.max(var17, Math.max(var19, Math.max(var21, var23)));
         this.sweepAABB.init(PZMath.fastfloor(var42), PZMath.fastfloor(var43), (int)Math.ceil((double)var44) - PZMath.fastfloor(var42), (int)Math.ceil((double)var29) - PZMath.fastfloor(var43), var6);
         this.polyVec[0].set(var16, var17);
         this.polyVec[1].set(var18, var19);
         this.polyVec[2].set(var22, var23);
         this.polyVec[3].set(var20, var21);
         int var50 = this.sweepAABB.left() / 8 - 1;
         int var51 = this.sweepAABB.top() / 8 - 1;
         int var49 = (int)Math.ceil((double)((float)this.sweepAABB.right() / 8.0F)) + 1;
         int var33 = (int)Math.ceil((double)((float)this.sweepAABB.bottom() / 8.0F)) + 1;

         for(int var34 = var51; var34 < var33; ++var34) {
            for(int var35 = var50; var35 < var49; ++var35) {
               IsoChunk var36 = GameServer.bServer ? ServerMap.instance.getChunk(var35, var34) : IsoWorld.instance.CurrentCell.getChunkForGridSquare(var35 * 8, var34 * 8, 0);
               if (var36 != null) {
                  for(int var37 = 0; var37 < var36.vehicles.size(); ++var37) {
                     BaseVehicle var38 = (BaseVehicle)var36.vehicles.get(var37);
                     if (var38 != var7 && var38.VehicleID != -1) {
                        this.vehiclePoly.init(var38.getPoly());
                        this.vehiclePoly.getAABB(this.vehicleAABB);
                        if (this.vehicleAABB.intersects(this.sweepAABB) && this.polyVehicleIntersect(this.vehiclePoly, var12)) {
                           var39 = true;
                           if (!var12) {
                              return true;
                           }
                        }
                     }
                  }
               }
            }
         }

         return var39;
      }
   }

   boolean testCollisionHorizontal(float var1, float var2, float var3, float var4, float var5, float var6, boolean var7, boolean var8, float var9, boolean var10) {
      float var11 = 0.3F;
      float var12 = 0.3F;
      float var13 = 0.3F;
      float var14 = 0.3F;
      if (var1 < var5 && var3 < var5) {
         var11 = 0.0F;
      } else if (var1 >= var5 + 1.0F && var3 >= var5 + 1.0F) {
         var13 = 0.0F;
      }

      if (var2 < var6 && var4 < var6) {
         var12 = 0.0F;
      } else if (var2 >= var6 && var4 >= var6) {
         var14 = 0.0F;
      }

      if (var7) {
         var12 = 0.0F;
      }

      if (var8) {
         var14 = 0.0F;
      }

      if (this.LB.lineRectIntersect(var1, var2, var3 - var1, var4 - var2, var5 - var11, var6 - var12, var5 + 1.0F + var13, var6 + var14)) {
         if (var10) {
            LineDrawer.addLine(var5 - var11, var6 - var12, var9, var5 + 1.0F + var13, var6 + var14, var9, 1.0F, 0.0F, 0.0F, (String)null, false);
         }

         return true;
      } else {
         return false;
      }
   }

   boolean testCollisionVertical(float var1, float var2, float var3, float var4, float var5, float var6, boolean var7, boolean var8, float var9, boolean var10) {
      float var11 = 0.3F;
      float var12 = 0.3F;
      float var13 = 0.3F;
      float var14 = 0.3F;
      if (var1 < var5 && var3 < var5) {
         var11 = 0.0F;
      } else if (var1 >= var5 && var3 >= var5) {
         var13 = 0.0F;
      }

      if (var2 < var6 && var4 < var6) {
         var12 = 0.0F;
      } else if (var2 >= var6 + 1.0F && var4 >= var6 + 1.0F) {
         var14 = 0.0F;
      }

      if (var7) {
         var11 = 0.0F;
      }

      if (var8) {
         var13 = 0.0F;
      }

      if (this.LB.lineRectIntersect(var1, var2, var3 - var1, var4 - var2, var5 - var11, var6 - var12, var5 + var13, var6 + 1.0F + var14)) {
         if (var10) {
            LineDrawer.addLine(var5 - var11, var6 - var12, var9, var5 + var13, var6 + 1.0F + var14, var9, 1.0F, 0.0F, 0.0F, (String)null, false);
         }

         return true;
      } else {
         return false;
      }
   }

   boolean isNotClearClipper(PolygonalMap2 var1, float var2, float var3, float var4, float var5, int var6, BaseVehicle var7, int var8) {
      boolean var9 = (var8 & 1) != 0;
      boolean var10 = (var8 & 2) != 0;
      boolean var11 = (var8 & 4) != 0;
      boolean var12 = (var8 & 8) != 0;
      IsoGridSquare var13 = IsoWorld.instance.CurrentCell.getGridSquare(PZMath.fastfloor(var2), PZMath.fastfloor(var3), var6);
      if (var13 != null && var13.HasStairs()) {
         return !var13.isSameStaircase(PZMath.fastfloor(var4), PZMath.fastfloor(var5), var6);
      } else if (!this.canStandAtClipper(var1, var4, var5, (float)var6, var7, var8)) {
         if (var12) {
            this.drawCircle(var4, var5, (float)var6, 0.3F, 1.0F, 0.0F, 0.0F, 1.0F);
         }

         return true;
      } else {
         return PolygonalMap2.instance.collideWithObstaclesPoly.isNotClear(var2, var3, var4, var5, var6, var12, var7, var9, var10);
      }
   }

   boolean isNotClear(PolygonalMap2 var1, float var2, float var3, float var4, float var5, int var6, BaseVehicle var7, int var8) {
      return this.isNotClearOld(var1, var2, var3, var4, var5, var6, var7, var8);
   }

   Vector2 getCollidepoint(PolygonalMap2 var1, float var2, float var3, float var4, float var5, int var6, BaseVehicle var7, int var8) {
      boolean var9 = (var8 & 1) != 0;
      boolean var10 = (var8 & 2) != 0;
      boolean var11 = (var8 & 4) != 0;
      boolean var12 = (var8 & 8) != 0;
      float var14 = var5 - var3;
      float var15 = -(var4 - var2);
      this.perp.set(var14, var15);
      this.perp.normalize();
      float var16 = var2 + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
      float var17 = var3 + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
      float var18 = var4 + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
      float var19 = var5 + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
      this.perp.set(-var14, -var15);
      this.perp.normalize();
      float var20 = var2 + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
      float var21 = var3 + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
      float var22 = var4 + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
      float var23 = var5 + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;

      int var24;
      for(var24 = 0; var24 < this.pts.size(); ++var24) {
         this.pointPool.release((Point)this.pts.get(var24));
      }

      this.pts.clear();
      this.pts.add(this.pointPool.alloc().init(PZMath.fastfloor(var2), PZMath.fastfloor(var3)));
      if (PZMath.fastfloor(var2) != PZMath.fastfloor(var4) || PZMath.fastfloor(var3) != PZMath.fastfloor(var5)) {
         this.pts.add(this.pointPool.alloc().init(PZMath.fastfloor(var4), PZMath.fastfloor(var5)));
      }

      var1.supercover(var16, var17, var18, var19, var6, this.pointPool, this.pts);
      var1.supercover(var20, var21, var22, var23, var6, this.pointPool, this.pts);
      this.pts.sort((var2x, var3x) -> {
         return PZMath.fastfloor(IsoUtils.DistanceManhatten(var2, var3, (float)var2x.x, (float)var2x.y) - IsoUtils.DistanceManhatten(var2, var3, (float)var3x.x, (float)var3x.y));
      });
      Point var25;
      if (var12) {
         for(var24 = 0; var24 < this.pts.size(); ++var24) {
            var25 = (Point)this.pts.get(var24);
            LineDrawer.addLine((float)var25.x, (float)var25.y, (float)var6, (float)var25.x + 1.0F, (float)var25.y + 1.0F, (float)var6, 1.0F, 1.0F, 0.0F, (String)null, false);
         }
      }

      for(var24 = 0; var24 < this.pts.size(); ++var24) {
         var25 = (Point)this.pts.get(var24);
         IsoGridSquare var13 = IsoWorld.instance.CurrentCell.getGridSquare(var25.x, var25.y, var6);
         if (var11 && var13 != null && SquareUpdateTask.getCost(var13) > 0) {
            return PolygonalMap2.temp.set((float)var25.x + 0.5F, (float)var25.y + 0.5F);
         }

         float var26;
         float var27;
         float var28;
         float var29;
         if (var13 != null && !var13.isSolid() && (!var13.isSolidTrans() || var13.isAdjacentToWindow() || var13.isAdjacentToHoppable()) && !var13.HasStairs()) {
            label251: {
               if (var13.SolidFloorCached) {
                  if (!var13.SolidFloor) {
                     break label251;
                  }
               } else if (!var13.TreatAsSolidFloor()) {
                  break label251;
               }

               if (var13.Is(IsoFlagType.collideW) || !var9 && var13.hasBlockedDoor(false)) {
                  var26 = 0.3F;
                  var27 = 0.3F;
                  var28 = 0.3F;
                  var29 = 0.3F;
                  if (var2 < (float)var25.x && var4 < (float)var25.x) {
                     var26 = 0.0F;
                  } else if (var2 >= (float)var25.x && var4 >= (float)var25.x) {
                     var28 = 0.0F;
                  }

                  if (var3 < (float)var25.y && var5 < (float)var25.y) {
                     var27 = 0.0F;
                  } else if (var3 >= (float)(var25.y + 1) && var5 >= (float)(var25.y + 1)) {
                     var29 = 0.0F;
                  }

                  if (this.LB.lineRectIntersect(var2, var3, var4 - var2, var5 - var3, (float)var25.x - var26, (float)var25.y - var27, (float)var25.x + var28, (float)var25.y + 1.0F + var29)) {
                     if (var12) {
                        LineDrawer.addLine((float)var25.x - var26, (float)var25.y - var27, (float)var6, (float)var25.x + var28, (float)var25.y + 1.0F + var29, (float)var6, 1.0F, 0.0F, 0.0F, (String)null, false);
                     }

                     return PolygonalMap2.temp.set((float)var25.x + (var2 - var4 < 0.0F ? -0.5F : 0.5F), (float)var25.y + 0.5F);
                  }
               }

               if (!var13.Is(IsoFlagType.collideN) && (var9 || !var13.hasBlockedDoor(true))) {
                  continue;
               }

               var26 = 0.3F;
               var27 = 0.3F;
               var28 = 0.3F;
               var29 = 0.3F;
               if (var2 < (float)var25.x && var4 < (float)var25.x) {
                  var26 = 0.0F;
               } else if (var2 >= (float)(var25.x + 1) && var4 >= (float)(var25.x + 1)) {
                  var28 = 0.0F;
               }

               if (var3 < (float)var25.y && var5 < (float)var25.y) {
                  var27 = 0.0F;
               } else if (var3 >= (float)var25.y && var5 >= (float)var25.y) {
                  var29 = 0.0F;
               }

               if (this.LB.lineRectIntersect(var2, var3, var4 - var2, var5 - var3, (float)var25.x - var26, (float)var25.y - var27, (float)var25.x + 1.0F + var28, (float)var25.y + var29)) {
                  if (var12) {
                     LineDrawer.addLine((float)var25.x - var26, (float)var25.y - var27, (float)var6, (float)var25.x + 1.0F + var28, (float)var25.y + var29, (float)var6, 1.0F, 0.0F, 0.0F, (String)null, false);
                  }

                  return PolygonalMap2.temp.set((float)var25.x + 0.5F, (float)var25.y + (var3 - var5 < 0.0F ? -0.5F : 0.5F));
               }
               continue;
            }
         }

         var26 = 0.3F;
         var27 = 0.3F;
         var28 = 0.3F;
         var29 = 0.3F;
         if (var2 < (float)var25.x && var4 < (float)var25.x) {
            var26 = 0.0F;
         } else if (var2 >= (float)(var25.x + 1) && var4 >= (float)(var25.x + 1)) {
            var28 = 0.0F;
         }

         if (var3 < (float)var25.y && var5 < (float)var25.y) {
            var27 = 0.0F;
         } else if (var3 >= (float)(var25.y + 1) && var5 >= (float)(var25.y + 1)) {
            var29 = 0.0F;
         }

         if (this.LB.lineRectIntersect(var2, var3, var4 - var2, var5 - var3, (float)var25.x - var26, (float)var25.y - var27, (float)var25.x + 1.0F + var28, (float)var25.y + 1.0F + var29)) {
            if (var12) {
               LineDrawer.addLine((float)var25.x - var26, (float)var25.y - var27, (float)var6, (float)var25.x + 1.0F + var28, (float)var25.y + 1.0F + var29, (float)var6, 1.0F, 0.0F, 0.0F, (String)null, false);
            }

            return PolygonalMap2.temp.set((float)var25.x + 0.5F, (float)var25.y + 0.5F);
         }
      }

      return PolygonalMap2.temp.set(var4, var5);
   }

   boolean polyVehicleIntersect(VehiclePoly var1, boolean var2) {
      this.vehicleVec[0].set(var1.x1, var1.y1);
      this.vehicleVec[1].set(var1.x2, var1.y2);
      this.vehicleVec[2].set(var1.x3, var1.y3);
      this.vehicleVec[3].set(var1.x4, var1.y4);
      boolean var3 = false;

      for(int var4 = 0; var4 < 4; ++var4) {
         Vector2 var5 = this.polyVec[var4];
         Vector2 var6 = var4 == 3 ? this.polyVec[0] : this.polyVec[var4 + 1];

         for(int var7 = 0; var7 < 4; ++var7) {
            Vector2 var8 = this.vehicleVec[var7];
            Vector2 var9 = var7 == 3 ? this.vehicleVec[0] : this.vehicleVec[var7 + 1];
            if (Line2D.linesIntersect((double)var5.x, (double)var5.y, (double)var6.x, (double)var6.y, (double)var8.x, (double)var8.y, (double)var9.x, (double)var9.y)) {
               if (var2) {
                  LineDrawer.addLine(var5.x, var5.y, 0.0F, var6.x, var6.y, 0.0F, 1.0F, 0.0F, 0.0F, (String)null, true);
                  LineDrawer.addLine(var8.x, var8.y, 0.0F, var9.x, var9.y, 0.0F, 1.0F, 0.0F, 0.0F, (String)null, true);
               }

               var3 = true;
            }
         }
      }

      return var3;
   }
}
