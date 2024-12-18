package zombie.characters.animals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoAnimalTrack;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.util.StringUtils;

public class AnimalTracks {
   public String animalType;
   public long addedTime;
   public String trackType;
   public IsoDirections dir;
   public int x;
   public int y;
   public int minSkill = 0;
   public boolean addedToWorld = false;
   public boolean discovered = false;
   public InventoryItem item = null;

   public AnimalTracks() {
   }

   public static AnimalTracks addAnimalTrack(VirtualAnimal var0, AnimalTracksDefinitions.AnimalTracksType var1) {
      AnimalTracks var2 = new AnimalTracks();
      var2.animalType = var0.migrationGroup;
      var2.trackType = var1.type;
      if (var1.needDir) {
         var2.dir = IsoDirections.fromAngle(var0.m_forwardDirection.x, var0.m_forwardDirection.y);
      }

      var2.x = Rand.Next((int)var0.m_x - 2, (int)var0.m_x + 2);
      var2.y = Rand.Next((int)var0.m_y - 2, (int)var0.m_y + 2);
      var2.addedTime = GameTime.getInstance().getCalender().getTimeInMillis();
      return var2;
   }

   public static AnimalTracks addAnimalTrackAtPos(VirtualAnimal var0, int var1, int var2, AnimalTracksDefinitions.AnimalTracksType var3, long var4) {
      AnimalTracks var6 = new AnimalTracks();
      var6.animalType = var0.migrationGroup;
      var6.trackType = var3.type;
      if (var3.needDir) {
         var6.dir = IsoDirections.fromAngle(var0.m_forwardDirection.x, var0.m_forwardDirection.y);
      }

      var6.x = var1;
      var6.y = var2;
      var6.addedTime = GameTime.getInstance().getCalender().getTimeInMillis() - var4;
      return var6;
   }

