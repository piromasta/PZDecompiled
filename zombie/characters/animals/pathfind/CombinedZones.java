package zombie.characters.animals.pathfind;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.joml.Vector2f;
import zombie.characters.animals.AnimalZone;
import zombie.characters.animals.VirtualAnimal;
import zombie.characters.animals.VirtualAnimalState;
import zombie.core.math.PZMath;
import zombie.iso.zones.Zone;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.Clipper;
import zombie.worldMap.UIWorldMap;

public final class CombinedZones {
   private static Clipper s_clipper = null;
   private static ByteBuffer s_clipperBuffer = null;
   static final HashMap<Zone, CombinedZones> combinedZonesMap = new HashMap();
   final HashMap<Zone, MeshList> zoneMeshListMap = new HashMap();
   public final MeshList m_meshList = new MeshList();
   public final MeshList m_combinedMeshList = new MeshList();
   final LowLevelAStar cdAStar = new LowLevelAStar((MeshList)null);
   final VirtualAnimal virtualAnimal = new VirtualAnimal();

   public CombinedZones() {
   }

   public void init(MeshList var1) {
      Iterator var2 = var1.m_meshes.iterator();

      Mesh var3;
      while(var2.hasNext()) {
         var3 = (Mesh)var2.next();
         Mesh var4 = new Mesh();
         var4.initFrom(var3);
         var4.meshList = new MeshList();
         var4.meshList.m_meshes.add(var4);
         this.zoneMeshListMap.put(var3.zone, var4.meshList);
         this.m_meshList.m_meshes.add(var4);
      }

      this.cdAStar.renderer = AnimalPathfind.getInstance();
      if (s_clipper == null) {
         s_clipper = new Clipper();
      }

      s_clipper.clear();

      int var9;
      int var11;
      for(var9 = 0; var9 < var1.size(); ++var9) {
         var3 = var1.get(var9);
         if (s_clipperBuffer == null || s_clipperBuffer.capacity() < var3.polygon.size() * 8 * 4) {
            s_clipperBuffer = ByteBuffer.allocateDirect(var3.polygon.size() * 8 * 4);
         }

         s_clipperBuffer.clear();
         Vector2f var5;
         if (this.isClockwise(var3.polygon)) {
            for(var11 = var3.polygon.size() - 1; var11 >= 0; --var11) {
               var5 = (Vector2f)var3.polygon.get(var11);
               s_clipperBuffer.putFloat(var5.x);
               s_clipperBuffer.putFloat(var5.y);
            }
         } else {
            for(var11 = 0; var11 < var3.polygon.size(); ++var11) {
               var5 = (Vector2f)var3.polygon.get(var11);
               s_clipperBuffer.putFloat(var5.x);
               s_clipperBuffer.putFloat(var5.y);
            }
         }

         s_clipper.addPath(var3.polygon.size(), s_clipperBuffer, false);
      }

      var9 = s_clipper.generatePolygons();
      if (var9 > 0) {
         for(int var10 = 0; var10 < var9; ++var10) {
            s_clipperBuffer.clear();
            s_clipper.getPolygon(var10, s_clipperBuffer);
            short var12 = s_clipperBuffer.getShort();
            if (var12 >= 3) {
               Mesh var13 = new Mesh();
               var13.meshList = this.m_combinedMeshList;

               int var6;
               float var7;
               float var8;
               for(var6 = 0; var6 < var12; ++var6) {
                  var7 = s_clipperBuffer.getFloat();
                  var8 = s_clipperBuffer.getFloat();
                  var13.polygon.add(new Vector2f(var7, var8));
               }

               s_clipperBuffer.clear();
               if (s_clipperBuffer.capacity() < var12 * 8 * 4) {
                  s_clipperBuffer = ByteBuffer.allocateDirect(var12 * 8 * 4);
               }

               var11 = s_clipper.triangulate(var10, s_clipperBuffer);

               for(var6 = 0; var6 < var11; ++var6) {
                  var7 = s_clipperBuffer.getFloat();
                  var8 = s_clipperBuffer.getFloat();
                  var13.triangles.add(new Vector2f(var7, var8));
               }

               var13.initEdges();
               var13.initAdjacentTriangles();
               this.m_combinedMeshList.m_meshes.add(var13);
            }
         }

         var3 = (Mesh)this.m_meshList.m_meshes.get(0);
         AnimalZone var15 = (AnimalZone)var3.zone;
         if ("Eat".equals(var15.getAction())) {
            this.virtualAnimal.setState(new StateEat(this.virtualAnimal));
         }

         if ("Follow".equals(var15.getAction())) {
            this.virtualAnimal.setState(new StateFollow(this.virtualAnimal));
         }

         if ("Sleep".equals(var15.getAction())) {
            this.virtualAnimal.setState(new StateSleep(this.virtualAnimal));
         }

         BaseState var14 = (BaseState)this.virtualAnimal.getState();
         var14.m_combinedZones = this;
         var14.m_mesh = var3;
         Vector2f var16 = var14.m_mesh.pickRandomPoint(new Vector2f());
         this.virtualAnimal.setX(var16.x);
         this.virtualAnimal.setY(var16.y);
      }
   }

