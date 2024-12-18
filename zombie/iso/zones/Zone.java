package zombie.iso.zones;

import gnu.trove.list.array.TIntArrayList;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joml.Vector2f;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaChunk;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.pathfind.LiangBarsky;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;
import zombie.util.SharedStrings;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;
import zombie.vehicles.ClipperOffset;

public class Zone {
   static final LiangBarsky LIANG_BARSKY = new LiangBarsky();
   static final Vector2 L_lineSegmentIntersects = new Vector2();
   private static final List<String> s_PreferredZoneTypes = List.of("DeepForest", "Farm", "FarmLand", "Forest", "Vegitation", "Nav", "TownZone", "TrailerPark");
   public static Clipper s_clipper = null;
   public final HashMap<String, Integer> spawnedZombies;
   public final TIntArrayList points;
   public UUID id;
   public int hourLastSeen;
   public int lastActionTimestamp;
   public boolean haveConstruction;
   public String zombiesTypeToSpawn;
   public Boolean spawnSpecialZombies;
   public String name;
   public String type;
   public int x;
   public int y;
   public int z;
   public int w;
   public int h;
   public ZoneGeometryType geometryType;
   public int polylineWidth;
   public float[] polylineOutlinePoints;
   public float[] triangles;
   public float[] triangleAreas;
   public float totalArea;
   public int pickedXForZoneStory;
   public int pickedYForZoneStory;
   public RandomizedZoneStoryBase pickedRZStory;
   public boolean isPreferredZoneForSquare;
   private boolean bTriangulateFailed;
   private String originalName;

   public Zone() {
      this.spawnedZombies = new HashMap();
      this.points = new TIntArrayList();
      this.hourLastSeen = 0;
      this.lastActionTimestamp = 0;
      this.haveConstruction = false;
      this.zombiesTypeToSpawn = null;
      this.spawnSpecialZombies = null;
      this.geometryType = ZoneGeometryType.INVALID;
      this.polylineWidth = 0;
      this.totalArea = 0.0F;
      this.isPreferredZoneForSquare = false;
      this.bTriangulateFailed = false;
   }

   public Zone(String var1, String var2, int var3, int var4, int var5, int var6, int var7) {
      this.spawnedZombies = new HashMap();
      this.points = new TIntArrayList();
      this.hourLastSeen = 0;
      this.lastActionTimestamp = 0;
      this.haveConstruction = false;
      this.zombiesTypeToSpawn = null;
      this.spawnSpecialZombies = null;
      this.geometryType = ZoneGeometryType.INVALID;
      this.polylineWidth = 0;
      this.totalArea = 0.0F;
      this.isPreferredZoneForSquare = false;
      this.bTriangulateFailed = false;
      this.id = UUID.randomUUID();
      this.originalName = var1;
      this.name = var1;
      this.type = var2;
      this.x = var3;
      this.y = var4;
      this.z = var5;
      this.w = var6;
      this.h = var7;
   }

   public Zone(String var1, String var2, int var3, int var4, int var5, int var6, int var7, ZoneGeometryType var8, TIntArrayList var9, int var10) {
      this(var1, var2, var3, var4, var5, var6, var7);
      this.geometryType = var8;
      if (var9 != null) {
         this.points.addAll(var9);
         this.polylineWidth = var10;
      }

      this.isPreferredZoneForSquare = isPreferredZoneForSquare(this.getType());
   }

   public Zone load(ByteBuffer var1, int var2, Map<Integer, String> var3, SharedStrings var4) {
      this.name = var4.get((String)var3.get(Integer.valueOf(var1.getShort())));
      this.type = var4.get((String)var3.get(Integer.valueOf(var1.getShort())));
      this.loadData(var1, var2);
      this.setOriginalName((String)var3.get(Integer.valueOf(var1.getShort())));
      if (var2 >= 215) {
         this.id = GameWindow.ReadUUID(var1);
      } else {
         var1.getDouble();
         this.id = UUID.randomUUID();
      }

      return this;
   }

   public Zone load(ByteBuffer var1, int var2) {
      this.name = GameWindow.ReadStringUTF(var1);
      this.type = GameWindow.ReadStringUTF(var1);
      this.loadData(var1, var2);
      this.setOriginalName(GameWindow.ReadStringUTF(var1));
      if (var2 >= 215) {
         this.id = GameWindow.ReadUUID(var1);
      } else {
         var1.getDouble();
         this.id = UUID.randomUUID();
      }

      return this;
   }

