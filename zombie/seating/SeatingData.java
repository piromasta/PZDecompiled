package zombie.seating;

import java.util.HashMap;
import java.util.Iterator;
import org.joml.Vector3f;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.util.StringUtils;

public final class SeatingData {
   private SeatingFile m_file;
   private final String m_mediaAbsPath;

   public SeatingData(String var1) {
      this.m_mediaAbsPath = var1;
   }

   public void init() {
      this.m_file = new SeatingFile();
      this.m_file.read(this.m_mediaAbsPath + "/seating.txt");
   }

   public void initMerged() {
      this.m_file = new SeatingFile();
   }

   public void write() {
      this.m_file.write(this.m_mediaAbsPath + "/seating.txt");
   }

   public void setProperty(String var1, int var2, int var3, String var4, String var5) {
      if (!StringUtils.isNullOrWhitespace(var4)) {
         SeatingFile.Tileset var6 = this.findTileset(var1);
         if (var6 == null) {
            if (var5 == null) {
               return;
            }

            var6 = new SeatingFile.Tileset();
            var6.name = var1;
            this.m_file.tilesets.add(var6);
         }

         SeatingFile.Tile var7 = var6.getOrCreateTile(var2, var3);
         if (var5 == null) {
            var7.properties.remove(var4.trim());
         } else {
            var7.properties.put(var4.trim(), var5.trim());
         }

      }
   }

   public String getProperty(String var1, int var2, int var3, String var4) {
      if (StringUtils.isNullOrWhitespace(var4)) {
         return null;
      } else {
         SeatingFile.Tile var5 = this.getTile(var1, var2, var3);
         return var5 == null ? null : (String)var5.properties.get(var4.trim());
      }
   }

   public int addPosition(String var1, int var2, int var3, String var4) {
      SeatingFile.Tile var5 = this.getOrCreateTile(var1, var2, var3);
      SeatingFile.Position var6 = new SeatingFile.Position();
      var6.id = var4.trim();
      var5.positions.add(var6);
      return var5.positions.size() - 1;
   }

   public void removePosition(String var1, int var2, int var3, int var4) {
      SeatingFile.Tile var5 = this.getTile(var1, var2, var3);
      var5.positions.remove(var4);
   }

   public int getPositionCount(String var1, int var2, int var3) {
      SeatingFile.Tile var4 = this.getTile(var1, var2, var3);
      return var4 == null ? 0 : var4.positions.size();
   }

   public SeatingFile.Position getPositionByIndex(String var1, int var2, int var3, int var4) {
      SeatingFile.Tile var5 = this.getTile(var1, var2, var3);
      return var5 == null ? null : (SeatingFile.Position)var5.positions.get(var4);
   }

   public String getPositionID(String var1, int var2, int var3, int var4) {
      SeatingFile.Tile var5 = this.getTile(var1, var2, var3);
      SeatingFile.Position var6 = (SeatingFile.Position)var5.positions.get(var4);
      return var6.id;
   }

   public SeatingFile.Position getPositionWithID(String var1, int var2, int var3, String var4) {
      SeatingFile.Tile var5 = this.getTile(var1, var2, var3);
      if (var5 == null) {
         return null;
      } else {
         Iterator var6 = var5.positions.iterator();

         SeatingFile.Position var7;
         do {
            if (!var6.hasNext()) {
               return null;
            }

            var7 = (SeatingFile.Position)var6.next();
         } while(!var7.id.equalsIgnoreCase(var4));

         return var7;
      }
   }

   public boolean hasPositionWithID(String var1, int var2, int var3, String var4) {
      return this.getPositionWithID(var1, var2, var3, var4) != null;
   }

   public Vector3f getPositionTranslate(String var1, int var2, int var3, int var4) {
      SeatingFile.Tile var5 = this.getTile(var1, var2, var3);
      SeatingFile.Position var6 = (SeatingFile.Position)var5.positions.get(var4);
      return var6.translate;
   }

   public HashMap<String, String> getPositionProperties(String var1, int var2, int var3, int var4) {
      SeatingFile.Tile var5 = this.getTile(var1, var2, var3);
      if (var5 == null) {
         return null;
      } else {
         SeatingFile.Position var6 = (SeatingFile.Position)var5.positions.get(var4);
         return var6.properties;
      }
   }

   public String getPositionProperty(String var1, int var2, int var3, int var4, String var5) {
      SeatingFile.Tile var6 = this.getTile(var1, var2, var3);
      if (var6 == null) {
         return null;
      } else {
         SeatingFile.Position var7 = var6.getPositionByIndex(var4);
         return var7 == null ? null : (String)var7.properties.get(var5);
      }
   }

   SeatingFile.Tileset findTileset(String var1) {
      Iterator var2 = this.m_file.tilesets.iterator();

      SeatingFile.Tileset var3;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         var3 = (SeatingFile.Tileset)var2.next();
      } while(!var1.equals(var3.name));

      return var3;
   }

   SeatingFile.Tileset getOrCreateTileset(String var1) {
      SeatingFile.Tileset var2 = this.findTileset(var1);
      if (var2 == null) {
         var2 = new SeatingFile.Tileset();
         var2.name = var1;
         this.m_file.tilesets.add(var2);
      }

      return var2;
   }

   SeatingFile.Tile getTile(String var1, int var2, int var3) {
      SeatingFile.Tileset var4 = this.findTileset(var1);
      return var4 == null ? null : var4.getTile(var2, var3);
   }

   SeatingFile.Tile getOrCreateTile(String var1, int var2, int var3) {
      SeatingFile.Tileset var4 = this.getOrCreateTileset(var1);
      return var4.getOrCreateTile(var2, var3);
   }

   void mergeTilesets(SeatingData var1) {
      Iterator var2 = var1.m_file.tilesets.iterator();

      while(var2.hasNext()) {
         SeatingFile.Tileset var3 = (SeatingFile.Tileset)var2.next();
         SeatingFile.Tileset var4 = this.getOrCreateTileset(var3.name);
         var4.merge(var3);
      }

   }

   public void Reset() {
      if (this.m_file != null) {
         this.m_file.Reset();
         this.m_file = null;
      }

   }

   public void fixDefaultPositions() {
      Iterator var1 = this.m_file.tilesets.iterator();

      while(var1.hasNext()) {
         SeatingFile.Tileset var2 = (SeatingFile.Tileset)var1.next();
         Iterator var3 = var2.tiles.iterator();

         while(var3.hasNext()) {
            SeatingFile.Tile var4 = (SeatingFile.Tile)var3.next();
            Iterator var5 = var4.positions.iterator();

            while(var5.hasNext()) {
               SeatingFile.Position var6 = (SeatingFile.Position)var5.next();
               if ("default".equalsIgnoreCase(var6.id)) {
                  String var7 = String.format("%s_%d", var2.name, var4.col + var4.row * 8);
                  IsoSprite var8 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var7);
                  if (var8 != null) {
                     String var9 = var8.getProperties().Val("Facing");
                     if (var9 != null) {
                        var6.id = var9;
                     }
                  }
               }
            }
         }
      }

   }
}
