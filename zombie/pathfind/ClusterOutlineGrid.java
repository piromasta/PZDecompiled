package zombie.pathfind;

import java.util.ArrayList;

final class ClusterOutlineGrid {
   ClusterOutline[] elements;
   int W;
   int H;

   ClusterOutlineGrid() {
   }

   ClusterOutlineGrid setSize(int var1, int var2) {
      if (this.elements == null || this.elements.length < var1 * var2) {
         this.elements = new ClusterOutline[var1 * var2];
      }

      this.W = var1;
      this.H = var2;
      return this;
   }

   void releaseElements() {
      for(int var1 = 0; var1 < this.H; ++var1) {
         for(int var2 = 0; var2 < this.W; ++var2) {
            if (this.elements[var2 + var1 * this.W] != null) {
               this.elements[var2 + var1 * this.W].release();
               this.elements[var2 + var1 * this.W] = null;
            }
         }
      }

   }

   void setInner(int var1, int var2, int var3) {
      ClusterOutline var4 = this.get(var1, var2, var3);
      if (var4 != null) {
         var4.inner = true;
      }

   }

   void setWest(int var1, int var2, int var3) {
      ClusterOutline var4 = this.get(var1, var2, var3);
      if (var4 != null) {
         var4.w = true;
      }

   }

   void setNorth(int var1, int var2, int var3) {
      ClusterOutline var4 = this.get(var1, var2, var3);
      if (var4 != null) {
         var4.n = true;
      }

   }

   void setEast(int var1, int var2, int var3) {
      ClusterOutline var4 = this.get(var1, var2, var3);
      if (var4 != null) {
         var4.e = true;
      }

   }

   void setSouth(int var1, int var2, int var3) {
      ClusterOutline var4 = this.get(var1, var2, var3);
      if (var4 != null) {
         var4.s = true;
      }

   }

   boolean canTrace_W(int var1, int var2, int var3) {
      ClusterOutline var4 = this.get(var1, var2, var3);
      return var4 != null && var4.inner && var4.w && !var4.tw;
   }

   boolean canTrace_N(int var1, int var2, int var3) {
      ClusterOutline var4 = this.get(var1, var2, var3);
      return var4 != null && var4.inner && var4.n && !var4.tn;
   }

   boolean canTrace_E(int var1, int var2, int var3) {
      ClusterOutline var4 = this.get(var1, var2, var3);
      return var4 != null && var4.inner && var4.e && !var4.te;
   }

   boolean canTrace_S(int var1, int var2, int var3) {
      ClusterOutline var4 = this.get(var1, var2, var3);
      return var4 != null && var4.inner && var4.s && !var4.ts;
   }

   boolean isInner(int var1, int var2, int var3) {
      ClusterOutline var4 = this.get(var1, var2, var3);
      return var4 != null && (var4.start || var4.inner);
   }