   boolean isClockwise(ArrayList<Vector2f> var1) {
      float var2 = 0.0F;

      for(int var3 = 0; var3 < var1.size(); ++var3) {
         float var4 = ((Vector2f)var1.get(var3)).x;
         float var5 = ((Vector2f)var1.get(var3)).y;
         float var6 = ((Vector2f)var1.get((var3 + 1) % var1.size())).x;
         float var7 = ((Vector2f)var1.get((var3 + 1) % var1.size())).y;
         var2 += (var6 - var4) * (var7 + var5);
      }

      return (double)var2 > 0.0;
   }

   float getLength(float[] var1) {
      float var2 = 0.0F;

      for(int var3 = 0; var3 < var1.length; var3 += 2) {
         float var4 = var1[var3];
         float var5 = var1[var3 + 1];
         float var6 = var1[(var3 + 2) % var1.length];
         float var7 = var1[(var3 + 3) % var1.length];
         var2 += Vector2f.length(var6 - var4, var7 - var5);
      }

      return var2;
   }

   public static void renderPath(UIWorldMap var0, Zone var1, float var2, float var3, float var4, float var5) {
      AnimalPathfind var6 = AnimalPathfind.getInstance();
      if (!var6.meshList.m_meshes.isEmpty()) {
         CombinedZones var7 = (CombinedZones)combinedZonesMap.get(var1);
         if (var7 == null) {
            var7 = new CombinedZones();
            var7.init(var6.meshList);
            Iterator var8 = AnimalPathfind.getInstance().meshList.m_meshes.iterator();

            while(var8.hasNext()) {
               Mesh var9 = (Mesh)var8.next();
               combinedZonesMap.put(var9.zone, var7);
            }
         }

         var7.render(var6, var2, var3, var4, var5);
         float var11 = var0.getAPI().worldToUIX(var7.virtualAnimal.getX(), var7.virtualAnimal.getY());
         float var12 = var0.getAPI().worldToUIY(var7.virtualAnimal.getX(), var7.virtualAnimal.getY());
         var11 = PZMath.floor(var11);
         var12 = PZMath.floor(var12);
         BaseState var10 = (BaseState)var7.virtualAnimal.getState();
         var0.DrawTextCentre(var10.getClass().getSimpleName() + " / " + ((AnimalZone)var10.m_mesh.zone).getAction(), (double)var11, (double)var12 + 4.0, 0.0, 0.0, 0.0, 1.0);
      }
   }

