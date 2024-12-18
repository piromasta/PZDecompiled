package zombie.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugType;
import zombie.util.list.PZArrayUtil;

public final class PZXmlUtil {
   private static final ThreadLocal<DocumentBuilder> documentBuilders = ThreadLocal.withInitial(() -> {
      try {
         DocumentBuilderFactory var0 = DocumentBuilderFactory.newInstance();
         return var0.newDocumentBuilder();
      } catch (ParserConfigurationException var1) {
         ExceptionLogger.logException(var1);
         throw new RuntimeException(var1);
      }
   });

   public PZXmlUtil() {
   }

   public static Element parseXml(String var0) throws PZXmlParserException {
      String var1 = ZomboidFileSystem.instance.resolveFileOrGUID(var0);

      Element var2;
      try {
         var2 = parseXmlInternal(var1);
      } catch (IOException | SAXException var6) {
         throw new PZXmlParserException("Exception thrown while parsing XML file \"" + var1 + "\"", var6);
      }

      var2 = includeAnotherFile(var2, var1);
      String var3 = var2.getAttribute("x_extends");
      if (var3 != null && var3.trim().length() != 0) {
         if (!ZomboidFileSystem.instance.isValidFilePathGuid(var3)) {
            var3 = ZomboidFileSystem.instance.resolveRelativePath(var1, var3);
         }

         Element var4 = parseXml(var3);
         Element var5 = resolve(var2, var4);
         return var5;
      } else {
         return var2;
      }
   }

   private static Element includeAnotherFile(Element var0, String var1) throws PZXmlParserException {
      String var2 = var0.getAttribute("x_include");
      if (var2 != null && var2.trim().length() != 0) {
         if (!ZomboidFileSystem.instance.isValidFilePathGuid(var2)) {
            var2 = ZomboidFileSystem.instance.resolveRelativePath(var1, var2);
         }

         Element var3 = parseXml(var2);
         if (!var3.getTagName().equals(var0.getTagName())) {
            return var0;
         } else {
            Document var4 = createNewDocument();
            Node var5 = var4.importNode(var0, true);
            Node var6 = var5.getFirstChild();

            for(Node var7 = var3.getFirstChild(); var7 != null; var7 = var7.getNextSibling()) {
               if (var7 instanceof Element) {
                  Element var8 = (Element)var4.importNode(var7, true);
                  var5.insertBefore(var8, var6);
               }
            }

            var5.normalize();
            return (Element)var5;
         }
      } else {
         return var0;
      }
   }

   private static Element resolve(Element var0, Element var1) {
      Document var2 = createNewDocument();
      Element var3 = resolve(var0, var1, var2);
      var2.appendChild(var3);
      if (DebugType.Xml.isEnabled()) {
         DebugType var10000 = DebugType.Xml;
         String var10001 = elementToPrettyStringSafe(var1);
         var10000.debugln("PZXmlUtil.resolve> \r\n<Parent>\r\n" + var10001 + "\r\n</Parent>\r\n<Child>\r\n" + elementToPrettyStringSafe(var0) + "\r\n</Child>\r\n<Resolved>\r\n" + elementToPrettyStringSafe(var3) + "\r\n</Resolved>");
      }

      return var3;
   }

