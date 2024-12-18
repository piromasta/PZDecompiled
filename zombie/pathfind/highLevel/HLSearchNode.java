package zombie.pathfind.highLevel;

import astar.ASearchNode;
import astar.ISearchNode;
import java.util.ArrayList;
import java.util.List;
import zombie.iso.IsoUtils;
import zombie.pathfind.Node;
import zombie.util.list.PZArrayUtil;

public class HLSearchNode extends ASearchNode {
   static final int CPW = 8;
   static int nextID = 1;
   Integer ID;
   HLSearchNode parent;
   HLAStar astar;
   HLChunkRegion chunkRegion;
   HLLevelTransition levelTransition;
   boolean bBottomOfStaircase;
   Node vgNode;
   int unloadedX = -1;
   int unloadedY = -1;
   boolean bInUnloadedArea = false;
   boolean bOnEdgeOfLoadedArea = false;
   final ArrayList<HLSuccessor> successors = new ArrayList();

   HLSearchNode() {
      this.ID = nextID++;
   }

   public double h() {
      float var1 = this.getX();
      float var2 = this.getY();
      float var3 = this.getZ();
      float var4 = this.astar.goalNode.searchNode.getX();
      float var5 = this.astar.goalNode.searchNode.getY();
      float var6 = this.astar.goalNode.searchNode.getZ();
      return Math.sqrt(Math.pow((double)(var1 - var4), 2.0) + Math.pow((double)(var2 - var5), 2.0) + Math.pow((double)((var3 - var6) * 2.5F), 2.0));
   }

   public double c(ISearchNode var1) {
      HLSearchNode var2 = (HLSearchNode)var1;
      if (var2.bInUnloadedArea) {
         return (double)IsoUtils.DistanceTo(this.getX(), this.getY(), this.getZ(), var2.getX(), var2.getY(), var2.getZ());
      } else if (this.vgNode != null && var2.vgNode != null) {
         return (double)IsoUtils.DistanceTo(this.getX(), this.getY(), this.getZ(), var2.getX(), var2.getY(), var2.getZ());
      } else {
         HLSuccessor var3 = (HLSuccessor)PZArrayUtil.find((List)this.successors, (var1x) -> {
            return var1x.searchNode == var2;
         });
         return var3.cost;
      }
   }

   public void getSuccessors(ArrayList<ISearchNode> var1) {
      this.astar.getSuccessors(this, var1);
   }

   public ISearchNode getParent() {
      return this.parent;
   }

   public void setParent(ISearchNode var1) {
      this.parent = (HLSearchNode)var1;
   }

   public Integer keyCode() {
      return this.ID;
   }

   float getX() {
      if (this.chunkRegion != null) {
         return (float)(this.chunkRegion.minX + this.chunkRegion.maxX + 1) / 2.0F;
      } else if (this.levelTransition != null) {
         return this.levelTransition.getSearchNodeX(this.bBottomOfStaircase);
      } else {
         return this.vgNode != null ? this.vgNode.x : (float)this.unloadedX;
      }
   }

   float getY() {
      if (this.chunkRegion != null) {
         return (float)(this.chunkRegion.minY + this.chunkRegion.maxY + 1) / 2.0F;
      } else if (this.levelTransition != null) {
         return this.levelTransition.getSearchNodeY(this.bBottomOfStaircase);
      } else {
         return this.vgNode != null ? this.vgNode.y : (float)this.unloadedY;
      }
   }

   float getZ() {
      if (this.chunkRegion != null) {
         return (float)this.chunkRegion.getLevel();
      } else if (this.levelTransition != null) {
         return this.bBottomOfStaircase ? (float)this.levelTransition.getBottomFloorZ() : (float)this.levelTransition.getTopFloorZ();
      } else {
         return this.vgNode != null ? (float)this.vgNode.z : 32.0F;
      }
   }

   boolean calculateOnEdgeOfLoadedArea() {
      if (this.chunkRegion != null) {
         return this.chunkRegion.isOnEdgeOfLoadedArea();
      } else if (this.levelTransition != null) {
         return this.levelTransition.isOnEdgeOfLoadedArea();
      } else {
         return this.vgNode != null ? this.vgNode.isOnEdgeOfLoadedArea() : false;
      }
   }
}