   public void render(IPathRenderer var1, float var2, float var3, float var4, float var5) {
      Iterator var6 = this.m_combinedMeshList.m_meshes.iterator();

      while(var6.hasNext()) {
         Mesh var7 = (Mesh)var6.next();
         var7.renderTriangles(var1, 1.0F, 1.0F, 1.0F, 1.0F);

         for(int var8 = 0; var8 < var7.edgesOnBoundaries.size(); ++var8) {
            short var9 = var7.trianglesOnBoundaries.get(var8);
            short var10 = var7.edgesOnBoundaries.get(var8);
            float var11 = ((Vector2f)var7.triangles.get(var9)).x();
            float var12 = ((Vector2f)var7.triangles.get(var9)).y();
            float var13 = ((Vector2f)var7.triangles.get(var9 + 1)).x();
            float var14 = ((Vector2f)var7.triangles.get(var9 + 1)).y();
            float var15 = ((Vector2f)var7.triangles.get(var9 + 2)).x();
            float var16 = ((Vector2f)var7.triangles.get(var9 + 2)).y();
            if ((var10 & 1) != 0) {
               var1.drawLine(var11, var12, var13, var14, 0.0F, 0.0F, 1.0F, 1.0F);
            }

            if ((var10 & 2) != 0) {
               var1.drawLine(var13, var14, var15, var16, 0.0F, 0.0F, 1.0F, 1.0F);
            }

            if ((var10 & 4) != 0) {
               var1.drawLine(var11, var12, var15, var16, 0.0F, 0.0F, 1.0F, 1.0F);
            }
         }

         var7.renderOffMeshConnections(var1, 1.0F, 0.0F, 0.0F, 1.0F);
         var7.renderPoints(var1, 1.0F, 1.0F, 1.0F, 1.0F);
      }

      BaseState var17 = (BaseState)this.virtualAnimal.getState();
      var17.m_mesh.renderOutline(var1, 0.0F, 1.0F, 0.0F, 1.0F);
      byte var18 = 0;
      this.cdAStar.setMeshList(this.m_combinedMeshList);
      this.cdAStar.findPath(var2, var3, var18, var4, var5, var18, (ArrayList)null);
      this.virtualAnimal.getState().update();
      float var19 = 1.0F;
      var1.drawRect(this.virtualAnimal.getX() - var19 / 2.0F, this.virtualAnimal.getY() - var19 / 2.0F, var19, var19, 0.0F, 0.0F, 1.0F, 1.0F);
   }

   public static class StateEat extends BaseState {
      public StateEat(VirtualAnimal var1) {
         super(var1);
      }

      public void reachedEnd() {
         super.reachedEnd();
         if (this.m_counter >= 5) {
            Iterator var1 = this.m_combinedZones.m_meshList.m_meshes.iterator();

            while(var1.hasNext()) {
               Mesh var2 = (Mesh)var1.next();
               if (var2.zone instanceof AnimalZone && ((AnimalZone)var2.zone).getAction().equals("Sleep")) {
                  StateSleep var3 = new StateSleep(this.m_combinedZones.virtualAnimal);
                  var3.m_combinedZones = this.m_combinedZones;
                  var3.m_mesh = var2;
                  this.m_combinedZones.virtualAnimal.setState(var3);
                  break;
               }
            }

         }
      }
   }

   public static class StateFollow extends BaseState {
      public StateFollow(VirtualAnimal var1) {
         super(var1);
      }

      public void reachedEnd() {
         super.reachedEnd();
         if (this.m_counter >= 5) {
            Iterator var1 = this.m_combinedZones.m_meshList.m_meshes.iterator();

            while(var1.hasNext()) {
               Mesh var2 = (Mesh)var1.next();
               if (var2.zone instanceof AnimalZone && ((AnimalZone)var2.zone).getAction().equals("Eat")) {
                  StateEat var3 = new StateEat(this.m_combinedZones.virtualAnimal);
                  var3.m_combinedZones = this.m_combinedZones;
                  var3.m_mesh = var2;
                  this.m_combinedZones.virtualAnimal.setState(var3);
                  break;
               }
            }

         }
      }
   }

   public static class StateSleep extends BaseState {
      public StateSleep(VirtualAnimal var1) {
         super(var1);
      }

      public void reachedEnd() {
         super.reachedEnd();
         if (this.m_counter >= 5) {
            Iterator var1 = this.m_combinedZones.m_meshList.m_meshes.iterator();

            while(var1.hasNext()) {
               Mesh var2 = (Mesh)var1.next();
               if (var2.zone instanceof AnimalZone && ((AnimalZone)var2.zone).getAction().equals("Follow")) {
                  StateFollow var3 = new StateFollow(this.m_combinedZones.virtualAnimal);
                  var3.m_combinedZones = this.m_combinedZones;
                  var3.m_mesh = var2;
                  this.m_combinedZones.virtualAnimal.setState(var3);
                  break;
               }
            }

         }
      }
   }

