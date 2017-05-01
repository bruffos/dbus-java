//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.freedesktop.dbus.bin;

import org.freedesktop.DBus.Introspectable;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.Gettext;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.types.DBusStructType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.*;

public class CreateInterface {
    public String comment = "";
    boolean builtin;
    private final CreateInterface.PrintStreamFactory factory;

    private static String collapseType(Type t, Set<String> imports, Map<StructStruct, Type[]> structs, boolean container, boolean fullnames) throws DBusException {
        if (!(t instanceof ParameterizedType)) {
            if (t instanceof Class) {
                Class<? extends Object> c = (Class) t;
                if (c.isArray()) {
                    return collapseType(c.getComponentType(), imports, structs, container, fullnames) + "[]";
                } else {
                    Package p = c.getPackage();
                    if (null != imports && !"java.lang".equals(p.getName())) {
                        imports.add(c.getName());
                    }

                    if (container) {
                        return fullnames ? c.getName() : c.getSimpleName();
                    } else {
                        try {
                            Field f = c.getField("TYPE");
                            Class<? extends Object> d = (Class) f.get(c);
                            return d.getSimpleName();
                        } catch (Exception var12) {
                            return c.getSimpleName();
                        }
                    }
                }
            } else {
                return "";
            }
        } else {
            Class<? extends Object> c = (Class) ((ParameterizedType) t).getRawType();
            if (null != structs && t instanceof DBusStructType) {
                int num = 1;

                String name;
                for (name = "Struct"; null != structs.get(new StructStruct(name + num)); ++num) {
                    ;
                }

                name = name + num;
                structs.put(new StructStruct(name), ((ParameterizedType) t).getActualTypeArguments());
                return name;
            } else {
                if (null != imports) {
                    imports.add(c.getName());
                }

                if (fullnames) {
                    return c.getName();
                } else {
                    String s = c.getSimpleName();
                    s = s + '<';
                    Type[] ts = ((ParameterizedType) t).getActualTypeArguments();
                    Type[] var8 = ts;
                    int var9 = ts.length;

                    for (int var10 = 0; var10 < var9; ++var10) {
                        Type st = var8[var10];
                        s = s + collapseType(st, imports, structs, true, fullnames) + ',';
                    }

                    s = s.replaceAll(",$", ">");
                    return s;
                }
            }
        }
    }

    private static String getJavaType(String dbus, Set<String> imports, Map<StructStruct, Type[]> structs, boolean container, boolean fullnames) throws DBusException {
        if (null != dbus && !"".equals(dbus)) {
            Vector<Type> v = new Vector();
            Marshalling.getJavaType(dbus, v, 1);
            Type t = (Type) v.get(0);
            return collapseType(t, imports, structs, container, fullnames);
        } else {
            return "";
        }
    }

    public CreateInterface(CreateInterface.PrintStreamFactory _factory, boolean _builtin) {
        this.factory = _factory;
        this.builtin = _builtin;
    }

    String parseReturns(Vector<Element> out, Set<String> imports, Map<String, Integer> tuples, Map<StructStruct, Type[]> structs) throws DBusException {
        String[] names = new String[]{"Pair", "Triplet", "Quad", "Quintuple", "Sextuple", "Septuple"};
        String sig = "";
        String name = null;
        switch (out.size()) {
            case 0:
                sig = sig + "void ";
                break;
            case 1:
                sig = sig + getJavaType(((Element) out.get(0)).getAttribute("type"), imports, structs, false, false) + " ";
                break;
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                name = names[out.size() - 2];
            default:
                if (null == name) {
                    name = "NTuple" + out.size();
                }

                tuples.put(name, Integer.valueOf(out.size()));
                sig = sig + name + "<";

                Element arg;
                for (Iterator var8 = out.iterator(); var8.hasNext(); sig = sig + getJavaType(arg.getAttribute("type"), imports, structs, true, false) + ", ") {
                    arg = (Element) var8.next();
                }

                sig = sig.replaceAll(", $", "> ");
        }

        return sig;
    }

