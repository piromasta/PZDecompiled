package zombie.pathfind;

import java.util.ArrayDeque;
import java.util.ArrayList;

/** @deprecated */
@Deprecated
final class ObjectOutline {
   int x;
   int y;
   int z;
   boolean nw;
   boolean nw_w;
   boolean nw_n;
   boolean nw_e;
   boolean nw_s;
   boolean w_w;
   boolean w_e;
   boolean w_cutoff;
   boolean n_n;
   boolean n_s;
   boolean n_cutoff;
   ArrayList<Node> nodes;
   static final ArrayDeque<ObjectOutline> pool = new ArrayDeque();

   ObjectOutline() {
   }

   ObjectOutline init(int var1, int var2, int var3) {
      this.x = var1;
      this.y = var2;
      this.z = var3;
      this.nw = this.nw_w = this.nw_n = this.nw_e = false;
      this.w_w = this.w_e = this.w_cutoff = false;
      this.n_n = this.n_s = this.n_cutoff = false;
      return this;
   }

   static void setSolid(int var0, int var1, int var2, ObjectOutline[][] var3) {
      setWest(var0, var1, var2, var3);
      setNorth(var0, var1, var2, var3);
      setWest(var0 + 1, var1, var2, var3);
      setNorth(var0, var1 + 1, var2, var3);
   }

   static void setWest(int var0, int var1, int var2, ObjectOutline[][] var3) {
      ObjectOutline var4 = get(var0, var1, var2, var3);
      if (var4 != null) {
         if (var4.nw) {
            var4.nw_s = false;
         } else {
            var4.nw = true;
            var4.nw_w = true;
            var4.nw_n = true;
            var4.nw_e = true;
            var4.nw_s = false;
         }

         var4.w_w = true;
         var4.w_e = true;
      }

      ObjectOutline var5 = var4;
      var4 = get(var0, var1 + 1, var2, var3);
      if (var4 == null) {
         if (var5 != null) {
            var5.w_cutoff = true;
         }
      } else if (var4.nw) {
         var4.nw_n = false;
      } else {
         var4.nw = true;
         var4.nw_n = false;
         var4.nw_w = true;
         var4.nw_e = true;
         var4.nw_s = true;
      }

   }

   static void setNorth(int var0, int var1, int var2, ObjectOutline[][] var3) {
      ObjectOutline var4 = get(var0, var1, var2, var3);
      if (var4 != null) {
         if (var4.nw) {
            var4.nw_e = false;
         } else {
            var4.nw = true;
            var4.nw_w = true;
            var4.nw_n = true;
            var4.nw_e = false;
            var4.nw_s = true;
         }

         var4.n_n = true;
         var4.n_s = true;
      }

      ObjectOutline var5 = var4;
      var4 = get(var0 + 1, var1, var2, var3);
      if (var4 == null) {
         if (var5 != null) {
            var5.n_cutoff = true;
         }
      } else if (var4.nw) {
         var4.nw_w = false;
      } else {
         var4.nw = true;
         var4.nw_n = true;
         var4.nw_w = false;
         var4.nw_e = true;
         var4.nw_s = true;
      }

   }

