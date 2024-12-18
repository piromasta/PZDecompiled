package zombie.characters.action;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import org.w3c.dom.Element;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.util.PZXmlUtil;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class ActionState {
   private final String m_name;
   public final ArrayList<ActionTransition> m_transitions = new ArrayList();
   private String[] m_tags;
   private String[] m_childTags;
   private ActionGroup m_parentActionGroup = null;
   private boolean m_isGrapplerState = false;
   private static final Comparator<ActionTransition> transitionComparator = (var0, var1) -> {
      return var1.conditionPriority != var0.conditionPriority ? var1.conditionPriority - var0.conditionPriority : var1.conditions.size() - var0.conditions.size();
   };

   public ActionState(String var1) {
      this.m_name = var1;
   }

   public String toString() {
      StringBuilder var1 = new StringBuilder();
      var1.append(this.getClass().getName()).append("\r\n");
      var1.append("{").append("\r\n");
      var1.append("\t").append("name:").append(this.m_name).append("\r\n");
      var1.append("\t").append("transitions:").append("\r\n");
      var1.append("\t{").append("\r\n");

      for(int var2 = 0; var2 < this.m_transitions.size(); ++var2) {
         var1.append(((ActionTransition)this.m_transitions.get(var2)).toString("\t")).append(",").append("\r\n");
      }

      var1.append("\t}").append("\r\n");
      var1.append("}");
      return var1.toString();
   }

   public final boolean canHaveSubStates() {
      return !PZArrayUtil.isNullOrEmpty((Object[])this.m_childTags);
   }

   public final boolean canBeSubstate() {
      return !PZArrayUtil.isNullOrEmpty((Object[])this.m_tags);
   }

   public final boolean canHaveSubState(ActionState var1) {
      return canHaveSubState(this, var1);
   }

   public boolean isGrapplerState() {
      return this.m_isGrapplerState;
   }

   public static boolean canHaveSubState(ActionState var0, ActionState var1) {
      String[] var2 = var0.m_childTags;
      String[] var3 = var1.m_tags;
      return tagsOverlap(var2, var3);
   }

   public static boolean tagsOverlap(String[] var0, String[] var1) {
      if (PZArrayUtil.isNullOrEmpty((Object[])var0)) {
         return false;
      } else if (PZArrayUtil.isNullOrEmpty((Object[])var1)) {
         return false;
      } else {
         boolean var2 = false;

         for(int var3 = 0; var3 < var0.length; ++var3) {
            String var4 = var0[var3];

            for(int var5 = 0; var5 < var1.length; ++var5) {
               String var6 = var1[var5];
               if (StringUtils.equalsIgnoreCase(var4, var6)) {
                  var2 = true;
                  break;
               }
            }
         }

         return var2;
      }
   }

   public String getName() {
      return this.m_name;
   }

   public void load(String var1) {
      File var2 = (new File(var1)).getAbsoluteFile();
      File[] var3 = var2.listFiles((var0, var1x) -> {
         return var1x.toLowerCase().endsWith(".xml");
      });
      if (var3 != null) {
         File[] var4 = var3;
         int var5 = var3.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            File var7 = var4[var6];
            this.parse(var7);
         }

         this.sortTransitions();
      }
   }

   public void parse(File var1) {
      ArrayList var2 = new ArrayList();
      ArrayList var3 = new ArrayList();
      ArrayList var4 = new ArrayList();
      String var5 = var1.getPath();

      try {
         Element var6 = PZXmlUtil.parseXml(var5);
         if (var6.getNodeName().equals("ActionState")) {
            PZXmlUtil.forEachElement(var6, (var1x) -> {
               try {
                  String var2 = var1x.getNodeName();
                  if ("isGrapplerState".equalsIgnoreCase(var2)) {
                     this.m_isGrapplerState = StringUtils.tryParseBoolean(var1x.getTextContent());
                  }
               } catch (Exception var3) {
                  DebugLog.ActionSystem.error("Error while parsing xml element: " + var1x.getNodeName());
                  DebugLog.ActionSystem.error(var3);
               }

            });
            return;
         }

         if (ActionTransition.parse(var6, var5, var2)) {
            for(int var7 = 0; var7 < var2.size(); ++var7) {
               if (((ActionTransition)var2.get(var7)).transitionTo != null && ((ActionTransition)var2.get(var7)).transitionTo.equals(this.getName())) {
                  DebugLog.ActionSystem.warn("Canceled loading wrong transition from %s to %s in file %s", this.getName(), ((ActionTransition)var2.get(var7)).transitionTo, var1.getName());
                  var2.remove(var7--);
               }
            }

            this.m_transitions.addAll(var2);
            if (DebugLog.isEnabled(DebugType.ActionSystem)) {
               DebugLog.ActionSystem.debugln("Loaded transitions from file: %s", var1.getName());
            }

            return;
         }

         if (this.parseTags(var6, var3, var4)) {
            this.m_tags = (String[])PZArrayUtil.concat(this.m_tags, (String[])var3.toArray(new String[0]));
            this.m_childTags = (String[])PZArrayUtil.concat(this.m_childTags, (String[])var4.toArray(new String[0]));
            if (DebugLog.isEnabled(DebugType.ActionSystem)) {
               DebugLog.ActionSystem.debugln("Loaded tags from file: %s", var5);
            }

            return;
         }

         if (DebugLog.isEnabled(DebugType.ActionSystem)) {
            DebugLog.ActionSystem.warn("Unrecognized xml file. It does not appear to be a transition nor a tag(s). %s", var5);
         }
      } catch (Exception var8) {
         DebugLog.ActionSystem.error("Error loading: " + var5);
         DebugLog.ActionSystem.error(var8);
      }

   }

   private boolean parseTags(Element var1, ArrayList<String> var2, ArrayList<String> var3) {
      var2.clear();
      var3.clear();
      if (var1.getNodeName().equals("tags")) {
         PZXmlUtil.forEachElement(var1, (var1x) -> {
            if (var1x.getNodeName().equals("tag")) {
               var3.add(var1x.getTextContent());
            }

         });
         return true;
      } else if (var1.getNodeName().equals("childTags")) {
         PZXmlUtil.forEachElement(var1, (var1x) -> {
            if (var1x.getNodeName().equals("tag")) {
               var3.add(var1x.getTextContent());
            }

         });
         return true;
      } else {
         return false;
      }
   }

   public void sortTransitions() {
      this.m_transitions.sort(transitionComparator);
   }

   public void resetForReload() {
      this.m_transitions.clear();
      this.m_tags = null;
      this.m_childTags = null;
   }

   public void setParentActionGroup(ActionGroup var1) {
      this.m_parentActionGroup = var1;
   }

   public ActionGroup getParentActionGroup() {
      return this.m_parentActionGroup;
   }
}