    String parseMethod(Element meth, Set<String> imports, Map<String, Integer> tuples, Map<StructStruct, Type[]> structs, Set<String> exceptions, Set<String> anns) throws DBusException {
        Vector<Element> in = new Vector();
        Vector<Element> out = new Vector();
        if (null == meth.getAttribute("name") || "".equals(meth.getAttribute("name"))) {
            System.err.println(Gettext.t("ERROR: Method name was blank, failed"));
            System.exit(1);
        }

        String annotations = "";
        String throwses = null;
        Iterator var11 = (new IterableNodeList(meth.getChildNodes())).iterator();

        while (var11.hasNext()) {
            Node a = (Node) var11.next();
            if (1 == a.getNodeType()) {
                checkNode(a, new String[]{"arg", "annotation"});
                Element e;
                if ("arg".equals(a.getNodeName())) {
                    e = (Element) a;
                    if ("out".equals(e.getAttribute("direction"))) {
                        out.add(e);
                    } else {
                        in.add(e);
                    }
                } else if ("annotation".equals(a.getNodeName())) {
                    e = (Element) a;
                    if (e.getAttribute("name").equals("org.freedesktop.DBus.Method.Error")) {
                        if (null == throwses) {
                            throwses = e.getAttribute("value");
                        } else {
                            throwses = throwses + ", " + e.getAttribute("value");
                        }

                        exceptions.add(e.getAttribute("value"));
                    } else {
                        annotations = annotations + this.parseAnnotation(e, imports, anns);
                    }
                }
            }
        }

        String sig = "";
        this.comment = "";
        sig = sig + this.parseReturns(out, imports, tuples, structs);
        sig = sig + IdentifierMangler.mangle(meth.getAttribute("name")) + "(";
        char defaultname = 97;
        String params = "";

        String type;
        String name;
        for (Iterator var14 = in.iterator(); var14.hasNext(); params = params + type + " " + IdentifierMangler.mangle(name) + ", ") {
            Element arg = (Element) var14.next();
            type = getJavaType(arg.getAttribute("type"), imports, structs, false, false);
            name = arg.getAttribute("name");
            if (null == name || "".equals(name)) {
                name = "" + defaultname++;
            }
        }

        return ("".equals(this.comment) ? "" : "   /**\n" + this.comment + "   */\n") + annotations + "  public " + sig + params.replaceAll("..$", "") + ")" + (null == throwses ? "" : " throws " + throwses) + ";";
    }

    String parseSignal(Element signal, Set<String> imports, Map<StructStruct, Type[]> structs, Set<String> anns) throws DBusException {
        Map<String, String> params = new HashMap();
        Vector<String> porder = new Vector();
        char defaultname = 97;
        imports.add("org.freedesktop.dbus.DBusSignal");
        imports.add("org.freedesktop.dbus.exceptions.DBusException");
        String annotations = "";
        Iterator var9 = (new IterableNodeList(signal.getChildNodes())).iterator();

        while (true) {
            while (true) {
                Node a;
                do {
                    if (!var9.hasNext()) {
                        String out = "";
                        out = out + annotations;
                        out = out + "   public static class " + signal.getAttribute("name");
                        out = out + " extends DBusSignal\n   {\n";

                        Iterator var15;
                        String name;
                        for (var15 = porder.iterator(); var15.hasNext(); out = out + "      public final " + (String) params.get(name) + " " + name + ";\n") {
                            name = (String) var15.next();
                        }

                        out = out + "      public " + signal.getAttribute("name") + "(String path";

                        for (var15 = porder.iterator(); var15.hasNext(); out = out + ", " + (String) params.get(name) + " " + name) {
                            name = (String) var15.next();
                        }

                        out = out + ") throws DBusException\n      {\n         super(path";

                        for (var15 = porder.iterator(); var15.hasNext(); out = out + ", " + name) {
                            name = (String) var15.next();
                        }

                        out = out + ");\n";

                        for (var15 = porder.iterator(); var15.hasNext(); out = out + "         this." + name + " = " + name + ";\n") {
                            name = (String) var15.next();
                        }

                        out = out + "      }\n";
                        out = out + "   }\n";
                        return out;
                    }

                    a = (Node) var9.next();
                } while (1 != a.getNodeType());

                checkNode(a, new String[]{"arg", "annotation"});
                if ("annotation".equals(a.getNodeName())) {
                    annotations = annotations + this.parseAnnotation((Element) a, imports, anns);
                } else {
                    Element arg = (Element) a;
                    String type = getJavaType(arg.getAttribute("type"), imports, structs, false, false);
                    String name = arg.getAttribute("name");
                    if (null == name || "".equals(name)) {
                        name = "" + defaultname++;
                    }

                    params.put(IdentifierMangler.mangle(name), type);
                    porder.add(IdentifierMangler.mangle(name));
                }
            }
        }
    }

