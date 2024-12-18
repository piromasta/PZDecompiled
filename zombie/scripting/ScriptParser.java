package zombie.scripting;

import java.util.ArrayList;
import java.util.Iterator;
import zombie.util.StringUtils;

public final class ScriptParser {
   private static StringBuilder stringBuilder = new StringBuilder();

   public ScriptParser() {
   }

   public static int readBlock(String var0, int var1, Block var2) {
      int var3;
      for(var3 = var1; var3 < var0.length(); ++var3) {
         if (var0.charAt(var3) == '{') {
            Block var4 = new Block();
            var2.children.add(var4);
            var2.elements.add(var4);
            String var5 = var0.substring(var1, var3).trim();
            String[] var6 = var5.split("\\s+");
            var4.type = var6[0];
            var4.id = var6.length > 1 ? var6[1] : null;
            if (ScriptBucket.getCurrentScriptObject() != null) {
               String var10001 = ScriptBucket.getCurrentScriptObject();
               var4.uid = "UID:" + var10001 + "@" + var4.type + "@" + Integer.toString(var3);
            }

            var3 = readBlock(var0, var3 + 1, var4);
            var1 = var3;
         } else {
            if (var0.charAt(var3) == '}') {
               return var3 + 1;
            }

            if (var0.charAt(var3) == ',') {
               Value var7 = new Value();
               var7.string = var0.substring(var1, var3);
               var2.values.add(var7);
               var2.elements.add(var7);
               var1 = var3 + 1;
            }
         }
      }

      return var3;
   }

   public static Block parse(String var0) {
      Block var1 = new Block();
      readBlock(var0, 0, var1);
      return var1;
   }

   public static String stripComments(String var0) {
      stringBuilder.setLength(0);
      stringBuilder.append(var0);

      int var2;
      for(int var1 = stringBuilder.lastIndexOf("*/"); var1 != -1; var1 = stringBuilder.lastIndexOf("*/", var2)) {
         var2 = stringBuilder.lastIndexOf("/*", var1 - 1);
         if (var2 == -1) {
            break;
         }

         int var4;
         for(int var3 = stringBuilder.lastIndexOf("*/", var1 - 1); var3 > var2; var3 = stringBuilder.lastIndexOf("*/", var4 - 2)) {
            var4 = var2;
            var2 = stringBuilder.lastIndexOf("/*", var2 - 2);
            if (var2 == -1) {
               break;
            }
         }

         if (var2 == -1) {
            break;
         }

         stringBuilder.replace(var2, var1 + 2, "");
      }

      var0 = stringBuilder.toString();
      stringBuilder.setLength(0);
      return var0;
   }

   public static ArrayList<String> parseTokens(String var0) {
      ArrayList var1 = new ArrayList();

      while(true) {
         int var2 = 0;
         int var3 = 0;
         int var4 = 0;
         if (var0.indexOf("}", var3 + 1) == -1) {
            if (var0.trim().length() > 0) {
               var1.add(var0.trim());
            }

            return var1;
         }

         do {
            var3 = var0.indexOf("{", var3 + 1);
            var4 = var0.indexOf("}", var4 + 1);
            if ((var4 >= var3 || var4 == -1) && var3 != -1) {
               var4 = var3;
               ++var2;
            } else {
               var3 = var4;
               --var2;
            }
         } while(var2 > 0);

         var1.add(var0.substring(0, var3 + 1).trim());
         var0 = var0.substring(var3 + 1);
      }
   }

   public static class Block implements BlockElement {
      public String type;
      public String id;
      public final ArrayList<BlockElement> elements = new ArrayList();
      public final ArrayList<Value> values = new ArrayList();
      public final ArrayList<Block> children = new ArrayList();
      private String uid;
      public String comment;

      public Block() {
      }

      public String getUid() {
         return this.uid;
      }

      public Block asBlock() {
         return this;
      }

      public Value asValue() {
         return null;
      }

      public boolean isEmpty() {
         return this.elements.isEmpty();
      }