   private void loadData(ByteBuffer var1, int var2) {
      this.x = var1.getInt();
      this.y = var1.getInt();
      this.z = var1.get();
      this.w = var1.getInt();
      this.h = var1.getInt();
      TIntArrayList var3 = new TIntArrayList();
      ZoneGeometryType[] var4 = ZoneGeometryType.values();
      int var5 = 0;
      byte var6 = var1.get();
      if (var6 < 0 || var6 >= var4.length) {
         var6 = 0;
      }

      this.geometryType = var4[var6];
      if (this.geometryType != ZoneGeometryType.INVALID) {
         if (this.geometryType == ZoneGeometryType.Polyline) {
            var5 = PZMath.clamp(var1.get(), 0, 255);
         }

         short var7 = var1.getShort();

         for(int var8 = 0; var8 < var7; ++var8) {
            var3.add(var1.getInt());
         }

         this.points.addAll(var3);
         this.polylineWidth = var5;
      }

      this.isPreferredZoneForSquare = isPreferredZoneForSquare(this.getType());
      this.hourLastSeen = var1.getInt();
      this.haveConstruction = var1.get() == 1;
      this.lastActionTimestamp = var1.getInt();
   }

   public static boolean isPreferredZoneForSquare(String var0) {
      return s_PreferredZoneTypes.contains(var0);
   }

   public void save(ByteBuffer var1, Map<String, Integer> var2) {
      var1.putShort(((Integer)var2.get(this.getName())).shortValue());
      var1.putShort(((Integer)var2.get(this.getType())).shortValue());
      this.saveData(var1);
      var1.putShort(((Integer)var2.get(this.getOriginalName())).shortValue());
      GameWindow.WriteUUID(var1, this.id);
   }

   public void save(ByteBuffer var1) {
      GameWindow.WriteStringUTF(var1, this.getName());
      GameWindow.WriteStringUTF(var1, this.getType());
      this.saveData(var1);
      GameWindow.WriteStringUTF(var1, this.getOriginalName());
      GameWindow.WriteUUID(var1, this.id);
   }

   private void saveData(ByteBuffer var1) {
      var1.putInt(this.x);
      var1.putInt(this.y);
      var1.put((byte)this.z);
      var1.putInt(this.w);
      var1.putInt(this.h);
      var1.put((byte)this.geometryType.ordinal());
      if (!this.isRectangle()) {
         if (this.isPolyline()) {
            var1.put((byte)this.polylineWidth);
         }

         var1.putShort((short)this.points.size());

         for(int var2 = 0; var2 < this.points.size(); ++var2) {
            var1.putInt(this.points.get(var2));
         }
      }

      var1.putInt(this.hourLastSeen);
      var1.put((byte)(this.haveConstruction ? 1 : 0));
      var1.putInt(this.lastActionTimestamp);
   }

   public boolean isFullyStreamed() {
      IsoGridSquare var1 = IsoWorld.instance.getCell().getGridSquare(this.x, this.y, this.z);
      IsoGridSquare var2 = IsoWorld.instance.getCell().getGridSquare(this.x + this.w, this.y + this.h, this.z);
      return var1 != null && var2 != null;
   }

   public void setW(int var1) {
      this.w = var1;
   }

   public void setH(int var1) {
      this.h = var1;
   }

   public boolean isPoint() {
      return this.geometryType == ZoneGeometryType.Point;
   }

   public boolean isPolygon() {
      return this.geometryType == ZoneGeometryType.Polygon;
   }

   public boolean isPolyline() {
      return this.geometryType == ZoneGeometryType.Polyline;
   }

   public boolean isRectangle() {
      return this.geometryType == ZoneGeometryType.INVALID;
   }

   public void setPickedXForZoneStory(int var1) {
      this.pickedXForZoneStory = var1;
   }

   public void setPickedYForZoneStory(int var1) {
      this.pickedYForZoneStory = var1;
   }

   public float getHoursSinceLastSeen() {
      return (float)GameTime.instance.getWorldAgeHours() - (float)this.hourLastSeen;
   }

   public void setHourSeenToCurrent() {
      if (!"Ranch".equals(this.type)) {
         this.hourLastSeen = (int)GameTime.instance.getWorldAgeHours();
      }

   }

   public void setHaveConstruction(boolean var1) {
      this.haveConstruction = var1;
      if (GameClient.bClient) {
         ByteBufferWriter var2 = GameClient.connection.startPacket();
         PacketTypes.PacketType.ConstructedZone.doPacket(var2);
         var2.putInt(this.x);
         var2.putInt(this.y);
         var2.putInt(this.z);
         PacketTypes.PacketType.ConstructedZone.send(GameClient.connection);
      }

   }

