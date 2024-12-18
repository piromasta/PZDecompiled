package zombie.vispoly;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjglx.BufferUtils;
import zombie.GameProfiler;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.Styles.FloatList;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.Shader;
import zombie.core.opengl.VBORenderer;
import zombie.core.rendering.RenderTarget;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.input.GameKeyboard;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.LosUtil;
import zombie.iso.PlayerCamera;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.objects.IsoTree;
import zombie.popman.ObjectPool;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;
import zombie.vehicles.ClipperPolygon;

public final class VisibilityPolygon2 {
   private static VisibilityPolygon2 instance = null;
   boolean USE_CIRCLE = false;
   private final Drawer[][] m_drawers = new Drawer[4][3];
   private final ObjectPool<VisibilityWall> s_visibilityWallPool = new ObjectPool(VisibilityWall::new);
   int dirtyObstacleCounter = 0;

   public static VisibilityPolygon2 getInstance() {
      if (instance == null) {
         instance = new VisibilityPolygon2();
      }

      return instance;
   }

   private VisibilityPolygon2() {
      for(int var1 = 0; var1 < 4; ++var1) {
         for(int var2 = 0; var2 < 3; ++var2) {
            this.m_drawers[var1][var2] = new Drawer();
         }
      }

   }

   public void renderMain(int var1) {
      if (DebugOptions.instance.FBORenderChunk.RenderVisionPolygon.getValue()) {
         int var2 = SpriteRenderer.instance.getMainStateIndex();
         Drawer var3 = this.m_drawers[var1][var2];
         var3.calculateVisibilityPolygon(var1);
         SpriteRenderer.instance.drawGeneric(var3);
      }
   }

   private static final class Drawer extends TextureDraw.GenericDrawer {
      int playerIndex;
      float px;
      float py;
      int pz;
      float visionCone;
      float lookAngleRadians;
      final Vector2 lookVector = new Vector2();
      float px1;
      float py1;
      float px2;
      float py2;
      float dirX1;
      float dirY1;
      float dirX2;
      float dirY2;
      float CIRCLE_RADIUS = 40.0F;
      static final ArrayList<Vector3f> circlePoints = new ArrayList();
      final Partition[] partitions = new Partition[8];
      final TFloatArrayList shadows = new TFloatArrayList();
      boolean bInsideTree = false;
      int dirtyObstacleCounter = -1;
      boolean bClipperPolygonsDirty = true;
      final ArrayList<ClipperPolygon> clipperPolygons = new ArrayList();
      static Texture blurTex;
      static Texture blurDepthTex;
      static TextureFBO blurFBO;
      static Shader blurShader;
      static Shader polygonShader;
      static Shader blitShader;
      static int downscale = 2;
      static float shadowBlurRamp = 0.015F;
      float zoom = 1.0F;
      private final PolygonData polygonData = new PolygonData();
      private final FloatList shadowVerts;
      private static final Vector2 edge = new Vector2();
      private static final Vector3[] angles = new Vector3[]{new Vector3(), new Vector3(), new Vector3(), new Vector3()};

      Drawer() {
         this.shadowVerts = new FloatList(FloatList.ExpandStyle.Normal, 10000);

         for(int var1 = 0; var1 < 8; ++var1) {
            this.partitions[var1] = new Partition();
            this.partitions[var1].minAngle = (float)(var1 * 45);
            this.partitions[var1].maxAngle = (float)((var1 + 1) * 45);
         }

         this.initCirclePoints(this.CIRCLE_RADIUS, 32);
      }

      void initCirclePoints(float var1, int var2) {
         this.CIRCLE_RADIUS = var1;
         circlePoints.clear();
         float var3 = 0.0F;
         float var4 = 0.0F;

         for(int var5 = 0; var5 < var2; ++var5) {
            double var6 = Math.toRadians((double)var5 * 360.0 / (double)var2);
            double var8 = (double)var3 + (double)var1 * Math.cos(var6);
            double var10 = (double)var4 + (double)var1 * Math.sin(var6);
            circlePoints.add(new Vector3f((float)var8, (float)var10, Vector2.getDirection((float)var8, (float)var10) + 3.1415927F));
         }

      }

      boolean isDirty(IsoPlayer var1) {
         if (this.bClipperPolygonsDirty) {
            return true;
         } else if (this.dirtyObstacleCounter != VisibilityPolygon2.instance.dirtyObstacleCounter) {
            return true;
         } else if (this.zoom != IsoCamera.frameState.zoom) {
            return true;
         } else if (this.px == var1.getX() && this.py == var1.getY() && this.pz == PZMath.fastfloor(var1.getZ())) {
            float var2 = LightingJNI.calculateVisionCone(var1);
            if (var2 != this.visionCone) {
               return true;
            } else {
               float var3 = var1.getLookAngleRadians();
               BaseVehicle var4 = var1.getVehicle();
               if (var4 != null && !var1.isAiming() && !var1.isLookingWhileInVehicle() && var4.isDriver(var1) && var4.getCurrentSpeedKmHour() < -1.0F) {
                  var3 += 3.1415927F;
               }

               return var3 != this.lookAngleRadians;
            }
         } else {
            return true;
         }
      }

      void calculateVisibilityPolygon(int var1) {
         if (DebugOptions.instance.UseNewVisibility.getValue()) {
            this.calculateVisibilityPolygonNew(var1);
         } else {
            this.calculateVisibilityPolygonOld(var1);
         }

      }

      void calculateVisibilityPolygonNew(int var1) {
         this.playerIndex = var1;
         IsoPlayer var2 = IsoPlayer.players[var1];
         if (this.isDirty(var2)) {
            this.bInsideTree = false;
            this.dirtyObstacleCounter = VisibilityPolygon2.instance.dirtyObstacleCounter;
            this.shadowVerts.clear();
            IsoChunkMap var3 = IsoWorld.instance.CurrentCell.ChunkMap[var1];
            int var4 = var3.getWorldXMinTiles();
            int var5 = var3.getWorldYMinTiles();
            int var6 = var3.getWorldXMaxTiles();
            int var7 = var3.getWorldYMaxTiles();
            this.px = IsoCamera.frameState.CamCharacterX;
            this.py = IsoCamera.frameState.CamCharacterY;
            this.pz = PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ);
            this.zoom = IsoCamera.frameState.zoom;
            this.visionCone = LightingJNI.calculateVisionCone(var2);
            this.lookAngleRadians = var2.getLookAngleRadians();
            var2.getLookVector(this.lookVector);
            this.polygonData.update(var1);
            BaseVehicle var8 = var2.getVehicle();
            if (var8 != null && !var2.isAiming() && !var2.isLookingWhileInVehicle() && var8.isDriver(var2) && var8.getCurrentSpeedKmHour() < -1.0F) {
               this.lookAngleRadians += 3.1415927F;
               this.lookVector.rotate(3.1415927F);
            }

            IsoGridSquare var9 = IsoPlayer.players[var1].getCurrentSquare();
            if (var9 != null) {
               IsoTree var10 = var9.getTree();
               if (var10 != null && var10.getProperties() != null && var10.getProperties().Is(IsoFlagType.blocksight)) {
                  this.bInsideTree = true;
               }

               if (this.bInsideTree) {
                  this.addFullScreenShadow();
                  this.bClipperPolygonsDirty = true;
               } else {
                  this.addViewShadow();
                  IsoChunk[] var11 = var3.getChunks();

                  for(int var12 = 0; var12 < var11.length; ++var12) {
                     IsoChunk var13 = var11[var12];
                     if (var13 != null && var13.bLoaded && !var13.bLightingNeverDone[var1]) {
                        FBORenderLevels var14 = var13.getRenderLevels(var1);
                        if (var14.isOnScreen(this.pz)) {
                           ChunkLevelData var15 = var13.getVispolyDataForLevel(this.pz);
                           if (var15 != null && var13.IsOnScreen(false)) {
                              if (var15.m_adjacentChunkLoadedCounter != var13.m_adjacentChunkLoadedCounter) {
                                 var15.m_adjacentChunkLoadedCounter = var13.m_adjacentChunkLoadedCounter;
                                 var15.recreate();
                              }

                              int var16;
                              for(var16 = 0; var16 < var15.m_allWalls.size(); ++var16) {
                                 VisibilityWall var17 = (VisibilityWall)var15.m_allWalls.get(var16);
                                 if (var17.isHorizontal()) {
                                    if (var17.y1 == var5 || var17.y1 == var7) {
                                       continue;
                                    }
                                 } else if (var17.x1 == var4 || var17.x1 == var6) {
                                    continue;
                                 }

                                 if (this.isInViewCone((float)var17.x1, (float)var17.y1, (float)var17.x2, (float)var17.y2)) {
                                    this.addWallShadow(var17);
                                 }
                              }

                              for(var16 = 0; var16 < var15.m_solidSquares.size(); ++var16) {
                                 IsoGridSquare var18 = (IsoGridSquare)var15.m_solidSquares.get(var16);
                                 if (this.isInViewCone(var18)) {
                                    this.addSquareShadow(var18);
                                 }
                              }
                           }
                        }
                     }
                  }

                  this.bClipperPolygonsDirty = true;
               }
            }
         }
      }

