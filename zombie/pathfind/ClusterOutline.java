package zombie.pathfind;

import java.util.ArrayDeque;

final class ClusterOutline {
   int x;
   int y;
   int z;
   boolean w;
   boolean n;
   boolean e;
   boolean s;
   boolean tw;
   boolean tn;
   boolean te;
   boolean ts;
   boolean inner;
   boolean innerCorner;
   boolean start;
   static final ArrayDeque<ClusterOutline> pool = new ArrayDeque();

   ClusterOutline() {
   }

   ClusterOutline init(int var1, int var2, int var3) {
      this.x = var1;
      this.y = var2;
      this.z = var3;
      this.w = this.n = this.e = this.s = false;
      this.tw = this.tn = this.te = this.ts = false;
      this.inner = this.innerCorner = this.start = false;
      return this;
   }

   static ClusterOutline alloc() {
      return pool.isEmpty() ? new ClusterOutline() : (ClusterOutline)pool.pop();
   }

   void release() {
      assert !pool.contains(this);

      pool.push(this);
   }
}
