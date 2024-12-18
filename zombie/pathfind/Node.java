package zombie.pathfind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.core.math.PZMath;

public final class Node {
   static int nextID = 1;
   final int ID;
   public float x;
   public float y;
   public int z;
   boolean ignore;
   public Square square;
   public ArrayList<VisibilityGraph> graphs;
   public final ArrayList<Edge> edges = new ArrayList();
   public final ArrayList<Connection> visible = new ArrayList();
   int flags = 0;
   static final ArrayList<Obstacle> tempObstacles = new ArrayList();
   static final ArrayDeque<Node> pool = new ArrayDeque();

   Node() {
      this.ID = nextID++;
   }

   public Node init(float var1, float var2, int var3) {
      this.x = var1;
      this.y = var2;
      this.z = var3;
      this.ignore = false;
      this.square = null;
      if (this.graphs != null) {
         this.graphs.clear();
      }

      this.edges.clear();
      this.visible.clear();
      this.flags = 0;
      return this;
   }

   Node init(Square var1) {
      this.x = (float)var1.x + 0.5F;
      this.y = (float)var1.y + 0.5F;
      this.z = var1.z;
      this.ignore = false;
      this.square = var1;
      if (this.graphs != null) {
         this.graphs.clear();
      }

      this.edges.clear();
      this.visible.clear();
      this.flags = 0;
      return this;
   }

   Node setXY(float var1, float var2) {
      this.x = var1;
      this.y = var2;
      return this;
   }

   void addGraph(VisibilityGraph var1) {
      if (this.graphs == null) {
         this.graphs = new ArrayList();
      }

      assert !this.graphs.contains(var1);

      this.graphs.add(var1);
   }

   boolean sharesEdge(Node var1) {
      for(int var2 = 0; var2 < this.edges.size(); ++var2) {
         Edge var3 = (Edge)this.edges.get(var2);
         if (var3.hasNode(var1)) {
            return true;
         }
      }

      return false;
   }

   boolean sharesShape(Node var1) {
      for(int var2 = 0; var2 < this.edges.size(); ++var2) {
         Edge var3 = (Edge)this.edges.get(var2);

         for(int var4 = 0; var4 < var1.edges.size(); ++var4) {
            Edge var5 = (Edge)var1.edges.get(var4);
            if (var3.obstacle != null && var3.obstacle == var5.obstacle) {
               return true;
            }
         }
      }

      return false;
   }

   void createGraphsIfNeeded() {
      if (this.graphs != null) {
         for(int var1 = 0; var1 < this.graphs.size(); ++var1) {
            VisibilityGraph var2 = (VisibilityGraph)this.graphs.get(var1);
            if (!var2.created) {
               var2.create();
            }
         }

      }
   }

   void getObstacles(ArrayList<Obstacle> var1) {
      for(int var2 = 0; var2 < this.edges.size(); ++var2) {
         Edge var3 = (Edge)this.edges.get(var2);
         if (!var1.contains(var3.obstacle)) {
            var1.add(var3.obstacle);
         }
      }

   }

   boolean onSameShapeButDoesNotShareAnEdge(Node var1) {
      tempObstacles.clear();
      this.getObstacles(tempObstacles);

      for(int var2 = 0; var2 < tempObstacles.size(); ++var2) {
         Obstacle var3 = (Obstacle)tempObstacles.get(var2);
         if (var3.hasNode(var1) && !var3.hasAdjacentNodes(this, var1)) {
            return true;
         }
      }

      return false;
   }

   public boolean hasFlag(int var1) {
      return (this.flags & var1) != 0;
   }

   boolean isConnectedTo(Node var1) {
      if (this.hasFlag(4)) {
         return true;
      } else {
         for(int var2 = 0; var2 < this.visible.size(); ++var2) {
            Connection var3 = (Connection)this.visible.get(var2);
            if (var3.node1 == var1 || var3.node2 == var1) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean isOnEdgeOfLoadedArea() {
      int var1 = PZMath.fastfloor(this.x);
      int var2 = PZMath.fastfloor(this.y);
      if (PZMath.coordmodulo(var1, 8) == 0 && PolygonalMap2.instance.getChunkFromSquarePos(var1 - 1, var2) == null) {
         return true;
      } else if (PZMath.coordmodulo(var1, 8) == 7 && PolygonalMap2.instance.getChunkFromSquarePos(var1 + 1, var2) == null) {
         return true;
      } else if (PZMath.coordmodulo(var2, 8) == 0 && PolygonalMap2.instance.getChunkFromSquarePos(var1, var2 - 1) == null) {
         return true;
      } else {
         return PZMath.coordmodulo(var2, 8) == 7 && PolygonalMap2.instance.getChunkFromSquarePos(var1, var2 + 1) == null;
      }
   }

   public static Node alloc() {
      boolean var0;
      if (pool.isEmpty()) {
         var0 = false;
      } else {
         var0 = false;
      }

      return pool.isEmpty() ? new Node() : (Node)pool.pop();
   }

   public void release() {
      assert !pool.contains(this);

      for(int var1 = this.visible.size() - 1; var1 >= 0; --var1) {
         PolygonalMap2.instance.breakConnection((Connection)this.visible.get(var1));
      }

      pool.push(this);
   }

   static void releaseAll(ArrayList<Node> var0) {
      for(int var1 = 0; var1 < var0.size(); ++var1) {
         ((Node)var0.get(var1)).release();
      }

   }
}
