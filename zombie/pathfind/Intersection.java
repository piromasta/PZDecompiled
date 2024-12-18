package zombie.pathfind;

final class Intersection {
   Edge edge1;
   Edge edge2;
   float dist1;
   float dist2;
   Node nodeSplit;

   Intersection(Edge var1, Edge var2, float var3, float var4, float var5, float var6) {
      this.edge1 = var1;
      this.edge2 = var2;
      this.dist1 = var3;
      this.dist2 = var4;
      this.nodeSplit = Node.alloc().init(var5, var6, var1.node1.z);
   }

   Intersection(Edge var1, Edge var2, float var3, float var4, Node var5) {
      this.edge1 = var1;
      this.edge2 = var2;
      this.dist1 = var3;
      this.dist2 = var4;
      this.nodeSplit = var5;
   }

   Edge split(Edge var1) {
      return var1.split(this.nodeSplit);
   }
}
