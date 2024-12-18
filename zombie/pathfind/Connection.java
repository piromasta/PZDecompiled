package zombie.pathfind;

import java.util.ArrayDeque;

public final class Connection {
   public Node node1;
   public Node node2;
   int flags;
   static final ArrayDeque<Connection> pool = new ArrayDeque();

   public Connection() {
   }

   Connection init(Node var1, Node var2, int var3) {
      this.node1 = var1;
      this.node2 = var2;
      this.flags = var3;
      return this;
   }

   public Node otherNode(Node var1) {
      assert var1 == this.node1 || var1 == this.node2;

      return var1 == this.node1 ? this.node2 : this.node1;
   }

   public boolean has(int var1) {
      return (this.flags & var1) != 0;
   }

   static Connection alloc() {
      boolean var0;
      if (pool.isEmpty()) {
         var0 = false;
      } else {
         var0 = false;
      }

      return pool.isEmpty() ? new Connection() : (Connection)pool.pop();
   }

   void release() {
      assert !pool.contains(this);

      pool.push(this);
   }
}