    String parseAnnotation(Element ann, Set<String> imports, Set<String> annotations) {
        String s = "  @" + ann.getAttribute("name").replaceAll(".*\\.([^.]*)$", "$1") + "(";
        if (null != ann.getAttribute("value") && !"".equals(ann.getAttribute("value"))) {
            s = s + '"' + ann.getAttribute("value") + '"';
        }

        imports.add(ann.getAttribute("name"));
        annotations.add(ann.getAttribute("name"));
        return s + ")\n";
    }

    void parseInterface(Element iface, PrintStream out, Map<String, Integer> tuples, Map<StructStruct, Type[]> structs, Set<String> exceptions, Set<String> anns) throws DBusException {
        if (null == iface.getAttribute("name") || "".equals(iface.getAttribute("name"))) {
            System.err.println(Gettext.t("ERROR: Interface name was blank, failed"));
            System.exit(1);
        }

        out.println("package " + iface.getAttribute("name").replaceAll("\\.[^.]*$", "") + ";");
        String methods = "";
        String signals = "";
        String properties = "";
        String annotations = "";
        Set<String> imports = new TreeSet();
        imports.add("org.freedesktop.dbus.DBusInterface");
        Iterator var11 = (new IterableNodeList(iface.getChildNodes())).iterator();

        while (var11.hasNext()) {
            Node meth = (Node) var11.next();
            if (1 == meth.getNodeType()) {
                checkNode(meth, new String[]{"method", "signal", "property", "annotation"});
                if ("method".equals(meth.getNodeName())) {
                    methods = methods + this.parseMethod((Element) meth, imports, tuples, structs, exceptions, anns) + "\n";
                } else if ("signal".equals(meth.getNodeName())) {
                    signals = signals + this.parseSignal((Element) meth, imports, structs, anns);
                } else if ("property".equals(meth.getNodeName())) {
                    System.err.println("WARNING: Ignoring property");
                } else if ("annotation".equals(meth.getNodeName())) {
                    annotations = annotations + this.parseAnnotation((Element) meth, imports, anns);
                }
            }
        }

        if (imports.size() > 0) {
            var11 = imports.iterator();

            while (var11.hasNext()) {
                String i = (String) var11.next();
                out.println("import " + i + ";");
            }
        }

        out.print(annotations);
        out.print("public interface " + iface.getAttribute("name").replaceAll("^.*\\.([^.]*)$", "$1"));
        out.println(" extends DBusInterface");
        out.println("{");
        out.println(signals);
        out.println(methods);
        out.println(properties);
        out.println("}");
    }


    void createException(String name, String pack, PrintStream out) throws DBusException {
        out.println("package " + pack + ";");
        out.println("import org.freedesktop.dbus.DBusExecutionException;");
        out.print("public class " + name);
        out.println(" extends DBusExecutionException");
        out.println("{");
        out.println("   public " + name + "(String message)");
        out.println("   {");
        out.println("      super(message);");
        out.println("   }");
        out.println("}");
    }

    void createAnnotation(String name, String pack, PrintStream out) throws DBusException {
        out.println("package " + pack + ";");
        out.println("import java.lang.annotation.Retention;");
        out.println("import java.lang.annotation.RetentionPolicy;");
        out.println("@Retention(RetentionPolicy.RUNTIME)");
        out.println("public @interface " + name);
        out.println("{");
        out.println("   String value();");
        out.println("}");
    }

