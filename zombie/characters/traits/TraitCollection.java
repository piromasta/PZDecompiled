package zombie.characters.traits;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import zombie.util.Lambda;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public class TraitCollection {
   private final List<String> m_activeTraitNames = new ArrayList();
   private final List<TraitSlot> m_traits = new ArrayList();

   public TraitCollection() {
   }

   public boolean remove(Object var1) {
      return this.remove(String.valueOf(var1));
   }

   public boolean remove(String var1) {
      int var2 = this.indexOfTrait(var1);
      if (var2 > -1) {
         this.deactivateTraitSlot(var2);
      }

      return var2 > -1;
   }

   public void addAll(Collection<? extends String> var1) {
      PZArrayUtil.forEach((Iterable)var1, this::add);
   }

   public void removeAll(Collection<?> var1) {
      PZArrayUtil.forEach((Iterable)var1, this::remove);
   }

   public void clear() {
      PZArrayUtil.forEach(this.m_traits, (var0) -> {
         var0.m_isSet = false;
      });
      this.m_activeTraitNames.clear();
   }

   public int size() {
      return this.m_activeTraitNames.size();
   }

   public boolean isEmpty() {
      return this.m_activeTraitNames.isEmpty();
   }

   public boolean contains(Object var1) {
      return this.contains(String.valueOf(var1));
   }

   public boolean contains(String var1) {
      int var2 = this.indexOfTrait(var1);
      return var2 > -1 && this.getSlotInternal(var2).m_isSet;
   }

   public void add(String var1) {
      if (var1 != null) {
         this.getOrCreateSlotInternal(var1).m_isSet = true;
         this.m_activeTraitNames.add(var1);
      }
   }

   public String get(int var1) {
      return (String)this.m_activeTraitNames.get(var1);
   }

   public void set(String var1, boolean var2) {
      if (var2) {
         this.add(var1);
      } else {
         this.remove(var1);
      }

   }

   public TraitSlot getTraitSlot(String var1) {
      return StringUtils.isNullOrWhitespace(var1) ? null : this.getOrCreateSlotInternal(var1);
   }

   private int indexOfTrait(String var1) {
      return PZArrayUtil.indexOf(this.m_traits, Lambda.predicate(var1, TraitSlot::isName));
   }

   private TraitSlot getSlotInternal(int var1) {
      return (TraitSlot)this.m_traits.get(var1);
   }

   private TraitSlot getOrCreateSlotInternal(String var1) {
      int var2 = this.indexOfTrait(var1);
      if (var2 == -1) {
         var2 = this.m_traits.size();
         this.m_traits.add(new TraitSlot(var1));
      }

      return this.getSlotInternal(var2);
   }

   private void deactivateTraitSlot(int var1) {
      TraitSlot var2 = this.getSlotInternal(var1);
      var2.m_isSet = false;
      int var3 = PZArrayUtil.indexOf(this.m_activeTraitNames, Lambda.predicate(var2.Name, String::equalsIgnoreCase));
      if (var3 != -1) {
         this.m_activeTraitNames.remove(var3);
      }

   }

   public String toString() {
      return "TraitCollection(" + PZArrayUtil.arrayToString((Iterable)this.m_activeTraitNames, "", "", ", ") + ")";
   }

   public class TraitSlot {
      public final String Name;
      private boolean m_isSet;

      private TraitSlot(String var2) {
         this.Name = var2;
         this.m_isSet = false;
      }

      public boolean isName(String var1) {
         return StringUtils.equalsIgnoreCase(this.Name, var1);
      }

      public boolean isSet() {
         return this.m_isSet;
      }

      public void set(boolean var1) {
         if (this.m_isSet != var1) {
            TraitCollection.this.set(this.Name, var1);
         }
      }

      public String toString() {
         return "TraitSlot(" + this.Name + ":" + this.m_isSet + ")";
      }
   }
}
