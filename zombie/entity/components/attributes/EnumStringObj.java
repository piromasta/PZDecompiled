package zombie.entity.components.attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import zombie.entity.util.enums.IOEnum;

public class EnumStringObj<E extends Enum<E> & IOEnum> {
   private final ArrayList<String> stringValues = new ArrayList();
   private EnumSet<E> enumValues;
   private boolean dirty = false;

   protected EnumStringObj() {
   }

   protected void initialize(Class<E> var1) {
      this.enumValues = EnumSet.noneOf(var1);
   }

   protected void reset() {
      this.clear();
      this.enumValues = null;
   }

   protected EnumSet<E> getEnumValues() {
      return this.enumValues;
   }

   protected ArrayList<String> getStringValues() {
      return this.stringValues;
   }

   public boolean equals(Object var1) {
      if (var1 == this) {
         return true;
      } else if (!(var1 instanceof EnumStringObj)) {
         return false;
      } else {
         EnumStringObj var2 = (EnumStringObj)var1;
         if (!this.enumValues.equals(var2.enumValues)) {
            return false;
         } else if (this.stringValues.size() == 0 && var2.stringValues.size() == 0) {
            return true;
         } else if (this.stringValues.size() != var2.stringValues.size()) {
            return false;
         } else {
            this.sort();
            var2.sort();
            return this.stringValues.equals(var2.stringValues);
         }
      }
   }

   public String toString() {
      if (this.enumValues.size() == 0 && this.stringValues.size() == 0) {
         return "E[] S[]";
      } else {
         String var1 = "E" + this.enumValues;
         this.sort();
         var1 = var1 + " S[";

         for(int var2 = 0; var2 < this.stringValues.size(); ++var2) {
            var1 = var1 + (String)this.stringValues.get(var2);
            if (var2 < this.stringValues.size() - 1) {
               var1 = var1 + ",";
            }
         }

         var1 = var1 + "]";
         return var1;
      }
   }

   public EnumStringObj<E> copy() {
      EnumStringObj var1 = new EnumStringObj();
      var1.enumValues = EnumSet.copyOf(this.enumValues);
      var1.stringValues.addAll(this.stringValues);
      return var1;
   }

   private void sort() {
      if (this.dirty) {
         Collections.sort(this.stringValues);
         this.dirty = false;
      }

   }

   public void getSortedNames(ArrayList<String> var1) {
      if (var1.size() > 0) {
         var1.clear();
      }

      var1.addAll(this.stringValues);
      Iterator var2 = this.enumValues.iterator();

      while(var2.hasNext()) {
         Enum var3 = (Enum)var2.next();
         var1.add(var3.toString());
      }

      Collections.sort(var1);
   }

   public int size() {
      return this.enumValues.size() + this.stringValues.size();
   }

   public int sizeEnums() {
      return this.enumValues.size();
   }

   public int sizeStrings() {
      return this.stringValues.size();
   }

   public void clear() {
      this.enumValues.clear();
      this.stringValues.clear();
      this.dirty = true;
   }

   public boolean isEmpty() {
      return this.enumValues.isEmpty() && this.stringValues.isEmpty();
   }

   public void add(E var1) {
      this.enumValues.add(var1);
   }

   public void add(String var1) {
      if (this.stringValues.size() >= 127) {
         throw new UnsupportedOperationException("String values size may not exceed: 127");
      } else {
         if (var1 != null && !this.stringValues.contains(var1)) {
            this.stringValues.add(var1);
            this.dirty = true;
         }

      }
   }

   public boolean remove(E var1) {
      return this.enumValues.remove(var1);
   }

   public boolean remove(String var1) {
      this.dirty = true;
      return this.stringValues.remove(var1);
   }

   public boolean contains(E var1) {
      return this.enumValues.contains(var1);
   }

   public boolean contains(String var1) {
      return this.stringValues.contains(var1);
   }

   public void removeAllStrings() {
      this.stringValues.clear();
   }

   public void removeAllEnums() {
      this.enumValues.clear();
   }

   public void addAll(boolean var1, EnumStringObj<E> var2) {
      if (var1) {
         if (this.stringValues.size() > 0) {
            this.stringValues.clear();
         }

         if (this.enumValues.size() > 0) {
            this.enumValues.clear();
         }
      }

      this.dirty = true;
      this.stringValues.addAll(var2.stringValues);
      this.enumValues.addAll(var2.enumValues);
   }

   public void addAll(EnumStringObj<E> var1) {
      this.addAll(false, var1);
   }
}
