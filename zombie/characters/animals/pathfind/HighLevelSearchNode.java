package zombie.characters.animals.pathfind;

import astar.ASearchNode;
import astar.ISearchNode;
import java.util.ArrayList;
import zombie.popman.ObjectPool;

public final class HighLevelSearchNode extends ASearchNode {
   static int nextID = 1;
   Integer ID;
   HighLevelSearchNode parent;
   HighLevelAStar astar;
   Mesh mesh;
   static final ObjectPool<HighLevelSearchNode> pool = new ObjectPool(HighLevelSearchNode::new);

   HighLevelSearchNode() {
      this.ID = nextID++;
   }

   public double h() {
      float var1 = this.getX();
      float var2 = this.getY();
      float var3 = this.astar.goalNode.searchNode.getX();
      float var4 = this.astar.goalNode.searchNode.getY();
      return Math.sqrt(Math.pow((double)(var1 - var3), 2.0) + Math.pow((double)(var2 - var4), 2.0));
   }

   public double c(ISearchNode var1) {
      HighLevelSearchNode var2 = (HighLevelSearchNode)var1;
      float var3 = this.getX();
      float var4 = this.getY();
      float var5 = var2.getX();
      float var6 = var2.getY();
      return Math.sqrt(Math.pow((double)(var3 - var5), 2.0) + Math.pow((double)(var4 - var6), 2.0));
   }

   public void getSuccessors(ArrayList<ISearchNode> var1) {
      this.astar.getSuccessors(this, var1);
   }

   public ISearchNode getParent() {
      return this.parent;
   }

   public void setParent(ISearchNode var1) {
      this.parent = (HighLevelSearchNode)var1;
   }

   public Integer keyCode() {
      return this.ID;
   }

   float getX() {
      return this.mesh.centroidX;
   }

   float getY() {
      return this.mesh.centroidY;
   }

   float getZ() {
      return (float)this.mesh.meshList.z;
   }
}