   private static Element resolve(Element var0, Element var1, Document var2) {
      Element var3;
      if (isTextOnly(var0)) {
         var3 = (Element)var2.importNode(var0, true);
         return var3;
      } else {
         var3 = var2.createElement(var0.getTagName());
         ArrayList var4 = new ArrayList();
         NamedNodeMap var5 = var1.getAttributes();

         Attr var8;
         for(int var6 = 0; var6 < var5.getLength(); ++var6) {
            Node var7 = var5.item(var6);
            if (!(var7 instanceof Attr)) {
               DebugType.Xml.trace("PZXmlUtil.resolve> Skipping parent.attrib: %s", var7);
            } else {
               var8 = (Attr)var2.importNode(var7, true);
               var4.add(var8);
            }
         }

         NamedNodeMap var17 = var0.getAttributes();

         for(int var19 = 0; var19 < var17.getLength(); ++var19) {
            Node var21 = var17.item(var19);
            if (!(var21 instanceof Attr)) {
               DebugType.Xml.trace("PZXmlUtil.resolve> Skipping attrib: %s", var21);
            } else {
               Attr var9 = (Attr)var2.importNode(var21, true);
               String var10 = var9.getName();
               boolean var11 = true;

               for(int var12 = 0; var12 < var4.size(); ++var12) {
                  Attr var13 = (Attr)var4.get(var12);
                  String var14 = var13.getName();
                  if (var14.equals(var10)) {
                     var4.set(var12, var9);
                     var11 = false;
                     break;
                  }
               }

               if (var11) {
                  var4.add(var9);
               }
            }
         }

         Iterator var20 = var4.iterator();

         while(var20.hasNext()) {
            var8 = (Attr)var20.next();
            var3.setAttributeNode(var8);
         }

         TagTable var15 = PZXmlUtil.TagTable.createTagTable(var1, var2);
         TagTable var16 = PZXmlUtil.TagTable.createTagTable(var0, var2);
         var15.resolveWith(var16, var2);
         Iterator var18 = var15.m_resolvedElements.iterator();

         while(var18.hasNext()) {
            NamedTagEntry var22 = (NamedTagEntry)var18.next();
            var3.appendChild(var22.m_element);
         }

         return var3;
      }
   }

   private static boolean isTextOnly(Element var0) {
      boolean var1 = false;

      for(Node var2 = var0.getFirstChild(); var2 != null; var2 = var2.getNextSibling()) {
         boolean var3 = false;
         if (var2 instanceof Text) {
            String var4 = var2.getTextContent();
            boolean var5 = StringUtils.isNullOrWhitespace(var4);
            var3 = !var5;
         }

         if (!var3) {
            var1 = false;
            break;
         }

         var1 = true;
      }

      return var1;
   }

   private static String elementToPrettyStringSafe(Element var0) {
      try {
         return elementToPrettyString(var0);
      } catch (TransformerException var2) {
         return null;
      }
   }

   private static String elementToPrettyString(Element var0) throws TransformerException {
      Transformer var1 = TransformerFactory.newInstance().newTransformer();
      var1.setOutputProperty("indent", "yes");
      var1.setOutputProperty("omit-xml-declaration", "yes");
      var1.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
      StreamResult var2 = new StreamResult(new StringWriter());
      DOMSource var3 = new DOMSource(var0);
      var1.transform(var3, var2);
      String var4 = var2.getWriter().toString();
      return var4;
   }

   public static Document createNewDocument() {
      DocumentBuilder var0 = (DocumentBuilder)documentBuilders.get();
      Document var1 = var0.newDocument();
      return var1;
   }

   private static Element parseXmlInternal(String var0) throws SAXException, IOException {
      try {
         FileInputStream var1 = new FileInputStream(var0);

         Element var6;
         try {
            BufferedInputStream var2 = new BufferedInputStream(var1);

            try {
               DocumentBuilder var3 = (DocumentBuilder)documentBuilders.get();
               Document var4 = var3.parse(var2);
               var2.close();
               Element var5 = var4.getDocumentElement();
               var5.normalize();
               var6 = var5;
            } catch (Throwable var9) {
               try {
                  var2.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }

               throw var9;
            }

            var2.close();
         } catch (Throwable var10) {
            try {
               var1.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }

            throw var10;
         }

         var1.close();
         return var6;
      } catch (SAXException var11) {
         System.err.println("Exception parsing filename: " + var0);
         throw var11;
      }
   }

   public static void forEachElement(Element var0, Consumer<Element> var1) {
      for(Node var2 = var0.getFirstChild(); var2 != null; var2 = var2.getNextSibling()) {
         if (var2 instanceof Element) {
            var1.accept((Element)var2);
         }
      }

   }

