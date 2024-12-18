package zombie.scripting.ui;

import java.util.ArrayList;
import java.util.Iterator;
import zombie.core.math.PZMath;
import zombie.scripting.ScriptParser;

public class XuiTableScript extends XuiScript {
   private final ArrayList<XuiTableColumnScript> columns = new ArrayList();
   private final ArrayList<XuiTableRowScript> rows = new ArrayList();
   private final ArrayList<XuiTableCellScript> cells = new ArrayList();
   private final XuiScript.XuiString xuiCellStyle = (XuiScript.XuiString)this.addVar(new XuiScript.XuiString(this, "xuiCellStyle"));
   private final XuiScript.XuiString xuiRowStyle;
   private final XuiScript.XuiString xuiColumnStyle;

   public XuiTableScript(String var1, boolean var2, XuiScriptType var3) {
      super(var1, var2, "ISXuiTableLayout", var3);
      this.xuiCellStyle.setAutoApplyMode(XuiAutoApply.Forbidden);
      this.xuiCellStyle.setScriptLoadEnabled(false);
      this.xuiCellStyle.setIgnoreStyling(true);
      this.xuiRowStyle = (XuiScript.XuiString)this.addVar(new XuiScript.XuiString(this, "xuiRowStyle"));
      this.xuiRowStyle.setAutoApplyMode(XuiAutoApply.Forbidden);
      this.xuiRowStyle.setScriptLoadEnabled(false);
      this.xuiRowStyle.setIgnoreStyling(true);
      this.xuiColumnStyle = (XuiScript.XuiString)this.addVar(new XuiScript.XuiString(this, "xuiColumnStyle"));
      this.xuiColumnStyle.setAutoApplyMode(XuiAutoApply.Forbidden);
      this.xuiColumnStyle.setScriptLoadEnabled(false);
      this.xuiColumnStyle.setIgnoreStyling(true);
   }

   public XuiScript.XuiString getCellStyle() {
      return this.xuiCellStyle;
   }

   public XuiScript.XuiString getRowStyle() {
      return this.xuiRowStyle;
   }

   public XuiScript.XuiString getColumnStyle() {
      return this.xuiColumnStyle;
   }

   public int getColumnCount() {
      return this.columns.size();
   }

   public int getRowCount() {
      return this.rows.size();
   }

   public XuiScript getColumn(int var1) {
      return var1 >= 0 && var1 < this.columns.size() ? (XuiScript)this.columns.get(var1) : null;
   }

   public XuiScript getRow(int var1) {
      return var1 >= 0 && var1 < this.rows.size() ? (XuiScript)this.rows.get(var1) : null;
   }

   public XuiScript getCell(int var1, int var2) {
      int var3 = var1 + var2 * this.columns.size();
      return var3 >= 0 && var3 < this.cells.size() ? (XuiScript)this.cells.get(var3) : null;
   }

   private int readCellIndex(String var1, int var2) {
      String[] var3 = var1.split(":");
      if (var3.length == 2) {
         int var4 = Integer.parseInt(var3[0].trim());
         int var5 = Integer.parseInt(var3[1].trim());
         return var4 + var5 * var2;
      } else {
         return -1;
      }
   }

   private int countRowsOrColumns(ScriptParser.Block var1) {
      int var2 = 0;
      Iterator var3 = var1.values.iterator();

      while(var3.hasNext()) {
         ScriptParser.Value var4 = (ScriptParser.Value)var3.next();
         String var5 = var4.getKey().trim();
         String var6 = var4.getValue().trim();
         if (!var5.isEmpty() && !var6.isEmpty() && var5.startsWith("[") && var5.contains("]")) {
            int var7 = this.getIndex(var5);
            var2 = PZMath.max(var7, var2);
         }
      }

      return var2;
   }