    void createStruct(String name, Type[] type, String pack, PrintStream out, Map<StructStruct, Type[]> existing) throws DBusException, IOException {
        out.println("package " + pack + ";");
        Set<String> imports = new TreeSet();
        imports.add("org.freedesktop.dbus.Position");
        imports.add("org.freedesktop.dbus.Struct");
        Map<StructStruct, Type[]> structs = new HashMap(existing);
        String[] types = new String[type.length];

        int i;
        for (i = 0; i < type.length; ++i) {
            types[i] = collapseType(type[i], imports, structs, false, false);
        }

        Iterator var17 = imports.iterator();

        while (var17.hasNext()) {
            String im = (String) var17.next();
            out.println("import " + im + ";");
        }

        out.println("public final class " + name + " extends Struct");
        out.println("{");
        i = 0;
        char c = 97;
        String params = "";
        String[] var12 = types;
        int var13 = types.length;

        for (int var14 = 0; var14 < var13; ++var14) {
            String t = var12[var14];
            out.println("   @Position(" + i++ + ")");
            out.println("   public final " + t + " " + c + ";");
            params = params + t + " " + c + ", ";
            ++c;
        }

        out.println("  public " + name + "(" + params.replaceAll("..$", "") + ")");
        out.println("  {");

        for (char d = 97; d < c; ++d) {
            out.println("   this." + d + " = " + d + ";");
        }

        out.println("  }");
        out.println("}");
        Map<StructStruct, Type[]> structs2 = StructStruct.fillPackages(structs, pack);
        Map<StructStruct, Type[]> tocreate = new HashMap(structs2);
        Iterator var21 = existing.keySet().iterator();

        while (var21.hasNext()) {
            StructStruct ss = (StructStruct) var21.next();
            tocreate.remove(ss);
        }

        this.createStructs(tocreate, structs);
    }

    void createTuple(String name, int num, String pack, PrintStream out) throws DBusException {
        out.println("package " + pack + ";");
        out.println("import org.freedesktop.dbus.Position;");
        out.println("import org.freedesktop.dbus.Tuple;");
        out.println("/** Just a typed container class */");
        out.print("public final class " + name);
        String types = " <";

        char t;
        for (t = 65; t < 65 + num; ++t) {
            types = types + t + ",";
        }

        out.print(types.replaceAll(",$", "> "));
        out.println("extends Tuple");
        out.println("{");
        t = 65;
        char n = 97;

        for (int i = 0; i < num; ++n) {
            out.println("   @Position(" + i + ")");
            out.println("   public final " + t + " " + n + ";");
            ++i;
            ++t;
        }

        out.print("   public " + name + "(");
        String sig = "";
        t = 65;
        n = 97;

        for (int i = 0; i < num; ++n) {
            sig = sig + t + " " + n + ", ";
            ++i;
            ++t;
        }

        out.println(sig.replaceAll(", $", ")"));
        out.println("   {");

        for (char v = 97; v < 97 + num; ++v) {
            out.println("      this." + v + " = " + v + ";");
        }

        out.println("   }");
        out.println("}");
    }

    void parseRoot(Element root) throws DBusException, IOException {
        Map<StructStruct, Type[]> structs = new HashMap();
        Set<String> exceptions = new TreeSet();
        Set<String> annotations = new TreeSet();
        Iterator var5 = (new IterableNodeList(root.getChildNodes())).iterator();

        while (true) {
            Node iface;
            HashMap tuples;
            String file;
            String path;
            String pack;
            do {
                while (true) {
                    do {
                        if (!var5.hasNext()) {
                            this.createStructs((Map) structs, (Map) structs);
                            this.createExceptions(exceptions);
                            this.createAnnotations(annotations);
                            return;
                        }

                        iface = (Node) var5.next();
                    } while (1 != iface.getNodeType());

                    checkNode(iface, new String[]{"interface", "node"});
                    if ("interface".equals(iface.getNodeName())) {
                        tuples = new HashMap();
                        String name = ((Element) iface).getAttribute("name");
                        file = name.replaceAll("\\.", "/") + ".java";
                        path = file.replaceAll("/[^/]*$", "");
                        pack = name.replaceAll("\\.[^.]*$", "");
                        break;
                    }

                    if ("node".equals(iface.getNodeName())) {
                        this.parseRoot((Element) iface);
                    } else {
                        System.err.println(Gettext.t("ERROR: Unknown node: ") + iface.getNodeName());
                        System.exit(1);
                    }
                }
            } while (pack.startsWith("org.freedesktop.DBus") && !this.builtin);

            this.factory.init(file, path);
            this.parseInterface((Element) iface, this.factory.createPrintStream(file), tuples, (Map) structs, exceptions, annotations);
            structs = StructStruct.fillPackages((Map) structs, pack);
            this.createTuples(tuples, pack);
        }
    }

    private void createAnnotations(Set<String> annotations) throws DBusException, IOException {
        Iterator var2 = annotations.iterator();

        while (true) {
            String name;
            String pack;
            do {
                if (!var2.hasNext()) {
                    return;
                }

                String fqn = (String) var2.next();
                name = fqn.replaceAll("^.*\\.([^.]*)$", "$1");
                pack = fqn.replaceAll("\\.[^.]*$", "");
            } while (pack.startsWith("org.freedesktop.DBus") && !this.builtin);

            String path = pack.replaceAll("\\.", "/");
            String file = name.replaceAll("\\.", "/") + ".java";
            this.factory.init(file, path);
            this.createAnnotation(name, pack, this.factory.createPrintStream(path, name));
        }
    }

