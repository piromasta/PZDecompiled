package zombie.pot;

import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import zombie.iso.BuildingDef;
import zombie.iso.BuildingID;
import zombie.iso.IsoLot;
import zombie.iso.LotHeader;
import zombie.iso.MetaObject;
import zombie.iso.RoomDef;
import zombie.iso.RoomID;
import zombie.iso.SliceY;
import zombie.util.BufferedRandomAccessFile;
import zombie.util.SharedStrings;

public final class POTLotHeader {
   private static final SharedStrings sharedStrings = new SharedStrings();
   private static final ArrayList<RoomDef> tempRooms = new ArrayList();
   public final boolean pot;
   public final int CHUNK_DIM;
   public final int CHUNKS_PER_CELL;
   public final int CELL_DIM;
   public final int x;
   public final int y;
   public int width = 0;
   public int height = 0;
   public int minLevel = -32;
   public int maxLevel = 31;
   public int minLevelNotEmpty = 1000;
   public int maxLevelNotEmpty = -1000;
   public int version = 0;
   public final HashMap<Long, RoomDef> Rooms = new HashMap();
   public final ArrayList<RoomDef> RoomList = new ArrayList();
   public final ArrayList<BuildingDef> Buildings = new ArrayList();
   public final ArrayList<String> tilesUsed = new ArrayList();
   public final TObjectIntHashMap<String> indexToTile = new TObjectIntHashMap(10, 0.5F, -1);
   public final byte[] zombieDensity;

   POTLotHeader(int var1, int var2, boolean var3) {
      this.CHUNK_DIM = var3 ? 8 : 10;
      this.CHUNKS_PER_CELL = var3 ? 32 : 30;
      this.CELL_DIM = var3 ? 256 : 300;
      this.pot = var3;
      this.x = var1;
      this.y = var2;
      this.width = this.CHUNK_DIM;
      this.height = this.CHUNK_DIM;
      this.zombieDensity = new byte[this.CHUNKS_PER_CELL * this.CHUNKS_PER_CELL];
   }

   void clear() {
      Iterator var1 = this.Buildings.iterator();

      while(var1.hasNext()) {
         BuildingDef var2 = (BuildingDef)var1.next();
         var2.Dispose();
      }

      this.Buildings.clear();
      this.Rooms.clear();
      this.RoomList.clear();
      this.tilesUsed.clear();
      this.indexToTile.clear();
   }