   public <T extends XuiScript> void LoadColumnsRows(ScriptParser.Block var1, ArrayList<T> var2) {
      Iterator var3 = var1.values.iterator();

      while(var3.hasNext()) {
         ScriptParser.Value var4 = (ScriptParser.Value)var3.next();
         String var5 = var4.getKey().trim();
         String var6 = var4.getValue().trim();
         if (!var5.isEmpty() && !var6.isEmpty() && var5.startsWith("[") && var5.contains("]")) {
            int var7 = this.getIndex(var5);
            if (var7 >= 0 && var7 < var2.size()) {
               ((XuiScript)var2.get(var7)).loadVar(this.getPostIndexKey(var5), var6);
            }
         }
      }

   }

   private String getPostIndexKey(String var1) {
      if (var1.contains("]")) {
         int var2 = var1.indexOf("]");
         return var1.substring(var2 + 1).trim();
      } else {
         return var1;
      }
   }

   private int getIndex(String var1) {
      if (var1.startsWith("[") && var1.contains("]")) {
         int var2 = var1.indexOf("]");
         return Integer.parseInt(var1.substring(1, var2));
      } else {
         return -1;
      }
   }

   public void Load(ScriptParser.Block var1) {
      XuiScript var2 = null;
      XuiScript var3 = null;
      XuiScript var4 = null;
      Iterator var5;
      ScriptParser.Value var6;
      String var7;
      String var8;
      if (this.isLayout()) {
         var5 = var1.values.iterator();

         while(var5.hasNext()) {
            var6 = (ScriptParser.Value)var5.next();
            var7 = var6.getKey().trim();
            var8 = var6.getValue().trim();
            if (!var7.isEmpty() && !var8.isEmpty()) {
               if (this.xuiCellStyle.acceptsKey(var7)) {
                  this.xuiCellStyle.fromString(var8);
               } else if (this.xuiRowStyle.acceptsKey(var7)) {
                  this.xuiRowStyle.fromString(var8);
               } else if (this.xuiColumnStyle.acceptsKey(var7)) {
                  this.xuiColumnStyle.fromString(var8);
               }
            }
         }

         var2 = XuiManager.GetStyle((String)this.xuiCellStyle.value());
         var3 = XuiManager.GetStyle((String)this.xuiRowStyle.value());
         var4 = XuiManager.GetStyle((String)this.xuiColumnStyle.value());
      }

      super.Load(var1);
      var5 = var1.values.iterator();

      while(true) {
         int var10;
         do {
            do {
               do {
                  do {
                     if (!var5.hasNext()) {
                        var5 = var1.children.iterator();

                        while(true) {
                           ScriptParser.Block var14;
                           int var20;
                           do {
                              do {
                                 if (!var5.hasNext()) {
                                    if (this.isLayout() && this.columns.size() > 0 && this.rows.size() > 0) {
                                       int var13 = this.columns.size() * this.rows.size();

                                       for(int var15 = 0; var15 < var13; ++var15) {
                                          XuiTableCellScript var18 = new XuiTableCellScript(this.xuiLayoutName, this.readAltKeys, var2);
                                          this.cells.add(var18);
                                       }

                                       Iterator var17 = var1.children.iterator();

                                       while(var17.hasNext()) {
                                          ScriptParser.Block var19 = (ScriptParser.Block)var17.next();
                                          if (var19.type.equalsIgnoreCase("xuiCell")) {
                                             var20 = this.readCellIndex(var19.id, this.columns.size());
                                             if (var20 >= 0 && var20 < var13) {
                                                ((XuiTableCellScript)this.cells.get(var20)).Load(var19);
                                                ((XuiTableCellScript)this.cells.get(var20)).cellHasLoaded = true;
                                             }
                                          }
                                       }
                                    } else if (this.isLayout() && (this.columns.size() > 0 || this.rows.size() > 0)) {
                                       this.warnWithInfo("XuiScript has only rows or columns.");
                                    }

                                    return;
                                 }

                                 var14 = (ScriptParser.Block)var5.next();
                              } while(!this.isLayout());
                           } while(!var14.type.equalsIgnoreCase("xuiColumns") && !var14.type.equalsIgnoreCase("xuiRows"));

                           boolean var16 = var14.type.equalsIgnoreCase("xuiRows");
                           var20 = this.countRowsOrColumns(var14);
                           int var21 = var16 ? this.rows.size() : this.columns.size();

                           for(var10 = 0; var10 < var20; ++var10) {
                              if (var10 >= var21) {
                                 if (var16) {
                                    XuiTableRowScript var22 = new XuiTableRowScript(this.xuiLayoutName, this.readAltKeys, var3);
                                    var22.height.setValue(1.0F, true);
                                    this.rows.add(var22);
                                 } else {
                                    XuiTableColumnScript var23 = new XuiTableColumnScript(this.xuiLayoutName, this.readAltKeys, var4);
                                    var23.width.setValue(1.0F, true);
                                    this.columns.add(var23);
                                 }
                              }
                           }

                           if (var16) {
                              this.LoadColumnsRows(var14, this.rows);
                           } else {
                              this.LoadColumnsRows(var14, this.columns);
                           }
                        }
                     }

                     var6 = (ScriptParser.Value)var5.next();
                     var7 = var6.getKey().trim();
                     var8 = var6.getValue().trim();
                  } while(var7.isEmpty());
               } while(var8.isEmpty());
            } while(!this.isLayout());
         } while(!var7.equalsIgnoreCase("xuiColumns") && !var7.equalsIgnoreCase("xuiRows"));

         String[] var9 = var8.split(":");

         for(var10 = 0; var10 < var9.length; ++var10) {
            String var11 = var9[var10].trim();
            if (var7.equalsIgnoreCase("xuiRows")) {
               XuiTableRowScript var12 = new XuiTableRowScript(this.xuiLayoutName, this.readAltKeys, var3);
               var12.loadVar("height", var11);
               this.rows.add(var12);
            } else {
               XuiTableColumnScript var24 = new XuiTableColumnScript(this.xuiLayoutName, this.readAltKeys, var4);
               var24.loadVar("width", var11);
               this.columns.add(var24);
            }
         }
      }
   }

