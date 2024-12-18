package zombie.worldMap;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.iso.IsoCell;
import zombie.vehicles.Clipper;

public final class WorldMapGeometry {
   public Type m_type;
   public final ArrayList<WorldMapPoints> m_points = new ArrayList();
   public int m_minX;
   public int m_minY;
   public int m_maxX;
   public int m_maxY;
   public float[] m_triangles = null;
   public ArrayList<TrianglesPerZoom> m_trianglesPerZoom = null;
   public boolean bFailedToTriangulate = false;
   public int m_vboIndex1 = -1;
   public int m_vboIndex2 = -1;
   public int m_vboIndex3 = -1;
   public int m_vboIndex4 = -1;
   private static Clipper s_clipper = null;
   private static ByteBuffer s_vertices = null;

   public WorldMapGeometry() {
   }

   public void calculateBounds() {
      this.m_minX = this.m_minY = 2147483647;
      this.m_maxX = this.m_maxY = -2147483648;

      for(int var1 = 0; var1 < this.m_points.size(); ++var1) {
         WorldMapPoints var2 = (WorldMapPoints)this.m_points.get(var1);
         var2.calculateBounds();
         this.m_minX = PZMath.min(this.m_minX, var2.m_minX);
         this.m_minY = PZMath.min(this.m_minY, var2.m_minY);
         this.m_maxX = PZMath.max(this.m_maxX, var2.m_maxX);
         this.m_maxY = PZMath.max(this.m_maxY, var2.m_maxY);
      }

   }

   public boolean containsPoint(float var1, float var2) {
      if (this.m_type == WorldMapGeometry.Type.Polygon && !this.m_points.isEmpty()) {
         return this.isPointInPolygon_WindingNumber(var1, var2, 0) != WorldMapGeometry.PolygonHit.Outside;
      } else {
         return false;
      }
   }

   public void triangulate(double[] var1) {
      if (s_clipper == null) {
         s_clipper = new Clipper();
      }

      s_clipper.clear();
      WorldMapPoints var2 = (WorldMapPoints)this.m_points.get(0);
      if (s_vertices == null || s_vertices.capacity() < var2.size() * 50 * 4) {
         s_vertices = ByteBuffer.allocateDirect(var2.size() * 50 * 4);
      }

      s_vertices.clear();
      int var3;
      if (var2.isClockwise()) {
         for(var3 = var2.numPoints() - 1; var3 >= 0; --var3) {
            s_vertices.putFloat((float)var2.getX(var3));
            s_vertices.putFloat((float)var2.getY(var3));
         }
      } else {
         for(var3 = 0; var3 < var2.numPoints(); ++var3) {
            s_vertices.putFloat((float)var2.getX(var3));
            s_vertices.putFloat((float)var2.getY(var3));
         }
      }

      s_clipper.addPath(var2.numPoints(), s_vertices, false);

      int var5;
      for(var3 = 1; var3 < this.m_points.size(); ++var3) {
         s_vertices.clear();
         WorldMapPoints var4 = (WorldMapPoints)this.m_points.get(var3);
         if (var4.isClockwise()) {
            for(var5 = var4.numPoints() - 1; var5 >= 0; --var5) {
               s_vertices.putFloat((float)var4.getX(var5));
               s_vertices.putFloat((float)var4.getY(var5));
            }
         } else {
            for(var5 = 0; var5 < var4.numPoints(); ++var5) {
               s_vertices.putFloat((float)var4.getX(var5));
               s_vertices.putFloat((float)var4.getY(var5));
            }
         }

         s_clipper.addPath(var4.numPoints(), s_vertices, true);
      }

      if (this.m_minX < 0 || this.m_minY < 0 || this.m_maxX > IsoCell.CellSizeInSquares || this.m_maxY > IsoCell.CellSizeInSquares) {
         var3 = IsoCell.CellSizeInSquares * 3;
         float var24 = (float)(-var3);
         float var26 = (float)(-var3);
         float var6 = (float)(IsoCell.CellSizeInSquares + var3);
         float var7 = (float)(-var3);
         float var8 = (float)(IsoCell.CellSizeInSquares + var3);
         float var9 = (float)(IsoCell.CellSizeInSquares + var3);
         float var10 = (float)(-var3);
         float var11 = (float)(IsoCell.CellSizeInSquares + var3);
         float var12 = (float)(-var3);
         float var13 = 0.0F;
         float var14 = 0.0F;
         float var15 = 0.0F;
         float var16 = 0.0F;
         float var17 = (float)IsoCell.CellSizeInSquares;
         float var18 = (float)IsoCell.CellSizeInSquares;
         float var19 = (float)IsoCell.CellSizeInSquares;
         float var20 = (float)IsoCell.CellSizeInSquares;
         float var21 = 0.0F;
         float var22 = (float)(-var3);
         float var23 = 0.0F;
         s_vertices.clear();
         s_vertices.putFloat(var24).putFloat(var26);
         s_vertices.putFloat(var6).putFloat(var7);
         s_vertices.putFloat(var8).putFloat(var9);
         s_vertices.putFloat(var10).putFloat(var11);
         s_vertices.putFloat(var12).putFloat(var13);
         s_vertices.putFloat(var14).putFloat(var15);
         s_vertices.putFloat(var16).putFloat(var17);
         s_vertices.putFloat(var18).putFloat(var19);
         s_vertices.putFloat(var20).putFloat(var21);
         s_vertices.putFloat(var22).putFloat(var23);
         s_clipper.addPath(10, s_vertices, true);
      }

      var3 = s_clipper.generatePolygons(0.0);
      if (var3 > 0) {
         int var25;
         for(var25 = 0; var25 < var3; ++var25) {
            s_vertices.clear();
            var5 = s_clipper.triangulate(var25, s_vertices);
            if (var5 >= 3) {
               int var27;
               if (this.m_triangles == null) {
                  this.m_triangles = new float[var5 * 2];
                  var27 = 0;
               } else {
                  var27 = this.m_triangles.length;
                  float[] var29 = new float[this.m_triangles.length + var5 * 2];
                  System.arraycopy(this.m_triangles, 0, var29, 0, this.m_triangles.length);
                  this.m_triangles = var29;
               }

               for(int var30 = 0; var30 < var5; ++var30) {
                  this.m_triangles[var27 + var30 * 2] = s_vertices.getFloat();
                  this.m_triangles[var27 + var30 * 2 + 1] = s_vertices.getFloat();
               }
            }
         }

         if (var1 != null) {
            for(var25 = 0; var25 < var1.length; ++var25) {
               double var28 = var1[var25] - (var25 == 0 ? 0.0 : var1[var25 - 1]);
               var3 = s_clipper.generatePolygons(var28);
               if (var3 > 0) {
                  TrianglesPerZoom var31 = new TrianglesPerZoom();
                  var31.m_delta = var1[var25];

                  for(int var32 = 0; var32 < var3; ++var32) {
                     s_vertices.clear();
                     int var33 = s_clipper.triangulate(var32, s_vertices);
                     if (var33 >= 3) {
                        int var34;
                        if (var31.m_triangles == null) {
                           var31.m_triangles = new float[var33 * 2];
                           var34 = 0;
                        } else {
                           var34 = var31.m_triangles.length;
                           float[] var35 = new float[var31.m_triangles.length + var33 * 2];
                           System.arraycopy(var31.m_triangles, 0, var35, 0, var31.m_triangles.length);
                           var31.m_triangles = var35;
                        }

                        for(int var36 = 0; var36 < var33; ++var36) {
                           var31.m_triangles[var34 + var36 * 2] = s_vertices.getFloat();
                           var31.m_triangles[var34 + var36 * 2 + 1] = s_vertices.getFloat();
                        }
                     }
                  }

                  if (var31.m_triangles != null) {
                     if (this.m_trianglesPerZoom == null) {
                        this.m_trianglesPerZoom = new ArrayList();
                     }

                     this.m_trianglesPerZoom.add(var31);
                  }
               }
            }

         }
      }
   }

