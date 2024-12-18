package zombie.characters.animals.pathfind;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TShortArrayList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.joml.Runtime;
import org.joml.Vector2f;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.LineDrawer;
import zombie.iso.zones.Zone;
import zombie.popman.ObjectPool;

public final class Mesh {
   public MeshList meshList;
   public final ArrayList<Vector2f> polygon = new ArrayList();
   public final ArrayList<Vector2f> triangles = new ArrayList();
   public float centroidX;
   public float centroidY;
   public final TIntArrayList adjacentTriangles = new TIntArrayList();
   public final TShortArrayList trianglesOnBoundaries = new TShortArrayList();
   public final TShortArrayList edgesOnBoundaries = new TShortArrayList();
   boolean offMeshDone = false;
   final ArrayList<OffMeshConnection> offMeshConnections = new ArrayList();
   public Zone zone;
   static final ObjectPool<Mesh> pool = new ObjectPool<Mesh>(Mesh::new) {
      public void release(Mesh var1) {
         AnimalPathfind.getInstance().vector2fObjectPool.releaseAll(var1.triangles);
         var1.triangles.clear();
         var1.trianglesOnBoundaries.clear();
         var1.edgesOnBoundaries.clear();
         OffMeshConnection.pool.releaseAll(var1.offMeshConnections);
         var1.offMeshConnections.clear();
         super.release((Object)var1);
      }
   };

   public Mesh() {
   }

   void initFrom(Mesh var1) {
      this.meshList = var1.meshList;
      this.polygon.addAll(var1.polygon);
      this.triangles.addAll(var1.triangles);
      this.centroidX = var1.centroidX;
      this.centroidY = var1.centroidY;
      this.adjacentTriangles.addAll(var1.adjacentTriangles);
      this.trianglesOnBoundaries.addAll(var1.trianglesOnBoundaries);
      this.edgesOnBoundaries.addAll(var1.edgesOnBoundaries);
      this.zone = var1.zone;
   }

   void initFromZone(Zone var1) {
      this.zone = var1;

      for(int var2 = 0; var2 < var1.points.size(); var2 += 2) {
         this.polygon.add(new Vector2f((float)var1.points.get(var2), (float)var1.points.get(var2 + 1)));
      }

      float var10 = 3.4028235E38F;
      float var3 = 3.4028235E38F;
      float var4 = 1.4E-45F;
      float var5 = 1.4E-45F;
      float[] var6 = var1.getPolygonTriangles();

      for(int var7 = 0; var7 < var6.length; var7 += 2) {
         float var8 = var6[var7];
         float var9 = var6[var7 + 1];
         this.triangles.add(new Vector2f(var8, var9));
         var10 = Float.min(var10, var8);
         var3 = Float.min(var3, var9);
         var4 = Float.max(var4, var8);
         var5 = Float.max(var5, var9);
      }

      this.initEdges();
      this.initAdjacentTriangles();
      this.centroidX = (var4 - var10) / 2.0F;
      this.centroidY = (var5 - var3) / 2.0F;
   }

   void initEdges() {
      this.edgesOnBoundaries.clear();
      this.trianglesOnBoundaries.clear();

      for(int var1 = 0; var1 < this.triangles.size(); var1 += 3) {
         Vector2f var2 = (Vector2f)this.triangles.get(var1);
         Vector2f var3 = (Vector2f)this.triangles.get(var1 + 1);
         Vector2f var4 = (Vector2f)this.triangles.get(var1 + 2);
         short var5 = 0;
         if (this.isEdgeOnBoundary(var2, var3)) {
            var5 = (short)(var5 | 1);
         }

         if (this.isEdgeOnBoundary(var3, var4)) {
            var5 = (short)(var5 | 2);
         }

         if (this.isEdgeOnBoundary(var4, var2)) {
            var5 = (short)(var5 | 4);
         }

         if (var5 != 0) {
            this.edgesOnBoundaries.add(var5);
            this.trianglesOnBoundaries.add((short)var1);
         }
      }

   }

   void initAdjacentTriangles() {
      for(int var1 = 0; var1 < this.triangles.size(); var1 += 3) {
         this.initAdjacentTriangles(var1);
      }

   }

   void initAdjacentTriangles(int var1) {
      int var2 = this.adjacentTriangles.size();
      this.adjacentTriangles.add(-1);
      this.adjacentTriangles.add(-1);
      this.adjacentTriangles.add(-1);

      for(int var3 = 0; var3 < this.triangles.size(); var3 += 3) {
         if (var3 != var1) {
            for(int var4 = 0; var4 < 3; ++var4) {
               int var5 = this.getSharedEdge(var1, var4, var3);
               if (var5 != -1) {
                  this.adjacentTriangles.set(var2 + var4, var3 << 16 | var5);
                  break;
               }
            }
         }
      }

   }