   static ObjectOutline get(int var0, int var1, int var2, ObjectOutline[][] var3) {
      if (var0 >= 0 && var0 < var3.length) {
         if (var1 >= 0 && var1 < var3[0].length) {
            if (var3[var0][var1] == null) {
               var3[var0][var1] = alloc().init(var0, var1, var2);
            }

            return var3[var0][var1];
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   void trace_NW_N(ObjectOutline[][] var1, Node var2) {
      if (var2 != null) {
         var2.setXY((float)this.x + 0.3F, (float)this.y - 0.3F);
      } else {
         Node var3 = Node.alloc().init((float)this.x + 0.3F, (float)this.y - 0.3F, this.z);
         this.nodes.add(var3);
      }

      this.nw_n = false;
      if (this.nw_e) {
         this.trace_NW_E(var1, (Node)null);
      } else if (this.n_n) {
         this.trace_N_N(var1, (Node)this.nodes.get(this.nodes.size() - 1));
      }

   }

   void trace_NW_S(ObjectOutline[][] var1, Node var2) {
      if (var2 != null) {
         var2.setXY((float)this.x - 0.3F, (float)this.y + 0.3F);
      } else {
         Node var3 = Node.alloc().init((float)this.x - 0.3F, (float)this.y + 0.3F, this.z);
         this.nodes.add(var3);
      }

      this.nw_s = false;
      if (this.nw_w) {
         this.trace_NW_W(var1, (Node)null);
      } else {
         ObjectOutline var4 = get(this.x - 1, this.y, this.z, var1);
         if (var4 == null) {
            return;
         }

         if (var4.n_s) {
            var4.nodes = this.nodes;
            var4.trace_N_S(var1, (Node)this.nodes.get(this.nodes.size() - 1));
         }
      }

   }

   void trace_NW_W(ObjectOutline[][] var1, Node var2) {
      if (var2 != null) {
         var2.setXY((float)this.x - 0.3F, (float)this.y - 0.3F);
      } else {
         Node var3 = Node.alloc().init((float)this.x - 0.3F, (float)this.y - 0.3F, this.z);
         this.nodes.add(var3);
      }

      this.nw_w = false;
      if (this.nw_n) {
         this.trace_NW_N(var1, (Node)null);
      } else {
         ObjectOutline var4 = get(this.x, this.y - 1, this.z, var1);
         if (var4 == null) {
            return;
         }

         if (var4.w_w) {
            var4.nodes = this.nodes;
            var4.trace_W_W(var1, (Node)this.nodes.get(this.nodes.size() - 1));
         }
      }

   }

   void trace_NW_E(ObjectOutline[][] var1, Node var2) {
      if (var2 != null) {
         var2.setXY((float)this.x + 0.3F, (float)this.y + 0.3F);
      } else {
         Node var3 = Node.alloc().init((float)this.x + 0.3F, (float)this.y + 0.3F, this.z);
         this.nodes.add(var3);
      }

      this.nw_e = false;
      if (this.nw_s) {
         this.trace_NW_S(var1, (Node)null);
      } else if (this.w_e) {
         this.trace_W_E(var1, (Node)this.nodes.get(this.nodes.size() - 1));
      }

   }

   void trace_W_E(ObjectOutline[][] var1, Node var2) {
      Node var3;
      if (var2 != null) {
         var2.setXY((float)this.x + 0.3F, (float)(this.y + 1) - 0.3F);
      } else {
         var3 = Node.alloc().init((float)this.x + 0.3F, (float)(this.y + 1) - 0.3F, this.z);
         this.nodes.add(var3);
      }

      this.w_e = false;
      if (this.w_cutoff) {
         var3 = (Node)this.nodes.get(this.nodes.size() - 1);
         var3.setXY((float)this.x + 0.3F, (float)(this.y + 1) + 0.3F);
         var3 = Node.alloc().init((float)this.x - 0.3F, (float)(this.y + 1) + 0.3F, this.z);
         this.nodes.add(var3);
         var3 = Node.alloc().init((float)this.x - 0.3F, (float)(this.y + 1) - 0.3F, this.z);
         this.nodes.add(var3);
         this.trace_W_W(var1, var3);
      } else {
         ObjectOutline var4 = get(this.x, this.y + 1, this.z, var1);
         if (var4 != null) {
            if (var4.nw && var4.nw_e) {
               var4.nodes = this.nodes;
               var4.trace_NW_E(var1, (Node)this.nodes.get(this.nodes.size() - 1));
            } else if (var4.n_n) {
               var4.nodes = this.nodes;
               var4.trace_N_N(var1, (Node)null);
            }

         }
      }
   }

   void trace_W_W(ObjectOutline[][] var1, Node var2) {
      if (var2 != null) {
         var2.setXY((float)this.x - 0.3F, (float)this.y + 0.3F);
      } else {
         Node var3 = Node.alloc().init((float)this.x - 0.3F, (float)this.y + 0.3F, this.z);
         this.nodes.add(var3);
      }

      this.w_w = false;
      if (this.nw_w) {
         this.trace_NW_W(var1, (Node)this.nodes.get(this.nodes.size() - 1));
      } else {
         ObjectOutline var4 = get(this.x - 1, this.y, this.z, var1);
         if (var4 == null) {
            return;
         }

         if (var4.n_s) {
            var4.nodes = this.nodes;
            var4.trace_N_S(var1, (Node)null);
         }
      }

   }

   void trace_N_N(ObjectOutline[][] var1, Node var2) {
      Node var3;
      if (var2 != null) {
         var2.setXY((float)(this.x + 1) - 0.3F, (float)this.y - 0.3F);
      } else {
         var3 = Node.alloc().init((float)(this.x + 1) - 0.3F, (float)this.y - 0.3F, this.z);
         this.nodes.add(var3);
      }

      this.n_n = false;
      if (this.n_cutoff) {
         var3 = (Node)this.nodes.get(this.nodes.size() - 1);
         var3.setXY((float)(this.x + 1) + 0.3F, (float)this.y - 0.3F);
         var3 = Node.alloc().init((float)(this.x + 1) + 0.3F, (float)this.y + 0.3F, this.z);
         this.nodes.add(var3);
         var3 = Node.alloc().init((float)(this.x + 1) - 0.3F, (float)this.y + 0.3F, this.z);
         this.nodes.add(var3);
         this.trace_N_S(var1, var3);
      } else {
         ObjectOutline var4 = get(this.x + 1, this.y, this.z, var1);
         if (var4 != null) {
            if (var4.nw_n) {
               var4.nodes = this.nodes;
               var4.trace_NW_N(var1, (Node)this.nodes.get(this.nodes.size() - 1));
            } else {
               var4 = get(this.x + 1, this.y - 1, this.z, var1);
               if (var4 == null) {
                  return;
               }

               if (var4.w_w) {
                  var4.nodes = this.nodes;
                  var4.trace_W_W(var1, (Node)null);
               }
            }

         }
      }
   }

   void trace_N_S(ObjectOutline[][] var1, Node var2) {
      if (var2 != null) {
         var2.setXY((float)this.x + 0.3F, (float)this.y + 0.3F);
      } else {
         Node var3 = Node.alloc().init((float)this.x + 0.3F, (float)this.y + 0.3F, this.z);
         this.nodes.add(var3);
      }

      this.n_s = false;
      if (this.nw_s) {
         this.trace_NW_S(var1, (Node)this.nodes.get(this.nodes.size() - 1));
      } else if (this.w_e) {
         this.trace_W_E(var1, (Node)null);
      }

   }

   void trace(ObjectOutline[][] var1, ArrayList<Node> var2) {
      var2.clear();
      this.nodes = var2;
      Node var3 = Node.alloc().init((float)this.x - 0.3F, (float)this.y - 0.3F, this.z);
      var2.add(var3);
      this.trace_NW_N(var1, (Node)null);
      if (var2.size() != 2 && var3.x == ((Node)var2.get(var2.size() - 1)).x && var3.y == ((Node)var2.get(var2.size() - 1)).y) {
         ((Node)var2.get(var2.size() - 1)).release();
         var2.set(var2.size() - 1, var3);
      } else {
         var2.clear();
      }

   }

   static ObjectOutline alloc() {
      return pool.isEmpty() ? new ObjectOutline() : (ObjectOutline)pool.pop();
   }

   void release() {
      assert !pool.contains(this);

      pool.push(this);
   }
}
