package zombie.tileDepth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;
import zombie.popman.ObjectPool;
import zombie.util.StringUtils;

public final class TileGeometry {
   private TileGeometryFile m_file;
   private final String m_mediaAbsPath;

   public TileGeometry(String var1) {
      this.m_mediaAbsPath = var1;
   }

   public void init() {
      this.m_file = new TileGeometryFile();
      this.m_file.read(this.m_mediaAbsPath + "/tileGeometry.txt");
   }

   public void write() {
      this.m_file.write(this.m_mediaAbsPath + "/tileGeometry.txt");
   }

   public void setGeometry(String var1, int var2, int var3, ArrayList<TileGeometryFile.Geometry> var4) {
      TileGeometryFile.Tileset var5 = this.findTileset(var1);
      if (var5 == null) {
         var5 = new TileGeometryFile.Tileset();
         var5.name = var1;
         this.m_file.tilesets.add(var5);
      }

      TileGeometryFile.Tile var6 = var5.getOrCreateTile(var2, var3);
      var6.setGeometry(var4);
   }

   public void copyGeometry(String var1, int var2, int var3, ArrayList<TileGeometryFile.Geometry> var4) {
      ArrayList var5 = new ArrayList();

      for(int var6 = 0; var6 < var4.size(); ++var6) {
         TileGeometryFile.Geometry var7 = (TileGeometryFile.Geometry)((TileGeometryFile.Geometry)var4.get(var6)).clone();
         var5.add(var7);
      }

      this.setGeometry(var1, var2, var3, var5);
   }

   public ArrayList<TileGeometryFile.Geometry> getGeometry(String var1, int var2, int var3) {
      TileGeometryFile.Tileset var4 = this.findTileset(var1);
      if (var4 == null) {
         return null;
      } else {
         TileGeometryFile.Tile var5 = var4.getTile(var2, var3);
         return var5 == null ? null : var5.m_geometry;
      }
   }

   public void setProperty(String var1, int var2, int var3, String var4, String var5) {
      if (!StringUtils.isNullOrWhitespace(var4)) {
         TileGeometryFile.Tileset var6 = this.findTileset(var1);
         if (var6 == null) {
            if (var5 == null) {
               return;
            }

            var6 = new TileGeometryFile.Tileset();
            var6.name = var1;
            this.m_file.tilesets.add(var6);
         }

         TileGeometryFile.Tile var7 = var6.getOrCreateTile(var2, var3);
         if (var7 != null) {
            if (var7.m_properties == null) {
               if (var5 == null) {
                  return;
               }

               var7.m_properties = new HashMap();
            }

            if (var5 == null) {
               var7.m_properties.remove(var4.trim());
            } else {
               var7.m_properties.put(var4.trim(), var5.trim());
            }

         }
      }
   }

   public String getProperty(String var1, int var2, int var3, String var4) {
      if (StringUtils.isNullOrWhitespace(var4)) {
         return null;
      } else {
         TileGeometryFile.Tileset var5 = this.findTileset(var1);
         if (var5 == null) {
            return null;
         } else {
            TileGeometryFile.Tile var6 = var5.getTile(var2, var3);
            if (var6 == null) {
               return null;
            } else {
               return var6.m_properties == null ? null : (String)var6.m_properties.get(var4.trim());
            }
         }
      }
   }

   TileGeometryFile.Tileset findTileset(String var1) {
      Iterator var2 = this.m_file.tilesets.iterator();

      TileGeometryFile.Tileset var3;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         var3 = (TileGeometryFile.Tileset)var2.next();
      } while(!var1.equals(var3.name));

