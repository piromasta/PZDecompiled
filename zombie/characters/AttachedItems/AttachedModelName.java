package zombie.characters.AttachedItems;

import java.util.ArrayList;

public final class AttachedModelName {
   public String attachmentNameSelf;
   public String attachmentNameParent;
   public String modelName;
   public float bloodLevel;
   public ArrayList<AttachedModelName> children;

   public AttachedModelName(AttachedModelName var1) {
      this.attachmentNameSelf = var1.attachmentNameSelf;
      this.attachmentNameParent = var1.attachmentNameParent;
      this.modelName = var1.modelName;
      this.bloodLevel = var1.bloodLevel;

      for(int var2 = 0; var2 < var1.getChildCount(); ++var2) {
         AttachedModelName var3 = var1.getChildByIndex(var2);
         this.addChild(new AttachedModelName(var3));
      }

   }

   public AttachedModelName(String var1, String var2, float var3) {
      this.attachmentNameSelf = var1;
      this.attachmentNameParent = var1;
      this.modelName = var2;
      this.bloodLevel = var3;
   }

   public AttachedModelName(String var1, String var2, String var3, float var4) {
      this.attachmentNameSelf = var1;
      this.attachmentNameParent = var2;
      this.modelName = var3;
      this.bloodLevel = var4;
   }

   public void addChild(AttachedModelName var1) {
      if (this.children == null) {
         this.children = new ArrayList();
      }

      this.children.add(var1);
   }

   public int getChildCount() {
      return this.children == null ? 0 : this.children.size();
   }

   public AttachedModelName getChildByIndex(int var1) {
      return (AttachedModelName)this.children.get(var1);
   }
}
