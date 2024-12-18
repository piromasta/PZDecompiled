package zombie.world;

import zombie.scripting.objects.Item;

public class ItemInfo extends DictionaryInfo<ItemInfo> {
   protected Item scriptItem;

   public ItemInfo() {
   }

   public String getInfoType() {
      return "item";
   }

   public Item getScriptItem() {
      return this.scriptItem;
   }

   public ItemInfo copy() {
      ItemInfo var1 = new ItemInfo();
      var1.copyFrom(this);
      var1.scriptItem = this.scriptItem;
      return var1;
   }
}