      public void prettyPrint(int var1, StringBuilder var2, String var3) {
         int var4;
         for(var4 = 0; var4 < var1; ++var4) {
            var2.append('\t');
         }

         if (!StringUtils.isNullOrWhitespace(this.comment)) {
            var2.append(this.comment);
            var2.append(var3);

            for(var4 = 0; var4 < var1; ++var4) {
               var2.append('\t');
            }
         }

         var2.append(this.type);
         if (this.id != null) {
            var2.append(" ");
            var2.append(this.id);
         }

         var2.append(var3);

         for(var4 = 0; var4 < var1; ++var4) {
            var2.append('\t');
         }

         var2.append('{');
         var2.append(var3);
         this.prettyPrintElements(var1 + 1, var2, var3);

         for(var4 = 0; var4 < var1; ++var4) {
            var2.append('\t');
         }

         var2.append('}');
         var2.append(var3);
      }

      public void prettyPrintElements(int var1, StringBuilder var2, String var3) {
         BlockElement var4 = null;

         BlockElement var6;
         for(Iterator var5 = this.elements.iterator(); var5.hasNext(); var4 = var6) {
            var6 = (BlockElement)var5.next();
            if (var6.asBlock() != null && var4 != null) {
               var2.append(var3);
            }

            if (var6.asValue() != null && var4 instanceof Block) {
               var2.append(var3);
            }

            var6.prettyPrint(var1, var2, var3);
         }

      }

      public Block addBlock(String var1, String var2) {
         Block var3 = new Block();
         var3.type = var1;
         var3.id = var2;
         this.elements.add(var3);
         this.children.add(var3);
         return var3;
      }

      public Block getBlock(String var1, String var2) {
         Iterator var3 = this.children.iterator();

         Block var4;
         do {
            do {
               if (!var3.hasNext()) {
                  return null;
               }

               var4 = (Block)var3.next();
            } while(!var4.type.equals(var1));
         } while((var4.id == null || !var4.id.equals(var2)) && (var4.id != null || var2 != null));

         return var4;
      }

      public Value getValue(String var1) {
         Iterator var2 = this.values.iterator();

         Value var3;
         int var4;
         do {
            if (!var2.hasNext()) {
               return null;
            }

            var3 = (Value)var2.next();
            var4 = var3.string.indexOf(61);
         } while(var4 <= 0 || !var3.getKey().trim().equals(var1));

         return var3;
      }

      public void setValue(String var1, String var2) {
         Value var3 = this.getValue(var1);
         if (var3 == null) {
            this.addValue(var1, var2);
         } else {
            var3.string = var1 + " = " + var2;
         }

      }

      public Value addValue(String var1, String var2) {
         Value var3 = new Value();
         var3.string = var1 + " = " + var2;
         this.elements.add(var3);
         this.values.add(var3);
         return var3;
      }

      public void moveValueAfter(String var1, String var2) {
         Value var3 = this.getValue(var1);
         Value var4 = this.getValue(var2);
         if (var3 != null && var4 != null) {
            this.elements.remove(var3);
            this.values.remove(var3);
            this.elements.add(this.elements.indexOf(var4) + 1, var3);
            this.values.add(this.values.indexOf(var4) + 1, var3);
         }
      }
   }

   public static class Value implements BlockElement {
      public String string;

      public Value() {
      }

      public Block asBlock() {
         return null;
      }

      public Value asValue() {
         return this;
      }

      public void prettyPrint(int var1, StringBuilder var2, String var3) {
         for(int var4 = 0; var4 < var1; ++var4) {
            var2.append('\t');
         }

         var2.append(this.string.trim());
         var2.append(',');
         var2.append(var3);
      }

      public String getKey() {
         int var1 = this.string.indexOf(61);
         return var1 == -1 ? this.string : this.string.substring(0, var1);
      }

      public String getValue() {
         int var1 = this.string.indexOf(61);
         return var1 == -1 ? "" : this.string.substring(var1 + 1);
      }
   }

   public interface BlockElement {
      Block asBlock();

      Value asValue();

      void prettyPrint(int var1, StringBuilder var2, String var3);
   }
}