    private void createExceptions(Set<String> exceptions) throws DBusException, IOException {
        Iterator var2 = exceptions.iterator();

        while (true) {
            String name;
            String pack;
            do {
                if (!var2.hasNext()) {
                    return;
                }

                String fqn = (String) var2.next();
                name = fqn.replaceAll("^.*\\.([^.]*)$", "$1");
                pack = fqn.replaceAll("\\.[^.]*$", "");
            } while (pack.startsWith("org.freedesktop.DBus") && !this.builtin);

            String path = pack.replaceAll("\\.", "/");
            String file = name.replaceAll("\\.", "/") + ".java";
            this.factory.init(file, path);
            this.createException(name, pack, this.factory.createPrintStream(path, name));
        }
    }

    private void createStructs(Map<StructStruct, Type[]> structs, Map<StructStruct, Type[]> existing) throws DBusException, IOException {
        Iterator var3 = structs.keySet().iterator();

        while (var3.hasNext()) {
            StructStruct ss = (StructStruct) var3.next();
            String file = ss.name.replaceAll("\\.", "/") + ".java";
            String path = ss.pack.replaceAll("\\.", "/");
            this.factory.init(file, path);
            this.createStruct(ss.name, (Type[]) structs.get(ss), ss.pack, this.factory.createPrintStream(path, ss.name), existing);
        }

    }

    private void createTuples(Map<String, Integer> typeMap, String pack) throws DBusException, IOException {
        Iterator var3 = typeMap.keySet().iterator();

        while (var3.hasNext()) {
            String tname = (String) var3.next();
            this.createTuple(tname, ((Integer) typeMap.get(tname)).intValue(), pack, this.factory.createPrintStream(pack.replaceAll("\\.", "/"), tname));
        }

    }

    static void checkNode(Node n, String... names) {
        String expected = "";
        String[] var3 = names;
        int var4 = names.length;

        for (int var5 = 0; var5 < var4; ++var5) {
            String name = var3[var5];
            if (name.equals(n.getNodeName())) {
                return;
            }

            expected = expected + name + " or ";
        }

        System.err.println(MessageFormat.format(Gettext.t("ERROR: Expected {0}, got {1}, failed."), new Object[]{expected.replaceAll("....$", ""), n.getNodeName()}));
        System.exit(1);
    }

    static void printSyntax() {
        printSyntax(System.err);
    }

    static void printSyntax(PrintStream o) {
        o.println("Syntax: CreateInterface <options> [file | busname object]");
        o.println("        Options: --no-ignore-builtin  --enable-dtd-validation --system -y --session -s --create-files -f --help -h --version -v");
    }

    public static void version() {
        System.out.println("Java D-Bus Version " + System.getProperty("Version"));
        System.exit(1);
    }

    static CreateInterface.Config parseParams(String[] args) {
        CreateInterface.Config config = new CreateInterface.Config();
        String[] var2 = args;
        int var3 = args.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            String p = var2[var4];
            if (!"--system".equals(p) && !"-y".equals(p)) {
                if (!"--session".equals(p) && !"-s".equals(p)) {
                    if ("--no-ignore-builtin".equals(p)) {
                        config.builtin = true;
                    } else if (!"--create-files".equals(p) && !"-f".equals(p)) {
                        if (!"--print-tree".equals(p) && !"-p".equals(p)) {
                            if ("--enable-dtd-validation".equals(p)) {
                                config.ignoreDtd = false;
                            } else if (!"--help".equals(p) && !"-h".equals(p)) {
                                if (!"--version".equals(p) && !"-v".equals(p)) {
                                    if (p.startsWith("-")) {
                                        System.err.println(Gettext.t("ERROR: Unknown option: ") + p);
                                        printSyntax();
                                        System.exit(1);
                                    } else if (null == config.busname) {
                                        config.busname = p;
                                    } else if (null == config.object) {
                                        config.object = p;
                                    } else {
                                        printSyntax();
                                        System.exit(1);
                                    }
                                } else {
                                    version();
                                    System.exit(0);
                                }
                            } else {
                                printSyntax(System.out);
                                System.exit(0);
                            }
                        } else {
                            config.printtree = true;
                        }
                    } else {
                        config.fileout = true;
                    }
                } else {
                    config.bus = 1;
                }
            } else {
                config.bus = 0;
            }
        }