   public boolean haveCons() {
      return this.haveConstruction;
   }

   public int getZombieDensity() {
      IsoMetaChunk var1 = IsoWorld.instance.MetaGrid.getChunkDataFromTile(this.x, this.y);
      return var1 != null ? var1.getUnadjustedZombieIntensity() : 0;
   }

   public boolean contains(int var1, int var2, int var3) {
      if (var3 != this.z) {
         return false;
      } else if (var1 >= this.x && var1 < this.x + this.w) {
         if (var2 >= this.y && var2 < this.y + this.h) {
            if (this.isPoint()) {
               return false;
            } else if (this.isPolyline()) {
               if (this.polylineWidth > 0) {
                  this.checkPolylineOutline();
                  return this.isPointInPolyline_WindingNumber((float)var1 + 0.5F, (float)var2 + 0.5F, 0) == Zone.PolygonHit.Inside;
               } else {
                  return false;
               }
            } else if (this.isPolygon()) {
               return this.isPointInPolygon_WindingNumber((float)var1 + 0.5F, (float)var2 + 0.5F, 0) == Zone.PolygonHit.Inside;
            } else {
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean intersects(int var1, int var2, int var3, int var4, int var5) {
      if (this.z != var3) {
         return false;
      } else if (var1 + var4 > this.x && var1 < this.x + this.w) {
         if (var2 + var5 > this.y && var2 < this.y + this.h) {
            if (this.isPolygon()) {
               return this.polygonRectIntersect(var1, var2, var4, var5);
            } else if (this.isPolyline()) {
               if (this.polylineWidth > 0) {
                  this.checkPolylineOutline();
                  return this.polylineOutlineRectIntersect(var1, var2, var4, var5);
               } else {
                  for(int var6 = 0; var6 < this.points.size() - 2; var6 += 2) {
                     int var7 = this.points.getQuick(var6);
                     int var8 = this.points.getQuick(var6 + 1);
                     int var9 = this.points.getQuick(var6 + 2);
                     int var10 = this.points.getQuick(var6 + 3);
                     if (LIANG_BARSKY.lineRectIntersect((float)var7, (float)var8, (float)(var9 - var7), (float)(var10 - var8), (float)var1, (float)var2, (float)(var1 + var4), (float)(var2 + var5))) {
                        return true;
                     }
                  }

                  return false;
               }
            } else {
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean difference(int var1, int var2, int var3, int var4, int var5, ArrayList<Zone> var6) {
      var6.clear();
      if (!this.intersects(var1, var2, var3, var4, var5)) {
         return false;
      } else if (this.isRectangle()) {
         int var14;
         int var15;
         if (this.x < var1) {
            var14 = Math.max(var2, this.y);
            var15 = Math.min(var2 + var5, this.y + this.h);
            var6.add(new Zone(this.name, this.type, this.x, var14, var3, var1 - this.x, var15 - var14));
         }

         if (var1 + var4 < this.x + this.w) {
            var14 = Math.max(var2, this.y);
            var15 = Math.min(var2 + var5, this.y + this.h);
            var6.add(new Zone(this.name, this.type, var1 + var4, var14, var3, this.x + this.w - (var1 + var4), var15 - var14));
         }

         if (this.y < var2) {
            var6.add(new Zone(this.name, this.type, this.x, this.y, var3, this.w, var2 - this.y));
         }

         if (var2 + var5 < this.y + this.h) {
            var6.add(new Zone(this.name, this.type, this.x, var2 + var5, var3, this.w, this.y + this.h - (var2 + var5)));
         }

         return true;
      } else {
         if (this.isPolygon()) {
            if (s_clipper == null) {
               s_clipper = new Clipper();
               IsoMetaGrid.s_clipperBuffer = ByteBuffer.allocateDirect(3072);
            }

            Clipper var7 = s_clipper;
            ByteBuffer var8 = IsoMetaGrid.s_clipperBuffer;
            var8.clear();

            int var9;
            for(var9 = 0; var9 < this.points.size(); var9 += 2) {
               var8.putFloat((float)this.points.getQuick(var9));
               var8.putFloat((float)this.points.getQuick(var9 + 1));
            }

            var7.clear();
            var7.addPath(this.points.size() / 2, var8, false);
            var7.clipAABB((float)var1, (float)var2, (float)(var1 + var4), (float)(var2 + var5));
            var9 = var7.generatePolygons();

            for(int var10 = 0; var10 < var9; ++var10) {
               var8.clear();
               var7.getPolygon(var10, var8);
               short var11 = var8.getShort();
               if (var11 < 3) {
                  var8.position(var8.position() + var11 * 4 * 2);
               } else {
                  Zone var12 = new Zone(this.name, this.type, this.x, this.y, this.z, this.w, this.h);
                  var12.geometryType = ZoneGeometryType.Polygon;

                  for(int var13 = 0; var13 < var11; ++var13) {
                     var12.points.add((int)var8.getFloat());
                     var12.points.add((int)var8.getFloat());
                  }

                  var6.add(var12);
               }
            }
         }

         if (this.isPolyline()) {
         }

         return true;
      }
   }

   private int pickRandomTriangle() {
      float[] var1 = this.isPolygon() ? this.getPolygonTriangles() : (this.isPolyline() ? this.getPolylineOutlineTriangles() : null);
      if (var1 == null) {
         return -1;
      } else {
         int var2 = var1.length / 6;
         float var3 = Rand.Next(0.0F, this.totalArea);
         float var4 = 0.0F;

         for(int var5 = 0; var5 < this.triangleAreas.length; ++var5) {
            var4 += this.triangleAreas[var5];
            if (var4 >= var3) {
               return var5;
            }
         }

         return Rand.Next(var2);
      }
   }

   private Vector2 pickRandomPointInTriangle(int var1, Vector2 var2) {
      float var3 = this.triangles[var1 * 3 * 2];
      float var4 = this.triangles[var1 * 3 * 2 + 1];
      float var5 = this.triangles[var1 * 3 * 2 + 2];
      float var6 = this.triangles[var1 * 3 * 2 + 3];
      float var7 = this.triangles[var1 * 3 * 2 + 4];
      float var8 = this.triangles[var1 * 3 * 2 + 5];
      float var9 = Rand.Next(0.0F, 1.0F);
      float var10 = Rand.Next(0.0F, 1.0F);
      boolean var13 = var9 + var10 <= 1.0F;
      float var11;
      float var12;
      if (var13) {
         var11 = var9 * (var5 - var3) + var10 * (var7 - var3);
         var12 = var9 * (var6 - var4) + var10 * (var8 - var4);
      } else {
         var11 = (1.0F - var9) * (var5 - var3) + (1.0F - var10) * (var7 - var3);
         var12 = (1.0F - var9) * (var6 - var4) + (1.0F - var10) * (var8 - var4);
      }

      var11 += var3;
      var12 += var4;
      return var2.set(var11, var12);
   }

   public IsoGameCharacter.Location pickRandomLocation(IsoGameCharacter.Location var1) {
      if (this.isPolygon() || this.isPolyline() && this.polylineWidth > 0) {
         int var2 = this.pickRandomTriangle();
         if (var2 == -1) {
            return null;
         } else {
            for(int var3 = 0; var3 < 20; ++var3) {
               Vector2 var4 = this.pickRandomPointInTriangle(var2, BaseVehicle.allocVector2());
               if (this.contains((int)var4.x, (int)var4.y, this.z)) {
                  var1.set((int)var4.x, (int)var4.y, this.z);
                  BaseVehicle.releaseVector2(var4);
                  return var1;
               }
            }

            return null;
         }
      } else {
         return !this.isPoint() && !this.isPolyline() ? var1.set(Rand.Next(this.x, this.x + this.w), Rand.Next(this.y, this.y + this.h), this.z) : null;
      }
   }

   public IsoGridSquare getRandomSquareInZone() {
      IsoGameCharacter.Location var1 = this.pickRandomLocation((IsoGameCharacter.Location)IsoMetaGrid.TL_Location.get());
      return var1 == null ? null : IsoWorld.instance.CurrentCell.getGridSquare(var1.x, var1.y, var1.z);
   }

   public IsoGridSquare getRandomFreeSquareInZone() {
      for(int var1 = 100; var1 > 0; --var1) {
         IsoGameCharacter.Location var2 = this.pickRandomLocation((IsoGameCharacter.Location)IsoMetaGrid.TL_Location.get());
         if (var2 == null) {
            return null;
         }

         IsoGridSquare var3 = IsoWorld.instance.CurrentCell.getGridSquare(var2.x, var2.y, var2.z);
         if (var3 != null && var3.isFree(true)) {
            return var3;
         }
      }

      return null;
   }

   public IsoGridSquare getRandomUnseenSquareInZone() {
      return null;
   }

   public void addSquare(IsoGridSquare var1) {
   }

   public ArrayList<IsoGridSquare> getSquares() {
      return null;
   }

   public void removeSquare(IsoGridSquare var1) {
   }

   public String getName() {
      return this.name;
   }

   public void setName(String var1) {
      this.name = var1;
   }

   public String getType() {
      return this.type;
   }

   public void setType(String var1) {
      this.type = var1;
   }

   public int getLastActionTimestamp() {
      return this.lastActionTimestamp;
   }

   public void setLastActionTimestamp(int var1) {
      this.lastActionTimestamp = var1;
   }

   public int getX() {
      return this.x;
   }

   public void setX(int var1) {
      this.x = var1;
   }

   public int getY() {
      return this.y;
   }

   public void setY(int var1) {
      this.y = var1;
   }

   public int getZ() {
      return this.z;
   }

   public int getHeight() {
      return this.h;
   }

   public int getWidth() {
      return this.w;
   }

   public float getTotalArea() {
      if (!this.isRectangle() && !this.isPoint() && (!this.isPolyline() || this.polylineWidth > 0)) {
         this.getPolygonTriangles();
         this.getPolylineOutlineTriangles();
         return this.totalArea;
      } else {
         return (float)(this.getWidth() * this.getHeight());
      }
   }

   public void sendToServer() {
      if (GameClient.bClient) {
         INetworkPacket.send(PacketTypes.PacketType.RegisterZone, this);
      }

   }

   public String getOriginalName() {
      return this.originalName;
   }

   public void setOriginalName(String var1) {
      this.originalName = var1;
   }

   public int getClippedSegmentOfPolyline(int var1, int var2, int var3, int var4, double[] var5) {
      if (!this.isPolyline()) {
         return -1;
      } else {
         float var6 = this.polylineWidth % 2 == 0 ? 0.0F : 0.5F;

         for(int var7 = 0; var7 < this.points.size() - 2; var7 += 2) {
            int var8 = this.points.getQuick(var7);
            int var9 = this.points.getQuick(var7 + 1);
            int var10 = this.points.getQuick(var7 + 2);
            int var11 = this.points.getQuick(var7 + 3);
            if (LIANG_BARSKY.lineRectIntersect((float)var8 + var6, (float)var9 + var6, (float)(var10 - var8), (float)(var11 - var9), (float)var1, (float)var2, (float)var3, (float)var4, var5)) {
               return var7 / 2;
            }
         }

         return -1;
      }
   }

   private void checkPolylineOutline() {
      if (this.polylineOutlinePoints == null) {
         if (this.isPolyline()) {
            if (this.polylineWidth > 0) {
               if (IsoMetaGrid.s_clipperOffset == null) {
                  IsoMetaGrid.s_clipperOffset = new ClipperOffset();
                  IsoMetaGrid.s_clipperBuffer = ByteBuffer.allocateDirect(3072);
               }

               ClipperOffset var1 = IsoMetaGrid.s_clipperOffset;
               ByteBuffer var2 = IsoMetaGrid.s_clipperBuffer;
               var1.clear();
               var2.clear();
               float var3 = this.polylineWidth % 2 == 0 ? 0.0F : 0.5F;

               int var4;
               int var6;
               for(var4 = 0; var4 < this.points.size(); var4 += 2) {
                  int var5 = this.points.get(var4);
                  var6 = this.points.get(var4 + 1);
                  var2.putFloat((float)var5 + var3);
                  var2.putFloat((float)var6 + var3);
               }

               var2.flip();
               var1.addPath(this.points.size() / 2, var2, ClipperOffset.JoinType.jtMiter.ordinal(), ClipperOffset.EndType.etOpenButt.ordinal());
               var1.execute((double)((float)this.polylineWidth / 2.0F));
               var4 = var1.getPolygonCount();
               if (var4 < 1) {
                  DebugLog.General.warn("Failed to generate polyline outline");
               } else {
                  var2.clear();
                  var1.getPolygon(0, var2);
                  short var7 = var2.getShort();
                  this.polylineOutlinePoints = new float[var7 * 2];

                  for(var6 = 0; var6 < var7; ++var6) {
                     this.polylineOutlinePoints[var6 * 2] = var2.getFloat();
                     this.polylineOutlinePoints[var6 * 2 + 1] = var2.getFloat();
                  }

               }
            }
         }
      }
   }

   float isLeft(float var1, float var2, float var3, float var4, float var5, float var6) {
      return (var3 - var1) * (var6 - var2) - (var5 - var1) * (var4 - var2);
   }

   PolygonHit isPointInPolygon_WindingNumber(float var1, float var2, int var3) {
      int var4 = 0;

      for(int var5 = 0; var5 < this.points.size(); var5 += 2) {
         int var6 = this.points.getQuick(var5);
         int var7 = this.points.getQuick(var5 + 1);
         int var8 = this.points.getQuick((var5 + 2) % this.points.size());
         int var9 = this.points.getQuick((var5 + 3) % this.points.size());
         if ((float)var7 <= var2) {
            if ((float)var9 > var2 && this.isLeft((float)var6, (float)var7, (float)var8, (float)var9, var1, var2) > 0.0F) {
               ++var4;
            }
         } else if ((float)var9 <= var2 && this.isLeft((float)var6, (float)var7, (float)var8, (float)var9, var1, var2) < 0.0F) {
            --var4;
         }
      }

      return var4 == 0 ? Zone.PolygonHit.Outside : Zone.PolygonHit.Inside;
   }

   PolygonHit isPointInPolyline_WindingNumber(float var1, float var2, int var3) {
      int var4 = 0;
      float[] var5 = this.polylineOutlinePoints;
      if (var5 == null) {
         return Zone.PolygonHit.Outside;
      } else {
         for(int var6 = 0; var6 < var5.length; var6 += 2) {
            float var7 = var5[var6];
            float var8 = var5[var6 + 1];
            float var9 = var5[(var6 + 2) % var5.length];
            float var10 = var5[(var6 + 3) % var5.length];
            if (var8 <= var2) {
               if (var10 > var2 && this.isLeft(var7, var8, var9, var10, var1, var2) > 0.0F) {
                  ++var4;
               }
            } else if (var10 <= var2 && this.isLeft(var7, var8, var9, var10, var1, var2) < 0.0F) {
               --var4;
            }
         }

         return var4 == 0 ? Zone.PolygonHit.Outside : Zone.PolygonHit.Inside;
      }
   }

   boolean polygonRectIntersect(int var1, int var2, int var3, int var4) {
      if (this.x >= var1 && this.x + this.w <= var1 + var3 && this.y >= var2 && this.y + this.h <= var2 + var4) {
         return true;
      } else {
         return this.lineSegmentIntersects((float)var1, (float)var2, (float)(var1 + var3), (float)var2) || this.lineSegmentIntersects((float)(var1 + var3), (float)var2, (float)(var1 + var3), (float)(var2 + var4)) || this.lineSegmentIntersects((float)(var1 + var3), (float)(var2 + var4), (float)var1, (float)(var2 + var4)) || this.lineSegmentIntersects((float)var1, (float)(var2 + var4), (float)var1, (float)var2);
      }
   }

   boolean lineSegmentIntersects(float var1, float var2, float var3, float var4) {
      L_lineSegmentIntersects.set(var3 - var1, var4 - var2);
      float var5 = L_lineSegmentIntersects.getLength();
      L_lineSegmentIntersects.normalize();
      float var6 = L_lineSegmentIntersects.x;
      float var7 = L_lineSegmentIntersects.y;

      for(int var8 = 0; var8 < this.points.size(); var8 += 2) {
         float var9 = (float)this.points.getQuick(var8);
         float var10 = (float)this.points.getQuick(var8 + 1);
         float var11 = (float)this.points.getQuick((var8 + 2) % this.points.size());
         float var12 = (float)this.points.getQuick((var8 + 3) % this.points.size());
         float var17 = var1 - var9;
         float var18 = var2 - var10;
         float var19 = var11 - var9;
         float var20 = var12 - var10;
         float var21 = 1.0F / (var20 * var6 - var19 * var7);
         float var22 = (var19 * var18 - var20 * var17) * var21;
         if (var22 >= 0.0F && var22 <= var5) {
            float var23 = (var18 * var6 - var17 * var7) * var21;
            if (var23 >= 0.0F && var23 <= 1.0F) {
               return true;
            }
         }
      }

      if (this.isPointInPolygon_WindingNumber((var1 + var3) / 2.0F, (var2 + var4) / 2.0F, 0) != Zone.PolygonHit.Outside) {
         return true;
      } else {
         return false;
      }
   }

   boolean polylineOutlineRectIntersect(int var1, int var2, int var3, int var4) {
      if (this.polylineOutlinePoints == null) {
         return false;
      } else if (this.x >= var1 && this.x + this.w <= var1 + var3 && this.y >= var2 && this.y + this.h <= var2 + var4) {
         return true;
      } else {
         return this.polylineOutlineSegmentIntersects((float)var1, (float)var2, (float)(var1 + var3), (float)var2) || this.polylineOutlineSegmentIntersects((float)(var1 + var3), (float)var2, (float)(var1 + var3), (float)(var2 + var4)) || this.polylineOutlineSegmentIntersects((float)(var1 + var3), (float)(var2 + var4), (float)var1, (float)(var2 + var4)) || this.polylineOutlineSegmentIntersects((float)var1, (float)(var2 + var4), (float)var1, (float)var2);
      }
   }

   boolean polylineOutlineSegmentIntersects(float var1, float var2, float var3, float var4) {
      L_lineSegmentIntersects.set(var3 - var1, var4 - var2);
      float var5 = L_lineSegmentIntersects.getLength();
      L_lineSegmentIntersects.normalize();
      float var6 = L_lineSegmentIntersects.x;
      float var7 = L_lineSegmentIntersects.y;
      float[] var8 = this.polylineOutlinePoints;

      for(int var9 = 0; var9 < var8.length; var9 += 2) {
         float var10 = var8[var9];
         float var11 = var8[var9 + 1];
         float var12 = var8[(var9 + 2) % var8.length];
         float var13 = var8[(var9 + 3) % var8.length];
         float var18 = var1 - var10;
         float var19 = var2 - var11;
         float var20 = var12 - var10;
         float var21 = var13 - var11;
         float var22 = 1.0F / (var21 * var6 - var20 * var7);
         float var23 = (var20 * var19 - var21 * var18) * var22;
         if (var23 >= 0.0F && var23 <= var5) {
            float var24 = (var19 * var6 - var18 * var7) * var22;
            if (var24 >= 0.0F && var24 <= 1.0F) {
               return true;
            }
         }
      }

      if (this.isPointInPolyline_WindingNumber((var1 + var3) / 2.0F, (var2 + var4) / 2.0F, 0) != Zone.PolygonHit.Outside) {
         return true;
      } else {
         return false;
      }
   }

   private boolean isClockwise() {
      if (!this.isPolygon()) {
         return false;
      } else {
         float var1 = 0.0F;

         for(int var2 = 0; var2 < this.points.size(); var2 += 2) {
            int var3 = this.points.getQuick(var2);
            int var4 = this.points.getQuick(var2 + 1);
            int var5 = this.points.getQuick((var2 + 2) % this.points.size());
            int var6 = this.points.getQuick((var2 + 3) % this.points.size());
            var1 += (float)((var5 - var3) * (var6 + var4));
         }

         return (double)var1 > 0.0;
      }
   }

   public float[] getPolygonTriangles() {
      if (this.triangles != null) {
         return this.triangles;
      } else if (this.bTriangulateFailed) {
         return null;
      } else if (!this.isPolygon()) {
         return null;
      } else {
         if (s_clipper == null) {
            s_clipper = new Clipper();
            IsoMetaGrid.s_clipperBuffer = ByteBuffer.allocateDirect(3072);
         }

         Clipper var1 = s_clipper;
         ByteBuffer var2 = IsoMetaGrid.s_clipperBuffer;
         var2.clear();
         int var3;
         if (this.isClockwise()) {
            for(var3 = this.points.size() - 1; var3 > 0; var3 -= 2) {
               var2.putFloat((float)this.points.getQuick(var3 - 1));
               var2.putFloat((float)this.points.getQuick(var3));
            }
         } else {
            for(var3 = 0; var3 < this.points.size(); var3 += 2) {
               var2.putFloat((float)this.points.getQuick(var3));
               var2.putFloat((float)this.points.getQuick(var3 + 1));
            }
         }

         var1.clear();
         var1.addPath(this.points.size() / 2, var2, false);
         var3 = var1.generatePolygons();
         if (var3 < 1) {
            this.bTriangulateFailed = true;
            return null;
         } else {
            var2.clear();
            int var4 = var1.triangulate(0, var2);
            this.triangles = new float[var4 * 2];

            for(int var5 = 0; var5 < var4; ++var5) {
               this.triangles[var5 * 2] = var2.getFloat();
               this.triangles[var5 * 2 + 1] = var2.getFloat();
            }

            this.initTriangleAreas();
            return this.triangles;
         }
      }
   }

   private float triangleArea(float var1, float var2, float var3, float var4, float var5, float var6) {
      float var7 = Vector2f.length(var3 - var1, var4 - var2);
      float var8 = Vector2f.length(var5 - var3, var6 - var4);
      float var9 = Vector2f.length(var1 - var5, var2 - var6);
      float var10 = (var7 + var8 + var9) / 2.0F;
      return (float)Math.sqrt((double)(var10 * (var10 - var7) * (var10 - var8) * (var10 - var9)));
   }

   private void initTriangleAreas() {
      int var1 = this.triangles.length / 6;
      this.triangleAreas = new float[var1];
      this.totalArea = 0.0F;

      for(int var2 = 0; var2 < this.triangles.length; var2 += 6) {
         float var3 = this.triangles[var2];
         float var4 = this.triangles[var2 + 1];
         float var5 = this.triangles[var2 + 2];
         float var6 = this.triangles[var2 + 3];
         float var7 = this.triangles[var2 + 4];
         float var8 = this.triangles[var2 + 5];
         float var9 = this.triangleArea(var3, var4, var5, var6, var7, var8);
         this.triangleAreas[var2 / 6] = var9;
         this.totalArea += var9;
      }

   }

   public float[] getPolylineOutlineTriangles() {
      if (this.triangles != null) {
         return this.triangles;
      } else if (this.isPolyline() && this.polylineWidth > 0) {
         if (this.bTriangulateFailed) {
            return null;
         } else {
            this.checkPolylineOutline();
            float[] var1 = this.polylineOutlinePoints;
            if (var1 == null) {
               this.bTriangulateFailed = true;
               return null;
            } else {
               if (s_clipper == null) {
                  s_clipper = new Clipper();
                  IsoMetaGrid.s_clipperBuffer = ByteBuffer.allocateDirect(3072);
               }

               Clipper var2 = s_clipper;
               ByteBuffer var3 = IsoMetaGrid.s_clipperBuffer;
               var3.clear();
               int var4;
               if (this.isClockwise()) {
                  for(var4 = var1.length - 1; var4 > 0; var4 -= 2) {
                     var3.putFloat(var1[var4 - 1]);
                     var3.putFloat(var1[var4]);
                  }
               } else {
                  for(var4 = 0; var4 < var1.length; var4 += 2) {
                     var3.putFloat(var1[var4]);
                     var3.putFloat(var1[var4 + 1]);
                  }
               }

               var2.clear();
               var2.addPath(var1.length / 2, var3, false);
               var4 = var2.generatePolygons();
               if (var4 < 1) {
                  this.bTriangulateFailed = true;
                  return null;
               } else {
                  var3.clear();
                  int var5 = var2.triangulate(0, var3);
                  this.triangles = new float[var5 * 2];

                  for(int var6 = 0; var6 < var5; ++var6) {
                     this.triangles[var6 * 2] = var3.getFloat();
                     this.triangles[var6 * 2 + 1] = var3.getFloat();
                  }

                  this.initTriangleAreas();
                  return this.triangles;
               }
            }
         }
      } else {
         return null;
      }
   }

   public float getPolylineLength() {
      if (this.isPolyline() && !this.points.isEmpty()) {
         float var1 = 0.0F;

         for(int var2 = 0; var2 < this.points.size() - 2; var2 += 2) {
            int var3 = this.points.get(var2);
            int var4 = this.points.get(var2 + 1);
            int var5 = this.points.get(var2 + 2);
            int var6 = this.points.get(var2 + 3);
            var1 += Vector2f.length((float)(var5 - var3), (float)(var6 - var4));
         }

         return var1;
      } else {
         return 0.0F;
      }
   }

   public void Dispose() {
      this.pickedRZStory = null;
      this.points.clear();
      this.polylineOutlinePoints = null;
      this.spawnedZombies.clear();
      this.triangles = null;
   }

   public String toString() {
      StringBuffer var1 = new StringBuffer("Zone{");
      var1.append("name='").append(this.name).append('\'');
      var1.append(", type='").append(this.type).append('\'');
      var1.append(", x=").append(this.x);
      var1.append(", y=").append(this.y);
      var1.append(", w=").append(this.w);
      var1.append(", h=").append(this.h);
      var1.append(", id=").append(this.id.toString());
      var1.append('}');
      return var1.toString();
   }

   private static enum PolygonHit {
      OnEdge,
      Inside,
      Outside;

      private PolygonHit() {
      }
   }
}