   public static <T> T parse(Class<T> var0, String var1) throws PZXmlParserException {
      Element var2 = parseXml(var1);
      return unmarshall(var0, var2);
   }

   public static <T> T unmarshall(Class<T> var0, Element var1) throws PZXmlParserException {
      try {
         Unmarshaller var2 = PZXmlUtil.UnmarshallerAllocator.get(var0);
         Object var3 = var2.unmarshal(var1);
         return var3;
      } catch (JAXBException var4) {
         throw new PZXmlParserException("Exception thrown loading source: \"" + var1.getLocalName() + "\". Loading for type \"" + var0 + "\"", var4);
      }
   }

   public static <T> void write(T var0, File var1) throws TransformerException, IOException, JAXBException {
      Document var2 = createNewDocument();
      Marshaller var3 = PZXmlUtil.MarshallerAllocator.get(var0);
      var3.marshal(var0, var2);
      write(var2, var1);
   }

   public static void write(Document var0, File var1) throws TransformerException, IOException {
      Element var2 = var0.getDocumentElement();
      String var3 = elementToPrettyString(var2);
      FileOutputStream var4 = new FileOutputStream(var1, false);
      PrintWriter var5 = new PrintWriter(var4);
      var5.write(var3);
      var5.flush();
      var4.flush();
      var4.close();
   }

   public static <T> boolean tryWrite(T var0, File var1) {
      try {
         write(var0, var1);
         return true;
      } catch (IOException | JAXBException | TransformerException var3) {
         ExceptionLogger.logException(var3, "Exception thrown writing data: \"" + var0 + "\". Out file: \"" + var1 + "\"");
         return false;
      }
   }

   public static boolean tryWrite(Document var0, File var1) {
      try {
         write(var0, var1);
         return true;
      } catch (IOException | TransformerException var3) {
         ExceptionLogger.logException(var3, "Exception thrown writing document: \"" + var0 + "\". Out file: \"" + var1 + "\"");
         return false;
      }
   }

   private static class TagTable {
      public final HashMap<String, NamedTags> m_tags = new HashMap();
      public final ArrayList<NamedTagEntry> m_resolvedElements = new ArrayList();

      private TagTable() {
      }

      public static TagTable createTagTable(Element var0, Document var1) {
         TagTable var2 = new TagTable();

         for(Node var3 = var0.getFirstChild(); var3 != null; var3 = var3.getNextSibling()) {
            if (!(var3 instanceof Element)) {
               DebugType.Xml.trace("PZXmlUtil.resolve> Skipping node: %s", var3);
            } else {
               Element var4 = (Element)var1.importNode(var3, true);
               var2.addEntry(var4);
            }
         }

         return var2;
      }

      public NamedTagEntry getEntry(NamedTagEntry var1) {
         NamedTags var2 = (NamedTags)this.m_tags.get(var1.m_tag);
         if (var2 == null) {
            return null;
         } else {
            NamedTagEntry var3;
            if (StringUtils.isNullOrWhitespace(var1.m_name)) {
               var3 = (NamedTagEntry)PZArrayUtil.find((Iterable)var2.m_namedTags.values(), (var1x) -> {
                  return var1x.m_index == var1.m_index;
               });
               return var3;
            } else {
               var3 = (NamedTagEntry)var2.m_namedTags.get(var1.m_name);
               return var3;
            }
         }
      }

      public void addEntry(Element var1) {
         String var2 = var1.getTagName();
         int var3 = this.getTagIndex(var2);
         String var4 = this.getNodeName(var1);
         NamedTagEntry var5 = new NamedTagEntry();
         var5.m_tag = var2;
         var5.m_name = var4;
         var5.m_element = var1;
         var5.m_index = var3;
         this.addEntry(var5);
      }

      public void addEntry(NamedTagEntry var1) {
         this.m_resolvedElements.add(var1);
         NamedTags var2 = this.getOrCreateTableEntry(var1.m_tag);
         var2.m_namedTags.put(var1.getUniqueKey(), var1);
      }