   public boolean canFindTrack(IsoGameCharacter var1) {
      if (this.addedToWorld) {
         return true;
      } else if (var1 != null && var1.getCurrentSquare() != null) {
         AnimalTracksDefinitions var2 = (AnimalTracksDefinitions)AnimalTracksDefinitions.tracksDefinitions.get(this.animalType);
         if (var2 == null) {
            return false;
         } else {
            AnimalTracksDefinitions.AnimalTracksType var3 = (AnimalTracksDefinitions.AnimalTracksType)var2.tracks.get(this.trackType);
            if (var3 == null) {
               return false;
            } else if (var1.getPerkLevel(PerkFactory.Perks.Tracking) >= var3.minSkill) {
               float var4 = (float)(var2.chanceToFindTrack + var3.chanceToFindTrack);
               var4 /= (float)(var1.getPerkLevel(PerkFactory.Perks.Tracking) + 1) / 0.7F;
               float var5 = var1.getCurrentSquare().DistToProper(this.x, this.y);
               if (var5 < 20.0F) {
                  var5 = (20.0F - var5) / 20.0F;
                  var4 /= var5 + 2.0F;
               }

               if (var5 < 4.0F) {
                  var4 /= 20.0F;
               }

               if (!Rand.NextBool((int)var4)) {
                  this.addTrackingExp(var1, false);
                  return false;
               } else {
                  this.addTrackingExp(var1, true);
                  return true;
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public void addTrackingExp(IsoGameCharacter var1, boolean var2) {
      if (var2) {
         if (GameServer.bServer) {
            GameServer.addXp((IsoPlayer)var1, PerkFactory.Perks.Tracking, Rand.Next(7.0F, 15.0F));
         } else if (!GameClient.bClient) {
            var1.getXp().AddXP(PerkFactory.Perks.Tracking, Rand.Next(7.0F, 15.0F));
         }
      } else if (GameServer.bServer) {
         if (Rand.NextBool(10)) {
            GameServer.addXp((IsoPlayer)var1, PerkFactory.Perks.Tracking, Rand.Next(2.0F, 4.0F));
         }
      } else if (!GameClient.bClient && Rand.NextBool(10)) {
         var1.getXp().AddXP(PerkFactory.Perks.Tracking, Rand.Next(2.0F, 4.0F));
      }

   }

   public static String getTrackStr(String var0) {
      return Translator.getText("IGUI_AnimalTracks_" + var0);
   }

   public static ArrayList<AnimalTracks> getAndFindNearestTracks(IsoGameCharacter var0) {
      ArrayList var1 = getNearestTracks((int)var0.getX(), (int)var0.getY(), 20 + var0.getPerkLevel(PerkFactory.Perks.Tracking) * 2);
      if (var1 == null) {
         return null;
      } else {
         Iterator var2 = var1.iterator();

         while(var2.hasNext()) {
            AnimalTracks var3 = (AnimalTracks)var2.next();
            if (var3.canFindTrack(var0)) {
               var3.setDiscovered(true);
            }
         }

         return var1;
      }
   }

   public static ArrayList<AnimalTracks> getNearestTracks(int var0, int var1, int var2) {
      ArrayList var3 = new ArrayList();
      AnimalCell var4 = AnimalManagerWorker.getInstance().getCellFromSquarePos(PZMath.fastfloor((float)var0), PZMath.fastfloor((float)var1));
      if (var4 == null) {
         return null;
      } else {
         IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare(var0, var1, 0);
         if (var5 == null) {
            return null;
         } else {
            AnimalManagerWorker.getInstance().loadIfNeeded(var4);

            for(int var6 = 0; var6 < var4.m_chunks.length; ++var6) {
               if (var4.m_chunks[var6] != null && !var4.m_chunks[var6].m_animalTracks.isEmpty()) {
                  for(int var7 = 0; var7 < var4.m_chunks[var6].m_animalTracks.size(); ++var7) {
                     AnimalTracks var8 = (AnimalTracks)var4.m_chunks[var6].m_animalTracks.get(var7);
                     if (var5.DistToProper(var8.x, var8.y) <= (float)var2) {
                        var3.add(var8);
                     }
                  }
               }
            }

            return var3;
         }
      }
   }

   public void save(ByteBuffer var1) throws IOException {
      GameWindow.WriteString(var1, this.animalType);
      GameWindow.WriteString(var1, this.trackType);
      var1.putInt(this.x);
      var1.putInt(this.y);
      if (this.dir != null) {
         var1.put((byte)1);
         var1.putInt(this.dir.index());
      } else {
         var1.put((byte)0);
      }

      var1.putLong(this.addedTime);
      var1.put((byte)(this.addedToWorld ? 1 : 0));
      var1.put((byte)(this.discovered ? 1 : 0));
   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      this.animalType = GameWindow.ReadString(var1);
      this.trackType = GameWindow.ReadString(var1);
      this.x = var1.getInt();
      this.y = var1.getInt();
      if (var1.get() == 1) {
         this.dir = IsoDirections.fromIndex(var1.getInt());
      }

      this.addedTime = var1.getLong();
      this.addedToWorld = var1.get() == 1;
      this.discovered = var1.get() == 1;
   }

   public String getTrackType() {
      return this.trackType;
   }

   public String getTrackAge(IsoGameCharacter var1) {
      long var10000 = GameTime.getInstance().getCalender().getTimeInMillis() - this.addedTime;
      return PZMath.fastfloor((float)(var10000 / 60000L)) + " mins ago";
   }

   public IsoDirections getDir() {
      return this.dir;
   }

   public int getMinSkill() {
      return this.minSkill;
   }

   public String getTrackItem() {
      AnimalTracksDefinitions.AnimalTracksType var1 = AnimalTracksDefinitions.getTrackType(this.animalType, this.trackType);
      return var1 == null ? null : var1.item;
   }

   public String getTrackSprite() {
      AnimalTracksDefinitions.AnimalTracksType var1 = AnimalTracksDefinitions.getTrackType(this.animalType, this.trackType);
      if (var1 == null) {
         return null;
      } else if (var1.sprites != null && this.dir != null) {
         return (String)var1.sprites.get(this.dir);
      } else if (!StringUtils.isNullOrEmpty(var1.sprite)) {
         return var1.sprite;
      } else {
         DebugLog.Animal.debugln("Couldn't find sprite for track " + this.trackType + " for animal " + this.animalType);
         return null;
      }
   }

   public boolean isAddedToWorld() {
      return this.addedToWorld;
   }

   public void setAddedToWorld(boolean var1) {
      this.addedToWorld = var1;
   }

   public InventoryItem addItemToWorld() {
      if (this.addedToWorld) {
         return this.item;
      } else {
         this.addedToWorld = true;
         IsoGridSquare var1 = IsoWorld.instance.CurrentCell.getGridSquare(this.x, this.y, 0);
         if (var1 != null) {
            String var2 = this.getTrackItem();
            if (StringUtils.isNullOrEmpty(var2)) {
               return null;
            } else {
               InventoryItem var3 = var1.AddWorldInventoryItem(InventoryItemFactory.CreateItem(var2), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
               var3.setAnimalTracks(this);
               return var3;
            }
         } else {
            return null;
         }
      }
   }

   public ArrayList<IsoAnimalTrack> getAllIsoTracks() {
      if (this.getSquare() == null) {
         return null;
      } else {
         ArrayList var1 = new ArrayList();

         for(int var2 = this.x - 5; var2 < this.x + 6; ++var2) {
            for(int var3 = this.y - 5; var3 < this.y + 6; ++var3) {
               IsoGridSquare var4 = IsoWorld.instance.CurrentCell.getGridSquare(var2, var3, this.getSquare().z);
               if (var4 != null) {
                  var1.add(var4.getAnimalTrack());
               }
            }
         }

         return var1;
      }
   }

   public ArrayList<IsoAnimalTrack> addToWorld() {
      if (this.addedToWorld) {
         return this.getAllIsoTracks();
      } else {
         this.addedToWorld = true;
         ArrayList var1 = new ArrayList();
         if (this.dir != null) {
            int var2 = 0;
            int var3 = 0;
            int var4 = 0;
            int var5 = 0;
            if (this.dir == IsoDirections.N || this.dir == IsoDirections.S) {
               var2 = this.x - 1;
               var3 = this.x + 1;
               var4 = this.y - 5;
               var5 = this.y + 5;
            }

            if (this.dir == IsoDirections.W || this.dir == IsoDirections.E) {
               var2 = this.x - 1;
               var3 = this.x + 1;
               var4 = this.y - 5;
               var5 = this.y + 5;
            }

            if (this.dir == IsoDirections.NE || this.dir == IsoDirections.SE) {
               var4 = this.y - 4;
               var5 = this.y + 4;
               var2 = this.x - 2;
               var3 = this.x + 2;
            }

            if (this.dir == IsoDirections.NW || this.dir == IsoDirections.SW) {
               var2 = this.x - 4;
               var3 = this.x + 4;
               var4 = this.y - 2;
               var5 = this.y + 2;
            }

            int var6 = 0;

            for(int var7 = var2; var7 < var3 + 1; ++var7) {
               for(int var8 = var4; var8 < var5 + 1; ++var8) {
                  IsoGridSquare var9 = IsoWorld.instance.CurrentCell.getGridSquare(var7, var8, 0);
                  if (var9 != null) {
                     String var10 = this.getTrackSprite();
                     if (var10 != null && (var6 == 0 || Rand.NextBool(6))) {
                        ++var6;
                        var1.add(new IsoAnimalTrack(var9, var10, this));
                     }
                  }
               }
            }
         } else {
            IsoGridSquare var11 = IsoWorld.instance.CurrentCell.getGridSquare(this.x, this.y, 0);
            if (var11 != null) {
               String var12 = this.getTrackSprite();
               if (var12 != null) {
                  var1.add(new IsoAnimalTrack(var11, var12, this));
               }
            }
         }

         return var1;
      }
   }

   public IsoAnimalTrack getIsoAnimalTrack() {
      IsoGridSquare var1 = IsoWorld.instance.CurrentCell.getGridSquare(this.x, this.y, 0);
      if (var1 == null) {
         return null;
      } else {
         for(int var2 = 0; var2 < var1.getSpecialObjects().size(); ++var2) {
            if (var1.getSpecialObjects().get(var2) instanceof IsoAnimalTrack) {
               return (IsoAnimalTrack)var1.getSpecialObjects().get(var2);
            }
         }

         return null;
      }
   }

   public String getFreshnessString(int var1) {
      int var2 = this.getTrackHours();
      if (var1 > 6) {
         if (var2 < 12) {
            return Translator.getText("IGUI_AnimalTracks_Time_12Hours");
         } else if (var2 < 24) {
            return Translator.getText("IGUI_AnimalTracks_Time_24Hours");
         } else if (var2 < 48) {
            return Translator.getText("IGUI_AnimalTracks_Time_2Days");
         } else if (var2 < 72) {
            return Translator.getText("IGUI_AnimalTracks_Time_3Days");
         } else {
            return var2 < 96 ? Translator.getText("IGUI_AnimalTracks_Time_4Days") : Translator.getText("IGUI_AnimalTracks_Time_5Days");
         }
      } else if (var1 > 3) {
         if (var2 < 24) {
            return Translator.getText("IGUI_AnimalTracks_Time_VeryRecent");
         } else if (var2 < 48) {
            return Translator.getText("IGUI_AnimalTracks_Time_Recent");
         } else {
            return var2 < 72 ? Translator.getText("IGUI_AnimalTracks_Time_SomeDays") : Translator.getText("IGUI_AnimalTracks_Time_Old");
         }
      } else if (var2 < 24) {
         return Translator.getText("IGUI_AnimalTracks_Time_Recent");
      } else {
         return var2 < 72 ? Translator.getText("IGUI_AnimalTracks_Time_SomeDays") : Translator.getText("IGUI_AnimalTracks_Time_Old");
      }
   }

   public int getTrackAgeDays() {
      return PZMath.fastfloor((float)((GameTime.getInstance().getCalender().getTimeInMillis() - this.addedTime) / 60000L)) / 1440;
   }

   public int getTrackHours() {
      return PZMath.fastfloor((float)((GameTime.getInstance().getCalender().getTimeInMillis() - this.addedTime) / 60000L)) / 60;
   }

   public boolean isItem() {
      AnimalTracksDefinitions.AnimalTracksType var1 = AnimalTracksDefinitions.getTrackType(this.animalType, this.trackType);
      if (var1 == null) {
         return false;
      } else {
         return !StringUtils.isNullOrEmpty(var1.item);
      }
   }

   public IsoGridSquare getSquare() {
      return IsoWorld.instance.CurrentCell.getGridSquare(this.x, this.y, 0);
   }

   public String getTimestamp() {
      return "Fresh";
   }

   public String getAnimalType() {
      return this.animalType;
   }

   public boolean isDiscovered() {
      return this.discovered;
   }

   public void setDiscovered(boolean var1) {
      this.discovered = var1;
   }
}
