package zombie.debug.options;

import java.util.ArrayList;
import java.util.Iterator;
import zombie.util.StringUtils;

public class OptionGroup implements IDebugOptionGroup {
   public final IDebugOptionGroup Group;
   private IDebugOptionGroup m_parentGroup;
   private final String m_groupName;
   private String m_fullName;
   private final ArrayList<IDebugOption> m_children;

   public OptionGroup() {
      this((IDebugOptionGroup)null);
   }

   public OptionGroup(IDebugOptionGroup var1) {
      this(var1, (String)null);
   }

   public OptionGroup(IDebugOptionGroup var1, String var2) {
      this.m_children = new ArrayList();
      this.Group = this;
      this.m_groupName = this.getGroupName(var2);
      this.m_fullName = this.m_groupName;
      if (var1 != null) {
         var1.addChild(this);
      }

   }

   public String getName() {
      return this.m_fullName;
   }

   public IDebugOptionGroup getParent() {
      return this.m_parentGroup;
   }

   public void setParent(IDebugOptionGroup var1) {
      if (this.m_parentGroup != null) {
         IDebugOptionGroup var2 = this.m_parentGroup;
         this.m_parentGroup = null;
         var2.removeChild(this);
      }

      this.m_parentGroup = var1;
      this.onFullPathChanged();
   }

   public void onFullPathChanged() {
      String var1 = getCombinedName(this.m_parentGroup, this.m_groupName);
      if (!StringUtils.equals(this.m_fullName, var1)) {
         this.m_fullName = var1;
      }

      Iterator var2 = this.m_children.iterator();

      while(var2.hasNext()) {
         IDebugOption var3 = (IDebugOption)var2.next();
         var3.onFullPathChanged();
      }

   }

   public Iterable<IDebugOption> getChildren() {
      return this.m_children;
   }

   public void addChild(IDebugOption var1) {
      if (!this.m_children.contains(var1)) {
         this.m_children.add(var1);
         var1.setParent(this);
         this.onChildAdded(var1);
      }
   }

   public void removeChild(IDebugOption var1) {
      if (this.m_children.contains(var1)) {
         this.m_children.remove(var1);
         var1.setParent((IDebugOptionGroup)null);
      }
   }

   public void onChildAdded(IDebugOption var1) {
      this.onDescendantAdded(var1);
   }

   public void onDescendantAdded(IDebugOption var1) {
      if (this.m_parentGroup != null) {
         this.m_parentGroup.onDescendantAdded(var1);
      }

   }

   public String getGroupName(String var1) {
      if (var1 == null) {
         var1 = this.getClass().getSimpleName();
         if (var1.endsWith("OG")) {
            var1 = var1.substring(0, var1.length() - 2);
         }
      }

      return var1;
   }

   public static String getCombinedName(IDebugOptionGroup var0, String var1) {
      return var0 == null ? var1 : var0.getCombinedName(var1);
   }
}
