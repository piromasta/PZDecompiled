package zombie.characters.action;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import zombie.ZomboidFileSystem;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.debug.DebugType;
import zombie.util.Type;

public final class ActionGroup {
   private String m_name;
   private String m_initialStateName;
   private final List<ActionState> m_states = new ArrayList();
   private final Map<String, ActionState> m_stateLookup = new HashMap();
   private final Map<Integer, String> stateNameLookup = new HashMap();
   private static final Map<String, ActionGroup> s_actionGroupMap = new HashMap();

   public ActionGroup() {
   }

   private void load() {
      String var1 = this.m_name;
      if (DebugLog.isEnabled(DebugType.ActionSystem)) {
         DebugLog.ActionSystem.debugln("Loading ActionGroup: " + var1);
      }

      File var2 = ZomboidFileSystem.instance.getMediaFile("actiongroups/" + var1 + "/actionGroup.xml");
      if (var2.exists() && var2.canRead()) {
         this.loadGroupData(var2);
      }

      File var3 = ZomboidFileSystem.instance.getMediaFile("actiongroups/" + var1);
      File[] var4 = var3.listFiles();
      if (var4 != null) {
         File[] var5 = var4;
         int var6 = var4.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            File var8 = var5[var7];
            if (var8.isDirectory()) {
               ActionState var9 = this.getOrCreate(var8.getName());
               String var10 = var8.getPath();
               var9.load(var10);
            }
         }
      }

   }

   private void loadGroupData(File var1) {
      Document var2;
      try {
         DocumentBuilderFactory var3 = DocumentBuilderFactory.newInstance();
         DocumentBuilder var4 = var3.newDocumentBuilder();
         var2 = var4.parse(var1);
      } catch (SAXException | IOException | ParserConfigurationException var6) {
         DebugLog.ActionSystem.error("Error loading: " + var1.getPath());
         var6.printStackTrace(DebugLog.ActionSystem);
         return;
      }

      var2.getDocumentElement().normalize();
      Element var7 = var2.getDocumentElement();
      if (!var7.getNodeName().equals("actiongroup")) {
         DebugLogStream var10000 = DebugLog.ActionSystem;
         String var10001 = var1.getPath();
         var10000.error("Error loading: " + var10001 + ", expected root element '<actiongroup>', received '<" + var7.getNodeName() + ">'");
      } else {
         for(Node var8 = var7.getFirstChild(); var8 != null; var8 = var8.getNextSibling()) {
            Element var5 = (Element)Type.tryCastTo(var8, Element.class);
            if (var5 != null && var5.getNodeName().equals("initial")) {
               this.m_initialStateName = var5.getTextContent().trim();
            }
         }

      }
   }

   public ActionState addState(ActionState var1) {
      if (this.m_states.contains(var1)) {
         DebugLog.ActionSystem.trace("State already added.");
         return var1;
      } else {
         var1.setParentActionGroup(this);
         this.m_states.add(var1);
         this.m_stateLookup.put(var1.getName().toLowerCase(), var1);
         this.stateNameLookup.put(var1.getName().hashCode(), var1.getName());
         return var1;
      }
   }

   public ActionState findState(String var1) {
      return (ActionState)this.m_stateLookup.get(var1.toLowerCase());
   }

   public String findStateName(int var1) {
      return (String)this.stateNameLookup.get(var1);
   }

   public ActionState getOrCreate(String var1) {
      var1 = var1.toLowerCase();
      ActionState var2 = this.findState(var1);
      if (var2 == null) {
         var2 = this.addState(new ActionState(var1));
      }

      return var2;
   }

   public ActionState getInitialState() {
      ActionState var1 = null;
      if (this.m_initialStateName != null) {
         var1 = this.findState(this.m_initialStateName);
      }

      if (var1 == null && !this.m_states.isEmpty()) {
         var1 = (ActionState)this.m_states.get(0);
      }

      return var1;
   }

   public ActionState getDefaultState() {
      return this.getInitialState();
   }

   public String getName() {
      return this.m_name;
   }

   public static ActionGroup getActionGroup(String var0) {
      var0 = var0.toLowerCase();
      ActionGroup var1 = (ActionGroup)s_actionGroupMap.get(var0);
      if (var1 == null && !s_actionGroupMap.containsKey(var0)) {
         var1 = new ActionGroup();
         var1.m_name = var0;
         s_actionGroupMap.put(var0, var1);

         try {
            var1.load();
         } catch (Exception var3) {
            DebugLog.ActionSystem.error("Error loading action group: " + var0);
            var3.printStackTrace(DebugLog.ActionSystem);
         }

         return var1;
      } else {
         return var1;
      }
   }

   public static void reloadAll() {
      Iterator var0 = s_actionGroupMap.entrySet().iterator();

      while(var0.hasNext()) {
         Map.Entry var1 = (Map.Entry)var0.next();
         ActionGroup var2 = (ActionGroup)var1.getValue();
         Iterator var3 = var2.m_states.iterator();

         while(var3.hasNext()) {
            ActionState var4 = (ActionState)var3.next();
            var4.resetForReload();
         }

         var2.load();
      }

   }
}
