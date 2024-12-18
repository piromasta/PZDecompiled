package zombie.characters.AttachedItems;

import java.util.ArrayList;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponPart;
import zombie.scripting.objects.ModelWeaponPart;
import zombie.util.StringUtils;
import zombie.util.Type;

public final class AttachedModelNames {
   protected AttachedLocationGroup group;
   protected final ArrayList<AttachedModelName> models = new ArrayList();

   public AttachedModelNames() {
   }

   AttachedLocationGroup getGroup() {
      return this.group;
   }

   public void copyFrom(AttachedModelNames var1) {
      this.models.clear();

      for(int var2 = 0; var2 < var1.models.size(); ++var2) {
         AttachedModelName var3 = (AttachedModelName)var1.models.get(var2);
         this.models.add(new AttachedModelName(var3));
      }

   }

   public void initFrom(AttachedItems var1) {
      if (var1 == null) {
         this.group = null;
         this.models.clear();
      } else {
         this.group = var1.getGroup();
         this.models.clear();

         for(int var2 = 0; var2 < var1.size(); ++var2) {
            AttachedItem var3 = var1.get(var2);
            String var4 = var3.getItem().getStaticModelException();
            if (!StringUtils.isNullOrWhitespace(var4)) {
               String var5 = this.group.getLocation(var3.getLocation()).getAttachmentName();
               HandWeapon var6 = (HandWeapon)Type.tryCastTo(var3.getItem(), HandWeapon.class);
               float var7 = var6 == null ? 0.0F : var6.getBloodLevel();
               AttachedModelName var8 = new AttachedModelName(var5, var4, var7);
               this.models.add(var8);
               if (var6 != null) {
                  ArrayList var9 = var6.getModelWeaponPart();
                  if (var9 != null) {
                     ArrayList var10 = var6.getAllWeaponParts();

                     for(int var11 = 0; var11 < var10.size(); ++var11) {
                        WeaponPart var12 = (WeaponPart)var10.get(var11);

                        for(int var13 = 0; var13 < var9.size(); ++var13) {
                           ModelWeaponPart var14 = (ModelWeaponPart)var9.get(var13);
                           if (var12.getFullType().equals(var14.partType)) {
                              AttachedModelName var15 = new AttachedModelName(var14.attachmentNameSelf, var14.attachmentParent, var14.modelName, 0.0F);
                              var8.addChild(var15);
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }

      }
   }

   public int size() {
      return this.models.size();
   }

   public AttachedModelName get(int var1) {
      return (AttachedModelName)this.models.get(var1);
   }

   public void clear() {
      this.models.clear();
   }
}
