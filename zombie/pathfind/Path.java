package zombie.pathfind;

import java.awt.geom.Line2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.iso.IsoUtils;

public final class Path {
   final ArrayList<PathNode> nodes = new ArrayList();
   final ArrayDeque<PathNode> nodePool = new ArrayDeque();

   public Path() {
   }

   public void clear() {
      for(int var1 = 0; var1 < this.nodes.size(); ++var1) {
         if (DebugOptions.instance.Checks.ObjectPoolContains.getValue() && this.nodePool.contains(this.nodes.get(var1))) {
            boolean var2 = true;
         }

         this.nodePool.push((PathNode)this.nodes.get(var1));
      }

      this.nodes.clear();
   }

   public boolean isEmpty() {
      return this.nodes.isEmpty();
   }

   public int size() {
      return this.nodes.size();
   }

   public PathNode addNode(float var1, float var2, float var3) {
      return this.addNode(var1, var2, var3, 0);
   }

   PathNode addNode(float var1, float var2, float var3, int var4) {
      PathNode var5 = this.nodePool.isEmpty() ? new PathNode() : (PathNode)this.nodePool.pop();
      var5.init(var1, var2, var3, var4);
      this.nodes.add(var5);
      return var5;
   }

   PathNode addNodeRawZ(float var1, float var2, float var3, int var4) {
      PathNode var5 = this.nodePool.isEmpty() ? new PathNode() : (PathNode)this.nodePool.pop();
      var5.init(var1, var2, var3 + 32.0F, var4);
      this.nodes.add(var5);
      return var5;
   }

   PathNode addNode(SearchNode var1) {
      return this.addNode(var1.getX(), var1.getY(), var1.getZ(), var1.vgNode == null ? 0 : var1.vgNode.flags);
   }

   public PathNode getNode(int var1) {
      return (PathNode)this.nodes.get(var1);
   }

   PathNode getLastNode() {
      return (PathNode)this.nodes.get(this.nodes.size() - 1);
   }

   public void copyFrom(Path var1) {
      assert this != var1;

      this.clear();

      for(int var2 = 0; var2 < var1.nodes.size(); ++var2) {
         PathNode var3 = (PathNode)var1.nodes.get(var2);
         this.addNode(var3.x, var3.y, var3.z, var3.flags);
      }

   }

   public float length() {
      float var1 = 0.0F;

      for(int var2 = 0; var2 < this.nodes.size() - 1; ++var2) {
         PathNode var3 = (PathNode)this.nodes.get(var2);
         PathNode var4 = (PathNode)this.nodes.get(var2 + 1);
         var1 += IsoUtils.DistanceTo(var3.x, var3.y, var3.z, var4.x, var4.y, var4.z);
      }

      return var1;
   }

   public boolean crossesSquare(int var1, int var2, int var3) {
      for(int var4 = 0; var4 < this.nodes.size() - 1; ++var4) {
         PathNode var5 = (PathNode)this.nodes.get(var4);
         PathNode var6 = (PathNode)this.nodes.get(var4 + 1);
         if (PZMath.fastfloor(var5.z) == var3 || PZMath.fastfloor(var6.z) == var3) {
            if (Line2D.linesIntersect((double)var5.x, (double)var5.y, (double)var6.x, (double)var6.y, (double)var1, (double)var2, (double)(var1 + 1), (double)var2)) {
               return true;
            }

            if (Line2D.linesIntersect((double)var5.x, (double)var5.y, (double)var6.x, (double)var6.y, (double)(var1 + 1), (double)var2, (double)(var1 + 1), (double)(var2 + 1))) {
               return true;
            }

            if (Line2D.linesIntersect((double)var5.x, (double)var5.y, (double)var6.x, (double)var6.y, (double)(var1 + 1), (double)(var2 + 1), (double)var1, (double)(var2 + 1))) {
               return true;
            }

            if (Line2D.linesIntersect((double)var5.x, (double)var5.y, (double)var6.x, (double)var6.y, (double)var1, (double)(var2 + 1), (double)var1, (double)var2)) {
               return true;
            }
         }
      }

      return false;
   }
}