   void load(File var1) {
      try {
         BufferedRandomAccessFile var2 = new BufferedRandomAccessFile(var1, "r", 4096);

         try {
            label114: {
               byte[] var3 = new byte[4];
               var2.read(var3, 0, 4);
               boolean var4 = Arrays.equals(var3, LotHeader.LOTHEADER_MAGIC);
               if (!var4) {
                  var2.seek(0L);
               }

               this.version = IsoLot.readInt(var2);
               if (this.version >= 0 && this.version <= 1) {
                  int var5 = IsoLot.readInt(var2);

                  for(int var6 = 0; var6 < var5; ++var6) {
                     String var7 = IsoLot.readString(var2);
                     var7 = sharedStrings.get(var7.trim());
                     this.tilesUsed.add(var7);
                     this.indexToTile.put(var7, var6);
                  }

                  if (this.version == 0) {
                     var2.read();
                  } else {
                     boolean var10000 = false;
                  }

                  this.width = IsoLot.readInt(var2);
                  this.height = IsoLot.readInt(var2);
                  if (this.version == 0) {
                     this.minLevel = 0;
                     this.maxLevel = IsoLot.readInt(var2);
                  } else {
                     this.minLevel = IsoLot.readInt(var2);
                     this.maxLevel = IsoLot.readInt(var2);
                  }

                  this.minLevelNotEmpty = this.minLevel;
                  this.maxLevelNotEmpty = this.maxLevel;
                  int var23 = IsoLot.readInt(var2);

                  int var8;
                  int var13;
                  for(var8 = 0; var8 < var23; ++var8) {
                     String var9 = IsoLot.readString(var2);
                     long var10 = RoomID.makeID(this.x, this.y, var8);
                     RoomDef var12 = new RoomDef(var10, sharedStrings.get(var9));
                     var12.level = IsoLot.readInt(var2);
                     var13 = IsoLot.readInt(var2);

                     int var14;
                     int var15;
                     int var16;
                     int var17;
                     int var18;
                     for(var14 = 0; var14 < var13; ++var14) {
                        var15 = IsoLot.readInt(var2);
                        var16 = IsoLot.readInt(var2);
                        var17 = IsoLot.readInt(var2);
                        var18 = IsoLot.readInt(var2);
                        RoomDef.RoomRect var19 = new RoomDef.RoomRect(var15 + this.x * this.CELL_DIM, var16 + this.y * this.CELL_DIM, var17, var18);
                        var12.rects.add(var19);
                     }

                     var12.CalculateBounds();
                     this.Rooms.put(var12.ID, var12);
                     this.RoomList.add(var12);
                     var14 = IsoLot.readInt(var2);

                     for(var15 = 0; var15 < var14; ++var15) {
                        var16 = IsoLot.readInt(var2);
                        var17 = IsoLot.readInt(var2);
                        var18 = IsoLot.readInt(var2);
                        var12.objects.add(new MetaObject(var16, var17 + this.x * this.CELL_DIM - var12.x, var18 + this.y * this.CELL_DIM - var12.y, var12));
                     }
                  }

                  var8 = IsoLot.readInt(var2);

                  int var24;
                  for(var24 = 0; var24 < var8; ++var24) {
                     BuildingDef var25 = new BuildingDef();
                     var25.ID = BuildingID.makeID(this.x, this.y, var24);
                     int var11 = IsoLot.readInt(var2);

                     for(int var27 = 0; var27 < var11; ++var27) {
                        var13 = IsoLot.readInt(var2);
                        long var28 = RoomID.makeID(this.x, this.y, var13);
                        RoomDef var29 = (RoomDef)this.Rooms.get(var28);
                        var29.building = var25;
                        var25.rooms.add(var29);
                     }

                     var25.CalculateBounds(tempRooms);
                     this.Buildings.add(var25);
                  }

                  var24 = 0;

                  while(true) {
                     if (var24 >= this.CHUNKS_PER_CELL) {
                        break label114;
                     }

                     for(int var26 = 0; var26 < this.CHUNKS_PER_CELL; ++var26) {
                        this.zombieDensity[var24 + var26 * this.CHUNKS_PER_CELL] = (byte)var2.read();
                     }

                     ++var24;
                  }
               }

               throw new IOException("Unsupported version " + this.version);
            }
         } catch (Throwable var21) {
            try {
               var2.close();
            } catch (Throwable var20) {
               var21.addSuppressed(var20);
            }

            throw var21;
         }

         var2.close();
      } catch (Exception var22) {
         var22.printStackTrace();
      }

   }

   void save(String var1) throws IOException {
      ByteBuffer var2 = SliceY.SliceBuffer;
      var2.order(ByteOrder.LITTLE_ENDIAN);
      var2.clear();
      var2.put(LotHeader.LOTHEADER_MAGIC);
      var2.putInt(1);
      var2.putInt(this.tilesUsed.size());

      int var3;
      for(var3 = 0; var3 < this.tilesUsed.size(); ++var3) {
         this.writeString(var2, (String)this.tilesUsed.get(var3));
      }

      var2.putInt(this.width);
      var2.putInt(this.height);
      var2.putInt(this.minLevelNotEmpty);
      var2.putInt(this.maxLevelNotEmpty);
      var2.putInt(this.RoomList.size());
      Iterator var9 = this.RoomList.iterator();

      Iterator var5;
      while(var9.hasNext()) {
         RoomDef var4 = (RoomDef)var9.next();
         this.writeString(var2, var4.name);
         var2.putInt(var4.level);
         var2.putInt(var4.rects.size());
         var5 = var4.rects.iterator();

         while(var5.hasNext()) {
            RoomDef.RoomRect var6 = (RoomDef.RoomRect)var5.next();
            var2.putInt(var6.x - this.getMinSquareX());
            var2.putInt(var6.y - this.getMinSquareY());
            var2.putInt(var6.w);
            var2.putInt(var6.h);
         }

         var2.putInt(var4.objects.size());
         var5 = var4.objects.iterator();

         while(var5.hasNext()) {
            MetaObject var13 = (MetaObject)var5.next();
            var2.putInt(var13.getType());
            var2.putInt(var13.getX());
            var2.putInt(var13.getY());
         }
      }

      var2.putInt(this.Buildings.size());
      var9 = this.Buildings.iterator();

      while(var9.hasNext()) {
         BuildingDef var10 = (BuildingDef)var9.next();
         var2.putInt(var10.rooms.size());
         var5 = var10.rooms.iterator();

         while(var5.hasNext()) {
            RoomDef var14 = (RoomDef)var5.next();

            assert var14.ID == (long)this.RoomList.indexOf(var14);

            var2.putInt(RoomID.getIndex(var14.ID));
         }
      }

      for(var3 = 0; var3 < this.CHUNKS_PER_CELL; ++var3) {
         for(int var11 = 0; var11 < this.CHUNKS_PER_CELL; ++var11) {
            var2.put(this.zombieDensity[var3 + var11 * this.CHUNKS_PER_CELL]);
         }
      }

      FileOutputStream var12 = new FileOutputStream(var1);

      try {
         var12.write(var2.array(), 0, var2.position());
      } catch (Throwable var8) {
         try {
            var12.close();
         } catch (Throwable var7) {
            var8.addSuppressed(var7);
         }

         throw var8;
      }

      var12.close();
   }

