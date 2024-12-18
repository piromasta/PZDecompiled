package zombie.iso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.core.utils.BooleanGrid;
import zombie.iso.enums.ChunkGenerationStatus;

public final class MapFiles {
   public final String mapDirectoryName;
   public final String mapDirectoryZFSPath;
   public final String mapDirectoryAbsolutePath;
   public final int priority;
   public int minX = 2147483647;
   public int minY = 2147483647;
   public int maxX = -2147483648;
   public int maxY = -2147483648;
   public final HashMap<String, LotHeader> InfoHeaders = new HashMap();
   public final ArrayList<String> InfoHeaderNames = new ArrayList();
   public final HashMap<String, String> InfoFileNames = new HashMap();
   public final HashMap<String, ChunkGenerationStatus> InfoFileModded = new HashMap();
   public BooleanGrid bgHasCell;
   int minCell300X;
   int minCell300Y;
   int maxCell300X;
   int maxCell300Y;
   public BooleanGrid bgHasCell300;

   public MapFiles(String var1, String var2, String var3, int var4) {
      this.mapDirectoryName = var1;
      this.mapDirectoryZFSPath = var2;
      this.mapDirectoryAbsolutePath = var3;
      this.priority = var4;
   }

   public int getWidthInCells() {
      return this.maxX - this.minX + 1;
   }

   public int getHeightInCells() {
      return this.maxY - this.minY + 1;
   }

   public void postLoad() {
      if (this.minX > this.maxX) {
         this.minX = this.maxX = 0;
         this.minY = this.maxY = 0;
      }

      this.bgHasCell = new BooleanGrid(this.getWidthInCells(), this.getHeightInCells());
      int var1 = 0;

      int var2;
      int var3;
      int var4;
      for(var2 = this.getHeightInCells(); var1 < var2; ++var1) {
         var3 = 0;

         for(var4 = this.getWidthInCells(); var3 < var4; ++var3) {
            this.bgHasCell.setValue(var3, var1, this.InfoHeaders.containsKey(String.format("%d_%d.lotheader", this.minX + var3, this.minY + var1)));
         }
      }

      this.minCell300X = (int)Math.floor((double)((float)this.minX * 256.0F / 300.0F));
      this.minCell300Y = (int)Math.floor((double)((float)this.minY * 256.0F / 300.0F));
      this.maxCell300X = (int)Math.floor((double)((float)(this.maxX + 1) * 256.0F / 300.0F));
      this.maxCell300Y = (int)Math.floor((double)((float)(this.maxY + 1) * 256.0F / 300.0F));
      this.bgHasCell300 = new BooleanGrid(this.maxCell300X - this.minCell300X + 1, this.maxCell300Y - this.minCell300Y + 1);

      for(var1 = this.minCell300Y; var1 <= this.maxCell300Y; ++var1) {
         for(var2 = this.minCell300X; var2 <= this.maxCell300X; ++var2) {
            var3 = (int)Math.floor((double)((float)var2 * 300.0F / 256.0F));
            var4 = (int)Math.floor((double)((float)var1 * 300.0F / 256.0F));
            if (this.hasCell(var3, var4) && this.hasCell(var3 + 1, var4 + 1)) {
               this.bgHasCell300.setValue(var2 - this.minCell300X, var1 - this.minCell300Y, true);
            }
         }
      }

   }

   public boolean isValidCellPos(int var1, int var2) {
      return var1 >= this.minX && var2 >= this.minY && var1 <= this.maxX && var2 <= this.maxY;
   }

   public LotHeader getLotHeader(int var1, int var2) {
      return this.hasCell(var1, var2) ? (LotHeader)this.InfoHeaders.get(String.format("%d_%d.lotheader", var1, var2)) : null;
   }

   public boolean hasCell(int var1, int var2) {
      return this.isValidCellPos(var1, var2) && this.bgHasCell.getValue(var1 - this.minX, var2 - this.minY);
   }

   public void Dispose() {
      Iterator var1 = this.InfoHeaders.values().iterator();

      while(var1.hasNext()) {
         LotHeader var2 = (LotHeader)var1.next();
         var2.Dispose();
      }

      this.InfoHeaders.clear();
      this.InfoHeaderNames.clear();
      this.InfoFileNames.clear();
      this.InfoFileModded.clear();
   }
}
