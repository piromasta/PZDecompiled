package zombie.iso.worldgen.maps;

import com.sixlegs.png.PngImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import se.krka.kahlua.vm.KahluaTable;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.debug.DebugLog;
import zombie.iso.IsoCell;
import zombie.iso.IsoWorld;
import zombie.iso.worldgen.WGReader;
import zombie.iso.worldgen.utils.CacheMap;
import zombie.iso.worldgen.utils.CellCoord;

public class BiomeMap {
   private Map<Integer, BiomeMapEntry> zoneMap;
   private Map<CellCoord, Raster> cache = new CacheMap(16);
   private List<String> foragingZones;

   public BiomeMap() {
      WGReader var1 = new WGReader();
      this.zoneMap = var1.loadBiomeMapConfig((KahluaTable)LuaManager.env.rawget("biome_map_config"));
      this.foragingZones = new ArrayList();
      Iterator var2 = this.zoneMap.entrySet().iterator();

      while(var2.hasNext()) {
         Map.Entry var3 = (Map.Entry)var2.next();
         this.foragingZones.add(((BiomeMapEntry)var3.getValue()).zone());
      }

   }

   private Raster getRaster(CellCoord var1) {
      if (this.cache.containsKey(var1)) {
         return (Raster)this.cache.get(var1);
      } else {
         String[] var2 = IsoWorld.instance.getMap().split(";");
         String var3 = null;
         String[] var4 = var2;
         int var5 = var2.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            String var7 = var4[var6];
            var3 = ZomboidFileSystem.instance.getString(String.format("media/maps/%s/maps/biomemap_%d_%d.png", var7, var1.x(), var1.y()));
            File var8 = new File(var3);
            if (var8.exists()) {
               break;
            }
         }

         Raster var10;
         if (var3 != null) {
            try {
               var10 = (new PngImage()).read(new File(var3)).getData();
            } catch (IOException var9) {
               DebugLog.log(String.format("Loading error of BiomeMap at (%d, %d)", var1.x(), var1.y()));
               var10 = null;
            }
         } else {
            DebugLog.log(String.format("BiomeMap could not find any biome image file at (%d, %d)", var1.x(), var1.y()));
            var10 = null;
         }

         this.cache.put(var1, var10);
         return var10;
      }
   }

   public int[] getZones(int var1, int var2) {
      int var3 = var1 / IsoCell.CellSizeInChunks;
      int var4 = var2 / IsoCell.CellSizeInChunks;
      int var5 = var3 * IsoCell.CellSizeInChunks;
      int var6 = var4 * IsoCell.CellSizeInChunks;
      Raster var7 = this.getRaster(new CellCoord(var3, var4));
      return var7 == null ? null : var7.getSamples((var1 - var5) * 8, (var2 - var6) * 8, 8, 8, 0, (int[])null);
   }

   public String getZoneName(int var1) {
      BiomeMapEntry var2 = (BiomeMapEntry)this.zoneMap.getOrDefault(var1, (Object)null);
      return var2 == null ? null : var2.zone();
   }

   public String getBiomeName(int var1) {
      BiomeMapEntry var2 = (BiomeMapEntry)this.zoneMap.getOrDefault(var1, (Object)null);
      return var2 == null ? null : var2.biome();
   }

   public String getOreName(int var1) {
      BiomeMapEntry var2 = (BiomeMapEntry)this.zoneMap.getOrDefault(var1, (Object)null);
      return var2 == null ? null : var2.ore();
   }

   public List<String> getForagingZones() {
      return this.foragingZones;
   }

   public BiomeMapEntry getEntry(int var1) {
      return (BiomeMapEntry)this.zoneMap.getOrDefault(var1, (Object)null);
   }

   public void Dispose() {
      this.cache = null;
      this.zoneMap = null;
      this.foragingZones = null;
   }
}