   protected void postLoad() {
      super.postLoad();
      Iterator var1 = this.rows.iterator();

      while(var1.hasNext()) {
         XuiTableRowScript var2 = (XuiTableRowScript)var1.next();
         var2.postLoad();
      }

      var1 = this.columns.iterator();

      while(var1.hasNext()) {
         XuiTableColumnScript var3 = (XuiTableColumnScript)var1.next();
         var3.postLoad();
      }

      var1 = this.cells.iterator();

      while(var1.hasNext()) {
         XuiTableCellScript var4 = (XuiTableCellScript)var1.next();
         var4.postLoad();
      }

   }

   public static class XuiTableRowScript extends XuiScript {
      public XuiTableRowScript(String var1, boolean var2, XuiScript var3) {
         super(var1, var2, "ISXuiTableLayoutRow", XuiScriptType.Layout);
         if (var3 != null) {
            this.setStyle(var3);
         }

         this.tryToSetDefaultStyle();
      }
   }

   public static class XuiTableColumnScript extends XuiScript {
      public XuiTableColumnScript(String var1, boolean var2, XuiScript var3) {
         super(var1, var2, "ISXuiTableLayoutColumn", XuiScriptType.Layout);
         if (var3 != null) {
            this.setStyle(var3);
         }

         this.tryToSetDefaultStyle();
      }
   }

   public static class XuiTableCellScript extends XuiScript {
      protected boolean cellHasLoaded = false;

      public XuiTableCellScript(String var1, boolean var2, XuiScript var3) {
         super(var1, var2, "ISXuiTableLayoutCell", XuiScriptType.Layout);
         if (var3 != null) {
            this.setStyle(var3);
         }

         this.tryToSetDefaultStyle();
      }

      public boolean isCellHasLoaded() {
         return this.cellHasLoaded;
      }
   }
}
