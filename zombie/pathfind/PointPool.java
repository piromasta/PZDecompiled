package zombie.pathfind;

import java.util.ArrayDeque;

final class PointPool {
   final ArrayDeque<Point> pool = new ArrayDeque();

   PointPool() {
   }

   Point alloc() {
      return this.pool.isEmpty() ? new Point() : (Point)this.pool.pop();
   }

   void release(Point var1) {
      this.pool.push(var1);
   }
}
