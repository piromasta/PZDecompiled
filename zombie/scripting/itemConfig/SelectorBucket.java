package zombie.scripting.itemConfig;

import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.GameEntity;
import zombie.inventory.ItemPickInfo;
import zombie.scripting.itemConfig.enums.SelectorType;
import zombie.scripting.itemConfig.enums.SituatedType;
import zombie.scripting.itemConfig.script.SelectorBucketScript;

public class SelectorBucket {
   private final SelectorBucket[] children;
   private final SelectorType selectorType;
   private final int[] selectorIDs;
   private final SituatedType selectorSituated;
   private final int selectorWorldAge;
   private final Randomizer randomizer;
   private final String origSelectorString;
   private SelectorBucket onCreate;

   public SelectorType getSelectorType() {
      return this.selectorType;
   }

   public int[] getSelectorIDs() {
      return this.selectorIDs;
   }

   public SituatedType getSelectorSituated() {
      return this.selectorSituated;
   }

   public int getSelectorWorldAge() {
      return this.selectorWorldAge;
   }

   public Randomizer getRandomizer() {
      return this.randomizer;
   }

   public String getOrigSelectorString() {
      return this.origSelectorString;
   }

   public boolean containsSelectorID(int var1) {
      if (var1 != -1 && this.selectorIDs != null) {
         if (this.selectorIDs.length == 1) {
            return this.selectorIDs[0] == var1;
         } else {
            for(int var2 = 0; var2 < this.selectorIDs.length; ++var2) {
               if (this.selectorIDs[var2] == var1) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public boolean hasSelectorIDs() {
      return this.selectorIDs != null && this.selectorIDs.length > 0;
   }

   public SelectorBucket(int[] var1, SelectorBucketScript var2, SelectorBucket[] var3, Randomizer var4) {
      this.selectorIDs = var1;
      this.selectorType = var2.getSelectorType();
      this.selectorSituated = var2.getSelectorSituated();
      this.selectorWorldAge = var2.getSelectorWorldAge();
      this.randomizer = var4;
      this.children = var3;
      this.origSelectorString = var2.getSelectorString();
   }

   public boolean Resolve(GameEntity var1, ItemPickInfo var2) {
      if (this.selectorType == SelectorType.None || var2.isMatch(this)) {
         if (this.children.length > 0) {
            for(int var3 = 0; var3 < this.children.length; ++var3) {
               SelectorBucket var4 = this.children[var3];
               if (var4.Resolve(var1, var2)) {
                  return true;
               }
            }
         }

         if (this.selectorType != SelectorType.None && this.randomizer != null) {
            this.randomizer.execute(var1);
            return true;
         }
      }

      return false;
   }

   public boolean ResolveOnCreate(GameEntity var1) {
      if (this.selectorType == SelectorType.OnCreate && this.randomizer != null) {
         this.randomizer.execute(var1);
         return true;
      } else {
         if (Core.bDebug) {
            DebugLog.General.error("Something went wrong in SelectorBucket.ResolveOnCreate.");
         }

         return false;
      }
   }
}
