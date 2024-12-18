package zombie.pot;

import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import zombie.GameWindow;
import zombie.core.math.PZMath;
import zombie.worldMap.WorldMapCell;
import zombie.worldMap.WorldMapFeature;
import zombie.worldMap.WorldMapGeometry;
import zombie.worldMap.WorldMapPoints;
import zombie.worldMap.WorldMapProperties;

public final class POTWorldMapData {
   static final int VERSION1 = 1;
   static final int VERSION2 = 2;
   static final int VERSION_LATEST = 2;
   static final TObjectIntHashMap<String> m_stringTable = new TObjectIntHashMap();
   public final ArrayList<WorldMapCell> m_cells = new ArrayList();
   public final HashMap<Integer, WorldMapCell> m_cellLookup = new HashMap();
   public int m_minX;
   public int m_minY;
   public int m_maxX;
   public int m_maxY;

   public POTWorldMapData() {
   }

   public WorldMapCell getCell(int var1, int var2) {
      Integer var3 = this.getCellKey(var1, var2);
      return (WorldMapCell)this.m_cellLookup.get(var3);
   }

   private Integer getCellKey(int var1, int var2) {
      return var1 + var2 * 1000;
   }

   public void addFeature(WorldMapFeature var1) {
      int var2 = this.getMinSquareX(var1) / 256;
      int var3 = this.getMinSquareY(var1) / 256;
      int var4 = this.getMaxSquareX(var1) / 256;
      int var5 = this.getMaxSquareY(var1) / 256;

      for(int var6 = var3; var6 <= var5; ++var6) {
         for(int var7 = var2; var7 <= var4; ++var7) {
            WorldMapCell var8 = this.getCell(var7, var6);
            if (var8 == null) {
               var8 = new WorldMapCell();
               var8.m_x = var7;
               var8.m_y = var6;
               this.m_cells.add(var8);
               this.m_cellLookup.put(this.getCellKey(var7, var6), var8);
            }

            WorldMapFeature var9 = new WorldMapFeature(var8);
            this.convertFeature(var9, var1);
            var8.m_features.add(var9);
         }
      }

   }

   public void saveBIN(String var1, boolean var2) throws IOException {
      ByteBuffer var3 = ByteBuffer.allocate(31457280);
      var3.order(ByteOrder.LITTLE_ENDIAN);
      var3.clear();
      var3.put((byte)73);
      var3.put((byte)71);
      var3.put((byte)77);
      var3.put((byte)66);
      var3.putInt(2);
      var3.putInt(var2 ? 256 : 300);
      var3.putInt(this.getWidthInCells());
      var3.putInt(this.getHeightInCells());
      this.writeStringTable(var3);

      for(int var4 = this.m_minY; var4 <= this.m_maxY; ++var4) {
         for(int var5 = this.m_minX; var5 <= this.m_maxX; ++var5) {
            WorldMapCell var6 = this.getCell(var5, var4);
            if (var6 == null) {
               var3.putInt(-1);
            } else {
               this.writeCell(var3, var6);
            }
         }
      }

      FileOutputStream var9 = new FileOutputStream(var1);

      try {
         var9.write(var3.array(), 0, var3.position());
      } catch (Throwable var8) {
         try {
            var9.close();
         } catch (Throwable var7) {
            var8.addSuppressed(var7);
         }

         throw var8;
      }

      var9.close();
   }

   void writeStringTable(ByteBuffer var1) {
      m_stringTable.clear();
      ArrayList var2 = new ArrayList();

      for(int var3 = this.m_minY; var3 <= this.m_maxY; ++var3) {
         for(int var4 = this.m_minX; var4 <= this.m_maxX; ++var4) {
            WorldMapCell var5 = this.getCell(var4, var3);
            if (var5 != null) {
               Iterator var6 = var5.m_features.iterator();

               while(var6.hasNext()) {
                  WorldMapFeature var7 = (WorldMapFeature)var6.next();
                  this.addString(var2, ((WorldMapGeometry)var7.m_geometries.get(0)).m_type.name());
                  Iterator var8 = var7.m_properties.entrySet().iterator();

                  while(var8.hasNext()) {
                     Map.Entry var9 = (Map.Entry)var8.next();
                     this.addString(var2, (String)var9.getKey());
                     this.addString(var2, (String)var9.getValue());
                  }
               }
            }
         }
      }

      var1.putInt(var2.size());
      Iterator var10 = var2.iterator();

      while(var10.hasNext()) {
         String var11 = (String)var10.next();
         this.SaveString(var1, var11);
      }

   }

   void addString(ArrayList<String> var1, String var2) {
      if (!m_stringTable.containsKey(var2)) {
         m_stringTable.put(var2, var1.size());
         var1.add(var2);
      }
   }

