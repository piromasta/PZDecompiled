package zombie.pathfind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.iso.Vector2;

final class EdgeRing extends ArrayList<Edge> {
   static final ArrayDeque<EdgeRing> pool = new ArrayDeque();

   EdgeRing() {
   }

   public boolean add(Edge var1) {
      assert !this.contains(var1);

      return super.add(var1);
   }

   public boolean hasNode(Node var1) {
      for(int var2 = 0; var2 < this.size(); ++var2) {
         Edge var3 = (Edge)this.get(var2);
         if (var3.hasNode(var1)) {
            return true;
         }
      }

      return false;
   }

   boolean hasAdjacentNodes(Node var1, Node var2) {
      for(int var3 = 0; var3 < this.size(); ++var3) {
         Edge var4 = (Edge)this.get(var3);
         if (var4.hasNode(var1) && var4.hasNode(var2)) {
            return true;
         }
      }

      return false;
   }

   boolean isPointInPolygon_CrossingNumber(float var1, float var2) {
      int var3 = 0;

      for(int var4 = 0; var4 < this.size(); ++var4) {
         Edge var5 = (Edge)this.get(var4);
         if (var5.node1.y <= var2 && var5.node2.y > var2 || var5.node1.y > var2 && var5.node2.y <= var2) {
            float var6 = (var2 - var5.node1.y) / (var5.node2.y - var5.node1.y);
            if (var1 < var5.node1.x + var6 * (var5.node2.x - var5.node1.x)) {
               ++var3;
            }
         }
      }

      return var3 % 2 == 1;
   }

   float isLeft(float var1, float var2, float var3, float var4, float var5, float var6) {
      return (var3 - var1) * (var6 - var2) - (var5 - var1) * (var4 - var2);
   }

   EdgeRingHit isPointInPolygon_WindingNumber(float var1, float var2, int var3) {
      int var4 = 0;

      for(int var5 = 0; var5 < this.size(); ++var5) {
         Edge var6 = (Edge)this.get(var5);
         if ((var3 & 16) != 0 && var6.isPointOn(var1, var2)) {
            return EdgeRingHit.OnEdge;
         }

         if (var6.node1.y <= var2) {
            if (var6.node2.y > var2 && this.isLeft(var6.node1.x, var6.node1.y, var6.node2.x, var6.node2.y, var1, var2) > 0.0F) {
               ++var4;
            }
         } else if (var6.node2.y <= var2 && this.isLeft(var6.node1.x, var6.node1.y, var6.node2.x, var6.node2.y, var1, var2) < 0.0F) {
            --var4;
         }
      }

      return var4 == 0 ? EdgeRingHit.Outside : EdgeRingHit.Inside;
   }

   boolean lineSegmentIntersects(float var1, float var2, float var3, float var4) {
      Vector2 var5 = L_lineSegmentIntersects.v1;
      var5.set(var3 - var1, var4 - var2);
      float var6 = var5.getLength();
      var5.normalize();
      float var7 = var5.x;
      float var8 = var5.y;

      for(int var9 = 0; var9 < this.size(); ++var9) {
         Edge var10 = (Edge)this.get(var9);
         if (!var10.isPointOn(var1, var2) && !var10.isPointOn(var3, var4)) {
            float var11 = var10.normal.dot(var5);
            if (var11 >= 0.01F) {
            }

            float var12 = var10.node1.x;
            float var13 = var10.node1.y;
            float var14 = var10.node2.x;
            float var15 = var10.node2.y;
            float var16 = var1 - var12;
            float var17 = var2 - var13;
            float var18 = var14 - var12;
            float var19 = var15 - var13;
            float var20 = 1.0F / (var19 * var7 - var18 * var8);
            float var21 = (var18 * var17 - var19 * var16) * var20;
            if (var21 >= 0.0F && var21 <= var6) {
               float var22 = (var17 * var7 - var16 * var8) * var20;
               if (var22 >= 0.0F && var22 <= 1.0F) {
                  return true;
               }
            }
         }
      }

      if (this.isPointInPolygon_WindingNumber((var1 + var3) / 2.0F, (var2 + var4) / 2.0F, 0) != EdgeRingHit.Outside) {
         return true;
      } else {
         return false;
      }
   }

   void getClosestPointOnEdge(float var1, float var2, ClosestPointOnEdge var3) {
      for(int var4 = 0; var4 < this.size(); ++var4) {
         Edge var5 = (Edge)this.get(var4);
         var5.getClosestPointOnEdge(var1, var2, var3);
      }

   }

   static EdgeRing alloc() {
      return pool.isEmpty() ? new EdgeRing() : (EdgeRing)pool.pop();
   }

   public void release() {
      Edge.releaseAll(this);
   }

   static void releaseAll(ArrayList<EdgeRing> var0) {
      for(int var1 = 0; var1 < var0.size(); ++var1) {
         ((EdgeRing)var0.get(var1)).release();
      }

   }
}
