package zombie.characters.animals.pathfind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.joml.Vector2f;
import zombie.core.random.Rand;
import zombie.iso.zones.Zone;
import zombie.util.list.PZArrayUtil;
import zombie.worldMap.UIWorldMap;

public final class MeshWanderer {
   Zone m_zone;
   Mesh m_mesh;
   int m_triangleIndex;
   float m_x;
   float m_y;
   int m_targetTriangleIndex;
   int m_targetTriangleEdge;
   float m_targetX;
   float m_targetY;
   boolean m_bMovingToOtherTriangle = false;

   public MeshWanderer() {
   }

   void update() {
      float var1 = Vector2f.length(this.m_targetX - this.m_x, this.m_targetY - this.m_y);
      if (var1 <= 1.0F) {
         if (!this.m_bMovingToOtherTriangle && !Rand.NextBool(2)) {
            Vector2f var6 = this.m_mesh.pickRandomPointInTriangle(this.m_triangleIndex, new Vector2f());
            this.m_targetX = var6.x;
            this.m_targetY = var6.y;
         } else if (!this.m_bMovingToOtherTriangle) {
            this.m_targetX = this.m_mesh.getEdgeMidPointX(this.m_targetTriangleIndex, this.m_targetTriangleEdge);
            this.m_targetY = this.m_mesh.getEdgeMidPointY(this.m_targetTriangleIndex, this.m_targetTriangleEdge);
            this.m_bMovingToOtherTriangle = true;
         } else {
            int var5 = this.m_triangleIndex;
            this.m_triangleIndex = this.m_targetTriangleIndex;
            this.m_targetTriangleIndex = this.chooseNextTriangle(this.m_triangleIndex);
            Vector2f var7 = this.m_mesh.pickRandomPointInTriangle(this.m_triangleIndex, new Vector2f());
            this.m_targetX = var7.x;
            this.m_targetY = var7.y;
            this.m_bMovingToOtherTriangle = false;
         }
      } else {
         float var2 = 1.0F;
         float var3 = (this.m_targetX - this.m_x) / var1 * var2;
         float var4 = (this.m_targetY - this.m_y) / var1 * var2;
         this.m_x += var3;
         this.m_y += var4;
      }
   }

   int chooseNextTriangle(int var1) {
      ArrayList var2 = new ArrayList();

      int var3;
      int var4;
      for(var3 = 0; var3 < 3; ++var3) {
         var4 = this.m_mesh.adjacentTriangles.get(var1 + var3);
         if (var4 != -1) {
            var2.add(var4);
         }
      }

      if (var2.isEmpty()) {
         var3 = Rand.Next(3);
         this.m_targetX = this.m_mesh.getEdgeMidPointX(var1, var3);
         this.m_targetY = this.m_mesh.getEdgeMidPointY(var1, var3);
         this.m_targetTriangleEdge = var3;
         return var1;
      } else {
         var3 = (Integer)PZArrayUtil.pickRandom((List)var2);
         var4 = var3 >> 16 & '\uffff';
         int var5 = var3 & '\uffff';
         this.m_targetX = this.m_mesh.getEdgeMidPointX(var4, var5);
         this.m_targetY = this.m_mesh.getEdgeMidPointY(var4, var5);
         this.m_targetTriangleEdge = var5;
         return var4;
      }
   }

   public void renderPath(UIWorldMap var1, Zone var2, float var3, float var4, float var5, float var6, IPathRenderer var7, Mesh var8, HashMap<Mesh, Zone> var9) {
      int var10;
      if (this.m_zone != var9.get(var8)) {
         this.m_zone = (Zone)var9.get(var8);
         this.m_mesh = var8;
         this.m_triangleIndex = 0;
         var10 = var8.adjacentTriangles.get(this.m_triangleIndex);
         if (var10 == -1) {
            var10 = var8.adjacentTriangles.get(this.m_triangleIndex + 1);
         }

         if (var10 == -1) {
            var10 = var8.adjacentTriangles.get(this.m_triangleIndex + 2);
         }

         this.m_targetTriangleIndex = var10 >> 16 & '\uffff';
         this.m_targetTriangleEdge = var10 & '\uffff';
         this.m_x = ((Vector2f)var8.triangles.get(this.m_triangleIndex)).x();
         this.m_y = ((Vector2f)var8.triangles.get(this.m_triangleIndex)).y();
         this.m_targetX = var8.getEdgeMidPointX(this.m_targetTriangleIndex, this.m_targetTriangleEdge);
         this.m_targetY = var8.getEdgeMidPointY(this.m_targetTriangleIndex, this.m_targetTriangleEdge);
      }

      this.update();
      var7.drawLine(this.m_x, this.m_y, this.m_targetX, this.m_targetY, 1.0F, 1.0F, 0.0F, 1.0F);
      var7.drawRect(this.m_x - 1.0F, this.m_y - 1.0F, 2.0F, 2.0F, 0.0F, 1.0F, 0.0F, 1.0F);
      var7.drawTriangleCentroid(var8, this.m_targetTriangleIndex, 1.0F, 0.0F, 0.0F, 1.0F);

      for(var10 = 0; var10 < 3; ++var10) {
         int var11 = var8.adjacentTriangles.get(this.m_triangleIndex + var10);
         if (var11 != -1) {
            int var12 = var11 >> 16 & '\uffff';
            var7.drawTriangleCentroid(var8, var12, 1.0F, 0.0F, 1.0F, 1.0F);
         }
      }

   }
}
