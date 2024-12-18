package zombie.core.skinnedmodel.population;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(
   name = "clothingItem"
)
public class ClothingItemXML {
   public String m_GUID;
   public String m_MaleModel;
   public String m_FemaleModel;
   public String m_AltMaleModel;
   public String m_AltFemaleModel;
   public boolean m_Static = false;
   public ArrayList<String> m_BaseTextures = new ArrayList();
   public String m_AttachBone;
   public ArrayList<Integer> m_Masks = new ArrayList();
   public String m_MasksFolder = "media/textures/Body/Masks";
   public String m_UnderlayMasksFolder = "media/textures/Body/Masks";
   public ArrayList<String> textureChoices = new ArrayList();
   public boolean m_AllowRandomHue = false;
   public boolean m_AllowRandomTint = false;
   public String m_DecalGroup = null;
   public String m_Shader = null;
   public String m_HatCategory = null;
   public ArrayList<String> m_SpawnWith = new ArrayList();

   public ClothingItemXML() {
   }
}