        if (null == config.busname) {
            printSyntax();
            System.exit(1);
        } else if (null == config.object) {
            config.datafile = new File(config.busname);
            config.busname = null;
        }

        return config;
    }

    public static void main(String[] args) throws Exception {
        CreateInterface.Config config = parseParams(args);
        Reader introspectdata = null;
        if (null != config.busname) {
            try {
                DBusConnection conn = DBusConnection.getConnection(config.bus);
                Introspectable in = (Introspectable) conn.getRemoteObject(config.busname, config.object, Introspectable.class);
                String id = in.Introspect();
                if (null == id) {
                    System.err.println(Gettext.t("ERROR: Failed to get introspection data"));
                    System.exit(1);
                }

                introspectdata = new StringReader(id);
                conn.disconnect();
            } catch (DBusException var8) {
                System.err.println(Gettext.t("ERROR: Failure in DBus Communications: ") + var8.getMessage());
                System.exit(1);
            } catch (DBusExecutionException var9) {
                System.err.println(Gettext.t("ERROR: Failure in DBus Communications: ") + var9.getMessage());
                System.exit(1);
            }
        } else if (null != config.datafile) {
            try {
                introspectdata = new InputStreamReader(new FileInputStream(config.datafile));
            } catch (FileNotFoundException var7) {
                System.err.println(Gettext.t("ERROR: Could not find introspection file: ") + var7.getMessage());
                System.exit(1);
            }
        }

        try {
            CreateInterface.PrintStreamFactory factory = config.fileout ? new CreateInterface.FileStreamFactory() : new CreateInterface.ConsoleStreamFactory();
            CreateInterface createInterface = new CreateInterface((CreateInterface.PrintStreamFactory) factory, config.builtin);
            createInterface.createInterface((Reader) introspectdata, config);
        } catch (DBusException var6) {
            System.err.println("ERROR: " + var6.getMessage());
            System.exit(1);
        }

    }

    public void createInterface(Reader _introspectdata, CreateInterface.Config _config) throws ParserConfigurationException, SAXException, IOException, DBusException {
        DocumentBuilderFactory lfactory = DocumentBuilderFactory.newInstance();
        if (_config != null && _config.ignoreDtd) {
            lfactory.setValidating(false);
            lfactory.setNamespaceAware(true);
            lfactory.setFeature("http://xml.org/sax/features/namespaces", false);
            lfactory.setFeature("http://xml.org/sax/features/validation", false);
            lfactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            lfactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            lfactory.setSchema((Schema) null);
        }

        DocumentBuilder builder = lfactory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(_introspectdata));
        Element root = document.getDocumentElement();
        checkNode(root, new String[]{"node"});
        this.parseRoot(root);
    }

    static class Config {
        int bus = 1;
        String busname = null;
        String object = null;
        File datafile = null;
        boolean printtree = false;
        boolean fileout = false;
        boolean builtin = false;
        boolean ignoreDtd = true;

        Config() {
        }
    }

    static class FileStreamFactory extends CreateInterface.PrintStreamFactory {
        FileStreamFactory() {
        }

        public void init(String file, String path) {
            (new File(path)).mkdirs();
        }

        public PrintStream createPrintStream(String file) throws IOException {
            return new PrintStream(new FileOutputStream(file));
        }
    }

    static class ConsoleStreamFactory extends CreateInterface.PrintStreamFactory {
        ConsoleStreamFactory() {
        }

        public void init(String file, String path) {
        }

        public PrintStream createPrintStream(String file) throws IOException {
            System.out.println("/* File: " + file + " */");
            return System.out;
        }

        public PrintStream createPrintStream(String path, String tname) throws IOException {
            return super.createPrintStream(path, tname);
        }
    }

    public abstract static class PrintStreamFactory {
        public PrintStreamFactory() {
        }

        public abstract void init(String var1, String var2);

        public PrintStream createPrintStream(String path, String tname) throws IOException {
            String file = path + "/" + tname + ".java";
            return this.createPrintStream(file);
        }

        public abstract PrintStream createPrintStream(String var1) throws IOException;
    }
}
