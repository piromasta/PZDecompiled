package zombie.pathfind.nativeCode;

import gnu.trove.list.array.TFloatArrayList;
import java.util.ArrayList;
import zombie.ai.KnownBlockedEdges;
import zombie.ai.astar.Mover;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.pathfind.IPathfinder;
import zombie.pathfind.Path;
import zombie.pathfind.highLevel.HLChunkLevel;
import zombie.pathfind.highLevel.HLStaircase;
import zombie.popman.ObjectPool;
import zombie.util.Type;

public final class PathFindRequest {
   IPathfinder finder;
   Mover mover;
   boolean bCanCrawl;
   boolean bCrawling;
   boolean bIgnoreCrawlCost;
   boolean bCanThump;
   boolean bCanClimbFences;
   int minLevel;
   int maxLevel;
   final ArrayList<HLChunkLevel> allowedChunkLevels = new ArrayList();
   final ArrayList<HLStaircase> allowedStaircases = new ArrayList();
   final ArrayList<KnownBlockedEdges> knownBlockedEdges = new ArrayList();
   float startX;
   float startY;
   float startZ;
   float targetX;
   float targetY;
   float targetZ;
   public final TFloatArrayList targetXYZ = new TFloatArrayList();
   final Path path = new Path();
   boolean cancel = false;
   boolean doNotRelease = false;
   static final ObjectPool<PathFindRequest> pool = new ObjectPool(PathFindRequest::new);

   public PathFindRequest() {
   }

   PathFindRequest init(IPathfinder var1, Mover var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      this.finder = var1;
      this.mover = var2;
      this.bCanCrawl = false;
      this.bCrawling = false;
      this.bIgnoreCrawlCost = false;
      this.bCanThump = false;
      this.bCanClimbFences = false;
      IsoAnimal var9 = (IsoAnimal)Type.tryCastTo(var2, IsoAnimal.class);
      if (var9 != null) {
         this.bCanThump = var9.shouldBreakObstaclesDuringPathfinding();
         this.bCanClimbFences = var9.canClimbFences();
      }

      IsoZombie var10 = (IsoZombie)Type.tryCastTo(var2, IsoZombie.class);
      if (var10 != null) {
         this.bCanCrawl = var10.isCrawling() || var10.isCanCrawlUnderVehicle();
         this.bCrawling = var10.isCrawling();
         this.bIgnoreCrawlCost = var10.isCrawling() && !var10.isCanWalk();
         this.bCanThump = true;
      }

      this.minLevel = 0;
      this.maxLevel = 63;
      this.allowedChunkLevels.clear();
      this.allowedStaircases.clear();
      this.startX = var3;
      this.startY = var4;
      this.startZ = var5;
      this.targetX = var6;
      this.targetY = var7;
      this.targetZ = var8;
      this.targetXYZ.resetQuick();
      this.path.clear();
      this.cancel = false;
      return this;
   }

   static PathFindRequest alloc() {
      return (PathFindRequest)pool.alloc();
   }

   public void release() {
      this.finder = null;
      this.mover = null;
      pool.release((Object)this);
   }
}
