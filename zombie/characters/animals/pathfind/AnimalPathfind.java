package zombie.characters.animals.pathfind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;
import zombie.popman.ObjectPool;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.UIWorldMapV1;
import zombie.worldMap.WorldMapRenderer;

public class AnimalPathfind implements IPathRenderer {
   private static AnimalPathfind instance;
   final ObjectPool<Vector2f> vector2fObjectPool = new ObjectPool(Vector2f::new);
   final ObjectPool<Vector3f> vector3fObjectPool = new ObjectPool(Vector3f::new);
   final MeshList meshList = new MeshList();
   final HighLevelAStar meshAStar;
   final LowLevelAStar cdAStar;
   final TriangleDrawer[] drawer;
   UIWorldMap uiWorldMap;
   UIWorldMapV1 uiWorldMapV1;
   final HashMap<Mesh, Zone> meshZoneHashMap;
   final HashMap<Zone, Mesh> zoneMeshHashMap;
   MeshWanderer meshWanderer;

   public AnimalPathfind() {
      this.meshAStar = new HighLevelAStar(this.meshList);
      this.cdAStar = new LowLevelAStar(this.meshList);
      this.drawer = new TriangleDrawer[3];
      this.uiWorldMap = null;
      this.uiWorldMapV1 = null;
      this.meshZoneHashMap = new HashMap();
      this.zoneMeshHashMap = new HashMap();
      this.meshWanderer = new MeshWanderer();
   }

   public static AnimalPathfind getInstance() {
      if (instance == null) {
         instance = new AnimalPathfind();
      }

      return instance;
   }

   public void renderPath(UIWorldMap var1, Zone var2, float var3, float var4, float var5, float var6) {
      this.uiWorldMap = var1;
      this.uiWorldMapV1 = var1.getAPIv1();
      this.cdAStar.renderer = this;
      this.meshList.m_meshes.clear();
      this.createMeshesFromZonesInArea((int)var3 - 300, (int)var4 - 300, 600, 600);
      Iterator var7 = this.meshList.m_meshes.iterator();

      Mesh var8;
      while(var7.hasNext()) {
         var8 = (Mesh)var7.next();
         this.cdAStar.initOffMeshConnections(var8);
      }

      byte var9 = 0;
      var8 = this.meshList.getMeshAt(var3, var4, var9);
      if (var8 != null) {
         this.meshList.m_meshes.clear();
         var8.gatherConnectedMeshes(this.meshList.m_meshes);
      }
   }

   private void createMeshesFromZonesInArea(int var1, int var2, int var3, int var4) {
      int var5 = (var1 + 300) / 300;
      int var6 = (var2 + 300) / 300;
      IsoMetaCell var7 = IsoWorld.instance.MetaGrid.getCellData(var5, var6);
      if (var7 != null) {
         ArrayList var8 = new ArrayList();

         int var9;
         for(var9 = 0; var9 < var7.getAnimalZonesSize(); ++var9) {
            var8.add(var7.getAnimalZone(var9));
         }

         for(var9 = 0; var9 < var8.size(); ++var9) {
            Zone var10 = (Zone)var8.get(var9);
            Mesh var11 = (Mesh)this.zoneMeshHashMap.get(var10);
            if (var11 != null) {
               this.meshList.m_meshes.add(var11);
            } else {
               if (var10.isRectangle()) {
               }

               if (var10.getPolygonTriangles() != null) {
                  var11 = new Mesh();
                  var11.meshList = this.meshList;
                  var11.initFromZone(var10);
                  this.meshList.m_meshes.add(var11);
                  this.meshZoneHashMap.put(var11, var10);
                  this.zoneMeshHashMap.put(var10, var11);
               }
            }
         }

      }
   }

   public void drawTriangleCentroid(Mesh var1, int var2, float var3, float var4, float var5, float var6) {
      Vector2f var7 = (Vector2f)this.meshWanderer.m_mesh.triangles.get(var2);
      Vector2f var8 = (Vector2f)this.meshWanderer.m_mesh.triangles.get(var2 + 1);
      Vector2f var9 = (Vector2f)this.meshWanderer.m_mesh.triangles.get(var2 + 2);
      float var10 = (var7.x + var8.x + var9.x) / 3.0F;
      float var11 = (var7.y + var8.y + var9.y) / 3.0F;
      this.drawRect(var10 - 1.0F, var11 - 1.0F, 2.0F, 2.0F, var3, var4, var5, var6);
   }

   public void drawLine(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      WorldMapRenderer var9 = this.uiWorldMapV1.getRenderer();
      float var10 = var9.worldToUIX(var1, var2, var9.getDisplayZoomF(), var9.getCenterWorldX(), var9.getCenterWorldY(), var9.getModelViewMatrix(), var9.getProjectionMatrix());
      float var11 = var9.worldToUIY(var1, var2, var9.getDisplayZoomF(), var9.getCenterWorldX(), var9.getCenterWorldY(), var9.getModelViewMatrix(), var9.getProjectionMatrix());
      float var12 = var9.worldToUIX(var3, var4, var9.getDisplayZoomF(), var9.getCenterWorldX(), var9.getCenterWorldY(), var9.getModelViewMatrix(), var9.getProjectionMatrix());
      float var13 = var9.worldToUIY(var3, var4, var9.getDisplayZoomF(), var9.getCenterWorldX(), var9.getCenterWorldY(), var9.getModelViewMatrix(), var9.getProjectionMatrix());
      SpriteRenderer.instance.renderline((Texture)null, (int)var10, (int)var11, (int)var12, (int)var13, var5, var6, var7, var8, 1.0F);
   }

   public void drawRect(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      this.drawLine(var1, var2, var1 + var3, var2, var5, var6, var7, var8);
      this.drawLine(var1 + var3, var2, var1 + var3, var2 + var4, var5, var6, var7, var8);
      this.drawLine(var1, var2 + var4, var1 + var3, var2 + var4, var5, var6, var7, var8);
      this.drawLine(var1, var2, var1, var2 + var4, var5, var6, var7, var8);
   }
}