   int getSharedEdge(int var1, int var2, int var3) {
      Vector2f var4 = (Vector2f)this.triangles.get(var1 + var2);
      Vector2f var5 = (Vector2f)this.triangles.get(var1 + (var2 + 1) % 3);

      for(int var6 = 0; var6 < 3; ++var6) {
         Vector2f var7 = (Vector2f)this.triangles.get(var3 + var6);
         Vector2f var8 = (Vector2f)this.triangles.get(var3 + (var6 + 1) % 3);
         if (this.isSameEdge(var4, var5, var7, var8)) {
            return var6;
         }
      }

      return -1;
   }

   boolean isSameEdge(Vector2f var1, Vector2f var2, Vector2f var3, Vector2f var4) {
      float var5 = 0.01F;
      return var1.equals(var3, var5) && var2.equals(var4, var5) || var1.equals(var4, var5) && var2.equals(var3, var5);
   }

   boolean isEdgeOnBoundary(Vector2f var1, Vector2f var2) {
      for(int var3 = 0; var3 < this.polygon.size(); ++var3) {
         Vector2f var4 = (Vector2f)this.polygon.get(var3);
         Vector2f var5 = (Vector2f)this.polygon.get((var3 + 1) % this.polygon.size());
         if (var1.equals(var4) && var2.equals(var5) || var1.equals(var5) && var2.equals(var4)) {
            return true;
         }
      }

      return false;
   }

   int getTriangleAt(float var1, float var2) {
      for(int var3 = 0; var3 < this.triangles.size(); var3 += 3) {
         Vector2f var4 = (Vector2f)this.triangles.get(var3);
         Vector2f var5 = (Vector2f)this.triangles.get(var3 + 1);
         Vector2f var6 = (Vector2f)this.triangles.get(var3 + 2);
         if (testPointInTriangle(var1, var2, 0.0F, var4.x, var4.y, 0.0F, var5.x, var5.y, 0.0F, var6.x, var6.y, 0.0F)) {
            return var3;
         }
      }

      return -1;
   }

   public Vector2f pickRandomPoint(Vector2f var1) {
      int var2 = Rand.Next(this.triangles.size() / 3);
      return this.pickRandomPointInTriangle(var2 * 3, var1);
   }

   public Vector2f pickRandomPointInTriangle(int var1, Vector2f var2) {
      Vector2f var3 = (Vector2f)this.triangles.get(var1);
      Vector2f var4 = (Vector2f)this.triangles.get(var1 + 1);
      Vector2f var5 = (Vector2f)this.triangles.get(var1 + 2);
      float var6 = 0.01F;
      float var7 = Rand.Next(0.0F + var6, 1.0F - var6);
      float var8 = Rand.Next(0.0F + var6, 1.0F - var6);
      boolean var11 = var7 + var8 <= 1.0F;
      float var9;
      float var10;
      if (var11) {
         var9 = var7 * (var4.x - var3.x) + var8 * (var5.x - var3.x);
         var10 = var7 * (var4.y - var3.y) + var8 * (var5.y - var3.y);
      } else {
         var9 = (1.0F - var7) * (var4.x - var3.x) + (1.0F - var8) * (var5.x - var3.x);
         var10 = (1.0F - var7) * (var4.y - var3.y) + (1.0F - var8) * (var5.y - var3.y);
      }

      var9 += var3.x;
      var10 += var3.y;
      return var2.set(var9, var10);
   }

   public static boolean testPointInTriangle(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11) {
      float var12 = var6 - var3;
      float var13 = var7 - var4;
      float var14 = var8 - var5;
      float var15 = var9 - var3;
      float var16 = var10 - var4;
      float var17 = var11 - var5;
      float var18 = var12 * var12 + var13 * var13 + var14 * var14;
      float var19 = var12 * var15 + var13 * var16 + var14 * var17;
      float var20 = var15 * var15 + var16 * var16 + var17 * var17;
      float var21 = var18 * var20 - var19 * var19;
      float var22 = var0 - var3;
      float var23 = var1 - var4;
      float var24 = var2 - var5;
      float var25 = var22 * var12 + var23 * var13 + var24 * var14;
      float var26 = var22 * var15 + var23 * var16 + var24 * var17;
      float var27 = var25 * var20 - var26 * var19;
      float var28 = var26 * var18 - var25 * var19;
      float var29 = var27 + var28 - var21;
      return ((long)(Runtime.floatToIntBits(var29) & ~(Runtime.floatToIntBits(var27) | Runtime.floatToIntBits(var28))) & -9223372036854775808L) != 0L;
   }