      private NamedTags getOrCreateTableEntry(String var1) {
         NamedTags var2 = (NamedTags)this.m_tags.get(var1);
         if (var2 == null) {
            var2 = new NamedTags();
            this.m_tags.put(var1, var2);
         }

         return var2;
      }

      private String getNodeName(Element var1) {
         String var2 = var1.getAttribute("x_name");
         return var2;
      }

      private String getNodeNameFromTagIdx(String var1) {
         int var2 = this.getTagIndex(var1);
         String var3 = "nodeTag_" + var2;
         return var3;
      }

      private int getTagIndex(String var1) {
         NamedTags var2 = (NamedTags)this.m_tags.get(var1);
         int var3 = 0;
         if (var2 != null) {
            var3 = var2.m_namedTags.size();
         }

         return var3;
      }

      public void resolveWith(TagTable var1, Document var2) {
         Iterator var3 = var1.m_resolvedElements.iterator();

         while(var3.hasNext()) {
            NamedTagEntry var4 = (NamedTagEntry)var3.next();
            NamedTagEntry var5 = this.getEntry(var4);
            if (var5 == null) {
               this.addEntry(var4);
            } else {
               var5.m_element = PZXmlUtil.resolve(var4.m_element, var5.m_element, var2);
            }
         }

      }
   }

   private static class NamedTagEntry {
      public String m_tag;
      public String m_name;
      public Element m_element;
      public int m_index;

      private NamedTagEntry() {
      }

      public String getUniqueKey() {
         return StringUtils.isNullOrWhitespace(this.m_name) ? "node_" + this.m_index : this.m_name;
      }
   }

   private static final class UnmarshallerAllocator {
      private static final ThreadLocal<UnmarshallerAllocator> instance = ThreadLocal.withInitial(UnmarshallerAllocator::new);
      private final Map<Class, Unmarshaller> m_map = new HashMap();

      private UnmarshallerAllocator() {
      }

      public static <T> Unmarshaller get(Class<T> var0) throws JAXBException {
         return ((UnmarshallerAllocator)instance.get()).getOrCreate(var0);
      }

      private <T> Unmarshaller getOrCreate(Class<T> var1) throws JAXBException {
         Unmarshaller var2 = (Unmarshaller)this.m_map.get(var1);
         if (var2 == null) {
            JAXBContext var3 = JAXBContext.newInstance(new Class[]{var1});
            var2 = var3.createUnmarshaller();
            var2.setListener(new Unmarshaller.Listener() {
               public void beforeUnmarshal(Object var1, Object var2) {
                  super.beforeUnmarshal(var1, var2);
               }
            });
            this.m_map.put(var1, var2);
         }

         return var2;
      }
   }

   private static final class MarshallerAllocator {
      private static final ThreadLocal<MarshallerAllocator> instance = ThreadLocal.withInitial(MarshallerAllocator::new);
      private final Map<Class<?>, Marshaller> m_map = new HashMap();

      private MarshallerAllocator() {
      }

      public static <T> Marshaller get(T var0) throws JAXBException {
         return get(var0.getClass());
      }

      public static <T> Marshaller get(Class<T> var0) throws JAXBException {
         return ((MarshallerAllocator)instance.get()).getOrCreate(var0);
      }

      private <T> Marshaller getOrCreate(Class<T> var1) throws JAXBException {
         Marshaller var2 = (Marshaller)this.m_map.get(var1);
         if (var2 == null) {
            JAXBContext var3 = JAXBContext.newInstance(new Class[]{var1});
            var2 = var3.createMarshaller();
            var2.setListener(new Marshaller.Listener() {
               public void beforeMarshal(Object var1) {
                  super.beforeMarshal(var1);
               }
            });
            this.m_map.put(var1, var2);
         }

         return var2;
      }
   }

   private static class NamedTags {
      public final HashMap<String, NamedTagEntry> m_namedTags = new HashMap();

      private NamedTags() {
      }
   }
}
