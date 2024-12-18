package zombie.characters.WornItems;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.function.Consumer;
import zombie.GameWindow;
import zombie.core.Color;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.Texture;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.InventoryContainer;
import zombie.util.StringUtils;

public final class WornItems {
   protected final BodyLocationGroup group;
   protected final ArrayList<WornItem> items = new ArrayList();

   public WornItems(BodyLocationGroup var1) {
      this.group = var1;
   }

   public WornItems(WornItems var1) {
      this.group = var1.group;
      this.copyFrom(var1);
   }

   public void copyFrom(WornItems var1) {
      if (this.group != var1.group) {
         throw new RuntimeException("group=" + this.group.id + " other.group=" + var1.group.id);
      } else {
         this.items.clear();
         this.items.addAll(var1.items);
      }
   }

   public BodyLocationGroup getBodyLocationGroup() {
      return this.group;
   }

   public WornItem get(int var1) {
      return (WornItem)this.items.get(var1);
   }

   public void setItem(String var1, InventoryItem var2) {
      this.group.checkValid(var1);
      int var3;
      if (!this.group.isMultiItem(var1)) {
         var3 = this.indexOf(var1);
         if (var3 != -1) {
            this.items.remove(var3);
         }
      }

      WornItem var4;
      for(var3 = 0; var3 < this.items.size(); ++var3) {
         var4 = (WornItem)this.items.get(var3);
         if (this.group.isExclusive(var1, var4.location)) {
            this.items.remove(var3--);
         }
      }

      if (var2 != null) {
         this.remove(var2);
         var3 = this.items.size();

         for(int var6 = 0; var6 < this.items.size(); ++var6) {
            WornItem var5 = (WornItem)this.items.get(var6);
            if (this.group.indexOf(var5.getLocation()) > this.group.indexOf(var1)) {
               var3 = var6;
               break;
            }
         }

         var4 = new WornItem(var1, var2);
         this.items.add(var3, var4);
      }
   }

   public InventoryItem getItem(String var1) {
      this.group.checkValid(var1);
      int var2 = this.indexOf(var1);
      return var2 == -1 ? null : ((WornItem)this.items.get(var2)).item;
   }

   public InventoryItem getItemByIndex(int var1) {
      return var1 >= 0 && var1 < this.items.size() ? ((WornItem)this.items.get(var1)).getItem() : null;
   }

   public void remove(InventoryItem var1) {
      int var2 = this.indexOf(var1);
      if (var2 != -1) {
         this.items.remove(var2);
      }
   }

   public void clear() {
      this.items.clear();
   }

   public String getLocation(InventoryItem var1) {
      int var2 = this.indexOf(var1);
      return var2 == -1 ? null : ((WornItem)this.items.get(var2)).getLocation();
   }

   public boolean contains(InventoryItem var1) {
      return this.indexOf(var1) != -1;
   }

   public int size() {
      return this.items.size();
   }

   public boolean isEmpty() {
      return this.items.isEmpty();
   }

   public void forEach(Consumer<WornItem> var1) {
      for(int var2 = 0; var2 < this.items.size(); ++var2) {
         var1.accept((WornItem)this.items.get(var2));
      }

   }

   public void setFromItemVisuals(ItemVisuals var1) {
      this.clear();

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         ItemVisual var3 = (ItemVisual)var1.get(var2);
         String var4 = var3.getItemType();
         InventoryItem var5 = InventoryItemFactory.CreateItem(var4);
         if (var5 != null) {
            if (var5.getVisual() != null) {
               var5.getVisual().copyFrom(var3);
               var5.synchWithVisual();
            }

            if (var5 instanceof Clothing && !StringUtils.isNullOrWhitespace(var5.getBodyLocation())) {
               this.setItem(var5.getBodyLocation(), var5);
            } else if (var5 instanceof InventoryContainer && !StringUtils.isNullOrWhitespace(((InventoryContainer)var5).canBeEquipped())) {
               this.setItem(((InventoryContainer)var5).canBeEquipped(), var5);
            }
         }
      }

   }

   public void getItemVisuals(ItemVisuals var1) {
      var1.clear();

      for(int var2 = 0; var2 < this.items.size(); ++var2) {
         InventoryItem var3 = ((WornItem)this.items.get(var2)).getItem();
         ItemVisual var4 = var3.getVisual();
         if (var4 != null) {
            var4.setInventoryItem(var3);
            var1.add(var4);
         }
      }

   }

   public void addItemsToItemContainer(ItemContainer var1) {
      for(int var2 = 0; var2 < this.items.size(); ++var2) {
         InventoryItem var3 = ((WornItem)this.items.get(var2)).getItem();
         int var4 = var3.getVisual().getHolesNumber();
         var3.setConditionNoSound(var3.getConditionMax() - var4 * 3);
         var1.AddItem(var3);
      }

   }

   private int indexOf(String var1) {
      for(int var2 = 0; var2 < this.items.size(); ++var2) {
         WornItem var3 = (WornItem)this.items.get(var2);
         if (var3.location.equals(var1)) {
            return var2;
         }
      }

      return -1;
   }

   private int indexOf(InventoryItem var1) {
      for(int var2 = 0; var2 < this.items.size(); ++var2) {
         WornItem var3 = (WornItem)this.items.get(var2);
         if (var3.getItem() == var1) {
            return var2;
         }
      }

      return -1;
   }

   public void save(ByteBuffer var1) throws IOException {
      short var2 = (short)this.items.size();
      var1.putShort(var2);

      for(int var3 = 0; var3 < var2; ++var3) {
         WornItem var4 = (WornItem)this.items.get(var3);
         GameWindow.WriteStringUTF(var1, var4.getLocation());
         GameWindow.WriteStringUTF(var1, var4.getItem().getType());
         GameWindow.WriteStringUTF(var1, var4.getItem().getTex().getName());
         var4.getItem().col.save(var1);
         var1.putInt(var4.getItem().getVisual().getBaseTexture());
         var1.putInt(var4.getItem().getVisual().getTextureChoice());
         ImmutableColor var5 = var4.getItem().getVisual().getTint();
         var1.putFloat(var5.r);
         var1.putFloat(var5.g);
         var1.putFloat(var5.b);
         var1.putFloat(var5.a);
      }

   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      short var3 = var1.getShort();
      this.items.clear();

      for(int var4 = 0; var4 < var3; ++var4) {
         String var5 = GameWindow.ReadString(var1);
         String var6 = GameWindow.ReadString(var1);
         String var7 = GameWindow.ReadString(var1);
         Color var8 = new Color();
         var8.load(var1, var2);
         int var9 = var1.getInt();
         int var10 = var1.getInt();
         ImmutableColor var11 = new ImmutableColor(var1.getFloat(), var1.getFloat(), var1.getFloat(), var1.getFloat());
         InventoryItem var12 = InventoryItemFactory.CreateItem(var6);
         if (var12 != null) {
            var12.setTexture(Texture.trygetTexture(var7));
            if (var12.getTex() == null) {
               var12.setTexture(Texture.getSharedTexture("media/inventory/Question_On.png"));
            }

            String var13 = var7.replace("Item_", "media/inventory/world/WItem_");
            var13 = var13 + ".png";
            var12.setWorldTexture(var13);
            var12.setColor(var8);
            var12.getVisual().m_Tint = new ImmutableColor(var8);
            var12.getVisual().setBaseTexture(var9);
            var12.getVisual().setTextureChoice(var10);
            var12.getVisual().setTint(var11);
            this.items.add(new WornItem(var5, var12));
         }
      }

   }
}
