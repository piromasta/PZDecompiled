package zombie.characters.animals.pathfind;

import astar.AStar;
import astar.ISearchNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public final class HighLevelAStar extends AStar {
   private final MeshList meshList;
   HighLevelSearchNode initialNode;
   HighLevelGoalNode goalNode;
   HashMap<Mesh, HighLevelSearchNode> nodeMap = new HashMap();

   public HighLevelAStar(MeshList var1) {
      this.meshList = var1;
   }

   void findPath(float var1, float var2, int var3, float var4, float var5, int var6) {
      Mesh var7 = this.meshList.getMeshAt(var1, var2, var3);
      Mesh var8 = this.meshList.getMeshAt(var4, var5, var6);
      if (var7 != null && var8 != null) {
         HighLevelSearchNode var9 = (HighLevelSearchNode)HighLevelSearchNode.pool.alloc();
         var9.parent = null;
         var9.astar = this;
         var9.mesh = var7;
         HighLevelSearchNode var10 = (HighLevelSearchNode)HighLevelSearchNode.pool.alloc();
         var10.parent = null;
         var10.astar = this;
         var10.mesh = var8;
         HighLevelGoalNode var11 = new HighLevelGoalNode();
         var11.init(var10);
         this.initialNode = var9;
         this.goalNode = var11;
         HighLevelSearchNode.pool.releaseAll(new ArrayList(this.nodeMap.values()));
         this.nodeMap.clear();
         this.nodeMap.put(var9.mesh, var9);
         this.nodeMap.put(var10.mesh, var10);
         ArrayList var12 = this.shortestPath(var9, var11);
         ISearchNode var14;
         HighLevelSearchNode var15;
         if (var12 != null) {
            for(Iterator var13 = var12.iterator(); var13.hasNext(); var15 = (HighLevelSearchNode)var14) {
               var14 = (ISearchNode)var13.next();
            }
         }

         HighLevelSearchNode.pool.releaseAll(new ArrayList(this.nodeMap.values()));
         this.nodeMap.clear();
      }
   }

   void getSuccessors(HighLevelSearchNode var1, ArrayList<ISearchNode> var2) {
      AnimalPathfind.getInstance().cdAStar.initOffMeshConnections(var1.mesh);
      if (!var1.mesh.offMeshConnections.isEmpty()) {
         Iterator var3 = var1.mesh.offMeshConnections.iterator();

         while(var3.hasNext()) {
            OffMeshConnection var4 = (OffMeshConnection)var3.next();
            this.addSuccessor(var4.meshTo, var2);
         }

      }
   }

   void addSuccessor(Mesh var1, ArrayList<ISearchNode> var2) {
      HighLevelSearchNode var3 = this.getSearchNode(var1);
      if (!var2.contains(var3)) {
         var2.add(var3);
      }

   }

   HighLevelSearchNode getSearchNode(Mesh var1) {
      HighLevelSearchNode var2 = (HighLevelSearchNode)this.nodeMap.get(var1);
      if (var2 == null) {
         var2 = (HighLevelSearchNode)HighLevelSearchNode.pool.alloc();
         var2.astar = this;
         var2.mesh = var1;
         this.nodeMap.put(var1, var2);
      }

      return var2;
   }
}