   public static class BaseState extends VirtualAnimalState {
      CombinedZones m_combinedZones;
      Mesh m_mesh;
      ArrayList<Vector2f> m_path = new ArrayList();
      float m_pathLength;
      float m_distanceAlongPath;
      int m_counter = 5;

      public BaseState(VirtualAnimal var1) {
         super(var1);
      }

      public void update() {
         if (this.m_mesh != null) {
            Vector2f var1;
            if (this.m_path.isEmpty()) {
               var1 = this.m_mesh.pickRandomPoint(new Vector2f());
               AnimalPathfind.getInstance().drawRect(var1.x - 0.5F, var1.y - 0.5F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F);
               Mesh var2 = this.m_combinedZones.m_meshList.getMeshAt(this.m_animal.getX(), this.m_animal.getY(), 0);
               Mesh var3 = this.m_combinedZones.m_meshList.getMeshAt(var1.x, var1.y, 0);
               if (var2 != var3) {
                  this.m_combinedZones.cdAStar.setMeshList(this.m_combinedZones.m_combinedMeshList);
               } else {
                  MeshList var4 = (MeshList)this.m_combinedZones.zoneMeshListMap.get(var2.zone);
                  this.m_combinedZones.cdAStar.setMeshList(var4);
               }

               this.m_combinedZones.cdAStar.findPath(this.m_animal.getX(), this.m_animal.getY(), 0, var1.x, var1.y, 0, this.m_path);
               this.m_pathLength = this.getPolylineLength(this.m_path);
               this.m_distanceAlongPath = 0.0F;
            }

            for(int var5 = 0; var5 < this.m_path.size() - 1; ++var5) {
               Vector2f var6 = (Vector2f)this.m_path.get(var5);
               Vector2f var7 = (Vector2f)this.m_path.get(var5 + 1);
               AnimalPathfind.getInstance().drawLine(var6.x, var6.y, var7.x, var7.y, 0.0F, 1.0F, 1.0F, 1.0F);
            }

            var1 = new Vector2f();
            if (this.getPointOnPath(this.m_distanceAlongPath + 1.0F / this.m_pathLength, var1)) {
               this.m_animal.setX(var1.x);
               this.m_animal.setY(var1.y);
               this.m_distanceAlongPath += 1.0F / this.m_pathLength;
               if (this.m_distanceAlongPath >= 1.0F) {
                  this.reachedEnd();
               }
            }

         }
      }

      public void reachedEnd() {
         this.m_path.clear();
         if (--this.m_counter <= 0) {
            this.m_counter = 5;
            this.m_mesh = (Mesh)PZArrayUtil.pickRandom((List)this.m_combinedZones.m_meshList.m_meshes);
         }

      }

      float getPolylineLength(ArrayList<Vector2f> var1) {
         float var2 = 0.0F;

         for(int var3 = 0; var3 < var1.size() - 1; ++var3) {
            float var4 = ((Vector2f)var1.get(var3)).x;
            float var5 = ((Vector2f)var1.get(var3)).y;
            float var6 = ((Vector2f)var1.get(var3 + 1)).x;
            float var7 = ((Vector2f)var1.get(var3 + 1)).y;
            var2 += Vector2f.length(var6 - var4, var7 - var5);
         }

         return var2;
      }

      boolean getPointOnPath(float var1, Vector2f var2) {
         var1 = PZMath.clampFloat(var1, 0.0F, 1.0F);
         var2.set(0.0F);
         float var3 = this.m_pathLength;
         if (var3 <= 0.0F) {
            return false;
         } else {
            float var4 = var3 * var1;
            float var5 = 0.0F;

            for(int var6 = 0; var6 < this.m_path.size() - 1; ++var6) {
               float var7 = ((Vector2f)this.m_path.get(var6)).x;
               float var8 = ((Vector2f)this.m_path.get(var6)).y;
               float var9 = ((Vector2f)this.m_path.get(var6 + 1)).x;
               float var10 = ((Vector2f)this.m_path.get(var6 + 1)).y;
               float var11 = Vector2f.length(var9 - var7, var10 - var8);
               if (var5 + var11 >= var4) {
                  float var12 = (var4 - var5) / var11;
                  var2.set(var7 + (var9 - var7) * var12, var8 + (var10 - var8) * var12);
                  return true;
               }

               var5 += var11;
            }

            return false;
         }
      }
   }
}
