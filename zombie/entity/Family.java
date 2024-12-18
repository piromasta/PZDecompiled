package zombie.entity;

import zombie.entity.util.BitSet;
import zombie.entity.util.ObjectMap;

public class Family {
   private static ObjectMap<String, Family> families = new ObjectMap();
   private static int familyIndex = 0;
   private static final Builder builder = new Builder();
   private static final BitSet zeroBits = new BitSet();
   private final BitSet all;
   private final BitSet one;
   private final BitSet exclude;
   private final int index;

   private Family(BitSet var1, BitSet var2, BitSet var3) {
      this.all = var1;
      this.one = var2;
      this.exclude = var3;
      this.index = familyIndex++;
   }

   public int getIndex() {
      return this.index;
   }

   public boolean matches(GameEntity var1) {
      BitSet var2 = var1.getComponentBits();
      if (var2 == null) {
         return false;
      } else if (!var2.containsAll(this.all)) {
         return false;
      } else if (!this.one.isEmpty() && !this.one.intersects(var2)) {
         return false;
      } else {
         return this.exclude.isEmpty() || !this.exclude.intersects(var2);
      }
   }

   public static final Builder all(ComponentType... var0) {
      return builder.reset().all(var0);
   }

   public static final Builder one(ComponentType... var0) {
      return builder.reset().one(var0);
   }

   public static final Builder exclude(ComponentType... var0) {
      return builder.reset().exclude(var0);
   }

   public int hashCode() {
      return this.index;
   }

   public boolean equals(Object var1) {
      return this == var1;
   }

   private static String getFamilyHash(BitSet var0, BitSet var1, BitSet var2) {
      StringBuilder var3 = new StringBuilder();
      if (!var0.isEmpty()) {
         var3.append("{all:").append(getBitsString(var0)).append("}");
      }

      if (!var1.isEmpty()) {
         var3.append("{one:").append(getBitsString(var1)).append("}");
      }

      if (!var2.isEmpty()) {
         var3.append("{exclude:").append(getBitsString(var2)).append("}");
      }

      return var3.toString();
   }

   private static String getBitsString(BitSet var0) {
      StringBuilder var1 = new StringBuilder();
      int var2 = var0.length();

      for(int var3 = 0; var3 < var2; ++var3) {
         var1.append(var0.get(var3) ? "1" : "0");
      }

      return var1.toString();
   }

   public static class Builder {
      private BitSet all;
      private BitSet one;
      private BitSet exclude;

      Builder() {
         this.all = Family.zeroBits;
         this.one = Family.zeroBits;
         this.exclude = Family.zeroBits;
      }

      public Builder reset() {
         this.all = Family.zeroBits;
         this.one = Family.zeroBits;
         this.exclude = Family.zeroBits;
         return this;
      }

      public final Builder all(ComponentType... var1) {
         this.all = ComponentType.getBitsFor(var1);
         return this;
      }

      public final Builder one(ComponentType... var1) {
         this.one = ComponentType.getBitsFor(var1);
         return this;
      }

      public final Builder exclude(ComponentType... var1) {
         this.exclude = ComponentType.getBitsFor(var1);
         return this;
      }

      public Family get() {
         String var1 = Family.getFamilyHash(this.all, this.one, this.exclude);
         Family var2 = (Family)Family.families.get(var1, (Object)null);
         if (var2 == null) {
            var2 = new Family(this.all, this.one, this.exclude);
            Family.families.put(var1, var2);
         }

         return var2;
      }
   }
}