   TrianglesPerZoom findTriangles(double var1) {
      if (this.m_trianglesPerZoom == null) {
         return null;
      } else {
         for(int var3 = 0; var3 < this.m_trianglesPerZoom.size(); ++var3) {
            TrianglesPerZoom var4 = (TrianglesPerZoom)this.m_trianglesPerZoom.get(var3);
            if (var4.m_delta == var1) {
               return var4;
            }
         }

         return null;
      }
   }

   public void dispose() {
      this.m_points.clear();
      this.m_triangles = null;
   }

   float isLeft(float var1, float var2, float var3, float var4, float var5, float var6) {
      return (var3 - var1) * (var6 - var2) - (var5 - var1) * (var4 - var2);
   }

   PolygonHit isPointInPolygon_WindingNumber(float var1, float var2, int var3) {
      int var4 = 0;
      WorldMapPoints var5 = (WorldMapPoints)this.m_points.get(0);

      for(int var6 = 0; var6 < var5.numPoints(); ++var6) {
         int var7 = var5.getX(var6);
         int var8 = var5.getY(var6);
         int var9 = var5.getX((var6 + 1) % var5.numPoints());
         int var10 = var5.getY((var6 + 1) % var5.numPoints());
         if ((float)var8 <= var2) {
            if ((float)var10 > var2 && this.isLeft((float)var7, (float)var8, (float)var9, (float)var10, var1, var2) > 0.0F) {
               ++var4;
            }
         } else if ((float)var10 <= var2 && this.isLeft((float)var7, (float)var8, (float)var9, (float)var10, var1, var2) < 0.0F) {
            --var4;
         }
      }

      return var4 == 0 ? WorldMapGeometry.PolygonHit.Outside : WorldMapGeometry.PolygonHit.Inside;
   }

   public static enum Type {
      LineString,
      Point,
      Polygon;

      private Type() {
      }
   }

   private static enum PolygonHit {
      OnEdge,
      Inside,
      Outside;

      private PolygonHit() {
      }
   }

   public static final class TrianglesPerZoom {
      public float[] m_triangles;
      double m_delta;

      public TrianglesPerZoom() {
      }
   }
}
