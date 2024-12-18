package zombie.inventory.types;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemType;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.scripting.objects.Item;

public final class Key extends InventoryItem {
   private int keyId = -1;
   private boolean padlock = false;
   private int numberOfKey = 0;
   private boolean digitalPadlock = false;
   public static final Key[] highlightDoor = new Key[4];
   public static final ArrayList<IsoObject> doors = new ArrayList();

   public Key(String var1, String var2, String var3, String var4) {
      super(var1, var2, var3, var4);
      this.cat = ItemType.Key;
   }

   public int getSaveType() {
      return Item.Type.Key.ordinal();
   }

   public void takeKeyId() {
      ItemContainer var1 = this.getOutermostContainer();
      if (var1 != null && var1.getSourceGrid() != null && var1.getSourceGrid().getBuilding() != null && var1.getSourceGrid().getBuilding().def != null) {
         this.setKeyId(var1.getSourceGrid().getBuilding().def.getKeyId());
      } else {
         this.setKeyId(Rand.Next(10000000));
      }

   }

   public static void setHighlightDoors(int var0, InventoryItem var1) {
      Iterator var2;
      IsoObject var3;
      if (var1 instanceof Key && !((Key)var1).isPadlock() && !((Key)var1).isDigitalPadlock()) {
         highlightDoor[var0] = (Key)var1;
         var2 = doors.iterator();

         while(var2.hasNext()) {
            var3 = (IsoObject)var2.next();
            var3.setHighlighted(false);
         }

         doors.clear();
         IsoPlayer var10 = IsoPlayer.players[var0];
         int var11 = (int)var10.getX();
         int var4 = (int)var10.getY();
         int var5 = (int)var10.getZ();

         for(int var6 = var11 - 20; var6 < var11 + 20; ++var6) {
            for(int var7 = var4 - 20; var7 < var4 + 20; ++var7) {
               IsoGridSquare var8 = IsoWorld.instance.getCell().getGridSquare(var6, var7, var5);
               if (var8 != null) {
                  IsoObject var9 = var8.getDoor(true);
                  if (var9 instanceof IsoDoor) {
                     var9.setHighlightColor(Core.getInstance().getObjectHighlitedColor());
                     ((IsoDoor)var9).checkKeyHighlight();
                     doors.add(var9);
                  }

                  if (var9 instanceof IsoThumpable && ((IsoThumpable)var9).isDoor()) {
                     var9.setHighlightColor(Core.getInstance().getObjectHighlitedColor());
                     ((IsoThumpable)var9).checkKeyHighlight();
                     doors.add(var9);
                  }

                  var9 = var8.getDoor(false);
                  if (var9 instanceof IsoDoor) {
                     var9.setHighlightColor(Core.getInstance().getObjectHighlitedColor());
                     ((IsoDoor)var9).checkKeyHighlight();
                     doors.add(var9);
                  }

                  if (var9 instanceof IsoThumpable && ((IsoThumpable)var9).isDoor()) {
                     var9.setHighlightColor(Core.getInstance().getObjectHighlitedColor());
                     ((IsoThumpable)var9).checkKeyHighlight();
                     doors.add(var9);
                  }
               }
            }
         }
      } else {
         if (!doors.isEmpty()) {
            var2 = doors.iterator();

            while(var2.hasNext()) {
               var3 = (IsoObject)var2.next();
               var3.setHighlighted(false, false);
            }

            doors.clear();
         }

         highlightDoor[var0] = null;
      }

   }

   public int getKeyId() {
      return this.keyId;
   }

   public void setKeyId(int var1) {
      this.keyId = var1;
   }

   public String getCategory() {
      return this.mainCategory != null ? this.mainCategory : "Key";
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
      var1.putInt(this.getKeyId());
      var1.put((byte)this.numberOfKey);
   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      super.load(var1, var2);
      this.setKeyId(var1.getInt());
      this.numberOfKey = var1.get();
   }

   public boolean isPadlock() {
      return this.padlock;
   }

   public void setPadlock(boolean var1) {
      this.padlock = var1;
   }

   public int getNumberOfKey() {
      return this.numberOfKey;
   }

   public void setNumberOfKey(int var1) {
      this.numberOfKey = var1;
   }

   public boolean isDigitalPadlock() {
      return this.digitalPadlock;
   }

   public void setDigitalPadlock(boolean var1) {
      this.digitalPadlock = var1;
   }
}
