package zombie.pathfind.highLevel;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.pathfind.PMMover;
import zombie.pathfind.Path;
import zombie.popman.ObjectPool;
import zombie.vehicles.Clipper;

public final class HLGlobals {
   static final ObjectPool<HLChunkRegion> chunkRegionPool = new ObjectPool(HLChunkRegion::new);
   static final ObjectPool<HLStaircase> staircasePool = new ObjectPool(HLStaircase::new);
   static final ObjectPool<HLSlopedSurface> slopedSurfacePool = new ObjectPool(HLSlopedSurface::new);
   static final ObjectPool<HLSuccessor> successorPool = new ObjectPool(HLSuccessor::new);
   static final FloodFill floodFill = new FloodFill();
   static final Clipper clipper = new Clipper();
   static ByteBuffer clipperBuffer = ByteBuffer.allocateDirect(512);
   public static final HLAStar astar = new HLAStar();
   static final ObjectPool<HLSearchNode> searchNodePool = new ObjectPool(HLSearchNode::new);
   public static int debugTargetLevel = 0;
   public static final PMMover mover = new PMMover();
   public static final ArrayList<HLChunkLevel> chunkLevelList = new ArrayList();
   public static final ArrayList<HLLevelTransition> levelTransitionList = new ArrayList();
   public static final ArrayList<HLStaircase> staircaseList2 = new ArrayList();
   public static final ArrayList<Boolean> bottomOfLevelTransition = new ArrayList();
   public static final Path path = new Path();

   public HLGlobals() {
   }
}