   int indexOf() {
      return this.meshList.indexOf(this);
   }

   void addConnection(int var1, int var2, Mesh var3, int var4, int var5, Vector2f var6, Vector2f var7) {
      assert var3 != this;

      OffMeshConnection var8 = (OffMeshConnection)OffMeshConnection.pool.alloc();
      var8.triFrom = var1;
      var8.edgeFrom = var2;
      var8.meshTo = var3;
      var8.triTo = var4;
      var8.edgeTo = var5;
      var8.edge1.set(var6);
      var8.edge2.set(var7);
      this.offMeshConnections.add(var8);
   }

   void gatherConnectedMeshes(Collection<Mesh> var1) {
      Iterator var2 = this.offMeshConnections.iterator();

      while(var2.hasNext()) {
         OffMeshConnection var3 = (OffMeshConnection)var2.next();
         if (!var1.contains(var3.meshTo)) {
            var1.add(var3.meshTo);
            var3.meshTo.gatherConnectedMeshes(var1);
         }
      }

   }

   public float getEdgeMidPointX(int var1, int var2) {
      Vector2f var3 = (Vector2f)this.triangles.get(var1 + var2);
      Vector2f var4 = (Vector2f)this.triangles.get(var1 + (var2 + 1) % 3);
      return (var3.x + var4.x) / 2.0F;
   }

   public float getEdgeMidPointY(int var1, int var2) {
      Vector2f var3 = (Vector2f)this.triangles.get(var1 + var2);
      Vector2f var4 = (Vector2f)this.triangles.get(var1 + (var2 + 1) % 3);
      return (var3.y + var4.y) / 2.0F;
   }

   int getTriangleEdgeOnX(int var1, float var2) {
      Vector2f var3 = (Vector2f)this.triangles.get(var1);
      Vector2f var4 = (Vector2f)this.triangles.get(var1 + 1);
      Vector2f var5 = (Vector2f)this.triangles.get(var1 + 2);
      if (PZMath.equal(var3.x, var4.x, 0.001F) && PZMath.equal(var3.x, var2, 0.001F)) {
         return 0;
      } else if (PZMath.equal(var4.x, var5.x, 0.001F) && PZMath.equal(var4.x, var2, 0.001F)) {
         return 1;
      } else {
         return PZMath.equal(var5.x, var3.x, 0.001F) && PZMath.equal(var5.x, var2, 0.001F) ? 2 : -1;
      }
   }

   int getTriangleEdgeOnY(int var1, float var2) {
      Vector2f var3 = (Vector2f)this.triangles.get(var1);
      Vector2f var4 = (Vector2f)this.triangles.get(var1 + 1);
      Vector2f var5 = (Vector2f)this.triangles.get(var1 + 2);
      if (PZMath.equal(var3.y, var4.y, 0.001F) && PZMath.equal(var3.y, var2, 0.001F)) {
         return 0;
      } else if (PZMath.equal(var4.y, var5.y, 0.001F) && PZMath.equal(var4.y, var2, 0.001F)) {
         return 1;
      } else {
         return PZMath.equal(var5.y, var3.y, 0.001F) && PZMath.equal(var5.y, var2, 0.001F) ? 2 : -1;
      }
   }

   void renderTriangleEdges() {
      int var1 = this.meshList.z;

      for(int var2 = 0; var2 < this.triangles.size(); var2 += 3) {
         Vector2f var3 = (Vector2f)this.triangles.get(var2);
         Vector2f var4 = (Vector2f)this.triangles.get(var2 + 1);
         Vector2f var5 = (Vector2f)this.triangles.get(var2 + 2);
         LineDrawer.addLine(var3.x, var3.y, (float)var1, var4.x, var4.y, (float)var1, 1.0F, 1.0F, 1.0F, (String)null, true);
         LineDrawer.addLine(var4.x, var4.y, (float)var1, var5.x, var5.y, (float)var1, 1.0F, 1.0F, 1.0F, (String)null, true);
         LineDrawer.addLine(var5.x, var5.y, (float)var1, var3.x, var3.y, (float)var1, 1.0F, 1.0F, 1.0F, (String)null, true);
      }

   }

