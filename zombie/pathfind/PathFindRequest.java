package zombie.pathfind;

import gnu.trove.list.array.TFloatArrayList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.ai.KnownBlockedEdges;
import zombie.ai.astar.Mover;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.pathfind.highLevel.HLChunkLevel;
import zombie.pathfind.highLevel.HLLevelTransition;
import zombie.util.Type;

final class PathFindRequest {
   IPathfinder finder;
   Mover mover;
   boolean bCanCrawl;
   boolean bIgnoreCrawlCost;
   boolean bCanThump;
   int minLevel;
   int maxLevel;
   final ArrayList<HLChunkLevel> allowedChunkLevels = new ArrayList();
   final ArrayList<HLLevelTransition> allowedLevelTransitions = new ArrayList();
   final ArrayList<KnownBlockedEdges> knownBlockedEdges = new ArrayList();
   float startX;
   float startY;
   float startZ;
   float targetX;
   float targetY;
   float targetZ;
   final TFloatArrayList targetXYZ = new TFloatArrayList();
   final Path path = new Path();
   boolean cancel = false;
   static final ArrayDeque<PathFindRequest> pool = new ArrayDeque();

   PathFindRequest() {
   }

   PathFindRequest init(IPathfinder var1, Mover var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      this.finder = var1;
      this.mover = var2;
      this.bCanCrawl = false;
      this.bIgnoreCrawlCost = false;
      this.bCanThump = false;
      IsoAnimal var9 = (IsoAnimal)Type.tryCastTo(var2, IsoAnimal.class);
      if (var9 != null) {
         this.bCanThump = var9.shouldBreakObstaclesDuringPathfinding();
      }

      IsoZombie var10 = (IsoZombie)Type.tryCastTo(var2, IsoZombie.class);
      if (var10 != null) {
         this.bCanCrawl = var10.isCrawling() || var10.isCanCrawlUnderVehicle();
         this.bIgnoreCrawlCost = var10.isCrawling() && !var10.isCanWalk();
         this.bCanThump = true;
      }

      this.minLevel = 0;
      this.maxLevel = 63;
      this.allowedChunkLevels.clear();
      this.allowedLevelTransitions.clear();
      this.startX = var3;
      this.startY = var4;
      this.startZ = var5;
      this.targetX = var6;
      this.targetY = var7;
      this.targetZ = var8;
      this.targetXYZ.resetQuick();
      this.path.clear();
      this.cancel = false;
      IsoGameCharacter var11 = (IsoGameCharacter)Type.tryCastTo(var2, IsoGameCharacter.class);
      if (var11 != null) {
         ArrayList var12 = var11.getMapKnowledge().getKnownBlockedEdges();

         for(int var13 = 0; var13 < var12.size(); ++var13) {
            KnownBlockedEdges var14 = (KnownBlockedEdges)var12.get(var13);
            this.knownBlockedEdges.add(KnownBlockedEdges.alloc().init(var14));
         }
      }

      return this;
   }

   void addTargetXYZ(float var1, float var2, float var3) {
      this.targetXYZ.add(var1);
      this.targetXYZ.add(var2);
      this.targetXYZ.add(var3);
   }

   static PathFindRequest alloc() {
      return pool.isEmpty() ? new PathFindRequest() : (PathFindRequest)pool.pop();
   }

   public void release() {
      KnownBlockedEdges.releaseAll(this.knownBlockedEdges);
      this.knownBlockedEdges.clear();

      assert !pool.contains(this);

      pool.push(this);
   }
}
