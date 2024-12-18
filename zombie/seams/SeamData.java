package zombie.seams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.util.StringUtils;

public final class SeamData {
   private SeamFile m_file;
   private final String m_mediaAbsPath;

   public SeamData(String var1) {
      this.m_mediaAbsPath = var1;
   }

   public void init() {
      this.m_file = new SeamFile();
      this.m_file.read(this.m_mediaAbsPath + "/seams.txt");
   }

   public void write() {
      this.m_file.write(this.m_mediaAbsPath + "/seams.txt");
   }

   public void setProperty(String var1, int var2, int var3, String var4, String var5) {
      if (!StringUtils.isNullOrWhitespace(var4)) {
         SeamFile.Tileset var6 = this.findTileset(var1);
         if (var6 == null) {
            if (var5 == null) {
               return;
            }

            var6 = new SeamFile.Tileset();
            var6.name = var1;
            this.m_file.tilesets.add(var6);
         }

         SeamFile.Tile var7 = var6.getOrCreateTile(var2, var3);
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
         SeamFile.Tileset var5 = this.findTileset(var1);
         if (var5 == null) {
            return null;
         } else {
            SeamFile.Tile var6 = var5.getTile(var2, var3);
            if (var6 == null) {
               return null;
            } else {
               return var6.m_properties == null ? null : (String)var6.m_properties.get(var4.trim());
            }
         }
      }
   }

   public ArrayList<String> getTileJoinE(String var1, int var2, int var3, boolean var4) {
      SeamFile.Tile var5 = var4 ? this.getOrCreateTile(var1, var2, var3) : this.getTile(var1, var2, var3);
      if (var5 == null) {
         return null;
      } else {
         if (var4 && var5.joinE == null) {
            var5.joinE = new ArrayList();
         }

         return var5.joinE;
      }
   }

   public ArrayList<String> getTileJoinS(String var1, int var2, int var3, boolean var4) {
      SeamFile.Tile var5 = var4 ? this.getOrCreateTile(var1, var2, var3) : this.getTile(var1, var2, var3);
      if (var5 == null) {
         return null;
      } else {
         if (var4 && var5.joinS == null) {
            var5.joinS = new ArrayList();
         }

         return var5.joinS;
      }
   }

   public ArrayList<String> getTileJoinBelowE(String var1, int var2, int var3, boolean var4) {
      SeamFile.Tile var5 = var4 ? this.getOrCreateTile(var1, var2, var3) : this.getTile(var1, var2, var3);
      if (var5 == null) {
         return null;
      } else {
         if (var4 && var5.joinBelowE == null) {
            var5.joinBelowE = new ArrayList();
         }

         return var5.joinBelowE;
      }
   }

   public ArrayList<String> getTileJoinBelowS(String var1, int var2, int var3, boolean var4) {
      SeamFile.Tile var5 = var4 ? this.getOrCreateTile(var1, var2, var3) : this.getTile(var1, var2, var3);
      if (var5 == null) {
         return null;
      } else {
         if (var4 && var5.joinBelowS == null) {
            var5.joinBelowS = new ArrayList();
         }

         return var5.joinBelowS;
      }
   }

   SeamFile.Tileset findTileset(String var1) {
      Iterator var2 = this.m_file.tilesets.iterator();

      SeamFile.Tileset var3;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         var3 = (SeamFile.Tileset)var2.next();
      } while(!var1.equals(var3.name));

      return var3;
   }

   SeamFile.Tile getTile(String var1, int var2, int var3) {
      SeamFile.Tileset var4 = this.findTileset(var1);
      return var4 == null ? null : var4.getTile(var2, var3);
   }

   SeamFile.Tile getOrCreateTile(String var1, int var2, int var3) {
      SeamFile.Tileset var4 = this.findTileset(var1);
      if (var4 == null) {
         var4 = new SeamFile.Tileset();
         var4.name = var1;
         this.m_file.tilesets.add(var4);
      }

      return var4.getOrCreateTile(var2, var3);
   }

   public void Reset() {
      this.m_file.Reset();
      this.m_file = null;
   }
}