   void writeString(ByteBuffer var1, String var2) {
      byte[] var3 = var2.getBytes(StandardCharsets.UTF_8);
      var1.put(var3);
      var1.put((byte)10);
   }

   int getMinSquareX() {
      return this.x * this.CELL_DIM;
   }

   int getMinSquareY() {
      return this.y * this.CELL_DIM;
   }

   int getMaxSquareX() {
      return (this.x + 1) * this.CELL_DIM - 1;
   }

   int getMaxSquareY() {
      return (this.y + 1) * this.CELL_DIM - 1;
   }

   boolean containsSquare(int var1, int var2) {
      return var1 >= this.getMinSquareX() && var1 <= this.getMaxSquareX() && var2 >= this.getMinSquareY() && var2 <= this.getMaxSquareY();
   }

   void addBuilding(BuildingDef var1) {
      BuildingDef var2 = new BuildingDef();
      var2.ID = (long)this.Buildings.size();
      Iterator var3 = var1.rooms.iterator();

      while(var3.hasNext()) {
         RoomDef var4 = (RoomDef)var3.next();
         RoomDef var5 = new RoomDef((long)this.RoomList.size(), var4.name);
         var5.ID = (long)this.RoomList.size();
         var5.level = var4.level;
         var5.building = var2;
         var5.rects.addAll(var4.rects);
         var5.objects.addAll(var4.objects);
         var5.CalculateBounds();
         var2.rooms.add(var5);
         this.Rooms.put(var5.ID, var5);
         this.RoomList.add(var5);
      }

      var2.CalculateBounds(tempRooms);
      this.Buildings.add(var2);
   }

   byte getZombieDensityForSquare(int var1, int var2) {
      if (!this.containsSquare(var1, var2)) {
         return 0;
      } else {
         int var3 = var1 - this.getMinSquareX();
         int var4 = var2 - this.getMinSquareY();
         return this.zombieDensity[var3 / this.CHUNK_DIM + var4 / this.CHUNK_DIM * this.CHUNKS_PER_CELL];
      }
   }

   void setZombieDensity(byte[] var1) {
      for(int var2 = 0; var2 < this.CHUNKS_PER_CELL; ++var2) {
         for(int var3 = 0; var3 < this.CHUNKS_PER_CELL; ++var3) {
            int var4 = 0;

            for(int var5 = 0; var5 < this.CHUNK_DIM * this.CHUNK_DIM; ++var5) {
               var4 += var1[var3 * this.CHUNK_DIM + var2 * this.CHUNK_DIM * this.CELL_DIM + var5 % this.CHUNK_DIM + var5 / this.CHUNK_DIM * this.CELL_DIM];
            }

            this.zombieDensity[var3 + var2 * this.CHUNKS_PER_CELL] = (byte)(var4 / (this.CHUNK_DIM * this.CHUNK_DIM));
         }
      }

   }

   int getTileIndex(String var1) {
      var1 = sharedStrings.get(var1);
      int var2 = this.indexToTile.get(var1);
      if (var2 == this.indexToTile.getNoEntryValue()) {
         var2 = this.tilesUsed.size();
         this.indexToTile.put(var1, this.tilesUsed.size());
         this.tilesUsed.add(var1);
      }

      return var2;
   }
}
