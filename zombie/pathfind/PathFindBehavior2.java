package zombie.pathfind;

import gnu.trove.list.array.TFloatArrayList;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector2f;
import org.joml.Vector3f;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.ai.State;
import zombie.ai.WalkingOnTheSpot;
import zombie.ai.astar.AStarPathFinder;
import zombie.ai.astar.Mover;
import zombie.ai.states.ClimbOverFenceState;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.ai.states.CollideWithWallState;
import zombie.ai.states.LungeNetworkState;
import zombie.ai.states.WalkTowardState;
import zombie.ai.states.ZombieGetDownState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.network.GameClient;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.VehicleScript;
import zombie.seating.SeatingManager;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public final class PathFindBehavior2 implements IPathfinder {
   private static final Vector2 tempVector2 = new Vector2();
   private static final Vector2f tempVector2f = new Vector2f();
   private static final Vector2 tempVector2_2 = new Vector2();
   private static final Vector3f tempVector3f_1 = new Vector3f();
   private static final PointOnPath pointOnPath = new PointOnPath();
   public boolean pathNextIsSet = false;
   public float pathNextX;
   public float pathNextY;
   public ArrayList<IPathfinder> Listeners = new ArrayList();
   public NPCData NPCData = new NPCData();
   private IsoGameCharacter chr;
   private float startX;
   private float startY;
   private float startZ;
   private float targetX;
   private float targetY;
   private float targetZ;
   private final TFloatArrayList targetXYZ = new TFloatArrayList();
   private final Path path = new Path();
   private int pathIndex;
   private boolean isCancel = true;
   private boolean bStartedMoving = false;
   public boolean bStopping = false;
   private boolean bTurningToObstacle = false;
   public final WalkingOnTheSpot walkingOnTheSpot = new WalkingOnTheSpot();
   private final ArrayList<DebugPt> actualPos = new ArrayList();
   private static final ObjectPool<DebugPt> actualPool = new ObjectPool(DebugPt::new);
   private Goal goal;
   private IsoGameCharacter goalCharacter;
   private IsoObject goalSitOnFurnitureObject;
   private boolean goalSitOnFurnitureAnySpriteGridObject;
   private BaseVehicle goalVehicle;
   private String goalVehicleArea;
   private int goalVehicleSeat;

   public PathFindBehavior2(IsoGameCharacter var1) {
      this.goal = PathFindBehavior2.Goal.None;
      this.goalSitOnFurnitureObject = null;
      this.goalSitOnFurnitureAnySpriteGridObject = false;
      this.chr = var1;
   }

   public boolean isGoalNone() {
      return this.goal == PathFindBehavior2.Goal.None;
   }

   public boolean isGoalCharacter() {
      return this.goal == PathFindBehavior2.Goal.Character;
   }

   public boolean isGoalLocation() {
      return this.goal == PathFindBehavior2.Goal.Location;
   }

   public boolean isGoalSound() {
      return this.goal == PathFindBehavior2.Goal.Sound;
   }

   public boolean isGoalSitOnFurniture() {
      return this.goal == PathFindBehavior2.Goal.SitOnFurniture;
   }

   public IsoObject getGoalSitOnFurnitureObject() {
      return this.goalSitOnFurnitureObject;
   }

   public boolean isGoalVehicleAdjacent() {
      return this.goal == PathFindBehavior2.Goal.VehicleAdjacent;
   }

   public boolean isGoalVehicleArea() {
      return this.goal == PathFindBehavior2.Goal.VehicleArea;
   }

   public boolean isGoalVehicleSeat() {
      return this.goal == PathFindBehavior2.Goal.VehicleSeat;
   }

   public void reset() {
      this.startX = this.chr.getX();
      this.startY = this.chr.getY();
      this.startZ = this.chr.getZ();
      this.targetX = this.startX;
      this.targetY = this.startY;
      this.targetZ = this.startZ;
      this.targetXYZ.resetQuick();
      this.pathIndex = 0;
      this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.notrunning;
      this.walkingOnTheSpot.reset(this.startX, this.startY);
   }

   public void pathToCharacter(IsoGameCharacter var1) {
      this.isCancel = false;
      this.bStartedMoving = false;
      this.goal = PathFindBehavior2.Goal.Character;
      this.goalCharacter = var1;
      if (var1.getVehicle() != null) {
         Vector3f var2 = var1.getVehicle().chooseBestAttackPosition(var1, this.chr, tempVector3f_1);
         if (var2 != null) {
            this.setData(var2.x, var2.y, (float)PZMath.fastfloor(var1.getVehicle().getZ()));
            return;
         }

         this.setData(var1.getVehicle().getX(), var1.getVehicle().getY(), (float)PZMath.fastfloor(var1.getVehicle().getZ()));
         if (this.chr.DistToSquared(var1.getVehicle()) < 100.0F) {
            IsoZombie var3 = (IsoZombie)Type.tryCastTo(this.chr, IsoZombie.class);
            if (var3 != null) {
               var3.AllowRepathDelay = 100.0F;
            }

            this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.failed;
         }
      }

      if (var1.isSittingOnFurniture()) {
         IsoDirections var7 = var1.getSitOnFurnitureDirection();
         int var8 = PZMath.fastfloor(var1.getX());
         int var4 = PZMath.fastfloor(var1.getY());
         int var5 = PZMath.fastfloor(var1.getZ());
         float var6 = 0.3F;
         switch (var7) {
            case N:
               this.setData((float)var8 + 0.5F, (float)var4 - var6, (float)var5);
               break;
            case S:
               this.setData((float)var8 + 0.5F, (float)(var4 + 1) + var6, (float)var5);
               break;
            case W:
               this.setData((float)var8 - var6, (float)var4 + 0.5F, (float)var5);
               break;
            case E:
               this.setData((float)(var8 + 1) + var6, (float)var4 + 0.5F, (float)var5);
               break;
            default:
               DebugLog.General.warn("unhandled sitting direction");
               this.setData(var1.getX(), var1.getY(), var1.getZ());
         }

      } else {
         this.setData(var1.getX(), var1.getY(), var1.getZ());
      }
   }

   public void pathToLocation(int var1, int var2, int var3) {
      this.isCancel = false;
      this.bStartedMoving = false;
      this.goal = PathFindBehavior2.Goal.Location;
      this.setData((float)var1 + 0.5F, (float)var2 + 0.5F, (float)var3);
   }

   public void pathToLocationF(float var1, float var2, float var3) {
      this.isCancel = false;
      this.bStartedMoving = false;
      this.goal = PathFindBehavior2.Goal.Location;
      this.setData(var1, var2, var3);
   }

   public void pathToSound(int var1, int var2, int var3) {
      this.isCancel = false;
      this.bStartedMoving = false;
      this.goal = PathFindBehavior2.Goal.Sound;
      this.setData((float)var1 + 0.5F, (float)var2 + 0.5F, (float)var3);
   }

   public void pathToNearest(TFloatArrayList var1) {
      if (var1 != null && !var1.isEmpty()) {
         if (var1.size() % 3 != 0) {
            throw new IllegalArgumentException("locations should be multiples of x,y,z");
         } else {
            this.isCancel = false;
            this.bStartedMoving = false;
            this.goal = PathFindBehavior2.Goal.Location;
            this.setData(var1.get(0), var1.get(1), var1.get(2));

            for(int var2 = 3; var2 < var1.size(); var2 += 3) {
               this.targetXYZ.add(var1.get(var2));
               this.targetXYZ.add(var1.get(var2 + 1));
               this.targetXYZ.add(var1.get(var2 + 2));
            }

         }
      } else {
         throw new IllegalArgumentException("locations is null or empty");
      }
   }

   public void pathToNearestTable(KahluaTable var1) {
      if (var1 != null && !var1.isEmpty()) {
         if (var1.len() % 3 != 0) {
            throw new IllegalArgumentException("locations table should be multiples of x,y,z");
         } else {
            TFloatArrayList var2 = new TFloatArrayList(var1.size());
            int var3 = 1;

            for(int var4 = var1.len(); var3 <= var4; var3 += 3) {
               Double var5 = (Double)Type.tryCastTo(var1.rawget(var3), Double.class);
               Double var6 = (Double)Type.tryCastTo(var1.rawget(var3 + 1), Double.class);
               Double var7 = (Double)Type.tryCastTo(var1.rawget(var3 + 2), Double.class);
               if (var5 == null || var6 == null || var7 == null) {
                  throw new IllegalArgumentException("locations table should be multiples of x,y,z");
               }

               var2.add(var5.floatValue());
               var2.add(var6.floatValue());
               var2.add(var7.floatValue());
            }

            this.pathToNearest(var2);
         }
      } else {
         throw new IllegalArgumentException("locations table is null or empty");
      }
   }

   public void pathToSitOnFurniture(IsoObject var1, boolean var2) {
      TFloatArrayList var3 = new TFloatArrayList(12);
      ArrayList var4 = new ArrayList();
      if (var2) {
         var1.getSpriteGridObjectsExcludingSelf(var4);
      }

      var4.add(var1);

      int var5;
      for(var5 = 0; var5 < var4.size(); ++var5) {
         this.pathToSitOnFurnitureNoSpriteGrid((IsoObject)var4.get(var5), var3);
      }

      if (var3.isEmpty()) {
         this.isCancel = false;
         this.bStartedMoving = false;
         this.goal = PathFindBehavior2.Goal.SitOnFurniture;
         this.goalSitOnFurnitureObject = var1;
         this.goalSitOnFurnitureAnySpriteGridObject = var2;
         this.setData(this.chr.getX(), this.chr.getY(), this.chr.getZ());
         this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.failed;
      } else {
         this.isCancel = false;
         this.bStartedMoving = false;
         this.goal = PathFindBehavior2.Goal.SitOnFurniture;
         this.goalSitOnFurnitureObject = var1;
         this.goalSitOnFurnitureAnySpriteGridObject = var2;
         this.setData(var3.get(0), var3.get(1), var3.get(2));

         for(var5 = 3; var5 < var3.size(); var5 += 3) {
            this.targetXYZ.add(var3.get(var5));
            this.targetXYZ.add(var3.get(var5 + 1));
            this.targetXYZ.add(var3.get(var5 + 2));
         }

      }
   }

   private void pathToSitOnFurnitureNoSpriteGrid(IsoObject var1, TFloatArrayList var2) {
      Vector3f var3 = new Vector3f();
      float var4 = 0.3F;
      String[] var5 = new String[]{"N", "S", "W", "E"};
      String[] var6 = new String[]{"Front", "Left", "Right"};
      String[] var7 = var5;
      int var8 = var5.length;

      for(int var9 = 0; var9 < var8; ++var9) {
         String var10 = var7[var9];
         String[] var11 = var6;
         int var12 = var6.length;

         for(int var13 = 0; var13 < var12; ++var13) {
            String var14 = var11[var13];
            boolean var15 = SeatingManager.getInstance().getAdjacentPosition(this.chr, var1, var10, var14, "sitonfurniture", "SitOnFurniture" + var14, var3);
            if (var15) {
               IsoGridSquare var16 = var1.getSquare();
               if ((var16.isSolid() || var16.isSolidTrans()) && this.isPointInSquare(var3.x, var3.y, var16.getX(), var16.getY())) {
                  float var21 = var3.x;
                  float var23 = var3.y;
                  if (var10 == var5[0]) {
                     if (var14 == var6[0]) {
                        var3.y = (float)var16.getY() - var4;
                     } else if (var14 == var6[1]) {
                        var3.x = (float)var16.getX() - var4;
                     } else if (var14 == var6[2]) {
                        var3.x = (float)(var16.getX() + 1) + var4;
                     }
                  } else if (var10 == var5[1]) {
                     if (var14 == var6[0]) {
                        var3.y = (float)(var16.getY() + 1) + var4;
                     } else if (var14 == var6[1]) {
                        var3.x = (float)(var16.getX() + 1) + var4;
                     } else if (var14 == var6[2]) {
                        var3.x = (float)var16.getX() - var4;
                     }
                  } else if (var10 == var5[2]) {
                     if (var14 == var6[0]) {
                        var3.x = (float)var16.getX() - var4;
                     } else if (var14 == var6[1]) {
                        var3.y = (float)(var16.getY() + 1) + var4;
                     } else if (var14 == var6[2]) {
                        var3.y = (float)var16.getY() - var4;
                     }
                  } else if (var10 == var5[3]) {
                     if (var14 == var6[0]) {
                        var3.x = (float)(var16.getX() + 1) + var4;
                     } else if (var14 == var6[1]) {
                        var3.y = (float)var16.getY() - var4;
                     } else if (var14 == var6[2]) {
                        var3.y = (float)(var16.getY() + 1) + var4;
                     }
                  }

                  LosUtil.TestResults var19 = LosUtil.lineClear(IsoWorld.instance.CurrentCell, PZMath.fastfloor(var3.x), PZMath.fastfloor(var3.y), PZMath.fastfloor(var3.z), PZMath.fastfloor(var21), PZMath.fastfloor(var23), PZMath.fastfloor(var3.z), false);
                  if (var19 == LosUtil.TestResults.Blocked || var19 == LosUtil.TestResults.ClearThroughClosedDoor) {
                     boolean var20 = true;
                     continue;
                  }
               } else if (!this.isPointInSquare(var3.x, var3.y, var16.getX(), var16.getY())) {
                  LosUtil.TestResults var17 = LosUtil.lineClear(IsoWorld.instance.CurrentCell, PZMath.fastfloor(var3.x), PZMath.fastfloor(var3.y), PZMath.fastfloor(var3.z), var16.getX(), var16.getY(), var16.getZ(), false);
                  if (var17 == LosUtil.TestResults.Blocked || var17 == LosUtil.TestResults.ClearThroughClosedDoor) {
                     boolean var18 = true;
                     continue;
                  }
               }

               if (!var16.isSolid() && !var16.isSolidTrans() && !this.chr.canStandAt(var3.x, var3.y, var3.z)) {
                  boolean var22 = true;
               } else {
                  var2.add(var3.x);
                  var2.add(var3.y);
                  var2.add(var3.z);
               }
            }
         }
      }

   }

   private boolean isPointInSquare(float var1, float var2, int var3, int var4) {
      return var1 >= (float)var3 && var1 < (float)var3 + 1.0F && var2 >= (float)var4 && var2 < (float)(var4 + 1);
   }

   private void fixSitOnFurniturePath(float var1, float var2) {
      if (this.goalSitOnFurnitureObject != null && this.goalSitOnFurnitureObject.getObjectIndex() != -1) {
         Vector3f var3 = new Vector3f();
         float var4 = 3.4028235E38F;
         ArrayList var5 = new ArrayList();
         if (this.goalSitOnFurnitureAnySpriteGridObject) {
            this.goalSitOnFurnitureObject.getSpriteGridObjectsExcludingSelf(var5);
         }

         var5.add(this.goalSitOnFurnitureObject);
         IsoObject var6 = this.goalSitOnFurnitureObject;

         for(int var7 = 0; var7 < var5.size(); ++var7) {
            IsoObject var8 = (IsoObject)var5.get(var7);
            String[] var9 = new String[]{"N", "S", "W", "E"};
            String[] var10 = new String[]{"Front", "Left", "Right"};
            Vector3f var11 = new Vector3f();
            String[] var12 = var9;
            int var13 = var9.length;

            for(int var14 = 0; var14 < var13; ++var14) {
               String var15 = var12[var14];
               String[] var16 = var10;
               int var17 = var10.length;

               for(int var18 = 0; var18 < var17; ++var18) {
                  String var19 = var16[var18];
                  boolean var20 = SeatingManager.getInstance().getAdjacentPosition(this.chr, var8, var15, var19, "sitonfurniture", "SitOnFurniture" + var19, var11);
                  if (var20) {
                     float var21 = IsoUtils.DistanceToSquared(var1, var2, var11.x, var11.y);
                     if (var21 < var4) {
                        var3.set(var11);
                        var4 = var21;
                        var6 = var8;
                     }
                  }
               }
            }
         }

         if (!(var4 > 1.0F)) {
            this.goalSitOnFurnitureObject = var6;
            if (IsoUtils.DistanceToSquared(var3.x, var3.y, var1, var2) > 0.0025000002F) {
               this.path.addNode(var3.x, var3.y, var3.z);
               this.targetX = var3.x;
               this.targetY = var3.y;
            }

         }
      }
   }

   public boolean shouldIgnoreCollisionWithSquare(IsoGridSquare var1) {
      return this.goal == PathFindBehavior2.Goal.SitOnFurniture && this.goalSitOnFurnitureObject != null && this.goalSitOnFurnitureObject.getSquare() == var1;
   }

   public void pathToVehicleAdjacent(BaseVehicle var1) {
      this.isCancel = false;
      this.bStartedMoving = false;
      this.goal = PathFindBehavior2.Goal.VehicleAdjacent;
      this.goalVehicle = var1;
      VehicleScript var2 = var1.getScript();
      Vector3f var3 = var2.getExtents();
      Vector3f var4 = var2.getCenterOfMassOffset();
      float var5 = var3.x;
      float var6 = var3.z;
      float var7 = 0.3F;
      float var8 = var4.x - var5 / 2.0F - var7;
      float var9 = var4.z - var6 / 2.0F - var7;
      float var10 = var4.x + var5 / 2.0F + var7;
      float var11 = var4.z + var6 / 2.0F + var7;
      TFloatArrayList var12 = new TFloatArrayList();
      Vector3f var13 = var1.getWorldPos(var8, var4.y, var4.z, tempVector3f_1);
      if (PolygonalMap2.instance.canStandAt(var13.x, var13.y, PZMath.fastfloor(this.targetZ), var1, false, true)) {
         var12.add(var13.x);
         var12.add(var13.y);
         var12.add(this.targetZ);
      }

      var13 = var1.getWorldPos(var10, var4.y, var4.z, tempVector3f_1);
      if (PolygonalMap2.instance.canStandAt(var13.x, var13.y, PZMath.fastfloor(this.targetZ), var1, false, true)) {
         var12.add(var13.x);
         var12.add(var13.y);
         var12.add(this.targetZ);
      }

      var13 = var1.getWorldPos(var4.x, var4.y, var9, tempVector3f_1);
      if (PolygonalMap2.instance.canStandAt(var13.x, var13.y, PZMath.fastfloor(this.targetZ), var1, false, true)) {
         var12.add(var13.x);
         var12.add(var13.y);
         var12.add(this.targetZ);
      }

      var13 = var1.getWorldPos(var4.x, var4.y, var11, tempVector3f_1);
      if (PolygonalMap2.instance.canStandAt(var13.x, var13.y, PZMath.fastfloor(this.targetZ), var1, false, true)) {
         var12.add(var13.x);
         var12.add(var13.y);
         var12.add(this.targetZ);
      }

      this.setData(var12.get(0), var12.get(1), var12.get(2));

      for(int var14 = 3; var14 < var12.size(); var14 += 3) {
         this.targetXYZ.add(var12.get(var14));
         this.targetXYZ.add(var12.get(var14 + 1));
         this.targetXYZ.add(var12.get(var14 + 2));
      }

   }

   public void pathToVehicleArea(BaseVehicle var1, String var2) {
      Vector2 var3 = var1.getAreaCenter(var2);
      if (var3 == null) {
         this.targetX = this.chr.getX();
         this.targetY = this.chr.getY();
         this.targetZ = this.chr.getZ();
         this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.failed;
      } else {
         this.isCancel = false;
         this.bStartedMoving = false;
         this.goal = PathFindBehavior2.Goal.VehicleArea;
         this.goalVehicle = var1;
         this.goalVehicleArea = var2;
         this.setData(var3.getX(), var3.getY(), (float)PZMath.fastfloor(var1.getZ()));
         if (this.chr instanceof IsoPlayer && PZMath.fastfloor(this.chr.getZ()) == PZMath.fastfloor(this.targetZ) && !PolygonalMap2.instance.lineClearCollide(this.chr.getX(), this.chr.getY(), this.targetX, this.targetY, PZMath.fastfloor(this.targetZ), (IsoMovingObject)null)) {
            this.path.clear();
            this.path.addNode(this.chr.getX(), this.chr.getY(), this.chr.getZ());
            this.path.addNode(this.targetX, this.targetY, this.targetZ);
            this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.found;
         }

      }
   }

   public void pathToVehicleSeat(BaseVehicle var1, int var2) {
      VehicleScript.Position var3 = var1.getPassengerPosition(var2, "outside2");
      Vector3f var4;
      Vector2 var5;
      VehicleScript.Area var6;
      Vector2 var7;
      if (var3 != null) {
         var4 = (Vector3f)((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).alloc();
         if (var3.area == null) {
            var1.getPassengerPositionWorldPos(var3, var4);
         } else {
            var5 = (Vector2)((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).alloc();
            var6 = var1.getScript().getAreaById(var3.area);
            var7 = var1.areaPositionWorld4PlayerInteract(var6, var5);
            var4.x = var7.x;
            var4.y = var7.y;
            var4.z = 0.0F;
            ((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).release(var5);
         }

         var4.sub(this.chr.getX(), this.chr.getY(), this.chr.getZ());
         if (var4.length() < 2.0F) {
            var1.getPassengerPositionWorldPos(var3, var4);
            this.setData(var4.x(), var4.y(), (float)PZMath.fastfloor(var4.z()));
            if (this.chr instanceof IsoPlayer && PZMath.fastfloor(this.chr.getZ()) == PZMath.fastfloor(this.targetZ)) {
               ((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).release(var4);
               this.path.clear();
               this.path.addNode(this.chr.getX(), this.chr.getY(), this.chr.getZ());
               this.path.addNode(this.targetX, this.targetY, this.targetZ);
               this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.found;
               return;
            }
         }

         ((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).release(var4);
      }

      var3 = var1.getPassengerPosition(var2, "outside");
      if (var3 == null) {
         VehiclePart var8 = var1.getPassengerDoor(var2);
         if (var8 == null) {
            this.targetX = this.chr.getX();
            this.targetY = this.chr.getY();
            this.targetZ = this.chr.getZ();
            this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.failed;
         } else {
            this.pathToVehicleArea(var1, var8.getArea());
         }
      } else {
         this.isCancel = false;
         this.bStartedMoving = false;
         this.goal = PathFindBehavior2.Goal.VehicleSeat;
         this.goalVehicle = var1;
         var4 = (Vector3f)((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).alloc();
         if (var3.area == null) {
            var1.getPassengerPositionWorldPos(var3, var4);
         } else {
            var5 = (Vector2)((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).alloc();
            var6 = var1.getScript().getAreaById(var3.area);
            var7 = var1.areaPositionWorld4PlayerInteract(var6, var5);
            var4.x = var7.x;
            var4.y = var7.y;
            var4.z = (float)PZMath.fastfloor(var1.jniTransform.origin.y / 2.44949F);
            ((BaseVehicle.Vector2ObjectPool)BaseVehicle.TL_vector2_pool.get()).release(var5);
         }

         this.setData(var4.x(), var4.y(), (float)PZMath.fastfloor(var4.z()));
         ((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).release(var4);
         if (this.chr instanceof IsoPlayer && PZMath.fastfloor(this.chr.getZ()) == PZMath.fastfloor(this.targetZ) && !PolygonalMap2.instance.lineClearCollide(this.chr.getX(), this.chr.getY(), this.targetX, this.targetY, PZMath.fastfloor(this.targetZ), (IsoMovingObject)null)) {
            this.path.clear();
            this.path.addNode(this.chr.getX(), this.chr.getY(), this.chr.getZ());
            this.path.addNode(this.targetX, this.targetY, this.targetZ);
            this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.found;
         }

      }
   }

   public void cancel() {
      this.isCancel = true;
   }

   public boolean getIsCancelled() {
      return this.isCancel;
   }

   public void setData(float var1, float var2, float var3) {
      this.startX = this.chr.getX();
      this.startY = this.chr.getY();
      this.startZ = this.chr.getZ();
      this.targetX = var1;
      this.targetY = var2;
      this.targetZ = var3;
      this.targetXYZ.resetQuick();
      this.pathIndex = 0;
      if (PathfindNative.USE_NATIVE_CODE) {
         PathfindNative.instance.cancelRequest(this.chr);
      } else {
         PolygonalMap2.instance.cancelRequest(this.chr);
      }

      this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.notrunning;
      this.bStopping = false;
      actualPool.release((List)this.actualPos);
      this.actualPos.clear();
   }

   public float getTargetX() {
      return this.targetX;
   }

   public float getTargetY() {
      return this.targetY;
   }

   public float getTargetZ() {
      return this.targetZ;
   }

   public float getPathLength() {
      if (this.path != null && this.path.nodes.size() != 0) {
         if (this.pathIndex + 1 >= this.path.nodes.size()) {
            return (float)Math.sqrt((double)((this.chr.getX() - this.targetX) * (this.chr.getX() - this.targetX) + (this.chr.getY() - this.targetY) * (this.chr.getY() - this.targetY)));
         } else {
            float var1 = (float)Math.sqrt((double)((this.chr.getX() - ((PathNode)this.path.nodes.get(this.pathIndex + 1)).x) * (this.chr.getX() - ((PathNode)this.path.nodes.get(this.pathIndex + 1)).x) + (this.chr.getY() - ((PathNode)this.path.nodes.get(this.pathIndex + 1)).y) * (this.chr.getY() - ((PathNode)this.path.nodes.get(this.pathIndex + 1)).y)));

            for(int var2 = this.pathIndex + 2; var2 < this.path.nodes.size(); ++var2) {
               var1 += (float)Math.sqrt((double)((((PathNode)this.path.nodes.get(var2 - 1)).x - ((PathNode)this.path.nodes.get(var2)).x) * (((PathNode)this.path.nodes.get(var2 - 1)).x - ((PathNode)this.path.nodes.get(var2)).x) + (((PathNode)this.path.nodes.get(var2 - 1)).y - ((PathNode)this.path.nodes.get(var2)).y) * (((PathNode)this.path.nodes.get(var2 - 1)).y - ((PathNode)this.path.nodes.get(var2)).y)));
            }

            return var1;
         }
      } else {
         return (float)Math.sqrt((double)((this.chr.getX() - this.targetX) * (this.chr.getX() - this.targetX) + (this.chr.getY() - this.targetY) * (this.chr.getY() - this.targetY)));
      }
   }

   public IsoGameCharacter getTargetChar() {
      return this.goal == PathFindBehavior2.Goal.Character ? this.goalCharacter : null;
   }

   public boolean isTargetLocation(float var1, float var2, float var3) {
      return this.goal == PathFindBehavior2.Goal.Location && var1 == this.targetX && var2 == this.targetY && PZMath.fastfloor(var3) == PZMath.fastfloor(this.targetZ);
   }

   public BehaviorResult update() {
      if (this.chr.getFinder().progress == AStarPathFinder.PathFindProgress.notrunning) {
         if (PathfindNative.USE_NATIVE_CODE) {
            zombie.pathfind.nativeCode.PathFindRequest var11 = PathfindNative.instance.addRequest(this, this.chr, this.startX, this.startY, this.startZ, this.targetX, this.targetY, this.targetZ);
            var11.targetXYZ.resetQuick();
            var11.targetXYZ.addAll(this.targetXYZ);
         } else {
            PathFindRequest var12 = PolygonalMap2.instance.addRequest(this, this.chr, this.startX, this.startY, this.startZ, this.targetX, this.targetY, this.targetZ);
            var12.targetXYZ.resetQuick();
            var12.targetXYZ.addAll(this.targetXYZ);
         }

         this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.notyetfound;
         this.walkingOnTheSpot.reset(this.chr.getX(), this.chr.getY());
         this.updateWhileRunningPathfind();
         return PathFindBehavior2.BehaviorResult.Working;
      } else if (this.chr.getFinder().progress == AStarPathFinder.PathFindProgress.notyetfound) {
         this.updateWhileRunningPathfind();
         return PathFindBehavior2.BehaviorResult.Working;
      } else if (this.chr.getFinder().progress == AStarPathFinder.PathFindProgress.failed) {
         return PathFindBehavior2.BehaviorResult.Failed;
      } else {
         State var1 = this.chr.getCurrentState();
         if (Core.bDebug && DebugOptions.instance.PathfindRenderPath.getValue() && this.chr instanceof IsoPlayer && !this.chr.isAnimal()) {
            while(this.actualPos.size() > 100) {
               actualPool.release((Object)((DebugPt)this.actualPos.remove(0)));
            }

            this.actualPos.add(((DebugPt)actualPool.alloc()).init(this.chr.getX(), this.chr.getY(), this.chr.getZ(), var1 == ClimbOverFenceState.instance() || var1 == ClimbThroughWindowState.instance()));
         }

         if (var1 != ClimbOverFenceState.instance() && var1 != ClimbThroughWindowState.instance()) {
            if (this.chr.getVehicle() != null) {
               return PathFindBehavior2.BehaviorResult.Failed;
            } else if (this.walkingOnTheSpot.check(this.chr)) {
               return PathFindBehavior2.BehaviorResult.Failed;
            } else {
               this.chr.setMoving(true);
               this.chr.setPath2(this.path);
               IsoZombie var2 = (IsoZombie)Type.tryCastTo(this.chr, IsoZombie.class);
               if (this.goal == PathFindBehavior2.Goal.Character && var2 != null && this.goalCharacter != null && this.goalCharacter.getVehicle() != null && this.chr.DistToSquared(this.targetX, this.targetY) < 16.0F) {
                  Vector3f var3 = this.goalCharacter.getVehicle().chooseBestAttackPosition(this.goalCharacter, this.chr, tempVector3f_1);
                  if (var3 == null) {
                     return PathFindBehavior2.BehaviorResult.Failed;
                  }

                  if (Math.abs(var3.x - this.targetX) > 0.1F || Math.abs(var3.y - this.targetY) > 0.1F) {
                     if (Math.abs(this.goalCharacter.getVehicle().getCurrentSpeedKmHour()) > 0.8F) {
                        if (!PolygonalMap2.instance.lineClearCollide(this.chr.getX(), this.chr.getY(), var3.x, var3.y, PZMath.fastfloor(this.targetZ), this.goalCharacter)) {
                           this.path.clear();
                           this.path.addNode(this.chr.getX(), this.chr.getY(), this.chr.getZ());
                           this.path.addNode(var3.x, var3.y, var3.z);
                        } else if (IsoUtils.DistanceToSquared(var3.x, var3.y, this.targetX, this.targetY) > IsoUtils.DistanceToSquared(this.chr.getX(), this.chr.getY(), var3.x, var3.y)) {
                           return PathFindBehavior2.BehaviorResult.Working;
                        }
                     } else if (var2.AllowRepathDelay <= 0.0F) {
                        var2.AllowRepathDelay = 6.25F;
                        if (PolygonalMap2.instance.lineClearCollide(this.chr.getX(), this.chr.getY(), var3.x, var3.y, PZMath.fastfloor(this.targetZ), (IsoMovingObject)null)) {
                           this.setData(var3.x, var3.y, this.targetZ);
                           return PathFindBehavior2.BehaviorResult.Working;
                        }

                        this.path.clear();
                        this.path.addNode(this.chr.getX(), this.chr.getY(), this.chr.getZ());
                        this.path.addNode(var3.x, var3.y, var3.z);
                     }
                  }
               }

               closestPointOnPath(this.chr.getX(), this.chr.getY(), this.chr.getZ(), this.chr, this.path, pointOnPath);
               this.pathIndex = pointOnPath.pathIndex;
               PathNode var13;
               if (this.pathIndex == this.path.nodes.size() - 2) {
                  var13 = (PathNode)this.path.nodes.get(this.path.nodes.size() - 1);
                  float var4 = IsoUtils.DistanceTo(this.chr.getX(), this.chr.getY(), var13.x, var13.y);
                  if (var4 <= 0.05F) {
                     this.chr.getDeferredMovement(tempVector2);
                     float var5 = 0.0F;
                     if (this.chr instanceof IsoAnimal) {
                        var5 = ((IsoAnimal)this.chr).adef.animalSize;
                     }

                     if (!(tempVector2.getLength() > var5)) {
                        this.pathNextIsSet = false;
                        return PathFindBehavior2.BehaviorResult.Succeeded;
                     }

                     if (var2 != null || this.chr instanceof IsoPlayer) {
                        this.chr.setMoving(false);
                     }

                     tempVector2_2.set(var13.x - this.chr.getX(), var13.y - this.chr.getY());
                     tempVector2_2.setLength(PZMath.min(var4, 0.005F * GameTime.getInstance().getMultiplier()));
                     this.chr.MoveUnmodded(tempVector2_2);
                     this.bStopping = true;
                     return PathFindBehavior2.BehaviorResult.Working;
                  }

                  this.bStopping = false;
               } else if (this.pathIndex < this.path.nodes.size() - 2 && pointOnPath.dist > 0.999F) {
                  ++this.pathIndex;
               }

               var13 = (PathNode)this.path.nodes.get(this.pathIndex);
               PathNode var14 = (PathNode)this.path.nodes.get(this.pathIndex + 1);
               this.pathNextX = var14.x;
               this.pathNextY = var14.y;
               this.pathNextIsSet = true;
               Vector2 var15 = tempVector2.set(this.pathNextX - this.chr.getX(), this.pathNextY - this.chr.getY());
               var15.normalize();
               this.chr.getDeferredMovement(tempVector2_2);
               if (!this.chr.isAnimationUpdatingThisFrame()) {
                  tempVector2_2.set(0.0F, 0.0F);
               }

               float var6 = tempVector2_2.getLength();
               if (var2 != null) {
                  var2.bRunning = false;
                  if (SandboxOptions.instance.Lore.Speed.getValue() == 1) {
                     var2.bRunning = true;
                  }
               }

               float var7 = 1.0F;
               float var8 = var6 * var7;
               float var9 = IsoUtils.DistanceTo(this.pathNextX, this.pathNextY, this.chr.getX(), this.chr.getY());
               if (var8 >= var9) {
                  var6 *= var9 / var8;
                  ++this.pathIndex;
               }

               if (var2 != null) {
                  this.checkCrawlingTransition(var13, var14, var9);
               }

               if (var2 == null && var9 >= 0.5F) {
                  if (this.checkDoorHoppableWindow(this.chr.getX() + var15.x * Math.max(0.5F, var6), this.chr.getY() + var15.y * Math.max(0.5F, var6), this.chr.getZ())) {
                     return PathFindBehavior2.BehaviorResult.Failed;
                  }

                  if (var1 != this.chr.getCurrentState()) {
                     return PathFindBehavior2.BehaviorResult.Working;
                  }
               }

               if (var6 <= 0.0F) {
                  this.walkingOnTheSpot.reset(this.chr.getX(), this.chr.getY());
                  return PathFindBehavior2.BehaviorResult.Working;
               } else {
                  if (this.shouldBeMoving()) {
                     tempVector2_2.set(var15);
                     tempVector2_2.setLength(var6);
                     this.chr.MoveUnmodded(tempVector2_2);
                     this.bStartedMoving = true;
                  }

                  if (this.isStrafing()) {
                     if ((this.goal == PathFindBehavior2.Goal.VehicleAdjacent || this.goal == PathFindBehavior2.Goal.VehicleArea || this.goal == PathFindBehavior2.Goal.VehicleSeat) && this.goalVehicle != null) {
                        this.chr.faceThisObject(this.goalVehicle);
                     }
                  } else if (!this.chr.isAiming()) {
                     if (this.isTurningToObstacle() && this.chr.shouldBeTurning()) {
                        boolean var16 = true;
                     } else if (this.chr.isAnimatingBackwards()) {
                        tempVector2.set(this.chr.getX() - this.pathNextX, this.chr.getY() - this.pathNextY);
                        if (tempVector2.getLengthSquared() > 0.0F) {
                           this.chr.DirectionFromVector(tempVector2);
                           tempVector2.normalize();
                           this.chr.setForwardDirection(tempVector2.x, tempVector2.y);
                           AnimationPlayer var10 = this.chr.getAnimationPlayer();
                           if (var10 != null && var10.isReady()) {
                              var10.updateForwardDirection(this.chr);
                           }
                        }
                     } else {
                        this.chr.faceLocationF(this.pathNextX, this.pathNextY);
                     }
                  }

                  return PathFindBehavior2.BehaviorResult.Working;
               }
            }
         } else {
            if (GameClient.bClient && this.chr instanceof IsoPlayer && !((IsoPlayer)this.chr).isLocalPlayer()) {
               this.chr.getDeferredMovement(tempVector2_2);
               this.chr.MoveUnmodded(tempVector2_2);
            }

            return PathFindBehavior2.BehaviorResult.Working;
         }
      }
   }

   private void updateWhileRunningPathfind() {
      if (this.pathNextIsSet) {
         this.moveToPoint(this.pathNextX, this.pathNextY, 1.0F);
      }
   }

   public void moveToPoint(float var1, float var2, float var3) {
      if (!(this.chr instanceof IsoPlayer) || this.chr.getCurrentState() != CollideWithWallState.instance()) {
         IsoZombie var4 = (IsoZombie)Type.tryCastTo(this.chr, IsoZombie.class);
         Vector2 var5 = tempVector2.set(var1 - this.chr.getX(), var2 - this.chr.getY());
         if (PZMath.fastfloor(var1) != PZMath.fastfloor(this.chr.getX()) || PZMath.fastfloor(var2) != PZMath.fastfloor(this.chr.getY()) || !(var5.getLength() <= 0.1F)) {
            var5.normalize();
            this.chr.getDeferredMovement(tempVector2_2);
            float var6 = tempVector2_2.getLength();
            var6 *= var3;
            boolean var7 = false;
            if (var4 != null) {
               var4.bRunning = SandboxOptions.instance.Lore.Speed.getValue() == 1;
               var7 = GameClient.bClient && var4.isRemoteZombie() && var4.getTarget() != null && var4.isCurrentState(LungeNetworkState.instance());
            }

            if (!(var6 <= 0.0F)) {
               tempVector2_2.set(var5);
               tempVector2_2.setLength(var6);
               this.chr.MoveUnmodded(tempVector2_2);
               if (!var7) {
                  this.chr.faceLocation(var1 - 0.5F, var2 - 0.5F);
                  this.chr.setForwardDirection(var1 - this.chr.getX(), var2 - this.chr.getY());
                  this.chr.getForwardDirection().normalize();
               }
            }
         }
      }
   }

   public void moveToDir(IsoMovingObject var1, float var2) {
      Vector2 var3 = tempVector2.set(var1.getX() - this.chr.getX(), var1.getY() - this.chr.getY());
      if (!(var3.getLength() <= 0.1F)) {
         var3.normalize();
         this.chr.getDeferredMovement(tempVector2_2);
         float var4 = tempVector2_2.getLength();
         var4 *= var2;
         if (this.chr instanceof IsoZombie) {
            ((IsoZombie)this.chr).bRunning = false;
            if (SandboxOptions.instance.Lore.Speed.getValue() == 1) {
               ((IsoZombie)this.chr).bRunning = true;
            }
         }

         if (!(var4 <= 0.0F)) {
            tempVector2_2.set(var3);
            tempVector2_2.setLength(var4);
            this.chr.MoveUnmodded(tempVector2_2);
            this.chr.faceLocation(var1.getX() - 0.5F, var1.getY() - 0.5F);
            this.chr.getForwardDirection().set(var1.getX() - this.chr.getX(), var1.getY() - this.chr.getY());
            this.chr.getForwardDirection().normalize();
         }
      }
   }

   private boolean checkDoorHoppableWindow(float var1, float var2, float var3) {
      this.bTurningToObstacle = false;
      IsoGridSquare var4 = this.chr.getCurrentSquare();
      if (var4 == null) {
         return false;
      } else {
         IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare((double)var1, (double)var2, (double)var3);
         if (var5 != null && var5 != var4) {
            int var6 = var5.x - var4.x;
            int var7 = var5.y - var4.y;
            if (var6 != 0 && var7 != 0) {
               return false;
            } else {
               IsoObject var8 = this.chr.getCurrentSquare().getDoorTo(var5);
               if (var8 instanceof IsoDoor) {
                  IsoDoor var9 = (IsoDoor)var8;
                  if (!var9.open) {
                     if (this.chr instanceof IsoPlayer && ((IsoPlayer)this.chr).TimeSinceCloseDoor < 50.0F) {
                        ((IsoPlayer)this.chr).setCollidable(false);
                     } else {
                        if (!var9.couldBeOpen(this.chr)) {
                           return true;
                        }

                        ((IsoPlayer)this.chr).setCollidable(true);
                        var9.ToggleDoor(this.chr);
                        if (!var9.open) {
                           return true;
                        }
                     }
                  }
               } else if (var8 instanceof IsoThumpable) {
                  IsoThumpable var13 = (IsoThumpable)var8;
                  if (!var13.open) {
                     if (this.chr instanceof IsoPlayer && ((IsoPlayer)this.chr).TimeSinceCloseDoor < 50.0F) {
                        ((IsoPlayer)this.chr).setCollidable(false);
                     } else {
                        if (!var13.couldBeOpen(this.chr)) {
                           return true;
                        }

                        ((IsoPlayer)this.chr).setCollidable(true);
                        var13.ToggleDoor(this.chr);
                        if (!var13.open) {
                           return true;
                        }
                     }
                  }
               }

               IsoWindow var14 = var4.getWindowTo(var5);
               if (var14 != null) {
                  if (var14.canClimbThrough(this.chr) && (!var14.isSmashed() || var14.isGlassRemoved())) {
                     if (this.chr.isAiming()) {
                        return false;
                     } else {
                        this.chr.faceThisObject(var14);
                        if (this.chr.shouldBeTurning()) {
                           this.bTurningToObstacle = true;
                           return false;
                        } else {
                           this.chr.climbThroughWindow(var14);
                           return false;
                        }
                     }
                  } else {
                     return true;
                  }
               } else {
                  IsoThumpable var10 = var4.getWindowThumpableTo(var5);
                  if (var10 != null) {
                     if (var10.isBarricaded()) {
                        return true;
                     } else if (this.chr.isAiming()) {
                        return false;
                     } else {
                        this.chr.faceThisObject(var10);
                        if (this.chr.shouldBeTurning()) {
                           this.bTurningToObstacle = true;
                           return false;
                        } else {
                           this.chr.climbThroughWindow(var10);
                           return false;
                        }
                     }
                  } else {
                     IsoWindowFrame var11 = var4.getWindowFrameTo(var5);
                     if (var11 != null) {
                        this.chr.climbThroughWindowFrame(var11);
                        return false;
                     } else {
                        IsoDirections var12 = IsoDirections.Max;
                        if (var6 > 0 && var5.Is(IsoFlagType.HoppableW)) {
                           var12 = IsoDirections.E;
                        } else if (var6 < 0 && var4.Is(IsoFlagType.HoppableW)) {
                           var12 = IsoDirections.W;
                        } else if (var7 < 0 && var4.Is(IsoFlagType.HoppableN)) {
                           var12 = IsoDirections.N;
                        } else if (var7 > 0 && var5.Is(IsoFlagType.HoppableN)) {
                           var12 = IsoDirections.S;
                        }

                        if (var12 != IsoDirections.Max) {
                           if (this.chr.isAiming()) {
                              return false;
                           }

                           this.chr.faceDirection(var12);
                           if (this.chr.shouldBeTurning()) {
                              this.bTurningToObstacle = true;
                              return false;
                           }

                           this.chr.climbOverFence(var12);
                        }

                        return false;
                     }
                  }
               }
            }
         } else {
            return false;
         }
      }
   }

   private void checkCrawlingTransition(PathNode var1, PathNode var2, float var3) {
      IsoZombie var4 = (IsoZombie)this.chr;
      if (this.pathIndex < this.path.nodes.size() - 2) {
         var1 = (PathNode)this.path.nodes.get(this.pathIndex);
         var2 = (PathNode)this.path.nodes.get(this.pathIndex + 1);
         var3 = IsoUtils.DistanceTo(var2.x, var2.y, this.chr.getX(), this.chr.getY());
      }

      if (var4.isCrawling()) {
         if (!var4.isCanWalk()) {
            return;
         }

         if (var4.isBeingSteppedOn()) {
         }

         if (var4.getStateMachine().getPrevious() == ZombieGetDownState.instance() && ZombieGetDownState.instance().isNearStartXY(var4)) {
            return;
         }

         this.advanceAlongPath(this.chr.getX(), this.chr.getY(), this.chr.getZ(), 0.5F, pointOnPath);
         if (!PolygonalMap2.instance.canStandAt(pointOnPath.x, pointOnPath.y, PZMath.fastfloor(var4.getZ()), (IsoMovingObject)null, false, true)) {
            return;
         }

         if (!var2.hasFlag(1) && PolygonalMap2.instance.canStandAt(var4.getX(), var4.getY(), PZMath.fastfloor(var4.getZ()), (IsoMovingObject)null, false, true)) {
            var4.setVariable("ShouldStandUp", true);
         }
      } else {
         if (var1.hasFlag(1) && var2.hasFlag(1)) {
            var4.setVariable("ShouldBeCrawling", true);
            ZombieGetDownState.instance().setParams(this.chr);
            return;
         }

         if (var3 < 0.4F && !var1.hasFlag(1) && var2.hasFlag(1)) {
            var4.setVariable("ShouldBeCrawling", true);
            ZombieGetDownState.instance().setParams(this.chr);
         }
      }

   }

   public boolean shouldGetUpFromCrawl() {
      return this.chr.getVariableBoolean("ShouldStandUp");
   }

   public boolean shouldBeMoving() {
      if (this.bStopping) {
         return false;
      } else {
         return !this.allowTurnAnimation() || !this.chr.shouldBeTurning();
      }
   }

   public boolean hasStartedMoving() {
      return this.bStartedMoving;
   }

   public boolean allowTurnAnimation() {
      return !this.hasStartedMoving() || this.isTurningToObstacle();
   }

   public boolean isTurningToObstacle() {
      return this.bTurningToObstacle;
   }

   public boolean isStrafing() {
      if (this.chr.isZombie()) {
         return false;
      } else {
         return this.path.nodes.size() == 2 && IsoUtils.DistanceToSquared(this.startX, this.startY, this.startZ * 3.0F, this.targetX, this.targetY, this.targetZ * 3.0F) < 0.25F;
      }
   }

   public static void closestPointOnPath(float var0, float var1, float var2, IsoMovingObject var3, Path var4, PointOnPath var5) {
      IsoCell var6 = IsoWorld.instance.CurrentCell;
      var5.pathIndex = 0;
      float var7 = 3.4028235E38F;

      for(int var8 = 0; var8 < var4.nodes.size() - 1; ++var8) {
         PathNode var9 = (PathNode)var4.nodes.get(var8);
         PathNode var10 = (PathNode)var4.nodes.get(var8 + 1);
         if (PZMath.fastfloor(var9.z) == PZMath.fastfloor(var2) || PZMath.fastfloor(var10.z) == PZMath.fastfloor(var2)) {
            float var11 = var9.x;
            float var12 = var9.y;
            float var13 = var10.x;
            float var14 = var10.y;
            double var15 = (double)((var0 - var11) * (var13 - var11) + (var1 - var12) * (var14 - var12)) / (Math.pow((double)(var13 - var11), 2.0) + Math.pow((double)(var14 - var12), 2.0));
            double var17 = (double)var11 + var15 * (double)(var13 - var11);
            double var19 = (double)var12 + var15 * (double)(var14 - var12);
            if (var15 <= 0.0) {
               var17 = (double)var11;
               var19 = (double)var12;
               var15 = 0.0;
            } else if (var15 >= 1.0) {
               var17 = (double)var13;
               var19 = (double)var14;
               var15 = 1.0;
            }

            int var21 = PZMath.fastfloor(var17) - PZMath.fastfloor(var0);
            int var22 = PZMath.fastfloor(var19) - PZMath.fastfloor(var1);
            IsoGridSquare var24;
            if ((var21 != 0 || var22 != 0) && Math.abs(var21) <= 1 && Math.abs(var22) <= 1) {
               IsoGridSquare var23 = var6.getGridSquare(PZMath.fastfloor(var0), PZMath.fastfloor(var1), PZMath.fastfloor(var2));
               var24 = var6.getGridSquare(PZMath.fastfloor(var17), PZMath.fastfloor(var19), PZMath.fastfloor(var2));
               if (var3 instanceof IsoZombie) {
                  boolean var25 = ((IsoZombie)var3).Ghost;
                  ((IsoZombie)var3).Ghost = true;

                  try {
                     if (var23 != null && var24 != null && var23.testCollideAdjacent(var3, var21, var22, 0)) {
                        continue;
                     }
                  } finally {
                     ((IsoZombie)var3).Ghost = var25;
                  }
               } else if (var23 != null && var24 != null && var23.testCollideAdjacent(var3, var21, var22, 0)) {
                  continue;
               }
            }

            float var30 = var2;
            if (Math.abs(var21) <= 1 && Math.abs(var22) <= 1) {
               var24 = var6.getGridSquare(PZMath.fastfloor(var9.x), PZMath.fastfloor(var9.y), PZMath.fastfloor(var9.z));
               IsoGridSquare var32 = var6.getGridSquare(PZMath.fastfloor(var10.x), PZMath.fastfloor(var10.y), PZMath.fastfloor(var10.z));
               float var26 = var24 == null ? var9.z : PolygonalMap2.instance.getApparentZ(var24);
               float var27 = var32 == null ? var10.z : PolygonalMap2.instance.getApparentZ(var32);
               var30 = var26 + (var27 - var26) * (float)var15;
            }

            float var31 = IsoUtils.DistanceToSquared(var0, var1, var2, (float)var17, (float)var19, var30);
            if (var31 < var7) {
               var7 = var31;
               var5.pathIndex = var8;
               var5.dist = var15 == 1.0 ? 1.0F : (float)var15;
               var5.x = (float)var17;
               var5.y = (float)var19;
            }
         }
      }

   }

   void advanceAlongPath(float var1, float var2, float var3, float var4, PointOnPath var5) {
      closestPointOnPath(var1, var2, var3, this.chr, this.path, var5);

      for(int var6 = var5.pathIndex; var6 < this.path.nodes.size() - 1; ++var6) {
         PathNode var7 = (PathNode)this.path.nodes.get(var6);
         PathNode var8 = (PathNode)this.path.nodes.get(var6 + 1);
         double var9 = (double)IsoUtils.DistanceTo2D(var1, var2, var8.x, var8.y);
         if (!((double)var4 > var9)) {
            var5.pathIndex = var6;
            var5.dist += var4 / IsoUtils.DistanceTo2D(var7.x, var7.y, var8.x, var8.y);
            var5.x = var7.x + var5.dist * (var8.x - var7.x);
            var5.y = var7.y + var5.dist * (var8.y - var7.y);
            return;
         }

         var1 = var8.x;
         var2 = var8.y;
         var4 = (float)((double)var4 - var9);
         var5.dist = 0.0F;
      }

      var5.pathIndex = this.path.nodes.size() - 1;
      var5.dist = 1.0F;
      var5.x = ((PathNode)this.path.nodes.get(var5.pathIndex)).x;
      var5.y = ((PathNode)this.path.nodes.get(var5.pathIndex)).y;
   }

   public void render() {
      if (this.chr.getCurrentState() == WalkTowardState.instance()) {
         WalkTowardState.instance().calculateTargetLocation((IsoZombie)this.chr, tempVector2);
         Vector2 var10000 = tempVector2;
         var10000.x -= this.chr.getX();
         var10000 = tempVector2;
         var10000.y -= this.chr.getY();
         tempVector2.setLength(Math.min(100.0F, tempVector2.getLength()));
         LineDrawer.addLine(this.chr.getX(), this.chr.getY(), this.chr.getZ(), this.chr.getX() + tempVector2.x, this.chr.getY() + tempVector2.y, this.targetZ, 1.0F, 1.0F, 1.0F, (String)null, true);
      } else if (this.chr.getPath2() != null) {
         int var1;
         PathNode var2;
         float var4;
         float var5;
         for(var1 = 0; var1 < this.path.nodes.size() - 1; ++var1) {
            var2 = (PathNode)this.path.nodes.get(var1);
            PathNode var3 = (PathNode)this.path.nodes.get(var1 + 1);
            var4 = 1.0F;
            var5 = 1.0F;
            if (PZMath.fastfloor(var2.z) != PZMath.fastfloor(var3.z)) {
               var5 = 0.0F;
            }

            LineDrawer.addLine(var2.x, var2.y, var2.z, var3.x, var3.y, var3.z, var4, var5, 0.0F, (String)null, true);
         }

         for(var1 = 0; var1 < this.path.nodes.size(); ++var1) {
            var2 = (PathNode)this.path.nodes.get(var1);
            float var7 = 1.0F;
            var4 = 1.0F;
            var5 = 0.0F;
            if (var1 == 0) {
               var7 = 0.0F;
               var5 = 1.0F;
            }

            LineDrawer.addLine(var2.x - 0.05F, var2.y - 0.05F, var2.z, var2.x + 0.05F, var2.y + 0.05F, var2.z, var7, var4, var5, (String)null, false);
         }

         closestPointOnPath(this.chr.getX(), this.chr.getY(), this.chr.getZ(), this.chr, this.path, pointOnPath);
         LineDrawer.addLine(pointOnPath.x - 0.05F, pointOnPath.y - 0.05F, this.chr.getZ(), pointOnPath.x + 0.05F, pointOnPath.y + 0.05F, this.chr.getZ(), 0.0F, 1.0F, 0.0F, (String)null, false);

         for(var1 = 0; var1 < this.actualPos.size() - 1; ++var1) {
            DebugPt var6 = (DebugPt)this.actualPos.get(var1);
            DebugPt var8 = (DebugPt)this.actualPos.get(var1 + 1);
            LineDrawer.addLine(var6.x, var6.y, var6.z, var8.x, var8.y, var8.z, 1.0F, 1.0F, 1.0F, (String)null, true);
            LineDrawer.addLine(var6.x - 0.05F, var6.y - 0.05F, var6.z, var6.x + 0.05F, var6.y + 0.05F, var6.z, 1.0F, var6.climbing ? 1.0F : 0.0F, 0.0F, (String)null, false);
         }

      }
   }

   public void Succeeded(Path var1, Mover var2) {
      this.path.copyFrom(var1);
      if (!this.isCancel) {
         this.chr.setPath2(this.path);
      }

      if (!var1.isEmpty()) {
         PathNode var3 = (PathNode)var1.nodes.get(var1.nodes.size() - 1);
         this.targetX = var3.x;
         this.targetY = var3.y;
         this.targetZ = var3.z;
         if (this.isGoalSitOnFurniture()) {
            this.fixSitOnFurniturePath(this.targetX, this.targetY);
         }
      }

      this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.found;
   }

   public void Failed(Mover var1) {
      this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.failed;
   }

   public boolean isMovingUsingPathFind() {
      return !this.bStopping && !this.isGoalNone() && !this.isCancel;
   }

   public class NPCData {
      public boolean doDirectMovement;
      public int MaxSteps;
      public int nextTileX;
      public int nextTileY;
      public int nextTileZ;

      public NPCData() {
      }
   }

   private static enum Goal {
      None,
      Character,
      Location,
      Sound,
      VehicleAdjacent,
      VehicleArea,
      VehicleSeat,
      SitOnFurniture;

      private Goal() {
      }
   }

   public static enum BehaviorResult {
      Working,
      Failed,
      Succeeded;

      private BehaviorResult() {
      }
   }

   private static final class DebugPt {
      float x;
      float y;
      float z;
      boolean climbing;

      private DebugPt() {
      }

      DebugPt init(float var1, float var2, float var3, boolean var4) {
         this.x = var1;
         this.y = var2;
         this.z = var3;
         this.climbing = var4;
         return this;
      }
   }

   public static final class PointOnPath {
      int pathIndex;
      float dist;
      float x;
      float y;

      public PointOnPath() {
      }
   }
}