      void calculateVisibilityPolygonOld(int var1) {
         this.playerIndex = var1;
         IsoPlayer var2 = IsoPlayer.players[var1];
         if (Core.bDebug && GameKeyboard.isKeyDown(28) || this.isDirty(var2)) {
            this.bClipperPolygonsDirty = true;
            this.dirtyObstacleCounter = VisibilityPolygon2.instance.dirtyObstacleCounter;
            this.visionCone = LightingJNI.calculateVisionCone(var2);
            this.lookAngleRadians = var2.getLookAngleRadians();
            var2.getLookVector(this.lookVector);
            BaseVehicle var3 = var2.getVehicle();
            if (var3 != null && !var2.isAiming() && !var2.isLookingWhileInVehicle() && var3.isDriver(var2) && var3.getCurrentSpeedKmHour() < -1.0F) {
               this.lookAngleRadians += 3.1415927F;
               this.lookVector.rotate(3.1415927F);
            }

            this.shadows.clear();
            this.bInsideTree = false;
            HashSet var4 = VisibilityPolygon2.L_calculateVisibilityPolygon.segmentHashSet;
            var4.clear();

            for(int var5 = 0; var5 < this.partitions.length; ++var5) {
               var4.addAll(this.partitions[var5].segments);
               this.partitions[var5].segments.clear();
            }

            VisibilityPolygon2.L_calculateVisibilityPolygon.segmentList.clear();
            VisibilityPolygon2.L_calculateVisibilityPolygon.segmentList.addAll(var4);
            VisibilityPolygon2.L_calculateVisibilityPolygon.m_segmentPool.releaseAll(VisibilityPolygon2.L_calculateVisibilityPolygon.segmentList);
            IsoChunkMap var26 = IsoWorld.instance.CurrentCell.ChunkMap[var1];
            int var6 = var26.getWorldXMinTiles();
            int var7 = var26.getWorldYMinTiles();
            int var8 = var26.getWorldXMaxTiles();
            int var9 = var26.getWorldYMaxTiles();
            this.px = IsoCamera.frameState.CamCharacterX;
            this.py = IsoCamera.frameState.CamCharacterY;
            this.pz = PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ);
            float var10 = -this.visionCone;
            float var11 = this.lookAngleRadians;
            float var12 = var11 + (float)Math.acos((double)var10);
            float var13 = var11 - (float)Math.acos((double)var10);
            this.dirX1 = (float)Math.cos((double)var13);
            this.dirY1 = (float)Math.sin((double)var13);
            this.dirX2 = (float)Math.cos((double)var12);
            this.dirY2 = (float)Math.sin((double)var12);
            this.px1 = this.px + this.dirX1 * 1500.0F;
            this.py1 = this.py + this.dirY1 * 1500.0F;
            this.px2 = this.px + this.dirX2 * 1500.0F;
            this.py2 = this.py + this.dirY2 * 1500.0F;
            IsoGridSquare var14 = IsoPlayer.players[var1].getCurrentSquare();
            if (var14 != null) {
               IsoTree var15 = var14.getTree();
               if (var15 != null && var15.getProperties() != null && var15.getProperties().Is(IsoFlagType.blocksight)) {
                  this.bInsideTree = true;
               } else {
                  GameProfiler.ProfileArea var16 = GameProfiler.getInstance().startIfEnabled("Collect");
                  ArrayList var17 = VisibilityPolygon2.L_calculateVisibilityPolygon.sortedWalls;
                  var17.clear();
                  ArrayList var18 = VisibilityPolygon2.L_calculateVisibilityPolygon.solidSquares;
                  var18.clear();

                  int var20;
                  for(int var19 = 0; var19 < IsoChunkMap.ChunkGridWidth; ++var19) {
                     for(var20 = 0; var20 < IsoChunkMap.ChunkGridWidth; ++var20) {
                        IsoChunk var21 = var26.getChunk(var20, var19);
                        if (var21 != null && !var21.bLightingNeverDone[var1] && var21.bLoaded && var21.IsOnScreen(true)) {
                           FBORenderLevels var22 = var21.getRenderLevels(var1);
                           if (var22.isOnScreen(this.pz)) {
                              ChunkLevelData var23 = var21.getVispolyDataForLevel(this.pz);
                              if (var23 != null) {
                                 if (Core.bDebug && GameKeyboard.isKeyDown(28)) {
                                    var23.invalidate();
                                 }

                                 if (var23.m_adjacentChunkLoadedCounter != var21.m_adjacentChunkLoadedCounter) {
                                    var23.m_adjacentChunkLoadedCounter = var21.m_adjacentChunkLoadedCounter;
                                    var23.recreate();
                                 }

                                 int var24;
                                 for(var24 = 0; var24 < var23.m_allWalls.size(); ++var24) {
                                    VisibilityWall var25 = (VisibilityWall)var23.m_allWalls.get(var24);
                                    if (var25.isHorizontal()) {
                                       if (var25.y1 == var7 || var25.y1 == var9) {
                                          continue;
                                       }
                                    } else if (var25.x1 == var6 || var25.x1 == var8) {
                                       continue;
                                    }

                                    if (this.isInViewCone((float)var25.x1, (float)var25.y1, (float)var25.x2, (float)var25.y2)) {
                                       var17.add(var25);
                                    }
                                 }

                                 for(var24 = 0; var24 < var23.m_solidSquares.size(); ++var24) {
                                    IsoGridSquare var33 = (IsoGridSquare)var23.m_solidSquares.get(var24);
                                    if (this.isInViewCone(var33)) {
                                       var18.add(var33);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }

                  GameProfiler.getInstance().end(var16);
                  GameProfiler.ProfileArea var27 = GameProfiler.getInstance().startIfEnabled("Walls");
                  var17.sort((var1x, var2x) -> {
                     float var3 = IsoUtils.DistanceToSquared((float)var1x.x1 + (float)(var1x.x2 - var1x.x1) * 0.5F, (float)var1x.y1 + (float)(var1x.y2 - var1x.y1) * 0.5F, this.px, this.py);
                     float var4 = IsoUtils.DistanceToSquared((float)var2x.x1 + (float)(var2x.x2 - var2x.x1) * 0.5F, (float)var2x.y1 + (float)(var2x.y2 - var2x.y1) * 0.5F, this.px, this.py);
                     return Float.compare(var3, var4);
                  });

                  for(var20 = 0; var20 < var17.size(); ++var20) {
                     VisibilityWall var29 = (VisibilityWall)var17.get(var20);
                     float var31 = 0.0F;
                     if (var29.y1 == var29.y2) {
                        this.addPolygonForLineSegment(this.px, this.py, (float)var29.x1 - var31, (float)var29.y1, (float)var29.x2 + var31, (float)var29.y2);
                     } else {
                        this.addPolygonForLineSegment(this.px, this.py, (float)var29.x1, (float)var29.y1 - var31, (float)var29.x2, (float)var29.y2 + var31);
                     }
                  }

                  GameProfiler.getInstance().end(var27);
                  GameProfiler.ProfileArea var28 = GameProfiler.getInstance().startIfEnabled("Squares");
                  var18.sort((var1x, var2x) -> {
                     float var3 = IsoUtils.DistanceToSquared((float)var1x.x + 0.5F, (float)var1x.y + 0.5F, this.px, this.py);
                     float var4 = IsoUtils.DistanceToSquared((float)var2x.x + 0.5F, (float)var2x.y + 0.5F, this.px, this.py);
                     return Float.compare(var3, var4);
                  });

                  for(int var30 = 0; var30 < var18.size(); ++var30) {
                     IsoGridSquare var32 = (IsoGridSquare)var18.get(var30);
                     if (Vector2.dot((float)var32.x + 0.5F - this.px, (float)var32.y - this.py, 0.0F, -1.0F) < 0.0F) {
                        this.addPolygonForLineSegment(this.px, this.py, (float)var32.x, (float)var32.y, (float)(var32.x + 1), (float)var32.y);
                     }

                     if (Vector2.dot((float)(var32.x + 1) - this.px, (float)var32.y + 0.5F - this.py, 1.0F, 0.0F) < 0.0F) {
                        this.addPolygonForLineSegment(this.px, this.py, (float)(var32.x + 1), (float)var32.y, (float)(var32.x + 1), (float)(var32.y + 1));
                     }

                     if (Vector2.dot((float)var32.x + 0.5F - this.px, (float)(var32.y + 1) - this.py, 0.0F, 1.0F) < 0.0F) {
                        this.addPolygonForLineSegment(this.px, this.py, (float)(var32.x + 1), (float)(var32.y + 1), (float)var32.x, (float)(var32.y + 1));
                     }

                     if (Vector2.dot((float)var32.x - this.px, (float)var32.y + 0.5F - this.py, -1.0F, 0.0F) < 0.0F) {
                        this.addPolygonForLineSegment(this.px, this.py, (float)var32.x, (float)(var32.y + 1), (float)var32.x, (float)var32.y);
                     }
                  }

                  GameProfiler.getInstance().end(var28);
               }
            }
         }
      }

      boolean isCollinear(float var1, float var2, float var3, float var4, float var5, float var6) {
         float var7 = (var3 - var1) * (var6 - var2) - (var5 - var1) * (var4 - var2);
         return var7 >= -0.05F && var7 < 0.05F;
      }

      float getDotWithLookVector(float var1, float var2) {
         Vector2 var3 = VisibilityPolygon2.L_calculateVisibilityPolygon.m_tempVector2_1.set(var1 - this.px, var2 - this.py);
         var3.normalize();
         return var3.dot(this.lookVector);
      }

      boolean lineSegmentsIntersects(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
         float var9 = 1500.0F;
         float var10 = var1 - var5;
         float var11 = var2 - var6;
         float var12 = var7 - var5;
         float var13 = var8 - var6;
         float var14 = 1.0F / (var13 * var3 - var12 * var4);
         float var15 = (var12 * var11 - var13 * var10) * var14;
         if (var15 >= 0.0F && var15 <= var9) {
            float var16 = (var11 * var3 - var10 * var4) * var14;
            if (var16 >= 0.0F && var16 <= 1.0F) {
               return true;
            }
         }

         return false;
      }

      boolean isInViewCone(float var1, float var2, float var3, float var4) {
         if (!VisibilityPolygon2.instance.USE_CIRCLE || !(IsoUtils.DistanceToSquared(var1, var2, this.px, this.py) >= this.CIRCLE_RADIUS * this.CIRCLE_RADIUS) && !(IsoUtils.DistanceToSquared(var3, var4, this.px, this.py) >= this.CIRCLE_RADIUS * this.CIRCLE_RADIUS)) {
            float var5 = this.getDotWithLookVector(var1, var2);
            float var6 = this.getDotWithLookVector(var3, var4);
            float var7 = this.getDotWithLookVector(var1 + (var3 - var1) * 0.5F, var2 + (var4 - var2) * 0.5F);
            if (Float.compare(var5, -this.visionCone) < 0 && Float.compare(var6, -this.visionCone) < 0 && Float.compare(var7, -this.visionCone) < 0) {
               return this.lineSegmentsIntersects(this.px, this.py, this.dirX1, this.dirY1, var1, var2, var3, var4) || this.lineSegmentsIntersects(this.px, this.py, this.dirX2, this.dirY2, var1, var2, var3, var4);
            } else {
               return true;
            }
         } else {
            return false;
         }
      }

      boolean isInViewCone(IsoGridSquare var1) {
         return this.isInViewCone((float)var1.x, (float)var1.y, (float)(var1.x + 1), (float)var1.y) || this.isInViewCone((float)(var1.x + 1), (float)var1.y, (float)(var1.x + 1), (float)(var1.y + 1)) || this.isInViewCone((float)(var1.x + 1), (float)(var1.y + 1), (float)var1.x, (float)(var1.y + 1)) || this.isInViewCone((float)var1.x, (float)(var1.y + 1), (float)var1.x, (float)var1.y);
      }

      void addPolygonForLineSegment(float var1, float var2, float var3, float var4, float var5, float var6) {
         this.addPolygonForLineSegment(var1, var2, var3, var4, var5, var6, false);
      }

      void addPolygonForLineSegment(float var1, float var2, float var3, float var4, float var5, float var6, boolean var7) {
         if (Core.bDebug) {
         }

         boolean var8 = false;
         float var9 = Vector2.getDirection(var3 - var1, var4 - var2) * 57.295776F;
         float var10 = Vector2.getDirection(var5 - var1, var6 - var2) * 57.295776F;
         if (!var7 && this.isCollinear(var1, var2, var3, var4, var5, var6) && Math.abs(var9 - var10) < 10.0F) {
            if (var8) {
               LineDrawer.addLine(var3, var4, (float)this.pz, var5, var6, (float)this.pz, 1.0F, 1.0F, 0.0F, 1.0F);
            }

            Vector2 var28 = VisibilityPolygon2.L_calculateVisibilityPolygon.m_tempVector2_1.set(var5 - var3, var6 - var4);
            var28.rotate(1.5707964F);
            var28.normalize();
            float var29 = 0.025F;
            if (IsoUtils.DistanceToSquared(var1, var2, var3, var4) < IsoUtils.DistanceToSquared(var1, var2, var5, var6)) {
               this.addPolygonForLineSegment(var1, var2, var3 - var28.x * var29, var4 - var28.y * var29, var3 + var28.x * var29, var4 + var28.y * var29, true);
            } else {
               this.addPolygonForLineSegment(var1, var2, var5 - var28.x * var29, var6 - var28.y * var29, var5 + var28.x * var29, var6 + var28.y * var29, true);
            }

         } else {
            Segment var11 = (Segment)VisibilityPolygon2.L_calculateVisibilityPolygon.m_segmentPool.alloc();
            var11.a.set(var3, var4, var3 - var1, var4 - var2, (Vector2.getDirection(var3 - var1, var4 - var2) + 3.1415927F) * 57.295776F, IsoUtils.DistanceToSquared(var3, var4, var1, var2));
            var11.b.set(var5, var6, var5 - var1, var6 - var2, (Vector2.getDirection(var5 - var1, var6 - var2) + 3.1415927F) * 57.295776F, IsoUtils.DistanceToSquared(var5, var6, var1, var2));
            int var12 = PZMath.fastfloor(var11.minAngle() / 45.0F);
            int var13 = PZMath.fastfloor(var11.maxAngle() / 45.0F);
            if (var11.maxAngle() - var11.minAngle() > 180.0F) {
               var12 = PZMath.fastfloor(var11.maxAngle() / 45.0F);
               var13 = PZMath.fastfloor((var11.minAngle() + 360.0F) / 45.0F);
            }

            boolean var14 = true;

            for(int var15 = var12; var15 <= var13; ++var15) {
               int var16 = var15 % 8;
               if (!this.partitions[var16].addSegment(var11)) {
                  --var15;

                  while(var15 >= var12) {
                     var16 = var15 % 8;
                     this.partitions[var16].segments.remove(var11);
                     --var15;
                  }

                  var14 = false;
                  break;
               }
            }

            if (!var14) {
               if (var8) {
                  LineDrawer.addLine(var11.a.x, var11.a.y, (float)this.pz, var11.b.x, var11.b.y, (float)this.pz, 1.0F, 0.0F, 0.0F, 1.0F);
               }

               VisibilityPolygon2.L_calculateVisibilityPolygon.m_segmentPool.release((Object)var11);
            } else {
               if (var8) {
                  LineDrawer.addLine(var11.a.x, var11.a.y, (float)this.pz, var11.b.x, var11.b.y, (float)this.pz, 1.0F, 1.0F, 1.0F, 1.0F);
               }

               float var30 = 1500.0F;
               float var31 = var3 + var11.a.dirX * var30;
               float var17 = var4 + var11.a.dirY * var30;
               float var18 = var5 + var11.b.dirX * var30;
               float var19 = var6 + var11.b.dirY * var30;
               IsoChunkMap var20 = IsoWorld.instance.CurrentCell.getChunkMap(this.playerIndex);
               int var21 = var20.getWorldXMinTiles();
               int var22 = var20.getWorldYMinTiles();
               int var23 = var20.getWorldXMaxTiles();
               int var24 = var20.getWorldYMaxTiles();
               Vector2 var25 = VisibilityPolygon2.L_calculateVisibilityPolygon.m_tempVector2_1;
               if (VisibilityPolygon2.instance.USE_CIRCLE) {
                  intersectLineSegmentWithCircle(var31, var17, var1, var2, this.CIRCLE_RADIUS, var25);
                  var31 = var25.x;
                  var17 = var25.y;
                  intersectLineSegmentWithCircle(var18, var19, var1, var2, this.CIRCLE_RADIUS, var25);
                  var18 = var25.x;
                  var19 = var25.y;
               } else {
                  if (intersectLineSegmentWithAABB(var3, var4, var31, var17, (float)var21, (float)var22, (float)var23, (float)var24, var25)) {
                     var31 = var25.x;
                     var17 = var25.y;
                  }

                  if (intersectLineSegmentWithAABB(var5, var6, var18, var19, (float)var21, (float)var22, (float)var23, (float)var24, var25)) {
                     var18 = var25.x;
                     var19 = var25.y;
                  }
               }

               float var26 = 0.0F;
               var26 += (var5 - var3) * (var6 + var4);
               var26 += (var18 - var5) * (var19 + var6);
               var26 += (var31 - var18) * (var17 + var19);
               var26 += (var3 - var31) * (var4 + var17);
               boolean var27 = var26 > 0.0F;
               if (var27) {
                  this.shadows.add(var5);
                  this.shadows.add(var6);
                  this.shadows.add(var3);
                  this.shadows.add(var4);
                  this.shadows.add(var31);
                  this.shadows.add(var17);
                  this.shadows.add(var18);
                  this.shadows.add(var19);
               } else {
                  this.shadows.add(var3);
                  this.shadows.add(var4);
                  this.shadows.add(var5);
                  this.shadows.add(var6);
                  this.shadows.add(var18);
                  this.shadows.add(var19);
                  this.shadows.add(var31);
                  this.shadows.add(var17);
               }

               if (var8) {
                  LineDrawer.addLine(var1, var2, (float)this.pz, var31, var17, (float)this.pz, 1.0F, 1.0F, 1.0F, 0.5F);
               }

               if (var8) {
                  LineDrawer.addLine(var1, var2, (float)this.pz, var18, var19, (float)this.pz, 1.0F, 1.0F, 1.0F, 0.5F);
               }

            }
         }
      }

      static boolean intersectLineSegments(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, Vector2 var8) {
         float var9 = (var7 - var5) * (var2 - var0) - (var6 - var4) * (var3 - var1);
         if (var9 == 0.0F) {
            return false;
         } else {
            float var10 = var1 - var5;
            float var11 = var0 - var4;
            float var12 = ((var6 - var4) * var10 - (var7 - var5) * var11) / var9;
            if (!(var12 < 0.0F) && !(var12 > 1.0F)) {
               float var13 = ((var2 - var0) * var10 - (var3 - var1) * var11) / var9;
               if (!(var13 < 0.0F) && !(var13 > 1.0F)) {
                  if (var8 != null) {
                     var8.set(var0 + (var2 - var0) * var12, var1 + (var3 - var1) * var12);
                  }

                  return true;
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      }

      static boolean intersectLineSegmentWithAABB(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, Vector2 var8) {
         if (intersectLineSegments(var0, var1, var2, var3, var4, var5, var6, var5, var8)) {
            return true;
         } else if (intersectLineSegments(var0, var1, var2, var3, var6, var5, var6, var7, var8)) {
            return true;
         } else if (intersectLineSegments(var0, var1, var2, var3, var6, var7, var4, var7, var8)) {
            return true;
         } else {
            return intersectLineSegments(var0, var1, var2, var3, var4, var7, var4, var5, var8);
         }
      }

      static int intersectLineSegmentWithAABBEdge(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, Vector2 var8) {
         if (intersectLineSegments(var0, var1, var2, var3, var4, var5, var6, var5, var8)) {
            return 1;
         } else if (intersectLineSegments(var0, var1, var2, var3, var6, var5, var6, var7, var8)) {
            return 2;
         } else if (intersectLineSegments(var0, var1, var2, var3, var6, var7, var4, var7, var8)) {
            return 3;
         } else {
            return intersectLineSegments(var0, var1, var2, var3, var4, var7, var4, var5, var8) ? 4 : 0;
         }
      }

      static void intersectLineSegmentWithCircle(float var0, float var1, float var2, float var3, float var4, Vector2 var5) {
         var5.set(var0 - var2, var1 - var3);
         var5.setLength(var4);
         var5.x += var2;
         var5.y += var3;
      }

      private void initBlur() {
         int var1 = Core.width / downscale;
         int var2 = Core.height / downscale;
         if (blurTex == null || blurTex.getWidth() != var1 || blurTex.getHeight() != var2) {
            if (blurTex != null) {
               blurTex.destroy();
            }

            if (blurDepthTex != null) {
               blurDepthTex.destroy();
            }

            blurTex = new Texture(var1, var2, 16, 6403, 6403);
            blurTex.setNameOnly("visBlur");
            blurDepthTex = new Texture(var1, var2, 512);
            blurDepthTex.setNameOnly("visBlurDepth");
            if (blurFBO == null) {
               blurFBO = new TextureFBO(blurTex, blurDepthTex, false);
            } else {
               blurFBO.startDrawing(false, false);
               blurFBO.attach(blurTex, 36064);
               blurFBO.attach(blurDepthTex, 36096);
               blurFBO.endDrawing();
            }
         }

         if (blurShader == null) {
            blurShader = new Shader("visibilityBlur");
         }

         if (polygonShader == null) {
            polygonShader = new Shader("visPolygon");
         }

         if (blitShader == null) {
            blitShader = new Shader("blitSimple");
         }

         this.polygonData.init();
      }

      private void addFullScreenShadow() {
         IsoChunkMap var1 = IsoWorld.instance.CurrentCell.getChunkMap(this.playerIndex);
         float var2 = (float)var1.getWorldXMinTiles();
         float var3 = (float)var1.getWorldYMinTiles();
         float var4 = (float)var1.getWorldXMaxTiles();
         float var5 = (float)var1.getWorldYMaxTiles();
         this.shadowVerts.add(var2);
         this.shadowVerts.add(var3);
         this.shadowVerts.add(0.0F);
         this.shadowVerts.add(var4);
         this.shadowVerts.add(var3);
         this.shadowVerts.add(0.0F);
         this.shadowVerts.add(var4);
         this.shadowVerts.add(var5);
         this.shadowVerts.add(0.0F);
         this.shadowVerts.add(var2);
         this.shadowVerts.add(var3);
         this.shadowVerts.add(0.0F);
         this.shadowVerts.add(var4);
         this.shadowVerts.add(var5);
         this.shadowVerts.add(0.0F);
         this.shadowVerts.add(var2);
         this.shadowVerts.add(var5);
         this.shadowVerts.add(0.0F);
      }

      private float getLen(float var1, float var2, float var3, float var4) {
         float var5 = var3 - var1;
         float var6 = var4 - var2;
         return PZMath.sqrt(var5 * var5 + var6 * var6);
      }

      private void addViewShadow() {
         IsoChunkMap var1 = IsoWorld.instance.CurrentCell.getChunkMap(this.playerIndex);
         int var2 = var1.getWorldXMinTiles();
         int var3 = var1.getWorldYMinTiles();
         int var4 = var1.getWorldXMaxTiles();
         int var5 = var1.getWorldYMaxTiles();
         float var6 = (float)Math.acos((double)(-this.visionCone));
         float var7 = this.lookAngleRadians - var6;
         float var8 = this.lookAngleRadians + var6;
         float var9 = (float)Math.cos((double)var7);
         float var10 = (float)Math.sin((double)var7);
         float var11 = (float)Math.cos((double)var8);
         float var12 = (float)Math.sin((double)var8);
         float var13 = this.px + var9 * 1500.0F;
         float var14 = this.py + var10 * 1500.0F;
         float var15 = this.px + var11 * 1500.0F;
         float var16 = this.py + var12 * 1500.0F;
         intersectLineSegmentWithAABBEdge(this.px, this.py, var15, var16, (float)var2, (float)var3, (float)var4, (float)var5, edge);
         var15 = edge.x;
         var16 = edge.y;
         intersectLineSegmentWithAABBEdge(this.px, this.py, var13, var14, (float)var2, (float)var3, (float)var4, (float)var5, edge);
         var13 = edge.x;
         var14 = edge.y;
         float var17 = Vector2.getDirection(var13 - this.px, var14 - this.py);
         float var18 = Vector2.getDirection(var15 - this.px, var16 - this.py);
         float var19 = Vector2.getDirection((float)var2 - this.px, (float)var3 - this.py);
         float var20 = Vector2.getDirection((float)var4 - this.px, (float)var3 - this.py);
         float var21 = Vector2.getDirection((float)var4 - this.px, (float)var5 - this.py);
         float var22 = Vector2.getDirection((float)var2 - this.px, (float)var5 - this.py);
         angles[0].set((float)var2, (float)var3, var19);
         angles[1].set((float)var4, (float)var3, var20);
         angles[2].set((float)var4, (float)var5, var21);
         angles[3].set((float)var2, (float)var5, var22);
         this.shadowVerts.add(this.px);
         this.shadowVerts.add(this.py);
         this.shadowVerts.add(0.0F);
         this.shadowVerts.add(var15);
         this.shadowVerts.add(var16);
         this.shadowVerts.add(this.getLen(this.px, this.py, var15, var16) * shadowBlurRamp);
         int var23;
         float var24;
         if (var17 > var18) {
            for(var23 = 0; var23 < 4; ++var23) {
               if (angles[var23].z > var18 && angles[var23].z < var17) {
                  var24 = this.getLen(this.px, this.py, angles[var23].x, angles[var23].y) * shadowBlurRamp;
                  this.shadowVerts.add(angles[var23].x);
                  this.shadowVerts.add(angles[var23].y);
                  this.shadowVerts.add(var24);
                  this.shadowVerts.add(this.px);
                  this.shadowVerts.add(this.py);
                  this.shadowVerts.add(0.0F);
                  this.shadowVerts.add(angles[var23].x);
                  this.shadowVerts.add(angles[var23].y);
                  this.shadowVerts.add(var24);
               }
            }
         } else {
            for(var23 = 0; var23 < 4; ++var23) {
               if (angles[var23].z > var18) {
                  var24 = this.getLen(this.px, this.py, angles[var23].x, angles[var23].y) * shadowBlurRamp;
                  this.shadowVerts.add(angles[var23].x);
                  this.shadowVerts.add(angles[var23].y);
                  this.shadowVerts.add(var24);
                  this.shadowVerts.add(this.px);
                  this.shadowVerts.add(this.py);
                  this.shadowVerts.add(0.0F);
                  this.shadowVerts.add(angles[var23].x);
                  this.shadowVerts.add(angles[var23].y);
                  this.shadowVerts.add(var24);
               }
            }

            for(var23 = 0; var23 < 4; ++var23) {
               if (angles[var23].z < var17) {
                  var24 = this.getLen(this.px, this.py, angles[var23].x, angles[var23].y) * shadowBlurRamp;
                  this.shadowVerts.add(angles[var23].x);
                  this.shadowVerts.add(angles[var23].y);
                  this.shadowVerts.add(var24);
                  this.shadowVerts.add(this.px);
                  this.shadowVerts.add(this.py);
                  this.shadowVerts.add(0.0F);
                  this.shadowVerts.add(angles[var23].x);
                  this.shadowVerts.add(angles[var23].y);
                  this.shadowVerts.add(var24);
               }
            }
         }

         this.shadowVerts.add(var13);
         this.shadowVerts.add(var14);
         this.shadowVerts.add(this.getLen(this.px, this.py, var13, var14) * shadowBlurRamp);
      }

      private void addLineShadow(float var1, float var2, float var3, float var4) {
         float var5 = var1 - this.px;
         float var6 = var2 - this.py;
         float var7 = var3 - this.px;
         float var8 = var4 - this.py;
         float var9 = var5 * var5 + var6 * var6;
         float var10 = PZMath.sqrt(PZMath.max(0.01F, var9));
         float var11 = var7 * var7 + var8 * var8;
         float var12 = PZMath.sqrt(PZMath.max(0.01F, var11));
         float var13 = var5 / var10;
         float var14 = var6 / var10;
         float var15 = var7 / var12;
         float var16 = var8 / var12;
         float var17 = 70.0F;
         float var18 = var17 * shadowBlurRamp;
         this.shadowVerts.add(var1);
         this.shadowVerts.add(var2);
         this.shadowVerts.add(0.0F);
         this.shadowVerts.add(var3);
         this.shadowVerts.add(var4);
         this.shadowVerts.add(0.0F);
         this.shadowVerts.add(var1 + var13 * var17);
         this.shadowVerts.add(var2 + var14 * var17);
         this.shadowVerts.add(var18);
         this.shadowVerts.add(var3);
         this.shadowVerts.add(var4);
         this.shadowVerts.add(0.0F);
         this.shadowVerts.add(var1 + var13 * var17);
         this.shadowVerts.add(var2 + var14 * var17);
         this.shadowVerts.add(var18);
         this.shadowVerts.add(var3 + var15 * var17);
         this.shadowVerts.add(var4 + var16 * var17);
         this.shadowVerts.add(var18);
      }

      private void addWallShadow(VisibilityWall var1) {
         this.addLineShadow((float)var1.x1, (float)var1.y1, (float)var1.x2, (float)var1.y2);
      }

      private void addSquareShadow(IsoGridSquare var1) {
         if ((float)var1.x < this.px) {
            this.addLineShadow((float)(var1.x + 1), (float)var1.y, (float)(var1.x + 1), (float)(var1.y + 1));
         } else {
            this.addLineShadow((float)var1.x, (float)var1.y, (float)var1.x, (float)(var1.y + 1));
         }

         if ((float)var1.y < this.py) {
            this.addLineShadow((float)var1.x, (float)(var1.y + 1), (float)(var1.x + 1), (float)(var1.y + 1));
         } else {
            this.addLineShadow((float)var1.x, (float)var1.y, (float)(var1.x + 1), (float)var1.y);
         }

      }

      public void render() {
         if (DebugOptions.instance.UseNewVisibility.getValue()) {
            GameProfiler.getInstance().invokeAndMeasure("renderNew", this::renderNew);
         } else {
            this.renderOld();
         }

      }

      public void renderNew() {
         this.initBlur();
         this.bClipperPolygonsDirty = false;
         this.polygonData.camera = SpriteRenderer.instance.getRenderingPlayerCamera(this.playerIndex);
         GL11.glDepthFunc(515);
         GL11.glDepthMask(true);
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glDisable(2884);
         GL11.glDisable(3008);
         this.renderPolygons();
         GL11.glDepthMask(false);
         if (!DebugOptions.instance.DisplayVisibilityPolygon.getValue()) {
            this.renderToScreen();
         }

         GL11.glEnable(3008);
         GL11.glDepthFunc(519);
         GLStateRenderThread.restore();
         int var1 = IsoCamera.getScreenLeft(this.playerIndex);
         int var2 = IsoCamera.getScreenTop(this.playerIndex);
         int var3 = IsoCamera.getScreenWidth(this.playerIndex);
         int var4 = IsoCamera.getScreenHeight(this.playerIndex);
         GL11.glViewport(var1, var2, var3, var4);
      }

      public void renderOld() {
         Clipper var1 = VisibilityPolygon2.L_render.m_clipper;
         ByteBuffer var2 = VisibilityPolygon2.L_render.clipperBuf;
         int var9;
         float var16;
         float var17;
         float var18;
         if (!this.clipperPolygons.isEmpty() && !this.bClipperPolygonsDirty) {
            this.releaseClipperPolygons(VisibilityPolygon2.L_render.clipperPolygons1);

            for(int var38 = 0; var38 < this.clipperPolygons.size(); ++var38) {
               ClipperPolygon var40 = (ClipperPolygon)this.clipperPolygons.get(var38);
               ClipperPolygon var42 = var40.makeCopy(VisibilityPolygon2.L_render.clipperPolygonPool, VisibilityPolygon2.L_render.floatArrayListPool);
               VisibilityPolygon2.L_render.clipperPolygons1.add(var42);
            }
         } else {
            GameProfiler.ProfileArea var3 = GameProfiler.getInstance().startIfEnabled("Update Dirty");
            this.releaseClipperPolygons(this.clipperPolygons);
            IsoChunkMap var4 = IsoWorld.instance.CurrentCell.getChunkMap(this.playerIndex);
            int var5 = var4.getWorldXMinTiles();
            int var6 = var4.getWorldYMinTiles();
            int var7 = var4.getWorldXMaxTiles();
            int var8 = var4.getWorldYMaxTiles();
            VisibilityPolygon2.L_addShadowToClipper.angle1cm = Vector2.getDirection((float)var5 - this.px, (float)var6 - this.py) + 3.1415927F;
            VisibilityPolygon2.L_addShadowToClipper.angle2cm = Vector2.getDirection((float)var5 - this.px, (float)var8 - this.py) + 3.1415927F;
            VisibilityPolygon2.L_addShadowToClipper.angle3cm = Vector2.getDirection((float)var7 - this.px, (float)var8 - this.py) + 3.1415927F;
            VisibilityPolygon2.L_addShadowToClipper.angle4cm = Vector2.getDirection((float)var7 - this.px, (float)var6 - this.py) + 3.1415927F;
            var1.clear();
            int var10;
            if (this.bInsideTree) {
               this.addShadowToClipper(var1, var2, (float)var5, (float)var6, (float)var7, (float)var6, (float)var7, (float)var8, (float)var5, (float)var8);
            } else {
               this.addViewConeToClipper(var1, var2);

               for(var9 = 0; var9 < this.shadows.size(); var9 += 8) {
                  var10 = var9 + 1;
                  float var11 = this.shadows.getQuick(var9);
                  float var12 = this.shadows.getQuick(var10++);
                  float var13 = this.shadows.getQuick(var10++);
                  float var14 = this.shadows.getQuick(var10++);
                  float var15 = this.shadows.getQuick(var10++);
                  var16 = this.shadows.getQuick(var10++);
                  var17 = this.shadows.getQuick(var10++);
                  var18 = this.shadows.getQuick(var10);
                  this.addShadowToClipper(var1, var2, var11, var12, var13, var14, var15, var16, var17, var18);
               }
            }

            var9 = var1.generatePolygons();
            this.releaseClipperPolygons(VisibilityPolygon2.L_render.clipperPolygons1);
            this.getClipperPolygons(var1, var9, VisibilityPolygon2.L_render.clipperPolygons1);

            for(var10 = 0; var10 < VisibilityPolygon2.L_render.clipperPolygons1.size(); ++var10) {
               ClipperPolygon var47 = (ClipperPolygon)VisibilityPolygon2.L_render.clipperPolygons1.get(var10);
               ClipperPolygon var50 = var47.makeCopy(VisibilityPolygon2.L_render.clipperPolygonPool, VisibilityPolygon2.L_render.floatArrayListPool);
               this.clipperPolygons.add(var50);
            }

            this.bClipperPolygonsDirty = false;
            GameProfiler.getInstance().end(var3);
         }

         PlayerCamera var39 = SpriteRenderer.instance.getRenderingPlayerCamera(this.playerIndex);
         VBORenderer var41 = VBORenderer.getInstance();
         var41.startRun(var41.FORMAT_PositionColorUVDepth);
         boolean var43 = true;
         var41.setMode(var43 ? 4 : 1);
         var41.setDepthTest(var43);
         GL11.glDepthFunc(515);
         GL11.glDepthMask(false);
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         if (DebugOptions.instance.DisplayVisibilityPolygon.getValue()) {
            GL11.glPolygonMode(1032, 6913);
         }

         var41.setTextureID(Texture.getWhite().getTextureId());
         byte var44 = 5;
         ArrayList var45 = VisibilityPolygon2.L_render.clipperPolygons3;
         float var46 = 0.0125F;
         VisibilityPolygon2.L_render.triangleIndex = 0;

         for(var9 = 0; var9 < var44; ++var9) {
            boolean var52 = true;
            int var51;
            int var56;
            int var57;
            if (var52) {
               GameProfiler.ProfileArea var49 = GameProfiler.getInstance().startIfEnabled("Blur");
               ClipperPolygon var55;
               if (var9 == var44 - 1) {
                  for(var51 = 0; var51 < VisibilityPolygon2.L_render.clipperPolygons1.size(); ++var51) {
                     var55 = (ClipperPolygon)VisibilityPolygon2.L_render.clipperPolygons1.get(var51);
                     var1.clear();
                     this.addClipperPolygon(var1, var2, var55, false);
                     var56 = var1.generatePolygons();
                     this.triangulatePolygons(var56, var1, var2, var39, var46 * (float)(var9 + 1));
                  }
               } else {
                  var45.clear();

                  for(var51 = 0; var51 < VisibilityPolygon2.L_render.clipperPolygons1.size(); ++var51) {
                     var55 = (ClipperPolygon)VisibilityPolygon2.L_render.clipperPolygons1.get(var51);
                     var1.clear();
                     this.addClipperPolygon(var1, var2, var55, false);
                     var56 = var1.generatePolygons(-0.35 / (double)var44, 2);
                     this.releaseClipperPolygons(VisibilityPolygon2.L_render.clipperPolygons2);
                     this.getClipperPolygons(var1, var56, VisibilityPolygon2.L_render.clipperPolygons2);
                     var1.clear();
                     this.addClipperPolygon(var1, var2, var55, false);
                     this.addClipperPolygons(var1, var2, VisibilityPolygon2.L_render.clipperPolygons2, true);
                     var57 = var1.generatePolygons();
                     this.triangulatePolygons(var57, var1, var2, var39, var46 * (float)(var9 + 1));
                     var45.addAll(VisibilityPolygon2.L_render.clipperPolygons2);
                     VisibilityPolygon2.L_render.clipperPolygons2.clear();
                  }

                  this.releaseClipperPolygons(VisibilityPolygon2.L_render.clipperPolygons1);
                  VisibilityPolygon2.L_render.clipperPolygons1.addAll(var45);
               }

               GameProfiler.getInstance().end(var49);
            } else {
               int var48 = var1.generatePolygons((double)(-(var9 + 1)) * 0.25 / (double)var44, 2);

               for(var51 = 0; var51 < var48; ++var51) {
                  var2.clear();
                  float var19;
                  float var20;
                  float var21;
                  float var22;
                  float var23;
                  float var24;
                  float var25;
                  if (var43) {
                     GameProfiler.ProfileArea var54 = GameProfiler.getInstance().startIfEnabled("Triangulate");
                     var56 = var1.triangulate(var51, var2);

                     for(var57 = 0; var57 < var56; var57 += 3) {
                        var16 = var2.getFloat();
                        var17 = var2.getFloat();
                        var18 = var2.getFloat();
                        var19 = var2.getFloat();
                        var20 = var2.getFloat();
                        var21 = var2.getFloat();
                        var22 = var39.XToScreenExact(var16, var17, (float)this.pz, 0);
                        var23 = var39.YToScreenExact(var16, var17, (float)this.pz, 0);
                        var24 = var39.XToScreenExact(var18, var19, (float)this.pz, 0);
                        var25 = var39.YToScreenExact(var18, var19, (float)this.pz, 0);
                        float var26 = var39.XToScreenExact(var20, var21, (float)this.pz, 0);
                        float var27 = var39.YToScreenExact(var20, var21, (float)this.pz, 0);
                        float var28 = 0.0F;
                        float var29 = 0.0F;
                        float var30 = -1.0E-4F;
                        float var31 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(this.px), PZMath.fastfloor(this.py), var16, var17, (float)this.pz).depthStart + var30;
                        float var32 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(this.px), PZMath.fastfloor(this.py), var18, var19, (float)this.pz).depthStart + var30;
                        float var33 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(this.px), PZMath.fastfloor(this.py), var20, var21, (float)this.pz).depthStart + var30;
                        float var34 = 0.0F;
                        float var35 = 0.0F;
                        float var36 = 0.0F;
                        float var37 = 0.066F / (float)var44 * 4.0F;
                        if (Core.bDebug) {
                        }

                        var41.addTriangleDepth(var22, var23, 0.0F, var28, var29, var31, var24, var25, 0.0F, var28 + 0.5F, var29 + 0.5F, var32, var26, var27, 0.0F, var28 + 1.0F, var29 + 1.0F, var33, var34, var35, var36, var37);
                     }

                     GameProfiler.getInstance().end(var54);
                  } else {
                     var1.getPolygon(var51, var2);
                     short var53 = var2.getShort();
                     if (var53 >= 3) {
                        var56 = var2.position();

                        for(var57 = 0; var57 < var53; ++var57) {
                           var16 = var2.getFloat();
                           var17 = var2.getFloat();
                           var18 = var2.getFloat(var56 + (var57 + 1) % var53 * 2 * 4);
                           var19 = var2.getFloat(var56 + (var57 + 1) % var53 * 2 * 4 + 4);
                           var20 = var39.XToScreenExact(var16, var17, (float)this.pz, 0);
                           var21 = var39.YToScreenExact(var16, var17, (float)this.pz, 0);
                           var22 = var39.XToScreenExact(var18, var19, (float)this.pz, 0);
                           var23 = var39.YToScreenExact(var18, var19, (float)this.pz, 0);
                           var41.addLine(var20, var21, 0.0F, var22, var23, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
                        }

                        short var58 = var2.getShort();

                        for(int var59 = 0; var59 < var58; ++var59) {
                           var53 = var2.getShort();
                           if (var53 >= 3) {
                              var56 = var2.position();

                              for(int var60 = 0; var60 < var53; ++var60) {
                                 var18 = var2.getFloat();
                                 var19 = var2.getFloat();
                                 var20 = var2.getFloat(var56 + (var60 + 1) % var53 * 2 * 4);
                                 var21 = var2.getFloat(var56 + (var60 + 1) % var53 * 2 * 4 + 4);
                                 var22 = var39.XToScreenExact(var18, var19, (float)this.pz, 0);
                                 var23 = var39.YToScreenExact(var18, var19, (float)this.pz, 0);
                                 var24 = var39.XToScreenExact(var20, var21, (float)this.pz, 0);
                                 var25 = var39.YToScreenExact(var20, var21, (float)this.pz, 0);
                                 var41.addLine(var22, var23, 0.0F, var24, var25, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         var41.endRun();
         var41.flush();
         GL11.glDepthFunc(519);
         GL11.glPolygonMode(1032, 6914);
         GLStateRenderThread.restore();
      }

      private void updatePolygons(int var1, int var2) {
         this.polygonData.reset();
         float[] var3 = this.shadowVerts.array();

         for(int var4 = var1; var4 < var2; var4 += 3) {
            float var5 = var3[var4];
            float var6 = var3[var4 + 1];
            float var7 = var3[var4 + 2];
            this.polygonData.addVertex(var5, var6, var7);
         }

         this.polygonData.data.flip();
      }

      private void renderPolygons() {
         GL11.glPushClientAttrib(2);
         if (DebugOptions.instance.DisplayVisibilityPolygon.getValue()) {
            GL11.glPolygonMode(1032, 6913);
         }

         polygonShader.Start();
         if (!DebugOptions.instance.DisplayVisibilityPolygon.getValue()) {
            GL11.glBlendFunc(1, 0);
            GL11.glViewport(0, 0, blurFBO.getWidth(), blurFBO.getHeight());
            blurFBO.startDrawing(true, true);
         }

         VertexBufferObject.setModelViewProjection(polygonShader.getProgram());
         Objects.requireNonNull(this.polygonData);
         int var1 = 3072 * 3;

         for(int var2 = 0; var2 < this.shadowVerts.size(); var2 += var1) {
            int var3 = PZMath.min(var1, this.shadowVerts.size() - var2);
            this.updatePolygons(var2, var2 + var3);
            this.polygonData.draw();
         }

         if (!DebugOptions.instance.DisplayVisibilityPolygon.getValue()) {
            blurFBO.endDrawing();
            GL11.glViewport(0, 0, Core.width, Core.height);
            GL11.glBlendFunc(770, 771);
         }

         polygonShader.End();
         if (DebugOptions.instance.DisplayVisibilityPolygon.getValue()) {
            GL11.glPolygonMode(1032, 6914);
         }

         GL11.glPopClientAttrib();
      }

      private void renderToScreen() {
         if (DebugOptions.instance.PreviewTiles.getValue()) {
            GL11.glDepthFunc(519);
            blitShader.Start();
            blitShader.getProgram().setValue("Tex", blurTex, 0);
            RenderTarget.DrawFullScreenQuad();
            blitShader.End();
         } else {
            GameProfiler.ProfileArea var1 = GameProfiler.getInstance().startIfEnabled("BlurShader");
            TextureFBO var2 = Core.getInstance().getOffscreenBuffer(this.playerIndex);
            int var3 = IsoPlayer.numPlayers > 1 ? 2 : 1;
            int var4 = IsoPlayer.numPlayers > 2 ? 2 : 1;
            int var5 = var2.getWidth() / var3;
            int var6 = var2.getHeight() / var4;
            int var7 = IsoCamera.getScreenLeft(this.playerIndex);
            int var8 = IsoCamera.getScreenTop(this.playerIndex);
            int var9 = IsoCamera.getScreenWidth(this.playerIndex);
            int var10 = IsoCamera.getScreenHeight(this.playerIndex);
            GL11.glViewport(var7, var8, var9, var10);
            blurShader.Start();
            VisibilityPolygon2.L_render.vector2.set((float)var9, (float)var10);
            blurShader.getProgram().setValue("screenSize", VisibilityPolygon2.L_render.vector2);
            VisibilityPolygon2.L_render.vector2.set((float)var7, (float)var8);
            blurShader.getProgram().setValue("displayOrigin", VisibilityPolygon2.L_render.vector2);
            VisibilityPolygon2.L_render.vector2.set((float)var5, (float)var6);
            blurShader.getProgram().setValue("displaySize", VisibilityPolygon2.L_render.vector2);
            blurShader.getProgram().setValue("tex", blurTex, 0);
            blurShader.getProgram().setValue("depth", blurDepthTex, 1);
            blurShader.getProgram().setValue("zoom", this.zoom);
            GameProfiler.getInstance().invokeAndMeasure("Render Quad", RenderTarget::DrawFullScreenQuad);
            blurShader.End();
            GameProfiler.getInstance().end(var1);
         }

      }

      void addViewConeToClipper(Clipper var1, ByteBuffer var2) {
         IsoChunkMap var3 = IsoWorld.instance.CurrentCell.getChunkMap(this.playerIndex);
         int var4 = var3.getWorldXMinTiles();
         int var5 = var3.getWorldYMinTiles();
         int var6 = var3.getWorldXMaxTiles();
         int var7 = var3.getWorldYMaxTiles();
         float var8 = -this.visionCone;
         float var9 = this.lookAngleRadians;
         float var10 = var9 + (float)Math.acos((double)var8);
         float var11 = var9 - (float)Math.acos((double)var8);
         float var12 = this.px + (float)Math.cos((double)var11) * 1500.0F;
         float var13 = this.py + (float)Math.sin((double)var11) * 1500.0F;
         float var14 = this.px + (float)Math.cos((double)var10) * 1500.0F;
         float var15 = this.py + (float)Math.sin((double)var10) * 1500.0F;
         Vector2 var16 = VisibilityPolygon2.L_render.vector2;
         if (VisibilityPolygon2.instance.USE_CIRCLE) {
            intersectLineSegmentWithCircle(var12, var13, this.px, this.py, this.CIRCLE_RADIUS, var16);
            var12 = var16.x;
            var13 = var16.y;
            intersectLineSegmentWithCircle(var14, var15, this.px, this.py, this.CIRCLE_RADIUS, var16);
            var14 = var16.x;
            var15 = var16.y;
         } else {
            intersectLineSegmentWithAABB(this.px, this.py, var12, var13, (float)var4, (float)var5, (float)var6, (float)var7, var16);
            var12 = var16.x;
            var13 = var16.y;
            intersectLineSegmentWithAABB(this.px, this.py, var14, var15, (float)var4, (float)var5, (float)var6, (float)var7, var16);
            var14 = var16.x;
            var15 = var16.y;
         }

         float var17 = Vector2.getDirection(var12 - this.px, var13 - this.py) + 3.1415927F;
         float var18 = Vector2.getDirection(var14 - this.px, var15 - this.py) + 3.1415927F;
         Vector3f var19 = VisibilityPolygon2.L_render.v1.set(var12, var13, var17);
         Vector3f var20 = VisibilityPolygon2.L_render.v2.set(var14, var15, var18);
         Vector3f var22;
         Vector3f var23;
         if (VisibilityPolygon2.instance.USE_CIRCLE) {
            for(int var27 = 0; var27 < circlePoints.size(); ++var27) {
               var22 = (Vector3f)circlePoints.get(var27);
               var22.x += this.px;
               var22.y += this.py;
            }

            ArrayList var28 = VisibilityPolygon2.L_render.vs;
            var28.clear();
            var28.add(var19);
            var28.add(var20);
            int var29;
            if (var17 > var18) {
               for(var29 = 0; var29 < circlePoints.size(); ++var29) {
                  var23 = (Vector3f)circlePoints.get(var29);
                  if (Float.compare(var23.z, var17) < 0 && Float.compare(var23.z, var18) > 0) {
                     var28.add(var23);
                  }
               }
            } else {
               for(var29 = 0; var29 < circlePoints.size(); ++var29) {
                  var23 = (Vector3f)circlePoints.get(var29);
                  if (Float.compare(var23.z, var17) < 0 || Float.compare(var23.z, var18) > 0) {
                     var28.add(var23);
                  }
               }
            }

            var28.sort((var0, var1x) -> {
               return Float.compare(var0.z, var1x.z);
            });
            var28.add(var28.indexOf(var20), VisibilityPolygon2.L_addShadowToClipper.v1.set(this.px, this.py, 0.0F));
            var2.clear();

            for(var29 = 0; var29 < var28.size(); ++var29) {
               var2.putFloat(((Vector3f)var28.get(var29)).x);
               var2.putFloat(((Vector3f)var28.get(var29)).y);
            }

            var1.addPath(var28.size(), var2, false);

            for(var29 = 0; var29 < circlePoints.size(); ++var29) {
               var23 = (Vector3f)circlePoints.get(var29);
               var23.x -= this.px;
               var23.y -= this.py;
            }

         } else {
            Vector3f var21 = VisibilityPolygon2.L_render.v3.set((float)var4, (float)var5, VisibilityPolygon2.L_addShadowToClipper.angle1cm);
            var22 = VisibilityPolygon2.L_render.v4.set((float)var4, (float)var7, VisibilityPolygon2.L_addShadowToClipper.angle2cm);
            var23 = VisibilityPolygon2.L_render.v5.set((float)var6, (float)var7, VisibilityPolygon2.L_addShadowToClipper.angle3cm);
            Vector3f var24 = VisibilityPolygon2.L_render.v6.set((float)var6, (float)var5, VisibilityPolygon2.L_addShadowToClipper.angle4cm);
            ArrayList var25 = VisibilityPolygon2.L_render.vs;
            var25.clear();
            var25.add(var19);
            var25.add(var20);
            if (var17 > var18) {
               if (Float.compare(var21.z, var17) < 0 && Float.compare(var21.z, var18) > 0) {
                  var25.add(var21);
               }

               if (Float.compare(var22.z, var17) < 0 && Float.compare(var22.z, var18) > 0) {
                  var25.add(var22);
               }

               if (Float.compare(var23.z, var17) < 0 && Float.compare(var23.z, var18) > 0) {
                  var25.add(var23);
               }

               if (Float.compare(var24.z, var17) < 0 && Float.compare(var24.z, var18) > 0) {
                  var25.add(var24);
               }
            } else {
               if (Float.compare(var21.z, var17) < 0 || Float.compare(var21.z, var18) > 0) {
                  var25.add(var21);
               }

               if (Float.compare(var22.z, var17) < 0 || Float.compare(var22.z, var18) > 0) {
                  var25.add(var22);
               }

               if (Float.compare(var23.z, var17) < 0 || Float.compare(var23.z, var18) > 0) {
                  var25.add(var23);
               }

               if (Float.compare(var24.z, var17) < 0 || Float.compare(var24.z, var18) > 0) {
                  var25.add(var24);
               }
            }

            var25.sort((var0, var1x) -> {
               return Float.compare(var0.z, var1x.z);
            });
            var25.add(var25.indexOf(var20), VisibilityPolygon2.L_addShadowToClipper.v1.set(this.px, this.py, 0.0F));
            var2.clear();

            for(int var26 = 0; var26 < var25.size(); ++var26) {
               var2.putFloat(((Vector3f)var25.get(var26)).x);
               var2.putFloat(((Vector3f)var25.get(var26)).y);
            }

            var1.addPath(var25.size(), var2, false);
         }
      }

      void addShadowToClipper(Clipper var1, ByteBuffer var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10) {
         IsoChunkMap var11 = IsoWorld.instance.CurrentCell.getChunkMap(this.playerIndex);
         int var12 = var11.getWorldXMinTiles();
         int var13 = var11.getWorldYMinTiles();
         int var14 = var11.getWorldXMaxTiles();
         int var15 = var11.getWorldYMaxTiles();
         float var16 = Vector2.getDirection(var7 - this.px, var8 - this.py) + 3.1415927F;
         float var17 = Vector2.getDirection(var9 - this.px, var10 - this.py) + 3.1415927F;
         Vector3f var18 = VisibilityPolygon2.L_render.v1.set(var7, var8, var16);
         Vector3f var19 = VisibilityPolygon2.L_render.v2.set(var9, var10, var17);
         Vector3f var23;
         if (VisibilityPolygon2.instance.USE_CIRCLE) {
            float var27 = PZMath.min(var17, var16);
            float var28 = PZMath.max(var17, var16);

            for(int var29 = 0; var29 < circlePoints.size(); ++var29) {
               var23 = (Vector3f)circlePoints.get(var29);
               var23.x += this.px;
               var23.y += this.py;
            }

            ArrayList var30 = VisibilityPolygon2.L_render.vs;
            var30.clear();
            var30.add(var18);
            var30.add(var19);
            var2.clear();
            int var31;
            Vector3f var32;
            if (var27 < 1.5707964F && var28 >= 4.712389F) {
               for(var31 = 0; var31 < circlePoints.size(); ++var31) {
                  var32 = (Vector3f)circlePoints.get(var31);
                  if (Float.compare(var32.z, var28) > 0 || Float.compare(var32.z, var27) < 0) {
                     var30.add(var32);
                  }
               }

               var30.sort((var0, var1x) -> {
                  return Float.compare(var0.z, var1x.z);
               });

               for(var31 = 0; var31 < var30.size(); ++var31) {
                  var32 = (Vector3f)var30.get(var31);
                  if (var32.z >= 4.712389F) {
                     var2.putFloat(var32.x);
                     var2.putFloat(var32.y);
                  }
               }

               for(var31 = 0; var31 < var30.size(); ++var31) {
                  var32 = (Vector3f)var30.get(var31);
                  if (var32.z < 1.5707964F) {
                     var2.putFloat(var32.x);
                     var2.putFloat(var32.y);
                  }
               }

               var2.putFloat(var3);
               var2.putFloat(var4);
               var2.putFloat(var5);
               var2.putFloat(var6);
            } else {
               for(var31 = 0; var31 < circlePoints.size(); ++var31) {
                  var32 = (Vector3f)circlePoints.get(var31);
                  if (Float.compare(var32.z, var28) < 0 && Float.compare(var32.z, var27) > 0) {
                     var30.add(var32);
                  }
               }

               var30.sort((var0, var1x) -> {
                  return Float.compare(var0.z, var1x.z);
               });

               for(var31 = 0; var31 < var30.size(); ++var31) {
                  var32 = (Vector3f)var30.get(var31);
                  var2.putFloat(var32.x);
                  var2.putFloat(var32.y);
               }

               var2.putFloat(var3);
               var2.putFloat(var4);
               var2.putFloat(var5);
               var2.putFloat(var6);
            }

            var1.addPath(var30.size() + 2, var2, false);

            for(var31 = 0; var31 < circlePoints.size(); ++var31) {
               var32 = (Vector3f)circlePoints.get(var31);
               var32.x -= this.px;
               var32.y -= this.py;
            }

         } else {
            Vector3f var20 = VisibilityPolygon2.L_render.v3.set((float)var12, (float)var13, VisibilityPolygon2.L_addShadowToClipper.angle1cm);
            Vector3f var21 = VisibilityPolygon2.L_render.v4.set((float)var12, (float)var15, VisibilityPolygon2.L_addShadowToClipper.angle2cm);
            Vector3f var22 = VisibilityPolygon2.L_render.v5.set((float)var14, (float)var15, VisibilityPolygon2.L_addShadowToClipper.angle3cm);
            var23 = VisibilityPolygon2.L_render.v6.set((float)var14, (float)var13, VisibilityPolygon2.L_addShadowToClipper.angle4cm);
            ArrayList var24 = VisibilityPolygon2.L_render.vs;
            var24.clear();
            var24.add(var18);
            var24.add(var19);
            int var25;
            if (var17 > var16) {
               if (Float.compare(var20.z, var17) < 0 && Float.compare(var20.z, var16) > 0) {
                  var24.add(var20);
               }

               if (Float.compare(var21.z, var17) < 0 && Float.compare(var21.z, var16) > 0) {
                  var24.add(var21);
               }

               if (Float.compare(var22.z, var17) < 0 && Float.compare(var22.z, var16) > 0) {
                  var24.add(var22);
               }

               if (Float.compare(var23.z, var17) < 0 && Float.compare(var23.z, var16) > 0) {
                  var24.add(var23);
               }

               var24.sort((var0, var1x) -> {
                  return Float.compare(var0.z, var1x.z);
               });
               var24.add(VisibilityPolygon2.L_addShadowToClipper.v1.set(var3, var4, 0.0F));
               var24.add(VisibilityPolygon2.L_addShadowToClipper.v2.set(var5, var6, 0.0F));
               var2.clear();

               for(var25 = 0; var25 < var24.size(); ++var25) {
                  var2.putFloat(((Vector3f)var24.get(var25)).x);
                  var2.putFloat(((Vector3f)var24.get(var25)).y);
               }

               var1.addPath(var24.size(), var2, false);
            } else {
               if (Float.compare(var20.z, var17) < 0 || Float.compare(var20.z, var16) > 0) {
                  var24.add(var20);
               }

               if (Float.compare(var21.z, var17) < 0 || Float.compare(var21.z, var16) > 0) {
                  var24.add(var21);
               }

               if (Float.compare(var22.z, var17) < 0 || Float.compare(var22.z, var16) > 0) {
                  var24.add(var22);
               }

               if (Float.compare(var23.z, var17) < 0 || Float.compare(var23.z, var16) > 0) {
                  var24.add(var23);
               }

               var24.sort((var0, var1x) -> {
                  return Float.compare(var0.z, var1x.z);
               });
               var25 = var24.indexOf(var18);
               var24.add(var25, VisibilityPolygon2.L_addShadowToClipper.v1.set(var3, var4, 0.0F));
               var24.add(var25 + 1, VisibilityPolygon2.L_addShadowToClipper.v2.set(var5, var6, 0.0F));
               var2.clear();

               for(int var26 = 0; var26 < var24.size(); ++var26) {
                  var2.putFloat(((Vector3f)var24.get(var26)).x);
                  var2.putFloat(((Vector3f)var24.get(var26)).y);
               }

               var1.addPath(var24.size(), var2, false);
            }

         }
      }

      void triangulatePolygons(int var1, Clipper var2, ByteBuffer var3, PlayerCamera var4, float var5) {
         for(int var6 = 0; var6 < var1; ++var6) {
            var3.clear();

            int var7;
            try {
               var7 = var2.triangulate(var6, var3);
            } catch (BufferOverflowException var21) {
               var3 = VisibilityPolygon2.L_render.clipperBuf = ByteBuffer.allocateDirect(var3.capacity() + 1024);
               --var6;
               continue;
            }

            for(int var8 = 0; var8 < var7; var8 += 3) {
               float var9 = var3.getFloat();
               float var10 = var3.getFloat();
               float var11 = var3.getFloat();
               float var12 = var3.getFloat();
               float var13 = var3.getFloat();
               float var14 = var3.getFloat();
               if (VisibilityPolygon2.instance.USE_CIRCLE) {
                  Vector2 var15 = VisibilityPolygon2.L_render.vector2;
                  Vector2 var16 = VisibilityPolygon2.L_render.vector2_2;
                  Vector2 var17 = VisibilityPolygon2.L_render.vector2_3;
                  float var18 = this.closestPointOnLineSegment(var9, var10, var11, var12, this.px, this.py, var15);
                  float var19 = this.closestPointOnLineSegment(var11, var12, var13, var14, this.px, this.py, var16);
                  float var20 = this.closestPointOnLineSegment(var13, var14, var9, var10, this.px, this.py, var17);
                  if (var18 < 0.001F) {
                     var18 = 3.4028235E38F;
                  }

                  if (var19 < 0.001F) {
                     var19 = 3.4028235E38F;
                  }

                  if (var20 < 0.001F) {
                     var20 = 3.4028235E38F;
                  }

                  if (var18 < var19 && var18 < var20) {
                     this.renderOneTriangle(var4, var9, var10, var15.x, var15.y, var13, var14, var5);
                     this.renderOneTriangle(var4, var15.x, var15.y, var11, var12, var13, var14, var5);
                     continue;
                  }

                  if (var19 < var18 && var19 < var20) {
                     this.renderOneTriangle(var4, var11, var12, var16.x, var16.y, var9, var10, var5);
                     this.renderOneTriangle(var4, var16.x, var16.y, var13, var14, var9, var10, var5);
                     continue;
                  }

                  if (var20 < var18 && var20 < var19) {
                     this.renderOneTriangle(var4, var13, var14, var17.x, var17.y, var11, var12, var5);
                     this.renderOneTriangle(var4, var17.x, var17.y, var9, var10, var11, var12, var5);
                     continue;
                  }
               }

               this.renderOneTriangle(var4, var9, var10, var11, var12, var13, var14, var5);
            }
         }

      }

      float closestPointOnLineSegment(float var1, float var2, float var3, float var4, float var5, float var6, Vector2 var7) {
         double var8 = (double)((var5 - var1) * (var3 - var1) + (var6 - var2) * (var4 - var2)) / (Math.pow((double)(var3 - var1), 2.0) + Math.pow((double)(var4 - var2), 2.0));
         if (Double.compare(var8, 0.0010000000474974513) <= 0) {
            var7.set(var1, var2);
            return IsoUtils.DistanceToSquared(var5, var6, var1, var2);
         } else if (Double.compare(var8, 0.9989999999525025) >= 0) {
            var7.set(var3, var4);
            return IsoUtils.DistanceToSquared(var5, var6, var3, var4);
         } else {
            double var10 = (double)var1 + var8 * (double)(var3 - var1);
            double var12 = (double)var2 + var8 * (double)(var4 - var2);
            var7.set((float)var10, (float)var12);
            return IsoUtils.DistanceToSquared(var5, var6, (float)var10, (float)var12);
         }
      }

      void renderOneTriangle(PlayerCamera var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
         VBORenderer var9 = VBORenderer.getInstance();
         float var10 = var1.XToScreenExact(var2, var3, (float)this.pz, 0);
         float var11 = var1.YToScreenExact(var2, var3, (float)this.pz, 0);
         float var12 = var1.XToScreenExact(var4, var5, (float)this.pz, 0);
         float var13 = var1.YToScreenExact(var4, var5, (float)this.pz, 0);
         float var14 = var1.XToScreenExact(var6, var7, (float)this.pz, 0);
         float var15 = var1.YToScreenExact(var6, var7, (float)this.pz, 0);
         float var16 = 0.0F;
         float var17 = 0.0F;
         float var18 = -1.0E-4F;
         float var19 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(this.px), PZMath.fastfloor(this.py), var2, var3, (float)this.pz).depthStart + var18;
         float var20 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(this.px), PZMath.fastfloor(this.py), var4, var5, (float)this.pz).depthStart + var18;
         float var21 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(this.px), PZMath.fastfloor(this.py), var6, var7, (float)this.pz).depthStart + var18;
         float var22 = 0.0F;
         float var23 = 0.0F;
         float var24 = 0.0F;
         if (DebugOptions.instance.DisplayVisibilityPolygon.getValue()) {
            int var25 = VisibilityPolygon2.L_render.triangleIndex++ % Model.debugDrawColours.length;
            var22 = Model.debugDrawColours[var25].r;
            var23 = Model.debugDrawColours[var25].g;
            var24 = Model.debugDrawColours[var25].b;
            var8 = 1.0F;
         }

         if (VisibilityPolygon2.instance.USE_CIRCLE) {
            float var28 = 1.0F - PZMath.clamp(IsoUtils.DistanceToSquared(var2, var3, this.px, this.py) / (this.CIRCLE_RADIUS * this.CIRCLE_RADIUS), 0.0F, 1.0F);
            float var26 = 1.0F - PZMath.clamp(IsoUtils.DistanceToSquared(var4, var5, this.px, this.py) / (this.CIRCLE_RADIUS * this.CIRCLE_RADIUS), 0.0F, 1.0F);
            float var27 = 1.0F - PZMath.clamp(IsoUtils.DistanceToSquared(var6, var7, this.px, this.py) / (this.CIRCLE_RADIUS * this.CIRCLE_RADIUS), 0.0F, 1.0F);
            var9.addTriangleDepth(var10, var11, 0.0F, var16, var17, var19, var28, var12, var13, 0.0F, var16 + 0.5F, var17 + 0.5F, var20, var26, var14, var15, 0.0F, var16 + 1.0F, var17 + 1.0F, var21, var27, var22, var23, var24, var8);
         } else {
            var9.addTriangleDepth(var10, var11, 0.0F, var16, var17, var19, var12, var13, 0.0F, var16 + 0.5F, var17 + 0.5F, var20, var14, var15, 0.0F, var16 + 1.0F, var17 + 1.0F, var21, var22, var23, var24, var8);
         }
      }

      void addClipperPolygon(Clipper var1, ByteBuffer var2, ClipperPolygon var3, boolean var4) {
         this.addClipperPolygon(var1, var2, var3.outer, var4);

         for(int var5 = 0; var5 < var3.holes.size(); ++var5) {
            this.addClipperPolygon(var1, var2, (TFloatArrayList)var3.holes.get(var5), var4);
         }

      }

      void addClipperPolygon(Clipper var1, ByteBuffer var2, TFloatArrayList var3, boolean var4) {
         var2.clear();
         if (var4) {
         }

         for(int var5 = 0; var5 < var3.size(); ++var5) {
            var2.putFloat(var3.get(var5));
         }

         var1.addPath(var3.size() / 2, var2, var4);
      }

      void addClipperPolygons(Clipper var1, ByteBuffer var2, ArrayList<ClipperPolygon> var3, boolean var4) {
         for(int var5 = 0; var5 < var3.size(); ++var5) {
            this.addClipperPolygon(var1, var2, (ClipperPolygon)var3.get(var5), var4);
         }

      }

      void getClipperPolygons(Clipper var1, int var2, ArrayList<ClipperPolygon> var3) {
         ByteBuffer var4 = VisibilityPolygon2.L_render.clipperBuf;

         for(int var5 = 0; var5 < var2; ++var5) {
            var4.clear();
            var1.getPolygon(var5, var4);
            short var6 = var4.getShort();
            if (var6 >= 3) {
               ClipperPolygon var7 = (ClipperPolygon)VisibilityPolygon2.L_render.clipperPolygonPool.alloc();

               for(int var8 = 0; var8 < var6; ++var8) {
                  float var9 = var4.getFloat();
                  float var10 = var4.getFloat();
                  var7.outer.add(var9);
                  var7.outer.add(var10);
               }

               short var14 = var4.getShort();

               for(int var15 = 0; var15 < var14; ++var15) {
                  var6 = var4.getShort();
                  if (var6 >= 3) {
                     TFloatArrayList var16 = (TFloatArrayList)VisibilityPolygon2.L_render.floatArrayListPool.alloc();
                     var16.clear();

                     for(int var11 = 0; var11 < var6; ++var11) {
                        float var12 = var4.getFloat();
                        float var13 = var4.getFloat();
                        var16.add(var12);
                        var16.add(var13);
                     }

                     var7.holes.add(var16);
                  }
               }

               var3.add(var7);
            }
         }

      }

      void releaseClipperPolygons(ArrayList<ClipperPolygon> var1) {
         for(int var2 = 0; var2 < var1.size(); ++var2) {
            ClipperPolygon var3 = (ClipperPolygon)var1.get(var2);
            var3.outer.clear();
            VisibilityPolygon2.L_render.floatArrayListPool.releaseAll(var3.holes);
            var3.holes.clear();
         }

         VisibilityPolygon2.L_render.clipperPolygonPool.releaseAll(var1);
         var1.clear();
      }

      private static class PolygonData {
         public final int MAX_VERTS = 3072;
         public final int NUM_ELEMENTS = 5;
         private final FloatBuffer data = BufferUtils.createFloatBuffer(15360);
         private int bufferID = -1;
         private PlayerCamera camera;
         private int sx;
         private int sy;
         private float z;

         private PolygonData() {
         }

         public void init() {
            if (this.bufferID == -1) {
               this.bufferID = GL20.glGenBuffers();
               GL20.glBindBuffer(34962, this.bufferID);
               GL20.glBufferData(34962, this.data, 35048);
               GL20.glBindBuffer(34962, 0);
            }

         }

         public void update(int var1) {
            this.sx = PZMath.fastfloor(IsoCamera.frameState.CamCharacterX);
            this.sy = PZMath.fastfloor(IsoCamera.frameState.CamCharacterY);
            this.z = (float)PZMath.fastfloor(IsoCamera.frameState.CamCharacterZ);
         }

         public void draw() {
            GL20.glBindBuffer(34962, this.bufferID);
            GL20.glBufferSubData(34962, 0L, this.data);
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(0, 3, 5126, false, 20, 0L);
            GL20.glVertexAttribPointer(1, 1, 5126, false, 20, 12L);
            GL20.glVertexAttribPointer(2, 1, 5126, false, 20, 16L);
            GL11.glDrawArrays(4, 0, this.data.limit() / 5);
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            GL20.glDisableVertexAttribArray(2);
            GL20.glBindBuffer(34962, 0);
         }

         public void addVertex(float var1, float var2, float var3) {
            float var4 = this.camera.XToScreenExact(var1, var2, this.z, 0);
            float var5 = this.camera.YToScreenExact(var1, var2, this.z, 0);
            float var6 = IsoDepthHelper.getSquareDepthData(this.sx, this.sy, var1, var2, this.z).depthStart;
            this.addTransformed(var4, var5, 0.0F, var3, var6 + -1.0E-4F);
         }

         public void addTransformed(float var1, float var2, float var3, float var4, float var5) {
            this.data.put(var1);
            this.data.put(var2);
            this.data.put(var3);
            this.data.put(var4);
            this.data.put(var5);
         }

         public void reset() {
            this.data.rewind();
            this.data.limit(this.data.capacity());
         }
      }
   }

   static final class L_addShadowToClipper {
      static float angle1cm;
      static float angle2cm;
      static float angle3cm;
      static float angle4cm;
      static final Vector3f v1 = new Vector3f();
      static final Vector3f v2 = new Vector3f();

      L_addShadowToClipper() {
      }
   }

   static final class L_render {
      static final Vector3f v1 = new Vector3f();
      static final Vector3f v2 = new Vector3f();
      static final Vector3f v3 = new Vector3f();
      static final Vector3f v4 = new Vector3f();
      static final Vector3f v5 = new Vector3f();
      static final Vector3f v6 = new Vector3f();
      static final ArrayList<Vector3f> vs = new ArrayList();
      static final Vector2 vector2 = new Vector2();
      static final Vector2 vector2_2 = new Vector2();
      static final Vector2 vector2_3 = new Vector2();
      static final Clipper m_clipper = new Clipper();
      static ByteBuffer clipperBuf = ByteBuffer.allocateDirect(10240);
      static final ArrayList<ClipperPolygon> clipperPolygons1 = new ArrayList();
      static final ArrayList<ClipperPolygon> clipperPolygons2 = new ArrayList();
      static final ArrayList<ClipperPolygon> clipperPolygons3 = new ArrayList();
      static final ObjectPool<ClipperPolygon> clipperPolygonPool = new ObjectPool(ClipperPolygon::new);
      static final ObjectPool<TFloatArrayList> floatArrayListPool = new ObjectPool(TFloatArrayList::new);
      static int triangleIndex;

      L_render() {
      }
   }

   static final class L_calculateVisibilityPolygon {
      static final ObjectPool<Segment> m_segmentPool = new ObjectPool(Segment::new);
      static final Vector2 m_tempVector2_1 = new Vector2();
      static final HashSet<Segment> segmentHashSet = new HashSet();
      static final ArrayList<Segment> segmentList = new ArrayList();
      static final ArrayList<VisibilityWall> sortedWalls = new ArrayList();
      static final ArrayList<IsoGridSquare> solidSquares = new ArrayList();

      L_calculateVisibilityPolygon() {
      }
   }

   public static final class ChunkData {
      final IsoChunk m_chunk;
      final TIntObjectHashMap<ChunkLevelData> m_levelData = new TIntObjectHashMap();

      public ChunkData(IsoChunk var1) {
         this.m_chunk = var1;
      }

      public ChunkLevelData getDataForLevel(int var1) {
         if (var1 >= -32 && var1 <= 31) {
            int var2 = var1 + 32;
            ChunkLevelData var3 = (ChunkLevelData)this.m_levelData.get(var2);
            if (var3 == null) {
               var3 = new ChunkLevelData(this, var1);
               this.m_levelData.put(var2, var3);
            }

            return var3;
         } else {
            return null;
         }
      }

      public void removeFromWorld() {
         Iterator var1 = this.m_levelData.valueCollection().iterator();

         while(var1.hasNext()) {
            ChunkLevelData var2 = (ChunkLevelData)var1.next();
            var2.removeFromWorld();
         }

      }
   }

   public static final class ChunkLevelData {
      final ChunkData m_chunkData;
      final int m_level;
      int m_adjacentChunkLoadedCounter = -1;
      final ArrayList<VisibilityWall> m_allWalls = new ArrayList();
      final ArrayList<IsoGridSquare> m_solidSquares = new ArrayList();

      ChunkLevelData(ChunkData var1, int var2) {
         this.m_chunkData = var1;
         this.m_level = var2;
      }

      public void invalidate() {
         this.m_adjacentChunkLoadedCounter = -1;
         ++VisibilityPolygon2.getInstance().dirtyObstacleCounter;
      }

      public void recreate() {
         int var1 = this.m_level;
         IsoChunk var2 = this.m_chunkData.m_chunk;
         ObjectPool var3 = VisibilityPolygon2.getInstance().s_visibilityWallPool;
         var3.releaseAll(this.m_allWalls);
         this.m_allWalls.clear();
         this.m_solidSquares.clear();
         if (var1 >= var2.minLevel && var1 <= var2.maxLevel) {
            IsoGridSquare[] var4 = var2.squares[var2.squaresIndexOfLevel(var1)];
            byte var5 = 8;

            int var6;
            VisibilityWall var7;
            int var8;
            IsoGridSquare var9;
            for(var6 = 0; var6 < var5; ++var6) {
               var7 = null;

               for(var8 = 0; var8 < var5; ++var8) {
                  var9 = var4[var8 + var6 * var5];
                  if (this.hasNorthWall(var9)) {
                     if (var7 == null) {
                        var7 = (VisibilityWall)var3.alloc();
                        var7.chunkLevelData = this;
                        var7.x1 = var9.x;
                        var7.y1 = var9.y;
                     }
                  } else if (var7 != null) {
                     var7.x2 = var2.wx * var5 + var8;
                     var7.y2 = var2.wy * var5 + var6;
                     this.m_allWalls.add(var7);
                     var7 = null;
                  }

                  if (var9 != null) {
                     if (var9.Has(IsoObjectType.tree)) {
                        IsoTree var10 = var9.getTree();
                        if (var10 != null && var10.getProperties() != null && var10.getProperties().Is(IsoFlagType.blocksight)) {
                           this.m_solidSquares.add(var9);
                        }
                     } else if (var9.isSolid()) {
                        this.m_solidSquares.add(var9);
                     }
                  }
               }

               if (var7 != null) {
                  var7.x2 = var2.wx * var5 + var5;
                  var7.y2 = var2.wy * var5 + var6;
                  this.m_allWalls.add(var7);
               }
            }

            for(var6 = 0; var6 < var5; ++var6) {
               var7 = null;

               for(var8 = 0; var8 < var5; ++var8) {
                  var9 = var4[var6 + var8 * var5];
                  if (this.hasWestWall(var9)) {
                     if (var7 == null) {
                        var7 = (VisibilityWall)var3.alloc();
                        var7.chunkLevelData = this;
                        var7.x1 = var9.x;
                        var7.y1 = var9.y;
                     }
                  } else if (var7 != null) {
                     var7.x2 = var2.wx * var5 + var6;
                     var7.y2 = var2.wy * var5 + var8;
                     this.m_allWalls.add(var7);
                     var7 = null;
                  }
               }

               if (var7 != null) {
                  var7.x2 = var2.wx * var5 + var6;
                  var7.y2 = var2.wy * var5 + var5;
                  this.m_allWalls.add(var7);
               }
            }

         }
      }

      boolean hasNorthWall(IsoGridSquare var1) {
         if (var1 == null) {
            return false;
         } else if (var1.isSolid()) {
            return false;
         } else if (var1.Has(IsoObjectType.tree)) {
            return false;
         } else {
            return var1.testVisionAdjacent(0, -1, 0, false, false) == LosUtil.TestResults.Blocked;
         }
      }

      boolean hasWestWall(IsoGridSquare var1) {
         if (var1 == null) {
            return false;
         } else if (var1.isSolid()) {
            return false;
         } else if (var1.Has(IsoObjectType.tree)) {
            return false;
         } else {
            return var1.testVisionAdjacent(-1, 0, 0, false, false) == LosUtil.TestResults.Blocked;
         }
      }

      void removeFromWorld() {
         this.m_adjacentChunkLoadedCounter = -1;
         VisibilityPolygon2.instance.s_visibilityWallPool.releaseAll(this.m_allWalls);
         this.m_allWalls.clear();
         this.m_solidSquares.clear();
      }
   }

   public static final class VisibilityWall {
      ChunkLevelData chunkLevelData;
      public int x1;
      public int y1;
      public int x2;
      public int y2;

      public VisibilityWall() {
      }

      boolean isHorizontal() {
         return this.y1 == this.y2;
      }
   }

   private static final class Partition {
      float minAngle;
      float maxAngle;
      final ArrayList<Segment> segments = new ArrayList();

      private Partition() {
      }

      boolean addSegment(Segment var1) {
         int var2 = this.segments.size();

         int var3;
         for(var3 = 0; var3 < this.segments.size(); ++var3) {
            if (Float.compare(var1.minDist(), ((Segment)this.segments.get(var3)).minDist()) < 0) {
               var2 = var3;
               break;
            }
         }

         for(var3 = 0; var3 < var2; ++var3) {
            if (var1.isInShadowOf((Segment)this.segments.get(var3))) {
               return false;
            }
         }

         this.segments.add(var2, var1);
         return true;
      }
   }

   private static final class Segment {
      final EndPoint a = new EndPoint();
      final EndPoint b = new EndPoint();

      private Segment() {
      }

      float minAngle() {
         return PZMath.min(this.a.angle, this.b.angle);
      }

      float maxAngle() {
         return PZMath.max(this.a.angle, this.b.angle);
      }

      float minDist() {
         return PZMath.min(this.a.dist, this.b.dist);
      }

      float maxDist() {
         return PZMath.max(this.a.dist, this.b.dist);
      }

      boolean isInShadowOf(Segment var1) {
         float var2 = this.minAngle();
         float var3 = this.maxAngle();
         float var4;
         if (var3 - var2 > 180.0F) {
            var2 += 360.0F;
            var4 = var2;
            var2 = var3;
            var3 = var4;
         }

         var4 = var1.minAngle();
         float var5 = var1.maxAngle();
         if (var5 - var4 > 180.0F) {
            var4 += 360.0F;
            float var6 = var4;
            var4 = var5;
            var5 = var6;
         }

         if (var3 > 360.0F && var5 < 180.0F) {
            if (var4 < 180.0F) {
               var4 += 360.0F;
            }

            var5 += 360.0F;
         }

         if (var5 > 360.0F && var3 < 180.0F) {
            if (var2 < 180.0F) {
               var2 += 360.0F;
            }

            var3 += 360.0F;
         }

         return Float.compare(var2, var4) >= 0 && Float.compare(var3, var5) <= 0 && Float.compare(this.minDist(), var1.maxDist()) >= 0;
      }
   }

   private static final class EndPoint {
      float x;
      float y;
      float dirX;
      float dirY;
      float angle;
      float dist;

      private EndPoint() {
      }

      EndPoint set(float var1, float var2, float var3, float var4, float var5, float var6) {
         this.x = var1;
         this.y = var2;
         Vector2 var7 = VisibilityPolygon2.L_calculateVisibilityPolygon.m_tempVector2_1.set(var3, var4);
         var7.normalize();
         this.dirX = var7.x;
         this.dirY = var7.y;
         this.angle = var5;
         this.dist = var6;
         return this;
      }
   }
}
