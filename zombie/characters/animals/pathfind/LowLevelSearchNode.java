package zombie.characters.animals.pathfind;

import astar.ASearchNode;
import astar.ISearchNode;
import java.util.ArrayList;
import org.joml.Vector2f;
import zombie.popman.ObjectPool;

public final class LowLevelSearchNode extends ASearchNode {
   static int nextID = 1;
   Integer ID;
   LowLevelSearchNode parent;
   LowLevelAStar astar;
   MeshList meshList;
   int meshIdx;
   int triangleIdx;
   int edgeIdx;
   float x = 0.0F / 0.0F;
   float y = 0.0F / 0.0F;
   static final ObjectPool<LowLevelSearchNode> pool = new ObjectPool(LowLevelSearchNode::new);

   LowLevelSearchNode() {
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
      LowLevelSearchNode var2 = (LowLevelSearchNode)var1;
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
      this.parent = (LowLevelSearchNode)var1;
   }

   public Integer keyCode() {
      return this.ID;
   }

   public float getEdgeMidPointX() {
      if (this.edgeIdx == -1) {
         return this.getCentroidX();
      } else {
         ArrayList var1 = this.meshList.get(this.meshIdx).triangles;
         Vector2f var2 = (Vector2f)var1.get(this.triangleIdx + this.edgeIdx);
         Vector2f var3 = (Vector2f)var1.get(this.triangleIdx + (this.edgeIdx + 1) % 3);
         return (var2.x + var3.x) / 2.0F;
      }
   }

   public float getEdgeMidPointY() {
      if (this.edgeIdx == -1) {
         return this.getCentroidY();
      } else {
         ArrayList var1 = this.meshList.get(this.meshIdx).triangles;
         Vector2f var2 = (Vector2f)var1.get(this.triangleIdx + this.edgeIdx);
         Vector2f var3 = (Vector2f)var1.get(this.triangleIdx + (this.edgeIdx + 1) % 3);
         return (var2.y + var3.y) / 2.0F;
      }
   }

   public float getCentroidX() {
      ArrayList var1 = this.meshList.get(this.meshIdx).triangles;
      Vector2f var2 = (Vector2f)var1.get(this.triangleIdx);
      Vector2f var3 = (Vector2f)var1.get(this.triangleIdx + 1);
      Vector2f var4 = (Vector2f)var1.get(this.triangleIdx + 2);
      return (var2.x + var3.x + var4.x) / 3.0F;
   }

   public float getCentroidY() {
      ArrayList var1 = this.meshList.get(this.meshIdx).triangles;
      Vector2f var2 = (Vector2f)var1.get(this.triangleIdx);
      Vector2f var3 = (Vector2f)var1.get(this.triangleIdx + 1);
      Vector2f var4 = (Vector2f)var1.get(this.triangleIdx + 2);
      return (var2.y + var3.y + var4.y) / 3.0F;
   }

   public float getX() {
      return !Float.isNaN(this.x) ? this.x : this.getEdgeMidPointX();
   }

   public float getY() {
      return !Float.isNaN(this.y) ? this.y : this.getEdgeMidPointY();
   }

   public float getZ() {
      return (float)this.meshList.z;
   }
}
