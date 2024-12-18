package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.characters.IsoPlayer;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class RDSRPGNight extends RandomizedDeadSurvivorBase {
   private final ArrayList<String> items = new ArrayList();
   private ArrayList<String> dice = new ArrayList();
   private ArrayList<String> paper = new ArrayList();
   private String manual = null;
   private String diceBag = null;
   private String pencil = null;

   public RDSRPGNight() {
      this.name = "RPG Night";
      this.setChance(1);
      this.setUnique(true);
      this.setMaximumDays(30);
      this.items.add("Base.Calculator");
      this.items.add("Base.Eraser");
      this.items.add("Base.Crisps");
      this.items.add("Base.Crisps2");
      this.items.add("Base.Crisps3");
      this.items.add("Base.Pop");
      this.items.add("Base.Pop2");
      this.items.add("Base.Pop3");
      this.items.add("Base.Magazine_Gaming");
      this.items.add("Base.RPGmanual");
      this.items.add("Base.ComicBook");
      this.items.add("Base.Hat_Wizard");
      this.items.add("Base.Paperback_Fantasy");
      this.dice.add("Base.Dice_00");
      this.dice.add("Base.Dice_10");
      this.dice.add("Base.Dice_12");
      this.dice.add("Base.Dice_20");
      this.dice.add("Base.Dice_4");
      this.dice.add("Base.Dice_6");
      this.dice.add("Base.Dice_8");
      this.dice.add("Base.DiceBag");
      this.paper.add("Base.Notebook");
      this.paper.add("Base.SheetPaper2");
      this.paper.add("Base.GraphPaper");
      this.paper.add("Base.Journal");
      this.paper.add("Base.Note");
      this.manual = "Base.RPGmanual";
      this.diceBag = "Base.DiceBag";
      this.pencil = "Base.Pencil";
   }

   public boolean isValid(BuildingDef var1, boolean var2) {
      this.debugLine = "";
      if (GameClient.bClient) {
         return false;
      } else if (var1.isAllExplored() && !var2) {
         return false;
      } else if (SpawnPoints.instance.isSpawnBuilding(var1)) {
         this.debugLine = "Spawn houses are invalid";
         return false;
      } else if (StashSystem.isStashBuilding(var1)) {
         this.debugLine = "Stash buildings are invalid";
         return false;
      } else {
         if (!var2) {
            for(int var3 = 0; var3 < GameServer.Players.size(); ++var3) {
               IsoPlayer var4 = (IsoPlayer)GameServer.Players.get(var3);
               if (var4.getSquare() != null && var4.getSquare().getBuilding() != null && var4.getSquare().getBuilding().def == var1) {
                  return false;
               }
            }
         }

         if (this.getRoom(var1, "livingroom") != null) {
            return true;
         } else {
            this.debugLine = "No living room";
            return false;
         }
      }
   }

   public void randomizeDeadSurvivor(BuildingDef var1) {
      RoomDef var2 = this.getRoom(var1, "livingroom");
      this.addZombies(var1, Rand.Next(4, 6), "Hobbyist", 10, var2);
      this.addRandomItemsOnGround(var2, this.items, Rand.Next(8, 13));
      this.addRandomItemsOnGround(var2, this.dice, Rand.Next(8, 13));
      this.addRandomItemsOnGround(var2, this.dice, Rand.Next(4, 6));
      this.addRandomItemsOnGround(var2, this.manual, 1);
      this.addRandomItemsOnGround(var2, this.diceBag, 1);
      this.addRandomItemsOnGround(var2, this.pencil, Rand.Next(4, 6));
      var1.bAlarmed = false;
   }
}