      return var3;
   }

   TileGeometryFile.Tile getTile(String var1, int var2, int var3) {
      TileGeometryFile.Tileset var4 = this.findTileset(var1);
      return var4 == null ? null : var4.getTile(var2, var3);
   }

   TileGeometryFile.Tile getOrCreateTile(String var1, int var2, int var3) {
      TileGeometryFile.Tileset var4 = this.findTileset(var1);
      if (var4 == null) {
         var4 = new TileGeometryFile.Tileset();
         var4.name = var1;
         this.m_file.tilesets.add(var4);
      }

      return var4.getOrCreateTile(var2, var3);
   }

   void initSpriteProperties() {
      Iterator var1 = this.m_file.tilesets.iterator();

      while(var1.hasNext()) {
         TileGeometryFile.Tileset var2 = (TileGeometryFile.Tileset)var1.next();
         var2.initSpriteProperties();
      }

   }

   void renderGeometry(IsoObject var1) {
      IsoSprite var2 = var1.getSprite();
      if (var2 != null) {
         Texture var3 = var2.getTextureForCurrentFrame(var1.getDir());
         if (var3 != null && var3.getName() != null) {
            TileGeometryFile.Tileset var4 = this.findTileset(var3.getName().substring(0, var3.getName().lastIndexOf(95)));
            if (var4 != null) {
               TileGeometryFile.Tile var5 = var4.getTile(var2.tileSheetIndex % 8, var2.tileSheetIndex / 8);
               if (var5 != null) {
                  SpriteRenderer.instance.drawGeneric(((Drawer)TileGeometry.Drawer.s_pool.alloc()).init(var5, (float)var1.square.x, (float)var1.square.y, (float)var1.square.z));
               }
            }
         }
      }
   }

   public void Reset() {
      this.m_file.Reset();
      this.m_file = null;
   }

   static final class Drawer extends TextureDraw.GenericDrawer {
      static final ObjectPool<Drawer> s_pool = new ObjectPool(Drawer::new);
      TileGeometryFile.Tile m_tile;
      float x;
      float y;
      float z;

      Drawer() {
      }

      Drawer init(TileGeometryFile.Tile var1, float var2, float var3, float var4) {
         this.m_tile = var1;
         Iterator var5 = this.m_tile.m_geometry.iterator();

         while(var5.hasNext()) {
            TileGeometryFile.Geometry var6 = (TileGeometryFile.Geometry)var5.next();
            TileGeometryFile.Polygon var7 = var6.asPolygon();
            if (var7 != null && var7.m_triangles.isEmpty()) {
               var7.triangulate();
            }
         }

         this.x = var2;
         this.y = var3;
         this.z = var4;
         return this;
      }

      public void render() {
         Core.getInstance().DoPushIsoStuff(this.x + 0.5F, this.y + 0.5F, this.z, 0.0F, false);
         GL11.glDisable(3553);
         GL11.glPolygonMode(1032, 6914);
         GL11.glMatrixMode(5888);
         GL11.glDepthMask(true);
         GL11.glEnable(2929);
         boolean var1 = false;
         GL11.glColorMask(var1, var1, var1, var1);
         GL11.glScalef(-0.6666667F, 0.6666667F, 0.6666667F);

         for(Iterator var2 = this.m_tile.m_geometry.iterator(); var2.hasNext(); GL11.glEnd()) {
            TileGeometryFile.Geometry var3 = (TileGeometryFile.Geometry)var2.next();
            GL11.glBegin(4);
            TileGeometryFile.Polygon var4 = var3.asPolygon();
            if (var4 != null) {
               for(int var5 = 0; var5 < var4.m_triangles.size(); var5 += 9) {
                  GL11.glVertex3f(var4.m_triangles.getQuick(var5), var4.m_triangles.getQuick(var5 + 1), var4.m_triangles.getQuick(var5 + 2));
                  GL11.glVertex3f(var4.m_triangles.getQuick(var5 + 3), var4.m_triangles.getQuick(var5 + 4), var4.m_triangles.getQuick(var5 + 5));
                  GL11.glVertex3f(var4.m_triangles.getQuick(var5 + 6), var4.m_triangles.getQuick(var5 + 7), var4.m_triangles.getQuick(var5 + 8));
               }
            }
         }

         GL11.glEnable(3553);
         GL11.glPolygonMode(1032, 6914);
         GL11.glColorMask(true, true, true, true);
         GL11.glDepthMask(false);
         GL11.glDisable(2929);
         Core.getInstance().DoPopIsoStuff();
      }

      public void postRender() {
         s_pool.release((Object)this);
      }
   }
}