   void writeCell(ByteBuffer var1, WorldMapCell var2) {
      if (var2.m_features.isEmpty()) {
         var1.putInt(-1);
      } else {
         var1.putInt(var2.m_x);
         var1.putInt(var2.m_y);
         var1.putInt(var2.m_features.size());
         Iterator var3 = var2.m_features.iterator();

         while(var3.hasNext()) {
            WorldMapFeature var4 = (WorldMapFeature)var3.next();
            this.writeFeature(var1, var4);
         }

      }
   }

   void writeFeature(ByteBuffer var1, WorldMapFeature var2) {
      WorldMapGeometry var3 = (WorldMapGeometry)var2.m_geometries.get(0);
      this.SaveStringIndex(var1, var3.m_type.name());
      var1.put((byte)var3.m_points.size());
      Iterator var4 = var3.m_points.iterator();

      while(var4.hasNext()) {
         WorldMapPoints var5 = (WorldMapPoints)var4.next();
         var1.putShort((short)var5.numPoints());

         for(int var6 = 0; var6 < var5.numPoints(); ++var6) {
            var1.putShort((short)var5.getX(var6));
            var1.putShort((short)var5.getY(var6));
         }
      }

      var1.put((byte)var2.m_properties.size());
      var4 = var2.m_properties.entrySet().iterator();

      while(var4.hasNext()) {
         Map.Entry var7 = (Map.Entry)var4.next();
         this.SaveStringIndex(var1, (String)var7.getKey());
         this.SaveStringIndex(var1, (String)var7.getValue());
      }

   }

   void SaveString(ByteBuffer var1, String var2) {
      GameWindow.WriteStringUTF(var1, var2);
   }

   void SaveStringIndex(ByteBuffer var1, String var2) {
      var1.putShort((short)m_stringTable.get(var2));
   }

   int getMinSquareX(WorldMapFeature var1) {
      int var2 = 2147483647;

      WorldMapGeometry var4;
      for(Iterator var3 = var1.m_geometries.iterator(); var3.hasNext(); var2 = PZMath.min(var2, var4.m_minX)) {
         var4 = (WorldMapGeometry)var3.next();
      }

      return var1.m_cell.m_x * 300 + var2;
   }

   int getMinSquareY(WorldMapFeature var1) {
      int var2 = 2147483647;

      WorldMapGeometry var4;
      for(Iterator var3 = var1.m_geometries.iterator(); var3.hasNext(); var2 = PZMath.min(var2, var4.m_minY)) {
         var4 = (WorldMapGeometry)var3.next();
      }

      return var1.m_cell.m_y * 300 + var2;
   }

   int getMaxSquareX(WorldMapFeature var1) {
      int var2 = 2147483647;

      WorldMapGeometry var4;
      for(Iterator var3 = var1.m_geometries.iterator(); var3.hasNext(); var2 = PZMath.min(var2, var4.m_maxX)) {
         var4 = (WorldMapGeometry)var3.next();
      }

      return var1.m_cell.m_x * 300 + var2;
   }

   int getMaxSquareY(WorldMapFeature var1) {
      int var2 = 2147483647;

      WorldMapGeometry var4;
      for(Iterator var3 = var1.m_geometries.iterator(); var3.hasNext(); var2 = PZMath.min(var2, var4.m_maxY)) {
         var4 = (WorldMapGeometry)var3.next();
      }

      return var1.m_cell.m_y * 300 + var2;
   }

   public int getWidthInCells() {
      return this.m_maxX - this.m_minX + 1;
   }

   public int getHeightInCells() {
      return this.m_maxY - this.m_minY + 1;
   }

   void convertFeature(WorldMapFeature var1, WorldMapFeature var2) {
      Iterator var3 = var2.m_geometries.iterator();

      while(var3.hasNext()) {
         WorldMapGeometry var4 = (WorldMapGeometry)var3.next();
         WorldMapGeometry var5 = new WorldMapGeometry();
         var5.m_type = var4.m_type;
         Iterator var6 = var4.m_points.iterator();

         while(var6.hasNext()) {
            WorldMapPoints var7 = (WorldMapPoints)var6.next();
            WorldMapPoints var8 = new WorldMapPoints();

            for(int var9 = 0; var9 < var7.numPoints(); ++var9) {
               int var10 = var2.m_cell.m_x * 300 + var7.getX(var9);
               int var11 = var2.m_cell.m_y * 300 + var7.getY(var9);
               int var12 = var10 - var1.m_cell.m_x * 256;
               int var13 = var11 - var1.m_cell.m_y * 256;
               var8.add(var12);
               var8.add(var13);
            }

            var5.m_points.add(var8);
         }

         var5.calculateBounds();
         var1.m_geometries.add(var5);
      }

      var1.m_properties = new WorldMapProperties();
      var1.m_properties.putAll(var2.m_properties);
   }
}