   ClusterOutline get(int var1, int var2, int var3) {
      if (var1 >= 0 && var1 < this.W) {
         if (var2 >= 0 && var2 < this.H) {
            if (this.elements[var1 + var2 * this.W] == null) {
               this.elements[var1 + var2 * this.W] = ClusterOutline.alloc().init(var1, var2, var3);
            }

            return this.elements[var1 + var2 * this.W];
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   void trace_W(ClusterOutline var1, ArrayList<Node> var2, Node var3) {
      int var4 = var1.x;
      int var5 = var1.y;
      int var6 = var1.z;
      if (var3 != null) {
         var3.setXY((float)var4, (float)var5);
      } else {
         Node var7 = Node.alloc().init((float)var4, (float)var5, var6);
         var2.add(var7);
      }

      var1.tw = true;
      if (this.canTrace_S(var4 - 1, var5 - 1, var6)) {
         this.get(var4, var5 - 1, var6).innerCorner = true;
         this.trace_S(this.get(var4 - 1, var5 - 1, var6), var2, (Node)null);
      } else if (this.canTrace_W(var4, var5 - 1, var6)) {
         this.trace_W(this.get(var4, var5 - 1, var6), var2, (Node)var2.get(var2.size() - 1));
      } else if (this.canTrace_N(var4, var5, var6)) {
         this.trace_N(var1, var2, (Node)null);
      }

   }

   void trace_N(ClusterOutline var1, ArrayList<Node> var2, Node var3) {
      int var4 = var1.x;
      int var5 = var1.y;
      int var6 = var1.z;
      if (var3 != null) {
         var3.setXY((float)(var4 + 1), (float)var5);
      } else {
         Node var7 = Node.alloc().init((float)(var4 + 1), (float)var5, var6);
         var2.add(var7);
      }

      var1.tn = true;
      if (this.canTrace_W(var4 + 1, var5 - 1, var6)) {
         this.get(var4 + 1, var5, var6).innerCorner = true;
         this.trace_W(this.get(var4 + 1, var5 - 1, var6), var2, (Node)null);
      } else if (this.canTrace_N(var4 + 1, var5, var6)) {
         this.trace_N(this.get(var4 + 1, var5, var6), var2, (Node)var2.get(var2.size() - 1));
      } else if (this.canTrace_E(var4, var5, var6)) {
         this.trace_E(var1, var2, (Node)null);
      }

   }

   void trace_E(ClusterOutline var1, ArrayList<Node> var2, Node var3) {
      int var4 = var1.x;
      int var5 = var1.y;
      int var6 = var1.z;
      if (var3 != null) {
         var3.setXY((float)(var4 + 1), (float)(var5 + 1));
      } else {
         Node var7 = Node.alloc().init((float)(var4 + 1), (float)(var5 + 1), var6);
         var2.add(var7);
      }

      var1.te = true;
      if (this.canTrace_N(var4 + 1, var5 + 1, var6)) {
         this.get(var4, var5 + 1, var6).innerCorner = true;
         this.trace_N(this.get(var4 + 1, var5 + 1, var6), var2, (Node)null);
      } else if (this.canTrace_E(var4, var5 + 1, var6)) {
         this.trace_E(this.get(var4, var5 + 1, var6), var2, (Node)var2.get(var2.size() - 1));
      } else if (this.canTrace_S(var4, var5, var6)) {
         this.trace_S(var1, var2, (Node)null);
      }

   }

   void trace_S(ClusterOutline var1, ArrayList<Node> var2, Node var3) {
      int var4 = var1.x;
      int var5 = var1.y;
      int var6 = var1.z;
      if (var3 != null) {
         var3.setXY((float)var4, (float)(var5 + 1));
      } else {
         Node var7 = Node.alloc().init((float)var4, (float)(var5 + 1), var6);
         var2.add(var7);
      }

      var1.ts = true;
      if (this.canTrace_E(var4 - 1, var5 + 1, var6)) {
         this.get(var4 - 1, var5, var6).innerCorner = true;
         this.trace_E(this.get(var4 - 1, var5 + 1, var6), var2, (Node)null);
      } else if (this.canTrace_S(var4 - 1, var5, var6)) {
         this.trace_S(this.get(var4 - 1, var5, var6), var2, (Node)var2.get(var2.size() - 1));
      } else if (this.canTrace_W(var4, var5, var6)) {
         this.trace_W(var1, var2, (Node)null);
      }

   }

   ArrayList<Node> trace(ClusterOutline var1) {
      int var2 = var1.x;
      int var3 = var1.y;
      int var4 = var1.z;
      ArrayList var5 = new ArrayList();
      Node var6 = Node.alloc().init((float)var2, (float)var3, var4);
      var5.add(var6);
      var1.start = true;
      this.trace_N(var1, var5, (Node)null);
      Node var7 = (Node)var5.get(var5.size() - 1);
      float var8 = 0.1F;
      if ((int)(var6.x + var8) == (int)(var7.x + var8) && (int)(var6.y + var8) == (int)(var7.y + var8)) {
         var7.release();
         var5.set(var5.size() - 1, var6);
      }

      return var5;
   }
}