   void renderOffMeshConnections() {
      int var1 = this.meshList.z;

      for(int var2 = 0; var2 < this.offMeshConnections.size(); ++var2) {
         OffMeshConnection var3 = (OffMeshConnection)this.offMeshConnections.get(var2);
         Vector2f var4;
         Vector2f var5;
         if (var3.edgeFrom != -1) {
            var4 = (Vector2f)this.triangles.get(var3.triFrom + var3.edgeFrom);
            var5 = (Vector2f)this.triangles.get(var3.triFrom + (var3.edgeFrom + 1) % 3);
            LineDrawer.addLine(var4.x, var4.y, (float)var1, var5.x, var5.y, (float)var1, 1.0F, 0.0F, 0.0F, (String)null, true);
         }

         var4 = (Vector2f)this.triangles.get(var3.triFrom);
         var5 = (Vector2f)this.triangles.get(var3.triFrom + 1);
         Vector2f var6 = (Vector2f)this.triangles.get(var3.triFrom + 2);
         float var7 = (var4.x + var5.x + var6.x) / 3.0F;
         float var8 = (var4.y + var5.y + var6.y) / 3.0F;
         Mesh var9 = var3.meshTo;
         Vector2f var10 = (Vector2f)var9.triangles.get(var3.triTo);
         Vector2f var11 = (Vector2f)var9.triangles.get(var3.triTo + 1);
         Vector2f var12 = (Vector2f)var9.triangles.get(var3.triTo + 2);
         float var13 = (var10.x + var11.x + var12.x) / 3.0F;
         float var14 = (var10.y + var11.y + var12.y) / 3.0F;
         LineDrawer.addLine(var7, var8, (float)var1, var13, var14, (float)var9.meshList.z, 1.0F, 0.0F, 0.0F, (String)null, true);
      }

   }

   void renderOutline() {
      int var1 = this.meshList.z;

      for(int var2 = 0; var2 < this.trianglesOnBoundaries.size(); ++var2) {
         short var3 = this.trianglesOnBoundaries.get(var2);
         short var4 = this.edgesOnBoundaries.get(var2);

         for(int var5 = 0; var5 < 3; ++var5) {
            if ((var4 & 1 << var5) != 0) {
               Vector2f var6 = (Vector2f)this.triangles.get(var3 + var5);
               Vector2f var7 = (Vector2f)this.triangles.get(var3 + (var5 + 1) % 3);
               LineDrawer.addLine(var6.x, var6.y, (float)var1, var7.x, var7.y, (float)var1, 0.0F, 1.0F, 0.0F, (String)null, true);
            }
         }
      }

   }

   public void renderOutline(IPathRenderer var1, float var2, float var3, float var4, float var5) {
      for(int var6 = 0; var6 < this.polygon.size(); ++var6) {
         float var7 = ((Vector2f)this.polygon.get(var6)).x();
         float var8 = ((Vector2f)this.polygon.get(var6)).y();
         float var9 = ((Vector2f)this.polygon.get((var6 + 1) % this.polygon.size())).x();
         float var10 = ((Vector2f)this.polygon.get((var6 + 1) % this.polygon.size())).y();
         var1.drawLine(var7, var8, var9, var10, var2, var3, var4, var5);
      }

   }

   public void renderPoints(IPathRenderer var1, float var2, float var3, float var4, float var5) {
      for(int var6 = 0; var6 < this.polygon.size(); ++var6) {
         float var7 = ((Vector2f)this.polygon.get(var6)).x();
         float var8 = ((Vector2f)this.polygon.get(var6)).y();
         var1.drawRect(var7 - 0.5F, var8 - 0.5F, 1.0F, 1.0F, var2, var3, var4, var5);
      }

   }

   public void renderTriangles(IPathRenderer var1, float var2, float var3, float var4, float var5) {
      for(int var6 = 0; var6 < this.triangles.size(); var6 += 3) {
         float var7 = ((Vector2f)this.triangles.get(var6)).x();
         float var8 = ((Vector2f)this.triangles.get(var6)).y();
         float var9 = ((Vector2f)this.triangles.get(var6 + 1)).x();
         float var10 = ((Vector2f)this.triangles.get(var6 + 1)).y();
         float var11 = ((Vector2f)this.triangles.get(var6 + 2)).x();
         float var12 = ((Vector2f)this.triangles.get(var6 + 2)).y();
         var1.drawLine(var7, var8, var9, var10, var2, var3, var4, var5);
         var1.drawLine(var9, var10, var11, var12, var2, var3, var4, var5);
         var1.drawLine(var7, var8, var11, var12, var2, var3, var4, var5);
      }

   }

   public void renderOffMeshConnections(IPathRenderer var1, float var2, float var3, float var4, float var5) {
      float var6 = 1.0F;

      for(int var7 = 0; var7 < this.offMeshConnections.size(); ++var7) {
         OffMeshConnection var8 = (OffMeshConnection)this.offMeshConnections.get(var7);
         var1.drawRect((var8.edge1.x + var8.edge2.x) / 2.0F - var6 / 2.0F, (var8.edge1.y + var8.edge2.y) / 2.0F - var6 / 2.0F, var6, var6, var2, var3, var4, var5);
      }

   }
}
